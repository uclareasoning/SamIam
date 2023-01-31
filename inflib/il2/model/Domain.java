package il2.model;
import il2.util.*;
import java.util.*;
public class Domain{
    private static final double LOG2=Math.log(2);
    IntList sizes;
    DoubleList logSizes;
    List names;
    List instanceNames;
    Map nameToInd;
    List instanceNamesMap;


    public Domain(){
	this(100);
    }

    public Domain(int initialSize){
	sizes=new IntList(initialSize);
	logSizes=new DoubleList(initialSize);
	names=new ArrayList(initialSize);
	instanceNames=new ArrayList(initialSize);
	nameToInd=new HashMap(initialSize);
	instanceNamesMap=new ArrayList(initialSize);
    }

    public int addDim(int size){
        return addDim("v"+sizes.size(),defaultNames(size));
    }
    public int addDim(String name,int size){
	return addDim(name,defaultNames(size));
    }
    private String[] defaultNames(int size){
	String[] s=new String[size];
	for(int i=0;i<size;i++){
	    s[i]=""+i;
	}
	return s;
    }
    public int addDim(String name,List vals){
	String[] v=new String[vals.size()];
	for(int i=0;i<v.length;i++){
	    v[i]=vals.get(i).toString();
	}
	return addDim(name,v);
    }
    public int addDim(String name,String[] vals){
	int var=sizes.size();
	sizes.add(vals.length);
	logSizes.add(Math.log(vals.length)/LOG2);
	names.add(name);
	instanceNames.add(vals);
	nameToInd.put(name,new Integer(var));
	HashMap inm=new HashMap(vals.length);
	for(int i=0;i<vals.length;i++){
	    inm.put(vals[i],new Integer(i));
	}
	instanceNamesMap.add(inm);
	return var;
    }
    public int size(){
	return sizes.size();
    }
    public int size(int var){
	return sizes.get(var);
    }
    public double logSize(int var){
	return logSizes.get(var);
    }
    public double logSize(IntSet vars){
        double total=0;
        for(int i=0;i<vars.size();i++){
            total+=logSize(vars.get(i));
        }
        return total;
    }

    public Map toStrings(IntMap inst){
	Map result=new HashMap(2*inst.size());
	for(int i=0;i<inst.size();i++){
	    int var=inst.keys().get(i);
	    int val=inst.values().get(i);
	    result.put(name(var),instanceName(var,val));
	}
	return result;
    }

    public int[] getSizes(IntSet vars){
	int[] result=new int[vars.size()];
	for(int i=0;i<result.length;i++){
	    result[i]=sizes.get(vars.get(i));
	}
	return result;
    }

    /** @author Keith Cascio
    	@since 020305 */
    public long[] getSizesLong( IntSet vars ){
	long[] result = new long[ vars.size() ];
	for( int i=0; i<result.length; i++ ){
	    result[i] = (long)sizes.get(vars.get(i));
	}
	return result;
    }

    public double[] getLogSizes(IntSet vars){
	double[] result=new double[vars.size()];
	for(int i=0;i<result.length;i++){
	    result[i]=logSizes.get(vars.get(i));
	}
	return result;
    }

    public double size(IntSet vars){
	double result=1;
	for(int i=0;i<vars.size();i++){
	    result*=sizes.get(vars.get(i));
	}
	return result;
    }

    /** @author keith cascio
    	@since 20060124 */
    public String namesToString(){
    	int length = size();
    	if( length < 1 ) return "";
    	StringBuffer buff = new StringBuffer( 64 + (length*32) );
    	buff.append( Domain.this.name(0) );
    	for( int i=1; i<length; i++ ){
    	    buff.append( ", " );
    	    buff.append( Domain.this.name(i) );
    	}
    	return buff.toString();
    }

    public String name(int var){
	return names.get(var).toString();
    }

    public int index(String name){
	return ((Integer)nameToInd.get(name)).intValue();
    }
    public int instanceIndex(int var,String val){
	return ((Integer)((HashMap)instanceNamesMap.get(var)).get(val)).intValue();
    }
    public String instanceName(int var,int val){
	return ((String[])instanceNames.get(var))[val];
    }
    public String[] instanceNames(int var){
	return (String[])instanceNames.get(var);
    }
}
