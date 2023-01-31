package il2.model;

import il2.util.*;

/** @author james d park
    @since 20030602 */
public class BayesianNetwork{
    Table[] cpts;

    public BayesianNetwork(Table[] cpts){
	this.cpts=cpts;
    }

    /** @author keith cascio
    	@since 20060123 */
    public Table forVariable( int var ){
        return cpts[var];
    }

    public Table[] cpts(){
	return cpts;
    }
    public Domain domain(){
        return cpts[0].domain();
    }
    public int size(){
	return cpts.length;
    }
    public Graph generateGraph(){
        Graph g=new Graph(cpts.length);
        for(int i=0;i<cpts.length;i++){
            g.add(i);
            IntSet vars=cpts[i].vars();
            for(int j=0;j<vars.size();j++){
                int n=vars.get(j);
                if(n!=i){
                    g.addEdge(n,i);
                }
            }
        }
        return g;
    }
    public IntMap generateConsistentAssignment(){
        IntMap current=new IntMap(cpts.length);
        java.util.Random r=new java.util.Random();
        double logProb=0;
        for(int i=0;i<cpts.length;i++){
            double[] vals=cpts[i].shrink(current).values();
            int val=r.nextInt(vals.length);
            boolean found=false;
            for(int j=0;j<vals.length;j++){
                if(vals[val]>0){
                    found=true;
                    current.put(i, val);
                    logProb+=Math.log(vals[val]);
                    break;
                }
                val=(val+1)%vals.length;
            }
            if(!found){
                throw new IllegalStateException();
            }
        }
        return current;
    }

    public Table[] simplify(IntSet interesting,IntMap evidence){
	Table[] temp=simplify(interesting.union(evidence.keys()));
	Table[] temp2=Table.shrink(temp,evidence);
	return temp2;
    }

    public boolean[] isRelevant(IntSet interesting,IntMap evidence){
	return isRelevant(interesting.union(evidence.keys()));
    }

    public boolean[] isRelevant(IntSet variablesOfInterest){
	boolean[] marked=new boolean[cpts.length];
	for(int i=0;i<variablesOfInterest.size();i++){
	    mark(marked,variablesOfInterest.get(i));
	}
	return marked;
    }

    public Table[] simplify(IntSet variablesOfInterest){
	boolean[] marked=isRelevant(variablesOfInterest);
	Table[] result=new Table[ArrayUtils.count(marked, true)];
	int count=0;
	for(int i=0;i<cpts.length;i++){
	    if(marked[i]){
		result[count++]=cpts[i];
	    }
	}
	return result;
    }


    private void mark(boolean[] marked,int ind){
	if(!marked[ind]){
	    marked[ind]=true;
	    IntSet family=cpts[ind].vars();
	    int parentCount=family.size()-1;
	    for(int i=0;i<parentCount;i++){
		mark(marked,family.get(i));
	    }
	}
    }

    public void sanityCheck(){
	for(int i=0;i<cpts.length;i++){
	    double[] mass=cpts[i].forget(i).values();
	    for(int j=0;j<mass.length;j++){
		if(Math.abs(mass[j]-1)>.000001){
		    System.err.println(cpts[i]);
		    throw new IllegalStateException("Potential is not a CPT for "+cpts[i].domain().name(i));
		}
	    }
	    IntSet vars=cpts[i].vars();
	    for(int j=0;j<vars.size();j++){
		if(vars.get(j)>i){
		    System.err.println(cpts[i]);
		    System.err.println(vars);
		    throw new IllegalStateException("Bad variables for "+i+".");
		}
	    }
	}
    }

    public BayesianNetwork ensureNormalized(){
	Table[] newCPTs=new Table[cpts.length];
	for(int i=0;i<cpts.length;i++){
	    newCPTs[i]=cpts[i].makeCPT(i);
	}
	return new BayesianNetwork(newCPTs);
    }


}
