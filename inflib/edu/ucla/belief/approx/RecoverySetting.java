package edu.ucla.belief.approx;

import        edu.ucla.util.*;
import        edu.ucla.util.PropertyKey;
import        edu.ucla.belief. CrouchingTiger. DynamatorImpl;
import static edu.ucla.belief.approx. EdgeDeletionBeliefPropagationSetting. CAPTION_COMPARE2EXACT;

import        il2.inf.edgedeletion. EDAlgorithm;
import        il2.inf.edgedeletion. EDAlgorithm.RankingHeuristic;

import        java.util.*;
import static java.awt.event.KeyEvent.*;
import static javax.swing.KeyStroke.getKeyStroke;

/** @author keith cascio
	@since  20091207 */
public enum RecoverySetting implements Setting{
	iterations      ( "Bound, maximum iterations",    "(0 = unbounded)",            0,                Integer.MAX_VALUE, EDAlgorithm.INT_MAX_ITERATIONS_DEFAULT,  Number.class, getKeyStroke( VK_B, CTRL_MASK ), false, 0x10   ),
	timeout         ( "Time out (milliseconds)",      "(0 = no time out)",          0,                Integer.MAX_VALUE, EDAlgorithm.LONG_TIMEOUT_MILLIS_DEFAULT, Number.class, getKeyStroke( VK_T, CTRL_MASK ), false, 0x1000 ),
	threshold       ( "Termination tolerance",        "(0 <= tolerance)",         0.0,                              1.0, EDAlgorithm.DOUBLE_THRESHOLD_DEFAULT,    Number.class, getKeyStroke( VK_C, CTRL_MASK | ALT_MASK ), false ),
	heuristic       ( "Edge ranking heuristic",       "",                           0, RankingHeuristic.values().length,      RankingHeuristic.DEFAULT, RankingHeuristic.class, getKeyStroke( VK_H, CTRL_MASK ), false ),
	recovery        ( "Edge recovery count",          "tree ... full network",      0,                               32,                                   0,     Number.class, getKeyStroke( VK_R, CTRL_MASK ), false, 0x1, "ratios", true ),
	seed            ( "Random seed",                  "",           Integer.MIN_VALUE,                Integer.MAX_VALUE,              new Random().nextInt(),     Number.class, getKeyStroke( VK_S, CTRL_MASK | ALT_MASK ), false, 0x1,  "seeds" ),
	compare2exact   ( "Report exact values",          CAPTION_COMPARE2EXACT(),      0,                                1,                               false,    Boolean.class, getKeyStroke( VK_E, CTRL_MASK ), false ),
	subalgorithm    ( "Sub-algorithm",                "partial derivatives",        0, DynamatorImpl.il2Partials().size(),             DynamatorImpl.zchugin, DynamatorImpl.class, getKeyStroke( VK_A, CTRL_MASK ), false, 0x1, "engines", false, DynamatorImpl.il2Partials() );

	private RecoverySetting( Object ... values ){
		PropertyKey[]                  keys = PropertyKey.values();
		EnumMap<PropertyKey,Object> enummap = new EnumMap<PropertyKey,Object>( PropertyKey.class );
		int i=0;
		for( Object value :  values ){
			if( value != null ){ enummap.put( keys[ i ], value ); }
			++i;
		}
		this.properties = Collections.unmodifiableMap( enummap );
	}

	public Object get( PropertyKey key ){ return properties.get( key ); }

	public Map<PropertyKey,Object> properties(){ return this.properties; }

	final public Map<PropertyKey,Object> properties;
}
