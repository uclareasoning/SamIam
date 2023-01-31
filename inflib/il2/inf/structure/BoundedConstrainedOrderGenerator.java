package il2.inf.structure;
import il2.util.*;
import il2.model.*;

public class BoundedConstrainedOrderGenerator extends ConstrainedMinFillOrderGenerator{
    double weightBound;
    public BoundedConstrainedOrderGenerator(Graph g,Domain d,IntSet postponeSet,double weightCutOff){
	super(g,d,postponeSet);
	weightBound=weightCutOff;
    }

    protected boolean isBetterValue(int x,int y){
	if(constrained[x]==constrained[y]){
	    return super.isBetterValue(x,y);
	}
	if(constrained[x]){
	    if(weights[y]>weightBound){
		return super.isBetterValue(x,y);
	    }else{
		return false;
	    }
	}else{
	    if(weights[x]<=weightBound){
		return true;
	    }else{
		return super.isBetterValue(x,y);
	    }
	}
    }
}
	
