package edu.ucla.belief.ui.clipboard;

import edu.ucla.belief.ui.UI;
import edu.ucla.belief.ui.NetworkInternalFrame;
import edu.ucla.belief.ui.internalframes.OutputPanel;
import edu.ucla.belief.ui.internalframes.Bridge2Tiger;
import edu.ucla.belief.ui.toolbar.MainToolBar;
import edu.ucla.belief.ui.util.Util;

import edu.ucla.belief.EvidenceController;
import edu.ucla.belief.StateNotFoundException;
import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.VariableComparator;
import edu.ucla.belief.IDComparator;
import edu.ucla.belief.io.NetworkIO;
import edu.ucla.belief.io.InflibFileFilter;
import edu.ucla.belief.io.InstantiationXmlizer;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Component;
import java.io.*;
import java.text.Collator;

/** @author keith cascio
	@since  20030516 */
public class InstantiationClipBoardImpl extends HashMap implements InstantiationClipBoard, ActionListener
{
	public InstantiationClipBoardImpl( UI ui )
	{
		myUI = ui;
		if( NetworkIO.xmlAvailable() ) myXmlizer = new InstantiationXmlizer();
		else if( Util.DEBUG_VERBOSE ) System.err.println( "InstantiationClipBoardImpl() xml API not available." );
	}

	public void copy( Map instantiation )
	{
		clear();
		putAll( instantiation );
		revalidate();
		if( myUI != null ){
			myUI.action_PASTEEVIDENCE.setSamiamUserMode( myUI.getSamiamUserMode() );//!isEmpty() );
		}
	}

	/** @since 20070904 */
	public boolean revalidate(){
		if( myOutputPanel != null && myGUI != null && myGUI.isShowing() ){
			myOutputPanel.newData( this, getVariables() );
			return true;
		}
		return false;
	}

	/** @since 20070904 */
	public void clear(){
		super.clear();
		revalidate();
	}

	public void cut( Map instantiation, EvidenceController controller )
	{
		copy( instantiation );
		try{
			controller.resetEvidence();
		}catch( Exception exception ){
			if( Util.DEBUG_VERBOSE ) edu.ucla.belief.ui.util.Util.STREAM_VERBOSE.println( "InstantiationClipBoardImpl.cut() caught " + exception );
			NetworkInternalFrame.showEvidenceWarning( exception, /*flagUnobserve*/ true, (FiniteVariable)null, null, (Component)myUI );
			return;
		}
	}

	public void paste( BeliefNetwork network )
	{
		//System.out.print( "InstantiationClipBoardImpl.paste()" );
		if( !isEmpty() )
		{
			EvidenceController controller = network.getEvidenceController();

			if( network.containsAll( keySet() ) )
			{
				//System.out.println( " network.containsAll()" );
				try{
					//controller.setObservations( this );
					controller.observe( this );
					return;
				}catch( Exception exception ){//StateNotFoundException snfe ){
					if( Util.DEBUG_VERBOSE ) edu.ucla.belief.ui.util.Util.STREAM_VERBOSE.println( "InstantiationClipBoardImpl.paste() caught " + exception );
					NetworkInternalFrame.showEvidenceWarning( exception, /*flagUnobserve*/ false, (FiniteVariable)null, null, (Component)myUI );
					return;
				}
			}
			//System.out.println( " !network.containsAll()" );

			//controller.resetEvidence();

			Map toObserve = new HashMap();
			Collection missingVariables = new LinkedList();
			Map missingStates = new HashMap(1);

			Object next;
			FiniteVariable nextFVar;
			String nextString;
			FiniteVariable forID;
			int index;
			Object instance;
			for( Iterator it = keySet().iterator(); it.hasNext(); )
			{
				next = it.next();
				//try{
					if( next instanceof FiniteVariable )
					{
						nextFVar = (FiniteVariable) next;
						if( network.contains( nextFVar ) ) forID = nextFVar;
						else forID = (FiniteVariable) network.forID( nextFVar.getID() );
						if( forID != null )
						{
							index = nextFVar.index( get( nextFVar ) );
							if( (int)0 <= index && index < forID.size() )//controller.observe( forID, forID.instance( index ) );
								toObserve.put( forID, forID.instance( index ) );
						}
						else missingVariables.add( nextFVar.getID() );
					}
					else
					{
						nextString = next.toString();
						forID = (FiniteVariable) network.forID( nextString );
						if( forID != null )
						{
							instance = forID.instance( get(next).toString() );
							if( instance != null )//controller.observe( forID, instance );
								toObserve.put( forID, instance );
							else missingStates.put( forID.getID(), get(next).toString() );
						}
						else missingVariables.add( nextString );
					}
				//}catch( StateNotFoundException snfe ){
				//	if( UI.DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "InstantiationClipBoardImpl.paste() caught " + snfe );
				//}
			}

			try{
				controller.setObservations( toObserve );
				//controller.observe( toObserve );
				//return;
			}catch( Exception exception ){//StateNotFoundException snfe ){
				if( Util.DEBUG_VERBOSE ) edu.ucla.belief.ui.util.Util.STREAM_VERBOSE.println( "InstantiationClipBoardImpl.paste() caught " + exception );
				NetworkInternalFrame.showEvidenceWarning( exception, /*flagUnobserve*/ false, (FiniteVariable)null, null, (Component)myUI );
			}

			if( (!missingVariables.isEmpty()) || (!missingStates.isEmpty()) )
			{
				String errmsg = "";
				for( Iterator it = missingVariables.iterator(); it.hasNext(); ){
					errmsg += "Network does not contain variable \""+it.next().toString()+"\".\n";
				}
				for( Iterator it = missingStates.keySet().iterator(); it.hasNext(); ){
					Object varID = it.next();
					Object stateName = missingStates.get( varID );
					errmsg += "Variable \""+varID.toString()+"\" does not contain state \""+stateName.toString()+"\".\n";
				}
				myUI.showErrorDialog( errmsg );
			}
		}
	}

	/**
		@author Keith Cascio
		@since 052303
	*/
	private Collection getVariables()
	{
		Collection variables = null;
		NetworkInternalFrame nif = myUI.getActiveHuginNetInternalFrame();
		if( nif != null ) variables = nif.getBeliefNetwork();
		return variables;
	}

	public JComponent view()
	{
		Collection variables = getVariables();

		if( myOutputPanel == null ) myOutputPanel = Bridge2Tiger.Troll.solicit().newOutputPanel( this, variables, true );//new OutputPanel( this, variables, true );
		else myOutputPanel.newData( this, variables );

		if( myGUI == null ) makeGUI();

		return myGUI;
	}

	protected void makeGUI()
	{
		if( myGUI != null ) return;

		JPanel pnlMain = new JPanel( new BorderLayout() );

		myPnlButtons = new JPanel();

		//myBtnCopy = makeButton( new JButton( MainToolBar.getIcon( "Copy16.gif" ) ) );
		//myBtnCut = makeButton( new JButton( MainToolBar.getIcon( "Cut16.gif" ) ) );
		//myBtnPaste = makeButton( new JButton( MainToolBar.getIcon( "Paste16.gif" ) ) );
		makeButton( myUI.action_COPYEVIDENCE );
		makeButton( myUI.action_CUTEVIDENCE );
		makeButton( myUI.action_PASTEEVIDENCE );
		if( NetworkIO.xmlAvailable() ){
			//myBtnLoad = makeButton( new JButton( "Load" ), "Load instantiation file to clipboard" );
			//myBtnSave = makeButton( new JButton( "Save" ), "Save clipboard instantiation to file" );
			makeButton( myUI.action_LOADINSTANTIATIONCLIPBOARD );
			makeButton( myUI.action_SAVEINSTANTIATIONCLIPBOARD );
		}
		else if( Util.DEBUG_VERBOSE ) System.err.println( "InstantiationClipBoardImpl not adding Save/Load buttons because xml API not available." );
		makeButton( myUI.action_IMPORTINSTANTIATION );
		makeButton( myUI.action_EXPORTINSTANTIATION );

		pnlMain.add( myPnlButtons, BorderLayout.NORTH );
		pnlMain.add( myOutputPanel, BorderLayout.CENTER );

		myGUI = pnlMain;
	}

	protected JButton makeButton( Action action )
	{
		JButton ret = new JButton( action );
		ret.setText( "" );
		myPnlButtons.add( ret );
		return ret;
	}

	protected JButton makeButton( JButton arg, String tooltip )
	{
		arg.addActionListener( this );
		arg.setToolTipText( tooltip );
		myPnlButtons.add( arg );
		return arg;
	}

	public void actionPerformed( ActionEvent evt )
	{
		Object src = evt.getSource();
	}

	public boolean load() throws UnsupportedOperationException, IOException
	{
		//System.out.println( "InstantiationClipBoardImpl.load()" );

		if( myXmlizer == null ) throw new UnsupportedOperationException( STR_MSG_API_ERROR );

		JFileChooser chooser = getFileChooser();
		if( chooser.showOpenDialog( getSuitableParentComponent() ) == JFileChooser.APPROVE_OPTION )
		{
			File fileSelected = chooser.getSelectedFile();
			if( fileSelected.exists() )
			{
				try{
					return load( fileSelected );
				}catch( IllegalArgumentException e ){
					System.err.println( "InstantiationClipBoardImpl this cannot happen." );
				}
			}
			return false;
		}
		else return true;
	}

	public boolean save() throws UnsupportedOperationException
	{
		if( myXmlizer == null ) throw new UnsupportedOperationException( STR_MSG_API_ERROR );

		JFileChooser chooser = getFileChooser();
		if( chooser.showSaveDialog( getSuitableParentComponent() ) == JFileChooser.APPROVE_OPTION )
		{
			File fileSelected = chooser.getSelectedFile();
			return save( myFileFilter.validateExtension( fileSelected ) );
		}
		return true;
	}

	public boolean load( File fileInput ) throws UnsupportedOperationException, IllegalArgumentException, IOException
	{
		//System.out.println( "InstantiationClipBoardImpl.load( "+fileInput.getPath()+" )" );

		if( myXmlizer == null ) throw new UnsupportedOperationException( STR_MSG_API_ERROR );
		if( !fileInput.exists() ) throw new IllegalArgumentException( fileInput.getPath() + " does not exist." );
		//return myXmlizer.load( this, fileInput );

		Map map = myXmlizer.getMap( fileInput );
		if( map == null ) return false;
		else{
			this.copy( map );
			return true;
		}
	}

	public boolean save( File fileOutput ) throws UnsupportedOperationException
	{
		if( myXmlizer == null ) throw new UnsupportedOperationException( STR_MSG_API_ERROR );
		return myXmlizer.save( this, fileOutput );
	}

	/** @since 20070904 */
	public  int          importFromSystem(){
		try{
			String contents = Util.pasteFromSystemClipboard();
			if( (contents == null) || (contents.length() < 1) ){
				clear();
				return 0;
			}

			int count = -1;
			if( myMatcherImportXML  == null ) myMatcherImportXML  = Pattern.compile( "<\\s*inst\\s+id\\s*=\\s*[\"']([^\"']+)[\"']\\s+value\\s*=\\s*[\"']([^\"']*)[\"']\\s*/>" ).matcher( "" );

			if( (count = importFromSystemImpl( myMatcherImportXML,  contents )) > 0 ){
				revalidate();
				return count;
			}

			if( myMatcherImportText == null ) myMatcherImportText = Pattern.compile( "(\\w+)\\s*=\\s*(\\w+)" ).matcher( "" );

			if( (count = importFromSystemImpl( myMatcherImportText, contents )) > 0 ){
				revalidate();
				return count;
			}
		}catch( Throwable throwable ){
			System.err.println( "warning: InstantiationClipBoardImpl.importFromSystem() caught " + throwable );
			if( Util.DEBUG_VERBOSE ){
				System.err.println( Util.STR_VERBOSE_TRACE_MESSAGE );
				throwable.printStackTrace();
			}
		}

		return 0;
	}

	/** @since 20070904 */
	private int      importFromSystemImpl( Matcher matcher, String contents ){
		String  id, value;
		int     count     = 0;
		if( matcher.reset( contents ).find() ){
			clear();
			do{
				put( matcher.group( 1 ), matcher.group( 2 ) );
				++count;
			} while( matcher.find() );
		}
		return count;
	}

	/** @since 20070904 */
	public  boolean        exportToSystem(){
		try{
			if(  myBufferOutput == null ) myBufferOutput = new StringBuffer( size() * 0x20 );
			else myBufferOutput.setLength( 0 );

			if( size() > 0 ){
				if(  myLexicographic == null ) myLexicographic = new ArrayList( size() );
				else myLexicographic.clear();

				if( myLexicographic.addAll( keySet() ) ){
					Comparator comparator = ( myLexicographic.get(0) instanceof FiniteVariable ) ? ((Comparator) IDComparator.getInstanceID()) : ((Comparator) Collator.getInstance());
					Collections.sort( myLexicographic, comparator );
				}

				Object key;
				String id, value;
				for( Iterator it = myLexicographic.iterator(); it.hasNext(); ){
					key   = it.next();
					id    = ( key instanceof FiniteVariable ) ? ((FiniteVariable) key).getID() : key.toString();
					value = get( key ).toString();
					myBufferOutput.append( id ).append( " = " ).append( value ).append( ", " );
				}
				if( myBufferOutput.length() > 0 ) myBufferOutput.setLength( myBufferOutput.length() - 2 );
			}

			Util.copyToSystemClipboard( myBufferOutput.toString() );

			return true;
		}catch( Throwable throwable ){
			System.err.println( "warning: InstantiationClipBoardImpl.exportToSystem() caught " + throwable );
			if( Util.DEBUG_VERBOSE ){
				System.err.println( Util.STR_VERBOSE_TRACE_MESSAGE );
				throwable.printStackTrace();
			}
		}
		return false;
	}

	protected JFileChooser getFileChooser()
	{
		if( myFileChooser == null )
		{
			myFileChooser = new JFileChooser( myUI.getSamiamPreferences().defaultDirectory );
			myFileChooser.setMultiSelectionEnabled(false);
			myFileChooser.setAcceptAllFileFilterUsed(false);
			myFileFilter = new InflibFileFilter( new String[]{".inst"}, UI.STR_SAMIAM_ACRONYM + " instantiation (*.inst)" );
			myFileChooser.addChoosableFileFilter( myFileFilter );
		}
		return myFileChooser;
	}

	protected Container getSuitableParentComponent()
	{
		if( myGUI != null ) return myGUI;
		else return myUI;
	}

	protected JFileChooser myFileChooser;
	protected InflibFileFilter myFileFilter;
	protected InstantiationXmlizer myXmlizer;
	protected UI myUI;
	protected OutputPanel myOutputPanel;
	protected JComponent myGUI;
	protected JPanel myPnlButtons;
	//protected JButton myBtnCopy;
	//protected JButton myBtnCut;
	//protected JButton myBtnPaste;
	//protected JButton myBtnLoad;
	//protected JButton myBtnSave;
	protected Matcher        myMatcherImportXML, myMatcherImportText;
	protected StringBuffer   myBufferOutput;
	protected ArrayList      myLexicographic;

	public static final String STR_MSG_API_ERROR = "XML API not available.";

}
