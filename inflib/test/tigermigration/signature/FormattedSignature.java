package signature;

import java.lang.reflect.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;

/** @author keith cascio
	@since 20051111 veterans day */
public class FormattedSignature
{
	public FormattedSignature( Method method ){
		this.method = method;
		this.signature = method.toGenericString();
		this.applied = EnumSet.noneOf( SignatureFormat.class );
	}

	public FormattedSignature( MethodDoc methoddoc ){
		this.methodDoc = methoddoc;
		this.signature = SignatureDoclet.signature( methoddoc );
		this.applied = EnumSet.noneOf( SignatureFormat.class );
	}

	public void apply( SignatureFormat format ){
		if( applied.contains( format ) ) return;
		signature = format.apply( this );
		applied.add( format );
	}

	public String getArgList(){
		if( arglist == null ) arglist = makeArgList( this );
		return arglist;
	}

	public String[] getParameterNames(){
		if( parameterNames == null ) parameterNames = makeParameterNames();
		return parameterNames;
	}

	public String[] getTypeNames(){
		if( typeNames == null ) typeNames = makeTypeNames();
		return typeNames;
	}

	public String getSourceLine(){
		return sourceLine;
	}

	public String getSignature(){
		return this.signature;
	}

	public Method getMethod(){
		return method;
	}

	public MethodDoc getMethodDoc(){
		return methodDoc;
	}

	public String getName(){
		if( FormattedSignature.this.methodDoc != null ) return FormattedSignature.this.methodDoc.name();
		else if( FormattedSignature.this.method != null ) return FormattedSignature.this.method.getName();
		else return "";
	}

	public int getNumParameters(){
		if( FormattedSignature.this.methodDoc != null ) return FormattedSignature.this.methodDoc.parameters().length;
		else if( FormattedSignature.this.method != null ) return FormattedSignature.this.method.getGenericParameterTypes().length;
		else return 0;
	}

	public boolean isFunction(){
		if( FormattedSignature.this.methodDoc != null ){
			com.sun.javadoc.Type type = FormattedSignature.this.methodDoc.returnType();
			return !(type.isPrimitive() && type.simpleTypeName().equals( "void" ));
		}
		else if( FormattedSignature.this.method != null ) return FormattedSignature.this.method.getGenericReturnType() != void.class;
		else return false;
	}

	public Pattern getPatternStrict(){
		if( myPatternStrict == null ){
			StringBuilder sb = new StringBuilder();
			sb.append( "^[\\w\\s>\\[\\]<]+\\Q" );
			sb.append( method.getName() );
			sb.append( "\\E\\s*\\(" );
			myPatternDebug = Pattern.compile( sb.toString() );

			String regexTypeName;
			for( String tName : getTypeNames() ){
				sb.append( "\\s*" );
				sb.append( regexTypeName = regexifyTypeName( tName ) );
				sb.append( "\\s*,?" );
			}
			sb.append( "\\)" );
			myPatternStrict = Pattern.compile( myRegexStrict = sb.toString() );
		}
		return myPatternStrict;
	}

	public void lookAtSource( String line ){
		Matcher m = getPatternStrict().matcher( line );
		if( m.find() ){
			if( this.sourceLine != null ){
				System.err.println( "Warning: signature "+getBrief()+" matches both:" );
				System.err.println( "    \"" +sourceLine+ "\"" );
				System.err.println( "    \"" +line      + "\"" );
			}
			this.sourceLine = line;
			int numParams = m.groupCount();
			if( parameterNames == null ) parameterNames = new String[numParams];
			for( int i=0; i<numParams; i++ ){
				parameterNames[i] = m.group(i+1);
			}
		}
		/*else{
			//if( line.indexOf( method.getName() ) > 0 ){
			if( myPatternDebug.matcher( line ).find() ){
				System.out.println();
				System.out.println( "*************************************************************" );
				System.out.println( "signature "+getBrief()+" did not match:" );
				System.out.println( "    \"" +line+ "\"" );
				System.out.println();
				System.out.println( "    \"" +myRegexStrict+ "\"" );
				System.out.println( "*************************************************************" );
			}
		}*/
	}

	public String getBrief(){
		return method.getName() + "(" + method.getGenericParameterTypes().length + ")";
	}

	public static final String REGEX_WHITESPACE = "\\s+";
	public static final Pattern PATTERN_WHITESPACE = Pattern.compile( REGEX_WHITESPACE );
	public static final String STR_REPLACE_WHITESPACE = " ";

	public static final String REGEX_TYPE_DELIMITERS = "\\s*([><,])\\s*";
	public static final Pattern PATTERN_TYPE_DELIMITERS = Pattern.compile( REGEX_TYPE_DELIMITERS );
	public static final String STR_REPLACE_TYPE_DELIMITERS = "$1";
	//public static final String STR_REPLACE_TYPE_DELIMITERS = "\\E\\s*&\\s*\\Q";
	//public static final String STR_REPLACE_TYPE_DELIMITERS = Matcher.quoteReplacement( "\\E\\s*" ) + "&" + Matcher.quoteReplacement( "\\s*\\Q" );

	public static String regexifyTypeName( String tName ){
		String componentType = tName;
		String arrayBrackets = "";

		boolean flagArrayType = false;
		int indexFirstBracket = tName.indexOf( '[' );
		if( indexFirstBracket > 0 ){
			flagArrayType = true;
			componentType = tName.substring( 0, indexFirstBracket );
			arrayBrackets = tName.substring( indexFirstBracket );
		}

		componentType = PATTERN_WHITESPACE.matcher( componentType ).replaceAll( STR_REPLACE_WHITESPACE );
		componentType = PATTERN_TYPE_DELIMITERS.matcher( componentType ).replaceAll( STR_REPLACE_TYPE_DELIMITERS );
		componentType = componentType.replace( "<", "\\E\\s*<\\s*\\Q" );
		componentType = componentType.replace( ">", "\\E\\s*>\\s*\\Q" );
		componentType = componentType.replace( ",", "\\E\\s*,\\s*\\Q" );
		componentType = componentType.replace( " ", "\\E\\s*\\Q" );
		//componentType = componentType.replace( "extends", "\\E\\s*extends\\s*\\Q" );
		//componentType = componentType.replace( "super", "\\E\\s*super\\s*\\Q" );
		//componentType = componentType.replace( "&&", "\\E\\s*&&"\\s*\\Q" );

		//String brackets1 = flagArrayType ? ("\\E(?:\\s*\\Q" + arrayBrackets + "\\E)?") : "\\E";
		//String brackets2 = flagArrayType ? (   "(?:\\s*\\Q" + arrayBrackets + "\\E)?") : "";
		String brackets = flagArrayType ? (   "(?:\\s*\\Q" + arrayBrackets + "\\E)?") : "";

		//String ret = "\\Q" + componentType + brackets1 + "\\s+(\\w+)" + brackets2;
		String ret = "\\Q" + componentType + "\\E" + brackets + "\\s+(\\w+)" + brackets;
		//if( arrayBrackets.length() > 0 ){
		//	ret = "(?:" + ret + "|\\Q" + componentType + "\\E\\s+(\\w+)" + brackets2 + "\\E)";
		//}
		return ret;
	}

	public String[] makeParameterNames(){
		if( FormattedSignature.this.methodDoc != null ) return makeParameterNames( FormattedSignature.this.methodDoc );
		else if( FormattedSignature.this.method != null ) return makeDefaultParameterNames( FormattedSignature.this.method );
		else return null;
	}

	public static String[] makeDefaultParameterNames( Method method ){
		Type[] types = method.getGenericParameterTypes();
		String[] ret = new String[ types.length ];
		for( int i=0; i<ret.length; i++ ){
			ret[i] = "arg" + Integer.toString( i );
		}
		return ret;
	}

	public static String[] makeParameterNames( MethodDoc methoddoc ){
		Parameter[] params = methoddoc.parameters();
		String[] ret = new String[ params.length ];
		for( int i=0; i<params.length; i++ ){
			ret[i] = params[i].name();
		}
		return ret;
	}

	private String[] makeTypeNames(){
		if( FormattedSignature.this.methodDoc != null ) return makeTypeNames( FormattedSignature.this.methodDoc );
		else if( FormattedSignature.this.method != null ) return makeTypeNames( FormattedSignature.this.method );
		else return null;
	}

	public static String[] makeTypeNames( Method method ){
		Type[] types = method.getGenericParameterTypes();
		String[] ret = new String[ types.length ];
		for( int i=0; i<types.length; i++ ){
			ret[i] = SignatureFormat.STRIP_NAME_PREFIX.apply( getTypeName( types[i] ) );
		}
		return ret;
	}

	public static String[] makeTypeNames( MethodDoc methoddoc ){
		Parameter[] params = methoddoc.parameters();
		String[] ret = new String[ params.length ];

		StringBuilder sb = new StringBuilder( 32 );
		for( int i=0; i<params.length; i++ ){
			sb.setLength(0);
			ret[i] = SignatureDoclet.typeToString( params[i].type(), sb ).toString();
		}
		return ret;
	}

	public static String makeArgList( FormattedSignature raw ){
		String[] tNames = raw.getTypeNames();
		String[] pNames = raw.getParameterNames();
		StringBuilder buff = new StringBuilder( tNames.length * 32 );
		for( int i=0; i<tNames.length; i++ ){
			buff.append( tNames[i] );
			buff.append( " " );
			buff.append( pNames[i] );
			buff.append( ", " );
		}
		if( buff.length() >= 2 ) buff.setLength( buff.length() - 2 );
		return buff.toString();
	}

	/* Utility routine to paper over array type names */
	public static String getTypeName( Type type ){
		if( !(type instanceof Class) ) return type.toString();

		Class clazz = (Class)type;
		if( clazz.isArray() ){
			try{
				Class cl = clazz;
				int dimensions = 0;
				while( cl.isArray() ){
					dimensions++;
					cl = cl.getComponentType();
				}
				String name = cl.getName();
				StringBuilder sb = new StringBuilder( name.length() + (dimensions*2) );
				sb.append( name );
				for( int i = 0; i < dimensions; i++ ){
					sb.append("[]");
				}
				return sb.toString();
			}catch( Throwable e ){
				System.err.println( "Warning: SignatureFormat.getTypeName() caught " + e );
				/*FALLTHRU*/
			}
		}
		return clazz.getName();
	}

	public String[][] getParametersQualNameCaptures(){
		if( myParametersQualNameCaptures == null ){
			myParametersQualNameCaptures = new String[ getNumParameters() ][];

			Parameter[] params = methodDoc.parameters();
			HashSet<String> util = new HashSet<String>(2);
			for( int i=0; i<myParametersQualNameCaptures.length; i++ ){
				util.clear();
				SignatureDoclet.captureQualNames( params[i].type(), util );
				myParametersQualNameCaptures[i] = util.toArray( new String[util.size()] );
			}
		}
		return myParametersQualNameCaptures;
	}

	public FormattedSignature getCorrelate(){
		return FormattedSignature.this.myCorrelate;
	}

	public void setCorrelate( FormattedSignature correlate ){
		if( myCorrelate != null ){
			System.err.println( "Warning: method " + getName() + " over-correlated" );
		}
		FormattedSignature.this.myCorrelate = correlate;
	}

	private Method method;
	private String signature;
	private String[] parameterNames;
	private String[] typeNames;
	private String arglist;
	private EnumSet<? super SignatureFormat> applied;
	private Pattern myPatternStrict;
	private String myRegexStrict;
	private Pattern myPatternDebug;
	private String sourceLine;

	private MethodDoc methodDoc;
	private String[][] myParametersQualNameCaptures;
	private FormattedSignature myCorrelate;
}
