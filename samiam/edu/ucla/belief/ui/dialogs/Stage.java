package edu.ucla.belief.ui.dialogs;

import javax.swing.*;

/** Represent a single stage
	in a wizard-esque ui sequence.

	@author Keith Cascio
	@since 020405 */
public abstract class Stage
{
	public Stage( String title ){
		myTitle = title;
	}

	public boolean isDelayLikely(){
		return false;
	}

	public String getProgressMessage(){
		return "empty progress message";
	}

	public String getTitle(){
		return myTitle;
	}

	public String getWarning(){
		return (String)null;
	}

	public boolean isGreenLightNext(){
		return myNext != null;
	}

	public void setNext( Stage stage ){
		if( myNext != stage ){
			myNext = stage;
			if( stage != null ) stage.setPrevious( this );
		}
	}

	public Stage next() throws Exception {
		return myNext;
	}

	public boolean isGreenLightPrevious(){
		return myPrevious != null;
	}

	public void setPrevious( Stage stage ){
		if( myPrevious != stage ){
			myPrevious = stage;
			if( stage != null ) stage.setNext( this );
		}
	}

	public Stage previous(){
		return myPrevious;
	}

	public void invalidate(){
		myFlagValid = false;
	}

	public void invalidateFuture(){
		if( myNext != null ){
			myNext.invalidate();
			myNext.invalidateFuture();
		}
	}

	public JComponent getView( boolean validate ) throws Exception {
		if( !validate ) return myViewCached;
		if( myViewCached == null ) myFlagValid = false;
		if( !myFlagValid ){
			myViewCached = refresh();
			myFlagValid = true;
		}
		return myViewCached;
	}

	abstract public JComponent refresh() throws Exception;

	public void edited(){
		invalidateFuture();
	}

	private String myTitle;
	private Stage myPrevious, myNext;
	private JComponent myViewCached;
	private boolean myFlagValid;
}
