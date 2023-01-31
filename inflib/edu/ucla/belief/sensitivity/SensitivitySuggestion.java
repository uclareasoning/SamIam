package edu.ucla.belief.sensitivity;

import edu.ucla.belief.FiniteVariable;

public interface SensitivitySuggestion {

	public void adoptChange() throws Exception;
	public Comparable getObject();
	public Object getCurrentValue();
	public Object getSuggestedValue();
	public double getAbsoluteChange();
	public double getLogOddsChange();
	public FiniteVariable getVariable();
}
