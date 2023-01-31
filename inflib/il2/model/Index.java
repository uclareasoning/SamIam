package il2.model;

import il2.util.*;

import java.math.BigInteger;

/** @author James Park
	@author Keith Cascio */
public class Index implements SubDomain
{
    Domain domain;
    IntSet vars;
    protected int[] sizes;
    protected int[] stepSizes;
    private boolean myFlagOverflowsInt = true;
    private long mySizeLong;
    private boolean myFlagOverflowsLong = true;
    private double mySizeDouble;
    private boolean myFlagOverflowsDouble = true;
    private BigInteger mySizeBig;

    public static final BigInteger BIGINTEGER_INT_MAX_VALUE = BigInteger.valueOf( (long)(Integer.MAX_VALUE-((int)2)) );
    public static final BigInteger BIGINTEGER_LONG_MAX_VALUE = BigInteger.valueOf( Long.MAX_VALUE-((long)2) );
    public static final BigInteger BIGINTEGER_DOUBLE_MAX_VALUE = BigInteger.valueOf( (long)(Double.MAX_VALUE-((double)2)) );

    public Index( Domain d, IntSet variables ){
	domain=d;
	vars=variables;
	sizes=d.getSizes(vars);
	mySizeBig = this.sizeBig();
	if( mySizeBig.max( BIGINTEGER_INT_MAX_VALUE ) == BIGINTEGER_INT_MAX_VALUE ){
		this.myFlagOverflowsInt = this.myFlagOverflowsLong = this.myFlagOverflowsDouble = false;
		this.stepSizes = ArrayUtils.cumProd( sizes );
		this.mySizeLong = mySizeBig.longValue();
		this.mySizeDouble = mySizeBig.doubleValue();
	}
	else if( mySizeBig.max( BIGINTEGER_LONG_MAX_VALUE ) == BIGINTEGER_LONG_MAX_VALUE ){
		this.myFlagOverflowsInt = true;
		this.myFlagOverflowsLong = this.myFlagOverflowsDouble = false;
		this.stepSizes = (int[])null;
		this.mySizeLong = mySizeBig.longValue();
		this.mySizeDouble = mySizeBig.doubleValue();
	}
	else if( mySizeBig.max( BIGINTEGER_DOUBLE_MAX_VALUE ) == BIGINTEGER_DOUBLE_MAX_VALUE ){
		this.myFlagOverflowsLong = this.myFlagOverflowsInt = true;
		this.myFlagOverflowsDouble = false;
		this.stepSizes = (int[])null;
		this.mySizeLong = (long)-1;
		this.mySizeDouble = mySizeBig.doubleValue();
	}
	else{
		this.myFlagOverflowsDouble = this.myFlagOverflowsLong = this.myFlagOverflowsInt = true;
		this.stepSizes = (int[])null;
		this.mySizeLong = (long)-1;
		this.mySizeDouble = (double)-1;
	}
    }

    public Index(Domain d,int var){
	this(d,IntSet.singleton(var));
    }

    public Index( Index ind ){
	this.domain = ind.domain;
	this.vars = new IntSet( ind.vars );
	this.sizes = (int[])ind.sizes.clone();
	if( ind.stepSizes != null ) this.stepSizes = (int[])ind.stepSizes.clone();
    	this.myFlagOverflowsInt = ind.myFlagOverflowsInt;
    	this.mySizeLong = ind.mySizeLong;
    	this.myFlagOverflowsLong = ind.myFlagOverflowsLong;
    	this.mySizeDouble = ind.mySizeDouble;
    	this.myFlagOverflowsDouble = ind.myFlagOverflowsDouble;
    	this.mySizeBig = ind.mySizeBig;
    }

    public Domain domain(){
	return domain;
    }

    public IntSet vars(){
	return vars;
    }

    public int[] sizes(){
	return sizes;
    }

    public int[] stepSizes(){
    	if( myFlagOverflowsInt ) throw new IllegalStateException( makeOverflowMessage( "int" ) );
	return stepSizes;
    }

    public int sizeInt(){
    	if( myFlagOverflowsInt ) throw new IllegalStateException( makeOverflowMessage( "int" ) );
	return stepSizes[stepSizes.length-1];
    }

    /** @author Keith Cascio
    	@since 020305 */
    public long sizeLong(){
    	if( myFlagOverflowsLong ) throw new IllegalStateException( makeOverflowMessage( "long" ) );
    	return mySizeLong;
    }

    /** @author Keith Cascio
    	@since 020805 */
    public double sizeDouble(){
    	if( myFlagOverflowsDouble ) throw new IllegalStateException( makeOverflowMessage( "double" ) );
    	return mySizeDouble;
    }

    /** @author Keith Cascio
    	@since 020305 */
    public BigInteger sizeBig(){
    	if( mySizeBig == null ) mySizeBig = ArrayUtils.cumProdLastBigInteger( sizes );
    	return mySizeBig;
    }

    /** @author Keith Cascio
    	@since 020805 */
    private String makeOverflowMessage( String type ){
    	StringBuffer buffer = new StringBuffer( 128 );
    	buffer.append( "Index size " );
    	buffer.append( sizeBig().toString() );
    	buffer.append( " overflows Java primitive type \"" );
    	buffer.append( type );
    	buffer.append( "\"" );
    	return buffer.toString();
    }

    public int[][] baselineOffsetIndex(Index subIndex){
        int[] baseline=baselineIndex(subIndex);
	int[] offset=baselineIndex(complementaryIndex(subIndex));
	return new int[][]{baseline,offset};
    }

    public final Index complementaryIndex(Index subIndex){
	return complementaryIndex(subIndex.vars);
    }

    public final Index shrinkIndex(IntSet shrinkVars){
	IntSet v2=vars.diff(shrinkVars);
	return new Index(domain,v2);
    }
    public final Index complementaryIndex(IntSet subvars){
	int[] exInds=vars.excludedIndices(subvars);
	IntSet variables=vars.subset(exInds);
	return new Index(domain,variables);
    }

    public int[] offsetIndex(Index subIndex){
	return baselineIndex(complementaryIndex(subIndex));
    }
    public int[] baselineIndex(Index subIndex){
	int[] inds=vars.indices(subIndex.vars);
	int[] weights=ArrayUtils.select(stepSizes,inds);
	//System.err.println("sub "+subIndex.size());
	//System.err.println("act "+size());
	if(subIndex.sizeInt()<0){
	    System.err.println(subIndex.vars());
	}
	int[] result=new int[subIndex.sizeInt()];
	for(int v=0;v<weights.length;v++){
	    int w=weights[v];
	    int ss=subIndex.stepSizes[v];
	    int vs=subIndex.sizes[v];
	    int ind=0;
	    int mult=0;
	    while(ind<result.length){
		int fin=ind+ss;
		int bw=w*mult;
		for(fin=ind+ss;ind<fin;ind++){
		    result[ind]+=bw;
		}
		mult=(mult+1)%vs;
	    }
	}
	return result;
    }

    public final int next(int[] current){
	for(int i=0;i<current.length;i++){
	    current[i]++;
	    if(current[i]==sizes[i]){
		current[i]=0;
	    }else{
		return i;
	    }
	}
	return -1;
    }

    /** @since 20060222 */
    public final int nextSafe(int[] current){
	for(int i=0;i<Index.this.sizes.length;i++){
	    current[i]++;
	    if(current[i]==sizes[i]){
		current[i]=0;
	    }else{
		return i;
	    }
	}
	return -1;
    }

    public int[] flipChange(Index bigIndex){
        int[] result=new int[bigIndex.vars.size()];
	int[] resets=new int[result.length];
	int[] increment=new int[result.length];
	int ourIndex=0;
	for(int i=0;i<result.length;i++){
	    if(ourIndex<vars.size() && bigIndex.vars.get(i)==vars.get(ourIndex)){
		increment[i]=stepSizes[ourIndex];
		resets[i]=stepSizes[ourIndex+1]-1;
		ourIndex++;
	    }else if(i>0){
		resets[i]=resets[i-1];
	    }
	}
	if(result.length!=0){
	    result[0]=increment[0];
	    for(int i=1;i<result.length;i++){
		result[i]=increment[i]-resets[i-1];
	    }
	}
	return result;
    }

    public final int offset(IntMap vals){
	int total=0;
	IntSet keys=vals.keys();
	IntList values=vals.values();
	for(int i=0;i<keys.size();i++){
	    int k=keys.get(i);
	    int ind=vars.indexOf(k);
	    if(ind>=0){
		total+=stepSizes[ind]*values.get(i);
	    }
	}
	return total;
    }


    public static Index createBigIndex(java.util.Collection indices){
	if(indices.size()==0){
	    throw new IllegalArgumentException("must not be empty");
	}
	java.util.Iterator iter=indices.iterator();
	Index first=(Index)iter.next();
	Domain d=first.domain();
	IntSet current=new IntSet(first.vars());
	while(iter.hasNext()){
	    current=current.union(((Index)iter.next()).vars());
	}
	return new Index(d,current);
    }

    public Index forgetIndex(int var){
	IntSet nv=new IntSet(vars);
	nv.remove(var);
	return new Index(domain,nv);
    }
    public Index forgetIndex(IntSet fvars){
        IntSet nv=vars.diff(fvars);
        return new Index(domain,nv);
    }

    public Index forgetIndex(Index ind){
	return forgetIndex(ind.vars());
    }

    public static class Partition{
	public final Index[] indices;
	public final int[] mappings;
	public Partition(Index[] inds,int[] assignments){
	    indices=inds;
	    mappings=assignments;
	}
    }

    public static Partition miniPartition(Index[] indices,int maxSize){
	Index[] result=new Index[indices.length];
	int[] mappings=new int[indices.length];
	int size=0;
	for(int i=0;i<indices.length;i++){
	    boolean found=false;
	    for(int j=0;!found && j<size;j++){
		Index bigInd=result[j].combineWith(indices[i]);
		if(bigInd.sizeInt()==result[j].sizeInt() || (bigInd.sizeInt()<maxSize && bigInd.sizeInt()>0)){
		    result[j]=bigInd;
		    mappings[i]=j;
		    found=true;
		}else{
		    //System.err.println(bigInd.vars()+"\t"+bigInd.size());
		}
	    }
	    if(!found){
		result[size]=new Index(indices[i]);
		mappings[i]=size;
		size++;
	    }
	}
        Index[] finalresult=new Index[size];
	System.arraycopy(result,0,finalresult,0,size);
	/*System.err.println("partition("+maxSize+")-----");
	for(int i=0;i<indices.length;i++){
	    System.err.println("assigned to "+mappings[i]+" "+indices[i].vars());
	}
	for(int i=0;i<finalresult.length;i++){
	    System.err.println(i+": "+finalresult[i].vars()+"\t"+finalresult[i].size());
	}
	System.err.println("end partition************");
	*/
	return new Partition(finalresult,mappings);
    }

    public Index separatorIndex(Index ind){
        IntSet v=vars.intersection(ind.vars);
        return new Index(domain,v);
    }

    public Index combineWith(Index ind){
	IntSet cvars=vars.union(ind.vars);
	return new Index(domain,cvars);
    }

    public String varString(){
	StringBuffer b=new StringBuffer(5*vars.size());
	for(int i=0;i<vars.size();i++){
	    b.append(domain.name(vars.get(i)));
	    b.append(' ');
	}
	return b.toString();
    }

    public int getIndexFromFullInstance(int[] inst){
	int ind=0;
	for(int i=0;i<vars.size();i++){
	    ind+=stepSizes[i]*inst[i];
	}
	return ind;
    }

    public void setFullInstanceFromIndex(int ind,int[] assignment){
	for(int i=vars.size()-1;i>=0;i--){
	    assignment[i]=ind/stepSizes[i];
	    ind%=stepSizes[i];
	}
    }
}
