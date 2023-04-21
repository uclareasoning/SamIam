package edu.ucla.util;

import java.text.NumberFormat;
import java.util.*;
import java.io.IOException;

/** Based on Chrisanna Waldrop, Copyright Officer, 805-893-7773, waldrop@research.ucsb.edu,
      package edu.ucsb.nmsl.tools, url http://www.nmsl.cs.ucsb.edu/autocap
    @author keith cascio
    @since 20060217 */
public class DataSetStatistic
{
  /**
   * Empty data set.
   *
   * @param name Descriptive name for data.
   * @param inititalSize Allocate initial size ArrayList.
   */
  public DataSetStatistic( String name, int inititalSize ){
    myName = name;
    myData = new ArrayList<Double>( inititalSize );
  }

  /**
   * This constructor creates a DataSetStatistic with a given name and with data
   * from the specified collection.
   *
   * @param name Descriptive name for data.
   * @param c A collection of data that becomes the data points in the data set.
   */
  public <T extends Number> DataSetStatistic( String name, Collection<T> c ){
    DataSetStatistic.this.myName = name;
    //DataSetStatistic.this.myData = new ArrayList<Double>( c );
    DataSetStatistic.this.myData = new ArrayList<Double>( c.size() );
    for( T element : c ){
      DataSetStatistic.this.addDataPoint( element.doubleValue() );
    }
  }

  public void addDataPoint( double x )
  {
    myData.add( x );
    mySum += x;
    myVariance += x * x;

    if( myData.size() == 1 ) myMin = myMax = x;
    else DataSetStatistic.this.considerMinMax( x );

    DataSetStatistic.this.myFlagSorted = false;
  }

  public boolean removeDataPoint( double x )
  {
    boolean ret = myData.remove( x );

    if( ret ){
      mySum -= x;
      myVariance -= x * x;
      if( (x == myMin) || (x == myMax) ) myFlagIntervalValid = false;
    }

    return ret;
  }

  /**
   * This method calculates are returns the standard deviation of all the data
   * in the set. The calculation is sped up by keeping a running sum and
   * variance as data points are added and subtracted from the set.
   *
   * @return The standard deviation of all the data points in the set.
   */
  public double getStdDev(){
    if( myData.isEmpty() ) return 0.0;

    double size = (double) myData.size();
    return Math.sqrt((myVariance - mySum * mySum / size) / size);
  }

  /**
   * This method returns the mean of all data points in the set. The calculation
   * is sped up by using a running sum of all the data points as they are added
   * to and subracted from the set.
   *
   * @return The mean of all data points in the set.
   */
  public double getMean(){
    if( myData.isEmpty() ) return 0.0;

    return mySum / myData.size();
  }

  /**
   * This method calculates and returns the median value of the data points in
   * the set. This is the straight forward linear implementation of the median
   * algorithm. Perhaps in the future a faster median algorithm can be used.
   *
   * @return The median value of the data points in the set.
   */
  public double getMedian()
  {
    if( myData.isEmpty() ) return 0.0;

    int size = myData.size();
    if( size < 2 ) return myData.get(0);

    sort();
    double median = -1;

    if(size % 2 == 0)
    {
      median =
        (
          myData.get( size / 2 - 1 ) +
          myData.get( size / 2 )
        ) / 2;
    }
    else
    {
      median = myData.get( size / 2 - 1 );
    }

    return median;
  }

  public double getMode()
  {
    if( myData.isEmpty() ) return 0.0;

    sort();

    double mode = -1, last = -1;
    int freqMax = 0, freqCurr = 0, freqZero = 0;

    for( double curr : myData ){
      if( curr == last ) ++freqCurr;
      else freqCurr = 1;

      if( freqCurr > freqMax ){
        freqMax = freqCurr;
        mode = curr;
      }

      last = curr;
      if( curr == 0.0 ) ++freqZero;
    }

    DataSetStatistic.this.myModeFrequency = freqMax;
    DataSetStatistic.this.myZeroFrequency = freqZero;
    return mode;
  }

  public int getModeFrequency(){
    return DataSetStatistic.this.myModeFrequency;
  }

  public int getZeroFrequency(){
    return DataSetStatistic.this.myZeroFrequency;
  }

  public double getMin(){
    if( !myFlagIntervalValid ) computeMinMax();
    return DataSetStatistic.this.myMin;
  }

  public double getMax(){
    if( !myFlagIntervalValid ) computeMinMax();
    return DataSetStatistic.this.myMax;
  }

  private void computeMinMax(){
  	if( myData.isEmpty() ){
  		DataSetStatistic.this.myMin = DataSetStatistic.this.myMax = 0;
  		return;
  	}

    DataSetStatistic.this.myMin = DataSetStatistic.this.myMax = myData.get(0);
    for( double point : myData ){
      considerMinMax( point );
    }
  }

  private void considerMinMax( double point ){
    myMin = Math.min( myMin, point );
    myMax = Math.max( myMax, point );
  }

  public void sort(){
    if( myFlagSorted ) return;

    Collections.sort( myData );
    myFlagSorted = true;
  }

  public Appendable report( Appendable buff ){
    return report( buff, (NumberFormat)null );
  }

  /** @since 20060313 */
  public Appendable report( Appendable buff, NumberFormat format ){
  	try{
		buff.append( myName );
		buff.append( ": " );
		buff.append( Integer.toString( myData.size() ) );
		buff.append( " values in [" );
		buff.append( format( getMin(), format ) );
		buff.append( ", " );
		buff.append( format( getMax(), format ) );
		buff.append( "], mean " );
		buff.append( format( getMean(), format ) );
		buff.append( ", median " );
		buff.append( format( getMedian(), format ) );
		buff.append( ", mode " );
		buff.append( format( getMode(), format ) );
		buff.append( " x" );
		reportFrequency( buff, getModeFrequency() );
		buff.append( ", " );
		reportFrequency( buff, getZeroFrequency() );
		buff.append( " zeros, std dev " );
		buff.append( format( getStdDev(), format ) );
	}catch( IOException ioexception ){
		System.err.println( "warning: DataSetStatistic.report() caught " + ioexception );
	}
    return buff;
  }

  private Appendable reportFrequency( Appendable buff, int freq ) throws IOException{
    buff.append( Integer.toString( freq ) );
    buff.append( " (" );
    buff.append( Double.toString( ( freq / ((double)myData.size())) * ((double)100) ) );
    buff.append( "%)" );
    return buff;
  }

  public static String format( double value, NumberFormat format ){
    return (format == null) ? Double.toString( value ) : format.format( value );
  }

	/** @since 20060312 */
	public void clear(){
		myData.clear();
		mySum = myVariance = myMin = myMax = 0;
		myFlagSorted = false;
		myFlagIntervalValid = true;
		myModeFrequency = myZeroFrequency = -1;
	}

  private ArrayList<Double> myData;
  private double mySum = 0, myVariance = 0, myMin = 0, myMax = 0;

  private String myName;
  private boolean myFlagSorted = false, myFlagIntervalValid = true;
  private int myModeFrequency = -1, myZeroFrequency = -1;
}
