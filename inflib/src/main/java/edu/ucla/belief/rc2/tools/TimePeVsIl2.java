package edu.ucla.belief.rc2.tools;

import java.util.*;
import java.io.*;

import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.io.NetworkIO;
//{superfluous} import edu.ucla.belief.EliminationOrders;
import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.CPTShell;

import edu.ucla.belief.rc2.structure.RC2;
//{superfluous} import edu.ucla.belief.rc2.structure.RC2Utils;
import edu.ucla.belief.rc2.creation.RC2CreatorFile;
import edu.ucla.belief.rc2.caching.*;
import edu.ucla.belief.rc2.io.*;

import il2.bridge.*;
import il2.model.BayesianNetwork;
import il2.inf.rc.*;

/** This class should be removed TODO!
 */

public class TimePeVsIl2 {

	/* Argument0: bn file
	   Argument1: il2 or rc2-all or rc2-skip or createEvid
	   Argument2: file name for dt
	   Argument3: file name for evid
	 */

	/* Examples:
	   TimePeVsIl2 "bn.net" "createEvid" "notUsed.txt" "evid.txt"
	   TimePeVsIl2 "bn.net" "il2" "rc_dt.txt" "evid.txt"  //always must do il2 1st as it creates rc file
	   TimePeVsIl2 "bn.net" "rc2" "rc_dt.txt" "evid.txt"  //always must do rc2 2nd as it requires rc file
	 */

    public static void main(String args[]) {
		if(args.length != 4) { System.err.println("Invalid command line: must have 4 arguments");  return;}

		try {

			BeliefNetwork bn1 = NetworkIO.read(args[0]);
			Map evid = new HashMap(1);

			if(args[1].equalsIgnoreCase("createEvid")) {//createEvid

				if(bn1.size()==0) { return;}

				//find a leaf variable
				FiniteVariable fv = null;
				for(Iterator itr = bn1.iterator(); itr.hasNext();) {
					Object v = itr.next();
					if(bn1.outDegree(v) == 0) {
						fv = (FiniteVariable)v;
						break;
					}
				}
				//pick a state with a non-0 probability
				Object state = null;
				{
					CPTShell sh = fv.getCPTShell();
					for(int i=0; i<fv.size(); i++) {
						if(sh.getCP(i) != 0) {
							state = fv.instance(i);
							break;
						}
					}
				}
				//write to file
				{
					PrintWriter out = new PrintWriter(new FileWriter(args[3]), true);
					out.println(fv.getID());
					out.println(state.toString());
					out.flush();
					out.close();
				}
			}
			else if(args[1].equalsIgnoreCase("il2")){//il2

				//load evidence file
				{
					BufferedReader in = new BufferedReader(new FileReader(args[3]));
					String var = in.readLine();
					String val = in.readLine();
					evid.put(bn1.forID(var),val);
				}

				//create structure & do calculations
				Converter cnv = new Converter();
				BayesianNetwork bn2 = cnv.convert(bn1);

				long ct1 = System.currentTimeMillis();
				RCEngine rcil2 = RCEngine.create( bn2, 1, (Random)null );

				rcil2.setEvidence( cnv.convert(evid));

				CachingScheme cs = new CachingScheme(rcil2.dgraph(), true); //to this so only doing p(e)
				cs.fullCaching();
				rcil2.rcCore().allocateCaches(cs.cachedNodes());
				//DEBUG
					System.out.println("rcCalls_FC:" + cs.recursiveCalls());
					System.out.println("cache_FC:" + cs.allocatedCacheEntries());
				//END DEBUG

				long ct2 = System.currentTimeMillis();
				double pe = rcil2.prEvidence();
				long ct3 = System.currentTimeMillis();
				double t1a = rcil2.getCompilationTime();
				double t2a = rcil2.getPropagationTime();
				long t1b = ct2 - ct1;
				long t2b = ct3 - ct2;

				System.out.println("compile time: " + t1a + " or " + t1b);
				System.out.println("prop time: " + t2a + " or " + t2b);
				System.out.println("p(e) = " + pe);

				//write rc file
				rcil2.dgraph().write( new PrintWriter(new FileWriter(args[2]),true), cs.cachedNodes(), cnv);
			}
			else if(args[1].equalsIgnoreCase("rc2-all") || args[1].equalsIgnoreCase("rc2-skip")){//rc2

				//load evidence file
				{
					BufferedReader in = new BufferedReader(new FileReader(args[3]));
					String var = in.readLine();
					String val = in.readLine();
					evid.put(bn1.forID(var),val);

					bn1.getEvidenceController().observe(evid);
				}

				//create structure & do calculations
				RC2.RCCreationParams rcParam = new RC2.RCCreationParams(bn1);
				rcParam.allowKBinst = false;;
				rcParam.allowKBsat = 0;
				//determines which iterator to use
				if(args[1].equalsIgnoreCase("rc2-all")) {
					rcParam.allowPartialDerivatives = true;  //must iterate over all
				}
				else {
					rcParam.allowPartialDerivatives = false; //allow skipping of instances
				}

				RC2CreatorFile.Params fileParam = new RC2CreatorFile.Params(bn1, args[2]);
				RC2 rc2 = RC2CreatorFile.readRC(rcParam, fileParam);

				rc2.synchEvidWithBN();

				double pe = rc2.compute_Pe();
				double t2 = rc2.getComputationTotalTime_ms();

				System.out.println("prop time: " + t2);
				System.out.println("p(e) = " + pe);

				//DEBUG
					System.out.println(rc2.compStats().toString());
					System.out.println("" + rc2.rcCallsCounter + " RC Calls");
					RC2WriteToFile.writeRC(rc2, new PrintWriter(new FileWriter(args[2]+"."+args[1]+".txt"),true));
				//END DEBUG
			}


		}
		catch(Exception e) {
			System.err.println("CmdLnDummy threw Exception: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}
}


