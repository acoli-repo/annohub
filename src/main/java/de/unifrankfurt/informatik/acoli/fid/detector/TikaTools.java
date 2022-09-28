package de.unifrankfurt.informatik.acoli.fid.detector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.langdetect.TextLangDetector;
import org.apache.tika.language.LanguageIdentifier;
import org.apache.tika.language.detect.LanguageResult;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.xml.XMLParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import de.unifrankfurt.informatik.acoli.fid.parser.ParserGlottologMap;
import de.unifrankfurt.informatik.acoli.fid.parser.ParserLexvoMap;
import de.unifrankfurt.informatik.acoli.fid.parser.ParserSILMap;
import de.unifrankfurt.informatik.acoli.fid.util.IndexUtils;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;







public class TikaTools {
	
	
	public static HashMap <String, URL> iso2Lexvo = null;
	public static HashMap <String, URL> iso2Glottolog = null;
	public static HashMap <String, URL> isoSIL = null;
	public static XMLConfiguration config = null;
	
	
	
	public static boolean isISO639LanguageCode(String string) {
		
		initISOCodeMaps();
		return (iso2Lexvo.keySet().contains(string.toLowerCase()) || isoSIL.keySet().contains(string.toLowerCase()));
		
		/*
		if (string.matches ("[a-zA-Z]+") && (string.length() >= 2 && string.length() <= 3))
			return true;
		else
			return false;
		*/
	}
	
	
	public static boolean isLexvoUrl(String urlString) {
		
		initISOCodeMaps();
		try {
			return iso2Lexvo.values().contains(new URL(urlString));
		} catch (MalformedURLException e) {
			//e.printStackTrace();
			return false;
		}
	}

	
	public static URL getLexvoUrlFromISO639_3Code (String languageCode) {
		
		/*
		 * ISO 639 is the International Standard for language codes. 
		 * The purpose of ISO 639 is to establish internationally recognised codes
		 * (either 2, 3, or 4 letters long) for the representation of languages or language families.
		 * ISO 639-2 is two letters long
		 * ISO 639-3 is three letters long
		 */
		
		/*
		Locale locale = new Locale(languageCode);
		String isoCode = locale.getISO3Language();
		*/
		
		// Get lexvo URL for isoCode
		
		initISOCodeMaps();
		
		if (!isISO639LanguageCode(languageCode.toLowerCase())) {
			Utils.debug("(No lexvo URL found for language code : "+languageCode+")");
			return null;
		}
		
		
		URL lexvoUrl = iso2Lexvo.get(languageCode.toLowerCase());
		if (lexvoUrl == null) {
			// Must be new ISO code (can be found on SIL)
			try {
				lexvoUrl = new URL("http://lexvo.org/id/iso639-3/"+languageCode.toLowerCase());
			} catch (Exception e) {e.printStackTrace();}
		}
		//Utils.debug("Converted Language string : "+languageCode+" -> "+lexvoUrl.toString());
		return lexvoUrl;
	}
	
	
	private static void initISOCodeMaps() {
		
		// Read language codes from file
		if (iso2Lexvo == null || iso2Glottolog == null) {
			
		iso2Lexvo = new ParserLexvoMap().parse(); // incomplete code table
		isoSIL = new ParserSILMap().parse(); // most actual code table but some (87) lexvo URLs do not resolve !
		iso2Glottolog = new ParserGlottologMap().parse();
		
		Utils.debug("");
		Utils.debug("Loaded ISO Code Maps :");
		Utils.debug("Lexvo : "+iso2Lexvo.size()+" codes");
		Utils.debug("SIL : "+isoSIL.size()+" codes");
		Utils.debug("Glottolog : "+iso2Glottolog.size()+" codes");
		
		}
		
	}

	
	public static String getISO639_3CodeFromLexvoUrl(URL lexvoUrl) {
		return new File(lexvoUrl.getPath()).getName();
	}
	
	public static String getISO639_3CodeFromLexvoUrl(String lexvoUrl) {
		return new File(lexvoUrl).getName();
	}
	
	
	/**
	 * Converts any ISO language code to ISO639-3 code format (which has 3 letters)
	 * @param anyIsoCode
	 * @return ISO639-3 code
	 */
	public static String getISO639_3CodeFromISOCode(String anyIsoCode) {
		URL lexvoUrl = TikaTools.getLexvoUrlFromISO639_3Code(anyIsoCode);
		if (lexvoUrl != null) {
			return TikaTools.getISO639_3CodeFromLexvoUrl(lexvoUrl);
		} else {
			return null;
		}
	}
	
	

	/**
	 * Uses the lexvo.org API
	 * @param languageCode
	 * @return
	 * @deprecated
	 */
	public String convertIsoCode639ToLexvo (String languageCode) {
	
		/*
		 * ISO 639 is the International Standard for language codes. 
		 * The purpose of ISO 639 is to establish internationally recognised codes
		 * (either 2, 3, or 4 letters long) for the representation of languages or language families.
		 * ISO 639-2 is two letters long
		 * ISO 639-3 is three letters long
		 */
		
		return Identifiers.getLanguageURIforISO639P3(languageCode);
	}
	
	
	/**
	 * Retrieve the Glottolog URL for a given language code
	 * @param languageCode
	 * @return URL Glottolog language entry
	 * @deprecated
	 */
	public URL convertLanguageCodeToGlottologUrl (String languageCode) {
		
		initISOCodeMaps();
		
		URL glottologUrl = iso2Glottolog.get(languageCode);
		Utils.debug("Converted Language string : "+languageCode+" -> "+glottologUrl.toString());
		if (glottologUrl == null) {
			Utils.debug("No glottolog URL found for language code : "+languageCode);
		}
		return glottologUrl;
	}
	
	
	/**
	 * Identify the language(s) of a given text
	 * @param text
	 * @return
	 * @deprecated
	 */
	public static String identifyLanguage(String text) {
		Utils.debug("Using file sample for language detection : ");
		Utils.debug(text+"\n");
	    LanguageIdentifier identifier = new LanguageIdentifier(text);
	    return identifier.getLanguage();
	}
	
		
	
	
	/**
	 * Identify the language(s) in a given text
	 * @param text
	 * @return List of languages
	 * @deprecated
	 */
	public List<LanguageResult> identifyAllLanguagesInText(String text) {
		
		TextLangDetector textLangDetector = new TextLangDetector();
		List<LanguageResult> result = textLangDetector.detectAll(text);
		Utils.debug("Using file sample for language detection : ");
		Utils.debug(text+"\n");
	    return result;
	}
	

	/**
	 * Detect file type as MIME type 
	 * @param Local file
	 * @return MIME type of file
	 */
	public static String detectFileType (File file) {
		
		String type = "";
		Tika tika = new Tika();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
						 
			//Construct BufferedReader from InputStreamReader
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		 
			String line = null;
			while ((line = br.readLine()) != null) {
				Utils.debug(line);
				break; // only read first line !
			}
			br.close();
			if (line != null && line.startsWith("<?xml version")) {
				return "application/xml";
			}
			
			fis = new FileInputStream(file);
			type = tika.detect(fis);
			Utils.debug("File : "+file.getAbsolutePath() +" -> "+ type);
			
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
		
		return type;
		}
	
	
	
	  /**
	   * Function extracts text from XML body
	   * @param inputFile
	   * @param outputFile
	   * @throws IOException
	   * @throws SAXException
	   * @throws TikaException
	   */
	  public static void xml2Text(File inputFile, File outputFile) throws IOException,SAXException, TikaException {

	      //detecting the file type
	      BodyContentHandler handler = new BodyContentHandler(-1);
	      
	      Metadata metadata = new Metadata();
	      FileInputStream inputstream = new FileInputStream(inputFile);
	      ParseContext pcontext=new ParseContext();
	     
	      
	      XMLParser  xmlParser = new XMLParser();
	      //Text document parser
	      //TXTParser  TexTParser = new TXTParser();
	      //TexTParser.parse(inputstream, handler, metadata,pcontext);
	      xmlParser.parse(inputstream, handler, metadata,pcontext);
	      Utils.debug("Contents of the document:" + handler.toString());
	      String text = handler.toString();
	      int mark = text.indexOf("TO BE CHECKED");
	      text = StringUtils.substring(text,mark+15);
	      FileUtils.write(outputFile, text);
	      Utils.debug("Metadata of the document:");
	      String[] metadataNames = metadata.names();
	      
	      for(String name : metadataNames) {
	         Utils.debug(name + " : " + metadata.get(name));
	      }
	   }
	  
	  
	  
	  
	  public static void main (String [] args) {
		  
		 
		Utils.debug(TikaTools.isLexvoUrl("http://lexvo.org/id/iso639-3/hun"));
		if (true)return;

		File rootDir = new File("/media/EXTRA/GITHUB-Bibles/xml");
		File targetDir = new File(rootDir,"txt");
		  
		for (String file : IndexUtils.listRecFilesInDir(rootDir)) {
		Utils.debug(file);
		try {
			File sourceFile = new File(file);
			File targetFile = new File(targetDir,FilenameUtils.removeExtension(sourceFile.getName())+".txt");
			xml2Text(sourceFile, targetFile);
			} 
		catch (Exception e) {
			e.printStackTrace();
			}
		}
	  }
	
}
