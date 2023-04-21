package edu.ucla.belief;

import java.util.*;

/** @author keith cascio
	@since  20020808 */
public class VariableComparator implements Comparator
{
	protected VariableComparator(){}

	private static void initStatic()
	{
		if( theCollator == null ) theCollator = java.text.Collator.getInstance();
	}

	public static VariableComparator getInstance()
	{
		if( theInstance == null )
		{
			initStatic();
			theInstance = new VariableComparator();
		}
		return theInstance;
	}

	public int compare( Object o1,     Object o2         ){
		if(                    o1 == null  && o2 == null ){ return  0; }
		else if(               o1 == null                ){ return  1; }
		else if(                              o2 == null ){ return -1; }
		else return biased(    o1.toString(), o2.toString(), o1, o2 );
	}

	/** @since 20040122 */
	public static final int biased( String str1, String str2, Object o1, Object o2 )
	{
		int    ret = theCollator.compare( str1, str2 );
		if(    ret == 0 ){ ret = System.identityHashCode(o1) - System.identityHashCode(o2); }
		return ret;
	}

	protected static     Comparator             theCollator;
	protected static     VariableComparator     theInstance;

}
