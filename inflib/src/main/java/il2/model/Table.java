package il2.model;

import java.util.*;

import il2.util.*;

/** @author james d park
    @since 20031031 */
public class Table extends Index{
    double[] values;

    public static Table createCompatible(Index index){
	return new Table(index.domain(),index.vars());
    }

    public Table copy(){
	Table result=createCompatible(this);
	System.arraycopy(values,0,result.values,0,values.length);
	return result;
    }

    public Table squareEntries(){
	Table result=createCompatible(this);
	double[] nv=result.values();
	for(int i=0;i<nv.length;i++){
	    nv[i]=values[i]*values[i];
	}
	return result;
    }

    public Index createCompatibleIndex(){
        return new Index(domain(), vars());
    }
    public static Table evidenceTable(Domain d,int var,int val){
	Table t=varTable(d,var);
	t.values[val]=1;
	return t;
    }
    public static Table varTable(Domain d,int var){
	Table t=new Table(d,new IntSet(new int[]{var}));
	return t;
    }
    public static Table constantTable(Domain d,double val){
	Table t=new Table(d,new IntSet(new int[]{}));
	t.values[0]=val;
	return t;
    }

    public static Table indicatorTable(Domain d,int var){
	Table t=new Table(d,new IntSet(new int[]{var}));
	java.util.Arrays.fill(t.values,1);
	return t;
    }

    public static Table[] shrink(Table[] tables,IntMap evidence){
	Table[] result=new Table[tables.length];
	for(int i=0;i<tables.length;i++){
	    result[i]=tables[i].shrink(evidence);
	}
	return result;
    }

    public Table(Domain d,IntSet vars){
        super(d,vars);
	values=new double[this.sizeInt()];
    }

    public Table(Index index,double[] vals){
	super(index);
	values=vals;
    }

    public Table(Domain d,IntSet vars,double[] vals){
	super(d,vars);
	values=vals;
    }

    public void projectInto(Table big){
	int[][] boIndex=big.baselineOffsetIndex(this);
	projectInto(big,boIndex);
    }

    public void projectInto(Table big,int[][] boIndex){
	projectInto(big.values,values,boIndex);
    }

    public static void projectInto(double[] big,double[] small,int[][] boIndex){
	int[] base=boIndex[0];
	int[] offset=boIndex[1];
	java.util.Arrays.fill(small,0);
	for(int i=0;i<base.length;i++){
	    int b=base[i];
	    for(int j=0;j<offset.length;j++){
		small[i]+=big[b+offset[j]];
	    }
	}
    }

    public void maximizeInto(Table big,int[][] boIndex){
	maximizeInto(big.values,values,boIndex);
    }
    public static void maximizeInto(double[] big,double[] small,int[][] boIndex){
	int[] base=boIndex[0];
	int[] offset=boIndex[1];
	java.util.Arrays.fill(small,0);
	for(int i=0;i<base.length;i++){
	    int b=base[i];
	    for(int j=0;j<offset.length;j++){
		small[i]=Math.max(small[i],big[b+offset[j]]);
	    }
	}
    }

    public static void multiplyInto(double[] small,double[] big,int[][] boIndex){
	int[] base=boIndex[0];
	int[] offset=boIndex[1];
	for(int i=0;i<base.length;i++){
	    int b=base[i];
	    double v=small[i];
	    for(int j=0;j<offset.length;j++){
		big[b+offset[j]]*=v;
	    }
	}
    }

    public void multiplyInto(Table small){
	multiplyInto(small,baselineOffsetIndex(small));
    }
    public void multiplyInto(Table small,int[][] boIndex){
	multiplyInto(small.values,values,boIndex);
    }



    public Table forget(IntSet fvars){
	Index ind=complementaryIndex(fvars);
	int[] fc=ind.flipChange(this);
	int[] current=new int[vars.size()];
	double[] vals=new double[ind.sizeInt()];
	int sind=0;
	int bound=values.length-1;
	for(int i=0;i<bound;i++){
	    vals[sind]+=values[i];
	    sind+=fc[next(current)];
	}
	vals[sind]+=values[values.length-1];
	return new Table(ind.domain(),ind.vars(),vals);
    }


    public static Table[] miniMultiplyAndForget(Collection tables,int var,int size){
	Index[] indices=new Index[tables.size()];
	tables.toArray(indices);
	Partition p=Index.miniPartition(indices,size);
	Table[] result=new Table[p.indices.length];
	for(int i=0;i<result.length;i++){
	    result[i]=createCompatible(p.indices[i]);
	    java.util.Arrays.fill(result[i].values,1);
	}
	for(int i=0;i<indices.length;i++){
	    result[p.mappings[i]].multiplyInto((Table)indices[i]);
	}
	if(result[0].vars.contains(var)){
	    result[0]=result[0].forget(var);
	}else{
	    //I am not sure if that is necessary.  I need to check on it.
	    result[0].multiplyByConstant(result[0].domain.size(var));
	}
	for(int i=1;i<result.length;i++){
	    result[i]=result[i].maximize(var);
	}
	return result;
    }
    public static Table[] miniMultiplyAndForget(Collection tables,int var,int size,boolean sumSmallest){
	Index[] indices=new Index[tables.size()];
	tables.toArray(indices);
	Partition p=Index.miniPartition(indices,size);
	Table[] result=new Table[p.indices.length];
	for(int i=0;i<result.length;i++){
	    result[i]=createCompatible(p.indices[i]);
	    java.util.Arrays.fill(result[i].values,1);
	}
	for(int i=0;i<indices.length;i++){
	    result[p.mappings[i]].multiplyInto((Table)indices[i]);
	}

        int summer=0;
        for(int i=1;i<result.length;i++){
            if(sumSmallest){
                if(result[i].sizeInt()<result[summer].sizeInt()){
                    summer=i;
                }
            }else if(result[i].sizeInt()>result[summer].sizeInt()){
                summer=i;
            }
        }
        if(summer!=0){
            Table temp=result[0];
            result[0]=result[summer];
            result[summer]=temp;
        }
	if(result[0].vars.contains(var)){
	    result[0]=result[0].forget(var);
	}else{
	    //I am not sure if that is necessary.  I need to check on it.
	    result[0].multiplyByConstant(result[0].domain.size(var));
	}
	for(int i=1;i<result.length;i++){
	    result[i]=result[i].maximize(var);
	}
	return result;
    }

    public static Table[] miniMultiplyAndMaximize(Collection tables,int var,int size){
	Index[] indices=new Index[tables.size()];
	tables.toArray(indices);
	Partition p=Index.miniPartition(indices,size);
	Table[] result=new Table[p.indices.length];
	for(int i=0;i<result.length;i++){
	    result[i]=createCompatible(p.indices[i]);
	    java.util.Arrays.fill(result[i].values,1);
	}
	for(int i=0;i<indices.length;i++){
	    result[p.mappings[i]].multiplyInto((Table)indices[i]);
	}
	for(int i=0;i<result.length;i++){
	    result[i]=result[i].maximize(var);
	}
	return result;
    }

    public void multiplyByConstant(double val){
	for(int i=0;i<values.length;i++){
	    values[i]*=val;
	}
    }

    public static Table multiplyAll(Collection tables){
	Table t=createCompatible(Index.createBigIndex(tables));
	java.util.Arrays.fill(t.values,1);
	for(Iterator iter=tables.iterator();iter.hasNext();){
	    t.multiplyInto((Table)iter.next());
	}
	return t;
    }

    public Table multiply(Table t2){
	IntSet resVars=vars.union(t2.vars);
	Table result=new Table(domain,resVars);
	java.util.Arrays.fill(result.values,1);
	result.setToProduct(this,t2);
	return result;
    }

    public void setToProduct(Table[] tables){
	double[][] vals=new double[tables.length][];
	for(int i=0;i<vals.length;i++){
	    vals[i]=tables[i].values;
	}
	int[][] fc=new int[vals.length][];
	for(int i=0;i<vals.length;i++){
	    fc[i]=tables[i].flipChange(this);
	}
	int[] current=new int[vars.size()];
	int[] inds=new int[vals.length];
	int bound=values.length-1;
	for(int i=0;i<bound;i++){
	    values[i]=1;
	    for(int j=0;j<vals.length;j++){
		values[i]*=vals[j][inds[j]];
	    }
	    int f=next(current);
	    for(int j=0;j<inds.length;j++){
		inds[j]+=fc[j][f];
	    }
	}
	values[values.length-1]=1;
	for(int j=0;j<vals.length;j++){
	    values[values.length-1]*=vals[j][inds[j]];
	}
    }

    public void  projectInto2(Table big){
	double[] vals=big.values();
	int[] fc=flipChange(big);
	int[] current=new int[big.vars.size()];
	int ind=0;
 	int bound=big.sizeInt()-1;
	java.util.Arrays.fill(values,0);
	for(int i=0;i<bound;i++){
	    values[ind]+=vals[i];
	    int f=big.next(current);
	    ind+=fc[f];
	}
	values[ind]+=vals[bound];
    }
    public void multiplyInto2(Table t){
	double[] vals=t.values();
	int[] fc=t.flipChange(this);
	int[] current=new int[vars.size()];
	int ind=0;
	int bound=sizeInt()-1;
	for(int i=0;i<bound;i++){
	    values[i]*=vals[ind];
	    int f=next(current);
	    ind+=fc[f];
	}
	values[bound]*=vals[ind];
    }

    public void multiplyAndProjectInto(Table[] tables){
	java.util.Arrays.fill(values,0);
	double[][] vals=new double[tables.length][];
	for(int i=0;i<vals.length;i++){
	    vals[i]=tables[i].values;
	}
	List wholeThing=new ArrayList(tables.length+1);
	wholeThing.addAll(Arrays.asList(tables));
	wholeThing.add(this);
	Index big=Index.createBigIndex(wholeThing);
	int[][] fc=new int[vals.length][];
	for(int i=0;i<tables.length;i++){
	    fc[i]=tables[i].flipChange(big);
	}
	int[] destFc=flipChange(big);
	int destInd=0;
	int[] current=new int[big.vars().size()];
	int[] inds=new int[vals.length];
	int bound=big.sizeInt()-1;
	for(int i=0;i<bound;i++){
	   double v=1;
	    for(int j=0;j<vals.length;j++){
		v*=vals[j][inds[j]];
	    }
	    values[destInd]+=v;
	    int f=big.next(current);
	    for(int j=0;j<inds.length;j++){
		inds[j]+=fc[j][f];
	    }
	    destInd+=destFc[f];
	}
	double v=1;
	for(int j=0;j<vals.length;j++){
	    v*=vals[j][inds[j]];
	}
	values[destInd]+=v;
    }

	// AC: check this
    public void multiplyAndProjectMaxInto(Table[] tables){
	java.util.Arrays.fill(values,0);
	double[][] vals=new double[tables.length][];
	for(int i=0;i<vals.length;i++){
	    vals[i]=tables[i].values;
	}
	List wholeThing=new ArrayList(tables.length+1);
	wholeThing.addAll(Arrays.asList(tables));
	wholeThing.add(this);
	Index big=Index.createBigIndex(wholeThing);
	int[][] fc=new int[vals.length][];
	for(int i=0;i<tables.length;i++){
	    fc[i]=tables[i].flipChange(big);
	}
	int[] destFc=flipChange(big);
	int destInd=0;
	int[] current=new int[big.vars().size()];
	int[] inds=new int[vals.length];
	int bound=big.sizeInt()-1;
	for(int i=0;i<bound;i++){
	   double v=1;
	    for(int j=0;j<vals.length;j++){
		v*=vals[j][inds[j]];
	    }
	    //values[destInd]+=v;
		values[destInd] = Math.max(values[destInd],v);
	    int f=big.next(current);
	    for(int j=0;j<inds.length;j++){
		inds[j]+=fc[j][f];
	    }
	    destInd+=destFc[f];
	}
	double v=1;
	for(int j=0;j<vals.length;j++){
	    v*=vals[j][inds[j]];
	}
	//values[destInd]+=v;
	values[destInd] = Math.max(values[destInd],v);
    }

    public void setToProduct(Table t1,Table t2){
	double[] v1=t1.values;
	double[] v2=t2.values;
	int[] fc1=t1.flipChange(this);
	int[] fc2=t2.flipChange(this);
	int[] current=new int[vars.size()];
	int ind1=0;
	int ind2=0;
	int bound=values.length-1;
	for(int i=0;i<bound;i++){
	    values[i]=v1[ind1]*v2[ind2];
	    int f=next(current);
	    ind1+=fc1[f];
	    ind2+=fc2[f];
	}
	values[values.length-1]=v1[ind1]*v2[ind2];
    }
    public Table maximize(IntSet fvars){
	Index ind=complementaryIndex(fvars);
	int[] fc=ind.flipChange(this);
	int[] current=new int[vars.size()];
	double[] vals=new double[ind.sizeInt()];
	int sind=0;
	int bound=values.length-1;
	for(int i=0;i<bound;i++){
	    vals[sind]=Math.max(values[i],vals[sind]);
	    sind+=fc[next(current)];
	}
	vals[sind]=Math.max(values[values.length-1],vals[sind]);
	return new Table(ind.domain(),ind.vars(),vals);
    }

    public Table forget(int var){
	if(!vars.contains(var)){
	    return this;
	}
	IntSet nv=new IntSet(vars);
	nv.remove(var);
	Table result=new Table(domain,nv);
        sumOut(this, values, var, result.values());
	return result;
    }

    public static void sumOut(Index ind,double[] vals,int var,double[] dest){
	int vindex=ind.vars().indexOf(var);
	int bs=ind.stepSizes()[vindex];
	int valc=ind.sizes()[vindex];
	int current=0;
        int sz=ind.sizeInt();
        int dsz=sz/ind.domain().size(var);
        java.util.Arrays.fill(dest,0,dsz, 0.0);
	for(int i=0;i<sz;){
	    for(int j=0;j<valc;j++){
		int bound=current+bs;
		for(int k=current;k<bound;k++,i++){
		    dest[k]+=vals[i];
		}
	    }
	    current+=bs;
	}
    }

    public Table maximize(int var){
	if(!vars.contains(var)){
	    return this;
	}
	IntSet nv=new IntSet(vars);
	nv.remove(var);
	if(nv.size()==vars.size()){
	    return this;
	}
	Table result=new Table(domain,nv);
        maxOut(this, values, var, result.values());
	return result;
    }

    public Table minimize(int var){
	if(!vars.contains(var)){
	    return this;
	}
	IntSet nv=new IntSet(vars);
	nv.remove(var);
	if(nv.size()==vars.size()){
	    return this;
	}
	Table result=new Table(domain,nv);
        minOut(this, values, var, result.values());
	return result;
    }
    
    public static void maxOut(Index ind,double[] vals,int var,double[] dest){
        int destSize=ind.sizeInt()/ind.domain().size(var);
        java.util.Arrays.fill(dest,0,destSize,Double.NEGATIVE_INFINITY);
	int vindex=ind.vars().indexOf(var);
	int bs=ind.stepSizes()[vindex];
	int valc=ind.sizes()[vindex];
	int current=0;
        int sz=ind.sizeInt();
	for(int i=0;i<sz;){
	    for(int j=0;j<valc;j++){
		int bound=current+bs;
		for(int k=current;k<bound;k++,i++){
		    dest[k]=Math.max(dest[k],vals[i]);
		}
	    }
	    current+=bs;
	}
    }

    public static void minOut(Index ind,double[] vals,int var,double[] dest){
        int destSize=ind.sizeInt()/ind.domain().size(var);
        java.util.Arrays.fill(dest,0,destSize,Double.POSITIVE_INFINITY);
	int vindex=ind.vars().indexOf(var);
	int bs=ind.stepSizes()[vindex];
	int valc=ind.sizes()[vindex];
	int current=0;
        int sz=ind.sizeInt();
	for(int i=0;i<sz;){
	    for(int j=0;j<valc;j++){
		int bound=current+bs;
		for(int k=current;k<bound;k++,i++){
		    dest[k]=Math.min(dest[k],vals[i]);
		}
	    }
	    current+=bs;
	}
    }
    
    public void shrinkInto(Table sub,IntMap settings){
	shrinkInto(sub,settings,offsetIndex(sub));
    }

    public void shrinkInto(Table sub,IntMap settings,int[] offsetMapping){
	ArrayUtils.selectWithOffset(values,offsetMapping,offset(settings),sub.values);
    }


    public Table shrink(IntMap fixed){
	Index ind=shrinkIndex(fixed.keys());
	if(ind.vars.size()==vars.size()){
	    return this;
	}
	int offset=offset(fixed);
	int[] mapping=baselineIndex(ind);
        return new Table(ind,ArrayUtils.selectWithOffset(values,mapping,offset));
    }




    public double[] values(){
	return values;
    }

    public Table shrink(int var,int val){
	IntSet nv=new IntSet(vars);
	nv.remove(var);
	if(nv.size()==vars.size()){
	    return this;
	}
	Table result=new Table(domain,nv);
        selectOut(this, values, var, val, result.values());
	return result;
    }

    public static void selectOut(Index ind,double[] values,int var,int val,double[] dest){
	int vindex=ind.vars.indexOf(var);
	int bs=ind.stepSizes()[vindex];
	int valc=ind.sizes()[vindex];
	int bigBlock=bs*valc;
	int current=bs*val;
        int sz=ind.sizeInt()/ind.domain().size(var);
	for(int i=0;i<sz;){
	    int bound=current+bs;
	    for(int j=current;j<bound;j++,i++){
		dest[i]=values[j];
	    }
	    current+=bigBlock;
	}
    }

    public String toString(){
	StringBuffer buf=new StringBuffer(4*sizeInt()*vars.size());
	for(int i=0;i<vars.size();i++){
	    buf.append(domain.name(vars.get(i)));
	    buf.append('\t');
	}
	buf.append("Value\n");
	int[] current=new int[vars.size()];
	for(int row=0;row<values.length;row++){
	    for(int i=0;i<vars.size();i++){
		buf.append(domain.instanceName(vars.get(i),current[i]));
		buf.append('\t');
	    }
	    buf.append(values[row]);
	    buf.append('\n');
	    next(current);
	}
	return buf.toString();
    }

    public double sum(){
	double total=0;
	for(int i=0;i<values.length;i++){
	    total+=values[i];
	}
	return total;
    }

    public Table projectOnto(int var){
	int ind=vars().indexOf(var);
	int ss=stepSizes()[ind];
	double[] vals=new double[sizes()[ind]];
	int current=0;
	for(int i=0;i<values.length;){
	    double total=0;
	    for(int j=0;j<ss;j++,i++){
		total+=values[i];
	    }
	    vals[current]+=total;
	    current=(current+1)%vals.length;
	}
	return new Table(domain,IntSet.singleton(var),vals);
    }

    public void normalizeInPlace(){
	double total=sum();
	for(int i=0;i<values.length;i++){
	    values[i]/=total;
	}
    }

    public Table normalize(){
	Table t=copy();
	t.normalizeInPlace();
	return t;
    }
    public void invertInPlace(){
	for(int i=0;i<values.length;i++){
	    if(values[i]!=0){
		values[i]=1/values[i];
	    }
	}
    }
    public Table invert() {
        double[] vals = new double[values.length];
        for (int i = 0; i < vals.length; i++) {
            if ( values[i] == 0.0 )
                vals[i] = 0.0;
            else
                vals[i] = 1.0/values[i];
        }
        return new Table(this,vals);
    }

    public Table makeCPT(int var){
	return conditionalize(forgetIndex(var));
    }

    public Table conditionalize(Index fixedIndex){
	int[][] mapping=baselineOffsetIndex(fixedIndex);
	int[] fixedBaseline=mapping[0];
	int[] offsets=mapping[1];
	Table result=createCompatible(this);
        double[] nv=result.values;
	for(int i=0;i<fixedBaseline.length;i++){
	    int b=fixedBaseline[i];
	    double total=0;
	    for(int j=0;j<offsets.length;j++){
		total+=values[b+offsets[j]];
	    }
	    if(total==0){//degenerate case gets uniform
		double val=1.0/offsets.length;
		for(int j=0;j<offsets.length;j++){
		    nv[b+offsets[j]]=val;
		}
	    }else{
		double itotal=1/total;
		for(int j=0;j<offsets.length;j++){
		    int ind=b+offsets[j];
		    nv[ind]=values[ind]*itotal;
		}
	    }
	}
        return result;
    }

    public Table makeCPT2(int var){
	return conditionalize2(forgetIndex(var));
    }

    public Table conditionalize2(Index fixedIndex){
	int[][] mapping=baselineOffsetIndex(fixedIndex);
	int[] fixedBaseline=mapping[0];
	int[] offsets=mapping[1];
	Table result=createCompatible(this);
        double[] nv=result.values;
	for(int i=0;i<fixedBaseline.length;i++){
	    int b=fixedBaseline[i];
	    double total=0;
	    for(int j=0;j<offsets.length;j++){
		total+=values[b+offsets[j]];
	    }
	    if(total==0){//degenerate case gets uniform
		double val=1.0/offsets.length;
		for(int j=0;j<offsets.length;j++){
		    nv[b+offsets[j]]=val;
		}
	    }else{
		//double itotal=1/total;
		for(int j=0;j<offsets.length;j++){
		    int ind=b+offsets[j];
		    nv[ind]=values[ind]/total;
		}
	    }
	}
        return result;
    }


    public Table makeCDF(int freeVar){
	return makeCumulative(forgetIndex(freeVar));
    }

    public Table makeCumulative(Index fixedIndex){
	int[][] mapping=baselineOffsetIndex(fixedIndex);
	int[] fixedBaseline=mapping[0];
	int[] offsets=mapping[1];
	Table result=createCompatible(this);
        double[] nv=result.values;
	for(int i=0;i<fixedBaseline.length;i++){
	    int b=fixedBaseline[i];
	    double total=0;
	    for(int j=0;j<offsets.length;j++){
		int ind=b+offsets[j];
		total+=values[ind];
		nv[ind]=total;
	    }
	    if(total==0){//degenerate case gets uniform
		double val=1.0/offsets.length;
		for(int j=0;j<offsets.length;j++){
		    nv[b+offsets[j]]=(j+1)*val;
		}
	    }else{
		double itotal=1/total;
		for(int j=0;j<offsets.length;j++){
		    int ind=b+offsets[j];
		    nv[ind]=values[ind]*itotal;
		}
	    }
	}
        return result;
    }

    public void divideRelevantInto(double[] d){
	for(int i=0;i<values.length;i++){
	    if(d[i]==0){
		values[i]=0;
	    }else{
		values[i]/=d[i];
	    }
	}
    }

    public void multiplyVarIndicators(int var,int val){
	int ind=vars.indexOf(var);
	if(ind<0){
	    throw new IllegalArgumentException(var+" not contained");
	}
	if(val>=sizes[ind]){
	    throw new IllegalArgumentException("val wrong size");
	}
	double[] vals=new double[sizes[ind]];
	vals[val]=1;
	multiplyVarIndicators(var,vals);
    }

    public void multiplyVarIndicators(int var,double[] vals){
	int ind=vars.indexOf(var);
	if(ind<0){
	    throw new IllegalArgumentException(var+" not contained");
	}
	if(vals.length!=sizes[ind]){
	    throw new IllegalArgumentException("vals wrong size");
	}
	int ss=stepSizes()[ind];
	for(int i=0;i<values.length;){
	    for(int j=0;j<vals.length;j++){
		int bound=i+ss;
		for(;i<bound;i++){
		    values[i]*=vals[j];
		}
	    }
	}
    }






    public void zeroConciousMultiplyInto(Table t,boolean[] zc){
	double[] vals=t.values();
	int[] fc=t.flipChange(this);
	int[] current=new int[vars.size()];
	int ind=0;
	int bound=sizeInt()-1;
	for(int i=0;i<bound;i++){
	    if(vals[ind]==0 & zc[i]){
		zc[i]=false;
	    }else{
		values[i]*=vals[ind];
	    }
	    int f=next(current);
	    ind+=fc[f];
	}
	if(vals[ind]==0 && zc[bound]){
	    zc[bound]=false;
	}else{
	    values[bound]*=vals[ind];
	}
    }


    public void zeroConciousMultiplyVarIndicators(int var,int val,boolean[] zc){
	int ind=vars.indexOf(var);
	if(ind<0){
	    throw new IllegalArgumentException(var+" not contained");
	}
	int sz=sizes[ind];
	if(val>=sizes[ind]){
	    throw new IllegalArgumentException("val wrong size");
	}
	int ss=stepSizes()[ind];
	for(int i=0;i<values.length;){
	    for(int j=0;j<sz;j++){
		int bound=i+ss;
		if(j==val){
		    i=bound;
		    continue;
		}
		for(;i<bound;i++){
		    if(zc[i]){
			zc[i]=false;
		    }else{
			values[i]=0;
		    }
		}
	    }
	}
    }

    public void zeroConciousRealProjectInto(Table big,boolean[] zc){
	double[] vals=big.values();
	int[] fc=flipChange(big);
	int[] current=new int[big.vars.size()];
	int ind=0;
 	int bound=big.sizeInt()-1;
	java.util.Arrays.fill(values,0);
	for(int i=0;i<bound;i++){
	    if(zc[i]){
		values[ind]+=vals[i];
	    }
	    int f=big.next(current);
	    ind+=fc[f];
	}
	if(zc[bound]){
	    values[ind]+=vals[bound];
	}
    }
    public void zeroConciousProjectInto(Table big,boolean[] zc,boolean[] zcDest){
	java.util.Arrays.fill(zcDest,0,values.length,false);
	double[] vals=big.values();
	int[] fc=flipChange(big);
	int[] current=new int[big.vars.size()];
	int ind=0;
 	int bound=big.sizeInt()-1;
	java.util.Arrays.fill(values,0);
	for(int i=0;i<bound;i++){
	    if(zc[i]){
		if(zcDest[ind]){
		    values[ind]+=vals[i];
		}else{
		    zcDest[ind]=true;
		    values[ind]=vals[i];
		}
	    }else{
		if(!zcDest[ind]){
		    values[ind]+=vals[i];
		}
		//remaining case does nothing
	    }

	    int f=big.next(current);
	    ind+=fc[f];
	}
	if(zc[bound]){
	    if(zcDest[ind]){
		values[ind]+=vals[bound];
	    }else{
		zcDest[ind]=true;
		values[ind]=vals[bound];
	    }
	}else{
	    if(!zcDest[ind]){
		values[ind]+=vals[bound];
	    }
	    //remaining case does nothing
	}

    }
    public void zeroConciousDivideRelevantInto(double[] vals,boolean[] zc){
	for(int i=0;i<values.length;i++){
	    if(zc[i]){
		values[i]/=vals[i];
	    }else if(vals[i]!=0){
		values[i]=0;
	    }
	}
    }

    public void zeroConciousMakeReal(boolean[] zc){
	for(int i=0;i<values.length;i++){
	    if(!zc[i]){
		values[i]=0;
	    }
	}
    }

    public Table zeroConciousProjectOnto(int var,boolean[] zc,boolean[] zcDest){
	int ind=vars().indexOf(var);
	int ss=stepSizes()[ind];
	double[] vals=new double[sizes()[ind]];
	java.util.Arrays.fill(zcDest,0,vals.length,false);
	int current=0;
	for(int i=0;i<values.length;){
	    double total=vals[current];
	    boolean zfree=zcDest[current];
	    for(int j=0;j<ss;j++,i++){
		if(zc[i]){
		    if(zfree){
			total+=values[i];
		    }else{
			zfree=true;
			total=values[i];
		    }
		}else{
		    if(!zfree){
			total+=values[i];
		    }
		}
	    }
	    vals[current]=total;
	    zcDest[current]=zfree;
	    current=(current+1)%vals.length;
	}
	return new Table(domain,IntSet.singleton(var),vals);
    }

    public Table zeroConciousRealProjectOnto(int var,boolean[] zc){
	int ind=vars().indexOf(var);
	int ss=stepSizes()[ind];
	double[] vals=new double[sizes()[ind]];
	int current=0;
	for(int i=0;i<values.length;){
	    double total=0;
	    for(int j=0;j<ss;j++,i++){
		if(zc[i]){
		    total+=values[i];
		}
	    }
	    vals[current]+=total;
	    current=(current+1)%vals.length;
	}
	return new Table(domain,IntSet.singleton(var),vals);
    }
    public double getCompatibleEntry(int[] globalState){
	int ind=getIndexFromFullInstance(globalState);
	return values[ind];
    }
    public double dotProduct(Table t2){
	double[] tv=t2.values;
	double total=0;
	for(int i=0;i<values.length;i++){
	    total+=tv[i]*values[i];
	}
	return total;
    }

	/**
	 * simulates an instantiation according to the Table, assuming
	 * that it is a valid probability distribution.
	 */
	public int[] simulate(java.util.Random r) {
		int index = values.length-1; // default to last index
		double sum = 0.0;
		double p = r.nextDouble(); // p in [0,1)
		for ( int i = 0; i < values.length; i++ ) {
			sum += values[i];
			if ( p < sum ) {
				index = i;
				break;
			}
		}
		int[] inst = new int[vars.size()];
		setFullInstanceFromIndex(index,inst);
		return inst;
	}

	/**
	 * simulates an instantiation according to the Table, assuming
	 * that it is a valid probability distribution.
	 */
	public double simulate(java.util.Random r, int[] inst) {
		int index = values.length-1; // default to last index
		double sum = 0.0;
		double p = r.nextDouble(); // p in [0,1)
		for ( int i = 0; i < values.length; i++ ) {
			sum += values[i];
			if ( p < sum ) {
				index = i;
				break;
			}
		}
		setFullInstanceFromIndex(index,inst);
		return values[index];
	}

	public int maxIndex()
	{
		int best = 0;
		for ( int i = 0; i < values.length; i++ )
			if ( values[i] > values[best] ) best = i;
		return best;
	}

	/**
	 * returns assignment of variables to values with highest value
	 */
	public int[] maxAssignment() {
		int index = this.maxIndex();
		int[] inst = new int[vars.size()];
		setFullInstanceFromIndex(index,inst);
		return inst;
	}
}
