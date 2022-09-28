package de.unifrankfurt.informatik.acoli.fid.search;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptException;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.tinkerpop.gremlin.groovy.jsr223.GremlinGroovyScriptEngine;
import org.apache.tinkerpop.gremlin.jsr223.GremlinScriptEngine;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jVertex;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.io.IoCore;
import org.apache.tinkerpop.gremlin.structure.io.graphml.GraphMLWriter;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONWriter;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONWriter.Builder;

import de.unifrankfurt.informatik.acoli.fid.detector.TikaTools;
import de.unifrankfurt.informatik.acoli.fid.exec.Executer;
import de.unifrankfurt.informatik.acoli.fid.gremlinQuery.BllTools;
import de.unifrankfurt.informatik.acoli.fid.gremlinQuery.EmbeddedQuery;
import de.unifrankfurt.informatik.acoli.fid.types.GEdge;
import de.unifrankfurt.informatik.acoli.fid.types.GNodeType;
import de.unifrankfurt.informatik.acoli.fid.types.INode;
import de.unifrankfurt.informatik.acoli.fid.types.ModelInfo;
import de.unifrankfurt.informatik.acoli.fid.types.ModelMatch;
import de.unifrankfurt.informatik.acoli.fid.types.ModelType;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.WriterSPO;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;



/**
 * Model graph implementation for Neo4j
 * @author frank
 *
 */
public class GWriterN implements GWriter,WriterSPO {

	private Graph graph;
	private GraphTraversalSource g;
	private EmbeddedQuery queries;
	
	private GremlinScriptEngine gremlinScriptEngine = new GremlinGroovyScriptEngine();
	
	private HashMap <String, Vertex> vertexM; // stores all written vertexes to avoid duplicates (not only used when writing olia models)
	private HashMap <String, Edge> edgeM;     // stores all written edges to avoid duplicates    (not only used when writing olia models)
	private HashSet <String> hitEdgeKeys;     // stores keys (as computed by getHitEdgeKey(hit,target) for existing HIT edges
	private long id = 1;	// has no function because Neo4J don't uses ids for vertexes and edges
	private int maxPredicateMissCount = 100;  // 5
	
	private AnnotationCache annotationCache;
	private BllTools bllTools;
	private XMLConfiguration fidConfig;


	public GWriterN(File directory, XMLConfiguration fidConfig) {
		
		this.graph = Neo4jGraph.open(directory.getAbsolutePath());
		this.graph.configuration().addProperty("dbms.tx_log.rotation.retention_policy", false);
		this.vertexM = new HashMap <String, Vertex> ();
		this.edgeM = new HashMap <String, Edge> ();
		this.queries = new EmbeddedQuery(graph);
		this.bllTools = new BllTools(graph, fidConfig);
		this.fidConfig = fidConfig;
		annotationCache = new AnnotationCache(this);
		this.hitEdgeKeys = new HashSet<String>();
		restoreVertexMapEdgeMap();
	}
	
	@Override
	public AnnotationCache getAnnotationCache(){
		return this.annotationCache;
	}
	
	
	@Override
	public void restoreVertexMapEdgeMap() {
		vertexM.clear();
		edgeM.clear();
		hitEdgeKeys.clear();
		GraphTools.restoreVertexMapEdgeMap(queries, vertexM, edgeM);
	}
	
	
	@Override
	public Graph getGraph () {
		return graph;
	}
	
	@Override
	public EmbeddedQuery getQueries () {
		return this.queries;
	}
	
	@Override
	public BllTools getBllTools() {
		return this.bllTools;
	}
	
	public HashMap <String, Vertex> getVertexMap() {
		return this.vertexM;
	}
	
	public HashMap <String, Edge> getEdgeMap() {
		return this.edgeM;
	}
	

	
	/**
	 * Write method for models (Must be run single threaded !)
	 * @param edges
	 * @param modelName
	 */
	public void writeModel(
			HashSet<GEdge> edges,
			HashMap <String,HashSet<String>> labels,
			HashMap<String, HashSet<String>> languages,
			HashMap<String,String> tiers,
			ModelInfo modelInfo
			) {
		
		
		String from;
		String to;
		String rt;
		GNodeType tFrom;
		GNodeType tTo;		
		
		String modelName = modelInfo.getModelType().toString();
		String fileName = modelInfo.getName();
		String lang639;
		int j = 0;

		
		// Create nodes & edges
		for (GEdge e : edges) {
			
			from = e.getFrom().getName();
			tFrom = e.getFrom().getType();
			to = e.getTo().getName();
			tTo = e.getTo().getType();
			rt = e.getRelationType().toString();
			
			//Utils.debug("before");
			//Utils.debug(from+"#"+tFrom.toString()+"***"+rt+"***"+to+"#"+tTo.toString());
			
			if (edgeM.keySet().contains(from+"#"+tFrom.toString()+"***"+rt+"***"+to+"#"+tTo.toString())) continue;
			
			//Utils.debug("executed");
			//Utils.debug(from+"#"+tFrom.toString()+"***"+rt+"***"+to+"#"+tTo.toString());
				
			Vertex _from = vertexM.get(from+"#"+tFrom.toString());
			if (_from == null || from.endsWith("22-rdf-syntax-ns#nil") || from.endsWith("22-rdf-syntax-ns#first")) {
				if (tFrom.equals(GNodeType.CLASS))
				_from =  graph.addVertex(T.label, tFrom.toString(),
						GWriter.Class, from,
						GWriter.Model,modelName,
						GWriter.ClassFile,fileName
						);
				else // TAG
				_from =  graph.addVertex(T.label, tFrom.toString(),
						GWriter.Class, from,
						GWriter.Model,modelName,
						GWriter.TagFile,fileName,
						GWriter.TagTag,((INode) e.getFrom()).getTag(),
						GWriter.TagType,((INode) e.getFrom()).getTagType().name().toString());
				vertexM.put(from+"#"+tFrom.toString(),_from);
				id++;
				}
			
			Vertex _to = vertexM.get(to+"#"+tTo.toString());
			if (_to == null || to.endsWith("22-rdf-syntax-ns#nil") || to.endsWith("22-rdf-syntax-ns#first")) {
				_to = graph.addVertex(T.label, tTo.toString(),
						GWriter.Class, to,
						GWriter.Model, modelName,
						GWriter.File,fileName);
				vertexM.put(to+"#"+tTo.toString(), _to);
				id++;
				}
			
			// Add labels
			j = 0;
			if (labels.containsKey(from)) {
				for (String label : labels.get(from)) {
					j++;
					writeLabel(_from, j, label);
				}
			}
			
			while (j < 3) {
				j++;
				writeLabel(_from, j, "");
			}
			
			
			j = 0;
			if (labels.containsKey(to)) {
				
				for (String label : labels.get(to)) {
					j++;
					writeLabel(_to, j, label);
				}
			}
			
			while (j < 3) {
				j++;
				writeLabel(_to, j, "");
			}
			
			
			
			// Add iso639 language identifier (3-letter code)
			lang639 = "";
			if (languages.containsKey(from)) {
				// Check is given language is code/url/somethingelse
				for (String langString : languages.get(from)) {
			
				if (TikaTools.isISO639LanguageCode(langString)) {
						lang639 = langString;
						break;
				}
					
				if (TikaTools.isLexvoUrl(langString)) {
					lang639 = TikaTools.getISO639_3CodeFromLexvoUrl(langString);
					break;
				}
				
				// Error 
				System.out.println("Error : Could not identify ISO-639-3 Code for > "+langString+" < !");

				}
			}
			_from.property(GWriter.ClassLang639, lang639);

			
			lang639 = "";
			if (languages.containsKey(to)) {
				// Check is given language is code/url/somethingelse
				for (String langString : languages.get(to)) {

				if (TikaTools.isISO639LanguageCode(langString)) {
					lang639 = langString;
					break;
				}
				
				if (TikaTools.isLexvoUrl(langString)) {
					lang639 = TikaTools.getISO639_3CodeFromLexvoUrl(langString);
					break;
				}
				
				// Error 
				System.out.println("Error : Could not identify ISO-639-3 Code for > "+langString+" < !");

				}
			}
			_to.property(GWriter.ClassLang639, lang639);
				
			
			// Add restriction on property /olia/system.owl#hasTier
			if (tiers.containsKey(from))
				_from.property(GWriter.ClassHasTier, tiers.get(from));
			
			if (tiers.containsKey(to))
				_to.property(GWriter.ClassHasTier, tiers.get(to));
			
			
			Utils.debug(rt+ " added "+ from + " -> "+ to);
			//_from.addEdge("sub", _to,T.id,id,"relation", rt);

			Edge edge = _from.addEdge(rt, _to,"relation", rt);
			//_from.addEdge(rt, _to,T.id,id,"relation", rt);
			edgeM.put(from+"#"+tFrom.toString()+"***"+rt+"***"+to+"#"+tTo.toString(), edge);


			id++;
			
		}
		
		Utils.debug(id);
		
		// commit
		graph.tx().commit();
	}
	
		
	private void writeLabel(Vertex v, int labelId, String labelValue) {
		
		switch(labelId) {
		case 1:
			v.property(GWriter.ClassLabel1, labelValue);
			break;
		case 2:
			v.property(GWriter.ClassLabel2, labelValue);
			break;
		case 3:
			v.property(GWriter.ClassLabel3, labelValue);
			break;
		default:
			Utils.debug("Error : more than 3 labels not supported !");
			break;
		}
		//Utils.debug("Add label : "+labelValue);
	}
	
	
	@Override
	/**
	 * Write method for result of GenericStreamParserSPO.
	 * Write parsed  predicates and objects to tinkerpop graph
	 * @param predicates Map : predicate -> (objectLiteral -> count)
	 * @param objects Map : predicate -> (objectUrl -> count)
	 */
    public void writeGenericRdf ( 
    		HashMap<String, HashMap<String,Long>> predicates,
    		HashMap<String, HashMap<String,Long>> objects,
    		ResourceInfo resourceInfo
    		) {
		
	    Utils.debug("writeGenericRdf");
		g = graph.traversal();
		HashMap <String, Integer> discardedPredicates = new HashMap <String, Integer>();
		long count=0;
	
	    // TODO improve error handling : 
		// Catch single errors instead of canceling everything if an error occurs
	    try {
	    	
	    	// write literal objects
	    	for(String predicateUrl : predicates.keySet()) {
	    		
	    		for (String literal : predicates.get(predicateUrl).keySet()) {
	    			
	    			if (!annotationCache.isAnnotationTag(literal)) continue;

	    			
	    			if (discardedPredicates.containsKey(predicateUrl) 
	    	    		    && discardedPredicates.get(predicateUrl) >= maxPredicateMissCount) {
	    				Utils.debug("Skipping predicate "+predicateUrl+" after "+maxPredicateMissCount+" unsuccessful writes !!");
	    				break;
	    			}
	    			
	    			
	    			count = predicates.get(predicateUrl).get(literal);
			    		
			    	Vertex vt = graph.addVertex(T.label, HitVertex,
							HitResourceUrl, resourceInfo.getDataURL(),
							HitFileId, resourceInfo.getFileInfo().getFileId(),
							HitType,HitTypeLiteralObject,
							HitPredicate, predicateUrl,
							HitObject, literal,
							HitCount, count);
			    	
			    	
			    	Utils.debug(predicateUrl);
					Utils.debug(literal);
			    	addEdgeHit2Tag (vt, literal, null, discardedPredicates);
					
			
			     // addEdgeHit2Class (vt, text);  makes sense ?
			    	
			    	Utils.debug("added hit node : "+ resourceInfo.getDataURL()+" type literal object : "+literal);
					id++;
		    	}
	    	}
	    	
		
	    	// write URI objects
	    	for(String predicateUrl : objects.keySet()) {
	    		
	    		for (String objectUrl : objects.get(predicateUrl).keySet()) {
	    			
	    			if (!annotationCache.isAnnotationClass(objectUrl)) continue;
	    			
	    			if (discardedPredicates.containsKey(predicateUrl) 
	    	    		    && discardedPredicates.get(predicateUrl) >= maxPredicateMissCount) {
	    				Utils.debug("Skipping predicate "+predicateUrl+" after "+maxPredicateMissCount+" unsuccessful writes !!");
	    				break;
	    			}
	    			
	    			count = objects.get(predicateUrl).get(objectUrl);
					
			    	Vertex vt = graph.addVertex(T.label, HitVertex,
							HitResourceUrl, resourceInfo.getDataURL(),
							HitFileId, resourceInfo.getFileInfo().getFileId(),
							HitType,HitTypeURIObject,
							HitPredicate, predicateUrl,
							HitObject, objectUrl,
							HitCount, count);
			    	
			    	addEdgeHit2Class(vt, objectUrl, discardedPredicates, null);
			
			    	Utils.debug("added hit node : "+ resourceInfo.getDataURL()+" type URI object : "+objectUrl);
			    	//Utils.debug("added hit node : "+ resourceInfo.getDataURL()+"  "+x+"  type object");
					id++;
			    }
	    	}
	
	    } catch (Exception e) {
			Utils.debug(e.getMessage());
	    }
	    
	    queries.deleteUnconnectedHitVertices(resourceInfo.getDataURL());
	    
		// commit
		graph.tx().commit();
    }
	
	
	
	
	
	
	
	/**
	 * Write method for parsed result of source files (in any RDF format)
	 * Write parsed subjects, predicates and objects to tinkerpop graph
	 * @param subjects Map : subjects -> subject count
	 * @param predicates Map : predicate+objectLiteral -> count
	 * @param objects Map : predicate+objectUrl -> count
	 */
	@Override
    public void writeRdf (HashMap <String, Long> subjects, 
    		HashMap <String, Long> predicates,
    		HashMap <String, Long> objects,
    		ResourceInfo resourceInfo
    		) {
		
		String predicateUrl;
		String objectUrl;
		String literal;
		g = graph.traversal();
		HashMap <String, Integer> discardedPredicates = new HashMap <String, Integer>();

    
    try {
    	// types.GNodeType cannot be cast to java.lang.String
    	
    	// Write subjects (not used anymore)
    	/*for(String x : subjects.keySet()) {
    		
		Vertex vt = graph.addVertex(T.label, HitVertex,
				HitResourceUrl, resourceInfo.getDataURL(),
				HitFileId, resourceInfo.getFileInfo().getFileId(),
				HitType,"s",
				HitPredicate, x,
				HitObject, "",
				HitCount,subjects.get(x));
    	
		
    	addEdgeHit2Class(vt, x);
    	
    	Utils.debug("added hit node : "+ resourceInfo.getDataURL()+" type subject");
		id++;
    	}*/
    	
    	// Write literal objects
    	for(String x : predicates.keySet()) {

    	String[] split = x.split(",");
    	if (split.length == 1) {
    		predicateUrl = x;literal="";
    	} else {
    		predicateUrl = split[0]; literal=split[1];
    	}
    	
    	if (discardedPredicates.containsKey(predicateUrl) 
    	&& discardedPredicates.get(predicateUrl) >= maxPredicateMissCount) continue;
    		
    	Vertex vt = graph.addVertex(T.label, HitVertex,
				HitResourceUrl, resourceInfo.getDataURL(),
				HitFileId, resourceInfo.getFileInfo().getFileId(),
				HitType, HitTypeLiteralObject,
				HitPredicate, predicateUrl,
				HitObject, literal,
				HitCount, predicates.get(x));
    	
    	Utils.debug(predicateUrl);
		Utils.debug(literal);
    	addEdgeHit2Tag (vt, literal, null, discardedPredicates);
		

     // addEdgeHit2Class (vt, text);  makes sense ?
    	
    	Utils.debug("added hit node : "+ resourceInfo.getDataURL()+" type literal object "+literal);
		id++;
    	}
    	
	
    	// Write URI objects
    	for(String x : objects.keySet()) {
    		
    	// TODO add disregardedPredicates 
    		
		String[] split = x.split(",");
    	if (split.length == 1) {
    		objectUrl = x;predicateUrl="";
    	} else {
    		objectUrl = split[1]; predicateUrl=split[0];
    	}
		
    	Vertex vt = graph.addVertex(T.label, HitVertex,
				HitResourceUrl, resourceInfo.getDataURL(),
				HitFileId, resourceInfo.getFileInfo().getFileId(),
				HitType, HitTypeURIObject,
				HitPredicate, predicateUrl,
				HitObject, objectUrl,
				HitCount, objects.get(x));
		
		
    	addEdgeHit2Class(vt, objectUrl, null, null); // TODO add disregardedPredicates 

    	Utils.debug("added hit node : "+ resourceInfo.getDataURL()+" type URI object "+objectUrl);
    	//Utils.debug("added hit node : "+ resourceInfo.getDataURL()+"  "+x+"  type object");
		id++;
    	}

    } catch (Exception e) {
		Utils.debug(e.getMessage());
    }
    
    queries.deleteUnconnectedHitVertices(resourceInfo.getDataURL());
    
	// commit
	graph.tx().commit();
    }
	
	
	/**
	 * Write method for parsed posTags from conll csv file
	 * @param csvColumn
	 * @param tagCounts Map with entries (TagName, count)
	 * @param resourceInfo
	 * @param modelType (restrict connections only to the given model)
	 * @param tagWords (not used)
	 */
	public void writeConll (
			int csvColumn,
			HashMap<String, Long> tagCounts,
			ResourceInfo resourceInfo,
			ModelType model,
			HashMap <String,HashSet<String>> tagWords
			) {
		
		HashMap <String, Vertex> vertices = new HashMap <String, Vertex> ();
		HashMap<String, Integer> discardedPredicates = new HashMap<String, Integer>();
		
		g = graph.traversal();
 
		String fileURL = resourceInfo.getDataURL();
	 	// Write
    	for(String x : tagCounts.keySet()) {
    		
    	if (!annotationCache.isAnnotationTag(x)) {
    		Utils.debug("annotation cache miss : "+x);
    		continue;
    	}
    		
		Vertex vt = graph.addVertex(T.label, HitVertex,
				HitResourceUrl, fileURL,
				HitFileId, resourceInfo.getFileInfo().getFileId(),
				HitType, HitTypeTag,
				HitTag,x,
				HitCount, tagCounts.get(x),
				HitConllColumn, csvColumn
				);
		
    	
    	vertices.put(x, vt);
    	
    	
    	addEdgeHit2Tag (vt, x, model, null);
    	Utils.debug("added hit node : "+ fileURL+" type tag : "+x);
		
    	id++;
    	}
    	
    	queries.deleteUnconnectedHitVertices(resourceInfo.getDataURL());
		// commit
		graph.tx().commit();
	}
	
	
	@Override
	/**
	 * Write method for features. Features are input via a HashMap with entries (FeatureName, [FeatureValues])
	 * @param csvColumn
	 * @param featureMap
	 * @param resourceInfo
	 */
	public void writeConllFeatures(
			int csvColumn,
			HashMap<String, HashMap<String, Long>> featureMap,
			ResourceInfo resourceInfo,
			ModelType model
			) {
		
		HashMap <String, Vertex> vertices = new HashMap <String, Vertex> ();
		
		g = graph.traversal();

		String fileURL = resourceInfo.getDataURL();
		HashMap<String,Long> value2Count = null;
	 	// Write
    	for(String featureName : featureMap.keySet()) {
    		
    		if (Executer.featureIgnoreList.contains(featureName) || featureName.startsWith("MWE")) continue;
    		
    		value2Count = featureMap.get(featureName);
    				
    		for (String featureValue : value2Count.keySet()) {
    			
 
				Vertex vt = graph.addVertex(T.label, HitVertex,
						HitResourceUrl, fileURL,
						HitFileId, resourceInfo.getFileInfo().getFileId(),
						HitType, HitTypeFeature,
						HitFeature, featureName,
						HitFeatureValue, featureValue,
						HitCount, value2Count.get(featureValue),
						HitConllColumn, csvColumn
						);
				
				vertices.put(featureName, vt);
		    	
		    	addEdgeFeatureHit2Tag (vt, featureName, featureValue, model);
		    	Utils.debug("added feature hit node : "+ fileURL+" "+featureName+" : "+featureValue);
				
		    	id++;
    		}
    	}
    	
    	queries.deleteUnconnectedHitVertices(resourceInfo.getDataURL());
		// commit
		graph.tx().commit();  	
	}



	public void saveAsJSON (String file) {

		Builder x = GraphSONWriter.build();
		GraphSONWriter y = x.create();
		OutputStream stream = createOutputstream(file);
		try {
			y.writeGraph(stream, graph);
		} catch (IOException e) {
			Utils.debug(e.getMessage());
		}
		
	}
	
	
	public void saveAsML (String file) {
		org.apache.tinkerpop.gremlin.structure.io.graphml.GraphMLWriter.Builder x = GraphMLWriter.build();
		GraphMLWriter y = x.create();
		OutputStream stream = createOutputstream(file);
		try {
			y.writeGraph(stream, graph);
		} catch (IOException e) {
			Utils.debug(e.getMessage());
		}
		
	}
	
	
	public void saveAsGyro (String file) {
		
		try (final OutputStream os = new FileOutputStream(file)) {
		    graph.io(IoCore.gryo()).writer().create().writeGraph(os, graph);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/*
		try (final InputStream stream = new FileInputStream("tinkerpop-modern.kryo")) {
		    newGraph.io(IoCore.gryo()).reader().vertexIdKey("name").create().readGraph(stream, newGraph);
		}
		*/
	}
	
	
	// Add CLASS edge
	/**
	 * Add edge HIT -> CLASS
	 * @param _class
	 * @param vt
	 */
	public void addEdgeHit2Class (Vertex hit, String _class, HashMap<String, Integer> discardedPredicates, ModelType modelType) {

		int writeOps = 0;
		// Remove single quotes (will crash query !) TODO escape quotes in queries
		_class = _class.replace("'", "");
		_class = _class.replace(",", "");
		_class = _class.replace(";", "");
		// never remove dots that occur in class URLs !!!
		if (_class.isEmpty()) return;
	
		g = graph.traversal();
		
		List <Neo4jVertex> results = new ArrayList<Neo4jVertex>();
		Bindings bindings = gremlinScriptEngine.createBindings();		
		bindings.put("g", g);
		bindings.put("results", results);
	
		try {
			// TODO limit namespace for class to olia namespace
			if (modelType == null) {
					gremlinScriptEngine.eval(
					"g.V().hasLabel('CLASS','TAG').filter {'"+_class+"'.matches(it.get().value('class'))}.fill(results)",bindings);
			} else {
					gremlinScriptEngine.eval(
						"g.V().hasLabel('CLASS','TAG').has('"+GWriter.Model+"','"+modelType.name()+"').filter {'"+_class+"'.matches(it.get().value('class'))}.fill(results)",bindings);
			}
			//gremlinScriptEngine.eval(
			//		"g.V().hasLabel('CLASS','TAG').filter {'"+_class+"'.matches(it.get().value('class'))}.fill(results)",bindings);
		
			//"g.V().hasLabel('TAG').filter {'"+_class+"'.matches(it.get().value('class'))}.fill(results)",bindings);
			// 	"g.V().or(hasLabel('TAG'),hasLabel('CLASS').where(inE().outV().hasLabel('CLASS')).filter {'"+_class+"'.matches(it.get().value('class'))}.fill(results)",bindings);

			//Utils.debug("addEdgeHit2Class : "+_class);
			for (Neo4jVertex y : results) {
				
				// skip edges already in graph (only for reconnectHits)
				if (hitEdgeKeys.contains(GraphTools.getHitEdgeKey(hit,y))) continue;
				
				Utils.debug("addHit2Class");
				Utils.debug(_class +" --> "+ y.id());
				Utils.debug(_class +" --> "+ y.value(GWriter.Class));

				
				// Add 'match' edge from hit node to a 'class' node
				hit.addEdge(GWriter.ClassMatchEdge, y);
				writeOps++;
			}
		} catch (ScriptException e) {
			if(e.getMessage().contains("TraversalInterruptedException")) {
				Executer.setInterrupted(true);
			}
			Utils.debug("Class : "+_class+ "#"+_class.length());
			Utils.debug(e.getMessage());
		}
		
		if (discardedPredicates != null) {
			if (results.isEmpty()) {
				if (!discardedPredicates.containsKey(hit.value(GWriter.HitPredicate))) {
					 discardedPredicates.put(hit.value(GWriter.HitPredicate), 1);
				} else {
				// Increase miss count
				discardedPredicates.replace(hit.value(GWriter.HitPredicate), discardedPredicates.get(hit.value(GWriter.HitPredicate))+1);
				}
			}
		}
		System.out.println("#addEdgeHit2Class# "+writeOps);
	}
	
	

	// Add TAG edge
	// misses hitTag = URL
	/**
	 * Add edge HIT -> TAG
	 * @param hit
	 * @param hitTag
	 * @param modelFilter
	 * @param discardedPredicates (set null for applications (e.g. conll) that do not need ! - used for predicate HIT otherwise set null !)
	 */
	@Override
	public void addEdgeHit2Tag (Vertex hit, String hitTag, ModelType modelFilter, HashMap<String, Integer> discardedPredicates) {
		
		int writeOps = 0;
		// Remove single quotes (will crash query !) TODO escape quotes in queries
		hitTag = hitTag.replace("'", "");
		hitTag = hitTag.replace(",", "");
		hitTag = hitTag.replace(";", "");
		// never remove dots that can occur in tags e.g. AB..Z !!!
		if (hitTag.isEmpty()) return;
		
		g = graph.traversal();
		
		List <Neo4jVertex> results = new ArrayList<Neo4jVertex>();
		Bindings bindings = gremlinScriptEngine.createBindings();		
		bindings.put("g", g);
		bindings.put("results", results);
		
		try {
		if (modelFilter == null) {
			
			try {
				gremlinScriptEngine.eval("g.V().hasLabel('TAG').filter {'"+hitTag+"'.matches(it.get().value('tag'))}.fill(results)",bindings);
			} catch (ScriptException e) {
				if(e.getMessage().contains("TraversalInterruptedException")) {
					Executer.setInterrupted(true);
				}
				//Utils.debug("hitTag error : "+hitTag);
				//e.printStackTrace();
				results.clear();
				gremlinScriptEngine.eval("g.V().hasLabel('TAG').has('tag','"+hitTag+"').fill(results)",bindings);
		}
		
		} else {
			try {
				gremlinScriptEngine.eval("g.V().hasLabel('TAG').has('model','"+modelFilter.toString()+"').filter {'"+hitTag+"'.matches(it.get().value('tag'))}.fill(results)",bindings);
			} catch (Exception e) {
				results.clear();
				gremlinScriptEngine.eval("g.V().hasLabel('TAG').has('model','"+modelFilter.toString()+"').has('tag','"+hitTag+"').fill(results)",bindings);
			}
		}
		
		
		for (Neo4jVertex y : results) {
			
			// skip edges already in graph (only for reconnectHits)
			if (hitEdgeKeys.contains(GraphTools.getHitEdgeKey(hit,y))) {
				continue;
			}
			
			Utils.debug("addHit2Tag");
			Utils.debug(hitTag +" --> "+ y.id());
			Utils.debug(hitTag +" --> "+ y.value(GWriter.Class));

			hit.addEdge(GWriter.TagMatchEdge, y);
			writeOps++;
		}
		
		} catch (Exception e) {
			Utils.debug("Hit tag : "+hitTag+ "#"+hitTag.length());
			Utils.debug(e.getMessage());

		}
		
		if (discardedPredicates != null) {
			if (results.isEmpty()) {
				if (!discardedPredicates.containsKey(hit.value(GWriter.HitPredicate))) {
					 discardedPredicates.put(hit.value(GWriter.HitPredicate), 1);
				} else {
				// Increase miss count
				discardedPredicates.replace(hit.value(GWriter.HitPredicate), discardedPredicates.get(hit.value(GWriter.HitPredicate))+1);
				}
			}
		}
		System.out.println("#addEdgeHit2Tag# "+writeOps);
	}
	
	
	
	/**
	 * Add edge feature HIT -> Tag/Class
	 * @param hitVertex
	 * @param featureName
	 * @param featureValue
	 * @param modelFilter only for HITs/TAGs in model
	 */
	@Override
	public void addEdgeFeatureHit2Tag (Vertex hitVertex, String featureName, String featureValue, ModelType modelFilter) {
		
		int writeOps = 0;
		g = graph.traversal();
		
		List <Neo4jVertex> results = new ArrayList<Neo4jVertex>();
		Bindings bindings = gremlinScriptEngine.createBindings();		
		bindings.put("g", g);
		bindings.put("results", results);
		
		try {
			
			if (modelFilter == null) {
			gremlinScriptEngine.eval(
					"g.V().hasLabel('"+GWriter.ClassVertex+"').or("+
					"has('"+GWriter.ClassLabel1+"','"+featureValue+"'),"+
					"has('"+GWriter.ClassLabel2+"','"+featureValue+"'),"+
					"has('"+GWriter.ClassLabel3+"','"+featureValue+"'))"+
				    ".as('v')"+
				    ".until(filter{it.get().value('"+GWriter.ClassClass+"')"+
				    ".endsWith('"+featureName+"')})"+
				    ".repeat(out().simplePath())"+
				    ".until(and(hasLabel('"+GWriter.ClassVertex+"'),"+
				    "has('tier','FEATS')))"+
				    ".repeat(out().simplePath())"+
				    ".select('v')"+
				    ".dedup().fill(results)",bindings);
			} else {
			gremlinScriptEngine.eval(
					"g.V().hasLabel('"+GWriter.ClassVertex+"').has('"+GWriter.ClassModel+"','"+modelFilter.name()+"').or("+
					"has('"+GWriter.ClassLabel1+"','"+featureValue+"'),"+
					"has('"+GWriter.ClassLabel2+"','"+featureValue+"'),"+
					"has('"+GWriter.ClassLabel3+"','"+featureValue+"'))"+
				    ".as('v')"+
				    ".until(filter{it.get().value('"+GWriter.ClassClass+"')"+
				    ".endsWith('"+featureName+"')})"+
				    ".repeat(out().simplePath())"+
				    ".until(and(hasLabel('"+GWriter.ClassVertex+"'),"+
				    "has('tier','FEATS')))"+
				    ".repeat(out().simplePath())"+
				    ".select('v')"+
				    ".dedup().fill(results)",bindings);	
			}
			
		for (Neo4jVertex y : results) {

			// skip edges already in graph (only for reconnectHits)
			if (hitEdgeKeys.contains(GraphTools.getHitEdgeKey(hitVertex,y))) continue;
	
			Utils.debug("addFeatureHit2Tag");
			Utils.debug(featureName +" --> "+ y.id());
			hitVertex.addEdge(GWriter.FeatureMatchEdge, y);
			writeOps++;
		}
		
			
		} catch (Exception e) {
			Utils.debug(e.getMessage());
		}
		System.out.println("#addEdgeFeatureHit2Tag# "+writeOps);
	}
		

	
	public void clear() {
	}
	

	public void finish() {
		
		try {
			graph.tx().commit();
			//graph.close();
		} catch (Exception e) {
			Utils.debug(e.getMessage());
		}
	}
	


	public static FileOutputStream createOutputstream (String filepath) {
		try {
			return new FileOutputStream(filepath);
			} catch (FileNotFoundException e) {
			Utils.debug(e.getMessage());
				return null;
			}
			}


	/**
	 * Deletes all vertices and all edges - BE CAREFULL !!!
	 */
	@Override
	public void deleteDatabase() {
		
		// Delete nodes and edges of graph
		queries.genericDeleteQuery("g.V()");
		queries.genericDeleteQuery("g.E()");
		graph.tx().commit();
		
		// Reset internal node and edge maps
		this.vertexM = new HashMap <String, Vertex> ();
		this.edgeM = new HashMap <String, Edge> ();
	}
	
	
	@Override
	public synchronized void deleteHitVertices() {
		queries.genericDeleteQuery("g.V().hasLabel('"+HitVertex+"')");
		graph.tx().commit();
		Utils.debug("Data DB ... deleted all HITS !");
	}
	
	


	@Override
	public XMLConfiguration getConfiguration() {
		return this.fidConfig;
	}
	
	/**
	 * This method assigns each CLASS and each TAG vertex in the model graph the correct annotation model
	 * (as defined in ModelDefinition.models2ClassNamespaces and ModelGroups.models2TagNamespaces.
	 */
	@Override
	public void updateModelsByNamespaces(LinkedHashMap<ModelType,String[]> models2TagNamespaces, LinkedHashMap<ModelType,String[]> models2ClassNamespaces) {
		
		String query = "";
		
		// Clear model attribute in all CLASS and TAG vertices
		ArrayList<Vertex> clist = this.getQueries().getClassNodes();
		for (Vertex v : clist) {
			v.property(GWriter.ClassModel,"");
		}
		ArrayList<Vertex> tlist = this.getQueries().getTagNodes();
		for (Vertex v : tlist) {
			v.property(GWriter.TagModel,"");
		}
		this.getQueries().commit();
		
		
		// Now update all CLASS and TAG vertices with the annotation defined in maps
		// ModelDefinition.models2ClassNamespaces and ModelDefinition.models2TagNamespaces
		
		// Update model attribute in CLASS vertices
		Utils.debug("Updating model attribute by models2ClassNamespaces in CLASS vertices !");
		for (ModelType model : models2ClassNamespaces.keySet()) {
			Utils.debug(model.name());
			if (models2ClassNamespaces.get(model).length == 0){
				Utils.debug("... has no defined namespaces !");}
			
			for (String ns : models2ClassNamespaces.get(model)) {
				
				
				// Get CLASS vertices where class has namespace ns
				query = 
						"g.V().hasLabel('"+GWriter.ClassVertex+"')"
						+ ".filter{it.get().value('"+GWriter.ClassClass+"')"
						+ ".matches('"+ns+".*')}";
				ArrayList<Vertex> vlist = this.getQueries().genericVertexQuery(query);
				
				Utils.debug(ns+ " : "+vlist.size());
				
				// Set model in found vertices
				for (Vertex v : vlist) {
					v.property(GWriter.ClassModel, model.name());
				}
				this.getQueries().commit();
			}
		}
		
		// Update model attribute in TAG vertices
		Utils.debug("Updating model attribute by models2TagNamespaces in TAG vertices !");
		for (ModelType model : models2TagNamespaces.keySet()) {
			Utils.debug(model.name());
			if (models2TagNamespaces.get(model).length == 0){
				Utils.debug("... has no defined namespaces !");}

			for (String ns : models2TagNamespaces.get(model)) {
				Utils.debugNor(ns+ " : ");

				// Get TAG vertices where class has namespace ns
				query = 
						"g.V().hasLabel('"+GWriter.TagVertex+"')"
						+ ".filter{it.get().value('"+GWriter.TagClass+"')"
						+ ".matches('"+ns+".*')}";
				ArrayList<Vertex> vlist = this.getQueries().genericVertexQuery(query);
				Utils.debug(vlist.size());

				// Set model in found vertices
				for (Vertex v : vlist) {
					v.property(GWriter.TagModel, model.name());
				}
				this.getQueries().commit();
			}
		}
		
		Utils.debug("updateModelsByNamespaces finished !");

		
	}

	@Override
	public HashSet<String> getHitEdgeKeys() {
		return hitEdgeKeys;
	}

	@Override
	public void setHitEdgeKeys(HashSet<String> hitEdgeKeys) {
		this.hitEdgeKeys = hitEdgeKeys;
	}
	
	@Override
	public void updateHitTotalTokenCount(ResourceInfo resourceInfo) {
		
		String fileQuery = "g.V().hasLabel('"+GWriter.HitVertex+"')"
				  + ".has('"+GWriter.HitResourceUrl+"','"+resourceInfo.getDataURL()+"')"
				  + ".has('"+GWriter.HitFileId+"','"+resourceInfo.getFileInfo().getFileId()+"')";
		
		String query = "";
		
		for (ModelMatch mm : resourceInfo.getFileInfo().getModelMatchings()) {
			
			switch (resourceInfo.getFileInfo().getProcessingFormat()) {
				
			case RDF :
				
				query = fileQuery
				+ ".has('"+GWriter.HitPredicate+"',"+mm.getRdfProperty()+")";
				break;
				
			case CONLL :
				
				query = fileQuery
				+ ".has('"+GWriter.HitConllColumn+"',"+mm.getConllColumn()+")"
				+ ".has('"+GWriter.HitXmlAttribute+"',"+mm.getXmlAttribute()+")";
				break;

			case XML :	// (not used by now)
				
				query = fileQuery
				+ ".has('"+GWriter.HitConllColumn+"',"+mm.getConllColumn()+")"
				+ ".has('"+GWriter.HitXmlAttribute+"',"+mm.getXmlAttribute()+")";
				break;

			default :
				
				query = "";
				break;
			}
			
			if (query.isEmpty()) continue;
			
			query+=".property('"+GWriter.HitTotalTokenCount+"',"+mm.getTotalTokenCount()+")";
			this.getQueries().genericVertexQuery(query);
			this.getQueries().commit();
		}
	}
	
}