package de.unifrankfurt.informatik.acoli.fid.search;

import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;

import de.unifrankfurt.informatik.acoli.fid.types.GEdge;
import de.unifrankfurt.informatik.acoli.fid.types.GNode;
import de.unifrankfurt.informatik.acoli.fid.types.INode;
import de.unifrankfurt.informatik.acoli.fid.types.ModelInfo;
import de.unifrankfurt.informatik.acoli.fid.types.RelationType;
import de.unifrankfurt.informatik.acoli.fid.types.TagType;

public class StreamReaderModel implements StreamRDF {
	
	String [] searchClues;
	String subj;
	String aSubj;
	String pred;
	String obj;
	String aObj;
	String pKey;
	String varPrefix = "filename#";
	String modelName;
	

	GWriter writer;

	// Graph includes class nodes
	public StreamReaderModel (GWriter writer) {
		this.writer = writer;
	}
	
	HashSet <GEdge> edges = new HashSet <GEdge> ();
	HashMap <String,INode> iNodeM = new HashMap <String,INode> ();
	HashMap <String,String> iTypeM = new HashMap <String,String> ();
	HashSet <String> individuals = new HashSet <String> ();
	
	HashMap <String,HashSet<String>> labels = new HashMap <String,HashSet<String>> ();
	HashMap <String,HashSet<String>> languages = new HashMap <String,HashSet<String>> ();
	HashSet <String> oliaHasTier = new HashSet <String> ();
	HashMap <String,String> values = new HashMap <String,String> ();

	private ModelInfo modelInfo;
	private long internalVertexCounter = 0;
	private HashMap <String, String> anonymousVertexM = new HashMap <String, String>();
	
	
	
	public void start() {
		// TODO Auto-generated method stub	
	}
	
	// TODO use clear() instead of creating new objects !
	public void reset() {
		edges = new HashSet <GEdge> ();
		labels = new HashMap <String, HashSet<String>>();
		languages = new HashMap <String, HashSet<String>>();
		iNodeM = new HashMap <String,INode> (); // use clear()
		iTypeM = new HashMap <String,String> ();// use clear()
	    individuals = new HashSet <String> ();// use clear()
	    values.clear();
	    oliaHasTier.clear();
	    internalVertexCounter = 0;
	    anonymousVertexM = new HashMap <String, String>();
	}
	

	public void triple(Triple triple) {	
		
		subj = "";
		aSubj = "";
		pred = triple.getPredicate().toString();
		pKey = pred;
		obj = "";
		aObj = "";
		
		
		if (triple.getSubject().isURI()) {
			subj = triple.getSubject().getURI().toString();
			if (subj.equals("http://www.w3.org/2002/07/owl#Thing") || subj.equals("http://www.w3.org/2002/07/owl#Nothing")) return;
		} else {
			if (triple.getSubject().isBlank()) {
				aSubj = varPrefix+triple.getSubject().getBlankNodeLabel();
				if (anonymousVertexM.containsKey(aSubj)) {
					subj = anonymousVertexM.get(aSubj);
				} else {
					subj = varPrefix+internalVertexCounter++;
					anonymousVertexM.put(aSubj, subj);
				}
			}
		}
		
		if (triple.getObject().isURI()) {
			obj = triple.getObject().getURI().toString();
			if (obj.equals("http://www.w3.org/2002/07/owl#Thing") || obj.equals("http://www.w3.org/2002/07/owl#Nothing")) return;
		} else {
			if (triple.getObject().isBlank()) {
				aObj = varPrefix+triple.getObject().getBlankNodeLabel();
				if (anonymousVertexM.containsKey(aObj)) {
					obj = anonymousVertexM.get(aObj);
				} else {
					obj = varPrefix+internalVertexCounter++;
					anonymousVertexM.put(aObj, obj);
				}
				
			} else {
				if (triple.getObject().isLiteral()) {
					obj = triple.getObject().getLiteral().toString().split("@")[0]; // remove lang
				} else {return;}
			}
			
		}
		
		
		/*
		try {
		Utils.debug();
		Utils.debug(subj);
		Utils.debug(pred);
		Utils.debug(obj);
		} catch (Exception e) {e.printStackTrace();}
		*/
		
		
		// subclass edge
		if (pred.endsWith("#subClassOf")) {
		// Add edge
		edges.add(new GEdge (new GNode (subj),new GNode(obj),RelationType.SUB));
		return;
		}
		
		// equivalent class edge
		if (pred.endsWith("owl#equivalentClass")) {
		// Add edge
		edges.add(new GEdge (new GNode (subj),new GNode(obj),RelationType.EQUIV));
		return;
		}
		
		// union edge
		if (pred.endsWith("07/owl#unionOf")) {
		// Add edge
		edges.add(new GEdge (new GNode (subj),new GNode(obj),RelationType.UNION));
		return;
		}
		
		
		// intersection edge
		if (pred.endsWith("07/owl#intersectionOf")) {
		// Add edge
		edges.add(new GEdge (new GNode (subj),new GNode(obj),RelationType.INTER));
		return;
		}
		
		// collection edge
		if (pred.endsWith("22-rdf-syntax-ns#first")) {
		// Add edge
		edges.add(new GEdge (new GNode (subj),new GNode(obj),RelationType.COLL));
		return;
		}
		
		// collection edge
		if (pred.endsWith("22-rdf-syntax-ns#rest")) {
		// Add edge
		edges.add(new GEdge (new GNode (subj),new GNode(obj),RelationType.COLL));
		return;
		}
		
		// complement edge
		if (pred.endsWith("07/owl#complementOf")) {
		// Add edge
		edges.add(new GEdge (new GNode (subj),new GNode(obj),RelationType.COMPL));
		return;
		}
		
		// tag
		if (pred.endsWith("hasTagStartingWith")) {
			iNodeM.put(subj, new INode(subj, obj, TagType.STARTS));
			return;
		}
		
		// tag
		if (pred.endsWith("m.owl#hasTagEndingWith")) {
			iNodeM.put(subj, new INode(subj, obj, TagType.ENDS));
			return;
		}
		
		// tag
		if (pred.endsWith("m.owl#hasTagMatching") || pred.endsWith("m.owl#hasTag")) {
			iNodeM.put(subj, new INode(subj, obj, TagType.MATCH));
			return;
		}
		
		// tag
		if (pred.endsWith("m.owl#hasTagContaining")) {
			iNodeM.put(subj, new INode(subj, obj, TagType.CONTAINS));
			return;
		}
		
		// label
		if (pred.endsWith("2000/01/rdf-schema#label")) {
			// Add label
			if (!labels.containsKey(subj)) {
				HashSet <String> labelList = new HashSet<String>();
				labelList.add(obj);
				labels.put(subj, labelList);
			} else {
				HashSet <String> labelList = labels.get(subj);
				labelList.add(obj);
				labels.put(subj, labelList);
			}
			
			return;
		}
		
		// other labels
		if (pred.endsWith("2004/12/q/contentlabel#hasLabel") ||
			pred.endsWith("2004/12/q/contentlabel#hasDefaultLLabel")) {
			// Add label
			if (!labels.containsKey(subj)) {
				HashSet <String> labelList = new HashSet<String>();
				labelList.add(obj);
				labels.put(subj, labelList);
			} else {
				HashSet <String> labelList = labels.get(subj);
				labelList.add(obj);
				labels.put(subj, labelList);
			}
			
			return;
		}
		
		// dc:language
		if (pred.endsWith("http://purl.org/dc/terms/language")
			|| pred.endsWith("http://purl.org/dc/elements/1.1/language")) {
			// Add language
			if (!languages.containsKey(subj)) {
				HashSet <String> languageList = new HashSet<String>();
				languageList.add(obj);
				languages.put(subj, languageList);
			} else {
				HashSet <String> languageList = languages.get(subj);
				languageList.add(obj);
				languages.put(subj, languageList);
			}
			return;
		}
		
		
		// feature class (modeled as restriction on property /olia/system.owl#hasTier
		// TODO use /olia/system.owl#hasTier instead of hasTier
		if (obj.endsWith("hasTier") && pred.endsWith("owl#onProperty")) {
			oliaHasTier.add(subj);
			return;
		}
		
		
		// values
		if (pred.endsWith("02/07/owl#hasValue")) {
			values.put(subj,obj);
			return;
		}
		
		
				
		// collect classes for tags and named individuals
		if (pred.endsWith("22-rdf-syntax-ns#type")) {
			
			iTypeM.put(subj,obj);
			
			if (obj.endsWith("owl#NamedIndividual")) {
				individuals.add(subj);
			} 
			return;
		}
	}

	
	public void quad(Quad quad) {
		// TODO Auto-generated method stub
	}

	
	public void base(String base) {
		// TODO Auto-generated method stub
	}

	
	public void prefix(String prefix, String iri) {
		// TODO Auto-generated method stub
	}

	
	public void finish() {
		
		/*
		Utils.debug("classes :");
		Utils.debug(classes.size());
		
		Utils.debug("Predicates :");
		Utils.debug(edges.size());
		*/

		// add edge from a tag containing entity to class with same name
		// (example brown:ComparativeAdverb a owl:Class; olia_system:hasTag RBR)
		for (String x : iNodeM.keySet()) {
			if (iTypeM.containsKey(x) && !iNodeM.get(x).getTag().isEmpty()) { // TAGs have been empty (model bug)
				edges.add(new GEdge (new INode (x,iNodeM.get(x).getTag(),iNodeM.get(x).getTagType()),
						  new GNode(iTypeM.get(x)),RelationType.A));
			}
		}
		
		// add edge for individual
		for (String x : individuals) {
			if (iTypeM.containsKey(x)) {
				edges.add(new GEdge (new GNode(x),new GNode(iTypeM.get(x)),RelationType.A));
			}
		}
		
		
		// Try to get an ontology identifier (URL)
		int foundOntologies = 0;
		String ontologyUrl = "";
		for (String subj : iTypeM.keySet()) {
			if (iTypeM.get(subj).equals("http://www.w3.org/2002/07/owl#Ontology")) {
				ontologyUrl = subj;
				foundOntologies++;
			}
		}
		if (foundOntologies == 1) modelInfo.setOntology(ontologyUrl);
		
		// Find restriction on olia:hasTier "FEATS"
		HashMap <String,String> tiers = new HashMap <String,String>();
		for (String subj_R : oliaHasTier) {
			if (iTypeM.containsKey(subj_R) && iTypeM.get(subj_R).endsWith("7/owl#Restriction"))
				if (values.containsKey(subj_R)) {//&& values.get(subj_R).equals("FEATS")) {
					tiers.put(subj_R,values.get(subj_R));
			}
		}
		
		
		/*
		Utils.debug("values :");
		for (String x : values.keySet()) {
			Utils.debug(x+","+values.get(x));
		}
		
		Utils.debug("oliaHasTier :");
		for (String x : oliaHasTier) {
			Utils.debug(x);
		}
		
		Utils.debug("tiers :");
		for (String x : tiers.keySet()) {
			Utils.debug(x+","+tiers.get(x));
		}
		*/
		
		
		writer.writeModel(edges, labels, languages, tiers, modelInfo);
		
	}
	
	public void setModelInfo(ModelInfo modelInfo) {
		this.modelInfo = modelInfo;
		varPrefix = "#"+FilenameUtils.removeExtension(modelInfo.getFileName())+"#";
	}

}
