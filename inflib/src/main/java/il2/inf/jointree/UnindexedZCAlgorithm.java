package il2.inf.jointree;

import il2.bridge.*;
import il2.util.*;
import il2.model.*;
import il2.inf.structure.*;
import java.util.Arrays;
import java.util.Collection;

public class UnindexedZCAlgorithm extends AbstractZCAlgorithm{

    public static UnindexedZCAlgorithm create( Converter c, BayesianNetwork bn2, IntList eliminationOrder ){
	return create( bn2.cpts(),eliminationOrder, c, bn2 );
    }

    private static UnindexedZCAlgorithm create( Table[] tables, IntList eliminationOrder, Converter c, BayesianNetwork bn2 )
    {
	long start=System.currentTimeMillis();
	Collection subdomains = Arrays.asList(tables);
	if( eliminationOrder == null ) eliminationOrder = EliminationOrders.minFill(subdomains,1,(java.util.Random)null).order;
	EliminationOrders.JT jt=EliminationOrders.traditionalJoinTree( subdomains, eliminationOrder, c, bn2 );
	UnindexedZCAlgorithm result=new UnindexedZCAlgorithm(jt,tables);
	long finish=System.currentTimeMillis();
	result.compilationTime=(finish-start)/1000.0;
	return result;
    }

    /**
    	@author Keith Cascio
    	@since 012904
    */
    public static UnindexedZCAlgorithm create( BayesianNetwork bn, EliminationOrders.JT jt ){
    	return create( bn.cpts(), jt );
    }

    /**
    	@author Keith Cascio
    	@since 012904
    */
    public static UnindexedZCAlgorithm create( Table[] tables, EliminationOrders.JT jt )
    {
	long start=System.currentTimeMillis();
	//Collection subdomains = Arrays.asList(tables);
	//if( eliminationOrder == null ) eliminationOrder = EliminationOrders.minFill(subdomains).order;
	//EliminationOrders.JT jt=EliminationOrders.traditionalJoinTree(subdomains,eliminationOrder);
	UnindexedZCAlgorithm result=new UnindexedZCAlgorithm(jt,tables);
	long finish=System.currentTimeMillis();
	result.compilationTime=(finish-start)/(1000.0);
	return result;
    }

    public static UnindexedZCAlgorithm create( BayesianNetwork bn, IntSet qvars, IntMap e ){
	return create( bn.simplify(qvars,e),(IntList)null, (Converter)null, bn );
    }

    public static UnindexedZCAlgorithm create( Converter c, BayesianNetwork bn2 ){
	return create( c, bn2, (IntList)null );
    }

    protected double[] scratch1;
    protected double[] scratch2;
    boolean[] zeroFreeScratch;

    public UnindexedZCAlgorithm(EliminationOrders.JT jointree,Table[] tables){
	super(jointree,tables);
	int largestSeparatorSize=findLargestSeparatorSize();
	scratch1=new double[largestSeparatorSize];
	scratch2=new double[largestSeparatorSize];
	zeroFreeScratch=new boolean[largestSeparatorSize];
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
	boolean[] zfc=clusterZeroFree[c];
	java.util.Arrays.fill(ctable.values(),1.0);
	java.util.Arrays.fill(zfc,true);
	for(int i=0;i<tables.length;i++){
	    ctable.zeroConciousMultiplyInto(tables[i],zfc);
	}
	IntSet assigned=assignedEvidence[c];
	for(int i=0;i<assigned.size();i++){
	    int var=assigned.get(i);
	    int val=evidence.get(var,-1);
	    if(val>=0){
		ctable.zeroConciousMultiplyVarIndicators(var,val,zfc);
	    }
	}
    }

    protected void sendMessage(int mind,boolean inward){
	Table sepTable=separatorTables[mind];
	Table fromTable;
	Table toTable;
	if(inward){
	    int from=messageOrder[mind].s1;
	    int to=messageOrder[mind].s2;
	    fromTable=clusterTables[from];
	    toTable=clusterTables[to];
	    sepTable.zeroConciousRealProjectInto(fromTable,clusterZeroFree[from]);
	    toTable.zeroConciousMultiplyInto(sepTable,clusterZeroFree[to]);

	}else{
	    int from=messageOrder[mind].s2;
	    int to=messageOrder[mind].s1;
	    fromTable=clusterTables[from];
	    toTable=clusterTables[to];
	    double[] sv=sepTable.values();
	    System.arraycopy(sv,0,scratch1,0,sv.length);
	    sepTable.zeroConciousProjectInto(fromTable,clusterZeroFree[from],zeroFreeScratch);
	    System.arraycopy(sv,0,scratch2,0,sv.length);
	    sepTable.zeroConciousDivideRelevantInto(scratch1,zeroFreeScratch);
	    toTable.zeroConciousMultiplyInto(sepTable,clusterZeroFree[to]);
	    System.arraycopy(scratch2,0,sv,0,sv.length);
	    sepTable.zeroConciousMakeReal(zeroFreeScratch);
	}

    }

    protected Table computeTableJoint(int t){
	Table result=Table.createCompatible(originalTables[t]);
	int c=tableClusterAssignments[t];
	result.zeroConciousRealProjectInto(clusterTables[c],clusterZeroFree[c]);
	return result;
    }
    public Table tablePartial(int t){
	makeValid();
	Table result=Table.createCompatible(originalTables[t]);
	boolean[] zf=new boolean[result.sizeInt()];
	int c=tableClusterAssignments[t];
	result.zeroConciousProjectInto(clusterTables[c],clusterZeroFree[c],zf);
	result.zeroConciousDivideRelevantInto(originalTables[t].values(),zf);
	return result;
    }

    public double getMemoryRequirements(){
	return (zeroFreeScratch.length/8+ 8*(scratch1.length+scratch2.length)+getTableSizes())/1024/1024;
    }
}
