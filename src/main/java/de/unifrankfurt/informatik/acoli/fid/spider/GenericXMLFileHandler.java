package de.unifrankfurt.informatik.acoli.fid.spider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import de.unifrankfurt.informatik.acoli.fid.types.*;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.unifrankfurt.informatik.acoli.fid.conll.ParserCONLL;
import de.unifrankfurt.informatik.acoli.fid.parser.CSVParserA;
import de.unifrankfurt.informatik.acoli.fid.parser.CSVParserConfig;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.TemplateManager;
import de.unifrankfurt.informatik.acoli.fid.search.GWriter;
import de.unifrankfurt.informatik.acoli.fid.util.IndexUtils;
import de.unifrankfurt.informatik.acoli.fid.xml.SubtreeGenerator;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;

public class GenericXMLFileHandler implements XMLFileHandlerI{
	
	CSVParserA xmlFileParser;
	CSVParserA conllFileParser;
	ResourceManager resourceManager;
	SubtreeGenerator xmlFile;
	XMLConfiguration config;
	TemplateManager templateManager;
	GWriter writer;
	private final static Logger LOGGER =
			Logger.getLogger(GenericXMLFileHandler.class.getName());
	private boolean processDuplicates = false;
	private int xmlValueSampleCount = 10;
	private ConllConversionMode makeConllMode = ConllConversionMode.SAMPLE;
	private ConllConverterChoice makeConllConverterChoice = ConllConverterChoice.GENERIC;
	private int makeConllSampleSentenceCount = 15000;
	private int makeConllAutoMaxFileSize = 5;

	
	public GenericXMLFileHandler(GWriter writer, ResourceManager resourceManager, TemplateManager templateManager, XMLConfiguration config){
		CSVParserConfig csvParserConfig = new CSVParserConfig();
		this.writer = writer;
		this.config = config;
		this.resourceManager = resourceManager;
		this.templateManager = templateManager;


		conllFileParser = new ParserCONLL(csvParserConfig, writer, null);
	}
	
	public void parse(ResourceInfo resourceInfo, Worker fidWorker) throws XMLStreamException, IOException {

		
		if (!resourceInfo.getFileInfo().isXMLFile()) {
			finishWithErrors(resourceInfo, "XMLParser error : wrong fileFormat '"+ resourceInfo.getFileInfo().getProcessingFormat()+"'");
			Utils.debug("XMLFileHandler : wrong fileFormat '"+ resourceInfo.getFileInfo().getProcessingFormat()+"'");
			return;
		}
		
		
		// Make dummy resource for sampling
		ResourceInfo sampleResource = new ResourceInfo("http://generic-xml-parser/sample/"+fidWorker.getWorkerId(), "http://generic-xml-parser/sample/"+fidWorker.getWorkerId(), "http://linghub/dummy/dataset");
		sampleResource.getFileInfo().setResourceFile(resourceInfo.getFileInfo().getResourceFile());
		
		XMLAttributeSamplerImpl xmlAttributeSampler = new XMLAttributeSamplerImpl();
		// Run sampling !
		try {
		   xmlValueSampleCount = fidWorker.getConfiguration().getInt("Processing.GenericXmlFileHandler.xmlValueSampleCount");
		} catch (Exception e){}
		xmlAttributeSampler.sample(sampleResource, xmlValueSampleCount);
		
		
		// Sampling results are retrieved with getters
		HashMap<String, HashMap<String,Long>> attributes2LitObjects = xmlAttributeSampler.getAttributes2LitObjects();
		HashMap<String, HashMap<String,Long>> attributes2URIObjects = xmlAttributeSampler.getAttributes2URIObjects();
		HashSet<String> allLitAttributes = new HashSet<String>();
		allLitAttributes.addAll(attributes2LitObjects.keySet());
		HashSet<String> allURIAttributes = new HashSet<String>();
		allURIAttributes.addAll(attributes2URIObjects.keySet());
		HashSet<String> availableAttributes = new HashSet<String>(allLitAttributes);
		availableAttributes.addAll(allURIAttributes);
		
		
		// Filter XML ID fields
		// (filter XML attributes were each value does only occur once in the the sample)
		// Examples are any form of if fields
		Iterator<Entry<String, HashMap<String, Long>>> iterator = attributes2LitObjects.entrySet().iterator();
		String attr_="";
		HashMap<String,Long> attrValues;
		while(iterator.hasNext()) {
			Entry<String, HashMap<String, Long>> pair = (Map.Entry<String, HashMap<String, Long>>)iterator.next();
			attr_ = (String) pair.getKey();
			attrValues = (HashMap<String, Long>) pair.getValue();
			int diffHits = attrValues.keySet().size();
			int hitsWithCountOne = 0;
			for (String tagOrClass_ : attrValues.keySet()) {

				// finding non tag skips leaves attribute untouched
				if (tagOrClass_.startsWith("http") || tagOrClass_.startsWith("file:") || tagOrClass_.startsWith("ftp:")) break;
				
				// count attributes were each value does only occur once in the the sample
				if (attrValues.get(tagOrClass_) == 1) hitsWithCountOne++;
			}
			if (diffHits == hitsWithCountOne) {
				iterator.remove();
				Utils.debug("Removing possible XML ID field "+attr_+" because all of its values occur only once !");
			}
		}
		
		/*Utils.debug("attributes2LitObjects :");
		for (String x : attributes2LitObjects.keySet()) {
			Utils.debug("attr :"+x);
			for (String y : attributes2LitObjects.get(x).keySet()) {
				Utils.debugNor(y+":");
				Utils.debugNor(attributes2LitObjects.get(x).get(y));
				;
			}
		}*/
		Utils.debug("Available attributes :");
		for (String x : availableAttributes) {
			Utils.debug(x);
		}
		
		//> old predicate testing with db
		// write sampled attributes (use sampleResource which serves to identify those nodes as test nodes)
		writer.writeGenericRdf(attributes2LitObjects, attributes2URIObjects, sampleResource);
		
		// determine predicates that have produced a hit and save information in
		// predicateMap attribute -> good | bad
		
		HashSet<String> allowedAttributes = new HashSet<String>();
		HashMap<String, HashSet<String>> foundTagsOrClassesForAttribute = new HashMap<String, HashSet<String>>();
		String hitAttribute="";
		HashMap<String, Boolean> attributeMap = new HashMap<String, Boolean>();
		
		Utils.debug("Allowed attributes :");
		ArrayList<Vertex> sampleHitVertices = writer.getQueries().getHitsForResource(sampleResource);
		for (Vertex vh : sampleHitVertices) {
			hitAttribute = vh.value(GWriter.HitPredicate);
			attributeMap.put(hitAttribute,true);
			if (!allowedAttributes.contains(hitAttribute)) {
				Utils.debug("*** : "+hitAttribute);
			}
			allowedAttributes.add(hitAttribute);
			if (!foundTagsOrClassesForAttribute.containsKey(hitAttribute)) {
				HashSet<String> classOrTag = new HashSet<String>();
				classOrTag.add(vh.value(GWriter.HitObject)+"@"+vh.value(GWriter.HitCount));
				foundTagsOrClassesForAttribute.put(hitAttribute, classOrTag);
			} else {
				HashSet<String> classOrTag = foundTagsOrClassesForAttribute.get(hitAttribute);
				classOrTag.add(vh.value(GWriter.HitObject)+"@"+vh.value(GWriter.HitCount));
				foundTagsOrClassesForAttribute.put(hitAttribute, classOrTag);
			}
		}
		
		if (allowedAttributes.isEmpty()) {Utils.debug("none");}
		
		
		for (String attribute : allLitAttributes) {
			if (!attributeMap.containsKey(attribute)) {
				attributeMap.put(attribute, false);
			}
		}
		for (String attribute : allURIAttributes) {
			if (!attributeMap.containsKey(attribute)) {
				attributeMap.put(attribute, false);
			}
		}
					
		// delete sample hit nodes (test nodes)
		writer.getQueries().deleteHitVertices(sampleResource.getDataURL());
		//< old predicate testing with db
		
		
		//> new predicate testing with cache
		/*HashMap<String, HashSet<String>> foundTagsOrClassesForAttribute = writer.getAnnotationCache().getFoundTagsOrClassesForAttribute(attributes2URIObjects);
		foundTagsOrClassesForAttribute.putAll(writer.getAnnotationCache().getFoundTagsOrClassesForAttribute(attributes2LitObjects));

		HashSet<String> allowedAttributes = new HashSet<String>();
		HashMap<String, Boolean> attributeMap = new HashMap<String, Boolean>();
		
		Utils.debug("Allowed attributes :");	
		for (String attribute : foundTagsOrClassesForAttribute.keySet()) {
			if (!foundTagsOrClassesForAttribute.get(attribute).isEmpty()) {
				allowedAttributes.add(attribute);
				attributeMap.put(attribute, true);
				Utils.debug("*** : "+attribute);
			} else {
				attributeMap.put(attribute, false);
			}
		}*/
	    //< new predicate testing with cache

		
		// Nothing usable found then exit
		if (allowedAttributes.isEmpty()) {
			finishWithoutResults(resourceInfo);
			return;
		}
		
		
		// Get hit results for later evaluation (?? old version)
		/*HashMap<String,ArrayList<ModelMatch>> sampleModelMatchingsMap = new HashMap<String,ArrayList<ModelMatch>>();
		for (String xpath : allowedAttributes) {
			sampleResource.getFileInfo().setFileFormat(FileFormat.XML);
			ArrayList<ModelMatch> sampleModelMatchings = writer.getQueries().
					getModelMatchingsNew(writer, sampleResource, ModelMatch.NOCOLUMN, xpath, 1000);
			if (sampleModelMatchings != null) {
				sampleModelMatchingsMap.put(xpath, sampleModelMatchings);
			}
		}*/

		
		// sample for later editing
		String xmlSample = IndexUtils.getFileSample(resourceInfo, 10);
		
		
		//=============== Part I. XML Parsing ===============//
		
		// initialize the generator object for all parsing
		Utils.debug("XMLFileHandler is parsing..");
		
		try 
		{
		  makeConllConverterChoice  = ConllConverterChoice.valueOf(fidWorker.getConfiguration().getString("Processing.GenericXmlFileHandler.makeConllConverterChoice").toUpperCase());
		} catch (Exception e){}
		try 
		{
		  makeConllMode   = ConllConversionMode.valueOf(fidWorker.getConfiguration().getString("Processing.GenericXmlFileHandler.makeConllMode").toUpperCase());
		} catch (Exception e){}
		try 
		{
		  makeConllAutoMaxFileSize  = fidWorker.getConfiguration().getInt("Processing.GenericXmlFileHandler.makeConllAutoMaxFileSize");
		} catch (Exception e){}
		try 
		{
		  makeConllSampleSentenceCount = fidWorker.getConfiguration().getInt("Processing.GenericXmlFileHandler.makeConllSampleSentenceCount");
		} catch (Exception e){}
		try 
		{
		  processDuplicates = fidWorker.getConfiguration().getBoolean("Processing.XMLAttributeEvaluator.processDuplicates");
		} catch (Exception e){}
		// Create CoNLL from selected attributes 
		// next line is replacement for inactive code block below (VERSION WITH TEMPLATES)

		// TODO: fix?
//		HashSet<String> completeXMLAttributeSet = XMLAttributeEvaluator.computeCompleteXMLAttributSet(availableAttributes, allowedAttributes, foundTagsOrClassesForAttribute, processDuplicates);
//		boolean success;
//		if (completeXMLAttributeSet.size() > 0) {
//			success = xmlAttributeSampler.makeConll(
//					resourceInfo,
//					writer,
//					completeXMLAttributeSet,
//					makeConllSampleSentenceCount);
//		} else
//			success =false;

		boolean success = xmlAttributeSampler.makeConll(
				resourceInfo,
				writer,
				XMLAttributeEvaluator.computeCompleteXMLAttributSet(availableAttributes, allowedAttributes, foundTagsOrClassesForAttribute, processDuplicates),
				makeConllMode,
				makeConllConverterChoice,
				makeConllSampleSentenceCount,
				makeConllAutoMaxFileSize);


		// VERSION WITH TEMPLATES
		/*String sentenceName = null;
		for (String sentenceNameCandidate : templateManager.getAllSentenceNames()){
			if (GenericXMLRecognizer.hasNode(resourceInfo.getFileInfo().getResourceFile(), sentenceNameCandidate, 100)) {
				sentenceName = sentenceNameCandidate;
			}
		}
		if (sentenceName == null){
			LOGGER.warning("No Template found with fitting sentenceName for file "+resourceInfo.getFileInfo().getFileName());
			return;
		}
		this.xmlFile = new SubtreeGenerator(sentenceName, resourceInfo.getFileInfo().getResourceFile());
		// Figure out which template fits the most.
		// todo add configurable measurement
		ArrayList<Document> sample = this.xmlFile.getSamples(20);
		
		TemplateMatcher tm = templateManager.createTemplateMatcher();
		TemplateQuality bestMatch = tm.getBestTemplateQuality(sample);

		XMLParser xmlParser = new XMLParser(this.writer, this.xmlFile, bestMatch.getTemplate(), 1000);
		
		// todo return from xmlParser.parse() is always true ?
		boolean success = xmlParser.parse(resourceInfo);
		//Utils.debug("hello");
		//for (int column : resourceInfo.getFileInfo().getConllcolumn2XMLAttr().keySet()) {
		//	Utils.debug("column "+column + ": "+resourceInfo.getFileInfo().getConllcolumn2XMLAttr().get(column));
		//}
		 
		*/
		
		
		if (!success) {
			// XML parser fail
			finishWithErrors(resourceInfo, "XMLParser error");
			return;
		}
		
		
		//=============== Part II. CONLL Parsing ===============//
		
		// set sample
		resourceInfo.getFileInfo().setSample(xmlSample);
		
		// update file format
		resourceInfo.getFileInfo().setProcessingFormat(ProcessingFormat.CONLL);
		resourceManager.setFileFormat(
				resourceInfo.getResource(),
				resourceInfo.getFileInfo().getFileVertex(),
				ProcessingFormat.CONLL);
		
		// parse conll
		fidWorker.getConllFileHandler().parse(resourceInfo);
		
		// finally write template info
		// TODO (check if required)
		/*try {
			templateManager.writeMatchToGraphDB(bestMatch, resourceInfo);
		} catch (Exception e) {e.printStackTrace();}*/
		
		LOGGER.info("Done.");
	}
	
	
	private void finishWithErrors(ResourceInfo resourceInfo, String errorMsg) {
			
		resourceManager.setFileErrorMsg(
				resourceInfo.getResource(),
				resourceInfo.getFileInfo().getFileVertex(),
				errorMsg);
		
		// Set process state
		resourceInfo.getFileInfo().setProcessState(ProcessState.PROCESSED);
		resourceManager.updateProcessState(resourceInfo);
		
		// Measure processing time
		resourceInfo.getFileInfo().setProcessingEndDate(new Date());
		resourceManager.updateFileProcessingEndDate(resourceInfo);
		
		// Set status code (old, not used anymore)
		//resourceInfo.getFileInfo().setStatusCode(IndexUtils.NoDocumentsFoundInIndex);
		//resourceManager.setFileStatusCode(resourceInfo.getResource(), fileVertex,  IndexUtils.NoDocumentsFoundInIndex);
	

		LOGGER.info("Done.");
	}
	
	
	private void finishWithoutResults(ResourceInfo resourceInfo) {
		
		// Set process state
		resourceInfo.getFileInfo().setProcessState(ProcessState.PROCESSED);
		resourceManager.updateProcessState(resourceInfo);
		
		// Measure processing time
		resourceInfo.getFileInfo().setProcessingEndDate(new Date());
		resourceManager.updateFileProcessingEndDate(resourceInfo);
	}
	
	
}
