package edu.ucla.belief.ui.networkdisplay;

import edu.ucla.belief.ui.event.NetStructureChangeListener;
import edu.ucla.belief.ui.event.NetStructureEvent;
import edu.ucla.belief.ui.NetworkInternalFrame;

import edu.ucla.util.JVMTI;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.util.*;

/** improve performance/responisiveness of network display graphics by making hidden components invisible
	@author keith cascio
	@since  20060721 */
public class OpacityConsultant implements NetStructureChangeListener, NetworkDisplay.ComponentMovementListener, Runnable
{
	public OpacityConsultant(){}

	public static boolean FLAG_CONTAINED = NetworkDisplay.FLAG_DESKTOP_CONTAINS_LABELS;

	/** decide if there is enough overlap in the network
		to warrant the overhead of an OpacityConsultant
		@return a configured OpacityConsultant if there is enough overlap to justify the overhead, null otherwise
		@since 20060801 */
	@SuppressWarnings( "unchecked" )
	static public OpacityConsultant installIfHelpful( NetworkDisplay networkdisplay ){
		OpacityConsultant ret = null;
		if( networkdisplay.myOpacityConsultant != null ){
			ret = (OpacityConsultant) networkdisplay.myOpacityConsultant;
			ret.updateSynchronous();
			return ret;
		}

		Collection<Component> components = networkdisplay.getNetworkComponentLabels();
		int threshold = components.size() >> 1;
		Set<Point> distinct = new HashSet<Point>( threshold );
		for( Component component : components ) distinct.add( component.getLocation( new Point() ) );

		boolean beneficial = distinct.size() < threshold;
	  //System.out.println( beneficial ? ("installing OpacityConsultant") : ("not enough overlap to justify overhead of OpacityConsultant") );
		if( !beneficial ) return null;

		ret = new OpacityConsultant();
		ret.configure( networkdisplay );
		return ret;
	}

	public void configure( NetworkDisplay networkdisplay ){
		if( myNetworkDisplay != networkdisplay ){
			myNetworkDisplay = networkdisplay;
			myContainer      = networkdisplay.getJDesktopPane();
			networkdisplay.addComponentMovementListener( (NetworkDisplay.ComponentMovementListener)this );
			NetworkInternalFrame nif = networkdisplay.getNetworkInternalFrame();
			if( nif != null ) nif.addNetStructureChangeListener( (NetStructureChangeListener)this );
		}

		updateSynchronous();
	}

	public enum Status { finished, interrupted, damaged }

	public Status updateSynchronous(){
		try{
			mySynchronousThread = Thread.currentThread();
			Status result = updateArrowsSynchronous();
			if( result == Status.finished ) return updateComponentsSynchronous();
			else return result;
		}finally{
			cleanup();
			mySynchronousThread = null;
		}
	}

	/*
	public Status updateSynchronousTest(){
		HashSet<Component> all = new HashSet<Component>( myNetworkDisplay.getNetworkComponentLabels() );
		long begin = JVMTI.getCurrentThreadCpuTimeUnsafe();
		int repetitions = 0x100;
		for( int i=0; i<repetitions; i++ ){
			updateComponentsSynchronous1();
			topographicSafety( all );
		}
		Status ret = updateComponentsSynchronous1();
		Util.STREAM_TEST.println( "updateComponentsSynchronous1() x"+(repetitions+1)+", completed in " + (JVMTI.getCurrentThreadCpuTimeUnsafe() - begin) );
		return ret;
	}

	public Status updateComponentsSynchronous1(){
		//long begin = JVMTI.getCurrentThreadCpuTimeUnsafe();//System.currentTimeMillis();
		int  total = 0;
		int  r     = requests;
		try{
			Thread    current         = mySynchronousThread = Thread.currentThread();
			Container container       = getContainer();
			int       num             = FLAG_CONTAINED ? container.getComponentCount() : myNetworkDisplay.getNetworkComponentLabels().size();

			if( myLocations == null )         myLocations  = new HashMap<Point,Map<Dimension,Overlap>>( num >> 1 );
			else myLocations.clear();
			Map<Point,Map<Dimension,Overlap>> locations    = myLocations;
			if( myOverlaps == null )          myOverlaps   = new LinkedList<Overlap>();
			else myOverlaps.clear();
			Collection<Overlap>               overlaps     = myOverlaps;
			if( myTopography == null )        myTopography = new HashMap<Component,Component>( num );
			else myTopography.clear();

			int initCapacity = Math.max( num >> 4, 4 );
			Point p = new Point();
			Map<Dimension,Overlap> sizes;
			Dimension d = new Dimension();
			Overlap overlap;
			Component component;
			for( int i=0; i<num; i++ ){
				if( current.isInterrupted() ) return Status.interrupted;
				component = FLAG_CONTAINED ? container.getComponent(i) : (Component) myNetworkDisplay.getNetworkComponentLabels().get(i);
				component.getLocation( p );
				if(  locations.containsKey(    p ) ) sizes   = locations.get( p );
				else locations.put( new Point( p ),  sizes   = new HashMap<Dimension,Overlap>( initCapacity ) );
				component.getSize( d );
				if(  sizes.containsKey(        d ) ) overlap = sizes.get( d );
				else{
					 sizes.put( new Dimension( d ),  overlap = new Overlap() );
					 overlaps.add( overlap );
				}
				overlap.add( component );
			}

			for( Overlap ol : overlaps ){
				if( current.isInterrupted() ) return Status.interrupted;
				total += ol.showOrHide();
			}

			for( Overlap ol : overlaps ){
				if( current.isInterrupted() ) return Status.interrupted;
				if( FLAG_CONTAINED ) ol.topography( myTopography );
				else ol.topoUncontained( myTopography );
			}
		}finally{
			cleanup();
			mySynchronousThread = null;
		}
		//System.out.println( "updateSynchronous1() hid "+total+" components, serviced "+r+" requests, completed in " + (JVMTI.getCurrentThreadCpuTimeUnsafe() - begin) );
		return Status.finished;
	}
	*/

	public Status updateComponentsSynchronous(){
		//long begin = JVMTI.getCurrentThreadCpuTimeUnsafe();//System.currentTimeMillis();
		int  total = 0;
		int  r     = requests;

		Thread    current   = Thread.currentThread();
		Container container = getContainer();
		int       num       = FLAG_CONTAINED ? container.getComponentCount() : myNetworkDisplay.getNetworkComponentLabels().size();

		if( myDistinctBounds == null ) myDistinctBounds = new HashMap<Rectangle,Overlap>( num >> 1 );
		else myDistinctBounds.clear();
		Map<Rectangle,Overlap>         distinct         = myDistinctBounds;
		if( myOverlaps == null )       myOverlaps       = new LinkedList<Overlap>();
		else myOverlaps.clear();
		Collection<Overlap>            overlaps         = myOverlaps;
		if( myTopography == null )     myTopography     = new HashMap<Component,Component>( num );
		else myTopography.clear();

		Rectangle rect = new Rectangle();
		Overlap overlap;
		Component component;
		for( int i=0; i<num; i++ ){
			if( current.isInterrupted() ) return Status.interrupted;
			component = FLAG_CONTAINED ? container.getComponent(i) : (Component) myNetworkDisplay.getNetworkComponentLabels().get(i);
			component.getBounds( rect );
			if(  distinct.containsKey(        rect ) ) overlap = distinct.get( rect );
			else{
				 distinct.put( new Rectangle( rect ),  overlap = new Overlap() );
				 overlaps.add( overlap );
			}
			overlap.add( component );
		}

		for( Overlap ol : overlaps ){
			if( current.isInterrupted() ) return Status.interrupted;
			total += ol.showOrHide();
		}

		for( Overlap ol : overlaps ){
			if( current.isInterrupted() ) return Status.interrupted;
			if( FLAG_CONTAINED ) ol.topography( myTopography );
			else ol.topoUncontained( myTopography );
		}
		//System.out.println( "updateSynchronous2() hid "+total+" components, serviced "+r+" requests, completed in " + (JVMTI.getCurrentThreadCpuTimeUnsafe() - begin) );
		return Status.finished;
	}

	/** @since 20060731 */
	@SuppressWarnings( "unchecked" )
	public Status updateArrowsSynchronous(){
		//long begin = JVMTI.getCurrentThreadCpuTimeUnsafe();//System.currentTimeMillis();
		int  total = 0;

		Thread            current = Thread.currentThread();
		Collection<Arrow> arrows  = myNetworkDisplay.getArrows();
		int               num     = arrows.size();

		if( myOrigins == null )         myOrigins         = new HashMap<Point,Map<Point,Arrowlap>>( num >> 1 );
		else myOrigins.clear();
		Map<Point,Map<Point,Arrowlap>>  origins           = myOrigins;
		if( myArrowlaps == null )       myArrowlaps       = new LinkedList<Arrowlap>();
		else myArrowlaps.clear();
		Collection<Arrowlap>            arrowlaps         = myArrowlaps;
		if( myArrowTopography == null ) myArrowTopography = new HashMap<Arrow,Arrow>( num );
		else myArrowTopography.clear();

		int initCapacity = Math.max( num >> 4, 4 );
		Point origin = new Point(), destination = new Point();
		Map<Point,Arrowlap> destinations;
		Arrowlap arrowlap;
		for( Arrow arrow : arrows ){
			if( current.isInterrupted() ) return Status.interrupted;
			arrow.getOrigin( origin );
			if(  origins.containsKey(         origin      ) ) destinations = origins.get( origin );
			else origins.put( new Point(      origin      ),  destinations = new HashMap<Point,Arrowlap>( initCapacity ) );
			arrow.getDestination( destination );
			if(  destinations.containsKey(    destination ) ) arrowlap     = destinations.get( destination );
			else{
				 destinations.put( new Point( destination ),  arrowlap     = new Arrowlap() );
				 arrowlaps.add( arrowlap );
			}
			arrowlap.add( arrow );
		}

		for( Arrowlap al : arrowlaps ){
			if( current.isInterrupted() ) return Status.interrupted;
			total += al.showOrHide();
		}

		for( Arrowlap al : arrowlaps ){
			if( current.isInterrupted() ) return Status.interrupted;
			if( FLAG_CONTAINED ) al.topography( myArrowTopography );
			else al.topoUncontained( myArrowTopography );
		}
		//System.out.println( "updateArrowsSynchronous() hid "+total+" arrows, completed in " + (JVMTI.getCurrentThreadCpuTimeUnsafe() - begin) );
		return Status.finished;
	}

	private void cleanup(){
		if( myLocations      != null ) myLocations.clear();
		if( myDistinctBounds != null ) myDistinctBounds.clear();
		if( myOverlaps       != null ) myOverlaps.clear();
		if( myArrowlaps      != null ) myArrowlaps.clear();
		if( myOrigins        != null ) myOrigins.clear();
	}

	public class Overlap extends LinkedList<Component>{
		public Overlap(){
			super();
		}

		public boolean add( Component component ){
			int zorder = FLAG_CONTAINED ? getContainer().getComponentZOrder( component ) : Overlap.this.size();
			if( zorder < Overlap.this.zorderMin ){
				Overlap.this.zorderMin = zorder;
				Overlap.this.top       = component;
			}
			return super.add( component );
		}

		public void clear(){
			Overlap.this.zorderMin = Integer.MAX_VALUE;
			Overlap.this.top       = null;
			super.clear();
		}

		/** @return number hidden */
		public int showOrHide(){
			boolean flag;
			for( Component component : Overlap.this ){
				flag = (component == Overlap.this.top);
				if( component.isVisible() != flag ) component.setVisible( flag );
			}
			return Overlap.this.size() - 1;
		}

		public void topoUncontained( Map<Component,Component> topography ){
			Component cTop = null;
			for( Component cBottom : Overlap.this ){
				if( cTop != null ) topography.put( cTop, cBottom );
				cTop = cBottom;
			}
		}

		public void topography( Map<Component,Component> topography ){
			if( isEmpty() ) return;
			int size = Overlap.this.size();
			if( (myArrayForSorting == null) || (myArrayForSorting.length < size) )
				myArrayForSorting = new Component[ size ];
			Overlap.this.toArray( myArrayForSorting );
			Arrays.sort( myArrayForSorting, 0, size, ZORDER );
			if( myArrayForSorting[0] != top ) throw new IllegalStateException( "after sorting, inconsistent top Component" );

			Component cTop = null, cBottom = null;
			for( int i=0; i<size; i++ ){
				cBottom = myArrayForSorting[i];
				if( cTop != null ) topography.put( cTop, cBottom );
				cTop = cBottom;
			}
		}

		private int       zorderMin = Integer.MAX_VALUE;
		private Component top;
	}

	/** @since 20060731 */
	public class Arrowlap extends LinkedList<Arrow>{
		public Arrowlap(){
			super();
		}

		public boolean add( Arrow arrow ){
			int zorder = FLAG_CONTAINED ? getContainer().getComponentZOrder( arrow.getEnd().asJLabel() ) : Arrowlap.this.size();
			//if( zorder < 0 ) throw new IllegalStateException( "illegal zorder " + zorder );
			if( zorder < Arrowlap.this.zorderMin ){
				Arrowlap.this.zorderMin = zorder;
				Arrowlap.this.top       = arrow;
			}
			else if( zorder == Arrowlap.this.zorderMin ){
				Container container = getContainer();
				int ztop    = container.getComponentZOrder( Arrowlap.this.top.getStart().asJLabel() );
				int zorigin = container.getComponentZOrder( arrow.getStart().asJLabel() );
				if( zorigin < ztop ) Arrowlap.this.top = arrow;
			}
			return super.add( arrow );
		}

		public void clear(){
			Arrowlap.this.zorderMin = Integer.MAX_VALUE;
			Arrowlap.this.top       = null;
			super.clear();
		}

		/** @return number hidden */
		public int showOrHide(){
			boolean flag;
			for( Arrow arrow : Arrowlap.this ){
				flag = (arrow == Arrowlap.this.top);
				//if( arrow.isVisible() != flag )
					arrow.setVisible( flag );
			}
			return Arrowlap.this.size() - 1;
		}

		public void topoUncontained( Map<Arrow,Arrow> topography ){
			Arrow aTop = null;
			for( Arrow aBottom : Arrowlap.this ){
				if( aTop != null ) topography.put( aTop, aBottom );
				aTop = aBottom;
			}
		}

		public void topography( Map<Arrow,Arrow> topography ){
			if( isEmpty() ) return;
			int size = Arrowlap.this.size();
			if( (myArrayForSortingArrows == null) || (myArrayForSortingArrows.length < size) )
				myArrayForSortingArrows = new Arrow[ size ];
			Arrowlap.this.toArray( myArrayForSortingArrows );
			Arrays.sort( myArrayForSortingArrows, 0, size, ZORDER_ARROWS );
			if( myArrayForSortingArrows[0] != top ) throw new IllegalStateException( "after sorting, inconsistent top Arrow" );

			Arrow aTop = null, aBottom = null;
			for( int i=0; i<size; i++ ){
				aBottom = myArrayForSortingArrows[i];
				if( aTop != null ) topography.put( aTop, aBottom );
				aTop = aBottom;
			}
		}

		private int   zorderMin = Integer.MAX_VALUE;
		private Arrow top;
	}

	public Container getContainer(){
		return myContainer;
	}

	/** @since 20060731 */
	@SuppressWarnings( "unchecked" )
	public void arrowSafetyByComponent( Set<NetworkComponentLabel> moved ){
		if( myArrowTopography == null ) return;

		if( moved.size() > (myNetworkDisplay.getBeliefNetwork().size() >>2) ){
			//System.out.println( "showing all Arrows" );
			for( Arrow arrow : myArrowTopography.values() ) arrow.setVisible( true );
			return;
		}

		for( NetworkComponentLabel ncl : moved ){
			arrowSafety( ncl.getAllInComingEdges() );
			arrowSafety( ncl.getAllOutBoundEdges() );
		}
	}

	/** @since 20060731 */
	private void arrowSafety( Collection<Arrow> keys ){
		for( Arrow arrow : keys ){
			arrow.setVisible( true );
			if( myArrowTopography.containsKey( arrow ) ){
				myArrowTopography.get( arrow ).setVisible( true );
			}
		}
	}

	public void topographicSafety( Set<NetworkComponentLabel> moved ){
		//long begin = JVMTI.getCurrentThreadCpuTimeUnsafe();//System.currentTimeMillis();
		if( moved.size() >= myNetworkDisplay.getBeliefNetwork().size() ) return;

		arrowSafetyByComponent( moved );

		if( myTopography == null ) return;
		//int total = 0;
		Component key, bottom;
		for( NetworkComponentLabel ncl : moved ){
			key = ncl.asJLabel();
			if( myTopography.containsKey( key ) ){
				bottom = myTopography.get( key );
				if( !bottom.isVisible() ) bottom.setVisible( true );
				//++total;
			}
		}
		//System.out.println( "topographicSafety() showed "+total+" components, completed in " + (JVMTI.getCurrentThreadCpuTimeUnsafe() - begin) );
	}

	/** interface NetStructureChangeListener */
	public void netStructureChanged( NetStructureEvent netstructureevent ){
		requestUpdate();
	}

	/** interface NetworkDisplay.ComponentMovementListener */
	@SuppressWarnings( "unchecked" )
	public void componentsMoved( Set/*<NetworkComponentLabel>*/ components ){
		requestUpdate();
		if( components != null ) topographicSafety( components );
	}

	public void requestUpdate(){
		++requests;
		touch();
		synchronized( this ){
			if( myTimerThread == null ) myTimerThread = start();
			else getThreadGroup().interrupt();
		}
	}
	private int requests = 0;

	public void touch(){
		myTouched = System.currentTimeMillis();
	}

	public enum Age { latent, ripe }

	public Age age(){
		return ((System.currentTimeMillis() - myTouched) > myLatency) ? Age.ripe : Age.latent;
	}

	private Status ripen(){
		long age = System.currentTimeMillis() - myTouched;
		if( age < myLatency ){
			try{
				Thread.sleep( myLatency - age );
			}catch( InterruptedException interruptedexception ){
				Thread.interrupted();
				return Status.interrupted;
			}
		}
		return Status.finished;
	}

	public long getLatency(){
		return myLatency;
	}

	public long setLatency( long latency ){
		long old = myLatency;
		myLatency = latency;
		return old;
	}

	public void run(){
		try{
			myInterruptionPolicy.run( OpacityConsultant.this );
			requests = 0;
		}catch( Exception exception ){
			System.err.println( "warning! " + Thread.currentThread().getName() + " caught: " + exception );
		}finally{
			synchronized( this ){
				if(      Thread.interrupted()                    ) run();
				else if( Thread.currentThread() == myTimerThread ) myTimerThread = null;
			}
		}
	}

	public enum InterruptionPolicy{
		relaxed{
			public void run( OpacityConsultant op ){
				do{
					Thread.interrupted();
					while( op.age() == Age.latent ) op.ripen();
				}
				while( op.updateSynchronous() == Status.interrupted );
			}

			public String description(){
				return "style 1: relaxed, i.e. when a request interrupts an update, ripen again before updating";
			}
		},
		eager{
			public void run( OpacityConsultant op ){
				while( op.age()               == Age.latent         ) { op.ripen();                           }
				while( op.updateSynchronous() == Status.interrupted ) { Thread.interrupted(); Thread.yield(); }
			}

			public String description(){
				return "style 2: eager,   i.e. when a request interrupts an update, update immediately";
			}
		};

		static public InterruptionPolicy getDefault(){
			return relaxed;
		}

		abstract public void   run( OpacityConsultant op );
		abstract public String description();
	}

	public InterruptionPolicy getInterruptionPolicy(){
		return myInterruptionPolicy;
	}

	public InterruptionPolicy setInterruptionPolicy( InterruptionPolicy policy ){
		InterruptionPolicy old = myInterruptionPolicy;
		myInterruptionPolicy   = policy;
		return old;
	}

	private Thread start(){
		Thread ret = new Thread( getThreadGroup(), OpacityConsultant.this, "OpacityConsultant " + Integer.toString( INT_THREAD_COUNTER++ ) );
		ret.start();
		return ret;
	}

	@SuppressWarnings( "deprecation" )
	public void stop(){
		Thread current = Thread.currentThread();
		try{
			if( (current == mySynchronousThread) || getThreadGroup().parentOf( current.getThreadGroup() ) ) throw new IllegalStateException();
			if( mySynchronousThread != null ) mySynchronousThread.interrupt();
			Thread.yield();
			getThreadGroup().stop();
		}catch( Exception exception ){
			System.err.println( "warning! OpacityConsultant.stop() caught: " + exception );
		}finally{
			cleanup();
		}
	}

	public ThreadGroup getThreadGroup(){
		if( myThreadGroup == null ){
			ThreadGroup parent = null;
			if( (myNetworkDisplay != null) && (myNetworkDisplay.getNetworkInternalFrame() != null) && (myNetworkDisplay.getNetworkInternalFrame().getParentFrame() != null) && (myNetworkDisplay.getNetworkInternalFrame().getParentFrame().getThreadGroup() != null) ){
				parent = myNetworkDisplay.getNetworkInternalFrame().getParentFrame().getThreadGroup().getParent();
			}
			else parent = Thread.currentThread().getThreadGroup();
			myThreadGroup = new ThreadGroup( parent, "OpacityConsultant threads" );
		}
		return myThreadGroup;
	}

	/** for performance, to save a comparison,
		we assume the zorder of two components
		can never be equal */
	final public Comparator<Component> ZORDER = new Comparator<Component>(){
		public int compare( Component c1, Component c2 ){
			return ( myContainer.getComponentZOrder( c1 ) < myContainer.getComponentZOrder( c2 ) ) ?
			     -1 : 1;
		}
	};

	final public Comparator<Arrow> ZORDER_ARROWS = new Comparator<Arrow>(){
		public int compare( Arrow a1, Arrow a2 ){
			NetworkComponentLabel dest1 = a1.getEnd();
			NetworkComponentLabel dest2 = a2.getEnd();
			if( dest1 == dest2 ) return ( myContainer.getComponentZOrder( a1.getStart().asJLabel() ) < myContainer.getComponentZOrder( a2.getStart().asJLabel() ) ) ?
			     -1 : 1;
			else return ( myContainer.getComponentZOrder( dest1.asJLabel() ) < myContainer.getComponentZOrder( dest2.asJLabel() ) ) ?
			     -1 : 1;
		}
	};

	private static int  INT_THREAD_COUNTER   = 0;
	private static long LONG_LATENCY_DEFAULT = 0x800;//0x400;

	private Map<Point,Map<Point,Arrowlap>>    myOrigins;
	private Collection<Arrowlap>              myArrowlaps;
	private Map<Arrow,Arrow>                  myArrowTopography;
	private Arrow[]                           myArrayForSortingArrows;

	private Map<Rectangle,Overlap>            myDistinctBounds;
	private Collection<Overlap>               myOverlaps;
	private Map<Component,Component>          myTopography;
	private Component[]                       myArrayForSorting;

	private Map<Point,Map<Dimension,Overlap>> myLocations;

	private Container                         myContainer;
	private NetworkDisplay                    myNetworkDisplay;
	private ThreadGroup                       myThreadGroup;
  	private Thread                            mySynchronousThread, myTimerThread;
	private long                              myTouched            = 0;
	private InterruptionPolicy                myInterruptionPolicy = InterruptionPolicy.getDefault();
	private long                              myLatency            = LONG_LATENCY_DEFAULT;
}
