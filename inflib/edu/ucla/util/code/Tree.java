package edu.ucla.util.code;

/**
	@author Keith Cascio
	@since 052404
*/
public interface Tree
{
	public void addChildOfRootNode( Object value );
	public void addChildOfLastNode( Object value );
	public void lastNodeGetsParentLastNode();//better method name would be stepUp()
}
