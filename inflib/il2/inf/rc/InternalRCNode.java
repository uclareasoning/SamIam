package il2.inf.rc;
import il2.util.*;
import il2.inf.structure.*;
import il2.model.*;

class InternalRCNode extends RCNode{

    private RCNode child1,child2;
    private double[] cache;

    private IntSet context;

    private InstanceIterator iter;

    private InstanceIterator.IterationRec child1Rec;
    private InstanceIterator.IterationRec child2Rec;

    private int[] flipChange1;
    private int[] flipChange2;

    public RCNode left() { return child1; };
    public RCNode right() { return child2; };

    InternalRCNode(Domain d,RCNode c1,RCNode c2,IntSet cv,int[] instance,Integer node){
	super(d,cv,instance,node);
	child1=c1;
	child2=c2;
	c1.initialize(cv.intersection(c1.clusterVars));
	c2.initialize(cv.intersection(c2.clusterVars));
    }

    protected void setCaching(boolean allocate){
	if(allocate){
	    cache=new double[contextIndex.sizeInt()];
	    java.util.Arrays.fill(cache,-1);
	}else{
	    cache=null;
	}
    }

    protected void initialize(IntSet cntxt){
	if(context!=null){
	    if(context.equals(cntxt)){
		return;
	    }else{
		throw new IllegalStateException("Incompatible contexts");
	    }
	}
	if(!clusterVars.containsAll(cntxt)){
	    throw new IllegalStateException("Evil context");
	}
	context=cntxt;
	contextIndex=new Index(domain,context);
	IntSet cutset=clusterVars.diff(cntxt);

	if(!child1.clusterVars.containsAll(cutset)){
	    java.io.PrintStream stream = System.out;
	    stream.println("cluster:"+clusterVars);
	    stream.println("context:"+context);
	    stream.println("cutset:"+cutset);
	    stream.println("child1 cluster: "+child1.clusterVars);
	    stream.println("child2 cluster: "+child2.clusterVars);
	    throw new IllegalStateException();
	}
	if(!child2.clusterVars.containsAll(cutset)){
	    java.io.PrintStream stream = System.out;
	    stream.println("cluster:"+clusterVars);
	    stream.println("context:"+context);
	    stream.println("cutset:"+cutset);
	    stream.println("child2 cluster: "+child2.clusterVars);
	    stream.println("child1 cluster: "+child1.clusterVars);
	    throw new IllegalStateException();
	}
	iter=new InstanceIterator(domain,globalInstance,cutset.toIntList());
	child1Rec=iter.flipChange(child1.context().vars());
	child2Rec=iter.flipChange(child2.context().vars());
	flipChange1=child1Rec.flipChange;
	flipChange2=child2Rec.flipChange;
    }

    protected double rc(int ind){
	if(cache!=null && cache[ind]>=0){
	    return cache[ind];
	}else{
	    int ind1=child1Rec.baseline();
	    int ind2=child2Rec.baseline();
	    double total=0;
	    while(true){
		total+=child1.rc(ind1)*child2.rc(ind2);
		int v=iter.next();
		if(v==-1){
		    break;
		}
		ind1+=flipChange1[v];
		ind2+=flipChange2[v];
	    }
	    if(cache!=null){
		cache[ind]=total;
	    }
	    return total;
	}
    }

    protected void invalidate(){
	if(cache!=null){
	    java.util.Arrays.fill(cache,-1);
	}
    }

}
