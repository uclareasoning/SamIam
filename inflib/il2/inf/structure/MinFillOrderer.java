package il2.inf.structure;

import il2.util.*;
import java.util.*;

public class MinFillOrderer extends ConnectivityOrderer{

    public MinFillOrderer(int size){
	super(size);
    }

    protected void generateUpdates(Set neighbors){
	updatesRequired.addAll(neighbors);
	for(Iterator iter=neighbors.iterator();iter.hasNext();){
	    updatesRequired.addAll(connectivity.neighbors(iter.next()));
	}
    }

    protected Object computeScore(Object node){
	Set neighbors=connectivity.neighbors(node);
	int total=0;
	for(Iterator iter1=neighbors.iterator();iter1.hasNext();){
	    Object n1=iter1.next();
	    for(Iterator iter2=neighbors.iterator();iter2.hasNext();){
		Object n2=iter2.next();
		if(!n1.equals(n2) && !connectivity.containsEdge(n1,n2)){
		    total++;
		}
	    }
	}
	return new Integer(total/2);
    }
}
