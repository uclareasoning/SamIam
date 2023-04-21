package edu.ucla.belief;

public class VariableInstance implements Comparable, Cloneable
{
	public VariableInstance(FiniteVariable myVariable, Object instance)
	{
		this.myVariable = myVariable;
		this.instance = instance;
	}

	public VariableInstance(FiniteVariable myVariable, int index)
	{
		this( myVariable, myVariable.instance( index ) );
	}

	/** @since 012004 */
	public Object clone(){
		return new VariableInstance( this.myVariable, this.instance );
	}

	/** @since 20070425
		not thread safe */
	public String toString(){
		BUFFER.setLength( 0 );
		BUFFER.append( myVariable );
		if( this.instance != null ) BUFFER.append( " = " ).append( instance );
		return BUFFER.toString();
	}
	static private final StringBuffer BUFFER = new StringBuffer( 0x40 );

	/** @since 012004 */
	public int compareTo( Object o )
	{
		if( !(o instanceof VariableInstance) ) return toString().compareToIgnoreCase( o.toString() );

		VariableInstance variableInstance = (VariableInstance)o;
		if( getVariable().equals( variableInstance.getVariable() ) ){
			return getIndex() - variableInstance.getIndex();
		}
		else return theVariableComparator.compare( getVariable(), variableInstance.getVariable() );
	}

	/** @since 012004 */
	public int hashCode(){
		int ret = 0;
		if( myVariable != null ) ret += myVariable.hashCode();
		if( instance != null ) ret += instance.hashCode();
		return ret;
	}

	/** @since 012004 */
	public boolean equals( Object obj )
	{
		FiniteVariable myVar = this.getVariable();

		if( obj instanceof VariableInstance )
		{
			VariableInstance him = (VariableInstance)obj;
			FiniteVariable hisVar = him.getVariable();
			Object myInst = this.getInstance();
			Object hisInst = him.getInstance();
			return ( (myVar==hisVar) || ((myVar!=null) && myVar.equals( hisVar )) ) && ( (myInst==hisInst) || ((myInst!=null) && myInst.equals( hisInst )) );
		}
		else if( myVar.equals( obj ) ) return true;
		else return false;
	}

	public FiniteVariable getVariable() {
		return myVariable;
	}

	public Object getInstance() {
		return instance;
	}

	/** @since 100803 */
	public void setInstance( Object o ){
		instance = o;
	}

	/** @since 012004 */
	public void setData( FiniteVariable myVariable, Object instance )
	{
		this.myVariable = myVariable;
		this.instance = instance;
	}

	public int getIndex() {
		return myVariable.index(instance);
	}

	public boolean isValid() {
		return myVariable.contains(instance);
	}

	public static final VariableComparator theVariableComparator = VariableComparator.getInstance();

	private FiniteVariable myVariable;
	private Object instance;
}
