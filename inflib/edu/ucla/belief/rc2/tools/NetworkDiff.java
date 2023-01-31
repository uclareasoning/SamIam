package edu.ucla.belief.rc2.tools;

import java.util.*;
import java.io.*;
import java.text.*;

//{superfluous} import edu.ucla.structure.MappedList;

import edu.ucla.belief.io.NetworkIO;
import edu.ucla.belief.*;
import edu.ucla.belief.rc2.structure.*;
import edu.ucla.belief.rc2.caching.*;
import edu.ucla.belief.rc2.creation.*;



public class NetworkDiff {

	static final boolean DEBUG_WANT_NETS_REDUCED = true;
	static final boolean DEBUG_TEST_PR_REDUCED = true;

	static final String programTitle = "NetworkDiff";
	static final String programVersion = "1.0";

	static final double epsilon = .000001;

	File f1;
	File f2;

	public NetworkDiff(File f1, File f2) {
		this.f1 = f1;
		this.f2 = f2;
	}

	public static void main(String args[]) {

		if(args.length!=2) {
			System.out.println("Command Line Error: file1 file2");
			return;
		}
		File f1 = new File(args[0]);
		File f2 = new File(args[1]);

		if(!f1.exists()) {
			System.out.println("Could not find: " + f1);
			return;
		}
		if(!f2.exists()) {
			System.out.println("Could not find: " + f2);
			return;
		}

		NetworkDiff prog = new NetworkDiff(f1, f2);
		prog.start();
	}

	void start() {

		try {

			BeliefNetwork bn1 = NetworkIO.read(f1);
			BeliefNetwork bn2 = NetworkIO.read(f2);

			if(bn1==null) {
				System.err.println("Error reading in network " + f1);
				return;
			}
			if(bn2==null) {
				System.err.println("Error reading in network " + f2);
				return;
			}

			System.out.println("Net1: " + bn1.size() + " variables (" + f1 + ")");
			System.out.println("Net2: " + bn2.size() + " variables (" + f2 + ")");

			TreeSet varNames1 = new TreeSet();
			TreeSet varNames2 = new TreeSet();

			TreeSet varNamesIdentical = new TreeSet();
			TreeSet varNamesSameStructure = new TreeSet();
			TreeSet varNamesDiffStructure = new TreeSet();
			TreeSet varNamesOnly1 = new TreeSet();
			TreeSet varNamesOnly2 = new TreeSet();

			for(Iterator itr = bn1.iterator(); itr.hasNext();) {
				FiniteVariable fv = (FiniteVariable)itr.next();
				varNames1.add(fv.getID());
			}
			for(Iterator itr = bn2.iterator(); itr.hasNext();) {
				FiniteVariable fv = (FiniteVariable)itr.next();
				varNames2.add(fv.getID());
			}

			if(varNames1.size()!=bn1.size()) throw new IllegalStateException();
			if(varNames2.size()!=bn2.size()) throw new IllegalStateException();


			while(varNames1.size()>0 || varNames2.size()>0) {
				Object o1 = (varNames1.isEmpty() ? null : varNames1.first());
				Object o2 = (varNames2.isEmpty() ? null : varNames2.first());

				if(o1!=null && o2!=null && o1.equals(o2)) {
					FiniteVariable fv1 = (FiniteVariable)bn1.forID(o1.toString());
					FiniteVariable fv2 = (FiniteVariable)bn2.forID(o2.toString());

					HashSet vars1 = vars(fv1);
					HashSet vars2 = vars(fv2);

					if(!vars1.equals(vars2)) {
						varNamesDiffStructure.add(o1 + "\t (" + vars1 + ")\t(" + vars2 + ")");
					}
					else {
						boolean samePr = true;

						Table t1 = fv1.getCPTShell().getCPT();
						Table t2 = fv2.getCPTShell().getCPT();

						if(t1.getCPLength()!=t2.getCPLength()) { //some variable has different size
							samePr = false;
						}
						else{
							int mapping[] = new int[t1.index().getNumVariables()];
							for(int i=0; i<mapping.length; i++) {
								mapping[i] = t2.index().variableIndex((FiniteVariable)bn2.forID(t1.index().variable(i).getID()));
							}

							int curr[] = new int[mapping.length];
							for(TableIndex.Iterator itrT = t1.index().iterator(); itrT.hasNext();) {
								int ci = itrT.next();

								for(int i=0; i<curr.length; i++) curr[mapping[i]] = itrT.current()[i];

								if(Math.abs(t1.getCP(ci) - t2.getCP(t2.index().index(curr))) > epsilon) {samePr = false; break;}
							}
						}

						if(samePr) varNamesIdentical.add(o1);
						else varNamesSameStructure.add(o1);
					}

					varNames1.remove(o1);
					varNames2.remove(o2);
				}
				else if(o1!=null && o1.toString().compareTo(o2.toString()) < 0) {
					varNamesOnly1.add(o1 + "\t (" + vars((FiniteVariable)bn1.forID(o1.toString())) + ")");
					varNames1.remove(o1);
				}
				else {
					varNamesOnly2.add(o2 + "\t (" + vars((FiniteVariable)bn2.forID(o2.toString())) + ")");
					varNames2.remove(o2);
				}
			}

			if(!varNamesIdentical.isEmpty()) System.out.println("\n\nIdentical Variables: " + varNamesIdentical.size());
			print(varNamesIdentical);

			if(!varNamesSameStructure.isEmpty()) System.out.println("\n\nSame Structure Diff Pr: " + varNamesSameStructure.size());
			print(varNamesSameStructure);

			if(!varNamesDiffStructure.isEmpty()) System.out.println("\n\nDifferent Structure: " + varNamesDiffStructure.size());
			print(varNamesDiffStructure);

			if(!varNamesOnly1.isEmpty()) System.out.println("\n\nOnly in "+f1+": " + varNamesOnly1.size());
			print(varNamesOnly1);

			if(!varNamesOnly2.isEmpty()) System.out.println("\n\nOnly in "+f2+": " + varNamesOnly2.size());
			print(varNamesOnly2);



			if(DEBUG_WANT_NETS_REDUCED) {
				System.out.println("\n\nCreating Reduced Networks");

				TreeSet iden = new TreeSet(varNamesIdentical);

				boolean done = false;
				while(!done) {
					done = true;

					for(Iterator itrID=iden.iterator(); itrID.hasNext();) {
						String vName = (String)itrID.next();
						FiniteVariable fv1 = (FiniteVariable)bn1.forID(vName);
						FiniteVariable fv2 = (FiniteVariable)bn2.forID(vName);
						if(fv1==null || fv2==null) throw new IllegalStateException();

						if(bn1.outDegree(fv1)==0) { //leaf
							bn1.removeVariable(fv1);
							bn2.removeVariable(fv2);
							done = false;
							itrID.remove();
						}
						else if(bn1.inDegree(fv1)==0) { //root with all children listed as identical
							boolean remove=true;
							for(Iterator itrCh = bn1.outGoing(fv1).iterator(); remove && itrCh.hasNext();) {
								FiniteVariable chi = (FiniteVariable)itrCh.next();
								if(!iden.contains(chi.getID())) remove=false;
							}
							for(Iterator itrCh = bn2.outGoing(fv2).iterator(); remove && itrCh.hasNext();) {
								FiniteVariable chi = (FiniteVariable)itrCh.next();
								if(!iden.contains(chi.getID())) remove=false;
							}

							if(remove) {
								bn1.removeVariable(fv1);
								bn2.removeVariable(fv2);
								done = false;
								itrID.remove();
							}
						}
					}
				}

				if(!iden.isEmpty()) System.out.println("\n\nIdentical but could not remove in reduced Nets:" + iden.size());
				print(iden);

				if(bn1.isEmpty()) System.out.println(f1.getCanonicalPath()+"_reduced.net would be empty, not created.");
				else NetworkIO.writeNetwork(bn1, new File(f1.getCanonicalPath()+"_reduced.net"));

				if(bn2.isEmpty()) System.out.println(f2.getCanonicalPath()+"_reduced.net would be empty, not created.");
				else NetworkIO.writeNetwork(bn2, new File(f2.getCanonicalPath()+"_reduced.net"));


				if(DEBUG_TEST_PR_REDUCED && !bn1.isEmpty() && !bn2.isEmpty()) {
					RC2 rc1 = null;
					RC2 rc2 = null;

					{
						RC2.RCCreationParams param = new RC2.RCCreationParams(bn1);
						Collection cachedNodes = new HashSet();
						RCGenerator.RC rcTmp = RCGenerator.generateRC(bn1,f1.getName(),null, false, null);
						{
							File tmprc = File.createTempFile("rcFile", null, new File("."));
							Writer rcOut = new BufferedWriter(new FileWriter(tmprc));
							rcTmp.writeFile(rcOut);
							rcOut.flush();
							rcOut.close();
							rcOut = null;

							//could cause memory trouble
							rc1 = RC2CreatorFile.readRC(param, new RC2CreatorFile.Params(bn1, tmprc.getPath()), cachedNodes);

							tmprc.delete();
							tmprc = null;
						}
						rc1.setCachingScheme(new RC2CachingScheme_Full());
					}
					{
						RC2.RCCreationParams param = new RC2.RCCreationParams(bn2);
						Collection cachedNodes = new HashSet();
						RCGenerator.RC rcTmp = RCGenerator.generateRC(bn2,f2.getName(),null, false, null);
						{
							File tmprc = File.createTempFile("rcFile", null, new File("."));
							Writer rcOut = new BufferedWriter(new FileWriter(tmprc));
							rcTmp.writeFile(rcOut);
							rcOut.flush();
							rcOut.close();
							rcOut = null;

							//could cause memory trouble
							rc2 = RC2CreatorFile.readRC(param, new RC2CreatorFile.Params(bn2, tmprc.getPath()), cachedNodes);

							tmprc.delete();
							tmprc = null;
						}
						rc2.setCachingScheme(new RC2CachingScheme_Full());
					}

					System.out.println(f1 + "   P(e) = " + rc1.compute_Pe());
					System.out.println(f2 + "   P(e) = " + rc2.compute_Pe());
				}//end DEBUG_TEST_PR_REDUCED
			}//end DEBUG_WANT_NETS_REDUCED
		}
		catch(Exception e) {
			System.err.println(programTitle + " caught the exception:\n" + e.getMessage());
			e.printStackTrace(System.err);
		}
		System.out.println("\n\n" + programTitle + " " + programVersion + " by Allen (2005)\n\n");
	}//end start


	static void print(Collection c) {
		for(Iterator itr = c.iterator(); itr.hasNext();) {
			System.out.println("  " + itr.next());
		}
	}

	static HashSet vars(FiniteVariable fv) {
		List vars = fv.getCPTShell().variables();
		HashSet ret = new HashSet(vars.size());
		for(ListIterator itr=vars.listIterator(); itr.hasNext();) {
			ret.add(((FiniteVariable)itr.next()).getID());
		}
		return ret;
	}
}


