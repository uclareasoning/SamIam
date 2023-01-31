package signature;

import java.lang.reflect.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

/** This class can:

	(1) help you decorate a class based on an interface,
	by generating decorating methods automatically,

	(2) help you migrate to tiger by replacing old method
	signatures with genericized signatures based on
	a compiled interface class that has already been
	genericized by hand.  The idea here is to reduce some
	of the repetitive work by letting you edit the
	method signatures only in the interface(s).

	@author keith cascio
	@since 20051111 veterans day */
public class SignatureHelper
{
	public SignatureHelper( Class clazz ){
		this.myClazz = clazz;

		this.myDeclaredMethods = clazz.getDeclaredMethods();
		Arrays.sort( myDeclaredMethods, CM );
	}

	public void addSourcePath( File sp ){
		if( (sp == null) || (!sp.exists()) || (!sp.isDirectory()) ){
			System.err.println( "Warning: failed to set source path " + sp );
			return;
		}
		if( mySourcePaths == null ) mySourcePaths = new LinkedList<File>();
		mySourcePaths.add( sp );

		//if( mySourceFile == null ) findSourceFile();
	}

	public void setSourcePaths( List<File> paths ){
		this.mySourcePaths = paths;
		//if( mySourceFile == null ) findSourceFile();
	}

	public void preprocess(){
		//if( mySourceFile == null ) return;
		//mySignatures = new FormattedSignature[ myDeclaredMethods.length ];
		if( myPreprocessor == null ) myPreprocessor = new SignaturePreprocessor( myClazz, mySourcePaths, myDeclaredMethods );
		mySignatures = myPreprocessor.getSignatures();
	}

	public void listSignatures( EnumSet<? extends SignatureFormat> formats, PrintStream out, boolean deep ){
		out.println();
		out.println();
		if( deep ){
			for( SignatureHelper helper : getSuperHelpers() ){
				helper.listSignatures( formats, out, false );
				out.println();
			}
		}

		String caption = "signatures of type " + myClazz.getName();
		if( mySignatures == null ) preprocess();
		File sourceFile = myPreprocessor.getSourceFile();
		if( sourceFile != null ) caption += " (" + sourceFile.getAbsolutePath() + ")";
		out.println( caption );
		out.println();

		if( mySignatures == null ){
			for( Method method : myDeclaredMethods ){
				out.println( SignatureFormat.applyAll( formats, method ) );
			}
		}
		else{
			for( FormattedSignature sig : mySignatures ){
				out.println( SignatureFormat.applyAll( formats, sig ) );
			}
		}
	}

	private Class[] getDeepInterfaces(){
		if( myDeepInterfaces == null ){
			Set<Class> found = findDeepInterfaces( myClazz, new HashSet<Class>() );
			myDeepInterfaces = found.toArray( new Class[ found.size() ] );
			Arrays.sort( myDeepInterfaces, CC );
		}
		return myDeepInterfaces;
	}

	private Set<Class> findDeepInterfaces( Class root, Set<Class> ret ){
		if( ret == null ) ret = new HashSet<Class>();
		for( Class clazz : root.getInterfaces() ){
			ret.add( clazz );
			findDeepInterfaces( clazz, ret );
		}
		return ret;
	}

	private SignatureHelper[] getSuperHelpers(){
		if( mySuperHelpers == null ){
			Class[] interfaces = getDeepInterfaces();
			mySuperHelpers = new SignatureHelper[ interfaces.length ];
			for( int i=0; i<mySuperHelpers.length; i++ ){
				mySuperHelpers[i] = new SignatureHelper( interfaces[i] );
				if( mySourcePaths != null ) mySuperHelpers[i].setSourcePaths( mySourcePaths );
			}
		}
		return mySuperHelpers;
	}

	public static Comparator<Method> CM = new Comparator<Method>(){
		public int compare( Method o1, Method o2 ){
			return COLLATOR.compare( o1.getName(), o2.getName() );
		}
	};

	public static Comparator<Class> CC = new Comparator<Class>(){
		public int compare( Class o1, Class o2 ){
			return COLLATOR.compare( o1.getName(), o2.getName() );
		}
	};

	public static final Collator COLLATOR = Collator.getInstance();
	public static final String STR_ARG_SOURCEPATH = "-sourcepath";
	//public static final String STR_ARG_DEFAULTSOURCEPATHS = "-defaultsourcepaths";

	public static void main( String[] args ){
		String nameClass = null;
		List<String> sourcePaths = new LinkedList<String>();
		try{
			for( int i=0; i<args.length; i++ ){
				if( args[i].equals( STR_ARG_SOURCEPATH ) ){
					if( args.length < (i+2) ) throw new IllegalStateException( STR_ARG_SOURCEPATH + " with no argument" );
					//sourcePath = args[ ++i ];
					sourcePaths.add( args[ ++i ] );
				}
				else nameClass = args[i];
			}
		}catch( Exception exception ){
			System.err.println( "Warning: SignatureHelper arg processing caught " + exception );
			exception.printStackTrace();
			return;
		}finally{
			//System.out.println( "nameClass? " + nameClass + ", sourcePaths? " + sourcePaths );
		}

		if( nameClass == null ){
			System.err.println( "usage: " + SignatureHelper.class.getName() + " <class name> "+STR_ARG_SOURCEPATH+" <source path>" );
			return;
		}

		SignatureHelper helper;
		try{
			Class clazz = Class.forName( nameClass );
			helper = new SignatureHelper( clazz );
			//if( sourcePath != null ) helper.addSourcePath( new File( sourcePath ) );
			for( String sourcePath : sourcePaths ){
				helper.addSourcePath( new File( sourcePath ) );
			}

			EnumSet<SignatureFormat> formats =
				EnumSet.of( SignatureFormat.ARGLIST,
							//SignatureFormat.DECORATE,
							SignatureFormat.STRIP_ABSTRACT,
							SignatureFormat.STRIP_NAME_PREFIX );

			helper.listSignatures( formats, System.out, /*deep*/true );
		}catch( Exception exception ){
			System.err.println( "Warning: SignatureHelper.main() caught " + exception );
			exception.printStackTrace();
			return;
		}
	}

	private Class myClazz;
	private Method[] myDeclaredMethods;
	private FormattedSignature[] mySignatures;
	private Class[] myDeepInterfaces;
	private SignatureHelper[] mySuperHelpers;

	//private File mySourceFile;
	private List<File> mySourcePaths;
	private SignaturePreprocessor myPreprocessor;
}
