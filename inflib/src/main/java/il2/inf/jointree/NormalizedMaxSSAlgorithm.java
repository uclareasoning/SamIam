package il2.inf.jointree;

import il2.bridge.*;
import il2.util.*;
import il2.model.*;
import il2.inf.structure.*;
import java.util.*;
import il2.model.BayesianNetwork;

public class NormalizedMaxSSAlgorithm extends UnindexedSSAlgorithm {

    public static NormalizedMaxSSAlgorithm create( Converter c, BayesianNetwork bn2, IntList eliminationOrder ){
	return create( bn2.cpts(),eliminationOrder, c, bn2 );
    }

    private static NormalizedMaxSSAlgorithm create(Table[] tables, IntList eliminationOrder, Converter c, BayesianNetwork bn2 )
    {
	long start=System.currentTimeMillis();
	Collection subdomains = Arrays.asList(tables);
	if( eliminationOrder == null ) eliminationOrder = EliminationOrders.minFill(subdomains,1,(Random)null).order;
	EliminationOrders.JT jt=EliminationOrders.traditionalJoinTree( subdomains, eliminationOrder, c, bn2 );
	NormalizedMaxSSAlgorithm result=new NormalizedMaxSSAlgorithm(jt,tables);
	long finish=System.currentTimeMillis();
	result.compilationTime=(finish-start)/1000.0;
	return result;
    }

    /**
    	@author Keith Cascio
    	@since 012904
    */
    public static NormalizedMaxSSAlgorithm create( BayesianNetwork bn, EliminationOrders.JT jt ){
    	return create( bn.cpts(), jt );
    }

    /**
    	@author Keith Cascio
    	@since 012904
    */
    public static NormalizedMaxSSAlgorithm create( Table[] tables, EliminationOrders.JT jt )
    {
	long start=System.currentTimeMillis();
	NormalizedMaxSSAlgorithm result=new NormalizedMaxSSAlgorithm(jt,tables);
	long finish=System.currentTimeMillis();
	result.compilationTime=(finish-start)/(1000.0);
	return result;
    }

    public static NormalizedMaxSSAlgorithm create(BayesianNetwork bn, IntSet qvars, IntMap e){
	return create(bn.simplify(qvars,e),(IntList)null, (Converter)null, bn );
    }

    public static NormalizedMaxSSAlgorithm create( Converter c, BayesianNetwork bn2 ){
	return create( c, bn2, (IntList)null );
    }

    /**
        This method is safe to use only in the absence of a BayesianNetwork object.
        @since 021004
    */
    public static NormalizedMaxSSAlgorithm create(Table[] tables){
	EliminationOrders.JT jt = EliminationOrders.traditionalJoinTree(Arrays.asList(tables));
	return new NormalizedMaxSSAlgorithm(jt,tables);
    }
    /**
        This method is safe to use only in the absence of a BayesianNetwork object.
        @since 160105
    */
    public static NormalizedMaxSSAlgorithm create(Table[] tables, IntList order){
	EliminationOrders.JT jt = EliminationOrders.traditionalJoinTree(Arrays.asList(tables), order);
	return new NormalizedMaxSSAlgorithm(jt,tables);
    }

    protected NormalizedMaxSSAlgorithm(EliminationOrders.JT jointree,Table[] tables){
	super(jointree,tables);
    }

    double logPrE;
    protected void sendMessage(int ind,boolean isInward){
	if (ind == 0 && isInward) this.logPrE = 0.0;
	if(isInward){
	    sendMessage(messageOrder[ind].s1,messages[0][ind],messages[1][ind],isInward);
	}else{
	    sendMessage(messageOrder[ind].s2,messages[1][ind],messages[0][ind],isInward);
	}
    }

    protected void sendMessage(int cluster,Table dest,Table excluded,boolean isInward){
	Table[] relevantTables=remove(getAllTables(cluster),excluded);
	dest.multiplyAndProjectMaxInto(relevantTables);
	//dest.normalizeInPlace();
	double sum = dest.sum();
	if ( sum > 0.0 ) dest.multiplyByConstant(1.0/sum);
	if (isInward) { this.logPrE += Math.log(sum); }
    }

    /*
    protected void sendMessage(int cluster,Table dest,Table excluded){
	Table[] relevantTables=remove(getAllTables(cluster),excluded);
	dest.multiplyAndProjectMaxInto(relevantTables);
	dest.normalizeInPlace();
    }
    */

    protected Table computeTableJoint(int table){
	int cluster=tableClusterAssignments[table];
	Table dest=Table.createCompatible(originalTables[table]);
	dest.multiplyAndProjectMaxInto(getAllTables(cluster));
	return dest;
    }

    public Table tablePartial(int table){
	makeValid();
	int cluster=tableClusterAssignments[table];
	Table dest=Table.createCompatible(originalTables[table]);
	dest.multiplyAndProjectMaxInto(remove(getAllTables(cluster),originalTables[table]));
	return dest;
    }

    protected Table computeVarJoint(int var){
	int cluster=containingClusters[var].get(0);
	Table dest=Table.varTable(domain,var);
	dest.multiplyAndProjectMaxInto(getAllTables(cluster));
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
	dest.multiplyAndProjectMaxInto(remove(getAllTables(cluster),excluded));
	return dest;
    }

    /**
     * This uses same basic algorithm from NormalizedSSAlgorithm,
     * which was originally for Pr(e) but also works for MPE.
     *
     * Alternative proof of correctness comes from anytime variable
     * elimination algorithm (Arthur).
     */
    protected double computeLogPrMPE() {
        int c0 = messageOrder[separators.size()-1].s2;
        Table dest=Table.constantTable(domain,1);
        dest.multiplyAndProjectMaxInto(getAllTables(c0));
        return this.logPrE + Math.log( dest.values()[0] );
    }

    protected double computePrE(){ return Math.exp(computeLogPrMPE()); }
    public double logPrEvidence(){ makeValid(); return computeLogPrMPE(); }
    public double logPrMPE()     { makeValid(); return computeLogPrMPE(); }

    class World {
        int[] world;
        public World(Domain d) {
            this.world = new int[d.size()];
            Arrays.fill(this.world,-1);
        }

        public void put(int var, int state) {
            world[var] = state;
        }

        public IntMap subWorld(IntSet vars) {
            IntMap sw = new IntMap(vars.size());
            for (int i = 0; i < vars.size(); i++) {
                int var = vars.get(i);
                int inst = world[var];
                if ( inst > -1 ) sw.put(var,inst);
            }
            return sw;
        }

        public IntMap toIntMap() {
            IntMap w = new IntMap(world.length);
            for (int var = 0; var < world.length; var++)
                if ( world[var] > -1 ) w.put(var,world[var]);
            return w;
        }

        public int[] toIntArray() {
            return this.world;
        }
    }

    public int[] mpe() {
        makeValid();
        /*
        int numVars = domain.size();
        IntMap world = new IntMap(numVars);
        */
        World world = new World(domain);
        Object root = tree.iterator().next();
        computeMPE(world,root,null);
        return world.toIntArray();
    }

    protected void computeMPE(World world, Object cur, Object last) {
        int cluster_index = cur.hashCode();
        computeMPEInCluster(world, cluster_index);
        for (Iterator iter=tree.neighbors(cur).iterator(); iter.hasNext();) {
            Object n = iter.next();
            if( !n.equals(last) ) computeMPE(world,n,cur);
        }
    }

    protected void computeMPEInCluster(World world, int cluster_index) {
        IntSet cvars = clusters[cluster_index];
        Table[] tables = getAllTables(cluster_index);
        IntMap subWorld = world.subWorld(cvars);
        // don't bother if all cluster vars are already set
        if ( cvars.size() == subWorld.size() ) return;
        tables = Table.shrink(tables,subWorld); //OPT1
        Table table = Table.multiplyAll(Arrays.asList(tables));
        //table = table.shrink(subWorld); //OPT2
        int[] inst = table.maxAssignment();
        IntSet vars = table.vars();
        for (int i = 0; i < vars.size(); i++)
            world.put(vars.get(i),inst[i]);
    }
}
