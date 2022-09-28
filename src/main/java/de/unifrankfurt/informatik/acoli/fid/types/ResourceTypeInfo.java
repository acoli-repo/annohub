package de.unifrankfurt.informatik.acoli.fid.types;

import java.io.Serializable;

/**
 * @author frank
 *
 */
public class ResourceTypeInfo implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2026625776662701799L;
	private ResourceType resourceType = ResourceType.UNKNOWN;
	private String typeSpecifier = "";
	private DetectionMethod detectionMethod = DetectionMethod.AUTO;
	
	public ResourceTypeInfo (ResourceType resourceType, String typeSpecifier, DetectionMethod detectionMethod) {
		this.resourceType = resourceType;
		this.typeSpecifier = typeSpecifier;
		this.detectionMethod = detectionMethod;
	}
	
	/**
	 * @param resourceType
	 */
	public ResourceTypeInfo(ResourceType resourceType) {
		this.resourceType = resourceType;
	}

	public ResourceType getResourceType() {
		return resourceType;
	}
	public void setResourceType(ResourceType resourceType) {
		this.resourceType = resourceType;
	}
	public String getTypeSpecifier() {
		return typeSpecifier;
	}
	public void setTypeSpecifier(String typeSpecifier) {
		this.typeSpecifier = typeSpecifier;
	}

	public DetectionMethod getDetectionMethod() {
		return detectionMethod;
	}

	public void setDetectionMethod(DetectionMethod detectionMethod) {
		this.detectionMethod = detectionMethod;
	}

}
