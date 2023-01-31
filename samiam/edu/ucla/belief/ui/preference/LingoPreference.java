package edu.ucla.belief.ui.preference;

import edu.ucla.belief.ui.actionsandmodes.*;
import edu.ucla.belief.ui.actionsandmodes.Grepable.Flag;
import edu.ucla.belief.ui.actionsandmodes.Grepable.Angle;
import edu.ucla.belief.ui.actionsandmodes.Grepable.Presentation;
import edu.ucla.belief.ui.actionsandmodes.Grepable.Vocabulary;
import edu.ucla.belief.ui.actionsandmodes.Grepable.Preset;
import static edu.ucla.belief.ui.actionsandmodes.Grepable.Preset.simpleliteral;
import edu.ucla.belief.ui.util.Util;
import edu.ucla.belief.ui.util.ElementHandler;
import edu.ucla.belief.ui.UI;

import org.xml.sax.SAXException;
import org.xml.sax.Attributes;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.lang.reflect.*;

/** Controls the "vocabulary" or "lingo" or "dialect"
	available for various grep actions.

	@author keith cascio
	@since  20070403 */
public class LingoPreference extends AbstractPreference
{
	public LingoPreference( String key, String name, Vocabulary defaultValue ){
		super( key, name, defaultValue );
	}

	static public LingoPreference bootstrap(){
		if( BOOTSTRAP != null ) return BOOTSTRAP;
		return BOOTSTRAP = new LingoPreference( SamiamPreferences.STR_GREP_VOCABULARY, "vocabulary", simpleliteral.vocabulary );
	}
	static private LingoPreference BOOTSTRAP;

	public boolean displayCaption(){ return false; }

	protected JComponent getEditComponentHook(){
		if( myTable == null ) myTable = new Table();
		collapseAll();
		clearPreset();
		return myTable;
	}

	/** @since 20070403 */
	public class Table extends JPanel{
		public Table(){
			super( new GridBagLayout() );
			init();
		}

		private void init(){
			LingoPreference.this.myButtonsImplicit       = new EnumMap<Flag,AbstractButton>( Flag.class );
			LingoPreference.this.myButtonsRepresentative = new EnumMap<Flag,AbstractButton>( Flag.class );
			LingoPreference.this.myButtons               = new HashMap<AbstractButton,Flag>( Flag.values().length << 1 );

			LingoPreference.this.myGroupsImplicit        = new EnumMap<Angle,ButtonGroup>(  Angle.class );
		  //LingoPreference.this.myGroupsRepresentative  = new EnumMap<Angle,ZeroOrOne>(    Angle.class );
			LingoPreference.this.myCombos                = new EnumMap<Angle,JComboBox>(    Angle.class );

			GridBagConstraints c                         = new GridBagConstraints();
			JLabel             label                     = null;
			ButtonGroup        groupImplicit             = null;
			ZeroOrOne          groupRepr                 = null;
			AbstractButton     button                    = null;
			JComboBox          combo                     = null;
			Font               bold                      = null;
			Component          leading                   = null;
			Color              colorSymb                 = Color.orange.darker().darker();

			myOrdinals                                   = new Integer[ Angle.values().length ];
			for( int i=0; i<myOrdinals.length; i++ ) myOrdinals[i] = new Integer( i );
		  //ComboBoxModel      model                     = new DefaultComboBoxModel( myOrdinals );
			JComboBox[]        combos                    = new JComboBox[ Angle.values().length ];

			Vamoose            vamoose                   = new Vamoose();

			c.gridwidth  = 1;
			c.anchor     = GridBagConstraints.NORTHWEST;
			c.ipadx      = 0x10;
			c.fill       = GridBagConstraints.NONE;

			Table.this.add( label = new JLabel( "option"   ), c );
			label.setFont( bold = label.getFont().deriveFont( Font.BOLD ) );
			label.setToolTipText( "<html>modify <font color='#cc6600'>"+SamiamPreferences.STR_GREP_DISPLAY_NAME_LOWER+"'s</font> behavior to suit your needs" );

			c.weightx    = 1;
			Table.this.add( Box.createGlue(), c );

			c.weightx    = 0;
			Table.this.add( label = new JLabel( "symbol"   ), c );
			label.setFont( bold );
			label.setToolTipText( "<html><font color='#cc6600'>identifier</font> shown on a toggle button" );

			Table.this.add( label = new JLabel( "name"     ), c );
			label.setFont( bold );
			label.setToolTipText( "<html>the option's <font color='#cc6600'>meaning</font>" );

			Table.this.add( label = new JLabel( "show"     ), c );
			label.setFont( bold );
			label.setToolTipText( "<html>choose which flag(s) " + UI.STR_SAMIAM_ACRONYM + " should use to <font color='#cc6600'>represent</font> the option, or none to hide it" );

			Table.this.add( label = new JLabel( "implicit" ), c );
			label.setFont( bold );
			label.setToolTipText( "<html>choose the <font color='#cc6600'>default</font> value of the option" );

			c.gridwidth  = GridBagConstraints.REMAINDER;
			Table.this.add( label = new JLabel( "position" ), c );
			label.setFont( bold );
			label.setToolTipText( "<html>choose the <font color='#cc6600'>order</font> the options appear in sequence" );

			String strExpand = Flag.supported( "\u25b6", "\u25ba", "\u25b7", "\u25bb", "\u25b8", "\u25b9", ">" );

			for( Angle angle : Angle.values() ){
				c.gridwidth  = GridBagConstraints.REMAINDER;
				Table.this.add( Box.createVerticalStrut( 0x10 ), c );//0x20 ),      c );

				leading = label = new JLabel( angle.name );
				label.setToolTipText( angle.description );

				//myGroupsRepresentative.put( angle, groupRepr = new ZeroOrOne()   );
				myGroupsImplicit      .put( angle, groupImplicit       = new ButtonGroup() );
				groupImplicit.add( new JToggleButton( angle + " phantom imp" ) );
				for( Flag flag : angle.canonicalize() ){
					c.gridwidth  = 1;

					Table.this.add( leading,                           c );

					c.weightx    = 1;
					c.ipadx      = 0;
					Table.this.add( leading instanceof JLabel ? listen( myCurrentExpander = new JButton(strExpand) ) : Box.createGlue(), c );
					myCurrentExpander.setToolTipText( "<html>expand <font color='#cc6600'>" + angle.name );
					c.ipadx      = 0x10;

					c.weightx    = 0;
					Table.this.add( collapse( label = new JLabel( flag.symbol() ) ), c );
					label.setFont(        bold      );
					label.setToolTipText( "<html><font color='#cc6600'>" + flag.symbol() + "</font> " + flag.name );
					label.setForeground(  colorSymb );

					Table.this.add( collapse( label = new JLabel( flag.name ) ),   c );
					label.setToolTipText( flag.description );

					Table.this.add( collapse( button = new JCheckBox() ),          c );
					button.addActionListener( LingoPreference.this );
					button.setToolTipText( "<html>show <font color='#cc6600'>" + flag.name );
					button.addActionListener( vamoose );//groupRepr );
					myButtonsRepresentative.put( flag, button );
					myButtons.put(               button, flag );

					Table.this.add( collapse( button = new JRadioButton() ),       c );
					button.addActionListener( LingoPreference.this );
					button.setToolTipText( "<html>make <font color='#cc6600'>" + flag.name + "</font> the default " + angle.name );
					button.addActionListener( vamoose );
					groupImplicit.add(           button );
					myButtonsImplicit.put( flag, button );
					myButtons.put(         button, flag );

					c.gridwidth  = GridBagConstraints.REMAINDER;
					if( leading instanceof JLabel ){
						Table.this.add( collapse( combo = new JComboBox( myOrdinals ) ), c );
						combo.addActionListener( LingoPreference.this );
						combo.setToolTipText( "<html>location of <font color='#cc6600'>"+angle.name+"</font> in sequence" );
						myCombos.put( angle, combo );
						combos[ angle.ordinal() ] = combo;
					}
					else Table.this.add( Box.createGlue(),                   c );

					leading = Box.createGlue();
				}
			}

			myCurrentExpander = null;

			JPanel             pnlPreset = new JPanel( new GridBagLayout() );
			GridBagConstraints   cPreset = new GridBagConstraints();
			cPreset.anchor     = GridBagConstraints.WEST;
			pnlPreset.add(         new JLabel( "or choose preset options:" ), cPreset );
			pnlPreset.add(                 Box.createHorizontalStrut( 0x10 ), cPreset );
			cPreset.gridwidth  = GridBagConstraints.REMAINDER;
			cPreset.fill       = GridBagConstraints.HORIZONTAL;
			cPreset.weightx    = 1;
			pnlPreset.add( myComboPresets = new JComboBox( Preset.values() ){
				public void updateUI(){
					setRenderer( null );
					super.updateUI();
					final ListCellRenderer rendrr = getRenderer();
					/*myComboPresets*/this.setRenderer( new ListCellRenderer(){
						public Component  getListCellRendererComponent( JList list, Object   value,            int index, boolean isSelected, boolean cellHasFocus ){
							return rendrr.getListCellRendererComponent(       list, ((Preset)value).displayname,   index,         isSelected,         cellHasFocus );
						}
					} );
				}
			}, cPreset );
			myComboPresets.updateUI();
			myComboPresets.addItemListener( new ItemListener(){
				public void itemStateChanged( ItemEvent e ){
					try{
						Vocabulary vocab = ((Preset)e.getItem()).vocabulary;
						if( vocab == null ) return;
						hookSetEditComponentValue( vocab );
						LingoPreference.this.myIsEdited = true;
					}catch( Exception exception ){
						System.err.println( "warning: LingoPreference.ItemListener.itemStateChanged() caught " + exception );
					}
				}
			} );
		  //pnlPreset.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );

			Table.this.add( Box.createVerticalStrut( 0x10 ), c );//0x20 ),      c );
			c.anchor     = GridBagConstraints.EAST;
			Table.this.add(                       pnlPreset, c );

			TotalOrder to = new TotalOrder( combos, myOrdinals );

			Table.this.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(2,8,2,8) ) );
		}
	}

	/** @since 20070423 */
	private <T extends Component> T collapse( T tohide ){
		if( myHidden == null ) myHidden = new HashMap<AbstractButton,Collection<Component>>( Angle.values().length );
		Collection<Component> comps = myHidden.get( myCurrentExpander );
		if( comps == null ) myHidden.put( myCurrentExpander, comps = new LinkedList<Component>() );
		comps.add( tohide );
		tohide.setVisible( false );
		return tohide;
	}

	/** @since 20070423 */
	private void collapseAll(){
		if( myHidden == null ) return;
		for( AbstractButton expander : myHidden.keySet() ){
			try{
				Collection<Component> comps = myHidden.get( expander );
				for( Component comp : comps ) comp.setVisible( false );
				expander.setEnabled( true );
				expander.setVisible( true );
				expander.getParent().setPreferredSize( null );
			}catch( Exception exception ){
				System.err.println( "warning: LingoPreference.collapseAll() caught " + exception );
			}
		}
	}

	/** @since 20070423 */
	private Collection<Component> expand( AbstractButton expander ) throws Exception{
		if( myHidden == null ) return null;
		Collection<Component> comps = myHidden.get( expander );
		if(    comps == null ) return null;
		for( Component comp : comps ) comp.setVisible( true );

		expander.setEnabled( false );
		expander.setVisible( false );

		JComponent parent = (JComponent) expander.getParent();
		//parent.removeAll();
		//parent.setLayout( null );
		//parent.revalidate();
		parent.setPreferredSize( new Dimension(0,0) );

		/*Dimension size = expander.getSize();
		expander.setVisible( false );
		expander.setPreferredSize( size );
		expander.setMinimumSize(   size );*/

		/*GridBagConstraints c = myGridBag.getConstraints( expander );
		myTable.remove( expander );
		myTable.add( new JLabel( "smjorn" ), c );
		myTable.revalidate();*/

		Window window = SwingUtilities.getWindowAncestor( myTable );
		if( window == null ) return comps;
		window.pack();
		Util.centerWindow( window );

		return comps;
	}

	/** @since 20070423 */
	private <B extends AbstractButton> JComponent listen( B expander ){
		if( myExpansionListener == null ) myExpansionListener = new ActionListener(){
			public void actionPerformed( final ActionEvent evt ){
				try{
					new Thread( new Runnable(){
						public void run(){
							try{
								expand( (AbstractButton) evt.getSource() );
							}catch( Exception exception ){
								System.err.println( "warning: LingoPreference.runExpand.run() caught " + exception );
							}
						}
					} ).start();
				}catch( Exception exception ){
					System.err.println( "warning: LingoPreference.myExpansionListener.aP() caught " + exception );
				}
			}
		};
		expander.addActionListener( myExpansionListener );
		Presentation.configure( expander );
		JPanel pnl = new JPanel( new BorderLayout() );
		pnl.add( expander );
		return pnl;
	}

	/** @since 20070423 */
	public void actionPerformed( ActionEvent evt ){
		super.actionPerformed( evt );
		clearPreset();
	}

	/** @since 20070423 */
	private void clearPreset(){
		if( myComboPresets != null && myFlagListeningEnabled ) myComboPresets.setSelectedItem( Preset.custom );
	}

	public Object hookValueClone(){
		return new Vocabulary( (Vocabulary) myValue );
	}

	public void hookSetEditComponentValue( Object newVal ){
		if( (myTable == null) || (!(newVal instanceof Vocabulary)) ) return;

		setListeningEnabled( false );

		Vocabulary vocab = (Vocabulary) newVal;
		Flag       flag  = null;
		for( Angle angle : Angle.values() ){
			//myGroupsRepresentative.get( angle ).clearSelection();
			for( Flag toClear : angle.membership              ) myButtonsRepresentative.get( toClear ).setSelected( false );
			for( Flag rep     : vocab.representation( angle ) ) myButtonsRepresentative.get( rep     ).setSelected( true  );

			clearSelection(       myGroupsImplicit.get( angle ) );
			myButtonsImplicit.get( flag = vocab.implicit( angle ) ).setSelected( true );

			myCombos.get( angle ).setSelectedItem( myOrdinals[ vocab.projection.get( angle ).ordinal ] );
		}

		setListeningEnabled( true );
	}

	static public void clearSelection( ButtonGroup group ){
		try{
			if( myFlagMethodClearSelection ){
				myMethodClearSelection = ButtonGroup.class.getMethod( "clearSelection" );
				myFlagMethodClearSelection = false;
			}
			if( myMethodClearSelection != null ){
				myMethodClearSelection.invoke( group );
				return;
			}
		}catch( Throwable throwable ){
			if( Util.DEBUG_VERBOSE ) System.err.println( "warning: LingoPreference.clearSelection() caught " + throwable );
		}
		/*for( Enumeration<AbstractButton> enumeration = group.getElements(); enumeration.hasMoreElements(); ){
			enumeration.nextElement().setSelected( false );
		}*/
		group.getElements().nextElement().setSelected( true );
	}

	/** Like ButtonGroup but allows de-selection
		@since 20070403 */
	static public class ZeroOrOne implements ItemListener{
		public void itemStateChanged( ItemEvent event ){
			AbstractButton src = (AbstractButton) event.getSource();
			if( selected != src && selected != null ) selected.setSelected( false );
			selected = src;
		}

		public AbstractButton clearSelection(){
			if( selected != null ) selected.setSelected( false );
			return selected;
		}

		private AbstractButton selected;
	}

	/** flips, stays one step ahead, chasing lights
		enforces rule: if( representatives.size() == 1 ){ implicit != representative };
		@since 20070403 */
	public class Vamoose implements ActionListener{
		public void actionPerformed( ActionEvent event ){
			try{
				Object src = event.getSource();
				if(      src instanceof JCheckBox    ) representation( (JCheckBox)    src );
				else if( src instanceof JRadioButton ) implication(    (JRadioButton) src );
			}catch( Exception exception ){
				System.err.println( "warning: LingoPreference.Vamoose.itemStateChanged() caught " + exception );
			}
		}

		private boolean    implication( JRadioButton src ) throws Exception{
			if( ! src.isSelected() ) return false;
			LingoPreference.this.myButtonsRepresentative.get( LingoPreference.this.myButtons.get( src ) ).setSelected( false );
			return true;
		}

		private boolean representation( JCheckBox    src ) throws Exception{
			int     count   = 0;
			Flag    next    = null;
			boolean crowded = false;
			for( Flag flag : Angle.forFlag( LingoPreference.this.myButtons.get( src ) ).membership ){
				if( LingoPreference.this.myButtonsRepresentative.get( flag ).isSelected() ){
					++count;
					crowded = LingoPreference.this.myButtonsImplicit.get( flag ).isSelected();
				}
				else next = flag;
			}

			if( crowded = ( count == 1 && crowded ) ) LingoPreference.this.myButtonsImplicit.get( next ).setSelected( true );
			return crowded;
		}
	}

	/** Remember the state of a JComboBox in order to swap values.
		@since 20070403 */
	static public class Memory{
		public Memory( JComboBox box, Object item ){
			this.box  = box;
			this.item = item;
		}

		/** @return the newly set item */
		public Object swap( Memory stale ){
			return this.item = stale.set( this.item );
		}

		/** @return the previously set item */
		public Object set( Object item ){
			Object ret = this.item;
			this.box.setSelectedItem( this.item = item );
			return ret;
		}

		public final JComboBox box;
		public       Object    item;
	}

	/** A collection of combo boxes which each contain the same list of elements.
		Make sure an element can only be selected in one combo box at a time.
		@since 20070403 */
	static public class TotalOrder implements ItemListener{
		public TotalOrder( JComboBox[] boxes, Object[] items ){
			myMemory = new Memory[ boxes.length ];
			for( int i=0; i<boxes.length; i++ ){
				myMemory[i] = new Memory( boxes[i], items[i] );
				boxes[i].addItemListener( TotalOrder.this );
			}
		}

		public void itemStateChanged( ItemEvent event ){
			JComboBox src      = (JComboBox) event.getSource();
			Object    selected = src.getSelectedItem();

			Memory    source = null, stale = null;
			for( Memory mem : myMemory ){
				if(      mem.box                   == src      ) source = mem;
				else if( mem.box.getSelectedItem() == selected ) stale  = mem;
			}

			if( stale != null ) if( source.swap( stale ) != selected ) throw new IllegalStateException();
		}

		private Memory[] myMemory;
	}

	public Object getCurrentEditedValue(){
		Map<Angle,Presentation> projection = new EnumMap<Angle,Presentation>( Angle.class );

		Presentation presentation = null;
		Flag         representative, implicit;
		Set<Flag>    representation = EnumSet.noneOf( Flag.class );
		int          ordinal = -1;
		for( Angle angle : Angle.values() ){
			representative = implicit = null;
			representation.clear();
			for( Flag flag : angle.membership ){
				if( myButtonsRepresentative.get( flag ).isSelected() ) representation.add( representative = flag );
				if( myButtonsImplicit      .get( flag ).isSelected() )                     implicit       = flag;
			}
			ordinal = ((Integer)myCombos.get( angle ).getSelectedItem()).intValue();
			projection.put( angle, presentation = new Presentation( angle, representation, implicit, ordinal ) );
		}

		return new Vocabulary( projection );
	}

	protected String valueToString(){
		return "";
	}

	public Object parseValue( String strVal ) throws Exception{
		return null;
	}

	/** @since 20070404 */
	public StringBuffer appendXML( StringBuffer buff ){
		appendableXML( buff );
		return buff;
	}

	/** @since 20070404 */
	public Appendable appendableXML( Appendable buff ){
		try{
			String classname = getClass().getName();
			buff.append( "<pref class=\"" ).append( classname ).append( "\" key=" );
			quoteXMLValue( buff, getKey().toString() ).append( " name="  );
			quoteXMLValue( buff, getDisplayName()    ).append( " value=\"\" >\n" );
			((Vocabulary)myValue).appendXML( buff );
			buff.append( "\t\t</pref>" );
		}catch( Exception exception ){
			System.err.println( "warning: LingoPreference.appendableXML() caught " + buff );
		}
		return buff;
	}

	/** @since 20070404 */
	static public Appendable quoteXMLValue( Appendable buff, String xmlValue ) throws Exception{
		char delim = (xmlValue.indexOf( '"' ) >= 0) ? '\'' : '"';
		return buff.append( delim ).append( xmlValue ).append( delim );
	}

	/** @since 20070404 */
	public ElementHandler getElementHandler(){
		if( myElementHandler == null ) myElementHandler = new ElementHandler(){
			public boolean startElement( String uri, String localName, String qName, Attributes attributes ) throws SAXException{
				boolean ret = false;
				try{
					if( ! qName.equals( Presentation.STR_ELEMENT_PRESENTATION ) ) return ret = false;

					String strAngle = attributes.getValue( Presentation.STR_ATTR_ANGLE          );
					String strRepre = attributes.getValue( Presentation.STR_ATTR_REPRESENTATION );
					String strImpli = attributes.getValue( Presentation.STR_ATTR_IMPLICIT       );
					String strOrdin = attributes.getValue( Presentation.STR_ATTR_ORDINAL        );

					Angle     angle          = Angle   .valueOf( strAngle );
					Set<Flag> representation = (strRepre != null && strRepre.length() > 0) ?
						Flag.valueOf( Flag.class, strRepre, Presentation.PATTERN_DELIMITERS ) :
						Flag.NONE;
					Flag      implicit       = Flag    .valueOf( strImpli );
					int       ordinal        = Integer.parseInt( strOrdin );

					projection.put( angle, new Presentation( angle, representation, implicit, ordinal ) );

					ret = true;
				}catch( Exception exception ){
					System.err.println( "warning: LingoPreference.ElementHandler.startElement() caught " + exception );
					return ret = false;
				}finally{
					if( ! ret ) finished();
				}
				return ret;
			}

			public boolean   endElement( String uri, String localName, String qName ) throws SAXException{
				if( ! qName.equals( Presentation.STR_ELEMENT_PRESENTATION ) ) return finished();
				else return true;
			}

			private boolean finished(){
				LingoPreference.this.setValue( new Vocabulary( projection ) );
				return false;
			}

			private Map<Angle,Presentation> projection = new EnumMap<Angle,Presentation>( Angle.class );
		};
		return myElementHandler;
	}

	private Table                    myTable;
	private Map<Flag,AbstractButton> myButtonsImplicit, myButtonsRepresentative;
	private Map<AbstractButton,Flag> myButtons;
	private Map<Angle,ButtonGroup>   myGroupsImplicit;
	private Map<AbstractButton,Collection<Component>> myHidden;
	private     AbstractButton       myCurrentExpander;
	private ActionListener           myExpansionListener;
  //private Map<Angle,ZeroOrOne>     myGroupsRepresentative;
	private Integer[]                myOrdinals;
	private Map<Angle,JComboBox>     myCombos;
	private JComboBox                myComboPresets;
	private ElementHandler           myElementHandler;

	static  private Method           myMethodClearSelection;
	static  private boolean          myFlagMethodClearSelection = true;
}
