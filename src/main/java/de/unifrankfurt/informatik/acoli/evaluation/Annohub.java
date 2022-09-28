package de.unifrankfurt.informatik.acoli.evaluation;


import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;

import au.com.bytecode.opencsv.CSVReader;

//import com.opencsv.CSVReader; newer version is tested, bytecode version not tested

public class Annohub {
	
	
	public static void compare (String annohubLanguagesInResources, String newDS) {
		
	try {
		
		HashMap<String, HashSet<String>> annohubResources2Languages = 
				readAnnohubLanguagesInResources(annohubLanguagesInResources);
		
		
		
		HashMap<String, HashSet<String>> newDsResources2Languages = 
				readNewDS(newDS);
		
		HashSet<String> identicalLanguageData = new HashSet<String>();
		HashMap<String, String> notInAnnohub = new HashMap<String, String>();
		HashMap<String, String> notInNewDS = new HashMap<String, String>();


		int totalAnnohubLanguages = 0;
		int unmatchedMeldTitles = 0;
		
		// compare languages for each resource
		for (String title : newDsResources2Languages.keySet()) {
			
			if (annohubResources2Languages.get(title) != null) {
				totalAnnohubLanguages += annohubResources2Languages.get(title).size();
			}
			
			// check equality
			if (newDsResources2Languages.get(title).equals(annohubResources2Languages.get(title))) {
				identicalLanguageData.add(title);
			} else {
				
				HashSet <String> annohubResourceLanguages = annohubResources2Languages.get(title);
				HashSet <String> newewDsResourceLanguages = newDsResources2Languages.get(title);
				
				if (!annohubResources2Languages.containsKey(title)) {
					System.out.println("annohubResources2Languages does not contain title : "+ title );
					unmatchedMeldTitles++;
					continue;
				}
				if (annohubResourceLanguages == null) {
					System.out.println("annohubResources2Languages has no languages for title : "+ title );
					continue;
				}
				
				if (newewDsResourceLanguages == null) {
					System.out.println("newDsResources2Languages has no languages for title : "+ title );
					continue;
				}
				
				notInAnnohub.put(title, SetUtils.difference(newewDsResourceLanguages, annohubResourceLanguages).toString());
				notInNewDS.put(title, SetUtils.difference(annohubResourceLanguages, newewDsResourceLanguages).toString());

			}
			
		}
		
		System.out.println("Unmatched Meld datasets "+unmatchedMeldTitles);
		System.out.println("Identical datasets : "+identicalLanguageData.size());
		for (String title : identicalLanguageData) {
			System.out.println("identical "+title);
		}
		System.out.println("Different datasets : ");
		int id = 1;
		for (String title : SetUtils.union(notInAnnohub.keySet(), notInNewDS.keySet())) {
			System.out.println(id+" title :"+ title);
			System.out.println("lcount in Melld "+newDsResources2Languages.get(title).size());
			System.out.println("lcount in Annohub "+annohubResources2Languages.get(title).size());
			if (notInAnnohub.containsKey(title)) {
				System.out.println("notInAnnohub : "+notInAnnohub.get(title));
			}
			if (notInNewDS.containsKey(title)) {
				System.out.println("notMelld : "+notInNewDS.get(title));
			}
			System.out.println();
			id++;
		}
		
		System.out.println("Total number of languages for Annohub records that appear in Meld : "+totalAnnohubLanguages);
		
		
		
		
	} catch (Exception e) {
		e.printStackTrace();
	}
		
		
	}
	
	
	public static HashMap<String, HashSet<String>> readAnnohubLanguagesInResources(String file) throws Exception {
		   
	      //Instantiating the CSVReader class
	      CSVReader reader = new CSVReader(new FileReader(file));
	      
	      HashMap <String, HashSet<String>> map = new HashMap <String, HashSet<String>>();
	      
	      //Reading the contents of the csv file
	      StringBuffer buffer = new StringBuffer();
	      String line[];
	      int lines = 0;
	      while ((line = reader.readNext()) != null) {
	    	  
	    	 lines++;
	    	  
	    	 String title = line[0].trim();
	    	 String language = line[1].trim();
	    	 //System.out.println(title+" "+language);

	    	 
	    	 if (!map.containsKey(title)) {
	    		 HashSet<String> lset = new HashSet<String>();
	    		 lset.add(language);
	    		 map.put(title, lset);
	    	 } else {
	    		 HashSet<String> lset = map.get(title);
	    		 lset.add(language);
	    		 map.put(title, lset); 
	    	 } 
	      }
	      System.out.println("Annohub records : "+map.keySet().size());
	      //System.out.println("lines in csv file : "+ lines);

	      return map;
	   }
	
	

   public static HashMap<String, HashSet<String>> readNewDS(String file) throws Exception {
	   
      //Instantiating the CSVReader class
      CSVReader reader = new CSVReader(new FileReader(file));
      
      HashMap <String, HashSet<String>> map = new HashMap <String, HashSet<String>>();
      
      //Reading the contents of the csv file
      int orcidFound = 0;
      StringBuffer buffer = new StringBuffer();
      String line[];
     
      while ((line = reader.readNext()) != null) {
    	 
    	 if (!line[2].equals("AnnoHub")) continue;
    	  
    	 String title = line[3].trim();
    	 //System.out.println(title);
    	 String orcid = line[3].trim();
    	 if (!orcid.isEmpty()) orcidFound++;

    	 String languageString = StringUtils.substring(line[12], 0, line[12].length());
    	 
    	 String [] languages = languageString.split(",");
    	 HashSet<String> lset = new HashSet<String>();
    	 for (String l : languages) {
    		 lset.add(l);
    	 }
    	 map.put(title, lset);
    	 
      }
      
      System.out.println("Annohub newDS records : "+map.keySet().size());
      System.out.println("Orcid found for Annohub title : "+orcidFound);
      return map;
   }
   
   
   
    public static void main(String[] args) {
    	
		compare("/home/demo/Schreibtisch/Review/sparql-results/languagesInResources.csv",
				"/home/demo/Schreibtisch/Review/melld.csv");
	}
   
}
