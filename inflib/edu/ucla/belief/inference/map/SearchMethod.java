package edu.ucla.belief.inference.map;

/**
	@author Keith Cascio
	@since 021903
*/
public abstract class SearchMethod
{
	abstract public MapApproximator getApproximator();
	abstract public String getJavaCodeName();

	/** @since 051004 */
	public static SearchMethod getDefault(){
		return TABOO;
	}

	public static final SearchMethod HILL = new SearchMethod()
	{
		public MapApproximator getApproximator(){
			return new CHillRR();
		}

		public String toString(){
			return "Hill Climbing";
		}

		public String getJavaCodeName(){
			return "HILL";
		}
	};

	public static final SearchMethod TABOO = new SearchMethod()
	{
		public MapApproximator getApproximator(){
			return new CTaboo();
		}

		public String toString(){
			return "Taboo Search";
		}

		public String getJavaCodeName(){
			return "TABOO";
		}
	};

	public static final SearchMethod[] ARRAY = new SearchMethod[]{ HILL, TABOO };
}
