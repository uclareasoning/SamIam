package il2.inf.map;
import il2.util.*;
import il2.model.*;

public class ShenoyShaferAlgorithm{
    IndexedJoinTree ijt;

    double[][] upMessage;
    double[][] downMessage;
    boolean[] downValid;
    boolean[] upValid;

    double[] scratch;
    Table[] leaves;
    int root;
    public ShenoyShaferAlgorithm(IndexedJoinTree ijt,Table[] leaves){
	this.ijt=ijt;
	root=ijt.root;
	upMessage=new double[root][];
	downMessage=new double[root][];
	upValid=new boolean[root];
	downValid=new boolean[root];
	this.leaves=leaves;
	int maxClusterSize=1;
	this.leaves=leaves;
	for(int i=0;i<ijt.root;i++){
	    upMessage[i]=new double[ijt.parentSeparators[i].sizeInt()];
	    downMessage[i]=new double[upMessage[i].length];
	    if(ijt.clusters[i].sizeInt()>maxClusterSize){
		maxClusterSize=ijt.clusters[i].sizeInt();
	    }
	}
	downMessage[root-1][0]=1;
	downValid[root-1]=true;
	scratch=new double[maxClusterSize];
    }

    public void setLeaf(int i,Table t){
	if(!leaves[i].vars().equals(t.vars())){
	    throw new IllegalArgumentException("Incompatible Table");
	}
	leaves[i]=t;
	invalidateUp(i);
    }
    public double[] getPartial(int i){
	computeDownMessage(i);
	return downMessage[i];
    }

    public double probability(){
	computeUpMessage(root-1);
	return upMessage[root-1][0];
    }

    private void invalidateUp(int t){
	if(upValid[t]){
	    upValid[t]=false;
	    for(int i=0;i<ijt.siblings[t].size();i++){
      		invalidateDown(ijt.siblings[t].get(i));
	    }
	    if(ijt.parentInd[t]!=ijt.root){
		invalidateUp(ijt.parentInd[t]);
	    }
	}
    }

    private void invalidateDown(int t){
	if(downValid[t]){
	    downValid[t]=false;
	    if(t>=leaves.length){
		for(int i=0;i<ijt.children[t].size();i++){
		    invalidateDown(ijt.children[t].get(i));
		}
	    }
	}
    }

    public void computeDownMessage(int t){

	if(!downValid[t]){
	    for(int i=0;i<ijt.siblings[t].size();i++){
		int s=ijt.siblings[t].get(i);
		computeUpMessage(s);
	    }
	    if(t!=ijt.root){
		int p=ijt.parentInd[t];
		computeDownMessage(p);
	    }
      	    int sz=ijt.clusters[ijt.parentInd[t]].sizeInt();
	    for(int i=0;i<sz;i++){
		scratch[i]=1;
	    }
	    for(int i=0;i<ijt.siblings[t].size();i++){
		int s=ijt.siblings[t].get(i);
		Table.multiplyInto(upMessage[s],scratch,ijt.downIndex[s]);
	    }
	    if(t!=ijt.root){
		int p=ijt.parentInd[t];
		Table.multiplyInto(downMessage[p],scratch,ijt.upIndex[p]);
	    }
	    Table.projectInto(scratch,downMessage[t],ijt.downIndex[t]);
	    downValid[t]=true;
	}
    }

    public void computeUpMessage(int t){
	if(!upValid[t]){
	    if(t<leaves.length){
		Table.projectInto(leaves[t].values(),upMessage[t],ijt.upIndex[t]);
	    }else{
		for(int i=0;i<ijt.children[t].size();i++){
		    computeUpMessage(ijt.children[t].get(i));
		}
		int sz=ijt.clusters[t].sizeInt();
		for(int i=0;i<sz;i++){
		    scratch[i]=1;
		}
		for(int i=0;i<ijt.children[t].size();i++){
		    int c=ijt.children[t].get(i);
		    Table.multiplyInto(upMessage[c],scratch,ijt.downIndex[c]);
		}
		Table.projectInto(scratch,upMessage[t],ijt.upIndex[t]);
	    }
	    upValid[t]=true;
	}
    }

}
