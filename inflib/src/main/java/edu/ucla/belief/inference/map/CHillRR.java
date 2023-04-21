package edu.ucla.belief.inference.map;
import java.util.Random;
class CHillRR extends CSpaceApproximator{
    Random rand;
    public CHillRR(){
        rand=new Random();
    }
    public String getName(){
        return "H";
    }
    protected boolean isAllowed(int var,int val){
        if(val==MapApproximator.UNASSIGNED){
            throw new IllegalArgumentException();
        }else{
            return true;
        }
    }
    protected Instance bestMove(){
        Instance inst=performFlip(bestFlip(currentInstance().score()));
        if(inst!=null){
            return inst;
        }else{
            //System.err.print("\n*");
            return generateRandomMove();
        }
    }
}
