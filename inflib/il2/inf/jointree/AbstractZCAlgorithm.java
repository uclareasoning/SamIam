package il2.inf.jointree;
import il2.util.*;
import il2.model.*;
import il2.inf.structure.*;
import java.util.*;
import il2.inf.PartialDerivativeEngine;

public abstract class AbstractZCAlgorithm extends JoinTreeAlgorithm implements PartialDerivativeEngine{

    Table[] clusterTables;
    boolean[][] clusterZeroFree;
    Table[] separatorTables;


    public AbstractZCAlgorithm(EliminationOrders.JT jointree,Table[] tables){
	super(jointree,tables);
	allocateClusters();
	allocateSeparators();
    }

    private void allocateClusters(){
	clusterTables=new Table[clusters.length];
	clusterZeroFree=new boolean[clusters.length][];
	for(int i=0;i<clusterTables.length;i++){
	    clusterTables[i]=new Table(domain,clusters[i]);
	    clusterZeroFree[i]=new boolean[clusterTables[i].sizeInt()];
	}
    }
    private void allocateSeparators(){
	separatorTables=new Table[messageOrder.length];
	for(int i=0;i<messageOrder.length;i++){
	    separatorTables[i]=new Table(domain,(IntSet)separators.get(new UPair(messageOrder[i])));
	}
    }

    protected void initialize(){
	for(int i=0;i<clusterTables.length;i++){
	    initializeCluster(i);
	}
    }

    protected double computePrE(){
	double values[]=clusterTables[smallestCluster].values();
	boolean[] zf=clusterZeroFree[smallestCluster];
	double total=0;
	for(int i=0;i<values.length;i++){
	    if(zf[i]){
		total+=values[i];
	    }
	}
	return total;
    }

    protected Table computeVarJoint(int var){
	int cluster=containingClusters[var].get(0);
	return clusterTables[cluster].zeroConciousRealProjectOnto(var,clusterZeroFree[cluster]);
    }
    public Table varPartial(int var){
	makeValid();
	if(!evidence.keys().contains(var)){
	    return computeVarJoint(var);
	}
	int cluster=containingClusters[var].get(0);
	boolean[] retracted=new boolean[domain.size(var)];
	Table result=clusterTables[cluster].zeroConciousProjectOnto(var,clusterZeroFree[cluster],retracted);
	double[] vals=new double[retracted.length];
	vals[evidence.get(var)]=1;
	result.zeroConciousDivideRelevantInto(vals,retracted);
	return result;
    }

    protected abstract void initializeCluster(int c);

    protected double getTableSizes(){
	double total=0;
	for(int i=0;i<clusterTables.length;i++){
	    total+=clusterTables[i].sizeDouble()*8.125;
	}
	for(int i=0;i<separatorTables.length;i++){
	    total+=separatorTables[i].sizeDouble()*8;
	}
	return total;
    }
}
