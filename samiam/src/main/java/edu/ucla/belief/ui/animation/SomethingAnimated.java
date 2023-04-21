package edu.ucla.belief.ui.animation;

/**
	@author Keith Cascio
	@since 072804
*/
public interface SomethingAnimated
{
	public void init( int steps );
	public void step();
	public Animator getAnimator();
	public void finish();
	public boolean finished();
}
