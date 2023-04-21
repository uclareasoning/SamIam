package edu.ucla.structure;

import java.util.*;

/**
	Cheap but slow.

	@author Keith Cascio
	@since 011805
*/
public class IdentityArrayMap implements Map
{
	private static final int INT_NOT_FOUND = (int)-1;

	public int size(){
		if( isEmpty() ) return 0;
		return keys.length;
	}

	public boolean isEmpty(){
		return keys == null;
	}

	public boolean containsKey( Object key ){
		if( myKeySet == null ) return indexOf( keys, key ) != INT_NOT_FOUND;
		else return myKeySet.contains( key );
	}

	public boolean containsValue(Object value){
		if( myValueSet == null ) return indexOf( values, value ) != INT_NOT_FOUND;
		else return myValueSet.contains( value );
	}

	private static int indexOf( Object[] array, Object element ){
		if( array == null ) return INT_NOT_FOUND;
		for( int i=0; i<array.length; i++ ) if( array[i] == element ) return i;
		return INT_NOT_FOUND;
	}

	public Object get( Object key ){
		if( isEmpty() ) return null;
		if( key == myCachedKey ) return myCachedValue;
		for( int i=0; i<keys.length; i++ )
			if( keys[i] == key ){
				myCachedKey = key;
				myCachedValue = values[i];
				return values[i];
			}
		return null;
	}

	public Object put( Object key, Object value )
	{
		clearCache();

		if( isEmpty() ){
			this.keys = new Object[] { key };
			this.values = new Object[] { value };
			return null;
		}

		Object oldval = null;
		int indexof = indexOf( keys, key );
		if( indexof == INT_NOT_FOUND )
		{
			Object[] oldkeys = keys;
			Object[] oldvals = values;

			allocate( oldkeys.length + 1 );

			keys[0] = key;
			values[0] = value;
			for( int i=0; i<oldkeys.length; i++ ){
				keys[i+1] = oldkeys[i];
				values[i+1] = oldvals[i];
			}

			if( myKeySet != null ) myKeySet.add( key );
		}
		else{
			oldval = values[indexof];
			values[indexof] = value;
		}

		if( myValueSet != null ) myValueSet.add( value );

		return oldval;
	}

	public Object remove(Object key){
		if( !containsKey( key ) ) return null;

		clearCache();

		Object oldval = null;
		if( size() == 1 ){
			oldval = get( key );
			clear();
			return oldval;
		}

		Object[] oldkeys = keys;
		Object[] oldvals = values;

		allocate( oldkeys.length - 1 );

		int newindex = 0;
		for( int i=0; i<oldkeys.length; i++ ){
			if( oldkeys[i] == key ) oldval = oldvals[i];
			else{
				keys[newindex] = oldkeys[i];
				values[newindex] = oldvals[i];
				++newindex;
			}
		}

		if( myKeySet != null ) myKeySet.remove( key );
		if( myValueSet != null ) myValueSet.remove( oldval );

		return oldval;
	}

	public void putAll( Map t ){
		Object key;
		Map util = new HashMap( this.size() + t.size() );
		for( int i=0; i<keys.length; i++ ) util.put( keys[i], values[i] );
		for( Iterator it = t.keySet().iterator(); it.hasNext(); ) util.put( key = it.next(), t.get( key ) );
		//util.putAll( t );
		this.clear();
		allocate( util.size() );

		int i = 0;
		for( Iterator it = util.keySet().iterator(); it.hasNext(); ){
			key = it.next();
			keys[i] = key;
			values[i] = util.get( key );
			++i;
		}
	}

	private void allocate( int size ){
		this.keys   = new Object[ size ];
		this.values = new Object[ size ];
	}

	public void clear(){
		this.keys = null;
		this.values = null;
		this.myKeySet = null;
		this.myValueSet = null;
		clearCache();
	}

	private void clearCache(){
		this.myCachedKey = null;
		this.myCachedValue = null;
	}

	public Set keySet(){
		if( isEmpty() ) return Collections.EMPTY_SET;
		if( myKeySet == null ) myKeySet = new HashSet( size() );
		for( int i=0; i<keys.length; i++ ) myKeySet.add( keys[i] );
		return myKeySet;
	}

	public Collection values(){
		if( isEmpty() ) return Collections.EMPTY_SET;
		if( myValueSet == null ) myValueSet = new HashSet( size() );
		for( int i=0; i<values.length; i++ ) myValueSet.add( values[i] );
		return myValueSet;
	}

	public Set entrySet(){
		throw new UnsupportedOperationException();
	}

	public boolean equals( Object o )
	{
		if( o == this ) return true;

		if( !(o instanceof Map) ) return false;

		Map t = (Map) o;
		if( t.size() != size() ) return false;

		try{
			for( int i=0; i<keys.length; i++ ){
				Object key = keys[i];
				Object value = values[i];
				if( value == null )
					if( !(t.get(key)==null && t.containsKey(key)) ) return false;
				else
					if( !value.equals(t.get(key)) ) return false;
			}
		} catch(ClassCastException unused)   {
			return false;
		} catch(NullPointerException unused) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		if( isEmpty() ) return 0;
		int h = 0;
		for( int i=0; i<keys.length; i++ ) h += hashCode( keys[i], values[i] );
		return h;
	}

	private static int hashCode( Object key, Object value ){
		return ((key   == null)   ? 0 :   key.hashCode()) ^
		   ((value == null)   ? 0 : value.hashCode());
	}

	private Object[] keys;
	private Object[] values;
	private HashSet myKeySet;
	private HashSet myValueSet;
	private Object myCachedKey, myCachedValue;
}
