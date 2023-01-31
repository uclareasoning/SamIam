package il2.inf.structure;
import java.util.*;
import il2.util.*;
import il2.model.*;
public class DynamicEliminator{
    private Binner bins;
    private Orderer orderer;
    private int lastRemoved;

    public DynamicEliminator(Orderer orderingMethod,Domain d){
	bins=new Binner(d);
	orderer=orderingMethod;
	lastRemoved=-1;
    }

    public Set removeBestGroup(){
	lastRemoved=orderer.removeBest();
	return bins.removeItemsContaining(lastRemoved);
    }

    public Set removeNoVarGroup(){
	lastRemoved=-1;
	return bins.removeNoVarItems();
    }

    public void addAll(Collection c){
	List elements=new ArrayList(c.size());
	for(Iterator iter=c.iterator();iter.hasNext();){
	    SubDomain sd=(SubDomain)iter.next();
	    bins.add(sd);
	    elements.add(sd.vars());
	}
	orderer.addAll(elements);
    }

    public void add(SubDomain item){
	bins.add(item);
	orderer.add(item.vars());
    }

    public int removedLabel(){
	return lastRemoved;
    }

    public Set activeVars(){
	return bins.activeVars();
    }

    public boolean hasActiveVars(){
	return bins.activeVars().size()>0;
    }
    
}     
