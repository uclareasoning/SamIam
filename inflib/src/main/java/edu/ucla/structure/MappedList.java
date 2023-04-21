package edu.ucla.structure;

import java.util.*;
import java.util.regex.*;

/**
* A List of unique values which can be indexed
* as a list and the index can be efficiently
* determined( O(1) time).
*/
public class MappedList implements List, Cloneable
{
	protected ArrayList list;
	protected HashMap map;

	/**
	* Creates an empty MappedList.
	*/
	public MappedList()
	{
		list = new ArrayList();
		map = new HashMap();
	}

	/**
		@author Keith Cascio
		@since 101702
	*/
	public MappedList( MappedList toCopy )
	{
		this.list = (ArrayList) toCopy.list.clone();
		this.map = (HashMap) toCopy.map.clone();
	}

	/** @since  20070419
		@return count matched
		@param  results accumulate matches */
	public int grep( Filter filter, Collection results ){
		int ret = 0;
		Object next;
		for( Iterator it = this.list.iterator(); it.hasNext(); ){
			if( filter.accept( next = it.next() ) ){
				if( results != null ) results.add( next );
				++ret;
			}
		}
		return ret;
	}

	/** @since  20070329
		@return count matched
		@param  invert  invert the sense of the grep, i.e. add non-matches
		@param  results accumulate matches */
	public int grep( Pattern pattern, boolean invert, Collection results ){
		return grep( pattern.matcher( "" ), invert, results );
	}

	/** @since  20070329
		@return count matched
		@param  invert  invert the sense of the grep, i.e. add non-matches
		@param  results accumulate matches */
	public int grep( Matcher matcher, boolean invert, Collection results ){
		int ret = 0;
		Object next;
		for( Iterator it = this.list.iterator(); it.hasNext(); ){
			if( matcher.reset( (next = it.next()).toString() ).find() ^ invert ){
				if( results != null ) results.add( next );
				++ret;
			}
		}
		return ret;
	}

	/**
		@author Keith Cascio
		@since 101702
	*/
	public Object clone()
	{
		return new MappedList( this );
	}

	/**
	* Creates an empty MappedList with specified capacity.
	*/
	public MappedList(int initialCapacity)
	{
		list = new ArrayList(initialCapacity);
		map = new HashMap(initialCapacity);
	}

	/**
	* Creates a MappedList containing Integers corresponding to the ints in values.
	*/
	public MappedList(int[] values)
	{
		this(values.length);
		for (int i = 0; i < values.length; i++)
		{
			add(new Integer(values[i]));
		}
	}

	/**
	* Creates a mapped list containing the elements of collection in
	* the order provided by the collections iterator.
	*/
	public MappedList(Collection c)
	{
		this();
		addAll(c);
	}

	/**
	* Not supported. Throws UnsupportedOperationException.
	*/
	public void add(int index, Object element)
	{
		throw new UnsupportedOperationException();
	}

	/**
	* Appends the specified element to the end of this list.
	* If o already appears in the list it is not appended.
	*/
	public boolean add(Object o)
	{
		if (map.containsKey(o))
		{
			return false;
		}

		map.put(o, new Integer(list.size()));
		list.add(o);
		return true;
	}

	/**
		@author Keith Cascio
		@since 101102
	*/
	/*
	public boolean replace( Object objOld, Object objNew )
	{
		if( map.containsKey( objOld ) )
		{
			return true;
		}

		else return false;
	}*/

	/**
	* Appends all of the elements in the specified collection to the end
	* of this list, in the order that they are returned by the specified
	* collection's iterator. If any of the elements are already contained
	* in the list they are not added again.
	*/
	public boolean addAll(Collection c)
	{
		boolean modified = false;
		Iterator iter = c.iterator();
		while (iter.hasNext())
		{
			if (add(iter.next()))
			{
				modified = true;
			}
		}

		return modified;
	}

	/**
	* Not supported. Throws UnsupportedOperationException
	*/
	public boolean addAll(int index, Collection c)
	{
		throw new UnsupportedOperationException();
	}

	/**
	* Removes all of the elements from this list.
	*/
	public void clear()
	{
		list.clear();
		map.clear();
	}

	/**
	* Returns true if this list contains the specified element.
	*/
	public boolean contains(Object o)
	{
		return map.containsKey(o);
	}

	/**
	* Returns true if this list contains all of the elements of the
	* specified collection.
	*/
	public boolean containsAll(Collection c)
	{
		Iterator iter = c.iterator();
		while (iter.hasNext())
		{
			if (!map.containsKey(iter.next()))
			{
				return false;
			}
		}

		return true;
	}

	/**
	* Compares the specified object with this list for equality.
	*/
	public boolean equals(Object o)
	{
		return list.equals(o);
	}

	/**
	* Returns the element at the specified position in this list.
	*/
	public Object get(int index)
	{
		return list.get(index);
	}

	/**
	* Returns the hash code value for this list.
	*/
	public int hashCode()
	{
		return list.hashCode();
	}

	/**
	* Returns the index in this list of the first occurence of the specified
	* element or -1 if this list does not contain this element.
	*/
	public int indexOf(Object o)
	{
		Integer result = (Integer) map.get(o);
		if (result == null)
		{
			return -1;
		}

		return result.intValue();
	}

	/**
	* Returns true if this list contains no elements.
	*/
	public boolean isEmpty()
	{
		return list.isEmpty();
	}

	/**
	* Returns an iterator over the elements in this list in proper sequence.
	*/
	public Iterator iterator()
	{
		return list.iterator();
	}

	/**
	* Returns the index in this list of the last occurrence of the specifed
	* element, or -1 if this list does not contain this element.
	*/
	public int lastIndexOf(Object o)
	{
		Integer index = (Integer) map.get(o);
		if (index == null)
		{
			return -1;
		}
		else
		{
			return index.intValue();
		}
	}

	/**
	* Returns a list iterator of the elements in this list (in
	* proper sequence).
	*/
	public ListIterator listIterator()
	{
		return list.listIterator();
	}

	/**
	* Returns a list iterator of the elements in this list (in
	* proper sequence) starting at the specified position in
	* this list.
	*/
	public ListIterator listIterator(int index)
	{
		return list.listIterator(index);
	}

	/**
		@author Keith Cascio
		@since 101502
	*/
	public Object remove(int index)
	{
		Object removed = list.remove( index );
		if( removed != null )
		{
			for( int i=index; i<list.size(); i++ )
			{
				map.put( list.get(i), new Integer(i) );
			}
			map.remove( removed );
		}

		return removed;
	}

	/**
	* Not supported. Throws UnsupportedOperationException.
	*/
	public boolean remove(Object o)
	{
		throw new UnsupportedOperationException();
	}

	/**
	* Not supported. Throws UnsupportedOperationException.
	*/
	public boolean removeAll(Collection c)
	{
		throw new UnsupportedOperationException();
	}

	/**
	* Not supported. Throws UnsupportedOperationException.
	*/
	public boolean retainAll(Collection c)
	{
		throw new UnsupportedOperationException();
	}

	/**
	* Replaces the element at the specified position in this list
	* with the specified element. If the array already contains
	* element an IllegalARgumentException is thrown.
	*/
	public Object set(int index, Object element)
	{
		if( list.size() == index )
		{
			add(element);
			return null;
		}

		if( map.containsKey(element) )
		{
			if (((Integer) map.get(element)).intValue() != index)
				throw new IllegalArgumentException( "Attempt to add identical element to MappedList." );
		}

		Object result = list.set( index, element );
		map.remove( result );
		map.put( element, new Integer(index) );
		return result;
	}

	/**
	* Returns the number of elements in this list.
	*/
	public int size()
	{
		return list.size();
	}

	/**
	* Not supported. Throws UnsupportedOperationException.
	*/
	public List subList(int fromIndex, int toIndex)
	{
		throw new UnsupportedOperationException();
	}

	/**
	* Returns an array containing all of the elements in this list
	* proper sequence.
	*/
	public Object[] toArray()
	{
		return list.toArray();
	}

	/**
	* Returns an array containing all of the elements in this list in
	* proper sequence; the runtime type of the returned array is that
	* of the specified array.
	*/
	public Object[] toArray(Object[] a)
	{
		return list.toArray(a);
	}

	/**
	* Swaps the elements at i and j
	*/
	public void swap(int i, int j)
	{
		map.remove(list.get(i));
		map.remove(list.get(j));
		Object temp = list.get(i);
		list.set(i, list.get(j));
		list.set(j, temp);
		map.put(list.get(i), new Integer(i));
		map.put(list.get(j), new Integer(j));
	}

	/**
	* sets the value at location i to null
	*/
	public void clear(int i)
	{
		map.remove(list.get(i));
		list.set(i, null);
	}

	public String toString()
	{
		return list.toString();
	}
}

