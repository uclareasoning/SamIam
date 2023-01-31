package il2.inf.structure;
import il2.util.*;
import il2.model.*;

public class MinFillOrderGenerator extends EliminationOrderGenerator{
    
    int[] fillCount;
    
    public MinFillOrderGenerator(Graph g,Domain d){
	super(g,d);
    }

    protected void initializeValues(){
	fillCount=new int[domain.size()];
	IntSet vertices=graph.vertices();
	for(int i=0;i<vertices.size();i++){
	    update(vertices.get(i));
	}
    }

    protected void update(int x){
	IntSet neighbors=graph.neighbors(x);
	fillCount[x]=0;
	for(int i=0;i<neighbors.size();i++){
	    for(int j=i+1;j<neighbors.size();j++){
		if(!graph.containsEdge(neighbors.get(i),neighbors.get(j))){
		    fillCount[x]++;
		}
	    }
	}
    }

    protected boolean isBetterValue(int x,int y){
	return fillCount[x]<fillCount[y] || 
              (fillCount[x]==fillCount[y] && weights[x]<weights[y]);
    }

    protected IntSet getUpdateNodes(int elimNode){
	IntSet n=new IntSet(graph.neighbors(elimNode));
	IntSet result=n;
	for(int i=0;i<n.size();i++){
	    int v1=n.get(i);
	    IntSet v1Neighbors=graph.neighbors(v1);
	    for(int j=i+1;j<n.size();j++){
		int v2=n.get(j);
		if(!graph.containsEdge(v1,v2)){
		    result=result.union(v1Neighbors.intersection(graph.neighbors(v2)));
		}
	    }
	}
	result.remove(elimNode);
	return result;
    }

}
