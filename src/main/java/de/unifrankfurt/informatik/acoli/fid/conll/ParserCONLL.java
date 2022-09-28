package de.unifrankfurt.informatik.acoli.fid.conll;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import de.unifrankfurt.informatik.acoli.fid.detector.OptimaizeLanguageTools1;
import de.unifrankfurt.informatik.acoli.fid.parser.CSVParserA;
import de.unifrankfurt.informatik.acoli.fid.parser.CSVParserConfig;
import de.unifrankfurt.informatik.acoli.fid.search.GWriter;
import de.unifrankfurt.informatik.acoli.fid.types.LanguageMatch;
import de.unifrankfurt.informatik.acoli.fid.types.ModelType;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.util.IndexUtils;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;
import edu.stanford.nlp.util.StringUtils;


// TODO introduce new conllColumn type that subsumes all features of a column
public class ParserCONLL extends CSVParserA {
	
	// HashMap for POS-TAGS
	private HashMap <String, Long> tagCountMap = new HashMap <String, Long> ();
	private HashMap <Integer, HashMap<String,Long>> columnTokens = new HashMap <Integer, HashMap<String,Long>>();
	private HashMap<String,HashMap<String,Long>> featureMap = new HashMap<String, HashMap<String,Long>>();
	private GraphTraversalSource g;
	private int sampleLines = 10;
	ConllInfo conllInfo;
	
	private int tagNumbers = 0;


	
	public ParserCONLL(GWriter graphWriter, String versionString) {
		super(graphWriter, ModelType.valueOf("UD1DEP"), versionString);
		this.setFormatSpec("http://universaldependencies.org/format.html");
		g = graphWriter.getGraph().traversal();
	}
	
	
	public ParserCONLL(CSVParserConfig config, GWriter graphWriter, String versionString) {
		super(config, graphWriter, ModelType.valueOf("UD1DEP"), versionString);
		this.setFormatSpec("http://universaldependencies.org/format.html");
		g = graphWriter.getGraph().traversal();
	}

	/**
	 * Parse resource
	 * @param resourceInfo
	 * @return success
	 */
	@Override
	public boolean parse(ResourceInfo resourceInfo) {
		
		reset();
		
		columnTokens = new HashMap <Integer, HashMap<String,Long>>();
		ArrayList <ConllCSVSentence> sampleSentences = new ArrayList<ConllCSVSentence>();
		//String posTag = "";
		
		conllInfo = resourceInfo.getConllInfo();
		try {
			if (conllInfo == null) {
				File conllSourceFile = resourceInfo.getFileInfo().getResourceFile();
				if (!resourceInfo.getFileInfo().getTemporaryFilePath().isEmpty()) {
					Utils.debug("getTemporaryFilePath : "+resourceInfo.getFileInfo().getTemporaryFilePath());
					// use path of converted xml file
					conllSourceFile = new File(resourceInfo.getFileInfo().getTemporaryFilePath());
				}
				
				Utils.debug("ParserCONLL : parsing "+conllSourceFile.getAbsolutePath());
				
				Iterable<CSVRecord> records = CSVParser.parse(
						conllSourceFile,
						config.getCharset(),
						config.getCsvFormat()
						);
			
				conllInfo = new ConllInfo(graphWriter, records);
			}
			
			
			// Detect conll format (e.g. text, postag and feature columns)
			conllInfo.detectColumTypes();
			
			
			// Detect languages and models from first n sentences of CONLL file for each text column
			sampleSentences = conllInfo.getConllSampleSentences(
					this.getGraphWriter().getConfiguration().getInt("Processing.ConllParser.languageSampleSentences"),
					this.getGraphWriter().getConfiguration().getInt("Processing.ConllParser.languageSampleSentencesMinTokenCount"));
			
			/* old
			int languageSampleSentences = 15;
			int i=0;
			while (i++ <= languageSampleSentences) {
				ConllCSVSentence conllSentence = conllInfo
						.getCSVRecordsOfSentence(i);
				if (conllSentence != null) {
					sampleSentences.add(conllSentence);
				}
			}*/
			
			ArrayList <String> sampleText = new ArrayList <String>();
			ArrayList<LanguageMatch> foundLanguages = new ArrayList<LanguageMatch>();
			
			
			for (int column : conllInfo.getTextColumns()) {
			//if (!conllInfo.getTextColumns().isEmpty()) {
				//int column = conllInfo.getTextColumns().get(0);
				sampleText.clear();
				// Get sentences from a text column
				for (ConllCSVSentence conllSentence : sampleSentences) {					
						sampleText.add(conllSentence.getSentenceText(column));
				}
				
			
				// Detect language(s) from text sample
				ArrayList<LanguageMatch> y = OptimaizeLanguageTools1.detectAllISO639_3Languages(sampleText);
				Utils.debug("Detected the following language(s) in CONLL column "+column+" :");
				//Utils.debug("Text sample :"+sampleText);
				for (LanguageMatch lm : y) {
					// Set column info where language was found
					lm.setConllColumn(column);
					Utils.debug(lm.getLexvoUrl().toString()+"\n");
					// Set xml attributes if source was XML
					if (resourceInfo.getFileInfo().getConllcolumn2XMLAttr().containsKey(column)) {
						lm.setXmlAttribute(resourceInfo.getFileInfo().getConllcolumn2XMLAttr().get(column));
					} else {
						Utils.debug("Error : could not find XMLAttr column "+column+" in conllColumn2XMLAttr map");
					}
				}
				foundLanguages.addAll(y);
			}
			
			// Store detected languages and sample text
			resourceInfo.getFileInfo().setLanguageMatchings(foundLanguages);
			resourceInfo.getFileInfo().setLanguageSample(
					org.apache.commons.lang3.StringUtils.substring(StringUtils.join(sampleText),
							0,200)); // not important (only text of last column saved
			// concatenate possible xml sample with conll sample
			resourceInfo.getFileInfo().setSample(resourceInfo.getFileInfo().getSample()+"\n"+conllInfo.getSample(sampleLines));
			tagNumbers = 0;
			
			for (int col : conllInfo.getPosTagColumns()) {
				
				tagCountMap = new HashMap <String, Long> ();
				// Parse POS-TAGS for every sentence of the file
				for (CSVRecord record : conllInfo.getCSVRecords()) {
					// Only allow : (used for ud dependencies rarely)
					for (String tag : record.get(col).split(":")) {
					   tag = tag.trim();if (tag.isEmpty()) continue;
					   //String tag = record.get(col);
					   //if (!PosTagFilter.tagHasAlpha(tag)) continue;
					   if (PosTagFilter.tagIsNumber(tag)) {
						   //Utils.debug("***"+tag);
						   tagNumbers++;
					   }
					   if (!tagCountMap.containsKey(tag)) tagCountMap.put(tag, 1L);
					   else tagCountMap.put(tag, tagCountMap.get(tag)+1);
					}
			   }
			
			   // omit column if many tags are numbers
			   if (tagNumbers > 100) {
				   tagCountMap.clear();
				   Utils.debug("Skipping postag column "+col+" after 100 found number tokens !");
				   tagNumbers=0;
				   continue;
			   }
				
			   Utils.debug("tags for postag column : "+col);
			   for (String t : tagCountMap.keySet()) {
				   Utils.debug(t);
			   }
			   
			   // Write tag vertices in graph
			   this.getGraphWriter().writeConll(col, tagCountMap, resourceInfo, null, null);
			   columnTokens.put(col, tagCountMap);
			}
			
			
			for (int fcol : conllInfo.getFeatureColumns()) {
				
				featureMap = new HashMap<String, HashMap<String,Long>>();

				for (CSVRecord record : conllInfo.getCSVRecords()) {
					//Utils.debugNor("Featurecolumn : "+fcol+" : ");
					//Utils.debug(record.get(fcol));
					
					// Parse features
					addFeatures(featureMap, record.get(fcol));
				}
				
				// show feature map
				Utils.debug("featureMap : "+featureMap.size());
				for (String feature : featureMap.keySet()) {
					Utils.debug("feature : "+feature);
					HashMap<String,Long> x = featureMap.get(feature);
					
					for (String value : x.keySet()) {
						Utils.debugNor(value+" : ");
						Utils.debug(x.get(value));
					}
					;
				}
				
				// Write found features (FeatureName, FeatureValue) as hit vertices in graph
				this.getGraphWriter().writeConllFeatures(fcol , featureMap, resourceInfo, null);
				
				HashMap<String,Long> featuresPlusValues = new HashMap<String,Long>();
				for (String feature : featureMap.keySet()) {
					
					HashMap<String,Long> values = featureMap.get(feature);
					for (String value : values.keySet()) {
						featuresPlusValues.put(feature+"="+value,values.get(value));
					}
				}
				columnTokens.put(fcol, featuresPlusValues);
			}
			
			
			// pattern columns (e.g. XY+Z+AB)
			
			for (int vcol : conllInfo.getPatternColumns().keySet()) {
				
				String vectorDelimiter = conllInfo.getPatternColumns().get(vcol);
				tagCountMap = new HashMap <String, Long> ();
				
				// Parse vectors in column for every sentence of the file
				for (CSVRecord record : conllInfo.getCSVRecords()) {   
				   
				   String vector = record.get(vcol);
				   
				   // omit column if many tags are numbers
				   if (tagNumbers > 100) {
					   tagCountMap.clear();
					   Utils.debug("Skipping pattern column "+vcol+" after 100 found number tokens !");
					   tagNumbers=0;
					   break;
				   }
				   
				   // split vector with found delimiter
				   //Utils.debug("pattern token : "+vector);
				   for (String ctag : org.apache.commons.lang3.StringUtils.split(vector,vectorDelimiter)){
				   //for (String ctag : vector.split("\\"+vectorDelimiter)){

					   Utils.debug("ctag : "+ctag);
					   for (String btag : ctag.split("\\|")) {
						   //Utils.debug("btag :"+btag);
					   // Make additional split on :
						   for (String tag : btag.split(":")) {
							   //Utils.debug("splitted : "+tag);
							   //Utils.debug("tag :"+tag);
							   tag = tag.trim();
							   if (PosTagFilter.tagIsNumber(tag)) {
								   //Utils.debug("***"+tag);
								   tagNumbers++;
							   }
							   //if (tag.matches("\\d+")) {continue;}
							   if (!tagCountMap.containsKey(tag)) tagCountMap.put(tag, 1L);
							   else tagCountMap.put(tag, tagCountMap.get(tag)+1);
						   	}
					   }
				   }
			   }
			
				if (!tagCountMap.isEmpty()) {
				   Utils.debug("tags for pattern column : "+vcol);
				   for (String t : tagCountMap.keySet()) {
					   Utils.debug(t);
				   }
				   
				   
				   // Write tag vertices in graph
				   this.getGraphWriter().writeConll(vcol, tagCountMap, resourceInfo, null, null);
				   columnTokens.put(vcol, tagCountMap);
				}
			}
			
			// Implementation not finished : example ? 
			// vectors of tokens with equal length
			// (disabled because not matchings seen so far)
			// TODO split vectors / create new annotation models
			/*if (conllInfo.getTreebankVectorColumn() > -1) {
				
				int tcol = conllInfo.getTreebankVectorColumn();
				tagNumbers = 0;
				
				// Parse vectors in column for every sentence of the file
				for (CSVRecord record : conllInfo.getCSVRecords()) {   
				   
				   String vector = record.get(tcol);
				   
				   // omit column if many tags are numbers
				   if (tagNumbers > 100) {
					   tagCountMap.clear();
					   Utils.debug("Skipping treebank vector column "+tcol+" after 100 found number tokens !");
					   tagNumbers=0;
					   break;
				   }
				   
				   if (PosTagFilter.tagIsNumber(vector)) {
					   //Utils.debug("***"+tag);
					   tagNumbers++;
				   }
				   //if (tag.matches("\\d+")) {continue;}
				   if (!tagCountMap.containsKey(vector)) tagCountMap.put(vector, 1L);
				   else tagCountMap.put(vector, tagCountMap.get(vector)+1);
			   	
			   }
			
				if (!tagCountMap.isEmpty()) {
				   Utils.debug("tags for pattern column : "+tcol);
				   for (String t : tagCountMap.keySet()) {
					   Utils.debug(t);
				   }
				      
				   // Write tag vertices in graph
				   this.getGraphWriter().writeConll(tcol, tagCountMap, resourceInfo, null, null);
				   columnTokens.put(tcol, tagCountMap);
				}
			}*/
			
			
			}
			catch (InvalidConllException e) {
				//e.printStackTrace();
				resourceInfo.getFileInfo().setErrorCode(IndexUtils.ERROR_CONLL_INVALID);
				resourceInfo.getFileInfo().setErrorMsg(IndexUtils.ERROR_CONLL_INVALID);
			    return false;
			}
			catch (ConllFileTooSmallException e) {
				e.printStackTrace();
				resourceInfo.getFileInfo().setErrorCode(IndexUtils.ERROR_CONLL_FILE_TOO_SMALL);
				resourceInfo.getFileInfo().setErrorMsg(IndexUtils.ERROR_CONLL_FILE_TOO_SMALL);
		    	return false;
			}
			catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		
		
		resourceInfo.getFileInfo().setColumnTokens(columnTokens);
		resourceInfo.getFileInfo().setSampleSentences(sampleSentences);
		return true;
	}
	
	
	/**
	 * Reads assignments feature=value in conll CSV column into HashMap
	 * @return Mapping from features to their assigned values
	 */
	public static void addFeatures (HashMap<String,HashMap<String,Long>> featureMap, String token) {
		
		if (token.equals("_")) return;
		// parse features from complex token
		// Mood=Ind|Number=Sing|Person=3|Tense=Pres|VerbForm=Fin
		String feature = "";
		String value = "";
		String [] splited;
		
		try {
			for (String subToken : token.split("\\|")) {
				//Utils.debug("Subtoken : "+ subToken);
				splited = subToken.trim().split("=");
				if (splited.length < 2) {
					Utils.debug("Error parsing token :"+subToken);
					continue;
				}
				feature = splited[0];
				value = splited[1];
				//Utils.debug("feature : "+feature);
				//Utils.debug("value : "+value);
		
			if (!featureMap.containsKey(feature)) {
				HashMap<String,Long> values = new HashMap<String,Long>();
				values.put(value,1L);
				featureMap.put(feature, values);
			} else {
				HashMap<String,Long> values = featureMap.get(feature);
				if (values.containsKey(value)) {
					values.put(value,values.get(value)+1);
				} else {
					values.put(value, 1L);
				}
				featureMap.put(feature, values);
			}
			
			}
		} catch (Exception e) {
			Utils.debug("Error parsing token : "+ token);
			e.printStackTrace();
		}
	}
	
	
	public HashMap <String, Long> getTagCountMap() {
		return tagCountMap;
	}
	


	@Override
	public void reset() {
		
		tagCountMap.clear(); // not required because always new instances created
		featureMap.clear();  // not required because always new instances created
	}

}