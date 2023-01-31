package edu.ucla.belief.decision;

import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.StateNotFoundException;
import java.util.*;

/** @author Keith Cascio
	@since 120804 */
public class DecisionLeaf extends DecisionNodeAbstract implements DecisionNode
{
	public DecisionLeaf( FiniteVariable joint, Factory factory )
	{
		this( joint, new Parameter[ joint.size() ], factory );
		makeUniform();
	}

	public DecisionLeaf( FiniteVariable joint, Parameter[] params, Factory factory )
	{
		this.myJoint = joint;
		this.myParameters = params;
		this.myFactory = factory;
		setID( "leaf[ " + myJoint.toString() + "]" );
	}

	/** @since 011105 */
	public DecisionLeaf( DecisionLeaf toCopy ){
		super( toCopy );
		this.myJoint = toCopy.myJoint;
		this.myFactory = toCopy.myFactory;
		if( toCopy.myParameters != null )
			this.myParameters = (Parameter[]) toCopy.myParameters.clone();
	}

	/** @since 011105 */
	public Object clone(){
		return new DecisionLeaf( (DecisionLeaf)this );
	}

	/** @since 011105 */
	public DecisionNode deepClone( Factory factory ){
		DecisionLeaf ret = (DecisionLeaf) factory.clone( this );
		for( int i=0; i<ret.myParameters.length; i++ ) ret.myParameters[i] = factory.clone( ret.myParameters[i] );
		return ret;
	}

	/** @since 011005 */
	public DecisionBackup deflate(){
		DecisionBackup ret = super.deflate();
		ret.deflateParameters( myParameters, (Map)null );
		return ret;
	}

	/** @since 011005 */
	public DecisionBackup deflate( Map alreadydeflated ){
		DecisionBackup ret = super.deflate( alreadydeflated );
		ret.deflateParameters( myParameters, alreadydeflated );
		return ret;
	}

	/** @since 020205 */
	protected boolean isDeeplyEquivalentHook( DecisionNode node, double epsilon, Map checked )
	{
		DecisionLeaf other = (DecisionLeaf) node;
		if( this.myJoint != other.myJoint ) return false;
		if( this.myParameters.length != other.myParameters.length ) return false;

		for( int i=0; i<this.myParameters.length; i++ ){
			//if( this.myParameters[i].getValue() != other.myParameters[i].getValue() ) return false;
			if( !Optimizer.epsilonEquals( this.myParameters[i].getValue(), other.myParameters[i].getValue(), epsilon ) ) return false;
		}

		return true;
	}

	public boolean equivales( DecisionNode node ){
		DecisionLeaf other = (DecisionLeaf) node;
		return Arrays.equals( this.myParameters, other.myParameters ) && (this.myJoint == other.myJoint);
	}

	public int equivalenceHashCode(){
		return 31 * hashCode( this.myParameters ) + myJoint.hashCode();
	}

	public Parameter getParameter( int index ) {
		return myParameters[index];//.getValue();
	}

	public Parameter getParameter( Object value ) throws StateNotFoundException {
		int index = myJoint.index( value );
		if( index >= 0 ) return getParameter( index );
		else throw new StateNotFoundException( myJoint, value );
	}

	public void setParameter( int index, Parameter param ){
		myParameters[index] = param;
		fireDecisionEvent( new DecisionEvent( (DecisionNode)this, DecisionEvent.ASSIGNMENT_CHANGE ) );
	}

	public void setParameter( Object value, Parameter param ) throws StateNotFoundException {
		int index = myJoint.index( value );
		if( index >= 0 ) setParameter( index, param );
		else throw new StateNotFoundException( myJoint, value );
	}

	public void makeUniform(){
		Parameter param = myFactory.newParameter( ((double)1)/((double)myJoint.size()) );
		Arrays.fill( myParameters, param );
	}

	public FiniteVariable getVariable() { return myJoint; }

	/** @since 020105 */
	public int numOutcomes(){
		return getOutcomes( new HashSet( myParameters.length ) ).size();
	}

	/** @since 010905 */
	public Set getOutcomes( Set container ){
		if( container == null ) container = new HashSet( myParameters.length );
		for( int i=0; i<myParameters.length; i++ ) container.add( myParameters[i] );
		return container;
	}

	/** @since 011605 */
	public boolean hasOutcome( Object outcome ){
		for( int i=0; i<myParameters.length; i++ ) if( myParameters[i] == outcome ) return true;
		return false;
	}

	/** @since 011405 */
	public void copyParametersInto( double[] data, int start ){
		for( int i=0; i<myParameters.length; i++ ) data[start+i] = myParameters[i].getValue();
	}

	public static final double DOUBLE_NORMALIZE_EPSILON = Double.MIN_VALUE;

	/** @since 011505 */
	public boolean normalize( boolean makeDistinct ){
		double sum = this.sum();
		double one = (double)1;
		if( Optimizer.epsilonEquals( one, sum, DOUBLE_NORMALIZE_EPSILON ) ) return false;
		double inverse = one/sum;
		double newval;
		for( int i=0; i<myParameters.length; i++ ){
			newval = myParameters[i].getValue() * inverse;
			if( makeDistinct ) clone(i);
			myParameters[i].setValue( newval );
		}
		fireDecisionEvent( new DecisionEvent( (DecisionNode)this, DecisionEvent.ASSIGNMENT_CHANGE ) );
		return true;
	}

	/** @since 011505 */
	public double sum(){
		double sum = (double)0;
		for( int i=0; i<myParameters.length; i++ ) sum += myParameters[i].getValue();
		return sum;
	}

	/** @since 011505 */
	public double sum( Collection instances ) throws StateNotFoundException
	{
		double sum = (double)0;
		Object instance;
		FiniteVariable fVar = getVariable();
		for( Iterator it = instances.iterator(); it.hasNext(); ){
			instance = it.next();
			//if( !fVar.contains( instance ) ) throw new IllegalArgumentException( "DecisionLeaf " + toString() + " variable " + fVar.getID() + " does not contain instance " + instance );
			sum += getParameter( instance ).getValue();
		}
		return sum;
	}

	/** @since 011505 */
	public void complement( Collection instances, boolean makeDistinct ) throws StateNotFoundException
	{
		if( (instances == null) || (instances.isEmpty()) ) return;
		List allinstances = getVariable().instances();
		Set remainder = new HashSet( allinstances );
		if( !remainder.containsAll( instances ) ) throw new IllegalArgumentException( "remainder: " + remainder + " -!contain- tocomplement: " + instances );
		//boolean normalized = normalize( false );
		remainder.removeAll( instances );
		if( remainder.isEmpty() ) return;
		double sumremainder = sum( remainder );
		double one = (double)1;
		if( sumremainder > one ) throw new IllegalStateException( "Sum of remaining parameters > 1" );
		double newval = (one - sumremainder) / ((double)instances.size());
		FiniteVariable fVar = getVariable();
		for( int i=0; i<myParameters.length; i++ ){
			if( instances.contains( fVar.instance(i) ) ){
				if( makeDistinct ) clone(i);
				myParameters[i].setValue( newval );
			}
			//else if( makeDistinct && normalized ) clone(i);
		}
		fireDecisionEvent( new DecisionEvent( (DecisionNode)this, DecisionEvent.ASSIGNMENT_CHANGE ) );
	}

	/** @since 011505 */
	public void clone( int index ){
		myParameters[index] = myFactory.clone( myParameters[index] );
	}

	public static int hashCode( Object a[] )
	{
		if( a == null ) return 0;

		int result = 1;

		//for( Object element : a )
		//	result = 31 * result + (element == null ? 0 : element.hashCode());
		for( int i=0; i<a.length; i++ )
			result = 31 * result + (a[i] == null ? 0 : a[i].hashCode());

		return result;
	}

	private Parameter[] myParameters;
	private FiniteVariable myJoint;
	private Factory myFactory;
}
