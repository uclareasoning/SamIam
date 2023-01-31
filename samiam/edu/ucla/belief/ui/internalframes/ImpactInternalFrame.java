package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.statusbar.StatusBar;
import edu.ucla.belief.ui.toolbar.MainToolBar;

import edu.ucla.belief.*;
import edu.ucla.util.ImpactProperty;
import edu.ucla.util.EnumValue;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import javax.swing.border.*;

/**
	@author Keith Cascio
	@since 073003
*/
public class ImpactInternalFrame extends JInternalFrame implements ActionListener, EvidenceChangeListener, CPTChangeListener, NetStructureChangeListener, NodePropertyChangeListener
{
	public static final String STR_MSG_IMPACT_PREFIX = "calculating impact of evidence change ";
	public static final String STR_MSG_IMPACT_POSTFIX = "...";
	public static final String STR_FILENAME_ICON = "Impact16.gif";

	public ImpactInternalFrame( NetworkInternalFrame fn )
	{
		super("Evidence Impact", true, true, true, true);

		hnInternalFrame = fn;
		hnInternalFrame.addEvidenceChangeListener( this );
		hnInternalFrame.addCPTChangeListener( this );
		hnInternalFrame.addNetStructureChangeListener( this );
		hnInternalFrame.addNodePropertyChangeListener( this );

		myEvidenceController = hnInternalFrame.getBeliefNetwork().getEvidenceController();

		init();

		return;
	}

	private void init()
	{
		Icon iconFrame = MainToolBar.getIcon( STR_FILENAME_ICON );
		if( iconFrame != null ) setFrameIcon( iconFrame );

		getContentPane().removeAll();

		BeliefNetwork bn = hnInternalFrame.getBeliefNetwork();
		myModel = new ImpactTableModel( bn );
		myJTable = new JTable( myModel );
		myModel.configure( myJTable );

		TableColumn column = myJTable.getColumnModel().getColumn( 1 );
		column.setPreferredWidth( INT_WIDTH_COLUMN_IMPACT );
		column.setMaxWidth( INT_WIDTH_COLUMN_IMPACT );
		column.setMinWidth( INT_WIDTH_COLUMN_IMPACT );
		column.setWidth( INT_WIDTH_COLUMN_IMPACT );

		JScrollPane pain = new JScrollPane( myJTable );

		/*
		myLblEvidence = new JLabel();
		FlowLayout layout = new FlowLayout();
		layout.setAlignment( FlowLayout.LEADING );
		JPanel pnlEvidenceDescription = new JPanel( layout );
		pnlEvidenceDescription.add( new JLabel( STR_DESCRIP_PREFIX ) );
		pnlEvidenceDescription.add( myLblEvidence );
		pnlEvidenceDescription.add( new JLabel( STR_DESCRIP_POSTFIX ) );
		*/

		myJTextArea = new JTextArea();
		myJTextArea.setEditable( false );
		myJTextArea.setLineWrap( true );
		myJTextArea.setBackground( Color.lightGray );
		myJTextArea.setBorder( BorderFactory.createLineBorder( Color.gray, 1 ) );

		JPanel pnlMain = new JPanel( new BorderLayout() );
		pnlMain.add( pain, BorderLayout.CENTER );
		pnlMain.add( myJTextArea, BorderLayout.SOUTH );

		getContentPane().add( pnlMain );
	}

	public static final int INT_WIDTH_COLUMN_IMPACT = (int)128;
	public static final String STR_DESCRIP_PREFIX = "Last evidence change { ";
	public static final String STR_DESCRIP_POSTFIX = " }";

	/**
		interface NodePropertyChangeListener
	*/
	public void nodePropertyChanged( NodePropertyChangeEvent e )
	{
		//System.out.println( "ImpactInternalFrame.nodePropertyChanged( "+dVar+" )" );
		if( myModel != null )
		{
			EnumValue value = e.variable.getProperty( ImpactProperty.PROPERTY );
			if( !( ImpactProperty.PROPERTY.toBoolean( value ) == myModel.contains( e.variable ) ) )
			{
				setVisible( false );
				myFlagReinitModel = true;
			}
		}
	}

	public void netStructureChanged( NetStructureEvent ev)
	{
		setVisible( false );
		myFlagReinitModel = true;
	}

	/**
		interface EvidenceChangeListener
	*/
	public void warning( EvidenceChangeEvent ece ) {}

	/**
		interface EvidenceChangeListener
	*/
	public void evidenceChanged( EvidenceChangeEvent ece )
	{
		if( isVisible() )
		{
			String message = STR_MSG_IMPACT_PREFIX;
			try{
				String descrip = ece.describe( myEvidenceController );
				message += descrip + STR_MSG_IMPACT_POSTFIX;
				edu.ucla.belief.ui.util.Util.pushStatusWest( hnInternalFrame, message );
				myModel.sort( ece );
				setEvidenceDescription( descrip );
			}catch( Exception e ){
				e.printStackTrace();
			}finally{
				edu.ucla.belief.ui.util.Util.popStatusWest( hnInternalFrame, message );
			}
		}
	}

	private void setEvidenceDescription( String text )
	{
		//myLblEvidence.setText( text );
		myJTextArea.setText( STR_DESCRIP_PREFIX + text + STR_DESCRIP_POSTFIX );
	}

	public void cptChanged( CPTChangeEvent evt )
	{
		setVisible( false );
	}

	public void setVisible( boolean flag )
	{
		if( flag && !isVisible() ) reInitialize();
		super.setVisible( flag );
	}

	public void reInitialize()
	{
		setEvidenceDescription( "waiting for input" );
		if( myFlagReinitModel )
		{
			myModel.init();
			myFlagReinitModel = false;
		}
		myModel.setInferenceEngine( hnInternalFrame.getInferenceEngine() );
		myModel.computeAllMarginals();
	}

	public void actionPerformed( ActionEvent e ) {
	}

	protected NetworkInternalFrame hnInternalFrame;
	protected EvidenceController myEvidenceController;
	protected JTable myJTable;
	protected JLabel myLblEvidence;
	protected JTextArea myJTextArea;
	protected ImpactTableModel myModel;
	private boolean myFlagReinitModel = false;
}
