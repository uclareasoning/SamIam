package il2.inf;
import il2.util.*;
import java.util.*;
import il2.inf.structure.*;
import il2.model.*;

public class BasicInference{


    public static double sumProduct(Collection potentials){
	EliminationOrders.Record er=EliminationOrders.minFill(potentials,1,(Random)null);
	Bucketer b=new Bucketer(er.order);
	b.placeInBuckets(potentials);
	for(int i=0;i<b.lastBucket();i++){
	    ArrayList contents=b.getBucket(i);
	    Table t=Table.multiplyAll(contents).forget(b.bucketLabel(i));
	    b.placeInBucket(t,i);
	}
	Table t=Table.multiplyAll(b.getBucket(b.lastBucket()));
	return t.values()[0];
    }

    public static double MAP(Collection potentials,IntSet mapvars){
	EliminationOrders.Record er=EliminationOrders.constrainedMinFill(potentials,mapvars);
	System.err.println("elimination size="+er.size);
	Bucketer b=new Bucketer(er.order);
	b.placeInBuckets(potentials);
	for(int i=0;i<b.lastBucket();i++){
	    ArrayList contents=b.getBucket(i);
	    Table t=null;
	    if(mapvars.contains(b.bucketLabel(i))){
		t=Table.multiplyAll(contents).maximize(b.bucketLabel(i));
	    }else{
		t=Table.multiplyAll(contents).forget(b.bucketLabel(i));
	    }
	    b.placeInBucket(t,i);
	}
	Table t=Table.multiplyAll(b.getBucket(b.lastBucket()));
	return t.values()[0];
    }

    public static double MinAP(Collection potentials,IntSet mapvars){
        //Author: Suming Chen. Finds the MINimizing instantiation.
	EliminationOrders.Record er=EliminationOrders.constrainedMinFill(potentials,mapvars);
	Bucketer b=new Bucketer(er.order);
	b.placeInBuckets(potentials);
	for(int i=0;i<b.lastBucket();i++){
	    ArrayList contents=b.getBucket(i);
	    Table t=null;
	    if(mapvars.contains(b.bucketLabel(i))){
            t=Table.multiplyAll(contents).minimize(b.bucketLabel(i));
	    }else{
		t=Table.multiplyAll(contents).forget(b.bucketLabel(i));
	    }
	    b.placeInBucket(t,i);
	}
    
	Table t=Table.multiplyAll(b.getBucket(b.lastBucket()));
	return t.values()[0];
    }
    
    public static double dynamicMiniSumProduct(Collection potentials,double widthBound){
        int sizeBound=(int)Math.pow(2, widthBound);
	Domain domain=((Table)potentials.iterator().next()).domain();
	DynamicEliminator de=new DynamicEliminator(new MinSizeOrderer(domain),domain);
	de.addAll(potentials);
        int approximationCount=0;
	while(de.hasActiveVars()){
	    Collection contents=de.removeBestGroup();
	    Table[] minifuns=Table.miniMultiplyAndForget(contents,de.removedLabel(),sizeBound);
	    if(minifuns.length>1){
                approximationCount++;
            }
            de.addAll(java.util.Arrays.asList(minifuns));
	}
        //System.out.println("dynamic approximation count "+approximationCount);
	Table t=Table.multiplyAll(de.removeNoVarGroup());
	return t.values()[0];
    }



    public static double miniSumProduct(Collection potentials,int widthBound,boolean sumSmallest){
        int sizeBound=(int)Math.pow(2, widthBound);
	EliminationOrders.Record er=EliminationOrders.minSize(potentials);
	Bucketer b=new Bucketer(er.order);
	b.placeInBuckets(potentials);
        int approximationCount=0;
	for(int i=0;i<b.lastBucket();i++){
	    ArrayList contents=b.getBucket(i);
	    Table[] minifuns=Table.miniMultiplyAndForget(contents,b.bucketLabel(i),sizeBound,sumSmallest);
	    b.placeInBuckets(java.util.Arrays.asList(minifuns),i);
            if(minifuns.length>1){
                approximationCount++;
            }
	}
        //System.out.println("miniSumProduct Approximation Count:"+approximationCount);
	Table t=Table.multiplyAll(b.getBucket(b.lastBucket()));
	return t.values()[0];
    }

    public static double miniMaxProduct(Collection potentials,int widthBound){
        int sizeBound=(int)Math.pow(2, widthBound);
	EliminationOrders.Record er=EliminationOrders.minFill(potentials,1,(Random)null);
	Bucketer b=new Bucketer(er.order);
	b.placeInBuckets(potentials);
	for(int i=0;i<b.lastBucket();i++){
	    ArrayList contents=b.getBucket(i);
	    Table[] minifuns=Table.miniMultiplyAndMaximize(contents,b.bucketLabel(i),sizeBound);
	    b.placeInBuckets(java.util.Arrays.asList(minifuns),i);
	}
	Table t=Table.multiplyAll(b.getBucket(b.lastBucket()));
	return t.values()[0];
    }

    public static double miniMAP(Collection potentials,IntSet mapvars,int sizeBound){
	EliminationOrders.Record er=EliminationOrders.constrainedMinFill(potentials,mapvars);
	System.err.println("Elimination width "+er.size);
	Bucketer b=new Bucketer(er.order);
	b.placeInBuckets(potentials);
	for(int i=0;i<b.lastBucket();i++){
	    ArrayList contents=b.getBucket(i);
	    Table[] minifuns=null;
	    if(mapvars.contains(b.bucketLabel(i))){
		minifuns=Table.miniMultiplyAndMaximize(contents,b.bucketLabel(i),sizeBound);
	    }else{
		minifuns=Table.miniMultiplyAndForget(contents,b.bucketLabel(i),sizeBound);
	    }
	    b.placeInBuckets(java.util.Arrays.asList(minifuns),i);
	}
	Table t=Table.multiplyAll(b.getBucket(b.lastBucket()));
	return t.values()[0];
    }

    public static double weakMAP(Collection potentials,IntSet mapvars,int sizeBound){
	//EliminationOrders.Record er=EliminationOrders.minFill(potentials,1,(Random)null);
	EliminationOrders.Record er=EliminationOrders.hardBoundedConstrainedFill(potentials,mapvars,Math.log(sizeBound)/Math.log(2));
	return weakMAP(potentials,mapvars,er.order);
    }
    public static double weakMAP(Collection potentials,IntSet mapvars,IntList order){
	Bucketer b=new Bucketer(order);
	b.placeInBuckets(potentials);
	for(int i=0;i<b.lastBucket();i++){
	    ArrayList contents=b.getBucket(i);
	    Table t=null;
	    if(mapvars.contains(b.bucketLabel(i))){
		t=Table.multiplyAll(contents).maximize(b.bucketLabel(i));
	    }else{
		t=Table.multiplyAll(contents).forget(b.bucketLabel(i));
	    }
	    b.placeInBucket(t,i);
	}
	Table t=Table.multiplyAll(b.getBucket(b.lastBucket()));
	return t.values()[0];
    }


    public static double maxProduct(Collection potentials){
	EliminationOrders.Record er=EliminationOrders.minFill(potentials,1,(Random)null);
	Bucketer b=new Bucketer(er.order);
	b.placeInBuckets(potentials);
	for(int i=0;i<b.lastBucket();i++){
	    ArrayList contents=b.getBucket(i);
	    Table t=Table.multiplyAll(contents).maximize(b.bucketLabel(i));
	    b.placeInBucket(t,i);
	}
	Table t=Table.multiplyAll(b.getBucket(b.lastBucket()));
	return t.values()[0];
    }

    public static Collection maxReduce(Collection potentials,double sizeBound){
        if(potentials.size()==0){
            return new ArrayList(0);
        }
        Domain domain=((SubDomain)potentials.iterator().next()).domain();
        EliminationOrders.Record er=EliminationOrders.minFill(potentials,1,(Random)null);
        Bucketer b=new Bucketer(er.order);
        b.placeInBuckets(potentials);
        boolean[] invalidated=new boolean[er.order.size()];
        List result=new ArrayList(potentials.size());
        for(int i=0;i<b.lastBucket();i++){
            ArrayList contents=b.getBucket(i);
            if(invalidated[i]){
                result.addAll(contents);
            }else{
                IntSet vars=getVars(contents);
                if(domain.size(vars)<=sizeBound){
                    Table t=Table.multiplyAll(contents).maximize(b.bucketLabel(i));
                    b.placeInBucket(t,i);
                }else{
                    for(int j=0;j<vars.size();j++){
                        invalidated[b.bucketIndex(vars.get(j))]=true;
                    }
                    result.addAll(contents);
                }
            }
        }
        ArrayList contents=b.getBucket(b.lastBucket());
        if(contents.size()>0){
            result.add(Table.multiplyAll(contents));
        }
        return result;
    }

    public static Collection sumReduce(Collection potentials,double sizeBound){
        if(potentials.size()==0){
            return new ArrayList(0);
        }
        Domain domain=((SubDomain)potentials.iterator().next()).domain();
        EliminationOrders.Record er=EliminationOrders.minFill(potentials,1,(Random)null);
        Bucketer b=new Bucketer(er.order);
        b.placeInBuckets(potentials);
        boolean[] invalidated=new boolean[er.order.size()];
        List result=new ArrayList(potentials.size());
        for(int i=0;i<b.lastBucket();i++){
            ArrayList contents=b.getBucket(i);
            IntSet vars=getVars(contents);
	    if(invalidated[i] || domain.size(vars)>sizeBound){
                result.addAll(contents);
		for(int j=0;j<vars.size();j++){
		    invalidated[b.bucketIndex(vars.get(j))]=true;
		}
            }else{
		Table t=Table.multiplyAll(contents).forget(b.bucketLabel(i));
		b.placeInBucket(t,i);
            }
        }
        ArrayList contents=b.getBucket(b.lastBucket());
        if(contents.size()>0){
            result.add(Table.multiplyAll(contents));
        }
        return result;
    }


    public static IntSet getVars(Collection c){
        IntSet result=new IntSet();
        for(Iterator iter=c.iterator();iter.hasNext();){
            result=result.union(((SubDomain)iter.next()).vars());
        }
        return result;
    }


}
