package de.unifrankfurt.informatik.acoli.fid.spider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Queue;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.unifrankfurt.informatik.acoli.fid.activemq.Consumer;
import de.unifrankfurt.informatik.acoli.fid.detector.ContentTypeDetector;
import de.unifrankfurt.informatik.acoli.fid.linghub.UrlBroker;
import de.unifrankfurt.informatik.acoli.fid.parser.ParserHtml;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager;
import de.unifrankfurt.informatik.acoli.fid.types.ProcessingFormat;
import de.unifrankfurt.informatik.acoli.fid.types.ParseResult;
import de.unifrankfurt.informatik.acoli.fid.types.ParseStats;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceFormat;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.Worker;
import de.unifrankfurt.informatik.acoli.fid.util.IndexUtils;
import de.unifrankfurt.informatik.acoli.fid.util.LocateUtils;
import de.unifrankfurt.informatik.acoli.fid.util.ScriptUtils;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;


public class VifaWorker implements Worker {
	
	// Worker ID
	private int workerID;
	
	// Queue with resources
	private Queue<ResourceInfo> resourceQueue;
	
	// Class for downloading files
	private DownloadManager downloadManager;
	
	private UrlBroker urlBroker;

	// Configuration options
	private XMLConfiguration config;
	
	// Variables
	private ResourceInfo resourceInfo;
	private File downloadFolder = null;
	private String decompressionUtility;
	
	// Handler
	private RdfFileHandlerI rdfFileHandler;
	private ConllFileHandler conllFileHandler;
	private GenericXMLFileHandler genericXmlFileHandler;
	
	ParseStats stats = new ParseStats();
	
	boolean foundRDF = false;
	boolean foundCONLL = false;
	boolean foundXML = false;

	boolean forceRescan = false;

	private File validateRdfScript;
	private LocateUtils locateUtils = new LocateUtils();

	
	// Constructor
	/**
	 * 
	 * @param id
	 * @param urlQueue
	 * @param rfh
	 * @param cfh
	 * @param config
	 * @param dlm
	 * @param urlBroker
	 * @param gxfh
	 */
	public VifaWorker (int id, Queue<ResourceInfo> urlQueue, RdfFileHandlerI rfh, ConllFileHandler cfh, XMLConfiguration config, DownloadManager dlm, UrlBroker urlBroker, GenericXMLFileHandler gxfh) {
		
		this.workerID = id;
		this.resourceQueue = urlQueue;
		this.rdfFileHandler = rfh;
		this.conllFileHandler = cfh;
		this.genericXmlFileHandler = gxfh;
		this.downloadManager = dlm;
		this.urlBroker = urlBroker;
		
		this.config = config;
		this.downloadFolder = new File (config.getString("RunParameter.downloadFolder"),Integer.toString(workerID));
		this.decompressionUtility = config.getString("RunParameter.decompressionUtility");
		this.validateRdfScript = locateUtils.getLocalFile(config.getString("RunParameter.convert2RdfXmlScript"));
	}
	
	
	public void run () {

		
		/******************************/
		/* PROCESS RESOURCES IN QUEUE */
        /******************************/
		
		while (!resourceQueue.isEmpty()) {
			

		// Pull next resource from queue
		resourceInfo = resourceQueue.poll();
		
		if (resourceInfo != null) {
			resourceInfo.getFileInfo().setProcessingStartDate(new Date());
			forceRescan = resourceInfo.getFileInfo().getForceRescan();
		} else {
			System.out.println("VifaWorker Error : got null ResourceInfo object from queue ?");
			continue;
		}
		
		// Update statistics
		Statistics.incrURLCounter();
		
		// Output resource info
		System.out.println(workerID+": Start processing "+resourceInfo.getDataURL());
		if (resourceInfo.getMetaDataURL() != null) {
			System.out.println("@"+resourceInfo.getMetaDataURL());
		} else {
			// TODO what ?
			resourceInfo.getFileInfo().setProcessingStartDate(new Date());
		}
		

		String url = resourceInfo.getDataURL();
		
		
	try {
		
		
		/*********************/
		/* DOWNLOAD RESOURCE */
        /*********************/
		
		if (resourceInfo.getResourceFormat() == ResourceFormat.HTML) { // html not handled
			
			// TODO HTML pages will  not be registered in resource database
			// HTML pages are permanently stored locally
			// Implementation not complete 
			/*targetFolder = new File (htmlFolder, IndexUtils.makeHashFolderName(url));
			if (targetFolder.exists()) {
				 File [] files = targetFolder.listFiles();
				 if (files.length == 0) continue;
				 	resourceInfo.getFileInfo().setResourceFile(
				 			new File(targetFolder, files[0].getAbsolutePath()),LocateUtils.getRelFilePath(
				 			new File(targetFolder, files[0].getAbsolutePath()), targetFolder));
			} else {
				 // Download data file and update resource database
			     downloadManager.getResource(workerID, resourceInfo, true);
			}*/
			
		} else {
			// Download data file and update resource database
			downloadManager.getResource(workerID, resourceInfo, true, false);
		}
		
		/* If the file object is empty then one of the following conditions is true
		 a) the download failed or
		 b) the resource is up-to-date or
		 c) the resource type (e.g. ftp) is not supported 
		 */
		
		Vertex resource = resourceInfo.getResource();
		if (resource == null) {
			System.out.println("Resource empty !");
			continue;
		}
		File file = resourceInfo.getFileInfo().getResourceFile();
		
		/*************************************************************************/
		/* Skip resource because the file was not downloaded because of its size */
        /*************************************************************************/
		if (file == null) continue;
			
				
		/***************************************************************************/
		/* Skip resource because unpacked file size too big or has too many files  */
        /***************************************************************************/
		
		if (ScriptUtils.getUncompressedArchiveSize7zB(file) > config.getLong("RunParameter.uncompressedFileSizeLimit")) {
			System.out.println("Skipping large resource "+file.getAbsolutePath());
			// Delete file in registry
			downloadManager.getResourceManager().deleteResource(resourceInfo.getDataURL());
			continue;
		}
		

		
		/*******************************/
		/* HANDLE DIFFERENT FILE TYPES */
        /*******************************/
		

		/*****************/
		/*    ARCHIVES   */
        /*****************/

		if (IndexUtils.fileIsCompressed(file.getAbsolutePath(), decompressionUtility)) {
			if (!IndexUtils.unpackFile(file, downloadFolder, decompressionUtility, config)) {
				System.out.println(IndexUtils.ERROR_DECOMPRESSION);
				// Delete file in registry
				downloadManager.getResourceManager().deleteResource(resourceInfo.getDataURL());
				continue;
			}

			System.out.println("Unpacked archive file : "+file.getAbsolutePath());
			
		// List unpacked files
		HashSet <String> filesInDir = IndexUtils.listRecFilesInDir(downloadFolder);
		HashMap <String, ArrayList<String>> filesInDirMap = IndexUtils.convertFileList2FolderMap(filesInDir);
		if (config.getBoolean("RunParameter.debugOutput")) {
			System.out.println("Files in archive :");
			for (String fname : filesInDir) {
				System.out.println(fname);
			}
		}
		
		
		// Rescanning a resource limits the processing to files that have previously have been processed
		// (have been sampled in the first run)
		if (forceRescan) {
			System.out.println("RESCAN");
			
			filesInDir.clear();
			
			for (Vertex v : downloadManager.getResourceManager().getResourceFilesWithHits(resourceInfo.getDataURL())) {
				
				String rpath = v.value(ResourceManager.FilePathRel);
				System.out.println(downloadFolder+"/"+rpath);
				if (new File(downloadFolder+"/"+rpath).exists()) {
					filesInDir.add(downloadFolder+"/"+rpath);
				}
			}
			
			// turn sampling off
			resourceInfo.setSamplingActive(false);
		}
		
		
		System.out.println("ResourceFormat : "+resourceInfo.getResourceFormat());
		
		// Handle files in archive
		switch (resourceInfo.getResourceFormat()) {
	
		case RDF :     // e.g. rdf.gz
		case CONLL :   // e.g. conll.tar.gz
		case ARCHIVE : // any other archive type
			
			
			foundRDF = false;
			foundCONLL = false;
			foundXML = false;
		   
		    
			System.out.println("Scanning archive for RDF, CONLL and XML files ... ");
			HashSet<String> rdfFiles = IndexUtils.filterRdf(filesInDir);
			HashSet<String> xmlFiles = IndexUtils.filterXml(filesInDir);
			HashSet<String> conllFilesByExtension = IndexUtils.filterConll(filesInDir);

			for (String path : filesInDir) {
				if (!rdfFiles.contains(path) &&
					!xmlFiles.contains(path) &&
					!conllFilesByExtension.contains(path)) {
					ResourceFormat rf = ContentTypeDetector.guessContentType(new File(path));
					if (rf == null) continue;
					switch (rf) {
					case RDF : rdfFiles.add(path);
						break;
					case XML : xmlFiles.add(path);
						break;
					default :
						break;
					}
				}
			}
			System.out.println("RDF files   : "+rdfFiles.size());
			System.out.println("CONLL files : "+conllFilesByExtension.size());
			System.out.println("XML files   : "+xmlFiles.size());
				
			// RDF
			if (!rdfFiles.isEmpty()) {
			
			System.out.println("Processing RDF files");

			// Explanation of sampling parameters : ParseStats(100,20,0,200,)
			// Take at most 1000 samples !
			// Leave a folder if 200 good files were found
			// Leave a folder if 10 bad files were found.
			// Apply sampling to any resource > 100 files
			if (resourceInfo.getCustomParseStats() == null) {
			stats = new ParseStats(
					config.getInt("Sampling.Rdf.thresholdForGood"),
					config.getInt("Sampling.Rdf.thresholdForBad"),
					config.getInt("Sampling.Rdf.activationThreshold"),
					config.getInt("Sampling.Rdf.maxSamples"),
					rdfFiles.size());
			} else {
				stats = resourceInfo.getCustomParseStats().get(ResourceFormat.RDF);
				stats.setVolumeSize(rdfFiles.size());
			}
			
			if (!resourceInfo.isSamplingActive()) {
				stats.setActivationThreshold(ParseStats.NO_SAMPLING);
			}
			
			
			for (String folder : filesInDirMap.keySet()) {
				
				System.out.println("Folder : "+folder);
				System.out.println("has "+filesInDirMap.get(folder).size()+ " files");
				
				for (String path : filesInDirMap.get(folder)) {

					if(rdfFiles.contains(path)) {
						
						if (stats.samplingIsDecided(folder)) {break;}
	
						// Create new resourceInfo object for file
						ResourceInfo resourceInfo_ = makeFileResource(resourceInfo, path, ProcessingFormat.RDF, false);
						
						// Validate RDF file and convert it eventually to rdfxml format 
						if (ScriptUtils.validateRdfFile(resourceInfo_, config, validateRdfScript) == ScriptUtils.VALIDATION_ERROR) {
							resourceInfo.getFileInfo().setErrorMsg(IndexUtils.ERROR_IN_RDF_VALIDATION);
							resourceInfo.getFileInfo().setProcessingEndDate(new Date());
							break; // skip file
						};
						
				        System.out.println(workerID +": Indexing " + path);
				        /* no file extension -> guess extension
				         * never true !
					    if (org.apache.jena.util.FileUtils.getFilenameExt(new File(path).getAbsolutePath()).isEmpty()) {
					    	path = ScriptUtils.checkFileType(new File(path), downloadFolder, vifaHome).getAbsolutePath();
					    	continue;
					    }
					    */
				        foundRDF = true;
						rdfFileHandler.parse(resourceInfo_, this);
						
						// count negative result in folder
						if (!(resourceInfo_.getFileInfo().getParseResult() == ParseResult.SUCCESS)) {
							stats.incrBad(folder);
						} else {
							stats.incrGood(folder);
						}
					}		
				}
				if (!stats.hasSamplesLeft()) {break;}
			}
			}
			
			// CONLL
			if (!conllFilesByExtension.isEmpty()) {
				
				System.out.println("Processing CONLL files");
				
				// Explanation of sampling parameters : ParseStats(100,20,0,200,)
				// Take at most 200 samples
				// Leave a folder if 100 good files were found
				// Leave a folder if 20 bad files were found.
				// Apply sampling to any resource > 0 files
				if (resourceInfo.getCustomParseStats() == null) {
				stats = new ParseStats(
						config.getInt("Sampling.Conll.thresholdForGood"),
						config.getInt("Sampling.Conll.thresholdForBad"),
						config.getInt("Sampling.Conll.activationThreshold"),
						config.getInt("Sampling.Conll.maxSamples"),
						conllFilesByExtension.size());
				} else {
					stats = resourceInfo.getCustomParseStats().get(ResourceFormat.CONLL);
					stats.setVolumeSize(conllFilesByExtension.size());
				}
				
				if (!resourceInfo.isSamplingActive()) {
					stats.setActivationThreshold(ParseStats.NO_SAMPLING);
				}
				
			
			for (String folder : filesInDirMap.keySet()) {
				
				for (String path : filesInDirMap.get(folder)) {

					if(conllFilesByExtension.contains(path)) {
						
						if (stats.samplingIsDecided(folder)) {break;}
	
						System.out.println("conll : "+path);
						File f = new File(path);
						if (f.isFile()) { // always true
							
							// Create new resourceInfo object for file
							ResourceInfo resourceInfo_ = makeFileResource(resourceInfo, path, ProcessingFormat.CONLL, false);
							foundCONLL = true;
							conllFileHandler.parse(resourceInfo_);
							
							// count negative result in folder
							if (!(resourceInfo_.getFileInfo().getParseResult() == ParseResult.SUCCESS)) {
								stats.incrBad(folder);
							} else {
								stats.incrGood(folder);
							}
						}
					}
				}
				if (!stats.hasSamplesLeft()) {break;}
			}
			}
			

			if (!xmlFiles.isEmpty()) {
				
				System.out.println("Processing XML files");

			
			// Explanation of sampling parameters : ParseStats(100,20,0,200,)
			// Take at most 200 samples
			// Leave a folder if 100 good files were found
			// Leave a folder if 20 bad files were found.
			// Apply sampling to any resource > 0 files
			if (resourceInfo.getCustomParseStats() == null) {
			stats = new ParseStats(
					config.getInt("Sampling.Xml.thresholdForGood"),
					config.getInt("Sampling.Xml.thresholdForBad"),
					config.getInt("Sampling.Xml.activationThreshold"),
					config.getInt("Sampling.Xml.maxSamples"),
					xmlFiles.size());
			} else {
				stats = resourceInfo.getCustomParseStats().get(ResourceFormat.XML);
				stats.setVolumeSize(xmlFiles.size());
			}
			
			if (!resourceInfo.isSamplingActive()) {
				stats.setActivationThreshold(ParseStats.NO_SAMPLING);
			}
			
			for (String folder : filesInDirMap.keySet()) {
				
				System.out.println("Folder : "+folder);
				System.out.println("has "+filesInDirMap.get(folder).size()+ " files");
				
				for (String path : filesInDirMap.get(folder)) {

					if(xmlFiles.contains(path)) {
						
						if (stats.samplingIsDecided(folder)) {break;}
												
						// Create new resourceInfo object for file
						ResourceInfo resourceInfo_ = makeFileResource(resourceInfo, path, ProcessingFormat.XML, false);
						
				        System.out.println("XML found");
				        System.out.println(workerID +": Indexing " + path);
						
				        foundXML = true;
				        genericXmlFileHandler.parse(resourceInfo_, this);
						
						// count negative result in folder
				        if (!(resourceInfo_.getFileInfo().getParseResult() == ParseResult.SUCCESS)) {
						//if (resourceInfo_.getFileInfo().getStatusCode() == IndexUtils.NoDocumentsFoundInIndex) {
				        	stats.incrBad(folder);
						} else {
							stats.incrGood(folder);
						}
					}
				}
				if (!stats.hasSamplesLeft()) {break;}
				// Only recognized RDF,CONLL or XML files are processed !!!
				// If no usable files were found then the archive resource keeps the result NONE
			}
			}
			break;
			
			
		/*********************/
		/* LOAD LINGHUB DUMP */
        /*********************/	
		
		case LINGHUB :
			
			System.out.println("Linghub");
			
			File [] files = downloadFolder.listFiles();
			for (File f : files) {
				
				resourceInfo.getFileInfo().setResourceFile(f,
						LocateUtils.getRelFilePath(f, downloadFolder));
				
				/*// Cleanup linghub database - (old, for TDB)
				File linghubDatabaseDirectory = new File (config.getString("Databases.Linghub.DBDirectory"));
				if (!linghubDatabaseDirectory.exists()) {
					FileUtils.forceMkdir(linghubDatabaseDirectory);
				} else {
					FileUtils.cleanDirectory(linghubDatabaseDirectory);
				}*/
				
				// Load linghub into blazegraph
				try {
					com.bigdata.rdf.store.DataLoader.main(
					new String[] {config.getString("Databases.Blazegraph.loadProperties"),f.getAbsolutePath()});
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
				break;
			
				/*case XML : // never the case ??
				System.out.println("XML found");
				xmlFileHandler.parse(resourceInfo);
				break;
				*/

			default :
				break;
			}

		} else {
			
			

		/*****************/
		/*  SINGLE FILE  */
        /*****************/
			
		ResourceInfo resourceInfo_ = null;
 
		switch (resourceInfo.getResourceFormat()) {
		
			case RDF :
			    System.out.println(workerID +": Indexing : "+file.getAbsolutePath());
			    
			    resourceInfo_ = makeFileResource(resourceInfo, resourceInfo.getFileInfo().getAbsFilePath(),
						ProcessingFormat.RDF, true);
			    
			    // Validate RDF file and convert it eventually to rdfxml format
				if (ScriptUtils.validateRdfFile(resourceInfo_, config, validateRdfScript) == ScriptUtils.VALIDATION_ERROR) {
					resourceInfo.getFileInfo().setErrorMsg(IndexUtils.ERROR_IN_RDF_VALIDATION);
					resourceInfo.getFileInfo().setProcessingEndDate(new Date());
					break; // skip file
				};
			    
			    rdfFileHandler.parse(resourceInfo_, this);
				break;
				
			case CONLL :
				System.out.println("Conll found");
				
				resourceInfo_ = makeFileResource(resourceInfo, resourceInfo.getFileInfo().getAbsFilePath(),
						ProcessingFormat.CONLL, true);
				conllFileHandler.parse(resourceInfo_);
				break;
				
			case XML :
				System.out.println("XML found");
				resourceInfo_ = makeFileResource(resourceInfo, resourceInfo.getFileInfo().getAbsFilePath(),
						ProcessingFormat.XML, true);
				genericXmlFileHandler.parse(resourceInfo_, this);
				break;
				
			// testing HTML page with PDF download
			case PDF :
				System.out.println("PDF :+"+resourceInfo.getDataURL());
				break;
				
			case HTML : // TODO not implemented , resource has only links, add resourceInfo_ support

				HashMap <String, ResourceInfo> rsMap = new HashMap <String, ResourceInfo> ();
				
				// Parse links from HTML page
				for (String link : ParserHtml.parseLinksInFile(file, "UTF-8", url)) {
					
					Utils.debug(link);
					// Make new resources
					rsMap.put(link, new ResourceInfo (link,resourceInfo.getMetaDataURL(),"http://linghub/dummy/dataset", ResourceFormat.UNKNOWN));
				}

				// Filter resources & add them to queue
				for (ResourceInfo rsi : urlBroker.applyResourceFilter(urlBroker.getResourceFilterHTML(), rsMap, false)) {
					resourceQueue.add(rsi);
				}
				
				break;
				
			default :
				// only RDF and CONLL recognized
				setFileTypeNotSupported(resourceInfo);
				break;
		}
		}

		
		} catch (Exception e) {
				e.printStackTrace();
				resourceInfo.getFileInfo().setErrorMsg(StringUtils.substring(e.getMessage(),0,100));
				resourceInfo.getFileInfo().setProcessingEndDate(new Date());
		}
	
		System.out.println("Processing "+resourceInfo.getDataURL()+" is finished !");

		}
		
		// Stop the clock
		//Statistics.setEndDate(new Date());
	}

	
	private ResourceInfo makeFileResource(ResourceInfo resourceInfo, String filePath, ProcessingFormat fileFormat, boolean single) {
		
		// copy values of master resource object
		ResourceInfo resourceInfo_ = new ResourceInfo(resourceInfo.getDataURL(), resourceInfo.getMetaDataURL());
		resourceInfo_.setResource(resourceInfo.getResource());
		resourceInfo_.setHttpResponseCode(resourceInfo.getHttpResponseCode());
		resourceInfo_.setHttpContentType(resourceInfo.getHttpContentType());
		resourceInfo_.setHttpContentLength(resourceInfo.getHttpContentLength());
		resourceInfo_.setHttpLastModified(resourceInfo.getHttpLastModified());
		resourceInfo_.setResourceFormat(resourceInfo.getResourceFormat());
		
		resourceInfo_.getFileInfo().setProcessingStartDate(new Date());
		resourceInfo_.setUserID("ub");
		resourceInfo_.setResourceUploadImportMetadata(resourceInfo.getResourceUploadImportMetadata());
		
		// !!!
		resourceInfo_.getFileInfo().setTemporaryFilePath(resourceInfo.getFileInfo().getTemporaryFilePath());
		resourceInfo_.setResourceState(resourceInfo.getResourceState());
		
		resourceInfo_.setResourceUploadImportMetadata(resourceInfo.getResourceUploadImportMetadata());
		resourceInfo_.setResourceUploadAutoAccept(resourceInfo.getResourceUploadAutoAccept());

	
		// set file path
		if (!single) {
			resourceInfo_.getFileInfo().setResourceFile(new File(filePath), 
				LocateUtils.getRelFilePath(new File(filePath), downloadFolder));
		} else {
			resourceInfo_.getFileInfo().setResourceFile(new File(filePath));	
		}
		
		// set file size
		resourceInfo_.getFileInfo().setFileSizeInBytes(FileUtils.sizeOf(resourceInfo_.getFileInfo().getResourceFile()));
		
		// Register file in resource database
		resourceInfo_.getFileInfo().setFileVertex(downloadManager.getResourceManager().addFile(resourceInfo_, fileFormat));
	
		return resourceInfo_;
	}
	
	
	private void setFileTypeNotSupported(ResourceInfo resourceInfo) {
		
		resourceInfo.getFileInfo().setErrorMsg(IndexUtils.ERROR_FILE_TYPE_NOT_SUPPORTED);
		Vertex fileVertex = downloadManager.getResourceManager().addFile(resourceInfo, ProcessingFormat.UNKNOWN);
		downloadManager.getResourceManager().
		setFileError(
				resourceInfo.getResource(),
				fileVertex,
				IndexUtils.FileTypeNotSupported ,
				IndexUtils.ERROR_FILE_TYPE_NOT_SUPPORTED);
	}

	@Override
	public RdfFileHandlerI getRdfFileHandler() {
		return this.rdfFileHandler;
	}

	@Override
	public ConllFileHandler getConllFileHandler() {
		return this.conllFileHandler;
	}

	@Override
	public XMLFileHandlerI getXmlFileHandler() {
		return this.genericXmlFileHandler;
	}


	@Override
	public int getWorkerId() {
		return this.workerID;
	}


	@Override
	public XMLConfiguration getConfiguration() {
		return this.config;
	}


	@Override
	public Consumer getResourceConsumer() {
		// TODO Auto-generated method stub
		return null;
	}


	/* (non-Javadoc)
	 * @see de.unifrankfurt.informatik.acoli.fid.types.Worker#getActiveResource()
	 */
	@Override
	public ResourceInfo getActiveResource() {
		// TODO Auto-generated method stub
		return null;
	}

}