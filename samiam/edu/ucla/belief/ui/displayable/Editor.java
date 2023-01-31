package edu.ucla.belief.ui.displayable;

import        edu.ucla.belief.ui.util.WholeNumberField;
import        edu.ucla.belief.ui.util.DecimalField;
import        edu.ucla.belief.ui.util.Dropdown;
import        edu.ucla.belief.ui.util.Dropdown.Flash;

import        java.awt.*;
import        java.awt.event.*;
import static java.awt.event.KeyEvent.getKeyModifiersText;
import static java.awt.event.KeyEvent.getKeyText;
import        javax.swing.*;
import        javax.swing.event.*;
import        javax.swing.text.JTextComponent;
import        java.util.*;
import        javax.swing.KeyStroke;

import        edu.ucla.util. PropertyKey;
import static edu.ucla.util. PropertyKey.*;

/** general support for GUI editing of simple data types: numbers and enumeral choices

	@author keith cascio
	@since  20080226 */
public enum Editor{
	integral   (   Integer.TYPE, Long.TYPE, Short.TYPE, Integer.class, Long.class, Short.class, java.util.concurrent.atomic.AtomicInteger.class, java.util.concurrent.atomic.AtomicLong.class, java.math.BigInteger.class ){
		public JComponent component( Map<PropertyKey,Object> properties ){
			boolean notext = Boolean.TRUE.equals( properties.get( PropertyKey.notext ) );
			return  notext ? componentNoText( properties ) : componentText( properties );
		}

		public JComponent componentNoText( Map<PropertyKey,Object> properties ){
			final JSlider       slider = new JSlider( 0, 8, 0 );
			slider.setPaintTicks(     true );
			slider.setMinorTickSpacing(  1 );
			slider.setMajorTickSpacing( 10 );
			slider.setSnapToTicks(    true );
			Number               floor = (Number)         properties.get( PropertyKey.floor     );
			Number             ceiling = (Number)         properties.get( PropertyKey.ceiling   );
			final ActionListener    al = (ActionListener) properties.get( actionlistener        );
			KeyStroke           stroke = (KeyStroke)      properties.get( keystroke             );
			Number           increment = (Number)         properties.get( PropertyKey.increment );
			if( increment == null ){ increment = 1; }
			if( (floor != null) && (ceiling != null) && (ceiling.intValue() > floor.intValue()) ){ slider.setMinimum( floor.intValue() ); slider.setMaximum( ceiling.intValue() ); }
			if(     al != null ){ slider.addChangeListener( new ChangeListener(){
				public void stateChanged( ChangeEvent e ){
					al.actionPerformed( new ActionEvent( e.getSource(), 0, "Editor.integral.componentNoText" ) );
				}
			} ); }
			if( stroke != null ){
				final int    inc       = increment.intValue();
				final String strstroke = toString( stroke );
				slider.setToolTipText( "<html>increment by "+inc+": <b>" + strstroke );
				final ChangeListener cl = new ChangeListener(){
					public void stateChanged( ChangeEvent e ){
						slider.setToolTipText( "<html>" + slider.getValue() + ", increment by "+inc+": <b>" + strstroke );
					}
				};
				Action       action = new AbstractAction(){
					public void actionPerformed( ActionEvent event ){
						if( event.getActionCommand() == STR_COMMAND_KEY_BINDING ){
							slider.setValue( slider.getValue() + inc );
							if( flash == null ){ flash = new Flash( slider ); }
							flash.start();
						}
					  //cl.stateChanged( null );
					}
					public Flash flash;
				};
				slider.registerKeyboardAction( action, STR_COMMAND_KEY_BINDING, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW );//stroke(   slider, stroke, action );
				slider.addChangeListener( cl );
			}
			return slider;
		}

		public JComponent componentText( Map<PropertyKey,Object> properties ){
			final WholeNumberField wnf = new WholeNumberField( 0, 8 );
			Number               floor = (Number)         properties.get( PropertyKey.floor     );
			Number             ceiling = (Number)         properties.get( PropertyKey.ceiling   );
			ActionListener          al = (ActionListener) properties.get( actionlistener        );
			KeyStroke           stroke = (KeyStroke)      properties.get( keystroke             );
			Number           increment = (Number)         properties.get( PropertyKey.increment );
			if( increment == null ){ increment = 1; }
			if( (floor != null) && (ceiling != null) && (ceiling.intValue() > floor.intValue()) ){ wnf.setBoundsInclusive( floor.intValue(), ceiling.intValue() ); }
			if(     al != null ){ wnf.addActionListener( al ); }
			if( stroke != null ){
				final int    inc       = increment.intValue();
				final String strstroke = toString( stroke );
				wnf.setToolTipText( "<html>increment by "+inc+": <b>" + strstroke );
				Action       action = new AbstractAction(){
					public void actionPerformed( ActionEvent event ){
						if( event.getActionCommand() == STR_COMMAND_KEY_BINDING ){
							wnf.setValue( wnf.getValue() + inc );
							if( flash == null ){ flash = new Flash( wnf ); }
							flash.start();
						}
						wnf.setToolTipText( "<html>" + wnf.getValue() + ", increment by "+inc+": <b>" + strstroke );
					}
					public Flash flash;
				};
				wnf.registerKeyboardAction( action, STR_COMMAND_KEY_BINDING, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW );//stroke(   wnf, stroke, action );
				wnf.addActionListener( action );
			}
			return wnf;
		}

		public Object          read( JComponent   comp               ){
			if(      comp instanceof WholeNumberField ){ return new Integer( ((WholeNumberField)comp).getValue() ); }
			else if( comp instanceof JSlider          ){ return new Integer( (         (JSlider)comp).getValue() ); }
			else return null;
		}
		public Object     writeImpl( JComponent   comp, Object value ){
			int intvalue = ((Number)value).intValue();
			if(      comp instanceof WholeNumberField ){ ((WholeNumberField)comp).setValue( intvalue ); }
			else if( comp instanceof JSlider          ){ (         (JSlider)comp).setValue( intvalue ); }
			return value;
		}
		/** @since 20091208 */
		public int             snap( JComponent   comp, Map<PropertyKey,Object> snapshot ){
			int count = 0;
			Number min = (Number) snapshot.get( PropertyKey.floor ), max = (Number) snapshot.get( PropertyKey.ceiling );

			if(      comp instanceof WholeNumberField ){
				WholeNumberField wnf = (WholeNumberField) comp;
				if( (min != null) && (wnf.getMinValue() != min.intValue()) ){ ++count; wnf.setMinValue( min.intValue() ); }
				if( (max != null) && (wnf.getMaxValue() != max.intValue()) ){ ++count; wnf.setMaxValue( max.intValue() ); }
			}else if( comp instanceof JSlider          ){
				JSlider          sly = (JSlider)          comp;
				if( (min != null) && (sly.getMinimum()  != min.intValue()) ){ ++count; sly.setMinimum(  min.intValue() ); }
				if( (max != null) && (sly.getMaximum()  != max.intValue()) ){ ++count; sly.setMaximum(  max.intValue() ); }
			}
			return count;
		}
	},
	fractional (    Double.TYPE, Float.TYPE, Double.class, Float.class, java.math.BigDecimal.class ){
		public JComponent component( Map<PropertyKey,Object> properties ){
			final DecimalField      df = new DecimalField( 0.0, 0x18 );
			Number               floor = (Number)         properties.get( PropertyKey.floor   );
			Number             ceiling = (Number)         properties.get( PropertyKey.ceiling );
			ActionListener          al = (ActionListener) properties.get( actionlistener      );
			KeyStroke           stroke = (KeyStroke)      properties.get( keystroke           );
			if( (floor != null) && (ceiling != null) && (ceiling.doubleValue() > floor.doubleValue()) ){ df.setBoundsInclusive( floor.doubleValue(), ceiling.doubleValue() ); }
			if( al != null ){ df.addActionListener( al ); }
			if( stroke != null ){
				final String strstroke = toString( stroke );
				df.setToolTipText( "<html>square: <b>" + strstroke );
				Action       action = new AbstractAction(){
					public void actionPerformed( ActionEvent event ){
						if( event.getActionCommand() == STR_COMMAND_KEY_BINDING ){
							double val = df.getValue();
							df.setValue( val * val );
							if( flash == null ){ flash = new Flash( df ); }
							flash.start();
						}
						df.setToolTipText( "<html>" + df.getValue() + ", square: <b>" + strstroke );
					}
					public Flash flash;
				};
				df.registerKeyboardAction( action, STR_COMMAND_KEY_BINDING, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW );//stroke(   df, stroke, action );
				df.addActionListener( action );
			}
			return df;
		}

		public Object          read( JComponent   comp               ){ return new Double( ((DecimalField)comp).getValue() ); }
		public Object     writeImpl( JComponent   comp, Object value ){
			((DecimalField)comp).setValue( ((Number)value).doubleValue() );
			return value;
		}
		/** @since 20091208 */
		public int             snap( JComponent   comp, Map<PropertyKey,Object> snapshot ){
			int  count = 0;
			Number min = (Number) snapshot.get( PropertyKey.floor ), max = (Number) snapshot.get( PropertyKey.ceiling );
			if(      comp instanceof DecimalField ){
				DecimalField dcf = (DecimalField) comp;
				if( (min != null) && (dcf.  getFloor() != min.intValue()) ){ ++count; }
				if( (max != null) && (dcf.getCeiling() != max.intValue()) ){ ++count; }
				if( count > 0 ){ dcf.setBoundsInclusive( min == null ? dcf.getFloor() : min.intValue(), max == null ? dcf.getCeiling() : max.intValue() ); }
			}
			return count;
		}
	},
	binary     (   Boolean.TYPE, Boolean.class ){
		public JComponent component( Map<PropertyKey,Object> properties ){
			final JCheckBox     jcb = new JCheckBox();
			ActionListener       al = (ActionListener) properties.get( actionlistener   );
			KeyStroke        stroke = (KeyStroke)      properties.get( keystroke        );
			if( stroke != null ){
				final String strstroke = toString( stroke );
				jcb.setToolTipText( "<html>toggle: <b>" + strstroke );
				jcb.setText( strstroke );
				Action       action = new AbstractAction(){
					public void actionPerformed( ActionEvent event ){
						if( event.getActionCommand() == STR_COMMAND_KEY_BINDING ){
							jcb.doClick();
							if( flash == null ){ flash = new Flash( jcb ); }
							flash.start();
						}
						String descrip = jcb.isSelected() ? "<font color='#006600'>on</font>" : "<font color='#990000'>off</font>";
						jcb.setToolTipText( "<html>" + descrip + ", toggle: <b>" + strstroke );
					}
					public Flash flash;
				};
				jcb.registerKeyboardAction( action, STR_COMMAND_KEY_BINDING, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW );//stroke(   jcb, stroke, action );
				jcb.addActionListener( action );
			}
			if( al != null ){ jcb.addActionListener( al ); }
			return jcb;
		}

		public Object          read( JComponent   comp               ){ return new Boolean( ((JCheckBox)comp).isSelected() ); }
		public Object     writeImpl( JComponent   comp, Object value ){
			((JCheckBox)comp).setSelected( ((Boolean)value).booleanValue() );
			return value;
		}
	},
	textual    ( Character.TYPE, Byte.TYPE, Character.class, Byte.class, String.class ){
		public JComponent component( Map<PropertyKey,Object> properties ){
			JTextField         jtf = new JTextField();
			ActionListener      al = (ActionListener) properties.get( actionlistener   );
			if( al != null ){ jtf.addActionListener( al ); }
			return jtf;
		}

		public Object          read( JComponent   comp               ){ return ((JTextComponent)comp).getText(); }
		public Object     writeImpl( JComponent   comp, Object value ){
			((JTextComponent)comp).setText( value.toString() );
			return value;
		}
	},
	enumerable ( Enum.class ){
		public JComponent component( Map<PropertyKey,Object> properties ){
			Enum             target = (Enum)           properties.get( defaultValue        );
			Number            floor = (Number)         properties.get( PropertyKey.floor   );
			Number          ceiling = (Number)         properties.get( PropertyKey.ceiling );
			ActionListener       al = (ActionListener) properties.get( actionlistener      );
			KeyStroke        stroke = (KeyStroke)      properties.get( keystroke           );
			Collection<?>      doma = (Collection<?>)  properties.get( domain              );
			Object[]         values = doma == null ? target.getDeclaringClass().getEnumConstants() : doma.toArray( new Object[]{ doma.size() } );
			if( (floor != null) && (ceiling != null) && (ceiling.intValue() > floor.intValue()) ){
				int floorEffective = Math.max(   floor.intValue(), 0 ),
					ceiliEffective = Math.min( ceiling.intValue(), values.length );
				Object[] range = new Object[ ceiliEffective - floorEffective ];
				for( int i=0; i<range.length; i++ ){ range[i] = values[i+floorEffective]; }
				values = range;
			}
			final JComboBox jcb = new JComboBox( values );
			if( stroke != null ){
				final String strstroke = toString( stroke );
				final Object[]   items = values;
				jcb.setToolTipText( "<html>cycle: <b>" + strstroke );
				Action       action = new AbstractAction(){
					public void actionPerformed( ActionEvent event ){
						if( event.getActionCommand() == STR_COMMAND_KEY_BINDING ){
							Object item  = jcb.getSelectedItem();
							int       i  = 0;
							for(    ; i <  items.length; i++ ){ if( items[i] == item ){ break; } }
							if(     ++i >= items.length      ){ i = 0; }
							jcb.setSelectedItem( items[i] );
							if( flash == null ){ flash = new Flash( jcb ); }
							flash.start();
						}
						jcb.setToolTipText( "<html>" + jcb.getSelectedItem().toString() + ", cycle: <b>" + strstroke );
					}
					public Flash flash;
				};
				jcb.registerKeyboardAction( action, STR_COMMAND_KEY_BINDING, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW );
				jcb.addActionListener( action );
			}
			if( al != null ){ jcb.addActionListener( al ); }
			return jcb;
		}

		public Object          read( JComponent   comp               ){ return ((JComboBox)comp).getSelectedItem(); }
		public Object     writeImpl( JComponent   comp, Object value ){
			((JComboBox)comp).setSelectedItem( value );
			return value;
		}
		/** @since 20091208 */
		public int             snap( JComponent   comp, Map<PropertyKey,Object> snapshot ){
			int               count = 0;
			Enum             target = (Enum)           snapshot.get( defaultValue        );
			Number            floor = (Number)         snapshot.get( PropertyKey.floor   );
			Number          ceiling = (Number)         snapshot.get( PropertyKey.ceiling );
			Collection<?>      doma = (Collection<?>)  snapshot.get( domain              );
			Object[]         values = doma == null ? target.getDeclaringClass().getEnumConstants() : doma.toArray( new Object[]{ doma.size() } );
			if( (floor != null) && (ceiling != null) && (ceiling.intValue() > floor.intValue()) ){
				int floorEffective = Math.max(   floor.intValue(), 0 ),
					ceiliEffective = Math.min( ceiling.intValue(), values.length );
				Object[] range = new Object[ ceiliEffective - floorEffective ];
				for( int i=0; i<range.length; i++ ){ range[i] = values[i+floorEffective]; }
				values = range;
			}
			if(      comp instanceof JComboBox ){
				JComboBox        jcb = (JComboBox) comp;
				ComboBoxModel  model = jcb.getModel();
				boolean diff = model.getSize() != values.length;
				if(   ! diff ){   for( int i=0; i<values.length; i++ ){ if( values[i] != model.getElementAt(i) ){ diff = true; break; } } }
				if(     diff ){
					count += 3;
					jcb.setModel( new DefaultComboBoxModel( values ) );
				}
			}
			return count;
		}
	},
	multienumerable ( EnumSet.class ){
		@SuppressWarnings( "unchecked" )
		public JComponent component( Map<PropertyKey,Object> properties ){
			ActionListener          al = (ActionListener) properties.get( actionlistener      );
			Number               floor = (Number)         properties.get( PropertyKey.floor   );
			Number             ceiling = (Number)         properties.get( PropertyKey.ceiling );
			EnumSet           estarget = (EnumSet)        properties.get( defaultValue        );
			String              plural = (String)         properties.get( PropertyKey.plural  );
			if( plural == null ){ plural = "values"; }
			Enum               element = estarget.isEmpty() ? (Enum) EnumSet.complementOf( estarget ).iterator().next() : (Enum) estarget.iterator().next();
			Collection<?>         doma = (Collection<?>)  properties.get( domain              );
			Object[]            values = doma == null ? element.getDeclaringClass().getEnumConstants() : doma.toArray( new Object[]{ doma.size() } );
			Dropdown<Object>  dropdown = new Dropdown<Object>( values, new int[]{ floor.intValue(), ceiling.intValue() }, plural );
			if( al != null ){ dropdown.addActionListener( al ); }
			return            dropdown;
		}
		@SuppressWarnings( "unchecked" )
		public Object          read( JComponent   comp               ){
			Dropdown<Object> dropdown =  (Dropdown<Object>) comp;
			return dropdown.getSelection( dropdown.emptyBucket() );
		}
		@SuppressWarnings( "unchecked" )
		public Object     writeImpl( JComponent   comp, Object value ){
			((Dropdown<Object>)comp).setSelection( (Collection<Object>) value );
			return value;
		}
		/** @since 20091208 */
		@SuppressWarnings( "unchecked" )
		public int             snap( JComponent   comp, Map<PropertyKey,Object> snapshot ){
			int                  count = 0;
			Number               floor = (Number)         snapshot.get( PropertyKey.floor   );
			Number             ceiling = (Number)         snapshot.get( PropertyKey.ceiling );
			EnumSet           estarget = (EnumSet)        snapshot.get( defaultValue        );
			String              plural = (String)         snapshot.get( PropertyKey.plural  );
			if( plural == null ){ plural = "values"; }
			Enum               element = estarget.isEmpty() ? (Enum) EnumSet.complementOf( estarget ).iterator().next() : (Enum) estarget.iterator().next();
			Collection<?>         doma = (Collection<?>)  snapshot.get( domain              );
			Object[]            values = doma == null ? element.getDeclaringClass().getEnumConstants() : doma.toArray( new Object[]{ doma.size() } );
			if(      comp instanceof Dropdown ){
				Dropdown<Object> jcb = (Dropdown<Object>) comp;
				ComboBoxModel  model = jcb.getModel();
				boolean diff = model.getSize() != values.length;
				if(   ! diff ){   for( int i=0; i<values.length; i++ ){ if( values[i] != model.getElementAt(i) ){ diff = true; break; } } }
				if(     diff ){
					count += 4;
					jcb.reset( values, new int[]{ floor.intValue(), ceiling.intValue() }, plural );
				}
			}
			return count;
		}
	};

	/** @since 20080228 */
	public static final String
	  STR_COMMAND_KEY_BINDING = "edu.ucla.belief.ui.displayable.Editor.STR_COMMAND_KEY_BINDING";

	/** @since 20080228 */
  /*static public JComponent stroke( JComponent comp, KeyStroke stroke, Action action ){
		int cond = JComponent.WHEN_IN_FOCUSED_WINDOW;
		InputMap   inputmap =  comp.getInputMap( cond ) == null ? new ComponentInputMap( comp ) : comp .getInputMap( cond );
		inputmap .put( stroke, stroke );
		ActionMap actionmap = comp.getActionMap()       == null ? new ActionMap()               : comp.getActionMap();
		actionmap.put( stroke, action );
		comp  .setInputMap( cond, inputmap );
		comp .setActionMap(      actionmap );
		return comp;
	}*/

	/** @since 20080228 */
	static public String toString( KeyStroke stroke ){
		String                         text  = getKeyModifiersText( stroke.getModifiers() );
		if(      text      == null ){  text  =  ""; }
		else if( text.length() > 0 ){  text += "+"; }
		{                              text += getKeyText( stroke.getKeyCode() ); }
		return   text;
	}

	private Editor( Class ... targets ){
		this.targets        = targets;
	}

	static public Editor forTarget( Object obj ){
		Class target = obj.getClass();
		for( Editor editor : values() ){
			for( Class<?> clazz : editor.targets ){
				if( clazz.isAssignableFrom( target ) ){ return editor; }
			}
		}
		return null;
	}

	/** @since 20080228 */
  /*public JComponent component(                 Object ... properties   ){
		Map<PropertyKey,Object> map  = new EnumMap<PropertyKey,Object>( PropertyKey.class );
		PropertyKey[]           keys = PropertyKey.values();
		int i=0;
		for( Object value : properties ){
			if( value != null ){ map.put( keys[ i ], value ); }
			++i;
		}
		return this.component( map );
	}*/
	public JComponent component( Map<PropertyKey,Object>   properties ){ return  null; }
	public Object          read( JComponent   comp                    ){ return  null; }
	final public Object   write( JComponent   comp, Object      value ){
		Class<?> clazz = value.getClass();
		for( Class<?> target : targets ){
			if( target.isAssignableFrom( clazz ) ){ return writeImpl( comp, value ); }
		}
		throw new IllegalArgumentException();
	}
	protected Object  writeImpl( JComponent   comp, Object value ){ return value; }

	/** @since 20091208 */
	public int             snap( JComponent   comp, Map<PropertyKey,Object> snapshot ){ return 0; }

	final public Class<?>[] targets;
}
