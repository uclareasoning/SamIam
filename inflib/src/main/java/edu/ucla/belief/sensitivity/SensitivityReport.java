package edu.ucla.belief.sensitivity;

import edu.ucla.belief.*;
import java.util.*;

public class SensitivityReport {
	protected Map singleParamMap;
	protected Map singleCPTMap;

	public SensitivityReport(Map singleParamMap, Map singleCPTMap) {
		this.singleParamMap = singleParamMap;
		this.singleCPTMap = singleCPTMap;
	}

	public Map getSingleParamMap() {
		return singleParamMap;
	}

	public Map getSingleCPTMap() {
		return singleCPTMap;
	}

	public List generateSingleParamSuggestions() {
		Object[] tables = singleParamMap.values().toArray();
		Vector allSuggestions = new Vector();
		for (int i = 0; i < tables.length; i++) {
			SensitivityTable table = 
				(SensitivityTable)tables[i];
			CPTShell shell = table.getCPTShell();
			Vector suggestions = new Vector();
			if (shell instanceof NoisyOrShell)
				suggestions = SingleParamSuggestion.
					generateNoisyOrSuggestions(
					(NoisyOrShell)shell, table);
			else if (shell instanceof TableShell)
				suggestions = SingleParamSuggestion.
					generateTableSuggestions(
					(TableShell)shell, table);
			allSuggestions.addAll(suggestions);
		}
		return allSuggestions;
	}
}
