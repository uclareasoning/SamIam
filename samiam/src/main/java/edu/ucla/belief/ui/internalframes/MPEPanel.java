package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.EvidenceController;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.clipboard.*;
import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.preference.*;
import edu.ucla.belief.ui.toolbar.MainToolBar;
import edu.ucla.belief.ui.actionsandmodes.SamiamAction;
import edu.ucla.belief.ui.displayable.DisplayableFiniteVariable;

import edu.ucla.belief.VariableComparator;
import edu.ucla.belief.inference.map.ExactMap;
import il2.inf.map.MapSearch;
import edu.ucla.util.Stringifier;
import edu.ucla.util.AbstractStringifier;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;
import java.text.DecimalFormat;
import java.lang.reflect.*;

/**
	Based on MPE_RC_InternalFrame
	@author Keith Cascio
	@since 080802
*/
public class MPEPanel extends JPanel implements ActionListener, SloppyPanel.Jumpy, Stringifier.Selector
{
	/** @since 021903 */
	public MPEPanel( Map instantiation, double result, String strResultCaption, Collection variables )
	{
		init( instantiation, result, strResultCaption, false, true, variables );
		this.myScore = result;
	}

	public MPEPanel( double result, Map instantiation, Collection variables ){
		this( result, instantiation, variables, /*flagAddCloseButton*/ true, /*flagAddCopyButtons*/ true );
	}

	/** @since 20051102 */
	public MPEPanel( double result, Map instantiation, Collection variables, boolean flagAddCloseButton, boolean flagAddCopyButtons ){
		init( instantiation, result, "P(mpe,e)=", flagAddCloseButton, flagAddCopyButtons, variables );
		this.myScore = result;
	}

	/** @since 062104 */
	public MPEPanel( MapSearch.MapInfo results, double PrE, Collection variables, NetworkInternalFrame nif )
	{
		this.myResults = results;
		this.myNetworkInternalFrame = nif;
		this.myPrE = PrE;
		this.myMapResultToOutputPanels = new HashMap( results.results.size() );
		this.myIndexCurrentResult = 0;
		MapSearch.MapResult result = (MapSearch.MapResult) results.results.get( myIndexCurrentResult );
		OutputPanel plnOut = init( result.getConvertedInstatiation(), result.score, "P(MAP,e)=", false, true, variables );
		myMapResultToOutputPanels.put( result, plnOut );
		this.myLabelScoreConditioned = addResult( result.score/myPrE, "P(MAP|e)=" );
		this.myLabelFinished = addMessage( InputPanel.getMessageFinished( results ) );
		if( results.results.size() > 1 ){
			initSloppyNavigation();
			setIndexCurrentResult( myIndexCurrentResult );

			myButtonExplore = addButton( "Explore", this );
			myButtonExplore.setToolTipText( "compare solutions side-by-side" );
		}
	}

	/** @since 20070310 */
	public double getScore(){
		double ret = (double)1;
		try{
			if( this.myResults == null ) ret = this.myScore;
			else ret = ((MapSearch.MapResult) this.myResults.results.get( myIndexCurrentResult )).score;
		}catch( Exception exception ){
			System.err.println( "warning: MPEPanel.getResult() caught " + exception );
		}
		return ret;
	}

	/** @since 092104 */
	public void explore()
	{
		if( (myFrameExplore == null) && (myResults != null) ){
			JFrame frame = new JFrame( "MAP Solution Explorer" );

			ArrayList variables = new ArrayList( ((MapSearch.MapResult)myResults.results.iterator().next()).getConvertedInstatiation().keySet() );
			Collections.sort( variables, VariableComparator.getInstance() );

			SloppyPanel panel = new SloppyPanel( myResults, myPrE, variables );
			panel.addJumpy( (SloppyPanel.Jumpy)this );
			panel.setClipBoard( myClipBoard, myEvidenceController );
			//panel.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );
			frame.getContentPane().add( panel );
			frame.addComponentListener( new ResizeAdapter( panel ) );

			try{
				Icon iconFrame = MainToolBar.getIcon( MAPInternalFrame.STR_FILENAME_ICON );
				if( iconFrame != null ) frame.setIconImage( ((ImageIcon)iconFrame).getImage() );

				boolean flagMaximize = true;
				if( myNetworkInternalFrame != null ){
					SamiamPreferences prefs = myNetworkInternalFrame.getPackageOptions();
					Preference prefMaximize = prefs.getMappedPreference( SamiamPreferences.inferenceMAPExplorerFullScreenWidth );
					flagMaximize            = ((Boolean)prefMaximize.getValue()).booleanValue();
				}

				Rectangle boundsScreen =
					flagMaximize ? Util.getScreenBounds() : myNetworkInternalFrame.getParentFrame().getBounds();

				Dimension tableSize  = panel.getJTable().getPreferredSize();
				int       heightPref = Math.min( tableSize.height + 190, boundsScreen.height - 0x80 );
				Dimension preferred  = new Dimension( boundsScreen.width, heightPref );

				frame.setSize( preferred );
				Util.centerWindow( frame, boundsScreen );
			}catch( Exception exception ){
				System.err.println( "Warning: MPEPanel.explore() caught " + exception );
				frame.setSize( new Dimension( 600,300 ) );
			}

			myFrameExplore = frame;
		}
		if( myFrameExplore != null ) myFrameExplore.setVisible( true );
	}

	/** @since 20040621 */
	private void setIndexCurrentResult( int i )
	{
		if( (myIndexCurrentResult != i) && (i >= 0) && (i<myResults.results.size()) )
		{
			MapSearch.MapResult result = (MapSearch.MapResult) myResults.results.get( myIndexCurrentResult = i );

			OutputPanel pnlOut = null;
			if( myMapResultToOutputPanels.containsKey( result ) ){
				pnlOut = (OutputPanel) myMapResultToOutputPanels.get( result );
			}
			else{
				pnlOut = Bridge2Tiger.Troll.solicit().newOutputPanel( result.getConvertedInstatiation(), myVariables, false );//new OutputPanel( result.getConvertedInstatiation(), myVariables );
				pnlOut.setMinimumSize( new Dimension( 0, 0 ) );
				if( myMouseListener != null ) pnlOut.addMouseListener( myMouseListener );
				myMapResultToOutputPanels.put( result, pnlOut );
			}

			myInstantiation = result.getConvertedInstatiation();

			this.remove( pnlOutput );
			this.add( pnlOutput = pnlOut, BorderLayout.CENTER );
			this.myLabelResult.setText( theFormat.format( result.score ) );
			this.myLabelScoreConditioned.setText( theFormat.format( result.score/myPrE ) );
			this.myLabelFinished.setText( InputPanel.getMessageFinished( myResults ) );
			this.myLabelDisplayCurrentIndex.setText( Integer.toString( myIndexCurrentResult+1 ) );

			try{
				pnlOutput.setClipboard( myClipBoard );
				pnlOutput.setDiff(      myFlagDiff  );
				//if( myGrep != null ) myGrep.setTarget( pnlOutput );
				if( (myMethodGrepActionSetTarget != null) && (myGrep != null) )
					myMethodGrepActionSetTarget.invoke( myGrep, new Object[]{ pnlOutput } );
			}catch( Exception exception ){
				System.err.println( "warning: MPEPanel.setIndexCurrentResult() caught " + exception );
			}

			this.revalidate();
			this.repaint();
		}

		myButtonPrevious.setEnabled( myIndexCurrentResult != 0 );
		myButtonNext.setEnabled( myIndexCurrentResult != (myResults.results.size()-1) );
	}

	/** @since 20030516 */
	public void setClipBoard( InstantiationClipBoard clipboard, EvidenceController ec )
	{
		if( clipboard == null ) return;

		try{
		myClipBoard = clipboard;
		myEvidenceController = ec;

		if( myPopupMenu == null )
		{
			myCopyItem = new JMenuItem( getActionCopy() );//"Copy to clipboard" );
			myCopyItem.addActionListener( this );
			myItemCopyPlusEvidence = new JMenuItem( getActionCopyPlus() );//"Copy (+evidence)" );
			myItemCopyPlusEvidence.addActionListener( this );

			myPopupMenu = new JPopupMenu();
			myPopupMenu.add( myCopyItem );
			myPopupMenu.add( myItemCopyPlusEvidence );

			myMouseListener = new MouseAdapter()
			{
				public void mousePressed( MouseEvent e )
				{
					showPopup(e);
				}

				public void mouseClicked( MouseEvent e )
				{
					showPopup(e);
				}

				public void mouseReleased( MouseEvent e )
				{
					showPopup(e);
				}
			};

			//pnlResult.addMouseListener( myMouseListener );
			pnlOutput.addMouseListener( myMouseListener );
		}

		if( pnlOutput != null ) pnlOutput.setClipboard( myClipBoard );

		if( myButtonCopy             != null ) enable( myButtonCopy );
		if( myButtonCopyPlusEvidence != null ) enable( myButtonCopyPlusEvidence );
		}
		catch( Exception exception ){
			System.err.println( "warning: MPEPanel.setClipBoard() caught " + exception );
		}
	}

	/** @since 070703 */
	private void enable( JButton button )
	{
		button.addActionListener( this );
		button.setEnabled( true );
		button.setVisible( true );
	}

	/** @since 051603 */
	protected void showPopup( MouseEvent e )
	{
		if( e.isPopupTrigger() )
		{
			Point p = e.getPoint();
			SwingUtilities.convertPointToScreen( p,e.getComponent() );
			myPopupMenu.setLocation( p );
			myPopupMenu.setInvoker( this );
			myPopupMenu.setVisible( true );
		}
	}

	protected JPopupMenu myPopupMenu;
	protected JMenuItem myCopyItem;
	protected JMenuItem myItemCopyPlusEvidence;
	protected MouseListener myMouseListener;

	/** @since 051603 */
	public void actionPerformed( ActionEvent evt )
	{
		Object src = evt.getSource();
		//if( src == myCopyItem || src == myButtonCopy ) doCopy( false );
		//else if( src == myItemCopyPlusEvidence || src == myButtonCopyPlusEvidence ) doCopy( true );
		if( src == myButtonPrevious ) setIndexCurrentResult( myIndexCurrentResult - 1 );
		else if( src == myButtonNext ) setIndexCurrentResult( myIndexCurrentResult + 1 );
		else if( src == myButtonExplore ) explore();
	}

	/** interface SloppyPanel.Jumpy
		@since 092204 */
	public void jump( int value )
	{
		if( (value < 0) || (value >= myResults.results.size()) ) throw new IllegalArgumentException();
		else setIndexCurrentResult( value );
	}

	/** @since 051903 */
	public void doCopy( boolean plusEvidence )
	{
		//System.out.println( "MPEPanel().doCopy()" );
		Map toCopy = null;
		JTable table = pnlOutput.getJTable();
		if( table.getSelectedRowCount() < (int)1 )
		{
			Map instantiation = null;
			if( myResults == null ) instantiation = myInstantiation;
			else{
				MapSearch.MapResult result = (MapSearch.MapResult) myResults.results.get( myIndexCurrentResult );
				instantiation = result.getConvertedInstatiation();
			}

			if( plusEvidence ) toCopy = new HashMap( instantiation );
			else toCopy = instantiation;
		}
		else
		{
			Map selectedInstantiation = new HashMap();
			int size = table.getRowCount();

			int indexColumnVariable = table.convertColumnIndexToModel( (int)0 );
			int indexColumnValue    = table.convertColumnIndexToModel( (int)1 );

			for( int i=0; i<size; i++ )
			{
				if( table.isRowSelected(i) )
				{
					selectedInstantiation.put( table.getValueAt(i,indexColumnVariable), table.getValueAt(i,indexColumnValue) );
				}
			}
			//System.out.println( "copying selected: " + selectedInstantiation );
			toCopy = selectedInstantiation;
		}

		if( plusEvidence ) toCopy.putAll( myEvidenceController.evidence() );

		myClipBoard.copy( toCopy );
	}

	/** @since 20040922 */
	public SamiamAction getActionCopy()
	{
		if( myActionCopy == null ){
			myActionCopy = new SamiamAction( "Copy", "Copy instantiation to clipboard", 'c', MainToolBar.getIcon( "CopyAlt16.gif" ) ){
				public void actionPerformed( ActionEvent e ){
					MPEPanel.this.doCopy( false );
				}
			};
		}
		return myActionCopy;
	}

	/** @since 20040922 */
	public SamiamAction getActionCopyPlus()
	{
		if( myActionCopyPlus == null ){
			myActionCopyPlus = new SamiamAction( "Copy (+evidence)", "Copy instantiation and current evidence to clipboard", 'e', MainToolBar.getIcon( "CopyAlt16.gif" ) ){
				public void actionPerformed( ActionEvent e ){
					MPEPanel.this.doCopy( true );
				}
			};
		}
		return myActionCopyPlus;
	}

	/** @since 20040621 */
	private void initSloppyNavigation()
	{
		JPanel pnlButtons = new JPanel( new GridBagLayout() );

		GridBagConstraints c = new GridBagConstraints();
		pnlButtons.add( myButtonPrevious = makeButton( "Previous" ), c );
		myButtonPrevious.addActionListener( this );
		pnlButtons.add( Box.createHorizontalStrut( 4 ),              c );
		pnlButtons.add( myLabelDisplayCurrentIndex = new JLabel( Integer.toString( myIndexCurrentResult+1 ) ),     c );
		pnlButtons.add( myLabelDisplayTotal = new JLabel( " of " + Integer.toString( myResults.results.size() ) ), c );
		pnlButtons.add( Box.createHorizontalStrut( 4 ),              c );
		pnlButtons.add( myButtonNext = makeButton( "Next" ),         c );
		myButtonNext.addActionListener( this );

		myButtonPrevious.setToolTipText( "view previous solution (higher probability)" );
		myButtonNext.setToolTipText( "view next solution (lower probability)" );

		myResultConstraints.gridwidth = GridBagConstraints.REMAINDER;
		pnlResult.add( pnlButtons, myResultConstraints );
	}

	/** @since 20030219 */
	public JLabel addResult( double result, String strResultCaption ){
		JLabel lblRes = null;
		try{
			JLabel lblCap = new JLabel( strResultCaption, JLabel.LEFT );
			String formatted = result == InputPanel.DOUBLE_INVALID ? "P(e)" : theFormat.format( result );
			lblRes = new JLabel( formatted, JLabel.LEFT );

			getBufferResults().append( strResultCaption ).append( formatted ).append( '\n' );

			myResultConstraints.gridwidth = (int)1;
			myResultGridbag.setConstraints( lblCap, myResultConstraints );
			pnlResult.add( lblCap );

			myResultConstraints.gridwidth = GridBagConstraints.REMAINDER;
			myResultGridbag.setConstraints( lblRes, myResultConstraints );
			pnlResult.add( lblRes );

			new ClipboardHelper( lblCap, lblRes, MPEPanel.this );
		}catch( Exception exception ){
			System.err.println( "Warning: MPEPanel.addResult() failed, caught " + exception );
		}//finally{}

		return lblRes;
	}

	/** @since 20070311 */
	public StringBuffer append( StringBuffer buff ) throws Exception{
		if( myBufferResults != null ) buff.append( myBufferResults.toString() );
		buff.append( "result " ).append( myIndexCurrentResult+1 ).append( " of " );
		buff.append( (myResults == null) ? 1 : myResults.results.size() );
		buff.append( "{\n" );
		//pnlOutput.append( buff, getPreferredStringifier() );
		Method method = getMethodOutputPanelAppend();
		if( method == null ) buff.append( "text representation of MPE results requires java 5 or later\n" );
		else{
			try{
				method.invoke( pnlOutput, new Object[]{ buff, selectStringifier() } );
			}catch( Exception exception ){
				buff.append( "error converting MPE results to text: " );
				buff.append( exception );
				buff.append( '\n' );
			}
		}
		buff.append( "}\n" );
		return buff;
	}

	/** @since 20070321 */
	public Method getMethodOutputPanelAppend(){
		if( myMethodOutputPanelAppend == null ){
			try{
				myMethodOutputPanelAppend = Class.forName( "edu.ucla.belief.ui.internalframes.OutputPanel5" ).getMethod( "append", new Class[]{ Class.forName( "java.lang.Appendable" ), Stringifier.class } );
			}catch( Throwable throwable ){
				if( Util.DEBUG_VERBOSE ) System.err.println( "warning: MPEPanel.getMethodOutputPanelAppend() caught " + throwable );
			}
		}
		return myMethodOutputPanelAppend;
	}
	private Method myMethodOutputPanelAppend;

	/** @since 20070311 */
	private StringBuffer getBufferResults(){
		if( myBufferResults == null ) myBufferResults = new StringBuffer( 0x80 );
		return myBufferResults;
	}

	/** @since 060203 */
	public JLabel addMessage( String message )
	{
		JLabel lblCap = new JLabel( message, JLabel.LEFT );

		myResultConstraints.gridwidth = GridBagConstraints.REMAINDER;
		myResultGridbag.setConstraints( lblCap, myResultConstraints );
		pnlResult.add( lblCap );

		getBufferResults().append( message ).append( '\n' );

		return lblCap;
	}

	protected JComponent makeResultsPanel()
	{
		myResultGridbag = new GridBagLayout();
		myResultConstraints = new GridBagConstraints();

		JPanel ret = new JPanel( myResultGridbag );

		myResultConstraints.anchor = GridBagConstraints.WEST;

		return ret;
	}

	/** @since 080802 */
	public Object addActionListener( ActionListener AL )
	{
		myButtonClose.addActionListener( AL );
		return myButtonClose;
	}

	protected static final DecimalFormat theFormat = new DecimalFormat( "0.0###############################################################################" );

	protected OutputPanel init( Map instantiation, double result, String strResultCaption, boolean addCloseButton, boolean addCopyButton, Collection variables )
	{
		myInstantiation = instantiation;
		myVariables = variables;

		pnlResult = makeResultsPanel();
		myLabelResult = addResult( result, strResultCaption );

		// Create an output panel to display the result
		OutputPanel pnlOut = Bridge2Tiger.Troll.solicit().newOutputPanel( instantiation, variables, false );//new OutputPanel( instantiation, variables );
		pnlOut.setMinimumSize (new Dimension (0,0));
		pnlOutput = pnlOut;

		this.setLayout( new BorderLayout() );
		this.add( pnlResult, BorderLayout.NORTH );
		this.add( pnlOut, BorderLayout.CENTER );

		//if( addCopyButton || addCloseButton ) initButtonPanel();
		//if( myPanelButtons != null ) this.add( myPanelButtons, BorderLayout.SOUTH );
		this.add( initSouthPanel(), BorderLayout.SOUTH );

		if( addCopyButton )
		{
			SamiamAction adiff = getDiff();
			if( adiff != null ){
				AbstractButton btn = (AbstractButton) addButton( configure( new JToggleButton( adiff ) ) );
				btn.setText( null );
			}
			addButton( myButtonCopy             = makeCopyButton( getActionCopy()     ) );
			myButtonCopy.setText( null );
			addButton( myButtonCopyPlusEvidence = makeCopyButton( getActionCopyPlus() ) );
			myButtonCopyPlusEvidence.setText( "+e" );
		}

		if( addCloseButton )
		{
			addButton( myButtonClose = makeButton( "Close" ) );
		}

		pnlOutput.setClipboard( myClipBoard );

		return pnlOut;
	}

	/** @since 20070311 */
	private JComponent initSouthPanel(){
		JPanel pnlSouth = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill      = GridBagConstraints.HORIZONTAL;
		c.weightx   = 1;
		getGrep();
		if( myPanelGrep != null ) pnlSouth.add( myPanelGrep, c );
		//JComponent pnlGrep = initGrepPanel();
		//if( pnlGrep != null ) pnlSouth.add( pnlGrep, c );
		pnlSouth.add( initButtonPanel(), c );

		return pnlSouth;
	}

	/** @since 20070311 */
	/*private JComponent initGrepPanel(){
		SamiamAction       actionGrep = getGrep();
		if( actionGrep == null ) return null;

		JPanel             pnlGrep    = new JPanel( new GridBagLayout() );
		GridBagConstraints c          = new GridBagConstraints();

		c.weightx   = 0;
		c.fill      = GridBagConstraints.NONE;
		pnlGrep.add( configure( new JButton( actionGrep ), 1 ), c );
		pnlGrep.add( Box.createHorizontalStrut(4),           c );
		c.weightx   = 1;
		c.fill      = GridBagConstraints.HORIZONTAL;
		pnlGrep.add( actionGrep.getInputComponent(),         c );

		return pnlGrep;
	}*/

	/** @since 20070312 */
	public SamiamAction getDiff(){
		if( myDiff != null ) return myDiff;

		if( (pnlOutput == null) || (!pnlOutput.isTiger()) ) return myDiff = null;

		return myDiff = new SamiamAction( "Diff", "diff against instantiation clipboard", 'd', MainToolBar.getIcon( "Diff16.gif" ) ){
			public void actionPerformed( ActionEvent e ){
				try{
					AbstractButton src = (AbstractButton) e.getSource();
					pnlOutput.setDiff( myFlagDiff = src.isSelected() );
				}catch( Exception exception ){
					System.err.println( "warning: MPEPanel diff caught " + exception );
				}
			}
		};
	}

	/** @since 20070311 */
	public Stringifier selectStringifier(){
		Stringifier ret  = AbstractStringifier.VARIABLE_ID;
		try{
			SamiamPreferences prefs = null;
			if( myNetworkInternalFrame != null ) prefs = myNetworkInternalFrame.getPackageOptions();
			else if( myVariables != null ){
				Object first = myVariables.iterator().next();
				if( first instanceof DisplayableFiniteVariable ) prefs = ((DisplayableFiniteVariable)first).getNetworkInternalFrame().getPackageOptions();
			}

			if( prefs == null ) return ret;

			boolean flagLabels   = ((Boolean) prefs.getMappedPreference( SamiamPreferences.displayNodeLabelIfAvail ).getValue()).booleanValue();
			if( flagLabels ) ret = AbstractStringifier.VARIABLE_LABEL;
		}catch( Exception exception ){
			System.err.println( "warning: EPEP.getPreferredStringifier() caught " + exception );
		}
		return ret;
	}

	/** @since 20070321 */
	public SamiamAction getGrep(){
		if( myGrep != null ) return myGrep;
		if( (pnlOutput == null) || (!pnlOutput.isTiger()) ) return myGrep = null;

		try{
			Class        clazzGrepAction = Class.forName( "edu.ucla.belief.ui.actionsandmodes.GrepAction" );
			Class        clazzGrepable   = Class.forName( "edu.ucla.belief.ui.actionsandmodes.Grepable" );
			Class[]      arrayGrepable   = new Class[]{ clazzGrepable };
			Constructor  uctor           = clazzGrepAction.getConstructor( arrayGrepable );
			SamiamAction agrep           = (SamiamAction) uctor.newInstance( new Object[]{ pnlOutput } );

			try{
				Method   methodContextua = clazzGrepAction.getMethod( "contextualize", new Class[]{ NetworkInternalFrame.class } );
				methodContextua.invoke( agrep, new Object[]{ myNetworkInternalFrame } );
			}catch( Throwable throwable ){
				if( Util.DEBUG_VERBOSE ) System.err.println( "warning: MPEPanel.getGrep() caught " + throwable );
			}

			try{
				if( myNetworkInternalFrame == null ){
					Set                       keys = null;
					DisplayableFiniteVariable dfv  = null;
					if(      !                       myVariables.isEmpty() ) dfv = (DisplayableFiniteVariable) myVariables.iterator().next();
					else if( ! (keys = myInstantiation.keySet()).isEmpty() ) dfv = (DisplayableFiniteVariable) keys       .iterator().next();
					if( dfv != null ) myNetworkInternalFrame = dfv.getNetworkInternalFrame();
				}
				if( myNetworkInternalFrame != null ){
					agrep.setPreferences( myNetworkInternalFrame.getPackageOptions() );
					myNetworkInternalFrame.getParentFrame().addPreferenceListener( agrep );
				}
			}catch( Throwable throwable ){
				if( Util.DEBUG_VERBOSE ){
					System.err.println( "warning: MPEPanel.getGrep() caught " + throwable );
				}
			}

			Method       methodSSS       = clazzGrepAction.getMethod( "setStringifierSelector", new Class[]{ Stringifier.Selector.class } );
			methodSSS.invoke( agrep, new Object[]{ MPEPanel.this } );
			myMethodGrepActionSetTarget  = clazzGrepAction.getMethod( "setTarget", arrayGrepable );

			Method       methodNP        = clazzGrepAction.getMethod( "newPanel", (Class[]) null );
			myPanelGrep                  = (JComponent) methodNP.invoke( agrep, (Object[]) null );
			myPanelGrep.setBorder( BorderFactory.createEmptyBorder(1,1,1,1) );
			myGrep                       = agrep;
		}catch( Throwable throwable ){
			if( Util.DEBUG_VERBOSE ){
				System.err.println( "warning: MPEPanel.getGrep() caught " + throwable );
				throwable.printStackTrace();
			}
		}

		/*return myGrep = new GrepAction( pnlOutput ){
			public Stringifier getStringifier(){
				return MPEPanel.this.getPreferredStringifier();
			}
		};*/
		return myGrep;
	}
	private Method myMethodGrepActionSetTarget;

	/** @since 20051102 */
	private JComponent initButtonPanel(){
		this.myListButtons  = new LinkedList();
		this.myPanelButtons = new JPanel( new GridBagLayout() );
		return this.myPanelButtons;
	}

	/** @since 20040602 */
	public JButton addButton( String text, ActionListener listener ){
		JButton ret = (JButton) configure( new JButton( text ), 2 );
		ret.addActionListener( listener );

		return (JButton) addButton( ret );
	}

	/** @since 20051102 */
	public JButton addButton( Action action ){
		return (JButton) addButton( configure( new JButton( action ) ) );
	}

	/** @since 20051102 */
	public JComponent addButton( JComponent ret ){
		myListButtons.addFirst( ret );

		myPanelButtons.removeAll();

		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1;
		myPanelButtons.add( Box.createHorizontalStrut(2), c );
		c.weightx = 0;
		for( Iterator it = myListButtons.iterator(); it.hasNext(); ){
			myPanelButtons.add( (JComponent) it.next(), c );
		}

		myPanelButtons.revalidate();
		myPanelButtons.repaint();

		return ret;
	}

	/** @since 070703 */
	public static JButton makeCopyButton( Action action )
	{
		JButton ret = (JButton) configure( new JButton( action ) );
		//ret.setIcon( MainToolBar.getIcon( "Copy16.gif" ) );
		ret.setEnabled( false );
		ret.setVisible( false );
		return ret;
	}

	/** @since 060403 */
	public static JButton makeButton( String text )
	{
		return (JButton) configure( new JButton( text ) );
	}

	/** @since 092204 */
	public static AbstractButton configure( AbstractButton btn ){
		return configure( btn, 0 );
	}

	/** @since 20070312 */
	public static AbstractButton configure( AbstractButton btn, int headroom ){
		btn.setMargin( new Insets( headroom,4,headroom,4 ) );
		Font fntButton = btn.getFont();
		btn.setFont( fntButton.deriveFont( (float)10 ) );
		return btn;
	}

	protected Map myInstantiation;
	protected InstantiationClipBoard myClipBoard;
	protected EvidenceController myEvidenceController;
	private MapSearch.MapInfo myResults;
	private int myIndexCurrentResult;
	private double myPrE, myScore;
	private Collection myVariables;
	private Map myMapResultToOutputPanels;
	protected SamiamAction myGrep, myDiff;
	//private GrepAction myGrep;
	protected boolean myFlagDiff = false;
	protected JComponent myPanelGrep;

	private LinkedList myListButtons;
	protected JPanel myPanelButtons;
	protected JButton myButtonClose;
	protected JButton myButtonCopy;
	protected JButton myButtonCopyPlusEvidence;
	protected JComponent pnlResult;
	protected StringBuffer myBufferResults;
	protected OutputPanel pnlOutput;
	protected GridBagLayout myResultGridbag;
	protected GridBagConstraints myResultConstraints;
	private JLabel myLabelResult;
	private JLabel myLabelScoreConditioned;
	private JLabel myLabelFinished;
	private JButton myButtonPrevious;
	private JLabel myLabelDisplayCurrentIndex;
	private JLabel myLabelDisplayTotal;
	private JButton myButtonNext;
	private JButton myButtonExplore;
	private JFrame myFrameExplore;
	private NetworkInternalFrame myNetworkInternalFrame;
	private SamiamAction myActionCopy;
	private SamiamAction myActionCopyPlus;
}
