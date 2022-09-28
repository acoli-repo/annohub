package de.unifrankfurt.informatik.acoli.fid.types;

import java.util.HashMap;
import java.util.Set;


public class LinghubResources {
	
	HashMap <String, String> recourceMap = new HashMap <String, String> ();
	
	public LinghubResources(){};
	public LinghubResources(HashMap <String,String> map) {
		this.recourceMap = map;
	}
	
	/**
	 * Get map from DataUrl -> LinghubUrl
	 * @return map from DataUrl -> LinghubUrl
	 */
	public HashMap <String, String> getResourceMap() {
		return recourceMap;
	}
	
	
	/**
	 * Set map from DataUrl -> LinghubUrl
	 */
	public void setResourceMap(HashMap <String, String> map) {
		recourceMap = map;
	}
	
	/**
	 * Get set of data URLs
	 * @return Data URLs
	 */
	public Set <String> getDataUrls() {
		return recourceMap.keySet();
	}

}
