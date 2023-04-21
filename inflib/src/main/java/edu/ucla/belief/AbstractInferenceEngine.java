package edu.ucla.belief;

import edu.ucla.util.*;

import java.util.*;

/** @author keith cascio
	@since  20030121 */
public abstract class AbstractInferenceEngine
  implements
  InferenceEngine,
  ChangeListener
{
	/*     YOU MUST IMPLEMENT     /

	/     interface EvidenceChangeListener     /
	public void evidenceChanged( EvidenceChangeEvent ece );
	public void warning( EvidenceChangeEvent ece );

	/     interface InferenceEngine     /
	public Table conditional( FiniteVariable var );
	public Table joint( FiniteVariable var );
	public double probability();
	public void setCPT( FiniteVariable var );
	public Table familyJoint( FiniteVariable var );
	public Table familyConditional( FiniteVariable var );
	public Set variables();
	public boolean isExhaustive();
	public InferenceEngine handledClone( QuantitativeDependencyHandler handler );
	public void printTables( java.io.PrintWriter out );
	*/

	/** @since 20091226 */
	public InferenceEngine canonical(){ return this; }

	/** @since 20091226 */
	public int hashCode(){
		InferenceEngine canonical = this.canonical();
		if( canonical == this ){ return     super.hashCode(); }
		else{                    return canonical.hashCode(); }
	}

	/** @since 20091226 */
	public boolean equals( Object other ){
		if( other == null ){ return false; }
		if( other instanceof InferenceEngine ){ other = ((InferenceEngine) other).canonical(); }
		InferenceEngine canonical = this.canonical();
		if( canonical == this ){ return     super.equals( other ); }
		else{                    return canonical.equals( other ); }
	}

	/** @since 20091226 */
	public String  compilationStatus( edu.ucla.belief.io.PropertySuperintendent bn ){ return null; }

	/** @since 20091218 */
	public Collection/*<DirectedEdge>*/ notoriousEdges(){ return null; }

	/** In case this InferenceEngine wants to report two sets of answers,
		for example, approximate and exact.
		@since 20080226 */
	public Table[] conditionals( FiniteVariable var, Table[] buckets ){
		if( (buckets == null) || (buckets.length != 1) ){ buckets = new Table[1]; }
		buckets[0] = conditional( var );
		return buckets;
	}

	/** In case this InferenceEngine wants to report two sets of answers,
		for example, approximate and exact.
		@since 20080226 */
	public String[] describeConditionals(){
		return DESCRIBE_CONDITIONAL;
	}

	/** @since 20080227 */
	public java.awt.Component getControlPanel(){ return myControlPanel; }
	/** @since 20080227 */
	public InferenceEngine    setControlPanel( java.awt.Component panel ){
		this.myControlPanel = panel;
		return this;
	}

	/** @since 20080226 */
	public static final String[] DESCRIBE_CONDITIONAL = new String[]{ "probability" };

	/** @since 20080221 */
	public char probabilityDisplayOperatorUnicode(){ return '='; }

	public AbstractInferenceEngine( Dynamator dyn )
	{
		//System.out.println( "(AbstractInferenceEngine)" + this.getClass().getName() + "()" );
		setDynamator( dyn );
	}

	/** @since 20030306 */
	public int random( FiniteVariable var )
	{
		double random = myRandom.nextDouble();
		Table cond = conditional( var );
		//System.out.println( cond );
		int len = cond.getCPLength() - 1;
		double upperbound = (double)0;
		int i;
		for( i=0; i<len; i++ )
		{
			upperbound += cond.getCP( i );
			if( random <= upperbound ) return i;
		}
		return i;
	}
	protected static Random myRandom = new Random();

	/** @since 090704 */
	public void die(){
		setValid( false );
	}

	/** interface EvidenceChangeListener
		@since 090704 */
	//public boolean isDead(){ return myFlagValid; }

	/** @since 061304 */
	public double[] getEffectiveCPTData( FiniteVariable var )
	{
		CPTShell effective = null;
		if( isQuantitativeDependencyHandled() ) effective = getQuantitativeDependencyHandler().getCPTShell( var );
		else effective = var.getCPTShell( var.getDSLNodeType() );

		return effective.getCPT().dataclone();
	}

	/** @since 061304 */
	public void setQuantitativeDependencyHandler( QuantitativeDependencyHandler handler ){
		this.myQuantitativeDependencyHandler = handler;
	}

	/** @since 061304 */
	public QuantitativeDependencyHandler getQuantitativeDependencyHandler(){
		return this.myQuantitativeDependencyHandler;
	}

	/** @since 061304 */
	public boolean isQuantitativeDependencyHandled(){
		return this.myQuantitativeDependencyHandler != null;
	}

	private QuantitativeDependencyHandler myQuantitativeDependencyHandler;

	/** @since 20030925 *//*
	public void actionPerformed( ActionEvent evt )
	{
		//System.out.println( "(AbstractInferenceEngine)" + this.getClass().getName() + ".actionPerformed()" );
		setValid( false );
		if( evt != null ){
			ChangeBroadcaster cb = (ChangeBroadcaster) evt.getSource();
			if( cb != null ){ cb.removeChangeListener( this ); }
		}
	}*/

	/** interface ChangeListener
		@since 20081128 */
	public ChangeListener settingChanged( ChangeEvent event ){
		this.setValid( false );
		if( event != null ){
			ChangeBroadcaster cb = event.getSource();
			if( cb != null ){ cb.removeChangeListener( this ); }
		}
		return this;
	}

	protected          Dynamator myDynamator;
	protected            boolean myFlagValid = true;
	protected java.awt.Component myControlPanel;

	/** @since 061504 */
	public void printTables( Table[] tables, java.io.PrintWriter stream )
	{
		stream.println( this.getClass().getName() + " tables:" );
		if( tables == null ) return;
		for( int i=0; i<tables.length; i++ ){
			stream.println( tables[i].tableString() );
			stream.println( "=================================================" );
		}
	}

	/** @since 091803 */
	public void printInfoCompilation( java.io.PrintWriter out ) {}
	public void printInfoPropagation( java.io.PrintWriter out ) {}
	public static final String STR_CONSOLE_MESSAGE_COMP_TIME	= "Compilation Time (sec): ";
	public static final String STR_CONSOLE_MESSAGE_COMP_MEM		= "Memory used (Mb): ";
	public static final String STR_CONSOLE_MESSAGE_PROP_TIME	= "(Cumulative) Propagation Time (sec): ";

	/** @since 012103 */
	public void setDynamator( Dynamator dyn )
	{
		myDynamator = dyn;
		//if( dyn != null ) myDynamator.registerInferenceEngine( this );
	}

	/** @since 012103 */
	public Dynamator getDynamator()
	{
		return myDynamator;
	}

	/** @since 012103 */
	public void setValid( boolean flag )
	{
		myFlagValid = flag;
	}

	/** @since 012103 */
	public boolean getValid()
	{
		return myFlagValid;
	}

	/**
		@ret The maximum single conditional probability
		value <= 1, over all variables.
		@author Keith Cascio
		@since 071003
	*/
	public double max()
	{
		double max = Double.NEGATIVE_INFINITY;
		double localMax;
		Table conditional;
		for( Iterator it = variables().iterator(); it.hasNext(); )
		{
			conditional = conditional( (FiniteVariable)it.next() );
			localMax = conditional.max();
			if( localMax > max ) max = localMax;
		}
		return max;
	}

	/** @since 20050830 */
	public boolean probabilitySupported(){
		return true;
	}

	/** @since 20030819 */
	public static boolean test( InferenceEngine ie1, InferenceEngine ie2, EvidenceController controller, double epsilon, int numEvidences )
	{
		Definitions.STREAM_TEST.println( "\nAbstractInferenceEngine.test()" );

		double prEvidence1 = ie1.probability();
		double prEvidence2 = ie2.probability();

		if( Math.abs( prEvidence1 - prEvidence2 ) > epsilon )
		{
			Definitions.STREAM_TEST.println( "Pr(e): " + prEvidence1 + " != " + prEvidence2 );
			return false;
		}
		else Definitions.STREAM_TEST.println( "Pr(e): " + prEvidence1 + " == " + prEvidence2 );

		Set variables = ie1.variables();
		int numVariables = variables.size();

		Table marginal1;
		Table marginal2;
		FiniteVariable fVar;
		for( Iterator it = variables.iterator(); it.hasNext(); )
		{
			fVar = (FiniteVariable) it.next();
			marginal1 = ie1.conditional( fVar );
			marginal2 = ie2.conditional( fVar );
			if( !marginal1.epsilonEquals( marginal2, epsilon ) )
			{
				Definitions.STREAM_TEST.println( "inference engines disagree over marginal for: " + fVar );
				return false;
			}
		}

		Definitions.STREAM_TEST.println( "marginals agree" );

		Random random = new Random();

		int[] variableIndices = new int[ numEvidences ];
		for( int i=0; i<numEvidences; i++ )
		{
			variableIndices[i] = random.nextInt( numVariables );
		}
		FiniteVariable[] evidenceVariables = new FiniteVariable[ numEvidences ];
		int count = (int)0;
		for( Iterator it = variables.iterator(); it.hasNext(); )
		{
			fVar = (FiniteVariable) it.next();
			for( int j=0; j<numEvidences; j++ )
			{
				if( variableIndices[j] == count ) evidenceVariables[j] = fVar;
			}
			++count;
		}

		int randomInstanceIndex;
		Object randomInstance;
		Map toObserve = new HashMap();
		for( int i=0; i<numEvidences; i++ )
		{
			fVar = evidenceVariables[i];
			randomInstanceIndex = ie1.random( fVar );
			randomInstance = fVar.instance( randomInstanceIndex );
			toObserve.put( fVar, randomInstance );
		}

		Definitions.STREAM_TEST.println( "setting evidence: " + toObserve );

		try{
			controller.setObservations( toObserve );
		}catch( StateNotFoundException e ){
			System.err.println( "caught " + e );
		}

		for( Iterator it = variables.iterator(); it.hasNext(); )
		{
			fVar = (FiniteVariable) it.next();
			marginal1 = ie1.conditional( fVar );
			marginal2 = ie2.conditional( fVar );
			if( !marginal1.epsilonEquals( marginal2, epsilon ) )
			{
				Definitions.STREAM_TEST.println( "inference engines disagree over marginal for: " + fVar );
				return false;
			}
		}

		Definitions.STREAM_TEST.println( "marginals agree" );
		return true;
	}
}
