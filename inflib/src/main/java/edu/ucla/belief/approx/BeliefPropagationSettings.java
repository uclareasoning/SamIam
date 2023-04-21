package edu.ucla.belief.approx;

import edu.ucla.util.*;

import java.util.*;

/** @author Arthur Choi
	@since  20050505 */
public class BeliefPropagationSettings implements UserObject, ChangeBroadcaster
{
	public static final int    INT_MAX_ITERATIONS_DEFAULT  = (int)     100;
	public static final long   LONG_TIMEOUT_MILLIS_DEFAULT = (long)  10000;
	public static final double DOUBLE_THRESHOLD_DEFAULT    = (double)10e-8;

	/** default IBP settings:
		timeout = 10 sec
		max iterations = 100
		convergence threshold = 10e-8
		use max iterations as stopping condition
	*/
	public BeliefPropagationSettings() {
		this.myTimeoutMillis = LONG_TIMEOUT_MILLIS_DEFAULT;
		this.myMaxIterations = INT_MAX_ITERATIONS_DEFAULT;
		this.myConvergenceThreshold = DOUBLE_THRESHOLD_DEFAULT;
		this.myScheduler = MessagePassingScheduler.getDefault();
	}

	/** IBP settings to use max iterations as stopping condition.
		If millis or max is 0, the respective stopping condition is unused.
	*/
	public BeliefPropagationSettings( long millis, int max, double thresh ) {
		this.myTimeoutMillis = millis;
		this.myMaxIterations = max;
		this.myConvergenceThreshold = thresh;
		this.myScheduler = MessagePassingScheduler.getDefault();
	}

	public BeliefPropagationSettings( long millis, int max, double thresh,
									  MessagePassingScheduler scheduler ) {
		this.myTimeoutMillis = millis;
		this.myMaxIterations = max;
		this.myConvergenceThreshold = thresh;
		this.myScheduler = scheduler;
	}

	/* sets timeout value for IBP. */
	public void setTimeoutMillis( long millis ){
		this.myTimeoutMillis = millis;
	}

	public long getTimeoutMillis(){
		return this.myTimeoutMillis;
	}

	/* sets max iteration value for IBP. */
	public void setMaxIterations( int max ){
		this.myMaxIterations = max;
	}

	public void setScheduler( MessagePassingScheduler scheduler){
		this.myScheduler = scheduler;
	}

	public int getMaxIterations(){
		return this.myMaxIterations;
	}

	public void setConvergenceThreshold( double thresh ){
		this.myConvergenceThreshold = thresh;
	}

	public double getConvergenceThreshold(){
		return this.myConvergenceThreshold;
	}

	public MessagePassingScheduler getScheduler() {
		return this.myScheduler;
	}

	public void killState(){
	}

	/** interface ChangeBroadcaster */
	public ChangeBroadcaster fireSettingChanged(){
  //public BeliefPropagationSettings fireSettingChanged(){
		//System.out.println( "BPSettings.fireSettingChanged(), " + myChangeListeners.size() );
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
		//System.out.println( "BPSettings.addChangeListener( "+listener.getClass().getName()+" )" );
		if(    myChangeListeners == null ){ myChangeListeners = new WeakLinkedList(); }
		return myChangeListeners.contains( listener ) ? false : myChangeListeners.add( listener );
	}

	/** interface ChangeBroadcaster */
	public boolean removeChangeListener( ChangeListener listener ){
		return myChangeListeners != null ? myChangeListeners.remove( listener ) : false;
	}

	/** interface UserObject */
	public UserObject onClone(){
		BeliefPropagationSettings ret = new BeliefPropagationSettings();
		ret.copy( this );
		//ret.myX = null;
		return ret;
	}

	public void copy( BeliefPropagationSettings toCopy )
	{
		boolean flagNotSettingsChanging = true;

		flagNotSettingsChanging &= myTimeoutMillis          == toCopy.myTimeoutMillis;
		flagNotSettingsChanging &= myMaxIterations          == toCopy.myMaxIterations;
		flagNotSettingsChanging &= myConvergenceThreshold   == toCopy.myConvergenceThreshold;
		flagNotSettingsChanging &= myScheduler              == toCopy.myScheduler;

		myTimeoutMillis          = toCopy.myTimeoutMillis;
		myMaxIterations          = toCopy.myMaxIterations;
		myConvergenceThreshold   = toCopy.myConvergenceThreshold;
		myScheduler              = toCopy.myScheduler;

		if( !flagNotSettingsChanging ) fireSettingChanged();
	}

	public final ChangeEvent EVENT_SETTING_CHANGED = new ChangeEventImpl().source( this );//, (int)0, "belief propagation setting changed" );

	private long myTimeoutMillis;
	private int myMaxIterations;
	private double myConvergenceThreshold;
	private MessagePassingScheduler myScheduler;

	// to be implemented :
	/*
	  private PropagationSchedule myPropagationSchedule;
	  private boolean myUseDamping;
	  private boolean myUseAbsoluteConvergence;
	*/

	transient private WeakLinkedList myChangeListeners;
}
