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
    private NormalDistribution ndStepSize;                  // Distribution from which to obtain step sizes
    private List<Particle> alives, deads;                   // List of dead and living particles
    private Random r;                                       // Random variable
    private double surface, totalSurface;                   // Surface of location where the user is most likely to be, and total surface
    private int[] cellDist;                                 // Array indicating how the particles are distributed among the cells
    private String activeCell;                              // Text displaying which cells are dominantly occupied by particles
    private List<Integer> dominantCells;                    // List containing dominant cells

    private List<Cluster> previousClusters, currentClusters; // List of cluster for previous and current iteration of move()
    private LinkedList<Cluster> deadClusters;                // Sorted list of most recently deceased clusters
    private NormalDistribution ndCluster;                    // Normal distribution for placing particles in recovered cluster
    private static int MAX_RECOVERED_CLUSTERS = 3;           // Max nr of clusters that will be revived when all particles are gone
    private static int MAX_DEAD_CLUSTERS = 3;                // Max nr of dead clusters program keeps track of
    private static double DELTA_CLUSTER_CENTRES = MainActivity.length*0.41;       // Distance that two clusters centres can differ while being
                                                             // seen as same cluster (used for cluster tracking) in meters
    private static double CLUSTER_RADIUS = 1.5;              // in meters
    private int CLUSTER_PARTICLES_THRESHOLD;                 // Min nr of particles in possible cluster to classify it as an actual cluster
    private List<Cluster> possibleClusters;

    public double getSurface() { return surface; }
    public double getSurfaceFraction() { return surface / totalSurface; }

    public ParticleController(CollisionMap map) {
        this.map = map;

        //ndDirection = new NormalDistribution(0, Math.toRadians(13));
        ndStepSize = new NormalDistribution(MainActivity.length*0.41, 0.15);    // in meters

        alives = new ArrayList<>();
        deads = new LinkedList<>();
        r = new Random(System.nanoTime());
        cellDist = new int[map.getCellCount() ];
        dominantCells = new ArrayList<Integer>();

        possibleClusters = new ArrayList<Cluster>();
        previousClusters = new ArrayList<Cluster>();
        currentClusters = new ArrayList<Cluster>();
        deadClusters = new LinkedList<Cluster>();
        ndCluster = new NormalDistribution(0,0.8);

        surface = 0;

        // Total surface
        totalSurface = map.width / 10 * map.height / 10;
    }

    public void initialize(int particleAmount) {
        particles = new Particle[particleAmount];
        Random r = new Random(System.nanoTime());
        CLUSTER_PARTICLES_THRESHOLD = particles.length / 10;

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

    public void initialize(int particleAmount, double[] cellDistribution) {
        particles = new Particle[particleAmount];
        Random r = new Random(System.nanoTime());
        Cell[] cells = map.getCells();

        // Cell distribution contains cells 0 - 20. 0 is not actually a cell
        double x, y;
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE, minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        Cell c;
        int pcount = 0;
        for (int i = 1; i < cellDistribution.length; i++) {
            int numParticles = (int)(particleAmount * cellDistribution[i]);
            if (i == cellDistribution.length - 1) // Make sure that particleAmount particles are placed
                numParticles = particleAmount - pcount;
            c = cells[i - 1];
            for (int j = 0; j < numParticles; j++) {
                do {
                    x = c.x + r.nextDouble() * (c.width);
                    y = c.y + r.nextDouble() * (c.height);
                } while (!map.isValidLocation(x, y));
                Particle p = new Particle(x, y);
                particles[pcount++] = p;
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

        // Get surface
        surface = (maxX - minX) * (maxY - minY);
    }

    /* Moves all particles */
    public void move(double directionRadians, NormalDistribution ndDirection) {

        alives.clear();
        deads.clear();
        possibleClusters.clear();

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
            p.x += Math.sin(newDirection) * stepSize;
            p.y += -Math.cos(newDirection) * stepSize;

            cell = map.getCell(p.x, p.y);

            if (cell == 0) {
                // Particle is dead
                deads.add(p);
            } else {
                // Particle is alive
                alives.add(p);
                cellDist[cell] += 1;

                // Update boundaries if needed
                if (p.x < minX)
                    minX = p.x;
                else if (p.x > maxX)
                    maxX = p.x;

                if (p.y < minY)
                    minY = p.y;
                else if (p.y > maxY)
                    maxY = p.y;
            }

            addPossibleCluster(p);

        }

        // Reposition dead particles
        // But not if there are none alive
        if (alives.size() > 0) {
            for (Particle p : deads) {
                Particle dest = alives.get(r.nextInt(alives.size()));
                p.x = dest.x;
                p.y = dest.y;

                addPossibleCluster(p);
            }
        }

        mergePossibleClusters();

        // Move dead clusters
        for (Cluster c : deadClusters) {
            // Get step size
            stepSize = ndStepSize.nextValue();

            // Offset given direction by random value
            newDirection = directionRadians + ndDirection.nextValue();

            // Change particle position
            c.x += -Math.sin(newDirection) * stepSize;
            c.y += -Math.cos(newDirection) * stepSize;
        }

        // Find current clusters
        currentClusters = findCluster();

        // Logging
        String current = "";
        String previous = "";
        for (int i = 0; i < currentClusters.size(); i++) {
            current += String.format("%d, ", map.getCell(currentClusters.get(i).x,currentClusters.get(i).y));
        }
        for (int i = 0; i < previousClusters.size(); i++) {
            //previous += map.getCell(previousClusters.get(i).x, previousClusters.get(i).y) + ",";
            previous += previousClusters.get(i).x + "," + previousClusters.get(i).y;
        }
        Log.d("previous", previousClusters.size() + " previous clusters: " + previous);
        Log.d("current", currentClusters.size() + " current clusters: " + current);

        // Compare newly found clusters to previously found clusters
        findDeadClusters();

        // Logging
        String dead = "";
        for (int i = 0; i < deadClusters.size(); i++) {
            dead += map.getCell(deadClusters.get(i).x, deadClusters.get(i).y) + ",";
        }
        Log.d("Dead", deadClusters.size() + " dead clusters: " + dead);

        previousClusters = currentClusters;

        // Check if all particles are dead; if so, recover
        if (alives.size() == 0) {

            int toRecover = Math.min(deadClusters.size(), MAX_RECOVERED_CLUSTERS);
            int n = (toRecover != 0) ? particles.length / toRecover : 0;   // Nr of particles to be recovered per cluster

            for (int i = 0; i < toRecover; i++) {
                recoverCluster(deadClusters.get(i), n);
            }

        }

        // Calculate particle area
        surface = (maxX - minX) * (maxY - minY);

        // Define which cells user is in
        dominantCells.clear();
        float p30 = Math.max(alives.size() * 0.3f, 1.0f); // Just to be sure, this must never be 0 (recovery should make this unnecessary)
        activeCell = "";
        for (int i = 0; i < cellDist.length; i++) {
            if (cellDist[i] >= p30) {
                if (!activeCell.equals(""))
                    activeCell += ",";
                activeCell += "C" + i;
                dominantCells.add(i);
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

    public List<Integer> getDominantCells() {
        return dominantCells;
    }

    // Use particle location to find possible cluster
    public void addPossibleCluster(Particle p) {
        if (possibleClusters.size() == 0) {
            possibleClusters.add(new Cluster(p.x, p.y));
        } else {
            boolean found = false;
            for (Cluster c : possibleClusters) {
                if (Math.abs(c.x - p.x) < CLUSTER_RADIUS && Math.abs(c.y - p.y) < CLUSTER_RADIUS) {
                    // Newly placed particle is close enough to cluster radius
                    // Add particle to possible cluster and update weighted centre
                    c.nrParticles++;
                    c.x = ((c.nrParticles - 1) * c.x + p.x) / c.nrParticles;
                    c.y = ((c.nrParticles - 1) * c.y + p.y) / c.nrParticles;
                    found = true;
                    break;
                }
            }
            if (!found) {
                // Add newly placed particle as a new possible location of a cluster
                possibleClusters.add(new Cluster(p.x, p.y));
            }
        }
    }

    // Compare with previous clusters to find deceased clusters
    public void findDeadClusters() {
        for (Cluster prev: previousClusters) {
            boolean deceased = true;

            for (Cluster curr : currentClusters) {

                if (matchingCluster(prev,curr)) {
                    // Prev cluster is still present
                    deceased = false;
                    break;
                }
            }

            if (deceased) {
                // Prev cluster is dead, add it to deadClusters
                deadClusters.addFirst(prev);
                if (deadClusters.size() > MAX_DEAD_CLUSTERS) {
                    deadClusters.removeLast();
                }
            }

        }
    }

    public void mergePossibleClusters() {
        // Merge clusters if they are close together
        for (int i = 0; i < possibleClusters.size(); i++) {
            for (int j = i + 1; j < possibleClusters.size(); j++) {
                Cluster a = possibleClusters.get(i);
                Cluster b = possibleClusters.get(j);

                if (Math.abs(a.x - b.x) < DELTA_CLUSTER_CENTRES &&
                        Math.abs(a.y - b.y) < DELTA_CLUSTER_CENTRES) {
                    // Merge clusters a and b
                    a.x = (a.nrParticles * a.x + b.nrParticles * b.x) / (a.nrParticles + b.nrParticles);
                    a.y = (a.nrParticles * a.y + b.nrParticles * b.y) / (a.nrParticles + b.nrParticles);
                    a.nrParticles += b.nrParticles;

                    // Remove cluster b from previousClusters and possibleClusters
                    for (int k = 0; k < previousClusters.size(); k++) {
                        Cluster prev = previousClusters.get(k);
                        if (Math.abs(prev.x - b.x) < DELTA_CLUSTER_CENTRES &&
                                Math.abs(prev.y - b.y) < DELTA_CLUSTER_CENTRES) {
                            previousClusters.remove(k);
                            break;
                        }
                    }
                    possibleClusters.remove(j);
                } else {

                    // If multiple clusters indicated in same cell, but they cannot be merged,
                    // large likelihood that it's a smeared out collection of particles.
                    // Don't detect this as a cluster, since it is not at all converged
                    if ((map.getCell(a.x,a.y)) == map.getCell(b.x, b.y)) {
                        possibleClusters.remove(i);
                        possibleClusters.remove(j - 1);
                    }
                }

            }
        }
    }

    /* Use particle location to find and merge possible clusters */
    public List<Cluster> findCluster() {

        // Check all possible clusters wether they have enough particles to be seen as an actual cluster
        List<Cluster> foundClusters = new ArrayList<Cluster>();

        for (Cluster c : possibleClusters) {
            if (c.nrParticles > CLUSTER_PARTICLES_THRESHOLD) {
                foundClusters.add(new Cluster(c.x, c.y));
                Log.d("Found",String.format("Cluster found at x: %f, y: %f in cell : %d",c.x,c.y,map.getCell(c.x,c.y)));
            }
        }

        return foundClusters;
    }

    /* Recover cluster by placing n particles around cluster centre */
    public void recoverCluster(Cluster cluster, int n) {
        int counter;

        // Place n particles on the map around provided cluster centre
        for (int i = 0; i < n && deads.size() > 0; i++) {
            Particle p = deads.get(0);
            counter = 0;
            do {
                p.x = cluster.x + ndCluster.nextValue();
                p.y = cluster.y + ndCluster.nextValue();
                counter++;
                if (counter > 10) return;   // If particles cannot be placed, at some point stop recovering the cluster
                                            // This is not the best solution, but we'll stick with this for now
            } while (!map.isValidLocation(p.x,p.y));

            cellDist[map.getCell(p.x,p.y)] += 1;

            deads.remove(0);
        }
    }

    /* Check if two clusters are actually the same cluster (ie approximately equal cluster centres) */
    public boolean matchingCluster(Cluster a, Cluster b) {
        boolean match = false;

        if (Math.abs(a.x - b.x) < DELTA_CLUSTER_CENTRES) {
            if (Math.abs(a.y - b.y) < DELTA_CLUSTER_CENTRES) {
                match = true;
            }
        }
        return match;
    }

}
