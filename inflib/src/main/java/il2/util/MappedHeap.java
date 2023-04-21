package il2.util;
import java.util.*;
public class MappedHeap extends Heap{

    private ArrayList heapContents;
    private Map heapLocation;
    private Map priority;

    public MappedHeap(){
	super();
    }

    public final void initialize(Map priorityMap){
	heapContents=new ArrayList(priorityMap.keySet());
	priority=new HashMap(priorityMap);
	heapLocation=new HashMap(2*heapContents.size());
	for(int i=0;i<heapContents.size();i++){
	    heapLocation.put(heapContents.get(i),new Integer(i));
	}
	initialize(heapContents.size());
    }

    public final void add(Object element,Object p){
	priority.put(element,p);
	Integer loc=new Integer(heapContents.size());
	heapContents.add(element);
	heapLocation.put(element,loc);
	elementAdded();
    }

    public final void updatePriority(Object element,Object p){
	Integer loc=(Integer)heapLocation.get(element);
	if(loc==null){
	    loc=new Integer(heapContents.size());
	    heapContents.add(element);
	    heapLocation.put(element,loc);
	    elementAdded();
	}else{
	    priority.put(element,p);
	    valueChanged(loc.intValue());
	}
    }

    public final void updatePriorities(Map changedPriorities){
        if(heapContents==null){
            initialize(changedPriorities);
            return;
        }
	priority.putAll(changedPriorities);
	for(Iterator iter=changedPriorities.keySet().iterator();iter.hasNext();){
	    Object element=iter.next();
	    Integer loc=(Integer)heapLocation.get(element);
	    if(loc==null){
		heapContents.add(element);
		heapLocation.put(element,loc);
		elementAdded();
	    }else{
		valueChanged(loc.intValue());
	    }
	}
    }
    public final Object removeBest(){
	Object result=heapContents.get(0);
	removeTop();
	return result;
    }

    protected final boolean isBetter(int i,int j){
	return higherPriority(priority.get(heapContents.get(i)),priority.get(heapContents.get(j)));
    }

    protected boolean higherPriority(Object p1,Object p2){
	return ((Comparable)p1).compareTo(p2)<0;
    }

    protected final void swap(int i,int j){
	Object temp1=heapContents.get(i);
	Object temp2=heapContents.get(j);
	heapContents.set(i,temp2);
	heapContents.set(j,temp1);
	heapLocation.put(temp2,new Integer(i));
	heapLocation.put(temp1,new Integer(j));
    }

}
