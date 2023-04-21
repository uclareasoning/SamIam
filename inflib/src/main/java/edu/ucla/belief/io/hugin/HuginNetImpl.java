package edu.ucla.belief.io.hugin;

//{superfluous} import java.awt.Point;
import java.awt.Dimension;
import java.util.*;

import edu.ucla.belief.*;
import edu.ucla.belief.io.*;
//{superfluous} import edu.ucla.structure.DirectedGraph;
//{superfluous} import edu.ucla.util.EnumProperty;
//{superfluous} import edu.ucla.util.EnumValue;
//{superfluous} import edu.ucla.util.UserEnumProperty;
//{superfluous} import edu.ucla.util.UserEnumValue;

public class HuginNetImpl extends BeliefNetworkImpl implements HuginNet
{
	public static boolean FLAG_DEBUG = Definitions.DEBUG;

	private HuginFileVersion myHuginFileVersion = HuginFileVersion.V57;//HuginFileVersion.UNSPECIFIED;

	/** @since 20080219 */
	public FiniteVariable newFiniteVariable( Map properties ){
		properties = minimalProperties( properties );
		return new HuginNodeImpl( (String) properties.get( KEY_HUGIN_ID ), (List) properties.get( KEY_HUGIN_STATES ), properties );
	}

	public boolean mayContain( Object obj )
	{
		return obj instanceof HuginNode;
	}

	/**
	* Sets the net parameters to the name value pairs contained in params.
	*/
	public void setParams( Map inputParams )
	{
		this.getProperties().clear();
		this.getProperties().putAll( inputParams );

		setNetDimension();
		makeUserEnumProperties( this.getProperties() );

		//this.params = Collections.unmodifiableMap( this.params );
		//System.out.println( "(HuginNetImpl)"+System.identityHashCode(this)+".(unmodifiableMap)"+System.identityHashCode(this.params)+": " + this.params );
	}

	/** @since 012204 */
	public HuginFileVersion getVersion(){
		return myHuginFileVersion;
	}

	/** @since 012204 */
	public void setVersion( HuginFileVersion version ){
		myHuginFileVersion = version;
	}

	public HuginNetImpl()
	{
		super( true );
	}

	/** @since 20060519 */
	public HuginNetImpl( boolean construct ){
		super( construct );
	}

	public HuginNetImpl( Map mapVariablesToPotentials )
	{
		super( mapVariablesToPotentials );
	}

	protected HuginNetImpl( HuginNetImpl toCopy )
	{
		super( toCopy );
		BeliefNetworkImpl.putAll( toCopy.getProperties(), this.getProperties() );//this.getProperties().putAll( toCopy.getProperties() );
	}

	/**
		@author Keith Cascio
		@since 020603
	*/
	public HuginNetImpl( BeliefNetworkImpl toUpgrade )
	{
		super( toUpgrade );

		Map variablesOldToNew = new HashMap( size() );

		FiniteVariable next;
		HuginNode replacement;
		for( Iterator it = iterator(); it.hasNext(); )
		{
			next = (FiniteVariable) it.next();
			replacement = new HuginNodeImpl( next );
			variablesOldToNew.put( next, replacement );
		}

		if( !variablesOldToNew.isEmpty() ) replaceVariables( variablesOldToNew );

		cloneAllCPTShells();
	}

	/** @since 021804 */
	public Copier getCopier()
	{
		return HuginCopier.getInstance();
	}

	public BeliefNetwork seededClone( Map variablesOldToNew )
	{
		if( FLAG_DEBUG ) Definitions.STREAM_VERBOSE.println( "HuginNetImpl.seededClone()" );
		if( FLAG_DEBUG ) Definitions.STREAM_VERBOSE.println( "\n\nHuginNetImpl(copy)" );
		HuginNetImpl ret = new HuginNetImpl( this );
		ret.replaceVariables( variablesOldToNew );
		return ret;
	}

	protected void setNetDimension()
	{
		Dimension netDim = getGlobalNodeSize( null );

		if( netDim != null )
		{
			Dimension tempDim = new Dimension();

			HuginNode node = null;
			for( Iterator it = iterator(); it.hasNext(); )
			{
				node = (HuginNode) it.next();
				node.getDimension( tempDim );
				if( !node.isSpecifiedDimension() )
				{
					node.setDimension( netDim );
				}
			}
		}
	}

	/**
		@author Keith Cascio
		@since 061002
	*/
	public String toString()
	{
		return "One HuginNetImpl";//super.toString().substring( 19 );
	}

	/*
	public void add( HuginNode var ){
		super.addVariable( var );
	}

	public void remove( HuginNode var ){
		super.removeVariable( var );
	}*/


	/**
	* Will return the node size listed in the HuginNetImpl file.  If it does not
	* exist, it will return a null.
	*
	* This function looks for a node size from the network.
	*/
	public Dimension getGlobalNodeSize( Dimension dim )
	{
		if( dim == null ){ dim = new Dimension(); }

		Object   value = this.getProperties().get( PropertySuperintendent.KEY_HUGIN_NODE_SIZE );
		if(      value instanceof Dimension ){ dim.setSize( (Dimension) value ); }
		else if( value instanceof List      ){
			//will return a list of 2 floats [x, y]
			List valList = (List) value;
			if( valList.size() >= 2 ){
				dim.width  = ((Number) valList.get(0)).intValue();
				dim.height = ((Number) valList.get(1)).intValue();
			}
		}
		return dim;
	}
}
