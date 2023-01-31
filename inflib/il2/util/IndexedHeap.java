package il2.util;

public abstract class IndexedHeap extends Heap{
    int[] heapLocation;
    int[] heapContents;

    public IndexedHeap(){
	super();
    }
    public void initialize(IntSet heapElements){
        heapContents=new int[heapElements.size()];
	if(heapElements.size()==0){
	    heapLocation=new int[0];
	}else{
	    heapLocation=new int[heapElements.get(heapElements.size()-1)+1];
	}
	java.util.Arrays.fill(heapLocation,-1);
	for(int i=0;i<heapContents.length;i++){
	    heapContents[i]=heapElements.get(i);
	    heapLocation[heapContents[i]]=i;
	}
	super.initialize(heapContents.length);
    }
	
    protected void initialize(int size){
	heapLocation=ArrayUtils.sequence(size);
	heapContents=ArrayUtils.sequence(size);
	super.initialize(size);
    }

    public int removeBest(){
	int result=heapContents[0];
	removeTop();
	return result;
    }

    protected final void initialize(){
	initialize(heapContents.length);
    }

    protected abstract boolean hasBetterValue(int x,int y);

    protected final boolean isBetter(int i,int j){
	return hasBetterValue(heapContents[i],heapContents[j]);
    }

    protected void swap(int i,int j){
	int x=heapContents[i];
	int y=heapContents[j];
	heapContents[i]=y;
	heapContents[j]=x;
	heapLocation[x]=j;
	heapLocation[y]=i;
    }

    public void valueUpdated(int x){
	valueChanged(heapLocation[x]);
    }
}
