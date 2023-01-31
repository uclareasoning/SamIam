package edu.ucla.util;
/**
* A class consisting of static utility functions dealing with double arrays.
*/
public class DblArrays {
//    /**
//     * searches for value in a sorted array between minloc and maxloc inclusive.
//     * returns the location if found, else returns -(insertloc+1) where
//     * insertloc is the location it should be inserted into.
//     */
//    public static int binarySearch(int value, int[] array, int minloc,
//            int maxloc) {
//        while (minloc <= maxloc) {
//            int current = (minloc + maxloc) / 2;
//            if (array[current] < value) {
//                minloc = current + 1;
//            } else if (array[current] > value) {
//                maxloc = current - 1;
//            } else {
//                return current;
//            }
//        }
//        return -minloc - 1;
//    }
//    /**
//     * returns an array which is basically values[inds]. In other words,
//     * result[i]=values[inds[i]].
//     */
//    public static int[] apply(int[] values, int[] inds) {
//        int[] result = new int[inds.length];
//        for (int i = 0; i < inds.length; i++) {
//            result[i] = values[inds[i]];
//        }
//        return result;
//    }
//    public static int[] apply(int[] values, int[] inds, int[] result) {
//        if (result == null) {
//            result = new int[inds.length];
//        }
//        for (int i = 0; i < inds.length; i++) {
//            result[i] = values[inds[i]];
//        }
//        return result;
//    }
    /**
     * Sets values[i]=i for all indices.
     */
    public static void index(double[] values) {
        for (int i = 0; i < values.length; i++) {
            values[i] = i;
        }
    }
    /**
     * returns an array of length size where result[i]=i.
     */
    public static double[] index(int size) {
        double[] result = new double[size];
        index(result);
        return result;
    }


    public static String convertToString( double[] values) {
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

    /**
     * displays the double array on System.stream
     */
    public static void print( double[] values, java.io.PrintStream stream ) {
        if( values == null) {
            stream.print( "[null]");
            return;
        }

        stream.print("[");
        if( values.length > 0) {
            stream.print("" + values[0]);
            for( int i=1; i<values.length; i++) {
                stream.print("," + values[i]);
            }
        }
        stream.print("]");
    }

    /**
     * displays the double array on System.stream
     */
    public static void println( double[] values, java.io.PrintStream stream ) {
        print( values, stream );
        stream.println();
    }


    /**
     *  Will shuffle the values in the double array.
     *  See Collections.shuffle for a description of the algorithm.
     */
    public static void shuffle( double[] values) {
        for( int i=values.length-1; i>0; i--) { //for last to second

            //pick another element lower than self
            double rndm = Math.random(); //  inclusive [0.0..1.0) exclusive
            int indx = (int)Math.floor(rndm * (i+1)); //number from 0..i (inclusive)

            //swap
            double tmp = values[i];
            values[i] = values[indx];
            values[indx] = tmp;
        }
    }

/* Added by Hei Chan */
	public static double[] multiply(double[] a, double k) {
		double[] result = new double[a.length];
		for (int i = 0; i < a.length; i++)
			result[i] = a[i] * k;
		return result;
	}

/* Added by Hei Chan */
	public static double[] subtract(double[] a, double[] b) {
		if (a.length != b.length)
			return null;
		double[] result = new double[a.length];
		for (int i = 0; i < a.length; i++)
			result[i] = a[i] - b[i];
		return result;
	}

/* Added by Hei Chan */
	public static double[] add(double[] a, double[] b) {
		if (a.length != b.length)
			return null;
		double[] result = new double[a.length];
		for (int i = 0; i < a.length; i++)
			result[i] = a[i] + b[i];
		return result;
	}

/* Added by Hei Chan */
	public static double dotProduct(double[] a, double[] b) {
		if (a.length != b.length)
			return Double.NaN;
		double result = 0.0;
		for (int i = 0; i < a.length; i++)
			result += a[i] * b[i];
		return result;
	}

/* Added by Hei Chan */
	public static double max(double[] a) {
		double max = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < a.length; i++)
			if (a[i] > max)
				max = a[i];
		return max;
	}

/* Added by Hei Chan */
	public static double min(double[] a) {
		double min = Double.POSITIVE_INFINITY;
		for (int i = 0; i < a.length; i++)
			if (a[i] < min)
				min = a[i];
		return min;
	}
}
