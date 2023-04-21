package edu.ucla.belief.recursiveconditioning;

import java.util.*;
import java.io.*;

import edu.ucla.belief.dtree.*;
import edu.ucla.belief.*;

//Add these lines to import the JAXP APIs you'll be using:
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
//{superfluous} import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.OutputKeys;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

//Add these lines for the exceptions that can be thrown when the XML document is parsed:
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

//Finally, import the W3C definition for a DOM and DOM exceptions:
import org.w3c.dom.Document;
//{superfluous} import org.w3c.dom.Node;
import org.w3c.dom.Element;
//{superfluous} import org.w3c.dom.Attr;
import org.w3c.dom.Text;
import org.w3c.dom.NodeList;
//{superfluous} import org.w3c.dom.DOMException;
import org.w3c.dom.DocumentType;

/**
	@author Keith Cascio
	@since 022603
*/
public class Xmlizer
{
	public static boolean FLAG_VALIDATE = false;
	public static int INT_ID_COUNTER = (int)0;
	public static int INT_ROOT_COUNTER = (int)0;
	public static final String STR_ATTR_ID_NAME = "id";
	public static final String STR_ATTR_FACTOR_NAME = "factor";
	public static final String STR_ATTR_LEFT_NAME = "left";
	public static final String STR_ATTR_RIGHT_NAME = "right";
	public static final String STR_ELT_VARIABLE_NAME = "var";
	public static final String STR_ELT_ROOT_NAME = "rc";
	public static final String STR_ELT_RCNODE_NAME = "rcnode";
	public static final String STR_PREFIX_ID_ROOT = "root";
	public static final String STR_ATTR_CREATIONMETHOD_NAME = "method";
	public static final String STR_ATTR_DTREEMAXCLUSTER_NAME = "maxcluster";
	public static final String STR_ATTR_DTREEHEIGHT_NAME = "height";
	public static final String STR_ATTR_DTREECUTSET_NAME = "maxcutset";
	public static final String STR_ATTR_DTREECONTEXT_NAME = "maxcontext";
	public static final String STR_ATTR_NETWORKNAME_NAME = "network";
	public static final String STR_ATTR_MARGINALVARS_NAME = "marginalvars";
	public static final String STR_ATTR_CANCOMPUTE_NAME = "computesfamilymarginals";
	public static final String STR_ATTR_VARSNULLROOT_NAME = "varsnullroot";
	public static final String STR_ATTR_OPTIMALMEMORY_NAME = "optimalmemory";
	public static final String STR_ATTR_USERMEMORY_NAME = "usermemory";
	public static final String STR_ATTR_ESTIMATEDTIME_NAME = "estimatedtime";

	public static final String STR_DELIMITERS = ",";

	public org.w3c.dom.Element makeElement( RCNode rcnode, org.w3c.dom.Document doc, String id )
	{
		Element ret = doc.createElement( STR_ELT_RCNODE_NAME );

		ret.setAttribute( STR_ATTR_ID_NAME, id );

		return ret;
	}

	public org.w3c.dom.Element recXmlize( RCNode rcnode, String id, org.w3c.dom.Document doc, org.w3c.dom.Element elt, java.util.Map visited )
	{
		Element myElement;

		if( visited.containsKey( rcnode ) ) myElement = (Element) visited.get( rcnode );
		else
		{
			myElement = makeElement( rcnode, doc, id );
			if( rcnode instanceof RCNodeInternalBinaryCache ) hookRCNodeInternalBinary( rcnode, myElement, doc, elt, visited );
			else hookRCNodeLeaf( rcnode, myElement, doc );
			elt.appendChild( myElement );
			visited.put( rcnode, myElement );
		}

		return myElement;//.getAttribute( STR_ATTR_ID_NAME );
	}

	public String recXmlize( RCNode rcnode, org.w3c.dom.Document doc, org.w3c.dom.Element elt, java.util.Map visited )
	{
		Element myElement;

		if( visited.containsKey( rcnode ) ) myElement = (Element) visited.get( rcnode );
		else
		{
			myElement = makeElement( rcnode, doc, String.valueOf( INT_ID_COUNTER++ ) );
			if( rcnode instanceof RCNodeInternalBinaryCache ) hookRCNodeInternalBinary( rcnode, myElement, doc, elt, visited );
			else hookRCNodeLeaf( rcnode, myElement, doc );
			elt.appendChild( myElement );
			visited.put( rcnode, myElement );
		}

		return myElement.getAttribute( STR_ATTR_ID_NAME );
	}

	public void hookRCNodeLeaf( RCNode rcnode, org.w3c.dom.Element rcnodeElt, org.w3c.dom.Document doc )
	{
		RCNodeLeaf leaf = (RCNodeLeaf) rcnode;
		//myTempList.clear();
		//leaf.vars( myTempList );

		//Element variableElt;
		//for( Iterator it = myTempList.iterator(); it.hasNext(); )
		//{
		//	variableElt = doc.createElement( STR_ELT_VARIABLE_NAME );
		//	variableElt.setAttribute( STR_ATTR_ID_NAME, ((Variable) it.next()).getID() );
		//	rcnodeElt.appendChild( variableElt );
		//}
		rcnodeElt.setAttribute( STR_ELT_VARIABLE_NAME, leaf.getLeafVar().getID() );
	}

	//protected List myTempList = new ArrayList( 32 );

	public void hookRCNodeInternalBinary( RCNode rcnode, org.w3c.dom.Element rcnodeElt, org.w3c.dom.Document doc, org.w3c.dom.Element elt, java.util.Map visited )
	{
		RCNodeInternalBinaryCache internal = (RCNodeInternalBinaryCache) rcnode;
		String strIDleft = recXmlize( internal.left(), doc, elt, visited );
		String strIDright = recXmlize( internal.right(), doc, elt, visited );
		rcnodeElt.setAttribute( STR_ATTR_FACTOR_NAME, Double.toString( rcnode.getCacheFactor() ) );
		rcnodeElt.setAttribute( STR_ATTR_LEFT_NAME, strIDleft );
		rcnodeElt.setAttribute( STR_ATTR_RIGHT_NAME, strIDright );
	}


	/**
		@author Keith Cascio
		@since 031903
	*/
	public FileInfo skimFile( File infile ) throws IOException
	{
		FileInfo ret = new FileInfo();

		if( mySAXParser == null )
		{
			try{
				mySAXParser = SAXParserFactory.newInstance().newSAXParser();
			}catch( ParserConfigurationException e ){
				System.err.println( "Warning: Xmlizer.skimFile() caused " + e );
				return ret;
			}catch( SAXException e ){
				System.err.println( "Warning: Xmlizer.skimFile() caused " + e );
				return ret;
			}
		}

		if( myStatsHandler == null ) myStatsHandler = new StatsHandler();

		myStatsHandler.setFileInfo( ret );

		try{
			mySAXParser.parse( infile, myStatsHandler );
		}catch( SAXException e ){
			System.err.println( "Warning: Xmlizer.skimFile() caused " + e );
			return ret;
		}

		return ret;
	}

	protected SAXParser mySAXParser;
	protected StatsHandler myStatsHandler;

	/** test/debug
		@since  20030319 */
	public static void main( String[] args ){
		Xmlizer izer = new Xmlizer();

		String fpath = "c:\\barley_graph.rc";
		File infile = new File( fpath );

		FileInfo info = null;
		try{
			info = izer.skimFile( infile );
		}catch( IOException e ){
			System.err.println( "Warning: Xmlizer.main() caught " + e );
		}

		Definitions.STREAM_TEST.println( info.stats );
	}

	/** @since 022603 */
	public org.w3c.dom.Document xmlize( Dtree dtree, RC rc )// throws javax.xml.parsers.ParserConfigurationException
	{
		return xmlize( dtree, null, rc );
	}

	/** @since 031803 */
	public org.w3c.dom.Document xmlize( Dtree dtree, Stats stats, RC rc )
	{
		return xmlize( dtree, stats, rc, null, null );
	}

	/** @since 031903 */
	public org.w3c.dom.Document xmlize( Dtree dtree, Stats stats, RC rc, Computation comp, String networkname )
	{
		INT_ID_COUNTER = (int)0;
		INT_ROOT_COUNTER = (int)0;
		Map visited = new HashMap();

		javax.xml.parsers.DocumentBuilder builder = null;
		org.w3c.dom.Document ret = null;
		try{
			builder = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
			ret = builder.newDocument();
		}catch( javax.xml.parsers.ParserConfigurationException e ){
			if( Definitions.DEBUG ) e.printStackTrace();
			return null;
		}

		org.w3c.dom.Element eltRoot = ret.createElement( STR_ELT_ROOT_NAME );
		if( networkname != null ) eltRoot.setAttribute( STR_ATTR_NETWORKNAME_NAME, networkname );

		if( dtree != null )
		{
			org.w3c.dom.Element eltDtreeRoot = xmlizeDtree( dtree, stats, ret );
			eltRoot.appendChild( eltDtreeRoot );
		}

		org.w3c.dom.Element eltRCRoot = ret.createElement( rc.getTagName() );
		RCNode[] roots = rc.getRoots();
		Map rootToVariableList = makeMapRootToVariableList( rc );
		org.w3c.dom.Element eltRCNode;
		for( int i=0; i<roots.length; i++ )
		{
			eltRCNode = recXmlize( roots[i], STR_PREFIX_ID_ROOT + Integer.toString( INT_ROOT_COUNTER++ ), ret, eltRCRoot, visited );
			if( rootToVariableList != null ) eltRCNode.setAttribute( STR_ATTR_MARGINALVARS_NAME, toString( (List) rootToVariableList.get( roots[i] ) ) );
		}
		eltRoot.appendChild( eltRCRoot );
		eltRCRoot.setAttribute( STR_ATTR_CANCOMPUTE_NAME, Boolean.toString( rc.canComputeFamilyMarginals() ) );
		if( comp != null )
		{
			//eltRCRoot.setAttribute( STR_ATTR_OPTIMALMEMORY_NAME, Double.toString( comp.getOptimalMemoryRequirement() ) );
			//eltRCRoot.setAttribute( STR_ATTR_USERMEMORY_NAME, Double.toString( comp.getNumCacheEntries( rc ) ) );
			eltRCRoot.setAttribute( STR_ATTR_USERMEMORY_NAME, Settings.formatMemoryNumbersConcise( comp, rc ) );
			String[] arrayEstimatedTime = comp.updateEstimatedMinutesDisplay();
			eltRCRoot.setAttribute( STR_ATTR_ESTIMATEDTIME_NAME, comp.getDescriptor() + " " + arrayEstimatedTime[0] + arrayEstimatedTime[1] );
		}
		if( rootToVariableList != null )
		{
			List listVarsNullRoot = (List) rootToVariableList.get( null );
			eltRCRoot.setAttribute( STR_ATTR_VARSNULLROOT_NAME, toString( listVarsNullRoot ) );
		}

		ret.appendChild( eltRoot );

		return ret;
	}

	/** @since 060303 */
	static public String toString( List list )
	{
		String ret = "";

		if( list == null ) return ret;

		for( Iterator it = list.iterator(); it.hasNext(); )
		{
			ret += ((Variable)it.next()).getID() + STR_DELIMITERS;
		}
		return ret;
	}

	/** @since 031903 */
	static public Map makeMapRootToVariableList( RC rc )
	{
		Map varToRoot = rc.varToRoot();
		//System.out.print( "Xmlizer.makeMapRootToVariableList("+varToRoot+")..." );

		if( varToRoot.isEmpty() ) return null;

		Map ret = new HashMap();

		List listTemp;
		Object varTemp;
		Object rootTemp;
		Map.Entry entry;
		for( Iterator it = varToRoot.entrySet().iterator(); it.hasNext(); )
		{
			entry = (Map.Entry) it.next();
			varTemp = entry.getKey();
			rootTemp = entry.getValue();
			if( ret.containsKey( rootTemp ) )
			{
				listTemp = (List) ret.get( rootTemp );
				if( !listTemp.contains( varTemp ) ) listTemp.add( varTemp );
			}
			else
			{
				listTemp = new LinkedList();
				listTemp.add( varTemp );
				ret.put( rootTemp, listTemp );
			}
		}

		//System.out.println( "returning " + ret );

		return ret;
	}

	/** @since 031803 */
	protected org.w3c.dom.Element xmlizeDtree( Dtree dtree, Stats stats, org.w3c.dom.Document ret )
	{
		org.w3c.dom.Element eltDtreeRoot = ret.createElement( dtree.getTagName() );

		if( dtree.myCreationMethod != null ) eltDtreeRoot.setAttribute( STR_ATTR_CREATIONMETHOD_NAME, dtree.myCreationMethod.getID() );

		if( stats != null )
		{
			if( stats.height != Stats.INT_INVALID_DTREE_STAT ) eltDtreeRoot.setAttribute( STR_ATTR_DTREEHEIGHT_NAME, Integer.toString( stats.height ) );
			if( stats.maxCluster != Stats.INT_INVALID_DTREE_STAT ) eltDtreeRoot.setAttribute( STR_ATTR_DTREEMAXCLUSTER_NAME, Integer.toString( stats.maxCluster ) );
			if( stats.maxCutset != Stats.INT_INVALID_DTREE_STAT ) eltDtreeRoot.setAttribute( STR_ATTR_DTREECUTSET_NAME, Integer.toString( stats.maxCutset ) );
			if( stats.maxContext != Stats.INT_INVALID_DTREE_STAT ) eltDtreeRoot.setAttribute( STR_ATTR_DTREECONTEXT_NAME, Integer.toString( stats.maxContext ) );
		}

		org.w3c.dom.Text txtDtree = ret.createTextNode( dtree.toString() );
		eltDtreeRoot.appendChild( txtDtree );
		return eltDtreeRoot;
	}

	/** @since 022703 */
	public boolean readxml( File fileSelected, BeliefNetwork bn, Settings settings, boolean useKB ) throws Exception
	{
		Document document = readxml( fileSelected );

		Element eltRoot = document.getDocumentElement();

		if( eltRoot == null ) throw new Exception( "null document element" );

		if( !eltRoot.getTagName().equals( STR_ELT_ROOT_NAME ) ) throw new Exception( "invalid root element tag name" );

		NodeList nodelistDtree = eltRoot.getElementsByTagName( Dtree.getTagName() );
		NodeList nodelistrcdtree = eltRoot.getElementsByTagName( RCDtree.getStaticTagName() );
		NodeList nodelistrcdgraph = eltRoot.getElementsByTagName( RCDgraph.getStaticTagName() );

		//if( nodelistDtree.getLength() != (int)1 ) throw new Exception( Dtree.getTagName() + " element not found" );

		if( !( nodelistrcdtree.getLength() + nodelistrcdgraph.getLength() == (int)1 ) )  throw new Exception( RCDtree.getStaticTagName() + " or " + RCDgraph.getStaticTagName() + " element not found or invalid" );

		Dtree dtree = null;
		if( nodelistDtree.getLength() > 0 ) dtree = readxmldtree( (Element) nodelistDtree.item( 0 ), bn );

		if( dtree == null ) settings.setDtreeRequired( false );
		else settings.setDtree( dtree );

		RC rc = null;
		if( nodelistrcdtree.getLength() == 1 ) rc = readxmlrcdtree( (Element) nodelistrcdtree.item( 0 ), bn, settings, (double)1, useKB, fileSelected );
		else if( nodelistrcdgraph.getLength() == 1 ) rc = readxmlrcdgraph( (Element) nodelistrcdgraph.item( 0 ), bn, settings, useKB, fileSelected );

		return ( rc != null );
	}

	/** @since 022703 */
	public Dtree readxmldtree( Element eltDtree, BeliefNetwork bn ) throws Exception
	{
		Text txtDtree = (Text) eltDtree.getFirstChild();
		if( txtDtree == null ) throw new Exception( "empty element " + eltDtree.getTagName() );

		Dtree newDtree = new Dtree(	bn,
						new DtreeCreateString( new PushbackReader( new StringReader( txtDtree.getData() ) ) ));
		String strMethod = eltDtree.getAttribute( STR_ATTR_CREATIONMETHOD_NAME );
		CreationMethod cm = CreationMethod.forID( strMethod );
		newDtree.myCreationMethod = cm;

		return newDtree;
	}


	public RCDtree readxmlrcdtree( BeliefNetwork bn, Settings settings, double scalar, boolean useKB, File fileSelected ) throws Exception
	{
		Document document = readxml( fileSelected );

		Element eltRoot = document.getDocumentElement();

		if( eltRoot == null ) throw new Exception( "null document element" );

		if( !eltRoot.getTagName().equals( STR_ELT_ROOT_NAME ) ) throw new Exception( "invalid root element tag name" );

		NodeList nodelistDtree = eltRoot.getElementsByTagName( Dtree.getTagName() );
		NodeList nodelistrcdtree = eltRoot.getElementsByTagName( RCDtree.getStaticTagName() );
		NodeList nodelistrcdgraph = eltRoot.getElementsByTagName( RCDgraph.getStaticTagName() );

		//if( nodelistDtree.getLength() != (int)1 ) throw new Exception( Dtree.getTagName() + " element not found" );

		if( !( nodelistrcdtree.getLength() + nodelistrcdgraph.getLength() == (int)1 ) )  throw new Exception( RCDtree.getStaticTagName() + " or " + RCDgraph.getStaticTagName() + " element not found or invalid" );
		RCDtree rc = null;
		if( nodelistrcdtree.getLength() == 1 ) rc = readxmlrcdtree( (Element) nodelistrcdtree.item( 0 ), bn, settings, scalar, useKB, fileSelected );

		return rc;
	}

	/** @since 022703 */
	public RCDtree readxmlrcdtree( Element eltRCDtree, BeliefNetwork bn, Settings settings, double scalar, boolean useKB, File fileSelected ) throws Exception
	{
		RC.RCCreationParams rcparam = new RC.RCCreationParams();
		{
			rcparam.scalar = scalar;
			rcparam.useKB = useKB;
			rcparam.allowKB = true;
			rcparam.bn = bn;
		}

		RCDtree tree = new RCDtree( rcparam);

		Map mapRCNodetoDouble = new HashMap();
		Collection roots = readxmlroots( tree, eltRCDtree, bn, mapRCNodetoDouble, null );

		if( roots.size() == (int)1 )
		{
			RCNode root = (RCNode)roots.iterator().next();
			tree.setRoot( root, true );
			Settings.CACHE_SCHEME_DFBnB.setCacheFactor( (double)1 );
			Settings.CACHE_SCHEME_DFBnB.allocateMemory( tree, null );
			if( settings != null && tree != null ) settings.setRC( tree, fileSelected );
			setCacheFactors( tree, mapRCNodetoDouble );
			if( settings != null ) settings.refresh( tree );
			return tree;
		}
		else return null;
	}

	/** @since 022703 */
	public RCDgraph readxmlrcdgraph( Element eltRCDgraph, BeliefNetwork bn, Settings settings, boolean useKB, File fileSelected ) throws Exception
	{
		RC.RCCreationParams rcparam = new RC.RCCreationParams();
		{
			rcparam.scalar = 1.0;
			rcparam.useKB = useKB;
			rcparam.allowKB = true;
			rcparam.bn = bn;
		}

		RCDgraph graph = new RCDgraph( rcparam );

		Map mapRCNodetoDouble = new HashMap();
		Map varToRoot = new HashMap();
		Collection roots = readxmlroots( graph, eltRCDgraph, bn, mapRCNodetoDouble, varToRoot );

		//System.out.println( "Xmlizer.readxmlrcdgraph() read varToRoot: " + varToRoot );
		if( varToRoot.isEmpty() ) System.err.println( "Warning: Xmlizer.readxmlrcdgraph() failed to read varToRoot." );

		if( roots.isEmpty() ) return null;
		else
		{
			String strcanComputeFamilyMarginals = eltRCDgraph.getAttribute( STR_ATTR_CANCOMPUTE_NAME );
			boolean canComputeFamilyMarginals = Boolean.valueOf( strcanComputeFamilyMarginals ).booleanValue();
			graph.setRoots( roots, true/*callInit*/, varToRoot, canComputeFamilyMarginals );
			Settings.CACHE_SCHEME_DFBnB.setCacheFactor( (double)1 );
			Settings.CACHE_SCHEME_DFBnB.allocateMemory( graph, null );
			//if( graph != null ) settings.updateOptimal( graph.numCacheEntries_All() );
			if( graph != null ) settings.setRC( graph, fileSelected );
			setCacheFactors( graph, mapRCNodetoDouble );
			settings.refresh( graph );
			return graph;
		}
	}

	/** @since 031403 */
	protected Map makeMapVariableToRoot( Collection roots, BeliefNetwork bn )
	{
		if( roots.isEmpty() ) return null;

		int numRoots = roots.size();
		if( bn.size() != numRoots ) throw new IllegalStateException();

		Map ret = new HashMap();

		if( numRoots < (int)3 )
		{
			Iterator iteratorNetwork = bn.iterator();
			Iterator iteratorRoots = roots.iterator();
			while( iteratorNetwork.hasNext() && iteratorRoots.hasNext() )
			{
				ret.put( iteratorNetwork.next(), iteratorRoots.next() );
			}

		}
		else
		{
			RCNodeInternalBinaryCache nextRoot;
			for( Iterator it = roots.iterator(); it.hasNext(); )
			{
				nextRoot = (RCNodeInternalBinaryCache)it.next();
				ret.put( getVariableForRoot( nextRoot ), nextRoot );
			}
		}

		return ret;
	}

	/** @since 031403 */
	protected FiniteVariable getVariableForRoot( RCNodeInternalBinaryCache root )
	{
		RCNode next;
		RCNode leaf = null;
		//if( root.left().isLeaf() ) leaf = root.left();
		//else if( root.right().isLeaf() ) leaf = root.right();
		//else throw new IllegalArgumentException();
		for( RCIterator it = root.childIterator(); it.hasNext(); )
		{
			next = it.nextNode();
			if( next.isLeaf() )
			{
				leaf = next;
				break;
			}
		}

		if( leaf == null ) throw new IllegalArgumentException();

		return ((RCNodeLeaf)leaf).getLeafVar();
	}

	/** @since 022703 */
	protected void setCacheFactors( RC rc, Map mapRCNodetoDouble )
	{
		RCNode nextNode;
		Double factor;
		for( RCIterator it = rc.getIterator(); it.hasNext(); )
		{
			nextNode = it.nextNode();
			if( !nextNode.isLeaf() )
			{
				factor = (Double) mapRCNodetoDouble.get( nextNode );
				((RCNodeInternalBinaryCache)nextNode).changeCacheFactor( factor.doubleValue() );
			}
		}
	}

	/** @since 022703 */
	protected void recSetCacheFactor( RCNode root, Map mapRCNodetoDouble )
	{
		if( !root.isLeaf() )
		{
			RCNodeInternalBinaryCache internal = (RCNodeInternalBinaryCache)root;
			Double factor = (Double) mapRCNodetoDouble.get( internal );
			internal.changeCacheFactor( factor.doubleValue() );
			//recSetCacheFactor( internal.left(), mapRCNodetoDouble );
			//recSetCacheFactor( internal.right(), mapRCNodetoDouble );
			for( RCIterator it = internal.childIterator(); it.hasNext(); )
			{
				recSetCacheFactor( it.nextNode(), mapRCNodetoDouble );
			}
		}
	}

	/** @since 022703 */
	protected Collection readxmlroots( RC rc, Element eltRC, BeliefNetwork bn, Map mapRCNodetoDouble, Map varToRoot ) throws Exception
	{
		NodeList nodelistRCNodes = eltRC.getElementsByTagName( STR_ELT_RCNODE_NAME );
		Map mapIDtoRCNode = new HashMap();
		Collection roots = new LinkedList();

		String strListMarginalVars;
		if( varToRoot != null )
		{
			strListMarginalVars = eltRC.getAttribute( STR_ATTR_VARSNULLROOT_NAME );
			addVarToRootMappings( varToRoot, bn, null, strListMarginalVars );
		}

		Element eltRCNode;
		String strNodeID;
		String strFactor;
		double factor;
		String strIDleftChild;
		String strIDrightChild;
		String strListChildren;
		RCNode[] arrayChildren;
		RCNode rcnodeLeft;
		RCNode rcnodeRight;
		RCNodeInternalBinaryCache internal;
		String strIDLeafVar;
		RCNodeLeaf leaf;
		FiniteVariable fVarLeaf;
		int len = nodelistRCNodes.getLength();
		for( int i=0; i<len; i++ )
		{
			eltRCNode = (Element) nodelistRCNodes.item(i);
			strNodeID = eltRCNode.getAttribute( STR_ATTR_ID_NAME );

			if( eltRCNode.hasAttribute( STR_ATTR_FACTOR_NAME ) )
			{
				strFactor = eltRCNode.getAttribute( STR_ATTR_FACTOR_NAME );
				factor = Double.parseDouble( strFactor );

				if( eltRCNode.hasAttribute( STR_ATTR_LEFT_NAME ) )
				{
					strIDleftChild = eltRCNode.getAttribute( STR_ATTR_LEFT_NAME );
					strIDrightChild = eltRCNode.getAttribute( STR_ATTR_RIGHT_NAME );
					rcnodeLeft = (RCNode) mapIDtoRCNode.get( strIDleftChild );
					rcnodeRight = (RCNode) mapIDtoRCNode.get( strIDrightChild );

					if( rcnodeLeft == null || rcnodeRight == null ) throw new Exception( "failed to create internal node, missing child " + strIDleftChild + " or " + strIDrightChild );

					internal = new RCNodeInternalBinaryCache( rc, rcnodeLeft, rcnodeRight );
				}
				else throw new Exception( "failed to create internal node "+strNodeID+", missing child definition" );

				//internal.changeCacheFactor( factor );
				mapRCNodetoDouble.put( internal, new Double( factor ) );
				mapIDtoRCNode.put( strNodeID, internal );

				if( strNodeID.startsWith( STR_PREFIX_ID_ROOT ) )
				{
					if( varToRoot != null )
					{
						strListMarginalVars = eltRCNode.getAttribute( STR_ATTR_MARGINALVARS_NAME );
						addVarToRootMappings( varToRoot, bn, internal, strListMarginalVars );
					}
					roots.add( internal );
				}
			}
			else
			{
				strIDLeafVar = eltRCNode.getAttribute( STR_ELT_VARIABLE_NAME );
				fVarLeaf = (FiniteVariable) bn.forID( strIDLeafVar );
				if( fVarLeaf == null ) throw new Exception( "variable " +strIDLeafVar+ " not found in beliefnetwork" );
				else
				{
					leaf = new RCNodeLeaf( rc, fVarLeaf);
					mapIDtoRCNode.put( strNodeID, leaf );
				}
			}
		}

		return roots;
	}

	/** @since 060303 */
	static protected void addVarToRootMappings( Map varToRoot, BeliefNetwork bn, RCNodeInternalBinaryCache root, String strListMarginalVars )
	{
		if( strListMarginalVars == null ) return;

		StringTokenizer toker = new StringTokenizer( strListMarginalVars, STR_DELIMITERS );

		String strCurrent;
		Variable var;
		while( toker.hasMoreTokens() )
		{
			strCurrent = toker.nextToken();
			var = bn.forID( strCurrent );
			if( var != null ) varToRoot.put( var, root );
		}
	}

	/** @since 051203 */
	protected RCNode[] createChildArray( String strListChildren, Map mapIDtoRCNode ) throws Exception
	{
		StringTokenizer toker = new StringTokenizer( strListChildren, STR_DELIMITERS );
		RCNode[] ret = new RCNode[ toker.countTokens() ];

		String strCurrent;
		RCNode nodeCurrent;
		int index = (int)0;
		while( toker.hasMoreTokens() )
		{
			strCurrent = toker.nextToken();
			nodeCurrent = (RCNode) mapIDtoRCNode.get( strCurrent );
			if( nodeCurrent == null ) throw new Exception( "cannot find child " + strCurrent );
			else ret[index] = nodeCurrent;
			++index;
		}

		return ret;
	}

	/** @since 022703 */
	public static Document readxml( File fileSelected )
	{
		Document document = null;
		DocumentBuilderFactory factory = null;
		DocumentBuilder builder = null;

		factory = DocumentBuilderFactory.newInstance();
		if( FLAG_VALIDATE ) factory.setValidating( true );

		Throwable caught = null;
		try {
			builder = factory.newDocumentBuilder();
			//System.out.println( "builder.getClass() == " + builder.getClass().getName() );
			document = builder.parse( fileSelected );
		}catch( ParserConfigurationException e ){
			if( Definitions.DEBUG ) e.printStackTrace();
			caught = e;
		}catch (SAXParseException spe) {
			if( Definitions.DEBUG ) spe.printStackTrace();
			caught = spe;
		}catch (SAXException se) {
			if( Definitions.DEBUG ) se.printStackTrace();
			caught = se;
		}catch (IOException ioe) {
			if( Definitions.DEBUG ) ioe.printStackTrace();
			caught = ioe;
		}

		if( caught != null )
		{
			System.err.println( "Failed to parse " + fileSelected.getPath() + ": " + caught.toString() );
		}

		return document;
	}

	/** @since 022603 */
	public static void writeXML( Document document, File fileSelected )
	{
		//System.out.println( "Xmlizer.writeXML()" );

		Throwable caught = null;
		try {
			//document.normalize();

			//File stylesheet = new File( "batchoutput.xsl" );
			//StreamSource stylesource = new StreamSource(stylesheet);

			TransformerFactory tFactory = TransformerFactory.newInstance();
			//Transformer transformer = tFactory.newTransformer( stylesource );
			Transformer transformer = tFactory.newTransformer();
			transformer.setOutputProperty( OutputKeys.INDENT, "yes" );

			DocumentType docType = document.getDoctype();
			if( docType != null )
			{
				transformer.setOutputProperty( OutputKeys.DOCTYPE_SYSTEM, docType.getSystemId() );
				//transformer.setOutputProperty( OutputKeys.DOCTYPE_PUBLIC, "batch" );
			}

			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult( new FileWriter( fileSelected ) );
			transformer.transform(source, result);
			result.getWriter().close();
		}
		catch (TransformerConfigurationException tce) {
			caught = tce;
			if (tce.getException() != null)
			caught = tce.getException();
			if( Definitions.DEBUG ) caught.printStackTrace();
		}catch (TransformerException te) {
			caught = te;
			if (te.getException() != null)
			caught = te.getException();
			if( Definitions.DEBUG ) caught.printStackTrace();
		}catch( IOException ioe ){
			if( Definitions.DEBUG ) ioe.printStackTrace();
			caught = ioe;
		}

		if( caught != null )
		{
			System.err.println( "Failed to write " + fileSelected.getPath() + ": " + caught.toString() );
		}
	}
}
