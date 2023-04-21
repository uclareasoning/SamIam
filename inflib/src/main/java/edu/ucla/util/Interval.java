package edu.ucla.util;

import java.text.*;

/** Class for interval. */
public class Interval {
	protected double lowerBound;
	protected double upperBound;
	protected boolean point;

	public Interval() {
		lowerBound = getAbsoluteLowerBound();
		upperBound = getAbsoluteUpperBound();
		point = false;
	}

	public Interval(double value) {
		if (value < getAbsoluteLowerBound() || value >
			getAbsoluteUpperBound()) {
			this.lowerBound = Double.POSITIVE_INFINITY;
			this.upperBound = Double.NEGATIVE_INFINITY;
			point = false;
		}
		else {
			lowerBound = value;
			upperBound = value;
			point = true;
		}
	}

	public Interval(double lowerBound, double upperBound) {
		if (lowerBound > upperBound) {
			this.lowerBound = Double.POSITIVE_INFINITY;
			this.upperBound = Double.NEGATIVE_INFINITY;
			point = false;
		}
		else {
			if (lowerBound < getAbsoluteLowerBound())
				this.lowerBound = getAbsoluteLowerBound();
			else
				this.lowerBound = lowerBound;
			if (upperBound > getAbsoluteUpperBound())
				this.upperBound = getAbsoluteUpperBound();
			else
				this.upperBound = upperBound;
			point = false;
		}
	}

	public Interval(Interval interval, double offset) {
		lowerBound = interval.getLowerBound() + offset;
		upperBound = interval.getUpperBound() + offset;
		point = interval.isPoint();
		if (lowerBound < getAbsoluteLowerBound())
			lowerBound = getAbsoluteLowerBound();
		if (upperBound > getAbsoluteUpperBound())
			upperBound = getAbsoluteUpperBound();
	}

	/**
		@author Keith Cascio
		@since 082102
	*/
	public int compareLowerBound( Object obj )
	{
		double otherLowerBound = ((Interval) obj).lowerBound;
		if( lowerBound < otherLowerBound ) return (int)-1;
		else if( lowerBound == otherLowerBound ) return (int)0;
		else return (int)1;
	}

	public int compareUpperBound( Object obj )
	{
		double otherUpperBound = ((Interval) obj).upperBound;
		if( upperBound < otherUpperBound ) return (int)-1;
		else if( upperBound == otherUpperBound ) return (int)0;
		else return (int)1;
	}

	public String toString() {
		if (isPoint())
			return new Double(lowerBound).toString();
		else if (isEmpty())
			return "";
		else if (lowerBound == getAbsoluteLowerBound())
			return "<= " + upperBound;
		else if (upperBound == getAbsoluteUpperBound())
			return ">= " + lowerBound;
		else
			return "[" + lowerBound + ", " + upperBound + "]";
	}

	/** @since 20021003 */
	public String toString( java.text.NumberFormat format )
	{
		return append( new StringBuffer( 32 ), format ).toString();
	}

	/** @since 20060209 */
	public StringBuffer append( StringBuffer buff, java.text.NumberFormat format ){
		if( isEmpty() ) return buff;
		else if( isPoint() )
			buff.append( format.format( lowerBound ) );
		else if( lowerBound == getAbsoluteLowerBound() ){
			buff.append( "<= " );
			buff.append( format.format( upperBound ) );
		}
		else if( upperBound == getAbsoluteUpperBound() ){
			buff.append( ">= " );
			buff.append( format.format( lowerBound ) );
		}
		else{
			buff.append( "[" );
			buff.append( format.format( lowerBound ) );
			buff.append( ", " );
			buff.append( format.format( upperBound ) );
			buff.append( "]" );
		}
		return buff;
	}

	public boolean isEmpty() {
		return lowerBound > upperBound;
	}

	public boolean isPoint() {
		return point && !isEmpty();
	}

	public double getLowerBound() {
		return lowerBound;
	}

	public double getUpperBound() {
		return upperBound;
	}

	public double getAbsoluteLowerBound() {
		return Double.NEGATIVE_INFINITY;
	}

	public double getAbsoluteUpperBound() {
		return Double.POSITIVE_INFINITY;
	}

	public void intersect(Interval interval) {
		lowerBound = java.lang.Math.max(lowerBound,
			interval.lowerBound);
		upperBound = java.lang.Math.min(upperBound,
			interval.upperBound);
		if (lowerBound > upperBound) {
			lowerBound = Double.POSITIVE_INFINITY;
			upperBound = Double.NEGATIVE_INFINITY;
		}
	}

	public boolean contains(double value) {
		return value >= lowerBound && value <= upperBound;
	}

	/** @since 20060208 */
	public boolean epsilonContains( double value, double epsilon ){
		boolean satLower = (value + epsilon) >= lowerBound;
		boolean satUpper = (value - epsilon) <= upperBound;
		return satLower && satUpper;
	}

	public double closestToZero() {
		if (isEmpty())
			return Double.NaN;
		else if (lowerBound > 0.0)
			return lowerBound;
		else if (upperBound < 0.0)
			return upperBound;
		else
			return 0.0;
	}

	public boolean allNegative() {
		return (upperBound < 0);
	}

	public boolean allPositive() {
		return (lowerBound > 0);
	}
}
