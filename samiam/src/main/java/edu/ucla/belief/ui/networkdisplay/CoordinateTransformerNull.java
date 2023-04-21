package edu.ucla.belief.ui.networkdisplay;

import java.awt.*;

/**
	Implements the zero transformation, i.e. no transformation at all.
	@author Keith Cascio
	@since 061404
*/
public class CoordinateTransformerNull implements CoordinateTransformer
{
	public static CoordinateTransformerNull getInstance()
	{
		if( INSTANCE == null ) INSTANCE = new CoordinateTransformerNull();
		return INSTANCE;
	}

	private CoordinateTransformerNull(){}

	private static CoordinateTransformerNull INSTANCE;

	public Point		virtualToActual( Point p )
	{
		return p;
	}
	public Point		actualToVirtual( Point p )
	{
		return p;
	}
	public Dimension	virtualToActual( Dimension d )
	{
		return d;
	}
	public Dimension	actualToVirtual( Dimension d )
	{
		return d;
	}
	public float		virtualToActual( float f )
	{
		return f;
	}
	public double		virtualToActual( double d )
	{
		return d;
	}
}
