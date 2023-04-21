package edu.ucla.belief.io.hugin;

import edu.ucla.belief.io.*;
//import edu.ucla.util.JVMTI;

import java.io.*;
import java.util.regex.*;

/** This class makes a lot of dangerous assumptions in order to
	achieve fast performance.  That is ok because the only purpose
	of this class is to enable progress monitoring.

	@author keith cascio
	@since 20060518 */
public class SkimmerEstimator implements Estimate
{
	//public static final String REGEX_NODEBLOCK         = "^\\s*node\\s";
	//public static final String REGEX_POTENTIALBLOCK    = "^\\s*potential\\s";
	//public static final String REGEX_START_OF_BLOCK    = "^\\s*(node|potential)\\s";
	//public static final String REGEX_END_OF_BLOCK      = "^[^%]*}";

	//public static final Pattern PATTERN_NODEBLOCK      = Pattern.compile( REGEX_NODEBLOCK );
	//public static final Pattern PATTERN_POTENTIALBLOCK = Pattern.compile( REGEX_POTENTIALBLOCK );
	//public static final Pattern PATTERN_START_OF_BLOCK = Pattern.compile( REGEX_START_OF_BLOCK );
	//public static final Pattern PATTERN_END_OF_BLOCK   = Pattern.compile( REGEX_END_OF_BLOCK );

	public SkimmerEstimator( File fileHugin ){
		this.myFile = fileHugin;
	}

	/*public SkimmerEstimator( Reader reader ){
		this.myReader = reader;
	}*/

	public SkimmerEstimator( InputStream stream ){
		//this.myReader = new InputStreamReader( myInputStream = stream );
		this.myInputStream = stream;
	}

	/** @since 20060619 */
	public SkimmerEstimator(){}

	/** interface Estimate */
	public void init( File fileHugin ){
		this.myFile = fileHugin;
	}

	/*private BufferedReader reader() throws IOException {
		if( this.myReader != null ){
			if( this.myReader instanceof BufferedReader ) return (BufferedReader)this.myReader;
			else return new BufferedReader( this.myReader );
		}
		else if( this.myFile != null ) return new BufferedReader( new FileReader( this.myFile ) );
		else if( this.myInputStream != null ) return new BufferedReader( new InputStreamReader( this.myInputStream ) );
		else return null;
	}*/

	private BufferedInputStream stream() throws IOException {
		if(      this.myReader      != null ) throw new IllegalStateException();
		else if( this.myFile        != null ) return new BufferedInputStream( new FileInputStream( this.myFile ) );
		else if( this.myInputStream != null ){
			if( this.myInputStream instanceof BufferedInputStream ) return (BufferedInputStream)this.myInputStream;
			else return new BufferedInputStream( this.myInputStream );
		}
		else return null;
	}

	/** interface Estimate */
	public void estimate(){
		if( myFlagDoneEstimate ) return;

		long start = System.currentTimeMillis();//JVMTI.getCurrentThreadCpuTimeUnsafe();

		myCountNodeBlocks        = 0;
		myCountPotentialBlocks   = 0;
		myCountProbabilityValues = 0;

		try{
			BufferedReader      reader   = null;
			BufferedInputStream stream   = null;

			boolean flagInPotentialBlock = false;
			//Matcher matcher;
			//String line;
			//String group;
			//boolean flagScan             = false;
			//boolean flagRealScan         = false;
			//int     lastScan             = -1;
			byte[]  buffer    = new byte[16];
			boolean freshLine = true;
			boolean matches   = false;
			boolean alive     = true;
			boolean inValue   = false;
			boolean isDigit   = false;
			int     nextChar;
			int     numRead   = -1;

			/*reader = this.reader();

			while( (line = reader.readLine()) != null ){
				matcher = PATTERN_NODEBLOCK.matcher( line );
				if( matcher.find() ){
					++myCountNodeBlocks;
					flagInPotentialBlock = false;
				}
				else{
					matcher.usePattern( PATTERN_POTENTIALBLOCK );
					if( matcher.find() ){
						++myCountPotentialBlocks;
						flagInPotentialBlock = flagRealScan = true;
					}
					else if( flagInPotentialBlock ){
						if( flagRealScan ){
							lastScan = scanForProbabilityValues( line );
							if( lastScan > 0 ) flagRealScan = false;
						}
						else fakeScan( line, lastScan );
					}
				}

				matcher.usePattern( PATTERN_END_OF_BLOCK );
				if( matcher.find() ) flagInPotentialBlock = false;
			}*/

			stream = this.stream();

			while( (nextChar = stream.read()) != -1 ){
				if( nextChar == (int)'\n' ){
					alive     = true;
					freshLine = true;
					inValue   = false;
				}
				else if( alive )//&& (!isWhitespace(nextChar)) )
				{
					if( flagInPotentialBlock ){
						isDigit  = isDigitISOLATIN1(nextChar);

						if( inValue ){
							if( (!isDigit) && (!isNonDigitNumerical(nextChar)) ) inValue = false;
						}
						else if( isDigit ){
							inValue = true;
							++myCountProbabilityValues;
						}
					}
					else if( freshLine ){
						if( nextChar == (int)'n' ){
							stream.mark( buffer.length );
							if( (numRead = stream.read( buffer )) >= 4 ){
								matches  = buffer[0] == 'o';
								matches &= buffer[1] == 'd';
								matches &= buffer[2] == 'e';
								matches &= isWhitespace( buffer[3] );

								if( matches ){
									++myCountNodeBlocks;
									flagInPotentialBlock = false;
									alive = false;
								}
							}
							stream.reset();
							stream.read();
							continue;
						}
						else if( nextChar == (int)'p' ){
							stream.mark( buffer.length );
							if( (numRead = stream.read( buffer )) >= 9 ){
								matches  = buffer[0] == 'o';
								matches &= buffer[1] == 't';
								matches &= buffer[2] == 'e';
								matches &= buffer[3] == 'n';
								matches &= buffer[4] == 't';
								matches &= buffer[5] == 'i';
								matches &= buffer[6] == 'a';
								matches &= buffer[7] == 'l';
								matches &= isWhitespace( buffer[8] );

								if( matches ){
									++myCountPotentialBlocks;
									flagInPotentialBlock = true;
									alive = false;
								}
							}
							stream.reset();
							stream.read();
							continue;
						}
					}

					if(      nextChar == (int)'%' ){
						alive = false;
						continue;
					}
					else if( nextChar == (int)'}' ){
						flagInPotentialBlock = false;
						alive = false;
						continue;
					}

					freshLine = isWhitespace(nextChar);
				}
			}

			try{
				if( myReader      != null ) myReader.reset();
				if( myInputStream != null ) myInputStream.reset();
			}catch( IOException ioexception ){
				//stream does not support reset()
			}
			if( reader != null ) reader.close();
		}
		catch( IOException ioexception ){
			System.err.println( "warning: SkimmerEstimator.estimate() caught " + ioexception );
		}

		this.interpret();

		long end = System.currentTimeMillis();//JVMTI.getCurrentThreadCpuTimeUnsafe();
		myCost = end - start;
		//System.out.println( "SkimmerEstimator.estimate() cost " + myCost + ", found " + getCountNodeBlocks() + " nodes, " + getCountPotentialBlocks() + " potentials, " + getCountProbabilityValues() + " values" );
		//System.out.println( "    inc node: " + getProgressIncrementNodeBlock() + ", inc potential: " + getProgressIncrementPoteBlock() + ", max: " + getProgressMax() );

		myFlagDoneEstimate = true;
	}

/* high performance
			stream = this.stream();

			byte[] buffer = new byte[16];
			boolean freshLine = true;
			boolean matches   = true;
			boolean alive     = true;
			boolean inValue   = false;
			boolean isDigit   = false;
			int nextChar;
			int numRead = -1;
			while( (nextChar = stream.read()) != -1 ){
				if( nextChar == (int)'\n' ){
					alive     = true;
					freshLine = true;
					inValue   = false;
				}
				else if( alive )//&& (!isWhitespace(nextChar)) )
				{
					if( flagInPotentialBlock ){
						isDigit  = isDigitISOLATIN1(nextChar);

						if( inValue ){
							if( (!isDigit) && (!isNonDigitNumerical(nextChar)) ) inValue = false;
						}
						else if( isDigit ){
							inValue = true;
							++myCountProbabilityValues;
						}
					}
					else if( freshLine ){
						if( nextChar == (int)'n' ){
							stream.mark( buffer.length );
							if( (numRead = stream.read( buffer )) >= 4 ){
								matches  = buffer[0] == 'o';
								matches &= buffer[1] == 'd';
								matches &= buffer[2] == 'e';
								matches &= isWhitespace( buffer[3] );

								if( matches ){
									++myCountNodeBlocks;
									flagInPotentialBlock = false;
									alive = false;
								}
							}
							stream.reset();
							stream.read();
							continue;
						}
						else if( nextChar == (int)'p' ){
							stream.mark( buffer.length );
							if( (numRead = stream.read( buffer )) >= 9 ){
								matches  = buffer[0] == 'o';
								matches &= buffer[1] == 't';
								matches &= buffer[2] == 'e';
								matches &= buffer[3] == 'n';
								matches &= buffer[4] == 't';
								matches &= buffer[5] == 'i';
								matches &= buffer[6] == 'a';
								matches &= buffer[7] == 'l';
								matches &= isWhitespace( buffer[8] );

								if( matches ){
									++myCountPotentialBlocks;
									flagInPotentialBlock = true;
									alive = false;
								}
							}
							stream.reset();
							stream.read();
							continue;
						}
					}

					if(      nextChar == (int)'%' ) alive = false;
					else if( nextChar == (int)'}' ){
						flagInPotentialBlock = false;
						alive = false;
					}

					freshLine = isWhitespace(nextChar);
				}
			}
*/

/* super high performance
			stream = this.stream();

			byte[] buffer = new byte[16];
			boolean freshLine = true;
			boolean matches   = true;
			boolean alive     = true;
			int nextChar;
			int numRead = -1;
			while( (nextChar = stream.read()) != -1 ){
				if( nextChar == (int)'\n' ){
					alive     = true;
					freshLine = true;
				}
				else if( alive && (!isWhitespace(nextChar)) ){

					if( freshLine ){
						if( nextChar == (int)'n' ){
							stream.mark( buffer.length );
							if( (numRead = stream.read( buffer )) >= 4 ){
								matches  = buffer[0] == 'o';
								matches &= buffer[1] == 'd';
								matches &= buffer[2] == 'e';
								matches &= isWhitespace( buffer[3] );

								if( matches ){
									++myCountNodeBlocks;
									alive = false;
								}
							}
							stream.reset();
							stream.read();
						}
						else if( nextChar == (int)'p' ){
							stream.mark( buffer.length );
							if( (numRead = stream.read( buffer )) >= 9 ){
								matches  = buffer[0] == 'o';
								matches &= buffer[1] == 't';
								matches &= buffer[2] == 'e';
								matches &= buffer[3] == 'n';
								matches &= buffer[4] == 't';
								matches &= buffer[5] == 'i';
								matches &= buffer[6] == 'a';
								matches &= buffer[7] == 'l';
								matches &= isWhitespace( buffer[8] );

								if( matches ){
									++myCountPotentialBlocks;
									alive = false;
								}
							}
							stream.reset();
							stream.read();
						}
					}

					if( nextChar == (int)'%' ) alive = false;

					freshLine = false;
				}
			}
*/

/* //original
			while( (line = reader.readLine()) != null ){
				matcher = PATTERN_NODEBLOCK.matcher( line );
				if( matcher.find() ){
					++myCountNodeBlocks;
					flagInPotentialBlock = false;
				}
				else{
					matcher.usePattern( PATTERN_POTENTIALBLOCK );
					if( matcher.find() ){
						++myCountPotentialBlocks;
						flagInPotentialBlock = flagRealScan = true;
					}
					else if( flagInPotentialBlock ){
						if( flagRealScan ){
							lastScan = scanForProbabilityValues( line );
							if( lastScan > 0 ) flagRealScan = false;
						}
						else fakeScan( line, lastScan );
					}
				}

				matcher.usePattern( PATTERN_END_OF_BLOCK );
				if( matcher.find() ) flagInPotentialBlock = false;
			}
*/

/* not really any better
			while( (line = reader.readLine()) != null ){
				matcher = PATTERN_START_OF_BLOCK.matcher( line );
				if( matcher.find() ){
					if( matcher.group(1).charAt(0) == 'n' ){
						++myCountNodeBlocks;
						flagInPotentialBlock = false;
					}
					else{
						++myCountPotentialBlocks;
						flagInPotentialBlock = flagRealScan = true;
					}
				}
				else if( flagInPotentialBlock ){
					if( flagRealScan ){
						lastScan = scanForProbabilityValues( line );
						if( lastScan > 0 ) flagRealScan = false;
					}
					else fakeScan( line, lastScan );
				}

				matcher.usePattern( PATTERN_END_OF_BLOCK );
				if( matcher.find() ) flagInPotentialBlock = false;
			}
*/

/* much worse for some reason
			while( (line = reader.readLine()) != null ){
				if( flagInPotentialBlock ){
					if( flagRealScan ){
						lastScan = scanForProbabilityValues( line );
						if( lastScan > 0 ) flagRealScan = false;
					}
					else fakeScan( line, lastScan );
				}

				matcher = PATTERN_END_OF_BLOCK.matcher( line );
				if( matcher.find() ) flagInPotentialBlock = false;

				matcher.usePattern( PATTERN_NODEBLOCK );
				if( matcher.find() ){
					++myCountNodeBlocks;
					flagInPotentialBlock = false;
				}
				else{
					matcher.usePattern( PATTERN_POTENTIALBLOCK );
					if( matcher.find() ){
						++myCountPotentialBlocks;
						flagInPotentialBlock = flagRealScan = true;
					}
				}
			}
*/

/* also bad
			while( (line = reader.readLine()) != null ){
				flagScan = flagInPotentialBlock;

				matcher = PATTERN_END_OF_BLOCK.matcher( line );
				if( matcher.find() ) flagInPotentialBlock = false;

				if( flagScan ){
					if( flagRealScan ){
						lastScan = scanForProbabilityValues( line );
						if( lastScan > 0 ) flagRealScan = false;
					}
					else fakeScan( line, lastScan );
				}
				else{
					matcher.usePattern( PATTERN_NODEBLOCK );
					if( matcher.find() ){
						++myCountNodeBlocks;
						flagInPotentialBlock = false;
					}
					else{
						matcher.usePattern( PATTERN_POTENTIALBLOCK );
						if( matcher.find() ){
							++myCountPotentialBlocks;
							flagInPotentialBlock = flagRealScan = true;
						}
					}
				}
			}
*/

	public long getCost(){
		return myCost;
	}

	public int getCountNodeBlocks(){
		return myCountNodeBlocks;
	}

	public int getCountPotentialBlocks(){
		return myCountPotentialBlocks;
	}

	public long getCountProbabilityValues(){
		return myCountProbabilityValues;
	}

	public int getProgressIncrementNodeBlock(){
		return myProgressIncrementNodeBlock;
	}

	public int getProgressIncrementPoteBlock(){
		return myProgressIncrementPoteBlock;
	}

	public int getProgressMax(){
		return myProgressMax;
	}

	/** interface Estimate */
	public int expectedNodes(){
		return getCountNodeBlocks();
	}

	/** interface Estimate */
	public int expectedEdges(){
		throw new UnsupportedOperationException();
	}

	/** interface Estimate */
	public long expectedValues(){
		return getCountProbabilityValues();
	}

	private void interpret(){
		float averageSizePotential = (float)(((double)myCountProbabilityValues) / ((double)myCountPotentialBlocks));

		float tn = 1;
		float tp = averageSizePotential / 64;//120;//120 was calculated for barley

		if( tp < 1 ){
			tn = 1/tp;
			tp = 1;
		}

		myProgressIncrementNodeBlock = (int) tn;
		myProgressIncrementPoteBlock = (int) tp;

		myProgressMax = (myCountNodeBlocks * myProgressIncrementNodeBlock) + (myCountPotentialBlocks * myProgressIncrementPoteBlock);
	}

	/*private int scanForProbabilityValues( String line ){
		int len = line.length();

		int     count    = 0;
		boolean inValue  = false;
		boolean isDigit  = false;
		boolean isPeriod = false;
		char at;
		for( int i=0; i<len; i++ ){
			at = line.charAt(i);
			if( (at == '%') || (at == '}') ) return count;

			isDigit  = isDigitISOLATIN1(at);

			if( inValue ){
				if( (!isDigit) && (!isNonDigitNumerical(at)) ) inValue = false;
			}
			else{
				if( isDigit ){
					inValue = true;
					++count;
				}
			}
		}

		myCountProbabilityValues += count;
		return count;
	}*/

	/** scan only the middle of the line, if it contains a digit
		assume is contains the same number of probability values
		as the last real scan */
	/*private int fakeScan( String line, int lastScan ){
		int len   = line.length();
		int start = len >> 2;
		int limit = len - start;
		char at;
		for( int i=start; i<limit; i++ ){
			at = line.charAt(i);
			if( isDigitISOLATIN1(at) ){
				myCountProbabilityValues += lastScan;
				return lastScan;
			}
			if( at == '%' ) return 0;
		}
		return 0;
	}*/

/* alternative implementation, too slow
	private int fakeScan( String line, int lastScan ){
		int len  = line.length();
		int head = Math.min( 8, len );

		int i;
		char at;
		for( i=0; i<head; i++ ){
			at = line.charAt(i);
			if( isDigitISOLATIN1(at) ){
				myCountProbabilityValues += lastScan;
				return lastScan;
			}
			if( at == '%' ) return 0;
		}

		int start = len >> 2;
		int limit = len - start;
		start = Math.max( head, start );

		for( i=start; i<limit; i++ ){
			at = line.charAt(i);
			if( isDigitISOLATIN1(at) ){
				myCountProbabilityValues += lastScan;
				return lastScan;
			}
			if( at == '%' ) return 0;
		}
		return 0;
	}
*/

	/*private boolean isDigitISOLATIN1( char at ){
		return (at >= '\u0030') && (at <= '\u0039');
	}*/

	private boolean isDigitISOLATIN1( int at ){
		return (at >= (int)'\u0030') && (at <= (int)'\u0039');
	}

	private boolean isWhitespace( int at ){
		return (at == (int)' ') || (at == (int)'\t');
	}

	/*private boolean isNonDigitNumerical( char at ){
		if( at == '.' ) return true;
		if( at == '-' ) return true;
		if( at == 'E' ) return true;
		if( at == 'e' ) return true;
		return false;
	}*/

	private boolean isNonDigitNumerical( int at ){
		if( at == (int)'.' ) return true;
		if( at == (int)'-' ) return true;
		if( at == (int)'E' ) return true;
		if( at == (int)'e' ) return true;
		return false;
	}

	private int  myCountNodeBlocks        = -1;
	private int  myCountPotentialBlocks   = -1;
	private long myCountProbabilityValues = -1;
	private long myCost                   = -1;

	private int myProgressIncrementNodeBlock = -1;
	private int myProgressIncrementPoteBlock = -1;
	private int myProgressMax                = -1;

	private File        myFile;
	private Reader      myReader;
	private InputStream myInputStream;
	private boolean     myFlagDoneEstimate = false;
}
