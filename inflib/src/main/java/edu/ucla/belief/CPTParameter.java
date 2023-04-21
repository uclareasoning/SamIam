package edu.ucla.belief;

import edu.ucla.util.VariableStringifier;

import java.util.*;

/** Modified by Hei Chan 20030610 */
public class CPTParameter implements Comparable
{
	protected CPTShell shell;
	protected int[] mindex;
	protected int indexLinear;
	protected VariableInstance[] parentInstances;
	protected VariableInstance varInstance;

	public CPTParameter( CPTShell shell, int[] mindex )
	{
		this.shell = shell;
		this.mindex = (int[])mindex.clone();
		List vars = shell.index().variables();
		parentInstances = new VariableInstance[vars.size()-1];
		for (int i = 0; i < parentInstances.length; i++)
			parentInstances[i] = new VariableInstance(
				(FiniteVariable)vars.get(i), mindex[i]);
		varInstance = new VariableInstance(
			(FiniteVariable)vars.get(parentInstances.length),
			mindex[parentInstances.length]);
		this.indexLinear = shell.index().index( this.mindex );
	}

	public CPTParameter( CPTShell shell, int weightIndex, int index, VariableInstance parentInstance )
	{
		this.shell           = shell;
		this.mindex          = new int[] { weightIndex };
		this.parentInstances = new VariableInstance[] { parentInstance };
		this.varInstance     = new VariableInstance( shell.getVariable(), index );
		this.indexLinear     = weightIndex;
	}

	/** @since 20060208 */
	public CPTParameter( FiniteVariable var, int index ){
		this.shell = var.getCPTShell();
		this.indexLinear = index;
		this.mindex = this.shell.index().mindex( index, (int[])null );
		this.varInstance = new VariableInstance( var, mindex[ mindex.length - 1 ] );
	}

	/** @since 20060208 */
	private void initVariableInstances(){
		List vars = shell.index().variables();
		parentInstances = new VariableInstance[vars.size()-1];
		for( int i = 0; i < parentInstances.length; i++ ) parentInstances[i] = new VariableInstance( (FiniteVariable)vars.get(i), mindex[i] );
		//varInstance = new VariableInstance( (FiniteVariable)vars.get(parentInstances.length),mindex[parentInstances.length]);
	}

	/** @since 20060208 */
	public boolean equals( Object o ){
		if( !(o instanceof CPTParameter) ) return false;

		CPTParameter other = (CPTParameter)o;
		return (this.getVariable() == other.getVariable()) && (this.indexLinear == other.indexLinear);
	}

	/** @since 20060208 */
	public int hashCode(){
		return this.getVariable().hashCode() + this.indexLinear;
	}

	public int compareTo(Object o)
	{
		//if(getClass() != o.getClass())
		if( !(o instanceof CPTParameter) ) return toString().compareToIgnoreCase( o.toString() );

		CPTParameter cptParameter = (CPTParameter)o;

		if( getVariable() != cptParameter.getVariable() )
		{
			return VariableComparator.getInstance().compare(getVariable(), cptParameter.getVariable());
		}

		for (int i = 0; i < parentInstances.length; i++)
		{
			int indexCompare = new
				Integer(parentInstances[i].getIndex()).
				compareTo(new
				Integer(cptParameter.parentInstances[i].
				getIndex()));
			if (indexCompare != 0) return indexCompare;
		}

		return new Integer(varInstance.getIndex()).compareTo(
			new Integer(cptParameter.varInstance.getIndex()));
	}

	/** @since 20060208 */
	public double getValue(){
		return this.shell.getCP( this.indexLinear );
	}

	/** @since 20060208 */
	public int getIndexOfCondition(){
		return this.indexLinear - getIndexWithinCondition();
	}

	/** @since 20060227 */
	public int getIndexWithinCondition(){
		return this.indexLinear % getVariable().size();
	}

	/** @since 20060208 */
	public int getLinearIndex(){
		return this.indexLinear;
	}

	public CPTShell getCPTShell() {
		return shell;
	}

	public int[] getIndices() {
		return mindex;
	}

	public FiniteVariable getVariable() {
		return varInstance.getVariable();
	}

	/** @author Keith Cascio
		@since 20031008 */
	public VariableInstance getJointInstance()
	{
		return varInstance;
	}

	/** @author Keith Cascio
		@since 20031008 */
	public VariableInstance[] getParentInstances()
	{
		return parentInstances;
	}

	public String toString()
	{
		if( parentInstances == null ) initVariableInstances();
		String s = "Pr( ";
		s += varInstance.toString();
		if (parentInstances.length > 0)
			s += " | ";
		for (int i = 0; i < parentInstances.length; i++) {
			if (parentInstances[i] == null)
				s += "LEAK";
			else
				s += parentInstances[i].toString();
			if (i < parentInstances.length - 1)
				s += " , ";
		}
		return s += " )";
	}

	/** @since 20060209 */
	public String toString( VariableStringifier stringifier ){
		return append( new StringBuffer( 256 ), stringifier ).toString();
	}

	/** @since 20060209 */
	public StringBuffer append( StringBuffer buff, VariableStringifier stringifier ){
		if( parentInstances == null ) initVariableInstances();
		buff.append( "p(" );
		buff.append( stringifier.variableToString( varInstance.getVariable() ) );
		buff.append( "=" );
		buff.append( varInstance.getInstance() );
		if( parentInstances.length > 0 ) buff.append( " | " );
		for( int i = 0; i < parentInstances.length; i++ ){
			if( parentInstances[i] == null ) buff.append( "LEAK" );
			else{
				buff.append( stringifier.variableToString( parentInstances[i].getVariable() ) );
				buff.append( "=" );
				buff.append( parentInstances[i].getInstance() );
			}
			if( i < parentInstances.length - 1 ) buff.append( ", " );
		}
		buff.append( ")" );
		return buff;
	}
}
