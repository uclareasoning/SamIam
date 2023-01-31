package edu.ucla.belief.rc2.tools;

import java.util.*;
import java.io.*;
import java.text.*;

//{superfluous} import edu.ucla.structure.MappedList;

import edu.ucla.util.JVMProfiler;

import edu.ucla.belief.io.geneticlinkage.*;
import edu.ucla.belief.io.NetworkIO;

import edu.ucla.belief.RCGenerator;
import edu.ucla.belief.EOGenerator;

import edu.ucla.belief.rc2.caching.*;
import edu.ucla.belief.rc2.structure.RC2;
import edu.ucla.belief.rc2.structure.RC2Utils;
import edu.ucla.belief.rc2.creation.RC2CreatorEO;
import edu.ucla.belief.rc2.creation.RC2CreatorFile;


public class TestClass {

	static final String programTitle = "Test Searching";
	static final String programVersion = "1.0";

//TODO SET FLAGS (move some to command line)
	boolean generateTestFiles = true;
	boolean extendedSearch = false;
//TODO SET FLAGS

	CmdLn cmdln = null;

	MultipleOutputs stream_All = new MultipleOutputs();
	MultipleOutputs stream_Time = new MultipleOutputs();

	long time_cpu[] = new long[20];
	long time_sys[] = new long[20];



	public TestClass(CmdLn cmdln) {
		this.cmdln = cmdln;
	}

	public static void main(String args[]) {
		TestClass prog = new TestClass(new CmdLn(args));
		prog.start();
	}

	void start() {

		PrintStream outDetails;
		PrintStream outResults;

		try{
			outDetails = new PrintStream(new BufferedOutputStream(new FileOutputStream(cmdln.outDir+File.separator+"detailed.txt")),true);

			stream_All.add(System.out);
			stream_All.add(outDetails);

			stream_Time.add(System.out);
			stream_Time.add(outDetails);
			stream_Time.add(new PrintStream(new BufferedOutputStream(new FileOutputStream(cmdln.outDir+File.separator+"times.txt")),true));

			{
				File oRF = new File("SearchResults.tsv");
				boolean appending = oRF.exists();
				outResults   = new PrintStream(new BufferedOutputStream(new FileOutputStream(oRF,true/*append*/)),true/*autoflush*/);
				if(!appending) {
					outResults.println("    \t      \t Full \t      \t Greedy \t \t");
					outResults.println("Net \t Heur \t Mem \t Calls \t Mem \t Calls \t");
				}
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

			markTime(0);
			{
				Loci loci = Loci.readLoci(cmdln.locus.getPath());

				Pedigree ped = Pedigree.createPedigree(cmdln.pedigree.getPath(), loci);

				markTime(1);
				displayTime(0,1,"Pedigree preprocess");

				gn = ped.createBeliefNetwork(cmdln.scalar);

				markTime(2);
				displayTime(1,2,"Convert to Belief Network");

				if(generateTestFiles) {

					NetworkIO.writeNetwork(gn.bn, new File(cmdln.outDir+File.separator+"X.net"));

				}//end generateTestFiles

				markTime(3);
				displayTime(2,3,"generateTestFiles");

				RC2.RCCreationParams rcParam = new RC2.RCCreationParams(gn.bn);

				RC2CachingScheme_None csNone = new RC2CachingScheme_None();
				RC2CachingScheme_Greedy csGrdy;
				{
					long mm = Runtime.getRuntime().maxMemory();
					double factor = .9;
					long ce = (long)Math.floor(mm/8*factor); //(max memory / 8)=max number of caches, but leave some room for overhead

					csGrdy= new RC2CachingScheme_Greedy(ce);

					println(stream_All,
							"Allow Caching of up to " + ce + " entries (MB: " + ce*8/1048576.0 + ")");
				}


				{ //EOGenerator - EO
					for(int ver=1; ver<=6; ver++) {
						String heurName = "EOGen-EO("+ver+")";

						EOGenerator.EO eo = EOGenerator.generateEO(gn.bn, cmdln.locus.getName(), "searchDetails.tsv", extendedSearch, ver);

						try{//test with RC
							RC2CreatorEO.Params eoParam = new RC2CreatorEO.Params(gn.bn, false, RC2CreatorEO.Params.EO_ConnectRandomly, eo.eo(), csNone);
							RC2 rc = RC2CreatorEO.createDtree(rcParam, eoParam);

							Collection greedyCaching = csGrdy.getCachingScheme(rc);

							String outst = cmdln.locus.getName() + "\t" + heurName + "\t" + rc.compStats().fullCaching().memUsedMB() + "\t" + rc.compStats().fullCaching().rcCalls_pe() + "\t" + (RC2CachingSchemeUtils.expectedMemoryUsage(greedyCaching)*8.0/1048576.0) + "\t" + RC2Utils.expectedRCCalls_Pe(rc, greedyCaching);
							outResults.println(outst);

							println(stream_All, outst);
						}
						catch(Exception e) {
							println(stream_All, "Skipping " + heurName + "...");
							outResults.println(cmdln.locus.getName() + "\t" + heurName + "\t" + Double.MAX_VALUE + "\t" + Double.MAX_VALUE + "\t" + Double.MAX_VALUE + "\t" + Double.MAX_VALUE);
						}
					}
				}

				{ //EOGenerator - RC
					for(int ver=1; ver<=15; ver++) {
						String heurName = "EOGen-RC("+ver+")";

						EOGenerator.RC tmprc = EOGenerator.generateRC(gn.bn, cmdln.locus.getName(), "searchDetails.tsv", extendedSearch, ver);
						HashSet cachedNodes = new HashSet();
						try{
							File tmprcFile = File.createTempFile("rcFile", null, new File("."));
							Writer rcOut = new BufferedWriter(new FileWriter(tmprcFile));
							tmprc.writeFile(rcOut);
							rcOut.flush();
							rcOut.close();
							rcOut = null;

							RC2 rc = RC2CreatorFile.readRC(rcParam, new RC2CreatorFile.Params(gn.bn, tmprcFile.getPath()), cachedNodes);
							tmprcFile.delete();
							tmprcFile = null;

							Collection greedyCaching = csGrdy.getCachingScheme(rc);

							String outst = cmdln.locus.getName() + "\t" + heurName + "\t" + rc.compStats().fullCaching().memUsedMB() + "\t" + rc.compStats().fullCaching().rcCalls_pe() + "\t" + (RC2CachingSchemeUtils.expectedMemoryUsage(greedyCaching)*8.0/1048576.0) + "\t" + RC2Utils.expectedRCCalls_Pe(rc, greedyCaching);
							outResults.println(outst);

							println(stream_All, outst);
						}
						catch(Exception e) {
							println(stream_All, "Skipping " + heurName + "...");
							outResults.println(cmdln.locus.getName() + "\t" + heurName + "\t" + Double.MAX_VALUE + "\t" + Double.MAX_VALUE + "\t" + Double.MAX_VALUE + "\t" + Double.MAX_VALUE);
						}
					}
				}

				{ //RCGenerator - EO
					String heurName = "RCGen-EO";

					RCGenerator.EO tmpeo = RCGenerator.generateEO(gn.bn, cmdln.locus.getName(), "searchDetails.tsv", extendedSearch, gn.equivPreProcessVars());
					try{
						RC2CreatorEO.Params eoParam = new RC2CreatorEO.Params(gn.bn, false, RC2CreatorEO.Params.EO_ConnectRandomly, tmpeo.eo(), csNone);
						RC2 rc = RC2CreatorEO.createDtree(rcParam, eoParam);

						Collection greedyCaching = csGrdy.getCachingScheme(rc);

						String outst = cmdln.locus.getName() + "\t" + heurName + "\t" + rc.compStats().fullCaching().memUsedMB() + "\t" + rc.compStats().fullCaching().rcCalls_pe() + "\t" + (RC2CachingSchemeUtils.expectedMemoryUsage(greedyCaching)*8.0/1048576.0) + "\t" + RC2Utils.expectedRCCalls_Pe(rc, greedyCaching);
						outResults.println(outst);

						println(stream_All, outst);
					}
					catch(Exception e) {
						println(stream_All, "Skipping " + heurName + "...");
						outResults.println(cmdln.locus.getName() + "\t" + heurName + "\t" + Double.MAX_VALUE + "\t" + Double.MAX_VALUE + "\t" + Double.MAX_VALUE + "\t" + Double.MAX_VALUE);
					}
				}

				{ //RCGenerator - RC
					String heurName = "RCGen";

					RCGenerator.RC tmprc = RCGenerator.generateRC(gn.bn, cmdln.locus.getName(), "searchDetails.tsv", extendedSearch, gn.equivPreProcessVars());
					HashSet cachedNodes = new HashSet();
					try{
						File tmprcFile = File.createTempFile("rcFile", null, new File("."));
						Writer rcOut = new BufferedWriter(new FileWriter(tmprcFile));
						tmprc.writeFile(rcOut);
						rcOut.flush();
						rcOut.close();
						rcOut = null;

						//could cause memory trouble
						RC2 rc = RC2CreatorFile.readRC(rcParam, new RC2CreatorFile.Params(gn.bn, tmprcFile.getPath()), cachedNodes);
						tmprcFile.delete();
						tmprcFile = null;

						Collection greedyCaching = csGrdy.getCachingScheme(rc);

						String outst = cmdln.locus.getName() + "\t" + heurName + "\t" + rc.compStats().fullCaching().memUsedMB() + "\t" + rc.compStats().fullCaching().rcCalls_pe() + "\t" + (RC2CachingSchemeUtils.expectedMemoryUsage(greedyCaching)*8.0/1048576.0) + "\t" + RC2Utils.expectedRCCalls_Pe(rc, greedyCaching);
						outResults.println(outst);

						println(stream_All, outst);
					}
					catch(Exception e) {
						println(stream_All, "Skipping " + heurName + "...");
						outResults.println(cmdln.locus.getName() + "\t" + heurName + "\t" + Double.MAX_VALUE + "\t" + Double.MAX_VALUE + "\t" + Double.MAX_VALUE + "\t" + Double.MAX_VALUE);
					}
				}
			}
			markTime(4);
			displayTime(3,4,"createEO");

			outResults.flush();

			long time = displayTime(0,4, "Total Time");
			println(stream_All, "\nTotal Time used: " + (time/1000.0) + " (sec)");
		}
		catch(Exception e) {
			println(stream_All, programTitle + " caught the exception:\n" + e.getMessage());
			e.printStackTrace(outDetails);
		}
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



	private void println(MultipleOutputs out, String str) {
		out.println(str);
	}
	private void println(PrintWriter out, String str) {
		out.println(str);
	}
	private void println(PrintStream out, String str) {
		out.println(str);
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
	 *  </ul>
	 */
	static private class CmdLn {

		String local_args[];

		File locus;
		File pedigree;

		File outDir = new File(".\\");
		int scalar = 6;

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
				if(args[i].equalsIgnoreCase("locus:")) {
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


