package il2.inf.rc;

import il2.util.*;
import il2.inf.PartialDerivativeEngine;
import il2.inf.structure.*;
import il2.model.*;

import java.util.*;

public class RCEngine implements PartialDerivativeEngine{
    private IntMap evidence;
    private IntMap varLocs;
    private RC rcCore;
    private double compilationTime=Double.NaN;

    /*public static RCEngine create(BayesianNetwork bn){
	return create(bn,null);
    }
    public static RCEngine create(BayesianNetwork bn,IntList eliminationOrder){
	return create(bn.cpts(),eliminationOrder);
    }

    public static RCEngine create(BayesianNetwork bn,IntSet qvars,IntMap e){
	return create(bn.simplify(qvars,e),null);
    }*/

    /** @since 081105 */
    public static RCEngine create( BayesianNetwork bn, int reps, Random seed ){
    	Table[] tables = bn.cpts();
    	return create( tables, EliminationOrders.minFill( Arrays.asList(tables), reps, seed ).order );
    }

    public static RCEngine create( Table[] tables, IntList eliminationOrder ){
    	if( eliminationOrder == null ) throw new IllegalArgumentException( "illegal null elimination order" );
	long start=System.currentTimeMillis();
	Domain domain=tables[0].domain();
	IntMap varLocs=getVarLocs(tables);
	Table[] allTables=new Table[tables.length+varLocs.size()];
	System.arraycopy(tables,0,allTables,0,tables.length);
	for(int i=0;i<varLocs.size();i++){
	    int var=varLocs.keys().get(i);
	    int loc=varLocs.values().get(i);
	    allTables[loc]=Table.indicatorTable(domain,var);
	}
	//if(eliminationOrder==null){
	//    eliminationOrder=EliminationOrders.minFill(Arrays.asList(tables)).order;
	//}
	DGraph dg=DGraphs.create(allTables,eliminationOrder);
	RC rcCore=new RC(dg,allTables);

	RCEngine result=new RCEngine(rcCore,varLocs);
	long finish=System.currentTimeMillis();
	result.compilationTime=(finish-start)/1000.0;
	//System.out.println("compilation time: "+result.compilationTime);
	return result;
    }

    private static IntMap getVarLocs(Table[] tables){
	Set vars=new TreeSet();
	for(int i=0;i<tables.length;i++){
	    IntSet s=tables[i].vars();
	    for(int j=0;j<s.size();j++){
		vars.add(new Integer(s.get(j)));
	    }
	}
	IntMap result=new IntMap(vars.size());
	int current=tables.length;
	for(Iterator iter=vars.iterator();iter.hasNext();){
	    result.putAtEnd(((Integer)iter.next()).intValue(),current++);
	}
	return result;
    }

    public RCEngine(DGraph dg,Table[] tables,IntMap locs){
	long start=System.currentTimeMillis();
	rcCore=new RC(dg,tables);
	varLocs=locs;
	evidence=new IntMap();
	long finish=System.currentTimeMillis();
	compilationTime=(finish-start)/1000.0;

    }
    private RCEngine(RC core, IntMap locs){
	rcCore=core;
	varLocs=locs;
	evidence=new IntMap();
    }

    public Domain domain(){
	return rcCore.domain();
    }

    public void setEvidence(IntMap ev){
	for(int i=0;i<evidence.size();i++){
	    int var=evidence.keys().get(i);
	    rcCore.setTable(varLocs.get(var),Table.indicatorTable(domain(),var));
	}
	evidence=ev;
	for(int i=0;i<evidence.size();i++){
	    int var=evidence.keys().get(i);
	    int val=evidence.values().get(i);
	    rcCore.setTable(varLocs.get(var),Table.evidenceTable(domain(),var,val));
	}
    }

    public void setTable(int t,Table table){
	rcCore.setTable(t,table);
    }

    public Table getTable(int i){
	return rcCore.getTable(i);
    }

    public Table tablePartial(int table){
	return rcCore.getIncoming(table);
    }

    public Table varPartial(int var){
	return rcCore.getIncoming(varLocs.get(var));
    }

    public double prEvidence(){
	return rcCore.value();
    }

    public double logPrEvidence(){
	return Math.log(prEvidence());
    }

    public Table tableJoint(int table){
	return getTable(table).multiply(tablePartial(table));
    }

    public Table tableConditional(int table){
	return tableJoint(table).normalize();
    }

    public Table varJoint(int var){
	int ind=varLocs.get(var);
	return getTable(ind).multiply(rcCore.getIncoming(ind));
    }

    public Table varConditional(int var){
	return varJoint(var).normalize();
    }

    public JoinTreeStats.Stat getClusterStats(){
	return (JoinTreeStats.Stat)null;
    }

    public JoinTreeStats.Stat getSeparatorStats(){
	return (JoinTreeStats.Stat)null;
    }

    public double getCompilationTime(){
	return compilationTime;
    }

    public double getPropagationTime(){
	return rcCore.propagationTime;
    }

    public double getMemoryRequirements(){
	return Double.NaN;
    }

    public void fullCaching(){
	rcCore.fullCaching();
    }

    public void allocateCaches(Set cachedNodes){
	rcCore.allocateCaches(cachedNodes);
    }

    public DGraph dgraph(){
	return rcCore.dgraph();
    }

    /**
    	@author Keith Cascio
    	@since 112603
    */
    public RC rcCore(){
    	return rcCore;
    }
}
