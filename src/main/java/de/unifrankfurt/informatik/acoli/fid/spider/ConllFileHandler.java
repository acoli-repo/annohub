package de.unifrankfurt.informatik.acoli.fid.spider;

import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.configuration2.XMLConfiguration;

import de.unifrankfurt.informatik.acoli.fid.conll.ParserCONLL;
import de.unifrankfurt.informatik.acoli.fid.detector.ModelEvaluatorQ;
import de.unifrankfurt.informatik.acoli.fid.owl.ResultUpdater;
import de.unifrankfurt.informatik.acoli.fid.parser.CSVParserA;
import de.unifrankfurt.informatik.acoli.fid.parser.CSVParserConfig;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager;
import de.unifrankfurt.informatik.acoli.fid.search.GWriter;
import de.unifrankfurt.informatik.acoli.fid.types.ModelMatch;
import de.unifrankfurt.informatik.acoli.fid.types.ProcessState;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceState;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceTypeInfo;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;

public class ConllFileHandler {
	
	CSVParserA conllFileParser;
	ResourceManager resourceManager;
	GWriter writer;
	private boolean autoDeleteConllModelsWithTrivialResults = false;
	ResultUpdater resultUpdater;

	
	public ConllFileHandler(GWriter writer, ResourceManager resourceManager, XMLConfiguration config){
		CSVParserConfig csvParserConfig = new CSVParserConfig();
		
		conllFileParser = new ParserCONLL(csvParserConfig, writer, null);
		
		this.resourceManager = resourceManager;
		this.writer = writer;
		try {
			autoDeleteConllModelsWithTrivialResults = writer.getConfiguration().getBoolean("Processing.ModelEvaluator.autoDeleteConllModelsWithTrivialResults");
		} catch (Exception e){}
		
		this.resultUpdater = new ResultUpdater(resourceManager, writer);
	}

	
	/**
	 * Parse resource
	 * @param resourceInfo
	 * @return success
	 */
	public void parse(ResourceInfo resourceInfo) {
		
		if (!resourceInfo.getFileInfo().isConllFile()) {
			finishWithoutResults(resourceInfo);
			Utils.debug("ConllFileHandler : wrong fileFormat '"+ resourceInfo.getFileInfo().getProcessingFormat()+"'");
			return;
		}
				
		boolean success = conllFileParser.parse(resourceInfo);
		
		// Check if anything was found and write the new file status code
		if (!success) {
			finishWithoutResults(resourceInfo);
			return;
		}
		
		finishWithResults(resourceInfo);
	}
	
	
	
private void finishWithoutResults(ResourceInfo resourceInfo) {
		
			
		// Set process state
		resourceInfo.getFileInfo().setProcessState(ProcessState.PROCESSED);
		resourceManager.updateProcessState(resourceInfo);
		
		// Measure processing time
		resourceInfo.getFileInfo().setProcessingEndDate(new Date());
		resourceManager.updateFileProcessingEndDate(resourceInfo);
		
		// Set status codes (old, no more used)
		//resourceInfo.getFileInfo().setStatusCode(IndexUtils.NoDocumentsFoundInIndex); 
		//resourceManager.setFileStatusCode(resourceInfo.getResource(), fileVertex, IndexUtils.NoDocumentsFoundInIndex);
		
		// Add file results (required for following detection of resource type)
		writer.getQueries().getFileResults(resourceInfo, resourceManager);
		// Save resource type
		ResourceTypeInfo resourceTypeInfo = 
				new ResourceTypeInfo(resourceInfo.getFileInfo().getResourceType());
		resourceManager.addResourceTypeInfo(resourceInfo.getDataURL(), resourceTypeInfo);
}


private void finishWithResults(ResourceInfo resourceInfo) {
	
	// Set detected languages stored in resourceInfo
	resourceManager.updateFileLanguages(
			resourceInfo.getResource(),
			resourceInfo.getFileInfo().getFileVertex(),
			resourceInfo.getFileInfo());

	// Store the text sample on which the language detection is based on
	// Actually not needed!
	resourceManager.setFileLanguageSample(
			resourceInfo.getResource(),
			resourceInfo.getFileInfo().getFileVertex(),
			resourceInfo.getFileInfo().getLanguageSample());
	
	
	// for RESCAN :
	if (resourceInfo.getResourceState() != ResourceState.ResourceNotInDB) {
		
		ArrayList<ResourceInfo> tmp = new ArrayList<ResourceInfo>();
		tmp.add(resourceInfo);
		resultUpdater.updateModelResultsAfterCONLLOrXMLRescan(tmp, resourceInfo.getFileInfo().getColumnTokens());
		
		// Save token counts
		resourceManager.updateFileUnitInfo(resourceInfo);
		
	} else {
		// for new resource :
		// Compute models
		ArrayList <ModelMatch> foundModels = new ArrayList <ModelMatch>();
		for (int col : resourceInfo.getFileInfo().getColumnTokens().keySet()) {
			foundModels.addAll(
			writer.getQueries().getModelMatchingsNew(writer, resourceInfo, col, null,
					resourceInfo.getFileInfo().getColumnTokens().get(col).keySet().size()));
		}
		resourceInfo.getFileInfo().setModelMatchings(foundModels);
				
		// Evaluate found models and selected best matchings
		ModelEvaluatorQ.selectBestModelMatchingsCONLL(resourceInfo.getFileInfo().getModelMatchings(), autoDeleteConllModelsWithTrivialResults);
		
		resourceManager.updateFileModels(
				resourceInfo.getResource(),
				resourceInfo.getFileInfo().getFileVertex(), 
				resourceInfo.getFileInfo());
		
		// Save token counts
		resourceManager.updateFileUnitInfo(resourceInfo);
	}
	
	
	resourceManager.updateFileTokens(
			resourceInfo.getResource(),
			resourceInfo.getFileInfo().getFileVertex(),
			resourceInfo.getFileInfo().getColumnTokens());
	
	resourceManager.setFileSample(
			resourceInfo.getResource(),
			resourceInfo.getFileInfo().getFileVertex(),
			resourceInfo.getFileInfo().getSample());
	
	resourceInfo.getFileInfo().setProcessState(ProcessState.PROCESSED);
	resourceManager.updateProcessState(resourceInfo);

	
	// Measure processing time
	resourceInfo.getFileInfo().setProcessingEndDate(new Date());
	resourceManager.updateFileProcessingEndDate(resourceInfo);
	
	// Add file results (required for following detection of resource type)
	writer.getQueries().getFileResults(resourceInfo, resourceManager);
	
	// Save resource type
	ResourceTypeInfo resourceTypeInfo = 
			new ResourceTypeInfo(resourceInfo.getFileInfo().getResourceType());
	resourceManager.addResourceTypeInfo(resourceInfo.getDataURL(), resourceTypeInfo);
	
	// auto-accept
	if (resourceInfo.getResourceUploadAutoAccept()) {
		resourceInfo.getFileInfo().setProcessState(ProcessState.ACCEPTED);
		resourceManager.updateProcessState(resourceInfo);
		resourceInfo.getFileInfo().setAcceptedDate(new Date());
		resourceManager.updateFileAcceptedDate(resourceInfo);
	}
}


}
