package il2.util;
public class IntMap{
    private static final int DEFAULT_ALLOCATION=7;

    IntSet keys;
    IntList values;

    // Added by Mark Chavira 2004-06-17

    public boolean equals (Object o) {
      if (!(o instanceof IntMap)) {
        return false;
      }
      IntMap m = (IntMap)o;
      return keys.equals (m.keys) && values.equals (m.values);
    }

    /** @author keith cascio
        @since  20080225 */
    final public void clear(){
	this   .keys .clear();
	this .values .clear();
    }

    public IntMap(){
        this(DEFAULT_ALLOCATION);
    }
    public IntMap(IntMap map){
	keys=new IntSet(map.keys);
	values=new IntList(map.values);
    }

    public static IntMap inverse(IntList list){
	IntMap result=new IntMap(list.size());
	for(int i=0;i<list.size();i++){
	    result.put(list.get(i),i);
	}
	return result;
    }

    public IntMap(int size){
	keys=new IntSet(size);
	values=new IntList(size);
    }

    public IntMap(IntSet k,IntList v){
	keys=k;
	values=v;
    }

    public IntMap(int[] k,int[] v){
	int[] sinds=ArrayUtils.sortedInds(k);
	keys=new IntSet(k);
	values=new IntList(ArrayUtils.select(v,sinds));
    }

    public IntMap subMap(IntSet subKeys){
        IntList subVals=new IntList(subKeys.size());
        for(int i=0;i<subKeys.size();i++){
            subVals.add(get(subKeys.get(i)));
        }
        return new IntMap(new IntSet(subKeys), subVals);
    }

    public void putAtEnd(int key,int value){
        if(keys.size()==0 || key>keys.largest()){
            keys.appendAdd(key);
            values.add(value);
        }else{
            throw new IllegalArgumentException();
        }
    }

    public IntMap combine(IntMap new_intmap) {
        if (this.keys().intersection(new_intmap.keys()).size() > 0)
            throw new IllegalArgumentException();
        else {
            IntMap ret_intmap = new IntMap(this);
            for (int i = 0; i < new_intmap.keys().size(); i++){
                int key = new_intmap.keys().get(i);
                ret_intmap.put(key, new_intmap.get(key));
            }
            return ret_intmap;
        }
        

    }
    
    public boolean put(int key,int value){
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


    public int get(int key){
	int loc=keys.indexOf(key);
	if(loc>=0){
	    return values.get(loc);
	}else{
	    throw new IllegalArgumentException(""+key+" is not a key of this map");
	}
    }


    public int get(int key,int defaultValue){
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


    public IntList values(){
	return values;
    }
    public int key(int i){
	return keys.get(i);
    }

    public int value(int i){
	return values.get(i);
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
