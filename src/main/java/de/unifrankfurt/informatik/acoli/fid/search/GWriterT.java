package de.unifrankfurt.informatik.acoli.fid.search;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.tinkerpop.gremlin.groovy.jsr223.GremlinGroovyScriptEngine;
import org.apache.tinkerpop.gremlin.jsr223.GremlinScriptEngine;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.unifrankfurt.informatik.acoli.fid.gremlinQuery.BllTools;
import de.unifrankfurt.informatik.acoli.fid.gremlinQuery.EmbeddedQuery;
import de.unifrankfurt.informatik.acoli.fid.types.GEdge;
import de.unifrankfurt.informatik.acoli.fid.types.ModelInfo;
import de.unifrankfurt.informatik.acoli.fid.types.ModelType;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.WriterSPO;


/**
 * Empty implementation for Tinkergraph (in-memory graph)
 * @author frank
 */
public class GWriterT implements GWriter,WriterSPO {

	private Graph graph;
	private HashMap <String, Vertex> vertexM; // stores all written vertexes to avoid duplicates (not only used when writing olia models)
	private HashMap <String, Edge> edgeM;     // stores all written edges to avoid duplicates    (not only used when writing olia models)
	private HashSet <String> hitEdgeKeys;     // stores keys (as computed by getHitEdgeKey(hit,target) for existing HIT edges
	
	private static long id;
	//private final AtomicLong count = new AtomicLong(0);
	private GraphTraversalSource g;
	final String [] oldBindings = new String [] {"i", "1"};
	GremlinScriptEngine gremlinScriptEngine = new GremlinGroovyScriptEngine();
	private EmbeddedQuery queries;
	private int maxPredicateMissCount = 5;
	private XMLConfiguration fidConfig;
	
	private AnnotationCache annotationCache;
	private BllTools bllTools;


	
	public GWriterT(Graph graph, XMLConfiguration fidConfig) {
		System.out.println("GWriterT NOT IMPLEMENTED !");
		this.graph = graph;
		this.g = graph.traversal();
		this.vertexM = new HashMap <String, Vertex> ();
		this.edgeM = new HashMap <String, Edge> ();
		this.queries = new EmbeddedQuery(graph);
		this.bllTools = new BllTools(graph, fidConfig);
		annotationCache = new AnnotationCache(this);
		this.hitEdgeKeys = new HashSet<String>();
		id = this.queries.getMaxVertexOrEdgeId()+1;
		this.fidConfig = fidConfig;
		restoreVertexMapEdgeMap();
	}
	
	
	@Override
	public void restoreVertexMapEdgeMap() {
		vertexM.clear();
		edgeM.clear();
		hitEdgeKeys.clear();
		GraphTools.restoreVertexMapEdgeMap(queries, vertexM, edgeM);
	}


	@Override
	public void writeModel(HashSet<GEdge> edges,
			HashMap<String, HashSet<String>> labels,
			HashMap<String, HashSet<String>> languages,
			HashMap<String, String> tiers, ModelInfo modelInfo) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void writeRdf(HashMap<String, Long> subjects,
			HashMap<String, Long> predicates, HashMap<String, Long> objects,
			ResourceInfo resourceInfo) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void writeConll(int column, HashMap<String, Long> tagCounts,
			ResourceInfo resourceInfo, ModelType model,
			HashMap<String, HashSet<String>> tagWords) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void writeConllFeatures(int csvColumn,
			HashMap<String, HashMap<String, Long>> featureMap,
			ResourceInfo resourceInfo, ModelType model) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public Graph getGraph() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public HashMap<String, Vertex> getVertexMap() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public HashMap<String, Edge> getEdgeMap() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void saveAsJSON(String file) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void saveAsML(String file) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void saveAsGyro(String file) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void addEdgeHit2Class(Vertex hit, String _class,
			HashMap<String, Integer> disregardedPredicates, ModelType modelType) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void addEdgeHit2Tag(Vertex hit, String hitTag,
			ModelType modelFilter, HashMap<String, Integer> discardedPredicates) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void finish() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public EmbeddedQuery getQueries() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void deleteDatabase() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void deleteHitVertices() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void writeGenericRdf(
			HashMap<String, HashMap<String, Long>> predicates,
			HashMap<String, HashMap<String, Long>> objects,
			ResourceInfo resourceInfo) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public BllTools getBllTools() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public XMLConfiguration getConfiguration() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void updateModelsByNamespaces(
			LinkedHashMap<ModelType, String[]> tags2namespaces,
			LinkedHashMap<ModelType, String[]> classes2namespaces) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void addEdgeFeatureHit2Tag(Vertex hitVertex, String featureName,
			String featureValue, ModelType modelFilter) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public HashSet<String> getHitEdgeKeys() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void setHitEdgeKeys(HashSet<String> hitEdgeKeys) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void updateHitTotalTokenCount(ResourceInfo resourceInfo) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public AnnotationCache getAnnotationCache() {
		// TODO Auto-generated method stub
		return null;
	}

}


