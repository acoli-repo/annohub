package de.unifrankfurt.informatik.acoli.fid.resourceDB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.unifrankfurt.informatik.acoli.fid.types.AccountType;
import de.unifrankfurt.informatik.acoli.fid.types.UserAccount;
import de.unifrankfurt.informatik.acoli.fid.types.UserQuota;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;

/**
 * @author frank
 *
 */
public class UserManagement {
	
	private ResourceManager resourceManager = null;
	private XMLConfiguration config;
	private final String usernameRule = "User creation error: username is at least 6 characters long with only alpha-numeric characters and at leat 1 upper-case and 1 lower-case letter !";
	private final String passwordRule = "Password creation error: password is at least 6 characters long with at least 1 number, 1 upper-case, 1 lower-case and 1 non-alpha-numeric character !";

	
	public UserManagement(ResourceManager resourceManager, XMLConfiguration config) {
		this.resourceManager = resourceManager;
		this.config = config;
	}
	
	public void update() {
		
	}
	
	public long getAdminUserCount() {
		return resourceManager.getUserCount(AccountType.ADMIN);
	}
	
	public long getMemberUserCount() {
		return resourceManager.getUserCount(AccountType.MEMBER);
	}
	
	public long getGuestUserCount() {
		return resourceManager.getUserCount(AccountType.GUEST);
	}
	
	public long getRetiredUserCount() {
		return resourceManager.getUserCount(AccountType.RETIRED);
	}
	
	public int getAllUserCount() {
		return getAllUserLogins().size();
	}
	
	public List<String> getAllUserLogins() {
		
		/*ArrayList<String> users = new ArrayList<String>();
    	users.add("user23452");
    	users.add("user252252");
    	users.add("very long user");*/
    	
		return resourceManager.getAllUserLogins();
	}
	
	
	public boolean updateUserAccount(UserAccount userAccount, Boolean createNewUser) {
		
		if(!createNewUser) {
			
			if (userAccount.getUserID().equals("ub_admin")) {
				userAccount.setAccountType(AccountType.ADMIN);
			}
			
			// update existing user
			if (resourceManager.updateUser(userAccount) != null) {
				return true;
			} else {
				return false;
			}	
		} else {
			
			// double check user exists
			if (resourceManager.userExists(userAccount.getUserID())) return false;
			
			// create new user
			if (resourceManager.addUser(userAccount) != null) {
				return true;
			} else {
				return false;
			}
		}
		
	}
	
	public UserAccount getUserAccount(String login) {
		
		return resourceManager.getUserAccount(login);
		//return new UserAccount("testlogin", "testpassword", "testemail");
	}
	
	
	
	public String deleteUserAccount(UserAccount userAccount) {
		
		
		// check user exists
		if (!resourceManager.userExists(userAccount.getUserID())) {
			return "User delete error : user '"+userAccount.getUserID()+"' does not exist !";
		}
		
		// can not delete admin user
		if (userAccount.getAccountType() == AccountType.ADMIN) {
			return "User delete error : Can not delete ADMIN user !";
		}
		
		
		// can not delete user ub_admin
		if (userAccount.getUserID().equals("ub_admin")) {
			return "User delete error : Can not delete ub ADMIN !";
		}
		
		// get all resources owned by user
		ArrayList<String> userResources = resourceManager.getResourcesOwnedByUserAsUrl(userAccount.getUserID());
		
		// delete user resources
		for (String url : userResources) {
			Utils.debug("Deleting resource : "+ url);
			resourceManager.deleteResource(url);
		}
		
		// delete user
		if(resourceManager.deleteUser(userAccount)) {
			return "";
		} else {
			return "Unknown user delete error !";
		}
		
	}
	
	
	/**
	 * Check password
	 * @param passwd
	 * @return if passwd empty return null, else on password error return err msg, else (password ok) return empty string  
	 */
	public String checkPassword(String passwd) {
		
		if (passwd.isEmpty()) return "";
		if (passwd.contains("'") || passwd.contains(" ") || passwd.contains("\\")) return "Password error : characters <space>, ' and \\ not allowed in password !";
		if (passwd.length() < 6) return passwordRule;
		if (!passwd.matches(".*\\d.*")) return passwordRule;
		if (!passwd.matches(".*[A-Z].*")) return passwordRule;
		if (!passwd.matches(".*[a-z].*")) return passwordRule;
		if (!passwd.matches(".*[^A-Za-z0-9].*")) return passwordRule;
		if (passwd.contains("guest")) return "Password error : password can not have infix 'guest' !";
		if (passwd.contains("admin")) return "Password error : password can not have infix 'admin' !";


		return "";
	}

	/**
	 * @return
	 */
	public String checkLogin(UserAccount userAccount) {
		
		String userLogin = userAccount.getUserID();
		
		if (!userLogin.matches("[A-Za-z0-9]+")) return usernameRule;
		if (!userLogin.matches(".*[A-Z].*")) return usernameRule;
		if (!userLogin.matches(".*[a-z].*")) return usernameRule;

		// check login starts with guest
		if (userAccount.getUserID().trim().length() < 6){
				return usernameRule;
		};
		
		// check login has reserved names
		if (userAccount.getUserID().trim().toLowerCase().contains("guest") ||
			userAccount.getUserID().trim().toLowerCase().contains("admin") ||
			userAccount.getUserID().trim().toLowerCase().startsWith("ub")
			){
				return "User creation error : prefix 'guest', 'admin' or user 'ub' in login not allowed !";
		};
		
		// check login already exist
		if (resourceManager.userExists(userAccount.getUserID())){
			return "User creation error : user '"+userAccount.getUserID()+"' already exists !";
		};
		
		
		// check 4 different characters
		HashSet<Character> diffSet = new HashSet<Character>();
		int i = 0;
		while (i < userLogin.length()) {
			diffSet.add(Character.toLowerCase(userLogin.charAt(i)));
			i++;
		}
		
		
		if (diffSet.size() < 4) {
			return "User creation error : user login must contain at least 4 different characters !";
		}
		
		return "";
	}
	
	
    public UserQuota getDefaultUserQuotas(AccountType accountType) {
    	
    	int maxResourceUploadSize = config.getInt("Quotas.maxResourceUploadSize");
 	    int maxResourceFiles=config.getInt("Quotas.maxResourceFiles");
 	    int maxResourceUploads=config.getInt("Quotas.maxResourceUploads");
 	    
 	    switch (accountType) {
 	    
 	    case GUEST :
 	 	    return new UserQuota(0, 0, 0);
 	    
 	    case MEMBER : 	    	
 	 	    return new UserQuota(maxResourceUploads, maxResourceFiles, maxResourceUploadSize);
 	    
 	    case ADMIN :
 	 	    return new UserQuota(100000, 1000000, 15000);
 	    
 	    case RETIRED :
 	    	return new UserQuota(0, 0, 0);
 	    
 	    default :
 	    	return null;
 	    }
 	    
    }
}
