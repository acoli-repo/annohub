package de.unifrankfurt.informatik.acoli.fid.spider;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.unifrankfurt.informatik.acoli.fid.conll.ConllInfo;
import de.unifrankfurt.informatik.acoli.fid.detector.ModelEvaluatorQ;
import de.unifrankfurt.informatik.acoli.fid.exec.Executer;
import de.unifrankfurt.informatik.acoli.fid.jena.RDFDataMgr;
import de.unifrankfurt.informatik.acoli.fid.owl.ResultUpdater;
import de.unifrankfurt.informatik.acoli.fid.parser.CSVParserA;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager;
import de.unifrankfurt.informatik.acoli.fid.search.GWriter;
import de.unifrankfurt.informatik.acoli.fid.search.GenericStreamReaderSPO;
import de.unifrankfurt.informatik.acoli.fid.types.ProcessingFormat;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceMetadata;
import de.unifrankfurt.informatik.acoli.fid.types.MetadataSource;
import de.unifrankfurt.informatik.acoli.fid.types.ModelMatch;
import de.unifrankfurt.informatik.acoli.fid.types.ProcessState;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceState;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceType;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceTypeInfo;
import de.unifrankfurt.informatik.acoli.fid.types.VocabularyMatch;
import de.unifrankfurt.informatik.acoli.fid.types.VocabularyType;
import de.unifrankfurt.informatik.acoli.fid.types.Worker;
import de.unifrankfurt.informatik.acoli.fid.util.IndexUtils;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;


/**
 * Handle RDF file
 * @author frank
 *
 */
public class GenericRdfFileHandler implements RdfFileHandlerI {
	
	GWriter writer;
	GenericStreamReaderSPO genericRdfStreamParser;
	CSVParserA conllFileParser;
	ResourceManager resourceManager;
	Model model;
	ResultUpdater resultUpdater;

	
	
	public GenericRdfFileHandler (GWriter writer, ResourceManager rm) {
		this.writer = writer;
		this.resourceManager = rm;
		HashSet<String> languageInfoProperties = new HashSet<String>(
				Arrays.asList(writer.getConfiguration().getString("Processing.RDFParser.languageInfoProperties").split(",")));
		this.genericRdfStreamParser = new GenericStreamReaderSPO(Executer.conllNs, writer, 10, languageInfoProperties);
		this.resultUpdater = new ResultUpdater(rm, writer);
	}
	
	@Override
	public void parse(ResourceInfo resourceInfo, Worker fidWorker) {
		
		Utils.debug("GenericRdfFileHandler start parsing ...");
		
		if (!resourceInfo.getFileInfo().isRDFFile()) {
			finishWithoutResults( resourceInfo);
			Utils.debug("GenericRdfFileHandler : wrong fileFormat '"+ resourceInfo.getFileInfo().getProcessingFormat()+"'");
			return;
		}
		
		// Set file to process
		String processingFile = "file:"+resourceInfo.getFileInfo().getResourceFile().getAbsolutePath(); 
		// if the original file has been converted then set processingFile to the path of the converted file
		if (!resourceInfo.getFileInfo().getTemporaryFilePath().isEmpty()) {
			processingFile = "file:"+resourceInfo.getFileInfo().getTemporaryFilePath();
		}

		HashSet<String> allowedPredicates = new HashSet<String>();
		// Start parsing
		try {
			
			// Make dummy resource for sampling
			ResourceInfo sampleResource = new ResourceInfo("http://generic-rdf-parser/sample/"+fidWorker.getWorkerId(), "http://generic-rdf-parser/sample/"+fidWorker.getWorkerId(), "http://linghub/dummy/dataset");
			
			// configure sampling
			genericRdfStreamParser.setRunModeSampling(sampleResource, 10);
			
			// sample values for all RDF predicates
			RDFDataMgr.parse(genericRdfStreamParser, processingFile);
			boolean fileIsOntology = genericRdfStreamParser.isOntology(); // save ontology info here for later use !
			resourceInfo.getFileInfo().setTripleCount(genericRdfStreamParser.getTripleCount());
			resourceManager.setFileTripleCount(
					resourceInfo.getResource(),
					resourceInfo.getFileInfo().getFileVertex(),
					genericRdfStreamParser.getTripleCount());
		
			
			// Parse conll-RDF with own parser
			if (sampleResource.getFileInfo().isConllFile()) {
		
				// update file format
				resourceInfo.getFileInfo().setProcessingFormat(ProcessingFormat.CONLL);
				resourceManager.setFileFormat(
						resourceInfo.getResource(),
						resourceInfo.getFileInfo().getFileVertex(),
						ProcessingFormat.CONLL);
			
				// convert RDF to csv records
				ConllInfo conllInfo = ConllInfo.convertConllRDF2CSVRecords(resourceInfo, writer);
				resourceInfo.setConllInfo(conllInfo);
				
				fidWorker.getConllFileHandler().parse(resourceInfo);
				return;
			}
			
			
			// Continue with RDF file

			// retrieve sampling results
			HashMap<String, HashMap<String,Long>> predicates2LitObjects = genericRdfStreamParser.getPredicates2LitObjects();
			HashMap<String, HashMap<String,Long>> predicates2URIObjects = genericRdfStreamParser.getPredicates2URIObjects();
			HashSet<String> allLitPredicates = new HashSet<String>();
			allLitPredicates.addAll(predicates2LitObjects.keySet());
			HashSet<String> allURIPredicates = new HashSet<String>();
			allURIPredicates.addAll(predicates2URIObjects.keySet());
			
			
			// filter those predicates that have been known to be bad (yielded no results in the past)
			if (writer.getConfiguration().getBoolean("RunParameter.RdfPredicateFilterOn")) {
				for (String oldUnsuccessful : resourceManager.getUnsuccessfulPredicates()) {
					if (predicates2LitObjects.keySet().contains(oldUnsuccessful)) {
						predicates2LitObjects.remove(oldUnsuccessful);
						Utils.debug("Skipping disabled predicate : "+oldUnsuccessful);
						}
					if (predicates2URIObjects.keySet().contains(oldUnsuccessful)) {
						predicates2URIObjects.remove(oldUnsuccessful);
						Utils.debug("Skipping disabled predicate : "+oldUnsuccessful);	
					}
				}
			}
			
			//> old predicate testing with db
			// write sampled predicates (use sampleResource which serves to identify these nodes as test nodes)
			genericRdfStreamParser.write(predicates2LitObjects, predicates2URIObjects, sampleResource);
			
			// determine predicates that have produced a hit and save information in
			// predicateMap predicate -> good | bad
			String hitPredicate="";
			HashMap<String, Boolean> predicateMap = new HashMap<String, Boolean>();
			allowedPredicates.clear();
			Utils.debug("Allowed predicates :");
			for (Vertex mm : writer.getQueries().getHitsForResource(sampleResource)) {
				hitPredicate = mm.value(GWriter.HitPredicate);
				predicateMap.put(hitPredicate,true);
				allowedPredicates.add(hitPredicate);
			}
			if (allowedPredicates.isEmpty()) {
				Utils.debug("none");}
			else {
				for (String alp : allowedPredicates) {
					Utils.debug("*** : "+alp);
				}
			}
			
			for (String predicate : allLitPredicates) {
				if (!predicateMap.containsKey(predicate)) {
					predicateMap.put(predicate, false);
				}
			}
			for (String predicate : allURIPredicates) {
				if (!predicateMap.containsKey(predicate)) {
					predicateMap.put(predicate, false);
				}
			}
			//< old predicate testing with db
			
			//> new predicate testing with cache
			/*HashMap<String, Boolean> predicateMap = new HashMap<String, Boolean>();
			allowedPredicates.clear();
			Utils.debug("Allowed predicates :");
			predicateMap=writer.getAnnotationCache().getAllowedLiteralPredicates(predicates2LitObjects);
			predicateMap.putAll(writer.getAnnotationCache().getAllowedUrlPredicates(predicates2URIObjects));
			
			for (String predicate : predicateMap.keySet()) {
				if (predicateMap.get(predicate)) {
					allowedPredicates.add(predicate);
					Utils.debug("*** : "+predicate);
				}
			}*/
			//< new predicate testing with cache
			
			
			// update known predicates list with results from the evaluation above
			resourceManager.updateFilePredicates(
					resourceInfo.getResource(),
					resourceInfo.getFileInfo().getFileVertex(),
					predicateMap);
						
			// delete sample hit nodes (test nodes)
			writer.getQueries().deleteHitVertices(sampleResource.getDataURL());
						
			// now make a full run of the parser on the computed 'good predicates'
			genericRdfStreamParser.setRunModeWrite(resourceInfo, allowedPredicates);
			RDFDataMgr.parse(genericRdfStreamParser, processingFile);
			System.out.println("#triples# "+genericRdfStreamParser.getTripleCount());
			if (fileIsOntology) {
				resourceInfo.getFileInfo().getVocabularyMatchings().add(new VocabularyMatch(VocabularyType.OWL)); // use ontology info from sampling (above)
				// update string representation manually !
				resourceInfo.getFileInfo().updateVocabularyMatchingsAsString();
			}

							
			// Check if anything was found
			if (!genericRdfStreamParser.foundDocuments()) {
				finishWithoutResults(resourceInfo);
				return;
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
			resourceManager.setFileErrorCode(
					resourceInfo.getResource(),
					resourceInfo.getFileInfo().getFileVertex(),
					IndexUtils.ParseError);
			
			String errorMsg = StringUtils.substring(e.getMessage(),0,100);
			
			resourceManager.setFileErrorMsg(
					resourceInfo.getResource(),
					resourceInfo.getFileInfo().getFileVertex(),
					errorMsg);
			
			finishWithoutResults(resourceInfo);
			return;
		}
		
		HashMap<String, HashMap<String,Long>> predicates2Objects = new HashMap<String, HashMap<String,Long>>();
		predicates2Objects.putAll(genericRdfStreamParser.getPredicates2LitObjects());
		predicates2Objects.putAll(genericRdfStreamParser.getPredicates2URIObjects());
		
		finishWithResults(resourceInfo, predicates2Objects);
	}
	
	
	
	private void finishWithoutResults(ResourceInfo resourceInfo) {
		
		// set sample for type ONTOLOGY
		if (resourceInfo.getFileInfo().getResourceType() == ResourceType.ONTOLOGY) {
			resourceManager.setFileSample(
					resourceInfo.getResource(),
					resourceInfo.getFileInfo().getFileVertex(),
					IndexUtils.getFileSample(resourceInfo, 20));
		}
		
		// Set detected languages stored in resourceInfo
		Utils.debug("languages : "+resourceInfo.getFileInfo().getLanguageMatchings().size());
		resourceManager.updateFileLanguages(
				resourceInfo.getResource(),
				resourceInfo.getFileInfo().getFileVertex(),
				resourceInfo.getFileInfo());
		
		// Update vocabularies
		resourceManager.updateFileVocabularies(
				resourceInfo.getResource(),
				resourceInfo.getFileInfo().getFileVertex(),
				resourceInfo.getFileInfo().getVocabularyMatchings());
		
		// Set process state
		resourceInfo.getFileInfo().setProcessState(ProcessState.PROCESSED);
		resourceManager.updateProcessState(resourceInfo);
		
		// Measure processing time
		resourceInfo.getFileInfo().setProcessingEndDate(new Date());
		resourceManager.updateFileProcessingEndDate(resourceInfo);
		
		// Set status code (old, not used anymore)
		//resourceInfo.getFileInfo().setStatusCode(IndexUtils.NoDocumentsFoundInIndex);
		//resourceManager.setFileStatusCode(resourceInfo.getResource(), fileVertex, IndexUtils.NoDocumentsFoundInIndex);
		
		// Add file results (required for following detection of resource type)
		writer.getQueries().getFileResults(resourceInfo, resourceManager);
		// Save resource type
		ResourceTypeInfo resourceTypeInfo = 
				new ResourceTypeInfo(resourceInfo.getFileInfo().getResourceType());
		resourceManager.addResourceTypeInfo(resourceInfo.getDataURL(), resourceTypeInfo);
	}
	
	
	private void finishWithResults(ResourceInfo resourceInfo,  HashMap<String, HashMap<String, Long>> predicate2FoundObjectsMap) {
		
		// set sample
		resourceManager.setFileSample(
				resourceInfo.getResource(),
				resourceInfo.getFileInfo().getFileVertex(),
				IndexUtils.getFileSample(resourceInfo, 50)); // new option RDFFileSampleLines

		// Set detected languages stored in resourceInfo
		Utils.debug("languages : "+resourceInfo.getFileInfo().getLanguageMatchings().size());
		resourceManager.updateFileLanguages(
				resourceInfo.getResource(),
				resourceInfo.getFileInfo().getFileVertex(),
				resourceInfo.getFileInfo());
		
		// Evaluate found models and selected best matchings
		// Set detected models in file
		
		//System.out.println(resourceInfo.getLinghubAttributes());

		if (resourceInfo.getResourceUploadImportMetadata()) {
			resourceInfo.getResourceMetadata().setMetadataSource(MetadataSource.DATAFILE);
			resourceManager.updateResourceMetadata(resourceInfo);
		}
		
		// for RESCAN :
		if (resourceInfo.getResourceState() != ResourceState.ResourceNotInDB) {
			ArrayList<ResourceInfo> tmp = new ArrayList<ResourceInfo>();
			tmp.add(resourceInfo);
			resultUpdater.updateModelResultsAfterRDFRescan(tmp, predicate2FoundObjectsMap);

		} else {
		// for new resource :
		ArrayList <ModelMatch> foundModels = new ArrayList<ModelMatch>();
		for (String predicate : predicate2FoundObjectsMap.keySet()) {
			foundModels.addAll(writer.getQueries().
			getModelMatchingsNew(writer, resourceInfo, ModelMatch.NOCOLUMN,
					predicate, predicate2FoundObjectsMap.get(predicate).size()));
		}
		
		
		boolean autoDeleteRdfModelsWithTrivialResults=false; // TODO create autoDeleteRdfModelsWithTrivialResults option
		ModelEvaluatorQ.filterTrivialModelMatchingsRDF(foundModels, autoDeleteRdfModelsWithTrivialResults);
		resourceInfo.getFileInfo().setModelMatchings(foundModels);
		
		// TODO Bundle all updates (models, languages, etc.) into one function call 
		// with only one getFileValues call instead of calling it for every update function
		// (also in every Handler class)
		
		resourceManager.updateFileModels(
				resourceInfo.getResource(),
				resourceInfo.getFileInfo().getFileVertex(),
				resourceInfo.getFileInfo());
		
		// Save token counts
		resourceManager.updateFileUnitInfo(resourceInfo);
		}
		
		resourceManager.updateFileVocabularies(
				resourceInfo.getResource(),
				resourceInfo.getFileInfo().getFileVertex(),
				resourceInfo.getFileInfo().getVocabularyMatchings());
		
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
