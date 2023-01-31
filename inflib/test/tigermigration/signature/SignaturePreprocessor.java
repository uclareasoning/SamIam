package signature;

import java.lang.reflect.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

/** @author keith cascio
	@since 20051111 veterans day */
public class SignaturePreprocessor
{
	public SignaturePreprocessor( Class clazz, List<File> paths, Method[] declared ){
		this.myClazz = clazz;
		//this.mySourceFile = sourceFile;
		this.mySourcePaths = paths;
		this.myDeclaredMethods = declared;
		init();
	}

	public FormattedSignature[] getSignatures(){
		return mySignatures;
	}

	private void init(){
		try{
			findSourceFile();
			if( this.mySourceFile == null ) return;

			this.mySignatures = new FormattedSignature[ myDeclaredMethods.length ];
			for( int i=0; i<mySignatures.length; i++ ){
				mySignatures[i] = new FormattedSignature( myDeclaredMethods[i] );
			}

			BufferedReader reader = new BufferedReader( new FileReader( mySourceFile ) );
			String line = null;
			while( (line = reader.readLine()) != null ){
				SignaturePreprocessor.this.process( line );
			}

			for( FormattedSignature sig : mySignatures ){
				if( sig.getSourceLine() == null ){
					System.err.println( "Warning: failed to find method " + sig.getBrief() + " in source file \"" +mySourceFile.getAbsolutePath()+ "\"" );
					System.err.println( "    " + sig.getPatternStrict() );
				}
			}
		}catch( Exception exception ){
			System.err.println( "Warning: SignaturePreprocessor.init() caught " + exception );
			exception.printStackTrace();
		}
	}

	private void process( String line ){
		for( FormattedSignature sig : mySignatures ){
			sig.lookAtSource( line );
		}
	}

	public static final String REGEX_CLASSNAME = "((\\w+\\.)*)(\\w+)";
	public static final Pattern PATTERN_CLASSNAME = Pattern.compile( REGEX_CLASSNAME, Pattern.CASE_INSENSITIVE );

	public void findSourceFile(){
		try{
			Matcher m = PATTERN_CLASSNAME.matcher( myClazz.getName() );
			if( !m.matches() ){
				System.err.println( "Warning: SignatureHelper.addSourcePath() could not interpret class name \"" + myClazz.getName() + "\"" );
				return;
			}

			String prefix = m.group(1);
			String name = m.group(3);

			String relativePath = prefix.replace( '.', File.separatorChar ) + name + ".java";

			for( File sp : mySourcePaths ){
				String absolutePath = sp.getCanonicalFile().getAbsolutePath() + File.separator + relativePath;

				File guess = new File( absolutePath );
				if( guess.exists() && guess.isFile() ){
					mySourceFile = guess;
					break;
				}
				else mySourceFile = null;
			}

			//if( mySourceFile != null ) preprocess();
		}catch( Exception exception ){
			System.err.println( "Warning: SignatureHelper.findSourceFile() caught " + exception );
		}
	}

	public File getSourceFile(){
		return mySourceFile;
	}

	private Class myClazz;
	private Method[] myDeclaredMethods;
	private FormattedSignature[] mySignatures;
	private File mySourceFile;
	private List<File> mySourcePaths;
}
