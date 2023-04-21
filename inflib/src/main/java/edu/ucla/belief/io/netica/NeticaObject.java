package edu.ucla.belief.io.netica
;
import java.util.*;
import edu.ucla.belief.*;
import edu.ucla.belief.io.*;

public class NeticaObject {
    String name;
    String type;
    List superTypeNames;
    List superTypeObjects;
    List children;
    Map definitions;
    Map entries;
    public NeticaObject() {
        superTypeNames = new ArrayList();
        definitions = new HashMap();
        entries = new HashMap();
        children = new ArrayList();
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setType(String type) {
        this.type = type;
    }
    public void setSupers(List types) {
        this.superTypeNames = types;
    }
    public void initializeInheritance(Map parentDefinitions) {
        superTypeObjects = new ArrayList(superTypeNames.size());
        for (int i = 0; i < superTypeNames.size(); i++) {
            superTypeObjects.add(
                    parentDefinitions.get(superTypeNames.get(i)));
        }
        Map newMap = new HashMap(parentDefinitions);
        newMap.putAll(definitions);
        for (Iterator iter = definitions.values().iterator();
                iter.hasNext();) {
            ((NeticaObject) iter.next()).initializeInheritance(newMap);
        }
        for (int i = 0; i < children.size(); i++) {
            ((NeticaObject) children.get(i)).initializeInheritance(
                    parentDefinitions);
        }
    }
    public Object getValue(String key) {
        Object result = entries.get(key);
        if (result != null) {
            return result;
        }
        for (int i = 0; i < superTypeObjects.size(); i++) {
            result = ((NeticaObject) superTypeObjects.get(i)).getValue(key);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
    public void addChild(NeticaObject obj) {
        children.add(obj);
    }
    public void addDefinition(NeticaObject obj) {
        definitions.put(obj.name, obj);
    }
    public void addEntry(String name, Object value) {
        entries.put(name, value);
    }
    public static BeliefNetwork makeNetwork(NeticaObject net) {
        if (!net.type.equals("bnet")) {
            throw new IllegalArgumentException("Not a bnet node");
        }
        Map nameToFV = new HashMap();
        Map fvToPotential = new HashMap();
        for (Iterator iter = net.children.iterator(); iter.hasNext();) {
            NeticaObject child = (NeticaObject) iter.next();
            if (child.type.equals("node")) {
                makePotential(child, nameToFV, fvToPotential);
            }
        }
        return new BeliefNetworkImpl(fvToPotential);
    }
    private static void makePotential(NeticaObject node, Map nameToFv,
            Map fvToPotential) {
        List states = (List) node.getValue("states");
        FiniteVariable fv = new FiniteVariableImpl(node.name, states.toArray());
        List parents = (List) node.getValue("parents");
        List variables = new ArrayList(parents.size() + 1);
        for (int i = 0; i < parents.size(); i++) {
            variables.add(nameToFv.get(parents.get(i)));
        }
        variables.add(fv);
        nameToFv.put(node.name, fv);
        double[] values = ListConverter.toDoubleArray(
                ListConverter.flatten((List) node.getValue("probs")));
        fvToPotential.put(fv, new Table(new TableIndex(variables), values));
    }
}
