package edu.ucla.belief.inference;

import edu.ucla.belief.*;

import java.util.*;

/** An inference engine that does no computation,
	instead returns random answers,
	for the purpose of testing.

	@author Keith Cascio
	@since 20060201 */
public class RandomInferenceEngine extends AbstractInferenceEngine implements PartialDerivativeEngine
{
	public RandomInferenceEngine( BeliefNetwork bn, Dynamator dyn ){
		super( dyn );
		this.myBeliefNetwork = bn;
		this.init();
	}

	private void init(){
		int numVariables = 10;
		if( this.myBeliefNetwork != null ){
			this.myEvidenceController = this.myBeliefNetwork.getEvidenceController();
			this.myEvidenceController.addPriorityEvidenceChangeListener( this );
			numVariables = this.myBeliefNetwork.size();
		}
		this.myMapIndices      = new HashMap/*<FiniteVariable,TableIndex>*/( numVariables );
		this.myMapJoints       = new HashMap/*<FiniteVariable,Table>     */( numVariables );
		this.myMapConditionals = new HashMap/*<FiniteVariable,Table>     */( numVariables );
	}

	/** @since 20060321 */
	public void die(){
		super.die();

		if( myMapConditionals       != null ) myMapConditionals.clear();
		if( myMapFamilyConditionals != null ) myMapFamilyConditionals.clear();
		if( myMapFamilyJoints       != null ) myMapFamilyJoints.clear();
		if( myMapIndices            != null ) myMapIndices.clear();
		if( myMapJoints             != null ) myMapJoints.clear();

		RandomInferenceEngine.this.myBeliefNetwork         = null;
		RandomInferenceEngine.this.myEvidenceController    = null;
		RandomInferenceEngine.this.myMapConditionals       = null;
		RandomInferenceEngine.this.myMapFamilyConditionals = null;
		RandomInferenceEngine.this.myMapFamilyJoints       = null;
		RandomInferenceEngine.this.myMapIndices            = null;
		RandomInferenceEngine.this.myMapJoints             = null;
	}

	public void printInfoCompilation( java.io.PrintWriter stream ){
		stream.println( "RandomInferenceEngine, no compilation necessary" );
	}

	public void printInfoPropagation( java.io.PrintWriter stream ){
		stream.println( "RandomInferenceEngine, no propagation necessary" );
	}

	public void printTables( java.io.PrintWriter out ){
		Collection collection = myMapJoints.values();
		Table[] array = new Table[ collection.size() ];
		int i = 0;
		for( Iterator it = collection.iterator(); it.hasNext(); ){
			array[i++] = (Table) it.next();
		}
		printTables( array, out );
	}

	public InferenceEngine handledClone( QuantitativeDependencyHandler handler ){
		throw new UnsupportedOperationException();
	}

	public void warning( EvidenceChangeEvent ece ){}
	public void evidenceChanged( EvidenceChangeEvent ece ){
		clearCache();
	}
	public void setCPT( FiniteVariable var ){
		clearCache();
	}

	public double probability(){
		return myRandom.nextDouble();
	}

	public Table joint( FiniteVariable var ){
		return jointTable( var );
	}

	public Table conditional( FiniteVariable var ){
		Table ret;
		if( myMapConditionals.containsKey( var ) ) ret = (Table) myMapConditionals.get( var );
		else{
			ret = Table.normalize( joint( var ) );
			myMapConditionals.put( var, ret );
		}
		return ret;
		//return jointTable( var );
	}

	public Table familyJoint( FiniteVariable var ){
		return randomFamilyTable( var );
	}

	public Table familyConditional( FiniteVariable var ){
		Table ret;
		Map map = getMapFamilyConditionals();
		if( map.containsKey( var ) ) ret = (Table) map.get( var );
		else{
			ret = Table.normalize( familyJoint( var ) );
			map.put( var, ret );
		}
		return ret;
		//return randomFamilyTable( var );
	}

	public Table partial( FiniteVariable var ){
		return jointTable( var );
	}

	public Table familyPartial( FiniteVariable var ){
		return randomFamilyTable( var );
	}

	public Set variables(){
		if( (myBeliefNetwork == null) || (myBeliefNetwork.isEmpty()) ) return Collections.EMPTY_SET;
		return new HashSet( myBeliefNetwork );
	}

	public boolean isExhaustive(){
		return true;
	}

	private void clearCache(){
		if( myMapJoints             != null ) myMapJoints.clear();
		if( myMapFamilyJoints       != null ) myMapFamilyJoints.clear();
		if( myMapConditionals       != null ) myMapConditionals.clear();
		if( myMapFamilyConditionals != null ) myMapFamilyConditionals.clear();
	}

	private Table jointTable( FiniteVariable var ){
		Table ret;
		if( myMapJoints.containsKey( var ) ) ret = (Table) myMapJoints.get( var );
		else{
			ret = newTable( var );
			myMapJoints.put( var, ret );
		}
		return ret;
	}

	private Table newTable( FiniteVariable var ){
		double[] values = null;
		if( myEvidenceController != null ){
			Object value = myEvidenceController.getValue( var );
			if( value != null ){
				values = indicatorValues( var.size(), var.index( value ) );
			}
		}
		if( values == null ) values = randomValues( var );
		return new Table( index( var ), values );
	}

	private TableIndex index( FiniteVariable var ){
		TableIndex ret;
		if( myMapIndices.containsKey( var ) ) ret = (TableIndex) myMapIndices.get( var );
		else{
			ret = new TableIndex( Collections.singleton( var ) );
			myMapIndices.put( var, ret );
		}
		return ret;
	}

	private Table randomFamilyTable( FiniteVariable var ){
		Table ret;
		Map map = getMapFamilyJoints();
		if( map.containsKey( var ) ) ret = (Table) map.get( var );
		else{
			TableIndex index = var.getCPTShell().index();
			ret = new Table( index, randomValues( index.size() ) );
			map.put( var, ret );
		}
		return ret;
	}

	private Map getMapFamilyJoints(){
		if( myMapFamilyJoints == null ){
			int numVariables = 10;
			if( myBeliefNetwork != null ) numVariables = myBeliefNetwork.size();
			myMapFamilyJoints = new HashMap/*<FiniteVariable,Table>*/( numVariables );
		}
		return myMapFamilyJoints;
	}

	private Map getMapFamilyConditionals(){
		if( myMapFamilyConditionals == null ){
			int numVariables = 10;
			if( myBeliefNetwork != null ) numVariables = myBeliefNetwork.size();
			myMapFamilyConditionals = new HashMap/*<FiniteVariable,Table>*/( numVariables );
		}
		return myMapFamilyConditionals;
	}

	private double[] randomValues( FiniteVariable var ){
		return randomValues( var.size() );
	}

	private double[] randomValues( int size ){
		double[] ret = new double[ size ];
		for( int i=0; i<size; i++ ) ret[i] = myRandom.nextDouble();
		return ret;
	}

	private double[] indicatorValues( int size, int index ){
		double[] ret = new double[ size ];
		for( int i=0; i<size; i++ ){
			ret[i] = (i==index) ? (double)1 : (double)0;
		}
		return ret;
	}

	private BeliefNetwork myBeliefNetwork;
	private EvidenceController myEvidenceController;
	private Map/*<FiniteVariable,TableIndex>*/ myMapIndices;
	private Map/*<FiniteVariable,Table>*/ myMapJoints;
	private Map/*<FiniteVariable,Table>*/ myMapConditionals;
	private Map/*<FiniteVariable,Table>*/ myMapFamilyJoints;
	private Map/*<FiniteVariable,Table>*/ myMapFamilyConditionals;
}
