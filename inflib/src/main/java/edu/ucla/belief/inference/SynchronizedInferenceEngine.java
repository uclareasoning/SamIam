package edu.ucla.belief.inference;

import edu.ucla.belief.*;
import edu.ucla.belief.io. PropertySuperintendent;

import java.util.*;
import java.io.*;
import java.awt.Component;

/** @author keith cascio
	@since  20091226 */
public class SynchronizedInferenceEngine
  implements
    InferenceEngine,
    EvidenceChangeListener
{
	protected SynchronizedInferenceEngine( InferenceEngine simple ){
		this.simple = simple;
	}
	private InferenceEngine simple;

	static public SynchronizedInferenceEngine decorate( InferenceEngine simple ){
		return simple instanceof PartialDerivativeEngine ? new SynchronizedPartialDerivativeEngine( simple ) : new SynchronizedInferenceEngine( simple );
	}

	static public class SynchronizedPartialDerivativeEngine
	  extends SynchronizedInferenceEngine
	  implements
	    PartialDerivativeEngine,
	    InferenceEngine,
	    EvidenceChangeListener
	{
		protected SynchronizedPartialDerivativeEngine( InferenceEngine pde ){
			super(             pde );
			if(             ! (pde instanceof PartialDerivativeEngine) ){ throw new IllegalArgumentException(); }
			this.              pde =         (PartialDerivativeEngine) pde;
		}
		private PartialDerivativeEngine pde;

		public synchronized Table                partial( FiniteVariable var ){ return    pde.              partial( var ); }// begin interface PartialDerivativeEngine
		public synchronized Table          familyPartial( FiniteVariable var ){ return    pde.        familyPartial( var ); }//   end interface PartialDerivativeEngine
	}
	public synchronized void        evidenceChanged( EvidenceChangeEvent ece ){        simple.      evidenceChanged( ece ); }// begin interface EvidenceChangeListener
	public synchronized void                warning( EvidenceChangeEvent ece ){        simple.              warning( ece ); }//   end interface PartialDerivativeEngine

	public synchronized double               probability(                    ){ return simple.          probability(     ); }// begin interface InferenceEngine
	public synchronized double                       max(                    ){ return simple.                  max(     ); }
	public synchronized void                      setCPT( FiniteVariable var ){        simple.               setCPT( var ); }
	public synchronized int                       random( FiniteVariable var ){ return simple.               random( var ); }
	public synchronized  Table                     joint( FiniteVariable var ){ return simple.                joint( var ); }
	public synchronized  Table               conditional( FiniteVariable var ){ return simple.          conditional( var ); }
	public synchronized  Table               familyJoint( FiniteVariable var ){ return simple.          familyJoint( var ); }
	public synchronized  Table         familyConditional( FiniteVariable var ){ return simple.    familyConditional( var ); }
	public synchronized  Table[]            conditionals( FiniteVariable var,
	                                                      Table[]    buckets ){ return simple.conditionals( var, buckets ); }
	public synchronized String[]    describeConditionals(                    ){ return simple. describeConditionals(     ); }
	public synchronized Collection        notoriousEdges(                    ){ return simple.       notoriousEdges(     ); }
	public synchronized String  compilationStatus( PropertySuperintendent bn ){ return simple.    compilationStatus(  bn ); }
	public synchronized void        printInfoCompilation( PrintWriter    out ){        simple. printInfoCompilation( out ); }
	public synchronized void        printInfoPropagation( PrintWriter    out ){        simple. printInfoPropagation( out ); }
	public synchronized void        printTables(          PrintWriter    out ){        simple. printTables(          out ); }

	public              Dynamator           getDynamator(                    ){ return simple.         getDynamator(     ); }
	public              void                setDynamator( Dynamator      dyn ){        simple.         setDynamator( dyn ); }
	public              boolean             getValid(                        ){ return simple.         getValid(         ); }
	public              void                setValid(     boolean       flag ){        simple.         setValid(    flag ); }

	public              InferenceEngine  setControlPanel( Component      pnl ){        simple.      setControlPanel( pnl ); return this; }
	public              Component        getControlPanel(                    ){ return simple.      getControlPanel(     ); }
	public              boolean             isExhaustive(                    ){ return simple.         isExhaustive(     ); }
	public              Set                    variables(                    ){ return simple.            variables(     ); }
	public              boolean     probabilitySupported(                    ){ return simple. probabilitySupported(     ); }
	public              char        probabilityDisplayOperatorUnicode(       ){ return simple. probabilityDisplayOperatorUnicode(); }
	public              void                         die(                    ){        simple.                  die(     ); }
	public              InferenceEngine        canonical(                    ){ return simple.            canonical(     ); }
	public              int                     hashCode(                    ){ return simple.             hashCode(     ); }
	public              boolean                   equals( Object         oth ){ return simple.               equals( oth ); }

	public InferenceEngine                                      handledClone( QuantitativeDependencyHandler handler ){ return decorate( simple.                    handledClone( handler ) ); }
	public void                             setQuantitativeDependencyHandler( QuantitativeDependencyHandler handler ){                  simple.setQuantitativeDependencyHandler( handler )  ; }
	public QuantitativeDependencyHandler    getQuantitativeDependencyHandler(){ return simple.getQuantitativeDependencyHandler(); }
	public boolean                           isQuantitativeDependencyHandled(){ return simple. isQuantitativeDependencyHandled(); }//   end interface InferenceEngine

	public String toString(){ return "threadsafe{" + simple.getClass().getName() + "." + System.identityHashCode( simple ) + "}." + System.identityHashCode( this ); }
}
