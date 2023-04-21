package edu.ucla.belief.recursiveconditioning;

import java.util.*;
import java.math.*;

import java.text.*;
import java.io.*;







/** This class contains utility functions for RC objects.
 *
 * @author David Allen
 */
public class RCUtilities {

	private RCUtilities() {} //don't allow creation of objects



	/** When finished, ret will include a mapping from all nodes to their parents in the
	 *  subtree rooted at rt.
	 */
	final static public void computeParentsInTree( RCNode rt, Map ret) {
		if( ret == null) { ret = new HashMap();}

		if( rt.isLeaf()) {
		}
		else {
			RCNodeInternalBinaryCache nd = (RCNodeInternalBinaryCache)rt;
			ret.put( nd.left, rt);
			ret.put( nd.right, rt);
			computeParentsInTree( nd.left, ret);
			computeParentsInTree( nd.right, ret);
		}
	}

	/** Compute leaf nodes in the subtree rooted at rt (will include rt if it is a leaf node).*/
	final static public void computeLeafs( RCNode rt, Collection ret) {
		if( ret == null) { ret = new HashSet();}

		if( rt.isLeaf()) {
			ret.add( rt);
		}
		else {
			RCNodeInternalBinaryCache nd = (RCNodeInternalBinaryCache)rt;
			computeLeafs( nd.left, ret);
			computeLeafs( nd.right, ret);
		}
	}




	/**Calculates the cutset of a variable based only on its children.  To calculate
	 * a "local cutset", acutset can be Collections.EMPTY_SET.
	 */
	final static public Collection calculateCutsetFromChildren( RCNodeInternalBinaryCache nd, Collection acutset) {
		Collection left = nd.left.vars( new HashSet());
		Collection right = nd.right.vars( new HashSet());

		left.retainAll( right); //intersect left and right
		left.removeAll( acutset); //remove acutset vars

		return left;
	}


    /** The ArrayList ord will contain the vars in an
     *   elimination order determined by the subtree rooted at rt (it is not unique).
     *   This assumes that leaf nodes have their contxt set and internal nodes have
     *   their iterators created.
     */
    final static public void getElimOrder( RCNode rt, ArrayList ord) {
        if( ord == null) { ord = new ArrayList();}


		if( rt.isLeaf()) {
			RCNodeLeaf rtleaf = (RCNodeLeaf)rt;
			Collection elim = rtleaf.vars();
			elim.removeAll( rtleaf.context);
			ord.addAll( elim);
		}
		else {
			RCNodeInternalBinaryCache rtibc = (RCNodeInternalBinaryCache)rt;
			getElimOrder( rtibc.left, ord);
			getElimOrder( rtibc.right, ord);

			//add own vars
			ord.addAll( rtibc.itr.getVars(null));//add cutset variables
		}
	}

    /** The ArrayList ord will contain the vars in an
     *   elimination order determined by the subtree rooted at rt (it is not unique).
     *   This assumes that leaf nodes know their variables.
     */
    final static public void getElimOrderUnInitialized( RCNode rt, Collection acutset, ArrayList ord) {
        if( ord == null) { ord = new ArrayList();}


		if( rt.isLeaf()) {
			RCNodeLeaf rtleaf = (RCNodeLeaf)rt;
			Collection elim = rtleaf.vars();
			elim.removeAll( acutset);
			ord.addAll( elim); //add singleton variables
		}
		else {
			RCNodeInternalBinaryCache rtibc = (RCNodeInternalBinaryCache)rt;
			Collection cutset = calculateCutsetFromChildren(rtibc, acutset);

			Collection newAcutset = new HashSet(acutset.size() + cutset.size());
			newAcutset.addAll(acutset);
			newAcutset.addAll(cutset);

			getElimOrderUnInitialized( rtibc.left, newAcutset, ord);
			getElimOrderUnInitialized( rtibc.right, newAcutset, ord);

			//add own vars
			ord.addAll( cutset);//add cutset variables
		}
	}

	final static public int clusterSize( RCNode rt) {
		int ret;
		if( rt.isLeaf()) {
			ret = rt.vars().size();
		}
		else {
			RCNodeInternalBinaryCache rtibc = (RCNodeInternalBinaryCache)rt;

			Collection cutset = rtibc.getCutset( new HashSet());
			Collection context = rtibc.cache().getContext( null);
			Collection cluster = new HashSet( cutset.size() + context.size());
			cluster.addAll( cutset);
			cluster.addAll( context);
			ret = cluster.size();

			ret = Math.max( ret, clusterSize( rtibc.left()));
			ret = Math.max( ret, clusterSize( rtibc.right()));
		}
		return ret;
	}

	/** Compute ln(a-b) from ln(a) and ln(b) using the log sub equation.
	  * Its possible that lna and/or lnb might be NaN, or pos/neg infin.
	  * If lna or lnb are NaN, the result is NaN.
	  * If lna=lnb=negInf, then (a=b=0 and ln(0)=negInf) return negInf.
	  * If lna=negInf or lnb=negInf return other one (possibly negated).
	  * If lna or lnb are posInf, then it is an error.
	  */
	final static public double logsub( double lna, double lnb) {
		double ret;

		//NaN will propagate by itself

		//Handle infinite values
		if( lna == Double.NEGATIVE_INFINITY && lnb == Double.NEGATIVE_INFINITY) { return Double.NEGATIVE_INFINITY;} //A=0 && B=0
		else if( lna == Double.NEGATIVE_INFINITY) { return -lnb;} //A=0
		else if( lnb == Double.NEGATIVE_INFINITY) { return lna;} //B=0
		else if( lna == Double.POSITIVE_INFINITY || lnb == Double.POSITIVE_INFINITY) { throw new IllegalStateException("Positive Infinity");}

		//use log sub equation
		// ln(a-b) = ln(a) + ln(1.0 - e ^ (ln(b)-ln(a)))
		ret = lna + Math.log( 1.0 - Math.exp( (lnb-lna)));

		//if lnb-lna is (really) large, then e^(lnb-lna) might=posInf but a better value is -lnB or lnA
		if( ret == Double.POSITIVE_INFINITY) { if(lna>lnb) {ret = lna;} else { ret = -lnb;}}

		//return result
		return ret;
	}

	/** Compute ln(a-b) from ln(a) and ln(b) using the log sub equation.
	  * Its possible that lna and/or lnb might be NaN, or pos/neg infin.
	  * If lna or lnb are NaN, the result is NaN.
	  * If lna=lnb=negInf, then (a=b=0 and ln(0)=negInf) return negInf.
	  * If lna=negInf or lnb=negInf return other one (possibly negated).
	  * If lna or lnb are posInf, then it is an error.
	  */
	final static public double logsub2( double lna, double lnb) {
		double ret;

		//NaN will propagate by itself

		//Handle infinite values
		if( lna == Double.NEGATIVE_INFINITY && lnb == Double.NEGATIVE_INFINITY) { return Double.NEGATIVE_INFINITY;} //A=0 && B=0
		else if( lna == Double.NEGATIVE_INFINITY) { return -lnb;} //A=0
		else if( lnb == Double.NEGATIVE_INFINITY) { return lna;} //B=0
		else if( lna == Double.POSITIVE_INFINITY || lnb == Double.POSITIVE_INFINITY) { throw new IllegalStateException("Positive Infinity");}

		//use log sub equation
		// ln(a-b) = ln(b) + ln(e ^ (ln(a)-ln(b)) - 1.0)
		ret = lnb + Math.log( Math.exp((lna-lnb)) - 1.0);

		//if lnb-lna is (really) large, then e^(lnb-lna) might=posInf but a better value is -lnB or lnA
		if( ret == Double.POSITIVE_INFINITY) { if(lna>lnb) {ret = lna;} else { ret = -lnb;}}

		//return result
		return ret;
	}

}//end class RCUtilities
