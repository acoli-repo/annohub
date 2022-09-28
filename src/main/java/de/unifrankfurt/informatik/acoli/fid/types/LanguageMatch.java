package de.unifrankfurt.informatik.acoli.fid.types;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import de.unifrankfurt.informatik.acoli.fid.detector.TikaTools;
import de.unifrankfurt.informatik.acoli.fid.parser.ParserISONames;

public class LanguageMatch implements Serializable {

	private URL lexvoUrl = null;
	private String languageISO639Identifier = "";
	private String languageNameEn = "unknown";
	private DetectionMethod detectionMethod = DetectionMethod.AUTO;
	private DetectionSource detectionSource = DetectionSource.LANGPROFILE;

	private Long hitCount=0L;
	private Long differentHitTypes=0L; 
	// value 0 for hitCount or differentHitTypes means no info available

	private boolean selected = true;
	private int conllColumn = NOCOLUMN;
	private String xmlAttribute="";		// default no
	private String rdfProperty="";		// default no
	
	private float minProb = 0.0f;		// lowest probability measured for a test sentence
	private float maxProb = 0.0f;		// highest probability measured for a test sentence
	private float averageProb = 0.0f;	// average probability measured for all test sentences
	
	public static final int NOCOLUMN = -1;
	private Date updated = new Date();
	private String updateText = "added";
	
	
	private static final long serialVersionUID = -447213071635L;
	
	
	public LanguageMatch (String languageISO639Identifier, Long hitCount, DetectionMethod detectionMethod) throws InvalidLanguageException{
		setLanguageISO639IdentifierAndLexvoUrl(languageISO639Identifier);
		this.hitCount = hitCount;
		setDetectionMethod(detectionMethod);
	}
	
	
	public LanguageMatch (String languageISO639Identifier, DetectionMethod detectionMethod) throws InvalidLanguageException{
		setLanguageISO639IdentifierAndLexvoUrl(languageISO639Identifier);
		setDetectionMethod(detectionMethod);
	}
	
	
	public LanguageMatch (URL lexvoUrl, DetectionMethod detectionMethod) throws InvalidLanguageException {
		setLanguageISO639IdentifierAndLexvoUrl(lexvoUrl);
		setDetectionMethod(detectionMethod);
	}
	
	
	public LanguageMatch (URL lexvoUrl) throws InvalidLanguageException {
		setLanguageISO639IdentifierAndLexvoUrl(lexvoUrl);	
	}
	
	public LanguageMatch (String languageISO639Identifier) throws InvalidLanguageException {
		setLanguageISO639IdentifierAndLexvoUrl(languageISO639Identifier);
	}
	

	

	public URL getLexvoUrl() {
		return lexvoUrl;
	}
	public void setLexvoUrl(URL lexvoUrl) {
		this.lexvoUrl = lexvoUrl;
	}
	public DetectionMethod getDetectionMethod() {
		return detectionMethod;
	}
	public void setDetectionMethod(DetectionMethod detectionMethod) {
		this.detectionMethod = detectionMethod;
		if (detectionMethod.equals(DetectionMethod.MANUAL)) this.detectionSource = DetectionSource.SELECTION;
	}

	public Long getDifferentHitTypes() {
		return differentHitTypes;
	}

	public void setDifferentHitTypes(Long dHitTypes) {
		this.differentHitTypes = dHitTypes;
	}

	public String getLanguageISO639Identifier() {
		return languageISO639Identifier;
	}

	
	/**
	 * 
	 * @param language lexvo URL
	 */
	public void setLanguageISO639IdentifierAndLexvoUrl(URL lexvoUrl) throws InvalidLanguageException {
		
		this.lexvoUrl = lexvoUrl;
		this.languageISO639Identifier = TikaTools.getISO639_3CodeFromLexvoUrl(lexvoUrl);
		try {
			this.languageNameEn = ParserISONames.getIsoCodes2Names().get(this.languageISO639Identifier).replace("'", "");
			} catch (Exception e){
				
				String languageISO639IdentifierError;
				String lexvoUrlError;
				if (this.languageISO639Identifier == null) languageISO639IdentifierError="null";
				else languageISO639IdentifierError = this.languageISO639Identifier;
				if (this.lexvoUrl == null) lexvoUrlError = "null";
				else lexvoUrlError = this.lexvoUrl.toString();
				
				String errorMsg = "Error while processing Lexvo URL '"+lexvoUrlError+"'\n"+
				"ISO639 language identifier '"+languageISO639IdentifierError+"'\n"+
				"Error : Could not get language name for ISO-Code from code table !";
				System.out.println(errorMsg);
//				throw new InvalidLanguageException("Error while processing Lexvo URL '"+lexvoUrlError+"'\n"+
//				"and derived ISO639 language identifier '"+languageISO639IdentifierError+"'\n"+
//				"Error : Could not get language name for ISO-Code from code table !");
			};
	}
	
	/**
	 * 
	 * @param languageISO639Identifier 2,3 letter ISO identifier
	 */
	public void setLanguageISO639IdentifierAndLexvoUrl(String languageISO639Identifier_) throws InvalidLanguageException {
		
		// check lexvo URL
		if (TikaTools.isLexvoUrl(languageISO639Identifier_)) {
			try {
				setLanguageISO639IdentifierAndLexvoUrl(new URL(languageISO639Identifier_));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			return;
		}
		
		// Verify string has only characters
		if (!TikaTools.isISO639LanguageCode(languageISO639Identifier_)) {
			this.languageISO639Identifier = "";
			this.lexvoUrl = null;
			throw new InvalidLanguageException("Error : The string '"+languageISO639Identifier_+"' could not be identified as ISO639 code !");
		} else {
			this.lexvoUrl = TikaTools.getLexvoUrlFromISO639_3Code(languageISO639Identifier_);
			this.languageISO639Identifier = TikaTools.getISO639_3CodeFromLexvoUrl(this.lexvoUrl);
		}
		// Determine language name
		try {
			this.languageNameEn = ParserISONames.getIsoCodes2Names().get(this.languageISO639Identifier).replace("'", "");
			} catch (Exception e){};
	}

	public Long getHitCount() {
		return hitCount;
	}

	public void setHitCount(Long hitCount) {
		this.hitCount = hitCount;
	}


	public boolean isSelected() {
		return selected;
	}
	
	public boolean getSelected() {
		return selected;
	}


	public void setSelected(boolean selected) {
		this.selected = selected;
	}


	public String getDetectionMethodShort() {
		if (getDetectionMethod() != null && getDetectionMethod() != DetectionMethod.MANUAL) {
			return StringUtils.substring("("+getDetectionMethod().name(), 0, 4)+")";
		}
		else {
			return "";
		}
	}


	public int getConllColumn() {
		return conllColumn;
	}


	public void setConllColumn(int conllColumn) {
		this.conllColumn = conllColumn;
	}


	public float getConfidence() {
		
		// Compute confidence from minProb, maxProb and averageProb		
		return this.averageProb;
	}


	public DetectionSource getDetectionSource() {
		return detectionSource;
	}


	public void setDetectionSource(DetectionSource detectionSource) {
		this.detectionSource = detectionSource;
	}


	public float getMinProb() {
		return minProb;
	}


	public void setMinProb(float minProb) {
		this.minProb = minProb;
	}


	public float getMaxProb() {
		return maxProb;
	}


	public void setMaxProb(float maxProb) {
		this.maxProb = maxProb;
	}


	public float getAverageProb() {
		return averageProb;
	}


	public void setAverageProb(float averageProb) {
		this.averageProb = averageProb;
	}


	public String getXmlAttribute() {
		return xmlAttribute;
	}


	public void setXmlAttribute(String xmlAttribute) {
		this.xmlAttribute = xmlAttribute;
	}


	public String getLanguageNameEn() {
		return languageNameEn;
	}


	public void setLanguageNameEn(String languageNameEn) {
		this.languageNameEn = languageNameEn;
	}
	
	public String getTableID() {
		return languageISO639Identifier+"#"+conllColumn+"#"+rdfProperty+"#"+xmlAttribute+"#"+detectionSource+"#"+detectionMethod+"#"+selected;
	}


	public String getRdfProperty() {
		return rdfProperty;
	}


	public void setRdfProperty(String rdfProperty) {
		this.rdfProperty = rdfProperty;
	}


	public Date getDate() {
		return updated;
	}


	public void setDate(Date updated) {
		this.updated = updated;
	}
	
	/**
	 * Return the value of the Date.getTime() function
	 * @return Date.getTime or 0 
	 */
	public Long getDateGetTime() {
		if (updated != null) {
			return updated.getTime();
		} else {
			return 0L;
		}
	}
	
	/** 
	 * Return short version YYYY-MM-DD for Date type
	 * @return
	 */
	public LocalDate getLocalDate() {
		return updated.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}


	public String getUpdateText() {
		return updateText;
	}


	public void setUpdateText(String updateText) {
		this.updateText = updateText;
	}
	
	
	public String getLanguageResultsAsString() {
		
		String results = "";
		results+="avg-prob "+getAverageProb()+",";
		results+="min-prob "+getMinProb()+",";
		results+="max-prob "+getMaxProb()+",";
		results+="hit-count "+getHitCount()+",";
		results+="diff-hit-types "+getDifferentHitTypes();
		
		return results;
	}
	
}
