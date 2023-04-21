package edu.ucla.belief.inference;

import il2.inf.*;
import il2.bridge.Converter;
import edu.ucla.belief.Table;
import edu.ucla.belief.FiniteVariable;
import il2.util.IntMap;
import il2.inf.structure.JoinTreeStats;

import java.util.*;
//{superfluous} import java.math.BigInteger;

public abstract class JointWrapper{

    private boolean evidenceChanged=true;
    private Map evidence;

    JointWrapper(){
	evidence=new HashMap();
    }

    protected abstract JointEngine engine();
    protected abstract Converter converter();

    /** @since 20060222 */
    public JointEngine getJointEngine(){
    	return engine();
    }

    /**
    	@author Keith Cascio
    	@since 112503
    */
    public Converter getConverter()
    {
    	return converter();
    }

    public void removeEvidence(FiniteVariable var){
	evidence.remove(var);
	evidenceChanged=true;
    }

    public void addEvidence(FiniteVariable var,Object obj){
	evidence.put(var,obj);
	evidenceChanged=true;
    }

    protected void ensureCoherent(){
	if(evidenceChanged){
	    IntMap e=converter().convert(evidence);
	    engine().setEvidence(e);
	    evidenceChanged=false;
	}
    }

    public void setCPT(FiniteVariable var,double[] vals){
	int i=converter().convert(var);
	Table t=new Table(var.getCPTShell( var.getDSLNodeType() ).variables(),vals);
	engine().setTable(i,converter().convert(t));
    }

    public double prEvidence(){
	ensureCoherent();
	return engine().prEvidence();
    }

    public Table varJoint(FiniteVariable var){
	ensureCoherent();
	return converter().convert(engine().varJoint(converter().convert(var)));
    }

    public Table familyJoint(FiniteVariable var){
	ensureCoherent();
	return converter().convert(engine().tableJoint(converter().convert(var)),var.getCPTShell( var.getDSLNodeType() ).variables());
    }

    public JoinTreeStats.Stat getClusterStats(){
	return engine().getClusterStats();
    }

    public JoinTreeStats.Stat getSeparatorStats(){
	return engine().getSeparatorStats();
    }

    public double getCompilationTime(){
	return engine().getCompilationTime();
    }

    public double getPropagationTime(){
	return engine().getPropagationTime();
    }

    public double getMemoryRequirements(){
	return engine().getMemoryRequirements();
    }
}
