package il2.inf.structure;

import java.util.*;
import java.io.*;
import java.math.BigInteger;

import il2.util.*;
import il2.model.*;
import il2.model.*;
import il2.bridge.*;

/**
 * A binary jointree basically.  When the tree is
 */
public class DGraph extends JoinTree
{
    Map cutsets;
    UPair root;

    private BigInteger largestCutsetSize;
    private int height;
    private int diameter;

    public static final String STR_PREFIX_ROOT = "ROOT ";
    public static final String STR_PREFIX_INTERNAL = "I ";
    public static final String STR_PREFIX_LEAF = "L ";
    public static final String STR_CACHE_FALSE = "cachefalse";
    public static final String STR_CACHE_TRUE = "cachetrue";
    public static final String STR_TYPE_EVIDENCE_INDICATOR = "ind";
    public static final String STR_TYPE_CPT = "cpt";

    /**
    	@author Keith Cascio
    	@since 120103
    */
    public void write( PrintWriter out, Set cachedNodes, Converter verter )
    {
    	new DGraph.Writer( out, cachedNodes, verter ).write();
    }

    /**
    	@author Keith Cascio
    	@since 120403
    */
    private class Writer
    {
    	public Writer( PrintWriter stream, Set cachedNodes, Converter verter )
    	{
    		this.stream = stream;
    		this.cachedNodes = cachedNodes;
    		this.verter = verter;
    		this.sizeDomain = verter.getDomain().size();
    		this.sizeGraph = tree().size();
    	}

    	public void write()
    	{
		UPair root = root();
		stream.println( STR_PREFIX_ROOT + createGraphLabel( this.sizeGraph ) + " " + STR_CACHE_FALSE + " " + createGraphLabel( root.s1 ) + " " + createGraphLabel( root.s2 ) );

		int sizeDomain = verter.getDomain().size();
		write( new Pair( root.s1, root.s2 ) );
		write( new Pair( root.s2, root.s1 ) );
    	}

	public void write( Pair pair )
	{
		if( isLeaf( pair.s1 ) )
		{
			boolean flagIsIndicator = pair.s1 >= sizeDomain;
			int variableLabel = pair.s1;
			if( flagIsIndicator ) variableLabel -= sizeDomain;
			String id = verter.convert( variableLabel ).getID();
			String strType = flagIsIndicator ? STR_TYPE_EVIDENCE_INDICATOR : STR_TYPE_CPT;
			stream.println( STR_PREFIX_LEAF + createGraphLabel( pair.s1 ) + " " + id + " " + strType );
		}
		else
		{
			String strCached = cachedNodes.contains( pair ) ? STR_CACHE_TRUE : STR_CACHE_FALSE;
			Integer[] children = children( pair );

			stream.println( STR_PREFIX_INTERNAL + createGraphLabel( pair.s1 ) + " " + strCached + " " + createGraphLabel( children[0].intValue() ) + " " + createGraphLabel( children[1].intValue() ) );

			write( new Pair( children[0].intValue(), pair.s1 ) );
			write( new Pair( children[1].intValue(), pair.s1 ) );
		}
	}

	private String createGraphLabel( int s1 )
	{
		return Integer.toString( sizeGraph - s1 );
	}

    	private PrintWriter stream;
    	private Set cachedNodes;
    	private Converter verter;
    	private int sizeDomain;
    	private int sizeGraph;
    }

    /**
     * Creates a DGraph from the supplied tree and cluster labels.
     * Note that the node labeling contract is stricter for DGraphs than for
     * JoinTrees.  The leaf nodes need to be 0..(# of leaves)-1.
     **/
    public DGraph(edu.ucla.structure.Graph tree,Map clusterLabels){
	super(tree,clusterLabels);
	validate();
	createCutsets();
	findRoot();
	computeStats();
    }
    private void computeStats(){
	largestCutsetSize=BigInteger.ZERO;
	for(Iterator iter=cutsets.values().iterator();iter.hasNext();){
	    Index ind=(Index)iter.next();
	    largestCutsetSize = largestCutsetSize.max( ind.sizeBig() );
	}
	height=Math.max(height(null,new Pair(root.s1,root.s2)),
			height(null,new Pair(root.s2,root.s1)));
	diameter=0;
	Map heightCache=new HashMap(4*tree().size());
	for(int i=0;isLeaf(i);i++){
	    Pair n=new Pair(leafNeighbor(i).intValue(),i);
	    int h=height(heightCache,n);
	    if(h>diameter){
		diameter=h;
	    }
	}
    }

    public BigInteger largestCutsetSize(){
	return largestCutsetSize;
    }

    public BigInteger largestContextSize(){
	return largestSeparatorSize();
    }

    public int height(){
	return height;
    }

    public int diameter(){
	return diameter;
    }

    private int height(Map hts,Pair node){
        if(hts!=null){
	    Object h=hts.get(node);
	    if(h!=null){
		return ((Integer)h).intValue();
	    }
	}
	Integer[] ch=children(node);
	int result;
	if(ch.length==0){
	    result=1;
	}else{
	    Pair ch1=new Pair(ch[0].intValue(),node.s1);
	    Pair ch2=new Pair(ch[1].intValue(),node.s1);
	    result=1+Math.max(height(hts,ch1),height(hts,ch2));
	}
	if(hts!=null){
	    hts.put(node,new Integer(result));
	}
	return result;
    }
    private void createCutsets(){
	cutsets=new HashMap(4*tree().size());
	for(Iterator iter=separators().keySet().iterator();iter.hasNext();){
	    UPair p=(UPair)iter.next();
	    cutsets.put(new Pair(p.s1,p.s2),cluster(p.s1).forgetIndex(separator(p)));
	    cutsets.put(new Pair(p.s2,p.s1),cluster(p.s2).forgetIndex(separator(p)));
	}
    }
    /*private void createCutsets(){
	cutsets=new HashMap(4*tree().size());
	Index left, right;
	for(Iterator iter=separators().keySet().iterator();iter.hasNext();){
	    UPair p=(UPair)iter.next();
	    left = cluster(p.s1).forgetIndex(separator(p));
	    right = cluster(p.s2).forgetIndex(separator(p));
	    left.setLongSizes( true );
	    right.setLongSizes( true );
	    cutsets.put(new Pair(p.s1,p.s2),left);
	    cutsets.put(new Pair(p.s2,p.s1),right);
	}
    }*/

    private void findRoot(){
	Integer top=new Integer(tree().size()-1);
	Set neighbors=tree().neighbors(top);
	Integer n=largest(neighbors);
	root=new UPair(top.intValue(),n.intValue());
    }

    private Integer largest(Set s){
	Iterator iter=s.iterator();
	Integer best=(Integer)iter.next();
	while(iter.hasNext()){
	    Integer current=(Integer)iter.next();
	    if(current.intValue()>best.intValue()){
		best=current;
	    }
	}
	return best;
    }

    private void validate(){
	if(!edu.ucla.structure.Graphs.isTree(tree())){
	    throw new IllegalStateException("Not a tree");
	}
	for(Iterator iter=tree().iterator();iter.hasNext();){
	    Object node=iter.next();
	    Set neighbors=tree().neighbors(node);
	    if(neighbors.size()!=1 && neighbors.size()!=3){
		throw new IllegalStateException(""+neighbors.size()+" neighbors");
	    }
	}
    }

    public Index cutset(Pair p){
	return (Index)cutsets.get(p);
    }

    public Index context(Pair p){
	return separator(p.s1,p.s2);
    }

    public UPair root(){
	return root;
    }

}
