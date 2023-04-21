package edu.ucla.belief.sensitivity;

import edu.ucla.belief.*;
import edu.ucla.util.*;
import java.util.*;

public class SingleParamSuggestion implements SensitivitySuggestion {
	private CPTParameter cptParameter;
	private double theta;
	private ProbabilityInterval interval;
	private CPTParameter[] familyParameters;
	private double[] familyThetas;
	private ProbabilityInterval[] familyIntervals;

	/** @since 20060208 */
	public String toString(){
		StringBuffer buff = new StringBuffer( 256 );
		buff.append( "SingleParamSuggestion " );
		buff.append( cptParameter.toString() );
		buff.append( " " );
		buff.append( interval.toString() );
		return buff.toString();
	}

	/** @since 20060209 */
	public String toString( VariableStringifier stringifier, java.text.NumberFormat format ){
		return append( new StringBuffer( 256 ), stringifier, format ).toString();
	}

	/** @since 20060209 */
	public StringBuffer append( StringBuffer buff, VariableStringifier stringifier, java.text.NumberFormat format ){
		//buff.append( "SingleParamSuggestion " );
		cptParameter.append( buff, stringifier );
		buff.append( " " );
		interval.append( buff, format );
		return buff;
	}

	public SingleParamSuggestion(CPTParameter cptParameter, double
		theta, ProbabilityInterval interval) {
		this.cptParameter = cptParameter;
		this.theta = theta;
		this.interval = interval;
		familyParameters = new CPTParameter[0];
		familyThetas = new double[0];
		familyIntervals = new ProbabilityInterval[0];
	}

	public SingleParamSuggestion(int index, CPTParameter[] parameters,
		double[] thetas, ProbabilityInterval[] intervals) {
		cptParameter = parameters[index];
		theta = thetas[index];
		int varSize = cptParameter.getVariable().size();

		int calculatedIndex = index % varSize;
		//if( 0 <= calculatedIndex && calculatedIndex < intervals.length ) interval = intervals[calculatedIndex];
		//else interval = new ProbabilityInterval( (double)0, (double)1 );
		interval = safeIndex( intervals, calculatedIndex );

		familyParameters = new CPTParameter[varSize - 1];
		familyThetas = new double[varSize - 1];
		familyIntervals = new ProbabilityInterval[varSize - 1];
		int start = index / varSize * varSize;
		for (int i = 0; i < index - start; i++) {
			familyParameters[i] = parameters[i + start];
			familyThetas[i] = thetas[i + start];
			familyIntervals[i] = safeIndex( intervals, i );//intervals[i];
		}
		for (int i = index - start + 1; i < varSize; i++) {
			familyParameters[i - 1] = parameters[i + start];
			familyThetas[i - 1] = thetas[i + start];
			familyIntervals[i - 1] = safeIndex( intervals, i );//intervals[i];
		}
	}

	public FiniteVariable getVariable() {
		return cptParameter.getVariable();
	}

	/**
		@author Keith Cascio
		@since 091102
	*/
	protected static ProbabilityInterval safeIndex( ProbabilityInterval[] intervals, int index )
	{
		if( 0 <= index && index < intervals.length ) return intervals[index];
		else
		{
			return new ProbabilityInterval( (double)0, (double)1 );
		}
	}

	/** @since 20060215 */
	public void undo() throws Exception{
		if( !getVariable().getDSLNodeType().isTableType() ) throw new IllegalStateException( "Sensitivity not supported on non-CPT type variables." );
		for( int i = 0; i < familyParameters.length; i++ ){
			SingleParamSuggestion.this.undo( familyParameters[i], familyThetas[i] );
		}
		SingleParamSuggestion.this.undo( cptParameter, theta );
	}

	/** @since 20060215 */
	private void undo( CPTParameter localParameter, double localTheta ){
		int[] indexArray = localParameter.getIndices();
		CPTShell shell = localParameter.getCPTShell();
		if( !(shell instanceof TableShell) ) throw new IllegalStateException( "Sensitivity not supported on non-CPT type variables." );
		Table cpt = shell.getCPT();
		cpt.setValue( indexArray, localTheta );
	}

	/**
		@author Keith Cascio
		@since 051602
	*/
	public void adoptChange() throws Exception
	{
		CPTShell shell = cptParameter.getCPTShell();
		if ( shell instanceof NoisyOrShell )
		{
			double suggestedValue = getSuggestedValue(theta, interval);
			FiniteVariable var = cptParameter.getVariable();
			NoisyOrShell noisyOrShell =
				(NoisyOrShell)shell;
			List noisyOrWeights = noisyOrShell.weightsAsList();
			int wp = cptParameter.getIndices()[0];
			noisyOrWeights.set(wp, new Double(suggestedValue));
			int wp1 = familyParameters[0].getIndices()[0];
			noisyOrWeights.set(wp1, new Double(1-suggestedValue));
//			try {
				noisyOrShell.setWeights(noisyOrWeights);
//			} catch (Exception e) { }
		}
		else if ( shell instanceof TableShell ) {
			for( int i = 0; i < familyParameters.length; i++ )
				adoptChange( familyParameters[i], familyThetas[i], familyIntervals[i] );
			adoptChange( cptParameter, theta, interval );
		}
	}

	/**
		@author Keith Cascio
		@since 051702
	*/
	public void adoptChange( CPTParameter localParameter,
		double localTheta, ProbabilityInterval localInterval )
	{
		int[] indexArray = localParameter.getIndices();
		CPTShell shell = localParameter.getCPTShell();
		if( !(shell instanceof TableShell) ) throw new IllegalStateException( "Sensitivity not supported on non-CPT type variables." );
		Table cpt = shell.getCPT();
		cpt.setValue( indexArray, getSuggestedValue( localTheta, localInterval ) );
	}

	public Comparable getObject() {
		return cptParameter;
	}

	public CPTParameter getCPTParameter() {
		return cptParameter;
	}

	public Object getCurrentValue() {
		return new Double(theta);
	}

	public double getTheta() {
		return theta;
	}

	public Object getSuggestedValue() {
		return interval;
	}

	public ProbabilityInterval getInterval() {
		return interval;
	}

	/** @since 20060210 */
	public double getSuggestedParameterValue(){
		return getSuggestedValue( theta, interval );
	}

	/** @since 20060227 */
	public void adoptChange( double epsilon ) throws Exception
	{
		double[] suggestedValues = computeSuggestedValues( epsilon );

		FiniteVariable var = getVariable();
		int sizeInt        = var.size();
		CPTShell shell     = var.getCPTShell();
		if( !(shell instanceof TableShell) ) throw new IllegalStateException( "Sensitivity not supported on non-CPT type variables." );
		Table cpt          = shell.getCPT();
		int indexCondition = cptParameter.getIndexOfCondition();
		for( int i=0; i<sizeInt; i++ ){
			cpt.setCP( indexCondition + i, suggestedValues[i] );
		}
	}

	public static final double DOUBLE_ONE = (double)1;

	/** @since 20060227 */
	public double[] computeSuggestedValues( double epsilon ){
		double strictSuggestedValue = getSuggestedValue( theta, interval );

		double suggestedValue = strictSuggestedValue;
		if( suggestedValue > theta ) suggestedValue += epsilon;
		else                         suggestedValue -= epsilon;

		FiniteVariable var = getVariable();
		int sizeInt  = var.size();
		int indexParameter    = cptParameter.getIndexWithinCondition();
		int indexCondition = cptParameter.getIndexOfCondition();
		double delta = suggestedValue - theta;
		double suggestedComplement = DOUBLE_ONE - suggestedValue;
		double[] ret = new double[ sizeInt ];

		if( theta == DOUBLE_ONE ){
			double fill = suggestedComplement / ((double)(sizeInt-1));
			Arrays.fill( ret, fill );
		}
		else{
			double scale = suggestedComplement / (DOUBLE_ONE - theta);
			CPTShell shell = var.getCPTShell();
			for( int i=0; i<sizeInt; i++ ){
				ret[i] = shell.getCP( indexCondition + i ) * scale;
			}
		}

		ret[indexParameter] = suggestedValue;
		return ret;
	}

	/**
		@author Keith Cascio
		@since 051602
	*/
	public static double getSuggestedValue( double localTheta, ProbabilityInterval localInterval )
	{
		double absLowerBoundDelta = Math.abs(localTheta - localInterval.getLowerBound());
		double absUpperBoundDelta = Math.abs(localTheta - localInterval.getUpperBound());
		if( absLowerBoundDelta < absUpperBoundDelta ) return
			localInterval.getLowerBound();
		else return localInterval.getUpperBound();
	}

	public double getAbsoluteChange() {
		return Math.min(
			Math.abs(theta - interval.getLowerBound()),
			Math.abs(theta - interval.getUpperBound()));
	}

	public double getLogOddsChange() {
		return Math.min(
			Math.abs(Prob.logOddsDiff(theta,
			interval.getLowerBound())),
			Math.abs(Prob.logOddsDiff(theta,
			interval.getUpperBound())));
	}

	public Vector getCorrespondingChanges() {
		Vector correspondingChanges = new Vector();
		correspondingChanges.add(new
			SingleParamSuggestion(cptParameter, theta,
			interval));
		for (int i = 0; i < familyThetas.length; i++)
			correspondingChanges.add(new
				SingleParamSuggestion(familyParameters[i],
				familyThetas[i], familyIntervals[i]));
		return correspondingChanges;
	}

	public static Vector generateTableSuggestions(TableShell shell,
		SensitivityTable table) {
		Vector suggestions = new Vector();
		Table cpt = shell.getCPT();
		double[] thetas = cpt.dataclone();
		List variables = cpt.index().variables();
		FiniteVariable[] vars = new
			FiniteVariable[variables.size()];
		for (int i = 0; i < vars.length; i++)
			vars[i] = (FiniteVariable)variables.get(i);
		int varSize = vars[vars.length-1].size();
		CPTParameter[] cptParameters = shell.getCPTParameters();
		for (int i = 0; i < cptParameters.length; i += varSize) {
			Vector vector = new Vector();
			for (int j = i; j < i + varSize; j++) {
				if (table.probabilityInterval(j).
					isEmpty())
					continue;
				vector.add(new
					SingleParamSuggestion(j,
					cptParameters, thetas,
					table.correspondingChanges(j)));
			}
			SingleParamSuggestion optimal =
				findOptimal(vector);
			if (optimal != null)
				suggestions.add(optimal);
		}
		return suggestions;
	}

	public static Vector tableSuggestions(TableShell shell,
		SensitivityTable table) {
		Vector suggestions = new Vector();
		Table cpt = shell.getCPT();
		double[] thetas = cpt.dataclone();
		List variables = cpt.index().variables();
		FiniteVariable[] vars = new
			FiniteVariable[variables.size()];
		for (int i = 0; i < vars.length; i++)
			vars[i] = (FiniteVariable)variables.get(i);
		int varSize = vars[vars.length-1].size();
		CPTParameter[] cptParameters = shell.getCPTParameters();
		for (int i = 0; i < cptParameters.length; i++) {
			if (table.probabilityInterval(i).isEmpty())
				continue;
			suggestions.add(new
				SingleParamSuggestion(i,
				cptParameters, thetas,
				table.correspondingChanges(i)));
		}
		return suggestions;
	}

	public static Vector generateNoisyOrSuggestions(NoisyOrShell
		shell, SensitivityTable table) {
		Vector suggestions = new Vector();
		CPTParameter[] parameters =
			shell.getWeightParameters();
		double[] weights = shell.weightsClone();
		for (int i = 0; i < table.size(); i += 2) {
			if (table.probabilityInterval(i).isEmpty())
				continue;
			suggestions.add(new SingleParamSuggestion(i,
				parameters, weights,
				table.correspondingChanges(i)));
		}
		return suggestions;
	}

	/**
		@author Hei Chan
		@author Keith Cascio
		@since 030503
	*/
	private static SingleParamSuggestion findOptimal( Vector vector )
	{
		if( vector.isEmpty() ) return null;
		else if( vector.size() < 3 ) return (SingleParamSuggestion)vector.get(0);
		else
		{
			SingleParamSuggestion optimal = null;
			SingleParamSuggestion suggestion;
			double optimalLogOddsChange = Double.POSITIVE_INFINITY;
			double logOddsChange;
			for(int i = 0; i < vector.size(); i++)
			{
				suggestion =
					(SingleParamSuggestion)vector.get(i);
				logOddsChange = suggestion.getLogOddsChange();
				if( logOddsChange < optimalLogOddsChange )
				{
					optimal = suggestion;
					optimalLogOddsChange = logOddsChange;
				}
			}

			return optimal;
		}
	}

	public int sign()
	{
		if (getSuggestedValue(theta, interval) > theta)
			return 1;
		else if (getSuggestedValue(theta, interval) == theta)
			return 0;
		else
			return -1;
	}
}
