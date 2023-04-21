package edu.ucla.belief.ui.networkdisplay;

import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.preference.SamiamPreferences;
import edu.ucla.belief.ui.internalframes.Bridge2Tiger;

import edu.ucla.belief.io.*;
import edu.ucla.structure.*;
import edu.ucla.util.AbstractStringifier;
import edu.ucla.util.Interruptable;

import javax.swing.filechooser.FileFilter;
import javax.swing.*;
import javax.swing.border.Border;
import java.io.*;
import java.util.*;
import java.awt.*;

/** See http://www.graphviz.org
	@author Keith Cascio
	@since 020805 */
public class DotLayoutManager implements FileLocationBrowser.FileInformationSource
{
	/** @since 020705 */
	public synchronized void dotLayout( DisplayableBeliefNetwork dbn ){
		if( myRunDotLayout == null ) myRunDotLayout = new RunDotLayout();
		myDisplayableBeliefNetwork = dbn;
		myRunDotLayout.start( dbn );
	}

	/** @since 020705 */
	public class RunDotLayout extends Interruptable {
		public void runImpl( Object arg1 ){
			DisplayableBeliefNetwork dbn = (DisplayableBeliefNetwork) arg1;
			String result = DotLayoutManager.this.dotLayoutImpl( dbn );
			UI ui = null;
			NetworkInternalFrame nif = dbn.getNetworkInternalFrame();
			if( nif != null ) ui = nif.getParentFrame();
			if( ( ui== null) || (!ui.isVisible()) ){
				Util.STREAM_TEST.println( result );
				return;
			}
			int type = (result == STR_SUCCESS_DOT_LAYOUT) ? JOptionPane.PLAIN_MESSAGE : JOptionPane.WARNING_MESSAGE;
			Object message = getMessage( result );
			if( message == null ) return;
			ui.showMessageDialog( message, "Dot Layout", type );
		}
	}
	private RunDotLayout myRunDotLayout;

	private static boolean FLAG_DEBUG = false;
	public static final String STR_SUCCESS_DOT_LAYOUT = "Dot layout success.";
	public static final String STR_FAILURE_DOT_LAYOUT = "Dot layout failed: ";
	public static final String STR_UNAVAILABLE_DOT_LAYOUT = "\"Dot\" program unavailable.";
	public static final String STR_URL_GRAPHVIZ = "http://www.graphviz.org";

   	/** @since 020805 */
   	public class NodeLabelSupplier implements GraphvizIO.ExtraDotInfoSupplier
   	{
   		public String getGlobalGraphInfo(){
   			return "";
   		}

   		public String getVertexInfo( Object vertex ){
   			String ret = "";
   			if( vertex instanceof DisplayableFiniteVariable ){
   				DisplayableFiniteVariable dfv = (DisplayableFiniteVariable)vertex;
   				Dimension dim = new Dimension();
   				NodeLabel nl = dfv.getNodeLabel();
   				if( nl == null ) dim = dfv.getDimension( dim );
   				else dim = nl.getVirtualSize( dim );
   				ret = "width=\""+GraphvizIO.scale( dim.width )+"\", height=\""+GraphvizIO.scale( dim.height )+"\"";
   			}
   			return ret;
   		}
   	};
   	private static GraphvizIO.ExtraDotInfoSupplier NODELABALSUPPLIER;

	/** @since 020705 */
	private String dotLayoutImpl( DisplayableBeliefNetwork dbn ){
		int numrepositioned = (int)0;
		File userDotExecutableLocation = null;
		try{
			File temp = File.createTempFile( "DotLayoutManager", ".dot" );
			temp.deleteOnExit();
			PrintStream out = new PrintStream( new FileOutputStream( temp ) );
			GraphvizIO io = new GraphvizIO();
			if( NODELABALSUPPLIER == null ) NODELABALSUPPLIER = new NodeLabelSupplier();
			io.writeDot(
				(DirectedGraph)dbn,
				/*title*/"DotLayoutManager",
				AbstractStringifier.VARIABLE_ID,
				NODELABALSUPPLIER,
				out );
			userDotExecutableLocation = getFileLocationBrowser().getFile();
			if( userDotExecutableLocation == null ) return STR_UNAVAILABLE_DOT_LAYOUT;
			File tempofile = File.createTempFile( "DotLayoutManager", ".lainout.dot" );
			tempofile.deleteOnExit();
			//String cmd = "\"" + userDotExecutableLocation.getAbsolutePath() + "\" -V";
			//String cmd = "\"" + userDotExecutableLocation.getAbsolutePath() + "\" -v \"-o" + tempofile.getAbsolutePath() + "\" \"" + temp.getAbsolutePath() + "\"";
			String cmd = "\"" + userDotExecutableLocation.getAbsolutePath() + "\" \"-o" + tempofile.getAbsolutePath() + "\" \"" + temp.getAbsolutePath() + "\"";
			//System.out.println( "executing command:\n" + cmd );
			Process process = Runtime.getRuntime().exec( cmd );
			//BufferedReader ifilereader = new BufferedReader( new FileReader( temp ) );
			//BufferedReader ofilereader = new BufferedReader( new FileReader( tempofile ) );
			//BufferedReader istream = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
			//BufferedReader errstream = new BufferedReader( new InputStreamReader( process.getErrorStream() ) );
			int result = process.waitFor();
			//Thread.sleep( (long)3000 );
			String line;
			//System.out.println( "command output:" );
			//while( (line = istream.readLine()) != null ) System.out.println( line );
			//System.out.println( "command error output:" );
			//while( (line = errstream.readLine()) != null ) System.out.println( line );
			//System.out.println( "ifile:" );
			//while( (line = ifilereader.readLine()) != null ) System.out.println( line );
			//System.out.println( "ofile:" );
			//while( (line = ofilereader.readLine()) != null ) System.out.println( line );
			//System.out.println( "glean:" );
			numrepositioned = gleanLayoutFromDot( dbn, tempofile, io );
			NetworkInternalFrame nif = this.getNetworkInternalFrame();
			if( nif != null ) nif.getNetworkDisplay().refresh();
		}catch( Exception exception ){
			if( FLAG_DEBUG ) exception.printStackTrace();
			return STR_FAILURE_DOT_LAYOUT + exception.toString();
		}finally{
			if( numrepositioned <= 0 ) getFileLocationBrowser().setFile( (File)null );
		}
		if( numrepositioned > 0 ) return STR_SUCCESS_DOT_LAYOUT;
		else return STR_FAILURE_DOT_LAYOUT + "\nexecutable might be incorrect or corrupted.";
	}

	/** @since 020705 */
	private int gleanLayoutFromDot( DisplayableBeliefNetwork dbn, File dotFile, GraphvizIO io ) throws IOException
	{
		Map layout = io.gleanLayoutFromDot( dotFile, new HashMap( dbn.size() ) );
		int count = (int)0;
		if( !layout.isEmpty() ){
			io.normalize( layout );
			String id;
			DisplayableFiniteVariable node;
			NodeLabel label;
			Point location;
			for( Iterator it = layout.keySet().iterator(); it.hasNext(); ){
				id = (String) it.next();
				node = (DisplayableFiniteVariable) dbn.forID( id );
				if( node != null ){
					location = (Point) layout.get( id );
					label = node.getNodeLabel();
					if( label == null ) node.setLocation( location );
					else label.setVirtualLocation( location );
					++count;
				}
			}
		}
		return count;
	}

	/** @since 031405 */
	public void forgetLocation(){
		getFileLocationBrowser().setFile( (File)null );
	}

	/** @since 020905 */
	private FileLocationBrowser getFileLocationBrowser(){
		if( myFileLocationBrowser == null ){
			myFileLocationBrowser = new FileLocationBrowser( (FileLocationBrowser.FileInformationSource)this );
		}
		return myFileLocationBrowser;
	}

	/** interface FileLocationBrowser.FileInformationSource */
	public FileFilter getFilter( FileLocationBrowser.Platform platform ){
		if( platform == FileLocationBrowser.Platform.WINDOWS ) return new InflibFileFilter( new String[] { ".exe" }, "Windows Executable (dot.exe)" );
		else return null;
	}

	/** interface FileLocationBrowser.FileInformationSource */
	public String getDescription( FileLocationBrowser.Platform platform ){
		if( platform == FileLocationBrowser.Platform.WINDOWS ) return "dot.exe";
		else return "the Dot program executable (dot*)";
	}

	/** interface FileLocationBrowser.FileInformationSource */
	public SamiamPreferences getPreferences(){
		UI ui = getUI();
		if( ui != null ) return ui.getSamiamPreferences();
		else return new SamiamPreferences( true );
	}

	/** interface FileLocationBrowser.FileInformationSource */
	public String getPreferenceFileToken(){
		return "UserDotExecutablePath";
	}

	/** interface FileLocationBrowser.FileInformationSource */
	public Component getDialogParent(){
		return getUI();
	}

	/** @since 020905 */
	public UI getUI(){
		NetworkInternalFrame nif = getNetworkInternalFrame();
		if( nif != null ) return nif.getParentFrame();
		else return (UI) null;
	}

	/** @since 020905 */
	public NetworkInternalFrame getNetworkInternalFrame(){
		if( myDisplayableBeliefNetwork != null ) return myDisplayableBeliefNetwork.getNetworkInternalFrame();
		else return (NetworkInternalFrame) null;
	}

	/** @since 020805 */
	private JComponent getMessage( String result ){
		if( result == STR_SUCCESS_DOT_LAYOUT ) return (JComponent)null;
		if( myMessageUnavailable == null ){
			myMessageUnavailable = new JPanel( new GridBagLayout() );
			GridBagConstraints c = new GridBagConstraints();

			c.anchor = GridBagConstraints.WEST;
			c.gridwidth = GridBagConstraints.REMAINDER;
			myMessageUnavailable.add( myLabelCaption = new JLabel( result, JLabel.LEFT ), c );
			myMessageUnavailable.add( new JLabel( "For more information, and to download,", JLabel.LEFT ), c );

			c.gridwidth = 1;
			myMessageUnavailable.add( new JLabel( "visit ", JLabel.LEFT ), c );
			c.gridwidth = GridBagConstraints.REMAINDER;
			myMessageUnavailable.add( new HyperLabel( STR_URL_GRAPHVIZ, getOnclick() ), c );
		}
		else myLabelCaption.setText( result );
		return myMessageUnavailable;
	}

	/** interface FileLocationBrowser.FileInformationSource
		@since 020805 */
	public JComponent getAccessory( FileLocationBrowser.Platform platform ){
		if( myAccessory == null ){
			myAccessory = new JPanel( new GridBagLayout() );
			GridBagConstraints c = new GridBagConstraints();

			c.anchor = GridBagConstraints.WEST;
			c.gridwidth = GridBagConstraints.REMAINDER;
			myAccessory.add( new JLabel( "For more information,", JLabel.LEFT ), c );
			myAccessory.add( new JLabel( "and to download", JLabel.LEFT ), c );
			myAccessory.add( new JLabel( "<html><nobr>the <b>Graphviz</b> package", JLabel.LEFT ), c );
			myAccessory.add( new JLabel( "<html><nobr>including <b>dot</b>,", JLabel.LEFT ), c );
			myAccessory.add( new JLabel( "please visit", JLabel.LEFT ), c );
			myAccessory.add( new HyperLabel( STR_URL_GRAPHVIZ, getOnclick() ), c );

			Border emptyinner = BorderFactory.createEmptyBorder( /*top*/4, /*left*/4, /*bottom*/4, /*right*/4 );
			Border etched = BorderFactory.createEtchedBorder();
			Border compoundinner = BorderFactory.createCompoundBorder( /*outside*/etched, /*inside*/emptyinner );
			Border emptyouter = BorderFactory.createEmptyBorder( /*top*/0, /*left*/16, /*bottom*/0, /*right*/4 );
			Border compoundouter = BorderFactory.createCompoundBorder( /*outside*/emptyouter, /*inside*/compoundinner );

			myAccessory.setBorder( compoundouter );
		}
		return myAccessory;
	}

	/** @since 020805 */
	private Runnable getOnclick(){
		if( myOnclick == null ){
			myOnclick = new Runnable(){
				public void run(){
					BrowserControl.displayURL( STR_URL_GRAPHVIZ );
				}
			};
		}
		return myOnclick;
	}

	public static final String STR_ARG_DEBUG = "-debug";
	public static final String STR_ARG_NOUI = "-noui";
   	public static final String STR_ARG_INPUTFILEPATH = "-in";
   	public static final String STR_ARG_OUTPUTFILEPATH = "-out";

    /** @since 020905 */
   	public static void main( String[] args ){
   		boolean flagUI = true;
   		boolean flagDebug = false;
   		String fname = null;
   		String opath = null;
   		for( int i=0; i<args.length; i++ ){
   			if( args[i].equals( STR_ARG_NOUI ) ) flagUI = false;
   			else if( args[i].equals( STR_ARG_DEBUG ) ) flagDebug = true;
   			else if( args[i].startsWith( STR_ARG_INPUTFILEPATH ) ){
   				fname = args[i].substring( STR_ARG_INPUTFILEPATH.length() );
   			}
   			else if( args[i].startsWith( STR_ARG_OUTPUTFILEPATH ) ){
   				opath = args[i].substring( STR_ARG_OUTPUTFILEPATH.length() );
   			}
   		}
   		if( flagDebug ) FLAG_DEBUG = true;
   		if( (fname == null) || (fname.length() < 1) ){
   			System.err.println( "usage: "+DotLayoutManager.class.getName()+" -in<input file path> [-dot]" );
   			return;
   		}
   		File infile = new File( fname );
   		if( !infile.exists() ){
   			System.err.println( "File " + fname + " not found." );
   			return;
   		}
   		String netname = edu.ucla.belief.io.NetworkIO.extractNetworkNameFromPath( fname );
   		if( opath == null ) opath = infile.getParentFile().getAbsolutePath() + File.separator + netname + ".lainout.net";

		DisplayableBeliefNetwork dbn = null;
   		try{
   			if( flagUI ){
				Util.STREAM_TEST.println( "Attempting to open input file using UI" );
				UI ui = new UI();
				ui.openFile( infile );
				NetworkInternalFrame nif = null;
				int count = 0;
				while( (nif == null) && (count<11) ){
					Thread.sleep( (long)1000 );
					Util.STREAM_TEST.println( "Waited " + (++count) + " seconds for open file" );
					nif = ui.getActiveHuginNetInternalFrame();
				}
				if( nif == null ){
					System.err.println( "Timeout waiting for file to open." );
					return;
				}

				dbn = nif.getBeliefNetwork();
			}
		}
		catch( Exception exception ){
			System.err.println( "caught: " + exception );
			if( flagDebug ) exception.printStackTrace();
			//return;
		}catch( Error error ){
			System.err.println( "caught: " + error );
			if( flagDebug ) error.printStackTrace();
		}

		if( dbn == null ){
			String message = flagUI ? "Failed using UI, attempting non-UI" : "Attempting to open input file, non-UI";
			Util.STREAM_TEST.println( message );
   			try{
				edu.ucla.belief.BeliefNetwork bn = edu.ucla.belief.io.NetworkIO.read( infile );
				dbn = Bridge2Tiger.Troll.solicit().newDisplayableBeliefNetworkImpl( bn, (NetworkInternalFrame)null );
			}catch( Exception exception ){
				System.err.println( "caught: " + exception );
				if( flagDebug ) exception.printStackTrace();
				//return;
			}catch( Error error ){
				System.err.println( "caught: " + error );
				if( flagDebug ) error.printStackTrace();
			}
		}

		if( dbn == null ){
			System.err.println( "Failed to open, giving up.  Good bye." );
			return;
		}
		else{
			DotLayoutManager dlm = new DotLayoutManager();
			dlm.dotLayout( dbn );
		}
   	}

	private FileLocationBrowser myFileLocationBrowser;
	//private JFileChooser myJFileChooser;
	private JComponent myMessageUnavailable;
	private JLabel myLabelCaption;
	private JComponent myAccessory;
	private Runnable myOnclick;
	//private FileFilter myAcceptAllFilter;
	private DisplayableBeliefNetwork myDisplayableBeliefNetwork;
}
