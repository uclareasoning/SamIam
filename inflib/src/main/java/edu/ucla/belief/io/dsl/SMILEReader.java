package edu.ucla.belief.io.dsl;

import edu.ucla.belief.*;
import edu.ucla.belief.io.*;
import edu.ucla.belief.io.hugin.HuginPotential;
import edu.ucla.util.EnumProperty;
//{superfluous} import edu.ucla.util.EnumValue;
import edu.ucla.util.UserEnumProperty;
import edu.ucla.belief.sensitivity.ExcludePolicy;

import java.util.*;
import java.io.*;
import java.awt.Point;

/** @author keith cascio
	@since  20020403 */
public class SMILEReader
{
	/** @since 070302 */
	protected              boolean FLAG_FATAL_ERROR        = false;
	private   static       boolean FLAG_VERBOSE            = Definitions.DEBUG;
	public    static final String  STR_NATIVE_LIBRARY_NAME = "callsmile";

	public SMILEReader(){}

	public boolean loadSMILE( File infile, FileType type ) throws Exception
	{
		if( FLAG_LOADLIBRARY_FAILED ) return false;

		FLAG_FATAL_ERROR = false;
		if( !infile.exists() )
		{
			System.err.println( "Error: " + infile.getPath() + " does not exist.");
			return false;
		}

		if( type == null ) type = FileType.getTypeForFile( infile );
		int genieConstant = (type==null) ? (int)0 : type.getGenieConstant();

		myMap = new HashMap();
		myIDsToFVars = new HashMap();
		myGenieNet = new GenieNetImpl();
		int mainSubmodelHandle = (int)-1;
		try
		{
			mainSubmodelHandle = loadDSL( infile.getPath(), genieConstant );
		}
		catch( Exception e )
		{
			String errmsg = "Error: SMILEReader failed to load "+type.getName()+" file: " + infile.getPath() + "\nDescription: " + e.getMessage();
			System.err.println( errmsg );
			throw e;
		}

		if( FLAG_FATAL_ERROR ) return false;
		else
		{
			Map mapNodesToShells = makeShells( myMap, myIDsToFVars );
			myGenieNet.induceGraph( mapNodesToShells );
			myGenieNet.getDSLSubmodelFactory().setMain( myGenieNet.getDSLSubmodelFactory().forHandle( mainSubmodelHandle ) );
			//myGenieNet.getProperties().put( DSLConstants.keyISDSL, DSLConstants.keyISDSL );
			Map properties = myGenieNet.getProperties();
			myGenieNet.makeUserEnumProperties( properties );
			NetworkIO.normalizeEnumProperties( myGenieNet );
			return true;
		}
	}

	/** @since 020304 */
	public static boolean librarySupports( FileType type )
	{
		//Definitions.STREAM_VERBOSE.println( "SMILEReader.librarySupports( "+type+" )" );
		//Definitions.STREAM_VERBOSE.println( "FLAG_LOADLIBRARY_FAILED? " + FLAG_LOADLIBRARY_FAILED );
		if( FLAG_LOADLIBRARY_FAILED ) return false;
		else return safeLibrarySupports( type );
	}

	/** @since 020404 */
	private static boolean safeLibrarySupports( FileType type )
	{
		try{
			return librarySupports( type.getGenieConstant() );
		}catch( Throwable e ){
			return false;
		}
	}

	//private DSLNode lastVariableLoaded = null;
	private Map myMap = null;
	private Map myIDsToFVars = null;

	public GenieNet getBeliefNetwork()
	{
		return myGenieNet;
	}

	public boolean updateSMILE( File oldFile, FileType originalSMILEType, File outfile, FileType newSMILEType, GenieNet theNet )
	{
		return updateSMILE( oldFile, originalSMILEType, outfile, newSMILEType, theNet, theNet );
	}

	/** @since 052802 */
	public boolean updateSMILE( File oldFile, FileType originalSMILEType, File outfile, FileType newSMILEType, GenieNet theNet, Collection modifiedFVars )
	{
		if( FLAG_LOADLIBRARY_FAILED ) return false;

		if( !oldFile.exists() )
		{
			System.err.println( "Error: " + oldFile.getPath() + " does not exist.");
			return false;
		}

		int numnodes = (modifiedFVars == null) ? (int)0 : modifiedFVars.size();
		String[] ids = new String[numnodes];
		double[][] probs = new double[numnodes][];

		//locals
		String tempID = null;
		FiniteVariable tempFVar = null;
		DSLNode tempDSLNode = null;
		Table tempCPT = null;
		double[] tempData = null;
		List tempWeights = null;
		CPTShell shell = null;
		String[][] arraysVariableKeys = new String[numnodes][];
		String[][] arraysVariableValues = new String[numnodes][];
		int[][] dslArgs = new int[numnodes][];

		String[] tempKeys = null;
		String[] tempValues = null;
		int[] tempDSLArgs = null;
		Object tempKey = null;
		EnumProperty tempEnumProperty = null;
		Map enumProperties = null;
		Map tempMapping = new HashMap();
		int k=0;
		int l=0;
		int i = 0;

		boolean success = false;
		try{
			for( Iterator varIt = modifiedFVars.iterator(); varIt.hasNext() && i < numnodes; i++ )
			{
				tempFVar = (FiniteVariable)(varIt.next());
				if( FLAG_VERBOSE ) Definitions.STREAM_VERBOSE.println( "Java will call SMILE to update node: " + tempFVar );
				tempDSLNode = (DSLNode) tempFVar;//theNet.getHNodeForFVar( tempFVar );
				tempID = tempDSLNode.getID();
				shell = tempFVar.getCPTShell( tempFVar.getDSLNodeType() );
				if ( tempDSLNode.getDSLNodeType() == DSLNodeType.NOISY_OR )
				{
					//tempWeights = (List)( tempDSLNode.getProperties().get( DSLConstants.keyNOISYORWEIGHTS ) );
					//tempData = new double[ tempWeights.size() ];
					//for( int j=0; j < tempData.length; j++ ) tempData[j] = ((Double)tempWeights.get(j)).doubleValue();
					tempData = ((NoisyOrShellPearl)shell).weightsClone();
				}
				else
				{
					tempCPT = shell.getCPT();//theNet.getCPT( tempFVar );
					tempData = tempCPT.dataclone();
				}

				ids[i] = tempID;
				probs[i] = tempData;

				tempMapping.clear();
				enumProperties = tempFVar.getEnumProperties();
				for( Iterator mapIt = enumProperties.keySet().iterator(); mapIt.hasNext(); )
				{
					tempEnumProperty = (EnumProperty) mapIt.next();
					if( ! tempEnumProperty.isTransient() ) tempMapping.put( tempEnumProperty.getID(), enumProperties.get( tempEnumProperty ) );
				}

				if( tempDSLNode.getExcludeArray() != null ){
					tempMapping.put( PropertySuperintendent.KEY_EXCLUDEARRAY, ExcludePolicy.makeExcludeString( tempDSLNode, tempDSLNode.getExcludeArray() ) );
				}

				if( FLAG_VERBOSE ) Definitions.STREAM_VERBOSE.println( "non-transient enum properties + other properties: " + tempMapping );
				arraysVariableKeys[i] = tempKeys = new String[ tempMapping.size() ];
				arraysVariableValues[i] = tempValues = new String[ tempKeys.length ];
				l=0;
				for( Iterator mapIt = tempMapping.keySet().iterator(); l<tempKeys.length && mapIt.hasNext(); l++ )
				{
					tempKey = mapIt.next();
					tempKeys[l] = tempKey.toString();
					tempValues[l] = tempMapping.get( tempKey ).toString();
				}

				tempDSLArgs = new int[ARGUMENT_LIST_SIZE_UPDATE];
				tempDSLArgs[INDEX_UPDATE_DiagnosisType] = tempDSLNode.getDiagnosisType().getSmileID();
				dslArgs[i] = tempDSLArgs;
			}

			String[] userEnumKeys = null;
			String[] userEnumValues = null;

			Collection userEnumProperties = theNet.getUserEnumProperties();
			if( userEnumProperties != null && !userEnumProperties.isEmpty() )
			{
				int numProps = userEnumProperties.size();
				userEnumKeys = new String[ numProps+1 ];
				userEnumValues = new String[ userEnumKeys.length ];

				int j=0;
				String list = "";
				EnumProperty property;
				for( Iterator iteratorProps = userEnumProperties.iterator(); iteratorProps.hasNext(); )
				{
					property = (EnumProperty) iteratorProps.next();
					list += property.getID() + ",";
					userEnumKeys[j] = property.getID();
					userEnumValues[j++] = UserEnumProperty.createString( property );
				}
				userEnumKeys[j] = PropertySuperintendent.KEY_USERPROPERTIES;
				userEnumValues[j++] = list;
			}

			success = updateDSL( oldFile.getPath(), originalSMILEType.getGenieConstant(), outfile.getPath(), newSMILEType.getGenieConstant(), ids, probs, userEnumKeys, userEnumValues, arraysVariableKeys, arraysVariableValues, dslArgs );
		}
		catch( Exception e )
		{
			System.err.println( "Error: SMILEReader failed to update "+newSMILEType.getName()+" file: " + outfile.getPath() + ". For more info, turn on verbose.\nDescription: " + e.getMessage() );
			if( FLAG_VERBOSE )
			{
				Definitions.STREAM_VERBOSE.println( Definitions.STR_VERBOSE_TRACE_MESSAGE );
				e.printStackTrace();
			}
			return false;
		}

		return success;
	}

	private void debugLoadNode( String name ){
		Definitions.STREAM_DEBUG.println( "DEBUG Adding node to network: " + name );
	}

	private static final int
	  ARGUMENT_LIST_SIZE_SANS_TARGETS_LIST    =      11,
	  INDEX_X_POSITION                        =       0,
	  INDEX_Y_POSITION                        =       1,
	  INDEX_WIDTH                             =       2,
	  INDEX_HEIGHT                            =       3,
	  INDEX_DSLNodeType                       =       4,
	  INDEX_DiagnosisType                     =       5,
	  INDEX_MANDATORY                         =       6,
	  INDEX_RANKED                            =       7,
	  INDEX_DEFAULT_STATE_INDEX               =       8,
	  INDEX_SUBMODEL_HANDLE                   =       9,
	  INDEX_NUM_TARGETS                       =      10,
	  INDEX_BEGIN_TARGETS_LIST                =      11,
	  ARGUMENT_LIST_SIZE_UPDATE               =       1,
	  INDEX_UPDATE_DiagnosisType              =       0;

	public static final int
	  DSL_CPT                                 =       0,
	  DSL_NOISY_MAX                           =       1,
	  DSL_TRUTHTABLE                          =       2,
	  UNDEFINED_NODE_TYPE                     =      -1,
	  TARGET                                  =       0,
	  OBSERVATION                             =       1,
	  AUXILIARY                               =       2;

  /*public static final String
	  KEY_USERPROPERTIES	                  = "user_properties";

	/// avoid "constant folding"
	    http://weblogs.java.net/blog/mlam/archive/2007/02/software_territ_1.html

	    @since 20081112 //
	public static String
	  KEY_USERPROPERTIES_UNFOLDED             = KEY_USERPROPERTIES;*/

	/** @since 041902 */
	private void loadChildOfRootSubmodels( int[] handles, String[] names, int[] xcoordinates, int[] ycoordinates )
	{
		if( handles.length != names.length || names.length != xcoordinates.length || xcoordinates.length != ycoordinates.length )
		{
			System.err.println( "Java warning: SMILEReader was passed incorrect submodel data." );
		}

		DSLSubmodelFactory fact = myGenieNet.getDSLSubmodelFactory();

		//Definitions.STREAM_VERBOSE.println( "Java loading submodels:" );//debug

		DSLSubmodel tempDSLSubmodel = null;
		Point tempPoint = null;

		for( int i=0; i< handles.length; i++ )
		{
			tempDSLSubmodel = fact.forHandle( handles[i] );
			tempDSLSubmodel.setName( names[i] );
			tempPoint = new Point( xcoordinates[i], ycoordinates[i] );
			tempDSLSubmodel.setLocation( tempPoint );
			//Definitions.STREAM_VERBOSE.println( "\t" + handles[i] + " " + names[i] + " @ { " + xcoordinates[i] + ", " + ycoordinates[i] + " }" );//debug
			//Definitions.STREAM_VERBOSE.println( "\t" + handles[i] + " " + names[i] + " @ " + tempPoint );//debug
		}
	}

	/** @since 050202 */
	private void loadSubmodel( int handle, String name, int xcoord, int ycoord, int[] childHandles )
	{
		try
		{
			DSLSubmodelFactory fact = myGenieNet.getDSLSubmodelFactory();

			DSLSubmodel newDSLSubmodel = fact.forHandle( handle );
			newDSLSubmodel.setName( name );
			Point tempPoint = new Point( xcoord, ycoord );
			newDSLSubmodel.setLocation( tempPoint );

			if( FLAG_VERBOSE ) Definitions.STREAM_VERBOSE.print( "Java adding submodel: { " + handle + ", " + name + " } @ " + tempPoint + "\n\tchild submodels: " );//debug

			DSLSubmodel tempDSLSubmodel = null;
			for( int i=0; i<childHandles.length; i++ )
			{
				tempDSLSubmodel = fact.forHandle( childHandles[i] );
				newDSLSubmodel.addChildSubmodel( tempDSLSubmodel );
				if( FLAG_VERBOSE ) Definitions.STREAM_VERBOSE.print( i + "th: " + childHandles[i] + ", " );//debug
			}
		}
		catch( Exception e )
		{
			System.err.println( "Java Exception occured during SMILEReader.loadSubmodel(): " + e );
			if( FLAG_VERBOSE )
			{
				Definitions.STREAM_VERBOSE.println( Definitions.STR_VERBOSE_TRACE_MESSAGE );
				e.printStackTrace();
			}
		}

		if( FLAG_VERBOSE ) Definitions.STREAM_VERBOSE.println( "\n***********************************" );//debug
	}

	/**
		@see SMILEReader.cpp
		@author Keith Cascio
		@since 040902
		@param name Node "label"
		@param id Node id.
		@param states Node states.
		@param dsl_args <p>This is an array of all the integer/boolean/enumerated type arguments
		that we need to set up a DSLNode object.  Refer to SMILEReader.cpp for the form.</p>
	*/
	private void loadNode( String name, String id, Object[] states, int[] dsl_args, String[] parentIDs, double[] weights, int[] strengths, String[] userKeys, String[] userValues )
	{
		if( FLAG_VERBOSE )
		{
			Definitions.STREAM_VERBOSE.print( "SMILEReader.loadNode(): { " + id + ", " + name + ", " );
			Table.printArr( weights, Definitions.STREAM_VERBOSE );
			Definitions.STREAM_VERBOSE.println( " }"  );
		}

		try
		{
			//List listStates = Arrays.asList( states );
			List listStates = new LinkedList();
			for( int i=0; i<states.length; i++ ) listStates.add( states[i] );
			if( FLAG_VERBOSE ) Definitions.STREAM_VERBOSE.println( "\tstates: " + listStates );//debug

			List huginPosition = new ArrayList( (int)2 );
			huginPosition.add( new Integer( dsl_args[INDEX_X_POSITION] ) );
			//Invert the y coordinate
			//huginPosition.add( new Integer( (int)0 - dsl_args[INDEX_Y_POSITION] ) );
			huginPosition.add( new Integer( dsl_args[INDEX_Y_POSITION] ) );
			if( FLAG_VERBOSE ) Definitions.STREAM_VERBOSE.println( "\tposition: " + huginPosition );//debug

			Map values = new HashMap();

			values.put( PropertySuperintendent.KEY_HUGIN_ID, id );
			values.put( PropertySuperintendent.KEY_HUGIN_LABEL, name );
			values.put( PropertySuperintendent.KEY_HUGIN_POSITION, huginPosition );
			values.put( PropertySuperintendent.KEY_HUGIN_STATES, listStates );

			values.put( DSLConstants.KEY_POSITION_WIDTH, new Integer( dsl_args[INDEX_WIDTH] ) );
			values.put( DSLConstants.KEY_POSITION_HEIGHT, new Integer( dsl_args[INDEX_HEIGHT] ) );
			values.put( DSLConstants.KEY_EXTRADEFINITION_MANDATORY, new Boolean( dsl_args[INDEX_MANDATORY] != (int)0 ) );
			values.put( DSLConstants.KEY_EXTRADEFINITION_RANKED, new Boolean( dsl_args[INDEX_RANKED] != (int)0 ) );

			if( userKeys != null && userValues != null && userKeys.length == userValues.length )
			{
				for( int i=0; i<userKeys.length; i++ )
				{
					//Definitions.STREAM_VERBOSE.println( "\tputting " + userKeys[i] + " -> " + userValues[i] );
					values.put( userKeys[i], userValues[i] );
				}
			}

			int intDSLNodeType = dsl_args[ INDEX_DSLNodeType ];
			DSLNodeType nodeType = null;
			if( intDSLNodeType == DSL_CPT ) nodeType = DSLNodeType.CPT;
			else if( intDSLNodeType == DSL_NOISY_MAX )
			{
				nodeType = DSLNodeType.NOISY_OR;
				values.put( DSLConstants.keyNOISYOR, DSLConstants.keyNOISYOR );
				//if( weights.length > 0 )
				//{
				//	List listWeights = new ArrayList( weights.length );
				//	for( int i=0; i<weights.length; i++ ) listWeights.add( new Double( weights[i] ) );
				//	values.put( DSLConstants.keyNOISYORWEIGHTS, listWeights );
				//}
			}
			else if( intDSLNodeType == DSL_TRUTHTABLE ) nodeType = DSLNodeType.TRUTHTABLE;
			values.put( DSLConstants.KEY_TYPE, nodeType );

			int intDiagnosisType = dsl_args[ INDEX_DiagnosisType ];
			DiagnosisType dType = DiagnosisType.forSmileID( intDiagnosisType );
			values.put( DSLConstants.KEY_EXTRADEFINITION_DIAGNOSIS_TYPE, dType );
			if( FLAG_VERBOSE ) Definitions.STREAM_VERBOSE.println( "\tintDiagnosisType: " + intDiagnosisType + ", DiagnosisType: " + dType );

			if( dsl_args[INDEX_DEFAULT_STATE_INDEX] != (int)-1 )
			{
				values.put( DSLConstants.KEY_EXTRADEFINITION_SETASDEFAULT, Boolean.TRUE );
				values.put( DSLConstants.KEY_EXTRADEFINITION_DEFAULT_STATE, new Integer( dsl_args[INDEX_DEFAULT_STATE_INDEX] ) );
			}

			List listTargets = null;
			if( dsl_args[INDEX_NUM_TARGETS] > 0 )
			{
				int numtargets = dsl_args[INDEX_NUM_TARGETS];
				int numstates = states.length;
				listTargets = new ArrayList( numstates );
				for( int i=0; i<numstates; i++ ) listTargets.add( zero );

				for( int offset=0; offset<numtargets; offset++ )
				{
					listTargets.set( dsl_args[ INDEX_BEGIN_TARGETS_LIST+offset ], one );
				}

				values.put( DSLConstants.KEY_EXTRADEFINITION_FAULT_STATES, listTargets );
			}

			DSLNode newNode = new DSLNodeImpl( id, values );
			newNode.setDSLSubmodel( myGenieNet.getDSLSubmodelFactory().forHandle( dsl_args[ INDEX_SUBMODEL_HANDLE ] ) );

			myIDsToFVars.put( newNode.getID(), newNode );
			loadPotential( newNode, parentIDs, weights, strengths, intDSLNodeType );
		}
		catch( Exception e )
		{
			System.err.println( "Java Exception occured during SMILEReader.loadNode(): " + e );
			if( FLAG_VERBOSE )
			{
				Definitions.STREAM_VERBOSE.println( Definitions.STR_VERBOSE_TRACE_MESSAGE );
				e.printStackTrace();
			}
		}

		if( FLAG_VERBOSE ) Definitions.STREAM_VERBOSE.println( "Java added node to network: { " + id + ", " + name + " }" );//debug
	}

	private static final Integer one = new Integer( (int)1 );
	private static final Integer zero = new Integer( (int)0 );

	private void putNetworkParameter( Object key, Object value )
	{
		myGenieNet.getProperties().put( key, value );
		if( FLAG_VERBOSE ) Definitions.STREAM_VERBOSE.println( "Java put network parameter: { " + key + ", " + value + " }" );//debug
	}

	private void putNetworkParameter( Object key, int value )
	{
		myGenieNet.getProperties().put( key, new Integer( value ) );
		if( FLAG_VERBOSE ) Definitions.STREAM_VERBOSE.println( "Java put network parameter: { " + key + ", " + value + " }" );
	}

	/** @since 051403 */
	static protected Map makeShells( Map mapNodesToInfos, Map mapIDsToFVars ) throws Exception
	{
		Map ret = new HashMap( mapNodesToInfos.size() );

		Object currentNode;
		CPTShell currentShell;
		for( Iterator it = mapNodesToInfos.keySet().iterator(); it.hasNext(); )
		{
			currentNode = it.next();
			currentShell = ((PotentialInfo) mapNodesToInfos.get( currentNode )).makeShell( mapIDsToFVars );
			ret.put( currentNode, currentShell );
		}

		return ret;
	}

	private void loadPotential( DSLNode newNode, String[] parentIDs, double[] probabilities, int[] strengths, int intDSLNodeType )
	{
		try
		{
			PotentialInfo info = null;

			if( intDSLNodeType == DSL_NOISY_MAX ) info = new PotentialInfo( newNode, parentIDs, probabilities, strengths, intDSLNodeType );
			else
			{
				List joints = new LinkedList();
				joints.add( newNode.getID() );
				List conditioned = new LinkedList();
				if( parentIDs != null )
				{
					for( int i=0; i < parentIDs.length; i++ )
					{
						conditioned.add( parentIDs[i] );
					}
				}

				Map values = new HashMap();
				List listData = new ArrayList( probabilities.length );
				for( int i=0; i<probabilities.length; i++ ) listData.add( new Double( probabilities[i] ) );
				values.put( PropertySuperintendent.KEY_HUGIN_potential_data, listData );

				info = new PotentialInfo( new HuginPotential( joints, conditioned, values ) );
			}

			FLAG_FATAL_ERROR = ( info == null );

			myMap.put( newNode, info );
			if( FLAG_VERBOSE ) Definitions.STREAM_VERBOSE.println( "Java added: " + info + "\n****************************" );
		}
		catch( Exception e )
		{
			System.err.println( "Java Exception occured during SMILEReader.loadPotential(): " + e );
			if( FLAG_VERBOSE )
			{
				Definitions.STREAM_VERBOSE.println( Definitions.STR_VERBOSE_TRACE_MESSAGE );
				e.printStackTrace();
			}
		}
	}

	public static void main( String[] args )
	{
		if( args.length > 0 )
		{
			SMILEReader smile = new SMILEReader();
			File f = new File( args[0] );
			try
			{
				Definitions.STREAM_VERBOSE.println( "testing " + SMILEReader.class.getName() + "..." );
				Definitions.STREAM_VERBOSE.println( "turning on verbose mode, FLAG_VERBOSE = true...\n" );
				SMILEReader.FLAG_VERBOSE = true;
				FileType type = FileType.getTypeForFile( f );
				smile.loadSMILE( f, type );
			}
			catch( Exception e )
			{
				System.err.println( e );
				e.printStackTrace();
			}

			//smile.updateSMILE( f, smile.myGenieNet );
		}
		else System.err.println( "Usage: java SMILEReader <filename>" );
	}

	private static boolean FLAG_LOADLIBRARY_FAILED = false;
	static
	{
		try{
			System.loadLibrary( STR_NATIVE_LIBRARY_NAME );
		}catch( Error e ){
			FLAG_LOADLIBRARY_FAILED = true;
			if( FLAG_VERBOSE ) System.err.println( "Warning: failed to load library " + System.mapLibraryName( STR_NATIVE_LIBRARY_NAME ) );
			//Definitions.STREAM_VERBOSE.println( "FLAG_LOADLIBRARY_FAILED? " + FLAG_LOADLIBRARY_FAILED );
		}
	}

	private native int loadDSL( String ifname, int genieType );
	private native boolean updateDSL( String originalFileName, int originalSMILEType, String ofname, int newSMILEType, String[] ids, double[][] probs, String[] userKeys, String[] userValues, String[][] arraysVariableKeys, String[][] arraysVariableValues, int[][] dsl_args );
	private static native boolean librarySupports( int genieType );

	private GenieNet myGenieNet = null;
}
