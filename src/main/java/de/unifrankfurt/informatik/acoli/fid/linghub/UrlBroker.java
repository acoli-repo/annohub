package de.unifrankfurt.informatik.acoli.fid.linghub;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;

import de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager;
import de.unifrankfurt.informatik.acoli.fid.types.MetadataSource;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceFilter;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceFormat;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.UpdatePolicy;
import de.unifrankfurt.informatik.acoli.fid.ub.PostgresManager;
import de.unifrankfurt.informatik.acoli.fid.util.IndexUtils;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;

/**
 * 
 * @author frank
 *
 */

public class UrlBroker {
	
	ResourceManager resourceManager;
	LinghubBroker linghubBroker;
	
	private HashMap <String, ResourceInfo> resourceMap = new HashMap <String, ResourceInfo>();
	private HashSet <ResourceInfo> resourcePool;
	private HashSet <String> seenFiles = new HashSet <String> ();
	private XMLConfiguration config;
	private File seedFile;
	private File outputFile;
	private String linghubQuery;
	private boolean useQuery;
	private boolean usePostgres;
	private UpdatePolicy updatePolicy;
	private String [] useSeenFilesWithStatusCodes = null;
	private ArrayList <ResourceFilter> resourceFilter = new ArrayList <ResourceFilter> ();
	private ArrayList <ResourceFilter> resourceFilterHTML = new ArrayList <ResourceFilter> ();
	private long blacklistedCount = 0;
	private boolean useLocalLinghubFile = true;
	
	

	
	public ArrayList<ResourceFilter> getResourceFilterHTML() {
		return resourceFilterHTML;
	}



	public UrlBroker (ResourceManager resourceManager, XMLConfiguration config) {
		
		this.resourceManager = resourceManager;
		
		if (config.getBoolean("Linghub.enabled")) {
			linghubBroker = new LinghubBroker (config);
		}
		//linghubBroker = new LinghubBroker (linghubDBDirectory.getAbsolutePath());
		
		this.config = config;
		seedFile = new File (config.getString("RunParameter.urlSeedFile"));
        outputFile = new File (config.getString("RunParameter.urlPoolFile"));
        updatePolicy = UpdatePolicy.valueOf(config.getString("RunParameter.updatePolicy"));
    	linghubQuery = config.getString("Linghub.linghubQueries.resourceQueries");
    	try {
    	useQuery = config.getBoolean("Linghub.useQueries");
    	} catch (Exception e) {
    		useQuery = true;
    	}
    	try {
        	usePostgres = config.getBoolean("Databases.Postgres.usePostgres");
        	} catch (Exception e) {
        	System.out.println("Configuration parameter Databases.Postgres.usePostgres not found !");
        	usePostgres = false;
        }
    	
    	/*
    	if (updatePolicy == UpdatePolicy.UPDATE_ALL || updatePolicy == UpdatePolicy.UPDATE_CHANGED)
    		useSeenFiles = true;
    	else useSeenFiles = false; // UPDATE_NEW
    	*/
    	
    	try {
    		useSeenFilesWithStatusCodes = config.getString("Linghub.statusCodeFilter").split(",");
    	} catch (Exception e) {}
    	try {
    		String urlFilter = config.getString("RunParameter.urlFilter");
    		if (!(urlFilter == null || urlFilter.isEmpty())) {
    		for (String uf : urlFilter.split(",")) {
    			try {
    			resourceFilter.add(ResourceFilter.valueOf(uf.trim()));
    			} catch (Exception e){}
    		}
    		// Do not allow links
    		resourceFilterHTML = new ArrayList <ResourceFilter> (resourceFilter);
    		resourceFilterHTML.remove(ResourceFilter.HTML);
    		resourceFilterHTML.remove(ResourceFilter.NOEXT);
    		}
    		
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    	try {
    		useLocalLinghubFile = config.getBoolean("Databases.Linghub.useLocalFile");
    	} catch (Exception e) {}
	}
	
	
	
	/**
	 * Make SPARQL query on Linghub RDF local file or http://linghub.org/sparql SPARQL endpoint
	 * @param SPARQL query string
	 * @param useLocalFile selects local Linghub RDF or http://linghub.org/sparql for retrieving info about Linghub resources 
	 * @param resourceMap
	 */
	private void sparqlLinghubDump(String linghubQuery, boolean useLocalFile, HashMap<String, ResourceInfo> resourceMap) {
		
		System.out.println("Querying Linghub resources\n");
		//System.out.println("Querystring : "+linghubQuery);
		
		ArrayList<TupleQueryResult> resultSets = null;
		
		resultSets = linghubBroker.queryLinghubResourcesLocal(linghubQuery);
		//resultSets = linghubBroker.queryLinghubResourcesOnline(linghubQuery);
		
		int all=0;
		int good=0;
		BindingSet x;
		for (TupleQueryResult rs : resultSets) {
			
			try {
				while (rs.hasNext()) {
					x = rs.next();
					all++;

					// filter garbage URLs
					if (IndexUtils.checkFileURL(x.getValue("accessUrl").toString())!= null) {
						ResourceInfo rsi = new ResourceInfo(
								x.getValue("accessUrl").toString(),
								x.getValue("dataset").toString(),
								x.getValue("distribution").toString(),
								ResourceFormat.UNKNOWN);
						System.out.println(x.getValue("accessUrl").toString());
						good++;
						
						// add additional attributes if available
						//Object dclanguage = x.getValue("dclanguage");
						//if (dclanguage != null) rsi.getLinghubAttributes().setDcLanguageString(dclanguage.toString());
						
						//Object dctlanguage = x.getValue("dctlanguage");
						//if (dctlanguage != null) rsi.getLinghubAttributes().setDctLanguageString(dctlanguage.toString());
						
						//Object type = x.getValue("dcttype");
						//if (type != null) rsi.getLinghubAttributes().setDctType(type.toString());
						
						//Object format = x.getValue("dcformat");
						//if (format != null) rsi.getLinghubAttributes().setDcFormat(format.toString());
						
						// also http://purl.org/ms-lod/MetaShare.ttl#languageName
						
						
						/*
						Utils.debug("dctlanguage : "+ x.getValue("dctlanguage"));
						Utils.debug("dclanguage : "+x.getValue("dclanguage"));
						Utils.debug("languageDescriptionInfo : " +x.getValue("languageDescriptionInfo"));
						Utils.debug("dcformat : "+ x.getValue("dcformat"));
						Utils.debug("dcttype : "+ x.getValue("dcttype"));
						*/
											
						// Finally add the resource to the resourceMap
						resourceMap.put(x.getValue("accessUrl").toString(), rsi);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("Found "+good+" access URLs in "+all+" results");
	}
	
	
	/**
	 * Add linghub attributes to ResourceInfo objects by quering Linghub's RDF dump
	 * (http://linghub.org/sparql SPARQL endpoint)
	 * @param SPARQL query string
	 * @param useLocalFile selects local Linghub RDF or http://linghub.org/sparql for retrieving info about Linghub resources 
	 * @param resourceMap
	 */
	public void sparqlLinghubAttributes(boolean alwaysLocal, Collection<ResourceInfo> resourceList) {
		
		Utils.debug("sparqlLinghubAttributes");
		
		ArrayList<TupleQueryResult> resultSets = null;
		
		System.out.println("updating linghub metadata ...");
		
		String linghubMetadataQuery = "";
    	linghubMetadataQuery = config.getString("Linghub.linghubQueries.metadataQueries");
		String [] queries = linghubMetadataQuery.split("### Querystart ###");
		String query;
		boolean success = false;
		
		for (String q : queries) {

			if (q.trim().isEmpty()) continue;
		
			for (ResourceInfo resourceInfo : resourceList) {
				//Utils.debug("resourceInfo"+resourceInfo==null);
				//Utils.debug("resourceInfo.getDataURL()"+resourceInfo.getDataURL()==null);
				//Utils.debug("resourceInfo.getMetaDataURL()"+resourceInfo.getMetaDataURL()==null);
				
				// filter resources
				if  (  resourceInfo.getDataURL() == null
					|| resourceInfo.getDataURL().startsWith("file")
					|| resourceInfo.getMetaDataURL().isEmpty()
					|| resourceInfo.getMetaDataURL2().isEmpty()
						// skip resources that have metadata already
					|| resourceInfo.getResourceMetadata().getMetadataSource() != MetadataSource.NONE
					)  continue;
				
				// fill access url into query
				query = q.replace("!ACCESSURL!", "<"+resourceInfo.getDataURL()+">");
				
				resultSets = linghubBroker.queryLinghubResourcesLocal(query);
				//resultSets = linghubBroker.queryLinghubResourcesOnline(linghubQuery);
	
				success = false;
				BindingSet x;
				for (TupleQueryResult rs : resultSets) {
					
					try {
						while (rs.hasNext()) {
	
							x = rs.next();
							
							// retrieve attributes from query
							Value dataset = (Value) x.getValue("dataset");
							if (dataset != null) {
								resourceInfo.setMetaDataURL(dataset.stringValue());
							}
							Value title = (Value) x.getValue("title");
							if (title != null) {
								resourceInfo.getResourceMetadata().setTitle(title.stringValue());
							}
							Value description = (Value) x.getValue("description");
							if (description != null) {
								success = true;
								resourceInfo.getResourceMetadata().setDescription(description.stringValue());
							}
							Value language = (Value) x.getValue("language");
							if (language != null) {
								success = true;
								resourceInfo.getResourceMetadata().setDcLanguageString(language.stringValue());
							}
							Value rights = (Value) x.getValue("rights");
							if (rights != null) {
								success = true;
								resourceInfo.getResourceMetadata().setRights(rights.stringValue());
							}
							Value date = (Value) x.getValue("date");
							if (date != null) {
								success = true;
								resourceInfo.getResourceMetadata().setDate(new Date(date.stringValue()));
							}
							Value creator = (Value) x.getValue("creator");
							if (creator != null) {
								success = true;
								resourceInfo.getResourceMetadata().setCreator(creator.stringValue());
							}
							
							Value contributor = (Value) x.getValue("contributor");
							if (contributor != null) {
								success = true;
								resourceInfo.getResourceMetadata().setContributor(contributor.stringValue());
							}
							
							Value subject = (Value) x.getValue("subject");
							if (subject != null) {
								success = true;
								resourceInfo.getResourceMetadata().addKeyword(subject.stringValue());
							}
							
							Value homepage = (Value) x.getValue("homepage");
							if (homepage != null) {
								success = true;
								resourceInfo.getResourceMetadata().setWebpage(homepage.stringValue());
							}
						
							
							if (success) {
								resourceInfo.getResourceMetadata().setMetadataSource(MetadataSource.LINGHUB);
							}
	
							// old
							// add additional attributes if available
							//Object dclanguage = x.getValue("dclanguage");
							//if (dclanguage != null) resourceInfo.getLinghubAttributes().setDcLanguageString(dclanguage.toString());
							
							//Object dctlanguage = x.getValue("dctlanguage");
							//if (dctlanguage != null) resourceInfo.getLinghubAttributes().setDctLanguageString(dctlanguage.toString());
							
							//Object type = x.getValue("dcttype");
							//if (type != null) rsi.getLinghubAttributes().setDctType(type.toString());
							
							//Object format = x.getValue("dcformat");
							//if (format != null) rsi.getLinghubAttributes().setDcFormat(format.toString());
		
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		System.out.println("done !");
	}
	
	
	/**
	 * Code backup
	 * Make SPARQL query on Linghub RDF local file or http://linghub.org/sparql SPARQL endpoint
	 * @param SPARQL query string
	 * @param useLocalFile selects local Linghub RDF or http://linghub.org/sparql for retrieving info about Linghub resources 
	 * @param resourceMap
	 * @deprecated
	 */
	private void sparqlLinghubDumpTDB(String linghubQuery, boolean useLocalFile, HashMap<String, ResourceInfo> resourceMap) {
		System.out.println("Querystring : "+linghubQuery);
		
		ArrayList<ResultSet> resultSets = null;
		
		if (useLocalFile) {
			resultSets = linghubBroker.queryLinghubResourcesLocalTDB(linghubQuery);
		} else {
			resultSets = linghubBroker.queryLinghubResourcesOnline(linghubQuery);
		}
	    
		for (ResultSet rs : resultSets) {
			
			while (rs.hasNext()) {
	    		QuerySolution x = rs.next();
	    		resourceMap.put(x.get("accessUrl").toString(),
	    				new ResourceInfo(
	    					x.get("accessUrl").toString(),
	    					x.get("distribution").toString(),
	    					x.get("dataset").toString(),
							ResourceFormat.UNKNOWN));	  
	    	}
		}
	}
	

	
	/**
	 * Read the urlseed file
	 * @param urlseed
	 * @param resourceMap
	 */
	public static Set<String> readUrlseedFile(File urlseedFile, HashMap <String, ResourceInfo> resourceMap) {
		
		// Omit seed file if missing !
		if (urlseedFile == null || !urlseedFile.exists()) return null;
		
		boolean samplingActive;
		
		try {
	
			Charset charset = Charset.forName("UTF-8");
			CSVParser parser = CSVParser.parse(urlseedFile, charset , CSVFormat.TDF);
			//CSVParser parser = CSVParser.parse(urlseedFile, charset , CSVFormat.RFC4180);
			String url;
			String metaURL;
			for (CSVRecord csvRecord : parser) {
				
				// skip empty lines
				if (csvRecord.get(0).trim().isEmpty()) continue;
				
				url="";
				metaURL="";
				
				switch (csvRecord.size()) {
				case 1:
					url = csvRecord.get(0).trim();
	    			metaURL = "http://fid/metadata/tbc";
	    			break;
	    			//System.out.println("Skipping incomplete line in urlseed :");
	    			//System.out.println(csvRecord.get(0));
					//continue;
				case 2:
					url = csvRecord.get(0).trim();
					metaURL = csvRecord.get(1).trim();
					break;
				/*case 3:
					url = csvRecord.get(0).trim();
					metaURL = csvRecord.get(1).trim();
					modelTypeString = csvRecord.get(2).trim();
					break;
				*/
					
				default:
					break;
				}
				
				if (url.isEmpty() || metaURL.isEmpty()) continue;
				
				// parse annotation format
				/*ModelType modelType = null;
				if (!modelTypeString.isEmpty()) {
				try {
					modelType = ModelType.valueOf(modelTypeString.toUpperCase());
					} catch (Exception e) {
    				e.printStackTrace();
    				continue;
    			}
				}*/
				
				if (!url.startsWith("@")) {
					samplingActive = true;
				} else {
					url = url.substring(1);
					samplingActive = false;
				}
				
				String parsedUrl = IndexUtils.checkFileURL(url);
				if (parsedUrl == null) {
					System.out.print("Skipping faulty URL in urlseed file : "+url+"\n");
					continue;
				}
				
				ResourceInfo resourceInfo = new ResourceInfo(url, metaURL);
				resourceInfo.setSamplingActive(samplingActive);
				// Create resource object
    			resourceMap.put(parsedUrl, resourceInfo); // ResourceFormat automatically computed in constructor !
				
			    //System.out.println(csvRecord.size());
			    //System.out.println(csvRecord.get(0).isEmpty());
			    //System.out.println(csvRecord.toString());

			 }
		} catch (IOException e) {
			e.printStackTrace();
		}
		return resourceMap.keySet();
	}
	
	
	
	/**
	 * Make the ResourcePool. Also write URLs to urlPool file.
	 * @param seedFile Parsed from vifaConfig.RunSh.UrlSeedFile
	 * @param linghubQueryFile Parsed from vifaConfig.Ldspider.seedQuery
	 * @param outputFile Parsed from vifaConfig.RunSh.UrlInputFile
	 * @param useSeenFiles Also retry already processed files in ldh (true) or do not retry such files
	 * @param errorCodeFilters Array with error codes (e.g. 888, 999)
	 * @return Set of found resources
	 * 
	 */
	public HashSet<ResourceInfo> makeUrlPool () {
		
		System.out.println("makeUrlPool");
	
		// 1. Put results from linghub sparql queries (Linghub.linghubQueries) and results from UB database in resourceMap
		if (config.getBoolean("Linghub.enabled") && useQuery) {
			sparqlLinghubDump(linghubQuery, useLocalLinghubFile, resourceMap);
			
			// Get result from UB database
			if (usePostgres) {
				PostgresManager mng = new PostgresManager (config);
				for (ResourceInfo rs : mng.getExternalCrawlerResultsAsResourceInfos()) {
					if (!resourceMap.containsKey(rs.getDataURL())) { // omit doubles
						resourceMap.put(rs.getDataURL(), rs);
					}
				}
			}
		}

		// 2. Put URLs from seedfile in urlPool
		readUrlseedFile(seedFile, resourceMap);
		
		// 3. Add or remove URLs of already seen files (in ldh) to/from resourceMap
		addRemoveSeenFiles(useSeenFilesWithStatusCodes, resourceMap);
		
		// 4. Remove blacklisted files
		applyBlacklistFilter(resourceMap);
		
		// 5. Filter resources with ResourceFilter
		resourcePool = applyResourceFilter(resourceFilter, resourceMap, config.getBoolean("Linghub.enabled"));
		
		// 6. Add linhub metadata (language, format, etc.)
		// TODO Get linghub attributes with LinghubBroker.testQueryLinghubAttribute
		// Set LinghubResource for each resource in the resourcePool by dataUrl
		
		// 7. Write resourcePool to file
		if (outputFile != null) {
			System.out.println("Writing urlpool to "+outputFile.getAbsolutePath());
			writeResourcePool(resourcePool, outputFile);
		}
		
		// close dataset
		try {
			//linghubBroker.shutdownRepository();
			//linghubBroker.linghubDump.getDataset().close(); // TDB
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return resourcePool;
	}
	


	/**
	 * Remove blacklisted resources
	 * @param resourceMp
	 */
	private void applyBlacklistFilter(HashMap<String, ResourceInfo> resourceMp) {
		HashSet <String> filtered = new HashSet <String> ();
		
		
		for (ResourceInfo rsi : resourceMp.values()) {
			
			String url = rsi.getDataURL();
			String metaUrl = rsi.getMetaDataURL();
			
			// Check data URL
			for (String x : IndexUtils.blacklist) {
				if (url.contains(x)) {
					filtered.add(url);
					continue;
				}
			
			// Check metadata URL	
			for (String y : IndexUtils.metadataBlacklist) {
				try {
				if (metaUrl.contains(y)) {
					filtered.add(url);
				}
				} catch (Exception e) {}
			}
			}
		}
		
		// Remove filtered resources
		for (String url : filtered)
			resourceMp.remove(url);
		
		blacklistedCount = filtered.size();
	}



	/**
	 * Classify a resource by its ResourceFormat and determine with the config option RunParameter.urlFilter those which
	 * will be processed later
	 * @param resourceMp
	 * @param outputStat Print distribution of found source types
	 * @param resourceFilter
	 * @return resources that match one of the given ResourceFilters
	 */
	public HashSet <ResourceInfo> applyResourceFilter(
			ArrayList <ResourceFilter> urlFilter,
			HashMap <String, ResourceInfo> resourceMp, boolean outputStat) {
		
		HashSet <ResourceInfo> resourcePool = new HashSet <ResourceInfo> ();
		long count = 0;
		long foundResources = 0;
		
		// remove linghub dump
		resourceMp.remove(IndexUtils.checkFileURL(config.getString("Linghub.linghubDataDumpURL")));
		
		for (ResourceFormat rf : ResourceFormat.values()) {
			if (outputStat) System.out.print(rf.toString()+" : ");
		
		boolean taken = false;
		switch (rf) {
			
		    // In order to avoid duplicates each filter will remove its found resources !
		
			case RDF :
				if (urlFilter.contains(ResourceFilter.RDF) || urlFilter.isEmpty()) {
				// take rdf file types
				resourcePool.addAll(IndexUtils.filterRDF(resourceMp, ResourceFormat.RDF));
				// take gziped rdf file types
				resourcePool.addAll(IndexUtils.filter2(resourceMp, IndexUtils.gzipFileType, IndexUtils.rdfFileType, ResourceFormat.RDF));
				taken = true;
				} else {
					count = IndexUtils.filterRDF(resourceMp, ResourceFormat.RDF).size()+
					IndexUtils.filter2(resourceMp, IndexUtils.gzipFileType, IndexUtils.rdfFileType, ResourceFormat.RDF).size();
					if (outputStat) System.out.print(count);
				}
				break;
				
			case CONLL :
				if (urlFilter.contains(ResourceFilter.CONLL) || urlFilter.isEmpty()) {
				// Does only filter resource which have the predefined CONLL resource type (in urlseed)
				resourcePool.addAll(IndexUtils.filterResourcesWithFormat(resourceMp, ResourceFormat.CONLL));
				resourcePool.addAll(IndexUtils.filterCONLL(resourceMp, ResourceFormat.CONLL));
				taken = true;
				} else {
					count = IndexUtils.filterResourcesWithFormat(resourceMp, ResourceFormat.CONLL).size()+
					IndexUtils.filterCONLL(resourceMp, ResourceFormat.CONLL).size();
					if (outputStat) System.out.print(count);
				}
				break;
			
			case ARCHIVE :
				if (urlFilter.contains(ResourceFilter.ARCHIVE) || urlFilter.isEmpty()) {
				resourcePool.addAll(IndexUtils.filterZip7zBz2RarArchive(resourceMp, ResourceFormat.ARCHIVE));
				resourcePool.addAll(IndexUtils.filterTarNgz(resourceMp, ResourceFormat.ARCHIVE));
				resourcePool.addAll(IndexUtils.filterGzip(resourceMp, ResourceFormat.ARCHIVE));
				taken = true;
				} else {
					count = IndexUtils.filterZip7zBz2RarArchive(resourceMp, ResourceFormat.ARCHIVE).size()+
					IndexUtils.filterTarNgz(resourceMp, ResourceFormat.ARCHIVE).size()+
					IndexUtils.filterGzip(resourceMp, ResourceFormat.ARCHIVE).size();
					if (outputStat) System.out.print(count);
				}
				break;
				
			case SPARQL :
				if (urlFilter.contains(ResourceFilter.SPARQL) || urlFilter.isEmpty()) {
				resourcePool.addAll(IndexUtils.filterSparql(resourceMp, ResourceFormat.SPARQL));
				taken = true;
				} else {
					count = IndexUtils.filterSparql(resourceMp, ResourceFormat.SPARQL).size();
					if (outputStat) System.out.print(count);
				}
				break;
				
			case METASHARE :
				if (urlFilter.contains(ResourceFilter.METASHARE) || urlFilter.isEmpty()) {
				resourcePool.addAll(IndexUtils.filterMetaShare(resourceMp));
				taken = true;
				} else {
					count = IndexUtils.filterMetaShare(resourceMp).size();
					if (outputStat) System.out.print(count);
				}
				break;
				
			case HTML :
				if (urlFilter.contains(ResourceFilter.HTML) || urlFilter.isEmpty()) {
					resourcePool.addAll(IndexUtils.filterHTML(resourceMp, ResourceFormat.HTML));
					resourcePool.addAll(IndexUtils.filterResourcesWithFormat(resourceMp,ResourceFormat.HTML));
					taken = true;
				} else {
					count = IndexUtils.filterHTML(resourceMp, ResourceFormat.HTML).size()+
					IndexUtils.filterResourcesWithFormat(resourceMp,ResourceFormat.HTML).size();
					if (outputStat) System.out.print(count);
				}
				break;
				
			case URN :
				if (urlFilter.isEmpty()) {
					resourcePool.addAll(IndexUtils.filterResourcesWithScheme(resourceMp,new String [] {"urn"}));
					taken = true;
				} else {
				count = IndexUtils.filterResourcesWithScheme(resourceMp,new String [] {"urn"}).size();
				if (outputStat) System.out.print(count);
				}
				break;
				
			case FTP :
				if (urlFilter.isEmpty()) {
					resourcePool.addAll(IndexUtils.filterResourcesWithScheme(resourceMp,new String [] {"ftp"}));
					taken = true;
				} else {
				count = IndexUtils.filterResourcesWithScheme(resourceMp,new String [] {"ftp"}).size();
				if (outputStat) System.out.print(count);
				}
				break;
				
			case MAILTO :
				count = IndexUtils.filterResourcesWithScheme(resourceMp,new String [] {"mailto"}).size();
				if (outputStat) System.out.print(count);
				break;
				
			case NEWS :
				count = IndexUtils.filterResourcesWithScheme(resourceMp,new String [] {"news"}).size();
				if (outputStat) System.out.print(count);
				break;
				
			case JSON :
				if (urlFilter.contains(ResourceFilter.JSON) || urlFilter.isEmpty()) {
					resourcePool.addAll(IndexUtils.filterJSON(resourceMp, ResourceFormat.JSON));
					taken = true;
				} else {
				count = IndexUtils.filterJSON(resourceMp, ResourceFormat.JSON).size();
				if (outputStat) System.out.print(count);
				}
				break;
				
			case PDF :
				if (urlFilter.contains(ResourceFilter.PDF) || urlFilter.isEmpty()) {
					resourcePool.addAll(IndexUtils.filterPDF(resourceMp, ResourceFormat.PDF));
					taken = true;
				} else {
					count = IndexUtils.filterPDF(resourceMp, ResourceFormat.PDF).size();
					if (outputStat) System.out.print(count);
				}
				break;
				
			case POSTSCRIPT :
				if (urlFilter.isEmpty()) {
					resourcePool.addAll(IndexUtils.filterPostscript(resourceMp, ResourceFormat.POSTSCRIPT));
					taken = true;
				} else {
				count = IndexUtils.filterPostscript(resourceMp, ResourceFormat.POSTSCRIPT).size();
				if (outputStat) System.out.print(count);
				}
				break;
				
			case XML :
				if (urlFilter.contains(ResourceFilter.XML) || urlFilter.isEmpty()) {
					resourcePool.addAll(IndexUtils.filterXML(resourceMp, ResourceFormat.XML));
					taken = true;
				} else {
				count = IndexUtils.filterXML(resourceMp, ResourceFormat.XML).size();
				if (outputStat) System.out.print(count);
				}
				break;
				
			case EXCEL :
				if (urlFilter.isEmpty()) {
					resourcePool.addAll(IndexUtils.filterExel(resourceMp, ResourceFormat.EXCEL));
					taken = true;
				} else {
				count = IndexUtils.filterExel(resourceMp, ResourceFormat.EXCEL).size();
				if (outputStat) System.out.print(count);
				}
				break;
			
			case TEXT :
				if (urlFilter.isEmpty()) {
					resourcePool.addAll(IndexUtils.filterText(resourceMp, ResourceFormat.TEXT));
					taken = true;
				} else {
				count = IndexUtils.filterText(resourceMp, ResourceFormat.TEXT).size();
				if (outputStat) System.out.print(count);
				}
				break;

			case CSV :
				if (urlFilter.isEmpty()) {
					resourcePool.addAll(IndexUtils.filterCSV(resourceMp, ResourceFormat.CSV));
					taken = true;
				} else {
				count = IndexUtils.filterCSV(resourceMp, ResourceFormat.CSV).size();
				if (outputStat) System.out.print(count);
				}
				break;
				
			case TSV :
				if (urlFilter.isEmpty()) {
					resourcePool.addAll(IndexUtils.filterTSV(resourceMp, ResourceFormat.TSV));
					taken = true;
				} else {
				count = IndexUtils.filterTSV(resourceMp, ResourceFormat.TSV).size();
				if (outputStat) System.out.print(count);
				}
				break;
				
			case GRAPHICS :
				count = IndexUtils.filterGraphics(resourceMp, ResourceFormat.GRAPHICS).size();
				if (outputStat) System.out.print(count);
				break;
				
			case SOUND :
				count = IndexUtils.filterSound(resourceMp, ResourceFormat.SOUND).size();
				if (outputStat) System.out.print(count);
				break;
				
			case NOEXT :
				if (urlFilter.contains(ResourceFilter.NOEXT) || urlFilter.isEmpty()) {
					resourcePool.addAll(IndexUtils.filterResourcesWithoutExtension(resourceMp, ResourceFormat.HTML));
					taken = true;
				} else {
					count = IndexUtils.filterResourcesWithoutExtension(resourceMp, ResourceFormat.HTML).size();
					if (outputStat) System.out.print(count);	
				}
				break;

			case UNKNOWN :
				if (urlFilter.contains(ResourceFilter.UNKNOWN) || urlFilter.isEmpty()) {
				resourcePool.addAll(IndexUtils.filterResourcesWithFormat(resourceMp,ResourceFormat.UNKNOWN));
				taken = true;
				} else {
					count = IndexUtils.filterResourcesWithFormat(resourceMp,ResourceFormat.UNKNOWN).size();
					if (outputStat) System.out.print(count);
				}
				break;
				
			default :
				break;
			}
		
		if (taken) {
			if (outputStat) System.out.println(resourcePool.size() - foundResources);
			}
		else {
			if (outputStat) System.out.println(" (not included)");
			}
		foundResources = resourcePool.size();

		}
		if (outputStat) System.out.println("BLACKLISTED : "+blacklistedCount+" (not included)");
		System.out.println("Resources : "+resourcePool.size());
		return resourcePool;
	}

	
	
	/**
	 * Write the resourcePool to the specified output file
	 * @param resourcePool
	 * @param outputFile
	 */
	private void writeResourcePool(HashSet<ResourceInfo> resourcePool,
			File outputFile) {
				
			if (resourcePool == null) return;
			
			try (BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath())) {
		
				for (ResourceInfo ri : resourcePool) {
					String url = ri.getDataURL();
					String metaUrl = ri.getMetaDataURL();
					//Utils.debug(ri.getFormat().toString());
					if (metaUrl == null || metaUrl.isEmpty()) {
							writer.write(url+"\n", 0, url.length()+1);
							
						} else {
							writer.write(url+"\t"+ metaUrl +"\n", 0, url.length()+metaUrl.length()+2);
							//writer.write(url+","+ metaUrl +"\n", 0, url.length()+metaUrl.length()+2);

						}
					}
				} catch (IOException e) {
					System.err.format("IOException: %s%n", e);
				}
		}
	
	
	

	/**
	 * Add or remove URLs of already processed files depending on useSeenFiles parameter
	 * @param useSeenFiles
	 * @param useSeenFilesWithStatusCodes Additionally filter files with StatusCode
	 * @param resourceMap
	 * @TODO fix useSeenFilesWithStatusCodes !
	 */
	private void addRemoveSeenFiles(String[] useSeenFilesWithStatusCodes, HashMap <String, ResourceInfo> resourceMap) {
		
		
		if (updatePolicy == UpdatePolicy.UPDATE_NEW) {
			System.out.println("Removing already processed files from file list !");
			for (String url : resourceManager.getDoneResourceUrls()) {
				resourceMap.remove(url);
			}
		}
		
		// more options to filter by resource status code (old)
		/*
		if (useSeenFilesWithStatusCodes == null || useSeenFilesWithStatusCodes.length == 0) {
				for (String url : resourceManager.getDoneResourceUrls()) {
				seenFiles.add(url);
			}
		} else {
				for (String fileStatusCode : useSeenFilesWithStatusCodes) {
					for (String url : resourceManager.getDoneResourcesWithFileWithStatus(fileStatusCode, false)) {
						seenFiles.add(url);
					}
				}
		}
		
		
		if (useSeenFiles) {
			// Add seen files to urlPool
			for (String url : seenFiles) {
				resourceMap.put(url,new ResourceInfo(url, resourceManager.getResourceMetaDataURL(url), resourceManager.getResourceFormat(url)));
			}

		} else {
			// Remove seen files from urlPool
			for (String url : seenFiles) {
				// Keep CONLL and SPARQL_ENDPOINT resources !
				if (resourceMap.containsKey(url) && resourceMap.get(url).getResourceFormat() != ResourceFormat.SPARQL) {
					resourceMap.remove(url);
				//if (resourceMap.containsKey(url) && resourceMap.get(url).getFormat() == ResourceFormat.RDF) {
				//	resourceMap.remove(url);
				}
			}
		}
		*/
	}

	
	
	public static void main (String [] args) {
		
		HashMap <String, ResourceInfo> resourceMap = new HashMap <String, ResourceInfo>();
		readUrlseedFile(new File(""), resourceMap);
	}
	
	
}