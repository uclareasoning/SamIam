package edu.ucla.util;

/** @author keith cascio
	@since  20030925 */
public interface ChangeBroadcaster{
	public boolean                    addChangeListener( ChangeListener listener );
	public boolean                 removeChangeListener( ChangeListener listener );
	public       ChangeBroadcaster   fireSettingChanged();
}
