package de.unifrankfurt.informatik.acoli.fid.resourceDB;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.unifrankfurt.informatik.acoli.fid.exec.Executer;
import de.unifrankfurt.informatik.acoli.fid.gremlinQuery.EmbeddedQuery;
import de.unifrankfurt.informatik.acoli.fid.gremlinQuery.ServerQuery;
import de.unifrankfurt.informatik.acoli.fid.types.AccountType;
import de.unifrankfurt.informatik.acoli.fid.types.Backup;
import de.unifrankfurt.informatik.acoli.fid.types.Comment;
import de.unifrankfurt.informatik.acoli.fid.types.ProcessingFormat;
import de.unifrankfurt.informatik.acoli.fid.types.FileInfo;
import de.unifrankfurt.informatik.acoli.fid.types.LanguageMatch;
import de.unifrankfurt.informatik.acoli.fid.types.ModelMatch;
import de.unifrankfurt.informatik.acoli.fid.types.ModelType;
import de.unifrankfurt.informatik.acoli.fid.types.ParseResult;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceCache;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceFormat;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceState;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceType;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceTypeInfo;
import de.unifrankfurt.informatik.acoli.fid.types.UpdatePolicy;
import de.unifrankfurt.informatik.acoli.fid.types.UserAccount;
import de.unifrankfurt.informatik.acoli.fid.types.VocabularyMatch;
import de.unifrankfurt.informatik.acoli.fid.types.VocabularyType;
import de.unifrankfurt.informatik.acoli.fid.util.IndexUtils;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;
import de.unifrankfurt.informatik.acoli.fid.xml.Template;
import de.unifrankfurt.informatik.acoli.fid.xml.TemplateQuality;


/*
 * Experimental !INCOMPLETE! implementation of ResourceManager that is actually not used, but
 * can be used if implemented properly !
 * 
 * For Neo4j and Tinkergraph
 * Be careful with getters that use vertex objects as arguments
 * - if the vertex object was deleted from the graph
 *   - the vertex object still is present but is invalid (e.g. you cannot retrieve properties !!!)
 */

public class RMEmbedded implements ResourceManager{
	
	Graph graph;
	EmbeddedQuery queries;
	UpdateManager updateManager;
	
	
	public RMEmbedded(Graph graph, UpdatePolicy updatePolicy) {
		
		this.graph = graph;
		this.queries = new EmbeddedQuery(graph);
		this.updateManager = new UpdateManager(this, updatePolicy);
	}
	
	

	public RMEmbedded(String directory, UpdatePolicy updatePolicy) {
					
		this.graph = Neo4jGraph.open(directory);
		this.queries = new EmbeddedQuery(graph);
		this.updateManager = new UpdateManager(this, updatePolicy);
	}
	
	
	@Override
	public Graph getGraph(){
		return this.graph;
	}
	
	@Override
	public EmbeddedQuery getQueries(){
		return this.queries;
	}
	
	/**
	 * Verify if resource is new and create/update entry in the resource database
	 * @param resourceInfo 
	 * @param HttpResponse Header info
	 * @return if true then download resource else skip the resource
	 * @deprecated
	 */
	@Override
	public void registerResource(ResourceInfo resourceInfo, HttpResponse header) {
		
		resourceInfo.setHttpResponseValues(header); // !
		
		// Verify if entry for resource in resource db exists (is actual)
		ResourceState resourceState = updateManager.getResourceState (resourceInfo);
		
		Utils.debug(resourceState.toString());
		
  	    switch (resourceState) {
  	    		
  	    	case ResourceHasNotChanged :
  	    		Utils.debug("is unchanged ... nothing to do");
  	    		return;

  	    	case ResourceHasChanged :
  	    		deleteResource(resourceInfo.getDataURL());
  	    		resourceInfo.setResource(addResource(resourceInfo));
  	    		return;
  	    
  	    	case ResourceNotInDB :
  	    		resourceInfo.setResource(addResource(resourceInfo));
  	    		return;
  	    		
  	    	default :
  	    		Utils.debug("ResourceState : "+resourceState+" not recognized !");
  	    		return;
  	    	}  	    
	}
	

	@Override
	public Vertex addResource(ResourceInfo resourceInfo) {
		
		// Parse HTTP header
		int responseCode = resourceInfo.getHttpResponseCode();
		String lastModified = resourceInfo.getHttpLastModified();
		String contentType = resourceInfo.getHttpContentType();
		Long contentLength = resourceInfo.getHttpContentLength();
		
		String dataUrl = resourceInfo.getDataURL();
		String metaDataUrl = resourceInfo.getMetaDataURL();
		String metaDataUrl2 = resourceInfo.getMetaDataURL2();
		String resourceFormat = resourceInfo.getResourceFormat().toString();
		String detectionType = resourceInfo.getDetectionMethod().toString();

		
		if(dataUrl == null) {
			Utils.debug("Cannot add resource without data url !");
			System.exit(0);
		}
		
		if(resourceFormat == null) {
			Utils.debug("Cannot add resource without resource format !");
			System.exit(0);
		};
		
		if(metaDataUrl == null) {metaDataUrl = "";};
		


		// Create resource vertex
		Vertex v = graph.addVertex(T.label, ResourceVertex,
						ResourceUrl, dataUrl,
						ResourceMetaUrl, metaDataUrl,
						ResourceMetaUrl2, metaDataUrl2,
						Resource4mat, resourceFormat,
						ResourceDetectionType, detectionType,
						ResourceResponseCode,responseCode,
						ResourceContentType,contentType,
						ResourceSize,contentLength,
						ResourceLastModified,lastModified
						);
		
		queries.commit();
		return v;
	}
	
	
	@Override
	public Vertex getResource(String resourceIdentifier) {
		ArrayList <Vertex> resource = 
				queries.genericVertexQuery("g.V().hasLabel('"+ResourceVertex+"').has('"+ResourceUrl+"','"+resourceIdentifier+"')");
		if (resource.isEmpty()) return null;
		else return resource.get(0);
	}
	
	
	@Override
	public boolean resourceExists(String resourceIdentifier) {
		if (getResource(resourceIdentifier) != null) return true;
		else return false;
	}
	
	@Override
	public void deleteResource(String resourceIdentifier) {
		
		// Delete files of resource
		queries.genericDeleteQuery("g.V().hasLabel('"+ResourceVertex+"').has('"+ResourceUrl+"','"+resourceIdentifier+"').out()");
		queries.commit();
		
		// Delete resource
		queries.genericDeleteQuery("g.V().hasLabel('"+ResourceVertex+"').has('"+ResourceUrl+"','"+resourceIdentifier+"')");
		queries.commit();
	
		// Delete HIT and TAG nodes for the resource
		try {
		Executer.getDataDBQueries().deleteHitVertices(resourceIdentifier);
		} catch (Exception e) {
			e.printStackTrace();
			Utils.debug("Ignore in test !");
		}
	}
	
	@Override
	public String getResourceMetaDataURL(String resourceIdentifier) {
		return getResource(resourceIdentifier).value(ResourceMetaUrl);
	}
	
	@Override
	public String getResourceMetaDataURL2(String resourceIdentifier) {
		return getResource(resourceIdentifier).value(ResourceMetaUrl2);
	}

	@Override
	public ResourceFormat getResourceFormat(String resourceIdentifier) {
		return ResourceFormat.valueOf(getResource(resourceIdentifier).value(Resource4mat));
		
	}
	
	
	@Override
	public int getResourceResponseCode(String resourceIdentifier) {
		return (Integer) getResource(resourceIdentifier).value(ResourceResponseCode);
	}
	
	@Override
	public String getResourceContentType(String resourceIdentifier) {
		return getResource(resourceIdentifier).value(ResourceContentType);
	}
	
	@Override
	public String getResourceLastModified(String resourceIdentifier) {
		return getResource(resourceIdentifier).value(ResourceLastModified);
	}
	
	@Override
	public Long getResourceSize(String resourceIdentifier) {
		Long size = 0L;
		try {size = getResource(resourceIdentifier).value(ResourceSize);} catch(Exception e) {};
		return size;
	}
	
	
	
	@Override
	public boolean resourceHadResults(String resourceIdentifier) {
		boolean results = false;
		for (Vertex f : getResourceFiles(resourceIdentifier)) {
			if (((String) f.value(FileStatusCode)).equals(IndexUtils.FoundDocumentsInIndex))
				results = true;break;
		}
		return results;
	}

	
	@Override
	public Vertex addFile(ResourceInfo resourceInfo, ProcessingFormat fileFormat) {
				
		// Create file vertex
		Vertex fileVertex = graph.addVertex(T.label, FileVertex,
						FileId, resourceInfo.getFileInfo().getFileId(),
						FileName, resourceInfo.getFileInfo().getResourceFile().getName(),
						FileSizeInBytes, FileUtils.sizeOf(resourceInfo.getFileInfo().getResourceFile()),
						File4ormat, fileFormat.toString(),
						FileStatusCode,IndexUtils.FoundDocumentsInIndex,
						FileTripleCount,new Long(0),
						FileErrorCode,"",
						FileErrorMsg,"",
						FileLanguageSample,""
						);
		
		resourceInfo.getResource().addEdge(FileEdge, fileVertex);
		queries.commit();
		return fileVertex;
	}
	
	@Override
	public ArrayList <Vertex> getResourceFiles(Vertex resource) {
		
		// Must catch error if resource vertex is invalid after deletion
		try {
		return getResourceFiles((String) resource.value(ResourceUrl));
		} catch (Exception e) {
			return new ArrayList <Vertex> ();
		}

		
		/* // works 
		Iterator <Edge> edges = resource.edges(Direction.OUT, FileEdge);
		ArrayList <Vertex> files = new ArrayList <Vertex> ();
		while (edges.hasNext()) {
			files.add(edges.next().inVertex());
		}
		return files;
		*/
	}
	
	
	@Override
	public ArrayList <Vertex> getResourceFiles(String resourceIdentifier) {
		return queries.genericVertexQuery("g.V().hasLabel('"+ResourceVertex+"').has('"+ResourceUrl+"','"+resourceIdentifier+"').out()");
	}
	
	
	@Override
	public ArrayList <Vertex> getResourceFilesWithHits(String resourceIdentifier) {
		String query = "g.V().hasLabel('"+ResourceVertex+"').has('"+ResourceUrl+"','"+resourceIdentifier+"').outE('"+FileEdge+"').inV()"
				+ ".has('"+FileStatusCode+"','"+IndexUtils.FoundDocumentsInIndex+"')";
		return queries.genericVertexQuery(query);
	}
	
	
	/*@Override
	public Vertex getResourceFile(Vertex resource, File file) {
		
		for (Vertex f : getResourceFiles(resource)) {
			if (f.value(FileName).equals(file.getName()))
				return f;
		}
		return null;
	}*/

	
	@Override
	public void setFileErrorCode(Vertex resource, Vertex file, String errorCode) {
		file.property(FileErrorCode, errorCode);
		queries.commit();
	}


	@Override
	public void setFileErrorMsg(Vertex resource, Vertex file, String errorMsg) {
		file.property(FileErrorMsg, errorMsg);
		queries.commit();
	}


	@Override
	public void setFileStatusCode(Vertex resource, Vertex file, String statusCode) {
		file.property(FileStatusCode, statusCode);
		queries.commit();
	}
	
	
	@Override
	public void setFileTripleCount(Vertex resource, Vertex file, long tripleCount) {
		file.property(FileTripleCount, tripleCount);
		queries.commit();
	}
	
	
	@Override
	public void setFileLanguageSample(Vertex resource, Vertex file,
			String textSample) {
		file.property(FileLanguageSample, IndexUtils.string2Hex(textSample));
		queries.commit();
	}
	
	@Override
	public void setFileSample(Vertex resource, Vertex file, String sample) {
		file.property(FileSample, IndexUtils.string2Hex(sample));
		queries.commit();
	}

	@Override
	public ArrayList <Vertex> getDoneResources() {
		return queries.genericVertexQuery("g.V().hasLabel('"+ResourceVertex+"')");
	}
	
	@Override
	public ArrayList <ResourceInfo> getDoneResourcesRI() {
		return getDoneResourcesRI(false);
	}
	
	@Override
	public HashSet<String> getDoneResourceUrls() {
		ArrayList <Vertex> resources = getDoneResources();
		HashSet <String> result = new HashSet <String>();
		for (Vertex v : resources) {
			result.add(v.value(ResourceManager.ResourceUrl));
		}
		return result;
	}
	
	/*
	 * "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
				+ "SELECT ?g WHERE {GRAPH ?g {"
				+ "?file rdf:type <"+_file+"> ;"
				+ "<"+_statusCode+"> '"+statusCode+"' .}}";
	 */
	/**
	 * Get scanned resources which do contain files with the given status code. In case that a resource
	 * contains multiple files multiple
	 * files per resource
	 * @param statusCode File status code
	 * @param checkAll Return resource only if all files have that status code (if FALSE then return
	 * the resource in case at least one file with that status code exists)
	 * @return Resources that have a file with the given statusCode
	 */
	@Override
	public HashSet<String> getDoneResourcesWithFileWithStatus(String statusCode, boolean checkAll) {
		HashSet<String> result = new HashSet <String>();
		
		if (!checkAll) {
		for (Vertex resource : getDoneResources()) {
			for (Vertex f : getResourceFiles(resource)) {
				if (f.value(FileStatusCode).equals(statusCode))
					result.add(resource.value(ResourceUrl));
					break;
			}
		}
		} else {
		for (Vertex resource : getDoneResources()) {
			boolean allTrue = true;
			for (Vertex f : getResourceFiles(resource)) {
				if (!f.value(FileStatusCode).equals(statusCode))
					allTrue = false;
					break;
			}
			if (allTrue) {
				result.add(resource.value(ResourceUrl));
			}
		}
		}
		
		return result;
	}

	/**
	 * Add a new language from lexvo.org
	 * @param lexvoUrl The lexvo.org URL
	 * @param description Language description
	 */
	@Override
	public Vertex addLanguage(URL lexvoUrl, String description) {
		// Create language vertex
		Vertex LangVertex = graph.addVertex(T.label, LanguageVertex,
						LanguageLexvoUrl, lexvoUrl.toString(),
						LanguageDescription, description
						);
		
		queries.commit();
		return LangVertex;
	}
	
	
	/**
	 * Delete language resource
	 * @param lexvoUrl The lexvo.org URL
	 */
	@Override
	public void deleteLanguage(URL lexvoUrl) {
		String query = "g.V().hasLabel('"+LanguageVertex+"').has('"+LanguageLexvoUrl+"','"+lexvoUrl.toString()+"')";
		// Delete language vertex
		queries.genericDeleteQuery(query);
		queries.commit();
	}
	
	/**
	 * Get a language by lexvo URL or iso-code string value. If both arguments are set then the lexvo URL will be used.
	 * @param lexvoURL
	 */
	@Override
	public Vertex getLanguage(URL lexvoUrl) {
		
		String query = null;
		if (lexvoUrl != null)
			query = "g.V().hasLabel('"+LanguageVertex+"').has('"+LanguageLexvoUrl+"','"+lexvoUrl.toString()+"')";
		else
			return null;
		
		ArrayList <Vertex> language = queries.genericVertexQuery(query);
		
		if (language.isEmpty()) return null;
		else return language.get(0);
	}


	/**
	 * Add all languages found in file
	 * @param fileVertex
	 * @param languages Set of language codes
	 */
	@Override
	public void updateFileLanguages(Vertex resourceVertex, Vertex fileVertex, FileInfo fileInfo) {
		
		
		clearFileLanguages(resourceVertex, fileVertex);
		ArrayList <String> doneLanguages = new ArrayList <String>();
		
		for (LanguageMatch languageMatch : fileInfo.getLanguageMatchings()) {
			
			// don't add same language edge twice
			if (doneLanguages.contains("")) continue;
			else doneLanguages.add(languageMatch.getLanguageISO639Identifier());
			
			Utils.debug("Set file language "+languageMatch.getLanguageISO639Identifier());
			Utils.debug("Lexvo url "+languageMatch.getLexvoUrl());
			
			
			if (languageMatch.getLexvoUrl() != null) {
				//setFileLanguage(resourceVertex, fileVertex, langMatch);
				
				Vertex langVertex = getLanguage(languageMatch.getLexvoUrl());
				
				// Case : language not in database
				if (langVertex == null) {
					langVertex = addLanguage(languageMatch.getLexvoUrl(), "");
				} 
					fileVertex.addEdge(LanguageEdgeLexvo, langVertex,
							Detectionmethod, languageMatch.getDetectionMethod().name(),
							DifferentHitTypes,languageMatch.getDifferentHitTypes(),
							TotalHitCount, languageMatch.getHitCount());
					queries.commit();
				
			}
		}
 		
	}


	
	@Override
	public Vertex addModel(ModelType modelType) {
		
		// Create model vertex
		Vertex modelVertex = graph.addVertex(T.label, ModelVertex,
				Modeltype, modelType.toString());
		
		queries.commit();
		return modelVertex;
	}
	
	
	@Override
	public void deleteModel(ModelType modelType) {
		
		// Delete model vertex
		queries.genericDeleteQuery("g.V().hasLabel('"+ModelVertex+"').has('"+Modeltype+"','"+modelType.toString()+"')");
		queries.commit();
	}
	
	
	/**
	 * Get model vertex for ModelType or null if not existing
	 * @param modelType
	 * @return modelVertex
	 */
	@Override
	public Vertex getModel(ModelType modelType) {
		
		String query = "g.V().hasLabel('"+ModelVertex+"').has('"+Modeltype+"','"+modelType.toString()+"')";
		ArrayList <Vertex> model = queries.genericVertexQuery(query);
		
		if (model.isEmpty()) return null;
		else return model.get(0);
	}


	/**
	 * Add all models found for a file
	 * @param fileVertex
	 * @param modelTypes Set of model types
	 */
	@Override
	public void updateFileModels(Vertex resourceVertex, Vertex fileVertex, FileInfo fileInfo) {
				
		// Clear assigned models of a file
		clearFileModels(resourceVertex, fileVertex);
		
		
		HashSet <ModelType> doneModels = new HashSet <ModelType> ();
		
		for (ModelMatch modelMatch : fileInfo.getModelMatchings()) {
			
			// Do not add edge to same model twice !
			if (doneModels.contains(modelMatch.getModelType())) continue;
			else doneModels.add(modelMatch.getModelType());
			
			Vertex modelVertex = getModel(modelMatch.getModelType());
			
			// Add new model to resource database
			if (modelVertex == null) {
				modelVertex = addModel(modelMatch.getModelType());
			}
			
			// Add edge from file to found model
			fileVertex.addEdge(ModelEdge, modelVertex,
					Detectionmethod, modelMatch.getDetectionMethod().name(),
					DifferentHitTypes, modelMatch.getDifferentHitTypes(),
					TotalHitCount, modelMatch.getHitCountTotal(),
					HitConllColumn, modelMatch.getConllColumn(),
					ModelCoverage, modelMatch.getCoverage()
					);
			queries.commit();
			
		}
 		
	}
	
	
	private void clearFileModels(Vertex resourceVertex, Vertex fileVertex) {
		
		String fileQuery = makeFileQuery(resourceVertex, fileVertex);
		String removeModelEdgeQuery = fileQuery+".outE('"+ModelEdge+"')";
		Utils.debug(removeModelEdgeQuery);
		queries.genericDeleteQuery(removeModelEdgeQuery);
		queries.commit();
	}
	
	
	private void clearFileLanguages(Vertex resourceVertex, Vertex fileVertex) {
		
		String fileQuery = makeFileQuery(resourceVertex, fileVertex);
		String removeEdgeQuery = fileQuery+".outE('"+LanguageEdgeLexvo+"')";
		Utils.debug(removeEdgeQuery);
		queries.genericDeleteQuery(removeEdgeQuery);
		queries.commit();
	}


	@Override
	public ArrayList <Vertex> getFileLanguages(Vertex resource, Vertex file) {
		return (ArrayList<Vertex>) IteratorUtils.toList(file.vertices(Direction.OUT, LanguageEdgeLexvo));
	}


	@Override
	public void closeDb() {
		try {
			this.graph.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}



	/**
	 * Delete the whole database !!!
	 * Use only for test purposes !
	 */
	@Override
	public void deleteDatabase() {
		queries.genericDeleteQuery("g.V()");
		queries.commit();
		queries.genericDeleteQuery("g.E()");
		queries.commit();
	}



	@Override
	public ArrayList<Vertex> getVertices() {
		return queries.genericVertexQuery("g.V()");
	}



	@Override
	public ArrayList<Edge> getEdges() {
		return queries.genericEdgeQuery("g.E()");
	}


	
	@Override
	public ArrayList<Vertex> getFileModels(Vertex resource, Vertex file) {
		
		String fileQuery = makeFileQuery(resource, file);
		String modelQuery = fileQuery+".outE('"+ModelEdge+"').inV()";
		return queries.genericVertexQuery(modelQuery);
		//return getFileModels(resource, new File((String)file.value(ResourceManager.FileName)));
	}
	
	
	
	private String makeFileQuery(Vertex resource, String fileIdentifier) {
		String query = "g.V()"
				+ ".hasLabel('"+ResourceVertex+"')"
				+ ".has('"+ResourceUrl+"','"+resource.value(ResourceUrl)+"')"
				+ ".outE('"+FileEdge+"')"
				+ ".inV()"
				+ ".has('"+FilePathRel+"','"+fileIdentifier+"')";
		
		return query;
	}
	
	
	private String makeFileQuery(Vertex resource, Vertex file) {
		String query = "g.V()"
				+ ".hasLabel('"+ResourceVertex+"')"
				+ ".has('"+ResourceUrl+"','"+resource.value(ResourceUrl)+"')"
				+ ".outE('"+FileEdge+"')"
				+ ".inV()"
				+ ".has('"+FilePathRel+"','"+file.value(ResourceManager.FilePathRel)+"')";
		
		return query;
	}



	/** Check if model is loaded
	 * @param modelType
	 */
	@Override
	public boolean isModelLoaded(ModelType modelType) {
			
		if (!(getModel (modelType) == null)) {
			return true;
		} else
			return false;
	}



	@Override
	public ServerQuery getServerQueries() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public boolean setResourceDataUrl(String dataUrl, String newDataUrl) {
		if (resourceExists(newDataUrl)) return false;
		
		// Code to change dataURL
		return true;
	}

	@Override
	public void setResourceMetaDataUrl(String dataUrl, String metaDataUrl) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setResourceMetaDataUrl2(String dataUrl, String metaDataUrl) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void updateFileModels(String resourceIdentifier, String fileIdentifier, FileInfo fileInfo, boolean forceOverwrite) {
		
		// TODO Get resource vertex and get file vertex with query
		
		// updateFileModels(resourceVertex, fileVertex, modelMatchings)
	}



	@Override
	public void updateFileLanguages(String resourceIdentifier,
			String fileIdentifier, FileInfo fileInfo, boolean force) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public ArrayList<Edge> getModelFileEdge(Vertex resource, Vertex f, Vertex model) {
		
		String fileQuery = makeFileQuery(resource, f);
		String modelFileQuery = fileQuery+".outE('"+ModelEdge+"').as('e').inV().has('"+Modeltype+"','"+model.value(Modeltype)+"').select('e')";
		return queries.genericEdgeQuery(modelFileQuery);
	}



	@Override
	public ArrayList<ResourceInfo> getDoneResourcesRI(boolean onlySuccessfull) {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public Cluster getCluster() {
		return null;
	}



	@Override
	public Vertex addXMLTemplate(Template template) {
		return null;
		
	}



	@Override
	public void removeXMLTemplate(String templateID) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public Template getXMLTemplate(String xmlTemplateIdentifier) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public ArrayList<Template> getAllXMLTemplates() {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public ArrayList<Edge> getLanguageFileEdge(Vertex resource, Vertex f,
			Vertex language) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void setFileFormat(Vertex resource, Vertex file,
			ProcessingFormat fileFormat) {
		// TODO Auto-generated method stub
	}



	@Override
	public void updateProcessState(ResourceInfo resourceInfo) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void updateFileComment(ResourceInfo resourceInfo) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public ArrayList<ResourceInfo> getAllResourceFilesRI() {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public ArrayList<ResourceInfo> getSuccessFullResourceFilesRI() {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public ArrayList<ResourceInfo> getErrorResourceFilesRI() {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public ArrayList<ResourceInfo> getUnSuccessFullResourceFilesRI() {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public void updateFileProcessingStartDate(ResourceInfo resourceInfo) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void updateFileProcessingEndDate(ResourceInfo resourceInfo) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public Edge addXMLTemplateQualityEdge(ResourceInfo resourceInfo, TemplateQuality bestMatch) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public void updateFileVocabularies(Vertex resourceVertex,
			Vertex fileVertex, ArrayList<VocabularyMatch> vocabularyMatches) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void updateFileVocabularies(String resourceIdentifier,
			String fileIdentifier, ArrayList<VocabularyMatch> vocabularyMatches) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public Vertex addVocabulary(VocabularyType vocabulary, String description) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public Vertex getVocabulary(VocabularyType vocabulary) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public ArrayList<Vertex> getFileVocabularies(Vertex resource, Vertex file) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public ArrayList<Edge> getVocabularyFileEdge(Vertex resource, Vertex f,
			Vertex vocabulary) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public Vertex addToken(String token) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public Vertex getToken(String token) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public void updateFileTokens(Vertex resourceVertex, Vertex fileVertex,
			HashMap<Integer, HashMap<String, Long>> tokenMap) {
		// TODO Auto-generated method stub
		
	}
	

	@Override
	public void updateFileTokens(String resourceIdentifier,
			String fileIdentifier,
			HashMap<Integer, HashMap<String, Long>> tokenMap) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public ArrayList<String> getFileTokens(ResourceInfo resourceInfo, int column) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public void updateFileAcceptedDate(ResourceInfo resourceInfo) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void resetResource(String resourceIdentifier, Boolean deleteUnitInfo) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public ArrayList<ResourceInfo> getDoneResourceRI(Vertex resourceVertex) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public Vertex getResourceFile(ResourceInfo resourceInfo) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public Vertex getResourceFile(Vertex resourceVertex, String fileIdentifier) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public Vertex getResourceFile(String resourceIdentifier,
			String fileIdentifier) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public Long getFileBytes(Vertex resource, String fileIdentifier) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public Long getFileTripleCount(Vertex resource, String fileIdentifier) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public String getFileErrorCode(Vertex resource, String fileIdentifier) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public String getFileErrorMsg(Vertex resource, String fileIdentifier) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public String getFileStatusCode(Vertex resource, String fileIdentifier) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public String getFileName(Vertex resource, String fileIdentifer) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public ProcessingFormat getFileFormat(Vertex resource, String fileIdentifier) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public String getFileLanguageSample(Vertex resource, String fileIdentifier) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public Integer getFileId(Vertex resource, String fileIdentifier) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public String getFileSample(Vertex resource, String fileIdentifier) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public Vertex addPredicate(String predicate) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public Vertex getPredicate(String predicate) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public void updateFilePredicates(Vertex resourceVertex, Vertex fileVertex,
			HashMap<String, Boolean> predicateMap) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void updateFilePredicates(String resourceIdentifier,
			String fileIdentifier, HashMap<String, Boolean> predicateMap) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void setPredicateDisabled(String predicateUrl) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void setPredicateEnabled(String predicateUrl) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public HashSet<String> getSuccessfulPredicates() {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public HashSet<String> getUnsuccessfulPredicates() {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public void setPredicateDefault(String predicateUrl) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void initPredicates() {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void setFileSize(Vertex resource, Vertex file, long fileSizeInBytes) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void setFileError(Vertex resource, Vertex file, String errorCode,
			String errorMsg) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public int getDoneResourceCount() {
		// TODO Auto-generated method stub
		return 0;
	}



	@Override
	public int getXMLResourcesWithModelOrLanguageCount() {
		// TODO Auto-generated method stub
		return 0;
	}



	@Override
	public int getXMLResourcesWithSelectedModelsCount() {
		// TODO Auto-generated method stub
		return 0;
	}



	@Override
	public int getXMLResourcesWithSelectedLanguagesCount() {
		// TODO Auto-generated method stub
		return 0;
	}



	@Override
	public int getXMLResourcesWithFileFormatXML() {
		// TODO Auto-generated method stub
		return 0;
	}



	@Override
	public int getXMLResourcesWithUnselectedModelAndLanguageCount() {
		// TODO Auto-generated method stub
		return 0;
	}



	@Override
	public ArrayList<Vertex> getDoneFileResources() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getDoneFileResourceCount() {
		// TODO Auto-generated method stub
		return 0;
	}



	@Override
	public HashSet<String> getGlobalProcessState(ResourceInfo resourceInfo,
			String resourcePrefix) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public ArrayList<Vertex> getResourceMetadata(String resourceIdentifier) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public Vertex addResourceMetadata(ResourceInfo resourceInfo) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public void updateResourceMetadata(ResourceInfo resourceInfo) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void deleteResourceFile(String resourceIdentifier, String relFilePath) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void resetResourceFile(String resourceIdentifier, String relFilePath) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void setFileRelPath(String resourceIdentifier,
			String fileIdentifier, String newFileIdentifier) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void setFileName(String resourceIdentifier, String relFilePath,
			String newFileName) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void setFileAbsPath(String resourceIdentifier, String relFilePath,
			String newFilePath) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public ArrayList<ResourceInfo> getDoneResourceRI(String resourceIdentifier) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public void updateFileModels(ResourceInfo resourceInfo,
			boolean forceOverwrite) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public HashMap<String, Long> getFileTokensWithCount(
			ResourceInfo resourceInfo, int column) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public Boolean updateResourceHeaderData(ResourceInfo resourceInfo) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public void updateFileUnitInfo(ResourceInfo resourceInfo) {
		// TODO Auto-generated method stub
	}



	@Override
	public HashMap<String, Integer> getRdfTokenCounts(ResourceInfo resourceInfo) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public HashMap<String, Integer> getXmlTokenCountsByAttribute(ResourceInfo resourceInfo) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public HashMap<Integer, Integer> getConllTokenCounts(
			ResourceInfo resourceInfo) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public HashMap<String, Integer> getXmlAttributes2ConllColumns(
			ResourceInfo resourceInfo) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public HashMap<Integer, String> getConllColumns2XmlAttributes(
			ResourceInfo resourceInfo) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public HashMap<Integer, Integer> getXmlTokenCountsByColumn(
			ResourceInfo resourceInfo) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public ArrayList<Integer> getFileTokenColumns(ResourceInfo resourceInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public void deleteResourceFileTokens(ResourceInfo resourceInfo, int col) {
		// TODO Auto-generated method stub
		
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getUpdateManager()
	 */
	@Override
	public UpdateManager getUpdateManager() {
		return this.updateManager;
	}


	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#updateUserEmail(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void setUserEmail(String login, String newEmail) {
		// TODO Auto-generated method stub
		
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#updateUserPwd(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void setUserPassword(String login, String newPwd) {
		// TODO Auto-generated method stub
		
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#updateResourceOwner(java.lang.String, java.lang.String)
	 */
	@Override
	public Boolean setResourceOwner(String resourceIdentifier,
			String userLogin) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getUser(java.lang.String)
	 */
	@Override
	public Vertex getUser(String login) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#addResource(de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo, java.lang.String)
	 */
	@Override
	public Vertex addResource(ResourceInfo resourceInfo, String userLogin) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getResourceOwner(java.lang.String)
	 */
	@Override
	public ArrayList<Vertex> getResourceOwners(String resourceIdentifier) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getResourcesOwnedByUser(java.lang.String)
	 */
	@Override
	public ArrayList<Vertex> getResourcesOwnedByUser(String userID) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getDoneResourcesRI(java.util.ArrayList, boolean)
	 */
	@Override
	public ArrayList<ResourceInfo> getDoneResourcesRINoFiles(
			ArrayList<Vertex> resourceVertices) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getDoneResourcesOwnedByUser(java.lang.String)
	 */
	@Override
	public ArrayList<ResourceInfo> getDoneResourcesOwnedByUser(String userID) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#setUserPermissions(int)
	 */
	@Override
	public Boolean setResourcePermissions(String resourceIdentifier, String userID, String permissions) {
		// TODO Auto-generated method stub
		return false;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getUserPermissions(java.lang.String)
	 */
	@Override
	public String getResourcePermissions(String resourceIdentifier) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#addUser(de.unifrankfurt.informatik.acoli.fid.types.UserAccount)
	 */
	@Override
	public Vertex addUser(UserAccount userAccount) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#deleteUser(de.unifrankfurt.informatik.acoli.fid.types.UserAccount)
	 */
	@Override
	public Boolean deleteUser(UserAccount userAccount) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#userExists(java.lang.String)
	 */
	@Override
	public boolean userExists(String userID) {
		// TODO Auto-generated method stub
		return false;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#userExists(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean userExists(String userID, String password) {
		// TODO Auto-generated method stub
		return false;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#selectResource(java.lang.String, java.lang.String)
	 */
	@Override
	public Boolean selectResource(String userID, String resourceIdentifier) {
		// TODO Auto-generated method stub
		return null;
		
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#deselectResource(java.lang.String, java.lang.String)
	 */
	@Override
	public Boolean deselectResource(String userID, String resourceIdentifier) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getSelectedResources(java.lang.String)
	 */
	@Override
	public ArrayList<Vertex> getResourcesSelectedByUser(String userID) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getDoneResourcesSelectedByUser(java.lang.String)
	 */
	@Override
	public ArrayList<ResourceInfo> getDoneResourcesSelectedByUser(String userID) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getResourceOwner(java.lang.String)
	 */
	@Override
	public String getResourceOwner(String resourceIdentifier) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#isResourceSelectedByUser(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean isResourceSelectedByUser(String resourceIdentifier,
			String userID) {
		// TODO Auto-generated method stub
		return false;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#resourcesWithLanguages(java.lang.String[])
	 */
	@Override
	public ArrayList<Vertex> resourcesWithLanguages(Set<String> languages, Boolean andTrueOrFalse, Boolean only) {
		// TODO Auto-generated method stub
		return null;
	}


	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#resourcesWithNameLike(java.lang.String)
	 */
	@Override
	public ArrayList<Vertex> resourcesWithNameLike(String resourceName, Boolean caseSensitive) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#resourcesWithType(de.unifrankfurt.informatik.acoli.fid.types.ResourceType)
	 */
	@Override
	public ArrayList<Vertex> getResourcesWithType(ResourceType resourceType) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#addFileInfo2Resources(java.util.List, java.util.HashSet, boolean)
	 */
	@Override
	public List<ResourceInfo> addFileInfo2Resources(List<ResourceInfo> rfl,
			HashSet<ParseResult> withParseResults) {
		// TODO Auto-generated method stub
		return null;
	}


	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#addResourceType(de.unifrankfurt.informatik.acoli.fid.types.ResourceType)
	 */
	@Override
	public Vertex addResourceType(ResourceType resourceType) {
		// TODO Auto-generated method stub
		return null;
	}


	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getResourceTypes()
	 */
	@Override
	public ArrayList<Vertex> getResourceType() {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getResourceTypeVertex(de.unifrankfurt.informatik.acoli.fid.types.ResourceType)
	 */
	@Override
	public Vertex getResourceType(ResourceType resourceType) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getResourceTypes(java.lang.String)
	 */
	@Override
	public ArrayList<ResourceTypeInfo> getResourceTypeInfo(
			String resourceIdentifier) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#updateResourceTypeInfo(java.lang.String, java.util.ArrayList)
	 */
	@Override
	public void updateResourceTypeInfos(String resourceIdentifier,
			ArrayList<ResourceTypeInfo> resourceTypeInfo) {
		// TODO Auto-generated method stub
		
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#addResourceTypeInfo(java.lang.String, de.unifrankfurt.informatik.acoli.fid.types.ResourceTypeInfo)
	 */
	@Override
	public void addResourceTypeInfo(String dataURL,
			ResourceTypeInfo resourceTypeInfo) {
		// TODO Auto-generated method stub
		
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#addComment(java.lang.String, de.unifrankfurt.informatik.acoli.fid.types.Comment)
	 */
	@Override
	public void addComment(String resourceIdentifier, Comment comment) {
		// TODO Auto-generated method stub
		
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#deleteComment(java.lang.String, int)
	 */
	@Override
	public void deleteComment(String resourceIdentifier, int postId) {
		// TODO Auto-generated method stub
		
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getComment(java.lang.String, int)
	 */
	@Override
	public Comment getComment(String resourceIdentifier, int postId) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getAllComments(java.lang.String)
	 */
	@Override
	public List<Comment> getAllComments(String resourceIdentifier) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getNextCommentId(java.lang.String)
	 */
	@Override
	public Integer getNextCommentId(String resourceIdentifier) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#setUserPrivileges(de.unifrankfurt.informatik.acoli.fid.types.UserAccount)
	 */
	@Override
	public void setUserPrivileges(UserAccount userAccount) {
		// TODO Auto-generated method stub
		
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getUserAccount(java.lang.String)
	 */
	@Override
	public UserAccount getUserAccount(String user) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getDoneResourcesRI(java.util.HashSet)
	 */
	@Override
	public ArrayList<ResourceInfo> getDoneResourcesRI(
			HashSet<ParseResult> withParseResults) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getDoneResourcesRIMap(java.util.HashSet)
	 */
	@Override
	public ResourceCache getDoneResourcesRIMap(
			HashSet<ParseResult> withParseResults) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getDoneResourcesRIMap(java.lang.String)
	 */
	@Override
	public ResourceCache getDoneResourcesRIMap(String resourceIdentifier) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getResourcesOwnedByUserAsUrl(java.lang.String)
	 */
	@Override
	public ArrayList<String> getResourcesOwnedByUserAsUrl(String userID) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getResourcesSelectedByUserAsUrl(java.lang.String)
	 */
	@Override
	public ArrayList<String> getResourcesSelectedByUserAsUrl(String userID) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#deleteResourceTypeInfos(java.lang.String)
	 */
	@Override
	public void deleteResourceTypeInfos(String resourceIdentifier) {
		// TODO Auto-generated method stub
		
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getDoneResourcesAsString()
	 */
	@Override
	public ArrayList<String> getDoneResourcesAsString() {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getResourceWithMetadataLike(java.lang.String)
	 */
	@Override
	public ArrayList<Vertex> getResourcesWithMetadataLike(String queryString, Boolean ignoreCase) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getResourcesWithCommentLike(java.lang.String)
	 */
	@Override
	public ArrayList<Vertex> getResourcesWithCommentLike(String queryString, Boolean ignoreCase) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getResourceFileCountOwnedByUser(java.lang.String)
	 */
	@Override
	public Long getResourceFileCountOwnedByUser(String userID) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getAllUserLogins()
	 */
	@Override
	public ArrayList<String> getAllUserLogins() {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getUserCount(de.unifrankfurt.informatik.acoli.fid.types.AccountType)
	 */
	@Override
	public Long getUserCount(AccountType accountType) {
		// TODO Auto-generated method stub
		return null;
	}


	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#updateUser(de.unifrankfurt.informatik.acoli.fid.types.UserAccount)
	 */
	@Override
	public Vertex updateUser(UserAccount userAccount) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#addBackup(de.unifrankfurt.informatik.acoli.fid.types.Backup)
	 */
	@Override
	public Vertex addBackup(Backup backup) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#deleteBackup(de.unifrankfurt.informatik.acoli.fid.types.Backup)
	 */
	@Override
	public Boolean deleteBackup(Backup backup) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getBackup(de.unifrankfurt.informatik.acoli.fid.types.Backup)
	 */
	@Override
	public Vertex getBackup(Backup backup) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#getBackups()
	 */
	@Override
	public List<Backup> getBackups() {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#backupExists(de.unifrankfurt.informatik.acoli.fid.types.Backup)
	 */
	@Override
	public boolean backupExists(Backup backup) {
		// TODO Auto-generated method stub
		return false;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#setCluster(org.apache.tinkerpop.gremlin.driver.Cluster)
	 */
	@Override
	public void setCluster(Cluster cluster) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public boolean addQueue(String queueName, List<ResourceInfo> resources) {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public List<ResourceInfo> getQueue(String queueName) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void deleteQueue(String queueName) {
		// TODO Auto-generated method stub
		
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#resourcesWithModels(java.util.Set, java.lang.Boolean, java.lang.Boolean)
	 */
	@Override
	public ArrayList<Vertex> resourcesWithModels(Set<String> models,
			Boolean andTrueOrFalse, Boolean findExclusiveModels) {
		return null;
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#updateFileSample(de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo)
	 */
	@Override
	public void updateFileSample(ResourceInfo resourceInfo) {
		// TODO Auto-generated method stub
		
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#updateFileLanguageSample(de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo)
	 */
	@Override
	public void updateFileLanguageSample(ResourceInfo resourceInfo) {
		// TODO Auto-generated method stub
		
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#addFileLanguage(java.lang.String, java.lang.String, de.unifrankfurt.informatik.acoli.fid.types.LanguageMatch)
	 */
	@Override
	public void addFileLanguage(String resourceIdentifier,
			String fileIdentifier, LanguageMatch languageMatch) {
		// TODO Auto-generated method stub
		
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#deleteFileLanguage(java.lang.String, java.lang.String, de.unifrankfurt.informatik.acoli.fid.types.LanguageMatch)
	 */
	@Override
	public void deleteFileLanguage(String resourceIdentifier,
			String fileIdentifier, LanguageMatch languageMatch) {
		// TODO Auto-generated method stub
		
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#addFileModel(java.lang.String, java.lang.String, de.unifrankfurt.informatik.acoli.fid.types.ModelMatch)
	 */
	@Override
	public void addFileModel(String dataURL, String relFilePath,
			ModelMatch model) {
		// TODO Auto-generated method stub
		
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#deleteFileModel(java.lang.String, java.lang.String, de.unifrankfurt.informatik.acoli.fid.types.ModelMatch)
	 */
	@Override
	public void deleteFileModel(String dataURL, String relFilePath,
			ModelMatch model) {
		// TODO Auto-generated method stub
		
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#updateResourceMD5Sha256(de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo)
	 */
	@Override
	public void updateResourceMD5Sha256(ResourceInfo resourceInfo) {
		// TODO Auto-generated method stub
		
	}



	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager#findResourceWithHash(java.lang.String, java.lang.String)
	 */
	@Override
	public ArrayList<Vertex> findResourcesWithHash(String md5, String sha256) {
		// TODO Auto-generated method stub
		return null;
	}

}
