package edu.ucla.belief.sensitivity;

import edu.ucla.belief.*;
import edu.ucla.util.*;
import java.util.*;

public class CovaryingScheme
{
	protected CPTShell shell;
	protected Object object;
	protected int[] covaryIndices;
	protected double[] ratios;

	public CovaryingScheme(CPTShell shell, Object object, int[] covaryIndices,
		double[] ratios)
	{
		this.shell = shell;
		this.object = object;
		this.covaryIndices = covaryIndices;
		this.ratios = ratios;
	}

	public LinearFunction covaryingFunction(LinearFunction function)
	{
		Object[] arguments = new Object[1];
		arguments[0] = object;
		double[] coefficients = function.getCoefficients();
		double[][] combinators = new
			double[1][coefficients.length];
		Arrays.fill(combinators[0], 0.0);
		for (int i = 0; i < covaryIndices.length; i++)
			combinators[0][covaryIndices[i]] = ratios[i];
		return function.combine(arguments, combinators);
	}

	public static CovaryingScheme[] proportionalSchemes(FiniteVariable var)
	{
		int varSize = var.size();
		CPTShell shell = var.getCPTShell( var.getDSLNodeType() );
		double[] thetas = shell.getCPT().dataclone();
		CPTParameter[] cptParameters = shell.getCPTParameters();
		boolean[] excludeArray = ExcludePolicy.getExcludeArray(var);
		CovaryingScheme[] schemes = new CovaryingScheme[thetas.length];
		for (int i = 0; i < thetas.length; i += varSize)
		{
			double sum = 0.0;
			for (int j = i; j < i + varSize; j++)
				if (!excludeArray[j])
					sum += thetas[j];
			for (int j = i; j < i + varSize; j++)
			{
				int[] covaryIndices = new int[varSize];
				for (int k = 0; k < varSize; k++)
					covaryIndices[k] = i+k;
				double[] ratios = new double[varSize];
				if (excludeArray[j])
					Arrays.fill(ratios, 0.0);
				else
					for (int k = 0; k < varSize; k++)
						if (j == i+k)
							ratios[k] = 1.0;
						else if (sum > thetas[j])
							ratios[k] = -thetas[i+k] /
								(sum-thetas[j]);
						else
							ratios[k] = -1 / (varSize-1);
				schemes[j] = new CovaryingScheme(shell,
					cptParameters[j], covaryIndices, ratios);
			}
		}
		return schemes;
	}
}
