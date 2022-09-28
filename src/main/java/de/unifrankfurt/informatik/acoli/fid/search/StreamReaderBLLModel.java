package de.unifrankfurt.informatik.acoli.fid.search;

import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;

import de.unifrankfurt.informatik.acoli.fid.types.GEdge;
import de.unifrankfurt.informatik.acoli.fid.types.GNode;
import de.unifrankfurt.informatik.acoli.fid.types.ModelInfo;
import de.unifrankfurt.informatik.acoli.fid.types.RelationType;

public class StreamReaderBLLModel implements StreamRDF {
	
	String [] searchClues;
	String subj;
	String aSubj;
	String pred;
	String obj;
	String aObj;
	String pKey;
	String varPrefix = "filename#";
	String fileName;
	
	
	GWriter writer;

	// Graph includes class nodes
	public StreamReaderBLLModel (GWriter writer) {
		this.writer = writer;
	}
	
	HashSet <GEdge> edges = new HashSet <GEdge> ();
	HashMap <String,String> iTypeM = new HashMap <String,String> ();
	HashSet <String> individuals = new HashSet <String> ();
	
	HashMap <String,HashSet<String>> labels = new HashMap <String,HashSet<String>> ();
	HashMap <String,HashSet<String>> languages = new HashMap <String,HashSet<String>> ();
	private ModelInfo modelInfo;
	private long internalVertexCounter = 0;
	private HashMap <String, String> anonymousVertexM = new HashMap <String, String>();

	
	
	
	public void start() {
		// TODO Auto-generated method stub	
	}
	
	public void reset() {
		edges = new HashSet <GEdge> ();
		iTypeM = new HashMap <String,String> ();
		internalVertexCounter = 0;
		anonymousVertexM = new HashMap <String, String>();
	    labels = new HashMap <String, HashSet<String>>();
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
		edges.add(new GEdge (new GNode (obj),new GNode(subj),RelationType.SUPER));
		return;
		}
		
		// equivalent class edge
		if (pred.endsWith("owl#equivalentClass")) {
		// Add edge
		edges.add(new GEdge (new GNode (obj),new GNode(subj),RelationType.EQUIV));
		return;
		}
		
		// union edge
		if (pred.endsWith("07/owl#unionOf")) {
		// Add edge
		edges.add(new GEdge (new GNode (obj),new GNode(subj),RelationType.UNION));
		return;
		}
		
		
		// intersection edge
		if (pred.endsWith("07/owl#intersectionOf")) {
		// Add edge
		edges.add(new GEdge (new GNode (obj),new GNode(subj),RelationType.INTER));
		return;
		}
		
		// collection edge
		if (pred.endsWith("22-rdf-syntax-ns#first")) {
		// Add edge
		edges.add(new GEdge (new GNode (obj),new GNode(subj),RelationType.COLL));
		return;
		}
		
		// collection edge
		if (pred.endsWith("22-rdf-syntax-ns#rest")) {
		// Add edge
		edges.add(new GEdge (new GNode (obj),new GNode(subj),RelationType.COLL));
		return;
		}
		
		// complement edge
		if (pred.endsWith("07/owl#complementOf")) {
		// Add edge
		edges.add(new GEdge (new GNode (obj),new GNode(subj),RelationType.COMPL));
		return;
		}
		
		
		// individual
		if (pred.endsWith("22-rdf-syntax-ns#type")) {
			
			iTypeM.put(subj,obj);
			
			if (obj.endsWith("owl#NamedIndividual")) {
				individuals.add(subj);
			} 
			
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
		
		// language
		if (pred.endsWith("http://lexvo.org/ontology#iso639P3Code")) {
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
		
		HashMap<String,String> tiers = new HashMap<String,String>();
		writer.writeModel(edges, labels, languages, tiers, modelInfo);
	
	}
	
	public void setModelInfo(ModelInfo modelInfo) {
		this.modelInfo = modelInfo;
		varPrefix = "#"+FilenameUtils.removeExtension(modelInfo.getFileName())+"#";
	}

}
