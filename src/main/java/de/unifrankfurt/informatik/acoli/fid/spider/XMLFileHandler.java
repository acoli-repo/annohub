package de.unifrankfurt.informatik.acoli.fid.spider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration2.XMLConfiguration;
import org.w3c.dom.Document;

import de.unifrankfurt.informatik.acoli.fid.conll.ParserCONLL;
import de.unifrankfurt.informatik.acoli.fid.parser.CSVParserA;
import de.unifrankfurt.informatik.acoli.fid.parser.CSVParserConfig;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.TemplateManager;
import de.unifrankfurt.informatik.acoli.fid.search.GWriter;
import de.unifrankfurt.informatik.acoli.fid.types.ProcessingFormat;
import de.unifrankfurt.informatik.acoli.fid.types.ProcessState;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.Worker;
import de.unifrankfurt.informatik.acoli.fid.util.IndexUtils;
import de.unifrankfurt.informatik.acoli.fid.xml.SubtreeGenerator;
import de.unifrankfurt.informatik.acoli.fid.xml.TemplateMatcher;
import de.unifrankfurt.informatik.acoli.fid.xml.TemplateQuality;
import de.unifrankfurt.informatik.acoli.fid.xml.XMLParser;
import de.unifrankfurt.informatik.acoli.fid.xml.XMLSampler;

/**
 * 
 * @author frank
 * @deprecated
 *
 */
public class XMLFileHandler implements XMLFileHandlerI {
	
	CSVParserA xmlFileParser;
	CSVParserA conllFileParser;
	ResourceManager resourceManager;
	SubtreeGenerator xmlFile;
	XMLConfiguration config;
	TemplateManager templateManager;
	GWriter writer;
	private final static Logger LOGGER =
			Logger.getLogger(XMLFileHandler.class.getName());
	
	public XMLFileHandler(GWriter writer, ResourceManager resourceManager, TemplateManager templateManager, XMLConfiguration config){
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
			System.out.println("XMLFileHandler : wrong fileFormat '"+ resourceInfo.getFileInfo().getProcessingFormat()+"'");
			return;
		}
		
		
		String xmlSample = IndexUtils.getFileSample(resourceInfo, 10);
		
		
		//=============== Part I. XML Parsing ===============//
		
		// initialize the generator object for all parsing
		System.out.println("XMLFileHandler is parsing..");
		
		String sentenceName = null;
		for (String sentenceNameCandidate : templateManager.getAllSentenceNames()){
			if (XMLSampler.hasNode(resourceInfo.getFileInfo().getResourceFile(), sentenceNameCandidate, 100)) { // TODO: find good n
				sentenceName = sentenceNameCandidate;
			}
		}
		if (sentenceName == null){
			LOGGER.warning("No Template found with fitting sentenceName for file "+resourceInfo.getFileInfo().getFileName());
			return;
		}
		this.xmlFile = new SubtreeGenerator(sentenceName, resourceInfo.getFileInfo().getResourceFile());
		// Figure out which template fits the most.
		// TODO: configurable measurement
		ArrayList<Document> sample = this.xmlFile.getSamples(20);
		
		TemplateMatcher tm = templateManager.createTemplateMatcher();
		TemplateQuality bestMatch = tm.getBestTemplateQuality(sample);

		XMLParser xmlParser = new XMLParser(this.writer, this.xmlFile, bestMatch.getTemplate(), 1000);
		
		// TODO return from xmlParser.parse() is always true ?
		boolean success = xmlParser.parse(resourceInfo);
		//System.out.println("hello");
		//for (int column : resourceInfo.getFileInfo().getConllcolumn2XMLAttr().keySet()) {
		//	System.out.println("column "+column + ": "+resourceInfo.getFileInfo().getConllcolumn2XMLAttr().get(column));
		//}
		
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
		try {
			templateManager.writeMatchToGraphDB(bestMatch, resourceInfo);
		} catch (Exception e) {e.printStackTrace();}
		
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
		
		// Set status code  (old, no more used)
		//resourceInfo.getFileInfo().setStatusCode(IndexUtils.NoDocumentsFoundInIndex);
		//resourceManager.setFileStatusCode(resourceInfo.getResource(), fileVertex,  IndexUtils.NoDocumentsFoundInIndex);
				
		LOGGER.info("Done.");
	}
	
	
}
