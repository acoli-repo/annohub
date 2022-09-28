package de.unifrankfurt.informatik.acoli.fid.conll;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.unifrankfurt.informatik.acoli.fid.detector.OptimaizeLanguageTools1;
import de.unifrankfurt.informatik.acoli.fid.jena.RDFDataMgr;
import de.unifrankfurt.informatik.acoli.fid.search.GWriter;
import de.unifrankfurt.informatik.acoli.fid.types.LanguageMatch;
import de.unifrankfurt.informatik.acoli.fid.types.ModelType;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;
import edu.emory.mathcs.backport.java.util.Collections;
import edu.stanford.nlp.util.StringUtils;



public class ConllInfo {
	
	private ArrayList<CSVRecord> csvRecords;
	private int columnCount = 0;
	private int rowCount = 0;
	private int treebankVectorColumn = -1;
	private final int treebankPostagVectorMinLength = 2;
	private HashSet <Integer> textColumns = new HashSet <Integer>();
	private ArrayList <Integer> featureColumns = new ArrayList <Integer>();
	//private ArrayList <Integer> featureColumns = new ArrayList <Integer>(5); // Default TODO default might conflict with other columns
	private ArrayList<Integer> posTagColumns = new ArrayList<Integer>();
	private HashMap<Integer,String> patternColumns = new HashMap<Integer,String>();
	private HashMap<Integer,ModelType> columns2Models = new HashMap<Integer,ModelType>();
	private final float posTagThreshold = 0.4f;	// Determines the amount of recognized posTags needed to mark a column as posTag column
	private int modelSampleSentenceMinTokens = 40;
	private GWriter writer;
	private int wordIndexColumn = 0; // default
	
	private HashSet<String> differentTokensInSample = new HashSet<String>();
	private float patternThreshold = 3;
	private int minimalFileRowCount = 100; // model and language detection not applicable for small files
	//private HashMap <Integer, String> column2XMLAttr = new HashMap <Integer, String>();
	private int maximalFileRowCount = 2000;
	private int maxSampleSentenceSize = 100;

	
	
	public ConllInfo (GWriter writer, Iterable<CSVRecord> r) throws InvalidConllException, ConllFileTooSmallException{
		
		this.writer = writer;
		this.csvRecords = (ArrayList<CSVRecord>) IteratorUtils.toList(r.iterator());
		
		try {
			this.modelSampleSentenceMinTokens = writer.getConfiguration().getInt("Processing.ConllParser.modelSampleSentenceMinTokens");
		} catch (Exception e){}

		try {
		this.minimalFileRowCount = writer.getConfiguration().getInt("Processing.ConllParser.conllFileMinLineCount");
		} catch (Exception e){}
		
		try {
			this.maximalFileRowCount = writer.getConfiguration().getInt("Processing.ConllParser.conllFileMaxLineCount");
			} catch (Exception e){}
		
		if (maximalFileRowCount == -1) {
			maximalFileRowCount = 100000000; // "unlimited row count" 
		} else {
		if (csvRecords.size() > maximalFileRowCount) {
			csvRecords = new ArrayList<CSVRecord>(csvRecords.subList(0, maximalFileRowCount));
		}	
		}
		
		this.rowCount = csvRecords.size();
		
		if (rowCount > 0) {
			
			// Language and model detection not reliable for small files
			if (rowCount < minimalFileRowCount) {
				throw new ConllFileTooSmallException(rowCount);
			}
			
			this.columnCount = csvRecords.get(0).size();
			if (columnCount == 1) {
				Utils.debug("ERROR in conll file : only 1 conll column ?\nProbably not in TSV format\n... skipping file !");
				throw new InvalidConllException("ERROR in conll file : only 1 conll column ?\nProbably not in TSV format\n... skipping file !");
			}
		} else {
				Utils.debug("ERROR in conll file : no records found\n ... skipping file !");
				throw new InvalidConllException("ERROR in conll file : no records found\n ... skipping file !");
		}
		
		Utils.debug("record count : "+ rowCount);
		Utils.debug("columns : "+ columnCount);
		
		wordIndexColumn = detectWordIndexColumn();
		if (wordIndexColumn == -1) {
			Utils.debug("Error : Could not retrieve word index column in CONLL");
			throw new InvalidConllException("Error : Could not retrieve word index column in CONLL");
		}
	}
	
	
	public void detectColumTypes() throws InvalidConllException {
		
		Utils.debug("Detecting CoNLL columns");
		// Choose sample sentence with modelSampleSentenceMinTokens
		ConllCSVSentence conllSampleSentence = getConllSampleSentences(1, modelSampleSentenceMinTokens).get(0);
		if (conllSampleSentence == null) {
			Utils.debug("Error - no sentences found in CONLL !");
			throw new InvalidConllException("Error - no sentences found in CONLL !");
		}
		Utils.debug("samplerecords "+conllSampleSentence.getCsvRecords().size());
		
		try {
			this.maxSampleSentenceSize = writer.getConfiguration().getInt("Processing.ConllParser.maxSampleSentenceSize");
			} catch (Exception e){}
		if (conllSampleSentence.getCsvRecords().size() > maxSampleSentenceSize) {
			Utils.debug("Shortening sample sentence to size "+maxSampleSentenceSize);
			conllSampleSentence = new ConllCSVSentence(new ArrayList<CSVRecord>(conllSampleSentence.getCsvRecords().subList(0, maxSampleSentenceSize)), this);
		}
			
			
		// Start column type detection
		//detectTreebankPostagVector(conllSampleSentence);
		
		// also process if feature columns have been marked elsewhere
		detectFeatureColumns(conllSampleSentence);								// Must be first !
		// only compute pos-tag columns here
		detectPatternColumns(conllSampleSentence);
		detectPosTagColumns(conllSampleSentence);								// Must be second !
		// only compute vector columns here
		detectTreebankPostagVector(conllSampleSentence);						// Must be before text, and after feature
		// do not compute text columns if text columns have been marked elsewhere
		if (textColumns.isEmpty()) 	  detectTextColumns(conllSampleSentence);	// Must be third !
		// only compute pattern columns here
		//detectPatternColumns(conllSampleSentence);
		
		
		if (this.textColumns.isEmpty()) {
			int s = -1;
			while (s++ < 8) {
				ConllCSVSentence sampleSentence = this.getCSVRecordsOfSentence(s);
				if (sampleSentence == null) break; // TODO better would be Iterator as result of getCSVRecordsOfSentence
				Utils.debug("Detect text columns");
				detectTextColumns(conllSampleSentence);
				Utils.debug("o.k.");
			}
		}
	}
	
	
	
	/**
	 * Find columns with tokens that are patterns that have one of the following delimiters ('+','|',',',':') e.g.
	 * VB|PRS|SFO .
	 * @param sampleSentence
	 */
	// TODO Allow more than one delimiter
	// TODO remove additional delimiters in ParserCONLL see loop -> for (int vcol : conllInfo.getPatternColumns().keySet())
	private void detectPatternColumns(ConllCSVSentence sampleSentence) {
		
		/*
		 *   DET+NOUN
		 *   PV+PVSUFF_SUBJ:3MS 	from ar\_nyuad
		 *   VB|PRS|SFO 			from sv\_talbanken, 
		 *   WW|pv|tgw|ev 			from nl\_lassysmall,nl\_alpino; 
		 *   pvg+ecs 				from ko\_kaist,ko\_gsd; cnjcoo from kmr\_mg,kk\_ktb; 
		 *   V,Act,Imprt,Sg2 		from fi\_ftb;
		 *   subst:sg:nom:m1 		from pl\_sz,pl\_lfg   
		 */
		
		
		Pattern p; 
		Matcher m; 
		String delimiters = "+|:,-.";
		
		int col = 0;
		boolean found;
		HashMap <String, Integer> patternCounts = new HashMap<String, Integer>();
		HashMap <String, Integer> splitCounts = new HashMap<String, Integer>();// count number of splitted tokens
		HashMap <String, Integer> regularPatternCounts = new HashMap<String, Integer>();// count regular patterns where number of tokens in pattern is delimiter_count+1
		while (col < this.columnCount) {
			
			if (columnIsAlreadyDetected(col)) {col++; continue;}
			
			/*if (this.featureColumns.contains(col)
				|| this.posTagColumns.contains(col)
				|| this.treebankVectorColumn == col
				|| this.textColumns.contains(col)
				|| this.wordIndexColumn == col) {col++;continue;}*/
			
			
			found = false;
			String key = "";
			for (Character d : delimiters.toCharArray()) {
				for (String token : sampleSentence.getFilteredColumnTokens(col)) {
				
					// match xa+y+zh
					//p = Pattern.compile("(([^+])+\\+)+[^+]+");
					//m = p.matcher(token);
					//if (m.matches()) {vectorColumns.put(col, "+");break;}
					
					p = Pattern.compile("(([^"+d+"])+\\"+d+")+[^"+d+"]+");
					m = p.matcher(token);
					if (m.matches()) {
						
							key = Character.toString(d)+col;
							//patternColumns.put(col, Character.toString(d));
							
							// count occurrence of pattern
							if (!patternCounts.containsKey(key)) {
								patternCounts.put(key, 1);
								splitCounts.put(key, token.split("\\"+d).length);
								if (token.split("\\"+d).length -1 == org.apache.commons.lang3.StringUtils.split(token,d).length) {
									regularPatternCounts.put(key, 1);
								} else {
									regularPatternCounts.put(key, 0);
								}
							} else {
								patternCounts.put(key, patternCounts.get(key)+1);
								splitCounts.put(key, splitCounts.get(key)+token.split("\\"+d).length);
								// check for regular pattern (ab-c-d-e and not a--bc-d)
								if (token.split("\\"+d).length -1 == org.apache.commons.lang3.StringUtils.split(token,d).length) {
									regularPatternCounts.put(key, regularPatternCounts.get(key)+1);
								}
							}
							//found=true;break;
						}
				}
				//if (found) break;
			}
			
			col++;
		}
		
		patternThreshold=3; // minimal 3 occurrences for a pattern to be recognized as pattern column
		for (String anyKey : patternCounts.keySet()) {
			
			String delimiter = anyKey.substring(0, 1);
			int column = Integer.parseInt(anyKey.substring(1, anyKey.length()));

			Utils.debug("column "+column+" has "+patternCounts.get(anyKey)+ " pattern(s) with delimiter "+delimiter);
			if (patternCounts.get(anyKey) >= patternThreshold) {
				
				// For '-' delimiter also a text column is possible
				// check if the average number of tokens in pattern > 2 ? - if not than assume a text column
				if (delimiter.equals("-")) {
					Utils.debug("splits "+splitCounts.get(anyKey));
					Utils.debug("patterns "+ patternCounts.get(anyKey));
					
					if (splitCounts.get(anyKey) * 1.0 / patternCounts.get(anyKey) <= 2.0) {
						Utils.debug("Assuming text column because delimiter is '-' and average pattern token count is 2 !");
						continue;
					}
					
					// Check for vector column, e.g. Va-r3s-n. This should not be splitted by 'y' but letterwise (later)
					// test if the number of tokens is number of delimiters-1
					if(regularPatternCounts.get(anyKey) != patternCounts.get(anyKey)) {
						Utils.debug("Assuming vector column because delimiter is '-' and irregular patterns occured !");
						continue;
					}
					
				}
				
				Utils.debug("Added pattern column "+column +" with delimiter '"+delimiter+"'");
				if (!patternColumns.containsKey(column)) {
					patternColumns.put(column, delimiter);
				} else {
					patternColumns.put(column, patternColumns.get(column)+delimiter);
				}
			} else {
				Utils.debug("failed because pattern count < pattern threshold : "+patternCounts.get(anyKey)+ "< "+patternThreshold+ " ?");
			}
		}
		
		// Add sparse pattern delimiters if a column already has a valid delimiter (e.g. a.b.c.d.e+f; . is found but + is too sparse to be valid)

		for (Map.Entry<Integer, String> entry : patternColumns.entrySet()) {
		//for (int c : patternColumns.keySet()) { // found pattern columns
			for (String anyKey : patternCounts.keySet()) {
				String delimiter = anyKey.substring(0, 1);
				int column = Integer.parseInt(anyKey.substring(1, anyKey.length()));
				if (entry.getKey() == column && !entry.getValue().contains(delimiter)) {
					entry.setValue(entry.getValue()+delimiter);
					Utils.debug("Added weak pattern delimiter "+delimiter);
				}
			}
		}
		
		
		Utils.debug("Pattern columns : ");
		for (int column : patternColumns.keySet()) {
			Utils.debug("column : "+column+" delimiter : "+patternColumns.get(column));
			Utils.debug("from : "+sampleSentence.getColumnTokens(column).toString());
		}
		
	}


	/**
	 * Detect conll column that stores word index (e.g. 1, 1-2)
	 * @return column
	 */
	private int detectWordIndexColumn() {
		int column=0;
		int line=0;
		while (column < columnCount) {
			Utils.debug("column "+column);
			// test the first 5 tokens of the first sentence for integer value in each column
			line=-1;
			while (line < 5) {
				line++;
				try {
					int wordIndex = Integer.parseInt(csvRecords.get(line).get(column));
					// return column if integer was found
					Utils.debug("Found CONLL word index column : "+column);
					return column;
				} catch (Exception e) {}
			}
			column++;
		}
		// error
		return -1;
	}


	/**
	 * Constructor does not detect column types automatically
	 */
	public ConllInfo(Iterable<CSVRecord> r) {
	
		this.csvRecords = (ArrayList<CSVRecord>) IteratorUtils.toList(r.iterator());
		this.rowCount = csvRecords.size();
		Utils.debug(rowCount);
		this.columnCount = csvRecords.get(0).size();
	}
	
	
	public int getRowCount() {
		return this.rowCount;
	}
	public int getcolumnCount() {
		return this.columnCount;
	}
	public HashSet<Integer> getTextColumns() {
		return this.textColumns;
	}
	
	public ArrayList<Integer> getPosTagColumns() {
		return posTagColumns;
	}
	
	public void setTextColumns(HashSet <Integer> textColumns) {
		this.textColumns = textColumns;
		Utils.debugNor("Text columns : ");
		for (int column : textColumns) {
			Utils.debugNor(column+" ");
		}
		if (textColumns.isEmpty())
			Utils.debugNor("none");
		;
	}
	
	public void setPosTagColumns(ArrayList<Integer> posTagColumns) {
		this.posTagColumns = posTagColumns;
		Utils.debugNor("PosTag columns : ");
		for (Integer col : this.posTagColumns) {
			Utils.debugNor(col+" ");
		}
		if (posTagColumns.isEmpty()) {
			Utils.debugNor("none");
		}
		;
	}
	
	public void setFeatureColumns(ArrayList <Integer> featureColumns) {
		this.featureColumns = featureColumns;
		Utils.debugNor("Feature columns : ");
		for (int col : featureColumns) {
			Utils.debugNor(col+" ");
		}
		if (featureColumns.isEmpty()) {
			Utils.debugNor("none");
		}
		;
	}
	
	
	public ArrayList<ConllCSVSentence> getConllSampleSentences(int count, int minSentenceTokenCount) {
		
		ArrayList<ConllCSVSentence> sentences = new ArrayList<ConllCSVSentence>();
		HashSet<Integer> usedSentences = new HashSet<Integer>();
		
		for (int i=1; i<= count; i++) {
			ConllCSVSentence tmp = getConllSampleSentence (minSentenceTokenCount, usedSentences);
			if (tmp != null) {
				// if sentence is null then no more sentences are available -> return list of sentences
				sentences.add(tmp);
			} else {
				
				int j=1;
				for (ConllCSVSentence x : sentences) {
					Utils.debug("sample sentence ("+j+"/"+sentences.size()+") : "+x.getCsvRecords().size());
				}
				
				return sentences;
			}
		}
		
		int j=1;
		for (ConllCSVSentence x : sentences) {
			Utils.debug("sample sentence ("+j+"/"+sentences.size()+") : "+x.getCsvRecords().size());
		}
		
		return sentences;
	}
	
	/**
	 * Build 'long' sentence by concatenating sentences until minTokenCound is reached
	 * @param minTokenCount Minimal length of sample sentence
	 * @param usedSentences List of sentence numbers already used 
	 * @return
	 */
	private ConllCSVSentence getConllSampleSentence(int minTokenCount, HashSet<Integer> usedSentences) {
		
		ConllCSVSentence sampleSentence = new ConllCSVSentence(new ArrayList <CSVRecord>(), this);
		
		int sentenceNo = 1;
		while (usedSentences.contains(sentenceNo)) {
			sentenceNo++;
		}
		
		// Look for long sentence
		while (sampleSentence == null || sampleSentence.getCsvRecords().size() < minTokenCount) {
			 
			 ConllCSVSentence tmp = this.getCSVRecordsOfSentence(sentenceNo);
			 if (tmp == null) return sampleSentence;
			 
			 usedSentences.add(sentenceNo++);
			 
			 try {
				 sampleSentence.getCsvRecords().addAll(tmp.getCsvRecords());
			 } catch (Exception e) {
				 // no more sentences
				 break;
			 }
		}
	
		return sampleSentence;
	}
	
	/**
	 * @deprecated (might take too long if text does not contains long sentences)
	 * @param minTokenCount
	 * @return
	 */
	private ConllCSVSentence getConllSampleSentence_old(int minTokenCount) {
		
		ConllCSVSentence sampleSentence = null;
		int sampleSentenceTokenCount = 0;
		int sentenceNo = 1;
		int maxTokenCount = 0;
		while (sampleSentenceTokenCount < minTokenCount) {
			 sampleSentence = this.getCSVRecordsOfSentence(sentenceNo++);
			 try {
				 sampleSentenceTokenCount = sampleSentence.getColumnTokens(0).size();
				 Utils.debug(sentenceNo+":"+sampleSentenceTokenCount);
			 if (sampleSentenceTokenCount > maxTokenCount) {
				 maxTokenCount = sampleSentenceTokenCount;
			 }
			 } catch (Exception e) {
				 e.printStackTrace();
				 // eof
				 sentenceNo = 1;
				 minTokenCount = maxTokenCount;
				 sampleSentenceTokenCount = 0; // reset !
				 
				 // catch no sentences found
				 if (maxTokenCount == 0) break;
			 }
		}
		return sampleSentence;
	}
	
	
	
	/**
	 * Test conll sentence : test all tokens in each column for being a postag or a feature column
	 */
	private void detectPosTagColumns(ConllCSVSentence sampleSentence) {
		
		ResourceInfo resourceInfo = new ResourceInfo("http://localhost/conllinfo-column-detection", "http://localhost/conllinfo-column-detection", "http://linghub/dummy/dataset");
		
		// I. For each column in conll csv file : add hit nodes
		ArrayList<String> tokenList = new ArrayList<String>();
		HashMap<String, Long> tagCounts = null;
		HashMap<Integer, HashMap<String, Long>> col2tagCounts = new HashMap<Integer, HashMap<String, Long>>();
		HashMap<Integer,Integer> column2tokenListSize = new HashMap<Integer,Integer>();
		
		int col = 0;
		while (col < this.columnCount) {
			
			if (columnIsAlreadyDetected(col)) {col++; continue;}
			
			/*if 		(  this.textColumns.contains(col)
					|| this.patternColumns.containsKey(col)
					|| this.featureColumns.contains(col) 
					|| this.treebankVectorColumn == col
					|| this.wordIndexColumn == col) 
			{col++; continue;}*/
		
			Utils.debug("Column : "+ col);
			tokenList = sampleSentence.getFilteredColumnTokens(col);
			column2tokenListSize.put(col, tokenList.size());
			tagCounts = ConllCSVSentence.getTokenCountMap(tokenList);
			col2tagCounts.put(col, tagCounts);

			for (String token : tagCounts.keySet()) {
				Utils.debug(token);
			}
			
			// Write tags into graph
			ModelType modelFilter = null;
			HashMap <String,HashSet<String>> tagWords = null;
			//resourceInfo = new ResourceInfo("http://localhost/conllinfo-column-detection", "http://localhost/conllinfo-column-detection", "http://linghub/dummy/dataset");
			//resourceInfo.getFileInfo().setFileId(col); // Use the csv column as Id
			
			// I. Insert tokens as HIT vertex into the graph
			writer.writeConll (col,
						  tagCounts,
						  resourceInfo,
						  modelFilter,
					      tagWords
					      );
			
			col++;
		}
			
	
	
		// II.	Detect matching tags by columns
		HashSet <Integer> posColumns = new HashSet <Integer>();
		Utils.debug("Check columns :");
			
		HashMap<Integer, HashSet <String>> col2FoundTags = new HashMap<Integer,HashSet<String>>();
		int conllCol;
		String hitTag;
		
		for (Vertex mm : writer.getQueries().getHitsForResource(resourceInfo)) {
			
			conllCol = mm.value(GWriter.HitConllColumn);
			hitTag = mm.value(GWriter.HitTag);
			
			if (!col2FoundTags.keySet().contains(conllCol)) {
				HashSet<String> foundTags = new HashSet<String>();
				foundTags.add(hitTag);
				col2FoundTags.put(conllCol, foundTags);
			} else {
				HashSet<String> foundTags = col2FoundTags.get(conllCol);
				foundTags.add(hitTag);
				col2FoundTags.put(conllCol, foundTags);
			}
			
			Utils.debug(conllCol);
			Utils.debug((String) mm.value(GWriter.HitFileId));
			Utils.debug((String) mm.value(GWriter.HitResourceUrl));
		}
		
		// Interpret a column as postTag column if at certain amount (posTagThreshold) 
		// of all tokens in that column were recognized as posTags
		// columnSum is the total number of occurrences for all identified tags : sum(count(tag(i))
		Utils.debug("PosTag columns : ");
		int tokenListSize=0;
		for (int c : col2FoundTags.keySet()) {
			
			if (column2tokenListSize.keySet().contains(c)) {
				tokenListSize = column2tokenListSize.get(c);
			} else {
				tokenListSize = 1; // nothing (do not put 0 here because x / 0 => infinity !)
			}
			Utils.debug("column "+c);
			int colSum = 0;
			for (String tag : col2FoundTags.get(c)) {
				Utils.debug("tag : "+tag);
				if (col2tagCounts.get(c).containsKey(tag)) {
					colSum+=col2tagCounts.get(c).get(tag);
					//Utils.debug("contained tag : "+tag);
				}
			}
			
			differentTokensInSample.clear();
			differentTokensInSample.addAll(sampleSentence.getColumnTokens(c));
			
			Utils.debug("Column "+c+" has sum "+colSum);
			Utils.debug("Tokens in sample : "+tokenListSize);
			Utils.debug("Different tokens in sample : "+differentTokensInSample.size());
			Utils.debug("Threshold : "+posTagThreshold);
			
			if (colSum >= posTagThreshold * column2tokenListSize.get(c)										// identified tokens / all tokens > threshold ?
				|| col2FoundTags.get(c).size() >= differentTokensInSample.size() * posTagThreshold	// identified tokens / all tokens > threshold ? 
				) {
				posColumns.add(c);
				Utils.debug("Adding posTag column : "+ c);
				Utils.debug("Column : "+ c +" was choosen because : "
						+ colSum  +" / "+tokenListSize+ " ("+((colSum *1.0 / tokenListSize* 1.0)*100)+"%) of all tokens were identified as tags !");
				Utils.debug(col2FoundTags.get(c).size() +" of "+ differentTokensInSample.size()+ " tag classes were identified !");
			} else { 
				Utils.debug("Column : "+ c +" was abandonded because : "+
						"less than "+posTagThreshold*100.0+"% of all pos tags and less than "+posTagThreshold*100.0+"% of all tag classes were identified !");
			}
		}
		
		// III.	Delete all hit nodes inserted in step I.
		writer.getQueries().deleteHitVertices(resourceInfo.getDataURL());
		
		// Set postag colums
		this.setPosTagColumns(new ArrayList<Integer>(posColumns));
	}
	
	
	
	/**
	 * Detect all columns that contain text. Detection rules are designed for sample texts with length at most 100 words.
	 */
	private void detectTextColumns(ConllCSVSentence sampleSentence) {
		
		HashSet<String> columnVocabulary = new HashSet<String>();
		Utils.debug("Detecting text columns");
				
		int col = 0;
		while (col < this.columnCount) {
			
			if (columnIsAlreadyDetected(col)) {col++; continue;}
			
			/*if (this.featureColumns.contains(col)
				|| this.posTagColumns.contains(col)
				|| this.treebankVectorColumn == col
				|| this.wordIndexColumn == col) {col++;continue;}*/
			
			// Test size of vocabulary of column
			// the vocabulary of text columns is at least 40%
			getColumnVocabulary(col, columnVocabulary);
			Utils.debug("vocabulary size for column "+col+ " : "+columnVocabulary.size());
			// if |V| >= 200 it should be a real text column
			// if |V| < 200 the decision for a text column is based on a linear function
			if (columnVocabulary.size() < (rowCount / 3.0) && columnVocabulary.size() < 140)  {
				Utils.debug("Vocabulary of column "+col+" is too small for a text column -> skipping !");
				col++;continue;
			}
			 
			//Utils.debug("Column : "+col);
			ArrayList<String> columnTokenList = sampleSentence.getColumnTokens(col);
			
			Utils.debug("Text column test "+col);
			Utils.debug(StringUtils.join(columnTokenList));
			
			int tokenNumber = 0;
			int tokenPlusPattern = 0;
			int tokenUnderscore = 0;
			int tokenCommaPattern = 0;
			int tokenColonPattern = 0;
			int digits = 0;
			//int plus = 0;
			int tokenHyphenPattern = 0;
			int tokenContainsDigit = 0;
			int tokenPipe = 0;
			
			boolean skip = false;
			for (String token : columnTokenList) {
				
				// no text column if token == "_" or pattern of the form a|b is found
				if (token.equals("_")) {skip=true;break;}
				//if (token.matches("(([^|])+\\|)+[^|]+")) {skip=true;break;}
				if (token.matches("(([^|])+\\|)+[^|]+")) {tokenPipe++;}
				
				// count underscore occurrences in word
				if (token.contains("_")) tokenUnderscore++;
				
				// count tokens that match a pattern : xy,z or xy+z or xy:z or xy|z
				if (token.matches("(([^,])+\\,)+[^,]+")) tokenCommaPattern++;
				if (token.matches("(([^+])+\\+)+[^+]+")) tokenPlusPattern++;
				if (token.matches("(([^:])+\\:)+[^:]+")) tokenColonPattern++;
				if (token.matches("(([^-])+(?<!\\s)\\-)(?!\\s)+[^-]+")) tokenHyphenPattern++; // match hyphen not not surrounded by blanks, e.g. abc-de

				
				// count numbers (not a text column if most of the tokens are numbers)
				if (token.matches("\\d+")) {tokenNumber++;}
				else {
					if (token.matches(".*(\\d)+.*")) tokenContainsDigit++;
				}
				
				
				
				// vector delimiter ,|+; (already parsed with detectPatternColumns)
				/*int containsComma = token.lastIndexOf(",");
				int containsSemicolon = token.lastIndexOf(";");
				int containsAlternative = token.lastIndexOf("|");
				int containsPlus = token.lastIndexOf("+");
				if (containsPlus > 0 && containsPlus < token.length()-1) {Utils.debug("del. plus fail");skip=true;break;}
				if (containsComma > 0 && containsComma < token.length()-1 && 
						(!StringUtils.isNumeric(token.substring(0,containsComma)) ||
						!StringUtils.isNumeric(token.substring(containsComma+1,token.length())))) {Utils.debug("del. comma fail");skip=true;break;}
				if (containsAlternative > 0 && containsAlternative < token.length()-1) {Utils.debug("del. alternative fail");skip=true;break;}
				if (containsSemicolon > 0 && containsSemicolon < token.length()-1) {Utils.debug("del. semicolon fail");skip=true;break;}*/
				
				
				for(int i=0; i<token.length(); i++){
		              //if (token.substring(i, i+1).equals("+")) {plus++;continue;}
		              //if (token.substring(i, i+1).equals(":")) {colon++;continue;}
		              if (Character.isDigit(token.charAt(i))) {digits++;break;} // count tokens that contain digits (but are not numbers - see above)
		        }

			}
			
			if (skip){
				Utils.debug("no text column because _ or | character found => skip column !");
				col++;
				continue;}
			
			// rules to skip columns
			if (tokenPlusPattern > 2) {Utils.debug("plus fail");col++;continue;}
			if (tokenColonPattern > 2) {Utils.debug("colon fail");col++;continue;}
			if (tokenCommaPattern > 2) {Utils.debug("comma fail");col++;continue;}
			if (tokenContainsDigit > 2) {Utils.debug("digit fail");col++;continue;}
			if (tokenPipe > 2) {Utils.debug("pipe fail");col++;continue;}

			// Hyphen not reliable enough
			//if (tokenHyphenPattern > 5) {Utils.debug("hyphen fail");col++;continue;}

			if ((tokenUnderscore * 1.0f) / (columnTokenList.size() * 1.0f) > 0.2f) {Utils.debug("underscore fail");col++;continue;}
			if ((tokenNumber * 1.0f) / (columnTokenList.size() * 1.0f) > 0.7f) {Utils.debug("number fail");col++;continue;}
			if ((digits * 1.0f) / (columnTokenList.size() * 1.0f) > 0.25f) {Utils.debug("digits fail");col++;continue;}

			
			String columnTokens = StringUtils.join(columnTokenList);
			
			ArrayList<String> sampleSentences = new ArrayList<String>();
			sampleSentences.add(columnTokens);
			
			ArrayList<LanguageMatch> result = OptimaizeLanguageTools1.detectAllISO639_3Languages(sampleSentences);
			//splits input into Array of single words
			//ArrayList<LanguageMatch> result = OptimaizeLanguageTools1.detectAllISO639_3Languages(columnTokens);
			for (LanguageMatch lm : result) {
				if (lm != null) {
					this.textColumns.add(col); // Add new detected text column if any language was identified
					Utils.debug(lm.getLanguageISO639Identifier());
				}
			}
			col++;
		}
		Utils.debug("Text columns : "+StringUtils.join(textColumns));
	}
	
	
	
	/**
	 * Detect CONLL feature column <p>
	 * Example : <p>
	 * Mood=Ind|Number=Plur|Person=3|Tense=Pres|VerbForm=Fin|Voice=Act
	 */
	private void detectFeatureColumns(ConllCSVSentence sampleSentence) {
		
		ArrayList<Integer> fc = new ArrayList<Integer>();
			
		// Start sampling columns
		int col = 0;
		boolean foundFeatureColumn = false;
		int assignmentOrUnion = 0;
		
		while (col < this.columnCount) {
			
			if (columnIsAlreadyDetected(col)) {col++; continue;}
			
			/*if (this.treebankVectorColumn == col
			 || this.featureColumns.contains(col)	// skip columns that already have been marked as feature column
			 || this.wordIndexColumn == col) 
			{col++; continue;}*/
						
			foundFeatureColumn = true;
			assignmentOrUnion = 0;
			for (String token : sampleSentence.getColumnTokens(col)) {
				//Utils.debug(token);
				boolean isAssignment = token.contains("=");
				boolean isBlank = token.equals("_");
				boolean isUnion = token.contains("|");
				
				if (isAssignment || isUnion) assignmentOrUnion++;
				
				if (!isAssignment && !isBlank && !isUnion) {
					foundFeatureColumn = false;
					break;
				}
				
			}
			if (foundFeatureColumn && assignmentOrUnion > 0) {
				fc.add(col);
			}
			
			col++;
		}
		
		// add the found feature columns to previously defined columns 
		this.featureColumns.addAll(fc);
		//this.setFeatureColumns(fc);
	}
	
	/**
	 * Find treebank specific posTag encodings where properties of a word are encoded as a vector
	 * (e.g. n-p---nb which stands for noun,plural,neuter,ablative)<p>
	 * @see https://github.com/cltk/latin_treebank_perseus
	 * @param sampleSentence
	 * @return column
	 */
	private void detectTreebankPostagVector(ConllCSVSentence sampleSentence) {
		
		HashSet<Integer> tokenLength = new HashSet<Integer>();
		HashSet<Boolean> tokenHasHyphen = new HashSet<Boolean>();
		// Start sampling columns
		int col = 0;
		while (col < this.columnCount) {
			
			if (columnIsAlreadyDetected(col)) {col++; continue;}
			
			/*if (this.featureColumns.contains(col)
					|| this.patternColumns.containsKey(col)
					|| this.posTagColumns.contains(col)
					|| this.wordIndexColumn == col) {col++;continue;}*/
			
			tokenLength.clear();
			tokenHasHyphen.clear();
			ArrayList<String> tokens = sampleSentence.getColumnTokens(col);
			for (String token : tokens) {
				tokenLength.add(token.length());
				tokenHasHyphen.add(token.contains("-"));
			}
			
			// Specify criteria for treebank vector column :
			// 1. All tokens in a column have the same length and length >= treebankPostagVectorMinLength
			// 2. At least one token in a column contains a '-' character
			if (tokenLength.size() == 1
				&& tokenLength.iterator().next() >= treebankPostagVectorMinLength) {
				//&& tokenHasHyphen.contains(true)) {
				this.setTreebankVectorColumn(col);
				Utils.debug("Set setTreebankVectorColumn : "+this.treebankVectorColumn);
				return;
			}
		col++;
		}
		this.setTreebankVectorColumn(-1);
	}
	
	
	/**
	 * Extract csv rows for sentence i.
	 * (Returns null if text has fewer sentences than i.)
	 * @param i
	 * @return
	 */
	public ConllCSVSentence getCSVRecordsOfSentence(int i) {
		

		if (i == 0) i = 1;
		
		//Utils.debug("i : "+i);
		
		// TODO use Iterator !
		ArrayList <CSVRecord> sentenceRecords = new ArrayList <CSVRecord>();

		int actualSentence = 1;
		int wordPos = -1;// i-th word of sentence
		
		for (CSVRecord record : csvRecords) {

			try 
			{
				Integer.parseInt(record.get(wordIndexColumn)); // skip lines which do not have line numbers (e.g. 4-5)
				//Utils.debug(Integer.parseInt(record.get(0)));
			} 
			catch (Exception e) 
			{
				continue;
			}
			
			
			// next sentence ?
			if (Integer.parseInt(record.get(wordIndexColumn)) <= wordPos) {
				if (i < actualSentence) {
					break;
				}
				// reset
				wordPos = -1;
				actualSentence++;
			} else {
				wordPos = Integer.parseInt(record.get(wordIndexColumn));
			}
			
			//Utils.debug("actualSentence "+actualSentence+","+i);
			
			// Get records of target sentence
			if (i == actualSentence) {
				sentenceRecords.add(record);
				//Utils.debug(record.get(3));
			}
			
		}
		
		if (!sentenceRecords.isEmpty()) {
			return new ConllCSVSentence(sentenceRecords, this);
		}
		else
		{
			return null;
		}
	}
	
	
	
	/**
	 * Extract all tokens in one column as sentences (columns start at 1 !)
	 * @param column
	 * @return
	 */
	public ArrayList <ArrayList<String>> getColumnTokensAsSentences(int column) {
		

		if (column == 0) column = 1;
		
		//Utils.debug("i : "+i);
		
		
		ArrayList <ArrayList<String>> tokensAsSentences = new ArrayList <ArrayList<String>>();

		int wordPos = -1;// i-th word of sentence
		ArrayList<String> sentence = new ArrayList<String>();
		
		for (CSVRecord record : csvRecords) {

			try 
			{
				Integer.parseInt(record.get(wordIndexColumn)); // skip lines which do not have line numbers (e.g. 4-5)
				//Utils.debug(Integer.parseInt(record.get(0)));
			} 
			catch (Exception e) 
			{
				continue;
			}
			
			// next sentence ?
			if (Integer.parseInt(record.get(wordIndexColumn)) <= wordPos) {
				
				tokensAsSentences.add(sentence);
				
				// reset
				sentence = new ArrayList<String>();
				wordPos = -1;
				
			} else {
				// records can have different length !
				if (record.size() >= column) {
					sentence.add(record.get(column-1));
				}
				// ADDED
				if (record.size() >= wordIndexColumn) {
					wordPos = Integer.parseInt(record.get(wordIndexColumn));
				}
			}			
		}
		
		// last sentence 
		if (sentence.size() > 0) {
			tokensAsSentences.add(sentence);
		}
		
		return tokensAsSentences;
	}
	
	


	public ArrayList<CSVRecord> getCSVRecords() {
		return csvRecords;
	}


	public ArrayList<Integer> getFeatureColumns() {
		return this.featureColumns;
	}

	
	public void addModel2Column(int column, ModelType modelType) {
		columns2Models.put(column, modelType);
	}

	public HashMap<Integer, ModelType> getColumns2Models() {
		return columns2Models;
	}


	public void setColumns2Models(HashMap<Integer, ModelType> columns2Models) {
		this.columns2Models = columns2Models;
	}


	public int getTreebankVectorColumn() {
		return treebankVectorColumn;
	}


	public void setTreebankVectorColumn(int treebankVectorColumn) {
		this.treebankVectorColumn = treebankVectorColumn;
		Utils.debugNor("Treebank vector column : ");
		if (treebankVectorColumn > 0) {
			Utils.debugNor(treebankVectorColumn);
		} else {
			Utils.debugNor("none");
		}
		;
	}
	
	


	public int getWordIndexColumn() {
		return wordIndexColumn;
	}


	public void setWordIndexColumn(int wordIndexColumn) {
		this.wordIndexColumn = wordIndexColumn;
	}
	
	/**
	 * Get the vocabulary of different tokens in one column
	 * @param column
	 * @param empty vocabulary which contains result after computation
	 */
	public void getColumnVocabulary (int column, HashSet <String> vocabulary) {
		
		// reset
		vocabulary.clear();
		for (ArrayList <String> sentence : getColumnTokensAsSentences(column+1)) {
			for (String token : sentence) {
				vocabulary.add(token);
			}
		}
	}


	public HashMap<Integer,String> getPatternColumns() {
		return patternColumns;
	}


	public void setPatternColumns(HashMap<Integer,String> vectorColumns) {
		this.patternColumns = vectorColumns;
	}
	
	public String getSample(int lines) {
		
		String sample = "";
		int i = 0;
		while (i < lines && i < rowCount) {
			sample+=convertCSVRecord2TSV(csvRecords.get(i))+"\n";
			i++;
		}	
		return sample;
	}
	
	public String convertCSVRecord2TSV(CSVRecord csvRecord) {
		
		String recordString = "";
		int i = 0;
		while (i < this.columnCount) {
			// ADDED
			if (csvRecord.size() <= i) break; // rows with different amount of columns may occur !
			recordString+=csvRecord.get(i)+"\t";
			i++;
		}
		
		if (!recordString.isEmpty()) {
			return recordString.substring(0, recordString.length()-1);
		}
		else return ""; 
	}
	
	
	/**
	 * Convert CONLL-RDF file to CSVRecord list
	 * @return
	 */
	public static ConllInfo convertConllRDF2CSVRecords(ResourceInfo resourceInfo, GWriter writer) 
			throws InvalidConllException, ConllFileTooSmallException, ConllRdfConversionException {
		
		ResultSet resultSet;
		ArrayList<CSVRecord> records = new ArrayList<CSVRecord>();
		
		/*
		 * :s476_2
		    nif:nextWord :s476_3 ;
		    conll:EDGE "discourse" ;
		    conll:HEAD :s476_1 ;
		    conll:ID "2" ;
		    conll:LEMMA "же" ;
		    conll:POS "Df" ;
		    conll:UPOS "ADV" ;
		    conll:WORD "же" ;
		    a nif:Word .
		 */
		
		try {
			
			// read rdf file into jena memory dataset
			Model model = RDFDataMgr.loadDataset(resourceInfo.getFileInfo().getAbsFilePath()).getDefaultModel();
		
			String query = ""+
						   "PREFIX conll: <http://ufal.mff.cuni.cz/conll2009-st/task-description.html#>"+
						   "PREFIX nif:   <http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#>"+
						   "PREFIX xsd:   <http://www.w3.org/2001/XMLSchema>"+
						   "SELECT ?head ?id ?word ?lemma ?feat ?upos ?pos ?edge ?misc WHERE {"+
						   "?head a nif:Word ."+
						   "FILTER NOT EXISTS {?z nif:nextWord ?head}"+
						   "?head nif:nextWord* ?y ."+
						   "?y a nif:Word ."+
						   "?y conll:ID ?id ."+
						   "OPTIONAL {?y conll:WORD ?word} ."+
						   "OPTIONAL {?y conll:LEMMA ?lemma}."+
						   "OPTIONAL {?y conll:FEAT ?feat}."+
						   "OPTIONAL {?y conll:UPOS ?upos}."+
						   "OPTIONAL {?y conll:POS ?pos}."+
						   "OPTIONAL {?y conll:EDGE ?edge}."+
						   "OPTIONAL {?y conll:MISC ?misc}."+
						   "} order by ?head ";
						   //"} order by ?head (xsd:integer(?id)) ";
			
			
			resultSet = queryModel(query,model);
			} catch (Exception e) {
				throw new ConllRdfConversionException(e.getMessage());
			}
			
			
			String head = "";
			String word = "";
			String feat = "";
			String lemma = "";
			String upos = "";
			String pos = "";
			String edge = "";
			String misc = "";
			int id = 0;
			String lastHead="";
			String csvRecord = "";
			long words = 0L;
			long lemmas = 0L;
			long features = 0L;
			
			String fileSample = "";
			int fileSampleSize = 50;
			int fileSampleCounter = 0;
		
			// stores all fields of a sentence
			HashMap<Integer, HashMap<String,String>> sentenceWords = new HashMap<Integer, HashMap<String,String>>();
			
			while (resultSet.hasNext()) {
				QuerySolution x = resultSet.next();
				head = x.get("head").toString();
				if (!x.contains("id")) continue; // skip words which do not have an ID
				try {
					id = Integer.parseInt(x.get("id").toString());
				} catch (Exception e) {
					// skip lines which do contain word range (e.g. 12-13) because they cannot be sorted !
					continue;
				}
				
				if (x.contains("word")) {word = x.get("word").toString();words++;} else {word="";}
				if (x.contains("lemma") && word.isEmpty()) {lemma = x.get("lemma").toString();lemmas++;} else {lemma="_";} // only use lemma if word is empty !
				if (x.contains("feat")) {feat = x.get("feat").toString();features++;} else {feat="_";}
				if (x.contains("upos")) upos = x.get("upos").toString(); else {upos="_";}
				if (x.contains("pos")) pos = x.get("pos").toString(); else {pos="_";}
				if (x.contains("edge")) edge = x.get("edge").toString(); else {edge="_";}
				if (x.contains("misc")) misc = x.get("misc").toString(); else {misc="_";}
				
				HashMap<String,String> values = new HashMap<String,String>();
				values.put("word", word);
				values.put("lemma", lemma);
				values.put("feat", feat);
				values.put("upos", upos);
				values.put("pos", pos);
				values.put("edge", edge);
				values.put("misc", misc);
				
				//Utils.debug(head+" "+id+" "+word);
				
				// new sentence
				if (!head.equals(lastHead)) {
					
					if (!sentenceWords.isEmpty()) {
						
						// Sort words of sentence and save to CSV record
						ArrayList<Integer> wordIDs = new ArrayList<Integer>(sentenceWords.keySet());
						Collections.sort(wordIDs);
						csvRecord="";
						for (int i : wordIDs) {
							
							// construct column
							csvRecord=i+"";
							csvRecord+="\t"+sentenceWords.get(i).get("word");
							csvRecord+="\t"+sentenceWords.get(i).get("lemma");
							csvRecord+="\t"+sentenceWords.get(i).get("feat");
							csvRecord+="\t"+sentenceWords.get(i).get("upos");
							csvRecord+="\t"+sentenceWords.get(i).get("pos");
							csvRecord+="\t"+sentenceWords.get(i).get("edge");
							csvRecord+="\t"+sentenceWords.get(i).get("misc");
							
							// write sentence to records
							try {
								records.add(CSVParser.parse(csvRecord, CSVFormat.TDF).getRecords().get(0));
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							if (fileSampleCounter++ < fileSampleSize) fileSample+=csvRecord+"\n";
						}
						sentenceWords.clear();
					}
					sentenceWords.put(id, values);
					lastHead = head;
					
				} else {
					sentenceWords.put(id, values);
				}
			}
						
			// Create ConllInfo object with records and text and feature column info
			ConllInfo conllInfo = new ConllInfo(writer, records);
			
			// set text and feature column accordingly
			HashSet<Integer> textColumns = new HashSet<Integer>();
			if (words >= lemmas) {
				textColumns.add(1);	// choose word column
			} else {
				textColumns.add(2); // choose lemma column (only if word column has fewer (no) entries than lemma column
			}
			conllInfo.setTextColumns(textColumns);
			
			ArrayList<Integer> featureColumns = new ArrayList<Integer>();
			if (features > 0) {
				featureColumns.add(3);
			}
			conllInfo.setFeatureColumns(featureColumns);
			
			resourceInfo.getFileInfo().setSample(fileSample);
			Utils.debug(fileSample);
			
			return conllInfo;
			
		/*} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;*/
	}
	
	
	private boolean columnIsAlreadyDetected(int col) {
		
		if (this.featureColumns.contains(col)
				|| this.patternColumns.containsKey(col)
				|| this.posTagColumns.contains(col)
				|| this.treebankVectorColumn == col
				|| this.textColumns.contains(col)
				|| this.wordIndexColumn == col) {return true;}
		
		return false;
	}

	
	public static ResultSet queryModel (String queryString, Model model) {
		  Query query = QueryFactory.create(queryString) ;
		  try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
		    ResultSet results = qexec.execSelect() ;
		    return ResultSetFactory.copyResults(results);
		  }
	}
	
}
