package edu.ucla.belief.ui.displayable;

import java.util.*;
import edu.ucla.structure.*;
import edu.ucla.belief.*;
import edu.ucla.belief.io.*;
import edu.ucla.belief.io.hugin.*;
import edu.ucla.belief.io.dsl.*;
import edu.ucla.belief.ui.NetworkInternalFrame;

/**
	@author Keith Cascio
	@since 100102
*/
public interface DisplayableBeliefNetwork extends BeliefNetwork, HuginNet, GenieNet
{
	public NetworkInternalFrame getNetworkInternalFrame();
	public boolean isHuginNet();
	public boolean isGenieNet();
	public DSLSubmodel getMainDSLSubmodel();
	public DFVIterator dfvIterator();
	public BeliefNetwork getSubBeliefNetwork();
	public int countUserEnumPropertiesInitial();
}
