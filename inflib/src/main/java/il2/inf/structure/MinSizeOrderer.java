package il2.inf.structure;


    
import il2.util.*;
import java.util.*;
import il2.model.*;

public class MinSizeOrderer extends ConnectivityOrderer{

    Domain domain;
    public MinSizeOrderer(Domain d){
	super(d.size());
        domain=d;
    }

    protected void generateUpdates(Set neighbors){
	updatesRequired.addAll(neighbors);
    }

    protected Object computeScore(Object node){
        int var=((Integer)node).intValue();
	Set neighbors=connectivity.neighbors(node);
	double total=domain.size(var);
	for(Iterator iter1=neighbors.iterator();iter1.hasNext();){
	    int n1=((Integer)iter1.next()).intValue();
	    total*=domain.size(n1);
	}
	return new Double(total);
    }
}