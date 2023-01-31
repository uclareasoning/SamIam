package edu.ucla.belief.approx;

import        edu.ucla.belief.*;
import        edu.ucla.belief.inference.SSEngine;
import        edu.ucla.belief.inference.SSEngineGenerator;
import        edu.ucla.belief. CrouchingTiger .DynamatorImpl;
import        edu.ucla.belief.io.*;
import        edu.ucla.belief.io.PropertySuperintendent;
import static edu.ucla.belief.io.PropertySuperintendent.*;
import        edu.ucla.belief.io.dsl.DSLNodeType;
import        edu.ucla.belief.io.hugin.*;
import        edu.ucla.util.Setting.Settings;
import        edu.ucla.util.SettingsImpl;
import        edu.ucla.util.CPTShells;
import        edu.ucla.util.HiddenProperty;
import static edu.ucla.util.PropertyKey.defaultValue;
import static edu.ucla.belief.approx.EdgeDeletionBeliefPropagationSetting.*;

import        il2.bridge.Converter;
import        il2.model.BayesianNetwork;
import        il2.inf.Algorithm;
import        il2.inf.Algorithm.Setting;
import        il2.inf.edgedeletion.*;
import        il2.inf.edgedeletion.EDAlgorithm.RankingHeuristic;
import        il2.util.IntMap;

import        java.lang.reflect.Array;
import static java.lang.reflect.Array.*;
import        java.util.regex.*;
import        java.util.*;
import        java.awt.Dimension;
import        java.awt.Point;
import static java.awt.event.KeyEvent.*;
import static java.awt.event.InputEvent.*;
import static javax.swing.KeyStroke.getKeyStroke;
import        javax.swing.KeyStroke;

/** Helper class for approximation related tasks,
	for example ed-bp edge removal/recovery.

	@author keith cascio
	@since  20080219 */
public class Macros
{
	public static final String
	  STR_IO_SEPARATE_FIELDS      =          ",",
	  STR_IO_SEPARATE_OBJECTS     =          ";",
	  STR_LABEL_SOFT_EVIDENCE     =          "S",
	  STR_SOFT_EVIDENCE_STATE0    =   "observed",
	  STR_SOFT_EVIDENCE_STATE1    = "unobserved",
	  STR_CHARACTER_SOFT_EVIDENCE =          "s",
	  STR_CHARACTER_CLONE         =          "u",
	  STR_CHARACTER_SEP           =          "_",
	  STR_PREFIX_SOFT_EVIDENCE    = STR_CHARACTER_SOFT_EVIDENCE + STR_CHARACTER_SEP,
	  STR_PREFIX_CLONE            = STR_CHARACTER_CLONE         + STR_CHARACTER_SEP,
	  STR_REGEX_INTEGER_ID        = "(\\d+)",
	  STR_REGEX_SOFT_EVIDENCE     = "([" + STR_CHARACTER_SOFT_EVIDENCE                       + "])" + STR_CHARACTER_SEP + STR_REGEX_INTEGER_ID,
	  STR_REGEX_CLONE             = "([" + STR_CHARACTER_CLONE                               + "])" + STR_CHARACTER_SEP + STR_REGEX_INTEGER_ID,
	  STR_REGEX_AUXILIARY_VAR     = "([" + STR_CHARACTER_SOFT_EVIDENCE + STR_CHARACTER_CLONE + "])" + STR_CHARACTER_SEP + STR_REGEX_INTEGER_ID;

	public static final Matcher
	//MATCHER_SOFT_EVIDENCE       = Pattern.compile( STR_REGEX_SOFT_EVIDENCE ).matcher( "" ),
	//MATCHER_CLONE               = Pattern.compile( STR_REGEX_CLONE         ).matcher( "" ),
	  MATCHER_AUXILIARY_VAR       = Pattern.compile( STR_REGEX_AUXILIARY_VAR ).matcher( "" );

	public static final Dimension
	  DIMENSION_MINIMUM           = new Dimension( 0x40, 0x20 ),
	  DIMENSION_SOFT_EVIDENCE     = new Dimension( 0x20, 0x20 );

	public static final double
	  NEGATIVE_ONE                = (double) -1;

	public static final int
	  INT_WIDTH_DIGITS            = 4;

	/** @since 20091124 */
	static public int audit( final BeliefNetwork bn, final Variable source, final Variable sink, final RecoveryInfo info, final String prefix, final String suffix ){
		if( (info == null) || (bn == null) ){ return 0; }
		String already = null, idsource = source == null ? null : source.getID(), idsink = sink == null ? null : sink.getID();
		for( Recoverable recoverable : info.recoverables ){
			if( (already = recoverable.softevidence( bn ).getID()).equals( idsource ) || already.equals( idsink ) ){
				throw new IllegalArgumentException( prefix + " auxiliary soft evidence " + suffix );
			}
			if( (already = recoverable.clone(        bn ).getID()).equals( idsource ) || already.equals( idsink ) ){
				throw new IllegalArgumentException( prefix + " auxiliary clone " + suffix );
			}
		}
		return info.size();
	}

	static public Collection<FiniteVariable> replaceEdge( final BeliefNetwork bn, final FiniteVariable source, final FiniteVariable sink, Collection<FiniteVariable> bucket ){
		if( ! (bn.outGoing( source ).contains( sink ) && bn.inComing( sink ).contains( source)) ){ return bucket; }

		if( ! (source instanceof PropertySuperintendent) ){ throw new IllegalArgumentException( "source variable must implement edu.ucla.belief.io.PropertySuperintendent" ); }
		if( ! (  sink instanceof PropertySuperintendent) ){ throw new IllegalArgumentException(   "sink variable must implement edu.ucla.belief.io.PropertySuperintendent" ); }
		if( ! (    bn instanceof PropertySuperintendent) ){ throw new IllegalArgumentException(  "belief network must implement edu.ucla.belief.io.PropertySuperintendent" ); }

		final Map<Object,Object> propertiesSource = properties( (PropertySuperintendent) source ),
		                         propertiesSink   = properties( (PropertySuperintendent)   sink ),
		                         propertiesBN     = properties( (PropertySuperintendent)     bn );

		if( propertiesSource == null ){ throw new IllegalArgumentException( "source variable getProperties() returned null" ); }
		if( propertiesSink   == null ){ throw new IllegalArgumentException(   "sink variable getProperties() returned null" ); }
		if( propertiesBN     != null ){ audit( bn, source, sink, recoverables( propertiesBN, KEY_RECOVERABLES ), "Will not replace", "edge." ); }

		if( ! bn.removeEdge( source, sink, false ) ){ throw new IllegalStateException( "failed to remove edge from belief network of type " + bn.getClass().getName() ); }

		LinkedList<Throwable> thrown = new LinkedList<Throwable>();

		final Map<Object,Object> pSE = new HashMap<Object,Object>( 0x10 ),
		                         pUP = new HashMap<Object,Object>( 0x10 );

		final Set<Integer>      used = new HashSet<Integer>( 2 );
		for( FiniteVariable fv : variables( bn ) ){
			if( MATCHER_AUXILIARY_VAR.reset( fv.getID() ).matches() ){
				used.add( new Integer( MATCHER_AUXILIARY_VAR.group( 2 ) ) );
			}
		}
		int unique = 0;
		while( used.contains( unique ) ){ ++unique; }
		String uniqueSuffix = Integer.toString( unique );
		StringBuilder buff = new StringBuilder();
		for( int i=INT_WIDTH_DIGITS - uniqueSuffix.length(); i > 0; i-- ){ buff.append( '0' ); }
		uniqueSuffix = buff.append( uniqueSuffix ).toString();

		pSE.put( KEY_HUGIN_ID,        STR_PREFIX_SOFT_EVIDENCE + uniqueSuffix );
		pSE.put( KEY_HUGIN_LABEL,     STR_LABEL_SOFT_EVIDENCE  );
		pSE.put( KEY_HUGIN_STATES,    Arrays.asList( new String[]{ STR_SOFT_EVIDENCE_STATE0, STR_SOFT_EVIDENCE_STATE1 } ) );

		Dimension sizeU = null, sizeX = null, sizeUP = null, sizeSE = null, sizeDefault = DIMENSION_MINIMUM;
		pSE.put( KEY_HUGIN_NODE_SIZE, sizeSE = new Dimension( DIMENSION_SOFT_EVIDENCE ) );
		Point locU = null, centerU = null, locX = null, centerX = null, locSE = null, locUP = null;
		StandardNode snSource = null, snSink = null;
		try{
			sizeDefault   = sizeDefault( bn );
			sizeU         = size(    source, propertiesSource, sizeDefault );
			sizeX         = size(      sink, propertiesSink,   sizeDefault );
			if(   source instanceof StandardNode ){ snSource = (StandardNode) source; }
			if( snSource != null ){
				locU      = snSource  .getLocation( new     Point() );
			}

			if(   sink   instanceof StandardNode ){   snSink = (StandardNode)   sink; }
			if( snSink   != null ){
				locX      = snSink    .getLocation( new     Point() );
			}

			if(  locU    == null ){  locU = location( propertiesSource, KEY_HUGIN_POSITION  ); }
			if(  locX    == null ){  locX = location(   propertiesSink, KEY_HUGIN_POSITION  ); }
		}catch( Throwable throwable ){
			thrown.add( throwable );
		}finally{
			if(        locU == null ){  locU = new Point(0,0); }
			if(        locX == null ){  locX = new Point(0,0); }
		}

		boolean reflectY = (bn instanceof HuginNet) && ((HuginNet)bn).getVersion().shouldReflectY();

		sizeUP = sizeOfClone( sizeU );
		Dimension radiusUSE = new Dimension( (sizeU.width + sizeSE.width) >> 1, (sizeU.height + sizeSE.height) >> 1 ),
		          radiusXUP = new Dimension( (sizeX.width + sizeUP.width) >> 1, (sizeX.height + sizeUP.height) >> 1 );
		try{
			centerU = translate( new Point( locU ), sizeU.width >> 1, sizeU.height >> 1, reflectY );
			centerX = translate( new Point( locX ), sizeX.width >> 1, sizeX.height >> 1, reflectY );

			locSE   = translate( new Point( centerU ), - (sizeSE.width >> 1), - (sizeSE.height >> 1), reflectY );
			locUP   = translate( new Point( centerX ), - (sizeUP.width >> 1), - (sizeUP.height >> 1), reflectY );

		  //System.out.println( "centerU? [" + centerU.x + "," + centerU.y + "], centerX.x? [" + centerX.x + "," + centerX.y + "]" );
		  //System.out.println( "radiusUSE? [" + radiusUSE.width + "," + radiusUSE.height + "], radiusXUP.x? [" + radiusXUP.width + "," + radiusXUP.height + "]" );

			if( centerU.x == centerX.x )//vertical line (divide by zero)
			{
				int dySE = radiusUSE.height;
				int dyUP = radiusXUP.height;
				if( centerU.y > centerX.y ){ dyUP = -dyUP; }
				else{                        dySE = -dySE; }
				translate( locSE, 0, dySE, reflectY );
				translate( locUP, 0, dyUP, reflectY );
			}
			else
			{
				double
				  slope  = ((double)(centerU.y - centerX.y))/((double)(centerU.x - centerX.x)),
				  width  = radiusUSE.getWidth(),
				  height = radiusUSE.getHeight(),
				  theta  = Math .atan( slope ),//*(width/height) ),
				  cos    = Math  .cos( theta ),
				  sin    = Math  .sin( theta ),
				  dx     = cos * width,
				  dy     = sin * height;

				if( centerX.x > centerU.x ){ dy *= NEGATIVE_ONE; }
				else{                        dx *= NEGATIVE_ONE; }

				translate( locSE, (int) dx, (int) dy, reflectY );

				width  = radiusXUP.getWidth();
				height = radiusXUP.getHeight();
				dx     = cos * width;
				dy     = sin * height;

				if( centerX.x > centerU.x ){ dx *= NEGATIVE_ONE; }
				else{                        dy *= NEGATIVE_ONE; }

				translate( locUP, (int) dx, (int) dy, reflectY );
			}
		}catch( Throwable throwable ){
			thrown.add( throwable );
		}finally{
		}

	  //System.out.println( "locSE? [" + locSE.x + "," + locSE.y + "], locUP.x? [" + locUP.x + "," + locUP.y + "]" );

		if( locSE != null ){ pSE.put( KEY_HUGIN_POSITION, locSE ); }

		final FiniteVariable varSE = bn.newFiniteVariable( pSE );
		varSE.setProperty( HiddenProperty.PROPERTY, HiddenProperty.PROPERTY.TRUE );

		pUP.put( KEY_HUGIN_ID,        STR_PREFIX_CLONE + uniqueSuffix );
		String labelU = propertiesSource.containsKey( KEY_HUGIN_LABEL ) ? propertiesSource.get( KEY_HUGIN_LABEL ).toString() : "U";
		pUP.put( KEY_HUGIN_LABEL,     labelU + "'"  );
		pUP.put( KEY_HUGIN_STATES,    source.instances() );
		pUP.put( KEY_HUGIN_NODE_SIZE, sizeUP );
		if( locUP != null ){ pUP.put( KEY_HUGIN_POSITION, locUP ); }

		final FiniteVariable varUP = bn.newFiniteVariable( pUP );
		varUP.setProperty( HiddenProperty.PROPERTY, HiddenProperty.PROPERTY.TRUE );

		if( bucket == null ){ bucket = new LinkedList<FiniteVariable>(); }
		try{
			bn     .addVariable( varSE, true );
			bucket .add(         varSE );
			bn     .addVariable( varUP, true );
			bucket .add(         varUP );
		}catch( Throwable throwable ){
			thrown.add( throwable );
		}

		try{
			bn     .addEdge(    source, varSE,  true );
			bn     .addEdge(     varUP,  sink, false );
		}catch( Throwable throwable ){
			thrown.add( throwable );
		}

		try{
			Table tableSE = varSE.getCPTShell( DSLNodeType.CPT ).getCPT();
			int       len = tableSE.getCPLength();
			for( int i=0; i<len; ){ tableSE.setCP( i++, 1.0 ); tableSE.setCP( i++, 0.0 ); }
		}catch( Throwable throwable ){
			thrown.add( throwable );
		}

		try{
			new CPTShells( "Macros.replaceEdge( "+source.getID()+" -> "+sink.getID()+" ) substituting "+varUP.getID()+" for "+source.getID()+" in "+sink.getID()+"'s cpt" ){
				public void doTask( FiniteVariable shellsvar, DSLNodeType type, CPTShell shell ){
					if( shell != null ){ shell.replaceVariables( old2new, true ); }
				}
				private Map<FiniteVariable,FiniteVariable> old2new = Collections.singletonMap( source, varUP );
			}.forAllDSLNodeTypes( sink );
		}catch( Throwable throwable ){
			thrown.add( throwable );
		}

		try{
			bn.getEvidenceController().observe( varSE, STR_SOFT_EVIDENCE_STATE0 );
		}catch( Throwable throwable ){
			thrown.add( throwable );
		}

		try{
		  //log( propertiesSource, KEY_SOFT_EVIDENCE_CHILDREN,   sink, varSE ); log(   propertiesSink,   KEY_APPROXIMATED_PARENTS, source, varUP );
			log( unique, source, varSE, varUP, sink, propertiesSource, propertiesSink, bn );
		}catch( Throwable throwable ){
			thrown.add( throwable );
		}

		report( thrown, "Macros.replaceEdge()" );

		return bucket;
	}

	/** @since 20080222 */
	static public Dimension size( FiniteVariable var, Map<Object,Object> props, Dimension sizeDefault ){
		Dimension    size = null;
		StandardNode sn   = null;
		if(   var instanceof StandardNode ){ sn = (StandardNode) var; }
		if( sn   != null ){
			size  = sn .getDimension( new Dimension() );
		}

		if( zero( size ) && (props != null) ){ size = size( props, KEY_HUGIN_NODE_SIZE ); }

		if( zero( size ) ){ size = sizeDefault; }

		return size;
	}

	/** @since 20080222 */
	static public Dimension sizeDefault( BeliefNetwork bn ){
		Dimension sizeDefault = DIMENSION_MINIMUM;
		HuginNet hnet = null;
		if( bn instanceof HuginNet ){ hnet = (HuginNet) bn; }
		if( hnet != null ){
			sizeDefault = hnet.getGlobalNodeSize( new Dimension() );
		}

		if( zero( sizeDefault ) ){
			Map<Object,Object> bnProps = properties( (PropertySuperintendent) bn );
			if( bnProps.containsKey(         KEY_HUGIN_NODE_SIZE ) ){
				sizeDefault = size( bnProps, KEY_HUGIN_NODE_SIZE );
			}
		}

		if( zero( sizeDefault ) ){
			sizeDefault = DIMENSION_MINIMUM;
		}

		return sizeDefault;
	}

	/** @since 20080222 */
	static public boolean zero( Dimension dim ){
		return (dim == null) || (dim.width < 1) || (dim.height < 1);
	}

	/** @since 20080222 */
	static public Dimension sizeOfClone( Dimension sizeOfU ){
		return new Dimension( Math.max( DIMENSION_MINIMUM.width, sizeOfU.width >> 1), Math.max( DIMENSION_MINIMUM.height, sizeOfU.height >> 1 ) );
	}

	/** @since 20080222 */
	static public Point translate( Point point, int dx, int dy, boolean reflectY ){
		point.x +=                   dx;
		point.y += (reflectY ? -dy : dy);
		return point;
	}

	/** @since 20080221 */
	static public boolean report( LinkedList<Throwable> thrown, String method ){
		if( thrown == null ){ return true; }
		if( ! thrown.isEmpty() ){
			System.err.println( "warning: " + method + " caught " + thrown.size() + " exceptions:" );
			for( Throwable throwable : thrown ){
				if( Definitions.DEBUG ){ throwable.printStackTrace( System.err ); }
				else{ System.err.println( thrown ); }

			}
			return false;
		}
		return true;
	}

	public static class Recoverable implements Cloneable{
		public Recoverable( int unique, FiniteVariable source, FiniteVariable softevidence, FiniteVariable clone, FiniteVariable sink ){
			this.unique       = unique;
			this.source       = source;
			this.softevidence = softevidence;
			this.clone        = clone;
			this.sink         = sink;
			this.split        = new String[]{ Integer.toString( unique ), source.getID(), softevidence.getID(), clone.getID(), sink.getID() };
		}

		/** @since 20081022 */
		public boolean isBridge( BeliefNetwork bn ){
			return Macros.isBridge( source( bn ), sink( bn ), bn );
		}

		/** @since 20080225 */
		public Object clone(){ return this; }//new Recoverable( this.unique, this.split );

		/** @since 20080225 */
		private Recoverable( int unique, String[] split ){
			if( split == null ){ throw new IllegalArgumentException(); }
			this.unique       = unique;
			this.split        = split;
		}

		public Recoverable( String str, BeliefNetwork bn ){
			split             = str.split( STR_IO_SEPARATE_FIELDS );
			if( split.length != 5 ){ throw new IllegalArgumentException( "malformed recoverable \""+str+"\"" ); }

			this.unique              = Integer.parseInt( split[0] );
			FiniteVariable    fvsink = sink( bn ), fvsource = source( bn );
			RecoveryInfo source_info = this.register( (PropertySuperintendent) fvsource, KEY_SOFT_EVIDENCE_CHILDREN );
			RecoveryInfo   sink_info = this.register( (PropertySuperintendent)   fvsink, KEY_APPROXIMATED_PARENTS   );

			try{
				properties( (PropertySuperintendent) fvsink ).put( KEY_IDS_RECOVERABLE_PARENTS, sink_info.sourcesToString( STR_IO_SEPARATE_FIELDS, bn ) );
			}catch( Throwable thrown ){
				System.err.println( "warning: Macros.Recoverable() caught " + thrown );
			}

			FiniteVariable se = null, up = null;
			try{
				se = this.softevidence( bn );
				if( se instanceof StandardNode ){
					((StandardNode)se).setDimension( DIMENSION_SOFT_EVIDENCE );
				}
				up = this.clone( bn );
				if( up instanceof StandardNode ){
					Map<Object,Object> props = (fvsource instanceof PropertySuperintendent) ? properties( (PropertySuperintendent) fvsource ) : null;
					((StandardNode)up).setDimension( sizeOfClone( size( fvsource, props, sizeDefault( bn ) ) ) );
				}
			}catch( Throwable thrown ){
				System.err.println( "warning: Macros.Recoverable() caught " + thrown );
			}

			try{
				if( se != null ){ bn.getEvidenceController().observe( se, se.instance( STR_SOFT_EVIDENCE_STATE0 ) ); }
			}catch( Throwable thrown ){
				System.err.println( "warning: Macros.Recoverable() caught " + thrown );
			}

		  /*System.err.println( "new Recoverable "+unique+", "+source.getID()+", "+softevidence.getID()+", "+clone.getID()+", "+sink.getID()+"" );
			System.err.println( "    source property " + recoverables( properties( (PropertySuperintendent) this.source ), KEY_SOFT_EVIDENCE_CHILDREN ).iterator().next() );
			System.err.println( "      sink property " + recoverables( properties( (PropertySuperintendent)   this.sink ), KEY_APPROXIMATED_PARENTS   ).iterator().next() );*/
		}

		private RecoveryInfo register( PropertySuperintendent psuper, Object key ){
			RecoveryInfo recoverables = null;
			try{
				Map<Object,Object>        properties = properties( psuper );
				Object                         value = properties.get( key );
				if( value instanceof RecoveryInfo ){ recoverables = recoverables( properties, key ); }
				else{ properties.put( key, recoverables = new RecoveryInfo() ); }
				recoverables.add( this );
			}catch( Throwable thrown ){
				System.err.println( "warning: Recoverable.register() caught " + thrown );
			}
			return recoverables;
		}

		private FiniteVariable validate( BeliefNetwork bn, String id, String description ){
			FiniteVariable var = (FiniteVariable) bn.forID( id );
			if( var == null ){ throw new IllegalArgumentException( description+" variable "+id+" not found for recoverable "+unique ); }
			return var;
		}

		public String toString(){
			return Integer.toString( unique );
		}

		public Appendable append( Appendable app ) throws java.io.IOException{
			if( source == null && split == null ){ throw new IllegalStateException(); }
			return app
			.append(                             Integer.toString( unique ) ).append( STR_IO_SEPARATE_FIELDS )
			.append( source       == null ? split[1] : source      .getID() ).append( STR_IO_SEPARATE_FIELDS )
			.append( softevidence == null ? split[2] : softevidence.getID() ).append( STR_IO_SEPARATE_FIELDS )
			.append( clone        == null ? split[3] : clone       .getID() ).append( STR_IO_SEPARATE_FIELDS )
			.append( sink         == null ? split[4] : sink        .getID() );
		}

		/** @since 20081022 */
		public Appendable append( BeliefNetwork bn, Appendable app ) throws java.io.IOException{
			return app.append( "[ " ).append( source( bn ).toString() ).append( " -> " ).append( sink( bn ).toString() ).append( " ]" );
		}

		/** @since 20081022 */
		public FiniteVariable[] asArray( BeliefNetwork bn ){
			return new FiniteVariable[]{ source( bn ), sink( bn ) };
		}

		private boolean validate( BeliefNetwork bn ){
			if( valid == bn ){ return true; }

			this.source       = validate( bn, split[1], "source"       );
			this.softevidence = validate( bn, split[2], "softevidence" );
			this.clone        = validate( bn, split[3], "clone"        );
			this.sink         = validate( bn, split[4], "sink"         );

			valid = bn;
			return true;
		}

		public FiniteVariable            sink(         BeliefNetwork bn ){
			return validate( bn ) ? this.sink         : null;
		}

		public FiniteVariable            source(       BeliefNetwork bn ){
			return validate( bn ) ? this.source       : null;
		}

		public FiniteVariable            clone(        BeliefNetwork bn ){
			return validate( bn ) ? this.clone        : null;
		}

		public FiniteVariable            softevidence( BeliefNetwork bn ){
			return validate( bn ) ? this.softevidence : null;
		}

		/** @since 20080223 */
		public FiniteVariable[] recover( BeliefNetwork bn ){
			FiniteVariable fvsrce = source( bn ), fvsink = sink( bn );
			recoverEdge( bn, fvsrce, fvsink, new LinkedList<FiniteVariable>() );
			return new FiniteVariable[]{ fvsrce, fvsink };
		}

		final     public   String[]        split;
		transient private  FiniteVariable  source, softevidence, clone, sink;
		final     public   int             unique;
		transient private  BeliefNetwork   valid;
	}

	/** @since 20080221 */
	public static class RecoveryInfo implements Cloneable{
		public RecoveryInfo(){}

		public RecoveryInfo( Recoverable ... recoverables ){
			this.add( recoverables );
		}

		/** @since 20080225 */
		public Object clone(){
			RecoveryInfo ret = new RecoveryInfo();
			for( Recoverable recoverable : recoverables ){ ret.add( recoverable ); }
			return ret;
		}

		public RecoveryInfo      add( Recoverable ... recoverables ){
			for( Recoverable recoverable : recoverables ){ this.recoverables   .add( recoverable ); }
			return this;
		}

		public RecoveryInfo   remove( Recoverable ... recoverables ){
			for( Recoverable recoverable : recoverables ){ this.recoverables.remove( recoverable ); }
			return this;
		}

		public Recoverable   forSink( FiniteVariable   sink, BeliefNetwork bn ){
			Recoverable found = null;
			for( Recoverable recoverable : recoverables ){ if( recoverable  .sink( bn ) ==   sink ){ found = recoverable; break; } }
			return found;
		}

		public Recoverable forSource( FiniteVariable source, BeliefNetwork bn ){
			Recoverable found = null;
			for( Recoverable recoverable : recoverables ){ if( recoverable.source( bn ) == source ){ found = recoverable; break; } }
			return found;
		}

		public boolean isEmpty(){
			return this.recoverables.isEmpty();
		}

		public int size(){
			return this.recoverables.size();
		}

		public String toString(){
			if( builder == null ){ builder = new StringBuilder( recoverables.size() * 4 ); }
			else{                  builder.setLength(0); }

			try{
				for( Recoverable recoverable : recoverables ){
					builder.append( recoverable.unique ).append( STR_IO_SEPARATE_OBJECTS );
				}
			}catch( Throwable thrown ){
				System.err.println( "warning: Macros.RecoveryInfo.toString() caught " + thrown );
			}
			return builder.toString();
		}

		public String sourcesToString( String separator, BeliefNetwork bn ){
			if( builder == null ){ builder = new StringBuilder( recoverables.size() * 0x10 ); }
			else{                  builder.setLength(0); }

			try{
				for( Recoverable recoverable : recoverables ){
					builder.append( recoverable.source( bn ).getID() ).append( separator );
				}
				builder.setLength( builder.length() - separator.length() );
			}catch( Throwable thrown ){
				System.err.println( "warning: Macros.sourcesToString.toString() caught " + thrown );
				thrown.printStackTrace( System.err );
			}
			return builder.toString();
		}

		/** @since 20080223 */
		public Recoverable[] asArray(){
			return recoverables.toArray( new Recoverable[ recoverables.size() ] );
		}

		protected List<Recoverable> recoverables = new LinkedList<Recoverable>();
		protected StringBuilder     builder;
	}

	/** @since 20080221 */
	public static class Recoverables extends RecoveryInfo implements Cloneable, BeliefNetwork.Auditor{
		public Recoverables(){}

		public Recoverables( Recoverable ... recoverables ){
			super( recoverables );
		}

		/** @since 20080225 */
		public Object clone(){
			Recoverables ret = new Recoverables();
			for( Recoverable recoverable : recoverables ){ ret.add( recoverable ); }
			return ret;
		}

		public String toString(){
			if( builder == null ){ builder = new StringBuilder( recoverables.size() * 0x40 ); }
			else{                  builder.setLength(0); }

			try{
				for( Recoverable recoverable : recoverables ){
					recoverable.append( builder ).append( STR_IO_SEPARATE_OBJECTS );
				}
			}catch( Throwable thrown ){
				System.err.println( "warning: Macros.Recoverables.toString() caught " + thrown );
			}

			return builder.toString();
		}

		/** @since 20091124
			@return an error message to veto creation/removal of the edge, or creation/removal of the variable */
		public String audit( BeliefNetwork bn, Variable from, Variable to, Collection targets, Deed deed ){
			Map<Object,Object> props = null;
			if( from == null && bn instanceof PropertySuperintendent && ((props = properties((PropertySuperintendent) bn)) != null) && props.containsKey( KEY_RECOVERABLES ) ){
				return "Will not " + deed.intention.verb + " auxiliary " + deed.unit.noun + ".";
			}
			if( deed == BeliefNetwork.Auditor.Deed.DROP_NODE ){
				Object             key   = null;
				if( ((from instanceof PropertySuperintendent) && ((props = properties((PropertySuperintendent) from)) != null) && (props.containsKey( key = KEY_SOFT_EVIDENCE_CHILDREN ) || props.containsKey( key = KEY_APPROXIMATED_PARENTS ))) ||
					((to   instanceof PropertySuperintendent) && ((props = properties((PropertySuperintendent)   to)) != null) && (props.containsKey( key = KEY_SOFT_EVIDENCE_CHILDREN ) || props.containsKey( key = KEY_APPROXIMATED_PARENTS ))) ){
					return "Will not " + deed.intention.verb + " auxiliary " + ((key == KEY_SOFT_EVIDENCE_CHILDREN) ? "soft evidence " : "clone ") + deed.unit.noun + ".";
				}
			}
			try{
				String    prefix  = "Will not " + deed.intention.verb;
				if(    deed.unit == Unit.belief ){
					String    id  = null;
					if(   auxiliaries == null ){ auxiliaries = new HashSet<String>(); }
					else{ auxiliaries.clear(); }
					Recoverable rec = null;
					for( Iterator it = recoverables.iterator(); it.hasNext(); ){
						auxiliaries.add( (rec = (Recoverable) it.next()).softevidence( bn ).getID() );
						auxiliaries.add(  rec                                  .clone( bn ).getID() );
					}
					if( from     != null && auxiliaries.contains( id = from.getID() ) ){ return prefix + " auxiliary " + id + "."; }
					if( targets  != null ){
						for( Iterator it = targets.iterator(); it.hasNext(); ){
							if( auxiliaries.contains( id = ((Variable) it.next()).getID() ) ){ return prefix + " auxiliary " + id + "."; }
						}
					}
				}else{
					if( from     != null ){ Macros.audit( bn, from, to, this, prefix, deed.unit.noun + "." ); }
				}
			}catch( Throwable thrown ){
				return thrown.getMessage();
			}
			if( deed == BeliefNetwork.Auditor.Deed.CREATE_EDGE ){
				Map<String,Set<String>> moreedges = replacedEdges( bn );
				if( moreedges.containsKey( from.getID() ) && moreedges.get( from.getID() ).contains( to.getID() ) ){ return "Proposed edge [ "+from.getID()+" -> "+to.getID()+" ] collides with a replaced edge."; }
				if( ! maintainsAcyclicity( bn, moreedges, from, to ) ){ return "Proposed edge [ "+from.getID()+" -> "+to.getID()+" ] induces an illegal cycle."; }
			}
			return null;
		}

		/** @since 20091203 */
		public Map<String,Set<String>> replacedEdges( BeliefNetwork bn ){
			Map<String,Set<String>> moreedges = new HashMap<String,Set<String>>( size() );
			String                  source    = null;
			Set<String>             outgoing  = null;
			for( Recoverable recoverable : recoverables ){
				if( (outgoing = moreedges.get( source = recoverable.source( bn ).getID() )) == null ){ moreedges.put( source, outgoing = new HashSet<String>(1) ); }
				outgoing.add( recoverable.sink( bn ).getID() );
			}
			return moreedges;
		}

		/** @since 20091203 */
		public boolean maintainsAcyclicity( BeliefNetwork bn, Map<String,Set<String>> moreedges, Variable from, Variable to ){
			return ! hasPath( bn, moreedges == null ? replacedEdges( bn ) : moreedges, to.getID(), from );
		}

		/** @since 20091203 */
		@SuppressWarnings( "unchecked" )
		public boolean hasPath( BeliefNetwork bn, Map<String,Set<String>> moreedges, String vertex1, Variable vertex2 ){
			Set<Variable> out   = bn.outGoing( bn.forID( vertex1 ) );
			Set<String>   empty = Collections.emptySet();
			Set<String>   more  = moreedges.containsKey( vertex1 ) ? moreedges.get( vertex1 ) : empty;
			if( out.contains( vertex2 ) || more.contains( vertex2.getID() ) ){ return true; }
			else{
				for( Variable next : out  ){ if( hasPath( bn, moreedges, next.getID(), vertex2 ) ){ return true; } }
				for( String   next : more ){ if( hasPath( bn, moreedges, next        , vertex2 ) ){ return true; } }
			}
			return false;
		}

		static public BeliefNetwork postProcess( BeliefNetwork bn ){
			if( ! (bn instanceof PropertySuperintendent) ){ return bn; }

			Map<Object,Object> properties       =          properties( (PropertySuperintendent) bn );
			Object             objRecoverables  =          properties.get( KEY_RECOVERABLES );
			if( objRecoverables instanceof Recoverables ){ return bn; }
			String             strRecoverables  = (String) objRecoverables;
			if(                strRecoverables == null ){  return bn; }

			Recoverables recoverables = new Recoverables();
			Recoverable  recoverable  = null;
			for( String strRecoverable : strRecoverables.split( STR_IO_SEPARATE_OBJECTS ) ){
				recoverables.add( recoverable = new Recoverable( strRecoverable, bn ) );
			}

			register( bn, properties, recoverables );

			return bn;
		}

		transient private Set<String>   auxiliaries;
	}

	/** @since 20091124 */
	static public boolean register( BeliefNetwork bn, Map<Object,Object> properties, Recoverables recoverables ){
		boolean ret = false;
		if( properties != null ){ properties.put( KEY_RECOVERABLES, recoverables ); }
		if( bn         != null ){ ret = bn.addAuditor( recoverables ); }
		return ret;
	}

	static public Collection<FiniteVariable> recoverEdge( final BeliefNetwork bn, final FiniteVariable source, final FiniteVariable sink, Collection<FiniteVariable> bucket ){
		if( bn.outGoing( source ).contains( sink ) || bn.inComing( sink ).contains( source ) ){ return bucket; }

		if( ! (source instanceof PropertySuperintendent) ){ throw new IllegalArgumentException( "source variable must implement edu.ucla.belief.io.PropertySuperintendent" ); }
		if( ! (  sink instanceof PropertySuperintendent) ){ throw new IllegalArgumentException(   "sink variable must implement edu.ucla.belief.io.PropertySuperintendent" ); }
		if( ! (    bn instanceof PropertySuperintendent) ){ throw new IllegalArgumentException(  "belief network must implement edu.ucla.belief.io.PropertySuperintendent" ); }

		final Map<Object,Object> propertiesSource = properties( (PropertySuperintendent) source ),
		                         propertiesSink   = properties( (PropertySuperintendent)   sink );

		if( propertiesSource == null ){ throw new IllegalArgumentException( "source variable getProperties() returned null" ); }
		if(   propertiesSink == null ){ throw new IllegalArgumentException(   "sink variable getProperties() returned null" ); }

		LinkedList<Throwable> thrown = new LinkedList<Throwable>();

		RecoveryInfo recoverablesSE = recoverables( propertiesSource, KEY_SOFT_EVIDENCE_CHILDREN );
		if( recoverablesSE == null ){ throw new IllegalArgumentException( "source variable "+source.getID()+" lacks recovery information" ); }
		Recoverable recoverSE = recoverablesSE.forSink( sink, bn );
		if( recoverSE == null ){ throw new IllegalArgumentException( "source variable "+source.getID()+" lacks recovery information pertaining to sink variable "+sink.getID() ); }

		RecoveryInfo recoverablesUP = recoverables( propertiesSink, KEY_APPROXIMATED_PARENTS );
		if( recoverablesUP == null ){ throw new IllegalArgumentException( "sink variable "+sink.getID()+" lacks recovery information" ); }
		Recoverable recoverUP = recoverablesUP.forSource( source, bn );
		if( recoverUP == null ){ throw new IllegalArgumentException( "sink variable "+sink.getID()+" lacks recovery information pertaining to source variable "+source.getID() ); }

		if( recoverSE != recoverUP ){ throw new IllegalArgumentException( "recovery information mismatch for variables "+source.getID()+", "+sink.getID() ); }

		final FiniteVariable softevidence = recoverSE.softevidence( bn ), clone = recoverUP.clone( bn );

		if( bucket == null ){ bucket = new LinkedList<FiniteVariable>(); }
		try{
			recoverablesSE.remove( recoverSE );
			if( recoverablesSE.isEmpty() ){ propertiesSource.remove( KEY_SOFT_EVIDENCE_CHILDREN ); }
		}catch( Throwable throwable ){
			thrown.add( throwable );
		}

		try{
			recoverablesUP.remove( recoverUP );
			if( recoverablesUP.isEmpty() ){   propertiesSink.remove( KEY_APPROXIMATED_PARENTS   ); propertiesSink.remove( KEY_IDS_RECOVERABLE_PARENTS ); }
			else{ propertiesSink.put( KEY_IDS_RECOVERABLE_PARENTS, recoverablesUP.sourcesToString( STR_IO_SEPARATE_FIELDS, bn ) ); }
		}catch( Throwable throwable ){
			thrown.add( throwable );
		}

		try{
			Map<Object,Object> global = properties( (PropertySuperintendent) bn );
			Recoverables recoverables = (Recoverables) global.get( KEY_RECOVERABLES );
			recoverables.remove( recoverUP );
			if( recoverables.isEmpty() ){
				bn.removeAuditor( recoverables );
				global.remove( KEY_RECOVERABLES ); }
		}catch( Throwable throwable ){
			thrown.add( throwable );
		}

		try{
			bn.removeEdge(     clone, sink, false );
			bn.removeVariable( clone );
			bucket.add(        clone );
		}catch( Throwable throwable ){
			thrown.add( throwable );
		}

		try{
			bn.removeEdge( source, softevidence, false );
			bn.removeVariable(     softevidence );
			bucket.add(            softevidence );
		}catch( Throwable throwable ){
			thrown.add( throwable );
		}

		try{
			bn.addEdge( source, sink, false );
		}catch( Throwable throwable ){
			thrown.add( throwable );
		}

		try{
			new CPTShells( "Macros.recoverEdge( "+source.getID()+" -> "+sink.getID()+" ) substituting "+source.getID()+" for "+clone.getID()+" in "+sink.getID()+"'s cpt" ){
				public void doTask( FiniteVariable shellsvar, DSLNodeType type, CPTShell shell ){
					if( shell != null ){ shell.replaceVariables( old2new, true ); }
				}
				private Map<FiniteVariable,FiniteVariable> old2new = Collections.singletonMap( clone, source );
			}.forAllDSLNodeTypes( sink );
		}catch( Throwable throwable ){
			thrown.add( throwable );
		}

		report( thrown, "Macros.recoverEdge()" );

		return bucket;
	}

	/** http://en.wikipedia.org/wiki/Bridge_(graph_theory)
		@since 20081022 */
	public static final FiniteVariable[][] findBurntBridges( final BeliefNetwork bn ){
		Map<Object,Object> properties = properties(     (PropertySuperintendent) bn );
		if(   properties == null ){ return null; }
		Recoverables     recoverables = (Recoverables) properties.get( KEY_RECOVERABLES );
		if( recoverables == null || recoverables.isEmpty() ){ return null; }

		Collection<Recoverable> burnt = new LinkedList<Recoverable>();
		for( Recoverable recoverable : recoverables.asArray() ){
			if( recoverable.isBridge( bn ) ){ burnt.add( recoverable ); }
		}

		if( burnt.isEmpty() ){ return null; }

		FiniteVariable[][] ret = new FiniteVariable[ burnt.size() ][];
		int i = 0;
		for( Recoverable recoverable : burnt ){
			ret[ i++ ] = recoverable.asArray( bn );
		}
		return ret;
	}

	/** http://en.wikipedia.org/wiki/Bridge_(graph_theory)
		@since 20081022 */
	public static boolean isBridge( FiniteVariable source, FiniteVariable sink, BeliefNetwork bn ){
	  //System.out.println( "Macros.isBridge( "+source+", "+sink+" )" );
	  //if( bn.containsEdge( source, sink ) ){ return null; }
		Set<FiniteVariable> visited = new HashSet<FiniteVariable>(0x10);
		visited.add( sink );
		int result = depthFirstDepth( 0, source, sink, bn, visited, 1 );
	  //System.out.println( "    result? " + result );
		return result < 0;
	}

	/** @since 20081022 */
	public static int depthFirstDepth( int depth, FiniteVariable source, FiniteVariable sink, BeliefNetwork bn, Set<FiniteVariable> visited, int floor ){
	  //System.out.println( "depth( "+depth+", "+source+", "+sink+", "+visited+", "+floor+" )" );
		visited.add( source );

		Collection<FiniteVariable> outGoing = variables( bn.outGoing( source ) );
		Collection<FiniteVariable> inComing = variables( bn.inComing( source ) );

		if( ++depth > floor ){
			if( outGoing.contains( sink ) ){ return depth; }
			if( inComing.contains( sink ) ){ return depth; }
		}

		int result = -1;
		for( FiniteVariable outgoing : outGoing ){
			if( (! visited.contains( outgoing )) && ((result = depthFirstDepth( depth, outgoing, sink, bn, visited, floor )) > 0) ){ return result; }
		}
		for( FiniteVariable incoming : inComing ){
			if( (! visited.contains( incoming )) && ((result = depthFirstDepth( depth, incoming, sink, bn, visited, floor )) > 0) ){ return result; }
		}

		return -1;
	}

	/** @since 20081024 */
	static public class RankingArgs{
		public RankingArgs( BeliefNetwork bn, EdgeDeletionInferenceEngine engine, RankingHeuristic heuristic, int count ){
			this.bn        = bn;
			this.engine    = engine;
			this.heuristic = heuristic;
			this.count     = count;
		}

		public final BeliefNetwork               bn;
		public final EdgeDeletionInferenceEngine engine;
		public final RankingHeuristic            heuristic;
		public final int                         count;
		public       Bridge                      bridge;
		public       FiniteVariable[][]          ranked;
	}

	/** @since 20081023 */
	public static RankingArgs firstK( RankingArgs args ) throws Exception{
		if( args.count < 1 ){ return null; }

		Map<Object,Object> properties = properties(     (PropertySuperintendent) args.bn );
		if(   properties == null ){ return null; }
		Recoverables     recoverables = (Recoverables) properties.get( KEY_RECOVERABLES );
		if( recoverables == null || recoverables.isEmpty() ){ return null; }

		args.bridge                 = args.engine == null ? convert( args.bn ) : args.engine.bridge();
		Converter            verter = args.bridge.verter;
		EDAlgorithm     edalgorithm = args.bridge.edalgorithm;
		int[][]              ranked = args.heuristic.rank( edalgorithm );

		FiniteVariable[][]   firstk = new FiniteVariable[ args.count ][];
		for( int i=0; i<args.count; i++ ){
			firstk[i] = new FiniteVariable[]{
				(FiniteVariable) args.bn.forID( verter.convert( ranked[i][0] ).getID() ),
				(FiniteVariable) args.bn.forID( verter.convert( ranked[i][1] ).getID() )
			};
		}

		args.ranked = firstk;

		return args;
	}

	/** @since 20071216 */
	public static final <T extends Appendable> T id( T app, Object obj ){
		if( app == null ) return app;
		try{
			if( obj == null ) app.append( "null" );
			app.append( obj.getClass().getSimpleName() ).append( '@' ).append( Integer.toString( System.identityHashCode( obj ), 0x10 ) );
		}catch( java.io.IOException ioe ){
			System.err.println( "warning: Macros.id() caught " + ioe );
		}
		return app;
	}

	static public Dimension size( Map<Object,Object> properties, Object key ){
		if(   properties             == null ){ return null; }
		if( ! properties .containsKey( key ) ){ return null; }
		Object value = properties.get( key );
		if(      value instanceof Dimension ){ return (Dimension) value; }
		else if( value instanceof      List ){
			float[] array = list2array( (List) value );
			return new Dimension( (int) array[0], (int) array[1] );
		}
		else if( value.getClass().isArray() ){
			double[] array = array2array( value );
			return new Dimension( (int) array[0], (int) array[1] );
		}
		return null;
	}

	static public Point location( Map<Object,Object> properties, Object key ){
		if(   properties             == null ){ return null; }
		if( ! properties .containsKey( key ) ){ return null; }
		Object value = properties.get( key );
		if(      value instanceof Point ){ return (Point) value; }
		else if( value instanceof  List ){
			float[] array = list2array( (List) value );
			return new Point( (int) array[0], (int) array[1] );
		}
		else if( value.getClass().isArray() ){
			double[] array = array2array( value );
			return new Point( (int) array[0], (int) array[1] );
		}
		return null;
	}

	static public float[] list2array( List list ){
		float[] ret = new float[ Math.max( 2, list.size() ) ];
		Arrays.fill( ret, 0 );
		int i = 0;
		for( Object obj : list ){
			try{
				ret[i++] = Float.parseFloat( obj.toString() );
			}catch( Throwable thrown ){
				System.err.println( "warning: Macros.list2array() caught " + thrown );
			}
		}
		return ret;
	}

	static public double[] array2array( Object array ){
		int   length = getLength( array );
		double[] ret = new double[ Math.max( 2, length ) ];
		Arrays.fill( ret, 0 );
		Doubler doubler = Doubler.forArray( array );
		for( int i=0; i<length; i++ ){ ret[i] = doubler.get( array, i ); }
		return ret;
	}

	/** Convert {@link java.lang.reflect.Array arrays} with various {@link java.lang.Class#getComponentType component types} to individual primitive {@link java.lang.Double#TYPE double} values.
		@author keith cascio
		@since  20080220 */
	public enum Doubler{
		/** {@link java.lang.Boolean#TYPE boolean[]} */
		BOOLEAN(   Boolean.TYPE  ){ public double get( Object ar, int in ){ return         getBoolean( ar, in ) ? 1.0 : 0.0;    } },/** {@link java.lang.Byte#TYPE        byte[]} */
		BYTE   (      Byte.TYPE  ){ public double get( Object ar, int in ){ return  (double)  getByte( ar, in );                } },/** {@link java.lang.Character#TYPE   char[]} */
		CHAR   ( Character.TYPE  ){ public double get( Object ar, int in ){ return  (double)  getChar( ar, in );                } },/** {@link java.lang.Double#TYPE    double[]} */
		DOUBLE (    Double.TYPE  ){ public double get( Object ar, int in ){ return          getDouble( ar, in );                } },/** {@link java.lang.Float#TYPE      float[]} */
		FLOAT  (     Float.TYPE  ){ public double get( Object ar, int in ){ return  (double) getFloat( ar, in );                } },/** {@link java.lang.Integer#TYPE      int[]} */
		INT    (   Integer.TYPE  ){ public double get( Object ar, int in ){ return  (double)   getInt( ar, in );                } },/** {@link java.lang.Long#TYPE        long[]} */
		LONG   (      Long.TYPE  ){ public double get( Object ar, int in ){ return  (double)  getLong( ar, in );                } },/** {@link java.lang.Short#TYPE      short[]} */
		SHORT  (     Short.TYPE  ){ public double get( Object ar, int in ){ return  (double) getShort( ar, in );                } },/** {@link java.util.concurrent.atomic.AtomicInteger AtomicInteger[]} | {@link java.util.concurrent.atomic.AtomicLong AtomicLong[]} | {@link java.math.BigDecimal BigDecimal[]} | {@link java.math.BigInteger BigInteger[]} | {@link java.lang.Byte Byte[]} | {@link java.lang.Double Double[]} | {@link java.lang.Float Float[]} | {@link java.lang.Integer Integer[]} | {@link java.lang.Long Long[]} | {@link java.lang.Short Short[]} */
		NUMBER (    Number.class ){ public double get( Object ar, int in ){ return ((Number)Array.get( ar, in )).doubleValue(); } },/** {@link java.lang.String String[]} and any other component type where {@link java.lang.Object#toString toString()} can be passed to {@link java.lang.Double Double}.{@link java.lang.Double#parseDouble parseDouble()}. */
		STRING (    Object.class ){ public double get( Object ar, int in ){
				try{
					return Double.parseDouble( Array.get( ar, in ).toString() );
				}catch( Throwable thrown ){
					return -1.0;
				}
			}
		};

		/** Choose the appropriate {@link Doubler Doubler} for the specified array. */
		static public Doubler forArray( Object array ){
			Class<?> type = array.getClass().getComponentType();
			for( Doubler doubler : values() ){
				if( doubler.clazz.isAssignableFrom( type ) ){ return doubler; }
			}
			return STRING;
		}

		/** Convert the element of the specified array at the specified index to a primitive {@link java.lang.Double#TYPE double} value. */
		public double get( Object array, int index ){ return 0.0; }

		/** A particular {@link Doubler Doubler} is designed to convert {@link java.lang.reflect.Array arrays} with {@link java.lang.Class#getComponentType component type} {@link java.lang.Class#isAssignableFrom assignable from} this type. */
		final   public   Class<?> clazz;
		private Doubler( Class<?> clazz ){ this.clazz = clazz; }
	}

	/** suppress unchecked caste warning */
	@SuppressWarnings( "unchecked" )
	static public Map<Object,Object> properties( PropertySuperintendent ps ){ return (Map<Object,Object>) ps.getProperties(); }

	/** suppress unchecked caste warning */
	@SuppressWarnings( "unchecked" )
	static public Collection<FiniteVariable> variables( Object bn ){ return (Collection<FiniteVariable>) bn; }

	/** suppress unchecked caste warning */
	@SuppressWarnings( "unchecked" )
	static public RecoveryInfo recoverables( Map<Object,Object> properties, Object key ){ return (RecoveryInfo) properties.get( key ); }

  /*static public List<Recoverable> log( Map<Object,Object> properties, Object key, FiniteVariable original, FiniteVariable simplification ){
		List<Recoverable> recoverables  = recoverables( properties,     key );
		if(               recoverables == null ){       properties.put( key, recoverables = new LinkedList<Recoverable>() ); }
		recoverables.add( new Recoverable( original, simplification ) );
		return recoverables;
	}*/

	/** @since 20080221 */
	static public RecoveryInfo log( Map<Object,Object> properties, Object key, Recoverable recoverable ){
		RecoveryInfo      recoverables  = recoverables( properties,     key );
		if(               recoverables == null ){       properties.put( key, recoverables = new RecoveryInfo() ); }
		recoverables.add( recoverable );
		return recoverables;
	}

	/** @since 20080221 */
	static public Recoverable log( int unique, FiniteVariable source, FiniteVariable softevidence, FiniteVariable clone, FiniteVariable sink, Map<Object,Object> propertiesSource, Map<Object,Object> propertiesSink, BeliefNetwork bn ){
		Recoverable recoverable = new Recoverable( unique, source, softevidence, clone, sink );
		RecoveryInfo source_info = log( propertiesSource, KEY_SOFT_EVIDENCE_CHILDREN, recoverable );
		RecoveryInfo   sink_info = log( propertiesSink,   KEY_APPROXIMATED_PARENTS,   recoverable );
		propertiesSink.put( KEY_IDS_RECOVERABLE_PARENTS, sink_info.sourcesToString( STR_IO_SEPARATE_FIELDS, bn ) );

		try{
			Map<Object,Object> properties = properties( (PropertySuperintendent) bn );
			Recoverables recoverables = (Recoverables) properties.get( KEY_RECOVERABLES );
			if( recoverables == null ){ register( bn, properties, recoverables = new Recoverables( recoverable ) ); }
			else{ recoverables.add( recoverable ); }
		}catch( Throwable thrown ){
			System.err.println( "warning: Macros.log() caught " + thrown );
		}

		return recoverable;
	}

	/** @since 20080221 */
	static public Bridge convert( BeliefNetwork bn ) throws Exception{
		return convert( bn, new SettingsImpl<EdgeDeletionBeliefPropagationSetting>( EdgeDeletionBeliefPropagationSetting.class ), null );
	}

	/** @since 20080225 */
	static public Bridge convert( BeliefNetwork bn, Settings<EdgeDeletionBeliefPropagationSetting> settings, Map<DynamatorImpl,Dynamator> team ) throws Exception{
		if( ! (    bn instanceof PropertySuperintendent) ){ throw new IllegalArgumentException(  "belief network must implement edu.ucla.belief.io.PropertySuperintendent" ); }

		BeliefNetwork             original = bn;
		Recoverable[]                array = null;
		Map<FiniteVariable,Integer>
							 u2id          = null,
							 x2id          = null,
		                clones2edgeindices = null,
		         softevidences2edgeindices = null,
		             variables2ids         = new HashMap<FiniteVariable,Integer>( original.size() );
		FiniteVariable[][]       recovered = null;
		Converter                   verter = new Converter();
		BayesianNetwork    bayesiannetwork = null;
		int[][]               edgesDeleted = null;
		Map<Object,Object>      properties = properties( (PropertySuperintendent) bn );
		Object             objrecoverables = properties.get( KEY_RECOVERABLES );

		if( objrecoverables == null ){//no deleted edges
			clones2edgeindices             = softevidences2edgeindices = Collections.emptyMap();

			recovered                      = new FiniteVariable[0][];
			bayesiannetwork                = verter.convert( bn );

			for( FiniteVariable fVar : variables( original ) ){
				variables2ids.put( fVar, verter.convert( fVar ) );
			}

			edgesDeleted                   = new int[0][];
		}
		else{//at least one deleted edge
			Object    originalrecoverables = objrecoverables;
			bn                             = (BeliefNetwork) bn.clone();
			properties                     = properties( (PropertySuperintendent) bn );
			objrecoverables                = properties.get( KEY_RECOVERABLES );

			if( objrecoverables instanceof String ){ Recoverables.postProcess( bn ); }

			Recoverables      recoverables = (Recoverables) properties.get( KEY_RECOVERABLES );
			if( recoverables == originalrecoverables ){ register( bn, properties, recoverables = (Recoverables) recoverables.clone() ); }
			array                          = recoverables.asArray();

			            u2id               = new HashMap<FiniteVariable,Integer>( array.length );
			            x2id               = new HashMap<FiniteVariable,Integer>( array.length );
				   clones2edgeindices      = new HashMap<FiniteVariable,Integer>( array.length );
			softevidences2edgeindices      = new HashMap<FiniteVariable,Integer>( array.length );

			recovered                      = new FiniteVariable[ array.length ][];
			int                          i = 0;
			for( Recoverable recoverable : array ){
				clones2edgeindices        .put( (FiniteVariable) original.forID( recoverable       .clone( bn ) .getID() ), i );
				softevidences2edgeindices .put( (FiniteVariable) original.forID( recoverable.softevidence( bn ) .getID() ), i );
				recovered[ i++ ]           = recoverable.recover( bn );
			}

			bayesiannetwork                = verter.convert( bn );

			FiniteVariable              in = null;
			for( Recoverable recoverable : array ){
				in                                                             = recoverable      .source( bn );
				u2id                      .put( (FiniteVariable) original.forID(                              in.getID() ), verter.convert( in ) );
				in                                                             = recoverable        .sink( bn );
				x2id                      .put( (FiniteVariable) original.forID(                              in.getID() ), verter.convert( in ) );
			}

			FiniteVariable    intermediate = null;
			for( FiniteVariable fVar : variables( original ) ){
				if( clones2edgeindices.keySet().contains( fVar ) || softevidences2edgeindices.keySet().contains( fVar ) ){ continue; }
				intermediate               = (FiniteVariable) bn.forID( fVar.getID() );
				if( intermediate == null ){ throw new IllegalStateException( "intermediate network missing variable " + fVar.getID() ); }
				variables2ids.put( fVar, verter.convert( intermediate ) );
			}

			edgesDeleted                   = new int[ recovered.length ][];
			for( i=0; i < recovered.length; i++ ){
				edgesDeleted[i]            = new int[]{ verter.convert( recovered[i][0] ), verter.convert( recovered[i][1] ) };
			}
		}

		DynamatorImpl                dynamatorimpl = (DynamatorImpl)     settings.get( subalgorithm );
		if( dynamatorimpl == null ){ dynamatorimpl = (DynamatorImpl) subalgorithm.get( defaultValue ); }
		Dynamator                    dynamator     = team == null ? null : team.get( dynamatorimpl );
		if( dynamator     == null ){ dynamator     = dynamatorimpl.create(); }
		Algorithm                        algorithm = dynamatorimpl.algorithm();
		Map<Algorithm.Setting,Object>  subsettings = subsettings( dynamator, (PropertySuperintendent) bn );

		EDAlgorithm edalgorithm = new EDAlgorithm( bayesiannetwork, edgesDeleted, ((Number) settings.get( iterations )).intValue(), ((Number) settings.get( timeout )).longValue(), ((Number) settings.get( threshold )).doubleValue(), algorithm, subsettings );
		Bridge      bridge      = new Bridge( original, verter, array, recovered, edgesDeleted, edalgorithm, clones2edgeindices, softevidences2edgeindices, variables2ids, u2id, x2id );

		return bridge;
	}

	/** @since 20081030 */
	@SuppressWarnings( "unchecked" )
	static public Map<Algorithm.Setting,Object> subsettings( Dynamator dynamator, PropertySuperintendent bn ){
		return (Map<Algorithm.Setting,Object>) dynamator.toIL2Settings( bn );
	}

	/** @since 20080228 */
	public enum Category{
		auxiliary( "auxiliary", getKeyStroke( VK_A, 0 ), getKeyStroke( VK_A, SHIFT_MASK ), "\u201cS\u201ds and \u201cU prime\u201ds" ){
			public Collection<FiniteVariable> me( Bridge bridge, Collection<FiniteVariable> bucket ){
				bucket.   addAll( bridge.       clones2edgeindices.keySet() );
				bucket.   addAll( bridge.softevidences2edgeindices.keySet() );
				return bucket;
			}
			public Collection<FiniteVariable> co( Bridge bridge, Collection<FiniteVariable> bucket ){
				bucket.   addAll( bridge.    variables2ids        .keySet() );
				return bucket;
			}
		},
		softevidence( "soft evidence", getKeyStroke( VK_E, 0 ), getKeyStroke( VK_E, SHIFT_MASK ), "\u201cS\u201ds" ){
			public Collection<FiniteVariable> me( Bridge bridge, Collection<FiniteVariable> bucket ){
				bucket.   addAll( bridge.softevidences2edgeindices.keySet() );
				return bucket;
			}
			@SuppressWarnings( "unchecked" )
			public Collection<FiniteVariable> co( Bridge bridge, Collection<FiniteVariable> bucket ){
				bucket.   addAll( bridge.original );
				bucket.removeAll( bridge.softevidences2edgeindices.keySet() );
				return bucket;
			}

			public edu.ucla.belief.Table table( FiniteVariable var, Bridge bridge ){ return bridge.getSoftEvidenceTable( var ); }
		},
		clone( "clones", getKeyStroke( VK_C, 0 ), getKeyStroke( VK_C, SHIFT_MASK ), "\u201cU prime\u201ds" ){
			public Collection<FiniteVariable> me( Bridge bridge, Collection<FiniteVariable> bucket ){
				bucket.   addAll( bridge.       clones2edgeindices.keySet() );
				return bucket;
			}
			@SuppressWarnings( "unchecked" )
			public Collection<FiniteVariable> co( Bridge bridge, Collection<FiniteVariable> bucket ){
				bucket.   addAll( bridge.original );
				bucket.removeAll( bridge.       clones2edgeindices.keySet() );
				return bucket;
			}

			public edu.ucla.belief.Table table( FiniteVariable var, Bridge bridge ){ return bridge.getCloneTable( var ); }
		},
		u( "\u201cU\u201ds", getKeyStroke( VK_U, 0 ), getKeyStroke( VK_U, SHIFT_MASK ), "parents of deleted edges" ){
			public Collection<FiniteVariable> me( Bridge bridge, Collection<FiniteVariable> bucket ){
				bucket.    addAll( bridge.           u2id         .keySet() );
				return bucket;
			}
			@SuppressWarnings( "unchecked" )
			public Collection<FiniteVariable> co( Bridge bridge, Collection<FiniteVariable> bucket ){
				bucket.   addAll( bridge.original );
				bucket.removeAll( bridge.            u2id         .keySet() );
				return bucket;
			}
		},
		x( "\u201cX\u201ds", getKeyStroke( VK_X, 0 ), getKeyStroke( VK_X, SHIFT_MASK ), "children of deleted edges" ){
			public Collection<FiniteVariable> me( Bridge bridge, Collection<FiniteVariable> bucket ){
				bucket.    addAll( bridge.           x2id         .keySet() );
				return bucket;
			}
			@SuppressWarnings( "unchecked" )
			public Collection<FiniteVariable> co( Bridge bridge, Collection<FiniteVariable> bucket ){
				bucket.   addAll( bridge.original );
				bucket.removeAll( bridge.            x2id         .keySet() );
				return bucket;
			}
		};

		private Category( String display, KeyStroke stroke, KeyStroke backstroke ){
			this( display, stroke, backstroke, null );
		}

		private Category( String display, KeyStroke stroke, KeyStroke backstroke, String extra ){
			this.display    =    display;
			this    .stroke =     stroke;
			this.backstroke = backstroke;
			this.extra      =      extra;
		}

		public edu.ucla.belief.Table table( FiniteVariable var, Bridge bridge ){ return null; }

		protected Collection<FiniteVariable>         me( Bridge bridge, Collection<FiniteVariable> bucket ){ return bucket; }
		protected Collection<FiniteVariable>         co( Bridge bridge, Collection<FiniteVariable> bucket ){ return bucket; }

		public    Collection<FiniteVariable> membership( Bridge bridge, Collection<FiniteVariable> bucket ){ bucket = bucket( bucket ); bucket.clear(); return me( bridge, bucket ); }
		public    Collection<FiniteVariable> complement( Bridge bridge, Collection<FiniteVariable> bucket ){ bucket = bucket( bucket ); bucket.clear(); return co( bridge, bucket ); }

		private Collection<FiniteVariable> bucket( Collection<FiniteVariable> bucket ){ return bucket == null ? new LinkedList<FiniteVariable>() : bucket; }

		final public String    display, extra;
		final public KeyStroke stroke, backstroke;
	}

	/** @since 20080223 */
	static public class Bridge implements EvidenceChangeListener{
		public Bridge( BeliefNetwork original, Converter verter, Recoverable[] recoverables, FiniteVariable[][] recovered, int[][] edgesDeleted, EDAlgorithm edalgorithm, Map<FiniteVariable,Integer> clones2edgeindices, Map<FiniteVariable,Integer> softevidences2edgeindices, Map<FiniteVariable,Integer> variables2ids, Map<FiniteVariable,Integer> u2id, Map<FiniteVariable,Integer> x2id ){
			this                    .original  =         original;
			this                .verter        =         verter;
			this                .recoverables  =         recoverables;
			this                .recovered     =         recovered;
			this                .edgesDeleted  =         edgesDeleted;
			this                .edalgorithm   =         edalgorithm;
			this        .clones2edgeindices    =          clones2edgeindices;
			this .softevidences2edgeindices    =   softevidences2edgeindices;
			this     .variables2ids            =       variables2ids;
			this             .u2id             =               u2id;
			this             .x2id             =               x2id;

			this .variables                    = Collections.unmodifiableSet( new HashSet<FiniteVariable>( Macros.variables( original ) ) );
		}

		/** @since 20080302 */
		@SuppressWarnings( "unchecked" )
		public FiniteVariable getUforS( FiniteVariable softevidence ){
			if( ! softevidences2edgeindices.containsKey( softevidence ) ){ throw new IllegalArgumentException( softevidence.getID() + " is not a soft evidence variable" ); }
			Set<FiniteVariable> incoming = (Set<FiniteVariable>) this.original.inComing( softevidence );
			if( incoming.size() != 1 ){ throw new IllegalStateException( softevidence.getID() + " should have exactly 1 parent" ); }
			return incoming.iterator().next();
		}

		public edu.ucla.belief.Table    getSoftEvidenceTable( FiniteVariable softevidence ){
			il2.model.Table    il2t  = edalgorithm.getSoftEvidenceCPT( softevidences2edgeindices.get( softevidence ) );
			double[]            row  = il2t.values();
			double[]         values  = new double[ il2t.sizeInt() * 2 ];
			int                   j  = -1;
			for( int              i  =  0; i<row.length; i++ ){
				                  j  = i << 1;
				values[           j] =       row[i];
				values[         ++j] = 1.0 - row[i];
			}

			FiniteVariable        u  = getUforS( softevidence );
			edu.ucla.belief.Table t  = new edu.ucla.belief.Table( new TableIndex( new FiniteVariable[]{ u, softevidence } ), values );
			return t;
		}

		public edu.ucla.belief.Table           getCloneTable( FiniteVariable clone ){
			return verter.convert( edalgorithm.getCloneCPT( clones2edgeindices.get( clone ) ) );
		}

		public edu.ucla.belief.Table          varConditional( FiniteVariable var ){
			il2.model.Table il2table = null;
			if(             clones2edgeindices.containsKey( var ) ){ il2table = edalgorithm.cloneConditional( clones2edgeindices    .get( var ) ); }
			else if( softevidences2edgeindices.containsKey( var ) ){
				return softEvidenceConditional( var );
			}
			else{                                        il2table = edalgorithm.  varConditional( variables2ids.get( var ) ); }
			return verter.convert( il2table );
		}

		public edu.ucla.belief.Table        exactConditional( FiniteVariable var ){
			if(             clones2edgeindices.containsKey( var ) ){ return null; }//zeroConditional( var );
			else if( softevidences2edgeindices.containsKey( var ) ){ return null; }//zeroConditional( var );

			BeliefNetwork intermediate = verter.getBeliefNetwork();
			if( ssengine == null ){
				if( ssenginegenerator == null ){ ssenginegenerator = new SSEngineGenerator(); }
				ssengine = (SSEngine) ssenginegenerator.manufactureInferenceEngine( intermediate );
				original.getEvidenceController().addPriorityEvidenceChangeListener( this );
				this.evidenceChanged( (EvidenceChangeEvent) null );
			}
			return ssengine.conditional( (FiniteVariable) intermediate.forID( var.getID() ) );
		}

		/** <p> interface EvidenceChangeListener
			<p> synchronize evidence between the original il1 belief network
			    and the intermediate representation used to compute exact answers.
			    surprisingly tedious.
			@since 20080227 */
		@SuppressWarnings( "unchecked" )
		public void evidenceChanged( EvidenceChangeEvent evidencechangeevent ){
			try{
				Collection<FiniteVariable> changed = null;
				if( (evidencechangeevent != null) && (evidencechangeevent.recentEvidenceChangeVariables != null) ){
					  changed = (Collection<FiniteVariable>) evidencechangeevent.recentEvidenceChangeVariables; }
				else{ changed = (Collection<FiniteVariable>) original; }

				if( exact_evidence   == null ){ exact_evidence   = new    HashMap<FiniteVariable,Object>( changed.size() ); }
				else{                           exact_evidence  .clear(); }

				if( exact_retraction == null ){ exact_retraction = new LinkedList<FiniteVariable>(); }
				else{                           exact_retraction.clear(); }

				BeliefNetwork         intermediate =   verter.getBeliefNetwork();
				EvidenceController    ecorig       = original.getEvidenceController();
				FiniteVariable        vint;
				Object                valueOrig;
				for( FiniteVariable var : changed ){
					if( (vint = (FiniteVariable) intermediate.forID( var.getID() )) == null ){ continue; }
					if( (valueOrig = ecorig.getValue( var )                       ) == null ){ exact_retraction.add( vint ); }
					else{ exact_evidence.put( vint, vint.instance( var.index( valueOrig ) ) ); }
				}

				EvidenceController    ecint        = intermediate.getEvidenceController();
				if( changed == original ){ ecint.setObservations( exact_evidence ); }
				else{                      ecint.observe(         exact_evidence );
					for( FiniteVariable retract : exact_retraction ){
					                       ecint.unobserve(              retract ); }
				}
			}catch( Throwable thrown ){
				System.err.println( "warning: Macros.Bridge.evidenceChanged() caught " + thrown );
				thrown.printStackTrace();
			}
		}

		/** interface EvidenceChangeListener
			@since 20080227 */
		public void         warning( EvidenceChangeEvent evidencechangeevent ){}

		public edu.ucla.belief.Table softEvidenceConditional( FiniteVariable var ){
			return new edu.ucla.belief.Table( new TableIndex( Collections.singleton( var ) ), ARRAY_SOFTEVIDENCE_MARGINAL );
		}

		public edu.ucla.belief.Table         zeroConditional( FiniteVariable var ){
			return new edu.ucla.belief.Table( new TableIndex( Collections.singleton( var ) ), new double[ var.size() ] );
		}

		public double prEvidence(){
			return edalgorithm.prEvidence();
		}

		public void setEvidence( Map<FiniteVariable,Object> il1evidence ){
			if(   this.evidence == null ){ this.evidence = new IntMap( il1evidence.size() ); }
			else{ this.evidence.clear(); }
			for( FiniteVariable fv : il1evidence.keySet() ){
				if( variables2ids.keySet().contains( fv ) ){ this.evidence.put( variables2ids.get( fv ), fv.index( il1evidence.get( fv ) ) ); }
			}
			edalgorithm.setEvidence( evidence );
		}

		public Set<FiniteVariable> variables(){ return this.variables; }

		public static final double[] ARRAY_SOFTEVIDENCE_MARGINAL = new double[]{ 1.0, 0.0 };

		/** @since 20080309 */
		public Bridge die(){
			try{
				BeliefNetwork originalbn  = original;
				if(             original != null ){ original.getEvidenceController().removePriorityEvidenceChangeListener( this ); }
				this           .original  = null;
			  //if(            variables != null ){ variables.clear(); }
				this          .variables  = null;
				if(         recoverables != null ){ Arrays.fill( recoverables, null ); }
				this       .recoverables  = null;
				if(            recovered != null ){ Arrays.fill(    recovered, null ); }
				this          .recovered  = null;
				if(          edalgorithm != null ){ edalgorithm.die(); }
				this        .edalgorithm  = null;
				if(    ssenginegenerator != null && verter != null ){
					BeliefNetwork intermediate = verter.getBeliefNetwork();
					if( (intermediate != null) && (intermediate instanceof PropertySuperintendent) ){
					   ssenginegenerator.killState( (PropertySuperintendent) intermediate );
					}
				}
				this  .ssenginegenerator  = null;
				if(             ssengine != null ){ ssengine.die(); }
				this           .ssengine  = null;
				if(       exact_evidence != null ){ exact_evidence.clear(); }
				this     .exact_evidence  = null;
				if(     exact_retraction != null ){ exact_retraction.clear(); }
				this   .exact_retraction  = null;
				if(               verter != null ){
					BeliefNetwork intermediate = verter.getBeliefNetwork();
					if(    (intermediate != null) && (intermediate != originalbn) ){
						intermediate.clear();
						if( intermediate instanceof PropertySuperintendent ){
							Map<?,?> props  = ((PropertySuperintendent) intermediate).getProperties();
							if(      props != null ){ props.clear(); }
						}
					}
				}
				this             .verter  = null;

				if(   clones2edgeindices != null ){ clones2edgeindices.clear(); }
				this .clones2edgeindices  = null;
				if(   softevidences2edgeindices != null ){ softevidences2edgeindices.clear(); }
				this .softevidences2edgeindices  = null;
				if(        variables2ids != null ){ variables2ids.clear(); }
				this      .variables2ids  = null;
				if(                 u2id != null ){ u2id.clear(); }
				this               .u2id  = null;
				if(                 x2id != null ){ x2id.clear(); }
				this               .x2id  = null;

				if(             evidence != null ){ evidence.clear(); }
				this           .evidence  = null;

			  //Recoverables auditor = (Recoverables) recoverables( properties( (PropertySuperintendent) originalbn ), KEY_RECOVERABLES );
			  //if( auditor != null ){ originalbn.removeAuditor( auditor ); }
			}catch( Exception thrown ){
				System.err.println( "warning: Macros.Bridge.die() caught " + thrown );
			}

			return this;
		}

		transient public            BeliefNetwork      original;
		transient public        Set<FiniteVariable>    variables;
		transient public               Recoverable[]   recoverables;
		transient public            FiniteVariable[][] recovered;
		transient public                       int[][] edgesDeleted;
		transient public               EDAlgorithm     edalgorithm;
		transient private        SSEngineGenerator     ssenginegenerator;
		transient private        SSEngine              ssengine;
		transient private  Map<FiniteVariable,Object>  exact_evidence;
		transient private  Collection<FiniteVariable>  exact_retraction;
		transient public                 Converter     verter;
		transient public   Map<FiniteVariable,Integer> clones2edgeindices, softevidences2edgeindices, variables2ids, u2id, x2id;
	  //transient private  Map<FiniteVariable,Object>  evidence;
		transient private  IntMap                      evidence;
	}

	/** @since 20091202
		unit tests */
	public static void main( String[] args ){
		boolean doCompile = false;
		for( String arg : args ){
			if( arg.equals( "compile" ) ){ doCompile = true; }
		}
		System.exit( unitTests( doCompile ) );
	}

	/** @since 20091203
		unit tests

		To understand the need for the setEvidence() call,
		see SamIam code NetworkInternalFrame.handleInferenceEngine(),
		specifically the line (~2368):
		IE.evidenceChanged( new EvidenceChangeEvent( evidence ) );

		and Inflib code EdgeDeletionInferenceEngine.evidenceChanged(),
		specifically the line (~570):
		bridge.setEvidence( (Map<FiniteVariable,Object>) this.beliefnetwork.getEvidenceController().evidence() ); */
	@SuppressWarnings( "unchecked" )
	public static int testCompile( BeliefNetwork bn, double expectedValue, java.io.PrintStream stream, int id ){
		stream.printf( FMT, id++, "compile" );
		Throwable caught = null;
		Bridge    bridge = null;
		int       ret    = -1;
		double    pr     = -1.0;
		try{
			bridge = Macros.convert( bn );
			bridge.setEvidence( (Map<FiniteVariable,Object>) bn.getEvidenceController().evidence() );
			pr     = bridge.prEvidence(); }
		catch( Throwable thrown ){ caught = thrown; }
		if( (caught == null) && (bridge != null) && (Math.abs(pr - expectedValue) < 0.0000000000000001) ){ ret = 0; stream.println( OK ); }
		else{ ret = 9; stream.println( FAIL ); }
		bridge.die();
		return ret;
	}

	/** @since 20091202
		unit tests */
	public static int unitTests( boolean doCompile ){
		long                 start  = System.currentTimeMillis();
		java.io.PrintStream  stream = Definitions.STREAM_TEST;
		Throwable            caught = null;
		int
		  count  = -1,
		  status = -1,
		  id     = UNIT_ID = 0;
		stream.printf( FMT1, "testing edge deletion operations" );
		stream.println(); stream.println();

		stream.printf( FMT, id++, "create test network" );
		HuginNet           bn = null, bn2 = null;
		FiniteVariable     v1 = null, v2 = null, v3 = null, v4 = null;
		EvidenceController ec = null;
		try{
			bn = new HuginNetImpl();
			v1 = new HuginNodeImpl( new FiniteVariableImpl( "v1", new String[]{ "t", "f" } ) );
			v2 = new HuginNodeImpl( new FiniteVariableImpl( "v2", new String[]{ "t", "f" } ) );
			v3 = new HuginNodeImpl( new FiniteVariableImpl( "v3", new String[]{ "t", "f" } ) );
			v4 = new HuginNodeImpl( new FiniteVariableImpl( "v4", new String[]{ "t", "f" } ) );
			bn.addVariable( v1,     true );
			bn.addVariable( v2,     true );
			bn.addVariable( v3,     true );
			bn.addVariable( v4,     true );
			bn.addEdge(     v1, v2, true );
			bn.addEdge(     v1, v3, true );
			bn.addEdge(     v1, v4, true );
			bn.addEdge(     v2, v3, true );
			ec = bn.getEvidenceController();
		}catch( Throwable thrown ){
			stream.println( FAIL ); thrown.printStackTrace(); return 9;
		}
		stream.println( OK );

		stream.printf( FMT, id++, "count variables" );
		if( bn.size() == 4 ){ stream.println( OK   ); }
		else{                 stream.println( FAIL ); return 9; }

		stream.printf( FMT, id++, "count edges" );
		if( bn.numEdges() == 4 ){ stream.println( OK   ); }
		else{                     stream.println( FAIL ); return 9; }

		if( doCompile ){ if( (status = testCompile( bn, 1.0, stream, id++ )) != 0 ){ return status; } }

		stream.printf( FMT, id++, "replace edge" );
		Collection<FiniteVariable> bucket = new HashSet<FiniteVariable>( 3 );
		bucket = Macros.replaceEdge( bn, v2, v3, bucket );
		FiniteVariable
		  se = (FiniteVariable) bn.forID( "s_0000" ),
		  cl = (FiniteVariable) bn.forID( "u_0000" );
		if( (se != null) && (cl != null) && (bn.size() == 6) && (bn.numEdges() == 5) ){ stream.println( OK ); }
		else{ stream.println( FAIL ); return 9; }

		stream.printf( FMT, id++, "write network to file" );
		java.io.File fyle = new java.io.File( String.valueOf( start ) + "edbpunittest.net" );
		fyle.deleteOnExit();
		if( fyle.exists() ){
			stream.println( FAIL );
			stream.printf( "Error: file %s already exists.", fyle.getPath() );
			return 9;
		}
		try{ NetworkIO.writeNetwork( bn, fyle ); }catch( Throwable thrown ){ caught = thrown; }
		if( (caught == null) && fyle.exists() && (fyle.length() > 0x10) ){ stream.println( OK ); }
		else{ stream.println( FAIL ); return 9; }

		if( (status = unitTests( stream, doCompile, id, bn, v1, v2, v3, v4, se, cl )) != 0 ){ return status; }
		id = UNIT_ID;

		stream.printf( FMT, id++, "read network from file (then duplicate tests)" );
		try{ bn2 = (HuginNet) NetworkIO.read( fyle ); }catch( Throwable thrown ){ caught = thrown; thrown.printStackTrace(); }
		if( (caught == null) && (bn2.size() == 6) && (bn2.numEdges() == 5) ){ stream.println( OK ); }
		else{ stream.println( FAIL ); return 9; }

		stream.printf( FMT, id++, "verify variables" );
		FiniteVariable v1p = (FiniteVariable) bn2.forID( "v1" ), v2p = (FiniteVariable) bn2.forID( "v2" ), v3p = (FiniteVariable) bn2.forID( "v3" ), v4p = (FiniteVariable) bn2.forID( "v4" ), sep = (FiniteVariable) bn2.forID( "s_0000" ), clp = (FiniteVariable) bn2.forID( "u_0000" );
		if( (v1p != null) && (v2p != null) && (v3p != null) && (v4p != null) && (sep != null) && (clp != null) ){ stream.println( OK ); }
		else{ stream.println( FAIL ); return 9; }

		if( (status = unitTests( stream, doCompile, id, bn2, v1p, v2p, v3p, v4p, sep, clp )) != 0 ){ return status; }
		id = UNIT_ID;

		long elapsed = System.currentTimeMillis() - start;
		stream.println();
		stream.printf( FMT1, String.format( "passed all %d in %.3f seconds (%d milliseconds)", id, ((float)elapsed)/1000.0, elapsed ) );
		stream.println( OK );

		return 0;
	}

	public static final String
	  FMT1 =      "%-52s...",
	  FMT  = "%03d %-48s...",
	  OK   = "ok",
	  FAIL = "fail";
	private static int
	  UNIT_ID;

	/** @since 20091202
		unit tests */
	public static int unitTests( java.io.PrintStream stream, boolean doCompile, int id, HuginNet bn, FiniteVariable v1, FiniteVariable v2, FiniteVariable v3, FiniteVariable v4, FiniteVariable se, FiniteVariable cl ){
		Throwable            caught = null;
		int
		  count  = -1,
		  status = -1;

		EvidenceController ec = bn.getEvidenceController();
		Collection<FiniteVariable> bucket = new HashSet<FiniteVariable>( 3 );

		stream.printf( FMT, id++, "verify recovery info logged" );
		if( bn.getProperties().containsKey( KEY_RECOVERABLES ) ){ stream.println( OK ); }
		else{ stream.println( FAIL ); return 9; }

		if( doCompile ){
			if( (status = testCompile( bn, 1.0, stream, id++ )) != 0 ){ return status; }

			stream.printf( FMT, id++, "assert  legal evidence" );
			caught = null;
			count  =   -1;
			try{ count = ec.observe( v1, v1.instance(0) ); }catch( Throwable thrown ){ caught = thrown; }
			if( (count == 1) && (caught == null) && (! ec.isEmpty()) && (ec.size() == 2) && ec.evidenceVariables().contains( v1 ) && ec.evidence().containsKey( v1 ) && (ec.getValue( v1 ) == v1.instance(0)) ){ stream.println( OK ); }
			else{ stream.println( FAIL ); return 9; }
			stream.flush();

			if( (status = testCompile( bn, 0.5, stream, id++ )) != 0 ){ return status; }

			stream.printf( FMT, id++, "retract legal evidence" );
			caught = null;
			count  =   -1;
			try{ count = ec.unobserve( v1 ); }catch( Throwable thrown ){ caught = thrown; }
			if( (count == 1) && (caught == null) && (! ec.isEmpty()) && (ec.size() == 1) && (! ec.evidenceVariables().contains( v1 )) && (! ec.evidence().containsKey( v1 )) && (ec.getValue( v1 ) == null) ){ stream.println( OK ); }
			else{ stream.println( FAIL ); return 9; }
			stream.flush();
		}

		stream.printf( FMT, id++, "veto delete  variable < soft ev >" );
		caught = null;
		try{ bn.removeVariable( se ); }catch( IllegalArgumentException iae ){ caught = iae; }
		if( (caught instanceof IllegalArgumentException) && (bn.size() == 6) && (bn.numEdges() == 5) ){ stream.println( OK ); }
		else{ stream.printf( "%s %d %d ", (caught == null ? "null" : caught.getClass().getSimpleName()), bn.size(), bn.numEdges() ); stream.println( FAIL ); return 9; }

		if( doCompile ){ if( (status = testCompile( bn, 1.0, stream, id++ )) != 0 ){ return status; } }

		stream.printf( FMT, id++, "veto delete  variable < clone   >" );
		caught = null;
		try{ bn.removeVariable( cl ); }catch( IllegalArgumentException iae ){ caught = iae; }
		if( (caught instanceof IllegalArgumentException) && (bn.size() == 6) && (bn.numEdges() == 5) ){ stream.println( OK ); }
		else{ stream.printf( "%s %d %d ", (caught == null ? "null" : caught.getClass().getSimpleName()), bn.size(), bn.numEdges() ); stream.println( FAIL ); return 9; }

		if( doCompile ){ if( (status = testCompile( bn, 1.0, stream, id++ )) != 0 ){ return status; } }

		stream.printf( FMT, id++, "veto delete  edge [   clone -> sink    ]" );
		caught = null;
		try{ bn.removeEdge( cl, v3 ); }catch( IllegalArgumentException iae ){ caught = iae; }
		if( (caught instanceof IllegalArgumentException) && (bn.size() == 6) && (bn.numEdges() == 5) ){ stream.println( OK ); }
		else{ stream.printf( "%s %d %d ", (caught == null ? "null" : caught.getClass().getSimpleName()), bn.size(), bn.numEdges() ); stream.println( FAIL ); return 9; }

		if( doCompile ){ if( (status = testCompile( bn, 1.0, stream, id++ )) != 0 ){ return status; } }

		stream.printf( FMT, id++, "veto delete  edge [  source -> soft ev ]" );
		caught = null;
		try{ bn.removeEdge( v2, se ); }catch( IllegalArgumentException iae ){ caught = iae; }
		if( (caught instanceof IllegalArgumentException) && (bn.size() == 6) && (bn.numEdges() == 5) ){ stream.println( OK ); }
		else{ stream.printf( "%s %d %d ", (caught == null ? "null" : caught.getClass().getSimpleName()), bn.size(), bn.numEdges() ); stream.println( FAIL ); return 9; }

		if( doCompile ){ if( (status = testCompile( bn, 1.0, stream, id++ )) != 0 ){ return status; } }

		stream.printf( FMT, id++, "veto replace edge [   clone -> sink    ]" );
		caught = null;
		bucket.clear();
		try{ bucket = Macros.replaceEdge( bn, cl, v3, bucket ); }catch( IllegalArgumentException iae ){ caught = iae; }
		if( (caught instanceof IllegalArgumentException) && (bn.size() == 6) && (bn.numEdges() == 5) ){ stream.println( OK ); }
		else{ stream.printf( "%s %d %d ", (caught == null ? "null" : caught.getClass().getSimpleName()), bn.size(), bn.numEdges() ); stream.println( FAIL ); return 9; }

		if( doCompile ){ if( (status = testCompile( bn, 1.0, stream, id++ )) != 0 ){ return status; } }

		stream.printf( FMT, id++, "veto replace edge [  source -> soft ev ]" );
		caught = null;
		bucket.clear();
		try{ bucket = Macros.replaceEdge( bn, v2, se, bucket ); }catch( IllegalArgumentException iae ){ caught = iae; }
		if( (caught instanceof IllegalArgumentException) && (bn.size() == 6) && (bn.numEdges() == 5) ){ stream.println( OK ); }
		else{ stream.printf( "%s %d %d ", (caught == null ? "null" : caught.getClass().getSimpleName()), bn.size(), bn.numEdges() ); stream.println( FAIL ); return 9; }

		if( doCompile ){ if( (status = testCompile( bn, 1.0, stream, id++ )) != 0 ){ return status; } }

		stream.printf( FMT, id++, "veto illegal edge [ soft ev -> var     ]" );
		caught = null;
		try{ bn.addEdge( se, v4, true ); }catch( IllegalArgumentException iae ){ caught = iae; }
		if( (caught instanceof IllegalArgumentException) && (bn.size() == 6) && (bn.numEdges() == 5) ){ stream.println( OK ); }
		else{ stream.printf( "%s %d %d ", (caught == null ? "null" : caught.getClass().getSimpleName()), bn.size(), bn.numEdges() ); stream.println( FAIL ); return 9; }

		if( doCompile ){ if( (status = testCompile( bn, 1.0, stream, id++ )) != 0 ){ return status; } }

		stream.printf( FMT, id++, "veto illegal edge [     var -> soft ev ]" );
		caught = null;
		try{ bn.addEdge( v4, se, true ); }catch( IllegalArgumentException iae ){ caught = iae; }
		if( (caught instanceof IllegalArgumentException) && (bn.size() == 6) && (bn.numEdges() == 5) ){ stream.println( OK ); }
		else{ stream.printf( "%s %d %d ", (caught == null ? "null" : caught.getClass().getSimpleName()), bn.size(), bn.numEdges() ); stream.println( FAIL ); return 9; }

		if( doCompile ){ if( (status = testCompile( bn, 1.0, stream, id++ )) != 0 ){ return status; } }

		stream.printf( FMT, id++, "veto illegal edge [   clone -> var     ]" );
		caught = null;
		try{ bn.addEdge( cl, v4, true ); }catch( IllegalArgumentException iae ){ caught = iae; }
		if( (caught instanceof IllegalArgumentException) && (bn.size() == 6) && (bn.numEdges() == 5) ){ stream.println( OK ); }
		else{ stream.printf( "%s %d %d ", (caught == null ? "null" : caught.getClass().getSimpleName()), bn.size(), bn.numEdges() ); stream.println( FAIL ); return 9; }

		if( doCompile ){ if( (status = testCompile( bn, 1.0, stream, id++ )) != 0 ){ return status; } }

		stream.printf( FMT, id++, "veto illegal edge [     var -> clone   ]" );
		caught = null;
		try{ bn.addEdge( cl, v4, true ); }catch( IllegalArgumentException iae ){ caught = iae; }
		if( (caught instanceof IllegalArgumentException) && (bn.size() == 6) && (bn.numEdges() == 5) ){ stream.println( OK ); }
		else{ stream.printf( "%s %d %d ", (caught == null ? "null" : caught.getClass().getSimpleName()), bn.size(), bn.numEdges() ); stream.println( FAIL ); return 9; }

		if( doCompile ){ if( (status = testCompile( bn, 1.0, stream, id++ )) != 0 ){ return status; } }

		stream.printf( FMT, id++, "veto retract     < soft ev >" );
		caught = null;
		count  = -1;
		try{ count = ec.unobserve( se ); }catch( IllegalArgumentException iae ){ caught = iae; }
		if( (count < 1) && (caught instanceof IllegalArgumentException) && (! ec.isEmpty()) && (ec.size() > 0) && ec.evidenceVariables().contains( se ) && ec.evidence().containsKey( se ) && (ec.getValue( se ) == se.instance(0)) ){ stream.println( OK ); }
		else{ stream.println( FAIL ); stream.printf( "count? %d, caught? %s\n", count, caught == null ? "null" : caught.getClass().getSimpleName() ); return 9; }

		if( doCompile ){ if( (status = testCompile( bn, 1.0, stream, id++ )) != 0 ){ return status; } }

		stream.printf( FMT, id++, "veto reset evidence" );
		caught = null;
		count  = -1;
		try{ count = ec.resetEvidence(); }catch( IllegalArgumentException iae ){ caught = iae; }
		if( (count < 1) && (caught instanceof IllegalArgumentException) && (! ec.isEmpty()) && (ec.size() > 0) && ec.evidenceVariables().contains( se ) && ec.evidence().containsKey( se ) && (ec.getValue( se ) == se.instance(0)) ){ stream.println( OK ); }
		else{ stream.println( FAIL ); return 9; }

		if( doCompile ){ if( (status = testCompile( bn, 1.0, stream, id++ )) != 0 ){ return status; } }

		stream.printf( FMT, id++, "veto setObservations()" );
		caught = null;
		count  = -1;
		try{ count = ec.setObservations( Collections.singletonMap( v4, v4.instance(0) ) ); }catch( IllegalArgumentException iae ){ caught = iae; }catch( Throwable thrown ){ thrown.printStackTrace(); }
		if( (count < 1) && (caught instanceof IllegalArgumentException) && (! ec.isEmpty()) && (ec.size() > 0) && ec.evidenceVariables().contains( se ) && ec.evidence().containsKey( se ) && (ec.getValue( se ) == se.instance(0)) ){ stream.println( OK ); }
		else{ stream.println( FAIL ); return 9; }

		if( doCompile ){ if( (status = testCompile( bn, 1.0, stream, id++ )) != 0 ){ return status; } }

		stream.printf( FMT, id++, "veto observe     < soft ev >" );
		caught = null;
		count  = -1;
		try{ count = ec.observe( se, STR_SOFT_EVIDENCE_STATE1 ); }catch( IllegalArgumentException iae ){ caught = iae; }catch( Throwable thrown ){ thrown.printStackTrace(); }
		if( (count < 1) && (caught instanceof IllegalArgumentException) && (! ec.isEmpty()) && (ec.size() > 0) && ec.evidenceVariables().contains( se ) && ec.evidence().containsKey( se ) && (ec.getValue( se ) == se.instance(0)) ){ stream.println( OK ); }
		else{ stream.println( FAIL ); return 9; }

		if( doCompile ){ if( (status = testCompile( bn, 1.0, stream, id++ )) != 0 ){ return status; } }

		stream.printf( FMT, id++, "veto observe all { soft ev }" );
		caught = null;
		count  = -1;
		try{ count = ec.observe( Collections.singletonMap( se, STR_SOFT_EVIDENCE_STATE1 ) ); }catch( IllegalArgumentException iae ){ caught = iae; }catch( Throwable thrown ){ thrown.printStackTrace(); }
		if( (count < 1) && (caught instanceof IllegalArgumentException) && (! ec.isEmpty()) && (ec.size() > 0) && ec.evidenceVariables().contains( se ) && ec.evidence().containsKey( se ) && (ec.getValue( se ) == se.instance(0)) ){ stream.println( OK ); }
		else{ stream.println( FAIL ); return 9; }

		if( doCompile ){ if( (status = testCompile( bn, 1.0, stream, id++ )) != 0 ){ return status; } }

		stream.printf( FMT, id++, "veto observe     < clone   >" );
		caught = null;
		count  = -1;
		try{ count = ec.observe( cl, cl.instance(0) ); }catch( IllegalArgumentException iae ){ caught = iae; }catch( Throwable thrown ){ thrown.printStackTrace(); }
		if( (count < 1) && (caught instanceof IllegalArgumentException) && (ec.size() == 1) && (! ec.evidenceVariables().contains( cl )) && (! ec.evidence().containsKey( cl )) && (ec.getValue( cl ) == null) ){ stream.println( OK ); }
		else{ stream.println( FAIL ); return 9; }

		if( doCompile ){ if( (status = testCompile( bn, 1.0, stream, id++ )) != 0 ){ return status; } }

		stream.printf( FMT, id++, "veto observe all { clone   }" );
		caught = null;
		count  = -1;
		try{ count = ec.observe( Collections.singletonMap( cl, cl.instance(0) ) ); }catch( IllegalArgumentException iae ){ caught = iae; }catch( Throwable thrown ){ thrown.printStackTrace(); }
		if( (count < 1) && (caught instanceof IllegalArgumentException) && (ec.size() == 1) && (! ec.evidenceVariables().contains( cl )) && (! ec.evidence().containsKey( cl )) && (ec.getValue( cl ) == null) ){ stream.println( OK ); }
		else{ stream.println( FAIL ); return 9; }

		if( doCompile ){ if( (status = testCompile( bn, 1.0, stream, id++ )) != 0 ){ return status; } }

		stream.printf( FMT, id++, "veto cycle" );
		caught = null;
		try{ bn.addEdge( v3, v2, true ); }catch( IllegalArgumentException iae ){ caught = iae; }
		if( (caught instanceof IllegalArgumentException) && (bn.size() == 6) && (bn.numEdges() == 5) ){ stream.println( OK ); }
		else{ stream.printf( "%s %d %d ", (caught == null ? "null" : caught.getClass().getSimpleName()), bn.size(), bn.numEdges() ); stream.println( FAIL ); return 9; }

		if( doCompile ){ if( (status = testCompile( bn, 1.0, stream, id++ )) != 0 ){ return status; } }

		stream.printf( FMT, id++, "veto edge collision" );
		caught = null;
		try{ bn.addEdge( v2, v3, true ); }catch( IllegalArgumentException iae ){ caught = iae; }
		if( (caught instanceof IllegalArgumentException) && (bn.size() == 6) && (bn.numEdges() == 5) ){ stream.println( OK ); }
		else{ stream.printf( "%s %d %d ", (caught == null ? "null" : caught.getClass().getSimpleName()), bn.size(), bn.numEdges() ); stream.println( FAIL ); return 9; }

		if( doCompile ){ if( (status = testCompile( bn, 1.0, stream, id++ )) != 0 ){ return status; } }

		stream.printf( FMT, id++, "refuse to recover unreplaced existing  edge" );
		bucket.clear();
		bucket = Macros.recoverEdge( bn, v1, v2, bucket );
		if( bucket.isEmpty() && (bn.size() == 6) && (bn.numEdges() == 5) ){ stream.println( OK ); }
		else{ stream.printf( "%d %d ", bn.size(), bn.numEdges() ); stream.println( FAIL ); return 9; }

		if( doCompile ){ if( (status = testCompile( bn, 1.0, stream, id++ )) != 0 ){ return status; } }

		stream.printf( FMT, id++, "refuse to recover unreplaced imaginary edge" );
		bucket.clear();
		try{ bucket = Macros.recoverEdge( bn, v3, v4, bucket ); }catch( IllegalArgumentException iae ){ caught = iae; }catch( Throwable thrown ){ thrown.printStackTrace(); }
		if( bucket.isEmpty() && (caught instanceof IllegalArgumentException) && (bn.size() == 6) && (bn.numEdges() == 5) ){ stream.println( OK ); }
		else{ stream.printf( "%d %d ", bn.size(), bn.numEdges() ); stream.println( FAIL ); return 9; }

		if( doCompile ){ if( (status = testCompile( bn, 1.0, stream, id++ )) != 0 ){ return status; } }

		stream.printf( FMT, id++, "recover edge" );
		bucket.clear();
		bucket = Macros.recoverEdge( bn, v2, v3, bucket );
		FiniteVariable
		  se_ = (FiniteVariable) bn.forID( "s_0000" ),
		  cl_ = (FiniteVariable) bn.forID( "u_0000" );
		if( (se_ == null) && (cl_ == null) && (bn.size() == 4) && (bn.numEdges() == 4) ){ stream.println( OK ); }
		else{ stream.printf( "%d %d ", bn.size(), bn.numEdges() ); stream.println( FAIL ); return 9; }

		stream.printf( FMT, id++, "verify recovery info destroyed" );
		if( bn.getProperties().containsKey( KEY_RECOVERABLES ) ){ stream.println( FAIL ); return 9; }
		else{ stream.println( OK ); }

		if( doCompile ){ if( (status = testCompile( bn, 1.0, stream, id++ )) != 0 ){ return status; } }

		UNIT_ID = id;

		return 0;
	}
}
