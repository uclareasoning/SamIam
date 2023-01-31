package edu.ucla.belief.ui.util;

import java.util.Map;
import java.util.HashMap;
import java.util.regex.*;

/** Encodes strings and caches the results in
	case the encoding is expensive.
	At inception, only supports html encoding.

	@author Keith Cascio
	@since 022505 */
public class Encoder
{
	public Encoder(){}

	public String htmlEncode( String input ){
		String ret;
		if( myMapToEncoded.containsKey( input ) ) ret = myMapToEncoded.get( input ).toString();
		else{
			ret = htmlEncodeImpl( input );
			myMapToEncoded.put( input, ret );
		}
		return ret;
	}

	/** @since 20050923 */
	public String htmlUnencode( String input ){
		String ret;
		if( myMapToUnencoded.containsKey( input ) ) ret = myMapToUnencoded.get( input ).toString();
		else{
			ret = htmlUnencodeImpl( input );
			myMapToUnencoded.put( input, ret );
		}
		return ret;
	}

	public String htmlEncodeImpl( String input )
	{
		myBuffer.setLength(0);
		char charat;
		String encoding;
		int len = input.length();
		for( int i=0; i<len; i++ ){
			charat = input.charAt(i);
			if( (charat < ' ') || (charat > 127) ) {
				// If the character is outside of ascii, write the
				// numeric value.
				myBuffer.append( "&#" );
				myBuffer.append( Integer.toString( (int)charat ) );
				myBuffer.append( ";" );
			}
			else{
				encoding = (String)null;
				switch( charat ) {
					// Character level entities.
					case '<':
						encoding = STR_CODE_LT;
						break;
					case '>':
						encoding = STR_CODE_GT;
						break;
					case '&':
						encoding = STR_CODE_AMP;
						break;
					case '"':
						encoding = STR_CODE_QUOT;
						break;
					// Special characters
					case '\n':
					case '\t':
					case '\r':
						//encoding = "";
						break;
					default:
						myBuffer.append( charat );
					break;
				}
				if( encoding != null ) myBuffer.append( encoding );
			}
		}
		return myBuffer.toString();
	}

	public static final String STR_CODE_LT   = "&lt;",   STR_LT   = "<";
	public static final String STR_CODE_GT   = "&gt;",   STR_GT   = ">";
	public static final String STR_CODE_AMP  = "&amp;",  STR_AMP  = "&";
	public static final String STR_CODE_QUOT = "&quot;", STR_QUOT = "\"";
	public static final String STR_CODE_NBSP = "&nbsp;", STR_NBSP = " ";

	public static final String STR_REGEX_HTML = "^<.*?html.*?>.*$";
	public static final String STR_REGEX_TAG = "<.*?>", STR_REPLACE_TAG = "";
	public static final String STR_REGEX_NONASCI = "&#(\\d+);";

	private static Pattern PATTERN_TAG, PATTERN_HTML, PATTERN_LT, PATTERN_GT, PATTERN_AMP, PATTERN_QUOT, PATTERN_NBSP, PATTERN_NONASCI;

	private static Pattern[] ARRAY_SIMPLE_PATTERNS;
	private static String[] ARRAY_SIMPLE_REPLACEMENTS;

	/** @since 20050923 */
	private static void initUnencode(){
		if( PATTERN_LT != null ) return;

		PATTERN_TAG     = Pattern.compile( STR_REGEX_TAG );
		PATTERN_LT      = Pattern.compile( STR_CODE_LT );
		PATTERN_GT      = Pattern.compile( STR_CODE_GT );
		PATTERN_AMP     = Pattern.compile( STR_CODE_AMP );
		PATTERN_QUOT    = Pattern.compile( STR_CODE_QUOT );
		PATTERN_NBSP    = Pattern.compile( STR_CODE_NBSP );
		PATTERN_HTML    = Pattern.compile( STR_REGEX_HTML, Pattern.CASE_INSENSITIVE );

		PATTERN_NONASCI = Pattern.compile( STR_REGEX_NONASCI );

		ARRAY_SIMPLE_PATTERNS = new Pattern[] { PATTERN_TAG, PATTERN_QUOT, PATTERN_NBSP, PATTERN_LT, PATTERN_GT, PATTERN_AMP };
		ARRAY_SIMPLE_REPLACEMENTS = new String[] { STR_REPLACE_TAG, STR_QUOT, STR_NBSP, STR_LT, STR_GT, STR_AMP };
	}

	/** @since 20050923 */
	public String htmlUnencodeImpl( String input )
	{
		initUnencode();

		if( !PATTERN_HTML.matcher( input ).matches() ) return input;

		String ret = input;
		Matcher mna = PATTERN_NONASCI.matcher( ret );
		if( mna.find() ){
			myBuffer.setLength(0);
			mna.reset();
			char[] singleton = new char[1];
			while( mna.find() ){
				singleton[0] = (char) Integer.parseInt( mna.group(1) );
				mna.appendReplacement( myBuffer, new String( singleton ) );
			}
			mna.appendTail( myBuffer );

			ret = myBuffer.toString();
		}

		for( int i=0; i<ARRAY_SIMPLE_PATTERNS.length; i++ ){
			ret = ARRAY_SIMPLE_PATTERNS[i].matcher( ret ).replaceAll( ARRAY_SIMPLE_REPLACEMENTS[i] );
		}

		return ret;
	}

	/** Test/debug
		@since 20050923 */
	public static void main( String[] args ){
		Encoder encoder = new Encoder();

		java.io.PrintStream stream = Util.STREAM_TEST;

		String code1 = "<html><nobr>&nbsp;Pr&nbsp;(e)&nbsp;=&nbsp;0.2157";
		String unencoded = encoder.htmlUnencode( code1 );

		stream.println( "code1:     \"" +code1+ "\"" );
		stream.println( "unencoded: \"" +unencoded+ "\"" );

		String code2 = " Pr (e) = 0.2157";
		unencoded = encoder.htmlUnencode( code2 );
		String encoded = encoder.htmlEncode( code2 );

		stream.println();
		stream.println( "code2:     \"" +code2+ "\"" );
		stream.println( "unencoded: \"" +unencoded+ "\"" );
		stream.println( "encoded:   \"" +encoded+ "\"" );

		String code3 = "<html>&#200;<font color=\"#FFFFFF\">bling</font>&nbsp;&amp;&amp;&quot; &gt;&gt;&#250;";
		unencoded = encoder.htmlUnencode( code3 );
		encoded = encoder.htmlEncode( code3 );
		String reunencoded = encoder.htmlUnencode( "<html>" + encoded );
		String rereunencoded = encoder.htmlUnencode( "<html>" + reunencoded );

		stream.println();
		stream.println( "code3:         \"" +code3+ "\"" );
		stream.println( "unencoded:     \"" +unencoded+ "\"" );
		stream.println( "encoded:       \"" +encoded+ "\"" );
		stream.println( "re-unencoded:  \"" +reunencoded+ "\"" );
		stream.println( "rereunencoded: \"" +rereunencoded+ "\"" );
	}

	private StringBuffer myBuffer = new StringBuffer();
	private Map myMapToEncoded = new HashMap();
	private Map myMapToUnencoded = new HashMap();
}
