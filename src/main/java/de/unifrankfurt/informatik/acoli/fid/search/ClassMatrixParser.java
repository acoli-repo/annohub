package de.unifrankfurt.informatik.acoli.fid.search;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.jena.riot.RDFDataMgr;



public class ClassMatrixParser {
	
	
	StreamReaderBLLMatrix matrixParser;
	int[][] matrix;
	String[][] path;
	HashMap<String, Integer> idMap;
	HashSet <String> upperClasses = new HashSet <String>();
	HashSet <String> lowerClasses = new HashSet <String>();
	private final int AllUpperLowerClasses = 0;
	private final int LongestPath = 1;
	
	/**
	 * Compute class hierarchy
	 */
	public ClassMatrixParser (String rdfFile) {
		
		System.out.println("Reading from "+rdfFile);
		
		matrixParser = new StreamReaderBLLMatrix(6000);
		matrixParser.reset();
		RDFDataMgr.parse(matrixParser, rdfFile);
		matrix = matrixParser.getMatrix();
		path = matrixParser.getPath();
		idMap = matrixParser.getMatrixIdMap();
		
		// print all classes
		/*for (String u : idMap.keySet()) {
			System.out.println(u);
		}*/
	}
	
	
	/**
	 * Retrieve those classes that are not upper classes of any other class of the input class set.
	 * Example : A-sub->B,A-sub->C,D will output A,D
	 * @param classes
	 * @return 
	 */
	public HashSet<String> getLowestClassesInClassHierarchy (HashSet <String> classes) {
		
		HashSet<String> result = new HashSet<String>();
		result.addAll(classes);
		HashSet <String> upperX;
		
		for (String x : classes) {
				upperX = this.getAllUpperClasses(x);
				for (String y : classes) {
					if(y.equals(x)) continue;
					if (upperX.contains(y)) result.remove(y);
				}
		}
		
		return result;
	}
	
	
	private Object getLongestUpperImpl (String class_, int function) {
		
		HashMap<Integer,String> longestPath = new HashMap<Integer,String>();
		upperClasses.clear();
		
		//System.out.println("Check matrix entry : "+class_);
		
		if (idMap.containsKey(class_)) {
			int z = idMap.get(class_);
			String maxPath = "";
			int maxPathLength = 0;
			String s = "";
			String s_ = "";
			String idString = "";
			//System.out.println("Forward");
			for (int column=0; column < matrixParser.getDimension();column++) {
				if (matrix[z][column] == 1) {
					
					//System.out.println(column+":"+getIdFromMap(idMap, column));
					s="";
					s_="";
					for (String a : path[z][column].split(",")) {
						idString = getIdFromMap(idMap, Integer.parseInt(a.trim()));
						upperClasses.add(idString);
						s_ = idString.split("#")[1];
						if (!s_.matches("\\d+")) s+=">"+idString;
					}
					//System.out.println(s);
					if (path[z][column].split(",").length > maxPathLength) {
						maxPathLength = path[z][column].split(",").length;
						maxPath = s;
					}
				}	
			}
			int index = 0;
			for (String p : maxPath.split(">")) {
				if (p.trim().isEmpty()) continue;
				longestPath.put(index++, p.trim());
			}
		}	
		
		switch (function) {
		
		case AllUpperLowerClasses :
			upperClasses.remove(class_);
			return upperClasses;
			
		case LongestPath :
			return longestPath;
	
		default :
			return null;
	}
	}
	
	
	private Object getLongestLowerImpl(String class_, int function) {
		
		HashMap<Integer,String> longestPath = new HashMap<Integer,String>();
		lowerClasses.clear();
		
		//System.out.println("Check matrix entry : "+class_);
		
		if (idMap.containsKey(class_)) {
			int z = idMap.get(class_);
			String maxPath = "";
			int maxPathLength = 0;
			String s = "";
			String s_ = "";
			String idString = "";
			//System.out.println("Backward");
			for (int row=0; row < matrixParser.getDimension();row++) {
				if (matrix[row][z] == 1) {
					
					//System.out.println(row+":"+getIdFromMap(idMap, row));
					s="";
					s_="";
					for (String a : path[row][z].split(",")) {
						idString = getIdFromMap(idMap, Integer.parseInt(a.trim()));
						lowerClasses.add(idString);
						s_ = idString.split("#")[1];
						if (!s_.matches("\\d+")) {
							s+=">"+idString;
						}
					}
					//System.out.println(row+":"+s);
					if (path[row][z].split(",").length > maxPathLength) {
						maxPathLength = path[row][z].split(",").length;
						maxPath = s;
					}
				}	
			}
			
			int index = 0;
			for (String p : maxPath.split(">")) {
				if (p.trim().isEmpty()) continue;
				longestPath.put(index++, p.trim());
			}
		}
		
		
		
		switch (function) {
		
			case AllUpperLowerClasses :
				lowerClasses.remove(class_);
				return lowerClasses;
				
			case LongestPath :
				// fix index order to 0,-1,-2, .. starting with 0 from searched class
				HashMap<Integer,String> longestPath_ = new HashMap<Integer,String>();
				for (int k : longestPath.keySet()) {
					longestPath_.put(k-(longestPath.size()-1), longestPath.get(k));
				}
				return longestPath_;
		
			default :
				return null;
		}
	}
	
	
	public HashMap<Integer,String> getLongestUpperPath (String class_){
		return (HashMap<Integer,String>) getLongestUpperImpl (class_, LongestPath);
	}
	
	public HashMap<Integer,String> getLongestLowerPath (String class_){
		return (HashMap<Integer,String>) getLongestLowerImpl (class_, LongestPath);
	}
	
	public HashSet <String> getAllUpperClasses (String class_){
		return (HashSet <String>) getLongestUpperImpl (class_, AllUpperLowerClasses);
	}
	
	public HashSet <String> getAllLowerClasses (String class_){
		return (HashSet <String>) getLongestLowerImpl (class_, AllUpperLowerClasses);
	}
	
	public boolean isLowerClass(String lower, String upper) {
		return getAllUpperClasses(lower).contains(upper);
	}
	
	public boolean isUpperClass(String upper, String lower) {
		return getAllLowerClasses(upper).contains(lower);
	}
	
	public String getIdFromMap(HashMap<String, Integer> idMap, int value) {
		for (String name : idMap.keySet()) {
			if (idMap.get(name) == value) {
				return name;
			}
		}
		return "";
	}
	
	public Set<String> getAllClasses () {
		return idMap.keySet();
	}

}
