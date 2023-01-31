package edu.ucla.belief.ui.preference;

import java.awt.Component;
import javax.swing.*;

/** <p>
	So far the most complex of the Preference sub-Classes.
	<p>
	This class represents a preference that the user chooses
	from a set of objects.  In that sense, it could have been
	called a SetPreference.

	@author keith cascio
	@since  20020712 */
public class ObjectPreference extends AbstractPreference implements ListCellRenderer
{
	public ObjectPreference(	String key,
					String name,
					Object defaultValue,
					Object[] domain,
					DomainConverter converter )
	{
		super( key, name, defaultValue );
		myDomain          = domain;
		myDomainConverter = converter;
	}

	protected Object[]         myDomain;
	protected DomainConverter  myDomainConverter;
  //protected Object[]         myDisplayDomain = null;
	protected ListCellRenderer myListCellRenderer;
	protected JComboBox        myJComboBox;

	/** @since 20050216 */
	public JComboBox getJComboBox(){
		return this.myJComboBox;
	}

	/** @since 20070421 */
  /*protected Object[] getDisplayDomain(){
		if( myDisplayDomain != null ) return myDisplayDomain;

		myDisplayDomain = new String[ myDomain.length ];
		for( int i=0; i<myDomain.length; i++ ){
			myDisplayDomain[i] = myDomainConverter.getDisplayName( myDomain[i] );
		}

		return myDisplayDomain;
	}*/

	protected JComponent getEditComponentHook()
	{
		if( myJComboBox == null ){
			myJComboBox = new JComboBox( myDomain );
			getRenderer();
			myJComboBox.setRenderer( (ListCellRenderer) ObjectPreference.this );
			myJComboBox.setSelectedItem( getValue() );
			myJComboBox.addActionListener( this );
		}
		return myJComboBox;
	}

	public void hookSetEditComponentValue( Object newVal )
	{
		if( myJComboBox == null ) return;

		setListeningEnabled( false );
		for( int i=0; i<myDomain.length; i++ )
		{
			if( myDomainConverter.getValue( myDomain[ i ] ).equals( newVal ) )
			{
				myJComboBox.setSelectedIndex( i );
				break;
			}
		}
		setListeningEnabled( true );
	}

	public Object hookValueClone()
	{
		return myValue;
	}

	public Object getCurrentEditedValue()
	{
		return myDomainConverter.getValue( myJComboBox.getSelectedItem() );//myDomain[ myJComboBox.getSelectedIndex() ] );
	}

	protected String valueToString()
	{
		return myDomainConverter.valueToString( myValue );
	}

	public Object parseValue( String strVal ) throws Exception
	{
		return myValue = myDomainConverter.parseValue( strVal );
	}

	/** @since 20070422 */
	public int indexOf( Object value ){
		for( int i=0; i<myDomain.length; i++ ) if( myDomain[i] == value ) return i;
		return -1;
	}

	/** @since 20070422 */
	public ListCellRenderer getRenderer(){
		if( myListCellRenderer == null ) myListCellRenderer = new DefaultListCellRenderer();
		return myListCellRenderer;
	}

	/** @since 20070422 */
	public Component              getListCellRendererComponent( JList list, Object                            value, int index, boolean isSelected, boolean cellHasFocus ){
		return myListCellRenderer.getListCellRendererComponent(       list, myDomainConverter.getDisplayName( value ),   index,         isSelected,         cellHasFocus );
	}

	/**
		<p>
		This interface defines the responsibilities of an
		Object with which you construct an ObjectPreference,
		that will define some of its behaviors.
		<p>
		A DomainConverter converts Objects from the domain
		of the ObjectPreference into oterh appropriate Objects,
		depending on the context.

		@author Keith Cascio
		@since 071202
	*/
	public interface DomainConverter
	{
		/**
			Given an Object from the domain, return
			a name suitable for display to the user.
		*/
		public Object getDisplayName( Object obj );
		/**
			Given an object from the domain, return
			the value that should exist as the
			ObjectPreference's value (i.e. that will be
			returned by ObjectPreference.getValue().
		*/
		public Object getValue( Object obj );
		/**
			<p>
			You must implement this method in order
			to define the behavior of ObjectPreference.parseValue().
			<p>
			Given a String, return the Object that
			will be the value of the ObjectPreference.
		*/
		public Object parseValue( String toParse ) throws Exception;
		/**
			Given an Object from the domain, return
			a String ID suitable to write to a file.
		*/
		public String valueToString( Object obj );
	}
}
