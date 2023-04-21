package edu.ucla.belief.rc2.structure;

import java.util.*;
import java.io.*;
//{superfluous} import java.text.NumberFormat;

import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.Table;
import edu.ucla.belief.TableIndex;
import edu.ucla.structure.MappedList;

import edu.ucla.belief.rc2.caching.RC2CachingSchemeUtils;
import edu.ucla.belief.rc2.caching.RC2CachingScheme_Full;

import edu.ucla.belief.rc2.kb.KnowledgeBase;
import edu.ucla.belief.rc2.kb.MultiValuedCNF;
import edu.ucla.belief.rc2.kb.KnowledgeBaseImpl;

import edu.ucla.belief.rc2.kb.sat.KB_SAT;
import edu.ucla.belief.rc2.kb.KBMap;
import edu.ucla.belief.rc2.kb.Map_L;

/** This class represents Dtrees and Dgraphs.
 *
 * <p>setRoots may only be called once, future calls will cause an exception to be thrown.
 * <p>Computations may be done by calls to compute_*.
 * <p>Evidence can be changed through the following functions, however if you are using
 *    threads be careful: synchEvidWithBN, observe, unobserve
 * <p>CPTs can be changed so long as the variables and their indexing remain the same
 *    (i.e. the only change allowed is parameter modifications).  To do this, change the
 *    BeliefNetworks FiniteVariable.CPTShell and then call setCPT().
 *
 * @author David Allen
 */
final public class RC2 {
	/*This can be null, therefor it is recommended that any output be done through a call to outputInfo().*/
	public PrintWriter outputConsole = null;

	public long rcCallsCounter = 0;
	final protected ExclusiveMode exclusiveMode = new ExclusiveMode();

    final protected BeliefNetwork myBeliefNetwork;
    final MappedList vars;

	/** This is a very special array which is set multiple ways.
	 *  <p>If a KnowledgeBaseImpl is in use, it should only be set through rcKB.setInst().
	 *  <p>It contains evidence from all cutset iterators and possibly user evidence (based on rcFlags.placeUserEvidInInstArr)
	 *  <p>Its default state varies based on whether allowKB or not.
	 *     If no KB is allowed, it is to have all variables in state 0 (possibly having evidence in other states) and is ordered by RC2.vars.
	 *     If KB is allowed, it is to have all variables in state -1 (possibly having evidence on some) and is ordered by RC2.vars.
	 *  <p>If observing evidence the evidence indicator possibly needs to be told & possibly need to set evidHasChanged flag.
	 *  <p>Note: just because instantiation has a value doesn't mean evidence is set, it could be the initial state.
	 */
	final int instantiation[];
	final int userEvid[];
	final int instantiationDefaultValue;

	final RCFlags rcFlags;
	final RC2KB rcKB;

	/*
	 * rootNodes[i]: Nodes for Pe,PeLog,Marginal,MPE calculations (user supplies i)
	 * parDerNodes[i]: Nodes for partial derivative calculations (user supplies i)
	 */
	protected RC2Node rootNodes[] = null;
	protected RC2Node peRootNode = null;
	protected RC2Node parDerNodes[] = null;
	protected RC2Node allNodes[] = null; //if [i] is ancestor of [j] then i>j (children first). (includes those under peRootNode).
	protected RC2Node peNodes[] = null; //if [i] is ancestor of [j] then i>j (children first). (only includes nodes in Pe dtree).

	final public double scalar;

	private final static int ComputationType_None   = 0;
	private final static int ComputationType_Pe     = 1;//includes marginals and partial derivs
	private final static int ComputationType_MPE    = 3;
	private int cachesContainComputationType = ComputationType_None;

	private double computationStartTime_ms = 0;
	private double computationTotalTime_ms = 0;

	public final KBMap.Mapping map_kb;
	public KB_SAT kb_sat;


	/**Leaf nodes are updated anytime evidence is observed or retracted, however internal
	 * nodes only need to be updated immediately before a compuation.  And only if they
	 * are using RC2InstItrSkp which is when no !allowParDeriv && !allowKBinst.*/
	private boolean evidHasChanged = false;


	/**Stores the computation stats for this RC object for full caching
	 *  and under the current caching scheme.
	 *  <p>It is valid once a call to setRoots has been made.
	 */
	private final StoredComputationStats compStats = new StoredComputationStats();


	/** An RC2 object should be created before any nodes are created, as it has to be
	 *  passed into the node constructors.  Once the structure is complete, call
	 *  setRoots once to initialize the RC2 object for computations and evidence.
	 */
	public RC2(RCCreationParams param) {
		param.verifyValid();

		rcFlags = new RCFlags(param);
		outputConsole = param.outputConsole;
		scalar = param.scalar;
		myBeliefNetwork = param.bn;

		vars = new MappedList(myBeliefNetwork);

		instantiation = new int[vars.size()];

		/*If using the SAT engine, the only iterator which will work is the KBinst iterator, as this
		 * default value will cause problems with the others!
		 */
		if(rcFlags.allowKBinst || rcFlags.allowKBsat) {
			if(rcFlags.allowParDeriv) { throw new IllegalStateException();}
			instantiationDefaultValue = -1;
		}
		else { instantiationDefaultValue = 0;}

		Arrays.fill(instantiation, instantiationDefaultValue); //set all to instantiationDefaultValue (0 or -1)
		userEvid = new int[vars.size()];
		Arrays.fill(userEvid, -1); //set all to default state -1


		if(param.allowKBinst) {
			rcKB = new RC2KB(param.useKBinst);
		}
		else {
			rcKB = null;
		}

		if(param.allowKBsat>0) {
			MultiValuedCNF cnf = MultiValuedCNF.createFromBN(myBeliefNetwork, vars);

			switch(param.allowKBsat) {
				case 1:
					KBMap.Mapping tkb = null;
					try {
						tkb = Map_L.createLogicEncoding(cnf,File.createTempFile("cnf_", ".cnf", new File(".")), "Title", null/*fvToColOfClauses*/, true/*includeComments*/);
					}
					catch(IOException e) {
						System.err.println("Error Creating cnf file: " + e.getMessage());
					}
					map_kb = tkb;
				break;

				default:
					throw new IllegalArgumentException("Unknown allowKBsat flag: " + param.allowKBsat);
			}

			if(cnf.numVarsAppearingInCNF()==0) {
				kb_sat = null;
				System.out.println("WARNING: Network does not contain any determinism");
				outputInfo("WARNING: Network does not contain any determinism");
			}
			else {
				kb_sat = KB_SAT.createKB(map_kb);
				if(kb_sat == null) {
					throw new IllegalArgumentException("Could not create the SAT engine.");
				}
			}
		}
		else {
			map_kb = null;
			kb_sat = null;
		}

		outputInfo("RC2 created: " +
					(param.allowKBinst ? "" : "!" ) + "allowKBinst " +
					"allowKBsat=" + param.allowKBsat + " " +
					(param.allowPartialDerivatives ? "" : "!") + "allowPartialDerivatives " +
					(param.scalar>1 ? ("scalar="+param.scalar) : ""));
	}



	/**This will clean up all resources used by RC (currently the SAT engine).  It is okay to
	 *  call this as many times as you want.
	 * <p>Any future calls using kb_sat after this will cause problems!
	 */
	public void close() {
		if(kb_sat != null) {
			kb_sat.releaseKB();
			kb_sat = null; //disallow using it, although internal nodes still have a reference to it
		}
	}

	/**This may or may not be called, to be sure, should actually call finally.*/
	protected void finalize() {
		close();
	}



	/** Will set this objects root(s).  It may only be called once, any further calls to it
	 *  will cause an exception to be thrown.
	 *  <p>Copies of rts and pds are used by this RC object.
	 *  <p>User is responsible for calling the initialize functions on these nodes BEFORE calling this function.
	 *  <p>peNd is also allowed to be a member of rts, however it does not have to be.
	 *  <p>peNd will be used for Pe, Pe_log, and MPE calculations.
	 */
	public void setRoots(RC2Node peNd, RC2Node[] rts, RC2Node[] pds, CachingScheme cs) {
		if(rootNodes != null || peRootNode != null || parDerNodes != null || allNodes != null || peNodes != null) {
			throw new IllegalStateException("RC object already has roots set");
		}
		if(cs==null) { throw new IllegalArgumentException();}

		if(rts!=null) { rootNodes = (RC2Node[])rts.clone();}
		peRootNode = peNd;
		if(pds!=null && rcFlags.allowParDeriv) { parDerNodes = (RC2Node[])pds.clone();}

		{//init allNodes[]
			TreeSet orderedNodes = new TreeSet();
			TreeSet orderedPeNodes = new TreeSet();
			if(rootNodes != null) {
				for(int i=0; i<rootNodes.length; i++) {
					RC2Utils.addAllNodes(rootNodes[i],orderedNodes);
				}
			}
			if(parDerNodes != null) {
				for(int i=0; i<parDerNodes.length; i++) {
					RC2Utils.addAllNodes(parDerNodes[i],orderedNodes);
				}
			}
			if(peRootNode != null) {
				RC2Utils.addAllNodes(peRootNode,orderedNodes);
				RC2Utils.addAllNodes(peRootNode,orderedPeNodes);
			}
			allNodes = (RC2Node[])orderedNodes.toArray(new RC2Node[orderedNodes.size()]);
			peNodes = (RC2Node[])orderedPeNodes.toArray(new RC2Node[orderedPeNodes.size()]);
		}

		{//set caching scheme
			//Calculate for Full Caching
			RC2CachingScheme_Full csf = new RC2CachingScheme_Full();
			Collection cachedNodes = csf.getCachingScheme(this);
			double mem = RC2CachingSchemeUtils.expectedMemoryUsage(cachedNodes);
			double cls_a = RC2Utils.expectedRCCalls_All(this,cachedNodes);
			double cls_p = RC2Utils.expectedRCCalls_Pe(this,cachedNodes);
			compStats.fullCaching = new ComputationStats(csf.toString(), mem, cachedNodes.size(), cls_a, cls_p);

			if(cs instanceof RC2CachingScheme_Full) {
				setCachingScheme(cs.toString(),cachedNodes,compStats.fullCaching);
			}
			else {
				setCachingScheme(cs);
			}
		}

		outputInfo("RC2 initialized with " + allNodes.length + " total nodes");
	}



	public double compute_Pe_sat() {
		double ret = -1;
		if(kb_sat != null && map_kb != null && peRootNode != null) {
			if(scalar==1) {
				startComputation(ComputationType_Pe);
				ret = peRootNode.recCondSAT(0);
				endComputation();
			}
			else {
				startComputation(ComputationType_Pe);
				ret = peRootNode.recCondSATLog(0);
				endComputation();
			}
			outputInfo("RC2.compute_Pe_sat = " + ret + " and used " + rcCallsCounter + " calls and time: " + computationTotalTime_ms + " ms");
			outputInfo(compStats.currentCaching.toString("RC Stats: "));
		}
		else {
			outputInfo("Did not run compute_Pe_sat: \n" + kb_sat + "\n" + map_kb + "\n" + peRootNode + "\n");
		}
		return ret;
	}


	/**This will return the scaled version if scaling is enabled, the caller is responsible for converting it, as that conversion
	 *  could cause an underflow to occur.
	 */
	public double compute_Pe() {
		double ret = -1;
		if(rcFlags.allowKBsat) { throw new IllegalStateException("cannot use iterators when using SAT");} //TODO: disable this since iterators will have trouble with instantiationDefaultValue
		if(rcKB != null && !rcKB.kbConsistant) { return 0;}
		if(peRootNode!=null) {
			startComputation(ComputationType_Pe);

			if(rcFlags.allowParDeriv) {
				if(!rcFlags.allowKBinst && scalar==1.0) { //nokb, no scaling
					ret = peRootNode.recCondAll(0);
				}
				else if(!rcFlags.allowKBinst && scalar!=1.0) { //nokb, with scaling
					ret = peRootNode.recCondAllLog(0);
				}
				else {
					outputInfo("Since partial derivatives are turned on, you cannot use scaling or the KB.");
				}
			}
			else {
				if(!rcFlags.allowKBinst && scalar==1.0) { //nokb, no scaling
					ret = peRootNode.recCondSkp(0);
				}
				else if(!rcFlags.allowKBinst && scalar!=1.0) { //nokb, with scaling
					ret = peRootNode.recCondSkpLog(0);
				}
				else if(rcFlags.allowKBinst && scalar==1.0) { //kb, no scaling
					if(!rcKB.useKBflag()) {outputInfo("Since no KB is in use, RC would run faster if allowKBinst was turned off.");}
					ret = peRootNode.recCondKB(0);
				}
				else if(rcFlags.allowKBinst && scalar!=1.0) { //kb, with scaling
					if(!rcKB.useKBflag()) {outputInfo("Since no KB is in use, RC would run faster if allowKBinst was turned off.");}
					ret = peRootNode.recCondKBLog(0);
				}
			}
			endComputation();
		}
		outputInfo("RC2.compute_Pe = " + ret + " and used " + rcCallsCounter + " calls and time: " + computationTotalTime_ms + " ms");
		outputInfo(compStats.currentCaching.toString("RC Stats: "));
		return ret;
	}

	/**Does not run with scaling enabled.*/
	public double compute_MPEValue() {
		if(rcFlags.allowKBsat) { throw new IllegalStateException("cannot use iterators when using SAT");} //TODO: disable this since iterators will have trouble with instantiationDefaultValue
		if(rcKB != null && !rcKB.kbConsistant) { return 0;}
		double ret = -1;
		if(rcFlags.allowKBinst) {
			outputInfo("MPE is not currently supported when using the KB.");
		}
		else if(scalar != 1.0) {
			outputInfo("MPE is not currently supported when using scaling.");
		}
		else if(peRootNode!=null) {
			startComputation(ComputationType_MPE);
			ret = peRootNode.recCondMPE(0);
			endComputation();
		}
		outputInfo("RC2.compute_MPEValue = " + ret + " and used " + rcCallsCounter + " calls and time: " + computationTotalTime_ms + " ms");
		return ret;
	}

	/**Does not run with scaling enabled.*/
	/**If the root rt is a leaf node, it will return null.  It also cannot
	 *  be used when the KB is allowed.
	 */
	public Table compute_CutMarginal(int rt) {
		if(rcFlags.allowKBsat) { throw new IllegalStateException("cannot use iterators when using SAT");} //TODO: disable this since iterators will have trouble with instantiationDefaultValue
		if(rcKB != null && !rcKB.kbConsistant) { return null;} //TODO: return something better?
		Table ret = null;
		if(rcFlags.allowKBinst) {
			outputInfo("compute_CutMarginal is not currently supported when using the KB.");
		}
		else if(scalar != 1.0) {
			outputInfo("compute_CutMarginal is not currently supported when using scaling.");
		}
		else if(rootNodes!=null && rt>=0 && rt<rootNodes.length) {
			if(!rootNodes[rt].isLeaf()) {
				startComputation(ComputationType_Pe);
				RC2NodeInternal ndi = (RC2NodeInternal)rootNodes[rt];
				double data[] = ndi.recCondCutMar();
				TableIndex indx = ndi.getCutsetIndx();
				ret = new Table(indx,data);
				endComputation();
			}
			else {
				outputInfo("compute_CutMarginal cannot currently be used with singleton leaf nodes.");
			}
		}
		outputInfo("RC2.compute_CutMarginal used " + rcCallsCounter + " calls and time: " + computationTotalTime_ms + " ms");
		return ret;
	}

	public Table compute_ParDer(int rt) {
		if(rcFlags.allowKBsat) { throw new IllegalStateException("cannot use iterators when using SAT");} //TODO: disable this since iterators will have trouble with instantiationDefaultValue
		if(rcKB != null && !rcKB.kbConsistant) { return null;} //TODO: return something better?
		Table ret = null;
		if(rcFlags.allowKBinst) {
			outputInfo("compute_ParDer is not currently supported when using the KB.");
		}
		else if(scalar != 1.0) {
			outputInfo("compute_ParDer is not currently supported when using scaling.");
		}
		else if(parDerNodes!=null && rt>=0 && rt<parDerNodes.length && rcFlags.allowParDeriv) {
			startComputation(ComputationType_Pe);
			TableIndex indx = parDerNodes[rt].context().getTableIndex();
			double data[] = new double[indx.size()];
			for(int i=0; i<data.length; i++) {
				data[i] = parDerNodes[rt].recCondAll(i); //only works with recCondAll, since root cutset evid is never placed anywhere other than in i
			}
			ret = new Table(indx,data);
			endComputation();
		}
		outputInfo("RC2.compute_ParDer used " + rcCallsCounter + " calls and time: " + computationTotalTime_ms + " ms");
		return ret;
	}


	/**Attempt to do some preparation steps before computations are actually run.*/
	public void prePrepareToStartComputation() {
		if(evidHasChanged) { notifyInternalsEvidChg();}
		if(rcKB != null) {rcKB.createKBIfNecessary();}
	}

	protected void startComputation(int computationType) {
		flushCachesIfNecessary(computationType);
		if(evidHasChanged) { notifyInternalsEvidChg();}
		if(rcKB != null) {rcKB.createKBIfNecessary();}
		cachesContainComputationType = computationType;
		exclusiveMode.enter();
		rcCallsCounter = 0;

		//This should be the last item here, so timing is more accurate
		computationStartTime_ms = System.currentTimeMillis();
	}
	protected void endComputation() {
		//This should be the first item here, so timing is more accurate
		double computationEndTime_ms = System.currentTimeMillis();
		computationTotalTime_ms = computationEndTime_ms - computationStartTime_ms;

		exclusiveMode.done();
	}

	/**This function will set the caching in the RC2 object based on the
	 * caching scheme passed in, and will compute the current memory usage
	 * and expected RC calls based on the changes.
	 */
	public void setCachingScheme(CachingScheme cs) {
		if(cs == null) { System.err.println("ERROR: Caching Scheme was null."); return;}

		if(cs instanceof RC2CachingScheme_Full) {
			setCachingScheme(cs.toString(),cs.getCachingScheme(this),compStats.fullCaching);
		}
		else {
			setCachingScheme(cs.toString(),cs.getCachingScheme(this),null);
		}
	}

	/**This function will set the caching in the RC2 object based on the
	 * caching scheme passed in, and will set (or compute if stats is null)
	 * the current memory usage and expected RC calls based on the changes.
	 */
	private void setCachingScheme(String name, Collection cachedNodes, ComputationStats stats) {
		RC2CachingSchemeUtils.removeLargeCaches(this, cachedNodes);
		for(int i=0; i<allNodes.length; i++) {
			if(!allNodes[i].isLeaf()) {
				RC2NodeInternal ndi = (RC2NodeInternal)allNodes[i];
				if(cachedNodes.contains(ndi)) {
					ndi.setCaching(true);
				}
				else {
					ndi.setCaching(false);
				}
			}
		}

		if(stats!=null) {
			compStats.currentCaching = stats;
		}
		else {
			double mem = RC2CachingSchemeUtils.expectedMemoryUsage(cachedNodes);
			double cls_a = RC2Utils.expectedRCCalls_All(this,null);
			double cls_p = RC2Utils.expectedRCCalls_Pe(this,null);
			compStats.currentCaching = new ComputationStats(name,mem,cachedNodes.size(),cls_a,cls_p);
		}

		outputInfo("RC2 set caching scheme to " + name + " (" + cachedNodes.size() + " nodes)");
	}


	public List vars() { return Collections.unmodifiableList(vars);}

	public double getComputationTotalTime_ms() { return computationTotalTime_ms;}

	public ArrayList getElimOrder() {
		ArrayList ret = new ArrayList();
		getPeRootNode().getElimOrder(ret);
		return ret;
	}


	/**Can use this and getRCNode_All to iterate through nodes.*/
	public int getNumRCNodes_All() { return allNodes.length;}
	/**Can use this and getNumRCNodes_All to iterate through nodes.*/
	public RC2Node getRCNode_All(int indx) {
		if(allNodes!=null && indx>=0 && indx<allNodes.length) { return allNodes[indx];}
		else { return null;}
	}
	/**Can use this and getRCNode_Pe to iterate through nodes.*/
	public int getNumRCNodes_Pe() { return peNodes.length;}
	/**Can use this and getNumRCNodes_Pe to iterate through nodes.*/
	public RC2Node getRCNode_Pe(int indx) {
		if(peNodes!=null && indx>=0 && indx<peNodes.length) { return peNodes[indx];}
		else { return null;}
	}

	public RC2Node getPeRootNode() { return peRootNode;}
	public RC2Node[] getRootNodes() { return (RC2Node[])rootNodes.clone();}

	public StoredComputationStats compStats() { return compStats;}



	private void flushCachesIfNecessary(int computationType) {
		if(cachesContainComputationType == ComputationType_None) { return;}
		else if(cachesContainComputationType != computationType) {
			flushCaches();
		}
	}
	public void flushCaches() {
		exclusiveMode.enter();
		for(int i=0; i<allNodes.length; i++) {
			allNodes[i].clearLocalCacheAndReset();
		}
		cachesContainComputationType = ComputationType_None;
		exclusiveMode.done();
		outputInfo("RC2 flushed caches.");
	}

	/**Clears out any userEvidence on this RC object.*/
	protected void clearEvidence() {
		exclusiveMode.enter();
		Arrays.fill(instantiation,instantiationDefaultValue);
		Arrays.fill(userEvid,-1);
		for(int i=0; i<allNodes.length; i++) {
			if(allNodes[i].isLeaf()) {
				((RC2Node.RC2LeafEventHandler)allNodes[i]).unobserveAll();
			}
			else {
				allNodes[i].clearLocalCacheAndReset();
			}
		}
		exclusiveMode.done();
		if(rcKB != null) {rcKB.clearEvidence();}
		if(!rcFlags.allowParDeriv && !rcFlags.allowKBinst) {evidHasChanged = true;}//tell RC2InstItrSkp
	}
	protected void addAllEvidFromBN() {
		Map evid = myBeliefNetwork.getEvidenceController().evidence();
		for(Iterator itr = evid.keySet().iterator(); itr.hasNext();) {
			FiniteVariable fv = (FiniteVariable)itr.next();
			observe(fv, evid.get(fv));
		}
	}
	public void synchEvidWithBN() {
		clearEvidence();
		addAllEvidFromBN();
	}


	final public void observe(FiniteVariable var, Object value) {
		if(var == null) { return;}
		if(value == null) { unobserve(var); return;}
		exclusiveMode.enter();

		int indx = vars.indexOf(var);
		int val = var.index(value);
		if(indx < 0) {
			exclusiveMode.done();
			throw new IllegalArgumentException("Variable not found: " + var);
		}
		if(val < 0) {
			exclusiveMode.done();
			throw new IllegalArgumentException("State not found: " + var + " - " + value);
		}

		int oldvalue = userEvid[indx];
		if(oldvalue != val) {
			userEvid[indx]=val;

			if(!rcFlags.allowParDeriv && !rcFlags.allowKBinst) { evidHasChanged = true;}//tell RC2InstItrSkp

			if(rcFlags.placeUserEvidInInstArr) {

				if(rcKB == null || !rcKB.useKBflag()) {
					instantiation[indx] = val;
				}
				else { //using KB (although it may not be created yet)
					if(oldvalue == -1) {//no previous evidence, add it now
						if( !rcKB.setInst(indx,val)) { //will update inst array and update KB
							rcKB.kbConsistant = false;
							outputInfo("RC2 observed evidence was inconsistent.");
							exclusiveMode.done();
//							rcKB.removeKB();
						}
					}
					else { //had previous evidence, need to reset KB
						exclusiveMode.done();
						synchEvidWithBN();
						return;
					}
				}
			}
			notifyLeafsEvidChg(indx,val);
		}
		exclusiveMode.done();
	}

	final public void unobserve(FiniteVariable var) {
		exclusiveMode.enter();

		int indx = vars.indexOf(var);
		if(indx < 0) {
			exclusiveMode.done();
			throw new IllegalArgumentException("Variable not found: " + var);
		}

		if(userEvid[indx] != -1) {
			userEvid[indx]=-1;

			if(!rcFlags.allowParDeriv && !rcFlags.allowKBinst) { evidHasChanged = true;} //tell RC2InstItrSkp

			if(rcFlags.placeUserEvidInInstArr) {

				if(rcKB == null || !rcKB.useKBflag()) {
					instantiation[indx] = -1;
				}
				else { //using KB (although it may not be created yet)
					exclusiveMode.done();
					synchEvidWithBN(); //cannot simply retract, because don't know what order things were added in
					return;
				}
			}
			notifyLeafsEvidChg(indx,-1);
		}
		exclusiveMode.done();
	}

	/**Call this function after updating the CPTShell object for a FiniteVariable,
	 * however, RC2 can only handle parameter changes, the variables and their
	 * indexing must remain the same.
	 */
	final public void setCPT(FiniteVariable var, boolean resetDeterminism) {
		exclusiveMode.enter();
		int indx = vars.indexOf(var);
		notifyLeafsCPT(indx);
		exclusiveMode.done();
		//handle KB
		if(rcKB != null && resetDeterminism) {
			boolean tmpUseKB = rcKB.useKBflag();
			rcKB.removeKB(); //clear it out, as new CPT may have different determinism
			if(tmpUseKB) { rcKB.useKB();}
		}
		if(rcFlags.allowKBsat && resetDeterminism) 	{
			throw new UnsupportedOperationException("setCPT with sat: resetDeterminism=true");//TODO
		}
	}

	final private void notifyLeafsEvidChg(int indxChanged, int value) {
		for(int i=0; i<allNodes.length; i++) {
			if(allNodes[i].isLeaf()) {
				((RC2Node.RC2LeafEventHandler)allNodes[i]).observe(indxChanged, value);
			}
		}
	}
	final private void notifyLeafsCPT(int indxChanged) {
		for(int i=0; i<allNodes.length; i++) {
			if(allNodes[i].isLeaf()) {
				((RC2Node.RC2LeafEventHandler)allNodes[i]).setCPT(indxChanged);
			}
		}
	}

	final private void notifyInternalsEvidChg() {

		if(!rcFlags.allowParDeriv && !rcFlags.allowKBinst) {

			for(int i=0; i<allNodes.length; i++) {
				if(!allNodes[i].isLeaf()) {
					//tell iterator that evidence changed since the last update
					((RC2InstItrSkp)((RC2NodeInternal)allNodes[i]).getCutsetIterator()).evidenceChanged();
				}
			}
			evidHasChanged = false;
		}
		else {
			System.err.println("notifyInternalsEvidChg was called, but either ParDer or KB are being allowed.");
		}
	}







    //HELPER FUNCTIONS

	public final void outputInfo(String str) {
		if(outputConsole != null) { outputConsole.println(str);}
	}





	//MISC CLASSES

	public interface CachingScheme {
		public String toString();
		public Collection getCachingScheme(RC2 rc);
	}


	final static public class StoredComputationStats {
		/*Never changed after setRoots.*/
		ComputationStats fullCaching = null;
		/*Changed after setCachigScheme.*/
		ComputationStats currentCaching = null;

		public ComputationStats fullCaching() { return fullCaching;}
		public ComputationStats currentCaching() { return currentCaching;}

		public String toString() {
			if(fullCaching!=null) {
				return "Computation Stats for RC:\n" + fullCaching + "\n(Currrently set) " + currentCaching;
			}
			else { return "";}
		}
	}


	final static public class ComputationStats {
		String csName = null;
		double memUsed = -1;
		int numNodesCached = -1;
		double rcCalls_all = -1;
		double rcCalls_pe = -1;

		public ComputationStats(String nm, double mem, int numNodes, double cls_all, double cls_pe) {
			csName = nm;
			memUsed = mem;
			numNodesCached = numNodes;
			rcCalls_all = cls_all;
			rcCalls_pe = cls_pe;
		}

		public double memUsed() { return memUsed;}
		public double memUsedMB() { return memUsed * 8.0 / 1048576.0;}
		public int numNodesCached() { return numNodesCached;}
		public double rcCalls_all() { return rcCalls_all;}
		public double rcCalls_pe() { return rcCalls_pe;}

		public String toString_all() {
			return csName + " expects " + rcCalls_all + " calls and " + memUsed + " cache entries using " + numNodesCached + " nodes";
		}
		public String toString_pe() {
			return csName + " expects " + rcCalls_pe + " calls and " + memUsed + " cache entries using " + numNodesCached + " nodes (mem and nodes are for total structure)";
		}
		public String toString() { return toString("");}
		public String toString(String prefix) {
			return prefix + csName + " expects\n" +
					prefix + " mem: " + memUsed + " (" + memUsedMB() + " MB) " + " cache entries using " + numNodesCached + " nodes on total structure\n" +
					prefix + " all: " + rcCalls_all + " calls\n" +
					prefix + " pe:  " + rcCalls_pe  + " calls";
		}
	}



	final static private class ExclusiveMode {
		boolean inUse = false;

		ExclusiveMode() {}

		synchronized void enter() {
			if(inUse) { throw new IllegalStateException("Already in use");}
			inUse = true;
		}
		synchronized void done() {
			inUse = false;
		}
	}


	/*TODO:Document this class.*/
	final static public class RCCreationParams {

		public boolean useKBinst = false;
		public boolean allowKBinst = false;
		public int allowKBsat = 0;
		public boolean allowPartialDerivatives = false;
		public BeliefNetwork bn;
		public PrintWriter outputConsole = null;
		public double scalar = 1.0;

		public RCCreationParams(BeliefNetwork bn) {
			this.bn = bn;
		}

		void verifyValid() {
			if(bn==null) { throw new IllegalArgumentException("RCCreationParams requires a valid belief network.");}
			//Cannot allowPartialDerivatives with either of the KBs, because iterator will not work with instantiationDefaultValue
			if(allowPartialDerivatives) {
				if(allowKBinst || allowKBsat>0) {throw new IllegalArgumentException("RCCreationParams cannot do partial derivatives with a KB.");}
			}
			if(scalar <= 0) { throw new IllegalArgumentException("Illegal scalar value, must be > 0.");}
			if(useKBinst && !allowKBinst) { throw new IllegalArgumentException("cannot useKBinst if not allowed");}
		}
	}//end class RCCreationParams



	final static class RCFlags {
		final public boolean allowKBinst;
		final public boolean allowKBsat;
		final public boolean allowParDeriv;
		final public boolean placeUserEvidInInstArr; //always opposite of allowParDeriv since they disallow evid on inst array

		public RCFlags(RCCreationParams par) {
			allowKBinst = par.allowKBinst;
			allowKBsat = par.allowKBsat>0;
			allowParDeriv = par.allowPartialDerivatives;
			placeUserEvidInInstArr = !allowParDeriv;

			//Cannot allowPartialDerivatives with either of the KBs, because iterator will not work with instantiationDefaultValue
			if(allowParDeriv && (allowKBinst || allowKBsat)) { throw new IllegalStateException("cannot compute partial derivatives with a KB");}
		}
	}





	/**This class deals with the usage of a KnowledgeBase with RC.*/
	final class RC2KB implements KnowledgeBase.KnowledgeBaseListener {


		/*A KnowledgeBase may be requested by using the function useKB, which will set
		 *  a flag to create a KnowledgeBase as necessary.
		 *Once created it can be removed (removeKB()), or it can be suspended
		 *  although this should be used with caution, as its possible to get the
		 *  KnowledgeBase out of synch with the evidence.
		 */

		private boolean useKBflag;//allow user to request KB, but not create it immediately (only valid if allowKBinst).

		private KnowledgeBase kb = null;
		private KnowledgeBase suspendedKB = null; //use with caution

		private int kbDefault=0; //where to retract kb to if remove all evidence (may not be 0 if learned info by itself)
		private int kbRetract[] = null;
		private boolean kbCallAssert[] = null;

		boolean kbConsistant = true;


		public boolean useKBflag() { return useKBflag;}

		public RC2KB(boolean useKBflag) {
			if(useKBflag) {useKB();}
		}

		final void clearEvidence() {
			if(kb != null) {
				exclusiveMode.enter();
				kb.retract(kbDefault);
				kbConsistant = true;
				exclusiveMode.done();
			}
			if(suspendedKB != null) {
				System.err.println("WARNING: RC2: Evidence updated while a KB was suspended.");
				outputInfo("WARNING: RC2: Evidence updated while a KB was suspended.");
			}
		}

		int numPossibleStates(FiniteVariable fv) {
			if(kb!=null) { return kb.numPossibleStates(fv);}
			else return Integer.MIN_VALUE;
		}

		/** When using KnowledgeBase, be careful about how algorithms will react to KB making changes.
		 *
		 *  <p>Should be used very cautiously.  In some cases, only iterators should call this
		 *     as no other evidence is supposed to be on the instantiation array and this doesn't
		 *     notify iterators about the evidence change.
		 *  <p>Can only fail if using the KB, otherwise it only sets the inst array.
		 *  <p>The KB contains evidence from the user (since in order to use KB then allowParDeriv must
		 *     be false implying rcFlags.placeUserEvidInInstArr = true;
		 *
		 *  @returns false if KB failed to set the value.
		 */
		final public boolean setInst(int indx, int newvalue) {
			int oldvalue = instantiation[indx];
			instantiation[indx] = newvalue; //set the array
			if(kb != null && kbCallAssert[indx]) { //if using KB
				if(newvalue == -1) { //removing evidence
					int s = kbRetract[indx];
					kbRetract[indx] = -1;
					if(s >= 0) {
						kb.retract(s);
					}
				}
				else { //setting evidence
					int ret = kb.assertPositive(indx, newvalue);
					if(ret == KnowledgeBase.KB_UNSATISFIABLE) {
						instantiation[indx] = oldvalue;
						return false;
					}
					kbRetract[indx] = ret;
				}
			}
			return true;
		}

		public void useKB() {
			if(!rcFlags.allowKBinst) { throw new IllegalStateException("Cannot call RC2.useKB if allowKBinst was set to false");}

			if(kb!=null || suspendedKB!=null) {
				System.err.println("RC2KB.useKB() called, however one already existed.");
				outputInfo("RC2KB.useKB() called, however one already existed.");
			}
			else {
				exclusiveMode.enter(); //cannot call while something else is in critical section (most likely computation)
				useKBflag = true; //set the flag (a KB may already exist for it), but if not startComputation will create it before running
//				kb = null; //clear any previous KBs (really should not have been called)
				kbConsistant = true;
//				suspendedKB = null; //clear any previously saved KBs
				exclusiveMode.done();
			}
		}

		public void removeKB() {
			exclusiveMode.enter(); //cannot call while something else is in critical section (most likely computation)
			if(useKBflag() || kb!=null || suspendedKB!=null) {
				useKBflag = false;
				kb = null;
				suspendedKB = null;
				kbConsistant = true;
				exclusiveMode.done();
				synchEvidWithBN();//reset evidence on RC, as KB may have put some there which is no longer "good"
			}
			else {
				exclusiveMode.done();
			}
		}

		/**Returns true unless a KnowledgeBase was unsatisfiable and removed.*/
		public boolean createKBIfNecessary() {
			if(rcFlags.allowKBinst && useKBflag()) {
				exclusiveMode.enter(); //cannot call while something else is in critical section (most likely computation)
				suspendedKB = null; //clear any previous one (should not happen as suspend turns off useKBflag)
				kbConsistant = true;

				if(kbRetract==null || kbRetract.length!=vars.size()) {
					kbRetract = new int[vars.size()];
				}

				Arrays.fill(kbRetract, -1);


				boolean fail = false;
				MultiValuedCNF mvcnf = MultiValuedCNF.createFromBN(myBeliefNetwork, vars);
				fail = fail || mvcnf.unsatisfiable();

				if(!fail) {
					kb = KnowledgeBaseImpl.createKB(vars, mvcnf, this);

					fail = fail || (kb==null);

					if(!fail) {

						kbDefault = kb.currentState();
						kbCallAssert = mvcnf.theseVarsAppearInCNF();

						//if evidence is stored in instantiation array, add it to KB
						// otherwise, cannot add this as may be interested in partial derivatives.
						if(rcFlags.placeUserEvidInInstArr) {
							for(int i=0; i<instantiation.length; i++) {
								if(instantiation[i] >= 0 && kbCallAssert[i]) {
									if( kb.assertPositive(i,instantiation[i]) == KnowledgeBase.KB_UNSATISFIABLE) {
										System.err.println("WARNING: The evidence on the network is inconsistent.");
										outputInfo("WARNING: The evidence on the network is inconsistent.");
										kbConsistant = false;
										exclusiveMode.done();
//										removeKB();
										return false;
									}
								}
							}
						}
						outputInfo("RC2 created a KnowledgeBase with " + kb.numClauses() + " clauses and " + kb.numLiterals() + " literals");
						exclusiveMode.done();
					}
				}
				if(fail) {
					System.err.println("WARNING: No KB created.");
					outputInfo("WARNING: No KB created.");
					exclusiveMode.done();
					removeKB();
					kbConsistant = false;
					return false;
				}
			}
			return true;
		}

		/**Warning: This function should not really be public and should not be called from outside
		 *  of the package unless you are sure it will not lead to computation errors!
		 */
		public void suspendKB() {
			suspendedKB = kb;
			kb = null; //remove current
			useKBflag = false; //don't create any new ones
		}
		/**Warning: This function should not really be public and should not be called from outside
		 *  of the package unless you are sure it will not lead to computation errors!
		 */
		public void resumeKB() {
			if(suspendedKB != null) {
				kb = suspendedKB;
				useKBflag = true;
				suspendedKB = null;
			}
			else {
				System.err.println("RC2: Called resumeKB, but nothing was suspended.");
				outputInfo("RC2: Called resumeKB, but nothing was suspended.");
				removeKB();
			}
		}


		public void assertLearnedPositive(int fv, int state) {instantiation[fv] = state;}
		public void assertUnLearnedPositive(int fv) {instantiation[fv] = -1;}

	}//end class RC2KB


}//end class RC2
