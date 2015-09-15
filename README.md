# xml parser
a simple xml parser in java

### how to use
create a new parser and load source.
variable xml is a string with xml tags.

    XmlParser xp = new XmlParser();
    xp.parseNode(xml)

then one check nodes either by xpath

	XmlPaser.Node n = xp.findNodeViaXpath("//parentNode/find/childNode");
	if(n != null){
		// node exists
	}
	
	XmlPaser.Node n = xp.findNodeViaXpath("//parentNode/find/childNodes/node[2]");
	if(n != null){
		// node exists
	}

or regex

	//find first node with attribute 'id='
	XmlPaser.Node n = xp.findNodeViaAttributes(.*id=.*);
	if(n != null){
		// node exists
	}

### see test file for further examples