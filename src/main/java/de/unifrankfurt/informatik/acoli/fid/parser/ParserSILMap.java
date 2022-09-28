package de.unifrankfurt.informatik.acoli.fid.parser;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import de.unifrankfurt.informatik.acoli.fid.exec.Executer;
import de.unifrankfurt.informatik.acoli.fid.util.LocateUtils;

/*
 * Parser for SIL ISO639-3 Codes from local file
 * (https://iso639-3.sil.org/sites/iso639-3/files/downloads/iso-639-3_Code_Tables_20180123.zip)
 */
public class ParserSILMap {
	private HashMap <String, URL> iso2Lexvo = new HashMap <String, URL> ();
	private LocateUtils locateUtils = new LocateUtils();
	private static final String lexvoPrefix = "http://lexvo.org/id/iso639-3/";
	
	public ParserSILMap() {}
	
	
	public HashMap<String, URL> parse() {
		
		String iso="";
		String url="";
				
		CSVParserConfig config = new CSVParserConfig();
		config.setCsvFormat(CSVFormat.TDF);
		config.setCharset(StandardCharsets.UTF_8);
		config.setFields(new HashMap <Integer,String> () {
			static final long serialVersionUID = 1L;
			{
				put(0,"ISO-CODE");
			}});
		config.setColumn(0);
		config.setCommentMarker('#');
		
		String [] silFiles = {"iso-639-3_20180123.tab"};

		CSVFormat csvFormat = config.getCsvFormat();
		csvFormat = csvFormat.withCommentMarker(config.getCommentMarker());
		// quote character null is needed - otherwise can miss correct field
		csvFormat = csvFormat.withQuote(config.getQuoteCharacter());
		csvFormat = csvFormat.withIgnoreEmptyLines(true);
		
		System.out.println("\nLoading SIL language code maps :");
		
		for (String silFile : silFiles) {
		try {
			File codeMapFile = locateUtils.getLocalFile("/languageCodes/"+silFile);
			if (codeMapFile == null) {
				System.out.println("Could not find path to ISO code file : "+silFile+" !");
				System.out.println("Trying 'RunParameter.isoCodeMapDirectory' parameter from FIDConfig.xml !");
				codeMapFile = new File(Executer.getFidConfig().getString("RunParameter.isoCodeMapDirectory"),silFile);
				System.out.println("Success !");
			} else {
				System.out.println(codeMapFile.getAbsolutePath());
			}
			
			Iterable<CSVRecord> records = CSVParser.parse(
					codeMapFile,
					config.getCharset(),
					csvFormat
					);
			

			for (CSVRecord record : records) {
				
			   iso = record.get(0);
			   url = lexvoPrefix+iso;
			   //System.out.println(iso+" : "+ url);
			   
			   iso2Lexvo.put(iso, new URL(url));
			   
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		}
		return iso2Lexvo;
	}
	
	


}
