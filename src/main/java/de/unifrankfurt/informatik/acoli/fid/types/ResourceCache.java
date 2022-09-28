package de.unifrankfurt.informatik.acoli.fid.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.unifrankfurt.informatik.acoli.fid.exec.Executer;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;
import de.unifrankfurt.informatik.acoli.fid.webclient.ExecutionBean;

/**
 * @author frank
 *
 */
public class ResourceCache {

	private ArrayList<ResourceInfo> resourceFiles=new ArrayList<ResourceInfo>();
	private HashMap<String, ResourceInfo> resourceMap=new HashMap<String, ResourceInfo>();
	private HashMap<String, ArrayList<ResourceInfo>> resourceFileMap=new HashMap<String, ArrayList<ResourceInfo>>();
	private HashSet<String> allResources = new HashSet<String>();
	public HashSet<String> recourcesInProgress = new HashSet<String>(); // contains resources, previously marked as inProgress 
	private Executer executer = null;
	private HashMap<String,HashSet<String>> annotationResourceMap = new HashMap<String,HashSet<String>>();
	private HashMap<String,HashSet<String>> annotationClassResourceMap = new HashMap<String,HashSet<String>>();
	private HashMap<String,HashSet<String>> oliaResourceMap = new HashMap<String,HashSet<String>>();
	private HashMap<String, ResourceInfo> errorResourceMap=new HashMap<String, ResourceInfo>();
	private HashMap<String, ResourceInfo> queuedResources = new HashMap<String, ResourceInfo>();
	private int nextResourceQueueID=0;

	
	public ResourceCache (ArrayList<ResourceInfo> resourceFiles) {
		this.resourceFiles = resourceFiles;
	}
	
	
	public ResourceCache() {}
	
	
	public ArrayList<ResourceInfo> getResourceFiles() {
		return resourceFiles;
	}

	public void setResourceFiles(ArrayList<ResourceInfo> resourceFiles) {
		this.resourceFiles = resourceFiles;
	}

	public HashMap<String, ArrayList<ResourceInfo>> getResourceFileMap() {
		return resourceFileMap;
	}

	public void setResourceFileMap(HashMap<String, ArrayList<ResourceInfo>> resourceFileMap) {
		this.resourceFileMap = resourceFileMap;
	}

	public HashMap<String, ResourceInfo> getResourceMap() {
		return resourceMap;
	}

	public void setResourceMap(HashMap<String, ResourceInfo> resourceMap) {
		this.resourceMap = resourceMap;
	}
	
	public void deleteResource(String resourceIdentifier) {
		
		resourceMap.remove(resourceIdentifier);
		resourceFileMap.remove(resourceIdentifier);
		allResources.remove(resourceIdentifier);
		
		for (String url : annotationResourceMap.keySet()) {
			if (annotationResourceMap.get(url).contains(resourceIdentifier)) {
				annotationResourceMap.get(url).remove(resourceIdentifier);
			}
		}
		for (String url : annotationClassResourceMap.keySet()) {
			if (annotationClassResourceMap.get(url).contains(resourceIdentifier)) {
				annotationClassResourceMap.get(url).remove(resourceIdentifier);
			}
		}
	}
	
	public void addResource(ResourceInfo resourceInfo, ArrayList<ResourceInfo> resourceFiles) {
		
		resourceMap.put(resourceInfo.getDataURL(), resourceInfo);
		resourceFileMap.put(resourceInfo.getDataURL(), resourceFiles);
	}
	
	
	public void updateResourceUrlInMaps(String oldResourceIdentifier, String newResourceIdentifier) {
		
		resourceMap.put(newResourceIdentifier,resourceMap.get(oldResourceIdentifier));
		resourceMap.remove(oldResourceIdentifier);
		resourceFileMap.put(newResourceIdentifier,resourceFileMap.get(oldResourceIdentifier));
		for (ResourceInfo resourceFile : resourceFileMap.get(newResourceIdentifier)) {
			resourceFile.setDataURL(newResourceIdentifier);
		}
		resourceFileMap.remove(oldResourceIdentifier);
		allResources.remove(oldResourceIdentifier);
		allResources.add(newResourceIdentifier);
		
		for (String url : annotationResourceMap.keySet()) {
			if (annotationResourceMap.get(url).contains(oldResourceIdentifier)) {
				annotationResourceMap.get(url).remove(oldResourceIdentifier);
				annotationResourceMap.get(url).add(newResourceIdentifier);
			}
		}
		for (String url : annotationClassResourceMap.keySet()) {
			if (annotationClassResourceMap.get(url).contains(oldResourceIdentifier)) {
				annotationClassResourceMap.get(url).remove(oldResourceIdentifier);
				annotationClassResourceMap.get(url).add(newResourceIdentifier);
			}
		}
	}
	
	
	/**
	 * 
	 * @param resources
	 * @return
	 */
	public ArrayList<ResourceInfo> getCachedResourcesByUrl(Collection<String> resources) {
		
		// Check if any resources in progress have been finished
		// checkInProgressCache(); called now in ExecutionBean (every time a finished file arrives)
		
		ArrayList<ResourceInfo> result = new ArrayList<ResourceInfo>();
		for (String url : resources) {
			
			if (resourceMap.containsKey(url)) {
				result.add(resourceMap.get(url));
			} else {
				//System.out.println("Cache miss : "+url);
				if (allResources.contains(url)) {

					// Skip resources with no result (none), errors (error) etc.
					continue;
					
				} else {// newly uploaded (not finished)
					
					List<ResourceInfo> rs = executer.getResourceManager().getDoneResourceRI(url);
					if (!rs.isEmpty()) {
						ResourceInfo x = executer.getResourceManager().getDoneResourceRI(url).get(0);
						System.out.println("hello, adding : "+x.getDataURL());
						addResource(x, new ArrayList<ResourceInfo>());
						result.add(resourceMap.get(url));
					}
					
					// recourcesInProgress.add(url); now set in worker
					
					// not required here, should be already done from call of
					// updateCacheWithResourceFromDB in onMessage function
					// get newly added resource from DB
					//updateCacheWithResourceFromDB(url);
					// actually called only once, in case the resource is NOT in the resourceMap
					// replacement for updateCacheWithResourceFromDB(url)
					// 3. add new resource to cache
					// add new resource to result (add dummy URL which is shown in GUI)
					//result.add(resourceMap.get(url));
				}
			}
		}
		return result;
	}
	
	
	public ArrayList<ResourceInfo> getCachedResourcesByUrl(List<Vertex> resources) {
		
		ArrayList<String> resourceUrls = new ArrayList<String>();
		
		for (Vertex v : resources) {
			resourceUrls.add((String) v.value(ResourceManager.ResourceUrl));
		}
		
		return getCachedResourcesByUrl(resourceUrls);
	}
	
	
	public ArrayList<ResourceInfo> getCachedErrorResourcesByUrl(Collection<String> resources) {
	
		ArrayList<ResourceInfo> errorResources = new ArrayList<ResourceInfo>();
		
		for (String resource : resources) {
			if (errorResourceMap.containsKey(resource)) {
				errorResources.add(errorResourceMap.get(resource));
			}
		}
		return errorResources;
	}
	
	
	public ArrayList<ResourceInfo> getCachedErrorResourcesByUserID(String userID) {
		
		ArrayList<ResourceInfo> errorResources = new ArrayList<ResourceInfo>();
		for (ResourceInfo resource : errorResourceMap.values()) {
			if (resource.getUserID().equals(userID)) {
				errorResources.add(resource);
			}
		}
		return errorResources;
	}
	
	
	/**
	 * Check resources that previously have been marked as inProgress. Update resource results in case
	 * a resource has finished processing.
	 */
	public void checkInProgressCache (ResourceInfo resourceInfo) {
		
		Utils.debug("checkInProgressCache");
		Utils.debug("recourcesInProgress : "+recourcesInProgress.size());
		
		String url = resourceInfo.getDataURL();
		boolean error = !resourceInfo.getFileInfo().getErrorMsg().isEmpty();
		
		// all files in the resource have finished processing
		if (!executer.getActiveResourceUrls().contains(url)) {
			
			if (error) {
				
				// add resource file(s) to error map
				errorResourceMap.put(url, resourceInfo); // no files available
				// remove resource from all other maps 
				deleteResource(url);
				// add it to the set of all resources thereby indicating it as resource without results
				allResources.add(url);
				
			} else {
				
				// get results from DB
				ResourceCache rc = updateCacheWithResourceFromDB(url);
				boolean good = false;
				// check if results found for resource
				for (ResourceInfo rf : rc.getResourceFileMap().get(url)) {
					if (rf.getFileInfo().getParseResult() == ParseResult.SUCCESS) {
						good=true; break;
					}
				}
			
				// add resource file(s) to error map
				if (!good) {
					for (ResourceInfo rf : rc.getResourceFileMap().get(url)) {
						// writes only one file (of all files in the resource)
						errorResourceMap.put(url, rf); 
					}
					// remove resource from all other maps 
					deleteResource(url);
					// add it to the set of all resources thereby indicating it as resource without results
					allResources.add(url);
				}
			}
			
			// remove resource from recourcesInProgress
			recourcesInProgress.remove(url);
		}

		
//		Iterator<String> iterator = recourcesInProgress.iterator();
//		
//		while (iterator.hasNext()) {
//			
//			String url = iterator.next();
//			if (!executer.getActiveResourceUrls().contains(url)) {
//				
//				// if resource has finished processing then get final results
//				ResourceCache rc = updateCacheWithResourceFromDB(url);
//				
//				// check if results found for resource
//				boolean good = false;
//				for (ResourceInfo rf : rc.getResourceFileMap().get(url)) {
//					if (rf.getFileInfo().getParseResult() == ParseResult.SUCCESS) {
//						good=true;
//					}
//				}
//				
//				// add resource file(s) to error map
//				if (!good) {
//					for (ResourceInfo rf : rc.getResourceFileMap().get(url)) {
//						this.errorResourceMap.put(url, rf);
//					}
//					// remove resource from all other maps 
//					this.deleteResource(url);
//					// add it to the set of all resources thereby indicating it as resource without results
//					this.allResources.add(url);
//				}
//				
//				// remove resource from recourcesInProgress
//				iterator.remove();
//			}
//		}
	}
	
	/**
	 * Fetch results of newly added resource from DB and put it in cache, assuming that all files of
	 * the resource have been processed.
	 * @param url
	 * @return
	 */
	public ResourceCache updateCacheWithResourceFromDB(String url) {
		
		Utils.debug("updateCacheWithResourceFromDB "+url);
		
		// 1. load resource from DB
		// should be called only once (otherwise getFileValues will be called again and again)
		ResourceCache newUploaded = executer.getResourceManager().getDoneResourcesRIMap(url);
		
		// 2. add files of new resource to resourceFileMap
		for (ResourceInfo fileResource : newUploaded.getResourceFileMap().get(url)) {
				// getFileValues
				executer.getWriter().getQueries().getFileResults(fileResource, executer.getResourceManager());
		}
		
		// query MD in Linghub
		if (ExecutionBean.publicFidConfig.getBoolean("Linghub.enabled")) {
			executer.getUrlBroker().sparqlLinghubAttributes(true, new ArrayList<ResourceInfo>(){{add(newUploaded.getResourceMap().get(url));}});	
		}
		
		
//		// 2. add files of new resource to resourceFileMap
//		for (ResourceInfo fileResource : newUploaded.getResourceFileMap().get(url)) {
//			if (!ExecutionBean.getResourceCache().getResourceFileMap().containsKey(url)) {
//				// getFileValues
//				System.out.println("1");
//				executer.getWriter().getQueries().getFileResults(fileResource, executer.getResourceManager());
//			} else {
//				boolean cached = false;
//				for (ResourceInfo cachedFileResource : ExecutionBean.getResourceCache().getResourceFileMap().get(url)) {
//					if (fileResource.getFileInfo().getRelFilePath().equals(cachedFileResource.getFileInfo().getRelFilePath())) {
//						// do not getFileValues for files that already have been cached
//						cached = true; break;
//					}
//				}
//				if (!cached) {
//					// getFileValues
//					System.out.println("2");
//					executer.getWriter().getQueries().getFileResults(fileResource, executer.getResourceManager());
//				}
//			}
//		}
		
		// 3. add new resource to cache
		addResource(
				newUploaded.getResourceMap().get(url),
				newUploaded.getResourceFileMap().get(url));
		
		return newUploaded;
	}


	public HashSet<String> getAllResources() {
		return allResources;
	}


	public void setAllResources(HashSet<String> allResources) {
		this.allResources = allResources;
	}


	public Executer getExecuter() {
		return executer;
	}


	public void setExecuter(Executer executer) {
		this.executer = executer;
	}


	public HashSet<String> getRecourcesInProgress() {
		return recourcesInProgress;
	}


	public void setRecourcesInProgress(HashSet<String> recourcesInProgress) {
		this.recourcesInProgress = recourcesInProgress;
	}


	public HashMap<String,HashSet<String>> getAnnotationResourceMap() {
		return annotationResourceMap;
	}
	
	public HashMap<String,HashSet<String>> getExtendedAnnotationResourceMap() {
		
		HashMap<String, HashSet<String>> result = new HashMap<String, HashSet<String>>();
		for (String key : annotationResourceMap.keySet()) {
			if (!key.startsWith("ZERO")) {
				result.put(key+" "+annotationResourceMap.get(key).size(), annotationResourceMap.get(key));
			}
		}
		return result;		
	}


	public void setAnnotationResourceMap(HashMap<String,HashSet<String>> annotation2Resources) {
		this.annotationResourceMap = annotation2Resources;
	}


	public HashMap<String,HashSet<String>> getAnnotationClassResourceMap() {
		return annotationClassResourceMap;
	}


	public void setAnnotationClassResourceMap(
			HashMap<String,HashSet<String>> annotationClassResourceMap) {
		this.annotationClassResourceMap = annotationClassResourceMap;
	}
	
	public HashMap<String,HashSet<String>> getExtendedAnnotationClassResourceMap() {
		
		HashMap<String, HashSet<String>> result = new HashMap<String, HashSet<String>>();
		for (String key : annotationClassResourceMap.keySet()) {
			result.put(key+" "+annotationClassResourceMap.get(key).size(), annotationClassResourceMap.get(key));
		}
		return result;		
	}


	public HashMap<String,HashSet<String>> getOliaResourceMap() {
		return oliaResourceMap;
	}
	
	public HashMap<String,HashSet<String>> getExtendedOliaResourceMap() {
		HashMap<String, HashSet<String>> result = new HashMap<String, HashSet<String>>();
		for (String key : oliaResourceMap.keySet()) {
			result.put(key+" "+oliaResourceMap.get(key).size(), oliaResourceMap.get(key));
		}
		return result;
	}


	public void setOliaResourceMap(HashMap<String,HashSet<String>> oliaResourceMap) {
		this.oliaResourceMap = oliaResourceMap;
	}


	/**
	 * @param annotationOrAnnotationClass
	 */
	public void addAnnotationOrClass2ResourceMap(String resourceIdentifier,
			String annotationOrAnnotationClass) {
		
		if (annotationOrAnnotationClass.startsWith("http")) {
			
			if (!getAnnotationClassResourceMap().keySet().contains(annotationOrAnnotationClass)) {
				HashSet<String> urlSet = new HashSet<String>();
				urlSet.add(resourceIdentifier);
				getAnnotationClassResourceMap().put(annotationOrAnnotationClass, urlSet);
			} else {
				HashSet<String> urlSet = getAnnotationClassResourceMap().get(annotationOrAnnotationClass);
				urlSet.add(resourceIdentifier);
				getAnnotationClassResourceMap().put(annotationOrAnnotationClass, urlSet);
			}
			
		} else {
			
			if (!getAnnotationResourceMap().keySet().contains(annotationOrAnnotationClass)) {
				HashSet<String> urlSet = new HashSet<String>();
				urlSet.add(resourceIdentifier);
				getAnnotationResourceMap().put(annotationOrAnnotationClass, urlSet);
			} else {
				HashSet<String> urlSet = getAnnotationResourceMap().get(annotationOrAnnotationClass);
				urlSet.add(resourceIdentifier);
				getAnnotationResourceMap().put(annotationOrAnnotationClass, urlSet);
			}
		}
	}


	/**
	 * @param dataURL
	 * @param matchedTagClass
	 */
	public void addMinimalOliaClassForMatchedTag2OliaResourceMap(String resourceIdentifier, FileResult f) {
		
		if (!f.isFeature()) {
			String matchedTagClass = f.getMatchingTagOrClass();
			
			if(!ExecutionBean.getHit2MinimalOliaPath().containsKey(matchedTagClass)) {
				Utils.debug("addMinimalOliaClassForMatchedTag2OliaResourceMap : unmatched : "+matchedTagClass);
				return;
			}
			f.setMinimalOliaPath(ExecutionBean.getHit2MinimalOliaPath().get(matchedTagClass));
				
			if (!getOliaResourceMap().keySet().contains(f.getBestMatchingOliaClass())) {
				HashSet<String> urlSet = new HashSet<String>();
				urlSet.add(resourceIdentifier);
				getOliaResourceMap().put(f.getBestMatchingOliaClass(), urlSet);
			} else {
				HashSet<String> urlSet = getOliaResourceMap().get(f.getBestMatchingOliaClass());
				urlSet.add(resourceIdentifier);
				getOliaResourceMap().put(f.getBestMatchingOliaClass(), urlSet);
			}
		} else {
			
			Utils.debug("addMinimalOliaClassForMatchedTag2OliaResourceMap : unmatched : "+f.getMatchingTagOrClass());

			// for feature : directly use matched olia class in file result
			/*if (!getOliaResourceMap().keySet().contains(f.getMatchingTagOrClass())) {
					HashSet<String> urlSet = new HashSet<String>();
					urlSet.add(resourceIdentifier);
					getOliaResourceMap().put(f.getMatchingTagOrClass(), urlSet);
			} else {
					HashSet<String> urlSet = getOliaResourceMap().get(f.getMatchingTagOrClass());
					urlSet.add(resourceIdentifier);
					getOliaResourceMap().put(f.getMatchingTagOrClass(), urlSet);
			}*/
		}
	}


	public HashMap<String, ResourceInfo> getErrorResourceMap() {
		return errorResourceMap;
	}


	public void setErrorResourceMap(HashMap<String, ResourceInfo> errorResourceMap) {
		this.errorResourceMap = errorResourceMap;
	}

	
	
	// alternative queue

	public synchronized HashMap<String, ResourceInfo> getQueuedResources() {
		return queuedResources;
	}
	
	
	public synchronized Boolean addResource2Queue(ResourceInfo resourceInfo) {
		
		String url = resourceInfo.getDataURL();
		if (queuedResources.keySet().contains(url)) return false;
		resourceInfo.setQueuePosition(getNextResourceQueueID());
		queuedResources.put(url, resourceInfo);
		
//		System.out.println("cache resource queue (add)");
//		for (String x : queuedResources.keySet()) {
//			System.out.println(x+" "+queuedResources.get(x).getQueuePosition());
//		}
		return true; // success
	}
	
	
	public synchronized Boolean removeResourceFromQueue(ResourceInfo resourceInfo) {
		queuedResources.remove(resourceInfo.getDataURL());
		Utils.debug("removeResourceFromQueue : "+resourceInfo.getDataURL());
//		for (String x : queuedResources.keySet()) {
//			ResourceInfo y = queuedResources.get(x);
//			y.setQueuePosition(y.getQueuePosition()-1);
//		}
//		
//		System.out.println("cache resource queue (remove)");
//		for (String x : queuedResources.keySet()) {
//			System.out.println(x+" "+queuedResources.get(x).getQueuePosition());
//		}
		
		return true; // success
	}
	
	
	public ArrayList<ResourceInfo> getQueuedResourceForUser(String userID) {
		Utils.debug("getQueuedResourceForUser");
		SortedMap<Integer, ResourceInfo> map = new TreeMap<Integer, ResourceInfo>();
		for (ResourceInfo ri : queuedResources.values()) {

			if (ri.getUserID().equals(userID)) {
				map.put(ri.getQueuePosition(), ri);
				Utils.debug("queued user resource : "+ri.getDataURL());
			}
		}
		Utils.debug("getQueuedResourceForUser finished with "+map.size()+ " entries");
		return new ArrayList<ResourceInfo>(map.values());
	}
	


	public synchronized int getNextResourceQueueID() {
		nextResourceQueueID+=1;
		Utils.debug("getNextResourceQueueID : "+nextResourceQueueID);
		return nextResourceQueueID;
	}

}
