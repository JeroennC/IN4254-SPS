package com.github.dnvanderwerff.lagrandefinale.util;

import java.util.Random;

/**
 * Created by Jeroen on 21/05/2016.
 */
public class NormalDistribution {
    private Random r;
    private double mean, sd;

    public NormalDistribution(double mean, double sd) {
        this.mean = mean;
        this.sd = sd;
        r = new Random(System.nanoTime());
    }

    public double nextValue() {
        double val = r.nextGaussian();
        val *= sd;
        return mean + val;
    }
}
