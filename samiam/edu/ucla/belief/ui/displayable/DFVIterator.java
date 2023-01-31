package edu.ucla.belief.ui.displayable;

import java.util.Iterator;

/**
	@author Keith Cascio
	@since 100202
*/
public class DFVIterator implements Iterator
{
	public DFVIterator( Iterator it )
	{
		myIterator = it;
	}

	public boolean hasNext()
	{
		return myIterator.hasNext();
	}

	public Object next()
	{
		return myIterator.next();
	}

	public void remove()
	{
		myIterator.remove();
	}

	public DisplayableFiniteVariable nextDFV()
	{
		return (DisplayableFiniteVariable) myIterator.next();
	}

	protected Iterator myIterator;
}
