package il2.inf.jointree;

import il2.bridge.*;
import il2.util.*;
import il2.model.*;
import il2.inf.structure.*;
import java.util.*;
import il2.model.BayesianNetwork;

public class UnindexedSSAlgorithm extends AbstractSSAlgorithm{

    public static UnindexedSSAlgorithm create( Converter c, BayesianNetwork bn2, IntList eliminationOrder ){
	return create( bn2.cpts(),eliminationOrder, c, bn2 );
    }

    private static UnindexedSSAlgorithm create(Table[] tables, IntList eliminationOrder, Converter c, BayesianNetwork bn2 )
    {
	long start=System.currentTimeMillis();
	Collection subdomains = Arrays.asList(tables);
	if( eliminationOrder == null ) eliminationOrder = EliminationOrders.minFill(subdomains,1,(Random)null).order;
	EliminationOrders.JT jt=EliminationOrders.traditionalJoinTree( subdomains, eliminationOrder, c, bn2 );
	UnindexedSSAlgorithm result=new UnindexedSSAlgorithm(jt,tables);
	long finish=System.currentTimeMillis();
	result.compilationTime=(finish-start)/1000.0;
	return result;
    }

    /**
    	@author Keith Cascio
    	@since 012904
    */
    public static UnindexedSSAlgorithm create( BayesianNetwork bn, EliminationOrders.JT jt ){
    	return create( bn.cpts(), jt );
    }

    /**
    	@author Keith Cascio
    	@since 012904
    */
    public static UnindexedSSAlgorithm create( Table[] tables, EliminationOrders.JT jt )
    {
	long start=System.currentTimeMillis();
	//Collection subdomains = Arrays.asList(tables);
	//if( eliminationOrder == null ) eliminationOrder = EliminationOrders.minFill(subdomains).order;
	//EliminationOrders.JT jt=EliminationOrders.traditionalJoinTree(subdomains,eliminationOrder);
	UnindexedSSAlgorithm result=new UnindexedSSAlgorithm(jt,tables);
	long finish=System.currentTimeMillis();
	result.compilationTime=(finish-start)/(1000.0);
	return result;
    }

    public static UnindexedSSAlgorithm create(BayesianNetwork bn, IntSet qvars, IntMap e){
	return create(bn.simplify(qvars,e),(IntList)null, (Converter)null, bn );
    }

    public static UnindexedSSAlgorithm create( Converter c, BayesianNetwork bn2 ){
	return create( c, bn2, (IntList)null );
    }

    /**
        This method is safe to use only in the absence of a BayesianNetwork object.
        @since 021004
    */
    public static UnindexedSSAlgorithm create(Table[] tables){
	EliminationOrders.JT jt = EliminationOrders.traditionalJoinTree(Arrays.asList(tables));
	return new UnindexedSSAlgorithm(jt,tables);
    }
    /**
        This method is safe to use only in the absence of a BayesianNetwork object.
        @since 160105
    */
    public static UnindexedSSAlgorithm create(Table[] tables, IntList order){
	EliminationOrders.JT jt = EliminationOrders.traditionalJoinTree(Arrays.asList(tables), order);
	return new UnindexedSSAlgorithm(jt,tables);
    }

    protected Table[][] evidenceIndicators;

    protected UnindexedSSAlgorithm(EliminationOrders.JT jointree,Table[] tables){
	super(jointree,tables);
    }

    protected void sendMessage(int ind,boolean isInward){
	if(isInward){
	    sendMessage(messageOrder[ind].s1,messages[0][ind],messages[1][ind]);
	}else{
	    sendMessage(messageOrder[ind].s2,messages[1][ind],messages[0][ind]);
	}
    }

    protected Table[] getAllTables(int cluster){
	Table[] e=evidenceIndicators[cluster];
	Table[] assigned=assignedTables[cluster];
	Table[] incoming=incomingMessages[cluster];
	Table[] allTables=new Table[e.length+assigned.length+incoming.length];
	System.arraycopy(e,0,allTables,0,e.length);
	System.arraycopy(assigned,0,allTables,e.length,assigned.length);
	System.arraycopy(incoming,0,allTables,e.length+assigned.length,incoming.length);
	return allTables;
    }

    protected Table[] remove(Table[] orig,Table excluded){
	Table[] result=new Table[orig.length-1];
	int current=0;
	for(int i=0;i<orig.length;i++){
	    if(orig[i]!=excluded){
		result[current++]=orig[i];
	    }
	}
	return result;
    }

    protected void sendMessage(int cluster,Table dest,Table excluded){
	Table[] relevantTables=remove(getAllTables(cluster),excluded);
	dest.multiplyAndProjectInto(relevantTables);
    }

    protected Table computeTableJoint(int table){
	int cluster=tableClusterAssignments[table];
	Table dest=Table.createCompatible(originalTables[table]);
	dest.multiplyAndProjectInto(getAllTables(cluster));
	return dest;
    }
    public Table tablePartial(int table){
	makeValid();
	int cluster=tableClusterAssignments[table];
	Table dest=Table.createCompatible(originalTables[table]);
	dest.multiplyAndProjectInto(remove(getAllTables(cluster),originalTables[table]));
	return dest;
    }

    protected Table computeVarJoint(int var){
	int cluster=containingClusters[var].get(0);
	Table dest=Table.varTable(domain,var);
	dest.multiplyAndProjectInto(getAllTables(cluster));
	return dest;
    }
    public Table varPartial(int var){
	makeValid();
	if(!evidence.keys().contains(var)){
	    return computeVarJoint(var);
	}
	int cluster=containingClusters[var].get(0);
	Table dest=Table.varTable(domain,var);
	Table excluded=findEvidenceTable(var);
	dest.multiplyAndProjectInto(remove(getAllTables(cluster),excluded));
	return dest;
    }

    protected Table findEvidenceTable(int var){
	Table[] ei=evidenceIndicators[containingClusters[var].get(0)];
	for(int i=0;i<ei.length;i++){
	    if(ei[i].vars().contains(var)){
		return ei[i];
	    }
	}
	throw new IllegalStateException();
    }

    protected double computePrE(){
	Table dest=Table.constantTable(domain,1);
	dest.multiplyAndProjectInto(getAllTables(smallestCluster));
	return dest.values()[0];
    }

    protected void initialize(){
	setUpEvidence();
    }

    private void setUpEvidence(){
	evidenceIndicators=new Table[clusters.length][];
	IntSet eKeys=evidence.keys();
	for(int i=0;i<assignedEvidence.length;i++){
	    IntSet is=eKeys.intersection(assignedEvidence[i]);
	    Table[] t=new Table[is.size()];
	    for(int j=0;j<is.size();j++){
		int var=is.get(j);
		t[j]=Table.evidenceTable(domain,var,evidence.get(var));
	    }
	    evidenceIndicators[i]=t;
	}
    }

    public double getMemoryRequirements(){
	return getTableSizes()/1024/1024;
    }


}
