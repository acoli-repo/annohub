package de.unifrankfurt.informatik.acoli.fid.conll;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.csv.CSVRecord;

import edu.stanford.nlp.util.StringUtils;


public class ConllCSVSentence {
	
	private ArrayList <CSVRecord> csvRecords = new ArrayList <CSVRecord>();
	private ConllInfo conllInfo;

		
	public ConllCSVSentence(ArrayList <CSVRecord>  csvRecords, ConllInfo conllInfo) {
		this.csvRecords = csvRecords;
		this.conllInfo = conllInfo;
	}
	
	
	
	public String getSentenceText(int column) {
		
		if (!conllInfo.getTextColumns().isEmpty())
			// Use the first found text column in the conll file for text (other text columns are omitted)
			return StringUtils.join(getColumnTokens(column));
		else 
			return "";
	}
	
	
	
	/**
	 * Columns starting at one.
	 * @param column
	 * @return
	 */
	public ArrayList<String> getColumnTokens(int column) {
		
		ArrayList <String> tokens = new ArrayList <String>();
		
		for (CSVRecord record : csvRecords) {
				if (record.size() > column) {
					tokens.add(record.get(column));
				}
		}
		return tokens;
	}
	
	
	/**
	 * Retrieve all tokens from a column and skip tokens that have length 1 and not alphabetic characters
	 * @param column
	 * @return token list
	 */
	public ArrayList<String> getFilteredColumnTokens(int column) {
		
		ArrayList <String> tokens = new ArrayList <String>();
		String token = "";
		
		for (CSVRecord record : csvRecords) {
			if (record.size() > column) {
				token = record.get(column).trim();
				if ((token.length() == 1 && !StringUtils.isAlpha(token)) || StringUtils.isPunct(token)) continue;
				tokens.add(token);
			}
		}
		return tokens;
	}
	

	public static HashMap<String, Long> getTokenCountMap(ArrayList<String> tokenSequence) {
		
		HashMap<String, Long> tokenCountMap = new HashMap<String, Long>();
		
		for (String token : tokenSequence) { 
			 if (!PosTagFilter.tagHasAlpha(token)) continue;
			 if (!tokenCountMap.containsKey(token)) tokenCountMap.put(token, 1L);
			   else tokenCountMap.put(token, tokenCountMap.get(token)+1);
		}
		return tokenCountMap;
	}

	public ArrayList<CSVRecord> getCsvRecords() {
		return csvRecords;
	}


}
