package de.unifrankfurt.informatik.acoli.fid.types;

import java.io.Serializable;

public class BLLConcept implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2509672033589965659L;
	
	public String classUrl = "";
	public String label	= "";
	
	
	public BLLConcept (String classUrl, String label) {
		this.classUrl = classUrl;
		this.label = label;
	}
	
	
	public String getClassWithoutNamespace() {
		return classUrl.substring(classUrl.lastIndexOf("#"),classUrl.length());
	}


	public String getClassUrl() {
		return classUrl;
	}


	public void setClassUrl(String classUrl) {
		this.classUrl = classUrl;
	}


	public String getLabel() {
		return label;
	}


	public void setLabel(String label) {
		this.label = label;
	}

}
