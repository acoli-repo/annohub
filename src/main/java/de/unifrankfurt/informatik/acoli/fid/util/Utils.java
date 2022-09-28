package de.unifrankfurt.informatik.acoli.fid.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import de.unifrankfurt.informatik.acoli.fid.exec.Executer;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;

public class Utils {
	

	/**
	 * Write string to file. If file not exists it will be created.
	 * @param file
	 * @param fileContent
	 * @return success
	 */
	public static boolean writeFile(File file, String fileContent) {
		try {
			file.createNewFile();	
			FileWriter writer = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(writer);
            bw.write(fileContent);
            bw.close();
            System.out.println("Saving "+file.getAbsolutePath()+" finished successfull !");
            return true;
            
    } catch (IOException e) {
        	Utils.debug("File : "+file.getAbsolutePath());
            System.err.format("IOException: %s%n", e);
	} catch (Exception e) {
		e.printStackTrace();
	}
		return false;
	}
	
	

	/**
	 * Encodes a string with sha256 hash function. If the function is not supported on a system, the vanilla
	 * java <code>hashCode()</code> function is used.
	 * @param stringToHash the String that should be hashed
	 * @return the hash Value in a string representation
	 */
	 public static String sha256(String stringToHash) {
	 	String hashedString;
	 	try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hash = md.digest(stringToHash.getBytes(StandardCharsets.UTF_8));
			hashedString = Base64.getEncoder().encodeToString(hash);
		} catch (NoSuchAlgorithmException e) {
	 		int hash = stringToHash.hashCode();
	 		hashedString = Integer.toString(hash);
		 }
	 	return hashedString;
	 }
	 
	 
	 
	public static String convertStringToHex(String str) {
	
	        // display in uppercase
	        //char[] chars = Hex.encodeHex(str.getBytes(StandardCharsets.UTF_8), false);
	
	        // display in lowercase, default
	        char[] chars = Hex.encodeHex(str.getBytes(StandardCharsets.UTF_8));
	
	        return String.valueOf(chars);
	}
	

	
    public static String convertHexToString(String hex) {

        String result = "";
        try {
            byte[] bytes = Hex.decodeHex(hex.toCharArray());
            result = new String(bytes, StandardCharsets.UTF_8);
        } catch (DecoderException e) {
            throw new IllegalArgumentException("Invalid Hex format!");
        }
        return result;
    }
    
	 
	 /**
		 * If the string contains "n/a" then return the empty string.
		 * @param s
		 * @param filterText
		 * @return filtered string
		 */
	public static String filterNa(String s) {
			if (s.toLowerCase().contains("n/a")) return "";
			else
			return s;
	}
	
	
	public static void debug(Object s) {
		if (Executer.getFidConfig().getBoolean("RunParameter.debugOutput") == true) {
			System.out.println(s);
		}
	}
	
	public static void debugNor(Object s) {
		if (Executer.getFidConfig().getBoolean("RunParameter.debugOutput") == true) {
			System.out.print(s);
		}
	}
	
	
	public static boolean writeQueuedResources2File(List<ResourceInfo> queuedResources, File destinationFile) {
		
		try(
			    FileOutputStream fout = new FileOutputStream(destinationFile , false);
			    ObjectOutputStream oos = new ObjectOutputStream(fout);
			){
			    oos.writeObject(queuedResources);
			    Utils.debug("Queue backuped to file "+destinationFile.getAbsolutePath()+" successfully !");
			} catch (Exception ex) {
			    ex.printStackTrace();
			    return false;
			}
		return true;
	}
	
	
	public static List<ResourceInfo> readQueuedResourcesFromFile(File serializationFile) {

        List<ResourceInfo> queuedResources = null;
 
        try {
            // reading binary data
        	FileInputStream fis = new FileInputStream(serializationFile);
 
            // converting binary-data to java-object
        	ObjectInputStream ois = new ObjectInputStream(fis);
 
            // reading object's value and casting ArrayList<String>
            queuedResources = (List<ResourceInfo>) ois.readObject();
            
            ois.close();
        } 
        catch (FileNotFoundException fnfex) {
            fnfex.printStackTrace();
        }
        catch (IOException ioex) {
            ioex.printStackTrace();
        } 
        catch (ClassNotFoundException ccex) {
            ccex.printStackTrace();
        }
        
		return queuedResources;
	}
	
	
	
	public static void main(String[] args) {
	
		/*String hex = convertStringToHex("'hello world';,$&2320");
		System.out.println(hex);
		System.out.println(convertHexToString(hex));*/
		
		ArrayList<ResourceInfo> x = new ArrayList<ResourceInfo>();
		int counter = 1;
		while (counter <= 1000) {
		ResourceInfo y = new ResourceInfo();
			y.setUserID("id"+counter);
			y.setDataURL("http://www.domain"+counter+".com");
			x.add(y);
			counter++;
		}
		

		File outputFile = new File("/home/debian7/Arbeitsfläche/output.ser");
		writeQueuedResources2File(x, outputFile);
		
		List<ResourceInfo> verify = readQueuedResourcesFromFile(outputFile);
		counter = 1;
		for (ResourceInfo rs : verify) {
			System.out.println(counter++);
			System.out.println(rs.getDataURL());
			System.out.println(rs.getUserID());
		}
		
	}
	
}
