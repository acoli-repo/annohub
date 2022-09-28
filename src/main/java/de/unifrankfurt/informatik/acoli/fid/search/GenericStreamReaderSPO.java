package de.unifrankfurt.informatik.acoli.fid.search;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;

import de.unifrankfurt.informatik.acoli.fid.types.DetectionMethod;
import de.unifrankfurt.informatik.acoli.fid.types.DetectionSource;
import de.unifrankfurt.informatik.acoli.fid.types.ProcessingFormat;
import de.unifrankfurt.informatik.acoli.fid.types.InvalidLanguageException;
import de.unifrankfurt.informatik.acoli.fid.types.LanguageMatch;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceMetadata;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.VocabularyMatch;
import de.unifrankfurt.informatik.acoli.fid.types.VocabularyType;
import de.unifrankfurt.informatik.acoli.fid.util.RDFPrefixUtils;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;


public class GenericStreamReaderSPO implements StreamRDF {
	
	String conllNs;	
	String pred;
	String obj;
	String language;
	boolean isConllFile=false;
	GWriter writer;
	public long tripleCount = 0;
	HashMap <String, HashMap<String,Long>> predicates2LitObjects = new HashMap <String, HashMap<String,Long>> ();
	HashMap <String, HashMap<String,Long>> predicates2URIObjects = new HashMap <String, HashMap<String,Long>> ();
	HashMap <String, HashMap<String,Long>> languageMap = new HashMap <String, HashMap<String, Long>> ();
	HashMap <String, HashMap<String,Long>> languageTagMap = new HashMap <String, HashMap<String,Long>> ();

	private ResourceInfo resourceInfo;
	HashSet <String> prefixes = new HashSet <String> ();
	private long maxSamples = 50L ;
	HashSet<String> allowedPredicates = new HashSet<String>();
	boolean WRITE = false;
	final long NOSAMPLING = 1000000000000L;
	boolean sampleMode = false;
	private boolean ontology;
	private Boolean parseMetadata;
	private ResourceMetadata resourceMetadata;
	
	
	private static HashSet <String> languageProperties = new HashSet <String> () {
		private static final long serialVersionUID = 1L;
	{
				add("http://purl.org/dc/terms/language");
				add("http://purl.org/dc/elements/1.1/language");
				add("http://lexvo.org/ontology#iso639P3PCode");
				add("http://lexvo.org/ontology#iso6392BCode");
				add("http://lexvo.org/ontology#iso6392TCode");
				add("http://lexvo.org/ontology#iso639P1Code");
				add("http://lexvo.org/ontology#iso639P5Code");
				add("http://lemon-model.net/lemon#language");
				add("http://www.w3.org/ns/lemon/lime#language");
				add("http://lexvo.org/ontology#language");
				add("http://mlode.nlp2rdf.org/resource/ids/vocabulary/hasIsoCode");
	}};
	
	
	private static HashSet <String> objectNameSpaceFilter = new HashSet <String> () {
		private static final long serialVersionUID = 1L;
	{
				add("http://rdfs.org/ns/");
				add("http://www.w3.org/1999/");
				add("http://www.w3.org/2002/");
				add("http://www.w3.org/2004/");
				add("http://purl.org/dc");
	}};
	
	
	public GenericStreamReaderSPO (String conllNs, GWriter streamWriter, long maxSamples) {
		this.conllNs = conllNs;
		this.writer = streamWriter;
		this.maxSamples = maxSamples;
		this.parseMetadata = parseMetadata;
	}
	
	@Override
	public void start() {
		// TODO Auto-generated method stub	
	}

	public void reset() {
		tripleCount=0;
		predicates2LitObjects.clear();
		predicates2URIObjects.clear();
		allowedPredicates.clear();
		languageMap.clear();
		languageTagMap.clear();
		isConllFile = false;
		prefixes.clear();
		ontology = false;
		parseMetadata = false;
		resourceMetadata = new ResourceMetadata();
	}
	
	@Override
	public void triple(Triple triple) {
		

		// count triples
		tripleCount++;
		
		pred = triple.getPredicate().toString(); if (pred.startsWith(conllNs,7)) isConllFile = true;
		
		// save prefix
		prefixes.add(triple.getPredicate().getNameSpace());
		
		obj = "";
		if (triple.getObject().isLiteral()) {
			obj = triple.getObject().getLiteral().toString();
		}
		if (triple.getObject().isURI()) {
			obj = triple.getObject().getURI().toString();
		}
		
		// skip all other object nodes
		if (obj.isEmpty()) return;
		
		// parse resource metadata
		if (parseMetadata) {
			switch (pred) {
				
				case "http://purl.org/dc/elements/1.1/title":			// title
				case "http://purl.org/dc/terms/title":
					resourceMetadata.setTitle(obj);
					break;
					
				case "http://purl.org/dc/terms/creator":				// creator
					resourceMetadata.setCreator(obj);
					break;
				
				case "http://www.w3.org/2000/01/rdf-schema#comment":	// description
				case "http://purl.org/dc/terms/bibliographicCitation":
					//resourceMetadata.setDescription(obj);
					if (!obj.trim().isEmpty() && 
						!resourceMetadata.getDescription().contains(obj.trim())) {
							if (resourceMetadata.getDescription().isEmpty()) {
								resourceMetadata.setDescription(obj.trim());
							} else {
								resourceMetadata.setDescription(resourceMetadata.getDescription()+";"+obj.trim());
							}
					}
					break;
					
				case "http://purl.org/dc/elements/1.1/publisher":		// publisher
				case "http://purl.org/dc/terms/publisher":
					resourceMetadata.setPublisher(obj);
					break;
					
				case "http://purl.org/dc/elements/1.1/format":			// format
					resourceMetadata.setFormat(obj);
					break;
					
				case "http://purl.org/dc/elements/1.1/date":			// year
					resourceMetadata.setYear(obj);
					break;
					
				case "http://purl.org/dc/terms/issued":					// date
				case "http://purl.org/dc/terms/dateSubmitted":	 
					//resourceMetadata.setDate(new Date(obj));
					break;
					
				case "http://purl.org/dc/terms/rights":					// rights
					if (!obj.trim().isEmpty()) {
						resourceMetadata.setRights(obj);
					}
					break;
					
				case "http://purl.org/dc/elements/1.1/license":			// license
				case "http://purl.org/dc/terms/license":
					if (!obj.trim().isEmpty()) {
						resourceMetadata.setLicense(obj);
					}
					break;
				
				case "http://purl.org/dc/terms/source":					// source
					if (!obj.trim().isEmpty()) {
						resourceMetadata.setDctSource(obj);
					}
					break;
					
				case "http://purl.org/dc/terms/identifier":				// identifier
					if (!obj.trim().isEmpty()) {
						resourceMetadata.setDctIdentifier(obj);
					}				
				
				default:
			}
		}
		
		// get language with property
		if (languageProperties.contains(pred) || 
			StringUtils.substring(pred.toLowerCase(),pred.lastIndexOf("/")+1,pred.length()).contains("iso"))	
				{addLanguage(obj.trim());return;}
		
		// get language from literal
		if (triple.getObject().isLiteral()) {
			
			// skip triple if object literal is too long for an annotation
			if (obj.length() > 35) return; 
			
			// get language annotation
			language = triple.getObject().getLiteralLanguage();
			if (!language.isEmpty()) {
				addLanguageTag(language);
				// TODO can return ?
			}
			
			// skip triple if object literal does'nt look like an annotation
			// (string contains one of ":/@.,[]{}" OR is a number)
			String regex = ".*[:/@., \\[\\]\\{\\}].*|\\d+";
			if (obj.matches(regex)) {
				return; 
			}
		}
		
		
		if (sampleMode || allowedPredicates.contains(pred)) {
			
			// CASE 1 : object is URI
			if (triple.getObject().isURI()) {
				//obj = triple.getObject().getURI().toString(); (see above)
				
				if (obj.equals("http://www.w3.org/2002/07/owl#Ontology")) {
					ontology = true;
				}
				
				// apply name-space filtering for objects
				for (String filter : objectNameSpaceFilter) {
					if (obj.startsWith(filter)) return;
				}
				
				
				
				if  (!pred.endsWith("#comment") && !pred.endsWith("#versionInfo") && !pred.endsWith("#label")) {
					if (!predicates2URIObjects.keySet().contains(pred)) {
						HashMap<String,Long> values = new HashMap<String,Long>();
						values.put(obj,1L);
						predicates2URIObjects.put(pred,values);
					} else {
					 	if (predicates2URIObjects.get(pred).size() < maxSamples) {
					 		HashMap<String,Long> values = predicates2URIObjects.get(pred);
					 		if (!values.containsKey(obj)) {
					 			values.put(obj, 1L);
					 		} else {
					 			values.replace(obj, values.get(obj)+1);
					 		}
					 	}
					}
				}
				return;
			}
			
			// CASE 2 : object is literal
			if (triple.getObject().isLiteral()) {
				
				if  (!pred.endsWith("#comment") && !pred.endsWith("#versionInfo") && !pred.endsWith("#label")) {
					if (!predicates2LitObjects.keySet().contains(pred)) {
						HashMap<String,Long> values = new HashMap<String,Long>();
						values.put(obj,1L);
						predicates2LitObjects.put(pred,values);
					} else {
					 	if (predicates2LitObjects.get(pred).size() < maxSamples) {
					 		HashMap<String,Long> values = predicates2LitObjects.get(pred);
					 		if (!values.containsKey(obj)) {
					 			values.put(obj, 1L);
					 		} else {
					 			values.replace(obj, values.get(obj)+1);
					 		}
					 	}
					}
				}
			}
		}
	}

	// for languages found with language properties
	private void addLanguage(String language) {
		
		//Utils.debug("addLanguage "+language);
		
		if (languageMap.containsKey(language)) {
			HashMap<String,Long> values = languageMap.get(language);
			if (values.containsKey(pred)) {
				values.replace(pred, values.get(pred)+1);
			} else {
				values.put(pred, 1L);
			}
		} else {
			HashMap<String,Long> values = new HashMap<String,Long>();
			values.put(pred, 1L);
			languageMap.put(language, values);
		}
	}
	
	// for languages found in language tags (@)
	private void addLanguageTag(String language) {
		
		if (languageTagMap.containsKey(language)) {
			HashMap<String,Long> values = languageTagMap.get(language);
			if (values.containsKey(pred)) {
				values.replace(pred, values.get(pred)+1);
			} else {
				values.put(pred, 1L);
			}
		} else {
			HashMap<String,Long> values = new HashMap<String,Long>();
			values.put(pred, 1L);
			languageTagMap.put(language, values);
		}
	}
	

	@Override
	public void quad(Quad quad) {
		// TODO Auto-generated method stub
	}

	@Override
	public void base(String base) {
		// TODO Auto-generated method stub
	}

	@Override
	public void prefix(String prefix, String iri) {
		prefixes.add(iri);
	}

	@Override
	public void finish() {
		
		Utils.debug("Sample size : "+maxSamples);
		Utils.debug("Different predicates : "+(predicates2LitObjects.keySet().size()+predicates2URIObjects.keySet().size()));
		Utils.debug("\n");
		Utils.debug("Different predicates with object URI: "+predicates2LitObjects.keySet().size());
		for (String p : predicates2LitObjects.keySet()) {
			Utils.debug("predicate : "+p+" ("+predicates2LitObjects.get(p).size()+" objects)");
			for (String o : predicates2LitObjects.get(p).keySet()) {
				Utils.debug(o+" : "+predicates2LitObjects.get(p).get(o));
			}
			Utils.debug("\n");
		}
		
		Utils.debug("Different predicates with literal object : "+predicates2URIObjects.keySet().size());
		for (String p : predicates2URIObjects.keySet()) {
			Utils.debug("predicate : "+p+" ("+predicates2URIObjects.get(p).size()+" objects)");
			for (String o : predicates2URIObjects.get(p).keySet()) {
				Utils.debug(o+" : "+predicates2URIObjects.get(p).get(o));
			}
			Utils.debug("\n");
		}
		

		// Save found vocabularies
		ArrayList<VocabularyMatch> vocabularyMatchings = new ArrayList<VocabularyMatch>();
		for (String iri : prefixes) {
			VocabularyType v = RDFPrefixUtils.getVocabularyFromPrefix(iri);
			if (v != null) {
				Utils.debug("PREFIX : "+iri+" ---> "+v.name());
				vocabularyMatchings.add(new VocabularyMatch(v));
			} else {
				Utils.debug("IRI "+iri+" does not match any known vocabulary !");
			}
		}
		
		resourceInfo.getFileInfo().setVocabularyMatchings(vocabularyMatchings);
		
		if (parseMetadata) {
			resourceInfo.setResourceMetadata(resourceMetadata);
		}
		
		if (isConllFile) {
			Utils.debug("RDF file in CONLL format found !");
			resourceInfo.getFileInfo().setProcessingFormat(ProcessingFormat.CONLL);
			// do not process file with writer but reparse it with conll-RDF parser later !
			return;
		}
		
		
		if (foundDocuments() || true) {
			
			Utils.debug("languageMap : "+languageMap.size());
		
			// Set language matchings (type langProp)
			ArrayList<LanguageMatch> lms = new ArrayList<LanguageMatch>();
			for (String lang : languageMap.keySet()) {
				for (String predicate : languageMap.get(lang).keySet()) {
					
					try {
						LanguageMatch lm = new LanguageMatch(lang, languageMap.get(lang).get(predicate), DetectionMethod.AUTO);
						lm.setDetectionSource(DetectionSource.LANGPROP);
						lm.setAverageProb(1.0f);
						lm.setRdfProperty(predicate);
						lm.setHitCount(languageMap.get(lang).get(predicate));
						lms.add(lm);
						Utils.debug("Adding language matching for : "+lang);
						
					} catch (InvalidLanguageException e) {}
				}
			}
			
			// Set language matchings (type langTag)
			for (String lang : languageTagMap.keySet()) {
				for (String predicate : languageTagMap.get(lang).keySet()) {
					
					try {
						LanguageMatch lm = new LanguageMatch(lang, languageTagMap.get(lang).get(predicate), DetectionMethod.AUTO);
						lm.setDetectionSource(DetectionSource.LANGTAG);
						lm.setAverageProb(1.0f);
						lm.setRdfProperty(predicate);
						lm.setHitCount(languageTagMap.get(lang).get(predicate));
						lms.add(lm);
						Utils.debug("Adding language matching for : "+lang);
						
					} catch (InvalidLanguageException e) {}
				}
			}

			resourceInfo.getFileInfo().setLanguageMatchings(lms);
			
			// write to model database
			if (WRITE) {
				write(predicates2LitObjects, predicates2URIObjects, resourceInfo);
			}
			
		}
	}
	
	
	public HashMap <String, HashMap<String,Long>> getPredicates2LitObjects() {
		return predicates2LitObjects;
	}
	
	public HashMap <String, HashMap<String,Long>> getPredicates2URIObjects() {
		return predicates2URIObjects;
	}
	
	/**
	 * Test weather documents where found during the parse
	 * @return True if documents where found else false
	 */
	public boolean foundDocuments () {
		return !(predicates2LitObjects.isEmpty() && predicates2URIObjects.isEmpty());
	}
	
	/**
	 * Get triple count of parsed file
	 * @return tripleCount
	 */
	public long getTripleCount () {
		return tripleCount;
	}
	
	public void setResourceInfo (ResourceInfo resourceInfo) {
		this.resourceInfo = resourceInfo;
	}

	public boolean getIsConllFile() {
		return isConllFile;
	}

	public long getMaxSamples() {
		return maxSamples;
	}

	public void setMaxSamples(long maxSamples) {
		this.maxSamples = maxSamples;
	}

	public HashSet<String> getAllowedPredicates() {
		return allowedPredicates;
	}

	public void setAllowedPredicates(HashSet<String> allowedPredicates) {
		this.allowedPredicates = allowedPredicates;
	}

	public void setRunModeSampling(ResourceInfo resourceInfo, long maxSamples) {
		reset();
		this.resourceInfo = resourceInfo;
		this.WRITE = false;
		this.maxSamples = maxSamples;
		this.sampleMode = true;
		this.parseMetadata = false;
	}
	
	public void setRunModeWrite(ResourceInfo resourceInfo, HashSet<String> allowedPredicates) {
		reset();
		this.resourceInfo = resourceInfo;
		this.WRITE = true;
		this.maxSamples = NOSAMPLING;
		this.allowedPredicates = allowedPredicates;
		this.sampleMode = false;
		this.parseMetadata = true;
	}
	
	
	public boolean isWRITE() {
		return WRITE;
	}

	public void setWRITE(boolean wRITE) {
		WRITE = wRITE;
	}

	public void write(HashMap<String, HashMap<String, Long>> allPredicates,
			HashMap<String, HashMap<String, Long>> allObjects,
			ResourceInfo sampleResource) {
		
		writer.writeGenericRdf(predicates2LitObjects, predicates2URIObjects, resourceInfo);	
	}

	public boolean isOntology() {
		return ontology;
	}

	public void setParseMetadata(Boolean parseMetadata) {
		this.parseMetadata = parseMetadata;
	}


}
