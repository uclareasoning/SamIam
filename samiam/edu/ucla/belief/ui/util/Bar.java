package edu.ucla.belief.ui.util;

import edu.ucla.belief.ui.networkdisplay.*;

import java.awt.*;

public class Bar extends EvidenceIcon {
	private int direction;

	public Bar(int width, int height, Color color, int direction) {
		super(width, height, color, color);
		this.direction = direction;
		setDirection(direction);
	}

	public int getDirection() {
		return direction;
	}
}
