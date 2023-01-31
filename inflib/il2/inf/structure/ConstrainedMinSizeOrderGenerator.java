package il2.inf.structure;
import il2.util.*;
import il2.model.*;

public class ConstrainedMinSizeOrderGenerator extends MinSizeOrderGenerator{
    boolean[] constrained;
    public ConstrainedMinSizeOrderGenerator(Graph g,Domain d,IntSet eliminateLast){
	super(g,d);
	constrained=new boolean[domain.size()];
	for(int i=0;i<eliminateLast.size();i++){
	    constrained[eliminateLast.get(i)]=true;
	}
    }

    protected boolean isBetterValue(int x,int y){
	if(constrained[x]!=constrained[y]){
	    return constrained[y];
	}else{
	    return super.isBetterValue(x,y);
	}
    }
}
