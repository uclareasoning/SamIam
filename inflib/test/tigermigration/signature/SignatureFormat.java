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
public enum SignatureFormat// implements Comparable<SignatureFormat>
{
	STRIP_ABSTRACT( "strip abstract modifier" ){
		public String apply( String raw ){
			Matcher m = PATTERN_ABSTRACT.matcher( raw );
			return m.replaceAll( "" );
		}
	},

	ARGLIST( "arglist" ){
		public String apply( FormattedSignature raw ){
			String ret = raw.getSignature();
			//Matcher m = PATTERN_ARGLIST_OPEN.matcher( ret );
			//ret = m.replaceAll( STR_REPLACE_ARGLIST_OPEN );
			//m = PATTERN_ARGLIST_CLOSE.matcher( ret );
			//ret = m.replaceAll( STR_REPLACE_ARGLIST_CLOSE );
			Matcher m = PATTERN_ARGLIST.matcher( ret );
			if( m.find() ){
				String args = m.group(1);
				if( args.length() > 0 ) ret = m.replaceFirst( STR_REPLACE_ARGLIST_OPEN + raw.getArgList() + STR_REPLACE_ARGLIST_CLOSE );
			}
			return ret;
		}

		public String apply( String raw ){
			String ret = raw;
			Matcher m = PATTERN_ARGLIST.matcher( ret );
			if( m.find() ){
				String args = m.group(1);
				if( args.length() > 0 ) ret = m.replaceFirst( STR_REPLACE_ARGLIST );
			}
			return ret;
		}
	},

	STRIP_NAME_PREFIX( "strip name prefix" ){
		public String apply( String raw ){
			Matcher m = PATTERN_NAME_PREFIX.matcher( raw );
			return m.replaceAll( "" );
		}
	},

	/*APPEND_RETURN_TYPE( "append return type" ){
		public String apply( FormattedSignature raw ){
			String ret = raw.getSignature() + " " + raw.method.getGenericReturnType();//.getClass().getName();
			if( raw.method.getGenericReturnType() == void.class ) ret += " VOID!!";
			return ret;
		}
	},*/

	DECORATE( "decorate" ){
		public String apply( FormattedSignature raw ){
			StringBuilder buff = new StringBuilder( raw.getSignature().length() * 2 );
			buff.append( raw.getSignature() );
			buff.append( "{\n\t" );
			if( raw.isFunction() ) buff.append( "return " );
			buff.append( "DECORATED." );
			buff.append( raw.getName() );
			String[] pNames = raw.getParameterNames();
			if( (pNames == null) || (pNames.length < 1) ){
				buff.append( "()" );
			}
			else{
				buff.append( "( " );
				for( String pName : pNames ){
					buff.append( pName );
					buff.append( ", " );
				}
				buff.setLength( buff.length() - 2 );
				buff.append( " )" );
			}
			buff.append( ";\n}" );
			return buff.toString();
		}

		public String apply( String raw ){
			throw new UnsupportedOperationException();
		}
	}
	;

	public static String applyAll( EnumSet<? extends SignatureFormat> formats, MethodDoc methoddoc ){
		return applyAll( formats, new FormattedSignature( methoddoc ) );
	}

	public static String applyAll( EnumSet<? extends SignatureFormat> formats, Method method ){
		return applyAll( formats, new FormattedSignature( method ) );
	}

	public static String applyAll( EnumSet<? extends SignatureFormat> formats, FormattedSignature ret ){
		for( SignatureFormat format : formats ){
			ret.apply( format );
		}
		return ret.getSignature();
	}

	private SignatureFormat( String name ){//, float ordinal ){
		SignatureFormat.this.myName = name;
		//SignatureFormat.this.myOrdinal = ordinal;
	}

	public String apply( FormattedSignature raw ){
		return apply( raw.getSignature() );
	}

	abstract public String apply( String raw );

	public static final String REGEX_NAME_PREFIX = "(\\w+\\.)+";
	public static final Pattern PATTERN_NAME_PREFIX = Pattern.compile( REGEX_NAME_PREFIX );

	public static final String REGEX_ABSTRACT = "abstract ";
	public static final Pattern PATTERN_ABSTRACT = Pattern.compile( REGEX_ABSTRACT );

	public static final String REGEX_ARGLIST_OPEN = "\\(";
	public static final Pattern PATTERN_ARGLIST_OPEN = Pattern.compile( REGEX_ARGLIST_OPEN );
	public static final String STR_REPLACE_ARGLIST_OPEN = "( ";

	public static final String REGEX_ARGLIST_CLOSE = "\\)";
	public static final Pattern PATTERN_ARGLIST_CLOSE = Pattern.compile( REGEX_ARGLIST_CLOSE );
	public static final String STR_REPLACE_ARGLIST_CLOSE = " )";

	public static final String REGEX_ARGLIST = "\\(([^\\)]*)\\)";
	public static final Pattern PATTERN_ARGLIST = Pattern.compile( REGEX_ARGLIST );
	public static final String STR_REPLACE_ARGLIST = "( $1 )";

	//public int compareTo( SignatureFormat o ){
	//	return Float.compare( this.myOrdinal, o.myOrdinal );
	//}

	private String myName;
	//private Float myOrdinal;
}
