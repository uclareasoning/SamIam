package edu.ucla.belief.rc2.creation;

import java.util.*;
import java.io.*;

import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.FiniteVariable;

import edu.ucla.belief.rc2.structure.*;
import edu.ucla.belief.rc2.caching.RC2CachingScheme_Collection;
import edu.ucla.belief.rc2.caching.RC2CachingScheme_None;


/** This class generates RC2 objects for a BeliefNetwork
 *  from a file.
 */

final public class RC2CreatorFile extends RC2Creator {

	private RC2CreatorFile(){}


	final static public RC2 readRC(RC2.RCCreationParams rcParam, Params fileParam) {
		RC2 rc = new RC2(rcParam);

		if( !readRCStructure(rc, fileParam, null)) { return null;}

		return rc;
	}

	/**Reads in the RC, but sets up no Caching and fills cachedNodes (if null, it attempts to setup caching).*/
	final static public RC2 readRC(RC2.RCCreationParams rcParam, Params fileParam, Collection cachedNodes) {
		RC2 rc = new RC2(rcParam);

		if( !readRCStructure(rc, fileParam, cachedNodes)) { return null;}

		return rc;
	}

	public final static class Params extends RC2Creator.Params {
		String fileName;

		public Params(BeliefNetwork bn, String fileName) {
			super(bn);
			this.fileName = fileName;
		}
	}


	///////////////////////////////
	//  Structure Creation Functions
	///////////////////////////////

//TODO: Currently doesn't support finding roots for partial derivatives
//TODO: Currently doesn't find a good PeNd root node

	static final private boolean readRCStructure(RC2 rc, Params fileParam, Collection cachedNodesParam) {

		Map minVals = new HashMap();
		ArrayList file = new ArrayList();
		int nextNodeID = 0;

		try{
			BufferedReader in = new BufferedReader(new FileReader( fileParam.fileName));

			while(in.ready()) {
				String ln = in.readLine().trim();
				if(ln.length() != 0 && !ln.startsWith("#")) {
					file.add(ln);
				}
			}

			in.close();
		}
		catch(FileNotFoundException e) {
			System.err.println("ERROR: Could not open file " + fileParam.fileName);
			return false;
		}
		catch(IOException e) {
			System.err.println("ERROR: Could not read file: " + e.getMessage());
			return false;
		}


		//create nodes
		Collection roots = new HashSet();
		Map intToNode = new HashMap(file.size()); //ints are those in file, but created nodes may not match
		Collection cachedNodes = new HashSet();

		//leafs have 4 tokens internal nodes have 5 tokens
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
							if(currTok >= tokenBuff.length) { System.err.println("ERROR: Could not parse line: " + ln); return false;}
							tokenBuff[currTok].append(c);
						}
					}
					else {
						if(Character.isWhitespace(c)) { lastWasWhiteSpace = true;} //found end of token
						else { tokenBuff[currTok].append(c);}//continue with currToken
					}
				}
				numTokens = currTok+1;
			}

			if(numTokens<0) { continue;} //line could be only white space which wasn't trimmed

			//copy tokenBuff into token
			for(int j=0; j<token.length; j++) { token[j] = tokenBuff[j].toString();}

			//create node
			if(numTokens == 4) { //leaf node
				if(!(token[0].equalsIgnoreCase("ROOT") || token[0].equalsIgnoreCase("L"))) { throw new IllegalStateException("File Format Error: " + ln);}

				Integer num = new Integer(token[1]);
				RC2Node nd;

				if(token[3].equalsIgnoreCase("cpt")) {
					nd = new RC2NodeLeaf(nextNodeID++, rc, (FiniteVariable)fileParam.bn.forID(token[2]), minVals);
					intToNode.put(num,nd);
				}
				else if(token[3].equalsIgnoreCase("ind")) {
					nd = new RC2NodeLeafEvidInd(nextNodeID++, rc, (FiniteVariable)fileParam.bn.forID(token[2]));
					intToNode.put(num,nd);
				}
				else {
					throw new IllegalStateException("File Format Error: " + ln);
				}
				if(token[0].equalsIgnoreCase("ROOT")) {roots.add(nd);}
			}
			else if(numTokens == 5) { //internal node
				if(!(token[0].equalsIgnoreCase("ROOT") || token[0].equalsIgnoreCase("I"))) { throw new IllegalStateException("File Format Error: " + ln);}

				Integer num = new Integer(token[1]);
				Integer lf = new Integer(token[3]);
				Integer rt = new Integer(token[4]);
				RC2NodeInternal nd = new RC2NodeInternal(nextNodeID++, rc, (RC2Node)intToNode.get(lf), (RC2Node)intToNode.get(rt), minVals);
				intToNode.put(num,nd);
				if(token[0].equalsIgnoreCase("ROOT")) {roots.add(nd);}
				if(token[2].equalsIgnoreCase("cachetrue")) {cachedNodes.add(nd);}
			}
			else {
				System.err.println("ERROR: Could not parse line: " + ln);
				return false;
			}
		}//end for each line


		//initialize roots
		for(Iterator itr_rt = roots.iterator(); itr_rt.hasNext();) {
			RC2Node rt = (RC2Node)itr_rt.next();
			rt.initialize(Collections.EMPTY_SET);
		}

		if(roots.size()==0) {
			System.err.println("ERROR: Did not read any root nodes from file");
			return false;
		}


		RC2.CachingScheme cs;
		if(cachedNodesParam==null) {
			cs = new RC2CachingScheme_Collection("From File", cachedNodes);
		}
		else {
			cs = new RC2CachingScheme_None();
			cachedNodesParam.addAll(cachedNodes);
		}

		//call setRoots on RC
		rc.setRoots((RC2Node)roots.iterator().next(), (RC2Node[])roots.toArray(new RC2Node[roots.size()]),
						null, cs);

		return true;
	}

}//end class RC2CreatorFile


