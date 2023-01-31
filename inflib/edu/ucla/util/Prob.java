package edu.ucla.util;

public final class Prob {
	public static double logOdds(double prob) {
		return Math.log(prob / (1.0-prob));
	}

	public static double logOddsToProb(double logOdds) {
		return 1.0 / (1.0+Math.exp(-logOdds));
	}

	public static double logOddsDiff(double prob1, double prob2) {
		return logOdds(prob1) - logOdds(prob2);
	}

	public static double applyLogOddsChange(double prob, double 
		logOddsChange) {
		return logOddsToProb(logOdds(prob) + logOddsChange);
	}

	public static double[] proportionalChanges(int index, double 
		delta, double[] thetas, boolean[] excludes) {
		double[] changes = new double[thetas.length];
		java.util.Arrays.fill(changes, 0.0);
		if (index < 0 || index >= thetas.length)
			return changes;
		changes[index] = delta;
		double sum = 0.0;
		for (int i = 0; i < thetas.length; i++)
			if (i != index && !excludes[i])
				sum += thetas[i];
		for (int i = 0; i < thetas.length; i++)
			if (i != index && !excludes[i])
				changes[i] = -delta * thetas[i] / sum;
		return changes;
	}

	public static double distance(double[] probs1, double[] probs2) {
		double max = 1.0, min = 1.0;
		for (int i = 0; i < probs1.length; i++) {
			if (probs1[i] == 0.0 && probs2[i] == 0.0)
				continue;
			double ratio = probs2[i] / probs1[i];
			if (ratio > max)
				max = ratio;
			else if (ratio < min)
				min = ratio;
		}
		return Math.log(max) - Math.log(min);
	}

	public static double kl(double[] probs1, double[] probs2) {
		double sum = 0.0;
		for (int i = 0; i < probs1.length; i++) {
			if (probs1[i] == 0.0)
				continue;
			sum -= probs1[i] * Math.log(probs2[i] / 
				probs1[i]);
		}
		return sum;
	}

	public static int random(int k) {
		return (int)Math.floor(Math.random() * k);
	}

	public static double random(double a, double b) {
		return Math.random() * (b-a) + a;
	}
}
