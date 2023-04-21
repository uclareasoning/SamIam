package edu.ucla.belief;

/**
	<p>
	The listener interface for evidence changes sent by
	EvidenceController.
	<p>
	Removed from samiam package edu.ucla.belief.ui.event;

	@author Keith Cascio
	@since 071802
*/
public interface EvidenceChangeListener {
	/** Invoked when there is an evidence change. */
	public void evidenceChanged( EvidenceChangeEvent ECE );

	/**
		warning() means: a real evidence change
		is about to happen - get ready!

		@author Keith Cascio
		@since 071003
	*/
	public void warning( EvidenceChangeEvent ECE );

	/**	@author Keith Cascio
		@since 090704
		Via this method, an EvidenceChangeListener tells
		EvidenceController 'I am no longer valid'.
	*/
	//public boolean isDead();
}
