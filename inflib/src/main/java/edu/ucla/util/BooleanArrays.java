package edu.ucla.util;

public class BooleanArrays
{
    public static String convertToString( boolean[] values) {
        StringBuffer ret = new StringBuffer();
        if( values == null) {
            ret.append("[null]");
            return ret.toString(); 
        }

        ret.append("[");
        if( values.length > 0) {
            ret.append( "" + values[0]);  
            for( int i=1; i<values.length; i++) {
                ret.append("," + values[i]);
            }
        }
        ret.append("]");
        return ret.toString();
    }

}
