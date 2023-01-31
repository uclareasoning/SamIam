package edu.ucla.belief.inference.map;
import java.util.*;
import edu.ucla.belief.*;
abstract class CSpaceApproximator extends MapApproximator{
    private Instance currentInstance;
    private int evaluationsRemaining;
    private int peakCount;
    protected abstract boolean isAllowed(int var,int val);
    protected final int evaluationsRemaining(){
        return evaluationsRemaining;
    }
    public void run(Instance initialInstance,int evaluationsAllowed){
        //System.err.print("\n\ni");
        for(int i=0;i<varcount();i++){
            setState(i,initialInstance.value(i));
        }
        currentInstance=initialInstance.flip(0,initialInstance.value(0),probability());
        setBestInstance(currentInstance);
        bestFoundTime=0;
        peaksToFindBest=0;
        int count=0; 
        peakCount=1;
        
        for(evaluationsRemaining=evaluationsAllowed;evaluationsRemaining>0;evaluationsRemaining--){
            count++;
            currentInstance=bestMove();
            //System.err.print("("+currentInstance.unassigned()+") ");
            if((bestInstance()==null || currentInstance.score()>bestInstance().score()*1.00001) && currentInstance.isComplete()){
                //System.err.println("setting best "+currentInstance.score());
                setBestInstance(currentInstance);
                bestFoundTime=count;
                peaksToFindBest=peakCount;
            }
        }
    }
    protected Instance currentInstance(){
        return currentInstance;
    }
    protected abstract Instance bestMove();
    protected Instance performFlip(Flip f){
        if(f==null){
            return null;
        }else{
            setState(f.var(),f.val());
            return currentInstance().flip(f);
        }
    }
    protected final Flip bestFlip(double initialScore){
        double bestScore=initialScore;
        double bestProb=initialScore;
        int bestvar=-1;
        int bestval=-1;
        for(int i=0;i<varcount();i++){
            double[] d=partial(i);
            for(int ind=0;ind<d.length;ind++){
                double score=d[ind];
                if(score>bestScore && currentInstance().value(i)!=ind && isAllowed(i,ind)){
                    bestScore=score;
                    bestProb=d[ind];
                    bestvar=i;
                    bestval=ind;
                }
            }
        }
        if(bestvar==-1){
            return null;
        }else{
            /*if(bestval==MapApproximator.UNASSIGNED){
                System.err.print("-");
            }else if(currentInstance().value(bestvar)==MapApproximator.UNASSIGNED){
                System.err.print("+");
            }else{
                System.err.print("=");
            }*/
            return new Flip(bestvar,bestval,bestProb);
        }
    }
    protected Instance generateRandomMove(){
        Instance current=currentInstance();
        for(int i=0;i<varcount()/4;i++){
            current=generateRandomMove(current);
        }
        peakCount++;
        return current.flip(0,current.value(0),probability());
    }
    private java.util.Random rand=new java.util.Random();
    protected Instance generateRandomMove(Instance inst){
        int ind=rand.nextInt(varcount());
        int val=rand.nextInt(var(ind).size()-1);
        if(val>=inst.value(ind)){
            val++;
        }
        return inst.flip(ind,val,0);
    }
}
