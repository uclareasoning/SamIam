package edu.ucla.belief.inference;
import il2.inf.*;
import java.util.*;
import edu.ucla.belief.Table;
import edu.ucla.belief.FiniteVariable;

public abstract class PartialDerivativeWrapper extends JointWrapper{


    PartialDerivativeWrapper(){
    }
    protected abstract PartialDerivativeEngine pdengine();

    public Table varPartial(FiniteVariable var){
	ensureCoherent();
	return converter().convert(pdengine().varPartial(converter().convert(var)));
    }

    public Table tablePartial(FiniteVariable var){
	ensureCoherent();
	return converter().convert(pdengine().tablePartial(converter().convert(var)),var.getCPTShell( var.getDSLNodeType() ).variables());
    }
}
