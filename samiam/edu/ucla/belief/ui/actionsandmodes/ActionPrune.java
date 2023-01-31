package edu.ucla.belief.ui.actionsandmodes;

import edu.ucla.belief.ui.displayable.DisplayableBeliefNetwork;
import edu.ucla.belief.ui.NetworkInternalFrame;
import edu.ucla.belief.ui.toolbar.MainToolBar;

import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.EvidenceController;
import edu.ucla.belief.Prune;
import edu.ucla.util.AbstractStringifier;
import edu.ucla.util.JVMProfiler;
import edu.ucla.util.QueryParticipantProperty;

import java.awt.event.*;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

/** @author Keith Cascio
	@since 20051010 */
public abstract class ActionPrune extends HandledModalAction implements SamiamUserModal
{
	public static final char CHAR_MNEMONIC       = 'p';
	public static final String STR_FILENAME_ICON = "Prune16.gif";
	public static final String STR_NAME          = "Prune";
	public static final String STR_TOOLTIP       = "Open a version of your network pruned according to evidence/query participants";

	public ActionPrune(){
		super( STR_NAME, STR_TOOLTIP, CHAR_MNEMONIC, MainToolBar.getIcon( STR_FILENAME_ICON ), ModeHandler.OPENANDNOTBUSYLOCAL );
	}

	abstract public NetworkInternalFrame getNIFToPrune();

	public void actionPerformed( ActionEvent e ){
		NetworkInternalFrame nif = this.getNIFToPrune();//getActiveHuginNetInternalFrame();
		if( nif == null ) return;
		new RunPrune( nif ).start();//prune( nif );
	}

	public NetworkInternalFrame prune( NetworkInternalFrame nif ){
		DisplayableBeliefNetwork unpruned = nif.getBeliefNetwork();
		if( unpruned == null ) return (NetworkInternalFrame)null;
		EvidenceController ec = unpruned.getEvidenceController();
		if( ec == null ) return (NetworkInternalFrame)null;

		Set participantsUnpruned = new HashSet( unpruned.findVariables( QueryParticipantProperty.PROPERTY, QueryParticipantProperty.PROPERTY.TRUE ) );
		Map evidenceUnpruned = ec.evidence();

		Map oldToNew = new HashMap( unpruned.size() );
		Map newToOld = new HashMap( unpruned.size() );
		Set participantsPruned = new HashSet( participantsUnpruned.size() );
		Map evidencePruned = new HashMap( evidenceUnpruned.size() );

		long sysMillisPre = System.currentTimeMillis();
		long cpuMillisPre = JVMProfiler.getCurrentThreadCpuTimeMS();
		BeliefNetwork pruned = Prune.prune( unpruned, participantsUnpruned, evidenceUnpruned, oldToNew, newToOld, participantsPruned, evidencePruned );
		long cpuMillisPost = JVMProfiler.getCurrentThreadCpuTimeMS();
		long sysMillisPost = System.currentTimeMillis();
		long sysMillisTotal = (sysMillisPost - sysMillisPre);
		long cpuMillisTotal = (cpuMillisPost - cpuMillisPre);

		String fileNameNew = adjustFileName( nif.getFileName(), evidenceUnpruned.size(), participantsUnpruned.size() );

		NetworkInternalFrame newFrame = null;
		try{
			pruned.getEvidenceController().setObservations( evidencePruned );

			newFrame = new NetworkInternalFrame( pruned, nif.getParentFrame(), fileNameNew );

			SamiamUserMode mode = newFrame.getSamiamUserMode();
			mode.setModeEnabled( SamiamUserMode.READONLY, true );
			mode.setModeEnabled( SamiamUserMode.EVIDENCEFROZEN, true );
			mode.setModeEnabled( SamiamUserMode.MODELOCK, true );
			newFrame.setSamiamUserMode( mode );

			new ConsoleMessage( newFrame, nif, evidencePruned, participantsPruned, cpuMillisTotal, sysMillisTotal ).start();
		}catch( Exception exception ){
			System.err.println( "ActionPrune.actionPerformed() caught " + exception );
		}

		return newFrame;
	}

	/** @author Keith Cascio
		@since 20051011 */
	public class RunPrune implements Runnable
	{
		public RunPrune( NetworkInternalFrame nif ){
			this.myNetworkInternalFrameToPrune = nif;
		}

		public void run(){
			ActionPrune.this.prune( myNetworkInternalFrameToPrune );
		}

		public void start(){
			ThreadGroup group = RunPrune.this.myNetworkInternalFrameToPrune.getThreadGroup();
			if( group == null ) group = ActionPrune.this.getGroupRunPrune();
			new Thread( group, (Runnable)RunPrune.this, "ActionPrune.RunPrune " + Integer.toString( INT_COUNTER_RUNPRUNE++ ) ).start();
		}

		private NetworkInternalFrame myNetworkInternalFrameToPrune;
	}

	/** @author Keith Cascio
		@since 20051010 */
	public class ConsoleMessage implements Runnable
	{
		public ConsoleMessage( NetworkInternalFrame newFrame, NetworkInternalFrame oldFrame, Map evidencePruned, Collection participantsPruned, long cpuMillisTotal, long sysMillisTotal ){
			this.newFrame           = newFrame;
			this.oldFrame           = oldFrame;
			this.evidencePruned     = evidencePruned;
			this.participantsPruned = participantsPruned;
			this.cpuMillisTotal     = cpuMillisTotal;
			this.sysMillisTotal     = sysMillisTotal;
		}

		public void consoleMessage(){
			try{
				ConsoleMessage.this.newFrame.setConsoleVisible( true );
				Thread.sleep( 256 );

				PrintWriter stream = ConsoleMessage.this.newFrame.console;
				stream.println( "----------------------------------------------------------------" );
				pause();
				stream.println( ConsoleMessage.this.newFrame.getFileNameSansPath() + "\n  is the prune of " + ConsoleMessage.this.oldFrame.getFileNameSansPath() + " under:" );
				pause();
				stream.println( "      " + ConsoleMessage.this.evidencePruned.size() + " evidence assertions { " + AbstractStringifier.VARIABLE_ID.mapToString( ConsoleMessage.this.evidencePruned ) + " }" );
				pause();
				stream.println( "      " + ConsoleMessage.this.participantsPruned.size() + " query participants { " + AbstractStringifier.VARIABLE_ID.collectionToString( ConsoleMessage.this.participantsPruned ) + " }" );
				pause();
				stream.println( "  prune operation required " + ConsoleMessage.this.cpuMillisTotal + " ms (cpu profile), " + ConsoleMessage.this.sysMillisTotal + " ms (elapsed system clock)" );
				pause();
				stream.println();
				pause();
				stream.println( "Note: this network is permanently read-only and evidence frozen!" );
				pause();
				stream.println( "----------------------------------------------------------------" );
				pause();
				stream.println( "\n" );

				ConsoleMessage.this.newFrame.getConsoleFrameWriter().scrollToBeginning();
			}catch( Exception exception ){
				System.err.println( "ActionPrune.ConsoleMessage.consoleMessage() caught " + exception );
				//exception.printStackTrace();
			}
		}

		private void pause() throws InterruptedException{
			Thread.sleep( 64 );
		}

		public void run(){
			ConsoleMessage.this.consoleMessage();
		}

		public void start(){
			ThreadGroup group = ConsoleMessage.this.newFrame.getThreadGroup();
			if( group == null ) group = ActionPrune.this.getGroupConsoleMessage();
			new Thread( group, (Runnable)ConsoleMessage.this, "ActionPrune.ConsoleMessage " + Integer.toString( INT_COUNTER_CONSOLEMESSAGE++ ) ).start();
		}

		public NetworkInternalFrame newFrame, oldFrame;
		public Map evidencePruned;
		public Collection participantsPruned;
		public long cpuMillisTotal, sysMillisTotal;
	}

	public String adjustFileName( String fileNameOld, int sizeEvidence, int numParticipants ){
		int indexPoint = fileNameOld.lastIndexOf( '.' );
		String prefix = fileNameOld.substring( 0, indexPoint );
		String extension = fileNameOld.substring( indexPoint );

		String fileNameNew;
		synchronized( mySynch ){
			if( myFormat == null ) myFormat = new SimpleDateFormat( "yyyyMMdd_kkmmss_SSS" );
			String date = myFormat.format( new Date( System.currentTimeMillis() ) );

			if( myBuff == null ) myBuff = new StringBuffer( 256 );
			else myBuff.setLength(0);

			myBuff.append( prefix );
			myBuff.append( "_prune_e" );
			myBuff.append( sizeEvidence );
			myBuff.append( "_q" );
			myBuff.append( numParticipants );
			myBuff.append( "_" );
			myBuff.append( date );
			myBuff.append( extension );
			fileNameNew = myBuff.toString();
		}
		return fileNameNew;
	}

	/** @since 20051011 */
	public ThreadGroup getGroupRunPrune(){
		synchronized( mySynch ){
			if( myThreadGroupRunPrune == null ) myThreadGroupRunPrune = new ThreadGroup( "ActionPrune.RunPrune" );
			return myThreadGroupRunPrune;
		}
	}

	/** @since 20051011 */
	public ThreadGroup getGroupConsoleMessage(){
		synchronized( mySynch ){
			if( myThreadGroupConsoleMessage == null ) myThreadGroupConsoleMessage = new ThreadGroup( "ActionPrune.ConsoleMessage" );
			return myThreadGroupConsoleMessage;
		}
	}

	private StringBuffer myBuff;
	private SimpleDateFormat myFormat;
	private Object mySynch = new Object();

	private ThreadGroup myThreadGroupRunPrune, myThreadGroupConsoleMessage;
	private static int INT_COUNTER_RUNPRUNE = 0, INT_COUNTER_CONSOLEMESSAGE = 0;
}
