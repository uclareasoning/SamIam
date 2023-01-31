package il2.inf.jointree;

import il2.bridge.*;
import il2.util.*;
import il2.model.*;
import il2.inf.structure.*;
//{superfluous} import java.util.Arrays;

/**
 * this class should be checked (AC):
 * = double check sendMessage
 * = define the semantics of partial and joint methods (only their relative values are correct)
 * = there is a faster way to do log Pr(e) by accumulating the normalizing constants during message passing
 *
 */
public class NormalizedZCAlgorithm extends UnindexedZCAlgorithm{

    public static NormalizedZCAlgorithm create( Table[] tables, EliminationOrders.JT jt )
    {
	long start=System.currentTimeMillis();
	NormalizedZCAlgorithm result=new NormalizedZCAlgorithm(jt,tables);
	long finish=System.currentTimeMillis();
	result.compilationTime=(finish-start)/(1000.0);
	return result;
    }

    public static NormalizedZCAlgorithm create( BayesianNetwork bn, EliminationOrders.JT jt ){
    	return create( bn.cpts(), jt );
    }

    public NormalizedZCAlgorithm(EliminationOrders.JT jointree,Table[] tables){
		super(jointree,tables);
    }

	/**
	 * for some reason here, I didn't need scratch2
	 */
    protected void sendMessage(int mind,boolean inward){
	Table sepTable=separatorTables[mind];
	Table fromTable;
	Table toTable;
	if(inward){
	    int from=messageOrder[mind].s1;
	    int to=messageOrder[mind].s2;
	    fromTable=clusterTables[from];
	    toTable=clusterTables[to];
	    sepTable.zeroConciousRealProjectInto(fromTable,clusterZeroFree[from]);
		//sepTable.normalizeInPlace();
		double sum = sepTable.sum();
		if ( sum > 0.0 ) sepTable.multiplyByConstant(1.0/sum);
	    toTable.zeroConciousMultiplyInto(sepTable,clusterZeroFree[to]);

	}else{
	    int from=messageOrder[mind].s2;
	    int to=messageOrder[mind].s1;
	    fromTable=clusterTables[from];
	    toTable=clusterTables[to];
	    double[] sv=sepTable.values();
	    System.arraycopy(sv,0,scratch1,0,sv.length);
	    sepTable.zeroConciousProjectInto(fromTable,clusterZeroFree[from],zeroFreeScratch);
	    //System.arraycopy(sv,0,scratch2,0,sv.length);
	    sepTable.zeroConciousDivideRelevantInto(scratch1,zeroFreeScratch);
		//sepTable.normalizeInPlace();
		double sum = sepTable.sum();
		if ( sum > 0.0 ) sepTable.multiplyByConstant(1.0/sum);
	    toTable.zeroConciousMultiplyInto(sepTable,clusterZeroFree[to]);
	    //System.arraycopy(scratch2,0,sv,0,sv.length);
	    //sepTable.zeroConciousMakeReal(zeroFreeScratch);
		for (int i=0;i<sv.length;i++) sv[i] *= scratch1[i];
	}

    }

    public double logPrEvidence(){
	makeValid();
	return computeLogPrE();
    }

	/**
	 * log Z = sum_i \log Z_i + sum_ij \log Z_ij
	 * where 
	 *     Z_i is the normalizing constant for the cluster table, and 
	 *     Z_ij is the normalizing constant for the separator table
	 *
	 * there is a faster way to do this by accumulating normalizing
	 * constants during message passing.
	 */
    protected double computeLogPrE(){
		double logPrE = 0.0, sum;
		double[] values;
		boolean[] zf;
		for (int i=0; i<clusterTables.length; i++) {
			values = clusterTables[i].values();
			zf = clusterZeroFree[i];
			sum = 0.0;
			for( int k=0; k<values.length; k++ )
				if( zf[k] ) sum += values[k];
			logPrE += Math.log( sum );
		}
		if (Double.isInfinite(logPrE) && logPrE < 0.0) return logPrE;
		for (int i=0; i<separatorTables.length; i++) {
			logPrE -= Math.log( separatorTables[i].sum() );
		}
		return logPrE;
    }

    protected double computePrE(){
		return Math.exp(computeLogPrE());
	}
}
