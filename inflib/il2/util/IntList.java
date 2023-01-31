/*
 * IntList.java
 *
 * Created on November 23, 2002, 8:57 AM
 */

package il2.util;

/**
 *
 * @author  jdpark
 */
public class IntList {
    private static final String ACCESS_ERROR="Attempt to modify locked IntList";
    private static final int DEFAULT_ALLOCATION=7;

    int[] values;
    int length;
    int hashCode;
    boolean validHashCode;
    boolean isLocked=false;

    /** @author Keith Cascio
    	@since 120704 */
    public IntStats calculateStats()
    {
    	IntStats ret = new IntStats();
    	if( this.size() < 1 ){ return ret; }
    	int[] array = this.toArray();
    	java.util.Arrays.sort( array );

    	int current;
    	int currentfreq;
    	int sum = 0;
    	int mode = Integer.MAX_VALUE;
    	int modefrequency = 0;
    	IntMap map = new IntMap( array.length );
    	for( int i=0; i<array.length; i++ ){
    		current = array[i];
    		sum += current;
    		if( map.keys().contains(current) ) currentfreq = map.get(current)+1;
    		else currentfreq = 1;
    		map.put( current, currentfreq );
    		if( currentfreq > modefrequency ){
    			modefrequency = currentfreq;
    			mode = current;
    		}
    	}

    	double medianindex = (((double)array.length)/((double)2));
    	int medianindexfloor = (int) Math.floor( medianindex );
    	int medianindexceil = (int) Math.ceil( medianindex );

		ret.count = array.length;
		ret.sum = sum;
    	ret.min = array[0];
    	ret.max = array[array.length-1];
    	ret.mean = (((double)sum)/((double)array.length));
    	ret.median = ((double)(array[medianindexfloor] + array[medianindexceil]))/((double)2);
    	ret.mode = mode;
    	ret.modefrequency = modefrequency;

    	return ret;
    }

    /** Creates a new instance of IntList */
    public IntList() {
        values=new int[DEFAULT_ALLOCATION];
        length=0;
        validHashCode=false;
    }


    public IntList(IntList orig){
        values=(int[])orig.values.clone();
        length=orig.length;
        validHashCode=orig.validHashCode;
        hashCode=orig.hashCode;
    }


    public IntList(int[] values){
        this.values=(int[])values.clone();
        length=values.length;
        validHashCode=false;
    }


    public IntList(int initialSize){
        values=new int[initialSize];
        length=0;
        validHashCode=false;
    }

    public int[] toArray(){
	int[] result=new int[length];
	System.arraycopy(values,0,result,0,length);
	return result;
    }


    public final int get(int ind){
        if(ind>=length){
            throw new IndexOutOfBoundsException();
        }
        return values[ind];
    }

    public final int[] select(int[] inds){
	return ArrayUtils.select(values,inds);
    }
    public final void set(int ind,int value){
	if(isLocked){
	    throw new IllegalStateException("Attempt to modify locked IntList");
	}
        if(ind>=length){
            throw new IndexOutOfBoundsException();
        }
        values[ind]=value;
        validHashCode=false;
    }


    public final void add(int val){
	if(isLocked){
	    throw new IllegalStateException(ACCESS_ERROR);
	}
        if(values.length==length){
            ensureAllocation(length+1);
        }
        values[length]=val;
        length++;
        validHashCode=false;
    }


    public final void insert(int ind,int val){
	if(isLocked){
	    throw new IllegalStateException(ACCESS_ERROR);
	}
	if(values.length==length){
	    ensureAllocation(length+1);
	}
	System.arraycopy(values,ind,values,ind+1,values.length-ind-1);
	values[ind]=val;
	length++;
	validHashCode=false;
    }


    public final void remove(int ind){
	if(isLocked){
	    throw new IllegalStateException(ACCESS_ERROR);
	}
	if(ind+1<length){
            System.arraycopy(values, ind+1, values, ind, length-ind-1);
        }
	length--;
        validHashCode=false;
    }


    public final void addAll(int[] vals){
        if(isLocked){
	    throw new IllegalStateException(ACCESS_ERROR);
	}
	if(values.length<vals.length+length){
            ensureAllocation(length+vals.length);
        }
        System.arraycopy(vals, 0, values, length, vals.length);
        length+=vals.length;
        validHashCode=false;
    }

    public IntList withoutIndex(int ind){
	int[] other=new int[length-1];
	if(ind>0){
	    System.arraycopy(values,0,other,0,ind);
	}
	if(ind+1<length){
	    System.arraycopy(values,ind+1,other,ind,length-ind-1);
	}
	return new IntList(other);
    }


    public final void clear(){
	length=0;
	validHashCode=false;
    }


    public final void ensureAllocation(int size){
        int allocation=3*length/2;
        if(allocation<size){
            allocation=size;
        }
        int[] newvals=new int[allocation];
        System.arraycopy(values,0,newvals, 0, length);
        values=newvals;
    }


    public final int size(){
        return length;
    }


    public final int lastIndex(){
	return length-1;
    }


    public final int lastValue(){
	return values[length-1];
    }

    public final void setLast(int val){
	values[length-1]=val;
    }
    public final void removeLast(){
	if(length==0){
	    throw new IllegalStateException();
	}
	length--;
    }



    public final int hashCode(){
        if(!validHashCode){
            hashCode=0;
            for(int i=0;i<length;i++){
                hashCode= (hashCode>>>1)^values[i];
            }
            validHashCode=true;
        }
        return hashCode;
    }


    public final boolean equals(Object obj){
        if(!(obj instanceof IntList)){
            return false;
        }
        IntList l=(IntList)obj;
        if(l.length!=length){
            return false;
        }
        for(int i=0;i<length;i++){
            if(values[i]!=l.values[i]){
                return false;
            }
        }
        return true;
    }


    public void lock(){
	isLocked=true;
    }


    public boolean isLocked(){
	return isLocked;
    }


    public String toString(){
	StringBuffer buf=new StringBuffer(5*size());
	buf.append('[');
        for(int i=0;i<length;i++){
	    buf.append(' ');
	    buf.append(get(i));
	    if(i+1<length){
		buf.append(',');
	    }
	}
	buf.append(']');
	return buf.toString();
    }

    public void swapWithLastAndRemove(int ind){
        int l=lastValue();
        values[ind]=l;
        length--;
    }
}
