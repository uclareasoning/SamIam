package edu.ucla.belief.decision;

//import edu.ucla.belief.FiniteVariable;
import java.util.*;

/** @author Keith Cascio
	@since 120804 */
public class ParameterImpl implements Parameter
{
	public ParameterImpl( String id, double value ){
		this.myID = id;
		this.myValue = value;
	}

	public ParameterImpl( int seed, double value ){
		this.myID = "p_" + makeID( seed );
		//System.out.println( this.myID );
		this.myValue = value;
	}

	public String toString(){
		return Double.toString( myValue );
	}

	public void setValue( double newval ){
		this.myValue = newval;
	}

	public double getValue(){
		return this.myValue;
	}

	public String getID(){
		return this.myID;
	}

	public void setID( String newid ){
		this.myID = newid;
	}

	/** @since 011505 */
	public static double sum( Collection parameters ){
		double sum = (double)0;
		for( Iterator it = parameters.iterator(); it.hasNext(); )
			sum += ((Parameter)it.next()).getValue();
		return sum;
	}

	public static String makeID( int seed ){
		return makeIDRec( seed );
	}

	public static String makeIDRecursive( int seed ){
		if( seed > INT_ALPHABET_LAST_INDEX )
			return makeIDRecursive( seed % INT_ALPHABET_LENGTH ) + makeIDRecursive( seed / INT_ALPHABET_LENGTH );
		else return new String( ARRAY_ID_ALPHABET, seed, 1 );
	}

	public static String makeIDRec( int seed )
	{
		/*int initpos = 0;
		if( seed > INT_ALPHABET_LAST_INDEX )
		{
			int thisrecursiveseed = seed / INT_ALPHABET_LENGTH;
			if( thisrecursiveseed == LASTRECURSIVESEED ){
				initpos = LASTRECURSIVEPOSITION;
				seed = seed % INT_ALPHABET_LENGTH;
			}
			else LASTRECURSIVESEED = thisrecursiveseed;
		}

		int finalpos = appendBuffer( seed, initpos );
		LASTRECURSIVEPOSITION = finalpos - 2;

		return new String( BUFFREC, 0, finalpos );*/

		return new String( BUFFREC, 0, appendBuffer( seed, 0 ) );
	}

	private static int appendBuffer( int seed, int position ){
		if( position >= BUFFREC.length ) growBuffer();
		if( seed > INT_ALPHABET_LAST_INDEX ){
			position = appendBuffer( seed % INT_ALPHABET_LENGTH, position );
			position = appendBuffer( seed / INT_ALPHABET_LENGTH, position );
		}
		else BUFFREC[ position++ ] = (char) ARRAY_ID_ALPHABET[ seed ];
		return position;
	}

	private static void growBuffer(){
		char[] oldBuffer = BUFFREC;
		BUFFREC = new char[ BUFFREC.length << 1 ];
		System.arraycopy( oldBuffer, 0, BUFFREC, 0, oldBuffer.length );
	}

	public static String makeIDLinear( int seed ){
		int dupl = 1;
		while( seed > INT_ALPHABET_LAST_INDEX ){
			++dupl;
			seed -= INT_ALPHABET_LENGTH;
		}
		char crumb = STR_ID_ALPHABET.charAt( seed );
		if( BUFFLINEAR.length < dupl ) BUFFLINEAR = new char[ dupl << 1 ];
		Arrays.fill( BUFFLINEAR, 0, dupl, crumb );
		return new String( BUFFLINEAR, 0, dupl );
	}

	public static final String STR_ID_ALPHABET = "abcdefghijklmnopqrstuvwxyz";
	public static final byte[] ARRAY_ID_ALPHABET = STR_ID_ALPHABET.getBytes();
	public static final int INT_ALPHABET_LENGTH = STR_ID_ALPHABET.length();
	public static final int INT_ALPHABET_LAST_INDEX = INT_ALPHABET_LENGTH - 1;
	public static final int INT_ALPHABET_LENGTH_PLUS = INT_ALPHABET_LENGTH + 1;
	private static char[] BUFFLINEAR = new char[ 1 << 4 ];
	private static char[] BUFFREC = new char[ 1 << 4 ];
	private static int LASTRECURSIVESEED = -1, LASTRECURSIVEPOSITION = -1;

	private String myID;
	private double myValue;
}
