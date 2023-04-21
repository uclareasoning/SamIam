package edu.ucla.util;
import java.util.*;
public class Maps{

    public static  void inverse(Map orig,Map result){
        for(Iterator iter=orig.entrySet().iterator();iter.hasNext();){
            Map.Entry e=(Map.Entry)iter.next();
            if(!result.containsKey(e.getValue())){
                result.put(e.getValue(),new HashSet());
            }
            Set s=(Set)result.get(e.getValue());
            s.add(e.getKey());
        }
    }
    public static HashMap inverse(Map orig){
        HashMap result=new HashMap(orig.size());
        inverse(orig,result);
        return result;
    }

    public static void bijectiveInverse(Map orig,Map result){
        for(Iterator iter=orig.entrySet().iterator();iter.hasNext();){
            Map.Entry e=(Map.Entry)iter.next();
            result.put(e.getValue(),e.getKey());
        }
    }

    public static  HashMap elementsContaining(Map orig){
        HashMap result=new HashMap(orig.size());
        for(Iterator iter=orig.keySet().iterator();iter.hasNext();){
            Object v=iter.next();
            for(Iterator riter=((Set)orig.get(v)).iterator();riter.hasNext();){
                Object k=riter.next();
                Set vals=(Set)result.get(k);
                if(vals==null){
                    vals=new HashSet();
                    result.put(k,vals);
                }
                vals.add(v);
            }
        }
        return result;
    }
    public static  HashMap bijectiveInverse(Map orig){
        HashMap result=new HashMap(orig.size());
        bijectiveInverse(orig,result);
        return result;
    }
    public static Comparable max(Collection vals){
        try{
            Iterator iter=vals.iterator();
            Comparable best=(Comparable)iter.next();
            while(iter.hasNext()){
                Comparable current=(Comparable)iter.next();
                if(current.compareTo(best)>0){
                    best=current;
                }
            }
            return best;
        }catch(NullPointerException npe){
            throw new IllegalArgumentException();
        }
    }

    public static Comparable min(Collection vals){
        try{
            Iterator iter=vals.iterator();
            Comparable best=(Comparable)iter.next();
            while(iter.hasNext()){
                Comparable current=(Comparable)iter.next();
                if(current.compareTo(best)<0){
                    best=current;
                }
            }
            return best;
        }catch(NullPointerException npe){
            throw new IllegalArgumentException();
        }
    }

    public static HashMap toMap(List vals){
        HashMap result=new HashMap(vals.size());
        int current=0;
        for(Iterator iter=vals.iterator();iter.hasNext();){
            result.put(new Integer(current),iter.next());
            current++;
        }
        return result;
    }
    public static HashMap bijectiveInverse(List vals){
        HashMap result=new HashMap(vals.size());
        int current=0;
        for(Iterator iter=vals.iterator();iter.hasNext();){
            result.put(iter.next(),new Integer(current));
            current++;
        }
        return result;
    }

    public static  void compose(Map m2,Map m1,Map result){
        for(Iterator iter=m1.keySet().iterator();iter.hasNext();){
            Object v=iter.next();
            result.put(v,m2.get(m1.get(v))); 
        }
    }
    public static  void setCompose(Map m2,Map m1,Map result){
	for(Iterator iter=m1.keySet().iterator();iter.hasNext();){
	    Object v=iter.next();
	    result.put(v,map(m2,(Set)m1.get(v)));
	}
    }

    public static HashMap compose(Map m2,Map m1){
        HashMap result=new HashMap(m1.size());
        compose(m2,m1,result);
        return result;
    }
    public static HashMap setCompose(Map m2,Map m1){
	HashMap result=new HashMap(m1.size());
	setCompose(m2,m1,result);
	return result;
    }

    public static  void compose(Function f,Map m1,Map result){
        for(Iterator iter=m1.keySet().iterator();iter.hasNext();){
            Object v=iter.next();
            result.put(v,f.apply(m1.get(v)));
        }
    }

    public static HashMap compose(Function f,Map m1){
        HashMap result=new HashMap(m1.size());
        compose(f,m1,result);
        return result;
    }

    public static HashMap fixPoint(Map map){
        HashMap result=new HashMap(map);
        for(Iterator iter=result.keySet().iterator();iter.hasNext();){
            Object key=iter.next();
            Object value1=key;
            Object value2=result.get(value1);
            while(!value1.equals(value2)){
                value1=value2;
                value2=result.get(value1);
            }
            result.put(key,value1);
        }
        return result;
    }
    public static HashMap identityMap(Collection t){
        HashMap result=new HashMap(t.size());
        for(Iterator iter=t.iterator();iter.hasNext();){
            Object val=iter.next();
            result.put(val,val);
        }
        return result;
    }
    public static  void map(Map map,Collection s,Collection result){
        for(Iterator iter=s.iterator();iter.hasNext();){
            result.add(map.get(iter.next()));
        }
    }

    public static HashSet map(Map m1,Set s){
        HashSet result=new HashSet(s.size());
        map(m1,s,result);
        return result;
    }

    public static Function makeComposite(Function f2,Function f1){
        return new CompositeFunction(f2,f1);
    }    

    private static class CompositeFunction implements Function{
        Function first;
        Function second;
        CompositeFunction(Function s,Function f){
            first=f;
            second=s;
        }
        public Object apply(Object val){
            return second.apply(first.apply(val));
        }
    }
    public static HashMap subMap(Map map,Collection sub){
        HashMap result=new HashMap(sub.size());
        for(Iterator iter=sub.iterator();iter.hasNext();){
            Object key=iter.next();
            result.put(key,map.get(key));
        }
        return result;
    }
         
}
