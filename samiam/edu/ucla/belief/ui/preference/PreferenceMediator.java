package edu.ucla.belief.ui.preference;

/**
	Responds to preference changes.

	@author Keith Cascio
	@since 080404
*/
public interface PreferenceMediator
{
	public void setPreferences( SamiamPreferences prefs );
	public void updatePreferences( SamiamPreferences prefs );
	public void massagePreferences( SamiamPreferences prefs );
}
