package com.github.dnvanderwerff.lagrandefinale.particle;

import com.github.dnvanderwerff.lagrandefinale.util.NormalDistribution;

import java.util.Random;

/**
 * Created by Jeroen on 21/05/2016.
 */
public class ParticleController {
    private CollisionMap map;
    private Particle[] particles;
    private NormalDistribution ndDirection;

    public ParticleController(CollisionMap map) {
        this.map = map;
        ndDirection = new NormalDistribution(0, 13);
    }

    public void initialize(int particleAmount) {
        particles = new Particle[particleAmount];
        Random r = new Random(System.nanoTime());
        double x, y;
        for (int i = 0; i < particleAmount; i++) {
            do {
                x = r.nextDouble() * (map.width * 1.0 / 10);
                y = r.nextDouble() * (map.height * 1.0 / 10);
            } while (!map.isValidLocation(x, y));
            particles[i] = new Particle(x, y);
        }
    }

    /* Moves all particles */
    public void move(double directionDegrees) {
        for (int i = 0; i < particles.length; i++) {
            // Get step size
            double stepSize = 0.3;
            // Offset given direction by random value
            double degreesOff = ndDirection.nextValue();
            double directionRads = Math.toRadians(directionDegrees + degreesOff);
            // Change particle position
            particles[i].x += Math.sin(directionRads) * stepSize;
            particles[i].y += -Math.cos(directionRads) * stepSize;

            if (!map.isValidLocation(particles[i].x,particles[i].y))
                particles[i].valid = false;
        }
    }

    public Particle[] getParticles() {
        return particles;
    }
}
