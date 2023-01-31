package il2.inf.bp;

import il2.inf.bp.schedules.MessagePassingScheduler;
import il2.model.Table;
import il2.util.IntSet;
import il2.util.Pair;

/**
 * Max-Product BP with more efficient message updates
 *
 * as in "Efficient Belief Propagation for Early Vision"
 * by Felzenszwalb and Huttenlocher CVPR 2004, IJCV 2006
 *
 * this assumes smoothness cost:
 *   \rho_p(d_s,d_t) = min(\lambda |d_s-d_t|,T)
 *
 * as in "Symmetric Stereo Matching for Occlusion Handling"
 * by Sun, Li, Kang, and Shum CVPR 2005
 */
public class MaxProductLinearSmoothness extends MaxProduct {
    double Lambda, T, Beta_o;

    public MaxProductLinearSmoothness(Table[] tables, 
            MessagePassingScheduler s, double lambda, double t, double beta,
            int mi, long tm, double ct) {
        super(tables,s,mi,tm,ct);
        this.Lambda = lambda;
        this.T = t;
        this.Beta_o = beta;
    }
    
    public MaxProductLinearSmoothness(Table[] tables, 
            double lambda, double t, double beta, int mi, long tm, double ct){
        super(tables,mi,tm,ct);
        this.Lambda = lambda;
        this.T = t;
        this.Beta_o = beta;
    }

    protected Table computeMessage(Pair pair) {
        Table[] tabs = collectTables(pair);
        if ( pair.s1 >= domain.size() ) // if table
            return computeMessageSpecial(tabs);
        int var = scheduler.varOfPair(pair);
        Table msg = Table.varTable(domain,var);
        msg.multiplyAndProjectMaxInto(tabs);
        msg.normalizeInPlace();
        return msg;
    }

    public Table computeMessageSpecial(Table[] tables) {
        Table m = null, M = null;
        if (tables.length == 2) {
            if      ( tables[0].vars().size() == 1 ) m = tables[0];
            else if ( tables[1].vars().size() == 1 ) m = tables[1];
            if      ( tables[0].vars().size() == 2 ) M = tables[0];
            else if ( tables[1].vars().size() == 2 ) M = tables[1];
        }
        if ( m == null || M == null ) {
            String st = "expecting 1 node and 1 edge table";
            throw new IllegalStateException(st);
        }
        int var1 = m.vars().get(0);
        int var2 = var1 == M.vars().get(1) ? M.vars().get(0):M.vars().get(1);
        int n = domain.size(var1) - 1; // # of depth states

        double[] vals = m.values().clone();
        double trunk = vals[0]; // truncation term
        for (int i = 1; i < n; i++)
            if ( vals[i] > trunk ) trunk = vals[i];
        double occ = Math.exp(-Beta_o)*trunk;
        trunk *= Math.exp(-T);

        double val;
        for (int i = 1; i < n; i++) {
            val = vals[i-1] * Math.exp(-Lambda);
            if ( val > vals[i] ) vals[i] = val;
        }
        for (int i = n-2; i >= 0; i--) {
            val = vals[i+1] * Math.exp(-Lambda);
            if ( val > vals[i] ) vals[i] = val;
        }
        for (int i = 0; i < n; i++)
            if ( trunk > vals[i] ) vals[i] = trunk;
        val = vals[n] * Math.exp(-Beta_o);
        for (int i = 0; i < n; i++)
            if ( val > vals[i] ) vals[i] = val;
        if ( occ > vals[n] ) vals[n] = occ;

        IntSet vars = new IntSet(new int[]{var2});
        Table t = new Table(domain,vars,vals);
        t.normalizeInPlace();
        return t;
    }

    /**
     * for testing purposes
     */
	public static double[] discontinuityCpt(int numStates,
                                            double Lambda, double T,
                                            double Beta_o) {
        int n = numStates;
		double[] vals = new double[(n + 1) * (n + 1)];
        double cost;
		
		for (int i = 0; i < n; ++i)
            for (int j = 0; j < n; ++j) {
                cost = Math.min(T, Lambda*Math.abs(i-j));
                vals[j*(n+1) + i] = Math.exp(-cost);
            }
		
		for (int i = 0; i < n; ++i) {
			vals[i*(n+1) + n] = Math.exp(-Beta_o);
			vals[n*(n+1) + i] = Math.exp(-Beta_o);
		}
		vals[(n+1)*(n+1) - 1] = Math.exp(0);

        return vals;
	}
}
