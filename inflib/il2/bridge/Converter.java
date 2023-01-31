package il2.bridge;
import il2.model.*;
import il2.util.*;
import java.util.*;
import edu.ucla.belief.FiniteVariable;
import edu.ucla.structure. DirectedEdge;
import edu.ucla.structure.         Edge;

public class Converter{
    Map index;
    List vars;
    Domain domain;
    edu.ucla.belief.BeliefNetwork myBeliefNetwork;
    BayesianNetwork myBayesianNetwork;

    /** 012904 */
    public Converter()
    {
    	//System.out.println( "Converter()" );
    	//new Throwable().printStackTrace();
    }

    /**
     * Initializes the fields without requiring a call to convert ().  James
     * set this class up so that one would always call convert () before
     * using the class.  convert () initializes the fields, performs some
     * conversions and returns.  I wanted to initialize the fields without
     * performing any conversions and without needing to specify a belief
     * network and without affecting others' code.  This method serves that
     * purpose.
     *
     * @param variables the inflib variables in topological order.
     * @author Mark Chavira
     * @since 011704
     */

    public void init (java.util.Collection variables) {
      vars = new java.util.ArrayList (variables);
      domain = new Domain (vars.size());
      index = new HashMap (vars.size());
      for (int i = 0; i < vars.size(); i++) {
        FiniteVariable fv=(FiniteVariable)vars.get(i);
        index.put(fv,new Integer(i));
        domain.addDim(fv.getID(),fv.instances());
      }
    }

    /** @author Keith Cascio
    	@since 020904 */
    public edu.ucla.belief.BeliefNetwork getBeliefNetwork()
    {
    	return myBeliefNetwork;
    }

    /** @author Keith Cascio
    	@since 20060123 */
    public BayesianNetwork getBayesianNetwork(){
    	return myBayesianNetwork;
    }

    /**
    	@author Keith Cascio
    	@since 112503
    */
    public Map getIndex()
    {
    	return index;
    }

    /**
    	@author Keith Cascio
    	@since 120103
    */
    public Domain getDomain()
    {
    	return domain;
    }

	public BayesianNetwork convert( edu.ucla.belief.BeliefNetwork bn )
	{
		if( myBayesianNetwork == null ){
			myBeliefNetwork = bn;
			vars=bn.topologicalOrder();
			il2.model.Table[] tables=new il2.model.Table[vars.size()];
			domain=new Domain(vars.size());
			index=new HashMap(vars.size());
			for(int i=0;i<vars.size();i++){
				FiniteVariable fv=(FiniteVariable)vars.get(i);
				index.put(fv,new Integer(i));
				domain.addDim(fv.getID(),fv.instances());
				tables[i]=convert( fv.getCPTShell( fv.getDSLNodeType() ).getCPT() );
			}
			myBayesianNetwork = new BayesianNetwork(tables);
		}
		else if( myBeliefNetwork != bn ) throw new IllegalArgumentException();

		return myBayesianNetwork;
	}

	/** @since 061404 */
	public il2.model.Table[] convertTables( edu.ucla.belief.BeliefNetwork bn, edu.ucla.belief.QuantitativeDependencyHandler handler )
	{
		if( myBeliefNetwork != bn ) throw new IllegalArgumentException();

		il2.model.Table[] tables=new il2.model.Table[vars.size()];
		int i=0;
		for( Iterator it = vars.iterator(); it.hasNext(); ){
			tables[i++] = convert( handler.getCPTShell( (FiniteVariable) it.next() ).getCPT() );
		}

		return tables;
	}

	public FiniteVariable convert(int var){
		return (FiniteVariable)vars.get(var);
	}

    public int convert( FiniteVariable fv )
    {
    	if( index.containsKey( fv ) ) return ((Integer)index.get(fv)).intValue();
    	else throw new ConversionException( this, fv );
    }

	/** @since 20091219 */
	public DirectedEdge[]   convertDirected( int[][]          edges ){
		if(                                                   edges == null ){ return null; }
		   DirectedEdge[] ret    = new DirectedEdge[          edges.length ];
		for( int i=0;         i  <                            edges.length;                 i++     ){
		                  ret[i] = new DirectedEdge( convert( edges[i][0] ), convert( edges[i][1] ) );
		}
		return            ret;
	}

	/** @since 20091219 */
	public         Edge[] convertUndirected( int[][]          edges ){
		if(                                                   edges == null ){ return null; }
		           Edge[] ret    = new         Edge[          edges.length ];
		for( int i=0;         i  <                            edges.length;                 i++     ){
		                  ret[i] = new         Edge( convert( edges[i][0] ), convert( edges[i][1] ) );
		}
		return            ret;
	}

    public Set convert(IntSet v){
	Set s=new HashSet(v.size());
	for(int i=0;i<v.size();i++){
	    s.add(vars.get(v.get(i)));
	}
	return s;
    }

    /** @since 20060224 */
    public List convert( IntList variablesToConvert ){
        ArrayList ret = new ArrayList( variablesToConvert.size() );
        for( int i=0; i<variablesToConvert.size(); i++ ){
            ret.add( vars.get( variablesToConvert.get(i) ) );
        }
        return ret;
    }

    public IntSet convert(Set vars){
	IntSet v=new IntSet(vars.size());
	for(Iterator iter=vars.iterator();iter.hasNext();){
	    v.add(convert((FiniteVariable)iter.next()));
	}
	return v;
    }

    public IntList convert(List vars){
        IntList v=new IntList(vars.size());
        for(Iterator iter=vars.iterator();iter.hasNext();){
	    v.add(convert((FiniteVariable)iter.next()));
	}
	return v;
    }

    /** @author keith cascio
        @since 20060123 */
    public List convertToList( IntSet variablesToConvert ){
        ArrayList ret = new ArrayList( variablesToConvert.size() );
        for( int i=0; i<variablesToConvert.size(); i++ ){
            ret.add( vars.get( variablesToConvert.get(i) ) );
        }
        return ret;
    }

    public IntMap convert(Map map){
	int[] keys=new int[map.size()];
	int[] values=new int[keys.length];
	int i=0;
	for(Iterator iter=map.entrySet().iterator();iter.hasNext();i++){
	    Map.Entry entry=(Map.Entry)iter.next();
	    FiniteVariable fv=(FiniteVariable)entry.getKey();
	    keys[i]=convert(fv);
	    values[i]=fv.index(entry.getValue());
	}
	return new IntMap(keys,values);
    }

    public Map convert(IntMap map){
	Map result=new HashMap(map.size());
	for(int i=0;i<map.size();i++){
	    FiniteVariable fv=convert(map.keys().get(i));
	    Object obj=fv.instance(map.values().get(i));
	    result.put(fv,obj);
	}
	return result;
    }

    public edu.ucla.belief.Table convert(il2.model.Table t,List varOrder){
	FiniteVariable[] fv=new FiniteVariable[varOrder.size()];
	varOrder.toArray(fv);
	return convert(t).permute(fv);
    }

	public edu.ucla.belief.Table convert(il2.model.Table t){
		IntSet vars=t.vars();
		List l=new ArrayList(vars.size());
		for(int i=vars.size()-1;i>=0;i--){
			l.add(convert(vars.get(i)));
		}
		double[] vals=(double[])t.values().clone();
		return new edu.ucla.belief.Table(l,vals);
	}

	/** @since 061504 */
	public edu.ucla.belief.Table[] convert( il2.model.Table[] tables ){
		edu.ucla.belief.Table[] ret = new edu.ucla.belief.Table[tables.length];
		for( int i=0; i<tables.length; i++ ){
			ret[i] = convert( tables[i] );
		}
		return ret;
	}

    public il2.model.Table convert(edu.ucla.belief.Table t){
        List fvl=t.index().variables();
	FiniteVariable[] fv=new FiniteVariable[fvl.size()];
	fvl.toArray(fv);
	int[] inds=new int[fv.length];
	int[] negInds=new int[fv.length];
	for(int i=0;i<inds.length;i++){
	    inds[i]=((Integer)index.get(fv[i])).intValue();
	    negInds[i]=-inds[i];
	    //the negative is because in the old table, entry 0 is most
	    //significant, and in the new table, it is least significant
	}
	int[] perm=ArrayUtils.sortedInds(negInds);
	FiniteVariable[] permfv=new FiniteVariable[perm.length];
	for(int i=0;i<perm.length;i++){
	    permfv[i]=fv[perm[i]];
	}
	edu.ucla.belief.Table t2=t.permute(permfv);
	IntSet is=new IntSet(inds);
	il2.model.Table result=new il2.model.Table(domain,is);

	System.arraycopy(t2.dataclone(),0,result.values(),0,result.sizeInt());
	return result;
    }
}
