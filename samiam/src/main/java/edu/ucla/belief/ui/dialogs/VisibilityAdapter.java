package edu.ucla.belief.ui.dialogs;

import        edu.ucla.belief.ui.actionsandmodes.SamiamAction;
import static edu.ucla.belief.ui.util.Util.*;

import        edu.ucla.belief.ui.dialogs.VisibilityAdapter.DelayedInitialization.Merchant;
import        edu.ucla.belief.ui.dialogs.EnumModels.Actionable;
import        edu.ucla.belief.ui.dialogs.EnumModels.Actionable.Property;
import static edu.ucla.belief.ui.dialogs.EnumModels.Actionable.Property.*;
import        edu.ucla.belief.ui.dialogs.EnumModels.View;
import static edu.ucla.belief.ui.dialogs.EnumModels.View.*;
import        edu.ucla.belief.ui.dialogs.EnumModels.Modeller;
import        edu.ucla.belief.ui.dialogs.EnumModels.Modeller.Model;
import static edu.ucla.belief.ui.dialogs.EnumModels.firstUniqueKeyStroke;
import static edu.ucla.belief.ui.dialogs.EnumModels.ensureContains;
import static edu.ucla.belief.ui.dialogs.EnumModels.alreadyContains;

import        java.util.List;
import        java.awt.*;
import        java.awt.event.*;
import static java.awt.event.HierarchyEvent.*;
import        javax.swing.*;
import        javax.swing.event.*;
import static javax.swing.KeyStroke.getKeyStroke;
import        javax.swing.JToggleButton.ToggleButtonModel;
import        java.util.*;
import        java.beans.PropertyChangeListener;
import        java.beans.PropertyChangeEvent;
import static java.lang.Thread.interrupted;
import static java.lang.Thread.sleep;
import static java.lang.Thread.yield;
import static java.lang.Thread.currentThread;
import static java.lang.System.nanoTime;
import static java.lang.System.currentTimeMillis;

/** VisibilityAdapter is a ButtonModel and Action in one designed to make it
	as easy as possible to build a GUI with graphical elements you can turn on and off.<br />
	The selected state of the model reflects whether the target Components are visible.
	The action is enabled if at least one target Component is added to a visible
	parent Container.<br />
	Usually when you turn an element on it becomes visible, probably taking
	up space, thus forcing
	the layout manager to reflow the window.  Same when you turn it off,
	probably it will suddenly take up less space, thus forcing the layout manager to
	reflow the window again.  Because of that aspect, VisibilityAdapter provides
	the ability to automatically pack the ancestor window after turning elements
	on or off.
	VisibilityAdapter is designed to work well with {@link EnumModels EnumModels} to
	provide {@link EnumModels.Semantics#exclusive exclusive} or
	{@link EnumModels.Semantics#additive additive} {@link EnumModels.Semantics semantics}.

	@author keith cascio
	@since  20071213     */
public class VisibilityAdapter    implements ButtonModel, Action, Runnable, HierarchyListener, ItemListener, ContainerListener
{
	/** A good default number of milliseconds to wait before packing the ancestor window. */
	public   static  final  long        LONG_PACK_DELAY             =    0x80;
	/** The number of milliseconds for the visibility worker thread to stay alive (sleeping) before returning from run() and thus ceasing to exist. */
	public   static  final  long        LONG_LINGER                 =    0x1000;
	/** The number of milliseconds the delayed initializer sleeps before waking up and trying again. */
	public   static  final  long        LONG_INITIALIZER_FREQ       =    0x80;
	/** The number of milliseconds the delayed initializer will continue trying before giving up and printing an error. */
	public   static  final  long        LONG_INITIALIZER_TIMEOUT    =    0x2000;

	private  static         int          INT_INITIALIZER_COUNTER    =    0x0;
	/** The thread priority of the worker thread that runs delayed initialization. */
	public   static  final  int          INT_INITIALIZER_PRIORITY   =    Thread.NORM_PRIORITY - 1;
	/** A bitmask that is used to determine if a HierarchyChangeEvent is relevant. */
	public   static  final  int              SIGNIFICANT_BITS       =    SHOWING_CHANGED;// | DISPLAYABILITY_CHANGED | PARENT_CHANGED;

	/** One target Component. Early initialization where you can provide all the configuration parameters up front, including especially the target Component itself.  That is not always easy to do.  In a context of complex primary, secondary and perhaps tertiary GUI initialization, {@link #VisibilityAdapter(EnumModels.Actionable,DelayedInitialization) VisibilityAdapter(Actionable,DelayedInitialization)} is a better choice. */
	public VisibilityAdapter( Component   target,  String name, String yea, String nay, long delay ){
		this( new Component[]{ target }, name, yea, nay, delay );
	}

	/** Two or more target Components. Early initialization where you can provide all the configuration parameters up front, including especially the target Components themselves.  That is not always easy to do.  In a context of complex primary, secondary and perhaps tertiary GUI initialization, {@link #VisibilityAdapter(EnumModels.Actionable,DelayedInitialization) VisibilityAdapter(Actionable,DelayedInitialization)} is a better choice. */
	public VisibilityAdapter( Component[] targets, String name, String yea, String nay, long delay ){
		this();

		this.setDelay(           delay );
		this.setTargets(       targets );
		this.setName(             name );
		this.setVerbPhrases(  yea, nay );
		this.initialized = true;
		this.firstReaction();
	}

	/** @since 20071215 */
	private VisibilityAdapter firstReaction(){
	  //this.stamp_begun      =    nanoTime();
		try{
			reaction();
		}catch( Throwable thrown ){
			System.err.println( "warning: VisibilityAdapter \""+this.name+"\" .firstReaction() caught " + thrown );
			thrown.printStackTrace();
		}
		return this;
	}

	/** Its dangerous to construct a VisibilityAdapter with a user ButtonModel or Action,
		because the whole point of this class is that the state of the model and action is
		simple a dependant reflection of the state of actual ui Components.  The user
		would have to agree to forget about the ButtonModel/Action and not call any methods
		on them directly.

		@since 20071215 */
	private VisibilityAdapter(){
		this.action           =    new SamiamAction( "", "", '\0', null ){
			public void actionPerformed( ActionEvent event ){
			  //synchronized( synch_motivated ){ stamp_motivated = true; }//nanoTime(); }
			  //restart();
			}
		};
		this.model            =    new ToggleButtonModel();
	  //this.addActionListener( this );
		this.addItemListener( this );
	}

	/** @since 20071217 */
	final public void itemStateChanged( ItemEvent event ){
		if( flag_ignore ) return;
		synchronized( synch_motivated ){ stamp_motivated = true; restart(); }//nanoTime(); }
	}

	/** Set the target components this VisibilityAdapter will track.  The selected
		status will be based on the visibility of these Components and the enabled status
		will be based on the visibility of their parent Components.

		@since 20071215 */
	public Component[]              setTargets( Component[] targets ){
	  /*if( name.equals( "effective" ) ){
			Util.STREAM_TEST.println( name + ".setTargets() @" + nanoTime() );
			for( Component target : targets ){ Util.STREAM_TEST.println( "    " + caption( target ) ); }
		}*/
		try{
			if( this.targets != null ){
				int count_visible = 0;
				for( Component target : this.targets ){ if( target.isVisible() ){ ++count_visible; }
					target.removeHierarchyListener( this );
				}
				boolean show = count_visible > (this.targets.length / 2);
				for( Component target :      targets ){ if( target.isVisible() != show ){ target.setVisible( show ); } }
			}
		}catch( Throwable thrown ){
			System.err.println( "warning: VisibilityAdapter.setTargets() caught " + thrown );
		}
		this.targets          =    targets;
		this.half             =    targets.length / 2;
		for( Component target : targets ){ target.addHierarchyListener( this ); }
		return initPackables().targets;
	}

	/** @since 20071218 */
	public Container[]             setGasTanks( Container[] tanks ){
		this.gastanks         =    tanks;
		if( tanks == null ) return tanks;
		for( Container tank : tanks ) tank.addContainerListener( this );
	  //flag_debug = true;
		return tanks;
	}

	/** This is meaningful only if at least one of the target Components
		is a {@link LabelConstrained LabelConstrained}.

		@since 20071216 */
	public Component setPinch( Component pincher ){
		System.err.println( "warning: VisibilityAdapter.setPinch() is impotent" );
		return /*this.pincher =*/ pincher;
	}

	/** Set the number of milliseconds to wait before packing the ancestor Window(s).
		A negative value means no packing will be done.
		@since 20071215 */
	public long                       setDelay( long delay ){
		this.delay = delay;
		return initPackables().delay;
	}

	/** @since 20071215 */
	private VisibilityAdapter    initPackables(){
		boolean flag_pack = (delay >= 0) && (targets != null) && (targets.length > 0);
		this.packables                      =    flag_pack ? new           Packable[  targets.length ] : null;
		if( packanimal == null ) packanimal =    flag_pack ? new ArrayList<Packable>( targets.length ) : null;
		return this;
	}

	/** A performance feature.
		Set the bucket used to remember ancestor Windows that need to be packed.  If more than one
		VisibilityAdapter uses the same 'pack animal' then the system can potentially avoid packing
		the same Window more than once.  Even if the Collection used is not a Set, the system will
		make sure it contains a given Window only once.

		@since 20071215 */
	public Collection<Packable>    setPackAnimal( Collection<Packable> packanimal ){
		if( this.packanimal != null && (! this.packanimal.isEmpty()) ){
			for( Packable window : this.packanimal ){ if( ! packanimal.contains( window ) ){ packanimal.add( window ); } }
		}
		return this.packanimal = packanimal;
	}

	/** Set the name used as the button text, the name of the thread group, part of the
		grammar of tool tip text, and also seen in various error/debug messages.

		@since 20071215 */
	public String                      setName( String name ){
	  //if( name.equals( "conditions function reference" ) ){ flag_debug = true; }
		this.action.setName( this.name = name );
		if( this.group == null ){ this.group(); }
		initPhrases();
		return name;
	}

	/** Set the verb words used to build the tool tip grammer.
		E.G. for a VisibilityAdapter named 'apple pies', with yea phrase 'oggle the'
		and nay phrase 'deny the', the tool tip text will be 'oggle the apple pies' when
		they are invisible and 'deny the apply pies' when they are visible.

		@since 20071215 */
	public VisibilityAdapter    setVerbPhrases( String yea, String nay ){
		if( yea != null ) this.yeaverbphrase = yea;
		if( nay != null ) this.nayverbphrase = nay;
		return initPhrases();
	}

	/** @since 20071217 */
	public VisibilityAdapter    setNillaText( String txtyea, String txtnay ){
		this.txtyea = txtyea;
		this.txtnay = txtnay;
		return initPhrases();
	}

	/** @since 20071215 */
	private VisibilityAdapter      initPhrases(){
		if( name          == null ) return this;
		if( yeaverbphrase != null ) this.tipyea = yeaverbphrase.length() > 0 ? (yeaverbphrase + " " + name) : null;
		if( nayverbphrase != null ) this.tipnay = nayverbphrase.length() > 0 ? (nayverbphrase + " " + name) : null;
		if( this.txtnay   == null ) this.txtnay =    "<< " + name;
		if( this.txtyea   == null ) this.txtyea =            name + " >>";
		return this;
	}

	/** Retrieve the inner action for the purpose of configuring it.
		@since 20071216 */
	public SamiamAction asSamiamAction(){
		return this.action;
	}

	/** Construct a VisibilityAdapter when all configuration parameters are not available yet.
		This can be very useful in the context of complex GUI initialization.  For example, it
		might be easier and more intuitive to create a menu of buttons that control the
		visibility of various GUI components before the components themselves are created.
		This contructor lets you do that.  The class {@link Merchant Merchant}
		makes it very easy to supply the second argument and then update the parameters later
		in your code.

		@since 20071214 */
	public <T extends Actionable<T>> VisibilityAdapter( T id, DelayedInitialization<T> di ){
		this();
		this .renew( id, di );
	}

	/** @since 20071219 */
	public <T extends Actionable<T>> Thread renew( T id, DelayedInitialization<T> di ){
		initialized = false;
		return new Shopper<T>( id, di ).start();
	}

	/** @since 20071215 */
	private ThreadGroup group(){
		if( group == null ){ group = new ThreadGroup( name == null ? "VisibilityAdapter?" : name ); }
		return group;
	}

	/** This helper class is useful when the state of one of more Components
		implies the state of one or more other Components.
		It helps you assign an on/off {@link Trait trait} of
		a target Component (or set of target Components) based on
		a boolean {@link Function function} of a set of proxy Components.  For example,
		show a Component only when two other components {@link Function#and are both} {@link Trait#visibility visible},
		otherwise hide it.  First used Wed Dec 19 19:50:11 Pacific Standard Time 2007.

		@since 20071219 */
	static public class Proxy extends ComponentAdapter implements ComponentListener, PropertyChangeListener, HierarchyListener
	{
		/** A boolean function where domain is the set of all bit sets, range is true or false.
			@since 20071219 */
		public enum Function{
			/** all input bits true */
			and   {  public boolean invoke( int card, int len ){ return      card          >=      len     ; } },
			/** at least one input bit true */
			or    {  public boolean invoke( int card, int len ){ return      card          >              0; } },
			/** at least one true and one false */
			xor   {  public boolean invoke( int card, int len ){ return 0 <  card  && card <       len     ; } },
			/** all input bits false */
			nand  {  public boolean invoke( int card, int len ){ return      card          <=             0; } },
			/** at least one input bit false */
			nor   {  public boolean invoke( int card, int len ){ return      card          <       len     ; } },
			/** either all true or all false */
			xnor  {  public boolean invoke( int card, int len ){ return 0 >= card  || card >=      len     ; } };

			/** Get the result value when all you have is the cardinality and length.
				@param cardinality = the number of true bits
				@param length      = the total number of bits */
			abstract public boolean invoke( int cardinality, int length );
			/** Get the result value on a BitSet. */
			public boolean invoke( BitSet bits ){ return invoke( bits.cardinality(), bits.length() ); }
		}

		/** An on/off trait of a graphical Component.
			@since 20071219 */
		public enum Trait{
			/** is the Component visible? */
			visibility    { public void    set( Component comp,  boolean value ){ comp          .setVisible( value ); }
							public int     get( Component comp          ){ return comp           .isVisible(       ) ? 1 : 0; }
							public void listen( Component comp,    Proxy proxy ){ comp.addComponentListener( proxy ); } },
			/** is the Component enabled? */
			capability    { public void    set( Component comp,  boolean value ){ comp          .setEnabled( value ); }
							public int     get( Component comp          ){ return comp           .isEnabled(       ) ? 1 : 0; }
							public void listen( Component comp,    Proxy proxy ){ comp.addPropertyChangeListener( STR_PROPERTY_ENABLED, proxy ); } },
			/** is the Component showing? note: as a target trait, equivalent to {@link Trait#visibility visibility}. set() sets the visibility of the Component. */
			presence      { public void    set( Component comp,  boolean value ){ comp          .setVisible( value ); }
							public int     get( Component comp          ){ return comp           .isShowing(       ) ? 1 : 0; }
							public void listen( Component comp,    Proxy proxy ){ comp.addHierarchyListener( proxy ); }
							public int    mask(                         ){ return SHOWING_CHANGED; } },
			/** is the Component displayable? note: probably suitable only as the proxy trait, not the target trait. set( false ) removes the Component from its parent. Probably not what you want. */
			displayability{ public void    set( Component comp,  boolean value ){ if( (! value) && (comp.getParent() != null) ){ comp.getParent().remove( comp ); } }
							public int     get( Component comp          ){ return comp       .isDisplayable(       ) ? 1 : 0; }
							public void listen( Component comp,    Proxy proxy ){ comp.addHierarchyListener( proxy ); }
							public int    mask(                         ){ return DISPLAYABILITY_CHANGED; } };

			/** If meaningful, set the value of this trait on the Component. */
			abstract        public void    set( Component comp,  boolean value );
			/** Query the state of the Component. */
			abstract        public int     get( Component comp );
			/** Listen for changes in the value of this trait. */
			abstract        public void listen( Component comp,    Proxy proxy );
			/** The bitmask for HierarchyEvents only. */
			public                 int    mask(                ){ return 0; }

			/** PropertyChangeListener listens for this property change. */
			public static final String STR_PROPERTY_ENABLED = "enabled";
		  ///** HierarchyListener listens for  HierarchyEvent.DISPLAYABILITY_CHANGED | HierarchyEvent.SHOWING_CHANGED */
		  //public static final int    INT_MASK_DISPL_SHOWI = DISPLAYABILITY_CHANGED |                SHOWING_CHANGED;
		}

		/** Create a Proxy relationship and start it running.

			@param proxies   A set of Components the state of which implies the state of the target Component(s).
			@param ptrait    The trait of the proxies that implies the trait of the target(s).
			@param function  The "meaning", i.e. the function that combines the values of the proxies to produce the implied value of the target(s).
			@param targets   A set of Components that depend on the state of other Components for their state.
			@param ttrait    The trait of the target Component(s) implied by the state of the proxies.
		*/
		public Proxy( Component[] proxies, Trait ptrait, Function function, Component[] targets, Trait ttrait ){
			this.proxies     =    proxies;
			this.length      =    proxies.length;
			this.function    =   function;
			this.targets     =    targets;
			this.ptrait      =     ptrait;
			this.mask        =     ptrait.mask();
			this.ttrait      =     ttrait;

			this.cardinality =          0;
			for( Component proxy : proxies ){
				ptrait.listen( proxy, this );
				cardinality += ptrait.get( proxy );
			}
			this.react();
		}

		/** Push out the result implied by the proxy Component(s) to the target Component(s). */
		public boolean         react(){
			boolean result = function.invoke( cardinality, length );
			for( Component target : targets ){ ttrait.set( target, result ); }
			return result;
		}

		/** For {@link Trait#capability enabled state}. */
		public void   propertyChange( PropertyChangeEvent pce ){
			if( ! pce.getPropertyName().equals( Trait.STR_PROPERTY_ENABLED ) ) return;
			if( ((Component) pce.getSource()).isEnabled() ){ ++cardinality; }else{ --cardinality; }
			react();
		}

		/** For {@link Trait#visibility visible state}. */
		public void  componentHidden( ComponentEvent ce ){
			--cardinality;
			react();
		}

		/** For {@link Trait#visibility visible state}. */
		public void   componentShown( ComponentEvent ce ){
			++cardinality;
			react();
		}

		/** For {@link Trait#presence showing state}. */
		public void hierarchyChanged( HierarchyEvent he ){
			if(    (he.getChanged() == he.getComponent()) && ((he.getChangeFlags() & mask) == mask) ){
				if( he.getChanged().isShowing() ){ ++cardinality; }else{ --cardinality; }
				react();
			}
		}

		private    Component[]    proxies, targets;
		private    Function       function;
		private    Trait          ptrait, ttrait;
		private    int            cardinality, length, mask;
	  //private    BitSet         bits;
	}

	/** This class handles the work of delayed initialization.
		{@link #LONG_INITIALIZER_FREQ Every so often} it tries again to retrieve all unknown initialization
		parameters.  It will keep trying until it {@link #LONG_INITIALIZER_TIMEOUT times out}.

		@since 20071215 */
	public class Shopper<E extends Actionable<E>> implements Runnable{
		/** designed for {@link #VisibilityAdapter(EnumModels.Actionable,DelayedInitialization) VisibilityAdapter(Actionable,DelayedInitialization)} */
		public Shopper( E id, DelayedInitialization<E> di ){
			this.id = id;
			this.di = di;

			Collection<Property> required = di.required();
			if( required != null ){ uninitialized.addAll( required ); }

			optional.removeAll( uninitialized );
		}

		/** @since 20071217 */
		private Thread finish(){
		  //if( name.equals( "effective" ) ){ Util.STREAM_TEST.println( "init " + name + ".finish() @"  + nanoTime() ); }
			try{
				iterate( ultimate );
			}catch( Throwable thrown ){
				System.err.println( "warning: Shopper.finish() caught " + thrown );
			}
			flag_ignore = false;
			initialized = true;
			try{
				this.di.checkout( this.id, VisibilityAdapter.this, this.cart );
			}catch( Throwable thrown ){
				System.err.println( "warning: Shopper.finish() caught " + thrown );
			}
			firstReaction();
			return currentThread();
		}

		/** Only starts a new Thread if it fails to fully initialize on the first round. */
		public Thread start(){
			if( complete() ){ return finish(); }

			flag_ignore = true;
			began = currentTimeMillis();
			Thread ret = new Thread( /*group(),*/ Shopper.this, "init " + (name == null ? Integer.toString( INT_INITIALIZER_COUNTER++ ) : name) );
			ret.setDaemon( true );
			ret.setPriority( INT_INITIALIZER_PRIORITY );
			ret.start();
			return ret;
		}

		/** Try to complete the initialization by retrieving values for all unknown initialization parameters. */
		public boolean complete(){
			return iterate( optional ).iterate( uninitialized ).uninitialized.isEmpty();
		}

		private Shopper<E> iterate( Collection<Property> remaining ){
			Object   value;
			Property property;
			for( Iterator<Property> pit = remaining.iterator(); pit.hasNext(); ){
				if( (value = di.get( id, property  = pit.next() )) != null ){
				  //if( property == Property.shy && name.equals( "effective" ) ){ Util.STREAM_TEST.println( name + " normal delayed init config '" +property+ "' @" + nanoTime() ); }
					property.configure( VisibilityAdapter.this, value );
					cart.put( property, value );
					pit.remove();
				}
			}
			return this;
		}

		/** Sleep/wakeup/sleep/wakeup. If interrupted, don't stop, keep trying until timeout. */
		public void run(){
			try{
				while( young() ){
					sleep( LONG_INITIALIZER_FREQ );
					if( complete() ) break;
				}
			  //synchronized( id ){
				if( complete() ){
					finish();
				  /*while( young() && (! VisibilityAdapter.this.isEnabled()) ){
						sleep( LONG_INITIALIZER_FREQ );
						firstReaction();
					}*/
				}
				else{ System.err.println( "warning: " + currentThread().getName() + " timed out, failed to initialize: " + uninitialized ); }
			  //}

			  //while( young() ){ try{ sleep( 0x80 ); }catch( InterruptedException e){} }
			  /*if( ! VisibilityAdapter.this.isEnabled() ){
					Util.STREAM_TEST.printf( "%-35s <- run() disabled %4d ms @%10d, %2d/%2d rxns, blame the %s \n", currentThread().getName(), (currentTimeMillis() - began), nanoTime() - ProbabilityRewrite.NANOGENESIS, c_reaction_finished, c_reaction_called, cause );
					Util.STREAM_TEST.flush();
				}*/
			}catch( InterruptedException ie ){
				run();
			}
		}

		/** @since 20071217 */
		private boolean young(){
			return (currentTimeMillis() - began) < LONG_INITIALIZER_TIMEOUT;
		}

		private    long                              began;
		private                          E           id;
		private    DelayedInitialization<E>          di;
		private               Map<Property,Object>   cart            =   new EnumMap<Property,Object>( Property.class );
		private        Collection<Property>          uninitialized   =       EnumSet.of( Property.display,    Property.targets, Property.yeaverbphrase, Property.nayverbphrase, Property.packdelaymillis );
		private        Collection<Property>          optional        =       EnumSet.of( Property.packanimal, Property.accelerator, Property.pinchcomponent, Property.tooltip, Property.nillatext, Property.gastanks );
		private        Collection<Property>          ultimate        =       EnumSet.of( Property.shy );
	}

	/** Any class can implement this interface to act as a registry that
		supplies initialization parameters for VisibilityAdapter as they become
		available.

		@since 20071214 */
	public interface DelayedInitialization<A>{
		/** For element id, get the value of {@link EnumModels.Actionable.Property property}. If the value is
			not yet available, return null. */
		public Object get( A id, Property property );

		/** Final staleness check before possibly proceeding to late initialization mode.
			@since 20071222 */
		public DelayedInitialization<A> checkout( A id, VisibilityAdapter adapter, Map<Property,Object> cart );

		/** Allows you to specify properties in addition to those essential to VisibilityAdapter that
			must be required. */
		public Collection<Property> required();

		/** Register a call back for late values. */
	  //public DelayedInitialization<A> enableLateInitialization( A id, VisibilityAdapter adapter );

		/** A useful ready-made implementation of {@link DelayedInitialization DelayedInitialization} for
			use with the values of an enum type that implements {@link EnumModels.Actionable Actionable}.

			@since 20071214 */
		public class Merchant<I extends Enum<I> & Actionable<I>> implements DelayedInitialization<I>, Modeller<I>{
			/** Start off by registering any values already available as propertis of the {@link EnumModels.Actionable Actionables}. */
			public Merchant( Class<I> clazz ){
				this( clazz, clazz.getEnumConstants() );
			}

			/** @since 20071221 */
			public Merchant( Class<I> clazz, I ... elements ){
				this.registry = new EnumMap<I,Map<Property,Object>>( this.clazz = clazz );
				this.values   = EnumSet.noneOf( clazz );
				for( I element : elements ){
					this .values.add( element );
					this     .putAll( element );
				}
			}

			synchronized public Merchant<I> checkout( I id, VisibilityAdapter adapter, Map<Property,Object> cart ){
				Object fresh;
				for( Property property : cart.keySet() ){
					if( ((fresh = Merchant.this.get( id, property )) != null) && (cart.get( property ) != fresh) ){
						property.configure( adapter, fresh );
					}
				}
				if( id.get( Property.late ) == Boolean.TRUE ){ enableLateInit( id, adapter ); }
				return this;
			}

			public Merchant<I> enableLateInit( I id, VisibilityAdapter adapter ){
			  /*if( adapter.name.equals( "buttons" ) ){
					STREAM_DEBUG.println( "enableLateInit( "+id+", "+adapter.name+" ) @" + nanoTime() );
				}*/
				if( id2late == null ) id2late = new EnumMap<I,VisibilityAdapter>( clazz );
				id2late.put( id, adapter );
				return this;
			}

			public Collection<Property> required(){
				return required;
			}

			/** for interface Modeller
				@since 20071216 */
			public Model<I> model( I constituent ){
				if( values.contains( constituent ) ){
					VisibilityAdapter adapter = new VisibilityAdapter( constituent, this );
				  /*if( id2adapter == null ){ id2adapter = new EnumMap<I,VisibilityAdapter>( clazz ); }
					id2adapter.put( constituent, adapter );*/
					return new Model<I>( constituent, adapter, adapter );
				}
				else return new Model<I>( constituent );
			}

			public Object           get( I id, Property  property ){
				if( registry.containsKey( id ) && registry.get( id ).containsKey( property ) ){
					return                        registry.get( id ).get(         property );
				}
				if(             forall != null &&             forall.containsKey( property ) ){
					return                                    forall.get(         property );
				}
				return null;
			}

			/** Add the Component to a list of Components associated with the given element 'id'.
				This adds comp to the list but does not yet make it
				available to DelayedInitialization clients via {@link Merchant#get get()}. To make registered Components available,
				call {@link Merchant#consummate consummate()}. */
			public <T extends Component> T        add( I id, T comp ){
				if( components           == null ) components = new EnumMap<I,List<Component>>( clazz );
				if( components.get( id ) == null ) components.put( id, new LinkedList<Component>() );
				if( comp != null ) components    .get( id ).add( comp );
				return comp;
			}

			/** Add the last Component to the list of Components associated with the given element 'id' and
				finish the list.  Once you call this method, the Component comp and any Components
				you added with {@link Merchant#put(Enum,Component) put()}
				become available to clients of DelayedInitialization via {@link Merchant#get get()}. */
			public <T extends Component> T consummate( I id, T comp ){
				Component[] array;
				if( components == null || components.get( id ) == null ){
					array = (comp == null) ? new Component[0] : new Component[]{ comp };
				}
				else{
					List<Component> list = components.get( id );
					if( comp != null ) list.add( comp );
					array =  list.toArray( new Component[ list.size() ] );
				}
				put( id, Property.targets, array );
			  //System.out.println( "consummate( "+id+", ["+array.length+"] )" );
				return comp;
			}

			/** Register a new initialization parameter for an element. */
			synchronized public <T> T            put( I id, Property property, T value ){
				if( registry.get( id ) == null ) registry.put( id, new EnumMap<Property,Object>( Property.class ) );
				    registry.get( id ).put( property, value );
			  //synchronized( id ){
				try{
					if( (id2late != null) && id2late.containsKey( id ) ){
					  //System.out.println( id2late.get( id ).name + " late config '" +property+ "' @" + nanoTime() );
						property.configure( id2late.get( id ), value );
					}
				}catch( Throwable thrown ){
					System.err.println( "warning: Merchant.put() caught " + thrown );
				}
			  //}
				return value;
			}

			/** Register as initialization parameters any non-null values returned by
				the {@link EnumModels.Actionable#get Actionable.get()} method of the given
				element, for all {@link EnumModels.Actionable.Property#SET Properties}. */
			public I             putAll( I actionable ){
				return           putAll(   actionable, Property.SET );
			}

			/** Register as initialization parameters any non-null values returned by
				the {@link EnumModels.Actionable#get Actionable.get()} method of the given
				element, for all the given Properties. */
			public I             putAll( I actionable, Collection<Property> properties ){
				Object value;
				for( Property property : properties ){
					if( (value = actionable.get( property )) != null ){ put( actionable, property, value ); }
				}
				return actionable;
			}

			/** Register a default value of a property that will be returned by {@link Merchant#get get()}
				on any element for which a specific value of the property is not known.
				This method returns this Merchant so that calls can be chained. Code example:<br /><br />
<pre>
private Merchant&lt;Constituent&gt; di = new Merchant&lt;Constituent&gt;( Constituent.class ).fallback( packdelaymillis, LONG_PACK_DELAY ).fallback( Property.packanimal, new ArrayList&lt;Window&gt;( 1 ) );
</pre>
				*/
			public Merchant<I>     fallback( Property property, Object value ){
				if( forall == null ) forall = new EnumMap<Property,Object>( Property.class );
				forall.put( property, value );
				return this;
			}

			/** Add a Property to the list of required Properties. */
			public Merchant<I>      require( Property property ){
				if( required.isEmpty() ) required = new LinkedList<Property>();
				required.add( property );
				return this;
			}

			/** @since 20071219 */
		  /*public Merchant<I>        clear(){
				try{
					I[] elements = values;//clazz.getEnumConstants();
					if( components != null ){
						List<Component> list;
						for( I element : elements ){
							if( (list = components.get(element)) != null ){ list.clear(); }
						}
					}
					if( registry != null ){
						Property[] properties = Property.values();
						Object registered, original;
						Map<Property,Object> p2o;
						for( I element : elements ){
							if( (p2o = registry.get(element)) != null ){
								for( Property property : properties ){
									if( (registered = p2o.get( property )) != null ){
										if( registered != (original = element.get( property )) ){
											if( original == null ){ p2o.remove( property ); }
											else{  p2o.put( property, original ); }
										}
									}
								}
							}
						}
					}
					if( id2adapter != null ){
						VisibilityAdapter adapter;
						for( I element : elements ){
							if( element.get( Property.late ) != Boolean.TRUE ){ continue; }
							if( (adapter = id2adapter.get(element)) != null ){ adapter.clear(); }
						}
					}
				}catch( Throwable thrown ){
					System.err.println( "warning: Merchant.clear() caught " + thrown );
				}
				return this;
			}*/

			/** @since 20071219 */
			public Component excommunicate( Component comp ){
				for(      HierarchyListener al : comp     .getHierarchyListeners() ){      comp.removeHierarchyListener( al ); }
				for(      ComponentListener al : comp     .getComponentListeners() ){      comp.removeComponentListener( al ); }
				for( PropertyChangeListener al : comp.getPropertyChangeListeners() ){ comp.removePropertyChangeListener( al ); }
				return comp;
			}

			/** All previously registered target Components no longer in the hierarchy of root will be forgotten.

				@since 20071219 */
			public Merchant<I>        renew( Container root ){
				try{
					if( components != null ){
						LinkedList<Component> garbage = new LinkedList<Component>();
						LinkedList<Component> trash = new LinkedList<Component>();
						for( List<Component> list : components.values() ){
							trash.clear();
							for( Component target : list ){
								if( ! root.isAncestorOf( target ) ){ trash.add( excommunicate( target ) ); }
							}
							list.removeAll( trash );
							garbage.addAll( trash );
						}
						HashMap<Class,Integer> counts = new HashMap<Class,Integer>(8);
						for( Component target : garbage ){
							counts.put( target.getClass(), (counts.containsKey( target.getClass() ) ? counts.get( target.getClass() ) : 0) + 1 );
						}
					  /*Util.STREAM_TEST.printf( "forgetting %3d targets:\n", garbage.size() );
						for( Class type : counts.keySet() ){
							Util.STREAM_TEST.printf( "    %5d %ss\n", counts.get( type ), type.getSimpleName() );
						}*/
					}
					if( id2late == null ){ return this; }
					for( I element : id2late.keySet() ){ id2late.get( element ).renew( element, this ); }
				}catch( Throwable thrown ){
					System.err.println( "warning: Merchant.renew() caught " + thrown );
				}
				return this;
			}

			private      Class<I>                        clazz;
			private Collection<I>                        values;
			private        Collection<Property>          required = Collections.emptySet();
			private               Map<Property,Object>   forall;
			private        Map<I, Map<Property,Object>>  registry;
			private        Map<I,List<Component      >>  components;
			private        Map<I,VisibilityAdapter>      id2late;
		  //private        Map<I,VisibilityAdapter>      id2adapter;
		}
	}

	/** Make {@link #dirty dirty} and restart the cleansing process.
		@since 20071218 */
	public void touch(){
		synchronized( synch_dirty ){ stamp_dirty = true; restart(); }//nanoTime(); }
	}

	/** {@link #touch touch()}<br />
		Only happens when there is a '{@link #setGasTanks gas tank}' constraint on visibility.
		@since 20071218 */
	public void   componentAdded( ContainerEvent containerevent ){ touch(); }

	/** {@link #touch touch()}<br />
		Only happens when there is a '{@link #setGasTanks gas tank}' constraint on visibility.
		@since 20071218 */
	public void componentRemoved( ContainerEvent containerevent ){ touch(); }

	/** @since 20071220 */
	static public String caption( Component comp ){
		String    name  = comp.getName();
		if(       name != null ){ return name; }
		Class    clazz  = comp.getClass();
		String  simple  = null;
		do{     simple  = clazz.getSimpleName();
			     clazz  = clazz.getSuperclass();
		}while( simple == null || (simple.length() <= 0) );
		String    text  = null;
		if(       comp instanceof AbstractButton ){ text = ((AbstractButton) comp).getText(); }
		else if(  comp instanceof JLabel         ){ text = (        (JLabel) comp).getText(); }
		String     ret  = simple;
		if( text != null ){ ret += " \"" + text.split( "\\n" )[0].replaceAll( "[<][^>]+[>]", "" ) + "\""; }
		return     ret;
	}

	/** Mark myself as {@link #dirty dirty} if the event is {@link #SIGNIFICANT_BITS significant}. */
	public void hierarchyChanged( HierarchyEvent hierarchyevent ){
		if( (hierarchyevent.getChangeFlags() & SIGNIFICANT_BITS) > 0 ){
			Component target  = hierarchyevent.getComponent();
			Component changed = hierarchyevent.getChanged();
			if( flag_debug ){
				STREAM_DEBUG.print( caption( changed ) + " " + caption( target ) );
				if( (hierarchyevent.getChangeFlags() &         PARENT_CHANGED) > 0 ) STREAM_DEBUG.print( " PARENT_CHANGED" );
				if( (hierarchyevent.getChangeFlags() & DISPLAYABILITY_CHANGED) > 0 ) STREAM_DEBUG.print( " DISPLAYABILITY_CHANGED" );
				if( (hierarchyevent.getChangeFlags() &        SHOWING_CHANGED) > 0 ) STREAM_DEBUG.print( " SHOWING_CHANGED" );
				STREAM_DEBUG.println();
				STREAM_DEBUG.flush();
			}
			if( delay >= 0 ){
				try{
					int    index = 0;
					for( ; index < targets.length; index++ ){ if( targets[index] == target ){ break; } }
					if(   (index < targets.length) && waitForPackables() ){
						Packable packable;
						if(  ((packable  = packables[index]) == null) || ((changed != target) && (changed instanceof RootPaneContainer) && (packable.asContainer() != changed)) ){
							   packable  = packables[index]   = new Packable( target );
						}
						if(   (packable != null) && (! packanimal.contains( packable ) ) ){ packanimal.add( packable ); }
					}
				}catch( Exception thrown ){
					warn( thrown, "VisibilityAdapter.hierarchyChanged()" );
				}
			}
			touch();
		}
	}

	/** @since 20080311 */
	private boolean waitForPackables() throws Exception{
		if( this.packables != null ){ return true; }
		long start = currentTimeMillis();
		while( (this.packables == null) && ((currentTimeMillis() - start) > LONG_INITIALIZER_TIMEOUT) ){
			sleep( LONG_INITIALIZER_FREQ );
		}
		if(    (this.packables == null) && ((currentTimeMillis() - start) > LONG_INITIALIZER_TIMEOUT) ){
			System.err.println( "warning: timed out while waiting for packables in VisibilityAdapter.waitForPackables()" );
		}
		return  this.packables != null;
	}

  //public int c_reaction_called = 0, c_reaction_finished = 0;
  //public String cause = null;

	/** Respond to dirtiness by making myself clean.
		This method is intended to be called by a worker Thread.
		This method sets the selected/enabled state of this model
		based on the visibility/existence of the target Component(s) and their
		parents.  It should never be called before initialization is complete. */
	private synchronized VisibilityAdapter reaction() throws Exception
	{
	  //++c_reaction_called;
	  //if( flag_debug ) Util.STREAM_TEST.println( name + ".reaction()" );
		synchronized( synch_dirty ){ if( interrupted() ) return this; stamp_dirty = false; }//stamp_clean = nanoTime(); }

		int count_visible = 0, i = 0, length = targets.length;
		boolean  showable = false;
		Component  target = null;
		Container  parent = null;
		for( ; i < length; i++ ){
			if( interrupted() ) return this;

			if(           (target = targets[i]                  ).isVisible() ){ ++count_visible;  }
			if( ((parent = target.getParent()) != null) && parent.isShowing() ){ showable = true; break; }//++count_showable; }
		}
	  //cause = showable ? null : (parent == null ? "rain" : parent.getName());
		for( ; i < length; i++ ){
			if( interrupted() ) return this;

			if(           (target = targets[i]                  ).isVisible() ){ ++count_visible;  }
		}
		if( interrupted()     ) return this;

		boolean fueled     = true;
		if( this.gastanks != null ){
			fueled         = false;
			for( Container tank : this.gastanks ){ if( tank.getComponentCount() > 0 ){ fueled = true; break; } }
		}

		boolean enabled =      showable && fueled;//(count_showable > 0);
	  //if( flag_debug ) Util.STREAM_TEST.println( name + "? " + enabled );
		action .setEnabled(     enabled );
		model  .setEnabled(     enabled );
		if( interrupted()     ) return this;

		boolean selected =      enabled && (count_visible  >  half);
		flag_ignore =  true;
		model .setSelected(    selected );
		flag_ignore = false;
		if( interrupted()     ) return this;

		if( tipnay != null && tipyea != null ){ action.setToolTipText( selected ? tipnay : tipyea ); }

		if( txtnay != null && txtyea != null ){ action.putValueProtected( Property.nillatext.key, selected ? txtnay : txtyea ); }

		interrupted();//clear the interrupted status before we sleep
		if( (delay >= 0) && (! packanimal.isEmpty()) ){
			if( delay > 0 ){ sleep( delay ); }

			Packable window;
			while( (window = one2pack()) != null ){
			  //if( flag_debug ) System.out.println( name + "." + id( window.asContainer() ) + ".pack()" );
				window.pack();
				if(  interrupted() ){ return this; }
			}
		}

	  /*if( flag_debug ) System.out.println( "    count_showable? " + count_showable );
		if( flag_debug ) System.out.println( "    count_visible?  " + count_visible );*/
	  //++c_reaction_finished;

		return this;
	}

	/** Am I dirty? If so I will eventually need to {@link #reaction react}. */
	private boolean dirty(){
		synchronized( synch_dirty ){ return stamp_dirty; }//stamp_clean <= stamp_dirty; }
	}

	/** Wake up a {@link #LONG_LINGER lingering} worker thread
		or if no such thread exists, spawn a new one. */
	private Thread restart(){
		if( ! initialized ) return thread;
		if( flag_debug ){ STREAM_DEBUG.println( name + ".restart() called by " + currentThread().getName() + " in " + currentThread().getStackTrace()[2] ); }
		synchronized( synch_thread ){
			if( thread == null ){
				thread  = new Thread( group, this );
				thread.setDaemon( true );
				thread.start();
			}
			else{ group.interrupt(); }
		}
		return thread;
	}

	/** Intended to be called only internally to this class. The implementation of Runnable for the worker Thread.<br />

		The strategy is:<br />
		<ol>
		<li>I am a thread
		<li>yield to other threads
		<li>sleep for a bit
		<li>enter a loop
		<li>while {@link #motivated motivated}, {@link #action     act} (this will cause {@link VisibilityAdapter my room} to become dirty)
		<li>while {@link #dirty         dirty}, {@link #reaction react} (this will make  {@link VisibilityAdapter my room} clean again)
		<li>{@link #LONG_LINGER linger} around (sleeping) in case I become motivated again soon
		<li>if I finish lingering and discover I am neither {@link #motivated motivated} nor {@link #dirty         dirty}, announce that I am crossing the 'point of no return' (I will never service another request) by clearing the reference to me
		<li>return from run() and cease to exist
		</ol> */
	public void run(){
//		yield();
		try{
//			yield();
		  //if( flag_debug ) System.out.println( name + ".run()" );
			long start = currentTimeMillis();
//			yield();
			while( currentTimeMillis() - start < 0x40 ){ try{ sleep( 0x40 ); }catch( InterruptedException ie ){} }

			while( thread == currentThread() ){
				try{
					while( motivated() ){   action(); }
					while(     dirty() ){ reaction(); }

					if( thread == currentThread() ){ sleep( LONG_LINGER ); }
				}catch( InterruptedException ie ){
//					yield();
					continue;
				}

				synchronized( synch_thread ){ if( ! (motivated() || dirty()) ){ thread = null; } }
			}
		}catch( Throwable thrown ){
			System.err.println( "warning: VisibilityAdapter \""+this.name+"\" .run() caught " + thrown );
			thrown.printStackTrace();
		}
	}

	/** Am I motivated? If so I will eventually need to {@link #action act}. */
	private boolean motivated(){
		synchronized( synch_motivated ){ return stamp_motivated; }//stamp_begun <= stamp_motivated; }
	}

	/** @since 20071217 */
	public VisibilityAdapter hide(){
	  /*if( name.equals( "effective" ) ){
			STREAM_DEBUG.println( name + ".hide() @" + nanoTime() );
			for( Component target : targets ){ STREAM_DEBUG.println( "    " + caption( target ) ); }
		  //Thread.dumpStack();
		}*/
		for( Component target : targets ){ target.setVisible( false ); }
		return this;
	}

	/** @since 20080227 */
	public VisibilityAdapter show(){
		for( Component target : targets ){ target.setVisible( true ); }
		return this;
	}

	/** Respond to motivation by making changing the visibility of my target Components.
		This method is intended to be called by a worker Thread.
		This method inverts the visibility of the target Components.
		It should never be called before initialization is complete. */
	private synchronized VisibilityAdapter action() throws Exception
	{
	  //if( flag_debug ) System.out.println( name + ".action()" );
		synchronized( synch_motivated ){ if( interrupted() ) return this; stamp_motivated = false; }//stamp_begun = nanoTime(); }

		int count_visible = 0;
		for( Component target : targets ){
			if(      interrupted() ) return this;
			if( target.isVisible() ){ ++count_visible; }
		}

		boolean show = count_visible <= half;

	//	yield();//                                   These lines tend to help do the 'right thing'
		if(          interrupted() ) return this;//  when action messages happen in quick succession.

		for( int i=0; i<targets.length; i++ ){ set( i, show ); }

	  /*interrupted();//clear the interrupted status that is almost certainly true now, before we sleep

		if( (delay >= 0) && (! packanimal.isEmpty()) ){//this now handled in reaction()
			if( delay > 0 ){ sleep( delay ); }

			Packable window;
			while( (window = one2pack()) != null ){
				window.pack();
				if(  interrupted() ) return this;
			}
		}*/

		return this;
	}

	/** @since 20071217 */
	private Packable one2pack(){
		Packable ret = null;
		if( packanimal != null ){
			synchronized( packanimal ){
				Iterator<Packable> wit = packanimal.iterator();
				if( wit.hasNext() ){
					ret = wit.next();
					wit.remove();
				}
			}
		}
		return ret;
	}

	/** Set the visibility of a single target Component. Also discover if there
		is an ancestor Window that should be {@link #setDelay packed}. */
	private Component set( int index, boolean show ) throws Exception
	{
		Component target = targets[ index ];

		if( target  .isVisible() != show ){
		  /*if( show && (pincher != null) && pincher.isVisible() && (target instanceof LabelConstrained) ){
				((LabelConstrained) target).setMaximumWidth( pincher.getSize().width );
			}*/

			target .setVisible(     show );

		  /*if( delay >= 0 ){//this now handled in hierarchyChanged()
				Packable packable;
				if(   (packable  = packables[index]) == null ){
					   packable  = packables[index]   = new Packable( target );
				}
				if(   (packable != null) && (! packanimal.contains( packable ) ) ){ packanimal.add( packable ); }
			}*/
		}
		return target;
	}

  /*private static void print( Object source, java.io.PrintStream stream, String indent ){
		if( ! (source instanceof Component) ) return;
		Component comp = (Component) source;
		stream.print( indent );
		stream.println( comp.getClass().getSimpleName() );
		stream.print( indent );
		stream.println( comp.getName() );
		print( comp.getParent(), stream, indent + "  " );
	}*/

	/** Delegate to my {@link #asSamiamAction() internal action}. */
	public void                 actionPerformed( ActionEvent event ){
	  /*if( flag_debug ){
		  //System.out.println( "\n" + name + ".actionPerformed() called by " + currentThread().getName() + " in " + currentThread().getStackTrace()[2] );
		  //System.out.println( "  source:" );
		  //print( event.getSource(), System.out, "  " );
		  //for( Object listener : ((AbstractButton)event.getSource()).getActionListeners() ) System.err.println( "    " + listener.getClass().getSimpleName() );
		  //Thread.dumpStack();
		}*/
		action.actionPerformed( event );
	}
	/** Delegate to my {@link #asSamiamAction() internal action}. */
	public Object                      getValue( String key ){
		return action.getValue( key );
	}
	/** Delegate to my {@link #asSamiamAction() internal action}. */
	public void                        putValue( String key, Object value ){
		action.putValue( key, value );
	}
	/** This method is a <b><font color='#cc0000'>NO OP</font></b>. It is not possible to directly set the enabled status of this action. */
	public void                      setEnabled( boolean b ){
	  //throw new UnsupportedOperationException();
	  //action.setEnabled( b );
	}
	/** I am enabled if at least one of my target Components is added to a visible Container.
		Implementationally, I am enabled if I am fully initialized, my internal action is enabled and my internal model is enabled. */
	public boolean                    isEnabled(){
		return action.isEnabled() && model.isEnabled() && initialized;
	}
	/** Delegate to my {@link #asSamiamAction() internal action}. */
	public void       addPropertyChangeListener( PropertyChangeListener listener ){
		action.addPropertyChangeListener( listener );
	}
	/** Delegate to my {@link #asSamiamAction() internal action}. */
	public void    removePropertyChangeListener( PropertyChangeListener listener ){
		action.removePropertyChangeListener( listener );
	}

	/** Delegate to my internal toggle button model. */
	public Object[]  getSelectedObjects(){
		return model.getSelectedObjects();
	}
	/** Delegate to my internal toggle button model. */
	public boolean              isArmed(){
		return model.isArmed();
	}
	/** Delegate to my internal toggle button model. */
	public boolean           isSelected(){
		return model.isSelected() && initialized;
	}
  //public boolean            isEnabled(){
  //	return model.isEnabled();
  //}
	/** Delegate to my internal toggle button model. */
	public boolean            isPressed(){
		return model.isPressed();
	}
	/** Delegate to my internal toggle button model. */
	public boolean           isRollover(){
		return model.isRollover();
	}
	/** Delegate to my internal toggle button model. */
	public void                setArmed( boolean b ){
		model.setArmed( b );
	}
	/** This method <b><font color='#cc0000'>DOES NOT</font></b> set the
		selected state of this model. There is no way to directly set the
		selected state.  You must initiate an action. */
	public void             setSelected( boolean b ){
		restart();//model.setSelected( b );
	}
  //public void              setEnabled( boolean b ){
  //	model.setEnabled( b );
  //}
	/** Delegate to my internal toggle button model. */
	public void              setPressed( boolean b ){
		model.setPressed( b );
	}
	/** Delegate to my internal toggle button model. */
	public void             setRollover( boolean b ){
		model.setRollover( b );
	}
	/** Delegate to my internal toggle button model. */
	public void             setMnemonic( int key ){
		model.setMnemonic( key );
	}
	/** Delegate to my internal toggle button model. */
	public int              getMnemonic(){
		return model.getMnemonic();
	}
	/** Delegate to my internal toggle button model. */
	public void        setActionCommand( String s ){
		model.setActionCommand( s );
	}
	/** Delegate to my internal toggle button model. */
	public String      getActionCommand(){
		return model.getActionCommand();
	}
	/** Delegate to my internal toggle button model. */
	public void                setGroup( ButtonGroup group ){
		model.setGroup( group );
	}
	/** Only adds a listener if it is not null and not already added. */
	public void       addActionListener( ActionListener l ){
		try{ if( (l != null) && (! alreadyContains( model, l, ActionListener.class )) ) model   .addActionListener( l ); }catch( Throwable thrown ){ if( DEBUG_VERBOSE ){ System.err.println( "warning: VisibilityAdapter.addActionListener()    caught " + thrown ); } }
	}
	/** Only removed a listener if it is not null. */
	public void    removeActionListener( ActionListener l ){
		try{ if(  l != null                                                           ) model.removeActionListener( l ); }catch( Throwable thrown ){ if( DEBUG_VERBOSE ){ System.err.println( "warning: VisibilityAdapter.removeActionListener() caught " + thrown ); } }
	}
	/** Only adds a listener if it is not null and not already added. */
	public void         addItemListener( ItemListener l ){
		try{ if( (l != null) && (! alreadyContains( model, l,   ItemListener.class )) ) model     .addItemListener( l ); }catch( Throwable thrown ){ if( DEBUG_VERBOSE ){ System.err.println( "warning: VisibilityAdapter.addItemListener()      caught " + thrown ); } }
	}
	/** Only removed a listener if it is not null. */
	public void      removeItemListener( ItemListener l ){
		try{ if(  l != null                                                           ) model  .removeItemListener( l ); }catch( Throwable thrown ){ if( DEBUG_VERBOSE ){ System.err.println( "warning: VisibilityAdapter.removeItemListener()   caught " + thrown ); } }
	}
	/** Only adds a listener if it is not null and not already added. */
	public void       addChangeListener( ChangeListener l ){
		try{ if( (l != null) && (! alreadyContains( model, l, ChangeListener.class )) ) model   .addChangeListener( l ); }catch( Throwable thrown ){ if( DEBUG_VERBOSE ){ System.err.println( "warning: VisibilityAdapter.addChangeListener()    caught " + thrown ); } }
	}
	/** Only removed a listener if it is not null. */
	public void    removeChangeListener( ChangeListener l ){
		try{ if(  l != null                                                           ) model.removeChangeListener( l ); }catch( Throwable thrown ){ if( DEBUG_VERBOSE ){ System.err.println( "warning: VisibilityAdapter.removeChangeListener() caught " + thrown ); } }
	}

	private    VisibilityAdapter     debugMenuItem( JMenu menu ){
		JMenuItem ret = menu.add( (JCheckBoxMenuItem) View.menubox.button( this, this, (Test.Constituent) null ) );
		ret.setName(  name );
		return this;
	}
/*
2007-12-15 - the brokenness of JCheckBoxMenuItem - as of now,
not sure how this is happening, likely because JCheckBoxMenuItem
is indirectly constructing a JCheckBox and AWT mouse handling is
dispatching both, but they have the same model so the effect on
listeners is exactly the same, but it uncovered a fragility
of the current setup (obviously it happened for a reason),
not a true race condition because the behavior is technically correct
if 2 actionPerformed() messages fire in quick succession.

actionPerformed() called by AWT-EventQueue-0 in javax.swing.AbstractButton.fireActionPerformed(AbstractButton.java:1995)
    source: JCheckBox
restart()

actionPerformed() called by AWT-EventQueue-0 in javax.swing.AbstractButton.fireActionPerformed(AbstractButton.java:1995)
    source: JCheckBoxMenuItem
restart()
run()
action()
SHOWING_CHANGED
restart()
reaction()
SHOWING_CHANGED
restart()
run()
reaction()
*/
	/** @since 20071216 */
	private static boolean plaf( Object candidate ){
		String cn;
		return (candidate != null) && ((cn = candidate.getClass().getName()).startsWith( "java" ) || cn.startsWith( "sun" )) && (cn.indexOf( ".plaf." ) > 0);
	}

	private    VisibilityAdapter       debugButton( JComponent panel ){
		JCheckBox ret = (JCheckBox) View.box.button( this, this, (Test.Constituent) null );
		ret.setName(  name );
		panel.add( ret );
		return this;
	}

	/** @since 20071219 */
  /*public     VisibilityAdapter           clear()
	{
		flag_ignore = true;
		if( targets   != null ){ for( Component target : targets ){ target.removeHierarchyListener( this ); } }

		synchronized( synch_dirty ){ synchronized( synch_motivated ){ synchronized( synch_thread ){
			stamp_dirty   = stamp_motivated = initialized = false;
		}}}

		if( thread       !=   null ){
			Thread dead   = thread;
			thread        =   null;
			while( dead.isAlive() ){
				stamp_dirty = stamp_motivated = false;
				dead.interrupt();
				yield();
			}
		}

		stamp_dirty    = stamp_motivated = flag_ignore = initialized = false;

		if( packables != null ){ Arrays.fill( packables, null ); }
		this.gastanks  = null;
		this.pincher   = null;
		if( targets   != null ){
			HashMap<Class,Integer> counts = new HashMap<Class,Integer>(5);
			for( Component target : targets ){
				counts.put( target.getClass(), (counts.containsKey( target.getClass() ) ? counts.get( target.getClass() ) : 0) + 1 );
			}
			System.out.printf( "%-20s forgetting %3d targets:\n", name, targets.length );
			for( Class type : counts.keySet() ){
				System.out.printf( "    %5d %ss\n", counts.get( type ), type.getSimpleName() );
			}
		}
		this.targets   = null;
		this.half      = 0;
		return this;
	}*/

	/** @since 20080227 */
	static public class Packable{
		public Packable( Component target ){
			JInternalFrame iframe = (JInternalFrame) SwingUtilities.getAncestorOfClass( JInternalFrame.class, target );
			if( iframe != null ){
				this.iframe       = iframe;
				this.window       = null;
			}else{
				Window     window =                  SwingUtilities.getWindowAncestor( target );
				if( window != null ){
					this.window   = window;
					this.iframe   = null;
				}else{
					this.iframe   = null;
					this.window   = null;
				}
			}
		}

		public Packable( Window window ){
			this.window = window;
			this.iframe = null;
		}

		public Packable( JInternalFrame iframe ){
			this.iframe = iframe;
			this.window = null;
		}

		public boolean equals( Object obj ){
			if( !(obj instanceof Packable) ){ return false; }
			Packable other = (Packable) obj;
			return (other.iframe == this.iframe) && (other.window == this.window);
		}

		public String toString(){
			return "packable " + (iframe == null ? (window == null ? "???" : "window") : "iframe");
		}

		public Container pack(){
			if( iframe != null ){ iframe.pack(); return iframe; }
			if( window != null ){ window.pack(); return window; }
			return null;
		}

		public Container asContainer(){
			if( iframe != null ){ return iframe; }
			if( window != null ){ return window; }
			return null;
		}

		public RootPaneContainer asRootPaneContainer(){
			if( iframe         !=              null ){ return                     iframe; }
			if( window instanceof RootPaneContainer ){ return (RootPaneContainer) window; }
			return null;
		}

		public Component asGlassPane(){
			if( iframe         != null               ){ return                      iframe .getGlassPane(); }
			if( window instanceof RootPaneContainer  ){ return ((RootPaneContainer) window).getGlassPane(); }
			return null;
		}

		final public JInternalFrame iframe;
		final public Window         window;
	}

	private    ToggleButtonModel           model;
	private    SamiamAction                action;
	private               Packable[]       packables;
	private    Collection<Packable>        packanimal;

	private    Container[]                 gastanks;
	private    Component                   pincher;
	private    Component[]                 targets;
	private    int                         half;
	private    String                      name, yeaverbphrase, nayverbphrase, tipyea, tipnay, txtyea, txtnay;
	private    long                        delay = LONG_PACK_DELAY;

	private    Thread                      thread;
	private    ThreadGroup                 group;
	private    Object                      synch_dirty = new Object(), synch_motivated = new Object(), synch_thread = new Object();
	private    boolean                     stamp_dirty = true, stamp_motivated = false, flag_debug, initialized = false, flag_ignore;

	/** test/debug  - run the {@link Test Test} class method {@link Test#mainImpl Test.mainImpl()} */
	public static void main( String[] args ){
		int result = -1;
		try{
			result = new Test().mainImpl( args );
		}catch( Throwable thrown ){
			thrown.printStackTrace();
		}finally{
			System.exit( result );
		}
	}

	/** Show a JFrame with a doubly nested Component, a JMenu, check boxes. Let a developer menually test for race conditions. */
	public static class Test implements Modeller<edu.ucla.belief.ui.dialogs.VisibilityAdapter.Test.Constituent>{
		enum Constituent implements Actionable<Constituent>{
			darkness     (    "enter the",   "exit the" ),
			surroundings ( "preceive the", "ignore the" );

			private Constituent( String yea, String nay ){
				properties.put(                display, name() );
				properties.put(            accelerator, firstUniqueKeyStroke( name() ) );
				properties.put( Property.yeaverbphrase, yea    );
				properties.put( Property.nayverbphrase, nay    );
			}

			public  Constituent          getDefault(){ return darkness; }
			public  Object                      get( Property property ){ return this.properties.get( property ); }
			private Map<Property,Object> properties = new EnumMap<Property,Object>( Property.class );
		}

		private Merchant<Constituent> di = new Merchant<Constituent>( Constituent.class ).fallback( packdelaymillis, LONG_PACK_DELAY ).fallback( Property.packanimal, new ArrayList<Window>( 1 ) );

		public Model<Constituent> model( Constituent constituent ){
			VisibilityAdapter va = new VisibilityAdapter( constituent, di );
			return new Model<Constituent>( constituent, va , va  );
		}

		private EnumModels<Constituent> enummodels = new EnumModels<Constituent>( Constituent.class, this );

		private Object id = new Object();

		int mainImpl( String[] args ) throws Exception{
		  //for( int i=0; i<6; i++ ) System.out.println( i + " / 2? " + (i/2) );
			setLookAndFeel();

			Collection<Constituent> debug = EnumSet.of( Constituent.darkness );

			JMenuBar bar = new JMenuBar();
			JMenu   menu = bar.add( new JMenu( "view" ) );
			JPanel  bpnl = new JPanel();
		  /*for( Constituent constituent : Constituent.values() ){
				new VisibilityAdapter( constituent, di ).debugMenuItem( menu ).debugButton( bpnl ).flag_debug = debug.contains( constituent );
			}*/
			for( AbstractButton butt : enummodels.newButtons( menubox, id ) ) menu.add( butt );
			for( AbstractButton butt : enummodels.newButtons(     box, id ) ) bpnl.add( butt );

			JComponent panel = di.consummate( Constituent.darkness, new JPanel( new BorderLayout() ) );
			panel.add( Box.createVerticalStrut(  0x100), BorderLayout.EAST   );
			panel.add( Box.createHorizontalStrut(0x100), BorderLayout.CENTER );
			panel.setBackground( Color.black );
			panel.setName( "darkness" );

			JComponent inbetween = new JPanel( new BorderLayout() );
			inbetween.add( panel, BorderLayout.CENTER );

			JComponent surroundings = di.consummate( Constituent.surroundings, new JPanel( new BorderLayout() ) );
			surroundings.add( inbetween, BorderLayout.CENTER );
			surroundings.setName( "surroundings" );

			surroundings.setBorder( BorderFactory.createLineBorder( Color.red, 8 ) );

		  /*VisibilityAdapter va1 = new VisibilityAdapter(        panel, "visible darkness",     "show", "hide", LONG_PACK_DELAY ),
			                  va2 = new VisibilityAdapter( surroundings, "visible surroundings", "show", "hide", LONG_PACK_DELAY );*/

		  //va1.flag_debug = true;

		  //JMenuBar bar = new JMenuBar();
		  //JMenu   menu = bar.add( new JMenu( "view" ) );
		  //menu.add( va1.debugMenuItem() );
		  //menu.add( va2.debugMenuItem() );

			JFrame frame = new JFrame( "visibilityadapter -- test/debug" );
			frame.add( surroundings );
			frame.add( bpnl, BorderLayout.SOUTH );
		  //frame.add( va1.debugButton(), BorderLayout.NORTH );
		  //frame.add( va2.debugButton(), BorderLayout.SOUTH );
			frame.setJMenuBar( bar );
			frame.pack();
			centerWindow( frame );
			frame.setVisible( true );

			while( frame.isVisible() ) sleep( 0x200 );

			return 0;
		}
	}
}
