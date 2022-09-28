package de.unifrankfurt.informatik.acoli.fid.types;

/**
 * @author frank
 *
 */
public class UserQuota {

	private int maxResourceUploads = 0;
	private int maxResourceFiles = 0;
	private int maxResourceUploadSize = 0; // in MB
	
	
	public UserQuota(){
		super();
	};
	
	public UserQuota (int maxResourceUploads, int maxResourceFiles, int maxResourceUploadSize) {
		this.maxResourceUploads = maxResourceUploads;
		this.maxResourceFiles = maxResourceFiles;
		this.maxResourceUploadSize = maxResourceUploadSize;
	}
	
	public int getMaxResourceUploads() {
		return maxResourceUploads;
	}
	public void setMaxResourceUploads(int maxResourceUploads) {
		this.maxResourceUploads = maxResourceUploads;
	}
	public int getMaxResourceFiles() {
		return maxResourceFiles;
	}
	public void setMaxResourceFiles(int maxResourceFiles) {
		this.maxResourceFiles = maxResourceFiles;
	}
	public int getMaxResourceUploadSize() {
		return maxResourceUploadSize;
	}
	public void setMaxResourceUploadSize(int maxResourceUploadSize) {
		this.maxResourceUploadSize = maxResourceUploadSize;
	}


	public void update(AccountType accountType) {
		
		if (accountType == AccountType.ADMIN) {
			maxResourceUploads = 100000;
			maxResourceFiles = 1000000;
			maxResourceUploadSize=15000;
		}
	}
	
}
