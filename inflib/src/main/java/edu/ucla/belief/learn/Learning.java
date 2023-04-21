package edu.ucla.belief.learn;

import java.util.*;
import java.io.*;
import java.lang.*;
import edu.ucla.belief.*;
//{superfluous} import edu.ucla.belief.inference.JEngineGenerator;
//{superfluous} import edu.ucla.belief.recursiveconditioning.RCInferenceEngine;
import edu.ucla.structure.*;
//{superfluous} import javax.swing.SwingUtilities;

//debug
//import edu.ucla.belief.inference.WrapperInferenceEngine;
//import il2.bridge.Converter;

/**
 * A collection of static functions for performing learning on BeliefNetworks.
 * This class cannot be instantiated.
 */
public class Learning
{
	public static final double DOUBLE_BIAS_VALUE = Double.MIN_VALUE;
	//public static final PrintWriter DEFAULT_WRITER = new PrintWriter( System.out );
	//public static PrintWriter WRITER = DEFAULT_WRITER;

	private static double mLikelihood = Double.POSITIVE_INFINITY;

	/**
	 * Returns a new BeliefNetwork with CPTs learned from the EM learning
	 * algorithm.  Learning continues until the relative difference in the
	 * log-likelihoods from one iteration to the next are less than "threshold"
	 * or "maxIterations" is reached.
	 * @param bn Initial BeliefNetwork.
	 * @param data Complete or incomplete LearningDataOld for variables in "bn".
	 * @param threshold Convergence threshold.
	 * @param maxIterations Learning will repeat no more than this number of times.
	 */
	public static BeliefNetwork learnParamsEM( BeliefNetwork bn, LearningDataOld data,
					double threshold, int maxIterations,
					Dynamator dyn, boolean withBias )
		throws ArithmeticException
	{
		testDynamator( dyn );

		BeliefNetwork new_bn = bn;
		//BeliefNetwork new_bn = (BeliefNetwork) bn.clone();
		double previous, current = 0.0;

		for (int i = 0; i < maxIterations; i++) {
			previous = current;
			new_bn = learnParamsEM( new_bn, data, dyn, withBias );
			current = getLastLikelihood();
			Definitions.STREAM_TEST.println(i+") "+current);
			if( Double.isNaN( current ) ) break;
			if (previous != 0.0) {
				if ((previous - current) / previous < threshold)
					break;
			}
		}

		return new_bn;
	}

	/** @since 20051017 */
	private static void testDynamator( Dynamator dyn ) throws UnsupportedOperationException{
		//System.out.println( "Learning.testDynamator( ("+dyn.getClass().getName()+")"+dyn.getDisplayName() +" )" );
		Dynamator canonical = dyn.getCanonicalDynamator();
		if( !canonical.probabilitySupported() ) throw new UnsupportedOperationException( "Dynamator \"" + dyn.getDisplayName() + "\" does not support pr(e)" );
	}

	public static BeliefNetwork learnParamsEM( BeliefNetwork bn, LearningData data2, double threshold, int maxIterations, Dynamator dyn, boolean withBias ) throws ArithmeticException
	{
		testDynamator( dyn );

		BeliefNetwork new_bn = bn;
		//BeliefNetwork new_bn = (BeliefNetwork) bn.clone();
		double previous, current = 0.0;

		for (int i = 1; i <= maxIterations; i++) {
			previous = current;
			new_bn = learnParamsEM( new_bn, data2, dyn, withBias );
			current = getLastLikelihood();
			Definitions.STREAM_TEST.println(i+") "+current);
			if( Double.isNaN( current ) ) break;
			if (previous != 0.0) {
				if ((previous - current) / previous < threshold)
					break;
			}
		}

		return new_bn;
	}

	/**
		@author Keith Cascio
		@since 052002

		Creates a new BeliefNetwork with CPTs learned from the EM learning
		algorithm.  Learning continues until the relative difference in the
		log-likelihoods from one iteration to the next are less than "threshold"
		or "maxIterations" is reached.

		@param bn Initial BeliefNetwork.
		@param data Complete or incomplete LearningDataOld for variables in "bn".
		@param threshold Convergence threshold.
		@param maxIterations Learning will repeat no more than this number of times.
		@param pm The progress monitor to update.
		@param ll The LearningListener that will be notified when the EM algorithm terminates.
	 */
	public static Thread learnParamsEM(	BeliefNetwork bn,
						LearningDataOld data,
						Dynamator dynamator,
						double threshold,
						int maxIterations,
						boolean withBias,
						javax.swing.ProgressMonitor pm,
						EMThread.LearningListener ll )
	{
		//System.out.println( "Learning.learnParamsEM( LearningDataOld, "+threshold+", "+maxIterations+", "+withBias+" )" );
		testDynamator( dynamator );

		EMThread runnable = new EMThread( bn, data, dynamator, threshold, maxIterations, withBias, pm, ll );
		return learnParamsEM( runnable, pm, maxIterations );
	}

	/** @since 20031105 */
	public static Thread learnParamsEM(	BeliefNetwork bn,
						LearningData data2,
						Dynamator dynamator,
						double threshold,
						int maxIterations,
						boolean withBias,
						javax.swing.ProgressMonitor pm,
						EMThread.LearningListener ll )
	{
		//System.out.println( "Learning.learnParamsEM( LearningData, "+threshold+", "+maxIterations+", "+withBias+" )" );
		testDynamator( dynamator );

		EMThread runnable = new EMThread( bn, data2, dynamator, threshold, maxIterations, withBias, pm, ll );
		return learnParamsEM( runnable, pm, maxIterations );
	}

	/** @since 20031105 */
	private static Thread learnParamsEM( EMThread runnable, javax.swing.ProgressMonitor pm, int maxIterations )
	{
		pm.setMinimum(0);
		pm.setMaximum(maxIterations);
		pm.setProgress(0);
		pm.setNote( "learning, iteration 1" );
		return runnable.start();
	}

	/**
	 * Returns a new BeliefNetwork with CPTs learned from one interation
	 * of the EM learning algorithm.
	 * @param bn Initial BeliefNetwork.
	 * @param data Complete or incomplete LearningDataOld for variables in "bn".
	 */
	public static BeliefNetwork learnParamsEM( BeliefNetwork bn, LearningDataOld data, Dynamator dynamator, boolean withBias )
		throws ArithmeticException
	{
		testDynamator( dynamator );

		Iterator dataIter, varIter;
		Table cpt, tableExperience;
		FiniteVariable var;
		Map next;

		Map exp = createExperienceTables( bn, withBias );

		Map tables = new HashMap();

		mLikelihood = 0.0;
		InferenceEngine ie = dynamator.manufactureInferenceEngine( bn );
		EvidenceController ec = bn.getEvidenceController();

		//PrintWriter writer = null;
		//try{
		//writer = new PrintWriter( new FileWriter( "learningdata.txt" ) );
		//}catch( Exception e ){ e.printStackTrace(); }
		//WRITER = writer;

		for( dataIter = data.iterator(); dataIter.hasNext(); )
		{
			try{
				next = (Map) dataIter.next();
				ec.setObservations( next );
				//writer.println( data.valuesToString( next ) );
			}catch( StateNotFoundException e ){
				System.err.println( "Learning.learnParamsEM() caught " + e );
				if( Definitions.DEBUG )
				{
					System.err.println( Definitions.STR_VERBOSE_TRACE_MESSAGE );
					e.printStackTrace();
				}
			}

			mLikelihood -= Math.log( ie.probability() );

			for( varIter = bn.iterator(); varIter.hasNext(); )
			{
				var = (FiniteVariable) varIter.next();
				tableExperience = (Table) exp.get(var);
				cpt = ie.familyConditional(var);
				//((Table) exp.get(var)).addInto(cpt);//THIS IS HIDEOUS!
				tableExperience.addIntoTrivial( cpt );
				//if( var == data.getDebugVariable() )
				//{
				//	Table.printArr( tableExperience, writer );
				//	writer.println( " 1.0" );
				//}
			}
		}

		//writer.close();
		//WRITER = DEFAULT_WRITER;
		//System.out.println( "\texperience tables LearningDataOld:" );

		for( varIter = bn.iterator(); varIter.hasNext(); )
		{
			var = (FiniteVariable) varIter.next();
			tableExperience = (Table) exp.get(var);
			//System.out.println( tableExperience + "\n\n" );
			BeliefNetworks.ensureCPTProperty( tableExperience, var );
			tables.put( var, tableExperience );
		}

		bn.replaceAllPotentials( tables );
		ec.resetEvidence();

		return bn;
	}

	/**
	 * Returns a new BeliefNetwork with CPTs learned from one interation
	 * of the EM learning algorithm.
	 * @param bn Initial BeliefNetwork.
	 * @param data Complete or incomplete LearningData for variables in "bn".
	 */
	public static BeliefNetwork learnParamsEM( BeliefNetwork bn, LearningData data, Dynamator dynamator, boolean withBias ) throws ArithmeticException
	{
		testDynamator( dynamator );

		Iterator varIter;
		Table cpt, tableExperience;
		FiniteVariable var;
		double currentWeight = (double)0;

		Map exp = createExperienceTables( bn, withBias );

		Map tables = new HashMap();

		mLikelihood = 0.0;
		InferenceEngine ie = dynamator.manufactureInferenceEngine( bn );
		EvidenceController ec = bn.getEvidenceController();

		//if( ie instanceof WrapperInferenceEngine )
		//{
		//	Converter converter = ((WrapperInferenceEngine)ie).getJointWrapper().getConverter();
		//	//System.out.println( "ie instanceof WrapperInferenceEngine, Converter.getIndex():" + converter.getIndex() );
		//	//System.out.println( "bn.vertices(): " + bn.vertices() );
		//	//System.out.println( "dynamator.getClass(): " + dynamator.getClass().getName() );
		//}

		//PrintWriter writer = null;
		//try{
		//writer = new PrintWriter( new FileWriter( "learningdata2.txt" ) );
		//}catch( Exception e ){ e.printStackTrace(); }
		//WRITER = writer;

		int numRecords = data.size();
		for( int i=0; i<numRecords; i++ )
		{
			data.setCurrentRecord( i );
			currentWeight = data.getCurrentWeight();
			try{
				ec.setObservations( data );
				//writer.println( data.valuesToString() );
			}catch( StateNotFoundException e ){
				System.err.println( "Learning.learnParamsEM( LearningData ) caught " + e );
				if( Definitions.DEBUG )
				{
					System.err.println( Definitions.STR_VERBOSE_TRACE_MESSAGE );
					e.printStackTrace();
				}
				return null;
			}

			mLikelihood -= ( Math.log( ie.probability() ) * currentWeight );

			for( varIter = bn.iterator(); varIter.hasNext(); )
			{
				var = (FiniteVariable) varIter.next();
				tableExperience = (Table) exp.get(var);
				cpt = ie.familyConditional(var);
				////((Table) exp.get(var)).addInto(cpt);//THIS IS HIDEOUS!
				//cpt.scale( data.getCurrentWeight() );
				//tableExperience.addIntoTrivial( cpt );
				tableExperience.addIntoTrivialScale( cpt, currentWeight );
				//if( var == data.getDebugVariable() )
				//{
				//	Table.printArr( tableExperience, writer );
				//	writer.println( " " + currentWeight );
				//}
			}
		}

		//writer.close();
		//WRITER = DEFAULT_WRITER;
		//System.out.println( "\texperience tables LearningData:" );

		for( varIter = bn.iterator(); varIter.hasNext(); )
		{
			var = (FiniteVariable) varIter.next();
			tableExperience = (Table) exp.get(var);
			//System.out.println( tableExperience + "\n\n" );
			BeliefNetworks.ensureCPTProperty( tableExperience, var );
			tables.put( var, tableExperience );
		}

		bn.replaceAllPotentials( tables );
		ec.resetEvidence();

		return bn;
	}

	/**
	 * Returns the -log(Likelihood) for the last call to learnParamsEM().
	 */
	public static double getLastLikelihood()
	{
		return mLikelihood;
	}

	private static Map createExperienceTables( Collection vars, boolean withBias )
	{
		FiniteVariable fVar;
		Table exp;
		TableIndex index;
		double[] data;

		Map varMap = new HashMap();

		for( Iterator i = vars.iterator(); i.hasNext(); )
		{
			fVar = (FiniteVariable) i.next();
			index = new TableIndex( fVar.getCPTShell( fVar.getDSLNodeType() ).variables() );
			data = new double[ index.size() ];
			if( withBias ) Arrays.fill( data, DOUBLE_BIAS_VALUE );
			exp = new Table( index, data );
			varMap.put( fVar, exp );
		}

		return varMap;
	}

	public static final String STR_EM_FILENAME_PREFIX = "EM_";

	/** @since 060105 */
	public static String renamePathForEmOutput( String oldHuginName ){
		int index = oldHuginName.lastIndexOf( File.separatorChar );
		if( index < 0 ) index = 0;
		else ++index;
		String prefix = oldHuginName.substring( 0, index );
		String suffix = oldHuginName.substring( index );
		return prefix + STR_EM_FILENAME_PREFIX + suffix;
	}
}
