package de.unifrankfurt.informatik.acoli.fid.exec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.broker.BrokerService;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFFormat;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.io.IoCore;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.codehaus.plexus.util.FileUtils;

import de.unifrankfurt.informatik.acoli.fid.activemq.Consumer;
import de.unifrankfurt.informatik.acoli.fid.activemq.Producer;
import de.unifrankfurt.informatik.acoli.fid.detector.OptimaizeLanguageTools1;
import de.unifrankfurt.informatik.acoli.fid.gremlinQuery.EmbeddedQuery;
import de.unifrankfurt.informatik.acoli.fid.linghub.UrlBroker;
import de.unifrankfurt.informatik.acoli.fid.owl.ModelDefinition;
import de.unifrankfurt.informatik.acoli.fid.owl.OntologyManager;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.RMEmbedded;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.RMServer;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.TemplateManager;
import de.unifrankfurt.informatik.acoli.fid.search.GWriter;
import de.unifrankfurt.informatik.acoli.fid.search.GWriterN;
import de.unifrankfurt.informatik.acoli.fid.search.GWriterT;
import de.unifrankfurt.informatik.acoli.fid.search.GraphTools;
import de.unifrankfurt.informatik.acoli.fid.serializer.RDFSerializer;
import de.unifrankfurt.informatik.acoli.fid.spider.ConllFileHandler;
import de.unifrankfurt.informatik.acoli.fid.spider.DownloadManager;
import de.unifrankfurt.informatik.acoli.fid.spider.GenericRdfFileHandler;
import de.unifrankfurt.informatik.acoli.fid.spider.GenericXMLFileHandler;
import de.unifrankfurt.informatik.acoli.fid.spider.ResourceChecker;
import de.unifrankfurt.informatik.acoli.fid.spider.Statistics;
import de.unifrankfurt.informatik.acoli.fid.spider.VifaWorker;
import de.unifrankfurt.informatik.acoli.fid.spider.VifaWorkerMQ;
import de.unifrankfurt.informatik.acoli.fid.types.AccountType;
import de.unifrankfurt.informatik.acoli.fid.types.Backup;
import de.unifrankfurt.informatik.acoli.fid.types.DBType;
import de.unifrankfurt.informatik.acoli.fid.types.DatabaseConfiguration;
import de.unifrankfurt.informatik.acoli.fid.types.ExecutionMode;
import de.unifrankfurt.informatik.acoli.fid.types.InvalidModelDefinitionException;
import de.unifrankfurt.informatik.acoli.fid.types.ModelInfo;
import de.unifrankfurt.informatik.acoli.fid.types.ParseResult;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceCache;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceFormat;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceType;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceTypeInfo;
import de.unifrankfurt.informatik.acoli.fid.types.UpdatePolicy;
import de.unifrankfurt.informatik.acoli.fid.types.UserAccount;
import de.unifrankfurt.informatik.acoli.fid.types.Worker;
import de.unifrankfurt.informatik.acoli.fid.ub.PostgresManager;
import de.unifrankfurt.informatik.acoli.fid.util.IndexUtils;
import de.unifrankfurt.informatik.acoli.fid.util.LocateUtils;
import de.unifrankfurt.informatik.acoli.fid.util.ScriptUtils;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;
import de.unifrankfurt.informatik.acoli.fid.webclient.ExecutionBean;

/**
 * Setup class
 * 
 * @author frank
 * 
 */
//@Singleton
public class Executer {
	
	private static DatabaseConfiguration registryDbConfig;
	private static DatabaseConfiguration dataDbConfig;
	private static XMLConfiguration fidConfig;
	private static UpdatePolicy updatePolicy;
	private Cluster cluster;
	
	private ExecutorService exs;
	private BrokerService activemqBroker;
	public final static String MQ_IN_1 = "WORKER-IN-1";
	public final static String MQ_OUT_1 = "WORKER-OUT-1";
	public final static String MQ_Default = "SERVICE-1";
	private Worker[] workers;
	private Future<?>[] workerRef;
	private ArrayList<Worker> mqWorkers = new ArrayList<Worker>();

	
	public ResourceManager resourceManager = null;
	public TemplateManager templateManager;
	
	/*
	Use separate instances !!!
	RdfFileHandler rdfFileHandler = null;     // use own instance for each worker thread because of rdfStreamParser.reset(); in handler
	ConllFileHandler conllFileHandler = null; // not safe to use same instance in each worker thread !!!
	DownloadManager downloadManager = null;   // application crashes with multiple threads if only one instance is used for all worker threads !!!
	*/
	
	UrlBroker urlBroker = null;
	OntologyManager ontologyManager = null;
	public static ModelDefinition modelDefinition = null;
	
	public static GWriter writer = null;
	public Graph graph = null;
	
	Queue <ResourceInfo> _queue;
	Queue <ResourceInfo> _finishedQueue;
	HashSet <ResourceInfo> _resources = new HashSet <ResourceInfo>();
	static ArrayList <ResourceInfo> defaultResources = new ArrayList<ResourceInfo>();
	
	public static final String conllNs = "ufal.mff.cuni.cz/conll2009-st/task-description.html#";
	public static final HashSet<String> featureIgnoreList = new HashSet<String>() {
		private static final long serialVersionUID = 1363621361L;
	{
				add("word");
				add("msd");
				add("Translit");
				add("LTranslit");
				add("ref");
				add("Gloss");
				add("MGloss");
				add("MSeg");
				add("LGloss");
				add("LId");
				add("LDeriv");
				add("Vib");
				add("PLemma");
				add("PForm");
				add("Root");
				add("VForm");
				add("Vform");
				add("MorphInd");
				add("MWE");
				add("LvtbNodeId");
				add("Id");
				add("En");
				add("Orig");
				add("Morphs");
				add("Offset");
				add("LDeriv");
				add("LNumValue");
				add("ChunkId");
				add("Tam");
				add("AltTag");
				add("Alt");
				add("msd");
				add("word");
				add("Morf");
	}};
	
	
	private static boolean updateModels = true;
	private boolean initLanguageProfiles = true;
	
	 // 50 gigabytes max file size (uncompressed) (1 GB = 1073741824 bytes)
	private static Long uncompressedFileSizeLimitDefault = 50*1073741824L;
	
	// 1 gigabyte max file size compressed
	private static Long compressedFileSizeLimitDefault = 1073741824L;
	
	
	private boolean isTestRun = false;
	private boolean onlyUpdateModels = false;
	private boolean resetRegistryDbAtStartup = false;
	private boolean resetDataDbAtStartup = false;
	
	private ExecutionMode executionMode = ExecutionMode.UNDEFINED;
	
	public static final float coverageThresholdOnLoad = 0.4f; // only select models automatically with coverage >= threshold
	
	public LocateUtils locateUtils = new LocateUtils();
	private UserAccount userAccount;
	private File manualOptimaizeLanguageProfilesDir;
	
	public static boolean interrupted = false;
	
	// quick fix (remove)
	public static int instances = 0;

	public static boolean backupInProgress=false;	
	
	public void setTestRun(boolean isTestRun) {
		this.isTestRun = isTestRun;
	}
		
	

	// Constructor 1
	public Executer (XMLConfiguration fidconfig) {
		
		System.out.println("Executer called from : "+new Exception().getStackTrace()[1].getClassName());
		
		instances++;
		System.out.println("Executer instances : "+instances);
		
		if (instances > 1) return;
		
		
		fidConfig = fidconfig;
		
		// SET DEFAULT DATABASE CONFIGURATION
		// REG-DB : GremlinServer
		fidConfig.setProperty("DatabaseConfigurations.RegistryDBType","GremlinServer");

		// MOD-DB : Neo4J embedded
		fidConfig.setProperty("DatabaseConfigurations.DataDBType","Neo4J");
		
		makeDatabaseConfiguration();
	}

	

	// Constructor 2
	public Executer (DatabaseConfiguration registryDbConfiguration, DatabaseConfiguration dataDbConfiguration, XMLConfiguration fidconfig) {
		
		registryDbConfig = registryDbConfiguration;
		dataDbConfig = dataDbConfiguration;
		fidConfig = fidconfig;
	}


	/**
	 * Check application configuration parameter before running
	 */
	private boolean isConfigurationOK() {
		
		// Check VifaConfig.xml
		if (!IndexUtils.checkConfigAndSetDefaultValues(fidConfig)) return false;
		
		// Set the update policy after setting default values
		enableUpdatePolicy();

		Statistics.initialize();
	    //Statistics.printReport();			
		return true; 
	}
	

	
	public void run(ExecutionMode exeMode) {
		this.executionMode = exeMode;
		run();
	}
	
	
	public void run(UserAccount userAccount) {
		this.userAccount = userAccount;
		run();
	}
	
	/**
	 * Run the application
	 */
	public void run() {
		
		// Check configuration
		if (!isConfigurationOK()) {System.out.println("Config Error !");System.exit(0);}
		
		//initializeLanguageDetector();
		
		// If data database is loaded from file then reset all
		if (dataDbConfig.getDbType() != DBType.TinkerGraph && dataDbConfig.getDatabaseImportJsonFile() != null) {
			
			System.out.println("Changing execution mode from "+this.executionMode+" to execution mode RESET because"
					+ " of data database reload "+dataDbConfig.getDatabaseImportJsonFile());
			this.executionMode = ExecutionMode.RESET;
		}
		
		System.out.println("\n\nStarting application in execution mode : "+this.executionMode.toString());
		
		
		switch (this.executionMode) {
		
			case INIT :		// Delete everything and reload models in dataDb
				
				updateModels = true;
				resetRegistryDbAtStartup = true;
				resetDataDbAtStartup = true;

				break;
			
			case ADD :		// Add more data
				
				updateModels = false;
				resetRegistryDbAtStartup = false;
				resetDataDbAtStartup = false;
				
				break;
				
			case CLEAN :	// Reset registry (includes removing hit nodes, but keep models !!!
				
				updateModels = false;
				resetRegistryDbAtStartup = true;
				resetDataDbAtStartup = false;
				
				break;
			
			case RESET :	// Reset all data (do not update models)
				
				updateModels = false;
				resetRegistryDbAtStartup = true;
				resetDataDbAtStartup = true;
				
				break;
				
			case DBSTART :	// Startup databases and return (test only)
				
				updateModels = false;
				resetRegistryDbAtStartup = false;
				resetDataDbAtStartup = false;
				
				break;
				
			case RUNDBPATCH :	// Run database patch
				
				updateModels = false;
				resetRegistryDbAtStartup = false;
				resetDataDbAtStartup = false;
				
				break;

			case UNDEFINED :// Only use parameters from configuration file and setters
				
				updateModels = false;
				resetRegistryDbAtStartup = false;
				resetDataDbAtStartup = false;
				break;
				
			case MAKERESULT :
				
				updateModels = false;
				resetRegistryDbAtStartup = false;
				resetDataDbAtStartup = false;
				break;
				
			case MAKEURLPOOL :
				
				updateModels = false;
				resetRegistryDbAtStartup = false;
				resetDataDbAtStartup = false;
				
				// set configuration parameter
				fidConfig.setProperty("Linghub.enabled", "true");
				fidConfig.setProperty("Linghub.useQueries", "true");
				
				break;
				
			case SERVICE :
				
				updateModels = false;
				resetRegistryDbAtStartup = false;
				resetDataDbAtStartup = false;
				break;
				
			case PUBLICSERVICE :
				
				updateModels = false;
				resetRegistryDbAtStartup = false;
				resetDataDbAtStartup = false;
				initLanguageProfiles = false;
				break;
				
			case EXPORTDDB :
			case EXPORTRDB :
				
				updateModels = false;
				resetRegistryDbAtStartup = false;
				resetDataDbAtStartup = false;
				break;
				
			case UPDATEMODELS :
				
				updateModels = true;
				resetRegistryDbAtStartup = false;
				resetDataDbAtStartup = false;
				break;
				
				
			case CREATEUSER :
			case DELETEUSER :
			case SETUSERPRIVILEGES :
				
				updateModels = false;
				resetRegistryDbAtStartup = false;
				resetDataDbAtStartup = false;
				initLanguageProfiles = false;
				break;
			
			default :		// Only use parameters from configuration file and setters
				
				updateModels = false;
				resetRegistryDbAtStartup = false;
				resetDataDbAtStartup = false;
				break;
		}
		
		// NEVER delete a database with a configuration parameter - use explicite function call !
		// TODO remove parameter resetRegistryDbAtStartup, resetDataDbAtStartup !
		if (executionMode != ExecutionMode.INIT) {
			resetRegistryDbAtStartup = false;
			resetDataDbAtStartup = false;
		}
		
		
		// Enable automatic model update from configuration parameter
		if (fidConfig.getString("OWL.modelUpdateMode").equals("auto")) {
			// not yet updateModels = true;
		}
		
		
		if (initLanguageProfiles) {
			initializeLanguageDetector();
		}
		

		//startGremlinServer();
		
		//if (!isConfigurationOK()) {System.out.println("Config Error !");System.exit(0);}
		
		System.out.println("\nDATABASE CONFIGURATION");
		registryDbConfig.setName("Registry");
		registryDbConfig.printConfiguration();
		dataDbConfig.setName("Model");
		dataDbConfig.printConfiguration();
		
		
		// Instantiate data database
		if (this.executionMode != ExecutionMode.CREATEUSER && 
			this.executionMode != ExecutionMode.DELETEUSER &&
			this.executionMode != ExecutionMode.SETUSERPRIVILEGES) {
			
		switch (dataDbConfig.getDbType()) {
		
		case TinkerGraph :
			
			graph = TinkerGraph.open();
			
			try {
				graph.io(IoCore.graphson()).readGraph(dataDbConfig.getDatabaseImportJsonFile().getAbsolutePath());
				System.out.println("Loaded graph : "+dataDbConfig.getDatabaseImportJsonFile().getAbsolutePath());
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("(Could not load json file !)");
			}
			
			writer = new GWriterT (graph, fidConfig);
			if (resetDataDbAtStartup) {
				writer.deleteDatabase();
				System.out.println("DELETING MODEL DATABASE");
			}
			break;
		
		case Neo4J :
			
			writer = new GWriterN (dataDbConfig.getDatabaseDirectory(), fidConfig);
			
			if (resetDataDbAtStartup) {
				writer.deleteDatabase();
				System.out.println("DELETING MODEL DATABASE");
			}
			
			if (dataDbConfig.getDatabaseImportJsonFile() != null) {
				try { // Load data from json file into neo4j
					writer.getGraph().io(IoCore.graphson()).readGraph(dataDbConfig.getDatabaseImportJsonFile().getAbsolutePath());
					System.out.println("Loaded graph : "+dataDbConfig.getDatabaseImportJsonFile().getAbsolutePath());
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("(Could not load json file !)");
				}
			}
			
			
			break;
			
		default :

			System.out.println("Database type not implemented !");
			break;
		}
		
		}
		
		
    	// Instantiate registry database 
		switch (registryDbConfig.getDbType()) {
		
		case Neo4J :

			resourceManager = new RMEmbedded(
					registryDbConfig.getDatabaseDirectory().getAbsolutePath(),
	    			updatePolicy
	    			);
			if (resetRegistryDbAtStartup) {
				resourceManager.deleteDatabase();
				System.out.println("DELETING REGISTRY DATABASE");
				
				if (!resetDataDbAtStartup) {
					
					// Delete hit nodes in data database
					writer.deleteHitVertices();
				}
			}
			break;
					
			
		case GremlinServer :
			
			cluster = Cluster.open(makeBasicGremlinClusterConfig(fidConfig));
			//cluster = Cluster.open();
			//cluster = Cluster.build().port(8182).create();
			

			resourceManager = new RMServer(cluster, updatePolicy);
			if (resetRegistryDbAtStartup) {
				resourceManager.deleteDatabase();
				System.out.println("DELETING REGISTRY DATABASE");
				
				if (!resetDataDbAtStartup) {
				
					// Delete hit nodes in data database
					writer.deleteHitVertices();
				}
			}
			break;
		
			
		case TinkerGraph :
			
			TinkerGraph registryGraph = TinkerGraph.open();
			
			try {
				registryGraph.io(IoCore.graphson()).readGraph(registryDbConfig.getDatabaseImportJsonFile().getAbsolutePath());
				System.out.println("Loaded graph : "+registryDbConfig.getDatabaseImportJsonFile().getAbsolutePath());
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("(Could not load json file !)");
			}
			
			resourceManager = new RMEmbedded(
					registryGraph,
	    			updatePolicy
	    			);
			
			if (resetRegistryDbAtStartup) {
				resourceManager.deleteDatabase();
				System.out.println("CLEARING REGISTRY DATABASE");
				
				if (!resetDataDbAtStartup) {
					
					// Delete hit nodes in data database
					writer.deleteHitVertices();
				}
			}
			break;
			
		
		default :
			System.out.println("Database type not implemented !");
    		System.exit(0);
		}
		
		// 
		if (this.executionMode == ExecutionMode.CREATEUSER || 
			this.executionMode == ExecutionMode.DELETEUSER ||
			this.executionMode == ExecutionMode.SETUSERPRIVILEGES) {
			
			
			if (this.executionMode == ExecutionMode.CREATEUSER) {
				resourceManager.addUser(this.userAccount);
			}
			
			if (this.executionMode == ExecutionMode.DELETEUSER) {
				
				resourceManager.deleteUser(this.userAccount);
			}
			
			if (this.executionMode == ExecutionMode.SETUSERPRIVILEGES) {
				
				resourceManager.setUserPrivileges(this.userAccount);
			}
			
			resourceManager.closeDb();
			return;
		}
		
		if (this.executionMode == ExecutionMode.DBSTART) {
			
			try {
				modelDefinition = new ModelDefinition(fidConfig);
			} catch (InvalidModelDefinitionException e) {
				e.printStackTrace();
				System.out.println("Error while loading model definitions !");
				return;
			}
			
			System.out.println("Close DB manually with closeDBConnections() !!");
			return;
		}
		
		if (this.executionMode == ExecutionMode.RUNDBPATCH) {
			System.out.println("Running database patch :");
			
			try {
				modelDefinition = new ModelDefinition(fidConfig);
			} catch (InvalidModelDefinitionException e) {
				e.printStackTrace();
				System.out.println("Error while loading model definitions !");
				return;
			}
			
			executePatch();
			System.out.println("Running database patch finished !");
			closeDBConnections();
			return;
		}
		
		
		/*if (this.executionMode == ExecutionMode.MAKERESULT) {
			makeResult();
			closeDBConnections();
			return;
		}*/
		
		// Only make json serialisation of Data database
		if (this.executionMode == ExecutionMode.EXPORTDDB) {
			try {
				
				writer.saveAsML(dataDbConfig.getDatabaseExportJsonFile().getAbsolutePath());
				//GraphTools.saveAsJSON(writer.getGraph(), dataDbConfig.getDatabaseExportJsonFile().getAbsolutePath());
				System.out.println("Exporting data database to file : "+dataDbConfig.getDatabaseExportJsonFile().getAbsolutePath()+" done !");
			} catch (Exception e) {
				System.out.println("DataDB export error :");
				e.printStackTrace();
			}
			closeDBConnections();
			return;
		}
		
//		// Only make json serialisation of Data database
//		if (this.executionMode == ExecutionMode.EXPORTRDB) {
//			
//			try {
//				
//				if (!resourceManager.getClass().equals(RMServer.class)) {	
//				
//					System.out.println("Exporting registry database -> "+registryDbConfig.getDatabaseExportJsonFile().getAbsolutePath());
//					GraphTools.saveAsJSON(resourceManager.getGraph(), registryDbConfig.getDatabaseExportJsonFile().getAbsolutePath());
//				} else {
//					System.out.println("Sorry, can export local registryDB but not server registryDB !");
//					System.out.println("To run a query please connect to gremlin server via :");
//					System.out.println(":remote connect tinkerpop.server conf/remote.yaml session");
//					System.out.println(":remote console)");
//					System.out.println();
//				}
//				
//				} catch (Exception e) {
//					System.out.println("RegistryDB export error :");
//					e.printStackTrace();
//					}
//			
//				closeDBConnections();
//				return;
//		}
		
		 
		// Init language detector
		OptimaizeLanguageTools1.setFIDConfig(fidConfig);
		
		// Init OLiA model definitions
		try {
			modelDefinition = new ModelDefinition(fidConfig);
			
			ResourceChecker resourceChecker = new ResourceChecker(
				 new DownloadManager(
				    	resourceManager, // global instance (Neo4J) or separate instance from above (GremlinServer)
				    	new File (fidConfig.getString("RunParameter.downloadFolder")), updatePolicy, fidConfig, 10)
					);
			
			
			if (fidConfig.getBoolean("OWL.checkModelsOnlineAtStartup")) {
				
				System.out.println("Checking if OLiA ontology files are online ...");
				ModelInfo.checkModelsOnline(modelDefinition.getModelInfoList(), resourceChecker);
				int notOnline = 0;
				for (ModelInfo mi : modelDefinition.getModelInfoList()) {
					if (!mi.isOnline()) {
						System.out.println("Model file \n"+mi.getUrl()+" is not online !\n"+"check model URL in \n"+
								modelDefinition.getModelFile().getAbsolutePath());
						notOnline++;
					}
				}

				if (notOnline > 0 && fidConfig.getBoolean("OWL.checkModelsOnlineAtStartupStopOnFail")) {
					System.out.println("Some Ontology files are not online !\n"+ "check model URLs in \n"+
							modelDefinition.getModelFile().getAbsolutePath());
					System.exit(0);
				}
			}

			
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Model definitions have an error,"
					+ " please check \n"+modelDefinition.getModelFile().getAbsolutePath());
			System.exit(0);
		}
		
	
		// validate backups
		List<String> errors = 
				Backup.validateBackups(new File(getFidConfig().getString("Backup.directory"),"backups.json"));
		if (!errors.isEmpty()) {
			Utils.debug("Errors have been found in backup data :");
			for (String error : errors) {
				Utils.debug(error);
			}
			Utils.debug("Please fix errors before restarting !");
			System.exit(0);
		}
		
		// Init blacklisted predicates
		resourceManager.initPredicates();
		
    	// Update ontology models
    	ontologyManager = new OntologyManager(this, new DownloadManager(
    			resourceManager, // global instance (Neo4J) or separate instance from above (GremlinServer)
    			new File (fidConfig.getString("RunParameter.downloadFolder")), updatePolicy, fidConfig, 60), fidConfig, modelDefinition);
    	
    	
    	// Init TemplateManager
    	templateManager = new TemplateManager(resourceManager);
    	
    	// Update ontology models (default true)
    	if(updateModels) {
    		
    			switch (this.executionMode) {
    			
    			case INIT :
    				ontologyManager.initModelGraph();
    				//ontologyManager.updateModelsOld(null);
    				break;
    				
    			case UPDATEMODELS :
    				ontologyManager.updateOliaModels(null);
    				break;
    			
    			// only active iff RunParameter.modelUpdateMode = auto
    			default :
    				//ontologyManager.updateAllModels();
    				break;
    			}
    		
    			
    			//loadXMLTemplates(templateManager);
    	} else {
    		if (this.executionMode == ExecutionMode.CLEAN) {
    			//loadXMLTemplates(templateManager);
    		}
    	}
    	
    	
    	// Breakpoint for -IN option (Initialization)
    	if (this.executionMode == ExecutionMode.UPDATEMODELS || 
    		this.executionMode == ExecutionMode.INIT) {
    		closeDBConnections();
			return;
		}
    	
    	// Break point for test
    	if (onlyUpdateModels) return;
    	
    	
    	loadXMLTemplates(templateManager);
    	    	
    	
    	// For test only !
    	// Use given resources (and not linghub resources) by setting defaultResources to s.t.
    	if (defaultResources != null && !defaultResources.isEmpty()) {
    		_resources.addAll(defaultResources);
    		
    	} else {

    		
        	if (
        			fidConfig.getBoolean("Linghub.enabled") &&
        			
        			(!UpdatePolicy.valueOf(fidConfig.getString("RunParameter.updatePolicy").toUpperCase().trim()).
        					equals(UpdatePolicy.UPDATE_ALL) 
        						|| fidConfig.getBoolean("Linghub.forceUpdate"))
        			)
        		{
        	
	        	ResourceInfo linghubDumpOnline = new ResourceInfo(fidConfig.getString("Linghub.linghubDataDumpURL"),
	        			"http://linghub.org","http://linghub/dummy/dataset", ResourceFormat.LINGHUB);
	        	
	        	// override update_policy (only debug)
	        	linghubDumpOnline.getFileInfo().setForceRescan(fidConfig.getBoolean("Linghub.forceUpdate"));
	        	
	        	System.out.println("Updating linghub dump !");
	        	
	        	_resources.add(linghubDumpOnline);
	        	VifaWorker linghubUpdate = new VifaWorker(
	        			0,
	        			new ConcurrentLinkedQueue<ResourceInfo>(_resources),
	        			new GenericRdfFileHandler (writer, resourceManager),
	            		new ConllFileHandler(writer, resourceManager, fidConfig),
	        			fidConfig,
	        			new DownloadManager(
	        					resourceManager, // global instance (Neo4J) or separate instance from above (GremlinServer)
	        	    			new File (fidConfig.getString("RunParameter.downloadFolder")), registryDbConfig.getUpdatePolicy(), fidConfig, 10),
	        			null, 
	        			new GenericXMLFileHandler(writer, resourceManager, templateManager, fidConfig));
	        	
		        	linghubUpdate.run();
        	}	
        	
        	urlBroker = new UrlBroker(
        			resourceManager,
        			fidConfig);
        	
        	_resources.clear();
        	_resources = urlBroker.makeUrlPool();
    	}
    	
    	
    	if (this.executionMode == ExecutionMode.MAKERESULT) {
			makeResult();
			closeDBConnections();
			return;
		}
		

    	
    	// Breakpoint for -mf make-filelist option
    	if (this.executionMode == ExecutionMode.MAKEURLPOOL) {
    		System.out.println(".. done making filelist !");
    		closeDBConnections();
			return;
		}
    	
 
    	// Start activemq (see activemq package)	
    	
    	// *** Start main process ***
    	_queue = new ConcurrentLinkedQueue<ResourceInfo>(_resources);
    	_finishedQueue = new ConcurrentLinkedQueue<ResourceInfo>();

    	
    	
  	    int threadPoolSize;
  	    try {
  	    	threadPoolSize = Integer.parseInt(fidConfig.getString("RunParameter.threads"));
  	    }
  	    catch (Exception e) {
  	    	threadPoolSize = 1;
  	    }
  	    
  	  if (this.executionMode == ExecutionMode.SERVICE) {
  		  threadPoolSize = 0;
  	  }
  	    
		
  	    System.out.println("Starting application with "+threadPoolSize+" thread(s)");
  	    
		exs = Executors.newFixedThreadPool(threadPoolSize+1);
	    
	    workers = new Worker[threadPoolSize+1];
	    workerRef = new Future<?>[threadPoolSize+1];
	    for (int i = 0; i < threadPoolSize+1; i++) {
	    	
	    	
	    	switch (registryDbConfig.getDbType()) {
	    	
	    		case TinkerGraph :
	    		case Neo4J :	// Share same resourceManager
	    		
	    		if (i != threadPoolSize) {
		        workers[i] = new VifaWorker(
		            		i, 
		            		_queue,
		            		new GenericRdfFileHandler (writer, resourceManager),
		            		new ConllFileHandler(writer, resourceManager, fidConfig),
		            		fidConfig,
		            		new DownloadManager(
		                			resourceManager,
		                			new File (fidConfig.getString("RunParameter.downloadFolder")),
		                			updatePolicy, fidConfig, 10),
		            		urlBroker, // TODO also seperate object per thread ???
		            		new GenericXMLFileHandler(writer, resourceManager, templateManager, fidConfig)
		            		);
	    		} else {
	    			
	    		// Start one worker with activemq for consuming web-service requests
	    		if (this.executionMode == ExecutionMode.SERVICE) {
	    		
	    		// Force update !
	    		updatePolicy = UpdatePolicy.UPDATE_ALL;
	    		
	    		workers[i] = new VifaWorkerMQ(
	            		i, 
	            		new Consumer(Executer.MQ_IN_1),
	            		new Producer(Executer.MQ_OUT_1),
	            		new GenericRdfFileHandler (writer, resourceManager),
	            		new ConllFileHandler(writer, resourceManager, fidConfig),
	            		fidConfig,
	            		new DownloadManager(
	                			resourceManager,
	                			new File (fidConfig.getString("RunParameter.downloadFolder")),
	                			updatePolicy, fidConfig, 10),
	            		new GenericXMLFileHandler(writer, resourceManager, templateManager, fidConfig)
	            		);
	    		mqWorkers.add(workers[i]);
	    		}
	    		}
		        	break;
		        			

	    		case GremlinServer :	// Each worker gets its own resourceManager (client)
	    			 
	    			 ResourceManager resourceManager = new RMServer(cluster, updatePolicy);
	    			 
	    			 if (i != threadPoolSize) {
	    			 workers[i] = new VifaWorker(
			            		i, 
			            		_queue,
			            		new GenericRdfFileHandler (writer, resourceManager),
			            		new ConllFileHandler(writer, resourceManager, fidConfig),
			            		fidConfig,
			            		new DownloadManager(
			                			resourceManager,
			                			new File (fidConfig.getString("RunParameter.downloadFolder")),
			                			updatePolicy, fidConfig, 10),
			            		urlBroker, // TODO also separate object per thread ???
			            		new GenericXMLFileHandler(writer, resourceManager, templateManager, fidConfig)
			            		);
	    			} else {
	    			// Start one worker with activemq for consuming web-service requests
	    			if (this.executionMode == ExecutionMode.SERVICE) {
	    		    	workers[i] = new VifaWorkerMQ(
    	            		i, 
    	            		new Consumer(Executer.MQ_IN_1),
    	            		new Producer(Executer.MQ_OUT_1),
    	            		new GenericRdfFileHandler (writer, resourceManager),
    	            		new ConllFileHandler(writer, resourceManager, fidConfig),
    	            		fidConfig,
    	            		new DownloadManager(
    	                			resourceManager,
    	                			new File (fidConfig.getString("RunParameter.downloadFolder")),
    	                			updatePolicy, fidConfig, 10),
    	            		new GenericXMLFileHandler(writer, resourceManager, templateManager, fidConfig)
    	            		);
	    		    mqWorkers.add(workers[i]);
	    			}
	    			}
	    			
	    			break;
	    			
	    			
	    		default :
	    			System.out.println("Database type not recognized - stopping !");
	    			System.exit(0);
	    			break;
	    			
	    	}
	    	
	    	
			// avoid starting non existing worker in case external queue is off
	    	// TODO more clear implementation for external queue !
	        if (i != threadPoolSize) {
	        	//exs.execute(workers[i]);
	        	workerRef[i] = exs.submit(workers[i]);
	        }
	        else {
	        	if (fidConfig.getBoolean("RunParameter.startExternalQueue")) {
	        		//exs.execute(workers[i]);
	        		
	        		if (this.executionMode != ExecutionMode.PUBLICSERVICE) // TODO otherwise Error
	        			workerRef[i] = exs.submit(workers[i]);
	        	}
	        }
	        }
	    
	    	if (this.executionMode != ExecutionMode.SERVICE) {
	    	
	    	exs.shutdown(); //exs.shutdownNow(); // will shutdown immediately
	         
	    	
	    	// Stopping service will terminate the application
	    	// This is required for test runs. The application will run 'forever' since
	    	// the service worker runs in an endless loop waiting for messages in the activemq
	    	if (isTestRun  && fidConfig.getBoolean("RunParameter.startExternalQueue")) {
	    		stopService();
	    	}
	        
	    	try {
	          // Wait for all workers to shutdown
	  		while (!exs.awaitTermination(20, TimeUnit.SECONDS)) {
	  				  System.out.println("VifaWorkers still running ...");
	  				}
	  		} catch (InterruptedException e) {
	  			e.printStackTrace();
	  		}
	    	
	    	closeDBConnections();
	    	
	    	}
	}
	
	private void initializeLanguageDetector() {


		File externalOptimaizeLanguageProfilesDir = null;

		ArrayList<File> optimaizeLanguageProfiles = new ArrayList<File>();
		File lexvoRdfFile=null;
		
		
		// Set directory that contains extra language profiles for Optimaize language detector
		if (fidConfig.containsKey("RunParameter.OptimaizeExtraProfilesDirectory")) {
			externalOptimaizeLanguageProfilesDir = new File(fidConfig.getString("RunParameter.OptimaizeExtraProfilesDirectory"));
		}
		if (fidConfig.containsKey("RunParameter.OptimaizeManualProfilesDirectory")) {
			manualOptimaizeLanguageProfilesDir = new File(fidConfig.getString("RunParameter.OptimaizeManualProfilesDirectory"));
			
			// create directory if not exist
			if(!manualOptimaizeLanguageProfilesDir.exists()) {
				FileUtils.mkdir(manualOptimaizeLanguageProfilesDir.getAbsolutePath());
			}
		}
		
		
		if (externalOptimaizeLanguageProfilesDir == null || !externalOptimaizeLanguageProfilesDir.exists()) {
			
			// Use extra language profiles			
			if (this.executionMode != ExecutionMode.SERVICE) {
				
				HashSet<String> profileNames = new HashSet<String>(locateUtils.getJarFolderFileList("OptimaizeExtraProfiles/ok/"));
				
				System.out.println("");
				
				System.out.println("Using additional language profiles :");
				for (String profileName : profileNames) {
					System.out.print(profileName+" ");
					optimaizeLanguageProfiles.add(locateUtils.getLocalFile("/OptimaizeExtraProfiles/ok/"+profileName));
				}
			} else {
				
				List<File> profiles = locateUtils.getLocalDirectoryFileList("/OptimaizeExtraProfiles/ok/");
				
				System.out.println("Using additional language profiles :");
				for (File profile : profiles) {
					System.out.print(profile.getName()+" ");
					// Via fs
					optimaizeLanguageProfiles.add(profile);
					// Via resourceAsStream
					//optimaizeLanguageProfiles.add(locateUtils.getLocalFile("/OptimaizeExtraProfiles/ok/"+profile.getName()));
				}
				
			}
			System.out.println();
		
		} else {
			// Load extra language profiles from custom external directory
			System.out.println("Using custom additional profiles :");

			for (File file : externalOptimaizeLanguageProfilesDir.listFiles()){
				if (!file.isDirectory()) {
					System.out.println(file.getAbsolutePath());
					optimaizeLanguageProfiles.add(file);
				}
			}
		}
		
		// Set manual language profiles
		ArrayList<File> optimaizeLanguageProfilesManual = new ArrayList<File>();
		if (manualOptimaizeLanguageProfilesDir != null && manualOptimaizeLanguageProfilesDir.exists()) {

			for (File file : manualOptimaizeLanguageProfilesDir.listFiles()){
				if (!file.isDirectory()) {
					Utils.debug(file.getAbsolutePath());
					optimaizeLanguageProfilesManual.add(file);
				}
			}
		}
		
		// Set lexvo RDF file
		if (fidConfig.containsKey("RunParameter.LexvoRdfFile")) {
			lexvoRdfFile = new File(fidConfig.getString("RunParameter.LexvoRdfFile"));
		}
		
		// Use default lexvo file
		if (lexvoRdfFile == null || !lexvoRdfFile.exists()) {
			lexvoRdfFile = locateUtils.getLocalFile("/owl/lexvo/lexvo_2013-02-09.rdf.gz");
		}
		
		
		// Init language detection
		OptimaizeLanguageTools1.initLanguageDetector(fidConfig, optimaizeLanguageProfiles, optimaizeLanguageProfilesManual, lexvoRdfFile);
	}


	/**
	 * Run database patch
	 */
	private void executePatch() {
		
		// DON'T !!!
		// executer.setUpdatePolicy(UpdatePolicy.UPDATE_ALL);
		// executer.setExecutionMode(ExecutionMode.DBSTART);
		// executer.run();
		// DON'T !!!
		
		
	

		// Archived patches (do not run again)
		// 1. Patches.addRdfProperty2ModelsTest (success)
		// 2. Patches.addFileSizePropertyTest (success)
		// 3. Patches.addMetadataSubjectPropertyTest (success)
		// 4. Patches.deleteNoThingClasses (success)
		// 5. Patches.addBLLModelTest (success)
		// 6. Patches.addRdfPropertyUbTitle2MetadataTest (success)
		// 7. Patches.fixFileRelPathFileNameFileAbsPathTest (success)
		// 8. Patches.deleteUnprocessedFiles (success)
		// 9. Patches.deleteResourceWithOnlyUnprocessedFiles (success)
		// 10. Patches.changeFormatXML2CONLL (success)
		// 11. UpdateNamespacesPatch.updateModelNameSpaces (success)
		// 12. addResourceETagPropertyTest (success)
		// 13. updateUnitTokenCounts (success)
		// 14. removeCorruptTokens (success)
		// 15. deleteDuplicateClasses (success)
		// 16. addModelLangaugeEdgeDateProperty (success)
		// 17. addLanguageModelUpdateTextProperty (success)
		// 18. addUserQuotaProperties (success)
		// 19. assignResources2UbUser (success)
		// 20. updateResourceSampleTextAndLanguageSampleText2HexFormat (success)
		// 21. addMetadataLicenseSourceIdentifier (success)
		// 22. remove file orphans (from gremlin console !)
		// 23. addMd5AndSha256Properties (success)
		// 24. updateMd5AndSha256Values (success) 1964/2207 with hash
	}
	
	
	
	public static Configuration makeBasicGremlinClusterConfig() {
        final Configuration conf = new BaseConfiguration();
        conf.setProperty("hosts", Arrays.asList("127.0.0.1"));
        conf.setProperty("connectionPool.maxContentLength", 8000000); // important !
        conf.setProperty("port", Integer.parseInt(fidConfig.getString("Databases.GremlinServer.port")));
        return conf;
    }
	

	public static Configuration makeBasicGremlinClusterConfig(XMLConfiguration fidConfig) {
        final Configuration conf = new BaseConfiguration();
        conf.setProperty("hosts", Arrays.asList("127.0.0.1"));
        conf.setProperty("connectionPool.maxContentLength", 8000000); // important !
        conf.setProperty("port", Integer.parseInt(fidConfig.getString("Databases.GremlinServer.port")));
        return conf;
    }
	
    public static Configuration makeGremlinClusterConfig(XMLConfiguration fidConfig) {
        final Configuration conf = new BaseConfiguration();
        conf.setProperty("port", Integer.parseInt(fidConfig.getString("Databases.GremlinServer.port")));
        conf.setProperty("nioPoolSize", 16);
        conf.setProperty("workerPoolSize", 32);
        //conf.setProperty("username", "user1");
        //conf.setProperty("password", "password1");
        //conf.setProperty("jaasEntry", "JaasIt");
        //conf.setProperty("protocol", "protocol0");
        conf.setProperty("hosts", Arrays.asList("127.0.0.1"));
        //conf.setProperty("serializer.className", "my.serializers.MySerializer");
        //conf.setProperty("serializer.config.any", "thing");
        conf.setProperty("connectionPool.enableSsl", false);
        //conf.setProperty("connectionPool.keyCertChainFile", "X.509");
        //conf.setProperty("connectionPool.keyFile", "PKCS#8");
        //conf.setProperty("connectionPool.keyPassword", "password1");
        //conf.setProperty("connectionPool.trustCertChainFile", "pem");
        conf.setProperty("connectionPool.minSize", 100);
        conf.setProperty("connectionPool.maxSize", 200);
        conf.setProperty("connectionPool.minSimultaneousUsagePerConnection", 300);
        conf.setProperty("connectionPool.maxSimultaneousUsagePerConnection", 400);
        conf.setProperty("connectionPool.maxInProcessPerConnection", 600);
        conf.setProperty("connectionPool.minInProcessPerConnection", 500);
        conf.setProperty("connectionPool.maxWaitForConnection", 700);
        conf.setProperty("connectionPool.maxContentLength", 8000000);
        conf.setProperty("connectionPool.reconnectInterval", 900);
        conf.setProperty("connectionPool.resultIterationBatchSize", 1100);
        //conf.setProperty("connectionPool.channelizer", "channelizer0");

        return conf;
    }
	
    
	
	public Boolean closeDBConnections() {
		
		boolean error = false;
		
		try {
			writer.getGraph().close();
		} catch (Exception e) {
			e.printStackTrace();
			error=true;
		}
		
		try {
			resourceManager.closeDb();
		} catch (Exception e) {
			e.printStackTrace();
			error=true;
		}
		return error;
	}
	
	
	
	public void skipMQJob() {
		
		System.out.println("stopMQJob");

		int mqWorkerId = workerRef.length-1;
		VifaWorkerMQ mqw = (VifaWorkerMQ) workers[mqWorkerId];
		
		// close url consumer queue in worker first !!! (otherwise queue will not reconnect)
		mqw.getResourceConsumer().close();
		
		// stop MQ worker thread
		boolean z = workerRef[mqWorkerId].cancel(true);
		
		// wait until worker is stopped
		while (!workerRef[mqWorkerId].isCancelled()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// restart MQ worker thread
		workerRef[mqWorkerId] = exs.submit(mqw);
	}
	
	public void stopMQWorker() {
		System.out.println("stopMQWorker");
		workerRef[workerRef.length-1].cancel(true);
	}
	
	
	public void stopService() {
		 // Stop activemq -> is needed to terminate web-service worker
        try {
  			activemqBroker.stop();
  			System.out.println("Activemq stop");
  		} catch (Exception e) {
  			e.printStackTrace();
  		}
		
	}
	
	public void stop() {
		
        try {
        	System.out.println("Stopping service");
        	exs.shutdown();
        	closeDBConnections();
        	
  		} catch (Exception e) {
  			e.printStackTrace();
  		}
        
        // will stop the webapp
        //System.exit(0);
		
	}
	
	
	/**
	 * Create RDF from results in database which is then used for the UB export (non - service mode)
	 * @deprecated
	 */
	// TODO implementation after several changes not tested
	public void makeResult() {
		
		
		System.out.println("exportRDF");
		
		// I.   Get successfull resources
		ArrayList<ResourceInfo> rfl = getResourceManager().getSuccessFullResourceFilesRI();
	
		// II.  Add Linghub metadata		(if no manual metadata exists)
		if (fidConfig.getBoolean("Linghub.enabled")) {
			getUrlBroker().sparqlLinghubAttributes(true, rfl);
		}
		
		// IIa) Add Clarin metadata			(if no manual or Linghub metadata exists)
		PostgresManager mng = new PostgresManager (getFidConfig());
		mng.updateClarinMetadata(rfl);
		
		// III. Add match info to resources objects that have results
		getWriter().getQueries().getFileResults(rfl, getResourceManager());
		
		// IV. Add BLL results
    	String exportString = "";		
		
		//HashMap<String,HashSet<String>> bllModelMap = getWriter().getQueries().queryBll(bllMatrixParser);

		HashSet<String> doneResources = new HashSet<String>();
		
		Model rdfModel = RDFSerializer.serializeResourceInfo2RDFModelIntern(
				(ArrayList<ResourceInfo>) rfl,
				getWriter().getBllTools(),
				fidConfig,
				getModelDefinition()
				);
				

		//rdfModel = RDFSerializer.serializeResourceInfo2RDFModel((ArrayList<ResourceInfo>) rfl, null, null, null);
		System.out.println("RDF model built !");

		exportString = RDFSerializer.serializeModel(rdfModel, RDFFormat.TURTLE_PRETTY);
		
		//System.out.println(exportString); // testing
		System.out.println("RDF model serialized !");
		String rdfExportFile = fidConfig.getString("RunParameter.RdfExportFile");

		if (!exportString.isEmpty()) {
    		System.out.println("Saving RDF export to file "+rdfExportFile+" ...");
    		Utils.writeFile(new File(rdfExportFile), exportString);
    	}
		
    	/*
    	HashMap<String,HashSet<String>> bllModelMap = new HashMap<String,HashSet<String>>(); 
    	int counter = 1;
		int all = rfl.size();
		for (ResourceInfo resourceInfo : rfl) {
			if (!doneResources.contains(resourceInfo.getDataURL())) {
				doneResources.add(resourceInfo.getDataURL());
				System.out.println((counter++)+"/"+all+" querying BLL on "+resourceInfo.getDataURL());
				bllModelMap.putAll(getWriter().getQueries().queryBllNew(bllMatrixParser, resourceInfo));
			}
		}*/
	}
	
	

	public void setRegistryDbConfig(DatabaseConfiguration registryDbConfig_) {
		registryDbConfig = registryDbConfig_;
	}
	
	
	public void setDataDbConfig(DatabaseConfiguration dataDbConfig_) {
		dataDbConfig = dataDbConfig_;
	}
	
	
	public void setVifaConfig(XMLConfiguration vifaConfig) {
		fidConfig = vifaConfig;
	}
	
	public void setUpdatePolicy(UpdatePolicy updatePolicy_) {
		updatePolicy = updatePolicy_;
		fidConfig.setProperty("RunParameter.updatePolicy", updatePolicy.toString());
	}
	
	
	public void setResources(ResourceInfo testResource) {
		defaultResources.clear();
		defaultResources.add(testResource);
	}
	
	public void setResources(ArrayList<ResourceInfo> testResources) {
		defaultResources = testResources;
	}

	public static EmbeddedQuery getDataDBQueries() {
		return writer.getQueries();
	}
	
	
	public GWriter getWriter() {
		return writer;
	}
	
	public ResourceManager getResourceManager() {
		return resourceManager;
	}
	
	public ResourceManager createNewResourceManagerInstance() {
		
		switch (DBType.valueOf(fidConfig.getString("DatabaseConfigurations.RegistryDBType"))) {

		case GremlinServer :
			
			Cluster cluster = Cluster.open(Executer.makeBasicGremlinClusterConfig(fidConfig));
			UpdatePolicy updatePolicy = UpdatePolicy.valueOf(
					fidConfig.getString("RunParameter.updatePolicy"));
			return new RMServer(cluster, updatePolicy);
		
		default :
			Utils.debug("Error in database configuration");
			return null;
	}
	}
	
	public Long getUncompressedFileSizeLimitDefault() {
		return this.uncompressedFileSizeLimitDefault;
	}

	public void setUncompressedFileSizeLimit(long limitInBytes) {
		fidConfig.setProperty("RunParameter.uncompressedFileSizeLimit", limitInBytes);
	}
	
	public Long getCompressedFileSizeLimitDefault() {
		return compressedFileSizeLimitDefault;
	}

	public void setCompressedFileSizeLimit(Long limitInBytes) {
		fidConfig.setProperty("RunParameter.compressedFileSizeLimit", limitInBytes);
	}

	public ExecutionMode getExecutionMode() {
		return executionMode;
	}

	public void setExecutionMode(ExecutionMode executionMode) {
		this.executionMode = executionMode;
	}

	public DatabaseConfiguration getDataDbConfig() {
		return dataDbConfig;
		
	}

	public static XMLConfiguration getFidConfig() {
		return fidConfig;
	}
	
	private void loadXMLTemplates(TemplateManager templateManager) {
		
		File templateFile = null;
		
		if (fidConfig.containsKey("RunParameter.XMLParserConfiguration.templateFolder")) {
			templateFile = new File(fidConfig.getString("RunParameter.XMLParserConfiguration.templateFolder"));
		}
		
		// Use default templates
		if (templateFile == null || !templateFile.exists()) {
			templateFile = locateUtils.getLocalFile("/templates.json");
		}
		
		templateManager.loadTemplatesToDatabase_(templateFile);
		System.out.println("Loaded templates from "+templateFile.getAbsolutePath());
	}
	
	
	private static void makeDatabaseConfiguration() {
		
		//*****************************
		//   DATABASE CONFIGURATION   *
		//*****************************
		
		// Registry database //
		switch (DBType.valueOf(fidConfig.getString("DatabaseConfigurations.RegistryDBType"))) {
		
		case GremlinServer :
			registryDbConfig = new DatabaseConfiguration (DBType.GremlinServer,null,null,null,fidConfig);
			break;
			
		case Neo4J :
			registryDbConfig = new DatabaseConfiguration (
					DBType.Neo4J,new File(fidConfig.getString("Databases.Registry.Neo4jDirectory")),null,null,null);
			break;
		
		case TinkerGraph :
			registryDbConfig = new DatabaseConfiguration (
					DBType.TinkerGraph,null,null,null,null);
			break;
			
		default :
			// Error type not recognized !
			break;
		}
		

		// Data database //
		switch (DBType.valueOf(fidConfig.getString("DatabaseConfigurations.DataDBType"))) {
		
		case GremlinServer :
			dataDbConfig = new DatabaseConfiguration (DBType.GremlinServer,null,null,null,null);
			break;
			
		case Neo4J :
			dataDbConfig = new DatabaseConfiguration (
					DBType.Neo4J,new File(fidConfig.getString("Databases.Data.Neo4jDirectory")),null,null,null);
			break;
		
		case TinkerGraph :
			dataDbConfig = new DatabaseConfiguration (
					DBType.TinkerGraph,null,null,null,null);
			break;
			
		default :
			// Error type not recognized !
			break;
		
		}
	}
	
	
	
	private static void enableUpdatePolicy() {
		
		if (registryDbConfig.getUpdatePolicy() != null) {
			updatePolicy = registryDbConfig.getUpdatePolicy();
		} else {
		updatePolicy = UpdatePolicy.valueOf(fidConfig.getString("RunParameter.updatePolicy"));
		}
	}
	

	public Runnable[] getWorkers() {
		return workers;
	}

	public Queue<ResourceInfo> get_queue() {
		return _queue;
	}

	public Queue<ResourceInfo> get_finishedQueue() {
		return _finishedQueue;
	}

	public Cluster getCluster() {
		return cluster;
	}

	public UrlBroker getUrlBroker() {
		return urlBroker;
	}


	public ModelDefinition getModelDefinition() {
		return this.modelDefinition;
	}


	public static boolean isInterrupted() {
		return interrupted;
	}


	public static void setInterrupted(boolean interrupted) {
		Executer.interrupted = interrupted;
	}

	
	public ArrayList<ResourceInfo> getActiveResources(String userID) {
		
		ArrayList<ResourceInfo> activeResourcesOwnedByUser = new ArrayList<ResourceInfo>();
		
		for (ResourceInfo resourceInfo : getActiveResources()) {
			if (resourceInfo.getUserID().equals(userID)) {
				activeResourcesOwnedByUser.add(resourceInfo);
			}
		}
		
		return activeResourcesOwnedByUser;
	}
	
	
	public ArrayList<ResourceInfo> getActiveResources() {
		
		ArrayList<ResourceInfo> activeResources = new ArrayList<ResourceInfo>();
		
		for (Worker worker : mqWorkers) {
			System.out.println("check active resource in worker : "+worker.getWorkerId());
			ResourceInfo resourceInfo = worker.getActiveResource();
			if (resourceInfo != null) {
				activeResources.add(resourceInfo);
			}
		}
		
		return activeResources;
	}
	
	
	public HashSet<String> getActiveResourceUrls() {
		
		HashSet<String> activeResourceUrls = new HashSet<String>();
		
		for (ResourceInfo rs : getActiveResources()) {
			activeResourceUrls.add(rs.getDataURL());
		}
		
		return activeResourceUrls;
	}


	public OntologyManager getOntologyManager() {
		return ontologyManager;
	}
	
	/**
	 * Create backup<p>
	 * Write DB-Dir to backups/backup-id/DB-dir.tar.gz
	 * @param backupInfo
	 * @return err on success, otherwise empty
	 */
	public String makePhysicalBackup (Backup backup) {
		
		Utils.debug("makeBackup");
		File backupRootDirectory = new File(fidConfig.getString("Backup.directory"));
//		saveBackups2File(backupRootDirectory);
//		saveUsers2File(backupRootDirectory);
		
		backupInProgress=true;
		String error = stopDB();
		if (!error.isEmpty()) {
			resourceManager=createNewResourceManagerInstance();
			return error;
		}
		
		try {
			ExecutionBean.setProgressValue(25);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		File sourceDirectory=registryDbConfig.getDatabaseDirectory();
		
		// REG-DB
		File targetDirectory=new File(backupRootDirectory,backup.getName());
		ScriptUtils.tarDirectory(sourceDirectory, new File(targetDirectory, sourceDirectory.getName()));
		
		try {
			ExecutionBean.setProgressValue(50);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		// DATA-DB
		sourceDirectory=dataDbConfig.getDatabaseDirectory();
		ScriptUtils.tarDirectory(sourceDirectory, new File(targetDirectory, sourceDirectory.getName()));
		
		try {
			ExecutionBean.setProgressValue(75);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		
		// Backup ModelDef.json
		File modelFile = modelDefinition.getModelFile();
		try {
			Utils.debug("copying model file");
			Utils.debug(modelFile.getAbsolutePath()+" -> "+targetDirectory.getAbsolutePath());
			FileUtils.copyFile(modelFile, new File(targetDirectory, "ModelDef.json"));
		} catch (Exception e) {
			e.printStackTrace();
			Utils.debug("Error while copying ModelDef.json !");
			return e.getMessage();
		}
		
		error=restartDB();
		if(!error.isEmpty()) {
			resourceManager=createNewResourceManagerInstance();
			return error;
		}
		
		try {
			ExecutionBean.setProgressValue(100);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		// success : save backups and users to file
		////saveBackups2File(backupRootDirectory);
		saveUsers2File(backupRootDirectory);
				
		backupInProgress=false;
		return "";
	}
	
	
	private void saveUsers2File(File backupRootDirectory) {
		
		List<UserAccount> userList = new ArrayList<UserAccount>();
		for (String login : resourceManager.getAllUserLogins()) {
			userList.add(resourceManager.getUserAccount(login));
		}
		UserAccount.saveUsersToFile(userList, new File(backupRootDirectory, "users.json"));

	}
	
//	private void saveBackups2File(File backupRootDirectory) {
//
//		List<Backup> backupList = resourceManager.getBackups();
//		for (Backup backup : backupList) {
//			backup.createChecksums(new File(backupRootDirectory,backup.getName()));
//		}
//		Backup.saveBackupsToFile(backupList, new File(backupRootDirectory, "backups.json"));
//	}
	
	/**
	 * Restore backup
	 * @param backupInfo
	 * return err on success, otherwise empty
	 */
	public String restoreBackup (Backup backup) {
		
		File backupRootDirectory = new File(fidConfig.getString("Backup.directory"));
		File archivDirectory = new File(backupRootDirectory, backup.getName());
		
		// validate backup integrity via checksums
		String errorMsg = Backup.validateBackup(backup, archivDirectory);
		if(!errorMsg.isEmpty()) {
			return errorMsg;
		}

		saveUsers2File(backupRootDirectory); // save current users !
		// backups are up2date
		
		Utils.debug("restoreBackup : "+backup.getName());
		backupInProgress=true;
		
		// restore ModelDef.json
		File modelFile = modelDefinition.getModelFile();
		try {
			Utils.debug("copying model file");
			File source = new File(archivDirectory, "ModelDef.json");
			Utils.debug(source.getAbsolutePath()+" -> "+modelFile.getAbsolutePath());
			FileUtils.copyFile(source, modelFile);
		} catch (IOException e) {
			e.printStackTrace();
			Utils.debug("Error copying model file !");
			return e.getMessage();
		}
		
		String error = stopDB();
		if (!error.isEmpty()) {
			resourceManager=createNewResourceManagerInstance();
			return error;
		}
		
		
		// REG-DB
		// Delete actual DB folder first
		File dbDirectory;
		dbDirectory = registryDbConfig.getDatabaseDirectory();
		try {
			FileUtils.deleteDirectory(dbDirectory);
		} catch (IOException e) {
			e.printStackTrace();
			return e.getMessage();
		}

		// untar DB archive file
		ScriptUtils.untarArchive(new File(archivDirectory, dbDirectory.getName()+".tar.gz"), dbDirectory.getParentFile());
		
		// DATA-DB
		// Delete actual DB folder first
		dbDirectory=dataDbConfig.getDatabaseDirectory();
		try {
			FileUtils.deleteDirectory(dbDirectory);
		} catch (IOException e) {
			e.printStackTrace();
			return e.getMessage();
		}
		
		// untar DB archive file
		ScriptUtils.untarArchive(new File(archivDirectory, dbDirectory.getName()+".tar.gz"), dbDirectory.getParentFile());
		
		error=restartDB();
		if (!error.isEmpty()) {
			resourceManager=createNewResourceManagerInstance();
			return error;
		}
		
		// restore backups and users from json file
		List<Backup> backups = Backup.readBackups(new File(backupRootDirectory, "backups.json"));
//		for (Backup b : backups) {
//			
//			// TODO synchronize db entries in reg-DB with list of backups in backups.json
//			// delete backup from db if backup does not exist in backups.json 
//			
//			// resourceManager.addBackup(b); // does nothing and returns null if backup already exists
//		}
		List<UserAccount> users = UserAccount.readUsers(new File(backupRootDirectory, "users.json"));
		for (UserAccount user : users) {
			if (!resourceManager.userExists(user.getUserID())) {
				Utils.debug("adding user "+user.getUserID());
				resourceManager.addUser(user);
			} else {
				Utils.debug("updating user "+user.getUserID());
				resourceManager.updateUser(user);
			}
		}
		
		backupInProgress=false;
		
		return "";
	}
	
	
	public String deletePhysicalBackup(Backup backup) {
		
		Utils.debug("deletePhysicalBackup : "+backup.getName());
		
		File backupRootDirectory = new File(fidConfig.getString("Backup.directory"));
		File backupDirectory=new File(backupRootDirectory,backup.getName());

		try {
			FileUtils.deleteDirectory(backupDirectory);
		} catch (IOException e) {
			e.printStackTrace();
			return e.getMessage();
		}

		return "";
	}
	

	public static boolean isBackupInProgress() {
		return backupInProgress;
	}

	public static void setBackupInProgress(boolean backupInProgress) {
		Executer.backupInProgress = backupInProgress;
	}

	
	public String restartDB() {
		
		startGremlinServer();
		// reconnect clients & refresh embedded DB objects
		
		// reconnect clients to restarted gremlin server
		resourceManager = createNewResourceManagerInstance();
		cluster = resourceManager.getCluster();
		
		// create new embedded DB connection
		writer = new GWriterN (dataDbConfig.getDatabaseDirectory(), fidConfig);
		
		// wait (solves connection timeout problem)
		try {
			Thread.sleep(fidConfig.getLong("Databases.restartTimeoutInMilliseconds"));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	
		return "";
	}
	
	
	public String stopDB() {
		
		// Shutdown REG-DB & DATA-DB
		closeDBConnections();
		stopGremlinServer();
		
		// wait 
		/*try {
			Thread.sleep(30000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return e.getMessage();
		}*/
		
		return "";
	}
	
	
	
	/**
	 * Start gremlin-server
	 */
	public void startGremlinServer() {
		ScriptUtils.gremlinServer("start", fidConfig);
	}
	
	
	/**
	 * Stop gremlin-server
	 */
	public void stopGremlinServer() {
		ScriptUtils.gremlinServer("stop", fidConfig);
	}
	
	public static void main(String[] args) {
		
		fidConfig = Run.loadFIDConfig();
		
		dataDbConfig = new DatabaseConfiguration(DBType.Neo4J, null ,null , null, fidConfig);
		dataDbConfig.setDatabaseDirectory(new File("/media/EXTRA/local/DB/data-public"));
		registryDbConfig = new DatabaseConfiguration(DBType.GremlinServer, null , null , null , fidConfig);
		registryDbConfig.setDatabaseDirectory(new File("/media/EXTRA/local/DB/registry-public"));

		Backup backup = new Backup("test-backup-1");
		//makeBackup(backup);
		
		dataDbConfig.setDatabaseDirectory(new File("/media/EXTRA/test-backup/restore/data-public"));
		registryDbConfig.setDatabaseDirectory(new File("/media/EXTRA/test-backup/restore/registry-public"));
		//restoreBackup(backup);
		
		
	}
	
}
