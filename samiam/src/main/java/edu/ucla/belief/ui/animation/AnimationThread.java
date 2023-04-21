package edu.ucla.belief.ui.animation;

import edu.ucla.belief.ui.networkdisplay.*;

import java.util.*;
import java.util.List;
import java.awt.*;

/**
	@author Keith Cascio
	@since 072804
*/
public class AnimationThread implements Runnable
{
	public AnimationThread( int steps, Animator anim ){
		this( steps, anim, "AnimationThread" );
	}

	public AnimationThread( int steps, Animator anim, String nameprefix )
	{
		this.mySteps = steps;
		this.myAnimator = anim;
		this.myNamePrefix = nameprefix;
	}

	public AnimationThread( AnimationThread toCopy )
	{
		this.mySteps = toCopy.mySteps;
		this.myAnimatedObjects = toCopy.myAnimatedObjects;
		this.myArray = toCopy.myArray;
		this.myParent = toCopy.myParent;
		this.myAnimator = toCopy.myAnimator;
		this.myNamePrefix = toCopy.myNamePrefix;
	}

	/** @since 051305 */
	public void start(){
		new Thread( (Runnable)this, myNamePrefix + " " + Integer.toString( INT_NAME_COUNTER++ ) ).start();
	}

	/** @since 080504 */
	public void die(){
		System.err.println( "Warning: "+Thread.currentThread().getName()+" dying" );
		myFlagDead = true;
	}

	/** @since 080404 */
	public Animator getAnimator(){
		return myAnimator;
	}

	public void setNetworkDisplay( NetworkDisplay nd ){
		myParent = nd;
	}

	/** @since 080904 */
	public void setReplay( boolean flag ){
		myFlagReplay = flag;
	}

	/** @since 080904 */
	public boolean getReplay(){
		return myFlagReplay;
	}

	public void add( SomethingAnimated ao )
	{
		synchronized( mySynch ){
			if( myAnimatedObjects == null ) throw new IllegalStateException();
			myAnimatedObjects.add( ao );
		}
	}

	public void addAll( Collection animatedObjects )
	{
		for( Iterator it = animatedObjects.iterator(); it.hasNext(); ) add( (SomethingAnimated) it.next() );
	}

	/** @since 072904 */
	protected void init( SomethingAnimated[] array ){
		//System.out.println( "("+ getName() + ").init()" );
		Animator.init( array, mySteps );
	}

	/** @since 080504 */
	protected void animate()
	{
		long delay = getAnimator().getDelay();

		for( int i=0; i<mySteps; i++ ){
			Animator.step( myArray );
			try{
				Thread.sleep( delay );
			}catch( InterruptedException e ){
				System.err.println( "Warning: animate() interrupted." );
				Thread.currentThread().interrupt();
				break;
			}
		}

		Animator.finish( myArray );
	}

	/** @since 080704 */
	protected void validateArray()
	{
		if( myArray == null )
		{
			synchronized( mySynch ){
				myArray = (SomethingAnimated[]) myAnimatedObjects.toArray( new SomethingAnimated[myAnimatedObjects.size()] );
				myAnimatedObjects = null;
			}
		}
	}

	public void run()
	{
		//System.out.println( "("+ getName() + ").start()" );
		if( myFlagDead ) return;

		validateArray();

		init( myArray );

		animate();

		if( myParent != null ) myParent.repaintTree();
	}

	protected static int INT_NAME_COUNTER = (int)0;

	protected int mySteps;
	protected List myAnimatedObjects = new LinkedList();
	protected SomethingAnimated[] myArray;
	protected Object mySynch = new Object();
	protected NetworkDisplay myParent;
	protected Animator myAnimator;
	protected String myNamePrefix;
	protected boolean myFlagDead = false;
	protected boolean myFlagReplay = false;
}
