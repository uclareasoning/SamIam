package edu.ucla.belief.io.dsl;

//import java.util.List;
//import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import java.util.HashSet;

/**
	@author Keith Cascio
	@since 041902
*/
public class DSLSubmodelFactory
{
	private Collection mySubModels = new HashSet();
	public static final int HANDLE_MAIN = (int)0;
	public DSLSubmodel MAIN = null;

	public DSLSubmodelFactory()
	{
		MAIN = new DSLSubmodel( HANDLE_MAIN );
		MAIN.setName( "Root Submodel" );
		mySubModels.add( MAIN );
		//System.out.println( "DSLSubmodelFactory()" );//debug
	}

	public void setMain( DSLSubmodel newMain )
	{
		MAIN = newMain;
	}

	public Iterator getAllSubmodels()
	{
		return mySubModels.iterator();
	}

	public int getNumSubmodels()
	{
		return mySubModels.size();
	}

	public DSLSubmodel forHandle( int handle )
	{
		DSLSubmodel ret = null;
		for( Iterator it = mySubModels.iterator(); it.hasNext(); )
		{
			ret = ((DSLSubmodel)it.next());
			if( ret.getHandle() == handle ) return ret;
		}

		ret = new DSLSubmodel( handle );
		mySubModels.add( ret );

		//System.out.println( "DSLSubmodelFactory constructed new submodel for handle: " + handle );//debug

		return ret;
	}
}
