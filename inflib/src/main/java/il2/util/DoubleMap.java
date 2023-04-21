package il2.util;
public class DoubleMap{
    private static final int DEFAULT_ALLOCATION=7;

    IntSet keys;
    DoubleList values;
    
    public DoubleMap(){
        this(DEFAULT_ALLOCATION);
    }
    public DoubleMap(DoubleMap map){
	keys=new IntSet(map.keys);
	values=new DoubleList(map.values);
    }

    /*public static IntMap inverse(IntList list){
	IntMap result=new IntMap(list.size());
	for(int i=0;i<list.size();i++){
	    result.put(list.get(i),i);
	}
	return result;
    }*/

    public DoubleMap(int size){
	keys=new IntSet(size);
	values=new DoubleList(size);
    }

    public DoubleMap(IntSet k,DoubleList v){
	keys=k;
	values=v;
    }

    public DoubleMap(int[] k,double[] v){
	int[] sinds=ArrayUtils.sortedInds(k);
	keys=new IntSet(k);
	values=new DoubleList(ArrayUtils.select(v,sinds));
    }
    public boolean put(int key,double value){
	int loc=keys.indexOf(key);
	if(loc>=0){
	    if(value!=values.get(loc)){
		values.set(loc,value);
		return true;
	    }else{
		return false;
	    }
	}else{
	    int actualloc=-loc-1;
	    keys.insertAt(actualloc,key);
	    values.insert(actualloc,value);
	    return true;
	}
    }


    public boolean remove(int key){
	int loc=keys.indexOf(key);
	if(loc>=0){
	    keys.removeEntryAt(loc);
	    values.remove(loc);
	    return true;
	}else{
	    return false;
	}
    }


    public double get(int key){
	int loc=keys.indexOf(key);
	if(loc>=0){
	    return values.get(loc);
	}else{
	    throw new IllegalArgumentException(""+key+" is not a key of this map");
	}
    }


    public double get(int key,double defaultValue){
	int loc=keys.indexOf(key);
	if(loc>=0){
	    return values.get(loc);
	}else{
	    return defaultValue;
	}
    }


    public int size(){
	return values.size();
    }


    public void lock(){
	keys.lock();
	values.lock();
    }


    public IntSet keys(){
	return keys;
    }


    public DoubleList values(){
	return values;
    }


    public String toString(){
	StringBuffer buf=new StringBuffer(7*size());
	buf.append('{');
	for(int i=0;i<size();i++){
	    buf.append(' ');
	    buf.append(keys.get(i));
	    buf.append("=>");
	    buf.append(values.get(i));
	    if(i+1<size()){
		buf.append(',');
	    }
	}
	buf.append('}');
	return buf.toString();
    }
}




