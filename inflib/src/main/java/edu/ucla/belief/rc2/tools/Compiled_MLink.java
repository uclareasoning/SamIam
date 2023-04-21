package edu.ucla.belief.rc2.tools;

import java.util.*;
import java.io.*;
import java.text.*;

import edu.ucla.structure.MappedList;

import edu.ucla.util.JVMProfiler;

//{superfluous} import edu.ucla.belief.BeliefNetwork;
//{superfluous} import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.io.geneticlinkage.*;
import edu.ucla.belief.io.NetworkIO;
import edu.ucla.belief.RCGenerator;

import edu.ucla.belief.rc2.kb.MultiValuedCNF;
import edu.ucla.belief.rc2.kb.Map_L;
import edu.ucla.belief.rc2.kb.Map_EClause;
import edu.ucla.belief.rc2.structure.RC2;
import edu.ucla.belief.rc2.creation.RC2CreatorFile;
import edu.ucla.belief.rc2.io.RC2WriteToFile;


public class Compiled_MLink {

	static final String programTitle = "Logic_Link";
	static final String programVersion = "2.0";

//TODO SET FLAGS (move some to command line)
	boolean generateTestFiles = true;
	boolean createRC = false;
	boolean extendedSearch = false;
	boolean includeComments = false;
	boolean debugOutput = true;
//TODO SET FLAGS

	CmdLn cmdln = null;

	MultipleOutputs stream_All = new MultipleOutputs();
	MultipleOutputs stream_Time = new MultipleOutputs();

	long time_cpu[] = new long[20];
	long time_sys[] = new long[20];



	public Compiled_MLink(CmdLn cmdln) {
		this.cmdln = cmdln;
	}

	public static void main(String args[]) {
		Compiled_MLink prog = new Compiled_MLink(new CmdLn(args));
		prog.start();
	}

	void start() {

		PrintStream outDetails;
		PrintStream outRCInfo;


		try{
			outDetails = new PrintStream(new BufferedOutputStream(new FileOutputStream(cmdln.outDir+File.separator+"detailed.txt")),true);

			stream_All.add(System.out);
			stream_All.add(outDetails);

			if(debugOutput) stream_Time.add(System.out);
			stream_Time.add(outDetails);
			stream_Time.add(new PrintStream(new BufferedOutputStream(new FileOutputStream(cmdln.outDir+File.separator+"times.txt")),true));

			{
				File fileTmp = new File("rcInfo.tsv");
				boolean exists = fileTmp.exists();
				outRCInfo = new PrintStream(new BufferedOutputStream(new FileOutputStream(fileTmp, /*append*/true)),/*autoFlush*/true);
				if(!exists) {
					outRCInfo.println("\t"       +"Full Caching\t\t");
					outRCInfo.println("Network\t"+"MB\t"+"Calls\t"  );
				}
				outRCInfo.print(cmdln.locus.getName());
			}
		}
		catch(IOException e) {
			System.err.println("Unrecoverable Error creating output streams");
			return;
		}



        println(stream_All, "");
        println(stream_All, "----------");
        println(stream_All, "running " + programTitle + " " + programVersion);
        println(stream_All, "" + new Date());
        println(stream_All, "Command-Line: " + cmdln);
        println(stream_All, "----------");


		try {
			Pedigree.GeneticNetwork gn;
			RCGenerator.RC rcBestGen = null;

			markTime(0);
			{
				Loci loci = Loci.readLoci(cmdln.locus.getPath());

				Pedigree ped = Pedigree.createPedigree(cmdln.pedigree.getPath(), loci);

				markTime(1);
				displayTime(0,1,"Pedigree preprocess");

				{
					boolean forceProbIntoRoot = (cmdln.noCNF==false); //if creating CNF, force prob into roots
					gn = ped.createBeliefNetwork(cmdln.scalar);

if(forceProbIntoRoot) throw new UnsupportedOperationException(); //no longer supported
				}

				markTime(2);
				displayTime(1,2,"Convert to Belief Network");

				if(generateTestFiles) {

					NetworkIO.writeNetwork(gn.bn, new File(cmdln.outDir+File.separator+"network.net"));

					if(Pedigree.PREPROCESS!=Pedigree.ALL) {
						PrintStream outEvid = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(cmdln.outDir+File.separator+"X.inst"))),true);
						gn.writeEvidToFile(outEvid);
						outEvid.flush();
						outEvid.close();
						outEvid = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(cmdln.outDir+File.separator+"X_All.inst"))),true);
						gn.writeEvidToFile_All(outEvid);
						outEvid.flush();
						outEvid.close();
					}

					gn.outputChangingVariables(new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(cmdln.outDir+File.separator+"thetas.list"))),true));

					{
						PrintStream outConst = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(cmdln.outDir+File.separator+"constant.txt"))),true);
						outConst.println("Constant (scaled): \t" + gn.constantScaled());
						outConst.println("Scalar: \t" + gn.scalar);
						outConst.flush();
						outConst.close();
					}
				}//end generateTestFiles
				markTime(3);
				displayTime(2,3,"Output Test Files");

				if(createRC) {
					RC2 rcBest = null;
					RC2.RCCreationParams param = new RC2.RCCreationParams(gn.bn);
					{
						param.scalar = gn.scalar;
					}
					Collection cachedNodes = new HashSet();
					rcBestGen = RCGenerator.generateRC(gn.bn,cmdln.locus.getName(),"rcSearch_detailed.tsv", extendedSearch, gn.equivPreProcessVars());
					try{
						File tmprc = File.createTempFile("rcFile", null, new File("."));
						Writer rcOut = new BufferedWriter(new FileWriter(tmprc));
						rcBestGen.writeFile(rcOut);
						rcOut.flush();
						rcOut.close();
						rcOut = null;

						//could cause memory trouble
						rcBest = RC2CreatorFile.readRC(param, new RC2CreatorFile.Params(gn.bn, tmprc.getPath()), cachedNodes);

						tmprc.delete();
						tmprc = null;

						outRCInfo.print("\t"+rcBest.compStats().fullCaching().memUsedMB()+"\t"+rcBest.compStats().fullCaching().rcCalls_pe());
					}
					catch(Exception e) {
						rcBest = null;
						rcBestGen = null;
						println(stream_All, "Error Creating Dtree: " + e.toString());
//						e.printStackTrace();
					}

					if(rcBest!=null) {
						PrintWriter rcOut = new PrintWriter(new BufferedWriter(new FileWriter(cmdln.outDir+File.separator+"net_rc.rc")));
						RC2WriteToFile.writeRC(rcBest, rcOut);
						rcOut.flush();
						rcOut.close();
					}
				}//end createRC
			}
			markTime(4);
			displayTime(3,4,"createRC");

			if(cmdln.noCNF) {
			}
			else if(Pedigree.PREPROCESS!=Pedigree.ALL) {
				println(stream_All, "Not doing anything since rules are turned off.");;
			}
			else {
				MappedList vars_e = new MappedList(gn.bn);
				MultiValuedCNF mvcnf_e = MultiValuedCNF.createFromBN(gn.bn, vars_e);

				MappedList vars_ne = new MappedList(gn.bn);
				MultiValuedCNF mvcnf_ne = MultiValuedCNF.createFromBN(gn.bn, vars_ne);

				markTime(5);
				displayTime(4,5,"Create mvcnf");

				MultiValuedCNF.AugmentForCompilation(mvcnf_e, gn.bn, true/*useEclauses*/);
				MultiValuedCNF.AugmentForCompilation(mvcnf_ne, gn.bn, false/*useEclauses*/);

				markTime(6);
				displayTime(5,6,"Augment for compilation");

				markTime(7);

				for(Iterator itr = cmdln.sat_encoding.iterator(); itr.hasNext();) {
					String encoding = (String)itr.next();

//					File saveCNF = File.createTempFile(cmdln.locus.getName()+"_"+cmdln.sat_encoding+"_", ".cnf", cmdln.outDir);
					File saveCNF = File.createTempFile(cmdln.locus.getName()+"_"+encoding+"_", ".cnf", new File("."));

					HashMap fvToColOfClauses = new HashMap();

					if(encoding.equalsIgnoreCase("L")) {
						if(Map_L.createLogicEncoding(mvcnf_ne, saveCNF, cmdln.locus.toString(), fvToColOfClauses, includeComments)==null) {
							throw new IllegalStateException("ERROR: Could not createMappingForCompilation");
						}
					}
					else if(encoding.equalsIgnoreCase("EClause")) {
						if(Map_EClause.createLogicEncoding(mvcnf_e, saveCNF, cmdln.locus.toString(), fvToColOfClauses, includeComments)==null) {
							throw new IllegalStateException("ERROR: Could not createMappingForCompilation");
						}
					}
					else {
						throw new IllegalStateException("Unknown encoding: " + encoding);
					}

					if(rcBestGen!=null) {
						File saveCNF_DT = File.createTempFile(cmdln.locus.getName()+"_"+encoding+"_", ".cnf_dtree", new File("."));
						PrintStream cnfdtreefile = new PrintStream(new BufferedOutputStream(new FileOutputStream(saveCNF_DT)), true);
						RCGenerator.Util.convertRCtoCnfDtree(rcBestGen, cnfdtreefile, fvToColOfClauses);
					}

					markTime(8);
					displayTime(7,8,"Encode and Create CNF File for encoding: " + encoding + " : " + saveCNF);
					markTime(7);
				}
				markTime(7);
				displayTime(6,7,"Encode and Create CNF File for all encodings");
			}

			//final output
			markTime(8);
			displayTime(0,8, "Total Time");

		}
		catch(Exception e) {
			System.gc(); //possible memory error
			println(stream_All, programTitle + " caught the exception:\n" + e.getMessage());
			e.printStackTrace(outDetails);
		}
		outRCInfo.println("");
		println(stream_All, "\n\n" + programTitle + " " + programVersion + " by Allen and Darwiche (2004)\n\n");
	}


	private void markTime(int indx) {
		time_cpu[indx] = JVMProfiler.getCurrentThreadCpuTimeMS();
		time_sys[indx] = System.currentTimeMillis();
	}
	private long displayTime(int st, int en, String desc) {
		long cpu = time_cpu[en] - time_cpu[st];
		long sys = time_sys[en] - time_sys[st];
		long ret = sys;
		println(stream_Time, "Timing for: " + desc);
		println(stream_Time, "\t" + sys + "\t (ms) System Elapsed Time.");
		if(cpu > 0) {
			println(stream_Time, "\t" + cpu + "\t (ms) CPU Time (if using profiler).");
			ret = cpu;
		}
		return ret;
	}






	/**  This class parses the command line.
	 *
	 *  <p>Possible Options are:
	 *  <ul><b>Required</b>
	 *   <li> sat_encoding: "L", "EClause", ..., "END"  (required if using RC-SAT)(put all of them, followed by the word "END"
	 *   <li> locus: [file]
	 *   <li> pedigree: [file]
	 *  </ul>
	 *  <ul><b>Optional</b>
	 *   <li> outDir: [dir] //default to current
	 *   <li> scalar: [integer] (if not present will use 6)
	 *   <li> noCNF
	 *  </ul>
	 */
	static private class CmdLn {

		String local_args[];

		ArrayList sat_encoding;
		File locus;
		File pedigree;

		File outDir = new File(".\\");
		int scalar = 6;
		boolean noCNF = false;

		public CmdLn( String[] args) {
			if( !parse( args)) { throw new IllegalArgumentException("ERROR: Could not parse command line");}
		}

		public String toString() {
			if( local_args != null) { return Arrays.asList( local_args).toString();}
			else { return "[null]";}
		}

		/** Will parse the command line and return true if it was valid and false if there were any errors.*/
		public boolean parse( String[] args) {

			local_args = args;
			int i=0;

			try {

			for(i=0; i<args.length; i++) {
				if(args[i].equalsIgnoreCase("sat_encoding:")) {
					i++;
					sat_encoding = new ArrayList();
					while(i < args.length) {
						if(args[i].equalsIgnoreCase("END")) {
							break;
						}
						sat_encoding.add(args[i]); i++;
					}
				}
				else if(args[i].equalsIgnoreCase("locus:")) {
					i++;
					locus = new File(args[i]);
				}
				else if(args[i].equalsIgnoreCase("pedigree:")) {
					i++;
					pedigree = new File(args[i]);
				}
				else if(args[i].equalsIgnoreCase("outDir:")) {
					i++;
					outDir = new File(args[i]);
				}
				else if(args[i].equalsIgnoreCase("scalar:")) {
					i++;
					scalar = Integer.parseInt(args[i]);
				}
				else if(args[i].equalsIgnoreCase("noCNF")) {
					noCNF = true;
				}
				else {
					System.err.println("ERROR: Unknown command line argument: " + args[i]);
					return false;
				}
			}

			}
			catch(Exception e) {
				System.err.println("ERROR: Could not parse command line: " + args[i]);
				return false;
			}


			return verifyValid();
		}//end parse


		private boolean verifyValid() {
			if(sat_encoding==null && noCNF==false) {
				System.out.println("ERROR (Command Line): Invalid sat_encoding was empty");
				return false;
			}
			for(Iterator itr=sat_encoding.iterator(); itr.hasNext();) {
				String enc = (String)itr.next();
				if(enc.equalsIgnoreCase("L") || enc.equalsIgnoreCase("EClause")) {}
				else {
					System.out.println("ERROR (Command Line): Invalid sat_encoding: " + enc);
					return false;
				}
			}
			if(locus==null || !locus.exists()) {
				System.out.println("ERROR (Command Line): Invalid locus file: " + locus);
				return false;
			}
			if(pedigree==null || !pedigree.exists()) {
				System.out.println("ERROR (Command Line): Invalid pedigree file: " + pedigree);
				return false;
			}
			if(scalar<=0) {
				System.out.println("ERROR (Command Line): Invalid scalar: " + scalar);
				return false;
			}
			return true;
		}//end verifyValid
	}//end class CmdLn



	private void println(MultipleOutputs out, String str) {
		out.println(str);
	}
	private void println(PrintWriter out, String str) {
		out.println(str);
	}
	private void println(PrintStream out, String str) {
		out.println(str);
	}

	private class MultipleOutputs {
		ArrayList outStreams = new ArrayList();
		ArrayList outWriters = new ArrayList();

		public MultipleOutputs() {}

		public void add(PrintStream out) {
			if(out!=null) { outStreams.add(out);}
		}
		public void add(PrintWriter out) {
			if(out!=null) { outWriters.add(out);}
		}

		public void println(String str) {
			for(int i=0; i<outStreams.size(); i++) {
				((PrintStream)outStreams.get(i)).println(str);
			}
			for(int i=0; i<outWriters.size(); i++) {
				((PrintWriter)outWriters.get(i)).println(str);
			}
		}
	}
}


