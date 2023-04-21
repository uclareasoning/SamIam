package edu.ucla.belief.decision;

import edu.ucla.belief.FiniteVariable;

import java.util.Collection;

/** @author Keith Cascio
	@since 120804 */
public interface Factory
{
	public DecisionNode clone( DecisionNode node );
	public Parameter clone( Parameter parameter );
	public Parameter newParameter( double value );
	public Parameter newParameter( String id, double value );
	public Parameter parameterForID( String id );
	public DecisionLeaf leafForID( String id );
	public DecisionInternal internalForID( String id );
	public DecisionNode nodeForID( String id );
	public DecisionLeaf getDefault();
	public DecisionLeaf newLeaf( FiniteVariable var );
	public DecisionLeaf newLeaf( FiniteVariable var, Parameter[] params );
	public DecisionInternal newInternal( FiniteVariable var );
	public Collection getLeafHistory();
	public Collection getInternalHistory();
	public Collection getParameterHistory();
	public boolean removeHistory( DecisionLeaf leaf );
	public void adopt( DecisionNode node );
	public boolean isUniqueNodeID( String id );
	public boolean isUniqueParameterID( String id );
	public boolean isValidID( String id );
}
