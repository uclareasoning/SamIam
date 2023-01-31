package edu.ucla.belief.ui.tree;

import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.VariableInstance;

import edu.ucla.belief.ui.actionsandmodes.*;
import edu.ucla.belief.ui.actionsandmodes.Grepable.Vocabulary;
import edu.ucla.belief.ui.actionsandmodes.Grepable.Presentation;
import edu.ucla.belief.ui.actionsandmodes.Grepable.Filter;
import edu.ucla.belief.ui.actionsandmodes.Grepable.Simple;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import java.lang.reflect.*;
import java.util.regex.*;

/** A grep action capable of not only selecting variables,
	but also setting evidence.  Takes 2 expressions as input.

	@author keith cascio
	@since  20070329 */
public class NetworkGrep<F extends VariableInstance> extends GrepAction<F,Simple,Filter>
{
	public NetworkGrep( Grepable<F,Simple,Filter> grepable ){
		super( grepable );
	}

	/** @since 20070515 */
	public Filter delegate( Filter filter, Vantage vantage ) throws Exception{
		Filter suggValue = null;
		if( myState != null ){
			if( (suggValue  = super.delegate( myState, vantage )) == CANCEL ) return CANCEL;
			if(  suggValue != null ) myState = suggValue;
		}

		Filter suggVaria = super.delegate( filter, vantage );
		if( suggVaria == CANCEL ) return CANCEL;
		if( suggVaria != null ) filter  = suggVaria;
		return ((suggValue == null) && (suggVaria == null)) ? null : filter;
	}

	public Filter snapshot(){
		if( (myDashEvidence == null) || (! myDashEvidence.isVisible()) ) return null;

		String exp = myDashEvidence.myTFPattern.getText();
		if( exp.length() < 1 ) return null;

		return filter( exp, myDashEvidence.memorize() ).blame( "evidence pattern" );
	}

	public JComponent getInputComponent(){
		final JComponent pnlSuper = super.getInputComponent();
		if( myPanelNetworkGrep == null ){
			SamiamAction actionE = new SamiamAction( "e", "<html>set <font color='#cc6600'>evidence</font>: for each matched variable, observe the <font color='#009900'>first value that matches</font> the second expression", 'e', null ){
				public void actionPerformed( ActionEvent evt ){
					try{
						AbstractButton btn = (AbstractButton) evt.getSource();
						boolean visible = btn.isSelected();
						myDashEvidence.setVisible( visible );
						if( visible ) myDashEvidence.myTFPattern.requestFocus();
					}catch( Exception exception ){
						System.err.println( "warning: NetworkGrep.action evidence.aP() caught " + exception );
					}
				}
			};

			myDashboard.extend( Box.createHorizontalStrut(2) );
			myDashboard.extend( Presentation.configure( new JToggleButton( actionE ) ) );

			myPanelNetworkGrep = new JPanel( new GridBagLayout() ){
				{ init(); }

				private void init(){
					GridBagConstraints c = new GridBagConstraints();

					c.weightx   = 1;
					c.anchor    = GridBagConstraints.WEST;
					c.gridwidth = GridBagConstraints.REMAINDER;
					c.fill      = GridBagConstraints.HORIZONTAL;
					this.add( pnlSuper, c );
					this.add( myDashEvidence = new Dashboard( "variable values (set evidence)" ), c );

					myDashEvidence.setBorder( BorderFactory.createEmptyBorder(2,0,0,0) );
					myDashEvidence.setVisible( false );
				}

				public void setEnabled( boolean flag ){
					super.setEnabled( flag );
					Component[] children = this.getComponents();
					for( int i=0; i<children.length; i++ ) children[i].setEnabled( flag );
				}
			};
		}
		return myPanelNetworkGrep;
	}

	/** @since 20070403 */
	public Vocabulary setVocabulary( Vocabulary vocab ){
		if( myDashEvidence != null ) myDashEvidence.setVocabulary( vocab );
		return super.setVocabulary( vocab );
	}

	/** @since 20070501 */
	public boolean setHighlight( boolean highlight ){
		try{
			if( myDashEvidence != null && myDashEvidence.myVocabulary != null ) myDashEvidence.myVocabulary.setHighlight( highlight );
		}catch( Exception exception ){
			System.err.println( "warning: NetworkGrep.setHighlight() caught " + exception );
		}
		return super.setHighlight( highlight );
	}

	private Dashboard  myDashEvidence;
	private JComponent myPanelNetworkGrep;
}
