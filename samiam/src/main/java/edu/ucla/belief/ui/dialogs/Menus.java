package edu.ucla.belief.ui.dialogs;

import        java.util.List;
import        java.awt.*;
import        java.awt.event.*;
import        javax.swing.*;
import static javax.swing.SwingUtilities.getAncestorOfClass;
import        javax.swing.JToggleButton.ToggleButtonModel;
import        javax.swing.event.*;
import        java.lang.reflect.*;
import        java.util.*;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;

import static edu.ucla.belief.ui.util.Util.id;

import static edu.ucla.belief.ui.dialogs.ProbabilityRewrite.reportErrors;
import static edu.ucla.belief.ui.dialogs.EnumModels.firstUniqueKeyStroke;
import        edu.ucla.belief.ui.dialogs.EnumModels.Actionable;
import        edu.ucla.belief.ui.dialogs.EnumModels.Actionable.Property;
import static edu.ucla.belief.ui.dialogs.EnumModels.Actionable.Property.*;
import        edu.ucla.belief.ui.dialogs.EnumModels.View;
import static edu.ucla.belief.ui.dialogs.EnumModels.View.*;
import        edu.ucla.belief.ui.dialogs.EnumModels.Semantics;
import static edu.ucla.belief.ui.dialogs.EnumModels.Semantics.*;
import        edu.ucla.belief.ui.dialogs.EnumModels.Semantics.Semantic;
import        edu.ucla.belief.ui.dialogs.EnumModels.Modeller;
import        edu.ucla.belief.ui.dialogs.VisibilityAdapter.DelayedInitialization;
import        edu.ucla.belief.ui.dialogs.VisibilityAdapter.DelayedInitialization.Merchant;

import        edu.ucla.belief.ui.dialogs.Menus.Dendriform;
import        edu.ucla.belief.ui.dialogs.Menus.Items;

/** Helper class initializes a swing menu system from collections
	of selectable items organized by enum types that declare their semantics,
	eg mutually exclusive or additive.

	@author keith cascio
	@since  20071217 */
public class Menus<M extends Enum<M> & edu.ucla.belief.ui.dialogs.Menus.Dendriform<M>>
{
	/** "Tree-like" - suitable to occur in system of hierarchical menus. */
	public interface Dendriform<Y>{
		public Y parent();
	}

	static public class Items<B extends Enum<B> & Actionable<B> & Dendriform<M> & Semantic, M extends Enum<M> & Dendriform<M>> extends Models<B,M> implements Semantic{
		public Items( Class<B> clazz ){
			this( clazz, clazz.getEnumConstants() );
		}

		public Items( Class<B> clazz, B ... values ){
			super( clazz, null, values );
			this.dii        = new Merchant<B>( clazz, values );
			this.di         = dii;
			this.modeller   = dii;
			this.models     = new EnumModels<B>( clazz, this.modeller );
		}

		public Items( Class<B> clazz, EnumModels<B> models ){
			this( clazz, models, clazz.getEnumConstants() );
		}

		public Items( Class<B> clazz, EnumModels<B> models, B ... items ){
			super( clazz, models, items );
			this.dii        = null;
			this.di         = null;
			this.modeller   = null;
		}

		/** Needed for gastanks.
			@since 20071218 */
		public <T> T            put( B id, Property property, T value ){
			return this.dii.put( id, property, value );
		}

		public <T extends Component> T        add( B id, T comp ){
			return this.dii.add( id, comp );
		}

		public <T extends Component> T consummate( B id, T comp ){
			return this.dii.consummate( id, comp );
		}

		public Items<B,M> fallback( Property property, Object value ){
			this.dii.fallback( property, value );
			return this;
		}

		public Items<B,M>  require( Property property ){
			this.dii.require( property );
			return this;
		}

		public M            parent( B element ){
			return element.parent();
		}

		public Items<B,M> addItemListener( Object id, ItemListener listener, B ... elements ){
			super.addItemListener( id, listener, elements );
			return this;
		}

		public Items<B,M> setSelected( boolean selected, Object id, B ... elements ){
			super.setSelected( selected, id, elements );
			return this;
		}

		/** @since 20071219 */
	  /*public Items<B,M> clear(){
			if( this.dii != null ){ this.dii.clear(); }
			return this;
		}*/

		/** @since 20071219 */
		public Items<B,M> renew( Container root ){
			if( this.dii != null ){ this.dii.renew( root ); }
			return this;
		}

		final   public                Merchant<B>    dii;
		final   public   DelayedInitialization<B>    di;
		final   public                Modeller<B>    modeller;
	}

	static public class Models<B extends Enum<B> & Actionable<B> & Semantic, M extends Enum<M> & Dendriform<M>> implements Semantic{
		public Models( Class<B> clazz, EnumModels<B> models ){
			this( clazz, models, clazz.getEnumConstants() );
		}

		public Models( Class<B> clazz, EnumModels<B> models, B ... items ){
			this.clazz      = clazz;
			this.items      = items;
			this.models     = models;
			this.semantics  = this.items[0].semantics();
		}

		public Semantics  semantics(){ return semantics; }

		public EnumModels<B> models(){ return    models; }

		public M             parent( B element ){
			if( element2parent == null ) return null;
			else return element2parent.get( element );
		}

		public Models<B,M>      def( M parent, B ... elements ){
			if( element2parent == null ){ element2parent = new EnumMap<B,M>( clazz ); }
			for( B element  : elements ){ element2parent.put( element, parent      ); }
			return this;
		}

		public Models<B,M> addItemListener( Object id, ItemListener listener, B ... elements ){
			models.addItemListener( semantics, id, listener, elements );
			return this;
		}

		public Models<B,M> setSelected( boolean selected, final Object id, B ... elements ){
		  //Util.STREAM_DEBUG.println( id(this)+"<"+clazz.getSimpleName()+","+parent( elements[0] ).getClass().getSimpleName()+">.setSelected( "+selected+", "+id(id)+", "+Arrays.toString(elements)+" )" );
			for( ButtonModel bm : models.models( semantics, id, elements ) ){
			  //Util.STREAM_DEBUG.println( "    bm.setSelected( "+selected+" )" );
				bm.setSelected( selected );
			}
		  /*Util.STREAM_DEBUG.println( models.selected( semantics, id, EnumSet.noneOf( clazz ) ) );
			for( ButtonModel bm : models.models( semantics, id, elements ) ){ bm.addItemListener( new ItemListener(){
				public void itemStateChanged( ItemEvent event ){
					Util.STREAM_DEBUG.println( models.selected( semantics, id, EnumSet.noneOf( clazz ) ) );
					Thread.dumpStack();
				}
			}); }*/
			return this;
		}

		/** @since 20071219 */
	  //public Models<B,M> clear(){ return this; }

		/** @since 20071219 */
		public Models<B,M> renew( Container root ){ return this; }

		final   public                   Class<B>    clazz;
		final   public                         B[]   items;
		protected                   EnumModels<B>    models;
		final   public               Semantics       semantics;
		protected                          Map<B,M>  element2parent;
	}

	public Menus( Class<M> clazz, Set<?> blacklist, Items<?,M> ... array ){
		this.clazz     = clazz;
		this.blacklist = blacklist == null ? emptySet() : blacklist;
		def( array );
	}

	/** @since 20071223 */
	public Menus blacklist( Object id, Set<?> blacklist ){
		if( this.blacklist != blacklist ){
			try{
				this.blacklist           = blacklist;
				MenusAndButtons menus    = id2menus == null ? null : id2menus.get( id );
				if( menus != null ){ menus.blacklist( blacklist ); }
			}catch( Throwable thrown ){
				System.err.println( "warning: Menus.blacklist() caught " + thrown );
				thrown.printStackTrace( System.err );
			}
		}
		return this;
	}

	@SuppressWarnings( "unchecked" )
	public Menus<M> def( Items<?,M> ... array ){
		state.add( new OrderableState( array ) );
		return this;
	}

	@SuppressWarnings( "unchecked" )
	public Menus<M> def( Models<?,M> ... array ){
		state.add( new OrderableState( array ) );
		return this;
	}

	public Menus<M> def( Action ... actions ){
		state.add( new OrderableState( actions ) );
		return this;
	}

	/** @since 20071221 */
	public boolean white( Object obj ){
		return ! blacklist.contains( obj );
	}

	/** @since 20071221 */
	public boolean black( Object obj ){
		return   blacklist.contains( obj );
	}

	public JMenuBar bar( Object id ){
		List<Throwable> thrown = new LinkedList<Throwable>();

		JMenuBar           bar = new JMenuBar();
		try{
			JMenu jmenu;
			for( M menu : clazz.getEnumConstants() ){
				if( menu.parent() == null ){
					bar.add(             jmenu = jmenu( id, menu ) );
					if( black( menu ) ){ jmenu.setVisible( false ); }
				}
			  //else jmenu( id, menu.parent() ).add( jmenu( id, menu ) );
			}
		}catch(         Throwable throwable ){ thrown.add( throwable ); }

		try{
			for( OrderableState os : state ){
				try{
					os.addAll( id );
				}catch( Throwable throwable ){ thrown.add( throwable ); }
			}
		}catch(         Throwable throwable ){ thrown.add( throwable ); }

		try{
			id2menus.get( id ).setthebar();
		}catch(         Throwable throwable ){ thrown.add( throwable ); }

		reportErrors( thrown, "Menus.bar()" );

		return bar;
	}

	@SuppressWarnings( "unchecked" )
	private Action[] addAll( Object id, Action[] actions ){
		MenusAndButtons mnb = mnb( id );
		JMenu           jmenu;
		View            view;
		Component       btn;
		for( Action  action : actions ){
			if( black(                                              action ) ){ continue; }
			jmenu = jmenu( id,            (M)                       action.getValue( Property.menu.key ) );
			view  =                       (View)                    action.getValue( Property.view.key );
			if( view == null ){ btn = jmenu.add(                    action ); }
			else{                     jmenu.add( btn = view.button( action, new ToggleButtonModel(), Definitions.Format.debug ) ); }
			mnb.put( action,                     btn );
		}
		return actions;
	}

	private <B extends Enum<B> & Actionable<B> & Dendriform<M> & Semantic> Items<B,M> addAll( Object id, Items<B,M> items ){
		MenusAndButtons mnb = mnb( id );
		Component       button;
		for( B dendriform : items.items ){
			jmenu( id, dendriform.parent() ).add( button = items.models.newButton( items.semantics().forMenu(), id, dendriform ) );
			mnb.put(   dendriform,                button );
		}
		return items;
	}

	private <B extends Enum<B> & Actionable<B> & Semantic> Models<B,M> addAll( Object id, Models<B,M> modzz ){
		MenusAndButtons mnb = mnb( id );
		Component       button;
		for( B element : modzz.items ){
			jmenu( id, modzz.parent( element ) ).add( button = modzz.models.newButton( modzz.semantics().forMenu(), id, element ) );
			mnb.put(                 element,         button );
		}
		return modzz;
	}

	public JMenu jmenu( Object id, M menu ){
		return mnb( id ).jmenu( menu );
	}

	/** @since 20071223 */
	private MenusAndButtons mnb( Object id ){
		MenusAndButtons menus;
		if( id2menus == null ){ id2menus = Collections.singletonMap( id, menus = new MenusAndButtons() ); }
		else{  menus  =         id2menus.get( id ); }
		if(    menus == null ){
			if( id2menus.size() < 2 ){ id2menus = new HashMap<Object,MenusAndButtons>( id2menus ); }
			id2menus.put( id, menus = new MenusAndButtons() );
		}
		return menus;
	}

	public JMenu parent( Object id, Dendriform<M> dendriform ){
		return jmenu( id, dendriform.parent() );
	}

	public class OrderableState<A extends Enum<A> & Actionable<A> & Dendriform<M> & Semantic, B extends Enum<B> & Actionable<B> & Semantic>{
		public OrderableState( Items <A,M>[]   itemz ){
			this.itemz = itemz;
		}

		public OrderableState( Models<B,M>[]  modelz ){
			this.modelz = modelz;
		}

		public OrderableState(      Action[] actions ){
			this.actions = actions;
		}

		public OrderableState addAll( Object id ){
			if(   itemz != null ) for( Items <A,M> items :   itemz   ) Menus.this.addAll( id,   items );
			if(  modelz != null ) for( Models<B,M> modzz :  modelz   ) Menus.this.addAll( id,   modzz );
			if( actions != null )                                      Menus.this.addAll( id, actions );
			return this;
		}

		private       Items <A,M>[]              itemz;
		private       Models<B,M>[]             modelz;
		private            Action[]            actions;
	}

	/** @since 20071223 */
	public class MenusAndButtons
	{
		public JMenu jmenu( M menu ){
			if( menus == null ){ return null; }
			JMenu jmenu = menus.get( menu );
			if( jmenu == null ){
				menus.put( menu, jmenu = new JMenu( menu.toString() ) );
				if( menu.parent() != null ){
					jmenu( menu.parent() ).add( jmenu );
					if( black( menu ) ){        jmenu.setVisible( false ); }
				}
			}
			return jmenu;
		}

		public                        MenusAndButtons         put( Object element, Component button ){
			if( black(               element ) ){     button.setVisible( false ); }
			element2component.put(   element,         button );
			return this;
		}

		public                        MenusAndButtons   blacklist( Set<?> blacklist ){
			List<Throwable> thrown = new LinkedList<Throwable>();
			try{
				if(             menus != null ){ blacklist( blacklist,             menus ); }
			}catch( Throwable throwable ){ thrown.add( throwable ); }

			try{
				if( element2component != null ){ blacklist( blacklist, element2component ); }
			}catch( Throwable throwable ){ thrown.add( throwable ); }

			try{
				setthebar();
			}catch( Throwable throwable ){ thrown.add( throwable ); }

			reportErrors( thrown, "Menus.MenusAndButtons.blacklist()" );
			return this;
		}

		public                        MenusAndButtons setthebar() throws Exception{
			JMenuBar     bar = (JMenuBar) getAncestorOfClass( JMenuBar.class, menus.values().iterator().next() );
			boolean  visible = false;
			for( Component comp : bar.getComponents() ){ visible = visible || comp.isVisible(); }
			bar.setVisible( visible );
			return this;
		}

		private <T extends Component> MenusAndButtons   blacklist( Set<?> blacklist, Map<?,T> map ){
			for( Object sumfin : map.keySet() ){ map.get( sumfin ).setVisible( ! blacklist.contains( sumfin ) ); }
			return this;
		}

		private            Map<Object,Component>       element2component = new HashMap<Object,Component>( 0x10 );
		private            Map<M     ,JMenu    >       menus             = new EnumMap<M,JMenu>( clazz );
	}

	private                Collection<OrderableState>  state = new LinkedList<OrderableState>();
	private                       Set<?>               blacklist;
	private                     Class<M>               clazz;
	private                Map<Object,MenusAndButtons> id2menus;
}
