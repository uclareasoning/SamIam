package edu.ucla.belief.learn;

import edu.ucla.belief.inference.SSEngineGenerator;
import edu.ucla.belief.inference.RCEngineGenerator;
import edu.ucla.belief.io.NetworkIO;
import edu.ucla.belief.*;

import java.util.*;
import java.io.*;

/**
	@author Keith Cascio
	@since 110603
*/
public class LearningData implements Map
{
	public static final boolean FLAG_REDUCE_CASES = true;

	public String valuesToString()
	{
		Iterator it = myListVariables.iterator();
		String ret = LearningDataOld.canonical( get( it.next() ) );
		while( it.hasNext() )
		{
			ret += "," + LearningDataOld.canonical( get( it.next() ) );
		}
		return ret;
	}

	public FiniteVariable getDebugVariable()
	{
		return (FiniteVariable) myListVariables.get(1);
	}

	public double getCurrentWeight()
	{
		return getWeight( myIndexCurrentRecord );
	}

	public double getWeight( int indexRecord )
	{
		if( myWeights == null ) return Table.ONE;
		else return myWeights[ indexRecord ];
	}

	public int getCurrentRecord()
	{
		return myIndexCurrentRecord;
	}

	public void setCurrentRecord( int index )
	{
		myIndexCurrentRecord = index;
	}

	public int size()
	{
		return myNumRecords;
	}

	public boolean isEmpty()
	{
		return myMapVariablesToColumns.isEmpty();
	}

	public boolean containsKey(Object key)
	{
		return myMapVariablesToColumns.containsKey( key );
	}

	public boolean containsValue(Object value)
	{
		throw new UnsupportedOperationException();
	}

	public Object get(Object key)
	{
		Object[] array = (Object[]) myMapVariablesToColumns.get( key );
		if( array == null ) return null;
		else return array[ myIndexCurrentRecord ];
	}

	public Object put(Object key,Object value)
	{
		throw new UnsupportedOperationException();
	}

	public Object remove(Object key)
	{
		Object ret = get( key );
		myMapVariablesToColumns.remove( key );
		return ret;
	}

	public void putAll(Map t)
	{
		throw new UnsupportedOperationException();
	}

	public void clear()
	{
		myMapVariablesToColumns.clear();
	}

	public Set keySet()
	{
		return myMapVariablesToColumns.keySet();
	}

	public Collection values()
	{
		throw new UnsupportedOperationException();
	}

	public Set entrySet()
	{
		return myMapVariablesToColumns.entrySet();
	}

	public boolean equals(Object o)
	{
		return myMapVariablesToColumns.equals( o );
	}

	public int hashCode()
	{
		return myMapVariablesToColumns.hashCode();
	}

	public void debugPrint( PrintWriter stream )
	{
		stream.println( "LearningData.debugPrint()" );
		stream.println( myListVariables );
		int numVariables = myListVariables.size();
		for( int i=0; i<myNumRecords; i++ )
		{
			debugPrint( myArrayColumns[0][i], stream );
			for( int j=1; j<numVariables; j++ )
			{
				stream.print( "," );
				debugPrint( myArrayColumns[j][i], stream );
			}
			stream.println();
		}
		stream.flush();
	}

	public void debugPrint( Object o, PrintWriter stream )
	{
		stream.print( (o==null) ? "N/A" : o );
	}

	/**
		@author Keith Cascio
		@since 111903
	*/
	public LearningData( List vars )
	{
		this();
		myListVariables = new LinkedList( vars );
		mySetVariables = new HashSet( vars );
	}

	/**
		@author Keith Cascio
		@since 111903
	*/
	public LearningData()
	{
		myMapVariablesToColumns = new HashMap();
	}

	/**
	 * Reads data from a Hugin-style plain text "case" file.
	 * The variables listed in the file must contain all of the variables
	 * in LearningData.  Erroneous records are ignored.
	 */
	public void readData( File infile, BeliefNetwork bn ) throws IOException
	{
		BufferedReader br = new BufferedReader( new FileReader( infile ) );
		int numLines = (int)0;
		while( br.readLine() != null ) ++numLines;
		myNumRecords = numLines - 1;

		readData( new BufferedReader( new FileReader( infile ) ), bn );
	}

	public void readData( BufferedReader in, BeliefNetwork bn ) throws IOException
	{
		myListVariables = new LinkedList();
		mySetVariables = new HashSet(bn);

		String strVars = in.readLine();
		if( strVars == null ) return;

		if( FLAG_REDUCE_CASES )
		{
			//System.out.println( "LearningData.readData() reducing records" );

			int numRecordsUnreduced = myNumRecords;
			HashMap map = new HashMap( myNumRecords/2 );
			String tempString;
			Struct tempStruct;

			for( int i=0; (tempString = in.readLine()) != null; i++ )
			{
				if( map.containsKey( tempString ) )
				{
					tempStruct = (Struct) map.get( tempString );
					++tempStruct.weight;
				}
				else
				{
					tempStruct = new Struct( tempString, i );
					map.put( tempString, tempStruct );
				}
			}

			Collection values = map.values();
			List structs = new ArrayList( values );
			Collections.sort( structs );
			myNumRecords = structs.size();
			readVars( strVars, bn );
			myWeights = new double[ myNumRecords ];
			int i=(int)0;
			double maxWeight = Table.ONE;
			for( Iterator it = structs.iterator(); it.hasNext(); i++ )
			{
				tempStruct = (Struct)it.next();
				readRecord( tempStruct.string, i );
				myWeights[i] = tempStruct.weight;
				maxWeight = Math.max( maxWeight,  tempStruct.weight );
			}

			//System.out.println( "\trecords reduced from " + numRecordsUnreduced + " to " + myNumRecords + ", max weight " + maxWeight );
		}
		else
		{
			readVars( strVars, bn );
			for( int i=0; i<myNumRecords; i++ ) readRecord( in.readLine(), i );
		}

		in.close();

		myArrayColumns = null;
		//debugPrint( new PrintWriter( new FileOutputStream( "learningData2.dat" ) ) );
	}

	public class Struct implements Comparable
	{
		public Struct( String string, int indexFirstOccurrence )
		{
			this.string = string;
			this.indexFirstOccurrence = indexFirstOccurrence;
		}

		public double weight = Table.ONE;
		public String string;
		public int indexFirstOccurrence;

		public int hashCode()
		{
			return string.hashCode();
		}

		public boolean equals( Object o )
		{
			return o.toString().equals( string );
		}

		public String toString()
		{
			return string;
		}

		public int compareTo(Object o)
		{
			if( o instanceof Struct )
			{
				Struct that = (Struct)o;
				if( this.indexFirstOccurrence < that.indexFirstOccurrence ) return (int)-1;
				else if( this.indexFirstOccurrence == that.indexFirstOccurrence ) return (int)0;
				else return 1;
			}
			else return (int)0;
		}
	}

	/**
	 * Writes data to a Hugin-style plain text "case" file.
	 */
	public void writeData( File outfile ) throws IOException
	{
		BufferedWriter out = new BufferedWriter( new FileWriter( outfile ) );
		writeVars(out);
		for( int i=0; i<myNumRecords; i++ )
		{
			setCurrentRecord( i );
			writeRecord( out, this );
		}
		out.close();
	}

	/**
	 * Returns the set of variables.
	 */
	public Set variables()
	{
		return mySetVariables;
	}

	/**
	 * Returns the number of evidence Maps in list.
	 */
	public int numRecords()
	{
		return size();
	}

	/*
	 * Reads variable list from file.
	 */
	private void readVars( String line, BeliefNetwork bn ) throws IOException
	{
		int numVars = 0;
		boolean found;
		Iterator i;
		Variable v;
		String tok;

		if (line != null)
		{
			StringTokenizer st = new StringTokenizer(line, ",\n");
			while( st.hasMoreTokens() )
			{
				tok = st.nextToken();
				/*
				i = mySetVariables.iterator();
				found = false;
				while (i.hasNext()) {
					v = (Variable) i.next();
					if (tok.compareTo(v.getID()) == 0) {
						myListVariables.add(v);
						numVars++;
						found = true;
						break;
					}
				}
				if (!found) {
					myListVariables.add(null);
				}
				*/
				v = bn.forID( tok );
				if( v == null ) myListVariables.add(null);
				else
				{
					myListVariables.add(v);
					numVars++;
				}
			}
		}

		if (mySetVariables.size() != numVars) {
			throw (new RuntimeException("One or more network variables not found in data file."));
		}

		ensureCapacity( myNumRecords );

		//System.out.println( "LearningData.readVars(): " + myListVariables );
	}

	/**
		@author Keith Cascio
		@since 111903
	*/
	public void ensureCapacity( int capacity )
	{
		if( myCapacity >= capacity ) return;

		if( myArrayColumns == null ) myArrayColumns = new Object[ myListVariables.size() ][];

		int index = (int)0;
		Object[] arrayValues;
		Object next;
		for( Iterator it = myListVariables.iterator(); it.hasNext(); )
		{
			next = it.next();
			if( myMapVariablesToColumns.containsKey( next ) )
			{
				arrayValues = (Object[])myMapVariablesToColumns.get( next );
				if( arrayValues.length < capacity )
				{
					Object[] arrayGrown = new Object[ capacity ];
					System.arraycopy( arrayValues, 0, arrayGrown, 0, arrayValues.length );
					myMapVariablesToColumns.put( next, arrayGrown );
					arrayValues = arrayGrown;
				}
			}
			else
			{
				arrayValues = new Object[ capacity ];
				myMapVariablesToColumns.put( next, arrayValues );
			}
			myArrayColumns[ index++ ] = arrayValues;
		}

		myCapacity = capacity;
	}

	/*
	 * Reads an evidence record from file.
	 */
	private boolean readRecord( String line, int indexSave ) throws IOException
	{
		if (line == null) return false;

		int numValues = 0;
		FiniteVariable varCurrent;
		String tok;
		Object inst;

		StringTokenizer toker = new StringTokenizer(line, ",\n");
		HashMap hm = new HashMap(mySetVariables.size());
		Iterator varIterator = myListVariables.iterator();

		for( int i=0; varIterator.hasNext() && toker.hasMoreTokens(); i++ )
		{
			varCurrent = (FiniteVariable) varIterator.next();
			tok = toker.nextToken();
			if( varCurrent != null )
			{
				inst = varCurrent.instance(tok);

				if( inst == null )
				{
					if( !tok.equals("N/A") )
					{
						//System.err.println( "Bad instance (state) name found in data file: '" + tok + "'" );
						//System.err.println( "varCurrent == " + varCurrent );
						//System.err.println( "varCurrent.instances() == " + varCurrent.instances() );
						throw new RuntimeException( "Bad instance (state) name found in data file: '" + tok + "'" );
					}
				}
				else
				{
					myArrayColumns[i][indexSave] = inst;
					myIndexLastRecord = indexSave;
				}
				numValues++;
			}
		}

		if (numValues != mySetVariables.size()) {
			throw new RuntimeException( "readRecord() failed for line: \"" +line+ "\"" );
		}

		return true;
	}

	/**
		@author Keith Cascio
		@since 111903
	*/
	public void add( Map record )
	{
		//if( !record.keySet().containsAll( myListVariables ) ) throw new IllegalArgumentException( "record does not contain all variables: " + record + ", " + myListVariables );

		ensureCapacity( ++myNumRecords );

		++myIndexLastRecord;
		Object next;
		Object[] array;
		for( Iterator varIterator = myListVariables.iterator(); varIterator.hasNext(); )
		{
			next = varIterator.next();
			array = (Object[]) myMapVariablesToColumns.get( next );
			array[ myIndexLastRecord ] = record.get( next );
		}
	}

	/*
	 * Writes variable list to file.
	 */
	private void writeVars( BufferedWriter out ) throws IOException
	{
		if( myListVariables.isEmpty() ) return;

		Iterator varIterator = myListVariables.iterator();
		out.write( ((Variable)varIterator.next()).getID() );
		while( varIterator.hasNext() ) out.write( "," + ((Variable)varIterator.next()).getID() );
		out.newLine();
	}

	/*
	 * Writes an evidence record to file.
	 */
	private void writeRecord(BufferedWriter out, Map record) throws IOException
	{
		String val;
		Object inst;

		boolean first = true;
		for( Iterator i = myListVariables.iterator(); i.hasNext(); )
		{
			if (first) first = false;
			else out.write(',');
			inst = record.get(i.next());
			if (inst == null)  out.write("N/A",0,3);
			else
			{
				val = inst.toString();
				out.write(val,0,val.length());
			}
		}
		out.newLine();
	}

	/**
		Test/debug
	*/
	public static void main( String[] args )
	{
		//try{
		//	UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		//}catch( Exception e ){
		//	System.err.println( "Error: Failed to set look and feel." );
		//}

		String pathNetwork = "c:\\keithcascio\\networks\\cancer_for_EM_test.net";
		String pathData = "c:\\keithcascio\\networks\\cancer_em.dat";
		if( args.length > 0 ) pathNetwork = args[0];
		if( args.length > 1 ) pathData = args[1];

		BeliefNetwork bn = null;

		try{
			bn = NetworkIO.read( pathNetwork );
		}catch( Exception e ){
			System.err.println( "Error: Failed to read " + pathNetwork );
			return;
		}

		double threshold = (double)0.05;
		int maxiterations = (int)1;
		Dynamator dyn = new RCEngineGenerator();////new SSEngineGenerator();
		boolean flagBias = false;
		LearningData learningData2 = new LearningData();

		//bn = (BeliefNetwork) bn.clone();

		////BeliefNetwork bnClone = (BeliefNetwork) bn.clone();
		////LearningDataOld learningData = new LearningDataOld( bnClone );

		try{
			File fileNetwork = new File( pathData );
			learningData2.readData( fileNetwork, bn );
			////learningData.readData( fileNetwork, bnClone );

			Definitions.STREAM_TEST.println( "debug test LearningData" );
			Learning.learnParamsEM( bn, learningData2, threshold, maxiterations, dyn, flagBias );
			Definitions.STREAM_TEST.println( "DONE debug test LearningData" );

			////Learning.learnParamsEM( bnClone, learningData, threshold, maxiterations, dyn, flagBias );
		}catch( Exception e ){
			System.err.println( "DEBUG LearningData.main() caught Exception:" );
			System.err.println( e );
			e.printStackTrace();
		}
	}

	private int myNumRecords = (int)0;
	private int myIndexLastRecord = (int)-1;
	private int myCapacity = (int)0;
	private int myIndexCurrentRecord = (int)0;
	private Object[][] myArrayColumns;
	private double[] myWeights;
	private Map myMapVariablesToColumns;
	private List myListVariables;
	private Set mySetVariables;
}
