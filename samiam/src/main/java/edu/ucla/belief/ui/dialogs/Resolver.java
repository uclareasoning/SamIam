package edu.ucla.belief.ui.dialogs;

import edu.ucla.belief.ui.util.*;

import edu.ucla.util.code.*;

import java.util.List;
import java.util.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.io.*;

/** Used to be inner class of ScriptGeniusWizard

	@author Keith Cascio
	@since 033105 */
public class Resolver extends JPanel implements ActionListener
{
	public Resolver( SoftwareEntity dep, BrowseHandler handler ){
		super( new GridBagLayout() );
		this.myDependency = dep;
		this.myBrowseHandler = handler;
		init();
	}

	private void init(){
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;

		c.gridwidth = GridBagConstraints.REMAINDER;

		this.add( new JLabel( "<html><nobr><b>" + myDependency.getDescriptionShort() + "</b>: " + myDependency.getDescriptionVerbose() ), c );

		c.gridwidth = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		this.add( makeScriptAndGuessComponent(), c );
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.NONE;
		this.add( Box.createHorizontalStrut(1), c );

		c.gridwidth = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		this.add( myNotifyField = new NotifyField( "", 10 ), c );
		myNotifyField.addActionListener( (ActionListener)this );
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;

		//this.add( myLabelGuess = new JLabel(), c );

		this.add( myButtonGuess = new JButton( "Guess" ), c );
		myButtonGuess.addActionListener( (ActionListener)this );

		c.gridwidth = GridBagConstraints.REMAINDER;
		this.add( myButtonBrowse = new JButton( "Browse" ), c );
		myButtonBrowse.addActionListener( (ActionListener)this );
	}

	private JComponent makeScriptAndGuessComponent(){
		if( myLabelScripts != null ) return (JComponent) null;

		JPanel ret = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.gridwidth = 1;
		c.weightx = 0;
		ret.add( myLabelScripts = new JLabel(), c );
		c.weightx = 1;
		ret.add( Box.createHorizontalStrut(1), c );
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 0;
		ret.add( myLabelGuess = new JLabel(), c );

		return ret;
	}

	public void actionPerformed( ActionEvent e ){
		Object src = e.getSource();
		if( (src == myNotifyField) && myFlagListen ) doEdited();
		else if( src == myButtonBrowse ) doBrowse();
		else if( src == myButtonGuess ) doGuess( true );
	}

	private void doEdited(){
		myDependency.setPath( (File)null );
		myLabelGuess.setText( "" );
	}

	public void doBrowse(){
		JFileChooser chooser = myBrowseHandler.getChooser( myDependency, myNotifyField.getText() );//getChooserResolve( myDependency, myNotifyField.getText() );
		chooser.showOpenDialog( myBrowseHandler.getParent() );
		File file = chooser.getSelectedFile();
		if( (file != null) && (file.exists()) ){
			if( myNotifyField != null ) myNotifyField.setText( file.getAbsolutePath() );
			myDependency.setPath( file );
		}
	}

	public void doGuess( boolean force ){
		if( force ) myDependency.setPath( (File)null );
		try{
			myDependency.guessLocationIfNecessary();
		}catch( Exception e ){
			System.err.println( "Warning: " + e );
		}
		File path = myDependency.getPath();
		String textpath = "", textguess = "";
		if( path != null ) textpath = path.getAbsolutePath();
		if( myDependency.isGuess() && (path != null) ) textguess = "<html><nobr><font color=\"#009900\">(location guessed)";
		myLabelGuess.setText( textguess );

		myFlagListen = false;
		myNotifyField.setText( textpath );
		myFlagListen = true;
	}

	public boolean validateDependency() throws Exception{
		File path = myDependency.getPath();
		if( path != null ){
			if( path.exists() ) return true;
			else throw new Exception( path.getAbsolutePath() + " dne" );
		}
		else{
			String text = myNotifyField.getText();
			if( text.trim().length() < 1 ) throw new Exception( "path to \"" + myDependency.getDescriptionShort() + "\" required" );
			path = new File( text );
			if( path.exists() ){
				myDependency.setPath( path );
				return true;
			}
			else throw new Exception( path.getAbsolutePath() + " does not exist" );
		}
	}

	public void addMentioner( Script script ){
		if( myScripts == null ) myScripts = new LinkedList();
		else if( myScripts.contains( script ) ) return;
		myScripts.add( script );
	}

	public void clearMentioners(){
		if( myScripts == null ) return;
		myScripts.clear();
	}

	public void refresh(){
		doGuess( false );
		myLabelScripts.setText( "required by: " + myScripts.toString() );
	}

	public interface BrowseHandler{
		public JFileChooser getChooser( SoftwareEntity entity, String path );
		public Component getParent();
	}

	private SoftwareEntity myDependency;
	private List myScripts;
	private JLabel myLabelScripts, myLabelGuess;
	private NotifyField myNotifyField;
	private boolean myFlagListen = true;
	private JButton myButtonBrowse, myButtonGuess;
	private BrowseHandler myBrowseHandler;
}