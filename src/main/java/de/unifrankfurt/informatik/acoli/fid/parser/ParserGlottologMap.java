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


public class ParserGlottologMap {

	HashMap <String, URL> iso2Glottolog = new HashMap <String, URL> ();
	String glottoLogBase = "http://glottolog.org/resource/languoid/id/";
	private LocateUtils locateUtils;
	
	public ParserGlottologMap() {
		locateUtils = new LocateUtils();
	}
	
	
	public HashMap<String, URL> parse() {
		
		String iso="";
		String glottologId="";
				
		CSVParserConfig config = new CSVParserConfig();
		config.setCsvFormat(CSVFormat.DEFAULT);
		config.setCharset(StandardCharsets.UTF_8);
		config.setFields(new HashMap <Integer,String> () {
			static final long serialVersionUID = 1L;
			{
				put(7,"ISO-CODE");
				put(8,"GLOTTOLOG-ID");
			}});
		config.setColumn(0);
		config.setCommentMarker('#');
		
		String [] glottologFiles = {"Languages.csv"};

		CSVFormat csvFormat = config.getCsvFormat();
		csvFormat = csvFormat.withCommentMarker(config.getCommentMarker());
		// quote character null is needed - otherwise can miss correct field
		csvFormat = csvFormat.withQuote(config.getQuoteCharacter());
		csvFormat = csvFormat.withIgnoreEmptyLines(true);
		
		System.out.println("\nLoading GlottoLog language code maps :");
		
		for (String glottologFile : glottologFiles) {
			
		try {
			File codeMapFile = locateUtils.getLocalFile("/languageCodes/"+glottologFile);
			if (codeMapFile == null) {
				
				System.out.println("Could not find path to ISO code file : "+glottologFile+" !");
				System.out.println("Trying 'RunParameter.isoCodeMapDirectory' parameter from FIDConfig.xml !");
				codeMapFile = new File(Executer.getFidConfig().getString("RunParameter.isoCodeMapDirectory"),glottologFile);
				System.out.println("Success !");
			} else {
				System.out.println(codeMapFile.getAbsolutePath());
			}
			Iterable<CSVRecord> records = CSVParser.parse(
					//new File(System.getProperty("user.dir")+"/glottolog.org/"+glottologFile),
					codeMapFile,
					config.getCharset(),
					csvFormat
					);
			

			for (CSVRecord record : records) {
				
			   iso = record.get(7);
			   glottologId = record.get(8);
			   //System.out.println(iso+" : "+ glottologId);
			   
			   iso2Glottolog.put(iso, new URL(glottoLogBase+glottologId));
			   
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		}
		return iso2Glottolog;
	}
	
	

}
