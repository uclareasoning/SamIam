package il2.inf.structure;
import java.util.*;
import il2.util.*;
import il2.model.*;


public class Bucketer{
    final ArrayList[] buckets;
    final IntList order;
    final IntMap varBucket;
    final int lastBucket;
    public Bucketer(IntList eliminationOrder){
	order=eliminationOrder;
	lastBucket=order.size();
	varBucket=IntMap.inverse(eliminationOrder);
	buckets=new ArrayList[order.size()+1];
	for(int i=0;i<buckets.length;i++){
	    buckets[i]=new ArrayList();
	}
    }
    public void placeInBucket(SubDomain sd){
	placeInBucket(sd,-1);
    }
    public void placeInBuckets(Collection subDomains){
	placeInBuckets(subDomains,-1);
    }
    public void placeInBuckets(Collection subDomains,int after){
	for(Iterator iter=subDomains.iterator();iter.hasNext();){
	    placeInBucket((SubDomain)iter.next(),after);
	}
    }

    public int lastBucket(){
	return lastBucket;
    }
	    

    public void placeInBucket(SubDomain sd,int after){
	IntSet vars= sd.vars();
	int best=lastBucket;
	for(int i=0;i<vars.size();i++){
	    int vind=varBucket.get(vars.get(i),lastBucket);
	    if(vind>after && vind<best){
		best=vind;
	    }
	}
	buckets[best].add(sd);
    }

	/**
	 * same as placeInBucket, except that it returns the index of the
	 * bucket the table is placed into.  
	 *
	 * created for EliminationOrders.bucketerJoinTree
	 */
    public int placeInBucket2(SubDomain sd,int after){
	IntSet vars= sd.vars();
	int best=lastBucket;
	for(int i=0;i<vars.size();i++){
	    int vind=varBucket.get(vars.get(i),lastBucket);
	    if(vind>after && vind<best){
		best=vind;
	    }
	}
	buckets[best].add(sd);
	return best;
    }

    public ArrayList getBucket(int i){
	return buckets[i];
    }
    public int bucketLabel(int i){
	return order.get(i);
    }
    public int bucketIndex(int var){
        return varBucket.get(var);
    }
}
