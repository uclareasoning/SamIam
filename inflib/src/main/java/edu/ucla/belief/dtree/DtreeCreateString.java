package edu.ucla.belief.dtree;

import java.util.*;
import java.io.*;

import edu.ucla.belief.*;
import edu.ucla.util.*;


/** This class can create a Dtree, using the supplied String.
 *
 * @author David Allen
 */

public class DtreeCreateString extends Dtree.Create {

    static final private boolean DEBUG_CreateStr = false;
    static final private boolean DEBUG_dtree1 = false;
    static final private boolean DEBUG_dtree2 = false;

	private PushbackReader pbrFile;

    public String abrev() { return "Str";}
    public String name() { return "Create-Str";}


    /** Create a DtreeCreateString which will use the PushbackReader pbrFile to
     *    generate dtrees.
     */
    public DtreeCreateString( PushbackReader pbrFile) {
        Assert.notNull( pbrFile, "DtreeCreateString: pbrFile cannot be null");
        this.pbrFile = pbrFile;
    }


    /** Create a Dtree from the data.
     *
     * @param data A Collection of DtreeNodeLeaf objects.
     * @return The root of the created dtree.
     */
    public DtreeNode create( Collection data) {


		Map varToLeaf = new HashMap();//from data
		{
			for( Iterator itr = data.iterator(); itr.hasNext();) {
				DtreeNodeLeaf dtn = (DtreeNodeLeaf)itr.next();
				FiniteVariable fv = dtn.child();
				varToLeaf.put( fv, dtn);
			}
		}


		DtreeNode root = readDtree( pbrFile, varToLeaf);
		return root;
    }



    /** Read in a dtree from a paren file.
     *
     * @param dtreeFile The file to use.
     * @param varToLeaf A Map from a finiteVariable to a DtreeNodeLeaf associated with it.
     * @throws Dtree.DtreeCreationException if could not read the Dtree.
     */
    static private DtreeNode readDtree( PushbackReader dtreeFile, Map varToLeaf) {
        DtreeNode ret = null;
        try{
            ignoreWS( dtreeFile);
            int ch = peek( dtreeFile);

            if( ch == '(') {
                ret = readPair( dtreeFile, varToLeaf);
                ignoreWS( dtreeFile);
                ch = dtreeFile.read();
                if( ch != -1) { throw new Dtree.DtreeCreationException("EOF not found when expected." + ch + "," + (char)ch);}
            }
            else {
                ret = readLeafNode( dtreeFile, varToLeaf);
                ignoreWS( dtreeFile);
                ch = dtreeFile.read();
                if( ch != -1) { throw new Dtree.DtreeCreationException("EOF not found when expected (single)." + ch + "," + (char)ch);}
            }
            if( !varToLeaf.isEmpty()) {
                throw new Dtree.DtreeCreationException("Not all variables were found. " + varToLeaf);
            }
        }
        catch( Dtree.DtreeCreationException e) { throw e;}
        catch( Exception e) { throw new Dtree.DtreeCreationException("Exception thrown: " + e.toString());}
        return ret;
    }


    /**Peek at the next character in the reader.*/
    static private int peek( PushbackReader in)
    throws IOException {
        int ch = in.read();
        if( DEBUG_dtree2 ) { Definitions.STREAM_VERBOSE.println("peek: " + ch + " '" + (char)ch + "'");}
        if( ch != -1) { in.unread( ch);}
        return ch;
    }

    /**Reads an internal node in the dtree.*/
    static private DtreeNode readPair( PushbackReader in, Map varToLeaf)
    throws IOException {
        int ch = in.read();
        if( ch != '(') { throw new Dtree.DtreeCreationException("readPair start found: " + (char)ch);}

        if( DEBUG_dtree1) { Definitions.STREAM_VERBOSE.println("readPair start");}

        DtreeNode left;
        DtreeNode right;

        ignoreWS( in);

        ch = peek( in);
        if( ch == '(') { left = readPair( in, varToLeaf);}
        else { left = readLeafNode( in, varToLeaf);}

        ignoreWS( in);

        ch = peek( in);
        if( ch == '(') { right = readPair( in, varToLeaf);}
        else { right = readLeafNode( in, varToLeaf);}

        ignoreWS( in);

        ch = in.read();
        if( ch != ')') { throw new Dtree.DtreeCreationException("readPair end found: " + (char)ch);}

        if( DEBUG_dtree1) { Definitions.STREAM_VERBOSE.println("readPair end (" + left + "," + right + ")");}

        return new DtreeNodeInternal( left, right);
    }

    /**Reads a leaf node ID.*/
    static private DtreeNodeLeaf readLeafNode( PushbackReader in, Map varToLeaf)
    throws IOException {
        StringBuffer nameBuff = new StringBuffer();
        ignoreWS( in);
        int ch = in.read();
        while( !Character.isWhitespace( (char)ch) && (ch != '(') && ch != -1) { nameBuff.append( (char)ch); ch = in.read();}
        if( ch != -1) { in.unread( ch);}
        String name = nameBuff.toString();

        if( DEBUG_dtree1) { Definitions.STREAM_VERBOSE.println("readLeafNode: " + name);}

        FiniteVariable var = null;
        for( Iterator itr = varToLeaf.keySet().iterator(); itr.hasNext();) {
            FiniteVariable v = (FiniteVariable)itr.next();
            if( v.getID().equals( name)) { var = v; break;}
        }

        if( var == null) { throw new Dtree.DtreeCreationException("Unknown variable found: " + name);}
        else {
            //don't allow this variable to be found a second time
            DtreeNodeLeaf fam = (DtreeNodeLeaf)varToLeaf.remove( var);
            if( fam == null) {  throw new Dtree.DtreeCreationException("Could not find leaf for " + name);}
            return fam;
        }
    }


    /**Ignores white space.*/
    static private void ignoreWS( PushbackReader in)
    throws IOException {
        int ch = in.read();
        if( DEBUG_dtree2 ) { Definitions.STREAM_VERBOSE.println("ws: " + ch + " '" + (char)ch + "'");}
        while( Character.isWhitespace( (char)ch) && (ch != -1)) {
            ch = in.read();
            if( DEBUG_dtree2 ) { Definitions.STREAM_VERBOSE.println("ws: " + ch + "'" + (char)ch + "'");}
        }
        if( ch != -1) { in.unread( ch);}
    }


}//end class DtreeCreateString

