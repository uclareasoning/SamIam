package edu.ucla.belief.ui.event;

import edu.ucla.belief.Dynamator;
import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.io.PropertySuperintendent;

/** @author keith cascio
	@since  20091119 */
public interface DynamatorListener extends java.beans.VetoableChangeListener{
	/** http://en.wikipedia.org/wiki/Dibs */
	public Dynamator dibs( BeliefNetwork bn, PropertySuperintendent ps );
}
