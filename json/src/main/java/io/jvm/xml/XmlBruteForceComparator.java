package io.jvm.xml;

import static java.lang.System.out;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A comparator for {@link org.w3c.dom.Element}, compares two XML subtrees. The
 * subtrees are considered equivalent if they contain the same nodes, having the
 * same attributes and values. The order of elements is irrelevant.
 * 
 * For each of the two trees the compiler builds a list of paths root-to-leaf,
 * and compares the two lists.
 * 
 * Two nodes are considered equal if their names and values are equal, and they
 * contain the same attributes.
 * 
 * Two attributes are equal if their names and values are equal.
 */
public class XmlBruteForceComparator implements Comparator<Element> {

    private List<List<Node>> xmlPaths_lhs = new ArrayList<List<Node>>();
    private List<List<Node>> xmlPaths_rhs = new ArrayList<List<Node>>();

    @Override
    public int compare(Element lhs, Element rhs) {

	buildPaths(xmlPaths_lhs, null, (Node) lhs);
	buildPaths(xmlPaths_rhs, null, (Node) rhs);
	
	return compareAllPaths(xmlPaths_lhs, xmlPaths_rhs);
    }

    /**
     * Recursively builds a list of all path from root to leaf
     * 
     * @param allPaths
     *            The list containing all paths for the current tree
     * @param pathUpToNode
     *            The current path being built; {@code null} in the first step
     * @param node
     *            The current node on the path; root in the first step, a leaf
     *            in the last step
     */
    private void buildPaths(List<List<Node>> allPaths, List<Node> pathUpToNode, Node node) {
	if (pathUpToNode == null)
	    pathUpToNode = new ArrayList<Node>();

	pathUpToNode.add(node);	
	
	if (node.hasChildNodes()) {
	    for (Node child : getListOfChildren(node)) {
		buildPaths(allPaths, new ArrayList<Node>(pathUpToNode), child);		
	    }
	} else {	    
	    allPaths.add(new ArrayList<Node>(pathUpToNode));
	    pathUpToNode.clear();
	}
    }

    /**
     * Compares all root-to-leaf paths of the two XML trees.
     * 
     * For each path in lhs list find an equivalent path in the rhs list, and
     * remove it if it exists.
     * 
     * If for every path in lhs a unique equivalent path in rhs is found, the
     * trees are considered equal.
     * 
     * @param lhs
     *            The lhs XML tree paths
     * @param rhs
     *            The rhs XML tree paths
     * @return {@code 0} if they are equal {@code -1} otherwise
     */
    private int compareAllPaths(List<List<Node>> lhs, List<List<Node>> rhs) {
	if (lhs.size() != rhs.size())
	    return -1;	
	
	for (List<Node> leftPath : lhs) {
	    boolean found = false;
	    for (List<Node> rightPath : rhs) {
		if (nodeListsEqual(leftPath, rightPath)) {
		    found = true;
		    rhs.remove(rightPath);				   
		    
		    break;
		}
	    }
	    if (!found){
		out.println(leftPath);
		return -1;
	    }
	}

	return 0;
    }

    /**
     * Compares two lists of nodes.
     * 
     * The lists are considered equal if their nodes are equal. Since they
     * represent paths in a tree, their ordering is relevant.
     * 
     * @param lhs
     *            The lhs List<Node>
     * @param rhs
     *            The rhs List<Node>
     * @return true if the {@code lists} are equal, {@code false} otherwise
     * @throws InterruptedException 
     */
    private boolean nodeListsEqual(List<Node> lhs, List<Node> rhs) {	
	
	if (lhs.size() != rhs.size())
	    return false;	

	for (int i = 0; i < lhs.size(); i++) {

	    Node node1 = lhs.get(i);
	    Node node2 = rhs.get(i);

	    if (!nodesEqual(node1, node2)) {		
		return false;
	    }
	}

	return true;
    }

    private boolean nodesEqual(Node node1, Node node2) {
	if (node1 == null && node2 == null)
	    return true;
	else if (node1.getNodeName() == null && node2.getNodeName() == null)
	    return true;
	else if (node1.hasAttributes() != node2.hasAttributes())
	    return false;
	else if ((node1.getNodeValue() != null && node2.getNodeValue() != null)
		&& !node1.getNodeValue().equals(node2.getNodeValue()))
	    return false;
	else if (node1.hasAttributes() && node2.hasAttributes()
		&& (node1.getAttributes().getLength() != node2.getAttributes().getLength()))
	    return false;
	else if (node1.getChildNodes().getLength() != node2.getChildNodes().getLength())
	    return false;
	else if (!node1.getNodeName().equals(node2.getNodeName()))
	    return false;
	else if (!nodesHaveEqualAttributes(node1, node2))
	    return false;

	return true;
    }

    private boolean nodesHaveEqualAttributes(Node node1, Node node2) {
	if (node1.hasAttributes() != node2.hasAttributes())
	    return false;
	else if (node1.hasAttributes() == false && node2.hasAttributes() == false)
	    return true;
	else if (node1.getAttributes().getLength() != node2.getAttributes().getLength())
	    return false;
	else {

	    for (Attr attr1 : getListOfAttributes(node1)) {
		boolean found = false;
		for (Attr attr2 : getListOfAttributes(node2)) {
		    if (equalsWithNull(attr1.getName(), attr2.getName())
			    && equalsWithNull(attr1.getValue(), attr2.getValue())) {
			found = true;
			break;
		    }
		}
		if (!found)
		    return false;
	    }
	}

	return true;
    }

    private List<Attr> getListOfAttributes(Node node) {
	List<Attr> aListOfAttributes = new ArrayList<Attr>();

	for (int i = 0; i < node.getAttributes().getLength(); i++)
	    aListOfAttributes.add((Attr) node.getAttributes().item(i));

	return aListOfAttributes;
    }

    private List<Node> getListOfChildren(Node node) {
	List<Node> nodes = new ArrayList<Node>();

	NodeList nodeList = node.getChildNodes();

	for (int i = 0; i < nodeList.getLength(); i++)
	    nodes.add(nodeList.item(i));

	return nodes;
    }

    private boolean equalsWithNull(Object o1, Object o2) {
	if (o1 == null && o2 == null)
	    return true;
	else if (o1 == null || o2 == null)
	    return false;
	else
	    return o1.equals(o2);
    }

}
