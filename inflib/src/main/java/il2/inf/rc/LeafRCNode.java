package il2.inf.rc;
import il2.util.*;
import il2.model.*;

class LeafRCNode extends RCNode{
    
    boolean isEvil;//Evil if the context does not contain all vars.  Then summation is required.
    Table t;
    int[] offsets;

    LeafRCNode(Table table,int[] instance,Integer node){
	super(table.domain(),table.vars(),instance,node);
	t=table;
    }

    protected void initialize(IntSet context){
        contextIndex=new Index(domain,context);
	if(context.equals(clusterVars)){
	    isEvil=false;
	}else{
	    isEvil=true;
	    offsets=t.offsetIndex(contextIndex);
	}
    }

    protected double rc(int ind){
	if(!isEvil){
	    return t.values()[ind];
	}else{
	    int baseline=t.getIndexFromFullInstance(globalInstance);
	    double total=0;
	    for(int i=0;i<offsets.length;i++){
		total+=t.values()[baseline+offsets[i]];
	    }
	    return total;
	}
    }

    protected void setCaching(boolean allocate){
    }
    protected void invalidate(){
    }
    
    void setTable(Table table){
	t=table;
    }
    Table getTable(){
	return t;
    }
}
