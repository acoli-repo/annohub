package de.unifrankfurt.informatik.acoli.fid.types;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * @author frank
 *
 */
public class UserAccount {
	
	private String userID="";
	private String userPassword="";
	private String userEmail="";
	private AccountType accountType = AccountType.GUEST;
	private UserQuota quotas = new UserQuota();
	
	
	
	public UserAccount() {
        super();
    }

	/**
	 * 
	 * @param userID
	 * @param userPassword
	 * @param userEmail
	 */
	public UserAccount(String userID, String userPassword, String userEmail){
		this.userID = userID;
		this.userPassword = userPassword;
		this.userEmail = userEmail;
	}
	
	
	public String getUserID() {
		return userID;
	}
	public void setUserID(String userID) {
		this.userID = userID;
	}
	public String getUserPassword() {
		return userPassword;
	}
	public void setUserPassword(String userPassword) {
		this.userPassword = userPassword;
	}
	public String getUserEmail() {
		return userEmail;
	}
	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}
	public AccountType getAccountType() {
		return accountType;
	}
	@JsonIgnore
	public String getAccountTypeAsString() {
		return accountType.name().toLowerCase();
	}
	public void setAccountType(AccountType accountType) {
		this.accountType = accountType;
		quotas.update(accountType);
	}


	public UserQuota getQuotas() {
		return quotas;
	}


	public void setQuotas(UserQuota quotas) {
		this.quotas = quotas;
	}
	
	
	/**
	 * Export users to json file
	 * @param userList users
	 * @param jsonFile output file
	 * @return true on success otherwise false
	 */
	public static Boolean saveUsersToFile(List<UserAccount> userList, File jsonFile) {
		
		try {
			
			ObjectMapper mapper = new ObjectMapper();
			mapper.writeValue(jsonFile, userList);
			return true;
			//ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
			//String json = ow.writeValueAsString(backupList);
			//System.out.println(json);
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Read json file with users
	 * @param jsonFile
	 * @return list with user objects
	 */
	public static List<UserAccount> readUsers(File jsonFile) {
		
		System.out.println("\n\nReading users from file : "+jsonFile.getAbsolutePath());
		
		try {
			
		    ObjectMapper mapper = new ObjectMapper();
		    List<UserAccount> users = Arrays.asList(mapper.readValue(jsonFile, UserAccount[].class));
		    return users;

		} catch (Exception e) {
		    e.printStackTrace();
		    return null;
		}	
	}
	
	
public static void main (String[] args) {
		
		UserAccount user1 = new UserAccount("id1", "password1", "email1");
		UserQuota quotas1 = new UserQuota();
		quotas1.setMaxResourceFiles(1);
		quotas1.setMaxResourceUploads(2);
		quotas1.setMaxResourceUploadSize(3);
		user1.setQuotas(quotas1);
		
		UserAccount user2 = new UserAccount("id2", "password2", "email2");
		UserQuota quotas2 = new UserQuota();
		quotas2.setMaxResourceFiles(4);
		quotas2.setMaxResourceUploads(5);
		quotas2.setMaxResourceUploadSize(6);
		user2.setQuotas(quotas2);
		
		List<UserAccount> userList = new ArrayList<UserAccount>();
		userList.add(user1);
		userList.add(user2);
		
		File file = new File("/tmp/users.json");
		UserAccount.saveUsersToFile(userList, file);
		
		userList = readUsers(file);
		for (UserAccount b : userList) {
			System.out.println(b.userID);
			System.out.println(b.userPassword);
			System.out.println(b.userEmail);
			System.out.println(b.accountType);
			System.out.println(b.getQuotas().getMaxResourceFiles());
			System.out.println(b.getQuotas().getMaxResourceUploads());
			System.out.println(b.getQuotas().getMaxResourceUploadSize());
			System.out.println();
		}
	}


}
