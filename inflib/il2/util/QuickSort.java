package il2.util;
public abstract class QuickSort{
    final int count;
    public QuickSort(int count){
        this.count=count;
    }
    public final void sort(){
        sort(0,count-1);
    }
    protected int selectPivot(int min,int max){
        return (min+max)/2;
    }
    protected abstract void swap(int i,int j);
    protected abstract boolean isLess(int i,int j);
    private final void sort(int min,int max){
        if(min<max){
            int q=partition(min,max);
            sort(min,q);
            sort(q+1,max);
        }
    }
    private final int partition(int min,int max){
        int pivot=selectPivot(min,max);
        swap(min,pivot);
        pivot=min;
        int i=min-1;
        int j=max+1;
        while(true){
            do{
                j--;
            }while(isLess(pivot,j));
            do{
                i++;
            }while(isLess(i,pivot));
            if(i<j){
                swap(i,j);
            }else{
                return j;
            }
        }
    }
}
