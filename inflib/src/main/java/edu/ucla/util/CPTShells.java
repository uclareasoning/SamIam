package edu.ucla.util;

import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.io.dsl.DSLNodeType;
import edu.ucla.belief.CPTShell;
import edu.ucla.belief.Definitions;

import java.util.Iterator;
import java.util.Collection;
import java.util.List;
import java.util.LinkedList;
import java.io.PrintStream;

/** @author keith cascio
	@since  20050118 */
public class CPTShells
{
	/** @since 20080220 */
	public CPTShells( String name ){
		this.name = name;
	}

	/** override this method */
	public void doTask( FiniteVariable shellsvar, DSLNodeType type, CPTShell shell ){}

	public void forAllDSLNodeTypes( FiniteVariable var ){
		this.forAllDSLNodeTypes( var, true );
	}

	/** @since 20080220 */
	private void forAllDSLNodeTypes( FiniteVariable var, boolean report_exceptions ){
		if( report_exceptions ){ clear(); }
		for( int i=0; i<NUMTYPES; i++ ){
			try{
				doTask( var, ARRAYTYPES[i], var.getCPTShell( ARRAYTYPES[i] ) );
			}catch( Throwable throwable ){
				if( thrown == null ){ thrown = new LinkedList(); }
				thrown.add( throwable );
			}
		}
		if( report_exceptions ){ report( System.err ); }
	}

	public void forAllFiniteVariables( Iterator it ){
		clear();
		while( it.hasNext() ){
			forAllDSLNodeTypes( (FiniteVariable) it.next(), false );
		}
		report( System.err );
	}

	public void forAllFiniteVariables( Collection collection ){
		forAllFiniteVariables( collection.iterator() );
	}

	/** @since 20080220 */
	public void clear(){
		if( thrown != null ){ thrown.clear(); }
	}

	/** @since 20080220 */
	public void report( PrintStream stream ){
		if( (thrown == null) || thrown.isEmpty() ){ return; }
		stream.print( "CPTShells \"" );
		stream.print( this.name );
		stream.print( "\" caught " );
		stream.print( thrown.size() );
		stream.println( " exceptions:" );

		int               i = 0;
		Throwable throwable = null;
		for( Iterator it = thrown.iterator(); it.hasNext(); ){
			stream.print( i++ );
			stream.print( ": " );
			stream.println( throwable = (Throwable) it.next() );
			if( Definitions.DEBUG ){
				throwable.printStackTrace( stream );
				stream.println();
			}
		}
	}

	private static DSLNodeType[] ARRAYTYPES = DSLNodeType.valuesAsArray();
	private static int           NUMTYPES   = ARRAYTYPES.length;

	/** @since 20080220 */
	final     public       String name;
	transient private      List   thrown;
}
