package com.github.dnvanderwerff.lagrandefinale.util;

import java.util.Random;

/**
 * Represents a Gaussian function
 */
public class NormalDistribution {
    private Random r;
    private double mean, sd;

    public NormalDistribution(double mean, double sd) {
        this.mean = mean;
        this.sd = sd;
        r = new Random(System.nanoTime());
    }

    public void setMean(double val) {
        mean = val;
    }

    public void setStandardDev(double val) {
        sd = val;
    }

    public double nextValue() {
        double val = r.nextGaussian();
        val *= sd;
        return mean + val;
    }
}
