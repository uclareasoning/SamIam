package edu.ucla.belief.ui.event;

/** The listener interface for CPT changes sent by
  * NetworkInternalFrame. */
public interface CPTChangeListener {
	/** Invoked when there is a CPT change. */
	public void cptChanged( CPTChangeEvent evt );
}
