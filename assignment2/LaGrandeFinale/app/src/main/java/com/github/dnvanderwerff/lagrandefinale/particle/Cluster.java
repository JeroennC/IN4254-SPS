package com.github.dnvanderwerff.lagrandefinale.particle;

/**
 * Created by Danielle on 16-6-16.
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
