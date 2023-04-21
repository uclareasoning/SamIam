package edu.ucla.belief.io.xmlbif;

import edu.ucla.belief.io.*;
//import edu.ucla.util.JVMTI;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import java.io.*;
import java.util.regex.*;

/** On smaller files costs about 13% or parse time,
	on larger files only costs about 4% of parse time.

	@author keith cascio
	@since  20060619 */
public class SkimmerEstimator extends DefaultHandler implements Estimate
{
	public SkimmerEstimator( File xmlFile ){
		this.myFile = xmlFile;
	}

	public SkimmerEstimator( InputStream stream ){
		this.myInputStream = stream;
	}

	public SkimmerEstimator(){}

	/** interface Estimate */
	public void init( File xmlFile ){
		this.myFile = xmlFile;
	}

	/** interface Estimate */
	public int expectedNodes(){
		return getCountVariables();
	}

	/** interface Estimate */
	public int expectedEdges(){
		throw new UnsupportedOperationException();
	}

	/** interface Estimate */
	public long expectedValues(){
		return getCountProbabilityValues();
	}

	private BufferedInputStream stream() throws IOException {
		if(      this.myReader      != null ) throw new IllegalStateException();
		else if( this.myFile        != null ) return new BufferedInputStream( new FileInputStream( this.myFile ) );
		else if( this.myInputStream != null ){
			if( this.myInputStream instanceof BufferedInputStream ) return (BufferedInputStream)this.myInputStream;
			else return new BufferedInputStream( this.myInputStream );
		}
		else return null;
	}

	/** interface Estimate */
	public void estimate(){
		if( myFlagDoneEstimate ) return;
		long start = System.currentTimeMillis();
		//long start = JVMTI.getCurrentThreadCpuTimeUnsafe();

		myCountVariables         = 0;
		myCountPotentials        = 0;
		myCountProbabilityValues = 0;

		try{
			getSAXParser().parse( stream(), (DefaultHandler)this );
		}
		catch( Throwable throwable ){
			System.err.println( "warning: SkimmerEstimator.estimate() caught " + throwable );
		}

		myCountPotentials = myCountVariables;//assumption

		this.interpret();

		long end = System.currentTimeMillis();
		//long end = JVMTI.getCurrentThreadCpuTimeUnsafe();
		myCost = end - start;
		//System.out.println( "SkimmerEstimator.estimate() cost " + myCost + ", found " + getCountVariables() + " nodes, " + getCountPotentials() + " potentials, " + getCountProbabilityValues() + " values" );
		//System.out.println( "    inc node: " + getProgressIncrementNodeBlock() + ", inc potential: " + getProgressIncrementPoteBlock() + ", max: " + getProgressMax() );

		myFlagDoneEstimate = true;
	}

	public void startElement( String uri, String localName, String qName, Attributes attributes ) throws SAXException
	{
		if( XmlbifParser.STR_TAG_VARIABLE.equalsIgnoreCase( qName ) ) ++myCountVariables;
	}

	public static SAXParserFactory getFactory(){
		if( FACTORY == null ){
			FACTORY = SAXParserFactory.newInstance();
		}
		FACTORY.setValidating( false );
		return FACTORY;
	}

	public static SAXParser getSAXParser() throws ParserConfigurationException,SAXException {
		if( SAXPARSER == null ) SAXPARSER = getFactory().newSAXParser();
		return SAXPARSER;
	}

	private static SAXParserFactory FACTORY;
	private static SAXParser        SAXPARSER;

	public long getCost(){
		return myCost;
	}

	public int getProgressIncrementNodeBlock(){
		return myProgressIncrementNodeBlock;
	}

	public int getProgressIncrementPoteBlock(){
		return myProgressIncrementPoteBlock;
	}

	public int getProgressMax(){
		return myProgressMax;
	}

	private void interpret(){
		float averageSizePotential = (float)(((double)myCountProbabilityValues) / ((double)myCountPotentials));

		if( averageSizePotential < 2 ) averageSizePotential = 2;

		float tn = 1;
		float tp = averageSizePotential / 64;//120;//120 was calculated for barley

		if( tp < 1 ){
			tn = 1/tp;
			tp = 1;
		}

		myProgressIncrementNodeBlock = (int) tn;
		myProgressIncrementPoteBlock = (int) tp;

		myProgressMax = (myCountVariables * myProgressIncrementNodeBlock) + (myCountPotentials * myProgressIncrementPoteBlock);
	}

	public int getCountVariables(){
		return myCountVariables;
	}

	public int getCountPotentials(){
		return myCountPotentials;
	}

	public long getCountProbabilityValues(){
		return myCountProbabilityValues;
	}

	private int  myCountVariables         = -1;
	private int  myCountPotentials        = -1;
	private long myCountProbabilityValues = -1;
	private long myCost                   = -1;

	private int myProgressIncrementNodeBlock = -1;
	private int myProgressIncrementPoteBlock = -1;
	private int myProgressMax                = -1;

	private File        myFile;
	private Reader      myReader;
	private InputStream myInputStream;
	private boolean     myFlagDoneEstimate = false;
}
