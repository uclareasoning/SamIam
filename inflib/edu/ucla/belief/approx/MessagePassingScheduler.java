package edu.ucla.belief.approx;

import edu.ucla.structure.DirectedEdge;
import edu.ucla.structure.DirectedGraph;
import java.util.*;


/**
	@author Arthur Choi
	@since 050505 */
public abstract class MessagePassingScheduler {
	abstract public List generateSchedule(DirectedGraph graph);
	abstract public boolean isParallel();

	/** @since 20100108 */
	public static String asJavaCode( MessagePassingScheduler scheduler ) throws Exception{
		java.lang.reflect.Field[] fields = MessagePassingScheduler.class.getFields();
		for( int i=0; i<fields.length; i++ ){
			if( ((fields[i].getModifiers() & java.lang.reflect.Modifier.STATIC) > 0) && (fields[i].get(null) == scheduler) ){ return MessagePassingScheduler.class.getName() + "." + fields[i].getName(); }
		}
		return null;
	}

	public static MessagePassingScheduler getDefault() {
		// return PARALLEL;
		return TOPDOWNBOTTUMUP;
	}

	private static DirectedEdge newEdge(Object from, Object to) {
		return new DirectedEdge(from,to);
	}

	public static final MessagePassingScheduler PARALLEL =
		new MessagePassingScheduler() {
			/*
			 * The order of edges do not matter in a parallel message
			 * passing schedule, so this is simply a list of all
			 * edges.
			 */
			public List generateSchedule(DirectedGraph graph) {
				// schedule of all edges
				List schedule = new Vector(2*graph.numEdges());
				List vars = graph.topologicalOrder();

				for ( Iterator it = vars.iterator(); it.hasNext(); ) {
					Object parent = it.next();
					for ( Iterator edgeIt = graph.outGoing(parent).iterator();
						edgeIt.hasNext(); ) {
						Object child = edgeIt.next();
						schedule.add(newEdge(parent,child));
						schedule.add(newEdge(child,parent));
					}
				}
				return schedule;
			}

			public boolean isParallel() { return true; }
			public String toString() { return "Parallel"; }
		};

	public static final MessagePassingScheduler TOPDOWNBOTTUMUP =
		new MessagePassingScheduler() {
			/*
			 * This is a top-down, bottom-up based sequential
			 * schedule.  Messages are passed from roots to leaves,
			 * and leaves back up to roots.  This message passing
			 * schedule converges immediately for networks without
			 * evidence.
			 */
			public List generateSchedule(DirectedGraph graph) {
				// schedule of all edges
				List schedule = new Vector(2*graph.numEdges());
				List scheduleUp = new Vector(graph.numEdges());
				List vars = graph.topologicalOrder();

				for ( Iterator it = vars.iterator(); it.hasNext(); ) {
					Object parent = it.next();
					for ( Iterator edgeIt = graph.outGoing(parent).iterator();
						edgeIt.hasNext(); ) {
						Object child = edgeIt.next();
						schedule.add(newEdge(parent,child));
						scheduleUp.add(newEdge(child,parent));
					}
				}
				/* the following should be equivalent to traversing
				 * variables in reverse topological order, and adding
				 * incoming edges, in the sense that a node does not
				 * pass messages to the parent until it receives all
				 * messages from children */
				Collections.reverse(scheduleUp);
				schedule.addAll(scheduleUp);
				return schedule;
			}

			public boolean isParallel() { return false; }
			public String toString() { return "Top-Down, Bottom-Up"; }
		};

	/*
	private void generateEmbeddedTreesMessageSchedule() {
		RandomSpanningTrees rst = new RandomSpanningTrees(myBeliefNetwork);
		messageSchedule = rst.getEmbeddedTreesMessageOrder();
	}
	*/

	public static final MessagePassingScheduler[] ARRAY =
		new MessagePassingScheduler[]{ TOPDOWNBOTTUMUP, PARALLEL };

}
