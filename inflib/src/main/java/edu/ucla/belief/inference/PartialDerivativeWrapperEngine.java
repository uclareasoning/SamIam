package edu.ucla.belief.inference;
import edu.ucla.belief.*;

public abstract class PartialDerivativeWrapperEngine extends WrapperInferenceEngine implements PartialDerivativeEngine{
    protected PartialDerivativeWrapper pdw;

    PartialDerivativeWrapperEngine(PartialDerivativeWrapper wrapper,BeliefNetwork bn,Dynamator dyn){
	super(wrapper,bn,dyn);
	pdw=wrapper;
    }

    public Table partial(FiniteVariable var){
	return pdw.varPartial(var);
    }

    public Table familyPartial(FiniteVariable var){
	return pdw.tablePartial(var);
    }
}
