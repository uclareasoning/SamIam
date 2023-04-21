package il2.inf.structure;

import il2.inf.jointree.JoinTreeAlgorithm;
import il2.util.*;
import il2.model.*;

import java.util.*;
//{superfluous} import java.math.BigInteger;

public class EliminationOrders{

    public static class Record{
	public final IntList order;
	public final double size;
        public final IntSet[] clusters;
	public Record(IntList o,double s,IntSet[] c){
	    order=o;
	    size=s;
            clusters=c;
	    order.lock();
	}
    }

    public static double computeSize(Collection subDomains,IntList order){
        return createRecord(subDomains,order).size;
    }
    public static Record createRecord(Collection subDomains,IntList order){
        IntSet[] clusters=new IntSet[order.size()];
        Bucketer b=new Bucketer(order);
        b.placeInBuckets(subDomains);
        double biggest=0;
        Domain d=((SubDomain)subDomains.iterator().next()).domain();
        for(int i=0;i<order.size();i++){
            ArrayList l=b.getBucket(i);
            Index ind=createIndex(l);
            double size=d.logSize(ind.vars());
            clusters[i]=ind.vars();
            if(size>biggest){
                biggest=size;
            }
            b.placeInBucket(ind.forgetIndex(order.get(i)));
        }
        return new Record(order,biggest,clusters);
    }
    private static Index createIndex(Collection sds){
        Domain d=null;
        IntSet elements=new IntSet();
        for(Iterator iter=sds.iterator();iter.hasNext();){
            SubDomain sd=(SubDomain)iter.next();
            elements=elements.union(sd.vars());
            d=sd.domain();
        }
        return new Index(d,elements);
    }

    public static Record minSize(Collection subDomains){
	Graph g=compatibilityGraph(subDomains);
	Domain d=((SubDomain)subDomains.iterator().next()).domain();
	MinSizeOrderGenerator og=new MinSizeOrderGenerator(g,d);
	return new Record(og.getOrder(),og.eliminationSize(),og.getClusters());
    }

    /**
     * Returns the set of vairables in the given subdomains.
     * @author Mark Chavira
     */

    private static IntSet variables (Collection subDomains) {
      Domain d = ((SubDomain)subDomains.iterator().next()).domain();
      boolean[] marked = new boolean[d.size ()];
      IntSet ans = new IntSet ();
      for (Iterator i = subDomains.iterator (); i.hasNext ();) {
        IntSet vars = ((SubDomain)i.next ()).vars ();
        for (int j = 0; j < vars.size (); j++) {
          int v1 = vars.get (j);
          if (!marked[v1]) {
            marked[v1] = true;
            ans.add (v1);
          }
        }
      }
      return ans;
    }

    /**
     * The old version of minfill which should not be used any more unless
     * there is a very good reason.
     */

    public static Record oldMinFill(Collection subDomains){
	Graph g=compatibilityGraph(subDomains);
	Domain d=((SubDomain)subDomains.iterator().next()).domain();
	MinFillOrderGenerator og=new MinFillOrderGenerator(g,d);
	return new Record(og.getOrder(),og.eliminationSize(),og.getClusters());
    }

    public static Record minFill( Collection subDomains, int reps, Random seed ){
	return minfill2( subDomains, new IntSet[]{ variables(subDomains) }, reps, seed );
    }

    /**
     * Replacing JD's original minfill with a much faster version.
     * @author Mark Chavira
     */
    public static Record minfill2( Collection subDomains, IntSet[] vars, int reps, Random seed ){

      if( reps < 1 ) throw new IllegalArgumentException( "reps must be >= 1" );

      // Create the moral graph.

      Domain d = ((SubDomain)subDomains.iterator().next()).domain();
      IntSet[] neighbors = new IntSet[d.size ()];
      for (int i = 0; i < neighbors.length; i++) {
        neighbors[i] = new IntSet ();
      }
      for (Iterator i = subDomains.iterator (); i.hasNext ();) {
        IntSet cptVars = ((SubDomain)i.next ()).vars ();
        for (int j = 0; j < cptVars.size (); j++) {
          int v1 = cptVars.get (j);
          for (int k = j + 1; k < cptVars.size (); k++) {
            int v2 = cptVars.get (k);
            neighbors[v1].add (v2);
            neighbors[v2].add (v1);
          }
        }
      }

      // Package up the parameters to the minfill2 algorithm.

      int[][] g = new int[neighbors.length][];
      int[] cardinalities = new int[neighbors.length];
      for (int i = 0; i < neighbors.length; i++) {
        cardinalities[i] = d.size (i);
        g[i] = neighbors[i].toArray ();
      }
      int[][] partition = new int[vars.length][];
      for (int i = 0; i < partition.length; i++) {
        partition[i] = vars[i].toArray ();
      }

      // Call minfill2 and return the result.

      if( seed == null ) seed = new Random();

      try {
        il2.inf.structure.minfill2.MinfillEoe engine = new il2.inf.structure.minfill2.MinfillEoe();
        int[] ans = null, best = null, worst = null;
        double logmaxclustersize = Double.NaN, min = Double.MAX_VALUE, max = (double)0;
        for( int i=0; i<reps; i++ ){
          ans = engine.order( seed, cardinalities, g, partition );
          logmaxclustersize = il2.inf.structure.minfill2.Util.logMaxClusterSize( ans, cardinalities, g );
          if( logmaxclustersize < min ){
            min = logmaxclustersize;
            best = ans;
          }
          if( logmaxclustersize > max ){
            max = logmaxclustersize;
            worst = ans;
          }
        }
        //System.out.println( "minfill2, best of " + reps + ": " + min + ", worst: " + max );
        return createRecord( subDomains, new IntList( best ) );
      }catch( Exception e ){
        System.err.println( e );
        return null;
      }

    }

    public static Record constrainedMinFill (Collection subDomains,IntSet eliminateLast) {
      IntSet all = variables (subDomains);
      IntSet eliminateFirst = new IntSet ();
      for (int i = 0; i < all.size (); i++) {
        if (!eliminateLast.contains (all.get (i))) {
          eliminateFirst.add (all.get (i));
        }
      }
      return minfill2 (subDomains, new IntSet[] {eliminateFirst, eliminateLast}, 1, (Random)null );
    }

    /**
     * The old version of minfill which should not be used any more unless
     * there is a very good reason.
     */

    public static Record oldConstrainedMinFill(Collection subDomains,IntSet eliminateLast){
	Graph g=compatibilityGraph(subDomains);
	Domain d=((SubDomain)subDomains.iterator().next()).domain();
	ConstrainedMinFillOrderGenerator og=new ConstrainedMinFillOrderGenerator(g,d,eliminateLast);
	return new Record(og.getOrder(),og.eliminationSize(),og.getClusters());
    }

    public static Record constrainedMinSize(Collection subDomains,IntSet eliminateLast){
	Graph g=compatibilityGraph(subDomains);
	Domain d=((SubDomain)subDomains.iterator().next()).domain();
	ConstrainedMinSizeOrderGenerator og=new ConstrainedMinSizeOrderGenerator(g,d,eliminateLast);
	return new Record(og.getOrder(),og.eliminationSize(),og.getClusters());
    }

    public static Record boundedConstrainedMinFill(Collection subDomains, IntSet postponeSet, double weightBound){
	Graph g=compatibilityGraph(subDomains);
	Domain d=((SubDomain)subDomains.iterator().next()).domain();
	BoundedConstrainedOrderGenerator og=new BoundedConstrainedOrderGenerator(g,d,postponeSet,weightBound);
	return new Record(og.getOrder(),og.eliminationSize(),og.getClusters());
    }

    public static Record hardBoundedConstrainedFill(Collection subDomains, IntSet postponeSet, double weightBound){
	double high=weightBound;
	double low=0;
	Record rec=null;
	while(low+1<=high){
	    double mid=(low+high)/2;
	    Record r=boundedConstrainedMinFill(subDomains,postponeSet,mid);
	    System.err.println(r.size+"\t"+mid);
	    if(r.size<=weightBound){
		if(rec==null || rec.size<r.size){
		    rec=r;
		}
		low=mid;
	    }else{
		high=mid;
	    }
	}
	return rec;
    }
    public static Graph compatibilityGraph(Collection potentials){
	if(potentials.size()==0){
	    return new Graph(0);
	}
	Graph graph=new Graph(((SubDomain)potentials.iterator().next()).domain().size());
	for(Iterator iter=potentials.iterator();iter.hasNext();){
	    SubDomain d=(SubDomain)iter.next();
	    IntSet vars=d.vars();
	    if(vars.size()==1){
		graph.add(vars.get(0));
	    }else{
	        for(int i=0;i<vars.size();i++){
		    for(int j=i+1;j<vars.size();j++){
			graph.addEdge(vars.get(i),vars.get(j));
		    }
		}
	    }
	}
	return graph;
    }

	public static class JT implements JoinTreeStats.StatsSource, JTUnifier
	{
		public IntList order;
		public Map clusters;
		public Graph tree;
		public Domain domain;
		public il2.bridge.Converter converter;
		public BayesianNetwork network;
		public JoinTreeStats.Stat clusterStats;
		public JoinTreeStats.Stat separatorStats;
		private List myListEliminationOrder;

		private JT( IntList order, Graph t, Map c, Domain d, il2.bridge.Converter conv, BayesianNetwork bn2 )
		{
			//System.out.println( this+"()" );
			JT.this.order = order;
			clusters = c;
			tree = t;
			domain = d;
			converter = conv;
			network = bn2;
		}

		/** @since 061404 */
		public edu.ucla.belief.tree.JoinTree asJoinTreeIL1(){
			return null;
		}
		public il2.inf.structure.EliminationOrders.JT asJTIL2(){
			return this;
		}

		/** @since 012904 */
		public JoinTreeStats.Stat getClusterStats(){
			computeStats();
			return clusterStats;
		}

		/** @since 012904 */
		public JoinTreeStats.Stat getSeparatorStats(){
			computeStats();
			return separatorStats;
		}

		/** @since 012904 */
		private void computeStats()
		{
			if( clusterStats == null || separatorStats == null ){
				Graph.Compressed c = tree.compress();
				Integer[] toInteger = edu.ucla.util.IntArrays.integerArray( c.graph.size() );

				IntSet[] statClusters = JoinTreeAlgorithm.createClusters( c, toInteger, clusters );
				clusterStats = JoinTreeAlgorithm.getStats( statClusters, domain );

				edu.ucla.structure.Graph tree = JoinTreeAlgorithm.createTree( c, toInteger );
				Map separators = JoinTreeAlgorithm.createSeparators( statClusters, tree );

				IntSet[] statSeparators = new IntSet[separators.size()];
				separators.values().toArray( statSeparators );
				separatorStats = JoinTreeAlgorithm.getStats( statSeparators, domain );
			}
		}

		/** @since 020904 */
		public JT clone( il2.bridge.Converter conv, BayesianNetwork bn2 )
		{
			throw new UnsupportedOperationException();
			//JT ret = new JT( order, tree, clusters, conv.getDomain(), conv, bn2 );
			//ret.clusterStats = this.clusterStats;
			//ret.separatorStats = this.separatorStats;
			//return ret;
		}

		/** @since 20060224 */
		public List eliminationOrder(){
			if( JT.this.myListEliminationOrder == null ){
				JT.this.myListEliminationOrder = JT.this.converter.convert( JT.this.order );
			}
			return JT.this.myListEliminationOrder;
		}
	}

    private static class Edge{
        int v1;
        int v2;
        public Edge(int i,int j){
            if(i>j){
                v1=i;
                v2=j;
            }else{
                v1=j;
                v2=i;
            }
        }
        public boolean equals(Object obj){
            if(!(obj instanceof Edge)){
                return false;
            }
            Edge e=(Edge)obj;
            return v1==e.v1 && v2==e.v2;
        }
        public int hashCode(){
            return v1<<10 | v2;
        }
    }
    private static List removeRedundantClusters(IntSet[] clusters){
        List result=new ArrayList(clusters.length);
        for(int i=0;i<clusters.length;i++){
            boolean subsumed=false;
            for(int j=0;j<clusters.length;j++){
                if(i==j){
                    continue;
                }
                if(clusters[j].containsAll(clusters[i])){
                    if(clusters[j].size()!=clusters[i].size() || i>j){
                        subsumed=true;
                        break;
                    }
                }
            }
            if(!subsumed){
                result.add(clusters[i]);
            }
        }
        return result;
    }

    /**
        This method is safe to use only in the absence of a BayesianNetwork object.
        @since 021004
    */
    public static JT traditionalJoinTree(Collection subdomains){
    	return traditionalJoinTree( subdomains, minFill( subdomains, 1, (Random)null ).order, (il2.bridge.Converter)null, (BayesianNetwork)null );
    }
    /**
        This method is safe to use only in the absence of a BayesianNetwork object.
        @since 160105
    */
    public static JT traditionalJoinTree(Collection subdomains, IntList order){
    	return traditionalJoinTree( subdomains, order, (il2.bridge.Converter)null, (BayesianNetwork)null );
    }

	/**
	   This method produces a jointree using max-spanning tree on a
	   clique graph.  Therefore this approach to constructing a
	   jointree is at least quadratic space and time (in the number of
	   clusters).

	   Not suitable for large Bayesian networks (with thousands of
	   nodes).
	 */
    public static JT traditionalJoinTree( Collection subdomains, IntList order, il2.bridge.Converter c, BayesianNetwork bn2 )
    {
    	//System.out.println( "EliminationOrders.traditionalJoinTree( Collection, IntList, Converter, BayesianNetwork )" );
        Domain domain=((SubDomain)subdomains.iterator().next()).domain();
		if ( order.size() == 0 ) return trivialJoinTree(domain, c, bn2);
        Record rec=createRecord(subdomains,order);
        List clusters=removeRedundantClusters(rec.clusters);
        Edge[] edges =
                new Edge[(clusters.size() * (clusters.size() - 1)) / 2];
        double[] scores = new double[edges.length];
        int current = 0;
        for (int i = 0; i < clusters.size(); i++) {
            IntSet icluster=(IntSet)clusters.get(i);
            for (int j = i + 1; j < clusters.size(); j++, current++) {
                edges[current] = new Edge(i, j);
                IntSet s = icluster.intersection((IntSet)clusters.get(j));
                scores[current] = domain.size(s);
            }
        }
        edu.ucla.structure.Heap h = new edu.ucla.structure.Heap(edges, scores);
        edu.ucla.structure.UnionFind forrest = new edu.ucla.structure.UnionFind(clusters.size());
        Graph tree = new Graph(clusters.size());
        Map clusterMap = new HashMap(clusters.size());
        for (int i = 0; i < clusters.size(); i++) {
            tree.add(i);
            clusterMap.put(new Integer(i), clusters.get(i));
        }
        for (int i = 0; i < clusters.size() - 1;) {
            Edge e = (Edge) h.extractMax().element();
            if (forrest.find(e.v1) != forrest.find(e.v2)) {
                forrest.union(e.v1, e.v2);
                tree.addEdge(e.v1, e.v2);
                i++;
            }
        }
        return new JT( order, tree, clusterMap, domain, c, bn2 );
    }

	/** @since 012904 */
	public static JT traditionalJoinTree( BayesianNetwork bn2, il2.bridge.Converter c, IntList order )
	{
		//System.out.println( "EliminationOrders.traditionalJoinTree( "+bn2+" )" );
		Collection subdomains = Arrays.asList( bn2.cpts() );
		JT ret = traditionalJoinTree( subdomains, order, c, bn2 );
		//ret.network = bn2;
		//ret.converter = c;
		return ret;
	}

	/**
	 * this is for traditionalJoinTree, if it detects an instance with no variables
	 * (i.e., variable ordering is empty).
	 */
	private static JT trivialJoinTree(Domain domain, il2.bridge.Converter c, BayesianNetwork bn2) {
		IntList order = new IntList(0);
		Graph tree = new Graph(1);
		Map clusterMap = new HashMap(1);
		tree.add(0);
		clusterMap.put(new Integer(0), new IntSet(0));
		return new JT( order, tree, clusterMap, domain, c, bn2 );
	}

	public static double largestCptClique( Collection subDomains ) {
		Domain d = ((SubDomain)subDomains.iterator().next()).domain();
		double largest = 0.0, cur;
		for (Iterator i = subDomains.iterator (); i.hasNext ();) {
			IntSet cptVars = ((SubDomain)i.next ()).vars ();
			if ( cptVars.size() == 0 ) continue;
			// AC: need to check this?
			cur = d.logSize(cptVars) - d.logSize(cptVars.largest());
			if ( cur > largest ) largest = cur;
		}
		return largest;
	}

    public static int[] safePrefix( Collection subDomains ) {
		/*
		long time = System.currentTimeMillis();
		//minFill(subDomains,1,new Random());
		//int[] asdf = {1};
		//if ( true ) return asdf;
		time = System.currentTimeMillis() - time;
		// System.out.println("AC: minfill: " + time + " (ms)");
		time = System.currentTimeMillis();
		*/

		IntSet[] vars = new IntSet[]{ variables(subDomains) };

		// Create the moral graph.

		Domain d = ((SubDomain)subDomains.iterator().next()).domain();
		IntSet[] neighbors = new IntSet[d.size ()];
		for (int i = 0; i < neighbors.length; i++) {
			neighbors[i] = new IntSet ();
		}
		for (Iterator i = subDomains.iterator (); i.hasNext ();) {
			IntSet cptVars = ((SubDomain)i.next ()).vars ();
			for (int j = 0; j < cptVars.size (); j++) {
				int v1 = cptVars.get (j);
				for (int k = j + 1; k < cptVars.size (); k++) {
					int v2 = cptVars.get (k);
					neighbors[v1].add (v2);
					neighbors[v2].add (v1);
				}
			}
		}

		// Package up the parameters to the minfill2 algorithm.

		int[][] g = new int[neighbors.length][];
		int[] cardinalities = new int[neighbors.length];
		for (int i = 0; i < neighbors.length; i++) {
			cardinalities[i] = d.size (i);
			g[i] = neighbors[i].toArray ();
		}
		int[][] partition = new int[vars.length][];
		for (int i = 0; i < partition.length; i++) {
			partition[i] = vars[i].toArray ();
		}

		// Call minfill2 and return the result.

		int[] ans = null;
		try {
			il2.inf.structure.minfill2.MinfillEoe engine = 
				new il2.inf.structure.minfill2.MinfillEoe();
			int[] best = null, worst = null;
			double low = largestCptClique(subDomains);
			ans = engine.safeOrder( cardinalities, g, partition, low );
		} catch( Exception e ) {
			e.printStackTrace(); // AC
			System.err.println( e );
			return null;
		}

		// time = System.currentTimeMillis() - time;
		// System.out.println("AC: prefix : " + time + " (ms)");

		return ans;
    }

	/**
	 * This method is an alternative to traditionalJoinTree.  It
	 * induces a jointree from a trace of bucket elimination (variable
	 * elimination).  The space consumed is only linear.
	 */
    public static JT bucketerJoinTree(Collection subdomains, IntList order) {
        Domain domain = ((SubDomain)subdomains.iterator().next()).domain();

		int numvars = order.size();
        IntSet[] clusters=new IntSet[numvars+1];
        Graph tree = new Graph(clusters.length);
		tree.add(numvars); // jt vertex for empty cluster

		// this code is based on EliminationOrders.createRecord(...)
        Bucketer b=new Bucketer(order);
        b.placeInBuckets(subdomains);
        double biggest=0;
        for(int i=0;i<numvars;i++){
            ArrayList l=b.getBucket(i);
            Index ind=createIndex(l);
            double size=domain.logSize(ind.vars());
            clusters[i]=ind.vars();
            if(size>biggest){
                biggest=size;
            }
			int j = b.placeInBucket2(ind.forgetIndex(order.get(i)),-1);
			tree.addEdge(i,j);
        }
		order.lock();
		clusters[numvars] = new IntSet();

        Map clusterMap = new HashMap(clusters.length);
        for (int i = 0; i < clusters.length; i++)
            clusterMap.put(new Integer(i), clusters[i]);

		JT jointree = new JT( order, tree, clusterMap, domain, 
						(il2.bridge.Converter)null, (BayesianNetwork)null );
        return jointree;
    }
}
