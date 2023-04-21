package il2.inf.jointree;

import        il2.inf.                                   PartialDerivativeEngine;
import        il2.inf.structure.      EliminationOrders. JT;
import        il2.model.                                 BayesianNetwork;

import        java.lang.reflect.                         Method;
import        java.lang.reflect.                         InvocationTargetException;
import        java.lang.reflect.                         Modifier;
import static java.lang.reflect.                         Modifier .*;
import        java.util.                                 EnumSet;
import        java.util.                                 Collection;
import        java.util.                                 Collections;
import        java.util.                                 Arrays;
import static java.lang.System .out;
import static java.lang.System .err;

/** {@link il2.inf.jointree.JoinTreeAlgorithm Join tree algorithm} "<b>flavor</b>", ie distinctive implementation.
	Some flavors support {@link il2.inf.PartialDerivativeEngine#varPartial( int ) partial derivatives}, others do not.
	Call {@link #partials() partials()} to get the flavors that do.

	@author keith cascio
	@since 20081029 */
public enum Flavor{
	/** shenoy-shafer -- <b>normalized</b>, {@link il2.inf.jointree.NormalizedSSAlgorithm    NormalizedSSAlgorithm}    */
	ssnormalized              (        NormalizedSSAlgorithm   .class ),
	/** shenoy-shafer -- <b>normalized, max-product</b>, {@link il2.inf.jointree.NormalizedMaxSSAlgorithm    NormalizedMaxSSAlgorithm}    */
	ssnormalizedmax           (        NormalizedMaxSSAlgorithm.class ),
	/** shenoy-shafer,          {@link il2.inf.jointree.UnindexedSSAlgorithm    UnindexedSSAlgorithm}    */
	shenoyshafer              (        UnindexedSSAlgorithm    .class ),
	/** hugin,                  {@link il2.inf.jointree.UnindexedHuginAlgorithm UnindexedHuginAlgorithm} */
	hugin                     (        UnindexedHuginAlgorithm .class ),
	/** "zero-conscious" hugin -- <b>normalized</b>, {@link il2.inf.jointree.NormalizedZCAlgorithm    NormalizedZCAlgorithm}    */
	zcnormalized              (        NormalizedZCAlgorithm   .class ),
	/** "zero-conscious" hugin, {@link il2.inf.jointree.UnindexedZCAlgorithm    UnindexedZCAlgorithm}    */
	zeroconscioushugin        (        UnindexedZCAlgorithm    .class );

	/** the flavors that support {@link #partial( il2.model.BayesianNetwork, il2.inf.structure.EliminationOrders.JT ) compiling} a {@link il2.inf.PartialDerivativeEngine partial derivative engine} */
	static   public    Collection<Flavor>   partials(){
		if( PARTIALS == null ){
			EnumSet<Flavor> list = EnumSet.noneOf( Flavor.class );
			for( Flavor flavor : values() ){ if( flavor.partial ){ list.add( flavor ); } }
			PARTIALS = Collections.unmodifiableSet( list );
		}
		return PARTIALS;
	}
	static   private   Collection<Flavor>   PARTIALS;

	/** the flavors that compute the log of answers to avoid underflow */
	static   public    Collection<Flavor>   normalized(){
		if( NORMALIZED == null ){
			EnumSet<Flavor> list = EnumSet.noneOf( Flavor.class );
			for( Flavor flavor : values() ){ if( flavor.normalized ){ list.add( flavor ); } }
			NORMALIZED = Collections.unmodifiableSet( list );
		}
		return NORMALIZED;
	}
	static   private   Collection<Flavor>   NORMALIZED;

	/** compile a {@link il2.inf.PartialDerivativeEngine partial derivative engine}, if {@link #partial supported} */
	public PartialDerivativeEngine partial(       BayesianNetwork bn, JT jt ){
		if( ! this.partial ){ throw new UnsupportedOperationException(); }
		return (PartialDerivativeEngine) this.compile( bn, jt );
	}

	/** compile a {@link il2.inf.jointree.JoinTreeAlgorithm JoinTreeAlgorithm} {@link il2.inf.JointEngine JointEngine} */
	public JoinTreeAlgorithm       compile(       BayesianNetwork bn, JT jt ){
		Throwable thrown = null;
		try{ return (JoinTreeAlgorithm) this.method.invoke( null, bn,    jt ); }
		catch( IllegalAccessException    iae ){ thrown = iae; }
		catch( InvocationTargetException ite ){ thrown = ite;
			Throwable causacausans = ite.getCause();
			if( causacausans instanceof OutOfMemoryError ){ throw (OutOfMemoryError) causacausans; }
            if( causacausans instanceof IllegalStateException ){ throw new OutOfMemoryError( causacausans.toString() ); } // AC: this is not correct
		}
		if( thrown != null ){
            err.append( "warning: Flavor." ).append( name() ).append( ".compile() caught " ).append( thrown.toString() ).append( "\n" );
            thrown.printStackTrace();
            Throwable causacausans = thrown.getCause();
            if( causacausans != null ){ causacausans.printStackTrace(); }
        }
		return null;
	}

	/** @param clazz implementation class */
	private Flavor( Class<? extends JoinTreeAlgorithm> clazz ){
		this.clazz      = clazz;
		this.method     = methodForClassSafe( clazz );
		this.partial    = (method != null) && PartialDerivativeEngine .class .isAssignableFrom( method.getReturnType() );
		this.normalized = shallowestContiguousConcreteDeclarer( clazz, STR_NAME_METHOD_NORMALIZED() ) != null;
	}

	/** the implementation class */
	public   final   Class<? extends JoinTreeAlgorithm>   clazz;
	/** the implementation class's factory method */
	public   final   Method                               method;
	/** supports {@link #partial( il2.model.BayesianNetwork, il2.inf.structure.EliminationOrders.JT ) compiling} a {@link il2.inf.PartialDerivativeEngine partial derivative engine} ?? */
	public   final   boolean                              partial;
	/** computes the log of answers to avoid underflow */
	public   final   boolean                              normalized;

	/** @since 20081103
		method name that identifies a normalizing JointEngine implementation */
	public    static final String               STR_NAME_METHOD_NORMALIZED(){ return "logPrEvidence"; }

	/** name factory methods */
	public    static final String               STR_NAME_METHOD(){ return "create"; }
	private   static       Class<?>[]           PARAMETER_TYPES;
	/** parameter types of factory methods */
	public    static final Class<?>[]           PARAMETER_TYPES(){
		if( PARAMETER_TYPES == null ){
			PARAMETER_TYPES = new Class<?>[]{ BayesianNetwork.class, JT.class };
		}
		return PARAMETER_TYPES;
	}
	/** return type of factory methods */
	public    static final Class<JoinTreeAlgorithm> RETURN_TYPE(){ return JoinTreeAlgorithm .class; }
	/** modifiers of factory methods */
	public    static final int                        MODIFIERS(){ return PUBLIC & STATIC; }

	/** find the factory method for a particular class, but don't throw any exceptions if not found */
	static public Method methodForClassSafe( Class<? extends JoinTreeAlgorithm> clazz ){
		try{
			return methodForClass( clazz );
		}catch( NoSuchMethodException nsme ){
			err.append( "warning: class " ).append( clazz.getName() ).append( " does not declare create( BayesianNetwork, JT )" ).append( "\n" );
		}
	  //for( Method method : clazz.getDeclaredMethods() ){
	  //	if( method.getName().equals( STR_NAME_METHOD() ) ){ out.println( method.toGenericString() ); }
	  //}
		return null;
	}

	/** find the factory method for a particular class using linear search */
	static public Method linearSearch( Class<? extends JoinTreeAlgorithm> clazz ) throws NoSuchMethodException{
		Class<?>[]    types        =    PARAMETER_TYPES();
		int           modifiers    =          MODIFIERS();
		String        name         =    STR_NAME_METHOD();
		Class<?>      rt           =        RETURN_TYPE();
	  /*out.append( "linearSearch( " ).append( clazz.toString() ).append( " )\n" );
		out.append( "    STR_NAME_METHOD()? " ).append( STR_NAME_METHOD() ).append( "\n" );
		out.append( "    PARAMETER_TYPES()? " ).append( Arrays.toString( PARAMETER_TYPES() ) ).append( "\n" );
		out.append( "        RETURN_TYPE()? " ).append( RETURN_TYPE().toString() ).append( "\n" );*/
		for( Method meth : clazz.getDeclaredMethods() ){
			if( name.equals( meth.getName() )                  &&
			  ((meth.getModifiers() & modifiers) == modifiers) &&
			    rt.isAssignableFrom( meth.getReturnType() )    &&
			    Arrays.equals( types, meth.getParameterTypes() ) ){ return meth; }
		}
		throw new NoSuchMethodException( Modifier.toString( modifiers ) + " " + rt.getName() + " " + STR_NAME_METHOD() + "( " + Arrays.toString( types ) + " )" );
	}

	/** find the factory method for a particular class, first using reflection, then linear search */
	static public Method methodForClass( Class<? extends JoinTreeAlgorithm> clazz ) throws NoSuchMethodException{
		try{
			return clazz.getMethod( STR_NAME_METHOD(), PARAMETER_TYPES );
		}catch( NoSuchMethodException nsme ){
			return linearSearch( clazz );
		}
	}

	/** convenience factory method */
	static public JoinTreeAlgorithm forClass( Class<? extends JoinTreeAlgorithm> clazz, BayesianNetwork bn, JT jt ) throws NoSuchMethodException{
		try{
			return (JoinTreeAlgorithm) methodForClass( clazz ).invoke( null, bn, jt );
		}catch( NoSuchMethodException nsme ){
			throw nsme;
		}catch( Throwable thrown ){
			err.println( thrown );
		}
		return null;
	}

	/** Beginning with <b>child</b>, considers the ancestry of <b>child</b>
		up to the first abstract ancestor.
		Return the shallowest non-abstract class in the
		contiguous concrete ancestry
		of <b>child</b> (including <b>child</b> itself) that
		declares a method named <b>signature</b> with parameters
		of types <b>parameterTypes</b>.  If no such class exists,
		return null.

		@since 20081103 */
	static public <T> Class<? super T> shallowestContiguousConcreteDeclarer( Class<T> child, String signature, Class<?>... parameterTypes ){
	  //out.append( "sccd( " ).append( child.getSimpleName() ).append( ", \"" ).append( signature ).append( "\" )\n" );
		if( (child.getModifiers() & Modifier.ABSTRACT) == Modifier.ABSTRACT ){ return null; }

		Method method = null;
		try{   method = child.getDeclaredMethod( signature, parameterTypes ); }catch( NoSuchMethodException thrown ){}
		if(    method != null ){ return child; }

		Class<? super T> superc = child.getSuperclass();
		if( superc != null ){ return shallowestContiguousConcreteDeclarer( superc, signature, parameterTypes ); }

		return null;
	}

	/** test/debug */
	static public void main( String[] args ){
		out.append( "Testing     " ).append( Flavor.class.getName()               ).append( "\n" );
		out.append( "values?     " ).append( Arrays.toString( Flavor.values() )   ).append( "\n" );
		out.append( "partials?   " ).append( Flavor.  partials().toString()       ).append( "\n" );
		out.append( "normalized? " ).append( Flavor.normalized().toString()       ).append( "\n" );

	  /*Method method = null;
		for( Flavor flavor : Flavor.values() ){
			method = null;
			try{ method = flavor.clazz.getDeclaredMethod( STR_NAME_METHOD_NORMALIZED() ); }catch( NoSuchMethodException thrown ){}
			out.append( flavor.toString() ).append( " declares logPrEvidence()? " )
			.append( method == null ? "no" : "yes" )
			.append( "\n" );
		}*/
	}
}
