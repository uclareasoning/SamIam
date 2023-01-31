package signature;

//import java.lang.reflect.*;
import java.io.*;
import java.text.*;
import java.util.*;
//import java.util.regex.*;

import com.sun.javadoc.*;

/** @author keith cascio
	@since 20051114 */
public class MigrationTarget
{
	public MigrationTarget( ClassDoc classdoc ){
		this.myClassDoc = classdoc;
	}

	public void setTargetSignatures( List<FormattedSignature> targets ){
		myTargetSignatures = targets;
	}

	public void correlate(){
		try{
			//BufferedReader reader = new BufferedReader( new FileReader( myClassDoc.position().file() ) );
			//String line = null;
			//while( (line = reader.readLine()) != null ){}

			FormattedSignature inspection;
			for( MethodDoc methoddoc : myClassDoc.methods() ){
				inspection = new FormattedSignature( methoddoc );
				correlate( inspection );
			}

			LinkedList<FormattedSignature> failed = new LinkedList<FormattedSignature>();
			int count = 0;
			for( FormattedSignature target : myTargetSignatures ){
				if( target.getCorrelate() != null ) ++count;
				else failed.add( target );
			}

			System.out.println( count + "/" + myTargetSignatures.size() + " signatures correlated." );

			System.out.println( "Failed:" );
			for( FormattedSignature failedTarget : failed ){
				System.out.println( failedTarget.getSignature() + " " + failedTarget.getMethodDoc().containingClass() );
			}

		}catch( Exception exception ){
			System.err.println( "Warning: MigrationTarget.correlate() caught " + exception );
			exception.printStackTrace();
		}
	}

	private FormattedSignature correlate( FormattedSignature inspection ){
		System.out.println();
		System.out.println( "*******************************************" );
		System.out.println( "MigrationTarget.correlate()" );
		System.out.println( "  inspect: " + inspection.getSignature() );

		String inspectionName = inspection.getName();
		int inspectionNumParams = inspection.getNumParameters();
		Parameter[] inspectionParams = inspection.getMethodDoc().parameters();

		String[][] targetCaptures;
		String[] capture;
		for( FormattedSignature target : myTargetSignatures ){
			if( target.getName().equals( inspectionName ) && (target.getNumParameters() == inspectionNumParams) ){
				targetCaptures = target.getParametersQualNameCaptures();
				if( (capture = match( inspectionParams, target.getMethodDoc().parameters(), targetCaptures )) != null ){
					target.setCorrelate( inspection );
					System.out.println( "  match:   " + target.getSignature()+ " " + target.getMethodDoc().containingClass() );
					System.out.println( "  capture: " + Arrays.toString( capture ) );
					return target;
				}
			}
		}

		System.out.println( "  no match" );
		return null;
	}

	public static String[] match( Parameter[] inspectionParams, Parameter[] targetParams, String[][] targetCaptures ){
		if( (inspectionParams == null) || (targetCaptures == null) ) return null;
		if( inspectionParams.length != targetCaptures.length ) return null;

		String[] ret = new String[inspectionParams.length];
		for( int i=0; i<inspectionParams.length; i++ ){
			if( (ret[i] = match( inspectionParams[i], targetParams[i], targetCaptures[i] )) == null ){
				return null;
			}
		}
		return ret;
	}

	public static String match( Parameter inspectionParam, Parameter targetParam, String[] targetCaptures ){
		Type inspectionType = inspectionParam.type();
		Type targetType = targetParam.type();

		String ret = null;
		try{
			if( !inspectionType.dimension().equals( targetType.dimension() ) ) return ret = null;

			if( inspectionType.equals( targetType ) ) return ret = targetType.qualifiedTypeName();

			SET_QUAL_NAMES_INSPECTION.clear();
			SET_QUAL_NAMES_INSPECTION = SignatureDoclet.captureQualNames( inspectionType, SET_QUAL_NAMES_INSPECTION );
			//SET_QUAL_NAMES_TARGET     = SignatureDoclet.captureQualNames( targetType,     SET_QUAL_NAMES_TARGET );

			for( String typeName : targetCaptures ){
				if( SET_QUAL_NAMES_INSPECTION.contains( typeName ) ) return ret = typeName;
			}
		}finally{
			if( ret == null ){
				System.out.println( "  abort:   " );//+ Arrays.toString( ret ) );
				System.out.println( "    targt: " + targetParam );
				System.out.println( "    tcapt: " + Arrays.toString( targetCaptures ) );
				System.out.println( "    inspt: " + inspectionParam );
				System.out.println( "    icapt: " + SET_QUAL_NAMES_INSPECTION );
			}
		}

		return ret;
	}

	private ClassDoc myClassDoc;
	private List<FormattedSignature> myTargetSignatures;

	private static Set<String> SET_QUAL_NAMES_INSPECTION = new HashSet<String>(2);
	//private Set<String> SET_QUAL_NAMES_TARGET     = new HashSet<String>(2);
}
