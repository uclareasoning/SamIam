package edu.ucla.structure;
import java.util.*;
public class IntGraph {
    private int size;
    private int[][] neighbors;
    public IntGraph() {
    }
    public IntGraph(int[][] neighbors) {
        edu.ucla.util.Assert.noNullElements(neighbors);
        size = neighbors.length;
        this.neighbors = neighbors;
    }
    public final int degree(int i) {
        return neighbors[i].length;
    }
    public final int getMaxDegree() {
    	int ret = (int)0;
    	for( int i=0; i<size; i++ ) ret = Math.max( ret, degree(i) );
    	return ret;
    }
    public final int[] neighbors(int i) {
        return neighbors[i];
    }
    public final int size() {
        return size;
    }
    public final boolean isLeaf(int i) {
        return neighbors[i].length < 2;
    }
    private static int intValue(Map m, Object vertex) {
        return ((Integer) m.get(vertex)).intValue();
    }
    public int neighborIndex(int from, int to) {
        int[] n = neighbors[from];
        for (int i = 0; i < n.length; i++) {
            if (n[i] == to) {
                return i;
            }
        }
        throw new IllegalArgumentException("Not a neighbor");
    }
}
