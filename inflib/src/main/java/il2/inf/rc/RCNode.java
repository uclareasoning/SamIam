package il2.inf.rc;
import il2.util.*;
import il2.model.*;

abstract class RCNode{
    protected int[] globalInstance;
    protected IntSet clusterVars;
    protected Index contextIndex;
    protected Domain domain;
    protected Integer dgraphNodeLabel;
    RCNode(Domain d,IntSet vars,int[] instance,Integer node){
	domain=d;
	clusterVars=vars;
	globalInstance=instance;
	dgraphNodeLabel=node;
    }

    protected abstract void initialize(IntSet context);
    protected abstract double rc(int ind);
    protected abstract void setCaching(boolean allocate);
    protected abstract void invalidate();
    public Index context(){
	return contextIndex;
    }
    public Integer getDGraphNodeLabel()
    {
    	return dgraphNodeLabel;
    }
}
