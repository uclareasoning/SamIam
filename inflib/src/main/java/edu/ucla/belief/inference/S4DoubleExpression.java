package edu.ucla.belief.inference;
import edu.ucla.structure.IntGraph;
import java.util.*;
import java.io.*;
public class S4DoubleExpression implements ArithmeticExpression {
    private LocalPotential[][] potentials;
    private int[][] clusterVariables;
    private int[][] clusterVariableHighest;
    private int[] sizes;
    private IntGraph graph;
    private LocalMessenger messenger;
    int root;
    private double propagationTime=0;

    S4DoubleExpression(IntGraph graph, int[][] clusterVars,
            int[][][] sepVars, int[] sizes) {
        this.graph = graph;
        messenger = new LocalMessenger(graph);
        this.clusterVariables = new int[clusterVars.length][];
        for (int i = 0; i < clusterVariables.length; i++) {
            clusterVariables[i] = new int[clusterVars[i].length];
            System.arraycopy(clusterVars[i], 0, clusterVariables[i], 0,
                    clusterVars[i].length);
            java.util.Arrays.sort(clusterVariables[i]);
        }
        this.sizes = sizes;
        clusterVariableHighest = new int[clusterVariables.length][];
        for (int i = 0; i < clusterVariables.length; i++) {
            clusterVariableHighest[i] = new int[clusterVariables[i].length];
            for (int j = 0; j < clusterVariables[i].length; j++) {
                clusterVariableHighest[i][j] =
                        sizes[clusterVariables[i][j]] - 1;
            }
        }
        potentials = new LocalPotential[graph.size()][];
        for (int i = 0; i < potentials.length; i++) {
            potentials[i] = new LocalPotential[graph.degree(i)];
            for (int j = 0; j < potentials[i].length; j++) {
                potentials[i][j] = new LocalPotential(sepVars[i][j], sizes);
            }
        }
        root = graph.size() - 1;
        potentials[root][0].values[0] = 1;
    }
    public double getValue() {
        long start=System.currentTimeMillis();
	messenger.collect(root);
        LocalPotential p = incomingMessage(root);
	long finish=System.currentTimeMillis();
	propagationTime+=(finish-start)/1000.0;
        return p.values[0];
    }
    public double[] getParameter(int parameter) {
        return outgoingMessage(parameter).getExternal();
    }
    public void setParameter(int parameter, double[] params) {
	propagationTime=0;
        outgoingMessage(parameter).setExternal(params);
        messenger.invalidate(parameter);
    }
    public double[] getPartial(int parameter) {
	long start=System.currentTimeMillis();
        messenger.collect(parameter);
	long finish=System.currentTimeMillis();
	propagationTime+=(finish-start)/1000.0;
        return incomingMessage(parameter).getExternal();
    }
    public int edgeCount() {
        return -1;
    }
    public int nodeCount() {
        return -1;
    }
    private final LocalPotential incomingMessage(int leaf) {
        return getMessage(graph.neighbors(leaf)[0], leaf);
    }
    private final LocalPotential outgoingMessage(int leaf) {
        return getMessage(leaf, graph.neighbors(leaf)[0]);
    }
    private final LocalPotential getMessage(int from, int to) {
        return potentials[from][graph.neighborIndex(from, to)];
    }
    private void sendMessage(int from, int to) {
        if (graph.isLeaf(from)) {
            return;
        }
        int[] vars = clusterVariables[from];
        int[] highest = clusterVariableHighest[from];
        int[] inst = new int[vars.length];
        LocalPotential[] potentials = getPotentials(from, to);
        int[] currentInd = new int[potentials.length];
        int[][] flipChange = getFlipChange(potentials, vars);
        double[][] values = getValues(potentials);
        java.util.Arrays.fill(values[0], 0.0f);
        while (true) {
            double total = 1f;
            for (int i = 1; i < values.length; i++) {
                total *= values[i][currentInd[i]];
            }
            values[0][currentInd[0]] += total;
            int change = next(inst, highest);
            if (change < 0) {
                break;
            }
            for (int i = 0; i < currentInd.length; i++) {
                currentInd[i] += flipChange[i][change];
            }
        }
    }
    public class CircuitWriter{
        private int[] clusterBases;
        private int[][] separatorBases;
        private int current;
        public CircuitWriter(){
            clusterBases=new int[graph.size()];
            java.util.Arrays.fill(clusterBases,-7);
            separatorBases=new int[graph.size()][graph.size()];
            for(int i=0;i<separatorBases.length;i++){
                java.util.Arrays.fill(separatorBases[i],-5);
            }
            current=0;
            System.err.println(graph);
            System.err.println("root="+root);
        }
        public void writeEdges(java.io.PrintWriter writer){
            int[] neighbors=graph.neighbors(root);
            if(neighbors.length>1){
                throw new IllegalStateException("root has multiple children");
            }
            writeEdges(writer,neighbors[0],root);
        }
            
        public void writeEdges(java.io.PrintWriter writer,int from, int to) {
            
            LocalPotential[] potentials = getPotentials(from, to);
            int[] baseLoc = getBases(writer,from,to);
            int[] vars = clusterVariables[from];
            int[] highest = clusterVariableHighest[from];
            int[] inst = new int[vars.length];
            if (graph.isLeaf(from)) {
                return;
            }
            int[] currentInd = new int[potentials.length];
            int[][] flipChange = getFlipChange(potentials, vars);
            //double[][] values = getValues(potentials);
            //java.util.Arrays.fill(values[0], 0.0f);
            int offset=0;
            while (true) {
                double total = 1f;
                for (int i = 1; i < baseLoc.length; i++) {
                    writer.println(""+(currentInd[i]+baseLoc[i])+"\t"+(clusterBases[from]+offset)+"\t*");
                    //total *= values[i][currentInd[i]];
                }
                writer.println(""+(clusterBases[from]+offset)+"\t"+(currentInd[0]+baseLoc[0])+"\t+");
                //values[0][currentInd[0]] += total;
                int change = next(inst, highest);
                if (change < 0) {
                    break;
                }
                for (int i = 0; i < currentInd.length; i++) {
                    currentInd[i] += flipChange[i][change];
                }
                offset++;
            }
        }
        private int[] getBases(PrintWriter writer,int from, int to) {
            int[] neighbors = graph.neighbors(from);
            int[] result = new int[neighbors.length];
            
            int count = 1;
            for (int i = 0; i < neighbors.length; i++) {
                if (neighbors[i] != to) {
                    writeEdges(writer,neighbors[i],from);
                    System.err.println("getting ["+neighbors[i]+","+from+"]="+separatorBases[neighbors[i]][from]);
                    result[count++] = separatorBases[neighbors[i]][from];
                }
            }
            System.err.println("setting cluster["+from+"]="+current);
            clusterBases[from]=current;
            if(!graph.isLeaf(from)){//if it is a leaf, we share it with the separator index
                              //since they have the same size, and we don't acually use it
                              //anyway.  This makes it easier to match up the parameters with
                              // the labels in the BeliefCompilation part.
                current+=getClusterSize(from);
            }
            System.err.println("setting separator["+from+","+to+"]="+current);
            separatorBases[from][to]=current;
            current+=getSeparatorSize(from,to);
            result[0] = separatorBases[from][to];
            return result;
        }
    }
    private int getClusterSize(int cluster){
        int[] vars=clusterVariables[cluster];
        int result=1;
        for(int i=0;i<vars.length;i++){
            result*=sizes[vars[i]];
        }
        return result;
    }
    private int getSeparatorSize(int from,int to){
        return getMessage(from,to).values.length;
    }
    /**
     * Returns the destination potential as result[0]. The rest are the source potentials.
     */
    private LocalPotential[] getPotentials(int from, int to) {
        int[] neighbors = graph.neighbors(from);
        LocalPotential[] result = new LocalPotential[neighbors.length];
        result[0] = getMessage(from, to);
        int count = 1;
        for (int i = 0; i < neighbors.length; i++) {
            if (neighbors[i] != to) {
                result[count++] = getMessage(neighbors[i], from);
            }
        }
        return result;
    }
    private static final int[][] getFlipChange(LocalPotential[] p,
            int[] vars) {
        int[][] result = new int[p.length][];
        for (int i = 0; i < result.length; i++) {
            result[i] = p[i].getFlipChange(vars);
        }
        return result;
    }
    private static final double[][] getValues(LocalPotential[] p) {
        double[][] result = new double[p.length][];
        for (int i = 0; i < result.length; i++) {
            result[i] = p[i].values;
        }
        return result;
    }
    private class LocalMessenger extends Messenger {
        private LocalMessenger(IntGraph graph) {
            super(graph);
        }
        protected void message(int from, int to) {
            sendMessage(from, to);
        }
    }
    private static final int next(final int[] inds,
            final int[] highestValues) {
        for (int i = inds.length - 1; i >= 0; i--) {
            if (inds[i] < highestValues[i]) {
                inds[i]++;
                return i;
            } else {
                inds[i] = 0;
            }
        }
        return -1;
    }
    private static final class LocalPotential {
        /** The order variable ordering as seen from outside classes */
        private final int[] externalOrder;
        /** The sorted variable ordering used internally */
        private final int[] storedOrder;
        /**
        * largest[i] is the largest legal value for variable storedOrder[i].
        */
        private final int[] largest;
        /** The ammount of offset from the current index caused by incrementing
        the given variable, while reseting all variables with a larger index.
        */
        private final int[] flipChange;
        /** The ammount of offset from the current index caused by reseting the
        variable and all larger index variables.
        */
        private final int[] resetChange;
        /** The ammount of offset in the external representation caused by reseting the variable (as ordered in storedOrder) and all larger index variables.
        */
        private final int[] externalFlipChange;
        /** The values in the potential stored in the format where storedOrder[0] is the most significant index and storedOrder[last] is least significant.*/
        private final double[] values;

        LocalPotential(int[] inds, int[] size) {
            this.externalOrder = inds;
            this.storedOrder = new int[inds.length];
            System.arraycopy(inds, 0, storedOrder, 0, storedOrder.length);
            java.util.Arrays.sort(storedOrder);
            int[] sortedToExt = new int[storedOrder.length];
            for (int i = 0; i < externalOrder.length; i++) {
                int loc = Arrays.binarySearch(storedOrder,
                        externalOrder[i]);
                sortedToExt[loc] = i;
            }
            largest = new int[storedOrder.length];
            for (int i = 0; i < largest.length; i++) {
                largest[i] = size[storedOrder[i]] - 1;
            }
            int[] blockSize = blockSize(storedOrder, size);
            int[] resetSize = new int[blockSize.length];
            for (int i = blockSize.length - 1; i >= 0; i--) {
                resetSize[i] = blockSize[i] * (size[storedOrder[i]] - 1);
            }
            flipChange = new int[blockSize.length];
            resetChange = new int[blockSize.length];
            int resetTotal = 0;
            for (int i = resetSize.length - 1; i >= 0; i--) {
                flipChange[i] = blockSize[i] - resetTotal;
                resetTotal += resetSize[i];
                resetChange[i] = -resetTotal;
            }
            int[] externalBlockSize = blockSize(externalOrder, size);
            int[] externalResetSize = new int[externalBlockSize.length];
            for (int i = externalBlockSize.length - 1; i >= 0; i--) {
                externalResetSize[i] = externalBlockSize[i] *
                        (size[externalOrder[i]] - 1);
            }
            externalFlipChange = new int[externalBlockSize.length];
            resetTotal = 0;
            for (int i = externalBlockSize.length - 1; i >= 0; i--) {
                externalFlipChange[i] =
                        externalBlockSize[sortedToExt[i]] - resetTotal;
                resetTotal += externalResetSize[sortedToExt[i]];
            }
            if (blockSize.length > 0) {
                values = new double[blockSize[0] * size[storedOrder[0]]];
            } else {
                values = new double[1];
            }
        }
        private static final int[] blockSize(int [] vars, int[] size) {
            int[] blockSize = new int[vars.length];
            int sz = 1;
            for (int i = blockSize.length - 1; i >= 0; i--) {
                blockSize[i] = sz;
                sz *= size[vars[i]];
            }
            return blockSize;
        }
        private final int[] getFlipChange(int[] vars) {
            int[] result = new int[vars.length];
            int current = flipChange.length - 1;
            int reset = 0;
            for (int i = vars.length - 1; i >= 0; i--) {
                if (current >= 0 && vars[i] == storedOrder[current]) {
                    result[i] = flipChange[current];
                    reset = resetChange[current--];
                } else {
                    result[i] = reset;
                }
            }
            return result;
        }
        private final void clear() {
            java.util.Arrays.fill(values, 0);
        }
        private void setExternal(double[] vals) {
            int[] inst = new int[storedOrder.length];
            int externalInd = 0;
            for (int i = 0; ;i++) {
                values[i] = (double) vals[externalInd];
                int change = next(inst, largest);
                if (change < 0) {
                    break;
                }
                externalInd += externalFlipChange[change];
            }
        }
        private double[] getExternal() {
            double[] result = new double[values.length];
            int[] inst = new int[storedOrder.length];
            int externalInd = 0;
            for (int i = 0; ;i++) {
                result[externalInd] = values[i];
                int change = next(inst, largest);
                if (change < 0) {
                    break;
                }
                externalInd += externalFlipChange[change];
            }
            return result;
        }

	public double memory(){
	    return values.length*8;
	}
    }

    public double getPropagationTime(){
	return propagationTime;
    }

    public double getMemoryRequirements(){
        double total=0;
	for(int i=0;i<potentials.length;i++){
	    for(int j=0;j<potentials[i].length;j++){
		total+=potentials[i][j].memory();
	    }
	}
	return total;
    }
}
