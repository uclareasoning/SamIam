package edu.ucla.belief.rc2.structure;

import java.util.*;
import java.math.BigInteger;

import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.TableIndex;


/** This class represent an Index for RC2.
 *
 * @author David Allen
 */
final public class RC2Index {

	//if totalStateSpace > Long.MAX_VALUE, then throw an exception

	final protected RC2 rc;
	final protected int varIndx[]; //index in rc.vars
	final protected long blockSize[];
	final protected BigInteger totalStateSpace; //total state space of vars, however doesn't necessarily match block sizes if user put special blocksizes in.
	final boolean totalStateSpaceLargerThanInt;
	final public Set vars;
	private TableIndex tblIndx = null;


	/*vars is a Collection of FiniteVariable Objects.*/
	public RC2Index(RC2 rc, Collection vars) {
		this.rc = rc;
		varIndx = new int[vars.size()];
		blockSize = new long[vars.size()];
		this.vars = Collections.unmodifiableSet(new HashSet(vars));

		{//init varIndx & blockSize
			BigInteger totalSizeLocal = BigInteger.ONE;
			Iterator vItr = vars.iterator();
			for( int i=0; i<varIndx.length; i++) {
				FiniteVariable fv = (FiniteVariable)vItr.next();
				varIndx[i] = rc.vars.indexOf(fv);
				blockSize[i] = totalSizeLocal.longValue();
				totalSizeLocal = totalSizeLocal.multiply(BigInteger.valueOf(fv.size()));
			}

			BigInteger maxLong = BigInteger.valueOf(Long.MAX_VALUE);
			BigInteger maxInt = BigInteger.valueOf(Integer.MAX_VALUE);
			if(totalSizeLocal.compareTo(maxLong)>0) {
				throw new IllegalStateException("Context Total state space larger than long: " + totalSizeLocal);
			}
			else if(totalSizeLocal.compareTo(maxInt)>0) { //totalSizeLocal > maxInt
				System.out.println("Total State Space of " + totalSizeLocal + " exceeds the capacity of an integer.");
				totalStateSpaceLargerThanInt = true;
			}
			else {
				totalStateSpaceLargerThanInt = false;
			}

			totalStateSpace = totalSizeLocal;
		}//end init varIndx & blockSize
	}

	/*vars is a Collection of FiniteVariable Objects.*/
	/*Uses blkSz directly, doesn't copy it.*/
	public RC2Index(RC2 rc, List vars, long blkSz[]) {
		this.rc = rc;
		varIndx = new int[vars.size()];
		if(blkSz.length != vars.size()) { throw new IllegalArgumentException("Illegal Block Sizes");}
		this.vars = Collections.unmodifiableSet(new HashSet(vars));

		{//init varIndx
			BigInteger totalSizeLocal = BigInteger.ONE;
			for( int i=0; i<varIndx.length; i++) {
				FiniteVariable fv = (FiniteVariable)vars.get(i);
				varIndx[i] = rc.vars.indexOf(fv);
				totalSizeLocal = totalSizeLocal.multiply(BigInteger.valueOf(fv.size()));
			}

			BigInteger maxLong = BigInteger.valueOf(Long.MAX_VALUE);
			BigInteger maxInt = BigInteger.valueOf(Integer.MAX_VALUE);
			if(totalSizeLocal.compareTo(maxLong)>0) {
				throw new IllegalStateException("Context Total state space larger than long: " + totalSizeLocal);
			}
			else if(totalSizeLocal.compareTo(maxInt)>0) { //totalSizeLocal > maxInt
				System.out.println("Total State Space of " + totalSizeLocal + " exceeds the capacity of an integer.");
				totalStateSpaceLargerThanInt = true;
			}
			else {
				totalStateSpaceLargerThanInt = false;
			}

			totalStateSpace = totalSizeLocal;
			blockSize = blkSz;
		}//end init varIndx
	}

	public BigInteger totalStateSpace() { return totalStateSpace;}
	public int memoryUsage() {
		return (totalStateSpaceLargerThanInt ? 0 : totalStateSpace.intValue());  //if the totalStateSpace is > Integer.MAX_VALUE cannot cache here
	}
	public boolean totalStateSpaceLargerThanInt() { return totalStateSpaceLargerThanInt;}


	//This could possibly throw a NullPointerException
	final public long blockSizeOfVar(int varIndxInRC) {
		int ret = -1;
		for(int i=0; i<varIndx.length; i++) {
			if(varIndxInRC==varIndx[i]) { return blockSize[i];}
		}
		System.err.println("ERROR: blockSizeOfVar == -1 (var not found)");
		return -1;
	}


	final public int numVars() { return varIndx.length;}

	final public boolean containsSameVars(Collection ret) {
		if(ret == null) { return false;}
		if(ret.size() != vars.size()) { return false;}
		if(!vars.containsAll(ret)) { return false;}
		return true;
	}

	final public boolean isSuperSetOf(RC2Index indx) {
		return vars.containsAll(indx.vars);
	}

	final TableIndex getTableIndex() {
		if(tblIndx == null) {
			ArrayList ordVars = new ArrayList(varIndx.length);
			for(int i=0; i<varIndx.length; i++) {
				ordVars.add(rc.vars.get(varIndx[i]));
			}
			tblIndx = new TableIndex(ordVars);
		}
		return tblIndx;
	}

}//end class RC2Index
