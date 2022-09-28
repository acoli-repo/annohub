package de.unifrankfurt.informatik.acoli.fid.ubExport;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class Constants {
	
	public static XMLConfiguration config;
	
	public static File[] OLIA_PATHS;
	public static String SPARQL_PREFIX;
	public static String TTL_PREFIX;
	public static int[] CONLL_COLS;
	
//	public static File[] OLIA_PATHS = {
////			new File("D:\\_SVN\\valian\\intern\\Virtuelle_Fachbibliothek\\UB\\OWL\\Linker\\bll_test"),
//			new File("D:\\_SVN\\valian\\intern\\Virtuelle_Fachbibliothek\\UB\\OWL\\BLLThesaurus\\bll-ontology.ttl"),
//			new File("D:\\_SVN\\valian\\intern\\Virtuelle_Fachbibliothek\\UB\\OWL\\Linker\\linked-bll-ontology-with-olia-and-olia-top.ttl"),
//			new File("D:\\_SVN\\olia-sf\\trunk\\owl\\stable\\"),
//			new File("D:\\_SVN\\olia-sf\\trunk\\owl\\core\\")
//	};
	
//	public static String SERVICE_URI = "http://localhost:7092/blazegraph/namespace/ds/sparql";
	public static String SERVICE_URI = "http://localhost:7093/ds/data";
//	public static String SERVICE_URI = "http://localhost:3030/ds/data";
	
	public static String OLIA_REGEX = "((http://purl.org/olia/olia).*)";
	public static String BLL_REGEX = "((http://data.linguistik.de/bll).*)";
	
	public static void loadConfig(String configurationFile) {
		Configurations configs = new Configurations();
    	try {
    	    config = configs.xml(configurationFile);
    	}
    	catch (ConfigurationException cex)
    	{
    		cex.printStackTrace();
    		System.exit(0);
    	}
    	loadConfig(config);
	}
	
	public static void loadConfig(XMLConfiguration config) {
		Constants.config = config;
		OLIA_PATHS = getOliaPaths();
    	CONLL_COLS = getConllCols();
    	SPARQL_PREFIX = getSparqlPrefix();
    	TTL_PREFIX = getTTLPrefix();
    	try {
    		OLIA_REGEX = config.getString("OliaSVN.oliaregex");
    	} catch (Exception e) {
    		System.err.println(e);
    	}
    	try {
    		BLL_REGEX = config.getString("OliaSVN.bllregex");
    	} catch (Exception e) {
    		System.err.println(e);
    	}
	}
	
	public static File[] getOliaPaths() {
		ArrayList<File> out = new ArrayList<File>();
		for (String s:(String[])config.getArray(String.class, "OliaSVN.path")) {
			out.add(new File(s));
		}
			
		return (File[])out.toArray(new File[]{});
	}
	
	public static int[] getConllCols() {
		int[] out = new int[0];
		try {
			out = (int[])config.getArray(int.class, "CONLL.col");
			Arrays.sort(out);
		} catch (Exception e) {
			System.err.println("CONLL col does not contain integer values. "
					+ "Please check " + config.getDocument().getDocumentURI());
		}
		return out;
	}
	
	public static String getSparqlPrefix() {
		String out = "";
		
		Iterator<String> iter = config.getKeys("PrefixURIs");
		while (iter.hasNext()) {
			String s = iter.next();
			out += "\r\n" + "prefix "+s.substring(11)+": <"+config.getString(s)+">";
		}
				
		return out;
	}
	
	public static String getTTLPrefix() {
		String out = "";
		
		Iterator<String> iter = config.getKeys("PrefixURIs");
		while (iter.hasNext()) {
			String s = iter.next();
			out += "\r\n" + "@prefix "+s.substring(11)+": <"+config.getString(s)+"> ."; 
		}
		
		return out;
	}
	
	public static String prefix2uri(String prefix) {
		String out = "";
		
		Iterator<String> iter = config.getKeys("PrefixURIs");
		while (iter.hasNext()) {
			String s = iter.next();
			if (s.endsWith("."+prefix)) {
				return config.getString(s); 
			}
		}
		
		return out;
	}
	
	public static boolean removeFolderContent(String s) {
		boolean success = true;
		File folder = new File(s);
		if (!folder.exists()) {
			folder.mkdirs();
			return success;
		}
		for (File f:folder.listFiles()) {
			if (!f.delete()) success = false;
		}
		return success;
	}

	
}
