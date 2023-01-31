package il2.inf.edgedeletion;
import java.util.Comparator;
import java.util.HashMap;

public class EdgeComparator implements Comparator<int[]> {
	HashMap<int[],Double> edgeToScoreMap;

	public EdgeComparator() {
		edgeToScoreMap = null;
	}

	public EdgeComparator(int[][] edges, double[] scores) {
		edgeToScoreMap = new HashMap<int[],Double>( edges.length );
		for (int edge = 0; edge < edges.length; edge++)
			edgeToScoreMap.put(edges[edge],new Double(scores[edge]));
	}
	
	public int compare(int[] edge1, int[] edge2) {
		if ( edgeToScoreMap == null ) 
			return compareByIndex(edge1,edge2);
		else 
			return compareByScore(edge1,edge2);
	}

	private int compareByIndex(int[] edge1, int[] edge2) {
		if ( edge1[0] < edge2[0] ) return -1;
		if ( edge1[0] == edge2[0] && edge1[1] < edge2[1] ) return -1;
		if ( edge1[0] == edge2[0] && edge1[1] == edge2[1] ) return 0;
		return 1;
	}

	private int compareByScore(int[] edge1, int[] edge2) {
        Double score1 = edgeToScoreMap.get(edge1);
        Double score2 = edgeToScoreMap.get(edge2);
        int cmp = score1.compareTo(score2);
        if ( cmp != 0 ) return cmp;
        else return compareByIndex(edge1,edge2);
        /*
		double score1 = edgeToScoreMap.get(edge1).doubleValue();
		double score2 = edgeToScoreMap.get(edge2).doubleValue();
		if ( score1 < score2 ) return -1;
		if ( score1 > score2 ) return 1;
		// now, must have same score
		if ( edge1[0] < edge2[0] ) return -1;
		if ( edge1[0] > edge2[0] ) return 1;
		// now, must have same parent
		if ( edge1[1] < edge2[1] ) return -1;
		if ( edge1[1] > edge2[1] ) return 1;
		// now, must have same child
		return 0;
        */
	}
}
