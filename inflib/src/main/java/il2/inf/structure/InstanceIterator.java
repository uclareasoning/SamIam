package il2.inf.structure;
import il2.model.*;
import il2.util.*;

public final class InstanceIterator{
    private final Domain domain;
    private final int[] globalInstance;

    private final IntSet varSet;
    private final int[] vars;
    private final int[] varLargests;
    
    public InstanceIterator(Domain d,int[] gInstance,IntSet v){
	this(d,gInstance,v.toIntList());
    }
    public InstanceIterator(Domain d,int[] gInstance,IntList v){
	domain=d;
	globalInstance=gInstance;
	vars=v.toArray();
	varLargests=new int[vars.length];
	for(int i=0;i<vars.length;i++){
	    varLargests[i]=domain.size(vars[i])-1;
	}
        varSet=new IntSet(v);
    }


    public final int next(){
	for(int i=0;i<vars.length;i++){
	    int var=vars[i];
	    if(globalInstance[var]<varLargests[i]){
		globalInstance[var]++;
		return i;
	    }else{
		globalInstance[var]=0;
	    }
	}
	return -1;
    }

    public IterationRec flipChange(IntSet fvars){
	return flipChange(fvars.toIntList());
    }

    public IterationRec flipChange(IntList fvars){
	IntMap fvarLocs=new IntMap(fvars.size());
	for(int i=0;i<fvars.size();i++){
	    fvarLocs.put(fvars.get(i),i);
	}
	int[] blockSizes=new int[fvars.size()];
	int currentSize=1;
	for(int i=0;i<blockSizes.length;i++){
	    blockSizes[i]=currentSize;
	    currentSize*=domain.size(fvars.get(i));
	}
	int[] flipChange=new int[vars.length];
	int reset=0;
	for(int i=0;i<vars.length;i++){
	    int currentBlockSize=blockSizes[fvarLocs.get(vars[i])];
	    flipChange[i]=currentBlockSize-reset;
	    reset+=currentBlockSize*varLargests[i];
	}
	int[] fixed=new int[fvars.size()-vars.length];
        int[] fixedBlockSizes=new int[fixed.length];
	int current=0;
	for(int i=0;i<fvars.size();i++){
	    if(!varSet.contains(fvars.get(i))){
                fixedBlockSizes[current]=blockSizes[i];
		fixed[current++]=fvars.get(i);
	    }
	}
	return new IterationRec(flipChange,fixed,fixedBlockSizes);
    }

    public final class IterationRec{
	public final int[] flipChange;
	public final int[] fixed;
	public final int[] blockSizes;
	private final int[] inst=globalInstance;

	private IterationRec(int[] fc,int[] fx,int[] bs){
	    flipChange=fc;
	    fixed=fx;
	    blockSizes=bs;
	}

	public final int baseline(){
	    int total=0;
	    for(int i=0;i<fixed.length;i++){
		total+=inst[fixed[i]]*blockSizes[i];
	    }
	    return total;
	}
    }
}
