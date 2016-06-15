package com.github.dnvanderwerff.lagrandefinale.particle;

import android.util.Log;

import com.github.dnvanderwerff.lagrandefinale.MainActivity;
import com.github.dnvanderwerff.lagrandefinale.util.NormalDistribution;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by Jeroen on 21/05/2016.
 */
public class ParticleController {
    private CollisionMap map;
    private Particle[] particles;
    private NormalDistribution ndDirection, ndStepSize;
    private List<Particle> alives, deads;
    private Random r;
    private double surface, totalSurface;
    private int[] cellDist;
    private String activeCell;

    public double getSurface() { return surface; }
    public double getSurfaceFraction() { return surface / totalSurface; }

    public ParticleController(CollisionMap map) {
        this.map = map;
        ndDirection = new NormalDistribution(0, Math.toRadians(13));
        //ndStepSize = new NormalDistribution(0.7, 0.15);
        ndStepSize = new NormalDistribution(MainActivity.length*0.41, 0.05);
        alives = new ArrayList<>();
        deads = new LinkedList<>();
        r = new Random(System.nanoTime());
        cellDist = new int[map.getCellCount() ];

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
        } else {
            // At this point, the system needs to recover, as the cluster of particles is in the wrong location.. what is this called again?

        }
        //long resampled = System.currentTimeMillis();
        //Log.d("ParticleTime", begin + "-" + moved + "-" + resampled);
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
}
