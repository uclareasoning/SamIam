package il2.inf.structure;
import java.util.*;
import il2.util.*;
import il2.model.*;
import edu.ucla.structure.*;

public class DGraphs{

    private static class DDom implements SubDomain{
	Integer node;
	Index ind;

	DDom(Integer n,Index i){
	    node=n;
	    ind=i;
	}

	public Domain domain(){
	    return ind.domain();
	}
	public IntSet vars(){
	    return ind.vars();
	}
    }

    public static DGraph create( Index[] leaves, int reps, Random seed ){
	EliminationOrders.Record er = EliminationOrders.minFill( Arrays.asList(leaves), reps, seed );
	return create(leaves,er.order);
    }
    public static DGraph create(Index[] leaves,IntList eliminationOrder){
	Map clusters=new HashMap(leaves.length*3);
	List potentials=new ArrayList(leaves.length);
	edu.ucla.structure.Graph tree=new HashGraph();
	for(int i=0;i<leaves.length;i++){
	    Integer n=new Integer(i);
	    Index ind=new Index(leaves[i]);
	    clusters.put(n,ind);
	    potentials.add(new DDom(n,ind));
	    tree.add(n);
	    //System.out.println(n+"[[ "+ind.vars()+" ]]");
	}
	Bucketer b=new Bucketer(eliminationOrder);
	b.placeInBuckets(potentials);
	for(int i=0;i<=b.lastBucket();i++){
	    ArrayList contents=b.getBucket(i);

	    DDom current=(DDom)contents.get(0);
	    for(int k=1;k<contents.size();k++){
		DDom other=(DDom)contents.get(k);
		Index ind=current.ind.combineWith(other.ind);
		Integer n=new Integer(tree.size());
		//System.out.println(current.node+"->"+n);
		//System.out.println(other.node+"->"+n);
		//System.out.println(n+"[[ "+ind.vars()+" ]]");
		clusters.put(n,ind);
		tree.addEdge(current.node,n);
		tree.addEdge(other.node,n);
		current=new DDom(n,ind);
	    }
	    if(i<b.lastBucket()){
		//System.out.println("eliminating "+b.bucketLabel(i));
		DDom newNode=new DDom(current.node,current.ind.forgetIndex(b.bucketLabel(i)));
		b.placeInBucket(newNode,i);
	    }else{
		Integer node=current.node;
		Set neighbors=new HashSet(tree.neighbors(node));
		if(neighbors.size()!=2){
		    throw new IllegalStateException();
		}
		Iterator iter=neighbors.iterator();
		Object n1=iter.next();
		Object n2=iter.next();
		//System.out.println("connecting "+n1+" to "+n2);
		tree.remove(node);
		tree.addEdge(n1,n2);
		clusters.remove(node);

	    }
	}
	return new DGraph(tree,reduce(clusters,tree));
    }

    static Map reduce(Map clusters,edu.ucla.structure.Graph tree){
	Set open=new HashSet(tree.size());
	for(Iterator iter=tree.iterator();iter.hasNext();){
	    Object node=iter.next();
	    if(tree.neighbors(node).size()>1){
		open.add(node);
	    }
	}
	while(open.size()!=0){
	    Object node=open.iterator().next();
	    open.remove(node);
	    IntSet singleOccurances=findSingleOccurances(node,tree,clusters);
	    if(singleOccurances.size()!=0){
		Index ind=(Index)clusters.get(node);
		clusters.put(node,ind.forgetIndex(singleOccurances));
		for(Iterator iter=tree.neighbors(node).iterator();iter.hasNext();){
		    Object n=iter.next();
		    if(tree.neighbors(n).size()>1){
			open.add(n);
		    }
		}
	    }
	}
	return clusters;
    }

    private static IntSet findSingleOccurances(Object node,edu.ucla.structure.Graph tree,Map clusters){
	IntSet vars=((Index)clusters.get(node)).vars();
	int[] count=new int[vars.size()];
	for(Iterator iter=tree.neighbors(node).iterator();iter.hasNext();){
	    IntSet nvars=((Index)clusters.get(iter.next())).vars();
	    for(int i=0;i<nvars.size();i++){
		int ind=vars.indexOf(nvars.get(i));
		if(ind>=0){
		    count[ind]++;
		}
	    }
	}
	IntSet result=new IntSet(vars.size());
	for(int i=0;i<count.length;i++){
	    if(count[i]<=1){
		result.add(vars.get(i));
	    }
	}
	return result;
    }



}
