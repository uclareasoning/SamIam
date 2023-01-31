package edu.ucla.belief.decision;

/** @author Keith Cascio
	@since 121304 */
public class DecisionEvent
{
	public DecisionEvent( DecisionNode node, Type type ){
		this.node = node;
		this.type = type;
	}

	public DecisionEvent( DecisionTree tree, DecisionNode node, Type type ){
		this.tree = tree;
		this.node = node;
		this.type = type;
	}

	public DecisionEvent( DecisionNode node, DecisionNode parent ){
		this.node = node;
		this.type = PARENT_INSERTED;
		this.parent = parent;
	}

	public DecisionTree tree;
	public DecisionNode node;
	public DecisionNode parent;
	public Type type;

	public String toString(){
		return "DecisionEvent/" + type.toString() + "/ /" + node.toString() + "/";
	}

	public static class Type{
		public Type( String name ){
			this.myName = name;
		}

		public String toString(){
			return getName();
		}

		public String getName(){
			return this.myName;
		}

		private String myName;
	}

	public static final Type ASSIGNMENT_CHANGE = new Type( "assignment change" );
	public static final Type PARENT_INSERTED = new Type( "parent inserted" );
	public static final Type DERACINATED = new Type( "deracinated (uprooted)" );
	public static final Type ASSIGNMENTS_DELETED = new Type( "assignments deleted" );
	public static final Type RENAME = new Type( "rename" );
}
