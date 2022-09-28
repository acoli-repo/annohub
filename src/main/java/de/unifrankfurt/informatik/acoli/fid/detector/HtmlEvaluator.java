package de.unifrankfurt.informatik.acoli.fid.detector;

import java.util.HashMap;
import java.util.HashSet;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;

/**
 * Experimental
 * @author frank
 *
 */
public class HtmlEvaluator {
	
	private final static HashMap <String,Double>	buzzWords = new HashMap<String,Double> (){
		private static final long serialVersionUID = 32305305L;
		{
		put("language",1.0);
		put("phonolog",1.0);
		put("lexic",1.0);
		put("synta",1.0);
		put("morpholog",1.0);
		put("annotat",1.0);
		put("gramma",1.0);
		put("linguistic",1.0);
		put("corpus",1.0);
		put("corpora",1.0);
		put("verb",1.0);
		put("noun",1.0);
		put("semantic",1.0);
		put("subject",1.0);
		put("object",1.0);
		put("library",1.0);
		put("database",1.0);
		put("dataset",1.0);
		put("resource",1.0);
		put("linked data",1.0);
		put("language resource",1000.0);
		put("semantic web",1000.0);
		put("text corp",1000.0);
		put("llod",1000.0);
		put("rdf",1000.0);
		put("conll",1000.0);
		put("sparql",1000.0);
		}
			}; 
	
	public static Double findbuzzWords(String text) {
		
		double score = 0;
		
		HashSet<String> foundWords = new HashSet<String>();
		for (String bw : buzzWords.keySet()) {
			if (text.contains(bw)) {
				Utils.debug(bw);
				foundWords.add(bw);
			}
		}

		for (String bw : foundWords) {
				score += buzzWords.get(bw);
		}
		
		return score;
	}

}
