package de.unifrankfurt.informatik.acoli.fid.parser;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.apache.commons.csv.CSVFormat;

public class CSVParserConfig {
	
	private String delimiter;
	private CSVFormat csvFormat;
	private Charset charset;
	private HashMap <Integer,String> fields;
	private int column;
	private Character commentMarker;
	private Character quoteCharacter;

	
	/**
	 * Config using default values
	 */
	public CSVParserConfig () {
		
		// Default values
		setCsvFormat(CSVFormat.TDF);
		setCharset(StandardCharsets.UTF_8);
		setCommentMarker('#');
		
		
		if (getCommentMarker() != null) {
			this.csvFormat = csvFormat.withCommentMarker(getCommentMarker());
		}
		
		// quote character null is needed - otherwise can miss correct field
		this.csvFormat = csvFormat.withQuote(getQuoteCharacter());
		this.csvFormat = csvFormat.withIgnoreEmptyLines(true);

		/*
		setFields(new HashMap <Integer,String> () {
			static final long serialVersionUID = 1L;
			{
				put(1,"Word");
				put(3,"POS-TAG-1");
				put(4,"POS-TAG-2");
			}});
	
		setColumn(3);
		*/
		
	}
	
	public String getDelimiter() {
		return delimiter;
	}
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}
	public HashMap <Integer,String> getFields() {
		return fields;
	}
	public void setFields(HashMap <Integer,String> fields) {
		this.fields = fields;
	}

	public CSVFormat getCsvFormat() {
		return csvFormat;
	}

	public void setCsvFormat(CSVFormat csvFormat) {
		this.csvFormat = csvFormat;
	}

	public Charset getCharset() {
		return charset;
	}

	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	public int getColumn() {
		return column;
	}

	public void setColumn(int column) {
		this.column = column;
	}

	public Character getCommentMarker() {
		return commentMarker;
	}

	public void setCommentMarker(Character commentMarker) {
		this.commentMarker = commentMarker;
	}
	
	public void setQuoteCharacter(Character c) {
		this.quoteCharacter = c;
	}
	
	public Character getQuoteCharacter() {
		return quoteCharacter;
	}

}
