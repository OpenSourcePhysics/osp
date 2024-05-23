package test;

import java.util.ArrayList;

import javajs.async.SwingJSUtils.Performance;

public class PerformanceTests {

	
	public static void main(String[] args) {
		testDataToolTabFindNearestXIndex();
	}

	private static void testDataToolTabFindNearestXIndex() {
		double[] xp = new double[] {1,2,3,         4,5,6,0,0,0};
		double[] yp = new double[] {0,0,Double.NaN,0,0,0};
		System.out.println(findNearestXIndex0(3.1, xp, yp, 6,0,10) == 3);
		System.out.println(findNearestXIndex0(3.1, xp, yp, 3,0,10) == 1);
		System.out.println(findNearestXIndex0(3.1, xp, yp, 6,0,3) == 1);
		System.out.println(findNearestXIndex0(3.1, xp, yp, 6,5,10) == 4);
		
		System.out.println(findNearestXIndex(3.1, xp, yp, 6,0,10) == 3);
		System.out.println(findNearestXIndex(3.1, xp, yp, 3,0,10) == 1);
		System.out.println(findNearestXIndex(3.1, xp, yp, 6,0,3) == 1);
		System.out.println(findNearestXIndex(3.1, xp, yp, 6,5,10) == 4);
		
		long t0;
		t0 = Performance.now(0);
		for (int i = 0; i < 100000; i++) {
			findNearestXIndex0(3.1, xp, yp, 6,0,10);
		}
		System.out.println(Performance.now(t0));

		t0 = Performance.now(0);
		for (int i = 0; i < 100000; i++) {
			findNearestXIndex(3.1, xp, yp, 6,0,10);
		}
		System.out.println(Performance.now(t0));

	    xp = new double[1000000];
		for (int i = 0; i < xp.length; i++)
			xp[i] = Math.random();
		
		double x = Math.random();

		System.out.println(findNearestXIndex0(x, xp, xp, xp.length,0,1));
		System.out.println(findNearestXIndex(x, xp, xp, xp.length,0,1));

		t0 = Performance.now(0);
		for (int i = 0; i < 10; i++) {
			findNearestXIndex0(x, xp, xp, xp.length,0,1);
		}
		System.out.println(Performance.now(t0));

		t0 = Performance.now(0);
		for (int i = 0; i < 10; i++) {
			findNearestXIndex(x, xp, xp, xp.length,0,1);
		}
		System.out.println(Performance.now(t0));

		System.out.println("");
	}

	public static int findNearestXIndex(double x, double[] xpoints, double[] ypoints, int len, double min,
			double max) {

		x = Math.min(max, Math.max(min, x));

		int imin = -1;
		double dxmin = Double.MAX_VALUE;
		for (int i = 0; i < len; i++) {
			if (Double.isNaN(ypoints[i]))
				continue;
			double dx = Math.abs(x - xpoints[i]);
			if (dx < dxmin) {
				dxmin = dx;
				imin = i;
			}
		}
		if (xpoints[imin] < min)
			imin++;
		if (imin == len || xpoints[imin] > max)
			imin = len - 1;
		return imin;
	}

	public static int findNearestXIndex0(double x, double[] xpoints, double[] ypoints, int len, double min, double max) {

		x = Math.min(max, Math.max(min, x));

		// sort x data, keeping only points for which the y-value is not NaN
		ArrayList<Double> valid = new ArrayList<Double>();
		for (int i = 0; i < len; i++) {
			if (Double.isNaN(ypoints[i]))
				continue;
			valid.add(xpoints[i]);
		}
		Double[] sorted = valid.toArray(new Double[valid.size()]);
		java.util.Arrays.sort(sorted);
		int last = sorted.length - 1;

		// check if pixel outside data range
		if (x < sorted[0]) {
			return 0;
		}
		if (x >= sorted[last]) {
			return last;
		}

		// look thru sorted data to find point nearest x
		for (int i = 1; i < sorted.length; i++) {
			if (x >= sorted[i - 1] && x < sorted[i]) {
				// found it
				if (sorted[i - 1] < min) {
					x = sorted[i];
				} else if (sorted[i] > max) {
					x = sorted[i - 1];
				} else {
					x = (Math.abs(x - sorted[i - 1]) < Math.abs(x - sorted[i])) ? sorted[i - 1] : sorted[i];
				}

				// find index of first data point with this value of x
				for (int j = 0; j < xpoints.length; j++) {
					if (xpoints[j] == x && !Double.isNaN(ypoints[j])) {
						return j;
					}
				}
				return -1;
			}
		}
		return -1; // none found (should never get here)
	}


}