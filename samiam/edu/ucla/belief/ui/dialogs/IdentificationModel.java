package edu.ucla.belief.ui.dialogs;

import java.util.Set;
import javax.swing.ListCellRenderer;

/** Express everything needed to carry out
	the process of identifying tokens
	from a data file.

	@author Keith Cascio
	@since 020405 */
public interface IdentificationModel extends ListCellRenderer
{
	public Object[] getRange();
	public Object guess( String token );
	public Object getElementUnidentified();
	public boolean isIdentified( Set selected );
	public boolean isOneToOne();
	public Stage getStage();
	//public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus );
}
