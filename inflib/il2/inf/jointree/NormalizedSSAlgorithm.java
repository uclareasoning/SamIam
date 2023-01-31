package il2.inf.jointree;

import il2.bridge.*;
import il2.util.*;
import il2.model.*;
import il2.inf.structure.*;
import java.util.*;
import il2.model.BayesianNetwork;

/**
 * this class should be checked (AC):
 * = double check sendMessage
 * = define the semantics of partial and joint methods (only their relative values are correct)
 * = there implements a faster way to do log Pr(e) by accumulating the normalizing constants during message passing
 *
 */
public class NormalizedSSAlgorithm extends UnindexedSSAlgorithm {

    public static NormalizedSSAlgorithm create( Table[] tables, EliminationOrders.JT jt )
    {
	long start=System.currentTimeMillis();
	NormalizedSSAlgorithm result=new NormalizedSSAlgorithm(jt,tables);
	long finish=System.currentTimeMillis();
	result.compilationTime=(finish-start)/(1000.0);
	return result;
    }

    public static NormalizedSSAlgorithm create( BayesianNetwork bn, EliminationOrders.JT jt ){
    	return create( bn.cpts(), jt );
    }

    public NormalizedSSAlgorithm(EliminationOrders.JT jointree,Table[] tables){
		super(jointree,tables);
    }

    protected void initialize() {
        this.logPrE = 0.0;
        super.initialize();
    }

    double logPrE;
    protected void sendMessage(int ind,boolean isInward){
	if(isInward){
	    sendMessage(messageOrder[ind].s1,messages[0][ind],messages[1][ind],isInward);
	}else{
	    sendMessage(messageOrder[ind].s2,messages[1][ind],messages[0][ind],isInward);
	}
    }

    protected void sendMessage(int cluster,Table dest,Table excluded,boolean isInward){
	Table[] relevantTables=remove(getAllTables(cluster),excluded);
	dest.multiplyAndProjectInto(relevantTables);
	//dest.normalizeInPlace();
	double sum = dest.sum();
	if ( sum > 0.0 ) dest.multiplyByConstant(1.0/sum);
	if (isInward) { this.logPrE += Math.log(sum); }
    }

	/**
	 * log Pr(e) = sum_i log Z_i - sum_ij log Z_ij
	 * where Z_i is normalizing constant for cluster and
	 * where Z_ij is normalizing constant for separator
	 *
	 * this is the old way and is slower
	 */
	/*
    protected double computeLogPrE(){
		double logPrE = 0.0;
		Table dest=Table.constantTable(domain,1);
		for (int i=0; i<clusters.length; i++) {
			dest.multiplyAndProjectInto(getAllTables(i));
			logPrE += Math.log( dest.values()[0] );
		}
		Table[] messagePair = new Table[2];
		for (int i=0; i<messages[0].length; i++) {
			messagePair[0] = messages[0][i];
			messagePair[1] = messages[1][i];
			dest.multiplyAndProjectInto(messagePair);
			logPrE -= Math.log( dest.values()[0] );
		}
		return logPrE;
    }
	*/

    protected double computePrE(){
        return Math.exp(computeLogPrE());
    }

    public double logPrEvidence(){
	makeValid();
	return computeLogPrE();
    }

    /**
     * y_ij = Z_i/Z_ij
     * where y_ij is normalizing constant for message from i to j
     * therefore:
     *   log Pr(e) = ( sum_ij log y_ij ) + log Z_0
     * where y_ij are for the inward messages and Z_0 is for the root
     */
    protected double computeLogPrE(){
        int c0;
        if ( separators.size() == 0 ) c0 = smallestCluster;
        else c0 = messageOrder[separators.size()-1].s2;
        Table dest=Table.constantTable(domain,1);
        dest.multiplyAndProjectInto(getAllTables(c0));
        return this.logPrE + Math.log( dest.values()[0] );
    }
}
