package il2.inf.mini;
import il2.util.*;
import il2.model.*;
import il2.inf.structure.*;
import java.util.*;
public class MiniTree{

    private Node root;
    private Node[] leaves;
    private boolean isExact;

    public MiniTree( Table[] baseTables, int reps, Random seed, double widthBound ){
	isExact=true;
	double sizeBound=Math.pow(2,widthBound+1);
	leaves=new Node[baseTables.length];
	for(int i=0;i<leaves.length;i++){
	    leaves[i]=new Node(baseTables[i],i);
	}
	EliminationOrders.Record er = EliminationOrders.minFill( Arrays.asList(baseTables), reps, seed );
	Bucketer b=new Bucketer(er.order);
	b.placeInBuckets(Arrays.asList(leaves));
	for(int i=0;i<b.lastBucket();i++){
	    List newResults=partition(b.getBucket(i),b.bucketLabel(i),sizeBound);
	    if(newResults.size()>1){
		isExact=false;
	    }
	    b.placeInBuckets(newResults);
	}
	List lastElements=b.getBucket(b.lastBucket());
	if(lastElements.size()==1){
	    root=(Node)lastElements.get(0);
	}else{
	    root=combineLast(baseTables[0].domain(),lastElements);
	}
	root.partial.values()[0]=1;
    }

    public double getValue(){
	return root.evaluate().values()[0];
    }


    public Table getLeafDerivative(int leaf){
	return leaves[leaf].differentiate();
    }

    public Table getLeafValue(int leaf){
	return leaves[leaf].evaluate();
    }

    public boolean isExact(){
	return isExact;
    }


    public void valueChanged(int leaf){
	leaves[leaf].invalidateValue();
    }

    public int[] dfsLeafOrder(){
	LinkedList lst=new LinkedList();
	root.dfsOrder(lst);
	int[] result=new int[lst.size()];
	for(int i=0;i<result.length;i++){
	    Integer val=(Integer)lst.removeFirst();
	    result[i]=val.intValue();
	}
	return result;
    }
    private Node combineLast(Domain d,List lastElements){
	Index ind=new Index(d,new IntSet());
	return new Node(ind,lastElements,1);
    }


    private List partition(List bucketContents,int bucketLabel,double sizeBound){
	Partition p = Partition.miniPartition(getIndices(bucketContents),sizeBound);
	List[] buckets=new List[p.indices.length];
	for(int i=0;i<buckets.length;i++){
	    buckets[i]=new LinkedList();
	}
	for(int i=0;i<bucketContents.size();i++){
	    buckets[p.mappings[i]].add(bucketContents.get(i));
	}
	List result=new ArrayList(buckets.length);
	result.add(
		   new Node(
			    p.indices[0].forgetIndex(bucketLabel),
			    buckets[0],
			    1));
	double scaleFactor=1.0/p.indices[0].domain().size(bucketLabel);
	for(int i=1;i<buckets.length;i++){
	    result.add(
		       new Node(
				p.indices[i].forgetIndex(bucketLabel),
				buckets[i],
				scaleFactor));
	}
	return result;
    }


    private Index[] getIndices(List elements){
	Index[] result=new Index[elements.size()];
	for(int i=0;i<result.length;i++){
	    result[i]=((Node)elements.get(i)).value;
	}
	return result;
    }




    private static class Node implements SubDomain{
	Table value;
	boolean valueValid;
	Table partial;
	boolean partialValid;
	Node parent;
	Node[] children;
	double scaleFactor;

	int leafId=-1;

	private Node(Table v,int leafID){
	    partialValid=false;
	    value=v;
	    partial=Table.createCompatible(v);
	    valueValid=true;
	    scaleFactor=1;
	    leafId=leafID;
	}

	private Node(Index ind,List childNodes,double scaleFactor){
	    children=new Node[childNodes.size()];
	    childNodes.toArray(children);
	    value=Table.createCompatible(ind);
	    partial=Table.createCompatible(ind);
	    valueValid=false;
	    partialValid=false;
	    this.scaleFactor=scaleFactor;
	    for(int i=0;i<children.length;i++){
		children[i].parent=this;
	    }
	}

	Table evaluate(){
	    if(!valueValid && children!=null){
		for(int i=0;i<children.length;i++){
		    children[i].evaluate();
		}
		Table[] tables=getChildTables();
		value.multiplyAndProjectInto(tables);
		if(scaleFactor!=1){
		    value.multiplyByConstant(scaleFactor);
		}
	    }
	    valueValid=true;
	    return value;
	}

	List dfsOrder(List l){
	    if(children==null){
		l.add(new Integer(leafId));
	    }else{
		for(int i=0;i<children.length;i++){
		    children[i].dfsOrder(l);
		}
	    }
	    return l;
	}

	Table differentiate(){
	    if(!partialValid){

		if(parent==null){
		    partial.values()[0]=1;
		}else{
		    Table[] t=getPartialDependencies();
		    partial.multiplyAndProjectInto(t);
		    if(parent.scaleFactor!=1){
			partial.multiplyByConstant(parent.scaleFactor);
		    }
		}
		partialValid=true;
	    }
	    return partial;
	}

	private Table[] getChildTables(){
	    Table[] result=new Table[children.length];
	    for(int i=0;i<result.length;i++){
		result[i]=children[i].evaluate();
	    }
	    return result;
	}

	private Table[] getPartialDependencies(){
	    Table[] result=new Table[parent.children.length];
	    for(int i=0;i<parent.children.length;i++){
		if(parent.children[i]==this){
		    result[i]=parent.differentiate();
		}else{
		    result[i]=parent.children[i].evaluate();
		}
	    }
	    return result;
	}


	void invalidateValue(){
	    valueValid=false;
	    if(parent!=null){
		for(int i=0;i<parent.children.length;i++){
		    if(parent.children[i]!=this){
			parent.children[i].invalidatePartial();
		    }
		}
		parent.invalidateValue();
	    }
	}



	void invalidatePartial(){
	    partialValid=false;
	    if(children!=null){
		for(int i=0;i<children.length;i++){
		    children[i].invalidatePartial();
		}
	    }
	}


	public Domain domain(){
	    return value.domain();
	}


	public IntSet vars(){
	    return value.vars();
	}
    }
}
