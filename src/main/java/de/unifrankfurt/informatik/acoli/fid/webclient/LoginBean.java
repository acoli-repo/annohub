package de.unifrankfurt.informatik.acoli.fid.webclient;


import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFFormat;

import java.nio.charset.StandardCharsets;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.primefaces.context.RequestContext;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.optimaize.langdetect.profiles.LanguageProfile;

import de.unifrankfurt.informatik.acoli.fid.activemq.Consumer;
import de.unifrankfurt.informatik.acoli.fid.activemq.MessageBrowser;
import de.unifrankfurt.informatik.acoli.fid.detector.OptimaizeLanguageTools1;
import de.unifrankfurt.informatik.acoli.fid.detector.TikaTools;
import de.unifrankfurt.informatik.acoli.fid.exec.Executer;
import de.unifrankfurt.informatik.acoli.fid.exec.Run;
import de.unifrankfurt.informatik.acoli.fid.linghub.UrlBroker;
import de.unifrankfurt.informatik.acoli.fid.owl.ModelDefinition;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.UserManagement;
import de.unifrankfurt.informatik.acoli.fid.serializer.JSONSerializer;
import de.unifrankfurt.informatik.acoli.fid.serializer.RDFSerializer;
import de.unifrankfurt.informatik.acoli.fid.types.AccountType;
import de.unifrankfurt.informatik.acoli.fid.types.AnnohubExportFormat;
import de.unifrankfurt.informatik.acoli.fid.types.Comment;
import de.unifrankfurt.informatik.acoli.fid.types.DetectionMethod;
import de.unifrankfurt.informatik.acoli.fid.types.FileInfo;
import de.unifrankfurt.informatik.acoli.fid.types.InvalidModelDefinitionException;
import de.unifrankfurt.informatik.acoli.fid.types.LanguageMatch;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceMetadata;
import de.unifrankfurt.informatik.acoli.fid.types.MetadataSource;
import de.unifrankfurt.informatik.acoli.fid.types.ModelType;
import de.unifrankfurt.informatik.acoli.fid.types.ParseResult;
import de.unifrankfurt.informatik.acoli.fid.types.ParseStats;
import de.unifrankfurt.informatik.acoli.fid.types.ProcessState;
import de.unifrankfurt.informatik.acoli.fid.types.RDFExportMode;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceFormat;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceProcessState;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceState;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceType;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceTypeInfo;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceTypeSpecifier;
import de.unifrankfurt.informatik.acoli.fid.types.UpdatePolicy;
import de.unifrankfurt.informatik.acoli.fid.types.UserAccount;
import de.unifrankfurt.informatik.acoli.fid.util.IndexUtils;
import de.unifrankfurt.informatik.acoli.fid.util.LocateUtils;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;
import edu.emory.mathcs.backport.java.util.Collections;


@ManagedBean(name="login")
@SessionScoped
//@Singleton
public class LoginBean implements Serializable {
	
	@ManagedProperty(value="#{execute}")
    private ExecutionBean executionBean;
	
	@ManagedProperty(value="#{auth.session}")
	HttpSession session;
	
	@ManagedProperty(value="#{auth.userAccount}")
	private UserAccount userAccount;
	
	private EditManager editManager;
	
	private ResourceInfo guiSelectedResource = null;

	private List <ResourceInfo> rfl = new ArrayList<ResourceInfo>();
	private List <ResourceInfo> filteredRfl = null;
	private static final long serialVersionUID = 1L;
	private boolean loaded = false;
	
	private ResourceInfo selectedDummyResource;
	private static Executer executer;
	private String metaDataURLGui="";
	private Model rdfModelIntern = null;
	private Model rdfModelExtern = null;
	private StreamedContent file;
	private String uploadURL="";
	private Boolean resourceUploadImportMetadata=false;
	private Boolean resourceUploadAutoAccept=false;
	//private static String uploadURL="";
	
	private List <ResourceInfo> rflError = new ArrayList<ResourceInfo>();
	private List <ResourceInfo> filteredRflError = null;
	
	@ManagedProperty(value="#{fidConfig}")
	private static XMLConfiguration fidConfig;
	
	private List <ResourceInfo> myResources = null;
	//private static List <ResourceInfo> myResources = null;
	
	private List<LanguageProfile> languageProfileList = new ArrayList<LanguageProfile>();
	private LocateUtils locateUtils = new LocateUtils();
	
	private String metaFormat = "";
	private String metaType = "";
	private String metaRights = "";
	private String metaLicense = "";
	private String metaDctSource = "";
	private String metaDctIdentifier = "";
	private String metaPublisher="";
	private String metaTitle="";
	private String metaUbTitle="";
	private String metaDescription="";
	private String metaCreator="";
	private String metaContributor="";
	private String metaLocation="";
	private Date   metaDate = null;
	private String metaYear = "";
	private String metaContact = "";
	private String metaWebpage = "";
	private String metaDctLanguageString = "";
	private String metaDcLanguageString = "";
	private String metaSubject = "";
	private String metaSource = "";
	private String metaUrl = "";
	
	private int rdfMaxSamples=0;
	private int rdfActivationThreshold=0;
	private int rdfThresholdForGood=0;
	private int rdfThresholdForBad=0;
	private int xmlMaxSamples=0;
	private int xmlActivationThreshold=0;
	private int xmlThresholdForGood=0;
	private int xmlThresholdForBad=0;
	private int conllMaxSamples=0;
	private int conllActivationThreshold=0;
	private int conllThresholdForGood=0;
	private int conllThresholdForBad=0;
	
	private int checkBrokenLinksInterval=0;
	
	private int maxResourceUploads = 0;
	private int maxResourceFiles = 0;
	private int maxResourceUploadSize = 0;
	private int publishRDFExportInterval = 0;
	private int newResources = 0;

	private String emptyMessage="No records found.";
	
	private String findSelectedModel="";

	
	
	@EJB
    ExecuterEjb executionEjb;
	
	private static String jsonFilePath = "";
	private int tableFirstPage = 0;
	
	@ManagedProperty(value="#{resourceManager}")
	private ResourceManager resourceManager;
	
	private GuiConfiguration guiConfiguration;

	private MessageBrowser messageBrowser;
	
	private ArrayList<ResourceInfo> inRegDB = new ArrayList<ResourceInfo>();

	private ArrayList<ResourceInfo> inProgress = new ArrayList<ResourceInfo>();

	private ArrayList<ResourceInfo> inQueue = new ArrayList<ResourceInfo>();
	
	private ArrayList<ResourceInfo> inSelection = new ArrayList<ResourceInfo>();


	public String myUserID = "";
	
	
	private boolean permOtherRead=false;
	private boolean permOtherEdit=false;
	private boolean permOtherExport=false;
	private boolean canRead=false;
	private boolean canEdit=false;
	private boolean canExport=false;
	private boolean canDelete=false;
	private boolean canAdd=false;     // only used for permission to comment
	
	private String searchTextField="";

	private String infobarText="";
	private String searchInfobarText="";
	
	private boolean autoSelectSearchResults=false;
	
	private String languageSearch="";
	private String modelSearch="";
	private String metadataSearch="";
	private String annotationSearch="";
	private String annotationSearchMode="Tag";
	private String oliaSearch="";
	private String oliaClassDescription="";
	private String commentSearch="";
	
	private String resourceNameSearch="";
	private Boolean resourceNameSearchIgnoreCase=true;
	private Boolean commentSearchIgnoreCase=true;
	private Boolean metadataSearchIgnoreCase=true;
	private Boolean annotationSearchIgnoreCase=true;
	
	//private static HashSet<String> editedResources = new HashSet<String>();
	
	private String findDataAndOR1 = "or";
	private String findDataAndOR2 = "or";
	private String annotationSearchOrAnd = "or";
	
	private String findFilterByType = null; // ALL
	 
	private boolean showMyResources = true;
	private boolean showSelectedResources = true;
	private boolean showSearchResources = true;

	
	private List <ResourceInfo> searchResourceResultList = new ArrayList<ResourceInfo>();

	//private HashSet<String> tmpResourceUrls = new HashSet<String>();
	
	private ResourceType selectedResourceType=null;
	private String selectedResourceTypeSpecifier="";
	private String resourceComments="";
	private String newCommentTitle;
	private String newCommentText;
	private Integer newRelatedPostId=0;
	private List<Integer> previousCommentIds;
	private HashMap<ResourceFormat, ParseStats> customParseStats=null;
	private boolean newCommentWasAdded=false;
	private String cPasswd_1="";
	private String cPasswd_2="";
	private String cPasswd_1a="";
	private String cPasswd_2a="";
	private String cEmail="";
	private boolean cPasswdActive=false;
	private String deleteMessage="";

	private ResourceInfo resource2Reprocess=null;
	private UserManagement userManagement;

	private String selectedUser="";
	private String selectedUserAccountType;

	private List<String> userLoginsList = new ArrayList<String>();

	private UserAccount selectedUserAccount=new UserAccount("","","");

	private boolean userLoginWritable=false;

	private boolean createNewUser=false;
	
	private String selectedUserSearch="";

	private ModelDefinition modelDefinition=null;
	
	private String editedDataUrl="";
	private Boolean editedDataUrlIsOnline;

	private Boolean findExclusiveLanguages=false;
	private Boolean findExclusiveModels=false;

	private boolean editMetadataOpen=false;
	
	private ResourceMetadata mdCopyBuffer=null;
	
	private int lexiconCount = 0;
	private int corporaCount = 0;
	private int ontologyCount = 0;
	private int languageCount = 0;
	private int xmlResCount = 0;
	private int rdfResCount = 0;
	private int conllResCount = 0;

	
	/*private static LanguageMatch selectedDummyLanguage;
	private static ModelMatch selectedDummyModel;
	private static FileResult selectedDummyFileResult;
	private static ResourceInfo selectedResource;
	private static ResourceInfo selectedResourceOrg;
	private static LanguageMatch selectedLanguageMatch;
	private static ModelMatch selectedModelMatch;
	private static ArrayList <Integer> conllColumnsWithText = new ArrayList<Integer>();
	private static ArrayList <Integer> conllColumnsWithModels = new ArrayList<Integer>();
	private static ArrayList <Integer> freeConllColumns = new ArrayList<Integer>();
	private static int editedConllModelColumn=0;
	private static ModelType editedConllModel = null;
	private String metaDataURLNew="";
	private String dataURLNew="";	
	private String dataURLGui="";
	private static TreeNode selectedNode = null;
	private static int selectedCol = 0;				// gui variable index for selected combobox entry
	private static int selectedLangCol = -1;		// gui variable index for selected combobox entry
	private ModelType selectedColModel = null;
	private HashSet<String> sessions = new HashSet<String>();
	//private static ArrayList<String> messages = new ArrayList<String>();
	private String commentGui="";
	private static String manualISOInput="";
	private static String isoLanguageLabel="unknown language";
	private static Integer newLangConllColumn = 0;
	private static Integer newModelConllColumn = 0;
	private static ModelType newModel;
	private static boolean noPolling = false;
	private static String selectedMultiAcdState=ProcessState.ACCEPTED.name();
	private static String selectedMultiAcdURL="";
	private static String multiACDInfo;
	private static HashSet<String> multiACDResources = new HashSet<String>();
	private static ArrayList<ResourceInfo> multiACDResources2 = new ArrayList<ResourceInfo>();
	private static PostgresManager mng;
	private int getBllInfoConllCall = 0;
	private FileResult selectedFileResult;
	private String confirmMessage;
	private Boolean resourceQueueIsEmpty;
	private static List<ModelMatch> selectedModels=null;*/


	@PostConstruct
    public void init() {
		
		System.out.println("init loginbean");
		
		if (loaded) return;
		loaded = true;

		executer = ExecutionBean.getPublicExecuter();Utils.debug(executer.getExecutionMode());
	    resourceManager = executer.createNewResourceManagerInstance();
		fidConfig = Executer.getFidConfig();
		editManager = executionBean.getEditManager();
	    
		
		myResources = new ArrayList<ResourceInfo>();
		//rflError = resourceManager.getUnSuccessFullResourcesRI();
		
		// add match info to resources objects that have results
		//executer.getWriter().getQueries().getFileResults(rfl, resourceManager);
	
//	    Consumer consumer = new Consumer(Executer.MQ_OUT_1);
//	    try {
//			consumer.getConsumer().setMessageListener(this);
//		} catch (JMSException e) {
//			e.printStackTrace();
//		}
	    
	    messageBrowser = new MessageBrowser();
	    //resourceManager = executionBean.getPublicExecuter().getResourceManager();
	    myUserID = (String) session.getAttribute("username");
	    System.out.println("1 :"+myUserID);
	    System.out.println(((HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false)).getAttribute("username"));
	    initParseOptions(fidConfig);
	    
	    // gui vars
	    publishRDFExportInterval=fidConfig.getInt("RunParameter.publishRDFExportInterval");
	    checkBrokenLinksInterval=fidConfig.getInt("RunParameter.checkBrokenLinksInterval");
	    maxResourceUploadSize=fidConfig.getInt("Quotas.maxResourceUploadSize");
	    maxResourceFiles=fidConfig.getInt("Quotas.maxResourceFiles");
	    maxResourceUploads=fidConfig.getInt("Quotas.maxResourceUploads");
	    try {
			jsonFilePath = fidConfig.getString("RunParameter.JsonExportFile");
			Utils.debug("jsonFilePath : "+jsonFilePath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	
	    userManagement = ExecutionBean.getUserManagement();
	    
	    /*rflError = ExecutionBean.getResourceCache().getCachedErrorResourcesByUrl(
				resourceManager.getResourcesOwnedByUserAsUrl(myUserID));*/
	    
	    languageProfileList = OptimaizeLanguageTools1.getLanguageProfiles();
	    
	    if (userAccount.getAccountType() == AccountType.GUEST) {
	    	//emptyMessage = "Please select language resources via the search tools!";
	    	showMyResources = false;
	    	showSelectedResources = false;
	    	showSearchResources = true;
	    	
	    	// random language
//	    	ArrayList<String> lcodes = new ArrayList<String>(TikaTools.isoSIL.keySet());
//	    	int z = ((Long) new Date().getTime()).intValue() % 6000;
//	    	languageSearch = lcodes.get(z);
//	    	findData();
	    	
	    	// open search
	    	resourceNameSearch = "http";
	    	RequestContext context = RequestContext.getCurrentInstance();
			context.execute("PF('findData').show();");
	    	
	    	
	    	//System.out.println("random :"+z);
	    	//modelSearch = "ONTOLEX";
	    	//findFilterByType = "LEXICON";
	    	//resourceNameSearch = "";
	    	
	    	
	    	
	    } else {
	    	showMyResources = true;
	    	showSelectedResources = true;
	    	showSearchResources = true;
	    }
	    
	    updateCounts();
	    refreshAction();
	}
	
	

	/**
	 * @param fidConfig
	 */
	private void initParseOptions(XMLConfiguration config) {
		
		rdfMaxSamples= config.getInt("Sampling.Rdf.maxSamples");
		rdfActivationThreshold = config.getInt("Sampling.Rdf.activationThreshold");
		rdfThresholdForGood = config.getInt("Sampling.Rdf.thresholdForGood");
		rdfThresholdForBad = config.getInt("Sampling.Rdf.thresholdForBad");
		
		xmlMaxSamples= config.getInt("Sampling.Xml.maxSamples");
		xmlActivationThreshold = config.getInt("Sampling.Xml.activationThreshold");
		xmlThresholdForGood = config.getInt("Sampling.Xml.thresholdForGood");
		xmlThresholdForBad = config.getInt("Sampling.Xml.thresholdForBad");
		
		conllMaxSamples= config.getInt("Sampling.Conll.maxSamples");
		conllActivationThreshold = config.getInt("Sampling.Conll.activationThreshold");
		conllThresholdForGood = config.getInt("Sampling.Conll.thresholdForGood");
		conllThresholdForBad = config.getInt("Sampling.Conll.thresholdForBad");

	}
	

	private Map<ResourceFormat, ParseStats> makeCustomParseStats() {
		
		customParseStats = new HashMap <ResourceFormat, ParseStats>();
		customParseStats.put(ResourceFormat.RDF,
				new ParseStats(
						rdfThresholdForGood,
						rdfThresholdForBad,
						rdfActivationThreshold,
						rdfMaxSamples,
						-1));
		
		customParseStats.put(ResourceFormat.XML,
				new ParseStats(
						xmlThresholdForGood,
						xmlThresholdForBad,
						xmlActivationThreshold,
						xmlMaxSamples,
						-1));
		
		customParseStats.put(ResourceFormat.CONLL,
				new ParseStats(
						conllThresholdForGood,
						conllThresholdForBad,
						conllActivationThreshold,
						conllMaxSamples,
						-1));
		return customParseStats;
	}


	private String getSessionID() {
		FacesContext fCtx = FacesContext.getCurrentInstance();
		HttpSession session = (HttpSession) fCtx.getExternalContext().getSession(false);
		Utils.debug(session.getId());
		return session.getId();
	}
	
	
	public void uploadFile(ActionEvent actionEvent) {
    	Utils.debug("Uploadfile");
    	FacesContext context = FacesContext.getCurrentInstance();
        Map<String,String> params = context.getExternalContext().getRequestParameterMap();        
        Utils.debug(
        		StringUtils.newStringUtf8(
        				Base64.decodeBase64(
        						params.get("param1").split(",")[1]))); // remove data:*/*;base64, at string start
        showInfo("File was uploaded successfully !"); 
    }
	
	public String uploadResourceFile() {
		
		Utils.debug("upload file");
		
		try {
			
			ResourceInfo resourceInfo;
			String fileName = FileUploadBean.getUploadedFile().getFileName();
			String inputFilePath = new File (new File(fidConfig.getString("RunParameter.ServiceUploadDirectory")),fileName).getAbsolutePath();			
			
			if (resource2Reprocess == null) {
				
				resourceInfo = new ResourceInfo("file://"+inputFilePath,"http://fid/metadata/tbc");
				resourceInfo.setUserID(myUserID);
				
				// Write uploaded file to local fs
				FileUploadBean.getUploadedFile().write(inputFilePath);
			} else {
			
				resourceInfo = resource2Reprocess;
			}
			
			
			HashMap<String, ResourceInfo> x = new HashMap<String, ResourceInfo>();
			x.put(resourceInfo.getDataURL(), resourceInfo);
			
			// check TSV file (upload of resource list)
			if (!IndexUtils.filterTSV(x , ResourceFormat.TSV).isEmpty()) {
				
				// Configure UpdatePolicy UPDATE_NEW for UrlBroker 
				// s.t. resources that are already present in the DB will be removed from the uploaded tsv list.
				// Finally from the remaining resources only those will be processed that have changed.
				// This behaviour is controlled by UPDATE_CHANGED that is configured in the executer
				// component (see init method on top).
				//UpdatePolicy updatePolicy = UpdatePolicy.UPDATE_NEW; 
				
				XMLConfiguration config = new XMLConfiguration();
				config.addProperty("RunParameter.urlSeedFile", inputFilePath);
				config.addProperty("RunParameter.urlPoolFile", "/tmp/urlpool");
				config.addProperty("RunParameter.updatePolicy", UpdatePolicy.UPDATE_NEW);
				config.addProperty("Linghub.linghubQueries.resourceQueries","");
				config.addProperty("Linghub.useQueries", false);
				config.addProperty("Linghub.enabled", false);
				config.addProperty("RunParameter.urlFilter", "RDF,CONLL,ARCHIVE,XML");
				
				UrlBroker urlBroker = new UrlBroker(resourceManager, config);
				HashSet<ResourceInfo> resourceInfoList = urlBroker.makeUrlPool();
				filterAlreadyQueuedResources(resourceInfoList);
				
				// Get the set of already processed resources in the tsv file
				// and add these to the users view
				Set<String> urlsInTSV = UrlBroker.readUrlseedFile(new File(inputFilePath), new HashMap<String, ResourceInfo>());
				int alreadyProcessed = 0;
				boolean found = false;
				for (String resourceUrl : urlsInTSV) {  	 // all resource URLs in uploaded file
					
					found = false;
					for (ResourceInfo rs : resourceInfoList) {// only new resource URLs from uploaded file
						if (resourceUrl.equals(rs.getDataURL())) {
							found = true;
							break;
						}
					}
					if (!found) { // resource was already processed -> select it
						resourceManager.selectResource(myUserID, resourceUrl);
						alreadyProcessed++;
					}
				}
				if (alreadyProcessed > 0) {
					// alternative queue
					// refreshAction(); not for new upload page
					setInfoText("Added "+alreadyProcessed+" already processed resources from upladed tsv file to the view !");
					showStickyMessage(getInfobarText(), FacesMessage.SEVERITY_INFO);
					//FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:infobar");
					//RequestContext.getCurrentInstance().reset("form:infobar");
				}
				
				
				if (!resourceInfoList.isEmpty()) {
					
					String error = isFileUploadAllowed(resourceInfoList.size());
					
					if(!error.isEmpty()){
						showError(error);
						return "";
					};
					
					showStickyMessage("TSV file : Queuing "+resourceInfoList.size()+" resources for processing !", FacesMessage.SEVERITY_INFO);
					//showInfo("Found TSV file : Queuing "+resourceInfoList.size()+" resources for processing !");
					for (ResourceInfo resourceInfo_ : resourceInfoList) {
						
						// set resource owner
						resourceInfo_.setUserID(myUserID);
						
						// skip local files (TODO implement upload)
						if(IndexUtils.urlHasFileProtocol(resourceInfo_.getDataURL())) {continue;}
						
						Utils.debug("queuing resource : "+resourceInfo_.getDataURL());
						//rfl.add(resourceInfo_);
						
						enqueueResource(resourceInfo_, false);
					}
					//guiTableRefresh();
					//guiQueueRefresh();
				} else {
					showInfo("No new resources found in "+inputFilePath+" !");
				}
				
				return "";
			} else {
				String error = isFileUploadAllowed(1);
				if(!error.isEmpty()){
					showError(error);
					return "";
				};
			}
			
			if (resourceInfo.getResourceFormat() == ResourceFormat.UNKNOWN) {
				showError("File Format not supported !");
				return "";
			} else {
				Boolean answer = isInResourceDatabaseOrIsAlreadyQueued(resourceInfo);
				if (answer == null) return "";// dialog
				if (answer) {
					setInfoText("Already processed resource "+inputFilePath+" is added to your workspace !");
					
					return "login?faces-redirect=true";
				}
				//if (isInResourceDatabase("file://"+inputFilePath)) return;
				enqueueResource(resourceInfo, true);
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	
	
	/**
	 * @param string
	 * @param severityInfo
	 */
	private void setInfoText(String string) {
		this.setInfobarText(string);
	}


	public String uploadResourceUrl() {
		
		Utils.debug("upload url : "+uploadURL);
		
		String error = isFileUploadAllowed(1);
		if(!error.isEmpty()){
			Utils.debug(error);
			showError(error);
			return "";
		};
		
		Utils.debug("upload is allowed");
		
		ResourceInfo resourceInfo;
		if (resource2Reprocess == null) {
			resourceInfo = new ResourceInfo(uploadURL,"http://fid/metadata/tbc");
			if (resourceInfo.getDataURL() == null) {
				error = "Invalid URL : "+uploadURL;
				Utils.debug(error);
				showError(error);
				return "";
			}
			resourceInfo.setUserID(myUserID);
		} else {
			resourceInfo = resource2Reprocess;
		}
		
		
		Boolean answer = isInResourceDatabaseOrIsAlreadyQueued(resourceInfo);
		if (answer == null) return "";// dialog
		if(answer) {
			Utils.debug("Resource "+uploadURL+" is already processed");
			showStickyMessage("Resource "+uploadURL+" is already processed",FacesMessage.SEVERITY_INFO);
			setInfoText("Resource "+uploadURL+" (already processed) added to workspace !");
			return "login?faces-redirect=true";
		}
		
		try {
			
			
			if (resourceInfo.getResourceFormat() == ResourceFormat.UNKNOWN) {
				showError("File Format not supported !");
				// reset input field
				uploadURL="";
				return"";
			} else {
				
				//showInfo("Enqueue resource !");
				enqueueResource(resourceInfo, true);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// reset input field
		uploadURL="";
		return "";
	}
	
	
	public List <ResourceInfo> getResourceInfoList() {

		//Utils.debug("getResourceInfoList");
		//sessions.add(getSessionID());		
		return rfl;
	}
	
	
	public void refreshButton() {
		
		newResources=0;
		refreshAction();
		setInfobarText("");
		try {
			FacesContext.getCurrentInstance().getExternalContext().redirect("login.xhtml");
		    FacesContext.getCurrentInstance().responseComplete();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//return "login?faces-redirect=true";
	}
	
	
	public void refreshAction() {
		
		System.out.println("refreshAction");
		
		guiSelectedResource = null;
		
		rfl.clear();
		inRegDB.clear();
		inProgress.clear();
		inQueue.clear();
		inSelection.clear();
		//rflError 

		// my resources
		if (showMyResources) {
			// 1. get all resources owned by user from reg-DB
			Utils.debug("loading my resources ");
			inRegDB = ExecutionBean.getResourceCache().getCachedResourcesByUrl(
					resourceManager.getResourcesOwnedByUserAsUrl(myUserID));
			//inRegDB = resourceManager.getDoneResourcesOwnedByUser(myUserID);
			Utils.debug("done");

			
			// 2. subtract all resources that are in progress by any worker (owned by user)
			Utils.debug("loading resources in progress");
			inProgress = ExecutionBean.getPublicExecuter().getActiveResources(myUserID);
			
			for (ResourceInfo resourceInfo : inRegDB) {
				boolean found = false;
				for (ResourceInfo activeResource : inProgress) {
					if (activeResource.getDataURL().equals(resourceInfo.getDataURL())) {found=true;}
				}
				if (found) {
					resourceInfo.setResourceProcessState(ResourceProcessState.INPROGRESS);
				} else {
					resourceInfo.setResourceProcessState(ResourceProcessState.FINISHED);
				}
				rfl.add(resourceInfo);
			}
			Utils.debug("done");

			// 3. add all resources in MQ_IN_1 queue (owned by user)
			Utils.debug("loading queued resources");
			// alternative queue
			inQueue = ExecutionBean.getResourceCache().getQueuedResourceForUser(myUserID);
			//inQueue = messageBrowser.getQueuedResources(myUserID); // very slow
			for (ResourceInfo waitingResource : inQueue) {
				waitingResource.setResourceProcessState(ResourceProcessState.WAITING);
				rfl.add(waitingResource);
			}
			Utils.debug("done");

		}
		
		// 4. add all selected resources by user
		if (showSelectedResources) {
			Utils.debug("loading selected resources ");
			System.out.println("showSelectedResources");
			inSelection = ExecutionBean.getResourceCache().getCachedResourcesByUrl(
					resourceManager.getResourcesSelectedByUserAsUrl(myUserID));
				
			//inSelection = resourceManager.getDoneResourcesSelectedByUser(myUserID);
		
			ArrayList<ResourceInfo> inProgressAll = executionBean.getPublicExecuter().getActiveResources();
			for (ResourceInfo selectedResource : inSelection) {
				boolean found = false;
				for (ResourceInfo inprogress : inProgressAll) {
					if (inprogress.getDataURL().equals(selectedResource.getDataURL()));
					found = true; break;
				}
				if (!found) {
					// the selectedResource from the DB is not in progress -> selectedResource is finished
					selectedResource.setResourceProcessState(ResourceProcessState.FINISHED);
					rfl.add(selectedResource);
				}
			}
			Utils.debug("done");
		}
		
		// 5. add all resources from search
		if (showSearchResources) {
			
			/*tmpResourceUrls.clear();
			for (ResourceInfo resourceInfo : rfl) {
				tmpResourceUrls.add(resourceInfo.getDataURL());
			}*/
			
			if(userAccount.getAccountType() == AccountType.GUEST) {
					searchInfobarText=""; // may contain wrong counts
			}
			
			ResourceInfo duplicate = null;
			for (ResourceInfo rs : searchResourceResultList) {
				//System.out.println("search result : "+rs.getDataURL());
				
				if(userAccount.getAccountType() == AccountType.GUEST && !rs.isApproved()) {
					continue; // GUEST users only see APPROVED resources
				}
				
				// filter resources from search result that are already the rfl list (are owned or selected)
				/*if (!tmpResourceUrls.contains(rs.getDataURL())) {
					rs.setResourceProcessState(ResourceProcessState.SEARCH);
					rfl.add(rs);
				}*/
				duplicate = null;
				for (ResourceInfo resourceInfo : rfl) {
					if (resourceInfo.getDataURL().equals(rs.getDataURL())) {
						duplicate = resourceInfo;
						break;
					}
				}
				if (duplicate == null) {
					rs.setResourceProcessState(ResourceProcessState.SEARCH);
					rfl.add(rs);
				} else {
					duplicate.setResourceProcessState(ResourceProcessState.SEARCH);
				}
			}
		}
		
		
		
		// check if resources with status SEARCH or FINISHED are in the resourceMap
    	/*if(fidConfig.getBoolean("RunParameter.cached")) {
    		for (ResourceInfo rs : rfl) {
    			if (rs.getResourceProcessState() == ResourceProcessState.FINISHED ||
    				rs.getResourceProcessState() == ResourceProcessState.SEARCH) {
    				// update resourceMap with results of new uploaded resources
	    			if (!ExecutionBean.getResourceCache().getResourceMap().containsKey(rs.getDataURL())) {
	    				System.out.println(rs.getDataURL()+ "not in resourceMap !");
	    				//ArrayList<ResourceInfo> fileResults = resourceManager.getDoneResourceRI(resourceManager.getResource(rs.getDataURL()));
	    				ResourceCache resourceCache = resourceManager.getDoneResourcesRIMap(rs.getDataURL());
	    				ArrayList<ResourceInfo> fileResults = resourceCache.getResourceFileMap().get(rs.getDataURL());
	    				if (!fileResults.isEmpty()) {
		    				executer.getWriter().getQueries().getFileResults(fileResults, resourceManager);
	    					Utils.debug("refreshAction : addResource to cache "+rs.getDataURL());
		    				ExecutionBean.getResourceCache().addResource2(resourceCache.getResourceMap().get(rs.getDataURL()), fileResults);
	    				}
	    			}
    			}
    		}
    	}*/
		
		System.out.println("RegDB resources count :  "+inRegDB.size());
		System.out.println("InProgress  resources count :  "+inProgress.size());
		System.out.println("queued resources count :  "+inQueue.size());
		System.out.println("selected resources count :  "+inSelection.size());

		
		// getResourceInfoList() is called automatically thereafter
	}
	
	
	
	
	public List<ResourceInfo> getFilteredResourceInfoList() {
	    return filteredRfl;
	}
	
	public void setFilteredResourceInfoList(List <ResourceInfo> filteredRfl) {
	    this.filteredRfl = filteredRfl;
	}
	
	public int getTableFirstPage() {
		return tableFirstPage;
	}


	public void setTableFirstPage(int tableFirstPage) {
		this.tableFirstPage = tableFirstPage;
	}
	
	public GuiConfiguration getGuiConfiguration() {
		return guiConfiguration;
	}
	
	public void setGuiConfiguration(GuiConfiguration config) {
		guiConfiguration = config;
	}
	
	// Dummy methods to enable row selection on table !
    public void setSelectedDummyResource(ResourceInfo selectedResource) {}
    public ResourceInfo getSelectedDummyResource() {
        return selectedDummyResource;
    }
    
    
    
    /**
     * Check if it is allowed to process a given resource. Processing is canceled if the resource is
     * <p> 1. is already in the process queue
     * <p> 2. currently processed
     * <p> 3. is already in the database (resource owner is allowed to reprocess resource)
     * @param resourceUrl
     * @return True if it is not allowed to process the resource - else false
     */
    private Boolean isInResourceDatabaseOrIsAlreadyQueued(ResourceInfo resourceInfo) {
    	
    	if(resource2Reprocess != null) return false; // not null if reprocess chosen in dialog
    	
    	String resourceUrl = resourceInfo.getDataURL();
    	
    	// 1. check if resourceUrl already queued
    	// alternative queue
		inQueue = ExecutionBean.getResourceCache().getQueuedResourceForUser(myUserID);
    	//ArrayList<ResourceInfo> inQueue = messageBrowser.getQueuedResources();
		for (ResourceInfo waitingResource : inQueue) {
			if (waitingResource.getDataURL().equals(resourceUrl)) {
				showError("Resource "+resourceUrl+" is already in queue - skipping !");
				return true;
			}
		}
    	
    	// 2. check if resource is actively processed
		for (String x : ExecutionBean.getPublicExecuter().getActiveResourceUrls()) {
			if (x.equals(resourceUrl)) {
				showError("Resource "+resourceUrl+" is actively processed - skipping !");
				return true;
			}
		}

    	
    	// 3. check if resourceUrl has already been parsed
		if (resourceManager.getDoneResourceUrls().contains(resourceUrl)) {
		
			if (!resourceManager.getResourceOwner(resourceUrl).equals(myUserID)) {
				showError("Resource "+resourceUrl+" was already processed by another user - skipping !");
				
				// automatically select the resource for the user, s.t. it is shown together with his own resources
				resourceManager.selectResource(myUserID, resourceUrl);
				refreshAction();
				return true;
				
			} else {
				//showInfo("Resource "+resourceUrl+" has been already processed by yourself - reprocessing !");
				Utils.debug("Resource "+resourceUrl+" has been already processed by yourself - reprocessing !");
				RequestContext context = RequestContext.getCurrentInstance();
				context.execute("PF('reprocessResourceswv').show();");
				resource2Reprocess=resourceInfo;
				return null;
			}
    	}
		
		
		// 4. check if resource is duplicate (check with etag ? before download starts)
		
		return false;
    }
    
    
    public void reprocessResource() {
    	
    	resource2Reprocess.getFileInfo().setForceRescan(true);
    	try {
			URL url = new URL(resource2Reprocess.getDataURL());
			if(url.getProtocol().equals("file")) {
				uploadResourceFile();
			} else {
				uploadResourceUrl();
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
    	notReprocessResource();
    }
    
    public void notReprocessResource() {
    	resource2Reprocess=null;
    }
    
    
    /**
     * @deprecated
     * @param resourceUrl
     * @return
     */
    private boolean isInResourceDatabase(String resourceUrl) {
		
		if (true) return false;
	
		Utils.debug("isInResourceDatabase : "+resourceUrl);
		
		// check url exists in database
		if (resourceManager.getDoneResourceUrls().contains(resourceUrl))
		{
			switch (resourceManager.getUpdateManager().getUpdatePolicy()) {
			
			case UPDATE_NEW:
			case UPDATE_CHANGED:
				
				// check if the resource had any results (is in the list)
				boolean found = false;
				for (ResourceInfo resourceInfo : rfl) {
					if (resourceInfo.getDataURL().equals(resourceUrl)) {
						found = true;
						break;
					}
				}
				
				if (found) {
					showInfo("Resource : "+resourceUrl+" already in database - use Rescan instead !");
					// reset input field
					// Uncomment to disable possible rescan
					uploadURL="";
					Utils.debug("true");
					return true;
				} 
				break;
				
			case UPDATE_ALL:
				break;
				
			default:
				
				showInfo(IndexUtils.ERROR_UNKNOWN_UPDATE_POLICY);
				Utils.debug(IndexUtils.ERROR_UNKNOWN_UPDATE_POLICY);
				// reset input field
				// Uncomment to disable possible rescan
				uploadURL="";
				return true;
			}	
		}
		
		// o.k.
		Utils.debug("false");
		return false;
	}


// now in ExecutionBean
//	/* (non-Javadoc) 
//	 * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
//	 */
//	@Override
//	public void onMessage(Message message) {
//		
//		ResourceInfo ri = Consumer.extractResourceInfo(message);
//		ExecutionBean.getResourceCache().updateCacheWithResourceFromDB(ri.getDataURL());
//		
//	}
	
	public ExecutionBean getExecutionBean() {
		return executionBean;
    }
	
    public void setExecutionBean (ExecutionBean neededBean) {
    	this.executionBean = neededBean;
    }
    
    
    private void enqueueResource(ResourceInfo resourceInfo, boolean updateGui) {
		
    	// use metadata from datafile
    	resourceInfo.setResourceUploadImportMetadata(resourceUploadImportMetadata);
    	resourceInfo.setResourceUploadAutoAccept(resourceUploadAutoAccept);
    	
		// check if current user has an account
    	if (!resourceManager.userExists(this.myUserID)) {
    		showError("Upload is not allowed !");
    		Utils.debug("Error : "+this.myUserID+" has no user account !");
    		return;
    	}
    	
    	//if (filterAlreadyQueuedResources(resourceInfo)) return;
    	
    	if (customParseStats != null) {
    		resourceInfo.setCustomParseStats(customParseStats);
    	}
		
		myResources.add(resourceInfo);
		executionEjb.addResource(resourceInfo);
		
		if (updateGui) {
			showInfo("Queuing resource for processing !");
			// guiTableRefresh();
			//guiQueueRefresh();
		}
	}
	
	
	
	/**
	 * Filter resources that are already queued for processing
	 * @param resourceInfoList
	 * @return true if filtering has been applied
	 */
	private void filterAlreadyQueuedResources(HashSet<ResourceInfo> resourceInfoList) {
	
		ArrayList<ResourceInfo> queuedResources = messageBrowser.getQueuedResources();
		Iterator<ResourceInfo> iterator = resourceInfoList.iterator();
		while (iterator.hasNext()) {
			ResourceInfo r = iterator.next();
			for (ResourceInfo rq : queuedResources) {
				if (r.getDataURL().equals(rq.getDataURL())) {
					iterator.remove();
				}
			}
		}
		
	}
	
	public String getUploadURL() {
		return uploadURL;
	}

	public void setUploadURL(String uploadURL_) {
		uploadURL = uploadURL_;
	}
	
	
	public void showInfo(String message) {
		showMessage(message, FacesMessage.SEVERITY_INFO);
	}
	
	public void showError(String message) {
		Utils.debug(message);
		showMessage(message, FacesMessage.SEVERITY_ERROR);
	}
	
	public void showWarning(String message) {
		showMessage(message, FacesMessage.SEVERITY_WARN);
	}
	
	public void showMessage(String message, Severity severity) {
	   	 FacesMessage msg = new FacesMessage(severity, "", message);
	     FacesContext.getCurrentInstance().addMessage(null, msg);
	     RequestContext.getCurrentInstance().update(("form:msgs"));
	}
	
	public void showStickyMessage(String message, Severity severity) {
	   	 FacesMessage msg = new FacesMessage(severity, "", message);
	     FacesContext.getCurrentInstance().addMessage(null, msg);
	     RequestContext.getCurrentInstance().update(("form:msgsSticky"));
	}
	
	public void guiTableRefresh() {
		FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:myResources");
		RequestContext.getCurrentInstance().reset("form:myResources");
	}
	
	
	public void onContextBackup(SelectEvent event) {
	}
	
	public void onContextMenu(SelectEvent event) {
			
			try {
				//System.out.println(event.getSource());
				//System.out.println(event.getComponent());

				Utils.debug("Context menu !");
				if (event.getObject() == null) {
					Utils.debug("onContextMenu call with null object !");
					showError("onContextMenu call with null object !");
					return;
				}
				Utils.debug(event.getObject().getClass().getName());
				ResourceInfo x = (ResourceInfo) event.getObject();
				Utils.debug(x.getDataURL());
				
				guiSelectedResource = x;
				
				initMetadataView(x.getResourceMetadata());
				
				String eventType = FacesContext.getCurrentInstance()
				        .getExternalContext()
				        .getRequestParameterMap()
				        .get("javax.faces.behavior.event");
				

				// open metadata by double-click
				if (eventType.equals("rowDblselect")) {
					editMetadataOpen();
				}

				// set values in gui
				FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:a4715m");
				RequestContext.getCurrentInstance().reset("form:a4715m");

				} catch (Exception e){e.printStackTrace();}
			
			readPermissions();
			
			//FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:resourceOptions");
			//RequestContext.getCurrentInstance().reset("form:resourceOptions");
			
			//FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:deselectResource");
			//RequestContext.getCurrentInstance().reset("form:deselectResource");
			
			}
	
	
	/**
	 * @param linghubAttributes
	 */
	private void initMetadataView(ResourceMetadata md) {
		
		// init meta-data view
		setMetaFormat(new String(md.getFormat()));
		setMetaTitle(new String(md.getTitle()));
		setMetaUbTitle(new String(md.getUbTitle()));
		setMetaType(new String(md.getType()));
		setMetaRights(new String(md.getRights()));
		setMetaLicense(new String(md.getLicense()));
		setMetaDctSource(new String(md.getDctSource()));
		setMetaDctIdentifier(new String(md.getDctIdentifier()));
		setMetaPublisher(new String(md.getPublisher()));
		setMetaDescription(new String(md.getDescription()));
		setMetaCreator(new String(md.getCreator()));
		setMetaContributor(new String(md.getContributor()));
		setMetaLocation(new String(md.getLocation()));
		setMetaYear(md.getYear());
		setMetaContact(md.getEmailContact());
		setMetaWebpage(md.getWebpage());
		setMetaDate(md.getDate());
		setMetaDctLanguageString(new String(md.getDctLanguageString()));
		setMetaDcLanguageString(new String(md.getDcLanguageString()));
		setMetaSubject(new String(md.getKeywords()));
		setMetaSource(new String(md.getMetadataSource().name()));
		if (md.getMetadataSource() == MetadataSource.DATAFILE) {
			setMetaUrl("");
		} else {
			setMetaUrl(new String(guiSelectedResource.getMetaDataURL()));
		}
		
	}



	public void readResourceTypes() {
		
		
		if (guiSelectedResource.getResourceTypeInfos() != null && 
			!guiSelectedResource.getResourceTypeInfos().isEmpty()) {
			//System.out.println("hello "+guiSelectedResource.getResourceTypeInfos().get(0).getResourceType());

			selectedResourceType = guiSelectedResource.getResourceTypeInfos().get(0).getResourceType();
			//System.out.println("hello "+selectedResourceType.name());
			selectedResourceTypeSpecifier = guiSelectedResource.getResourceTypeInfos().get(0).getTypeSpecifier();
		
		} else {
			selectedResourceType = ResourceType.UNKNOWN;
			selectedResourceTypeSpecifier = "";
			//System.out.println("hello UNKNOWN");
		}
		
		// set values in gui
		//FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:resourceTypeClassification");
		//RequestContext.getCurrentInstance().reset("form:resourceTypeClassification");
	}
	
	
	public void addToWorkspace() {
		
		if (guiSelectedResource.getUserID().equals(myUserID)) {
			showInfo("Resource is already in workspace !");
			return;
		}
		
		selectResource();
		showInfo("Resource was sucessfully added to your workspace !");
	}
	
	
	public String deleteResource () {
		
		Utils.debug("deleteResource");
		if (guiSelectedResource == null) {
			Utils.debug("guiSelectedResource == null");
			return "login?faces-redirect=true";
		}
		
		// FINISHED resources selected by other users (than the resource owner)
		if (userAccount.getAccountType() != AccountType.ADMIN) {
			if (resourceManager.isResourceSelectedByUser(guiSelectedResource.getDataURL(), myUserID)) {
				return deselectResource();
			}
		}
		
		if (editManager.isLocked(guiSelectedResource.getDataURL())){
			showInfo("Can not delete resource "+guiSelectedResource.getDataURL()+", because it is currently edited by another user !");
			return "login";
		}
		
		
		HashSet<String> resources = new HashSet<String>();
		
		// In the following only cases were a owner tries to delete one of his resources are considered !
		
		for (ResourceInfo rs : rfl) {
			if (rs.getDataURL().equals(guiSelectedResource.getDataURL())) {
				
				ResourceProcessState pt = rs.getResourceProcessState();
				
				System.out.println(rs.getDataURL()+" : "+pt);
				
				switch (pt) {
				
				case FINISHED :
					
					System.out.println("userID : "+guiSelectedResource.getUserID());
					// delete resource from regDB
					if (!guiSelectedResource.getUserID().equals(myUserID)) {
						//Utils.debug("Cannot delete resource because not owner !");
						//showError("Cannot delete resource because not owner !");
						//return "";
						//deselectResource();
					}
					
					Utils.debug("deleting resource");
					
					resources.clear();
			    	resources.add(guiSelectedResource.getDataURL());
			    	deleteResourceImpl(resources);
			    	
			    	// delete cached version
			    	if(fidConfig.getBoolean("RunParameter.cached")) {
			    		ExecutionBean.getResourceCache().deleteResource(guiSelectedResource.getDataURL());
			    	}
			    	
			    	Utils.debug("Deleted resource "+guiSelectedResource.getDataURL()+" !");
			    	//showInfo("Deleted !");
			    	refreshAction();
			    	return "login?faces-redirect=true";
					
				
				case SEARCH :
					
					showInfo ("INFO : can only delete resources with status WAITING or FINISHED !");
					return "";

					
					/*if (!canDelete) {// ok
						// resource must be in progress as defined in method readPermissions
						showInfo ("Info : can not delete a resource that is actively processed !");
						return "";
					}
					
					System.out.println("userID : "+guiSelectedResource.getUserID());
					// delete resource from regDB
					if (!guiSelectedResource.getUserID().equals(myUserID)) {
						Utils.debug("Cannot delete resource because not owner !");
						showError("Cannot delete resource because not owner !");
						return "";
					}
					
					Utils.debug("deleting resource");
					
					resources.clear();
			    	resources.add(guiSelectedResource.getDataURL());
			    	deleteResourceImpl(resources);
			    	
			    	Utils.debug("Deleted resource "+guiSelectedResource.getDataURL()+" !");
			    	//showInfo("Deleted !");
			    	refreshAction();
			    	return "login?faces-redirect=true";*/
			    	
					
				case WAITING :
					// remove resource from queue
					boolean success = messageBrowser.deQueueResource(myUserID, guiSelectedResource.getDataURL());
					if (!success) {
						Utils.debug("Deleting queue item "+guiSelectedResource.getMessageID()+" not successful");
						showError("Deleting queue item "+guiSelectedResource.getMessageID()+" not successful");
					} else {
						//showInfo("Deleted !");
					}
					
					refreshAction();
					return "login?faces-redirect=true";
				
				case INPROGRESS : // only seen by owner !
					// stop worker process
					showInfo ("INFO : can not delete a resource that is actively processed !");
					break;
					
				default :
					Utils.debug("Error deleteResource : "+"invalid state "+pt);
					break;
				}
			}
		}
		
		return "login?faces-redirect=true";
	}
	
	
	private void deleteResourceImpl(Set<String> set) {
    	
		System.out.println("deleteResourceImpl");
    	// Delete resources from database
    	for (String dataURL : set) {
    		resourceManager.deleteResource(dataURL);
    	}
    	
    	/*// Delete resources from table
    	Iterator <ResourceInfo> iterator = rfl.iterator();
    	while (iterator.hasNext()) {
    		ResourceInfo resourceInfo = iterator.next();
    		if (set.contains(resourceInfo.getDataURL())) {
    			iterator.remove();
    		}
    	}	*/
	}


	public ResourceInfo getGuiSelectedResource() {
		return guiSelectedResource;
	}


	public void setGuiSelectedResource(ResourceInfo guiSelectedResource) {
		this.guiSelectedResource = guiSelectedResource;
	}


	public boolean isPermOtherRead() {
		return permOtherRead;
	}


	public void setPermOtherRead(boolean permOtherRead) {
		this.permOtherRead = permOtherRead;
	}


	public boolean isPermOtherEdit() {
		return permOtherEdit;
	}


	public void setPermOtherEdit(boolean permOtherEdit) {
		this.permOtherEdit = permOtherEdit;
	}


	public boolean isPermOtherExport() {
		return permOtherExport;
	}


	public void setPermOtherExport(boolean permOtherExport) {
		this.permOtherExport = permOtherExport;
	}
	
	public void savePermissions() {
		
		if (!resourceManager.getResourceOwner(guiSelectedResource.getDataURL()).equals(myUserID)) {
			showError("Can not change privileges of foreign resource !");
			return;
		}
		
		Utils.debug("savePermissions for resource "+guiSelectedResource.getDataURL());
		
		int myPermissions = 7;
		int groupPermissions = 0;
		int otherPermissions = 0;
		if (permOtherRead) otherPermissions+=1;
		if (permOtherEdit) otherPermissions+=2;
		if (permOtherExport) otherPermissions+=4;
		
		// Special rule sets read permissions by default if edit permissions are selected
		if (permOtherEdit && !permOtherRead) {
			otherPermissions+=1;
		}

		String permissionString = ""+myPermissions+groupPermissions+otherPermissions;
		// Save permissions to regDB
		boolean success = resourceManager.setResourcePermissions(guiSelectedResource.getDataURL(), myUserID, permissionString);
		if (success) {
			showInfo("Permissions updated !");
		}
	}
	
	
	public void readPermissions() {
		
		Utils.debug("readPermissions for resource "+guiSelectedResource.getDataURL());
		
		permOtherRead=false;
		permOtherEdit=false;
		permOtherExport=false;
		canDelete=false;
		canAdd=false;
		
		
		// get permissions from regDB
		String permissions = resourceManager.getResourcePermissions(guiSelectedResource.getDataURL());
		System.out.println("resource permissions : "+permissions);
		
		// Error
		if (permissions.isEmpty()) return;
		
		int otherPermissions = Integer.parseInt(permissions.substring(2, 3));
		if ((otherPermissions & 1) == 1) permOtherRead = true;
		if ((otherPermissions & 2) == 2) permOtherEdit = true;
		if ((otherPermissions & 4) == 4) permOtherExport = true;


		// get edit possible from resource state
		switch(guiSelectedResource.getResourceProcessState()) {
		
			case FINISHED :
			case SEARCH :
				canEdit = true;
				break;
				
			case STARTED :
			case INPROGRESS :
			case WAITING :
			case ARCHIVE_FINISHED :
				canEdit = false;
				
			default :
				canEdit = false;
				
		}
		
		if (guiSelectedResource.getUserID().equals(myUserID) ||
			userAccount.getAccountType() == AccountType.ADMIN) {
			
			canRead=true;
			canExport=true;
			canDelete=true; 
			switch (guiSelectedResource.getResourceProcessState()) {
			case WAITING :
				deleteMessage="Remove resource from process queue ?";
				break;
			case FINISHED :
				deleteMessage="Delete resource from database ???";
				break;
			default :
				deleteMessage="";// can not delete resources with other states
			}
			// canEdit see above
		} else {
			canRead=permOtherRead;
			canEdit=(canEdit && permOtherEdit); // canEdit from above + rights
			// Override export permission and automatically allow export of approved resources
			canExport=guiSelectedResource.isApproved();
			//canExport=permOtherExport;
			canDelete=true; deleteMessage="Remove resource from workspace ?";
		}
		
		// Maximum restrictions on resources being actively processed
		if (ExecutionBean.getPublicExecuter()
							.getActiveResourceUrls().contains(guiSelectedResource.getDataURL())){
			canRead=false;
			canEdit=false;
			canExport=false;
			canDelete=false;
		};
		
		if (guiSelectedResource.getResourceProcessState() == ResourceProcessState.SEARCH) {
			canDelete=false;
			
			if (!guiSelectedResource.getUserID().equals(myUserID) &&
				!resourceManager.isResourceSelectedByUser(guiSelectedResource.getDataURL(), myUserID)) {
				canAdd = true;
			} else {
				canAdd = false;
			}
		}
		
		
		System.out.println("permissions :");
		System.out.println("canRead : "+canRead);
		System.out.println("canEdit : "+canEdit);
		System.out.println("canExport : "+canExport);
		System.out.println("canDelete : "+canDelete);

		
		//update gui
		try {
			FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:otherpermissions");
			RequestContext.getCurrentInstance().reset("form:otherpermissions");
		} catch (Exception e){}
	}
	
	
	public String selectResource() {
		
		System.out.println("selectResource");
		
		if (guiSelectedResource == null) {
			Utils.debug("guiSelectedResource == null");
		}
		
		if (myUserID.equals(guiSelectedResource.getUserID())) return "";
		
		resourceManager.selectResource(myUserID, guiSelectedResource.getDataURL());
		
		
		return "login?faces-redirect=true";
	}
	
	
	public String deselectResource() {
		
		System.out.println("deselectResource");
		
		if (guiSelectedResource == null) {
			Utils.debug("guiSelectedResource == null");
		}
		
		if (myUserID.equals(guiSelectedResource.getUserID())) return "";
		
		resourceManager.deselectResource(myUserID, guiSelectedResource.getDataURL());
		
		setInfoText("INFO : resource "+guiSelectedResource.getDataURL()+" was removed from your workspace !");
		refreshAction();		
		return "login?faces-redirect=true";
	}


	public String getSearchTextField() {
		return searchTextField;
	}


	public void setSearchTextField(String searchTextField) {
		this.searchTextField = searchTextField;
	}


	public HttpSession getSession() {
		return session;
	}


	public void setSession(HttpSession session) {
		this.session = session;
	}


	public String getMyUserID() {
		return myUserID;
	}


	public String getInfobarText() {
		return infobarText;
	}


	public void setInfobarText(String infobarText) {
		this.infobarText = infobarText;
	}


	public boolean isAutoSelectSearchResults() {
		return autoSelectSearchResults;
	}


	public void setAutoSelectSearchResults(boolean autoSelectSearchResults) {
		this.autoSelectSearchResults = autoSelectSearchResults;
	}


	public String getLanguageSearch() {
		return languageSearch;
	}


	public void setLanguageSearch(String languageSearch) {
		this.languageSearch = languageSearch;
	}
	
	public List <ModelType> getModels() {
		return executer.getModelDefinition().getSpecialModels();

		//List<ModelType> models = (ArrayList<ModelType>) Arrays.asList(ModelType.values()); // removing fails

		/*ModelType[] y = ModelType.values();
		List<ModelType> models = new ArrayList<ModelType>();
		
		for (ModelType m : y) {
			models.add(m);
		}
		// remove irrelevant models
		models.remove(ModelType.valueOf("UD2DEP"));
		models.remove(ModelType.valueOf("UD2FEAT"));
		models.remove(ModelType.valueOf("UD2POS"));
		models.remove(ModelType.valueOf("UDEP"));
		models.remove(ModelType.valueOf("BLL"));
		//models.remove(ModelType.valueOf("UNKNOWN"));*/
		//List<ModelType> models = ;
		//return models;
	}
	
	
	public List <ResourceType> getResourceTypes() {
		
		return (List<ResourceType>) Arrays.asList(ResourceType.values());
	}
	
	public List <ResourceTypeSpecifier> getResourceTypeSpecifiers() {
		
		return (List<ResourceTypeSpecifier>) Arrays.asList(ResourceTypeSpecifier.values());
	}
	
	
	


	public String getResourceNameSearch() {
		return resourceNameSearch;
	}


	public void setResourceNameSearch(String resourceNameSearch) {
		this.resourceNameSearch = resourceNameSearch;
	}
	
	public String editResource() {
		
		if (fidConfig.getBoolean("RunParameter.cached") && 
			!ExecutionBean.getResourceCache().getResourceMap().containsKey(guiSelectedResource.getDataURL())) {
			showInfo("Sorry, the resource "+guiSelectedResource.getDataURL()+" was deleted"
					+ "by its owner in the meantime !");
			//showInfo("Sorry, the resource "+guiSelectedResource.getDataURL()+" produced no results, or was deleted"
			//		+ "by the owner !");
			return "login";
		}

		
		if (editManager.getResourceEditLock(guiSelectedResource.getDataURL(), userAccount)) {
		//if (editManager.getResourceEditLock(guiSelectedResource.getDataURL(), myUserID)) {
	
			//setEditMode(guiSelectedResource.getDataURL());
			
			// auto-select resource from search
			if (guiSelectedResource.getResourceProcessState() == ResourceProcessState.SEARCH) {
				
				// additionally check if the resource is INPROGRESS
				HashSet<String> activeResourceUrls = executionBean.getPublicExecuter().getActiveResourceUrls();
				if (activeResourceUrls.contains(guiSelectedResource.getDataURL())) {
					editManager.clearResourceEditLock(guiSelectedResource.getDataURL());
					showInfo("Sorry, the resource "+guiSelectedResource.getDataURL()+" is currently processed !");
					return "login";	
				}
				
				resourceManager.selectResource(myUserID, guiSelectedResource.getDataURL());
			}
			try {
				FacesContext.getCurrentInstance().getExternalContext().redirect("login-resources.xhtml");
			    FacesContext.getCurrentInstance().responseComplete();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return ""; 
		} else {
			showInfo("Sorry, the resource "+guiSelectedResource.getDataURL()+" is currently edited by another user !");
			return "login";
			
		}
	}
	
	
	public String closeEdit(){
		editManager.clearResourceEditLock(guiSelectedResource.getDataURL());
		//clearEditMode(guiSelectedResource.getDataURL());
		
		// update approved state of edited resource
		if(fidConfig.getBoolean("RunParameter.cached")) {
			HashSet<ProcessState> states = new HashSet<ProcessState>();
			for (ResourceInfo resourceFile : ExecutionBean.getResourceCache().getResourceFileMap().get(guiSelectedResource.getDataURL())) {
				states.add(resourceFile.getFileInfo().getProcessState());
			}
			
			ExecutionBean.getResourceCache().getResourceMap().
			get(guiSelectedResource.getDataURL()).setApproved(ResourceInfo.computeApproved(states));
		}
		
		// reload list in case the resource was deleted
		refreshAction();
		return "login?faces-redirect=true";
	}
	

	/*private boolean isEditedRightNow(String resourceIdentifier) {
		return editedResources.contains(resourceIdentifier);
	}*/
	
	/*private void setEditMode(String resourceIdentifier) {
		editedResources.add(resourceIdentifier);
	}*/
	
	/*private void clearEditMode(String resourceIdentifier) {
		editedResources.remove(resourceIdentifier);
	}*/


	public String getFindDataAndOR1() {
		return findDataAndOR1;
	}


	public void setFindDataAndOR1(String findDataAndOR1) {
		this.findDataAndOR1 = findDataAndOR1;
	}


	public String getFindDataAndOR2() {
		return findDataAndOR2;
	}


	public void setFindDataAndOR2(String findDataAndOR2) {
		this.findDataAndOR2 = findDataAndOR2;
	}

	
	public String findData() {
		
		ArrayList<Vertex> rwm = new ArrayList<Vertex>();
		ArrayList<Vertex> rwl = new ArrayList<Vertex>();
		ArrayList<Vertex> rwn = new ArrayList<Vertex>();
		
		boolean searchModel=false;
		boolean searchLanguage=false;
		boolean searchResourceName=false;
		
		Set<String> models = new HashSet<String>();
		Set<String> languages= new HashSet<String>();
		
		
		Utils.debug("modelSearch : "+modelSearch);
		Utils.debug("languageSearch : "+languageSearch);
		Utils.debug("findFilterByType : "+findFilterByType);
		Utils.debug("resourceNameSearch : "+resourceNameSearch);

				
		// Check empty query
		if (modelSearch.isEmpty() 				&& 
			resourceNameSearch.trim().isEmpty() &&
			languageSearch.trim().isEmpty()		&&
			findFilterByType.isEmpty()
			) {
			showError("Search parameter required, please set : annotation model | language | type | name !");
			return "";
		}
		
		// Check resource name input
		if (resourceNameSearch.contains("*") || resourceNameSearch.contains(".*")) {
			showError("Input error : no wildcards in resource name allowed !");
			return "";
		}
		
		
		// Check language search field input
		if (!languageSearch.trim().isEmpty()) {
			
			String[] tmp = languageSearch.split("[,; ]");
			String langerr="";
			for (String x : tmp) {
				x = x.trim();
				if (!x.isEmpty()) {
					if (x.length() != 3) {langerr=x;break;};
					// test non character value
				}
			}
			if (!langerr.isEmpty()) {
				showError("Input error : '"+langerr+"' not a valid ISO639-3 language code !");
			return "";
			}
		}
		
		
		// 1. query resource format
		//ResourceType resourceType = ResourceType.CORPUS;
		//ArrayList<Vertex> rwt = resourceManager.resourcesWithType(resourceType);
		// not directly available by query because not stored in DB
		
		// 2. query annotation model
		if (!modelSearch.isEmpty()) {
			
			searchModel = true;
			
			//models.add(findSelectedModel);
			boolean andTrueOrFalse=false;
			if (findDataAndOR1.equals("and")) andTrueOrFalse = true;
			for (String model : modelSearch.split(",")){
				models.add(model.trim());
			}
			Utils.debug("rwm start");
			rwm = resourceManager.resourcesWithModels(models, andTrueOrFalse, findExclusiveModels);
			Utils.debug("rwm stop");

		}
		
		
		// 3. query language
		if (!languageSearch.trim().isEmpty()) {
			
			searchLanguage=true;
			
			String[] tmp = languageSearch.split("[,; ]");
			for (String lang : tmp) {
				lang = lang.trim();
				if (lang.isEmpty()) continue;
				URL lexvoUrl = TikaTools.getLexvoUrlFromISO639_3Code(lang);
				if (lexvoUrl != null) {
					languages.add(lexvoUrl.toString());
				}
			}
			System.out.println(languages.size());
			
			boolean andTrueOrFalse=false;
			if (findDataAndOR2.equals("and")) andTrueOrFalse = true;
			Utils.debug("rwl start");
			rwl = resourceManager.resourcesWithLanguages(languages, andTrueOrFalse, findExclusiveLanguages);
			Utils.debug("rwl stop");
		}
		
		
		// 4. query resource name
		if (!resourceNameSearch.trim().isEmpty()) {
			
			searchResourceName=true;
			String resourceName = ".*"+resourceNameSearch+".*";
			Utils.debug("rwn stop");
			rwn = resourceManager.resourcesWithNameLike(resourceName, resourceNameSearchIgnoreCase);
			Utils.debug("rwn stop");
		}
		
		
		
		// Merge results of individual queries with AND
		HashSet<String> modelResourceUrls = new HashSet<String>();	
		for (Vertex v_m : rwm) {
			modelResourceUrls.add((String) v_m.value(ResourceManager.ResourceUrl)); 		
		}
		HashSet<String> languageResourceUrls = new HashSet<String>();	
		for (Vertex v_l : rwl) {
			languageResourceUrls.add((String) v_l.value(ResourceManager.ResourceUrl)); 		
		}
		HashSet<String> resourceNameResourceUrls = new HashSet<String>();	
		for (Vertex v_n : rwn) {
			resourceNameResourceUrls.add((String) v_n.value(ResourceManager.ResourceUrl)); 			
		}
		
		HashSet<String> intersection = new HashSet<String>();
		
		// All 7 combinations for searching
		// one search
		if (searchModel && !searchLanguage && !searchResourceName) {
			intersection=modelResourceUrls;
		}
		if (!searchModel && searchLanguage && !searchResourceName) {
			intersection=languageResourceUrls;
		}
		if (!searchModel && !searchLanguage && searchResourceName) {
			intersection=resourceNameResourceUrls;
		}
		
		// two searches
		if (searchModel && searchLanguage && !searchResourceName) {
			modelResourceUrls.retainAll(languageResourceUrls);
			intersection=modelResourceUrls;
		}
		if (searchModel && !searchLanguage && searchResourceName) {
			modelResourceUrls.retainAll(resourceNameResourceUrls);
			intersection=modelResourceUrls;
		}
		if (!searchModel && searchLanguage && searchResourceName) {
			languageResourceUrls.retainAll(resourceNameResourceUrls);
			intersection=languageResourceUrls;
		}
		
		// three searches
		if (searchModel && searchLanguage && searchResourceName) {
			modelResourceUrls.retainAll(languageResourceUrls);
			modelResourceUrls.retainAll(resourceNameResourceUrls);
			intersection=modelResourceUrls;
		}
		
		
		// Get the vertex set for the URLs in the intersection
		ArrayList<Vertex> mergedResources = new ArrayList<Vertex>();
		for (Vertex v : rwm) {
			if (intersection.contains(
					v.value((String) ResourceManager.ResourceUrl))) {
				mergedResources.add(v);
				intersection.remove(v.value((String) ResourceManager.ResourceUrl));
			}
		}
		for (Vertex v : rwl) {
			if (intersection.contains(
					v.value((String) ResourceManager.ResourceUrl))) {
				mergedResources.add(v);
				intersection.remove(v.value((String) ResourceManager.ResourceUrl));
			}
		}
		for (Vertex v : rwn) {
			if (intersection.contains(
					v.value((String) ResourceManager.ResourceUrl))) {
				mergedResources.add(v);
				intersection.remove(v.value((String) ResourceManager.ResourceUrl));
			}
		}
		
		searchResourceResultList = ExecutionBean.getResourceCache().getCachedResourcesByUrl(mergedResources);

		
//		if(!fidConfig.getBoolean("RunParameter.cached")) {
//			// Finally get the resource data for resources in the query result 
//			searchResourceResultList = resourceManager.getDoneResourcesRI(mergedResources, false);
//			System.out.println("hello");
//		
//		} else {
//			searchResourceResultList = ExecutionBean.getResourceCache().getCachedResourcesByUrl(mergedResources);			
//		}
		
		
		// Filter by resource type
		if (!findFilterByType.isEmpty()) {
			
			if (!searchModel && !searchLanguage && !searchResourceName) {
				Utils.debug("findFilterByType start");
				ArrayList<Vertex> resourcesWithType = 
						resourceManager.getResourcesWithType(ResourceType.valueOf(findFilterByType));
				Utils.debug("findFilterByType stop");
				
				searchResourceResultList = ExecutionBean.getResourceCache().getCachedResourcesByUrl(resourcesWithType);

//				if(!fidConfig.getBoolean("RunParameter.cached")) {
//					searchResourceResultList = resourceManager.getDoneResourcesRI(resourcesWithType, false);
//				} else {
//					searchResourceResultList = ExecutionBean.getResourceCache().getCachedResourcesByUrl(resourcesWithType);
//				}
			}
			else {
				Iterator<ResourceInfo> iterator = searchResourceResultList.iterator();
				boolean found = false;
				while (iterator.hasNext()) {
					ResourceInfo rs = iterator.next();
					found=false;
					for (ResourceTypeInfo rti : rs.getResourceTypeInfos()) {
						if (findFilterByType.equals(rti.getResourceType().name())) {
							found=true;break;
						}
					}
					if (!found) iterator.remove();
				}
			}
		}
		
		// add results to view via select (temporary ?)
		if (autoSelectSearchResults) {
			for (ResourceInfo resourceInfo : searchResourceResultList) {
				resourceManager.selectResource(myUserID, resourceInfo.getDataURL());
			}
		}
		
		
		searchInfobarText="Found "+searchResourceResultList.size();
		if (!findFilterByType.isEmpty()) {
			
			switch(findFilterByType) {
			case "CORPUS" : 
				if (searchResourceResultList.size() == 1) {
					searchInfobarText+=" corpus";
				} else {
					searchInfobarText+=" corpora";

				}break;
			case "ONTOLOGY" :
				if (searchResourceResultList.size() == 1) {
					searchInfobarText+=" ontology";
				} else {
					searchInfobarText+=" ontologies";

				}break;
				
			case "LEXICON" : 
				if (searchResourceResultList.size() == 1) {
					searchInfobarText+=" lexicon";
				} else {
					searchInfobarText+=" lexica";

				}break;
				
			case "UNKNOWN" : 
				if (searchResourceResultList.size() == 1) {
					searchInfobarText+=" resource with unknown type";
				} else {
					searchInfobarText+=" resources with unknown type";
				}break;
				
			case "WORDNET" : 
				if (searchResourceResultList.size() == 1) {
					searchInfobarText+=" wordnet";
				} else {
					searchInfobarText+=" wordnets";
				}break;
				
			default : break;
			}
			
		} else {
			if (mergedResources.size()==1) {
				searchInfobarText+= " resource";
			} else {
				searchInfobarText+= " resources";
			}
		}
		
		if (!modelSearch.isEmpty()) {
			if (models.size() == 1) {
				searchInfobarText+=" that "+((mergedResources.size() == 1) ? "uses" :"use")+" annotation model "+models.iterator().next();
			} else {
				searchInfobarText+=" that "+((mergedResources.size() == 1) ? "uses" :"use")+" annotation models ";
				ArrayList<String> tmp = new ArrayList<String>(models);
				int tmpn = tmp.size();
				int i=0;
				while (i < tmpn-1) {
					searchInfobarText+=tmp.get(i)+",";
				i++;
				}
				searchInfobarText = searchInfobarText.substring(0,searchInfobarText.length()-1);
				searchInfobarText+=" "+findDataAndOR1+" "+tmp.get(i);
			}
		} 
		
	
		if (!languageSearch.isEmpty()) {
			if (languages.size() == 1) {
				String lang=languages.iterator().next();
				lang=lang.substring(lang.length()-3,lang.length());
				searchInfobarText+=" with language "+lang;
			} else {
				searchInfobarText+=" with languages ";
				ArrayList<String> tmp = new ArrayList<String>(languages);
				int tmpn = tmp.size();
				int i=0;
				String lang="";
				while (i < tmpn-1) {
					lang=tmp.get(i);
					lang=lang.substring(lang.length()-3,lang.length());
					searchInfobarText+=lang+",";
				i++;
				}
				searchInfobarText = searchInfobarText.substring(0,searchInfobarText.length()-1);
				lang=tmp.get(i);
				lang=lang.substring(lang.length()-3,lang.length());
				searchInfobarText+=" "+findDataAndOR2+" "+lang;
			}
		}
		
		if (!resourceNameSearch.trim().isEmpty()) {
			String filler = "";
			if (!languageSearch.isEmpty()) filler = " and";
			searchInfobarText+=filler+" with name like '"+resourceNameSearch.trim()+"'";	
		}
		
		refreshAction();
		return "login?faces-redirect=true";
	}
	
	
	
	public String searchByMetadata() {
		
		metadataSearch=metadataSearch.trim();
		if (metadataSearch.isEmpty()) return "";
		
		//ArrayList<Vertex> rwmd = resourceManager.getResourcesWithMetadataLike(metadataSearch, metadataSearchIgnoreCase);
		//searchResourceResultList = ExecutionBean.getResourceCache().getCachedResourcesByUrl(rwmd);
		
		
		// Alternative implementation with Cache can handle case in hex-encoded data
		List<String> matchingUrls = new ArrayList<String>();
		String ignoreCaseQuery = "(?i)";
		if (!metadataSearchIgnoreCase){
			ignoreCaseQuery="";
		};
		
		String queryString = metadataSearch.replaceAll("[*',;\\(\\)\\{\\}\\[\\]\\\"]", "");
		queryString = ignoreCaseQuery+".*"+queryString+".*";
		
		for (ResourceInfo ri : ExecutionBean.getResourceCache().getResourceMap().values()) {
			if(ri.getResourceMetadata().asRawText().matches(queryString)) {
				matchingUrls.add(ri.getDataURL());
			}
		}
		searchResourceResultList = ExecutionBean.getResourceCache().getCachedResourcesByUrl(matchingUrls);

	
		if(searchResourceResultList.isEmpty()) {
			searchInfobarText="No resources found with meta-data like : "+metadataSearch;
		}
		
		if(searchResourceResultList.size() == 1) {
			searchInfobarText="Found 1 resource with meta-data like : "+metadataSearch;
		}
		
		if(searchResourceResultList.size() > 1) {
			searchInfobarText="Found "+searchResourceResultList.size()+" resources with "
					+ "meta-data like : "+metadataSearch;
		}
		
		noSearchByMetadata();
		refreshAction();
		return "login?faces-redirect=true";
	}
	
	
	public void noSearchByMetadata() {
		metadataSearch="";
	}
	
	
	public String searchByAnnotation() {
		
		searchResourceResultList.clear();
		
		annotationSearch = annotationSearch.trim();

		boolean isAnnotation = false;
		boolean isAnnotationClass = false;
		String searchText = "";
		
		if (annotationSearch.startsWith("http")) {
			isAnnotationClass = true;
			searchText = "annotation class";
		} else {
			isAnnotation = true;
			searchText = "annotation";
		}
		
		/*System.out.println("annotations "+ExecutionBean.getResourceCache().getAnnotationResourceMap().size());
		for (String y : ExecutionBean.getResourceCache().getAnnotationResourceMap().keySet()) {
			System.out.println(y+"+ "+ExecutionBean.getResourceCache().getAnnotationResourceMap().get(y));
		}
		System.out.println("annotation classes "+ExecutionBean.getResourceCache().getAnnotationClassResourceMap().size());
		for (String y : ExecutionBean.getResourceCache().getAnnotationClassResourceMap().keySet()) {
			System.out.println(y+"- "+ExecutionBean.getResourceCache().getAnnotationClassResourceMap().get(y));
		}*/
		
		if (isAnnotation) {
			
			//if (ExecutionBean.getResourceCache().getAnnotationResourceMap().containsKey(annotationSearch)) {
			
			annotationSearchIgnoreCase=false;
			
			// Get resources with that annotation 
			if (!annotationSearchIgnoreCase) {
				
				// Check annotation cache if annotation exists
				if (!ExecutionBean.getPublicExecuter().getWriter().getAnnotationCache().
						getTagDefinitions().contains(annotationSearch)) {
					showInfo("The annotation '"+annotationSearch+"' is unknown !");
					return "";
				}
				
				if (ExecutionBean.getResourceCache().getAnnotationResourceMap().containsKey(annotationSearch)) {

					searchResourceResultList = ExecutionBean.getResourceCache().getCachedResourcesByUrl(
							ExecutionBean.getResourceCache().getAnnotationResourceMap().get(annotationSearch));
				}
				} else {
					
					String searchedAnnotation = annotationSearch.toLowerCase();
					HashSet<String> resourceUrls = new HashSet<String>();
					for (String existingAnnotation : ExecutionBean.getResourceCache().getAnnotationResourceMap().keySet()) {
						if (existingAnnotation.toLowerCase().equals(searchedAnnotation)) {
							resourceUrls.addAll(ExecutionBean.getResourceCache().
								getAnnotationResourceMap().get(existingAnnotation));
						}
					}
					searchResourceResultList = ExecutionBean.
							getResourceCache().getCachedResourcesByUrl(resourceUrls);
				}
			//}
		}
			
		if (isAnnotationClass) {
		
			if (!annotationSearchIgnoreCase) {
				
				if (!ExecutionBean.getPublicExecuter().getWriter().getAnnotationCache().getClassDefinitions()
						.contains(annotationSearch)) {
					showInfo("The annotation class '"+annotationSearch+"' is unknown !");
					return "";
				}
			
				if (ExecutionBean.getResourceCache().getAnnotationClassResourceMap().containsKey(annotationSearch)) {
					// Get resources with that annotation 
					searchResourceResultList = ExecutionBean.getResourceCache().getCachedResourcesByUrl(
							ExecutionBean.getResourceCache().getAnnotationClassResourceMap().get(annotationSearch));
				}
				} else {
					String searchedAnnotation = annotationSearch.toLowerCase();
					HashSet<String> resourceUrls = new HashSet<String>();
					for (String existingAnnotation : ExecutionBean.getResourceCache().getAnnotationClassResourceMap().keySet()) {
						if (existingAnnotation.toLowerCase().equals(searchedAnnotation)) {
							resourceUrls.addAll(ExecutionBean.getResourceCache().
								getAnnotationClassResourceMap().get(existingAnnotation));
						}
					}
					searchResourceResultList = ExecutionBean.
							getResourceCache().getCachedResourcesByUrl(resourceUrls);
			}
		}
		
		
		if(searchResourceResultList.isEmpty()) {
			showInfo("No resources with annotation '"+annotationSearch+"' were found!");
			searchInfobarText="No resource found that contains "+searchText+" : "+annotationSearch;
			return "";
		}
		
		if(searchResourceResultList.size() == 1) {
			searchInfobarText="Found 1 resource that contains "+searchText+" : "+annotationSearch;
		}
		
		if(searchResourceResultList.size() > 1) {
			searchInfobarText="Found "+searchResourceResultList.size()+ " resources that "
					+ "contain "+searchText+" : "+annotationSearch;
		}
		
		
		noSearchByAnnotation();
		refreshAction();
		return "login?faces-redirect=true";
	}
	
	
	public void noSearchByAnnotation() {
		annotationSearch = "";
	}
	
	
	public String getOliaSearchClass() {
		if (oliaSearch.trim().isEmpty()) return "";
		else
		return oliaSearch.split(" ")[0];
	}
	
	public String searchByOlia() {
		
		if(oliaSearch.isEmpty()) return "";
		
		searchResourceResultList.clear();
		
		String queriedOliaClass = getOliaSearchClass();
		
		if (ExecutionBean.getResourceCache().getOliaResourceMap().containsKey(queriedOliaClass)) {
			searchResourceResultList = ExecutionBean.getResourceCache().getCachedResourcesByUrl(
					ExecutionBean.getResourceCache().getOliaResourceMap().get(queriedOliaClass));
		}
		
		if(searchResourceResultList.isEmpty()) {
			searchInfobarText="No resource found that contains OLiA : "+queriedOliaClass;
		}
		
		if(searchResourceResultList.size() == 1) {
			searchInfobarText="Found 1 resource that contains OLiA : "+queriedOliaClass;
		}
		
		if(searchResourceResultList.size() > 1) {
			searchInfobarText="Found "+searchResourceResultList.size()+ " resources that "
					+ "contain OLiA : "+queriedOliaClass;
		}
		
				
		noSearchByOlia();
		refreshAction();
		return "login?faces-redirect=true";
	}
	
	
	public String searchByComment() {
		
		commentSearch=commentSearch.trim();
		if (commentSearch.isEmpty()) return "";
		
		//ArrayList<Vertex> rwmd = resourceManager.getResourcesWithCommentLike(commentSearch, commentSearchIgnoreCase);
		//searchResourceResultList = ExecutionBean.getResourceCache().getCachedResourcesByUrl(rwmd);
	
		
		// Alternative implementation with Cache can handle case in hex-encoded data
		List<String> matchingUrls = new ArrayList<String>();
		String ignoreCaseQuery = "(?i)";
		if (!commentSearchIgnoreCase){
			ignoreCaseQuery="";
		};
		
		String queryString = commentSearch.replaceAll("[*',;\\(\\)\\{\\}\\[\\]\\\"]", "");
		queryString = ignoreCaseQuery+".*"+queryString+".*";
		
		for (ResourceInfo ri : ExecutionBean.getResourceCache().getResourceMap().values()) {
			System.out.println(ri.getDataURL());
			System.out.println("comments : "+ri.getComments().size());
			for (Comment co : ri.getComments()) {
				if(co.asRawText().matches(queryString)) {
					matchingUrls.add(ri.getDataURL());
					break;
				}
			}
		}
		searchResourceResultList = ExecutionBean.getResourceCache().getCachedResourcesByUrl(matchingUrls);

		if(searchResourceResultList.isEmpty()) {
			searchInfobarText="No resource found that matches comment like : "+commentSearch;
		}
		
		if(searchResourceResultList.size() == 1) {
			searchInfobarText="Found 1 resource that matches comment like : "+commentSearch;
		}
		
		if(searchResourceResultList.size() > 1) {
			searchInfobarText="Found "+searchResourceResultList.size()+ " resources that "
					+ "match comment like : "+commentSearch;
		}
		
		noSearchByComment();
		refreshAction();
		return "login?faces-redirect=true";
	}
	
	
	public void noSearchByComment() {
		commentSearch="";
	}
	
	
public String searchByUser() {
		
		if (selectedUserSearch.isEmpty()) return "";
		
		ArrayList<Vertex> rwmd = resourceManager.getResourcesWithCommentLike(commentSearch, commentSearchIgnoreCase);
		searchResourceResultList = ExecutionBean.getResourceCache().getCachedResourcesByUrl(
				resourceManager.getResourcesOwnedByUserAsUrl(selectedUserSearch));
		
		if(searchResourceResultList.isEmpty()) {
			searchInfobarText="No resource found that was uploaded by user : "+selectedUserSearch;
		}
		
		if(searchResourceResultList.size() == 1) {
			searchInfobarText="Found 1 resource that was uploaded by user : "+selectedUserSearch;
		}
		
		if(searchResourceResultList.size() > 1) {
			searchInfobarText="Found "+searchResourceResultList.size()+ " resources that "
					+ "were uploaded by user '"+selectedUserSearch;
		}
		
		noSearchByUser();
		refreshAction();
		return "login?faces-redirect=true";
	}
	
	
	public void noSearchByUser() {
		selectedUserSearch="";
	}
	
	
	
	
	
	
	
	public void updateOliaClassDescription() {
		
		String queriedOliaClass = getOliaSearchClass();
		
		if(queriedOliaClass.isEmpty()) {
			oliaClassDescription = "";
		} else {
			oliaClassDescription = ExecutionBean.getMinimalOliaClasses().get(queriedOliaClass);
		}
		FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:oliaClassDescriptionArea");
		RequestContext.getCurrentInstance().reset("form:oliaClassDescriptionArea");
	}
	
	
	public void noSearchByOlia() {
		oliaSearch = "";
		oliaClassDescription="";
	}
	
	
	public void addModelSearch() {
		System.out.println("addModelSearch");
		System.out.println(findSelectedModel);
		if (modelSearch.isEmpty()) {
			modelSearch=findSelectedModel;
		} else {
			if (!modelSearch.contains(findSelectedModel)){
				modelSearch+=","+findSelectedModel;
			}
		}
	}
	
	
	public void removeModelSearch() {
		System.out.println("removeModelSearch");
		modelSearch="";
	}
	
	
	 /**
     * Save single resource to RDF (includes BLL-Concepts)
     */
	public void exportRDFSingle() {
		
		if (guiSelectedResource == null) return;
		
		ArrayList<ResourceInfo> resources = new ArrayList<ResourceInfo>();
		resources.add(guiSelectedResource);
		exportRDFJSONImpl(resources, AnnohubExportFormat.RDF, RDFExportMode.SINGLE);
	}
	
	 /**
     * Save all accepted resources to RDF (includes BLL-Concepts)
	 * @param exportMode TODO
     */
    public void exportRDF(RDFExportMode exportMode) {
    	
    	List<ResourceInfo> result = getResources2BExported(exportMode);
    	if (result == null) return;
    	exportRDFJSONImpl(result, AnnohubExportFormat.RDF, exportMode);

    }
    
    
    /**
     * Save all accepted resources to RDF (includes BLL-Concepts)
	 * @param exportMode TODO
     */
    public List<ResourceInfo> getResources2BExported(RDFExportMode exportMode) {
    	
    	Utils.debug("getResources2BExported "+exportMode);
    	
    	Collection<ResourceInfo> resources2Export = null;
    	
    	switch (exportMode) {
		
		case 	WORKSPACE :
    		resources2Export = rfl;
    		break;
    		
		case 	ALL :
    		resources2Export = ExecutionBean.getResourceCache().getResourceMap().values();
    		break;

		
		default :
			String error = "Error exportRDF : Unknown export mode "+exportMode.toString();
				Utils.debug(error);
				showError(error);
				return null;
		}
    	
    
    	if(resources2Export.isEmpty()) {
    		
    		showError("Error : No resources to export !");
    		return null;
    	}
    	
    	
		HashSet<String> activeResourceUrls = ExecutionBean.getPublicExecuter().getActiveResourceUrls();
    	
    	ArrayList<ResourceInfo> rfl2 = new ArrayList<ResourceInfo>();
    	for (ResourceInfo resourceInfo : resources2Export) {
    		
    		if (activeResourceUrls.contains(resourceInfo.getDataURL())) {
    			Utils.debug("Skipping resource in processing : "+resourceInfo.getDataURL()+ " from export !");
    			continue;
    		}
    		
    		// Skip resources that have a broken data link
    		if (resourceInfo.getResourceState() == ResourceState.ResourceUrlIsBroken &&
    			fidConfig.getBoolean("RunParameter.exportBrokenLinks")) {
    			Utils.debug("Skipping resource with broken data link : "+resourceInfo.getDataURL()+ " from export !");
    			continue;
    		}
  
    		// new implementation uses resource APPROVED flag (simpler)
    		if (!resourceInfo.isApproved()) {
    			Utils.debug("Skipping export of unapproved resource "+resourceInfo.getDataURL()+ " from export !");
    			continue;
    		}
    		
    		// old implementation that uses export permission set by owner
    		/*if (!resourceInfo.getUserID().equals(myUserID)) {
	    		String permissions = resourceManager.getResourcePermissions(resourceInfo.getDataURL());
	    		
	    		// Error
	    		if (permissions.isEmpty()) continue;
	    		
	    		int otherPermissions = Integer.parseInt(permissions.substring(2, 3));
	    		// Check export permissions
	    		if ((otherPermissions & 4) != 4) {
	    			Utils.debug("Skipping private resource "+resourceInfo.getDataURL()+ " from export !");
	    			continue;
	    		}
	    	}*/
    		
    		rfl2.add(resourceInfo);
    	}
    	
    	if(rfl2.isEmpty()) {
    		showError("None of the selected resources are available for export, due to restrictions by their owners !");
    		return rfl2;
    	}
    
    	//Utils.debug("resources2BExported : "+rfl2.size());
    	return rfl2;
    }
    
    
	  /**
     * Save all accepted resources (includes BLL-Concepts)
     * @param resourcesForExport List of resources (not resource files, as in old impl)
	 * @param exportFormat TODO
	 * @param exportMode TODO
	 * @param format TODO
     */
    private void exportRDFJSONImpl(List<ResourceInfo> resourcesForExport, AnnohubExportFormat exportFormat, RDFExportMode exportMode) {
    	
    	// Same implementation as in FileDownloadBean (but with easier access to ResourceInfo list)
    	
    	// Catch initial call on startup
    	//if (jsonFilePath == null || jsonFilePath.isEmpty()) return;
    	
    	if (fidConfig.getString("RunParameter.RdfExportFile").isEmpty()) {
    		Utils.debug("RunParameter.RdfExportFile not set - skipping RDF export !");
    		return;
    	}
    	
    	Utils.debug("exportRDFImpl");
    	
    	// refresh will update Accepted state !
    	refreshAction();
    	
    	// ALL
		HashSet <ParseResult> withResults = new HashSet<ParseResult>();
		withResults.add(ParseResult.SUCCESS);
		//withResults.add(ParseResult.ERROR);
		//withResults.add(ParseResult.NONE);
		//withResults.add(ParseResult.UNKNOWN);
    	
		List<ResourceInfo> rflf = null;
		
		// TODO delete cached version
    	if(!fidConfig.getBoolean("RunParameter.cached")) {
    
	    	// get file info for resources
	    	rflf = resourceManager.addFileInfo2Resources(resourcesForExport, withResults);
	    	
	    	// must get file results for all files in resources !
	    	executer.getWriter().getQueries().getFileResults(rflf, resourceManager);
    	} else {
    		rflf = new ArrayList<ResourceInfo>();
    		
    		for (ResourceInfo resourceInfo : resourcesForExport) {
    			if (ExecutionBean.getResourceCache().getResourceFileMap().containsKey(resourceInfo.getDataURL())) {
    				rflf.addAll(ExecutionBean.getResourceCache().getResourceFileMap().get(resourceInfo.getDataURL()));
    			} else {
    				Utils.debug("Dismissing resource "+resourceInfo.getDataURL()+" from export because the resource produced no results !");
    			}
    		}
    	}
    	
    	switch(exportFormat) {
    	
    	case RDF:
    		
    		// Save two RDF versions (intern for portal, extern for publication)
        	String exportString = "";
        	String exportStringR = "";
        	
        	// TODO will set BLL concepts in FileResults again if they have been already initialized (see init() above)
    		rdfModelIntern = RDFSerializer.serializeResourceInfo2RDFModelIntern(
    				(ArrayList<ResourceInfo>) rflf,
    				executer.getWriter().getBllTools(),
    				fidConfig,
    				executer.getModelDefinition()
    				);
    		rdfModelExtern = RDFSerializer.serializeResourceInfo2RDFModelExtern(
    				(ArrayList<ResourceInfo>) rflf,
    				executer.getWriter().getBllTools(),
    				fidConfig,
    				executer.getModelDefinition()
    				);

    		Utils.debug("RDF model built !");

    		exportString = RDFSerializer.serializeModel(rdfModelIntern, RDFFormat.TURTLE_PRETTY);
    		exportStringR = RDFSerializer.serializeModel(rdfModelExtern, RDFFormat.TURTLE_PRETTY);
    		
    		if (exportMode == RDFExportMode.ALL) {
    			
	    		//Utils.debug(exportString);
	    		Utils.debug("RDF model serialized !");
	    		String rdfExport = fidConfig.getString("RunParameter.RdfExportFile");
	    		String annohubRelease = fidConfig.getString("RunParameter.AnnohubRelease");
	    		
	    		
	        	if (!exportString.isEmpty()) {
	        		Utils.debug("Saving RDF export to file "+rdfExport+" ...");
	        		Utils.writeFile(new File(rdfExport), exportString);
	        	} else {
	        		Utils.debug("Error : exportString is Empty !");
	        	}
	        	
	        	if (fidConfig.getString("RunParameter.AnnohubRelease").isEmpty()) {
	        		Utils.debug("RunParameter.AnnohubRelease not set - skipping export of AnnohubRelease !");
	        		return;
	        	}
	        	
	        	if (!exportStringR.isEmpty()) {
	        		Utils.debug("Saving RDF export to file "+annohubRelease+" ...");
	        		Utils.writeFile(new File(annohubRelease), exportStringR);
    	            showInfo("ACCEPTED data published successfully as RDF!");
	        	}
	        	
    		} else {
    			
	    		// prepare file for download
	    		String fileType = "text/rdf";
	    		String fileName = "Annohub-Export_"+(new Date().toString())+".ttl";
	    		
	    	    try {
	    			InputStream stream = new ByteArrayInputStream(exportString.getBytes(StandardCharsets.UTF_8.name()));
	    			     file = new DefaultStreamedContent(stream, fileType, fileName);
	    	
	    			} catch (UnsupportedEncodingException e) {
	    				e.printStackTrace();
	    		}
    		}
    		break;
    	
    		
    		
    	case JSON:
    		
    		// Wait until jsonFilePath is initialized (in init method)
    		if (jsonFilePath == null || jsonFilePath.isEmpty()) {
    			showError("Configuration variable jsonFilePath not set - skipping JSON export !");
    			return;
    		}
    			
    		HashSet<ProcessState> allowedProcessStates = new HashSet<ProcessState>();
    		allowedProcessStates.add(ProcessState.ACCEPTED);
    		
    		try {
    			exportString = JSONSerializer.serializeResourceInfos2JSON(
    					rflf,
    					allowedProcessStates,
    					executer.getModelDefinition()
    					);
    	

    		Utils.debug("Saving JSON export to file "+jsonFilePath+ " ...");
    		try (FileWriter writer = new FileWriter(jsonFilePath);
    	            BufferedWriter bw = new BufferedWriter(writer)) {
    	            bw.write(exportString);
    	            Utils.debug("JSON export finished sucessfully !");
    	            showInfo("ACCEPTED data published successfully as JSON!");
    	            
    	        } catch (IOException e) {
    	        	Utils.debug("jsonFilePath : "+jsonFilePath);
    	            System.err.format("IOException: %s%n", e);
    	        }
    		} catch (Exception e) {
    			e.printStackTrace();
    				showInfo("Publish Error : "+e.getMessage().substring(0, Math.min(40, +e.getMessage().length()-1)));
    		}
    		break;
    	
    	default :
    		showError("Error : ExportFormat "+exportFormat.toString()+" not recognized !");
    		return;
    	}
    }
	

	public boolean isShowMyResources() {
		return showMyResources;
	}


	public void setShowMyResources(boolean showMyResources) {
		this.showMyResources = showMyResources;
	}


	public boolean isShowSelectedResources() {
		return showSelectedResources;
	}


	public void setShowSelectedResources(boolean showSelectedResources) {
		this.showSelectedResources = showSelectedResources;
	}


	public boolean isShowSearchResources() {
		return showSearchResources;
	}


	public void setShowSearchResources(boolean showSearchResources) {
		this.showSearchResources = showSearchResources;
	}


	public List <ResourceInfo> getSearchResourceResultList() {
		return searchResourceResultList;
	}


	public void setSearchResourceResultList(List <ResourceInfo> searchResourceResultList) {
		this.searchResourceResultList = searchResourceResultList;
	}


	public String getModelSearch() {
		return modelSearch;
	}


	public void setModelSearch(String modelSearch) {
		this.modelSearch = modelSearch;
	}


	public String getFindFilterByType() {
		return findFilterByType;
	}


	public void setFindFilterByType(String findFilterByType) {
		this.findFilterByType = findFilterByType;
	}


	public String getSearchInfobarText() {
		return searchInfobarText;
	}


	public void setSearchInfobarText(String searchInfobarText) {
		this.searchInfobarText = searchInfobarText;
	}


	public Boolean getResourceNameSearchIgnoreCase() {
		return resourceNameSearchIgnoreCase;
	}


	public void setResourceNameSearchIgnoreCase(
			Boolean resourceNameSearchIgnoreCase) {
		this.resourceNameSearchIgnoreCase = resourceNameSearchIgnoreCase;
	}
	
	public void cancelMetadataEdit() {
		
		Utils.debug("Cancel meta-data edit");
		try {
	
			// Restore old values
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		editMetadataClose();
	}
	
	public void saveMetadataEdit() {
		
		Utils.debug("saveMetadataEdit");
		
		if (!canEdit) {
				showError("Editing is not allowed by resource owner !");
				return;
		}
		
		// temporarily off until update URLs in Linghub and CLARIN are available
//		if (!metaUrl.trim().isEmpty() && !metaUrl.trim().endsWith("/tbc")) {
//			if (!ExecutionBean.getResourceChecker().urlIsOnline(metaUrl, 10)) {
//				showError("Not saved, because metadata URL is not online !");
//				return;
//			};
//		}
		
		guiSelectedResource.getResourceMetadata().setMetadataSource(MetadataSource.USER);
		guiSelectedResource.getResourceMetadata().setFormat(metaFormat);
		guiSelectedResource.getResourceMetadata().setRights(metaRights);
		guiSelectedResource.getResourceMetadata().setLicense(metaLicense);
		guiSelectedResource.getResourceMetadata().setDctIdentifier(metaDctIdentifier);
		guiSelectedResource.getResourceMetadata().setDctSource(metaDctSource);
		guiSelectedResource.getResourceMetadata().setPublisher(metaPublisher);
		guiSelectedResource.getResourceMetadata().setTitle(metaTitle);
		guiSelectedResource.getResourceMetadata().setUbTitle(metaUbTitle);
		guiSelectedResource.getResourceMetadata().setDescription(metaDescription);
		guiSelectedResource.getResourceMetadata().setCreator(metaCreator);
		guiSelectedResource.getResourceMetadata().setContributor(metaContributor);
		guiSelectedResource.getResourceMetadata().setLocation(metaLocation);
		guiSelectedResource.getResourceMetadata().setYear(metaYear);
		guiSelectedResource.getResourceMetadata().setEmailContact(metaContact);
		guiSelectedResource.getResourceMetadata().setWebpage(metaWebpage);
		guiSelectedResource.getResourceMetadata().setType(metaType);
		//guiSelectedResource.getLinghubAttributes().setDate(metaDate); parse date from string
		guiSelectedResource.getResourceMetadata().setDcLanguageString(metaDcLanguageString);
		guiSelectedResource.getResourceMetadata().setDctLanguageString(metaDctLanguageString);
		guiSelectedResource.getResourceMetadata().setKeywords(metaSubject);
		guiSelectedResource.setMetaDataURL(metaUrl);

		
		/*if (!metaSource.equals("linghub") && !metaSource.equals("clarin") && !metaSource.equals("user")
			&& !metaSource.trim().isEmpty()) {
			selectedResource.getLinghubAttributes().setMetadataSource("");
		} else {
			selectedResource.getLinghubAttributes().setMetadataSource(ResourceManager.MetaDatasourceUser);
		}*/
		
		
		// update meta-data for all other files in resource
		/*HashSet <String> resourceIdentifiers = new HashSet<String>();
		resourceIdentifiers.add(selectedResource.getDataURL());		
		copyResourceMetadataImpl(resourceIdentifiers, selectedResource.getLinghubAttributes());*/
		
		// Save to DB
		resourceManager.updateResourceMetadata(guiSelectedResource);
		//resourceManager.setResourceMetaDataUrl(resourceInfo.getDataURL(), ResourceManager.MetaDataFromUser);
		resourceManager.setResourceMetaDataUrl(guiSelectedResource.getDataURL(), metaUrl);
		
		// fetch availabe CLARIN MD by OAI dc:identifer field
		MetadataSource oldMDsource = guiSelectedResource.getResourceMetadata().getMetadataSource();
		if (executionBean.getPostgresManager() != null) {
			ArrayList <ResourceInfo> tmp = new ArrayList<ResourceInfo>();
			tmp.add(guiSelectedResource);
			executionBean.getPostgresManager().getClarinMetadataByDcIdentifer(tmp);
		}

		String message = "Metadata saved !";
		if (guiSelectedResource.getResourceMetadata().getMetadataSource() != oldMDsource) {
			message = "Resource was updated with CLARIN metadata !";
		}

		// Show info msg
        FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO,"INFO", message);
        FacesContext.getCurrentInstance().addMessage(null, msg);
        
        editMetadataClose();
	}

	public void cancelParseOptionsEdit() {
		
	}
	
	
	public void saveParseOptionsEdit() {
		this.makeCustomParseStats();
	}
	
	
	public void saveEditResourceType() {
		
		ArrayList<ResourceTypeInfo> resourceTypeInfos = guiSelectedResource.getResourceTypeInfos();
		
		if (resourceTypeInfos.isEmpty()) {// not type
			ResourceTypeInfo rti = new ResourceTypeInfo(selectedResourceType);
			rti.setDetectionMethod(DetectionMethod.MANUAL);
			resourceTypeInfos.add(rti);
		} else {
			ResourceTypeInfo rti = resourceTypeInfos.get(0); // only use-case with one resource type
			rti.setResourceType(selectedResourceType);
			rti.setDetectionMethod(DetectionMethod.MANUAL);
		}
		
		// save resource type
		resourceManager.updateResourceTypeInfos(guiSelectedResource.getDataURL(), resourceTypeInfos);
		
		refreshButton();
	}
	
	
	public void initEditDataUrl() {
		setEditedDataUrl(guiSelectedResource.getDataURL());
	}
	
	
	public void saveEditedDataUrl() {
		
		editedDataUrlIsOnline = ExecutionBean.getResourceChecker().urlIsOnline(editedDataUrl, 10);
		if (editedDataUrlIsOnline) {
			
			// 1. Save Data URL to REG-DB
			boolean success = resourceManager.setResourceDataUrl(guiSelectedResource.getDataURL(), editedDataUrl);
			if (!success) {
				
				// Restore old values
				//cancelResourceEdit();
				
				// Show error msg
	            showError("EDIT CANCELED because dataURL already exists in database !");
				return;
			}
			
			
			// update data url in cache
			ExecutionBean.getResourceCache().updateResourceUrlInMaps(guiSelectedResource.getDataURL(), editedDataUrl);
		
			
			// update data url in model DB
			Executer.getDataDBQueries().updateHitDataUrl(
					guiSelectedResource.getDataURL(), editedDataUrl);
			
			
			// update data url in object
			guiSelectedResource.setDataURL(editedDataUrl);
			
			
			// update url in edit manager
			executionBean.getEditManager().clearResourceEditLock(guiSelectedResource.getDataURL());
			//executionBean.getEditManager().getResourceEditLock(editedDataUrl, userAccount);
			
			//FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:myResources");
			//RequestContext.getCurrentInstance().reset("form:myResources");
			
			refreshButton();
			
			showInfo("URL was updated sucessfully !");
			
		} else {
			showError("The URL '"+editedDataUrl+"'\n is not online !");
		}
	}
	
	
	
	
	public StreamedContent getFile(String mode) {
		
		if(mode.equals("workspace")) {
			exportRDF(RDFExportMode.WORKSPACE);
		}
		
		if(mode.equals("single")) {
			
			if (!canExport) {
				showError("Export not allowed by resource owner !");
				return null;
			}
			
			exportRDFSingle();
		}
		
        return file;
    }


	public String getMetaFormat() {
		return metaFormat;
	}


	public void setMetaFormat(String metaFormat) {
		this.metaFormat = metaFormat;
	}


	public String getMetaDataURLGui() {
		return metaDataURLGui;
	}


	public void setMetaDataURLGui(String metaDataURLGui) {
		this.metaDataURLGui = metaDataURLGui;
	}


	public String getMetaType() {
		return metaType;
	}


	public void setMetaType(String metaType) {
		this.metaType = metaType;
	}


	public String getMetaRights() {
		return metaRights;
	}


	public void setMetaRights(String metaRights) {
		this.metaRights = metaRights;
	}


	public String getMetaPublisher() {
		return metaPublisher;
	}


	public void setMetaPublisher(String metaPublisher) {
		this.metaPublisher = metaPublisher;
	}


	public String getMetaTitle() {
		return metaTitle;
	}


	public void setMetaTitle(String metaTitle) {
		this.metaTitle = metaTitle;
	}


	public String getMetaUbTitle() {
		return metaUbTitle;
	}


	public void setMetaUbTitle(String metaUbTitle) {
		this.metaUbTitle = metaUbTitle;
	}


	public String getMetaDescription() {
		return metaDescription;
	}


	public void setMetaDescription(String metaDescription) {
		this.metaDescription = metaDescription;
	}


	public String getMetaCreator() {
		return metaCreator;
	}


	public void setMetaCreator(String metaCreator) {
		this.metaCreator = metaCreator;
	}


	public String getMetaContributor() {
		return metaContributor;
	}


	public void setMetaContributor(String metaContributor) {
		this.metaContributor = metaContributor;
	}


	public String getMetaLocation() {
		return metaLocation;
	}


	public void setMetaLocation(String metaLocation) {
		this.metaLocation = metaLocation;
	}


	public Date getMetaDate() {
		return metaDate;
	}


	public void setMetaDate(Date metaDate) {
		this.metaDate = metaDate;
	}


	public String getMetaYear() {
		return metaYear;
	}


	public void setMetaYear(String metaYear) {
		this.metaYear = metaYear;
	}


	public String getMetaContact() {
		return metaContact;
	}


	public void setMetaContact(String metaContact) {
		this.metaContact = metaContact;
	}


	public String getMetaWebpage() {
		return metaWebpage;
	}


	public void setMetaWebpage(String metaWebpage) {
		this.metaWebpage = metaWebpage;
	}


	public String getMetaDctLanguageString() {
		return metaDctLanguageString;
	}


	public void setMetaDctLanguageString(String metaDctLanguageString) {
		this.metaDctLanguageString = metaDctLanguageString;
	}


	public String getMetaDcLanguageString() {
		return metaDcLanguageString;
	}


	public void setMetaDcLanguageString(String metaDcLanguageString) {
		this.metaDcLanguageString = metaDcLanguageString;
	}


	public String getMetaSubject() {
		return metaSubject;
	}


	public void setMetaSubject(String metaSubject) {
		this.metaSubject = metaSubject;
	}


	public String getMetaSource() {
		return metaSource;
	}


	public void setMetaSource(String metaSource) {
		this.metaSource = metaSource;
	}


	public boolean isCanRead() {
		return canRead;
	}


	public void setCanRead(boolean canRead) {
		this.canRead = canRead;
	}


	public boolean getCanEdit() {
		return canEdit;
	}


	public void setCanEdit(boolean canEdit) {
		this.canEdit = canEdit;
	}


	public boolean isCanExport() {
		return canExport;
	}


	public void setCanExport(boolean canExport) {
		this.canExport = canExport;
	}


	public boolean isCanDelete() {
		return canDelete;
	}


	public void setCanDelete(boolean canDelete) {
		this.canDelete = canDelete;
	}


	public int getRdfMaxSamples() {
		return rdfMaxSamples;
	}


	public void setRdfMaxSamples(int rdfMaxSamples) {
		this.rdfMaxSamples = rdfMaxSamples;
	}


	public int getRdfActivationThreshold() {
		return rdfActivationThreshold;
	}


	public void setRdfActivationThreshold(int rdfActivationThreshold) {
		this.rdfActivationThreshold = rdfActivationThreshold;
	}


	public int getRdfThresholdForGood() {
		return rdfThresholdForGood;
	}


	public void setRdfThresholdForGood(int rdfThresholdForGood) {
		this.rdfThresholdForGood = rdfThresholdForGood;
	}


	public int getRdfThresholdForBad() {
		return rdfThresholdForBad;
	}


	public void setRdfThresholdForBad(int rdfThresholdForBad) {
		this.rdfThresholdForBad = rdfThresholdForBad;
	}


	public int getXmlMaxSamples() {
		return xmlMaxSamples;
	}


	public void setXmlMaxSamples(int xmlMaxSamples) {
		this.xmlMaxSamples = xmlMaxSamples;
	}


	public int getXmlActivationThreshold() {
		return xmlActivationThreshold;
	}


	public void setXmlActivationThreshold(int xmlActivationThreshold) {
		this.xmlActivationThreshold = xmlActivationThreshold;
	}


	public int getXmlThresholdForGood() {
		return xmlThresholdForGood;
	}


	public void setXmlThresholdForGood(int xmlThresholdForGood) {
		this.xmlThresholdForGood = xmlThresholdForGood;
	}


	public int getXmlThresholdForBad() {
		return xmlThresholdForBad;
	}


	public void setXmlThresholdForBad(int xmlThresholdForBad) {
		this.xmlThresholdForBad = xmlThresholdForBad;
	}


	public int getConllMaxSamples() {
		return conllMaxSamples;
	}


	public void setConllMaxSamples(int conllMaxSamples) {
		this.conllMaxSamples = conllMaxSamples;
	}


	public int getConllActivationThreshold() {
		return conllActivationThreshold;
	}


	public void setConllActivationThreshold(int conllActivationThreshold) {
		this.conllActivationThreshold = conllActivationThreshold;
	}


	public int getConllThresholdForGood() {
		return conllThresholdForGood;
	}


	public void setConllThresholdForGood(int conllThresholdForGood) {
		this.conllThresholdForGood = conllThresholdForGood;
	}


	public int getConllThresholdForBad() {
		return conllThresholdForBad;
	}


	public void setConllThresholdForBad(int conllThresholdForBad) {
		this.conllThresholdForBad = conllThresholdForBad;
	}



	public ResourceType getSelectedResourceType() {
		return selectedResourceType;
	}



	public void setSelectedResourceType(ResourceType selectedResourceType) {
		this.selectedResourceType = selectedResourceType;
	}



	public String getSelectedResourceTypeSpecifier() {
		return selectedResourceTypeSpecifier;
	}



	public void setSelectedResourceTypeSpecifier(
			String selectedResourceTypeSpecifier) {
		this.selectedResourceTypeSpecifier = selectedResourceTypeSpecifier;
	}



	public void readComments() {
		
		Utils.debug("readComments : "+guiSelectedResource.getDataURL());
		
		List<Comment> comments = resourceManager.getAllComments(guiSelectedResource.getDataURL());
		guiSelectedResource.setComments(comments);
		
		resourceComments = "";
		String headline = "";
		Collections.sort(comments); // sort by id
		
		for (Comment c : comments) {
			headline = 
				"["+c.getId()+"] "+
				" ## "+c.getTitle()+" ## "+
				" by <"+c.getUserId()+">";
			if (c.getRelatedPostId() > 0) {
				headline += " as response to ["+c.getRelatedPostId()+"]";
			}
			headline += ", "+new Date(c.getDate()).toString();
				
			
			resourceComments = "\n"+headline+"\n"+c.getText()+"\n"+resourceComments;
		}
		
	}
	
	
	public void openAddComment() {
		
		Utils.debug("openAddComment");
		int nextFreeId = resourceManager.getNextCommentId(guiSelectedResource.getDataURL());
		previousCommentIds = new ArrayList<Integer>();
		
		int i = 1;
		while (i < nextFreeId) {
			previousCommentIds.add(i);
			i++;
		}
		
		// reset
		newCommentTitle="";
		newCommentText="";
		newRelatedPostId = nextFreeId-1;
	}
	
	public void closeComments() {
		if(newCommentWasAdded) {
			// update
			refreshButton();
		}
		// reset
		newCommentWasAdded=false;
	}
	
	
	public List<Integer> getPreviousCommentIds() {
		return previousCommentIds;
	}
	
	
	public void addComment() {
		
		Utils.debug("addComment");
		if (newCommentText.trim().isEmpty()) return;
		
		//System.out.println(newCommentText);
		
		Comment comment = new Comment();
		comment.setId(resourceManager.getNextCommentId(guiSelectedResource.getDataURL()));
		comment.setDate(new Date().getTime());
		comment.setUserId(myUserID);
		comment.setTitle(newCommentTitle);
		comment.setText(newCommentText);
		if (newRelatedPostId == null) {
			comment.setRelatedPostId(0);
		} else {
			comment.setRelatedPostId(newRelatedPostId);
		}
		
		resourceManager.addComment(guiSelectedResource.getDataURL(), comment);
		
		// update
		readComments();
		// set variable to update table later
		newCommentWasAdded=true;
	}
	
	
	
	public String getResourceComments() {
		
		if (resourceComments.trim().isEmpty()) return "NO COMMENTS YET !";
		
		return resourceComments;
	}



	public void setResourceComments(String resourceComments) {
		this.resourceComments = resourceComments;
	}



	public String getNewCommentTitle() {
		return newCommentTitle;
	}



	public void setNewCommentTitle(String newCommentTitle) {
		this.newCommentTitle = newCommentTitle;
	}



	public String getNewCommentText() {
		return newCommentText;
	}



	public void setNewCommentText(String newCommentText) {
		this.newCommentText = newCommentText;
	}



	public int getNewRelatedPostId() {
		return newRelatedPostId;
	}



	public void setNewRelatedPostId(int newRelatedPostId) {
		this.newRelatedPostId = newRelatedPostId;
	}



	public UserAccount getUserAccount() {
		return userAccount;
	}


	public void setUserAccount(UserAccount userAccount) {
		this.userAccount = userAccount;
	}
	
	public void saveMyAccount() {
		
		Utils.debug("saveMyAccount");
				
		if (cPasswdActive) {
			if(!passwordOK(cPasswd_1a, cPasswd_2a)) {
				return;
			}
			
			boolean emailChanged=!cEmail.equals(userAccount.getUserEmail());
			if (emailChanged) {
				resourceManager.setUserEmail(myUserID, cEmail);
				userAccount.setUserEmail(cEmail);
			}
			boolean passwdChanged=!cPasswd_1a.trim().isEmpty();
			if (passwdChanged) {
				resourceManager.setUserPassword(myUserID, cPasswd_1a);
			}
			if (emailChanged && passwdChanged) {
				showInfo("Password and email data were sucessfully updated !");
			} else {
				if (emailChanged) {
					showInfo("Email data was sucessfully updated !");
				}
				if (passwdChanged) {
					showInfo("Password data was sucessfully updated !");
				}
			}
			
		} else {
			if (!cEmail.equals(userAccount.getUserEmail())) {
				resourceManager.setUserEmail(myUserID, cEmail);
				userAccount.setUserEmail(cEmail);
				showInfo("Email data was sucessfully updated !");
			}
		}
	}
	
	
	public void initMyAccountGui() {

		System.out.println("initMyAccountGui");
		cPasswd_1a = "";
		cPasswd_2a = "";
		cEmail = userAccount.getUserEmail();
		setcPasswdActive(false);
	}
	
	
	public void changeUserPassword() {
		setcPasswdActive(true);
	}

	
	public boolean iscPasswdActive() {
		return cPasswdActive;
	}



	public void setcPasswdActive(boolean cPasswdActive) {
		this.cPasswdActive = cPasswdActive;
	}



	public String getcPasswd_1() {
		return cPasswd_1;
	}



	public void setcPasswd_1(String cPasswd_1) {
		this.cPasswd_1 = cPasswd_1;
	}



	public String getcPasswd_2() {
		return cPasswd_2;
	}



	public void setcPasswd_2(String cPasswd_2) {
		this.cPasswd_2 = cPasswd_2;
	}



	public String getcEmail() {
		return cEmail;
	}



	public void setcEmail(String cEmail) {
		this.cEmail = cEmail;
	}



	public String getDeleteMessage() {
		return deleteMessage;
	}



	public void setDeleteMessage(String deleteMessage) {
		this.deleteMessage = deleteMessage;
	}


	// only used for permission to comment ?
	public boolean isCanBeAdded() {
		return canAdd;
	}



	public void setCanBeAdded(boolean canBeAdded) {
		this.canAdd = canBeAdded;
	}



	public String getMetadataSearch() {
		return metadataSearch;
	}

	public void setMetadataSearch(String metadataSearch) {
		this.metadataSearch = metadataSearch;
	}

	public String getAnnotationSearch() {
		return annotationSearch;
	}

	public void setAnnotationSearch(String annotationSearch) {
		this.annotationSearch = annotationSearch;
	}

	public String getAnnotationSearchMode() {
		return annotationSearchMode;
	}

	public void setAnnotationSearchMode(String annotationSearchMode) {
		this.annotationSearchMode = annotationSearchMode;
	}

	public String getAnnotationSearchOrAnd() {
		return annotationSearchOrAnd;
	}

	public void setAnnotationSearchOrAnd(String annotationSearchOrAnd) {
		this.annotationSearchOrAnd = annotationSearchOrAnd;
	}

	public String getOliaSearch() {
		return oliaSearch;
	}

	public void setOliaSearch(String oliaSearch) {
		this.oliaSearch = oliaSearch;
	}

	public String getOliaClassDescription() {
		return oliaClassDescription;
	}
	
	public void setOliaClassDescription(String oliaClassDescription) {
		this.oliaClassDescription = oliaClassDescription;
	}

	public List<String> getOliaClasses() {
		//List<String> x = new ArrayList<String>(ExecutionBean.getMinimalOliaClasses().keySet());
		List<String> x = new ArrayList<String>(ExecutionBean.getResourceCache().getExtendedOliaResourceMap().keySet());
		Collections.sort(x);
		return x;
	}
	
	
	public StreamedContent getProcessStateImage() throws IOException {

		FacesContext context = FacesContext.getCurrentInstance();

		if (context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE) {
			return new DefaultStreamedContent();
		}

		else {

			String imagePath=null;
			byte[] image;
			
			boolean online = Boolean.parseBoolean(context.getExternalContext().getRequestParameterMap()
					.get("resourceIsOnline"));
			
			
			if (online) {
				imagePath = locateUtils.getLocalFile("/images/dot-green.png").getAbsolutePath();
			} else {
				imagePath = locateUtils.getLocalFile("/images/dot-red.png").getAbsolutePath();
			}
			
			try {
				Path path = new File(imagePath).toPath();
				image = Files.readAllBytes(path);
				return new DefaultStreamedContent(new ByteArrayInputStream(image));
			} catch (Exception e) {
				e.printStackTrace();
			}
			}
		
			return new DefaultStreamedContent();
				
			/*if (state == null) {
				imagePath = locateUtils.getLocalFile("/images/dot-green.png").getAbsolutePath();
			} else {
				
				Utils.debug("resourceIsOnline : "+state);
				
				switch (ResourceState.valueOf(state)) {
					
				case 	ResourceUrlIsBroken :
						imagePath = locateUtils.getLocalFile("/images/dot-red.png").getAbsolutePath();
						//imagePath = new File(new File(fidConfig.getString("WebApp.iconFolder")),"dot-blue.png").getAbsolutePath();
						break;
				case	ResourceHasNotChanged :
				case	ResourceNotInDB :
				case	ResourceHasChanged :
				case	Invalid :
						imagePath = locateUtils.getLocalFile("/images/dot-green.png").getAbsolutePath();
						break;
				
				default :
						Utils.debug("Error : unknown ProcessState : "+state);
						return null;
				}
			}*/
	}
	
	
	public void startBrokenLinkCheck(){
		
		Utils.debug("startBrokenLinkCheck");
		
		if (executionBean.startBrokenLinksCheck(10)) {
			showInfo("Started Broken-Links-Check successfully !");
		} else {
			showError("Error : Broken-Links-Check already in progress !");
		}
		
	}
	
	
	public void saveAdminOptions(){
		
		Utils.debug("saveAdminOptions");
		
		// Save parameter to loaded config
		fidConfig.setProperty("RunParameter.checkBrokenLinksInterval", checkBrokenLinksInterval);
		fidConfig.setProperty("RunParameter.publishRDFExportInterval", publishRDFExportInterval);
		fidConfig.setProperty("Quotas.maxResourceUploads", maxResourceUploads);
		fidConfig.setProperty("Quotas.maxResourceFiles", maxResourceFiles);
		fidConfig.setProperty("Quotas.maxResourceUploadSize", maxResourceUploadSize);
		
		// Save parameter to FidConfig file
		String error = Run.saveFIDConfig(fidConfig);
		if (error.isEmpty()) {
			showInfo("FID configuration succesfully updated !");
		}
		else {
			showError("Error : "+error+"!");
		}
	}


	public int getCheckBrokenLinksInterval() {
		return checkBrokenLinksInterval;
	}


	public void setCheckBrokenLinksInterval(int checkBrokenLinksInterval) {
		this.checkBrokenLinksInterval = checkBrokenLinksInterval;
	}


	public int getMaxResourceUploads() {
		return maxResourceUploads;
	}
	
//	public int getMaxResourceUploadsMAX() {
//		return Math.max(getMaxResourceUploads(), userAccount.getQuotas().getMaxResourceUploads());
//	}

	public void setMaxResourceUploads(int maxResourceUploads) {
		this.maxResourceUploads = maxResourceUploads;
	}


	public int getMaxResourceFiles() {
		return maxResourceFiles;
	}


	public void setMaxResourceFiles(int maxResourceFiles) {
		this.maxResourceFiles = maxResourceFiles;
	}


	public int getMaxResourceUploadSize() {
		return maxResourceUploadSize;
	}


	public void setMaxResourceUploadSize(int maxResourceUploadSize) {
		this.maxResourceUploadSize = maxResourceUploadSize;
	}
	
//	public int getMaxResourceFilesMAX() {
//	return Math.max(getMaxResourceFiles(), userAccount.getQuotas().getMaxResourceFiles());
//}	

//	public int getMaxResourceUploadSizeMAX() {
//	return Math.max(getMaxResourceUploadSize(), userAccount.getQuotas().getMaxResourceUploadSize());
//}
	
	private String isFileUploadAllowed(int countResources2Upload) {
		
		Utils.debug("isFileUploadAllowed");
		
		// check if current user has an account
    	if (!resourceManager.userExists(this.myUserID)) {
    		Utils.debug("Error in isFileUploadAllowed : "+this.myUserID+" has no user account !");
    		return "not allowed";
    	}

		
		int actualuserResourceCount = resourceManager.getResourcesOwnedByUserAsUrl(myUserID).size();
		long actualUserResourceFileCount=resourceManager.getResourceFileCountOwnedByUser(myUserID);
		//System.out.println("+++"+actualUserResourceFileCount);
		long uploadedResourceSize = 0;
		if (FileUploadBean.getUploadedFile() != null) {
			uploadedResourceSize=FileUploadBean.getUploadedFile().getSize();
		}
		//if (uploadedResourceSize == 0) return "Error : uploaded resource size is zero";

		Utils.debug("uploadedResourceSize "+uploadedResourceSize);
		Utils.debug("uploadFileSizeInMBytes " +uploadedResourceSize/1000000.0);
		//Utils.debug("getMaxResourceUploadSizeMAX() "+getMaxResourceUploadSizeMAX());
		
		
		if (uploadedResourceSize/1000000.0 > userAccount.getQuotas().getMaxResourceUploadSize()) return IndexUtils.ERROR_UPLOAD_FILE_SIZE_LIMIT_EXECEEDED;
		if (actualuserResourceCount + countResources2Upload >= userAccount.getQuotas().getMaxResourceUploads()) return IndexUtils.ERROR_UPLOAD_RESOURCE_COUNT_LIMIT_EXECEEDED;
		if (actualUserResourceFileCount >= userAccount.getQuotas().getMaxResourceFiles()) return IndexUtils.ERROR_UPLOAD_RESOURCE_FILE_COUNT_LIMIT_EXECEEDED;

//		if (uploadedResourceSize/1000000.0 > this.getMaxResourceUploadSizeMAX()) return IndexUtils.ERROR_UPLOAD_FILE_SIZE_LIMIT_EXECEEDED;
//		if (actualuserResourceCount + countResources2Upload >= this.getMaxResourceUploadsMAX()) return IndexUtils.ERROR_UPLOAD_RESOURCE_COUNT_LIMIT_EXECEEDED;
//		if (actualUserResourceFileCount >= this.getMaxResourceFilesMAX()) return IndexUtils.ERROR_UPLOAD_RESOURCE_FILE_COUNT_LIMIT_EXECEEDED;

		return "";
	}



	public int getPublishRDFExportInterval() {
		return publishRDFExportInterval;
	}

	public void setPublishRDFExportInterval(int publishRDFExportInterval) {
		this.publishRDFExportInterval = publishRDFExportInterval;
	}
	
	
	public void publish() {
		
		Utils.debug("publish");
		exportRDF(RDFExportMode.ALL);
		exportJSON(RDFExportMode.ALL);
		
	}
	
	private void exportJSON(RDFExportMode exportMode) {
		
		Utils.debug("exportJSON "+exportMode);
		
		List<ResourceInfo> result = getResources2BExported(exportMode);
    	if (result == null) return;
    	exportRDFJSONImpl(result, AnnohubExportFormat.JSON, exportMode);
		
	}


	public void initRflError() {
		rflError = ExecutionBean.getResourceCache().getCachedErrorResourcesByUserID(myUserID);
//		this.rflError = ExecutionBean.getResourceCache().getCachedErrorResourcesByUrl(
//				resourceManager.getResourcesOwnedByUserAsUrl(myUserID));
//		// does not show 404 errors (which is not saved in DB)
	}

	public List <ResourceInfo> getRflError() {
		return rflError;
	}
	

	public void setRflError(List <ResourceInfo> rflError_) {
		rflError = rflError_;
	}


	public List <ResourceInfo> getFilteredRflError() {
		return this.filteredRflError;
	}



	public void setFilteredRflError(List <ResourceInfo> filteredRflError) {
		this.filteredRflError = filteredRflError;
	}
	
	
	public void clearLog() {
		Utils.debug("Clear log");
		Iterator <ResourceInfo> rt = rflError.iterator();
		while (rt.hasNext()) {
			ResourceInfo x = rt.next();
			rt.remove();
		}
	}

	public String getCommentSearch() {
		return commentSearch;
	}

	public void setCommentSearch(String commentSearch) {
		this.commentSearch = commentSearch;
	}
	
	public List <ParseResult> getParseResultStates() {
		return Arrays.asList(ParseResult.values());
	}



	public Boolean getCommentSearchIgnoreCase() {
		return commentSearchIgnoreCase;
	}



	public void setCommentSearchIgnoreCase(Boolean commentSearchIgnoreCase) {
		this.commentSearchIgnoreCase = commentSearchIgnoreCase;
	}



	public Boolean getMetadataSearchIgnoreCase() {
		return metadataSearchIgnoreCase;
	}



	public void setMetadataSearchIgnoreCase(Boolean metadataSearchIgnoreCase) {
		this.metadataSearchIgnoreCase = metadataSearchIgnoreCase;
	}



	public Boolean getAnnotationSearchIgnoreCase() {
		return annotationSearchIgnoreCase;
	}



	public void setAnnotationSearchIgnoreCase(Boolean annotationSearchIgnoreCase) {
		this.annotationSearchIgnoreCase = annotationSearchIgnoreCase;
	}
	
	
	public List<String> annotationAutoComplete (String query) {
        String queryLowerCase = query.toLowerCase();
        List<String> annotations = new ArrayList<>();
        
        if(annotationSearchMode.equals("Tag")) {
        	annotations = new ArrayList<String>(ExecutionBean.getResourceCache().getExtendedAnnotationResourceMap().keySet());
            return annotations.stream().filter(t -> t.toLowerCase().contains(queryLowerCase)).collect(Collectors.toList());
        } 
        if(annotationSearchMode.equals("Class")) {
        	annotations = new ArrayList<String>(ExecutionBean.getResourceCache().getExtendedAnnotationClassResourceMap().keySet());
            return annotations.stream().filter(t -> t.toLowerCase().contains(queryLowerCase)).collect(Collectors.toList());
        }
        
       return annotations;
    }
	
    public List<String> noResults(String query) {
        return Collections.emptyList();
    }
    
    public void splitAnnotationSearch() {
    	String[] split = annotationSearch.split(" ");
    	annotationSearch=split[0];
    }
	
    
    public String initUserManagement() {
    	
    	System.out.println("initUserManagement");
    	
    	createNewUser=false;
    	
    	getAdminUserCount();
    	getMemberUserCount();
    	getGuestUserCount();
    	getRetiredUserCount();
    	getAllUserCount();

    	
    	setUserLoginsList(getAllUsers());
    	if (!userLoginsList.isEmpty()) {
    		selectedUser = userLoginsList.get(0);
    		selectedUserAccount = userManagement.getUserAccount(selectedUser);
        	selectedUserAccountType = selectedUserAccount.getAccountType().name();
    	} else {
    		newUserAccount();
    	}
    	
    	userLoginWritable=false;    	
    	return "";
    }
    
    public void closeUserManagement() {
    	refreshAction();
    }
    
    
    public void initUserSearch() {
    	setUserLoginsList(getAllUsers());
    	if (!userLoginsList.isEmpty()) {
    		selectedUserSearch = userLoginsList.get(0);
    	}
    }

    
    public void setUserDefaultQuotas() { 
    	selectedUserAccount.setQuotas(userManagement.
    			getDefaultUserQuotas(AccountType.valueOf(selectedUserAccountType)));		
    }
    
    
    public void selectUser() {
    	System.out.println("select user :"+selectedUser);
		selectedUserAccount = userManagement.getUserAccount(selectedUser);
    	selectedUserAccountType = selectedUserAccount.getAccountType().name();
    	userLoginWritable=false;
    	createNewUser=false;
    }
    
    public String getSelectedUser() {
    	return this.selectedUser;
    }
    
    public UserAccount getSelectedUserAccount() {
    	return selectedUserAccount;
    }
    
    public void setSelectedUser(String user) {
    	this.selectedUser = user;
    	this.selectedUserAccountType = this.getSelectedUserAccount().getAccountType().name();
    }
    
    public List<String> getAllUsers() {
    	return userManagement.getAllUserLogins();
    }
    
    public int getAllUserCount() {
    	return userManagement.getAllUserLogins().size();
    }
    
    public long getAdminUserCount() {
    	return userManagement.getAdminUserCount();
    }
    
    public long getMemberUserCount() {
    	return userManagement.getMemberUserCount();
    }
    
    public long getGuestUserCount() {
    	return userManagement.getGuestUserCount();
    }
    
    public long getRetiredUserCount() {
    	return userManagement.getRetiredUserCount();
    }
    
    public int getOnlineUserCount() {
    	int count=-1;
    	try {
    		count = ExecutionBean.getUserLog().getUsersOnline().size();
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	return count;
    }
    
    public void newUserAccount() {
    	
    	Utils.debug("newUserAccount");
    	selectedUser="";
		selectedUserAccount = new UserAccount("", "", "");
    	selectedUserAccountType = selectedUserAccount.getAccountType().name();    	
    	setUserLoginWritable(true);
    	createNewUser=true;
    }
    
    public void saveUserAccount() {
    	
    	Utils.debug("saveUserAccount");
    	
    	if (createNewUser) {
    		String error = userManagement.checkLogin(selectedUserAccount);
    		if (!error.isEmpty()) {
    			showError(error);
    			return;
    		}
		}
    	
    	if (!passwordOK(cPasswd_1, cPasswd_2)) return;
    	
		selectedUserAccount.setUserPassword(cPasswd_1);
    	selectedUserAccount.setAccountType(AccountType.valueOf(selectedUserAccountType));
    	boolean success = userManagement.updateUserAccount(selectedUserAccount, createNewUser);
    	userLoginWritable=false;
    	if (success) {
    		if (!createNewUser) {
    			showInfo("Updated user '"+selectedUserAccount.getUserID()+"' successfully !");
    		} else {
    			showInfo("Created user '"+selectedUserAccount.getUserID()+"' successfully !");
    		}
    		
    	} else {
    		if (!createNewUser) {
    			showError("User update failed !");
    		} else {
    			showError("User creation failed !");
    		}
    	}
    	
    	if (createNewUser) {
    		initUserManagement();
    	}
    	createNewUser=false;
    }
    

	/**
	 * @param passwd
 	 * @param passwdr
 	 * @param cPasswd_12
 	 * @return
	 */
	private boolean passwordOK(String passwd, String passwdr) {
		
		Utils.debug("passwordOK");
		
		
		if (createNewUser && passwd.isEmpty()) {
			showError("Password error : supplied password is empty !");
    		return false;
		};
		
		
		// check passwd
    	if(!passwd.equals(passwdr)) {
  
    		showError("Password error : supplied passwords are not identical !");
    		return false;
    	}
    	    	
    	String error = userManagement.checkPassword(passwd);

		if(!error.isEmpty()) {
			Utils.debug(error);
			showError(error);
			return false;
		}
		
		if (!createNewUser && !passwd.isEmpty()) {
			showStickyMessage("Warning : changed password of existing user !", FacesMessage.SEVERITY_WARN);
		}
		return true;
	}



	public void deleteUserAccount() {
		
		Utils.debug("deleteUserAccount");
		
		if (selectedUserAccount.getAccountType() == AccountType.ADMIN) {
			showError("Error : can not delete admin user !");
			return;
		}
		
		if (ExecutionBean.getUserLog().userIsOnline(selectedUser)) {
			showError("Error : can not delete user because is logged in !");
			return;
		}
		
		List<String> userResources = resourceManager.getResourcesOwnedByUserAsUrl(selectedUserAccount.getUserID());
		
 		String error = userManagement.deleteUserAccount(selectedUserAccount);
 		System.out.println("error "+error);
 		if (!error.isEmpty()) {
 			showError(error);
 			return;
 		} else {
 			// Remove all resources of deleted user from cache
 			for (String url : userResources) {
 				Utils.debug("delete user resource "+url+" from cache");
 				ExecutionBean.getResourceCache().deleteResource(url);
 			}
 			
 			showInfo("Successfully deleted user '"+selectedUser+"' and all of its resources !");
 		}
 		
 		// autoselect next free user in list
 		createNewUser=false;
		initUserManagement();
 	}
	
	
	public String getDeleteUserMsg() {
		return "Delete user '"+selectedUserAccount.getUserID()+"' ???";
	}

	public String getSelectedUserAccountType() {
		return selectedUserAccountType;
	}

	public void setSelectedUserAccountType(String selectedUserAccountType) {
		this.selectedUserAccountType = selectedUserAccountType;
	}
	
	public List<String> getAccountTypes() {
		
		ArrayList<String> result = new ArrayList<String>();
		for (AccountType x :AccountType.values()) {
			if (x == AccountType.ADMIN) {
				if(selectedUserAccount.getAccountType() == AccountType.ADMIN) {
					result.add(x.name());
				}
			} else {
				result.add(x.name());
			}
		}
		return result;
	}



	public List<String> getUserLoginsList() {
		System.out.println("getUserLoginsList");
		return userLoginsList;
	}



	public void setUserLoginsList(List<String> userLoginsList) {
		this.userLoginsList = userLoginsList;
	}



	public boolean isUserLoginWritable() {
		return userLoginWritable;
	}



	public void setUserLoginWritable(boolean userLoginWritable) {
		this.userLoginWritable = userLoginWritable;
	}


	public String getcPasswd_1a() {
		return cPasswd_1a;
	}


	public void setcPasswd_1a(String cPasswd_1a) {
		this.cPasswd_1a = cPasswd_1a;
	}


	public String getcPasswd_2a() {
		return cPasswd_2a;
	}


	public void setcPasswd_2a(String cPasswd_2a) {
		this.cPasswd_2a = cPasswd_2a;
	}



	public String getSelectedUserSearch() {
		return selectedUserSearch;
	}



	public void setSelectedUserSearch(String selectedUserSearch) {
		this.selectedUserSearch = selectedUserSearch;
	}



	public List<LanguageProfile> getLanguageProfileList() {
		return languageProfileList;
	}

	public void setLanguageProfileList(List<LanguageProfile> languageProfileList) {
		this.languageProfileList = languageProfileList;
	}



	public void languageEvalStart() {
    	System.out.println("languageEvalStart");
    }

	
	public String showLanguageManager() {
    	
    	System.out.println("initLanguageManager");
	    //languageProfileList = OptimaizeLanguageTools1.getLanguageProfiles();
    	
    	try {
			FacesContext.getCurrentInstance().getExternalContext().redirect("login-languages.xhtml");
		    FacesContext.getCurrentInstance().responseComplete();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	return ""; 
	}
	
	
	public String closeLanguageManager() {
		
		System.out.println("lpClose");
		
		try {
			FacesContext.getCurrentInstance().getExternalContext().redirect("login-admin.xhtml");
		    FacesContext.getCurrentInstance().responseComplete();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
		
//		refreshAction();
//		return "login?faces-redirect=true";

	}
	
	
	public String showOntologyManager() {
    	
    	System.out.println("initOntologyManager");
	    //languageProfileList = OptimaizeLanguageTools1.getLanguageProfiles();
    	
    	try {
			FacesContext.getCurrentInstance().getExternalContext().redirect("login-models.xhtml");
		    FacesContext.getCurrentInstance().responseComplete();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	return ""; 
	}
	
	
	
	public String closeOntologyManager() {
		
	    //resourceManager = executer.createNewResourceManagerInstance();
		Utils.debug("closeOntologyManager");
		
		try {
			FacesContext.getCurrentInstance().getExternalContext().redirect("login-admin.xhtml");
		    FacesContext.getCurrentInstance().responseComplete();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
		
//		refreshAction();
//		return "login?faces-redirect=true";
	}
	
	
	public String showBackupManager() {
    	
    	System.out.println("initBackupManager");
    	
    	try {
			FacesContext.getCurrentInstance().getExternalContext().redirect("login-backup.xhtml");
		    FacesContext.getCurrentInstance().responseComplete();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	return ""; 
	}
		
	public String closeBackupManager() {
	
	    //resourceManager = executer.createNewResourceManagerInstance();
		Utils.debug("closeBackupManager");
		
		try {
			FacesContext.getCurrentInstance().getExternalContext().redirect("login-admin.xhtml");
		    FacesContext.getCurrentInstance().responseComplete();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//refreshAction();
		//return "login?faces-redirect=true";
		return "";

	}
	
	
	public String showAdminManager() {
    	
    	System.out.println("initAdminManager");
    	
    	try {
			FacesContext.getCurrentInstance().getExternalContext().redirect("login-admin.xhtml");
		    FacesContext.getCurrentInstance().responseComplete();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return ""; 
	}
		
	public String closeAdminManager() {
	
	    resourceManager = executer.createNewResourceManagerInstance();
		Utils.debug("closeAdminManager");
		refreshAction();
		return "login?faces-redirect=true";
		
	}
	
	
	public String showMyAccount() {
    	
    	System.out.println("initMyAccount");
    	
    	initMyAccountGui();
    	
    	try {
			FacesContext.getCurrentInstance().getExternalContext().redirect("login-my.xhtml");
		    FacesContext.getCurrentInstance().responseComplete();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return ""; 
	}
		
	public String closeMyAccount() {
	
	    resourceManager = executer.createNewResourceManagerInstance();
		Utils.debug("closeMyAccount");
		refreshAction();
		return "login?faces-redirect=true";
	}
	
	
	public String showUploadManager() {
    	
    	System.out.println("initUploadManager");
    	
    	try {
			FacesContext.getCurrentInstance().getExternalContext().redirect("login-upload.xhtml");
		    FacesContext.getCurrentInstance().responseComplete();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return ""; 
	}
		
	public String closeUploadManager() {
	
	    resourceManager = executer.createNewResourceManagerInstance();
		Utils.debug("closeUploadManager");
		refreshAction();
		return "login?faces-redirect=true";
	}

	

	public XMLConfiguration getFidConfig() {
		return fidConfig;
	}

	public void setFidConfig(XMLConfiguration fidConfig) {
		LoginBean.fidConfig = fidConfig;
	}

	public String getEmptyMessage() {
		return emptyMessage;
	}

	public void setEmptyMessage(String emptyMessage) {
		this.emptyMessage = emptyMessage;
	}

	public ResourceManager getResourceManager() {
		return resourceManager;
	}

	public void setResourceManager(ResourceManager resourceManager) {
		this.resourceManager = resourceManager;
	}
	
	
	public void shutdown () {
		
		File backupFile = new File(fidConfig.getString("RunParameter.QueueBackupFile"));
		backupFile.delete();
		
		List<ResourceInfo> queuedResources = messageBrowser.getQueuedResources();
		if (!queuedResources.isEmpty()) {
			// Save queued resources to file
			Utils.writeQueuedResources2File(queuedResources, backupFile);
		} else {
			Utils.debug("Queue empty : nothing to backup !");
		}
	}
		
	

	public void testQueueAdd() {
		
		ArrayList<ResourceInfo> x = new ArrayList<ResourceInfo>();
		int counter = 1;
		while (counter <= 10000) {
		ResourceInfo y = new ResourceInfo();
			y.setUserID("id"+counter);
			y.setDataURL("http://www.domain"+counter+".com");
			y.setResourceFormat(ResourceFormat.RDF);
			x.add(y);
			counter++;
		}
		
		resourceManager.addQueue("testQueue", x);
	}
	
	
	public void testQueueRead() {
		
		List<ResourceInfo> resources = resourceManager.getQueue("testQueue");
		int counter = 1;
		for (ResourceInfo rs : resources) {
			System.out.println(counter++);
			System.out.println(rs.getUserID());
			System.out.println(rs.getDataURL());
			System.out.println(rs.getMetaDataURL());
			System.out.println(rs.getResourceFormat().name());

		}
	}
	
	
	public void testQueueDelete() {
		resourceManager.deleteQueue("testQueue");
	}


	public String getFindSelectedModel() {
		return findSelectedModel;
	}

	
	public void setFindSelectedModel(String findSelectedModel) {
		System.out.println(findSelectedModel);
		this.findSelectedModel = findSelectedModel;
	}



	public Boolean getResourceUploadImportMetadata() {
		return resourceUploadImportMetadata;
	}



	public void setResourceUploadImportMetadata(
			Boolean resourceUploadImportMetadata) {
		this.resourceUploadImportMetadata = resourceUploadImportMetadata;
	}
	
	
	public String getEditedDataUrl() {
		return editedDataUrl;
	}


	public void setEditedDataUrl(String editedDataUrl) {
		this.editedDataUrl = editedDataUrl;
	}


	public Boolean getEditedDataUrlIsOnline() {
		return editedDataUrlIsOnline;
	}


	public void setEditedDataUrlIsOnline(Boolean editedResourceUrlIsOnline) {
		this.editedDataUrlIsOnline = editedResourceUrlIsOnline;
	}



	public Boolean getFindExclusiveLanguages() {
		return findExclusiveLanguages;
	}



	public void setFindExclusiveLanguages(Boolean findOnlyLanguages) {
		this.findExclusiveLanguages = findOnlyLanguages;
	}



	public Boolean getFindExclusiveModels() {
		return findExclusiveModels;
	}



	public void setFindExclusiveModels(Boolean findExclusiveModels) {
		this.findExclusiveModels = findExclusiveModels;
	}



	public Boolean getResourceUploadAutoAccept() {
		return resourceUploadAutoAccept;
	}



	public void setResourceUploadAutoAccept(Boolean resourceUploadAutoAccept) {
		this.resourceUploadAutoAccept = resourceUploadAutoAccept;
	}
	
	
	public void editMetadataOpen() {
		setEditMetadataOpen(true);
	}
	
	
	public void editMetadataClose() {
		setEditMetadataOpen(false);
	}
	
	
	public void hotKeyC () {
		
		if (editMetadataOpen) {
			
			if (guiSelectedResource != null){
				mdCopyBuffer = SerializationUtils.clone(guiSelectedResource.getResourceMetadata());
				Utils.debug("metadata copy");
			}
			showInfo ("Copy Metadata");
		}
	}
	
	
	
	public void hotKeyV () {
		
		if (editMetadataOpen) {
			if(mdCopyBuffer != null){
				
				Utils.debug("metadata paste");
				initMetadataView(mdCopyBuffer);
				
				FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:aboutdisplay");
				RequestContext.getCurrentInstance().update("form:aboutdisplay");
			}
			showInfo ("Paste Metadata");
		}
	}
	
	

	public boolean isEditMetadataOpen() {
		return editMetadataOpen;
	}



	public void setEditMetadataOpen(boolean editMetadataOpen) {
		this.editMetadataOpen = editMetadataOpen;
	}



	public String getMetaUrl() {
		return metaUrl;
	}



	public void setMetaUrl(String metaUrl) {
		this.metaUrl = metaUrl;
	}



	public String getMetaLicense() {
		return metaLicense;
	}



	public void setMetaLicense(String metaLicense) {
		this.metaLicense = metaLicense;
	}



	public String getMetaDctSource() {
		return metaDctSource;
	}



	public void setMetaDctSource(String metaDctSource) {
		this.metaDctSource = metaDctSource;
	}



	public synchronized String getMetaDctIdentifier() {
		return metaDctIdentifier;
	}



	public synchronized void setMetaDctIdentifier(String metaDctIdentifier) {
		this.metaDctIdentifier = metaDctIdentifier;
	}



	public int getLexiconCount() {
		return lexiconCount;
	}



	public void setLexiconCount(int lexiconCount) {
		this.lexiconCount = lexiconCount;
	}



	public int getCorporaCount() {
		return corporaCount;
	}



	public void setCorporaCount(int corporaCount) {
		this.corporaCount = corporaCount;
	}
	
	public void updateCounts() {
		
		HashSet<String> languages = new HashSet<String>();
		lexiconCount=0;
		corporaCount=0;
		ontologyCount=0;
		
		rdfResCount=0;
		conllResCount=0;
		xmlResCount=0;
		
		for (ResourceInfo rs : ExecutionBean.getResourceCache().getResourceMap().values()) {
			if (rs.isApproved()) {
				
				for (ResourceTypeInfo rt : rs.getResourceTypeInfos()) {
					
					switch (rt.getResourceType()) {
					
					case LEXICON:
						lexiconCount++;
						break;
					
					case CORPUS:
						corporaCount++;
						break;
						
					case ONTOLOGY:
						ontologyCount++;
						break;
						
					default:
						break;
					}
				}
				
				for (ResourceInfo f : ExecutionBean.getResourceCache().getResourceFileMap().get(rs.getDataURL())) {
					languages.addAll(Arrays.asList(f.getFileInfo().getLanguageMatchingsAsString().split(",")));
				}
				languageCount = languages.size();
				
				// determine xml,rdf,conll resources
				boolean foundConll = false;
				boolean foundRdf = false;
				boolean foundXml = false;
				for (ResourceInfo fileResource : 
					ExecutionBean.getResourceCache().getResourceFileMap().get(rs.getDataURL())) {
					if (fileResource.getFileInfo().getProcessState() == ProcessState.ACCEPTED) {
						ResourceFormat format = IndexUtils.determineFileFormat(fileResource);
						if (format == ResourceFormat.CONLL) {
							foundConll = true;
							continue;
						}
						if (format == ResourceFormat.RDF) {
							foundRdf = true;
							continue;
						}
						if (format == ResourceFormat.XML) {
							foundXml = true;
							continue;
						}
					}
				}
				if (foundRdf) {
					rdfResCount++;
					System.out.println("RDF : "+rs.getDataURL());
				}
				if (foundConll) {
					conllResCount++;
					System.out.println("CONLL : "+rs.getDataURL());
				}
				if (foundXml) {
					xmlResCount++;
					System.out.println("XML : "+rs.getDataURL());
				}
			}
		}
		
		System.out.println("\nApproved resources by file format:");
		System.out.println("RDF : "+rdfResCount);
		System.out.println("CONLL : "+conllResCount);
		System.out.println("XML : "+xmlResCount);
		System.out.println("");

	}
	
	public String getInfobarTopText() {
		
		String text = lexiconCount+" Lexica, "+corporaCount+" Corpora ,"+ontologyCount+" Ontologies and "+languageCount+" Languages";
		return text;
	}
	
	
	
 }