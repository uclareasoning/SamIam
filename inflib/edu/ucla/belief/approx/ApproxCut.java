package edu.ucla.belief.approx;

import edu.ucla.belief.*;

public class ApproxCut {
	protected FiniteVariable[] parents, children;
	protected double[][] point;
	protected CPTShell[] cptShells;
	protected double probE, klTotal, klBound;

	public ApproxCut(FiniteVariable[] parents, FiniteVariable[] 
		children, double[][] point, CPTShell[] cptShells, 
		double probE, double klTotal, double klBound) {

		this.parents = parents;
		this.children = children;
		this.point = point;
		this.cptShells = cptShells;
		this.probE = probE;
		this.klTotal = klTotal;
		this.klBound = klBound;
	}
}
