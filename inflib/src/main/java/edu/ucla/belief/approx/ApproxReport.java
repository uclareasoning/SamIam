package edu.ucla.belief.approx;

import edu.ucla.belief.*;
import java.util.*;

public class ApproxReport {
	protected List parentList, childList;
	protected boolean conv;
	protected int loops;
	protected double origProbE;
	protected CPTShell[] origCPTShells;
	protected ApproxCut condCut;
	protected Stack cutStack;
	protected Map conditionalMap;

	public ApproxReport(List parentList, List childList, boolean conv, 
		int loops, double origProbE, CPTShell[] origCPTShells, 
		ApproxCut condCut, Stack cutStack, Map conditionalMap) {

		this.parentList = parentList;
		this.childList = childList;
		this.conv = conv;
		this.loops = loops;
		this.origProbE = origProbE;
		this.origCPTShells = origCPTShells;
		this.condCut = condCut;
		this.cutStack = cutStack;
		this.conditionalMap = conditionalMap;
	}

	public CPTShell getCPTShell(FiniteVariable child) {
		int index = childList.lastIndexOf(child);
		if (index == -1)
			return null;
		return ((ApproxCut)cutStack.peek()).cptShells[index];
	}

	public List getParentList() {
		return parentList;
	}

	public List getChildList() {
		return childList;
	}

	public double[] getFixedPoint(FiniteVariable parent) {
		int index = parentList.lastIndexOf(parent);
		if (index == -1)
			return null;
		return ((ApproxCut)cutStack.peek()).point[index];
	}

	public boolean converges() {
		return conv;
	}

	public int getLoops() {
		return loops;
	}

	public double getOrigProbE() {
		return origProbE;
	}

	public double getCondProbE() {
		return condCut.probE;
	}

	public double getCondKLTotal() {
		return condCut.klTotal;
	}

	public double getCondKLBound() {
		return condCut.klBound;
	}

	public double getFixedProbE() {
		return ((ApproxCut)cutStack.peek()).probE;
	}

	public double getFixedKLTotal() {
		return ((ApproxCut)cutStack.peek()).klTotal;
	}

	public double getFixedKLBound() {
		return ((ApproxCut)cutStack.peek()).klBound;
	}

	public Table getConditional(FiniteVariable var) {
		return (Table)conditionalMap.get(var);
	}

	public void printStack() {
		/* Yet to be implemented */
	}
}
