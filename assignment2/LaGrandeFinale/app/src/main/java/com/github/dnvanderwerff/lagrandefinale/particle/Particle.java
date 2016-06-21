package com.github.dnvanderwerff.lagrandefinale.particle;

/**
 * Represents a single particle.
 */
public class Particle {
    public double x, y;
    public boolean valid;

    public Particle() {
        x = 0;
        y = 0;
        valid = true;
    }

    public Particle (double x, double y) {
        this.x = x;
        this.y = y;
        valid = true;
    }
}
