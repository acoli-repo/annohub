package de.unifrankfurt.informatik.acoli.fid.types;

import java.io.Serializable;
import java.util.ArrayList;

public class ModelGroup implements Serializable {
	
	
	private static final long serialVersionUID = 1282322958739564787L;

	ArrayList <ModelInfo> modelFiles = new ArrayList<ModelInfo>();
	
	String documentationUrl = "";
	
	boolean useDocumentationUrl = false;
	// If false then the documentation url of each model in the modelFiles array will be shown
	// if true the the documentation above will be used
	
	String [] classNamespaces = {};
	String [] tagNamespaces = {};
	String niceName="";
	ModelType modelType = null;
	
	
	public ModelGroup(){};
	
	public ModelGroup (ArrayList <ModelInfo> modelFiles, String documentationUrl, boolean useDefaultDocumentation, String[] classNamespaces, String[] tagNamespaces) {
		
		this.modelFiles = modelFiles;
		this.documentationUrl = documentationUrl;
		this.useDocumentationUrl = useDefaultDocumentation;
		this.classNamespaces = classNamespaces;
		this.tagNamespaces = tagNamespaces;
	}

	public String getDocumentationUrl() {
		return documentationUrl;
	}

	public void setDocumentationUrl(String documentationUrl) {
		this.documentationUrl = documentationUrl;
	}

	public boolean isUseDocumentationUrl() {
		return useDocumentationUrl;
	}

	public void setUseDocumentationUrl(boolean useDocumentationUrl) {
		this.useDocumentationUrl = useDocumentationUrl;
	}

	public ArrayList<ModelInfo> getModelFiles() {
		return modelFiles;
	}

	public void setModelFiles(ArrayList<ModelInfo> modelFiles) {
		this.modelFiles = modelFiles;
	}

	public String[] getTagNameSpaces() {
		return tagNamespaces;
	}
	
	public void setTagNameSpaces(String[] nameSpaces) {
		this.tagNamespaces = nameSpaces;
	}
	
	public String[] getClassNameSpaces() {
		return classNamespaces;
	}
	
	public void setClassNameSpaces(String[] classNameSpaces) {
		this.classNamespaces = classNameSpaces;
	}
	
	public String getNiceName() {
		return niceName;
	}
	
	public void setNiceName(String niceName) {
		this.niceName = niceName;
	}
	
	public ModelType getModelType() {
		return modelType;
	}

	public void setModelType(ModelType modelType) {
		this.modelType = modelType;
	}
	
	public String getID() {
		return modelType.name();
	}
	
}