package edu.ucla.belief.rc2.kb.sat;


//{superfluous} import edu.ucla.belief.rc2.kb.MultiValuedCNF;
import edu.ucla.belief.rc2.kb.KBMap;
import edu.ucla.belief.rc2.kb.KnowledgeBase;

//{superfluous} import edu.ucla.structure.MappedList;
//{superfluous} import edu.ucla.belief.FiniteVariable;


import java.util.*;
import java.io.*;

/** Class implements a knowledgebase using Prof. Darwiche's SAT engine.
 *
 *  @author David Allen
 */
public class KB_SAT
{

	private static boolean loaded;
	public static boolean loaded() { return loaded;}

	static {
		try{
			System.loadLibrary("darwiche_sat");
			loaded = true;
			load_library(KnowledgeBase.KB_UNSATISFIABLE);
		}
		catch(Throwable e) {
			loaded = false;
			System.err.println("Could not load library: " + e.getMessage());
		}
	}





	final private String fileName;
	final int map[][];


	private KB_SAT(KBMap.Mapping map) {
		fileName = map.file;
		this.map = map.map;
	}




	/*Will mark the library as inUse.*/
	static public KB_SAT createKB(KBMap.Mapping map) {

		if(!loaded) { return null;}

		KB_SAT ret = new KB_SAT(map);

		int r = ret.sat_createKB(ret.fileName);


		if(r==KnowledgeBase.KB_UNSATISFIABLE) {
			System.err.println("KB was unsatisfiable");
			return null;
		}
		else if(r<0) { System.err.println("ERROR (" + r + ") during createKB"); return null;}

		return ret;
	}

	public void releaseKB() {
		sat_releaseKB();
	}


	/* Returns:
	 *  2 : if bvar was already known in KB_SAT (so no error, but also no undo_decide call needed)
	 *  1 : if bvar was successfully set (will need to call undo_decide at some point)
	 *  0 : if -bvar was already known in KB_SAT (so no assert_level is set, but still failure)
	 * <0 : in backtracking state. already called undo_decide & assert_cd_literal. ret = -number of undo_decide calls (never 0).
	 */
	public int my_decide(int var, int val) {return my_decide(map[var][val]);}
	/* Returns:
	 *  2 : if bvar was already known in KB_SAT (so no error, but also no undo_decide call needed)
	 *  1 : if bvar was successfully set (will need to call undo_decide at some point)
	 *  0 : if -bvar was already known in KB_SAT (so no assert_level is set, but still failure)
	 * <0 : in backtracking state. already called undo_decide & assert_cd_literal. ret = -number of undo_decide calls (never 0).
	 */
	public native int my_decide(int bvar);

	public native int undo_decide();

	/* Returns:
	 *   1: If True
	 *   0: If Unknown
	 *  -1: if False
	 */
	public native int varStatus(int bvar);


	/*Will mark the library as inUse.*/
	private native int sat_createKB(String file);

	/*Will mark the library as !inUse.*/
	private native void sat_releaseKB();


	public native int numClauses();
	public native int numLiterals();


	static private native void load_library(int KB_UNSAT);

} //end class KB_SAT
