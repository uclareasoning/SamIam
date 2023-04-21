package edu.ucla.util.code;

import edu.ucla.util.Interruptable;

import java.util.*;
import java.util.regex.*;
import java.io.*;

/** @author keith cascio
	@since  20050324 */
public class ParanoidFileFinder
{
	public static final String STR_REGEX_NONWORD_NONPERIOD_NONSEP = "[^\\w\\."+getFileSeparatorUnicode()+"]";
	public static final String STR_REGEX_AFTER_LAST_FILE_SEPARATOR_NO_PERIODS = getFileSeparatorUnicode() + "([^\\."+getFileSeparatorUnicode()+"]*"+STR_REGEX_NONWORD_NONPERIOD_NONSEP+"|)";
	public static final String STR_REGEX_QUOTE = "\\Q";
	public static final String STR_REGEX_ENDQUOTE = "\\E";
	public static final String STR_REGEX_NONWORD_NONSEP = "[^\\w"+getFileSeparatorUnicode()+"]";
	public static final String STR_REGEX_ANY_NONSEPS = "[^"+getFileSeparatorUnicode()+"]*";

	public static final String STR_REGEX_PRE_EXECUTABLE = STR_REGEX_AFTER_LAST_FILE_SEPARATOR_NO_PERIODS;
	public static final String STR_REGEX_POST_EXECUTABLE = regexOrEnd( STR_REGEX_NONWORD_NONSEP + STR_REGEX_ANY_NONSEPS );

	public static final String STR_REGEX_PRE_SEARCH_SECURITY = getFileSeparatorUnicode() + STR_REGEX_ANY_NONSEPS;
	public static final String STR_REGEX_POST_SEARCH_SECURITY = regexOrEnd( STR_REGEX_ANY_NONSEPS );
	public static final String STR_REGEX_JDK = "([Jj]dk|JDK|[Ss]dk|SDK|[Jj]re|JRE|[Jj]ava|JAVA)";

	public static final String STR_REGEX_PRE_BEST = getFileSeparatorUnicode() + "\\Q";
	public static final String STR_REGEX_POST_BEST = "\\E$";

	/** @since 20050324 */
	public static String regexOrEnd( String regex ){
		return "($|(" + regex + "$))";
	}

	public void setFileNameQuoted( String name ){
		setRegexFileName( STR_REGEX_QUOTE + name + STR_REGEX_ENDQUOTE );
		myFileName = name;
	}

	public String getFileName(){
		return myFileName;
	}

	public void setRegexFileName( String regex ){
		setRegexFilePath( STR_REGEX_PRE_EXECUTABLE + regex + STR_REGEX_POST_EXECUTABLE );
		myFileName = null;
	}

	public void setRegexFilePath( String regex ){
		myPatternFilePath = Pattern.compile( regex );
		myFileName = null;
	}

	public void setRegexSecurityName( String regex ){
		setRegexSecurityPath( STR_REGEX_PRE_SEARCH_SECURITY + regex + STR_REGEX_POST_SEARCH_SECURITY );
	}

	public void setRegexSecurityPath( String regex ){
		myPatternSecurityPath = Pattern.compile( regex );
	}

	public void setDepth( int depth ){
		myDepth = depth;
	}

	/** @since 20050324 */
	public File chooseBestJavaExecutable( String name ) throws IOException {
		return chooseBestExecutable( searchJavaExecutable( name ) );
	}

	/** @since 20050330 */
	public File chooseBestJavaExecutable( String name, File hint ) throws IOException {
		if( !hint.isDirectory() ) hint = hint.getParentFile();
		int saveDepth = myDepth;
		setDepth( 1 );
		hint = surface( hint );
		File ret = chooseBestExecutable( searchJavaExecutable( name, hint ) );
		myDepth = saveDepth;
		return ret;
	}

	/** @since 20050324 */
	public File chooseBestExecutable( List found ){
		//System.out.println( "found.size()? " + found.size() );

		if( found.isEmpty() ) return (File)null;

		String filename = getFileName();
		if( filename == null ) throw new IllegalStateException( "filename == null, call setFileNameQuoted() first" );

		Collections.sort( found, ParanoidFileFinder.getNewestFirstComparator() );
		//System.out.println( "found? " + found );
		//printTimes( found, System.out );

		String regexPre = STR_REGEX_PRE_BEST + filename;

		Pattern pattern = Pattern.compile( regexPre + STR_REGEX_POST_BEST );
		File naked = getBestMatch( found, pattern );
		if( naked != null ) return naked;

		pattern = Pattern.compile( regexPre + ".exe" + STR_REGEX_POST_BEST );
		File exe = getBestMatch( found, pattern );
		if( exe != null ) return exe;

		return (File) found.iterator().next();
	}

	/** @since 20050323 */
	public static File getBestMatch( Collection files, Pattern pattern ){
		ArrayList matches = new ArrayList( 32 );
		long firsttime = (long)-1;
		boolean youngenough = true;

		File filefound;
		long lastmodified = (long)-1;
		for( Iterator it = files.iterator(); youngenough && it.hasNext(); ){
			filefound = (File) it.next();
			if( pattern.matcher( filefound.getPath() ).find() )
			{
				lastmodified = filefound.lastModified();
				if( matches.isEmpty() ) firsttime = lastmodified;
				else if( (firsttime - lastmodified) > LONG_TOO_OLD ) youngenough = false;

				if( youngenough ) matches.add( filefound );
			}
		}
		return getShortest( matches );
	}

	public static final long LONG_TOO_OLD = (long)90000000;//25 hours

	/** @since 20050329 */
	public static File getShortest( List matches ){
		if( (matches == null) || (matches.isEmpty()) ) return (File)null;

		File shortest = (File) matches.iterator().next();

		File next;
		for( Iterator it = matches.iterator(); it.hasNext(); ){
			next = (File) it.next();
			if( next.getAbsolutePath().length() < shortest.getAbsolutePath().length() ) shortest = next;
		}

		return shortest;
	}

	/** @since 20050330 */
	private File surface( File hint ){
		for( int i=0; (i<myDepth) && (hint.getParentFile() != null); i++ ) hint = hint.getParentFile();
		return hint;
	}

	/** @since 20050324 */
	public ArrayList searchJavaExecutable( String name ) throws IOException {
		String strjavahome = System.getProperty( "java.home" );
		if( (strjavahome == null) ) return null;
		//System.out.println( "javahome? " + strjavahome );

		File filejavahome = new File( strjavahome );
		if( !filejavahome.exists() ) return null;
		File filebase = filejavahome;
		filebase = surface( filebase );
		//System.out.println( "filebase? " + filebase );

		return searchJavaExecutable( name, filebase );
	}

	/** @since 20050323 */
	public ArrayList searchJavaExecutable( String name, File directory ) throws IOException {
		this.setFileNameQuoted( name );
		this.setRegexSecurityName( STR_REGEX_JDK );
		return (ArrayList) paranoidFindAll( directory, new ArrayList( 16 ) );
	}

	/** @since 20050323 */
	public Collection paranoidFindAll( File directory, Collection container ) throws IOException {
		return paranoidFindAll( directory, 0, true, container, new HashSet() );
	}

	/** @since 20050324 */
	private Collection paranoidFindAll( File directory, int depth, boolean paranoid, Collection container, Set seen ) throws IOException
	{
		this.myLocation = directory;

		try{
			Thread.sleep(4);//Interruptable.checkInterrupted();
		}catch( InterruptedException interruptedexception ){
			Thread.currentThread().interrupt();
			return container;
		}

		//System.out.print( "paranoidFindAll( depth? "+depth+", paranoid? "+paranoid+" )" );
		if( !directory.isDirectory() ) return container;

		File canonical = directory.getCanonicalFile();
		if( seen.contains( canonical ) ) return container;
		else seen.add( canonical );

		if( paranoid && myPatternSecurityPath.matcher( directory.getPath() ).find() ){
			//System.out.print( " !paranoid @ " + directory.getPath() );
			paranoid = false;
		}
		//System.out.println();

		File[] files = canonical.listFiles();
		if( (files == null) || (files.length < 1) ) return container;

		String absolute;
		for( int i=0; i<files.length; i++ ){
			if( !files[i].isDirectory() ){
				absolute = files[i].getAbsolutePath();
				if( myPatternFilePath.matcher( absolute ).find() ) container.add( files[i] );
			}
		}

		int deeper = depth + 1;
		if( (!paranoid) || (deeper <= myDepth) ){
			for( int i=0; i<files.length; i++ ){
				if( files[i].isDirectory() && (!ignore( files[i] )) ){
					paranoidFindAll( files[i], deeper, paranoid, container, seen );
				}
			}
		}

		return container;
	}

	/** @since 20091219 */
	public File pole(){
		return myLocation;
	}

	/** @since 032305 */
	public boolean ignore( File directory ){
		String path = directory.getPath();
		if( ARRAY_IGNORE_ENDINGS == null ){
			ARRAY_IGNORE_ENDINGS = new String[] {
				File.separator + "docs",
				File.separator + "demo",
				File.separator + "include",
				File.separator + "lib",
				File.separator + "sample",
				File.separator + "src"
			};
		}
		for( int i=0; i<ARRAY_IGNORE_ENDINGS.length; i++ )
			if( path.endsWith( ARRAY_IGNORE_ENDINGS[i] ) ) return true;
		return false;
	}

	/** @since 032305 */
	public static String getFileSeparatorUnicode(){
		if( STR_FILESEPARATOR_UNICODE == null ){
			//int codePoint = Character.codePointAt( File.separator, 0 );
			int codePoint = (int) File.separatorChar;
			String digits = Integer.toHexString( codePoint );
			while( digits.length() < 4 ) digits = "0" + digits;
			STR_FILESEPARATOR_UNICODE = "\\u" + digits;
		}
		return STR_FILESEPARATOR_UNICODE;
	}

	/** @since 032405 */
	public static Comparator getNewestFirstComparator(){
		if( COMPARATOR_NEWEST_FIRST == null ){
			COMPARATOR_NEWEST_FIRST = new Comparator(){
				public int compare( Object o1, Object o2 ){
					int ret = 0;
					//if( (!(o1 instanceof File)) || (!(o2 instanceof File)) ) ret = 0;
					//else ret = (int)(((File)o1).lastModified() - ((File)o2).lastModified());
					if( (!(o1 instanceof File)) || (!(o2 instanceof File)) ) ret = 0;
					else{
						long time1 = ((File)o1).lastModified();
						long time2 = ((File)o2).lastModified();
						if( time2 > time1 ) ret = 1;
						else if( time2 == time1 ) ret = 0;
						else ret = -1;
					}
					//System.out.println( "ret? " + ret );
					return ret;
				}
			};
		}
		return COMPARATOR_NEWEST_FIRST;
	}

	/** @since 032905 */
	public static void printTimes( Collection files, PrintStream out ){
		File next;
		for( Iterator it = files.iterator(); it.hasNext(); ){
			next = (File)it.next();
			out.println( next.lastModified() + " " + next.getAbsolutePath() );
		}
	}

	private static String STR_FILESEPARATOR_UNICODE;
	private static String[] ARRAY_IGNORE_ENDINGS;
	private static Comparator COMPARATOR_NEWEST_FIRST;

	private int myDepth = 3;
	private String myFileName;
	private Pattern myPatternFilePath;
	private Pattern myPatternSecurityPath;
	private File    myLocation;
}
