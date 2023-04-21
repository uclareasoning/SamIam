package il2.inf.map;

import il2.util.*;
import il2.model.*;

public class IndexedJoinTree extends JoinTree{
    int[][][] upIndex;
    int[][][] downIndex;


    public IndexedJoinTree(Index[] initial,IntList eliminationOrder){
	super(initial,eliminationOrder);
	upIndex=new int[root+1][][];
	downIndex=new int[root+1][][];
	for(int i=0;i<root;i++){
	    upIndex[i]=clusters[i].baselineOffsetIndex(parentSeparators[i]);
	    downIndex[i]=clusters[parentInd[i]].baselineOffsetIndex(parentSeparators[i]);
	}
	//sanityCheck();
    }

}
