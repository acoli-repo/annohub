package de.unifrankfurt.informatik.acoli.fid.xml;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Template {

	private String id;
	public String getId() {
		return this.id;
	}
	private final transient static Logger LOGGER =
			Logger.getLogger(Template.class.getName());

	public String description;
	@Deprecated
	public String chunkPath;
	public String sentencePath;
	public String wordPath;
	public LinkedHashMap<String, String> columnPaths;
	public LinkedHashMap<String, String> featurePaths;

	// only existent after compiling.
	public transient XPathExpression wordXPath;
	public transient LinkedHashMap<String, XPathExpression> columnXPaths;
	public transient LinkedHashMap<String, XPathExpression> featureXPaths;

	/**
	 * empty constructor for gson serialization
	 */
	Template(){
	}

	/**
	 * Constructor with only required fields.
	 * @param sentencePath
	 * @param wordPath
	 * @param columnPaths
	 */
	Template(String sentencePath, String wordPath, Map<String, String> columnPaths){
		this.description = "MISSING";
		this.sentencePath = sentencePath;
		this.wordPath = wordPath;
		this.columnPaths = new LinkedHashMap<>(columnPaths);

	}
	/**
	 * Proper constructor with all fields.
	 * @param description
	 * @param chunkPath
	 * @param sentencePath
	 * @param wordPath
	 * @param columnPaths
	 * @param featurePaths
	 */
	public Template(String description, String chunkPath, String sentencePath, String wordPath,
			 Map<String, String> columnPaths, Map<String, String> featurePaths) {
		this.description = description;
		this.chunkPath = chunkPath;
		this.sentencePath = sentencePath;
		this.wordPath = wordPath;
		this.columnPaths = new LinkedHashMap<>(columnPaths);
		if (featurePaths != null) {
			this.featurePaths = new LinkedHashMap<>(featurePaths);
		}
	}


	/**
	 * throws together all values from the extraction template and returns
	 * them.
	 * @return
	 */
	HashSet<String> getAllPaths(){
    	HashSet<String> paths = new HashSet<>(this.columnPaths.values());
    	if (this.featurePaths != null) {
			paths.addAll(this.featurePaths.values());
		}
    	return paths;
	}

	public void setColumnXPaths(LinkedHashMap<String, XPathExpression> columnXPaths) {
		this.columnXPaths = columnXPaths;
	}
	public void setColumnPaths(LinkedHashMap<String, String> columnPaths) {
		this.columnPaths = columnPaths;
	}

	public void setColumnPaths(Set<String> columns) {
		LinkedHashMap<String, String> columnPaths = new LinkedHashMap<>();
		for (String column : columns) {
			columnPaths.put(column, column);
		}
		this.columnPaths = columnPaths;
	}

	public String getSentencePath(){
		return this.sentencePath;
	}
	
	@Override
	public String toString() {
		return "XMLTemplate #"+this.id+"\n\tDescription: '"+this.description+"'\n\t"
				+"sentencePath: "+this.sentencePath+"\n\t"
				+"wordPath: "+this.wordPath+"\n\t"
				+"columnPaths:\n\t\t"+this.columnPaths.entrySet()
												.stream()
												.map(e -> e.getKey()+": "+e.getValue())
												.collect(Collectors.joining("\n\t\t"));
	}

	/**
	 * tests, if a template is more than threshold t similar to another template.
	 * returns falls otherwise
	 * @param other another Template object to compare to
	 * @param t the float threshold between 0 and 1
	 * @return
	 */
	public boolean isSimilar(Template other, float t) {
		// TODO: ask if it's good practice to use asserts here
		assert t <= 1;
		assert t >= 0;
		return howSimilar(other) >= t;
	}

	public float howSimilar(Template other) {
		int total = 2; // we always compare sentence and word.
		int matches = 0;
		if (this.sentencePath.equals(other.sentencePath)) matches++;
		if (this.wordPath.equals(other.wordPath)) matches++;

		if (this.chunkPath == null ^ other.chunkPath == null) total++;
		if (this.chunkPath != null && other.chunkPath != null) {
			total++;
			if (this.chunkPath.equals(other.chunkPath)) matches++;
		}

		total = total + this.columnPaths.values().size();
		for (String path : this.columnPaths.values()) {
			if (other.columnPaths.values().contains(path)) matches++;
		}
		total = total + other.columnPaths.values().size();
		for (String path : other.columnPaths.values()) {
			if (this.columnPaths.values().contains(path)) matches++;
		}
		// If we don't find feature paths in either of the templates but had them
		// in the other, we penalize them.
		if (this.featurePaths != null && other.featurePaths == null) {
			total = total + this.featurePaths.values().size();
		}
		if (this.featurePaths == null && other.featurePaths != null) {
			total = total + other.featurePaths.values().size();
		}
		// only if both have feature paths we creating their similarity
		if (this.featurePaths != null && other.featurePaths != null) {
			total = total + this.featurePaths.values().size();
			for (String path : this.featurePaths.values()) {
				if (other.featurePaths.values().contains(path)) matches++;
			}
			total = total + other.featurePaths.values().size();
			for (String path : other.featurePaths.values()) {
				if (this.featurePaths.values().contains(path)) matches++;
			}
		}
		LOGGER.info("Template(#"+this.id+") compared to Template(#"+other.id+"): "+matches+"/"+total);

		return (float) matches / (float) total;
	}

	@Override
	public boolean equals(Object obj) {
		try {
			final Template other = (Template) obj;
			if (this.id.equals(other.id)) {
				return true;
			}
			else {
				return false;
			}
		}	
		catch (ClassCastException e) {
			return false;
		}
	}

	/**
	 * Iterates over all string paths and compiles them into proper XPaths
	 */
	public void compile() {
			XPath xPath = XPathFactory.newInstance().newXPath();
			if (this.wordPath != null) {
				try {
					this.wordXPath = xPath.compile(this.wordPath);
				}
				catch(XPathExpressionException ex){
						LOGGER.warning("Couldn't compile wordXPath {" + this.wordPath +"}");
				}
			}
			this.columnXPaths = new LinkedHashMap<>();
			if (this.columnPaths != null) {
				for (Map.Entry<String, String> e : this.columnPaths.entrySet()) {
					if (e.getKey().equals("FEATS")){
						this.columnXPaths.put(e.getKey(), null);
					} else {
						try {
							this.columnXPaths.put(e.getKey(), xPath.compile(e.getValue()));
						} catch (XPathExpressionException ex) {
							LOGGER.warning("Couldn't compile columnXPath {" + e.getKey() + ": " + e.getValue() + "}");
						}
					}
			}
			this.featureXPaths = new LinkedHashMap<>();
			if (this.featurePaths != null){
				for (Map.Entry<String, String> e : this.featurePaths.entrySet()) {
					try {
						this.featureXPaths.put(e.getKey(), xPath.compile(e.getValue()));
					}
					catch (XPathExpressionException ex) {
							LOGGER.warning("Couldn't compile featureXPath {"+e.getKey()+": "+e.getValue()+"}");
					}
				}
			}

		}
	}
}
