package de.unifrankfurt.informatik.acoli.fid.types;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import de.unifrankfurt.informatik.acoli.fid.detector.ContentTypeDetector;
import de.unifrankfurt.informatik.acoli.fid.detector.TikaTools;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;

/**
 * Metadata container
 * @author frank
 *
 */
public class ResourceMetadata implements Serializable {
	
	private static final long serialVersionUID = -2271071631L;

	private String dcFormat = "";
	private String dctType = "";
	private String dcRights = "";
	private String dcLicense = "";	// TODO not implemented
	private String dcPublisher="";
	private String dcTitle="";
	private String ubTitle="";
	private String dcDescription="";
	private String dcCreator="";
	private String dcContributor="";
	private String dctLocation="";
	private Date   dctDate = new Date();
	private String year = "";
	private String emailContact = "";
	private String webpage = "";
	private String dctLanguageString = "";
	private String dcLanguageString = "";
	private HashSet<String> keywords = new HashSet<String>();
	private String dctSource = "";
	private String dctIdentifier = "";
	private MetadataSource metadataSource = MetadataSource.NONE;
	
	private ResourceFormat resourceFormat = ResourceFormat.UNKNOWN;
	private HashSet <URL> lexvoUrls = new HashSet <URL>();
	private String lang="";

	/*
	 *  Linghub properties
	 *  http://purl.org/dc/elements/1.1/title
	 *	http://purl.org/dc/terms/language
	 *	http://purl.org/dc/elements/1.1/rights
	 *	http://purl.org/dc/terms/type
	 *	http://purl.org/dc/elements/1.1/creator
	 *	http://purl.org/dc/elements/1.1/source
	 *	http://purl.org/dc/elements/1.1/contributor
	 *	http://purl.org/dc/elements/1.1/subject
	 *	http://purl.org/dc/elements/1.1/description
	 *	http://www.w3.org/ns/dcat#accessURL
	 *	http://www.w3.org/ns/dcat#contactPoint
	 */

	public ResourceMetadata() {}
	
	public ResourceMetadata (String dcLanguages, String dcFormat, String dctType) {
		
		this.dctType = dctType;
		this.setFormat(dcFormat);
		this.setDcLanguageString(dcLanguages);
	}
	
	
	public HashSet <URL> getLexvoUrls() {
		return lexvoUrls;
	}
	public void setLexvoUrls(HashSet <URL> lexvoUrls) {
		this.lexvoUrls = lexvoUrls;
	}
	
	public String getDcLanguageString() {
		return dcLanguageString;
	}
	
	public void setDcLanguageString(String s) {
		this.dcLanguageString = s;
		convertLanguageString(s);
	}
	
	private void convertLanguageString(String s) {
		
		s = s.trim().toLowerCase();
		if (s.isEmpty()) return;
		
		// remove enclosing double-quotes
		if (s.charAt(0) == '"' && s.charAt(s.length()-1) == '"') {
			s = s.substring(1, s.length()-1);
		}
		
		Utils.debug("parsing "+s);
		
		for (String language : s.split(",|;")) {
			
		lang = language.trim();
		
		URL lexvoUrl = null;
		
		
		// Case 1 : is lexvo url
		if (lang.startsWith("http://lexvo.org")
		||  lang.startsWith("http://www.lexvo.org")) {
			try {
				Utils.debug("found lexvo URL "+lang);
				lexvoUrl = new URL(lang);
			} catch (MalformedURLException e) {e.printStackTrace();
			}
		} else {
		// Case 2  : is iso language identifier
			if (lang.length() == 2 || lang.length() == 3) {
				lexvoUrl = TikaTools.getLexvoUrlFromISO639_3Code(lang);
			} else {
		// Case 3  : is complex language identifier 
		// Case 3a : and contains '/' (e.g. baq/eus) -> use first identifier (baq)
		// Case 3b : or contains '-' (e.g. sv-FI) -> use first identifier (sv)
			if (lang.contains("/") || lang.contains("-")) {
				lexvoUrl = TikaTools.getLexvoUrlFromISO639_3Code(lang.split("-|/")[0].trim());
			} else {
				if (!lang.isEmpty()) {
					// Case 4b : must be language name (and not iso-code)
				// TODO use SIL table (iso-639-3_20180123.tab) that has more entries than lexvo
					
				// too slow !
				//lexvoUrl = OptimaizeLanguageTools1.getISO639_3_CodeForLanguageName(lang);
				//Utils.debug("Found iso639-3 identifier from language descriptor "+lang+" : "+lexvoUrl);
				}
			}
			}
			}
		
			if (lexvoUrl != null) lexvoUrls.add(lexvoUrl);
		}
	}
	
	public String getFormat() {
		return dcFormat;
	}
	public void setFormat(String dcFormat) {
		this.dcFormat = dcFormat;
		this.resourceFormat = ContentTypeDetector.detectResourceFormatFromContentTypeString(dcFormat);
	}
	public String getType() {
		return dctType;
	}
	public void setType(String dctType) {
		this.dctType = dctType;
	}
	public ResourceFormat getResourceFormat() {
		return resourceFormat;
	}
	public void setResourceFormat(ResourceFormat resourceFormat) {
		this.resourceFormat = resourceFormat;
	}

	public String getLinghubLanguagesAsString() {
		
		String stringRepr = "";
		String isoCode="";
		HashSet<String> doneLanguages = new HashSet<String>();
		for (URL url : lexvoUrls) {
			isoCode = TikaTools.getISO639_3CodeFromLexvoUrl(url);
			if (!doneLanguages.contains(isoCode)) {
				doneLanguages.add(isoCode);
				stringRepr += isoCode +", ";
			}
		}
		if (!stringRepr.isEmpty()) {
			stringRepr = stringRepr.substring(0, stringRepr.length()-2);
		} else {
			stringRepr = "---";
		}
		return stringRepr;
	}

	public String getDctLanguageString() {
		return dctLanguageString;
	}

	public void setDctLanguageString(String s) {
		this.dctLanguageString = s;
		convertLanguageString(s);
	}


	public void setRights(String dcRights) {
		this.dcRights = dcRights;
	}
	

	public String getRights() {
		return this.dcRights;
	}
	

	public Date getDate() {
		return dctDate;
	}

	public void setDate(Date date) {
		this.dctDate = date;
	}

	public String getPublisher() {
		return dcPublisher;
	}

	public ArrayList<String> getPublisherList(){
		ArrayList<String> result = new ArrayList<String>();
		for (String c : Utils.filterNa(getPublisher()).split("[;,]")) {
			if (!c.trim().isEmpty()) result.add(c.trim());
		}
		return result;
	}
	
	public void setPublisher(String dcPublisher) {
		this.dcPublisher = dcPublisher;
	}

	public String getTitle() {
		return dcTitle;
	}

	public void setTitle(String dcTitle) {
		this.dcTitle = dcTitle;
	}

	public String getDescription() {
		return dcDescription;
	}

	public void setDescription(String dcDescription) {
		this.dcDescription = dcDescription;
	}

	public String getCreator() {
		return dcCreator;
	}

	public ArrayList<String> getCreatorList(){
		ArrayList<String> result = new ArrayList<String>();
		for (String c : Utils.filterNa(getCreator()).split("[;,]")) {
			if (!c.trim().isEmpty()) result.add(c.trim());
		}
		return result;
	}
	
	public void setCreator(String dcCreator) {
		this.dcCreator = dcCreator;
	}

	public String getLocation() {
		return dctLocation;
	}

	public void setLocation(String location) {
		this.dctLocation = location;
	}

	public String getContributor() {
		return dcContributor;
	}
	
	public ArrayList<String> getContributorList(){
		ArrayList<String> result = new ArrayList<String>();
		for (String c : Utils.filterNa(getContributor()).split("[;,]")) {
			if (!c.trim().isEmpty()) result.add(c.trim());
		}
		return result;
	}

	public void setContributor(String dcContributor) {
		this.dcContributor = dcContributor;
	}

	public String getYear() {
		return year;
	}

	public void setYear(String year) {
		this.year = year;
	}

	public String getEmailContact() {
		return emailContact;
	}

	public void setEmailContact(String contact) {
		this.emailContact = contact;
	}

	public String getWebpage() {
		return webpage;
	}

	public void setWebpage(String webpage) {
		this.webpage = webpage;
	}

	public MetadataSource getMetadataSource() {
		return metadataSource;
	}
	
	public void setMetadataSource(MetadataSource metadataSource) {
		this.metadataSource = metadataSource;
	}

	public String getKeywords() {
		
		String result="";
		for (String k : this.keywords) {
			if (k.trim().isEmpty()) continue;
			result+=","+k.trim();
		}
		if (result.isEmpty()) return "";
		else
		return result.substring(1, result.length());
		//Utils.debug("getKeywords :"+result+":"+result.length());
	}

	/**
	 * List of keywords with , delimiter
	 * @param keywords
	 */
	public void setKeywords(String keywords) {
		this.keywords.clear();
		for (String x : keywords.split(",")) {
			this.keywords.add(x);
		}
	}
	
	public void addKeyword(String keyword) {
		keyword=keyword.replaceAll(",", "");
		keyword=keyword.replaceAll("'", "");
		keyword=keyword.replaceAll("\"", "");
		this.keywords.add(keyword.trim());
	}
	

	public MetadataState getMetadataState() {
		
		if (this.metadataSource == MetadataSource.NONE) return MetadataState.EMPTY;
			
		if (!this.dcTitle.isEmpty() &&
			!this.dcDescription.isEmpty() &&
			!this.dcCreator.isEmpty() &&
			!this.year.isEmpty() &&
			!this.dcRights.isEmpty() &&
			!this.webpage.isEmpty() &&
			!this.emailContact.isEmpty()
			) return MetadataState.COMPLETE;
	
		if (!this.dcTitle.isEmpty() &&
			!this.dcDescription.isEmpty() &&
			!this.dcCreator.isEmpty() &&
			!this.year.isEmpty() 
			) return MetadataState.SUFFICIENT;

			return MetadataState.INCOMPLETE;
		}

	public String getUbTitle() {
		return ubTitle;
	}

	public void setUbTitle(String ubTitle) {
		this.ubTitle = ubTitle;
	}

	
	/**
	 * Return hashValue. The hashValue is 0 if no metadata was provided
	 * @return
	 */
	public int getHashCode() {
		String hashString="";
		hashString+="1"+dcFormat;
		hashString+="2"+dctType;
		hashString+="3"+dcRights;
		hashString+="4"+dcPublisher;
		hashString+="5"+dcTitle;
		hashString+="6"+ubTitle;
		hashString+="7"+dcDescription;
		hashString+="8"+dcCreator;
		hashString+="9"+dcContributor;
		hashString+="10"+dctLocation;
		hashString+="11"+year;
		hashString+="12"+webpage;
		hashString+="13"+dctLanguageString;
		hashString+="14"+dcLanguageString;
		hashString+="15"+metadataSource.name();
		hashString+="16"+dcLicense;
		hashString+="17"+dctSource;
		hashString+="18"+dctIdentifier;
		if (hashString.startsWith("123456789101112131415161718")) return 0;
		else 
		return hashString.hashCode();
	}

	
	
	
	
	
	public String asRawText() {
		String result = dcFormat+" "
						+dctType+" "
						+dcLicense+" "
						+dcRights +" "
						+dcPublisher+" "
						+dcTitle+" "
						+ubTitle+" "
						+dcDescription+" "
						+dcCreator+" "
						+dcContributor+" "
						+dctLocation+" "
						+dctDate.toString()+" "
						+year+" "
						+emailContact+" "
						+webpage+" "
						+dctLanguageString+" "
						+dcLanguageString+" "
						+getKeywords()+" "
						+getDctIdentifier()+" "
						+getDctSource()+" "
						+metadataSource+" "
						+resourceFormat.name()+" "
						//+lexvoUrls+" "
						+lang;
		//System.out.println(result.replace("\n", ""));
		return result.replaceAll("\n|\r", "");
	}

	// TODO not implemented
	public String getLicense() {
		return dcLicense;
	}

	// TODO not implemented
	public void setLicense(String license) {
		this.dcLicense = license;
	}

	public String getDctSource() {
		return dctSource;
	}

	public void setDctSource(String dctSource) {
		this.dctSource = dctSource;
	}

	

	public String getDctIdentifier() {
		return dctIdentifier;
	}

	public void setDctIdentifier(String dctIdentifier) {
		this.dctIdentifier = dctIdentifier;
	}

	@Override
	public String toString() {
		return "LinghubResource [dcFormat=" + dcFormat + ", dctType=" + dctType
				+ ", dcRights=" + dcRights + ", dcLicense=" + dcLicense
				+ ", dcPublisher=" + dcPublisher + ", dcTitle=" + dcTitle
				+ ", ubTitle=" + ubTitle + ", dcDescription=" + dcDescription
				+ ", dcCreator=" + dcCreator + ", dcContributor="
				+ dcContributor + ", dctLocation=" + dctLocation + ", dctDate="
				+ dctDate + ", year=" + year + ", emailContact=" + emailContact
				+ ", webpage=" + webpage + ", dctLanguageString="
				+ dctLanguageString + ", dcLanguageString=" + dcLanguageString
				+ ", keywords=" + keywords + ", dctSource=" + dctSource
				+ ", dctIdentifier=" + dctIdentifier + ", metadataSource="
				+ metadataSource + ", resourceFormat=" + resourceFormat
				+ ", lexvoUrls=" + lexvoUrls + ", lang=" + lang + "]";
	}

	
	
}
