package de.unifrankfurt.informatik.acoli.fid.webclient;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import de.unifrankfurt.informatik.acoli.fid.types.AccountType;
import de.unifrankfurt.informatik.acoli.fid.types.EditInfo;
import de.unifrankfurt.informatik.acoli.fid.types.UserAccount;

/**
 * @author frank
 *
 */
public class EditManager {
	
	private ArrayList<EditInfo> editedResources = new ArrayList<EditInfo>();
	private static long maxEditTimeInMinutes = 30L;
	
	
	/**
	 * Ask for editing permission. 
	 * @param resourceIdentifier
	 * @param userID
	 * @return edit permission yes/no
	 */
	public boolean getResourceEditLock(String resourceIdentifier, UserAccount userAccount) {
		
		// If a resource has an edit lock then it can be opened only in read mode !
		// TODO guest can make changes in the edit window, but cannot save edits.
		// This may affect the view of the same resource of another user ?
		if (userAccount.getAccountType() == AccountType.GUEST) return true; // cannot save and requires no lock
		
		for (EditInfo ei : editedResources) {
			if (ei.getResourceIdentifier().equals(resourceIdentifier)) {
				
				if (TimeUnit.MINUTES.convert(Math.abs(new Date().getTime()
			    		-  ei.getEditingStartTime()), TimeUnit.MILLISECONDS) < maxEditTimeInMinutes) {
					return false;
				} else {
					
					// max edit time is reached then reset editInfo
					ei.setUserID(userAccount.getUserID());
					ei.setEditingStartTime(new Date().getTime());
					return true;
				}
			}
		}
	   
		// Resource is not edited by any user => get edit lock for resource
		editedResources.add(new EditInfo(resourceIdentifier, userAccount.getUserID()));
		return true;
	}
	
	
	/**
	 * Release edit lock for a resource
	 * @param resourceIdentifier
	 */
	public void clearResourceEditLock(String resourceIdentifier) {
		Iterator<EditInfo> iterator = editedResources.iterator();
		while (iterator.hasNext()) {
			EditInfo ei = iterator.next();
			if (ei.getResourceIdentifier().equals(resourceIdentifier)) {
				iterator.remove();
				break;
			};
		}
	}
	
	/**
	 * Remove all resource edit locks for user
	 * @param userID
	 */
	public void removeEditLocks(String userID) {
		
		Iterator<EditInfo> iterator = editedResources.iterator();
		while (iterator.hasNext()) {
			EditInfo ei = iterator.next();
			if (ei.getUserID().equals(userID)) {
				iterator.remove();
			};
		}
	}
	
	
	public boolean isLocked(String resourceIdentifier) {
		
		for (EditInfo ei : editedResources) {
			if (ei.getResourceIdentifier().equals(resourceIdentifier)) {
				
				// found in edited resource and max edit time not reached
				if (TimeUnit.MINUTES.convert(Math.abs(new Date().getTime()
			    		-  ei.getEditingStartTime()), TimeUnit.MILLISECONDS) < maxEditTimeInMinutes) {
					return true;
				} else {
				
				// found in edited resources, but max edit time passed
				return false;
				}
			}
		}
		
		// not found in edited resources
		return false;
	}

	

}
