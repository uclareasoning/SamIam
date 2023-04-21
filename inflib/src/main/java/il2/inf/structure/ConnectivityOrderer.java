package il2.inf.structure;

import il2.util.*;
import java.util.*;
import edu.ucla.structure.Graph;
import edu.ucla.structure.Graphs;
import edu.ucla.structure.HashGraph;

public abstract class ConnectivityOrderer implements Orderer{
    protected Graph connectivity;
    private MappedHeap priorityQueue;
    protected Set updatesRequired;

    public ConnectivityOrderer(int size){
	connectivity=new HashGraph(size);
	priorityQueue=new MappedHeap();
	updatesRequired=new HashSet(2*size);
    }

    
    public int removeBest(){
	handleUpdates();
	Integer ind=(Integer)priorityQueue.removeBest();
	Set neighbors=new HashSet(connectivity.neighbors(ind));
	connectivity.remove(ind);
	generateUpdates(connectivity.neighbors(ind));
	return ind.intValue();
    }

    protected void handleUpdates(){
	Map result=new HashMap(2*updatesRequired.size());
	for(Iterator iter=updatesRequired.iterator();iter.hasNext();){
	    Object obj=iter.next();
	    result.put(obj,computeScore(obj));
	}
        priorityQueue.updatePriorities(result);
	updatesRequired.clear();
    }
    
    protected abstract void generateUpdates(Set neighbors);
    protected abstract Object computeScore(Object node);
    
    private Set toSet(IntSet element){
	Set result=new HashSet(2*element.size());
	for(int i=0;i<element.size();i++){
	    result.add(new Integer(element.get(i)));
	}
	return result;
    }

    public void add(IntSet element){
	Set updates=toSet(element);
	Graphs.makeClique(connectivity,updates);
	generateUpdates(updates);
    }


    public void addAll(Collection elements){
	Set updates=new HashSet(5*elements.size());
	for(Iterator iter=elements.iterator();iter.hasNext();){
	    Set members=toSet((IntSet)iter.next());
	    Graphs.makeClique(connectivity,members);
	    updates.addAll(members);
	}
	generateUpdates(updates);
    }
	    
	    
}
