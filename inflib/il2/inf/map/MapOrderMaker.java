package il2.inf.map;
import java.util.*;
import il2.util.*;
import il2.model.*;
import il2.inf.structure.*;

public class MapOrderMaker{

    public static IntList order(Collection potentials,IntSet mv,IntList orderHint){
	return new MapOrderMaker(potentials,mv,orderHint).getOrder();
    }
    public static IntList order(Collection potentials,IntSet mv,double bound,IntList orderHint){
	return new MapOrderMaker(potentials,mv,bound,orderHint).getOrder();
    }

    public static MapJoinTree mapJoinTree(Collection potentials,IntSet mv,IntList orderHint,double promotionWidthBound){
        MapOrderMaker mom=new MapOrderMaker(potentials,mv,promotionWidthBound,orderHint);
        return new MapJoinTree(mom.tree, mom.root,potentials, mv);
    }

    EliminationOrders.JT tree;
    IntSet mapvars;
    Domain domain;
    int root;
    double bound;

    Map changeCosts;
    int largestCluster;

    private MapOrderMaker(Collection potentials,IntSet mv,IntList order){
	this(potentials,mv,-1,order);
    }
    private MapOrderMaker(Collection potentials,IntSet mv,double widthBound,IntList order){
        tree=EliminationOrders.traditionalJoinTree( potentials, order, (il2.bridge.Converter)null, (BayesianNetwork)null );
	domain=((Table)potentials.iterator().next()).domain();
        mapvars=mv;
	largestCluster=findLargestRoot();
	if(widthBound<0){
	    this.bound=domain.size(getCluster(largestCluster));
	}else{
	    this.bound=Math.pow(2,widthBound);
	}
        root=chooseBestRoot();
        promoteMapVars();
	computeChangeCosts();
    }

    private void computeChangeCosts(){
        changeCosts=new HashMap(200);
	computeChangeCosts(root,-1,0);
    }
    private void computeChangeCosts(int node,int parent,double parentCost){
	double cost=parentCost+domain.size(getCluster(node));
	changeCosts.put(new Integer(node),new Double(cost));
	IntSet neighbors=getOtherNeighbors(node,parent);
	for(int i=0;i<neighbors.size();i++){
	    computeChangeCosts(neighbors.get(i),node,cost);
	}
    }

    private IntList largestToSmallest(IntSet treeNodes){
	double[] values=new double[treeNodes.size()];
	for(int i=0;i<values.length;i++){
	    values[i]=((Double)changeCosts.get(new Integer(treeNodes.get(i)))).doubleValue();
	}
	int[] inds=ArrayUtils.sortedInds(values);
	IntList result=new IntList(values.length);
	for(int i=0;i<inds.length;i++){
	    result.add(treeNodes.get(inds[i]));
	}
	return result;
    }
    private double getSize(IntSet vars){
	return domain.size(vars);
    }
    private IntList getOrder(){
	return getOrder(root,-1);
    }

    private IntSet getEliminator(int node,int parent){
	IntSet s=new IntSet(getCluster(node));
	return s.diff(getCluster(parent));
    }

    private IntSet getCluster(int node){
	return (IntSet)tree.clusters.get(new Integer(node));
    }

    private IntList getOrder(int node,int parent){
	IntSet eliminator;
	if(node==root){
	    eliminator=new IntSet(getCluster(node));
	}else{
	    eliminator=getEliminator(node,parent);
	}
	IntList neighbors=largestToSmallest(getOtherNeighbors(node,parent));
	IntList[] results=new IntList[neighbors.size()];
	int length=eliminator.size();
	for(int i=0;i<neighbors.size();i++){
	    results[i]=getOrder(neighbors.get(i),node);
	    length+=results[i].size();
	}
	int[] current=new int[results.length];
	IntList result=new IntList(length);
	for(int i=0;i<results.length;i++){
	    current[i]=0;
	    while(current[i]<results[i].size() && !mapvars.contains(results[i].get(current[i]))){
		result.add(results[i].get(current[i]));
		current[i]++;
	    }
	}
	for(int i=0;i<results.length;i++){
	    while(current[i]<results[i].size()){
		result.add(results[i].get(current[i]));
		current[i]++;
	    }
	}
	IntSet localSV=eliminator.diff(mapvars);
	IntList localMV=smallestToLargestVars(eliminator.diff(localSV));
	for(int i=0;i<localSV.size();i++){
	    result.add(localSV.get(i));
	}
	for(int i=localMV.size()-1;i>=0;i--){
	    result.add(localMV.get(i));
	}
	return result;
    }

    private IntSet neighbors(int node){
	return tree.tree.neighbors(node);
    }

    private IntSet getOtherNeighbors(int node,int parent){
	IntSet result=new IntSet(neighbors(node));
	result.remove(parent);
	return result;
    }



    private int findLargestRoot(){
	IntSet v=tree.tree.vertices();
	double bestScore=0;
	int best=-1;
	for(int i=0;i<v.size();i++){
	    Integer vi=new Integer(v.get(i));
	    IntSet vars=(IntSet)tree.clusters.get(vi);
	    double sz=domain.size(vars);
	    if(sz>bestScore){
		bestScore=sz;
		best=vi.intValue();
	    }
	}
	return best;
    }


    private double findParticipation(Map participation,int node,int parent){
	IntSet neighbors=getOtherNeighbors(node,parent);
	for(int i=0;i<neighbors.size();i++){
	    findParticipation(participation,neighbors.get(i),node);
	}
	IntSet mv=getCluster(node).intersection(mapvars);
	for(int i=0;i<neighbors.size();i++){
	    mv=mv.union((IntSet)participation.get(new Integer(neighbors.get(i))));
	}
        participation.put(new Integer(node),mv);
	return domain.size(mv);
    }
    private int chooseBestRoot(){

        IntSet neighbors=neighbors(largestCluster);
        double bestSize=0;
        int best=-1;
	Map participation=new HashMap(tree.tree.size());

	int neighbori = (int)-1;
        for(int i=0;i<neighbors.size();i++){
        	neighbori = neighbors.get(i);
            double sz=findParticipation(participation,neighbori,largestCluster);
            if(sz>bestSize){
                bestSize=sz;
                best=neighbori;
            }
        }
	double target=Math.sqrt(bestSize);
	int nextChosen=best;
	double nextSize=bestSize;
	int previous;
	int current=largestCluster;
	while(nextSize>target && nextChosen>=0){
	    previous=current;
	    current=nextChosen;
	    nextChosen=-1;
	    neighbors=getOtherNeighbors(current,previous);
	    nextSize=0;
	    for(int i=0;i<neighbors.size();i++){
		double sz=getSize((IntSet)participation.get(new Integer(neighbors.get(i))));
		if(sz>nextSize){
		    nextChosen=neighbors.get(i);
		    nextSize=sz;
		}
	    }
	}
	return current;
    }

    private void promoteMapVars(){
	promoteMapVars(root,-1);
    }

    private void promoteMapVars(int node,int parent){
	IntSet neighbors=getOtherNeighbors(node,parent);
	IntSet members=getCluster(node);
	double size=getSize(members);
	for(int i=0;i<neighbors.size();i++){
	    int child=neighbors.get(i);
	    promoteMapVars(child,node);
	    IntSet childmvars=mapvars.intersection(getCluster(child));

	    IntList inplay=smallestToLargestVars(childmvars.diff(members));
	    for(int j=0;j<inplay.size();j++){
		int v=inplay.get(j);
		int vsize=domain.size(v);
		if(vsize*size<=bound){
		    members.add(v);
		    size*=vsize;
		}
	    }
	}
    }

    private IntList smallestToLargestVars(IntSet vars){
        int[] sizes=new int[vars.size()];
        for(int i=0;i<sizes.length;i++){
            sizes[i]=domain.size(vars.get(i));
        }
        int[] inds=ArrayUtils.sortedInds(sizes);
        IntList result=new IntList(inds.length);
        for(int i=0;i<inds.length;i++){
            result.add(vars.get(inds[i]));
        }
        return result;
    }












}




