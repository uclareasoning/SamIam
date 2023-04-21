package edu.ucla.belief.inference;

import edu.ucla.belief.*;
import edu.ucla.belief.io.PropertySuperintendent;

import java.util.*;
//{superfluous} import javax.swing.JComponent;
//{superfluous} import javax.swing.JMenu;
//{superfluous} import java.awt.Container;
import java.io.Serializable;

/**
	@author Keith Cascio
	@since 011703
*/
public class JEngineGenerator extends DefaultGenerator implements Serializable
{
	static final long serialVersionUID = -6060830225489488864L;

	public String getDisplayName()
	{
		return FLAG_DEBUG_DISPLAY_NAMES ? "ss (edu.ucla.belief.inference)" : "shenoy-shafer";//"shenoy-shafer";
	}

	public Object getKey()
	{
		return "jenginegenerator6060830225489488864L";
	}

	protected InferenceEngine makeInferenceEngine( BeliefNetwork bn, JoinTreeSettings settings )
	{
		return makeJoinTreeInferenceEngineImpl( bn, settings );
	}

	/** @since 012904 */
	protected il2.inf.structure.JTUnifier makeJoinTree( BeliefNetwork bn, JoinTreeSettings settings )
	{
		List eliminationOrder = settings.getEliminationHeuristic().getEliminationOrder( bn );
		return edu.ucla.belief.tree.Trees.traditionalJoinTree( bn, eliminationOrder );
	}

	public InferenceEngine manufactureInferenceEngine( BeliefNetwork bn, DefaultGenerator dyn )
	{
		JoinTreeSettings settings = getSettings( choosePropertySuperintendent( (PropertySuperintendent) bn ) );
		JoinTreeInferenceEngineImpl engine = makeJoinTreeInferenceEngineImpl( bn, settings );
		engine.setDynamator( dyn );
		return engine;
	}

	/** @since 012904 */
	public JoinTreeInferenceEngineImpl makeJoinTreeInferenceEngineImpl( BeliefNetwork bn, JoinTreeSettings settings )
	{
		il2.inf.structure.JTUnifier jt = settings.getJoinTree();
		if( jt == null ){
			JoinTreeInferenceEngineImpl ret = createInferenceEngine( bn, settings.getEliminationHeuristic().getEliminationOrder( bn ), this, (QuantitativeDependencyHandler)null );
			edu.ucla.belief.tree.JoinTree newJoinTree = ret.underlyingCompilation().getJoinTree();
			settings.setJoinTree( newJoinTree );
			return ret;
		}
		else if( jt instanceof edu.ucla.belief.tree.JoinTree )
		{
			edu.ucla.belief.tree.JoinTree jointree = (edu.ucla.belief.tree.JoinTree)jt;
			if( jointree.getBeliefNetwork() != bn )
			{
				throw new IllegalStateException();
				//jointree = jointree.clone( bn );
			}
			return createInferenceEngine( bn, this, jointree, (QuantitativeDependencyHandler)null );
		}
		else throw new IllegalArgumentException( STR_EXCEPTION_ILLEGAL_JOINTREE );
	}

	public InferenceEngine prunedEngine(BeliefNetwork bn,Collection queryVariables, Map evidence){
		throw new UnsupportedOperationException();
	}

	public static BeliefCompilation compile( BeliefNetwork bn, int reps, Random seed )
	{
		List order = EliminationOrders.minFill( bn, reps, seed );
		return compile( bn, order, (QuantitativeDependencyHandler)null );
	}

	public static BeliefCompilation compile( BeliefNetwork bn, List order )
	{
		return compile( bn, order, (QuantitativeDependencyHandler)null );
	}

	public static BeliefCompilation compile( BeliefNetwork bn, List order, QuantitativeDependencyHandler handler )
	{
		//this really should not be done, but the gui needs to be able to
		// handle empty networks and it apparently always compiles.
		if(order.size()==0)
		{
			return new BeliefCompilation( bn,null,new TableIndex[0],new HashMap(),new HashMap());
		}
		else return S4Compiler.compile( bn, order, handler );
	}

	/** @since 012904 */
	public static BeliefCompilation compile( BeliefNetwork bn, edu.ucla.belief.tree.JoinTree jt, QuantitativeDependencyHandler handler )
	{
		// this really should not be done, but the gui needs to be able to
		// handle empty networks and it apparently always compiles.
		if( bn.isEmpty() ) return new BeliefCompilation( bn,null,new TableIndex[0],new HashMap(),new HashMap());
		else return S4Compiler.compile( bn, jt, handler );
	}

	/*public static JoinTreeInferenceEngineImpl createInferenceEngine( BeliefNetwork bn, Dynamator dyn ) throws OutOfMemoryError
	{
		JoinTreeInferenceEngineImpl ie;
		double compilationTime=Double.NaN;
		long start=System.currentTimeMillis();
		ie = new JoinTreeInferenceEngineImpl( compile(bn), dyn );
		compilationTime=(System.currentTimeMillis()-start)/DOUBLE_MILLIS_PER_SECOND;
		ie.setCompilationTime(compilationTime);
		return ie;
	}*/

	public static JoinTreeInferenceEngineImpl createInferenceEngine( BeliefNetwork bn, List eo, Dynamator dyn, QuantitativeDependencyHandler handler ) throws OutOfMemoryError
	{
		JoinTreeInferenceEngineImpl ie;
		double compilationTime=Double.NaN;
		long start=System.currentTimeMillis();
		ie = new JoinTreeInferenceEngineImpl( compile(bn,eo,handler), dyn );
		compilationTime=(System.currentTimeMillis()-start)/DOUBLE_MILLIS_PER_SECOND;
		ie.setCompilationTime(compilationTime);
		ie.setQuantitativeDependencyHandler( handler );
		return ie;
	}

	/** @since 012904 */
	public static JoinTreeInferenceEngineImpl createInferenceEngine( BeliefNetwork bn, Dynamator dyn, edu.ucla.belief.tree.JoinTree jt, QuantitativeDependencyHandler handler ) throws OutOfMemoryError
	{
		JoinTreeInferenceEngineImpl ie;
		double compilationTime=Double.NaN;
		long start=System.currentTimeMillis();
		ie = new JoinTreeInferenceEngineImpl( compile(bn,jt,handler), dyn );
		compilationTime=(System.currentTimeMillis()-start)/DOUBLE_MILLIS_PER_SECOND;
		ie.setCompilationTime(compilationTime);
		ie.setQuantitativeDependencyHandler( handler );
		return ie;
	}

	/** @since 052004 */
	public Collection getClassDependencies(){
		return Arrays.asList( new Class[] {
			JEngineGenerator.class,
			JoinTreeSettings.class,
			JoinTreeInferenceEngineImpl.class,
			edu.ucla.belief.tree.JoinTree.class,
			il2.inf.structure.JTUnifier.class } );
	}

	public static final double DOUBLE_MILLIS_PER_SECOND = (double)1000;
}
