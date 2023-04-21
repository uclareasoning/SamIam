package il2.inf.mini;
import il2.model.*;
import il2.util.*;
import java.util.*;

public class Partition{

    /**
     * The index of each partition
     */
    public final Index[] indices;
    /**
     * The partition that each of the initial members was assigned to
     */
    public final int[] mappings;

    public Partition(Index[] inds,int[] assignments){
	indices=inds;
	mappings=assignments;
    }

    public static Partition miniPartition(List indices,double maxSize){
	Index[] holder=new Index[indices.size()];
	indices.toArray(holder);
	return miniPartition(holder,maxSize);
    }
    public static Partition miniPartition(Index[] indices,double maxSize){
	Index[] result=new Index[indices.length];
	int[] mappings=new int[indices.length];
	int size=0;
	for(int i=0;i<indices.length;i++){
	    boolean found=false;
	    for(int j=0;!found && j<size;j++){
		Index bigInd=result[j].combineWith(indices[i]);
		if(bigInd.sizeInt()==result[j].sizeInt() || (bigInd.sizeInt()<maxSize && bigInd.sizeInt()>0)){
		    result[j]=bigInd;
		    mappings[i]=j;
		    found=true;
		}else{
		    //System.err.println(bigInd.vars()+"\t"+bigInd.size());
		}
	    }
	    if(!found){
		result[size]=new Index(indices[i]);
		mappings[i]=size;
		size++;
	    }
	}
        Index[] finalresult=new Index[size];
	System.arraycopy(result,0,finalresult,0,size);
	return new Partition(finalresult,mappings);
    }
}
