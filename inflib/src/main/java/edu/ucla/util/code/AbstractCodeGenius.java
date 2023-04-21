package edu.ucla.util.code;

import edu.ucla.util.*;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.PrintStream;

/** @author keith cascio
	@since  20040507 */
public abstract class AbstractCodeGenius implements CodeGenius
{
	//public String describe();
	//public String getShortDescription();
	//public void writeCode( PrintStream out );
	//public CodeOption[] getOptions();

	public AbstractCodeGenius(){
		resetOptions();
	}

	/** @since 20060327 */
	public CodeOptionValue breadth(){
		return getOption( getOptionBreadth() );
	}

	/** @since 20060327 */
	public boolean isCompilable(){
		return getOptionBreadth().booleanValue( breadth() );
	}

	/** @since 20051107 */
	public Object getWarnings(){
		return null;
	}

	public CodeOptionValue getOption( CodeOption option ){
		Object ret = getMap().get( option );
		if( ret instanceof CodeOptionValue ) return (CodeOptionValue) ret;
		else return null;
	}
	public void setOption( CodeOption option, CodeOptionValue value ){
		if( getOption( option ) != value )
		{
			getMap().put( option, value );
			fireSettingChanged();
		}
	}
	public boolean getFlag( CodeOption option ){
		return getMap().get( option ) == Boolean.TRUE;
	}
	public void setFlag( CodeOption option, boolean value ){
		if( (!getMap().containsKey( option )) || (getFlag( option ) != value) )
		{
			getMap().put( option, value ? Boolean.TRUE : Boolean.FALSE );
			fireSettingChanged();
		}
	}

	public void resetOptions(){
		CodeOption[] options = getOptions();
		if( options == null ){
			System.err.println( "Warning: getOptions() == null" );
			return;
		}
		for( int i=0; i<options.length; i++ ){
			CodeOption option = options[i];
			if( option.isFlag() ) setFlag( option, option.getDefaultFlag() );
			else setOption( option, option.getDefault() );
		}
	}

	private WeakLinkedList myChangeListeners;
	public final ChangeEvent EVENT_SETTING_CHANGED	= new ChangeEventImpl().source( this );//, (int)0, getClass().getName() + " option changed" );

	/** interface ChangeBroadcaster */
	public ChangeBroadcaster fireSettingChanged(){
		if( myChangeListeners == null ){ return this; }

		myChangeListeners.cleanClearedReferences();
		ChangeEvent evt = EVENT_SETTING_CHANGED;
		ArrayList list = new ArrayList( myChangeListeners );
		for( Iterator it = list.iterator(); it.hasNext(); ){
			((ChangeListener)it.next()).settingChanged( evt );
		}
		return this;
	}

	/** interface ChangeBroadcaster */
	public boolean    addChangeListener( ChangeListener listener ){
		if(    myChangeListeners == null ){ myChangeListeners = new WeakLinkedList(); }
		return myChangeListeners.contains( listener ) ? false : myChangeListeners.add( listener );
	}

	/** interface ChangeBroadcaster */
	public boolean removeChangeListener( ChangeListener listener ){
		return myChangeListeners != null ? myChangeListeners.remove( listener ) : false;
	}

	protected Map getMap(){
		if( myMapOptionsToValues == null ) myMapOptionsToValues = new HashMap( getOptions().length );
		return myMapOptionsToValues;
	}

	/** @since 021005 */
	final public String getOutputClassName(){
		if( myOutputClassName == null ) return getOutputClassNameDefault();
		else return myOutputClassName;
	}

	/** @since 021005 */
	final public void setOutputClassName( String name ){
		this.myOutputClassName = name;
	}

	private Map myMapOptionsToValues;
	private String myOutputClassName;
}
