package il2.inf.jointree;
import il2.util.*;
import il2.inf.structure.*;
import il2.model.*;
import java.util.*;

public abstract class AbstractHuginAlgorithm extends JoinTreeAlgorithm{

    Table[] clusterTables;
    Table[] separatorTables;

    public AbstractHuginAlgorithm(EliminationOrders.JT jointree,Table[] tables){
	super(jointree,tables);
	allocateClusters();
	allocateSeparators();
    }

    private void allocateClusters(){
	clusterTables=new Table[clusters.length];
	for(int i=0;i<clusterTables.length;i++){
	    clusterTables[i]=new Table(domain,clusters[i]);
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
	return clusterTables[smallestCluster].sum();
    }

    protected Table computeVarJoint(int var){
	return clusterTables[containingClusters[var].get(0)].projectOnto(var);
    }
    protected double getTableSizes(){
	double total=0;
	for(int i=0;i<clusterTables.length;i++){
	    total+=clusterTables[i].sizeDouble();
	}
	for(int i=0;i<separatorTables.length;i++){
	    total+=separatorTables[i].sizeDouble();
	}
	return total*8;
    }

    protected abstract void initializeCluster(int c);
}
