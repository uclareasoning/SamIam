package il2.inf.map;

import edu.ucla.util.JVMProfiler;
import il2.bridge.Converter;
import il2.util.*;
import java.util.*;
import il2.model.*;

/**
 *
 * @author  jdpark, Mark Chavira
 */
public class MapSearch {

    //======================
    // Begin code added by Mark Chavira
    //======================

    /**
     * The number of search nodes visited.
     */

    private long numSearchNodesVisited = 0;

    /**
     * The seed may be enterd into the answer twice, and so we save it here,
     * so that it will be easier to eliminate the duplicate.
     */

    private IntMap seed;

    /**
     * The user-specified slop value.  See the description for computeMAP ()
     * for more information.
     */

    private double slop;

    /**
     * The heap which stores the results.  Using a min-heap makes it easy to
     * add and remove map results as necessary.
     */

    private edu.ucla.structure.Heap heap;

    /**
     * The probability of evidence.
     */

    private double probOfEvidence;

    /**
     * The MapResults.
     */

    java.util.ArrayList results;

    /**
     * The time to stop the search.
     */

    long stopTime;

    /**
     * Returns whether a path with the given upper bound should be taken.
     *
     * @param ub the given upper bound.
     * @return whether the path should be taken.
     */

    private boolean consider (double ub) {
      if (ub <= 0.0) {
        return false;
      }
      if (slop < 0.0) {
        return ub > bestScore;
      }
      return ub >= bestScore - slop;
    }

    /**
     * Runs the map search algorithm and returns a list of MapResults, each
     * describing an instantiation of the map variables, sorted from max
     * probability to min probability.  The user specifies a value slop.  Let
     * p be the probability of the highest probability instantiation of the
     * map variables found.  If slop < 0, then the method returns the first
     * found instantiation with probability p.  Otherwise, the method returns
     * all found instantiations with probability >= p - slop.
     */

    public static MapInfo computeMAP (
     Collection potentials, IntSet mapvars, IntList order,
     double timeAllowed, double promotionWidthBound, double slop) {

      // First, perform the search.

      long iStart = System.currentTimeMillis ();
      long istart_cpu_ms = JVMProfiler.getCurrentThreadCpuTimeMS();
      MapSearch ms =
        new MapSearch(
          potentials, mapvars, order, timeAllowed, promotionWidthBound,
          slop);
      long istop_cpu_ms = JVMProfiler.getCurrentThreadCpuTimeMS();
      long iStop = System.currentTimeMillis ();
      long sStart = iStop;
      long sstart_cpu_ms = JVMProfiler.getCurrentThreadCpuTimeMS();
      ms.hardSearch();
      long sstop_cpu_ms = JVMProfiler.getCurrentThreadCpuTimeMS();
      long sStop = System.currentTimeMillis ();

      long iElapsed = iStop - iStart;
      long iProfiled = istop_cpu_ms - istart_cpu_ms;
      long sElapsed = sStop - sStart;
      long sProfiled = sstop_cpu_ms - sstart_cpu_ms;

      return
        new MapInfo (
          ms.results, iElapsed, iProfiled, sElapsed, sProfiled, !ms.timeExpired,
          ms.probOfEvidence, ms.numSearchNodesVisited);

    }

    /**
     * Found a new instantiation.
     *
     */

    private void newInstantiation (double score) {
      long foundTime = System.currentTimeMillis ();
      IntMap instance = new IntMap (currentInstance);
      if (score > bestScore) {
        bestScore = score;
        bestInstance = instance;
        while (!heap.isEmpty () && !consider (-heap.maxScore ())) {
          heap.extractMax ();
        }
      }
      heap.insert (new MapResult (instance, score, foundTime), -score);
    }

    /**
     * 2004-09-28: Changed result of map query from array list of map results
     * to this object.
     */

    public static class MapInfo {
      public java.util.ArrayList results;  // MapResults
      public long pruneDurationMillisElapsed = (long)-1;
      public long pruneDurationMillisProfiled = (long)-1;
      public long initDurationMillisElapsed = (long)-1;
      public long initDurationMillisProfiled = (long)-1;
      public long searchDurationMillisElapsed = (long)-1;
      public long searchDurationMillisProfiled = (long)-1;
      //public long initStartTime;
      //public long initStopTime;
      //public long searchStartTime;
      //public long searchStopTime;
      public boolean finished;
      public double probOfEvidence;
      public long numSearchNodesVisited;
      public MapInfo (
       java.util.ArrayList r, long iElapsed, long iProfiled, long sElapsed,
       long sProfiled, boolean f, double pe, long numSearchNodesVisited) {
         results = r;
         initDurationMillisElapsed=iElapsed;
         initDurationMillisProfiled=iProfiled;
         searchDurationMillisElapsed=sElapsed;
         searchDurationMillisProfiled=sProfiled;
         finished=f;
         probOfEvidence=pe;
         this.numSearchNodesVisited = numSearchNodesVisited;
      }
    }

    //======================
    // End code added by Mark Chavira 2004-06-17
    //======================

    MapJoinTree jt;
    IntMap currentInstance;
    IntMap bestInstance;
    IntSet candidates;
    IntList selectedVars;
    IntList selectedVarsProgress;
    double bestScore;
    boolean timeExpired;

    public static MapInfo computeMAP(Collection potentials,IntSet mapvars,IntList order,double timeAllowed,double promotionWidthBound){
      return computeMAP(potentials,mapvars,order,timeAllowed,promotionWidthBound,-1);
    }

    private MapSearch(Collection potentials,IntSet mapvars,IntList initialOrder,double timeAllowed,double promotionWidthBound,double slop){

        this.stopTime=System.currentTimeMillis()+(long)(timeAllowed*1000);

        // 2004-08-27 : Change to use less memory.  Before, created search
        // jointree, then seed jointree, then find seed, then release seed
        // jointree.  Now create seed jointree, find seed, release seed
        // jointree, create search jointree.

        Table[] cpts=new Table[potentials.size()];
        potentials.toArray(cpts);
        MapInitializer.MapResult mr=MapInitializer.initial(cpts, mapvars, initialOrder);

        this.slop = slop;
        this.heap = new edu.ucla.structure.Heap ();

        // 2004-08-27 : Change to use less memory.  Before, created prob (e)
        // jointree then search jointree then release prob (e) jointree.  Now
        // create prob (e) jointree, then release prob (e) jointree, then
        // create search jointree.

        jt=MapOrderMaker.mapJoinTree(potentials, new IntSet (), initialOrder, promotionWidthBound);
        probOfEvidence= jt.getValue ();
        jt = null;

        jt=MapOrderMaker.mapJoinTree(potentials, mapvars, initialOrder,promotionWidthBound);

        bestInstance=null;
        bestScore=-1;
        candidates=new IntSet(mapvars);
        selectedVars=new IntList(mapvars.size());
        selectedVarsProgress=new IntList(mapvars.size());
        currentInstance = mr.result;
        seed = new IntMap (currentInstance);
        newInstantiation (mr.score);
        currentInstance=new IntMap(mapvars.size());

    }


    private class ChangeElement{
        IntList modifiedVars;
        double[][] newLikelihoods;
        double[][] oldLikelihoods;
        boolean failed;
        IntMap fixedVars;
        int candidate;
        private ChangeElement(IntList mv,double[][] newLiks,boolean f,IntMap fix,int cand){
            modifiedVars=mv;
            newLikelihoods=newLiks;
            failed=f;
            fixedVars=fix;
            candidate=cand;
            oldLikelihoods=new double[mv.size()][];
            for(int i=0;i<mv.size();i++){
                oldLikelihoods[i]=(double[])jt.getLikelihood(mv.get(i)).clone();
            }
        }
    }

    private ChangeElement getNext(){
        double[][] newLikelihoods=new double[candidates.size()][];
        IntList modifiedVars=new IntList();
        boolean failed=false;
        IntMap fixedVars=new IntMap();
        double highestScore=0;
        int highestScoringCandidate=-1;
        for(int i=0;i<candidates.size();i++){
            int var=candidates.get(i);
            double[] vals=jt.getIntoMessage(var);
            double[] settings=(double[])jt.getLikelihood(var).clone();
            boolean changed=false;
            int goodCount=0;
            int agoodvalue=0;
            double localHighest=0;
            double incomingTotal=0;
            for(int j=0;j<vals.length;j++){
                if(settings[j]!=0){
                    if(consider (vals[j])) {
                        goodCount++;
                        agoodvalue=j;
                        incomingTotal+=vals[j];
                        if(vals[j]>localHighest){
                            localHighest=vals[j];
                        }
                    }else{
                        settings[j]=0;
                        changed=true;
                    }
                }
            }
            if(goodCount==0){  //The upper bound for every value of this variable is below the best found, so no solution below here is possible.
                failed=true;
                break;
            }
            if(goodCount==1){
                fixedVars.put(var, agoodvalue);
            }
            if(goodCount>0 && changed){
                newLikelihoods[modifiedVars.size()]=settings;
                modifiedVars.add(var);
            }
            if(goodCount>1){
                double finalScore;

                finalScore=localHighest/incomingTotal;
                if(finalScore>highestScore){
                    highestScoringCandidate=var;
                    highestScore=settings.length*localHighest/incomingTotal;
                }
            }
        }
        return new ChangeElement(modifiedVars,newLikelihoods,failed,fixedVars,highestScoringCandidate);
    }

    private void hardSearch(){

      // Perform the search.

      try{
          timeExpired=false;
          search();
          stopTime=System.currentTimeMillis();
      }catch(TimeExpiredException tee){
          timeExpired=true;
      }

      // Now, get all instances in the heap and add them to a list in reverse
      // order.

      results = new java.util.ArrayList ();
      while (!heap.isEmpty ()) {
        results.add (heap.extractMax ().element ());
      }
      java.util.Collections.reverse (results);

      // Now, remove any duplicate seed.

      MapResult first = null;
      int firstIndex = -1;
      for (int i = 0; i < results.size (); i++) {
        MapResult r = (MapResult)results.get (i);
        if (r.result.equals (seed)) {
          if (firstIndex == -1) {
            firstIndex = i;
            first = r;
          } else {
            results.remove (first.foundTime < r.foundTime ? i : firstIndex);
            break;
          }
        }
      }

    }

    private static class TimeExpiredException extends Exception{}


    private void search() throws TimeExpiredException{
        if(System.currentTimeMillis()>=stopTime){
            throw new TimeExpiredException();
        }
++numSearchNodesVisited;
        double score=jt.getValue();
        if(!consider (score)) {
            return;
        }
        if(candidates.size()==0){
            newInstantiation (score);
            return;
        }
        ChangeElement ce=getNext();
        if(ce.failed){
            return;
        }
        for(int i=0;i<ce.oldLikelihoods.length;i++){
            jt.setLikelihood(ce.modifiedVars.get(i),ce.newLikelihoods[i]);
        }
        for(int i=0;i<ce.fixedVars.size();i++){
            int var=ce.fixedVars.keys().get(i);
            currentInstance.put(var,ce.fixedVars.get(var));
            candidates.remove(var);
            selectedVars.add(jt.domain().size(var));
            selectedVarsProgress.add(jt.domain().size(var));
        }
        if(candidates.size()==0){
            score=jt.getValue();
            if(consider (score)) {
                newInstantiation (score);
            }
        }else{
            int var=ce.candidate;
            candidates.remove(var);
            double[] hscores=(double[])jt.getIntoMessage(var).clone();
            int[] order=ArrayUtils.reversed(ArrayUtils.sortedInds(hscores));
            double[] oldLik=(double[])jt.getLikelihood(var).clone();
            int ind=selectedVars.size();
            selectedVars.add(jt.domain().size(var));
            selectedVarsProgress.add(jt.domain().size(var));
            for(int i=0;i<order.length;i++){
                selectedVarsProgress.set(ind, i+1);
                if(consider (hscores[order[i]])){
                    currentInstance.put(var,order[i]);
                    jt.setLikelihood(var,lik(var,order[i]));
                    search();
                }
            }
            jt.setLikelihood(var,oldLik);
            currentInstance.remove(var);
            candidates.add(var);
            selectedVars.removeLast();
            selectedVarsProgress.removeLast();
            // clean up candidate
        }
        for(int i=0;i<ce.fixedVars.size();i++){
            int fv=ce.fixedVars.keys().get(i);
            currentInstance.remove(fv);
            candidates.add(fv);
            selectedVars.removeLast();
            selectedVarsProgress.removeLast();
        }
        for(int i=0;i<ce.oldLikelihoods.length;i++){
            jt.setLikelihood(ce.modifiedVars.get(i), ce.oldLikelihoods[i]);
        }
    }

    private void printCurrent(){
        for(int i=0;i<selectedVars.size();i++){
            System.err.print(selectedVarsProgress.get(i)+"/"+selectedVars.get(i)+" ");
        }
        System.err.println("");
    }
    private double[] lik(int var,int val){
        double[] result=new double[jt.domain().size(var)];
        result[val]=1;
        return result;
    }

	// An instantiation and a probability.
	public static class MapResult
	{
		public IntMap result;
		public final double score;
		public final long foundTime;
		private Map myInstatiationConverted;

		private MapResult(IntMap r,double s,long foTime){
			result=r;
			score=s;
			foundTime=foTime;
		}

		/** @author Keith Cascio @since 092804 */
		public Map getConvertedInstatiation( Factorizer fact, Converter verter ){
			if( myInstatiationConverted == null ){
				if( (verter == null) || (result == null) ) throw new IllegalStateException();
				IntMap deFactorizedResult = result;
				if( fact != null ) deFactorizedResult = fact.toOriginal( deFactorizedResult );
				myInstatiationConverted = verter.convert( deFactorizedResult );
			}
			result = null;
			return myInstatiationConverted;
		}

		/** @author Keith Cascio @since 092804 */
		public Map getConvertedInstatiation(){
			return myInstatiationConverted;
		}

		public String toString(){
			StringBuffer sb=new StringBuffer(400);
			sb.append("instance:");
			sb.append(result);
			sb.append('\n');
			sb.append("score: "+score);
			sb.append('\n');
			return sb.toString();
		}
	}
}
