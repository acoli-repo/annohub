package de.unifrankfurt.informatik.acoli.fid.webclient;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.jena.rdf.model.Model;

import de.unifrankfurt.informatik.acoli.fid.activemq.Consumer;
import de.unifrankfurt.informatik.acoli.fid.exec.Executer;
import de.unifrankfurt.informatik.acoli.fid.exec.Run;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.RMServer;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.UserManagement;
import de.unifrankfurt.informatik.acoli.fid.serializer.RDFSerializer;
import de.unifrankfurt.informatik.acoli.fid.spider.DownloadManager;
import de.unifrankfurt.informatik.acoli.fid.spider.ResourceChecker;
import de.unifrankfurt.informatik.acoli.fid.types.ExecutionMode;
import de.unifrankfurt.informatik.acoli.fid.types.ParseResult;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceCache;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceProcessState;
import de.unifrankfurt.informatik.acoli.fid.types.UpdatePolicy;
import de.unifrankfurt.informatik.acoli.fid.ub.PostgresManager;
import de.unifrankfurt.informatik.acoli.fid.util.JenaUtils;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;

/**
 * @author frank
 *
 */
//@Startup removed in order to check if "Quick fix" is called in ResourceInfoBean is fixed
@ManagedBean(name="execute",eager=true)
@ApplicationScoped
@Singleton
public class ExecutionBean implements MessageListener {
	
public static XMLConfiguration publicFidConfig;
private static boolean loaded;
private static int timesLoaded=0;
private static Executer publicExecuter;
private static Executer internalExecuter;
private static EditManager editManager;
private static ResourceCache resourceCache;
private int nextFreeGuestAccountId=0;
private static HashMap<String, ArrayList<String>> hit2MinimalOliaPath = new HashMap<String, ArrayList<String>>();
private static HashMap<String,String> minimalOliaClasses = new HashMap<String,String>();
private static ResourceChecker resourceChecker;
private static UserManagement userManagement;
private static UserLog userLog = new UserLog();

@EJB
ExecuterEjb executionEjb;
private static PostgresManager postgresManager=null;


@PostConstruct
public void init() {
	
	if (loaded) return;
	loaded = true;
	
	System.out.println("Init execution bean");
	System.out.println("times loaded : "+(++timesLoaded));
	
	publicFidConfig = Run.loadFIDConfig();
	//publicFidConfig = Run.loadFIDConfig("/home/debian7/Arbeitsfläche/FIDConfigPublic.xml");
	publicExecuter = new Executer (publicFidConfig);
	publicExecuter.run(ExecutionMode.SERVICE);
	//publicExecuter.run(ExecutionMode.PUBLICSERVICE);
	editManager = new EditManager();
	resourceChecker = new ResourceChecker(
		new DownloadManager(
				publicExecuter.createNewResourceManagerInstance(), // global instance (Neo4J) or separate instance from above (GremlinServer)
				new File (publicFidConfig.getString("RunParameter.downloadFolder")),
				null,
				publicFidConfig,
				10)
		);
	//resourceChecker = new ResourceChecker(publicExecuter.getOntologyManager().getDownloadManager());

	if (publicFidConfig.getBoolean("Databases.Postgres.usePostgres")) {
		// add clarin metadata		(if no manual or linghub metadata exists)
		postgresManager = new PostgresManager (publicFidConfig);
	}
	
	initApplication(false);
	
	userManagement = new UserManagement(publicExecuter.getResourceManager(), publicFidConfig);
	
	restoreQueue();
	
    Consumer consumer = new Consumer(Executer.MQ_OUT_1);
    try {
		consumer.getConsumer().setMessageListener(this);
	} catch (JMSException e) {
		e.printStackTrace();
	}
}



public static void initResourceCache() {
	
	Utils.debug("initResourceCache");
	
	ResourceManager resourceManager = publicExecuter.createNewResourceManagerInstance();
	//new RMServer(publicExecuter.getCluster(), UpdatePolicy.valueOf(publicFidConfig.getString("RunParameter.updatePolicy")));
	HashSet<ParseResult> withParseResults = new HashSet<ParseResult>();
	withParseResults.add(ParseResult.SUCCESS);
	
	if (publicFidConfig.getBoolean("RunParameter.loadUnsuccessfull")) {
		withParseResults.add(ParseResult.ERROR);
		withParseResults.add(ParseResult.NONE);
		withParseResults.add(ParseResult.UNKNOWN);
		System.out.println("loadUnsuccessfull resources");
	}
	setResourceCache(resourceManager.getDoneResourcesRIMap(withParseResults));
	
	// check broken links
	/*if (publicFidConfig.getBoolean("RunParameter.checkBrokenLinksAtServerStart")) {
		startBrokenLinksCheck(10);
	}*/
	
	int n = resourceCache.getResourceFileMap().keySet().size();
	int i = 1;
	for (String resourceIdentifier : resourceCache.getResourceFileMap().keySet()) {
		System.out.println(i+"/"+n+" Getting file results for resource : "+resourceIdentifier);
		publicExecuter.getWriter().getQueries().
			getFileResults(resourceCache.getResourceFileMap().get(resourceIdentifier), resourceManager);
		i++;
	}
	// Save urls of all done resources in cache
	resourceCache.setAllResources(new HashSet<String>(resourceManager.getDoneResourcesAsString()));
	
	// Set executer
	resourceCache.setExecuter(getPublicExecuter());
}


/**
 * Part of the init methode that is also invoked during restoring a backup or model update operations
 */
public static void initApplication(Boolean update) {
	
	initMinimalOliaClasses();
	
	if(publicFidConfig.getBoolean("RunParameter.cached")) {
		initResourceCache();
	}
	
	// check broken links
	if (update || publicFidConfig.getBoolean("RunParameter.checkBrokenLinksAtServerStart")) {
			startBrokenLinksCheck(15);
	}
	
	// add linghub metadata		(if no manual metadata exists)
	if (publicFidConfig.getBoolean("Linghub.enabled")) {
		publicExecuter.getUrlBroker().sparqlLinghubAttributes(true, resourceCache.getResourceMap().values());
	}
	
	if (publicFidConfig.getBoolean("Databases.Postgres.usePostgres")) {
		// add clarin metadata		(if no manual or linghub metadata exists)
		postgresManager.updateClarinMetadata(resourceCache.getResourceMap().values());
	}

	if (update || publicFidConfig.getBoolean("RunParameter.initRdfExporterAtServerStart")) {
	// init rdf export
	ArrayList<ResourceInfo> dummyList = new ArrayList<ResourceInfo>();
	RDFSerializer.serializeResourceInfo2RDFModelIntern(
			(ArrayList<ResourceInfo>) dummyList,
			publicExecuter.getWriter().getBllTools(),
			publicFidConfig,
			publicExecuter.getModelDefinition()
			);
	}
}


/**
 * Start checking broken links
 * @param httpTimeout HTTP timeout in seconds
 * @return True if successfully started; else false
 */
public static boolean startBrokenLinksCheck(int httpTimeout) {
	
		boolean brokenLinksCheckStarted = resourceChecker.checkBrokenLinksThreaded(resourceCache.getResourceMap().values(), httpTimeout);
		//resourceChecker.checkBrokenLinks(resourceCache.getResourceMap().values(), httpTimeout);
		return brokenLinksCheckStarted;
}



private static void initMinimalOliaClasses() {
	
	minimalOliaClasses.clear(); // for update
	
	/*minimalOliaClasses.put("http://purl.org/olia/olia.owl#Diminuitive", "http://purl.org/olia/mte/multext-east.owl#Diminuitive, in MTE v.4 originally modelled as an aspect of Degree, but this is a misplacement." 
			+"There are languages where Degree and Diminuitivity are independent. In Latvian, for example, the diminutive suffix may be attached to an adjective,"
			+"not only in the positive but in the comparative and superlative degrees (Ruke-Dravina 1953).");
	minimalOliaClasses.put("http://purl.org/olia/olia.owl#Apocope","deletion of the final element in a word (http://www.isocat.org/datcat/DC-2254)");
	minimalOliaClasses.put("http://purl.org/olia/olia.owl#Residual", "From a linguistic point of view, Residuals are a heterogeneous class and so, Residualmay overlap with every linguistically motivate annotation concept.Also between subconcepts, overlap may occur (e.g. \\LaTeX which is a symbol which can be read as an Acronym or acronyms which are related to Abbreviations, e.g. GNU \"Gnu is not Unix\")");
*/
	hit2MinimalOliaPath = publicExecuter.getWriter().getQueries().getMinimalOliaPathMapping();
	HashMap<String, String> oliaComments = readOliaComments();
	
	for (String tagClass : hit2MinimalOliaPath.keySet()) {
		
		// put minimal olia classes that match HIT tags as keys in map
		// olia class has last index in path
		String oliaClass = hit2MinimalOliaPath.get(tagClass).get(hit2MinimalOliaPath.get(tagClass).size()-1);
		String comment = "";
		if (oliaComments.containsKey(oliaClass)) {
			comment = oliaComments.get(oliaClass).replaceAll("\t", "");
			comment = comment.replaceAll("\n", "");
		}
		minimalOliaClasses.put(oliaClass, comment.trim());
		
	}	
}


private static HashMap<String, String> readOliaComments() {
	
	
	// olia.owl
	Model model = JenaUtils.readDatasetFromUrl(Executer.modelDefinition.getOliaCoreFileUrls().get("olia"));
	HashMap<String, String> result_1 = JenaUtils.queryModelViaSelector(
			model,
			null,
			"http://www.w3.org/2000/01/rdf-schema#comment",
			null);
	// system.owl
	model = JenaUtils.readDatasetFromUrl(Executer.modelDefinition.getOliaCoreFileUrls().get("system"));
	HashMap<String, String> result_2 = JenaUtils.queryModelViaSelector(
			model,
			null,
			"http://www.w3.org/2000/01/rdf-schema#comment",
			null);
	// olia-top.owl
	model = JenaUtils.readDatasetFromUrl(Executer.modelDefinition.getOliaCoreFileUrls().get("olia-top"));
	HashMap<String, String> result_3 = JenaUtils.queryModelViaSelector(
			model,
			null,
			"http://www.w3.org/2000/01/rdf-schema#comment",
			null);
	result_1.putAll(result_2);
	result_1.putAll(result_3);
	return result_1;
	
}


public static Executer getInternalExecuter() {
	return internalExecuter;
}

public static Executer getPublicExecuter() {
	return publicExecuter;
}

public EditManager getEditManager() {
	return editManager;
}

public static ResourceCache getResourceCache() {
	return resourceCache;
}

public static void setResourceCache(ResourceCache resourceCache) {
	ExecutionBean.resourceCache = resourceCache;
}


public int getNextFreeGuestAccountId() {
	nextFreeGuestAccountId++;
	return nextFreeGuestAccountId;
}


public static HashMap<String,String> getMinimalOliaClasses() {
	return minimalOliaClasses;
}


public static void setMinimalOliaClasses(HashMap<String,String> oliaClasses) {
	ExecutionBean.minimalOliaClasses = oliaClasses;
}



public static HashMap<String, ArrayList<String>> getHit2MinimalOliaPath() {
	return hit2MinimalOliaPath;
}



public void setHit2MinimalOliaClasses(HashMap<String, ArrayList<String>> tagClass2MinimalOliaClass) {
	hit2MinimalOliaPath = tagClass2MinimalOliaClass;
}



public static UserManagement getUserManagement() {
	return userManagement;
}



public static void setUserManagement(UserManagement userManagement) {
	ExecutionBean.userManagement = userManagement;
}



public static UserLog getUserLog() {
	return userLog;
}



public static void setUserLog(UserLog userLog) {
	ExecutionBean.userLog = userLog;
}



public static ResourceChecker getResourceChecker() {
	return resourceChecker;
}


public void restoreQueue() {
	
	File backupFile = new File(publicFidConfig.getString("RunParameter.QueueBackupFile"));
	if (backupFile.exists()) {
		List<ResourceInfo> queuedResources = Utils.readQueuedResourcesFromFile(backupFile);
		
		// Requeue resources
		for (ResourceInfo r : queuedResources) {
			executionEjb.addResource(r);
		}
	}
}



/* (non-Javadoc)
* @see javax.jms.MessageListener#onMessage(javax.jms.Message)
*/
@Override
public void onMessage(Message message) {
	
	ResourceInfo ri = Consumer.extractResourceInfo(message);
	
	Utils.debug("onMessage : "+ri.getDataURL()+" "+ri.getFileInfo().getRelFilePath());
	Utils.debug(ri.getResourceProcessState());
	Utils.debug("");
	
	if (ri.getResourceProcessState() == ResourceProcessState.FINISHED ||
		ri.getResourceProcessState() == ResourceProcessState.ARCHIVE_FINISHED) {
		ExecutionBean.getResourceCache().checkInProgressCache(ri);
	}
}



public PostgresManager getPostgresManager() {
	return postgresManager;
}


}
