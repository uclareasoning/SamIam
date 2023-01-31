package signature;

//import java.lang.reflect.*;
import java.io.*;
import java.text.*;
import java.util.*;
//import java.util.regex.*;

import com.sun.javadoc.*;

/** @author keith cascio
	@since 20051114 */
public class SignatureDoclet
{
	public static final String STR_ARG_ANCESTOR   = "-anc";
	public static final String STR_ARG_DESCENDANT = "-desc";
	public static final String STR_ARG_DEEP       = "-deep";
	public static final String STR_ARG_DECORATE   = "-decorate";

	/** doclet standard method */
	public static LanguageVersion languageVersion(){
		return LanguageVersion.JAVA_1_5;
	}

	/** doclet standard method */
	public static boolean start( RootDoc root ){
		try{
			//SignatureDoclet sd =
			new SignatureDoclet( root ).execute();//listSignatures( System.out );
			return true;
		}catch( Exception exception ){
			System.err.println( "Warning: SignatureDoclet.start() caught " + exception );
			exception.printStackTrace();
			return false;
		}
	}

	public SignatureDoclet( RootDoc root ){
		SignatureDoclet.this.myRootDoc = root;
		SignatureDoclet.this.readOptions( root.options() );
	}

	private void execute(){
		if( myDocDescendant == null ) listSignatures();
		else transform();
	}

	private void transform(){
		MigrationTarget mt = new MigrationTarget( myDocDescendant );
		mt.setTargetSignatures( getSignatures( myFlagDeep, new ArrayList<FormattedSignature>() ) );
		mt.correlate();
	}

	private void listSignatures(){
		SignatureDoclet.this.listSignatures( System.out );
	}

	private void listSignatures( PrintStream out ){
		SignatureDoclet.this.listSignatures( out, myFlagDeep );
	}

	private void listSignatures( PrintStream out, boolean deep ){
		if( deep ){
			for( ClassDoc classdoc : getDeepInterfaces() ){
				listSignatures( out, classdoc );
			}
		}
		listSignatures( out, myDocAncestor );
	}

	private void listSignatures( PrintStream out, ClassDoc classdoc ){
		out.println();
		out.println();

		StringBuilder caption = new StringBuilder( 64 );
		caption.append( "signatures for type " );
		caption.append( classdoc.toString() );//typeToString( classdoc, caption );
		File sourceFile = classdoc.position().file();
		if( sourceFile != null ){
			caption.append( " (" );
			caption.append( sourceFile.getAbsolutePath() );
			caption.append( ")" );
		}
		out.println( caption );
		out.println();

		for( MethodDoc methoddoc : classdoc.methods() ){
			//out.println( signature( methoddoc ) );
			out.println( SignatureFormat.applyAll( myFormats, methoddoc ) );
		}
	}

	public List<FormattedSignature> getSignatures( boolean deep, List<FormattedSignature> ret ){
		if( ret == null ) ret = new ArrayList<FormattedSignature>();

		if( deep ){
			for( ClassDoc classdoc : getDeepInterfaces() ){
				getSignatures( classdoc, ret );
			}
		}
		return getSignatures( myDocAncestor, ret );
	}

	public List<FormattedSignature> getSignatures( ClassDoc classdoc, List<FormattedSignature> ret ){
		if( ret == null ) ret = new ArrayList<FormattedSignature>();

		FormattedSignature sig;
		for( MethodDoc methoddoc : classdoc.methods() ){
			SignatureFormat.applyAll( myFormats, sig = new FormattedSignature( methoddoc ) );
			ret.add( sig );
		}
		return ret;
	}

	public void setFormats( EnumSet<SignatureFormat> formats ){
		this.myFormats = formats;
	}

	public static String signature( MethodDoc methoddoc ){
		//return methoddoc.toString();
		StringBuilder sb = new StringBuilder( 128 );
		sb.append( methoddoc.modifiers() );
		sb.append( " " );

		TypeVariable[] tParams = methoddoc.typeParameters();
		if( (tParams != null) && (tParams.length > 0) ){
			sb.append( "<" );
			for( TypeVariable tVar : tParams ){
				typeToString( tVar, sb );
				sb.append( "," );
			}
			sb.setLength( sb.length() - 1 );
			sb.append( "> " );
		}

		typeToString( methoddoc.returnType(), sb );
		sb.append( " " );
		sb.append( methoddoc.name() );
		sb.append( "(" );

		Parameter[] params = methoddoc.parameters();
		if( (params != null) && (params.length > 0) ){
			sb.append( " " );
			for( Parameter param : params ){
				typeToString( param.type(), sb );
				sb.append( " " );
				sb.append( param.name() );
				sb.append( ", " );
			}
			sb.setLength( sb.length() - 2 );
			sb.append( " " );
		}
		sb.append( ")" );
		Type[] eTypes = methoddoc.thrownExceptionTypes();
		if( (eTypes != null) && (eTypes.length > 0) ){
			sb.append( " throws " );
			for( Type eType : eTypes ){
				typeToString( eType, sb );
				sb.append( ", " );
			}
			sb.setLength( sb.length() - 2 );
		}

		//if( methoddoc.overriddenMethod() != null ){
		//	sb.append( " OVERRIDES" );
		//}

		return sb.toString();
	}

	public static StringBuilder typeToString( Type type, StringBuilder sb ){
		if( sb == null ) sb = new StringBuilder( 64 );
		sb.append( type.simpleTypeName() );//qualifiedTypeName() );

		ParameterizedType pt = type.asParameterizedType();
		if( pt != null ){
			//sb.append( "< pt[" );
			//sb.append( pt.typeArguments().length );
			//sb.append( "] >" );
			sb.append( "<" );
			Type[] tArgs = pt.typeArguments();
			if( (tArgs != null) && (tArgs.length > 0) ){
				for( Type tArg : tArgs ){
					typeToString( tArg, sb );
					sb.append( ", " );
				}
				sb.setLength( sb.length() - 2 );
			}
			sb.append( ">" );
		}

		TypeVariable tv = type.asTypeVariable();
		if( tv != null ){
			//sb.append( "tv" );
			Type[] bounds = tv.bounds();
			/*if( (bounds != null) && (bounds.length > 0) ){
				for( Type tBound :  ){
					sb.append( " extends " );
					typeToString( tBound, sb );
					sb.append( " & " );
				}
				sb.setLength( sb.length() - 3 );
			}*/
		}

		WildcardType wt = type.asWildcardType();
		if( wt != null ){
			//sb.append( "<? " );
			Type[] boundsExtends = wt.extendsBounds();
			if( (boundsExtends != null) && (boundsExtends.length > 0) ){
				sb.append( " extends " );
				for( Type tExtendsBound : boundsExtends ){
					typeToString( tExtendsBound, sb );
					sb.append( " & " );
				}
				sb.setLength( sb.length() - 3 );
			}
			Type[] boundsSuper = wt.superBounds();
			if( (boundsSuper != null) && (boundsSuper.length > 0) ){
				sb.append( " super " );
				for( Type tSuperBound : boundsSuper ){
					typeToString( tSuperBound, sb );
					sb.append( " & " );
				}
				sb.setLength( sb.length() - 3 );
			}
			//sb.append( ">" );
		}

		sb.append( type.dimension() );
		return sb;//.toString();
	}

	public static Set<String> captureQualNames( Type type, Set<String> ret ){
		if( ret == null ) ret = new HashSet<String>( 2 );

		Type[] extendsBounds = null;
		TypeVariable tv = type.asTypeVariable();
		if( tv != null ){
			extendsBounds = tv.bounds();
		}

		WildcardType wt = type.asWildcardType();
		if( wt != null ){
			extendsBounds = wt.extendsBounds();
		}

		if( extendsBounds != null ){
			if( extendsBounds.length == 0 ) ret.add( Object.class.getName() );
			for( Type capture : extendsBounds ){
				captureQualNames( capture, ret );
			}
		}
		else ret.add( type.qualifiedTypeName() );

		//ParameterizedType pt = type.asParameterizedType();
		//if( pt != null ) return ret.add( simpleTypeName() );

		return ret;
	}

	private ClassDoc[] getDeepInterfaces(){
		if( myDeepInterfacesAncestor == null ){
			Set<ClassDoc> list = findDeepInterfaces( myDocAncestor, new HashSet<ClassDoc>() );
			myDeepInterfacesAncestor = list.toArray( new ClassDoc[ list.size() ] );
			Arrays.sort( myDeepInterfacesAncestor, CC );
		}
		return myDeepInterfacesAncestor;
	}

	public static Comparator<ClassDoc> CC = new Comparator<ClassDoc>(){
		public int compare( ClassDoc o1, ClassDoc o2 ){
			return COLLATOR.compare( o1.name(), o2.name() );
		}
	};
	public static final Collator COLLATOR = Collator.getInstance();

	public Set<ClassDoc> findDeepInterfaces( ClassDoc child, Set<ClassDoc> ret ){
		//System.out.println( "SignatureDoclet.findDeepInterfaces( "+child.name()+" )" );
		if( ret == null ) ret = new HashSet<ClassDoc>();
		for( ClassDoc parent : child.interfaces() ){
			ret.add( parent );
			findDeepInterfaces( parent, ret );
		}
		return ret;
	}

	private void readOptions( String[][] options ){
		for( String[] opt : options ){
			if( opt[0].equals( STR_ARG_ANCESTOR ) ){
				String myNameAncestor = opt[1];
				if( (myDocAncestor = myRootDoc.classNamed( myNameAncestor )) == null )
					throw new IllegalArgumentException( "source file for ancestor class \"" +myNameAncestor+ "\" not found" );
			}
			else if( opt[0].equals( STR_ARG_DESCENDANT ) ){
				String myNameDescendant = opt[1];
				if( (myDocDescendant = myRootDoc.classNamed( myNameDescendant )) == null )
					throw new IllegalArgumentException( "source file for descendant class \"" +myNameDescendant+ "\" not found" );
			}
			else if( opt[0].equals( STR_ARG_DEEP ) ){
				myFlagDeep = true;
			}
			else if( opt[0].equals( STR_ARG_DECORATE ) ){
				myFormats.add( SignatureFormat.DECORATE );
			}
		}
	}

	/** doclet standard method */
	public static int optionLength( String option ){
		if( option.equals( STR_ARG_ANCESTOR ) ){
			return 2;
		}
		else if( option.equals( STR_ARG_DESCENDANT ) ){
			return 2;
		}
		else if( option.equals( STR_ARG_DEEP ) ){
			return 1;
		}
		else if( option.equals( STR_ARG_DECORATE ) ){
			return 1;
		}
		return 0;
	}

	/** doclet standard method */
	public static boolean validOptions( String options[][], DocErrorReporter reporter ){
		boolean foundAncestorOption = false;
		for( String[] opt : options ){
			if( opt[0].equals( STR_ARG_ANCESTOR ) ){
				if( opt[1].length() > 0 ){
					if( foundAncestorOption ){
						reporter.printError("Only one "+STR_ARG_ANCESTOR+" option allowed.");
						return false;
					}
					foundAncestorOption = true;
				}
			}
		}

		if( !foundAncestorOption ){
			reporter.printError( "Usage: javadoc -source 1.5 -sourcepath <sourcepath> [ packagenames ] ... -docletpath <docletpath> -doclet "+SignatureDoclet.class.getName()+" "+STR_ARG_ANCESTOR+" <ansector interface> ["+STR_ARG_DEEP+"] ["+STR_ARG_DECORATE+"] ["+STR_ARG_DESCENDANT+" <descendant class>]" );
		}

		return foundAncestorOption;
	}

	private RootDoc myRootDoc;
	//private String myNameAncestor, myNameDescendant;
	private ClassDoc myDocAncestor, myDocDescendant;
	private ClassDoc[] myDeepInterfacesAncestor;
	private boolean myFlagDeep;
	private EnumSet<SignatureFormat> myFormats = EnumSet.noneOf( SignatureFormat.class );
}
