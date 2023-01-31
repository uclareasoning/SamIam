package il2.bridge;
import edu.ucla.belief.io.NetworkIO;
import il2.util.*;
import il2.model.*;
import java.io.*;
import java.util.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class IO{

    public static BayesianNetwork readNetwork(String file) throws Exception{
	BayesianNetwork result=new Converter().convert(NetworkIO.read(file));
	return result.ensureNormalized();
    }
    private static final String CASE_LIST_TAG="CASELIST";
    private static final String EVIDENCE_TAG="CASE";
    private static final String VAR_VAL_TAG="OBSERVE";
    private static final String VAR_TAG="var";
    private static final String VAL_TAG="val";

    public static IntMap[] readEvidenceCases(String file,Domain d) throws Exception{
	DocumentBuilderFactory fact=DocumentBuilderFactory.newInstance();
	fact.setIgnoringComments(true);
	fact.setIgnoringElementContentWhitespace(true);
	Document doc=fact.newDocumentBuilder().parse(new File(file));
	Element e=doc.getDocumentElement();
	return parseEvidenceCases(e,d);
    }
    private static IntMap[] parseEvidenceCases(Element e,Domain d) throws Exception{
	NodeList evidenceCases=e.getElementsByTagName(EVIDENCE_TAG);
	IntMap[] result=new IntMap[evidenceCases.getLength()];
	for(int i=0;i<result.length;i++){
	    result[i]=parseEvidence(evidenceCases.item(i),d);
	}
	return result;
    }

    private static IntMap parseEvidence(Node n,Domain d) throws Exception{
	NodeList entries=n.getChildNodes();
	IntMap result=new IntMap(entries.getLength());
	for(int i=0;i<entries.getLength();i++){
	    Node obs=entries.item(i);
	    if(!VAR_VAL_TAG.equals(obs.getNodeName())){
		continue;
	    }
	    NamedNodeMap nm=obs.getAttributes();
	    String var=nm.getNamedItem(VAR_TAG).getNodeValue();
	    String val=nm.getNamedItem(VAL_TAG).getNodeValue();
	    int ivar=d.index(var);
	    int ival=d.instanceIndex(ivar,val);
	    result.put(ivar,ival);
	}
	return result;
    }


    public static void writeEvidenceCases(String file,Domain d,IntMap[] evidenceCases) throws Exception{
	
	Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
	Element root=doc.createElement(CASE_LIST_TAG);
	doc.appendChild(root);
		
	for(int i=0;i<evidenceCases.length;i++){
	    root.appendChild(createEvidenceNode(doc,d,evidenceCases[i]));
	}
	TransformerFactory tranFactory = TransformerFactory.newInstance();
	Transformer aTransformer = tranFactory.newTransformer();
	aTransformer.setOutputProperty(OutputKeys.INDENT,"yes");
	Source src = new DOMSource(doc);
	Result dest = new StreamResult(new File(file));
	aTransformer.transform(src,dest);
    }

    private static Element createEvidenceNode(Document doc, Domain d,IntMap e){
	Element node=doc.createElement(EVIDENCE_TAG);
	for(int i=0;i<e.size();i++){
	    int ivar=e.keys().get(i);
	    String var=d.name(ivar);
	    String val=d.instanceName(ivar,e.values().get(i));
	    Element varValNode=doc.createElement(VAR_VAL_TAG);
	    varValNode.setAttribute(VAR_TAG,var);
	    varValNode.setAttribute(VAL_TAG,val);
	    node.appendChild(varValNode);
	}
	return node;
    }
}
