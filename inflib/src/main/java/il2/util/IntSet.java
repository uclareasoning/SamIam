/*
 * IntSet.java
 *
 * Created on November 23, 2002, 9:16 AM
 */

package il2.util;

/**
 *
 * @author  jdpark
 */
public class IntSet {
    IntList elements;


    /** Creates a new instance of IntSet */
    public IntSet() {
        elements=new IntList();
    }


    public IntSet(int size){
        elements=new IntList(size);
    }

    public IntSet(IntSet s){
	elements=new IntList(s.elements);
    }
    public static IntSet singleton(int value){
        IntSet result=new IntSet(1);
        result.add(value);
        return result;
    }
    public IntSet(IntList l){
        this(l.toArray());
    }
    private IntSet(IntList l,boolean isSorted){
	if(!isSorted){
	    throw new IllegalStateException();
	}else{
	    elements=l;
	}
    }

    public IntSet(int[] entries){
        int[] values=new int[entries.length];
        System.arraycopy(entries, 0,values, 0,entries.length);
        java.util.Arrays.sort(values);
        elements=new IntList(values.length);
        if(values.length>0){
            elements.add(values[0]);
        }
        for(int i=1;i<values.length;i++){
            if(values[i-1]!=values[i]){
                elements.add(values[i]);
            }
        }
    }

    /** @author keith cascio
        @since  20080225 */
    final public void clear(){
	this.elements.clear();
    }

    public final int get(int ind){
        return elements.get(ind);
    }


    public final int size(){
        return elements.size();
    }

    public final IntSet subset(int[] inds){
        return new IntSet(new IntList(elements.select(inds)),true);
    }

    public final IntSet union(IntSet s){
        IntList e=new IntList(s.size()+size());
        int i=0;
        int si=0;
        while(i<size() && si<s.size()){
            int vi=get(i);
            int svi=s.get(si);
            if(vi<svi){
                e.add(vi);
                i++;
            }else if(vi==svi){
                e.add(vi);
                i++;
                si++;
            }else{
                e.add(svi);
                si++;
            }
        }
        while(i<size()){
            e.add(get(i));
            i++;
        }
        while(si<s.size()){
            e.add(s.get(si));
            si++;
        }
        return new IntSet(e,true);
    }


    public final IntSet intersection(IntSet s){
        IntList e=new IntList(Math.min(s.size(),size()));
        int i=0;
        int si=0;
        while(i<size() && si<s.size()){
            int vi=get(i);
            int svi=s.get(si);
            if(vi<svi){
                i++;
            }else if(vi==svi){
                e.add(vi);
                i++;
                si++;
            }else{
                si++;
            }
        }
        return new IntSet(e,true);
    }

    public IntSet diff(IntSet s){
	IntList e=new IntList(size());
        int i=0;
        int si=0;
        while(i<size() && si<s.size()){
            int vi=get(i);
            int svi=s.get(si);
            if(vi<svi){
		e.add(vi);
                i++;
            }else if(vi==svi){
                i++;
                si++;
            }else{
                si++;
            }
        }
	while(i<size()){
	    e.add(get(i));
	    i++;
	}
        return new IntSet(e,true);
    }

    /**
     * Returns the indices in this set of the items in the subset
     */
    public int[] indices(IntSet is){
        int[] result=new int[is.size()];
	if(result.length==0){
	    return result;
	}
	int current=0;
	for(int i=0;i<result.length;i++){
	    int target=is.get(i);
	    while(current<size() && get(current)!=target){
		current++;
	    }
	    result[i]=current;
	}
	if(result[result.length-1]==size()){
	    throw new IllegalArgumentException("Must be a subset "+this+" "+is);
	}
	return result;
    }


    public int[] excludedIndices(IntSet is){
	int[] result=new int[size()-is.size()];
	if(result.length==0){
	    return result;
	}
	int current=0;
	int smallCurrent=0;
	for(int i=0;i<result.length;i++){
	    while(current<size() && smallCurrent<is.size() &&get(current)==is.get(smallCurrent)){
		current++;
		smallCurrent++;
	    }
	    result[i]=current;
	    current++;
	}
	if(result[result.length-1]==size()){
	    throw new IllegalArgumentException("Must be a subset");
	}
	return result;
    }

    public final int hashCode(){
        return elements.hashCode();
    }


    public final boolean equals(Object obj){
        if(!(obj instanceof IntSet)){
            return false;
        }
        IntSet s=(IntSet)obj;
        return elements.equals(s.elements);
    }


    public final boolean contains(int value){
	return indexOf(value)>=0;
    }


    public final boolean containsAll(IntSet s){
	if(s.size()==0){
	    return true;
	}
	int lower=0;
	for(int i=0;i<s.size();i++){
	    lower=binarySearch(s.get(i),lower,size());
	    if(lower<0){
		return false;
	    }else{
		lower++;
	    }
	}
	return true;
    }

    public final void removeEntryAt(int index){
	elements.remove(index);
    }


    public final boolean remove(int value){
        int ind=indexOf(value);
        if(ind>=0){
            elements.remove(ind);
	    return true;
        }else{
	    return false;
	}
    }


    public final boolean add(int value){
        int ind=indexOf(value);
        if(ind<0){
            ind=-ind-1;
            elements.insert(ind,value);
	    return true;
        }else{
	    return false;
	}
    }


    public final boolean appendAdd(int value){
	if(size()==0){
	    elements.add(value);
	    return true;
	}
        int last=elements.lastValue();
	if(last>value){
	    elements.add(value);
	    return true;
	}else if(last==value){
	    return false;
	}else{
	    return add(value);
	}
    }


    public final int indexOf(int value){
        return binarySearch(value,0,size());
    }
    public final void insertAt(int index,int value){
	if((index==0 || value>elements.get(index-1)) && (index==size() || value<elements.get(index))){
	    elements.insert(index,value);
	}else{
	    throw new IllegalArgumentException("Bad insertion location");
	}
    }


    private final int binarySearch(int value,int low,int high){
	while(true){
	    if(low==high){
		return -low-1;
	    }
	    int ind=(low+high)/2;
	    int ival=get(ind);
	    if(value<ival){
		high=ind;
	    }else if(value>ival){
		low=ind+1;
	    }else{
		return ind;
	    }
	}
    }


    public void lock(){
	elements.lock();
    }


    public String toString(){
	StringBuffer buf=new StringBuffer(5*size());
	buf.append('{');
        for(int i=0;i<size();i++){
	    buf.append(' ');
	    buf.append(get(i));
	    if(i+1<size()){
		buf.append(',');
	    }
	}
	buf.append('}');
	return buf.toString();
    }
    public void sanityCheck(){
	for(int i=1;i<size();i++){
	    if(elements.get(i)<=elements.get(i-1)){
		throw new IllegalStateException("IntSet property violated  "+this);
	    }
	}
    }

    public IntSet withoutIndex(int ind){
	return new IntSet(elements.withoutIndex(ind),true);
    }

    public final int largest(){
        return elements.lastValue();
    }
    public final boolean isEmpty(){
        return elements.size()==0;
    }

    public IntSet selectRandomly(double fraction){
        return randomSubset((int)Math.ceil(fraction*size()));
    }
    public IntSet randomSubset(int size){
        if(size>=size()){
            return new IntSet(this);
        }
        IntList temp=new IntList(elements);
        IntSet result=new IntSet(size);
        java.util.Random r=new java.util.Random();
        for(int i=0;i<size;i++){
            int ind=r.nextInt(temp.size());
            result.add(temp.get(ind));
            temp.swapWithLastAndRemove(ind);
        }
        return result;
    }

    public int[] toArray(){
        return elements.toArray();
    }

    public IntList toIntList(){
	return new IntList(elements);
    }

}
