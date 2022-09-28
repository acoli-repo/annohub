package de.unifrankfurt.informatik.acoli.fid.search;

import java.util.HashMap;

import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;

public class StreamReaderBLLMatrix implements StreamRDF {
	
	String [] searchClues;
	String subj;
	String aSubj;
	String pred;
	String obj;
	String aObj;
	String objL;
	String pKey;
	String varPrefix = "filename#";
	String fileName;
	int matrixDimension=0;
	private int [][] matrix;
	private String[][] path;

	// Graph includes class nodes
	public StreamReaderBLLMatrix (int matrixDimension) {
		this.matrixDimension = matrixDimension;
		matrix = new int [matrixDimension][matrixDimension];
		path = new String[matrixDimension][matrixDimension];
	}
	
	
	private long internalVertexCounter = 0;
	long tripleCounter = 0;
	private HashMap <String, String> anonymousVertexM = new HashMap <String, String>();
	private HashMap<String,Integer> matrixIdMap = new HashMap<String,Integer>();
	int nextFreeMatrixId=0;
	int i=0; // matrix row
	int j=0; // matrix column
	
	
	
	
	public void start() {}
	
	public void reset() {
	
		internalVertexCounter = 0;
		anonymousVertexM = new HashMap <String, String>();
		tripleCounter=0;
		
		// initialize matrix & path
		for (int i = 0;i<matrixDimension;i++) {
			for (int j = 0;j<matrixDimension;j++) {
				matrix [i][j] = 0;
				path [i][j] = "";
			}	
		}
	}
	
	
	public int getMatrixId(String key) {
		
		if (matrixIdMap.containsKey(key)) return matrixIdMap.get(key);
		else {
			matrixIdMap.put(key, nextFreeMatrixId);
			return nextFreeMatrixId++;
		}
	}
	
	
	public void triple(Triple triple) {		
		
		subj = "";
		aSubj = "";
		pred = triple.getPredicate().toString();
		pKey = pred;
		obj = "";
		aObj = "";
		objL = "";
		
		tripleCounter++;
		
		
		if (triple.getSubject().isURI()) {
			subj = triple.getSubject().getURI().toString();
			if(subj.startsWith("http://www.w3.org/2002/07/owl")) return;
		} else {
			if (triple.getSubject().isBlank()) {
				aSubj = varPrefix+triple.getSubject().getBlankNodeLabel();
				if (anonymousVertexM.containsKey(aSubj)) {
					subj = anonymousVertexM.get(aSubj);
				} else {
					subj = varPrefix+internalVertexCounter++;
					anonymousVertexM.put(aSubj, subj);
				}
			}
		}
		
		if (triple.getObject().isURI()) {
			obj = triple.getObject().getURI().toString();
			if(obj.startsWith("http://www.w3.org/2002/07/owl")) return;
		} else {
			if (triple.getObject().isBlank()) {
				aObj = varPrefix+triple.getObject().getBlankNodeLabel();
				if (anonymousVertexM.containsKey(aObj)) {
					obj = anonymousVertexM.get(aObj);
				} else {
					obj = varPrefix+internalVertexCounter++;
					anonymousVertexM.put(aObj, obj);
				}
			} else {
				if (triple.getObject().isLiteral()) {
					obj = triple.getObject().getLiteral().toString().split("@")[0]; // remove lang
				} else {return;}
			}
			
		}
		
		
		// any relation edge
		if (pred.endsWith("#subClassOf") 			|| 	
			pred.endsWith("owl#equivalentClass")	||	
			pred.endsWith("07/owl#unionOf")			||	
			pred.endsWith("07/owl#intersectionOf")	||
			pred.endsWith("22-rdf-syntax-ns#first") ||
			pred.endsWith("22-rdf-syntax-ns#rest")	||
			pred.endsWith("07/owl#complementOf")) {
		
			i = getMatrixId(subj);
			j = getMatrixId(obj);
			matrix [i][j] = 1;
			path [i][j] = i+","+j;
		}
	}

	
	public void quad(Quad quad) {
		// TODO Auto-generated method stub
	}

	
	public void base(String base) {
		// TODO Auto-generated method stub
	}

	
	public void finish() {
		
		System.out.println("Started matrix computation");
		int y = 0;
		int y1 = 0;
		// Floyd-Warshall
		for (int k = 0;k<matrixDimension;k++) {
			for (int i = 0;i<matrixDimension;i++) {
				if (matrix[i][k] == 1) {
					for (int j = 0;j<matrixDimension;j++) {
						if (matrix[k][j] == 1) {	
							matrix [i][j] = 1;
							y = path[i][k].lastIndexOf(",");
							//path [i][j] = path[i][k]+","+path[k][j];
							path [i][j] = path[i][k].substring(0, y)+","+path[k][j];
						}
					}
				}
			}
		}
		
		System.out.println("Finished matrix computation");
		System.out.println("Matrix entries : "+nextFreeMatrixId);
		System.out.println("Triples : "+tripleCounter);
	}
	
	
	public int[][] getMatrix() {
		return matrix;
	}
	
	public HashMap<String, Integer> getMatrixIdMap() {
		return matrixIdMap;
	}
	
	public String[][] getPath() {
		return path;
	}
	
	public int getDimension() {
		return matrixDimension;
	}
	

	@Override
	public void prefix(String prefix, String iri) {
		// TODO Auto-generated method stub
		
	}

}
