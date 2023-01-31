package edu.ucla.belief.ui.dialogs;

import edu.ucla.belief.*;
import edu.ucla.belief.io.*;

import java.util.List;
import java.util.*;
import javax.swing.*;
import java.awt.*;

/** Model supports identifying the columns
	of a tab-delimited data file containing CPT
	data.  The columns will represent either
	variable instances or probability data.

	@author Keith Cascio
	@since 020405 */
public class ColumnIdentificationModel extends AbstractIdentificationModel
{
	public ColumnIdentificationModel( FiniteVariable fv, Stage stage ){
		super( stage );
		this.myVariable = fv;
	}

	public Color getColor( Object value ){
		if( value == CPTImportWizard.STR_PROBABILITY ) return COLOR_DARK_BLUE;
		else if( value instanceof Variable ) return COLOR_DARK_GREEN;
		else return Color.black;
	}

	public Object[] makeRange(){
		myVariables = myVariable.getCPTShell( myVariable.getDSLNodeType() ).index().variables();

		Object[] ret = new Object[ myVariables.size() + 2 ];
		int index = 0;
		ret[index++] = getElementUnidentified();
		for( Iterator it = myVariables.iterator(); it.hasNext(); ){
			ret[index++] = it.next();
		}
		ret[index++] = CPTImportWizard.STR_PROBABILITY;
		return ret;
	}

	public static final String[] ARRAY_PROBABILITY_ID = new String[] { "probability", "conditional", "prob" };

	public Object guess( String token ){
		String tokenlower = token.trim().toLowerCase();

		for( int i=0; i<ARRAY_PROBABILITY_ID.length; i++ ){
			if( guess( tokenlower, ARRAY_PROBABILITY_ID[i] ) ) return CPTImportWizard.STR_PROBABILITY;
		}

		StandardNode standard;
		for( Iterator it = myVariables.iterator(); it.hasNext(); ){
			standard = (StandardNode) it.next();
			if( guess( tokenlower, standard ) ){
				return standard;
			}
		}

		return getElementUnidentified();
	}

	private boolean guess( String tokenlower, StandardNode standard ){
		String idlower = standard.getID().trim().toLowerCase();
		String labellower = standard.getLabel().trim().toLowerCase();

		if( guess( idlower, tokenlower ) ) return true;
		if( guess( labellower, tokenlower ) ) return true;
		if( guess( tokenlower, idlower ) ) return true;
		if( guess( tokenlower, labellower ) ) return true;

		return false;
	}

	public boolean isIdentified( Set selected ){
		if( !selected.contains( CPTImportWizard.STR_PROBABILITY ) ) return false;
		selected.remove( CPTImportWizard.STR_PROBABILITY );
		selected.remove( getElementUnidentified() );
		return !selected.isEmpty();
	}

	private FiniteVariable myVariable;
	private List myVariables;
}
