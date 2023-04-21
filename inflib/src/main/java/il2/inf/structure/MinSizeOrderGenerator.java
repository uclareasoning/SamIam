package il2.inf.structure;
import il2.util.*;
import il2.model.*;

class MinSizeOrderGenerator extends EliminationOrderGenerator{
    MinSizeOrderGenerator(Graph g,Domain d){
	super(g,d);
    }

    protected void update(int x){
    }

    protected void initializeValues(){
    }

    protected boolean isBetterValue(int x,int y){
	return weights[x]<weights[y];
    }

    protected IntSet getUpdateNodes(int elimNode){
	return new IntSet(graph.neighbors(elimNode));
    }
	
	
}
