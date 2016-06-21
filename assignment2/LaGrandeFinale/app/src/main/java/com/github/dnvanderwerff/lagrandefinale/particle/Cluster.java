package com.github.dnvanderwerff.lagrandefinale.particle;

/**
 * Represents a particle cluster
 */
public class Cluster {

    public double x, y;         // x and y coordinates of cluster centre in meters
    public int nrParticles;     // Nr of particles in the cluster


    public Cluster(double xCoord, double yCoord) {
        this.x = xCoord;
        this.y = yCoord;
        this.nrParticles = 1;

    }

}
