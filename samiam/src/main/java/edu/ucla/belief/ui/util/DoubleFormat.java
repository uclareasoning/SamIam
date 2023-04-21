package edu.ucla.belief.ui.util;

import java.math.*;
import java.text.*;

public class DoubleFormat extends DecimalFormat {
	private boolean format;
	private int maxDigits;
	public DoubleFormat() {
		format = false;
	}

	public DoubleFormat(int maxDigits) {
		format = true;
		this.maxDigits = maxDigits;
		setMaximumFractionDigits(maxDigits);
	}

	public String doubleFormat(double value) {
		if (!format || value == 0)
			return format(value);
		int log = (int)Math.floor(Math.log(value) / Math.log(10));
		if (log >= 0)
			return format(value);
		setMaximumFractionDigits(maxDigits - log - 1);
		String doubleFormat = format(value);
		setMaximumFractionDigits(maxDigits);
		return doubleFormat;
	}
}
