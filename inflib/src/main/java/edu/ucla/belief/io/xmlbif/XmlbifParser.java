package edu.ucla.belief.io.xmlbif;

import edu.ucla.belief.io.*;
import edu.ucla.belief.*;
import edu.ucla.util.JVMTI;

import java.util.List;
import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.awt.*;
import javax.xml.XMLConstants;
import javax.xml.validation.*;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.text.DecimalFormat;

//Add these lines for the exceptions that can be thrown when the XML document is parsed:
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.*;
import org.xml.sax.Attributes;

/**
	<a href=
	"http://www.cs.cmu.edu/~fgcozman/Research/InterchangeFormat">
	                xml bif format described

	</a>
	<br />

	<a href=
	"http://www.w3.org/TR/xmlschema-0">
	                xml schema validation primer

	</a>
	<br />

	<a href=
	"http://w3.org/2000/04/schema_hack/dtd2xsd.pl">
	                perl script to convert DTD to schema

	</a>
	<br />
	updated 20060531 for version 0.3a proposed by Jeff Bilmes <bilmes@cuba.ee.washington.edu> 20060529

	@author keith cascio
	@since  20060523 */
public class XmlbifParser extends AbstractSaxHandler implements RunReadBIF.MonitorableReusableParser
{
	public static final String STR_TAG_ROOT_0_3   = "BIF";
	public static final String STR_ATTR_VERSION_U = "VERSION";
	public static final String STR_ATTR_VERSION_L = "version";
	public static final String STR_TAG_NETWORK    = "NETWORK";

	public static final String STR_TAG_VARIABLE   = "VARIABLE";
	public static final String STR_ATTR_TYPE_U    = "TYPE";
	public static final String STR_ATTR_TYPE_L    = "type";
	public static final String STR_TAG_NAME       = "NAME";
	public static final String STR_TAG_OUTCOME    = "OUTCOME";
	public static final String STR_TAG_PROPERTY   = "PROPERTY";

	public static final String STR_TAG_DEFINITION_0_3 = "DEFINITION";
	public static final String STR_TAG_FOR        = "FOR";
	public static final String STR_TAG_GIVEN      = "GIVEN";
	public static final String STR_TAG_TABLE      = "TABLE";

	public static final String STR_VERSION_0_3    = "0.3";
	//public static final double DOUBLE_VERSION_0_3 =  0.3;

	public static final String STR_VERSION_0_3A         = "0.3a";
	public static final String STR_TAG_ROOT_0_3A        = "xbif";
	public static final String STR_TAG_ANONYMOUS_VALUES = "values";
	public static final String STR_TAG_OBSERVATION      = "observed";
	public static final String STR_TAG_PROBABILITY_0_3A = "probability";

	public  static final String[] ARRAY_VALID_ROOT_ELEMENT = new String[] { STR_TAG_ROOT_0_3A,        STR_TAG_ROOT_0_3       };
	public  static final String[] ARRAY_VERSIONS_SUPPORTED = new String[] { STR_VERSION_0_3A,         STR_VERSION_0_3        };
	public  static final String[] ARRAY_TAGS_POTENTIAL     = new String[] { STR_TAG_PROBABILITY_0_3A, STR_TAG_DEFINITION_0_3 };
	private String myTagPotential = null;

	public enum VariableType{
		nature( true ), discrete( true ), decision( false ), utility( false );

		private VariableType( boolean isNature ){
			myFlagIsNature = isNature;
		}

		public boolean isNatural(){
			return myFlagIsNature;
		}

		private boolean myFlagIsNature = false;
	};

	public boolean versionSupported( String version ){
		try{
			for( int i=0; i<ARRAY_VERSIONS_SUPPORTED.length; i++ ){
				if( ARRAY_VERSIONS_SUPPORTED[i].equalsIgnoreCase( version ) ){
					myTagPotential = ARRAY_TAGS_POTENTIAL[i];
					return true;
				}
			}
			//if( Double.parseDouble(     version ) == DOUBLE_VERSION_0_3 ) return true;
		}catch( Exception exception ){
			System.err.println( "warning: XmlbifParser.versionSupported() caught " + exception );
		}
		return false;
	}

	public boolean isValidating(){
		return false;
	}

	public boolean isValidationErrorFatal(){
		return false;
	}

	public boolean isValidRootElementName( String qname ){
		//return STR_TAG_ROOT_0_3.equalsIgnoreCase( qname );
		for( String valid : ARRAY_VALID_ROOT_ELEMENT ){
			if( valid.equalsIgnoreCase( qname ) ) return true;
		}
		return false;
	}

	/** @since 20060614 */
	public static boolean isOneOf( String qname, String[] alternatives ){
		for( String alternative : alternatives ){
			if( alternative.equalsIgnoreCase( qname ) ) return true;
		}
		return false;
	}

	/** @since 20060530 */
	public static String getValue( Attributes attributes, String[] alternates ){
		String ret = null;
		int i = 0;
		for(; (ret == null) && (i<alternates.length); i++ ){
			ret = attributes.getValue( alternates[i] );
		}
		if( (i>0) && (ret != null) ){
			String winner = alternates[i];
			alternates[i] = alternates[0];
			alternates[0] = winner;
		}
		return ret;
	}

	/** @since 20060530 */
	public static String getValueIgnoreCase( Attributes attributes, String name ){
		int len = attributes.getLength();
		for( int i=0; i<len; i++ ){
			if( attributes.getQName(i).equalsIgnoreCase( name ) ) return attributes.getValue(i);
		}
		return null;
	}

	public ElementHandler getValidRootHandler( String qName, Attributes attributes ){
		String version = getValueIgnoreCase( attributes, STR_ATTR_VERSION_U );
		if( version == null ) throw new RuntimeException( "xml bif file must declare version as attribute of root tag" );
		if( !versionSupported( version ) ) throw new RuntimeException( "unsupported xml bif file format version \"" + version + "\"" );
		return theValidRootHandler;
	}

	public static Schema getSchema(){
		if( SCHEMA == null ){
			try{
				SchemaFactory factory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
				String raw = "edu/ucla/belief/io/xmlbif/bif.xsd";
				String alt = "./../inflib/" + raw;
				java.net.URL url = NetworkIO.findURL( raw, alt );
				if( url != null ) SCHEMA = factory.newSchema( url );
			}catch( SAXParseException saxparseexception ){
				System.err.println( "warning: failed to create xml validation schema, line " + saxparseexception.getLineNumber() + ", col " + saxparseexception.getColumnNumber() + " " + saxparseexception );
				SCHEMA = null;
			}catch( SAXException saxexception ){
				System.err.println( "warning: failed to create xml validation schema, " + saxexception );
				SCHEMA = null;
			}
		}
		return SCHEMA;
	}

	/** interface RunReadBIF.MonitorableReusableParser
		@since 20060622 */
	public void setHighPerformance( boolean flag ){
		if( myFlagHighPerformance == flag ) return;

		myFlagHighPerformance = flag;
		mySAXParser           = null;
		if( myFactory != null ) configureFactory( myFactory );
	}

	public void configureFactory( SAXParserFactory factory ){
		try{
			factory.setFeature( "http://xml.org/sax/features/string-interning", true );
		}catch( ParserConfigurationException parserconfigurationexception ){
			System.err.println( "warning: unable to configure sax factory to intern strings, " + parserconfigurationexception );
		}catch( SAXException saxexception ){
			System.err.println( "warning: unable to configure sax factory to intern strings, " + saxexception );
		}

		if( myFlagHighPerformance ){
			factory.setSchema( null );
		}
		else{
			Schema schema = getSchema();
			if( schema != null ) factory.setSchema( schema );
		}

		boolean enable = !myFlagHighPerformance;

		factory.setValidating(     false );
		factory.setNamespaceAware( false );
		factory.setXIncludeAware(  false );

		try{
			if( REFERENCE_FACTORY == null ){
				REFERENCE_FACTORY = SAXParserFactory.newInstance();
				//System.out.println( "reference factory interns strings? " + REFERENCE_FACTORY.getFeature( "http://xml.org/sax/features/string-interning" ) );
			}
			for( String name : ARRAY_SAX_AVOIDABLE_PROPERTIES ){
				factory.setFeature( name, enable ? REFERENCE_FACTORY.getFeature( name ) : false );
			}
		}catch( ParserConfigurationException parserconfigurationexception ){
			System.err.println( "warning: failed configuring sax factory, " + parserconfigurationexception );
		}catch( SAXException saxexception ){
			System.err.println( "warning: failed configuring sax factory, " + saxexception );
		}
	}

	public static final String[] ARRAY_SAX_AVOIDABLE_PROPERTIES = new String[] {
		"http://xml.org/sax/features/external-general-entities",
		"http://xml.org/sax/features/external-parameter-entities",
		"http://xml.org/sax/features/lexical-handler/parameter-entities",
		"http://xml.org/sax/features/namespaces",
		"http://xml.org/sax/features/namespace-prefixes",
	  //"http://xml.org/sax/features/resolve-dtd-uris",
		"http://xml.org/sax/features/unicode-normalization-checking",
		"http://xml.org/sax/features/validation"
	};

	private ElementHandler theValidRootHandler = new ElementHandler()
	{
		public void startElement(String uri,String localName,String qName,Attributes attributes) throws SAXException
		{
			if(      qName.equalsIgnoreCase( STR_TAG_VARIABLE   ) ){
				myVariables.add( myVariable = new Variable() );
				String type = getValueIgnoreCase( attributes, STR_ATTR_TYPE_U );
				if( type != null ){
					myVariable.type = VariableType.valueOf( type );
					if( !myVariable.type.isNatural() ) throw new RuntimeException( "unsupported variable type \"" + myVariable.type + "\"" );
				}
				mySubHandler = theVariableHandler;
			}
			//else if( qName.equalsIgnoreCase( myTagPotential ) ){
			else if( isOneOf( qName, ARRAY_TAGS_POTENTIAL ) ){
				myDefinitions.add( myDefinition = new Definition() );
				mySubHandler = theDefinitionHandler;
			}
			else if( qName.equalsIgnoreCase( STR_TAG_NAME ) ){
				setAccumulator( myName );
			}
			else if( qName.equalsIgnoreCase( STR_TAG_PROPERTY ) ){
				myProperties.add( newAccumulator() );
			}
		}

		public void endElement(String uri,String localName,String qName) throws SAXException {
			disableAccumulator();
		}
	};

	public static final int INT_SIZE_ACCUMULATOR_INITIAL = 64;

	protected StringBuilder newAccumulator(){
		myCharactersHandler  = theCharactersAccumulator;
		return myAccumulator = new StringBuilder( INT_SIZE_ACCUMULATOR_INITIAL );
	}

	protected StringBuilder newAccumulator( int size ){
		myCharactersHandler  = theCharactersAccumulator;
		return myAccumulator = new StringBuilder( size );
	}

	protected StringBuilder setAccumulator( StringBuilder accumulator ){
		if( (myAccumulator = accumulator) == null ) throw new IllegalArgumentException( "illegal null accumulator" );
		else{
			accumulator.setLength(0);
			myCharactersHandler = theCharactersAccumulator;
		}
		return accumulator;
	}

	protected StringBuilder initAccumulator( StringBuilder accumulator, int size ){
		return ( accumulator == null ) ? newAccumulator( size ) : setAccumulator( accumulator );
	}

	protected void disableAccumulator(){
		myCharactersHandler = theCharactersNoop;
		myAccumulator       = null;
	}

	private CharactersHandler theCharactersAccumulator = new CharactersHandler()
	{
		public void characters(	char[] ch, int start, int length ) throws SAXException{
			myAccumulator.append( ch, start, length );
		}
	};

	private StringBuilder myAccumulator;

	private ElementHandler theVariableHandler = new ElementHandler()
	{
		public void startElement(String uri,String localName,String qName,Attributes attributes) throws SAXException
		{
			if(      qName.equalsIgnoreCase( STR_TAG_NAME     ) ){
				setAccumulator( myVariable.id );
			}
			else if( qName.equalsIgnoreCase( STR_TAG_ANONYMOUS_VALUES ) ){
				myVariable.anonymousCount = initAccumulator( myVariable.anonymousCount, 4 );
			}
			else if( qName.equalsIgnoreCase( STR_TAG_OBSERVATION      ) ){
				myVariable.indexObserved  = initAccumulator( myVariable.indexObserved,  4 );
			}
			else if( qName.equalsIgnoreCase( STR_TAG_OUTCOME  ) ){
				myVariable.outcomes.add( newAccumulator() );
			}
			else if( qName.equalsIgnoreCase( STR_TAG_PROPERTY ) ){
				myVariable.properties.add( newAccumulator() );
			}
			else disableAccumulator();
		}

		public void endElement(String uri,String localName,String qName) throws SAXException {
			disableAccumulator();
			if( qName.equalsIgnoreCase( STR_TAG_VARIABLE ) ){
				myVariable          = null;
				mySubHandler        = theValidRootHandler;

				if( myTask != null ) myTask.touch();
			}
		}
	};

	public static Integer intValueOf( StringBuilder accumulator ){
		if( (accumulator == null) || (accumulator.length() < 1) ) return null;
		return new Integer( accumulator.toString() );
	}

	public static class Variable{
		public StringBuilder       id         = new StringBuilder( INT_SIZE_ACCUMULATOR_INITIAL );
		public VariableType        type;
		public List<StringBuilder> outcomes   = new LinkedList<StringBuilder>();
		public List<StringBuilder> properties = new LinkedList<StringBuilder>();
		public List<String>        outcomeStrings;
		public String              idString;

		public StringBuilder       anonymousCount;// = new StringBuilder( 8 );
		public StringBuilder       indexObserved;// = new StringBuilder( 8 );

		public String toString(){
			return id.toString() + " " + type + " " + outcomes.toString() + " " + properties.toString();
		}

		public FiniteVariable toFiniteVariable(){
			return new BifNode( this );
		}

		public List<String> getOutcomes(){
			if( outcomeStrings != null ) return outcomeStrings;

			boolean hasNamedOutcomes = outcomes.size() > 0;

			Integer anon = null;
			try{
				anon = intValueOf( anonymousCount );
			}catch( NumberFormatException numberformatexception ){
				throw new RuntimeException( "could not parse # of values for variable \"" +getID()+ "\", data \"" +anonymousCount.toString()+ "\"", numberformatexception );
			}
			boolean hasAnonymousOutcomes = (anon != null);

			if( hasNamedOutcomes && hasAnonymousOutcomes ) throw new RuntimeException( "variable \"" +getID()+ "\" illegally defined with named and anonymous outcomes, i.e. <outcome> and <values>" );

			if( hasNamedOutcomes ){
				outcomeStrings = new ArrayList<String>( outcomes.size() );
				for( StringBuilder outcome : outcomes ){
					outcomeStrings.add( outcome.toString() );
				}
			}
			else if( hasAnonymousOutcomes ){
				outcomeStrings = createAnonymousOutcomes( getID(), anon.intValue() );
			}

			return outcomeStrings;
		}

		public String getID(){
			if( idString == null ) idString = id.toString();
			return idString;
		}

		public int getEvidence(){
			if( indexObserved == null ) return -1;
			Integer evidence = null;
			try{
				evidence = intValueOf( indexObserved );
			}catch( NumberFormatException numberformatexception ){
				throw new RuntimeException( "could not parse evidence observation for variable \"" +getID()+ "\", data \"" +indexObserved.toString()+ "\"", numberformatexception );
			}
			if( evidence      == null ) return -99;

			int ret = evidence.intValue();
			if( (ret < 0) || (ret >= getOutcomes().size()) ) throw new RuntimeException( "evidence == "+ret+", for variable \"" +getID()+ "\" out of range [0,"+(getOutcomes().size()-1)+"]" );

			return ret;
		}
	}

	private static final StringBuilder BUFFER_ANONYMOUS_VALUES = new StringBuilder( 64 );
	private static final DecimalFormat FORMAT_ANONYMOUS_VALUES = new DecimalFormat( "000" );
	//private static final FieldPosition FIELDPOSITION_ANONYMOUS = new FieldPosition( DecimalFormat.INTEGER_FIELD );

	public static List<String> createAnonymousOutcomes( String id, int numAnonymousValues ){
		ArrayList<String> outcomeStrings = new ArrayList<String>( numAnonymousValues );

		BUFFER_ANONYMOUS_VALUES.setLength(0);
		BUFFER_ANONYMOUS_VALUES.append( id );
		if( BUFFER_ANONYMOUS_VALUES.length() > 16 ) BUFFER_ANONYMOUS_VALUES.setLength( 16 );
		BUFFER_ANONYMOUS_VALUES.append( '_' );
		int lenPrefix = BUFFER_ANONYMOUS_VALUES.length();

		for( int i=0; i<numAnonymousValues; i++ ){
			BUFFER_ANONYMOUS_VALUES.setLength( lenPrefix );
			BUFFER_ANONYMOUS_VALUES.append( FORMAT_ANONYMOUS_VALUES.format(i) );
			outcomeStrings.add( BUFFER_ANONYMOUS_VALUES.toString() );
		}

		return outcomeStrings;
	}

	public static final String  REGEX_POSITION   = "position\\s*=\\s*\\(\\s*(-?\\d+),\\s*(-?\\d+)\\s*\\)";
	public static final Pattern PATTERN_POSITION = Pattern.compile( REGEX_POSITION, Pattern.CASE_INSENSITIVE );

	public static class BifNode extends StandardNodeImpl{
		public BifNode( Variable variable ){
			super( variable.getID(), variable.getOutcomes() );
			init( variable );
		}

		private void init( Variable variable ){
			setLabel( variable.getID() );

			Matcher matcher;
			for( StringBuilder property : variable.properties ){
				matcher = PATTERN_POSITION.matcher( property.toString() );
				if( matcher.find() ){
					huginPosition( matcher.group(1), matcher.group(2) );
				}
			}
		}

		private void huginPosition( String x, String y ){
			myProperties.put( KEY_HUGIN_POSITION, Arrays.asList( new Integer[] { new Integer(x), new Integer(y) } ) );
		}

		public void setLabel( String label ){
			myLabel = label;
			if( myProperties != null ) myProperties.put( PropertySuperintendent.KEY_HUGIN_LABEL, myLabel );
		}

		public String getLabel(){
			return myLabel;
		}

		protected Dimension makeDimension(){
			return new Dimension();
		}

		public Map getProperties(){
			return myProperties;
		}

		private String myLabel;
		private Map<Object,Object> myProperties = new HashMap<Object,Object>();
	}

	protected List<Variable> myVariables = new LinkedList<Variable>();
	protected Variable       myVariable;

	private ElementHandler theDefinitionHandler = new ElementHandler()
	{
		public void startElement(String uri,String localName,String qName,Attributes attributes) throws SAXException
		{
			if(      qName.equalsIgnoreCase( STR_TAG_FOR      ) ){
				setAccumulator( myDefinition.joint );
			}
			else if( qName.equalsIgnoreCase( STR_TAG_GIVEN    ) ){
				myDefinition.givens.add( newAccumulator() );
			}
			else if( qName.equalsIgnoreCase( STR_TAG_TABLE    ) ){
				setAccumulator( myDefinition.table );
			}
			else if( qName.equalsIgnoreCase( STR_TAG_PROPERTY ) ){
				myDefinition.properties.add( newAccumulator() );
			}
			else disableAccumulator();
		}

		public void endElement(String uri,String localName,String qName) throws SAXException {
			disableAccumulator();
			//if(      qName.equalsIgnoreCase( myTagPotential ) ){
			if( isOneOf( qName, ARRAY_TAGS_POTENTIAL ) ){
				myDefinition        = null;
				mySubHandler        = theValidRootHandler;

				if( myTask != null ) myTask.touch();
			}
		}
	};

	public static final String  REGEX_DATA_DELIMITER   = "\\s+";
	public static final Pattern PATTERN_DATA_DELIMITER = Pattern.compile( REGEX_DATA_DELIMITER, Pattern.MULTILINE );

	public static final String  REGEX_COMMENT          = "//.*$";
	public static final Pattern PATTERN_COMMENT        = Pattern.compile( REGEX_COMMENT,        Pattern.MULTILINE );
	public static final Matcher MATCHER_COMMENT        = PATTERN_COMMENT.matcher( "" );

	/** @since 20060531 */
	public static StringBuilder blankOutComments( StringBuilder buff ){
		MATCHER_COMMENT.reset( buff );
		int end = -1;
		while( MATCHER_COMMENT.find() ){
			end = MATCHER_COMMENT.end();
			for( int i=MATCHER_COMMENT.start(); i<end; i++ ){
				buff.setCharAt( i, ' ' );
			}
		}
		return buff;
	}

	public static class Definition{
		public StringBuilder       joint      = new StringBuilder( INT_SIZE_ACCUMULATOR_INITIAL );
		public List<StringBuilder> givens     = new LinkedList<StringBuilder>();
		public StringBuilder       table      = new StringBuilder( 128 );
		public List<StringBuilder> properties = new LinkedList<StringBuilder>();

		public Table toTable( BeliefNetwork bn ){
			List<FiniteVariable> vars = new ArrayList<FiniteVariable>( givens.size() + 1 );
			FiniteVariable fvConditioned = null;
			for( StringBuilder given : givens ){
				vars.add( fvConditioned = (FiniteVariable) bn.forID( given.toString() ) );
				if( fvConditioned == null ) throw new RuntimeException( "condition variable \"" +given.toString()+ "\" not found" );
			}
			FiniteVariable fvJoint = (FiniteVariable) bn.forID( joint.toString() );
			if( fvJoint == null ) throw new RuntimeException( "joint variable \"" +joint.toString()+ "\" not found" );
			vars.add( fvJoint );
			TableIndex index = new TableIndex( vars );
			Table ret = new Table( index, parseData() );
			return ret;
		}

		public double[] parseData(){
			String[] tokens = PATTERN_DATA_DELIMITER.split( blankOutComments( table ) );
			//System.out.println( Arrays.toString( tokens ) );
			int len = tokens.length;
			if( tokens[0].length() < 1 ) --len;
			double[] ret = new double[ len ];
			int i = 0;
			String current = null;
			try{
				for( String token : tokens ){
					if( token.length() > 0 ) ret[i++] = Double.parseDouble( current = token );
				}
			}catch( NumberFormatException numberformatexception ){
				throw new RuntimeException( "unparseable probability data \"" +current+ "\" in cpt for variable \""+joint.toString()+"\"", numberformatexception );
			}
			return ret;
		}
	}

	protected List<Definition> myDefinitions = new LinkedList<Definition>();
	protected Definition       myDefinition;

	public void clear(){
		myVariables.clear();
		myDefinitions.clear();
		myProperties.clear();
		myName.setLength(0);
	}

	/** interface RunReadBIF.MonitorableReusableParser */
	public void cleanup(){
		this.clear();
	}

	public BeliefNetwork beliefNetwork( File input, NodeLinearTask task ) throws Exception {
		myTask = task;
		clear();
		this.parse( input );
		return this.toBeliefNetwork( task );
	}

	public BeliefNetwork beliefNetwork( InputStream input, NodeLinearTask task ) throws Exception {
		myTask = task;
		clear();
		this.parse( input );
		return this.toBeliefNetwork( task );
	}

	/** @since 20060615 */
	public static String translateToIdentifier( String name ){
		int len = name.length();
		if( len < 1 ) return "";

		StringBuilder builder = new StringBuilder( len );
		char charat;
		int i = 0;
		if( !Character.isUnicodeIdentifierStart( charat = name.charAt(i) ) ){
			builder.append( '_' );
		}

		for( i = 0; i<len; i++ ){
			if( Character.isUnicodeIdentifierPart( charat = name.charAt(i) ) ){
				builder.append( charat );
			}
		}

		return builder.toString();
	}

	private BeliefNetwork toBeliefNetwork( NodeLinearTask task ) throws Exception {
		BeliefNetworkImpl ret = new BeliefNetworkImpl();

		try{
			String label = myName.toString();
			ret.getProperties().put( PropertySuperintendent.KEY_HUGIN_LABEL, label );
			ret.getProperties().put( PropertySuperintendent.KEY_HUGIN_NAME,  translateToIdentifier( label ) );
		}catch( Throwable throwable ){
			System.err.println( "warning: XmlbifParser.toBeliefNetwork() failed to set network name/label: " + throwable );
		}

		for( Variable variable : myVariables ){
			ret.addVariable( variable.toFiniteVariable(), false );
		}

		Map<FiniteVariable,CPTShell> mapVariablesToCPTShells = new HashMap<FiniteVariable,CPTShell>( ret.size() );
		TableShell shell;
		FiniteVariable fv;
		for( Definition def : myDefinitions ){
			shell = new TableShell( def.toTable( ret ) );
			fv    = shell.index().getJoint();
			mapVariablesToCPTShells.put( fv, shell );
			if( task != null ) task.touch();
		}

		ret.induceGraph( mapVariablesToCPTShells, task );

		if( ret.size() != myVariables.size() ) throw new RuntimeException( "something wrong, found " +myVariables.size()+ " variables but inducing graph resulted in network size " + ret.size() );

		try{
			EvidenceController ec = ret.getEvidenceController();
			int index = -1;
			for( Variable variable : myVariables ){
				if( (index = variable.getEvidence()) >= 0 ){
					fv = (FiniteVariable) ret.forID( variable.getID() );
					if( fv == null ) throw new RuntimeException( "variable \"" +variable.getID()+ "\" not found" );
					ec.observe( fv, fv.instance( index ) );
				}
			}
		}catch( Throwable throwable ){
			throw new RuntimeException( "error setting evidence, network \"" +myName.toString()+ "\": " + throwable.toString(), throwable );
		}

		return ret;
	}

	/** test/debug */
	public static void main( String[] args ){
		if( args.length < 1 ){
			System.err.println( "usage: "+XmlbifParser.class.getName()+" <path of file to parse>" );
			System.exit(1);
		}

		XmlbifParser parserLP = new XmlbifParser();
		parserLP.setHighPerformance( false );
		XmlbifParser parserHP = new XmlbifParser();
		parserHP.setHighPerformance( true );

		mainImpl( args[0], parserLP, parserHP );
	}

	/** @since 20060622 */
	public static void mainImpl( String pathFileNetwork, XmlbifParser parserLP, XmlbifParser parserHP )
	{
		PrintStream test = System.out;
		try{
			File fileNetwork = new File( pathFileNetwork );

			parserLP.getSAXParser();
			parserHP.getSAXParser();

			test.println( "warmup:" );
			profile( fileNetwork, parserLP, parserHP, false );

			test.println( "sprint 1 (time the 1st of one):" );
			profile( fileNetwork, parserLP, parserHP, false );

			test.println( "sprint 2 (time the 2nd of 2):" );
			profile( fileNetwork, parserLP, parserHP, true );
		}catch( Exception exception ){
			//System.err.println( exception.getMessage() );
			exception.printStackTrace( test );
		}

		/*test.println( "found " + bifparser.myVariables.size() + " variables" );
		for( Variable variable : bifparser.myVariables ){
			test.println( variable );
		}*/
		//test.println( "lp network, size " + beliefnetworkHP.size() );
		//test.println( "hp network, size " + beliefnetworkLP.size() );

		/*Point point = new Point();
		for( Iterator it = beliefnetworkHP.iterator(); it.hasNext(); ){
			test.println( ((StandardNode)it.next()).getLocation( point ) );
		}*/
	}

	/** @since 20060622 */
	private static void profile( File fileNetwork, XmlbifParser parserLP, XmlbifParser parserHP, boolean twice ) throws Exception{
		PrintStream test = System.out;
		long startHP = -1, endHP = -1, startLP = -1, endLP = -1, elapsedHP = -1, elapsedLP = -1;
		float fracLPoverHP, fracHPoverLP;
		BeliefNetwork beliefnetworkHP = null, beliefnetworkLP = null;

		if( twice ) beliefnetworkHP = parserHP.beliefNetwork( fileNetwork, null );
		startHP = JVMTI.getCurrentThreadCpuTimeUnsafe();
		beliefnetworkHP = parserHP.beliefNetwork( fileNetwork, null );
		endHP = JVMTI.getCurrentThreadCpuTimeUnsafe();

		if( twice ) beliefnetworkHP = parserLP.beliefNetwork( fileNetwork, null );
		startLP = JVMTI.getCurrentThreadCpuTimeUnsafe();
		beliefnetworkLP = parserLP.beliefNetwork( fileNetwork, null );
		endLP = JVMTI.getCurrentThreadCpuTimeUnsafe();

		elapsedHP = endHP - startHP;
		elapsedLP = endLP - startLP;

		fracLPoverHP = (float)((double)elapsedLP/(double)elapsedHP);
		fracHPoverLP = 1/fracLPoverHP;

		test.println( "    low  performance: " + NetworkIO.formatTime( elapsedLP ) + " ("+NetworkIO.formatPercent( fracLPoverHP, NetworkIO.FORMAT_PROFILE_PERCENT )+" of hp)" );
		test.println( "    high performance: " + NetworkIO.formatTime( elapsedHP ) + " ("+NetworkIO.formatPercent( fracHPoverLP, NetworkIO.FORMAT_PROFILE_PERCENT )+" of lp)" );

	  //test.println( "    lp network, size " + beliefnetworkHP.size() );
	  //test.println( "    hp network, size " + beliefnetworkLP.size() );
	}

	private   NodeLinearTask      myTask;
	protected List<StringBuilder> myProperties = new LinkedList<StringBuilder>();
	protected StringBuilder       myName       = new StringBuilder( INT_SIZE_ACCUMULATOR_INITIAL );
	private   boolean             myFlagHighPerformance = true;

	private static Schema           SCHEMA;
	private static SAXParserFactory REFERENCE_FACTORY;
}
