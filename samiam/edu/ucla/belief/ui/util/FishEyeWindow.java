package edu.ucla.belief.ui.util;

import edu.ucla.belief.ui.toolbar.MainToolBar;

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
public class FishEyeWindow extends JWindow implements MouseListener, MouseMotionListener, KeyListener {
	static final int MAX_ANTIALIAS_SIZE = 10;
	static Image up = null;
	static Image down = null;

	FishEyeMenu menu = null;
	Item items[];
	int numItems = 0;
	Font fonts[];
	Font boldFonts[];
	int desiredFocusLength = 11;
	int desiredMaxFontSize = 12;
	int focusLength = desiredFocusLength;
	int maxFontSize = desiredMaxFontSize;
	int minFontSize = desiredMaxFontSize;
	float desiredSpacing = 0.5f;	 // Percentage of font size
	float minDesiredSpacing = 0.1f;  // Percentage of font size
	int desiredSpace = (int)(maxFontSize * desiredSpacing);
	int minSpace;
	int borderLeft = desiredMaxFontSize + 5;
	int borderRight = 3;
	int labelBorderLeft = 3;
	int borderY = 3;
	int mouseY = 0;
	Image backBuffer = null;
	Graphics gb = null;
	Color backgroundColor = new Color(207, 207, 207);
	Color hilightColor = new Color(144, 151, 207);
	Color labelColor = Color.black;
	int selectionIndex = 0;	// The index of the item that is currently selected
	int focusIndex = 0;		// The index of the item that is centered in the focus
	int focusIndexLUT[];
	String labelLUT[];
	int labelPosLUT[];
	int sizeLUT[][];
	int spaceLUT[][];
	int flFocusPosition;
	int flSizeLUT[];
	int flSpaceLUT[];
	int numLabels = 0;
	boolean focusLock = false;

	public FishEyeWindow(FishEyeMenu menu) {
		super();
		int i;

		this.menu = menu;

		items = new Item[10];
		for (i=0; i<items.length; i++) {
			items[i] = new Item();
		}

		fonts = new Font[maxFontSize + 1];
		for (i=1; i<=maxFontSize; i++) {
			fonts[i] = new Font(null, Font.PLAIN, i);
		}

		boldFonts = new Font[maxFontSize + 1];
		for (i=1; i<=maxFontSize; i++) {
			boldFonts[i] = new Font(null, Font.BOLD, i);
		}

		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);

		if (up == null) {
			URL resource = MainToolBar.findImageURL( "arrow-up.gif" );//this.getClass().getClassLoader().getResource("arrow-up.gif");
			up = Toolkit.getDefaultToolkit().createImage(resource);
		  //System.out.println( "   FishEyeWindow.up = " + up );
			resource = MainToolBar.findImageURL( "arrow-down.gif" );//this.getClass().getClassLoader().getResource("arrow-down.gif");
			down = Toolkit.getDefaultToolkit().createImage(resource);
		}
	}

	public void setDesiredMaxFontSize(int desiredMaxFontSize) {
		if (desiredMaxFontSize > this.desiredMaxFontSize) {
			int i;
			Font newFonts[] = new Font[desiredMaxFontSize + 1];
			Font newBoldFonts[] = new Font[desiredMaxFontSize + 1];
			for (i=1; i<=this.desiredMaxFontSize; i++) {
				newFonts[i] = fonts[i];
				newBoldFonts[i] = boldFonts[i];
			}
			for (i=this.desiredMaxFontSize+1; i<=desiredMaxFontSize; i++) {
				newFonts[i] = new Font(null, Font.PLAIN, i);
				newBoldFonts[i] = new Font(null, Font.BOLD, i);
			}
			fonts = newFonts;
			boldFonts = newBoldFonts;
		}
		this.desiredMaxFontSize = desiredMaxFontSize;
		desiredSpace = (int)(desiredMaxFontSize * desiredSpacing);
		borderLeft = desiredMaxFontSize + 5;
	}

	public void setDesiredFocusLength(int desiredFocusLength) {
		this.desiredFocusLength = desiredFocusLength;
		calculateSizes();
	}

	public void setVisible(boolean visible) {
		if (visible) {
			calculateSizes();
			focusIndex = 0;
			selectionIndex = 0;
			mouseY = 0;
			focusLock = false;
			labelColor = Color.black;
		}
		super.setVisible(visible);
	}

	public void mouseMoved(MouseEvent e) {
								// Determine if we are in focus lock mode
		if (e.getX() < getSize().width/2) {
			if (focusLock) {
				repaint();
			}
			focusLock = false;
		} else {
			if (!focusLock) {
								// Just entered focus lock mode, so update look up tables, and calculate focus item position
				flFocusPosition = borderY;
				for (int i=0; i<numItems; i++) {
					flSizeLUT[i] = sizeLUT[focusIndex][i];
					flSpaceLUT[i] = spaceLUT[focusIndex][i];
					if (i < focusIndex) {
						flFocusPosition += flSizeLUT[i] + flSpaceLUT[i];
					}
				}
			}
			focusLock = true;
		}

		mouseY = e.getY();

		if (focusLock) {
								// FOCUS LOCK mode
			boolean focusIncreased = false;
			if (mouseY > flFocusPosition) {
				int i;
				int y = flFocusPosition;
				selectionIndex = focusIndex;
								// First, set the items between the focus and the cursor to full size
				for (i=focusIndex; i<numItems; i++) {
					if (flSizeLUT[i] < maxFontSize) {
						flSizeLUT[i] = maxFontSize;
						flSpaceLUT[i] = desiredSpace;
						focusIncreased = true;
					}
					y += flSizeLUT[i] + flSpaceLUT[i];
					selectionIndex = i;
					if (mouseY < y) {
						break;
					}
				}
				i++;
								// Then, add the fisheye size decrease
				if ((i < numItems) && (flSizeLUT[i] < maxFontSize)) {
					boolean done = false;
					while ((i < numItems) && !done) {
						if (flSizeLUT[i] == minFontSize) {
							done = true;
						}
						if (flSizeLUT[i] < maxFontSize) {
							flSizeLUT[i]++;
							if (flSizeLUT[i] == maxFontSize) {
								flSpaceLUT[i] = desiredSpace;
								focusIncreased = true;
							}
						}
						i++;
					}
				}
			} else if (focusIndex > 0) {
				int i;
				int y = flFocusPosition - flSizeLUT[focusIndex-1] - flSpaceLUT[focusIndex-1];
				selectionIndex = focusIndex;
				for (i=focusIndex-1; i>=0; i--) {
					if (flSizeLUT[i] < maxFontSize) {
						flSizeLUT[i] = maxFontSize;
						flSpaceLUT[i] = desiredSpace;
						focusIncreased = true;
					}
					selectionIndex = i;
					if (mouseY > y) {
						break;
					}
					y -= flSizeLUT[i] + flSpaceLUT[i];
				}
				i--;
				if ((i >= 0) && (flSizeLUT[i] < maxFontSize)) {
					boolean done = false;
					while ((i >= 0) && !done) {
						if (flSizeLUT[i] == minFontSize) {
							done = true;
						}
						if (flSizeLUT[i] < maxFontSize) {
							flSizeLUT[i]++;
							if (flSizeLUT[i] == maxFontSize) {
								flSpaceLUT[i] = desiredSpace;
								focusIncreased = true;
							}
						}
						i--;
					}
				}
			}
			if (focusIncreased) {
				int bg = backgroundColor.getRed();
				int c = labelColor.getRed() + 30;
				if (c > bg) {
					c = bg;
				}
				labelColor = new Color(c, c, c);
			}
			repaint();
		} else {
								// Not FOCUS LOCK mode

								// Calculate index of focus item based on pointer position
			int prevFocusIndex = focusIndex;
			focusIndex = focusIndexLUT[mouseY];
			selectionIndex = focusIndex;
			labelColor = Color.black;
			repaintDamagedRegion(prevFocusIndex);
		}
	}

	void repaintDamagedItem(int index) {
		int y = borderY;
		for (int i=0; i<index; i++) {
			y += sizeLUT[focusIndex][i] + spaceLUT[focusIndex][i];
		}
		int height = sizeLUT[focusIndex][index] + spaceLUT[focusIndex][index];
		repaint(borderLeft, y, getSize().width - borderLeft - borderRight, height);
	}

	void repaintDamagedRegion(int prevFocusIndex) {
		int i;
		int y;
		int height;
		int yStart1 = -1;
		int yStart2 = -1;
		int height1 = -1;
		int height2 = -1;

								// Compute the portion of the menu that was changed, and just
								// repaint that portion.
		y = borderY;
		for (i=0; i<numItems; i++) {
			if ((yStart1 == -1) && (sizeLUT[prevFocusIndex][i] > minFontSize)) {
				yStart1 = y;
			} else if ((yStart1 >= 0) && (sizeLUT[prevFocusIndex][i] == minFontSize)) {
				height1 = y - yStart1;
				break;
			}
			y += sizeLUT[prevFocusIndex][i] + spaceLUT[prevFocusIndex][i];
		}
		if (height1 == -1) {
			height1 = getSize().height - yStart1;
		}

		y = borderY;
		for (i=0; i<numItems; i++) {
			if ((yStart2 == -1) && (sizeLUT[focusIndex][i] > minFontSize)) {
				yStart2 = y;
			} else if ((yStart2 >= 0) && (sizeLUT[focusIndex][i] == minFontSize)) {
				height2 = y - yStart2;
				break;
			}
			y += sizeLUT[focusIndex][i] + spaceLUT[focusIndex][i];
		}
		if (height2 == -1) {
			height2 = getSize().height - yStart2;
		}

		if (yStart1 < yStart2) {
			y = yStart1;
			height = height2 + (yStart2 - yStart1);
		} else {
			y = yStart2;
			height = height1 + (yStart1 - yStart2);
		}

		repaint(borderLeft, y, getSize().width - borderLeft - borderRight, height);
	}

	public void mouseDragged(MouseEvent e) {
		mouseMoved(e);
	}

	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {
		items[selectionIndex].menuItem.doClick();
		menu.fireMenuCanceled();
		requestFocus();
	}
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}

	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();

		switch (key) {
		case KeyEvent.VK_UP:
			if (selectionIndex > 0) {
				selectionIndex--;
				focusIndex--;
				repaint();
			}
			break;
		case KeyEvent.VK_DOWN:
			if (selectionIndex < (numItems - 1)) {
				selectionIndex++;
				focusIndex++;
				repaint();
			}
			break;
		case KeyEvent.VK_ENTER:
			items[selectionIndex].menuItem.doClick();
			menu.fireMenuCanceled();
			break;
		default:
								// Move index to next element starting with key
			char c = Character.toUpperCase(e.getKeyChar());
			if (Character.isLetter(c)) {
								// If already on such an element, try the next one
				if (Character.toUpperCase(items[selectionIndex].label.charAt(0)) == c) {
					selectionIndex++;
					focusIndex++;
					repaint();
				}

								// If not on such an element, then search for one
				if ((selectionIndex >= numItems) || (Character.toUpperCase(items[selectionIndex].label.charAt(0)) != c)) {
					for (int i=0; i<numItems; i++) {
						if (Character.toUpperCase(items[i].label.charAt(0)) == c) {
							selectionIndex = i;
							focusIndex = i;
							repaint();
							break;
						}
					}
				}
			}
			break;
		}
	}
	public void keyReleased(KeyEvent e) {}
	public void keyTyped(KeyEvent e) {}

	public void add(JMenuItem item) {
		try {
			items[numItems].menuItem = item;
			items[numItems].label = item.getText();
		} catch (ArrayIndexOutOfBoundsException e) {
			Item[] newItems = new Item[(numItems == 0) ? 1 : (2 * numItems)];
			System.arraycopy(items, 0, newItems, 0, numItems);
			items = newItems;
			for (int i=numItems; i<items.length; i++) {
				items[i] = new Item();
			}
			items[numItems].menuItem = item;
			items[numItems].label = item.getText();
		}
		numItems++;
	}


	/**
	 * Calculate the sizes of the window, the max and min font sizes, and the focus length.
	 * It uses the following the algorithm:
	 *   Try to follow desired max font size and focus length by first computing
	 *   the biggest minimum font size that will fit in the available space.
	 *   If this is not satisfied for a minimum font size of 1, then first
	 *   the focus length, and then the max font size is reduced until it fits.
	 */
	public void calculateSizes() {
		int i;
		int width = 0;
		int height = 0;
		int maxHeight = 0;

		focusLength = desiredFocusLength;
		maxFontSize = desiredMaxFontSize;

								// Calculate window width based on content
		FontRenderContext frc = new FontRenderContext(null, false, false);
		float stringWidth;
		for (i=0; i<numItems; i++) {
			stringWidth = (float)fonts[maxFontSize].getStringBounds(items[i].label, frc).getWidth();
			if (stringWidth > width) {
				width = (int)stringWidth + borderLeft + borderRight;
			}
		}

								// Calculate max height based on window position and screen size
		Point location = getLocation();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		maxHeight = screenSize.height - location.y - 30;	   // Leave space for Start Menu Bar

		int availWidth = width - (borderLeft + borderRight);
		int availHeight = maxHeight - 2*borderY;
		int size;
		boolean done = false;
								// Calculate minimum font size, and actual window height
		do {
			for (size=maxFontSize; size >= 1; size--) {
				int tmpHeight = calculateWindowHeight(size);
				if (tmpHeight <= maxHeight) {
					height = tmpHeight;
					minFontSize = size;
					if (size == maxFontSize) {
								// If whole window at max size, then no compressed spacing
						minSpace = desiredSpace;
					} else {
						minSpace = (int)(minFontSize * minDesiredSpacing);
					}
					done = true;
					break;
				}
			}
			if (!done) {
								// If not done, there wasn't enough room, so try a smaller focus or max font size
				if (focusLength > 2) {
					focusLength -= 2;
				} else {
					maxFontSize--;
				}
			}
		} while (!done);
								// If entire menu is full size, then just use requested space.
								// Else, use all the available space.
		if (size < maxFontSize) {
			height = maxHeight;
		}

								// Set the size of the window
		setSize(width, height);
		backBuffer = null;

								// Now, calculate layout of all items for each focus
		sizeLUT = new int[numItems][numItems];
		spaceLUT = new int[numItems][numItems];
		flSizeLUT = new int[numItems];
		flSpaceLUT = new int[numItems];
		int d;
		int j, k;
		int fl2;
		int y;
		int space;
		for (i=0; i<numItems; i++) {
			fl2 = focusLength / 2;
			y = borderY;
			for (j=0; j<numItems; j++) {
								// Calculate size and spacing for the current item
				d = Math.abs(j - i);
				if (d > fl2) {
					d -= fl2;
					size = maxFontSize - d;
					space = (int)(size * desiredSpacing);
					if (size <= minFontSize) {
						size = minFontSize;
						space = minSpace;
					}
				} else {
					size = maxFontSize;
					space = desiredSpace;
				}
				sizeLUT[i][j] = size;
				spaceLUT[i][j] = space;
				y += size + space;
			}

								// If extra space, then grow items to fill it
			int extraSpace = getSize().height - y - 2*borderY;
			if (extraSpace > 0) {
				int j1, j2;
				int i1, i2;

				i1 = i - fl2 - 1;
				i2 = i + fl2 + 1;
				while ((extraSpace > 0) && ((i1 >= 0) || (i2 < numItems))) {
					j1 = i1;
					j2 = i2;
					for (k=0; k<(maxFontSize - minFontSize); k++) {
						if (j1 >= 0) {
							if (extraSpace > 0) {
								if (sizeLUT[i][j1] < maxFontSize) {
									sizeLUT[i][j1]++;
									extraSpace--;
								}
							}
							if (extraSpace > 0) {
								if (spaceLUT[i][j1] < desiredSpace) {
									spaceLUT[i][j1]++;
									extraSpace--;
								}
							}
							j1--;
						}
						if (extraSpace > 0) {
							if (j2 < numItems) {
								if (sizeLUT[i][j2] < maxFontSize) {
									sizeLUT[i][j2]++;
									extraSpace--;
								}
								if (extraSpace > 0) {
									if (spaceLUT[i][j2] < desiredSpace) {
										spaceLUT[i][j2]++;
										extraSpace--;
									}
								}
								j2++;
							}
						}
					}
					i1--;
					i2++;
				}
			}
		}

								// Calculate look-up-table of mouse position => focus index
		focusIndexLUT = new int[height];
		for (i=0; i<numItems; i++) {
			y = borderY;
			for (j=0; j<i; j++) {
				y += sizeLUT[i][j] + spaceLUT[i][j];
			}
			focusIndexLUT[y] = i;
		}
		for (i=1; i<height; i++) {
			if (focusIndexLUT[i] == 0) {
				focusIndexLUT[i] = focusIndexLUT[i-1];
			}
		}

								// Then, calculate label positions
		char currentLabel = ' ';
		char label;
		int labelY = 0;
		labelLUT = new String[26];
		labelPosLUT = new int[26];
		numLabels = 0;
		for (i=0; i<numItems; i++) {
			label = Character.toUpperCase(items[i].label.charAt(0));
			if (currentLabel != label) {
				y = borderY;
				for (j=0; j<i; j++) {
					y += sizeLUT[i][j] + spaceLUT[i][j];
				}
				y += sizeLUT[i][i];
				if (y > labelY+maxFontSize+1) {
					labelLUT[numLabels] = new Character(label).toString();
					labelPosLUT[numLabels] = y;
					numLabels++;
					labelY = y;
					currentLabel = label;
				}
			}
		}
	}

	/**
	 * Calculate the height of the window needed to render the items with
	 * fisheye distortion using the class variables such as # of elements,
	 * max font size, spacing, and focus length, and the
	 * specified minimum font size.
	 */
	public int calculateWindowHeight(int minFontSize) {
		int height;
		int fl = focusLength;
		int size;
		int space;
		int n = numItems;

		if (minFontSize == maxFontSize) {
								// If whole window in max font, then no fisheye
			height = (int)(n * (maxFontSize + desiredSpace));
			return height;
		}

								// Start with focus area
		if (n < fl) {
			fl = n;
		}
		height = (int)(fl * (maxFontSize + desiredSpace));
		n -= fl;
								// Then, calculate distortion area
		for (size=(maxFontSize-1); size > minFontSize; size--) {
			space = (int)(size * desiredSpacing);
			height += 2*(size + space);
			n -= 2;
			if (n <= 0) {
				break;
			}
		}

								// Finally, add minimum size area
		height += n * (minFontSize + (int)(minFontSize*minDesiredSpacing));

		height += 2*borderY;

		return height;
	}

	public void update(Graphics g) {
		paint(g);
	}

	public void paint(Graphics g) {
		int i;
		int x;
		int y;
		int space;
		int size;
		int fl2;
		boolean antialias = false;
		int width = getSize().width;
		int height = getSize().height;

		Rectangle clipBounds = g.getClipBounds();

								// Allocate back buffer the first time through
		if (backBuffer == null) {
			backBuffer = createImage(width, height);
			gb = backBuffer.getGraphics();
		}
		((Graphics2D)gb).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
										  RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		gb.setClip(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);

								// Fill background
		gb.setColor(backgroundColor);
		gb.fill3DRect(0, 0, width, height, true);

								// Draw FOCUS LOCK background
		y = borderY;
		int y1 = 0;
		int y2 = height - borderY;
		boolean start;
		size = 0;
		if (focusLock) {
			start = true;
			y = flFocusPosition;
			for (i=focusIndex-1; i>=0; i--) {
				size = flSizeLUT[i];
				space = flSpaceLUT[i];
				if (size == minFontSize) {
					y1 = y + size + space;
					break;
				}
				y -= size + space;
			}
			y = flFocusPosition;
			for (i=focusIndex; i<numItems; i++) {
				size = flSizeLUT[i];
				space = flSpaceLUT[i];
				if (size == minFontSize) {
					y2 = y;
					break;
				}
				y += size + space;
			}
			if (y1 < borderY) {
				y1 = borderY;
			}
		} else {
			start = true;
			for (i=0; i<numItems; i++) {
				size = sizeLUT[focusIndex][i];
				space = spaceLUT[focusIndex][i];
				if (start) {
					if (size > minFontSize) {
						y1 = y;
						start = false;
					}
				} else {
					if (size == minFontSize) {
						y2 = y - size;
						break;
					}
				}
				y += size + space;
			}
		}
		gb.setColor(Color.lightGray);
		if (focusLock) {
			gb.fillRect(width/2, y1, width/2-borderRight, y2 - y1);
		} else {
			gb.drawRect(width/2, y1, width/2-borderRight, y2 - y1);
		}

		gb.setColor(Color.black);

								// Then, render items
		x = borderLeft;
		y = borderY;
		if (!focusLock) {
			for (i=0; i<numItems; i++) {
				size = sizeLUT[focusIndex][i];
				space = spaceLUT[focusIndex][i];

				if ((y >= clipBounds.y) && (y <= (clipBounds.y + clipBounds.height))) {
								// Draw focused element with a background hilight
					if (selectionIndex == i) {
						gb.setColor(hilightColor);
						gb.fillRect(borderLeft, y, width-(borderLeft+borderRight), size + space);
						gb.setColor(Color.black);
					} else {
						gb.setColor(Color.black);
					}
								// Draw item
					if ((size > MAX_ANTIALIAS_SIZE) && antialias) {
						((Graphics2D)gb).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
														  RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
						antialias = false;
					} else if ((size <= MAX_ANTIALIAS_SIZE) && !antialias) {
						((Graphics2D)gb).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
														  RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
						antialias = true;
					}
					gb.setFont(fonts[size]);
					gb.drawString(items[i].label, x, y+size);
				}

				y += space + size;
			}
		} else {
								// FOCUS LOCK mode
								// First render from focus up
			if (focusIndex > 0) {
				y = flFocusPosition - flSizeLUT[focusIndex-1] - flSpaceLUT[focusIndex-1];
				for (i=focusIndex-1; i>=0; i--) {
					if (y < borderY) {
						break;
					}
					size = flSizeLUT[i];
					space = flSpaceLUT[i];

								// Draw focused element with a background hilight
					if (selectionIndex == i) {
						gb.setColor(hilightColor);
						gb.fillRect(borderLeft, y, width-(borderLeft+borderRight), size + space);
					}
					gb.setColor(Color.black);
								// Draw item
					if ((size > MAX_ANTIALIAS_SIZE) && antialias) {
						((Graphics2D)gb).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
														  RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
						antialias = false;
					} else if ((size <= MAX_ANTIALIAS_SIZE) && !antialias) {
						((Graphics2D)gb).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
														  RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
						antialias = true;
					}
					gb.setFont(fonts[size]);
					gb.drawString(items[i].label, x, y+size);

					if (i > 0) {
						y -= flSizeLUT[i-1] + flSpaceLUT[i-1];
					}
				}
			}
								// Then render from focus down
			y = flFocusPosition;
			for (i=focusIndex; i<numItems; i++) {
				if (y > height-borderY) {
					break;
				}
				size = flSizeLUT[i];
				space = flSpaceLUT[i];

								// Draw focused element with a background hilight
				if (selectionIndex == i) {
					gb.setColor(hilightColor);
					gb.fillRect(borderLeft, y, width-(borderLeft+borderRight), size + space);
				}
				gb.setColor(Color.black);
								// Draw item
				if ((size > MAX_ANTIALIAS_SIZE) && antialias) {
					((Graphics2D)gb).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
													  RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
					antialias = false;
				} else if ((size <= MAX_ANTIALIAS_SIZE) && !antialias) {
					((Graphics2D)gb).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
													  RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					antialias = true;
				}
				gb.setFont(fonts[size]);
				gb.drawString(items[i].label, x, y+size);

				y += space + size;
			}
		}
								// Draw alphabetic labels
		gb.setFont(boldFonts[maxFontSize]);
		gb.setColor(labelColor);
		((Graphics2D)gb).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
										  RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		for (i=0; i<numLabels; i++) {
			gb.drawString(labelLUT[i], labelBorderLeft, labelPosLUT[i]);
		}

								// Draw FOCUS LOCK background arrows
		if (y1 > borderY) {
			gb.drawImage(up, 3*width/4, y1+5, this);
		}
		if (y2 < height-borderY) {
			gb.drawImage(down, 3*width/4, y2-15, this);
		}

								// Swap buffers
		g.drawImage(backBuffer, 0, 0, this);
	}
}

class Item {
	JMenuItem menuItem;
	String label;
	int size;
	int space;
}
