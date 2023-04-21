package il2.inf.map;

import il2.util.*;
import il2.model.*;
import il2.inf.structure.*;

import java.util.Random;

public class JoinTreeInference
{
    IntSet indicators;
    IntMap evidence;
    ShenoyShaferAlgorithm jta;
    Domain domain;

    public JoinTreeInference(Table[] cpts,IntList eliminationOrder){
	this(new IntSet(eliminationOrder),cpts,eliminationOrder);
    }

    public JoinTreeInference( Table[] cpts, int reps, Random seed ){
	this( cpts, computeEliminationOrder( cpts, reps, seed ) );
    }

    private static IntList computeEliminationOrder( Table[] cpts, int reps, Random seed ){
	EliminationOrders.Record rec = EliminationOrders.minFill( java.util.Arrays.asList(cpts), reps, seed );
	return rec.order;
    }

    public JoinTreeInference( IntSet indicatorVars, Table[] cpts, int reps, Random seed ){
	this( indicatorVars, cpts, computeEliminationOrder( cpts, reps, seed ) );
    }

    public JoinTreeInference(IntSet indicatorVars,Table[] cpts,IntList eliminationOrder){
	indicators=new IntSet(indicatorVars);
	evidence=new IntMap(indicators.size());
	domain=cpts[0].domain();
	Table[] leaves=new Table[indicators.size()+cpts.length];
	for(int i=0;i<indicators.size();i++){
	    leaves[i]=Table.indicatorTable(domain,indicators.get(i));
	}
	System.arraycopy(cpts,0,leaves,indicators.size(),cpts.length);
	IndexedJoinTree ijt=new IndexedJoinTree(leaves,eliminationOrder);
	jta=new ShenoyShaferAlgorithm(ijt,leaves);
    }

    public void setEvidence(int var,int val){
	evidence.put(var,val);
	jta.setLeaf(indicators.indexOf(var),Table.evidenceTable(domain,var,val));
    }

    public void clearEvidence(int var){
	evidence.remove(var);
	jta.setLeaf(indicators.indexOf(var),Table.indicatorTable(domain,var));
    }


    public double[] getPartial(int var){
	return jta.getPartial(indicators.indexOf(var));
    }

    public IntSet indicators(){
	return indicators;
    }

    public double probability(){
	return jta.probability();
    }

}
