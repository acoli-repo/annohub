package de.unifrankfurt.informatik.acoli.fid.gremlinQuery;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import javax.script.Bindings;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.maven.shared.utils.io.FileUtils;
import org.apache.tinkerpop.gremlin.groovy.jsr223.GremlinGroovyScriptEngine;
import org.apache.tinkerpop.gremlin.jsr223.GremlinScriptEngine;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.unifrankfurt.informatik.acoli.fid.search.ClassMatrixParser;
import de.unifrankfurt.informatik.acoli.fid.search.GWriter;
import de.unifrankfurt.informatik.acoli.fid.types.BLLConcept;
import de.unifrankfurt.informatik.acoli.fid.types.FileResult;
import de.unifrankfurt.informatik.acoli.fid.types.ModelMatch;
import de.unifrankfurt.informatik.acoli.fid.types.ModelType;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.util.AnnotationUtil;
import de.unifrankfurt.informatik.acoli.fid.util.JenaUtils;
import de.unifrankfurt.informatik.acoli.fid.util.ScriptUtils;
import edu.emory.mathcs.backport.java.util.Collections;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;

public class BllTools {
	
	
	private HashMap<String,HashSet<String>> tag2Bll = new HashMap<String,HashSet<String>>();
	private HashMap<String,HashSet<String>> class2Bll = new HashMap<String,HashSet<String>>();
	private HashMap<String,String> hit2Tag = new HashMap<String,String>();
	private HashMap<String,String> hit2Class = new HashMap<String,String>();
	private Graph graph;
	private String graphN = "g";
	private GraphTraversalSource g;
	private GremlinScriptEngine gremlinScriptEngine = new GremlinGroovyScriptEngine();
	
	private HashMap<String, String> bllLanguageLinkMap = new HashMap<String, String>();
	private ClassMatrixParser bllMatrixParser;
	private HashMap<String, HashSet<String>> bllMap;
	private XMLConfiguration fidConfig;
	String bllLinkFileName;
	private String bllSVNcheckoutDirectory="";

	
	public BllTools (Graph graph, XMLConfiguration fidConfig) {
		
		this.graph = graph;
		this.fidConfig = fidConfig;
		this.bllLinkFileName = getBllLinkFileName();
		
		if (fidConfig.getBoolean("RunParameter.useBllOntologiesFromSVN")) {
			this.bllSVNcheckoutDirectory = new File(fidConfig.getString("RunParameter.BLLOntologiesDirectory"),"bll").getAbsolutePath();
		}
	}
	
	
	public void updateBLLFilesFromSVN() {
		
		if (!fidConfig.getBoolean("RunParameter.useBllOntologiesFromSVN") ||
				bllSVNcheckoutDirectory.trim().isEmpty()
			) return;
		
		Utils.debug("updateBLLFilesFromSVN");
		
		try {
			
			FileUtils.deleteDirectory(bllSVNcheckoutDirectory);
			Utils.debug("creating directory : "+bllSVNcheckoutDirectory);
			FileUtils.mkdir(fidConfig.getString(bllSVNcheckoutDirectory));
		
			// SVN files to be load into local BLL ontology directory
			HashSet<String> svnFiles = new HashSet<String>();
			svnFiles.add(fidConfig.getString("OWL.BLL.BllOntology"));
			svnFiles.add(fidConfig.getString("OWL.BLL.BllLink"));
			svnFiles.add(fidConfig.getString("OWL.BLL.BllLanguageLink"));
		
			// Execute svn export
			ScriptUtils.exportSVNFiles(svnFiles, bllSVNcheckoutDirectory);
		
		} catch (Exception e) {e.printStackTrace();}
	}
	
	

	
	
	public HashMap<String, HashSet<String>> makeBllMap() {
		
		if (bllMap != null) return bllMap;
		
		// init
		bllMatrixParser = new ClassMatrixParser(getBllOntologieFilePath());
		bllLanguageLinkMap = JenaUtils.loadBllLanguageLinkMap(getBllLanguageLinkFilePath());
		
		HashMap<String,HashSet<String>> bllMap = new HashMap<String,HashSet<String>>();
		HashMap<String,HashSet<String>> resultMap = new HashMap<String,HashSet<String>>();
		HashSet<String> missingBllLinking = new HashSet<String>();

		// generate partial hash maps
		makeTag2BllMap();
		makeClass2BllMap();
		makeHit2TagMap();
		makeHit2ClassMap();
		
		Utils.debug("tag2Bll :"+tag2Bll.size());
		Utils.debug("class2Bll :"+class2Bll.size());
		Utils.debug("hit2Tag :"+hit2Tag.size());
		Utils.debug("hit2Class :"+hit2Class.size());
		
		
		// generate final hash map from partial maps
		for (String h : hit2Tag.keySet()) {
			if (!tag2Bll.containsKey(hit2Tag.get(h))) {
				missingBllLinking.add("tag2Bll does not contain :"+hit2Tag.get(h));
			} else {
				bllMap.put(h, tag2Bll.get(hit2Tag.get(h)));
			}
		}
		for (String h : hit2Class.keySet()) {
			if (!class2Bll.containsKey(hit2Class.get(h))) {
				missingBllLinking.add("class2Bll does not contain :"+hit2Class.get(h));
			} else {
				bllMap.put(h, class2Bll.get(hit2Class.get(h)));
			}
		}
		
		
		// (optimization) find least general concept of matched BLL concepts
		Utils.debug("Find least general BLL concepts for "+bllMap.size()+" hits !");
		int counter = 1;
		int all = bllMap.size();
		HashSet<String> temp;
		for (String xx : bllMap.keySet()) {
			// find least general concept of matched BLL concepts
			temp = bllMatrixParser.getLowestClassesInClassHierarchy(bllMap.get(xx));
			resultMap.put(xx, temp);
			/*Utils.debug(bllMap.get(xx)+" :");
			for (String x : temp) {
				Utils.debugNor(x);
			}*/
			Utils.debug((counter++)+"/"+all);
		}
		
		
		
		/*Utils.debug("BLL map :"+resultMap.size());
		for (String yy : resultMap.keySet()) {
			Utils.debugNor(yy+ ": ");
			for (String yyy : resultMap.get(yy)) {
				Utils.debugNor(yyy+" ");
			}
			Utils.debug();
		}*/
		
		Utils.debug("Errors :");
		ArrayList<String> errors = new ArrayList<String>(missingBllLinking);
		Collections.sort(errors);
		for (String e : errors) {
			Utils.debug(e);
		}
		
		
		this.bllMap = resultMap;
		return resultMap;
	}



	private void makeTag2BllMap () {
		
		Utils.debug("makeTag2BllMap");
		
		g = graph.traversal();
		
		ArrayList <LinkedHashMap <String,Vertex>> results = new ArrayList<LinkedHashMap <String,Vertex>>();
		Bindings bindings = gremlinScriptEngine.createBindings();
		bindings.put(graphN, g);
		bindings.put("results", results);
		
		
		String query =
		graphN+".V().hasLabel('"+GWriter.ClassVertex+"').has('"+GWriter.ClassFile+"','"+bllLinkFileName+"').as('x').in()."
		+ "optional(filter{it.get().value('"+GWriter.ClassClass+"').matches('#.*')}."
		+ "emit().repeat(inE('COLL','UNION','INTER','EQUIV','COMPL')."
		+ "outV().dedup())).has('"+GWriter.Model+"',within("
		+ "'"+ModelType.valueOf("OLIA").name()+"'))."
		//+ "emit().repeat(inE().outV().simplePath())."
		+ "emit().repeat(inE('A','SUB','COLL','UNION','INTER','EQUIV','COMPL').outV().simplePath())." // disallow complement edges !
		+ "hasLabel('"+GWriter.TagVertex+"').as('y').select('x','y').dedup()";
		
		
		//Utils.debug("-+-"+query);
		
		
		try {
			gremlinScriptEngine.eval(query+".fill(results)",bindings);
			
			String bllUrl="";
			String tag = "";
			String model="";
			String key="";
			for (LinkedHashMap <String,Vertex> e : results) {
				
				
				bllUrl = ((Vertex) e.get("x")).value(GWriter.Class);
				if (bllUrl.startsWith("#")) continue; // quickfix TODO #bll-link#11 should be filtered query 
				tag = ((Vertex) e.get("y")).value(GWriter.TagTag);
				model = ((Vertex) e.get("y")).value(GWriter.TagModel);
				
				key = tag+"@"+model;
				
				//Utils.debug(":::key "+key);
				
				if (!tag2Bll.containsKey(key)) {
					HashSet<String> bllConcepts = new HashSet<String>();
					bllConcepts.add(bllUrl);
					tag2Bll.put(key, bllConcepts);
				} else {
					HashSet<String> bllConcepts = tag2Bll.get(key);
					bllConcepts.add(bllUrl);
					tag2Bll.put(key, bllConcepts);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}


	private void makeClass2BllMap() {
		
		Utils.debug("makeClass2BllMap");
		
		g = graph.traversal();
		
		ArrayList <LinkedHashMap <String,Vertex>> results = new ArrayList<LinkedHashMap <String,Vertex>>();
		Bindings bindings = gremlinScriptEngine.createBindings();
		bindings.put(graphN, g);
		bindings.put("results", results);
		
		String query =
		graphN+".V().hasLabel('"+GWriter.ClassVertex+"').has('"+GWriter.ClassFile+"','"+bllLinkFileName+"').as('x').in()."
		+ "optional(filter{it.get().value('"+GWriter.ClassClass+"').matches('#.*')}."
		+ "emit().repeat(inE('COLL','UNION','INTER','EQUIV','COMPL')."
		+ "outV().dedup())).has('"+GWriter.Model+"',within("
		+ "'"+ModelType.valueOf("OLIA").name()+"'))."
		//+ "emit().repeat(inE().outV().simplePath())."
		+ "emit().repeat(inE('A','SUB','COLL','UNION','INTER','EQUIV','COMPL').outV().simplePath())."  // disallow complement edges !
		+ "hasLabel('"+GWriter.ClassVertex+"').as('y').select('x','y').dedup()";
		
		Utils.debug(query);
		
		
		try {
			gremlinScriptEngine.eval(query+".fill(results)",bindings);
			String bllUrl="";
			String class_ = "";
			Utils.debug(results.size());
			for (LinkedHashMap <String,Vertex> e : results) {
				
				bllUrl = ((Vertex) e.get("x")).value(GWriter.Class);
				if (bllUrl.startsWith("#")) continue; // quickfix TODO #bll-link#11 should be filtered query 
				class_ = ((Vertex) e.get("y")).value(GWriter.ClassClass);
				
				if (!class2Bll.containsKey(class_)) {
					HashSet<String> bllConcepts = new HashSet<String>();
					bllConcepts.add(bllUrl);
					class2Bll.put(class_, bllConcepts);
				} else {
					HashSet<String> bllConcepts = class2Bll.get(class_);
					bllConcepts.add(bllUrl);
					class2Bll.put(class_, bllConcepts);
				}
			}
			
			/*Utils.debug("class2Bll :"+class2Bll.size());
			for (String yy : class2Bll.keySet()) {
				Utils.debugNor(yy+":");
				for (String yyy : class2Bll.get(yy)) {
					Utils.debugNor(yyy+" ");
				}
				Utils.debug();
			}*/
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private void makeHit2TagMap() {
		
		Utils.debug("makeHit2TagMap");
		
		g = graph.traversal();
		
		ArrayList <LinkedHashMap <String,Vertex>> results = new ArrayList<LinkedHashMap <String,Vertex>>();
		Bindings bindings = gremlinScriptEngine.createBindings();
		bindings.put(graphN, g);
		bindings.put("results", results);
		
		String query =
		graphN+".V().hasLabel('"+GWriter.HitVertex+"').as('y').out()."
				+ "hasLabel('"+GWriter.TagVertex+"').as('x').select('x','y').dedup()";
		
		Utils.debug(query);
		
		String key="";
		String hitResourceUrl="";
		String hitFileId="";
		String foundTagOrClass="";
		String model;
		int column;
		String tag = "";
		
		try {
			gremlinScriptEngine.eval(query+".fill(results)",bindings);
			
			for (LinkedHashMap <String,Vertex> e : results) {
				
				hitResourceUrl = ((Vertex) e.get("y")).value(GWriter.HitResourceUrl);
				hitFileId = ((Vertex) e.get("y")).value(GWriter.HitFileId);
				tag = ((Vertex) e.get("x")).value(GWriter.TagTag);
				model = ((Vertex) e.get("x")).value(GWriter.TagModel);
				
				// reset
				foundTagOrClass="";
				column=-1;
	
				switch ((String) ((Vertex) e.get("y")).value(GWriter.HitType)) {
				
				case GWriter.HitTypeTag :
					foundTagOrClass = e.get("y").value("tag");
					column = ((Vertex) e.get("y")).value(GWriter.HitConllColumn);
					break;
					
				case GWriter.HitTypeFeature :
					foundTagOrClass = e.get("y").value(GWriter.HitFeature)+"="+e.get("y").value(GWriter.HitFeatureValue);
					column = ((Vertex) e.get("y")).value(GWriter.HitConllColumn);
					//Utils.debug("feature key :"+hitResourceUrl+"@"+hitFileId+"@"+column+"@"+foundTagOrClass+"@"+model);
					break;
					
				case GWriter.HitTypeLiteralObject :
				case GWriter.HitTypeURIObject :
					foundTagOrClass = e.get("y").value(GWriter.HitObject);
					column = -1;
					break;
				
				// Defined but not used !
				case GWriter.HitTypeXML :
					break;
					
				default :
					Utils.debug("Error queryBll : HitType "+
						((Vertex) e.get("y")).value(GWriter.HitType)+" is not recognized !");
					break;
				}
				
				
				key = hitResourceUrl+"@"+hitFileId+"@"+column+"@"+foundTagOrClass+"@"+model;
				//Utils.debug("key :"+key+" "+tag+"@"+model);
				hit2Tag.put(key, tag+"@"+model);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private void makeHit2ClassMap() {
		
		Utils.debug("makeHit2ClassMap");
		
		g = graph.traversal();
		
		ArrayList <LinkedHashMap <String,Vertex>> results = new ArrayList<LinkedHashMap <String,Vertex>>();
		Bindings bindings = gremlinScriptEngine.createBindings();
		bindings.put(graphN, g);
		bindings.put("results", results);
		
		String query =
		graphN+".V().hasLabel('"+GWriter.HitVertex+"').as('y').out()."
				+ "hasLabel('"+GWriter.ClassVertex+"').as('x').select('x','y').dedup()";
		
		Utils.debug(query);
		
		String key="";
		String hitResourceUrl="";
		String hitFileId="";
		String foundTagOrClass="";
		String model;
		int column;
		String class_ = "";
		
		try {
			gremlinScriptEngine.eval(query+".fill(results)",bindings);
			
			for (LinkedHashMap <String,Vertex> e : results) {
				
				hitResourceUrl = ((Vertex) e.get("y")).value(GWriter.HitResourceUrl);
				hitFileId = ((Vertex) e.get("y")).value(GWriter.HitFileId);
				class_ = ((Vertex) e.get("x")).value(GWriter.ClassClass);
				model = ((Vertex) e.get("x")).value(GWriter.ClassModel);
				
				// reset
				foundTagOrClass="";
				column=-1;
	
				switch ((String) ((Vertex) e.get("y")).value(GWriter.HitType)) {
				
				case GWriter.HitTypeTag :
					foundTagOrClass = e.get("y").value("tag");
					column = ((Vertex) e.get("y")).value(GWriter.HitConllColumn);
					break;
					
				case GWriter.HitTypeFeature :
					foundTagOrClass = e.get("y").value(GWriter.HitFeature)+"="+e.get("y").value(GWriter.HitFeatureValue);
					column = ((Vertex) e.get("y")).value(GWriter.HitConllColumn);
					//Utils.debug("feature key :"+hitResourceUrl+"@"+hitFileId+"@"+column+"@"+foundTagOrClass+"@"+model);
					break;
					
				case GWriter.HitTypeLiteralObject :
				case GWriter.HitTypeURIObject :
					foundTagOrClass = e.get("y").value(GWriter.HitObject);
					column = -1;
					break;
				
				// Defined but not used !
				case GWriter.HitTypeXML :
					break;
					
				default :
					Utils.debug("Error queryBll : HitType "+
						((Vertex) e.get("y")).value(GWriter.HitType)+" is not recognized !");
					break;
				}
				
				
				key = hitResourceUrl+"@"+hitFileId+"@"+column+"@"+foundTagOrClass+"@"+model;
				//Utils.debug("key :"+key+ " "+class_);
				hit2Class.put(key, class_);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	public String getBllKey(ResourceInfo rs, FileResult r, ModelMatch mm) {
		
		String bllKey="";
		if (r.getFeatureName().isEmpty()) {
				bllKey=rs.getDataURL()+"@"+rs.getFileInfo().getRelFilePath()+"@"+mm.getConllColumn()+"@"+r.getFoundTagOrClass()+"@"+mm.getModelType();
			}
			else {
				bllKey=rs.getDataURL()+"@"+rs.getFileInfo().getRelFilePath()+"@"+mm.getConllColumn()+"@"+r.getFeatureName()+"="+r.getFoundTagOrClass()+"@"+mm.getModelType();
			}
		return bllKey;
	}
	
	
	public void extendFileResults (List<ResourceInfo> resources) {
		
		Utils.debug("extendFileResults");
		
		String bllKey="";
		int counter = 1;
		int all = resources.size();
		for (ResourceInfo rs : resources) {
			
			Utils.debug((counter++)+"/"+all);
			
			for (ModelMatch mm : rs.getFileInfo().getModelMatchings()) {	
				if (mm.getModelType().equals(ModelType.valueOf("UNKNOWN"))) continue;
				
				if (! rs.getFileInfo().getFileResults().containsKey(mm)) continue; // catch bug
				for (FileResult r : rs.getFileInfo().getFileResults().get(mm)) {
					if (r.getFoundTagOrClass().equals(AnnotationUtil.unmatchedAnnotations)) continue;
					
					bllKey = getBllKey(rs, r, mm);
					if (bllMap != null && bllMap.get(bllKey) != null) {
						ArrayList<BLLConcept> bllConcepts = new ArrayList<BLLConcept>();
						for (String yy : bllMap.get(bllKey)) {
 							bllConcepts.add(new BLLConcept(yy, "")); // TODO add label
 						}
 						r.setBllConcepts(bllConcepts);
					}
				}	
			}
		}
	}
	
	
	
	public ArrayList<String> getBllFileResult(ResourceInfo resourceInfo, ModelMatch modelMatch, FileResult fileResult) {
		
		ArrayList<String> result = new ArrayList<String>();
		
		if (fileResult.getFoundTagOrClass().equals(AnnotationUtil.unmatchedAnnotations)) return result;
		
		String bllKey = getBllKey(resourceInfo, fileResult, modelMatch);
		if (bllMap != null && bllMap.get(bllKey) != null) {
			for (String yy : bllMap.get(bllKey)) {
				result.add(yy);
				}
		}
		return result;
	}
	


	public HashMap<String, String> getBllLanguageLinkMap() {
		return bllLanguageLinkMap;
	}



	public ClassMatrixParser getBllMatrixParser() {
		return bllMatrixParser;
	}


	public HashMap<String, HashSet<String>> getBllMap() {
		return bllMap;
	}
	
	public String getBllOntologieFilePath(){
		
		try {
			URL url = new URL(fidConfig.getString("OWL.BLL.BllOntology"));
			String fileName = new File(url.getFile()).getName();
			
			if (!fidConfig.getBoolean("RunParameter.useBllOntologiesFromSVN")) {
				// use default files
				return "/owl/bll/"+fileName;
			} else {
				return new File(bllSVNcheckoutDirectory,fileName).getAbsolutePath();
			}			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public String getBllLinkFilePath(){
		
		try {
			URL url = new URL(fidConfig.getString("OWL.BLL.BllLink"));
			String fileName = new File(url.getFile()).getName();
			
			if (!fidConfig.getBoolean("RunParameter.useBllOntologiesFromSVN")) {
				// use default files
				return "/owl/bll/"+fileName;
			} else {
				return new File(bllSVNcheckoutDirectory,fileName).getAbsolutePath();
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public String getBllLinkFileName(){
		
		try {
			URL url;
			url = new URL(fidConfig.getString("OWL.BLL.BllLink"));
			String bllLinkFileName = FileUtils.removeExtension(new File(url.getFile()).getName());
			return bllLinkFileName; 
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	public String getBllLanguageLinkFilePath(){
		
		try {
			URL url = new URL(fidConfig.getString("OWL.BLL.BllLanguageLink"));
			String fileName = new File(url.getFile()).getName();
			
			if (!fidConfig.getBoolean("RunParameter.useBllOntologiesFromSVN")) {
				// use default files
				return "/owl/bll/"+fileName;
			} else {
				return new File(bllSVNcheckoutDirectory,fileName).getAbsolutePath();
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return "";
	}


	public HashMap<String, HashSet<String>> getTag2Bll() {
		return tag2Bll;
	}


	public HashMap<String, HashSet<String>> getClass2Bll() {
		return class2Bll;
	}
		
}
