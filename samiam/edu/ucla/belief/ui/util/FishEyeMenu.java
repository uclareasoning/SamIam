package edu.ucla.belief.ui.util;

import java.awt.*;
import java.awt.font.*;
import java.awt.event.*;
import java.util.*;
import java.net.*;
import javax.swing.*;
import javax.swing.event.*;

/**
	Copyright (C) 1998-2000 by University of Maryland, College Park, MD 20742, USA
	All rights reserved.

	http://www.cs.umd.edu/hcil/fisheyemenu/fisheyemenu-demo.shtml
	http://www.cs.umd.edu/hcil/pubs/products.shtml

	@author Ben Bederson
	@since 011305
	@author Keith Cascio
*/
public class FishEyeMenu extends JMenu implements MenuListener {
	FishEyeWindow fishEyeWindow = null;

	public FishEyeMenu(String title) {
		super(title);
		addMenuListener(this);

		fishEyeWindow = new FishEyeWindow(this);
	}

	public JMenuItem add(JMenuItem item) {
		fishEyeWindow.add(item);
		return item;
	}

	public void setDesiredMaxFontSize(int desiredMaxFontSize) {
		fishEyeWindow.setDesiredMaxFontSize(desiredMaxFontSize);
	}

	public void setDesiredFocusLength(int desiredFocusLength) {
		fishEyeWindow.setDesiredFocusLength(desiredFocusLength);
	}

	public void menuSelected(MenuEvent e) {
		Point location = getLocationOnScreen();
		fishEyeWindow.setLocation(location.x, location.y + getSize().height);
		fishEyeWindow.setVisible(true);
		fishEyeWindow.requestFocus();
	}

	public void menuCanceled(MenuEvent e) {
		fishEyeWindow.setVisible(false);
	}

	public void menuDeselected(MenuEvent e) {
		fishEyeWindow.setVisible(false);
	}

	protected void fireMenuCanceled() {
		super.fireMenuCanceled();
	}
}