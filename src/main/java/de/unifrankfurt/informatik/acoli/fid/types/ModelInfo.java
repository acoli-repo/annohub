package de.unifrankfurt.informatik.acoli.fid.types;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import de.unifrankfurt.informatik.acoli.fid.spider.ResourceChecker;
import de.unifrankfurt.informatik.acoli.fid.webclient.ExecutionBean;

public class ModelInfo implements Serializable {
	
	private static final long serialVersionUID = -7751332324328663485L;
	private File file = null;
	private ModelType modelType = null;
	private URL url = null;
	private String name = null;
	private String fileName = null;
	private boolean active = true;
	private ModelUsage usage = null;
	private String documentationUrl = "";
	//private boolean online = false;
	private DataLinkState dataLinkState = DataLinkState.BROKEN;
	private Date date = null; // the date a model was loaded into the model graph
	
	private final static String oliaHtmlBase = "http://www.acoli.informatik.uni-frankfurt.de/resources/olia/html/";

	
	public ModelInfo(String urlString, String documentationUrl, ModelType modelType, ModelUsage usage, boolean active) {
		try {
			this.url = new URL (urlString);
			this.fileName = new File(url.getPath()).getName();
			this.name = FilenameUtils.removeExtension(fileName).toLowerCase();
			this.file = null;
			this.active = active;
			this.modelType = modelType;
			this.usage = usage;
			this.documentationUrl = documentationUrl;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public File getFile() {
		return file;
	}
	
	public void setFile(File file) {
		this.file = file;
		this.fileName = file.getName();
	}
	
	public ModelType getModelType() {
		return modelType;
	}
	
	public void setModelType(ModelType modelType) {
		this.modelType = modelType;
	}

	public URL getUrl() {
		return url;
	}

	public void setUrl(URL ontology) {
		this.url = ontology;
	}

	public void setOntology(String ontologyUrl) {	
		try {
			this.url = new URL (ontologyUrl);
		} catch (MalformedURLException e) {}
	}

	/**
	 * File name without extension as lower case
	 * @return 
	 */
	public String getName() {
		return name;
	}

	public String getFileName() {
		return fileName;
	}
	
	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	
	public void setModelUsage(ModelUsage usage) {
		this.usage = usage;
	}
	
	public ModelUsage getModelUsage() {
		return this.usage;
	}
	
	/**
	 *(Experimental) Get a link to http://www.acoli.informatik.uni-frankfurt.de/resources/olia/html/ .
	 * The link provides a detailed description of OLiA annotation/linking model
	 * for the model
	 * @return
	 * @deprecated
	 */
	public String getOliaHtmlInfoUrl() {

		try {
			Path path = Paths.get(getUrl().getFile());
			String modelName = FilenameUtils.removeExtension(path.getFileName().toString());
			return oliaHtmlBase+path.subpath(path.getNameCount()-2, path.getNameCount()-1).toString()+"/"+modelName+".html";
		} catch (Exception e) {
		}
		
		return null;
	}
	
	public String getDocumentationUrl() {
		return documentationUrl;
	}
	
	public void setDocumentationUrl(String documentationUrl) {
		this.documentationUrl = documentationUrl;
	}
	
	public boolean isOnline() {
		return this.dataLinkState != DataLinkState.BROKEN;
		//return this.online;
	}
	
	public String getID() {
		return modelType.name()+"#"+url+"#"+usage.name()+"#"+active;
	}


	public ModelUsage getUsage() {
		return usage;
	}


	public void setUsage(ModelUsage usage) {
		this.usage = usage;
	}


	public void setName(String name) {
		this.name = name;
	}
	
	
	public static void checkModelsOnline(List<ModelInfo> modelList, ResourceChecker resourceChecker) {
		
		HashMap<ModelInfo, ResourceInfo> rs = new HashMap<ModelInfo,ResourceInfo>();
		for (ModelInfo mi : modelList) {
			
			ResourceInfo resourceInfo = new ResourceInfo(mi.getUrl().toString(),"");
    		rs.put(mi, resourceInfo);
		}
		
		resourceChecker.checkBrokenLinksThreaded(rs.values(), 10);
    		
    		
//		while (!resourceChecker.getThreadRef().isDone()) {
//			try {
//				Thread.sleep(500);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//		
//		for (ModelInfo mi : rs.keySet()) {
//    		mi.setOnline(rs.get(mi).getResourceState() != ResourceState.ResourceUrlIsBroken);
//		}
	}
	
	
	public static void checkModelsOnlineOld(List<ModelInfo> modelList, ResourceChecker resourceChecker) {
		
		List<ResourceInfo> rs = new ArrayList<ResourceInfo>();
		for (ModelInfo mi : modelList) {
			
			ResourceInfo resourceInfo = new ResourceInfo(mi.getUrl().toString(),"");
    		rs.add(resourceInfo);
		}
		
		for (ModelInfo mi : modelList) {
    		
    		//progressText = mi.getUrl().toString();
    		
    		//showInfo(progressText);
    		/*FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:progressbar");
    		RequestContext.getCurrentInstance().reset("form:progressbar");*/
    		
    		ResourceInfo resourceInfo = new ResourceInfo(mi.getUrl().toString(),"");
    		ArrayList<ResourceInfo> resources = new ArrayList<ResourceInfo>();
    		resources.add(resourceInfo);

    		resourceChecker.checkBrokenLinksThreaded(resources, 10);
    		
    		
    		while (!resourceChecker.getThreadRef().isDone()) {
    			try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
    		}
    		//mi.setOnline(resourceInfo.getResourceState() != ResourceState.ResourceUrlIsBroken);
    	}
	}


	public DataLinkState getDataLinkState() {
		return dataLinkState;
	}


	public void setDataLinkState(DataLinkState dataLinkState) {
		this.dataLinkState = dataLinkState;
	}


	public Date getDate() {
		return date;
	}


	public void setDate(Date date) {
		this.date = date;
	}
	
}
