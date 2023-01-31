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
public class ZCEngineGenerator extends DefaultGenerator implements Serializable
{
	static final long serialVersionUID = -6314619632174047614L;

	public String getDisplayName()
	{
		return "zc-hugin";
	}

	public Object getKey()
	{
		return "zcenginegenerator6314619632174047614L";
	}

	public InferenceEngine manufactureInferenceEngine( BeliefNetwork bn, DefaultGenerator dyn )
	{
		JoinTreeSettings settings = getSettings( choosePropertySuperintendent( (PropertySuperintendent) bn ) );
		ZCEngine ret = makeZCEngine( bn, settings );
		ret.setDynamator( dyn );
		return ret;
	}

	protected InferenceEngine makeInferenceEngine( BeliefNetwork bn, JoinTreeSettings settings )
	{
		return makeZCEngine( bn, settings );
	}

	/** @since 20040129 */
	private ZCEngine makeZCEngine( BeliefNetwork bn, JoinTreeSettings settings )
	{
		ZCEngine engine = null;
		il2.inf.structure.JTUnifier jt = settings.getJoinTree();
		if( jt == null ){
			engine = new ZCEngine( bn, this, settings.getEliminationHeuristic().getEliminationOrder( bn ) );
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
			engine = new ZCEngine( bn, this, jointree );
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
		return new ZCEngine(bn,queryVariables,evidence,this);
	}

	/** @since 20040520 */
	public Collection getClassDependencies(){
		return DEPENDENCIES;
	}

	static public final List DEPENDENCIES = Collections.unmodifiableList( Arrays.asList( new Class[]{
	  ZCEngineGenerator                       .class,
	  JoinTreeSettings                        .class,
	  ZCEngine                                .class,
	  il2.inf.jointree  .UnindexedZCAlgorithm .class,
	  il2.inf.structure .EliminationOrders    .class,
	  il2.inf.structure .JTUnifier            .class,
	  il2.inf.structure .JoinTreeStats        .class } ) );
}
