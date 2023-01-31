package il2.inf.structure;

import java.math.BigInteger;

/**
	il2.inf.structure.JoinTreeStats
	@author Keith Cascio
	@since 072803
*/
public abstract class JoinTreeStats
{
	/** @since 020305 */
	public interface StatsSource{
		/** @since 110502 */
		public Stat getClusterStats();
		/** @since 111302 */
		public Stat getSeparatorStats();
	}

	/** @since 020305 */
	public interface Stat{
		public BigInteger getMax();
		public double getNormalizedMax();
		public BigInteger getTotal();
	}

	/** @since 020305 */
	public static class StatImpl implements Stat
	{
		public StatImpl( BigInteger max, double normalized, BigInteger total ){
			this.myMax = max;
			this.myNormalizedMax = normalized;
			this.myTotal = total;
		}
		public BigInteger getMax(){
			return myMax;
		}
		public double getNormalizedMax(){
			return myNormalizedMax;
		}
		public BigInteger getTotal(){
			return myTotal;
		}
		public void combine( Stat other ){
			myMax = myMax.max( other.getMax() );
			myNormalizedMax = Math.max( myNormalizedMax, other.getNormalizedMax() );
			myTotal = myTotal.add( other.getTotal() );
		}

		private BigInteger myMax;
		private double myNormalizedMax;
		private BigInteger myTotal;
	}

	/** @since 020405 */
	public static final String toStringGrouped( BigInteger biggy ){
		return toStringGrouped( biggy, ',', (int)3, (int)10 );
	}

	/** @since 020405 */
	public static final String toStringGrouped( BigInteger biggy, char separator, int groupsize, int radix ){
		String ungrouped = biggy.toString( radix );
		int length = ungrouped.length();
		StringBuffer buff = new StringBuffer( length * 2 );
		for( int i=length-1; i>=0; ){
			for( int j=0; (j<groupsize) && (i>=0); j++ ){
				buff.append( ungrouped.charAt( i-- ) );
			}
			if( i>=0 ) buff.append( separator );
		}
		return buff.reverse().toString();
	}

	/** @since 120203 */
	public static final double logBaseTwo( double arg ){
		return Math.log( arg ) * DOUBLE_INVERSE_LN_2;
	}
	public static final double DOUBLE_INVERSE_LN_2 = (double)1 / Math.log( (double)2 );
	/** @since 020305 */
	public static final BigInteger logBaseTwo( BigInteger arg ){
		return BigInteger.valueOf( (long)logBaseTwo(arg.doubleValue()) );
	}
	public static final BigInteger BIGINTEGER_TWO = BigInteger.ONE.add( BigInteger.ONE );
	//public static final BigInteger BIGINTEGER_INVERSE_LN_2 = BigInteger.ONE.divide( Math.log( BIGINTEGER_TWO ) );
}
