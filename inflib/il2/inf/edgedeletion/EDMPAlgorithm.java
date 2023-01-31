package il2.inf.edgedeletion;

import il2.model.BayesianNetwork;
import il2.model.Domain;
import il2.model.Table;
import il2.util.IntMap;
import il2.util.IntSet;

import il2.inf.Algorithm;
import il2.inf.Algorithm.Setting;

import java.util.Map;

public class EDMPAlgorithm extends EDAlgorithm {

    // AC: needs new interface or something ...
	//il2.inf.PartialDerivativeEngine ie;
    il2.inf.jointree.NormalizedMaxSSAlgorithm ie;
	public static final Algorithm ALGORITHM_DEFAULT = Algorithm.ssnormalizedmax;

	public EDMPAlgorithm( BayesianNetwork bn, int[][] ed, 
                          int mi, long tm, double ct, 
                          Algorithm alg, Map<Setting,?> settings ){
        super(bn,ed,mi,tm,ct,ALGORITHM_DEFAULT,settings);
        this.ie = (il2.inf.jointree.NormalizedMaxSSAlgorithm)super.ie;
	}
  
    protected void dampPotential(double[] oldvals, double[] newvals) {
        double p = 0.5;
        for (int i = 0; i < newvals.length; i++)
            newvals[i] = p*oldvals[i] + (1.0-p)*newvals[i];
    }

    /*
	protected void normalizeEdgeParameters(double[] t1, double[] t2) {
		double sum1 = 0.0, sum2 = 0.0;
		for (int i = 0; i < t1.length; i++) {
			sum1 = Math.max(sum1,t1[i]);
			sum2 = Math.max(sum1,t2[i]);
		}
		for (int i = 0; i < t1.length; i++) {
			if (sum1 > 0.0) t1[i] /= sum1;
			if (sum2 > 0.0) t2[i] /= sum2;
		}
	}
    */

	public double[] getZeroMiCorrections() {
		double[] z_ij = new double[ned];
		for (int edge = 0; edge < ned; edge++) {
			double[] t1 = edNet.getPmTable(edge).values();
			double[] t2 = edNet.getSeTable(edge).values();
            z_ij[edge] = t1[0]*t2[0];
            for (int i = 1; i < t1.length; i++)
                z_ij[edge] = Math.max(z_ij[edge],t1[i]*t2[i]);
		}
		return z_ij;
	}

	public double getLogZeroMiCorrection() {
		double[] z_ij = getZeroMiCorrections();
		double z = 0.0;
		for (int edge = 0; edge < ned; edge++)
			z += Math.log( z_ij[edge] );
		return z;
	}

    /**
     * This computes an approximation to log Pr(MPE).
     *
     * via Compensation.  Analagous to the approximation for Pr(e) and
     * the Bethe free energy.
     */
    public double logCompMPE() {
        makeValid();
        return ie.logPrMPE() - getLogZeroMiCorrection();
    }

    /**
     * This computes an approximation to log Pr(MPE), where:
     *  = MPE is computed in simplified network
     *  = the value log Pr(MPE) is computed in original network
     */
    public double logPrMPE() {
        makeValid();
        int[] mpe = this.mpe();
        return this.logPrMPE(edNet.oldTables(),mpe);
    }

    /**
     * This computes an approximation to log Pr(MPE), where:
     *  = MPE is given
     *  = the value log Pr(MPE) is computed in original network
     *
     * Use this if you call EDMPAlgorithm.mpe() yourself, as the
     * method EDMPAlgorithm.logPrMPE() also calls EDMPAlgorithm.mpe(),
     * which performs a non-trivial amount of work.
     */
    public double logPrMPE(int[] mpe) {
        if ( isInvalidWorld(mpe) )
            return Double.NEGATIVE_INFINITY;
        else
            return this.logPrMPE(edNet.oldTables(),mpe);
    }

    protected double logPrMPE(Table[] tables, int[] mpe) {
        double log_mpe = 0.0;
        for ( Table table : tables ) {
            IntSet vars = table.vars();
            int[] inst = new int[vars.size()];
            for (int i = 0; i < vars.size(); i++) {
                int var = vars.get(i);
                inst[i] = mpe[var];
            }
            int index = table.getIndexFromFullInstance(inst);
            log_mpe += Math.log( table.values()[index] );
        }
        return log_mpe;
    }

    /**
     * This computes an approximation to log Pr(MPE), where:
     *  = MPE is given
     *  = the value log Pr'(MPE) is computed in simplfied network
     *
     * This gives another approximation to the value of the MPE in the
     * original network.  However, we recommend using
     * EDMPAlgorithm.logCompMPE() instead.
     */
    public double logPrpMPE() {
        makeValid();
        int[] mpe = ie.mpe();
        return logPrMPE(edNet.newTables(),mpe);
    }

    public int[] mpe() {
        makeValid();
        Domain d = edNet.oldDomain();
        int[] ed_mpe = ie.mpe();
        int[] my_mpe = new int[d.size()];
        for (int var = 0; var < d.size(); var++) {
            int ed_var = edNet.oldToNewVar(var);
            my_mpe[var] = ed_mpe[ed_var];
        }
        return my_mpe;
    }

    /**
     * check MPE assignment against evidence
     */
    protected boolean isInvalidWorld(int[] mpe) {
        for (int i = 0; i < evidence.size(); i++) {
            int var = evidence.keys().get(i);
            int state = evidence.get(var);
            var = edNet.newToOldVar(var);
            if ( mpe[var] != state )
                return true;            
        }
        return false;
    }
}
