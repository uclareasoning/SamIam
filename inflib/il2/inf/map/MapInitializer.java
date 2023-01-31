package il2.inf.map;
import il2.util.*;
import il2.model.*;

public class MapInitializer{

    IntMap solution;
    double best=0;

    int computationsToFind=0;
    int computationCount=0;
    int maximumComputations;
    boolean allowedToFinish=true;
    IntSet[] varTableLocations;
    Table[] currentTables;
    IntSet remaining;
    IntMap currentInst;
    JoinTreeInference inf;

    private MapInitializer(Table[] cpts,IntSet mapVars,IntList order){
	currentTables=(Table[])cpts.clone();
	remaining=new IntSet(mapVars);
	currentInst=new IntMap(remaining.size());
	computeVarTableLocations();
        //EliminationOrders.Record rec=EliminationOrders.minFill(java.util.Arrays.asList(currentTables));
	inf=new JoinTreeInference(remaining,currentTables,order);
    }

    private void computeVarTableLocations(){
	varTableLocations=new IntSet[currentTables[0].domain().size()];
	for(int i=0;i<varTableLocations.length;i++){
	    varTableLocations[i]=new IntSet();
	}
	for(int i=0;i<currentTables.length;i++){
	    IntSet is=currentTables[i].vars();
	    for(int j=0;j<is.size();j++){
		varTableLocations[is.get(j)].add(i);
	    }
	}
    }

    public static MapResult initial(Table[] cpts,IntSet mapVars,IntList orderHint){
	MapInitializer s=new MapInitializer(cpts,mapVars,orderHint);
	s.search();
	return s.getResult();
    }

    public static class MapResult{
	public final IntMap result;
	public final double score;
	private MapResult(IntMap r,double s){
	    result=r;
	    score=s;
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
    private MapResult getResult(){
	return new MapResult(solution,best);
    }

    private void search(){
	int var=computeBest();
        double[] values=(double[])inf.getPartial(var).clone();
	int[] varOrder=varOrder(values);
	if(remaining.size()==1){
	    best=values[varOrder[0]];
	    solution=new IntMap(currentInst);
	    solution.put(var,varOrder[0]);
	    computationsToFind=computationCount;
	    hillClimb(var,varOrder[0]);
	    return;
	}
	remaining.remove(var);
	Table[] stored=getVarTables(var);
	int val=varOrder[0];
	currentInst.put(var,val);
	inf.setEvidence(var,val);
	shrinkTables(var,val,stored);
	search();
    }

    private void hillClimb(int var,int val){
	IntMap original=solution;
	IntSet vars=original.keys();
	inf.setEvidence(var,val);
	boolean betterFound=false;
	boolean changed;
	double currentBest=best;
	IntMap actual=new IntMap(original);
	do{
	    changed=false;
	    double bestScore=currentBest;
	    int bestVar=-1;
	    int bestVal=-1;
	    for(int i=0;i<vars.size();i++){
		double[] vals=inf.getPartial(vars.get(i));
		int maxInd=ArrayUtils.maxInd(vals);
		if(vals[maxInd]>bestScore){
		    bestScore=vals[maxInd];
		    bestVar=vars.get(i);
		    bestVal=maxInd;
		}
	    }
	    if(bestScore>currentBest){
		actual.put(bestVar,bestVal);
		currentBest=bestScore;
		inf.setEvidence(bestVar,bestVal);
		computationCount++;
		changed=true;
		betterFound=true;
	    }
	}while(changed);
	if(betterFound){
	    solution=actual;
	    best=currentBest;
	    for(int i=0;i<vars.size();i++){
		int v=vars.get(i);
		inf.setEvidence(v,original.get(v));
	    }
	}
	inf.clearEvidence(var);
    }
    private int[] varOrder(double[] vals){
	return ArrayUtils.reversed(ArrayUtils.sortedInds(vals));
    }

    private int computeBest(){
	double bestScore=Double.POSITIVE_INFINITY;
	int bestInd=0;
	for(int i=0;i<remaining.size();i++){
	    double[] vals=inf.getPartial(remaining.get(i));
	    double score=ArrayUtils.max(vals);
	    if(score<bestScore){
		bestScore=score;
		bestInd=remaining.get(i);
	    }
	}
	return bestInd;
    }

    private Table[] getVarTables(int var){
	IntSet is=varTableLocations[var];
	Table[] result=new Table[is.size()];
	for(int i=0;i<result.length;i++){
	    result[i]=currentTables[is.get(i)];
	}
	return result;
    }


    private void shrinkTables(int var,int val,Table[] stored){
	IntSet is=varTableLocations[var];
	for(int i=0;i<is.size();i++){
	    currentTables[is.get(i)]=stored[i].shrink(var,val);
	}
	
    }
  
    private void restoreTables(int var,Table[] stored){
	IntSet is=varTableLocations[var];
	for(int i=0;i<is.size();i++){
	    currentTables[is.get(i)]=stored[i];
	}
    }
	    

    
}
    
