package edu.ucla.belief.io;

import edu.ucla.belief.*;
import edu.ucla.util.Interruptable;

import java.util.*;
import java.io.*;

/** Import cpt data from a tab-delimited file,
	e.g. created by Excel.
	Supports interpreting partial information.

	@author Keith Cascio
	@since 022405 */
public class CPTImport
{
	public static final String STR_DELIMITERS = "\t";

	public CPTImport(){
	}

	public void scanColumnNames( File inputfile ) throws Exception {
		BufferedReader reader = new BufferedReader( new FileReader( myFile = inputfile ) );
		String line = reader.readLine();
		if( line == null ) throw new IllegalStateException( "input file empty" );
		scanColumnNames( line );
	}

	private void scanColumnNames( String firstline ){
		StringTokenizer toker = new StringTokenizer( firstline, STR_DELIMITERS );
		if( myColumns == null ) myColumns = new LinkedList();
		else myColumns.clear();
		while( toker.hasMoreTokens() ){
			myColumns.addLast( new CPTImportColumnInfo( toker.nextToken() ) );
		}
	}

	public List getColumns(){
		return myColumns;
	}

	public void scanRows() throws Exception {
		if( myColumns == null ) throw new IllegalStateException( "call scanColumnNames() first" );
		BufferedReader reader = new BufferedReader( new FileReader( myFile ) );
		String line = reader.readLine();
		myNumConditionsInFile = 0;
		while( (line = reader.readLine()) != null ) scanRow( line );
	}

	private void scanRow( String line ) throws Exception {
		StringTokenizer toker = new StringTokenizer( line, STR_DELIMITERS );
		if( toker.countTokens() != myColumns.size() ) throw new IOException( "wrong number of columns: " + toker.countTokens() );
		Iterator it = myColumns.iterator();
		CPTImportColumnInfo next;
		while( toker.hasMoreTokens() && it.hasNext() ){
			next = (CPTImportColumnInfo) it.next();
			next.addValue( toker.nextToken() );
		}
		++myNumConditionsInFile;
	}

	private Set getValues( String columnname ){
		CPTImportColumnInfo next;
		for( Iterator it = myColumns.iterator(); it.hasNext(); ){
			next = (CPTImportColumnInfo) it.next();
			if( columnname.equals( next.getToken() ) ) return next.getValues();
		}
		return Collections.EMPTY_SET;
	}

	public void assign( String token, FiniteVariable variable ){
		CPTImportColumnInfo struct = forToken( token );
		if( struct == null ) throw new RuntimeException( "no column named \"" + token + "\"" );
		else struct.setVariable( variable );
	}

	public CPTImportColumnInfo forToken( String token ){
		CPTImportColumnInfo struct;
		for( Iterator it = myColumns.iterator(); it.hasNext(); ){
			struct = (CPTImportColumnInfo) it.next();
			if( token.equals( struct.getToken() ) ) return struct;
		}
		return (CPTImportColumnInfo)null;
	}

	public int getNumConditionsInFile(){
		return myNumConditionsInFile;
	}

	public CPTInfo createCPT( CPTInfo info, String tokenProbabilityColumn ) throws Exception {
		Thread.sleep(4);//Interruptable.checkInterrupted();
		if( myColumns == null ) throw new IllegalStateException( "call scanColumnNames() first" );
		CPTImportColumnInfo probabilityColumn = forToken( tokenProbabilityColumn );
		if( probabilityColumn == null ) throw new RuntimeException( "column \"" + tokenProbabilityColumn + "\" not found" );
		myProgress = 0;

		FiniteVariable joint = info.getJoint();
		//CPTInfo info = new CPTInfo( joint );
		ParameterRecorder recorder =
			isDataCompleteFor( joint ) ?
			((ParameterRecorder)new CompleteParameterRecorder( info )) :
			((ParameterRecorder)new IncompleteParameterRecorder( info ));

		fillCPT( recorder, probabilityColumn );
		//info.normalize();
		return info;
	}

	private void fillCPT( ParameterRecorder recorder, CPTImportColumnInfo probabilityColumn ) throws Exception {
		Thread.sleep(4);//Interruptable.checkInterrupted();
		BufferedReader reader = new BufferedReader( new FileReader( myFile ) );
		String line = reader.readLine();
		Thread.sleep(4);//Interruptable.checkInterrupted();
		while( (line = reader.readLine()) != null ){
			recordData( line, probabilityColumn, recorder );
			Thread.sleep(4);//Interruptable.checkInterrupted();
		}
	}

	private void recordData( String line, CPTImportColumnInfo probabilityColumn, ParameterRecorder recorder ) throws Exception
	{
		StringTokenizer toker = new StringTokenizer( line, STR_DELIMITERS );
		Iterator it = myColumns.iterator();
		CPTImportColumnInfo struct;
		String token;
		FiniteVariable variable;
		int indexInstance;
		String tokenValue = null;
		recorder.init();//int[] mindex = cpt.getUtilIndices();
		while( toker.hasMoreTokens() && it.hasNext() ){
			struct = (CPTImportColumnInfo) it.next();
			token = toker.nextToken();
			if( struct == probabilityColumn ){
				tokenValue = token;//value = Double.parseDouble( token );
			}
			else{
				variable = struct.getVariable();
				if( variable != null ){
					indexInstance = struct.indexForToken( token );
					if( indexInstance < 0 ) throw new RuntimeException( "value \"" + token + "\" not mapped in column " + struct.getToken() );
					recorder.record( variable, indexInstance );//mindex[ cpt.index(variable) ] = indexInstance;
				}
			}
		}
		if( tokenValue == null ) throw new RuntimeException( "value not found" );
		recorder.setParameter( tokenValue );//cpt.setParameter( mindex, tokenValue );
		++myProgress;
	}

	public boolean isDataCompleteFor( FiniteVariable joint ){
		Collection variables = new LinkedList( joint.getCPTShell( joint.getDSLNodeType() ).variables() );
		CPTImportColumnInfo struct;
		for( Iterator it = myColumns.iterator(); it.hasNext(); ){
			struct = (CPTImportColumnInfo) it.next();
			variables.remove( struct.getVariable() );
		}
		return variables.isEmpty();
	}

	public int getProgress(){
		return myProgress;
	}

	public File getInputFile(){
		return myFile;
	}

	public interface ParameterRecorder{
		public void init();
		public void record( FiniteVariable variable, int indexInstance );
		public void setParameter( String tokenValue ) throws Exception;
	}

	public static class CompleteParameterRecorder implements ParameterRecorder{
		public CompleteParameterRecorder( CPTInfo cpt ){
			this.cpt = cpt;
		}

		public void init(){
			myIndices = cpt.getUtilIndices();
		}

		public void record( FiniteVariable variable, int indexInstance ){
			myIndices[ cpt.index(variable) ] = indexInstance;
		}

		public void setParameter( String tokenValue ) throws Exception{
			cpt.setParameter( myIndices, tokenValue );
		}

		private CPTInfo cpt;
		private int[] myIndices;
	}

	public static class IncompleteParameterRecorder implements ParameterRecorder{
		public IncompleteParameterRecorder( CPTInfo cpt ){
			this.cpt = cpt;
		}

		public void init(){
			myMapping = cpt.getUtilMap();
		}

		public void record( FiniteVariable variable, int indexInstance ){
			myMapping.put( variable, new Integer( indexInstance ) );
		}

		public void setParameter( String tokenValue ) throws Exception{
			cpt.setParameter( myMapping, tokenValue );
		}

		private CPTInfo cpt;
		private Map myMapping;
	}

	private File myFile;
	private LinkedList myColumns;
	private int myNumConditionsInFile = (int)-1;
	private int myProgress = (int)-1;
}
