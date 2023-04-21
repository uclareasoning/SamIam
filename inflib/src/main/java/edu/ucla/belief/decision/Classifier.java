package edu.ucla.belief.decision;

/** @author Keith Cascio
	@since 121104 */
public class Classifier
{
	private Classifier() {}

	public static final Classifier ALL = new Classifier(){
		public boolean isMember( DecisionNode node ) { return true; }
	};

	public static final Classifier LEAVES = new Classifier(){
		public boolean isMember( DecisionNode node ) { return node.isLeaf(); }
	};

	public static final Classifier INTERNALS = new Classifier(){
		public boolean isMember( DecisionNode node ) { return !node.isLeaf(); }
	};

	public boolean isMember( DecisionNode node ) { return false; }
}
