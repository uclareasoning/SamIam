package edu.ucla.belief.ui.util;

import javax.swing.*;
import javax.swing.text.*;
import java.text.*;

/** @author keith cascio
	@since  The Beginning of Known Time */
public class NotifyField extends JTextField
{
	public NotifyField(String value, int columns)
	{
		super(value,columns);
	}

	protected Document createDefaultModel()
	{
		return new NotifyDocument()
		{
			public void fireActionPerformed()
			{
				NotifyField.this.fireActionPerformed();
			}

			/** @since 20040805 */
			public String getLastValidData(){
				return STR_EMPTY;
			}
		}.setJTextComponent( this );//since 20080626
	}
}
