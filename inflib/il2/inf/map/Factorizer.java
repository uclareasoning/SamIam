package il2.inf.map;
import il2.util.*;
import il2.model.*;
/**
 *
 * @author  jdpark
 */
public class Factorizer {
    private final static int[] primes=new int[]{2,3,5,7,11,13,17,19,23,29,31,37,41,43,47,53,59,61,67,71,73,79,83,89,97};

    Domain originalDomain;
    Domain factoredDomain;
    int[][] origToFactored;
    IntList factoredToOrig;
    /** Creates a new instance of Binarizer */
    public Factorizer(Domain d) {
        originalDomain=d;
        factoredDomain=new Domain(2*originalDomain.size());
        factoredToOrig=new IntList(2*originalDomain.size());
        origToFactored=new int[originalDomain.size()][];
        for(int i=0;i<originalDomain.size();i++){
            createVar(i);
        }
    }
    private static int sum(IntList vals){
        int total=0;
        for(int i=0;i<vals.size();i++){
            total+=vals.get(i);
        }
        return total;
    }
    
    IntMap factor(int value){
        int current=value;
        IntMap result=new IntMap();
        for(int i=0;i<primes.length;i++){
            if(current==1){
                return result;
            }
            int power=0;
            while(current%primes[i]==0){
                power++;
                current/=primes[i];
            }
            if(power!=0){
                result.putAtEnd(primes[i], power);
            }
        }
	result.putAtEnd(current,1);
	return result;
        //throw new IllegalStateException("not factored "+value+" to "+current);
    }
                
    private void createVar(int i){
        int osize=originalDomain.size(i);
        IntMap factorization=factor(osize);
        origToFactored[i]=new int[sum(factorization.values())];
        int current=0;
        for(int j=0;j<factorization.size();j++){
            int factor=factorization.keys().get(j);
            int power=factorization.get(factor);
            for(int k=0;k<power;k++){
                origToFactored[i][current++]=factoredDomain.addDim(factor);
                factoredToOrig.add(i);
            }
        }
    }
    
    
    public Table convert(Table orig){
        return new Table(factoredDomain, convert(orig.vars()),orig.values());
    }
    
    public IntMap convert(IntMap orig){
        IntMap result=new IntMap(2*orig.size());
        for(int i=0;i<orig.size();i++){
            int ovar=orig.keys().get(i);
            int oval=orig.get(ovar);
            int current=oval;
            for(int j=0;j<origToFactored[ovar].length;j++){
                int nvar=origToFactored[ovar][j];
                int fsize=factoredDomain.size(nvar);
                int nval=current % fsize;
                result.putAtEnd(nvar,nval);
                current/=fsize;
            }
        }
        return result;
    }
    
    public IntMap toOriginal(IntMap bin){
        IntMap result=new IntMap(bin.size());
        IntSet bvars=new IntSet(bin.keys());
        while(!bvars.isEmpty()){
            int bind=bvars.largest();
            int ovar=factoredToOrig.get(bind);
            int ind=0;
            int offset=1;
            for(int i=0;i<origToFactored[ovar].length;i++){
                ind+=bin.get(origToFactored[ovar][i])*offset;
                offset*=factoredDomain.size(origToFactored[ovar][i]);
                bvars.remove(origToFactored[ovar][i]);
            }
            result.put(ovar, ind);
        }
        return result;
    }
    public IntSet convert(IntSet orig){
        IntSet result=new IntSet(3*orig.size());
        for(int i=0;i<orig.size();i++){
            int v=orig.get(i);
            for(int j=0;j<origToFactored[v].length;j++){
                result.appendAdd(origToFactored[v][j]);
            }
        }
        return result;
    }
    public IntList convert(IntList orig){
        IntList result=new IntList(3*orig.size());
        for(int i=0;i<orig.size();i++){
            int v=orig.get(i);
            for(int j=0;j<origToFactored[v].length;j++){
                result.add(origToFactored[v][j]);
            }
        }
        return result;
    }
    
    public Table[] convert(Table[] vals){
        Table[] result=new Table[vals.length];
        for(int i=0;i<vals.length;i++){
            result[i]=convert(vals[i]);
        }
        return result;
    }
}
