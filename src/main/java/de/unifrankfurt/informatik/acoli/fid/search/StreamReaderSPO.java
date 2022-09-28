package de.unifrankfurt.informatik.acoli.fid.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;

import de.unifrankfurt.informatik.acoli.fid.types.DetectionMethod;
import de.unifrankfurt.informatik.acoli.fid.types.DetectionSource;
import de.unifrankfurt.informatik.acoli.fid.types.ProcessingFormat;
import de.unifrankfurt.informatik.acoli.fid.types.InvalidLanguageException;
import de.unifrankfurt.informatik.acoli.fid.types.LanguageMatch;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.VocabularyMatch;
import de.unifrankfurt.informatik.acoli.fid.types.VocabularyType;
import de.unifrankfurt.informatik.acoli.fid.util.RDFPrefixUtils;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;


public class StreamReaderSPO implements StreamRDF {
	
	String [] searchClues;
	String conllNs;
	String subj;
	String pred;
	String obj;
	String objL;
	String pKey;
	String language;
	boolean isConllFile=false;
	

	GWriter writer;
	
	public StreamReaderSPO (String [] searchClues, String conllNs, GWriter streamWriter) {
		this.searchClues = searchClues;
		this.conllNs = conllNs;
		this.writer = streamWriter;
	}
	

	
	public long tripleCount = 0;
	HashMap <String, Long> subjects = new HashMap <String, Long> ();
	HashMap <String, Long> predicates = new HashMap <String, Long> ();
	HashMap <String, Long> objects = new HashMap <String, Long> ();
	HashMap <String, HashMap<String,Long>>	  languageMap = new HashMap <String, HashMap<String, Long>> ();
	private ResourceInfo resourceInfo;
	
	HashSet <String> prefixes = new HashSet <String> ();


	
	@Override
	public void start() {
		// TODO Auto-generated method stub	
	}

	public void reset() {
		tripleCount=0;
		subjects.clear();
		predicates.clear();
		objects.clear();
		languageMap.clear();
		isConllFile = false;
		prefixes.clear();
	}
	
	@Override
	public void triple(Triple triple) {
		
		tripleCount++;
		
		subj = "";
		pred = triple.getPredicate().toString();
		pKey = pred;
		obj = "";
		objL = "";
		
		prefixes.add(triple.getPredicate().getNameSpace());
		
		
		if (triple.getObject().isURI())
			obj = triple.getObject().getURI().toString();
	
		
		if (triple.getObject().isLiteral()) {
			objL = triple.getObject().getLiteral().toString();
			String regex = ".*[:/@., \\[\\]\\{\\}].*|\\d+";
			if (objL.matches(regex)) {
				objL = "";
			} else {
				pKey =  pred+","+objL;
			}
			language = triple.getObject().getLiteralLanguage();
			// get language annotation
			language = triple.getObject().getLiteralLanguage();
			if (!language.isEmpty()) {
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
		}
		
		//TODO Check if file declares namespace instead of testing each tripel
		if (pred.startsWith(conllNs,7)) isConllFile = true;
		
		
		for (String clue : searchClues) {
			
			if  (pred.contains(clue) && !pred.endsWith("#comment") && !pred.endsWith("#versionInfo") && !pred.endsWith("#label")) {
				if (!predicates.containsKey(pKey)) {
					  predicates.put(pKey,1L);
					  //Utils.debug("pred : "+pred);
				 	} else {
				 	  predicates.replace(pKey ,predicates.get(pKey) + 1);
				    }
			}
			
			if  (obj.contains(clue)) {
				 if (!objects.containsKey(pred+","+obj)) {
					  objects.put(pred+","+obj,1L);
					  //Utils.debug("obj : "+ obj);
				 	} else {
				 	  objects.replace(pred+","+obj ,objects.get(pred+","+obj) + 1);
				    }
			}
			
			
			if  (subj.contains(clue)) {
				 if (!subjects.containsKey(subj)) {
					  subjects.put(subj,1L);
					  //Utils.debug("subj : "+ subj);
				 	} else {
				 	  subjects.replace(subj ,subjects.get(subj) + 1);
				    }
			}
			
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
		
		Utils.debug("Subjects :");
		Utils.debug(subjects.keySet().size());
		
		Utils.debug("Predicates :");
		Utils.debug(predicates.keySet().size());
		
		Utils.debug("Objects :");
		Utils.debug(objects.keySet().size());
		
		
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
		
		
		if (isConllFile) {
			Utils.debug("RDF file in CONLL format found !");
			resourceInfo.getFileInfo().setProcessingFormat(ProcessingFormat.CONLL);
			// do not process file with writer but reparse it with conll-RDF parser later !
			return;
		}
		
		
		if (foundDocuments()) {
		
			// Set language matchings (type langtag)
			ArrayList<LanguageMatch> lms = new ArrayList<LanguageMatch>();
			for (String lang : languageMap.keySet()) {
				for (String predicate : languageMap.get(lang).keySet()) {
					
					try {
						LanguageMatch lm = new LanguageMatch(lang, languageMap.get(lang).get(predicate), DetectionMethod.AUTO);
						lm.setDetectionSource(DetectionSource.LANGTAG);
						lm.setAverageProb(1.0f);
						lm.setRdfProperty(predicate);
						lms.add(lm);
						
					} catch (InvalidLanguageException e) {}
				}
			}
			
			resourceInfo.getFileInfo().setLanguageMatchings(lms);
			writer.writeRdf(subjects, predicates, objects, resourceInfo);
			
		}
	}
	
	/**
	 * Test weather documents where found during the parse
	 * @return True if documents where found else false
	 */
	public boolean foundDocuments () {
		return !(subjects.isEmpty() && predicates.isEmpty() && objects.isEmpty());
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

}
