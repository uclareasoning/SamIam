package il2.inf.jointree;

import il2.util.*;
import il2.model.*;
import il2.inf.structure.*;

import java.util.*;
import java.math.BigInteger;

public abstract class JoinTreeAlgorithm implements il2.inf.JointEngine{

    Domain domain;
    EliminationOrders.JT myJoinTree;

    edu.ucla.structure.Graph tree;//the tree's nodes are 0..n-1
    IntSet[] clusters;

    Map separators;
    private boolean isValid=false;

    Table[] originalTables;//The list of tables originally submitted
    Table[][] assignedTables;//The tables associated with each cluster

    int[] tableClusterAssignments;//gives the cluster the table is assigned to
    int[] assignmentIndex;//gives the order in the assignment for the appropriate cluster

    IntList[] containingClusters;//for each variable, the list of clusters containing it, ordered in increasing size
    IntSet[] assignedEvidence;//for each cluster, the variables who are assigned to the cluster when evidence is asserted.

    Pair[] messageOrder;
    IntMap evidence;
    int smallestCluster;

    protected double compilationTime=Double.NaN;
    protected double propagationTime=Double.NaN;

    public JoinTreeAlgorithm(EliminationOrders.JT jointree,Table[] tables)
    {
    	myJoinTree = jointree;
	domain=tables[0].domain();
	reduce(jointree);
	createSeparators();
	this.originalTables=(Table[])tables.clone();
	assignTables();
	computeMessageOrder();
	evidence=new IntMap();
    }

    private void reduce(EliminationOrders.JT jointree){
	Graph.Compressed c=jointree.tree.compress();
	Integer[] toInteger=edu.ucla.util.IntArrays.integerArray(c.graph.size());
	int[] new2old=c.mapping;
	clusters=new IntSet[new2old.length];
	Map oldClusterMap=jointree.clusters;
	for(int i=0;i<clusters.length;i++){
	    clusters[i]=(IntSet)oldClusterMap.get(toInteger[new2old[i]]);
	}
	tree=new edu.ucla.structure.HashGraph(c.graph.size());
	il2.util.Graph oldG=c.graph;
	for(int i=0;i<oldG.size();i++){
	    tree.add(toInteger[i]);
	    IntSet neighbors=oldG.neighbors(i);
	    for(int j=0;j<neighbors.size();j++){
		tree.addEdge(toInteger[i],toInteger[neighbors.get(j)]);
	    }
	}
    }

    /** @since 012904 */
    public static IntSet[] createClusters( Graph.Compressed c, Integer[] toInteger, Map oldClusterMap )
    {
    	int[] new2old = c.mapping;
    	IntSet[] ret = new IntSet[new2old.length];
 	for(int i=0;i<ret.length;i++){
 	    ret[i]=(IntSet)oldClusterMap.get(toInteger[new2old[i]]);
	}
	return ret;
    }

    /** @since 012904 */
    public static edu.ucla.structure.Graph createTree( Graph.Compressed c, Integer[] toInteger )
    {
	edu.ucla.structure.Graph tree = new edu.ucla.structure.HashGraph(c.graph.size());
	il2.util.Graph oldG = c.graph;
	for(int i=0;i<oldG.size();i++){
	    tree.add(toInteger[i]);
	    IntSet neighbors=oldG.neighbors(i);
	    for(int j=0;j<neighbors.size();j++){
		tree.addEdge(toInteger[i],toInteger[neighbors.get(j)]);
	    }
	}
	return tree;
    }

    /** @since 012904 */
    public static Map createSeparators( IntSet[] clusters, edu.ucla.structure.Graph tree )
    {
    	Map separators = new HashMap( 2*clusters.length );
	for(Iterator iter=tree.iterator();iter.hasNext();){
	    Object node1=iter.next();
	    int n1=node1.hashCode();//saves on the type casting
	    for(Iterator niter=tree.neighbors(node1).iterator();niter.hasNext();){
		int n2=niter.next().hashCode();
		if(n1<n2){
		    UPair p=new UPair(n1,n2);
		    separators.put(p,clusters[n1].intersection(clusters[n2]));
		}
	    }
	}
	return separators;
    }

    private void createSeparators(){
	separators=new HashMap(2*clusters.length);
	for(Iterator iter=tree.iterator();iter.hasNext();){
	    Object node1=iter.next();
	    int n1=node1.hashCode();//saves on the type casting
	    for(Iterator niter=tree.neighbors(node1).iterator();niter.hasNext();){
		int n2=niter.next().hashCode();
		if(n1<n2){
		    UPair p=new UPair(n1,n2);
		    separators.put(p,clusters[n1].intersection(clusters[n2]));
		}
	    }
	}
    }

    protected void assignTables(){
	tableClusterAssignments=new int[originalTables.length];
	assignmentIndex=new int[originalTables.length];
	int[] smallestClusters=orderClusters();
	containingClusters=createContainmentLists(smallestClusters);
	int[] assignment=tableClusterAssignments;
	int[] assignmentCount=new int[tree.size()];
	smallestCluster=smallestClusters[0];
	for(int i=0;i<originalTables.length;i++){
	    IntSet vars=originalTables[i].vars();
	    if(vars.size()==0){
		assignment[i]=smallestCluster;
		assignmentCount[smallestCluster]++;
	    }else{
		IntList candidates=containingClusters[vars.get(0)];
	        boolean found=false;
		for(int j=0;j<candidates.size();j++){
		    int c=candidates.get(j);
		    if(clusters[c].containsAll(vars)){
			assignment[i]=c;
			assignmentCount[c]++;
			found=true;
			break;
		    }
		}
		if(!found){
		    throw new IllegalStateException();
		}
	    }
	}
	assignedTables=new Table[clusters.length][];
	for(int i=0;i<assignmentCount.length;i++){
	    assignedTables[i]=new Table[assignmentCount[i]];
	    assignmentCount[i]=0;
	}
	for(int i=0;i<assignment.length;i++){
	    int a=assignment[i];
	    assignmentIndex[i]=assignmentCount[a]++;
	    assignedTables[a][assignmentIndex[i]]=originalTables[i];
	}
	assignedEvidence=new IntSet[clusters.length];
	for(int i=0;i<assignedEvidence.length;i++){
	    assignedEvidence[i]=new IntSet();
	}
	for(int i=0;i<containingClusters.length;i++){
	    if(containingClusters[i]!=null && containingClusters[i].size()>0){
		assignedEvidence[containingClusters[i].get(0)].appendAdd(i);
	    }
	}
    }

    private IntList[] createContainmentLists(int[] sortedClusters){
	IntList[] result=new IntList[domain.size()];
	for(int i=0;i<sortedClusters.length;i++){
	    IntSet is=clusters[sortedClusters[i]];
	    for(int j=0;j<is.size();j++){
		int v=is.get(j);
		if(result[v]==null){
		    result[v]=new IntList();
		}
		result[v].add(sortedClusters[i]);
	    }
	}
	return result;
    }

    private int[] orderClusters(){
	double[] sizes=new double[clusters.length];
	for(int i=0;i<sizes.length;i++){
	    sizes[i]=domain.size(clusters[i]);
	}
	return ArrayUtils.sortedInds(sizes);
    }

    private void computeMessageOrder(){
	messageOrder=new Pair[separators.size()];
	Object start=tree.iterator().next();
	if(computeMessageOrder(start,null,0)!=messageOrder.length){
	    throw new IllegalStateException();
	}
    }

    private int computeMessageOrder(Object from,Object excluding,int currentIndex){
	int fnode=from.hashCode();
	for(Iterator iter=tree.neighbors(from).iterator();iter.hasNext();){
	    Object n=iter.next();
	    if(!n.equals(excluding)){
		currentIndex=computeMessageOrder(n,from,currentIndex);
		messageOrder[currentIndex]=new Pair(n.hashCode(),fnode);
		currentIndex++;
	    }
	}
	return currentIndex;
    }


    protected void makeValid(){
    	//System.out.println( "("+getClass().getName()+")JoinTreeAlgorithm.makeValid()" );
	if(!isValid){
	    long start=System.currentTimeMillis();
	    initialize();
	    for(int i=0;i<messageOrder.length;i++){
		sendMessage(i,true);
	    }
	    for(int i=messageOrder.length-1;i>=0;i--){
		sendMessage(i,false);
	    }
	    isValid=true;
	    long finish=System.currentTimeMillis();
	    propagationTime=(finish-start)/1000.0;
	}
    }

    public double prEvidence(){
	makeValid();
	return computePrE();
    }

    public double logPrEvidence(){
	return Math.log(prEvidence());
    }

    public Table tableJoint(int table){
	makeValid();
	return computeTableJoint(table);
    }

    public Table tableConditional(int table){
	//Table t=computeTableJoint(table); // AC: bug?
	Table t=tableJoint(table);
	t.normalizeInPlace();
	return t;
    }


    public Table varJoint(int var){
	makeValid();
	return computeVarJoint(var);
    }

    public Table varConditional(int var){
	Table t=varJoint(var);
	t.normalizeInPlace();
	return t;
    }

    public void setEvidence(IntMap e){
	isValid=false;
	this.evidence=e;
    }

    public void setTable(int i,Table t){
	Table oldT=originalTables[i];
	if(t.vars().size()!=oldT.vars().size() || !t.vars().containsAll(oldT.vars())){
	    throw new IllegalArgumentException("Incompatible table");
	}
	originalTables[i]=t;
	assignedTables[tableClusterAssignments[i]][assignmentIndex[i]]=t;
	isValid=false;
    }

    /** @since 012904 */
    public EliminationOrders.JT getJoinTree()
    {
    	return myJoinTree;
    }

    /** @since 061504 */
    public Table[] getOriginalTables(){
		return originalTables;
	}

    //private static final double INVERSE_LN_2 = ((double)1) / Math.log( (double)2 );

    /** @since 012904 */
    public static JoinTreeStats.Stat getStats( IntSet[] members, Domain domain )
    {
	double largest = (double)0;
	BigInteger total = BigInteger.ZERO;
	for(int i=0;i<members.length;i++){
	    double currentSize = domain.size(members[i]);
	    if(currentSize>largest){
		largest=currentSize;
	    }
	    total = total.add( BigInteger.valueOf( (long)currentSize ) );//total+=currentSize;
	}
	//return new BigInteger[]{ BigInteger.valueOf( (long)(Math.log(largest) * INVERSE_LN_2) ), BigInteger.valueOf( (long)total ) };
	return new JoinTreeStats.StatImpl( BigInteger.valueOf( (long)largest ), JoinTreeStats.logBaseTwo( largest ), total );
    }

    private JoinTreeStats.Stat getStats(IntSet[] members){
    	return getStats( members, domain );
    }

    public JoinTreeStats.Stat getClusterStats(){
	return getStats(clusters);
    }

    public JoinTreeStats.Stat getSeparatorStats(){
	IntSet[] is=new IntSet[separators.size()];
	separators.values().toArray(is);
	return getStats(is);
    }

    protected abstract Table computeVarJoint(int var);
    protected abstract Table computeTableJoint(int var);
    protected abstract double computePrE();
    protected abstract void initialize();
    protected abstract void sendMessage(int ind,boolean isInward);

    public double getCompilationTime(){
	return compilationTime;
    }
    public double getPropagationTime(){
	return propagationTime;
    }
    public abstract double getMemoryRequirements();

  /**
   * Counts the nodes and edges of the AC that is encoded by the jointree.
   *
   * @return the number of nodes (index 0) and edges (index 1).
   * @author Mark Chavira
   * @since 011605
   */

  public double[] acStats () {

    // The node count is at index 0, and the edge count is at index 1.  We
    // will have use to refer to clusters by Integer in addition to int.  We
    // will also have use to know which cluster corresponds to the root.

    double[] ans = new double[2];
    Integer[] toInteger =
      edu.ucla.util.IntArrays.integerArray (clusters.length);
    int rootCluster = ((Integer)tree.iterator ().next ()).intValue ();

    // Add one input node for each value of each variable and one for each
    // row of each CPT.  Add a + node for the root of the AC and a + node for
    // each instantiation of the separator variables of each separator.

    for (int i = 0; i < domain.size (); i++) {
      ans[0] += domain.size (i);
//ans[1] += domain.size (i);
    }
    for (int i = 0; i < originalTables.length; i++) {
      ans[0] += originalTables[i].values ().length;
//ans[1] += originalTables[i].values ().length;
    }
    ++ans[0];
    for (
     java.util.Iterator i = separators.values ().iterator ();
     i.hasNext ();) {
      ans[0] += domain.size ((IntSet)i.next ());
    }

    // Now traverse the clusters.  Add a * node for each instantiation of the
    // cluster variables.  For each * node, add an edge for each connected
    // separator, another for each attached table, another for each attached
    // evidence indicator, and, if the cluster is the root of the jointree,
    // one more.

    for (int i = 0; i < clusters.length; i++) {
      double numClusterInstantiations = domain.size (clusters[i]);
      ans[0] += numClusterInstantiations;
      ans[1] +=
        numClusterInstantiations *
        (tree.neighbors (toInteger[i]).size () +
        assignedTables[i].length +
        assignedEvidence[i].size () +
        (i == rootCluster ? 1 : 0));
    }

    // Return the answer.

    return ans;

  }

}
