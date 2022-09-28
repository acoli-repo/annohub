package de.unifrankfurt.informatik.acoli.fid.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.jena.ext.com.google.common.io.Files;

import de.unifrankfurt.informatik.acoli.fid.types.ResourceFormat;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;


/**
 * @author frank
 */

public class IndexUtils {
	
	public static final String FoundDocumentsInIndex = "000";
	public static final String NoDocumentsFoundInIndex = "888";
	public static final String ParseError = "666";
	public static final String FileTypeNotSupported = "777";
	
	// Encode file type endings with '.' and in lower case (upper case is tested automatically)
	public static final String [] sparqlEndpointType = {"sparql"};
	public static final String [] rdfFileType = {".nt",".ttl",".rdf",".owl",".nq",".n3",".ntriples"};
	public static final String [] conllFileType = {".conll", ".conllu", ".conllx"};
	public static final String [] tarFileType = {".tar", ".tar.gz", ".tgz", ".tar.bz2", ".tbz2", ".tbz", ".tar.xz", ".tar.lzma",".tgz"};
	public static final String [] gzipFileType = {".gz", ".gzip"};
	public static final String [] zip7zBz2RarArchiveFileType = {".zip", ".7z", ".bz2", ".rar"};
	public static final String [] htmlFileType = {".html",".htm",".xhtml"};
	public static final String [] jsonFileType = {".json"};
	public static final String [] phpFileType = {".php"};
	public static final String [] pdfFileType = {".pdf"};
	public static final String [] psFileType = {".ps"};
	public static final String [] xmlFileType = {".xml",".treex"};
	public static final String [] csvFileType = {".csv"};
	public static final String [] tsvFileType = {".tsv"};
	public static final String [] exelFileType = {".xls",".xlsx"};
	public static final String [] textFileType = {".txt",".doc",".docx",".odt"};
	public static final String [] graphicsFileType = {".jpg",".jpeg",".png",".tiff"};
	public static final String [] soundFileType = {".wav",".mp3",".ogg"};
	public static final String metaSharePrefix = "http://metashare.";
	public static final String metaShareDownloadMarker = "I agree to these licence terms and would like to download the resource.";
	public static final String [] blacklist = {
		"http://diglib.hab.de","http://paradisec.org.au/fieldnotes","dbpedia",
		"http://mlode.nlp2rdf.org/datasets/mlsa.nt.gz,http://gnoss.com/gnoss.owl,https://clarin-pl.eu/dspace/bitstream/handle/11321/39/czywieszki1.1.zip?sequence=1"
		};
	public static final String [] metadataBlacklist = {"http://linghub.lider-project.eu/clarin/Nederands_Instituut_voor_Beeld_en_Geluid_OAI_PMH_repository"};

	
	public static final String ERROR_UNCOMPRESSED_FILE_SIZE_LIMIT_EXCEEDED = "Resource exceeds FileSizeLimit";
	public static final String ERROR_COMPRESSED_FILE_SIZE_LIMIT_EXCEEDED ="Resource exceeds compressedFileSizeLimit";
	public static final String ERROR_DECOMPRESSION = "Decompression error";
	public static final String ERROR_FTP_SUPPORT = "FTP protocol not supported";
	public static final String ERROR_RESOURCE_UP_TO_DATE = "The resource is up-to-date";
	public static final String ERROR_UNKNOWN_HOST = "UnknownHostException";
	public static final String ERROR_HTTP_CONNECTION = "HttpHostConnectException";
	public static final String ERROR_TIMEOUT = "TimeoutException";
	public static final String ERROR_CONLL_INVALID = "Conll file format is invalid";
	public static final String ERROR_CONLL_FILE_TOO_SMALL = "Conll File too small";
	public static final String ERROR_OUT_OF_DISK_SPACE = "Available disk space not sufficient";
	public static final String ERROR_MAX_ARCHIVE_FILE_COUNT_EXCEEDED = "Archive file has more files than allowed";
	public static final String ERROR_FILE_TYPE_NOT_SUPPORTED = "The file type could not be handeled";
	public static final String ERROR_IN_RDF_VALIDATION = "The validation of the RDF did not succeed";
	public static final String ERROR_UNKNOWN_UPDATE_POLICY = "Configuration Error : value of updatePolicy not recognized !";
	public static final String ERROR_UPLOAD_FILE_SIZE_LIMIT_EXECEEDED = "Size of uploaded file exeeds limit";
	public static final String ERROR_UPLOAD_RESOURCE_COUNT_LIMIT_EXECEEDED = "Quota error : amount of allowed resources to be uploaded is execeeded";
	public static final String ERROR_UPLOAD_RESOURCE_FILE_COUNT_LIMIT_EXECEEDED = "Quota error : amount of allowed resources files to be uploaded is execeeded";
	public static final String ERROR_RESOURCE_IS_DUPLICATE = "Error : file is duplicate ";



	static final String linghubResourceQueries = 
			
				"PREFIX dcat: <http://www.w3.org/ns/dcat#>"+
				"PREFIX dct: <http://purl.org/dc/terms/>"+
				"PREFIX dc: <http://purl.org/dc/elements/1.1/>"+
				"PREFIX metashare: <http://purl.org/ms-lod/MetaShare.ttl#>"+
				"PREFIX rdfs: <http://rdfs.org/ns/void#>"+

				"SELECT ?accessUrl ?distribution ?dataset WHERE {"+
				  "?dataset rdf:type rdfs:Dataset. "+
				  "?dataset dcat:distribution ?distribution."+
				  "?distribution dcat:accessURL ?accessUrl ."+
				"}"+

				"### Querystart ###"+
				"PREFIX dcat: <http://www.w3.org/ns/dcat#>"+
				"PREFIX dct: <http://purl.org/dc/terms/>"+
				"PREFIX dc: <http://purl.org/dc/elements/1.1/>"+
				"PREFIX metashare: <http://purl.org/ms-lod/MetaShare.ttl#>"+
				"SELECT ?dataset ?distribution ?accessUrl  WHERE {"+
				"  ?dataset rdf:type dcat:Dataset. "+
				"  ?dataset dcat:contactPoint/metashare:affiliation/metashare:communicationInfo/dcat:distribution ?distribution."+
				" ?distribution dcat:accessURL ?accessUrl ."+
				"}";
	
	static final String linghubMetadataQueries =
			
				"PREFIX dcat: <http://www.w3.org/ns/dcat#>"+
				"PREFIX dct: <http://purl.org/dc/terms/>"+
				"PREFIX dc: <http://purl.org/dc/elements/1.1/>"+
				"PREFIX metashare: <http://purl.org/ms-lod/MetaShare.ttl#>"+
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>"+

				"SELECT ?dataset ?title ?description ?language ?rights ?date ?creator ?contributor ?subject ?homepage WHERE {"+

				  "?dataset dcat:distribution ?z."+
				  "?z dcat:accessURL !ACCESSURL! ."+
				  
				  "OPTIONAL {?dataset dct:title ?title}."+
				  "OPTIONAL {?dataset dct:description ?description}."+
				  "OPTIONAL {?dataset dct:language ?language}."+
				  "OPTIONAL {?dataset dct:rights ?rights}."+
				  "OPTIONAL {?dataset dct:date ?date}."+
				  "OPTIONAL {?dataset dct:creator ?creator}."+
				  "OPTIONAL {?dataset dct:contributor ?contributor}."+
				  "OPTIONAL {?dataset dct:subject ?subject}."+
				  "OPTIONAL {?dataset foaf:homepage ?homepage}.}"+
				  
				"### Querystart ###"+
				"PREFIX dcat: <http://www.w3.org/ns/dcat#>"+
				"PREFIX dct: <http://purl.org/dc/terms/>"+
				"PREFIX dc: <http://purl.org/dc/elements/1.1/>"+
				"PREFIX metashare: <http://purl.org/ms-lod/MetaShare.ttl#>"+
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>"+

				"SELECT ?title ?description ?language ?rights ?date ?creator ?contributor ?subject ?homepage WHERE {"+

				  "?v dcat:distribution ?z."+
				  "?z dcat:accessURL !ACCESSURL! ."+
				  
				  "OPTIONAL {?v dc:title ?title}."+
				  "OPTIONAL {?v dc:description ?description}."+
				  "OPTIONAL {?v dc:language ?language}."+
				  "OPTIONAL {?v dc:rights ?rights}."+
				  "OPTIONAL {?v dc:date ?date}."+
				  "OPTIONAL {?v dc:creator ?creator}."+
				  "OPTIONAL {?v dc:contributor ?contributor}."+
				  "OPTIONAL {?v dc:subject ?subject}."+
				  "OPTIONAL {?v foaf:homepage ?homepage}.}";
			
			
	
	/**
	 * Configuration defaults for FidConfig.xml. Set a default value to null if the application
	 * cannot start if it is missing.
	 */
	static HashMap <String, Object> configDefaults = new HashMap <String, Object> () {
		private static final long serialVersionUID = 1L;
	{
				put("Databases.GremlinServer.conf", null);
				put("Databases.GremlinServer.home", null);
				// only used if value missing in gremoin-server.yaml configuration file !
				put("Databases.GremlinServer.port", 8182);
				put("Databases.Registry.Neo4jDirectory", "");
				put("Databases.Data.Neo4jDirectory", null);
				put("Databases.Blazegraph.loadProperties", "");
				put("Databases.Postgres.usePostgres", false);
				put("Databases.Postgres.keyFile", "");
				put("Databases.Postgres.remoteHost", "");
				put("Databases.Postgres.database", "");
				put("Databases.Postgres.databaseUser", "");
				put("Databases.Postgres.databasePassword", "");
				put("Databases.Postgres.sshUser", "");
				put("Databases.deleteRdfDataAfterIndex", false);
				put("Databases.retryUnsuccessfulRdfData", false);
				put("Databases.restartTimeoutInMilliseconds", 10000);
				
				put("RunParameter.downloadFolder", null);
				put("RunParameter.htmlFolder", "");
				put("RunParameter.urlSeedFile","/tmp/urlSeedFile");
				put("RunParameter.urlPoolFile","/tmp/urlpool");
				put("RunParameter.urlFilter","CONLL,RDF,ARCHIVE");
				put("RunParameter.updatePolicy", "UPDATE_NEW");
				put("RunParameter.threads", 1);
				put("RunParameter.decompressionUtility","7z");
				put("RunParameter.RdfPredicateFilterOn",false);
				put("RunParameter.ExitProcessDiskSpaceLimit",1000);			// in Megabytes (Exit process if free disk space below)
				put("RunParameter.MaxArchiveFileCount",30000);				// Skip archive files which do contain more than MaxArchiveFileCount files
				put("RunParameter.compressedFileSizeLimit",2048576000); 	// in bytes (<!-- in bytes (1 GB = 1073741824 bytes)
				put("RunParameter.uncompressedFileSizeLimit",2048576000);	// in bytes 
				put("RunParameter.isoCodeMapDirectory",""); // TODO set priority
				put("RunParameter.XMLParserConfiguration.matchingMeasurement","RECALL");
				put("RunParameter.XMLParserConfiguration.sampleSentenceSize",10);
				put("RunParameter.startExternalQueue",true);
				put("RunParameter.OptimaizeExtraProfilesDirectory",null);
				put("RunParameter.OptimaizeManualProfilesDirectory",null);
				put("RunParameter.OptimaizeAnnotationModelsProfilesDirectory","");
				put("RunParameter.LexvoRdfFile","");
				put("RunParameter.RdfExportFile", "/tmp/FidExport.rdf");
				put("RunParameter.JsonExportFile", "/tmp/JsonExport.json");
				put("RunParameter.AnnohubRelease", "/tmp/AnnoHubDataset.rdf");
				put("RunParameter.RdfPredicateFilterOn", false);
				put("RunParameter.useBllOntologiesFromSVN", false);
				put("RunParameter.BLLOntologiesDirectory", "");
				put("RunParameter.convert2RdfXmlScript","/bash/convert2RdfXml");
				put("RunParameter.debugOutput", true);
				put("RunParameter.guiPropertiesFile", "");
				put("RunParameter.ServiceUploadDirectory", "/tmp");
				put("RunParameter.defaultResourcePermissions", "701");  // owner/group/world, 1=Read,2=Edit,4=Export
				put("RunParameter.cached", true);
				put("RunParameter.loadUnsuccessfull", false);
				put("RunParameter.initRdfExporterAtServerStart", true);
				put("RunParameter.checkBrokenLinksAtServerStart", false);
				put("RunParameter.exportBrokenLinks", false);
				put("RunParameter.checkBrokenLinksInterval", 0);
				put("RunParameter.publishRDFExportInterval", 0);
				put("RunParameter.JavaHome", null);
				put("RunParameter.QueueBackupFile", null);

				
				// (only for member account)
				put("Quotas.maxResourceUploads", 10);
				put("Quotas.maxResourceFiles", 100);
				put("Quotas.maxResourceUploadSize", 200);  // in MB

				put("Linghub.linghubDataDumpURL","http://linghub.org/linghub.nt.gz");
				put("Linghub.linghubQueries.resourceQueries",linghubResourceQueries);
				put("Linghub.linghubQueries.metadataQueries",linghubMetadataQueries);
				put("Linghub.statusCodeFilter","");				// deprecated ??
				put("Linghub.useQueries", false);
				put("Linghub.enabled", false);
				put("Linghub.forceUpdate", false);
				
				put("ActiveMQ.brokerUrl", "tcp://localhost:61616");
				put("Backup.directory", null);
				put("Backup.autobackupInterval", 0); // 0 means no autobackup, otherwise days between backup
				
				//put("OWL.BLL.ModelDefinitionsFile", "");
				put("OWL.BLL.BllOntology","https://valian.uni-frankfurt.de/svn/repository/intern/Virtuelle_Fachbibliothek/UB/OWL/BLLThesaurus/bll-ontology.rdf");
				put("OWL.BLL.BllLink","https://valian.uni-frankfurt.de/svn/repository/intern/Virtuelle_Fachbibliothek/UB/OWL/BLLThesaurus/bll-link.rdf");
				put("OWL.BLL.BllLanguageLink","https://valian.uni-frankfurt.de/svn/repository/intern/Virtuelle_Fachbibliothek/UB/OWL/BLLThesaurus/bll-language-link.ttl");
				put("OWL.modelUpdateMode", "manual");
				put("OWL.modelUpdateHitDeletePolicy", "manual");
				put("OWL.checkModelsOnlineAtStartup", false);
				put("OWL.checkModelsOnlineAtStartupStopOnFail", false);

				 
				// Sampling parameter    (for each folder of an language resource archive)
				// maxSamples          : (set -1 for unlimited samples) Maximum number of samples to be taken (from all folders)
				// thresholdForGood    : Stop parsing more files after thresholdForGood files have been parsed successfully
				// thresholdForBad     : Stop parsing more files after thresholdForGood files have been parsed unsuccessfully
				// activationThreshold : If the file count is smaller than the activationThreshold all files will be parsed
				
				put("Sampling.Rdf.maxSamples",100);
				put("Sampling.Rdf.activationThreshold",50);
				put("Sampling.Rdf.thresholdForGood",20);
				put("Sampling.Rdf.thresholdForBad",10);
				put("Sampling.Xml.maxSamples",15);
				put("Sampling.Xml.activationThreshold",10);
				put("Sampling.Xml.thresholdForGood",3);
				put("Sampling.Xml.thresholdForBad",2);
				put("Sampling.Conll.maxSamples",15);
				put("Sampling.Conll.activationThreshold",20);
				put("Sampling.Conll.thresholdForGood",3);
				put("Sampling.Conll.thresholdForBad",3);
				
				put("Processing.ConllParser.conllFileMinLineCount",10);
				put("Processing.ConllParser.conllFileMaxLineCount",-1);	// -1 = unlimited
				put("Processing.ConllParser.maxSampleSentenceSize",100);
				put("Processing.ConllParser.modelSampleSentenceMinTokens", 40);
				put("Processing.ConllParser.languageSampleSentences", 15);
				put("Processing.ConllParser.languageSampleSentencesMinTokenCount", 10);
				put("Processing.GenericXmlFileHandler.xmlValueSampleCount",10);
				put("Processing.GenericXmlFileHandler.makeConllMode", "sample"); // auto | sample | full
				put("Processing.GenericXmlFileHandler.makeConllSampleSentenceCount", 5000); // #sentences
				put("Processing.GenericXmlFileHandler.makeConllAutoMaxFileSize", 5); // in megabytes
				put("Processing.GenericXmlFileHandler.makeConllConverterChoice", "generic");
				// if makeConllMode=auto then use full conversion for files larger than makeConllAutoMaxFileSize megabytes and sample mode otherwise
				
				put("Processing.XMLAttributeEvaluator.processDuplicates",false);
				put("Processing.ModelEvaluator.autoDeleteConllModelsWithTrivialResults",false);
				
				/*put("AccountProperties.uploadResourceCountLimit.GUEST",20);
				put("AccountProperties.uploadResourceCountLimit.MEMBER",50);
				put("AccountProperties.uploadResourceCountLimit.ADMIN",-1);
				put("AccountProperties.uploadResourceFileCountLimit.GUEST",20);
				put("AccountProperties.uploadResourceFileCountLimit.MEMBER",50);
				put("AccountProperties.uploadResourceFileCountLimit.ADMIN",-1);
				put("AccountProperties.uploadTotalSizeLimit.GUEST",500); // total content size in MB
				put("AccountProperties.uploadTotalSizeLimit.MEMBER",5000);
				put("AccountProperties.uploadTotalSizeLimit.ADMIN",-1);*/
				
				put("Clarin.clarinQueries","SELECT title, description, resource_type, date, author, licence, publisher, language from metadata where link = 'ACCESSURL';");
				
				// legacy options
				put("SearchEngine.SERVICE_URI","http://localhost:3030/ds/data");
				put("SearchEngine.PATH-SearchTerms","/home/vifa/VifaRun/searchEngine/searchTerms.ttl");
				put("SearchEngine.PATH-SearchTermsConcise","/home/vifa/VifaRun/searchEngine/searchterms_concise.ttl");
				put("SearchEngine.PATH-OLiA-TDB","/home/vifa/olia-tdb/");
				put("SearchEngine.PATH-SearchEngine-TDB","/home/vifa/se-tdb/");
				put("SearchEngine.PATH-CONLL","/home/vifa/conll/");
				put("SearchEngine.resultFile","/home/vifa/VifaRun/searchEngine/searchEngineResults.ttl");
				put("SearchEngine.xmlExportFile","/home/vifa/VifaRun/searchEngine/ub-export.xml");
				put("SearchEngine.xmlExportBase","/home/vifa/VifaRun/searchEngine/llod-mods-base.xml");
				put("SearchEngine.conciseSearchTerms",true);
				
				put("OliaSVN.path","/home/vifa/svn/vifa-owl/BLLThesaurus/bll-ontology.ttl");
				//put("Clarin.path","/home/vifa/svn/vifa-owl/BLLThesaurus/bll-link.rdf");
				//put("Clarin.path","/home/vifa/svn/olia-sf/trunk/owl/stable/bll-link.rdf");
				put("OliaSVN.path","/home/vifa/svn/olia-sf/trunk/owl/stable/");
				put("OliaSVN.path","/home/vifa/svn/olia-sf/trunk/owl/core/");
				put("OliaSVN.path","/home/vifa/svn/olia-sf/trunk/owl/experimental/univ_dep/");
				put("OliaSVN.path","/home/vifa/svn/olia-sf/trunk/owl/experimental/lexinfo/");
				put("OliaSVN.path","/home/vifa/svn/olia-sf/trunk/owl/experimental/gold/");
				put("OliaSVN.path","/home/vifa/svn/olia-sf/trunk/owl/experimental/dcr/6.owl");
				put("OliaSVN.path","/home/vifa/svn/olia-sf/trunk/owl/experimental/dcr/dcr-link.rdf");
				//put("OliaSVN.exception","multext_east");
				put("OliaSVN.exception","");
				put("OliaSVN.clarinQueries","old");
				put("OliaSVN.oliaregex","((http://purl.org/olia/(olia)|(system)).*)");
				put("OliaSVN.bllregex","((http://data.linguistik.de/bll).*)");
				
				put("CONLL.col",3);
				put("CONLL.col",4);
				put("CONLL.col",5);
				put("CONLL.col",8);
				put("CONLL.tag","conll:POS");
				put("CONLL.tag","conll:UPOS");
				put("CONLL.tag","conll:XPOS");
				put("CONLL.tag","conll:CPOS");
				put("CONLL.tag","conll:POSTAG");
				put("CONLL.tag","conll:UPOSTAG");
				put("CONLL.tag","conll:XPOSTAG");
				put("CONLL.tag","conll:CPOSTAG");
				put("CONLL.tag","conll:FEAT");
				put("CONLL.tag","conll:FEATS");
				put("CONLL.tag","conll:EDGE");
				put("CONLL.tag","conll:DEP<");
				put("CONLL.tag","conll:DEPS");
				put("CONLL.tag","conll:DEPREL");
				put("CONLL.tag","conll:DEPRELS");
				put("CONLL.tag","conll:PDEPREL");
				put("CONLL.tag","conll:PDEPRELS");
				put("CONLL.amURL","http://ud-pos-all.owl</amURL");
				put("CONLL.amURL","http://ud-dep-all.owl");
				
				put("PrefixURIs.bll-skos","http://data.linguistik.de/bll/bll-thesaurus#");
				put("PrefixURIs.bll-owl","http://data.linguistik.de/bll/bll-ontology#");
				put("PrefixURIs.bll-link","http://purl.org/olia/bll-link.rdf#");
				put("PrefixURIs.bll-tit","http://data.linguistik.de/records/bll/");
				put("PrefixURIs.bll-tit-link","http://data.linguistik.de/bll/bll-index#");
				put("PrefixURIs.olia","http://purl.org/olia/olia.owl#");
				put("PrefixURIs.olia-top","http://purl.org/olia/olia-top.owl#");
				put("PrefixURIs.olia-system","http://purl.org/olia/system.owl#");
				put("PrefixURIs.rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#");
				put("PrefixURIs.rdfs","http://www.w3.org/2000/01/rdf-schema#");
				put("PrefixURIs.skos","http://www.w3.org/2004/02/skos/core#");
				put("PrefixURIs.owl","http://www.w3.org/2002/07/owl#");
				put("PrefixURIs.conll","http://ufal.mff.cuni.cz/conll2009-st/task-description.html#");
				put("PrefixURIs.dcr","http://www.isocat.org/ns/dcr.rdf#");
				put("PrefixURIs.void","http://rdfs.org/ns/void#");
				put("PrefixURIs.xsd","http://www.w3.org/2001/XMLSchema#");
				put("PrefixURIs.afn","http://jena.hpl.hp.com/ARQ/function#");
	}};
	
	



	
	/**
	 * Filter resource by its URL extension and set the associated ResourceFormat
	 * @param resourceMap
	 * @param suffixes Filter URLs by suffix
	 * @param rf set this ResourceFormat for found resources
	 * @return resources that match the given filter
	 */
	private static HashSet<ResourceInfo> filterResources(
		
		HashMap <String, ResourceInfo> resourceMap, String[] suffixes,
		ResourceFormat rf) {		
		
		HashSet <ResourceInfo> out = new HashSet <ResourceInfo>();
		HashSet <String> usedKeys = new HashSet <String> ();
		boolean ok;
		for (String url : resourceMap.keySet()) {
			for (String suffix : suffixes) {
				
				ok = false;
				
				if (!url.startsWith("file:")) {
				try {
					URI uri = URI.create(url);
					if ((uri.getPath() != null && uri.getPath().toLowerCase().endsWith(suffix)) 
					||	(uri.getQuery() != null && uri.getQuery().toLowerCase().endsWith(suffix))
					||  (uri.getFragment() != null && uri.getFragment().toLowerCase().endsWith(suffix)))
					{
						ok = true;
					}
					} catch (Exception e) {
						//System.out.println(url);
					}
				} 
				else {
					if (new File(url).getName().toLowerCase().endsWith(suffix)) {
						ok = true;
					}
				}
				if (ok) {
					ResourceInfo ri = resourceMap.get(url);
					ri.setResourceFormat(rf);
					out.add(ri);
					usedKeys.add(url);
				}
			}
		}
		
		// remove filtered resources from resource map
		for (String key : usedKeys) {
			resourceMap.remove(key);
		}

		return out;
	}
	
	
	
	/**
	 * Filter URLs which have one of the given suffixes
	 * @param Set with URLs
	 * @param cut If true then remove found extension from URL
	 * @param ext Array with filter extensions
	 * @return URLs that have ext as suffix
	 */
	public static HashSet <String> filterSuffix (HashSet <String> set, String [] suffixes) {
		HashSet <String> out = new HashSet <String>();
		
		for (String url : set) {
			for (String suffix : suffixes) {
				
				try {
				if (URI.create(url).getPath().toLowerCase().endsWith(suffix) ||
					URI.create(url).getQuery().toLowerCase().endsWith(suffix)) {
						out.add(url);
				}
				} catch (Exception e) {
					//System.out.println(url);
				}
			}
		}
		
		return out;
	}
	
	
	/**
	 * Filter URLs which have one of the given prefixes
	 * @param Set with URLs
	 * @param cut If true then remove found extension from URL
	 * @param ext Array with filter extensions
	 * @return URLs that have ext as suffix
	 */
	public static HashSet <String> filterPrefix (HashSet <String> set, String [] prefix) {
		HashSet <String> out = new HashSet <String>();
		
		for (String url : set) {
			for (String suffix : prefix) {
				
				try {
				if (URI.create(url).getPath().toLowerCase().startsWith(suffix)) {
						out.add(url);
				}
				} catch (Exception e) {
					//System.out.println(url);
				}
			}
		}
		
		return out;
	}
	
	

	public static boolean fileIsCompressed (String file, String decompressionUtility) {
		boolean hasArchiveExt = (!filterSuffix (
				new HashSet<String>(Arrays.asList(file)),
				(String []) ArrayUtils.addAll(ArrayUtils.addAll(zip7zBz2RarArchiveFileType,tarFileType),gzipFileType)).isEmpty());
		if (hasArchiveExt) {
			// filename has no archive format extension
		return true;
		} else {
			if(FilenameUtils.getExtension(file).isEmpty()) {
			// filename has no extension -> check file type with 7z (samples file)
			//return ScriptUtils.isArchive(file, decompressionUtility);
			return false;
		} else {
			// filename has other extension -> file has no archive format
			return false;
		}
		}
	}
	
	
	public static boolean fileHasExtension (String file, String [] extensions) {
		return (!filterSuffix (
				new HashSet<String>(Arrays.asList(file)),extensions).isEmpty());
	}
	
	
	public static boolean fileIsLoadable (String file, String [] extensions) {
		return (!filterSuffix (
				new HashSet<String>(Arrays.asList(file)),
				(String []) ArrayUtils.addAll(ArrayUtils.addAll(
				ArrayUtils.addAll(zip7zBz2RarArchiveFileType,tarFileType),gzipFileType),rdfFileType)).isEmpty());
	}
	
	
	
	
	/**
	 * Resource filter which uses two extension lists by testing any combination from them <p>
	 * (e.g. filename.nt.gz) where suffix = .gz && preSuffix = .nt
	 * @param resources
	 * @param suffix
	 * @param preSuffix 
	 * @param rf set this format in filtered resources
	 * @return resources that match the given filter
	 */
	public static HashSet <ResourceInfo> filter2 (HashMap <String, ResourceInfo> resources, String [] suffix, String [] preSuffix, ResourceFormat rf) {
		
		String [] combinedSuffix = new String [suffix.length * preSuffix.length];
		int i = 0;
		for (String psf : preSuffix) {
			for (String sf : suffix) {
				combinedSuffix[i] = psf+sf;
				i++;
			}
		}
		
		return filterResources(resources, combinedSuffix, rf);
	}
	
	
	/**
	 * String filter which uses two extension lists by testing any combination from them <p>
	 * (e.g. filename.nt.gz) where suffix = .gz && preSuffix = .nt
	 * @param set String set
	 * @param suffix Array with suffixes
	 * @param preSuffix Array with preSuffixes 
	 * @return
	 */
	public static HashSet <String> filter2 (HashSet <String> set, String [] suffix, String [] preSuffix) {
		
		String [] combinedSuffix = new String [suffix.length * preSuffix.length];
		int i = 0;
		for (String psf : preSuffix) {
			for (String sf : suffix) {
				combinedSuffix[i] = psf+sf;
				i++;
			}
		}
		
		return filterSuffix(set, combinedSuffix);
	}
	
	
	
	/**
	 * Function for filtering resources that are sparql endpoints
	 * @param resources
	 * @param rf set this format in filtered resources
	 * @return resources that match the given filter
	 */
	public static HashSet <ResourceInfo> filterSparql(HashMap <String,ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, sparqlEndpointType, rf);
	}
	
	
	/**
	 * Function for filtering URLs that show SPARQL endpoints
	 * @return Set with URLs that are sparql endpoints
	 */
	public static HashSet <String> filterSparql(HashSet <String> set) {
		return filterSuffix (set, sparqlEndpointType);
	}
	
	
	/**
	 * Function for filtering URLs that show gzip format
	 * @param resources
	 * @param rf set this format in filtered resources
	 * @return resources that match the given filter
	 */
	public static HashSet <ResourceInfo> filterGzip (HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, gzipFileType, rf);
	}
	
	
	/**
	 * Function for filtering URLs that are in gzip format
	 * @param Set of file path
	 * @return List of gzipped resources
	 */
	public static HashSet <String> filterGzip (HashSet <String> set) {
		return filterSuffix (set, gzipFileType);
	}
	
	
	
	/**
	 * Function for filtering resources that are in tar or (gziped tar) format
	 * @param resources
	 * @param rf set this format in filtered resources
	 * @return resources that match the given filter
	 */
	public static HashSet <ResourceInfo> filterTarNgz (HashMap <String,ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, tarFileType, rf);
	}
	
	
	/**
	 * Function for filtering URLs that are in tar or (zipped tar) format
	 * @param Set of file path
	 * @return List resources in tar format
	 */
	public static HashSet <String> filterTarNgz (HashSet <String> set) {
		return filterSuffix (set, tarFileType);
	}
	
	
	
	/**
	 * Function for filtering URLs that show a archive format other than gzip or tar
	 * @param resources
	 * @param rf set this format in filtered resources
	 * @return resources that match the given filter
	 */
	public static HashSet <ResourceInfo> filterZip7zBz2RarArchive(HashMap <String,ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, zip7zBz2RarArchiveFileType, rf);
	}
	
	
	/**
	 * Function for filtering URLs that show a archive format other than gzip or tar
	 * @param URLs
	 * @return Set of resources in archive format other than gzip
	 */
	public static HashSet <String> filterZip7zBz2RarArchive(HashSet <String> set) {
		return filterSuffix (set, zip7zBz2RarArchiveFileType);
	}
	
	
	/**
	 * Function for filtering URLs that show a archive format
	 * @param URLs
	 * @return Set of resources which relate to archive files
	 */
	public static HashSet <String> filterArchive(HashSet <String> set, String decompressionUtility) {
		
		HashSet <String> result = new HashSet <String> ();
		for (String file : set) {
			if (fileIsCompressed(file, decompressionUtility)) {
				result.add(file);
			}
		}
		
		return result;
	}
	
	
	/**
	 * Function for filtering URLs that show a RDF format
	 * @return List of RDF resources
	 */
	public static HashSet <String> filterRdf(HashSet <String> set) {
		return filterSuffix (set, rdfFileType);
	}
	
	
	/**
	 * Function for filtering URLs that show a XML format
	 * @return List of RDF resources
	 */
	public static HashSet <String> filterXml(HashSet <String> set) {
		return filterSuffix (set, xmlFileType);
	}
	
	
	/**
	 * Function for filtering URLs that show a RDF format
	 * @param resources
	 * @param rf set this format in filtered resources
	 * @return resources that match the given filter
	 */
	public static HashSet <ResourceInfo> filterRDF(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, rdfFileType, rf);
	}
	
	
	/**
	 * Function for filtering URLs that show a CONLL format
	 * @param resources
	 * @param rf set this format in filtered resources
	 * @return resources that have conll format
	 */
	public static HashSet <ResourceInfo> filterCONLL(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, conllFileType, rf);
	}
	
	
	/**
	 * Function for filtering URLs that show a HTML format
	 * @param resourceMp
	 * @param rdf
	 */
	public static HashSet <ResourceInfo> filterHTML(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, htmlFileType, rf);
		
	}
	
	/**
	 * Function for filtering URLs that show a HTML format
	 * @param resourceMp
	 * @param assigned format to all filtered resources
	 */
	public static HashSet <ResourceInfo> filterJSON(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, jsonFileType, rf);
		
	}
	
	/**
	 * Function for filtering URLs that show a PDF format
	 * @param resourceMp
	 * @param assigned format to all filtered resources
	 */
	public static HashSet <ResourceInfo> filterPDF(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, pdfFileType, rf);
		
	}
	
	/**
	 * Function for filtering URLs that show a Postscript format
	 * @param resourceMp
	 * @param assigned format to all filtered resources
	 */
	public static HashSet <ResourceInfo> filterPostscript(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, psFileType, rf);
		
	}
	
	
	/**
	 * Function for filtering URLs that show a XML format
	 * @param resourceMp
	 * @param assigned format to all filtered resources
	 */
	public static HashSet <ResourceInfo> filterXML(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, xmlFileType, rf);
		
	}
	
	
	/**
	 * Function for filtering URLs that show an archive format
	 * @param resourceMp
	 * @param assigned format to all filtered resources
	 */
	public static HashSet <ResourceInfo> filterARCHIVE(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, (String[])ArrayUtils.addAll(zip7zBz2RarArchiveFileType,ArrayUtils.addAll(gzipFileType, tarFileType)), rf);
		
	}
	
	
	
	/**
	 * Function for filtering URLs that show a TEXT format
	 * @param resourceMp
	 * @param assigned format to all filtered resources
	 */
	public static HashSet <ResourceInfo> filterText(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, textFileType, rf);
		
	}
	
	/**
	 * Function for filtering URLs that show a CSV format
	 * @param resourceMp
	 * @param assigned format to all filtered resources
	 */
	public static HashSet <ResourceInfo> filterCSV(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, csvFileType, rf);
		
	}
	
	
	/**
	 * Function for filtering URLs that show a TSV format
	 * @param resourceMp
	 * @param assigned format to all filtered resources
	 */
	public static HashSet <ResourceInfo> filterTSV(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, tsvFileType, rf);
		
	}
	
	/**
	 * Function for filtering URLs that show Exel format
	 * @param resourceMp
	 * @param assigned format to all filtered resources
	 */
	public static HashSet <ResourceInfo> filterExel(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, exelFileType, rf);
		
	}
	
	/**
	 * Function for filtering URLs that show a graphics format
	 * @param resourceMp
	 * @param assigned format to all filtered resources
	 */
	public static HashSet <ResourceInfo> filterGraphics(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, graphicsFileType, rf);
		
	}
	
	/**
	 * Function for filtering URLs that show a sound format
	 * @param resourceMp
	 * @param assigned format to all filtered resources
	 */
	public static HashSet <ResourceInfo> filterSound(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, soundFileType, rf);
		
	}
	
	/**
	 * Recursively get files from root directory
	 * @param root Directory
	 * @return Set with files
	 */
	public static HashSet <String> listRecFilesInDir (File root) {
		
		if (!root.exists() || !root.isDirectory()) return null;
		
		HashSet <String> allFiles = new HashSet <String> ();
		Queue<File> dirs = new LinkedList<File>();
		dirs.add(root);
		
		while (!dirs.isEmpty()) {
		  for (File f : dirs.poll().listFiles()) {
		    if (f.isDirectory()) {
		      dirs.add(f);
		    } else if (f.isFile()) {
		      allFiles.add(f.getAbsolutePath());
		    }
		  }
		}
		
		return allFiles;
	}
	
	
	/**
	 * Convert a list of file path to a map where files are sorted by their parent folder
	 * @param listOfFilePath
	 * @return Map folderPath -> filePath
	 */
	public static HashMap <String, ArrayList<String>> convertFileList2FolderMap(HashSet<String> listOfFilePath) {
		
		HashMap <String, ArrayList<String>> result = new HashMap <String, ArrayList<String>>();
		String key;
		for (String path : listOfFilePath) {
			key = (new File(path)).getParent();
			if (!result.containsKey(key)) {
				ArrayList <String> filesInDir = new ArrayList<String>();
				filesInDir.add(path);
				result.put(key, filesInDir);
			} else {
				ArrayList <String> filesInDir = result.get(key);
				filesInDir.add(path);
				result.put(key, filesInDir);
			}
		}
		return result;
	}
	
	
	
	/**
	 * Verify configuration file for missing parameter (does not verify values though)
	 * @param config Configuration 
	 * @return parameter set is complete
	 */
	public static boolean checkConfigAndSetDefaultValues(XMLConfiguration config) {

		// print config default values in file
		printConfiguration(config);
		
		boolean complete = true;
		for (String param : configDefaults.keySet()) {
			
			if (!config.containsKey(param)) {
				
				// Try using the default parameter
				if (configDefaults.get(param) != null) {
					config.addProperty(param, configDefaults.get(param));
				} else {
					complete = false;
					System.out.println("Configuration error : parameter "+param+" is missing !");
				}
			}
		}
		
		
		if (!config.getBoolean("Linghub.enabled")) {
			config.setProperty("Linghub.useQueries", false);
		}
		
		// with default values
		// printConfiguration(config);
				
		return complete;
	}
	
	
	
	private static void printConfiguration(XMLConfiguration config) {
		
		System.out.println("*******************************");
		System.out.println("* Using configuration options *");
		System.out.println("*******************************");
		
		Iterator<String> iterator = config.getKeys();
		while (iterator.hasNext()) {
			String key = iterator.next();
			System.out.print("# "+key+" :");
			System.out.println(config.getProperty(key));
		}
	}


	
	/**
	 * URL validation using org.apache.commons.validator.Validator
	 * @param url
	 * @return
	 */
	public static boolean isValidURL(String url) {
		
		try {
		if (url.startsWith("file:")) return true;
		
		UrlValidator urlValidator = new UrlValidator(UrlValidator.ALLOW_ALL_SCHEMES + UrlValidator.ALLOW_LOCAL_URLS);
		if (urlValidator.isValid(url)) {
		   return true;
		} else {
		   return false;
		}
		} catch (Exception e){e.printStackTrace();}
		return false;
	}
	
	
	
	/**
	 * Check if an url uses the file protocol
	 * @param url
	 * @return true if protocol is file
	 */
	public static boolean urlHasFileProtocol(String urlString) {
		
		try {
			URL url = new URL(urlString);
			if (url.getProtocol().equals("file")) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e){}
		return false;
	}
	
	
	/**
	 * Convert /path or file:/path or file://path -> file:///path
	 * @param url
	 * @return
	 */
	public static String checkFileURL (String url) {
		
		if (url == null || url.trim().isEmpty()) return null;
		
		if  (!url.startsWith("http://")
		&&	 !url.startsWith("https://")
		&&	 !url.startsWith("file://")
		&&	 !url.startsWith("ftp://")
		&&	 !url.startsWith("urn:")
		&&	 !url.startsWith("ssh://")) {
			
			try {
				//System.out.println("what "+url);
				url = "file://"+new URL (url).getPath();
			} catch (MalformedURLException e) {
				Utils.debug("checkFileURL ERROR "+ url+" !!!!!!!!!!!!!!!!!");
				// TODO Auto-generated catch block
				//e.printStackTrace();
				return null;
			}
		}
		
		return url;
	}

/**
 * Filter resource by Scheme.
 * @param resourceMap
 * @param schemes List of allowed schemes
 * @return Resources which have scheme
 */
public static HashSet <ResourceInfo> filterResourcesWithScheme(HashMap<String, ResourceInfo> resourceMap, String [] schemes) {
		
		HashSet <ResourceInfo> filteredResources = new HashSet <ResourceInfo> ();
		HashSet <String> usedKeys = new HashSet <String> ();
		
		for (ResourceInfo rsi : resourceMap.values()) {
			//String uriScheme = URI.create(new File(rsi.getDataURL()).getPath()).getScheme();
			for (String s : schemes) {
			if (rsi.getDataURL().startsWith(s+":")) {
				// fails with some URLs which start url: but from obviously different charset
				filteredResources.add(rsi);
				usedKeys.add(rsi.getDataURL());
				break;
				}
			}
		}
		// remove filtered resources from resource map
		for (String key : usedKeys) {
			resourceMap.remove(key);
			}
		
		return filteredResources;
	}


/**
 * Filter resources which do not have a extension in their URL and set the ResourceFormat to HTML
 * @param resourceMap
 * @param format ResourceFormat will be set for each filtered resource
 * @return Resources without extension
 */
public static HashSet <ResourceInfo> filterResourcesWithoutExtension(HashMap<String, ResourceInfo> resourceMap, ResourceFormat format) {
		
		HashSet <ResourceInfo> filteredResources = new HashSet <ResourceInfo> ();
		HashSet <String> usedKeys = new HashSet <String> ();
		
		for (ResourceInfo rsi : resourceMap.values()) {
			try {
			if (!new File(URI.create(rsi.getDataURL()).getPath()).getName().contains(".")) {
				rsi.setResourceFormat(format);
				filteredResources.add(rsi);
				usedKeys.add(rsi.getDataURL());
			}} catch (Exception e) {}
		}
		
		// remove filtered resources from resource map
		for (String key : usedKeys) {
			resourceMap.remove(key);
			}
		
		return filteredResources;
	}
	
	
	/**
	 * Filter resources by ResourceFormat
	 * @param resourceMap
	 * @return resources which have the given resource format
	 */
	public static HashSet <ResourceInfo> filterResourcesWithFormat(HashMap<String, ResourceInfo> resourceMap, ResourceFormat rf) {
		
		HashSet <ResourceInfo> filteredResources = new HashSet <ResourceInfo> ();
		HashSet <String> usedKeys = new HashSet <String> ();
		
		for (ResourceInfo rsi : resourceMap.values()) {
			if (rsi.getResourceFormat().equals(rf)) {
				filteredResources.add(rsi);
				usedKeys.add(rsi.getDataURL());
			}
		}
		
		// remove filtered resources from resource map
		for (String key : usedKeys) {
			resourceMap.remove(key);
			}
		
		return filteredResources;
	}
	
	
	
	/**
	 * Filter resources which belong to META-SHARE
	 * @param resourceMap
	 * @return META-SHARE resources
	 */
	public static HashSet <ResourceInfo> filterMetaShare(
			HashMap<String, ResourceInfo> resourceMap) {
			
		HashSet <ResourceInfo> metaShareResources = new HashSet <ResourceInfo> ();
		HashSet <String> usedKeys = new HashSet <String> ();
		
		for (ResourceInfo rsi : resourceMap.values()) {
			String url = rsi.getDataURL();
			if (url.startsWith(IndexUtils.metaSharePrefix)) {
				try {
					rsi.setResourceFormat(ResourceFormat.METASHARE);
					
					/*
					// Parse URL type
					if (url.contains("/browse/")) {
						/* change metashare URL to download page
						   Example : http://metashare.metanet4u.eu/repository/browse/
						   acopost-a-collection-of-pos-taggers/
						   acae1ab62f3e11e2a2aa782bcb074135cbaf365868fe4aecb947bcf617c8395b/
						   
						   newUrl = metaSharePrefix+"/download/"+acae1ab62f3e11e2a2aa782bcb074135cbaf365868fe4aecb947bcf617c8395b/
						   
						rsi.setDataURL(new URL (IndexUtils.metaSharePrefix+"/download/"+new File(url).getName()).toString());
					} else {
					if (url.contains("/download/")) {
					}
					}
					*/

					metaShareResources.add(rsi);
					usedKeys.add(rsi.getDataURL());
				} catch (Exception e) {e.printStackTrace();}
			}
		}
		
		// remove filtered resources from resource map
		for (String key : usedKeys) {
			resourceMap.remove(key);
			}
		
		return metaShareResources;
	}
	
	
	/**
	 * Function for filtering URLs that are in CoNLL format
	 * @return List of CoNLL resources
	 */
	public static HashSet <String> filterConll(HashSet <String> set) {
		return filterSuffix (set, conllFileType);
	}
	
	
	
	public static boolean unpackFile (File file, File downloadFolder, String decompressionUtility, XMLConfiguration config) {
  
		HashSet <String> compressedFiles = new HashSet <String> ();
		long fileCount = 0;
		
		try {
		// Copy a local file to the download folder
		if (!file.getParent().equals(downloadFolder.getAbsolutePath())) {
			System.out.println("Copy : "+file+ " -> "+new File (downloadFolder,file.getName()).getAbsolutePath());
			Files.copy(file, new File (downloadFolder,file.getName()));
			
			// initialize compressed file list
			compressedFiles.add(new File (downloadFolder,file.getName()).getAbsolutePath());
		} else {
			// initialize compressed file list
			compressedFiles.add(file.getAbsolutePath());
		}

		// Recursively expand compressed file
		int stopper = 0;
		while (!compressedFiles.isEmpty()) {
		
		for (String filePath : compressedFiles) {
			fileCount += ScriptUtils.unpack7z(filePath, decompressionUtility);
			if (fileCount > config.getLong("RunParameter.MaxArchiveFileCount")) {
				System.out.println(IndexUtils.ERROR_MAX_ARCHIVE_FILE_COUNT_EXCEEDED+ "- limit is "+config.getLong("RunParameter.MaxArchiveFileCount"));
				return false;
			}
			}
			
			// initialize next round of unpacking
			compressedFiles = IndexUtils.filterArchive(IndexUtils.listRecFilesInDir(downloadFolder), decompressionUtility);
			
			//for (String x : compressedFiles) 
			//	System.out.println(x);
			
			stopper ++;
			// anything wrong ?
			if (stopper > 20) break;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	
	/**
	 * Neu schreiben ohne Maps etc. !!!
	 * @param resourceInfo
	 * @return
	 */
	public static ResourceFormat determineResourceFormat(ResourceInfo resourceInfo) {
		
		if (resourceInfo.getDataURL() == null) return null;
		
		HashMap <String, ResourceInfo> resource = new HashMap <String, ResourceInfo>();
		HashSet <ResourceInfo> result = new HashSet <ResourceInfo> ();
		resource.put(resourceInfo.getDataURL(), resourceInfo);
		
		// filter functions set ResourceFormat in resourceInfo accordingly
		result.addAll(filterCONLL(resource, ResourceFormat.CONLL));
		result.addAll(filterRDF(resource, ResourceFormat.RDF));
		result.addAll(filterXML(resource, ResourceFormat.XML));
		result.addAll(filterARCHIVE(resource, ResourceFormat.ARCHIVE));
		result.addAll(filterHTML(resource, ResourceFormat.HTML));
		
		if (result.iterator().hasNext()) {
			return result.iterator().next().getResourceFormat();
		} else {
			return ResourceFormat.UNKNOWN;
		}
	}
	
	
	/**
	 * Get the format of a file. Only recognizes conll, rdf, xml, tsv and csv formats, returns unknown
	 * otherwise !
	 * @param resourceInfo with FileInfo
	 * @return ResourceFormat
	 */
	// TODO add other file types
	public static ResourceFormat determineFileFormat(ResourceInfo resourceInfo) {
		
		HashMap <String, ResourceInfo> resource = new HashMap <String, ResourceInfo>();
		HashSet <ResourceInfo> result = new HashSet <ResourceInfo> ();
		resource.put("file:/"+resourceInfo.getFileInfo().getAbsFilePath(), resourceInfo);
		
		// filter functions set ResourceFormat in resourceInfo accordingly
		result.addAll(filterCONLL(resource, ResourceFormat.CONLL));
		result.addAll(filterRDF(resource, ResourceFormat.RDF));
		result.addAll(filterXML(resource, ResourceFormat.XML));
		result.addAll(filterTSV(resource, ResourceFormat.TSV));
		result.addAll(filterCSV(resource, ResourceFormat.CSV));
		//result.addAll(filterARCHIVE(resource, ResourceFormat.ARCHIVE));
		//result.addAll(filterHTML(resource, ResourceFormat.HTML));
		
		if (result.iterator().hasNext()) {
			return result.iterator().next().getResourceFormat();
		} else {
			return ResourceFormat.UNKNOWN;
		}
	}
	
	
	/**
	 * Retrieve lineCount lines from file
	 * @param resourceInfo
	 * @param lineCount
	 * @return
	 */
	public static String getFileSample(ResourceInfo resourceInfo, int lineCount) {
		
		int maxTextSampleLength = 3000; // TODO : new option maxSampleTextChars
		String fileSample = "";
		final int preroll=0;
		try {

			BufferedReader br = new BufferedReader(new FileReader(resourceInfo.getFileInfo().getAbsFilePath()));
			String line;
			int lineCounter = 0;
		    while ((line = br.readLine()) != null && lineCounter++ < lineCount + preroll) {
		       if(lineCounter > preroll) {
		    	   fileSample += line+"\n";
		       }
		    }
		    br.close();
		    } catch (Exception e) {e.printStackTrace();}
		
		String sampleText = StringUtils.substring(fileSample, 0, maxTextSampleLength);
		
		return sampleText;
	}
	
	
	public static String string2Hex(String text) {
	    return DatatypeConverter.printHexBinary(text.getBytes(StandardCharsets.UTF_8));
	}

	public static String hex2String(String hexString) {
	    return new String(DatatypeConverter.parseHexBinary(hexString));
	}
	
	
	
	public static void main (String [] args) {
		
		String url = "https://www.clarin.si/repository/xmlui/bitstream/handle/11356/1431/ParlaMint-LV.ana.tgz?sequence=31";
		HashMap <String, ResourceInfo> resourceMp = new HashMap <String, ResourceInfo>();
		resourceMp.put(url, new ResourceInfo());
		//IndexUtils.filterRDF(resourceMp, ResourceFormat.RDF);
		
		String [] suffixes = {".tar", ".tar.gz", ".tgz", ".tar.bz2", ".tbz2", ".tbz", ".tar.xz", ".tar.lzma",".tgz"};

		boolean ok=false;
		
		for (String suffix : suffixes) {
			
			ok = false;
			
			if (!url.startsWith("file:")) {
			try {
				URI uri = URI.create(url);
				System.out.println(uri.getPath());
				System.out.println(uri.getQuery());
				System.out.println(uri.getFragment());
				if ((uri.getPath() != null && uri.getPath().toLowerCase().endsWith(suffix)) 
				||	(uri.getQuery() != null && uri.getQuery().toLowerCase().endsWith(suffix))
				||  (uri.getFragment() != null && uri.getFragment().toLowerCase().endsWith(suffix)))
				{
					ok = true;
				}
				} catch (Exception e) {
					//System.out.println(url);
				}
			} 
			else {
				if (new File(url).getName().toLowerCase().endsWith(suffix)) {
					ok = true;
				}
			}
			if (ok) {
				System.out.println("url "+ url+ "\nsuccess");
			}
		}

		
//		String input = "hgkljhil";
//		String encode = string2Hex(input);
//		encode = "3041333130394541393938373039354630393546303934333446344534413039343332443039363336333039354630413332303944304246443042454431383145413939393144304242443042304431414444313832443138413039354630393444364636463634334434393645363437433445373536443632363537323344353036433735373237433530363537323733364636453344333337433534363536453733363533443530373236353733374335363635373236323436364637323644334434363639364537433536364636393633363533443431363337343039353634353532343230393536324430393732364636463734303935463041333330394430424144313841303935463039354630393431343435303039353232443039363336313733363530393546304133343039443042444430423544304243443042454431383330393546303934333631373336353344343436313734374334373635364536343635373233443444363137333633374334453735364436323635373233443533363936453637374335303635373237333646364533443333374335303732364636453534373937303635334435303732373330393530353234463445303935303730303936393646363236413039354630413335303944304245443138334431383744304235443042444430423844304241454139393931303935463039343336313733363533443431363336333743343736353645363436353732334434443631373336333743344537353644363236353732334435303643373537323039344534463535344530393445363230393634364636323641303935463041333630394431383144304232443042454431413930393546303934333631373336353344343136333633374334373635364536343635373233443436363536443243344436313733363337433445373536443632363537323344353036433735373237433530363537323733364636453344333337433530364637333733334435393635373337433530373236463645353437393730363533443530373237333743353236353636364336353738334435393635373330393530353234463445303935303734303936453644364636343039354630413337303944313831443138413039354630393546303934313434353030393532324430393633363137333635303935463041333830394431393644313830443042454430423444304238443141334430424445413939393130393546303934333631373336353344343936453733374334373635364536343635373233443444363137333633374334453735364436323635373233443530364337353732303934453446353534453039344536323039364536443646363430393546304133393039443042334432383344304242443141444431383944304235303935463039343336313733363533443445364636443743343736353645363436353732334434443631373336333743344537353644363236353732334435303643373537323743353337343732363536453637373436383344353337343732364636453637374335343635364537333635334435303732363537333743353636353732363234363646373236443344353036313732373437433536364636393633363533443431363337343039353634353532343230393536324430393631363437363633364330393546304133313039443042454430424444313841303935463039343336313733363533443445364636443743343736353645363436353732334434443631373336333743344537353644363236353732334435333639364536373039343134343441303935303634303936453733373536323641303935463041";
//		encode = "0A310942095F095F0958094974656D096E756D6D6F64095F0A32093A095F095F0950554E43540950756E63740970756E6374095F0A330944656972095F094D6F6F643D496E647C54656E73653D5072657309564552420956544909726F6F74095F0A34096D6F095F094E756D6265723D53696E677C506572736F6E3D317C506F73733D5965730944455409446574096E6D6F643A706F7373095F0A350963686169726465095F09436173653D436F6D7C466F726D3D4C656E7C47656E6465723D4D6173637C4E756D6265723D506C7572094E4F554E094E6F756E096E7375626A095F0A36096C696F6D095F094E756D6265723D53696E677C506572736F6E3D31094144500950726570096E6D6F643A70726570095F0A3709676F095F0950617274547970653D436D706C0950415254095662096D61726B3A707274095F0A380962686675696C095F09466F726D3D45636C7C4D6F6F643D496E647C54656E73653D5072657309564552420950726573496E640963636F6D70095F0A39094D656C095F09436173653D436F6D7C47656E6465723D4D6173637C4E756D6265723D53696E670950524F504E094E6F756E09636F6D706F756E64095F0A313009476962736F6E095F09436173653D436F6D7C47656E6465723D4D6173637C4E756D6265723D53696E670950524F504E094E6F756E096E7375626A095F0A";
//		String decode = new String(IndexUtils.hex2String(encode));
//		System.out.println("input "+input);
//		System.out.println("encode "+encode);		
//		System.out.println("decode "+decode);

		
		//HashSet <String> x = new HashSet <String>();
		//x.add("https://clarin-pl.eu/dspace/bitstream/handle/11321/115/wyniki.csv.zip?sequence=7");
		//System.out.println(filterZip7zBz2RarArchive(x).iterator().next().toString());
		
	}

}
