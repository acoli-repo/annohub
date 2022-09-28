package de.unifrankfurt.informatik.acoli.fid.detector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.apache.maven.shared.utils.io.FileUtils;

import com.optimaize.langdetect.DetectedLanguage;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileBuilder;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.profiles.LanguageProfileWriter;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;

import de.unifrankfurt.informatik.acoli.fid.jena.RDFDataMgr;
import de.unifrankfurt.informatik.acoli.fid.parser.ParserISONames;
import de.unifrankfurt.informatik.acoli.fid.types.DetectionMethod;
import de.unifrankfurt.informatik.acoli.fid.types.InvalidLanguageException;
import de.unifrankfurt.informatik.acoli.fid.types.LanguageMatch;
import de.unifrankfurt.informatik.acoli.fid.types.ModelMatch;
import de.unifrankfurt.informatik.acoli.fid.types.ModelType;
import de.unifrankfurt.informatik.acoli.fid.util.IndexUtils;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;
import edu.emory.mathcs.backport.java.util.Collections;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.DocumentPreprocessor;




public class OptimaizeLanguageTools1 {
	
	
	private static LanguageProfileReader languageProfileReader;
	//private static List<LanguageProfile> languageProfiles;
	private static HashMap<String, LanguageProfile> languageProfileMap = new HashMap<String, LanguageProfile>();
	//private static HashSet<String> languageProfileLanguages = new HashSet<String>();
	private static ArrayList<File> optimaizeExtraProfiles = null;
	private static ArrayList<File> optimaizeManualProfiles = null;
	private static LanguageDetector languageDetector;
	private static TextObjectFactory textObjectFactory;
	private static Dataset lexvoDump;
	private static XMLConfiguration fidConfig = null;
	
	private static String lexvoRdfFilePath="";
	
		
	
	/**
	 * Determine probabilities for languages in text
	 * @param text
	 * @return Sorted list of languages with best match on top
	 */
	public static List<DetectedLanguage> getLanguageProbabilities (String text) {
		
		//initLanguageDetector();
		
		//String language="";
        Utils.debug("Get language probabilities :");


		try {
			TextObject textObject = textObjectFactory.forText(text);
			
			// 1. find match with high probability (> 0,85)
			//com.google.common.base.Optional<LdLocale> lang = languageDetector.detect(textObject);
			//if (lang.isPresent()) {
			//	language = lang.get().getLanguage();
			
			List<DetectedLanguage> detectedLanguages = languageDetector.getProbabilities(textObject);
			Collections.sort(detectedLanguages);
			
			Utils.debug("Sample sentence : "+text);
			if (!detectedLanguages.isEmpty()) {
				for (DetectedLanguage xy : detectedLanguages) {
					Utils.debug(xy.getLocale().getLanguage()+" "+xy.getProbability());
				}
			}
			
			return detectedLanguages;
			
			/*// Get language with highest probability
			if (!detectedLanguages.isEmpty()) {
			 language = detectedLanguages.get(0).getLocale().getLanguage();
			}
			
			Utils.debug("probabilities :");
			for (DetectedLanguage xy : languageDetector.getProbabilities(textObject)) {
				Utils.debug(xy.getLocale().getLanguage()+" "+xy.getProbability());
			}
			 
			if (!language.isEmpty()) 
				return TikaTools.convertLanguageIsoCode639ToLexvoUrl(language);
			else 
				return null;*/
		
		} catch (Exception e) {e.printStackTrace();}
		
		return null;
	}
	
	
	
	/**
	 * Detect dominant language in text file (Can not handle multiple languages).
	 * Returns null if probability for any language < 90 % !
	 * @param file
	 * @return Lexvo URL for iso-639 language code
	 * @deprecated ?
	 */
	public static URL detectISO639_3_Language (TextObject textObject) {
				
		try {
			com.google.common.base.Optional<LdLocale> lang = languageDetector.detect(textObject);
			return TikaTools.getLexvoUrlFromISO639_3Code(lang.get().getLanguage());
			
		} catch (Exception e) {e.printStackTrace();}
		
		return null;
	}
	
	
	public static LanguageProfile createOptimaizeLanguageProfile(File trainingData, String languageIso639_3, File targetDirectory) {
		
		try {
			String text = FileUtils.fileRead(trainingData);
			return createOptimaizeLanguageProfile (text, languageIso639_3, targetDirectory);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}		
	}
	
	public static LanguageProfile createOptimaizeLanguageProfile(String text, String languageIso639_3, File targetDirectory) {
		
		try {
		//create text object factory:
		TextObjectFactory textObjectFactory = CommonTextObjectFactories.forIndexingCleanText();

		//load your training text:
		TextObject inputText = textObjectFactory.create()
		        .append(text);
		        //.append("training text");

		//create the profile:
		LanguageProfile languageProfile = new LanguageProfileBuilder(LdLocale.fromString(languageIso639_3))
		        .ngramExtractor(NgramExtractors.standard())
		        .minimalFrequency(5) //adjust please
		        .addText(inputText)
		        .build();
		
		if (targetDirectory != null && targetDirectory.exists()) {
			new LanguageProfileWriter().writeToDirectory(languageProfile, targetDirectory);
		}
		
		return languageProfile;
		
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	public static String writeLanguageProfile(LanguageProfile lp, File targetDir, boolean forceOverwrite) {
		
		File file = new File(targetDir, lp.getLocale().getLanguage());
        if (file.exists() && !forceOverwrite) {
            return ("File exists already, refusing to overwrite: "+file);
        }
        try (FileOutputStream output = new FileOutputStream(file)) {
            new LanguageProfileWriter().write(lp, output);
        } catch (Exception e) {
        	e.printStackTrace();
        	return e.getMessage();
        }
        return "";
	}
	


	public static List<LanguageProfile> getLanguageProfiles() {
		return new ArrayList<LanguageProfile>(languageProfileMap.values());
	}
	
	
	public static ArrayList<File> getExtraOptimaizeProfiles() {
		return optimaizeExtraProfiles;
	}
	
	public static void setExtraOptimaizeProfiles(ArrayList<File> extraProfiles) {
		optimaizeExtraProfiles = extraProfiles;
	}

	public static void initLanguageDetector(
			XMLConfiguration fidConfig_,
			ArrayList<File> extraProfiles,
			ArrayList<File> manualProfiles,
			File lexvoRdfFile)
	{
		
		fidConfig = fidConfig_;
		setExtraOptimaizeProfiles(extraProfiles);
		setManualOptimaizeProfiles(manualProfiles);
		setLexvoRdfFilePath(lexvoRdfFile.getAbsolutePath());
		
		initLanguageDetector();
	}
	
	
	public static void initLanguageDetector() {
		
		//if(languageDetector != null) return;
		
		languageProfileReader = new LanguageProfileReader();
		
		//optimaizeAnnotationModelsProfilesDir= new File(fidConfig.getString("RunParameter.OptimaizeAnnotationModelsProfilesDirectory"));

		
		try {
			
			List<LanguageProfile>internalLanguageProfiles = languageProfileReader.readAllBuiltIn();
			Utils.debug("Loading builtin Optimaize language profiles : ("+internalLanguageProfiles.size()+")");
			
			// internal profiles in ISO-6391 format (2-letter codes)
			// convert language codes to ISO-6393
			for (LanguageProfile x : internalLanguageProfiles) {
				// convert ISO6391 code to ISO-6393
				String iso6393 = TikaTools.getISO639_3CodeFromISOCode(x.getLocale().getLanguage());
				languageProfileMap.put(iso6393, x);
			}
			
			
			Utils.debug("Loading additional profiles");
			
			// all 639-3 codes
			for (File profile : optimaizeExtraProfiles) {
								
				LanguageProfile externalProfile = languageProfileReader.read(profile);
				languageProfileMap.put(externalProfile.getLocale().getLanguage(), externalProfile);
				//languageProfiles.add(externalProfile);
				//Utils.debug("Reading profile o.k.");
			};
			
			
			
			Utils.debug("Loading manual profiles");
			
			// all 639-3 codes
			for (File profile : optimaizeManualProfiles) {
								
				LanguageProfile manualProfile = languageProfileReader.read(profile);
				languageProfileMap.put(manualProfile.getLocale().getLanguage(), manualProfile);
				//languageProfiles.add(manualProfile);
			};
			
			// experimental
			/*for (String profile : IndexUtils.listRecFilesInDir(optimaizeAnnotationModelsProfilesDir)) {
				
				LanguageProfile annotationModelProfile = 
						languageProfileReader.read(new File(optimaizeAnnotationModelsProfilesDir, new File(profile).getName()));
				languageProfiles.add(annotationModelProfile);
				//Utils.debug("Reading profile o.k.");
			}*/
			
			
			updateLanguageDetector();
			
			//create a text object factory
			textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();

			} catch (Exception e) {e.printStackTrace();}
		
		
			System.out.println("\nOptimaize languages profiles (total) : "+languageProfileMap.size());
			//System.out.println("\nOptimaize languages profiles (total) : "+languageProfileLanguages.size());
			/*if (languageProfileLanguages.size()>0) { // breaks for missing lexvos otherwise
				Utils.debug("Example : "+languageProfileLanguages.get(0));
			}*/
			// Read lexvo dataset
			try{
				Utils.debug("loading lexvo RDF " + lexvoRdfFilePath);
				lexvoDump = RDFDataMgr.loadDataset(lexvoRdfFilePath);
				} catch (Exception e) {e.printStackTrace();}
			
			
			// Read file with iso codes + language names
			ParserISONames ips = new ParserISONames();
			ips.parse();
			
			}
	
	public static void updateLanguageDetector() {
		
		//build language detector:
		Utils.debug("updateLanguageDetector ...");
		languageDetector =null;
		languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
				.probabilityThreshold(0.1d)
		        .withProfiles(languageProfileMap.values())
		        .build();
		Utils.debug("finished !");
	}
	
	
	private static ArrayList<String> splitText2Sentences(String text) {
		
		ArrayList <String> splitSentences = new ArrayList <String>();
		try {
			String filePath = "/tmp/tmp.txt";
			FileUtils.fileWrite(filePath, StandardCharsets.UTF_8.name(),text);
			DocumentPreprocessor dp = new DocumentPreprocessor(filePath);
			
			
			String sentence = "";
		    for (List<HasWord> tokenList : dp) {
		    	sentence = "";
		    	for (HasWord token : tokenList) {
		    		sentence += token.word()+" ";
		    	}
		    	//splitSentences.add(tokenList.toString());
		    	splitSentences.add(sentence);
		    }
			
			// Split text into sentences
//		    for (List<HasWord> tokenList : dp) {
//		    	splitSentences.add(tokenList.toString());
//		    }
		} catch (Exception e) {e.printStackTrace();}
		
		return splitSentences;
	}
	
	
		
	public static ArrayList <LanguageMatch> detectAllISO639_3Languages(String text) {
		
		// Detect language for each sentence
	    return detectAllISO639_3Languages(splitText2Sentences(text));
	}
	
	/**
	 * Detects languages in sentences
	 * @param sampleSentences
	 * @return list with all found languages
	 */
	public static ArrayList <LanguageMatch> detectAllISO639_3Languages(ArrayList<String> sampleSentences) {
		
		List <DetectedLanguage> bestDetectedLanguages = new ArrayList<DetectedLanguage>();
		
		for (String sampleSentence : sampleSentences) {
			
			List<DetectedLanguage> detectedLanguages = getLanguageProbabilities(sampleSentence);
				if (detectedLanguages == null || detectedLanguages.isEmpty()) continue;
			DetectedLanguage bestLanguage = detectedLanguages.get(0);
			bestDetectedLanguages.add(detectedLanguages.get(0));
		}
		
	    return computeBestLanguageMatchings(bestDetectedLanguages);
	}
	
	
	/**
	 * Detects languages in sentences
	 * @param sampleSentences
	 * @return list with all found languages
	 */
	public static ArrayList <DetectedLanguage> detectRawISO639_3Languages(String text) {
		
		ArrayList<String> sampleSentences = splitText2Sentences(text);
		HashSet <DetectedLanguage> detectedLanguages = new HashSet<DetectedLanguage>();
		
		for (String sampleSentence : sampleSentences) {
			detectedLanguages.addAll(getLanguageProbabilities(sampleSentence));
		}
		
	    return new ArrayList<DetectedLanguage>(detectedLanguages);
	}
	
	
	
	public static ArrayList<LanguageMatch> detectedLanguage2LanguageMatch(List<DetectedLanguage> detectedLanguages) {

		//Utils.debug("detectedLanguages : "+detectedLanguages.size());
		ArrayList<LanguageMatch> result = new ArrayList<LanguageMatch>();
		
		HashMap<String,Integer> count = new HashMap<String,Integer>();
		HashMap<String,Double> minProb = new HashMap<String,Double>();
		HashMap<String,Double> maxProb = new HashMap<String,Double>();
		HashMap<String,ArrayList<Double>> probs = new HashMap<String,ArrayList<Double>>();
		
		String lang;
		Double prob;
		
		for (DetectedLanguage dl : detectedLanguages) {
			
			lang = dl.getLocale().getLanguage();
			prob = dl.getProbability();
			
			//Utils.debug("lang :"+lang);
			//Utils.debug("prob :"+prob);
			if (!count.containsKey(lang)) {
				count.put(lang, 1);
				minProb.put(lang, prob);
				maxProb.put(lang, prob);
				ArrayList<Double> plist = new ArrayList<Double>();
				plist.add(prob);
				probs.put(lang, plist);
			} else {
				count.put(lang, count.get(lang)+1);
				// TODO two lines lines below not used
				if (minProb.get(lang) < prob) minProb.put(lang, prob); //  (minProb.get(lang) > prob) ??
				if (maxProb.get(lang) > prob) maxProb.put(lang, prob); //  (maxProb.get(lang) > prob) ??
				ArrayList<Double> plist = probs.get(lang);
				plist.add(prob);
				probs.put(lang, plist);
			}
		}
		
		for (String lc : count.keySet()) {
			
			LanguageMatch lm;
			try {
			lm = new LanguageMatch(lc);
			} catch (InvalidLanguageException e) {
				continue;
			}
			
			lm.setHitCount(new Long(count.get(lc)));
			ArrayList<Double> plist = probs.get(lc);
			Collections.sort(plist);
			lm.setSelected(false);
			
			if (!plist.isEmpty()) {
				lm.setMinProb(plist.get(0).floatValue());
				lm.setMaxProb(plist.get(plist.size()-1).floatValue());
				double sum = plist.stream().collect(Collectors.summingDouble(d -> d));
				lm.setAverageProb((float) (sum/detectedLanguages.size())); // TODO (sum/plist.size())) ??
			}
			result.add(lm);
		}
		return result;
	}
	

	
	public static LanguageMatch computeBestLanguageMatching(List<DetectedLanguage> detectedLanguages) {
		
		ArrayList <LanguageMatch> tmp = computeBestLanguageMatchings(detectedLanguages);
		if (tmp.size() > 0) return tmp.get(0);
		else 
		return null;
	}
	
	
	public static ArrayList <LanguageMatch> computeBestLanguageMatchings(List<DetectedLanguage> detectedLanguages) {
		
		ArrayList<LanguageMatch> result = detectedLanguage2LanguageMatch(detectedLanguages);	
		
		// all entries in result are from the same column !
		LanguageMatch bestLanguage = null;
		float bestAverage = 0.0f;
		for (LanguageMatch x : result) {
			if (x.getAverageProb() > bestAverage) {
				bestAverage = x.getAverageProb();
				bestLanguage = x;
			}
		}
		
		ArrayList<LanguageMatch> bestLanguages = new ArrayList<LanguageMatch>();
		if (bestLanguage != null) {
			bestLanguage.setSelected(true);
			bestLanguages.add(bestLanguage);
		}
		
		return bestLanguages;
	}
	


	/**
	 * Detect annotation models by using language profiles (experimental)
	 * @param sampleSentences Tokens in CONLL CSV file
	 * @param column CONLL CSV column
	 * @return List of possible annotation models sorted by probability
	 */
	public static ArrayList <ModelMatch> detectAnnotationModells(ArrayList<String> sampleSentences, int column) {
		
		ArrayList <ModelMatch> modelMatchings = new ArrayList <ModelMatch>();
		HashMap<String,Integer> modelCounts = new HashMap<String,Integer>();
		
		for (String sampleSentence : sampleSentences) {
			
			TextObject textObject = textObjectFactory.forText(sampleSentence);
			com.google.common.base.Optional<LdLocale> lang = languageDetector.detect(textObject);
			if (!lang.isPresent()) continue;
			
			String modelCode = lang.get().getLanguage();
			
	        if (modelCode != null) {
	        	if (modelCounts.containsKey(modelCode)) {
	        		modelCounts.put(modelCode, modelCounts.get(modelCode)+1);
	        	} else {
	        		modelCounts.put(modelCode, 1);
	        	}
	        }
		}
		
		    
	    // Add models that start with modelCode (modelCode has to be a 3-letter code in lowercase !)
	    for (String modelCode : modelCounts.keySet()) {
	    	for (ModelType modelType : ModelType.values()) {
	    		if (modelType.name().toLowerCase().startsWith(modelCode)) {
	    			ModelMatch mm = new ModelMatch(modelType, 0L, new Long(modelCounts.get(modelCode)), DetectionMethod.AUTO);
	    			mm.setConllColumn(column);
	    			mm.setCoverage(1.0f * modelCounts.get(modelCode) / sampleSentences.size());
	    			modelMatchings.add(mm);
	    			Utils.debug("Found model profile for : "+modelType);
	    			break;
	    		}
	    	}
	    }
	    
	    //Collections.sort(modelMatchings,Collections.reverseOrder()); done later
	    return modelMatchings;
	}
	
	/**
	 * Get used configuration
	 * @return
	 */
	public static XMLConfiguration getFIDconfig() {
		return fidConfig;
	}

	/**
	 * Set configuration (required before using all methods)
	 * @param fidConfig
	 */
	public static void setFIDConfig(XMLConfiguration fidConfig) {
		OptimaizeLanguageTools1.fidConfig = fidConfig;
	}
	
	
	public static void main (String [] args) {
		
		String configurationFile = System.getProperty("user.dir")+"/FIDConfig.xml";
	 	Utils.debug("Configuration file : "+configurationFile);
		Configurations configs = new Configurations();
    	try {
    	    fidConfig = configs.xml(configurationFile);
    	}
    	catch (ConfigurationException cex)
    	{
    		cex.printStackTrace();
    		System.exit(0);
    	}
	
		OptimaizeLanguageTools1.setFIDConfig(fidConfig);
		OptimaizeLanguageTools1.initLanguageDetector();
		//OptimaizeLanguageTools1.getISO639_3_CodeForLanguageName("English");
		
		String [] langNames = {
				"Lezghian",
				"Tabasaran",
				"Agul",
				"Rutul",
				"Tsakhur",
				"Kryts",
				"Budukh",
				"Archi",
				"Udi"};
		for (String name : langNames) {
			Utils.debug(name +" : "+ OptimaizeLanguageTools1.getISO639_3_CodeForLanguageName(name));
		}
		
		if (true) return;
		
		//OptimaizeLanguageTools1.createOptimaizeLanguageProfile(new File("/home/debian7/Arbeitsfläche/trainingData.txt"),
		//		"en", new File("/home/debian7/Arbeitsfläche/LanguageProfiles"));
		File rootDir = new File("/media/EXTRA/GITHUB-Bibles/xml/txt");
		//File rootDir = new File("/media/EXTRA/bibles.all/german-otfrit-870/data/txt");
		//File rootDir = new File("/media/EXTRA/bibles.all/other/txt");
		File targetDir = new File("/media/EXTRA/LanguageProfiles");
		  
		for (String file : IndexUtils.listRecFilesInDir(rootDir)) {
		Utils.debug(file);
		try {
			File sourceFile = new File(file);
			String iso = StringUtils.substring(sourceFile.getName(),0, 3);
			Utils.debug(iso);
			File targetFile = new File(targetDir,FilenameUtils.removeExtension(sourceFile.getName())+".txt");
			OptimaizeLanguageTools1.createOptimaizeLanguageProfile(sourceFile,iso,targetDir);
			} 
		catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
			}
		}
		}


	public static String getLexvoRdfFilePath() {
		return lexvoRdfFilePath;
	}


	public static void setLexvoRdfFilePath(String lexvoRdfFilePath) {
		OptimaizeLanguageTools1.lexvoRdfFilePath = lexvoRdfFilePath;
	}
	
	
	/**
	 * Try to detect a ISO639-3 identifier from the (English) name (label) of a language 
	 * @param languageLabel (without language tag - will be choosen as en!)
	 * @return Lexvo URL with iso-639-3 language code
	 * TODO Input string must have (any) language tag !
	 */
	public static URL getISO639_3_CodeForLanguageName(String languageLabel) {
		
		// Parse language label / iso639P3PCode
		/*
		 * <http://lexvo.org/id/iso639-3/bdx>
    		lvont:iso639P3PCode "bdx"^^xsd:string ;
    		lvont:label <http://lexvo.org/id/term/eng/Budong-Budong> ;
    		a <lvont:Language> ;
    		rdfs:label "Budong-Budong"@en ; // can be list ! "a"@b,"c"@d ...
    		skos:prefLabel "Budong-Budong"@en .
		 */
		
		Utils.debug("Looking up ISO-Code for : "+languageLabel);
		
		if (lexvoDump == null) {
			Utils.debug("Error : LanguageDetector not initialized !");
			return null;
		}
		
		String query = 
				  "PREFIX lvont: <http://lexvo.org/ontology#>"
				+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
				+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
				+ "PREFIX skos: <http://www.w3.org/2008/05/skos#>"
				+ "Select ?lexvoUrl where {"
				+ "?lexvoUrl a <lvont:Language> ;"
				+ "skos:prefLabel ?x;" 
				+ "FILTER (lcase(str(?x)) = \""+languageLabel.toLowerCase()+"\").}";
				
				//+ "rdfs:label \"English\"@en .}";
				//+ "skos:prefLabel \""+languageLabel+"\"@en .}"

		
		Utils.debug(query);
			
		QueryExecution qExec = QueryExecutionFactory.create(query, lexvoDump);
		ResultSet rs = qExec.execSelect();
		
	
		Resource lexvoUrl=null;
		
		while(rs.hasNext()) {
			
		    QuerySolution qs = rs.next() ;
		    lexvoUrl = qs.getResource("lexvoUrl") ;
		    Utils.debug("LexvoUrl for : "+languageLabel+" -> "+lexvoUrl.toString()) ;
		    break;
			}
		
		if (lexvoUrl == null) {
			Utils.debug("nothing found");
			return null;
		}
		
		// Convert to Url
		try {
			return new URL(lexvoUrl.toString());
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	public static String detectISO639_3_Code(File file) {
		try {
			TextObject textObject = textObjectFactory.forText(FileUtils.fileRead(file));
			return new File(detectISO639_3_Language(textObject).getPath()).getName();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	/**
	 * Detect dominant language in text file (Can not handle multiple languages).
	 * Returns null if probability for any language < 90 % !
	 * @param file
	 * @return Lexvo URL for iso-639 language code
	 * @deprecated
	 */
	public static URL detectISO639_3_Language (File file) {

		try {
			TextObject textObject = textObjectFactory.forText(FileUtils.fileRead(file));
			return detectISO639_3_Language(textObject);
	
		} catch (Exception e) {e.printStackTrace();}
		
		return null;
	}



	public static ArrayList<File> getManualOptimaizeProfiles() {
		return optimaizeManualProfiles;
	}



	public static void setManualOptimaizeProfiles(
			ArrayList<File> optimaizeManualProfiles) {
		OptimaizeLanguageTools1.optimaizeManualProfiles = optimaizeManualProfiles;
	}



	public static HashMap<String, LanguageProfile> getLanguageProfileMap() {
		return languageProfileMap;
	}



	public static void setLanguageProfileMap(
			HashMap<String, LanguageProfile> languageProfileMap) {
		OptimaizeLanguageTools1.languageProfileMap = languageProfileMap;
	}
	
}


   