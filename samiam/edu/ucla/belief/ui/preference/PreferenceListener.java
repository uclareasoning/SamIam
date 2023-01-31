package edu.ucla.belief.ui.preference;

/**
	The interface to Objects that can respond to preference changes.

	@author keith cascio
	@since 20020712
*/
public interface PreferenceListener
{
	/**
		Call this method to ask a PreferenceListener to
		respond to RECENT changes in COMMITTED values
	*/
	public void updatePreferences();
	/**
		Call this method to ask a PreferenceListener to
		respond to RECENT changes in EDITED values
	*/
	public void previewPreferences();
	/**
		Call this method to force a PreferenceListener to
		reset itself
	*/
	public void setPreferences();
}
