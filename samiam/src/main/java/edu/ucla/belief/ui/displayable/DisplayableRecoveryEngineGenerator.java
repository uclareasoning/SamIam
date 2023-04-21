package edu.ucla.belief.ui.displayable;

import        edu.ucla.belief.*;
import        edu.ucla.belief.approx.*;
import        edu.ucla.belief.approx. RecoverySetting;
import static edu.ucla.belief.approx. RecoverySetting.*;
import        edu.ucla.belief.io. PropertySuperintendent;

import        edu.ucla.belief.ui. UI;

import        java.util.*;
import        javax.swing.*;
import        java.awt.*;

/** based on DisplayableEdgeDeletionEngineGenerator

	@author keith cascio
	@since  20091207 */
public class DisplayableRecoveryEngineGenerator extends DisplayableApproxEngineGenerator<RecoverySetting>
{
	public String getTitle(){ return "edge-deletion belief-propagation"; }

	public DisplayableRecoveryEngineGenerator( RecoveryEngineGenerator peg, UI ui ){
		super( peg, ui );
	}
}
