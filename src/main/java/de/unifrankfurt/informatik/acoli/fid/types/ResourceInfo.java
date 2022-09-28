package de.unifrankfurt.informatik.acoli.fid.types;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.unifrankfurt.informatik.acoli.fid.conll.ConllInfo;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager;
import de.unifrankfurt.informatik.acoli.fid.util.IndexUtils;


/**
 * Language data resource (not restricted to Linghub)
 * @author frank
 */
public class ResourceInfo implements Serializable {

	private String dataURL;												// used with linghub accessUrl
	private String metaDataURL=ResourceManager.MetaDataToBeClarified;	// used with linghub distro
	private String metaDataURL2="http://linghub/dataset/dummy";			// used with linghub dataset (default)
	private ResourceFormat resourceFormat = ResourceFormat.UNKNOWN;
	private Vertex resource = null;
	private FileInfo fileInfo = new FileInfo();
	private int httpResponseCode = 0;
	private Long httpContentLength = 0L;
	private String httpContentType = "";
	private String httpLastModified = "";
	private ResourceMetadata linghubAttributes = new ResourceMetadata();
	private transient ConllInfo conllInfo = null;
	private DetectionMethod detectionMethod=DetectionMethod.LINGHUB; 	// Default
	private boolean samplingActive = true;
	private String httpETag = "";
	private ResourceState resourceState;
	private ResourceProcessState resourceProcessState = ResourceProcessState.STARTED;
	private String userID=null;
	private ArrayList<ResourceTypeInfo> resourceTypeInfos = new ArrayList<ResourceTypeInfo>();
	private HashMap <ResourceFormat, ParseStats> customParseStats = null;
	private List<Comment> comments = new ArrayList<Comment>();
	private boolean approved = false;
	private int queuePosition = 0;
	private Boolean resourceUploadImportMetadata=false;
	private Boolean resourceUploadAutoAccept = false;
	private String md5="";
	private String sha256="";

	
	public static final long serialVersionUID = -667211071635L;
	

	/**
	 * Use this constructor for a resource with unknown format !
	 * @param dataURL
	 * @param metaDataURL	(linghub distribution)
	 * @param metaDataURL2  (linghub dataset)
	 */
	public ResourceInfo(String dataURL, String metaDataURL, String metaDataURL2) {
		
		setDataURL(dataURL);
		setMetaDataURL(metaDataURL);
		setMetaDataURL2(metaDataURL2);
		this.resourceFormat = IndexUtils.determineResourceFormat(this);
	}
	
	/**
	 * 
	 * @param dataURL
	 * @param metaDataURL	(linghub dataset)
	 * @param metaDataURL2	(linghub distribution)
	 * @param resourceFormat
	 */
	public ResourceInfo(String dataURL, String metaDataURL, String metaDataURL2, ResourceFormat resourceFormat) {
		
		setDataURL(dataURL);
		setMetaDataURL(metaDataURL);
		setMetaDataURL2(metaDataURL2);
		this.resourceFormat = resourceFormat;	
	}
	
	
	/**
	 * 
	 * @param dataURL
	 * @param metaDataURL	(Linghub dataset)
	 * @param resourceFormat
	 */
	public ResourceInfo(String dataURL, String metaDataURL, ResourceFormat resourceFormat) {
		
		setDataURL(dataURL);
		setMetaDataURL(metaDataURL);
		this.resourceFormat = resourceFormat;		
	}
	
	
	/**
	 * Use this constructor for a resource with unknown format !
	 * @param dataURL
	 * @param metaDataURL	(Linghub dataset)
	 */
	public ResourceInfo(String dataURL, String metaDataURL) {
		
		setDataURL(dataURL);
		setMetaDataURL(metaDataURL);
		this.resourceFormat = IndexUtils.determineResourceFormat(this);
	}
	
	
	
	/**
	 * 
	 * @param dataURL
	 * @param metaDataURL	(linghub dataset)
	 * @param metaDataURL2	(linghub distribution)
	 * @param resourceFormat
	 * @param httpResponseCode
	 * @param httpContentLength
	 * @param httpContentType
	 * @param httpLastModified
	 */
	public ResourceInfo(String dataURL, String metaDataURL, String metaDataURL2,
			ResourceFormat resourceFormat, int httpResponseCode,
			Long httpContentLength, String httpContentType,
			String httpLastModified, String httpETag) {
		
		setDataURL(dataURL);
		setMetaDataURL(metaDataURL);
		setMetaDataURL2(metaDataURL2);
		this.resourceFormat = resourceFormat;		
		this.httpResponseCode = httpResponseCode;
		this.httpContentLength = httpContentLength;
		this.httpContentType = httpContentType;
		this.httpLastModified = httpLastModified;
		this.httpETag = httpETag;
	}


	public ResourceInfo() {}

	
	public String getDataURL() {
		return dataURL;
	}
	
	
	public void setDataURL(String dataURL) {
		String tmp = IndexUtils.checkFileURL(dataURL);
		if(tmp != null) this.dataURL = tmp;
		// otherwise use default dummy URL
	}
	/**
	 * @return metadataURL
	 */
	public String getMetaDataURL() {
		return metaDataURL;
	}
	/**
	 * @param metadataURL
	 */
	public void setMetaDataURL(String metaDataURL) {
		String tmp = IndexUtils.checkFileURL(metaDataURL);
		if (tmp != null) this.metaDataURL = tmp; 
		// otherwise use default dummy URL
	}
	
	/**
	 * @param metaDataURL2
	 */
	public void setMetaDataURL2(String metaDataURL2) {
		if (!(metaDataURL == null)) {
			String tmp = IndexUtils.checkFileURL(metaDataURL2);
			if(tmp != null) this.metaDataURL2 = tmp;
		}
		// otherwise use default dummy URL
	}
	
	/**
	 * @return metaDataURL2
	 */
	public String getMetaDataURL2() {
		return metaDataURL2;
	}

	public ResourceFormat getResourceFormat() {
		return resourceFormat;
	}
	public void setResourceFormat(ResourceFormat format) {
		this.resourceFormat = format;
	}
	
	/**
	 * @return the resource
	 */
	public Vertex getResource() {
		return resource;
	}

	/**
	 * @param resource the resource to set
	 */
	public void setResource(Vertex resource) {
		this.resource = resource;
	}

	public FileInfo getFileInfo() {
		return fileInfo;
	}

	public void setFileInfo(FileInfo fileInfo) {
		this.fileInfo = fileInfo;
	}
	
	
	public void setHttpResponseValues(HttpResponse httpResponse) {
		this.httpResponseCode = parseHttpStatusCode(httpResponse);
		this.httpContentLength = parseHttpContentLength(httpResponse);
		this.httpContentType = parseHttpContentType(httpResponse);
		this.httpLastModified = parseHttpLastModified(httpResponse);
		this.httpETag  = parseHttpETag(httpResponse);
	}
	
	
	private int parseHttpStatusCode(HttpResponse httpResponse) {
		int responseCode=0;
		try {responseCode = httpResponse.getStatusLine().getStatusCode();} catch (Exception e){};
		return responseCode;
	}
	
	private String parseHttpLastModified(HttpResponse httpResponse) {
		String lastModified = "";
		try {lastModified = httpResponse.getFirstHeader("Last-Modified").getValue();} catch (Exception e){}
		return lastModified;
	}
	
	private String parseHttpContentType(HttpResponse httpResponse) {
		String contentType="";
		try {contentType = httpResponse.getFirstHeader("Content-Type").getValue();} catch (Exception e){};
		return contentType;
	}

	private Long parseHttpContentLength(HttpResponse httpResponse) {
		Long contentLength=0L;
		try {contentLength = Long.parseLong(httpResponse.getFirstHeader("Content-Length").getValue());} catch (Exception e){};
		return contentLength;
	}
	
	private String parseHttpETag(HttpResponse httpResponse) {
		String etag="";
		try {etag = httpResponse.getFirstHeader("ETag").getValue();} catch (Exception e){};
		return etag;
	}


	// Getter for HTTP response fields
	public String getHttpLastModified() {
		return this.httpLastModified;
	}
	
	public String getHttpContentType() {
		return this.httpContentType;
	}

	public Long getHttpContentLength() {
		return this.httpContentLength;
	}
	
	public Float getHttpContentLengthAsMBytes() {
		if (this.httpContentLength == null) return 0.0f;
		Float x = this.httpContentLength * 1.0f / 1048576.0f;
		return (float)((int) (x * 100.0f)) /100.0f;
	}
	

	public ConllInfo getConllInfo() {
		return conllInfo;
	}

	public void setConllInfo(ConllInfo conllInfo) {
		this.conllInfo = conllInfo;
	}

	public DetectionMethod getDetectionMethod() {
		return detectionMethod;
	}

	public void setDetectionMethod(DetectionMethod detectionMethod) {
		this.detectionMethod = detectionMethod;
	}

	public String getResourceID() {
		return dataURL+"###"+fileInfo.getRelFilePath();
	}

	public ResourceMetadata getResourceMetadata() {
		return linghubAttributes;
	}

	public void setResourceMetadata(ResourceMetadata linghubAttributes) {
		this.linghubAttributes = linghubAttributes;
	}

	public int getHttpResponseCode() {
		return httpResponseCode;
	}

	public void setHttpResponseCode(int httpResponseCode) {
		this.httpResponseCode = httpResponseCode;
	}

	public void setHttpContentLength(Long httpContentLength) {
		this.httpContentLength = httpContentLength;
	}

	public void setHttpContentType(String httpContentType) {
		this.httpContentType = httpContentType;
	}

	public void setHttpLastModified(String httpLastModified) {
		this.httpLastModified = httpLastModified;
	}

	public void setSamplingActive(boolean samplingActive) {
		this.samplingActive = samplingActive;
	}
	
	public boolean isSamplingActive() {
		return samplingActive;
	}

	public String getHttpETag() {
		return httpETag;
	}

	public void setResourceState(ResourceState resourceState) {
		this.resourceState = resourceState;
	}
	
	public ResourceState getResourceState() {
		return this.resourceState;
	}
	
	public boolean getResourceIsOnline() {
		if (this.resourceState == null) {
			//System.out.println(this.dataURL+" is online");
			return true;
		}
		else {
			//System.out.println(this.dataURL+" is online = "+(this.resourceState != ResourceState.ResourceUrlIsBroken));
			return this.resourceState != ResourceState.ResourceUrlIsBroken;
		}
	}

	public ResourceProcessState getResourceProcessState() {
		return resourceProcessState;
	}
	
	public String getExtendedResourceProcessState() {
		if (this.resourceProcessState != ResourceProcessState.WAITING) {
			return this.resourceProcessState.name();
		}
		else {
			// alternative queue
			return this.resourceProcessState.name();
			//return this.resourceProcessState.name()+" ("+this.getQueuePosition()+")";
		}
	}

	public void setResourceProcessState(ResourceProcessState resourceProcessState) {
		this.resourceProcessState = resourceProcessState;
	}

	public void setUserID(String userid) {
		this.userID = userid;		
	}
	
	public String getUserID() {
		return this.userID;
	}
	
	public String getShadedUserID() {
		if (userID.equals("ub_admin")) {
			return "UB";
		} else {
			return this.userID;
		}
	}

	public String getMessageID() {
		return this.userID+"#"+this.dataURL;
	}

	public ArrayList<ResourceTypeInfo> getResourceTypeInfos() {
		return resourceTypeInfos;
	}

	public void setResourceTypeInfos(ArrayList<ResourceTypeInfo> resourceTypeInfo) {
		this.resourceTypeInfos = resourceTypeInfo;
	}
	
	public String getResourceTypeInfosAsString() {
		
		HashSet<ResourceType> done = new HashSet<ResourceType>();
		
		String info = "";
		for (ResourceTypeInfo rti : resourceTypeInfos) {
			ResourceType x = rti.getResourceType();
			if (x != ResourceType.UNKNOWN && !done.contains(rti.getResourceType())) {
				info += rti.getTypeSpecifier()+" "+x.name()+",";
				done.add(rti.getResourceType());
			}
		}
		if (!info.isEmpty()) {
			info = info.substring(0,info.length()-1);
		} else {
			info = ResourceType.UNKNOWN.name();
		}
		
		return info;
	}

	public HashMap <ResourceFormat, ParseStats> getCustomParseStats() {
		return customParseStats;
	}

	public void setCustomParseStats(HashMap <ResourceFormat, ParseStats> customParseStats) {
		this.customParseStats = customParseStats;
	}


	public void setComments(List<Comment> comments) {
		this.comments = comments;
	}
	
	public List<Comment> getComments() {
		return comments;
	}
	
	public Comment getLatestComment() {
		
		if (comments.isEmpty()) return null;
		Collections.sort(comments);
		return comments.get(comments.size()-1);
	}
	
	public String getCommentInfo() {
		
		if (comments.isEmpty()) return null;
		
		int cCount=comments.size();
		Collections.sort(comments);
		Comment lc = comments.get(comments.size()-1);
		String info = cCount+",  latest on\n"+new Date(lc.getDate())+"\n by "+lc.getUserId();
		return info;
	}

	public boolean isApproved() {
		return approved;
	}

	public void setApproved(boolean approved) {
		this.approved = approved;
	}
	
	public String getApprovedText() {
		if(approved) return "yes";else return "no";
	}
	
	/**
	 * Determine the resource approved state by the following rule: If at least one file of the resource has state
	 * ACCEPTED then the resource shall be approved.
	 * @param fileprocessStates
	 * @return approved (true/false)
	 */
	public static boolean computeApproved(HashSet<ProcessState> fileprocessStates) {
			
		if (fileprocessStates.contains(ProcessState.ACCEPTED)) {
			return true;
		  } else {
 			return false;
		  }
	}
	
	/**
	 * More strict method to determine approved state of a resource. All resources must have either state
	 * ACCEPTED or state DISABLED. In this way it is enshured that all files in a resource have been reviewed.
	 * @param fileprocessStates
	 * @return approved (true/false)
	 */
	public static boolean computeApproved2(HashSet<ProcessState> fileprocessStates) {
		
		if (fileprocessStates.contains(ProcessState.CHECK)		||
			fileprocessStates.contains(ProcessState.EDITED)		||
			fileprocessStates.contains(ProcessState.PROCESSED)	||
			fileprocessStates.contains(ProcessState.UNPROCESSED)
		) {
			return false;
		  } else {
 			return true;
		  }
	}

	public int getQueuePosition() {
		return queuePosition;
	}

	public void setQueuePosition(int queuePosition) {
		this.queuePosition = queuePosition;
	}

	
	public Boolean getResourceUploadImportMetadata() {
		return resourceUploadImportMetadata;
	}

	public void setResourceUploadImportMetadata(
			Boolean resourceUploadImportMetadata) {
		this.resourceUploadImportMetadata = resourceUploadImportMetadata;
	}

	public void setResourceUploadAutoAccept(Boolean resourceUploadAutoAccept) {
		this.resourceUploadAutoAccept = resourceUploadAutoAccept;		
	}

	public Boolean getResourceUploadAutoAccept() {
		return resourceUploadAutoAccept;
	}

	public String getMd5() {
		return md5;
	}

	public void setMd5(String md5) {
		this.md5 = md5;
	}

	public String getSha256() {
		return sha256;
	}

	public void setSha256(String sha256) {
		this.sha256 = sha256;
	}

}