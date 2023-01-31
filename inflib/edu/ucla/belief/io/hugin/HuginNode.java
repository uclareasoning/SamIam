package edu.ucla.belief.io.hugin;

import edu.ucla.belief.io.*;
import edu.ucla.belief.*;

/**
* A class for encapsulating a hugin node description.
*/
public interface HuginNode extends StandardNode, HuginReaderConstants
{
	public int getValueType();
	public int getNodeType();
	public boolean isSpecifiedDimension();
	public void resetSpecifiedDimension();
}
