package io.jvm.json;

import static org.junit.Assert.assertTrue;
import io.jvm.json.deserializers.XmlJsonDeserializer;
import io.jvm.json.serializers.XmlJsonSerializer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class Xml2JsonRoundTripTest {

	/**
	 * For each of the XML files in 'resources/roundtripTests/source/xml'
	 * generates the entire roundtrip conversion (xml -> json -> xml) using
	 * io.jvm.json.serializers.XmlJsonSerializer, and asserts the generated XML
	 * equivalence with the reference conversions (obtained by using Json.NET)
	 */
	// TODO: Separate tests for these cases
	@Test
	public void assertRoundTripEquivalenceWithReferenceConversion()
			throws URISyntaxException, JSONException, SAXException,
			IOException, ParserConfigurationException, TransformerConfigurationException, TransformerException, TransformerFactoryConfigurationError {		
		final File xmlSources_dir = getFileForResource("/roundtripTests/source/");				
		
		/* Iterate through the sources directory */
		for (final File xmlSourceFile : xmlSources_dir.listFiles()) {						
			
			/* If perchance this is a directory, skip */
			if (xmlSourceFile.isFile()) {
				
				/*
				 * In short, we deal with five files: 
				 * 		- source/source.xml 
				 * 		- converted/source.xml.json 
				 * 		- converted/source.xml.json.xml 
				 * 		- reference/source.xml.json 
				 * 		- reference/source.xml.json
				 */

				/* Filename initialisation */
				final String sourceFilename_xml = xmlSourceFile.getName();
				final String convertedFilename_json = sourceFilename_xml	+ ".json";
				final String roundtripFilename_xml = sourceFilename_xml	+ ".json.xml";
				
				final File referenceFile_json = getFileForResource("/roundtripTests/reference/"+ convertedFilename_json);			
				assertTrue("The reference JSON file does not exist for: " + sourceFilename_xml, (referenceFile_json != null && referenceFile_json.exists()));
				
				final File referenceRoundtripFile_xml = getFileForResource("/roundtripTests/reference/"+ roundtripFilename_xml);								
				assertTrue("The reference XML->JSON roundtrip file does not exist for: " + sourceFilename_xml, (referenceRoundtripFile_xml != null && referenceRoundtripFile_xml.exists()));
				
				/* Parse input files */
				final Document source_xml = parseXmlFile(xmlSourceFile);
				final String referenceJson = stringFromFile(referenceFile_json);
				final Document referenceRoundTrip_xml = parseXmlFile(referenceRoundtripFile_xml);															
				
				/* Convert to json and compare with reference conversion */
				final String convertedJson = jsonStringFromXml(source_xml);				
				assertJsonEquivalence(convertedJson, referenceJson);								

				/* Convert back to XML, and compare with reference documents */
//				final Document roundtripXmlDocument = xmlDocumentfromJson(convertedJson);				
//				
//				assertXmlEquivalence("The reference roundtrip XML does not match the converted roundtrip XML",roundtripXmlDocument, referenceRoundTrip_xml);
//				assertXmlEquivalence("The roundtrip XML does not match the source XML",roundtripXmlDocument,source_xml);
//				assertXmlEquivalence("The reference roundtrip XML does not match the source XML",referenceRoundTrip_xml, source_xml);
				
				/* Save the newly generated files for future reference */
				// TODO:
			}
		}					
	}		
		
	private static Document xmlDocumentfromJson(String json) throws IOException
	{
		JsonReader jr = new JsonReader(new StringReader(json));
		
		return new XmlJsonDeserializer().fromJson(jr);
	}

	private static String jsonStringFromXml(final Document source_xml) throws IOException{
		
		System.out.println("Konvertiramo: "+ source_xml.getDocumentURI());
		
		final StringWriter sw = new StringWriter();
		new XmlJsonSerializer().toJson(new JsonWriter(sw), source_xml.getDocumentElement());
		return sw.toString();
	}
	
	
	/* XXX: Does not care for encoding */
	private static String stringFromFile(File file) throws IOException
	{				
		BufferedReader br = new BufferedReader(new FileReader(file));				
		try {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}
			return sb.toString();
		} finally {
			br.close();
		}
	}				
	
	private static void assertJsonEquivalence(String lhs, String rhs) throws JSONException
	{
		System.out.println("Checking for json equivalence of the following:");
		System.out.println(lhs);
		System.out.println(rhs);
		
		JSONAssert.assertEquals(lhs, rhs, false);
	}
	
	private static void assertXmlEquivalence(String message, Document lhs, Document rhs)
	{		
		XMLUnit.setIgnoreAttributeOrder(true);
		XMLUnit.setIgnoreWhitespace(true);
		XMLUnit.setNormalize(true);		
		
		/*System.out.println("Roundtrip");
		System.out.println(xmlDocumentToString(lhs));
		System.out.println("Source");
		System.out.println(xmlDocumentToString(rhs));*/
		
		final Diff diff = new Diff(lhs, rhs);
		final DetailedDiff dd = new DetailedDiff(diff);
		
		StringBuffer msg = new StringBuffer();
		diff.appendMessage(msg);
		
		System.out.println(msg);
		
		assertTrue(message, dd.similar());
		//assertTrue(message, diff.similar());
	}
	
	private static File getFileForResource(String resourcePath) throws URISyntaxException
	{		
		final URL resourceURL = Xml2JsonRoundTripTest.class.getResource(resourcePath);				
		
		if (resourceURL==null) 
			return null;
		else
			return new File(resourceURL.toURI());
		
	}
	
	private static Document parseXmlFile(final File file)
			throws SAXException, IOException, ParserConfigurationException {				
		
		final Document doc =	DocumentBuilderFactory
					.newInstance()
					.newDocumentBuilder()
					.parse(file);
		trimWhitespaceTextNodes(doc);
		
		return doc;
	}
	
	private static void trimWhitespaceTextNodes(final org.w3c.dom.Node node) {
		if (node != null && node.getChildNodes() != null)
			for (int i = 0; i < node.getChildNodes().getLength(); i++) {
				final org.w3c.dom.Node child = node.getChildNodes().item(i);
				if (child.getNodeType() == org.w3c.dom.Node.TEXT_NODE
						&& child.getNodeValue().trim().length() == 0)
					node.removeChild(child);
				trimWhitespaceTextNodes(node.getChildNodes().item(i));
			}
	}
	
	
	/* =================================== Irrelevant helpers ======================================================*/

	private static void printDocumentTree(Node el){
		System.out.println(el.toString());
		for(int i=0;i<el.getChildNodes().getLength();i++)
			printDocumentTree(el.getChildNodes().item(i));		
	} 
	
	public static String xmlDocumentToString(Document doc)
	{
	    try
	    {
	       DOMSource domSource = new DOMSource(doc);
	       StringWriter writer = new StringWriter();
	       StreamResult result = new StreamResult(writer);
	       TransformerFactory tf = TransformerFactory.newInstance();
	       Transformer transformer = tf.newTransformer();
	       transformer.transform(domSource, result);
	       return writer.toString();
	    }
	    catch(TransformerException ex)
	    {
	       ex.printStackTrace();
	       return null;
	    }
	} 
}