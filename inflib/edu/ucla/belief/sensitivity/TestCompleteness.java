package edu.ucla.belief.sensitivity;

import edu.ucla.belief.*;
import edu.ucla.belief.io.*;
import edu.ucla.belief.io.InstantiationXmlizer;
import edu.ucla.util.VariableStringifier;
import edu.ucla.util.DataSetStatistic;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.text.NumberFormat;
//{superfluous} import java.text.DateFormat;

/** @author Keith Cascio
	@since 20060208 */
public class TestCompleteness
{
	public static final int  INT_ITERATIONS_DEFAULT       = 1;
	public static final int  INT_MINIMUM_SIZE             = 3;
	public static final int  INT_LOOP_THRESHOLD           = 1024;
	public static final int  INT_SCARCITY_THRESHOLD       = 16;
	public static final int  INT_THRESHOLD_EXHAUSTED      = 16;
	public static final long LONG_DELAY_MS_SCAN_USER_STOP = 4096;

	public static final double DOUBLE_ZERO                     = (double)0;
	public static final double DOUBLE_ONE                      = (double)1;
	public static final double DOUBLE_EPSILON_TWEAK            = (double)0.00000001;
	public static final double DOUBLE_EPSILON_TOLERANCE        = (double)0.0000000001;//(double)0.000000000001;
	public static final double DOUBLE_EPSILON_FACTOR_SOUNDNESS = (double)0.0000005;

	public static final VariableStringifier STRINGIFIER = edu.ucla.util.AbstractStringifier.VARIABLE_ID;
	public static final NumberFormat        FORMAT      = new java.text.DecimalFormat( "0.00000000000000000000" );

	public static final String STR_OPT_PATH_NETWORK_FILE        = "-network";
	public static final String STR_OPT_ITERATIONS               = "-iterations";
	public static final String STR_OPT_PATH_EVIDENCE_FILE       = "-evidence";
	public static final String STR_OPT_TARGETPARAMETERTEST      = "-test";
	public static final String STR_OPT_CONSTRAINT               = "-challenge";
	public static final String STR_TEMPLATE_TARGETPARAMETERTEST = "variable_id[linear_index]=new value";
	public static final String STR_EXAMPLE_TARGETPARAMETERTEST  = "e.g. varA[12]=0.1287361283761119";
	public static final String STR_TEMPLATE_CONSTRAINT          = "variable_id[index]";
	public static final String STR_EXAMPLE_CONSTRAINT           = "e.g. varA[1]";

	public static final String  REGEX_FILENAME_USER_HALT    = "\\.?(die|halt|stop|cancel|quit|end|exit|cease|finish|kill|break|terminate|return)(\\.(txt|dat|bat|sh|exe|dll|gz|zip))?$";
	private static      Pattern PATTERN_FILENAME_USER_HALT  = null;
	public static final String  STR_FILENAME_STATUS         = "status.txt";

	public static final String  REGEX_TARGETPARAMETERTEST   = "(\\w+)\\[(\\d+)\\]=(\\d\\.?\\d*)";
	public static final Pattern PATTERN_TARGETPARAMETERTEST = Pattern.compile( REGEX_TARGETPARAMETERTEST );

	public static final String  REGEX_CONSTRAINT            = "(\\w+)\\[(\\d+)\\]";
	public static final Pattern PATTERN_CONSTRAINT          = Pattern.compile( REGEX_CONSTRAINT );

	public static final String  STR_OPT_SOUNDNESS           = "-soundness";
	public enum OptionSoundnessPrecision
	{
		strict{
			public boolean accept( Constraint constraint, SingleParamSuggestion suggestion, TestCompleteness testcompleteness, double value, double valueWas ){
				return constraint.satisfies( value );//no adjustment
			}

			public StringBuffer describeMiss( StringBuffer buff ){
				buff.append( name() );
				buff.append( " soundness miss (bare floating point arithmetic)" );
				return buff;
			}

			public String description(){
				return "Relaxed soundness mode OFF, i.e. consider suggestion sound only if it satisfies the constraint as tested by bare floating point arithmetic.";
			}

			public StringBuffer report( StringBuffer buff ){
				buff.append( name() );
				buff.append( " soundness (bare floating point arithmetic)" );
				return buff;
			}

			public PrintWriter status( PrintWriter out ){ return out; }

			public String synonymsAsRegex(){
				return SYNONYMS;
			}

			public final String SYNONYMS = name() + "|bare|float|arithmetic";
		},
		relaxed{
			public boolean accept( Constraint constraint, SingleParamSuggestion suggestion, TestCompleteness testcompleteness, double value, double valueWas ){
				epsilon = Math.abs( value - valueWas ) * DOUBLE_EPSILON_FACTOR_SOUNDNESS;
				if( constraint.opComparison == SensitivityEngine.OPERATOR_GTE ){
					adjusted = value+epsilon;
				}
				else if( constraint.opComparison == SensitivityEngine.OPERATOR_LTE ){
					adjusted = value-epsilon;
				}
				epsilonMin = Math.min( epsilonMin, epsilon );
				epsilonMax = Math.max( epsilonMax, epsilon );
				return constraint.satisfies( adjusted );
			}

			public StringBuffer describeMiss( StringBuffer buff ){
				buff.append( name() );
				buff.append( " soundness miss, epsilon: " );
				buff.append( FORMAT.format( epsilon ) );
				buff.append( ", adjusted value: " );
				buff.append( FORMAT.format( adjusted ) );
				return buff;
			}

			public String description(){
				return "Relaxed soundness mode ON, i.e. consider suggestion sound if it results in a value that is within epsilon of the constraint.";
			}

			public StringBuffer report( StringBuffer buff ){
				buff.append( name() );
				buff.append( " soundness, epsilon: [" );
				buff.append( epsilonMin );
				buff.append( ", " );
				buff.append( epsilonMax );
				buff.append( "]" );
				return buff;
			}

			public PrintWriter status( PrintWriter out ){
				out.print( name() );
				out.print( ", epsilon [" );
				out.print( epsilonMin );
				out.print( ", " );
				out.print( epsilonMax );
				out.print( "]" );
				return out;
			}

			public String synonymsAsRegex(){
				return SYNONYMS;
			}

			public final String SYNONYMS = name() + "|relax|relative";

			private double epsilon, adjusted;
			private double epsilonMin = 0, epsilonMax = 0;
		},
		find{
			public boolean accept( Constraint constraint, SingleParamSuggestion suggestion, TestCompleteness testcompleteness, double value, double valueWas ){
				double skew = -1;
				if( constraint.opComparison == SensitivityEngine.OPERATOR_GTE ){
					//adjusted = value+epsilon;
					//return value >= Constraint.this.c;
					skew = constraint.c - value;
				}
				else if( constraint.opComparison == SensitivityEngine.OPERATOR_LTE ){
					//adjusted = value-epsilon;
					//return value <= Constraint.this.c;
					skew = value - constraint.c;
				}

				//double delta = Math.abs( value - valueWas );
				double error = Math.max( skew, 0 );

				//myDeltas.add( Math.abs( value - valueWas ) );//delta );
				//myErrors.add( Math.max( skew, 0 ) );//error );

				myDeltas.addDataPoint( Math.abs( value - valueWas ) );
				myErrors.addDataPoint( error );

				if( myErrors.getMax() == error ){
					snapshotMaximumError.set( constraint, suggestion, testcompleteness.ec().evidence(), valueWas, value );
				}

				return true;
			}

			public StringBuffer describeMiss( StringBuffer buff ){
				return buff;//miss(ion) impossible
			}

			public String description(){
				return "Find soundness mode ON, i.e. characterize soundness rather than abruptly failing.";
			}

			public StringBuffer report( StringBuffer buff ){
				buff.append( "soundness statistics report:" );
				buff.append( "\n    " );
				myErrors.report( buff );
				buff.append( "\n    " );
				myDeltas.report( buff );
				buff.append( "\n    max error occurred at:\n" );
				snapshotMaximumError.report( buff );
				return buff;
			}

			public PrintWriter status( PrintWriter out ){
				out.print( name() );
				out.print( ", error [" );
				out.print( myErrors.getMin() );
				out.print( ", " );
				out.print( myErrors.getMax() );
				out.print( "]" );
				return out;
			}

			public String synonymsAsRegex(){
				return SYNONYMS;
			}

			public final String SYNONYMS = name() + "|error|finderror|characterize|statistics|stats|report";

			public void init(){
				myDeltas = new DataSetStatistic( "delta", 128 );
				myErrors = new DataSetStatistic( "error", 128 );
			}

			private DataSetStatistic myDeltas, myErrors;
			private SoundnessSnapshot snapshotMaximumError = new SoundnessSnapshot();
		};

		abstract public boolean accept( Constraint constraint, SingleParamSuggestion suggestion, TestCompleteness testcompleteness, double value, double valueWas );
		abstract public StringBuffer describeMiss( StringBuffer buff );
		abstract public String description();
		abstract public String synonymsAsRegex();
		abstract public StringBuffer report( StringBuffer buff );
		abstract public PrintWriter status( PrintWriter out );

		public void init(){}

		public static OptionSoundnessPrecision deflt(){
			return relaxed;
		}

		public Pattern synonymsAsPattern(){
			if( myPatternSynonyms == null ){
				myPatternSynonyms = Pattern.compile( synonymsAsRegex(), Pattern.CASE_INSENSITIVE );
			}
			return myPatternSynonyms;
		}

		//protected boolean result;
		private Pattern myPatternSynonyms;
	};

	public enum OptionTestSoundness {
		none{
			public int test( Constraint constraint, InferenceEngine ie, double valueBefore ){
				return 0;
			}

			public String description(){
				return "Soundness testing OFF.";
			}

			public String synonymsAsRegex(){
				return SYNONYMS;
			}

			public final String SYNONYMS = name() + "|off|no|skip|least";
		},
		answers{
			public int test( Constraint constraint, InferenceEngine ie, double valueBefore ) throws Exception{
				SingleParamSuggestion answer = constraint.getAnswer();
				if( answer == null ) errorMessage( "no answer" );
				constraint.testSoundness( answer, ie, valueBefore );
				return 1;
			}

			public String description(){
				return "Soundness testing ANSWERS - testing soundness of answers only.";
			}

			public String synonymsAsRegex(){
				return SYNONYMS;
			}

			public final String SYNONYMS = name() + "|hits|some";
		},
		all{
			public int test( Constraint constraint, InferenceEngine ie, double valueBefore ) throws Exception{
				List<SingleParamSuggestion> suggestions = constraint.getSuggestions();
				if( suggestions == null ) errorMessage( "no suggestions" );
				for( SingleParamSuggestion suggestion : suggestions ){
					constraint.testSoundness( suggestion, ie, valueBefore );
				}
				return suggestions.size();
			}

			public String description(){
				return "Soundness testing ALL - testing soundness of all suggestions.";
			}

			public String synonymsAsRegex(){
				return SYNONYMS;
			}

			public final String SYNONYMS = name() + "|on|yes|full|most|complete";
		};

		abstract public int test( Constraint constraint, InferenceEngine ie, double valueBefore ) throws Exception;
		abstract public String description();
		abstract public String synonymsAsRegex();

		public static OptionTestSoundness deflt(){
			return none;
		}

		public static Pattern patternValue(){
			if( PATTERNVALUE == null ){
				StringBuffer buff = new StringBuffer( 64 );
				for( OptionTestSoundness value : OptionTestSoundness.values() ){
					buff.append( value.synonymsAsRegex() );
					buff.append( '|' );
				}
				int indexDelimiter = buff.length()-1;
				String usage0 = buff.substring(0,indexDelimiter);
				for( OptionSoundnessPrecision precision : OptionSoundnessPrecision.values() ){
					buff.append( precision.synonymsAsRegex() );
					buff.append( '|' );
				}
				buff.setLength( buff.length()-1 );
				String usage3 = usage0 + "[," + buff.substring( indexDelimiter+1 ) + "]";
				String regexAllValues = buff.toString();
				buff.setLength(0);
				buff.append( "(" );
				buff.append( regexAllValues );
				buff.append( ")(?:,(" );
				buff.append( regexAllValues );
				buff.append( "))*" );
				PATTERNVALUE = Pattern.compile( buff.toString(), Pattern.CASE_INSENSITIVE );
			}
			return PATTERNVALUE;
		}

		public static boolean parse( String arg )
		{
			Matcher m = patternValue().matcher( arg );
			if( !m.matches() ) return false;

			String token;
			//System.out.println( m.groupCount() + " groups" );
			//int groupCeiling = m.groupCount() + 1;
			//for( int i=1; i<groupCeiling; i++ ){
			tokenizer:
			for( StringTokenizer toker = new StringTokenizer( arg, "," ); toker.hasMoreTokens(); ){
				token = toker.nextToken();//m.group(i);
				for( OptionTestSoundness option : OptionTestSoundness.values() ){
					if( option.synonymsAsPattern().matcher( token ).matches() ){
						TestCompleteness.SOUNDNESSOPTION = option;
						continue tokenizer;
					}
				}
				for( OptionSoundnessPrecision precision : OptionSoundnessPrecision.values() ){
					if( precision.synonymsAsPattern().matcher( token ).matches() ){
						TestCompleteness.PRECISIONSOUNDNESS = precision;
						continue tokenizer;
					}
				}
				errorMessage( "error parsing \"" +arg+ "\"" );
			}
			return true;
		}

		public static Pattern patternKey(){
			if( PATTERNKEY == null ){
				PATTERNKEY = Pattern.compile( REGEX_OPT_SOUNDNESS, Pattern.CASE_INSENSITIVE );
			}
			return PATTERNKEY;
		}

		public static String usage(){
			if( OptionTestSoundness.USAGE == null ){
				patternValue();
				StringBuffer buff = new StringBuffer( 64 );
				//buff.append( '[' );
				buff.append( STR_OPT_SOUNDNESS );
				buff.append( ' ' );
				for( OptionTestSoundness value : OptionTestSoundness.values() ){
					buff.append( value.name() );
					buff.append( '|' );
				}
				buff.setLength( buff.length()-1 );
				buff.append( "[," );
				for( OptionSoundnessPrecision precision : OptionSoundnessPrecision.values() ){
					buff.append( precision.name() );
					buff.append( '|' );
				}
				buff.setCharAt( buff.length()-1, ']' );
				//buff.append( USAGE_VALUE );
				//buff.append( ']' );
				OptionTestSoundness.USAGE = buff.toString();
			}
			return OptionTestSoundness.USAGE;
		}

		public static String meaning(){
			if( OptionTestSoundness.MEANING == null ){
				StringBuffer buff = new StringBuffer( 64 );
				buff.append( " (naked option means '" );
				buff.append( OptionTestSoundness.all );
				buff.append( "', no option means '" );
				buff.append( deflt() );
				buff.append( "', default soundness precision '" );
				buff.append( OptionSoundnessPrecision.deflt() );
				buff.append( "')" );
				OptionTestSoundness.MEANING = buff.toString();
			}
			return OptionTestSoundness.MEANING;
		}

		public Pattern synonymsAsPattern(){
			if( myPatternSynonyms == null ){
				myPatternSynonyms = Pattern.compile( synonymsAsRegex(), Pattern.CASE_INSENSITIVE );
			}
			return myPatternSynonyms;
		}

		private Pattern myPatternSynonyms;

		public static final String  REGEX_OPT_SOUNDNESS   = "-soundness|-sound|-correctness|-correct";
		private static Pattern PATTERNVALUE, PATTERNKEY;
		private static String USAGE, MEANING;
	};

	public enum OptionCompletenessPrecision
	{
		strict{
			public boolean accept( SingleParamSuggestion suggestion, double valueAfter, double testDelta ){
				epsilon = DOUBLE_EPSILON_TOLERANCE * Math.abs( testDelta );
				return result = suggestion.getInterval().epsilonContains( valueAfter, epsilon );
			}

			public StringBuffer describeMiss( StringBuffer buff ){
				buff.append( " epsilon: " );
				buff.append( FORMAT.format( epsilon ) );
				return buff;
			}

			public String description(){
				return "Relaxed completeness mode OFF, i.e. suggestion is considered equivalent only if it is within epsilon of the challenge.";
			}

			public String[] synonyms(){
				return SYNONYMS;
			}

			public String synonymsAsRegex(){
				return REGEX_SYNONYMS;
			}

			public final String REGEX_SYNONYMS = name();// + "|";
			private String[] SYNONYMS = new String[] { name() };

			private double epsilon;
		},
		relaxed{
			public boolean accept( SingleParamSuggestion suggestion, double valueAfter, double testDelta ){
				double suggestionDelta = suggestion.getSuggestedParameterValue() - suggestion.getTheta();
				boolean smaller = (Double.compare(suggestionDelta,DOUBLE_ZERO) == Double.compare(testDelta,DOUBLE_ZERO)) && (Math.abs(suggestionDelta) < Math.abs(testDelta));
				if( smaller ) return result = smaller;//i.e. same sign
				ratio = suggestionDelta/testDelta;
				return result = ratio < 9.9;//i.e. within a factor of 2
			}

			public StringBuffer describeMiss( StringBuffer buff ){
				buff.append( " ratio: " );
				buff.append( FORMAT.format( ratio ) );
				return buff;
			}

			public String description(){
				return "Relaxed completeness mode ON, i.e. suggestion is considered equivalent to the challenge if it is for the same condition, same sign, and is smaller or same order of magnitude.";
			}

			public String[] synonyms(){
				return SYNONYMS;
			}

			public String synonymsAsRegex(){
				return REGEX_SYNONYMS;
			}

			public final String REGEX_SYNONYMS = name() + "|relax";
			private String[] SYNONYMS = new String[] { name(), "relax" };
			private double ratio;
		};

		abstract public boolean accept( SingleParamSuggestion suggestion, double valueAfter, double testDelta );
		abstract public StringBuffer describeMiss( StringBuffer buff );
		abstract public String description();
		abstract public String synonymsAsRegex();
		abstract public String[] synonyms();

		public static OptionCompletenessPrecision deflt(){
			return strict;
		}

		public String getLiteral(){
			return "-" + OptionCompletenessPrecision.this.name();
		}

		public static OptionCompletenessPrecision valueOfLiteral( String literal ){
			//if( literal.equals( STR_OPT_RELAX ) ) return relaxed;

			if( !literal.startsWith( "-" ) ) return null;
			String token = literal.substring(1);

			OptionCompletenessPrecision ret;
			for( OptionCompletenessPrecision precision : OptionCompletenessPrecision.values() ){
				if( precision.synonymsAsPattern().matcher( token ).matches() ) return precision;
			}

			return null;
		}

		public static Pattern patternKey(){
			if( PATTERN == null ){
				StringBuffer buff = new StringBuffer( 64 );
				for( OptionCompletenessPrecision precision : OptionCompletenessPrecision.values() ){
					for( String synonym : precision.synonyms() ){
						buff.append( '-' );
						buff.append( synonym );
						buff.append( '|' );
					}
				}
				buff.setLength( buff.length()-1 );
				//buff.append( STR_OPT_RELAX );
				PATTERN = Pattern.compile( buff.toString(), Pattern.CASE_INSENSITIVE );
			}
			return PATTERN;
		}

		public static String usage(){
			if( OptionCompletenessPrecision.USAGE == null ){
				patternKey();
				StringBuffer buff = new StringBuffer( 64 );
				//buff.append( '[' );
				for( OptionCompletenessPrecision precision : OptionCompletenessPrecision.values() ){
					buff.append( '-' );
					buff.append( precision.name() );
					buff.append( '|' );
				}
				buff.setLength( buff.length()-1 );
				buff.append( " (default completeness precision '" );
				buff.append( deflt() );
				buff.append( "')" );
				//buff.append( ']' );
				OptionCompletenessPrecision.USAGE = buff.toString();
			}
			return OptionCompletenessPrecision.USAGE;
		}

		public Pattern synonymsAsPattern(){
			if( myPatternSynonyms == null ){
				myPatternSynonyms = Pattern.compile( synonymsAsRegex(), Pattern.CASE_INSENSITIVE );
			}
			return myPatternSynonyms;
		}

		//private static String REGEX;
		private static Pattern PATTERN;
		private static String USAGE;

		protected boolean result;
		private Pattern myPatternSynonyms;
	};

	public static String usage(){
		StringBuffer buff = new StringBuffer( 256 );
		buff.append( "usage: " );
		String nameClass = TestCompleteness.class.getName();
		buff.append( nameClass.substring( nameClass.lastIndexOf('.')+1 ) );
		buff.append( ' ' );
		buff.append( STR_OPT_PATH_NETWORK_FILE );
		buff.append( " <path to file> [OPTIONS] [REPLAY OPTIONS]" );
		buff.append( "\n\n    [OPTIONS]:\n" );
		buff.append( "\n      " );
		buff.append( OptionCompletenessPrecision.usage() );
		buff.append( "\n\n      " );
		buff.append( STR_OPT_ITERATIONS );
		buff.append( " <number> (default " );
		buff.append( INT_ITERATIONS_DEFAULT );
		buff.append( ")" );
		buff.append( "\n\n      " );
		buff.append( OptionTestSoundness.usage() );
		buff.append( "\n        " );
		buff.append( OptionTestSoundness.meaning() );
		buff.append( "\n\n\n    [REPLAY OPTIONS] (used together):\n" );
		buff.append( "\n      " );
		buff.append( STR_OPT_PATH_EVIDENCE_FILE );
		buff.append( " <path to file>" );
		buff.append( "\n\n      " );
		buff.append( STR_OPT_TARGETPARAMETERTEST );
		buff.append( " <" );
		buff.append( STR_TEMPLATE_TARGETPARAMETERTEST );
		buff.append( ">" );
		buff.append( "\n\n                      " );
		buff.append( STR_EXAMPLE_TARGETPARAMETERTEST );
		buff.append( "\n\n      " );
		buff.append( STR_OPT_CONSTRAINT );
		buff.append( " <" );
		buff.append( STR_TEMPLATE_CONSTRAINT );
		buff.append( ">" );
		buff.append( "\n\n                      " );
		buff.append( STR_EXAMPLE_CONSTRAINT );
		return buff.toString();
	}

	public static final String STR_USAGE                 = "usage: " + TestCompleteness.class.getName() + " " + STR_OPT_PATH_NETWORK_FILE + " <path to file> [" + OptionCompletenessPrecision.usage() + "] [" + STR_OPT_ITERATIONS + " <number> (default "+INT_ITERATIONS_DEFAULT+")] ["+STR_OPT_PATH_EVIDENCE_FILE+" <path to file> "+STR_OPT_TARGETPARAMETERTEST+" <"+STR_TEMPLATE_TARGETPARAMETERTEST + ", " + STR_EXAMPLE_TARGETPARAMETERTEST+">] ["+STR_OPT_CONSTRAINT+" <"+STR_TEMPLATE_CONSTRAINT + ", " + STR_EXAMPLE_CONSTRAINT+">] [" + OptionTestSoundness.usage() + OptionTestSoundness.meaning() + "]";

	private static final Throwable CAUSA_SUI = new Throwable();

	/** @since 20060221 */
	public static class SoundnessSnapshot{
		public SoundnessSnapshot(){}

		public SoundnessSnapshot( Constraint constraint, SingleParamSuggestion suggestion, Map<FiniteVariable,Object> evidence, double valueBefore, double valueAfter ){
			SoundnessSnapshot.this.set( constraint, suggestion, evidence, valueBefore, valueAfter );
		}

		public void set( Constraint constraint, SingleParamSuggestion suggestion, Map<FiniteVariable,Object> evidence, double valueBefore, double valueAfter ){
			SoundnessSnapshot.this.constraint  = constraint;
			SoundnessSnapshot.this.suggestion  = suggestion;
			SoundnessSnapshot.this.evidence    = evidence;
			SoundnessSnapshot.this.valueBefore = valueBefore;
			SoundnessSnapshot.this.valueAfter  = valueAfter;
		}

		public void report( StringBuffer buff ){
			buff.append( "      " );
			SoundnessSnapshot.this.constraint.append( buff, STRINGIFIER, FORMAT, /*flagAppendSuggestions*/ false );
			buff.append( "\n      suggestion " );
			SoundnessSnapshot.this.suggestion.append( buff, STRINGIFIER, FORMAT );
			buff.append( "\n      evidence {" );
			buff.append( STRINGIFIER.mapToString( SoundnessSnapshot.this.evidence ) );
			buff.append( "}\n      pr( " );
			SoundnessSnapshot.this.constraint.appendCommandLineOption( buff );
			buff.append( " ), " );
			buff.append( SoundnessSnapshot.this.valueBefore );
			buff.append( " -> " );
			buff.append( SoundnessSnapshot.this.valueAfter );
		}

		public Constraint constraint;
		public SingleParamSuggestion suggestion;
		public Map<FiniteVariable,Object> evidence;
		public double valueBefore, valueAfter;
	}

	/** @since 20060221 */
	public static String flatten( String[] args, String delimiter ){
		StringBuffer buff = new StringBuffer( args.length * (64 + delimiter.length()) );
		for( int i=0; i<args.length; i++ ){
			buff.append( args[i] );
			buff.append( delimiter );
		}
		buff.setLength( buff.length() - delimiter.length() );
		return buff.toString();
	}

	public static void main( String[] args ){
		if( scanUserStopped() ) return;

		if( FILE_STATUS.exists() ){
			System.err.println( "Please delete/move/rename runtime status file \"" +FILE_STATUS.getAbsolutePath()+ "\"" );
			return;
		}

		COMMAND_LINE = flatten( args, " " );

		File fileNetwork = null, fileEvidence = null;
		Integer intIterations = null;
		Matcher mTargetParameterTest = null;
		Matcher mConstraint = null;

		String msgerr = null;
		boolean flagPrintUsage = false;
		try{
			for( int i=0; i < args.length; i++ ){
				if( args[i].startsWith( STR_OPT_PATH_NETWORK_FILE ) ){
					String pathNetwork = args[i].substring( STR_OPT_PATH_NETWORK_FILE.length() );
					if( pathNetwork.length() < 1 ) pathNetwork = null;
					if( pathNetwork == null ){
						int ind = i + 1;
						if( ind < args.length ){
							pathNetwork = args[ind];
							i = ind;
						}
					}
					if( (pathNetwork == null) || (pathNetwork.length() < 1) ){
						flagPrintUsage = true;
						errorMessage( STR_OPT_PATH_NETWORK_FILE + " does not specify a file." );
					}
					else if( !(fileNetwork = new File(pathNetwork)).exists() ){
						errorMessage( "Network file \"" +pathNetwork+ "\" does not exist." );
					}
				}
				else if( args[i].startsWith( STR_OPT_ITERATIONS ) ){
					String strIterations = args[i].substring( STR_OPT_ITERATIONS.length() );
					if( strIterations.length() < 1 ) strIterations = null;
					if( strIterations == null ){
						int ind = i + 1;
						if( ind < args.length ){
							strIterations = args[ind];
							i = ind;
						}
					}
					if( (strIterations == null) || (strIterations.length() < 1) ){
						flagPrintUsage = true;
						errorMessage( STR_OPT_ITERATIONS + " requires a number." );
					}
					else if( (intIterations = new Integer(strIterations)).intValue() <= 0 ){
						errorMessage( "Iterations \"" +strIterations+ "\" is not valid." );
					}
				}
				else if( args[i].startsWith( STR_OPT_PATH_EVIDENCE_FILE ) ){
					String pathEvidence = args[i].substring( STR_OPT_PATH_EVIDENCE_FILE.length() );
					if( pathEvidence.length() < 1 ) pathEvidence = null;
					if( pathEvidence == null ){
						int ind = i + 1;
						if( ind < args.length ){
							pathEvidence = args[ind];
							i = ind;
						}
					}
					if( (pathEvidence == null) || (pathEvidence.length() < 1) ){
						flagPrintUsage = true;
						errorMessage( STR_OPT_PATH_EVIDENCE_FILE + " does not specify a file." );
					}
					else if( !(fileEvidence = new File(pathEvidence)).exists() ){
						errorMessage( "Evidence file \"" +pathEvidence+ "\" does not exist." );
					}
				}
				else if( args[i].startsWith( STR_OPT_TARGETPARAMETERTEST ) ){
					String strTest = args[i].substring( STR_OPT_TARGETPARAMETERTEST.length() );
					if( strTest.length() < 1 ) strTest = null;
					if( strTest == null ){
						int ind = i + 1;
						if( ind < args.length ){
							strTest = args[ind];
							i = ind;
						}
					}
					if( (strTest == null) || (strTest.length() < 1) ){
						flagPrintUsage = true;
						errorMessage( STR_OPT_TARGETPARAMETERTEST + " requires a value." );
					}
					if( !(mTargetParameterTest = PATTERN_TARGETPARAMETERTEST.matcher( strTest )).matches() ){
						errorMessage( "Test \"" +strTest+ "\" is not valid, must be of form "+STR_EXAMPLE_TARGETPARAMETERTEST+"." );
					}
				}
				else if( args[i].startsWith( STR_OPT_CONSTRAINT ) ){
					String strConstraint = args[i].substring( STR_OPT_CONSTRAINT.length() );
					if( strConstraint.length() < 1 ) strConstraint = null;
					if( strConstraint == null ){
						int ind = i + 1;
						if( ind < args.length ){
							strConstraint = args[ind];
							i = ind;
						}
					}
					if( (strConstraint == null) || (strConstraint.length() < 1) ){
						flagPrintUsage = true;
						errorMessage( STR_OPT_CONSTRAINT + " requires a value." );
					}
					if( !(mConstraint = PATTERN_CONSTRAINT.matcher( strConstraint )).matches() ){
						errorMessage( "Constraint \"" +strConstraint+ "\" is not valid, must be of form "+STR_EXAMPLE_CONSTRAINT+"." );
					}
				}
				else if( OptionCompletenessPrecision.patternKey().matcher( args[i] ).matches() ){//args[i].equals( STR_OPT_RELAX ) ){
					//TestCompleteness.FLAG_RELAX = true;
					TestCompleteness.PRECISIONCOMPLETENESS = OptionCompletenessPrecision.valueOfLiteral( args[i] );
				}
				else if( OptionTestSoundness.patternKey().matcher( args[i] ).matches() ){
					//TestCompleteness.FLAG_SOUNDNESS = true;
					TestCompleteness.SOUNDNESSOPTION = OptionTestSoundness.all;
					int ind = i + 1;
					if( ind < args.length ){
						//OptionTestSoundness option = OptionTestSoundness.valueOf( args[ind] );
						//if( option != null ){
						if( OptionTestSoundness.parse( args[ind] ) ){
							//TestCompleteness.SOUNDNESSOPTION = option;
							i = ind;
						}
					}
				}
			}

			if( fileNetwork == null ){
				flagPrintUsage = true;
				errorMessage( "Must specify network file using " + STR_OPT_PATH_NETWORK_FILE );
			}

			boolean flagRunRepeat = false;
			String idVariable = null, strLinearIndex = null, strTargetValueAfter = null;
			if( (mTargetParameterTest != null) || (fileEvidence != null) ){
				if( (mTargetParameterTest == null) || (fileEvidence == null) ){
					errorMessage( STR_OPT_PATH_EVIDENCE_FILE + " and " + STR_OPT_TARGETPARAMETERTEST + " must be used together." );
				}
				//mTargetParameterTest.reset();
				idVariable          = mTargetParameterTest.group(1);
				strLinearIndex      = mTargetParameterTest.group(2);
				strTargetValueAfter = mTargetParameterTest.group(3);
				flagRunRepeat = true;
			}

			String idVariableConstraint = null, strIndexConstraint = null;
			if( mConstraint != null ){
				if( !flagRunRepeat ){
					errorMessage( STR_OPT_CONSTRAINT + " must be used with "  + STR_OPT_PATH_EVIDENCE_FILE + " and " + STR_OPT_TARGETPARAMETERTEST );
				}

				idVariableConstraint = mConstraint.group(1);
				strIndexConstraint   = mConstraint.group(2);
			}

			System.out.println();
			System.out.println( TestCompleteness.PRECISIONCOMPLETENESS.description() );
			System.out.println();
			System.out.println( TestCompleteness.SOUNDNESSOPTION.description() );
			System.out.println();
			if( TestCompleteness.SOUNDNESSOPTION != OptionTestSoundness.none )
				System.out.println( TestCompleteness.PRECISIONSOUNDNESS.description() );
			System.out.println();

			TestCompleteness TEST = new TestCompleteness( NetworkIO.read( fileNetwork ) );
			TEST.myFileNetwork = fileNetwork;
			if( intIterations != null ) TEST.myIterations = intIterations.intValue();

			if( flagRunRepeat ) TEST.runOnce( fileEvidence, idVariable, strLinearIndex, strTargetValueAfter, idVariableConstraint, strIndexConstraint );
			else TEST.run();

			TEST.report();
		}catch( Exception exception ){
			fileNetwork = null;
			msgerr = exception.getMessage();
			if( msgerr == null ) msgerr = exception.toString();
			if( exception.getCause() != CAUSA_SUI ) exception.printStackTrace();
		}finally{
			if( msgerr != null ){
				System.err.println();
				System.err.println( msgerr );
				if( flagPrintUsage ){
					System.err.println();
					System.err.println( TestCompleteness.usage() );
				}
				System.exit(1);
			}
		}
	}

	public static void errorMessage( String msgerr ) throws RuntimeException{
		RuntimeException runtimeexception = new RuntimeException( msgerr, CAUSA_SUI );
		//runtimeexception.initCause( runtimeexception );//whoops java.lang.IllegalArgumentException: Self-causation not permitted
		throw runtimeexception;
	}

	private TestCompleteness( BeliefNetwork bn ){
		this.myBeliefNetwork = bn;

		this.init();
	}

	public void report(){
		String descrip = myFlagParametersExhausted ? "(parameters exhausted)" : "(done)";
		descrip = (TestCompleteness.PATH_USER_HALT == null) ? descrip : "(user halted)";

		System.out.println( "completeness test successful, finished " + myCurrentInteration + " iterations, tested " + mySetParametersTested.size() + " parameters " + descrip );
		System.out.println( "tested " + TestCompleteness.this.myNumSoundnessTests + " suggestions for soundness" );
		if( SOUNDNESSOPTION != OptionTestSoundness.none )
			System.out.println( PRECISIONSOUNDNESS.report( new StringBuffer(128) ).toString() );
	}

	/** @since 20060221 */
	public boolean userRequestedStatus(){
		if( !FILE_STATUS.exists() ) return false;

		PrintWriter out = null;
		try{
			out = new PrintWriter( new FileWriter( FILE_STATUS ) );

			out.print( "sensitivity completeness test status at " );
			out.println( DateFormatFilename.now() );
			out.print( "command line: " );
			out.println( COMMAND_LINE );
			out.println();

			out.print( "network: " );
			out.println( myFileNetwork.getAbsolutePath() );
			out.print( "current iteration: " );
			out.print( myCurrentInteration );
			out.print( " of " );
			out.println( myIterations );

			out.print( "soundness tests completed: " );
			out.println( myNumSoundnessTests );
			PRECISIONSOUNDNESS.status( out );

			System.out.println( "status written to file \"" + FILE_STATUS.getAbsolutePath() + "\""  );
		}catch( Exception exception ){
			System.err.println( "Warning: failed to write status to file \"" +FILE_STATUS.getAbsolutePath()+ "\"" );
		}finally{
			if( out != null ) out.close();
		}

		return true;
	}

	/** @since 20060217 */
	public boolean userStoppedLite(){
		long now = System.currentTimeMillis();

		boolean result = false;
		if( (now - myTimeLastScanForStopMillis) > LONG_DELAY_MS_SCAN_USER_STOP ){
			result = scanUserStopped();
			myTimeLastScanForStopMillis = now;
		}

		return result;
	}

	/** @since 20060217 */
	public static boolean scanUserStopped(){
		if( PATTERN_FILENAME_USER_HALT == null ) PATTERN_FILENAME_USER_HALT = Pattern.compile( REGEX_FILENAME_USER_HALT, Pattern.CASE_INSENSITIVE );

		TestCompleteness.PATH_USER_HALT = null;

		Matcher matcher = PATTERN_FILENAME_USER_HALT.matcher( "" );
		for( String path : new File(".").list() ){
			if( matcher.reset( path ).find() ){
				TestCompleteness.PATH_USER_HALT = path;
				System.out.println( "\nCompleteness test halted at "+DateFormatFilename.now()+" by presence of file \""+path+"\".\n" );
				return true;
			}
		}

		return false;
	}

	public void run() throws Exception{
		String date = DateFormatFilename.getInstance().format( new Date( System.currentTimeMillis() ) );
		System.out.println( "Completeness test started at " + DateFormatFilename.now() + ". To halt, touch (create) a file named \"die.txt\"." );//." + File.separator + "die.txt\".\n" );
		System.out.println( "To write status to a file, touch (create) a file named \"" +STR_FILENAME_STATUS+ "\"." );
		System.out.println( "\nOn each test, will write evidence to " + myFileEvidence.getAbsolutePath() );
		System.out.println();

		TestCompleteness.this.myCountScarcity = 0;
		TestCompleteness.this.mySetParametersTested.clear();
		TestCompleteness.this.myFlagParametersExhausted = false;
		TestCompleteness.this.myFlagPersistentScarcity  = false;
		TestCompleteness.this.myNumSoundnessTests = 0;
		for( myCurrentInteration=0; myCurrentInteration<myIterations; myCurrentInteration++ ){
			runOnce();
			//if( myFlagParametersExhausted || TestCompleteness.this.scanUserStopped() ) break;
			if( myFlagParametersExhausted || (TestCompleteness.PATH_USER_HALT != null) ) break;
		}
	}

	public void runOnce( File fileEvidence, String idVariable, String strLinearIndex, String strTargetValueAfter, String idVariableConstraint, String strIndexConstraint ) throws Exception{
		MAP_UTIL_EVIDENCE.clear();
		boolean resultLoad = myXmlizer.loadMap( MAP_UTIL_EVIDENCE, fileEvidence );
		if( !resultLoad ) errorMessage( "Failed to load evidence from file \"" + fileEvidence.getAbsolutePath() + "\"." );

		Map<FiniteVariable,Object> evidence = new HashMap<FiniteVariable,Object>( MAP_UTIL_EVIDENCE.size() );
		String id, strValue;
		FiniteVariable evidenceVariable;
		Object value;
		for( Object key : MAP_UTIL_EVIDENCE.keySet() ){
			id = (String) key;
			strValue = (String) MAP_UTIL_EVIDENCE.get( id );
			evidenceVariable = (FiniteVariable) myBeliefNetwork.forID( id );
			if( evidenceVariable == null ) errorMessage( "Network does not contain variable with id \"" + id + "\"." );
			value = evidenceVariable.instance( strValue );
			if( value == null ) errorMessage( "Variable \""+ id +"\" has no state named \"" + strValue + "\"." );
			evidence.put( evidenceVariable, value );
		}
		ec().resetEvidence();
		ec().setObservations( evidence );

		FiniteVariable targetVariable  = (FiniteVariable) myBeliefNetwork.forID( idVariable );
		if( targetVariable == null ) errorMessage( "Network does not contain variable with id \"" + idVariable + "\"." );
		CPTShell       targetShell     = targetVariable.getCPTShell( targetVariable.getDSLNodeType() );
		int            sizeCPT         = targetShell.index().size();
		int            index           = Integer.parseInt( strLinearIndex );
		if( (index < 0) || (sizeCPT <= index) ) errorMessage( "Linear index \"" +strLinearIndex+ "\" is invalid or out of range [0,"+Integer.toString( sizeCPT )+")." );
		CPTParameter   targetParameter = new CPTParameter( targetVariable, index );
		double         targetValueAfter = Double.parseDouble( strTargetValueAfter );

		FiniteVariable varConstraint = null;
		int indexConstraint = -1;
		if( idVariableConstraint != null ){
			varConstraint = (FiniteVariable) myBeliefNetwork.forID( idVariableConstraint );
			if( varConstraint == null ) errorMessage( "Network does not contain variable with id \"" + idVariableConstraint + "\"." );
			int      sizeVarConstraint = varConstraint.size();
			indexConstraint            = Integer.parseInt( strIndexConstraint );
			if( (indexConstraint < 0) || (sizeVarConstraint <= indexConstraint) ) errorMessage( "Index \"" +strIndexConstraint+ "\" is invalid or out of range [0,"+Integer.toString( sizeVarConstraint )+")." );
		}

		this.runOnce( evidence, targetParameter, targetValueAfter, varConstraint, indexConstraint );
	}

	public void runOnce() throws Exception{
		Map<FiniteVariable,Object> evidence = randomEvidence();
		myXmlizer.save( evidence, myFileEvidence );

		CPTParameter targetParameter   = pickParameter( evidence.keySet(), mySetParametersTested );
		if( targetParameter == null ){
			myFlagParametersExhausted = (myCountMisses++ > INT_THRESHOLD_EXHAUSTED);
			return;
		}

		double       targetValueBefore = targetParameter.getValue();
		double       targetValueAfter  = pickRandomNewValue( targetValueBefore );
		this.runOnce( evidence, targetParameter, targetValueAfter, null, -1 );
	}

	public void runOnce( Map<FiniteVariable,Object> evidence, CPTParameter targetParameter, double targetValueAfter, FiniteVariable varConstraint, int indexConstraint ) throws Exception{
		if( (targetValueAfter < DOUBLE_ZERO) || (DOUBLE_ONE < targetValueAfter) ) errorMessage( "Target value \"" +FORMAT.format(targetValueAfter)+ "\" is invalid or out of range [0,1]." );

		if( mySensitivityEngine != null ) mySensitivityEngine.resetEquations();

		System.out.println( "evidence "+myCurrentInteration+": {" + STRINGIFIER.mapToString( evidence ) + "}" );
		mySetParametersTested.add( targetParameter );

		//System.out.println( "target "+myCurrentInteration+": " + targetParameter.toString( STRINGIFIER ) );

		SET_UTIL_COMPLEMENT.clear();
		SET_UTIL_COMPLEMENT.addAll( myBeliefNetwork );
		SET_UTIL_COMPLEMENT.removeAll( evidence.keySet() );
		SET_UTIL_COMPLEMENT.remove( targetParameter.getVariable() );

		InferenceEngine ie = ie();

		MAP_UTIL_MARGINALS_BEFORE.clear();
		for( FiniteVariable var : SET_UTIL_COMPLEMENT ){
			MAP_UTIL_MARGINALS_BEFORE.put( var, ie.conditional( var ) );
		}

		System.out.println( "    pr(e) before " + ie.probability() );//FORMAT.format( ie.probability() ) );

		double targetValueBefore = targetParameter.getValue();
		//double targetValueAfter  = pickRandomNewValue( targetValueBefore );

		Test test = new Test( targetParameter, targetValueBefore, targetValueAfter );

		System.out.println( "test "+myCurrentInteration+": " + test.toString( STRINGIFIER, FORMAT ) );

		test.doChange();
		ie.setCPT( targetParameter.getVariable() );

		MAP_UTIL_MARGINALS_AFTER.clear();
		for( FiniteVariable var : SET_UTIL_COMPLEMENT ){
			MAP_UTIL_MARGINALS_AFTER.put( var, ie.conditional( var ) );
		}

		System.out.println( "    pr(e) after  " + ie.probability() );//FORMAT.format( ie.probability() ) );

		test.undoChange();
		ie.setCPT( targetParameter.getVariable() );

		System.out.println( "    pr(e) back   " + ie.probability() );//FORMAT.format( ie.probability() ) );

		if( varConstraint == null ){
			if( LIST_UTIL_COMPLEMENT == null ) LIST_UTIL_COMPLEMENT = new ArrayList<FiniteVariable>( SET_UTIL_COMPLEMENT.size() );
			else LIST_UTIL_COMPLEMENT.clear();
			LIST_UTIL_COMPLEMENT.addAll( SET_UTIL_COMPLEMENT );
			Collections.shuffle( LIST_UTIL_COMPLEMENT, myRandom );

			challengeAll( test, LIST_UTIL_COMPLEMENT, MAP_UTIL_MARGINALS_BEFORE, MAP_UTIL_MARGINALS_AFTER );
		}
		else challenge( test, varConstraint, indexConstraint, MAP_UTIL_MARGINALS_BEFORE.get( varConstraint ), MAP_UTIL_MARGINALS_AFTER.get( varConstraint ) );
	}

	private void challengeAll( Test test, Collection<FiniteVariable> vars, Map<FiniteVariable,Table> marginalsBefore, Map<FiniteVariable,Table> marginalsAfter ) throws Exception{
		Table conditionalBefore, conditionalAfter;
		int sizeVar;
		for( FiniteVariable var : vars ){
			sizeVar = var.size();

			conditionalBefore = marginalsBefore.get( var );
			conditionalAfter  = marginalsAfter .get( var );

			for( int i=0; i<sizeVar; i++ ){
				challenge( test, var, i, conditionalBefore, conditionalAfter );
				TestCompleteness.this.userRequestedStatus();
				if( TestCompleteness.this.userStoppedLite() ) return;
			}
		}
	}

	private Constraint challenge( Test test, FiniteVariable var, int indexConstraint, Table conditionalBefore, Table conditionalAfter ) throws Exception{
		double valueBefore = conditionalBefore.getCP(indexConstraint);
		double valueAfter  = conditionalAfter .getCP(indexConstraint);

		Constraint constraint = null;
		SingleParamSuggestion answer;
		if( Math.abs( valueAfter - valueBefore ) > DOUBLE_EPSILON_TOLERANCE ){
			constraint = new Constraint( var, indexConstraint, valueBefore, valueAfter );
			System.out.println( "challenge: " + constraint.toString( STRINGIFIER, FORMAT ) );
			answer = constraint.challenge( se(), test );

			if( answer == null ) testFailed( test, constraint, null );
			else System.out.println( "answer:    " + answer.toString( STRINGIFIER, FORMAT ) );

			TestCompleteness.this.CLUDGE = test;
			TestCompleteness.this.myNumSoundnessTests += SOUNDNESSOPTION.test( constraint, TestCompleteness.this.ie(), valueBefore );
		}

		return constraint;
	}

	private void testFailed( Test test, Constraint constraint, String header ){
		StringBuffer buff = new StringBuffer( 256 + constraint.estimateSizeStringBuffer() );
		buff.append( '.' );
		buff.append( File.separator );
		buff.append( NetworkIO.extractNetworkNameFromPath( TestCompleteness.this.myFileNetwork.getPath() ) );
		buff.append( ".failed." );
		DateFormatFilename.getInstance().format( new Date( System.currentTimeMillis() ),buff );
		buff.append( ".evidence.inst" );

		File fileEvidenceFailed = new File( buff.toString() );

		buff.setLength(0);
		try{
			if( header == null ){
				buff.append( "\nCompleteness test failed for:\n" );
				test.append( buff, STRINGIFIER, FORMAT );
				buff.append( "\n" );
				constraint.append( buff, STRINGIFIER, FORMAT );
			}
			else buff.append( header );

			buff.append( "\n\n" );
			buff.append( "repeat test using options:\n" );

			StringBuffer buffOptions = new StringBuffer( 256 );
			buffOptions.append( STR_OPT_PATH_NETWORK_FILE );
			buffOptions.append( " \"" );
			buffOptions.append( myFileNetwork.getCanonicalFile().getAbsolutePath() );
			buffOptions.append( "\" " );
			buffOptions.append( STR_OPT_PATH_EVIDENCE_FILE );
			buffOptions.append( " \"" );
			buffOptions.append( fileEvidenceFailed.getCanonicalFile().getAbsolutePath() );
			buffOptions.append( "\" " );
			buffOptions.append( STR_OPT_TARGETPARAMETERTEST );
			buffOptions.append( " \"" );
			test.appendCommandLineOption( buffOptions );
			buffOptions.append( "\" " );
			buffOptions.append( STR_OPT_CONSTRAINT );
			buffOptions.append( " \"" );
			constraint.appendCommandLineOption( buffOptions );
			buffOptions.append( "\" " );
			//if( TestCompleteness.FLAG_RELAX ) buffOptions.append( STR_OPT_RELAX );
			buffOptions.append( TestCompleteness.PRECISIONCOMPLETENESS.getLiteral() );
			buffOptions.append( ' ' );
			buffOptions.append( STR_OPT_SOUNDNESS );
			buffOptions.append( ' ' );
			buffOptions.append( TestCompleteness.SOUNDNESSOPTION );
			buffOptions.append( ',' );
			buffOptions.append( TestCompleteness.PRECISIONSOUNDNESS );
			String strOptions = buffOptions.toString();

			buff.append( strOptions );

			Map evidence = ec().evidence();
			myXmlizer.save( evidence, (Map)null, Collections.singletonMap( "options", strOptions ), fileEvidenceFailed );
		}catch( Exception exception ){
			System.err.println( "Warning: TestCompleteness.testFailed() caught " + exception );
		}
		errorMessage( buff.toString() );
	}

	public static class DateFormatFilename{
		private static DateFormatFilename INSTANCE;
		private DateFormatFilename(){}
		public static DateFormatFilename getInstance(){
			if( INSTANCE == null ) INSTANCE = new DateFormatFilename();
			return INSTANCE;
		}

		public static String now(){
			return getInstance().format( new Date( System.currentTimeMillis() ) );
		}

		public String format( Date date ){
			myBuffer.setLength(0);
			return format( date, myBuffer ).toString();
		}

		public StringBuffer format( Date date, StringBuffer toAppendTo ){
			Calendar calendar = Calendar.getInstance();
			calendar.setTime( date );

			int year   = calendar.get( Calendar.YEAR );
			int month  = calendar.get( Calendar.MONTH );
			int day    = calendar.get( Calendar.DAY_OF_MONTH );
			int hour24 = calendar.get( Calendar.HOUR_OF_DAY );
			int minute = calendar.get( Calendar.MINUTE );
			int second = calendar.get( Calendar.SECOND );
			int millis = calendar.get( Calendar.MILLISECOND );

			toAppendTo.append( myNumberFormat.format( year ) );
			toAppendTo.append( myNumberFormat.format( month+1 ) );
			toAppendTo.append( myNumberFormat.format( day ) );
			toAppendTo.append( '_' );
			toAppendTo.append( myNumberFormat.format( hour24 ) );
			toAppendTo.append( myNumberFormat.format( minute ) );
			toAppendTo.append( myNumberFormat.format( second ) );
			toAppendTo.append( myNumberFormat.format( millis ) );

			return toAppendTo;
		}

		private StringBuffer myBuffer = new StringBuffer( 32 );
		private NumberFormat myNumberFormat = new java.text.DecimalFormat( "##00" );
	}

	public class Constraint{
		public Constraint( FiniteVariable var, int index, double valueBefore, double valueAfter ){
			Constraint.this.varY        = var;
			Constraint.this.index       = index;
			Constraint.this.valueBefore = valueBefore;
			Constraint.this.valueAfter  = valueAfter;

			Constraint.this.init();
		}

		private void init(){
			Constraint.this.valueY = Constraint.this.varY.instance( Constraint.this.index );

			Constraint.this.c = valueAfter;

			if( Constraint.this.valueAfter > Constraint.this.valueBefore ) Constraint.this.opComparison = SensitivityEngine.OPERATOR_GTE;
			else Constraint.this.opComparison = SensitivityEngine.OPERATOR_LTE;
		}

		public SingleParamSuggestion challenge( SensitivityEngine se, Test test ){
			Constraint.this.sr = se.getResults( Constraint.this.varY, Constraint.this.valueY, /*varZ*/null, /*valueZ*/null, /*opArithmetic*/null, Constraint.this.opComparison, Constraint.this.c, /*flagSingleParameter*/true, /*flagSingleCPT*/false );
			Constraint.this.suggestions = Constraint.this.sr.generateSingleParamSuggestions();

			for( SingleParamSuggestion suggestion : Constraint.this.suggestions ){
				if( test.equivales( suggestion ) ) return Constraint.this.answer = suggestion;
			}

			return null;
		}

		public String toString( VariableStringifier stringifier, java.text.NumberFormat format ){
			return append( new StringBuffer( estimateSizeStringBuffer() ), stringifier, format ).toString();
		}

		public StringBuffer append( StringBuffer buff, VariableStringifier stringifier, java.text.NumberFormat format ){
			return append( buff, stringifier, format, true );
		}

		public StringBuffer append( StringBuffer buff, VariableStringifier stringifier, java.text.NumberFormat format, boolean flagAppendSuggestions ){
			buff.append( "constraint " );
			Constraint.this.appendCommandLineOption( buff );
			buff.append( " p(" );
			buff.append( stringifier.variableToString( varY ) );
			buff.append( " == " );
			buff.append( valueY );
			buff.append( ") " );
			buff.append( opComparison );
			buff.append( ' ' );
			buff.append( c );
			if( flagAppendSuggestions && (Constraint.this.suggestions != null) ){
				buff.append( '\n' );
				buff.append( Constraint.this.suggestions.size() );
				buff.append( " suggestions:" );
				for( SingleParamSuggestion suggestion : Constraint.this.suggestions ){
					buff.append( '\n' );
					suggestion.append( buff, stringifier, format );
				}
			}
			return buff;
		}

		public StringBuffer appendCommandLineOption( StringBuffer buff ){
			buff.append( varY.getID() );
			buff.append( '[' );
			buff.append( Constraint.this.index );
			buff.append( ']' );
			return buff;
		}

		public int estimateSizeStringBuffer(){
			int size = 128;
			if( Constraint.this.suggestions != null ) size += (Constraint.this.suggestions.size() * 128);
			return size;
		}

		public List<SingleParamSuggestion> getSuggestions(){
			return Constraint.this.suggestions;
		}

		public SingleParamSuggestion getAnswer(){
			return Constraint.this.answer;
		}

		public void testSoundness( SingleParamSuggestion suggestion, InferenceEngine ie, double valueWas ) throws Exception{
			suggestion.adoptChange();
			ie.setCPT( suggestion.getVariable() );

			Table conditional = ie.conditional( Constraint.this.varY );
			double value = conditional.getCP( Constraint.this.index );

			boolean success = PRECISIONSOUNDNESS.accept( Constraint.this, suggestion, TestCompleteness.this, value, valueWas );
			if( !success ){
				StringBuffer buff = new StringBuffer( 128 );
				buff.append( "\nsoundness test failed for:\nsuggestion: " );
				suggestion.append( buff, STRINGIFIER, FORMAT );
				buff.append( '\n' );
				Constraint.this.append( buff, STRINGIFIER, FORMAT, false );
				buff.append( "\nvalue found: " );
				buff.append( FORMAT.format( value ) );
				buff.append( ' ' );
				PRECISIONSOUNDNESS.describeMiss( buff );

				testFailed( TestCompleteness.this.CLUDGE, Constraint.this, buff.toString() );
			}

			suggestion.undo();
			ie.setCPT( suggestion.getVariable() );
		}

		private boolean satisfies( double value ){
			if( Constraint.this.opComparison == SensitivityEngine.OPERATOR_GTE ){
				return value >= Constraint.this.c;
			}
			else if( Constraint.this.opComparison == SensitivityEngine.OPERATOR_LTE ){
				return value <= Constraint.this.c;
			}
			else throw new IllegalStateException( "unknown opComparison " + opComparison );
		}

		private FiniteVariable varY;
		private int index;
		private double valueBefore, valueAfter, c;
		private Object valueY;
		private Object opComparison;
		private SensitivityReport sr;
		private List<SingleParamSuggestion> suggestions;
		private SingleParamSuggestion answer;
	}

	public static class Test{
		public Test( CPTParameter targetParameter, double targetValueBefore, double targetValueAfter ){
			Test.this.targetParameter   = targetParameter;
			Test.this.targetValueBefore = targetValueBefore;
			Test.this.targetValueAfter  = targetValueAfter;

			Test.this.init();
		}

		private void init(){
			Test.this.fVar           = Test.this.targetParameter.getVariable();
			Test.this.sizeVar        = Test.this.fVar.size();
			Test.this.indexParameter = Test.this.targetParameter.getLinearIndex();
			Test.this.indexCondition = Test.this.targetParameter.getIndexOfCondition();
			Test.this.shell          = Test.this.fVar.getCPTShell();
			Test.this.delta          = Test.this.targetValueAfter - Test.this.targetValueBefore;
			//Test.this.deltaInv     = - Test.this.delta;
			Test.this.invBefore      = DOUBLE_ONE - Test.this.targetValueBefore;
			Test.this.invAfter       = DOUBLE_ONE - Test.this.targetValueAfter;
			Test.this.scale          = Test.this.invAfter / Test.this.invBefore;

			Test.this.valuesBefore   = new double[ sizeVar ];
			Test.this.valuesAfter    = new double[ sizeVar ];

			int curr = Test.this.indexCondition;
			double valBefore, valAfter;
			for( int i=0; i<sizeVar; i++ ){
				valBefore = shell.getCP( curr );

				if( curr == indexParameter ) valAfter = targetValueAfter;
				else                         valAfter = valBefore * scale;

				valuesBefore[i] = valBefore;
				valuesAfter [i] = valAfter;

				++curr;
			}
		}

		public FiniteVariable getVariable(){
			return Test.this.fVar;
		}

		public void doChange(){
			if( !(Test.this.shell instanceof TableShell) ) errorMessage( "This test is designed to work only with CPT-style probability representation at this time." );

			Test.this.cpt = Test.this.shell.getCPT();

			int curr = Test.this.indexCondition;
			for( int i=0; i<sizeVar; i++ ){
				cpt.setCP( curr++, valuesAfter[i] );
			}
		}

		public void undoChange(){
			int curr = Test.this.indexCondition;
			for( int i=0; i<sizeVar; i++ ){
				cpt.setCP( curr++, valuesBefore[i] );
			}
		}

		public boolean equivales( SingleParamSuggestion suggestion ){
			if( suggestion.getVariable() != Test.this.fVar ) return false;

			CPTParameter suggestionParam     = suggestion.getCPTParameter();
			int          suggestionCondition = suggestionParam.getIndexOfCondition();
			if( suggestionCondition != Test.this.indexCondition ) return false;

			int offsetSuggestion = suggestionParam.getLinearIndex() - suggestionCondition;
			double testDelta       = valuesAfter[ offsetSuggestion ]         - valuesBefore[ offsetSuggestion ];

			boolean result = PRECISIONCOMPLETENESS.accept( suggestion, valuesAfter[ offsetSuggestion ], testDelta );
			if( result ) return true;
			else{
				StringBuffer buff = new StringBuffer( 256 );
				buff.append( "\nnear miss:\ntest: " );
				Test.this.append( buff, STRINGIFIER, FORMAT );
				buff.append( "\nsuggestion: " );
				suggestion.append( buff, STRINGIFIER, FORMAT );
				PRECISIONCOMPLETENESS.describeMiss( buff );
				System.out.println( buff.toString() );
				return false;
			}
		}

		public String toString( VariableStringifier stringifier, java.text.NumberFormat format ){
			return append( new StringBuffer( 256 ), stringifier, format ).toString();
		}

		public StringBuffer append( StringBuffer buff, VariableStringifier stringifier, java.text.NumberFormat format ){
			//buff.append( "test " );
			buff.append( fVar.getID() );
			buff.append( '[' );
			buff.append( Test.this.indexParameter );
			buff.append( "] " );
			targetParameter.append( buff, stringifier );
			/*buff.append( ", " );
			buff.append( format.format( targetValueBefore ) );
			buff.append( " -> " );
			buff.append( format.format( targetValueAfter ) );*/
			buff.append( "\n" );

			int offset = Test.this.indexParameter - Test.this.indexCondition;
			int width = maxLengthInstanceDisplay( fVar ) + 1;
			for( int i=0; i<valuesBefore.length; i++ ){
				appendFixedWidth( fVar.instance(i), width, buff );
				buff.append( format.format( valuesBefore[i] ) );
				buff.append( " -> " );
				buff.append( format.format( valuesAfter [i] ) );
				if( i == offset ) buff.append( " ***" );
				buff.append( "\n" );
			}
			return buff;
		}

		public StringBuffer appendCommandLineOption( StringBuffer buff ){
			buff.append( fVar.getID() );
			buff.append( '[' );
			buff.append( Test.this.indexParameter );
			buff.append( "]=" );
			buff.append( FORMAT.format( targetValueAfter ) );
			return buff;
		}

		private CPTParameter targetParameter;
		private FiniteVariable fVar;
		private CPTShell shell;
		private Table cpt;
		private double targetValueBefore, targetValueAfter, delta, scale, invBefore, invAfter;//, deltaInv;
		private int indexParameter, indexCondition, sizeVar;

		private double[] valuesBefore, valuesAfter;
	}

	public static int maxLengthInstanceDisplay( FiniteVariable var ){
		int max = 0;
		for( Object instance : var.instances() ){
			max = Math.max( max, instance.toString().length() );
		}
		return max;
	}

	public static StringBuffer appendFixedWidth( Object obj, int width, StringBuffer buff ){
		String display = obj.toString();
		buff.append( display );
		int remainder = width - display.length();
		for( int i=0; i<remainder; i++ ) buff.append( ' ' );
		return buff;
	}

	private void init(){
		this.mySize = myBeliefNetwork.size();

		if( mySize < INT_MINIMUM_SIZE ) errorMessage( "Network must contain at least " + INT_MINIMUM_SIZE + " variables." );

		this.mySizeHalf = (mySize / 2) + 1;
		this.myArray = (FiniteVariable[]) myBeliefNetwork.topologicalOrder().toArray( new FiniteVariable[ mySize ] );

		this.mySetParametersTested = new HashSet<CPTParameter>( INT_ITERATIONS_DEFAULT );

		this.myXmlizer = new InstantiationXmlizer();
		this.myFileEvidence = new File( "." + File.separator + "completeness_test_evidence.inst" );

		PRECISIONSOUNDNESS.init();
	}

	private Map<FiniteVariable,Object> randomEvidence() throws Exception{
		ec().resetEvidence();

		int numAssertions = myRandom.nextInt( mySizeHalf );

		MAP_UTIL_EVIDENCE.clear();
		FiniteVariable var;
		Object value;
		for( int i=0; i<numAssertions; i++ ){
			var = pickRandomVariable( MAP_UTIL_EVIDENCE.keySet() );
			value = pickRandomValue( var );
			MAP_UTIL_EVIDENCE.put( var, value );
		}

		ec().setObservations( MAP_UTIL_EVIDENCE );

		return MAP_UTIL_EVIDENCE;
	}

	private double pickRandomNewValue( double before ){
		double rotated = -1;
		int i = 0;
		do{
			rotated = before + myRandom.nextDouble();
			if( rotated > DOUBLE_ONE ) rotated -= DOUBLE_ONE;
			if( i++ > INT_LOOP_THRESHOLD ) errorMessage( "Failed to pick new value." );
		}while( Math.abs( rotated - before ) < DOUBLE_EPSILON_TWEAK );

		return rotated;
	}

	private CPTParameter pickParameter( Set<FiniteVariable> excludedVariables, Set<CPTParameter> excludedParameters ){
		if( myFlagPersistentScarcity ) return pickFirstUntestedParameter( excludedVariables, excludedParameters );

		boolean        scarce = false;
		CPTParameter   targetParameter = null;
		FiniteVariable targetVariable;
		CPTShell       targetShell;

		int counterOuter = 0, stopOuter = ((myArray.length - excludedVariables.size()) * 3);
		while( targetParameter == null ){
			if( counterOuter++ > stopOuter ){
				targetParameter = null;
				scarce = true;
				break;
			}

			targetVariable = pickRandomVariable( excludedVariables );
			targetShell = targetVariable.getCPTShell( targetVariable.getDSLNodeType() );

			int sizeCPT = targetShell.index().size();
			int counterInner=0, stopInner = sizeCPT * 2;
			do{
				targetParameter = new CPTParameter( targetVariable, myRandom.nextInt( sizeCPT ) );
				if( counterInner++ > stopInner ){
					targetParameter = null;
					scarce = true;
					break;
				}
			}while( excludedParameters.contains( targetParameter ) );
		}

		if( scarce ){
			myFlagPersistentScarcity = ( ++myCountScarcity > INT_SCARCITY_THRESHOLD );
			if( myFlagPersistentScarcity ) System.out.println( "    /* persistent scarcity */" );
			targetParameter = pickFirstUntestedParameter( excludedVariables, excludedParameters );
			//errorMessage( "Failed to pick parameter not contained in set of " + excludedParameters.size() + " exclusions." );
		}

		return targetParameter;
	}

	private CPTParameter pickFirstUntestedParameter( Set<FiniteVariable> excludedVariables, Set<CPTParameter> excludedParameters ){
		//System.out.println( " /* pickFirstUntestedParameter() */" );
		CPTParameter   targetParameter = null;
		CPTShell       targetShell;
		int sizeCPT;
		for( FiniteVariable targetVariable : myArray ){
			if( excludedVariables.contains( targetVariable ) ) continue;
			targetShell = targetVariable.getCPTShell( targetVariable.getDSLNodeType() );
			sizeCPT = targetShell.index().size();
			for( int j=0; j<sizeCPT; j++ ){
				targetParameter = new CPTParameter( targetVariable, j );
				if( !excludedParameters.contains( targetParameter ) ) return targetParameter;
			}
		}
		return null;
	}

	private FiniteVariable pickRandomVariable( Set<FiniteVariable> exclusions ){
		FiniteVariable ret = null;
		int i=0;
		while( exclusions.contains( ret = myArray[ myRandom.nextInt( mySize ) ]  ) ){
			if( i++ > INT_LOOP_THRESHOLD ) errorMessage( "Failed to pick random variable not contained in set of " + exclusions.size() + " exclusions." );
		}
		return ret;
	}

	private Object pickRandomValue( FiniteVariable var ){
		return var.instance( myRandom.nextInt( var.size() ) );
	}

	private EvidenceController ec(){
		return myBeliefNetwork.getEvidenceController();
	}

	private InferenceEngine ie(){
		if( myInferenceEngine == null ){
			myInferenceEngine = new edu.ucla.belief.inference.SSEngineGenerator().manufactureInferenceEngine( myBeliefNetwork );
			Set evidence = ec().evidence().keySet();
			if( !evidence.isEmpty() ){
				myInferenceEngine.evidenceChanged( new EvidenceChangeEvent( evidence ) );
			}
		}
		return myInferenceEngine;
	}

	private PartialDerivativeEngine pde(){
		if( myPartialDerivativeEngine == null ){
			if( myInferenceEngine instanceof PartialDerivativeEngine ) return (PartialDerivativeEngine) myInferenceEngine;
			else errorMessage( "Cannot create PartialDerivativeEngine" );
		}
		return myPartialDerivativeEngine;
	}

	private SensitivityEngine se(){
		if( mySensitivityEngine == null ){
			mySensitivityEngine = new SensitivityEngine( myBeliefNetwork, ie(), pde() );
		}
		return mySensitivityEngine;
	}

	private File myFileNetwork;
	private BeliefNetwork myBeliefNetwork;
	private FiniteVariable[] myArray;
	private InferenceEngine myInferenceEngine;
	private PartialDerivativeEngine myPartialDerivativeEngine;
	private SensitivityEngine mySensitivityEngine;
	private int mySize = -1, mySizeHalf = -1;
	private Random myRandom = new Random();
	private int myIterations = INT_ITERATIONS_DEFAULT, myCurrentInteration = -1;
	private InstantiationXmlizer myXmlizer;
	private File myFileEvidence;
	private static File FILE_STATUS = new File( "./" + STR_FILENAME_STATUS );
	private boolean myFlagPersistentScarcity, myFlagParametersExhausted;
	private int myCountScarcity = 0, myCountMisses = 0, myNumSoundnessTests = 0;
	//private static boolean FLAG_RELAX     = false;
	//private static boolean FLAG_SOUNDNESS = false;
	private static OptionCompletenessPrecision PRECISIONCOMPLETENESS = OptionCompletenessPrecision.deflt();
	private static OptionSoundnessPrecision    PRECISIONSOUNDNESS    = OptionSoundnessPrecision   .deflt();
	private static OptionTestSoundness         SOUNDNESSOPTION       = OptionTestSoundness        .deflt();
	private Test CLUDGE;
	private static String PATH_USER_HALT;
	private long myTimeLastScanForStopMillis = 0;
	private static String COMMAND_LINE;

	private static final Set <FiniteVariable>        SET_UTIL_COMPLEMENT       = new HashSet<FiniteVariable>();
	private static       List<FiniteVariable>        LIST_UTIL_COMPLEMENT;
	private static final Map <FiniteVariable,Object> MAP_UTIL_EVIDENCE         = new HashMap<FiniteVariable,Object>();
	private static final Map <FiniteVariable,Table>  MAP_UTIL_MARGINALS_BEFORE = new HashMap<FiniteVariable,Table>();
	private static final Map <FiniteVariable,Table>  MAP_UTIL_MARGINALS_AFTER  = new HashMap<FiniteVariable,Table>();

	private Set<CPTParameter> mySetParametersTested;
}
