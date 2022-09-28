package de.unifrankfurt.informatik.acoli.fid.parser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import de.unifrankfurt.informatik.acoli.fid.exec.Executer;
import de.unifrankfurt.informatik.acoli.fid.util.LocateUtils;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;

/*
 * Parser for ISO639-3 Codes from local file
 * (https://iso639-3.sil.org/sites/iso639-3/files/downloads/iso-639-3_Code_Tables_20180123.zip)
 */
public class ParserISONames {
	private ArrayList <String> isocodesWithNames = new ArrayList <String> ();
	private LocateUtils locateUtils = new LocateUtils();
	private static HashMap<String, String> isoCodes2Names = new HashMap<String,String>();

	
	public ParserISONames() {}
	
	
	public void parse() {
		
		String iso="";
		String name="";
				
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
		
		String [] files = {"iso-639-3_20180123.tab"};

		CSVFormat csvFormat = config.getCsvFormat();
		csvFormat = csvFormat.withCommentMarker(config.getCommentMarker());
		// quote character null is needed - otherwise can miss correct field
		csvFormat = csvFormat.withQuote(config.getQuoteCharacter());
		csvFormat = csvFormat.withIgnoreEmptyLines(true);
		
		for (String file : files) {
		try {
			File codeMapFile = locateUtils.getLocalFile("/languageCodes/"+file);
			if (codeMapFile == null) {
				System.out.println("Could not find path to ISO code file : "+file+" !");
				System.out.println("Trying 'RunParameter.isoCodeMapDirectory' parameter from FIDConfig.xml !");
				codeMapFile = new File(Executer.getFidConfig().getString("RunParameter.isoCodeMapDirectory"),file);
				System.out.println("Success !");
			}
			
			Iterable<CSVRecord> records = CSVParser.parse(
					//new File(System.getProperty("user.dir")+"/lexvo.org/"+lexvoFile),
					codeMapFile,
					config.getCharset(),
					csvFormat
					);
			

			for (CSVRecord record : records) {
			
			try {
			   iso = record.get(0);
			   name = record.get(6);
			   //Utils.debug(iso+StringUtils.repeat(' ', 6-iso.length())+name);
			   isocodesWithNames.add(iso+StringUtils.repeat(' ', 6-iso.length())+name);
			   isoCodes2Names.put(iso, name);
			} catch (Exception e){}
			
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		}
		
	}


	public static HashMap<String, String> getIsoCodes2Names() {
		return isoCodes2Names;
	}


	public ArrayList<String> getIsocodesWithNames() {
		return isocodesWithNames;
	}

}
