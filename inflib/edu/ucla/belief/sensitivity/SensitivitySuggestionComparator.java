package edu.ucla.belief.sensitivity;

import java.util.*;

public class SensitivitySuggestionComparator implements Comparator {
	public static int OBJECT = 0;
	public static int ABSOLUTE_CHANGE = 1;
	public static int LOG_ODDS_CHANGE = 2;
	private int choice;

	public SensitivitySuggestionComparator(int choice) {
		this.choice = choice;
	}

	public int compare(Object o1, Object o2) {
		if (o1 == o2)
			return 0;
		SensitivitySuggestion suggestion1 =
			(SensitivitySuggestion)o1;
		SensitivitySuggestion suggestion2 =
			(SensitivitySuggestion)o2;

		if (choice == OBJECT)
			return suggestion1.getObject().
				compareTo(suggestion2.getObject());
		else if (choice == ABSOLUTE_CHANGE)
			return new Double(suggestion1.
				getAbsoluteChange()).compareTo(new
				Double(suggestion2.getAbsoluteChange()));
		else if (choice == LOG_ODDS_CHANGE)
			return new Double(suggestion1.getLogOddsChange()).
				compareTo(new Double(suggestion2.
				getLogOddsChange()));
		return 0;
	}
}
