package com.bm;

// TODO if duplicates found? grab first?
import java.util.ArrayList;

public class XmlParser {
	public static final String FIND_BY_REGEX_TAG = "regex";
	public static final String FIND_BY_XPATH_TAG = "xpath";
	private XmlParser.Node nodes;
	private int offset = 1;
	private boolean locked = false;
	ArrayList<Node> foundMatchingNodes = new ArrayList<Node>();
	
	public XmlParser() {
		nodes = this.new Node("");
	}

	/**
	 * parse supplied xml string into a node tree
	 * 
	 * @param sml - xml string to be parsed
	 */
	public void parseNode(String xml) {
		if(!locked){
			// remove <?xml version=\"1.0\" encoding=\"UTF-8\"?>";
			if (xml.indexOf("<?xml") == 0) {
				xml = xml.substring(xml.indexOf(">") + 1);
			}
	
			nodes = this.new Node("");
			parseNode(xml, nodes);
			log("updated nodes");
		}else{
			log("parseNode() called, but node is locked since setNode() has been called. must call unLockNode() when done");
		}
	}


	public Node findChildNode(Node parent, String tag, String type){
		XmlParser.Node n = null;
		switch(type){
			case FIND_BY_XPATH_TAG:
				n = findChildNodeViaXpath(parent, tag);
				break;
			case FIND_BY_REGEX_TAG:
				n = findChildNodeViaAttributes(parent, tag);
				break;
		}
		return n;
	}
	
	//TODO refactor and make these private and the other once too
	/**
	 * finds a node that is a child of parent, will retrun first found.
	 * @param parent - the node to go down from
	 * @param regex - an regex that matches the nodes attributes in the xml
	 * @return returns found node or null
	 */
	public Node findChildNodeViaAttributes(Node parent, String regex) {
		ArrayList<Node> foundNodes = new ArrayList<Node>();
		findAllNodesWithAttributes(parent, regex, foundNodes);
		if(foundNodes.size() > 0){
			return foundNodes.get(0); // return first node found;
		}
		return null;
	}

	/**
	 * finds a node that is a child of parent, will retrun first found.
	 * @param parent - the node to go down from
	 * @param xPath - partial xpath from parent to node
	 * @return returns found node or null
	 */
	public Node findChildNodeViaXpath(Node parent, String xPath) {
		if (xPath.startsWith("//")) {
			xPath = xPath.substring(2, xPath.length());
			String[] xmlTags = xPath.split("/");
			String elemName = xmlTags[xmlTags.length - 1]; // TODO check outofbounds
			ArrayList<Node> foundNodes = new ArrayList<Node>();

			findAllNodes(parent, elemName, foundNodes);

			for (Node fn : foundNodes) {
				if(fn.getFullXpath().endsWith(xPath)){
					return fn;
				}
			}
		} else {
			// TODO report error
			System.out.println("xPath must be partial. e.g. not start with / but with //");
		}
		return null;
	}

	
	public Node findNode(String tag, String type){
		XmlParser.Node n = null;
		switch(type){
			case FIND_BY_XPATH_TAG:
				n = findNodeViaXpath(tag);
				break;
			case FIND_BY_REGEX_TAG:
				n = findNodeViaAttributes(tag);
				break;
		}
		return n;
	}
	
	public ArrayList<Node> getAllMatchingNodes(){
		return foundMatchingNodes;
	}
	
	//TODO refactor and make these private and the other once too
	/**
	 * finds a node with given regexp, will retrun first found.
	 * @param regex - an regex that matches the nodes attributes in the xml
	 * @return returns found node or null
	 */
	public Node findNodeViaAttributes(String regex) {
		ArrayList<Node> foundNodes = new ArrayList<Node>();
		findAllNodesWithAttributes(nodes, regex, foundNodes);
		foundMatchingNodes = foundNodes;
		
		int position = 1;
		for (Node fn : foundNodes) {
			if(position == offset && fn != null){
				return fn;
			}
			position++;
		}
		return null;
	}
	
	/**
	 * finds a node via xpath, searches from root of tree
	 * accepts full or partial xpaths
	 * @param xPath
	 * @return
	 */
	public Node findNodeViaXpath(String xPath) {
		XmlParser.Node result = null;
		if (xPath.startsWith("//")) {
			xPath = xPath.substring(2, xPath.length());
			String[] xmlTags = xPath.split("/");
			String elemName = xmlTags[xmlTags.length - 1];
			ArrayList<Node> foundNodes = new ArrayList<Node>();
			foundMatchingNodes = new ArrayList<Node>();
			findAllNodes(nodes, elemName, foundNodes);
			int position = 1;
			for (Node fn : foundNodes) {
				String fullXPath = fn.getFullXpath();
				if (fullXPath.endsWith(xPath) ) {
					if(position == offset && result == null){ // keep first found
						result = fn;
					}
					foundMatchingNodes.add(fn);
					position++;
				}
			}
		} else {
			return findNodeFullXpath(nodes, xPath);
		}
		return result;
	}

	public Node getNodeViaFullXpath(Node n, String xPath) {
		Node found = null;
		String[] xmlTags = new String[0];
		int tagCount = 0;

		if (xPath.startsWith("/")) {
			xPath = xPath.substring(1, xPath.length());
			xmlTags = xPath.split("/");

			String elemName = xmlTags[tagCount++];

			if (n.getName().equals(elemName) && xmlTags.length == 1) { // looking for root node?
				return n;
			}

			ArrayList<Node> c = n.getChildren();

			while (tagCount < xmlTags.length) {
				elemName = xmlTags[tagCount++];
				int pos = -1;
				found = null;
				if (elemName.contains("[") && elemName.contains("]")) {
					pos = Integer.parseInt(elemName.substring(elemName.indexOf("[") + 1, elemName.indexOf("]")));
					elemName = elemName.substring(0, elemName.indexOf("["));
				}
				int siblingsCount = 0;

				for (int xc = 0; xc < c.size(); xc++) {
					Node n1 = c.get(xc);
					if (n1.getName().equals(elemName)) {
						if ((pos == -1) || (pos != -1 && (pos - 1) == siblingsCount)) {
							found = n1;
							c = n1.getChildren();
							break;
						}
						siblingsCount++;
					}
				}
				if (found == null) {
					break;
				}
			}
		}
		if (tagCount != xmlTags.length) {
			found = null;
		}
		return found;
	}

	/**
	 * search for node with name from given node and then depth breadth first and store found nodes in provided ArrayList
	 * @param n - start node to search from
	 * @param name - name of node(s) to find
	 * @param foundNodes - array with found nodes
	 */
	private void findAllNodes(Node n, String name, ArrayList<Node> foundNodes) {
		if (name.contains("[") && name.contains("]")) {
			name = name.substring(0, name.indexOf("["));
		}
		if (n.getName().equals(name)) {
			foundNodes.add(n);
		}
		ArrayList<Node> c = n.getChildren();
		for (int x = 0; x < c.size(); x++) {
			Node n1 = c.get(x);
			findAllNodes(n1, name, foundNodes);
		}
	}

	private Node findNodeFullXpath(Node n, String xPath) {
		Node found = null;
		String[] xmlTags = new String[0];
		if (xPath.startsWith("/")) {
			xPath = xPath.substring(1, xPath.length());
			xmlTags = xPath.split("/");

			String elemName = xmlTags[xmlTags.length - 1];
			xPath = "/" + xPath;
			ArrayList<Node> foundNodes = new ArrayList<Node>();
			findAllNodes(n, elemName, foundNodes); // cache search from previous?
			for (Node fn : foundNodes) {
				String foundPath = fn.getFullXpath();
				if (foundPath.equals(xPath)) {
					found = getNodeViaFullXpath(n, foundPath);
					break;
				}
			}
		}
		return found;
	}

	private void findAllNodesWithAttributes(Node n, String regex, ArrayList<Node> foundNodes) {
		String nodeAttributes = n.getAttributes();

		if (nodeAttributes != null && nodeAttributes.matches(regex)) {
			foundNodes.add(n);
		}

		ArrayList<Node> c = n.getChildren();
		for (int x = 0; x < c.size(); x++) {
			Node n1 = c.get(x);
			findAllNodesWithAttributes(n1, regex, foundNodes);
		}
	}

	private void parseNode(String s, Node n) {
		if (s.indexOf("<?xml") == 0) {
			s = s.substring(s.indexOf(">") + 1);
		}
		parseToNode(s, n, 0);
	}

	private int parseToNode(String s, Node n, int x) {
		for (; x < s.length(); x++) {
			char c = s.charAt(x);
			if (c == '<') {
				char c1 = s.charAt(x + 1);
				if (c1 == '/') {
					n.setEndPos(x);
					return x;
				} else {
					String name = s.substring(x + 1, s.indexOf('>', x));
					if (x == 0) {
						n.setName(name);
						n.setStartPos(x);
					} else {
						Node newNode = new Node(name);
						newNode.setStartPos(x);
						n.addChild(newNode);
						x = parseToNode(s, newNode, ++x);
					}
				}
			} else if (c == '/') {
				char c1 = s.charAt(x + 1);
				if (c1 == '>') {
					n.setEndPos(x);
					return x;
				}
			}else{
				// grab text or non tag inside tag
			}
		}
		return x;
	}

	public XmlParser.Node setNode(XmlParser.Node node){
		locked = true;
		return this.nodes = node;
	}
	
	public void unLockNode(){
		locked = false;
	}
	
	public boolean isLockNode(){
		return locked;
	}
	
	public XmlParser.Node getNodes() {
		return nodes;
	}
	
	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}
	
	public void resetOffset() {
		this.offset = 1;
	}

	private void log(String m) {
		LOG.info(m);
	}

	public class Node {
		private String name;
		private String attributes;
		private Node parent;
		private int startPos;
		private int endPos;
		private ArrayList<Node> children;
		private String identifier = null;
		private double id = Math.random();

		public Node(String name) {
			this.name = parseName(name);
			parent = null;
			children = new ArrayList<Node>();
		}

		/**
		 * copies provided node into this node, it also copies the nodes children recursively down to last child
		 * 
		 * @param copy
		 */
		public Node(Node copy) {
			this.name = copy.name;
			this.attributes = copy.attributes;
			this.parent = copy.parent;
			this.startPos = copy.startPos;
			this.endPos = copy.endPos;
			this.children = new ArrayList<Node>();
			deepCloneChildArray(copy);
			this.identifier = copy.identifier;
		}

		/**
		 * checks if provided child has a parent if it has not, it get this as parent if it has, the node is coppied and the parent is set to this
		 * 
		 * @param child
		 * @return a node with this as parent
		 * @see deepCloneChildArray
		 */
		private Node checkParent(Node child) {
			Node checkChild = child;
			if (child.getParent() == null) {
				checkChild.setParent(this);
			} else if (child.getParent() != this) {
				Node c = new Node(child);
				c.setParent(this);
				checkChild = c;
			}
			return checkChild;
		}

		/**
		 * adds children to this children array
		 * 
		 * @param copy
		 */
		private void deepCloneChildArray(Node copy) {
			for (Node childCopy : copy.getChildren()) {
				addChild(childCopy);
			}
		}

		/**
		 * get the xpath to provided node by iterate up through its parents and stop at the provided toNode
		 * 
		 * @param n
		 *            - the node to get the xpath from
		 * @param toNode
		 *            - the node to stop at
		 * @return String - an xpath to provided child
		 */
		private String getFullXpath(Node n, Node toNode) {
			if (this.equals(toNode))
				return "";

			if (n.getParent() != null) {
				int positionInParent = 0;
				int siblings = 0;
				for (Node c : n.getParent().getChildren()) {
					boolean nameMatch = c.getName().equals(n.getName());

					if (c.getStartPos() == n.getStartPos() && c.getEndPos() == n.getEndPos() && nameMatch) {
						positionInParent = siblings + 1;
					} else if (nameMatch) {
						siblings++;
					}
				}

				if (siblings != 0) {
					return n.getFullXpath(n.getParent(), toNode) + "/" + n.getName() + "[" + positionInParent + "]";
				}
				return n.getFullXpath(n.getParent(), toNode) + "/" + n.getName();
			}
			return "/" + n.name;
		}

		/**
		 * get the xpath to provided node by iterate up through its parents and to the top node
		 * 
		 * @param n
		 *            - the node to get the xpath from
		 * @return String - an xpath to provided child
		 */
		private String getFullXpath(Node n) {

			if (n.getParent() != null) {
				int positionInParent = 0;
				int siblings = 0;

				// get siblings
				for (int x = 0; x < n.getParent().getChildren().size(); x++) {
					Node c = n.getParent().getChildren().get(x);
					boolean nameMatch = c.getName().equals(n.getName());
					if (c.equals(n)) {
						positionInParent = siblings + 1;
					} else if (nameMatch) {
						siblings++;
					}
				}

				if (siblings != 0) {
					return n.getFullXpath(n.getParent()) + "/" + n.getName() + "[" + positionInParent + "]";
				}
				return n.getFullXpath(n.getParent()) + "/" + n.getName();
			}
			return "/" + n.name;
		}

		/**
		 * parse the xml tag to this node and store its attributes if any
		 * 
		 * @param name
		 * @return
		 */
		private String parseName(String name) {
			name = name.trim();
			if (name.indexOf(" ") >= 0) {
				attributes = name.substring(name.indexOf(" "));
				if (attributes.lastIndexOf('/') == attributes.length() - 1) {
					attributes = attributes.substring(0, attributes.length() - 1);
				}
				attributes = attributes.trim();
				name = name.substring(0, name.indexOf(" "));
			}
			return name;
		}

		public void setName(String n) {
			name = parseName(n);
		}

		public String getName() {
			return name;
		}

		public double getId() {
			return id;
		}

		public void setParent(Node parent) {
			this.parent = parent;
		}

		public Node getParent() {
			return parent;
		}

		public void addChild(Node child) {
			Node c = checkParent(child);
			children.add(c);
		}

		public void addChildren(ArrayList<Node> newChildren) {
			for (Node newChild : newChildren) {
				if (!children.contains(newChild)) {
					addChild(newChild);
				}
			}
		}

		public boolean addChildUnique(Node newChild) {
			boolean result = false;
			if (!children.contains(newChild)) {
				addChild(newChild);
				result = true;
			}
			return result;
		}

		public ArrayList<Node> getChildren() {
			return children;
		}

		public void resetChildren() {
			children = new ArrayList<Node>();
		}

		public void setStartPos(int i) {
			startPos = i;
		}

		public int getStartPos() {
			return startPos;
		}

		public void setEndPos(int i) {
			endPos = i;
		}

		public int getEndPos() {
			return endPos;
		}

		public String getAttributes() {
			return attributes;
		}

		public String getAttribute(String attributeName) {
			String[] attribs = attributes.split("(?<=\"\\s)");
			for (String a : attribs) {
				if (a.startsWith(attributeName + "=")) {
					return a.replace(attributeName + "=", "").trim().replaceAll("\"", "");
				}
			}
			return null;
		}

		public String getFullXpath() {
			String x = getFullXpath(this);
			return x;
		}

		public String getFullXpathToNode(Node toNode) {
			return getFullXpath(this, toNode);
		}

		public String getUniqueXpath(){
			// TODO implement this, returns smalles unique xpath
			return "todo";
		}
		
		public void setIdentifier(String id) {
			identifier = id;
		}

		public String getIdentifier() {
			return identifier;
		}

		public void resetIdentifier() {
			identifier = null;
		}

		@Override
		public int hashCode() {
			int hash = 40;

			if (identifier != null) {
				hash = 11 + identifier.hashCode();
			}else{
				hash = hash * 11 + startPos;
				hash = hash * 17 + endPos;
				hash = hash * 31 + name.hashCode();
				hash = hash * 13 + attributes.hashCode();
			}
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Node)) {
				return false;
			}
			if (obj == this) {
				return true;
			}

			Node n = (Node) obj;

			if ((attributes != null && n.attributes == null) || (identifier == null && n.identifier != null)) {
				return false;
			}

			int hash = 40;

			if (identifier != null) {
				hash = 11 + identifier.hashCode();
			}else{
				hash = hash * 11 + startPos;
				hash = hash * 17 + endPos;
				hash = hash * 31 + name.hashCode();
				if (attributes != null) {
					hash = hash * 13 + attributes.hashCode();
				}
			}
			
			int hashCompare = 40;

			if (n.identifier != null) {
				hashCompare = 11 + n.identifier.hashCode();
			}else{
				hashCompare = hashCompare * 11 + n.startPos;
				hashCompare = hashCompare * 17 + n.endPos;
				hashCompare = hashCompare * 31 + n.name.hashCode();
				if (n.attributes != null) {
					hashCompare = hashCompare * 13 + n.attributes.hashCode();
				}
			}
			return hash == hashCompare;
		}

		public String toString() {
			String s = "";
			if (parent != null) {
				s = "parentNode: " + parent.getName() + "\n";
			}
			s += "name: " + name + "\n";
			s += "startPos: " + startPos + "\n";
			s += "endPos: " + endPos + "\n";
			s += "childens: " + children.size() + "\n";
			if (identifier != null) {
				s += "identifier: " + identifier + "\n";
			}
			if (attributes != null) {
				s += "attributes: " + attributes + "\n";
			}
			return s;
		}
	}
}
