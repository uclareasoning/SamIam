package edu.ucla.belief;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
	<p>
	The listener interface for evidence changes sent by
	EvidenceController.
	<p>
	Removed from samiam package edu.ucla.belief.ui.event;

	@author Keith Cascio
	@since 071802
*/
public class EvidenceChangeEvent
{
	public Collection recentEvidenceChangeVariables = null;

	public static EvidenceChangeEvent EMPTY_EVENT = new EvidenceChangeEvent( Collections.EMPTY_SET );

	public EvidenceChangeEvent( Collection recentEvidenceChangeVariables )
	{
		this.recentEvidenceChangeVariables = recentEvidenceChangeVariables;
	}

	/**
		@since 071003
	*/
	public String toString()
	{
		String ret = "EvidenceChangeEvent ";
		if( recentEvidenceChangeVariables != null ) ret += recentEvidenceChangeVariables.toString();
		return ret;
	}

	/**
		@since 073003
	*/
	public String describe( EvidenceController controller )
	{
		String ret = "";
		if( recentEvidenceChangeVariables != null )
		{
			FiniteVariable var;
			Object value;
			String toAppend;
			for( Iterator it = recentEvidenceChangeVariables.iterator(); it.hasNext(); )
			{
				var = (FiniteVariable)it.next();
				value = controller.getValue( var );
				toAppend = var.toString();
				if( value == null ) toAppend += " unobserved";
				else toAppend += " = " + value.toString();
				if( it.hasNext() ) toAppend += ", ";
				ret += toAppend;
			}
		}
		return ret;
	}
}
