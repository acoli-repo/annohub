package de.unifrankfurt.informatik.acoli.fid.owl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;

import de.unifrankfurt.informatik.acoli.fid.types.ProcessingFormat;
import de.unifrankfurt.informatik.acoli.fid.types.FileResult;
import de.unifrankfurt.informatik.acoli.fid.types.InvalidModelDefinitionException;
import de.unifrankfurt.informatik.acoli.fid.types.LanguageMatch;
import de.unifrankfurt.informatik.acoli.fid.types.ModelGroup;
import de.unifrankfurt.informatik.acoli.fid.types.ModelInfo;
import de.unifrankfurt.informatik.acoli.fid.types.ModelMatch;
import de.unifrankfurt.informatik.acoli.fid.types.ModelType;
import de.unifrankfurt.informatik.acoli.fid.types.ModelUsage;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.util.AnnotationUtil;
import de.unifrankfurt.informatik.acoli.fid.util.LocateUtils;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;
import edu.emory.mathcs.backport.java.util.Collections;

/**
 * OLIA model definitions 
 * @author frank
 *
 */
public class ModelDefinition implements Serializable {
	
	private static final long serialVersionUID = -6436165938760373725L;

	// ModelDefinitions
	private static LinkedHashMap <ModelType, ModelGroup> modelDef = new LinkedHashMap <ModelType, ModelGroup>();
	
	// Maps generated from modelDef
	private static LinkedHashMap<ModelType, String> modelType2ModelNameNice = new LinkedHashMap<ModelType, String>();
	private static LinkedHashMap<ModelType, String[]> models2ClassNamespaces = new LinkedHashMap<ModelType, String[]>();
	private static LinkedHashMap<ModelType, String[]> models2TagNamespaces = new LinkedHashMap<ModelType, String[]>();

	private LocateUtils locateUtils = new LocateUtils();
	
	private static File modelFile = null;
	private static HashSet <String> modelIDPool = new HashSet<String>();

	
	public ModelDefinition(XMLConfiguration config) throws InvalidModelDefinitionException {
		
		
		File backupRootDirectory = new File(config.getString("Backup.directory"));
		modelFile = new File(backupRootDirectory,"ModelDef.json");
		
		if (modelFile == null || !modelFile.exists()) {
			
			// copy default model definitions
			try {
				FileUtils.copyFile(locateUtils.getLocalFile("/ModelDef.json"), modelFile);
				modelFile = locateUtils.getLocalFile("/ModelDef.json");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("using model definitions in "+modelFile.getAbsolutePath());
		
		
		/*if (config.containsKey("OWL.ModelDefinitionFile")) {
			modelFile = new File(config.getString("OWL.ModelDefinitionFile"));
		}
		
		if (modelFile == null || !modelFile.exists()) {
			
			// use default model definitions
			modelFile = locateUtils.getLocalFile("/ModelDef.json");
		}*/
		
		
		if (!readModelDef(modelFile)) {
			throw new InvalidModelDefinitionException("");
		};
	}
	
	/**
	 * Reading model definitions from ModelDef.json file initializes all model definitions
	 * @param success 
	 */
	public static boolean readModelDef(File jsonFile) {
		
		// init
		modelIDPool.clear();
		modelIDPool.add("UNKNOWN");
		
		System.out.println("\n\nReading OLiA model definitions from file : "+jsonFile.getAbsolutePath());
		
		LinkedHashMap<ModelType, ModelGroup> newModelDef = new LinkedHashMap <ModelType, ModelGroup>();	
	
		int errors=0;
		int modelGroups=0;
		try {
			String jsonString = FileUtils.readFileToString(jsonFile, StandardCharsets.UTF_8);
			modelGroups = JsonPath.read(jsonString, "$.models.length()");
			int namespaces;
			int files;
			String access = "";
			
			String modelID;
			String documentationUrl;
			String niceName;
			String ns;
			String url;
			String modelUsage;
			boolean active;
			String fileDocumentationURL;
			
			int i = 0;
			int j;
			
			ModelType modelType;
			
			while (i < modelGroups) {
				
				ModelGroup mg = new ModelGroup();
				modelType = null;
				
				// reset variables
				modelID="";
				documentationUrl="";
				niceName="";
				ns= "";
				url="";
				modelUsage="";
				active=false;
				fileDocumentationURL = "";
				
				// create path for model i
				access = "$.models["+i+"]";
				
				System.out.println("*** Model "+(i+1)+" ***");
				
				/*
				 *  R E A D
				 *  
				 *  M O D E L 
				 *  
				 *  P A R A M E T E R
				 *
				 */
				try{modelID = JsonPath.read(jsonString, access+".modelID");
				System.out.println("Model ID   : "+modelID);
				modelIDPool.add(modelID);
				modelType = ModelType.valueOf(modelID);
				mg.setModelType(modelType);
				}catch(Exception e){errors++;e.printStackTrace();}
				
				try{niceName = JsonPath.read(jsonString, access+".niceName");
				System.out.println("Nice name  : "+niceName);
				mg.setNiceName(niceName);
				}catch(Exception e){errors++;e.printStackTrace();}
				
				try{documentationUrl = JsonPath.read(jsonString, access+".documentationUrl");
				System.out.println("Doc URL    : "+documentationUrl);
				mg.setDocumentationUrl(documentationUrl);
				}catch(Exception e){errors++;e.printStackTrace();}
				
				
				
				// N A M E S P A C E S
				namespaces = JsonPath.read(jsonString, access+".namespaces.length()");
				j = 0;
				Utils.debugNor("Namespaces : ");
				HashSet<String> jns = new HashSet<String>();
				while (j < namespaces) {
					
					try{ns = JsonPath.read(jsonString, access+".namespaces["+j+"]");
					Utils.debugNor(ns+" ");
					jns.add(ns);
					}catch(Exception e){errors++;e.printStackTrace();}
					
					j++;
				}
				System.out.println();
				mg.setClassNameSpaces(jns.toArray(new String[jns.size()]));
				mg.setTagNameSpaces(jns.toArray(new String[jns.size()]));
				

				
				
				// F I L E S
				files = JsonPath.read(jsonString, access+".files.length()");
				j = 0;
				System.out.println("Files :");
				ArrayList<ModelInfo> modelInfoList = new ArrayList<ModelInfo>();
				while (j < files) {
					
					try{url = JsonPath.read(jsonString, access+".files["+j+"].url");
					System.out.println((j+1)+" "+url);
					}catch(Exception e){errors++;e.printStackTrace();}
					
					try{modelUsage = JsonPath.read(jsonString, access+".files["+j+"].modelUsage");
					System.out.println("Usage   : "+modelUsage);
					}catch(Exception e){errors++;e.printStackTrace();}
					
					try{fileDocumentationURL = JsonPath.read(jsonString, access+".files["+j+"].documentationURL");
					System.out.println("Doc URL : "+fileDocumentationURL);
					}catch(Exception e){errors++;e.printStackTrace();}
					
					/*try{active = JsonPath.read(jsonString, access+".files["+j+"].active");
					System.out.println(active);
					}catch(Exception e){errors++;e.printStackTrace();}*/
					
					ModelInfo mi = new ModelInfo(url, fileDocumentationURL, modelType, ModelUsage.valueOf(modelUsage), true);
					modelInfoList.add(mi);
				
					j++;
				}
				
				mg.setModelFiles(modelInfoList);
				//mg.setModelFiles(modelInfoList.toArray(new ModelInfo[modelInfoList.size()]));
				newModelDef.put(modelType, mg);
				
				i++;
			}
			
			
			
			System.out.println("#modelGroups = "+modelGroups);
			System.out.println("Errors : "+errors);
			if (errors > 0) {
				return false;
			} else {
				
				/*
				 * I F 
				 *  
				 * N O  
				 *  
				 * E R R O R 
				 * 
				 * T H E N
				 * 
				 * U S E
				 * 
				 * N E W
				 * 
				 * M O D E L   D E F I N I T I O N S
				 *
				 */
				
				modelDef = newModelDef;
				makeMaps();

				return true;
			}
		} catch (Exception e){
			e.printStackTrace();
			System.out.println("#modelGroups = "+modelGroups);
			System.out.println("Errors : "+errors);
			return false;
		}
	}
	
	
	/**
	 * Export model definitions from modelDef variable
	 */
	public void saveModelDef() {
		
		File exportFile = new File("/home/debian7/Arbeitsfläche/ModelDef.json");
		System.out.println("saving model definitions to "+exportFile.getAbsolutePath());
		
		ObjectNode root = JsonNodeFactory.instance.objectNode();
		ArrayNode models = JsonNodeFactory.instance.arrayNode();

	 	for (ModelType mt : modelDef.keySet()) {
	 		
	 		ObjectNode model = JsonNodeFactory.instance.objectNode();
	 		model.put("modelID", mt.getId());
	 		model.put("documentationUrl", modelDef.get(mt).getDocumentationUrl());
	 		model.put("niceName", modelDef.get(mt).getNiceName());
	 		ArrayNode namespaces = JsonNodeFactory.instance.arrayNode();
	 		HashSet<String> classAndTagNamespaces = new HashSet<String>();
	 		for (String ns : modelDef.get(mt).getClassNameSpaces()) {
	 			classAndTagNamespaces.add(ns);
	 		}
	 		for (String ns : modelDef.get(mt).getTagNameSpaces()) {
	 			classAndTagNamespaces.add(ns);
	 		}
	 		for (String ns : classAndTagNamespaces) {
	 			namespaces.add(ns);
	 		}
	 		model.put("namespaces", namespaces);
	 		
	 		ArrayNode files = JsonNodeFactory.instance.arrayNode();
	 		for (ModelInfo mi : modelDef.get(mt).getModelFiles()) {
	 			ObjectNode file = JsonNodeFactory.instance.objectNode();
	 			file.put("url", mi.getUrl().toString());
	 			file.put("modelUsage", mi.getModelUsage().name());
	 			file.put("active", mi.isActive());
	 			file.put("documentationURL", mi.getDocumentationUrl().toString());
	 			files.add(file);
	 		}
	 		model.put("files", files);
	 		models.add(model);
		}
	 	
	 	root.put("models", models);
	 	ObjectMapper mapper = new ObjectMapper();
	 	try {
			String jsonString = mapper.writeValueAsString(root);
			FileWriter file = new FileWriter(exportFile);
			file.write(jsonString);
			file.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	 }

	

	public LinkedHashMap<ModelType, ModelGroup> getModelDefinitions() {
		return modelDef;
	}
	
	public LinkedHashMap<ModelType, String> getModelType2ModelNameNice() {
		return modelType2ModelNameNice;
	}
	
	
	
	private static void makeMaps() {
		
		models2ClassNamespaces.clear();
		models2TagNamespaces.clear();
		modelType2ModelNameNice.clear();
		
		for (ModelType mt : modelDef.keySet()) {
				models2ClassNamespaces.put(mt, modelDef.get(mt).getClassNameSpaces());
				models2TagNamespaces.put(mt, modelDef.get(mt).getTagNameSpaces());
				modelType2ModelNameNice.put(mt, modelDef.get(mt).getNiceName());
		}
	}

	
	public LinkedHashMap<ModelType, String[]> getModels2TagNamespaces() {
		return models2TagNamespaces;
	}

	public LinkedHashMap<ModelType, String[]> getModels2ClassNamespaces() {
		return models2ClassNamespaces;
	}

	public File getModelFile() {
		return modelFile;
	}

	
	public static List <ModelType> getModels() {
		List<ModelType> tmp = new ArrayList<ModelType>(modelDef.keySet());
		Collections.sort(tmp);
		return tmp;
	}
	
	/**
	 * Used model IDs so far.
	 * @return
	 */
	public static List <String> getModelIDs() {
		List<String> tmp = new ArrayList<String>();
		for (ModelType mt : getModels()) {
			tmp.add(mt.getId());
		}
		//Collections.sort(tmp);
		return tmp;
	}
	
	public static List <ModelType> getSpecialModels() {
		List<ModelType> tmp = getModels();
		tmp.remove(ModelType.valueOf("bll"));
		return tmp;
	}
	
	/**
	 * A sublist of all model IDs (e.g. BLL missing)
	 * @return
	 */
	public static List <String> getSpecialModelIDs() {
		
		List<String> tmp = new ArrayList<String>();
		for (ModelType mt : getSpecialModels()) {
			tmp.add(mt.getId());
		}
		//Collections.sort(tmp);
		return tmp;
	}
	
	
	public List<ModelInfo> getModelInfoList() {
		
		ArrayList<ModelInfo> modelList = new ArrayList<ModelInfo>();
		for (ModelType modelType : getModelDefinitions().keySet()) {
	    	ModelGroup mg = getModelDefinitions().get(modelType);
	    	
	    	for (ModelInfo mi : mg.getModelFiles()) {
	    	    modelList.add(mi);
	    	}
	    }
		return modelList;
	}
	
	
	public HashMap<String, String> getOliaCoreFileUrls() {
		
		ModelGroup x = modelDef.get(ModelType.valueOf("olia"));
		
		HashMap<String, String> oliaCoreFiles = new HashMap<String, String>();
		for (ModelInfo mi : x.getModelFiles()) {
			oliaCoreFiles.put(FilenameUtils.removeExtension(mi.getFileName()).toLowerCase(), mi.getUrl().toString());
		}
		
		return oliaCoreFiles;
	}
	
	
	public static void main(String[] args) {
		
		File file = new File("/home/debian7/Arbeitsfläche/ModelDef.json");
		readModelDef(file);
	}

	public static HashSet <String> getModelIDPool() {
		return modelIDPool;
	}
}
