package edu.ucla.belief.ui.util;

import        edu.ucla.belief.ui.*;
import        edu.ucla.belief.ui.networkdisplay.*;
import        edu.ucla.belief.ui.displayable.*;
import        edu.ucla.belief.ui.preference.*;
import        edu.ucla.belief.ui.tree.*;
import        edu.ucla.belief.ui.actionsandmodes.*;
import        edu.ucla.belief.ui.dialogs.*;
import        edu.ucla.belief.ui.internalframes.*;
import        edu.ucla.belief.ui.toolbar.MainToolBar;

import        edu.ucla.belief.ui.actionsandmodes.Grepable.Redirect;
import        edu.ucla.belief.ui.actionsandmodes.Grepable.Redirect.Context;
import        edu.ucla.belief.ui.actionsandmodes.Grepable.Redirect.Dest;
import        edu.ucla.belief.ui.util.JOptionResizeHelper.JOptionResizeHelperListener;

import static edu.ucla.belief.ui.dialogs.ProbabilityRewrite.getMethodJDialogSetIconImage;

import        edu.ucla.belief.ui.dialogs.EnumModels;
import static edu.ucla.belief.ui.dialogs.EnumModels.firstUniqueKeyStroke;
import        edu.ucla.belief.ui.dialogs.EnumModels.Actionable;
import        edu.ucla.belief.ui.dialogs.EnumModels.Actionable.Property;
import static edu.ucla.belief.ui.dialogs.EnumModels.Actionable.Property.*;
import        edu.ucla.belief.ui.dialogs.EnumModels.Semantics;
import static edu.ucla.belief.ui.dialogs.EnumModels.Semantics.*;
import        edu.ucla.belief.ui.dialogs.EnumModels.Semantics.Semantic;
import        edu.ucla.belief.ui.dialogs.EnumModels.View;
import static edu.ucla.belief.ui.dialogs.EnumModels.View.*;

import        edu.ucla.belief.*;
import        edu.ucla.belief.sensitivity.*;
import        edu.ucla.belief.inference.RCSettings;
import        edu.ucla.util.code.*;

import        javax.swing.filechooser.FileFilter;
import        java.awt.*;
import        java.awt.event.*;
import        javax.imageio.*;
import        java.awt.image.BufferedImage;
import        java.awt.image.*;
import        javax.swing.*;
import        javax.swing.event.*;
import        javax.swing.plaf.basic.*;
import        javax.swing.tree.*;
import        javax.swing.border.*;
import        javax.swing.table.*;
import static javax.swing.SwingUtilities.convertPointToScreen;
import static javax.swing.SwingUtilities.convertRectangle;
import static javax.swing.SwingUtilities.getWindowAncestor;
import static javax.swing.SwingUtilities.invokeLater;
import        java.io.*;
import        java.util.*;
import        java.text.*;

/** This class, as originally conceived,
	will run through a single script that
	sets up and captures all the screenshots
	we bundle with the html help.

	@since  20050211
	@author keith cascio */
public class ScreenCaptureAutomation
{
	public   static  final  String
	  STR_NAME_DIRECTORY_OUTPUT_PRE  =  "shots",
	  STR_NAME_DIRECTORY_NETWORKS    =  "networks",
	  STR_PATH_CANCER                =  STR_NAME_DIRECTORY_NETWORKS + "/cancer.net",
	  ENDL                           =  "\n",
	  STR_CHANNEL_BARS               =  "bars",
	  STR_VAR_NEW_DOC                =  "doc",
	  STR_ARG_SELECT                 =  "+",
	  STR_ARG_PAUSE                  =  "-pause",
	  STR_ARG_DELAY                  =  "-delay",
	  STR_ARG_NOEXIT                 =  "-noexit",
	  STR_ARG_NOTHUMBS               =  "-nothumbs",
	  STR_ARG_POINTER                =  "-pointer",
	  STR_ARG_FORMATS                =  "-formats";
	private  static         boolean
	  FLAG_EXIT                      =  true,
	  FLAG_THUMBS                    =  true,
	  FLAG_FORMATS                   =  false;
	private  static         String
	  STR_DELAY                      =  null,
	  STR_PATH_POINTER               =  "resources/pointer33x57samiam.gif";
	public   static  final  PrintStream
	  STREAM_VERBOSE                 =  System.out;

	public static void main( String[] args )
	{
		Set<String> selected = processArgs( args );
		if( FLAG_FORMATS ){
			ScreenCaptureAutomation.printImageIOFormats( System.out );
			return;
		}

		UI ui = UI.mainImpl( args );
		ScreenCaptureAutomation auto = new ScreenCaptureAutomation( ui, selected );
		auto.runAllSafe();
		if( FLAG_EXIT ) ui.exitProgram();
	}

	public ScreenCaptureAutomation( UI ui, Set<String> selected ){
		this.myUI          = ui;
		this.mySetSelected = selected;
		init();
	}

	/** @since 20080122 */
	public enum Section{
		whole   ( "entire thing" ),
		titlebar( "title bar"    ){
			public Rectangle[]    bounds( Component comp, Rectangle rect ){
			  /*if(        comp instanceof JInternalFrame ){
					JInternalFrame jcomp = (JInternalFrame) comp;
					javax.swing.plaf.InternalFrameUI ifui = jcomp.getUI();
					Dimension size = null;
					if( ifui instanceof javax.swing.plaf.basic.BasicInternalFrameUI ){
						size = ((javax.swing.plaf.basic.BasicInternalFrameUI)ifui).getNorthPane().getSize();
					}
					if( size == null ){ size = new Dimension( comp.getSize().width, 0x40 ); }
					rect.setLocation( comp.getLocation() );
					rect.setSize(     size );
					return new Rectangle[]{ rect };
				}*//*
				if(        comp instanceof RootPaneContainer ){
					JRootPane  root    =  ((RootPaneContainer) comp).getRootPane();
					Insets     insets  =  ((        Container) comp).getInsets();
					Dimension  size    =  comp.getSize();
					size.height       -=  root.getSize().height - insets.top - insets.bottom;
					rect.setLocation( 0,0 );
					rect.setSize(     size );
					return new Rectangle[]{ rect };
				}*/
				if(        comp instanceof RootPaneContainer ){
					JRootPane  root    =  ((RootPaneContainer) comp).getRootPane();
					Dimension  size    =  comp.getSize();
					size.height        =  root.getLocation().y;
				  //if( comp instanceof Window ){ rect.setLocation( 0,0 ); }
				  //else{                         rect.setLocation( comp.getLocation() ); }
					rect.setLocation( comp.getLocation() );
					rect.setSize(     size );
					return new Rectangle[]{ clip( comp, rect ) };
				}
				if(        comp instanceof Container ){
					Insets    insets = ((Container)comp).getInsets();
					if( insets != null ){
						Dimension size   = comp.getSize();
						size.height      = insets.top;
					  //if( comp instanceof Window ){ rect.setLocation( 0,0 ); }
					  //else{                         rect.setLocation( comp.getLocation() ); }
						rect.setLocation( comp.getLocation() );
						rect.setSize(     size );
						return new Rectangle[]{ clip( comp, rect ) };
					}
				}

				throw new IllegalArgumentException( "don't know how to calculate the title bar size for " + comp.getClass().getSimpleName() );
			}
		},
		insets( "inset border minus title bar" ){
			public Rectangle[]    bounds( Component comp, Rectangle rect ){
				if( ! (comp instanceof Container) ){ return null; }
				Container cont   = (Container) comp;

				Insets    insets = null;
				if( comp instanceof RootPaneContainer ){
					JRootPane  root    =  ((RootPaneContainer) comp).getRootPane();
					Point     lroot    =    root.getLocation();
					Dimension zroot    =    root.getSize();
					Dimension zcomp    =    comp.getSize();
					insets = new Insets( lroot.y, lroot.x, zcomp.height - zroot.height - lroot.y, zcomp.width - zroot.width - lroot.x );
				}
				else{ insets = cont.getInsets(); }

				comp.getBounds( rect );
				Rectangle left   = new Rectangle( rect.x,                             rect.y + insets.top,                  insets.left,  rect.height   );
				Rectangle right  = new Rectangle( rect.x + rect.width - insets.right, rect.y + insets.top,                  insets.right, rect.height   );
				Rectangle bottom = new Rectangle( rect.x,                             rect.y + rect.height - insets.bottom, rect.width,   insets.bottom );
				Rectangle top    = new Rectangle( rect.x,                             rect.y,                               rect.width,   insets.top    );

				clip( comp, left   );
				clip( comp, right  );
				clip( comp, bottom );
				clip( comp, top    );

				return comp instanceof RootPaneContainer ?
					new Rectangle[]{ left, right, bottom } :
					new Rectangle[]{ left, right, bottom, top };
			}
		};

		public         Rectangle[] bounds( Component comp,       Rectangle rect ){
			return new Rectangle[]{            clip( comp, comp.getBounds( rect ) ) };
		}

		public  Rectangle clip( Component comp, Rectangle rect ){
			if( comp instanceof JComponent ){
				Rectangle vr = ((JComponent)comp).getVisibleRect();
				rect.x      += vr.x;
				rect.y      += vr.y;
				rect.width   = Math.min( rect.width,  vr.width  );
				rect.height  = Math.min( rect.height, vr.height );
			}
			return rect;
		}

		private Section( String description ){ this.description = description; }

		public final String description;
	}

	/** @since 20080122 */
	public enum Quadrant implements Actionable<Quadrant>, Semantic, Choice<Quadrant>{
		titlebar         ( UI.STR_SAMIAM_ACRONYM,    Section.titlebar ){ public Component component( UI ui ){ return ui; } },
		insets           ( UI.STR_SAMIAM_ACRONYM,    Section.insets   ){ public Component component( UI ui ){ return ui; } },
		menubar          ( UI.STR_SAMIAM_ACRONYM + " menu bar"        ){ public Component component( UI ui ){ return ui.getJMenuBar(); } },
		toolbar          ( UI.STR_SAMIAM_ACRONYM + " toolbar"         ){ public Component component( UI ui ){ return ui.getMainToolBar(); } },
		icbtoolbar       ( UI.STR_SAMIAM_ACRONYM + " inst toolbar"    ){ public Component component( UI ui ){ return ui.getInstantiationToolBar(); } },
		statusbar        ( UI.STR_SAMIAM_ACRONYM + " status bar"      ){ public Component component( UI ui ){ return ui.getStatusBar(); } },
		activeniftitlebar( "network window",         Section.titlebar ){ public Component component( UI ui ){
				NetworkInternalFrame nif = ui.getActiveHuginNetInternalFrame();
				return nif == null ? null : nif;
			}
		},
		activendtitlebar ( "network picture window", Section.titlebar ){ public Component component( UI ui ){
				NetworkInternalFrame nif = ui.getActiveHuginNetInternalFrame();
				return nif == null ? null : nif.getNetworkDisplay();
			}
		};

		private Quadrant( String widgettext ){ this( widgettext, Section.whole ); }
		private Quadrant( String widgettext, Section section ){
			this.section = section;

			String text = widgettext + ", " + section.description;
			this.properties.put(     display, text );
			this.properties.put(     tooltip, text );
			this.properties.put( accelerator, firstUniqueKeyStroke( text ) );
		}

		public Component component( UI ui ){ return null; }

		public Rectangle[]  bounds( UI ui, Rectangle rect ){
		  //System.out.println( name() + ".bounds:" );
			if( ui == null ){ return null; }
			Component comp = component( ui );
			if( (comp == null) || (! comp.isVisible()) ){ return null; }
			Rectangle[] ret = section.bounds( comp, rect == null ? (rect = new Rectangle()) : rect );

		  /*int    r  = 10, w = 5;
			char   l  = ' ';
			for( Rectangle re : ret ){
				String sx = ScriptLanguage.s( re.x     , r, w, l ),
					   sy = ScriptLanguage.s( re.y     , r, w, l ),
					   sw = ScriptLanguage.s( re.width , r, w, l ),
					   sh = ScriptLanguage.s( re.height, r, w, l );
				System.out.println(   "    " + sx + sy + sw + sh );
			}*/

			return ret;
		}

		public final Section section;

		public EnumModels<Quadrant>     models(){ return MODELS; }

		public String            describeAngle(){ return "quadrants"; }

		public Semantics semantics(){ return    additive; }

		public Quadrant getDefault(){ return    titlebar; }

		public Object   get( Property property ){ return this.properties.get( property ); }

		private Map<Property,Object> properties = new EnumMap<Property,Object>( Property.class );

		public static AbstractButton[]   buttons( View view, Object id, ItemListener listener ){
			return MODELS.newButtons( view, id, listener );
		}

		public static Set<Quadrant>     selected( Object id, Set<Quadrant> results ){
			return MODELS.selected(  additive, id, results);
		}

		public static EnumModels<Quadrant> MODELS = new EnumModels<Quadrant>( Quadrant.class );
	}

	/** @since 20080122 */
	public static class Mask{
		public Mask( Quadrant quadrant, Rectangle[] rectangles, Component source, Window destination ){
			this.quadrant         = quadrant;
			this.rectangles       = rectangles;

		  //if( source == destination ){ return; }

			Point       upperleft = destination.getLocation();

			Point     point;
			Component parent;
			for( Rectangle rect : rectangles ){
				parent            = source.getParent();
				point             = rect.getLocation();
				if( parent != null ){ convertPointToScreen( point, parent ); }
				point.x          -= upperleft.x;
				point.y          -= upperleft.y;
				rect.setLocation( point );
			}
		}

	  /*public Mask( Quadrant quadrant, Rectangle[] rectangles, Component source, Window destination ){
			boolean debug = true;//quadrant == Quadrant.activeniftitlebar;
			this.quadrant         = quadrant;
			this.rectangles       = rectangles;

		  //if( source == destination ){ return; }

			Point       upperleft = destination.getLocation();
			if( debug ) System.out.println( "upperleft?          " + upperleft );

			Point     point;
			Component parent;
			for( Rectangle rect : rectangles ){
				parent = source.getParent();
				if( debug && parent != null ) System.out.println( "parent? " + parent.getClass().getName() + ", (" + parent.getName() + ")" );
				if( debug ) System.out.println( "rect.getLocation()? " + rect.getLocation() );
				point = rect.getLocation();
				if( parent != null ){ convertPointToScreen( point, parent ); }
				if( debug ) System.out.println( "converted?          " + point );
				point.x          -= upperleft.x;
				point.y          -= upperleft.y;
				if( debug ) System.out.println( "adjusted?           " + point );
				rect.setLocation( point );
			}
		}*/

	  /*public <T extends Window & RootPaneContainer> Mask( Quadrant quadrant, Rectangle[] rectangles, Component source, T destination ){
			this.quadrant         = quadrant;
			this.rectangles       = rectangles;

			if( source == destination ){ return; }

			int        adjx       = 0, adjy     = 0;

			JRootPane    rootpane = destination.getRootPane();
			JLayeredPane layeredp = destination.getLayeredPane();
			Container    contentp = destination.getContentPane();

//			Dimension   zrootpane = rootpane.getSize();
//			Dimension   zcontentp = contentp.getSize();
			Point       lcontentp = contentp.getLocation();
			if(          contentp.isAncestorOf( source ) ){
				adjx             += lcontentp.x;
				adjy             += lcontentp.y;
			}

//			Insets     insets     = destination.getInsets();
//			if( insets != null ){
//				adjx             += insets.left;
//				adjy             += insets.top;
//			}

			Point       lrootpane = rootpane.getLocation();
			if(          rootpane.isAncestorOf( source ) ){
				adjx             += lrootpane.x;
				adjy             += lrootpane.y;
			}

			Container  parent;
			for( Rectangle rect : rectangles ){
				parent = source.getParent();
				if( parent != null ){ convertRectangle( parent, rect, layeredp ); }
				rect.x += adjx;
				rect.y += adjy;
			}
		}*/

		public Quadrant    quadrant;
		public Rectangle[] rectangles;
	}

	/** @since 20080122 */
	public static Map<Quadrant,Mask> silhouette( UI ui, Quadrant ... quads ){
		Map<Quadrant,Mask> rectangles = new EnumMap<Quadrant,Mask>( Quadrant.class );

		Component   comp;
		Rectangle   rect;
		Rectangle[] rects;
		for( Quadrant quad : quads ){
			comp = quad.component( ui );
			if( (comp == null) || (! comp.isVisible()) ){ continue; }
			rects = quad.bounds( ui, rect = new Rectangle() );
			if( rects != null ){
				rectangles.put( quad, new Mask( quad, rects, comp, ui ) );
			}
		}

		return rectangles;
	}

	/** @since 20080122 */
	public enum ScriptLanguage implements Actionable<ScriptLanguage>, Semantic, Choice<ScriptLanguage>{
		adobejavascript ( "Adobe Javascript", "jsx", "//", "Photoshop16.gif", "Photoshop48.png", "http://www.adobe.com/products/photoshop" ){
			public Dest           begin( String scriptname,                 Dest app ) throws Exception{
				return app.nl()
				.app( "function main(){" ).nl();
			}

			public Dest          finish( String scriptname, String[] descr, Dest app ) throws Exception{
				return app
				.app( "}" ).nl().nl().app( "main();" ).nl();
			}

			public Dest newDocument( Dimension size, Dest app ) throws Exception{
				app.nl()
				.app( "var " )
				.app( STR_VAR_NEW_DOC )
				.app( " = documents.add( " ).app( "new UnitValue( " )
				.app( s( size.width ) ).app( ", \"px\" ), new UnitValue( " ).app( s( size.height ) )
				.app( ", \"px\" ), 72, \"" );
				docname( size, app )
				.app( "\", NewDocumentMode.RGB );" );
				return app;
			}

			public Dest modifySelection( Rectangle rect, SelectionType st, Dest app ) throws Exception{
				int    r  = 10, w = 4;
				char   l  = ' ';
				String sx1 = s( rect.x              , r, w, l ),
				       sy1 = s( rect.y              , r, w, l ),
				       sx2 = s( rect.x + rect.width , r, w, l ),
				       sy2 = s( rect.y + rect.height, r, w, l );
				app
				.app( STR_VAR_NEW_DOC ).app( ".selection.select( Array( " )
				.app( "Array( " ).app( sx1 ).app( ", " ).app( sy1 ).app( " ), " )
				.app( "Array( " ).app( sx2 ).app( ", " ).app( sy1 ).app( " ), " )
				.app( "Array( " ).app( sx2 ).app( ", " ).app( sy2 ).app( " ), " )
				.app( "Array( " ).app( sx1 ).app( ", " ).app( sy2 ).app( " ), " )
				.app( "Array( " ).app( sx1 ).app( ", " ).app( sy1 ).app( " ) ), " )
				.app( encode( st ) ).app( " );" );
				return app;
			}

			public Dest   saveSelection(    String name, Dest app ) throws Exception{
				app
				.app( "var            channel1 = " ).app( STR_VAR_NEW_DOC ).app( ".channels.add();" ).nl()
				.app( "channel1          .name = \"" ).app( name ).app( "\";" ).nl()
				.app( STR_VAR_NEW_DOC ).app( ".selection.store( channel1 );" ).nl()
				.app( STR_VAR_NEW_DOC ).app( ".channels[0].visible = true;" ).nl()
				.app( STR_VAR_NEW_DOC ).app( ".channels[1].visible = true;" ).nl()
				.app( STR_VAR_NEW_DOC ).app( ".channels[2].visible = true;" ).nl()
			  //.app( STR_VAR_NEW_DOC ).app( ".channels.getByName( \"RGB\" ).visible = true;" ).nl()
				.app( "channel1       .visible = false;" ).nl();
				return app;
			}

			public Dest assertOpenDocument( Dest app ) throws Exception{
				return app
				.app( "if( documents.length == 0 ){ alert( \"Please open an image document before running this script.\" ); return; }" ).nl();
			}

			public Dest assertChannelExists( String name, Dest app ) throws Exception{
				return app
				.app( "try{ activeDocument.channels.getByName( \"" )
				.app( name ).app( "\" ); }catch(e){ alert( \"Please save a channel named '" )
				.app( name ).app( "' before running this script.\" ); return; }" ).nl();
			}

			public Dest   saveLayerName( String variablename, Dest app ) throws Exception{
				return app
				.app( "var " ).app( variablename ).app( " = activeDocument.activeLayer.name;" ).nl();
			}

			public Dest    defineString( String variablename, String value, Dest app ) throws Exception{
				return app
				.app( "var " ).app( variablename ).app( " = \"" )
				.app( value.replace( "\"", "\\\"" ) ).app( "\";" ).nl();
			}

			public Dest     concatenate( String lhs, String rhs1, String rhs2, Dest app ) throws Exception{
				return app
				.app( "var " ).app( lhs ).app( " = " )
				.app( rhs1 ).app( " + " ).app( rhs2 ).app( ";" ).nl();
			}

			public Dest       duplicate( Dest app ) throws Exception{
				return app.
				app( "activeDocument.activeLayer = activeDocument.activeLayer.duplicate();" ).nl();
			}

			public Dest      selectNone( Dest app ) throws Exception{
				return app.
				app( "activeDocument.selection.deselect();" ).nl();
			}

			public Dest selectionFromChannel( String name, SelectionType st, Dest app ) throws Exception{
				return app.
				app( "activeDocument.selection.load( activeDocument.channels.getByName( \"" )
				.app( name ).app( "\" ), " ).app( encode( st ) ).app( " );" ).nl();
			}

			public Dest brightnessContrast( float brightness, float contrast, Dest app ) throws Exception{
				return app
				.app( "activeDocument.activeLayer.adjustBrightnessContrast( " )
				.app( s( (int) Math.floor( brightness * 100f ) ) ).app( ", " )
				.app( s( (int) Math.floor(   contrast * 100f ) ) ).app( " )" ).nl();
			}

			public Dest      setLayerName( String variablename, String value, Dest app ) throws Exception{
				app
				.app( "activeDocument.activeLayer.name " );
				if( variablename != null ){
					app.app( " = " ).app( variablename );
				}else if(  value != null ){
					app.app( " = \"" ).app( value.replace( "\"", "\\\"" ) ).app( "\"" );
				}
				return app.app( ";" ).nl();
			}
		},
		gimpscriptfu    ( "GIMP Script-Fu", "scm", ";", "Gimp16.gif", "Gimp48.png", "http://docs.gimp.org/en/gimp-concepts-script-fu.html"  ){
			public Dest  openLocalScope(                 Dest app, String ... locals ) throws Exception{
				app
				.app( "(let* (" );
				for( String vlocal : locals ){
					app.app( " (" ).app( vlocal ).app( ")" );
				}
				return app
				.app( " )" ).nl();
			}

			public Dest closeLocalScope(                                    Dest app ) throws Exception{
				return app.app( ")" ).nl();
			}

			public Dest newDocument( Dimension size, Dest app ) throws Exception{
				app
				.app( "(gimp-image-set-filename " )
				.app( "(set! " ).app( STR_VAR_NEW_DOC ).app( " (car (gimp-image-new "  )
				.app( s( size.width  ) ).app( " " )
				.app( s( size.height ) )
				.app( " RGB))) \"" );
				return docname( size, app )
				.app( "\")" );
			}

			public Dest modifySelection( Rectangle rect, SelectionType st, Dest app ) throws Exception{
				int    r  = 10, w = 4;
				char   l  = ' ';
				String sx = s( rect.x     , r, w, l ),
				       sy = s( rect.y     , r, w, l ),
				       sw = s( rect.width , r, w, l ),
				       sh = s( rect.height, r, w, l );
				return app
				.app( "(gimp-rect-select " ).app( STR_VAR_NEW_DOC )
				.app( ' ' ).app( sx )
				.app( ' ' ).app( sy )
				.app( ' ' ).app( sw )
				.app( ' ' ).app( sh )
				.app( " " ).app( encode( st ) ).app( " FALSE 0)" );
			}

			public Dest   saveSelection(    String name, Dest app ) throws Exception{
				return app
				.app( "(gimp-drawable-set-name (car (gimp-selection-save " ).app( STR_VAR_NEW_DOC ).app( ")) \"" ).app( name ).app( "\")" ).nl();
			}

			public Dest            show(                 Dest app ) throws Exception{
				return app.app( "(gimp-display-new " ).app( STR_VAR_NEW_DOC ).app( ")" ).nl();
			}

			public Dest   headercomment(                  String[] comment, Dest app ) throws Exception{
				app.app( "; -*-scheme-*-" ).nl().nl();
				return super.headercomment( comment, app );
			}

			public Dest           begin( String scriptname,                 Dest app ) throws Exception{
				return app.nl()
				.app( "(define (script-fu-" ).app( scriptname ).app( ")" ).nl();
			}

			public Dest          finish( String scriptname, String[] descr, Dest app ) throws Exception{
				app.app( ")" ).nl();
				String ds = makeDateString();
				app
				.nl().app( "(script-fu-register \"script-fu-" ).app( scriptname ).app( "\"" ).nl()
				.app( "  \"" ).app( scriptname ).app( "\"" ).nl()
				.app( "  \"" );
				String   firstlines = descr[0], lastlines = descr[descr.length - 1], first, last;
				String[] split;
				for( String lines : descr ){
					split = lines.replace( "\"", "\\\"" ).split( " *\n *" );
					first = split[0];
					last  = split[split.length - 1];
					for( String line : split ){
						if( lines != firstlines || line != first ){ app.app( "   " ); }
						app.app( line );
						if( lines != lastlines  || line != last  ){ app.app( "\\" ).nl(); }
					}
				}
				app
				.app( "\"" ).nl()
				.app( "  \"keith cascio\"" ).nl()
				.app( "  \"keith cascio, " ).app( ds ).app( ".\"" ).nl()
				.app( "  \""               ).app( ds ).app(  "\"" ).nl()
				.app( "  \"\"" ).nl().app( ")" ).nl()
				.nl().app( "(script-fu-menu-register \"script-fu-" ).app( scriptname ).app( "\"" ).nl()
				.app( "  \"<Toolbox>/Xtns/" ).app( UI.STR_SAMIAM_ACRONYM ).app( "\")" ).nl();
				return app;
			}

			public Dest assertChannelExists(   String name,                   Dest app ) throws Exception{
				app
				.app( "(gimp-image-get-channels " ).app( STR_VAR_NEW_DOC ).app( ")" );
				return app;
			}

			public Dest     saveLayerName(    String vname,                   Dest app ) throws Exception{ return app; }
			public Dest      defineString(    String vname, String   value,   Dest app ) throws Exception{ return app; }
			public Dest       concatenate( String lhs, String rhs1, String rhs2, Dest app ) throws Exception{ return app; }
			public Dest         duplicate(                                    Dest app ) throws Exception{ return app; }
			public Dest        selectNone(                                    Dest app ) throws Exception{ return app; }
			public Dest selectionFromChannel( String  name, SelectionType st, Dest app ) throws Exception{ return app; }
			public Dest brightnessContrast( float brightness, float contrast, Dest app ) throws Exception{ return app; }
			public Dest      setLayerName(    String vname, String   value,   Dest app ) throws Exception{ return app; }
		};

		public static Dest   docname( Dimension size, Dest app ) throws Exception{
			return app.app( makeDateString() ).app( "_SamIam_screenshots_template_" ).app( s( size.width ) ).app( 'x' ).app( s( size.height ) );
		}

		public static String s( int integer ){ return s( integer, 10, -1, ' ' ); }

		public static String s( int integer, int radix, int width, char left ){
			String digits = Integer.toString( integer, radix );
			if( width < 1 ){ return digits; }

			BUFF.setLength(0);
			for( int i = width - digits.length(); i > 0; --i ){ BUFF.append( left ); }
			return BUFF.append( digits ).toString();
		}
		public static final StringBuilder BUFF = new StringBuilder( 8 );

		public Dest           begin(    String sname,                   Dest app ) throws Exception{ return app; }
		public Dest   headercomment(                  String[] comment, Dest app ) throws Exception{
			app.app( slcomment )
			   .app( " author: keith cascio, since: " )
			   .app( makeDateString() ).nl();
			for( String line : comment ){
				for( String comm : line.split( " *\n *" ) ){
					app.app( slcomment ).app( ' ' ).app( comm ).nl();
				}
			}
			return app;
		}
		public Dest    openLocalScope(                 Dest app, String ... locals ) throws Exception{ return app; }
		public Dest   closeLocalScope(                                    Dest app ) throws Exception{ return app; }
		public Dest       newDocument( Dimension  size,                   Dest app ) throws Exception{ return app; }
		public Dest   modifySelection( Rectangle  rect, SelectionType st, Dest app ) throws Exception{ return app; }
		public Dest     saveSelection(    String  name,                   Dest app ) throws Exception{ return app; }
		public Dest              show(                                    Dest app ) throws Exception{ return app; }
		public Dest            finish(    String sname, String[] comment, Dest app ) throws Exception{ return app; }

		public Dest assertOpenDocument(                                   Dest app ) throws Exception{ return app; }
		public Dest assertChannelExists(   String name,                   Dest app ) throws Exception{ return app; }
		public Dest     saveLayerName(    String vname,                   Dest app ) throws Exception{ return app; }
		public Dest      defineString(    String vname, String   value,   Dest app ) throws Exception{ return app; }
		public Dest       concatenate( String lhs, String rhs1, String rhs2, Dest app ) throws Exception{ return app; }
		public Dest         duplicate(                                    Dest app ) throws Exception{ return app; }
		public Dest        selectNone(                                    Dest app ) throws Exception{ return app; }
		public Dest selectionFromChannel( String  name, SelectionType st, Dest app ) throws Exception{ return app; }
		public Dest brightnessContrast( float brightness, float contrast, Dest app ) throws Exception{ return app; }
		public Dest      setLayerName(    String vname, String   value,   Dest app ) throws Exception{ return app; }

		public String        encode( SelectionType selectiontype ){
			if( SELECTION_TYPES == null ){
				SELECTION_TYPES = new EnumMap<ScriptLanguage,Map<SelectionType,String>>( ScriptLanguage.class );
				Map<SelectionType,String> map_adobejavascript = new EnumMap<SelectionType,String>( SelectionType.class );
				map_adobejavascript .put( SelectionType.diminish,  "SelectionType.DIMINISH " );
				map_adobejavascript .put( SelectionType.extend,    "SelectionType.EXTEND   " );
				map_adobejavascript .put( SelectionType.intersect, "SelectionType.INTERSECT" );
				map_adobejavascript .put( SelectionType.replace,   "SelectionType.REPLACE  " );
				Map<SelectionType,String> map_gimpscriptfu = new EnumMap<SelectionType,String>( SelectionType.class );
				map_gimpscriptfu    .put( SelectionType.diminish,     "CHANNEL-OP-SUBTRACT " );
				map_gimpscriptfu    .put( SelectionType.extend,       "CHANNEL-OP-ADD      " );
				map_gimpscriptfu    .put( SelectionType.intersect,    "CHANNEL-OP-INTERSECT" );
				map_gimpscriptfu    .put( SelectionType.replace,      "CHANNEL-OP-REPLACE  " );
				SELECTION_TYPES.put( adobejavascript, map_adobejavascript );
				SELECTION_TYPES.put( gimpscriptfu,    map_gimpscriptfu    );
			}
			return SELECTION_TYPES.get( this ).get( selectiontype );
		}

		static private Map<ScriptLanguage,Map<SelectionType,String>> SELECTION_TYPES;

		private ScriptLanguage( String name, String fileextension, String slcomment, String sifname, String lifname, String url ){
			this.name             =    name;
			this.fileextension    =    fileextension;
			this.slcomment        =    slcomment;
			this.url              =    url;

			this.properties.put(     display, name );
			this.properties.put(     tooltip, "<html>" + name + ", <a href='" + url + "'>" + url + "</a>" );
			this.properties.put( accelerator, firstUniqueKeyStroke( name ) );
			if( sifname != null ){
				this.properties.put( smallicon, MainToolBar.getIcon( sifname ) );
			}
			if( lifname != null ){
				this.properties.put( largeicon, MainToolBar.getIcon( lifname ) );
			}
		}
		public  final           String slcomment, name, url, fileextension;

		public EnumModels<ScriptLanguage> models(){ return MODELS; }

		public String              describeAngle(){ return "language"; }

		public Semantics       semantics(){ return          exclusive; }

		public ScriptLanguage getDefault(){ return    adobejavascript; }

		public Object   get( Property property ){ return this.properties.get( property ); }

		private Map<Property,Object> properties = new EnumMap<Property,Object>( Property.class );

		public static AbstractButton[]   buttons( View view, Object id, ItemListener listener ){
			return MODELS.newButtons( view, id, listener );
		}

		public static   ScriptLanguage  selected( Object id ){
			return MODELS.selected( exclusive, id );
		}

		public static EnumModels<ScriptLanguage> MODELS = new EnumModels<ScriptLanguage>( ScriptLanguage.class );
	}

	/** @since 20080122 */
	public enum SelectionType{
		diminish,
		extend,
		intersect,
		replace;
	}

	/** @since 20080122 */
	public enum Parameter{
		samiamui         ( UI.STR_SAMIAM_ACRONYM + " main window" ),
		quadrants2mask   ( "quadrants added to channel '"+STR_CHANNEL_BARS+"'", Quadrant.class, Quadrant.titlebar, Quadrant.titlebar, Quadrant.menubar, Quadrant.toolbar, Quadrant.icbtoolbar, Quadrant.statusbar, Quadrant.activeniftitlebar, Quadrant.activendtitlebar ),
		quadrants2unmask ( "quadrants subtracted from '" +STR_CHANNEL_BARS+"'", Quadrant.class, Quadrant.insets );

		public static Map<Parameter,Object> params( Map<Parameter,Object> params ){
			if( params == null ){ params = new EnumMap<Parameter,Object>( Parameter.class ); }
			return params;
		}

		public static Map<Parameter,Object> params( Map<Parameter,Object> params, UI ui ){
			params = params( params );
			params.put( samiamui,  ui    );
			return params;
		}

		public static Map<Parameter,Object> params( Map<Parameter,Object> params, Quadrant ... quads ){
			params = params( params );
			params.put( quadrants2mask, quads );
			return params;
		}

		@SuppressWarnings( "unchecked" )
		private Parameter( String name ){ this( name, null ); }

		private <T extends Enum<T> & Actionable<T> & Semantic & Choice<T>> Parameter( String name, Class<T> clazz, T ... selected ){
			this.name    = name;
			this.choices = clazz == null ? null : new Choices<T>( clazz, selected );
		}

		@SuppressWarnings( "unchecked" )
		public <T extends Enum<T> & Actionable<T> & Semantic & Choice<T>> Choices<T> choices(){
			return (Choices<T>) this.choices;
		}

		public final String     name;
		public final Choices<?> choices;
	}

	/** @since 20080122 */
	static public class Choices<E extends Enum<E> & Actionable<E> & Semantic & Choice<E>>{
		public Choices( Class<E> clazz, E ... selected ){
			this.clazz    = clazz;
			this.selected = selected;
		}

		public Enum<E> firstEnum(){
			return clazz.getEnumConstants()[0];
		}

		public Actionable<E> firstActionable(){
			return clazz.getEnumConstants()[0];
		}

		public Semantic firstSemantic(){
			return clazz.getEnumConstants()[0];
		}

		public Choice<E> firstChoice(){
			return clazz.getEnumConstants()[0];
		}

		public E getDefault(){
			return clazz.getEnumConstants()[0].getDefault();
		}

		public int size(){
			return clazz.getEnumConstants().length;
		}

		@SuppressWarnings( "unchecked" )
		public E[] selectedAsArray( Object id ){
			E             element = getDefault();
			EnumModels<E> models  = element.models();
			Set<E>        set     = models.selected( element.semantics(), id, EnumSet.noneOf( clazz ) );
			return set.toArray( (E[]) java.lang.reflect.Array.newInstance( clazz, set.size() ) );
		}

		@SuppressWarnings( "unchecked" )
		public Set<E>[] allAndSelected(){
			Set<E> ss = EnumSet.noneOf( clazz );
			for( E elt : selected ){ ss.add( elt ); }
			return (Set<E>[]) new Set[]{ EnumSet.allOf( clazz ), ss };
		}

		public final Class<E>   clazz;
		public final       E[]  selected;
	}

	/** @since 20080122 */
	public enum Task implements Actionable<Task>, Semantic, Choice<Task>{
		create_screenshot_template_doc(        "create template document",
		                                "samiam-create-template",
		                                EnumSet.of( Parameter.samiamui, Parameter.quadrants2mask, Parameter.quadrants2unmask ),
		                                "this script creates a template document",
		                                "for screenshots based on the current SamIam window size",
		                                "and title bar/menu bar configuration.",
		                                "adds a channel named '"+STR_CHANNEL_BARS+"'." ){
			public Dest impl( ScriptLanguage lang, Map<Parameter,Object> params, Dest app ) throws Exception
			{
				lang.openLocalScope( app, STR_VAR_NEW_DOC );

				UI ui = (UI) params.get( Parameter.samiamui );
				lang.newDocument( ui.getSize(), app ).nl().nl();

				Quadrant[] quadrants2mask = (Quadrant[]) params.get( Parameter.quadrants2mask );
				if( quadrants2mask == null ){ quadrants2mask = Quadrant.values(); }

				Map<Quadrant,Mask> silhouette;
				if( quadrants2mask.length > 0 ){
					silhouette = silhouette( ui, quadrants2mask );
					for( Quadrant quad : silhouette.keySet() ){
						for( Rectangle rect : silhouette.get( quad ).rectangles ){
							lang.modifySelection( rect, SelectionType.extend, app ).app( lang.slcomment ).app( " select " ).app( quad.get( display ).toString() ).nl();
						}
					}
					app.nl();
				}

				Quadrant[] quadrants2unmask = (Quadrant[]) params.get( Parameter.quadrants2unmask );
				if( (quadrants2unmask != null) && (quadrants2unmask.length > 0) ){
					silhouette = silhouette( ui, quadrants2unmask );
					for( Quadrant quad : silhouette.keySet() ){
						for( Rectangle rect : silhouette.get( quad ).rectangles ){
							lang.modifySelection( rect, SelectionType.diminish, app ).app( lang.slcomment ).app( " de-select " ).app( quad.get( display ).toString() ).nl();
						}
					}
					app.nl();
				}

				lang.saveSelection( STR_CHANNEL_BARS, app );
				lang.show( app );

				lang.closeLocalScope( app );

				return app;
			}
		},
		fade_bars(        "fade bars",
		           "samiam-fade-bars",
		           EnumSet.of( Parameter.samiamui ),
		           "this script operates on the 'bars' channel",
		           "created by the '"+create_screenshot_template_doc.name+"' script.",
		           "it fades the part of the image designated by",
		           "the 'bars' channel by brightening it, lessening",
		           "the contrast on a white background." ){
			public Dest impl( ScriptLanguage lang, Map<Parameter,Object> params, Dest app ) throws Exception
			{
				UI ui = (UI) params.get( Parameter.samiamui );

				String vLayerName = "layername";
				String vSuffix    =    "suffix";
				String vBaptism   =  "baptized";

				lang.openLocalScope( app, vLayerName, vSuffix, vBaptism );

				app.nl();
				lang.assertOpenDocument( app );
				lang.assertChannelExists( STR_CHANNEL_BARS, app );
				lang.saveLayerName( vLayerName,                    app );
				lang .defineString( vSuffix,             " brite", app );
				lang  .concatenate( vBaptism, vLayerName, vSuffix, app );
				lang    .duplicate(                                app );
				lang   .selectNone(                                app );
				lang.selectionFromChannel( STR_CHANNEL_BARS, SelectionType.extend, app );
				lang  .brightnessContrast(               0.5f, 0f, app );
				lang   .selectNone(                                app );
				lang .setLayerName( vBaptism,                null, app );

				lang.closeLocalScope( app );

				return app;
			}
		},
		empty( "test", "samiam-test", EnumSet.noneOf( Parameter.class ), "this script is an empty test" );

		public Dest   impl( ScriptLanguage lang, Map<Parameter,Object> params, Dest app ) throws Exception{ return app; }

		public Dest script( ScriptLanguage lang, Map<Parameter,Object> params, Dest app ) throws Exception{
			lang                       .headercomment(                this.comment, app );
			lang                               .begin( this.codename,               app );
			this                                .impl(  lang, params,               app );
			lang                              .finish( this.codename, this.comment, app );
			return app;
		}

		private Task( String name, String codename, Set<Parameter>  relevance, String ... comment ){
			this.name      =     name;
			this.codename  = codename;
			this.relevance = relevance;
			this.comment   = comment;

			String html = "<html>" + name;
			this.properties.put(     display, html );
			this.properties.put(     tooltip, html + ": " + comment[0] );
			this.properties.put( accelerator, firstUniqueKeyStroke( name ) );
		}
		public final  String          name, codename;
		public final  String[]        comment;
		public final  Set<Parameter>  relevance;

		public EnumModels<Task>     models(){ return MODELS; }

		public String        describeAngle(){ return "task"; }

		public Semantics   semantics(){ return                         exclusive; }

		public Task       getDefault(){ return    create_screenshot_template_doc; }

		public Object   get( Property property ){ return this.properties.get( property ); }

		private Map<Property,Object> properties = new EnumMap<Property,Object>( Property.class );

		public static AbstractButton[]   buttons( View view, Object id, ItemListener listener ){
			return MODELS.newButtons( view, id, listener );
		}

		public static   Task  selected( Object id ){
			return MODELS.selected( exclusive, id );
		}

		public static EnumModels<Task> MODELS = new EnumModels<Task>( Task.class );
	}

	/** @since 20080122 */
	public static Dest script( ScriptLanguage lang, Task task, Map<Parameter,Object> params, Dest app ) throws Exception{
		return task.script( lang, params, app );
	}

	/** @since 20080122 */
	public static Dest create_screenshot_template_doc( UI ui, ScriptLanguage lang, Dest app ) throws Exception
	{
		Task task = Task.create_screenshot_template_doc;

		Map<Parameter,Object> params = Parameter.params( Parameter.params( null ), ui );
		Parameter.params( params, Quadrant.titlebar, Quadrant.titlebar, Quadrant.menubar, Quadrant.toolbar, Quadrant.icbtoolbar, Quadrant.statusbar, Quadrant.activeniftitlebar, Quadrant.activendtitlebar );
		params.put( Parameter.quadrants2unmask, Quadrant.insets );

		return script( lang, task, params, app );
	}

	/** @since 20080122 */
	public static UI scriptDialog( UI ui ){
		JComponent                    msg = scriptPanel( ui );
		JOptionResizeHelperListener jorhl = new JOptionResizeHelperListener(){
			public void topLevelAncestorDialog( JDialog container, JOptionResizeHelper helper ){
				try{
					java.lang.reflect.Method method = getMethodJDialogSetIconImage();
					if( method != null ){
						ImageIcon icon = MainToolBar.getIcon( "CursiveS16.gif" );
						if( icon != null ){
							method.invoke( container, icon.getImage() );//container.setIconImage( icon.getImage() );//
						}
					}

					Thread.sleep( 0x40 );
					container.pack();
				}catch( Throwable thrown ){
					System.err.println( "warning: ScreenCaptureAutomation.scriptDialog().jorhl.topLevelAncestorDialog() caught " + thrown );
				}
			}
		};
		new JOptionResizeHelper( msg, true, 0x1000, jorhl ).start();
		JOptionPane.showMessageDialog( ui, msg, "Screenshots Helper Scripts", JOptionPane.PLAIN_MESSAGE );
		return ui;
	}

	/** @since 20080123 */
	static public class TaskListener extends SamiamAction implements ItemListener, Runnable, ActionListener{
		public TaskListener(){
			super( "Write Script", "encode the selected task as a script in the selected language", 'w', MainToolBar.getIcon( "CursiveS16.gif" ) );
		}

		public void itemStateChanged( ItemEvent event ){
			if( event.getStateChange() != ItemEvent.SELECTED ){ return; }
			invokeLater( this );
		}

		public void run(){
			try{
				Task           task = Task.selected( menuid );
				if( task == null ){ return; }
				Set<Parameter> relevance = task.relevance;
				boolean        pack      = false;
				boolean        visible;
				Component      comp;
				for( Parameter param : mapP2C.keySet() ){
					visible = relevance.contains( param );
					comp    = mapP2C.get( param );
					if( comp.isVisible() != visible ){
						comp.setVisible( visible );
						pack = true;
					}
				}
				if( pack ){
					Window window = getWindowAncestor( mapP2C.values().iterator().next() );
					if( window != null ){
						Thread.sleep( edu.ucla.belief.ui.dialogs.ProbabilityRewrite.LONG_PACK_DELAY );
						window.pack();
					}
				}
			}catch( Throwable thrown ){
				System.err.println( "warning: ScreenCaptureAutomation.TaskListener.run() caught " + thrown );
				if( Util.DEBUG_VERBOSE ){
					Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
					thrown.printStackTrace( System.err );
				}
			}
		}

		public void actionPerformed( ActionEvent event ){
			try{
				if( myui == null ){
					System.err.println( "warning: ScreenCaptureAutomation.TaskListener.actionPerformed() called with no UI reference" );
					return;
				}

				Task                  task      =  Task.selected(           menuid );
				ScriptLanguage        language  =  ScriptLanguage.selected( menuid );
				Redirect              redirect  =  Redirect.selected(       menuid );
				Map<Parameter,Object> params    =  Parameter.params( null );
				params.put( Parameter.samiamui, myui );
				for( Parameter parameter : task.relevance ){
					if( parameter.choices == null ){ continue; }
					params.put( parameter, parameter.choices.selectedAsArray( mapP2ID.get( parameter ) ) );
				}
				NetworkInternalFrame  nif       = myui.getActiveHuginNetInternalFrame();
				String                title     = null;//"writing script: " + task.name;

				Map<Context,Object>   bucket    = Context.bucket();
				bucket.put( Context.nif,           nif      );
				bucket.put( Context.title,         title    );
				bucket.put( Context.actionable,    language );
				bucket.put( Context.filename,      task.codename + "." + language.fileextension );
				bucket.put( Context.fileextension,                       language.fileextension );

				script( language, task, params, redirect.open( bucket ) ).flush();
			}catch( Throwable thrown ){
				System.err.println( "warning: ScreenCaptureAutomation.TaskListener.actionPerformed() caught " + thrown );
				if( true ){//Util.DEBUG_VERBOSE ){
					Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
					thrown.printStackTrace( System.err );
				}
			}
		}

		public JComponent scriptPanel( UI ui ){
			this.myui              = ui;
			JPanel             pnl = new JPanel( new GridBagLayout() );
			GridBagConstraints c   = new GridBagConstraints();

		  /*JPanel pnlDebug = new JPanel();
			pnlDebug.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );
			c.weightx              = 1;
			c.gridwidth            = GridBagConstraints.REMAINDER;
			c.fill                 = GridBagConstraints.BOTH;
			pnl.add( pnlDebug, c );*/

			c.weightx              = 0;
			c.gridwidth            = 1;
			c.fill                 = GridBagConstraints.NONE;
			pnl.add( Box.createGlue(), c );

			final TaskListener tasklistener = this;

			for( Parameter param : Parameter.values() ){ mapP2ID.put( param, new Object() ); }

			Quadrant.MODELS.enforceExclusivity(  additive,
				mapP2ID.get( Parameter.quadrants2mask   ),
				mapP2ID.get( Parameter.quadrants2unmask ) );

			final JComponent pnlTasks = pnl( EnumSet.allOf( Task.class ), EnumSet.of( Task.values()[0].getDefault() ), false, tasklistener, menuid );

			JPanel             pnlLeft = new JPanel( new GridBagLayout() );
			GridBagConstraints   cLeft = new GridBagConstraints();
			cLeft.gridwidth            = GridBagConstraints.REMAINDER;
			cLeft.weighty              = 1;
			cLeft.fill                 = GridBagConstraints.BOTH;
			pnlLeft.add( pnlTasks, cLeft );
			cLeft.fill                 = GridBagConstraints.HORIZONTAL;
			cLeft.weightx              = cLeft.weighty = 0;
			cLeft.insets               = new Insets( 4,8,4,8 );
			pnlLeft.add( new JButton( this ), cLeft );

			c.anchor               = GridBagConstraints.NORTHWEST;
			c.weightx              = 1;
			c.weighty              = 1;
			c.fill                 = GridBagConstraints.BOTH;
			pnl.add( pnlLeft, c );

		  //c.gridheight = 2;
			JComponent pnlparameter;
			for( Parameter parameter : Parameter.values() ){
				if( parameter.choices == null ){ continue; }

				pnl.add( pnlparameter = ScreenCaptureAutomation.pnl( parameter.choices, parameter.name, FLAG_SCROLL && (parameter.choices.size() > 2), (ItemListener) null /*mapP2IL.get( parameter )*/, mapP2ID.get( parameter ) ), c );
				mapP2C.put( parameter, pnlparameter );
			}

			final JComponent pnlLangs    = pnl( EnumSet.allOf( ScriptLanguage.class ), EnumSet.of( ScriptLanguage.values()[0].getDefault() ), false, (ItemListener) null, menuid );
			pnl.add( pnlLangs, c );

			final JComponent pnlRedirect = pnl( Redirect.MODELS, strRedirect, EnumSet.allOf( Redirect.class ), EnumSet.of( Redirect.txt2dialog ), FLAG_SCROLL, (ItemListener) null, menuid );
			pnl.add( pnlRedirect, c );

			c.weightx              = 0;
			c.weighty              = 0;
			c.gridwidth = GridBagConstraints.REMAINDER;
			pnl.add( Box.createGlue(), c );

		  /*c.gridwidth            = 1;
			c.weightx              = 0;
			c.weighty              = 0;
			c.fill                 = GridBagConstraints.BOTH;
			for( int i=0; i<7; i++ ){ pnl.add( wort( i == 0 || i == 6 ? 0x20 : 0x80 ), c ); }*/

		  /*c.gridheight = 1;
			c.gridwidth  = 1;
			pnl.add( Box.createGlue(),              c );
			c.fill       = GridBagConstraints.HORIZONTAL;
			c.weightx    = c.weighty = 0;
			c.insets     = new Insets( 4,4,0,4 );
			pnl.add( new JButton( this ), c );*/

			return pnl;
		}

		public static           boolean   FLAG_SCROLL  = true;

		public static     final String    strLangs     = "language",
		                                  strRedirect  = "output";

		public            final Object       menuid    = new Object();
		private   UI                         myui;
		private   Map<Parameter,Component>   mapP2C    =   new EnumMap<Parameter,Component>( Parameter.class );
		private   Map<Parameter,Object>      mapP2ID   =   new EnumMap<Parameter,   Object>( Parameter.class );
	}

	static public JComponent wort( int width ){
		JLabel ret = new JLabel( "wort" );
		ret.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createEmptyBorder( 1,1,1,1 ), BorderFactory.createLineBorder( Color.blue, 1 ) ) );
		Dimension dim = ret.getPreferredSize();
		dim.width = width;
		ret.setPreferredSize( dim );
		ret.setMinimumSize( dim );
		return ret;
	}

	/** @since 20080122 */
	public static JComponent scriptPanel( UI ui ){
		return new TaskListener().scriptPanel( ui );
	}

	/** @since 20080122 */
	public interface Choice<E extends Enum<E> & Actionable<E>>{
		public EnumModels<E>        models();
		public String        describeAngle();
	}

	/** @since 20080122 */
	@SuppressWarnings( "unchecked" )
	public static JComponent pnl( Choices choices, String title, boolean scrollable, ItemListener il, Object id ){
		Set[]  array  = choices.allAndSelected();
		Choice choice = choices.firstChoice();
		return pnl( choice.models(), title == null ? choice.describeAngle() : title, array[0], array[1], scrollable, il, id );
	}
  /*public static <T extends Enum<T> & Actionable<T> & Semantic & Choice<T>> JComponent pnl( Choices<T> choices, String title, boolean scrollable, ItemListener il, Object id ){
		Set<T>[] array = ((Choices<T>) choices).allAndSelected();
		Choice<T> choice = choices.firstChoice();
		return pnl( choice.models(), title == null ? choice.describeAngle() : title, array[0], array[1], scrollable, il, id );
	}*/

	/** @since 20080122 */
	public static JComponent pnl( Set elements, Set selected, boolean scrollable, ItemListener il, Object id ){
		Choice element = (Choice) elements.iterator().next();
		return pnl( element.models(), element.describeAngle(), elements, selected, scrollable, il, id );
	}
  /*public static <T extends Enum<T> & Actionable<T> & Semantic & Choice<T>> JComponent pnl( Set<T> elements, Set<T> selected, boolean scrollable, ItemListener il, Object id ){
		T element = elements.iterator().next();
		return pnl( element.models(), element.describeAngle(), elements, selected, scrollable, il, id );
	}*/

	/** @since 20080123 */
	static public class ScrollablePanel extends JPanel implements Scrollable{
		public ScrollablePanel( LayoutManager lm ){ super( lm ); }

		public Dimension getPreferredScrollableViewportSize(){
			Dimension ret = getPreferredSize();
			ret.width    += 0x20;
			return ret;
		}
		public int     getScrollableUnitIncrement(  Rectangle visibleRect, int orientation, int direction ){ return 1; }
		public int     getScrollableBlockIncrement( Rectangle visibleRect, int orientation, int direction ){ return 1; }
		public boolean getScrollableTracksViewportWidth() { return true;  }
		public boolean getScrollableTracksViewportHeight(){ return false; }
	}

	/** @since 20080124 */
	public static Component iconComponent( Actionable element ){
		Icon icon = null;
		if(      element.get( largeicon ) != null ){ icon = (Icon) element.get( largeicon ); }
		else if( element.get( smallicon ) != null ){ icon = (Icon) element.get( smallicon ); }

		if(   icon == null ){ return Box.createGlue(); }
		else{                 return new JLabel( icon ); }
	}

	/** @since 20080122 */
	@SuppressWarnings( "unchecked" )
	public static JComponent pnl( EnumModels models, String title, Set elements, Set selected, boolean scrollable, ItemListener il, Object id ){
		if( elements.isEmpty() ){ return null; }

		JComponent           pnl = new ScrollablePanel( new GridBagLayout() );//new GridLayout( elements.size(), 1 ) );
		GridBagConstraints   c   = new GridBagConstraints();
		c.anchor                 = GridBagConstraints.WEST;
		c.gridwidth              = GridBagConstraints.REMAINDER;

		Semantic         element = (Semantic) elements.iterator().next();
		if( selected == null ){ selected = Collections.emptySet(); }

		LinkedList          list = new LinkedList( elements );
		Actionable[]       array = (Actionable[]) list.toArray( new Actionable[ list.size() ] );
		AbstractButton[] buttons = models.newButtons( element.semantics().forPanel(), id, il, list, selected );

		for( int i=0; i<array.length; i++ ){
			c.weightx            = 0;
			c.gridwidth          = 1;
			pnl.add( iconComponent( array[i] ), c );
			pnl.add(              buttons[i]  , c );
			c.weightx            = 1;
			c.gridwidth          = GridBagConstraints.REMAINDER;
			pnl.add(          Box.createGlue(), c );
		}
		c.weightx                = 0;
		c.weighty                = 1;
		pnl.add( Box.createGlue(), c );

		pnl.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createEmptyBorder( 0,4,0,4 ), BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), title ) ) );

		if( scrollable ){ pnl = SCROLLABILITY.addScroll( pnl ); }

		return pnl;
	}
  /*public static <T extends Enum<T> & Actionable<T> & Semantic> JComponent pnl( EnumModels<T> models, String title, Set<T> elements, Set<T> selected, boolean scrollable, ItemListener il, Object id ){
		if( elements.isEmpty() ){ return null; }

		JComponent           pnl = new ScrollablePanel( new GridBagLayout() );//new GridLayout( elements.size(), 1 ) );
		GridBagConstraints   c   = new GridBagConstraints();
		c.anchor                 = GridBagConstraints.WEST;
		c.gridwidth              = GridBagConstraints.REMAINDER;

		T                element = elements.iterator().next();
		if( selected == null ){ selected = Collections.emptySet(); }

		LinkedList<T>       list = new LinkedList<T>( elements );
		T[]                array = list.toArray( (T[]) new Enum[ list.size() ] );
		AbstractButton[] buttons = models.newButtons( element.semantics().forPanel(), id, il, list, selected );

		for( int i=0; i<array.length; i++ ){
			c.weightx            = 0;
			c.gridwidth          = 1;
			pnl.add( iconComponent( array[i] ), c );
			pnl.add(              buttons[i]  , c );
			c.weightx            = 1;
			c.gridwidth          = GridBagConstraints.REMAINDER;
			pnl.add(          Box.createGlue(), c );
		}
		c.weightx                = 0;
		c.weighty                = 1;
		pnl.add( Box.createGlue(), c );

		pnl.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createEmptyBorder( 0,4,0,4 ), BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), title ) ) );

		if( scrollable ){ pnl = SCROLLABILITY.addScroll( pnl ); }

		return pnl;
	}*/

	/** @since 20080123 */
	public enum Scrollability implements Semantic, Actionable<Scrollability>{
		panel( "JScrollPane wrapped in JPanel( GridBagLayout )" ){
			public JComponent addScroll( JComponent comp ){
				Border          border = comp.getBorder();
				JPanel             ret = new JPanel( new GridBagLayout() );
				GridBagConstraints c   = new GridBagConstraints();
				c.weightx              = c.weighty = 1;
				c.fill                 = GridBagConstraints.BOTH;
				c.gridwidth            = GridBagConstraints.REMAINDER;
				ret.add( configure( new JScrollPane( comp ) ), c );
				ret.setBorder( border );
				comp.setBorder( null );
				return ret;
			}
		},
		pain( "bare JScrollPane" );

		public JComponent addScroll( JComponent comp ){
			Border    border = comp.getBorder();
			JScrollPane  ret = configure( new JScrollPane( comp ) );
			ret.setBorder( border );
			comp.setBorder( null );
			return ret;
		}

		static public JScrollPane configure( JScrollPane pain ){
			Dimension    dim = pain.getViewport().getView().getPreferredSize();
			Border   vborder = pain.getViewportBorder();
			Insets    insets = vborder == null ? new Insets(4,4,4,4) : vborder.getBorderInsets( pain.getViewport() );
			dim.width       += insets.left + insets.right + pain.getVerticalScrollBar().getPreferredSize().width;
			dim.height      += insets.top  + insets.bottom;
			pain.setPreferredSize( dim );
			pain.setVerticalScrollBarPolicy(   ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED );//VERTICAL_SCROLLBAR_ALWAYS    );//
			pain.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER   );
			return pain;
		}

		private Scrollability( String description ){
			this.description = description;

			this.properties.put(     display, name() );
			this.properties.put(     tooltip, description );
			this.properties.put( accelerator, firstUniqueKeyStroke( name() ) );
		}
		public final String description;

		public Semantics      semantics(){ return  exclusive; }

		public Scrollability getDefault(){ return       pain; }

		public Object   get( Property property ){ return this.properties.get( property ); }

		private Map<Property,Object> properties = new EnumMap<Property,Object>( Property.class );
	}
	public static Scrollability SCROLLABILITY = Scrollability.values()[0].getDefault();

	/** @since 20050303 */
	@SuppressWarnings( "unchecked" )
	public static void printImageIOFormats( PrintStream out ){
		String[] formats = ImageIO.getWriterFormatNames();
		Comparator comp = java.text.Collator.getInstance();
		java.util.List<String> listFormats = Arrays.asList( formats );
		Collections.sort( listFormats, (Comparator<String>) comp );
		out.println( "write formats: " + listFormats );

		formats = ImageIO.getReaderFormatNames();
		listFormats = Arrays.asList( formats );
		Collections.sort( listFormats, (Comparator<String>) comp );
		out.println( "read  formats: " + listFormats );
	}

	private void init(){
		try{
			boolean flagDoPause = mySetSelected.contains( STR_ARG_PAUSE );
			mySetSelected.remove( STR_ARG_PAUSE );
			this.myPause               = new Pause( flagDoPause );
			this.myDelay               = STR_DELAY == null ? -1 : Long.valueOf( STR_DELAY );
			this.mySamiamPreferences   = myUI.getSamiamPreferences();
			this.globalPrefs           = mySamiamPreferences.getPreferenceGroup( SamiamPreferences.PkgDspNme      );
			this.netPrefs              = mySamiamPreferences.getPreferenceGroup( SamiamPreferences.NetDspNme      );
			this.treePrefs             = mySamiamPreferences.getPreferenceGroup( SamiamPreferences.TreeDspNme     );
			this.monitorPrefs          = mySamiamPreferences.getPreferenceGroup( SamiamPreferences.MonitorsDspNme );
			this.myRobot               = new Robot();
			this.myNameDirectoryOutput = STR_NAME_DIRECTORY_OUTPUT_PRE + makeDateString();
			File doutput               = new File( myNameDirectoryOutput );
			if( !doutput.exists() ) doutput.mkdir();
		}catch( Exception e ){
			fail( e );
		}
	}

	private static Set<String> processArgs( String[] args ){
		Set<String> ret = new HashSet<String>();
		for( int i=0; i<args.length; i++ ){
			if( args[i].startsWith( STR_ARG_SELECT ) ){
				ret.add( args[i].substring( STR_ARG_SELECT.length() ) );
				args[i] = UI.STR_ARG_NOSPLASH;
			}
			if( args[i].startsWith( STR_ARG_POINTER ) ){
				STR_PATH_POINTER = args[i].substring( STR_ARG_POINTER.length() );
				args[i] = UI.STR_ARG_NOSPLASH;
			}
			if( args[i].startsWith( STR_ARG_DELAY ) ){
				STR_DELAY = args[i].substring( STR_ARG_DELAY.length() );
				args[i] = UI.STR_ARG_NOSPLASH;
			}
			else if( args[i].equals( STR_ARG_PAUSE ) ){
				ret.add( args[i] );
				args[i] = UI.STR_ARG_NOSPLASH;
			}
			else if( args[i].equals( STR_ARG_NOEXIT ) ){
				FLAG_EXIT = false;
				args[i] = UI.STR_ARG_NOSPLASH;
			}
			else if( args[i].equals( STR_ARG_NOTHUMBS ) ){
				FLAG_THUMBS = false;
				args[i] = UI.STR_ARG_NOSPLASH;
			}
			else if( args[i].equals( STR_ARG_FORMATS ) ){
				FLAG_FORMATS = true;
				args[i] = UI.STR_ARG_NOSPLASH;
			}
		}
		return ret;
	}

	private void fail( Throwable throwable ){
		System.err.println();
		System.err.println( "Automated screen capture failed in series \"" + myCurrentSeries + "\"" );
		System.err.println( "**********STACK**************************" );
		if( throwable != null ) throwable.printStackTrace();
		System.err.println( "**********STACK**************************" );
		if( FLAG_EXIT ){ System.exit( 9 ); }
	}

	public void runAllSafe(){
		try{
			runAll();
		}catch( Throwable throwable ){
			fail( throwable );
		}
	}

	//+wwf +sn +ae +monitors +modes +ecp +ens +sa +rc +mpe +map +em +codebandit +pref

	public void runAll() throws Exception {
		if( shouldRun( "wwf"        ) ) workingWithFiles();
		if( shouldRun( "sn"         ) ) selectingNodes();
		if( shouldRun( "ae"         ) ) assertingEvidence();
		if( shouldRun( "monitors"   ) ) queryMonitors();
		if( shouldRun( "modes"      ) ) editQueryModes();
		if( shouldRun( "ecp"        ) ) editingConditionalProbabilities();
		if( shouldRun( "ens"        ) ) editingNetworkStructure();
		if( shouldRun( "sa"         ) ) sensitivityAnalysis();
		if( shouldRun( "rc"         ) ) recursiveConditioning();
		if( shouldRun( "mpe"        ) ) mpe();
		if( shouldRun( "map"        ) ) map();
		if( shouldRun( "em"         ) ) emLearning();
		if( shouldRun( "codebandit" ) ) codebandit();
		if( shouldRun( "pref"       ) ) preferences();
		if( FLAG_EXIT ) cleanup( "finished" );
		STREAM_VERBOSE.println( "Automated screen capture finished." );
	}

	private boolean shouldRun( String id ){
		return (mySetSelected == null) || (mySetSelected.isEmpty()) || mySetSelected.contains( id );
	}

	public void cleanup( String strNewSeries ) throws Exception {
		myCurrentSeries = strNewSeries;

		myUI.closeAllNoPrompt();

		myUI.setVisibleStatusBar(            true );
		myUI.setVisibleToolbarMain(          true );
		myUI.setVisibleToolbarInstantiation( true );

	  //Point locCurrent = java.awt.MouseInfo.getPointerInfo().getLocation();//only in 1.5
	  //mouseChoreography( locCurrent, new Point( 1,1 ) );

		clickUIHandleBar();
		mouseMoveRecorded( 1, 1 );

		myUI.setSize( new Dimension( 800, 600 ) );

		this.mynif  = null;
		this.mynd   = null;
		this.mydbn  = null;
		this.myec   = null;
		this.myetsp = null;
	  //this.myet = null;
	}

	public void clickUIHandleBar() throws Exception{
		Point locUI = myUI.getLocation();
		Dimension dimUI = myUI.getSize();
		mouseMoveRecorded( locUI.x + (dimUI.width>>1), locUI.y + 4 );
		//Thread.sleep( 1000 );
		click( 1 );
	}

	public void preferences() throws Exception {
		cleanup( "preferences" );

		//filesExist( PATHS_PREFERENCES );

		setNodeDefaultSize( 80, 40 );
		final Object waiter = new Object();

		Runnable runnable = new Runnable(){
			public void run(){
				try{
					Thread.sleep( 0x10 );

					PackageOptionsDialog dialog = null;
					long timestart = System.currentTimeMillis();

					while( (dialog = myUI.getPackageOptionsDialog()) == null ){
						if( (System.currentTimeMillis() - timestart) > 5000 ) throw new IllegalStateException( "timed out waiting for package options dialog to exist" );
						Thread.sleep( 0x200 );
					}
					while( !dialog.isVisible() ){
						if( (System.currentTimeMillis() - timestart) > 10000 ) throw new IllegalStateException( "timed out waiting for package options dialog to be visible" );
						Thread.sleep( 0x200 );
					}
					dialog.setSelected( netPrefs );
					myPause.bind( dialog.forGroup( netPrefs ) );

					DimensionPreference prefNodeSize = (DimensionPreference) mySamiamPreferences.getMappedPreference( SamiamPreferences.nodeDefaultSize );
					final WholeNumberField fieldWidth = prefNodeSize.getWidthField();

					//fieldWidth.setValue( (int)155 );
					invokeLater( new Runnable(){ public void run(){ fieldWidth.setValue( (int)155 ); } } );
					Thread.sleep( 0x100 );
					new Condition(){ public boolean unsatisfied(){ return fieldWidth.getValue() != 155; } }.spin();

					mouseOverLowerRightCorner( fieldWidth );
					click( 1 );
					fieldWidth.selectAll();

					screenshot( dialog, "pref1" );

					//if( true ) return;
					//*****************************************************

					dialog.setSelected( globalPrefs );
					myPause.bind( dialog.forGroup( globalPrefs ) );

					ObjectPreference prefLookFeel = (ObjectPreference) mySamiamPreferences.getMappedPreference( SamiamPreferences.STR_LOOKANDFEEL_CLASSNAME );
					JComboBox comboLookFeel = prefLookFeel.getJComboBox();
					ComboBoxModel comboModel = comboLookFeel.getModel();
					Object element;
					for( int i=0; i<comboModel.getSize(); i++ ){
						element = comboModel.getElementAt(i);
						if( element.toString().indexOf( "CDE" ) >= 0 ) comboLookFeel.setSelectedIndex(i);
					}
					setPopupVisible( comboLookFeel, true );
					mouseOverLowerRightCorner( comboLookFeel );

					screenshot( dialog, "pref2" );

					setPopupVisible( comboLookFeel, false );

					dialog.setVisible( false );

					synchronized( waiter ){ waiter.notifyAll(); }
				}catch( Throwable throwable ){
					fail( throwable );
				}
			}
		};
		new Thread( runnable, "ScreenCaptureAutomation preferences dialog capture thread" ).start();
		myUI.action_PREFERENCES.actionP( this );
		synchronized( waiter ){ waiter.wait(); }
	}

	/** @since 20100111 */
	private NetworkInternalFrame cram( boolean expandall ) throws Exception{
		return cram( expandall, false );
	}

	/** @since 20100111 */
	private NetworkInternalFrame cram( boolean expandall, boolean fit ) throws Exception{
		best( mynif, fit );
		if( expandall ){ myetsp.getEvidenceTree().expandAll(); }
		myUI.action_EVIDENCETREECRAM.actionP( this );
		best( mynif, fit );
		return mynif;
	}

	public void codebandit() throws Exception {
		cleanup( "code bandit" );

		filesExist( PATHS_CODEBANDIT );

		setNodeDefaultSize( 150, 55 );
		setDynamatorByName( "zc-hugin" );
		setMonitorZoom( (double)100.00 );

		waitForOpen( new File( STR_PATH_CANCER ), (long)5000 );
		boolean ictbVisible = myUI.getInstantiationToolBar().isVisible();
		myUI.setVisibleToolbarInstantiation( false );
		Thread.sleep( 0x20 );
		cram( true );

		myUI.action_CODETOOLNETWORK.actionP( this );
		CodeToolInternalFrame ctif = mynif.getCodeToolInternalFrame();
	  /*Point loc = ctif.getLocation(); loc.x = 1; ctif.setLocation( loc );*/
		CodeOptionsPanel cop = ctif.getCodeOptionsPanel();

		final JComboBox combo = (JComboBox) cop.getComponent( ModelCoder.OPTION_LIBRARY_VERSION );
		combo.setSelectedItem( ModelCoder.OPTION_LIBRARY_VERSION.BOTH );
		new Condition(){ public boolean unsatisfied(){ return combo.getSelectedItem() != ModelCoder.OPTION_LIBRARY_VERSION.BOTH; } }.spin();
		setPopupVisible( combo, true );

		ctif.setOutputFile( new File( STR_NAME_DIRECTORY_NETWORKS + "/ModelTutorial.java" ) );

		JButton buttonWrite = findButton( ctif, ctif.action_WRITECODE );
		if( buttonWrite == null ) throw new IllegalStateException( "button \"write code\" not found" );

		mouseOverLowerRightCorner( buttonWrite );

		screenshot( "codebandit1" );

		setPopupVisible( combo, false );
		Thread.sleep( 0x10 );
		myUI.setVisibleToolbarInstantiation( ictbVisible );
		Thread.sleep( 0x10 );
	}

	public static final String[] PATHS_CODEBANDIT = new String[] {
		STR_NAME_DIRECTORY_NETWORKS,
		STR_PATH_CANCER
	};

	public void emLearning() throws Exception {
		cleanup( "em learning" );

		filesExist( PATHS_EM );

		setNodeDefaultSize( 135, 50 );
		setDynamatorByName( "recursive conditioning" );

		waitForOpen( new File( STR_NAME_DIRECTORY_NETWORKS + "/barley.net" ), (long)7000 );
		best( mynif, false );

		queryMode( 60000 );

		JOptionResizeHelper.JOptionResizeHelperListener listener = new JOptionResizeHelper.JOptionResizeHelperListener(){
			public void topLevelAncestorDialog( JDialog container, JOptionResizeHelper helper ){
				try{
					EMLearningDlg learnDlg = (EMLearningDlg) container;

					learnDlg.setDataFile( new File( STR_NAME_DIRECTORY_NETWORKS + "/barley_em.dat" ) );

					JButton buttonLearn = findButton( learnDlg, EMLearningDlg.STR_TEXT_BUTTON_LEARN );
					if( buttonLearn == null ) throw new IllegalStateException( "button \"Learn\" not found" );

					myPause.bind( buttonLearn );

					mouseOverLowerRightCorner( buttonLearn );

					screenshot( "em1" );

					container.setVisible( false );
				}catch( Throwable throwable ){
					fail( throwable );
				}
			}
		};

		mynif.emTool( listener );
	}

	public static final String[] PATHS_EM = new String[] {
		STR_NAME_DIRECTORY_NETWORKS,
		STR_NAME_DIRECTORY_NETWORKS + "/barley.net"
	};

	public void map() throws Exception {
		cleanup( "map" );

		filesExist( PATHS_MAP );

		setNodeDefaultSize( 150, 55 );
		setPreEShown( true );
		setDynamatorByName( "shenoy-shafer" );
		setMonitorZoom( (double)100.00 );

		waitForOpen( new File( STR_NAME_DIRECTORY_NETWORKS + "/Water.hugin" ), (long)5000 );
		cram( false, true );

		DisplayableFiniteVariable varCBODD1230 = (DisplayableFiniteVariable) mydbn.forID( "CBODD_12_30" );
		DisplayableFiniteVariable varCNOD1230 = (DisplayableFiniteVariable) mydbn.forID( "CNOD_12_30" );

		observe( varCBODD1230, "15_MG/L" );
		observe( varCNOD1230, "1_MG/L" );

		Set<DisplayableFiniteVariable> evidence = new HashSet<DisplayableFiniteVariable>(2);
		evidence.add( varCBODD1230 );
		evidence.add( varCNOD1230 );
		EvidenceTree tree = myetsp.getEvidenceTree();
		tree.setExpandedSet( evidence );
		Dimension sizeTree = tree.getSize();
		Dimension sizePain = myetsp.getSize();

		int yCoord = Math.max( 0, (sizeTree.height-sizePain.height)/2 );

		Rectangle rect = new Rectangle( /*x*/0,/*y*/yCoord,/*w*/32,/*h*/sizePain.height-16 );
		myetsp.getJScrollPane().getViewport().scrollRectToVisible( rect );

		mouseOverToolBarForAction( myUI.action_MAP );

		Thread.sleep( 850 );//wait for tooltip
		screenshot( "map1" );

		//if( true ) return;
		//*****************************************************

		setMAPProperty( "CBODN_12_45", true );
		setMAPProperty( "CNOD_12_00", true );
		setMAPProperty( "CNOD_12_45", true );
		setMAPProperty( "CNON_12_15", true );
		setMAPProperty( "CNON_12_30", true );

		myUI.action_MAP.actionP( this );
		MAPInternalFrame mapif = mynif.getMAPInternalFrame();
		InputPanel inputpanel  = mapif.getInputPanel();

		inputpanel.setExact( true );
		inputpanel.doMAP();

		JButton                btnCopy = findButton( mapif, "Copy" );
		if( btnCopy == null ){ btnCopy = findButton( mapif, "Text" ); }
		if( btnCopy == null ){ throw new IllegalArgumentException( "button \"Copy\" or \"Text\" not found" ); }
		mouseOverLowerRightCorner( btnCopy );

		Thread.sleep( 650 );//wait for tooltip
		screenshot( "map2" );
	}

	public void setMAPProperty( String id, boolean flag ) throws Exception {
		DisplayableFiniteVariable dfv = (DisplayableFiniteVariable) mydbn.forID( id );
		edu.ucla.util.EnumValue val = edu.ucla.belief.inference.map.MapProperty.PROPERTY.valueOf( flag );
		dfv.setProperty( edu.ucla.belief.inference.map.MapProperty.PROPERTY, val );
	}

	public void observe( DisplayableFiniteVariable var, String instance ) throws Exception {
		Thread.sleep( 0x100 );
		myec.observe( var, var.instance( var.index( instance ) ) );
	}

	public static final String[] PATHS_MAP = new String[] {
		STR_NAME_DIRECTORY_NETWORKS,
		STR_NAME_DIRECTORY_NETWORKS + "/Water.hugin"
	};

	public void mpe() throws Exception {
		cleanup( "mpe" );

		filesExist( PATHS_MPE );

		setNodeDefaultSize( 135, 50 );
		setDynamatorByName( "shenoy-shafer" );

		waitForOpen( new File( STR_NAME_DIRECTORY_NETWORKS + "/barley.net" ), (long)10000 );
		cram( false, true );

		DisplayableFiniteVariable varAvailableN = (DisplayableFiniteVariable) mydbn.forID( "ntilg" );

		if( varAvailableN == null ) throw new IllegalStateException( "var \"ntilg\" not found" );

		observe( varAvailableN, "x35_150" );
		myetsp.getEvidenceTree().setExpandedSet( Collections.singleton( varAvailableN ) );

		queryMode( (long)60000 );

		mouseOverToolBarForAction( myUI.action_MPE );

		Thread.sleep( 850 );//wait for tooltip
		screenshot( "mpe1" );

		//if( true ) return;
		//*****************************************************

		myUI.action_MPE.actionP( this );

		MPEInternalFrame mpeif = mynif.getMPEInternalFrame();

		mpeif.setLocation( new Point( mpeif.getLocation().x, 8 ) );

		Rectangle bounds = mpeif.getBounds();
		Point loc = bounds.getLocation();
		SwingUtilities.convertPointToScreen( loc, mpeif.getParent() );
		mouseMoveRecorded( loc.x + ((bounds.width*7)/8), loc.y + (bounds.height/2) );
		myRobot.mousePress( InputEvent.BUTTON3_MASK );
		myRobot.mouseRelease( InputEvent.BUTTON3_MASK );

		Thread.sleep( 300 );
		screenshot( "mpe2" );

		mouseMoveRecorded( loc.x, loc.y );
		click( 1 );
	}

	public static final String[] PATHS_MPE = new String[] {
		STR_NAME_DIRECTORY_NETWORKS,
		STR_NAME_DIRECTORY_NETWORKS + "/barley.net"
	};

	public void recursiveConditioning() throws Exception {
		cleanup( "recursive conditioning" );

		filesExist( PATHS_RC );

		setNodeDefaultSize( 150, 55 );
		setDynamatorByName( "recursive conditioning" );

		waitForOpen( new File( STR_NAME_DIRECTORY_NETWORKS + "/barley.net" ), (long)10000 );
		cram( false, true );

		DisplayableFiniteVariable varAvailableN = (DisplayableFiniteVariable) mydbn.forID( "ntilg" );

		if( varAvailableN == null ) throw new IllegalStateException( "var \"ntilg\" not found" );

		observe( varAvailableN, "x35_150" );
		myetsp.getEvidenceTree().setExpandedSet( Collections.singleton( varAvailableN ) );

		new Condition(){ public boolean unsatisfied(){ return ! myUI.action_RC.isEnabled(); } }.spin();
		myUI.action_RC.actionP( this );

		edu.ucla.belief.ui.rc.RecursiveConditioningInternalFrame rcif = mynif.getRCInternalFrame();
		edu.ucla.belief.ui.rc.RCPanel rcpanel = rcif.getSubComponent();

		RCSettings rcsettings = rcpanel.getSettings();
		rcsettings.setUserMemoryProportion( (double)0.55 );
		rcpanel.resetSlider();

		rcpanel.allocateMemory();

		final Object wait = new Object();
		edu.ucla.belief.recursiveconditioning.RC.RecCondThreadListener listener =
			new edu.ucla.belief.recursiveconditioning.RC.RecCondThreadListener(){
				public void rcFinished( double result, long time_ms){
					try{
						myFlagComputationFinished = true;
						screenshot( "rc1" );
						synchronized( wait ){ wait.notifyAll(); }
					}catch( Throwable throwable ){
						fail( throwable );
					}
				}
				public void rcFinishedMPE( double result, Map instantiation, long time_ms){
					fail( new Exception( "rcFinishedMPE() should never happen" ) );
				}
				public void rcComputationError( String msg ){
					fail( new Exception( msg ) );
				}
		};
		rcpanel.addListener( listener );

		JComponent pnlResults = rcpanel.getPanelResults();
		JButton buttonRun = findButton( pnlResults, edu.ucla.belief.ui.rc.RCPanel.STR_TEXT_BUTTON_RUN_RC );
		if( buttonRun == null ) throw new IllegalStateException( "run button not found" );

		mouseOverLowerRightCorner( buttonRun );

		myFlagComputationFinished = false;
		rcpanel.safeRunRecursiveConditioning();
		synchronized( wait ){
			wait.wait( (long)60000 );
			if( !myFlagComputationFinished ) throw new IllegalStateException( "rc timed out" );
		}

		//if( true ) return;
		//*****************************************************

		if( !rcpanel.setTabSelectedForTitle( edu.ucla.belief.ui.rc.RCPanel.STR_TAB_DGRAPH_SETTINGS ) ){
			throw new IllegalStateException( "failed to select dgraph tab" );
		}

		JComponent paneldg = rcpanel.getPanelDgraphSettings();
		Collection<JComboBox> combos = getDescendantsOfType( paneldg, JComboBox.class, new LinkedList<JComboBox>() );
		if( combos.size() != 1 ) throw new IllegalStateException( "heuristic combo box not found" );
		JComboBox combo = (JComboBox) combos.iterator().next();

		JButton buttonGenerate = findButton( paneldg, edu.ucla.belief.ui.rc.RCPanel.STR_TEXT_BUTTON_GENERATE_DGRAPH );
		if( buttonGenerate == null ) throw new IllegalStateException( "generate button not found" );

		setPopupVisible( combo, true );
		mouseOverLowerRightCorner( buttonGenerate );

		screenshot( "rc2" );

		Thread.sleep( 0x10 );
		mouseOverLowerRightCorner( paneldg );
		Thread.sleep( 0x10 );
		mouseOverLowerRightCorner( combo );
		Thread.sleep( 0x10 );
		setPopupVisible( combo, false );
		paneldg.repaint();
		Thread.sleep( 0x10 );
	}

	private JButton findButton( Container container, String hotText ){
		Collection<JButton> buttons = getDescendantsOfType( container, JButton.class, new LinkedList<JButton>() );
		JButton ret = null;
		for( JButton buttonNext : buttons ){
			if( hotText.equals( buttonNext.getText() ) ) ret = buttonNext;
		}
		return ret;
	}

	private JButton findButton( Container container, Action action ){
		Collection<JButton> buttons = getDescendantsOfType( container, JButton.class, new LinkedList<JButton>() );
		JButton ret = null;
		for( JButton buttonNext : buttons ){
			if( buttonNext.getAction() == action ) ret = buttonNext;
		}
		return ret;
	}

	public static final String[] PATHS_RC = new String[] {
		STR_NAME_DIRECTORY_NETWORKS,
		STR_NAME_DIRECTORY_NETWORKS + "/barley.net"
	};

	/** @since 20100110 */
	public JComboBox setPopupVisible( final JComboBox box, final boolean visible ) throws Exception{
		new Condition(){
			public void later(){ box.setPopupVisible( visible ); }
			public boolean unsatisfied(){ return box.isPopupVisible() != visible; } }.spin();
		return box;
	}

	public void sensitivityAnalysis() throws Exception {
		cleanup( "sensitivity analysis" );

		filesExist( PATHS_SENSITIVITY );

		setNodeDefaultSize( 150, 55 );
		setDynamatorByName( "shenoy-shafer" );

		waitForOpen( new File( STR_NAME_DIRECTORY_NETWORKS + "/pregnancy1.net" ), (long)5000 );
		boolean ictbVisible = myUI.getInstantiationToolBar().isVisible();
		myUI.setVisibleToolbarInstantiation( false );
		Thread.sleep( 0x20 );
		cram( false );

		final DisplayableFiniteVariable varPregnancy = (DisplayableFiniteVariable) mydbn.forID( "Pr" );
		final DisplayableFiniteVariable varBlood = (DisplayableFiniteVariable) mydbn.forID( "BT" );
		final DisplayableFiniteVariable varScan = (DisplayableFiniteVariable) mydbn.forID( "Sc" );

		observe( varBlood, "negative" );
		observe( varScan, "negative" );

		queryMode( 5000 );

		varPregnancy.getNodeLabel().setSelected( true );

		EvidenceTree et = myetsp.getEvidenceTree();
		et.expandAll();

		mouseOverToolBarForAction( myUI.action_SENSITIVITY );

		Thread.sleep( 850 );//wait for tooltip
		screenshot( "sa1" );

		//if( true ) return;
		//*****************************************************

		//myUI.action_SENSITIVITY.actionP( this );
		mynif.sensitivityTool( false );
		final SensitivityInternalFrame sif = mynif.getSensitivityInternalFrame();

		sif.setEvent2( false );
		ChooseInstancePanel2 py = sif.getPanelY();

		py.setSelectedVariable( varPregnancy );
		Object instance = varPregnancy.instance( varPregnancy.index( "yes" ) );
		if( instance == null ) throw new IllegalStateException( "instance \"yes\" not found for variable " + varPregnancy.getID() );
		py.setInstance( instance );

		sif.setOperatorComparison( SensitivityEngine.OPERATOR_LTE );
		sif.setConstValue( (double)0.08 );

		JComboBox comboComparison = sif.getComboComparison();
		Point lowerright = mouseOverLowerRightCorner( comboComparison );
		setPopupVisible( comboComparison, true );

		screenshot( "sa2" );

		Thread.sleep( 0x10 );
		mouseOverLowerRightCorner( py );
		Thread.sleep( 0x10 );
		mouseOverLowerRightCorner( comboComparison );
		Thread.sleep( 0x10 );
		setPopupVisible( comboComparison, false );
		py.repaint();
		Thread.sleep( 0x10 );

		//if( true ) return;
		//*****************************************************

		if( !sif.action_START.isEnabled() ) throw new IllegalStateException( "expected sensitivity \"start\" enabled" );
		sif.action_START.actionP( this );

		SensitivitySuggestionTable sst = sif.getSensitivitySuggestionTable();
		SensitivitySuggestion suggestion;
		int rowIndexFound = (int)-1;
		for( int i=0; i<sst.getRowCount(); i++ ){
			suggestion = sst.getSuggestion( i );
			if( suggestion.getVariable() == varScan ) rowIndexFound = i;
		}

		if( rowIndexFound < 0 ) throw new IllegalStateException( "suggestion for \"Scan\" not found" );

		sif.action_TOGGLETABLEDETAILS.actionPerformed( true );

		sst.addRowSelectionInterval( rowIndexFound, rowIndexFound );

		Rectangle bounds = sst.getCellRect( rowIndexFound, 0, true );
		Point loc = bounds.getLocation();
		SwingUtilities.convertPointToScreen( loc, sst );
		mouseMoveRecorded( loc.x + ((bounds.width*7)/8), loc.y + ((bounds.height*7)/8) );

		screenshot( "sa3" );

		//if( true ) return;
		//*****************************************************

		sif.setMaximum( false );

		Dimension sizeDesktop = mynif.getRightHandDesktopPane().getSize();
		Dimension sizeNetwork = mynd.getNetworkSize( new Dimension() );
		Point locIF = new Point( 0, sizeNetwork.height + 32 );
		Dimension dimIF = new Dimension( sizeDesktop.width, sizeDesktop.height - locIF.y );

		sif.setSize( dimIF );
		sif.setLocation( locIF );

		myUI.action_SHOWSELECTED.actionP( this );
		Monitor monitorPr = varPregnancy.getNodeLabel().getEvidenceDialog();
		monitorPr.translateActualLocation( -64, 0 );
		monitorPr.confirmActualLocation();

		new Condition( 0x20 ){ public boolean unsatisfied(){ return ! sif.action_ADOPTCHANGE.isEnabled(); } }.spin();
		sif.action_ADOPTCHANGE.actionP( this );

		mouseOverLowerRightCorner( sif.getMenuAdopt() );

		Thread.sleep( 200 );
		screenshot( "sa4" );

		Thread.sleep( 0x10 );
		myUI.setVisibleToolbarInstantiation( ictbVisible );
		Thread.sleep( 0x10 );
	}

	public static final String[] PATHS_SENSITIVITY = new String[] {
		STR_NAME_DIRECTORY_NETWORKS,
		STR_NAME_DIRECTORY_NETWORKS + "/pregnancy1.net"
	};

	/** @since 20100108 */
	public abstract class Condition implements Runnable{ abstract public boolean unsatisfied();
		public Condition(){ this(8); }
		public Condition( int reps ){ this.reps = reps; }
		public Condition name( String name ){ this.name = name; return this; }
		public void run(){ later(); }
		public void later(){}
		public boolean spin() throws Exception {
			SwingUtilities.invokeLater( Condition.this );
			int i=0;
			for( i=0; unsatisfied() && (i<reps); i++ ){ Thread.sleep( 0x40 ); }
			if( name != null ){ System.out.printf( "%-16s x%d\n", name, i ); }
			return      ! unsatisfied();
		}
		private int reps = 8;
		private String name;
	}

	public void editingNetworkStructure() throws Exception {
		cleanup( "editing network structure" );

		filesExist( PATHS_ENS );

		setNodeDefaultSize( 150, 55 );
		setDynamatorByName( "recursive conditioning" );

		waitForOpen( new File( STR_PATH_CANCER ), (long)5000 );
		cram( false );

		if( !mouseOverToolBarForAction( myUI.action_ADDNODE ) ) throw new IllegalStateException( "failed to mouse over toolbar add node" );

		new Condition(){ public boolean unsatisfied(){ return ! myUI.action_ADDNODE.isEnabled(); } }.spin();
		myUI.action_ADDNODE.actionP( this );

		Thread.sleep( 850 );//wait for tooltip
		screenshot( "ens1" );

		//if( true ) return;
		//*****************************************************

		final DisplayableFiniteVariable varC = (DisplayableFiniteVariable) mydbn.forID( "C" );
		NodeLabel labelC = varC.getNodeLabel();
		Point loc = labelC.getActualLocation( new Point() );
		SwingUtilities.convertPointToScreen( loc, labelC.getParent() );

		mouseMoveRecorded( loc.x + 156, loc.y - 55 );
		click( 1 );
		Thread.sleep( 100 );

		final DisplayableFiniteVariable varNew = (DisplayableFiniteVariable) mydbn.forID( "variable0" );
		varNew.setID( "newnode" );
		varNew.setLabel( "NEW NODE" );
		new Condition(){ public boolean unsatisfied(){ return varNew.getNodeLabel() == null; } }.spin();
		NodeLabel labelNew = varNew.getNodeLabel();
		labelNew.setSelected( true );
		EvidenceTree et = myetsp.getEvidenceTree();
		et.setExpandedSet( Collections.singleton( varNew ) );
		varNew.changeDisplayText();

		final DisplayableFiniteVariable varA = (DisplayableFiniteVariable) mydbn.forID( "A" );
		mynd.addEdge( varA, varNew );

		if( !mouseOverToolBarForAction( myUI.action_ADDEDGE ) ) throw new IllegalStateException( "failed to mouse over toolbar add edge" );

		Thread.sleep( 100 );
		screenshot( "ens2" );

		labelNew.setSelected( false );

		//if( true ) return;
		//*****************************************************

		final DisplayableFiniteVariable varToDelete = (DisplayableFiniteVariable) mydbn.forID( "D" );
		NodeLabel labelToDelete = varToDelete.getNodeLabel();
		labelToDelete.setSelected( true );

		JPopupMenu popup = nodeLabelPopup( labelToDelete, 3 );

		screenshot( "ens3" );

		popup.setVisible( false );

		//if( true ) return;
		//*****************************************************

		mynd.deleteNodes( Collections.singleton( varToDelete ), /*force*/true );

		screenshot( "ens4" );

		//if( true ) return;
		//*****************************************************

		mynd.nodeSelectionClearAll();

		final DisplayableFiniteVariable varB = (DisplayableFiniteVariable) mydbn.forID( "B" );
		varA.getNodeLabel().setSelected( true );
		varB.getNodeLabel().setSelected( true );
		varC.getNodeLabel().setSelected( true );

		new Condition(){ public boolean unsatisfied(){ return ! myUI.action_COPY.isEnabled(); } }.spin();
		myUI.action_COPY.actionP( this );
		new Condition(){ public boolean unsatisfied(){ return ! myUI.action_PASTE.isEnabled(); } }.spin();
		myUI.action_PASTE.actionP( this );

		Point locA = varA.getNodeLabel().getActualLocation( new Point() );
		final DisplayableFiniteVariable varE = (DisplayableFiniteVariable) mydbn.forID( "E" );
		Point locE = varE.getNodeLabel().getActualLocation( new Point() );
		SwingUtilities.convertPointToScreen( locA, varA.getNodeLabel().getParent() );
		SwingUtilities.convertPointToScreen( locE, varE.getNodeLabel().getParent() );

		mouseMoveRecorded( locA.x, locE.y );
		click( 1 );

		Thread.sleep( 0x40 );
		mynd.nodeSelectionClearAll();
		new Condition(){ public boolean unsatisfied(){ return mydbn.forID( "A_paste0" ) == null; } }.spin();
		final DisplayableFiniteVariable varA_pasted = (DisplayableFiniteVariable) mydbn.forID( "A_paste0" );
		final DisplayableFiniteVariable varB_pasted = (DisplayableFiniteVariable) mydbn.forID( "B_paste0" );
		final DisplayableFiniteVariable varC_pasted = (DisplayableFiniteVariable) mydbn.forID( "C_paste0" );
		if( (varA_pasted == null) || (varB_pasted == null) || (varC_pasted == null) ) throw new IllegalStateException( "failed to find pasted nodes" );
		new Condition(){ public boolean unsatisfied(){ return varA_pasted.getNodeLabel() == null; } }.spin();
		varA_pasted.getNodeLabel().setSelected( true );
		varB_pasted.getNodeLabel().setSelected( true );
		varC_pasted.getNodeLabel().setSelected( true );

		mouseOverMenuItem( "Edit", "Paste" );

		screenshot( "ens5" );

		clickUIHandleBar();

		//if( true ) return;
		//*****************************************************

		final JComponent compPastePrompt = myUI.getNetworkClipBoard().getPromptPastePanel();
		JOptionResizeHelper.JOptionResizeHelperListener jorhl = new JOptionResizeHelper.JOptionResizeHelperListener(){
			public void topLevelAncestorDialog( JDialog container, JOptionResizeHelper helper ){
				try{
					myPause.bind( compPastePrompt );

					Collection<JCheckBox> checkboxes = getDescendantsOfType( compPastePrompt, JCheckBox.class, new LinkedList<JCheckBox>() );
					if( checkboxes.isEmpty() ) throw new IllegalStateException( "check boxes not found" );

					JCheckBox found = null;
					for( JCheckBox cb : checkboxes ){
						if( cb.getText().equals( "Paste edges" ) ) found = cb;
					}

					if( found == null ) throw new IllegalStateException( "check box \"Paste edges\" not found" );

					if( !found.isSelected() ) found.doClick();
					mouseOverLowerRightCorner( found );

					screenshot( "ens6" );

					container.setVisible( false );
				}catch( Throwable throwable ){
					fail( throwable );
				}
			}
		};
		JOptionResizeHelper helper = new JOptionResizeHelper( compPastePrompt, true, (long)10000 );
		helper.setListener( jorhl );
		helper.start();
		//myUI.action_PASTESPECIAL.actionP( this );
		myUI.doPasteSubnetwork( new Point( locA.x, locE.y ), mynif, /*special*/true );

		//if( true ) return;
		//*****************************************************

		NetworkInformation networkinformation = myUI.getNetworkInformation();
		JOptionResizeHelper.JOptionResizeHelperListener jorhlNI = new JOptionResizeHelper.JOptionResizeHelperListener(){
			public void topLevelAncestorDialog( JDialog container, JOptionResizeHelper helper ){
				try{
					Collection<JButton> buttons = getDescendantsOfType( container, JButton.class, new LinkedList<JButton>() );
					if( buttons.isEmpty() ) throw new IllegalStateException( "failed network information find buttons" );

					final JButton firstButton = (JButton) buttons.iterator().next();
					myPause.bind( firstButton );

					mouseOverLowerRightCorner( firstButton );

					screenshot( mynd, "ens7" );

					container.setVisible( false );
					Thread.sleep( 0x40 );
					new Condition(){ public boolean unsatisfied(){ return firstButton.isDisplayable(); } }.spin();
				}catch( Throwable throwable ){
					fail( throwable );
				}
			}
		};
		networkinformation.showDialog( mynd, mydbn, jorhlNI );

		//if( true ) return;
		//*****************************************************

		mynd.setZoomFactor( (double)1.5 );

		JComboBox zoombox = myUI.getMainToolBar().getZoomBox();
		setPopupVisible( zoombox, true );
		Point lowerright = mouseOverLowerRightCorner( zoombox );
		Point destiny = new Point( lowerright.x, lowerright.y + 50 );
		mouseChoreography( lowerright, destiny );

		screenshot( "ens8" );

		setPopupVisible( zoombox, false );
	}

	private Point mouseOverLowerRightCorner( Component comp ){
		Point loc = comp.getLocation();
		Dimension dim = comp.getSize();
		SwingUtilities.convertPointToScreen( loc, comp.getParent() );
		Point ret = new Point( loc.x + (dim.width-8), loc.y + (dim.height-8) );
		mouseMoveRecorded( ret.x, ret.y );
		return ret;
	}

	private Point mouseOverRelative( Component comp, float xfactor, float yfactor ){
		Point loc = comp.getLocation();
		Dimension dim = comp.getSize();
		SwingUtilities.convertPointToScreen( loc, comp.getParent() );
		Point ret = new Point( loc.x + ((int)(((float)dim.width)*xfactor)), loc.y + ((int)(((float)dim.height)*yfactor)) );
		mouseMoveRecorded( ret.x, ret.y );
		return ret;
	}

	private void mouseOverMenuItem( String nameMenu, String nameItem ){
		JMenuBar menubar = myUI.getJMenuBar();
		JMenu menu;
		for( int i=0; i<menubar.getMenuCount(); i++ ){
			menu = menubar.getMenu(i);
			if( nameMenu.equals( menu.getText() ) ){
				if( mouseOverMenuItem( menu, nameItem ) ) return;
			}
		}
		throw new IllegalStateException( "failed to mouse over item " + nameItem + " in menu " + nameMenu );
	}

	private boolean mouseOverMenuItem( JMenu menu, String nameItem ){
		JMenuItem item;
		for( int j=0; j<menu.getItemCount(); j++ ){
			item = menu.getItem( j );
			if( (item != null) && (nameItem.equals( item.getText() )) ){
				if( mouseOverMenuItem( menu, item ) ) return true;
			}
		}
		return false;
	}

	private boolean mouseOverMenuItem( JMenu menu, JMenuItem item ){
		//menu.setPopupMenuVisible( true );
		menu.doClick();
		mouseOverLowerRightCorner( item );
		return true;
	}

	public static final String[] PATHS_ENS = new String[] {
		STR_NAME_DIRECTORY_NETWORKS,
		STR_PATH_CANCER
	};

	private int indexLastRow;

	public void editingConditionalProbabilities() throws Exception {
		cleanup( "editing conditional probabilities" );

		filesExist( PATHS_ECP );

		myUI.setSize( new Dimension( 920, 600 ) );

		setNodeDefaultSize( 150, 55 );
		setDynamatorByName( "zc-hugin" );

		waitForOpen( new File( STR_PATH_CANCER ), (long)5000 );

		mynif.setMaximum( true );
		packNetworkDisplay();

		final DisplayableFiniteVariable varA = (DisplayableFiniteVariable) mydbn.forID( "A" );
		varA.getNodeLabel().setSelected( true );

		JOptionResizeHelper.JOptionResizeHelperListener jorhlStates = new JOptionResizeHelper.JOptionResizeHelperListener(){
			public void topLevelAncestorDialog( JDialog container, JOptionResizeHelper helper ){
				try{
					final NodePropertiesPanel npp = varA.getGUI();
					myPause.bind( npp );

					Util.centerWindow( container, Util.convertBoundsToScreen( mynif ) );

					final String value = "new_state";
					final JTable table = npp.getTableStates();
					indexLastRow       = table.getRowCount() - 1;
					new Condition(){ public void later(){
						table.addRowSelectionInterval( indexLastRow, indexLastRow );

						npp.doInsertState();

						table.removeRowSelectionInterval( indexLastRow, indexLastRow );
						++indexLastRow;
						table.addRowSelectionInterval( indexLastRow, indexLastRow );

						table.setValueAt( value, indexLastRow, 0 );
					} public boolean unsatisfied(){ return table.getValueAt( indexLastRow, 0 ) != value; } }.spin();
					new Condition(){ public void later(){ table.editCellAt( indexLastRow, 0 );
					} public boolean unsatisfied(){ return ! table.isEditing(); } }.spin();

					TableCellEditor editor = table.getCellEditor( indexLastRow, 0 );
					final JTextField tf = (JTextField) ((DefaultCellEditor)editor).getComponent();

					table.scrollRectToVisible( new Rectangle( 0, table.getHeight() - 1, 1, 1 ) );
					table.repaint();
					Thread.sleep( 0x10 );

					Point loc = tf.getLocation();
					Dimension dim = tf.getSize();
					SwingUtilities.convertPointToScreen( loc, tf.getParent() );
					mouseMoveRecorded( loc.x + (dim.width/2), loc.y + ((dim.height*7)/8) );
					click( 1 );
				  //new Condition(){ public boolean unsatisfied(){ return tf.getCaretPosition() < 9; } }.name( "tf caret" ).spin();
					new Condition( 0x10 ){ public boolean unsatisfied(){ return ! tf.isValid(); } }/*.name( "tf valid 000" )*/.spin();
					new Condition(){ public void later(){ tf.selectAll(); } public boolean unsatisfied(){ return tf.getSelectionEnd() < 9; } }/*.name( "tf sel end" )*/.spin();
					tf.revalidate();
					new Condition( 0x10 ){ public boolean unsatisfied(){ return ! tf.isValid(); } }/*.name( "tf valid 001" )*/.spin();

				  //System.out.println( table.getParent().getClass().getName() + " 7 " + ((JViewport) table.getParent()).getViewPosition() );

					Thread.sleep( 0x10 );

					screenshot( mynif, "ecp1" );//mynd, "ecp1" );

					container.setVisible( false );
				}catch( Throwable throwable ){
					fail( throwable );
				}
			}
		};
		varA.showNodePropertiesDialog( mynd, jorhlStates );

		varA.getNodeLabel().setSelected( false );

		//if( true ) return;
		//*****************************************************

		final DisplayableFiniteVariableImpl varD = (DisplayableFiniteVariableImpl) mydbn.forID( "D" );
		varD.getNodeLabel().setSelected( true );

		JOptionResizeHelper.JOptionResizeHelperListener jorhlProbs = new JOptionResizeHelper.JOptionResizeHelperListener(){
			public void topLevelAncestorDialog( JDialog container, JOptionResizeHelper helper ){
				try{
					JComponent pEditComp = varD.getProbabilityEditComponent();
					myPause.bind( varD.getGUI() );//pEditComp );

					Util.centerWindow( container, Util.convertBoundsToScreen( mynif ) );

					Collection<JTable> jtables = getDescendantsOfType( pEditComp, JTable.class, new LinkedList<JTable>() );
					if( jtables.isEmpty() ) throw new IllegalStateException( "probability edit table not found" );

					JTable foont = null;
					for( JTable jtable : jtables ){
						if( foont == null ){ foont = jtable; }
						else if( jtable.getColumnCount() > foont.getColumnCount() ){ foont = jtable; }
					}
					final JTable found = foont;

					final int row    = 1;
					final int column = 2;
					final Double value = new Double( 0.00000056 );

					new Condition(){ public void later(){
						found.setValueAt( value, row, column );
					} public boolean unsatisfied(){ return found.getValueAt( row, column ) != value; } }.spin();

					new Condition(){ public void later(){ found.editCellAt( row, column );
					} public boolean unsatisfied(){ return ! found.isEditing(); } }.spin();
					//TableCellEditor editor = found.getCellEditor( row, column );
					//JTextField tf = (JTextField) ((DefaultCellEditor)editor).getComponent();
					//Point loc = tf.getLocation();
					//Dimension dim = tf.getSize();
					//SwingUtilities.convertPointToScreen( loc, tf.getParent() );

					Rectangle rect = found.getCellRect( row, column, true );
					//System.out.println( "rect   ? " + rect );
					Point loc = rect.getLocation();
					SwingUtilities.convertPointToScreen( loc, found );
					//System.out.println( "loc    ? " + loc );
					Dimension dim = rect.getSize();
					Point locNext = found.getCellRect( row, column+1, true ).getLocation();
					SwingUtilities.convertPointToScreen( locNext, found );
					//System.out.println( "locNext? " + locNext );
					mouseMoveRecorded( loc.x + ((locNext.x - loc.x)/2), loc.y + (dim.height/2) );
					click( 2 );
					//tf.selectAll();

					//tf.getSize( dim );
					//mouseMoveRecorded( loc.x + ((dim.width*3)/4), loc.y + ((dim.height*7)/8) );

					Thread.sleep( 0x80 );

					screenshot( mynif, "ecp2" );//mynd, "ecp2" );

					container.setVisible( false );
				}catch( Throwable throwable ){
					fail( throwable );
				}
			}
		};
		varD.showNodePropertiesDialog( mynd, /*showprobs*/true, jorhlProbs );
	}

	public void click( int num ) throws Exception{
		for( int i=0; i<num; i++ ){
			myRobot.mousePress( InputEvent.BUTTON1_MASK );
			Thread.sleep( 8 );
			myRobot.mouseRelease( InputEvent.BUTTON1_MASK );
		}
	}

	public static final String[] PATHS_ECP = new String[] {
		STR_NAME_DIRECTORY_NETWORKS,
		STR_PATH_CANCER
	};

	public void editQueryModes() throws Exception {
		cleanup( "edit/query modes" );

		filesExist( PATHS_MODES );

		setNodeDefaultSize( 150, 55 );
		setPreEShown( true );
		setDynamatorByName( "recursive conditioning" );
		setMonitorZoom( (double)100.00 );

		waitForOpen( new File( STR_PATH_CANCER ), (long)5000 );
		cram( false );

		DisplayableFiniteVariable varC = (DisplayableFiniteVariable) mydbn.forID( "C" );
		NodeLabel nlC = varC.getNodeLabel();
		nlC.translateActualLocation( -90, 0 );
		nlC.confirmActualLocation();

		Dimension sizeNetwork = mynd.getNetworkSize( new Dimension() );
		Point placement = new Point( (sizeNetwork.width*2)/3, sizeNetwork.height/4 );
		DisplayableFiniteVariable nodeNew = mynd.createNewNode( placement, "newnode", "NEW NODE" );
		mynd.addNewNode( nodeNew );

		EvidenceTree et = myetsp.getEvidenceTree();
		et.setExpandedSet( Collections.singleton( nodeNew ) );

		NodeLabel nl = nodeNew.getNodeLabel();
		nl.setSelected( true );

		Point loc = nl.getActualLocation( new Point() );
		SwingUtilities.convertPointToScreen( loc, nl.getParent() );
		mouseMoveRecorded( loc.x + 76, loc.y + 42 );
		Thread.sleep( 100 );

		screenshot( "modes1" );

		//if( true ) return;
		//*****************************************************

		queryMode( (long)5000 );

		boolean mouseOverSuccess = false;
		mouseOverSuccess = mouseOverToolBarForAction( myUI.action_SHOWALL );
		if( !mouseOverSuccess ) mouseOverSuccess = mouseOverToolBarForAction( myUI.action_SHOWSELECTED );
		if( !mouseOverSuccess ) throw new IllegalStateException( "failed to mouse over toolbar button for show monitors" );

		myUI.action_SHOWALL.actionP( this );

		for( DFVIterator it = mydbn.dfvIterator(); it.hasNext(); ){
			offsetMonitor( it.nextDFV(), (float)0.5, (float)0.85 );
		}
		DisplayableFiniteVariable varE = (DisplayableFiniteVariable) mydbn.forID( "E" );
		offsetMonitor( varE, (float)0.1, (float)0.85 );

		myetsp.getEvidenceTree().setExpandVariableBranches( true );

		screenshot( "modes2" );
	}

	public static final String[] PATHS_MODES = new String[] {
		STR_NAME_DIRECTORY_NETWORKS,
		STR_PATH_CANCER
	};

	public boolean mouseOverToolBarForAction( Action action ){
		JButton button = myUI.getMainToolBar().forAction( action );
		if( button == null ) button = myUI.getInstantiationToolBar().forAction( action );
		if( button == null ) return false;
		else{
			mouseOverLowerRightCorner( button );
			return true;
		}
	}

	public void queryMonitors() throws Exception {
		cleanup( "query monitors" );

		filesExist( PATHS_QM );

		setNodeDefaultSize( 150, 55 );
		setPreEShown( true );
		setDynamatorByName( "shenoy-shafer" );
		setMonitorZoom( (double)300.00 );

		waitForOpen( new File( STR_NAME_DIRECTORY_NETWORKS + "/twin.net" ), (long)5000 );

		DisplayableFiniteVariable var0 = (DisplayableFiniteVariable) mydbn.forID( "variable0" );

		packNetworkDisplay();

		queryMode( (long)10000 );

		//Thread.sleep( 1000 );

		mynd.setSelected( true );

		JPopupMenu popup = nodeLabelPopup( var0.getNodeLabel(), 1 );

		screenshot( mynd, "monitors1" );

		popup.setVisible( false );

		//if( true ) return;
		//*****************************************************

		cleanup( "query monitors" );
		waitForOpen( new File( STR_NAME_DIRECTORY_NETWORKS + "/Water.hugin" ), (long)5000 );
		best( mynif, true );//myUI.action_BESTWINDOWARRANGEMENT.actionP( this );

		DisplayableFiniteVariable varCBODD1215 = (DisplayableFiniteVariable) mydbn.forID( "CBODD_12_15" );

		queryMode( (long)10000 );

		NodeLabel label = varCBODD1215.getNodeLabel();
		label.setSelected( true );

		//Thread.sleep( 1000 );

		//myUI.action_SHOWSELECTED.actionP( this );
		label.setEvidenceDialogShown( true );

		//Thread.sleep( 1000 );

		offsetMonitor( varCBODD1215, (float)0.46, (float)0.85 );
		Monitor monitor = label.getEvidenceDialog();
		monitor.pack();
		mouseOverRelative( monitor.asJComponent(), (float)0.1, (float)0.9 );

		//mynd.revalidate();
		//mynd.repaint();

		Thread.sleep( 1000 );

		screenshot( mynd, "monitors2" );
	}

	private JPopupMenu nodeLabelPopup( NodeLabel label, int indexItem ){
		JPopupMenu popup = mynd.nodeLabelPopup( label );
		popup.revalidate();
		popup.repaint();

		//popup.getSelectionModel().setSelectedIndex( indexItem );
		mouseOverLowerRightCorner( popup.getComponent( indexItem ) );

		return popup;
	}

	private void packNetworkDisplay(){
		Dimension sizeNetwork = mynd.getNetworkSize( new Dimension() );
		sizeNetwork.width += 32;
		sizeNetwork.height += 64;
		mynd.setSize( sizeNetwork );
		mynd.refresh();
	}

	public static final String[] PATHS_QM = new String[] {
		STR_NAME_DIRECTORY_NETWORKS,
		STR_NAME_DIRECTORY_NETWORKS + "/twin.net",
		STR_NAME_DIRECTORY_NETWORKS + "/Water.hugin"
	};

	private void queryMode( long timeout ) throws Exception {
		myUI.action_QUERYMODE.actionP( this );
		long starttime = System.currentTimeMillis();
		while( mynif.getSamiamUserMode().contains( SamiamUserMode.COMPILING ) || (!mynif.getSamiamUserMode().contains( SamiamUserMode.QUERY )) ){
			if( (System.currentTimeMillis() - starttime) > timeout ) throw new IllegalStateException( "timed out waiting for query mode" );
			Thread.sleep( (long)250 );
		}
	}

	private void setMonitorZoom( double newZoom ){
		mySamiamPreferences.getMappedPreference( SamiamPreferences.evidDlgZooms ).setValue( Boolean.FALSE );
		mySamiamPreferences.getMappedPreference( SamiamPreferences.evidDlgZoomFactor ).setValue( new Double( newZoom ) );
	}

	public void assertingEvidence() throws Exception {
		cleanup( "asserting evidence" );

		filesExist( PATHS_AE );

		setNodeDefaultSize( 150, 55 );
		setPreEShown( true );
		setDynamatorByName( "shenoy-shafer" );
		setMonitorZoom( (double)100.00 );

		waitForOpen( new File( STR_PATH_CANCER ), (long)5000 );
		cram( false );

		DisplayableFiniteVariable varC = (DisplayableFiniteVariable) mydbn.forID( "C" );
		observe( varC, "Absent" );

		queryMode( (long)5000 );
		myUI.action_SHOWALL.actionP( this );

		for( DFVIterator it = mydbn.dfvIterator(); it.hasNext(); ){
			offsetMonitor( it.nextDFV(), (float)0.5, (float)0.85 );
		}
		DisplayableFiniteVariable varE = (DisplayableFiniteVariable) mydbn.forID( "E" );
		offsetMonitor( varE, (float)0.1, (float)0.85 );

		myetsp.getEvidenceTree().setExpandVariableBranches( true );

		Point locPain = myetsp.getLocation();
		SwingUtilities.convertPointToScreen( locPain, myetsp.getParent() );
		Dimension sizePain = myetsp.getSize();
		int xPop = locPain.x + (int)(((float)sizePain.width)*((float)0.22));
		int yPop = locPain.y + (int)(((float)sizePain.height)*((float)0.75));
		Point locPopup = new Point( xPop, yPop );

		JPopupMenu popup = myetsp.showPopup( locPopup );

		int xExpand = locPopup.x + 10;
		int yExpand = locPopup.y + popup.getSize().height - 10;
		Point locItemExpand = new Point( xExpand, yExpand );

		mouseChoreography( locPopup, locItemExpand );
		Thread.sleep( (long)250 );

		screenshot( "ae1" );

		popup.setVisible( false );
	}

	public static final String[] PATHS_AE = new String[] {
		STR_NAME_DIRECTORY_NETWORKS,
		STR_PATH_CANCER
	};

	private void setDynamatorByName( String name ) throws java.beans.PropertyVetoException{
		//System.out.println( "ScreenCaptureAutomation.setDynamatorByName( \""+name+"\" )" );
		Dynamator dyn;
		String dispName;
		for( Iterator it = myUI.getDynamators().iterator(); it.hasNext(); ){
			dyn = (Dynamator) it.next();
			dispName = dyn.getDisplayName();
			//System.out.println( "found \"" + dispName + "\"" );
			if( dispName.equals( name ) ){
				myUI.setDynamator( dyn );
				return;
			}
		}
		throw new IllegalStateException( "dynamator not found with name \"" +name+ "\"" );
	}

	private void offsetMonitor( DisplayableFiniteVariable dfv, float xfactor, float yfactor ){
		NodeLabel nl = dfv.getNodeLabel();
		Monitor monitor = nl.getEvidenceDialog();
		Dimension dim = nl.getActualSize( new Dimension() );
		Point loc = nl.getActualLocation( new Point() );
		loc.x += (int)(((float)dim.width)*xfactor);
		loc.y += (int)(((float)dim.height)*yfactor);
		monitor.setActualLocation( loc );
	}

	private void setNodeDefaultSize( int width, int height ){
		mySamiamPreferences.getMappedPreference( SamiamPreferences.nodeDefaultSize ).setValue( new Dimension( width, height ) );
	}

	private void setPreEShown( boolean flag ){
		mySamiamPreferences.getMappedPreference( SamiamPreferences.autoCalculatePrE ).setValue( new Boolean( flag ) );
	}

	public void selectingNodes() throws Exception {
		cleanup( "selecting nodes" );

		filesExist( PATHS_SN );

		setNodeDefaultSize( 150, 55 );

		waitForOpen( new File( STR_PATH_CANCER ), (long)5000 );
		cram( false );

		DisplayableFiniteVariable varB = (DisplayableFiniteVariable) mydbn.forID( "B" );
		DisplayableFiniteVariable varE = (DisplayableFiniteVariable) mydbn.forID( "E" );

		NodeLabel labelB = varB.getNodeLabel();
		Point begin = labelB.getActualLocation( new Point() );
		SwingUtilities.convertPointToScreen( begin, labelB.getParent() );
		begin.y -= 20;
		begin.x += 20;

		NodeLabel labelE = varE.getNodeLabel();
		Point end = labelE.getActualLocation( new Point() );
		SwingUtilities.convertPointToScreen( end, labelE.getParent() );
		Dimension sizeE = labelE.getActualSize( new Dimension() );
		end.x += (int)(((float)sizeE.width)*((float)0.75));
		end.y += sizeE.height + 10;

		mouseMoveRecorded( begin.x, begin.y );
		myRobot.mousePress( InputEvent.BUTTON1_MASK );
		Thread.sleep( 0x100 );
		mouseMoveRecorded( end.x, end.y );
		Thread.sleep( 0x100 );

		screenshot( "sn1", false );

		myRobot.mouseRelease( InputEvent.BUTTON1_MASK );
	}

	private void best( NetworkInternalFrame nif, boolean fit ) throws Exception {
		//try{
			nif.setMaximum( false );
			nif.setBounds( new Rectangle( new Point(0,0), nif.getParent().getSize() ) );
			NetworkDisplay nd = nif.getNetworkDisplay();
			nd.setMaximum( false );
			nd.setBounds( new Rectangle( new Point(0,0), nif.getRightHandDesktopPane().getSize() ) );
			if( fit ) nd.fitOnScreen();
			nd.setSelected( true );
		//}catch( Exception e ){
		//	System.err.println( "Warning: ScreenCaptureAutomation.best() caught: " + e );
		//}
	}

	public NetworkInternalFrame waitForOpen( File filenetwork, long timeout ) throws Exception {
		myUI.openFile( filenetwork );
		NetworkInternalFrame nif = null;
		long starttime = System.currentTimeMillis();
		while( nif == null ){
			if( (System.currentTimeMillis() - starttime) > timeout ) throw new IllegalStateException( "timed out waiting to open " + filenetwork.getAbsolutePath() );
			Thread.sleep( (long)500 );
			nif = myUI.getActiveHuginNetInternalFrame();
		}
		if( nif != null ){
			mynif = nif;
			mynd = nif.getNetworkDisplay();
			mydbn = nif.getBeliefNetwork();
			myec = mydbn.getEvidenceController();
			myetsp = nif.getTreeScrollPane();
			//myet = myetsp.getEvidenceTree();
		}
		return nif;
	}

	public static final String[] PATHS_SN = new String[] {
		STR_NAME_DIRECTORY_NETWORKS,
		STR_PATH_CANCER
	};

	/** @since 20100108 */
	public UI setLookAndFeelLater( final String classname ){
		invokeLater( new Runnable(){
			public void run(){
				myUI.setLookAndFeel( classname );
			}
		} );
		return myUI;
	}

	public void workingWithFiles() throws Exception {
		cleanup( "working with files" );

		filesExist( PATHS_WWF );

		LookAndFeel	lookandfeel0          = UIManager.getLookAndFeel();
		String      lookandfeel0classname = lookandfeel0.getClass().getName();
		String      lfname                = lookandfeel0.getName().toLowerCase();
		if( (lfname.indexOf( "windows" ) < 0) || (lfname.indexOf( "classic" ) >= 0) ){
			for( UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels() ){
				lfname = info.getName().toLowerCase();
				if( (lfname.indexOf( "windows" ) >= 0) && (lfname.indexOf( "classic" ) < 0) ){ setLookAndFeelLater( info.getClassName() ); Thread.sleep( 0x100 ); }
			}
		}

		final JFileChooser chooser = myUI.getFileChooser( (File)null );
		chooser.setCurrentDirectory( new File( STR_NAME_WWF_DEMO_DIRECTORY ) );

		JOptionResizeHelper helper = new JOptionResizeHelper( chooser, true, (long)3000 );
		helper.setListener( new JOptionResizeHelper.JOptionResizeHelperListener()
		{
			public void topLevelAncestorDialog( JDialog container, JOptionResizeHelper helper ){
				try
				{
				Point locationUI = myUI.getLocation();
				Point locationDialog = container.getLocation();
				locationDialog.y = locationUI.y + 32;
				container.setLocation( locationDialog );

				Collection<JComboBox> combos = getDescendantsOfType( chooser, JComboBox.class, new LinkedList<JComboBox>() );
				JComboBox             comboFilters        = null;
				ComboBoxModel         model, modelFilters = null;
				for( JComboBox next : combos ){
					model = next.getModel();
					if( model.getElementAt(0) instanceof FileFilter ){
						comboFilters = next;
						modelFilters = model;
					}
				}
				combos = null;
				if( comboFilters == null ) fail( new Exception( "could not find file filter combo box" ) );

				myPause.bind( comboFilters );

				Object toSelect = modelFilters.getElementAt( modelFilters.getSize()-1 );
				comboFilters.setSelectedItem( toSelect );
				setPopupVisible( comboFilters, true );
				comboFilters.setSelectedItem( toSelect );

				Rectangle boundsCombo = comboFilters.getBounds();
				Point locationMouseOver = comboFilters.getLocation();
				locationMouseOver.x += ( boundsCombo.width - 5 );
				locationMouseOver.y += ( boundsCombo.height - 5 );

				SwingUtilities.convertPointToScreen( locationMouseOver, comboFilters.getParent() );
				Rectangle boundsScreen = Util.getScreenBounds();

				Point locationEnd = new Point( locationMouseOver );
				locationEnd.y = boundsScreen.height;

				mouseChoreography( locationMouseOver, locationEnd );

				screenshot( "wwf1" );

			  //System.out.println( "topLevelAncestorDialog 004 showing? " + chooser.isShowing() + ", displayable? " + chooser.isDisplayable() );
				Thread.sleep( 0x400 );

				BasicFileChooserUI basicfilechooserui = (BasicFileChooserUI) chooser.getUI();
				Action aCancel = basicfilechooserui.getCancelSelectionAction();
				aCancel.actionPerformed( new ActionEvent( chooser, 0, "" ) );

				new Condition(){ public boolean unsatisfied(){ return chooser.isDisplayable(); } }.spin();

			  //System.out.println( "topLevelAncestorDialog 005 showing? " + chooser.isShowing() + ", displayable? " + chooser.isDisplayable() );
				if( chooser.isDisplayable() ){ container.dispose(); }
			  //System.out.println( "topLevelAncestorDialog 006 showing? " + chooser.isShowing() + ", displayable? " + chooser.isDisplayable() );
				}
				catch( Throwable throwable ){
					fail( throwable );
				}
			}
		} );
		helper.start();

		myUI.setSize( new Dimension( 800, 600 ) );
		chooser.showOpenDialog( myUI );

		if( UIManager.getLookAndFeel() != lookandfeel0 ){ setLookAndFeelLater( lookandfeel0.getClass().getName() ); Thread.sleep( 0x100 ); }
	}

	public void screenshot( String filenamesansextension ) throws Exception {
		this.screenshot( myUI, filenamesansextension );
	}

	public void screenshot( String filenamesansextension, boolean flagRepaint ) throws Exception {
		this.screenshot( myUI, filenamesansextension, flagRepaint );
	}

	public void screenshot( Component comp, String filenamesansextension ) throws Exception {
		this.screenshot( comp, filenamesansextension, true );
	}

	public void screenshot( Component comp, String filenamesansextension, boolean flagRepaint ) throws Exception {
		String prefix = myNameDirectoryOutput + File.separator + filenamesansextension;
		File ofile = new File( prefix + ".png" );
		File thumbfile = new File( prefix + ".thumb.png" );
		this.screenshot( comp, "png", ofile, "png", thumbfile, flagRepaint );
	}

	public void screenshot( Component comp, String formatname, java.io.File ofile, String thumbformat, java.io.File thumbfile, boolean flagRepaint ) throws Exception
	{
		if( flagRepaint ){
			//comp.revalidate();
			comp.invalidate();
			comp.validate();
			comp.repaint();
		}
		boolean paused = myPause.pause();
		if( myDelay > 0 ){ Thread.sleep( myDelay ); }
		else if( (!paused) && flagRepaint ){ Thread.sleep( 10 ); }
		Rectangle screenRect = SwingUtilities.getLocalBounds( comp );
		Point location = comp.getLocation();
		Container parent = comp.getParent();
		if( parent != null ) SwingUtilities.convertPointToScreen( location, parent );
		screenRect.setLocation( location );
		BufferedImage image = myRobot.createScreenCapture( screenRect );
		drawPointerOnto( image, comp );
		ImageIO.write( image, formatname, ofile );
		if( FLAG_THUMBS ){ ImageIO.write( thumb( image ), thumbformat, thumbfile ); }
	}

	public void drawPointerOnto( BufferedImage image, Component comp ) throws Exception
	{
		Graphics2D graphics = image.createGraphics();
		Point locPointer = new Point( myLastMousePosition );
		SwingUtilities.convertPointFromScreen( locPointer, comp );
		//System.out.println( "drawing pointer at: " + locPointer );
		//graphics.setColor( Color.black );
		//graphics.drawString( "pointer", locPointer.x, locPointer.y );
		//graphics.fillOval( locPointer.x, locPointer.y, 16, 8 );
		getImageFinisher().drawPointerAndWait( graphics, locPointer );
		graphics.dispose();
	}

	public BufferedImage thumb( BufferedImage image ) throws Exception {
		int width = (int)(((float)image.getWidth())*FLOAT_SCALE_THUMB);
		int height = (int)(((float)image.getHeight())*FLOAT_SCALE_THUMB);
		//System.out.println( "thumb() [" + width + "x" + height + "]" );
		BufferedImage ret = new BufferedImage( width, height, image.getType() );
		Graphics2D graphics = ret.createGraphics();

		getImageFinisher().drawAndWait( graphics, image, new Dimension( width, height ) );

		//Image scaled = image.getScaledInstance( width, height, Image.SCALE_FAST );
		//getImageFinisher().drawAndWait( graphics, scaled, new Point(0,0) );

		graphics.dispose();
		return ret;
	}

	public static final long LONG_IMAGE_TIMEOUT = (long)10000;
	public static final float FLOAT_SCALE_THUMB = (float)0.25;

	private Object imageReady = new Object();

	public Image getPointerImage() throws Exception {
		synchronized( ScreenCaptureAutomation.this ){
			if( myPointerImage == null ){
				Toolkit toolkit = Toolkit.getDefaultToolkit();
				File filePointer = new File( STR_PATH_POINTER );
				if( !filePointer.exists() ) throw new IllegalStateException( "mouse pointer image file not found: " + filePointer.getAbsolutePath() );
				java.net.URL url = filePointer.toURI().toURL();
				myPointerImage = toolkit.createImage( url );
				synchronized( imageReady ){
					toolkit.prepareImage( myPointerImage, -1, -1, new ImageObserver(){
						public boolean imageUpdate( Image img, int infoflags, int x, int y, int width, int height ) {
							if( (infoflags & (ImageObserver.ALLBITS | ImageObserver.FRAMEBITS)) > 0 ){
								synchronized( imageReady ){ imageReady.notifyAll(); }
								return false;
							}
							return true;
						}
					} );
					imageReady.wait( LONG_IMAGE_TIMEOUT );
				}
			}
		}
		return myPointerImage;
	}

	public ImageFinisher getImageFinisher() throws Exception {
		synchronized( ScreenCaptureAutomation.this ){
			if( myImageFinisher == null ) myImageFinisher = new ImageFinisher();
		}
		return myImageFinisher;
	}

	public class ImageFinisher implements ImageObserver
	{
		public boolean imageUpdate( Image img, int infoflags, int x, int y, int width, int height ){
			//System.out.println( "ImageFinisher.imageUpdate( "+infoflags+", "+x+", "+y+", "+width+", "+height+" )" );
			if( (infoflags & (ImageObserver.ALLBITS|ImageObserver.FRAMEBITS)) > 0 ){
				synchronized( myWait ){
					flagFinished = true;
					myWait.notifyAll();
					return false;
				}
			}

			return true;
		}

		public void drawPointerAndWait( Graphics graphics, Point loc ) throws Exception {
			this.drawAndWait( graphics, getPointerImage(), loc );
		}

		public void drawAndWait( Graphics graphics, Image image, Point loc ) throws Exception {
			synchronized( myWait ){
				flagFinished = graphics.drawImage( image, loc.x, loc.y, (ImageObserver)this );
				if( !flagFinished ) myWait.wait( LONG_IMAGE_TIMEOUT );
				if( !flagFinished ) throw new IllegalStateException( "ImageFinisher failed" );
			}
		}

		public void drawAndWait( Graphics graphics, BufferedImage image, Dimension scaled ) throws Exception {
			synchronized( myWait ){
				//System.out.println( "graphics.drawImage( image, /*dx1*/0, /*dy1*/0, /*dx2*/"+scaled.width+", /*dy2*/"+scaled.height+", /*sx1*/0, /*sy1*/0, /*sx2*/"+image.getWidth()+", /*sy2*/"+image.getHeight()+" );" );
				flagFinished = graphics.drawImage( image, /*dx1*/0, /*dy1*/0, /*dx2*/scaled.width, /*dy2*/scaled.height, /*sx1*/0, /*sy1*/0, /*sx2*/image.getWidth(), /*sy2*/image.getHeight(), (ImageObserver)this );
				if( !flagFinished ) myWait.wait( LONG_IMAGE_TIMEOUT );
				if( !flagFinished ) throw new IllegalStateException( "ImageFinisher failed" );
			}
		}

		private boolean flagFinished;
		private Object myWait = new Object();
	}

	public void mouseChoreography( Point begin, Point end ){
		int dispx = end.x - begin.x;
		int dispy = end.y - begin.y;
		int steps = Math.max( dispx, dispy );

		float deltax = ((float)dispx) / ((float)steps);
		float deltay = ((float)dispy) / ((float)steps);
		float x = (float) begin.x;
		float y = (float) begin.y;

		for( int i=0; i<=steps; i++ ){
			mouseMoveRecorded( (int)x, (int)y );
			x += deltax;
			y += deltay;
		}
	}

	private void mouseMoveRecorded( int x, int y ){
		myRobot.mouseMove( x, y );
		myLastMousePosition.setLocation( x, y );
	}

	public static <T extends Component> Collection<T> getDescendantsOfType( Container container, Class<T> type, Collection<T> found ){
		Component[] components = container.getComponents();
		Component   comp;
		for( int i=0; i<components.length; i++ ){
			comp = components[i];
			if( type.isInstance( comp ) ) found.add( type.cast( comp ) );
			else if( comp instanceof Container ) getDescendantsOfType( (Container)comp, type, found );
		}
		return found;
	}

	public static final String STR_NAME_WWF_DEMO_DIRECTORY = "six network types";

	static public final String[] PATHS_WWF = new String[] {
		STR_NAME_WWF_DEMO_DIRECTORY,
		STR_NAME_WWF_DEMO_DIRECTORY+"/ergo.erg",
		STR_NAME_WWF_DEMO_DIRECTORY+"/genie xml.xdsl",
		STR_NAME_WWF_DEMO_DIRECTORY+"/genie.dsl",
		STR_NAME_WWF_DEMO_DIRECTORY+"/hugin.hugin",
		STR_NAME_WWF_DEMO_DIRECTORY+"/hugin.net",
		STR_NAME_WWF_DEMO_DIRECTORY+"/interchange.dsc",
		STR_NAME_WWF_DEMO_DIRECTORY+"/netica.dne",
		STR_NAME_WWF_DEMO_DIRECTORY+"/netica.dnet",
	};

	static public void filesExist( String[] paths ){
		for( int i=0; i<paths.length; i++ ){
			fileExists( paths[i] );
		}
	}

	static public void fileExists( String path ){
		//assert( new File( path ).exists() );
		File required = new File( path );
		if( !required.exists() ) throw new IllegalStateException( "Required file \""+required.getAbsolutePath()+"\" not found." );
	}

	static public String makeDateString(){
		Date now = new Date( System.currentTimeMillis() );
		Calendar cal = Calendar.getInstance();
		cal.setTime( now );
		DecimalFormat format = new DecimalFormat( "00" );
		String year = format.format( cal.get(Calendar.YEAR) );
		String month = format.format( cal.get(Calendar.MONTH)+1 );
		String day = format.format( cal.get(Calendar.DAY_OF_MONTH) );
		return year + month + day;
	}

	public class Pause extends AbstractAction{//extends KeyAdapter{
		public Pause( boolean flag ){
			this.myFlagPause = flag;
			//if( flag ) ScreenCaptureAutomation.this.myUI.addKeyListener( this );
			bind( myUI.getJMenuBar() );
		}

		public void bind( JComponent component ){
			if( myFlagPause ){
				String key = "continueAfterPause";
				component.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW ).put( KeyStroke.getKeyStroke( KeyEvent.VK_SEMICOLON, InputEvent.CTRL_MASK ), key );
				component.getActionMap().put( key, (Action)this );
			}
		}

		synchronized public boolean pause() throws InterruptedException {
			if( myFlagPause ){
				STREAM_VERBOSE.println( "automated screen capture paused, press \"Ctrl-;\" to continue..." );
				this.wait();
				return true;
			}
			return false;
		}

		//public void keyPressed( KeyEvent e ){
		//	System.out.println( "Pause.keyPressed()" );
		//	cont();
		//}

		public void actionPerformed( ActionEvent evt ){
			cont();
		}

		synchronized public void cont(){
			this.notifyAll();
		}

		private boolean myFlagPause;
	}

	private   Point                      myLastMousePosition = new Point();
	private   Image                      myPointerImage;
	private   ImageFinisher              myImageFinisher;
	private   Set<String>                mySetSelected;
	private   UI                         myUI;
	private   Pause                      myPause;
	private   long                       myDelay = -1;
	private   Robot                      myRobot;
	private   String                     myNameDirectoryOutput, myCurrentSeries;
	private   SamiamPreferences          mySamiamPreferences;
	private   PreferenceGroup            globalPrefs, netPrefs, treePrefs, monitorPrefs;
	private   NetworkInternalFrame       mynif;
	private   DisplayableBeliefNetwork   mydbn;
	private   EvidenceController         myec;
	private   EvidenceTreeScrollPane     myetsp;
  //private   EvidenceTree               myet;
	private   NetworkDisplay             mynd;
	private   boolean                    myFlagComputationFinished;
}
