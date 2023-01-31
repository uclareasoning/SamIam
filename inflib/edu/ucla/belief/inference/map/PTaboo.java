package edu.ucla.belief.inference.map;
import java.util.*;
class PTaboo extends PSpaceApproximator{
    Set visited;
    Random rand;
    public PTaboo(){
        rand=new Random();
    }
    public void run(Instance initial,int count){
        visited=new HashSet( Math.max( count+1, 1 ) );
        super.run(initial,count);
    }
    public String getName(){
        return "PTab";
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
    private Instance generateRandomMove(){
        int[] vals=new int[varcount()];
        for(int i=0;i<vals.length;i++){
            if(rand.nextBoolean()){
                vals[i]=rand.nextInt(varsize(i));
            }else{
                vals[i]=-1;
            }
            setState(i,vals[i]);
        }
        return new Instance(vals,probability());
    }
}
