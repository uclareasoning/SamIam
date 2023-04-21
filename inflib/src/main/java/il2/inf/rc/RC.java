package il2.inf.rc;
import java.util.*;
import il2.util.*;
import il2.inf.structure.*;
import il2.model.*;

public class RC{
    private DGraph dgraph;
    private Map rcNodes;
    private int[] globalInstance;
    private Domain domain;
    double propagationTime;

    public RC(DGraph dg,Table[] leafValues){
	if(dg.tree().size()!=(2*leafValues.length-2)){
	    throw new IllegalArgumentException();
	}
	dgraph=dg;
	domain=leafValues[0].domain();
	rcNodes=new HashMap(dg.tree().size()*3);
	globalInstance=new int[domain.size()];
	for(int i=0;i<leafValues.length;i++){
	    createIncomingRC(i,leafValues);
	}
    }

    private void createIncomingRC(int ind,Table[] leaves){
	Integer n=new Integer(ind);
	Integer child=(Integer)(dgraph.tree().neighbors(n).iterator().next());
	RCNode node=createRCSubtree(child,n,leaves);
	node.initialize(dgraph.separator(n.intValue(),child.intValue()).vars());
    }

    private RCNode getRCSubtree(Pair p){
	return (RCNode)rcNodes.get(p);
    }
    private RCNode getRCSubtree(Integer from,Integer to){
	return (RCNode)rcNodes.get(new Pair(from,to));
    }

    private RCNode createRCSubtree(Integer from,Integer to,Table[] leaves){
	Pair p=new Pair(from.intValue(),to.intValue());
	if(rcNodes.containsKey(p)){
	    return (RCNode)rcNodes.get(p);
	}else{
	    RCNode result;
	    if(dgraph.tree().degree(from)==3){
		Integer[] ch=dgraph.children(p);
		result=new InternalRCNode(
				      domain,
				      createRCSubtree(ch[0],from,leaves),
				      createRCSubtree(ch[1],from,leaves),
				      dgraph.cluster(from).vars(),
				      globalInstance,
				      from);
	    }else{
		result=new LeafRCNode(
				      leaves[from.intValue()],
				      globalInstance,
				      from);
	    }
	    rcNodes.put(p,result);
	    return result;
	}
    }

    public void fullCaching(){
	for(Iterator iter=dgraph.separators().keySet().iterator();iter.hasNext();){
	    UPair sep=(UPair)iter.next();
	    Pair p1=new Pair(sep.s1,sep.s2);
	    Pair p2=new Pair(sep.s2,sep.s1);
	    getRCSubtree(p1).setCaching(true);
	    getRCSubtree(p2).setCaching(true);
	}
    }
    public void allocateCaches(Set caches){
	for(Iterator iter=dgraph.separators().keySet().iterator();iter.hasNext();){
	    UPair sep=(UPair)iter.next();
	    Pair p1=new Pair(sep.s1,sep.s2);
	    Pair p2=new Pair(sep.s2,sep.s1);
	    getRCSubtree(p1).setCaching(caches.contains(p1));
	    getRCSubtree(p2).setCaching(caches.contains(p2));
	}
    }

    public void invalidate(int tableIndex){
	Integer node=new Integer(tableIndex);
	Integer neighbor=(Integer)dgraph.tree().neighbors(node).iterator().next();
	invalidateOutGoing(new Pair(tableIndex,neighbor.intValue()));
	propagationTime=0;
    }

    public double value(){
	UPair rootEdge=dgraph.root();
	Integer s1=new Integer(rootEdge.s1);
	Integer s2=new Integer(rootEdge.s2);
	RCNode c1=getRCSubtree(s1,s2);
	RCNode c2=getRCSubtree(s2,s1);

	long start=System.currentTimeMillis();
	RCNode root=new InternalRCNode(domain,c1,c2,c1.context().vars(),globalInstance,new Integer(-1));
	root.initialize(new IntSet());
	double result=root.rc(0);
	long finish=System.currentTimeMillis();
	propagationTime+=(finish-start)/1000.0;
	return result;
    }



    private void verifyAtInitialInstance(){
	for(int i=0;i<globalInstance.length;i++){
	    if(globalInstance[i]!=0){
		for(int j=0;j<globalInstance.length;j++){
		    System.err.println(" "+globalInstance[j]);
		}
		throw new IllegalStateException();
	    }
	}
    }


    public Table getIncoming(int tableIndex){
	Integer to=new Integer(tableIndex);
	Integer from=(Integer)dgraph.tree().neighbors(to).iterator().next();
	RCNode node=getRCSubtree(from,to);
	Index context=dgraph.separator(tableIndex,from.intValue());
	long start=System.currentTimeMillis();
	InstanceIterator iter=new InstanceIterator(domain,globalInstance,context.vars());
	int ind=0;
	double[] result=new double[context.sizeInt()];
	for(int i=0;;i++){
	    result[i]=node.rc(i);
	    int v=iter.next();
	    if(v==-1){
		break;
	    }
	}
	long finish=System.currentTimeMillis();
	propagationTime+=(finish-start)/1000.0;
	return new Table(context,result);
    }

    private void invalidateOutGoing(Pair p){
	propagationTime=0;
	getRCSubtree(p).invalidate();
	Integer[] others=dgraph.parents(p);
	for(int i=0;i<others.length;i++){
	    invalidateOutGoing(new Pair(p.s2,others[i].intValue()));
	}
    }

    private LeafRCNode leaf(int leafInd){
	Integer lind=new Integer(leafInd);
	Integer neighbor=(Integer)dgraph.tree().neighbors(lind).iterator().next();
	return (LeafRCNode)getRCSubtree(lind,neighbor);
    }

    public void setTable(int leafInd,Table t){
	leaf(leafInd).setTable(t);
	invalidate(leafInd);
    }

    public Table getTable(int leafInd){
	return leaf(leafInd).getTable();
    }

    public Domain domain(){
	return domain;
    }

    public DGraph dgraph(){
	return dgraph;
    }
}
