package edu.ucla.belief.ui.displayable;

//import edu.ucla.belief.decision.*;
//import edu.ucla.belief.FiniteVariable;

//import edu.ucla.belief.ui.event.*;
//import edu.ucla.belief.ui.util.*;
//import edu.ucla.belief.ui.*;

//import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
//import java.util.*;
import java.awt.*;
import java.awt.event.*;

/**
	@author Keith Cascio
	@since 121204
*/
public class GroupingColoringPopupHandler implements MouseListener, MenuListener, ActionListener
{
	public static final double DOUBLE_SPINNER_MIN = (double).1;
	public static final double DOUBLE_SPINNER_MAX = (double)1;
	public static final double DOUBLE_SPINNER_STEP = (double).1;
	public static final double DOUBLE_SPINNER_VALUE = (double).1;

	public GroupingColoringPopupHandler( GroupingColoringTableModel model )
	{
		this.myGroupingColoringTableModel = model;
		init();
	}

	public void addItemViewOptions( JMenuItem item ){
		myMenuViewOptions.insert( item, 0 );
	}

	public void insertItem( JMenuItem item, int index ){
		myJPopupMenu.insert( item, index );
	}

	public void addPopupMenuListener( PopupMenuListener listener ){
		myJPopupMenu.addPopupMenuListener( listener );
	}

	public void removePopupMenuListener( PopupMenuListener listener ){
		myJPopupMenu.removePopupMenuListener( listener );
	}

	private void init(){
		myJPopupMenu = new JPopupMenu( "table" );

		myJPopupMenu.add( myMenuViewOptions = new JMenu( "view options" ) );
		myMenuViewOptions.addMenuListener( (MenuListener)this );

		myMenuViewOptions.add( myItemDoGrouping = new JCheckBoxMenuItem( "group table" ) );
		myItemDoGrouping.addActionListener( (ActionListener)this );

		myMenuViewOptions.add( myItemDoColoring = new JCheckBoxMenuItem( "color table" ) );
		myItemDoColoring.addActionListener( (ActionListener)this );

		SpinnerNumberModel model = new SpinnerNumberModel( DOUBLE_SPINNER_VALUE, DOUBLE_SPINNER_MIN, DOUBLE_SPINNER_MAX, DOUBLE_SPINNER_STEP );
		mySpinnerSaturation = new JSpinner( model );
		JPanel panelSpinner = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 1;
		panelSpinner.add( new JLabel( "saturation: ", JLabel.LEFT ), c );
		c.gridwidth = GridBagConstraints.REMAINDER;
		panelSpinner.add( mySpinnerSaturation, c );
		myMenuViewOptions.add( panelSpinner );
	}

	protected boolean showPopup( MouseEvent e )
	{
		if(	e.isPopupTrigger() ){
			myJPopupMenu.show( myGroupingColoringTableModel.getJTable(), e.getX(), e.getY() );
			return true;
		}

		return false;
	}

	private void validateViewOptions(){
		myItemDoColoring.setSelected( myGroupingColoringTableModel.isColoringEnabled() );
		myItemDoGrouping.setSelected( myGroupingColoringTableModel.isGroupingEnabled() );
		mySpinnerSaturation.setValue( new Double( myGroupingColoringTableModel.getSaturation() ) );
	}

	/** interface ActionListener */
	public void actionPerformed( ActionEvent e ){
		Object src = e.getSource();
		try{
		if( src == myItemDoColoring ){
			myGroupingColoringTableModel.setColoringEnabled( myItemDoColoring.isSelected() );
			myGroupingColoringTableModel.getJTable().repaint();
		}
		else if( src == myItemDoGrouping ) myGroupingColoringTableModel.setGroupingEnabled( myItemDoGrouping.isSelected() );
		}catch( InterruptedException interruptedexception ){
			System.err.println( "Warning: " +this.getClass().getName()+ ".actionPerformed() interrupted" );
			Thread.currentThread().interrupt();
		}
	}

	/** interface MenuListener */
	public void menuSelected(MenuEvent e){
		Object src = e.getSource();
		if( src == myMenuViewOptions ) validateViewOptions();
	}
	/** interface MenuListener */
	public void menuDeselected(MenuEvent e){
		menuByeBye( e );
	}
	/** interface MenuListener */
	public void menuCanceled(MenuEvent e){
		menuByeBye( e );
	}

	private void menuByeBye( MenuEvent e ){
		Object src = e.getSource();
		if( src == myMenuViewOptions ) handleSaturation();
	}

	private void handleSaturation(){
		myGroupingColoringTableModel.setSaturation( ((Number)mySpinnerSaturation.getValue()).floatValue() );
		myGroupingColoringTableModel.getJTable().repaint();
	}

	/** interface MouseListener */
	public void mouseClicked(MouseEvent e){
		if( showPopup(e) ) return;
	}
	public void mousePressed(MouseEvent e){
		if( showPopup(e) ) return;
	}
	public void mouseReleased(MouseEvent e){
		if( showPopup(e) ) return;
	}
	public void mouseEntered(MouseEvent e){
	}
	public void mouseExited(MouseEvent e){
	}

	private GroupingColoringTableModel myGroupingColoringTableModel;
	private JPopupMenu myJPopupMenu;
	private JMenu myMenuViewOptions;
	private JCheckBoxMenuItem myItemDoColoring;
	private JCheckBoxMenuItem myItemDoGrouping;
	private JSpinner mySpinnerSaturation;
}
