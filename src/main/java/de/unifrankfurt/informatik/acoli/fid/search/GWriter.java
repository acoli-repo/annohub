package de.unifrankfurt.informatik.acoli.fid.search;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.unifrankfurt.informatik.acoli.fid.gremlinQuery.BllTools;
import de.unifrankfurt.informatik.acoli.fid.gremlinQuery.EmbeddedQuery;
import de.unifrankfurt.informatik.acoli.fid.types.GEdge;
import de.unifrankfurt.informatik.acoli.fid.types.ModelInfo;
import de.unifrankfurt.informatik.acoli.fid.types.ModelType;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;


/**
 * Interface for reading/writing  computation graph
 * @author frank
 *
 */

public interface GWriter {

	// Vertices
	final String ClassVertex 		= "CLASS";
	final String HitVertex	    	= "HIT";
	final String WordVertex			= "WORD";
	final String TagVertex			= "TAG";
	final String ProductionVertex 	= "PROD";
	final String GrammarStartVertex = "S";
	// Shared vertex properties
	final String Model			= "model";
	final String Class			= "class";
	// Vertex Properties
	final String File			= "file";
	final String ClassFile		= "file";
	final String ClassLabel		= "label"; // for neo4j label_1, label_2, label_3, ... is used !
	final String ClassLabel1	= "label_1";
	final String ClassLabel2	= "label_2";
	final String ClassLabel3	= "label_3";
	final String ClassLang639	= "lang639";
	final String ClassHasTier 	= "tier";
	final String ClassModel	 	= Model;
	final String ClassClass		= Class;
	final String TagFile		= "file";
	final String TagClass		= ClassClass;
	final String TagModel	 	= Model;
	final String TagTag	 		= "tag";
	final String TagType		= "type";
	final String HitResourceUrl	= "resourceUrl";
	final String HitFileId		= "fileId";
	final String HitType		= "type";
	final String HitPredicate	= "pred";
	final String HitObject		= "obj";
	final String HitCount		= "count";
	final String HitTag			= "tag";
	final String HitConllColumn	= "conllCol";
	final String HitXmlAttribute= "xmlAttr";
	final String HitFeature		= "featureN";
	final String HitFeatureValue		= "featureV";
	final String HitTotalTokenCount 	= "ttc";
	final String WordRepr				= "repr";
	final String WordLang				= "lang";
	final String GrammarStartModel 		= "SourceModel";
	final String GrammarStartFileUrl	= "fileURL";
	final String ProductionFileUrl 		= "fileURL";
	final String ProductionLhs			= "lhs"; // left hand side
	final String ProductionT1			= "t1";  // terminal_1
	final String ProductionT2			= "t2";  // terminal_2 
	
	// Edge Properties
	final String TypeEdge			= "A";
	final String SuperEdge			= "SUPER";
	final String SubEdge			= "SUB";
	final String InterEdge			= "INTER";
	final String UnionEdge			= "UNION";
	final String ComplementEdge		= "COMP";
	final String EquivalentEdge		= "EQUIV";
	final String CollectionEdge		= "COLL";
	final String TagMatchEdge		= "tmatch";
	final String FeatureMatchEdge	= "fmatch";
	final String ClassMatchEdge		= "cmatch";
	final String WordEdge			= "repr";
	final String GrammarEdge		= "d";
	final String ProductionOfEdge	= "prodOf"; // edge from Production -> GrammarStartVertex
	
	// Olia constants
	final String OliaTierFeats	= "FEATS";
	final String OliaTier		= "tier";
	
	// Hit types 
	final String HitTypeLiteralObject = "l";
	final String HitTypeURIObject = "u";
	final String HitTypeTag		  = "t";
	final String HitTypeFeature   = "f";
	final String HitTypeXML   	  = "x";
	
	/**
	 * Write method for models
	 * @param edges
	 * @param labels
	 * @param languages
	 * @param tiers
	 * @param modelInfo
	 */
	void writeModel(HashSet<GEdge> edges, HashMap<String, HashSet<String>> labels, HashMap<String, HashSet<String>> languages, HashMap<String,String> tiers, ModelInfo modelInfo);
	
	
	
	/**
	 * Write method for parsed result of source files (in any RDF format)
	 * Write parsed subjects, predicates and objects to tinkerpop graph
	 * @param subjects Map : subjects -> subject count
	 * @param predicates Map : predicates -> predicate count
	 * @param objects Map : objects -> object count
	 * @param languages TODO
	 */
    void writeRdf (HashMap<String, Long> subjects, 
    		HashMap<String, Long> predicates,
    		HashMap<String, Long> objects,
    		ResourceInfo resourceInfo
    		); 
	
	
	/**
	 * Write method for found posTags in conll csv file
	 * @param column
	 * @param tagCounts
	 * @param resourceInfo
	 * @param model
	 * @param tagWords
	 */
	void writeConll (int column, HashMap<String, Long> tagCounts,
					   ResourceInfo resourceInfo, ModelType model,
					   HashMap <String,HashSet<String>> tagWords
					   );
	/**
	 * Write method for found features in conll csv file
	 * @param csvColumn
	 * @param featureMap
	 * @param resourceInfo
	 * @param model
	 */
	void writeConllFeatures(int csvColumn, HashMap<String, HashMap<String, Long>> featureMap,
			ResourceInfo resourceInfo, ModelType model);
	
	Graph getGraph();
	
	
	HashMap <String, Vertex> getVertexMap();
	
	
	HashMap <String, Edge> getEdgeMap();
	
	
	void saveAsJSON (String file);
	
	
	void saveAsML (String file);
	
	
	void saveAsGyro (String file);
	

	// Add CLASS edge
	/**
	 * 
	 * @param _class
	 * @param disregardedPredicates TODO
	 * @param modelType
	 * @param vt
	 */
	void addEdgeHit2Class (Vertex hit, String _class, HashMap<String, Integer> disregardedPredicates, ModelType modelType);
	
	
	// Add TAG edge
	// misses hitTag = URL
	void addEdgeHit2Tag (Vertex hit, String hitTag, ModelType modelFilter, HashMap <String, Integer> discardedPredicates);
	

	
	/**
	 * Remove all data
	 */
	void clear();


	/**
	 * Close etc.
	 */
	public void finish();
	
	EmbeddedQuery getQueries();
	
	void deleteDatabase();

	void deleteHitVertices();

	void writeGenericRdf(HashMap<String, HashMap<String, Long>> predicates,
			HashMap<String, HashMap<String, Long>> objects,
			ResourceInfo resourceInfo);

	
	BllTools getBllTools();
	
	XMLConfiguration getConfiguration();

	void updateModelsByNamespaces(LinkedHashMap<ModelType,String[]> tags2namespaces, LinkedHashMap<ModelType,String[]> classes2namespaces);

	void addEdgeFeatureHit2Tag(Vertex hitVertex, String featureName,
			String featureValue, ModelType modelFilter);

	void restoreVertexMapEdgeMap();

	HashSet<String> getHitEdgeKeys();

	void setHitEdgeKeys(HashSet<String> hitEdgeKeys);

	void updateHitTotalTokenCount(ResourceInfo resourceInfo);

	AnnotationCache getAnnotationCache();
	
}


