package il2.util;

public class Graph{
    IntSet[] edges;
    IntSet vertices;
    int vertexCount;
    private int largest=-1;
    private static int LARGEST_UNDEFINED=-2; 

    public Graph(int size){
	edges=new IntSet[size];
	vertexCount=0;
    }

    private Graph(IntSet[] n){
	edges=n;
	vertexCount=edges.length;
    }

    public static class Compressed{
	public final Graph graph;
	public final int[] mapping;
	private Compressed(Graph g,int[] m){
	    graph=g;
	    mapping=m;
	}
    }
    
    public Compressed compress(){
	int[] new2old=vertices().toArray();
	int[] old2new=new int[new2old[new2old.length-1]+1];
	for(int i=0;i<new2old.length;i++){
	    old2new[new2old[i]]=i;
	}
	IntSet[] newneighbs=new IntSet[new2old.length];
	for(int i=0;i<newneighbs.length;i++){
	    newneighbs[i]=convert(edges[new2old[i]],old2new);
	}
	Graph g=new Graph(newneighbs);
	return new Compressed(g,new2old);
    }
    private IntSet convert(IntSet old,int[] old2new){
	IntSet is=new IntSet(old.size());
	for(int i=0;i<old.size();i++){
	    is.appendAdd(old2new[old.get(i)]);
	}
	return is;
    }
    
    public IntSet roots(){
        IntSet result=new IntSet(30);
        for(int i=0;i<edges.length;i++){
            if(edges[i]!=null && (edges[i].size()==0 || edges[i].get(0)>i)){
                result.add(i);
            }
        }
        return result;
    }
    public IntSet leaves(){
        IntSet result=new IntSet(30);
        for(int i=0;i<edges.length;i++){
            if(edges[i]!=null && (edges[i].size()==0 || edges[i].largest()<i)){
                result.add(i);
            }
        }
        return result;
    }
    public boolean isConnected(IntSet vertices){
        IntSet notFound=new IntSet(vertices);
        int n=notFound.largest();
        notFound.remove(n);
        lookFor(n,notFound);
        return notFound.size()==0;
    }
    private void lookFor(int node,IntSet notFound){
        IntSet neighbors=neighbors(node);
        for(int i=0;i<neighbors.size();i++){
            int n=neighbors.get(i);
            if(notFound.contains(n)){
                notFound.remove(n);
                lookFor(n,notFound);
            }
        }
    }
    
    public boolean isTree(){
        if(vertexCount==0){
            return false;
        }
        boolean[] marked=new boolean[edges.length];
        int node=0;
        while(edges[node]==null){
            node++;
        }
        marked[node]=true;
        boolean valid=testTreeStructure(node,-1,marked);
        if(!valid){
            return false;
        }
        for(int i=0;i<edges.length;i++){
            if(edges[i]!=null && !marked[i]){
                return false;
            }
        }
        return true;
    }

    private boolean testTreeStructure(int node,int parent,boolean[] marked){
        IntSet neighbors=neighbors(node);
        for(int i=0;i<neighbors.size();i++){
            int n=neighbors.get(i);
            if(n!=parent){
                if(marked[n]){
                    return false;
                }
                marked[n]=true;
                boolean result=testTreeStructure(n,node,marked);
                if(!result){
                    return false;
                }
            }
        }
        return true;
    }
    public IntSet treeLeaves(){
        IntSet result=new IntSet(30);
        for(int i=0;i<edges.length;i++){
            if(edges[i]!=null && edges[i].size()==1){
                result.add(i);
            }
        }
        return result;
    }
    public boolean contains(int vertex){
	return edges.length<=vertex || edges[vertex]!=null;
    }

    public boolean add(int vertex){
	ensureCapacity(vertex);
	if(edges[vertex]!=null){
	    return false;
	}
	vertexCount++;
	vertices=null;
	edges[vertex]=new IntSet();
        if(largest!=LARGEST_UNDEFINED && vertex>largest){
            largest=vertex;
        }
	return true;
    }
    public int addNew(){
        if(largest<0){
            findLargest();
        }
        int result=largest+1;
        if(!add(result)){
            throw new IllegalStateException();
        }
        return result;
    }
       
    private void findLargest(){
        for(int i=edges.length-1;i>=0;i--){
            if(edges[i]!=null){
                largest=i;
                return;
            }
        }
        largest=-1;
    }

    public IntSet vertices(){
	if(vertices==null){
	    vertices=new IntSet(vertexCount);
	    for(int i=0;i<edges.length;i++){
		if(edges[i]!=null){
		    vertices.appendAdd(i);
		}
	    }
	}
	return vertices;
    }

    public boolean remove(int vertex){
	if(edges.length<=vertex || edges[vertex]==null){
	    return false;
	}
	vertexCount--;
	vertices=null;
	IntSet neighbors=edges[vertex];
	for(int i=0;i<neighbors.size();i++){
	    edges[neighbors.get(i)].remove(vertex);
	}
	edges[vertex]=null;
        if(vertex==largest){
            largest=LARGEST_UNDEFINED;
        }
	return true;
    }


    public boolean addEdge(int vertex1, int vertex2){
	boolean result=add(vertex1);
	result |=add(vertex2);
	result |= edges[vertex1].add(vertex2);
	result |= edges[vertex2].add(vertex1);
	return result;
    }


    public boolean removeEdge(int vertex1, int vertex2){
	if(edges.length<=vertex1 || edges[vertex1]==null || edges.length<=vertex2 || edges[vertex2]==null){
	    return false;
	}
	boolean result=edges[vertex1].remove(vertex2);
	if(result){
	    edges[vertex2].remove(vertex1);
	}
	return result;
    }


    public boolean containsEdge(int vertex1, int vertex2){
	return contains(vertex1) && contains(vertex2) && edges[vertex1].contains(vertex2);
    }

    public IntSet neighbors(int vertex){
	if(vertex>=edges.length){
	    return null;
	}else{
	    return edges[vertex];
	}
    }

    private void ensureCapacity(int vert){
	if(edges.length<=vert){
	    IntSet[] oldEdges=edges;
	    edges=new IntSet[(vert*3)/2+2];
	    System.arraycopy(oldEdges,0,edges,0,oldEdges.length);
	}
    }

    public int size(){
	return vertexCount;
    }

    public boolean removeAndConnect(int vertex){
        if(edges.length<=vertex || edges[vertex]==null){
	    return false;
	}
	IntSet neighbors=edges[vertex];
	remove(vertex);
	for(int i=0;i<neighbors.size();i++){
	    for(int j=i+1;j<neighbors.size();j++){
		addEdge(neighbors.get(i),neighbors.get(j));
	    }
	}
	return true;
    }
	
    public void sanityCheck(){
	for(int i=0;i<edges.length;i++){
	    if(edges[i]!=null){
		edges[i].sanityCheck();
	    }
	    for(int j=0;j<edges[i].size();j++){
		int n=edges[i].get(j);
		if(edges[n]==null){
		    throw new IllegalStateException("Edge has non-existant node");
		}else{
		    if(!edges[n].contains(i)){
			throw new IllegalStateException("Non symetric edge lists");
		    }
		}
	    }
	}
	int count=0;
	for(int i=0;i<edges.length;i++){
	    if(edges[i]!=null){
		count++;
	    }
	}
	if(count!=vertexCount){
	    throw new IllegalStateException("vertexCount not accurate");
	}
    }
	    
}
