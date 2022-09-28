package de.unifrankfurt.informatik.acoli.fid.types;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

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
		
		try {
			
			ObjectMapper mapper = new ObjectMapper();
			mapper.writeValue(jsonFile, backupList);
			return true;
			//ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
			//String json = ow.writeValueAsString(backupList);
			//System.out.println(json);
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
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

}
