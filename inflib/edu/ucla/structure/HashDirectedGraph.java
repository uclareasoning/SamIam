/*
* HashDirectedGraph.java
*
* Created on August 31, 1999, 7:07 AM
*/
package edu.ucla.structure;

import edu.ucla.belief.io.NodeLinearTask;

// profiling ...
//import edu.ucla.belief.io.NetworkIO;
//import edu.ucla.util.JVMTI;
// ... profiling

import java.util.*;
import java.io.*;

/**
* An implementation of a dirrected graph which stores the nodes and edges in a hash table
* to allow O(1) testing for inclusion,
* insertion, removal etc.
* @author jpark
* @author Keith Cascio
* @version 1.0
*/
public class HashDirectedGraph extends AbstractDirectedGraph implements DirectedGraph
{
	protected Set vertices = null;
	protected Map out = null;
	protected Map in = null;

	public static final PrintStream STREAM_DEBUG = System.out;

	/** Creates new HashDirectedGraph */
	public HashDirectedGraph()
	{
		vertices = new HashSet();
		out = new HashMap();
		in = new HashMap();
	}

	public HashDirectedGraph(int size)
	{
		vertices = new HashSet(size);
		out = new HashMap(size);
		in = new HashMap(size);
	}

	public HashDirectedGraph(DirectedGraph g)
	{
		vertices = new HashSet();
		out = new HashMap();
		in = new HashMap();
		Iterator verts = g.vertices().iterator();
		while (verts.hasNext())
		{
			Object v = verts.next();
			add(v);
			Iterator ins = g.inComing(v).iterator();
			while (ins.hasNext())
			addEdge(ins.next(), v);
			Iterator outs = g.outGoing(v).iterator();
			while (outs.hasNext())
			addEdge(v, outs.next());
		}
	}

	/**
		@author Keith Cascio
		@since 081502
	*/
	public Object clone()
	{
		HashDirectedGraph result = new HashDirectedGraph();
		result.vertices = new HashSet( vertices );
		result.in = cloneAdjacencyMap( in );
		result.out = cloneAdjacencyMap( out );
		return result;
	}

	/**
		@author Keith Cascio
		@since 081502
	*/
	public Map cloneAdjacencyMap( Map map )
	{
		Map ret = new HashMap();
		Object objTemp = null;
		Set setTemp = null;
		for(Iterator it = map.keySet().iterator(); it.hasNext(); )
		{
			objTemp = it.next();
			setTemp = (Set) map.get( objTemp );
			ret.put( objTemp, new HashSet( setTemp ) );
		}

		return ret;
	}

	protected static boolean DEBUG = false;

	/*public void replaceVerticesOriginalOptimization( Map verticesOldToNew, NodeLinearTask task )
	{
		int      size = HashDirectedGraph.this.vertices.size();
		Object[] keys = HashDirectedGraph.this.vertices.toArray( new Object[size] );
		Object[] replacements = new Object[size];
		for( int i=0; i<size; i++ ){
			replacements[i] = verticesOldToNew.get( keys[i] );
		}
		Object   retired, replacement;
		for( int i=0; i<size; i++ ){
			retired = keys[i];
			if( (replacement = replacements[i]) != null ){
				HashDirectedGraph.this.vertices.remove( retired     );
				HashDirectedGraph.this.vertices.add(    replacement );
			}
			replaceVerticesOriginalOptimization( HashDirectedGraph.this.in,  (Set) HashDirectedGraph.this.in.get(  retired ), keys, retired, replacements, replacement );
			replaceVerticesOriginalOptimization( HashDirectedGraph.this.out, (Set) HashDirectedGraph.this.out.get( retired ), keys, retired, replacements, replacement );
			if( Thread.currentThread().isInterrupted() ) return;
			if( task != null ) task.touch();
		}
	}

	private void replaceVerticesOriginalOptimization(
		Map            adjacency,
		Set            set,
		Object[]       keys,
		Object         retired,
		Object[]       replacements,
		Object         replacement )
	{
		if( replacement != null ){
			adjacency.remove( retired );
			adjacency.put(    replacement, set );
		}

		int size = keys.length;
		for( int j=0; j<size; j++ ){
			if( ((replacement = replacements[j]) != null) && set.contains( retired = keys[j] ) ){
				set.remove( retired     );
				set.add(    replacement );
			}
		}
	}*/

	/*public void replaceAllVertices( Map verticesOldToNew, NodeLinearTask task )
	{
		int      size = HashDirectedGraph.this.vertices.size();
		Object[] keys = HashDirectedGraph.this.vertices.toArray( new Object[size] );
		Object[] replacements = new Object[size];
		for( int i=0; i<size; i++ ){
			replacements[i] = verticesOldToNew.get( keys[i] );
		}
		Object   retired, replacement;
		Set      setIn, setOut;
		for( int i=0; i<size; i++ ){
			HashDirectedGraph.this.vertices.remove( retired     = keys[i]         );
			HashDirectedGraph.this.vertices.add(    replacement = replacements[i] );

			//replaceAllVertices( HashDirectedGraph.this.in,  keys, retired, replacements, replacement );
			//replaceAllVertices( HashDirectedGraph.this.out, keys, retired, replacements, replacement );

			HashDirectedGraph.this.in.put(  replacement, setIn  = (Set) HashDirectedGraph.this.in.remove(  retired ) );
			HashDirectedGraph.this.out.put( replacement, setOut = (Set) HashDirectedGraph.this.out.remove( retired ) );

			for( int j=0; j<size; j++ ){
				retired     = keys[j];
				replacement = replacements[j];
				if( setIn.contains(  retired ) ){
					setIn.remove(    retired     );
					setIn.add(       replacement );
				}
				if( setOut.contains( retired ) ){
					setOut.remove(   retired     );
					setOut.add(      replacement );
				}
				//if( setIn.remove(  retired ) ) setIn.add(  replacement );//slower!
				//if( setOut.remove( retired ) ) setOut.add( replacement );//slower!
			}

			if( Thread.currentThread().isInterrupted() ) return;
			if( task != null ) task.touch();
		}
	}*/

	/** @since 20060520 */
	/*private void replaceAllVertices(
		Map            adjacency,
		Object[]       keys,
		Object         retired,
		Object[]       replacements,
		Object         replacement )
	{
		adjacency.put( replacement, adjacency.remove( retired ) );

		int size = keys.length;
		for( int j=0; j<size; j++ ){
			if( set.contains( retired = keys[j] ) ){
				set.remove( retired     );
				set.add(    replacements[j] );
			}
			//if( set.remove( keys[j] ) ) set.add( replacements[j] );//slower!
		}
	}*/

	/* profiling:

	   before inlining 'all':
		warmup
			normal:    3906250000
			all:       3687500000
			sparse:    4390625000
		sprint
			normal:    3718750000
			all:       3562500000
			sparse:    3156250000

	   after inlining 'all':
		warmup
			normal:    3843750000
			all:       3140625000
			sparse:    3968750000
		sprint
			normal:    3734375000
			all:       3093750000
			sparse:    3156250000
	*/

	/*public void replaceSparseVertices( Map verticesOldToNew, NodeLinearTask task )
	{
		int      size         = verticesOldToNew.size();
		Object[] keys         = verticesOldToNew.keySet().toArray( new Object[size] );
		Object[] replacements = new Object[size];
		Object   vertex, retired, replacement;
		for( int i=0; i<size; i++ ){
			if( HashDirectedGraph.this.vertices.contains( retired = keys[i] ) ){
				replacements[i] = verticesOldToNew.get(   retired );
			}
		}

		Set setIn, setOut;
		for( Iterator it = HashDirectedGraph.this.vertices.iterator(); it.hasNext(); ){
			vertex = it.next();
			//replaceSparseVertices( (Set) HashDirectedGraph.this.in.get(  vertex ), keys, replacements );
			//replaceSparseVertices( (Set) HashDirectedGraph.this.out.get( vertex ), keys, replacements );
			setIn  = (Set) HashDirectedGraph.this.in.get(  vertex );
			setOut = (Set) HashDirectedGraph.this.out.get( vertex );
			for( int i=0; i<size; i++ ){
				retired = keys[i];
				if( (replacement = replacements[i]) != null ){
					if( setIn.contains( retired ) ){
						setIn.remove(   retired     );
						setIn.add(      replacement );
					}
					if( setOut.contains( retired ) ){
						setOut.remove(   retired     );
						setOut.add(      replacement );
					}
					//if( setIn.remove(  retired ) ) setIn.add(  replacement );//slower!
					//if( setOut.remove( retired ) ) setOut.add( replacement );//slower!
				}
			}
			if( Thread.currentThread().isInterrupted() ) return;
			if( task != null ) task.touch();
		}

		for( int i=0; i<size; i++ ){
			retired = keys[i];
			if( (replacement = replacements[i]) != null ){
				HashDirectedGraph.this.vertices.remove(   retired     );
				HashDirectedGraph.this.vertices.add(      replacement );

				HashDirectedGraph.this.in.put(  replacement, HashDirectedGraph.this.in.remove(  retired ) );
				HashDirectedGraph.this.out.put( replacement, HashDirectedGraph.this.out.remove( retired ) );
			}
		}
	}*/

	/** @since 20060520 */
	/*private void replaceSparseVertices(
		Set            set,
		Object[]       keys,
		Object[]       replacements )
	{
		Object retired;
		int size = keys.length;
		for( int j=0; j<size; j++ ){
			if( set.contains( retired = keys[j] ) ){
				set.remove( retired     );
				set.add(    replacements[j] );
			}
			//if( set.remove( keys[j] ) ) set.add( replacements[j] );//slower!
		}
	}*/

	/** @since 20060520 */
	/*private void replaceVertices( Map adjacency, Object[] keys, Object[] replacements, NodeLinearTask task )
	{
		Object retired, replacement;
		Set    set;
		int    size = keys.length;
		for( int i=0; i<size; i++ ){
			if( ((replacement = replacements[i]) != null) ){
				adjacency.put( replacement, set = (Set) adjacency.remove( keys[i] ) );
			}

			for( int j=0; j<size; j++ ){
				if( ((replacement = replacements[j]) != null) && set.contains( retired = keys[j] ) ){
					set.remove( retired     );
					set.add(    replacement );
				}
				//if( ((replacement = replacements[j]) != null) && set.remove( keys[j] ) ){
				//	set.add( replacement );
				//}//slower!
			}

			if( Thread.currentThread().isInterrupted() ) return;
			if( task != null ) task.touch();
		}
	}*/

	/* profiling:

		normal thread/process priority
		warmup
			original (complete data):   33218750000
			normal   (complete data):    3671875000  904% speedup
			all      (complete data):    2843750000 1168% speedup
			sparse   (complete data):    2890625000 1149% speedup
		sprint
			original (sparse   data):   16093750000
			normal   (sparse   data):    1953125000  824% speedup
			all      (complete data):    2781250000
			sparse   (sparse   data):    1625000000  990% speedup

			original (junky    data):   15312500000
			normal   (junky    data):    1984375000  771% speedup
			all      (complete data):    2859375000
			sparse   (junky    data):    1812500000  844% speedup

			original (complete data):   33703125000
			normal   (complete data):    3625000000  929% speedup
			all      (complete data):    2843750000 1185% speedup
			sparse   (complete data):    2843750000 1185% speedup

		normal thread/process priority
		warmup
			original (complete data):   32390625000
			normal   (complete data):    3546875000  913% speedup
			all      (complete data):    2796875000 1158% speedup
			sparse   (complete data):    2765625000 1171% speedup
		sprint
			original (sparse   data):   14062500000
			normal   (sparse   data):    1828125000  769% speedup
			all      (complete data):    2734375000
			sparse   (sparse   data):    1546875000  909% speedup

			original (junky    data):   14984375000
			normal   (junky    data):    1875000000  799% speedup
			all      (complete data):    2781250000
			sparse   (junky    data):    1687500000  887% speedup

			original (complete data):   31750000000
			normal   (complete data):    3484375000  911% speedup
			all      (complete data):    2765625000 1148% speedup
			sparse   (complete data):    2750000000 1154% speedup

		normal thread/process priority
		warmup
			original (complete data):   32921875000
			normal   (complete data):    3968750000  830% speedup
			all      (complete data):    3015625000 1092% speedup
			sparse   (complete data):    3015625000 1092% speedup
		sprint
			original (sparse   data):   17078125000
			normal   (sparse   data):    1890625000  903% speedup
			all      (complete data):    3062500000
			sparse   (sparse   data):    1578125000 1082% speedup

			original (junky    data):   17109375000
			normal   (junky    data):    1875000000  912% speedup
			all      (complete data):    3015625000
			sparse   (junky    data):    1765625000  969% speedup

			original (complete data):   35125000000
			normal   (complete data):    3921875000  896% speedup
			all      (complete data):    3109375000 1130% speedup
			sparse   (complete data):    3109375000 1130% speedup

		high thread/process priority
		warmup
			original (complete data):   29234375000
			normal   (complete data):    3656250000  800% speedup
			all      (complete data):    2843750000 1028% speedup
			sparse   (complete data):    2750000000 1063% speedup
		sprint
			original (sparse   data):   14640625000
			normal   (sparse   data):    1796875000  815% speedup
			all      (complete data):    2750000000
			sparse   (sparse   data):    1531250000  956% speedup

			original (junky    data):   12812500000
			normal   (junky    data):    1781250000  719% speedup
			all      (complete data):    2765625000
			sparse   (junky    data):    1718750000  745% speedup

			original (complete data):   31109375000
			normal   (complete data):    3515625000  885% speedup
			all      (complete data):    2765625000 1125% speedup
			sparse   (complete data):    2843750000 1094% speedup

		high thread/process priority
		warmup
			original (complete data):   29640625000
			normal   (complete data):    3515625000  843% speedup
			all      (complete data):    2703125000 1097% speedup
			sparse   (complete data):    2718750000 1090% speedup
		sprint
			original (sparse   data):   14906250000
			normal   (sparse   data):    1796875000  830% speedup
			all      (complete data):    2656250000
			sparse   (sparse   data):    1546875000  964% speedup

			original (junky    data):   14656250000
			normal   (junky    data):    1781250000  823% speedup
			all      (complete data):    2656250000
			sparse   (junky    data):    1671875000  877% speedup

			original (complete data):   30046875000
			normal   (complete data):    3453125000  870% speedup
			all      (complete data):    2687500000 1118% speedup
			sparse   (complete data):    2750000000 1093% speedup

		after replacing HashSet.contains() with HashSet.remove(), notice the slowdown
		warmup
			original (complete data):   33281250000
			normal   (complete data):    4000000000  832% speedup
			all      (complete data):    3796875000  877% speedup
			sparse   (complete data):    3890625000  855% speedup
		sprint
			original (sparse   data):   17265625000
			normal   (sparse   data):    1875000000  921% speedup
			all      (complete data):    3781250000
			sparse   (sparse   data):    1953125000  884% speedup

			original (junky    data):   17296875000
			normal   (junky    data):    1890625000  915% speedup
			all      (complete data):    3796875000
			sparse   (junky    data):    2093750000  826% speedup

			original (complete data):   35406250000
			normal   (complete data):    3968750000  892% speedup
			all      (complete data):    3843750000  921% speedup
			sparse   (complete data):    3796875000  933% speedup

		after replacing HashSet.contains() with HashSet.remove(), notice the slowdown
		warmup
			original (complete data):   32203125000
			normal   (complete data):    3984375000  808% speedup
			all      (complete data):    3812500000  845% speedup
			sparse   (complete data):    3796875000  848% speedup
		sprint
			original (sparse   data):   16812500000
			normal   (sparse   data):    2109375000  797% speedup
			all      (complete data):    3812500000
			sparse   (sparse   data):    2078125000  809% speedup

			original (junky    data):   15140625000
			normal   (junky    data):    2109375000  718% speedup
			all      (complete data):    3796875000
			sparse   (junky    data):    2156250000  702% speedup

			original (complete data):   33687500000
			normal   (complete data):    3953125000  852% speedup
			all      (complete data):    3828125000  880% speedup
			sparse   (complete data):    3796875000  887% speedup

		actually, no to the above, original, when implemented correctly, is far faster
		warmup
			original (complete data):      78125000
			normal   (complete data):    4125000000    2% speedup
			all      (complete data):    3125000000    3% speedup
			sparse   (complete data):    3140625000    2% speedup
		sprint
			original (sparse   data):      31250000
			normal   (sparse   data):    1921875000    2% speedup
			all      (complete data):    3109375000
			sparse   (sparse   data):    1687500000    2% speedup

			original (junky    data):      46875000
			normal   (junky    data):    1859375000    3% speedup
			all      (complete data):    3093750000
			sparse   (junky    data):    1796875000    3% speedup

			original (complete data):      46875000
			normal   (complete data):    4062500000    1% speedup
			all      (complete data):    3093750000    2% speedup
			sparse   (complete data):    3125000000    2% speedup
	*/

	/** @since 20060521 */
	/*public static final String[] ARRAY_PROFILING_NOTES = new String[]{
		"warmup original complete old2new 1",
		"warmup original complete new2old 2",
		"warmup sparse   complete old2new 1",
		"warmup sparse   complete new2old 2",
		"warmup normal   complete old2new 1",
		"warmup normal   complete new2old 2",
		"warmup all      complete old2new 1",
		"warmup all      complete new2old 2",

		"sprint original sparse   old2new 1",
		"sprint original sparse   new2old 2",
		"sprint sparse   sparse   old2new 1",
		"sprint sparse   sparse   new2old 2",
		"sprint normal   sparse   old2new 1",
		"sprint normal   sparse   new2old 2",
		"sprint all      sparse   old2new 1",
		"sprint all      sparse   new2old 2",

		"sprint original junky    old2new 1",
		"sprint original junky    new2old 2",
		"sprint sparse   junky    old2new 1",
		"sprint sparse   junky    new2old 2",
		"sprint normal   junky    old2new 1",
		"sprint normal   junky    new2old 2",
		"sprint all      junky    old2new 1",
		"sprint all      junky    new2old 2",

		"sprint original complete old2new 1",
		"sprint original complete new2old 2",
		"sprint sparse   complete old2new 1",
		"sprint sparse   complete new2old 2",
		"sprint normal   complete old2new 1",
		"sprint normal   complete new2old 2",
		"sprint all      complete old2new 1",
		"sprint all      complete new2old 2",

		"profiling finished"
	};*/

	/** @since 20060521 */
	/*public void runProfileScript( Map variablesOldToNew, NodeLinearTask task ){
		int size     = variablesOldToNew.size();
		int sizeHalf = size/2;
		int sizeJunk = sizeHalf*3;

		Map completeOldToNew = variablesOldToNew;
		Map completeNewToOld = new HashMap(   size );

		Map sparseOldToNew   = new HashMap(   size );
		Map sparseNewToOld   = new HashMap(   size );

		Map junkyOldToNew    = new HashMap(   sizeJunk );
		Map junkyNewToOld    = new HashMap(   sizeJunk );

		ArrayList sparseOld  = new ArrayList( size );
		ArrayList junkyOld   = new ArrayList( sizeJunk );

		int count = 0;
		Object nextOld;
		Object nextNew;
		for( Iterator it = completeOldToNew.keySet().iterator(); it.hasNext(); count++ ){
			nextOld = it.next();
			completeNewToOld.put( nextNew = completeOldToNew.get( nextOld ), nextOld );
			if( count < sizeHalf ){
				junkyOld.add(  nextOld );
				sparseOld.add( nextOld );
			}
		}

		for( int i=sizeHalf; i<size; i++ ){
			junkyOld.add(  nextOld = new Object() );
			sparseOld.add( nextOld );
		}
		for( int i=size; i<sizeJunk; i++ ) junkyOld.add( new Object() );

		Collections.shuffle( sparseOld );
		Collections.shuffle( junkyOld );

		for( Iterator it = sparseOld.iterator(); it.hasNext(); ){
			nextOld = it.next();
			nextNew = completeOldToNew.get( nextOld );
			if( nextNew == null ) nextNew = new Object();

			sparseOldToNew.put( nextOld, nextNew );
			sparseNewToOld.put( nextNew, nextOld );
		}

		for( Iterator it = junkyOld.iterator(); it.hasNext(); ){
			nextOld = it.next();
			nextNew = completeOldToNew.get( nextOld );
			if( nextNew == null ) nextNew = new Object();

			junkyOldToNew.put( nextOld, nextNew );
			junkyNewToOld.put( nextNew, nextOld );
		}

		java.text.DecimalFormat format = new java.text.DecimalFormat( "00000%" );
		long pre, sparse, norm, all, orig;
		Map old2new, new2old;

		STREAM_DEBUG.println();
		STREAM_DEBUG.println( "warmup" );

		old2new = completeOldToNew;
		new2old = completeNewToOld;
		pre = JVMTI.getCurrentThreadCpuTimeUnsafe();
		HashDirectedGraph.this.replaceVertices( old2new, task );
		HashDirectedGraph.this.replaceVertices( new2old, task );
		orig = JVMTI.getCurrentThreadCpuTimeUnsafe() - pre;

		pre = JVMTI.getCurrentThreadCpuTimeUnsafe();
		HashDirectedGraph.this.replaceSparseVertices( old2new, task );
		HashDirectedGraph.this.replaceSparseVertices( new2old, task );
		sparse = JVMTI.getCurrentThreadCpuTimeUnsafe() - pre;

		pre = JVMTI.getCurrentThreadCpuTimeUnsafe();
		HashDirectedGraph.this.replaceVerticesOriginalOptimization(    old2new, task );
		HashDirectedGraph.this.replaceVerticesOriginalOptimization(    new2old, task );
		norm = JVMTI.getCurrentThreadCpuTimeUnsafe() - pre;

		pre = JVMTI.getCurrentThreadCpuTimeUnsafe();
		HashDirectedGraph.this.replaceAllVertices( completeOldToNew, task );
		HashDirectedGraph.this.replaceAllVertices( completeNewToOld, task );
		all = JVMTI.getCurrentThreadCpuTimeUnsafe() - pre;

		STREAM_DEBUG.println( "    original (complete data): " + NetworkIO.formatTime( orig   ) );
		STREAM_DEBUG.println( "    normal   (complete data): " + NetworkIO.formatTime( norm   ) + NetworkIO.formatPercent( ((float)orig)/((float)norm  ), format ) + " speedup" );
		STREAM_DEBUG.println( "    all      (complete data): " + NetworkIO.formatTime( all    ) + NetworkIO.formatPercent( ((float)orig)/((float)all   ), format ) + " speedup" );
		STREAM_DEBUG.println( "    sparse   (complete data): " + NetworkIO.formatTime( sparse ) + NetworkIO.formatPercent( ((float)orig)/((float)sparse), format ) + " speedup" );

		STREAM_DEBUG.println( "sprint" );

		old2new = sparseOldToNew;
		new2old = sparseNewToOld;
		pre = JVMTI.getCurrentThreadCpuTimeUnsafe();
		HashDirectedGraph.this.replaceVertices( old2new, task );
		HashDirectedGraph.this.replaceVertices( new2old, task );
		orig = JVMTI.getCurrentThreadCpuTimeUnsafe() - pre;

		pre = JVMTI.getCurrentThreadCpuTimeUnsafe();
		HashDirectedGraph.this.replaceSparseVertices( old2new, task );
		HashDirectedGraph.this.replaceSparseVertices( new2old, task );
		sparse = JVMTI.getCurrentThreadCpuTimeUnsafe() - pre;

		pre = JVMTI.getCurrentThreadCpuTimeUnsafe();
		HashDirectedGraph.this.replaceVerticesOriginalOptimization(    old2new, task );
		HashDirectedGraph.this.replaceVerticesOriginalOptimization(    new2old, task );
		norm = JVMTI.getCurrentThreadCpuTimeUnsafe() - pre;

		pre = JVMTI.getCurrentThreadCpuTimeUnsafe();
		HashDirectedGraph.this.replaceAllVertices( completeOldToNew, task );
		HashDirectedGraph.this.replaceAllVertices( completeNewToOld, task );
		all = JVMTI.getCurrentThreadCpuTimeUnsafe() - pre;

		STREAM_DEBUG.println( "    original (sparse   data): " + NetworkIO.formatTime( orig   ) );
		STREAM_DEBUG.println( "    normal   (sparse   data): " + NetworkIO.formatTime( norm   ) + NetworkIO.formatPercent( ((float)orig)/((float)norm  ), format ) + " speedup" );
		STREAM_DEBUG.println( "    all      (complete data): " + NetworkIO.formatTime( all    ) );
		STREAM_DEBUG.println( "    sparse   (sparse   data): " + NetworkIO.formatTime( sparse ) + NetworkIO.formatPercent( ((float)orig)/((float)sparse), format ) + " speedup" );

		old2new = junkyOldToNew;
		new2old = junkyNewToOld;
		pre = JVMTI.getCurrentThreadCpuTimeUnsafe();
		HashDirectedGraph.this.replaceVertices( old2new, task );
		HashDirectedGraph.this.replaceVertices( new2old, task );
		orig = JVMTI.getCurrentThreadCpuTimeUnsafe() - pre;

		pre = JVMTI.getCurrentThreadCpuTimeUnsafe();
		HashDirectedGraph.this.replaceSparseVertices( old2new, task );
		HashDirectedGraph.this.replaceSparseVertices( new2old, task );
		sparse = JVMTI.getCurrentThreadCpuTimeUnsafe() - pre;

		pre = JVMTI.getCurrentThreadCpuTimeUnsafe();
		HashDirectedGraph.this.replaceVerticesOriginalOptimization(    old2new, task );
		HashDirectedGraph.this.replaceVerticesOriginalOptimization(    new2old, task );
		norm = JVMTI.getCurrentThreadCpuTimeUnsafe() - pre;

		pre = JVMTI.getCurrentThreadCpuTimeUnsafe();
		HashDirectedGraph.this.replaceAllVertices( completeOldToNew, task );
		HashDirectedGraph.this.replaceAllVertices( completeNewToOld, task );
		all = JVMTI.getCurrentThreadCpuTimeUnsafe() - pre;

		STREAM_DEBUG.println();
		STREAM_DEBUG.println( "    original (junky    data): " + NetworkIO.formatTime( orig   ) );
		STREAM_DEBUG.println( "    normal   (junky    data): " + NetworkIO.formatTime( norm   ) + NetworkIO.formatPercent( ((float)orig)/((float)norm  ), format ) + " speedup" );
		STREAM_DEBUG.println( "    all      (complete data): " + NetworkIO.formatTime( all    ) );
		STREAM_DEBUG.println( "    sparse   (junky    data): " + NetworkIO.formatTime( sparse ) + NetworkIO.formatPercent( ((float)orig)/((float)sparse), format ) + " speedup" );

		old2new = completeOldToNew;
		new2old = completeNewToOld;
		pre = JVMTI.getCurrentThreadCpuTimeUnsafe();
		HashDirectedGraph.this.replaceVertices( old2new, task );
		HashDirectedGraph.this.replaceVertices( new2old, task );
		orig = JVMTI.getCurrentThreadCpuTimeUnsafe() - pre;

		pre = JVMTI.getCurrentThreadCpuTimeUnsafe();
		HashDirectedGraph.this.replaceSparseVertices( old2new, task );
		HashDirectedGraph.this.replaceSparseVertices( new2old, task );
		sparse = JVMTI.getCurrentThreadCpuTimeUnsafe() - pre;

		pre = JVMTI.getCurrentThreadCpuTimeUnsafe();
		HashDirectedGraph.this.replaceVerticesOriginalOptimization(    old2new, task );
		HashDirectedGraph.this.replaceVerticesOriginalOptimization(    new2old, task );
		norm = JVMTI.getCurrentThreadCpuTimeUnsafe() - pre;

		pre = JVMTI.getCurrentThreadCpuTimeUnsafe();
		HashDirectedGraph.this.replaceAllVertices( completeOldToNew, task );
		HashDirectedGraph.this.replaceAllVertices( completeNewToOld, task );
		all = JVMTI.getCurrentThreadCpuTimeUnsafe() - pre;

		STREAM_DEBUG.println();
		STREAM_DEBUG.println( "    original (complete data): " + NetworkIO.formatTime( orig   ) );
		STREAM_DEBUG.println( "    normal   (complete data): " + NetworkIO.formatTime( norm   ) + NetworkIO.formatPercent( ((float)orig)/((float)norm  ), format ) + " speedup" );
		STREAM_DEBUG.println( "    all      (complete data): " + NetworkIO.formatTime( all    ) + NetworkIO.formatPercent( ((float)orig)/((float)all   ), format ) + " speedup" );
		STREAM_DEBUG.println( "    sparse   (complete data): " + NetworkIO.formatTime( sparse ) + NetworkIO.formatPercent( ((float)orig)/((float)sparse), format ) + " speedup" );
	}*/

	/** @since 20020603 */
	public void replaceVertices( Map verticesOldToNew, NodeLinearTask task )
	{
		int count = 0;
		Object vertexOld;
		for( Iterator it = verticesOldToNew.keySet().iterator(); it.hasNext(); count++ ){
			vertexOld = it.next();
			replaceVertex( vertexOld, verticesOldToNew.get( vertexOld ) );
			if( Thread.currentThread().isInterrupted() ) return;
			if( task != null ) task.touch();
		}

		if( task != null ){
			int size = HashDirectedGraph.this.vertices.size();
			while( count++ < size ) task.touch();
		}
	}

	/** @since 20020603
		@since 20060523 */
	public void replaceVertex( Object oldVertex, Object newVertex )
	{
		if( vertices.contains( oldVertex ) )
		{
			vertices.remove( oldVertex );
			vertices.add(    newVertex );

			Set parents = (Set) in.remove( oldVertex );
			Set reflection;
			for( Iterator itParents = parents.iterator(); itParents.hasNext(); ){
				reflection = (Set) out.get( itParents.next() );
				reflection.remove( oldVertex );
				reflection.add(    newVertex );
			}
			in.put( newVertex, parents );

			Set children = (Set) out.remove( oldVertex );
			for( Iterator itChildren = children.iterator(); itChildren.hasNext(); ){
				reflection = (Set) in.get( itChildren.next() );
				reflection.remove( oldVertex );
				reflection.add(    newVertex );
			}
			out.put( newVertex, children );
		}
	}

	public String toString()
	{
		StringWriter ret = new StringWriter();
		PrintWriter pout = new PrintWriter( ret );
		pout.println( "HashDirectedGraph vertices:" );//debug
		pout.println( vertices );
		pout.println( "HashDirectedGraph in:" );//debug
		pout.println( in );
		pout.println( "HashDirectedGraph pout:" );//debug
		pout.println( out );
		return ret.toString();
	}

	/** Constructs an Iterator over all vertices.
	* @return Iterator over all vertices.
	*/
	protected Set verticesProtected(){
		return vertices;
	}

	/** Constructs an Iterator over the vertices adjacent to edges entering the specified vertex.
	* <p><dd><dl>
	* <dt> <b>Precondition:</b>
	* <dd> vertex is in the graph(tested by "equals").
	* </dl></dd>
	* @param vertex- An Object which is in the graph.
	* @return Iterator over the vertices adjacent to edges entering vertex.
	*/
	public Set inComing(Object vertex)
	{
		Set s = (Set) in.get(vertex);
		if (s == null)
		return Collections.EMPTY_SET;
		else
		return Collections.unmodifiableSet(s);
	}

	/** Constructs an Iterator over the vertices adjacent to edges leaving the specified vertex.
	* <p><dd><dl>
	* <dt> <b>Precondition:</b>
	* <dd> vertex is in the graph(tested by "equals").
	* </dl></dd>
	* @param vertex- An Object which is in the graph.
	* @return Iterator over the vertices adjacent to edges leaving vertex.
	*/
	public Set outGoing(Object vertex)
	{
		Set s = (Set) out.get(vertex);
		if (s == null)
		return Collections.EMPTY_SET;
		else
		return Collections.unmodifiableSet(s);
	}

	/** Returns the degree of the vertex. This includes both in and out edges
	* <p><dd><dl>
	* <dt> <b>Precondition:</b>
	* <dd> vertex is in the graph(tested by "equals").
	* </dl></dd>
	* @param vertex- An Object which is in the graph.
	* @return the number of vertices adjacent to vertex.
	*/
	public int degree(Object vertex)
	{
		Set s = (Set) in.get(vertex);
		if (s == null)
		return 0;
		else
		return s.size() + ((Set) out.get(vertex)).size();
	}

	/** Returns the number of edges entering the vertex.
	* <p><dd><dl>
	* <dt> <b>Precondition:</b>
	* <dd> vertex is in the graph(tested by "equals").
	* </dl></dd>
	* @param vertex- An Object which is in the graph.
	* @return the number of edges entering the vertex.
	*/
	public int inDegree(Object vertex)
	{
		Set s = (Set) in.get(vertex);
		if (s == null)
		return 0;
		else
		return s.size();
	}

	/** Returns the number of edges leaving the vertex.
	* <p><dd><dl>
	* <dt> <b>Precondition:</b>
	* <dd> vertex is in the graph(tested by "equals").
	* </dl></dd>
	* @param vertex- An Object which is in the graph.
	* @return the number of edges leaving the vertex.
	*/
	public int outDegree(Object vertex)
	{
		Set s = (Set) out.get(vertex);
		if (s == null)
		return 0;
		else
		return s.size();
	}

	/** Returns whether or not a particular edge is in the graph.
	* The edge leaves vertex1 and enters vertex2.
	* <p><dd><dl>
	* <dt> <b>Precondition:</b>
	* <dd> vertex1 and vertex2 are in the graph(tested by "equals").
	* </dl></dd>
	* @param vertex1- An Object which is in the graph.
	* @param vertex2- An Object which is in the graph.
	* @return true if edge (vertex1,vertex2) is in the graph, false otherwise.
	*/
	public boolean containsEdge(Object vertex1, Object vertex2)
	{
		Set s = (Set) out.get(vertex1);
		if (s == null)
		return false;
		else
		return s.contains(vertex2);
	}

	/** Returns whether or not a particular Object is a vertex in the graph.
	* @param vertex- Any Object.
	* @return true if vertex is in the graph(tested by "equals"), false otherwise.
	*/
	public boolean contains(Object vertex)
	{
		return vertices.contains(vertex);
	}

	/**
		@author Keith Cascio
		@since 102402
	*/
	public int numEdges()
	{
		int count = (int)0;
		for( Iterator it = out.values().iterator(); it.hasNext(); )
		{
			count += ((Collection)it.next()).size();
		}
		return count;
	}

	/** Adds vertex to the graph(Optional operation). If the vertex is already a member
	* of the graph, the graph is unchanged and the method returns false, following the
	* Collection convention.
	* <p><dd><dl>
	* <dt> <b>Postcondition:</b>
	* <dd> vertex is in the graph(as tested by "equals").
	* </dl></dd>
	* @param vertex- An Object to be added as a vertex.
	* @return true if the graph was modified(i.e. vertex was not
	* a vertex already) false otherwise.
	*/
	public boolean addVertex(Object vertex)
	{
		boolean added = vertices.add(vertex);
		if (added)
		{
			in.put(vertex, new HashSet());
			out.put(vertex, new HashSet());
		}

		return added;
	}

	/** Removes vertex from the graph(Optional operation). If the vertex is not a member
	* of the graph, the method returns false and leaves the graph unchanged. If the
	* parameter is a vertex of the graph, it is removed and the method returns true.
	* <p><dd><dl>
	* <dt> <b>Postcondition:</b>
	* <dd> vertex is not in the graph(as tested by "equals").
	* </dl></dd>
	* @param vertex- An Object which is currently in the graph.
	*/
	public boolean removeVertex(Object vertex)
	{
		boolean removed = vertices.remove(vertex);
		if (removed)
		{
			Set inlist = (Set) in.get(vertex);
			Iterator iter = inlist.iterator();
			while (iter.hasNext())
			{
				Object vert = iter.next();
				Set s = (Set) out.get(vert);
				s.remove(vertex);
			}

			Set outlist = (Set) out.get(vertex);
			iter = outlist.iterator();
			while (iter.hasNext())
			{
				Object vert = iter.next();
				Set s = (Set) in.get(vert);
				s.remove(vertex);
			}

			in.remove(vertex);
			out.remove(vertex);
		}

		return removed;
	}

	/** Adds the directed edge to the graph(Optional operation). If either of the vertices
	* are not in the graph, they are added, and the edge is added. If the edge was
	* already in the graph, it returns false, otherwise it returns true.
	* <p><dd><dl>
	* <dt> <b>Postcondition:</b>
	* <dd> the edge (vertex1,vertex2) is in the graph.
	* </dl></dd>
	*/
	public boolean addEdge(Object vertex1, Object vertex2)
	{
		boolean result = addVertex(vertex1);
		result |= addVertex(vertex2);
		Set s = (Set) out.get(vertex1);
		result |= s.add(vertex2);
		s = (Set) in.get(vertex2);
		result |= s.add(vertex1);
		return result;
	}

	/** Removes the directed edge from the graph(Optional operation). If the edge is
	* not in the graph when the call is made, it returns false, otherwise it returns true.
	* <p><dd><dl>
	* <dt> <b>Postcondition:</b>
	* <dd> the edge (vertex1,vertex2) is in the graph.
	* </dl></dd>
	*/
	public boolean removeEdge(Object vertex1, Object vertex2)
	{
		if (!contains(vertex1) || !contains(vertex2))
		return false;
		Set s = (Set) out.get(vertex1);
		boolean result = s.remove(vertex2);
		if (result)
		{
			s = (Set) in.get(vertex2);
			s.remove(vertex1);
		}

		return result;
	}

	public void clear()
	{
		vertices.clear();
		in.clear();
		out.clear();
	}

	public int hashCode()
	{
		return vertices.hashCode() ^ in.hashCode() ^ out.hashCode();
	}

	public boolean equals(final java.lang.Object p)
	{
		if( p instanceof HashDirectedGraph )
		{
			HashDirectedGraph dg = (HashDirectedGraph) p;
			return vertices.equals(dg.vertices) && in.equals(dg.in) &&
			out.equals(dg.out);
		}
		else return false;
	}
}
