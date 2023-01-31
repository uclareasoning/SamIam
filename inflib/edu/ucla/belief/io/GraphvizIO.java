package edu.ucla.belief.io;

import edu.ucla.structure.DirectedGraph;
import edu.ucla.belief.BeliefNetwork;
import edu.ucla.util.*;

import java.util.*;
import java.io.*;
import java.util.regex.*;
import java.awt.Point;
import java.awt.Dimension;

/** @author Keith Cascio
	@since 020805 */
public class GraphvizIO
{
	public GraphvizIO() {}

	/** @since 020805 */
	public void normalize( Map data ){
		if( data.isEmpty() ) return;
		Point min = new Point( Integer.MAX_VALUE, Integer.MAX_VALUE );
		Collection values = data.values();
		Point point;
		for( Iterator it = values.iterator(); it.hasNext(); ){
			point = (Point) it.next();
			min.x = Math.min( min.x, point.x );
			min.y = Math.min( min.y, point.y );
		}
		if( (min.x <= 0) || (min.y <= 0) ) return;
		int deltax = -min.x;
		int deltay = -min.y;
		for( Iterator it = values.iterator(); it.hasNext(); ){
			point = (Point) it.next();
			point.translate( deltax, deltay );
		}
	}

	/** @since 020705 */
	public Map gleanLayoutFromDot( File dotFile, Map data ) throws IOException
	{
		if( data == null ) data = new HashMap();
		BufferedReader reader = new BufferedReader( new FileReader( dotFile ) );
		String regex = "^\\s*\"?(\\w|\\w[\\w ]*\\w)\"?\\s*\\[.*pos=\"(\\d+),(\\d+)\".*\\];";
		//System.out.println( "regex \"" + regex + "\"" );
		Pattern pattern = Pattern.compile( regex );
		Matcher matcher;
		String line, id, xcoord, ycoord;
		Point point;
		while( (line = reader.readLine()) != null ){
			matcher = pattern.matcher( line );
			if( matcher.find() ){
				id		= matcher.group(1);
				xcoord	= matcher.group(2);
				ycoord	= matcher.group(3);
				//System.out.println( "Found \"" + id + "\" -> [" + xcoord + "," + ycoord + "]" );
				point = new Point( Integer.parseInt( xcoord ), Integer.parseInt( ycoord ) );
				data.put( id, point );
			}
		}
		return data;
	}

    /** @since 020705 */
   	public void writeDot( DirectedGraph dg, String title, java.io.PrintStream out ){
   		writeDot( dg, title, AbstractStringifier.VARIABLE_ID, new StandardNodeDimensionSupplier( dg ), out );
   	}

    /** @since 020705 */
   	public void writeDot( DirectedGraph dg, String title, Stringifier stringifier, ExtraDotInfoSupplier supplier, java.io.PrintStream out ){
   		out.println( "digraph \""+title+"\" {" );
		out.println( "\tgraph [label = \""+title+"\""+supplier.getGlobalGraphInfo()+"];" );
		//out.println( "\tnode [fixed-size=true, shape = polygon, sides = 4, distortion = \"0.0\", orientation = \"0.0\", skew = \"0.0\", color = white, style = filled, fontname = \"Helvetica-Outline\" ];" );
		out.println( "\tnode [shape=polygon, sides=4];" );
		Object vertex;
		for( Iterator it = dg.iterator(); it.hasNext(); ){
			vertex = it.next();
			out.println( "\t\""+ stringifier.objectToString( vertex ) +"\" ["+supplier.getVertexInfo( vertex )+"];" );
		}
		Set outgoing;
		String vstring;
		for( Iterator vit = dg.iterator(); vit.hasNext(); ){
			vertex = vit.next();
			outgoing = dg.outGoing( vertex );
			vstring = stringifier.objectToString( vertex );
			for( Iterator outgoingit = outgoing.iterator(); outgoingit.hasNext(); ){
				out.println( "\t\"" + vstring + "\" -> \""+ stringifier.objectToString( outgoingit.next() ) +"\"" );
			}
		}
   		out.println( "}" );
   	}

   	/** @since 020805 */
   	public interface ExtraDotInfoSupplier{
   		public String getVertexInfo( Object vertex );
   		public String getGlobalGraphInfo();
   	}

   	public static float FLOAT_PIXELS_PER_INCH = (float)80;
   	public static int INT_DIM_PADDING = (int)16;

   	/** @since 020805 */
	public static float scale( int dim ){
		return ((float)(dim + INT_DIM_PADDING))/FLOAT_PIXELS_PER_INCH;
	}

   	/** @since 020805 */
   	public static class StandardNodeDimensionSupplier implements ExtraDotInfoSupplier
   	{
   		public StandardNodeDimensionSupplier( Collection vertices ){
   			//System.out.println( "new StandardNodeDimensionSupplier()" );
   			Point max = new Point();
   			Point min = new Point();
   			Point point = new Point();
   			for( Iterator it = vertices.iterator(); it.hasNext(); ){
   				//point = (Point) it.next();
				((StandardNode) it.next()).getLocation( point );
				max.x = Math.max( max.x, point.x );
				max.y = Math.max( max.y, point.y );
				min.x = Math.min( min.x, point.x );
				min.y = Math.min( min.y, point.y );
   			}
   			this.myWidthInches = scale( Math.abs( max.x - min.x ) );
   			this.myHeightInches = scale( Math.abs( max.y - min.y ) );
   			//System.out.println( "    width? " + myWidthInches );
   			//System.out.println( "    height? " + myHeightInches );
   		}

   		public String getGlobalGraphInfo(){
   			return ", size=\"" +myWidthInches+ "," +myHeightInches+ "\"";
   		}

   		public String getVertexInfo( Object vertex ){
   			String ret = "color=goldenrod2";
   			if( vertex instanceof StandardNode ){
   				Dimension dim = ((StandardNode)vertex).getDimension( new Dimension() );
   				String dimensions = ", width=\""+scale( dim.width )+"\", height=\""+scale( dim.height )+"\"";
   				ret += dimensions;
   			}
   			return ret;
   		}

   		private float myWidthInches;
   		private float myHeightInches;
   	};

   	public static final String STR_ARG_DOT = "-dot";
   	public static final String STR_ARG_INPUTFILEPATH = "-in";
   	public static final String STR_ARG_OUTPUTFILEPATH = "-out";

    /** @since 020705 */
   	public static void main( String[] args ){
   		java.io.PrintStream streamVerbose = System.out;
   		boolean flagWriteDot = false;
   		String fname = null;
   		String opath = null;
   		for( int i=0; i<args.length; i++ ){
   			if( args[i].equals( STR_ARG_DOT ) ) flagWriteDot = true;
   			else if( args[i].startsWith( STR_ARG_INPUTFILEPATH ) ){
   				fname = args[i].substring( STR_ARG_INPUTFILEPATH.length() );
   			}
   			else if( args[i].startsWith( STR_ARG_OUTPUTFILEPATH ) ){
   				opath = args[i].substring( STR_ARG_OUTPUTFILEPATH.length() );
   			}
   		}
   		if( (fname == null) || (fname.length() < 1) ){
   			System.err.println( "usage: "+GraphvizIO.class.getName()+" -in<input file path> [-dot]" );
   			return;
   		}
   		File infile = new File( fname );
   		if( !infile.exists() ){
   			System.err.println( "File " + fname + " not found." );
   			return;
   		}
   		String netname = edu.ucla.belief.io.NetworkIO.extractNetworkNameFromPath( fname );
   		if( opath == null ) opath = infile.getParentFile().getAbsolutePath() + File.separator + netname + ".dot";
   		if( flagWriteDot ){
   			try{
				BeliefNetwork bn = edu.ucla.belief.io.NetworkIO.read( infile );
				PrintStream out = new PrintStream( new FileOutputStream( new File( opath ) ) );
				new GraphvizIO().writeDot( bn, netname, out );
				out.close();
				streamVerbose.println( "Wrote " +  opath );
			}catch( Exception e ){
				System.err.println( "Failed to write dot format." );
				e.printStackTrace();
				return;
			}
   		}
   		else{
   			System.err.println( "Nothing to do.  Try '-dot'." );
   			return;
   		}
   	}
}
