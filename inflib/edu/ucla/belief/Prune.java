package edu.ucla.belief;

/**
 * A class containing utility methods that assist code that wants to work
 * with a pruned network.
 *
 * @author Mark Chavira
 */

public class Prune {

  /**
   * Returns the image of the given set according to the given map.
   *
   * @param s the given set.
   * @param m the given map.
   * @param image on entry, an empty set; on exit, the image.
   */

  private static void image (
   java.util.Set s, java.util.Map m, java.util.Set newS) {
    for (java.util.Iterator i = s.iterator (); i.hasNext ();) {
      newS.add (m.get (i.next ()));
    }
  }

  /**
   * Renames the variables in the given instantiation.  In the result, the
   * key set of the new instantiation will be the image of the key set in
   * the old instantiation according to the given map.  A value for a
   * variable in the new instantiation will have the same index as the value
   * for the corresponding variable in the old instantiation.
   *
   * @param pw the given instantiation.
   * @param m the map from old variable to new variable.
   * @param newPw on entry, an empty map; on exit, the renamed instantiation.
   */

  private static void renameInstantiation (
   java.util.Map pw, java.util.Map m, java.util.Map newPw) {
    for (java.util.Iterator i = pw.keySet ().iterator (); i.hasNext ();) {
      edu.ucla.belief.FiniteVariable oldVar =
        (edu.ucla.belief.FiniteVariable)i.next ();
      Object oldVal = pw.get (oldVar);
      int intVal = oldVar.index (oldVal);
      edu.ucla.belief.FiniteVariable newVar =
        (edu.ucla.belief.FiniteVariable)m.get (oldVar);
      Object newVal = newVar.instance (intVal);
      newPw.put (newVar, newVal);
    }
  }

  /**
   * Returns a map from id to variable for the given belief network.
   *
   * @param bn the given belief network.
   * @return the map.
   */

  public static java.util.HashMap idToVariableMap (
   edu.ucla.belief.BeliefNetwork bn) {
    java.util.HashMap ans = new java.util.HashMap ();
    for (java.util.Iterator i = bn.iterator (); i.hasNext ();) {
      edu.ucla.belief.FiniteVariable v =
        (edu.ucla.belief.FiniteVariable)i.next ();
      ans.put (v.getID (), v);
    }
    return ans;
  }

  /**
   * Returns the image of the given set according to the given map.
   *
   * @param s the given set.
   * @param m the given map.
   * @return the image.
   */

  public static java.util.HashSet image (java.util.Set s, java.util.Map m) {
    java.util.HashSet ans = new java.util.HashSet ();
    image (s, m, ans);
    return ans;
  }

  /**
   * Renames the variables in the given instantiation.  In the result, the
   * key set of the new instantiation will be the image of the key set in
   * the old instantiation according to the given map.  A value for a
   * variable in the new instantiation will have the same index as the value
   * for the corresponding variable in the old instantiation.
   *
   * @param pw the given instantiation.
   * @param m the map from old variable to new variable.
   * @return the renamed instantiation.
   */

  public static java.util.HashMap renameInstantiation (
   java.util.Map pw, java.util.Map m) {
    java.util.HashMap ans = new java.util.HashMap ();
    renameInstantiation (pw, m, ans);
    return ans;
  }

  /**
   * Returns a copy of the given network pruned according to the given query
   * variables and the given evidence.  One may then perform inference with
   * the pruned version.  However, since the pruned version is a copy, its
   * variables are distinct from those in the given network, and we therefore
   * need a mechanism for translating between the variables of the given
   * network and those of the pruned copy.  As a result, this method accepts
   * two parameters which should be empty maps upon input and which upon exit
   * will encode the translations.  In addition, this method accepts two
   * parameters which upon exit will contain the pruned copy's map variables
   * and evidence.
   *
   * @param bn the given network.
   * @param queryVars the given query variables.
   * @param evidence the given evidence.
   * @param oldToNew on entry, an empty map; on exit, a map from the given
   *   network's variables to those of the pruned copy.
   * @param newToOld on entry, an empty map; on exit, a map from the pruned
   *   copy's variables to those of the given network.
   * @param newQueryVars upon entry, an empty set; upon exit, the set of
   *   query variables in the pruned copy.
   * @param newEvidence upon entry, an empty map; upon exit, the evidence of
   *   the pruned copy.
   * @return the pruned copy of the network.
   */

  public static edu.ucla.belief.BeliefNetwork prune (
   edu.ucla.belief.BeliefNetwork bn, java.util.Set queryVars,
   java.util.Map evidence, java.util.Map oldToNew,
   java.util.Map newToOld, java.util.Set newQueryVars,
   java.util.Map newEvidence) {
    // Clone the network,

    edu.ucla.belief.BeliefNetwork newBn =
      (edu.ucla.belief.BeliefNetwork)bn.clone ();

    // Get maps between old variable and new variables.

    {
      //java.util.HashMap idToOld = idToVariableMap (bn);
      for (java.util.Iterator i = newBn.iterator (); i.hasNext ();) {
        edu.ucla.belief.FiniteVariable n =
          (edu.ucla.belief.FiniteVariable)i.next ();
        edu.ucla.belief.FiniteVariable o =
          (edu.ucla.belief.FiniteVariable) bn.forID( n.getID() );
        oldToNew.put (o, n);
        newToOld.put (n, o);
      }
    }

    // Convert the query and evidence.

    image (queryVars, oldToNew, newQueryVars);
    renameInstantiation (evidence, oldToNew, newEvidence);


    // Prune the nodes.  Traverse the variables bottom-up, deciding
    // which to keep.  We keep a variable if it has any kept children or if
    // it is in the query or evidence sets.

    java.util.HashSet keep = new java.util.HashSet ();
    java.util.HashSet queryAndEvidence =
      new java.util.HashSet (newQueryVars);
    queryAndEvidence.addAll (newEvidence.keySet ());
    for (
     java.util.Iterator i =
     new edu.ucla.structure.DepthFirstIterator (newBn); i.hasNext ();) {
      edu.ucla.belief.FiniteVariable v =
        (edu.ucla.belief.FiniteVariable)i.next ();
      boolean keptChild = false;
      for (
       java.util.Iterator j = newBn.outGoing (v).iterator ();
       j.hasNext ();) {
        if (keep.contains (j.next ())) {
          keptChild = true;
          break;
        }
      }
      boolean queryOrEvidence = false;
      if (!keptChild) {
        //edu.ucla.belief.CPTShell d =
        //  (edu.ucla.belief.CPTShell)v.getCPTShell ();
        //java.util.List vars = d.variables ();
        //queryOrEvidence = queryAndEvidence.contains (vars.get (vars.size () - 1));
        queryOrEvidence = queryAndEvidence.contains( v );
      }
      if (keptChild || queryOrEvidence) {
        keep.add (v);
      }
    }

    // Now we prune edges and return the result.

    java.util.HashMap varToDist = new java.util.HashMap ();
    for (java.util.Iterator i = keep.iterator (); i.hasNext ();) {
      edu.ucla.belief.FiniteVariable v =
        (edu.ucla.belief.FiniteVariable)i.next ();
      Object val = newEvidence.remove (v);
      varToDist.put (
        v,
        new edu.ucla.belief.TableShell (
          v.getCPTShell ().getCPT ().shrink (newEvidence)));
      if (val != null) {
        newEvidence.put (v, val);
      }
    }
    //return new edu.ucla.belief.BeliefNetworkImpl (varToDist);

	newBn.induceGraph( varToDist );
	return newBn;
  }

}
