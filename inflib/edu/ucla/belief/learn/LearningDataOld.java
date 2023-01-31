package edu.ucla.belief.learn;

import edu.ucla.belief.*;
import java.util.*;
import java.io.*;

/**
* A structure that contains a list of evidence mappings.
*/
public class LearningDataOld
{
	private List listVariables, listRecords;
	private Set setVariables;

	public String valuesToString( Map map )
	{
		Iterator it = listVariables.iterator();
		String ret = canonical( map.get( it.next() ) );
		while( it.hasNext() )
		{
			ret += "," + canonical( map.get( it.next() ) );
		}
		return ret;
	}

	public static String canonical( Object o )
	{
		if( o == null ) return "N/A";
		else return o.toString();
	}

	public FiniteVariable getDebugVariable()
	{
		return (FiniteVariable) listVariables.get(1);
	}

	/**
	 * Creates an empty LearningDataOld list.
	 * @param vars The set of FiniteVariables that the data will contain
	 * evidence about.
	 */
	public LearningDataOld( Collection vars )
	{
		listRecords = new LinkedList();
		listVariables = new LinkedList();
		setVariables = new HashSet(vars);
	}

	/**
	 * Reads data from a Hugin-style plain text "case" file.
	 * The variables listed in the file must contain all of the variables
	 * in LearningDataOld.  Erroneous records are ignored.
	 */
	public void readData( File infile, BeliefNetwork bn ) throws IOException, RuntimeException
	{
		BufferedReader in = new BufferedReader( new FileReader( infile ) );
		readVars( in, bn );
		while( readRecord(in) );
		in.close();
	}

	/**
	 * Writes data to a Hugin-style plain text "case" file.
	 */
	public void writeData( File outfile ) throws IOException
	{
		BufferedWriter out = new BufferedWriter( new FileWriter( outfile ) );
		writeVars(out);
		Iterator i = iterator();
		while (i.hasNext())
			writeRecord(out, (Map) i.next());
		out.close();
	}

	/**
	 * Returns the set of variables.
	 */
	public Set variables()
	{
		return setVariables;
	}

	/**
	 * Returns an iterator over the evidence in the list.
	 */
	public Iterator iterator()
	{
		return listRecords.iterator();
	}

	/**
	 * Adds an evidence record to the list.
	 */
	public boolean add(Map record)
	{
		if (!setVariables.containsAll(record.keySet()))
			throw (new RuntimeException("evidence variables do not match data variables"));

		return listRecords.add(record);
	}

	/**
	 * Returns the number of evidence Maps in list.
	 */
	public int numRecords()
	{
		return listRecords.size();
	}

	/**
	 * Determines if list is empty.
	 */
	public boolean isEmpty()
	{
		return listRecords.isEmpty();
	}

	/**
	 * Determines if every evidence map in list contains an instantiation
	 * of every variable.
	 */
	public boolean isComplete()
	{
		return isComplete(setVariables);
	}

	/**
	 * Determines if every evidence map in list contains an instantiation
	 * of every variable in subset "vars".
	 * @param vars A subset of LearningDataOld's variables.
	 */
	public boolean isComplete(Set vars)
	{
		boolean complete = true;

		Iterator i = iterator();
		while (i.hasNext()) {
			if (!(((Map) i.next()).keySet()).containsAll(vars)) {
				complete = false;
				break;
			}
		}

		return complete;
	}

	/**
	 * Removes all records that do not contain a complete instantiation
	 * of the variables.
	 */
	public int removeIncomplete()
	{
		return removeIncomplete(setVariables);
	}

	/**
	 * Removes all records that do not contain a complete instantiation
	 * of the subset "vars".
	 * @param vars A subset of LearningDataOld's variables.
	 */
	public int removeIncomplete(Set vars)
	{
		int num_incomplete = 0;

		ListIterator i = listRecords.listIterator();
		while (i.hasNext()) {
			if (!(((Map) i.next()).keySet()).containsAll(vars)) {
				i.remove();
				num_incomplete++;
			}
		}

		return num_incomplete;
	}

	/*
	 * Reads variable list from file.
	 */
	private void readVars( BufferedReader in, BeliefNetwork bn ) throws IOException
	{
		int numVars = 0;
		boolean found;
		Iterator i;
		Variable v;
		String tok;

		String line = in.readLine();
		if (line != null)
		{
			StringTokenizer st = new StringTokenizer(line, ",\n");
			while( st.hasMoreTokens() )
			{
				tok = st.nextToken();
				/*
				i = setVariables.iterator();
				found = false;
				while (i.hasNext()) {
					v = (Variable) i.next();
					if (tok.compareTo(v.getID()) == 0) {
						listVariables.add(v);
						numVars++;
						found = true;
						break;
					}
				}
				if (!found) {
					listVariables.add(null);
				}
				*/
				v = bn.forID( tok );
				if( v == null ) listVariables.add(null);
				else
				{
					listVariables.add(v);
					numVars++;
				}
			}
		}

		if (setVariables.size() != numVars) {
			throw (new RuntimeException("One or more network variables not found in data file."));
		}

		//System.out.println( "LearningDataOld.readVars(): " + listVariables );
	}

	/*
	 * Reads an evidence record from file.
	 */
	private boolean readRecord(BufferedReader in) throws IOException
	{
		int numValues = 0;
		FiniteVariable varCurrent;
		String tok;
		Object inst;

		String line = in.readLine();
		if (line == null) {
			return false;
		}

		StringTokenizer toker = new StringTokenizer(line, ",\n");
		HashMap hm = new HashMap(setVariables.size());
		Iterator varIterator = listVariables.listIterator();

		while( varIterator.hasNext() && toker.hasMoreTokens() )
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
				else hm.put(varCurrent, inst);
				numValues++;
			}
		}

		if (numValues == setVariables.size()) {
			listRecords.add(hm);
		}

		return true;
	}

	/*
	 * Writes variable list to file.
	 */
	private void writeVars( BufferedWriter out ) throws IOException
	{
		String id;

		boolean first = true;
		for( Iterator i = listVariables.iterator();i.hasNext(); )
		{
			if(first) first = false;
			else  out.write(',');
			id = ((Variable)i.next()).getID();
			//out.write(id,0,id.length());
			out.write( id );
		}
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
		for( Iterator i = listVariables.iterator(); i.hasNext(); )
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
}
