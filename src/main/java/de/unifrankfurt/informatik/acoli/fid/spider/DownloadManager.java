package de.unifrankfurt.informatik.acoli.fid.spider;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.util.EntityUtils;

import de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceState;
import de.unifrankfurt.informatik.acoli.fid.types.UpdatePolicy;
import de.unifrankfurt.informatik.acoli.fid.util.IndexUtils;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;


public class DownloadManager {
	
	private HttpEntity fileEntity;
	private HttpResponse response;	
	private SSLContext ctx=null;
	
	private ResourceManager resourceManager;
	//private MetashareWebdriver wd;
	private File downloadedFile = null;
	private File localFile = null;
	
	int httpTimeout = 10;
	RequestConfig requestConfig;
	private String downloadPage;
	private UpdatePolicy updatePolicy;
	private File downloadFolder;
	private XMLConfiguration fidConfig;
	
	public DownloadManager (ResourceManager resourceManager, File downloadFolder, UpdatePolicy updatePolicy, XMLConfiguration fidConfig, int httpTimeout) {
		this.resourceManager = resourceManager;
		this.updatePolicy = updatePolicy;
		this.downloadFolder = downloadFolder;
		this.fidConfig = fidConfig;
		this.httpTimeout = httpTimeout;
		this.updateRequestConfig();
		
		try {
			ctx = SSLContext.getInstance("TLS");
			ctx.init(new KeyManager[0], new TrustManager[] {new DefaultTrustManager()}, new SecureRandom());
	        SSLContext.setDefault(ctx);
		} catch (Exception e) {
			e.printStackTrace();
		}        
	}
	
	
	private void updateRequestConfig() {
		requestConfig = RequestConfig.custom().
				  setConnectTimeout(httpTimeout * 1000).
				  setConnectionRequestTimeout(httpTimeout * 1000).
				  setSocketTimeout(httpTimeout * 1000).build();
	}

	/**
	 * Get resource thereby registering it in resource database
	 * @param workerID
	 * @param resourceInfo ResourceInfo
	 * @param emptyFolder 
	 * @param onlyCheckHeader
	 * @param File object which keeps the resource
	 * @return Resource vertex
	 */
	public void getResource(int workerID, ResourceInfo resourceInfo, boolean emptyFolder, boolean onlyCheckHeader) {

	File downloadDirectory = new File (downloadFolder,Integer.toString(workerID));
	System.out.println("DownloadFolder : "+downloadDirectory.getAbsolutePath());
	
	// Clean download folder before downloading next resource
	if (emptyFolder) {
	try {
		FileUtils.deleteDirectory(downloadDirectory);
		downloadDirectory.mkdirs();
	} catch (IOException e1) {
		downloadDirectory.mkdirs();
	}
	}
	
	
	// if available space on disk < limit => do not download the resource ! 
	/*System.out.println("Available disk space in download folder "+downloadDirectory.getAbsolutePath()+" : "+
			downloadDirectory.getUsableSpace());
	if (downloadDirectory.getUsableSpace()/1000.00 <= fidConfig.getLong("RunParameter.ExitProcessDiskSpaceLimit")) {
		System.out.println(IndexUtils.ERROR_OUT_OF_DISK_SPACE);
		resourceInfo.getFileInfo().setErrorMsg(IndexUtils.ERROR_OUT_OF_DISK_SPACE);
		return;
	}*/

	
	try {
		String url = resourceInfo.getDataURL();
		//System.out.println(url);
		String protocol = new URL(url).getProtocol();
				
		switch (protocol) {
			
			// Local file
			case "file" :
				
				// create response parameter
				localFile = new File (new URL(url).getPath());
				String contentLength = localFile.length()+"";
				SimpleDateFormat formatter = new SimpleDateFormat("E d. MMM HH:mm:ss z yyyy"); // Fr 21. Okt 02:09:29 CEST 2016
				Date now = new Date();
				String date = formatter.format(now);
				Date modified = new Date(localFile.lastModified());
				String lastModified = formatter.format(modified);
				
				// simulate http response with local file parameter
				response = new BasicHttpResponse(
						   new BasicStatusLine(new ProtocolVersion("HTTP",1,1),HttpStatus.SC_OK,""));
				response.addHeader("Content-Length", contentLength);
				response.addHeader("Date", date);
				response.addHeader("Last-Modified", lastModified);
				
				// stop here if only resource properties are checked (broken link?)
				if (onlyCheckHeader) {
					resourceInfo.setHttpResponseValues(response);
					return;
				}

				resourceInfo.getFileInfo().setResourceFile(localFile);
				
				// compute hash values for file
				resourceManager.updateResourceMD5Sha256(resourceInfo);
				
				// Register resource in the resource database (returns resource vertex or null)
				resourceManager.registerResource(resourceInfo, response);
			
				return;
				
			case "ftp" :
				System.out.println(IndexUtils.ERROR_FTP_SUPPORT+" - skipping resource !");
				resourceInfo.getFileInfo().setErrorMsg(IndexUtils.ERROR_FTP_SUPPORT+ " - skipping resource !");
				return;
				
			/*	
			case "https" :
				try {
					System.out.println("https : "+url);
					HttpURLConnection httpsCon = null;
					httpsCon = (HttpsURLConnection)new URL(url).openConnection();
					System.out.println(httpsCon.getResponseCode());
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.exit(0);
			*/
				
			default :
				// Make HTTP request (works also for https)
				CloseableHttpClient client = HttpClientBuilder.create().setSSLContext(ctx).setDefaultRequestConfig(requestConfig).build();
				HttpGet request = new HttpGet(url);
				request.addHeader("Connection", "close");
				response = client.execute(request);
				//request.releaseConnection();
				System.out.println(response.getStatusLine().getStatusCode());

				// stop here if only resource properties are checked (broken link?)
				if (onlyCheckHeader) {
					resourceInfo.setHttpResponseValues(response);
					return;
				}
				
				// check status
				int responseCode = -1;
				try {responseCode = response.getStatusLine().getStatusCode();} catch (Exception e){e.printStackTrace();};
				if (!(responseCode == HttpStatus.SC_OK)) {
					System.out.println(responseCode + " ... Skipping !");
					resourceInfo.getFileInfo().setErrorMsg(responseCode + " ... Skipping !");
					return;
					}
				
				// Register the resource in the resource database if download was successful
				resourceManager.registerResource(resourceInfo, response);
		}

		
		
		/*********************
		 * Download Resource *
		 *********************/
		
		// In case the resource is null the resource has not to been updated
		if (resourceInfo.getResource() == null) {
			resourceInfo.getFileInfo().setErrorMsg(IndexUtils.ERROR_RESOURCE_UP_TO_DATE);
			return;
		}
		
		// Do not download with size above limit
		if (resourceInfo.getHttpContentLength() != 0 && resourceInfo.getHttpContentLength() > fidConfig.getLong("RunParameter.compressedFileSizeLimit")) {
			System.out.println(IndexUtils.ERROR_COMPRESSED_FILE_SIZE_LIMIT_EXCEEDED+" ... skipping !");
			resourceManager.deleteResource(resourceInfo.getDataURL());
			resourceInfo.getFileInfo().setErrorMsg("Error : File size limit is : "+fidConfig.getLong("RunParameter.compressedFileSizeLimit"));
			return;
		}

		
		fileEntity = response.getEntity();
		if (fileEntity == null) return;

		// Download file to downloadFolder
		switch (resourceInfo.getResourceFormat()) {
				
		case RDF :
		case CONLL :		
		case ARCHIVE :
		case XML :
		case HTML :
		case LINGHUB :
		case PDF :
		case ONTOLOGY :

		   System.out.println(workerID + " : Downloading");
		   downloadedFile = new File(downloadDirectory,new File(url).getName());
		   System.out.println(downloadedFile.getAbsolutePath());
		   
		   FileUtils.copyInputStreamToFile(fileEntity.getContent(), downloadedFile);
		   resourceInfo.getFileInfo().setResourceFile(downloadedFile);
		   resourceInfo.getFileInfo().setFileSizeInBytes(FileUtils.sizeOf(downloadedFile));
		   
		   // compute hash values for file
		   resourceManager.updateResourceMD5Sha256(resourceInfo);
		   // check duplicate
		   ResourceState resourceState = resourceManager.getUpdateManager().getResourceState(resourceInfo);
		   // unregister resource from DB
		   if (resourceState == ResourceState.ResourceIsDuplicate) {
			   resourceManager.deleteResource(resourceInfo.getDataURL());
			   resourceInfo.setResource(null);
			   resourceInfo.setResourceState(resourceState);
		   }
		   return;
		   
		case METASHARE : // not used
			
			// Get HTML of download page
			downloadPage = EntityUtils.toString(fileEntity);
			
			// Analyze html for items that indicate info or download page on metashare
			
			// Look for download link in download page
			if (downloadPage.contains(IndexUtils.metaShareDownloadMarker)) {
			
			// call webdriver
			//wd = new MetashareWebdriver(downloadFolder.getAbsolutePath());
			//if (!wd.goDownloadPage(url)) return;
			downloadedFile = new File (downloadFolder,"archive.tar.gz");
			//file = downloadedFile;
			resourceInfo.getFileInfo().setResourceFile(downloadedFile);
			return;
				} else {
			return;
			}
			
		case SPARQL :
			return;
			
		case UNKNOWN :
			return;
			/* old
			// find download link in html for viable types RDF,CONLL
			downloadPage = EntityUtils.toString(fileEntity);
			// Look for download link in download page
			for (String suf : IndexUtils.rdfFileType) {
			if (downloadPage.contains(suf)) {
				System.out.println(suf+" *** "+url);
			}
			}
			*/

		default :
			return;
		}
		
	// Http exception handling
	} catch (Exception e) {

		String errorMessage = e.getMessage();
		String customErrorMessage = "";
		
		if (e instanceof SocketTimeoutException || e instanceof  java.net.SocketTimeoutException
		 || e instanceof ConnectTimeoutException) {
					customErrorMessage = resourceInfo.getDataURL()+" timeout after "+httpTimeout+" sec !";							
			}
			else if (e instanceof UnknownHostException) {
					customErrorMessage = resourceInfo.getDataURL()+" is unknown !";
			}
			else if (e instanceof HttpHostConnectException) {
					customErrorMessage = resourceInfo.getDataURL()+" could not establish connection !";
			}
			else if (e instanceof ClientProtocolException) {
					customErrorMessage = resourceInfo.getDataURL()+" ClientProtocolException !";
			} else {
					customErrorMessage = "A unknown error occured while downloading the resource !";
			}
		
			if (errorMessage != null) {
				resourceInfo.getFileInfo().setErrorMsg(StringUtils.substring(e.getMessage(),0,100));
			} else {
				resourceInfo.getFileInfo().setErrorMsg(customErrorMessage);
			}
			Utils.debug(customErrorMessage);

		//request.releaseConnection();
		return;
	}
	}
	
	public File getDownloadFolder() {
		return this.downloadFolder;
	}

	public int getHttpTimeout() {
		return httpTimeout;
	}

	/**
	 * Set timeout for HTTP request
	 * @param httpTimeout in seconds
	 */
	public void setHttpTimeout(int httpTimeout) {
		this.httpTimeout = httpTimeout;
		updateRequestConfig();
	}
	
	public ResourceManager getResourceManager(){
		return this.resourceManager;
	}
	
	
	/**
	 * Download URL to target folder. 
	 * @param url URL to download
	 * @param targetDirectory Target folder
	 * @return
	 */
	public File simpleFileDownload(String url, File targetDirectory) {
		
		System.out.println("Download URL : "+url);
		
		try {
			String protocol = new URL(url).getProtocol();
			
			switch (protocol) {
				
				// Local file
				case "file" :
					
					// create response parameter
					localFile = new File (new URL(url).getPath());
					if (localFile.exists()) {
						return localFile;
					} else {
						System.err.println("Local file : "+localFile.getAbsolutePath()+ " does not exist!");
						return null;
					}
			
				case "ftp" :
					System.out.println(IndexUtils.ERROR_FTP_SUPPORT+" - skipping resource !");
					return null;
				
				default:
					// Make HTTP request (works also for https://svn.code.sf.net/p/olia/code/trunk/owl/)
					CloseableHttpClient client = HttpClientBuilder.create().setSSLContext(ctx).setDefaultRequestConfig(requestConfig).build();
					HttpGet request = new HttpGet(url);
					CloseableHttpResponse response;
					
					response = client.execute(request);
					
					// check status
					int responseCode = -1;
					try {responseCode = response.getStatusLine().getStatusCode();} catch (Exception e){};
					if (!(responseCode == HttpStatus.SC_OK)) {
						System.out.println(responseCode + " ... Skipping !");
						return null;
					}
					
					fileEntity = response.getEntity();
					if (fileEntity == null) return null;
					
					 System.out.println("Downloading file to "+targetDirectory.getAbsolutePath());
					 downloadedFile = new File(targetDirectory,new File(url).getName());
					 System.out.println("Downloaded file : "+downloadedFile.getAbsolutePath());
					 FileUtils.copyInputStreamToFile(fileEntity.getContent(), downloadedFile);
					 
					 return downloadedFile;
				}	
				} catch (ClientProtocolException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
					
		return null;
	
	}
	
	
	
	private static class DefaultTrustManager implements X509TrustManager {

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			// TODO Auto-generated method stub
			return null;
		}
    }
	
}