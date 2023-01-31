package edu.ucla.belief.ui.actionsandmodes;

import edu.ucla.belief.ui.NetworkInternalFrame;
import edu.ucla.belief.ui.UI;

/** @author keith cascio
	@since  20040114 */
public /*abstract*/ class ModeHandler
{
	//abstract public boolean decideEnabled( SamiamUserMode mode );
	public boolean decideEnabled( SamiamUserMode mode ){
		throw new RuntimeException();
	}

	public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
		return decideEnabled( mode );
	}

	private ModeHandler( String name ){
		myName = ( name == null ) ? "" : name;
	}

	public String toString(){
		return super.toString() + " " + myName;
	}

	public static final ModeHandler INDEPENDANT = new ModeHandler( "independant" )
	{
		public boolean decideEnabled( SamiamUserMode mode ){
			return true;
		}
	};

	public static final ModeHandler OPEN_NETWORK = new ModeHandler( "open network dependant" )
	{
		public boolean decideEnabled( SamiamUserMode mode ){
			return mode.contains( SamiamUserMode.OPENFILE );
		}
	};

	/** @since 20100111 */
	public static final ModeHandler OPENTHAWED = new ModeHandler( "open network &&  evidence not frozen" ){
		public boolean decideEnabled( SamiamUserMode mode ){
			return OPEN_NETWORK.decideEnabled( mode ) && (! mode.contains( SamiamUserMode.EVIDENCEFROZEN ));
		}
	};

	public static final ModeHandler OPENBUTNOTCOMPILING = new ModeHandler( "open network && not compiling dependant" )
	{
		public boolean decideEnabled( SamiamUserMode mode ){
			return mode.contains( SamiamUserMode.OPENFILE ) && (!mode.contains( SamiamUserMode.COMPILING ));
		}
	};

	/** @since 20050922 */
	public static final ModeHandler NOTCOMPILING = new ModeHandler( "not compiling dependant" )
	{
		public boolean decideEnabled( SamiamUserMode mode ){
			return !mode.contains( SamiamUserMode.COMPILING );
		}
	};

	/** @since 20080309 */
	static public int countNonDaemons( ThreadGroup group ){
		int        active = group.activeCount();
		if(        active  <    1 ){ return  active; }
		Thread[]  threadz = new Thread[ active ];
		int         count = -1;
		while( (count = group.enumerate( threadz )) >= threadz.length ){ threadz = new Thread[ threadz.length + 4 ]; }
		count             =  0;
		for( int i=0; i<threadz.length; i++ ){
			if( (threadz[i] != null) && (! threadz[i].isDaemon()) ){ ++count; }
		}
		return count;
	}

	/** @since 20060719 */
	public static final ModeHandler NOTBUSYLOCAL = new ModeHandler( "local not busy" )
	{
		public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
			if(                               mode == null ){ return false; }
			if( ! NOTCOMPILING.decideEnabled( mode )       ){ return false; }
			if(                                nif == null ){ return  true; }
			ThreadGroup group = nif.getThreadGroup();
			return     (group == null) || (countNonDaemons( group ) < 1);
		}
	};

	/** @since 20060720 */
	public static final ModeHandler NOTBUSYGLOBAL = new ModeHandler( "global not busy" )
	{
		public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
			if(                               mode == null ){ return false; }
			if( ! NOTCOMPILING.decideEnabled( mode )       ){ return false; }
			UI ui = (nif  == null) ? UI.STATIC_REFERENCE : nif.getParentFrame();
			if(                                 ui == null ){ return  true; }
			ThreadGroup group = ui.getThreadGroup();
			return     (group == null) || (countNonDaemons( group ) < 1);
		}
	};

	/** @since 20060720 */
	public static final ModeHandler BUSYLOCAL = new ModeHandler( "local busy" )
	{
		public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
			if(                               mode == null ){ return false; }
			if( mode.contains( SamiamUserMode .COMPILING ) ){ return  true; }
			if(                                nif == null ){ return false; }
			ThreadGroup group = nif.getThreadGroup();
			return     (group != null) && (countNonDaemons( group ) > 0);
		}
	};

	/** @since 20060720 */
	public static final ModeHandler BUSYGLOBAL = new ModeHandler( "global busy" )
	{
		public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
			if(                               mode == null ){ return false; }
			if( mode.contains( SamiamUserMode .COMPILING ) ){ return  true; }
			UI ui = (nif  == null) ? UI.STATIC_REFERENCE : nif.getParentFrame();
			if(                                 ui == null ){ return false; }
			ThreadGroup group = ui.getThreadGroup();
			return     (group != null) && (countNonDaemons( group ) > 0);
		}
	};

	/** @since 20060721 */
	public static final ModeHandler OPENANDNOTBUSYLOCAL = new ModeHandler( "open network && local not busy" )
	{
		public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
			return mode.contains( SamiamUserMode.OPENFILE ) && NOTBUSYLOCAL.decideEnabled( mode, nif );
		}
	};

	/** @since 20051007 */
	public static final ModeHandler OPENBUTNOTMODELOCKED = new ModeHandler( "open network && not mode locked" )
	{
		public boolean decideEnabled( SamiamUserMode mode ){
			return mode.contains( SamiamUserMode.OPENFILE ) && (!mode.contains( SamiamUserMode.MODELOCK ));
		}
	};

	public static final ModeHandler QUERY_MODE = new ModeHandler( "query mode dependant" )
	{
		public boolean decideEnabled( SamiamUserMode mode ){
			return mode.contains( SamiamUserMode.QUERY ) && (!mode.contains( SamiamUserMode.COMPILING ));
		}
	};

	/** @since 20051017 */
	public static final ModeHandler QUERY_MODE_SUPPORTS_PRE = new ModeHandler( "query mode && supports pr(e)" )
	{
		public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
			return QUERY_MODE.decideEnabled( mode ) && (nif != null) && (nif.getInferenceEngine() != null) && nif.getInferenceEngine().probabilitySupported();
		}
	};

	public static final ModeHandler PARTIAL_ENGINE = new ModeHandler( "query mode && partial engine dependant" )
	{
		public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
			return QUERY_MODE.decideEnabled( mode ) && (nif != null) && (nif.getPartialDerivativeEngine() != null);
		}
	};

	/** @since 20051017 */
	public static final ModeHandler QUERY_THAWED_PARTIAL_ENGINE = new ModeHandler( "query mode && partial engine && evidence not frozen" )
	{
		public boolean decideEnabled( SamiamUserMode mode ){
			return PARTIAL_ENGINE.decideEnabled( mode );
		}

		public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
			return PARTIAL_ENGINE.decideEnabled( mode, nif ) && (!mode.contains( SamiamUserMode.EVIDENCEFROZEN ));
		}
	};

	public static final ModeHandler EDIT_MODE = new ModeHandler( "edit mode dependant" )
	{
		public boolean decideEnabled( SamiamUserMode mode ){
			return mode.contains( SamiamUserMode.EDIT ) && (!mode.contains( SamiamUserMode.SMILEFILE )) && (!mode.contains( SamiamUserMode.COMPILING )) && (!mode.contains( SamiamUserMode.READONLY ));
		}
	};

	public static final ModeHandler WRITABLE = new ModeHandler( "not read only && not compiling" )
	{
		public boolean decideEnabled( SamiamUserMode mode ){
			return (!mode.contains( SamiamUserMode.COMPILING )) && (!mode.contains( SamiamUserMode.READONLY ));
		}
	};

	public static final ModeHandler SAVE_OKAY = new ModeHandler( "save ok: open network && not compiling && not read only" )
	{
		public boolean decideEnabled( SamiamUserMode mode ){
			return mode.contains( SamiamUserMode.OPENFILE ) && (!mode.contains( SamiamUserMode.COMPILING )) && (!mode.contains( SamiamUserMode.READONLY ));
		}
	};

	private String myName;
}
