package de.unifrankfurt.informatik.acoli.fid.types;

import java.util.Date;

/**
 * @author frank
 *
 */
public class EditInfo {

	private String resourceIdentifier;
	private long   editingStartTime;
	private String userID;
	
	
	public EditInfo (String resourceIdentifier, String userID) {
		this.userID = userID;
		this.resourceIdentifier = resourceIdentifier;
		this.editingStartTime = new Date().getTime();
	}
	
	public String getResourceIdentifier() {
		return resourceIdentifier;
	}
	public void setResourceIdentifier(String resourceIdentifier) {
		this.resourceIdentifier = resourceIdentifier;
	}
	public long getEditingStartTime() {
		return editingStartTime;
	}
	public void setEditingStartTime(long editingStartTime) {
		this.editingStartTime = editingStartTime;
	}
	public String getUserID() {
		return userID;
	}
	public void setUserID(String userID) {
		this.userID = userID;
	}
	
	
}
