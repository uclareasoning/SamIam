package edu.ucla.belief.io.hugin;

import java.util.*;
import edu.ucla.belief.*;
import edu.ucla.belief.io.*;

public class HuginPotential
{
	public final Map values;
	public final List joints, conditioned;
	public final boolean relaxed;

	public HuginPotential(List joints, List conditioned, Map values)
	{
		this( joints, conditioned, values, false );
	}

	/** @since 20060804 */
	public HuginPotential( List joints, List conditioned, Map values, boolean relaxed )
	{
		this.joints      = joints;
		this.conditioned = conditioned;
		this.values      = values;
		this.relaxed     = relaxed;
	}

	public String toString(){
		return "HuginPotential (" + joints + "|" + conditioned + "){" + values + "}";
	}

	public Potential makePotential( Map IDsToVars )
	{
		//System.out.println( "\n\n************************************\nHuginPotential.makePotential()" );
		if (joints.size() != 1)
		{
			throw new IllegalArgumentException("Only potentials of the form (A| B,...) are supported. Potential ("+
				joints + "|"+conditioned + "is not of this form");
		}

		ArrayList list = new ArrayList(conditioned.size() + 1);

		//Keith Cascio 052402
		FiniteVariable fVarTemp = null;
		for (int i = 0; i < conditioned.size(); i++)
		{
			fVarTemp = (FiniteVariable)IDsToVars.get(conditioned.get(i));
			if( fVarTemp == null )
			{
				System.err.println( "Error in HuginPotential.makePotential(): FiniteVariable not found for node name '" + conditioned.get(i) + "' in Map " + IDsToVars );
				return null;
			}
			else list.add( fVarTemp );
		}

		fVarTemp = (FiniteVariable)IDsToVars.get(joints.get(0));
		if( fVarTemp == null )
		{
			System.err.println( "Error in HuginPotential.makePotential(): FiniteVariable not found for node name '" + joints.get(0) + "' in Map " + IDsToVars );
			return null;
		}
		else list.add( fVarTemp );
		//Keith Cascio 052402

		TableIndex ind = new TableIndex(list);
		List dataList = (List) values.get( PropertySuperintendent.KEY_HUGIN_potential_data );
		int dataSize = ind.size();
		if( this.relaxed ){
			dataSize = (dataList == null) ? 0 : deepSize( dataList );
		}
		double[] data = new double[ dataSize ];

		if( dataList == null ){
			//Fill it with equal values. This is what hugin does apparently.
			FiniteVariable joint = (FiniteVariable) IDsToVars.get( joints.get(0) );
			double val = 1.0 / joint.size();
			java.util.Arrays.fill( data, val );
		}
		else{
			int len = load( dataList, data, 0 );
			if( len != data.length ){
				throw new IllegalArgumentException( "Length of parsed probability data, "+len+", does not match length implied by definition: "+data.length );
			}
		}

		Table ret = new Table( ind, data, !this.relaxed );
		return ret;
	}

	/** @since 20060804 */
	static public int deepSize( List list ){
		int size = 0;
		Object next;
		for( Iterator it = list.iterator(); it.hasNext(); ){
			if( (next = it.next()) instanceof List ) size += deepSize( (List)next );
			else ++size;
		}
		return size;
	}

	private int load( List l, double[] d, int start ){
	  int size = l.size();
	  if( size < 1 ) return start;
	  int ret = start;
	  Object current = l.get(0);

	  try
	  {
	    if( current instanceof Number ){
	      for( int i = 0; i < size; i++ ){
	        d[start + i] = ((Number) (current = l.get(i))).doubleValue();
	      }
	      ret = start + size;
	      current = null;
	    }
	    else if( current instanceof List ){
	      for( Iterator it = l.iterator(); it.hasNext(); ){
	        start = load( (List) (current = it.next()), d, start);
	      }
	      ret = start;
	      current = null;
	    }
	  }catch( ClassCastException classcastexception ){
	  	System.err.println( "warning! HuginPotential.load() caught " + classcastexception );
	  }
	  finally{
	    if( current != null ){
	      StringBuffer buff = new StringBuffer( 128 );
	      buff.append( "Invalid data '" );
	      buff.append( current.toString() );
	      buff.append( "' encountered when loading potential for variable \"" );
	      if( (joints != null) && (!joints.isEmpty()) ) buff.append( joints.iterator().next().toString() );
	      buff.append( "\"" );
	      throw new IllegalArgumentException( buff.toString() );
	    }
	  }
	  return ret;
	}
}
