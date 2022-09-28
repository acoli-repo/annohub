package de.unifrankfurt.informatik.acoli.fid.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;
import net.sf.sevenzipjbinding.simple.impl.SimpleInArchiveImpl;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.io.FileUtils;

import riotcmd.riot;
import de.unifrankfurt.informatik.acoli.fid.detector.TikaTools;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.RiotValidationLogger;

public class ScriptUtils {
	
	
	public static final int VALIDATION_ERROR = 1;
	public static final int VALIDATION_SUCCESS = 0;


	/**
	 * Unpack archive file with 7z tool
	 * 
	 * @param filePath
	 * @param pathTo7zOr7zaCompressionUtility
	 * @return file count of archive
	 */
	public static long unpack7z(String filePath, String pathTo7zOr7zaCompressionUtility) {
		
		System.out.println("unpacking ...");
		HashSet <String> temp = new HashSet <String> ();
		temp.add(filePath);
		HashSet <String> tarType = IndexUtils.filterTarNgz(temp);
		long filesInArchive = 0;
		
	try {
		 File tmp = new File(filePath);
		 String shellCommand;
		 if (tarType.isEmpty() && !TikaTools.detectFileType(new File(filePath)).equals("application/x-tar")) {
		 //if (tarType.isEmpty() && isTarArchive(filePath, decompressUtility) == false) {
			 shellCommand = pathTo7zOr7zaCompressionUtility+" x -y -o"+tmp.getParent()+" "+filePath;
		 } else {
			 shellCommand = "tar -xf "+filePath +" --directory "+tmp.getParent();
		 }
		 System.out.println(shellCommand);
		 
		 Process p = Runtime.getRuntime().exec(shellCommand);
		 System.out.println("waiting");
		  
 		 // Shell output
		 BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		 
		 String line = "";
		 while ((line = reader.readLine())!= null) {
		  	   System.out.println(line);
		  	   // parse file count of archive (Files: 3)
		  	   if (line.startsWith("Files")) {
		  		   try {
		  			   filesInArchive = Long.parseLong(line.split(":")[1].trim());
		  		   } catch (Exception e) {}
		  	   }
		 }
		 System.out.println("finished !");
		 
		 // Delete compressed file
		 tmp.delete();
		 
		 
		 /*int seconds = 0;
		 int max = 20;
		 // p.waitFor() may not terminate !
		 while(p.isAlive()) {
			 Thread.sleep(1000);
			 if (seconds++ == max) {
				 System.out.println("Timeout reached for unpacking : probably subprocess did not terminate - assuming decompression is finished !");
				 break;
			 }
		 }
		 
		  
		 System.out.println("proceeding");
		 
		 // Delete compressed file
		 tmp.delete();
		 
		 
		 // Shell output
		 BufferedReader reader =
	             new BufferedReader(new InputStreamReader(p.getInputStream()));
		   
	     String line = "";
	     while ((line = reader.readLine())!= null) {
	  	   System.out.println(line);
	     	}*/
	     
		
	} catch (Exception e) {}
	
	return filesInArchive;
	}
	

	
	
	
	public static Long getUncompressedArchiveSize7zB(File file) {
		
		Long totalSize = 0L;
		
		try {
			RandomAccessFile randomAccessFile = new RandomAccessFile(file.getAbsolutePath(), "r");
			
			IInArchive inArchive = SevenZip.openInArchive(null, // Choose format automatically
		            new RandomAccessFileInStream(randomAccessFile));
			
			// Using inArchive.getProperty(PropID.SIZE) unfortunately returns null !
			// (getProperty(PropID propId) not implemented in InArchiveImpl class)
			
			// Therefore sum original size of individual size
			ISimpleInArchive simple = new SimpleInArchiveImpl(inArchive);
			for (ISimpleInArchiveItem x : simple.getArchiveItems()) {
				try {
					Utils.debug("archive file : "+x.getPath());
					Utils.debug("archive size : "+x.getPackedSize());
				totalSize += x.getSize();
				} catch (Exception e) {}
			}
		}
		// Archive type could not be detected -> return size of file
		catch (net.sf.sevenzipjbinding.SevenZipException noArchive) {
			
			return FileUtils.sizeOf(file);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Archive bytes : "+totalSize);
		return totalSize;
	}
	
	/**
	 * Compute MD5 and SHA256 hashes
	 * @param filePath
	 * @return Map with hashes
	 */
	public static Map<String, String> computeMd5AndSha256(String filePath) {
		
		Map<String, String> result = new HashMap<String, String>();
		System.out.println("get md5 for "+filePath+" ...");
		
		try {
			
			 String shellCommand = "md5sum "+filePath;			
			 System.out.println(shellCommand);
			 
			 Process p = Runtime.getRuntime().exec(shellCommand);
			 System.out.println("waiting");
			  
	 		 // Shell output
			 BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			 
			 String line = "";
			 while ((line = reader.readLine())!= null) {
			  	   System.out.println(line);
			  	   // parse file count of archive (Files: 3)
			  	   String[] split = line.split("\\s+");
			  	   if (split.length == 2) {
			  		   result.put("md5", split[0]);
			  	   } else {
			  		   result.put("md5", "");
			  	   }
			 }
			 System.out.println("finished MD5 !");
			 
			
			 shellCommand = "sha256sum "+filePath;			
			 System.out.println(shellCommand);
			 
			 p = Runtime.getRuntime().exec(shellCommand);
			 System.out.println("waiting");
			  
	 		 // Shell output
			 reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			 
			 line = "";
			 while ((line = reader.readLine())!= null) {
			  	   System.out.println(line);
			  	   // parse file count of archive (Files: 3)
			  	   String[] split = line.split("\\s+");
			  	   if (split.length == 2) {
			  		   result.put("sha256", split[0]);
			  	   } else {
			  		   result.put("sha256", "");
			  	   }
			 }
			 System.out.println("finished SHA256 !");
			 
		} catch (Exception e) {e.printStackTrace();}
	
		return result;
	}
	
	
	
	/**
	 * Export files from SVN
	 */
	public static void exportSVNFiles(HashSet<String> svnFiles, String targetDirectory) {
		
		System.out.println("exporting SVN files ...");
		String shellCommand="";
		
	try {
		 for (String svnFile : svnFiles) {
			 
			 shellCommand = "svn export --force "+svnFile+"  "+targetDirectory;
			 System.out.println(shellCommand);
			 
			 Process p = Runtime.getRuntime().exec(shellCommand);
			 System.out.println("waiting");
			  
	 		 // Shell output
			 BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			 
			 String line = "";
			 while ((line = reader.readLine())!= null) {
			  	   System.out.println(line);
			 }
			 System.out.println("finished !");
		 }
		 
		 } catch (Exception e) {e.printStackTrace();}
	}
	
	
	/**
	 * A common problem of parsing RDF files with Jena are invalid IRIs caused by
	 * literal utf characters (e.g. \\u00E). These will force the parsing process to stop
	 * immediately. As a solution for some of these errors :
	 * Validate an RDF file with the Jena riot cmdline tool. If the file has ERRORs
	 * then convert it with the rapper utility to RDFXML format. This will convert literal utf
	 * characters automatically. The shell script requires the riot command-line tool that
	 * comes with Apache Jena and also the rapper RDF utility.
	 *
	 * @param resourceInfo File to be validated
	 * @param fidConfig Configuration file
	 * @param scriptFile
	 * @return error=1, noerror=0
	 */	
	public static int validateRdfFile (ResourceInfo resourceInfo, XMLConfiguration fidConfig, File scriptFile) {
		
		System.out.print("Starting RDF validation ...");
		
		try {
			
			RiotValidationLogger riotErrorLogger = new RiotValidationLogger();
			org.apache.jena.riot.system.ErrorHandlerFactory.setDefaultErrorHandler(riotErrorLogger);
			riot.main("--validate", resourceInfo.getFileInfo().getResourceFile().getAbsolutePath());
			if (!riotErrorLogger.validationHasFailed()) {
				
				System.out.println(" O.K.");
				return VALIDATION_SUCCESS;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		

		// RDF file had ERRORS and was converted to rdfxml format !
		System.out.println(resourceInfo.getFileInfo().getResourceFile().getAbsolutePath());
	   	System.out.println("... RDF file validation failed !");
	   	System.out.println("-> file will be converted to RdfXml format !");
		
		// Run rapper utility to convert RDF to rdfXML (will work afterwards)
		String shellCommand = 
				"bash '"
				+scriptFile.getAbsolutePath()
				+"' '"+resourceInfo.getFileInfo().getResourceFile().getAbsolutePath()
				+"'";
		
		 /* optional 2nd argument can specify rapper input encoding parameter (-i option)
		  * if ommitted then -i guess is used !
		 # rapper input formats
		 # -i FORMAT, --input FORMAT   Set the input format/parser to one of:
		 #    rdfxml          RDF/XML (default)
		 #    ntriples        N-Triples
		 #    turtle          Turtle Terse RDF Triple Language
		 #    trig            TriG - Turtle with Named Graphs
		 #    rss-tag-soup    RSS Tag Soup
		 #    grddl           Gleaning Resource Descriptions from Dialects of Languages
		 #    guess           Pick the parser to use using content type and URI
		 #    rdfa            RDF/A via librdfa
		 #    json            RDF/JSON (either Triples or Resource-Centric)
		 #    nquads          N-Quads
		 */
		 
		 	     
        CommandLine oCmdLine = CommandLine.parse(shellCommand);
        DefaultExecutor oDefaultExecutor = new DefaultExecutor();
      	try {
			oDefaultExecutor.execute(oCmdLine);
		} catch (IOException e) {
			e.printStackTrace();
		}
      	
	   	// Change source file in resourceInfo file.y -> file.y.rdf
	   	resourceInfo.getFileInfo().
	   		setTemporaryFilePath(resourceInfo.getFileInfo().getResourceFile().getAbsolutePath()+".rdf");
	   	 
	   	return VALIDATION_SUCCESS;
	}
	
	
	
public static String tarDirectory (File sourceDir, File targetFile_) {
		
		LocateUtils locateUtils = new LocateUtils();
		String scriptFile = locateUtils.getLocalFile("/bash/tar").getAbsolutePath();
		String sourceDirParent = sourceDir.getParent();
		String sourceDirName = sourceDir.getName();
		String targetDir = targetFile_.getParent();
		String targetFile = targetFile_.getAbsolutePath()+".tar.gz";

		System.out.println("tarDirectory ...");
		
		String shellCommand = "bash '"+scriptFile+"' '"+sourceDirParent+"' '"+sourceDirName+"' '"+targetDir+"' '"+targetFile+"'";
		
		System.out.println(shellCommand);
		
        CommandLine oCmdLine = CommandLine.parse(shellCommand);
        DefaultExecutor oDefaultExecutor = new DefaultExecutor();
      	try {
			oDefaultExecutor.execute(oCmdLine);
		} catch (IOException e) {
			e.printStackTrace();
			return e.getMessage();
		}
	   	 
	   	return "";
	}

	
	
	
	
	/**
	 * Unpack archive file in tar.gz format
	 * 
	 * @param archiveFile
	 * @param targetDirectory
	 */
	public static void untarArchive(File archiveFile, File targetDirectory) {
		
		System.out.println("unpacking ... "+archiveFile.getAbsolutePath());
		System.out.println("to folder ... "+targetDirectory);
		
	try {
		
		 String shellCommand = "tar xf "+archiveFile+" -C "+targetDirectory;
		 
		 System.out.println(shellCommand);
		 
		 Process p = Runtime.getRuntime().exec(shellCommand);
		 System.out.println("waiting");
		  
 		 // Shell output
		 BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		 
		 String line = "";
		 while ((line = reader.readLine())!= null) {
		  	   System.out.println(line);
		 }
		 System.out.println("finished !"); 
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	public static String gremlinServer (String serverCommand, XMLConfiguration config) {
		
		HashMap<String,String> env = new HashMap<String, String>();

		// RunParameter.JavaHome
		env.put("JAVA_HOME", config.getString("RunParameter.JavaHome"));
		// Databases.GremlinServer.home
		env.put("GREMLIN_HOME", config.getString("Databases.GremlinServer.home"));
		// Databases.GremlinServer.conf
		env.put("GREMLIN_YAML", config.getString("Databases.GremlinServer.conf"));
		
		LocateUtils locateUtils = new LocateUtils();
		File scriptFile = locateUtils.getLocalFile("/bash/gremlin-server");

		System.out.println(serverCommand+" GremlinServer ...");
		
		String shellCommand = "bash '"+scriptFile.getAbsolutePath()+"' '"+env.get("GREMLIN_HOME")+"' "+serverCommand;
		
		//System.out.println(shellCommand);
		
        CommandLine oCmdLine = CommandLine.parse(shellCommand);
        DefaultExecutor oDefaultExecutor = new DefaultExecutor();
      	try {
			oDefaultExecutor.execute(oCmdLine, env);
		} catch (IOException e) {
			e.printStackTrace();
			return e.getMessage();
		}
	   	 
	   	return "";
	}
	
	
	
	
	
	public static void main(String[] args) {
		
		String file = "/media/EXTRA/ud-treebanks-v2.7.tgz";
		Map<String, String> result = computeMd5AndSha256(file);
		
		for (String key : result.keySet()) {
			System.out.println(key+" : "+result.get(key));
		}
		
//		XMLConfiguration config = Run.loadFIDConfig();
//		
//		HashMap<String,String> env = new HashMap<String, String>();
//
//		env.put("JAVA_HOME", config.getString("RunParameter.JavaHome"));
//		// Databases.GremlinServer.home
//		env.put("GREMLIN_HOME", config.getString("Databases.GremlinServer.home"));
//		// Databases.GremlinServer.conf
//		env.put("GREMLIN_YAML", config.getString("Databases.GremlinServer.conf"));
//		//File startScript = new File("/media/Cloud/fabromeit/workspace/search/src/main/resources/bash/gremlin-server");		
//		
//		LocateUtils locateUtils = new LocateUtils();
//		File startScript = locateUtils.getLocalFile("/bash/gremlin-server");
//		gremlinServer("stop", config);
		
	}
	
}
