package edu.ucla.util;

import edu.ucla.belief.Variable;
import edu.ucla.belief.io.StandardNode;

import java.util.Collection;
import java.util.Iterator;
import java.util.Date;
import java.util.Calendar;
import java.text.NumberFormat;
//{superfluous} import java.text.DateFormat;

/** @author keith cascio
	@since 20040520 */
public abstract class AbstractStringifier implements VariableStringifier
{
	public static final char CHAR_SEPARATOR = ',';
	public static final char CHAR_MAP_PREPOSITION = '=';
	public static final char CHAR_SPACE = ' ';

	public String objectToString( Object o ){
		return o.toString();
	}

	public String variableToString( Variable var ){
		return var.toString();
	}

	public String collectionToString( Collection list ){
		if( (list == null) || list.isEmpty() ) return "";
		StringBuffer buff = new StringBuffer( list.size()*20 );
		String token;
		for( Iterator it = list.iterator(); it.hasNext(); ){
			token = this.objectToString( it.next() );
			if( token.indexOf( CHAR_SEPARATOR ) >= 0 ) throw new IllegalArgumentException( "illegal element \"" +token+ "\" contains separator '"+CHAR_SEPARATOR+"'" );
			buff.append( token );
			buff.append( CHAR_SEPARATOR );
		}
		//buff.deleteCharAt( buff.length()-1 );
		buff.setLength( Math.max( buff.length()-1, 0 ) );
		return buff.toString();
	}

	public String mapToString( java.util.Map map ){
		if( (map == null) || map.isEmpty() ) return "";
		StringBuffer buff = new StringBuffer( map.size()*40 );
		String token;
		Object key;
		for( Iterator it = map.keySet().iterator(); it.hasNext(); ){
			key = it.next();
			buff.append( this.objectToString( key ) );
			buff.append( CHAR_MAP_PREPOSITION );
			buff.append( this.objectToString( map.get( key ) ) );
			buff.append( CHAR_SEPARATOR );
			buff.append( CHAR_SPACE );
		}
		buff.setLength( Math.max( buff.length()-2, 0 ) );
		return buff.toString();
	}

	public static final AbstractStringifier BASICOBJECT = new AbstractStringifier(){};

	public static final AbstractStringifier VARIABLE_ID = new AbstractStringifier()
	{
		public String variableToString( Variable var ){
			return var.getID();
		}

		public String objectToString( Object o ){
			if( o instanceof Variable ) return variableToString( (Variable)o );
			else return super.objectToString( o );
		}
	};

	/** @since 20070310 */
	public static final AbstractStringifier VARIABLE_LABEL = new AbstractStringifier(){
		public String variableToString( Variable var ){
			String ret = null;
			if( var instanceof StandardNode     ) ret = ((StandardNode)var).getLabel();
			if( ret == null || ret.length() < 1 ) ret = var.getID();
			return ret;
		}

		public String objectToString( Object o ){
			if( o instanceof Variable ) return variableToString( (Variable)o );
			else return super.objectToString( o );
		}
	};

	/** copied from sensitivity.TestCompleteness */
	public static class DateFormatFilename{
		private static DateFormatFilename INSTANCE;
		private DateFormatFilename(){}
		public static DateFormatFilename getInstance(){
			if( INSTANCE == null ) INSTANCE = new DateFormatFilename();
			return INSTANCE;
		}

		public static String now(){
			return getInstance().format( new Date( System.currentTimeMillis() ) );
		}

		public String format( Date date ){
			myBuffer.setLength(0);
			return format( date, myBuffer ).toString();
		}

		public StringBuffer format( Date date, StringBuffer toAppendTo ){
			Calendar calendar = Calendar.getInstance();
			calendar.setTime( date );

			int year   = calendar.get( Calendar.YEAR );
			int month  = calendar.get( Calendar.MONTH );
			int day    = calendar.get( Calendar.DAY_OF_MONTH );
			int hour24 = calendar.get( Calendar.HOUR_OF_DAY );
			int minute = calendar.get( Calendar.MINUTE );
			int second = calendar.get( Calendar.SECOND );
			int millis = calendar.get( Calendar.MILLISECOND );

			toAppendTo.append( myNumberFormat.format( year ) );
			toAppendTo.append( myNumberFormat.format( month+1 ) );
			toAppendTo.append( myNumberFormat.format( day ) );
			toAppendTo.append( '_' );
			toAppendTo.append( myNumberFormat.format( hour24 ) );
			toAppendTo.append( myNumberFormat.format( minute ) );
			toAppendTo.append( myNumberFormat.format( second ) );
			toAppendTo.append( myNumberFormat.format( millis ) );

			return toAppendTo;
		}

		private StringBuffer myBuffer = new StringBuffer( 32 );
		private NumberFormat myNumberFormat = new java.text.DecimalFormat( "##00" );
	}

	/** @since 20091119 */
	static public Object reflect( Class clazz, String clazzName, String methodName, Class[] parameterTypes, Object thiz, Object[] args, Object defaultReturn ){
		java.lang.reflect.Method meth = null;
		try{
			if( clazz == null ){ clazz = Class.forName( clazzName ); }
			meth           = clazz.getMethod( methodName, parameterTypes );
			defaultReturn  = meth.invoke( thiz, args );
		}catch( NoSuchMethodException  e ){
		}catch( ClassNotFoundException e ){
		}catch( IllegalAccessException e ){
		}catch( java.lang.reflect.InvocationTargetException e ){
		}catch( Throwable thrown ){
			System.err.println( "warning: AbstractStringifier.reflect() caught " + thrown );
		}
		return defaultReturn;
	}
}
