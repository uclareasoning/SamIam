package edu.ucla.belief.recursiveconditioning;

import java.util.*;

import edu.ucla.util.*;



/** This class iterates/traverses through trees & graphs visiting each node once.
 *
 *  <p>For trees it ensures a parent before child ordering (by using a LIFO queue traversal).
 *  <p>For graphs no such guarantee is given.
 *
 * @author David Allen
 */
public class RCIteratorTraversal extends RCIterator {

    static final private boolean DEBUG_RCIterator = false;

    protected boolean isTree;
    protected ArrayList openNodes = new ArrayList();
    protected ArrayList restartNodes = null;
    protected HashSet closedNodes = null; //not used for trees, only for graphs

    public RCIteratorTraversal( RCNode root) {
        Assert.notNull( root);
        isTree = true;
        openNodes.add( root);
        restartNodes = new ArrayList( openNodes);
    }

    public RCIteratorTraversal( RCNode[] roots) {
        isTree = false;
        closedNodes = new HashSet();
        for( int i=0; i<roots.length; i++) {
            Assert.notNull( roots[i]);
            openNodes.add( roots[i]);
        }
        restartNodes = new ArrayList( openNodes);
    }

    public RCIteratorTraversal( Collection roots) {
        if( roots.size() <= 1) {
            isTree = true;
        }
        else {
            isTree = false;
            closedNodes = new HashSet();
        }
        openNodes.addAll( roots);
        restartNodes = new ArrayList( openNodes);
    }

    public void restart() { openNodes.clear(); openNodes.addAll( restartNodes);}

    public boolean hasNext() { return !openNodes.isEmpty();}

    /** Does not guarantee any particular ordering of the nodes returned.*/
    public RCNode nextNode() {
        if( !hasNext()) {
            throw new NoSuchElementException();
        }

        RCNode ret = (RCNode)openNodes.remove( openNodes.size()-1);
        if( closedNodes != null) { closedNodes.add( ret);}
        if( !ret.isLeaf()) {


			RCIterator chi_itr = ((RCNodeInternalBinaryCache)ret).childIterator();
			while( chi_itr.hasNext()) {
				RCNode chi = chi_itr.nextNode();
				Assert.notNull( chi);
				if( isTree) {
					openNodes.add( chi);
				}
				else {
	                if( !closedNodes.contains( chi)) { openNodes.add( chi);}
				}
			}
        }
        return ret;
    }

}//end class RCIteratorTraversal

