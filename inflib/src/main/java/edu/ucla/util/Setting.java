package edu.ucla.util;

import java.util.Map;

/** A Setting is key identifying a single configurable value.
	Declare an Enum to implement Setting.

	@author keith cascio
	@since  20091207 */
public interface Setting{
	/** A map of Setting keys to arbitrary values.

		@author keith cascio
		@since  20091207 */
	public interface Settings<E extends     Enum<E> & Setting> extends UserObject, ChangeBroadcaster{
		public       Settings<E>  copy( Settings<E>   toCopy  );
		public       Object       get(           E    setting );
		public       Settings<E>  put(           E    setting, Object value );
		public       Settings<E>  killState();
		public          Class<E>  clazz();
		public       Object       get(           E    setting, PropertyKey key );
		public       Object       put(           E    setting, PropertyKey key, Object value );
		public Map<PropertyKey,Object> snapshot( E    setting );
	}
	public                 Object         get(                 PropertyKey key );
	public Map<PropertyKey,Object> properties();
}
