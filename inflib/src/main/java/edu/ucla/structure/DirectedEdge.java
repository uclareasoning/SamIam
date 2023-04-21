package edu.ucla.structure;

/** @author keith cascio
	@since  20040613 */
public class DirectedEdge extends Edge{
    public DirectedEdge( Object source, Object sink ){
        super(                  source,        sink );
    }

	/** Swapping the vertices must result in a distinct hash.
		@since 20091219 */
    public int hashCode(){
        return v1.hashCode() ^ (v2.hashCode() << 1);
    }

    public boolean equals( Object obj ){
        if( obj instanceof Edge ){
            Edge e = (Edge) obj;
            return e.v1().equals( v1() ) && e.v2().equals( v2() );
        }
        else if( obj instanceof Object[] ){
			Object[] array = (Object[]) obj;
			if( array.length > 1 ){ return v1().equals( array[0] ) && v2().equals( array[1] ); }
		}

		return false;
    }
}
