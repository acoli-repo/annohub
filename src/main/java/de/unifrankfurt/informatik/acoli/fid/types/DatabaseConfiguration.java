package de.unifrankfurt.informatik.acoli.fid.types;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.commons.configuration2.XMLConfiguration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class DatabaseConfiguration {
	
	String name = "";
	DBType dbType;
	File databaseDirectory;				// only for persistent databases (e.g. Neo4j)
	File databaseImportJsonFile;
	File databaseExportJsonFile;
	UpdatePolicy updatePolicy;
	private String gremlinGraph;
	private String gremlinConfigYaml="";


	
	public DatabaseConfiguration (DBType dbType, File databaseDirectory, File jsonImportFile,File jsonExportFile, XMLConfiguration fidConfig) {
		this.dbType = dbType;
		this.databaseDirectory = databaseDirectory;
		this.databaseImportJsonFile = jsonImportFile;
		this.databaseExportJsonFile = jsonExportFile;
		this.updatePolicy = null;
		if (dbType == DBType.GremlinServer) readGremlinServerConfigFile(fidConfig);
	}
	


	public DBType getDbType() {
		return dbType;
	}


	public void setDbType(DBType dbType) {
		this.dbType = dbType;
	}


	public File getDatabaseDirectory() {
		return databaseDirectory;
	}


	public void setDatabaseDirectory(File databaseDirectory) {
		this.databaseDirectory = databaseDirectory;
	}


	public File getDatabaseExportJsonFile() {
		return databaseExportJsonFile;
	}


	public void setDatabaseExportJsonFile(File databaseJsonExportFile) {
		this.databaseExportJsonFile = databaseJsonExportFile;
	}
	
	
	public UpdatePolicy getUpdatePolicy() {
		return updatePolicy;
	}

	
	public void setUpdatePolicy(UpdatePolicy updatePolicy) {
		this.updatePolicy = updatePolicy;
	}
	
	
	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}
	
	public File getDatabaseImportJsonFile() {
		return databaseImportJsonFile;
	}


	public void setDatabaseImportJsonFile(File databaseImportJsonFile) {
		this.databaseImportJsonFile = databaseImportJsonFile;
	}
	
	
	private boolean readGremlinServerConfigFile(XMLConfiguration fidConfig) {
		
		if (fidConfig == null) {
			//System.out.println("Configuration file missing !");
			return false;
		}
		
		try {
			File gremlinServerHome = new File((String) fidConfig.getProperty("Databases.GremlinServer.home"));
			File gremlinServerConfigFile = new File((String) fidConfig.getProperty("Databases.GremlinServer.conf"));

		if (!gremlinServerConfigFile.exists() || !gremlinServerHome.exists()) return false;
		
			// open yaml file
		    final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		    JsonNode tree = mapper.readTree(gremlinServerConfigFile);
		    
		    int port = 8182;
		    try {
		    	port = tree.findValue("port").asInt();
		    } catch (Exception e){ // use standard port otherwise
		    }
	    	fidConfig.setProperty("Databases.GremlinServer.port", port);
		    
		    JsonNode result = tree.findValue("graphs");
		    if (result != null) {
		    	//System.out.println("found");
		    	//System.out.println(result.findValue("graph"));
		    	File graphConfigFile = new File(gremlinServerHome, result.findValue("graph").asText());
		    	System.out.println(graphConfigFile.getAbsolutePath());
		    	
		    	// open properties file
		    	Properties props = new Properties();
		    	props.load(new FileInputStream(graphConfigFile));
		    	this.databaseDirectory = new File(props.getProperty("gremlin.neo4j.directory"));
		    	this.gremlinGraph = props.getProperty("gremlin.graph");
		    	this.gremlinConfigYaml = gremlinServerConfigFile.getAbsolutePath();
		    }
			
		} catch (Exception e) {e.printStackTrace();}
		
		return false;
	}
	
	
	public void printConfiguration() {
		
		System.out.println(name+" DB : "+ dbType+" ");
		switch (dbType) {
		
			case TinkerGraph :
				System.out.println(" (in Memory) ");
				if (databaseExportJsonFile != null)
					System.out.println("from json file : "+databaseExportJsonFile.getAbsolutePath());
				break;
				
			case Neo4J :
				if (databaseDirectory != null)
					System.out.println(databaseDirectory.getAbsolutePath());
				if (databaseExportJsonFile != null)
					System.out.println("Backup to json file : "+databaseExportJsonFile.getAbsolutePath());
				break;
				
			case GremlinServer :
				if (!this.gremlinConfigYaml.isEmpty()) {
					System.out.println(gremlinConfigYaml);
				}
				if (gremlinGraph != null) {
					System.out.println(gremlinGraph);
				}
				if (databaseDirectory != null)
					System.out.println(databaseDirectory.getAbsolutePath());
				break;
				
			default :
				break;
		}
		
		System.out.println();
		
	}
	
}
