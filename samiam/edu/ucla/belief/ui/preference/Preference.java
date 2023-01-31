package edu.ucla.belief.ui.preference;

import edu.ucla.belief.ui.util.ElementHandler;

import javax.swing.JComponent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/** This interface represents a single user-preferenceable object.
	@author keith cascio
	@since  20020712 */
public interface Preference
{
	/** @since 20100112 */
	public Preference describe( String description );
	/** @since 20100112 */
	public String getDescription();

	/**
		Get the name to display when we identify this Preference to the user.
	*/
	public String getDisplayName();
	/** @since 20070403
		@return true if we should display the display name as a caption when editing */
	public boolean displayCaption();

	/** if we require a special ElementHandler
		@since 20070404 */
	public ElementHandler getElementHandler();
	/**
		Get a JComponent that can be used to edit this Preference.
		(e.g. a JCheckBox for a BooleanPreference)
	*/
	public JComponent getEditComponent();
	/**
		<p>
		Register an ActionListener Object that will receive
		ActionEvents when the user edits the edit component.
		<p>
		Useful, for example, for updating a preview GUI when
		the user edits values.
	*/
	public void addActionListener( ActionListener AL );
	/**
		Get the value represented by the current
		state of the edit component returned by
		getEditComponent().
	*/
	public Object getCurrentEditedValue();
	/**
		Accessor.
	*/
	public Object getValue();

	/** @since 20060718 */
	public Object getDefault();

	/** revert back to default value
		@since 20060718 */
	public Object revert();

	/**
		Mutator
	*/
	public void setValue( Object newVal );
	/**
		Get's the key that uniquely identifies this preference
		within it's PreferenceGroup, and used to identify it
		when writing to a file.
	*/
	public Object getKey();
	/**
		Call this method to find out whether the user has
		done something to the edit component returned by
		getEditComponent() that constitutes an 'edit'.
		( e.g. clicking a JCheckBox )
	*/
	public boolean isComponentEdited();
	/**
		Call this method to ask a Preference to set
		it's value to the value represented by the current
		state of the edit component returned by getEditComponent().
	*/
	public void commitValue();
	/**
		Ask a Preference to return an XML string
		representation of itself.
	*/
	public StringBuffer appendXML( StringBuffer buff );
	/**
		<p>
		Ask a Preference whether commitValue() has been called
		on it since the last call to resetRecentlyCommittedFlag().
		<p>
		Useful for Classes that want to know whether a user has edited
		this preference lately, before doing expensive work to update
		themselves.
	*/
	public boolean isRecentlyCommittedValue();
	/**
		<p>
		Calling this method is equicvalent to saying:
		"We no longer consider any edits to this Preference
		to be recent."
		<p>
		Call this method after all relevant Objects have had
		the opportunity to update themselves to a new value.
	*/
	public void setRecentlyCommittedFlag( boolean flag );
	/**
		Call this method to ask a Preference to assume as its
		value the Object represented by strVal.  Will throw
		an Exception if parsing fails because strVal is not
		meaningful to the Preference.
	*/
	public Object parseValue( String strVal ) throws Exception;
}
