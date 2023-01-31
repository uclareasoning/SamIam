package edu.ucla.belief.uai2006;

import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.BeliefNetworkImpl;
import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.FiniteVariableImpl;
//import edu.ucla.belief.io.*;
import il2.inf.structure.*;
import il2.model.*;
import il2.util.IntList;
import il2.util.IntSet;
import il2.bridge.*;
import java.io.*;
import java.util.*;

public class UaiEdgeCutter {
	public static final double EPSILON = 0.0001;
	public static BeliefNetwork myBeliefNetwork;
	public static Converter myConverter;

	public static void main(String[] args) {
		String filename = args[0];
		double targetSize = Double.valueOf(args[1]).doubleValue();
		String outfilename = filename + ".output.txt";
		BeliefNetwork origNet = null;
		BeliefNetwork approxNet = null;

		origNet = UaiEdgeCutter.loadBeliefNetwork(filename);
		if ( origNet == null ) return;

		System.out.println("Examining Network ... ");
		approxNet = examineNetwork(origNet,targetSize);
		System.out.println("Simplifying Network ... ");

		Map varToClones = new HashMap<FiniteVariable,Set<FiniteVariable>>(origNet.size());
		Map oldToNew = new HashMap<FiniteVariable,FiniteVariable>(origNet.size());
		try {
			approxNet=simplifyNetwork(origNet,targetSize,varToClones,oldToNew);
		} catch ( Exception e ) {
			System.err.println(e);
			e.printStackTrace();
		}
		System.out.println(varToClones);
		System.out.println(oldToNew);
		//UaiEdgeCutter.writeNetwork(approxNet,filename+".pruned.net");
	}

	public static final String STR_XBIF = ".XBIF";
	/* given filename, read in network, evidence (and MAP
	 * variables?) */
	public static BeliefNetwork loadBeliefNetwork(String filename) {
		BeliefNetwork bn = null;

		if ( filename == null ) {
			System.err.println("Input network not specified.");
			System.exit(1);
		}

		// will check for XBIF extention, and warn if not XBIF
		String fileExt = filename.substring(filename.lastIndexOf('.'));

		verbosePrintln("Opening network : " + filename);
		try {
			if ( fileExt.equalsIgnoreCase( STR_XBIF ) ) {
				bn = new edu.ucla.belief.io.xmlbif.XmlbifParser().
					beliefNetwork( new java.io.File(filename), null );
			} else {
				System.err.println("Input file not XBIF, continuing ...");
				bn = edu.ucla.belief.io.NetworkIO.read(filename);
			}
		} catch ( Exception e ) {
			System.err.println("Failed loading " + filename + ": " + e);
			System.exit(1);
		}

		if ( bn == null) {
			System.err.println("Failed loading " + filename);
			System.exit(1);
		}

		return bn;
	}

	public static final boolean verbose = true;
	public static void errorPrintln(String s) {
		System.out.println(s);
	}

 	public static void verbosePrint(String s) {
		if ( verbose ) System.out.print(s);
	}

	public static void verbosePrintln(String s) {
		if ( verbose ) System.out.println(s);
	}

	public static void writeNetwork(BeliefNetwork bn, String filename) {
		try {
			verbosePrintln("Writing out approximated network : " + filename);
			edu.ucla.belief.io.NetworkIO.writeNetwork
				(bn, new java.io.File(filename));
			//java.io.PrintStream ps = new java.io.PrintStream(new java.io.FileOutputStream(new java.io.File(filename)));
			//new edu.ucla.belief.io.xmlbif.XmlbifWriter().write(bn,ps);
		} catch (Exception e) {
			System.err.println("Error writing approx net: " + e);
			e.printStackTrace();
		}
	}

	public static int countLeafNodes(BeliefNetwork bn) {
		int count = 0;
		Set v = bn.vertices();
		for (Iterator it = v.iterator(); it.hasNext(); )
			if ( bn.outDegree(it.next()) == 0 )
				count++;
		return count;
	}

	public static IntList getNewOrder(Collection origDomains) {
		EliminationOrders.Record newRecord =
			EliminationOrders.minFill(origDomains,12,new Random());
		return newRecord.order;
	}

	public static void getEdgesToCut(BeliefNetwork origBeliefNetwork,
									 double targetSize,
									 List parentList, List childList) {
		Converter netConverter = new Converter();
		BayesianNetwork origNet = netConverter.convert(origBeliefNetwork);
		myConverter = netConverter; myBeliefNetwork = origBeliefNetwork;

		List origDomains = Arrays.asList( origNet.cpts() );
		IntList order = getNewOrder(origDomains);
		//MyRecord record = createMyRecord(origDomains, order);

		Vector myParentList = new Vector();
		Vector myChildList = new Vector();

		createMyRecordWithinBound(origDomains, order, targetSize,
								  origDomains, myParentList, myChildList);

		for (int i = 0; i < myParentList.size(); i++) {
			FiniteVariable parent = netConverter.convert
				(((Integer)myParentList.get(i)).intValue());
			FiniteVariable child = netConverter.convert
				(((Integer)myChildList.get(i)).intValue());

			parentList.add(parent);
			childList.add(child);
		}
	}

	public static BeliefNetwork examineNetwork(BeliefNetwork origBeliefNet,
											   double targetSize) {
		Converter netConverter = new Converter();
		BeliefNetwork approxBeliefNet = (BeliefNetwork)origBeliefNet.clone();
		BayesianNetwork origNet = netConverter.convert(approxBeliefNet);
		Domain domain = origNet.domain();

		myConverter = netConverter; myBeliefNetwork = approxBeliefNet;

		List origDomains = Arrays.asList( origNet.cpts() );

		IntList order = getNewOrder(origDomains);
		MyRecord record = createMyRecord(origDomains, order);

		System.out.println("Original Size    : " + record.size);
		System.out.println("  # of leaves    : " +
						   countLeafNodes(origBeliefNet));
		System.out.println("  # of non-leaves: " + (origBeliefNet.size() -
						   countLeafNodes(origBeliefNet)));
		System.out.println("Original # edges : " + origBeliefNet.numEdges());
		System.out.println("Max CPT size : " +
						   (Math.log(origBeliefNet.getMaxTheoreticalCPTSize())
							/Math.log(2)));
		System.out.println();

		long time;
		System.out.println("running ...");
		Vector parentList = new Vector();
		Vector childList = new Vector();
		time = System.currentTimeMillis();
		createMyRecordWithinBound(origDomains, order, targetSize,
								  origDomains, parentList, childList);
		time = System.currentTimeMillis() - time;
		System.out.print("done!");
		System.out.println(" Time:" + time);
		System.out.println();


		for (int i = 0; i < parentList.size(); i++)
			approxBeliefNet.removeEdge
				(netConverter.convert(((Integer)parentList.get(i)).intValue()),
				 netConverter.convert(((Integer)childList.get(i)).intValue()));

		/*
		System.out.println("Edges Deleted: " + parentList.size());
		System.out.println();
		for (int i = 0; i < parentList.size(); i++)
//			System.out.println(childList.get(i) + " <- " + parentList.get(i));
			System.out.println
				(domain.name(((Integer)parentList.get(i)).intValue()) +
				 " -> " +
				 domain.name(((Integer)childList.get(i)).intValue()));
		System.out.println();
		*/

		Converter netConverter2 = new Converter();
		BayesianNetwork approxNet = netConverter2.convert(approxBeliefNet);
		List origDomains2 = Arrays.asList( approxNet.cpts() );
		Domain domain2 = approxNet.domain();
		IntList order2 = new IntList();
		for (int i = 0; i < order.size(); i++)
			order2.add(netConverter2.convert
					   (netConverter.convert(order.get(i))));
		MyRecord record2 = createMyRecord(origDomains2, order2);
		// System.out.print("Order2 : " + order2 + " checking ... ");
		boolean checkOk = true;
		for (int i = 0; i < order2.size(); i++)
			if ( domain.name(order.get(i)) != domain2.name(order2.get(i)) )
				checkOk = false;
		System.out.println(checkOk ? "OK!" : "\nFAILED!!\nFAILED!!");

		System.out.println("Approx Size  : " + record2.size);
		System.out.println("Edges Deleted: " + parentList.size());
		Set parentSet = new HashSet(parentList);
		System.out.println("Num parents  : " + parentSet.size());
		int numroots = 0;
		for (Iterator it = parentSet.iterator(); it.hasNext(); ) {
			int v = ((Integer)it.next()).intValue();
			edu.ucla.belief.FiniteVariable var = netConverter.convert(v);
			if ( origBeliefNet.inDegree(var) == 0 ) numroots++;
		}
		System.out.println("Num roots    : " + numroots);
		System.out.println();

		return approxBeliefNet;
	}

	/****************************************
	 ****************************************
	 ****************************************/

	// from EliminationOrders.java
	// MyRecord start
    public static class MyRecord {
		public final IntList order;
		public final double size;
        public final IntSet[] clusters;

		public final Bucketer buckets;
		public MyRecord(IntList o,double s,IntSet[] c,Bucketer b){
			order=o;
			size=s;
            clusters=c;
			order.lock();
			buckets = b;
		}
    }
	// MyRecord end

	// MyIndex start
	public static class MyIndex extends Index {
		Vector subDomains;
		Domain myDomain;
		IntSet myVars;

		public MyIndex( Domain d, IntSet variables, Collection c ){
			super(d,variables);
			myDomain = d;
			myVars = variables;
			subDomains = new Vector(c);
		}

		public MyIndex forgetIndex(int var) {
			IntSet nv = new IntSet(myVars);
			nv.remove(var);
			return new MyIndex(myDomain,nv,subDomains);
		}

		public void printSubDomains(List origDomains) {
			UaiEdgeCutter.printSubDomains(subDomains, origDomains);
		}

		/* assumes none of the tables contained have var as a child */
		public void findEdgesToDropTable(int var, List origDomains,
										 List parentList, List childList) {
			UaiEdgeCutter.findEdgesToDropTable(subDomains,var,origDomains,
											parentList,childList);
		}

		/* assumes none of the tables contained have var as a child */
		public int countEdgesToDropTable(int var, List origDomains) {
			return UaiEdgeCutter.countEdgesToDropTable(subDomains,var,
													   origDomains);
		}

		public boolean containsVarAsChild(int var, List origDomains) {
			return UaiEdgeCutter.containsVarAsChild(subDomains,
													 var,origDomains);
		}
	}
	// MyIndex end

    private static MyIndex createIndex(Collection sds){
        Domain d=null;
        IntSet elements=new IntSet();
        for(Iterator iter=sds.iterator();iter.hasNext();){
            SubDomain sd=(SubDomain)iter.next();
            elements=elements.union(sd.vars());
            d=sd.domain();
        }
		MyIndex myIndex = new MyIndex(d,elements,sds);
        return myIndex;
    }

	private static MyIndex createMyIndex(Index index) {
		Vector v = new Vector();
		v.add(index);
		return createIndex(v);
	}

	// from EliminationOrders.java
	// from EliminationOrders.java
    public static MyRecord createMyRecord(Collection subDomains,IntList order){
        IntSet[] clusters=new IntSet[order.size()];
        Bucketer b=new Bucketer(order);
        b.placeInBuckets(subDomains);
        double biggest=0;
        Domain d=((SubDomain)subDomains.iterator().next()).domain();
        for(int i=0;i<order.size();i++){
            ArrayList l=b.getBucket(i);
            MyIndex ind=createIndex(l);
            double size=d.logSize(ind.vars());
            clusters[i]=ind.vars();
            if(size>biggest){
                biggest=size;
            }
            b.placeInBucket(ind.forgetIndex(order.get(i)));
        }
        return new MyRecord(order,biggest,clusters,b);
    }

	public static double getSubDomainSize(Index index, Domain domain) {
		return domain.logSize(index.vars());
	}

	public static double getBucketSize(Collection subDomains, Domain domain) {
		MyIndex ind = createIndex(subDomains);
		return domain.logSize(ind.vars());
	}

	/***** other edge deletion stuff *****/

    public static MyRecord createMyRecordWithinBound
		(Collection subDomains, IntList order, double bound, List origDomains,
		 List parentList, List childList) {
		Vector bannedEdgeList = new Vector();
		return createMyRecordWithinBound
			(subDomains,order,bound,origDomains,
			 parentList,childList,bannedEdgeList);
	}

    public static MyRecord createMyRecordWithinBound
		(Collection subDomains, IntList order, double bound, List origDomains,
		 List parentList, List childList, List bannedEdgeList) {
		// AC : is this ok?  does not work necessarily if variables
		// are binary (sometimes need to decrement by more than one)
		boolean boundDecremented = false;

		boolean testing = false;

        IntSet[] clusters=new IntSet[order.size()];
        Bucketer b=new Bucketer(order);
        b.placeInBuckets(subDomains);
        double biggest=0;
        Domain d=((SubDomain)subDomains.iterator().next()).domain();
        for(int i=0;i<order.size();i++){
            ArrayList l=b.getBucket(i);
			MyIndex ind=createIndex(l);
			double size=d.logSize(ind.vars());
			clusters[i]=ind.vars();

			if ( testing ) {
				System.out.print("::: B" + i + " : " + order.get(i) +
								 "(" + d.name(order.get(i)) + ")");
				System.out.print(" Size : " + size + "\n  BUCKET: ");
				printSubDomains(l,origDomains);
				System.out.println();
			}

			while ( size > bound + EPSILON ) {
				int smallestSizeIndex = -1, deepestDropIndex = -1;
				int leastEdgesIndex = -1, bestIndex = -1;
				double smallestSize = Double.POSITIVE_INFINITY;
				int deepestDrop = -1, leastEdges = Integer.MAX_VALUE;
				double curSize = 0.0;
				int curDrop = 0, curEdges = 0;

				double largestDroppedSize = Double.POSITIVE_INFINITY;
				double curDroppedSize;

				for (int j = 0; j < l.size(); j++) {

					if ( testing ) {
						System.out.print("    ::: " + getSubDomainSize
										 ((Index)l.get(j),d)+" ");
						printSubDomains((Index)l.get(j),origDomains);
					}

					if ( containsVarAsChild((Index)l.get(j), order.get(i),
											origDomains) ) {
						if ( testing ) System.out.println();
						continue;
					}

					Vector tempPL = new Vector();
					Vector tempCL = new Vector();
					findEdgesToDropTable((Index)l.get(j),order.get(i),
										 origDomains,tempPL,tempCL);
					/* no longer banning edges?
					if ( containsBannedEdges(tempPL, tempCL, bannedEdgeList) )
						continue;
					*/

					if ( testing )
						System.out.print(" : " + tempPL + "," + tempCL);

					Object curObject;
					curObject = l.remove(j);
					curSize = getBucketSize(l,d);

					if ( ( curSize < smallestSize ) ) {
				 //	|| ( curSize+EPSILON <= bound && Math.random() < .5 ) ) {
						bestIndex = j;
						smallestSize = curSize;
					}

					/*
					curDrop = b.goesInBucket((Index)curObject,i);
					curEdges = countEdgesToDropTable
						((Index)curObject, order.get(i), origDomains);
					curDroppedSize = getSubDomainSize((Index)curObject,d);
					*/

					/*
					if ( ( curSize + EPSILON < smallestSize ) ||
						 ( curSize <= smallestSize + EPSILON &&
						   curDroppedSize <= largestDroppedSize + EPSILON ) ||
						 ( curSize <= smallestSize + EPSILON &&
						   curDroppedSize <= largestDroppedSize + EPSILON &&
						   curDrop > deepestDrop ) ||
						 ( curSize <= smallestSize + EPSILON &&
						   curDroppedSize <= largestDroppedSize + EPSILON &&
						   curDrop == deepestDrop &&
						   curEdges < leastEdges ) ) {
						bestIndex = j;
						smallestSize = curSize;
						deepestDrop = curDrop;
						leastEdges = curEdges;
						largestDroppedSize = curDroppedSize;
					}
					*/

					if ( testing )
						System.out.println(" - S:" + curSize + " D:" +
										   curDrop + " E:" + curEdges );

					l.add(j,curObject);
				}

				if ( bestIndex == -1 ) {
					// failed to drop enough tables
					/*
					java.text.NumberFormat sizeNf =
						java.text.NumberFormat.getInstance();
					sizeNf.setMinimumFractionDigits(2);
					sizeNf.setMaximumFractionDigits(2);

					System.out.print("Failed on bound : ");
					System.out.print(sizeNf.format(size) + " > " +
									 sizeNf.format(bound));
					System.out.print(", at " + d.name(order.get(i)));
					System.out.println(", adjusting...");
					*/
					System.out.print("x");
					// AC
					// bound = size;
					if ( ! boundDecremented ) {
						bound--;
						boundDecremented = true;
					}
					break;
				}

				Index best = (Index)l.remove(bestIndex);

				if ( testing )
					System.out.println("Dropped Table in bucket B" +
									   i + " " + d.name(order.get(i)) +
									   " index " + bestIndex);

				Vector tempPL = new Vector();
				Vector tempCL = new Vector();
				findEdgesToDropTable(best, order.get(i), origDomains,
									 tempPL, tempCL);
				// banIncomingEdges(tempCL,bannedEdgeList); // AC
				//System.out.println(" ::: " + bannedEdgeList.size());
				parentList.addAll(tempPL);
				childList.addAll(tempCL);
				b.placeInBucket(createMyIndex(best).forgetIndex(order.get(i)));

				ind=createIndex(l);
				size=d.logSize(ind.vars());
				clusters[i]=ind.vars();
			}
			if(size>biggest){
                biggest=size;
            }
			MyIndex newIndex = ind.forgetIndex(order.get(i));
			b.placeInBucket(newIndex);
        }
        return new MyRecord(order,biggest,clusters,b);
    }

	public static void printSubDomains(Index subDomain, List origDomains) {
		Vector singleDomain = new Vector();
		singleDomain.add(subDomain);
		printSubDomains(singleDomain,origDomains);
	}

	public static void printSubDomains(List subDomains, List origDomains) {
		System.out.print(" (");
		for (int i = 0; i < subDomains.size(); i++)
			if ( origDomains.indexOf(subDomains.get(i)) == -1 )
				((MyIndex)subDomains.get(i)).printSubDomains(origDomains);
			else
				System.out.print(" [ " +
								 ((Table)subDomains.get(i)).varString() + "]");
		System.out.print(" )");
	}

	public static void findEdgesToDropTable(Index subDomain, int var,
											List origDomains,
											List parentList, List childList) {
		Vector singleDomain = new Vector();
		singleDomain.add(subDomain);
		findEdgesToDropTable(singleDomain,var,origDomains,
							 parentList,childList);
	}

	/* assumes none of the tables contained have var as a child */
	public static void findEdgesToDropTable(List subDomains, int var,
											List origDomains,
											List parentList, List childList) {
		for (int i = 0; i < subDomains.size(); i++) {
			Index subDomain = (Index)subDomains.get(i);
			if ( origDomains.contains(subDomain) ) {
				IntSet curVars = subDomain.vars();
				if ( curVars.contains(var) ) {
					parentList.add(var);
					childList.add(curVars.get(curVars.size()-1));
				}
			} else
				((MyIndex)subDomains.get(i)).findEdgesToDropTable
					(var, origDomains, parentList, childList);
		}
	}

	public static int countEdgesToDropTable(Index subDomain, int var,
											List origDomains) {
		Vector singleDomain = new Vector();
		singleDomain.add(subDomain);
		return countEdgesToDropTable(singleDomain,var,origDomains);
	}

	/* assumes none of the tables contained have var as a child */
	public static int countEdgesToDropTable(List subDomains, int var,
											List origDomains) {
		int count = 0;
		for (int i = 0; i < subDomains.size(); i++) {
			Index subDomain = (Index)subDomains.get(i);
			if ( origDomains.contains(subDomain) ) {
				IntSet curVars = subDomain.vars();
				if ( curVars.contains(var) ) count++;
			} else
				count += ((MyIndex)subDomains.get(i)).countEdgesToDropTable
					(var, origDomains);
		}
		return count;
	}

	public static boolean containsVarAsChild(Index subDomain,
											 int var, List origDomains) {
		Vector singleDomain = new Vector();
		singleDomain.add(subDomain);
		return containsVarAsChild(singleDomain,var,origDomains);
	}

	public static boolean containsVarAsChild(List subDomains,
											 int var, List origDomains) {
		for (int i = 0; i < subDomains.size(); i++) {
			Index subDomain = (Index)subDomains.get(i);
			if ( origDomains.contains(subDomain) ) {
				IntSet curVars = subDomain.vars();
				if ( curVars.indexOf(var) == (curVars.size()-1) )
					return true;
			} else
				if (((MyIndex)subDomain).containsVarAsChild(var, origDomains))
					return true;
		}
		return false;
	}

	/* AC: this banned edge code is not great... */
	public static boolean containsBannedEdges(List parentList,
											  List childList,
											  List bannedEdgeList) {
		for (int i = 0; i < parentList.size(); i++) {
			String edge =
				(Integer)parentList.get(i) + "->" + (Integer)childList.get(i);
			if ( bannedEdgeList.contains(edge) ) {
				// System.out.println("found banned edge : " + edge);
				return true;
			}
		}
		return false;
	}

	public static void banIncomingEdges(List childList, List bannedEdgeList) {
		for (int i = 0; i < childList.size(); i++) {
			int child = ((Integer)childList.get(i)).intValue();
			edu.ucla.belief.FiniteVariable var = myConverter.convert(child);
			Vector parents = new Vector(myBeliefNetwork.inComing(var));
			for (int j = 0; j < parents.size(); j++) {
				var = (edu.ucla.belief.FiniteVariable)parents.get(j);
				int parent = myConverter.convert(var);
				String edge = parent + "->" + child;
				bannedEdgeList.add(edge);
			}
		}
	}

	/****************************************/
	/****************************************/
	/****************************************/
	/****************************************/
	/****************************************/

	public static FiniteVariable[] createClones
		(List parentList,Map<FiniteVariable,Set<FiniteVariable>> varToClones) {
		FiniteVariable[] clones = new FiniteVariable[parentList.size()];

		Set parentSet = new HashSet(parentList);
		for (Iterator it = parentSet.iterator(); it.hasNext(); )
			varToClones.put((FiniteVariable)it.next(),
							new HashSet<FiniteVariable>());

		// add dongle to child of each deleted edge
		for (int i = 0; i < parentList.size(); i++) {
			FiniteVariable parent = (FiniteVariable)parentList.get(i);
			String id = parent.getID() + "_" + i;
			List instances = parent.instances();
			clones[i] = new FiniteVariableImpl(id, instances);
			varToClones.get(parent).add(clones[i]);
		}
		return clones;
	}

	public static double[] createIdentity(int states) {
		double[] table = new double[states];
		for ( int i = 0; i < states; i++ ) table[i] = 1.0;
		return table;
	}

	public static int getEdgeIndex(List parentList, List childList,
								   FiniteVariable parent,FiniteVariable child){
		for ( int i = 0; i < parentList.size(); i++ )
			if ( parentList.get(i) == parent && childList.get(i) == child )
				return i;
		return -1;
	}

	/**
	 * this method creates the structure for the network, ready for
	 * parameterization.
	 */
	public static BeliefNetwork createApproxNet
		(BeliefNetwork bn, List parentList, List childList,
		 FiniteVariable[] clones,
		 Map<FiniteVariable,Set<FiniteVariable>> varToClones,
		 Map<FiniteVariable,FiniteVariable> oldToNew) {
		Map varToCptShells = new HashMap(bn.size());

		// let us try to do this in topological order, of the edge
		// deleted network: clones, then original variables

		// add clones to map
		for ( int i = 0; i < clones.length; i++ ) {
			List cptVars = new ArrayList(1);
			cptVars.add(clones[i]);
			edu.ucla.belief.TableShell cpt = new edu.ucla.belief.TableShell
				(new edu.ucla.belief.Table(cptVars));
			cpt.setValues(createIdentity(clones[i].size()));
			varToCptShells.put(clones[i],cpt);
		}

		// clone original network variables to map
		for ( Iterator it = bn.topologicalOrder().iterator(); it.hasNext(); ) {
			FiniteVariable var = (FiniteVariable)it.next();
			FiniteVariable approxVar = (FiniteVariable)var.clone(); //AC
			edu.ucla.belief.CPTShell cpt =
				(edu.ucla.belief.CPTShell)var.getCPTShell().clone();
			oldToNew.put(var,approxVar);

			if ( varToClones.keySet().contains(var) )
				varToClones.get(var).add(approxVar);

			// replace original vars of cpt with approx net vars
			Map replaceMap = new HashMap(cpt.variables().size());
			for ( Iterator vit = cpt.variables().iterator(); vit.hasNext(); ) {
				FiniteVariable replaceVar = (FiniteVariable)vit.next();
				int varIndex =
					getEdgeIndex(parentList,childList,replaceVar,var);
				if ( varIndex == -1 )
					replaceMap.put(replaceVar,oldToNew.get(replaceVar));
				else
					replaceMap.put(replaceVar,clones[varIndex]);
			}
			cpt.replaceVariables(replaceMap,false);
			varToCptShells.put(approxVar,cpt);
		}

		return new BeliefNetworkImpl(varToCptShells);
	}

	public static Map oldEvidenceToNewEvidence
		(Map oldEvidence, Map<FiniteVariable,FiniteVariable> oldToNew) {
		Map newEvidence = new HashMap(oldEvidence.size());
		for ( Iterator it = oldEvidence.keySet().iterator(); it.hasNext(); ) {
			FiniteVariable oldVar = (FiniteVariable)it.next();
			FiniteVariable newVar = oldToNew.get(oldVar);
			int index = oldVar.index(oldEvidence.get(oldVar));
			newEvidence.put(newVar,newVar.instance(index));
		}
		return newEvidence;
	}

	public static BeliefNetwork simplifyNetwork
		(BeliefNetwork bn, double targetSize,
		 Map<FiniteVariable,Set<FiniteVariable>> varToClones,
		 Map<FiniteVariable,FiniteVariable> oldToNew) throws Exception {
		List parentList = new ArrayList();
		List childList = new ArrayList();

		getEdgesToCut(bn,targetSize,parentList,childList);
		System.out.println("Edges Deleted In Simplification: " +
						   parentList.size() + "("+childList.size()+")");

		FiniteVariable[] clones = createClones(parentList,varToClones);
		BeliefNetwork newBn = createApproxNet(bn,parentList,childList,clones,
											  varToClones,oldToNew);

		Map oldEvidence = bn.getEvidenceController().evidence();
		Map newEvidence = oldEvidenceToNewEvidence(oldEvidence, oldToNew);
		newBn.getEvidenceController().setObservations(newEvidence);

		return newBn;
	}

	/****************************************/
	/****************************************/
	/****************************************/
	/****************************************/
	/****************************************/

	public static class BpVarComparator implements java.util.Comparator {
		edu.ucla.belief.approx.PropagationInferenceEngineImpl ibpIe;

		public BpVarComparator
			(edu.ucla.belief.approx.PropagationInferenceEngineImpl ibpIe) {
			this.ibpIe = ibpIe;
		}

		public int compare(Object o1, Object o2) {
			FiniteVariable v1 = (FiniteVariable)o1;
			FiniteVariable v2 = (FiniteVariable)o2;
			double e1 = ibpIe.conditional(v1).entropy();
			double e2 = ibpIe.conditional(v2).entropy();

			if ( e1 < e2 ) return -1;
			if ( e1 > e2 ) return 1;
			else return v1.getID().compareTo(v2.getID());
		}
	}

	public static class BpInstanceComparator implements java.util.Comparator {
		FiniteVariable var;
		edu.ucla.belief.Table beliefs;

		public BpInstanceComparator(FiniteVariable var,
									edu.ucla.belief.Table beliefs) {
			this.var = var;
			this.beliefs = beliefs;
		}

		public int compare(Object instance1, Object instance2) {
			int index1 = var.index(instance1);
			int index2 = var.index(instance2);
			double b1 = beliefs.getCP(index1);
			double b2 = beliefs.getCP(index2);

			if ( b1 < b2  ) return -1;
			if ( b1 > b2 ) return 1;
			if ( index1 < index2 ) return -1;
			if ( index1 > index2 ) return 1;
			else return 0;
		}
	}

	public static List orderedInstances(FiniteVariable var,
										edu.ucla.belief.Table beliefs) {
		List instances = new ArrayList(var.instances());
		Collections.sort(instances,new BpInstanceComparator(var,beliefs));
		Collections.reverse(instances);
		return instances;
	}

	//import edu.ucla.belief.approx.*;
	/* Input: a Bayesian Network bn,
	   Output: a mapping from all network variables to the variable
	   instance that maximizes the respective IBP node marginal.
	*/
	public static TreeMap<FiniteVariable,List> maxBpMarginals(BeliefNetwork bn){
		edu.ucla.belief.approx.PropagationInferenceEngineImpl ibpIe = null;

		// These are parameters for IBP.  These appear to be
		// sufficient to allow algorithm to converge on coding
		// networks.
		int maxIterations = 100;
		long timeoutMillis = 10000;
		double convergenceThreshold = 10e-4;

		// Construct InferenceEngine
		edu.ucla.belief.approx.BeliefPropagationSettings ibpIeSettings =
			new edu.ucla.belief.approx.BeliefPropagationSettings
			(timeoutMillis,maxIterations,convergenceThreshold,
			 edu.ucla.belief.approx.MessagePassingScheduler.TOPDOWNBOTTUMUP);
		edu.ucla.belief.approx.PropagationEngineGenerator dynamator =
			new edu.ucla.belief.approx.PropagationEngineGenerator();
		ibpIe = new edu.ucla.belief.approx.PropagationInferenceEngineImpl
			(bn,ibpIeSettings,dynamator);

		TreeMap instantiation = new TreeMap(new BpVarComparator(ibpIe));
		Map bestinst = new HashMap(bn.size());
		FiniteVariable var;
		edu.ucla.belief.Table cur;
		// iterate over each variable, and identify the instance that
		// maximized each individual variable
		for (Iterator it = bn.topologicalOrder().iterator(); it.hasNext(); ){
			var = (FiniteVariable)it.next();
			if ( !hasDeterministicCpt(var) )
				cur = ibpIe.conditional(var);
			else
				cur = var.getCPTShell().getCPT().shrink(bestinst);
			instantiation.put(var,orderedInstances(var,cur));
			bestinst.put(var,((List)instantiation.get(var)).get(0));
		}

		/*
		System.out.println("converged : " + ibpIe.converged());
		for ( Iterator it = instantiation.keySet().iterator(); it.hasNext(); ) {
			var = (FiniteVariable)it.next();
			System.out.println(var.getID() + " : " +
							   ibpIe.conditional(var) + " : " +
							   (List)instantiation.get(var));
		}
		*/

		return instantiation;
	}

	public static boolean hasDeterministicCpt(FiniteVariable var) {
	    double[] table = var.getCPTShell().getCPT().dataclone();
		for (int i = 0; i < table.length; i++)
			if ( table[i] != 0.0 && table[i] != 1.0 )
				return false;
		return true;
	}

	public static double evaluateInstantiation(BeliefNetwork bn,
											   Map instantiation) {
		List vars = bn.topologicalOrder();
		double logpr = 0.0;

		java.util.Map bestinst = new HashMap(instantiation.keySet().size());
		for (Iterator it = instantiation.keySet().iterator(); it.hasNext(); ) {
			FiniteVariable var = (FiniteVariable)it.next();
			List instances = (List)instantiation.get(var);
			bestinst.put(var,instances.get(0));
		}

		for ( Iterator it = vars.iterator(); it.hasNext(); ) {
			FiniteVariable var = (FiniteVariable)it.next();
			edu.ucla.belief.Table t =
				var.getCPTShell().getCPT().shrink(bestinst);
			logpr += Math.log( t.getCP(0) );
		}

		return logpr;
	}
}
