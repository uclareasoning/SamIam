package edu.ucla.belief.recursiveconditioning;

import edu.ucla.structure.*;
import edu.ucla.belief.*;
import edu.ucla.belief.dtree.*;
import edu.ucla.belief.inference.BeliefCompilation;
//{superfluous} import edu.ucla.belief.io.hugin.HuginLogReader;
import edu.ucla.util.*;

import javax.swing.*;
import java.util.*;
import java.io.*;

/** @author keith cascio
	@since  20030123 */
public class Settings implements UserObject, CreationMethod.Settings, ChangeBroadcaster
{
	public static boolean FLAG_DEBUG_VERBOSE = Definitions.DEBUG;

	public Settings(){}

	/** interface UserObject
		@since 20030314 */
	public UserObject onClone()
	{
		Settings ret = new Settings();
		ret.copy( this );
		ret.myBundle = null;
		return ret;
	}

	private Bundle myBundle;

	public Bundle getBundle()
	{
		if( myBundle == null ) myBundle = new Bundle();
		return myBundle;
	}

	/** @since 20030918 */
	public void setBundle( Bundle bundle )
	{
		//System.out.println( "(Settings)"+myDebugID+".setBundle()" );
		if( myBundle != bundle )
		{
			myBundle = bundle;

			Dtree newDtree = bundle.getDtree();
			setDtreeRequired( newDtree != null );

			RC newRC = bundle.getRC();

			if( myOutStream != null && newRC != null )
			{
				newRC.outputConsole = myOutStream;
				//System.out.println( "newRC.outputConsole == " + newRC.outputConsole );
			}

			if( newDtree != null ) fireNewDtree();
			fireSettingChanged();
		}
	}

	/** @since 20030730 */
	public void setOutStream( PrintWriter writer )
	{
		myOutStream = writer;
	}
	private PrintWriter myOutStream;

	public void copy( Settings toCopy )
	{
		boolean flagNotSettingsChanging = true;

		flagNotSettingsChanging &= myUserMemoryProportion == toCopy.myUserMemoryProportion;
		flagNotSettingsChanging &= myUserMemorySetting == toCopy.myUserMemorySetting;

		flagNotSettingsChanging &= myFlagUseKB == toCopy.myFlagUseKB;

		flagNotSettingsChanging &= myMemoryConversionFactor == toCopy.myMemoryConversionFactor;
		flagNotSettingsChanging &= myMemoryUnit == toCopy.myMemoryUnit;

		flagNotSettingsChanging &= mySettingsMightHaveChanged == toCopy.mySettingsMightHaveChanged;

		flagNotSettingsChanging &= myFlagDtreeSettingsChanged == toCopy.myFlagDtreeSettingsChanged;

		flagNotSettingsChanging &= myDtreeMethod == toCopy.myDtreeMethod;

		flagNotSettingsChanging &= myElimAlgo == toCopy.myElimAlgo;

		flagNotSettingsChanging &= myMetisAlgo == toCopy.myMetisAlgo;
		flagNotSettingsChanging &= myNumDtrees == toCopy.myNumDtrees;
		flagNotSettingsChanging &= myNumPartitions == toCopy.myNumPartitions;
		flagNotSettingsChanging &= myBalanceFactor == toCopy.myBalanceFactor;

		flagNotSettingsChanging &= myDtreeStyle == toCopy.myDtreeStyle;
		flagNotSettingsChanging &= myHuginLogFile == toCopy.myHuginLogFile;
		flagNotSettingsChanging &= myTentativeHuginLogFilePath == toCopy.myTentativeHuginLogFilePath;


		myUserMemoryProportion = toCopy.myUserMemoryProportion;
		myUserMemorySetting = toCopy.myUserMemorySetting;

		myFlagUseKB = toCopy.myFlagUseKB;

		myMemoryConversionFactor = toCopy.myMemoryConversionFactor;
		myMemoryUnit = toCopy.myMemoryUnit;

		mySettingsMightHaveChanged = toCopy.mySettingsMightHaveChanged;

		myFlagDtreeSettingsChanged = toCopy.myFlagDtreeSettingsChanged;

		myRCComparator = toCopy.myRCComparator;
		myFlagKeepBest = toCopy.myFlagKeepBest;
		myDtreeMethod = toCopy.myDtreeMethod;

		myElimAlgo = toCopy.myElimAlgo;

		myMetisAlgo = toCopy.myMetisAlgo;
		myNumDtrees = toCopy.myNumDtrees;
		myNumPartitions = toCopy.myNumPartitions;
		myBalanceFactor = toCopy.myBalanceFactor;

		myDtreeStyle = toCopy.myDtreeStyle;
		myHuginLogFile = toCopy.myHuginLogFile;
		myTentativeHuginLogFilePath = toCopy.myTentativeHuginLogFilePath;

		myOutStream = toCopy.myOutStream;

		//setFlagDtreeSettingsChanged( false );

		if( toCopy.myBundle == null )
		{
			flagNotSettingsChanging &= myBundle == null;
			myBundle = null;
		}
		else
		{
			if( myBundle == null ) myBundle = new Bundle();
			flagNotSettingsChanging &= myBundle.copy( toCopy.myBundle );
		}

		if( !flagNotSettingsChanging ) fireSettingChanged();
	}

	protected String myDebugID = "NormalSettings";

	public void setDebugID( String id )
	{
		myDebugID = id;
	}

	protected WeakLinkedList myChangeListeners;

	/** interface ChangeBroadcaster */
	public boolean    addChangeListener( ChangeListener listener ){
		if(    myChangeListeners == null ){ myChangeListeners = new WeakLinkedList(); }
		return myChangeListeners.contains( listener ) ? false : myChangeListeners.add( listener );
	}

	/** interface ChangeBroadcaster */
	public boolean removeChangeListener( ChangeListener listener ){
		return myChangeListeners != null ? myChangeListeners.remove( listener ) : false;
	}

	protected WeakLinkedList myNewDtreeListeners;

	public boolean addNewDtreeListener( ChangeListener listener )
	{
		if(    myChangeListeners == null ){ myChangeListeners = new WeakLinkedList(); }
		return myChangeListeners.contains( listener ) ? false : myChangeListeners.add( listener );
	}

	public boolean removeNewDtreeListener( ChangeListener listener )
	{
		return myChangeListeners != null ? myChangeListeners.remove( listener ) : false;
	}

	/** interface ChangeBroadcaster */
	public ChangeBroadcaster fireNewDtree(){
		if( myNewDtreeListeners == null ){ return this; }

		myNewDtreeListeners.cleanClearedReferences();
		ChangeEvent evt = EVENT_NEW_DTREE;
		ArrayList list = new ArrayList( myNewDtreeListeners );
		for( Iterator it = list.iterator(); it.hasNext(); ){
			((ChangeListener)it.next()).settingChanged( evt );
		}
		return this;
	}

	public final ChangeEvent
	  EVENT_NEW_DTREE       = new ChangeEventImpl().source( this ),//, (int)2, "new dtree" );
	  EVENT_SETTING_CHANGED = new ChangeEventImpl().source( this );//, (int)0, "setting changed" );

	/** interface ChangeBroadcaster */
	public ChangeBroadcaster fireSettingChanged(){
		if( myChangeListeners == null ){ return this; }

		myChangeListeners.cleanClearedReferences();
		ChangeEvent evt = EVENT_SETTING_CHANGED;
		ArrayList list = new ArrayList( myChangeListeners );
		for( Iterator it = list.iterator(); it.hasNext(); ){
			((ChangeListener)it.next()).settingChanged( evt );
		}
		return this;
	}

	public static void setMillisPerRCCall( double millisPerRCCall )
	{
		DOUBLE_SECONDS_PER_RC_CALL = millisPerRCCall * DOUBLE_SECONDS_PER_MILLISECOND;
		DOUBLE_MINUTES_PER_RC_CALL = millisPerRCCall * DOUBLE_MINUTES_PER_MILLISECOND;
		DOUBLE_HOURS_PER_RC_CALL = millisPerRCCall * DOUBLE_HOURS_PER_MILLISECOND;
	}

	/** @since 20020813 */
	public static void setSecondsPerRCCall( double secondsPerRCCall )
	{
		DOUBLE_SECONDS_PER_RC_CALL	= secondsPerRCCall;
		DOUBLE_MINUTES_PER_RC_CALL	= DOUBLE_SECONDS_PER_RC_CALL / (double)60;
		DOUBLE_HOURS_PER_RC_CALL	= DOUBLE_MINUTES_PER_RC_CALL / (double)60;
	}

	public static double getSecondsPerRCCall()
	{
		return DOUBLE_SECONDS_PER_RC_CALL;
	}

	public static double getMinutesPerRCCall()
	{
		return DOUBLE_MINUTES_PER_RC_CALL;
	}

	public static double getHoursPerRCCall()
	{
		return DOUBLE_HOURS_PER_RC_CALL;
	}

	protected static double DOUBLE_SECONDS_PER_RC_CALL	= (double)0.00000028571428571424;
	protected static double DOUBLE_MINUTES_PER_RC_CALL	= (double)0.000000004761904761904;
	protected static double DOUBLE_HOURS_PER_RC_CALL	= (double)7.9365079365066666666666666666667e-11;
	public static final long LONG_BYTES_PER_DOUBLE = (int)8;

	public static double DOUBLE_INVALID_OPTIMAL_MEMORY = -2097140;//(int)52428899;
	protected double myUserMemoryProportion = (double)1;
	protected int myUserMemorySetting = (int)500;

	protected CachingScheme.RCCreateListener myRCCreateListener;

	public void setRCCreateListener( CachingScheme.RCCreateListener list )
	{
		myRCCreateListener = list;
	}

	public CachingScheme.RCCreateListener getRCCreateListener()
	{
		return myRCCreateListener;
	}

	public Dtree getDtree()
	{
		return ( myBundle == null ) ? null : myBundle.getDtree();
	}

	public void setDtree( Dtree dtree )
	{
		//System.out.println( "(Settings)"+myDebugID+".setDtree()" );

		if( dtree != getDtree() )
		{
			myBundle = new Bundle();
			if( dtree != null )
			{
				myBundle.setDtree( dtree );
				setDtreeRequired( true );
				fireNewDtree();
				fireSettingChanged();
			}
		}
	}

	private boolean myFlagUseKB = false;

	public boolean getUseKB()
	{
		return myFlagUseKB;
	}

	public void setUseKB( boolean flag )
	{
		//System.out.println( "Settings.setUseKB("+flag+")" );
		if( myFlagUseKB != flag )
		{
			myFlagUseKB = flag;
			if( myBundle != null )
			{
				RC rc = myBundle.getRC();
				if( rc != null )
				{
					if( myFlagUseKB ) rc.useKB();
					else rc.clearKB();
				}
			}
		}
	}

	/**
		@ret true iff d is a different memory proportion */
	public boolean setUserMemoryProportion( double d )
	{
		return setUserMemoryProportion( d, true );
	}

	/**
		@ret true iff d is a different memory proportion
		@author Keith Cascio
 @since 20030702 */
	public boolean setUserMemoryProportion( double d, boolean makeStale )
	{
		//System.out.println( "Settings.setUserMemoryProportion( " +d+ " )" );

		if( d != myUserMemoryProportion )
		{
			myUserMemoryProportion = d;
			if( makeStale ) myBundle.setStale( true );
			fireSettingChanged();
			return true;
		}
		else return false;
	}

	public double getUserMemoryProportion()
	{
		return myUserMemoryProportion;
	}

	/** @since 20030318 */
	public interface RCFactory
	{
		public RC manufactureRC( Dtree dtree, BeliefNetwork bn, CachingScheme cs, boolean myFlagUseKB );
	}
	public void setRCFactory( RCFactory factory )
	{
		myRCFactory = factory;
	}
	public RCFactory getRCFactory()
	{
		return myRCFactory;
	}
	protected RCFactory myRCFactory;

	public static final int INT_SECOND_MINUTE_THRESHOLD = (int)120;
	public static final int INT_MINUTE_HOUR_THRESHOLD = (int)256;
	public static final String STR_SECOND_UNIT = " seconds";
	public static final String STR_MINUTE_UNIT = " minutes";
	public static final String STR_HOURS_UNIT = " hours";

	protected String[] myReturnArray2 = new String[2];

	public String[] updateElapsedTimeDisplay( double milliseconds )
	{
		String newTimeUnit = null;
		int newNumber = (int)(DOUBLE_SECONDS_PER_MILLISECOND * milliseconds);
		if( newNumber < INT_SECOND_MINUTE_THRESHOLD ) newTimeUnit = STR_SECOND_UNIT;
		else
		{
			newNumber = (int)(DOUBLE_MINUTES_PER_MILLISECOND * milliseconds);
			if( newNumber < INT_MINUTE_HOUR_THRESHOLD ) newTimeUnit = STR_MINUTE_UNIT;
			else
			{
				newNumber = (int)(DOUBLE_HOURS_PER_MILLISECOND * milliseconds);
				newTimeUnit = STR_HOURS_UNIT;
			}
		}

		myReturnArray2[0] = String.valueOf( newNumber );
		myReturnArray2[1] = newTimeUnit;

		return myReturnArray2;
	}

	public static final long LONG_BYTE_KILO_THRESHOLD = (int)65536;
	public static final long LONG_KILO_MEGA_THRESHOLD = (int)1073741824;
	public static final double DOUBLE_BYTES_PER_BYTE = (double)1;
	public static final double DOUBLE_KILOBYTES_PER_BYTE = (double)0.0009765625;
	public static final double DOUBLE_MEGABYTES_PER_BYTE = (double)0.00000095367431640625;
	public static final String STR_BYTE_UNIT = " bytes";
	public static final String STR_KILOBYTE_UNIT = " KB";
	public static final String STR_MEGABYTE_UNIT = " MB";
	protected double myMemoryConversionFactor = DOUBLE_KILOBYTES_PER_BYTE;
	protected String myMemoryUnit = STR_KILOBYTE_UNIT;

	protected String[] myReturnArray3 = new String[2];

	/** @since 20030218 */
	public String describeUserMemoryProportion()
	{
		return describeUserMemoryProportion( getBundle().getAll() );
	}

	public String describeUserMemoryProportion( Computation comp )
	{
		String[] formatted = updateOptimalMemoryDisplay( comp );
		String userFormatted = updateUserMemoryDisplay( comp );
		String text = "using " + userFormatted + formatted[1] + " out of " + formatted[0];
		return text;
	}

	/** @since 20030707 */
	public String describeUserMemConcise( Computation comp )
	{
		String[] formatted = updateOptimalMemoryDisplay( comp );
		String userFormatted = updateUserMemoryDisplay( comp );
		String text = userFormatted + "/" + formatted[0]  + formatted[1];
		return text;
	}

	protected static String[] myReturnArray4 = new String[3];

	/** @since 20030707 */
	public static String[] formatMemoryNumbers( Computation comp, RC rc )
	{
		//System.out.println( "Settings.formatMemoryNumbers() " + comp.getNumCacheEntries( rc ) + " " + comp.getNumMaxCacheEntries() );

		String newMemoryUnit = STR_BYTE_UNIT;
		String optimalDisplayText;
		String userDisplayText;

		double optimalMemory = comp.getOptimalMemoryRequirement();

		double conversionFactor = (double)1;
		if( optimalMemory == DOUBLE_INVALID_OPTIMAL_MEMORY ) optimalDisplayText = userDisplayText = "0";
		else
		{
			if( optimalMemory < LONG_BYTE_KILO_THRESHOLD )
			{
				newMemoryUnit = STR_BYTE_UNIT;
				conversionFactor = DOUBLE_BYTES_PER_BYTE;
			}
			else if( optimalMemory < LONG_KILO_MEGA_THRESHOLD )
			{
				newMemoryUnit = STR_KILOBYTE_UNIT;
				conversionFactor = DOUBLE_KILOBYTES_PER_BYTE;
			}
			else
			{
				newMemoryUnit = STR_MEGABYTE_UNIT;
				conversionFactor = DOUBLE_MEGABYTES_PER_BYTE;
			}

			int optimalDisplayNumber = (int)( optimalMemory * conversionFactor );
			optimalDisplayText = Integer.toString( optimalDisplayNumber );

			int userDisplayNumber = (int)( comp.getNumCacheEntries( rc ) * LONG_BYTES_PER_DOUBLE * conversionFactor );
			userDisplayText = Integer.toString( userDisplayNumber );
		}

		myReturnArray4[0] = userDisplayText;
		myReturnArray4[1] = optimalDisplayText;
		myReturnArray4[2] = newMemoryUnit;

		return myReturnArray4;
	}

	/** @since 20030707 */
	public static String formatMemoryNumbersConcise( Computation comp, RC rc )
	{
		String[] array = formatMemoryNumbers( comp, rc );
		return array[0] + "/" + array[1] + array[2];
	}

	public String[] updateOptimalMemoryDisplay( Computation comp )
	{
		String newMemoryUnit = null;
		String memoryDisplayText;

		double optimalMemory = comp.getOptimalMemoryRequirement();

		if( optimalMemory == DOUBLE_INVALID_OPTIMAL_MEMORY ) memoryDisplayText = "0";
		else
		{
			if( optimalMemory < LONG_BYTE_KILO_THRESHOLD )
			{
				newMemoryUnit = STR_BYTE_UNIT;
				myMemoryConversionFactor = DOUBLE_BYTES_PER_BYTE;
			}
			else if( optimalMemory < LONG_KILO_MEGA_THRESHOLD )
			{
				newMemoryUnit = STR_KILOBYTE_UNIT;
				myMemoryConversionFactor = DOUBLE_KILOBYTES_PER_BYTE;
			}
			else
			{
				newMemoryUnit = STR_MEGABYTE_UNIT;
				myMemoryConversionFactor = DOUBLE_MEGABYTES_PER_BYTE;
			}

			int memoryDisplayNumber = (int)((double)optimalMemory * myMemoryConversionFactor);
			memoryDisplayText = Integer.toString( memoryDisplayNumber );
		}

		myReturnArray3[0] = memoryDisplayText;
		myReturnArray3[1] = newMemoryUnit;

		return myReturnArray3;
	}

	public String updateUserMemoryDisplay( Computation comp )
	{
		return updateUserMemoryDisplay( comp, myUserMemoryProportion );
	}

	public String updateUserMemoryDisplay( Computation comp, double proportion )
	{
		//System.out.println( "Settings.updateUserMemoryDisplay("+proportion+")" );
		//new Throwable().printStackTrace();

		String memoryDisplayTextUser;

		double optimalMemory = comp.getOptimalMemoryRequirement();

		if( optimalMemory == DOUBLE_INVALID_OPTIMAL_MEMORY ) memoryDisplayTextUser = "0";
		else
		{
			double myUserMemorySetting =  ((double)optimalMemory) * proportion;
			int memoryDisplayNumber = (int)(myUserMemorySetting * myMemoryConversionFactor);
			memoryDisplayTextUser = String.valueOf( memoryDisplayNumber );
		}

		return memoryDisplayTextUser;
	}

	protected boolean mySettingsMightHaveChanged = false;

	protected Thread myRunningCacheAllocationThread = null;

	/** @since 20020807 */
	public synchronized Thread createRCDtreeInThread( BeliefNetwork bn )
	{
		//System.out.println( "Settings.createRCDtreeInThread()" );

		if( bn != null && myBundle != null && validateDtree( bn ) )
		{
			//validateCacheFactor();
			CachingScheme scheme = myBundle.getCachingScheme();
			scheme.setCacheFactor( myUserMemoryProportion );
			Dtree dtree = myBundle.getDtree();

			RC.RCCreationParams rcparam = new RC.RCCreationParams();
			{
				rcparam.scalar = 1.0;
				rcparam.useKB = false;
				rcparam.allowKB = true;
				rcparam.bn = bn;
			}

			return RCDtree.createRCDtreeInThread( rcparam, scheme, myRCCreateListener,
					new DecompositionStructureUtils.ParamsTreeDT( bn, null, dtree, /*includeMPE*/true));
		}
		else return null;
	}

	/** @since 20030606 */
	public synchronized Thread allocRCDtreeInThread( BeliefNetwork bn )
	{
		//System.out.println( "Settings.allocRCDtreeInThread()" );

		if( bn != null && myBundle != null && ensureRCExists( bn ) )
		{
			CachingScheme scheme = myBundle.getCachingScheme();
			scheme.setCacheFactor( myUserMemoryProportion );
			RCDtree tree = (RCDtree) myBundle.getRC();
			double seed_bestCost = -1.0;//myBundle.getAll().getExpectedNumberOfRCCalls();
			Map seed_cf = null;

			return RCDtree.allocateRCDtreeInThread( tree, scheme, myRCCreateListener, seed_bestCost, seed_cf );
		}
		else return null;
	}

	/** @since 20020807 */
	public synchronized Thread createRCDgraphInThread( BeliefNetwork bn )
	{
		Dtree dtree = myBundle.getDtree();
		return createRCDgraphInThread( bn, new DecompositionStructureUtils.ParamsGraphDT( bn, null, dtree) );
	}

	/** @since 20030929 */
	public synchronized Thread createRCDgraphInThread( BeliefNetwork bn, DecompositionStructureUtils.ParamsGraph pgraph )
	{
		//System.out.println( "Settings.createRCDgraphInThread()" );

		if( bn != null && myBundle != null && validateDtree( bn ) )
		{
			//validateCacheFactor();
			CachingScheme scheme = myBundle.getCachingScheme();
			scheme.setCacheFactor( myUserMemoryProportion );

			RC.RCCreationParams rcparam = new RC.RCCreationParams();
			{
				rcparam.scalar = 1.0;
				rcparam.useKB = false;
				rcparam.allowKB = true;
				rcparam.bn = bn;
			}

			return RCDgraph.createRCDgraphInThread( rcparam, scheme, myRCCreateListener, pgraph );
		}
		else return null;
	}

	/** @since 20030606 */
	public synchronized Thread allocRCDgraphInThread( BeliefNetwork bn )
	{
		//System.out.println( "Settings.allocRCDgraphInThread()" );

		if( bn != null && myBundle != null && ensureRCExists( bn ) )
		{
			CachingScheme scheme = myBundle.getCachingScheme();
			scheme.setCacheFactor( myUserMemoryProportion );
			RCDgraph graph = (RCDgraph) myBundle.getRC();
			double seed_bestCost = -1.0;//myBundle.getAll().getExpectedNumberOfRCCalls();
			Map seed_cf = null;

			return RCDgraph.allocateRCDgraphInThread( graph, scheme, myRCCreateListener, seed_bestCost, seed_cf );
		}
		else return null;
	}

	public static CachingUniform CACHE_SCHEME_UNIFORM = new CachingUniform();
	public static CachingDFBnB CACHE_SCHEME_DFBnB = new CachingDFBnB();
	public static Object[] ARRAY_CACHE_SCHEMES = new Object[] { CACHE_SCHEME_DFBnB, CACHE_SCHEME_UNIFORM };

	public CachingScheme getCachingScheme()
	{
		return (myBundle==null) ? null : myBundle.getCachingScheme();
	}

	public void setCachingScheme( CachingScheme cs )
	{
		if( myBundle==null ) myBundle = new Bundle();
		myBundle.setCachingScheme( cs );
	}

	public static final double DOUBLE_SECONDS_PER_MILLISECOND = (double)1/(double)1000;
	public static final double DOUBLE_MINUTES_PER_MILLISECOND = (double)1/(double)60000;
	public static final double DOUBLE_HOURS_PER_MILLISECOND = (double)1/(double)3600000;

	public boolean rcFromJT2( BeliefNetwork bn, BeliefCompilation comp )
	{
		CACHE_SCHEME_DFBnB.setCacheFactor( (double)1 );

		DecompositionStructureUtils.CreateNodeMethod cnm =
			DecompositionStructureUtils.CreateNodeMethod.binaryNode_MinRCCalls;

		DecompositionStructureUtils.ParamsGraph pgraph =
			new DecompositionStructureUtils.ParamsGraphJT2( bn, null, comp, cnm );

		RCInferenceEngine engine =
			RCEngineGenerator.createInferenceEngine( bn, CACHE_SCHEME_DFBnB, 1.0, false, pgraph );

		RCDgraph newRC = engine.underlyingCompilation();

		setDtree( null );
		setRC( newRC );

		return true;
	}

	/** @since 20030606 */
	public boolean validateRC( BeliefNetwork bn )
	{
		//System.out.println( "Settings.validateRC" );

		if( !ensureRCExists( bn ) ) return false;

		if( myBundle.isStale() )
		{
			//System.out.println( "\t myBundle.isStale()" );

			RC rc = myBundle.getRC();

			if( rc instanceof RCDgraph ) allocRCDgraphInThread( bn );
			else allocRCDtreeInThread( bn );
		}

		return true;
	}

	/** @since 20030606 */
	public boolean ensureRCExists( BeliefNetwork bn )
	{
		if( !validateDtree( bn ) ) return false;

		if( myBundle.getRC() == null )
		{
			//System.out.println( "\t myBundle.getRC() == null" );

			RC newRC;
			myUserMemoryProportion = (double)1;

			CACHE_SCHEME_DFBnB.setCacheFactor( myUserMemoryProportion );

			if( myRCFactory == null ) newRC = RCEngineGenerator.createInferenceEngine( bn, getDtree(), CACHE_SCHEME_DFBnB, 1.0, false).underlyingCompilation();
			else newRC = myRCFactory.manufactureRC( getDtree(), bn, CACHE_SCHEME_DFBnB, false );
			setRC( newRC );
		}

		return true;
	}

	/** @since 20030214 */
	public boolean validateDtree( BeliefNetwork bn )
	{
		//System.out.println( "Settings.validateDtree()...getDtree() == null " +(getDtree() == null)+ "...isDtreeSettingChanged() " + isDtreeSettingChanged() );

		if( !isDtreeRequired() ) return true;

		Dtree oldDtree = getDtree();

		if( oldDtree == null || isDtreeSettingChanged() )
		{
			Dtree generated = generateDtree( bn );
			Dtree newDtree = generated;

			Stats stats = null;
			if( myFlagKeepBest )
			{
				if( oldDtree != null ) stats = myBundle.getStats( true );
				newDtree = selectBetterDtree( generated, oldDtree, stats );
			}

			if( newDtree != oldDtree )
			{
				RC newRC;
				CACHE_SCHEME_DFBnB.setCacheFactor( (double)1 );
				if( myRCFactory == null ) newRC = RCEngineGenerator.createInferenceEngine( bn, newDtree, CACHE_SCHEME_DFBnB, 1.0, false).underlyingCompilation();
				else newRC = myRCFactory.manufactureRC( newDtree, bn, CACHE_SCHEME_DFBnB, false );

				Bundle newBundle = new Bundle();
				newBundle.setDtree( newDtree );
				newBundle.setRC( newRC );
				if( stats != null ) newBundle.setStats( stats );

				Bundle best = myRCComparator.decideBest( myBundle, newBundle );

				if( best != myBundle )
				{
					setBundle( best );
					if( myUserMemoryProportion != (double)1 ) best.setStale( true );
				}
			}
		}

		return (getDtree() != null);
	}

	/** @since 20020822 */
	public Dtree selectBetterDtree( Dtree newDtree, Dtree oldDtree, Stats oldStats )
	{
		if( newDtree == null ) return oldDtree;

		boolean replace = false;

		int newHeight = Stats.INT_INVALID_DTREE_STAT;
		int newWidth = Stats.INT_INVALID_DTREE_STAT;
		//int newCutset = Stats.INT_INVALID_DTREE_STAT;
		int newContext = Stats.INT_INVALID_DTREE_STAT;

		if( oldDtree == null ) replace = true;
		else
		{
			//DtreeNode root = newDtree.root();
			//newCutset = root.getCutsetWidth( Collections.EMPTY_SET );

			newWidth = newDtree.getClusterSize( ) - 1;
			if( newWidth < oldStats.maxCluster ) replace = true;
			else if( newWidth == oldStats.maxCluster )
			{
				newContext = newDtree.getContextSize( );
				if( newContext < oldStats.maxContext ) replace = true;
				else if( newContext == oldStats.maxContext )
				{
					newHeight = newDtree.getHeight();
					if( newHeight < oldStats.height ) replace = true;
				}
			}
		}

		if( replace )
		{
			if( oldStats != null )
			{
				oldStats.height = newHeight;
				oldStats.maxCluster = newWidth;
				oldStats.maxCutset = Stats.INT_INVALID_DTREE_STAT;
				oldStats.maxContext = newContext;
			}
			return newDtree;
		}
		else return oldDtree;
	}

	public synchronized Dtree generateDtree( BeliefNetwork bn )
	{
		//System.out.println( "Settings.generateDtree()..." );
		//new Throwable().printStackTrace();

		if( bn == null )
		{
			System.err.println( "Settings.generateDtree(), null BeliefNetwork" );
			return null;
		}

		Dtree ret = null;
		try{
			if( FLAG_DEBUG_VERBOSE ){ Definitions.STREAM_VERBOSE.println( "Creating new Dtree." ); }

			CreationMethod method = getDtreeMethod();
			ret = method.getInstance( bn, this );

			if( FLAG_DEBUG_VERBOSE ){ Definitions.STREAM_VERBOSE.println( "DONE Creating new Dtree." ); }
		}catch( Dtree.DtreeCreationException e ){
			showErrorMessage( "Failed to create new Dtree.\n" + e.getMessage(), "Dtree error" );
			if( FLAG_DEBUG_VERBOSE )
			{
				System.err.println( "FAILED to create new Dtree." );
				Definitions.STREAM_VERBOSE.println( Definitions.STR_VERBOSE_TRACE_MESSAGE );
				e.printStackTrace( Definitions.STREAM_VERBOSE );
			}
		}catch( Exception e ){
			if( FLAG_DEBUG_VERBOSE )
			{
				System.err.println( "FAILED to create new Dtree." );
				Definitions.STREAM_VERBOSE.println( Definitions.STR_VERBOSE_TRACE_MESSAGE );
				e.printStackTrace( Definitions.STREAM_VERBOSE );
			}
		}

		if( ret == null )
		{
			if( FLAG_DEBUG_VERBOSE ) System.err.println( "Warning: Settings.generateDtree() failed." );
		}
		else
		{
			mySettingsMightHaveChanged = false;
			setFlagDtreeSettingsChanged( false );
		}

		return ret;
	}

	/** @since 20021213 */
	public Dtree doOpenDtree( BeliefNetwork bn, File fileSelected ) throws Exception
	{
		String strDtree = null;

		if( fileSelected.getPath().endsWith( ".dtree" ) )
		{
			BufferedReader in = new BufferedReader( new FileReader( fileSelected ) );
			String tempLine;
			while( (tempLine = in.readLine()) != null )
			{
				if( tempLine.indexOf( "(" ) != (int)-1 && tempLine.indexOf( ")" ) != (int)-1 )
				{
					strDtree = tempLine;
					break;
				}
			}
		}
		else if( fileSelected.getPath().endsWith( ".hlg" ) )
		{
			return MethodHuginLog.readDtree( bn, fileSelected, myDtreeStyle );
		}

		if( strDtree == null )
		{
			showErrorMessage( Definitions.STR_SAMIAM_ACRONYM+" could not read dtree from\n" + fileSelected.getPath(), "Dtree file error" );
			return null;
		}
		else return openSamiamDtree( strDtree, bn );
	}

	public static final String STR_DEFAULT_RCFILEPATH = "no rc file";

	public void setRC( RC rc )
	{
		setRC( rc, null );
	}

	public void setRC( RC rc, File fileSelected )
	{
		//System.out.println( "(Settings)"+myDebugID+".setRC( "+rc+((rc==getRC())?" ==":" !=")+" getRC() )..." );

		//myRC = rc;
		//myRCFilePath = STR_DEFAULT_RCFILEPATH;

		if( rc != getRC() )
		{
			if( rc == null ) { if( myBundle != null ) myBundle.setRC( null, null ); }
			else
			{
				if( myOutStream != null ) rc.outputConsole = myOutStream;
				if( myBundle == null ) myBundle = new Bundle();
				myBundle.setRC( rc, fileSelected );

				if( myFlagUseKB ) rc.useKB();
				else rc.clearKB();

				Computation comp = myBundle.getAll();
				myUserMemoryProportion = rc.statsAll().numCacheEntries() / comp.getNumMaxCacheEntries();
				//System.out.print( "...rc.numCacheEntries_All() = " + rc.numCacheEntries_All() );
				//System.out.print( "...comp.getNumMaxCacheEntries() = " + comp.getNumMaxCacheEntries() );
				//System.out.println( "...myUserMemoryProportion = " + myUserMemoryProportion );
				fireSettingChanged();
			}
		}

		//System.out.println( "rc.outputConsole == " + rc.outputConsole );
	}

	public void refresh( RC rc )
	{
		Computation comp = myBundle.getAll();

		//System.out.print( "Settings.refresh()...memory numbers = " + formatMemoryNumbersConcise( comp, rc ) );

		setRC( rc );
		double newProportion = comp.getNumCacheEntries( rc ) / comp.getNumMaxCacheEntries();
		setUserMemoryProportion( newProportion, false );
		myBundle.refresh();
	}

	public RC getRC()
	{
		return (myBundle==null) ? null : myBundle.getRC();
	}

	/**
		@author Keith Cascio
		@since 012803
	*/
	public boolean doOpenRC( BeliefNetwork bn, File fileSelected )
	{
		//System.out.println( "Settings.doOpenRC()" );
		try{
			Class.forName( "javax.xml.parsers.DocumentBuilder" );
		}catch( ClassNotFoundException e ){
			showErrorMessage( Definitions.STR_SAMIAM_ACRONYM+" could not open RC from\n" + fileSelected.getPath() + "\n XML packages not available.", "Open RC Error" );
			return false;
		}

		boolean exception = false;

		try
		{
			if( fileSelected.getPath().endsWith( ".rc" ) )
			{
				//myRC = RC.readXML( bn, fileSelected );
				new Xmlizer().readxml( fileSelected, bn, this, myFlagUseKB );
				//myRCFilePath = fileSelected.getPath();
			}

		}catch( Exception e ){
			exception = true;
			if( FLAG_DEBUG_VERBOSE ){
				Definitions.STREAM_VERBOSE.println( Definitions.STR_VERBOSE_TRACE_MESSAGE );
				e.printStackTrace( Definitions.STREAM_VERBOSE );
			}
		}

		if( exception ){
			showErrorMessage( Definitions.STR_SAMIAM_ACRONYM+" could not read rc from\n" + fileSelected.getPath(), "Error" );
			return false;
		}
		else return true;
	}

	/** @since 20021213 */
	protected void showErrorMessage( String message, String title )
	{
		JOptionPane.showMessageDialog( null, message, title, JOptionPane.ERROR_MESSAGE );
	}

	/** @since 20021213 */
	static public Dtree openSamiamDtree( String strDtree, BeliefNetwork bn ) throws Exception
	{
		Dtree newDtree = new Dtree(	bn,
						new DtreeCreateString( new PushbackReader( new StringReader( strDtree ) ) ));

		return newDtree;
	}

	/** @since 20021213 */
	public void doSaveDtree( File fileSelected )
	{
		String errmsg = null;

		try
		{
			if( fileSelected.getPath().endsWith( ".dtree" ) )
			{
				getDtree().write( fileSelected );
			}
			else errmsg = "Please choose a file with .dtree extension.";
		}catch( Exception e ){
			errmsg = e.getMessage();
			if( FLAG_DEBUG_VERBOSE ){
				Definitions.STREAM_VERBOSE.println( Definitions.STR_VERBOSE_TRACE_MESSAGE );
				e.printStackTrace( Definitions.STREAM_VERBOSE );
			}
		}

		if( errmsg != null )
		{
			showErrorMessage( Definitions.STR_SAMIAM_ACRONYM+" could not save dtree to\n" + fileSelected.getPath() + "\n" + errmsg, "Save Dtree Error" );
		}
	}

	/** @since 20030128 */
	public boolean doSaveRC( Dtree dtree, RC toSave, Computation comp, File fileSelected, String networkName )
	{
		//System.out.println( "Settings.doSaveRC()" );
		try{
			Class.forName( "javax.xml.parsers.DocumentBuilder" );
		}catch( ClassNotFoundException e ){
			showErrorMessage( Definitions.STR_SAMIAM_ACRONYM+" could not save RC to\n" + fileSelected.getPath() + "\n XML packages not available.", "Save RC Error" );
			return false;
		}

		String errmsg = null;

		if( dtree == null ) dtree = getDtree();
		else setDtree( dtree );

		Stats stats = myBundle.getStats( true );

		if( toSave == null )
		{
			if( getRC() == null ) errmsg = "null RC ";
			else toSave = getRC();
		}
		else if( myBundle != null ) myBundle.setRC( toSave );

		if( toSave != null )
		{
			try
			{
				if( !fileSelected.getPath().endsWith( ".rc" ) )
				{
					if( fileSelected.exists() ) errmsg = "Please choose a file with .rc extension.";
					else
					{
						String newPath = fileSelected.getPath();
						if( !newPath.endsWith( "." ) ) newPath += ".";
						newPath += "rc";
						fileSelected = new File( newPath );
					}
				}

				if( fileSelected.getPath().endsWith( ".rc" ) )
				{
					Xmlizer izer = new Xmlizer();
					org.w3c.dom.Document doc;
					if( dtree == getDtree() ) doc = izer.xmlize( dtree, stats, toSave, comp, networkName );
					else doc = izer.xmlize( dtree, null, toSave, comp, networkName );
					Xmlizer.writeXML( doc, fileSelected );
					//myRCFilePath = fileSelected.getPath();
				}
			}catch( Exception e ){
				errmsg = e.getMessage();
				if( FLAG_DEBUG_VERBOSE ){
					Definitions.STREAM_VERBOSE.println( Definitions.STR_VERBOSE_TRACE_MESSAGE );
					e.printStackTrace( Definitions.STREAM_VERBOSE );
				}
			}
		}

		if( errmsg != null )
		{
			showErrorMessage( Definitions.STR_SAMIAM_ACRONYM+" could not save RC to\n" + fileSelected.getPath() + "\n" + errmsg, "Save RC Error" );
			return false;
		}
		else return true;
	}

	//protected Stats myDtreeStats = new Stats();

	public int getDtreeHeight()
	{
		return (myBundle==null) ? Stats.INT_INVALID_DTREE_STAT : myBundle.getStats( false ).height;
	}

	public int getDtreeMaxCluster()
	{
		return (myBundle==null) ? Stats.INT_INVALID_DTREE_STAT : myBundle.getStats( false ).maxCluster;
	}

	public int getDtreeMaxCutset()
	{
		return (myBundle==null) ? Stats.INT_INVALID_DTREE_STAT : myBundle.getStats( false ).maxCutset;
	}

	public int getDtreeMaxContext()
	{
		return (myBundle==null) ? Stats.INT_INVALID_DTREE_STAT : myBundle.getStats( false ).maxContext;
	}

	protected boolean myFlagDtreeSettingsChanged = false;

	/** @since 20030218 */
	protected void setFlagDtreeSettingsChanged( boolean flag )
	{
		myFlagDtreeSettingsChanged = flag;
		if( myFlagDtreeSettingsChanged )
		{
			setDtreeRequired( true );
			//clearRC();
			fireSettingChanged();
		}
	}

	/** @since 20030214 */
	public boolean isDtreeSettingChanged()
	{
		return myFlagDtreeSettingsChanged;
	}

	protected boolean myFlagRequireDtree = true;

	/** @since 20030513 */
	public boolean isDtreeRequired()
	{
		return myFlagRequireDtree;
	}

	/** @since 20030513 */
	public void setDtreeRequired( boolean flag )
	{
		myFlagRequireDtree = flag;
		if( !myFlagRequireDtree ) setDtree( null );
	}

	protected RCComparator myRCComparator = RCComparator.getDefault();

	public RCComparator getRCComparator()
	{
		return myRCComparator;
	}

	public void setRCComparator( RCComparator comp )
	{
		myRCComparator = comp;
	}

	protected boolean myFlagKeepBest = false;

	public boolean getKeepBest()
	{
		return myFlagKeepBest;
	}

	public void setKeepBest( boolean flag )
	{
		myFlagKeepBest = flag;
	}

	public CreationMethod getDtreeMethod()
	{
		return myDtreeMethod;
	}

	public void setDtreeMethod( CreationMethod method )
	{
		if( method != myDtreeMethod )
		{
			setFlagDtreeSettingsChanged( true );
			myDtreeMethod = method;
		}
	}

	protected CreationMethod myDtreeMethod = (CreationMethod) CreationMethod.getArray()[0];

	/** @since 20030422 */
	protected MethodHuginLog.Style myDtreeStyle = MethodHuginLog.BALANCED;
	public MethodHuginLog.Style getDtreeStyle()
	{
		return myDtreeStyle;
	}
	public void setDtreeStyle( MethodHuginLog.Style style )
	{
		if( myDtreeStyle != style )
		{
			setFlagDtreeSettingsChanged( true );
			myDtreeStyle = style;
		}
	}

	protected File myHuginLogFile;
	public File getHuginLogFile()
	{
		return myHuginLogFile;
	}
	public void setHuginLogFile( File newFile )
	{
		if( !newFile.equals( myHuginLogFile ) )
		{
			if( newFile != null && !newFile.exists() )  throw new IllegalArgumentException( "Settings.setHuginLogFile(), file does not exist.." );
			myHuginLogFile = newFile;
			myTentativeHuginLogFilePath = null;
			setFlagDtreeSettingsChanged( true );
		}
	}
	protected String myTentativeHuginLogFilePath;
	public String getTentativeHuginLogFilePath()
	{
		return myTentativeHuginLogFilePath;
	}
	public void setTentativeHuginLogFilePath( String newPath )
	{
		//System.out.println( "Settings.setTentativeHuginLogFilePath( "+newPath+" )" );
		if( !newPath.equals( myTentativeHuginLogFilePath ) )
		{
			myTentativeHuginLogFilePath = newPath;
			myHuginLogFile = null;
			setFlagDtreeSettingsChanged( true );
		}
	}

	public static boolean FLAG_HMETIS_LOADED = Hmetis.loaded();

	protected EliminationHeuristic myElimAlgo = EliminationHeuristic.getDefault();

	public EliminationHeuristic getElimAlgo()
	{
		return myElimAlgo;
	}

	public void setElimAlgo( EliminationHeuristic ea )
	{
		if( ea != myElimAlgo )
		{
			setFlagDtreeSettingsChanged( true );
			myElimAlgo = ea;
		}
	}

	protected MethodHmetis.Algorithm myMetisAlgo = MethodHmetis.ALGO_HMETIS_STANDARD;

	public MethodHmetis.Algorithm getHMeTiSAlgo()
	{
		return myMetisAlgo;
	}

	public void setHMeTiSAlgo( MethodHmetis.Algorithm metisAlgo )
	{
		if( metisAlgo != myMetisAlgo )
		{
			setFlagDtreeSettingsChanged( true );
			myMetisAlgo = metisAlgo;
		}
	}

	protected int myNumDtrees = (int)3;

	public int getNumDtrees()
	{
		return myNumDtrees;
	}

	public void setNumDtrees( int num )
	{
		if( num != myNumDtrees )
		{
			setFlagDtreeSettingsChanged( true );
			myNumDtrees = num;
		}
	}

	protected int myNumPartitions = (int)3;

	public int getNumPartitions()
	{
		return myNumPartitions;
	}

	public void setNumPartitions( int num )
	{
		//System.out.println( myDebugID + ".setNumPartitions( " + num + " )" );
		if( num != myNumPartitions )
		{
			setFlagDtreeSettingsChanged( true );
			myNumPartitions = num;
		}
	}

	protected Object myBalanceFactor = MethodHmetis.STR_BALANCE_FACTOR_ALL;

	public Object getBalanceFactor()
	{
		return myBalanceFactor;
	}

	public void setBalanceFactor( Object balance )
	{
		if( balance != myBalanceFactor )
		{
			setFlagDtreeSettingsChanged( true );
			myBalanceFactor = balance;
		}
	}
}
