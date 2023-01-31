package edu.ucla.util;

/** @author keith cascio
	@since  20081128 */
public class ChangeEventImpl implements ChangeEvent{
	public   ChangeEventImpl source( ChangeBroadcaster source ){
		this.source = source;
		return this;
	}

	public   ChangeBroadcaster       getSource(){
		return          this.source;
	}

	private  ChangeBroadcaster          source;
}
