package edu.ucla.belief.io;

import edu.ucla.belief.FiniteVariable;

import java.util.*;
import java.io.*;

/** Bundle info about a single
	column of a tab-delimited file
	containing CPT data.

	@author Keith Cascio
	@since 022405 */
public class CPTImportColumnInfo
{
	public CPTImportColumnInfo( String token ){
		this.myToken = token;
	}

	public String toString(){
		return this.myToken;
	}

	public void addValue( String value ){
		if( myValues == null ) myValues = new HashSet();
		else if( myValues.contains( value ) ) return;
		myValues.add( value );
	}

	public void mapInstance( String token, Object instance ){
		if( myVariable == null ) throw new RuntimeException( "call setVariable() first" );
		if( !myVariable.contains( instance ) ) throw new RuntimeException( myVariable.getID() + " does not contain \"" + instance.toString() + "\"" );
		int index = myVariable.index( instance );
		if( myMapTokenToIndex == null ) myMapTokenToIndex = new HashMap( myVariable.size() );
		myMapTokenToIndex.put( token, new Integer(index) );
	}

	public int indexForToken( String token ){
		if( myMapTokenToIndex == null ) return -1;
		Integer mapping = (Integer) myMapTokenToIndex.get( token );
		if( mapping == null ) return -1;
		else return mapping.intValue();
	}

	public String getToken(){
		return this.myToken;
	}

	public Set getValues(){
		return this.myValues;
	}

	public FiniteVariable getVariable(){
		return this.myVariable;
	}

	public void setVariable( FiniteVariable var ){
		this.myVariable = var;
	}

	private String myToken;
	private Set myValues;
	private FiniteVariable myVariable;
	private Map myMapTokenToIndex;
}
