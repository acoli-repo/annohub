package de.unifrankfurt.informatik.acoli.fid.spider;

import java.util.HashMap;
import java.util.HashSet;

import de.unifrankfurt.informatik.acoli.fid.search.GWriter;
import de.unifrankfurt.informatik.acoli.fid.types.ConllConverterChoice;
import de.unifrankfurt.informatik.acoli.fid.types.ConllConversionMode;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;

public interface XmlAttributeSamplerI {
	
	// Sample 
	void sample(ResourceInfo resourceInfo, int sampleLines);
	
	// Create CoNLL from XML for selected XPATHs
	boolean makeConll(ResourceInfo resourceInfo, GWriter writer, HashSet<String> allowedAttributes, ConllConversionMode makeConllMode, ConllConverterChoice conllConverterChoice, int makeConllSampleSentenceCount, int makeConllFullMaxFileSize);
	
	
	// Getter for sample results is a map of attributes and their values with the count of occurrences
	// Example : /sentence[0]/word[0]/@xpos has values VPX with count 20, NP with count 151, etc. 

	// Only attributes with literal values
	HashMap<String, HashMap<String,Long>> getAttributes2LitObjects();

	// Only attributes with URL values
	HashMap<String, HashMap<String,Long>> getAttributes2URIObjects();
	
	// (Attributes with mixed literal/URL values to be ommitted ?)
}
