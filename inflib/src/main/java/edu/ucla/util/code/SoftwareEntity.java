package edu.ucla.util.code;

import java.io.File;
import java.util.*;
import java.text.Collator;

/** @author Keith Cascio
	@since 032405 */
public class SoftwareEntity implements Comparable
{
	public SoftwareEntity( String descriptionshort, String descriptionverbose ){
		this.myDescriptionShort = descriptionshort;
		this.myDescriptionVerbose = descriptionverbose;
	}

	/** interface Comparable */
	public int compareTo( Object o ){
		if( !(o instanceof SoftwareEntity) ) return 0;
		return COLLATOR.compare( this.getDescriptionShort(), ((SoftwareEntity)o).getDescriptionShort() );
	}

	private static Collator COLLATOR = Collator.getInstance();

	public String getDescriptionShort(){
		return this.myDescriptionShort;
	}

	public String getDescriptionVerbose(){
		return this.myDescriptionVerbose;
	}

	public File guessLocation() throws Exception {
		return (File)null;
	}

	public File getPath(){
		return myPath;
	}

	public void setPath( File path ){
		if( myPath != path ){
			myPath = path;
			myFlagIsGuess = false;
		}
	}

	/** @since 033005 */
	public boolean isGuess(){
		return myFlagIsGuess;
	}

	public File guessLocationIfNecessary() throws Exception {
		if( getPath() == null ) setPath( guessLocation() );
		File noncanonical = getPath();
		if( noncanonical != null ){
			try{
				setPath( noncanonical.getCanonicalFile() );
			}catch( java.io.IOException e ){
				System.err.println( "Warning in SoftwareEntity.guessLocationIfNecessary(): " + e );
			}
			myFlagIsGuess = true;
		}
		return getPath();
	}

	/** @since 033005 */
	public void appendHint( SoftwareEntity hint ){
		if( myHints == null ) myHints = new LinkedList();
		myHints.addLast( hint );
	}

	/** @since 033005 */
	public List getHints(){
		if( myHints == null ) return Collections.EMPTY_LIST;
		else return Collections.unmodifiableList( myHints );
	}

	private String myDescriptionShort, myDescriptionVerbose;
	private File myPath;
	private boolean myFlagIsGuess = false;
	private LinkedList myHints;
}
