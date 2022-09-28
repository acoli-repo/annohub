package de.unifrankfurt.informatik.acoli.fid.detector;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.tika.Tika;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.unifrankfurt.informatik.acoli.fid.types.ResourceFormat;



/**
 * Guess content of text of a file that does not have an extension
 * @author frank
 * 
 * 
 * # List of mime types
 * https://www.iana.org/assignments/media-types/media-types.xhtml
 *
 */
public class ContentTypeDetector {
	
/**
 * Map content type text descriptions (used on linghub.org) which contain a specific keyword to a ResourceFormat 
 */
private static final HashMap <String, ResourceFormat> contentDescriptionsMap = new HashMap <String, ResourceFormat> () {
	private static final long serialVersionUID = 1L;
{
			put("rdf",ResourceFormat.RDF);
			put("owl",ResourceFormat.RDF);
			put("gif",ResourceFormat.GRAPHICS);
			put("jpg",ResourceFormat.GRAPHICS);
			put("pdf",ResourceFormat.PDF);
			put("database",ResourceFormat.SQL);
			put("duration",ResourceFormat.SOUND);
			put("digitised",ResourceFormat.SOUND);
			put("compressed",ResourceFormat.ARCHIVE);
			put("digital image",ResourceFormat.GRAPHICS);
			put("16:9",ResourceFormat.VIDEO);
			put("4:3",ResourceFormat.VIDEO);
			put("palplus",ResourceFormat.VIDEO);
			put("beeldformaat",ResourceFormat.VIDEO);
			put("avi",ResourceFormat.VIDEO);
			put("text file",ResourceFormat.TEXT);
			put("textgrid",ResourceFormat.TEXT);
			put("hd1080i",ResourceFormat.VIDEO);
			put("video",ResourceFormat.VIDEO);
			put("mp4",ResourceFormat.VIDEO);
			put("flac",ResourceFormat.SOUND);
			put("recorded",ResourceFormat.SOUND);
			put("khz",ResourceFormat.SOUND);
			put("32 bit",ResourceFormat.SOUND);
			put("24 bit",ResourceFormat.SOUND);
			put("16 bit",ResourceFormat.SOUND);			
			put("tape",ResourceFormat.SOUND);
			put("mp3",ResourceFormat.SOUND);
			put("mpeg",ResourceFormat.SOUND);
			put("mov",ResourceFormat.VIDEO);
			put("pcm",ResourceFormat.SOUND);
			put("u-law",ResourceFormat.SOUND);
			put("reel to reel", ResourceFormat.SOUND);
			put("sampling format",ResourceFormat.SOUND);
			put("sampling rate",ResourceFormat.SOUND);
			put("digital audio",ResourceFormat.SOUND);
			put("digital wav", ResourceFormat.SOUND);
			put("digital video",ResourceFormat.VIDEO);
			put("record",ResourceFormat.SOUND);
			put("scotch",ResourceFormat.SOUND);
			put("sony",ResourceFormat.SOUND);
			put("maxwell",ResourceFormat.SOUND);
			put("tdk",ResourceFormat.SOUND);
			put("fuji",ResourceFormat.SOUND);
			put("ampex",ResourceFormat.SOUND);
			put("cassette",ResourceFormat.SOUND);
			put("minute",ResourceFormat.SOUND);
			put("audio",ResourceFormat.SOUND);
			put("cd",ResourceFormat.SOUND);
			put("dvd",ResourceFormat.SOUND);
			put("minidis",ResourceFormat.SOUND);
			put("dak industries",ResourceFormat.SOUND);
			put("floppy disk",ResourceFormat.UNKNOWN);
			put("wax cylinder",ResourceFormat.SOUND);
}};;


/**
 * Determine the ResourceFormat from both file extension and magic-mime-type
 * @param mime
 * @param fileExtension
 * @return ResourceFormat
 */
public static ResourceFormat getResourceFormatFromMIMEAndFileExtension(String mime, String fileExtension) {
	
	ResourceFormat rfMime = detectResourceFormatFromContentTypeString(mime);
	ResourceFormat rfFileExtension = detectResourceFormatFromContentTypeString(fileExtension);
	
	// Case 1 : 
	if (rfMime  == rfFileExtension) return rfMime;
	else {
	// Case 2 :
		if (rfMime == ResourceFormat.UNKNOWN) return rfFileExtension;
		
		// Case 3 : (fileExtension .rdf =>RDF rules out application/xml=>XML !)
		else return rfMime;
	}
}


/**
 * Classify a given content type identifier. Identifiers may be MIME types, file extensions or text descriptions.
 * Return a known type otherwise return ResourceFormat.UNKNOWN.
 * @param contentType
 * @return ResourceFormat
 */
public static ResourceFormat detectResourceFormatFromContentTypeString (String contentType) {
	
	if (contentType == null) return ResourceFormat.UNKNOWN;
	
	ResourceFormat resourceFormat = ResourceFormat.UNKNOWN;
			
	switch (contentType.toLowerCase().trim()) {
	
	case "application/rdf+xml" :
	case "rdf+xml" :
	case "application/x-turtle" :
	case "text/rdf+n3" :
	case "text/n3" :
	case "n3" :
	case "owl" :
	case "rdf" :
	case "rdf-turtle" :
	case "linked data" :
	case "n-quads" :
	case "html+rdfa" :
	case "ttl":
		resourceFormat = ResourceFormat.RDF;
		break;
		
	case "application/xml" :
	case "text/xml" :
	case "xml" :
	case "xml/tmx":
		resourceFormat = ResourceFormat.XML;
		break;
		
	case "text/plain":
	case "text/tsv":
	case "text/sgml":
	case "text":
		resourceFormat = ResourceFormat.TEXT;
		break;
		
	case "conll" :
	case "conll-u":
		resourceFormat = ResourceFormat.CONLL;
		break;

	case "sparql" :
		resourceFormat = ResourceFormat.SPARQL;
		break;
		
	case "sql":
	case "access":
		resourceFormat = ResourceFormat.SQL;
		break;

	case "archive":
	case "application/zip":
	case "tar.gz":
	case "gz":
	case "tgz":
	case "gzip":
	case "gz:txt":
	case "zip:ttl":
	case "zip:bib":
	case "zip:csv":
	case "gz:nt":
	case "gzip:ntriples":
	case ".tar.gz":
	case "gz:ttl:owl":
	case "gz:ttl":
	case "zip:8c":
		resourceFormat = ResourceFormat.ARCHIVE;
		break;
		
	case "html" :
	case "text/html":
		resourceFormat = ResourceFormat.HTML;
		break;
		
	case "json" :
		resourceFormat = ResourceFormat.JSON;
		break;
		
	case "csv" :
		resourceFormat = ResourceFormat.CSV;
		break;
	
	case "pdf" :
	case "application/pdf" :
		resourceFormat = ResourceFormat.PDF;
		break;
		
	case "ps" :
		resourceFormat = ResourceFormat.POSTSCRIPT;
		break;
		
	case "exel" :
		resourceFormat = ResourceFormat.EXCEL;
		break;
	
	case "image/jpeg" :
	case "image/png" :
	case "graphics" :
	case "png":
	case "gif":
		resourceFormat = ResourceFormat.GRAPHICS;
		break;
		
	case "sound":
	case "audio/x-wav":
	case "audio/mpeg":
	case "ogg vorbis":
	case "ogg":
	case "wav":
	case "wave":
	case "mpeg4":
	case "avi":
	case "audio/aac":
		resourceFormat = ResourceFormat.SOUND;
		break;
		
	case "video/x-flv":
	case "video/mpeg":
		resourceFormat = ResourceFormat.VIDEO;
		break;
		
	case "urn" :
		resourceFormat = ResourceFormat.URN;
		break;
		
	case "ftp" :
		resourceFormat = ResourceFormat.FTP;
		break;
		
	default :
		resourceFormat = ResourceFormat.UNKNOWN;
		break;
		
	}
	
	// not found yet -> search for keywords in description
	if (resourceFormat.equals(ResourceFormat.UNKNOWN)) {
		
		// html, rdf, dcif
		// compressed tarfile containing n-triples
		for (String keyword : contentDescriptionsMap.keySet()) {
			if (contentType.toLowerCase().trim().contains(keyword)) {
				resourceFormat = contentDescriptionsMap.get(keyword);
				break;
			}
		}
	}
	
	return resourceFormat;
}


public static ResourceFormat guessContentType (URL url) {
	return guessContentType (new File(url.getFile()));
}
	

public static ResourceFormat guessContentType (File file) {
	
	ResourceFormat resourceFormat = null;
	
	try {
		
		InputStream input;
		input = new FileInputStream(file.getAbsolutePath());
	
		
		String contentType = new Tika().detect(input);
		//Utils.debug("Detected ContentType :");
		//Utils.debug(contentType);
		

		switch (contentType) {
		
		case "application/rdf+xml" :
		case "application/x-turtle" :
		case "text/rdf+n3" :
		case "text/n3" :
			resourceFormat = ResourceFormat.RDF;
			break;
			
		case "application/xml" :
			resourceFormat = ResourceFormat.XML;
			break;
			
		case "text/plain" :
			resourceFormat = ResourceFormat.TEXT;
			break;
			
		case "application/octet-stream" :
			break;
			
		}
		
		
		
		
		// Use markers or regular expressions to guess type of text/plain	
		if (resourceFormat != null && resourceFormat.equals(ResourceFormat.TEXT)) {
			
			// Read sample of first 50 lines of file
			FileReader fr = new FileReader(file);
			BufferedReader reader = new BufferedReader(fr);
			int lines = 100;
			String text = "";
			String line;
			
			while ((line = reader.readLine()) != null && lines-- > 0) {
				text+= line;
			}
				

		// Detect RDF
			if (text.contains("@prefix")   ||
				text.contains("_:")) {
				resourceFormat = ResourceFormat.RDF;
			}
			
		// Detect CONLL
			if (text.contains("# text =")   ||
				text.contains("id=")) {
				resourceFormat = ResourceFormat.CONLL;
			}
		}
		
		
		//if (resourceFormat != null) 
		//	Utils.debug("ResourceFormat : "+resourceFormat.toString());
		
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	
	
	return resourceFormat;	
	}

	/**
	 * gets a raw input string and tries to figure out whether it's an xml file or not.
	 * more precisely, tries to generate a dom from a String input source, if it succeedes
	 * we assume it's xml, if it fails we assume it isn't.
	 * @param in
	 * @return
	 * @throws ParserConfigurationException
	 */
	public boolean isXML(String in) throws ParserConfigurationException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		try {
			Document doc = dBuilder.parse(new InputSource(new StringReader(in)));
			return true;
		}
		catch (IOException | SAXException e) {
			return false;
		}
	}

	/**
	 * gets a raw input string and tries to figure out whether it's an xml file or not.
	 * more precisely, tries to generate a dom from a File, if it succeedes
	 * we assume it's xml, if it fails we assume it isn't.
	 * @param in File
	 * @return
	 * @throws ParserConfigurationException
	 */
	public boolean isXML(File in) throws ParserConfigurationException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		try {
			Document doc = dBuilder.parse(in);
			return true;
		}
		catch (IOException | SAXException e) {
			return false;
		}
	}

	public static void main (String [] args) {
		
		try {
			
			URL url = new URL("file:/media/EXTRA/VifaRun/conll/464c776a14c3fff2ab3e4b7055af4147/1276-da.conll");
			ContentTypeDetector.guessContentType(url);
			ContentTypeDetector.guessContentType(new File("/media/EXTRA/VifaRun/conll/464c776a14c3fff2ab3e4b7055af4147/1276-da.conll"));
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		
		
	}
	
}
