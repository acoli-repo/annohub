package de.unifrankfurt.informatik.acoli.fid.parser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.jayway.jsonpath.JsonPath;

import de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager;
import de.unifrankfurt.informatik.acoli.fid.types.MetadataSource;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;

public class JsonMetadataTools {
	
	
	/**
	 * Reader for Spraakbanken metadata local file
	 * @param file
	 * @return
	 */
	public static HashMap<String, String> readSpraakbankenJsonFile1(File file) {
		
		HashMap<String, String> result = new HashMap<String, String>();
    	JSONParser parser = new JSONParser();
		JSONArray a;
		
		try {
			
			a = (JSONArray) parser.parse(new FileReader(file));
		

		  for (Object o : a)
		  {
		    JSONObject rmd = (JSONObject) o;

		    String resource = (String) rmd.get("url");
		    resource = (new File(resource)).getName();
		    //Utils.debug(resource);

		    HashSet<String> parsedLinks = new HashSet<String>();
		    ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>) rmd.get("download_links + licenses");
		    
		    if (links.isEmpty()) continue;
		    for (ArrayList<String> link : links) {
			    result.put(link.get(0), resource);
		    }

		  }
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	
	
	
	/**
	 * Parsed JSON from spraakbanken website (used for patch restoreSpraakbanken data previously fetched with
	 * method readSpraakbankenJsonUrl)
	 * @param file Spraakbanken json file with the metadata
	 * @param resourceInfo Resource that needs metadata
	 */
	public static boolean readSpraakbankenJsonFile2(File file, ResourceInfo resourceInfo) {
		
		if (file == null) return false;
		
		// parsing JSON with https://github.com/json-path/JsonPath
		try {
			String jsonString = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
			int downloadCount = JsonPath.read(jsonString, "$.results.bindings.length()");
			int i = 0;
			
			Utils.debug("# "+downloadCount);
			
			
			String download="";
			String title="";
			String description="";
			String email= "";
			String rights="";
			
			boolean found = false;
			String access = "";
			
			while (i < downloadCount) {

				access = "$.results.bindings["+i+"]";
				try {
					
					download="";
					title="";
					description="";
					email="";
					rights="";
					
					try{download = JsonPath.read(jsonString, access+".accessUrl.value");}catch(Exception e){}
					try{title = JsonPath.read(jsonString, access+".title.value");}catch(Exception e){}
					try{description = JsonPath.read(jsonString, access+".description.value");}catch(Exception e){}
					try{email = JsonPath.read(jsonString, access+".email.value");}catch(Exception e){}
					try{rights = JsonPath.read(jsonString, access+".rights.value");}catch(Exception e){}
					
					Utils.debug("download "+i+" : "+download);
					Utils.debug("title "+i+" : "+title);
					Utils.debug("description "+i+" : "+description);
					Utils.debug("email "+i+" : "+email);
					Utils.debug("rights "+i+" : "+rights);
					
					if (download.equals(resourceInfo.getDataURL())) {
						resourceInfo.getResourceMetadata().setTitle(title);
						resourceInfo.getResourceMetadata().setDescription(description);
						resourceInfo.getResourceMetadata().setEmailContact(email);
						resourceInfo.getResourceMetadata().setRights(rights);
						resourceInfo.getResourceMetadata().setMetadataSource(MetadataSource.USER);
						return true;
					}
					
				} catch (Exception e) {e.printStackTrace();}
				//if (found) break;
				i++;
			}
			return false;
	
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	
	/**
	 * Parsed JSON from spraakbanken website (example json path https://spraakbanken.gu.se/eng/resource/kubhist-tidningforwenersborg-1870/json)
	 * @param file
	 * @deprecated links on spraakbanken website that provide json are broken
	 */
	public static boolean readSpraakbankenJsonUrl(File file, ResourceInfo resourceInfo) {
		
		if (file == null) return false;
		
		// parsing JSON with https://github.com/json-path/JsonPath
		try {
			String jsonString = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

			String title = JsonPath.read(jsonString, "$.metadata.identificationInfo.resourceName.eng");
			String description = JsonPath.read(jsonString, "$.metadata.identificationInfo.description.eng");
			int downloadCount = JsonPath.read(jsonString, "$.metadata.access.download.length()");
			int i = 0;
			
			String contact="";
			String contactSurname="";
			String contactGivenName="";
			String email= "";
			String type = "";
			String version="";
			String download="";
			String licence="";
			
			boolean found = false;
			
			while (i < downloadCount) {

				try {
					String download_ = JsonPath.read(jsonString, "$.metadata.access.download["+i+"].url");
					Utils.debug("download "+i+" : "+download_);
					String licence_ = JsonPath.read(jsonString, "$.metadata.access.download["+i+"].licence");
					
					if (download_.equals(resourceInfo.getDataURL())) {
						licence = licence_;
						download = download_;
						found=true;
					}
					
				} catch (Exception e) {e.printStackTrace();}
				if (found) break;
				i++;
			}
			
			try {contactSurname = JsonPath.read(jsonString, "$.metadata.contactPerson.surname");} catch (Exception e1) {}
			try {contactGivenName = JsonPath.read(jsonString, "$.metadata.contactPerson.givenName");} catch (Exception e1) {}
			try {email = JsonPath.read(jsonString, "$.metadata.contactPerson.communicationInfo.email");} catch (Exception e1) {}
			try {version = JsonPath.read(jsonString, "$.metadata.versionInfo.lastUpdated");} catch (Exception e) {}
			try {type = JsonPath.read(jsonString, "$.metadata.type");} catch (Exception e) {}
			
			contact = contactGivenName+" "+contactSurname;
			Utils.debug("title : "+title);
			Utils.debug("description : "+description);
			Utils.debug("contact : "+ contact);
			Utils.debug("email : "+email);
			Utils.debug("version : "+version);
			Utils.debug("type : "+type);
			Utils.debug("licence : "+licence);
			Utils.debug("download : "+download);
			
			resourceInfo.getResourceMetadata().setTitle(title);
			resourceInfo.getResourceMetadata().setDescription(description);
			resourceInfo.getResourceMetadata().setEmailContact(email);
			resourceInfo.getResourceMetadata().setType(type);
			resourceInfo.getResourceMetadata().setRights(licence);
			resourceInfo.getResourceMetadata().setYear(version);
			resourceInfo.getResourceMetadata().setMetadataSource(MetadataSource.CLARIN);
			resourceInfo.setMetaDataURL(ResourceManager.MetaDataFromClarin);
			//resourceInfo.getLinghubAttributes().setCreator(author);
        	//ri.getLinghubAttributes().setDate(new Date(rs.getString("date")));
			//resourceInfo.getLinghubAttributes().setCreator(contact);
			//resourceInfo.getLinghubAttributes().setPublisher(publisher);
			//resourceInfo.getLinghubAttributes().setDcLanguageString(languages);
			//resourceInfo.getLinghubAttributes().setDctLanguageString(languages);
			
			return found;
			
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	

}




/*while (i < licenceCount) {
String licence = JsonPath.read(jsonString, "$.metadata.distributionInfo.licenceInfo["+i+"].licence");

try {
	String download = JsonPath.read(jsonString, "$.metadata.distributionInfo.licenceInfo["+i+"].downloadLocation");
	Utils.debug("download "+i+" : "+download);
} catch (Exception e) {}

Utils.debug("licence "+i+" : "+licence);

i++;
}*/
//String download = JsonPath.read(jsonString, "$.metadata.distributionInfo.licenceInfo[0].downloadLocation");