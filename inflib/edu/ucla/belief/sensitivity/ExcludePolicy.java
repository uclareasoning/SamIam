package edu.ucla.belief.sensitivity;

import edu.ucla.belief.*;
import edu.ucla.belief.io.*;
import java.util.*;
import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;
import edu.ucla.util.AbstractEnumProperty;

/** @since 20030722 */
public abstract class ExcludePolicy implements EnumValue
{
	/* @since 20060411 */
	public static boolean FLAG_ENABLE_LOCK_BY_PARAMETER = false;

	/* @since 20060411 */
	abstract protected boolean[] getExcludeArray( int size, StandardNode node );

	protected ExcludePolicy( String displayName )
	{
		myDisplayName = displayName;
	}

	public String toString(){
		return myDisplayName;
	}

	public static ExcludePolicy forString( String displayName ){
		ExcludePolicy[] array = values();
		for( int i=0; i<array.length; i++ )
		{
			if( array[i].toString().equals( displayName ) ) return array[i];
		}
		return null;
	}

	public static boolean[] makeExcludeArray(StandardNode node) {
		String s = (String)node.getProperties().get( PropertySuperintendent.KEY_EXCLUDEARRAY );
		//bad idea Table cpt = node.getCPTShell( bad idea ).getCPT();//bad idea
		CPTShell shell = node.getCPTShell( node.getDSLNodeType() );
		TableIndex index = shell.index();
		int CPLength = index.size();
		boolean[] excludeArray = new boolean[ CPLength ];
		Arrays.fill(excludeArray, false);
		if (s == null)
			return excludeArray;

		Vector vector = new Vector();
		while (true) {
			int p1 = s.indexOf('[');
			if (p1 == -1)
				break;
			int p2 = s.indexOf(']', p1+1);
			if (p2 == -1)
				break;
			String ss = s.substring(p1+1, p2).trim();
			String[] insts = ss.split(";");
			Hashtable hashtable = new Hashtable();
			for (int i = 0; i < insts.length; i++) {
				String[] pairs = insts[i].split("=", 2);
				if (pairs.length == 1)
					continue;
				hashtable.put(pairs[0], pairs[1]);
			}
			vector.add(hashtable);
			s = s.substring(p2+1);
		}

		//TableIndex index= cpt.index();
		List vars = index.variables();
		for (int i = 0; i < CPLength; i++) {
			int[] mindex = index.mindex(i, null);
			for (int j = 0; j < vector.size(); j++) {
				if (excludeArray[i])
					break;
				excludeArray[i] = true;
				Map map = (Map)vector.get(j);
				for (int k = 0; k < vars.size(); k++) {
					FiniteVariable var = (FiniteVariable)vars.get(k);
					Object obj = map.get(var.getID());
					if (obj != null && var.index(obj) != mindex[k]) {
						excludeArray[i] = false;
						break;
					}
				}
			}
		}

		return excludeArray;
	}

	/** @since 20060411 */
	public static String makeExcludeString( FiniteVariable excludeVar, boolean[] excludeArray ){
		//String s = "";
		TableIndex index = excludeVar.getCPTShell( excludeVar.getDSLNodeType() ).index();

		int[] mindex = new int[ excludeVar.size() ];
		StringBuffer buff = new StringBuffer( 256 );
		List vars = index.variables();
		for( int i = 0; i < excludeArray.length; i++ ){
			if( !excludeArray[i] ) continue;
			buff.append( "[" );
			mindex = index.mindex( i, mindex );
			for( int j = 0; j < mindex.length; j++ ){
				FiniteVariable var = (FiniteVariable)vars.get(j);
				buff.append( var.getID() );
				buff.append( "=" );
				buff.append( var.instance(mindex[j]) );
				if( j != mindex.length-1 )
					buff.append( ";" );
			}
			buff.append( "]" );
		}
		return buff.toString();
	}

	/** @since 20060411 */
	public static ExcludePolicy getExcludePolicy( Object var ){
		if( var instanceof StandardNode ) return ((StandardNode)var).getExcludePolicy();
		else return DEFAULT;
	}

	/** @since 20060411 */
	public static boolean[] getExcludeArray( FiniteVariable var ){
		int size = var.getCPTShell( var.getDSLNodeType() ).index().size();

		ExcludePolicy policy = DEFAULT;
		StandardNode node = null;

		if( var instanceof StandardNode ){
			node = (StandardNode) var;
			policy = node.getExcludePolicy();
		}

		return policy.getExcludeArray( size, node );
	}

	public static boolean exclude(FiniteVariable var) {
		if (var instanceof StandardNode)
			return ((StandardNode)var).
				getExcludePolicy() == BY_VARIABLE;
		else
			return false;
	}

	/** @author Keith Cascio
		@since 20030819 */
	public static EnumProperty PROPERTY = new AbstractEnumProperty()
	{
		public String getName()
		{
			return "sensitivity policy";
		}
		public boolean contains( EnumValue val )
		{
			if( myList == null ) myList = Arrays.asList( ExcludePolicy.values() );
			return myList.contains( val );
		}
		public EnumValue forString( String str )
		{
			return ExcludePolicy.forString( str );
		}
		public Iterator iterator()
		{
			if( myList == null ) myList = Arrays.asList( ExcludePolicy.values() );
			return myList.iterator();
		}
		public EnumValue[] valuesAsArray()
		{
			return ExcludePolicy.values();
		}
		public int size()
		{
			return ExcludePolicy.values().length;
		}
		public EnumValue getDefault()
		{
			return ExcludePolicy.DEFAULT;
		}
		public EnumValue valueOf( boolean flag )
		{
			return flag ? ExcludePolicy.BY_VARIABLE : ExcludePolicy.INCLUDE;
		}
		public String getID()
		{
			return "excludepolicy";
		}

		/** @since 20050823 */
		public int indexOf( EnumValue value ){
			ExcludePolicy[] array = ExcludePolicy.values();
			for( int i=0; i<array.length; i++ ){
				if( array[i] == value ) return i;
			}
			return -1;
		}

		/** @since 20050823 */
		public EnumValue forIndex( int index ){
			ExcludePolicy[] array = ExcludePolicy.values();
			if( (-1 < index) && (index < array.length) ) return array[index];
			else return (EnumValue) null;
		}

		private List myList;
	};

	/** @since 20070420 */
	public EnumProperty property(){
		return PROPERTY;
	}

	private String myDisplayName;

	/* @since 20060411 */
	private static class ByParameter extends ExcludePolicy{
		private ByParameter(){
			super( "lock by parameter" );
		}

		/* @since 20060411 */
		protected boolean[] getExcludeArray( int size, StandardNode node ){
			if( node == null ) throw new IllegalStateException( "policy == \"" + ByParameter.this + "\" but node !instanceof StandardNode" );
			boolean[] excludeArray = node.getExcludeArray();
			if( excludeArray == null ){
				excludeArray = makeExcludeArray( node );
				node.setExcludeArray( excludeArray );
			}
			return excludeArray;
		}
	}

	/* @since 20060411 */
	private static boolean[] getCachedUniformExcludeArray( int size, boolean value, Map/*<Integer,boolean[]>*/ map ){
		Integer key = new Integer( size );
		boolean[] ret = null;

		if( map.containsKey( key ) ) ret = (boolean[]) map.get( key );
		else{
			ret = new boolean[ size ];
			Arrays.fill( ret, value );
			map.put( key, ret );
		}
		return ret;
	}

	public static final ExcludePolicy   INCLUDE      = new ExcludePolicy( "include whole CPT" ){
		/* @since 20060411 */
		protected boolean[] getExcludeArray( int size, StandardNode node ){
			if( map == null ) map = new HashMap/*<Integer,boolean[]>*/( 8 );
			return getCachedUniformExcludeArray( size, false, map );
		}

		private Map/*<Integer,boolean[]>*/ map;
	};
	public static final ExcludePolicy   BY_VARIABLE  = new ExcludePolicy( "exclude whole CPT" ){
		/* @since 20060411 */
		protected boolean[] getExcludeArray( int size, StandardNode node ){
			if( map == null ) map = new HashMap/*<Integer,boolean[]>*/( 8 );
			return getCachedUniformExcludeArray( size, true, map );
		}

		private Map/*<Integer,boolean[]>*/ map;
	};
	public static final ExcludePolicy   BY_PARAMETER = new ByParameter();
	public static final ExcludePolicy   DEFAULT      = INCLUDE;
	private static      ExcludePolicy[] VALUES;
	//public static final ExcludePolicy[] ARRAY        = new ExcludePolicy[] { INCLUDE, BY_VARIABLE, BY_PARAMETER };

	/* @since 20060411 */
	public static ExcludePolicy[] values(){
		if( VALUES == null ){
			VALUES = FLAG_ENABLE_LOCK_BY_PARAMETER ? new ExcludePolicy[] { INCLUDE, BY_VARIABLE, BY_PARAMETER } : new ExcludePolicy[] { INCLUDE, BY_VARIABLE };
		}
		return VALUES;
	}
}
