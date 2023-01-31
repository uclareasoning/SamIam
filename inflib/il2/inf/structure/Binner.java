package il2.inf.structure;
import java.util.*;
import il2.model.*;
import il2.util.*;

public class Binner{
    Set[] bins;
    Set activeVars;
    Set resultActiveVars;
    int noVarBin;
    public Binner(Domain d){
	bins=new Set[d.size()+1];
	noVarBin=d.size();
	activeVars=new HashSet();
	resultActiveVars=Collections.unmodifiableSet(activeVars);
    }
    
    public Set activeVars(){
	return resultActiveVars;
    }
    public Set removeNoVarItems(){
	Set result=bins[noVarBin];
	bins[noVarBin]=null;
	return result;
    }
    public Set removeItemsContaining(int var){
	Integer varI=new Integer(var);
	if(!activeVars.remove(varI)){
	    throw new IllegalArgumentException("Does not contain "+var);
	}
	Set result=bins[var];
	bins[var]=null;
	
	for(Iterator iter=result.iterator();iter.hasNext();){
	    SubDomain element=(SubDomain)iter.next();
	    IntSet vars=element.vars();
	    for(int i=0;i<vars.size();i++){
		int v=vars.get(i);
		if(v!=var){
		    bins[v].remove(element);
		    if(bins[v].size()==0){
			activeVars.remove(new Integer(v));
			bins[v]=null;
		    }
		}
	    }
	}
	return result;
    }

    public void add(SubDomain sd){
	IntSet vars=sd.vars();
	if(vars.size()==0){
	    if(bins[noVarBin]==null){
		bins[noVarBin]=new HashSet();
	    }
	    bins[noVarBin].add(sd);
	}
	for(int i=0;i<vars.size();i++){
	    int var=vars.get(i);
	    if(bins[var]==null){
		bins[var]=new HashSet();
		activeVars.add(new Integer(var));
	    }
	    bins[vars.get(i)].add(sd);
	}
    }

    public void addAll(Collection c){
	for(Iterator iter=c.iterator();iter.hasNext();){
	    add((SubDomain)iter.next());
	}
    }
}
