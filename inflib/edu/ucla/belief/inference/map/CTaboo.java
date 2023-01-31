package edu.ucla.belief.inference.map;
import java.util.*;
class CTaboo extends CSpaceApproximator{
    Set visited;
    Random rand;
    public CTaboo(){
        rand=new Random();
    }
    public void run(Instance initial,int count){
        visited=new HashSet( Math.max( count+1, 1 ) );
        super.run(initial,count);
    }
    public String getName(){
        return "T";
    }
    protected boolean isAllowed(int var,int val){
        Instance inst=currentInstance().flip(var,val,0);
        return !visited.contains(inst);
    }
    protected Instance bestMove(){
        visited.add(currentInstance());
        Instance inst=performFlip(bestFlip(0));
        if(inst!=null){
            return inst;
        }else{
            return generateRandomMove();
        }
    }
}
