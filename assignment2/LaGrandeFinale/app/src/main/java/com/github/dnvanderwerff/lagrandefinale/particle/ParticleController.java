package com.github.dnvanderwerff.lagrandefinale.particle;

import android.provider.Telephony;
import android.util.Log;

import com.github.dnvanderwerff.lagrandefinale.MainActivity;
import com.github.dnvanderwerff.lagrandefinale.util.NormalDistribution;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

/**
 * Created by Jeroen on 21/05/2016.
 */
public class ParticleController {
    private CollisionMap map;                               // The collision map
    private Particle[] particles;                           // Array of all particles within the map
    private NormalDistribution ndDirection, ndStepSize;     // Distribution from which to obtain direction and step sizes
    private List<Particle> alives, deads;                   // List of dead and living particles
    private Random r;                                       // Random variable
    private double surface, totalSurface;                   // Surface of location where the user is most likely to be, and total surface
    private int[] cellDist;                                 // Array indicating how the particles are distributed among the cells
    private String activeCell;                              // Text displaying which cells are dominantly occupied by particles

    private List<Cluster> previousClusters, currentClusters; // List of cluster for previous and current iteration of move()
    private LinkedList<Cluster> deadClusters;                // Sorted list of most recently deceased clusters
    private NormalDistribution ndCluster;                    // Normal distribution for placing particles in recovered cluster
    private static int MAX_RECOVERED_CLUSTERS = 3;           // Max nr of clusters that will be revived when all particles are gone
    private static int MAX_DEAD_CLUSTERS = 5;                // Max nr of dead clusters program keeps track of

    public double getSurface() { return surface; }
    public double getSurfaceFraction() { return surface / totalSurface; }

    public ParticleController(CollisionMap map) {
        this.map = map;
        ndDirection = new NormalDistribution(0, Math.toRadians(13));
        ndStepSize = new NormalDistribution(MainActivity.length*0.41, 0.15);    // in meters
        alives = new ArrayList<>();
        deads = new LinkedList<>();
        r = new Random(System.nanoTime());
        cellDist = new int[map.getCellCount() ];

        previousClusters = new ArrayList<Cluster>();
        currentClusters = new ArrayList<Cluster>();
        deadClusters = new LinkedList<Cluster>();
        ndCluster = new NormalDistribution(0,0.5);  // TODO test of deze sd wel een handige waarde is

        surface = 0;

        // Total surface
        totalSurface = map.width / 10 * map.height / 10;
    }

    public void initialize(int particleAmount) {
        particles = new Particle[particleAmount];
        Random r = new Random(System.nanoTime());
        double x, y;
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE, minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        for (int i = 0; i < particleAmount; i++) {
            do {
                x = r.nextDouble() * (map.width * 1.0 / 10);
                y = r.nextDouble() * (map.height * 1.0 / 10);
            } while (!map.isValidLocation(x, y));
            particles[i] = new Particle(x, y);
            if (particles[i].x < minX)
                minX = particles[i].x;
            else if (particles[i].x > maxX)
                maxX = particles[i].x;

            if (particles[i].y < minY)
                minY = particles[i].y;
            else if (particles[i].y > maxY)
                maxY = particles[i].y;
        }

        // Get surface
        surface = (maxX - minX) * (maxY - minY);
    }

    /* Moves all particles */
    public void move(double directionRadians) {
        //long begin = System.currentTimeMillis();
        alives.clear();
        deads.clear();
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE, minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        double stepSize;
        double newDirection;
        int cell;

        for (int i = 0; i < cellDist.length; i++)
            cellDist[i] = 0;

        // Move particles
        for (Particle p : particles) {
            // Get step size
            stepSize = ndStepSize.nextValue();

            // Offset given direction by random value
            newDirection = directionRadians + ndDirection.nextValue();

            // Change particle position
            p.x += -Math.sin(newDirection) * stepSize;
            p.y += -Math.cos(newDirection) * stepSize;

            cell = map.getCell(p.x, p.y);
            if (cell == 0) {
                deads.add(p);
            } else {
                alives.add(p);
                cellDist[cell] += 1;
                // If alive, update boundaries if needed
                if (p.x < minX)
                    minX = p.x;
                else if (p.x > maxX)
                    maxX = p.x;

                if (p.y < minY)
                    minY = p.y;
                else if (p.y > maxY)
                    maxY = p.y;
            }
        }
        //long moved = System.currentTimeMillis();


        // Reposition dead particles
        // But not if there are none alive
        if (alives.size() > 0) {
            for (Particle particle : deads) {
                Particle dest = alives.get(r.nextInt(alives.size()));
                particle.x = dest.x;
                particle.y = dest.y;
            }
        }

        // Find current clusters
        currentClusters = findCluster(alives);

        // Logging
        String current = "";
        String previous = "";
        for (int i = 0; i < currentClusters.size(); i++) {
            current += map.getCell(currentClusters.get(i).x,currentClusters.get(i).y);
        }
        for (int i = 0; i < previousClusters.size(); i++) {
            previous += map.getCell(previousClusters.get(i).x, previousClusters.get(i).y);
        }
        Log.d("previous", "Previous clusters: " + previous);
        Log.d("current","Current clusters: " + current);

        // Compare with previous clusters to find deceased clusters
        for (Cluster prev : previousClusters) {

            if (! currentClusters.contains(prev)) {
                deadClusters.addFirst(prev);
                if (deadClusters.size() > MAX_DEAD_CLUSTERS) {
                    deadClusters.removeLast();
                }
            }
        }
        Log.d("Dead", "Dead clusters: " + String.format(deadClusters.toString()));

        previousClusters = currentClusters;

        if (alives.size() == 0) {
            // At this point, the system needs to recover, as the cluster of particles is in the wrong location.. what is this called again?

            int toRecover = Math.min(deadClusters.size(), MAX_RECOVERED_CLUSTERS);
            int n = particles.length / toRecover;

            for (int i = 0; i < toRecover; i++) {
                placeParticles(deadClusters.get(i), n);
            }

        }

        // Calculate particle area
        surface = (maxX - minX) * (maxY - minY);

        // Define which cells user is in
        float p30 = alives.size() * 0.3f;
        activeCell = "";
        for (int i = 0; i < cellDist.length; i++) {
            if (cellDist[i] >= p30) {
                if (!activeCell.equals(""))
                    activeCell += ",";
                activeCell += "C" + i;
            }
        }
        if (activeCell.equals(""))
            activeCell = "No dominant cells";

    }

    public Particle[] getParticles() {
        return particles;
    }

    public String getActiveCell() {
        return activeCell;
    }


    // TODO IMPLEMENT THIS FUNCTION CORRECTLY.
    //
    // TODO FIRST, TRY SOMETHING EASY: RIGHT NOW IT JUST ASSUMES CENTRE OF THE CELL IN WHICH THE CLUSTER IS LOCATED.
    // TODO (this will not work if cluster is right in the middle of two cells for example.)
    //
    // TODO I don't really understand the x and y values that are returned from the cells in the map.getCells array.. Not what I expect them to be.
    // TODO so currently recovery does not result in a new cluster at the correct location
    public List<Cluster> findCluster(List<Particle> particles) {
        // Use cellDistr to get an estimation of where clusters are, then compute actual centres.

        List<Cluster> foundClusters = new ArrayList<Cluster>();

        double threshold = particles.size() / 3.0;    // TODO check of dit goede waarde is
        double x, y;

        // TODO getCells has length of 18, while there are 21 cells.. Solve this by modifying MapView and CollisionMap.
        for (int i = 0; i < map.getCells().length; i++) {
            if (cellDist[i] >= threshold) {
                // There is probably a cluster in the cell. Store center of cell as estimation of cluster centre.
                x = (map.getCells())[i].x;
                y = (map.getCells())[i].y;
                foundClusters.add(new Cluster(x,y));
            }
        }

        return foundClusters;
    }

    /* Place n particles around cluster centre */
    public void placeParticles(Cluster cluster, int n) {

        // Place n particles on the map around provided cluster centre
        for (int i = 0; i < n && deads.size() > 0; i++) {
            Particle p = deads.get(0);

            do {
                p.x = cluster.x + ndCluster.nextValue();
                p.y = cluster.y + ndCluster.nextValue();
            } while (map.getCell(p.x,p.y) != 0);

            cellDist[map.getCell(p.x,p.y)] += 1;

            deads.remove(0);
        }

        // TODO update minX and minY to calculate surface
        // TODO indicate what the new dominating cells are

    }

}
