package edu.ucla.belief.inference.map;
import java.util.*;
import edu.ucla.belief.*;
abstract class PSpaceApproximator extends MapApproximator{
    private Instance currentInstance;
    private int evaluationsRemaining;
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
        if(currentInstance.isComplete()){
            setBestInstance(currentInstance);
        }
        
        for(evaluationsRemaining=evaluationsAllowed;evaluationsRemaining>0;evaluationsRemaining--){
            currentInstance=bestMove();
            ensureSynchronized(currentInstance);
            //System.err.print("("+currentInstance.unassigned()+") ");
            if((bestInstance()==null || currentInstance.score()>bestInstance().score()) && currentInstance.isComplete()){
                //System.err.println("setting best "+currentInstance.score());
                setBestInstance(currentInstance);
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
    private int maxIndExcluding(double[] vals,int exc){
        int best=(exc==0)?1:0;
        for(int i=0;i<vals.length;i++){
            if(vals[best]<vals[i] && i!=exc){
                best=i;
            }
        }
        return best;
    }
        
    protected final Flip bestFlip(double initial){
        return bestFlip(initial==0);
    }
    protected final Flip bestFlip(final boolean allowSubOptimal){
        double bestScore=0;
        boolean backup=false;
        int bestvar=-1;
        int bestval=-1;
        for(int i=0;i<varcount();i++){
            double[] d=partial(i);
            int ind=maxInd(d);
            int oldInd=currentInstance().value(i);
            if(oldInd==MapApproximator.UNASSIGNED && isAllowed(i,ind)){
                if(!backup && d[ind]>=bestScore){
                    bestScore=d[ind];
                    bestvar=i;
                    bestval=ind;
                }
            }else{
                if(ind!=oldInd && (!backup || d[ind]>bestScore) && isAllowed(i,MapApproximator.UNASSIGNED)){
                    bestScore=d[ind];
                    bestvar=i;
                    bestval=MapApproximator.UNASSIGNED;
                    backup=true;
                }
            }
        }
        if(bestvar==-1){
            return null;
        }else{
            /*if(bestval==MapApproximator.UNASSIGNED){
                //System.err.print("-");
                System.err.print(" "+(currentInstance().unassigned()+1));
            }else if(currentInstance().value(bestvar)==MapApproximator.UNASSIGNED){
                //System.err.print("+");
                System.err.print(" "+(currentInstance().unassigned()-1));
            }else{
                System.err.print("("+bestvar+","+bestval+")");
            }*/
            return new Flip(bestvar,bestval,bestScore);
        }
    }
}
