package edu.ucla.belief.io;

import edu.ucla.belief.Definitions;
import java.util.*;

public class ListConverter {
    public static List flatten(List list) {
        List result = new LinkedList();
        flatten(result, list);
        return new ArrayList(result);
    }
    private static void flatten(List destination, List source) {
        for (Iterator iter = source.iterator(); iter.hasNext();) {
            Object val = iter.next();
            if (val instanceof List) {
                flatten(destination, (List) val);
            } else {
                destination.add(val);
            }
        }
    }
    public static List makeDimensional(List values, int[] sizes) {
        int size = 1;
        int[] blockSizes = new int[sizes.length];
        for (int i = sizes.length - 1; i >= 0; i--) {
            blockSizes[i] = size;
            size *= sizes[i];
        }
        if (values.size() != size) {
            throw new IllegalArgumentException("list size does not equal size required by the specified dimensions");
        }
        List blockSizeList = toList(blockSizes);
        List sizeList = toList(sizes);
        return makeDimensional(values, sizeList, blockSizeList);
    }
    private static List makeDimensional(List values,
            List dimensionSizes, List blockSizes) {
        if (dimensionSizes.size() <= 1) {
            return new ArrayList(values);
        } else {
            int dimSize = ((Integer) dimensionSizes.get(0)).intValue();
            int blockSize = ((Integer) blockSizes.get(0)).intValue();
            List childDims =
                    dimensionSizes.subList(1, dimensionSizes.size());
            List childBlocks = blockSizes.subList(1, blockSizes.size());
            List result = new ArrayList(dimSize);
            for (int i = 0; i < dimSize; i++) {
                result.add( makeDimensional( values.subList(i * blockSize,
                        (i + 1) * blockSize), childDims, childBlocks));
            }
            return result;
        }
    }
    public static List toList(int[] values) {
        List result = new ArrayList(values.length);
        for (int i = 0; i < values.length; i++) {
            result.add(new Integer(values[i]));
        }
        return result;
    }
    public static List toList(double[] values) {
        List result = new ArrayList(values.length);
        for (int i = 0; i < values.length; i++) {
            result.add(new Double(values[i]));
        }
        return result;
    }
    public static int[] toIntArray(List values) {
        int[] result = new int[values.size()];
        int i = 0;
        for (Iterator iter = values.iterator(); iter.hasNext(); i++) {
            result[i] = ((Number) iter.next()).intValue();
        }
        return result;
    }
    public static double[] toDoubleArray(List values) {
        double[] result = new double[values.size()];
        int i = 0;
        for (Iterator iter = values.iterator(); iter.hasNext(); i++) {
            result[i] = ((Number) iter.next()).doubleValue();
        }
        return result;
    }
    public static void main(String[] args) {
        double[] vals = new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        List list = makeDimensional(toList(vals), new int[]{2, 3, 2});
        Definitions.STREAM_TEST.println(list);
        Definitions.STREAM_TEST.println(flatten(list));
    }
}
