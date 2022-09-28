package de.unifrankfurt.informatik.acoli.fid.search;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.tinkerpop.gremlin.structure.Vertex;

public class AnnotationCache {
	
	private HashSet <String> tagDefinitions = new HashSet<String>();
	private HashSet <String> classDefinitions = new HashSet<String>();
	private GWriter writer;
	
	
	public AnnotationCache(GWriter writer) {
		
		this.writer = writer;
		update();
	}
	
	
	public void update() {
		
		System.out.println("Loading tag definitions ...");
		ArrayList <Vertex> tagVertices = writer.getQueries().getTagNodes();
		String tag="";
		String tagClass="";
		for (Vertex v : tagVertices) {
			tag = "";tagClass="";
			tag = v.value(GWriter.TagTag);
			tagClass = v.value(GWriter.TagClass);
			//if (tag.contains(":")) {
			//System.out.println(tag);
			tagDefinitions.add(tag);
			classDefinitions.add(tagClass);
			//}
		}

		System.out.println("Tag definitions : "+tagDefinitions.size());
		
		System.out.println("Loading class definitions ...");
		ArrayList <Vertex> classVertices = writer.getQueries().getClassNodes();
		
		int oliaClassCount=0;
		for (Vertex v : classVertices) {
			String c = v.value(GWriter.ClassClass);
			try {
				URL url = new URL(c);
				classDefinitions.add(c);
				String x = (String) v.value(GWriter.ClassModel);
				if (x.equals("OLIA")) {
					oliaClassCount++;
					//System.out.println(c);
				}
				//System.out.println(c);
			} catch (MalformedURLException e) {}
		}

		System.out.println("Class definitions : "+classDefinitions.size());
		//System.out.println("olia class count (olia,top,system:"+oliaClassCount);
	}
	
	
	public boolean isAnnotationTag(String token) {
		
		if (tagDefinitions.contains(token) || isRegexToken(token)) return true;
		else 
		return false;
	}
	
	
	public boolean isAnnotationClass(String token) {
		
		if (classDefinitions.contains(token)) return true;
		else
		return false;
	}
	
	private boolean isRegexToken(String token) {
		
		if (token.contains("$") ||
			token.contains("*") ||
			token.contains("^") ||
			token.contains(".") ||
			token.contains("[") ||
			token.contains("|")) return true;
		else
		return false;
	}


	public HashSet<String> getTagDefinitions() {
		return tagDefinitions;
	}


	public HashSet<String> getClassDefinitions() {
		return classDefinitions;
	}

	
	public HashMap<String,Boolean> getAllowedLiteralPredicates(
			HashMap<String, HashMap<String,Long>> predicates2Objects
			) {
		return getAllowedPredicatesImpl(predicates2Objects,"literal");
	}
	
	
	public HashMap<String,Boolean> getAllowedUrlPredicates(
			HashMap<String, HashMap<String,Long>> predicates2Objects
			) {
		return getAllowedPredicatesImpl(predicates2Objects,"url");
	}
	
	
	private HashMap<String,Boolean> getAllowedPredicatesImpl(
			HashMap<String, HashMap<String,Long>> predicates2Objects,
			String objectType
			) {
		
		HashMap<String,Boolean> allowedPredicatesMap = new HashMap<String,Boolean>();
		
		switch (objectType) {
		
		case "literal":
			for (String pred : predicates2Objects.keySet()) {
				boolean found=false;
				for (String tag : predicates2Objects.get(pred).keySet()) {
					if (tagDefinitions.contains(tag)) {
						allowedPredicatesMap.put(pred, true); found=true;
						break; // next predicate
					}
				}
				if(!found) {
					allowedPredicatesMap.put(pred, false);
				}
			}
		break;
		
		case "url":
			for (String pred : predicates2Objects.keySet()) {
				boolean found=false;
				for (String tag : predicates2Objects.get(pred).keySet()) {
					if (classDefinitions.contains(tag)) {
						allowedPredicatesMap.put(pred, true); found=true;
						break; // next predicate
					}
				}
				if(!found) {
					allowedPredicatesMap.put(pred, false);
				}
			}
		break;
		
		default: break;
		
		}
		
		return allowedPredicatesMap;
	}

	
	public HashMap<String, HashSet<String>> getFoundTagsOrClassesForAttribute(
			HashMap<String, HashMap<String,Long>> attributes2Objects) {

		HashMap<String, HashSet<String>> foundTagsOrClassesForAttribute = new HashMap<String, HashSet<String>>();
		
		for (String attribute : attributes2Objects.keySet()) {
			
			boolean found = false;
			for (String tagOrClass : attributes2Objects.get(attribute).keySet()) {
				if (tagDefinitions.contains(tagOrClass) || classDefinitions.contains(tagOrClass)) {
					
					found=true;
					if(!foundTagsOrClassesForAttribute.containsKey(attribute)) {
						HashSet<String> good = new HashSet<String>();
						good.add(tagOrClass);
						foundTagsOrClassesForAttribute.put(attribute, good);
					} else {
						HashSet<String> good = foundTagsOrClassesForAttribute.get(attribute);
						good.add(tagOrClass);
						foundTagsOrClassesForAttribute.put(attribute, good);
					}
				}
			}
			if (!found) {
				foundTagsOrClassesForAttribute.put(attribute, new HashSet<String>());
			}
		}
		
		return foundTagsOrClassesForAttribute;
	}
	
	
}
