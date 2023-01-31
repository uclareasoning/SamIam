package edu.ucla.belief.inference.map;

import edu.ucla.belief.*;
import edu.ucla.belief.inference.BeliefCompilation;
import java.util.*;

abstract class MapApproximator{
    private Instance bestInstance;
    private BeliefCompilation engine;
    private List mapvars;
    private Set mapvarSet;
    private int[] varsizes;
    protected static final int UNASSIGNED=-1;
    protected int bestFoundTime;
    protected int peaksToFindBest;
    public void init(BeliefCompilation bc,Set mapvars){
        bestInstance=null;
        this.mapvars=new ArrayList(mapvars);
        Collections.shuffle(this.mapvars);
        this.mapvarSet=mapvars;
        this.engine=bc;
        varsizes=new int[mapvars.size()];
        for(int i=0;i<varsizes.length;i++){
            varsizes[i]=var(i).size();
        }
        clearAssignments();
    }
    public int peaksToFindBest(){
        return peaksToFindBest;
    }
    public int bestFoundTime(){
        return bestFoundTime;
    }
    protected final int varcount(){
        return varsizes.length;
    }
    protected final int varsize(int i){
        return varsizes[i];
    }
    protected final double probability(){
        double[] x=joint(0);
        double total=0;
        for(int i=0;i<x.length;i++){
            total+=x[i];
        }
        return total;
    }
    public final void clearAssignments(){
        for(int i=0;i<varcount();i++){
            setState(i,UNASSIGNED);
        }
    }
    public final Instance bestInstance(){
        return bestInstance;
    }
    protected final void setBestInstance(Instance instance){
        bestInstance=instance;
        //System.err.println(" setting best: "+instance.score());
    }
    public abstract String getName();
    public abstract void run(Instance initial,int allowedEvaluations);
    protected final FiniteVariable var(int i){
        return (FiniteVariable)mapvars.get(i);
    }
    protected void ensureSynchronized(Instance current){
        for(int i=0;i<varcount();i++){
            int val=current.value(i);
            double[] lik=likelihood(i);
            if(val==UNASSIGNED){
                for(int j=0;j<lik.length;j++){
                    if(lik[j]<.999){
                        throw new IllegalStateException("Unsynchronized");
                    }
                }
            }else{
                for(int j=0;j<lik.length;j++){
                    if(j==val){
                        if(lik[j]<.999){
                            throw new IllegalStateException("Unsynched");
                        }
                    }else if(lik[j]>.0001){
                        throw new IllegalStateException("Unsynchroed");
                    }
                }
            }
        }
    }
    protected final double[] likelihood(int i){
        return engine.getLikelihood(var(i)).dataclone();
    }
    protected final void setLikelihood(int i,double[] vals){
        engine.setLikelihood(var(i),vals);
    }
    protected final void setState(int i,int ind){
        double[] likelihood=new double[var(i).size()];
        if(ind==UNASSIGNED){
            java.util.Arrays.fill(likelihood,1);
        }else{
            likelihood[ind]=1;
        }
        setLikelihood(i,likelihood);
    }
    protected final double[] partial(int i){
        return engine.getPartial(var(i)).dataclone();
    }
    protected final double retractedProbability(int var){
        double[] p=partial(var);
        double total=0;
        for(int i=0;i<p.length;i++){
            total+=p[i];
        }
        return total;
    }
    protected final double[] joint(int i){
        double[] p=partial(i);
        double[] l=likelihood(i);
        double[] result=new double[p.length];
        for(int j=0;j<p.length;j++){
            result[j]=p[j]*l[j];
        }
        return result;
    }
    protected final int maxInd(double[] vals){
        int best=0;
        for(int i=0;i<vals.length;i++){
            if(vals[i]>vals[best]){
                best=i;
            }
        }
        return best;
    }
    public final class Flip{
        int var;
        int val;
        double score;
        public Flip(int var,int val,double score){
            this.var=var;
            this.val=val;
            this.score=score;
        }
        public int var(){
            return var;
        }
        public int val(){
            return val;
        }
        public double score(){
            return score;
        }
    }
    protected void setState(Instance inst){
        for(int i=0;i<varcount();i++){
            setState(i,inst.value(i));
        }
    }
    public final class Instance{
        int[] inds;
        double score;
        public Instance(Map inst){
            inds=new int[varcount()];
            for(int i=0;i<inds.length;i++){
                Object val=inst.get(var(i));
                if(val==null){
                    inds[i]=-1;
                }else{
                    inds[i]=var(i).index(val);
                }
            }
        }
        public Instance(int[] ind,double score){
            inds=ind;
            this.score=score;
        }
        public Instance flip(int var,int val,double sc){
            int[] ninds=(int[])inds.clone();
            ninds[var]=val;
            return new Instance(ninds,sc);
        }
        public Instance flip(Flip f){
            return flip(f.var(),f.val(),f.score());
        }
        public double score(){
            return score;
        }
        void setScore(double val){
            score=val;
        }
        public boolean equals(Object obj){
            if(obj==null){
                return false;
            }
            if(!(obj instanceof Instance)){
                return false;
            }
            Instance temp=(Instance)obj;
            for(int i=0;i<inds.length;i++){
                if(inds[i]!=temp.inds[i]){
                    return false;
                }
            }
            return true;
        }
        public final Map mapping(){
	    Map result=new HashMap(inds.length);
	    for(int i=0;i<inds.length;i++){
	        result.put(var(i),var(i).instance(inds[i]));
	    }
	    return result;
        }
        public int value(int var){
            return inds[var];
        }
        public int unassigned(){
            int total=0;
            for(int i=0;i<inds.length;i++){
                if(inds[i]==UNASSIGNED){
                    total++;
                }
            }
            return total;
        }
        boolean isComplete(){
            return unassigned()==0;
        }
        public int hashCode(){
            int total=inds[0];
            for(int i=1;i<inds.length;i++){
                total*=var(i-1).size();
                total+=inds[i];
            }
            return total;
        }
    }
}


