package op;

import java.util.Random;

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
	
	public static void main(String[] args) {
		NormalDistribution nd = new NormalDistribution(10, 5);
		
		int c = 0;
		int c2 = 0;
		int c3 = 0;
		
		double val;
		for (int i = 0; i < 10000; i++) {
			val = nd.nextValue();
			if (Math.abs(val - 10) <= 5)
				c++;
			if (Math.abs(val - 10) <= 10)
				c2++;
			if (Math.abs(val - 10) <= 15)
				c3++;
		}
		
		System.out.println(c);
		System.out.println(c2);
		System.out.println(c3);
	}

}
