package de.unifrankfurt.informatik.acoli.fid.types;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.codehaus.plexus.util.StringUtils;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import de.unifrankfurt.informatik.acoli.fid.conll.ConllCSVSentence;
import de.unifrankfurt.informatik.acoli.fid.exec.Executer;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;

/**
 * Container for file attributes during processing
 * @author frank
 *
 */
public class FileInfo implements Serializable {
	
	private static final long serialVersionUID = -167210312635L;
	
	private ArrayList <ModelMatch> modelMatchings = new ArrayList <ModelMatch>();
	private ArrayList <LanguageMatch> languageMatchings = new ArrayList <LanguageMatch>();
	private String languageMatchingsAsString = "---";
	private String modelMatchingsAsString = "";
	
	private File resourceFile = null;
	private String relFilePath = ""; // relative path e.g. file-archive/subfolder/z.txt
	private String absFilePath = "";
	private String fileName = "";
	/**
	 * Internal file representation used for processing. Can differ from original format. For example
	 * xml files are always converted to conll, and rdf files that contain conll data are converted to conll 
	 * format before processing. 
	 */
	private ProcessingFormat fileFormat = ProcessingFormat.UNKNOWN;
	private String languageSample = "";
	private String sample="";
	
	private String statusCode	= "";
	private Long tripleCount	= 0L;
	private Long fileSizeInBytes= 0L;
	private String errorCode	= "";
	private String errorMsg		= "";
	
	private ArrayList<String> languageMatchingURLs = new ArrayList <String>();
	private static TreeNode languageMatchingsAsTree = new DefaultTreeNode("Languages", null);
	private TreeNode modelMatchingsAsTree = new DefaultTreeNode("Models", null);
	private TreeNode modelMatchingsAsTreeForColumn = new DefaultTreeNode("Models", null);
	private TreeNode selectedModelMatchingsAsTree = null;//new DefaultTreeNode("Models", null);

	private HashMap <ModelMatch,Set <String>> tagMatchingsForModel = new HashMap <ModelMatch,Set <String>>();
	private HashMap <ModelMatch, ArrayList <FileResult>> fileResults = new HashMap <ModelMatch, ArrayList <FileResult>>();
	
	private ProcessState processState = ProcessState.UNPROCESSED;
	private String comment	= "";
	
	private Date processingStartDate = new Date();
	private Date processingEndDate = new Date();
	private Date acceptedDate = null;
	
	private ArrayList<VocabularyMatch> vocabularyMatchings = new ArrayList<VocabularyMatch>();
	private String vocabularyMatchingsAsString="";

	private HashMap<Integer, HashMap<String, Long>> columnTokens = new HashMap<Integer, HashMap<String, Long>>();
	private HashMap <Integer, String> conllcolumn2XMLAttr = new HashMap <Integer, String>();

	private Vertex fileVertex = null;
	private String temporaryFilePath = ""; // use this for converted files
	
	private boolean forceRescan = false; // flag overrides updatePolicy

	private transient ArrayList<ConllCSVSentence> sampleSentences;
	
	
	
	/**
	 * Replace found models (will delete manual selection)
	 * @param modelMatchings
	 */
	public void setModelMatchings(ArrayList <ModelMatch> modelMatchings) {
		
		this.modelMatchings = modelMatchings;
		updateModelMatchingsAsString();
	}
	
	/**
	 * Method used by web-interface to display empty model list
	 */
	public void setModelMatchingsUnknown() {
		// overwrite previously added models
		this.modelMatchings.clear();
		ModelMatch modelMatch = new ModelMatch(ModelType.valueOf("UNKNOWN"));
		modelMatch.setDate(new Date(0L));	// !!!
		modelMatch.setUpdateText("");		// !!!
		this.modelMatchings.add(modelMatch);
		updateModelMatchingsAsString();
	}
	
	public ArrayList <ModelMatch> getModelMatchings() {
		return modelMatchings;
	}
	
	public ArrayList<LanguageMatch> getLanguageMatchings() {
		return languageMatchings;
	}
	public void setLanguageMatchings(ArrayList<LanguageMatch> languageMatchings) {
		this.languageMatchings = languageMatchings;
		Utils.debug("setLanguageMatchings ");
		/*for (LanguageMatch lm : languageMatchings) {
			Utils.debug(lm.getConllColumn());
			Utils.debug(lm.getLexvoUrl());
			Utils.debug(lm.isSelected());
		}*/
		updateLanguageMatchingsAsString();
	}
	public File getResourceFile() {
		return resourceFile;
	}
	
	/**
	 * Default constructor will set the working file and the relative path of the file
	 * which is later used as the file ID ! <b>
	 * Example : <b>
	 * File : /home/user/download/folder/archive/folder.file.txt<b>
	 * relPath : archive/folder.file.txt (will serve together with the resource-URL as file ID)<b> 
	 * @param resourceFile
	 * @param relPath
	 */
	public void setResourceFile(File resourceFile, Path relPath) {
		this.resourceFile = resourceFile;
		this.absFilePath = resourceFile.getAbsolutePath();
		this.relFilePath = relPath.toString();
		this.fileName = resourceFile.getName();
		this.statusCode=""; // reset status information for new file !
	}
	
	/** Legacy constructor will set the working file and its relative path to the files name.
	 * @deprecated
	 */
	public void setResourceFile(File resourceFile) {
		this.resourceFile = resourceFile;
		this.absFilePath = resourceFile.getAbsolutePath();
		this.relFilePath = resourceFile.getName(); // relative path is filename !
		this.fileName = resourceFile.getName();
		this.statusCode=""; // reset status information for new file !		
	}
	
	public String getFileName() {
		return fileName;
	}
	
	/**
	 * Internal file representation used for processing. Can differ from original format. For example
	 * xml files are always converted to conll, and rdf files that contain conll data are converted to conll 
	 * format before processing. The original file type can be obtained from IndexUtls.dertermineFileFormat.
	 * @return internally used representation of the data
	 */
	public ProcessingFormat getProcessingFormat() {
		return fileFormat;
	}
	
	/**
	 * Internal file representation used for processing. Can differ from original format. For example
	 * xml files are always converted to conll, and rdf files that contain conll data are converted to conll 
	 * format before processing.  
	 */
	public void setProcessingFormat(ProcessingFormat fileFormat) {
		this.fileFormat = fileFormat;
	}
	
	// relative filePath used to identify a file in a resource !
	public String getFileId() {
		return this.relFilePath;
	}
	
	
	public String getLanguageSample() {
		return languageSample;
	}
	
	public void setLanguageSample(String languageSample) {
		this.languageSample = languageSample;
	}
	
	/**
	 * Deprecated, use getParseResult instead
	 * @return
	 * @deprecated
	 */
	public String getStatusCode() {
		return statusCode;
	}

	/**
	 * Deprecated, use getParseResult instead
	 * @param statusCode
	 * @deprecated
	 */
	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	public Long getTripleCount() {
		return tripleCount;
	}

	public void setTripleCount(Long tripleCount) {
		this.tripleCount = tripleCount;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}


	
	public void setLanguageMatchings(HashMap<String, Long> languageMap) {
		
		this.languageMatchings.clear(); // important because same resourceInfo object is used for all files of resource
		
		for (String lang : languageMap.keySet()) {
			try {
				this.languageMatchings.add(new LanguageMatch(lang, languageMap.get(lang), DetectionMethod.AUTO));
			} catch (InvalidLanguageException e) {}
		}
		
		updateLanguageMatchingsAsString();
	}
	
	
	public void updateLanguageMatchingsAsString() {
		
		String result = "";
		HashMap <String,ArrayList<Integer>> isos = new HashMap<String,ArrayList <Integer>>();
		String iso;
		for (LanguageMatch lm : getSelectedLanguages()) {
			
			if (!lm.isSelected()) continue;
			if (lm.getLexvoUrl() == null) continue;
			
			iso = lm.getLanguageISO639Identifier();
			if (!isos.containsKey(iso)) {
				ArrayList <Integer> columns = new ArrayList <Integer>();
				columns.add(lm.getConllColumn());
				isos.put(iso,columns);
			} else {
				ArrayList <Integer> columns = isos.get(iso);
				columns.add(lm.getConllColumn());
				isos.put(iso,columns);
			}
		}
		
		for (String iso_ : isos.keySet()) {
			String urlWithColumn = iso_;
			String cols = "";
			ArrayList<Integer> cols_ = isos.get(iso_);
			Collections.sort(cols_);
			for (Integer c : cols_) {
				if (c > 0) {
					cols += c+",";
				}
			}
			if (!cols.isEmpty()) cols = " ("+cols.substring(0, cols.length()-1)+")";
			result+= urlWithColumn+cols+",";
		}
		
		if (!result.isEmpty()) {
			result = result.substring(0, result.length()-1);
		} else {
			result = "---";
		}
		
		
		this.languageMatchingsAsString = result;
	}
	
	
	public String getLanguageMatchingsAsString() {
		return this.languageMatchingsAsString;
	}
	
	
	/**
	 * If too many languages return only the first 100 chars of the resulting String
	 * @return Short version of found languages
	 */
	public String getLanguageMatchingsAsStringShort() {
		if (this.languageMatchingsAsString.length() > 100) {
			return StringUtils.substring(this.languageMatchingsAsString, 0, 100)+" ...";
		} else {
			return this.languageMatchingsAsString;
		}

	}
	
	
	
	/**
	 * Getter for browser
	 * @return
	 */
	public String getModelMatchingsAsString() {
		return this.modelMatchingsAsString;
	}

	
	
	public void updateModelMatchingsAsString() {
		
		String result = "";
		HashMap <String,ArrayList<Integer>> models = new HashMap<String,ArrayList <Integer>>();
		String model;
		for (ModelMatch mm : getSelectedModels()) {
			
			model = mm.getModelType().name();
			if (!models.containsKey(model)) {
				ArrayList <Integer> columns = new ArrayList <Integer>();
				columns.add(mm.getConllColumn());
				models.put(model,columns);
			} else {
				ArrayList <Integer> columns = models.get(model);
				columns.add(mm.getConllColumn());
				models.put(model,columns);
			}
		}
		
		for (String model_ : models.keySet()) {
			String modelWithColumn = model_;
			String cols = "";
			ArrayList<Integer> cols_ = models.get(model_);
			Collections.sort(cols_);
			for (Integer c : cols_) {
				if (c > 0) {
					cols += c+",";
				}
			}
			if (!cols.isEmpty()) cols = " ("+cols.substring(0, cols.length()-1)+")";
			result+= modelWithColumn+cols+",";
		}
		
		if (!result.isEmpty()) {
			result = result.substring(0, result.length()-1);
		} else {
			result = "";
		}
		
		if (result.length() > 100) result = StringUtils.substring(result, 0, 100)+" ...";
		
		this.modelMatchingsAsString = result;
	}
	


	/**
	 * @return a string representation of the matching languages with lexvo urls
	 */
	public ArrayList<String> getLanguageMatchingURLs() {
		
		this.languageMatchingURLs.clear();
		HashMap <String,ArrayList<Integer>> urls = new HashMap<String,ArrayList <Integer>>();
		String url;
		for (LanguageMatch lm : getSelectedLanguages()) {
			
			//if (!lm.isSelected()) continue;
			
			if (lm.getLexvoUrl() == null) continue;
			url = lm.getLexvoUrl().toString();
			if (!urls.containsKey(url)) {
				ArrayList <Integer> columns = new ArrayList <Integer>();
				columns.add(lm.getConllColumn());
				urls.put(url,columns);
			} else {
				ArrayList <Integer> columns = urls.get(url);
				columns.add(lm.getConllColumn());
				urls.put(url,columns);
			}
		}
		
		for (String url_ : urls.keySet()) {
			String urlWithColumn = url_;
			String cols = "";
			ArrayList<Integer> cols_ = urls.get(url_);
			Collections.sort(cols_);
			for (Integer c : cols_) {
				if (c > 0) {
					cols += c+",";
				}
			}
			if (!cols.isEmpty()) cols = " ("+cols.substring(0, cols.length()-1)+")";
			languageMatchingURLs.add(urlWithColumn+cols);
		}
		
		return this.languageMatchingURLs;
	}
	

	// Object representations for web-interface
	
	public TreeNode getLanguageMatchingsAsTree() {
		 languageMatchingsAsTree = new DefaultTreeNode("Languages", null);
	     
	     for (String lmurl: getLanguageMatchingURLs()) {
	        	new DefaultTreeNode(lmurl, languageMatchingsAsTree);
	     }
	    
		return languageMatchingsAsTree;
	}
	
	public TreeNode getAllModelMatchingsAsTree() {
		
		 Utils.debug("getAllModelMatchingsAsTree "+modelMatchingsAsTree.getChildCount());
		 
		 // root
		 modelMatchingsAsTree = new DefaultTreeNode("Models", null);
		 TreeNode x;
		 
   	 if (fileResults != null && !fileResults.isEmpty()) {
   		 
   		 ArrayList<ModelMatch> mms;
   		 
   		 mms = new ArrayList<ModelMatch>(); 
   		 mms.addAll(fileResults.keySet());
   		
   		 // First sort modelMatchings by conllColumn
   		 //Collections.sort(mms, Comparator.comparing(ModelMatch::getConllColumn));
   		 HashMap <Integer,ArrayList <ModelMatch>> modelsByColumn = new HashMap <Integer,ArrayList <ModelMatch>> ();
   		 int column = 0;
   		 for (ModelMatch mm_ : getModelMatchings()) {
   			 column = mm_.getConllColumn();
   			 if (!modelsByColumn.keySet().contains(column)) {
   				 ArrayList<ModelMatch> y = new ArrayList<ModelMatch>();
   				 y.add(mm_);
				modelsByColumn.put(column,y);
   			 } else {
   				ArrayList<ModelMatch> y = modelsByColumn.get(column);
   				y.add(mm_);
   				modelsByColumn.put(column,y);
   			 } 
   		 }
   		 
   		ArrayList <Integer> columns = new ArrayList <Integer>();
   		columns.addAll(modelsByColumn.keySet());
   		Collections.sort(columns);
   		
   		DecimalFormat df = new DecimalFormat("0.00");
   		String provenance = "";
   		
   		// Build tree
   		for (Integer col : columns) {
   			
   			FileResult xyz = new FileResult();
   			xyz.setFoundTagOrClass(col.toString()); // column label node
   			TreeNode cx = new DefaultTreeNode(xyz, modelMatchingsAsTree);
   			
   			// Sort models by coverage
   			ArrayList <ModelMatch> mmc_ = modelsByColumn.get(col);
      		Collections.sort(mmc_, Comparator.comparing(ModelMatch::getCoverage).reversed());

   			for (ModelMatch mmc : mmc_) {
   				
   				if (mmc.isSelected()) {
   					provenance = "(selection)";
   				} else {
   					provenance = "";
   				}
   				
   				FileResult xy = new FileResult();
      	    	xy.setFoundTagOrClass(mmc.getModelType().name()+" "+df.format(mmc.getCoverage()*100)+"% "+provenance); // model label node
   	   			TreeNode mx = new DefaultTreeNode(xy, cx);
   	   			
   	   			
   	   			// Check if results for model exist (necessary if models were manual edited) 
   	   			if (fileResults.containsKey(mmc)) {
   	   			
	   	   		 /*Utils.debug("File : "+fileName);
	   			 Utils.debug(mmc.getModelType().name());
	   			 Utils.debug(fileResults.get(mmc).size());*/
   	   			
   	   			
   	   			for (FileResult fileResult : fileResults.get(mmc)) {
  				 
  				 //Utils.debug(fileName+ " added :");
  				/* Utils.debug(fileResult.getFoundTagOrClass());
  				 Utils.debug(fileResult.getMatchingTagOrClass());*/
  				 //Utils.debug(fileResult.matchCount);
  				 
  				 new DefaultTreeNode(fileResult, mx);
  			 };
   	    	 }
   				
   			}
   		}
   	 
   	 } else {
   		 FileResult xyz = new FileResult();
	    	 xyz.setFoundTagOrClass(ModelType.valueOf("UNKNOWN").name());
	    	 x = new DefaultTreeNode(xyz, modelMatchingsAsTree);	
   	 }
	    	 
	    	 
   	 /*
   	 if (tagMatchingsForModel != null && !tagMatchingsForModel.isEmpty()) {
   		 
   		 for (ModelMatch key : tagMatchingsForModel.keySet()) {
   			 Utils.debug(key.getModelType().name());
   			 ArrayList <String> temp = new ArrayList <String>(tagMatchingsForModel.get(key));
   			 Collections.sort(temp);
   			 for (String tagUrl : temp) {
   				 new DefaultTreeNode(tagUrl, x);
   			 };
   		 };
   	 }
   	 */
	     
	     
	     if (modelMatchingsAsTree.getChildCount() == 0) {
	    	 new DefaultTreeNode("UNKNOWN", modelMatchingsAsTree);
	     }
		return modelMatchingsAsTree;
	}
	
	
	
	public TreeNode getAllModelMatchingsAsTreeForColumn(Long j) {
		
		 Utils.debug("getAllModelMatchingsAsTreeForColumn : "+j);
		 // root
		 modelMatchingsAsTreeForColumn = new DefaultTreeNode("Models", null);
		 TreeNode x;
		try { 
  	 if (fileResults != null && !fileResults.isEmpty()) {
  		 
  		 ArrayList<ModelMatch> mms;
  		 
  		 mms = new ArrayList<ModelMatch>(); 
  		 mms.addAll(fileResults.keySet());
  		
  		 // First sort modelMatchings by conllColumn
  		 //Collections.sort(mms, Comparator.comparing(ModelMatch::getConllColumn));
  		 HashMap <Integer,ArrayList <ModelMatch>> modelsByColumn = new HashMap <Integer,ArrayList <ModelMatch>> ();
  		 int column = 0;
  		 for (ModelMatch mm_ : getModelMatchings()) {
  			 column = mm_.getConllColumn();
  			 
  			 // Filter column
  			 if (column != j.intValue()) continue;
  			 
  			 if (!modelsByColumn.keySet().contains(column)) {
  				 ArrayList<ModelMatch> y = new ArrayList<ModelMatch>();
  				 y.add(mm_);
				modelsByColumn.put(column,y);
  			 } else {
  				ArrayList<ModelMatch> y = modelsByColumn.get(column);
  				y.add(mm_);
  				modelsByColumn.put(column,y);
  			 } 
  		 }
  		 
  		ArrayList <Integer> columns = new ArrayList <Integer>();
  		columns.addAll(modelsByColumn.keySet());
  		Collections.sort(columns);
  		
  		DecimalFormat df = new DecimalFormat("0.00");
  		String provenance = "";
  		
  		// Build tree
  		for (Integer col : columns) {
  			
  			/*
  			FileResult xyz = new FileResult();
  			xyz.setFoundTagOrClass(col.toString()); // column label node
  			TreeNode cx = new DefaultTreeNode(xyz, modelMatchingsAsTree);
  			*/
  			
  			// Sort models by coverage
  			ArrayList <ModelMatch> mmc_ = modelsByColumn.get(col);
     		Collections.sort(mmc_, Comparator.comparing(ModelMatch::getCoverage).reversed());

  			for (ModelMatch mmc : mmc_) {
  				
  				String provenceData = 
  	   					mmc.getDifferentHitTypes()+","+
  	   					mmc.getHitCountTotal()+","+
  	   					mmc.getExclusiveHitTypes()+","+
  						mmc.getExclusiveHitCountTotal();
  	   				
  	   			if (mmc.isSelected()) {
  	   					provenance = provenceData+" (selection) ";
  	   			} else {
  	   					provenance = provenceData;
  	   			}
  				
  				FileResult xy = new FileResult();
     	    	xy.setFoundTagOrClass(mmc.getModelType().name()+" "+df.format(mmc.getCoverage()*100)+"% "+provenance); // model label node
  	   			TreeNode mx = new DefaultTreeNode(xy, modelMatchingsAsTreeForColumn);
  	   			
  	   			// Check if results for model exist (necessary if models were manual edited) 
  	   			if (fileResults.containsKey(mmc)) {
  	   			
	   	   		 /*Utils.debug("File : "+fileName);
	   			 Utils.debug(mmc.getModelType().name());
	   			 Utils.debug(fileResults.get(mmc).size());*/
  	   			
  	   			
  	   			for (FileResult fileResult : fileResults.get(mmc)) {
 				 
 				 //Utils.debug(fileName+ " added :");
 				/* Utils.debug(fileResult.getFoundTagOrClass());
 				 Utils.debug(fileResult.getMatchingTagOrClass());*/
 				 //Utils.debug(fileResult.matchCount);
 				 
 				 new DefaultTreeNode(fileResult, mx);
 			 };
 			 }
  				
  			}
  		}
  	 
  	 } else {
  		 FileResult xyz = new FileResult();
	    	 xyz.setFoundTagOrClass(ModelType.valueOf("UNKNOWN").name());
	    	 x = new DefaultTreeNode(xyz, modelMatchingsAsTreeForColumn);	
  	 }
	    	 
	    	 
  	 /*
  	 if (tagMatchingsForModel != null && !tagMatchingsForModel.isEmpty()) {
  		 
  		 for (ModelMatch key : tagMatchingsForModel.keySet()) {
  			 Utils.debug(key.getModelType().name());
  			 ArrayList <String> temp = new ArrayList <String>(tagMatchingsForModel.get(key));
  			 Collections.sort(temp);
  			 for (String tagUrl : temp) {
  				 new DefaultTreeNode(tagUrl, x);
  			 };
  		 };
  	 }
  	 */
	     
	     
	     if (modelMatchingsAsTreeForColumn.getChildCount() == 0) {
	    	 new DefaultTreeNode("UNKNOWN", modelMatchingsAsTreeForColumn);
	     }
		} catch (Exception e) {e.printStackTrace();}
		return modelMatchingsAsTreeForColumn;
	}
	
	
	
	public TreeNode getSelectedModelMatchingsAsTree() {
		
		// removed output Utils.debug("getSelectedModelMatchingsAsTree ");
		
		 if (selectedModelMatchingsAsTree == null || selectedModelMatchingsAsTree.getChildCount() > 0) {
			 return updateSelectedModelMatchingsAsTree();
		 }
		 else {
			 return selectedModelMatchingsAsTree;
		 }
	}
	
	
	public TreeNode updateSelectedModelMatchingsAsTree() {
		
		 
		 /*Utils.debug("getSelectedModelMatchingsAsTree "+selectedModelMatchingsAsTree.getChildCount());
		 if (!updateSelectedModelMatchingsAsTree
			 && selectedModelMatchingsAsTree.getChildCount() > 0) return selectedModelMatchingsAsTree;
		 */
		
		 // root
		 selectedModelMatchingsAsTree = new DefaultTreeNode("Models", null);
		 TreeNode x;
		 
    	 if (fileResults != null && !fileResults.isEmpty()) {
    		 
    		ArrayList<ModelMatch> mms;
    		mms = getSelectedModels();
    		 
    		 // First sort modelMatchings by conllColumn
    		 Collections.sort(mms, Comparator.comparing(ModelMatch::getConllColumn));
    		 for (ModelMatch mm : mms) {
  	 
    			 // add child node for model
    	    	 FileResult xyz = new FileResult();
    	    	 String conllColumnInfo = "";
    	    	 if (isConllFile()) {
    	    	 //if (mm.getConllColumn() > 0) {
    	    		 conllColumnInfo = " ("+mm.getConllColumn()+")";
    	    		 //conllColumnInfo = " ("+mm.getConllColumn()+" "+mm.getCoverage()*100+"%)";
    	    	 }
    	    	 
    	    	 // TODO this.isXMLFile() does not work since XML files get type CONLL
    	    	 if(!mm.getXmlAttribute().isEmpty()) {
    	    		 conllColumnInfo = " ("+mm.getXmlAttribute()+")"; 
    	    	 } else {
    	    		 
    	    		 if (this.isRDFFile()) {
        	    		 conllColumnInfo = " ("+mm.getRdfProperty()+")"; 
        	    	 }
    	    	 }
    	    	 
    	    	 
    	    	 
    	    	 if (this.isXMLFile()) {
    	    		 conllColumnInfo = " ("+mm.getXmlAttribute()+")"; 
    	    	 }
    	    	 
    	    	 xyz.setFoundTagOrClass(mm.getModelType().name()+conllColumnInfo);
    	    	 x = new DefaultTreeNode(xyz, selectedModelMatchingsAsTree);
    	    	 
    	    	 
    	    	 // Check if results for model exist (necessary if models were manual edited) 
    	    	 if (fileResults.containsKey(mm)) {
    	    	 
    			 /*Utils.debug("File : "+fileName);
    			 Utils.debug(mm.getModelType().name());
    			 Utils.debug(fileResults.get(mm).size());*/
    			 
    			 
    			 
    			 for (FileResult fileResult : fileResults.get(mm)) {
    				 
    				 //Utils.debug(fileName+ " added :");
    				/* Utils.debug(fileResult.getFoundTagOrClass());
    				 Utils.debug(fileResult.getMatchingTagOrClass());*/
    				 //Utils.debug(fileResult.matchCount);
    				 
    				 new DefaultTreeNode(fileResult, x);
    			 };
    		 	}
    		 }
    	 
    	 } else {
    		 FileResult xyz = new FileResult();
	    	 xyz.setFoundTagOrClass(ModelType.valueOf("UNKNOWN").name());
	    	 x = new DefaultTreeNode(xyz, selectedModelMatchingsAsTree);	
    	 }
	    	 
	    	 
    	 /*
    	 if (tagMatchingsForModel != null && !tagMatchingsForModel.isEmpty()) {
    		 
    		 for (ModelMatch key : tagMatchingsForModel.keySet()) {
    			 Utils.debug(key.getModelType().name());
    			 ArrayList <String> temp = new ArrayList <String>(tagMatchingsForModel.get(key));
    			 Collections.sort(temp);
    			 for (String tagUrl : temp) {
    				 new DefaultTreeNode(tagUrl, x);
    			 };
    		 };
    	 }
    	 */
	     
	     
	     if (selectedModelMatchingsAsTree.getChildCount() == 0) {
	    	 new DefaultTreeNode("UNKNOWN", selectedModelMatchingsAsTree);
	     }
		return selectedModelMatchingsAsTree;
	}

	public HashMap <ModelMatch,Set <String>> getTagMatchingsForModel() {
		return tagMatchingsForModel;
	}


	public HashMap <ModelMatch, ArrayList <FileResult>> getFileResults() {
		return this.fileResults;
	}

	public void setFileResults(HashMap <ModelMatch, ArrayList <FileResult>> fileResults) {
		this.fileResults = fileResults;
	}
	
	
	
	/**
	 * Get the model selection. Initially that models are 'selected' automatically that are found (RDF) or fit best (CONLL) to the data.
	 * Later this selection can be altered manually by the user through the Web-Interface.
	 * @return model view
	 */
	public ArrayList<ModelMatch> getSelectedModels() {
		return getSelectedModels(this.modelMatchings);
	}
	
	/**
	 * Get the model selection. Initially that models are 'selected' automatically that are found (RDF) or fit best (CONLL) to the data.
	 * Later this selection can be altered manually by the user through the Web-Interface.
	 * @param modelMatchings
	 * @return model view
	 */
	public ArrayList<ModelMatch> getSelectedModels(ArrayList<ModelMatch> modelMatchings) {
		
		ArrayList<ModelMatch> selectedColumnModels = new ArrayList<ModelMatch>();
		for (ModelMatch mm : modelMatchings) {
			if (mm.isSelected()) {
				selectedColumnModels.add(mm);
			}
		}
		return selectedColumnModels;
	}
	
	
	
	public ModelMatch getSelectedModelForColumn(Long column) {

		try {
			//Utils.debug("getSelectedModelForColumn "+column);
		for (ModelMatch mm : getSelectedModels()) {
			if (mm.getConllColumn() == column.intValue()) {
				//Utils.debug("Model is "+mm.getModelType().name());
				return mm;
			}
		}
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	
	
	/**
	 * Get the language selection. Initially all found languages are 'selected'.
	 * Later this selection can be altered manually by the user through the Web-Interface.
	 * @return language view
	 */
	public ArrayList<LanguageMatch> getSelectedLanguages() {
		return getSelectedLanguages(this.getLanguageMatchings());
	}
	
	/**
	 * Get the language selection. Initially all found languages are 'selected'.
	 * Later this selection can be altered manually by the user through the Web-Interface.
	 * @param languageMatchings
	 * @return language view
	 */
	public ArrayList<LanguageMatch> getSelectedLanguages(ArrayList<LanguageMatch> languageMatchings) {
		
		ArrayList<LanguageMatch> selectedLanguages = new ArrayList<LanguageMatch>();
		
		for (LanguageMatch lm : languageMatchings) {
			if (lm.isSelected()) {
				selectedLanguages.add(lm);
			}
		}
		return selectedLanguages;
	}
	
	
	
	public ArrayList<LanguageMatch> getLanguageMatchingsForColumn(int column) {
		
		//Utils.debug("getLanguageMatchingsForColumn "+column);
		ArrayList<LanguageMatch> languageMatchingsForColumn = new ArrayList<LanguageMatch>();
		
		for (LanguageMatch lm : languageMatchings) {
			if (lm.getConllColumn() == column) {
				languageMatchingsForColumn.add(lm);
			}
		}
		return languageMatchingsForColumn;
	}
	
	
	
	
	public ArrayList<ModelMatch> getModelMatchingsForColumn(int column) {
		
		Utils.debug("getModelMatchingsForColumn "+column);
		ArrayList<ModelMatch> modelMatchingsForColumn = new ArrayList<ModelMatch>();
		
		for (ModelMatch mm : modelMatchings) {
			if (mm.getConllColumn() == column) {
				modelMatchingsForColumn.add(mm);
			}
		}
		return modelMatchingsForColumn;
	}
	
	
	public boolean isConllFile() {
		return (fileFormat == ProcessingFormat.CONLL);
	}
	
	
	public boolean isRDFFile() {
		return (fileFormat == ProcessingFormat.RDF);
	}
	
	
	public boolean isXMLFile() {
		return (fileFormat == ProcessingFormat.XML);
	}
	
	
	public boolean getIsConllFile() {
		return isConllFile();
	}
	
	/**
	 * Get all columns that have produced model matchings
	 * @return
	 */
	public ArrayList<Integer> getConllColumnsWithModels() {
		 
		 HashSet<Integer> columns = new HashSet<Integer>();
		 for (ModelMatch mm : getModelMatchings()) {
			 columns.add (mm.getConllColumn());
  		 }
		 /*for (Integer i : columns) {
			 Utils.debug("# "+ i);
		 }*/
		 return new ArrayList<Integer>(columns);
	}
	
	
	public ArrayList<Integer> getConllColumnsWithText() {
		 
		 HashSet<Integer> columns = new HashSet<Integer>();
		 
		 if (isConllFile()) {
			 for (LanguageMatch lm : getLanguageMatchings()) {
				 int column = lm.getConllColumn();
				 columns.add(column);
	 		 }
			 /*for (Integer i : columns) {
				 Utils.debug("# "+ i);
			 }*/
		 }
		 return new ArrayList<Integer>(columns);
	}
	
	
	public HashSet<Integer> getSelectedConllColumnsWithText() {
		 
		 HashSet<Integer> columns = new HashSet<Integer>();
		 
		 if (isConllFile()) {
			 for (LanguageMatch lm : getLanguageMatchings()) {
				 if (lm.isSelected()) {
					 columns.add(lm.getConllColumn());
				 }
	 		 }
		 }
		 return columns;
	}
	
	
	
	/**
	 * Get CoNLL language columns that contain selected languages
	 * @return
	 */
	public ArrayList<Integer> getActiveConllColumnsWithText() {
		 
		 HashSet<Integer> columns = new HashSet<Integer>();
		 
		 for (int c : getConllColumnsWithText()) {
			 for (LanguageMatch lm : getSelectedLanguages()) {
				 if (lm.getConllColumn() == c) {
					 columns.add(c);
				 }
			 }
		 }
		 
		 return new ArrayList<Integer>(columns);
	}
	
	/**
	 * Get CoNLL model columns that contain selected models
	 * @return
	 */
	public ArrayList<Integer> getActiveConllColumnsWithModels() {
		 
		 HashSet<Integer> columns = new HashSet<Integer>();
		 
		 for (int c : getConllColumnsWithModels()) {
			 
			 if (getSelectedModelForColumn(new Long(c)) != null)
					columns.add(c);
		 }
		 return new ArrayList<Integer>(columns);
	}

	
	/**
	 * Web-method for manual CONLL model selection. Sets the given model as selected and deselects all
	 * other model choices for that column
	 * @param col
	 * @param selectedColModel
	 */
	public void setSelectedModelForColumn(int col, ModelType selectedColModel) {
		
		try {
		Utils.debug("setSelectedModelForColumn "+col+" "+selectedColModel);
		
		for (ModelMatch mm : getAllModelMatchingsForColumn(col)) {
			if(mm.getModelType().equals(selectedColModel)) {
				Utils.debug("true");
				mm.setSelected(true);
				mm.setDetectionMethod(DetectionMethod.MANUAL);
			} else {
				Utils.debug("false");
				mm.setSelected(false);
			}
		}
		setModelMatchings(getModelMatchings()); // update other representations
		}catch (Exception e){e.printStackTrace();}
	}
	
	
	
	/**
	 * Web-method for manual RDF model selection
	 * @param column
	 * @param modelType
	 */
	public void selectModel(int column, ModelType modelType) {
		
		Utils.debug("Select model : "+modelType.name());
		for (ModelMatch mm : getModelMatchings()) {
			if(mm.getConllColumn() == column &&
			   mm.getModelType().equals(modelType)) {
				mm.setSelected(true);
				mm.setDetectionMethod(DetectionMethod.MANUAL);
				break;
			}
		}
		setModelMatchings(getModelMatchings()); // update other representations
	}
	
	
	public int deselectModel(ModelType modelType) {
		Utils.debug("Unselect model : "+modelType.name());
		int atLeastOne = 0;
		for (ModelMatch mm : getModelMatchings()) {
			if(mm.isSelected()) {
				atLeastOne++;
			}
		}
		if (atLeastOne < 2) return 2;
		for (ModelMatch mm : getModelMatchings()) {
			if(mm.getModelType().equals(modelType)) {
				if (!mm.isSelected()) return 1;
				mm.setSelected(false);break;
			}
		}
		setModelMatchings(getModelMatchings()); // update other representations
		return 0;
	}
	
	
	public HashSet<ModelMatch> getAllModelMatchingsForColumn(int col) {
		  
		 HashSet <ModelMatch> columnModels = new HashSet <ModelMatch>();
		 for (ModelMatch mm : getModelMatchings()) {
  			 if (col != mm.getConllColumn()) continue;
  			 	columnModels.add(mm);
  		 }
		 return columnModels;
	}

	public ProcessState getProcessState() {
		return processState;
	}

	public void setProcessState(ProcessState processState) {
		
		// process state Accepted will not be withdrawn by rescanning the resource
		if (this.processState == ProcessState.ACCEPTED && processState == ProcessState.PROCESSED) return;
		this.processState = processState;
	}
	
	/**
	 * Determine the the process state of a resource depending on the quality of its results. 
	 * If the detected resource type is unknown then assign the process state
	 * DISABLED. If on the other hand model information has been detected but coverage values are very low
	 * then assign the process state CHECK. Both states CHECK and DISABLED will disqualify a resource for
	 * later RDF export (RDF export is done for resources with ACCEPPTED state only).
	 * The ACCEPTED state can be assigned to the resource by the user in the web-interface.
	 * @return True if the current process state was changed to DISABLED or CHECK else false
	 */
	public boolean verifyProcessState () {

		// TODO could be called directly in setProcessState(), update db value accordingly  

		// Do not alter process states unprocessed, accepted, edited, accepted or disabled
		if (this.processState != ProcessState.PROCESSED) return false;
		
		// DISABLED if ResourceType is unknown
		if (this.getResourceType() == ResourceType.UNKNOWN) {
			this.processState = ProcessState.DISABLED;
			return true;
		}
		
		// if ResourceType corpus and best model coverage is < 30%
		if (this.getResourceType() == ResourceType.CORPUS) {
			
			boolean minCoverageSucceeded = false;
			for (ModelMatch mm : getSelectedModels()) {
				if (mm.getModelType().equals(ModelType.valueOf("UNKNOWN"))) continue;
				if (mm.getCoverage() >= Executer.coverageThresholdOnLoad) minCoverageSucceeded = true;
			}
			
			if (!minCoverageSucceeded) {
				this.processState = ProcessState.CHECK;
				return true;
			}
		}
		
		return false;
	}
	

	public void setLanguageMatchings(int conllColumn, ArrayList<LanguageMatch> result) {
		// Remove languages of conll column
		Iterator <LanguageMatch> lms = languageMatchings.iterator();
		while (lms.hasNext()) {
			LanguageMatch next = lms.next();
			if (next.getConllColumn() == conllColumn) {
				lms.remove();
			}
		}

		// add new languages of conll column
		result.addAll(languageMatchings);
		this.setLanguageMatchings(result);
	}

	
	public ParseResult getParseResult() {
		//if (this.processState == ProcessState.UNPROCESSED) return ParseResult.UNKNOWN;
		if (!this.getErrorMsg().isEmpty()) return ParseResult.ERROR;
		if ((this.getModelMatchings().isEmpty() ||
			(this.getModelMatchings().size() == 1) && (this.getModelMatchings().get(0).getModelType().equals(ModelType.valueOf("UNKNOWN")))) &&
			this.getLanguageMatchings().isEmpty()) return ParseResult.NONE;
		return ParseResult.SUCCESS;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getRelFilePath() {
		return relFilePath;
	}

	public Date getProcessingStartDate() {
		return processingStartDate;
	}

	public void setProcessingStartDate(Date processingStartDate) {
		this.processingStartDate = processingStartDate;
	}

	public Date getProcessingEndDate() {
		return processingEndDate;
	}

	public void setProcessingEndDate(Date processingEndDate) {
		this.processingEndDate = processingEndDate;
	}
	
	public Duration getProcessingDuration() {
		
		/*long diff = processingEndDate.getTime() - processingStartDate.getTime();
		long diffSeconds = diff / 1000 % 60;
		long diffMinutes = diff / (60 * 1000) % 60;
		long diffHours = diff / (60 * 60 * 1000) % 24;
		long diffDays = diff / (24 * 60 * 60 * 1000);
		*/
		return Duration.between(processingStartDate.toInstant(), processingEndDate.toInstant());
	}

	public ArrayList<VocabularyMatch> getVocabularyMatchings() {
		return this.vocabularyMatchings;
	}

	public void setVocabularyMatchings(ArrayList<VocabularyMatch> vocabularyMatchings) {
		this.vocabularyMatchings = vocabularyMatchings;
		updateVocabularyMatchingsAsString(); // TODO move to getVocabularyMatchingsAsString()
	}
	
	/**
	 * Getter for browser
	 * @return
	 */
	public String getVocabularyMatchingsAsString() {
		return this.vocabularyMatchingsAsString;
	}
	
	
	public void updateVocabularyMatchingsAsString() {
		
		HashSet <String> vocabularies = new HashSet <String>();
		String stringRepr = "";
		
		// Filter duplicates
		for (VocabularyMatch vm : getVocabularyMatchings()) {
			vocabularies.add(vm.getVocabulary().name());	
		}
		for (String vocabulary : vocabularies) {
			stringRepr += vocabulary+", ";
		}
		if (!stringRepr.isEmpty()) stringRepr = stringRepr.substring(0, stringRepr.length()-2);
		this.vocabularyMatchingsAsString = stringRepr;
		
		//return stringRepr;
	}

	public void setColumnTokens(
			HashMap<Integer, HashMap<String, Long>> columnTokens) {
		this.columnTokens = columnTokens;
	}
	
	
	public HashMap<Integer, HashMap<String, Long>> getColumnTokens() {
		return this.columnTokens;
	}

	public String getSample() {
		return this.sample;
	}
	
	
	
	public void setSample(String sample) {
		this.sample = sample;
	}

	
	public ArrayList<Integer> getConllColumns() {
		
		ArrayList<Integer> result = new ArrayList<Integer>();
		String sample = getSample();
		if (isConllFile() && !sample.isEmpty()) {

			try {
				int columns=0;
				for (String line : sample.split("\n")) {
					if (line.trim().matches("\\d.*")) {
						columns = line.split("\t").length;
						Utils.debug("+++");
						Utils.debug("First CONLL line : "+ line);
						Utils.debug("has "+columns+" columns");
						Utils.debug("+++");
						break;
					}
				}
				int j=1;
				while (j <= columns) {
					result.add(j++);
				}
			} catch (Exception e) {
				// exception if is sample is empty (which is an error)
				Utils.debug("Error getConllColumns : "+relFilePath);
				e.printStackTrace();
			}
		}
		
		Utils.debug("countcol "+result);
		return result;
	}
	
	
	public ArrayList<Integer> getFreeConllColumns() {
		
		HashSet<Integer> freeColumns = new HashSet<Integer>(getConllColumns());
		freeColumns.removeAll(this.getConllColumnsWithText());
		freeColumns.removeAll(this.getConllColumnsWithModels());
		return new ArrayList<Integer>(freeColumns);
		
	}
	
	
	/**
	 * Get the set of different RDF properties occurring in model matchings together with
	 * the attribute value ModelMatch.differentHitTypes.
	 * @return
	 */
	public HashMap<String, Integer> getRdfPropertyDifferentValues() {
			 
		HashMap<String,Integer> rdfProperty2DifferentHitTypes = new HashMap<String,Integer>();
		for (ModelMatch mm : getModelMatchings()) {
				rdfProperty2DifferentHitTypes.put(mm.getRdfProperty(), Math.round(mm.getDifferentHitTypes()/mm.getCoverage()));
		}
		return rdfProperty2DifferentHitTypes;
	}
	
	
	/**
	 * Get the set of different RDF properties occurring in model matchings together with
	 * the attribute value ModelMatch.differentHitTypes.
	 * @return Map with values per column
	 */
	public HashMap<Integer, Integer> getConllColumnDifferentValues() {
			
		Utils.debug("getConllColumnDifferentHitTypes");
		HashMap<Integer,Integer> column2DifferentHitCount = new HashMap<Integer,Integer>();
		for (int col : getConllColumnsWithModels()) {
			for (ModelMatch mm : getModelMatchingsForColumn(col)) {
				column2DifferentHitCount.put(col, Math.round(mm.getDifferentHitTypes()/mm.getCoverage()));
				break;
			}
		}
		return column2DifferentHitCount;
	}
	
	
	/**
	 * Get the set of different XML attributes occurring in model matchings
	 * @return
	 */
	public HashSet<String> getXmlAttributes() {
		 
		HashSet<String> xmlAttributes = new HashSet<String>();
		for (ModelMatch mm : getModelMatchings()) {
				xmlAttributes.add (mm.getXmlAttribute());
		}
		return xmlAttributes;
	}
	

	/**
	 * Accepted date of resource. Can be null ! Use getAcceptedDateGetTime alternatively
	 * @return Accept date
	 */
	public Date getAcceptedDate() {
		return acceptedDate;
	}

	public void setAcceptedDate(Date acceptedDate) {
		this.acceptedDate = acceptedDate;
	}
	
	/**
	 * If the file is not accepted return 0 - else return the value of the Date.getTime() function
	 * @return Date.getTime or 0 
	 */
	public Long getAcceptedDateGetTime() {
		if (acceptedDate != null) {
			return acceptedDate.getTime();
		} else {
			return 0L;
		}
	}
	
	
	
	/**
	 * Update the list of found languages with automatically detected results from parse. Manual language selections added with
	 * the editor will always override automatic selections.
	 * @param oldLanguages Languages in database (can include manual selected languages)
	 * @param newLanguages Languages that were automatically detected by RDF/CONLL/XML parsers
	 * @param fileFormat 
	 * 
	 */
	public void updateLanguageMatchings(ArrayList <LanguageMatch> oldLanguages, ArrayList <LanguageMatch> newLanguages, ProcessingFormat fileFormat) {
	
		Utils.debug("Update language matchings");
		Utils.debug("File format : "+fileFormat);
		
		Utils.debug("Old languages :");
		for (LanguageMatch l_ : oldLanguages) {
			Utils.debug(l_.getConllColumn());
			Utils.debug(l_.getLanguageISO639Identifier());
			Utils.debug(l_.isSelected());
		}
		
		
		Utils.debug("New languages :");
		for (LanguageMatch l_ : newLanguages) {
			Utils.debug(l_.getConllColumn());
			Utils.debug(l_.getLanguageISO639Identifier());
			Utils.debug(l_.isSelected());
		}
		
		
		
		// I. Match old with new automatic languages, keep old matched languages in oldMatchedAutoLanguages
		Iterator <LanguageMatch> iteratorNew = newLanguages.iterator();
		ArrayList<LanguageMatch> oldMatchedAutoLanguages = new ArrayList<LanguageMatch>();
		LanguageMatch lmn;
		while (iteratorNew.hasNext()) {
			
			lmn = iteratorNew.next();
			if (lmn.getDetectionMethod() == DetectionMethod.AUTO) {
				
				for (LanguageMatch lmo : oldLanguages) {
					
					if (lmo.getDetectionMethod() == DetectionMethod.AUTO) {
						
						if (specifySameLanguageResult(lmo,lmn)) {
							
							// copy counts
							lmo.setAverageProb(lmn.getAverageProb());
							lmo.setMinProb(lmn.getMinProb());
							lmo.setMaxProb(lmn.getMaxProb());
							lmo.setHitCount(lmn.getHitCount());
							lmo.setDifferentHitTypes(lmn.getDifferentHitTypes());
							
							if(!lmo.getLanguageResultsAsString().equals(lmn.getLanguageResultsAsString())) {
								lmo.setUpdateText("changed");
							}
							
							// remove matched new language
							iteratorNew.remove();
							
							// remember matched old language
							oldMatchedAutoLanguages.add(lmo);
						}
					}
				}
			}
		}
		

		// II. Prepare old languages for step III.
		// a) Remove all automatic selected old languages
		// b) Remember all columns of old manual selected languages 
		Iterator <LanguageMatch> iteratorOld = oldLanguages.iterator();
		HashSet<Integer> oldSelectedManualColumns = new HashSet<Integer>();
		LanguageMatch oldLanguage;
		while (iteratorOld.hasNext()) {
			oldLanguage = iteratorOld.next();
			if (oldLanguage.getDetectionMethod() == DetectionMethod.AUTO) {
				iteratorOld.remove();
			} else {
				if (oldLanguage.isSelected()) {
					oldSelectedManualColumns.add(oldLanguage.getConllColumn());
				}
			}
		}
		
		// III. Match new auto languages with old manual languages
		// a) Copy results from new auto languages to old manual languages
		// b) Remove all new auto languages that were matched with old manual languages 
		iteratorNew = newLanguages.iterator();
		
		while (iteratorNew.hasNext()) {
			
			lmn = iteratorNew.next();
			for (LanguageMatch lmo : oldLanguages) {
				
				if (specifySameLanguageResult(lmo,lmn)) {
					
					// copy counts
					lmo.setAverageProb(lmn.getAverageProb());
					lmo.setMinProb(lmn.getMinProb());
					lmo.setMaxProb(lmn.getMaxProb());
					lmo.setHitCount(lmn.getHitCount());
					lmo.setDifferentHitTypes(lmn.getDifferentHitTypes());
					
					// save update comment for changes in results
					if(!lmo.getLanguageResultsAsString().equals(lmn.getLanguageResultsAsString())) {
						lmo.setUpdateText("changed");
					}
					
					// remove matched new language
					iteratorNew.remove();
				}
			}
		}
			
		
		// IV. Add unmatched new automatic languages
		switch (fileFormat) {
		
		case RDF :
			
			for (LanguageMatch lm : newLanguages) {
				lm.setSelected(true);
				oldLanguages.add(lm);
			}
			break;
			
		case XML :
		case CONLL :
			
			for (LanguageMatch lm : newLanguages) {
				
				// If column already has a manual selection then disable the language match
				if (oldSelectedManualColumns.contains(lm.getConllColumn())) {
					lm.setSelected(false);
				}
				oldLanguages.add(lm);
				
			}
			break;
			
		default :
			Utils.debug("Error updateLanguageMatchingsNew : file format "+fileFormat+" not recognized !");
			return;
		}
		
		// V. Add matched old automatic languages from step I.
		oldLanguages.addAll(oldMatchedAutoLanguages);
		
		// VI. Save results
		this.setLanguageMatchings(oldLanguages);
	}
	
	
	/**
	 * Return (CONLL/XML/RDF) columns that do not have a selected model
	 * @return
	 */
	public HashSet<Integer> getDisabledColumns() {
		
		HashSet<Integer> cwsm = new HashSet<Integer>(getConllColumns());
		for (ModelMatch mm : getSelectedModels()) {
			cwsm.remove(mm.getConllColumn());
		}
		return cwsm;
	}
	
	
	public String getAbsFilePath() {
		return absFilePath;
	}

	public void setAbsFilePath(String absFilePath) {
		this.absFilePath = absFilePath;
	}

	public HashMap <Integer, String> getConllcolumn2XMLAttr() {
		return conllcolumn2XMLAttr;
	}

	public void setConllcolumn2XMLAttr(HashMap <Integer, String> conllcolumn2xmlAttr) {
		conllcolumn2XMLAttr = conllcolumn2xmlAttr;
	}
	
	public ResourceType getResourceType(){
		
		if (fileFormat == ProcessingFormat.CONLL) return ResourceType.CORPUS;
		
		/*for (VocabularyMatch vm : getVocabularyMatchings()) {
			if (vm.getVocabulary() == VocabularyType.OWL) return ResourceType.ONTOLOGY;
		}*/
		
		for (ModelMatch mm : getSelectedModels()) {
			
			if (mm.getModelType().equals(ModelType.valueOf("UNKNOWN"))) continue;
			
			if(getFileResults().keySet().contains(mm)) { // model match with file result ?
				for (FileResult result : getFileResults().get(mm)) { // error nullpointer with mm once
					
					//if (result.getMatchingTagOrClass() == "http://www.w3.org/2002/07/owl#Ontology") return ResourceType.ONTOLOGY;
					
					if (result.getMatchingTagOrClass().equals("http://www.w3.org/ns/lemon/lime#Lexicon")
					|| result.getMatchingTagOrClass().equals("http://www.monnet-project.eu/lemon#Lexicon")
					|| result.getFoundTagOrClass().equals("http://www.w3.org/ns/lemon/vartrans#Translation")
					|| result.getFoundTagOrClass().equals("http://www.w3.org/ns/lemon/ontolex#LexicalEntry")
					|| result.getFoundTagOrClass().equals("http://www.lemon-model.net/lemon#LexicalEntry")
					|| result.getFoundTagOrClass().equals("http://lemon-model.net/lemon#LexicalEntry")
					|| result.getFoundTagOrClass().equals("http://www.lemon-model.net/lemon#Word")
					|| result.getFoundTagOrClass().equals("http://lemon-model.net/lemon#Word")
					|| result.getFoundTagOrClass().equals("http://www.lemon-model.net/lemon#Form")
					|| result.getFoundTagOrClass().equals("http://lemon-model.net/lemon#Form")
					|| result.getFoundTagOrClass().equals("http://www.lemon-model.net/lemon#representation")  // not an object (but anyway)
										
					) return ResourceType.LEXICON;
				}
			}
		}

		
		for (ModelMatch mm : getSelectedModels()) {
			if (!mm.getModelType().equals(ModelType.valueOf("UNKNOWN"))) return ResourceType.CORPUS;
		}
		
		
		for (VocabularyMatch vm : getVocabularyMatchings()) {
			if (vm.getVocabulary() == VocabularyType.OWL) return ResourceType.ONTOLOGY;
		}
		
		
		return ResourceType.UNKNOWN;
	}

	public Long getFileSizeInBytes() {
		return fileSizeInBytes;
	}
	
	
	public Float getFileSizeAsMBytes() {
		if (this.fileSizeInBytes == null) return 0.0f;
		Float x = this.fileSizeInBytes * 1.0f / 1048576.0f;
		return (float)((int) (x * 100.0f)) /100.0f;
	}
	

	public void setFileSizeInBytes(Long fileSizeInBytes) {
		this.fileSizeInBytes = fileSizeInBytes;
	}

	
	public void clearFileObject() {
		this.resourceFile = null;
	}

	public Vertex getFileVertex() {
		return fileVertex;
	}

	public void setFileVertex(Vertex fileVertex) {
		this.fileVertex = fileVertex;
	}

	public String getTemporaryFilePath() {
		return temporaryFilePath;
	}

	public void setTemporaryFilePath(String temporaryFilePath) {
		this.temporaryFilePath = temporaryFilePath;
	}

	public boolean getForceRescan() {
		return forceRescan;
	}

	public void setForceRescan(boolean forceRescan) {
		this.forceRescan = forceRescan;
	}

	
	public void setSampleSentences(ArrayList<ConllCSVSentence> sampleSentences) {
		this.sampleSentences = sampleSentences;
	}
	
	public ArrayList<ConllCSVSentence> getSampleSentences() {
		return this.sampleSentences;
	}
	
	/**
	 * Get file update status
	 * @param date
	 * @return Returns true if a model or a language was updated after the given date
	 */
	public boolean getWasUpdatedAfterDate (Date updatedAfter) {
	
		for (ModelMatch mm : getModelMatchings()) {
			if (updatedAfter.before(mm.getDate())) {return true;}
		}
		
		for (LanguageMatch lm : getLanguageMatchings()) {
			if (updatedAfter.before(lm.getDate())) {return true;}
		}
		
		return false;
	}
	
	
	/**
	 * Get most recent date were a model or a language has been updated
	 * @return Date
	 */
	public Date getLastUpdated () {
	
		Date lastUpdated = new Date(0);
		
		for (ModelMatch mm : getModelMatchings()) {
			if (mm.getDate().after(lastUpdated)) {
				lastUpdated = mm.getDate();
			}
		}
		
		for (LanguageMatch lm : getLanguageMatchings()) {
			if (lm.getDate().after(lastUpdated)) {
				lastUpdated = lm.getDate();
			}
		}
		
		return lastUpdated;
	}
	
	
	/**
	 * Get different comments for update date, a model or language has been updated
	 * @param Date date
	 * @return Set of different update comments
	 */
	public String getLastUpdatedText() {
		
		Date date = getLastUpdated();
		String text = "";
		String utext = "";
	
		HashSet<String> updateComments = new HashSet<String>();
		
		for (ModelMatch mm : getModelMatchings()) {
			if (mm.getDate().equals(date)) {
				utext = mm.getUpdateText().trim();
				if (!utext.isEmpty()) {
					updateComments.add(utext+" model");
				}
			}
		}
		
		for (LanguageMatch lm : getLanguageMatchings()) {
			if (lm.getDate().equals(date)) {
				utext = lm.getUpdateText().trim();
				if (!utext.isEmpty()) {
					updateComments.add(utext+" language");
				}
			}
		}
		
		for (String c : updateComments) {
			c=c.trim();
			if (!c.isEmpty()) text+=c+",";
		}
		if (text.length()>0) {
			text = text.substring(0,text.length()-1);
		}
		
		return text;
	}
	
	
	private Boolean specifySameLanguageResult(LanguageMatch lmo, LanguageMatch lmn) {
		
		if (lmo.getConllColumn() == lmn.getConllColumn()		&&
			lmo.getRdfProperty().equals(lmn.getRdfProperty())	&&
			lmo.getXmlAttribute().equals(lmn.getXmlAttribute())	&&
			lmo.getLanguageISO639Identifier().equals(lmn.getLanguageISO639Identifier())) {
			return true;
		}
		return false;
	}
	
	

}