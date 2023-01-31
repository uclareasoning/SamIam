package edu.ucla.belief.ui.displayable;

import edu.ucla.belief.ui.NetworkInternalFrame;
import edu.ucla.belief.ui.actionsandmodes.Grepable;
import edu.ucla.belief.ui.actionsandmodes.Grepable.Filter;
import edu.ucla.belief.ui.actionsandmodes.Grepable.Simple;
import edu.ucla.belief.ui.util.*;
import static edu.ucla.belief.ui.util.Util.DEBUG_VERBOSE;
import edu.ucla.belief.ui.statusbar.StatusBar;

import edu.ucla.belief.*;
import edu.ucla.util.Stringifier;
import edu.ucla.belief.VariableComparator;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;

/** An enhanced version of DisplayableBeliefNetworkImpl that uses features of java 5, java 6 and beyond.
	@author keith cascio
	@since 20070326 */
public class DisplayableBeliefNetwork5 extends DisplayableBeliefNetworkImpl implements DisplayableBeliefNetwork, Grepable<VariableInstance,Simple,Filter>
{
	public DisplayableBeliefNetwork5( BeliefNetwork toDecorate, NetworkInternalFrame hnif ){
		super( toDecorate, hnif );
	}

	/** interface Grepable
		@since 20070326 */
	public Class<Simple> grepFields(){
		return Simple.class;
	}

	/** interface Grepable
		@since 20070326 */
	public EnumSet<Simple> grepFieldsDefault(){
		return Simple.SINGLETON;
	}

	/** interface Grepable
		@since 20070326 */
	public String grepInfo(){
		return "select variables, set evidence";
	}

	/** @since 20070402 */
	@SuppressWarnings( "unchecked" )
	public Collection<DisplayableFiniteVariable> asCheckedCollection(){
		return (Collection<DisplayableFiniteVariable>) this;
	}

	/** interface Grepable
		@since 20070326 */
	public long grep( Filter filter, EnumSet<Simple> field_selector, Stringifier stringifier, Filter vFilt, Collection<VariableInstance> results ){
		int                                     matches  = 0, selected = 0, deselected = 0, ambiguous = 0;
		Map<DisplayableFiniteVariable,Object[]> ambi     = null;
		Map<DisplayableFiniteVariable,Object>   evidence = null;
		Collection<DisplayableFiniteVariable>   accumul  = null;
		Thread                                  thread   = Thread.currentThread();
		try{
			boolean            destructive = filter.flags().contains( Flag.destructive );
			String             data        = null;
			LinkedList<Object> values      = (vFilt == null) ? null : new LinkedList<Object>();
			evidence = (vFilt == null) ? Collections.singletonMap( (DisplayableFiniteVariable)null, null ) : new HashMap<DisplayableFiniteVariable,Object>(   0x10 );
			ambi     = (vFilt == null) ? null : new HashMap<DisplayableFiniteVariable,Object[]>( 0x10 );
			accumul  = new LinkedList<DisplayableFiniteVariable>();

			for( DisplayableFiniteVariable dfv : this.asCheckedCollection() ){
				data  = (stringifier == null) ? dfv.toString() : stringifier.objectToString( dfv );

				if( thread.isInterrupted() ) break;
				if( filter.accept( data ) ){
					if( dfv.getNodeLabel().setSelected( true  ) ) ++selected;
					++matches;
					if( accumul != null ) accumul.add( dfv );
					if( vFilt   != null ){
						values.clear();
						if( dfv.grep( vFilt, values ) > 0 ){
							if( values.size() > 1 ){
								++ambiguous;
								ambi.put( dfv, values.toArray( new Object[ values.size() ] ) );
							}
							evidence.put( dfv, values.getFirst() );
						}
					}
				}
				else if( destructive      ){
					if( dfv.getNodeLabel().setSelected( false ) ) ++deselected;
				}
			}
		}catch( Exception exception ){
			System.err.println( "warning: DBN5.grep() caught " + exception );
			if( DEBUG_VERBOSE ) exception.printStackTrace();
		}

		try{
			if( results != null )
				for( DisplayableFiniteVariable dfv : accumul )
					results.add( new VariableInstance( dfv, evidence.get( dfv ) ) );
		}catch( Exception exception ){
			System.err.println( "warning: DBN5.grep() caught " + exception );
			if( DEBUG_VERBOSE ) exception.printStackTrace();
		}

		if( thread.isInterrupted() ) return matches;

		try{
			StringBuilder buff = new StringBuilder( 0x40 );
			buff.append( edu.ucla.belief.ui.preference.SamiamPreferences.STR_GREP_DISPLAY_NAME_LOWER );
			buff.append( " matched "     ).append( matches    );
			if( selected   > 0 ) buff.append( ", selected "   ).append( selected   );
			if( deselected > 0 ) buff.append( ", deselected " ).append( deselected );

			if( (vFilt != null) && (evidence != null) && (! evidence.isEmpty()) ){
				buff.append( ", observed " ).append( evidence.size() );
				EvidenceController ec    = DisplayableBeliefNetwork5.this.getEvidenceController();
				int                delta = vFilt.flags().contains( Flag.destructive ) ?
					ec.setObservations( evidence ) :
					ec   .observe(      evidence );
				buff.append( STR_STATUS_DELTA ).append( delta );
				if( ambiguous > 0 ) buff.append( ", " ).append( ambiguous ).append( " ambiguous" );
			}

			StatusBar sb = Util.getStatusBar( hnInternalFrame );
			if( sb != null ) sb.setText( buff.toString(), StatusBar.WEST );
		}catch( Exception exception ){
			System.err.println( "warning: DBN5.grep() caught " + exception );
			if( DEBUG_VERBOSE ) exception.printStackTrace();
		}

		if( thread.isInterrupted() ) return matches;

		try{
			if( (accumul != null) && (! accumul.isEmpty()) ) hnInternalFrame.getNetworkDisplay().ensureNodesVisible( accumul );
		}catch( Exception exception ){
			System.err.println( "warning: DBN5.grep() caught " + exception );
			if( DEBUG_VERBOSE ) exception.printStackTrace();
		}

		if( thread.isInterrupted() ) return matches;

		try{
			if( (ambi != null) && (! ambi.isEmpty()) ){
				final JComponent msg = ambiguityMessage( filter, vFilt, ambi);
				if( msg != null ){
				JOptionResizeHelper.JOptionResizeHelperListener listener = new JOptionResizeHelper.JOptionResizeHelperListener(){
					public void topLevelAncestorDialog( JDialog container, JOptionResizeHelper helper ){
						container.addComponentListener( new ResizeAdapter( msg ) );
					}
				};
				new JOptionResizeHelper( msg, true, 0x1000 ).start();
				JOptionPane.showMessageDialog( hnInternalFrame, msg, "warning: ambiguous expressions", JOptionPane.WARNING_MESSAGE );
				}
			}
		}catch( Exception exception ){
			System.err.println( "warning: DBN5.grep() caught " + exception );
			if( DEBUG_VERBOSE ) exception.printStackTrace();
		}
		return matches;
	}

	public static <T> JComponent ambiguityMessage( Filter filter, Filter vFilt, Map<T,Object[]> ambi ){
		StringBuilder buff = new StringBuilder( ambi.size() * 0x40 );

		JPanel             pnl = new JPanel( new GridBagLayout() );
		GridBagConstraints c   = new GridBagConstraints();
		JLabel             jtc = null;

		c.anchor    = GridBagConstraints.NORTHWEST;
		c.gridwidth = GridBagConstraints.REMAINDER;
		pnl.add( Box.createHorizontalStrut(0x100),    c );

		buff.append( "<html>Search patterns under-determine the evidence for <font color='#990000'>" ).append( ambi.size() ).append( "</font> matching variables. " );
		buff.append( "For each, the first matching value was observed." );
		c.weightx   = 1;
		c.fill      = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
		pnl.add( jtc = new JLabel( buff.toString() ), c );
		pnl.add( Box.createVerticalStrut(8),    c );

		Font bigbold = jtc.getFont().deriveFont( Font.BOLD ).deriveFont( (float)14 );

		try{
		c.weightx   = 0;
		c.fill      = GridBagConstraints.NONE;
		c.gridwidth = 1;
		pnl.add( jtc = new JLabel( "Variable "+filter.language().name+":" ), c );
		c.weightx   = 1;
		c.fill      = GridBagConstraints.HORIZONTAL;
		pnl.add( Box.createHorizontalStrut(8),    c );

		buff.setLength(0);
		filter.informal( buff );
		c.weightx   = 0;
		c.fill      = GridBagConstraints.NONE;
		pnl.add( jtc = new JLabel( buff.toString() ), c );
		jtc.setFont( bigbold );
		jtc.setForeground( Color.blue.darker() );
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx   = 1;
		c.fill      = GridBagConstraints.HORIZONTAL;
		pnl.add( Box.createHorizontalStrut(1),    c );
		}catch( Exception exception ){
			System.err.println( "warning: DBN5.ambiguityMessage() caught " + exception );
		}

		pnl.add( Box.createVerticalStrut(2),    c );

		try{
		c.weightx   = 0;
		c.fill      = GridBagConstraints.NONE;
		c.gridwidth = 1;
		pnl.add( jtc = new JLabel( "Value "+vFilt.language().name+":" ), c );
		c.weightx   = 1;
		c.fill      = GridBagConstraints.HORIZONTAL;
		pnl.add( Box.createHorizontalStrut(8),    c );

		buff.setLength(0);
		vFilt.informal( buff );
		c.weightx   = 0;
		c.fill      = GridBagConstraints.NONE;
		pnl.add( jtc = new JLabel( buff.toString() ), c );
		jtc.setFont( bigbold );
		jtc.setForeground( Color.blue.darker() );
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx   = 1;
		c.fill      = GridBagConstraints.HORIZONTAL;
		pnl.add( Box.createHorizontalStrut(1),    c );
		}catch( Exception exception ){
			System.err.println( "warning: DBN5.ambiguityMessage() caught " + exception );
		}

		pnl.add( Box.createVerticalStrut(8),    c );

		c.weightx   = 0;
		c.fill      = GridBagConstraints.NONE;
		pnl.add( jtc = new JLabel( "Matching, under-determined variables {" ), c );

		buff.setLength(0);
		Set<T>   keyset = ambi.keySet();
		Object[] list   = keyset.toArray( new Object[ keyset.size() ] );
		Arrays.sort( list, (Comparator<Object>) VariableComparator.getInstance() );
		for( Object dfv : list ) buff.append( dfv ).append( ' ' ).append( Arrays.toString( ambi.get( dfv ) ) ).append( '\n' );
		JTextArea ta = new JTextArea( buff.toString() );
		ta.setEditable( false );
		ta.setBackground( pnl.getBackground() );
		ta.setFont( jtc.getFont() );
		Dimension dimTA = ta.getPreferredSize();
		JScrollPane pain = new JScrollPane( ta );
		pain.setBorder( null );
		pain.setPreferredSize( new Dimension( 0x100, Math.min( dimTA.height + 0x10, 0x80 ) ) );
		c.weightx   = c.weighty = 1;
		c.fill      = GridBagConstraints.BOTH;
		pnl.add( pain, c );

		c.weightx   = c.weighty = 0;
		c.fill      = GridBagConstraints.NONE;
		pnl.add( jtc = new JLabel( "}" ), c );

		Dimension dim = pnl.getPreferredSize();
		dim.width  = Math.min( 0x180, dim.width  );
		dim.height = Math.min( 0x100, dim.height );
		pnl.setPreferredSize( dim );

		return pnl;
	}

  //public static final String STR_STATUS_DELTA = ", |delta e| = ";
	public static final String STR_STATUS_DELTA = ", |\u0394e| = ";//greek delta upper case
  //public static final String STR_STATUS_DELTA = ", |\u03B4e| = ";//greek delta lower case
}
