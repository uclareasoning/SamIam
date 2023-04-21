package edu.ucla.belief;

import edu.ucla.structure.MappedList;
import edu.ucla.belief.FiniteVariable;
import edu.ucla.util.IntArrays;
import java.util.*;

/**
	A class for mapping from variable instantiations to a linear index.
	@author jpark
	@author Keith Cascio
	@version
*/
public class TableIndex implements Cloneable
{
	private MappedList vars;
	private Set variableSet;
	private int[] size;
	private int[] elementSize;
	//private int[] variableArray;
	private int totalSize;

	public TableIndex( TableIndex toCopy )
	{
		this.vars = new MappedList( toCopy.vars );
		this.variableSet = new HashSet( toCopy.variableSet );
		this.size = (int[]) toCopy.size.clone();
		this.elementSize = (int[]) toCopy.elementSize.clone();
		//this.variableArray = (int[]) toCopy.variableArray.clone();
		this.totalSize = toCopy.totalSize;
	}

	public Object clone()
	{
		return new TableIndex( this );
	}

	/**
	* creates an index over the variables supplied. The ordering will be the
	* order the iterator returns the variables.
	*/
	public TableIndex(Collection variables)
	{
		this.vars = new MappedList(variables);
		this.variableSet = new HashSet(variables);
		//this.size = generateSizes(vars);
		this.size = debugGenerateSizes();
		computeElementSize();
	}

	private TableIndex(MappedList vars, int[] size)
	{
		this.vars = vars;
		this.variableSet = new HashSet(vars);
		this.size = size;
		computeElementSize();
	}

	/**
	* creates an index over the variables.	The first variable is most
	* significant in the resulting index.
	*/
	public TableIndex(Object[] variables)
	{
		this(java.util.Arrays.asList(variables));
	}

	/**
		@author Keith Cascio
		@since 101402
	*/
	public int cardinality( int index )
	{
		return size[index];
	}

	private int[] generateSizes(List v)
	{
		int[] sizes = new int[v.size()];
		for (int i = 0; i < sizes.length; i++)
		{
			sizes[i] = variable(i).size();
		}

		return sizes;
	}

	/**
		@author Keith Cascio
		@since 041602
	*/
	private int[] debugGenerateSizes()
	{
		//System.out.println( "Java TableIndex.debugGenerateSizes():\nvars = " + vars );
		int[] sizes = new int[this.vars.size()];
		for (int i = 0; i < sizes.length; i++)
		{
			sizes[i] = ((FiniteVariable)(this.vars.get(i))).size();
		}

		return sizes;
	}

	/**
		@author Keith Cascio
		@since 041602
	*/
	public List variables()
	{
		return Collections.unmodifiableList( vars );
	}

	/**
	* Returns the position of the variable in the array.
	*/
	public int variableIndex(FiniteVariable fv)
	{
		return vars.indexOf(fv);
	}

	/**
	* Returns the change in index of incrementing the value of the ith dimension by 1
	* while holding all others constant.
	*/
	public int blockSize(int i)
	{
		return elementSize[i];
	}

	/**
	* Returns the change in index of incrementing the value of the dimension corresponding to var by 1
	* while holding all others constant.
	*/
	public int blockSize(FiniteVariable var)
	{
		int index = vars.indexOf(var);
		//System.out.println( "TableIndex.blockSize("+var+")" );
		//System.out.println( "\tindex = " + index );
		//System.out.println( "\tvariables = " + vars );
		//System.out.println( "\t[]elementSize.length = " + elementSize.length );
		return elementSize[index];
	}

	/**
	* Creates and initializes elementSize which contains the cumulative size of each
	* dimension.
	*/
	private void computeElementSize()
	{
		elementSize = new int[size.length];
		totalSize = 1;
		for (int i = elementSize.length - 1; i >= 0; i--)
		{
			elementSize[i] = totalSize;
			totalSize *= size[i];
		}
	}

	/**
	* Returns the variables associated with this index.
	*/
	//public Set variables(){
	//	return variableSet;
	//}

	/**
		@author Keith Cascio
		@since 100702
	*/
	public int getNumVariables()
	{
		return size.length;
	}

	/**
	* returns the total number of elements that the index represents.
	*/
	public int size()
	{
		return totalSize;
	}

	/**
	* Converts the multidimensional index to the corresponding linear index.
	*/
	public int index(int[] mind)
	{
		int total = 0;
		for (int i = 0; i < size.length; i++)
		{
			total += mind[i] * elementSize[i];
		}

		return total;
	}

	/** @since 021704 */
	public int[] fill( Map mapInstantiations, int[] mind )
	{
		int numVars = getNumVariables();
		if( mind == null || (mind.length != numVars) ) mind = new int[numVars];

		FiniteVariable fVar;
		int i=0;
		for( i=0; i<numVars; i++ ){
			fVar = (FiniteVariable) vars.get(i);
			mind[i] = fVar.index( mapInstantiations.get(fVar) );
		}
		if( !mapInstantiations.containsKey( getJoint() ) ) mind[--i] = (int)0;

		return mind;
	}

	/** @since 021704 */
	public int index( Map mapInstantiations )
	{
		int total = 0;

		FiniteVariable fVar;
		int i = 0;
		int stop = vars.size() - 1;
		for( i = 0; i < stop; i++ )
		{
			fVar = (FiniteVariable) vars.get(i);
			if( mapInstantiations.containsKey( fVar ) ) total += fVar.index( mapInstantiations.get(fVar) ) * elementSize[i];
			else return (int)-1;
		}
		fVar = (FiniteVariable) vars.get(i);
		if( mapInstantiations.containsKey( fVar ) ) total += fVar.index( mapInstantiations.get(fVar) ) * elementSize[i];

		return total;
	}

	/** @since 100702 */
	public int index( Object[] instantiations )
	{
		int total = 0;
		FiniteVariable fVar;
		for (int i = 0; i < size.length; i++)
		{
			fVar = (FiniteVariable) vars.get(i);
			total += fVar.index( instantiations[i] ) * elementSize[i];
		}

		return total;
	}

	/**
	* Converts the linear index to the corresponding multidimensional index.
	* If mind!=null, the mind is set to contain the result.
	*/
	public int[] mindex(int index, int[] mind)
	{
		if (mind == null)
		{
			mind = new int[size.length];
		}

		int total = index;
		for (int i = 0; i < mind.length; i++)
		{
			mind[i] = total / elementSize[i];
			total %= elementSize[i];
		}

		return mind;
	}

	/**
	* returns an iterator over the indices in the array.
	*/
	public Iterator iterator()
	{
		return new Iterator();
	}

	/**
	* returns the variable index of the specified dimesion.
	*/
	public FiniteVariable variable(int dim)
	{
		return (FiniteVariable) vars.get(dim);
	}

	/**
		Returns the child variable.
		@author Keith Cascio
		@since 101502
	*/
	public FiniteVariable getJoint()
	{
		return variable( size.length - 1 );
	}

	/**
	* Returns the dimensions that correspond to the given variables.
	*/
	public int[] getDimensionIndices(Object[] variables)
	{
		int[] result = new int[variables.length];
		for (int i = 0; i < result.length; i++)
		{
			result[i] = vars.indexOf(variables[i]);
		}

		return result;
	}

	/**
	* returns the dimensions that correspond to the variables in ind.
	*/
	public int[] getDimensionIndices(TableIndex ind)
	{
		int[] result = new int[ind.vars.size()];
		for (int i = 0; i < result.length; i++)
		{
			result[i] = vars.indexOf(ind.vars.get(i));
		}

		return result;
	}

	public boolean equals(Object obj)
	{
		if (obj == this)
		{
			return true;
		}
		else if (obj instanceof TableIndex)
		{
			TableIndex ind = (TableIndex) obj;
			return vars.equals(ind.vars);
		}
		else return false;
	}

	public int hashCode()
	{
		return vars.hashCode();
	}

	/**
	* Returns an array containing the variable associated with each dimension.
	*/
	/*
	public FiniteVariable[] getVariableArray()
	{
		return (FiniteVariable[]) vars.toArray( new FiniteVariable[vars.size()] );
	}*/

	public FiniteVariable[] getParents()
	{
		FiniteVariable[] ret = new FiniteVariable[ vars.size() - 1 ];
		for( int i=0; i<ret.length; i++ )
		{
			ret[i] = (FiniteVariable) vars.get(i);
		}
		return ret;
	}
	public HashSet getParentsSet()
	{
		int size = vars.size()-1; //only parents, not child which is the last variable
		HashSet ret = new HashSet(size);

		for( int i=0; i<size; i++ )
		{
			ret.add(vars.get(i));
		}
		return ret;
	}

	/**
	* Returns the Index which corresponds to basically the cartesian product of the two arrays.
	*/
	public TableIndex multiply(TableIndex ind2)
	{
		MappedList newvars = new MappedList(vars);
		for (int i = 0; i < ind2.vars.size(); i++)
		{
			newvars.add(ind2.vars.get(i));
		}

		int[] newsize = new int[newvars.size()];
		for (int i = 0; i < size.length; i++)
		{
			int ind = newvars.indexOf(vars.get(i));
			newsize[ind] = size[i];
		}

		for (int i = 0; i < ind2.size.length; i++)
		{
			int ind = newvars.indexOf(ind2.vars.get(i));
			newsize[ind] = ind2.size[i];
		}

		return new TableIndex(newvars, newsize);
	}

	/**
	* Returns a new index formed from limiting the current index to the variables contained in variables.
	* note: variables need not be restricted to those in the current index, but the result will only contain
	* variables in the intersection.
	*/
	public TableIndex project(Set variables)
	{
		MappedList newvars = new MappedList();
		for (int i = 0; i < vars.size(); i++)
		{
			Object var = vars.get(i);
			if (variables.contains(var))
			{
				newvars.add(var);
			}
		}

		int[] newsize = new int[newvars.size()];
		for (int i = 0; i < newsize.length; i++)
		{
			int ind = vars.indexOf(newvars.get(i));
			newsize[i] = size[ind];
		}

		return new TableIndex(newvars, newsize);
	}

	/**
	* Returns a new index formed by removing any dimensions whose associated with the variables contained in variables.
	*/
	public TableIndex forget(Set variables)
	{
		MappedList newvars = new MappedList();
		for (int i = 0; i < vars.size(); i++)
		{
			Object var = vars.get(i);
			if (!variables.contains(var))
			{
				newvars.add(var);
			}
		}

		int[] newsize = new int[newvars.size()];
		for (int i = 0; i < newsize.length; i++)
		{
			int ind = vars.indexOf(newvars.get(i));
			newsize[i] = size[ind];
		}

		return new TableIndex(newvars, newsize);
	}

	/**
	* Returns an array where result[i] is the entry in this index that is
	* compatible with the ith entry in ind2.
	*/
	public int[] intoMapping(TableIndex ind2)
	{
		int[] result = new int[ind2.size()];
		int[] perm = ind2.getDimensionIndices(this);
		if (perm.length == 0)
		{
			return result;
		}

		int[] mind = null;
		TableIndex.Iterator iter = ind2.iterator();
		while (iter.hasNext())
		{
			int sind = iter.next();
			mind = IntArrays.apply(iter.current(), perm, mind);
			result[sind] = index(mind);
		}

		return result;
	}

	/**
	* Returns an array where result[i] is the entry in ind2 that is
	* compatible with the ith entry of this index and is compatible
	* with inst.
	* @param ind2	The TableIndex to shrink from
	* @param inst The mapping from variable to value for some subset
	* of the variables in ind2 that aren't in this.
	*/
	public int[] shrinkMapping(TableIndex ind2, Map inst)
	{
		int[] result = new int[size()];
		int[] perm = ind2.getDimensionIndices(this);
		int[] mind = new int[ind2.vars.size()];
		java.util.Arrays.fill(mind, -1);
		for (int i = 0; i < mind.length; i++)
		{
			Object v = ind2.vars.get(i);
			if (inst.keySet().contains(v))
			{
				FiniteVariable fv = (FiniteVariable) v;
				mind[i] = fv.index(inst.get(v));
			}
			else
			{
				mind[i] = -1;
			}
		}

		TableIndex.Iterator iter = iterator();
		while (iter.hasNext())
		{
			int sind = iter.next();
			int[] vals = iter.current();
			for (int i = 0; i < perm.length; i++)
			{
				mind[perm[i]] = vals[i];
			}

			result[sind] = ind2.index(mind);
		}

		return result;
	}

	private int intoMapping(TableIndex ind2, int[] result)
	{
		int[] perm = ind2.getDimensionIndices(this);
		int[] mind = null;
		TableIndex.Iterator iter = ind2.iterator();
		int sind = 0;
		while (iter.hasNext())
		{
			sind = iter.next();
			mind = IntArrays.apply(iter.current(), perm, mind);
			result[sind] = index(mind);
		}

		return sind + 1;
	}

	/**
	* A class for stepping through the instantiations of an index.
	*/
	public class Iterator implements edu.ucla.util.IntIterator
	{
		boolean hasNext = true;
		int currentIndex;
		int lastIndex;
		int[] instance;
	/**
	 * creates an iterator.
		*/
		public Iterator()
		{
			currentIndex = -1;
			instance = new int[size.length];
			//set back one so next will increment to first.
			if (instance.length > 0)
			{
				instance[instance.length - 1] = -1;
			}

			lastIndex = totalSize - 1;
		}

	/**
	 * returns true if some index values still remain to be visited.
		*/
		public boolean hasNext()
		{
			return currentIndex < lastIndex;
		}

	/**
	 * returns the next linear index.
		*/
		public int next()
		{
			currentIndex++;
			for (int i = instance.length - 1; i >= 0; i--)
			{
				if (instance[i] + 1 < size[i])
				{
					instance[i]++;
					return currentIndex;
				}
				else
				{
					instance[i] = 0;
				}
			}

			return currentIndex;
		}

	/**
	 * returns the current instantiation.
		*/
		public int[] current()
		{
			return instance;
		}
	}
}

