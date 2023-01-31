package edu.ucla.belief.ui.dialogs;

import static edu.ucla.belief.ui.util.Util.id;
import static edu.ucla.belief.ui.util.Util.STREAM_TEST;
import static edu.ucla.belief.ui.internalframes.CrouchingTiger.id;
import static edu.ucla.belief.ui.actionsandmodes.SamiamAction.SAMIAMACTION_LARGE_ICON_KEY_PUBLIC;

import        edu.ucla.belief.ui.dialogs.EnumModels.Actionable;
import        edu.ucla.belief.ui.dialogs.EnumModels.Actionable.Property;
import static edu.ucla.belief.ui.dialogs.EnumModels.Actionable.Property.*;
import        edu.ucla.belief.ui.dialogs.EnumModels.Semantics;
import static edu.ucla.belief.ui.dialogs.EnumModels.Semantics.*;
import        edu.ucla.belief.ui.dialogs.EnumModels.Semantics.Semantic;
import        edu.ucla.belief.ui.dialogs.EnumModels.View;
import static edu.ucla.belief.ui.dialogs.EnumModels.View.*;
import        edu.ucla.belief.ui.dialogs.EnumModels.Action;
import        edu.ucla.belief.ui.dialogs.EnumModels.State;
import        edu.ucla.belief.ui.dialogs.VisibilityAdapter.Packable;

import static java.lang.System.out;
import        java.awt.*;
import        java.awt.event.*;
import static java.awt.event.KeyEvent.*;
import static java.awt.event.InputEvent.*;
import static java.awt.GridBagConstraints.*;
import static javax.swing.Box.*;
import        javax.swing.*;
import        javax.swing.event.*;
import static javax.swing.KeyStroke.getKeyStroke;
import        javax.swing.JToggleButton.ToggleButtonModel;
import        java.util.*;
import        java.beans.*;
//import static java.lang.Character.isLetter;

/** A helper class that makes it easy to
	implement an enum that provides
	typical GUI functionality, namely:
<br />
	(1) {@link View#radio radio buttons} and {@link View#menuradio radio button menu items} for {@link Semantics#exclusive exclusive semantics}
<br />
	(2) {@link View#box   check boxes}   and {@link View#menubox   check    box menu items} for {@link Semantics#additive   additive semantics}
<br />
	The user identifies a logical collection of 'models' using an id Object.
	That way, a single Enum 'factory' can support an unlimited number of
	logically independant GUI selection menus.
<br />
Example code:
<br />
<pre>
import        edu.ucla.belief.ui.dialogs.EnumModels;
import static edu.ucla.belief.ui.dialogs.EnumModels.firstUniqueKeyStroke;
import        edu.ucla.belief.ui.dialogs.EnumModels.Actionable;
import        edu.ucla.belief.ui.dialogs.EnumModels.Actionable.Property;
import static edu.ucla.belief.ui.dialogs.EnumModels.Actionable.Property.*;
import        edu.ucla.belief.ui.dialogs.EnumModels.Semantics;
import static edu.ucla.belief.ui.dialogs.EnumModels.Semantics.*;
import        edu.ucla.belief.ui.dialogs.EnumModels.View;
import static edu.ucla.belief.ui.dialogs.EnumModels.View.*;

import        javax.swing.AbstractButton;
import        javax.swing.KeyStroke;
import static javax.swing.KeyStroke.getKeyStroke;
import        java.awt.event.ItemListener;
import        java.awt.event.InputEvent;
import        java.awt.event.KeyEvent;
import        java.util.EnumMap;
import        java.util.HashSet;
import        java.util.Map;
import        java.util.Set;

public enum Example implements {@link Actionable Actionable}&lt;Example&gt;
{
	example1( "foo", "walk like a foo" ),
	example2( "bar", "talk like a bar" ),
	example3( "bat",  "fly like a bat" );

	private Example( String d, String t ){
		this.properties.put(     {@link Actionable.Property#display         display}, d );
		this.properties.put(     {@link Actionable.Property#tooltip         tooltip}, t );
		this.properties.put( {@link     Actionable.Property#accelerator accelerator}, firstUniqueKeyStroke( d ) );
	}

	public Example {@link Actionable#getDefault getDefault}(){ return example1; }

	public Object {@link Actionable#get get}( {@link Actionable.Property Property} property ){ return this.properties.get( property ); }

	private Map&lt;{@link Actionable.Property Property},Object&gt; properties = new EnumMap&lt;{@link Actionable.Property Property},Object&gt;( {@link Actionable.Property Property}.class );

	public static AbstractButton[]  buttons( {@link View View} view, Object id, ItemListener listener ){
		return MODELS.newButtons( view, id, listener );
	}

	public static     Example      selected( Object id ){
		return MODELS.selected( {@link  Semantics#exclusive exclusive}, id );
	}

	public static Set&lt;Example&gt;     selected( Object id, Set&lt;Example&gt; results ){
		return MODELS.selected(  {@link Semantics#additive   additive}, id, results);
	}

	private static EnumModels&lt;Example&gt; MODELS = new {@link EnumModels#EnumModels(Class) EnumModels}&lt;Example&gt;( Example.class );

}
</pre>

	@param <E> This class is designed to provide buttons for the values of an enum type that also implements {@link Actionable Actionable}.

	@since  20071208
	@author keith cascio */
public class EnumModels<E extends Enum<E> & Actionable<E>>
{
	/** @since 20071208 */
	public EnumModels( Class<E> clazz ){
		this.clazz = clazz;
	}

	/** Create an EnumModels that, only when it needs them,
		asks the supplied modeller to create new ButtonModel/Action pairs.
		A way to use EnumModels with your own custom
		ButtonModel/Action classes.

		@since 20071216 */
	public EnumModels( Class<E> clazz, Modeller<E> modeller ){
		this( clazz );
		this.modeller = modeller;
	}

	/** A factory that, when asked, supplies a ButtonModel/Action pair for a given element.

		@param <G> THe factory provides models for the values of an enum type that also implements {@link Actionable Actionable}.

		@since 20071216 */
	public interface Modeller<G extends Enum<G> & Actionable<G>>
	{
		/** Ask the factory to supply one new ButtonModel/Action pair - not intended to be a cached value. */
		public Model<G> model( G element );

		/**	Represents a ButtonModel/Action pair.

			@param <H> For an enum type that also implements {@link Actionable Actionable}.

			@since 20071216 */
		public class Model<H extends Enum<H> & Actionable<H>>{
			/**	Default used by {@link EnumModels EnumModels} class. */
			public Model( H element ){
				this( element, null, null );
			}

			/**	element must not be null. buttonmodel and/or action can be null.  If so, they are given default values. */
			public Model( H element, ButtonModel buttonmodel, javax.swing.Action action ){
				if( element == null ) throw new IllegalArgumentException( "1st argument 'element' must not be null" );
				this.element     = element;
				this.buttonmodel = buttonmodel == null ? new ToggleButtonModel( ) : buttonmodel;
				this.action      =      action == null ? new Action<H>( element ) :      action;
			}

			/** The element associated with the button modelled here. */
			final public        H           element;
			/** A toggle button model that will be set as the model of an arbitrary number of buttons. */
			final public        ButtonModel buttonmodel;
			/** Controls the enabled state of buttons. */
			final public javax.swing.Action action;
		}
	}

	/** InputEvent modifiers for automatically building KeyStroke objects.
		The order of declaration is most important here.
		It represents the order in which the system will search for an
		unused/unreserved key stroke to associate with a menu item. */
	public enum Modifiers{
		ctrl       ( CTRL_MASK              ),
		alt        (  ALT_MASK              ),
		ctrl_alt   ( CTRL_MASK |   ALT_MASK ),
		ctrl_shift ( CTRL_MASK | SHIFT_MASK ),
		alt_shift  ( CTRL_MASK | SHIFT_MASK ),
		none       ( 0 );

		private Modifiers( int modifiers ){ this.modifiers = modifiers; }

		/** The bit flags passed as the 2nd argument to KeyStroke.getKeyStroke(). */
		public final int modifiers;
	}

	private static Set<KeyStroke> GLOBAL_RESERVED;

	/** Return a KeyStroke that corresponds to the first alphabetic character plus modifiers that is not already contained in the global reserved set. */
	public static KeyStroke firstUniqueKeyStroke( CharSequence name ){
		if( GLOBAL_RESERVED == null ) GLOBAL_RESERVED = new HashSet<KeyStroke>();
		return firstUniqueKeyStroke( name, GLOBAL_RESERVED );
	}

	/** Return a KeyStroke that corresponds to the first alphabetic character plus modifiers that is not already contained in the reserved Set. */
	public static KeyStroke firstUniqueKeyStroke( CharSequence name, Set<KeyStroke> reserved )
	{
		Modifiers[]  array = Modifiers.values();
		KeyStroke    stroke;
		char         ch;
		int          code;
		int          length = name.length();
		for( int i=0; i<length; i++ )
		{
			if( ! Character.isLetter( ch = Character.toLowerCase( name.charAt(i) ) ) ) continue;

			if( (code = keyCodeForChar( ch )/* getKeyStroke( ch ).getKeyCode() */) == VK_UNDEFINED ) continue;

			for( Modifiers modifiers : array )
			{
				if( ! reserved.contains( stroke = getKeyStroke( code, modifiers.modifiers ) ) ){
					reserved.add( stroke );
					return stroke;
				}
			}
		}
		return null;
	}

	/** Return the KeyEvent key code the corresponds to the first alphabetic character that is not already reserved. There's only 28 of 'em!! */
  /*public static int firstUniqueKeyCode( CharSequence name, Set<Integer> reserved ){
		int  length = name.length();
		int  code;
		char character;
		for( int i=0; i<length; i++ ){
			if( ((code = keyCodeForChar( character = Character.toLowerCase( name.charAt(i) ) )) != VK_UNDEFINED) && (! reserved.contains( code )) ){
				reserved.add( code );
				return code;
			}
		}
		return VK_UNDEFINED;
	}*/

	/** Convenience for automatically creating a keyboard accelerator. */
	public static int keyCodeForChar( char character ){
		if( CHAR2KEYCODE == null ){
			Map<Character,Integer> m = CHAR2KEYCODE = new HashMap<Character,Integer>( 0x20 );
			m.put( 'a', VK_A );
			m.put( 'b', VK_B );
			m.put( 'c', VK_C );
			m.put( 'd', VK_D );
			m.put( 'e', VK_E );
			m.put( 'f', VK_F );
			m.put( 'g', VK_G );
			m.put( 'h', VK_H );
			m.put( 'i', VK_I );
			m.put( 'j', VK_J );
			m.put( 'k', VK_K );
			m.put( 'l', VK_L );
			m.put( 'm', VK_M );
			m.put( 'n', VK_N );
			m.put( 'o', VK_O );
			m.put( 'p', VK_P );
			m.put( 'q', VK_Q );
			m.put( 'r', VK_R );
			m.put( 's', VK_S );
			m.put( 't', VK_T );
			m.put( 'u', VK_U );
			m.put( 'v', VK_V );
			m.put( 'w', VK_W );
			m.put( 'x', VK_X );
			m.put( 'y', VK_Y );
			m.put( 'z', VK_Z );
		}
		return CHAR2KEYCODE.containsKey( character ) ? CHAR2KEYCODE.get( character ) : VK_UNDEFINED;
	}
	private static Map<Character,Integer> CHAR2KEYCODE;

	/** To use EnumModels the enum you write must implement this interface.
		EnumModels initializes javax.swing.Actions using the properties returned by {@link Actionable#get get}(). */
	public interface Actionable<E>
	{
		/** Some of these corresponds to standard javax.swing.Action properties.  Some are supported only in java 6 and onward. */
		public enum Property{
			/** The display name, ie button text. Can start with &lt;html&gt;. */
			display     ( javax.swing.Action.NAME ){
				protected void conf( VisibilityAdapter adapter, Object value ){
					adapter.setName( value.toString() );
				}
			},
			/** The tool tip text. Can start with &lt;html&gt;. */
			tooltip     ( javax.swing.Action.SHORT_DESCRIPTION ){
				protected void conf( VisibilityAdapter adapter, Object value ){
					adapter.asSamiamAction().setToolTipText( value.toString() );
				}
			},
			/** The keyboard accelerator. */
			accelerator ( javax.swing.Action.ACCELERATOR_KEY   ){
				protected void conf( VisibilityAdapter adapter, Object value ){
					adapter.asSamiamAction().setAccelerator( (KeyStroke) value );
				}
			},
			/** Value is a javax.swing.Icon */
			smallicon( javax.swing.Action.SMALL_ICON ),
			/** Value is a javax.swing.Icon */
			largeicon( SAMIAMACTION_LARGE_ICON_KEY_PUBLIC ),//javax.swing.Action.LARGE_ICON_KEY ),//
			/** Supported only in java 6 and onward. The logical selected state of the action. */
			selected    ( "SELECTED_KEY", null ),
			/** The enabled state of the action (not supported). */
			enabled,
			/** java.awt.Component[] array correlated to a single Actionable for some purpose, eg their visibility is controlled by it. */
			targets{
				protected void conf( VisibilityAdapter adapter, Object value ){
					adapter.setTargets( (Component[]) value );
				}
			},
			/** The english verb or verb phrase that can be used to build grammar that describes the act of   selecting the thing(s) an Actionable represents, eg "show" or "choose". */
			yeaverbphrase{
				protected void conf( VisibilityAdapter adapter, Object value ){
					adapter.setVerbPhrases( value.toString(), null );
				}
			},
			/** The english verb or verb phrase that can be used to build grammar that describes the act of deselecting the thing(s) an Actionable represents, eg "hide" or "reject". */
			nayverbphrase{
				protected void conf( VisibilityAdapter adapter, Object value ){
					adapter.setVerbPhrases( null, value.toString() );
				}
			},
			/** The number property (negative value means no) declaring whether an Actionable correlated to target Components wants their Window ancestor packed after the action happens, eg actions that make large parts of a GUI visible or invisible, and if so, how many milliseconds to delay before doing the pack(). */
			packdelaymillis{
				protected void conf( VisibilityAdapter adapter, Object value ){
					adapter.setDelay( ((Number) value).longValue() );
				}
			},
			/** Value must be of type Collection&lt;Window&gt;. Facilitates coelescing of Windows to pack based on visibility changes. */
			packanimal{
				@SuppressWarnings( "unchecked" )
				protected void conf( VisibilityAdapter adapter, Object value ){
					adapter.setPackAnimal( (Collection<Packable>) value );
				}
			},
			/** Value is a Component used to constrain maximum width of any targets of type {@link LabelConstrained LabelConstrained}. */
			pinchcomponent{
				protected void conf( VisibilityAdapter adapter, Object value ){
					adapter.setPinch( (Component) value );
				}
			},
			/** Value is an Object (preferrably an enum constant) that identifies the GUI menu an {@link Actionable Actionable} belongs to. */
			menu,
			/** Value is a View. */
			view,
			/** Value is a java.awt.Insets for use only with {@link View#nilla vanilla} JButtons. */
			insets,
			/** Value is a Boolean. If true, it means initialization of target Components never ends. */
			late,
			/** Value is a Boolean. If true, it means target Components must be thrown away and reinitialized between invocations. */
		  //renewable,
			/** Value is a Boolean. If true, the 'boot' setting is all target Components hidden. */
			shy{
				protected void conf( VisibilityAdapter adapter, Object value ){
					if( Boolean.TRUE.equals( value ) ){ adapter.hide(); }
				}
			},
			/** Value is a String, but this property is most likely used for its key.
				The objective is to control the text of nilla JButtons while leaving the 'name' of the Action alone. */
			nillatext{
				protected void conf( VisibilityAdapter adapter, Object value ){
					adapter.setNillaText( value.toString(), null );
				}
			},
			/** Value is Container[]. Represents an additional visibility constraint.
				The target components are only visible if at least one of the 'gas tank' Containers is non-empty.
				I.E. the machine won't run if all gas tanks are empty. */
			gastanks{
				protected void conf( VisibilityAdapter adapter, Object tanks ){
					adapter.setGasTanks( (Container[]) tanks );
				}
			};

			/** The EnumSet of all Properties.
				@since 20071215 */
			public static final Set<Property> SET = EnumSet.allOf( Property.class );

			/** @since 20071215 */
			protected void conf( VisibilityAdapter adapter, Object value ){}

			/** Call the appropriate mutator function.
				@since 20071215 */
			final public VisibilityAdapter configure( VisibilityAdapter adapter, Object value ){
				try{
					if( value != null ) conf( adapter, value );
				}catch( Throwable thrown ){
					System.err.println( "warning: (Property)" + name() + ".configure() caught " + thrown );
				}
				return adapter;
			}

			private Property(){ this( null ); }

			private Property( String key ){ this.key = key == null ? alternate() : key; }

			private Property( String field, String alternate ){
				String key;
				try{
					key = javax.swing.Action.class.getField( field ).get( null ).toString();
				}catch( Throwable thrown ){
					key = alternate == null ? alternate() : alternate;
				}
				this.key = key;
			}

			private String alternate(){
				return name() + "." + backwardClassName();
			}

			/** The String passed as the key to javax.swing.Action.putValue(). */
			public final String key;

			/** The fully-qualified name of class {@link Property Property} from narrowest scope to widest instead of widest to narrowest. Used to build default {@link Property#key key} strings. */
			public static String backwardClassName(){
				if( REVERSAL != null ) return REVERSAL;

				String         forward = Property.class.getName();
				StringBuilder backward = new StringBuilder( forward.length() );
				int  mark = forward.length();
				char ch;
				for( int i=forward.length() - 1; i >= 0; i-- ){
					ch = forward.charAt(i);
					if( ch == '.' || ch == '$' ){
						backward.append( forward.substring( i+1, mark ) ).append( ch );
						mark = i;
					}
				}
				backward.append( forward.substring( 0, mark ) );

				return REVERSAL = backward.toString();
			}

			private static String REVERSAL;

			/** test/debug  - print out all the property keys */
			public static void main( String[] args ){
				for( Property property : values() ){
					STREAM_TEST.println( property.name() + ": \"" + property.key + "\"" );
				}
			}
		}

		/** Report the value of the given property. */
		public Object get( Property property );

		/** Get the default element for exclusive semantics. */
		public E      getDefault();
	}

	/** An extension of javax.swing.Action that gets its initial properties from an {@link Actionable Actionable}.
		actionPerformed() is a noop. */
	public static class Action<F> extends javax.swing.AbstractAction implements javax.swing.Action{
		public Action( Actionable<F> actionable ){
			this.actionable = actionable;

			Object value;
			for( Property property : Property.values() ){
				try{
					if( (value = actionable.get( property )) != null ){ this.putValue( property.key, value ); }
				}catch( Throwable thrown ){
					System.err.println( "warning: " + getClass().getName() + " constructor caught " + thrown );
				}
			}
		}

		/** no op */
		public void actionPerformed( ActionEvent event ){}

		/** Initialize properties from this guy. */
		public final Actionable<F> actionable;
	}

  /*public static <T extends Enum<T> & Actionable> EnumModels<T> instance( Class<T> clazz ){
		return new EnumModels<T>( clazz );
	}*/

	/** Logical selection semantics. */
	public enum Semantics{
		/** One and only one element selected at all times, ie radio buttons. */
		exclusive{
			public ButtonGroup  group(){ return new ButtonGroup(); }
			public View       forMenu(){ return         menuradio; }
			public View      forPanel(){ return             radio; }
		},
		/** Zero or more elements can be selected, ie check boxes. */
		additive;

		/** Exclusive semantics use a ButtonGroup to enforce the constraint. */
		public ButtonGroup  group(){ return    null; }
		/** The customary View that represents the given semantics in a dropdown menu. */
		public View       forMenu(){ return menubox; }
		/** The customary View that represents the given semantics in a normal panel. */
		public View      forPanel(){ return     box; }

		/** Anything that can be treated according to a particular {@link Semantics Semantics}.
			@since 20071217 */
		public interface Semantic{
			/** You can treat me according to these {@link Semantics Semantics}. */
			public Semantics semantics();
		}
	}

	/** The type of GUI widget to use.
		The buttons returned by the button() methods of these choices are special subclasses
		with a fixed ButtonModel/javax.swing.Action that can never be changed.
		Also they handle events slightly differently from standard buttons - they
		register non-platform (i.e. ! javax.swing.plaf.**) listeners directly on the
		ButtonModel. */
	public enum View implements Semantic{
		/** JRadioButton */
		radio    ( exclusive ) { public <T extends Enum<T>/* & Actionable<T>*/> AbstractButton button( final javax.swing.Action fa, final ButtonModel tbmodel, T element ){ return new JRadioButton( "", tbmodel.isSelected() ){
			{ setAction( fa ); }
			public void setAction( javax.swing.Action action ){ super .setAction(      fa ); }
			public void setModel (        ButtonModel  model ){ super . setModel( tbmodel ); }

			public void         addActionListener( ActionListener al ){ if( ! plaf( al ) ){ tbmodel    .addActionListener( al ); }else{ super    .addActionListener( al ); } }
			public void         addChangeListener( ChangeListener cl ){ if( ! plaf( cl ) ){ tbmodel    .addChangeListener( cl ); }else{ super    .addChangeListener( cl ); } }
			public void           addItemListener(   ItemListener il ){ if( ! plaf( il ) ){ tbmodel      .addItemListener( il ); }else{ super      .addItemListener( il ); } }
			public String        getActionCommand(                   ){              return tbmodel     .getActionCommand(    );                                             }
			public int                getMnemonic(                   ){              return tbmodel          .getMnemonic(    );                                             }
			public Object[]    getSelectedObjects(                   ){              return tbmodel   .getSelectedObjects(    );                                             }
			public boolean              isEnabled(                   ){              return tbmodel            .isEnabled(    );                                             }
			public boolean             isSelected(                   ){              return tbmodel           .isSelected(    );                                             }
			public void      removeActionListener( ActionListener al ){ if( ! plaf( al ) ){ tbmodel .removeActionListener( al ); }else{ super .removeActionListener( al ); } }
			public void      removeChangeListener( ChangeListener cl ){ if( ! plaf( cl ) ){ tbmodel .removeChangeListener( cl ); }else{ super .removeChangeListener( cl ); } }
			public void        removeItemListener(   ItemListener il ){ if( ! plaf( il ) ){ tbmodel   .removeItemListener( il ); }else{ super   .removeItemListener( il ); } }
			public void          setActionCommand(         String ac ){                     tbmodel     .setActionCommand( ac );                                             }
			public void                setEnabled(        boolean en ){                     tbmodel           .setEnabled( en );                                             }
			public void               setMnemonic(            int mn ){                     tbmodel          .setMnemonic( mn );                                             }
			public void               setSelected(        boolean se ){                                                                                                      }

		  /*public ActionListener createActionListener(){ return null; }
			public ChangeListener createChangeListener(){ return null; }
			public   ItemListener   createItemListener(){ return null; }

			public void  fireActionPerformed(  ActionEvent event ){}
			public void fireItemStateChanged(   ItemEvent event  ){}
			public void     fireStateChanged(                    ){}*/
		  //public boolean getHideActionText()
		}; } },
		/** JRadioButtonMenuItem */
		menuradio( exclusive ) { public <T extends Enum<T>/* & Actionable<T>*/> AbstractButton button( final javax.swing.Action fa, final ButtonModel tbmodel, T element ){ return new JRadioButtonMenuItem( "", tbmodel.isSelected() ){
			{ setAction( fa ); }
			public void setAction( javax.swing.Action action ){ super .setAction(      fa ); }
			public void setModel (        ButtonModel  model ){ super . setModel( tbmodel ); }

			public void         addActionListener( ActionListener al ){ if( ! plaf( al ) ){ tbmodel    .addActionListener( al ); }else{ super    .addActionListener( al ); } }
			public void         addChangeListener( ChangeListener cl ){ if( ! plaf( cl ) ){ tbmodel    .addChangeListener( cl ); }else{ super    .addChangeListener( cl ); } }
			public void           addItemListener(   ItemListener il ){ if( ! plaf( il ) ){ tbmodel      .addItemListener( il ); }else{ super      .addItemListener( il ); } }
			public String        getActionCommand(                   ){              return tbmodel     .getActionCommand(    );                                             }
			public int                getMnemonic(                   ){              return tbmodel          .getMnemonic(    );                                             }
			public Object[]    getSelectedObjects(                   ){              return tbmodel   .getSelectedObjects(    );                                             }
			public boolean              isEnabled(                   ){              return tbmodel            .isEnabled(    );                                             }
			public boolean             isSelected(                   ){              return tbmodel           .isSelected(    );                                             }
			public void      removeActionListener( ActionListener al ){ if( ! plaf( al ) ){ tbmodel .removeActionListener( al ); }else{ super .removeActionListener( al ); } }
			public void      removeChangeListener( ChangeListener cl ){ if( ! plaf( cl ) ){ tbmodel .removeChangeListener( cl ); }else{ super .removeChangeListener( cl ); } }
			public void        removeItemListener(   ItemListener il ){ if( ! plaf( il ) ){ tbmodel   .removeItemListener( il ); }else{ super   .removeItemListener( il ); } }
			public void          setActionCommand(         String ac ){                     tbmodel     .setActionCommand( ac );                                             }
			public void                setEnabled(        boolean en ){                     tbmodel           .setEnabled( en );                                             }
			public void               setMnemonic(            int mn ){                     tbmodel          .setMnemonic( mn );                                             }
			public void               setSelected(        boolean se ){                                                                                                      }
		}; } },
		/** JCheckBox */
		box      (  additive ) { public <T extends Enum<T>/* & Actionable<T>*/> AbstractButton button( final javax.swing.Action fa, final ButtonModel tbmodel, T element ){ return new JCheckBox( "", tbmodel.isSelected() ){
			{ setAction( fa ); }
			public void setAction( javax.swing.Action action ){ super .setAction(      fa ); }
			public void setModel (        ButtonModel  model ){ super . setModel( tbmodel ); }

			public void         addActionListener( ActionListener al ){ if( ! plaf( al ) ){ tbmodel    .addActionListener( al ); }else{ super    .addActionListener( al ); } }
			public void         addChangeListener( ChangeListener cl ){ if( ! plaf( cl ) ){ tbmodel    .addChangeListener( cl ); }else{ super    .addChangeListener( cl ); } }
			public void           addItemListener(   ItemListener il ){ if( ! plaf( il ) ){ tbmodel      .addItemListener( il ); }else{ super      .addItemListener( il ); } }
			public String        getActionCommand(                   ){              return tbmodel     .getActionCommand(    );                                             }
			public int                getMnemonic(                   ){              return tbmodel          .getMnemonic(    );                                             }
			public Object[]    getSelectedObjects(                   ){              return tbmodel   .getSelectedObjects(    );                                             }
			public boolean              isEnabled(                   ){              return tbmodel            .isEnabled(    );                                             }
			public boolean             isSelected(                   ){              return tbmodel           .isSelected(    );                                             }
			public void      removeActionListener( ActionListener al ){ if( ! plaf( al ) ){ tbmodel .removeActionListener( al ); }else{ super .removeActionListener( al ); } }
			public void      removeChangeListener( ChangeListener cl ){ if( ! plaf( cl ) ){ tbmodel .removeChangeListener( cl ); }else{ super .removeChangeListener( cl ); } }
			public void        removeItemListener(   ItemListener il ){ if( ! plaf( il ) ){ tbmodel   .removeItemListener( il ); }else{ super   .removeItemListener( il ); } }
			public void          setActionCommand(         String ac ){                     tbmodel     .setActionCommand( ac );                                             }
			public void                setEnabled(        boolean en ){                     tbmodel           .setEnabled( en );                                             }
			public void               setMnemonic(            int mn ){                     tbmodel          .setMnemonic( mn );                                             }
			public void               setSelected(        boolean se ){                                                                                                      }
		}; } },
		/** JCheckBoxMenuItem */
		menubox  (  additive ) { public <T extends Enum<T>/* & Actionable<T>*/> AbstractButton button( final javax.swing.Action fa, final ButtonModel tbmodel, T element ){ return new JCheckBoxMenuItem( "", tbmodel.isSelected() ){
			{ setAction( fa ); }
			public void setAction( javax.swing.Action action ){ super .setAction(      fa ); }
			public void setModel (        ButtonModel  model ){ super . setModel( tbmodel ); }

			public void         addActionListener( ActionListener al ){ if( ! plaf( al ) ){ tbmodel    .addActionListener( al ); }else{ super    .addActionListener( al ); } }
			public void         addChangeListener( ChangeListener cl ){ if( ! plaf( cl ) ){ tbmodel    .addChangeListener( cl ); }else{ super    .addChangeListener( cl ); } }
			public void           addItemListener(   ItemListener il ){ if( ! plaf( il ) ){ tbmodel      .addItemListener( il ); }else{ super      .addItemListener( il ); } }
			public String        getActionCommand(                   ){              return tbmodel     .getActionCommand(    );                                             }
			public int                getMnemonic(                   ){              return tbmodel          .getMnemonic(    );                                             }
			public Object[]    getSelectedObjects(                   ){              return tbmodel   .getSelectedObjects(    );                                             }
			public boolean              isEnabled(                   ){              return tbmodel            .isEnabled(    );                                             }
			public boolean             isSelected(                   ){              return tbmodel           .isSelected(    );                                             }
			public void      removeActionListener( ActionListener al ){ if( ! plaf( al ) ){ tbmodel .removeActionListener( al ); }else{ super .removeActionListener( al ); } }
			public void      removeChangeListener( ChangeListener cl ){ if( ! plaf( cl ) ){ tbmodel .removeChangeListener( cl ); }else{ super .removeChangeListener( cl ); } }
			public void        removeItemListener(   ItemListener il ){ if( ! plaf( il ) ){ tbmodel   .removeItemListener( il ); }else{ super   .removeItemListener( il ); } }
			public void          setActionCommand(         String ac ){                     tbmodel     .setActionCommand( ac );                                             }
			public void                setEnabled(        boolean en ){                     tbmodel           .setEnabled( en );                                             }
			public void               setMnemonic(            int mn ){                     tbmodel          .setMnemonic( mn );                                             }
			public void               setSelected(        boolean se ){                                                                                                      }
		}; } },
		/** JToggleButton */
		toggle   (  additive ) { public <T extends Enum<T>/* & Actionable<T>*/> AbstractButton button( final javax.swing.Action fa, final ButtonModel tbmodel, T element ){ return new JToggleButton( "", tbmodel.isSelected() ){
			{ setAction( fa ); }
			public void setAction( javax.swing.Action action ){ super .setAction(      fa ); }
			public void setModel (        ButtonModel  model ){ super . setModel( tbmodel ); }

			public void         addActionListener( ActionListener al ){ if( ! plaf( al ) ){ tbmodel    .addActionListener( al ); }else{ super    .addActionListener( al ); } }
			public void         addChangeListener( ChangeListener cl ){ if( ! plaf( cl ) ){ tbmodel    .addChangeListener( cl ); }else{ super    .addChangeListener( cl ); } }
			public void           addItemListener(   ItemListener il ){ if( ! plaf( il ) ){ tbmodel      .addItemListener( il ); }else{ super      .addItemListener( il ); } }
			public String        getActionCommand(                   ){              return tbmodel     .getActionCommand(    );                                             }
			public int                getMnemonic(                   ){              return tbmodel          .getMnemonic(    );                                             }
			public Object[]    getSelectedObjects(                   ){              return tbmodel   .getSelectedObjects(    );                                             }
			public boolean              isEnabled(                   ){              return tbmodel            .isEnabled(    );                                             }
			public boolean             isSelected(                   ){              return tbmodel           .isSelected(    );                                             }
			public void      removeActionListener( ActionListener al ){ if( ! plaf( al ) ){ tbmodel .removeActionListener( al ); }else{ super .removeActionListener( al ); } }
			public void      removeChangeListener( ChangeListener cl ){ if( ! plaf( cl ) ){ tbmodel .removeChangeListener( cl ); }else{ super .removeChangeListener( cl ); } }
			public void        removeItemListener(   ItemListener il ){ if( ! plaf( il ) ){ tbmodel   .removeItemListener( il ); }else{ super   .removeItemListener( il ); } }
			public void          setActionCommand(         String ac ){                     tbmodel     .setActionCommand( ac );                                             }
			public void                setEnabled(        boolean en ){                     tbmodel           .setEnabled( en );                                             }
			public void               setMnemonic(            int mn ){                     tbmodel          .setMnemonic( mn );                                             }
			public void               setSelected(        boolean se ){                                                                                                      }
		}; } },
		/** JButton - in fact a very special JButton that can use a ToggleButtonModel. */
		nilla    (  additive ) { public <T extends Enum<T>/* & Actionable<T>*/> AbstractButton button( final javax.swing.Action fa, final ButtonModel tbmodel, final T element ){ return new JButton(              fa ){
			{ if( element instanceof Actionable ){
				Actionable<?> ea = (Actionable<?>) element;
				if( ea != null && ea.get( Property.insets ) != null ) setMargin( (Insets) ea.get( Property.insets ) );
			  }

			  fa.addPropertyChangeListener( new PropertyChangeListener(){
			      public void propertyChange( PropertyChangeEvent evt ){
  			          if( evt.getPropertyName() == pname ){ setText( fa.getValue( pname ).toString() ); }
			      }
			      private String pname = Property.nillatext.key;
			  } );
			}
			public void setAction( javax.swing.Action action ){ super .setAction(      fa ); }
			public void setModel (        ButtonModel  model ){ super . setModel( bmmodel == null ? bmmodel = new Unselectable( tbmodel ) : bmmodel ); }
			private ButtonModel bmmodel;

			public void         addActionListener( ActionListener al ){ if( ! plaf( al ) ){ tbmodel    .addActionListener( al ); }else{ super    .addActionListener( al ); } }
			public void         addChangeListener( ChangeListener cl ){ if( ! plaf( cl ) ){ tbmodel    .addChangeListener( cl ); }else{ super    .addChangeListener( cl ); } }
			public void           addItemListener(   ItemListener il ){ if( ! plaf( il ) ){ tbmodel      .addItemListener( il ); }else{ super      .addItemListener( il ); } }
			public String        getActionCommand(                   ){              return tbmodel     .getActionCommand(    );                                             }
			public int                getMnemonic(                   ){              return tbmodel          .getMnemonic(    );                                             }
			public Object[]    getSelectedObjects(                   ){              return tbmodel   .getSelectedObjects(    );                                             }
			public boolean              isEnabled(                   ){              return tbmodel            .isEnabled(    );                                             }
			public boolean             isSelected(                   ){              return tbmodel           .isSelected(    );                                             }
			public void      removeActionListener( ActionListener al ){ if( ! plaf( al ) ){ tbmodel .removeActionListener( al ); }else{ super .removeActionListener( al ); } }
			public void      removeChangeListener( ChangeListener cl ){ if( ! plaf( cl ) ){ tbmodel .removeChangeListener( cl ); }else{ super .removeChangeListener( cl ); } }
			public void        removeItemListener(   ItemListener il ){ if( ! plaf( il ) ){ tbmodel   .removeItemListener( il ); }else{ super   .removeItemListener( il ); } }
			public void          setActionCommand(         String ac ){                     tbmodel     .setActionCommand( ac );                                             }
			public void                setEnabled(        boolean en ){                     tbmodel           .setEnabled( en );                                             }
			public void               setMnemonic(            int mn ){                     tbmodel          .setMnemonic( mn );                                             }
			public void               setSelected(        boolean se ){                                                                                                      }

		}; } },
		/** JMenuItem - in fact a very special JMenuItem that can use a ToggleButtonModel. */
		menuitem    (  additive ) { public <T extends Enum<T>/* & Actionable<T>*/> AbstractButton button( final javax.swing.Action fa, final ButtonModel tbmodel, final T element ){ return new JMenuItem(              fa ){
			{ fa.addPropertyChangeListener( new PropertyChangeListener(){
			      public void propertyChange( PropertyChangeEvent evt ){
  			          if( evt.getPropertyName() == pname ){ setText( fa.getValue( pname ).toString() ); }
			      }
			      private String pname = Property.nillatext.key;
			  } );
			}
			public void setAction( javax.swing.Action action ){ super .setAction(      fa ); }
			public void setModel (        ButtonModel  model ){ super . setModel( bmmodel == null ? bmmodel = new Unselectable( tbmodel ) : bmmodel ); }
			private ButtonModel bmmodel;

			public void         addActionListener( ActionListener al ){ if( ! plaf( al ) ){ tbmodel    .addActionListener( al ); }else{ super    .addActionListener( al ); } }
			public void         addChangeListener( ChangeListener cl ){ if( ! plaf( cl ) ){ tbmodel    .addChangeListener( cl ); }else{ super    .addChangeListener( cl ); } }
			public void           addItemListener(   ItemListener il ){ if( ! plaf( il ) ){ tbmodel      .addItemListener( il ); }else{ super      .addItemListener( il ); } }
			public String        getActionCommand(                   ){              return tbmodel     .getActionCommand(    );                                             }
			public int                getMnemonic(                   ){              return tbmodel          .getMnemonic(    );                                             }
			public Object[]    getSelectedObjects(                   ){              return tbmodel   .getSelectedObjects(    );                                             }
			public boolean              isEnabled(                   ){              return tbmodel            .isEnabled(    );                                             }
			public boolean             isSelected(                   ){              return tbmodel           .isSelected(    );                                             }
			public void      removeActionListener( ActionListener al ){ if( ! plaf( al ) ){ tbmodel .removeActionListener( al ); }else{ super .removeActionListener( al ); } }
			public void      removeChangeListener( ChangeListener cl ){ if( ! plaf( cl ) ){ tbmodel .removeChangeListener( cl ); }else{ super .removeChangeListener( cl ); } }
			public void        removeItemListener(   ItemListener il ){ if( ! plaf( il ) ){ tbmodel   .removeItemListener( il ); }else{ super   .removeItemListener( il ); } }
			public void          setActionCommand(         String ac ){                     tbmodel     .setActionCommand( ac );                                             }
			public void                setEnabled(        boolean en ){                     tbmodel           .setEnabled( en );                                             }
			public void               setMnemonic(            int mn ){                     tbmodel          .setMnemonic( mn );                                             }
			public void               setSelected(        boolean se ){                                                                                                      }

		}; } }
		;//,
		/** JComboBox */
	  //combo    ( exclusive ) { public JComponent      combo(       ComboBoxModel model ){ return new JComboBox(             model ); } };

/** Determine whether a given Object candidate is of type that is a sun platform class. This method is necessary because this is what happens without it:<br />
<pre>
java.lang.ClassCastException: javax.swing.JToggleButton$ToggleButtonModel cannot be cast to javax.swing.AbstractButton
        at javax.swing.plaf.basic.BasicButtonListener.stateChanged(BasicButtonListener.java:141)
        at javax.swing.DefaultButtonModel.fireStateChanged(DefaultButtonModel.java:333)
        at javax.swing.DefaultButtonModel.setMnemonic(DefaultButtonModel.java:274)
        at edu.ucla.belief.ui.dialogs.VisibilityAdapter.setMnemonic(VisibilityAdapter.java:495)
        at edu.ucla.belief.ui.dialogs.VisibilityAdapter$3.setMnemonic(VisibilityAdapter.java:602)
        at javax.swing.AbstractButton.setMnemonicFromAction(AbstractButton.java:1238)
        at javax.swing.AbstractButton.configurePropertiesFromAction(AbstractButton.java:1140)
        at javax.swing.AbstractButton.setAction(AbstractButton.java:1087)
        at javax.swing.JCheckBox.&lt;init&gt;(JCheckBox.java:120)
</pre>
@since 20071216
*/
		public static boolean plaf( Object candidate ){
			String cn;
			return (candidate != null) && ((cn = candidate.getClass().getName()).startsWith( "java" ) || cn.startsWith( "sun" )) && (cn.indexOf( ".plaf." ) > 0);
		}

		private View( Semantics semantics ){
			this.semantics = semantics;
		}

	  //protected JComponent      combo(      ComboBoxModel  model ){ return null; }

		protected AbstractButton button( javax.swing.Action action ){ return null; }

		/** Make a brand new button. */
		public <T extends Enum<T>/* & Actionable<T>*/> AbstractButton button( final javax.swing.Action fa, final ButtonModel tbmodel, T element ){
			AbstractButton ret = button(     fa );
			ret.setModel(               tbmodel );
			return ret;
		}

		public Semantics semantics(){
			return this.semantics;
		}

		/** The semantics customarily associated with this View (or at least a sensible default). */
		public final Semantics semantics;

		/** Implement this if you know exactly how you want to {@link View look}.
			@since 20071217 */
		public interface Stylish{
			/** I want to {@link View look} exactly like so. */
			public View view();
		}
	}

	/** Decorates a ToggleButtonModel so isSelected() always returns false.
		That way a JButton behaves 'normally'. It should use other visual means to express the
		selected state to the user, usually by a change of text. */
	static public class Unselectable implements ButtonModel{
		public Unselectable( ButtonModel model ){
			this.model = model;
		}

		public Object[]  getSelectedObjects(){
			return model.getSelectedObjects();
		}
		public boolean              isArmed(){
			return model.isArmed();
		}
		public boolean           isSelected(){
			return false;//model.isSelected();
		}
		public boolean            isEnabled(){
			return model.isEnabled();
		}
		public boolean            isPressed(){
			return model.isPressed();
		}
		public boolean           isRollover(){
			return model.isRollover();
		}
		public void                setArmed( boolean b ){
			model.setArmed( b );
		}
		public void             setSelected( boolean b ){
			model.setSelected( b );
		}
		public void              setEnabled( boolean b ){
			model.setEnabled( b );
		}
		public void              setPressed( boolean b ){
			model.setPressed( b );
		}
		public void             setRollover( boolean b ){
			model.setRollover( b );
		}
		public void             setMnemonic( int key ){
			model.setMnemonic( key );
		}
		public int              getMnemonic(){
			return model.getMnemonic();
		}
		public void        setActionCommand( String s ){
			model.setActionCommand( s );
		}
		public String      getActionCommand(){
			return model.getActionCommand();
		}
		public void                setGroup( ButtonGroup group ){
			model.setGroup( group );
		}
		public void       addActionListener( ActionListener l ){
			model.addActionListener( l );
		}
		public void    removeActionListener( ActionListener l ){
			model.removeActionListener( l );
		}
		public void         addItemListener( ItemListener l ){
			model.addItemListener( l );
		}
		public void      removeItemListener( ItemListener l ){
			model.removeItemListener( l );
		}
		public void       addChangeListener( ChangeListener l ){
			model.addChangeListener( l );
		}
		public void    removeChangeListener( ChangeListener l ){
			model.removeChangeListener( l );
		}

		private ButtonModel model;
	}

	/** Used with exclusive semantics. Get the first selected element from the logical menu identified by the id argument, based on implicit semantics defined by the view argument. */
	public E selected( View           view, Object id ){
		return this.selected( view.semantics, id );
	}

	/** Used with exclusive semantics. Get the first selected element from the logical menu identified by the id argument, based on excplicit semantics. */
	public E selected( Semantics semantics, Object id ){
		Map<E,State> models = models( semantics, id );
		if( models == null ) return null;

		for( E element : clazz.getEnumConstants() ) if( models.get( element ).model.isSelected() ) return element;
		return null;
	}

	/** Used with additive semantics. Get all selected elements from the logical menu identified by the id argument, based on implicit semantics defined by the view argument. */
	public Set<E> selected( View           view, Object id, Set<E> results ){
		return this.selected( view.semantics, id, results );
	}

	/** Used with additive semantics. Get all selected elements from the logical menu identified by the id argument, based on explicit semantics. */
	public Set<E> selected( Semantics semantics, Object id, Set<E> results ){
		if( results == null ) results = EnumSet.allOf( clazz );

		Map<E,State> models = models( semantics, id );
		if( models == null ){
			System.err.println( "warning: EnumModels<"+clazz.getSimpleName()+">.selected( "+semantics+", "+id(id)+" ) found no models" );
		  //Thread.dumpStack();
			return results;
		}

		for( E element : clazz.getEnumConstants() ) if( models.get( element ).model.isSelected() ) results.add( element );

		return results;
	}

	/** Create an array of new buttons of the type identified by the view argument for all the values of the enum, modelled on the logical menu identified by the id argument. */
	public AbstractButton[] newButtons( View view, Object id ){
		return this.newButtons( view, id, null );
	}

	/** Create an array of new buttons of the type identified by the view argument for all the values of the enum, modelled on the logical menu identified by the id argument. Also add the listener to each button model it had not already been added to before. */
	public AbstractButton[] newButtons( View view, Object id, ItemListener listener ){
		return this.newButtons( view, id, listener, EnumSet.allOf( clazz ), null );
	}

	/** Create an array of new buttons of the type identified by the view argument for only the values contained in the values argument, modelled on the logical menu identified by the id argument. Also add the listener to each button model it had not already been added to before. */
	public AbstractButton[] newButtons( View view, Object id, ItemListener listener, Collection<E> values, Collection<E> selected ){
		AbstractButton[] ret    = new AbstractButton[ values.size() ];
		Map<E,State>     models = this.state( view.semantics, id, listener );

		State state;
		int   index = 0;
		for( E value : (values == null ? EnumSet.allOf( clazz ) : values) ){
			state          = models.get( value );
			ret[ index++ ] = view.button( state.action, state.model, value );
			if( selected != null ) state.model.setSelected( selected.contains( value ) );
		}

		return ret;
	}

	/** @since 20071216 */
	public AbstractButton newButton( View view, Object id, E value ){
		State state = this.state( view.semantics, id, null ).get( value );
		return view.button( state.action, state.model, value );
	}

	/** Create a combo box modelled on the logical menu identified by the id argument. Also add the listener to each button model it had not already been added to before.
		@since 20071210 */
	public JComboBox newComboBox( Object id, ItemListener listener ){
		Map<E,State>     models = this.state( exclusive, id, listener );

		ComboModel       model  = models.get( clazz.getEnumConstants()[0] ).listmodel();

		return new JComboBox( model );
	}

	/** Get the existing state or create virgin state if necessary. */
	private Map<E,State> state( Semantics semantics, Object id, ItemListener listener )
	{
		Map<E,State> state = null;

		Map<Object,Map<E,State>> id2models = semantics2id2models.get( semantics );

		if(      id2models == null           )                id2models = Collections.singletonMap(                            id, state = new EnumMap<E,State>( clazz ) );
		else if( id2models.containsKey( id ) )                                                                                     state = id2models.get( id );
		else   ((id2models.size()         > 1) ? id2models : (id2models = new HashMap<Object,Map<E,State>>( id2models ))).put( id, state = new EnumMap<E,State>( clazz ) );
		semantics2id2models.put( semantics, id2models );

		Modeller.Model<E>     model;
		boolean               virgin    = false;
		E[]                   constants = clazz.getEnumConstants();
		if( state.isEmpty() ){
			virgin = true;
			ButtonGroup bg = semantics.group();
			for( E element : constants ){
				model   = modeller == null ? new Modeller.Model<E>( element ) : modeller.model( element );
				if( bg != null ) model.buttonmodel.setGroup( bg );
				state.put( element, new State( element, semantics, id, model.buttonmodel, model.action ) );
			}
		}

	  /*if( semantics == exclusive ){
			ComboModel combomodel = new ComboModel( state );
			for( E element : constants ) state.get( element ).listmodel = combomodel;
		}*/

		if( listener != null ) for( State again : state.values() ) ensureContains( again.model, listener );

		if( virgin && (semantics == exclusive) ) state.get( constants[0].getDefault() ).model.setSelected( true );

		return state;
	}

	/** Get an array of button models for each of the enum element, based on the given semantics, for the logical menu identified by the id argument. Also add the listener to each button model it had not already been added to before. */
	public ButtonModel[] models( Semantics semantics, Object id, E ... elements )//, ItemListener listener )
	{
		Map<E,State>  state     = state( semantics, id, null );

		E[]           constants = ((elements == null) || (elements.length < 1)) ? clazz.getEnumConstants() : elements;
		ButtonModel[] models    = new ButtonModel[ constants.length ];
		int           index     = 0;
		for( E element : constants ){ models[ index++ ] = state.get( element ).model; }

		return models;
	}

	/** @since 20080123 */
	public EnumModels<E> setSelected( boolean selected, Semantics semantics, Object id, Collection<E> elements ){
		Map<E,State> state = models( semantics, id );
		if( state == null ) return this;
		for( E element : elements ){
			state.get( element ).model.setSelected( selected );
		}
		return this;
	}

	/** @since 20080123 */
	@SuppressWarnings( "unchecked" )
	private     State[]                 states( int size ){
		return (State[]) new EnumModels.State[      size ];
	}

	/** @since 20080123 */
	public EnumModels<E> enforceExclusivity( Semantics semantics, Object ... ids )
	{
		final Map<ItemSelectable,State[]> taker2givers = new HashMap<ItemSelectable,State[]>();
		final ItemListener itemlistener = new ItemListener(){
			@SuppressWarnings( "unchecked" )
			public void itemStateChanged( ItemEvent event ){
				if( event.getStateChange() != ItemEvent.SELECTED ){ return; }

				for( State state : taker2givers.get( event.getItemSelectable() ) ){
					if( state.model.isSelected() ){ state.model.setSelected( false ); }
				}
			}
		};

		for( Object id : ids ){ state( semantics, id, null ); }
		Map<Object,Map<E,State>> id2states = semantics2id2models.get( semantics );

		int               count_competition = ids.length - 1;
		Map<E,State>      states;
		State[]           competitors;
		State             competition;
		boolean           flag_added;
		for( Object id : ids ){
			states = id2states.get( id );
			for( State state : states.values() ){
				for( Object competitor : ids ){
					if( competitor == id ){ continue; }
					if( (competitors = taker2givers.get( state.model )) == null ){
						taker2givers.put( state.model, competitors = states( count_competition ) );
					}
					competition = id2states.get( competitor ).get( state.element );
				  //competitors.add( competition );
					flag_added = false;
					for( int i=0; i<count_competition; i++ ){ if( competitors[i] == null ){
						competitors[i] = competition;
						flag_added     = true;
						break;
					} };
					if( ! flag_added ){ throw new IllegalStateException(); }
				}
				state.model.addItemListener( itemlistener );
			}
		}
		return this;
	}

	/** @since 20071217 */
	public ButtonModel fire( Semantics semantics, Object id, E element ){
		Map<E,State> state = models( semantics, id );
		if( state == null ) return null;
		ButtonModel model = state.get( element ).model;
        model.setArmed(    true );
        model.setPressed(  true );
        model.setPressed( false );
        model.setArmed(   false );
		return model;
	}

	/** Ensure listener is registered with button models only for the given elements. */
	public EnumModels<E> addItemListener( Semantics semantics, Object id, ItemListener listener, E ... elements ){
		Map<E,State> state = state( semantics, id, null );
		for( E element : elements ) ensureContains( state.get( element ).model, listener );
		return this;
	}

	/** Stop listening. Go deaf. */
	public EnumModels<E> removeItemListener( Semantics semantics, Object id, ItemListener listener ){
		Map<E,State> models = models( semantics, id );
		if( models == null ) return this;

		for( E element : clazz.getEnumConstants() ) models.get( element ).model.removeItemListener( listener );

		return this;
	}

	/** Return state only if it already exists.  Will not create virgin state. */
	private Map<E,State> models( Semantics semantics, Object id ){
		Map<Object,Map<E,State>> id2models = semantics2id2models.get( semantics );
		if( id2models == null ) return null;

		return id2models.get( id );
	}

	/** Add listener only if not already contained. */
	public static <T extends ButtonModel> T ensureContains( T model, ItemListener listener ){
		if( model instanceof ToggleButtonModel ){
			if( ! alreadyContains( (ToggleButtonModel) model, listener, ItemListener.class ) ){ model.addItemListener( listener ); }
		}
		else{
			model.removeItemListener( listener );
			model.   addItemListener( listener );
		}
		return model;
	}

	/** Does the model already contain the listener? */
	public static <T extends EventListener> boolean alreadyContains( ToggleButtonModel model, T listener, Class<T> listenerType ){
		if( (listener == null) || (model == null) ) return false;
		for( Object known : model.getListeners( listenerType ) ){ if( known == listener ){ return true; } }//contained == contained !! major bug
		return false;
	}

	/** A ComboModel/ListModel that has no state of its own.  Rather its behavior is completely
		dependant on ButtonModels, thus mirroring their selection state.

		@since 20071210 */
	public class ComboModel implements ComboBoxModel, ListModel, ItemListener{
		public ComboModel( Map<E,State> allstate ){
			this.states = new Object[ allstate.size() ];
			State state;
			for( E element : clazz.getEnumConstants() ){
				this.states[ element.ordinal() ] = state = allstate.get( element );
				state.model.addItemListener( this );
			}
		}

		/** Does not actually keep state, wrapper around EnumModels state. */
		public Object getElementAt( int index ){
			return ((State)states[ index ]).element;
		}

		/** Does not actually keep state, wrapper around EnumModels state. */
		public int getSize(){
			return states.length;
		}

		/** Keeps it's own collection of ListDataListeners. */
		public void    addListDataListener( ListDataListener listdatalistener ){
			LinkedList<ListDataListener> bucket = new LinkedList<ListDataListener>();
			if( listeners != null ){
				for( ListDataListener listener : listeners ){
					if( listener == listdatalistener ) listdatalistener = null;
					bucket.add( listener );
				}
			}
			if( listdatalistener != null ) bucket.add( listdatalistener );
			listeners = bucket.isEmpty() ? null : bucket.toArray( new ListDataListener[ bucket.size() ] );
		}

		/** Keeps it's own collection of ListDataListeners. */
		public void removeListDataListener( ListDataListener listdatalistener ){
			if( listeners == null ) return;
			LinkedList<ListDataListener> bucket = new LinkedList<ListDataListener>();
			for( ListDataListener listener : listeners ) if( listener != listdatalistener ) bucket.add( listener );
			listeners = bucket.isEmpty() ? null : bucket.toArray( new ListDataListener[ bucket.size() ] );
		}

		/** Fire ListDataListeners. */
		protected ComboModel fireChange(){
			if( listeners == null ) return this;
			ListDataEvent event = new ListDataEvent( this, ListDataEvent.CONTENTS_CHANGED, 0, states.length );
			for( ListDataListener listener : listeners ) listener.contentsChanged( event );
			return this;
		}

		/** Fire ListDataListeners if a button was selected. */
		public void itemStateChanged( ItemEvent itemevent ){
			if( itemevent.getStateChange() == ItemEvent.SELECTED ) fireChange();
		}

		/** Note: Linear time in the number of items. */
		public Object getSelectedItem(){
			for( Object state : states ) if( ((State)state).model.isSelected() ) return ((State)state).element;
			return null;
		}

		/** Note: Linear time in the number of items. */
		public void setSelectedItem( Object item ){
		  //System.out.println( "setSelectedItem("+item+")" );
			for( Object state : states ) if( ((State)state).element == item ) ((State)state).model.setSelected( true );
		}

		private    Object[]             states;
		private    ListDataListener[]   listeners;
	}

	/** Keep track of models and actions we've already created. */
	public class State{
		public State( E element, Semantics semantics, Object id, ButtonModel model, javax.swing.Action action ){
			this.element                  = element;
			this.semantics                = semantics;
			this.id                       = id;
			this.model                    = model;
			this.action                   = action;
		}

		public ComboModel listmodel(){
			if( this.listmodel == null ){
				Map<E,State> state      = models( exclusive, this.id );
				ComboModel   combomodel = new ComboModel( state );
				for( E element : clazz.getEnumConstants() ) state.get( element ).listmodel = combomodel;
			}
			return this.listmodel;
		}

		public String toString(){ return id(this); }

		public  final   E                   element;
		public  final   Semantics           semantics;
		public  final   Object              id;
		public  final   ButtonModel         model;
		public  final   javax.swing.Action  action;
		private         ComboModel          listmodel;
	}

	private                        Class<E>         clazz;
	private                     Modeller<E>         modeller;
	private Map<Semantics,Map<Object,Map<E,State>>> semantics2id2models = new EnumMap<Semantics,Map<Object,Map<E,State>>>( Semantics.class );

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

	/** Showcase the full range of possible ways of selecting things from the three-person set of musketeers.
		Especially useful for testing the response of the combo box to the radio buttons and vice versa. */
	public static class Test
	{
		enum Musketeer implements Actionable<Musketeer>{
			athos   ( "<html><font color='#cc0000'>The oldest by some years, Athos is a father figure to the other musketeers." ),
			porthos ( "<html><font color='#00cc00'>Porthos, honest and slightly gullible, is the extrovert of the group." ),
			aramis  ( "<html><font color='#0000cc'>Aramis seems to be followed by luck, but it is never enough." );

			private Musketeer( String html ){
				properties.put(     display, name() );
				properties.put(     tooltip, html   );
				properties.put( accelerator, firstUniqueKeyStroke( name() ) );
			}

			public  Musketeer          getDefault(){ return porthos; }
			public  Object                      get( Property property ){ return this.properties.get( property ); }
			private Map<Property,Object> properties = new EnumMap<Property,Object>( Property.class );

			private static EnumModels<Musketeer> MODELS = new EnumModels<Musketeer>( Musketeer.class );
		}

		Component strut(){
			return true ? createHorizontalStrut( 0x10 ) : new JLabel( "strut-" );
		}

		int mainImpl( String[] args ) throws Exception{
			edu.ucla.belief.ui.util.Util.setLookAndFeel();

			for( Semantics semantics : Semantics.values() ){
				report.put( semantics, new JLabel() );
			}
			reaction.put( exclusive, new ItemListener(){
				public void itemStateChanged( ItemEvent event ){
					if( event == null || event.getStateChange() == ItemEvent.SELECTED ){
						bucket.clear();
						report.get( exclusive ).setText( Musketeer.MODELS.selected( exclusive, id, bucket ).toString() );
					}
				}
			} );
			reaction.put( additive, new ItemListener(){
				public void itemStateChanged( ItemEvent event ){
					bucket.clear();
					report.get( additive ).setText( Musketeer.MODELS.selected( additive, id, bucket ).toString() );
				}
			} );

			Musketeer[]   musketeers = Musketeer.values();

			AbstractButton[]  radios = Musketeer.MODELS.newButtons(     radio, id, reaction.get(  radio.semantics ) ),
			                   boxes = Musketeer.MODELS.newButtons(       box, id, reaction.get(    box.semantics ) ),
			                 toggles = Musketeer.MODELS.newButtons(    toggle, id, reaction.get( toggle.semantics ) ),
			                  nillas = Musketeer.MODELS.newButtons(     nilla, id, reaction.get(  nilla.semantics ) );

			JMenuBar         menubar = new JMenuBar();
			JMenu menu;
			for( Semantics semantics : Semantics.values() ){
				                menu = menubar.add(      new JMenu( semantics   .name()           ) );
			  for( AbstractButton ab : Musketeer.MODELS.newButtons( semantics.forMenu(), id, reaction.get( semantics ) ) ) menu.add( ab );
			}

			JPanel             pnl   = new JPanel( new GridBagLayout() );
			GridBagConstraints c     = new GridBagConstraints();
			c.anchor                 = NORTHWEST;
			c.fill                   = HORIZONTAL;

			c.gridheight             = musketeers.length;
			pnl.add( Musketeer.MODELS.newComboBox( id, reaction.get( exclusive ) ), c );
			pnl.add(                                  strut(), c );
			c.gridheight             = 1;

			int ordinal;
			for( Musketeer musketeer : musketeers ){
				ordinal              = musketeer.ordinal();

				c.gridwidth          = 1;
				pnl.add(  radios[ ordinal ], c );
				pnl.add(            strut(), c );
				pnl.add(   boxes[ ordinal ], c );
				pnl.add(            strut(), c );
				pnl.add( toggles[ ordinal ], c );
				pnl.add(            strut(), c );
				c.gridwidth          = REMAINDER;
				pnl.add(  nillas[ ordinal ], c );
			}

			for( Semantics semantics : Semantics.values() ){
				c.gridwidth          = REMAINDER;
				pnl.add(                       Box.createVerticalStrut( 8 ), c );
				c.gridwidth          = 2;
				pnl.add( new JLabel( "<html><b>" + semantics.name() + ":" ), c );
				pnl.add(                                            strut(), c );
				c.gridwidth          = REMAINDER;
				pnl.add(                            report.get( semantics ), c );
			}

			for( ItemListener il : reaction.values() ){ il.itemStateChanged( null ); }

			pnl.setBorder( BorderFactory.createEmptyBorder( 2, 4, 2, 4 ) );

			JFrame             frame = new JFrame( "enummodels -- test/debug" );
			frame.add( pnl );
			frame.setJMenuBar( menubar );
			frame.pack();
			edu.ucla.belief.ui.util.Util.centerWindow( frame );
			frame.setVisible( true );

			while( frame.isVisible() ) Thread.sleep( 0x200 );

			return 0;
		}

	  /*public void itemStateChanged( ItemEvent event ){
			if( event == null || event.getStateChange() == ItemEvent.SELECTED ){
				for( Semantics semantics : Semantics.values() ){
					bucket.clear();
					report.get( semantics ).setText( Musketeer.MODELS.selected( semantics, id, bucket ).toString() );
				}
			}
		}*/

		Object                       id        = new Object();//{ public String toString(){ return id(this); } };
		Map<Semantics,JLabel>        report    = new EnumMap<Semantics,JLabel      >( Semantics.class );
		Map<Semantics,ItemListener>  reaction  = new EnumMap<Semantics,ItemListener>( Semantics.class );
		Set<Musketeer>               bucket    = EnumSet.allOf( Musketeer.class );
	}
}
