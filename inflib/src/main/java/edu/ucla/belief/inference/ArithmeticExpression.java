package edu.ucla.belief.inference;
public interface ArithmeticExpression extends java.io.Serializable {
    public static final int ADDITION = 1;
    public static final int MULTIPLICATION = 2;
    public static final int VALUE = 3;
    public double getValue();
    public double[] getParameter(int parameter);
    public void setParameter(int parameter, double[] values);
    public double[] getPartial(int parameter);
    public double getMemoryRequirements();
    public double getPropagationTime();
    public int edgeCount();
    public int nodeCount();
}
