/*
 * DoubleList.java
 *
 */

package il2.util;

/**
 *
 * @author  jdpark
 */
public class DoubleList {
    private static final String ACCESS_ERROR="Attempt to modify locked DoubleList";
    private static final int DEFAULT_ALLOCATION=7;

    double[] values;
    int length;
    int hashCode;
    boolean validHashCode;
    boolean isLocked=false;
    

    /** Creates a new instance of DoubleList */
    public DoubleList() {
        values=new double[DEFAULT_ALLOCATION];
        length=0;
        validHashCode=false;
    }
    

    public DoubleList(DoubleList orig){
        values=(double[])orig.values.clone();
        length=orig.length;
        validHashCode=orig.validHashCode;
        hashCode=orig.hashCode;
    }
    

    public DoubleList(double[] values){
        this.values=(double[])values.clone();
        length=values.length;
        validHashCode=false;
    }


    public DoubleList(int initialSize){
        values=new double[initialSize];
        length=0;
        validHashCode=false;
    }


    public final double get(int ind){
        if(ind>=length){
            throw new IndexOutOfBoundsException();
        }
        return values[ind];
    }

    public final double[] select(int[] inds){
	return ArrayUtils.select(values,inds);
    }
    public final void set(int ind,double value){
	if(isLocked){
	    throw new IllegalStateException(ACCESS_ERROR);
	}
        if(ind>=length){
            throw new IndexOutOfBoundsException();
        }
        values[ind]=value;
        validHashCode=false;
    }


    public final void add(double val){
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


    public final void insert(int ind,double val){
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


    public final void addAll(double[] vals){
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


    public final void clear(){
	length=0;
	validHashCode=false;
    }


    public final void ensureAllocation(int size){
        int allocation=3*length/2;
        if(allocation<size){
            allocation=size;
        }
        double[] newvals=new double[allocation];
        System.arraycopy(values,0,newvals, 0, length);
        values=newvals;
    }


    public final int size(){
        return length;
    }


    public final int lastIndex(){
	return length-1;
    }


    public final double lastValue(){
	return values[length-1];
    }


    public final int hashCode(){
        if(!validHashCode){
            hashCode=0;
            for(int i=0;i<length;i++){
		long  v=Double.doubleToLongBits(values[i]);
                hashCode= (hashCode>>>1)^(int)(v^(v>>>32));
            }
            validHashCode=true;
        }
        return hashCode;
    }


    public final boolean equals(Object obj){
        if(!(obj instanceof DoubleList)){
            return false;
        }
        DoubleList l=(DoubleList)obj;
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
}
