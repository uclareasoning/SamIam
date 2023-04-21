package edu.ucla.util;

import edu.ucla.belief.*;
import java.util.*;

/** Represents a function c_0 + c_1 x_1 + ... + c_n x_n */
public class LinearFunction {
	protected String name;
	protected Object[] arguments;
	protected double constant;
	protected double[] coefficients;

	public LinearFunction(String name, Object[] arguments, double constant, 
		double[] coefficients) {
		this.name = name;
		this.arguments = arguments;
		this.constant = constant;
		this.coefficients = coefficients;
	}

	public String getName() {
		return name;
	}

	public Object[] getArguments() {
		return arguments;
	}

	public double getConstant() {
		return constant;
	}

	public double[] getCoefficients() {
		return coefficients;
	}

	public double output(double[] inputs) {
		return constant + DblArrays.dotProduct(coefficients, inputs);
	}

	public String toString() {
		String s = "F[" + name + "] = " + constant;
		for (int i = 0; i < coefficients.length; i++)
			s += " + " + coefficients[i] + " * " + arguments[i];
		return s;
	}

	public LinearFunction multiply(double k) {
		return new LinearFunction(String.valueOf(k) + " * " + name, 
			arguments, k * constant,
			DblArrays.multiply(coefficients, k));
	}

	public LinearFunction add(LinearFunction function) {
		return new LinearFunction(name + " + " + function.name, 
			arguments, constant + function.constant, 
			DblArrays.add(coefficients, function.coefficients));
	}

	public LinearFunction subtract(LinearFunction function) {
		return new LinearFunction(name + " - " + function.name, 
			arguments, constant - function.constant, 
			DblArrays.subtract(coefficients, function.coefficients));
	}

	/** Projects onto a subset of arguments */
	public LinearFunction project(LinearFunction function, List indices) {
		int indicesLength = indices.size();
		Object[] newArguments = new Object[indicesLength];
		double[] newCoefficients = new double[indicesLength];
		ListIterator indexIterator = indices.listIterator();
		for (int i = 0 ; i < indicesLength; i++) {
			int index = ((Integer)indexIterator.next()).intValue();
			newArguments[i] = arguments[index];
			newCoefficients[i] = coefficients[index];
		}
		return new LinearFunction(name, newArguments, constant, 
			newCoefficients);
	}

	/** Linear combination of linear functions */
	/** New number of arguments = newArguments.length = combinators.length */
	/** Old number of arguments = combinators[i].length */
	public LinearFunction combine(Object[] newArguments, double[][] combinators) 
	{
		double[] products = new double[combinators.length];
		for (int i = 0; i < combinators.length; i++)
			products[i] = DblArrays.dotProduct(coefficients, 
				combinators[i]);
		return new LinearFunction(name, newArguments, constant, products);
	}
}
