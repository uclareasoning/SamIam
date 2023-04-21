package il2.inf.rc;

import il2.util.*;
import il2.inf.structure.*;
import il2.model.*;

import java.util.*;
import java.math.*;

public class CachingScheme{
    DGraph dg;
    boolean upOnly;
    Set queryRoots;
    Set availableCacheNodes;
    double callsUnderFullCaching;
    BigInteger totalPossibleEntries;//long totalPossibleEntries;

    Set cachedNodes;
    double totalCalls;
    BigInteger cacheEntries;//long cacheEntries;

    boolean valid;


    public CachingScheme(DGraph dgraph,boolean cacheUpOnly){
	if(dgraph==null){
	    throw new IllegalArgumentException();
	}
	dg=dgraph;
	upOnly=cacheUpOnly;
	allocateQueryRoots();
        allocateAvailableCacheNodes();
	fullCaching();
	callsUnderFullCaching=recursiveCalls();
	totalPossibleEntries=allocatedCacheEntries();
	cachedNodes=new HashSet();
	valid=false;
    }


    private void validate(){
	totalCalls=countCalls();
	cacheEntries=countSpace();
	valid=true;
    }


    private void allocateQueryRoots(){
	if(upOnly){
	    UPair root=dg.root();
	    queryRoots=new HashSet();
	    queryRoots.add(new Pair(root.s1,root.s2));
	    queryRoots.add(new Pair(root.s2,root.s1));
	}else{
	    queryRoots=new HashSet(dg.tree().size());
	    for(int i=0;dg.isLeaf(i);i++){
		queryRoots.add(new Pair(dg.leafNeighbor(i).intValue(),i));
	    }
	}
    }


    private void allocateAvailableCacheNodes(){
	availableCacheNodes=new HashSet(3*dg.tree().size());
	for(Iterator iter=queryRoots.iterator();iter.hasNext();){
	    allocateAvailableCacheNodesFrom((Pair)iter.next());
	}
    }

    private void allocateAvailableCacheNodesFrom(Pair p){
	if(availableCacheNodes.contains(p)){
	    return;
	}else{
	    availableCacheNodes.add(p);
	    Integer[] children=dg.children(p);
	    for(int i=0;i<children.length;i++){
		allocateAvailableCacheNodesFrom(new Pair(children[i].intValue(),p.s1));
	    }
	}
    }


    public Set cachedNodes(){
	return Collections.unmodifiableSet(cachedNodes);
    }


    public double recursiveCalls(){
	validate();
	return totalCalls;
    }


    public BigInteger allocatedCacheEntries(){
	validate();
	return cacheEntries;
    }


    public DGraph dgraph(){
	return dg;
    }


    public double recursiveCallsFullCaching(){
	return callsUnderFullCaching;
    }


    public BigInteger cacheEntriesFullCaching(){
	return totalPossibleEntries;
    }


    private int cacheSize(Pair p){
	return dg.context(p).sizeInt();
    }


    public void fullCaching(){
	cachedNodes=new HashSet(availableCacheNodes);
	eliminateDeadCaches();
	valid=false;
    }

    private void eliminateDeadCaches(){
	Map calls=new HashMap(2*cachedNodes.size());
	for(Iterator iter=cachedNodes.iterator();iter.hasNext();){
	    Pair node=(Pair)iter.next();
	    if(dg.isLeaf(node.s1)){
		iter.remove();
	    }else{
		double c=getCalls(calls,node);
		if(c==dg.context(node).sizeDouble()){
		    iter.remove();
		}
	    }
	}
    }

    private double countCalls(){
	double total=0;
	HashMap calls=new HashMap(2*availableCacheNodes.size());
	for(Iterator iter=availableCacheNodes.iterator();iter.hasNext();){
	    total+=getCalls(calls,(Pair)iter.next());
	}
	return total;
    }

    /** keith cascio 020305 */
    private BigInteger countSpace(){
        BigInteger total = BigInteger.ZERO;
	for(Iterator iter=cachedNodes.iterator();iter.hasNext();){
	    Pair p=(Pair)iter.next();
	    Index ct=dg.context(p);
	    if(ct==null){
		System.err.println("Separators don't contain "+p);
		throw new IllegalStateException();
	    }else{
		total = total.add( ct.sizeBig() );//total+= ((long)ct.size());
	    }
	}
	return total;
    }


    private void updateCallsForSubtree(Map calls, Pair t){
	calls.put(t,new Double(computeLocalCalls(calls,t)));
	Integer[] children=dg.children(t);
	for(int i=0;i<children.length;i++){
	    updateCallsForSubtree(calls,new Pair(children[i].intValue(),t.s1));
	}
    }

    private double getCalls(Map calls,Pair t){
	if(!availableCacheNodes.contains(t)){
	    return 0;
	}
	Object obj=calls.get(t);
	if(obj!=null){
	    return ((Double)obj).doubleValue();
	}
	double c=computeLocalCalls(calls,t);
	calls.put(t,new Double(c));
	return c;
    }

    /**
     * Returns the number of times that rc is called on the dtree node "node"
     */
    private double computeLocalCalls(Map calls,Pair node){
	if(queryRoots.contains(node)){
	    return dg.context(node).sizeDouble();
	}
	Integer[] p=dg.parents(node);
	if(p.length==0){
	    return 0;
	}
	Pair p1=new Pair(node.s2,p[0].intValue());
	Pair p2=new Pair(node.s2,p[1].intValue());
	return parentCallContribution(calls,p1)
              +parentCallContribution(calls,p2);
    }

    private double parentCallContribution(Map calls,Pair parent){
	if(cachedNodes.contains(parent)){
     	    return dg.cutset(parent).sizeDouble()*dg.context(parent).sizeDouble();
	}else{
	    return getCalls(calls,parent)*dg.cutset(parent).sizeDouble();
	}
    }

    private double getCPC(Map cpc,Pair node){
	Object obj=cpc.get(node);
	if(obj!=null){
	    return ((Double)obj).doubleValue();
	}
	double c=computeLocalCPC(cpc,node);
	cpc.put(node,new Double(c));
	return c;
    }

    /**
     * Returns the number of recursive calls generated by a call to the node
     * including the call itself.
     */
    private double computeLocalCPC(Map cpc,Pair node){
	if(cachedNodes.contains(node)){
	    return 1;
	}
	Integer[] children=dg.children(node);
	if(children.length==0){
	    return 1;
	}
	Pair c1=new Pair(children[0].intValue(),node.s1);
	Pair c2=new Pair(children[1].intValue(),node.s1);
	return 1+dg.cutset(node).sizeDouble()*(getCPC(cpc,c1)+getCPC(cpc,c2));
    }

    public boolean allocateGreedily( long cachesRequested ){
        try{
            if( Thread.interrupted() ) throw new InterruptedException();
            cachedNodes=new HashSet(2*availableCacheNodes.size());
            Set candidates=new HashSet(availableCacheNodes);
            long availableSpace=cachesRequested;
            Map calls = new HashMap(2*availableCacheNodes.size());
            Map cpc = new HashMap(2*availableCacheNodes.size());
            Map scores = new HashMap(2*availableCacheNodes.size());
            while(availableSpace>0 && candidates.size()>0){
                if( Thread.interrupted() ) throw new InterruptedException();
                double bestScore=0;
                Pair bestNode=null;
                for(Iterator iter=candidates.iterator();iter.hasNext();){
                    Pair node=(Pair)iter.next();
                    if( availableSpace < dg.context(node).sizeLong() ){
                        iter.remove();
                        continue;
                    }
                    double score=getScore(scores,cpc,calls,node);
                    if( score == 0 ) iter.remove();
                    else if( score < 0 ){
                        //throw new IllegalStateException();
                        System.err.println("negative score");
                        score=Double.POSITIVE_INFINITY;
                    }
                    else if( score > bestScore ){
                        bestScore=score;
                        bestNode=node;
                    }
                }
                if(bestNode!=null){
                    cachedNodes.add(bestNode);
                    candidates.remove(bestNode);
                    invalidateScores(scores,calls,cpc,bestNode);
                    availableSpace -= dg.context(bestNode).sizeLong();
                }
            }
            return true;
        }catch( InterruptedException interruptedexception ){
            Thread.currentThread().interrupt();
            valid = false;
            cachedNodes = new HashSet();
            return false;
        }
    }

    private double getScore(Map scores,Map cpc, Map calls,Pair node){
	Object result=scores.get(node);
	if(result!=null){
	    return ((Double)result).doubleValue();
	}
	double score=computeLocalScore(cpc,calls,node);
	scores.put(node,new Double(score));
	return score;
    }


    private double computeLocalScore(Map cpc, Map calls,Pair node){
	Integer[] c=dg.children(node);
	if(c.length==0){
	    return 0;
	}
	double cls=getCalls(calls,node);
	double cxt=dg.context(node).sizeDouble();
	double cts=dg.cutset(node).sizeDouble();

	Pair c1=new Pair(c[0].intValue(),node.s1);
	Pair c2=new Pair(c[1].intValue(),node.s1);
	return cts*(cls/cxt-1)*(getCPC(cpc,c1)+getCPC(cpc,c2));
    }

    private void invalidateScores(Map scores,Map calls,Map cpc,Pair bestNode){
	Set descendants=descendants(bestNode);
	calls.keySet().removeAll(descendants);
	scores.keySet().removeAll(descendants);
	Set ancestors=ancestors(bestNode);
	cpc.keySet().removeAll(ancestors);
	scores.keySet().removeAll(ancestors);
	/*calls.clear();
	scores.clear();
	cpc.clear();*/
    }

    /**
     * Returns the descendants of the node (self excluded)
     */
    private Set descendants(Pair node){
	Set result=new HashSet(dg.tree().size());
	descendants(result,node);
	return result;
    }

    private void descendants(Set s,Pair node){
	Integer[] c=dg.children(node);
	for(int i=0;i<c.length;i++){
	    Pair ch=new Pair(c[i].intValue(),node.s1);
	    s.add(ch);
	    descendants(s,ch);
	}
    }

    /**
     * Returns the ancestors of the node (self included)
     */
    private Set ancestors(Pair node){
	Set result=new HashSet(dg.tree().size());
	ancestors(result,node);
	return result;
    }

    private void ancestors(Set s,Pair node){
	s.add(node);
	Integer[] p=dg.parents(node);
	for(int i=0;i<p.length;i++){
	    Pair par=new Pair(node.s2,p[i].intValue());
	    ancestors(s,par);
	}
    }

}
