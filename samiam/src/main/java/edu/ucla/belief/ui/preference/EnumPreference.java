package edu.ucla.belief.ui.preference;

import edu.ucla.belief.ui.actionsandmodes.GrepAction;
import edu.ucla.belief.ui.dialogs.ProbabilityRewrite;

import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import java.util.regex.*;

/** A preference based on a java 5 tiger typesafe enum
	@author keith cascio
	@since  20070416 */
public class EnumPreference<E extends Enum<E>> extends ObjectPreference implements ListSelectionListener
{
	static public class Autosuggest extends EnumPreference<GrepAction.Autosuggest>{
		public Autosuggest( String key, String name ){
			super( key, name, GrepAction.Autosuggest.class, GrepAction.Autosuggest.ALL );

			this.setBoundsInclusive( new int[]{ 0, GrepAction.Autosuggest.ALL.size() } );
		}

		public Autosuggest( String key, String name, String toParse ){
			this( key, name );
			try{ this.setValue( this.parseValue( toParse ) ); }
			catch( Exception exception ){
				System.err.println( "warning: EnumPreference.Autosuggest<init> caught " + exception );
			}
		}
	}

	/** @since 20071226 */
	static public class Caste extends EnumPreference<ProbabilityRewrite.Caste>{
		public Caste( String key, String name ){
			super( key, name, ProbabilityRewrite.Caste.class, ProbabilityRewrite.Caste.publik(), EnumSet.of( ProbabilityRewrite.Caste.values()[0].getDefault() ) );
			this.setBoundsInclusive( EXACTLY_ONE );
		}

		public Caste( String key, String name, String toParse ){
			this( key, name );
			try{ this.setValue( this.parseValue( toParse ) ); }
			catch( Exception exception ){
				System.err.println( "warning: EnumPreference.Caste<init> caught " + exception );
			}
		}
	}

	public EnumPreference( String key, String name, Class<E> clazz, Set<E> defaultt ){
		this( key, name, clazz, clazz.getEnumConstants(), defaultt );
	}

	/** @since 20071226 */
	public EnumPreference( String key, String name, Class<E> clazz, E[] values, Set<E> defaultt ){
		super( key, name, defaultt, values, new EnumConverter<E>( clazz ) );
		myDefault   = defaultt;
		myClazz     = clazz;
		myConstants = values;
	}

	/** @since 20070421 */
	public int[] setBoundsInclusive( int[] bounds ){
		int[] old = myBoundsInclusive;
		myBoundsInclusive = bounds;
		return old;
	}

	/** @since 20070421 */
	public int[] getBoundsInclusive(){
		return myBoundsInclusive;
	}

	/** interface ListSelectionListener
		@since 20070421 */
	public void valueChanged( ListSelectionEvent e ){
		if( ignore ) return;

		Throwable caught = null;
		try{
			ignore = true;
			ListSelectionModel lsm = myJList.getSelectionModel();
			Integer            index;
			for( int i = e.getFirstIndex(); i <= e.getLastIndex(); i++ ){
				index = new Integer(i);
				if( lsm.isSelectedIndex(i) ) { if(! myHistory.contains(index)) myHistory.addLast( index ); }
				else                         {                                 myHistory .remove( index ); }
			}
			int removeable = -1;
			while( myHistory.size() > myBoundsInclusive[1] ){
				lsm.removeSelectionInterval( removeable = myHistory.removeFirst(), removeable );
			}
		}catch( Exception exception ){ caught = exception;
		}finally{                      ignore = false; }

		try{
			this.actionPerformed( null );
		}catch( Exception exception ){ caught = exception; }

		if( caught != null ){
			System.err.println( "warning: EnumPreference.valueChanged() caught " + caught );
			System.err.println( caught.getStackTrace()[0] );
		}
	}

	/** @since 20070421 */
	public boolean useCombo(){
		return myBoundsInclusive[0] == 1 && myBoundsInclusive[1] == 1;
	}

	/** @since 20070421 */
	protected JComponent getEditComponentHook(){
		if( useCombo() ) return super.getEditComponentHook();

		if( myJList == null ){
			myJList = new JList( myDomain );
			getRenderer();
			myJList.setCellRenderer( (ListCellRenderer) EnumPreference.this );
			hookSetEditComponentValue( getValue() );
			myJList.addListSelectionListener( this );
			myJList.setBorder( BorderFactory.createEtchedBorder() );
		}

		clearHistory();
		return myJList;
	}

	/** @since 20070422 */
	protected LinkedList<Integer> clearHistory(){
		if( myHistory == null ) myHistory = new LinkedList<Integer>();
		else                    myHistory.clear();
		return myHistory;
	}

	/** @since 20070421 */
	@SuppressWarnings( "unchecked" )
	public void hookSetEditComponentValue( Object newVal ){
		if( useCombo() ){
			if( newVal instanceof Collection ){
				Collection collection = (Collection) newVal;
				if( collection.isEmpty() ){ newVal = null; }
				else{ newVal = collection.iterator().next(); }
			}
			super.hookSetEditComponentValue( newVal );
		}
		if( myJList == null ) return;

		try{
			ignore = true;
			setListeningEnabled( false );

			clearHistory();
			if( newVal instanceof Set ){
				Set<E>             values = (Set<E>) newVal;
				ListSelectionModel lsm    = myJList.getSelectionModel();
				lsm.clearSelection();
				for( int i=0; i<myDomain.length; i++ ){
					if( values.contains( myConstants[i] ) ){
						lsm.addSelectionInterval( i,i );
						myHistory.addLast(i);
					}
				}
			}
			else{
				myJList.setSelectedValue( newVal, true );
				myHistory.addLast( indexOf( newVal ) );
			}
		}catch( Exception exception ){
			System.err.println( "warning: EnumPreference.hookSetEditComponentValue("+newVal+") caught " + exception );
		}finally{
			ignore = false;
			setListeningEnabled( true );
		}
	}

	/** @since 20070421 */
	@SuppressWarnings( "unchecked" )
	public Object getCurrentEditedValue(){
		Set<E>           ret    = EnumSet.noneOf( myClazz );
		if( useCombo() ) ret.add( (E) super.getCurrentEditedValue() );
		else{
			ListSelectionModel lsm = myJList.getSelectionModel();
			for( int i=0; i<myDomain.length; i++ ){
				if( lsm.isSelectedIndex( i ) ) ret.add( myConstants[i] );
			}
		}
		return ret;
	}

	public static class EnumConverter<F extends Enum<F>> implements DomainConverter
	{
		public EnumConverter( Class<F> clazz ){
			EnumConverter.this.myClazz = clazz;
		}

		@SuppressWarnings( "unchecked" )
		public Object getDisplayName( Object obj ){
			return obj.toString();//((F)obj).name();
		}

		public Object getValue( Object obj ){
			return obj;
		}

		public Object parseValue( String toParse ) throws Exception{
			Set<F>    ret    = EnumSet.noneOf( myClazz );
			if( toParse == null || toParse.length() < 1 ) return ret;
			Exception thrown = null;
			for( String token : PATTERN_DELIM.split( toParse ) ){
				try{
					if( token != null && token.length() > 0 ) ret.add( Enum.valueOf( myClazz, token ) );
				}catch( Exception exception ){
					System.err.println( "warning: EnumPreference.EnumConverter.parseValue() caught " + (thrown = exception) );
				}
			}
			return ret;
		}

		@SuppressWarnings( "unchecked" )
		public String valueToString( Object obj ){
			if( obj instanceof Collection ) return toString( (Collection<F>) obj );
			return ((F)obj).name();
		}

		/** @since 20070422 */
		public String toString( Collection<F> values ){
			StringBuilder builder = new StringBuilder( values.size()*0x10 );
			for( F value: values ) builder.append( value.name() ).append( CHAR_DELIM );
			if( builder.length() > 0 ) builder.setLength( builder.length() - 1 );
			return builder.toString();
		}

		private        Class<F> myClazz;
		public  static Pattern  PATTERN_DELIM = Pattern.compile( "\\W+" );
		public  static char        CHAR_DELIM = ',';
	}

	private Class<E>            myClazz;
	private   Set<E>            myDefault;
	private       E[]           myConstants;
	private int[]               myBoundsInclusive = EXACTLY_ONE;
	private JList               myJList;
	private boolean             ignore = false;
	private LinkedList<Integer> myHistory;

	private static final int[] EXACTLY_ONE = new int[]{ 1,1 };
}
