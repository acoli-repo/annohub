package de.unifrankfurt.informatik.acoli.fid.spider;

import java.util.Collection;

import org.apache.commons.httpclient.HttpStatus;
import org.glassfish.jersey.server.model.Resource;

import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceState;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;

/**
 * @author frank
 *
 */
public class ResourceCheckerW implements Runnable {

	private DownloadManager downloadManager;
	private int httpTimeout;
	private Collection<ResourceInfo> resources;
	

	public ResourceCheckerW(DownloadManager downloadManager, Collection <ResourceInfo> resources,
			int httpTimeout) {
		this.downloadManager = downloadManager;
		this.resources = resources;
		this.httpTimeout = httpTimeout;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		
		// save old timeout setting
		int oldTimeout = downloadManager.getHttpTimeout();
		// set new httpTimeout
		downloadManager.setHttpTimeout(httpTimeout);
		
		int online = 0;
		int broken = 0;
		int onlineApproved=0;
		int brokenApproved=0;
		int approved=0;
		int counter=1;
		
		for (ResourceInfo resourceInfo : resources) {
			
			/*try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			if (resourceInfo.isApproved()) approved++;
			
			Utils.debug("Checking data link ("+counter+"/"+resources.size()+") : "+resourceInfo.getDataURL());
			counter++;
			downloadManager.getResource(99, resourceInfo,false, true);
			
			// Check HTTP response code
			if (resourceInfo.getHttpResponseCode() != HttpStatus.SC_OK
				|| !resourceInfo.getFileInfo().getErrorMsg().isEmpty()
				) {
				// Update resource state
				resourceInfo.setResourceState(ResourceState.ResourceUrlIsBroken);
				Utils.debug(resourceInfo.getDataURL()+" is broken !");
				broken++;
				if (resourceInfo.isApproved()) brokenApproved++;
			} else {
				Utils.debug("O.K.");
				online++;
				if (resourceInfo.isApproved()) onlineApproved++;
			}
		}
		
		// restore previous timeout setting
		downloadManager.setHttpTimeout(oldTimeout);
		
		Utils.debug("finished");
		Utils.debug("online :"+online+"/"+resources.size());
		Utils.debug("broken :"+broken+"/"+resources.size());
		Utils.debug("online (approved) :"+onlineApproved+"/"+approved);
		Utils.debug("broken (approved) :"+brokenApproved+"/"+approved);
	}

}
