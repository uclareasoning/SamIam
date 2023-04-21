package edu.ucla.belief;

import java.util.*;

/**
	An interface for noisy-or
*/
public interface NoisyOrShell extends CPTShell
{
	public void setWeights( List listWeights ) throws Exception;
	public void setWeights( double[] weights) throws Exception;
	public double[] weightsClone();
	public List weightsAsList();
	public CPTParameter[] getWeightParameters();
	public CPTParameter getWeightParameter(int weightIndex);
	public double[] expandNoisyOr();
}
