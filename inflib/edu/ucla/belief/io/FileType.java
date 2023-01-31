package edu.ucla.belief.io;

import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.io.dsl.SMILEReader;
import edu.ucla.belief.io.xmlbif.RunReadBIF;
import edu.ucla.belief.io.xmlbif.RunWriteBIF;

import javax.swing.JFileChooser;
import javax.swing.filechooser.*;
import java.util.List;
import java.util.LinkedList;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.File;
import java.io.PrintStream;
import java.io.FileOutputStream;

/** represents a type of Bayesian network file, e.g. .net, .dsl, etc
	@author keith cascio
	@since  20030515 */
public class FileType
{
	private static Collection REGISTRY = new ArrayList( (int)10 );

	public FileFilter getFileFilter()
	{
		return myFileFilter;
	}

	public String getName()
	{
		return myName;
	}

	public int getGenieConstant()//defined constants.h
	{
		return myGenieConstant;
	}

	public boolean isSMILEType()
	{
		return myFlagSMILEType;
	}

	public boolean isHuginType()
	{
		return myFlagHuginType;
	}

	public boolean isReadable(){
		return true;
	}

	public boolean isWritableFrom( FileType type ){
		return (type == this) || ((type != null) && (this.isSMILEType() == type.isSMILEType()));
	}

	/** a.k.a.: {@link #distinguishes distinguishes()}
	    @since 20070228
	    @return true if this type is equivalent to the argument type or a subtype of it */
	public boolean narrows( FileType type ){
		return type == this;
	}

	/** alias of {@link #narrows narrows()}
	    @since 20070228 */
	final public boolean distinguishes( FileType type ){
		return this.narrows( type );
	}

	/** a.k.a.: {@link #generalizes generalizes()}
	    @since 20070228
	    @return true if this type is equivalent to the argument type or a supertype of it */
	public boolean broadens( FileType type ){
		return type == this;
	}

	/** alias of {@link #broadens broadens()}
	    @since 20070228 */
	final public boolean generalizes( FileType type ){
		return this.broadens( type );
	}

	public String toString()
	{
		return myFileFilter.getDescription();
	}

	/** @since 20060525 */
	public String getURL(){
		return myURL;
	}

	/** @since 20060524 */
	public Thread openFile( File input, NetworkIO.BeliefNetworkIOListener listener ){
		if(      this.myFlagHuginType ) return NetworkIO.readHuginNet( input,       listener );
		else if( this.myFlagSMILEType ) return NetworkIO.readSMILE(    input, this, listener );
		else{
			if( listener != null ) listener.handleBeliefNetworkIOError( "don't know how to read " + getName() + " format" );
			return null;
		}
	}

	/** @since 20060525 */
	public boolean save( File output, BeliefNetwork bn ) throws Exception {
		if( this.myFlagHuginType ) return NetworkIO.writeNetwork( bn, output );
		else{
			throw new RuntimeException( "don't know how to write " + getName() + " format" );
		}
	}

	public File appendExtension( File selectedFile )
	{
		String path = selectedFile.getPath();
		if( path.length() > 0 ) return new File( path + myExtension );
		else return null;
	}

	protected FileType( String name, String url, FileFilter filter, int constant, String extension, boolean flagSMILEType, boolean flagHuginType )
	{
		myName = name;
		myURL  = url;
		myFileFilter = filter;
		myGenieConstant = constant;
		myExtension = extension;
		myFlagSMILEType = flagSMILEType;
		myFlagHuginType = flagHuginType;

		REGISTRY.add( this );
	}

	protected String myName;
	protected String myURL;
	protected FileFilter myFileFilter;
	protected int myGenieConstant;
	protected String myExtension;
	protected boolean myFlagSMILEType;
	protected boolean myFlagHuginType;

	public static final FileType ERGO = new FileType(
		"Ergo",
		"http://www.noeticsystems.com",
		new InflibFileFilter( new String[]{ ".erg" }, "Ergo (*.erg)" ),
		(int)1, ".erg", true, false );
	public static final FileType NETICA = new FileType(
		"Netica",
		"http://www.norsys.com",
		new InflibFileFilter( new String[]{ ".dne", ".dnet" }, "Netica (*.dne, *.dnet)" ),
		(int)2, ".dne", true, false );
	public static final FileType INTERCHANGE = new FileType(
		"Interchange",
		"http://research.microsoft.com/adapt/MSBNx",
		new InflibFileFilter( new String[]{ ".dsc" }, "Interchange (*.dsc)" ),
		(int)3, ".dsc", true, false );
	public static final FileType DSL = new FileType(
		"Decision Systems Lab",
		"http://genie.sis.pitt.edu",
		new InflibFileFilter( new String[]{ ".dsl" }, "GeNIe (*.dsl)" ),
		(int)4, ".dsl", true, false );
	public static final FileType HUGIN = new FileType(
		"Hugin",
		"http://www.hugin.com",
		new InflibFileFilter( new String[]{ ".net", ".hugin", ".oobn" }, "Hugin Net [v6.*, v5.7] (*.net, *.hugin, *.oobn)" ),
		(int)5, ".net", false, true ){

		public boolean isWritableFrom( FileType type ){
			return false;
		}

		/** @since 20070228 */
		public boolean broadens( FileType type ){
			return type == this || type == HUGIN57;
		}
	};
	public static final FileType HUGIN57 = new FileType(
		"Hugin [v5.7]",
		"http://www.hugin.com",
		new InflibFileFilter( new String[]{ ".net", ".hugin" }, "Hugin Net [v5.7] (*.net, *.hugin)" ),
		(int)5, ".net", false, true ){

		public boolean isReadable(){
			return false;
		}

		/** @since 20070228 */
		public boolean narrows( FileType type ){
			return type == this || type == HUGIN;
		}
	};
	public static final FileType XMLBIF_SMILE = new FileType(
		"XML BIF",
		"http://www.cs.cmu.edu/~fgcozman/Research/InterchangeFormat",
		new InflibFileFilter( new String[]{ ".xmlbif", ".xbif", ".xml" }, "XMLBIF (*.xmlbif, *.xbif, *.xml)" ),
		(int)6, ".xbif", true, false ){
		public boolean isReadable(){
			return false;
		}

		public boolean isWritableFrom( FileType type ){
			return false;
		}
	};
	//public static final FileType KI = new FileType(
	//	"KI",
	//  "http://www.kic.com",
	//	new InflibFileFilter( new String[]{ ".dxp" }, "KI (*.dxp)" ),
	//	(int)7, ".dxp", true, false );
	public static final FileType XDSL = new FileType(
		"Decision Systems Lab Xml",
		"http://genie.sis.pitt.edu",
		new InflibFileFilter( new String[]{ ".xdsl" }, "GeNIe XML (*.xdsl)" ),
		(int)8, ".xdsl", true, false );
	//public static final FileType TEXT = new FileType(
	//	"Text Only",
	//	new InflibFileFilter( new String[]{ ".txt" }, "Text Test (*.txt)" ),
	//	(int)9, ".txt", true, false );
	/** @since 20060524 */
	public static final FileType XMLBIF_INFLIB = new FileType(
		"XML BIF",
		"http://www.cs.cmu.edu/~fgcozman/Research/InterchangeFormat",
		new InflibFileFilter( new String[]{ ".xmlbif", ".xbif", ".xml" }, "XMLBIF (*.xmlbif, *.xbif, *.xml)" ),
		(int)6, ".xbif", false, false ){

		public Thread openFile( File input, NetworkIO.BeliefNetworkIOListener listener ){
			return new RunReadBIF( input, listener ).start();
		}

		public boolean save( File output, BeliefNetwork bn ) throws Exception {
			//return new XmlbifWriter().write( bn, new PrintStream( new FileOutputStream( output ) ) );
			RunWriteBIF runwritebif = new RunWriteBIF( output, bn );
			runwritebif.run();
			return runwritebif.getResult();
		}
	};

	private static FileType[] ARRAY;// = new FileType[]{ HUGIN, DSL, XDSL, INTERCHANGE, NETICA, ERGO };
	private static FileType[] ARRAY_SMILE;// = new FileType[]{ DSL, XDSL, INTERCHANGE, NETICA, ERGO };

	/** @since 20060524 */
	public static int javaSpecificationVersion(){
		if( INT_JAVA_VERSION < 0 ){
			try{
				String strVersion = System.getProperty( "java.specification.version" );
				if( strVersion == null ) return INT_JAVA_VERSION = 0;

				int len = strVersion.length();
				String digit = null;
				if( strVersion.startsWith( "1." ) && (len >= 3) ) digit = strVersion.substring( 2,3 );
				else if( len > 0 )                                digit = strVersion.substring( 0,1 );

				if( digit == null ) return INT_JAVA_VERSION = 0;
				else                return INT_JAVA_VERSION = Integer.parseInt( digit );
			}catch( Exception exception ){
				return INT_JAVA_VERSION = 0;
			}
		}
		return INT_JAVA_VERSION;
	}
	private static int INT_JAVA_VERSION = -1;

	/** @since 20040203 */
	public static FileType[] getArrayCanonical()
	{
		if( ARRAY == null )
		{
			Collection types = new ArrayList( 10 );
			types.add( HUGIN );
			types.add( HUGIN57 );
			if( javaSpecificationVersion() >= 5 ) types.add( XMLBIF_INFLIB );
			addSupportedSMILETypes( types );
			ARRAY = (FileType[]) types.toArray( new FileType[ types.size() ] );
		}
		return ARRAY;
	}

	/** @since 20040203 */
	public static FileType[] getArraySMILE()
	{
		if( ARRAY_SMILE == null )
		{
			Collection types = new ArrayList( 10 );
			addSupportedSMILETypes( types );
			ARRAY_SMILE = (FileType[]) types.toArray( new FileType[ types.size() ] );
		}
		return ARRAY_SMILE;
	}

	/** http://genie.sis.pitt.edu/SMILEHelp/Application_Programmers_Manual/Reading_&_Writing_File_Formats.htm
		as of SMILE 20060214, support for XML BIF seems broken

		@since 20040203 */
	private static void addSupportedSMILETypes( Collection types )
	{
		FileType type;
		for( Iterator it = REGISTRY.iterator(); it.hasNext(); )
		{
			type = (FileType) it.next();
			if( type.isSMILEType() && type.isReadable() && SMILEReader.librarySupports( type ) ) types.add( type );
		}
	}

	public static final FileFilter getPanFilter()
	{
		if( myPanFilter == null )
		{
			List listExtensions = new LinkedList();
			String[] arrayTemp;
			FileType[] array = getArrayCanonical();
			int count = 0;
			for(     int i=0; i<    array.length; i++ ){ count += (arrayTemp = ((InflibFileFilter) array[i].getFileFilter()).getExtensions()).length;
				for( int j=0; j<arrayTemp.length; j++ ){ listExtensions.add( arrayTemp[j] ); }
			}
			StringBuffer buff = new StringBuffer( (array.length*8) + 64 );
			buff.append( "All Network Types ( " );
			if( count < 9 ){
				for( int i=0; i<array.length; i++ ){
					arrayTemp = ((InflibFileFilter) array[i].getFileFilter()).getExtensions();
					for( int j=0; j<arrayTemp.length; j++ ){
						listExtensions.add( arrayTemp[j] );
						buff.append( '*' );
						buff.append( arrayTemp[j] );
						buff.append( ", " );
					}
				}
			}else{
				buff.append( "*._____" );
			}

			buff.setLength( buff.length()-2 );
			buff.append( " )" );

			myPanFilter = new InflibFileFilter( (String[]) listExtensions.toArray( new String[listExtensions.size()] ), buff.toString() );
		}

		return myPanFilter;
	}

	public static final FileType getTypeForFile( File dummyFile )
	{
		FileType[] array = getArrayCanonical();
		FileType type;
		for( int i=0; i<array.length; i++ )
		{
			type = array[i];
			if( type.getFileFilter().accept( dummyFile ) ) return type;
		}

		return null;
	}

	public static final FileType getTypeForFilter( FileFilter filter )
	{
		FileType type;
		for( Iterator it = REGISTRY.iterator(); it.hasNext(); )
		{
			type = (FileType) it.next();
			if( type.getFileFilter() == filter ) return type;
		}

		return null;
	}

	public static final void load( JFileChooser chooser )
	{
		FileType[] array = getArrayCanonical();
		for( int i=0; i<array.length; i++ ){
			if( array[i].isReadable() ) chooser.addChoosableFileFilter( array[i].getFileFilter() );
		}
		chooser.addChoosableFileFilter( getPanFilter() );
	}

	/** @see #narrows
	    @see #distinguishes */
	public static final void loadForSaveAs( JFileChooser chooser, File oldFile )
	{
		//System.out.println( "FileType.loadForSaveAs("+ (( oldFile == null ) ? "null" : oldFile.getName()) +")" );
		FileType   type    = (oldFile == null) ?  null  : getTypeForFile( oldFile );
		String     name    = (oldFile == null) ? "null" : oldFile.getName();

		FileType   subtype = null;
		FileType[] array   = getArrayCanonical();
		for( int i=0; i<array.length; i++ ){
			if( array[i].isWritableFrom( type ) ){
				chooser.addChoosableFileFilter( array[i].getFileFilter() );
				if( array[i].narrows( type ) ) subtype = array[i];
			}
		}
		if( subtype == null ) System.err.println( "warning: FileType.loadForSaveAs("+name+") could not infer a writable type" );
		else chooser.setFileFilter( subtype.getFileFilter() );
	}

	protected static FileFilter myPanFilter;
}
