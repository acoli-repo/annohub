package de.unifrankfurt.informatik.acoli.fid.util;

import java.util.HashMap;

import de.unifrankfurt.informatik.acoli.fid.types.VocabularyType;

public class RDFPrefixUtils {
	
	
	// Vocabularies that are used for corpus and lexica creation and serve as an indicator
	// for an RDF document with a linguistic content
	public static final HashMap<String,VocabularyType> vocabulariesForCorpusAndLexicaCreation = new HashMap<String,VocabularyType>() {
		   private static final long serialVersionUID = 1363621361L;
		
	{
				put("http://ufal.mff.cuni.cz/conll2009-st/task-description.html#",VocabularyType.CONLL);
				put("http://purl.org/linguistics/gold",VocabularyType.GOLD);
				put("http://www.xces.org/ns/GrAF/1.0/",VocabularyType.GRAF);
				put("http://www.monnet-project.eu/lemon#",VocabularyType.LEMON);
				put("http://lemon-model.net/lemon#",VocabularyType.LEMON);
				put("http://www.lemon-model.net/lemon#",VocabularyType.LEMON);
				put("http://www.w3.org/ns/lemon/ontolex#",VocabularyType.LEMON);
				put("http://www.lexinfo.net/ontology/2.0/lexinfo",VocabularyType.LEXINFO);
				put("http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#",VocabularyType.NIF);
				put("http://purl.org/olia/olia.owl",VocabularyType.OLIA);
				put("http://purl.org/olia/olia-system.owl",VocabularyType.OLIA);
				put("http://purl.org/olia/olia-top.owl",VocabularyType.OLIA);
				put("http://purl.org/powla/powla.owl",VocabularyType.POWLA); //https://sourceforge.net/p/powla/code/HEAD/tree/trunk/owl/powla.owl
				put("http://purl.org/olia/ubyCat.owl",VocabularyType.UBY);
				
	}};
	
	
	
	// Vocabularies that include annotation definitions (e.g. V,NP)
	public static final HashMap<String,VocabularyType> vocabulariesWithAnnotationDefs = new HashMap<String,VocabularyType>() {
		private static final long serialVersionUID = 1363621361L;
	{
				put("http://purl.org/olia/olia.owl",VocabularyType.OLIA);
				put("http://purl.org/olia/olia-system.owl",VocabularyType.OLIA);
				put("http://purl.org/olia/olia-top.owl",VocabularyType.OLIA);
				put("http://purl.org/linguistics/gold",VocabularyType.GOLD);

	}};
	
	
	// Vocabularies that can be used to define POS tags
	public static final HashMap<String,VocabularyType> vocabulariesWithTagAssignmentCapabilities = new HashMap<String,VocabularyType>() {
		   private static final long serialVersionUID = 1363621361L;
		{
					put("http://purl.org/olia/olia",VocabularyType.OLIA);
					// hasTag, hasTagStartingWith, hasTagEndingWith, hasTagMatching, hasTagContaining
					
					put("http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#",VocabularyType.NIF);
					// posTag, annotation, Annotation, oliaLink, oliaCategory
					
					put("http://purl.org/powla/powla.owl",VocabularyType.POWLA); //https://sourceforge.net/p/powla/code/HEAD/tree/trunk/owl/powla.owl
					// hasPos, hasCat, hasAnnotation, hasMetadata
					
					put("http://purl.org/olia/ubyCat.owl",VocabularyType.UBY);
					// has_partOfSpeech, Lemma, LexicalAnnotation (more?)
					
					// only XML 
					put("http://www.xces.org/ns/GrAF/1.0/",VocabularyType.GRAF);
					/*
					<a label="FE" ref="fn-n2"
							as="FrameNet">
							<fs>
							<f name="FE" value="Recipient"/>
							<f name="GF" value="Obj"/>
							<f name="PT" value="NP"/>
							</fs>
					</a>
					*/
							
					
					

		}};
	

	// General purpose vocabularies
	public static final HashMap<String,VocabularyType> generalPurposeVocabularies = new HashMap<String,VocabularyType>() {
		private static final long serialVersionUID = 1363621361L;
	{
		// general purpose
		put("http://www.w3.org/ns/oa",VocabularyType.WAM); // https://www.w3.org/TR/annotation-vocab/#hastarget, https://www.w3.org/TR/annotation-model/#aims-of-the-model			
		put("http://nerd.eurecom.fr/ontology#",VocabularyType.NERD);
		put("http://www.w3.org/2005/11/its/rdf#",VocabularyType.ITSRDF);
		put("http://www.isocat.org/",VocabularyType.ISOCAT);
		

	}};
	
	
	
	public static VocabularyType getVocabularyFromPrefix(String url) {
		for (String prefixUrl : vocabulariesForCorpusAndLexicaCreation.keySet()) {
			
			if (url.startsWith(prefixUrl) || prefixUrl.startsWith(url)) return vocabulariesForCorpusAndLexicaCreation.get(prefixUrl);
		}
		return null;
	}
	
	public static boolean isVocabularyPrefix(String url) {
		if (vocabulariesForCorpusAndLexicaCreation.keySet().contains(url)) return true;
		else return false;
	}
	
	
	public static boolean isModelPrefix(String url) {
		if (vocabulariesWithAnnotationDefs.keySet().contains(url)) return true;
		else return false;
	}
}
