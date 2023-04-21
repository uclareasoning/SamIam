package edu.ucla.util;

/** general properties of something editable
	@author keith cascio
	@since  20080228 */
public enum PropertyKey{
	caption         (                        String .class ),
	info            (                        String .class ),
	floor           (                        Number .class ),
	ceiling         (                        Number .class ),
	defaultValue    (                        Object .class ),
	legal           (                         Class .class ),
	keystroke       (         javax.swing.KeyStroke .class ),
	advanced        (                       Boolean .class ),
	increment       (                        Number .class ),
	plural          (                        String .class ),
	notext          (                       Boolean .class ),
	domain          (          java.util.Collection .class ),
	actionlistener  ( java.awt.event.ActionListener .class );

	private PropertyKey( Class<?> clazz ){
		this.clazz = clazz;
	}

	public final Class<?> clazz;
}
