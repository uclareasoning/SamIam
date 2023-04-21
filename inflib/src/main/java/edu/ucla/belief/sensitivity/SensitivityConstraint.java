package edu.ucla.belief.sensitivity;

import edu.ucla.belief.*;
import edu.ucla.util.*;

public class SensitivityConstraint {
	protected FiniteVariable varY;
	protected Object valueY;
	protected FiniteVariable varZ;
	protected Object valueZ;
	protected Object arithmeticOp;
	protected Object comparisonOp;
	protected double epsilon;
	protected Interval interval;
	protected boolean flagSingleParameter;
	protected boolean flagSingleCPT;

	public static final String DIFFERENCE = " - ";
	public static final String RATIO = " / ";
	public static final Object[] ARITHMETIC_OPERATORS = { DIFFERENCE, RATIO };

	public static final String OPERATOR_EQUALS = "=";
	public static final String OPERATOR_GTE = ">=";
	public static final String OPERATOR_LTE = "<=";

	public static final Object[] COMPARISON_OPERATORS =
		{ OPERATOR_GTE, OPERATOR_EQUALS, OPERATOR_LTE };

	public SensitivityConstraint(FiniteVariable varY, Object valueY,
		FiniteVariable varZ, Object valueZ, Object arithmeticOp,
		Object comparisonOp, double epsilon ){
		this( varY, valueY, varZ, valueZ, arithmeticOp, comparisonOp, epsilon, true, true );
	}

	public SensitivityConstraint(FiniteVariable varY, Object valueY,
		FiniteVariable varZ, Object valueZ, Object arithmeticOp,
		Object comparisonOp, double epsilon, boolean flagSingleParameter, boolean flagSingleCPT )
	{
		this.varY = varY;
		this.valueY = valueY;
		this.varZ = varZ;
		this.valueZ = valueZ;
		this.arithmeticOp = arithmeticOp;
		this.comparisonOp = comparisonOp;
		this.epsilon = epsilon;
		this.flagSingleParameter = flagSingleParameter;
		this.flagSingleCPT = flagSingleCPT;

		if( !(this.flagSingleParameter || this.flagSingleCPT) ) throw new IllegalArgumentException( "must request at least one of { single-parameter, single-cpt } suggestions" );

		if (comparisonOp == OPERATOR_GTE)
			interval = new Interval(epsilon, Double.POSITIVE_INFINITY);
		else if (comparisonOp == OPERATOR_LTE)
			interval = new Interval(Double.NEGATIVE_INFINITY, epsilon);
		else if (comparisonOp == OPERATOR_EQUALS)
			interval = new Interval(epsilon);
	}

	public boolean satisfied(InferenceEngine ie)
	{
		double condY = 0.0, condZ = 0.0;
		condY = ie.conditional(varY).getCP(varY.index(valueY));
		if (varZ != null)
			condZ = ie.conditional(varZ).getCP(varZ.index(valueZ));
		if (arithmeticOp == DIFFERENCE)
			return interval.contains(condY-condZ);
		else if (arithmeticOp == RATIO)
			return interval.contains(condY/condZ);
		else
			return interval.contains(condY);
	}

	public String toString()
	{
		String s = "Pr(" + varY.getID() + "=" + valueY.toString() + ")";
		if (arithmeticOp == DIFFERENCE)
			s += " - Pr(" + varZ.getID() + "=" + valueY.toString() + ")";
		else if (arithmeticOp == RATIO)
			s += " / Pr(" + varZ.getID() + "=" + valueY.toString() + ")";
		if (comparisonOp == OPERATOR_GTE)
			s += " >= ";
		else if (comparisonOp == OPERATOR_LTE)
			s += " <= ";
		else if (comparisonOp == OPERATOR_EQUALS)
			s += " = ";
		return s + String.valueOf(epsilon);
	}
}
