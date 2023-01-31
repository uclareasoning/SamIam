package edu.ucla.belief.io;

import edu.ucla.belief.*;
//import edu.ucla.belief.decision.Parameter;

import java.util.*;
import java.io.*;

/** Bundle info about a CPT
	imported from a tab-delimited file.

	@author Keith Cascio
	@since 022405 */
public class CPTInfo
{
	public static final double DOUBLE_EPSILON = Double.MIN_VALUE * ((double)1000);
	public static final double DOUBLE_FILL_VALUE = (double)-1;

	public CPTInfo( FiniteVariable joint, ReadableWritableTable destination ){
		this.myJoint = joint;
		this.myDestination = destination;
		this.myIndex = joint.getCPTShell( joint.getDSLNodeType() ).index();
		this.length = myIndex.size();
		if( myDestination.getCPLength() != this.length ) throw new IllegalStateException( "lengths do not match" );
		this.myCPT = new Parameter[ this.length ];
		//Arrays.fill( myCPT, DOUBLE_FILL_VALUE );
	}

	public int index( FiniteVariable var ){
		return myIndex.variableIndex( var );
	}

	public Map getUtilMap(){
		if( myUtilMap == null ) myUtilMap = new HashMap( myIndex.getNumVariables() );
		else myUtilMap.clear();
		return this.myUtilMap;
	}

	public void setParameter( Map mapping, String token ) throws Exception {
		double value = Double.parseDouble( token );
		setParameter( 0, 0, mapping, value );
	}

	private void setParameter( int indexVar, int indexLinear, Map mapping, double value ) throws Exception {
		FiniteVariable var = myIndex.variable( indexVar );

		if( var == myJoint ){
			if( mapping.containsKey( myJoint ) ){
				int instanceJoint = ((Integer)mapping.get( myJoint )).intValue();
				setParameter( indexLinear + instanceJoint, value );
			}
			else{
				int end = indexLinear + myJoint.size();
				for( int i=indexLinear; i<end; i++ ){
					setParameter( i, value );
				}
			}
			return;
		}

		int indexVarNext = indexVar + 1;
		int blockSize = myIndex.blockSize( indexVar );
		int skip = -4096;

		if( mapping.containsKey( var ) ){
			int instanceVar = ((Integer)mapping.get( var )).intValue();
			skip = blockSize * instanceVar;
			setParameter( indexVarNext, indexLinear + skip, mapping, value );
		}
		else{
			int sizeVar = var.size();
			skip = 0;
			for( int i=0; i<sizeVar; i++ ){
				setParameter( indexVarNext, indexLinear + skip, mapping, value );
				skip += blockSize;
			}
		}
	}

	public int[] getUtilIndices(){
		if( myUtilIndices == null ) myUtilIndices = new int[ myIndex.getNumVariables() ];
		Arrays.fill( this.myUtilIndices, (int)-1 );
		return this.myUtilIndices;
	}

	public void setParameter( int[] mindex, String token ) throws Exception {
		double value = Double.parseDouble( token );
		int linearIndex = myIndex.index( mindex );
		setParameter( linearIndex, value );
	}

	private void setParameter( int indexLinear, double value ) throws Exception {
		//if( myCPT[ indexLinear ] != DOUBLE_FILL_VALUE ){
		//	if( epsilonEquals( myCPT[ indexLinear ], value, DOUBLE_EPSILON ) ) ++myCountRedundant;
		//	else ++myCountConflict;
		//}
		//myCPT[ indexLinear ] = value;
		Parameter param = myCPT[ indexLinear ];
		if( myCPT[ indexLinear ] == null ) myCPT[ indexLinear ] = param = new Parameter();
		else{
			if( epsilonEquals( param.value, value, DOUBLE_EPSILON ) ){
				param.redundant = true;
				++myCountRedundant;
			}
			else{
				param.conflicts = true;
				++myCountConflict;
			}
		}

		double existing = myDestination.getCP( indexLinear );
		if( epsilonEquals( existing, value, DOUBLE_EPSILON ) ){
			param.agrees = true;
			++myCountSpared;
		}
		else ++myCountChanged;

		param.mentioned = true;
		param.value = value;
		++myCountInfo;
	}

	public static boolean epsilonEquals( double v1, double v2, double epsilon ){
		return Math.abs( v1 - v2 ) < epsilon;
	}

	public int getNumRedundant(){
		return myCountRedundant;
	}

	public int getNumConflicts(){
		return myCountConflict;
	}

	public int countInformationParameters(){
		return myCountInfo;
	}

	public int countInformationConditions(){
		//return myCountConditionsWithInformation;
		myCountConditionsWithInformation = 0;
		int sizeJoint = myJoint.size();
		for( int segment=0; segment<myCPT.length; segment += sizeJoint ){
			myCountConditionsWithInformation += countInformationCondition( segment, segment+sizeJoint );
		}
		return myCountConditionsWithInformation;
	}

	/** @since 022805 */
	private int countInformationCondition( int begin, int end ){
		for( int i=begin; i<end; i++ ){
			if( (myCPT[i] != null) && myCPT[i].mentioned ) return 1;
		}
		return 0;
	}

	public int getNumChanged(){
		return myCountChanged;
	}

	public int getNumSpared(){
		return myCountSpared;
	}

	public void normalizeAllNew(){
		//System.out.println( "CPTInfo.normalize()" );
		int sizeJoint = myJoint.size();
		for( int i=0; i<myCPT.length; i += sizeJoint ){
			normalizeAllNew( i, sizeJoint );
		}
		myFlagNormalized = true;
	}

	private void normalizeAllNew( int begin, int length ){
		int end = begin + length;
		boolean informationPresent = false;
		int countMissing = 0;
		double sum = (double)0;
		for( int i=begin; i<end; i++ ){
			if( myCPT[i] == null ) ++countMissing;
			else{
				sum += myCPT[i].value;
				informationPresent = true;
			}
		}
		if( countMissing > 0 ){//!informationPresent ){
			double uniform = (double)-1;
			if( sum >= (double)1 ) uniform = (double)0;
			else uniform = (((double)1)-sum)/((double)countMissing);
			for( int i=begin; i<end; i++ ){
				if( myCPT[i] == null ){
					myCPT[i] = new Parameter( uniform );
					myCPT[i].agrees = epsilonEquals( uniform, myDestination.getCP(i), DOUBLE_EPSILON );
				}
			}
		}
	}

	/** @since 022805 */
	public double getDistanceMeasure( boolean superimpose ){
		return this.distanceMeasure( myDestination, superimpose );
	}

	/** @since 022805 */
	public double distanceMeasure( ReadableWritableTable destination, boolean superimpose )
	{
		if( this.length != destination.getCPLength() ) return Double.NaN;
		double max = 1.0, min = 1.0;
		double ratio = Double.NaN;
		//for( int i = 0; i < this.length; i++ ){
		int sizeJoint = myJoint.size();
		double uniform = ((double)1)/((double)sizeJoint);
		double hypothetical = Double.NaN;
		double existing = Double.NaN;
		int i, end = (int)-1;
		for( int segment=0; segment<this.length; segment += sizeJoint )
		{
			end = segment+sizeJoint;
			if( countInformationCondition( segment, end ) > 0 ) hypothetical = (double)0;
			else hypothetical = uniform;

			for( i = segment; i < end; i++ )
			{
				existing = destination.getCP(i);
				if( existing != (double)0 ){
					if( myCPT[i] == null ){
						if( superimpose ) ratio = (double)1;
						else ratio = hypothetical / existing;
					}
					else ratio = myCPT[i].value / existing;

					//System.out.println( "ratio? " + ratio + ", myCPT[i]? " + myCPT[i] + ", existing? " + existing );

					if( ratio > (double)0 ){
						if( ratio > max ) max = ratio;
						else if( ratio < min ) min = ratio;
					}
				}
			}
		}
		return Math.log( max / min );
	}

	/** @since 022805 */
	public void commit( boolean superimpose, boolean normalize ){
		//System.out.println( "CPTInfo.commit( superimpose? "+superimpose+" )" );
		if( (!superimpose) && (!myFlagNormalized) ) normalizeAllNew();

		if( normalize ) normalizeSuperimposed();

		for( int i=0; i<this.length; i++ ){
			if( myCPT[i] != null ) myDestination.setCP( i, myCPT[i].value );
		}

		myFlagCommitted = true;
	}

	/** @since 030105 */
	public void normalizeSuperimposed(){
		for( int i=0; i<this.length; i++ ){
			if( myCPT[i] == null ){
				myCPT[i] = new Parameter( myDestination.getCP(i) );
				myCPT[i].agrees = true;
			}
		}

		int sizeJoint = myJoint.size();
		for( int i=0; i<myCPT.length; i += sizeJoint ){
			normalizeSuperimposed( i, i+sizeJoint );
		}
	}

	/** @since 030105 */
	private void normalizeSuperimposed( int begin, int end ){
		double sum = sum( begin, end );
		for( int i=begin; i<end; i++ ){
			myCPT[i].value = myCPT[i].value/sum;
		}
	}

	/** @since 030105 */
	private double sum( int begin, int end ){
		double sum = (double)0;
		for( int i=begin; i<end; i++ ){
			if( myCPT[i] != null ) sum += myCPT[i].value;
		}
		return sum;
	}

	/** @since 022805 */
	public FiniteVariable getJoint(){
		return myJoint;
	}

	/** @since 030705 */
	public Parameter getParameter( int i ){
		return myCPT[i];
	}

	/** @since 030805 */
	public boolean isCommitted(){
		return myFlagCommitted;
	}

	/** @since 022805 */
	public interface ReadableWritableTable{
		public int getCPLength();
		public double getCP( int indLinear );
		public void setCP( int indLinear, double val );
	}

	/** @since 030705 */
	public static class Parameter{
		public Parameter(){
			this.value = DOUBLE_FILL_VALUE;
		}

		public Parameter( double value ){
			this.value = value;
		}

		public java.awt.Color getColor(){
			if( !this.agrees ) return java.awt.Color.red;
			else if( this.mentioned ) return java.awt.Color.blue;
			else return (java.awt.Color)null;
		}

		public double value;
		public boolean mentioned = false;
		public boolean conflicts = false;
		public boolean redundant = false;
		public boolean agrees = false;
	}

	public final int length;

	private Map myUtilMap;
	private int[] myUtilIndices;

	private FiniteVariable myJoint;
	private TableIndex myIndex;
	private Parameter[] myCPT;
	private boolean myFlagNormalized = false;
	private boolean myFlagCommitted = false;

	private ReadableWritableTable myDestination;
	private int myCountRedundant = (int)0;
	private int myCountConflict = (int)0;
	private int myCountInfo = (int)0;
	private int myCountConditionsWithInformation = (int)0;
	private int myCountSpared = (int)0;
	private int myCountChanged = (int)0;
}
