package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.*;
import edu.ucla.util.EnumValue;
import edu.ucla.util.Stringifier;
import edu.ucla.util.VariableStringifier;

import edu.ucla.belief.ui.util.Util;
import edu.ucla.belief.ui.actionsandmodes.Grepable;
import edu.ucla.belief.ui.actionsandmodes.Grepable.Filter;
import edu.ucla.belief.ui.actionsandmodes.Grepable.Simple;
import edu.ucla.belief.ui.internalframes.EnumTableModel.Struct;

import java.util.List;
import java.util.*;
import java.util.regex.*;

/** An enhanced version of EnumTableModel that uses features of java 5, java 6 and beyond.
	@author keith cascio
	@since  20030820 */
public class EnumTableModel5 extends EnumTableModel implements Grepable<Struct,Simple,EnumValue>
{
	@SuppressWarnings( "unchecked" )
	public EnumTableModel5( BeliefNetwork bn )
	{
		super( bn );
		myStructs = (List<Struct>) myData;
	}

	/** @since 20070324 */
	@SuppressWarnings( "unchecked" )
	public Collection<FiniteVariable> invertShown(){
		Collection<FiniteVariable> show = null;
		try{
			int total = myBeliefNetwork.size();
			int shown = myStructs.size();

			Collection<FiniteVariable> all  = (Collection<FiniteVariable>) myBeliefNetwork;

			if(      shown <  1     )  show = all;
			else if( shown == total )  show = Collections.emptySet();
			else{
				Set<FiniteVariable>  bucket = new HashSet<FiniteVariable>( all );
				for( Struct struct : myStructs ) bucket.remove( struct.variable );
				show = bucket;
			}

			setVariables( show );
		}catch( Exception exception ){
			System.err.println( "warning: EnumTableModel5.invertShown() caught " + exception );
			exception.printStackTrace();
		}
		return show;
	}

	/** @since 20070324 */
	public EnumTableModel5 rotateAllRows(){
		return rotateRows( null );
	}

	/** @since 20070324 */
	public EnumTableModel5 rotateRows( int[] selected ){
		try{
			if( selected != null ) for( int    i      : selected  ) myStructs.get( i ).rotate();
			else                   for( Struct struct : myStructs ) struct.rotate();
			this.fireTableDataChanged();
		}catch( Exception throwable ){
			System.err.println( "warning: EnumTableModel5.rotateRows() caught " + throwable );
		}
		return this;
	}

	/** @since 20070308 nasa */
	public EnumTableModel5 setRows( boolean flag, int[] selected ){
		try{
			this.setRows( myProperty.valueOf( flag ), selected );
		}catch( Exception exception ){
			System.err.println( "warning: EnumTableModel5.setRows() caught " + exception );
		}
		return this;
	}

	/** @since 20070310 */
	public EnumTableModel5 setRows( EnumValue value, int[] selected ){
		try{
			for( int i : selected ){
				myStructs.get( i ).setValue( value );
			}
			this.fireTableDataChanged();
		}catch( Exception throwable ){
			System.err.println( "warning: EnumTableModel5.setRows() caught " + throwable );
		}
		return this;
	}

	/** interface Grepable
		@since 20070324 */
	public Class<Simple> grepFields(){
		return Simple.class;
	}

	/** interface Grepable
		@since 20070324 */
	public EnumSet<Simple> grepFieldsDefault(){
		return Simple.SINGLETON;
	}

	/** interface Grepable
		@since 20070324 */
	public String grepInfo(){
		return STR_GREP_TIP;
	}

	public static final String STR_GREP_TIP = "set target value on matching rows";

	/** interface Grepable
		@since 20070309 nasa
		@since 20070324 */
	public long grep( Filter filter, EnumSet<Simple> field_selector, Stringifier stringifier, EnumValue valueTrue, Collection<Struct> results ){
		long matches = 0;
		try{
			Thread      thread       = Thread.currentThread();
			boolean     destructive  = filter.flags().contains( Flag.destructive );
			EnumValue   valueDefault = destructive ? valueTrue.property().getDefault() : null;

			for( Struct struct : myStructs ){
				if( thread.isInterrupted() ) break;
				if( filter.accept( stringifier.objectToString( struct.variable ) ) ){
					++matches;
					struct.setValue( valueTrue );
					if( results != null ) results.add( struct );
				}
				else if( destructive && (struct.getValue() == valueTrue) ) struct.setValue( valueDefault );
			}
			this.fireTableDataChanged();
		}catch( Exception exception ){
			System.err.println( "warning: EnumTableModel5.grep() caught " + exception );
			if( Util.DEBUG_VERBOSE ) exception.printStackTrace();
		}
		return matches;
	}

	/** @since 20070310 */
	public void copy( boolean flag, VariableStringifier stringifier ){
		try{
			this.copy( myProperty.valueOf( flag ), stringifier );
		}catch( Exception exception ){
			System.err.println( "warning: EnumTableModel5.copy() caught " + exception );
		}
	}

	/** @since 20070309 nasa */
	public void copy( EnumValue value, VariableStringifier stringifier ){
		try{
			StringBuilder buff   = new StringBuilder( myStructs.size() * 0x40 );
			for( Struct struct : myStructs ){
				if( struct.getValue() == value ){
					buff.append( stringifier.variableToString( struct.variable ) );
					buff.append( '\n' );
				}
			}
			Util.copyToSystemClipboard( buff.toString() );
		}catch( Exception exception ){
			System.err.println( "warning: EnumTableModel5.copy() caught " + exception );
		}
	}

	/** @since 20070309 nasa */
	public void paste( boolean flag, VariableStringifier stringifier ){
		try{
			this.paste( myProperty.valueOf( flag ), stringifier );
		}catch( Exception exception ){
			System.err.println( "warning: EnumTableModel5.paste() caught " + exception );
		}
	}

	/** @since 20070310 */
	public void paste( EnumValue value, VariableStringifier stringifier ){
		try{
			String   contents = Util.pasteFromSystemClipboard();
			if( contents == null || contents.length() < 1 ) return;

			String[]    splits   = contents.split( "\n" );
			Set<String> members  = new HashSet<String>( splits.length );
			for( String split : splits ) members.add( split );

			for( Struct struct : myStructs ){
				if( members.contains( stringifier.variableToString( struct.variable ) ) ) struct.setValue( value );
			}
			this.fireTableDataChanged();
		}catch( Exception exception ){
			System.err.println( "warning: EnumTableModel5.paste() caught " + exception );
		}
	}

	private List<Struct> myStructs;
}
