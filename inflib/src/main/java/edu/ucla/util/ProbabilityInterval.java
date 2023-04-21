package edu.ucla.util;

/** Class for probability interval. */
public class ProbabilityInterval extends Interval {
	public ProbabilityInterval() {
		super();
	}

	public ProbabilityInterval(double value) {
		super(value);
	}

	public ProbabilityInterval(double lowerBound, double upperBound) {
		super(lowerBound, upperBound);
	}

	public ProbabilityInterval(Interval interval, double offset) {
		super(interval, offset);
	}

	public double getAbsoluteLowerBound() {
		return 0.0;
	}

	public double getAbsoluteUpperBound() {
		return 1.0;
	}
}
