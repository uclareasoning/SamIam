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

/** Code gleaned and removed from ScriptGeniusWizard

	@author Keith Cascio
	@since 033105 */
public class ResolutionPanel extends JPanel
{
	public ResolutionPanel(){
		super( new GridBagLayout() );
		this.myGridBagLayout = (GridBagLayout) this.getLayout();
	}

	public boolean isDelayLikely(){
		return (this.myMapDependenciesToResolvers == null);
	}

	public void setDependencies( Script script ){
		clearDependencies();

		SoftwareEntity[] deps = script.getDependencies();
		if( deps == null ) return;

		for( int i=0; i<deps.length; i++ ){
			addDependency( deps[i], script );
		}
	}

	public void addDependency( SoftwareEntity dep, Script script ){
		if( myMapDependenciesToResolvers == null ) myMapDependenciesToResolvers = new HashMap( INT_INIT_SIZE );
		if( myDependencies == null ) myDependencies = new HashSet( INT_INIT_SIZE );

		myDependencies.add( dep );

		Map map = myMapDependenciesToResolvers;
		Resolver resolver;
		if( map.containsKey( dep ) ) resolver = (Resolver) map.get( dep );
		else{
			resolver = new Resolver( dep, getBrowseHandler() );
			map.put( dep, resolver );
		}
		resolver.addMentioner( script );
	}

	public void clearDependencies(){
		if( myDependencies != null ) myDependencies.clear();
		if( myMapDependenciesToResolvers == null ) return;
		Resolver resolver;
		for( Iterator it = myMapDependenciesToResolvers.values().iterator(); it.hasNext(); ){
			((Resolver) it.next()).clearMentioners();
		}
	}

	public boolean validateDependencies() throws Exception {
		if( myDependencies == null ) return true;
		SoftwareEntity dep;
		Resolver resolver;
		for( Iterator it = myDependencies.iterator(); it.hasNext(); ){
			dep = (SoftwareEntity) it.next();
			resolver = getResolver( dep );
			if( !resolver.validateDependency() ) return false;
		}
		return true;
	}

	public void fill() throws Exception {
		this.removeAll();
		if( myDependencies == null ) return;

		if( myListDependencies == null ) myListDependencies = new ArrayList( myDependencies );
		else{
			myListDependencies.clear();
			myListDependencies.addAll( myDependencies );
		}
		Collections.sort( myListDependencies );

		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		SoftwareEntity dep;
		Resolver resolver;
		Component strut = null;
		for( Iterator it = myListDependencies.iterator(); it.hasNext(); ){
			dep = (SoftwareEntity) it.next();
			resolver = getResolver( dep );
			this.add( resolver, c );
			this.add( strut = Box.createVerticalStrut( 16 ), c );
			resolver.refresh();
		}
		if( strut != null ) this.remove( strut );

		Dimension sizeLayout = myGridBagLayout.preferredLayoutSize( (Container)this );
		sizeLayout.width = Math.max( sizeLayout.width, INT_WIDTH_MIN );
		this.setPreferredSize( sizeLayout );
	}

	public static final int INT_WIDTH_MIN = (int)512;

	private Resolver getResolver( SoftwareEntity dep ){
		if( myMapDependenciesToResolvers == null ) return (Resolver)null;
		else return (Resolver) myMapDependenciesToResolvers.get( dep );
	}

	/** @since 033105 */
	public Resolver.BrowseHandler getBrowseHandler(){
		if( myBrowseHandler == null ){
			myBrowseHandler = new Resolver.BrowseHandler(){
				public JFileChooser getChooser( SoftwareEntity entity, String path ){
					if( myChooserResolve == null ){
						JFileChooser chooser = myChooserResolve = new JFileChooser();
						//chooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
						chooser.setAcceptAllFileFilterUsed( true );

						//chooser.addChoosableFileFilter( getFilterDest() );
						//chooser.setFileFilter( getFilterDest() );

						//chooser.setAccessory();

						chooser.setApproveButtonText( "Select" );
						//chooser.setApproveButtonToolTipText( STR_TOOLTIP_PRE + descrip );
						chooser.setMultiSelectionEnabled( false );
					}
					myChooserResolve.setDialogTitle( "Browse for " + entity.getDescriptionShort() + ": " + entity.getDescriptionVerbose() );

					if( (path != null) && (path.length() > 0 ) ){
						File current = new File( path );
						if( current.exists() ){
							if( !current.isDirectory() ) current = current.getParentFile();
							myChooserResolve.setCurrentDirectory( current );
						}
					}

					return myChooserResolve;
				}

				public Component getParent(){
					return ResolutionPanel.this;
				}
			};
		}
		return myBrowseHandler;
	}

	public static final int INT_INIT_SIZE = (int)3;

	private ArrayList myListDependencies;
	private Set myDependencies;
	private Map myMapDependenciesToResolvers;
	private JFileChooser myChooserResolve;
	private Resolver.BrowseHandler myBrowseHandler;
	private GridBagLayout myGridBagLayout;
}
