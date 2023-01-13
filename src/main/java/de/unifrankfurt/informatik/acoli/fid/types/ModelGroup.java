package de.unifrankfurt.informatik.acoli.fid.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


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
	
	public void setTagNameSpaces(String[] namespaces) {
		
		HashSet<String> set = new HashSet<String>();
		for (String ns : namespaces) {
			set.add(ns.trim());
		}
		this.tagNamespaces = set.toArray(new String[set.size()]);
	}
	
	public String[] getClassNameSpaces() {
		return classNamespaces;
	}
	
	public void setClassNameSpaces(String[] namespaces) {

		HashSet<String> set = new HashSet<String>();
		for (String ns : namespaces) {
			set.add(ns.trim());
		}
		this.classNamespaces = set.toArray(new String[set.size()]);
	}
	
	@SuppressWarnings("unchecked")
	public List<String> getNameSpaces() {
		
		Set<String> namespaces = new HashSet<String>();
		namespaces.addAll(Arrays.asList(getTagNameSpaces()));
		namespaces.addAll(Arrays.asList(getClassNameSpaces()));
		return new ArrayList<String>(namespaces);
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

	public String getNameSpacesAsString() {
		return String.join(",", getNameSpaces());
	}

	public void setNameSpaces(String[] namespaces) {
		setTagNameSpaces(namespaces);
		setClassNameSpaces(namespaces);
	}
	
}