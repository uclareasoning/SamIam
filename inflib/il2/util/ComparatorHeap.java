package il2.util;
import java.util.Comparator;
public class ComparatorHeap extends MappedHeap{
    Comparator comparator;
    public ComparatorHeap(Comparator comp){
	super();
	comparator=comp;
    }

    protected boolean higherPriority(Object o1,Object o2){
	return comparator.compare(o1,o2)<0;
    }
}
