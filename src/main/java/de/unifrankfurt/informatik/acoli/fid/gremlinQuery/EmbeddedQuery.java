package de.unifrankfurt.informatik.acoli.fid.gremlinQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptException;

import org.apache.commons.lang.StringUtils;
import org.apache.tinkerpop.gremlin.groovy.jsr223.GremlinGroovyScriptEngine;
import org.apache.tinkerpop.gremlin.jsr223.GremlinScriptEngine;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.ImmutablePath;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.unifrankfurt.informatik.acoli.fid.exec.Executer;
import de.unifrankfurt.informatik.acoli.fid.owl.ResultUpdater;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager;
import de.unifrankfurt.informatik.acoli.fid.search.ClassMatrixParser;
import de.unifrankfurt.informatik.acoli.fid.search.GWriter;
import de.unifrankfurt.informatik.acoli.fid.types.DetectionMethod;
import de.unifrankfurt.informatik.acoli.fid.types.DetectionSource;
import de.unifrankfurt.informatik.acoli.fid.types.FileResult;
import de.unifrankfurt.informatik.acoli.fid.types.LanguageMatch;
import de.unifrankfurt.informatik.acoli.fid.types.ModelMatch;
import de.unifrankfurt.informatik.acoli.fid.types.ModelType;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceCache;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.util.AnnotationUtil;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;
import de.unifrankfurt.informatik.acoli.fid.webclient.ExecutionBean;
import edu.emory.mathcs.backport.java.util.Collections;

public class EmbeddedQuery {
	
	private GraphTraversalSource g;
	private GremlinScriptEngine gremlinScriptEngine = new GremlinGroovyScriptEngine();
	private Graph graph;
	private String graphN = "g";

	
	public EmbeddedQuery (Graph graph) {
		this.graph = graph;
	}
	
	
	
	public void commit() {	
		if (graph.getClass() == org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph.class) 
			graph.tx().commit();
	}

	
	/**
	 * Get class nodes (model)
	 * @return
	 */
	public ArrayList <Vertex> getClassNodes() {
		String query = graphN+".V().hasLabel('"+GWriter.ClassVertex+"').dedup()";
		return this.genericVertexQuery(query);
	}
	
	
	/**
	 * Get tag nodes (model)
	 * @return
	 */
	public ArrayList <Vertex> getTagNodes() {
		String query = graphN+".V().hasLabel('"+GWriter.TagVertex+"').dedup()";
		return this.genericVertexQuery(query);
	}
	
	
	/**
	 * Get tag nodes (model)
	 * @return
	 */
	public ArrayList <Vertex> getHitNodes() {
		String query = graphN+".V().hasLabel('"+GWriter.HitVertex+"').dedup()";
		return this.genericVertexQuery(query);
	}
	
	
	/**
	 * Get Inter-class edges (model edges)
	 * @return Edges
	 */
	public ArrayList <Edge> getClassEdges() {
		
		g = graph.traversal();
		
		ArrayList <Edge> results = new ArrayList<Edge>();
		Bindings bindings = gremlinScriptEngine.createBindings();
		bindings.put(graphN, g);
		bindings.put("results", results);
		
		String query =  graphN+".E().filter{it.get().inVertex().label().equals('"+GWriter.ClassVertex+"')"
				+ ".and(it.get().outVertex().label().equals('"+GWriter.ClassVertex+"'))}";
		
		try {
			gremlinScriptEngine.eval(query+".fill(results)",bindings);
			return results;
		
		} catch (Exception e) {
			return null;
		}
	}
	
	
	
	/**
	 * Get edges from a HIT vertex to its matching TAG/CLASS vertex
	 * @return Edges
	 */
	public ArrayList <Edge> getHitEdges() {
		
		g = graph.traversal();
		
		ArrayList <Edge> results = new ArrayList<Edge>();
		Bindings bindings = gremlinScriptEngine.createBindings();
		bindings.put(graphN, g);
		bindings.put("results", results);
		
		String query = graphN+".E().filter{it.get().outVertex().label().equals('"+GWriter.HitVertex+"')}";
		
		try {
			gremlinScriptEngine.eval(query+".fill(results)",bindings);
			return results;
		
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	

	
	public Long getModelVertexCount (ModelType model) {	
		return (Long) genericCountQuery(graphN+".V().has('"+GWriter.Model+"','"+model+"').count()");
	}
	
	
	/**
	 * Get count of vertices which have the fileIdentifier in fieldName
	 * @param fileIdentifier
	 * @param fieldName
	 * @return Count
	 */
	public Long getVertexCountByFileName (String fileIdentifier, String fieldName) {
		return (Long) genericCountQuery(graphN+".V().has('"+fieldName+"','"+fileIdentifier+"').count()");
	}
	
	
	/**
	 * Delete all vertices from the model graph that are not HIT vertices or that have a HIT vertex as
	 * a neighbor.
	 */
	public void resetModelGraph() {
		
		// Delete all vertices from model graph that are not HIT vertices or have a HIT vertex as a neighbor
		String deleteModelVertices = 
			"g.V().not(hasLabel('"+GWriter.HitVertex+"'))"
		 + ".filter(inE().outV().hasLabel('"+GWriter.HitVertex+"').count().is(eq(0))).dedup()";

		genericDeleteQuery(deleteModelVertices);
		commit();
	}
	
	
	
	/**
	 * Delete all neighbors of HIT vertices that do not connect to the model graph.
	 * If n is a neighbor of a HIT vertex h and n has no outgoing edge then delete n.
	 * Is part of the model update procedure.
	 */
	public void removeOrphanHits() {
		
		// Delete all HIT vertices that have no outgoing edge (don't match anything) or
		// have neighbors that do not have an outgoing edge. An exception are the ONTOLEX vertices
		// because they don't have a connection to OLiA by default.
		// TODO Are there any other models the behave like ONTOLEX ?
		
		// 1. Delete all neighbors of such HIT vertices
		String deleteOrphanHitNeighbors = 	
			"g.V().hasLabel('"+GWriter.HitVertex+"').out()"
		  + ".not(has('"+GWriter.Model+"','"+ModelType.valueOf("ONTOLEX").name()+"'))"
		  + ".filter(outE().count().is(eq(0)))";
		
		ArrayList<Vertex> orphanHits = genericVertexQuery(deleteOrphanHitNeighbors);
		Utils.debug("orphan hits : "+orphanHits.size());
		/*for (Vertex v : orphanHits) {
			Utils.debug(v.label());
			Utils.debug((String) v.value(GWriter.TagClass));
			Utils.debug((String) v.value(GWriter.TagTag));
			Utils.debug((String) v.value(GWriter.TagModel));
			Utils.debug((String) v.value(GWriter.TagFile));
		}*/
		genericDeleteQuery(deleteOrphanHitNeighbors);
		commit();
	}
	
	
	
	
	
	
	
	
	
	/**
	 * Delete all vertices with the model = modelType attribute
	 * @param modelType
	 * @deprecated Never used because in any update all olia models will be replaced 
	 */
	public void deleteModel (ModelType modelType, GWriter writer) {
		
		Iterator <String> it;
		// delete vertices from vertexMap
		it = writer.getVertexMap().keySet().iterator();

		while(it.hasNext()){
			Vertex v = writer.getVertexMap().get(it.next());
			// TAG hat auch model - einfacher : v.hasLabel('CLASS') anstatt v.keys().contains('model')
			if (v.keys().contains(GWriter.Model) && v.value(GWriter.Model).equals(modelType.toString())) {
				it.remove();
			}
		}
		
		// delete edges from edgeMap
		it = writer.getEdgeMap().keySet().iterator();
		while(it.hasNext()){
			
			Edge e = writer.getEdgeMap().get(it.next());
			Vertex v1 = e.inVertex();
			Vertex v2 = e.outVertex();
			
			if (v1.keys().contains(GWriter.Model) && v1.value(GWriter.Model).equals(modelType.toString())) {
				it.remove();
				continue;
			}
			
			if (v2.keys().contains(GWriter.Model) && v2.value(GWriter.Model).equals(modelType.toString())) {
				it.remove();
			}
		}
		
		// delete vertices from graph
		if (genericDeleteQuery(graphN+".V().has('"+GWriter.Model+"','"+modelType.toString()+"')"));
			Utils.debug("Deleted model : "+modelType.toString()+ " !");	
			commit();
	}
	

	
	/**
	 * Generic query which has a list of vertices as result. (Does NOT COMMIT !!!)
	 * @param query
	 * @return List of vertices
	 */
	public ArrayList <Vertex> genericVertexQuery(String query) {
		
		g = graph.traversal();
		
		ArrayList <Vertex> results = new ArrayList <Vertex>();
		Bindings bindings = gremlinScriptEngine.createBindings();
		bindings.put(graphN, g);
		bindings.put("results", results);
				
		try {
			gremlinScriptEngine.eval(query+".fill(results)" ,bindings);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return results;
	}
	
	
	/**
	 * Generic query which has a list of edges as result. (Does NOT COMMIT !!!)
	 * @param query
	 * @return List of edges
	 */
	public ArrayList<Edge> genericEdgeQuery(String query) {
		
		g = graph.traversal();
		
		ArrayList <Edge> results = new ArrayList <Edge>();
		Bindings bindings = gremlinScriptEngine.createBindings();
		bindings.put(graphN, g);
		bindings.put("results", results);
				
		try {
			gremlinScriptEngine.eval(query+".fill(results)" ,bindings);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return results;
	}
	
	/**
	 * Generic query which has a list of strings as result. (Does NOT COMMIT !!!)
	 * @param query
	 * @return List of strings
	 */
	public ArrayList <String> genericStringQuery(String query) {
		
		g = graph.traversal();
		
		ArrayList <String> results = new ArrayList <String>();
		Bindings bindings = gremlinScriptEngine.createBindings();
		bindings.put(graphN, g);
		bindings.put("results", results);
				
		try {
			gremlinScriptEngine.eval(query+".fill(results)" ,bindings);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return results;
	}
	
	
	
	
	/**
	 * Generic query which has a list of strings as result. (Does NOT COMMIT !!!)
	 * @param query
	 * @return List of strings
	 */
	public ArrayList<Object> genericObjectListQuery(String query) {
		
		g = graph.traversal();
		
		ArrayList<Object> results = new ArrayList<Object>();
		Bindings bindings = gremlinScriptEngine.createBindings();
		bindings.put(graphN, g);
		bindings.put("results", results);
				
		try {
			gremlinScriptEngine.eval(query+".fill(results)" ,bindings);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return results;
	}
	
	
	/**
	 * Generic query which has a list of integers as result. (Does NOT COMMIT !!!)
	 * @param query
	 * @return List of integers
	 */
	public ArrayList <Integer> genericIntegerQuery(String query) {
		
		g = graph.traversal();
		
		ArrayList <Integer> results = new ArrayList <Integer>();
		Bindings bindings = gremlinScriptEngine.createBindings();
		bindings.put(graphN, g);
		bindings.put("results", results);
				
		try {
			gremlinScriptEngine.eval(query+".fill(results)" ,bindings);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return results;
	}
	
	
	
	/**
	 * Generic query which has a map <String, Long> as result. (Does NOT COMMIT !!!)
	 * @param query
	 * @return HashMap <String, Long>
	 */
	public ArrayList <HashMap <String,Long>> genericMapQuery(String query) {
		
		g = graph.traversal();
		
		ArrayList <HashMap <String,Long>> results = new ArrayList <HashMap <String, Long>> ();
		Bindings bindings = gremlinScriptEngine.createBindings();
		bindings.put(graphN, g);
		bindings.put("results", results);

		try {
			gremlinScriptEngine.eval(query+".fill(results)" ,bindings);
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList <HashMap <String, Long>> ();
		}
		
		return results;
	}
	
	
	
	/**
	 * Generic query which has a map <String, Long> as result. (Does NOT COMMIT !!!)
	 * @param query
	 * @return HashMap <String, Long>
	 */
	public ArrayList <HashMap <String,ArrayList<String>>> genericMapQuery2(String query) {
		
		g = graph.traversal();
		
		ArrayList <HashMap <String,ArrayList<String>>> results = new ArrayList <HashMap <String, ArrayList<String>>> ();
		Bindings bindings = gremlinScriptEngine.createBindings();
		bindings.put(graphN, g);
		bindings.put("results", results);

		try {
			gremlinScriptEngine.eval(query+".fill(results)" ,bindings);
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList <HashMap <String, ArrayList<String>>> ();
		}
		
		return results;
	}
	
	
	/**
	 * Generic query for deleting vertices, edges, etc. (with COMMIT)
	 * @param query
	 * @return List of vertices
	 */
	public boolean genericDeleteQuery(String query) {
		
		g = graph.traversal();
		
		Bindings bindings = gremlinScriptEngine.createBindings();
		bindings.put(graphN, g);

		try {
			
			gremlinScriptEngine.eval(query+".drop().iterate()" ,bindings);
			commit();	
			return true;
		
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	
	/**
	 * Generic query which has a Integer or Long value as result. (Does NOT COMMIT !!!)
	 * @param query
	 * @return List of vertices
	 */
	public Object genericCountQuery(String query) {
		
		g = graph.traversal();
		
		ArrayList <Object> results = new ArrayList <Object>();
		Bindings bindings = gremlinScriptEngine.createBindings();
		bindings.put(graphN, g);
		bindings.put("results", results);
				
		try {
			gremlinScriptEngine.eval(query+".fill(results)" ,bindings);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return results.get(0);
	}
	
	
	/**
	 * Generic query which has a boolean value as result. (Does NOT COMMIT !!!)
	 * @param query
	 * @return True or false
	 */
	public boolean genericBooleanQuery(String query) {
		
		g = graph.traversal();
		
		ArrayList <Boolean> results = new ArrayList <Boolean>();
		Bindings bindings = gremlinScriptEngine.createBindings();
		bindings.put(graphN, g);
		bindings.put("results", results);
				
		try {
			gremlinScriptEngine.eval(query+".fill(results)" ,bindings);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return results.get(0);
	}
	
	
	
	/**
	 * Get HITs with type object
	 * @return vertex list
	 */
	public ArrayList <Vertex> getHitsTypeURIObject() {
		String query = graphN+".V().hasLabel('"+GWriter.HitVertex+"').has('"+GWriter.HitType+"','"+GWriter.HitTypeURIObject+"')";
		return genericVertexQuery(query);
	}
	
	/**
	 * Get HITs with type predicate
	 * @return vertex list
	 */
	public ArrayList <Vertex> getHitsTypeLiteralObject() {
		String query = graphN+".V().hasLabel('"+GWriter.HitVertex+"').has('"+GWriter.HitType+"','"+GWriter.HitTypeLiteralObject+"')";
		return genericVertexQuery(query);
	}
	
	/**
	 * Get HITs with type tag
	 * @return vertex list
	 */
	public ArrayList <Vertex> getHitsTypeTag() {
		String query = graphN+".V().hasLabel('"+GWriter.HitVertex+"').has('"+GWriter.HitType+"','"+GWriter.HitTypeTag+"')";
		return genericVertexQuery(query);
	}
	
	
	/**
	 * Get HITs with type feature
	 * @return vertex list
	 */
	public ArrayList <Vertex> getHitsTypeFeature() {
		String query = graphN+".V().hasLabel('"+GWriter.HitVertex+"').has('"+GWriter.HitType+"','"+GWriter.HitTypeFeature+"')";
		return genericVertexQuery(query);
	}
	
	
	
	/**
	 * Get HITs with type tag
	 * @return vertex list
	 */
	public ArrayList <Vertex> getHitsForResource(ResourceInfo resourceInfo) {
		String query = 
				  graphN+".V().hasLabel('"+GWriter.HitVertex+"')"
				  + ".has('"+GWriter.HitResourceUrl+"','"+resourceInfo.getDataURL()+"')"
				  + ".has('"+GWriter.HitFileId+"','"+resourceInfo.getFileInfo().getFileId()+"')";
		return genericVertexQuery(query);
	}
	
	
	
	/**
	 * Get TAGs of model
	 * @param modelType
	 * @return Vertex list
	 */
	public ArrayList <Vertex> getTagsOfModel(ModelType modelType) {
		String query = graphN+".V().hasLabel('"+GWriter.TagVertex+"').has('"+GWriter.TagModel+"','"+modelType.toString()+"')";
		return genericVertexQuery(query);
	}
	

	/**
	 * Get the highest used ID for a vertex or an edge (only TinkerGraph)
	 * @return MaxID
	 */
	public Integer getMaxVertexOrEdgeId() {
		Integer vMax = (Integer) genericCountQuery(graphN+".V().id().max()");
		Integer eMax = (Integer) genericCountQuery(graphN+".E().id().max()");
		return Math.max(vMax,eMax);
	}
	
	
	
	/**
	 * Delete all HIT vertices that do not match anything. Please use carefully because
	 * results that may connect to a future AM will be lost.
	 * @param resourceIdentifier
	 */
	public void deleteAllUnconnectedHitVertices() {
		
		// alternative query :
		// g.V().hasLabel('HIT').filter(outE().count().is(eq(0)))
		
		String deleteQuery = 
				graphN+".V().hasLabel('"+GWriter.HitVertex+"')"
			  + ".not(outE("
			  + "'"+GWriter.ClassMatchEdge+"',"
			  + "'"+GWriter.TagMatchEdge+"',"
			  + "'"+GWriter.FeatureMatchEdge+"'))";
		
		genericDeleteQuery(deleteQuery);
		commit();
	}
	
	/**
	 * Delete HIT vertices that did not match anything
	 * @param resourceIdentifier
	 */
	public void deleteUnconnectedHitVertices(String resourceIdentifier) {
		
		String deleteQuery = 
				graphN+".V().hasLabel('"+GWriter.HitVertex+"')"
			  + ".has('"+GWriter.HitResourceUrl+"','"+resourceIdentifier+"')"
			  + ".not(outE("
			  + "'"+GWriter.ClassMatchEdge+"',"
			  + "'"+GWriter.TagMatchEdge+"',"
			  + "'"+GWriter.FeatureMatchEdge+"'))";
		
		genericDeleteQuery(deleteQuery);
	}


	/**
	 * Delete HIT vertices for all files of a resource
	 * @param resourceIdentifier
	 */
	public void deleteHitVertices(String resourceIdentifier) {
		
		String deleteQuery = 
				graphN+".V().hasLabel('"+GWriter.HitVertex+"')"
			  + ".has('"+GWriter.HitResourceUrl+"','"+resourceIdentifier+"')";
		genericDeleteQuery(deleteQuery);
		commit();
		
	}
	
	/**
	 * Delete HIT vertices for a single file of a resource
	 * @param resourceIdentifier
	 * @param relFilePath
	 */
	public void deleteHitVertices(String resourceIdentifier, String relFilePath) {
		
		String deleteQuery = 
				graphN+".V().hasLabel('"+GWriter.HitVertex+"')"
			  + ".has('"+GWriter.HitResourceUrl+"','"+resourceIdentifier+"')"
			  + ".outE('"+ResourceManager.FileEdge+"').inV().has('"+ResourceManager.FilePathRel+"','"+relFilePath+"')";
		
		genericDeleteQuery(deleteQuery);
		commit();
		
	}
	
	
	
	public void updateHitDataUrl(String dataUrlOld, String dataUrlNew) {
				
		String query = "g.V().hasLabel('"+GWriter.HitVertex+"')"
				  + ".has('"+GWriter.HitResourceUrl+"','"+dataUrlOld+"')"
				  + ".property('"+GWriter.HitResourceUrl+"','"+dataUrlNew+"')";
		
		genericVertexQuery(query);
		commit();
	}
	
	

	/**
	 * Get matching models </b>
	 * for a column CONLL resource </b>
	 * for a property of a RDF resource </b>
	 * for a attribute of a XML resource </b>
	 * @param writer
	 * @param resourceInfo
	 * @param col CONLL column
	 * @param propOrAttr RDF property or XML Attribute
	 * @param colPropAttrValues Number of different values for CONLL column,RDF property or XML attribute
	 * @return found models
	 */
	public ArrayList <ModelMatch> getModelMatchingsNew(		
		GWriter writer, ResourceInfo resourceInfo, Integer col, String propOrAttr, Integer colPropAttrValues) {
		
		Utils.debug("get model matchings new");
		
		boolean showLog = true;

		ArrayList <ModelMatch> foundModelsForColumn = new ArrayList <ModelMatch>();
		
		String langQuery = "";
		String colPropAttrQuery="";
		if (resourceInfo.getFileInfo().isConllFile()) {
		//if (col > 0) {
			if (!resourceInfo.getFileInfo().getLanguageMatchings().isEmpty()) {
				for (LanguageMatch lm : resourceInfo.getFileInfo().getLanguageMatchings()) {
					langQuery+="has('"+GWriter.ClassLang639+"','"+lm.getLanguageISO639Identifier()+"'),";
				}
				langQuery = ".or("+langQuery+"has('"+GWriter.ClassLang639+"','')"+")";
			} else {
				langQuery = "has('"+GWriter.ClassLang639+"','')";
			}
			
			colPropAttrQuery=".has('"+GWriter.HitConllColumn+"',"+col+")";
		}
		
		
		if (resourceInfo.getFileInfo().isRDFFile()) {
			colPropAttrQuery=".has('"+GWriter.HitPredicate+"','"+propOrAttr+"')";
		}
		
		
		if (resourceInfo.getFileInfo().isXMLFile()) {
			//colPropAttrQuery=".has('"+GWriter.HitPredicate+"','"+propOrAttr+"')";
		}
		
		
		String queryDifferentHitTypes = 
				graphN+".V().hasLabel('"+GWriter.HitVertex+"')"
			  + ".has('"+GWriter.HitResourceUrl+"','"+resourceInfo.getDataURL()+"')"
			  + ".has('"+GWriter.HitFileId+"','"+resourceInfo.getFileInfo().getFileId()+"')"
			  + colPropAttrQuery
			  + ".out()"
			  + langQuery
			  + ".dedup()"	// catch error : multiple edges from hit to same model node ??
			  + ".groupCount('a')"
			  + ".by('"+GWriter.Model+"')"
			  + ".cap('a')";
		
		
		String queryDifferentHitTags = 
				graphN+".V().hasLabel('"+GWriter.HitVertex+"')"
			  + ".has('"+GWriter.HitResourceUrl+"','"+resourceInfo.getDataURL()+"')"
			  + ".has('"+GWriter.HitFileId+"','"+resourceInfo.getFileInfo().getFileId()+"')"
			  + colPropAttrQuery
			  + ".out().hasLabel('"+GWriter.TagVertex+"')"
			  + langQuery
			  + ".dedup()"	// catch error : multiple edges from hit to same model node ??
			  + ".group()"
			  + ".by('"+GWriter.Model+"')"
		      + ".by('"+GWriter.TagTag+"')";
		
		//Utils.debug(queryDifferentHitTags);
		
		// TODO this number only represents those tags in the conll column that have been found in any known AM. The actual count
		// of different tags in that conll column may be larger !
		String queryColumnHits = 
					graphN+".V().hasLabel('"+GWriter.HitVertex+"')"
				  + ".has('"+GWriter.HitResourceUrl+"','"+resourceInfo.getDataURL()+"')"
				  + ".has('"+GWriter.HitFileId+"','"+resourceInfo.getFileInfo().getFileId()+"')"
				  + colPropAttrQuery
				  + ".count()";
			
			// Query exclusive hit count : HashMap <Model,exclusive hit count>
			String queryExclusiveHitTypes = 
					graphN+".V().hasLabel('"+GWriter.HitVertex+"')"
				  + ".has('"+GWriter.HitResourceUrl+"','"+resourceInfo.getDataURL()+"')"
				  + ".has('"+GWriter.HitFileId+"','"+resourceInfo.getFileInfo().getFileId()+"')"
				  + colPropAttrQuery
				  + ".filter(outE().count().is(1))"
				  + ".out()"
				  + langQuery
				  + ".dedup()"	// catch error : multiple edges from hit to same model node ??
				  + ".groupCount('a')"
				  + ".by('"+GWriter.Model+"')"
				  + ".cap('a')";
			
			Utils.debug(queryExclusiveHitTypes);

			
			String queryExclusiveHitCount = 
					graphN+".V().hasLabel('"+GWriter.HitVertex+"')"
				  + ".has('"+GWriter.HitResourceUrl+"','"+resourceInfo.getDataURL()+"')"
				  + ".has('"+GWriter.HitFileId+"','"+resourceInfo.getFileInfo().getFileId()+"')"
				  + colPropAttrQuery
				  + ".filter(outE().count().is(1))"
				  + ".as('h')"
				  + ".out()"
				  + langQuery
				  + ".dedup()"	// catch error : multiple edges from hit to same model node ??
				  + ".group()"
				  + ".by('"+GWriter.Model+"')"
				  + ".by(select('h').by('"+GWriter.HitCount+"').sum())";
			
			//Utils.debug(queryExclusiveHitCount);
			
			String queryHitCountTotal = 
					graphN+".V().hasLabel('"+GWriter.HitVertex+"')"
				  + ".has('"+GWriter.HitResourceUrl+"','"+resourceInfo.getDataURL()+"')"
				  + ".has('"+GWriter.HitFileId+"','"+resourceInfo.getFileInfo().getFileId()+"')"
				  + colPropAttrQuery
				  + ".as('h')"
				  + ".out()"
				  + langQuery
				  + ".dedup()"	// catch error : multiple edges from hit to same model node ??
				  + ".group()"
				  + ".by('"+GWriter.Model+"')"
				  + ".by(select('h').by('"+GWriter.HitCount+"').sum())";
				 
			//Utils.debug(queryHitCountTotal);

		
		Long columnHitCount = (Long) writer.getQueries().genericCountQuery(queryColumnHits);
		ArrayList<HashMap<String, Long>> differentHitTypes = writer.getQueries().genericMapQuery(queryDifferentHitTypes);
		ArrayList<HashMap<String, Long>> exclusiveHitTypes = writer.getQueries().genericMapQuery(queryExclusiveHitTypes);
		ArrayList<HashMap<String, Long>> hitCountTotal = writer.getQueries().genericMapQuery(queryHitCountTotal);
		ArrayList<HashMap<String, Long>> exclusiveHitCount = writer.getQueries().genericMapQuery(queryExclusiveHitCount);
		ArrayList<HashMap<String, ArrayList<String>>> differentHitTags = writer.getQueries().genericMapQuery2(queryDifferentHitTags);
		/*if (!differentHitTags.isEmpty()) {
			Utils.debug("hello "+differentHitTags.get(0).size());
			for (String mod : differentHitTags.get(0).keySet()) {
				Utils.debug("model: "+mod);
				for (String ta : differentHitTags.get(0).get(mod)) {
					Utils.debug(ta);
				}
			}
		}*/


		/*
		Utils.debug("Result for column : "+col);
		Utils.debug("Tags for column : "+ columnHitCount);
		*/
		int modelsDCount = 0;
		for (HashMap<String,Long> modelCounts : differentHitTypes) {
			if (showLog)
			Utils.debug("modelsD ");
			modelsDCount++;
			
			for (String modelType : modelCounts.keySet()) {
				
				
				// hits connected to something not a model vertex
				if (modelType.isEmpty()) {
					if (showLog)
					Utils.debug(modelCounts.get(modelType));
					continue;
				}
				
				if (showLog)
				Utils.debug("modelType : "+modelType);
				ModelMatch modelMatch = new ModelMatch(ModelType.valueOf(modelType), 0L, 0L, DetectionMethod.AUTO);
				modelMatch.setConllColumn(col);
				
				modelMatch.setTotalTokenCount(colPropAttrValues);
				
				if(resourceInfo.getFileInfo().isRDFFile()) {
					modelMatch.setRdfProperty(propOrAttr);
				}
				modelMatch.setDifferentHitTypes(modelCounts.get(modelType));
				if (showLog)
				Utils.debug("setDifferentHitTypes : "+modelCounts.get(modelType));
				
				// Find special properties of the found tags
				if(!differentHitTags.isEmpty()) {				
					if (differentHitTags.get(0).containsKey(modelType)) {
						modelMatch.computeTagProperties(differentHitTags.get(0).get(modelType));
					}
				}
				
				
				// Compute coverage with hits per model / number of different values
				if (showLog) {
					Utils.debug("hello "+modelType+" "+col);
					Utils.debug(1.0f * modelCounts.get(modelType) / colPropAttrValues);
					Utils.debug(modelCounts.get(modelType));
					Utils.debug(colPropAttrValues);
				}
				/*
				hello MAMBA 1
				2.9236916E-4
				3
				10261
				*/
				
				modelMatch.setCoverage(1.0f * modelCounts.get(modelType) / colPropAttrValues);
				//modelMatch.setCoverage(1.0f * modelCounts.get(modelType) / columnHitCount); // old
				
				// Set xml attributes
				if (resourceInfo.getFileInfo().getConllcolumn2XMLAttr().containsKey(col)) {
					modelMatch.setXmlAttribute(resourceInfo.getFileInfo().getConllcolumn2XMLAttr().get(col));
				} else {
					if (col != ModelMatch.NOCOLUMN) {
						if (showLog)
						Utils.debug("Error : could not find XMLAttr column "+col+" in conllColumn2XMLAttr map (ignore for non-XML files !)");
					}
				}

				
				/*if (!resourceInfo.getFileInfo().getColumnTokens().isEmpty()) {
					int diffColTypes = resourceInfo.getFileInfo().getColumnTokens().get(col).keySet().size();
					modelMatch.setCoverage(1.0f * modelCounts.get(modelType) / diffColTypes);
				} else {
				//old coverage computation : only RDF ?
					modelMatch.setCoverage(1.0f * modelCounts.get(modelType) / columnHitCount);
				}*/
				
				/*Utils.debug("*+*");
				Utils.debug("col :"+col);
				Utils.debug("model : "+modelType);
				Utils.debug("diffColTypes : "+diffColTypes);
				Utils.debug("colHitCount : "+columnHitCount);
				Utils.debug("\n");*/
				modelMatch.setHitCountTotal(hitCountTotal.get(0).get(modelType));
				if (showLog)
				Utils.debug("hitCountTotal : "+hitCountTotal.get(0).get(modelType));
				if (!exclusiveHitTypes.isEmpty() && exclusiveHitTypes.get(0).containsKey(modelType)) {
					modelMatch.setExclusiveHitTypes(exclusiveHitTypes.get(0).get(modelType));
					if (showLog)
					Utils.debug("exclusiveHitTypes : "+exclusiveHitTypes.get(0).get(modelType));
				} else {
					if (showLog)
					Utils.debug("exclusiveHitTypes : "+0);
				}
				if (!exclusiveHitCount.isEmpty() && exclusiveHitCount.get(0).containsKey(modelType)) {
					modelMatch.setExclusiveHitCountTotal(exclusiveHitCount.get(0).get(modelType));
					if (showLog)
					Utils.debug("exclusiveHitCount : "+exclusiveHitCount.get(0).get(modelType));
				} else {
					if (showLog)
					Utils.debug("exclusiveHitCount : "+0);
				}
				foundModelsForColumn.add(modelMatch);
			}
			Collections.sort(foundModelsForColumn, Collections.reverseOrder());
			Utils.debug("Sorted models");
			for (ModelMatch sorted : foundModelsForColumn) {
				if (showLog) {
					Utils.debug(sorted.getConllColumn());
					Utils.debug(sorted.getModelType().name());
					Utils.debug(sorted.getCoverage());
				}
			}
		}
		
		if (showLog)
		Utils.debug("modelsD "+modelsDCount);
	
		Utils.debug("debug :");
		for (ModelMatch mm : foundModelsForColumn) {
			Utils.debug(ResultUpdater.computeModelMatchHashCode(mm));
		}
		return foundModelsForColumn;
	}
	
	
	
	/**
	 * Retrieve all associated models for a specific TAG string of a hit
	 * @param resourceInfo
	 * @param TAG string
	 * @return Matching Model information
	 * @deprecated
	 */ 
	public ArrayList <ModelMatch> getModelNamesForTagHit(ResourceInfo resourceInfo, String tag) {
		
		ArrayList <ModelMatch> modelMatchings = new ArrayList <ModelMatch>();
		
		String queryModels = 
					  graphN+".V().hasLabel('"+GWriter.HitVertex+"')"
					  + ".has('"+GWriter.HitResourceUrl+"','"+resourceInfo.getDataURL()+"')"
					  + ".has('"+GWriter.HitFileId+"','"+resourceInfo.getFileInfo().getFileId()+"')"
					  + ".has('"+GWriter.HitType+"','t')"
					  + ".has('"+GWriter.HitTag+"','"+tag+"')"
					  + ".outE('"+GWriter.TagMatchEdge+"')"
					  + ".inV()"
					  + ".hasLabel('"+GWriter.TagVertex+"')"
					  + ".values('"+GWriter.TagModel+"')"
					  + ".dedup()";

		
		ArrayList <String> modelsForTag = genericStringQuery(queryModels);
		if (modelsForTag.size() == 0) return modelMatchings;
		
		for (String modelName : modelsForTag) {
			try {
				modelMatchings.add(new ModelMatch(ModelType.valueOf(modelName)));
				} 
			catch (Exception e) {e.printStackTrace();}
		}
		
		return modelMatchings;
	}
	
	
	/**
	 * Retrieve all associated models for a specific TAG string of a hit
	 * @param resourceInfo
	 * @param TAG string
	 * @return Matching Model information
	 * @deprecated
	 */
	public ArrayList <ModelMatch> getModelNamesForFeatureHit(ResourceInfo resourceInfo, String featureName, String featureValue) {
		
		ArrayList <ModelMatch> modelMatchings = new ArrayList <ModelMatch>();
		
		String queryModels = 
					  graphN+".V().hasLabel('"+GWriter.HitVertex+"')"
					  + ".has('"+GWriter.HitResourceUrl+"','"+resourceInfo.getDataURL()+"')"
					  + ".has('"+GWriter.HitFileId+"','"+resourceInfo.getFileInfo().getFileId()+"')"
					  + ".has('"+GWriter.HitType+"','f')"
					  + ".has('"+GWriter.HitFeature+"','"+featureName+"')"
					  + ".has('"+GWriter.HitFeatureValue+"','"+featureValue+"')"
					  + ".outE('"+GWriter.FeatureMatchEdge+"')"
					  + ".inV()"
					  + ".values('"+GWriter.ClassModel+"')"
					  + ".dedup()";

		
		ArrayList <String> modelsForTag = genericStringQuery(queryModels);
		if (modelsForTag.size() == 0) return modelMatchings;
		
		for (String modelName : modelsForTag) {
			try {
				modelMatchings.add(new ModelMatch(ModelType.valueOf(modelName)));
				} 
			catch (Exception e) {e.printStackTrace();}
		}
		
		return modelMatchings;
	}
	
	
	/**
	 * Retrieve all associated models for a specific pair (feature name, feature value)
	 * (e.g. the UD example (VerbForm,Fin) as taken from a CONLL column VerbForm=Fin) 
	 * @param resourceInfo
	 * @param Feature name
	 * @param Feature value 
	 * @return Matching Model information
	 * @deprecated
	 */
	public ArrayList <ModelMatch> getModelNamesForFeature(ResourceInfo resourceInfo, String feature, String featureValue) {
		
		ArrayList <ModelMatch> modelMatchings = new ArrayList <ModelMatch>();		
		String queryModels = 
					  graphN+".V().hasLabel('"+GWriter.ClassVertex+"')"
					  + ".or("
					  + "has('"+GWriter.ClassLabel1+"','"+featureValue+"'),"
					  + "has('"+GWriter.ClassLabel2+"','"+featureValue+"'),"
					  + "has('"+GWriter.ClassLabel3+"','"+featureValue+"')"
					  + ")"
					  + ".as('v')"
					  + ".until(filter{it.get().value('"+GWriter.ClassVertex+"').endsWith('"+feature+"')})"
					  + ".repeat(out()"
					  + ".until(and(hasLabel('"+GWriter.ClassVertex+"'),has('"+GWriter.OliaTier+"','"+GWriter.OliaTierFeats+"')))"
					  + ".repeat(out())"
					  + ".select('v')"
					  + ".values('"+GWriter.ClassModel+"')"
					  + ".dedup()";

		
		ArrayList <String> modelsForTag = genericStringQuery(queryModels);
		if (modelsForTag.size() == 0) return modelMatchings;
		
		for (String modelName : modelsForTag) {
			try {
				modelMatchings.add(new ModelMatch(ModelType.valueOf(modelName)));
				} 
			catch (Exception e) {e.printStackTrace();}
		}
		
		return modelMatchings;
	}

	
	
	/**
	 * Retrieve all associated tags in models for a specific TAG string of a hit
	 * @param resourceInfo
	 * @param TAG string
	 * @return Matching Model information
	 * @deprecated
	 */
	public ArrayList <String> getModelTagsForTagHit(ResourceInfo resourceInfo, String tag, ModelType modelType) {
		
		ArrayList <String> matchingTags = new ArrayList <String>();
		
		String queryModels = 
					  graphN+".V().hasLabel('"+GWriter.HitVertex+"')"
					  + ".has('"+GWriter.HitResourceUrl+"','"+resourceInfo.getDataURL()+"')"
					  + ".has('"+GWriter.HitFileId+"','"+resourceInfo.getFileInfo().getFileId()+"')"
					  + ".has('"+GWriter.HitType+"','t')"
					  + ".has('"+GWriter.HitTag+"','"+tag+"')"
					  + ".outE('"+GWriter.TagMatchEdge+"')"
					  + ".inV()"
					  + ".hasLabel('"+GWriter.TagVertex+"')"
					  + ".has('"+GWriter.TagModel+"','"+modelType.toString()+"')";

		
		ArrayList<Vertex> modelsForTag = genericVertexQuery(queryModels);
		if (modelsForTag.size() == 0) return matchingTags;
		
		for (Vertex tagVertex : modelsForTag) {
			try {
				matchingTags.add((String) tagVertex.value(GWriter.TagTag) +" "+ (String) tagVertex.value(GWriter.TagClass));
				} 
			catch (Exception e) {e.printStackTrace();}
		}
		
		return matchingTags;
	}
	
	
	/**
	 * Retrieve all URLs of matching tags and classes of a given model that occur in a resource
	 * @param resourceInfo
	 * @param model
	 * @return URLs of tags and classes as String together with its type TAG/CLASS
	 */
	public ArrayList <FileResult> getTagsOfModelForResource(ResourceInfo resourceInfo, ModelMatch modelMatch) {
		
		ArrayList <FileResult> fileResultList = new ArrayList <FileResult>();
		String queryModels;
		
		// Languages
		String langQuery = "";
		if (!resourceInfo.getFileInfo().getLanguageMatchings().isEmpty()) {
			int argCount = 0; // TODO maybe increase gremlin argument limit ?
			for (LanguageMatch lm : resourceInfo.getFileInfo().getLanguageMatchings()) {
				langQuery+="has('"+GWriter.ClassLang639+"','"+lm.getLanguageISO639Identifier()+"'),";
				if (argCount++ > 200) break; // Gremlin argument limit is 255 !
			}
			langQuery = ".or("+langQuery+"has('"+GWriter.ClassLang639+"','')"+")";
		} else {
			langQuery = "has('"+GWriter.ClassLang639+"','')";
		}
		
		
		// Query CONLL resource
		if (resourceInfo.getFileInfo().isConllFile()) {
		//if (modelMatch.getConllColumn() > 0) {
		queryModels = 
					  graphN+".V().hasLabel('"+GWriter.HitVertex+"')"
					  + ".has('"+GWriter.HitResourceUrl+"','"+resourceInfo.getDataURL()+"')"
					  + ".has('"+GWriter.HitFileId+"','"+resourceInfo.getFileInfo().getFileId()+"')"
					  + ".has('"+GWriter.HitConllColumn+"',"+modelMatch.getConllColumn()+")" // difference
					  + ".as('h')"
					  + ".outE()"
					  + ".inV()"
					  + ".has('"+GWriter.Model+"','"+modelMatch.getModelType().toString()+"')"
					  + langQuery
					  + ".dedup()"
					  + ".as('m')"
					  + ".select('h','m')";
		} else {
		// Query RDF resource
		queryModels = 
				  graphN+".V().hasLabel('"+GWriter.HitVertex+"')"
				  + ".has('"+GWriter.HitResourceUrl+"','"+resourceInfo.getDataURL()+"')"
				  + ".has('"+GWriter.HitFileId+"','"+resourceInfo.getFileInfo().getFileId()+"')"
				  + ".has('"+GWriter.HitPredicate+"','"+modelMatch.getRdfProperty().toString()+"')"
				  + ".as('h')"
				  + ".outE()"
				  + ".inV()"
				  + ".has('"+GWriter.Model+"','"+modelMatch.getModelType().toString()+"')"
				  + langQuery
				  + ".dedup()"
				  + ".as('m')"
				  + ".select('h','m')";	
		}
		
		
		
		
		
			/*Utils.debug("hello");
			Utils.debug(resourceInfo.getResourceFormat());
			Utils.debug(modelMatch.getConllColumn());
			Utils.debug(queryModels);*/
		
		
		
		try {
			
		ArrayList <LinkedHashMap <String, Vertex>> results = new ArrayList <LinkedHashMap <String, Vertex>>();
		Bindings bindings = gremlinScriptEngine.createBindings();
		bindings.put(graphN, g);
		bindings.put("results", results);
		gremlinScriptEngine.eval(queryModels+".fill(results)",bindings);
		
		// map from hit -> tag/class with model
		for (LinkedHashMap <String, Vertex> map : results) {
			
			FileResult fileResult = new FileResult();
			for (String hit : map.keySet()) {
				switch (hit) {
				
				case "h" : // TODO what ? + use variable instead of string
					//Utils.debug("h: "+map.get(hit).value(GWriter.HitResourceUrl));
					switch ((String) map.get(hit).value(GWriter.HitType)) {
					
					
					case GWriter.HitTypeTag :	// Tag
						//Utils.debug("h: "+map.get(hit).value(GWriter.HitTag));
						fileResult.setFoundTagOrClass((String) map.get(hit).value(GWriter.HitTag));
						break;
						
					case GWriter.HitTypeFeature : // Feature
						//Utils.debug("h: "+map.get(hit).value(GWriter.HitTag));
						fileResult.setFoundTagOrClass((String) map.get(hit).value(GWriter.HitFeatureValue));
						fileResult.setFeatureName((String) map.get(hit).value(GWriter.HitFeature));
						break;
					
					case GWriter.HitTypeLiteralObject : // Literal previously p
						fileResult.setFoundTagOrClass((String) map.get(hit).value(GWriter.HitObject));
						fileResult.setPropertyOrAttribute((String) map.get(hit).value(GWriter.HitPredicate));
						break;
						
					case GWriter.HitTypeURIObject : // URL previously o
						fileResult.setFoundTagOrClass((String) map.get(hit).value(GWriter.HitObject));
						fileResult.setPropertyOrAttribute((String) map.get(hit).value(GWriter.HitPredicate));
						break;
						
					case GWriter.HitTypeXML : // XML not implemented
						break;
						
					default : // no default
						/*
						Utils.debug("h: "+map.get(hit).value(GWriter.HitResourceUrl));
						Utils.debug("h: "+map.get(hit).value(GWriter.HitFileId));
						Utils.debug("h: "+map.get(hit).value(GWriter.HitLiteral));
						Utils.debug("h: "+map.get(hit).value(GWriter.HitUrl));
						*/
						//fileResult.setFoundTagOrClass((String) map.get(hit).value(GWriter.HitObject));
						break;
					}
					
					//Utils.debug("h: "+map.get(hit).value(GWriter.HitCount));
					fileResult.setMatchCount((Long) map.get(hit).value(GWriter.HitCount));
					break;
				
				case "m" : // TODO what ? + use variable instead of string
					try {
					/*
					Utils.debug("m: "+map.get(hit).value(GWriter.Class));
					Utils.debug("m: "+map.get(hit).value(GWriter.ClassModel));
					Utils.debug("m: "+map.get(hit).value(GWriter.ClassFile));
					*/
					fileResult.setMatchingTagOrClass((String) map.get(hit).value(GWriter.ClassClass));
					fileResult.setMatchType((String) map.get(hit).label());

					} catch (Exception e) {}
					break;
					
				default :
				}
			}
			
			fileResultList.add(fileResult);
		}
		
		return fileResultList;
		
		} catch (Exception e) {
			e.printStackTrace();
			Utils.debug("errorby:"+queryModels);
			return null;
		}
		
		
		/*
		HashMap <String, String> tagOrClassMatch = new HashMap <String, String>();
		ArrayList <Vertex> tagOrClassVertex = genericVertexQuery(queryModels);
		if (tagOrClassVertex.size() == 0) return fileResultList;
		//if (tagOrClassVertex.size() == 0) return tagOrClassMatch;

		for (Vertex v : tagOrClassVertex) {
			
				switch (v.label()) {
				
				case GWriter.ClassVertex :
					tagOrClassMatch.put(v.value(GWriter.ClassClass), GWriter.ClassVertex);
					break;
					
				case GWriter.TagVertex :
					tagOrClassMatch.put(v.value(GWriter.TagClass), GWriter.TagVertex);
					break;
				
				// Only the two types should occur
				default :
					break;
				}
		}
		*/
	}
	
	
	
	public String makeFileQuery(Vertex resource, Vertex file) {
		String query = graphN+".V()"
				+ ".hasLabel('"+ResourceManager.ResourceVertex+"')"
				+ ".has('"+ResourceManager.ResourceUrl+"','"+resource.value(ResourceManager.ResourceUrl)+"')"
				+ ".outE('"+ResourceManager.FileEdge+"')"
				+ ".inV()"
				+ ".has('"+ResourceManager.FilePathRel+"','"+file.value(ResourceManager.FilePathRel)+"')";
		
		return query;
	}
	
	
	public void getFileResults(ResourceInfo resourceInfo, ResourceManager rm) {
		ArrayList<ResourceInfo> rfl = new ArrayList<ResourceInfo>();
		rfl.add(resourceInfo);
		getFileResults(rfl, rm);
	}
	
	
	/**
	 * Add info for matched classes to ResourceInfo objects
	 * @param rfl
	 */
	public void getFileResults(List<ResourceInfo> rfl, ResourceManager rm) {
		
			ResourceCache resourceCache = new ResourceCache();
			if (ExecutionBean.getResourceCache() != null) {
				resourceCache = ExecutionBean.getResourceCache();
			}
			
		
			int counter = 0;
			int sum = rfl.size();

			
			for (ResourceInfo resourceInfo : rfl) {
			
			Utils.debug("resource # "+(++counter)+"/"+sum);
			
			Utils.debug("Resource : "+resourceInfo.getDataURL());
			Utils.debug("File : "+resourceInfo.getFileInfo().getFileName());
		
			/*for(ModelMatch x : resourceInfo.getFileInfo().getModelMatchings()) {
				Utils.debug(x.getModelType());
				Utils.debug(x.getConllColumn());
			}*/
			
			
			// TODO use fileResults = HashMap <Integer, <HashMap <ModelMatch,ArrayList<FileResult>>>
			//fileResults = new HashMap <ModelType, ArrayList <FileResult>>();
			HashMap <ModelMatch, ArrayList <FileResult>> fileResults = new HashMap <ModelMatch, ArrayList <FileResult>>();
						
			//for (ModelMatch mm : resourceInfo.getFileInfo().determineBestModels()) {
			//for (ModelMatch mm : resourceInfo.getFileInfo().getSelectedModels()) {
			for (ModelMatch mm : resourceInfo.getFileInfo().getModelMatchings()) {

				//Utils.debug("Model : "+mm.getModelType());
				//Utils.debug("Coverage : "+mm.getCoverage());

				ArrayList <FileResult> fileResult = getTagsOfModelForResource(resourceInfo, mm);
				
				// Extract found tags and compute special tag properties
				HashSet<String> onlyTags = new HashSet<String>();
				for (FileResult fir : fileResult) {
					if (fir.matchesTag()) {
						onlyTags.add(fir.getFoundTagOrClass());
					}
				}
				//Utils.debug("onlyTags "+onlyTags.size());
				
				// (Legacy code for old computed results) 
				// Disable model matchings automatically that match only 
				// trivial entities like 1-letter tags, numbers or symbols.
				// (New results will be excluded earlier during parsing stage)
				// *** The filtering will not be applied to models that have
				// *** have been set manually !

				mm.computeTagProperties(new ArrayList(onlyTags));
				if (mm.isTrivialModelMatch() &&
					mm.getCoverage() < Executer.coverageThresholdOnLoad &&
					mm.getDetectionSource() != DetectionSource.SELECTION //&&
				 // resourceInfo.getFileInfo().getProcessState() != ProcessState.ACCEPTED
					) {
					mm.setSelected(false);
					resourceInfo.getFileInfo().updateModelMatchingsAsString();
					Utils.debug("Disabled the model "+mm.getModelType()+" "+mm.getConllColumn());
					mm.outputTagProperties();				
				}
				
				addUnmatchedConllTokens(rm, resourceInfo, mm, fileResult);
				
				Collections.sort(fileResult);
				
				if (!fileResult.isEmpty()) {
					if (!fileResults.keySet().contains(mm)) {
						fileResults.put(mm, new ArrayList<FileResult>(fileResult));
					} else {
						ArrayList<FileResult> more = fileResults.get(mm.getModelType());
						Utils.debug("more " +more == null);
						for (FileResult fResult : fileResult) {
							Utils.debug("fResult " +fResult == null);
							// Filter same results from different columns
							if (!more.contains(fResult)) {
								more.add(fResult);
							}
						}
						fileResults.put(mm, more);
					}
				}
				
				// TODO check if it is too slow loading olia results for all models
				// if so, then load olia file results on demand (e.g. when changing model in editor)
				//if (mm.isSelected() || true) {
				if (!fileResult.isEmpty() && fileResults.keySet().contains(mm)) {
					for (FileResult f : fileResults.get(mm)) {
						
						// enable annotated tag/class search
						String annotationOrAnnotationClass = f.getFoundTagOrClass();
						
						resourceCache.addAnnotationOrClass2ResourceMap( 
								resourceInfo.getDataURL(),
								annotationOrAnnotationClass
								);
						
						// enable olia search
						resourceCache.addMinimalOliaClassForMatchedTag2OliaResourceMap(
								resourceInfo.getDataURL(),
								f);
					}
				}
			}
	
			Utils.debug("File results :");
			Utils.debug(fileResults.size());
			
			resourceInfo.getFileInfo().setFileResults(fileResults);
			if(resourceInfo.getFileInfo().getModelMatchings().isEmpty()) {
				resourceInfo.getFileInfo().setModelMatchingsUnknown();
			}
		}
	}
	
	
	private void addUnmatchedConllTokens(ResourceManager rm, ResourceInfo resourceInfo, ModelMatch mm, ArrayList<FileResult> fileResult) {
		
		if (!resourceInfo.getFileInfo().isConllFile()) return;
		
		
		HashSet<String> allTokens = new HashSet(rm.getFileTokens(resourceInfo, mm.getConllColumn()));
		HashSet<String> matchedTokens = new HashSet<String>();
		
		if (!fileResult.isEmpty() && fileResult.get(0).getFeatureName().isEmpty()) {
		
		for (FileResult result : fileResult) {
			matchedTokens.add(result.getFoundTagOrClass());
		}
		} else {
			// complex token
			for (FileResult result : fileResult) {
				matchedTokens.add(result.getFeatureName()+"="+result.getFoundTagOrClass());
			}
		}
		
		// compute set difference
		allTokens.removeAll(matchedTokens);
		
		FileResult unmatched = new FileResult();
		unmatched.setFoundTagOrClass(AnnotationUtil.unmatchedAnnotations);
		String output = org.apache.commons.lang3.StringUtils.join(allTokens);
		if (allTokens.isEmpty()) {
			output = "";
		} else {
			output = StringUtils.substring(output, 1,output.length()-1);
		}
		unmatched.setMatchingTagOrClass(output);
		unmatched.setMatchCount(new Long(allTokens.size()));
		fileResult.add(unmatched);
		
	}




	/**
	 * Retrieve all used RDF predicates in HITs of a resource
	 * @param resourceInfo
	 * @return
	 */
	public HashSet<String> getResourceHitRdfProperties(ResourceInfo resourceInfo) {
		
		String query = 
		   "g.V().hasLabel('"+GWriter.HitVertex+"')"
		 + ".has('"+GWriter.HitResourceUrl+"','"+resourceInfo.getDataURL()+"')"
		 + ".has('"+GWriter.HitFileId+"','"+resourceInfo.getFileInfo().getFileId()+"')"
		 + ".values('"+GWriter.HitPredicate+"').dedup()";

		Utils.debug(query);
		ArrayList<String> result = genericStringQuery(query);
		Utils.debug("HIT predicates :");
		for (String x : result) {
			Utils.debug(x);
		}
		return new HashSet<String> (result);
	}


	/**
	 * Retrieve all used CONLL columns in HITs of a resource
	 * @param resourceInfo
	 * @return
	 */
	public HashSet<Integer> getResourceHitConllColumns(ResourceInfo resourceInfo) {
		String query = 
				   "g.V().hasLabel('"+GWriter.HitVertex+"')"
				 + ".has('"+GWriter.HitResourceUrl+"','"+resourceInfo.getDataURL()+"')"
				 + ".has('"+GWriter.HitFileId+"','"+resourceInfo.getFileInfo().getFileId()+"')"
				 + ".values('"+GWriter.HitConllColumn+"').dedup()";

				return new HashSet<Integer> (genericIntegerQuery(query));
	}
	
	
	public HashSet<String> getBLLResultPath(ResourceInfo resourceInfo, ModelMatch modelMatch, FileResult fileResult) {
		
		String resourceUrl = resourceInfo.getDataURL();
		String fileId = resourceInfo.getFileInfo().getFileId();
		int conllColumn = modelMatch.getConllColumn();
		String tagOrClass = fileResult.getFoundTagOrClass();
		ModelType modelType = modelMatch.getModelType();
		
		String query;
		if (fileResult.isUrl()) {
		  
		  query = "g.V().hasLabel('"+GWriter.HitVertex+"')."+
		  "has('"+GWriter.HitResourceUrl+"','"+resourceUrl+"').has('"+GWriter.HitFileId+"','"+fileId+"')."+
		  "has('"+GWriter.HitObject+"','"+tagOrClass+"')."+
		  "outE().inV().has('"+GWriter.Model+"','"+modelType.name()+"')"+
		  "emit().repeat(outE().inV().simplePath())."+
		  "has('"+GWriter.ClassModel+"','"+ModelType.valueOf("BLL")+"')."+
		  "path().by(choose(label()).option('"+GWriter.HitVertex+"',constant('START'))."+
		  "option('"+GWriter.ClassVertex+"',values('"+GWriter.ClassClass+"'))."+
		  "option('"+GWriter.TagVertex+"',values('"+GWriter.TagClass+"'))."+
		  "option('"+GWriter.TagMatchEdge+"',constant('tag match'))."+
		  "option('"+GWriter.ClassMatchEdge+"',constant('class match'))."+
		  "option('"+GWriter.FeatureMatchEdge+"',constant('feature match'))."+
		  "option(none,label()))";
		} else {
			
			if (!fileResult.isFeature()) {
		
			  query = "g.V().hasLabel('"+GWriter.HitVertex+"')."+
			  "has('"+GWriter.HitResourceUrl+"','"+resourceUrl+"').has('"+GWriter.HitFileId+"','"+fileId+"')."+
			  "has('"+GWriter.HitConllColumn+"',"+conllColumn+")."+
			  "has('"+GWriter.HitTag+"','"+tagOrClass+"')."+
			  "outE().inV().has('"+GWriter.Model+"','"+modelType.name()+"')"+
			  "emit().repeat(outE().inV().simplePath())."+
			  "has('"+GWriter.ClassModel+"','"+ModelType.valueOf("BLL")+"')."+
			  "path().by(choose(label()).option('"+GWriter.HitVertex+"',constant('START'))."+
			  "option('"+GWriter.ClassVertex+"',values('"+GWriter.ClassClass+"'))."+
			  "option('"+GWriter.TagVertex+"',values('"+GWriter.TagClass+"'))."+
			  "option('"+GWriter.TagMatchEdge+"',constant('tag match'))."+
			  "option('"+GWriter.ClassMatchEdge+"',constant('class match'))."+
			  "option('"+GWriter.FeatureMatchEdge+"',constant('feature match'))."+
			  "option(none,label()))";
			  
			} else {
			
			  // will not give results because features are not linked
			  query = "g.V().hasLabel('"+GWriter.HitVertex+"')."+
			  "has('"+GWriter.HitResourceUrl+"','"+resourceUrl+"').has('"+GWriter.HitFileId+"','"+fileId+"')."+
			  "has('"+GWriter.HitConllColumn+"',"+conllColumn+")."+
			  "has('"+GWriter.HitFeature+"','"+fileResult.getFeatureName()+"')."+
			  "has('"+GWriter.HitFeatureValue+"','"+tagOrClass+"')."+
			  "outE().inV().has('"+GWriter.Model+"','"+modelType.name()+"')"+
			  "emit().repeat(outE().inV().simplePath())."+
			  "has('"+GWriter.ClassModel+"','"+ModelType.valueOf("BLL")+"')."+
			  "path().by(choose(label()).option('"+GWriter.HitVertex+"',constant('START'))."+
			  "option('"+GWriter.ClassVertex+"',values('"+GWriter.ClassClass+"'))."+
			  "option('"+GWriter.TagVertex+"',values('"+GWriter.TagClass+"'))."+
			  "option('"+GWriter.TagMatchEdge+"',constant('tag match'))."+
			  "option('"+GWriter.ClassMatchEdge+"',constant('class match'))."+
			  "option('"+GWriter.FeatureMatchEdge+"',constant('feature match'))."+
			  "option(none,label()))";
			}
		}
		
		Utils.debug("###"+query);
		
		ArrayList<Object> queryResult = genericObjectListQuery(query);
		HashSet <String> results = new HashSet<String>();
		for (Object x : queryResult) {
			results.add(x.toString());
		}
		return results;
		
	}


	
	public HashMap<String, ArrayList <String>> getMinimalOliaPathMapping() {
		
		HashMap<String, ArrayList<String>> minimalOliaPathMapping = new HashMap<String, ArrayList<String>>();
		String query = "g.V().hasLabel('"+GWriter.HitVertex+"')"+
				".out().as('x')"+
				".out().until(has('"+GWriter.Model+"','"+ModelType.valueOf("OLIA").name()+"'))"+
				".repeat(out().simplePath())"+
				".path().group().by(select('x').values('"+GWriter.Class+"'))";
		
		// ==>[http://purl.org/olia/brown.owl#PPS:[[v[58165],v[1767],v[1768],v[177]],[v[58165],v[1767],v[1768],v[1686],v[19]],[v[58165],v[1767],v[1768],v[1686],v[1667],v[1207]]]]

		System.out.println(query);
		try {
			ArrayList<HashMap <String, ArrayList<ImmutablePath>>> results = new ArrayList<HashMap <String, ArrayList<ImmutablePath>>>();
			Bindings bindings = gremlinScriptEngine.createBindings();
			bindings.put(graphN, g);
			bindings.put("results", results);
			gremlinScriptEngine.eval(query+".fill(results)",bindings);
			
			
			for (HashMap <String, ArrayList<ImmutablePath>> x : results) {
				for (String key : x.keySet()) {
					System.out.println(key+":");
					ArrayList<String> shortestPath=new ArrayList<String>();
					for (ImmutablePath y : x.get(key)) {
						System.out.println(y.labels().size());
						ArrayList<String> path_ = new ArrayList<String>();
						for (Object o : y.objects()) {
							Vertex v = (Vertex) o;
							if (v.keys().contains("class")) {
								String cname = (String) v.value("class");
								if (!cname.startsWith("#")) {
									System.out.println((String) v.value("class"));
									path_.add((String) v.value("class"));
								}
							}
						}
						if (shortestPath.equals(path_)) {System.out.println("equals");}
						if (shortestPath.isEmpty()) {
							shortestPath = path_;
						} else {
							if(shortestPath.size() > path_.size()) shortestPath = path_;
						}
					}
					System.out.println("Shortest path :");
					for (String q : shortestPath) {
						System.out.println(q);
					}
					minimalOliaPathMapping.put(key, shortestPath);
				}
			}
			
		} catch (ScriptException e) {
			e.printStackTrace();
		}

		return minimalOliaPathMapping;
	}
	
	
	/**
	 * Get class mapping x->[y,z], were HIT->X->a-Y, HIT->b->Z. (see query : x is outgoing vertex of HIT and y,z.. are
	 * the first outgoing vertices that have model OLIA. Intermediate result is used later in the Olia query tool to identify
	 * resources that include tags/classes for any given Olia annotation class.
	 * @return
	 */
	public HashMap<String, HashSet<String>> getMinimalOliaClassMappingOld() {

		HashMap<String, HashSet<String>> minimalOliaClassMapping = new HashMap<String, HashSet<String>>();
		String query = "g.V().hasLabel('"+GWriter.HitVertex+"')"+
				".out().as('x')"+
				".out().until(has('"+GWriter.Model+"','"+ModelType.valueOf("OLIA").name()+"'))"+
				".repeat(out().simplePath())"+
				".as('y')"+
				".select('x','y')"+
				".by('"+GWriter.Class+"')";
		System.out.println(query);
		try {
			ArrayList <LinkedHashMap <String, String>> results = new ArrayList <LinkedHashMap <String, String>>();
			Bindings bindings = gremlinScriptEngine.createBindings();
			bindings.put(graphN, g);
			bindings.put("results", results);
			gremlinScriptEngine.eval(query+".fill(results)",bindings);
			
			String x,y;
			for (LinkedHashMap <String, String> map : results) {
				x = map.get("x");
				y = map.get("y");
				if (!minimalOliaClassMapping.containsKey(x)) {
					HashSet<String> oliaClasses = new HashSet<String>();
					oliaClasses.add(y);
					minimalOliaClassMapping.put(x, oliaClasses);
				} else {
					HashSet<String> oliaClasses = minimalOliaClassMapping.get(x);
					oliaClasses.add(y);
					minimalOliaClassMapping.put(x, oliaClasses);					
				}
			}
			/*System.out.println(minimalOliaClassMapping.keySet().size());
			for (String key : minimalOliaClassMapping.keySet()) {
				System.out.println(key+"#");
				for (String value : minimalOliaClassMapping.get(key)) {
					System.out.println(value);
				}
			}*/
		} catch (ScriptException e) {
			e.printStackTrace();
		}

		return minimalOliaClassMapping;
	}
	
	
}