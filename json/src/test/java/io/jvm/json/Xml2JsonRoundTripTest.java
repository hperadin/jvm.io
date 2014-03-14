package io.jvm.json;

import static io.jvm.json.Helpers.getFileForResource;
import static io.jvm.json.Helpers.parseXmlFile;
import static io.jvm.json.Helpers.stringFromFile;
import static org.junit.Assert.assertTrue;
import io.jvm.json.deserializers.XmlJsonDeserializer;
import io.jvm.json.serializers.XmlJsonSerializer;
import io.jvm.xml.XmlBruteForceComparator;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URISyntaxException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class Xml2JsonRoundTripTest {
	
	/**
	 * For each of the XML files in 'resources/roundtripTests/source/xml'
	 * generates the entire roundtrip conversion (xml -> json -> xml) using
	 * io.jvm.json.serializers.XmlJsonSerializer, and asserts the generated XML
	 * equivalence with the reference conversions (obtained by using Json.NET)
	 */
	@Test
	public void assertRoundTripEquivalenceWithReferenceConversion()
			throws URISyntaxException, JSONException, SAXException,
			IOException, ParserConfigurationException, TransformerConfigurationException, TransformerException, TransformerFactoryConfigurationError {		
		
		final File xmlSources_dir = getFileForResource("/roundtripTests/xml/source/");				
				
		for (final File xmlSourceFile : xmlSources_dir.listFiles()) {			
			if (xmlSourceFile.isFile()) {
			    	System.out.println("Testiramo za datoteku: " + xmlSourceFile.getName());
				/* Filename initialisation */
				final String sourceFilename_xml = xmlSourceFile.getName();
				final String convertedFilename_json = sourceFilename_xml	+ ".json";
				final String roundtripFilename_xml = sourceFilename_xml	+ ".json.xml";
				
				final File referenceFile_json = getFileForResource("/roundtripTests/xml/reference/"+ convertedFilename_json);			
				assertTrue("The reference JSON file does not exist for: " + sourceFilename_xml, (referenceFile_json != null && referenceFile_json.exists()));
				
				final File referenceRoundtripFile_xml = getFileForResource("/roundtripTests/xml/reference/"+ roundtripFilename_xml);								
				assertTrue("The reference XML->JSON roundtrip file does not exist for: " + sourceFilename_xml, (referenceRoundtripFile_xml != null && referenceRoundtripFile_xml.exists()));
				
				/* Parse input files */
				final Document source_xml = parseXmlFile(xmlSourceFile);
				final String referenceJson = stringFromFile(referenceFile_json);
				final Document referenceRoundTrip_xml = parseXmlFile(referenceRoundtripFile_xml);															
				
				/* Convert to json and compare with reference conversion */
				final String convertedJson = jsonStringFromXml(source_xml);				
				assertJsonEquivalence(convertedJson, referenceJson);								

				/* Convert back to XML, and compare with reference documents */
				final Document roundtripXmlDocument = xmlDocumentfromJson(convertedJson);				
				
				assertXmlEquivalence("The reference roundtrip XML does not match the converted roundtrip XML",roundtripXmlDocument, referenceRoundTrip_xml);
				assertXmlEquivalence("The roundtrip XML does not match the source XML",roundtripXmlDocument,source_xml);
				assertXmlEquivalence("The reference roundtrip XML does not match the source XML",referenceRoundTrip_xml, source_xml);				
			}
		}					
	}		
		
	private static Document xmlDocumentfromJson(String json) throws IOException
	{
		JsonReader jr = new JsonReader(new StringReader(json));
		
		return new XmlJsonDeserializer().fromJson(jr);
	}

	private static String jsonStringFromXml(final Document source_xml) throws IOException{						
		final StringWriter sw = new StringWriter();
		new XmlJsonSerializer().toJson(new JsonWriter(sw), source_xml.getDocumentElement());
		return sw.toString();
	}						
	
	private static void assertJsonEquivalence(String lhs, String rhs) throws JSONException
	{
		
		JSONAssert.assertEquals(lhs, rhs, false);
	}
	
	private static void assertXmlEquivalence(String message, Document lhs, Document rhs){
		XmlBruteForceComparator comparator = new XmlBruteForceComparator();
		
		assertTrue(message, comparator.compare(lhs.getDocumentElement(), rhs.getDocumentElement())==0);
	}
	
//	private static void assertXmlEquivalence(String message, Document lhs, Document rhs)
//	{		
//		XMLUnit.setIgnoreAttributeOrder(true);
//		XMLUnit.setIgnoreWhitespace(true);
////		XMLUnit.setNormalize(true);		
//		
//		System.out.println("Roundtrip:");
//		System.out.println(xmlDocumentToString(lhs));
//		System.out.println("Source:");
//		System.out.println(xmlDocumentToString(rhs));
//		
//		final Diff diff = new Diff(lhs, rhs);
//		final DetailedDiff dd = new DetailedDiff(diff);
//		
//		StringBuffer msg = new StringBuffer();
//		diff.appendMessage(msg);
//		
////		System.out.println(msg);
//		
//		//assertTrue(message, dd.similar());
//		assertTrue(message, diff.similar());
//	}
	
}