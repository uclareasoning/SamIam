package edu.ucla.belief.inference.map;
import java.util.Random;
class PHillRR extends PSpaceApproximator{
    Random rand;
    public PHillRR(){
        rand=new Random();
    }
    public String getName(){
        return "PH-RR";
    }
    protected boolean isAllowed(int var,int val){
        if(val==MapApproximator.UNASSIGNED){//only allows backup if it has
                                            //time to fully recover.
            return evaluationsRemaining()>currentInstance().unassigned()+1;
        }else if(currentInstance().value(var)!=MapApproximator.UNASSIGNED){
            return evaluationsRemaining()>currentInstance().unassigned();
        }else{
            return true;
        }
    }
    protected Instance bestMove(){
        Instance inst=performFlip(bestFlip(currentInstance().score()));
        if(inst!=null){
            return inst;
        }else{
            return generateRandomMove();
        }
    }
    private Instance generateRandomMove(){
        Instance b=bestInstance();
        int[] vals=new int[varcount()];
        for(int i=0;i<vals.length;i++){
            if(rand.nextDouble()>.75){
                vals[i]=rand.nextInt(varsize(i)+1)-1;
            }else{
                vals[i]=b.value(i);
            }
            setState(i,vals[i]);
        }
        return new Instance(vals,probability());
    }
}
