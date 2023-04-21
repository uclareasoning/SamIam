package edu.ucla.belief.io.geneticlinkage;

import edu.ucla.util.*;

import java.io.*;
import java.util.*;

public class UtilGeneReader
{
    private UtilGeneReader() {};
    private static boolean DEBUG_UtilReader = false;



    static public int readIntExpected( StringBuffer in) {
        String buf = nextToken( in);
        if( buf.length() > 0) {
            return Integer.parseInt( buf);
        }
        else {
            throw new IllegalArgumentException("Expected integer but found nothing");
        }
    }

    static public int readInt( StringBuffer in) {
        String buf = nextToken( in);
        if( buf.length() > 0) {
            return Integer.parseInt( buf);
        }
        else {
            return Integer.MIN_VALUE;
        }
    }

    static public double readDblExpected( StringBuffer in) {
        String buf = nextToken( in);
        if( buf.length() > 0) {
            return Double.parseDouble( buf);
        }
        else {
            throw new IllegalArgumentException("Expected double but found nothing");
        }
    }

    static public double readDbl( StringBuffer in) {
        String buf = nextToken( in);
        if( buf.length() > 0) {
            return Double.parseDouble( buf);
        }
        else {
            return Double.NEGATIVE_INFINITY;
        }
    }

    /** Read in the next token, returning an empty string if no more exist.  It will then remove
     *  the characters from the StringBuffer that it read.  If it finds "// or <<", then it will ignore
     *  all characters after that and delete them from the buffer.
     */
    static private String nextToken( StringBuffer in) {
        StringBuffer retBuff = new StringBuffer();
        skipWhiteSpace( in);

        char ch = ' ';
        boolean done = false;
        if( in.length() > 0) { ch = in.charAt(0);}
        else { done = true;}

        //read token (until hit whitespace again, possibly comment)
        while( !done && !Character.isWhitespace((char)ch) && ch != '/' && ch != '<') {
            retBuff.append( (char)ch);
            in.deleteCharAt(0);
            if( in.length() > 0) {ch = in.charAt(0);}
            else { done = true;}
        }


        if( retBuff.length() > 0) {
            if( DEBUG_UtilReader) { System.out.println("NextToken: " + retBuff.toString());}
            return retBuff.toString();
        }
        else {
            return "";
        }
    }


    /** Remove whitespace characters & then any trailing comments.*/
    static private void skipWhiteSpace( StringBuffer in) {
        if( in == null || in.length() < 1) { return;}
        int ch = in.charAt(0);
        while( Character.isWhitespace((char)ch)) {
            in.deleteCharAt(0);
            if( in.length() > 0) { ch = in.charAt(0);}
            else { break;}
        }

        if(in.length() > 1 && in.charAt(0) == '/' && in.charAt(1) == '/') {
            if( DEBUG_UtilReader) { System.out.println("skipWhiteSpace (found comment)");}
            in.setLength(0);
        }
        else if(in.length() > 1 && in.charAt(0) == '<' && in.charAt(1) == '<') {
            if( DEBUG_UtilReader) { System.out.println("skipWhiteSpace (found comment)");}
            in.setLength(0);
        }
    }
}
