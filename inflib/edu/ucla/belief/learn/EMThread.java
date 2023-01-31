package edu.ucla.belief.learn;

import java.util.*;
import java.io.*;
import java.lang.*;
import edu.ucla.belief.*;
import edu.ucla.structure.*;
import javax.swing.SwingUtilities;

/** Removed from Learning.java

	@author keith cascio
	@since 20020520 */
public class EMThread implements Runnable//extends Thread
{
	/** @since 20020520 */
	public interface LearningListener
	{
		public void setBeliefNetwork( BeliefNetwork bn );
		public void handleLearningError( String msg );
		public ThreadGroup getThreadGroup();
	}

	public EMThread(BeliefNetwork bn,
			LearningDataOld data,
			Dynamator dynamator,
			double threshold,
			int maxIterations,
			boolean withBias,
			javax.swing.ProgressMonitor pm,
			LearningListener ll )
	{
		this.bn = bn;
		this.data = data;
		this.dynamator = dynamator;
		this.threshold = threshold;
		this.maxIterations = maxIterations;
		this.flagWithBias = withBias;
		this.pm = pm;
		this.ll = ll;
	}

	public EMThread(BeliefNetwork bn,
			LearningData data2,
			Dynamator dynamator,
			double threshold,
			int maxIterations,
			boolean withBias,
			javax.swing.ProgressMonitor pm,
			LearningListener ll )
	{
		this( bn, (LearningDataOld)null, dynamator, threshold, maxIterations, withBias, pm, ll );
		this.data2 = data2;
	}

	/** @since 20060719 */
	public Thread start(){
		ThreadGroup group = EMThread.this.ll.getThreadGroup();
		if( group == null ) group = Thread.currentThread().getThreadGroup();
		Thread ret = new Thread( group, (Runnable) EMThread.this, "em learn " + Integer.toString( INT_THREAD_COUNTER++ ) );
		ret.start();
		return ret;
	}

	private BeliefNetwork bn = null;
	private LearningDataOld data = null;
	private LearningData data2 = null;
	protected Dynamator dynamator = null;
	private double threshold = (double)0;
	private int maxIterations = (int)0;
	private boolean flagWithBias = false;
	private javax.swing.ProgressMonitor pm = null;
	private LearningListener ll = null;
	private int counter = (int)0;

	private static int INT_THREAD_COUNTER = 0;

	public void run()
	{
		BeliefNetwork new_bn = null;
		try{
			new_bn = bn;

			double previous, current = 0.0;

			for (counter = 1; counter <= maxIterations; ++counter)
			{
				SwingUtilities.invokeLater( new Runnable(){
					public void run()
					{
						pm.setProgress(counter);
						pm.setNote( "learning, iteration " + String.valueOf( counter ) );
					}
				});

				previous = current;
				if( this.data == null ) new_bn = Learning.learnParamsEM( new_bn, data2, dynamator, flagWithBias );
				else new_bn = Learning.learnParamsEM( new_bn, data, dynamator, flagWithBias );
				current = Learning.getLastLikelihood();
				Definitions.STREAM_TEST.println(counter+") "+current);
				if( Double.isNaN( current ) ) break;
				if (previous != 0.0) {
					if ((previous - current) / previous < threshold)
						break;
				}
			}
		}catch( Exception e ){
			ll.handleLearningError( "EM Learning failed. " + e.toString() );
			if( Definitions.DEBUG )
			{
				Definitions.STREAM_VERBOSE.println( Definitions.STR_VERBOSE_TRACE_MESSAGE );
				e.printStackTrace();
			}
			return;
		}

		if( new_bn.checkValidProbabilities() ) ll.setBeliefNetwork( new_bn );
		else ll.handleLearningError( "EM Learning failed.  Generated probabilities out of range." );
	}
}
