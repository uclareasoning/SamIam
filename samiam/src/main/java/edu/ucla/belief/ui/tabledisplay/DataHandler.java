package edu.ucla.belief.ui.tabledisplay;

import javax.swing.table.*;

/** @author keith cascio
	@since  20030721 */
public interface DataHandler extends TableCellRenderer, TableCellEditor
{
	/** @since 20080229 */
	public boolean             minimize();
	public String        getDisplayName();
	public Object            getValueAt( int linearIndex );
	public void              setValueAt( Object aValue, int linearIndex );
	public Object        getEventSource();
	public boolean             isEdited();
	public void               setEdited( boolean flag );
	public boolean handlesProbabilities();
	public void          setStainWright( StainWright wright );
}
