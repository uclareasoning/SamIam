package edu.ucla.belief.io;

import edu.ucla.belief.*;
import edu.ucla.belief.io.dsl.*;
import edu.ucla.belief.io.hugin.*;
import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;
import edu.ucla.util.UserEnumProperty;
import edu.ucla.util.JVMProfiler;
//import edu.ucla.util.JVMTI;
import edu.ucla.util.ProgressMonitorable;
//{superfluous} import edu.ucla.util.CompoundTask;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.Point;
import java.awt.Dimension;
import java.text.*;
//{superfluous} import javax.swing.ProgressMonitor;

public class NetworkIO
{
	/** @author Keith Cascio
		@since 021005 */
	public static Map parseArgs( String[] flags, String[] args, Map container ){
		return parseArgs( new HashSet( Arrays.asList( flags ) ), args, container );
	}

	/** @author Keith Cascio
		@since 021005 */
	public static Map parseArgs( Set flags, String[] args, Map container )
	{
		if( container == null ) container = new HashMap( args.length );
		String flag;
		String value;
		for( int i=0; i<args.length; i++ ){
			if( flags.contains( args[i] ) ) container.put( args[i], Boolean.TRUE );
			else{
				for( Iterator it = flags.iterator(); it.hasNext(); ){
					flag = it.next().toString();
					if( args[i].startsWith( flag ) ){
						value = args[i].substring( flag.length() );
						container.put( flag, value );
					}
				}
			}
		}
		return container;
	}

	/** @author Keith Cascio
		@since 051903 */
	public static boolean xmlAvailable()
	{
		try{
			Class.forName( "javax.xml.parsers.DocumentBuilder" );
		}catch( ClassNotFoundException e ){
			return false;
		}
		return true;
	}

	public static boolean writeNetwork( BeliefNetwork bn, File ofile ) throws IOException
	{
		PrintWriter pw = new PrintWriter(
			new BufferedWriter(new FileWriter(ofile)));
		writeNetwork(bn, pw);
		pw.close();
		return true;
	}

	/**
	* Writes out a network in the Hugin file format.
	* @param bn the BeliefNetwork to write out.
	* @param out the stream to write it out to.
	*/
	public static void writeNetwork( BeliefNetwork bn, PrintWriter out )
	{
		out.println("net\n{");
		writeUserEnumProperties( bn.getUserEnumProperties(), out );
		if( bn instanceof PropertySuperintendent ) writeValues( ((PropertySuperintendent)bn).getProperties(), out );
		out.println("}\n");

		for( Iterator iter = bn.iterator(); iter.hasNext(); )
		{
			writeNode( (FiniteVariable) iter.next(), out );
		}

		FiniteVariable fVar = null;
		for( Iterator iter = bn.iterator(); iter.hasNext(); )
		{
			fVar = (FiniteVariable) iter.next();
			writePotential( fVar.getCPTShell( DSLNodeType.CPT ).getCPT(), out );
		}
	}

	private static void writeUserEnumProperties( Collection props, PrintWriter out )
	{
		if( props == null || props.isEmpty() ) return;

		String list = "\t" + PropertySuperintendent.KEY_USERPROPERTIES + " = \"";
		EnumProperty property;
		for( Iterator it = props.iterator(); it.hasNext(); )
		{
			property = (EnumProperty) it.next();
			list += property.getID() + ",";
			writeUserEnumProperty( property, out );
		}
		list += "\";";
		out.println( list );
	}

	private static void writeUserEnumProperty( EnumProperty property, PrintWriter out )
	{
		out.println( "\t" + property.getID() + " = \"" + UserEnumProperty.createString( property ) + "\";" );
	}

	public static void normalizeEnumProperties( BeliefNetwork bn )
	{
		EnumProperty[] properties = bn.propertiesAsArray();
		Map mapIDtoProperty = new HashMap( properties.length );
		for( int i=0; i<properties.length; i++ ) mapIDtoProperty.put( properties[i].getID(), properties[i] );

		StandardNode node;
		Map nodeProps;
		Object key;
		EnumProperty prop;
		EnumValue val;
		Map mapReplacements = new HashMap( mapIDtoProperty.size() );
		for( Iterator varIt = bn.iterator(); varIt.hasNext(); )
		{
			node = (StandardNode) varIt.next();
			nodeProps = node.getProperties();
			mapReplacements.clear();
			for( Iterator propsIt = nodeProps.keySet().iterator(); propsIt.hasNext(); )
			{
				key = propsIt.next();
				if( mapIDtoProperty.containsKey( key ) )
				{
					prop = (EnumProperty) mapIDtoProperty.get( key );
					val = prop.forString( nodeProps.get( key ).toString() );
					mapReplacements.put( prop, val );
				}
			}
			for( Iterator repIt = mapReplacements.keySet().iterator(); repIt.hasNext(); )
			{
				prop = (EnumProperty) repIt.next();
				val = (EnumValue) mapReplacements.get( prop );
				node.setProperty( prop, val );
			}
		}
	}

	//private static void writeNode( StandardNode fv, PrintWriter out )
	private static void writeNode( FiniteVariable fv, PrintWriter out )
	{
		//System.out.println( "NetworkIO.writeNode( ("+fv.getClass().getName()+")" + fv.getID() + " ), props: " + fv.getProperties() );

		out.println( "node "+fv.getID() );
		out.println("{");
		out.print( "\t" + PropertySuperintendent.KEY_HUGIN_STATES + " = (" );
		for( int i = 0; i < fv.size(); i++ ) out.print("\""+fv.instance(i) + "\" ");
		out.println(");");

		theUtilMap.clear();

		if( fv instanceof StandardNode )
		{
			StandardNode standardnode = (StandardNode)fv;
			standardnode.getLocation( theUtilPoint );
			out.println( "\t" + PropertySuperintendent.KEY_HUGIN_POSITION + " = ("+theUtilPoint.x+" "+theUtilPoint.y+");" );

			Map nodeProperties = standardnode.getProperties();
			if( nodeProperties != null ) theUtilMap.putAll( nodeProperties );
		}

		//if( fv instanceof DSLNode )
		//{
		//	fv.getDimension( theUtilDimension );
		//	out.println( "\t" + PropertySuperintendent.KEY_HUGIN_NODE_SIZE + " = ("+theUtilDimension.width+" "+theUtilDimension.height+");" );
		//}

		Map.Entry entry;
		EnumProperty key;
		for( Iterator iter = fv.getEnumProperties().entrySet().iterator(); iter.hasNext(); )
		{
			entry = (Map.Entry) iter.next();
			key = (EnumProperty)entry.getKey();
			if( !key.isTransient() ) theUtilMap.put( key.getID(), entry.getValue() );
		}

		writeValuesSansStates( theUtilMap, out );

		out.println("}");
	}

	protected static final Map theUtilMap = new HashMap();
	protected static final Point theUtilPoint = new Point();
	protected static final Dimension theUtilDimension = new Dimension();

	private static void writePotential(Table table, PrintWriter out)
	{
		List vars = table.variables();
		int indexChild = vars.size() - 1;
		FiniteVariable fVar = (FiniteVariable) vars.get( indexChild );
		out.print( PropertySuperintendent.KEY_HUGIN_potential + " ( "+ fVar.getID() + " | ");
		for( int i = 0; i < indexChild; i++ )
		{
			out.print( ((FiniteVariable) vars.get(i)).getID() + " " );
		}

		int[] sizes = new int[vars.size()];
		for(int i = 0; i < sizes.length; i++)
		{
			sizes[i] = ((FiniteVariable) vars.get(i)).size();
		}

		out.print( ")\n{\n\t" + PropertySuperintendent.KEY_HUGIN_potential_data + " = " );
		writeBlock(sizes, 0, 0, table.dataclone(), out);
		out.println( ";\n}" );
	}

	private static int writeBlock(int[] blockSizes, int var, int start, double[] data, PrintWriter out)
	{
		out.print("(");
		if (var == blockSizes.length - 1)
		{
			for (int i = 0; i < blockSizes[var]; i++)
			{
				out.print("\t"+data[start]);
				start++;
			}

			out.print("\t");
		}
		else
		{
			for (int i = 0; i < blockSizes[var]; i++)
			{
				start = writeBlock(blockSizes, var + 1, start, data, out);
				if (i + 1 < blockSizes[var])
				{
					out.print("\n\t\t");
				}
			}
		}

		out.print(")");
		return start;
	}

	private static void writeValues(Map values, PrintWriter out)
	{
		for( Iterator iter = values.entrySet().iterator(); iter.hasNext(); )
		{
			Map.Entry entry = (Map.Entry) iter.next();
			Object key = entry.getKey();
			out.print("\t"+ key + " = ");
			Object val = entry.getValue();

			if( val instanceof Map ) writeMap( (Map)val, key, out );
			else
			{
				writeValue( val, 0, out);
				out.println(";");
			}
		}
	}

	private static void writeValuesSansStates(Map values, PrintWriter out)
	{
		Map.Entry entry = null;
		for( Iterator iter = values.entrySet().iterator(); iter.hasNext(); )
		{
			entry = (Map.Entry) iter.next();
			Object key = entry.getKey();
			if( key.toString().equals( PropertySuperintendent.KEY_HUGIN_STATES ) ) continue;

			Object val = entry.getValue();
			out.print( "\t"+ key + " = " );

			if( val instanceof Map ) writeMap( (Map)val, key, out );
			else if( key.toString().equals( PropertySuperintendent.KEY_HUGIN_SUBTYPE ) ){
				out.println( val + ";" );
			}
			else{
				writeValue( val, 0, out );
				out.println(";");
			}
		}
	}

	/** @since 041304 */
	private static void writeValue( Object value, int indent, PrintWriter out )
	{
		if( value instanceof List ) writeList( (List) value, indent, out );
		else if( value instanceof Map ) System.err.println( "Error detected in NetworkIO.writeValue()" );//this case should never happen
		else out.print( "\"" + value + "\"" );
	}

	/** @since 041304 */
	private static void writeListValue( Object value, int indent, PrintWriter out )
	{
		if( value instanceof String ){
			out.print( "\""+ value + "\"" );
		}
		else if( value instanceof Number ){
			out.print(value);
		}
		else if( value instanceof List ){
			writeList((List) value, indent, out);
		}
		else if( value instanceof Map ){
			//fail,
			//do nothing,
			//nested Maps not supported
			System.err.println( "Error detected in NetworkIO.writeListValue()" );
		}
		else out.print( "\""+ value + "\"" );//Keith Cascio 031902
	}

	private static void writeMap( Map map, Object key, PrintWriter out )
	{
		out.print( HuginReader.STR_MAP_TOKEN + ";\n" );
		Map.Entry entry = null;
		String strSubordinateKey = null;
		for( Iterator it = map.entrySet().iterator();
		it.hasNext();)
		{
			entry = (Map.Entry) it.next();
			strSubordinateKey = entry.getKey().toString();
			if( strSubordinateKey.indexOf( "\"" ) != (int)-1 )
			{
				//fail,
				//do nothing,
				//Map keys with quotes not supported
				continue;
			}

			String new_key = key.toString() + strSubordinateKey;
			out.print("\t"+ new_key + " = ");
			Object val = entry.getValue();

			if( val instanceof Map ) writeMap( (Map)val, key, out );
			else
			{
				writeListValue( val, 0, out);
				out.println(";");
			}
		}
	}

	private static void writeList(List list, int indent, PrintWriter out)
	{
		if (indent == 0 && list.size() > 0 && list.get(0) instanceof List)
		{
			out.print("\n\t");
		}

		out.print("(");
		if (list.size() > 0)
		{
			writeListValue(list.get(0), indent + 1, out);
			StringBuffer indentBuffer = new StringBuffer(indent);
			for (int i = 0; i <= indent; i++)
			{
				indentBuffer.append(' ');
			}

			String indentSpaces = indentBuffer.toString();
			for (int i = 1; i < list.size(); i++)
			{
				Object value = list.get(i);
				if (value instanceof List)
				{
					out.print("\n\t");
					out.print(indentSpaces);
				}
				else
				{
					out.print(" ");
				}

				writeListValue(value, indent + 1, out);
			}
		}

		out.print(")");
	}

	/** @since 120203 */
	public static String extractFileNameFromPath( String path )
	{
		int index = path.lastIndexOf( File.separatorChar );
		++index;
		if( index < path.length() ) return path.substring( index );
		else return "";
	}

	/** @since 120203 */
	public static String extractNetworkNameFromPath( String path )
	{
		String filename = extractFileNameFromPath( path );
		int index = filename.lastIndexOf( '.' );
		return filename.substring( 0, index );
	}

	/** @since 082103 */
	public static BeliefNetwork read( String path ) throws Exception
	{
		return read( new File( path ) );
	}

	/** @since 120103 */
	public static BeliefNetwork read( File fileNetwork ) throws Exception
	{
		BeliefNetwork bn = null;

		if( FileType.HUGIN.getFileFilter().accept( fileNetwork ) ) bn = readHuginNet( fileNetwork );
		else bn = readSMILE( fileNetwork );

		return bn;
	}

	/** Reads a hugin description.
		Does not try to estimate the network size
		for the purposes of progress monitoring.

		@param networkFile file to read
		@return A HuginNet representation described in the file
	*/
	public static BeliefNetwork readHuginNet( File networkFile ) throws Exception {
		return readNetwork( new RunReadHugin( networkFile, (BeliefNetworkIOListener)null, false ) );
	}

	/** @since 20040210 */
	public static BeliefNetwork readHuginNet( Reader input ) throws Exception {
		return readNetwork( new RunReadHugin( input, "unknown", (BeliefNetworkIOListener)null ) );
	}

	/** Does not try to estimate the network size
		for the purposes of progress monitoring.

		@since 20040210 */
	public static BeliefNetwork readHuginNet( InputStream stream ) throws Exception {
		return readNetwork( new RunReadHugin( stream, "unknown", (BeliefNetworkIOListener)null, false ) );
	}

	/** @since 20040210 */
	private static BeliefNetwork readNetwork( RunReadNetwork runreadnetwork ) throws Exception
	{
		boolean flagProf   = Definitions.DEBUG && JVMProfiler.profilerRunning();
		long    startnanos = 0, endnanos = 0, elapsednanos = 0;

		if( flagProf ) startnanos = JVMProfiler.getCurrentThreadCpuTime();

		//BeliefNetwork ret = hr.beliefNetwork();
		//normalizeEnumProperties( ret );
		BeliefNetwork ret = runreadnetwork.computeResult();

		if( flagProf ){
			endnanos     = JVMProfiler.getCurrentThreadCpuTime();
			elapsednanos = endnanos - startnanos;
			Definitions.STREAM_VERBOSE.println( "read network in " + elapsednanos + " nanos" );
		}

		return ret;
	}

	public static BeliefNetwork readSMILE( File networkFile ) throws Exception {
		return readSMILE( networkFile, (FileType)null );
	}

	/** @since 20040203 */
	public static BeliefNetwork readSMILE( File networkFile, FileType type ) throws Exception {
		return readNetwork( new RunReadSMILE( networkFile, type, (BeliefNetworkIOListener)null ) );
	}

	/** Skims the file once before actually parsing it
		in order to estimate network size
		for the purposes of
		progress monitoring.

		@since 20020521 */
	public static Thread readHuginNet( File f, BeliefNetworkIOListener bnil ){
		return new RunReadHugin( f, bnil, true ).start();
	}

	/** Skims the file once before actually parsing it
		in order to estimate network size
		for the purposes of
		progress monitoring.

		@since 20040210 */
	public static Thread readHuginNet( InputStream stream, String name, BeliefNetworkIOListener bnil ){
		return new RunReadHugin( stream, name, bnil, true ).start();
	}

	/** @since 20040210 */
	public static Thread readHuginNet( Reader input, String name, BeliefNetworkIOListener bnil ){
		return new RunReadHugin( input, name, bnil ).start();
	}

    /** @since 20020521 */
	public static Thread readSMILE( File networkFile, BeliefNetworkIOListener bnil ){
		return readSMILE( networkFile, (FileType)null, bnil );
	}

	/** @since 20040203 */
	public static Thread readSMILE( File networkFile, FileType type, BeliefNetworkIOListener bnil ){
		return new RunReadSMILE( networkFile, type, bnil ).start();
	}

	public static final int INT_PRIORITY_READ = ((Thread.NORM_PRIORITY - Thread.MIN_PRIORITY)/(int)2) + Thread.MIN_PRIORITY;

	/** @author keith cascio
		@since 20020521 */
	public interface BeliefNetworkIOListener
	{
		public void handleNewBeliefNetwork( BeliefNetwork bn, File f );//, ProgressHolder progressholder );
		public void handleBeliefNetworkIOError( String msg );
		//public ProgressMonitorable waitForProgressMonitorable( BeliefNetwork bn ) throws InterruptedException;
		public void handleProgress( ProgressMonitorable readTask, Estimate estimate );
		public void handleCancelation();
		public void handleSyntaxErrors( String[] errors, FileType filetype );
		public ThreadGroup getThreadGroup();
	}

	/** @since 20050601 */
	public static boolean saveFileAs( BeliefNetwork bn, File newFile ) throws IOException{
		return saveFileAs( bn, newFile, (File)null );
	}

	/** @since 20050601 */
	public static boolean saveFileAs( BeliefNetwork bn, File newFile, File oldFile ) throws IOException
	{
		boolean success = false;
		boolean saveAsNet = false;
		boolean saveAsSMILE = false;

		final FileType typeExplicit = FileType.getTypeForFile( newFile );
		//FileType typeImplicit = null;
		FileType typeEffective = null;

		String pathOldFile = (oldFile == null) ? "?" : oldFile.getAbsolutePath();

		if( typeExplicit != null )
		{
			typeEffective = typeExplicit;
			if( typeExplicit.isHuginType() ) saveAsNet = true;
			else if( typeExplicit.isSMILEType() ) saveAsSMILE = true;
		}

		if( saveAsNet ){
			NetworkIO.writeNetwork( bn, newFile );
			success = true;
		}
		else if( saveAsSMILE )
		{
			final FileType typeOldFile = FileType.getTypeForFile( oldFile );
			//System.out.println( "saveAsSMILE, oldFile " + oldFile.getPath() + ", type: " + typeOldFile );
			if( !typeOldFile.isSMILEType() )
				throw new IllegalStateException( "The file " + pathOldFile + " cannot be saved to " + newFile.getName() + "\nCannot convert .net to "+typeEffective.getName()+" format." );

			//if( !oldFile.exists() ) oldFile = EMLearningDlg.findFileFromPath( currentFilePath );

			if( (oldFile != null) && typeOldFile.isSMILEType() )
			{
				FileType effectiveType = typeExplicit;
				//if( effectiveType == null ) effectiveType = typeImplicit;
				success = saveToSMILEType( (GenieNet)bn, oldFile, newFile, effectiveType );
			}
		}

		return success;
	}

	/** @since 060105 */
	public static boolean saveToSMILEType( GenieNet bn, File oldFile, File newFile, FileType smileType ) throws IOException
	{
		String typeName = smileType.getName();
		//if( !override && myNetStructureHasChanged ) throw new IllegalStateException( "Error saving in "+typeName+" format.  Changes to network structure not allowed.  No file written." );

		FileType typeOldFile = FileType.getTypeForFile( oldFile );
		if( typeOldFile == null || !typeOldFile.isSMILEType() ) throw new IllegalStateException( "Error saving in "+typeName+" format.  Original network incompatible format (not SMILE type).  No file written." );

		Collection modifiedFVars = bn;
		//boolean okayToRetitle = (smileType == typeOldFile);

		if( (new SMILEReader()).updateSMILE( oldFile, typeOldFile, newFile, smileType, bn, modifiedFVars ) ){
			return true;
		}
		else throw new RuntimeException( "Error saving in "+typeName+" format. No file written. (The network may not be of SMILE origin.)" );
	}

	/** @since 20060525 */
	public static URL findURL( String pathToResource, String altPathToFile )
	{
		if( CLASSLOADER == null ){
			CLASSLOADER = NetworkIO.class.getClassLoader();
		}
		URL iconURL = CLASSLOADER.getResource( pathToResource );

		if( iconURL == null ){
			File alt = new File( altPathToFile );
			if( alt.exists() ){
				try{
					iconURL = alt.toURL();
				} catch( java.net.MalformedURLException e ){
					iconURL = null;
				}
			}
		}
		return iconURL;
	}
	private static ClassLoader CLASSLOADER;

	public static final DecimalFormat FORMAT_PROFILE_TIME    = new DecimalFormat( "0000000000000" );
	public static final DecimalFormat FORMAT_PROFILE_PERCENT = new DecimalFormat( "000.00%" );
	public static final StringBuffer BUFFER = new StringBuffer( 32 );
	public static final FieldPosition FIELDPOSITION = new FieldPosition(DecimalFormat.INTEGER_FIELD);

	public static String formatTime( long time ){
		BUFFER.setLength(0);
		FORMAT_PROFILE_TIME.format( time, BUFFER, FIELDPOSITION );

		int limit = BUFFER.length() - 1;
		for( int i=0; i<limit; i++ ){
			if( BUFFER.charAt(i) == '0' ) BUFFER.setCharAt( i, ' ' );
			else break;
		}

		return BUFFER.toString();
	}

	public static String formatPercent( float ratio, DecimalFormat format ){
		BUFFER.setLength(0);
		format.format( ratio, BUFFER, FIELDPOSITION );

		int limit = BUFFER.length() - 1;
		for( int i=0; i<limit; i++ ){
			if( BUFFER.charAt(i) == '0' ) BUFFER.setCharAt( i, ' ' );
			else break;
		}

		return BUFFER.toString();
	}
}
