package il2.inf.jointree;

import il2.bridge.*;
import il2.util.*;
import il2.inf.structure.*;
import il2.model.*;
import java.util.Arrays;
import java.util.Collection;

public class UnindexedHuginAlgorithm extends AbstractHuginAlgorithm{

    public static UnindexedHuginAlgorithm create( Converter c, BayesianNetwork bn2, IntList eliminationOrder ){
	return create( bn2.cpts(),eliminationOrder, c, bn2 );
    }

    private static UnindexedHuginAlgorithm create( Table[] tables, IntList eliminationOrder, Converter c, BayesianNetwork bn2 )
    {
	long start=System.currentTimeMillis();
	Collection subdomains = Arrays.asList(tables);
	if( eliminationOrder == null ) eliminationOrder = EliminationOrders.minFill(subdomains,1,(java.util.Random)null).order;
	EliminationOrders.JT jt=EliminationOrders.traditionalJoinTree( subdomains, eliminationOrder, c, bn2 );
	UnindexedHuginAlgorithm result=new UnindexedHuginAlgorithm(jt,tables);
	long finish=System.currentTimeMillis();
	result.compilationTime=(finish-start)/(1000.0);
	return result;
    }

    /**
    	@author Keith Cascio
    	@since 012904
    */
    public static UnindexedHuginAlgorithm create( BayesianNetwork bn, EliminationOrders.JT jt ){
    	return create( bn.cpts(), jt );
    }

    /**
    	@author Keith Cascio
    	@since 012904
    */
    public static UnindexedHuginAlgorithm create( Table[] tables, EliminationOrders.JT jt )
    {
	long start=System.currentTimeMillis();
	//Collection subdomains = Arrays.asList(tables);
	//if( eliminationOrder == null ) eliminationOrder = EliminationOrders.minFill(subdomains).order;
	//EliminationOrders.JT jt=EliminationOrders.traditionalJoinTree(subdomains,eliminationOrder);
	UnindexedHuginAlgorithm result=new UnindexedHuginAlgorithm(jt,tables);
	long finish=System.currentTimeMillis();
	result.compilationTime=(finish-start)/(1000.0);
	return result;
    }

    public static UnindexedHuginAlgorithm create( BayesianNetwork bn, IntSet qvars, IntMap e ){
	return create( bn.simplify(qvars,e),(IntList)null, (Converter)null, bn );
    }

    public static UnindexedHuginAlgorithm create( Converter c, BayesianNetwork bn2 ){
	return create( c, bn2, (IntList)null );
    }

    private double[] scratch1;
    private double[] scratch2;

    protected UnindexedHuginAlgorithm(EliminationOrders.JT jointree,Table[] tables){
	super(jointree,tables);
	int largestSeparatorSize=findLargestSeparatorSize();
	scratch1=new double[largestSeparatorSize];
	scratch2=new double[largestSeparatorSize];
    }

    private int findLargestSeparatorSize(){
	int result=0;
	for(int i=0;i<separatorTables.length;i++){
	    if(separatorTables[i].sizeInt()>result){
		result=separatorTables[i].sizeInt();
	    }
	}
	return result;
    }

    protected void initializeCluster(int c){
	Table[] tables=assignedTables[c];
	Table ctable=clusterTables[c];
	java.util.Arrays.fill(ctable.values(),1.0);
	for(int i=0;i<tables.length;i++){
	    ctable.multiplyInto2(tables[i]);
	}
	IntSet assigned=assignedEvidence[c];
	for(int i=0;i<assigned.size();i++){
	    int var=assigned.get(i);
	    int val=evidence.get(var,-1);
	    if(val>=0){
		ctable.multiplyVarIndicators(var,val);
	    }
	}
    }

    protected void sendMessage(int mind,boolean inward){
	Table sepTable=separatorTables[mind];
	Table fromTable;
	Table toTable;
	if(inward){
	    fromTable=clusterTables[messageOrder[mind].s1];
	    toTable=clusterTables[messageOrder[mind].s2];
	    sepTable.projectInto2(fromTable);
	    toTable.multiplyInto2(sepTable);
	}else{
	    fromTable=clusterTables[messageOrder[mind].s2];
	    toTable=clusterTables[messageOrder[mind].s1];
	    double[] sv=sepTable.values();
	    System.arraycopy(sv,0,scratch1,0,sv.length);
	    sepTable.projectInto2(fromTable);
	    System.arraycopy(sv,0,scratch2,0,sv.length);
	    sepTable.divideRelevantInto(scratch1);
	    toTable.multiplyInto2(sepTable);
	    System.arraycopy(scratch2,0,sv,0,sv.length);
	}

    }

    protected Table computeTableJoint(int t){
	Table result=Table.createCompatible(originalTables[t]);
	result.projectInto2(clusterTables[tableClusterAssignments[t]]);
	return result;
    }

    public double getMemoryRequirements(){
	return (8*(scratch1.length+scratch2.length)+getTableSizes())/1024/1024;
    }
}
