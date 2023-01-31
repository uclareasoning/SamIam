package il2.util;

import edu.ucla.belief.FiniteVariable;

import il2.bridge.Converter;
import il2.model.Table;
import il2.model.BayesianNetwork;
import edu.ucla.util.*;
import java.util.*;

/** @author hei chan
	@since 20060119 */
public class MPE
{
    protected ArrayList instList;

    public MPE()
    {
        instList = new ArrayList(1);
        instList.add(new Instantiation());
    }

    public MPE(ArrayList instList)
    {
        this.instList = instList;
    }

    public MPE(IntSet vars, int[] current)
    {
        instList = new ArrayList(1);
	Instantiation inst = new Instantiation(vars, current);
	instList.add(inst);
    }

    /** @author keith cascio
    	@since 20060123 */
    public int size(){
    	return instList.size();
    }

    public Map[] convertToIL1( Converter converter )
    {
        Object[] instArray = instList.toArray();
        Map[] maps = new Map[instArray.length];
        for (int i = 0; i < instArray.length; i++)
             maps[i] = ((Instantiation)instArray[i]).convertToIL1(converter);
        return maps;
    }

    /** @author keith cascio
    	@since 20060123 */
/*
    public Map toMapIL1( Converter converter ){
		BayesianNetwork bn = converter.getBayesianNetwork();
		// <FiniteVariable,Object>
		Map mapIL1 = new HashMap( bn.size() );

		Map mpeMap;
		Integer key;
		int[] current;
		FiniteVariable il1Var;
		Object instance;
		Table table;
		IntSet vars;
		int numVars;
		int il2Var;
		int indexInstance;
		for( Iterator iterOverInsts = instList.iterator(); iterOverInsts.hasNext(); ){
			mpeMap = (Map) iterOverMaps.next();
			for( Iterator iterOverKeys = mpeMap.keySet().iterator(); iterOverKeys.hasNext(); ){
				key     = (Integer) iterOverKeys.next();
				current = (int[]) mpeMap.get( key );

				il2Var  = key.intValue();
				table   = bn.forVariable( il2Var );
				vars    = table.vars();
				numVars = vars.size();

				for( int i=0; i<numVars; i++ ){
					il2Var        = vars.get(i);
					il1Var        = converter.convert( il2Var );
					indexInstance = current[i];
					instance      = il1Var.instance( indexInstance );
					recordInstantiation( mapIL1, il2Var, il1Var, indexInstance, instance );
				}
			}
		}

		return mapIL1;
    }
*/
    /** @author keith cascio
    	@since 20060123 */
/*
    private void recordInstantiation( Map map, int il2Var, FiniteVariable il1Var, int index, Object instance ){
    	if( map.containsKey( il1Var ) ){
    		if( map.get(il1Var) != instance ) throw new IllegalStateException( "Error: when converting il2.util.MPE to IL1-style Map, disagreeing instantiations recorded for variable IL2("+il2Var+")/IL1("+il1Var+"): existing("+map.get(il1Var)+") ... new("+index+"/"+instance+")" );
    	}
    	map.put( il1Var, instance );
    }
*/
    public void add(Instantiation inst)
    {
        instList.add(inst);
    }

    public void addAll(MPE mpe)
    {
        Object[] instArray = mpe.instList.toArray();
        for (int i = 0; i < instArray.length; i++)
            instList.add(instArray[i]);
    }

    public static MPE merge(MPE mpe1, MPE mpe2)
    {
	Object[] instArray1 = mpe1.instList.toArray();
	Object[] instArray2 = mpe2.instList.toArray();
        ArrayList instList = new ArrayList(instArray1.length + instArray2.length);
        for (int i = 0; i < instArray1.length; i++)
        {
            for (int j = 0; j < instArray2.length; j++)
            {
                instList.add(((Instantiation)instArray1[i]).merge((Instantiation)instArray2[j]));
            }
        }
        return new MPE(instList);
    }

    public boolean contains(IntSet vars, int[] current)
    {
        Instantiation inst = new Instantiation(vars, current);
	Object[] instArray = instList.toArray();
        for (int i = 0; i < instArray.length; i++)
            if (inst.consistent((Instantiation)instArray[i]))
                return true;
        return false;
    }

    public String toString()
    {
        String s = "";
        Object[] instArray = instList.toArray();
        for (int i = 0; i < instArray.length; i++)
            s += ((Instantiation)instArray[i]).toString();
        return s;
    }

/*
    public static ArrayList flatten(MPE mpe, Table[] tables)
    {       
        ArrayList mpeIntMap = new ArrayList(mpe.mpes.size());
        for (int i = 0; i < mpe.mpes.size(); i++)
        {   
            Map map = (Map)mpe.mpes.get(i);
            Object[] keys = map.keySet().toArray();
            IntMap intMap = new IntMap();
            for (int j = 0; j < keys.length; j++)
            {
                int[] current = (int[])map.get(keys[j]);
                int var = ((Integer)keys[j]).intValue();
                intMap.put(var, current[tables[var].vars().indexOf(var)]); 
            }
            mpeIntMap.add(intMap);
        }
        return mpeIntMap;
    }
*/
}
