package edu.ucla.structure;

/** @author JC Park
	@author Keith Cascio */
public class Edge
{
    public Edge( Object vertex1, Object vertex2 ){
        setVertices( vertex1, vertex2 );
    }

    public Object v1(){
		return v1;
	}

    public Object v2(){
		return v2;
	}

	public void setVertices( Object v1, Object v2 ){
		this.v1 = v1;
		this.v2 = v2;
	}

    public String toString() {
        return "["+v1 + ","+v2 + "]";
    }
    public int hashCode() {
        return v1.hashCode() ^ v2.hashCode();
    }
    public boolean equals(Object obj) {
        if (!(obj instanceof Edge)) {
            return false;
        } else {
            Edge e = (Edge) obj;
            return (e.v1.equals(v1) && e.v2.equals(v2)) ||
                    (e.v2.equals(v1) && e.v1.equals(v2));
        }
    }

    protected Object v1, v2;
}
