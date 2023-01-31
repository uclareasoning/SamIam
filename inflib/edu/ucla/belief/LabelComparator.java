package edu.ucla.belief;

import  edu.ucla.belief.io. StandardNode;

import  java.util. *;

/** @author keith cascio
	@since  20081110 */
public class LabelComparator extends VariableComparator{
	public int compare( Object o1, Object    o2         ){
		if(                    o1 == null && o2 == null ){ return  0; }
		else if(               o1 == null               ){ return  1; }
		else if(                             o2 == null ){ return -1; }
		else{
			String str1 = ( o1 instanceof StandardNode ) ? ((StandardNode)o1).getLabel() : o1.toString();
			String str2 = ( o2 instanceof StandardNode ) ? ((StandardNode)o2).getLabel() : o2.toString();
			return biased( str1, str2, o1, o2 );
		}
	}

	public static LabelComparator getInstanceLabel(){
		if(    INSTANCE == null ){ INSTANCE = new LabelComparator(); }
		return INSTANCE;
	}

	protected static  LabelComparator           INSTANCE;
}
