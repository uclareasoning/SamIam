package edu.ucla.belief.dtree;

import java.util.*;
import edu.ucla.util.*;


/** This class can create a Dtree, using the supplied elimination order.
 *
 * @author David Allen
 */

public class DtreeCreateEO extends Dtree.Create {

    static final private boolean DEBUG_CreateEO = false;

    private List eo;

    public String abrev() { return "EO";}
    public String name() { return "Create-EO";}


    /** Create a DtreeCreateEO which will use the elimination order eo to
     *    generate dtrees.
     */
    public DtreeCreateEO( List eo) {
        Assert.notNull( eo, "DtreeCreateEO: eo cannot be null");
        this.eo = new ArrayList( eo);
    }


    /** Create a Dtree from the data.
     *
     * @param data A Collection of DtreeNodes.
     * @return The root of the created dtree.
     */
    public DtreeNode create( Collection data) {

        Dtree.Create cr = new Dtree.Create();
        HashSet nodesLeft = new HashSet( data);
        Collection tmpNodes = new HashSet();

		HashSet nodesLeftSingletons = new HashSet();
		DtreeNode dtnsin = null;

        for( Iterator itr_v = eo.iterator(); itr_v.hasNext();) {
            Object var = itr_v.next();

            for( Iterator itr_nl = nodesLeft.iterator(); itr_nl.hasNext();) {
                DtreeNode dn = (DtreeNode)itr_nl.next();

                if( dn.containsVar( var)) {
                    tmpNodes.add( dn);   //find all nodes with var
                    itr_nl.remove(); //remove dn from nodesLeft
                    dtnsin = dn;
                }
            }

            if( tmpNodes.size() == 1 && dtnsin.vars.size()==1) { //if only one dtnode for this variable && this is only variable in node don't put it back into nodesleft
				nodesLeftSingletons.add( dtnsin);
			}
            else if( tmpNodes.size() == 1) { //if only one dtnode for this variable, but it has other variables add it back into nodes left
				nodesLeft.add( dtnsin);
			}
            else if( tmpNodes.size() >= 1) { //if variable appears in multiple places, add them up & put back into nodes left
                nodesLeft.add( cr.create( tmpNodes)); //create dtree with them
            }
            tmpNodes.clear();
        }

		nodesLeft.addAll( nodesLeftSingletons);

        //All nodes might not have been joined together yet (e.g. if the
        //  root node has an empty cutset).  Therefore, join the rest of the
        //  nodes together.
        return cr.create( nodesLeft);
    }

}//end class DtreeCreateEO

