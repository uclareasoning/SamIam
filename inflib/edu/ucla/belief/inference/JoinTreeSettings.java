package edu.ucla.belief.inference;

import edu.ucla.structure.*;
import edu.ucla.belief.*;
import edu.ucla.util.*;

import java.util.*;
import java.lang.ref.SoftReference;

/** @author keith cascio
	@since  20030923 */
public class JoinTreeSettings implements UserObject, ChangeBroadcaster
{
	public static boolean FLAG_DEBUG_VERBOSE = Definitions.DEBUG;

	public JoinTreeSettings()
	{
	}

	public void setEliminationHeuristic( EliminationHeuristic h )
	{
		//System.out.println( "("+this+")" + myDebugID + ".setEliminationHeuristic( "+h+" )" );
		if( myEliminationHeuristic != h )
		{
			myEliminationHeuristic = h;
			settingChanged();
		}
	}

	public EliminationHeuristic getEliminationHeuristic()
	{
		return myEliminationHeuristic;
	}

	/** @since 012904 */
	public void setJoinTree( il2.inf.structure.JTUnifier jointree )
	{
		//System.out.println( "("+this+")" + myDebugID + ".setJoinTree( "+jointree+" )" );
		if( myJoinTree != jointree )
		{
			myJoinTree = jointree;
		}
	}

	/** @since 012904 */
	public il2.inf.structure.JTUnifier getJoinTree()
	{
		return myJoinTree;
	}

	public void setEngine( JoinTreeInferenceEngine engine )
	{
		//System.out.println( "("+this+")" + myDebugID + ".setEngine( "+engine+" )" );
		if( engine == null ) myEngine = null;
		else
		{
			myEngine = new SoftReference( engine );
			addChangeListener( engine );
		}
	}

	public JoinTreeInferenceEngine getEngine()
	{
		JoinTreeInferenceEngine ret = ( myEngine == null ) ? null : (JoinTreeInferenceEngine) myEngine.get();
		try{
			if( (ret != null) && (! ret.getValid()) ){ myEngine.clear(); myEngine = null; ret = null; }
		}catch( Throwable thrown ){
			System.err.println( "warning: JoinTreeSettings.getEngine() caught " + thrown );
		}
		return ret;
	}

	public void copy( JoinTreeSettings toCopy )
	{
		boolean flagNotSettingsChanging = true;

		flagNotSettingsChanging &= this.myEliminationHeuristic == toCopy.myEliminationHeuristic;
		flagNotSettingsChanging &= this.myEngine == toCopy.myEngine;
		flagNotSettingsChanging &= this.myJoinTree == toCopy.myJoinTree;

		this.myEliminationHeuristic = toCopy.myEliminationHeuristic;
		this.myEngine = toCopy.myEngine;
		this.myJoinTree = toCopy.myJoinTree;

		if( !flagNotSettingsChanging ) fireSettingChanged();
	}

	public void setDebugID( String id )
	{
		myDebugID = id;
	}

	protected void settingChanged()
	{
		myEngine = null;
		myJoinTree = null;
	}

	private WeakLinkedList myChangeListeners;
	public final ChangeEvent EVENT_SETTING_CHANGED	= new ChangeEventImpl().source( this );//, (int)0, "join tree setting changed" );

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

	/**
		interface UserObject
	*/
	public UserObject onClone()
	{
		JoinTreeSettings ret = new JoinTreeSettings();
		ret.copy( this );
		ret.myJoinTree = null;
		ret.myEngine = null;
		return ret;
	}

	private String myDebugID = "NormalJoinTreeSettings";
	private EliminationHeuristic myEliminationHeuristic = EliminationHeuristic.getDefault();
	//private InferenceEngine myEngine;
	private SoftReference myEngine;
	private il2.inf.structure.JTUnifier myJoinTree;
}
