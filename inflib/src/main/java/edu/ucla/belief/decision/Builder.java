package edu.ucla.belief.decision;

import edu.ucla.belief.io.*;
import edu.ucla.belief.*;
import edu.ucla.util.AbstractStringifier;

//Add these lines for the exceptions that can be thrown when the XML document is parsed:
//{superfluous} import org.xml.sax.SAXException;
//{superfluous} import org.xml.sax.SAXParseException;
//{superfluous} import javax.xml.parsers.ParserConfigurationException;
//{superfluous} import javax.xml.parsers.SAXParser;
//{superfluous} import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.helpers.*;
import org.xml.sax.Attributes;

//{superfluous} import java.io.File;
//{superfluous} import java.io.IOException;

import java.util.*;

/** @author Keith Cascio
	@since 020205 */
public class Builder
{
	public Builder( BeliefNetwork bn, Attributes attributes ){
		this.bn = bn;
		this.strJoint = attributes.getValue( XmlWriter.STR_ATTR_JOINT );
		this.strParentOrder = attributes.getValue( XmlWriter.STR_ATTR_PARENTORDER );
		this.strEpsilon = attributes.getValue( XmlWriter.STR_ATTR_EPSILON );
		//this.root = this.current = this.parent = (DecisionNode) null;
		init();
	}

	public DecisionInternal internal( Attributes attr ){
		if( attr == null ){
			return (DecisionInternal) up( DecisionInternal.class );
		}
		DecisionInternal node;
		//= (DecisionInternal) forSerial( attr );
		//if( node == null ){
			String id = attr.getValue( XmlWriter.STR_ATTR_ID );
			String strEditable = attr.getValue( XmlWriter.STR_ATTR_EDITABLE );
			String strVariableID = attr.getValue( XmlWriter.STR_ATTR_VARIABLE );
			if( (id==null) || (strEditable==null) || (strVariableID==null) ) throw new IllegalStateException( "incomplete information for serial " + attr.getValue( XmlWriter.STR_ATTR_SERIAL ) );
			boolean editable = strEditable.equals( "true" );//Boolean.parseBoolean( strEditable );
			FiniteVariable variable = (FiniteVariable) mapVariableIDsToVariables.get( strVariableID );
			if( variable == null ) throw new IllegalStateException( "variable " + strVariableID + " not found" );
			node = factory.newInternal( variable );
			node.setID( id );
			node.setEditable( editable );
			map( attr, node );
		//}
		List instances = instanceList( attr, instancelist );
		if( (instances == null) && (root != null) ) throw new IllegalStateException( "missing instance list for serial " + attr.getValue( XmlWriter.STR_ATTR_SERIAL ) );
		down( node, instances );
		return node;
	}

	public DecisionLeaf leaf( Attributes attr ){
		if( attr == null ){
			return (DecisionLeaf) up( DecisionLeaf.class );
		}
		DecisionLeaf node;
		//= (DecisionLeaf) forSerial( attr );
		//if( node == null ){
			String id = attr.getValue( XmlWriter.STR_ATTR_ID );
			String strEditable = attr.getValue( XmlWriter.STR_ATTR_EDITABLE );
			if( (id==null) || (strEditable==null) ) throw new IllegalStateException( "incomplete information for serial " + attr.getValue( XmlWriter.STR_ATTR_SERIAL ) );
			boolean editable = strEditable.equals( "true" );//Boolean.parseBoolean( strEditable );
			node = factory.newLeaf( index.getJoint() );
			node.setID( id );
			node.setEditable( editable );
			map( attr, node );
		//}
		List instances = instanceList( attr, instancelist );
		if( (instances == null) && (root != null) ) throw new IllegalStateException( "missing instance list for serial " + attr.getValue( XmlWriter.STR_ATTR_SERIAL ) );
		down( node, instances );
		return node;
	}

	public DecisionNode generic( Attributes attr ){
		if( attr == null ){
			return (DecisionNode) up( DecisionNode.class );
		}
		DecisionNode node = (DecisionNode) forSerial( attr );
		if( node == null ) throw new IllegalStateException( "node not found for serial " + attr.getValue( XmlWriter.STR_ATTR_SERIAL ) );
		List instances = instanceList( attr, instancelist );
		if( instances == null ) throw new IllegalStateException( "missing instance list for serial " + attr.getValue( XmlWriter.STR_ATTR_SERIAL ) );
		down( node, instances );
		return node;
	}

	public Parameter parameter( Attributes attr ){
		if( attr == null ){
			return (Parameter) up( Parameter.class );
		}
		Parameter param = (Parameter) forSerial( attr );
		if( param == null ){
			String id = attr.getValue( XmlWriter.STR_ATTR_ID );
			String strValue = attr.getValue( XmlWriter.STR_ATTR_VALUE );
			if( (id==null) || (strValue==null) ) throw new IllegalStateException( "incomplete information for serial " + attr.getValue( XmlWriter.STR_ATTR_SERIAL ) );
			double value = Double.parseDouble( strValue );
			param = factory.newParameter( id, value );
			map( attr, param );
		}
		List instances = instanceList( attr, instancelist );
		int index = (int)-1;
		if( instances == null ){
			String strIndex = attr.getValue( XmlWriter.STR_ATTR_INDEX );
			if( strIndex == null ) throw new IllegalStateException( "missing instance list or index for serial " + attr.getValue( XmlWriter.STR_ATTR_SERIAL ) );
			index = Integer.parseInt( strIndex );
		}
		down( param, instances, index );
		return param;
	}

	private Object up( Class type ){
		Object pop = trace.removeLast();
		if( !type.isInstance( pop ) ) throw new IllegalArgumentException( "(" + pop.getClass().getName() + ")" + pop + " !instanceof " + type.getName() );
		return pop;
	}

	private void down( DecisionNode node, List instances ){
		if( trace.isEmpty() ){
			if( root != null ) throw new IllegalStateException( "multiple roots" );
			else if( instances != null ) throw new IllegalStateException();
			else trace.addLast( root = node );
			//System.out.println( "root trace node " + root.toString() );
			return;
		}
		else if( instances == null ) throw new IllegalStateException();

		DecisionInternal internal = (DecisionInternal) trace.getLast();

		try{
			for( Iterator it = instances.iterator(); it.hasNext(); ){
				internal.setNext( it.next(), node );
			}
		}catch( StateNotFoundException statenotfoundexception ){
			throw new IllegalStateException( statenotfoundexception.getMessage() );
		}

		trace.addLast( node );
		//System.out.println( "add trace node " + node.toString() );
	}

	private void down( Parameter param, List instances, int index ){
		if( trace.isEmpty() ) throw new IllegalStateException();
		DecisionLeaf leaf = (DecisionLeaf) trace.getLast();
		if( instances != null ){
			try{
				for( Iterator it = instances.iterator(); it.hasNext(); ){
					leaf.setParameter( it.next(), param );
				}
			}catch( StateNotFoundException statenotfoundexception ){
				throw new IllegalStateException( statenotfoundexception.getMessage() );
			}
		}
		else if( index >= 0 ){
			leaf.setParameter( index, param );
		}
		else throw new IllegalArgumentException();

		trace.addLast( param );
		//System.out.println( "add trace parameter " + param.getID() );
	}

	public Object forSerial( Attributes attributes ){
		String serial = attributes.getValue( XmlWriter.STR_ATTR_SERIAL );
		if( serial == null ) return null;
		return mapSerialToObject.get( serial );
	}

	public void map( Attributes attributes, Object obj ){
		String serial = attributes.getValue( XmlWriter.STR_ATTR_SERIAL );
		if( serial == null ) return;
		mapSerialToObject.put( serial, obj );
	}

	public List instanceList( Attributes attributes, List instances ){
		String strList = attributes.getValue( XmlWriter.STR_ATTR_INSTANCELIST );
		if( strList == null ) return null;
		StringTokenizer toker = new StringTokenizer( strList, STR_DELIMITERS );
		//ArrayList ret = new ArrayList( toker.countTokens() );
		instances.clear();
		while( toker.hasMoreTokens() ){
			instances.add( toker.nextToken() );
		}
		return instances;
	}

	private void init(){
		this.trace = new LinkedList();
		createIndex();
		this.factory = this.tree = new DecisionTreeImpl( index );
		mapSerialToObject = new HashMap();
		instancelist = new ArrayList();
	}

	private void createIndex(){
		List variables = new LinkedList();
		mapVariableIDsToVariables = new HashMap();

		StringTokenizer toker = new StringTokenizer( strParentOrder, STR_DELIMITERS );

		String id;
		Variable variable;
		while( toker.hasMoreTokens() ){
			id = toker.nextToken();
			variable = bn.forID( id );
			if( variable == null ) throw new IllegalStateException( "variable " +id+ " not found" );
			variables.add( variable );
			mapVariableIDsToVariables.put( id, variable );
		}

		index = new TableIndex( variables );
	}

	public DecisionTreeImpl result(){
		if( root == null ) return null;
		tree.initRoot( root );
		return tree;
	}

	public DecisionNode getRoot(){
		return root;
	}

	public static final String STR_DELIMITERS = new String( new char[] { AbstractStringifier.CHAR_SEPARATOR } );

	private BeliefNetwork bn;
	private TableIndex index;
	private DecisionNode root;
	//private DecisionNode current;
	//private DecisionNode parent;
	private LinkedList trace;
	private Parameter parameter;
	private String strJoint;
	private String strParentOrder;
	private String strEpsilon;
	private Map mapVariableIDsToVariables;
	private Map mapSerialToObject;
	private Factory factory;
	private DecisionTreeImpl tree;
	private List instancelist;
}