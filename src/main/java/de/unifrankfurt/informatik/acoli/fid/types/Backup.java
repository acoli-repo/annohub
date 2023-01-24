package de.unifrankfurt.informatik.acoli.fid.types;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.unifrankfurt.informatik.acoli.fid.exec.Executer;
import de.unifrankfurt.informatik.acoli.fid.util.ScriptUtils;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;

/**
 * @author frank
 *
 */
public class Backup {
	
	private Date date;
	private String name = ""; // is folder that keeps DB images
	private Integer sizeInMB=0;
	private String versionGremlin="";
	private String versionDBReg="";
	private String versionDBData="";
	private String comment="";
	private List<String> checksumList = new ArrayList<String>();
	private String backupRootDirectory = Executer.getFidConfig().getString("Backup.directory");


	
	public Backup() {
		this.date = new Date();
    }


	public Backup(String name){
		this.name = name;
		this.date = new Date();
	}
	
	public Date getDate() {
		return date;
	}
	
	public void setDate(Date date) {
		this.date = date;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Integer getSizeInMB() {
		return sizeInMB;
	}
	
	public void setSizeInMB(Integer sizeInMB) {
		this.sizeInMB = sizeInMB;
	}

	public String getVersionGremlin() {
		return versionGremlin;
	}

	public void setVersionGremlin(String versionGremlin) {
		this.versionGremlin = versionGremlin;
	}

	public String getVersionDBReg() {
		return versionDBReg;
	}

	public void setVersionDBReg(String versionDBReg) {
		this.versionDBReg = versionDBReg;
	}

	public String getVersionDBData() {
		return versionDBData;
	}

	public void setVersionDBData(String versionDBData) {
		this.versionDBData = versionDBData;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}
	
	
	/**
	 * Read json file with backups
	 * @param jsonFile
	 * @return list with backup objects
	 */
	public static List<Backup> readBackups(File jsonFile) {
		
		System.out.println("\n\nReading backups from file : "+jsonFile.getAbsolutePath());
		
		try {
			
		    ObjectMapper mapper = new ObjectMapper();
		    List<Backup> backups = Arrays.asList(mapper.readValue(jsonFile, Backup[].class));
		    return backups;

		} catch (Exception e) {
		    e.printStackTrace();
		    return null;
		}	
	}
	
	
	/**
	 * Export backups to json file
	 * @param backupList users
	 * @param jsonFile output file
	 * @return true on success otherwise false
	 */
	public static Boolean saveBackupsToFile(List<Backup> backupList, File jsonFile) {
		
		Utils.debug("saveBackupsToFile");
		
		try {
			
			ObjectMapper mapper = new ObjectMapper();
			mapper.writeValue(jsonFile, backupList);
			Utils.debug("Writing backups to file "+jsonFile.getAbsoluteFile());
			return true;
			//ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
			//String json = ow.writeValueAsString(backupList);
			//System.out.println(json);
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	
	public Boolean addBackupRecord() {
		
		if (backupRecordExists()) return false;
		
		File jsonFile = new File(backupRootDirectory,"backups.json");
		List<Backup> backups = new ArrayList<Backup>(readBackups(jsonFile));
				
		createChecksums(new File(backupRootDirectory, this.getName()));
		backups.add(this);

		saveBackupsToFile(backups, jsonFile);
		return true;
	}
	
	
	public Boolean deleteBackupRecord() {
		
		Utils.debug("deleteBackup");
		
		if (!backupRecordExists()) {
			Utils.debug("Backup delete Error : record for Backup does not exist !");
			return false;
		}
		
// 		obsolete : physical already done in caller
//		if (!backupDirectoryExists()) {
//			Utils.debug("Backup delete Error : the backup directory does not exist - please check the file system !");
//			return false;
//		}
		
		File jsonFile = new File(backupRootDirectory,"backups.json");
		List<Backup> backups = new ArrayList<Backup>(readBackups(jsonFile));

	
		Utils.debug("Delete backup record : "+this.name);
		Iterator<Backup> iterator = backups.iterator();
		System.out.println("searching ...");
		boolean found = false;
		while (iterator.hasNext()) {
			Backup backup = iterator.next();
			
			if (backup.getName().equals(this.name)) {
				Utils.debug("remove backup "+backup.name);
				iterator.remove();
				Utils.debug("success");
				found=true;
				break;
			}
		}
		
		if (!found) {
			System.out.println("Error : could not find record for backup "+this.name);
			return false;
		}
		saveBackupsToFile(backups, jsonFile);
		return true;
	}
	
	
	public Boolean backupRecordExists() {
				
		List<Backup> backups = readBackups(new File(backupRootDirectory,"backups.json"));
		for (Backup b : backups) {
			if (b.getName().equals(this.name)) return true;
		}
		
		return false;
	}
	
	
	
	public Boolean backupDirectoryExists() {
		
		if (new File (backupRootDirectory, this.getName()).exists()) return true;
		else
		return false;
	}
	
	
	
	public Boolean createChecksums(File backupDirectory) {
		
		Utils.debug("createChecksums");
		List<String> fileChecksums = new ArrayList<String>();

		for (String name : backupDirectory.list()) {
			
			File file = new File(backupDirectory, name); 
			if (!file.isFile()) return false;						// subdirectories not allowed
			
			Map<String, String> hashes = ScriptUtils.computeMd5AndSha256(file.getAbsolutePath());
			fileChecksums.add(hashes.get("sha256"));
		}
		
		setChecksumList(fileChecksums);
		return true;
	}
	
	
	
	public static List<String> validateBackups(File backupFile) {
		
		List<Backup> backupList = readBackups(backupFile);
		ArrayList<String> errors = new ArrayList<String>();
		for (Backup backup : backupList) {
			String error = validateBackup(backup, new File (backupFile.getParent(), backup.getName()));
			if (!error.isEmpty()) {
				errors.add(error);
			}
		}
		
		return errors;
	}
	
	
	
	public static String validateBackup(Backup backup, File archivDirectory) {
		
		// backup directory does not exist
		if (!archivDirectory.exists() || !archivDirectory.isDirectory()) return "Error : The Backup directory '"
			+archivDirectory.getAbsolutePath()+"' does not exist !";
		
		// check sha256-checkums for all files in the archivDirectory
		List<String> fileChecksums = new ArrayList<String>();
		for (String name : archivDirectory.list()) {
			
			File file = new File(archivDirectory, name); 
			if (!file.isFile()) {
				Utils.debug("Error : subdirectories not allowed in Backup folder!");
				return "Error : subdirectories not allowed in Backup folder!";	// subdirectories not allowed
			}
			
			Map<String, String> hashes = ScriptUtils.computeMd5AndSha256(file.getAbsolutePath());
			fileChecksums.add(hashes.get("sha256"));
		}
		
		
//		Utils.debug("fileChecksums");
//		for (String x : fileChecksums) {
//			System.out.println(x);
//		}
//
//		Utils.debug(" backup.getChecksumList()");
//		for (String x : backup.getChecksumList()) {
//			System.out.println(x);
//		}

		if (fileChecksums.size() != backup.getChecksumList().size()) {
			Utils.debug("Error in Backup folder '"+backup.getName()+"' : file count has changed !");
			return "Error in Backup folder '"+backup.getName()+"' : file count has changed !";
		}
		
		// verify the sha256 checksum for every file in the backup directory  
		for (String fileChecksum : fileChecksums) {
			boolean found = false;
			for (String checksum : backup.getChecksumList()) {
				if (fileChecksum.equals(checksum)) found = true;
			}
			if (!found) {
				Utils.debug("Checksum Error in backup '"+backup.getName()+"' : The file with the sha256-checksum '"+fileChecksum+"' is not registered in the backup record !");
				return "Checksum Error in backup '"+backup.getName()+"' : The file with the sha256-checksum '"+fileChecksum+"' is not registered in the backup record !";
			}
		}
		return "";
	}
	
	

	public static void main (String[] args) {
		
		Backup backup = new Backup("test");
		backup.setComment("comment");
		backup.setSizeInMB(10000);
		backup.setVersionDBData("versionDBData");
		backup.setVersionDBReg("versionDBReg");
		backup.setVersionGremlin("versionGremlin");
		
		Backup backup2 = new Backup("test");
		backup2.setComment("comment");
		backup2.setSizeInMB(10000);
		backup2.setVersionDBData("versionDBData");
		backup2.setVersionDBReg("versionDBReg");
		backup2.setVersionGremlin("versionGremlin");
		
		List<Backup> backupList = new ArrayList<Backup>();
		backupList.add(backup);
		backupList.add(backup2);
		
		File file = new File("/tmp/backups.json");
		Backup.saveBackupsToFile(backupList, file);
		
		backupList = readBackups(file);
		for (Backup b : backupList) {
			System.out.println(b.name);
			System.out.println(b.date);
			System.out.println(b.comment);
			System.out.println(b.sizeInMB);
			System.out.println(b.versionDBData);
			System.out.println(b.versionDBReg);
			System.out.println();
		}
	}


	public List<String> getChecksumList() {
		return checksumList;
	}


	public void setChecksumList(List<String> checksumList) {
		this.checksumList = checksumList;
	}

}
