package edu.ucla.structure;
/**
* Implements a union find set.  Initially, the set each index corresponds to
* is the label.  Subsequent calls to union combine the sets, and calls to find
* return the set it belongs to.
*/
public class UnionFind implements java.io.Serializable {
    private final int[] rank;
    private final int[] parent;
    /**
    * Creates a union find set of the specified size.
    * @param size The number of elements.
    */
    public UnionFind(int size) {
        rank = new int[size];
        parent = new int[size];
        for (int i = 0; i < parent.length; i++) {
            parent[i] = i;
        }
    }
    /**
    * Merges the sets that the parameters belong to.  If x belongs to set w 
    * and y belongs to set z, after calling union(x,y) all the members of w
    * and z will belong to the same set.
    * @param x an element
    * @param y another element
    */
    public void union(int x, int y) {
        link(find(x), find(y));
    }
    private void link(int x, int y) {
        if (rank[x] > rank[y]) {
            parent[y] = x;
        } else {
            parent[x] = y;
            if (rank[x] == rank[y]) {
                rank[y]++;
            }
        }
    }
    /**
    * Returns the set that element x belongs to.
    * @param x the element whose set we wish to discover.
    */
    public int find(int x) {
        int y = x;
        while (y != parent[y]) {
            y = parent[y];
        }
        while (x != y) {
            int p = parent[x];
            parent[x] = y;
            x = p;
        }
        return y;
    }
}
