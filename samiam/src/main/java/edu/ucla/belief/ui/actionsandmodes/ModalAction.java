package edu.ucla.belief.ui.actionsandmodes;

import edu.ucla.belief.ui.NetworkInternalFrame;
import edu.ucla.belief.ui.util.Util;

import java.awt.*;
import javax.swing.*;
import java.util.*;

/** @author keith cascio
	@since  20040114 */
public abstract class ModalAction extends SamiamAction implements SamiamUserModal
{
	public static final int     INT_REGISTER_SIZE_INITIAL = 88;
	public static final boolean FLAG_VERBOSE_STATICS      = false;

	public ModalAction( String name, String descrip, char mnemonic, Icon icon ){
		this( name, descrip, mnemonic, icon, (KeyStroke)null, true );
	}

	public ModalAction(	String name,
				String descrip,
				char mnemonic,
				Icon icon,
				KeyStroke stroke ){
		this( name, descrip, mnemonic, icon, stroke, true );
	}

	public ModalAction(	String name,
				String descrip,
				char mnemonic,
				Icon icon,
				KeyStroke stroke,
				boolean register )
	{
		super( name, descrip, mnemonic, icon, stroke );
		if( register ) register( this );
	}

	//private ModalAction(){
	//	throw new UnsupportedOperationException();
	//}

	public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
		return false;
	}

	public void setSamiamUserMode( SamiamUserMode mode ){
		setEnabled( decideEnabled( mode, (NetworkInternalFrame)null ) );
	}

	public void setMode( SamiamUserMode mode, NetworkInternalFrame nif ){
		setEnabled( decideEnabled( mode, nif ) );
	}

	//public static Iterator iterator(){
	//	return ( theRegister == null ) ? Collections.EMPTY_SET.iterator() : theRegister.iterator();
	//}

	/** @return The number of ModalActions whose enabled state
				changed.
		@since 20060720 */
	public static int setEnabledAllRegistered( boolean flag ){
		//if( FLAG_VERBOSE_STATICS ) Util.STREAM_VERBOSE.println( "ModalAction.setEnabledAllRegistered( "+flag+" )" );

		int count = 0;
		synchronized( theSynchronization ){

		for( int i=0; i<theNextRegisterIndex; i++ ){
			if( theRegister[i].isEnabled() != flag ){
				theRegister[i].setEnabled( flag );
				++count;
				//if( FLAG_VERBOSE_STATICS ) Util.STREAM_VERBOSE.print( theRegister[i] + " " );
			}
		}

		}
		//if( FLAG_VERBOSE_STATICS ) Util.STREAM_VERBOSE.println();
		return count;
	}

	public static void setModeAllRegistered( SamiamUserMode mode )
	{
		setModeAllRegistered( mode, null );
	}

	public static void setModeAllRegistered( SamiamUserMode mode, NetworkInternalFrame nif )
	{
		if( FLAG_VERBOSE_STATICS ) Util.STREAM_VERBOSE.println( "ModalAction.setModeAllRegistered( "+mode+" )" );

		synchronized( theSynchronization ){

		for( int i=0; i<theNextRegisterIndex; i++ ){
			theRegister[i].setMode( mode, nif );
			if( FLAG_VERBOSE_STATICS ) Util.STREAM_VERBOSE.print( theRegister[i] + " " );
		}

		}

		if( FLAG_VERBOSE_STATICS ) Util.STREAM_VERBOSE.println();
	}

	private static void register( ModalAction action )
	{
		synchronized( theSynchronization ){

		if( theRegister == null ){
			theRegister = new ModalAction[ INT_REGISTER_SIZE_INITIAL ];//ArrayList( INT_REGISTER_SIZE_INITIAL );
			theNextRegisterIndex = 0;
		}
		if( theNextRegisterIndex == INT_REGISTER_SIZE_INITIAL ){
			System.err.println( "Warning: number of ModalActions registered surpassed initial list size "+INT_REGISTER_SIZE_INITIAL+"." );
			return;
		}
		theRegister[ theNextRegisterIndex++ ] = action;//theRegister.add( action );

		if( FLAG_VERBOSE_STATICS ) Util.STREAM_VERBOSE.println( "ModalAction.register() #" + theNextRegisterIndex + ": " + action );

		}
	}

	//private static int countRegister(){
	//	return theNextRegisterIndex;
	//}

	//private static ModalAction[] getRegister(){
	//	return theRegister;
	//}

	//private static ArrayList theRegister;
	private static ModalAction[] theRegister;
	private static int theNextRegisterIndex;
	private static Object theSynchronization = new Object();
}
