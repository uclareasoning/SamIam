package edu.ucla.belief.inference;

import il2.inf.structure.EliminationOrders;
import edu.ucla.belief.*;
import edu.ucla.belief.io.PropertySuperintendent;

//{superfluous} import javax.swing.JComponent;
//{superfluous} import javax.swing.JMenu;
//{superfluous} import java.awt.Container;
import java.io.Serializable;
import java.util.*;

/** @author keith cascio
    @since  20030624 */
public class SSEngineGenerator extends DefaultGenerator implements Serializable
{
	static final long serialVersionUID = 2437417063154807649L;

	public String getDisplayName()
	{
		return FLAG_DEBUG_DISPLAY_NAMES ? "ss (il2.inf)" : "shenoy-shafer";//"shenoy-shafer (exp)";
	}

	public Object getKey()
	{
		return "ssenginegenerator2437417063154807649L";
	}

	public InferenceEngine manufactureInferenceEngine( BeliefNetwork bn, DefaultGenerator dyn )
	{
		JoinTreeSettings settings = getSettings( choosePropertySuperintendent( (PropertySuperintendent) bn ) );
		SSEngine ret = makeSSEngine( bn, settings );
		ret.setDynamator( dyn );
		return ret;
	}

	protected InferenceEngine makeInferenceEngine( BeliefNetwork bn, JoinTreeSettings settings )
	{
		return makeSSEngine( bn, settings );
	}

	/** @since 20040129 */
	private SSEngine makeSSEngine( BeliefNetwork bn, JoinTreeSettings settings )
	{
		SSEngine engine = null;
		il2.inf.structure.JTUnifier jt = settings.getJoinTree();
		if( jt == null ){
			engine = new SSEngine( bn, this, settings.getEliminationHeuristic().getEliminationOrder( bn ) );
			EliminationOrders.JT newJoinTree = ((il2.inf.jointree.JoinTreeAlgorithm)engine.getJointWrapper().engine()).getJoinTree();
			settings.setJoinTree( newJoinTree );
		}
		else if( jt instanceof EliminationOrders.JT )
		{
			EliminationOrders.JT jointree = (EliminationOrders.JT)jt;
			if( jointree.converter != null && jointree.converter.getBeliefNetwork() != bn )
			{
				throw new IllegalStateException();
				//il2.bridge.Converter c = new il2.bridge.Converter();
				//il2.model.BayesianNetwork bn2 = c.convert( bn );
				//jointree = jointree.clone( c, bn2 );
			}
			engine = new SSEngine( bn, this, jointree );
		}
		else throw new IllegalArgumentException( STR_EXCEPTION_ILLEGAL_JOINTREE );
		return engine;
	}

	/** @since 20040129 */
	protected il2.inf.structure.JTUnifier makeJoinTree( BeliefNetwork bn, JoinTreeSettings settings )
	{
		il2.bridge.Converter c = new il2.bridge.Converter();
		il2.model.BayesianNetwork bn2 = c.convert( bn );
		List eliminationOrder = settings.getEliminationHeuristic().getEliminationOrder( bn );
		il2.util.IntList order = c.convert( eliminationOrder );
		return EliminationOrders.traditionalJoinTree( bn2, c, order );
	}

	public InferenceEngine prunedEngine(BeliefNetwork bn, Collection queryVariables, Map evidence){
		return new SSEngine(bn,queryVariables,evidence,this);
	}

	/** @since 20040520 */
	public Collection getClassDependencies(){
		return DEPENDENCIES;
	}

	static public final List DEPENDENCIES = Collections.unmodifiableList( Arrays.asList( new Class[]{
	  SSEngineGenerator                       .class,
	  JoinTreeSettings                        .class,
	  SSEngine                                .class,
	  il2.inf.jointree  .UnindexedSSAlgorithm .class,
	  il2.inf.structure .EliminationOrders    .class,
	  il2.inf.structure .JTUnifier            .class,
	  il2.inf.structure .JoinTreeStats        .class } ) );
}
