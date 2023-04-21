package edu.ucla.belief.rcform;

import edu.ucla.belief.*;
import edu.ucla.belief.io.*;
import edu.ucla.belief.rc2.creation.*;
import edu.ucla.belief.rc2.structure.*;

import java.io.*;
import java.util.*;

/**
 * RCForm Test Code.
 *
 * @author Taylor Curtis
 */
public class RCFormTester
{
	public static void main(String[] args) throws Exception
	{	
		boolean run = false;
		boolean extra_caching = false;
		
		System.out.println("Begin testing RCForm");
	
		Random rng = new Random();	
		BeliefNetwork bn = null;
		
		//Read a network
		bn = NetworkIO.read(args[0]);
		
		RCFormDtree dtree = null;
		
		//Get dtree from file if provided
		if (args.length > 1)
		{
			dtree = new RCFormDtree(bn, args[1]);		
		}
		else
		{
			//Create a dtree from the network
		
			//Generate an RCGenerator.RC object that has the structure of the dtree
			RCGenerator.RC skeleton = RCGenerator.generateRC(bn, "testbn", "rcSearch_detailed.tsv", false, null, null);
		
			dtree = new RCFormDtree(bn, skeleton);
			/*
			BufferedWriter bw = new BufferedWriter(new FileWriter("blah.autodt"));
			StringWriter sw = new StringWriter();
			skeleton.writeFile(sw);
			bw.write(sw.toString());
			bw.close();
			sw.close();
			*/
		}
	
		//Print the dtree
		//System.out.println(dtree);
		
		//Prepare the dtree for inference
		dtree.preprocess();
			
		if (run)
		{
			//Run RCForm
			
			//Run with no evidence
			System.out.println("Prob of no evidence = " + RCForm.rcform(dtree, new Term(), extra_caching) + " (" + RCFormStats.sum_num_calls + " calls)");
			
			// See if the number of calls at each node was as predicted
			
			//dtree.getRoot().checkNodeCalls(extra_caching);
			//dtree.getRoot().checkCacheSizes(extra_caching);
			
			
			//Run with random evidence
			
			List<FiniteVariable> network_vars = dtree.getRoot().getEndogenousVars();
			Term random_instantiation = null;
			for (int i=0; i<5; i++)
			{
				random_instantiation = RCFormUtils.randomTerm(network_vars, rng);
				System.out.println("Prob(" + random_instantiation + ") = " + RCForm.rcform(dtree, random_instantiation, extra_caching)+ " (" + RCFormStats.sum_num_calls + " calls)");	
			
			}
			
		}
	}
	
	//Copied from edu/ucla/belief/rc2/tools/NetworkDiff.java (David's code)
	/*
	public static RC2 createRC2FromSkeleton(BeliefNetwork bn, RCGenerator.RC skeleton) throws Exception
	{
		File tmprc = File.createTempFile("rcFile", null, new File("."));
		Writer rcOut = new BufferedWriter(new FileWriter(tmprc));
		skeleton.writeFile(rcOut);
		rcOut.flush();
		rcOut.close();
		rcOut = null;

		//could cause memory trouble
		RC2 rc2 = RC2CreatorFile.readRC(new RC2.RCCreationParams(bn), new RC2CreatorFile.Params(bn, tmprc.getPath()), new HashSet());

		tmprc.delete();
		tmprc = null;	
		
		return rc2;
	}
	*/
	
	
}