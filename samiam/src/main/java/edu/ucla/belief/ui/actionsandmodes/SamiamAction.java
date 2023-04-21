package edu.ucla.belief.ui.actionsandmodes;

import edu.ucla.belief.ui.preference.*;

import java.awt.*;
import javax.swing.*;
import java.awt.event.ActionEvent;

/** @author keith cascio
	@since  20021022 */
public abstract class SamiamAction extends AbstractAction implements PreferenceListener, Runnable
{
	public SamiamAction( String name, String descrip, char mnemonic, Icon icon )
	{
		setName( name );
		setToolTipText( descrip );
		//super.putValue( Action.MNEMONIC_KEY, new Character( mnemonic ) );
		setIcon( icon );
		init();
	}

	public SamiamAction(	String name,
				String descrip,
				char mnemonic,
				Icon icon,
				KeyStroke stroke )
	{
		this( name, descrip, mnemonic, icon );
		if( stroke != null ) super.putValue( Action.ACCELERATOR_KEY, stroke );
	}

	private static String SAMIAMACTION_SELECTED_KEY, SAMIAMACTION_LARGE_ICON_KEY;
	static{
		try{
			SAMIAMACTION_SELECTED_KEY = Action.class.getField( "SELECTED_KEY" ).get( null ).toString();
		}catch( Throwable thrown ){
			SAMIAMACTION_SELECTED_KEY = "SAMIAMACTION_SELECTED_KEY";
		}
		try{
			SAMIAMACTION_LARGE_ICON_KEY = Action.class.getField( "LARGE_ICON_KEY" ).get( null ).toString();
		}catch( Throwable thrown ){
			SAMIAMACTION_LARGE_ICON_KEY = "SAMIAMACTION_LARGE_ICON_KEY";
		}
	}

	/** @since 20080124 */
	public static final String SAMIAMACTION_SELECTED_KEY_PUBLIC   = SAMIAMACTION_SELECTED_KEY,
	                           SAMIAMACTION_LARGE_ICON_KEY_PUBLIC = SAMIAMACTION_LARGE_ICON_KEY;

	/** Comes in handy sometimes.
		@since 20071217	*/
	public void run(){
		this.actionP( null );
	}

	/** @since 20071207 */
	public SamiamAction setSelected( boolean selected ){
		super.putValue( SAMIAMACTION_SELECTED_KEY, selected ? Boolean.TRUE : Boolean.FALSE );
		return this;
	}

	/** @since 20071207 */
	public boolean       isSelected(){
		return super.getValue( SAMIAMACTION_SELECTED_KEY ) == Boolean.TRUE;
	}

	/** @since 20071209 */
	public SamiamAction setAccelerator( KeyStroke stroke ){
		super.putValue( Action.ACCELERATOR_KEY, stroke );
		return this;
	}

	/** @since 020905 */
	public void setName( String name ){
		super.putValue( Action.NAME, name );
	}

	/** @since 011504 */
	public void setIcon( Icon icon ){
		super.putValue( Action.SMALL_ICON, icon );
	}

	/** @since 011504 */
	public void setToolTipText( String descrip ){
		super.putValue( Action.SHORT_DESCRIPTION, descrip );
	}

	/** construction-time hook
		@since 20021022 */
	public void init() {}

	/** @since 20070309 nasa */
	public JComponent getInputComponent(){
		return null;
	}

	/** @since 20070402 */
	public SamiamPreferences setPreferences( SamiamPreferences pref ){
		return myPreferences = pref;
	}

	/** interface PreferenceListener */
	public void  updatePreferences(){}
	/** interface PreferenceListener */
	public void previewPreferences(){}
	/** interface PreferenceListener */
	public void     setPreferences(){}

	/** @since 20070309 nasa */
	public void setEnabled( boolean flag ){
		super.setEnabled( flag );
		JComponent comp  = this.getInputComponent();
		if( comp != null ) comp.setEnabled( flag );
	}

	/** @since 20071210 */
	public SamiamAction putValueProtected( String key, Object newValue ){
		super.putValue( key, newValue );
		return this;
	}

	/** @since 20021024 */
	public void putValue( String key, Object newValue )
	{
		//throw new UnsupportedOperationException();
	}

	/** @since 111802 */
	public String toString()
	{
		return (String) super.getValue( Action.NAME );
	}

	/** @since 012004 */
	public void actionP( Object source ){
		((AbstractAction)this).actionPerformed( new ActionEvent( source, INT_ACTIONID_GENERIC, STR_ACTIONCOMMAND_GENERIC ) );
	}

	public static final String
	  KEY_EPHEMERAL             = "SamiamAction.KEY_EPHEMERAL",
	  STR_ACTIONCOMMAND_GENERIC = "SamiamAction.STR_ACTIONCOMMAND_GENERIC";
	public static final int INT_ACTIONID_GENERIC = (int)2004;

	protected SamiamPreferences myPreferences;
}
