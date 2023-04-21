package il2.inf.edgedeletion;

import il2.util.*;
import il2.model.*;
import il2.inf.edgedeletion.EDEdgeDeleter;

public class EDNetwork {
	Table[] oldTables; // original (input) tables
	Domain  oldDomain; // original domain
	Table[] newTables; // original network tables + new ED tables
	Domain  newDomain; // new ED version of domain

	int[] oldToNewVar; // map from original var to new var
	int[] newToOldVar; // map from ED network var to original var
	int[] edgeToClone; // map from edge to clone var
	
	int[][] edgesDeleted;// maps edge index to original var index
	int ned;       // number of edges deleted
	int numtables; // number of original tables

	double networkConstructionTime;

	public EDNetwork(BayesianNetwork bn, int[][] edgesDeleted) {
		long start=System.nanoTime();

		this.oldTables = bn.cpts();
		this.oldDomain = bn.domain();
		prepareGivenEdges(edgesDeleted);
		createEDNetwork();

		long finish=System.nanoTime();
		networkConstructionTime=(finish-start)*1e-6;
	}

	public EDNetwork(BayesianNetwork bn) {
		this(bn,EDEdgeDeleter.getAllEdges(bn)); //AC: bn version or fg?
	}

	/****************************************
	 ***  statistics and settings functions
	 ****************************************/

	public int[][] edgesDeleted() { return edgesDeleted; }
	public Table[] newTables() { return newTables; }
	public Table[] oldTables() { return oldTables; }
	public Domain newDomain() { return newDomain; }
	public Domain oldDomain() { return oldDomain; }

	/****************************************
	 ***  Helper functions
	 ****************************************/

	/* returns index of var in new network */
	public int oldToNewVar(int var) {
		return oldToNewVar[var];
	}

	/* returns index of var in new network */
	public int newToOldVar(int var) {
		return newToOldVar[var];
	}

	public IntMap oldToNewEvidence(IntMap e) {
		IntMap f = new IntMap(e.size());
		IntSet vars = e.keys();
		for (int i = 0; i < vars.size(); i++) { // convert original evidence
			int var = vars.get(i);
			f.put(oldToNewVar[var],e.get(var));
		}
		return f;
	}

	/* returns parent of edge, original network index */
	public int oldVarOfEdge(int edge) {
		return edgesDeleted[edge][0];
	}

	/* returns child of edge, original network index */
	public int oldTableOfEdge(int edge) {
		return edgesDeleted[edge][1];
	}

	/* returns parent of edge, approximate network index */
	public int newVarOfEdge(int edge) { //AC
		//return oldVarToNewVar[ oldVarOfEdge(edge) ];
		return oldToNewVar[ edgesDeleted[edge][0] ];
	}

	public int cloneVarOfEdge(int edge) { //AC
		return edgeToClone[edge];
	}

	/* table index for edge head (clone) */
	public int tableIndexOfEdgeHead(int edge) {
		return numtables + edge;
	}

	/* table index for edge tail (orig var) */
	public int tableIndexOfEdgeTail(int edge) {
		return numtables + ned + edge;
	}

	// AC: need to check if these are proper prefixes
	private String cloneVarName(int edge) {
		return "u"+edge;
	}

	/****************************************
	 ***  Functions for constructing the shell (via CPTs) of the edge
	 ***  deleted network, given a set of edges to delete.  This is to
	 ***  prepare for parametrization.
	 ****************************************/

	private void constructNewDomain() {
		Domain oldd = this.oldDomain;
		Domain newd = new Domain(oldd.size()+ned);

		// initialize maps
		this.oldToNewVar = new int[ oldd.size() ];
		this.newToOldVar = new int[ oldd.size()+ned ];
		this.edgeToClone = new int[ ned ];

		int edge = 0;
		int newvar = 0;
		for (int oldvar = 0; oldvar < oldd.size(); oldvar++) {
			// add original network variable
			newd.addDim( oldd.name(oldvar), oldd.instanceNames(oldvar) );
			oldToNewVar[oldvar] = newvar;
			newToOldVar[newvar] = oldvar;
			newvar++;

			// add clone variables for deleted edges
			while ( edge < ned && edgesDeleted[edge][0] == oldvar ) {
				newd.addDim( cloneVarName(edge), oldd.instanceNames(oldvar) );
				newToOldVar[newvar] = oldvar;
				edgeToClone[edge] = newvar;
				newvar++;
				edge++;
			}
		}
		this.newDomain = newd;
	}

	private Table createEDTable(int var, double[] vals) {
		IntSet vars = new IntSet(1); vars.add(var);
		return new Table(newDomain, vars, vals);
	}

	private Table createUniformEDTable(int var) {
		int size = newDomain.size(var);
		double[] vals = new double[size];
		for (int i = 0; i < size; i++)
            //vals[i] = 1.0;
			//vals[i] = 1.0/Math.sqrt(size);
			vals[i] = 1.0/(double)size; // should be default for SamIam
			//vals[i] = 0.0; //logspace
		return createEDTable(var,vals);
	}

	/** 
	 * this copies the CPT of the original network, translating it
	 * into the new domain */
	public Table createNewNetworkCpt(Table oldTable, int table) {
		IntSet oldVars = oldTable.vars();
		IntSet newVars = new IntSet(oldVars.size());
		// IntSet incoming = tableToDeletedEdges[table];
		IntSet incoming = edgeDeletedIncomingTable(table);
		int var; int edge;

		// convert old vars to new vars
		for (int i = 0; i < oldVars.size(); i++) {
			var = oldVars.get(i);
			newVars.add(oldToNewVar[var]);
		}

		// replace parents with clones for deleted edges
		for (int i = 0; i < incoming.size(); i++) {
			edge = incoming.get(i);
			if ( oldVars.contains(oldVarOfEdge(edge)) ) {
				newVars.remove(newVarOfEdge(edge));
				newVars.add(cloneVarOfEdge(edge));
			} else {
				String errormsg = "unexpected edge" + 
					EDEdgeDeleter.edgeToString(edgesDeleted[edge]);
				throw new IllegalStateException(errormsg);
			}
		}
		return new Table(newDomain, newVars, oldTable.values());
	}

	/**
	 * this converts a table of the new network into a table of the
	 * old network.  Since ordering of new vars and clones is
	 * consistent with the ordering of the old vars, simply copy the
	 * values of the new table into a constructed old table.
	 */
	public Table newTableToOldTable(Table newTable) {
		IntSet newVars = newTable.vars();
		IntSet oldVars = new IntSet(newVars.size());

		int var;
		for (int i = 0; i < newVars.size(); i++) {
			var = newVars.get(i);
			oldVars.add(newToOldVar[var]);
		}

		return new Table(oldDomain, oldVars, newTable.values());
	}

	/**
	 * this function constructs the initial tables of an edge deletion
	 * network implicit in the newTables array.  the network tables,
	 * and the new ED tables are in fixed locations of newTables
	 * array.
	 */
	private void createEDNetwork() {
		Table[] tables = this.oldTables;
		this.newTables = new Table[numtables+ned*2];
		constructNewDomain();

		// the first ned tables are the network tables
		for (int i = 0; i < numtables; i++)
			newTables[i] = createNewNetworkCpt(tables[i],i);
		// the next ned tables are tables for edge head (clones)
		for (int i = 0; i < ned; i++)
			newTables[ tableIndexOfEdgeHead(i) ] 
				= createUniformEDTable( cloneVarOfEdge(i) );
		// the last ned tables are tables for edge tail (orig var)
		for (int i = 0; i < ned; i++)
			newTables[ tableIndexOfEdgeTail(i) ] 
				= createUniformEDTable( newVarOfEdge(i) );
	}

	/* does some setup */
	private void prepareGivenEdges(int[][] edges) {
		this.edgesDeleted = edges.clone();
		EDEdgeDeleter.sortEdges(this.edgesDeleted);
		this.ned = edges.length;
		this.numtables = this.oldTables.length;
	}
	
	IntSet[] tableToDeletedEdgesMap = null; // AC
	/* this sets up a mapped version of edgeDeletedIncomingTable */
	private IntSet[] mapDeletedNeighbors() {
		IntSet[] tableToDeletedEdges = new IntSet[numtables];
		for (int edge = 0; edge < this.numtables; edge++)
			tableToDeletedEdges[edge] = new IntSet();
		for (int edge = 0; edge < edgesDeleted.length; edge++)
			tableToDeletedEdges[oldTableOfEdge(edge)].add(edge);
		return tableToDeletedEdges;
	}

	/**
	 * this is used when converting network tables into the new
	 * network.  AC: this is potentially inefficient for large
	 * networks
	*/
	private IntSet edgeDeletedIncomingTable(int table) {
		if ( tableToDeletedEdgesMap == null )
			tableToDeletedEdgesMap = mapDeletedNeighbors();
		return tableToDeletedEdgesMap[table];
		/* // slow version:
		IntSet incoming = new IntSet();
		for (int edge = 0; edge < edgesDeleted.length; edge++)
			if ( edgesDeleted[edge][1] == table ) incoming.add(edge);
		return incoming;
		*/
	}

	/****************************************
	 ***  functions for parametrizing the edges of a network
	 ****************************************/

	public Table getCloneEdgeTable(int edge) {
		return newTableToOldTable(getPmTable(edge));
	}

	public Table getSoftEvidenceEdgeTable(int edge) {
		return newTableToOldTable(getSeTable(edge));
	}

	public Table getPmTable(int edge) {
		return newTables[ tableIndexOfEdgeHead(edge) ];
	}

	public Table getSeTable(int edge) {
		return newTables[ tableIndexOfEdgeTail(edge) ];
	}

	public void setPmTable(int edge, Table table) {
		newTables[ tableIndexOfEdgeHead(edge) ] = table;
	}

	public void setSeTable(int edge, Table table) {
		newTables[ tableIndexOfEdgeTail(edge) ] = table;
	}

	public void resetTables() {
		int v1, v2;
		for (int edge = 0; edge < ned; edge++) {
			setPmTable(edge,createUniformEDTable( cloneVarOfEdge(edge) ));
			setSeTable(edge,createUniformEDTable( newVarOfEdge(edge) ));
		}
	}

	public void resetTables(Table[] seed_pms, Table[] seed_ses) {
		int v1, v2;
		for (int edge = 0; edge < ned; edge++) {
            double[] pm_vals = seed_pms[edge].values();
            double[] se_vals = seed_ses[edge].values();
			setPmTable(edge,createEDTable(cloneVarOfEdge(edge), pm_vals));
            setSeTable(edge,createEDTable(newVarOfEdge(edge), se_vals));
		}
	}

	/* In ED, this is time to initalize the algorithm: e.g. construct
	 * the approximate network (but not parametrize it) */
    public double getConstructionTime() {
		return networkConstructionTime;
	}

}
