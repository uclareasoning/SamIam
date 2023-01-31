package il2.inf.structure;
import il2.model.*;
import il2.util.*;

abstract class EliminationOrderGenerator{

    private IndexedHeap heap=new IndexedHeap(){
	    protected boolean hasBetterValue(int x,int y){
		return isBetterValue(x,y);
	    }
	};

    protected Graph graph;
    protected Domain domain;
    protected double[] weights;
    protected double[] logSizes;
    private double largestCluster=0;
    private IntList order;
    private IntSet[] clusters;
    


    EliminationOrderGenerator(Graph g,Domain d){
	graph=g;
	domain=d;
        clusters=new IntSet[g.size()];
    }

    protected abstract boolean isBetterValue(int x,int y);
    protected abstract void update(int x);
    protected abstract void initializeValues();
    private void initializeWeights(){
	logSizes=new double[domain.size()];
	for(int i=0;i<logSizes.length;i++){
	    logSizes[i]=domain.logSize(i);
	}
	weights=new double[domain.size()];
	IntSet vertices=graph.vertices();
	for(int i=0;i<vertices.size();i++){
	    updateWeight(vertices.get(i));
	}
    }
	
    protected void updateWeight(int x){
	IntSet neighbors=graph.neighbors(x);
	weights[x]=logSizes[x];
	for(int i=0;i<neighbors.size();i++){
	    weights[x]+=logSizes[neighbors.get(i)];
	}
    }

    IntList getOrder(){
	if(order==null){
	    computeOrder();
	}
	return order;
    }
    IntSet[] getClusters(){
        return clusters;
    }
    
    double eliminationSize(){
	if(order==null){
	    computeOrder();
	}
	return largestCluster;
    }

    double eliminationWidth(){
	return eliminationSize()-1;
    }

    protected abstract IntSet getUpdateNodes(int eliminationNode);

    private void computeOrder(){
	initializeWeights();
	initializeValues();
	heap.initialize(graph.vertices());
	order=new IntList(graph.size());
	while(!heap.isEmpty()){
	    int best=heap.removeBest();
	    order.add(best);
	    if(weights[best]>largestCluster){
		largestCluster=weights[best];
	    }
	    IntSet neighbors=getUpdateNodes(best);
            clusters[order.size()-1]=neighbors.union(IntSet.singleton(best));
	    graph.removeAndConnect(best);
	    for(int i=0;i<neighbors.size();i++){
		int n=neighbors.get(i);
		updateWeight(n);
		update(n);
		heap.valueUpdated(n);
	    }
	}
    }
}
