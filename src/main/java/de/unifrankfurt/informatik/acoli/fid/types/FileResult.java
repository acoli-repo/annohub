package de.unifrankfurt.informatik.acoli.fid.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;

import de.unifrankfurt.informatik.acoli.fid.util.IndexUtils;

/**
 * Class models hits in Olia or BLL model
 * @author frank
 *
 */
public class FileResult implements Comparable<FileResult>, Serializable {

	private static final long serialVersionUID = -6855853564872799762L;
	private String propertyOrAttribute="";
	private String foundTagOrClass = "";
	private String matchingTagOrClass = "";
	private String bestMatchingOliaClass = "";
	private ArrayList<String> minimalOliaPath = new ArrayList<String>();
	private String matchCount = "";
	private String matchType="";	// GWriter.VertexType
	private boolean unique;
	private String featureName=""; // left hand side of complex expression name=value
	private ArrayList<BLLConcept> bllConcepts = new ArrayList<BLLConcept>();
	
	
	
	public FileResult(){};
	
	public FileResult (String foundTagOrClass, String matchingTagOrClass, Long matchCount) {
		this.matchCount = matchCount.toString();
		this.foundTagOrClass = foundTagOrClass;
		this.matchingTagOrClass = matchingTagOrClass;
	}
		
	
	public String getFoundTagOrClass() {
		return foundTagOrClass;
	}

	public String getMatchCount() {
		return matchCount;
	}
	
	public String getMatchingTagOrClass() {
		return matchingTagOrClass;
	}

	public void setFoundTagOrClass(String foundTagOrClass) {
		this.foundTagOrClass = foundTagOrClass;
	}

	public void setMatchingTagOrClass(String matchingTagOrClass) {
		this.matchingTagOrClass = matchingTagOrClass;
	}

	public void setMatchCount(Long matchCount) {
		this.matchCount = matchCount.toString();
	}

	public void setMatchType(String label) {
		this.matchType = label;
	}
	
	public String getMatchType() {
		return this.matchType;
	}

	
	@Override
	public int compareTo(FileResult other) {
		return foundTagOrClass.compareTo(other.foundTagOrClass);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((foundTagOrClass == null) ? 0 : foundTagOrClass.hashCode());
		result = prime * result
				+ ((matchCount == null) ? 0 : matchCount.hashCode());
		result = prime * result
				+ ((matchType == null) ? 0 : matchType.hashCode());
		result = prime
				* result
				+ ((matchingTagOrClass == null) ? 0 : matchingTagOrClass
						.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileResult other = (FileResult) obj;
		if (foundTagOrClass == null) {
			if (other.foundTagOrClass != null)
				return false;
		} else if (!foundTagOrClass.equals(other.foundTagOrClass))
			return false;
		if (matchCount == null) {
			if (other.matchCount != null)
				return false;
		} else if (!matchCount.equals(other.matchCount))
			return false;
		if (matchType == null) {
			if (other.matchType != null)
				return false;
		} else if (!matchType.equals(other.matchType))
			return false;
		if (matchingTagOrClass == null) {
			if (other.matchingTagOrClass != null)
				return false;
		} else if (!matchingTagOrClass.equals(other.matchingTagOrClass))
			return false;
		return true;
	}

	public boolean isUnique() {
		return unique;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public String getFeatureName() {
		return featureName;
	}

	public void setFeatureName(String featureName) {
		this.featureName = featureName;
	}

	public String getPropertyOrAttribute() {
		return propertyOrAttribute;
	}

	public void setPropertyOrAttribute(String propertyOrAttribute) {
		this.propertyOrAttribute = propertyOrAttribute;
	}

	public ArrayList<BLLConcept> getBllConcepts() {
		return bllConcepts;
	}

	public void setBllConcepts(ArrayList<BLLConcept> bllConcepts) {
		this.bllConcepts = bllConcepts;
	}
	
	public String getBllConceptsAsString() {
		
		String result = "";
		for (BLLConcept c : getBllConcepts()) {
			result+=","+c.getClassUrl().split("#")[1];
		}
		if (result.isEmpty()) 
			return result;
		else
			return result.substring(1, result.length());
	}
	
	/**
	 * The result matches a feature
	 * @return is a feature
	 */
	public boolean matchesFeature() {
		if (!featureName.trim().isEmpty()) return true;
		else 
		return false;
	}
	
	/**
	 * The result matches a ontology class
	 * @return is a class
	 */
	public boolean matchesClass() {
		if (this.foundTagOrClass.startsWith("http")) return true;
		else 
		return false;
	}
	
	/**
	 * The result matches a annotation tag
	 * @return is a tag
	 */
	public boolean matchesTag() {
		if (!matchesFeature() && !matchesClass()) return true;
		else 
		return false;
	}
	
	
	public String getTableID() {
		return  propertyOrAttribute+"#"+
				foundTagOrClass+"#"+
				matchingTagOrClass+"#"+
				matchCount+"#"+
				matchType+"#"+
				featureName;
	}
	
	
	public boolean isUrl() {
		return IndexUtils.isValidURL(foundTagOrClass);
	}
	
	public boolean isTag() {
		return (!isUrl());
	}
	
	public boolean isFeature() {
		return (!getFeatureName().trim().isEmpty() && !getFoundTagOrClass().trim().isEmpty());
	}

	public ArrayList<String> getMinimalOliaPath() {
		return minimalOliaPath;
	}
	
	/*public String getMinimalOliaClassesAsString() {
		String result = "";
		for (String y : minimalOliaClasses) {
			result+=y.split("#")[1]+" ";
		}
		return result;
	}*/

	public void setMinimalOliaPath(ArrayList <String> minimalOliaPath) {
		this.minimalOliaPath = minimalOliaPath;
		if (!minimalOliaPath.isEmpty()) {
			bestMatchingOliaClass = minimalOliaPath.get(minimalOliaPath.size()-1);
			//System.out.println("hello "+bestMatchingOliaClass);
		}
	}

	public String getBestMatchingOliaClass() {
		return bestMatchingOliaClass;
	}
	
	
	public String getBestMatchingOliaClassAsString() {
		if (bestMatchingOliaClass.isEmpty()) return "";
		else
		return bestMatchingOliaClass.split("#")[1];
	}

	
	public void setBestMatchingOliaClass(String bestMatchedOliaClass) {
		this.bestMatchingOliaClass = bestMatchedOliaClass;
	}

}
