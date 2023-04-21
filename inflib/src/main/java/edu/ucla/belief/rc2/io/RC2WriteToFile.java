package edu.ucla.belief.rc2.io;

import java.util.*;
import java.io.*;

//{superfluous} import edu.ucla.belief.BeliefNetwork;
//{superfluous} import edu.ucla.belief.FiniteVariable;

import edu.ucla.belief.rc2.structure.*;


/** This class writes RC2 objects to a file.
 *  <p>To read a file back from disk, see RC2CreatorFile.
 */

final public class RC2WriteToFile {

	private RC2WriteToFile(){}


	final static public void writeRC(RC2 rc, PrintWriter out) {

		{//write out header info
			RC2.StoredComputationStats compStats = rc.compStats();
			out.println("# " + new Date());
			out.println( compStats.fullCaching().toString("# "));
			out.println( compStats.currentCaching().toString("# "));
			out.println("# number of nodes " + rc.getNumRCNodes_All());
			out.println("");
		}
		{
			for(int i=rc.getNumRCNodes_All()-1; i>=0; i--) {
				RC2Node nd = rc.getRCNode_All(i);
				if(nd instanceof RC2NodeLeaf) {
					RC2NodeLeaf ndl = (RC2NodeLeaf)nd;
					out.println((nd.isRoot()?"ROOT ":"L ") + nd.nodeID + " " + ndl.getLeafVar().getID() + " cpt");
				}
				else if(nd instanceof RC2NodeLeafEvidInd) {
					RC2NodeLeafEvidInd ndl = (RC2NodeLeafEvidInd)nd;
					out.println((nd.isRoot()?"ROOT ":"L ") + nd.nodeID + " " + ndl.getLeafVar().getID() + " ind");
				}
				else if(nd instanceof RC2NodeInternal) {
					RC2NodeInternal ndi = (RC2NodeInternal)nd;
					out.println((nd.isRoot()?"ROOT ":"I ") + nd.nodeID + " " + (ndi.actualMemoryAllocated()==0?"cachefalse ":"cachetrue ") + ndi.left.nodeID + " " + ndi.right.nodeID);
				}
				else {
					System.err.println("Illegal Node Type " + nd);
				}
			}
		}
	}



}//end class RC2WriteToFile


