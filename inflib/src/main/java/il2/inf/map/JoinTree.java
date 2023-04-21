package il2.inf.map;
import java.util.*;
import il2.util.*;
import il2.model.*;
import il2.inf.structure.*;
public class JoinTree{
    Index[] clusters;
    Index[] parentSeparators;
    int[] parentInd;
    IntSet[] siblings;
    IntSet[] children;
    int root;
    int leafCount;

    private static class Holder implements SubDomain{
	int index;
	Index[] pots;
	Holder(int ind,Index[] pots){
	    this.index=ind;
	    this.pots=pots;
	}
	public IntSet vars(){
	    return pots[index].vars();
	}
	public Domain domain(){
	    return pots[index].domain();
	}
    }

    public JoinTree(Index[] initial,IntList eliminationOrder){
	leafCount=initial.length;
	Domain domain=initial[0].domain();
	clusters=new Index[2*initial.length+1];
	parentSeparators=new Index[clusters.length];
	parentInd=new int[clusters.length];
	siblings=new IntSet[clusters.length];
	children=new IntSet[clusters.length];
	Bucketer b=new Bucketer(eliminationOrder);
	for(int i=0;i<initial.length;i++){
	    clusters[i]=new Index(initial[i]);
	    parentSeparators[i]=new Index(initial[i]);
	    b.placeInBucket(new Holder(i,parentSeparators));
	}
	int current=initial.length;
	for(int i=0;i<b.lastBucket();i++){
	    ArrayList contents=b.getBucket(i);
	    if(contents.size()==1){
		Holder h=(Holder)contents.get(0);
		parentSeparators[h.index]=parentSeparators[h.index].forgetIndex(b.bucketLabel(i));
		b.placeInBucket(h,i);
	    }else{
		IntSet members=new IntSet(contents.size());
		Holder h=(Holder)contents.get(0);
		IntSet vars=h.vars();
		parentInd[h.index]=current;
		members.add(h.index);
		for(int j=1;j<contents.size();j++){
		    h=(Holder)contents.get(j);
		    vars=vars.union(h.vars());
		    parentInd[h.index]=current;
		    members.add(h.index);
		}
		clusters[current]=new Index(domain,vars);
		parentSeparators[current]=clusters[current].forgetIndex(b.bucketLabel(i));
		h=new Holder(current,parentSeparators);
		b.placeInBucket(h,i);
		for(int j=0;j<members.size();j++){
		    siblings[members.get(j)]=members.withoutIndex(j);
		}
		children[current]=members;
		current++;

	    }
	}
	ArrayList contents=b.getBucket(b.lastBucket());
	clusters[current]=new Index(domain,new IntSet());
	IntSet members=new IntSet(contents.size());
	for(int i=0;i<contents.size();i++){
	    Holder h=(Holder)contents.get(i);
	    parentInd[h.index]=current;
	    members.add(h.index);
	}
	for(int j=0;j<members.size();j++){
	    siblings[members.get(j)]=members.withoutIndex(j);
	}
	children[current]=members;
	siblings[current]=new IntSet();
	parentSeparators[current]=new Index(domain,new IntSet());
	root=current+1;
	parentInd[current]=root;
	parentInd[root]=-1;
	clusters[root]=new Index(domain,new IntSet());
	children[root]=new IntSet(new int[]{current});
	siblings[root]=new IntSet();
    }

    public int clusterCount(){
	return root+1;
    }
    public Index cluster(int i){
	return clusters[i];
    }
    public IntSet children(int i){
	return children[i];
    }

    public int parent(int i){
	return parentInd[i];
    }
    public void sanityCheck(){
	for(int i=leafCount;i<=root;i++){
	    IntSet ch=children[i];
	    for(int j=0;j<ch.size();j++){
		if(parentInd[ch.get(j)]!=i){
		    throw new IllegalStateException();
		}
	    }
	}
	for(int i=0;i<root;i++){
	    int pi=parentInd[i];
	    if(!children[pi].contains(i)){
		throw new IllegalStateException();
	    }
	    for(int j=0;j<siblings[i].size();j++){
		if(!children[pi].contains(siblings[i].get(j))){
		    throw new IllegalStateException();
		}
	    }
	}
    }

    public Domain domain(){
	return clusters[0].domain();
    }
}
