package edu.ucla.belief.recursiveconditioning;

import java.util.*;
import java.math.*;

import java.text.*;
import java.io.*;

import edu.ucla.util.*;
import edu.ucla.structure.*;
import edu.ucla.belief.*;




/** This class contains utility functions for input and output of RC objects.
 *
 * @author David Allen
 */
final public class RCUtilitiesIO {

	private RCUtilitiesIO() {} //don't allow creation of objects



	final static public void writeToVCGFileBasic( String outFileName, RC rc) {
        try {
            FileWriter fw = new FileWriter( outFileName);
            writeToVCGFileBasic( fw, rc);
            fw.flush();
            fw.close();
        }
        catch( IOException e) {
        }
	}

	final static public void writeToVCGFileBasic( Writer out, RC rc)
		throws IOException {


		if( rc instanceof RCDtree) {
	        out.write( "\ngraph: { title: \"rc\"\nlayoutalgorithm: tree\ntreefactor: 0.9\n");
		}
		else if( rc instanceof RCDgraph) {
			out.write( "\ngraph: { title: \"rc\"\nlayoutalgorithm: mindepthslow\ntreefactor: 0.9\n");
		}
		else {
			throw new IllegalStateException("Unsupported class in writeToVCGFileBasic: " + rc.getClass());
		}

        Map nodeToInt = new HashMap();
        int[] t = {1};

        RCIterator itr = rc.getIterator();
        while( itr.hasNext()) {
			writeToVCGFileNodeBasic( out, itr.nextNode(), nodeToInt, t);
        }
        itr.restart();
        while( itr.hasNext()) {
			writeToVCGFileEdge( out, itr.nextNode(), nodeToInt);
        }
        out.write( "} \n");

	}


    final static public void writeToVCGFileDetailed( String outFileName, RC rc) {
        try {
            FileWriter fw = new FileWriter( outFileName);
            writeToVCGFileDetailed( fw, rc);
            fw.flush();
            fw.close();
        }
        catch( IOException e) {
        }
    }

    final static public void writeToVCGFileDetailed( Writer out, RC rc)
        throws IOException {

		if( rc instanceof RCDtree) {
	        out.write( "\ngraph: { title: \"rc\"\nlayoutalgorithm: tree\ntreefactor: 0.9\n");
		}
		else if( rc instanceof RCDgraph) {
			out.write( "\ngraph: { title: \"rc\"\nlayoutalgorithm: mindepthslow\ntreefactor: 0.9\n");
		}
		else {
			throw new IllegalStateException("Unsupported class in writeToVCGFileBasic: " + rc.getClass());
		}

        Map nodeToInt = new HashMap();
        int[] t = {1};

        RCIterator itr = rc.getIterator();
        while( itr.hasNext()) {
			writeToVCGFileNodeDetailed( out, itr.nextNode(), nodeToInt, t);
        }
        itr.restart();
        while( itr.hasNext()) {
			writeToVCGFileEdge( out, itr.nextNode(), nodeToInt);
        }
        out.write( "} \n");
    }



	final static private void writeToVCGFileNodeBasic( Writer out, RCNode nd, Map nodeToInt, int next[])
		throws IOException {

        Assert.notNull( nodeToInt, "RCNode: nodeToInt cannot be null");
        Assert.notNull( next, "RCNode: next cannot be null");
        Assert.condition( next.length==1, "RCNode: next.length must be 1");

        Integer i = new Integer( next[0]);
        nodeToInt.put( nd, i);
        next[0] = next[0] + 1;

        out.write( "node: { title: \"" + i + "\"" + "\n");


        if( !nd.isLeaf()) {
            out.write( " label: \"" +
                       "cutset: " + nd.cutsetInstantiations + "\n" +
                       "context: " + nd.contextInstantiations + "\n" +
                       "cf: " + nd.getCacheFactor() + "\"\n");
            if( nd.getCacheFactor() > .99) {
                out.write( "color: darkgreen \n");
            }
            else if( nd.getCacheFactor() > .01) {
                out.write( "color: green \n");
            }
            else {
            }
            out.write( "shape: ellipse}" + "\n");
        }
        else {
            out.write( " label: \"" +
                       "context: " + nd.contextInstantiations + "\n" +
                       "var: " + ((RCNodeLeaf)nd).getLeafVar() + "\n" +
//                     "vars: " + ((RCNodeLeaf)this).lkup.getLeafVars( null) + "\n" +
                       "leaf\"\n");
            out.write( "shape: box}" + "\n");
        }




	}
	final static private void writeToVCGFileNodeDetailed( Writer out, RCNode nd, Map nodeToInt, int next[])
		throws IOException {

        Assert.notNull( nodeToInt, "RCNode: nodeToInt cannot be null");
        Assert.notNull( next, "RCNode: next cannot be null");
        Assert.condition( next.length==1, "RCNode: next.length must be 1");

        Integer i = new Integer( next[0]);
        nodeToInt.put( nd, i);
        next[0] = next[0] + 1;

        out.write( "node: { title: \"" + i + "\"" + "\n");


		if( nd.isLeaf()) {
            out.write( " label: \"" +
                       "context: " + nd.contextInstantiations + "\n" +
                       "var: " + ((RCNodeLeaf)nd).getLeafVar() + "\n" +
                       "vars: " + ((RCNodeLeaf)nd).lkup.getLeafVars( null) + "\n" +
                       "leaf\"\n");
            out.write( "shape: box}" + "\n");
		}
		else {
			RCNodeInternalBinaryCache ndibc = (RCNodeInternalBinaryCache)nd;

            out.write( " label: \"" +
                       "cutset: " + ndibc.cutsetInstantiations + " " + ndibc.itr.getVars( null)  + "\n" +
                       "context: " + ndibc.contextInstantiations + " " + ndibc.cache.getContext( null) + "\n" +
                       "cf: " + ndibc.getCacheFactor() + "\"\n");
            if( ndibc.getCacheFactor() > .99) {
                out.write( "color: darkgreen \n");
            }
            else if( ndibc.getCacheFactor() > .01) {
                out.write( "color: green \n");
            }
            else {
            }
            out.write( "shape: ellipse}" + "\n");

		}
	}
	final static private void writeToVCGFileEdge( Writer out, RCNode nd, Map nodeToInt)
		throws IOException {

		if( nd.isLeaf()) {
	        //don't do anything
		}
		else {
			RCNodeInternalBinaryCache ndibc = (RCNodeInternalBinaryCache)nd;
			out.write( "edge: {" + "\n");
			out.write( "sourcename: \"" + nodeToInt.get(ndibc) + "\"" + "\n");
			out.write( "targetname: \"" + nodeToInt.get(ndibc.left) + "\"" + "\n");
			out.write( "}" + "\n");

			out.write( "edge: {" + "\n");
			out.write( "sourcename: \"" + nodeToInt.get(ndibc) + "\"" + "\n");
			out.write( "targetname: \"" + nodeToInt.get(ndibc.right) + "\"" + "\n");
			out.write( "}" + "\n");
		}
	}//end writeToVCGFileEdge


	final static public void writeRCToFile( PrintWriter stream, RC rc) {
		MappedList nodes = new MappedList(rc.statsAll().numNodes());
		{
			RCIterator itr_nd = rc.getIteratorParentChild();
			while(itr_nd.hasNext()) {
				RCNode nd = itr_nd.nextNode();
				nodes.add(nd);
			}
		}
		{//write stream header info
			RC.RCStats stats = rc.statsAll();
			stream.println("# " + new Date());
			stream.println("# allocated cache entries " + stats.numCacheEntries());
			stream.println("# recursive calls " + stats.expectedNumberOfRCCalls());
			stream.println("# number of dtree nodes " + stats.numNodes());
			stream.println("");
		}
		{//write stream nodes
			RCIterator itr_nd = rc.getIteratorParentChild();
			while(itr_nd.hasNext()) {
				RCNode nd = itr_nd.nextNode();

				if(nd instanceof RCNodeInternalBinaryCache) {
					RCNodeInternalBinaryCache ndb = (RCNodeInternalBinaryCache)nd;
					stream.println((nd.isRoot?"ROOT ":"I ") + nodes.indexOf(nd) + " " + (nd.getCacheFactor()==0?"cachefalse ":"cachetrue ") + nodes.indexOf(ndb.left) + " " + nodes.indexOf(ndb.right));
				}
				else {
					RCNodeLeaf ndl = (RCNodeLeaf)nd;
					stream.println((nd.isRoot?"ROOT ":"L ") + nodes.indexOf(nd) + " " + ndl.getLeafVar().getID() + " cpt");
				}
			}
		}
	}

	final static public RC readRCFromFile( BufferedReader in, BeliefNetwork bn, double scalar) {
		ArrayList file = new ArrayList();
		int numRoots = 0;

		try {
			while(in.ready()) {
				String ln = in.readLine().trim();
				if(ln.length() != 0 && !ln.startsWith("#")) {
					file.add(ln);
					if(ln.startsWith("ROOT")) { numRoots++;}
				}
			}
		}
		catch(IOException e) {
			System.err.println("ERROR: Could not read file: " + e.getMessage());
			return null;
		}

		//create rc object
		RC rc;
		{
			RC.RCCreationParams rcpar = new RC.RCCreationParams();
			rcpar.scalar = scalar;
			rcpar.useKB = false;
			rcpar.allowKB = false;
			rcpar.bn = bn;

			if(numRoots > 1) {
				rc = new RCDgraph(rcpar);
			}
			else {
				rc = new RCDtree(rcpar);
			}
		}

		//create nodes
		Collection roots = new HashSet(numRoots);
		Map intToNode = new HashMap(file.size());
		Collection cachedNodes = new HashSet();

		//leafs have 4 tokens, internal nodes have 5 tokens
		StringBuffer tokenBuff[] = {new StringBuffer(4), new StringBuffer(10), new StringBuffer(), new StringBuffer(10), new StringBuffer(10)};
		String token[] = new String[tokenBuff.length];

		for( int i=file.size()-1; i>=0; i--) {
			String ln = (String)file.get(i);

			//parse line
			int numTokens;
			{
				for(int j=0; j<tokenBuff.length; j++) {tokenBuff[j].setLength(0);}

				int currTok = -1;
				boolean lastWasWhiteSpace = true;

				for(int indx=0; indx<ln.length(); indx++) {
					char c = ln.charAt(indx);

					if(lastWasWhiteSpace) {
						if(Character.isWhitespace(c)) { continue;} //skip ws
						else { //found start of new token
							lastWasWhiteSpace = false;
							currTok++;
							if(currTok >= tokenBuff.length) { System.err.println("ERROR1: Could not parse line: " + ln + "(" + currTok + ")("+c+")"); return null;}
							tokenBuff[currTok].append(c);
						}
					}
					else {
						if(Character.isWhitespace(c)) {
							lastWasWhiteSpace = true;
						} //found end of token
						else {
							tokenBuff[currTok].append(c);
						}//continue with currToken
					}
				}
				numTokens = currTok+1;
			}

			if(numTokens<0) { continue;} //line could be only white space which wasn't trimmed

			//copy tokenBuff into token
			for(int j=0; j<token.length; j++) { token[j] = tokenBuff[j].toString();}

			//create node
			if(numTokens == 4) { //leaf node
				Integer num = new Integer(token[1]);
				RCNodeLeaf nd = new RCNodeLeaf(rc, (FiniteVariable)bn.forID(token[2]));
				intToNode.put(num,nd);
				if(token[0].equalsIgnoreCase("ROOT")) {roots.add(nd);}
			}
			else if(numTokens == 5) { //internal node
				Integer num = new Integer(token[1]);
				Integer lf = new Integer(token[3]);
				Integer rt = new Integer(token[4]);
				RCNodeInternalBinaryCache nd = new RCNodeInternalBinaryCache(rc, (RCNode)intToNode.get(lf), (RCNode)intToNode.get(rt));
				intToNode.put(num,nd);
				if(token[0].equalsIgnoreCase("ROOT")) {roots.add(nd);}
				if(token[2].equalsIgnoreCase("cachetrue")) {cachedNodes.add(nd);}
			}
			else {
				System.err.println("ERROR2: Could not parse line: " + ln + "(" + numTokens + ")");
				return null;
			}
		}//end for each line

		//initialize
		if(rc instanceof RCDgraph) {

			Map varToRoot = new HashMap(bn.size());

			//create varToRoot
			if(roots.size()==bn.size()) { //non-reduced
				for(Iterator itr = roots.iterator(); itr.hasNext();) {
					RCNodeInternalBinaryCache rt = (RCNodeInternalBinaryCache)itr.next();
					if(rt.left instanceof RCNodeLeaf) {
						varToRoot.put(((RCNodeLeaf)rt.left).getLeafVar(), rt);
					}
					if(rt.right instanceof RCNodeLeaf) {
						varToRoot.put(((RCNodeLeaf)rt.right).getLeafVar(), rt);
					}
				}
			}
			else {//reduced
				Collection varsLeft = new HashSet(bn);
				for(Iterator itr = roots.iterator(); itr.hasNext();) {
					RCNodeInternalBinaryCache rt = (RCNodeInternalBinaryCache)itr.next();
					if(rt.left instanceof RCNodeLeaf) {
						Collection vars = rt.left.vars(); //vars in cutset (or singletons)
						vars.retainAll(varsLeft); //keep vars still need
						for(Iterator itr2 = vars.iterator(); itr2.hasNext();) {
							Object fv = itr2.next();
							varToRoot.put(fv, rt);
							varsLeft.remove(fv);
						}
					}
					if(rt.right instanceof RCNodeLeaf) {
						Collection vars = rt.right.vars(); //vars in cutset (or singletons)
						vars.retainAll(varsLeft); //keep vars still need
						for(Iterator itr2 = vars.iterator(); itr2.hasNext();) {
							Object fv = itr2.next();
							varToRoot.put(fv, rt);
							varsLeft.remove(fv);
						}
					}
				}
			}


			RCDgraph rcdg = (RCDgraph)rc;
			rcdg.setRoots(roots, true, varToRoot, (roots.size()==bn.size()));
		}
		else {
			if(roots.size()!=1) { System.err.println("ERROR: Dtree had " + roots.size() + " roots"); return null;}
			RCDtree rcdt = (RCDtree)rc;
			rcdt.setRoot((RCNode)roots.iterator().next(), true);
		}

		//set caching on nodes
		for(Iterator itr_nd = intToNode.values().iterator(); itr_nd.hasNext();) {
			RCNode nd = (RCNode)itr_nd.next();
			if(cachedNodes.contains(nd)) {
				nd.changeCacheFactor(1);
			}
			else if(!nd.isLeaf()) {
				nd.changeCacheFactor(0);
			}
		}

		return rc;
	}
}//end class RCUtilities
