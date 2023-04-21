package il2.inf.structure;

import edu.ucla.structure.*;
import edu.ucla.util.Maps;

import il2.util.UPair;
import il2.util.Pair;
import il2.util.IntSet;
import il2.model.*;

import java.util.*;
import java.math.BigInteger;

public class JoinTree{

    /**
     * Nodes should be Integers from  0..size-1
     */
    private Graph tree;

    /**
     * Mapping from Integer node to Index label.
     */
    private Map clusters;

    /**
     * Mapping from UPair to Index
     */
    private Map separators;

    private BigInteger largestClusterSize;
    private BigInteger largestSeparatorSize;

    public JoinTree(Graph tree,Map clusterLabels){
	this.tree=tree;
	this.clusters=clusterLabels;
	createSeparators();
	computeStats();
    }
    private void computeStats(){
	largestClusterSize=BigInteger.ZERO;
	for(Iterator iter=clusters.values().iterator();iter.hasNext();){
	    Index ind=(Index)iter.next();
	    largestClusterSize = largestClusterSize.max( ind.sizeBig() );
	}
	largestSeparatorSize=BigInteger.ZERO;
	for(Iterator iter=separators.values().iterator();iter.hasNext();){
	    Index ind=(Index)iter.next();
	    largestSeparatorSize = largestSeparatorSize.max( ind.sizeBig() );
	}
    }
    private void createSeparators(){
	separators=new HashMap(3*tree.size());
	for(Iterator iter=tree.iterator();iter.hasNext();){
	    Integer n1=(Integer)iter.next();
	    Index c1=(Index)clusters.get(n1);
	    for(Iterator niter=tree.neighbors(n1).iterator();niter.hasNext();){
		Integer n2=(Integer)niter.next();
		if(n1.intValue()<n2.intValue()){
		    Index c2=(Index)clusters.get(n2);
		    Index sep =c1.separatorIndex(c2);
		    separators.put(new UPair(n1.intValue(),n2.intValue()),sep);
		}
	    }
	}
    }

    public Graph tree(){
	return tree;
    }

    public Map clusters(){
	return clusters;
    }


    public Index cluster(Object node){
	return (Index)clusters.get(node);
    }
    public Index cluster(int node){
	return (Index)clusters.get(new Integer(node));
    }

    public Map separators(){
	return separators;
    }

    public Index separator(int e1,int e2){
	return (Index)separators.get(new UPair(e1,e2));
    }
    public Index separator(UPair p){
	return (Index)separators.get(p);
    }

    public JoinTree relabelNodes(Map nodeMapping){
	Graph g = tree.createIsomorphic(nodeMapping);
	Map newClusters = Maps.compose(clusters,nodeMapping);
	return new JoinTree(g,newClusters);
    }


    public Integer[] children(Pair p){
	Set s=new HashSet(tree().neighbors(new Integer(p.s1)));
	s.remove(new Integer(p.s2));
	Integer[] result=new Integer[s.size()];
	s.toArray(result);
	return result;
    }

    public Integer[] parents(Pair p){
	Set s=new HashSet(tree().neighbors(new Integer(p.s2)));
	s.remove(new Integer(p.s1));
	Integer[] result=new Integer[s.size()];
	s.toArray(result);
	return result;
    }




    private Map orderedVarLists;
    private Integer smallestCluster;

    private void initializeOrderedVarList(){
	orderedVarLists=new HashMap(3*tree.size());
	Comparator sizeComparator=new Comparator(){
		public int compare(Object o1,Object o2){
		    int s1=((Index)clusters.get(o1)).sizeInt();
		    int s2=((Index)clusters.get(o2)).sizeInt();
		    if(s1<s2){
			return -1;
		    }else if(s1==s2){
			return 0;
		    }else{
			return 1;
		    }
		}
	    };
	for(Iterator iter=tree.iterator();iter.hasNext();){
	    Object node=iter.next();
	    Index ind=(Index)clusters.get(node);
	    for(int i=0;i<ind.vars().size();i++){
		Integer v=new Integer(ind.vars().get(i));
		Set nodesContaining=(Set)orderedVarLists.get(v);
		if(nodesContaining==null){
		    nodesContaining=new TreeSet(sizeComparator);
		    orderedVarLists.put(v,nodesContaining);
		}
		nodesContaining.add(v);
	    }
	}
	double smallestSize=Double.POSITIVE_INFINITY;
	for(Iterator iter=orderedVarLists.values().iterator();iter.hasNext();){
	    Object node=((Set)iter.next()).iterator().next();
	    double size=((Index)clusters.get(node)).sizeInt();
	    if(size<smallestSize){
		smallestSize=size;
		smallestCluster=(Integer)node;
	    }
	}
    }

    public Integer smallestClusterContaining(IntSet vars){
	if(orderedVarLists==null){
	    initializeOrderedVarList();
	}
	if(vars.size()==0){
	    return smallestCluster;
	}else{
	    Collection varClusters=(Collection)orderedVarLists.get(new Integer(vars.get(0)));
	    if(varClusters==null){
		throw new IllegalArgumentException("No containing cluster exists");
	    }
	    for(Iterator iter=varClusters.iterator();iter.hasNext();){
		Object node=iter.next();
		Index ind=(Index)clusters.get(node);
		if(ind.vars().containsAll(vars)){
		    return (Integer)node;
		}
	    }
	    throw new IllegalArgumentException("No containing cluster exists");
	}
    }
    public Integer leafNeighbor(int leaf){
	Integer i=new Integer(leaf);
	Set neighbors=tree.neighbors(i);
	if(neighbors.size()!=1){
	    throw new IllegalArgumentException("not a leaf");
	}
	return (Integer)neighbors.iterator().next();
    }

    public boolean isLeaf(int i){
	return tree.neighbors(new Integer(i)).size()<2;
    }

    public BigInteger largestClusterSize(){
	return largestClusterSize;
    }

    public BigInteger largestSeparatorSize(){
	return largestSeparatorSize;
    }
}
