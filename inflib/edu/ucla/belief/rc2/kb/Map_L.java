package edu.ucla.belief.rc2.kb;

import edu.ucla.belief.FiniteVariable;


import java.util.*;
import java.io.*;

/**
 *  This class creates a mapping using the L (logarithmic)
 *  mapping for multi-valued variables.
 *
 *  @author David Allen
 */
public class Map_L extends KBMap {

	private Map_L(){}


	/**This creates a cnf file and a Mapping which allows Variables and states from a
	 *  Bayesian network to be set on the cnf.
	 */
	public static Mapping createLogicEncoding(MultiValuedCNF mvcnf, File cnfFile, String title, Map fvToColOfClauses, boolean includeComments) {
		if(mvcnf.usingEClauses()) throw new IllegalStateException("MultiValuedCNF is using EClauses.");
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
		if(mvcnf.usingEClauses()) throw new IllegalStateException("MultiValuedCNF is using EClauses.");

		List indexingList = mvcnf.indexingList();
		int map[][] = new int[indexingList.size()][];

		//create mapping & calculate num_vars & num_clauses
		int nextBoolVar = 1;	//index of next boolean variable
		int num_vars = 0;		//total number of boolean variables
		int num_clauses = 0;	//total number of clauses to create

		int max_num_bool_per_var = 1;

		for(int i=0; i<map.length; i++) {
			FiniteVariable var = (FiniteVariable)indexingList.get(i);
			if(mvcnf.contains(var)) {
				int v_numStates = var.size();

				map[i] = new int[v_numStates];
				if(v_numStates!=2) { //non-binary variable
					for(int z=0; z<v_numStates; z++) {map[i][z] = nextBoolVar+z;}

					nextBoolVar += v_numStates;

					double v_log2 = log2(v_numStates);
					int v_log2_f = (int)Math.floor(v_log2);
					int v_log2_c = (int)Math.ceil(v_log2);

					if(v_log2_c > max_num_bool_per_var) { max_num_bool_per_var = v_log2_c;}

					num_vars += v_numStates; //one var per state
					num_vars += v_log2_c; //one var per binary var (in L mapping)

					int full_lev = (int)Math.pow(2, v_log2_f);

					int all_b; //num which require all bool vars
					int par_b; //num which require 1 less var

					if(full_lev == v_numStates) {
						all_b = full_lev;
						par_b = 0;
					}
					else {
						all_b = 2*(v_numStates - full_lev);
						par_b = v_numStates - all_b;
					}

					//for each clause which uses all boolean vars, it requires num_bool_vars +1
					num_clauses += (all_b * (v_log2_c +1));
					//for each which uses less, they require num_used +1
					num_clauses += (par_b * (v_log2_c));
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


		//for each multi-valued clause we need a boolean clause
		num_clauses += mvcnf.clausesIndex.length;


		//create cnf file
		{
			if(includeComments) outFile.println("c This cnf file was used to map a multivalued cnf to a binary cnf (using L)");
			if(includeComments) outFile.println("c Title: " + title);
			outFile.println("p cnf " + num_vars + " " + num_clauses);

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


			//create them for logarithmic mapping
			if(includeComments) outFile.println("c");
			if(includeComments) outFile.println("c Add logarithmic clauses");
			int buff[] = new int[max_num_bool_per_var];
			for(int i=0; i<map.length; i++) {
				FiniteVariable var = (FiniteVariable)indexingList.get(i);
				if(mvcnf.contains(var) && var.size()!=2) {
					int v_numStates = var.size();

					int oldnumClCreated = numClCreated;

					//continue writing cnf
					double v_log2 = log2(v_numStates);
					int v_log2_c = (int)Math.ceil(v_log2);

					numClCreated += createClausesLog(map[i][0], map[i][v_numStates-1], nextBoolVar, buff, 0, outFile, includeComments);
					nextBoolVar += v_log2_c;

					//update local fvToColOfClauses
					{
						Collection col = (Collection)fvToColOfClauses.get(var);
						if(col==null) {
							col = new HashSet();
							fvToColOfClauses.put(var, col);
						}
						//for each clauses added
						for(int clN=oldnumClCreated; clN<numClCreated; clN++) {
							col.add(new Integer(clN));
						}
					}

				}//end if var is in cnf
			}//end for each variable



			if(numClCreated != num_clauses) {
				outFile.println("\n\n\nERROR: Expected to create " + num_clauses + " clauses, but actually created " + numClCreated);
				outFile.flush();
				throw new IllegalStateException("Expected to create " + num_clauses + " clauses, but actually created " + numClCreated);
			}
		}//end create cnf file

		outFile.flush();
		outFile.close();

		return new Mapping(cnfFile.getCanonicalPath(), map, num_vars, num_clauses);
	}


	static private int createClausesLog(int first, int last, int nextBoolIndx, int buff[], int buff_len, PrintWriter outFile, boolean includeComments) {

		int ret = 0;

		if(first==last) {
			//output: first <-> buff
			//output: (!first OR buff) AND (first OR !buff)
			//buff may be 1 AND 2 AND 3... (not CNF)
			//output: (!first OR buff1) AND (!first OR buff2) ... AND (first OR !buff1 OR !buff2 OR ...)


			if(includeComments) {
				outFile.print("c These represent " + first + " <--> ");
				printIntArr(buff, buff_len, false, outFile);
				outFile.println("");
			}

			for(int i=0; i<buff_len; i++) {
				outFile.println((-first) + " " + buff[i] + " 0");
				ret++;
			}

			outFile.print(first + " ");
			printIntArr(buff, buff_len, true, outFile);
			outFile.println("0");
			ret++;

			return ret;
		}

		int numStatesLeft = last - first;
		int half = numStatesLeft/2; //integer division
		buff[buff_len] = nextBoolIndx;
		ret += createClausesLog(first, first+half, nextBoolIndx+1, buff, buff_len+1, outFile, includeComments);
		buff[buff_len] = -nextBoolIndx;
		ret += createClausesLog(first+half+1, last, nextBoolIndx+1, buff, buff_len+1, outFile, includeComments);
		return ret;
	}

	static private void printIntArr(int buff[], int buff_len, boolean invertSigns, PrintWriter outFile) {
		for(int i=0; i<buff_len; i++) {
			outFile.print((invertSigns ? -buff[i] : buff[i]) + " ");
		}
	}



} //end class Map_L
