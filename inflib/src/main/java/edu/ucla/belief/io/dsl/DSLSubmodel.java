package edu.ucla.belief.io.dsl;

import java.awt.Point;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/**
	@author Keith Cascio
	@since 041902
*/
public class DSLSubmodel
{
	public Object userobject;

	public DSLSubmodel( int handle )
	{
		this.myHandle = handle;
		this.myLocation = new Point();
		//this.myName = name;
	}

	public int getHandle()
	{
		return myHandle;
	}

	public String getName()
	{
		return myName;
	}

	public void setName( String newName )
	{
		myName = newName;
	}

	public void setLocation( Point p )
	{
		myLocation.setLocation( p );
	}

	public Point getLocation( Point ret )
	{
		if( ret == null ) ret = new Point();
		ret.x = myLocation.x;
		ret.y = myLocation.y;
		return ret;
	}

	public void addChildSubmodel( DSLSubmodel child )
	{
		if( myChildSubmodels == null ) myChildSubmodels = new HashSet();
		myChildSubmodels.add( child );
		child.myParent = this;
	}

	public Iterator getChildSubmodels()
	{
		if( myChildSubmodels == null ) return Collections.EMPTY_SET.iterator();
		return myChildSubmodels.iterator();
	}

	/** @since 072304 */
	public int getNumChildSubmodels()
	{
		if( myChildSubmodels == null ) return (int)0;
		return myChildSubmodels.size();
	}

	public String toString()
	{
		return getName();
	}

	public DSLSubmodel getParent()
	{
		return myParent;
	}

	private DSLSubmodel myParent = null;
	private int myHandle;
	private String myName;
	private Point myLocation;
	private Set myChildSubmodels = null;
}
