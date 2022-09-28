package de.unifrankfurt.informatik.acoli.fid.webclient;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import de.unifrankfurt.informatik.acoli.fid.util.Utils;

/**
 * @author frank
 *
 */
public class UserLog {
	
	private static HashMap<String, Long> usersOnline = new  HashMap<String, Long>();
	private Long loginLimit = 60 * 60 * 6 * 1000L; // milliseconds in 6 hours

	
	public UserLog(){};
	
	public HashMap<String, Long> getUsersOnline() {
		
		
		Iterator<Map.Entry<String, Long>> iterator = usersOnline.entrySet().iterator();
		
		while(iterator.hasNext()) {
			
		    Map.Entry<String, Long> entry = iterator.next();
		    String user = entry.getKey();
		    Utils.debug("online : "+user);
		    
		    if(!userIsOnline(user)) { // remove users from list that exceed max login time
		    	iterator.remove();
				Utils.debug("removed user "+user+" because of timeout !");
			}
		}
		
		return usersOnline;
		
//		Set<String> users = usersOnline.keySet();
//		for (String user : users) {
//			Utils.debug("online : "+user);
//			if(!userIsOnline(user)) { // remove users from list that exceed max login time
//				usersOnline.remove(user);
//				Utils.debug("removed user "+user+" because of timeout !");
//			}
//		}
		
	}

	public void setUsersOnline(HashMap<String, Long> usersOnline_) {
		usersOnline = usersOnline_;
	}

	public void login(String userId) {
		
		long time = new Date().getTime();
		usersOnline.put(userId, time);
		
	}
	
	public void logout(String userId) {
		//Utils.debug("UserLog logout:"+userId);
		usersOnline.remove(userId);
	}
	
	public Boolean userIsOnline(String userId) {
		
		long now = new Date().getTime();		
		if (!usersOnline.containsKey(userId)) return false;
		
		if (now - usersOnline.get(userId) > loginLimit) {
			return false;
		}
		else {
			return true;
		}
	}
	
	
}
