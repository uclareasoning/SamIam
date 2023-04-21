package edu.ucla.belief.ui.preference;

import edu.ucla.belief.ui.util.ElementHandler;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Iterator;
import java.awt.event.*;
import javax.swing.JComponent;

/** This class represents a factoring out of
	one way of accomplishing the tasks
	that must be handled in order to
	implement the Preference interface.

	@author keith cascio
	@since  20020712 */
public abstract class AbstractPreference implements Preference, ActionListener
{
	public AbstractPreference( String key, String name, Object defaultValue )
	{
		this.myKey                    = key;
		this.myName                   = name;
		this.myValue = this.myDefault = defaultValue;
		this.myActionEvent            = new ActionEvent( this, (int)0, this.myKey );
	}

	protected String  myKey, myName, myDescription;
	protected Object  myValue, myDefault;
	protected transient ActionEvent myActionEvent;
	protected boolean myIsEdited = false, myFlagListeningEnabled = true;
	//public boolean debug = false;

	public void actionPerformed( ActionEvent evt )
	{
		if( myFlagListeningEnabled )
		{
			//System.out.println( "\n\n*****************************************************" );
			//(new Throwable()).printStackTrace();
			myIsEdited = true;
			if( myActionListeners != null )
			{
				for( Iterator it = myActionListeners.iterator(); it.hasNext(); )
				{
					((ActionListener)it.next()).actionPerformed( myActionEvent );
				}
			}
		}
	}

	/** @since 20100112 */
	public Preference describe( String description ){
		myDescription = description;
		return this;
	}
	/** @since 20100112 */
	public String getDescription(){
		return myDescription;
	}

	/** @since 20021029 */
	public void setListeningEnabled( boolean enabled )
	{
		myFlagListeningEnabled = enabled;
	}

	public void addActionListener( ActionListener AL )
	{
		if( myActionListeners == null ) myActionListeners = new LinkedList();
		myActionListeners.add( AL );
	}

	protected Collection myActionListeners = null;

	public String getDisplayName() { return myName; }

	/** @since 20070403 */
	public boolean displayCaption(){ return true; }
	public Object getValue()
	{
		return hookValueClone();
	}
	public Object getKey() { return myKey; }
	public boolean isComponentEdited() { return myIsEdited; }

	public abstract Object hookValueClone();

	public void setValue( Object newVal )
	{
		myValue = newVal;
		hookSetEditComponentValue( newVal );
	}

	/** @since 20060718 */
	public Object getDefault(){
		return this.myDefault;
	}

	/** revert back to default value
		@since 20060718 */
	public Object revert(){
		Object ret = this.getDefault();
		this.setValue( ret );
		return ret;
	}

	public abstract void hookSetEditComponentValue( Object newVal );

	public final JComponent getEditComponent()
	{
		myIsEdited = false;
		JComponent ret = getEditComponentHook();
		hookSetEditComponentValue( myValue );
		return ret;
	}

	/**
		<p>
		Sub-Classes must implement this method to actually
		construct the edit component.  In order for isComponentEdited()
		to work, add this as an actionListener on a JComponent
		that fires action events when the user does something that
		consitutes an 'edit'.
		<p>
		E.G. newJComponent.addActionListener( this );
	*/
	protected abstract JComponent getEditComponentHook();

	/** @since 20070402 */
	public StringBuffer appendXML( StringBuffer buff ){
		buff.append( "<pref class=\"" )
		.append( getClass().getName() ).append( "\""    ).append( " key="   );
		quoteXMLValue( buff, getKey().toString()        ).append( " value=" );
		quoteXMLValue( buff, valueToString()            ).append( " name="  );
		quoteXMLValue( buff, getDisplayName()           ).append( " />"     );
		return buff;
	}

	/** @since 20070402 */
	static public StringBuffer quoteXMLValue( StringBuffer buff, String xmlValue ){
		char delim = (xmlValue.indexOf( '"' ) >= 0) ? '\'' : '"';
		return buff.append( delim ).append( xmlValue ).append( delim );
	}

	/** @since 20070404 */
	public ElementHandler getElementHandler(){ return null; }

	/**
		Sub-Classes must implement this method to
		return a String representation of their value
		suitable to be written in the XML preference file.
	*/
	protected abstract String valueToString();

	public boolean isRecentlyCommittedValue()
	{
		//if( debug ) System.out.println( "("+getClass().getName().substring( getClass().getName().lastIndexOf('.')+1 )+")("+getKey()+")("+System.identityHashCode(this)+").isRecentlyCommittedValue() <- " + myFlagIsRecentlyCommitted );
		return myFlagIsRecentlyCommitted;
	}

	/**
		Sub-Classes must implement this method to
		commit the Object value represented by
		the current state of an edit component.
	*/
	//abstract public void commitValueHook();

	public final void commitValue()
	{
		//System.out.println( "AbstractPreference.commitValue()" );
		//commitValueHook();
		if( isComponentEdited() )
		{
			myValue = getCurrentEditedValue();
			myFlagIsRecentlyCommitted = true;
			//System.out.println( "\tnew value: " + myValue );
		}
	}

	public void setRecentlyCommittedFlag( boolean flag )
	{
		//if( debug ) System.out.println( "("+getClass().getName().substring( getClass().getName().lastIndexOf('.')+1 )+")("+getKey()+")("+System.identityHashCode(this)+").setRecentlyCommittedFlag( "+flag+" )" );
		myFlagIsRecentlyCommitted = flag;
	}

	protected boolean myFlagIsRecentlyCommitted = false;
}
