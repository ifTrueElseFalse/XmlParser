package com.bm.test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.bm.XmlParser;

public class Testing {
	/**
	 * @param args
	 * @throws JSONException 
	 */
	public static void main(String args[]){
		// add find index and replace end from end not offset
		// TODO add test for get all nodes with xpath and attrib
		// TODO add test for get node with attribute but count 2, if exists. for children too
		XmlParser testXp = new XmlParser();
		String xml = "<root>"
						+ "<div attribute=\"one\">"
							+ "<div temp=\"\" />"
						+ "</div>"
						+ "<div attribute=\"one\">"
							+ "<list source-id=\"com.via:id/rootList\" attribute=\"value\">"
								+ "<item test=\"\" source-id=\"com.via:id/first_item\" attribute=\"value\"></item>"
								+ "<text></text>"
								+ "<item test=\"\" source-id=\"com.via:id/second_item\"></item>"
							+ "</list>"
							+ "<item someItemAttrib=\"value\" />"
						+ "</div>"
						+ "<div>test</div>"
						+ "<div>"
							+ "<list id=\"listWithChildren\">"
								+ "<row>"
									+ "<item>"
										+ "<i text=\"firstI\">"
										+ "</i>"
									+ "</item>"
								+ "</row>"
								+ "<row>"
									+ "<item>"
										+ "<i text=\"secondI\">"
										+ "</i>"
									+ "</item>"
								+ "</row>"
								+ "<row>"
									+ "<item>"
										+ "<i text=\"thirdI\">"
										+ "</i>"
									+ "</item>"
								+ "</row>"
							+ "</list>"
						+ "</div>"
						+"<div>"
							+"<android.widget.LinearLayout>"
								+ "<android.support.v7.app.ActionBar$Tab>"
									+ "<android.widget.TextView test=\"something\"></android.widget.TextView>"
								+ "</android.support.v7.app.ActionBar$Tab>"
							+ "</android.widget.LinearLayout>"
						+"</div>"
					+ "</root>";
		
		testXp.parseNode(xml);
		
		int xpath = 0;
		int attributes = 0;
		int addChild = 0;
		XmlParser.Node tn = testXp.findNodeViaXpath("/root");
		if(tn.getName().equals("root")){
			System.out.println("test root node ok");
			xpath++;
		}

		tn = testXp.findNodeViaXpath("/root/div[2]");
		if(tn.getChildren().size() == 2){
			System.out.println("test full with numbers at end ok");
			xpath++;
		}
		
		tn = testXp.findNodeViaXpath("/root/div[2]/list/item[1]");
		if(tn.getAttribute("attribute").equals("value")){
			System.out.println("test full with numbers ok");
			xpath++;
		}

		// need number
		tn = testXp.findNodeViaXpath("/root/div[2]/list/item");
		if(tn == null){
			System.out.println("test full missing number ok");
			xpath++;
		}
		
		// dont skip xpath
		tn = testXp.findNodeViaXpath("/root/div[2]/item[1]");
		if(tn == null){
			System.out.println("test full skipping node ok");
			xpath++;
		}
		
		tn = testXp.findNodeViaXpath("/root/div[2]/list/text");
		if(tn != null){
			System.out.println("test full with number ok");
			xpath++;
		}
		
		// partial
		tn = testXp.findNodeViaXpath("//div[2]");
		if(tn.getChildren().size() == 2){
			System.out.println("test partial single node number at end ok");
			xpath++;
		}
		
		tn = testXp.findNodeViaXpath("//list/item[1]");
		if(tn.getAttribute("attribute").equals("value")){
			System.out.println("test partial with number at end ok");
			xpath++;
		}
		
		tn = testXp.findNodeViaXpath("//list/item");
		if(tn == null){
			System.out.println("test partial skipped node ok");
			xpath++;
		}
		
		tn = testXp.findNodeViaXpath("//div[2]/list/text");
		if(tn != null){
			System.out.println("test partial with number ok");
			xpath++;
		}
		
		tn = testXp.findNodeViaXpath("//div[2]/text");
		if(tn == null){
			System.out.println("test partial skipped node in path ok");
			xpath++;
		}
		
		// attributes
		tn = testXp.findNodeViaAttributes(".*source-id=\"[a-zA-Z\\.]+:id/second_item\".*");
		if(tn.getFullXpath().equals("/root/div[2]/list/item[2]")){
			System.out.println("test find with attributes ok");
			attributes++;
		}
		
		tn = testXp.findNodeViaAttributes(".*attribute=\"one\".*");
		if(tn.getFullXpath().equals("/root/div[1]")){
			System.out.println("test find with attributes but attrib exists multiple times ok");
			attributes++;
		}
		
		XmlParser.Node duplicateNode  = testXp.new Node("item test=\"\" resource-id=\"com.via:id/second_item\"");
		duplicateNode.setStartPos(195);
		duplicateNode.setEndPos(246);

		XmlParser.Node list = testXp.findNodeViaXpath("/root/div[2]/list");
		
		int listCount = list.getChildren().size();
		list.addChildUnique(duplicateNode);
		
		if(listCount == list.getChildren().size()){
			System.out.println("test add unique and not adding ok");
			addChild++;
		}
		
		XmlParser.Node newNode  = testXp.new Node("item test=\"\" resource-id=\"com.via:id/second_item\"");
		newNode.setStartPos(196);
		newNode.setEndPos(250);
		
		list.addChildUnique(newNode);
		
		if(listCount+1 == list.getChildren().size()){
			System.out.println("test add unique new node and adding ok");
			addChild++;
		}
		
		XmlParser.Node hasNode = testXp.findNodeViaXpath("/root/div[2]/list/item[2]");
		hasNode.setIdentifier("secondItem");

		list.addChildUnique(duplicateNode);
		if(listCount+2 == list.getChildren().size()){
			System.out.println("test add unique identifier set on existing and adding ok");
			addChild++;
		}
		
		XmlParser.Node duplicateNode2  = testXp.new Node("item test=\"\" resource-id=\"com.via:id/second_item\"");
		duplicateNode2.setStartPos(195);
		duplicateNode2.setEndPos(246);
		duplicateNode2.setIdentifier("secondItem");
		
		list.addChildUnique(duplicateNode2);
		if(listCount+2 == list.getChildren().size()){
			System.out.println("test add unique with identifier and not adding ok");
			addChild++;
		}
		
		
		if(xpath == 11 && attributes == 2 && addChild == 4){
			System.out.println("all test ok");
		}else{
			System.out.println("all test not ok, check test or adjust count");
			System.out.println("xpath tests: "+ xpath);
			System.out.println("attributes tests: "+ attributes);
			System.out.println("addChild tests: "+ addChild);
		}
		
		tn = testXp.findNodeViaAttributes(".*id=\"listWithChildren\".*");
		for(XmlParser.Node n : tn.getChildren()){
			XmlParser.Node c = testXp.findChildNodeViaXpath(n, "//item");
			
			System.out.println("path is: "+ c.getFullXpath());
		}
		//		add test
//		node to child
//		add unique, not added and added
		
		
		System.exit(0);
	}
}