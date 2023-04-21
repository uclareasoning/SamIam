package edu.ucla.belief.recursiveconditioning;

import java.util.*;
import java.io.*;
import edu.ucla.util.*;
import edu.ucla.belief.*;

abstract public class RCNode {


	/** Used by DFBnB during allocate memory and by DecompositionStructureUtils during RC object creation.*/
	public int userDefinedInt = -1;

    protected long cutsetInstantiations;
    protected long contextInstantiations;

    protected int[] vars_to_save_mpe = null; //possibly null (only used by MPE1)
    final protected RC rc;

	final protected Set parentNodes = new HashSet();

	RCNodeCache cache = null;

	public boolean isRoot = false;


	/** Warning: by using this constructor you must later call init.*/
    public RCNode(RC rc) {
		this.rc = rc;
		init( -1, -1);
	}

    public RCNode( RC rc, long cutsetInstantiations, long contextInstantiations) {
		this.rc = rc;
		init( cutsetInstantiations, contextInstantiations);
    }

	/** Function which should be called if using default constructor.*/
	abstract void init( Collection acutset, Collection context, boolean includeMPE, ArrayList eo);
	abstract public void initCacheOrder( ArrayList eo);

	protected void init( long cutsetInstantiations, long contextInstantiations) {
        this.cutsetInstantiations = cutsetInstantiations;
        this.contextInstantiations = contextInstantiations;
        if( this.contextInstantiations == 0) { throw new IllegalArgumentException("had contextInstantiations == 0");}
	}

	public Set parentNodes() { return Collections.unmodifiableSet(parentNodes);}

    abstract public boolean isLeaf();
    abstract /*package*/ double recCond();
    abstract /*package*/ double recCondMPE1( int[] tmp_inst);
    abstract /*package*/ double recCondMPE3( double cutoff);
    abstract /*package*/ double recCondMPE4( double cutoff);
    abstract /*package*/ double lookAheadMPE3();
    abstract /*package*/ double lookAheadMPE3_setBaseLineIndexing();
    abstract /*package*/ double lookAheadMPE4();
    abstract /*package*/ Table recCondCutsetJoint();

	final void clearContextIndexing() {
		if( cache != null) {
			cache.lastIndex = -1;
			cache.numInstVars = -1;
		}
	}


	/** For all parents (and their ancestors in the RC object) clear their caches
	*    (e.g. in response to changed evidence or a CPT change).
	*/
	void resetAncestorNodes() {
		resetLocal();
		for( Iterator itr = parentNodes.iterator(); itr.hasNext();) {
			RCNode nd = (RCNode)itr.next();
			nd.resetAncestorNodes();
		}
	}

	/** Removes par from parent set and if no other parents exist, removes self from all children.*/
	void removeParentAndSelfRecursive( RCNode par) {
		parentNodes.remove( par);
		if( parentNodes.size() == 0 && !isLeaf()) {
			for( RCIterator chiitr = ((RCNodeInternalBinaryCache)this).childIterator(); chiitr.hasNext();) {
				RCNode chi = chiitr.nextNode();
				chi.removeParentAndSelfRecursive( this);
			}
		}
	}

    final public long cutsetInstantiations() { return cutsetInstantiations;}
    final public long contextInstantiations() { return contextInstantiations;}


	public void allocRCCaches() {}
	public void allocRCMPECaches() {}
	public void allocRCMPE3Caches() {}
	public void allocRCMPE4Caches() {}

    public String toString() { return "[node:" + cutsetInstantiations + "," + contextInstantiations + "]";}

    public void resetLocal() { }
    public double getCacheFactor() { return 0;}
    public void changeCacheFactor( double cf) {
		throw new UnsupportedOperationException("This class does not support cache factor changes");
    }
    final public RCNodeCache cache() { return cache;}




    static final protected double expectedNumberOfRCCalls_local( long par_cutset_insts, double par_cf,
                                                                 long par_context_insts, double par_estimate) {
        return (double)par_cutset_insts * (( par_cf * par_context_insts) + ((1.0-par_cf) * par_estimate));
    }

	public long numCacheEntries_local_total() { return 0;}
	public long numCacheEntriesMpe_local_total() { return 0;}
	public long numCacheEntries_local_used() { return 0;}
	public long numCacheEntriesMpe_local_used() { return 0;}

	/** This height is calculated by how many levels of nodes
	*   there are, not by how many edges there are between them.
	*   (e.g. leaf nodes have height of 1, their parents have
	*    a height of 2...)
	*/
	abstract public int getHeight();

	abstract public Collection vars( );
	abstract public Collection vars( Collection ret);

}
