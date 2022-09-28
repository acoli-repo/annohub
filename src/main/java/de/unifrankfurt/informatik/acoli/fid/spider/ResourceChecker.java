package de.unifrankfurt.informatik.acoli.fid.spider;


import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.tinkerpop.gremlin.driver.Cluster;

import de.unifrankfurt.informatik.acoli.fid.exec.Executer;
import de.unifrankfurt.informatik.acoli.fid.exec.Run;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.RMServer;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceState;
import de.unifrankfurt.informatik.acoli.fid.types.UpdatePolicy;
import de.unifrankfurt.informatik.acoli.fid.util.IndexUtils;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;
import edu.emory.mathcs.backport.java.util.concurrent.Executor;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutorCompletionService;

/**
 * @author frank
 *
 */
public class ResourceChecker {
	
	DownloadManager downloadManager;
	private ExecutorService exs;
	private Future<?> threadRef;

	
	public ResourceChecker(DownloadManager downloadManager) {
		this.downloadManager = downloadManager;
		exs = Executors.newFixedThreadPool(1);
	}

	
	/**
	 * Check if data link of resource is broken
	 * @param resources Resource list
	 * @param httpTimeout HTTP timeout in seconds
	 */
	public boolean checkBrokenLinksThreaded (Collection <ResourceInfo> resources, int httpTimeout) {
				
		if (threadRef == null || threadRef.isDone()) {
			if (threadRef != null) {
				System.out.println(threadRef.isDone());
			}
			threadRef = exs.submit(new ResourceCheckerW(downloadManager, resources, httpTimeout));
			Utils.debug("started checkBrokenLinksThreaded sucessfully !");
			return true;
		} else {
			Utils.debug("checkBrokenLinksThreaded already in progress !");
			return false;
		}
	}
	
	/**
	 * Check if URL is broken (unthreaded)
	 * @param url
	 * @param httpTimeout
	 * @return
	 */
	public Boolean urlIsOnline(String url, int httpTimeout) {
		
		Utils.debug("urlIsOnline");


		boolean isOnline = false;
		
		// save old timeout setting
		int oldTimeout = downloadManager.getHttpTimeout();
		// set new httpTimeout
		downloadManager.setHttpTimeout(httpTimeout);
		
		ResourceInfo resourceInfo = new ResourceInfo(url,"");
		if (resourceInfo.getDataURL() == null) {
			return false;
		}
			
		Utils.debug("Checking broken link : "+resourceInfo.getDataURL());
		downloadManager.getResource(99, resourceInfo,false, true);
		
		System.out.println(resourceInfo.getDataURL());
		System.out.println("http-response :"+resourceInfo.getHttpResponseCode());
		System.out.println("error message :"+resourceInfo.getFileInfo().getErrorMsg());
		System.out.println("error code:"+resourceInfo.getFileInfo().getErrorCode());

		
		// Check HTTP response code
		if (resourceInfo.getHttpResponseCode() != HttpStatus.SC_OK
				|| !resourceInfo.getFileInfo().getErrorMsg().isEmpty()) {
			// Update resource state
			isOnline = false;
			Utils.debug(resourceInfo.getDataURL()+" is broken !");
		} else {
			Utils.debug(resourceInfo.getDataURL()+"is online !");
			isOnline = true;
		}

		// restore previous timeout setting
		downloadManager.setHttpTimeout(oldTimeout);
		return isOnline;
	}
	
	/**
	 * Check resources that have a broken data link
	 * @param resources Resource list
	 * @param httpTimeout HTTP timeout in seconds
	 * @deprecated
	 */
	public void checkBrokenLinks(Collection <ResourceInfo> resources, int httpTimeout) {
		
		// save old timeout setting
		int oldTimeout = downloadManager.getHttpTimeout();
		// set new httpTimeout
		downloadManager.setHttpTimeout(httpTimeout);
		
		for (ResourceInfo resourceInfo : resources) {
			
			Utils.debug("Checking broken link : "+resourceInfo.getDataURL());
			downloadManager.getResource(99, resourceInfo,false, true);
			
			System.out.println(resourceInfo.getDataURL());
			System.out.println("http-response :"+resourceInfo.getHttpResponseCode());
			System.out.println("error message :"+resourceInfo.getFileInfo().getErrorMsg());
			System.out.println("error code:"+resourceInfo.getFileInfo().getErrorCode());

			
			// Check HTTP response code
			if (resourceInfo.getHttpResponseCode() != HttpStatus.SC_OK
					|| !resourceInfo.getFileInfo().getErrorMsg().isEmpty()) {
				// Update resource state
				resourceInfo.setResourceState(ResourceState.ResourceUrlIsBroken);
				Utils.debug(resourceInfo.getDataURL()+" is broken !");
			} else {
				Utils.debug(resourceInfo.getDataURL()+"is online !");
			}
		}
		
		// restore previous timeout setting
		downloadManager.setHttpTimeout(oldTimeout);
		
	}
	
	public static void main(String[] args) {
		String configurationFile ="/home/debian7/Arbeitsfläche/FIDConfigPublic.xml";
		XMLConfiguration fidConfig = Run.loadFIDConfig(configurationFile);
		IndexUtils.checkConfigAndSetDefaultValues(fidConfig);
	
		Cluster cluster = Cluster.open(Executer.makeBasicGremlinClusterConfig(fidConfig));

		ResourceManager resourceManager=new RMServer(cluster, UpdatePolicy.UPDATE_ALL);
		DownloadManager dm = new DownloadManager(
    			resourceManager, // global instance (Neo4J) or separate instance from above (GremlinServer)
    			new File (fidConfig.getString("RunParameter.downloadFolder")), UpdatePolicy.UPDATE_ALL,
    			fidConfig, 60);
    	
		ResourceChecker resourceChecker = new ResourceChecker(dm);
		ArrayList<ResourceInfo> resources = new ArrayList<ResourceInfo>();
		resources.add(new ResourceInfo("http://yovisto.com/labs/dumps/latest.ttl.tar.gz","",""));
		/*resources.add(new ResourceInfo("http://deepblue.rkbexplorer.com/models/dump.tgz","",""));
		resources.add(new ResourceInfo("http://usgs-stko.geog.ucsb.edu/resource/GNIS-LD.zip","",""));
		resources.add(new ResourceInfo("https://datahub.ckan.io/dataset/8e1b3a4f-9f5f-4f8c-8ad7-9599e0bc85c0/resource/08f808b5-e5da-444e-8702-ee2cac1bf059/download/udoldchurchslavonic.rdf","",""));*/
		resources.add(new ResourceInfo("http://brown.nlp2rdf.org/lod/d01.ttl","",""));
		//resources.add(new ResourceInfo("http://yovisto.com/labs/dumps/latest.ttl.tar.gz","",""));

		resourceChecker.checkBrokenLinksThreaded(resources , 10);
		
	}


	public Future<?> getThreadRef() {
		return threadRef;
	}

	
}
