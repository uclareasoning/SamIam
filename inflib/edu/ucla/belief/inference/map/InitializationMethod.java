package edu.ucla.belief.inference.map;

import java.util.*;

import edu.ucla.belief.*;
import edu.ucla.belief.inference.BeliefCompilation;

/**
	@author Keith Cascio
	@since 021903
*/
public abstract class InitializationMethod
{
	abstract public Map getInitial( BeliefNetwork bn, Map evidence, Set mapvars, BeliefCompilation comp );
	abstract public int getPenalty();
	abstract public String getJavaCodeName();

	/** @since 051004 */
	public static InitializationMethod getDefault(){
		return SEQ;
	}

	public static final InitializationMethod RANDOM = new InitializationMethod()
	{
		public Map getInitial( BeliefNetwork bn, Map evidence, Set mapvars, BeliefCompilation comp )
		{
			Random r=new Random();
			Map result=new HashMap(mapvars.size());
			for(Iterator iter=mapvars.iterator();iter.hasNext();)
			{
				FiniteVariable var=(FiniteVariable)iter.next();
				result.put(var,var.instance(r.nextInt(var.size())));
			}

			return result;
		}

		public int getPenalty()
		{
			return (int)0;
		}

		public String toString()
		{
			return "Random";
		}

		public String getJavaCodeName(){
			return "RANDOM";
		}
	};

	public static final InitializationMethod MPE = new InitializationMethod()
	{
		public Map getInitial( BeliefNetwork bn, Map evidence, Set mapvars, BeliefCompilation comp )
		{
			Set allVars=new HashSet(bn);
			allVars.removeAll(evidence.keySet());
			MapEngine mpe=new MapEngine(bn,allVars,evidence);
			return mpe.getInstance();
		}

		public int getPenalty()
		{
			return (int)1;
		}

		public String toString()
		{
			return "MPE";
		}

		public String getJavaCodeName(){
			return "MPE";
		}
	};

	public static final InitializationMethod ML = new InitializationMethod()
	{
		public Map getInitial( BeliefNetwork bn, Map evidence, Set mapvars, BeliefCompilation comp )
		{
			Map result=new HashMap(mapvars.size());
			for(Iterator iter=mapvars.iterator();iter.hasNext();)
			{
				FiniteVariable var=(FiniteVariable)iter.next();
				Table partials=comp.getPartial(var);
				int ind = partials.maxInd();
				result.put(var,var.instance(ind));
			}

			return result;
		}

		public int getPenalty()
		{
			return (int)1;
		}

		public int maxInd( double[] values )
		{
			int best=0;
			for(int i=0;i<values.length;i++) if(values[i]>values[best]) best=i;

			return best;
		}

		public String toString()
		{
			return "Maximum Likelihoods";
		}

		public String getJavaCodeName(){
			return "ML";
		}
	};

	public static final InitializationMethod SEQ = new InitializationMethod()
	{
		public Map getInitial( BeliefNetwork bn, Map evidence, Set mapvars, BeliefCompilation comp )
		{
			myMAPVars = mapvars;

			MapApproximator app=new PHillRR();
			app.init(comp,mapvars);
			MapApproximator.Instance inst=app.new Instance(new HashMap());
			app.run(inst,mapvars.size());
			MapApproximator.Instance result=app.bestInstance();
			if(!result.isComplete())
			{
				System.err.println("\nunassigned="+result.unassigned()+"\n\n");
				for(int i=0;i<mapvars.size();i++)
				{
					System.err.print(result.value(i)+" ");
				}

				throw new IllegalStateException();
			}

			app.clearAssignments();
			return result.mapping();
		}

		public int getPenalty()
		{
			return myMAPVars.size();
		}

		protected Set myMAPVars;

		public String toString()
		{
			return "Sequential";
		}

		public String getJavaCodeName(){
			return "SEQ";
		}
	};

	public static final InitializationMethod[] ARRAY = new InitializationMethod[]{ RANDOM, MPE, ML, SEQ };
}
