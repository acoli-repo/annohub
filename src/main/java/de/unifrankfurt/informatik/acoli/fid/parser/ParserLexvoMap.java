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

/**
 * Parse lexvo ISO tables from local file
 * @author frank
 *
 */
public class ParserLexvoMap {

	private HashMap <String, URL> iso2Lexvo = new HashMap <String, URL> ();
	private LocateUtils locateUtils = new LocateUtils();
	
	public ParserLexvoMap() {}
	
	
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
				put(1,"LEXVO-URL");
			}});
		config.setColumn(0);
		config.setCommentMarker('#');
		
		String [] lexvoFiles = {"lexvo-iso639-1.tsv","lexvo-iso639-2.tsv","lexvo-iso639-3.tsv",
				"lexvo-iso639-5.tsv","lexvo-marc21.tsv"};

		CSVFormat csvFormat = config.getCsvFormat();
		csvFormat = csvFormat.withCommentMarker(config.getCommentMarker());
		// quote character null is needed - otherwise can miss correct field
		csvFormat = csvFormat.withQuote(config.getQuoteCharacter());
		csvFormat = csvFormat.withIgnoreEmptyLines(true);
		
		System.out.println("\nLoading lexvo code maps :");
		
		for (String lexvoFile : lexvoFiles) {
		try {
			File codeMapFile = locateUtils.getLocalFile("/languageCodes/"+lexvoFile);
			if (codeMapFile == null) {
				System.out.println("Could not find path to ISO code file : "+lexvoFile+" !");
				System.out.println("Trying 'RunParameter.isoCodeMapDirectory' parameter from FIDConfig.xml !");
				codeMapFile = new File(Executer.getFidConfig().getString("RunParameter.isoCodeMapDirectory"),lexvoFile);
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
			   url = record.get(1);
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
