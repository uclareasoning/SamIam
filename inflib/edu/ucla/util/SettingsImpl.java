package edu.ucla.util;

import edu.ucla.util.Setting.Settings;

import        java.util.*;

/** Reference implementation of Setting.Settings.

	@author keith cascio
	@since  20091207 */
public class SettingsImpl<E extends Enum<E> & Setting> implements UserObject, ChangeBroadcaster, Settings<E>
{
	public SettingsImpl( Class<E> clazz ){
		this.myMap = new EnumMap<E,Object>( this.clazz = clazz );
		for( E setting : clazz.getEnumConstants() ){ myMap.put( setting, setting.get( PropertyKey.defaultValue ) ); }
	}

	public Class<E>                              clazz(){ return this.clazz; }

	public Object                                get( E setting ){ return myMap.get( setting ); }

	public SettingsImpl<E>                       put( E setting, Object value ){
		Class<?> legal = (Class<?>) setting.get( PropertyKey.legal );
		if( ! legal.isAssignableFrom( value.getClass() ) ){ throw new IllegalArgumentException( value.getClass().getSimpleName() + " != " + legal.getSimpleName() ); }
		myMap.put( setting, value );
		return this;
	}

	public SettingsImpl<E> killState(){ return this; }

	/** interface ChangeBroadcaster */
	public ChangeBroadcaster fireSettingChanged(){
		if( myChangeListeners == null ){ return this; }

		myChangeListeners.cleanClearedReferences();
		for( ChangeListener cl : (ChangeListener[]) myChangeListeners.toArray( new ChangeListener[ myChangeListeners.size() ] ) ){
			cl.settingChanged( this.event );
		}
		return this;
	}

	/** interface ChangeBroadcaster */
	public boolean    addChangeListener( ChangeListener listener ){
		if(    myChangeListeners == null ){ myChangeListeners = new WeakLinkedList(); }
		return myChangeListeners.contains( listener ) ? false : myChangeListeners.add( listener );
	}

	/** interface ChangeBroadcaster */
	public boolean removeChangeListener( ChangeListener listener ){
		return myChangeListeners != null ? myChangeListeners.remove( listener ) : false;
	}

	/** interface UserObject */
	public UserObject onClone(){
		return new SettingsImpl<E>( this.clazz ).copy( this );
	}

	public SettingsImpl<E> copy( Settings<E> tc ){
		SettingsImpl<E> toCopy = (SettingsImpl<E>) tc;

		boolean change = ! myMap.equals( toCopy.myMap );

		myMap.putAll( toCopy.myMap );

		if( toCopy.myMapMap != null ){
			WeakLinkedList cls = myChangeListeners;
			myChangeListeners  = null;
			try{
				for( Map.Entry<E,Map<PropertyKey,Object>> entry : toCopy.myMapMap.entrySet() ){
					if( entry.getValue() != null ){
						for( Map.Entry<PropertyKey,Object> pair : entry.getValue().entrySet() ){
							change |= (this.put( entry.getKey(), pair.getKey(), pair.getValue() ) != pair.getValue());
						}
					}
				}
			}finally{
				if( myChangeListeners != null ){ throw new IllegalStateException(); }
				myChangeListeners = cls;
			}
		}

		if( change ){ fireSettingChanged(); }

		return this;
	}

	public       Object       get(           E    setting, PropertyKey key ){
		if( setting  == null ){ return null; }
		Map<PropertyKey,Object> map = myMapMap == null ? null : myMapMap.get( setting );
		if( map != null && map.containsKey( key ) ){ return map.get( key ); }
		return setting.get( key );
	}

	public       Object       put(           E    setting, PropertyKey key, Object value ){
		if( myMapMap == null ){ myMapMap = new EnumMap<E,Map<PropertyKey,Object>>( this.clazz ); }
		Map<PropertyKey,Object> map = myMapMap.get( setting );
		if( map      == null ){ myMapMap.put( setting, map = new EnumMap<PropertyKey,Object>( PropertyKey.class ) ); }
		Object ret = map.put( key, value );
		if( ret != value ){ fireSettingChanged(); }
		return ret;
	}

	public Map<PropertyKey,Object> snapshot( E    setting ){
		Map<PropertyKey,Object> enummap = new EnumMap<PropertyKey,Object>( setting.properties() );
		Map<PropertyKey,Object>     map = myMapMap == null ? null : myMapMap.get( setting );
		if( map != null ){ enummap.putAll( map ); }
		return enummap;
	}

	private     Class<E>                            clazz;
	transient   final public ChangeEvent            event = new ChangeEventImpl().source( this );
	private       Map<E,Object>                     myMap;
	private       Map<E,Map<PropertyKey,Object>>    myMapMap;
	transient        private WeakLinkedList         myChangeListeners;
}
