package edu.ucla.structure;
import java.util.*;
public class IntDirectedGraph {
    private int size;
    private int[][] inComing;
    private int[][] outGoing;
    public IntDirectedGraph() {
    }
    public IntDirectedGraph(int[][] inComing, int[][] outGoing) {
        size = inComing.length;
        this.inComing = inComing;
        this.outGoing = outGoing;
    }
    public final int inDegree(int i) {
        return inComing[i].length;
    }
    public final int outDegree(int i) {
        return outGoing[i].length;
    }
    public final int size() {
        return size;
    }
    public final int[] inComing(int i) {
        return inComing[i];
    }
    public final int[] outGoing(int i) {
        return outGoing[i];
    }
}
