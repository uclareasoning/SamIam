/*
* Messenger.java
*
*/
package edu.ucla.belief.inference;
import java.util.*;
import edu.ucla.structure.*;
import edu.ucla.util.*;
/**
*
* @author unknown
* @version
*/
public abstract class Messenger extends Object {
    private boolean[][] validMessage;
    private IntGraph tree;
    /** Creates new AbstractJoinTreeEngine */
    public Messenger(IntGraph tree) {
        this.tree = tree;
        validMessage = new boolean[tree.size()][];
        for (int i = 0; i < validMessage.length; i++) {
            validMessage[i] = new boolean[tree.degree(i)];
        }
    }
    /**
     * Performs a collect evidence on the specified vertex. This results in calling message(i,j) for all of the incoming edges
     * which are invalid, which may recursively pull evidence from farther nodes.
     */
    public void collect(int vertex) {
        collect(vertex, -1);
    }
    private void collect(int vertex, int parent) {
        int[] neighbors = tree.neighbors(vertex);
        for (int i = 0; i < neighbors.length; i++) {
            if (neighbors[i] != parent && !isValid(neighbors[i], vertex)) {
                collect(neighbors[i], vertex);
                message(neighbors[i], vertex);
                setValid(neighbors[i], vertex, true);
            }
        }
    }
    /**
     * Performs a distribute evidence on the specified vertex. This results in calling message(i,j) for all out going edges which are
     * invalid. This will continue recursively.
     */
    public void distribute(int vertex) {
        distribute(-1, vertex);
    }
    private void distribute(int parent, int vertex) {
        int[] neighbors = tree.neighbors(vertex);
        for (int i = 0; i < neighbors.length; i++) {
            if (neighbors[i] != parent && !isValid(vertex, neighbors[i])) {
                message(vertex, neighbors[i]);
                setValid(vertex, neighbors[i], true);
                distribute(vertex, neighbors[i]);
            }
        }
    }
    public boolean isValid(int from, int to) {
        return validMessage[from][tree.neighborIndex(from, to)];
    }
    private void setValid(int from, int to, boolean isValid) {
        validMessage[from][tree.neighborIndex(from, to)] = isValid;
    }
    public void invalidate(int vertex) {
        invalidate(-1, vertex);
    }
    protected void invalidate(int parent, int vertex) {
        int[] neighbors = tree.neighbors(vertex);
        for (int i = 0; i < neighbors.length; i++) {
            if (neighbors[i] != parent) {
                validMessage[vertex][i] = false;
                invalidate(vertex, neighbors[i]);
            }
        }
    }
    public void invalidateAll() {
        for (int i = 0; i < validMessage.length; i++) {
            for (int j = 0; j < validMessage[i].length; j++) {
                validMessage[i][j] = false;
            }
        }
    }
    /**
     * performs the message pass from vertex src to vertex dest. When this is called
     * the src vertex is guaranteed to have already received all of its incoming messages
     * except possibly the one from vertex dest.
     */
    protected abstract void message(int src, int dest);
}
