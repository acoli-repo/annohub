package de.unifrankfurt.informatik.acoli.fid.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

/*
 * Get resources
 */
public class LocateUtils implements Serializable {
	
	
	private static final long serialVersionUID = 8630090259508887954L;


	public static Path getRelFilePath(File file, File downloadDir) {
		
		Path temp = Paths.get(file.getAbsolutePath());
		Path temp2 = Paths.get(downloadDir.getAbsolutePath());
		Path relPath = temp2.relativize(temp);
		Utils.debug("relpath : "+relPath.toString());
		return relPath;
	}
	
	
	public File getLocalFile(String fileName) {

		return stream2File(this.getClass().getResourceAsStream(fileName), fileName);
		// Update !
		//return ClassLoader.getSystemClassLoader().getResourceAsStream(fileName), fileName);
	}
	
	

	
	
	public File getLocalDirectory(String dirName) {
				
		return new File(this.getClass().getResource(dirName).getPath());
	}
	
	
	public List<File> getLocalDirectoryFileList(String directory) {

		List <File> files = new ArrayList<File>();
		
		File folder = getLocalDirectory(directory);

		for (File file : folder.listFiles()) {
			if (!file.isDirectory()) {
				files.add(file);
			}
		}
		return files;
	}
	
	
	
	
	public List<String> getJarFolderFileList(String directory) {
		
	if (!directory.endsWith("/")) directory = directory+"/";
		
	CodeSource src = this.getClass().getProtectionDomain().getCodeSource();
	List<String> list = new ArrayList<String>();

	if( src != null ) {
	    URL jar = src.getLocation();
	    	 
	    ZipInputStream zip;
		try {
			zip = new ZipInputStream( jar.openStream());
		
	    ZipEntry ze = null;

	    while((ze = zip.getNextEntry()) != null) {
	        String entryName = ze.getName();
	        
	        //System.out.println(entryName);
	        if(entryName.contains(directory) && !ze.isDirectory()) {
	            list.add(new File(entryName).getName());
	            //System.out.println(new File(entryName).getName());
	        }
	    }
		} catch (IOException e) {
			e.printStackTrace();
		}
	  }
		return list;
	}
	
	
	public static File stream2File (InputStream in, String fileName) {
  
	   String ext = LocateUtils.getFileExtensions(fileName);
	  
	   try {
		   
		   File tempFile = File.createTempFile(fileName, ext);
		   tempFile.deleteOnExit();
		   
		   FileOutputStream out = new FileOutputStream(tempFile);
	   
	       IOUtils.copy(in, out);
	       
	       return tempFile;
	   } catch (Exception e) {
		   e.printStackTrace();
	   }
	  
	return null;
	
	}
	
	
	public static String getFileExtensions(String fileName) {
		
		String ext = "";
		
		while (!fileName.equals(FilenameUtils.getBaseName(fileName))) {
			ext ="."+FilenameUtils.getExtension(fileName)+ext;
			fileName = FilenameUtils.getBaseName(fileName);
		}
		
		return ext;
	}
	
	
	// working !
	public static File getResourceAsFile(String resourcePath) {
	    try {
	        InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(resourcePath);
	        if (in == null) {
	            return null;
	        }

	        File tempFile = File.createTempFile(String.valueOf(in.hashCode()), ".tmp");
	        tempFile.deleteOnExit();

	        try (FileOutputStream out = new FileOutputStream(tempFile)) {
	            //copy stream
	            byte[] buffer = new byte[1024];
	            int bytesRead;
	            while ((bytesRead = in.read(buffer)) != -1) {
	                out.write(buffer, 0, bytesRead);
	            }
	        }
	        return tempFile;
	    } catch (IOException e) {
	        e.printStackTrace();
	        return null;
	    }
	}
	
	   
}
