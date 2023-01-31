package edu.ucla.util;

import edu.ucla.belief.*;

import java.util.*;

/** @author keith cascio
	@since  20040712 */
public class EvidenceAssertedProperty extends FlagProperty
{
	public static final EvidenceAssertedProperty PROPERTY = new EvidenceAssertedProperty();

	private EvidenceAssertedProperty() {}

	public String getName(){
		return "evidence";
	}

	public EnumValue getDefault(){
		return FALSE;
	}

	public String getID(){
		return "isevidence";
	}

	public boolean isUserEditable(){
		return false;
	}

	public boolean isTransient(){
		return true;
	}

	public static FlagValue valueFor( Variable var, EvidenceController controller )
	{
		return ( controller.getValue(var) == null ) ? PROPERTY.FALSE : PROPERTY.TRUE;
	}

	public static void setValue( Variable var, EvidenceController controller )
	{
		var.setProperty( PROPERTY, valueFor( var, controller ) );
	}

	public static void setAllValues( Collection vars, EvidenceController controller )
	{
		for( Iterator it = vars.iterator(); it.hasNext(); )
			setValue( (Variable)it.next(), controller );
	}
}
