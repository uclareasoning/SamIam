package edu.ucla.belief.ui.networkdisplay;

import edu.ucla.belief.ui.displayable.DisplayableFiniteVariable;
import edu.ucla.belief.ui.preference.*;

/** @author keith cascio
	@since 20040813 */
public abstract class IconFactory
{
	public NodeIcon makeIcon( DisplayableFiniteVariable dVar ){
		return makeIcon( dVar, null );
	}

	abstract public NodeIcon makeIcon( DisplayableFiniteVariable dVar, SamiamPreferences netPrefs );
	abstract public boolean corresponds( NodeIcon icon );

	private IconFactory( String name ){
		this.myName = name;
	}

	public String toString(){
		return myName;
	}

	public static final IconFactory OVAL = new IconFactory( "oval" )
	{
		public NodeIcon makeIcon( DisplayableFiniteVariable dVar, SamiamPreferences netPrefs ){
			return new NodeIconOval( dVar );//, netPrefs );
		}

		public boolean corresponds( NodeIcon icon ){
			return icon instanceof NodeIconOval;
		}
	};

	public static final IconFactory SQUARE = new IconFactory( "square" )
	{
		public NodeIcon makeIcon( DisplayableFiniteVariable dVar, SamiamPreferences netPrefs ){
			return new NodeIconSquare( dVar );//, netPrefs );
		}

		public boolean corresponds( NodeIcon icon ){
			return icon instanceof NodeIconSquare;
		}
	};

	public static final IconFactory DEFAULT = OVAL;
	public static final IconFactory[] ARRAY = new IconFactory[] { OVAL, SQUARE };

	public static IconFactory forString( String token )
	{
		for( int i=0; i<ARRAY.length; i++ ){
			if( ARRAY[i].toString().equals( token ) ) return ARRAY[i];
		}
		return (IconFactory)null;
	}

	private String myName;
}
