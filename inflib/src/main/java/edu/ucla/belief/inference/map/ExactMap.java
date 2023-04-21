package edu.ucla.belief.inference.map;

import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.EliminationHeuristic;
import edu.ucla.util.JVMProfiler;

import il2.util.*;
import il2.model.*;
import il2.inf.structure.*;
import il2.inf.map.*;
import il2.bridge.Converter;
import il2.inf.*;

import java.util.*;

/** @author JD Park
	@author Mark Chavira
	@author Keith Cascio
*/
public class ExactMap
{
	/*public static class Result{
	public final Map result;
	public final double score;
	public final boolean finished;
	public final double PrE;
		private Result(Map m,double s,boolean f,double pre){
			result=m;
			score=s;
			finished=f;
			PrE=pre;
		}
	}*/

	/** @author Keith Cascio
	@since 062104 */
	public static class SizedOrder
	{
		public final IntList order;
		public final double size;
		public SizedOrder( IntList order, double size ){
			this.order = order;
			this.size = size;
		}
	}

    public static MapSearch.MapInfo computeMap(BeliefNetwork bn,Set vars,Map evid,double seconds){
	return computeMap(bn,vars,evid,seconds,0,null);
    }
    public static MapSearch.MapInfo computeMap(BeliefNetwork bn,Set vars,Map evid,double seconds,int widthBarrier){
	return computeMap(bn,vars,evid,seconds,widthBarrier,null);
    }
	public static MapSearch.MapInfo computeMap(BeliefNetwork bn,Set vars,Map evid,double seconds,int widthBarrier,List orderHint){
		Converter converter=new Converter();
		BayesianNetwork bn2=converter.convert(bn);
		IntMap evidence=converter.convert(evid);
		IntSet mapvars=converter.convert(vars);

		long start_ms = System.currentTimeMillis();
		long start_cpu_ms = JVMProfiler.getCurrentThreadCpuTimeMS();

		Table[] tables=bn2.simplify(mapvars,evidence);

		long end_cpu_ms = JVMProfiler.getCurrentThreadCpuTimeMS();
		long end_ms = System.currentTimeMillis();

		long start_fact_ms = System.currentTimeMillis();
		long start_fact_cpu_ms = JVMProfiler.getCurrentThreadCpuTimeMS();

		Factorizer fact=new Factorizer(tables[0].domain());
		Table[] nt=fact.convert(tables);
		IntSet nmapvars=fact.convert(mapvars);
		IntMap nevidence=fact.convert(evidence);
		IntList norder;
		if( orderHint == null ) norder=null;
		else norder = fact.convert( converter.convert( orderHint ) );

		long end_fact_cpu_ms = JVMProfiler.getCurrentThreadCpuTimeMS();
		long end_fact_ms = System.currentTimeMillis();

		MapSearch.MapInfo info=runMapTest(nt, nmapvars,nevidence,norder,seconds,widthBarrier);
		info.pruneDurationMillisElapsed = end_ms - start_ms;
		info.pruneDurationMillisProfiled = end_cpu_ms - start_cpu_ms;
		info.initDurationMillisElapsed += end_fact_ms - start_fact_ms;
		info.initDurationMillisProfiled += end_fact_cpu_ms - start_fact_cpu_ms;

		//MapSearch.MapResult res = (MapSearch.MapResult)info.results.get (0);
		//IntMap actual=fact.toOriginal(res.result);
		//Map result=converter.convert(actual);
		//return new Result(result,res.score,info.finished,info.probOfEvidence);

		return convert( info, fact, converter );
	}

	/** @since 062104 */
	public static MapSearch.MapInfo computeMapSloppy( BeliefNetwork bn, Set vars, Map evid, double seconds, int widthBarrier, double slop ){
		return computeMapSloppy( bn, vars, evid, seconds, widthBarrier, slop, null );
	}
	/** @since 062104 */
	public static MapSearch.MapInfo computeMapSloppy( BeliefNetwork bn, Set vars, Map evid, double seconds, int widthBarrier, double slop, List orderHint )
	{
		Converter converter=new Converter();
		BayesianNetwork bn2=converter.convert(bn);

		IntMap evidence=converter.convert(evid);
		IntSet mapvars=converter.convert(vars);

		long start_ms = System.currentTimeMillis();
		long start_cpu_ms = JVMProfiler.getCurrentThreadCpuTimeMS();

		Table[] tables=bn2.simplify(mapvars,evidence);

		long end_cpu_ms = JVMProfiler.getCurrentThreadCpuTimeMS();
		long end_ms = System.currentTimeMillis();

		long start_fact_ms = System.currentTimeMillis();
		long start_fact_cpu_ms = JVMProfiler.getCurrentThreadCpuTimeMS();

		Factorizer fact=new Factorizer(tables[0].domain());
		Table[] nt=fact.convert(tables);
		IntSet nmapvars=fact.convert(mapvars);
		IntMap nevidence=fact.convert(evidence);
		IntList norder = (orderHint==null) ? (IntList)null : fact.convert(converter.convert(orderHint));

		long end_fact_cpu_ms = JVMProfiler.getCurrentThreadCpuTimeMS();
		long end_fact_ms = System.currentTimeMillis();

		//MapSearch.MapResult res=runMapTest(nt, nmapvars,nevidence,norder,seconds,widthBarrier);
		//IntMap actual=fact.toOriginal(res.result);
		//Map result=converter.convert(actual);
		//return new Result(result,res.score,res.finished);
		List cptList = java.util.Arrays.asList( nt );
		SizedOrder order = decideOrder( nt, cptList, norder );
		double promotionBound=order.size-widthBarrier;

		MapSearch.MapInfo info = MapSearch.computeMAP(cptList,nmapvars,order.order,seconds,promotionBound,slop);
		info.pruneDurationMillisElapsed = end_ms - start_ms;
		info.pruneDurationMillisProfiled = end_cpu_ms - start_cpu_ms;
		info.initDurationMillisElapsed += end_fact_ms - start_fact_ms;
		info.initDurationMillisProfiled += end_fact_cpu_ms - start_fact_cpu_ms;

		return convert( info, fact, converter );
	}

	/** @author Keith Cascio @since 092804 */
	private static MapSearch.MapInfo convert( MapSearch.MapInfo info, Factorizer fact, Converter converter )
	{
		for( Iterator it = info.results.iterator(); it.hasNext(); ){
			((MapSearch.MapResult) it.next()).getConvertedInstatiation( fact, converter );
		}
		return info;
	}

	/* @since 062104
	private static Result[] convert( MapSearch.MapInfo info, Factorizer fact, Converter converter)
	{
		Result[] ret = new Result[ info.results.size() ];
		MapSearch.MapResult res = null;
		IntMap actual = null;
		Map result = null;
		int i = (int)0;
		for( Iterator it = info.results.iterator(); it.hasNext(); )
		{
			res = (MapSearch.MapResult) it.next();
			actual = fact.toOriginal( res.result );
			result = converter.convert( actual );
			ret[i++] = new Result( result, res.score, info.finished, info.probOfEvidence );
		}
		return ret;
	}*/

	/** @since 062104 */
	private static SizedOrder decideOrder( Table[] cpts, List cptList, IntList orderHint )
	{
		EliminationOrders.Record rec2 = EliminationHeuristic.MIN_FILL.getEliminationOrder( cpts );//EliminationOrders.minFill(cptList);
		IntList order=rec2.order;
		double size=rec2.size;

		if(orderHint!=null){
			//System.err.println("original order= "+orderHint);
			orderHint=stripIrrelevant(cpts, orderHint);
			//System.err.println("stripped order= "+orderHint);
			double size2=EliminationOrders.computeSize(cptList,orderHint);
			if(size2<rec2.size){
				order=orderHint;
				size=size2;
			}
		}

		return new SizedOrder( order, size );
	}

	private static MapSearch.MapInfo runMapTest(Table[] cpts,IntSet mapvars,IntMap evidence,IntList orderHint,double timeBound,int widthBarrier){
		List cptList = java.util.Arrays.asList( cpts );
		SizedOrder order = decideOrder( cpts, cptList, orderHint );
		double promotionBound=order.size-widthBarrier;
		return MapSearch.computeMAP(cptList,mapvars,order.order,timeBound,promotionBound);
	}


    private static IntList stripIrrelevant(Table[] cpts,IntList original){
        IntList result=new IntList(original.size());
        IntSet vars=getVars(cpts);
        for(int i=0;i<original.size();i++){
            if(vars.contains(original.get(i))){
                result.add(original.get(i));
            }
        }
        return result;
    }
    private static IntSet getVars(Table[] cpts){
        IntSet is=new IntSet(cpts.length);
        for(int i=0;i<cpts.length;i++){
            is=is.union(cpts[i].vars());
        }
        return is;
    }
}
