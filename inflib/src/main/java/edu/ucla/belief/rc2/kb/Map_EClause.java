package edu.ucla.belief.rc2.kb;

import edu.ucla.belief.FiniteVariable;


import java.util.*;
import java.io.*;

/**
 *  This class creates a mapping using the new eclause (which handles the multivalued variables).
 *
 *  @author David Allen
 */
public class Map_EClause extends KBMap {

	private Map_EClause(){}

	/**This creates a cnf file and a Mapping which allows Variables and states from a
	 *  Bayesian network to be set on the cnf.
	 */
	public static Mapping createLogicEncoding(MultiValuedCNF mvcnf, File cnfFile, String title, Map fvToColOfClauses, boolean includeComments) {
		if(!mvcnf.usingEClauses()) throw new IllegalStateException("MultiValuedCNF is not using EClauses."); //TODO possibly adjust this, as this would only be true if it was augmented for compilation
		try {
			PrintWriter outFile = new PrintWriter(new BufferedWriter(new FileWriter(cnfFile)));
			Mapping ret = createLogicEncoding(mvcnf, cnfFile, outFile, title, fvToColOfClauses, includeComments);
			if(outFile != null) {outFile.flush();outFile.close();}
			return ret;
		}
		catch(Exception e) {
			System.err.println("Exception during createLogicEncoding: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}


	final private static Mapping createLogicEncoding(MultiValuedCNF mvcnf, File cnfFile, PrintWriter outFile, String title, Map fvToColOfClauses, boolean includeComments) throws IOException {
		if(!mvcnf.usingEClauses()) throw new IllegalStateException("MultiValuedCNF is not using EClauses."); //TODO possibly adjust this, as this would only be true if it was augmented for compilation

		List indexingList = mvcnf.indexingList();
		int map[][] = new int[indexingList.size()][];

		//create mapping & calculate num_vars & num_clauses
		int nextBoolVar = 1;	//index of next boolean variable
		int num_vars = 0;		//total number of boolean variables
		int num_clauses = 0;	//total number of clauses to create (does not include eclauses)
		int num_eclauses = 0;   //total number of eclauses to create

		for(int i=0; i<map.length; i++) {
			FiniteVariable var = (FiniteVariable)indexingList.get(i);
			if(mvcnf.contains(var)) {
				int v_numStates = var.size();

				map[i] = new int[v_numStates];

				if(v_numStates!=2) {
					for(int z=0; z<v_numStates; z++) {map[i][z] = nextBoolVar+z;}

					nextBoolVar += v_numStates;
					num_vars += v_numStates; //one var per state

					num_eclauses++; //one eclause
				}
				else {//for binary variables, don't create extra states
					map[i] = new int[2];
					map[i][0] = nextBoolVar;
					map[i][1] = -nextBoolVar;
					nextBoolVar++;
					num_vars++;
					//no clauses for binary variables
				}
			}//end if var is in MVCNF
		}//end for each var

		//extra eclauses
		num_eclauses += mvcnf.eclauses().size();

		//for each multi-valued clause we need a boolean clause
		num_clauses += mvcnf.clausesIndex.length;


		//create cnf file
		{
			if(includeComments) outFile.println("c This cnf file was used to map a multivalued BN to a cnf (using EClauses)");
			if(includeComments) outFile.println("c Title: " + title);
			outFile.println("p cnf " + num_vars + " " + (num_clauses+num_eclauses));
			outFile.println("eclauses " + num_eclauses);

			if(includeComments) {
				outFile.println("c");
				outFile.println("c Variable States:");
				for(int i=0; i<map.length; i++) {
					FiniteVariable var = (FiniteVariable)indexingList.get(i);
					if(mvcnf.contains(var)) {
						for(int j=0; j<var.size(); j++) {
							outFile.println("c " + (map[i][j]) + ":" + var.getID() + "=" + var.instance(j));
						}
					}
				}
				outFile.println("c");
			}

			//as long as clause index in new cnf is same as mvcnf, can copy its mapping
			if(fvToColOfClauses==null) fvToColOfClauses = new HashMap();
			{//pseudo-deep clone
				Map mvMap = mvcnf.fvToColOfClauses();
				HashSet oldColToNewCol[][] = new HashSet[mvMap.size()][2]; //use this to catch variables mapped to same collection

				for(Iterator itr=mvMap.keySet().iterator(); itr.hasNext();) {
					Object var = itr.next();
					HashSet old_obj = (HashSet)mvMap.get(var);
					HashSet new_obj = null;

					for(int i=0; i<oldColToNewCol.length; i++) {
						if(oldColToNewCol[i][0] == null) { //not in list, clone it and add it
							new_obj = (HashSet)old_obj.clone();
							oldColToNewCol[i][0] = old_obj;
							oldColToNewCol[i][1] = new_obj;
							break;
						}
						else if(old_obj == oldColToNewCol[i][0]) { //have previously already cloned this one
							new_obj = oldColToNewCol[i][1];
							break;
						}
					}
					fvToColOfClauses.put(var, new_obj);
				}
			}

			//create clauses
			int numClCreated = 0;
			int numEClCreated = 0;

			//create them for multi-valued clauses
			//Do these first, so that clause indecies match those in the MVCNF
			if(includeComments) outFile.println("c Add multi-value clauses");
			int mvClIndx = 0;
			for(int lit=0; lit<mvcnf.literalsVars.length; lit++) {
				int var = mvcnf.literalsVars[lit];
				int val = mvcnf.literalsStates[lit];
				boolean pos = mvcnf.literalsPositive[lit];

				int mapval = map[var][val]; //this could possibly be positive or negative (in the case of boolean variables)
				if(!pos) {mapval = -mapval;}

				outFile.print(mapval + " ");

				if(mvClIndx+1 < mvcnf.clausesIndex.length) {
					if(lit+1==mvcnf.clausesIndex[mvClIndx+1]) {//doing last literal in this clause
						outFile.println("0");
						numClCreated++;
						mvClIndx++;
					}
				}
				else if(lit == mvcnf.literalsVars.length-1){ //working on last clause, go until end
					outFile.println("0");
					numClCreated++;
					mvClIndx++;
				}
			}//end for each literal

			if(includeComments) outFile.println("c");
			if(includeComments) outFile.println("c Add EClauses for phenotype variable manipulation");

			List eclauses = mvcnf.eclauses();
			List eclausesFV = mvcnf.eclausesFV();

			for(int i=0; i<eclauses.size(); i++) {
				int ec[] = (int[])eclauses.get(i);
				FiniteVariable fv = (FiniteVariable)eclausesFV.get(i);

				//update local fvToColOfClauses
				Collection col = (Collection)fvToColOfClauses.get(fv);
				if(col==null) {
					col = new HashSet();
					fvToColOfClauses.put(fv, col);
				}
				col.add(new Integer(numClCreated+numEClCreated));

				//write out cnf
				for(int z=0; z<ec.length; z++) {

					int varIndx = ec[z];
					int mapval = map[varIndx][0];

					outFile.print(mapval + " ");
				}
				outFile.println("0");
				numEClCreated++;
			}
			if(includeComments) outFile.println("c");

			if(includeComments) outFile.println("c Add EClauses for multivalued variables");
			for(int vi=0; vi<map.length; vi++) {
				if((map[vi] != null) && (map[vi].length != 2)) {

					//update local fvToColOfClauses
					Collection col = (Collection)fvToColOfClauses.get(indexingList.get(vi));
					if(col==null) {
						col = new HashSet();
						fvToColOfClauses.put(indexingList.get(vi), col);
					}
					col.add(new Integer(numClCreated+numEClCreated));


					for(int z=0; z<map[vi].length; z++) {
						outFile.print(map[vi][z] + " ");
					}
					outFile.println("0");
					numEClCreated++;
				}
			}


			if(numClCreated != num_clauses) {
				outFile.println("\n\n\nERROR: Expected to create " + num_clauses + " clauses, but actually created " + numClCreated);
				outFile.flush();
				throw new IllegalStateException("Expected to create " + num_clauses + " clauses, but actually created " + numClCreated);
			}
			if(numEClCreated != num_eclauses) {
				outFile.println("\n\n\nERROR: Expected to create " + num_eclauses + " EClauses, but actually created " + numEClCreated);
				outFile.flush();
				throw new IllegalStateException("Expected to create " + num_eclauses + " EClauses, but actually created " + numEClCreated);
			}
		}//end create cnf file

		return new Mapping(cnfFile.getCanonicalPath(), map, num_vars, (num_clauses+num_eclauses));
	}


} //end class Map_EClause
