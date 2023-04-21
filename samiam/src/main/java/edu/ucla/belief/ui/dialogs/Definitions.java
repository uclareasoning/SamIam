package edu.ucla.belief.ui.dialogs;

import        edu.ucla.belief.*;
import        edu.ucla.belief.ui.util.JOptionResizeHelper;
import        edu.ucla.belief.ui.util.JOptionResizeHelper.JOptionResizeHelperListener;
import        edu.ucla.belief.ui.util.Util;
import        edu.ucla.belief.ui.util.HyperLabel;
import        edu.ucla.belief.ui.util.DecimalField;
import        edu.ucla.belief.io.*;
import        edu.ucla.util.*;
import static edu.ucla.util.AbstractStringifier.VARIABLE_ID;
import        edu.ucla.belief.ui.actionsandmodes.SamiamAction;
import        edu.ucla.belief.ui.actionsandmodes.GrepAction;
import        edu.ucla.belief.ui.actionsandmodes.Grepable.Redirect.Dest;
import        edu.ucla.belief.ui.actionsandmodes.Grepable.Redirect.PrintStreamDest;

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

import static edu.ucla.belief.ui.dialogs.Definitions.Domain.*;

import        java.util.List;
import static java.util.Collections.*;
import        java.text.*;
import        java.awt.*;
import        java.awt.event.*;
import        javax.swing.*;
import        javax.swing.JToggleButton.ToggleButtonModel;
import        javax.swing.border.*;
import        javax.swing.event.*;
import        java.io.*;
import        java.util.*;
import        java.util.regex.*;

/** Define something by mapping each one of a
	set of '{@link Domain#dest destination}' elements to either a
	'{@link Domain#src_exclusive source}' element or a '{@link Domain#src_shared function}'.

	@param <D> the type of elements defined by this group of definitions, both '{@link Domain#dest destination}' and '{@link Domain#src_exclusive source}'.  Please define {@link #characterize characterize()} consistent with this category.
	@param <S> t
	@param <F> the type of '{@link Domain#src_shared function}' that can define a '{@link Domain#dest destination}' element.  Please define {@link #characterize characterize()} consistent with this category.

	@author keith cascio
	@since  20071205 */
public abstract class Definitions<D,S,F>
{
	/** all elements are of one kind or another */
	public enum Kind{
		/** can map to at most one other element: elements of the '{@link Domain#dest destination}' and '{@link Domain#src_exclusive source}' groups */
		exclusive,
		/** can map to an arbitrary number of elements: a '{@link Domain#src_shared function}' */
		shared,
		/** an element we {@link Domain#undefined don't know about} */
		undefined }

	/** Please define this method to correctly report which group a given element belongs to.<br />
		For example, to define a mapping from each 'girl' to either an exclusive 'boyfriend' or a 'passtime':<br /><br />
<pre>
{@link Definitions#Definitions Definitions}&lt;Girl,Boy,Passtime&gt; girls2boys = new {@link Definitions#Definitions Definitions}&lt;Girl,Boy,Passtime&gt;( girls, passtimes, boys, skiing ){
	public {@link Kind Kind} characterize( Object element ){
		if(      element instanceof Girl     ) return Kind.{@link Kind#exclusive   exclusive};
		else if( element instanceof Boy      ) return Kind.{@link Kind#exclusive   exclusive};
		else if( element instanceof Passtime ) return Kind.{@link Kind#shared      shared};
		else                                   return Kind.{@link Kind#undefined   undefined};
	}
}
</pre>
	*/
	abstract public Kind characterize( Object element );/*{
		if(      element instanceof D ) return Kind.exclusive;
		else if( element instanceof S ) return Kind.exclusive;
		else if( element instanceof F ) return Kind.shared;
		else                            return Kind.undefined;
	}*/

	/** Throughout, Definitions respects the order given by the iterators of the three domain Collections domain_dest, domain_src_shared and domain_src_exclusive.<br /><br />
		If &lt;F&gt; is an enumerated type, Definitions uses container classes EnumMap and EnumSet for better performance. */
	@SuppressWarnings( "unchecked" )
	public <K extends Enum<K>> Definitions(	Collection<D> domain_dest, Collection<F> domain_src_shared, Collection<S> domain_src_exclusive, F src_default ){
		this.domain_dest           =   new LinkedHashSet<D>( domain_dest          );
		this.domain_src_shared     =   new LinkedHashSet<F>( domain_src_shared    );
		this.array_src_shared      =   domain_src_shared.toArray();
		this.domain_src_exclusive  =   new LinkedHashSet<S>( domain_src_exclusive );
		this.src_default           =   domain( src_default ) == Domain.undefined ? null : src_default;

		this.domain_src_all        = new LinkedHashSet<Object>( domain_src_shared.size() + domain_src_exclusive.size() );
		this.domain_src_all.addAll( domain_src_shared    );
		this.domain_src_all.addAll( domain_src_exclusive );

	  /*F      domain_src_shared_0 = domain_src_shared.iterator().next();
		K                   enum_0 = domain_src_shared_0 instanceof Enum ? (K) domain_src_shared_0 : null;*/
		int size_dest              =                        this.domain_dest         .size();
		this.dest2src              = new HashMap<D,Object>( size_dest                        );
		this.src2dest_exclusive    = new HashMap<S,    D >( this.domain_src_exclusive.size() );
		this.src2dest_shared       = new HashMap<F,Set<D>>( this.domain_src_shared   .size() );
	  //this.src2dest_shared       = domain_src_shared.isEmpty() ? ((Map<F,Set<D>>) emptyMap()) : new HashMap<F,Set<D>>( this.domain_src_shared   .size() );
	  /*this.src2dest_shared       = enum_0 == null ?
		                             new HashMap<F,Set<D>>( this.domain_src_shared   .size() ) :
		             (Map<F,Set<D>>) new EnumMap<K,Set<D>>( enum_0.getClass()                );*/
		if( (! domain_src_shared.isEmpty()) && (domain_src_shared.iterator().next() instanceof Enum) && (this.src_default != null) ){
		      this.src2dest_shared.put( src_default, null );
		      this.src2dest_shared = (Map<F,Set<D>>) new EnumMap<K,Set<D>>( (Map<K,Set<D>>) this.src2dest_shared );
		}

		for( F shared : this.domain_src_shared  ) this.src2dest_shared.put( shared, new HashSet<D>( size_dest ) );

		this.   locked             = new LinkedHashSet<D>( size_dest );
		this.available             = new LinkedHashSet<S>( size_dest );
	}

	/** define what group(s) an element belongs to */
	public enum Domain{
		/** element is a '{@link Domain#src_shared function}' */
		src_shared{
			public <T,V,U> T         get( Definitions<T,V,U> defs, Object src2, T dest2 ){
				return null;
			}
			public <T,V,U> T         put( Definitions<T,V,U> defs, Object src2, T dest2 ){
				defs.src2dest_shared.get( src2 ).add( dest2 );
				return null;
			}
			public <T,V,U> T      remove( Definitions<T,V,U> defs, Object src1, T dest1 ){
			  //System.out.println( "src2dest_shared."+src1+".remove( "+dest1+" )" );
				defs.src2dest_shared.get( src1 ).remove( dest1 );
				return dest1;
			}
		},
		/** element is in the '{@link Domain#src_exclusive source}' group only */
		src_exclusive ( true ),
		/** element is in the '{@link Domain#src_exclusive source}' group and the '{@link Domain#dest destination}' group */
		intersection  ( true ),
		/** element is in the '{@link Domain#dest destination}' group only */
		dest          ( true ){
			public <T,V,U> T         get( Definitions<T,V,U> defs, Object src2, T dest2 ){ throw new UnsupportedOperationException(); }
			public <T,V,U> T         put( Definitions<T,V,U> defs, Object src2, T dest2 ){ throw new UnsupportedOperationException(); }
			public <T,V,U> T      remove( Definitions<T,V,U> defs, Object src1, T dest1 ){ throw new UnsupportedOperationException(); }
		},
		/** element is undefined */
		undefined{
			public <T,V,U> T         get( Definitions<T,V,U> defs, Object src2, T dest2 ){ return null; }
			public <T,V,U> T         put( Definitions<T,V,U> defs, Object src2, T dest2 ){ return null; }
			public <T,V,U> T      remove( Definitions<T,V,U> defs, Object src1, T dest1 ){ return null; }
		};

		private Domain(){ this( false ); }

		private Domain( boolean exclusive ){ this.exclusive = exclusive; }

		/** query the reverse mapping of src2 */
		public <T,V,U> T         get( Definitions<T,V,U> defs, Object src2          ){
			return this.get( defs, src2, null );
		}
		/** query the reverse mapping of src2 */
		public <T,V,U> T         get( Definitions<T,V,U> defs, Object src2, T dest2 ){
			return defs.src2dest_exclusive.get( src2 );
		}
		/** create a reverse mapping of src2 to dest2 */
		@SuppressWarnings( "unchecked" )
		public <T,V,U> T         put( Definitions<T,V,U> defs, Object src2, T dest2 ){
			return defs.src2dest_exclusive.put( (V) src2, dest2 );
		}
		/** remove the reverse mapping of src1 to dest1 */
		public <T,V,U> T      remove( Definitions<T,V,U> defs, Object src1, T dest1 ){
		  //System.out.println( "src2dest_exclusive.remove( "+src1+" )" );
			return defs.src2dest_exclusive.remove( src1 );
		}

		/** the element is not a '{@link Domain#src_shared function}', ie it can map to only one other element */
		public   final   boolean   exclusive;
	}

	/** query the what group an element belongs to */
	final public Domain domain( Object element )
	{
		switch( characterize( element ) ){
			case exclusive:
				boolean  dest = domain_dest         .contains( element );
				boolean   src = domain_src_exclusive.contains( element );
				if(      dest ) return src ? Domain.intersection : Domain.dest;
				else if(  src ) return       Domain.src_exclusive;
				break;
			case shared:
				if( domain_src_shared.contains( element ) ) return Domain.src_shared;
				break;
		}
		return Domain.undefined;
	}

  //public   Object                  get( D dest )
  //public   Definitions<D,S,F>   define( D dest2, Object src2 )
  //private  Definitions<D,S,F> undefine( D dest1, Object src1 )
  //public   Definitions<D,S,F> undefine( D dest1 )
  //public   Definitions<D,S,F>     lock( D dest )
  //public   Definitions<D,S,F>   unlock( D dest )

	/** query the definition of a '{@link Domain#dest destination}' element */
	public   Object                  get( D dest ){
		return dest2src.get( dest );
	}

	/** Define a '{@link Domain#dest destination}' element by mapping it to a '{@link Domain#src_exclusive source}' element or a '{@link Domain#src_shared function}'.
		@throws IllegalStateException if the definition of dest2 is locked. */
	public   Definitions<D,S,F>   define( D dest2, Object src2 ){
	  //System.out.println( "  define( "+dest2+", "+src2+" ){" );

		if( dest2src.get( dest2 ) == src2 ) return this;

		checkDomain( dest2 );

		Domain domain  = domain(     src2 );
		if(    domain == Domain.undefined ){ throw new IllegalArgumentException( src2 + " undefined (not a member of the shared domain)" ); }

		synchronized( dest2 ){
			checkLock( dest2 );

			D       dest1  = domain  .get( this, src2 );
			Object   src1  = dest2src.get(      dest2 );

			if(      src1 != null ) undefine( dest2, src1 );
			if(     dest1 != null )   define( dest1, src_default == null ? src1 : src_default );

			dest2src.put(      dest2,  src2 );
			domain  .put( this, src2, dest2 );

		  //System.out.println( "}" );
		}

		return fireContentsChanged();
	}

  //public   Definitions<D,S,F> println(){ System.out.println(); return this; }

	private  Definitions<D,S,F> undefine( D dest1, Object src1 ){
		synchronized( dest1 ){
			checkLock( dest1 );

			Object removed = null;
			if( dest1 != null )  removed         = this. dest2src.remove(             dest1 );
			if(  src1 != null && removed == src1 ) domain( src1 ).remove( this, src1, dest1 );
		  //System.out.println( "undefine( "+dest1+", "+src1+" ), removed? " + removed /*+ ", domain? " + domain + ", src1 != null? " + (src1 != null) + ", removed == dest1? " + (removed == dest1)*/ );
		}
		return this;
	}

	/** Destroy the definition of a '{@link Domain#dest destination}' element by deleting any mapping to a '{@link Domain#src_exclusive source}' element or '{@link Domain#src_shared function}'.
		@throws IllegalStateException if the definition of dest1 is locked. */
	public   Definitions<D,S,F> undefine( D dest1 ){
		return dest2src.containsKey( dest1 ) ? undefine( dest1, dest2src.get( dest1 ) ) : this;
	}

	/** throw an Exception if the definition of the '{@link Domain#dest destination}' element dest is locked */
	private Definitions<D,S,F> checkLock( D dest ){
		synchronized( dest ){
			if( this.locked.contains( dest ) ) throw new IllegalStateException( "definition of \"" + dest + "\" is locked!" );
		}
		return this;
	}

	/** query whether we are prohibited from editing ({@link #define defining} or {@link #undefine undefining}) the definition of a '{@link Domain#dest destination}' element
		@return true if the definition of the '{@link Domain#dest destination}' element dest is {@link #lock locked} */
	public   boolean          isLocked( D dest ){
		synchronized( dest ){
			return this.locked.contains( dest );
		}
	}

	/** throw an Exception if the '{@link Domain#dest destination}' element dest is not a member of the {@link Domain#dest domain} */
	private Definitions<D,S,F> checkDomain( D dest ){
		if( ! domain_dest    .contains( dest ) ) throw new IllegalArgumentException( dest + " undefined" );
		return this;
	}

	/** Lock the definition of a '{@link Domain#dest destination}' element so it cannot be {@link #define edited}.<br />
		Note: for Thread safety we synchronize on the '{@link Domain#dest destination}' element Object. */
	public   Definitions<D,S,F>     lock( D dest ){
		checkDomain( dest );

		synchronized( dest ){
			if( this.locked      .contains( dest ) ) return this;
			else{
				this.locked.add( dest );
				return fireAction( dest ).fireContentsChanged();
			}
		}
	}

	/** Unlock the definition of a '{@link Domain#dest destination}' element so it can be {@link #define edited}.<br />
		Note: for Thread safety we synchronize on the '{@link Domain#dest destination}' element Object. */
	public   Definitions<D,S,F>   unlock( D dest ){
		checkDomain( dest );

		synchronized( dest ){
			if( this.locked      .contains( dest ) ){
				this.locked.remove(         dest );
				return fireAction( dest ).fireContentsChanged();
			}
			else return this;
		}
	}

	/** for interface ButtonModel */
	public            boolean model_____________isSelected( D dest ){
		return this.locked.contains( dest );
	}

	/** for interface ButtonModel */
	public Definitions<D,S,F> model____________setSelected( D dest, boolean selected ){
		return selected ? this.lock( dest ) : this.unlock( dest );
	}

	/** for interface ListModel */
	public             Object model___________getElementAt( D dest, int index ){
		if(      index  < 0                       ) return null;
		if(      index  < array_src_shared.length ) return array_src_shared[ index ];

		Object           ret  = mycache.cache( dest, index );
		if(              ret != null ) return ret;

		Object           src = get( dest );
		boolean    exclusive = this.domain_src_exclusive.contains( src );
		boolean       locked = this.locked.contains( dest );
		boolean  unavailable = locked && exclusive;

		try{
			if(      index == array_src_shared.length && unavailable ) return ret = src;
			refresh();
			int        projected = index - array_src_shared.length;
			if( unavailable ) --projected;
			if(  projected  < 0                       ) throw new IllegalStateException();
			if(  projected >= array_available.length  ){
				this.append( new PrintStreamDest( System.err, null ), Format.debug, VARIABLE_ID );
				throw new ArrayIndexOutOfBoundsException( index );
			}

			return ret = this.array_available[ projected ];
		}finally{
			mycache.cache( dest, index, ret );
		}
	}

	/** Cache the list model for better performance especially when the {@link ListModelByDestination popup menu} is showing.<br />
		Also keep hit/miss statistics. */
	public class Cache
	{
		/** query the cache */
		public Object cache( D dest, int index ){
			Object ret = null;
			if( cache_destination == dest && index < cache.length ) ret = this.cache[ index ];
			if( ret == null ) ++misses;
			else              ++hits;
			return ret;
		}

		/** delete all cache entries */
		public void   clear(){
			int stop = Math.min( cache_max + 1, cache.length );
			for( int i=0; i<stop; i++ ) cache[i] = null;
			cache_max = -1;
		}

		/** add a cache entru */
		public Object cache( D dest, int index, Object value ){
			if(          index >= cache.length ) cache = new Object[ cache.length << 1 ];
			else if( cache_destination != dest ){
				clear();
				cache_destination = dest;
			}
			if( index > cache_max ) cache_max = index;
			return cache[ index ] = value;
		}

		private   Object[]   cache               =   new Object[ 1 << 5 ];
		private   D          cache_destination   =   null;
		private   int        cache_max           =     -1;
		private   int        hits = 0, misses = 0;
	}

	/** for interface ListModel */
	public                int model________________getSize( D dest ){
		refresh();
		Object src = get( dest );
		if( this.domain_src_shared.contains( src ) || this.available.contains( src ) ) return count;
		else return count + 1;
	}

	/** for interface ComboBoxModel */
	public             Object model________getSelectedItem( D dest ){
		return get( dest );
	}

	/** for interface ComboBoxModel */
	public Definitions<D,S,F> model________setSelectedItem( D dest, Object item ){
		if( ! auditDefinition( dest, item ) ){ return this; }
		return define( dest, item );
	}

	/** Override this method to prevent the user from making particular prohibited assignments.
		E.G. prohibit assigning conditions for variables with unequal cardinalities.
		This method should not throw any Exception.  But it should probably popup a dialog window
		explaining the prohibition to the user.

		@since 20071224 */
	public boolean auditDefinition( D dest, Object item ){ return true; }

	/** for interface ListModel */
	public Definitions<D,S,F> model____addListDataListener( D dest, ListDataListener listdatalistener ){
		if( myListeners == null ) myListeners = new LinkedList<ListenByDestination>();
		else{
			for( ListenByDestination lbd : myListeners ){
				if( lbd.listener == listdatalistener ){
					lbd.destination = dest;
					return this;
				}
			}
		}
		myListeners.add( new ListenByDestination( dest, listdatalistener ) );
		return this;
	}

	/** for interface ListModel */
	public Definitions<D,S,F> model_removeListDataListener( D dest, ListDataListener listdatalistener ){
		if( myListeners == null ) return this;

		for( Iterator<ListenByDestination> iterator = myListeners.iterator(); iterator.hasNext(); ){
			if( iterator.next().listener == listdatalistener ) iterator.remove();
		}

		return this;
	}

	/** an Action that represents the enabled state for one '{@link Domain#dest destination}' element, ie locking */
	public class ActionByDestination extends AbstractAction implements Action
	{
		public ActionByDestination( D destination, Component[] to_disable ){
			this.destination = destination;
			this.to_disable  = to_disable;
		}

		public void setEnabled( boolean enabled ){
			super.setEnabled( enabled );
			if( to_disable != null ) for( Component component : to_disable ) component.setEnabled( enabled );
		}

		public void actionPerformed( ActionEvent event ){}

		private       D                destination;
		private       Component[]      to_disable;
	}

	/** get an Action that represents the enabled state for one '{@link Domain#dest destination}' element */
	public Action getAction( D dest ){
		return getAction( dest, null );
	}

	/** get an Action that represents the enabled state for one '{@link Domain#dest destination}' element
		@param to_disable an optional array of gui Components that should mimic the enabled state of the Action. */
	public Action getAction( D dest, Component[] to_disable )
	{
		ActionByDestination ret = null;
		if(      this.actions == null ) this.actions = new HashMap<D,ActionByDestination>( this.domain_dest.size() );
		else if( this.actions.containsKey( dest ) ) ret = this.actions.get( dest );

		if( ret == null ) this.actions.put( dest, ret = new ActionByDestination( dest, to_disable ) );

		this.fireAction( dest );
		return ret;
	}

	/** update the Action  that represents the enabled state for one '{@link Domain#dest destination}' element */
	public Definitions<D,S,F> fireAction( D dest ){
		if( this.actions == null ) return this;

		try{
			ActionByDestination abd = this.actions.get( dest );
			if( abd != null ) abd.setEnabled( ! this.locked.contains( dest ) );
			ToggleButtonModelByDestination tbmbd = buttonmodels == null ? null : buttonmodels.get( dest );
			if( tbmbd != null ) tbmbd.fireStateChanged();
		}catch( Throwable thrown ){
			System.err.println( "warning: Definitions.fireAction() caught " + thrown );
		}

		return this;
	}

	/** A ListModel/ComboBoxModel that serves the choices for one '{@link Domain#dest destination}' element.<br />
		Delegates to the single, centralized {@link Definitions model}, which uses {@link Cache caching} for better performance. */
	public class ListModelByDestination implements ListModel, ComboBoxModel
	{
		public ListModelByDestination( D destination ){
			this.destination = destination;
		}

		public void    addListDataListener( ListDataListener listdatalistener ){
			       Definitions.this.model____addListDataListener( destination, listdatalistener );
		}

		public Object         getElementAt( int index ){
			return Definitions.this.model___________getElementAt( destination, index );
		}

		public int                 getSize(){
			return Definitions.this.model________________getSize( destination );
		}

		public Object      getSelectedItem(){
			return Definitions.this.model________getSelectedItem( destination );
		}

		public void removeListDataListener( ListDataListener listdatalistener ){
			       Definitions.this.model_removeListDataListener( destination, listdatalistener );
		}

		public void        setSelectedItem( Object anItem ){
		           Definitions.this.model________setSelectedItem( destination, anItem );
		}

		private       D                destination;
	}

	/** get a ListModel/ComboBoxModel that serves the choices for one '{@link Domain#dest destination}' element */
	public ComboBoxModel getListModel( D dest )
	{
		ListModelByDestination ret = null;
		if(      this.listmodels == null ) this.listmodels = new HashMap<D,ListModelByDestination>( this.domain_dest.size() );
		else if( this.listmodels.containsKey( dest ) ) ret = this.listmodels.get( dest );

		if( ret == null ) this.listmodels.put( dest, ret = new ListModelByDestination( dest ) );

		return ret;
	}

	/** a ButtonModel that represents the locked/unlocked state for one '{@link Domain#dest destination}' element */
	public class ToggleButtonModelByDestination extends ToggleButtonModel implements ButtonModel
	{
		public ToggleButtonModelByDestination( D destination ){
			this.destination = destination;
		}

		public boolean isSelected(){
			return Definitions.this.model_____________isSelected( destination );
		}

		public void setSelected( boolean selected ){
		           Definitions.this.model____________setSelected( destination, selected );
		}

		public void fireStateChanged(){ super.fireStateChanged(); }

		private       D                destination;
	}

	/** get a ButtonModel that represents the locked/unlocked state for one '{@link Domain#dest destination}' element */
	public ToggleButtonModel getButtonModel( D dest )
	{
		ToggleButtonModelByDestination ret = null;
		if(      this.buttonmodels == null ) this.buttonmodels = new HashMap<D,ToggleButtonModelByDestination>( this.domain_dest.size() );
		else if( this.buttonmodels.containsKey( dest ) ) ret = this.buttonmodels.get( dest );

		if( ret == null ) this.buttonmodels.put( dest, ret = new ToggleButtonModelByDestination( dest ) );

		return ret;
	}

	/** a wrapper around a ListDataListener for one '{@link Domain#dest destination}' element */
	public class ListenByDestination{
		public ListenByDestination( D destination, ListDataListener listener ){
			this.destination = destination;
			this.listener    = listener;
		}

		/** automatically create a ListDataEvent and send it to the wrapped listener */
		public ListDataListener fireContentsChanged(){
			ListDataEvent event =
				new ListDataEvent( /* source */ Definitions.this,
								   /* type   */ ListDataEvent.CONTENTS_CHANGED,
								   /* index0 */ 0,
								   /* index1 */ Definitions.this.model________________getSize( destination ) );
			this.listener.contentsChanged( event );
			return this.listener;
		}

		public       D                destination;
		public final ListDataListener listener;
	}

	/** notify all listeners that either the {@link #define definition} or {@link #lock locked}/{@link #unlock unlocked} state of at least one '{@link Domain#dest destination}' element has changed */
	public Definitions<D,S,F> fireContentsChanged(){
		smudge();

		if( myListeners == null ) return this;

		try{
			for( ListenByDestination lbd : myListeners ) lbd.fireContentsChanged();
		}catch( Throwable thrown ){
			System.err.println( "warning: Definitions.fireContentsChanged() caught " + thrown );
		}

		return this;
	}

	/** mark all state as 'dirty', ie in need of refresh */
	public Definitions<D,S,F> smudge(){
		clean = false;
		if( mycache != null ) mycache.clear();
		return this;
	}

	/** Check if we're dirty.  If so, re-initialize all dirty state.  Also keep statistics. */
	private Definitions<D,S,F> refresh(){
		++refresh_total;
		if( clean ){ ++refresh_hits; return this; }
		try{
			++refresh_misses;
			this.      available.clear();
			this.      available.addAll( this.domain_src_exclusive );
			for( D locked : this.locked ) this.available.remove( this.dest2src.get( locked ) );

			int    num_available = this.available.size();
			this.count           = this.array_src_shared.length + num_available;
			this.array_available = this.available.toArray( this.array_available = new Object[ num_available ] );
		}finally{
			clean = true;
		}
		return this;
	}

	private  Definitions<D,S,F> runWithoutLocks( Runnable runnable ){
		Collection<D> save = new ArrayList<D>( locked );
		locked.clear();
		try{
			runnable.run();
		}finally{
			locked.addAll( save );
		}
		return this;
	}

	/** A scale used to rate the quality of a guess.
		The automatic guess code is greedy.  It will always try to
		make the highest quality guesses first.  Generally
		that should be more than adequate.

		@since 20071207 */
	public enum GuessQuality{
	  //excellent, good, fair, poor, prohibited;
		prohibited, poor, mediocre, fair, good, excellent;

		/** Note: {@link #prohibited prohibited} does not occur in this array. */
		public static final GuessQuality[] BEST_TO_WORST = new GuessQuality[]{ excellent, good, fair, mediocre, poor };
	}

	/** override this method if you don't need the state of the guess-in-progress to make a good guess or {@link #auditGuess(Object,Object,Map,Collection,Collection) the other version} if you do (you never have to override both)
		@since 20071207 */
	public          GuessQuality        auditGuess( D destination, S guess ){
		return destination == guess ? GuessQuality.excellent : (destination.equals( guess ) ? GuessQuality.good : GuessQuality.fair);
	}

	/** override this method if you need the state of the guess-in-progress to make a good guess or {@link #auditGuess(Object,Object) the other version} if you don't (you never have to override both)
		@since 20071207 */
	public          GuessQuality        auditGuess( D destination, S guess, Map<D,S> guessed, Collection<D> dest_remaining, Collection<S> src_remaining ){
		return this.auditGuess( destination, guess );
	}

	/** override this method to change the strategy for assigning guesses to destination elements that could not be assigned by the normal guess strategy
		@since 20071207 */
	public    Definitions<D,S,F>         postGuess( Map<D,S> guessed, Collection<D> dest_remaining, Collection<S> src_remaining ){
		if( (dest_remaining != null) && (! dest_remaining.isEmpty()) && (src_default == null) ){ throw new IllegalStateException( "can't finish guessing "+dest_remaining.size()+" without a default function" ); }

		for( D unguessable :   dest_remaining ){ define( unguessable, src_default ); }
		dest_remaining.clear();

		return this;
	}

	/** @since 20071207 */
	private   Definitions<D,S,F>         bestGuess(){
		Map<D,S>             guessed = new    HashMap<D,S>( domain_dest.size()   );
		Collection<D> dest_remaining = new LinkedList<D>(   domain_dest          );
		Collection<S>  src_remaining = new LinkedList<S>(   domain_src_exclusive );

		if(  quality_counts == null ) quality_counts = new EnumMap<GuessQuality,Integer>( GuessQuality.class );
		else quality_counts.clear();

		D   dest;
		S   src;
		int count;
		for( GuessQuality quality : GuessQuality.BEST_TO_WORST ){
			count = 0;
			for( Iterator<D> dit = dest_remaining.iterator(); dit.hasNext(); ){
				dest = dit.next();
				for( Iterator<S> sit = src_remaining.iterator(); sit.hasNext(); ){
					src = sit.next();

					if( Thread.currentThread().isInterrupted() ) return this;
					if( auditGuess( dest, src ) == quality ){
						guessed.put( dest, src );
						dit.remove();
						sit.remove();
						++count;
						break;
					}
				}
			}
			quality_counts.put( quality, count );
		}

		for( D   guessable : guessed.keySet() ) define(  guessable, guessed.get( guessable ) );

		return postGuess( guessed, dest_remaining, src_remaining );
	}

	/** Retrieve the quantitative quality of the guess defined
		by the the number of assignments made at each level of guess quality.

		@since 20071207 */
	public Map<GuessQuality,Integer>      quality(){
		return this.quality_counts;
	}

	/** @since 20071207 */
	private  Definitions<D,S,F>       simpleGuess(){
		Iterator<S> sources = domain_src_exclusive.iterator();
		Object      src;
		for( D dest : domain_dest ){
			if( (src = sources.hasNext() ? sources.next() : src_default) == null ){ throw new IllegalStateException( "can't finish simple guess without a default function" ); }
			define( dest, src );

			if( Thread.currentThread().isInterrupted() ) break;
		}

		return this;
	}

	/** Initialize the definitions using the best guessing strategy. */
	public   Definitions<D,S,F>           guess(){
		runWithoutLocks( new Runnable(){
			public void run(){ bestGuess(); }
		} );
		guesses = new HashMap<D,Object>( dest2src );
		return this;
	}

	/** Reset the definitions to the values arrived at by a previous call to {@link #guess guess()}. */
	public   Definitions<D,S,F>          revert(){
		if( guesses == null || guesses.isEmpty() ) return this;

		return this.runWithoutLocks( new Runnable(){
			public void run(){
				for( D dest : domain_dest ){
					define( dest, guesses.get( dest ) );

					if( Thread.currentThread().isInterrupted() ) break;
				}
			}
		} );
	}

	/** Get the domain of all functions configured on these definitions. */
	public             Set<F>       functions(){
		return this.domain_src_shared;
	}

	/** Define every destination element as the given funtion. Make sure none of the definitions is locked. Sleep the given number of nanoseconds between redefining each destination (for animation). Negative sleep number means no sleep. */
	public   Definitions<D,S,F>  setFunctionAll( F function, long nanos ) throws InterruptedException{
		return setFunction( domain_dest, function, nanos );
	}

	/** For each destination given, define it as the given funtion. Make sure none of the definitions is locked. Sleep the given number of nanoseconds between redefining each destination (for animation). Negative sleep number means no sleep. */
	public   Definitions<D,S,F>  setFunction( Collection<D> destinations, F function, long nanos ) throws InterruptedException{
		for( D dest : destinations ){
			define( dest, function );

			if( nanos > 0 ) Thread.sleep( nanos );
			else if( Thread.currentThread().isInterrupted() ) break;
		}
		return this;
	}

	/** Lock or unlock all definitions. Sleep the given number of nanoseconds between changing the lock status of each destination (for animation). Negative sleep number means no sleep. */
	public   Definitions<D,S,F>    setLockedAll( boolean locked, long nanos ) throws InterruptedException{
		return setLocked( domain_dest, locked, nanos );
	}

	/** Lock or unlock the definition for each destination given. Sleep the given number of nanoseconds between changing the lock status of each destination (for animation). Negative sleep number means no sleep. */
	public   Definitions<D,S,F>    setLocked( Collection<D> destinations, boolean locked, long nanos ) throws InterruptedException{
		for( D dest : destinations ){
			if( locked )   lock( dest );
			else         unlock( dest );

			if( nanos > 0 ) Thread.sleep( nanos );
			else if( Thread.currentThread().isInterrupted() ) break;
		}
		return this;
	}

	/** Compute the number of locked definitions.
		@since 20071206 */
	public   int               countLocked(){
		return this.locked.size();
	}

	/** Return the number of destination elements configured here.
		@since 20071206 */
	public   int                      size(){
		return this.domain_dest.size();
	}

	/** Compute the ration of locked definitions to total definitions.
		@since 20071206 */
	public   float          fractionLocked(){
		return ((float) countLocked()) / ((float) size());
	}

	/** @since 20071209 */
  //public static final String SPACES = "                                                                      ";

	/** @since 20071209 */
	private Dest    pad( Dest out, String str, int width ){
		return pad( out, str.length(), width );
	}

	/** @since 20071209 */
	private Dest    pad( Dest out, int len, int width ){
		int diff = width - len;
		if( diff < 1 ) return out;
		for( int i=0; i<diff; i++ ) out.app( ' ' );
		return out;
	}

	/** Write a text description of these definitions in human readable format.
		@since 20071209 */
	public Dest append( Dest out ){
		return this.append( out, Format.human );
	}

	/** Write a text description of these definitions in the given format.
		@since 20071209 */
	public Dest append( Dest out, Format format ){
		return this.append( out, format, VARIABLE_ID );
	}

	/** Write a text description of these definitions in the given format with the given Stringifier.
		@since 20071209 */
	public Dest append( Dest out, Format format, Stringifier ifier ){
		return this.append( out, format, ifier, null );
	}

	/** A client should implement this interface if it will be necessary to provide
		more information in order to write an informative textual description of
		these definitions.

		@since 20071209 */
	public interface MoreInformation<X,Y,Z>{
		/** Return true if there is no extra information for the given destination. */
		public    boolean      none(                                             X destination ) throws Exception;
		/** Write a custom prefix before writing out the definition.  For example, write the ordinal number itself in order to number the definitions. */
		public    Dest       prefix( Dest out, int   ordinal ) throws Exception;
		/** Write any whitespace that should separate the definitions.  For example, a newline. */
		public    Dest     separate( Dest out, Format format ) throws Exception;
		/** Write more infomation that may exist for a given definition.  For example, the values definition for a given variable-to-variable definition. */
		public    Dest       append( Dest out, Format format, Stringifier ifier, X destination ) throws Exception;
		/** Do any processing on the function.  For example, substitute a constant number value for a constant function. */
		public    String    specify( Z function, X destination ) throws Exception;
	}

	/** Utility class to surround a string with double quotes.

		@since 20071209 */
	public static class Quoter{
		public static final   char CHARQUOTE = '"';
		public static final String  STRQUOTE = "\"";

		public String quote( String str ){
			buff.setLength( 1 );
			return buff.append( str ).append( CHARQUOTE ).toString();
		}

		private StringBuilder buff = new StringBuilder( STRQUOTE );
	}

	/** Write a text description of these definitions in the given format with the given Stringifier and given extra information.

		@since 20071209 */
	@SuppressWarnings( "unchecked" )
	public Dest append( Dest out, Format format, Stringifier ifier, MoreInformation<D,S,F> moreinformation ){
		try{
			switch( format ){
				case human:
				  //out.app( "human readable probability rewrite definitions on " ).app( new Date( System.currentTimeMillis() ).toString() ).nl();
				  	String strundef           = "undefined";
				  	Map<D, String>  specifics = new HashMap<D, String>( domain_dest.size() );
				  	Map<D,Integer>    lengths = new HashMap<D,Integer>( domain_dest.size() );
				  	Quoter             quoter = new Quoter();

				  	String specific;
					int lenWidestDest = 0, lenWidestSource = 0, lenUndef = strundef.length(), lenDest, lenSource;
					Object src;
				  	for( D dest : domain_dest ){
				  		src     = dest2src.get( dest );
				  		lenDest = ifier.objectToString( dest ).length();
						switch( domain( src ) ){
							case src_exclusive:
							case intersection:
								lenSource = (specific = quoter.quote( ifier.objectToString( src ) )).length();
								break;
							case src_shared:
								lenSource = (specific = ((moreinformation == null) ? src.toString() : moreinformation.specify( (F) src, dest ))).length();
								break;
							default:
								specific  = strundef;
								lenSource = lenUndef;
								break;
						}
						lenWidestDest   = Math.max( lenWidestDest,   lenDest   );
						lenWidestSource = Math.max( lenWidestSource, lenSource );
						specifics.put( dest, specific  );
						lengths  .put( dest, lenSource );
				  	}

					Domain domain;
				  	String strdest, strsrc;
				  	int    ordinal = 1;
					for( D dest : domain_dest ){
						domain = domain( src = dest2src.get( dest ) );

					  //System.out.println( ifier.objectToString( dest ) + " = " + src + " (" + domain( src ) + ")" );

						if( moreinformation != null ) moreinformation.prefix( out, ordinal );
						out.app( '"' ).app( strdest = ifier.objectToString( dest ) ).app( '"' );
						pad( out,             strdest, lenWidestDest   ).app( " = " );
						if( domain != src_shared ) pad( out, lengths.get( dest ), lenWidestSource );
						out.app( specifics.get( dest ) );

					  //if( locked.contains( dest ) ) out.app( " (locked)" );

						if( moreinformation == null ) out.nl();
						else{
							if( moreinformation.none( dest ) ) out.nl();
							else{
								out.app( '{' ).nl();
								moreinformation  .append( out, format, ifier, dest ).app( '}' ).nl();
							}
							moreinformation.separate( out, format );
						}
						++ordinal;
					}
					break;
				case xml:
					break;
				case debug:
				default:
					return
					out.app( "domain_dest:        " ).app( domain_dest         .toString()        ).nl()
					   .app( "domain_src_exclusi: " ).app( domain_src_exclusive.toString()        ).nl()
					   .app( "domain_src_shared:  " ).app( domain_src_shared   .toString()        ).nl()
					   .app( "dest2src:           " ).app(   dest2src          .toString()        ).nl()
					   .app( "src2dest_exclusive: " ).app(   src2dest_exclusive.toString()        ).nl()
					   .app( "src2dest_shared:    " ).app(   src2dest_shared   .toString()        ).nl()
					   .app( "locked:             " ).app(   locked            .toString()        ).nl()
					   .app( "count:              " ).app(              Integer.toString( count ) ).nl();
			}
		}catch( Throwable thrown ){
			System.err.println( "warning: Definitions.append() caught " + thrown );
		}
		return out;
	}

	/** Possible output formats.  Only {@link Definitions.Format#xml xml} and {@link Definitions.Format#human human} are intended
		to be exposed in a release version user program.

		@since 20071208 */
	public enum Format implements Actionable<Format>, Semantic{
		/** write debug format, not intended for release software */
		debug  ( "debug",          "write debug format, not intended for release software" ),
		/** write xml that you can load at a later time */
		xml    ( "xml",            "write xml that you can load at a later time" ),
		/** write a human readable description */
		human  ( "human readable", "write a human readable description" );

	  //public Dest append( Definitions defs, Dest app ){ return defs.append( app, this ); }

		private Format( String d, String t ){
			this.properties.put( display,     d );
			this.properties.put( tooltip,     t );
			this.properties.put( accelerator, firstUniqueKeyStroke( d ) );
		}

		/** @since 20071217 */
		public Semantics semantics(){ return exclusive; }

		/** interface Actionable */
		public Format   getDefault(){ return     human; }

		/** interface Actionable */
		public Object get( Property property ){ return this.properties.get( property ); }

		private Map<Property,Object> properties = new EnumMap<Property,Object>( Property.class );

		/** Get new buttons suitable to represent the choice of output format in a user menu. */
		public static AbstractButton[] buttons( View view, Object id, ItemListener listener, Collection<Format> values ){
			return MODELS.newButtons( view, id, listener, values, null );
		}

		/** Retrieve which format the user selected from an {@link EnumModels.Semantics#exclusive exclusive} menu. */
		public static     Format      selected( Object id ){
			return MODELS.selected( exclusive, id );
		}

		/** Retrieve which format the user selected from an {@link EnumModels.Semantics#additive additive} menu. */
		public static Set<Format>     selected( Object id, Set<Format> results ){
			return MODELS.selected(  additive, id, results);
		}

		public static EnumModels<Format> MODELS = new EnumModels<Format>( Format.class );
	}

	/** Get the default definition that was configured on these definitions.

		@since 20071210 */
	public F getDefault(){
		return this.src_default;
	}

	/** Get the set of single-use source elements that are not currently being
		used as the definition of any destination element.

		@since 20071210 */
	public Set<S> unused(){
		Set<S> unused = new HashSet<S>( domain_src_exclusive );
		unused.removeAll( dest2src.values() );
		return unused;
	}

	private     Set<S>              domain_src_exclusive, available;
	private     Set<D>              domain_dest, locked;
	private     Set<Object>         domain_src_all;
	private     Set<F>              domain_src_shared;
	private     Object[]             array_src_shared, array_available;

	private         F                      src_default;
	private     Map<S,    D >              src2dest_exclusive;
	private     Map<F,Set<D>>              src2dest_shared;
	private     Map<D,Object>              dest2src, guesses;
	private     Map<GuessQuality,Integer>  quality_counts;

	private     Map<D,           ActionByDestination> actions;
	private     Map<D,        ListModelByDestination> listmodels;
	private     Map<D,ToggleButtonModelByDestination> buttonmodels;

	private     Collection<ListenByDestination> myListeners = new LinkedList<ListenByDestination>();

	private         boolean          clean   = true;
	private         Cache            mycache = new Cache();
	private         int              count   = -1, refresh_misses = 0, refresh_hits = 0, refresh_total = 0;

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

	/** test/debug - show a window that let's the user edit a simple 4x4 definition while monitoring refresh/cache statistics and state. */
	public static class Test
	{
		/** hypothetical functions */
		enum Function{ add, subtract, f_default, multiply }

		void row( String left, String right ){
			row( left, new JLabel( right ) );
		}

		void row( String left, JComponent right ){
			c.gridwidth  = 1;
			hstrut();
			c.anchor    = GridBagConstraints.EAST;
			pnl.add( new JLabel( left ), c );
			hstrut();
			c.anchor    = GridBagConstraints.WEST;
			c.gridwidth = GridBagConstraints.REMAINDER;
			pnl.add( right, c );
			vstrut();
		}

		void label( String text ){
			pnl.add( new JLabel( text ), c );
		}

		void vstrut(){
			vstrut( strut );
		}

		void vstrut( int size ){
			c.gridwidth = GridBagConstraints.REMAINDER;
			pnl.add( Box.createVerticalStrut( size ), c );
		}

		void hstrut(){
			pnl.add(  Box.createHorizontalStrut( 0x10 ), c );
		}

		int                strut = 0x4;
		JPanel             pnl;
		GridBagConstraints c;

		public int mainImpl( String[] args ) throws Exception{
			String arg0                          = args.length>0 ? args[0] : "";

			String intersectional = "sam";
			String[] array_dest = null, array_src = null;
			Collection<String>   domain_dest          = Arrays.asList( array_dest = new String[]{ "bing", intersectional,    "foo",  "bar" } );
			Collection<Function> domain_src_shared    = EnumSet.allOf( Function.class );
			Collection<String>   domain_src_exclusive = Arrays.asList( array_src  = new String[]{  "sis", intersectional, "smjorn", "smoo" } );
			Function             src_default          = Function.f_default;

			Definitions<String,String,Function> defs = new Definitions<String,String,Function>( domain_dest, domain_src_shared, domain_src_exclusive, src_default ){
				public Kind characterize( Object element ){
					if(      element instanceof String   ) return Kind.exclusive;
					else if( element instanceof Function ) return Kind.shared;
					else                                   return Kind.undefined;
				}
			};

			defs.define( array_dest[0], array_src[3] );
			defs.define( array_dest[1], array_src[0] );
			defs.define( array_dest[2], array_src[1] );
			defs.define( array_dest[3], Function.multiply );

			defs.lock( array_dest[1] );

			defs.define( array_dest[2], array_src[3] );//override

			if( arg0.equals( "setup" ) ) System.exit(0);


			Util.setLookAndFeel();

			pnl         = new JPanel( new GridBagLayout() );
			c           = new GridBagConstraints();
			c.anchor    = GridBagConstraints.WEST;

			vstrut();

			row( "<html>destination domain:",    "<html><b>" + Arrays.toString( array_dest ) );
			row(      "<html>source domain:",    "<html><b>" + Arrays.toString(  array_src ) );
			row(      "<html>source functions:", "<html><b>" + EnumSet.allOf( Function.class ) );
			vstrut( 0x20 );

			c.anchor     = GridBagConstraints.WEST;
			c.gridwidth  = 1;
			hstrut();
			label( "<html><b>destination" );
			hstrut();
			label( "<html><b>source definition" );
			hstrut();
			label( "<html><b>lock" );
			c.gridwidth = GridBagConstraints.REMAINDER;
			hstrut();

			vstrut( 0x10 );

			ProbabilityRewrite.DecoratedRenderer renderer = ProbabilityRewrite.getInstance().new DecoratedRenderer( new DefaultListCellRenderer()/*, ProbabilityRewrite.Stringification.ids*/, EnumSet.allOf( Function.class ) );
			JCheckBox cb    = null;
			JComboBox combo = null;
			for( String dest : domain_dest ){
				c.gridwidth = 1;
				c.anchor    = GridBagConstraints.WEST;
				hstrut();
				label( dest );
				hstrut();
				pnl.add( combo = new JComboBox( defs.getListModel( dest ) ), c );
				hstrut();
				c.anchor    = GridBagConstraints.EAST;
				pnl.add(    cb = new JCheckBox(), c );
				c.gridwidth = GridBagConstraints.REMAINDER;
				hstrut();

				cb.setModel( defs.getButtonModel( dest ) );

				combo.        setAction( defs.getAction( dest ) );
				combo      .setRenderer( renderer );
				combo.addActionListener( renderer );

				renderer.actionPerformed( new ActionEvent( combo, 0, "" ) );
			}

			c.anchor    = GridBagConstraints.WEST;
			vstrut( 0x40 );

			final JLabel labelRTotal    = new JLabel(), labelRHits       = new JLabel(),
			             labelRMisses   = new JLabel(), labelRHitPercent = new JLabel(),
			             labelCTotal    = new JLabel(), labelCHits       = new JLabel(),
			             labelCMisses   = new JLabel(), labelCHitPercent = new JLabel(),
			             labelsrc2dest  = new JLabel(), labeldest2src    = new JLabel(), labelsrc2dest_shared = new JLabel();

			row(         "refresh misses:", labelRMisses     );
			row(           "refresh hits:", labelRHits       );
			row(              "refreshes:", labelRTotal      );
			row( "refresh hit percentage:", labelRHitPercent );

			vstrut( 0x10 );

			row(           "cache misses:", labelCMisses     );
			row(             "cache hits:", labelCHits       );
			row(         "cache accesses:", labelCTotal      );
			row(   "cache hit percentage:", labelCHitPercent );

			vstrut( 0x10 );

			row( "dest2src:",           labeldest2src );
			row( "src2dest_exclusive:", labelsrc2dest );
			row( "src2dest_shared:",    labelsrc2dest_shared );

		  /*EnumMap<Function,JLabel> functionLabels = new EnumMap<Function,JLabel>( Function.class );
			for( Function function : Function.values() ){
				functionLabels.put( function, new JLabel() );
				row( function.name() + ":", functionLabels.get( function ) );
			}*/

			JFrame frame = Util.getDebugFrame( "Definitions TEST/DEBUG", pnl );
			frame.setSize( new Dimension( 0x200, 700 ) );
			frame.setVisible( true );

			int cache_accesses = 0;
			while( pnl.isVisible() ){
				Thread.sleep( 0x100 );

				update( labelRTotal,      defs.refresh_total   );
				update( labelRHits,       defs.refresh_hits    );
				update( labelRMisses,     defs.refresh_misses  );
				update( labelRHitPercent, percent( defs.refresh_hits, defs.refresh_total ) );


				update( labelCTotal,      cache_accesses = defs.mycache.hits + defs.mycache.misses );
				update( labelCHits,       defs.mycache.hits    );
				update( labelCMisses,     defs.mycache.misses  );
				update( labelCHitPercent, percent( defs.mycache.hits, cache_accesses ) );

				update( labeldest2src,        defs.          dest2src.toString() );
				update( labelsrc2dest,        defs.src2dest_exclusive.toString() );
				update( labelsrc2dest_shared, defs.src2dest_shared   .toString() );

			  /*for( Function function : Function.values() ){
					update( functionLabels.get( function ), defs.src2dest_shared.get( function ).toString() );
				}*/
			}

			return 0;
		}

		void update( JLabel label, int value ){
			update( label, Integer.toString( value ) );
		}

		void update( JLabel label, String value ){
			if( ! label.getText().equals( value ) ) label.setText( value );
		}

		String percent( int numerator, int denominator ){
			return Float.toString( (((float)numerator)/((float)denominator)) * 100f ) + "%";
		}
	}
}
