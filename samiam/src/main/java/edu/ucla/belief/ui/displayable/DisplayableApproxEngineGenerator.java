package edu.ucla.belief.ui.displayable;

import        edu.ucla.util. Setting;
import        edu.ucla.util. Setting.Settings;
import        edu.ucla.util. SettingsImpl;
import        edu.ucla.util. PropertyKey;
import        edu.ucla.util. WeakLinkedList;
import        edu.ucla.belief.*;
import        edu.ucla.belief.inference. SynchronizedInferenceEngine;
import        edu.ucla.belief.approx.*;
import        edu.ucla.belief.io. PropertySuperintendent;

import        edu.ucla.belief.ui. UI;
import        edu.ucla.belief.ui. NetworkInternalFrame;
import        edu.ucla.belief.ui.preference. SamiamPreferences;
import        edu.ucla.belief.ui.preference. PreferenceListener;
import static edu.ucla.belief.ui.util. Util. htmlEncode;

import        java.util.*;
import        javax.swing.*;
import        java.awt.*;

/** @author keith cascio
	@since  20091208 */
public abstract class DisplayableApproxEngineGenerator<E extends Enum<E> & Setting> extends Dynamator
  implements
    PreferenceListener
{
	abstract public     String          getTitle();

	public DisplayableApproxEngineGenerator( ApproxEngineGenerator<E> peg, UI ui ){
		this.myApproxEngineGenerator   = peg;
		this.myUI                      = ui;
		if( ui != null ){ ui.addPreferenceListener( this ); }
	}

	public boolean probabilitySupported(){
		return myApproxEngineGenerator.probabilitySupported();
	}

	public String getDisplayName(){
		return myApproxEngineGenerator.getDisplayName();
	}

	public Object getKey(){
		return myApproxEngineGenerator.getKey();
	}

	public InferenceEngine manufactureInferenceEngineOrDie( BeliefNetwork bn, Dynamator dyn ) throws Throwable{
		if( bn instanceof PropertySuperintendent ){ multiplex( (PropertySuperintendent) bn ); }
		InferenceEngine ie = SynchronizedInferenceEngine.decorate( myApproxEngineGenerator.manufactureInferenceEngineOrDie( bn, dyn ) );
		return ie;
	}

	/** @since 20080229 */
	@SuppressWarnings( "unchecked" )
	static public Comparator<FiniteVariable> comparator(){
		return VariableComparator.getInstance();
	}

	public boolean isEditable(){ return true; }

	public Commitable getEditComponent( Container cont ){
		NetworkInternalFrame nif = myUI == null ? null : myUI.getActiveHuginNetInternalFrame();
		if( (nif != null) && (nif.getBeliefNetwork() != null) ){ multiplex( nif.getBeliefNetwork() ); }
		return validateSettingsPanel().setNetworkInternalFrame( nif, myApproxEngineGenerator );
	}

	public void commitEditComponent(){
		if( mySettingsPanel != null ){ mySettingsPanel.commitChanges(); }
	}

	protected SettingsPanel<E> validateSettingsPanel(){
		if( mySettingsPanel == null ){
			mySettingsPanel  = new SettingsPanel<E>( myApproxEngineGenerator.clazz(), getTitle() );
		}
		return mySettingsPanel;
	}

	public JMenu getJMenu(){
		return null;
	}

	public boolean equals( Object obj ){
		return obj == myApproxEngineGenerator || obj == this;
	}

	public void killState( PropertySuperintendent bn ){
		myApproxEngineGenerator.killState(  bn );
	}

	public Object retrieveState( PropertySuperintendent bn ){
		multiplex( bn );
		return myApproxEngineGenerator.retrieveState( bn );
	}

	@SuppressWarnings( "unchecked" )
	public int refresh( PropertySuperintendent bn ){
		int count = 0;
		Object ret  = myApproxEngineGenerator.retrieveState( bn );
		if(    ret != null ){
			try{
				Settings<E> settings = (Settings<E>) ret;
				E           key      = Enum.valueOf( settings.clazz(), "compare2exact" );
				String      strInfo  = null;
				if(        (key     != null) && (((strInfo = (String) key.get( PropertyKey.info )) == null) || (strInfo.indexOf( "approx over" ) >= 0)) ){
					settings.put( key, PropertyKey.info, htmlTipCompare2Exact() );
					++count;
				}
			}catch( Throwable thrown ){
				System.err.println( "warning: DisplayableApproxEngineGenerator.refresh() caught " + thrown );
			}
		}
		return count;
	}

	/** @since 20091228 Linus Torvalds 40th birthday! http://en.wikipedia.org/wiki/Linus_Torvalds */
	public String htmlTipCompare2Exact(){
		SamiamPreferences prefs = myUI.getSamiamPreferences();
		Color evidDlgAutoClr  = (Color) prefs.getMappedPreference( prefs.evidDlgAutoClr  ).getValue();
		Color evidDlgAutoClr2 = (Color) prefs.getMappedPreference( prefs.evidDlgAutoClr2 ).getValue();
		StringBuilder buff = new StringBuilder( "<html>" );
		buff.append( "<font color='#" )
		    .append( htmlEncode( evidDlgAutoClr  ) )
		    .append( "'>approx over</font>/<font color='#" )
		    .append( htmlEncode( evidDlgAutoClr2 ) )
		    .append( "'>exact under</font>" );
		return buff.toString();
	}

	/** @since 20091228 Linus Torvalds 40th birthday! http://en.wikipedia.org/wiki/Linus_Torvalds */
	public boolean multiplex( PropertySuperintendent bn ){
		refresh( bn );
		if( myMultiplexed == null ){ myMultiplexed = new WeakLinkedList(); }
		if( ! myMultiplexed.contains( bn ) ){
			return myMultiplexed.add( bn );
		}
		return false;
	}

	/** interface PreferenceListener
		@since 20091228 Linus Torvalds 40th birthday! http://en.wikipedia.org/wiki/Linus_Torvalds */
	public void     setPreferences(){
		if( myMultiplexed == null ){ return; }
		PropertySuperintendent bn = null;
		for( Iterator it = myMultiplexed.iterator(); it.hasNext(); ){
			if( (bn = (PropertySuperintendent) it.next()) == null ){ it.remove(); }
			refresh( bn );
		}
	}
	public void  updatePreferences(){}
	public void previewPreferences(){}

	public Collection<Class> getClassDependencies(){ return myApproxEngineGenerator.getClassDependencies(); }

	public Dynamator getCanonicalDynamator(){ return myApproxEngineGenerator.getCanonicalDynamator(); }

	protected              SettingsPanel<E>    mySettingsPanel;
	protected      ApproxEngineGenerator<E>    myApproxEngineGenerator;
	protected      UI                          myUI;
	protected      WeakLinkedList              myMultiplexed;
}
