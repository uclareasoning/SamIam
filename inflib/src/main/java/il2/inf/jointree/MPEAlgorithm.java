package il2.inf.jointree;

import il2.bridge.*;
import il2.util.*;
import il2.model.*;
import il2.inf.structure.*;
import java.util.*;
import il2.model.BayesianNetwork;
import edu.ucla.util.*;

/** @author hei chan
	@since 20060119 */
public class MPEAlgorithm extends UnindexedSSAlgorithm
{
    private MPE myMPE;

    public static MPEAlgorithm createMPEAlgorithm( BayesianNetwork bn, EliminationOrders.JT jt ){
        Table[] cpts = bn.cpts();
        MPETable[] mpeTables = new MPETable[cpts.length];
        for (int i = 0; i < cpts.length; i++)
        {
            MPE[] mpeInsts = new MPE[cpts[i].values().length];
            int[] current = new int[cpts[i].vars().size()];
            for (int j = 0; j < mpeInsts.length; j++)
            {
                mpeInsts[j] = new MPE(cpts[i].vars(), (int[])current.clone());
                cpts[i].next(current);
            }
            mpeTables[i] = new MPETable(cpts[i], cpts[i].values(), mpeInsts);
        }
        return new MPEAlgorithm(jt, mpeTables);
    }


    protected MPEAlgorithm(EliminationOrders.JT jointree, MPETable[] mpeTables)
    {
        super(jointree, mpeTables);
        allocateTables();
        findIncomingMessages();
    }

    protected void allocateTables()
    {
        messages=new Table[2][messageOrder.length];
        for(int i=0;i<messageOrder.length;i++)
        {
            IntSet vars=(IntSet)separators.get(new UPair(messageOrder[i]));
            messages[0][i]=new MPETable(domain,vars);
            messages[1][i]=new MPETable(domain,vars);
        }
    }

    /** @author keith cascio
    	@since 20060123 */
    public MPE getMPE(){
    	return this.myMPE;
    }

    public double[][][] flipPoints() {
        makeValid();
        double prMPE = computePrE();
        MPE mpe = computeMPE();

      //System.out.println("Pr(MPE) = " + prMPE);
      //System.out.println("MPE = " + mpe);

        double[][][] flipPoints = new double[3][originalTables.length][];
        for (int i = 0; i < originalTables.length; i++)
        {
            flipPoints[1][i] = tablePartial(i).values();
        }
        for (int i = 0; i < originalTables.length; i++)
        {
            MPETable cpt = (MPETable)originalTables[i];
            double[] vals = cpt.values();
            MPE[] mpeInsts = cpt.mpeInstantiations();
            int[] current = new int[cpt.vars().size()];
            boolean[] signs = new boolean[vals.length];

            flipPoints[2][i] = new double[vals.length];
            flipPoints[0][i] = new double[vals.length];
            for (int j = 0; j < vals.length; j++)
            {
		double constant;
                signs[j] = mpe.contains(cpt.vars(), current);
                if (signs[j])
                {
                    double origVal = vals[j];
                    vals[j] = 0;
                    MPETable newCPT = new MPETable(cpt, vals, mpeInsts);
                    setTable(i, newCPT);
                    makeValid();
                    flipPoints[2][i][j] = computePrE();
                    flipPoints[0][i][j] = flipPoints[2][i][j] / flipPoints[1][i][j];
                    setTable(i, cpt);
                    vals[j] = origVal;
                }
		else
                {
                    flipPoints[2][i][j] = prMPE;
                    flipPoints[0][i][j] = flipPoints[2][i][j] / flipPoints[1][i][j];
                }
                cpt.next(current);
            }

          //System.out.println("Variable " + i + ":");
          //System.out.println("CPT = " + DblArrays.convertToString(vals));
          //System.out.println("Ratios = " + DblArrays.convertToString(flipPoints[1][i]));
          //System.out.println("Consts = " + DblArrays.convertToString(flipPoints[2][i]));
          //System.out.println("Points = " + DblArrays.convertToString(flipPoints[0][i]));
        }
        makeValid();
        this.myMPE = mpe;

        return flipPoints;
    }

    protected void initialize() {
        setUpEvidence();
    }

    protected double computePrE() {
	makeValid();
        MPETable dest = MPETable.constantMPETable(domain, 0);
        dest.multiplyAndProjectInto(getAllTables(smallestCluster));
        return dest.values()[0];
    }

    protected MPE computeMPE() {
	makeValid();
        MPETable dest = MPETable.constantMPETable(domain, 0);
        dest.multiplyAndProjectInto(getAllTables(smallestCluster));
        return dest.mpeInstantiations()[0];
    }

    public Table tablePartial(int table) {
	makeValid();
        int cluster=tableClusterAssignments[table];
        MPETable dest=MPETable.createCompatibleMPE(originalTables[table]);
        dest.multiplyAndProjectInto(remove(getAllTables(cluster),originalTables[table]));
        return dest;
    }

    protected void setUpEvidence(){
        evidenceIndicators=new Table[clusters.length][];
        IntSet eKeys=evidence.keys();
        for(int i=0;i<assignedEvidence.length;i++){
            IntSet is=eKeys.intersection(assignedEvidence[i]);
            Table[] t=new Table[is.size()];
            for(int j=0;j<is.size();j++){
                int var=is.get(j);
                t[j]=MPETable.evidenceMPETable(domain,var,evidence.get(var));
            }
            evidenceIndicators[i]=t;
        }
    }
}
