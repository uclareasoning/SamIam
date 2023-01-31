package edu.ucla.belief;



import  java.util. *;

/** @author keith cascio
	@since  20081110 */
public class    IDComparator extends VariableComparator{
	public int compare( Object o1, Object    o2         ){
		if(                    o1 == null && o2 == null ){ return  0; }
		else if(               o1 == null               ){ return  1; }
		else if(                             o2 == null ){ return -1; }
		else{
			String str1 = ( o1 instanceof     Variable ) ? (    (Variable)o1).getID()    : o1.toString();
			String str2 = ( o2 instanceof     Variable ) ? (    (Variable)o2).getID()    : o2.toString();
			return biased( str1, str2, o1, o2 );
		}
	}

	public static    IDComparator getInstanceID(){
		if(    INSTANCE == null ){ INSTANCE = new IDComparator(); }
		return INSTANCE;
	}

	protected static     IDComparator           INSTANCE;
}
