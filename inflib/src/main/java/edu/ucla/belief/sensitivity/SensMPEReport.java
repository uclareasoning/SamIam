package edu.ucla.belief.sensitivity;

import edu.ucla.belief.*;
import edu.ucla.util.*;

import java.util.*;

public class SensMPEReport
{
	protected Map[] mpe;
	protected Map resultsMap;

	public SensMPEReport(Map[] mpe, Map resultsMap)
	{
		this.mpe = mpe;
		this.resultsMap = resultsMap;
	}

	public Map[] getMPE()
	{
		return mpe;
	}

	public Map getResultsMap()
	{
		return resultsMap;
	}

	public List generateSingleParamSuggestions()
	{
		Object[] tables = resultsMap.values().toArray();
		ArrayList allSuggestions = new ArrayList();
		for (int i = 0; i < tables.length; i++)
		{
			SensitivityTable table = (SensitivityTable)tables[i];
			CPTShell shell = table.getCPTShell();
			Collection suggestions = Collections.EMPTY_SET;
			if (shell instanceof NoisyOrShell)
				suggestions = SingleParamSuggestion.
					generateNoisyOrSuggestions(
					(NoisyOrShell)shell, table);
			else if (shell instanceof TableShell)
				suggestions = SingleParamSuggestion.
					tableSuggestions(
					(TableShell)shell, table);
			allSuggestions.addAll(suggestions);
		}
		return allSuggestions;
	}

	public List[] splitSingleParamSuggestions()
	{
		List[] splitSuggestions = new List[2];
		List allSuggestions = generateSingleParamSuggestions();
		splitSuggestions[0] = new ArrayList();
		splitSuggestions[1] = new ArrayList();
		for (int i = 0; i < allSuggestions.size(); i++)
		{
			SingleParamSuggestion suggestion =
				(SingleParamSuggestion)allSuggestions.get(i);
			if (suggestion.sign() == 1)
				splitSuggestions[0].add(suggestion);
			if (suggestion.sign() == -1)
				splitSuggestions[1].add(suggestion);
		}
		return splitSuggestions;
	}
}
