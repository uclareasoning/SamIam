package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.*;
import edu.ucla.util.EnumValue;
import edu.ucla.util.Stringifier;

import edu.ucla.belief.ui.actionsandmodes.*;
import edu.ucla.belief.ui.actionsandmodes.Grepable.Filter;
import edu.ucla.belief.ui.actionsandmodes.Grepable.Simple;
import edu.ucla.belief.ui.tree.Hierarchy;
import edu.ucla.belief.ui.toolbar.MainToolBar;
import edu.ucla.belief.ui.displayable.DisplayableBeliefNetwork;
import edu.ucla.belief.ui.NetworkInternalFrame;
import edu.ucla.belief.ui.internalframes.EnumTableModel.Struct;
import edu.ucla.belief.ui.internalframes.EnumPropertyEditPanel5.GrepEffect;

import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;
import java.util.*;
import java.util.regex.Matcher;
import java.lang.reflect.Method;

/** An enhanced version of EnumPropertyEditPanel that uses features of java 5, java 6 and beyond.
	@author keith cascio
	@since  20070324 */
public class EnumPropertyEditPanel5 extends EnumPropertyEditPanel implements Grepable<Struct,Simple,GrepEffect>
{
	public EnumPropertyEditPanel5( BeliefNetwork bn ){
		super( bn );
		myEnumTableModel5 = (EnumTableModel5) myTableModel;
	}

	/** interface Grepable
		@since 20070326 */
	public Class<Simple> grepFields(){
		return Simple.class;
	}

	/** interface Grepable
		@since 20070326 */
	public EnumSet<Simple> grepFieldsDefault(){
		return Simple.SINGLETON;
	}

	/** interface Grepable
		@since 20070326 */
	public String grepInfo(){
		return "the effect of this grep depends on the value of GrepEffect";
	}

	/** Allow the user to choose the effect of grepping an EnumPropertyEditPanel5.
		Some effects are higher impact than others.
		@since 20070326 */
	public enum GrepEffect{
		/** select rows that match the pattern */
		select{
			protected void in( JTable table, int row, boolean match, boolean destructive ){
				//System.out.println( "      select.in( "+row+", "+match+", "+destructive+" )" );
				if(      match       ) table.addRowSelectionInterval(    row, row );
				else if( destructive ) table.removeRowSelectionInterval( row, row );

				//if(      match       ) table.getSelectionModel().addSelectionInterval(    row, row );
				//else if( destructive ) table.getSelectionModel().removeSelectionInterval( row, row );
			}

			public String tip(){
				return "select rows that match the pattern";
			}
		},
		/** hide all rows that do not match the pattern */
		retain{
			protected void pre(){
				myList.clear();
			}

			protected void in( JTable table, int row, boolean match, boolean destructive ){
				if( match ) myList.add( row );
			}

			protected void post( EnumTableModel5 model ){
				//if( myList.isEmpty() ) return;
				int[] rows = new int[ myList.size() ];
				int   i    = 0;
				for( int row : myList ) rows[i++] = row;
				model.setRowsDisplayed( rows, true );
			}

			public String tip(){
				return "hide all rows that do not match the pattern";
			}

			private ArrayList<Integer> myList = new ArrayList<Integer>( 0x10 );
		},
		/** set target value on matching rows */
		settarget{
			public String tip(){
				return EnumTableModel5.STR_GREP_TIP;
			}
		};

		/** define a default effect, as of 20070326, default is {@link #select select}, the lowest impact alternative */
		public static final GrepEffect DEFAULT = select;

		/** setup for a grep */
		protected void   pre(){}
		/** process a single 'line' */
		protected void   in( JTable table, int row, boolean match, boolean destructive ){}
		/** conclude a grep */
		protected void   post( EnumTableModel5 model ){}
		/** tool tip for any associated GUI buttons, menu items, etc */
		public    String tip(){ return ""; }
	}

	/** interface Grepable
		@since 20070326 */
	public long grep( Filter filter, EnumSet<Simple> field_selector, Stringifier stringifier, GrepEffect effect, Collection<Struct> results ){
		long matches = 0;
		try{
			if(      effect == GrepEffect.settarget ) return myEnumTableModel5.grep( filter, field_selector, stringifier, getTargetValue(), results );
			else if( effect == null                 ) effect = GrepEffect.DEFAULT;

			Thread          thread      = Thread.currentThread();
			boolean         destructive = filter.flags().contains( Flag.destructive ), match;
			int             rows        = myJTable.getRowCount(), row;

			effect.pre();
			for( int i=0; i<rows; i++ ){
				if( thread.isInterrupted() ) break;
				effect.in( myJTable, i, match = filter.accept( stringifier.objectToString( myJTable.getValueAt(i,0) ) ), destructive );
				if( match ){
					++matches;
					if( results != null ){
						row = i;
						//row = myJTable.convertRowIndexToModel( i );
						if( myMethodCRITM != null ) row = ((Integer) myMethodCRITM.invoke( myJTable, row )).intValue();
						results.add( myEnumTableModel5.getRow( row ) );
					}
				}
			}
			effect.post( myEnumTableModel5 );
		}catch( Exception exception ){
			System.err.println( "warning: EnumPropertyEditPanel5.grep() caught " + exception );
		}
		return matches;
	}

	public Hierarchy getTargetHierarchy(){
		Hierarchy ret = super.getTargetHierarchy();
		if( flagHierarchyTarget || (ret == null) ) return ret;

		try{
			ret.add( getHierarchySystemClipboard() );
			flagHierarchyTarget = true;
		}catch( Exception exception ){
			System.err.println( "warning: EnumPropertyEditPanel5.getTargetHierarchy() caught " + exception );
		}

		return ret;
	}

	public Hierarchy getHierarchyView(){
		Hierarchy ret = super.getHierarchyView();
		if( flagHierarchyView || (ret == null) ) return ret;

		try{
			ret.add( action_INVERTSHOWN );
			ret.add( getGrepAction( GrepEffect.retain ).newPanel() );
			flagHierarchyView = true;
		}catch( Exception exception ){
			System.err.println( "warning: EnumPropertyEditPanel5.getHierarchyView() caught " + exception );
		}

		return ret;
	}

	public Hierarchy getHierarchySelect(){
		Hierarchy ret = super.getHierarchySelect();
		if( flagHierarchySelect || (ret == null) ) return ret;

		try{
			ret.add( getGrepAction( GrepEffect.select ).newPanel() );
			flagHierarchySelect = true;
		}catch( Exception exception ){
			System.err.println( "warning: EnumPropertyEditPanel5.getHierarchySelect() caught " + exception );
		}

		return ret;
	}

	public Hierarchy getHierarchySelected(){
		Hierarchy ret = super.getHierarchySelected();
		if( flagHierarchySelected || (ret == null) ) return ret;

		try{
			((AbstractButton) ret.add( action_TARGETSELECTED )).setText( "set target" );
			ret.add( action_ROTATESELECTED );
			flagHierarchySelected = true;
		}catch( Exception exception ){
			System.err.println( "warning: EnumPropertyEditPanel5.getHierarchySelected() caught " + exception );
		}

		return ret;
	}

	public Hierarchy getHierarchySet(){
		Hierarchy ret = super.getHierarchySet();
		if( (myPanelGrep != null) || (ret == null) ) return ret;

		try{
			ret.add( action_TARGETSELECTED );
			ret.add( myPanelGrep = getGrepSetTarget().newPanel() );
		}catch( Exception exception ){
			System.err.println( "warning: EnumPropertyEditPanel5.getHierarchySet() caught " + exception );
		}

		return ret;
	}

	public Hierarchy getHierarchySystemClipboard(){
		if( myHierarchySystemClipboard != null ) return myHierarchySystemClipboard;

		try{
			myHierarchySystemClipboard = new Hierarchy( "system clipboard", "transfer variable names to and from the system clipboard as text" );

			myHierarchySystemClipboard.add( action_COPY  );
			myHierarchySystemClipboard.add( action_PASTE );
		}catch( Exception exception ){
			System.err.println( "warning: EnumPropertyEditPanel5.getHierarchySystemClipboard() caught " + exception );
		}

		return myHierarchySystemClipboard;
	}

	/** @since 20070309 nasa */
	public GrepAction<Struct,Simple,EnumValue> getGrepSetTarget(){
		if( myGrepSetTarget != null ) return myGrepSetTarget;

		myGrepSetTarget = new GrepAction<Struct,Simple,EnumValue>( myEnumTableModel5 ){
			public EnumValue snapshot(){
				return getTargetValue();
			}

			public Stringifier getStringifier(){
				return selectStringifier();
			}
		};
		try{
			NetworkInternalFrame nif = ((DisplayableBeliefNetwork) myBeliefNetwork).getNetworkInternalFrame();
			myGrepSetTarget.setPreferences( nif.getPackageOptions() );
			nif.getParentFrame().addPreferenceListener( myGrepSetTarget );

			myGrepSetTarget.contextualize( nif );
		}catch( Exception exception ){
			System.err.println( "warning: EPEP5.getGrepSetTarget() caught " + exception );
		}
		return myGrepSetTarget;
	}
	private GrepAction<Struct,Simple,EnumValue> myGrepSetTarget;

	/** @since 20070326 */
	public GrepAction<Struct,Simple,GrepEffect> getGrepAction( final GrepEffect effect ){
		GrepAction<Struct,Simple,GrepEffect> ret = myGreps.get( effect );
		if( ret != null ) return ret;

		ret = new GrepAction<Struct,Simple,GrepEffect>( this ){
			{
				setToolTipText( effect.tip() );
			}

			public GrepEffect snapshot(){
				return effect;
			}

			public Stringifier getStringifier(){
				return selectStringifier();
			}
		};
		myGreps.put( effect, ret );
		try{
			NetworkInternalFrame nif = ((DisplayableBeliefNetwork) myBeliefNetwork).getNetworkInternalFrame();
			ret.setPreferences( nif.getPackageOptions() );
			nif.getParentFrame().addPreferenceListener( ret );

			ret.contextualize( nif );
		}catch( Exception exception ){
			System.err.println( "warning: EPEP5.getGrepAction() caught " + exception );
		}

		return ret;
	}
	private EnumMap<GrepEffect,GrepAction<Struct,Simple,GrepEffect>> myGreps = new EnumMap<GrepEffect,GrepAction<Struct,Simple,GrepEffect>>( GrepEffect.class );

	/** @since 20070309 nasa */
	public final SamiamAction action_TARGETSELECTED = new SamiamAction( "selected", "set target value on all selected rows (additive)", 's', null ){
		public void actionPerformed( ActionEvent e ){
			try{
				myEnumTableModel5.setRows( getTargetValue(), myJTable.getSelectedRows() );
			}catch( Exception exception ){
				System.err.println( "warning: EPEP.action_TARGETSELECTED.aP() caught " + exception );
			}
		}
	};

	/** @since 20070309 nasa */
	public final SamiamAction action_COPY = new SamiamAction( "copy", "copy name to system clipboard for each variable with target value", 'c', MainToolBar.getIcon( "Copy16.gif" ) ){
		public void actionPerformed( ActionEvent e ){
			try{
				myEnumTableModel5.copy( getTargetValue(), selectVariableStringifier() );
			}catch( Exception exception ){
				System.err.println( "warning: EPEP.action_COPY.aP() caught " + exception );
			}
		}
	};

	/** @since 20070309 nasa */
	public final SamiamAction action_PASTE = new SamiamAction( "paste", "set target value for each variable listed on the system clipboard", 'p', MainToolBar.getIcon( "Paste16.gif" ) ){
		public void actionPerformed( ActionEvent e ){
			try{
				myEnumTableModel5.paste( getTargetValue(), selectVariableStringifier() );
			}catch( Exception exception ){
				System.err.println( "warning: EPEP.action_PASTE.aP() caught " + exception );
			}
		}
	};

	/** @since 20070324 */
	public final SamiamAction action_INVERTSHOWN = new SamiamAction( "invert", "invert view (show hidden variables)", 'i', null ){
		public void actionPerformed( ActionEvent e ){
			try{
				myEnumTableModel5.invertShown();
			}catch( Exception exception ){
				System.err.println( "warning: EPEP.action_INVERTSHOWN.aP() caught " + exception );
			}
		}
	};

	/** @since 20070324 */
	public final SamiamAction action_ROTATESELECTED = new SamiamAction( "rotate", "for selected variables, set subsequent value (complement flags), ", 'r', null ){
		public void actionPerformed( ActionEvent e ){
			try{
				myEnumTableModel5.rotateRows( myJTable.getSelectedRows() );
			}catch( Exception exception ){
				System.err.println( "warning: EPEP.action_ROTATESELECTED.aP() caught " + exception );
			}
		}
	};

	/** @since 20070324 */
	public SamiamAction[] getMutators(){
		if( myMutators5 == null ){
			SamiamAction[] array4 = super.getMutators();
			SamiamAction[] array5 = new SamiamAction[] { action_TARGETSELECTED, getGrepSetTarget(), action_PASTE, action_ROTATESELECTED };
			myMutators5 = new SamiamAction[ array4.length + array5.length ];
			System.arraycopy( array4, 0, myMutators5, 0,             array4.length );
			System.arraycopy( array5, 0, myMutators5, array4.length, array5.length );
		}
		return myMutators5;
	}

	private Method          myMethodCRITM = OutputPanel5.getMethodConvertRowIndexToModel();
	private EnumTableModel5 myEnumTableModel5;
	private Hierarchy       myHierarchyTarget, myHierarchySystemClipboard;
	private JComponent      myPanelGrep;
	private boolean         flagHierarchyTarget = false, flagHierarchyView = false, flagHierarchySelected = false, flagHierarchySelect = false;
	private SamiamAction[]  myMutators5;
}
