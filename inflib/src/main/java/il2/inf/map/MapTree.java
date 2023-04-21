package il2.inf.map;
import java.util.*;
import il2.util.*;
import il2.model.*;
import il2.inf.structure.*;

public class MapTree{
    Domain domain;
    IntSet mapvars;
    Tree tree;
    Map treeLocations;
    double[] scratch;

    public MapTree( Table[] potentials, int reps, Random seed, IntSet mapvars, double bound ){
        this( createJoinTree( potentials, reps, seed ), potentials, mapvars, bound );
    }

    public Domain domain(){
	return domain;
    }
    public IntList mapOrdering(){
	return tree.getEliminationOrder(0).order;
    }

    public double getValue(){
	tree.validateMessage();
	return tree.children[0].upMessage[0];
    }

    private static JoinTree createJoinTree( Table[] potentials, int reps, Random seed ){
	EliminationOrders.Record rec = EliminationOrders.minFill( java.util.Arrays.asList(potentials), reps, seed );
	return new JoinTree(potentials,rec.order);
    }


    public MapTree(JoinTree jt,Table[] potentials,IntSet mapvars,double bound){
	domain=jt.domain();
	this.mapvars=mapvars;
	int root=findBestRoot(jt,mapvars);
	Map treeLocations=new HashMap(2*jt.clusterCount());
	Tree t1=convertToTree(jt,root,potentials);
	if(t1.jointreeNode!=root){
	    throw new IllegalStateException();
	}
	if(root<potentials.length){
	    verifyTreeConformance(jt,t1.children[0],root);
	}else{
	    verifyTreeConformance(jt,t1);
	}
	Tree t2=liftMapVars(t1,mapvars,bound);
	Tree t3=binarize(t2);
	tree=ensureMapSummations(t3,mapvars);
	//tree=ensureMapSummations(binarize(liftMapVars(convertToTree(jt,root,potentials),mapvars,bound)),mapvars);
	tree.fix();
	tree.sanityCheck();
	scratch=new double[findBiggestTreeCluster(tree)];
    }

    private void verifyTreeConformance(JoinTree jt,Tree t){
        t=t.children[0];
	IntSet vars=jt.cluster(t.jointreeNode).vars();
	if(!vars.equals(t.vars())){
	    throw new IllegalStateException(vars+"\t"+t.vars());
	}
	int[] neighbors=neighbors(jt,t.jointreeNode);
	if(t.children.length!=neighbors.length){
	    throw new IllegalStateException(neighbors.length+"\t"+t.children.length);
	}
	for(int i=0;i<neighbors.length;i++){
	    boolean found=false;
	    for(int j=0;j<t.children.length;j++){
		if(neighbors[i]==t.children[j].jointreeNode){
		    found=true;
		    break;
		}
	    }
	    if(!found){
		System.err.println(ArrayUtils.toString(neighbors));
		for(int k=0;k<t.children.length;k++){
		    System.err.print(t.children[i].jointreeNode+" ");
		}
		System.err.println("");
		throw new IllegalStateException();
	    }
	}

	for(int i=0;i<t.children.length;i++){
	    verifyTreeConformance(jt,t.children[i],t.jointreeNode);
	}
    }
    private void verifyTreeConformance(JoinTree jt,Tree t,int parent){
	IntSet vars=jt.cluster(t.jointreeNode).vars();
	if(!vars.equals(t.vars())){
	    throw new IllegalStateException(vars+"\t"+t.vars());
	}
	int[] neighbors=otherNeighbors(jt,t.jointreeNode,parent);
	if(t.children.length!=neighbors.length){
	    throw new IllegalStateException(neighbors.length+"\t"+t.children.length);
	}
	for(int i=0;i<neighbors.length;i++){
	    boolean found=false;
	    for(int j=0;j<t.children.length;j++){
		if(neighbors[i]==t.children[j].jointreeNode){
		    found=true;
		    break;
		}
	    }
	    if(!found){
		System.err.println(ArrayUtils.toString(neighbors));
		for(int k=0;k<t.children.length;k++){
		    System.err.print(t.children[i].jointreeNode+" ");
		}
		System.err.println("");
		throw new IllegalStateException();
	    }
	}

	for(int i=0;i<t.children.length;i++){
	    verifyTreeConformance(jt,t.children[i],t.jointreeNode);
	}
    }


    public void setSelectedValue(int mapvar,int value){
	Tree t=(Tree)treeLocations.get(new Integer(mapvar));
	t.setSelectedValue(value);
    }


    public void sanityCheck(){
	tree.sanityCheck();
	for(int i=0;i<mapvars.size();i++){
	    Tree t=(Tree)treeLocations.get(new Integer(mapvars.get(i)));
	    IntSet e=t.eliminator(t.parent);
	    if(e.size()!=1 || e.get(1)!=mapvars.get(i)){
		throw new IllegalStateException();
	    }
	}
    }


    public Tree ensureMapSummations(Tree t,IntSet mapvars){
	Tree[] children=t.children();
	for(int i=0;i<children.length;i++){
	    ensureMapSummations(children[i],mapvars);
	    IntSet elim=children[i].eliminator(t);
	    IntSet s=elim.intersection(mapvars);
	    if(s.size()!=0){
		IntSet sumOut=elim.diff(s);
		children[i]=new Tree(sumOut,children[i]);
		children[i].isMapNode=true;
		children[i].mapvar=s.get(0);
		treeLocations.put(new Integer(s.get(0)),children[i]);
		IntSet c=sumOut;
	        for(int j=1;j<s.size();j++){
		    IntSet newc=new IntSet(c);
		    newc.remove(s.get(j-1));
		    children[i]=new Tree(newc,children[i]);
		    c=newc;
		    children[i].isMapNode=true;
		    children[i].mapvar=s.get(j);
		    treeLocations.put(new Integer(s.get(j)),children[i]);
		}
	    }
	}
	return t;
    }


    private int findLargestCluster(JoinTree jt){
	int best=0;
	int bestSize=0;
	for(int i=0;i<jt.clusterCount();i++){
	    int size=jt.cluster(i).sizeInt();
	    if(size>bestSize){
		bestSize=size;
		best=i;
	    }
	}
	return best;
    }

    private Tree convertToTree(JoinTree jt,int root,Table[] potentials){
	int[] n=neighbors(jt,root);
	Tree[] children=null;
	if(root<potentials.length){
	    System.err.println("root is leaf");
	    children=new Tree[n.length+1];
	    children[children.length-1]=new Tree(potentials[root]);
	    children[children.length-1].jointreeNode=root;
	}else{
	    children=new Tree[n.length];
	}
	for(int i=0;i<n.length;i++){
	    children[i]=convertToTree(jt,n[i],root,potentials);
	}
	System.err.println("convertToTree children.length="+children.length);
	Tree result=new Tree(new IntSet(),new Tree[]{new Tree(new IntSet(jt.cluster(root).vars()),children)});
	result.jointreeNode=root;
        result.children[0].jointreeNode=root;
	return result;
    }


    private Tree convertToTree(JoinTree jt,int node,int parent,Table[] potentials){
	if(node<potentials.length){
	    Tree result=new Tree(potentials[node]);
	    result.jointreeNode=node;
	    return result;
	}
	int[] n=otherNeighbors(jt,node,parent);
	Tree[] children=new Tree[n.length];
	for(int i=0;i<n.length;i++){
	    children[i]=convertToTree(jt,n[i],node,potentials);
	}
        Tree result;
        if(node+1==jt.root){
            Table bogoTable=new Table(domain,new IntSet(),new double[]{1});
            result=new Tree(bogoTable);
        }else{
            result=new Tree(jt.cluster(node).vars(),children);
        }
	result.jointreeNode=node;
	return result;
    }


    private void mapVarsInBranch(Map map,JoinTree jt,IntSet mapvars,int from,int to){
	int[] n=otherNeighbors(jt,to,from);
	for(int i=0;i<n.length;i++){
	    mapVarsInBranch(map,jt,mapvars,to,n[i]);
	}
	IntSet vars;
	vars=jt.cluster(to).vars().intersection(mapvars);
	for(int i=0;i<n.length;i++){
	    vars=vars.union((IntSet)map.get(new Integer(n[i])));
	}
	map.put(new Integer(to),vars);
    }

    private int[] otherNeighbors(JoinTree jt,int cluster,int exclude){
	int[] neighbors=neighbors(jt,cluster);
	if(exclude==jt.root){
	    return neighbors;
	}
	int[] result=new int[neighbors.length-1];
	int ri=0;
	for(int i=0;i<neighbors.length;i++){
	    if(neighbors[i]!=exclude){
		result[ri++]=neighbors[i];
	    }
	}
	return result;
    }

    private int[] neighbors(JoinTree jt,int cluster){
	IntSet children=jt.children(cluster);
	if(children==null){
	    children=new IntSet();
	}
	int[] result=new int[children.size()+1];
	for(int i=0;i<children.size();i++){
	    result[i]=children.get(i);
	}
	result[result.length-1]=jt.parent(cluster);
	if(result[result.length-1]==jt.root){
	    int[] temp=result;
	    result=new int[temp.length-1];
	    System.arraycopy(temp,0,result,0,result.length);
	}
	return result;
    }


    private int findBestRoot(JoinTree jt,IntSet mapvars){
	int largestCluster=findLargestCluster(jt);
	int[] n=neighbors(jt,largestCluster);
	double best=0;
	int bestNode=-1;
	Map varMap=new HashMap(jt.clusterCount());
	for(int i=0;i<n.length;i++){
	    mapVarsInBranch(varMap,jt,mapvars,largestCluster,n[i]);
	    double sz=domain.size((IntSet)varMap.get(new Integer(n[i])));
	    if(sz>best){
		best=sz;
		bestNode=n[i];
	    }
	}
	int previous=largestCluster;
	double currentSize=best;
	double bound=Math.sqrt(best);
	while(currentSize>bound){
	    int[] neibs=otherNeighbors(jt,bestNode,previous);
	    if(neibs.length==0){
		return bestNode;
	    }else{
		double highsc=0;
		int next=-1;
		for(int i=0;i<neibs.length;i++){
		    double sc=domain.size((IntSet)varMap.get(new Integer(neibs[i])));
		    if(sc>highsc){
			next=neibs[i];
			highsc=sc;
		    }
		}
		previous=bestNode;
		bestNode=next;
		currentSize=highsc;
	    }
	}
	return bestNode;
    }

    private Tree binarize(Tree t){
	if(t.isLeaf()){
	    return t;
	}
	Tree[] children=t.children;
	Tree[] bc=new Tree[children.length];
	for(int i=0;i<children.length;i++){
	    bc[i]=binarize(children[i]);
	}
        return binarize(t.vars(),bc);
    }
    private Tree binarize(IntSet clusterElements,Tree[] children){
	int elementCount=children.length;
	while(elementCount>1){
	    Pair p=findBestPair(clusterElements,children,elementCount);
	    children[p.e1]=combine(clusterElements,children[p.e1],children[p.e2]);
	    children[p.e2]=children[elementCount-1];
	    elementCount--;
	}
	return children[0];
    }
    private Pair findBestPair(IntSet elements,Tree[] trees,int valids){
	Pair result=new Pair();
	double bestScore=Double.POSITIVE_INFINITY;
	IntSet[] seps=new IntSet[valids];
	for(int i=0;i<seps.length;i++){
	    seps[i]=elements.intersection(trees[i].vars());
	}
	for(int i=0;i<valids;i++){
	    for(int j=i;j<valids;j++){
		double score=computeScore(seps[i],seps[j]);
		if(score<bestScore){
		    bestScore=score;
		    result.e1=i;
		    result.e2=j;
		}
	    }
	}
	return result;
    }
    private double computeScore(IntSet s1,IntSet s2){
	IntSet u=s1.union(s2);
	return domain.size(u)/Math.max(domain.size(s1),domain.size(s2));
    }

    private static class Pair{
	int e1,e2;
    }

    private Tree liftMapVars(Tree t,IntSet mapvars,double bound){
	if(t.children.length==0){
	    return t;
	}
	Tree[] children=t.children;
	Tree[] liftedChildren=new Tree[children.length];
	for(int i=0;i<children.length;i++){
	    liftedChildren[i]=liftMapVars(children[i],mapvars,bound);
	}
	Tree result=new Tree(t.vars,liftedChildren);
	for(int i=0;i<liftedChildren.length;i++){
	    tryToLift(result,liftedChildren[i],mapvars,bound);
	}
	return result;
    }

    private void tryToLift(Tree t,Tree child,IntSet mapvars,double bound){
	if(t.size()>=bound){
	    return;
	}
	IntSet eliminator=child.eliminator(t);
	IntSet mv=mapvars.intersection(eliminator);
	for(int i=0;i<mv.size();i++){
	    if(domain.size(mv.get(i))*t.size()<=bound){
		t.addToCluster(mv.get(i));
	    }
	}
	eliminator=child.eliminator(t);
	if(eliminator.size()==0 && !child.isLeaf()){
	    t.removeChild(child);
	    Tree[] grandChildren=child.children();
	    for(int i=0;i<grandChildren.length;i++){
		t.addChild(grandChildren[i]);
	    }
	}
    }


    private class Tree{
	int jointreeNode;
	IntSet upSeparator;
	IntSet vars;
	Tree parent;
	Tree[] children;
	Table leafTable;
	int[][] upMapping;
	int[][][] childrenIntoMapping;
	int depth;
	int size;
	double[] upMessage;
	boolean validUpMessage=false;

	boolean isMapNode;
	int mapvar;
	int selectedValue=1;

	void sanityCheck(){
	    System.err.println("sanity check");
	    System.err.println("children:"+children.length);
	    if(isMapNode){
		System.err.println("is map var "+mapvar);
	    }
	    if(children.length==0 && leafTable==null){
		throw new IllegalStateException();
	    }
	    if(children.length==0){
		System.err.println("leaf vars "+leafTable.vars());
	    }
	    if(children.length>2){
		throw new IllegalStateException("not binary");
	    }
	    for(int i=0;i<children.length;i++){
		children[i].sanityCheck();
	    }
	}
	private class Holder{
	    IntList order;
	    double maxcost;
	}
	Holder getEliminationOrder(double cost){
	    double thiscost=cost+(children.length+1)*size;
	    Holder[] orders=new Holder[children.length];
	    Holder result=new Holder();
	    if(isMapNode){
		result.maxcost=thiscost;
	    }
	    int ind=0;
	    for(int i=0;i<orders.length;i++){
		orders[i]=children[i].getEliminationOrder(thiscost);
		if(result.maxcost>orders[i].maxcost){
		    result.maxcost=orders[i].maxcost;
		    ind=i;
		}

	    }
	    System.err.println("** "+ind);
	    System.err.println(orders.length);
	    System.err.println(children.length);
	    if(orders.length>0){
		result.order=orders[ind].order;
	    }else{
		result.order=new IntList();
	    }
	    if(orders.length>1){
		result.order.addAll(orders[1-ind].order.toArray());
	    }
	    if(isMapNode){
		System.err.println("is Map node");
		result.order.add(mapvar);
	    }
	    return result;
	}
	void fix(){
	    if(parent!=null){
		upSeparator=parent.vars.intersection(vars);
		upMessage=new double[upSeparator.size()];
		Index ind=new Index(domain,upSeparator);
		Index cind=new Index(domain,vars);
		size=cind.sizeInt();
		upMapping=cind.baselineOffsetIndex(ind);
		depth=parent.depth+1;
	    }else{
		depth=0;
		size=1;
	    }
	    childrenIntoMapping=new int[children.length][][];
	    for(int i=0;i<children.length;i++){
		children[i].parent=this;
		children[i].fix();
		Index sind=new Index(domain,children[i].upSeparator);
		Index ind=new Index(domain,vars);
		childrenIntoMapping[i]=ind.baselineOffsetIndex(ind);
	    }
	}
	void setSelectedValue(int sv){
	    if(!isMapNode){
		throw new IllegalStateException();
	    }
	    selectedValue=sv;
	    invalidate();
	}
	void invalidate(){
	    validUpMessage=false;
	    if(parent!=null){
		parent.invalidate();
	    }
	}

	void validateMessage(){
	    for(int i=0;i<children.length;i++){
		if(!children[i].validUpMessage){
		    children[i].validateMessage();
		}
	    }
	    computeLocalMessage();
	    validUpMessage=true;
	}

	void selectMessage(){
	    int offset=upMapping[1][selectedValue];
	    int[] baseline=upMapping[0];
	    for(int i=0;i<upMessage.length;i++){
		upMessage[i]=scratch[baseline[i]+offset];
	    }
	}

	void maxMessage(){
	    Table.maximizeInto(scratch,upMessage,upMapping);
	}

	void sumMessage(){
    	    Table.projectInto(scratch,upMessage,upMapping);
	}

	void multiplyChildren(){
	    if(leafTable!=null){
		double[] vals=leafTable.values();
		System.arraycopy(vals,0,scratch,0,vals.length);
		return;
	    }
	    for(int i=0;i<size;i++){
		scratch[i]=1;
	    }
	    for(int i=0;i<children.length;i++){
		Table.multiplyInto(children[i].upMessage,scratch,childrenIntoMapping[i]);
	    }
	}

	private void computeLocalMessage(){
	    multiplyChildren();
	    if(isMapNode){
		if(selectedValue>=0){
		    selectMessage();
		}else{
		    maxMessage();
		}
	    }else{
		sumMessage();
	    }
	}

	Tree(IntSet v,Tree[] c){
	    vars=v;
	    children=c;
	    if(c.length==0){
		throw new IllegalStateException("creating evil tree");
	    }
	}

	Tree(Table t){
	    vars=t.vars();
	    leafTable=t;
	    children=new Tree[0];

	}

	Tree(IntSet v,Tree c){
	    vars=v;
	    children=new Tree[]{c};
	}

	double size(){
	    return domain.size(vars);
	}

	boolean isLeaf(){
	    return children.length==0;
	}

	IntSet vars(){
	    return vars;
	}

	Tree[] children(){
	    return children;
	}

	void addToCluster(int i){
	    vars.add(i);
	}

	IntSet eliminator(Tree parent){
	    return vars.diff(parent.vars);
	}

	private void addChild(Tree t){
	    Tree[] nc=new Tree[children.length+1];
	    nc[children.length]=t;
	    System.arraycopy(children,0,nc,0,children.length);
	    children=nc;
	}

	private void removeChild(Tree t){
	    for(int i=0;i<children.length;i++){
		if(t==children[i]){
		    Tree[] nc=new Tree[children.length-1];
		    System.arraycopy(children,0,nc,0,i);
		    System.arraycopy(children,i+1,nc,i,nc.length-i);
		    children=nc;
		}
		return;
	    }
	}

    }

    private int findBiggestTreeCluster(Tree t){
	int size=t.size;
	for(int i=0;i<t.children.length;i++){
	    size=Math.max(size,findBiggestTreeCluster(t.children[i]));
	}
	return size;
    }

    private Tree combine(IntSet clusterElements,Tree t1,Tree t2){
	IntSet s1=clusterElements.intersection(t1.vars);
	IntSet s2=clusterElements.intersection(t2.vars);
	IntSet r=s1.union(s2);
	return new Tree(r,new Tree[]{t1,t2});
    }

}
