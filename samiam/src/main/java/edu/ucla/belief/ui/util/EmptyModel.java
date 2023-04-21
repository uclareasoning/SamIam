package edu.ucla.belief.ui.util;

/**
	@author Keith Cascio
	@since 061304
*/
public class EmptyModel implements javax.swing.ListModel, javax.swing.ComboBoxModel
{
	public static final EmptyModel getInstance(){
		if( INSTANCE == null ) INSTANCE = new EmptyModel();
		return INSTANCE;
	}
	private static EmptyModel INSTANCE;
	private EmptyModel() { return; }
	public int getSize() { return 0; }
	public Object getElementAt(int index) { return null; }
	public void addListDataListener( javax.swing.event.ListDataListener l) { return; }
	public void removeListDataListener( javax.swing.event.ListDataListener l) { return; }
	public Object getSelectedItem() { return null; }
	public void setSelectedItem(Object anItem) { return; }
}
