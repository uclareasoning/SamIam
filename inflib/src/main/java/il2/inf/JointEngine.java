package il2.inf;

import il2.util.*;
import il2.model.*;
import il2.inf.structure.JoinTreeStats;

//{superfluous} import java.math.BigInteger;

public interface JointEngine{
    public void setEvidence(IntMap evidence);
    public void setTable(int t,Table table);
    public double prEvidence();
    public double logPrEvidence();
    public Table tableJoint(int table);
    public Table tableConditional(int table);
    public Table varJoint(int var);
    public Table varConditional(int var);
    public JoinTreeStats.Stat getClusterStats();
    public JoinTreeStats.Stat getSeparatorStats();

    public double getCompilationTime();
    public double getPropagationTime();
    public double getMemoryRequirements();

}
