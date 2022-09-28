package de.unifrankfurt.informatik.acoli.fid.webclient;


import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.event.PhaseId;
import javax.faces.validator.ValidatorException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFFormat;
import org.primefaces.context.RequestContext;
import org.primefaces.event.NodeExpandEvent;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.ToggleEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.TreeNode;
import org.primefaces.model.Visibility;

import de.unifrankfurt.informatik.acoli.fid.activemq.Consumer;
import de.unifrankfurt.informatik.acoli.fid.exec.Executer;
import de.unifrankfurt.informatik.acoli.fid.exec.Run;
import de.unifrankfurt.informatik.acoli.fid.linghub.UrlBroker;
import de.unifrankfurt.informatik.acoli.fid.owl.ModelDefinition;
import de.unifrankfurt.informatik.acoli.fid.parser.ParserISONames;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager;
import de.unifrankfurt.informatik.acoli.fid.serializer.JSONSerializer;
import de.unifrankfurt.informatik.acoli.fid.serializer.RDFSerializer;
import de.unifrankfurt.informatik.acoli.fid.types.DetectionMethod;
import de.unifrankfurt.informatik.acoli.fid.types.FileInfo;
import de.unifrankfurt.informatik.acoli.fid.types.FileResult;
import de.unifrankfurt.informatik.acoli.fid.types.InvalidLanguageException;
import de.unifrankfurt.informatik.acoli.fid.types.LanguageMatch;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceMetadata;
import de.unifrankfurt.informatik.acoli.fid.types.MetadataSource;
import de.unifrankfurt.informatik.acoli.fid.types.MetadataState;
import de.unifrankfurt.informatik.acoli.fid.types.ModelMatch;
import de.unifrankfurt.informatik.acoli.fid.types.ModelType;
import de.unifrankfurt.informatik.acoli.fid.types.ParseResult;
import de.unifrankfurt.informatik.acoli.fid.types.ProcessState;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceFormat;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceProcessState;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceType;
import de.unifrankfurt.informatik.acoli.fid.types.UserAccount;
import de.unifrankfurt.informatik.acoli.fid.types.VocabularyMatch;
import de.unifrankfurt.informatik.acoli.fid.types.VocabularyType;
import de.unifrankfurt.informatik.acoli.fid.ub.PostgresManager;
import de.unifrankfurt.informatik.acoli.fid.util.IndexUtils;
import de.unifrankfurt.informatik.acoli.fid.util.LocateUtils;
import de.unifrankfurt.informatik.acoli.fid.util.RDFPrefixUtils;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;


@ManagedBean(name="loginrinfo")
@ViewScoped
//@Singleton
public class LoginResourceInfoBean implements Serializable {
	
	
	@ManagedProperty(value="#{execute}")
    private ExecutionBean executionBean;
	
	@ManagedProperty(value="#{login.guiSelectedResource}")
    private ResourceInfo displayedResource = null;
	
	@ManagedProperty(value="#{login.canEdit}")
    private boolean canEdit;
	
	
	private List <ResourceInfo> rfl = null;
	private List <ResourceInfo> filteredRfl = null;
	private static final long serialVersionUID = 1L;
	private boolean loaded = false;
	
	private static ResourceInfo selectedDummyResource;
	private static LanguageMatch selectedDummyLanguage;
	private static ModelMatch selectedDummyModel;
	private static FileResult selectedDummyFileResult;
	
	private ResourceInfo selectedResource;
	private ResourceInfo selectedResourceOrg;
	
	private LanguageMatch selectedLanguageMatch;
	private ModelMatch selectedModelMatch;
	private ArrayList <Integer> conllColumnsWithText = new ArrayList<Integer>();
	private ArrayList <Integer> conllColumnsWithModels = new ArrayList<Integer>();
	private ArrayList <Integer> freeConllColumns = new ArrayList<Integer>();
	
	
	private int editedConllModelColumn=0;
	private ModelType editedConllModel = null;
	
	private static Executer executer;
	
	private String metaDataURLNew="";
	private String dataURLNew="";
	
	private String metaDataURLGui="";
	private String dataURLGui="";
	
	private Model rdfModelIntern = null;
	private Model rdfModelExtern = null;
	private StreamedContent file;
	
	private TreeNode selectedNode = null;
	private int selectedCol = 0;				// gui variable index for selected combobox entry
	private int selectedLangCol = -1;		// gui variable index for selected combobox entry
	
	private ModelType selectedColModel = null;
	
	private HashSet<String> sessions = new HashSet<String>();
	
	private String uploadURL;
	private ArrayList<String> messages = new ArrayList<String>();
	
	private String commentGui="";
	
	private static List <ResourceInfo> rflError = null;
	private static List <ResourceInfo> filteredRflError = null;
	
	private static XMLConfiguration fidConfig;
	
	private static List <ResourceInfo> resourceQueue = null;
	
	private String manualISOInput="";
	private String isoLanguageLabel="unknown language";
	
	private Integer newLangConllColumn = 0;
	private Integer newModelConllColumn = 0;
	private ModelType newModel=null;
	
	private boolean noPolling = false;
	
	
	private String metaFormat = "";
	private String metaType = "";
	private String metaRights = "";
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
	
	private LocateUtils locateUtils = new LocateUtils();
	
	
	
	@EJB
    ExecuterEjb executionEjb;
	
	
	private String jsonFilePath = "";

	private static String selectedMultiAcdState=ProcessState.ACCEPTED.name();
	
	private String selectedMultiAcdURL="";
		
	private String multiACDInfo;
	
	private HashSet<String> multiACDResources = new HashSet<String>();
	private ArrayList<ResourceInfo> multiACDResources2 = new ArrayList<ResourceInfo>();
	
	private int tableFirstPage = 0;
	private int newResources = 0;
	
	//private static PostgresManager mng;
	
	private ResourceManager resourceManager;
	private int getBllInfoConllCall = 0;
	private FileResult selectedFileResult;
	
	private GuiConfiguration guiConfiguration;
	private String confirmMessage;
	private Boolean resourceQueueIsEmpty;

	private String testUser = "testUser";
	
	private List<ModelMatch> selectedModels=null;

	
	@PostConstruct
    public void init() {
		
		if (loaded) return;
		loaded = true;
		
		fidConfig = Executer.getFidConfig();
		//fidConfig = Run.loadFIDConfig("/home/debian7/Arbeitsfläche/FIDConfigPublic.xml");
		executer = ExecutionBean.getPublicExecuter();
		resourceManager = ExecutionBean.getPublicExecuter().getResourceManager();
		
		Utils.debug("init - getDoneResourcesRI");
		Utils.debug("******************* "+resourceManager.getCluster().getMaxContentLength()+ "*********************");
		
		rfl = ExecutionBean.getResourceCache().getResourceFileMap().get(displayedResource.getDataURL());
		
		// needed somewhere (actually not required)
		resourceQueue = new ArrayList<ResourceInfo>();
		
		guiConfiguration = new GuiConfiguration(fidConfig);
	}
	
	
	private String getSessionID() {
		FacesContext fCtx = FacesContext.getCurrentInstance();
		HttpSession session = (HttpSession) fCtx.getExternalContext().getSession(false);
		Utils.debug(session.getId());
		return session.getId();
	}
	
	
	public List <ResourceInfo> getResourceInfoList() {

		Utils.debug("getResourceInfoList");
		sessions.add(getSessionID());		
		return rfl;
	}
	
	
	public void refreshAction(ActionEvent actionEvent) {
		rfl = null; 
		// getResourceInfoList() is called automatically thereafter 
	}
	
	
	public List<ResourceInfo> getFilteredResourceInfoList() {
	    return filteredRfl;
	}
	
	public void setFilteredResourceInfoList(List <ResourceInfo> filteredRfl) {
	    this.filteredRfl = filteredRfl;
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
		models.remove(ModelType.valueOf("UDEP"));
		//models.remove(ModelType.UNKNOWN);
		return models;*/
	}
	
	/*public List <ModelType> getModels() {
	List<ModelType> models =  Arrays.asList(ModelType.values());
	return models;
    }*/
	
	public List <VocabularyType> getVocabularies() {
		ArrayList <VocabularyType> temp = new ArrayList <VocabularyType> (new HashSet <VocabularyType> (RDFPrefixUtils.vocabulariesForCorpusAndLexicaCreation.values()));
		Collections.sort(temp);
		return new ArrayList <VocabularyType> (temp);
	}
	
	public List <MetadataState> getMetadataStates() {
		return Arrays.asList(MetadataState.values());
	}
	
	
	public List <MetadataSource> getMetadataSources() {
		return Arrays.asList(MetadataSource.values());
	}
	
	public List <ResourceType> getResourceTypes() {
		return Arrays.asList(ResourceType.values());
	}
	
	public List <ParseResult> getParseResultStates() {
		return Arrays.asList(ParseResult.values());
	}
	
	public List <ProcessState> getProcessingStates() {
		return Arrays.asList(ProcessState.values());
	}
	
	
    
   
    
    
    public void showLanguageInputError(ResourceInfo resourceInfo) {
    	 FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_FATAL, "Input Error", "Please enter ISO-639 code of language !");
         FacesContext.getCurrentInstance().addMessage(null, msg);        
    }
    
    public void showModelInputError(ResourceInfo resourceInfo) {
   	 FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_FATAL, "Input Error", "Please enter valid model name !");
        FacesContext.getCurrentInstance().addMessage(null, msg);        
    }
    
    
    // Dummy methods to enable row selection on table !
    public void setSelectedDummyResource(ResourceInfo selectedResource) {}
    public ResourceInfo getSelectedDummyResource() {
        return selectedDummyResource;
    }
    
    // Dummy methods to enable row selection on table !
    public void setSelectedDummyLanguage(LanguageMatch selectedLanguage) {}
    public LanguageMatch getSelectedDummyLanguage() {
        return selectedDummyLanguage;
    }
    
    // Dummy methods to enable row selection on table !
    public void setSelectedDummyModel(ModelMatch selectedModel) {}
    public ModelMatch getSelectedDummyModel() {
		return selectedDummyModel;
	}
    
    // Dummy methods to enable row selection on table !
    public void setSelectedDummyFileResult(FileResult selectedFileResult) {}
    public FileResult getSelectedDummyFileResult() {
		return selectedDummyFileResult;
	}
    
    
    public void deleteResource() {
    	
    	// Delete resource in database
    	String dataURL = selectedResource.getDataURL();
    	
    	HashSet<String> resources = new HashSet<String>();
    	resources.add(dataURL);
    	deleteResourceImpl(resources);
    	
    	selectedResource = null;
    	Utils.debug("Deleted resource "+dataURL+" !");
    	
    }
    
    
 public String deleteResourceFile() {
    
    	// Delete resource in database
    	String dataURL = selectedResource.getDataURL();
    	String relFilePath = selectedResource.getFileInfo().getRelFilePath();
    	
    	
    	// count files of resource in list (files without results are not shown in the list)
    	int resourceFileCount=0;
    	for (ResourceInfo r : rfl) {
    		if (r.getDataURL().equals(dataURL)) {
    			resourceFileCount++;
    		}
    	}
    	
    	// last file of resource -> delete resource (and all other resource files without results (not shown in the list)
    	if(resourceFileCount == 1) {
    		//Utils.debug("Delete resource because of last file");
    		deleteResource();
        	return "loginresources?faces-redirect=true";
    	}
    	
    	// Delete file from table
    	Iterator <ResourceInfo> iterator = rfl.iterator();
    	while (iterator.hasNext()) {
    		ResourceInfo resourceInfo = iterator.next();
    		if (resourceInfo.getDataURL().equals(dataURL) &&
    			resourceInfo.getFileInfo().getRelFilePath().equals(relFilePath)) {
    			iterator.remove();
    		}
    	}
    	
    	resourceManager.deleteResourceFile(dataURL, relFilePath);
    	
    	selectedResource = null;
    	Utils.debug("Deleted file "+dataURL+" -> "+relFilePath+ " !");
    	return "loginresources?faces-redirect=true";
    }
    
    
    
    private void deleteResourceImpl(Set<String> set) {
    	
    	// Delete resources from database
    	for (String dataURL : set) {
    		resourceManager.deleteResource(dataURL);
    	}
    	
    	// Delete resources from table
    	Iterator <ResourceInfo> iterator = rfl.iterator();
    	while (iterator.hasNext()) {
    		ResourceInfo resourceInfo = iterator.next();
    		if (set.contains(resourceInfo.getDataURL())) {
    			iterator.remove();
    		}
    	}	
	}
    
    /**
     * Update resource metadata in database & update table
     * @param resourceIdentifiers - list of resources to be modified
     * @param linghubAttributes
     */
    private void copyResourceMetadataImpl(Set <String> resourceIdentifiers, ResourceMetadata linghubAttributes) {
    	
    	HashSet <String> tmpResourceIdentifiers = new HashSet <String>();
    	tmpResourceIdentifiers.addAll(resourceIdentifiers);
    	
    	// update metadata for resource in database
    	Iterator <ResourceInfo> iterator = rfl.iterator();
    	while (iterator.hasNext()) {
    		ResourceInfo resourceInfo = iterator.next();
    		if (tmpResourceIdentifiers.contains(resourceInfo.getDataURL())) {
    			resourceInfo.setResourceMetadata((ResourceMetadata) SerializationUtils.clone(linghubAttributes));
    			// update resource metadata
    			resourceManager.updateResourceMetadata(resourceInfo);
    			// set metadata url
				resourceManager.setResourceMetaDataUrl(resourceInfo.getDataURL(), ResourceManager.MetaDataFromUser);
				tmpResourceIdentifiers.remove(resourceInfo.getDataURL()); // update of only one resource is needed !
    		}
    	}
    	
    	// update table
    	updateTableMetadata(resourceIdentifiers, linghubAttributes);
    }

    
    
    /**
     * Save all accepted resources to RDF (includes BLL-Concepts)
     */
    private void exportRDF() {
    	
    	// Same implementation as in FileDownloadBean (but with easier access to ResourceInfo list)
    	
    	// Catch initial call on startup
    	if (jsonFilePath == null || jsonFilePath.isEmpty()) return;
    	
    	if (fidConfig.getString("RunParameter.RdfExportFile").isEmpty()) {
    		Utils.debug("RunParameter.RdfExportFile not set - skipping RDF export !");
    		return;
    	}
    	
    	Utils.debug("exportRDF");
    	
    	// Save two RDF versions (intern for portal, extern for publication)
    	String exportString = "";
    	String exportStringR = "";
    	
    	// TODO will set BLL concepts in FileResults again if they have been already initialized (see init() above)
		rdfModelIntern = RDFSerializer.serializeResourceInfo2RDFModelIntern(
				(ArrayList<ResourceInfo>) rfl,
				executer.getWriter().getBllTools(),
				fidConfig,
				executer.getModelDefinition()
				);
		rdfModelExtern = RDFSerializer.serializeResourceInfo2RDFModelExtern(
				(ArrayList<ResourceInfo>) rfl,
				executer.getWriter().getBllTools(),
				fidConfig,
				executer.getModelDefinition()
				);

		Utils.debug("RDF model built !");

		exportString = RDFSerializer.serializeModel(rdfModelIntern, RDFFormat.TURTLE_PRETTY);
		exportStringR = RDFSerializer.serializeModel(rdfModelExtern, RDFFormat.TURTLE_PRETTY);
		
		//Utils.debug(exportString);
		Utils.debug("RDF model serialized !");
		String rdfExport = fidConfig.getString("RunParameter.RdfExportFile");
		String annohubRelease = fidConfig.getString("RunParameter.AnnohubRelease");

		
		//Utils.debug(exportString);
    	if (!exportString.isEmpty()) {
    		Utils.debug("Saving RDF export to file "+rdfExport+" ...");
    		Utils.writeFile(new File(rdfExport), exportString);
    	}
    	
    	
    	
    	if (fidConfig.getString("RunParameter.AnnohubRelease").isEmpty()) {
    		Utils.debug("RunParameter.AnnohubRelease not set - skipping export of AnnohubRelease !");
    		return;
    	}
    	
    	if (!exportStringR.isEmpty()) {
    		Utils.debug("Saving RDF export to file "+annohubRelease+" ...");
    		Utils.writeFile(new File(annohubRelease), exportStringR);
    	}
    }
    

	public StreamedContent getFile() {
		return file;
	}


	public String getDataURLNew() {
		return dataURLNew;
	}


	public void setDataURLNew(String dataURLNew) {
		this.dataURLNew = dataURLNew;
	}


	public void cancelResourceEdit() {
		
		Utils.debug("Cancel ");
		try {
			Utils.debug(selectedResource == null);
			Utils.debug(selectedResourceOrg == null);
			Utils.debug(selectedResourceOrg.getDataURL() == null);
			Utils.debug("Org : "+selectedResourceOrg.getDataURL());
	
			// Restore old values
			selectedResource.setDataURL(selectedResourceOrg.getDataURL());
			selectedResource.setMetaDataURL(selectedResourceOrg.getMetaDataURL());
			selectedResource.setMetaDataURL2(selectedResourceOrg.getMetaDataURL2());
			selectedResource.setResourceFormat(selectedResourceOrg.getResourceFormat());
			selectedResource.getFileInfo().setLanguageMatchings(selectedResourceOrg.getFileInfo().getLanguageMatchings());
			selectedResource.getFileInfo().setModelMatchings(selectedResourceOrg.getFileInfo().getModelMatchings());

			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	public void saveResourceEdit() {
		
		Utils.debug("saveResourceEdit");
		
		if(!canEdit) {
			showError("Editing is not allowed by owner !");
			return;
		}
		
		// Save to database 
		
		try {
	
		// cannot save unprocessed resource !
		if (selectedResource.getFileInfo().getProcessState() == ProcessState.UNPROCESSED) {
			cancelResourceEdit();
			return;
		}
		
		
		// Use dataURL of unedited resource (DB-Key)
		String dataURL = selectedResourceOrg.getDataURL();
		String relFilePath = selectedResourceOrg.getFileInfo().getRelFilePath();
		
        String dataURLNew = getDataURLGui();//selectedResource.getDataURL();
        String linghubURLNew = getMetaDataURLGui();//selectedResource.getMetaDataURL();
        
        
        // Validate URLs in input fields
        if (!(IndexUtils.isValidURL(dataURLNew) && IndexUtils.isValidURL(linghubURLNew))) {
        	
        	// Restore old values
        	cancelResourceEdit();
			
        	// Show error msg
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_FATAL,"ERROR", "BAD URL in URL input field !");
            FacesContext.getCurrentInstance().addMessage(null, msg);
            
            return;
		} else {
			
			// dataUrl was changed - never the case because not editable in editor
			if (!dataURL.equals(dataURLNew)) {
				
				showError("Implementation Error 21 !");
				if (true) return;
				
				// 1. Save Data URL to REG-DB
				boolean success = resourceManager.setResourceDataUrl(dataURL, dataURLNew);
				if (!success) {
					
					// Restore old values
					cancelResourceEdit();
					
					// Show error msg
		            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_FATAL,"ERROR", "EDIT CANCELED because dataURL already exists in database !");
		            FacesContext.getCurrentInstance().addMessage(null, msg);
					return;
				}
			}
		}
        
		dataURL = dataURLNew;

		// Save Linghub URL to DB //
		resourceManager.setResourceMetaDataUrl(dataURL, linghubURLNew);
		
		
		// Save language changes to DB
		HashSet<String> oldLanguages = new HashSet<String>();
		for (LanguageMatch lang : selectedResourceOrg.getFileInfo().getLanguageMatchings()) {
			oldLanguages.add(lang.getTableID());
		}
		

		for (LanguageMatch lang : selectedResource.getFileInfo().getLanguageMatchings()) {
			if (!oldLanguages.contains(lang.getTableID())) {
				resourceManager.addFileLanguage(
					selectedResource.getDataURL(),
					selectedResource.getFileInfo().getRelFilePath(), lang);
			}
		}
		
		HashSet<String> newLanguages = new HashSet<String>();
		for (LanguageMatch lang : selectedResource.getFileInfo().getLanguageMatchings()) {
			newLanguages.add(lang.getTableID());
		}
		
		for (LanguageMatch lang : selectedResourceOrg.getFileInfo().getLanguageMatchings()) {
			if (!newLanguages.contains(lang.getTableID())) {
				resourceManager.deleteFileLanguage(
						selectedResource.getDataURL(),
						selectedResource.getFileInfo().getRelFilePath(), lang);
			}
		}
		
        // Save Languages to DB (old slow)
		//resourceManager.updateFileLanguages(dataURL, relFilePath, selectedResource.getFileInfo(), true); // manual editing will overwrite
		
		
		// Save model changes to DB //
		HashSet<String> oldModels = new HashSet<String>();
		for (ModelMatch model : selectedResourceOrg.getFileInfo().getModelMatchings()) {
			oldModels.add(model.getTableID());
		}
		

		for (ModelMatch model : selectedResource.getFileInfo().getModelMatchings()) {
			if (!oldModels.contains(model.getTableID())) {
				resourceManager.addFileModel(
					selectedResource.getDataURL(),
					selectedResource.getFileInfo().getRelFilePath(), model);
			}
		}
		
		HashSet<String> newModels = new HashSet<String>();
		for (ModelMatch model : selectedResource.getFileInfo().getModelMatchings()) {
			newModels.add(model.getTableID());
		}
		
		
		for (ModelMatch model : selectedResourceOrg.getFileInfo().getModelMatchings()) {
			if (!newModels.contains(model.getTableID())) {
				resourceManager.deleteFileModel(
						selectedResource.getDataURL(),
						selectedResource.getFileInfo().getRelFilePath(), model);
			}
		}
		
		// Save Models to DB (old slow)
		//resourceManager.updateFileModels(dataURL, relFilePath, selectedResource.getFileInfo(), true);  // manual editing will overwrite
		
		
		// Save comment
		selectedResource.getFileInfo().setComment(getCommentGui());
		resourceManager.updateFileComment(selectedResource);
		
		// set process state edited
		selectedResource.getFileInfo().setProcessState(ProcessState.EDITED);
		resourceManager.updateProcessState(selectedResource);

		
		selectedResource.getFileInfo().updateSelectedModelMatchingsAsTree();
		
		// Update gui table values
		selectedResource.setDataURL(dataURLNew);
		selectedResource.setMetaDataURL(linghubURLNew);
		
				
		// Show info msg
        FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO,"INFO", "Resource saved !");
        FacesContext.getCurrentInstance().addMessage(null, msg);
        
		} catch (Exception e) {
			e.printStackTrace();
			FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,"ERROR", e.getMessage());
	        FacesContext.getCurrentInstance().addMessage(null, msg);
		}
	}
	
	
//	public void saveMetadataEdit() {
//		
//		Utils.debug("saveMetadataEdithier");
//		
//		selectedResource.getResourceMetadata().setMetadataSource(MetadataSource.USER);
//		selectedResource.getResourceMetadata().setFormat(metaFormat);
//		selectedResource.getResourceMetadata().setRights(metaRights);
//		selectedResource.getResourceMetadata().setPublisher(metaPublisher);
//		selectedResource.getResourceMetadata().setTitle(metaTitle);
//		selectedResource.getResourceMetadata().setUbTitle(metaUbTitle);
//		selectedResource.getResourceMetadata().setDescription(metaDescription);
//		selectedResource.getResourceMetadata().setCreator(metaCreator);
//		selectedResource.getResourceMetadata().setContributor(metaContributor);
//		selectedResource.getResourceMetadata().setLocation(metaLocation);
//		selectedResource.getResourceMetadata().setYear(metaYear);
//		selectedResource.getResourceMetadata().setEmailContact(metaContact);
//		selectedResource.getResourceMetadata().setWebpage(metaWebpage);
//		selectedResource.getResourceMetadata().setType(metaType);
//		//selectedResource.getLinghubAttributes().setDate(metaDate); parse date from string
//		selectedResource.getResourceMetadata().setDcLanguageString(metaDcLanguageString);
//		selectedResource.getResourceMetadata().setDctLanguageString(metaDctLanguageString);
//		selectedResource.getResourceMetadata().setKeywords(metaSubject);
//		
//		/*if (!metaSource.equals("linghub") && !metaSource.equals("clarin") && !metaSource.equals("user")
//			&& !metaSource.trim().isEmpty()) {
//			selectedResource.getLinghubAttributes().setMetadataSource("");
//		} else {
//			selectedResource.getLinghubAttributes().setMetadataSource(ResourceManager.MetaDatasourceUser);
//		}*/
//		
//		
//		// update meta-data for all other files in resource
//		HashSet <String> resourceIdentifiers = new HashSet<String>();
//		resourceIdentifiers.add(selectedResource.getDataURL());	
//
//		copyResourceMetadataImpl(resourceIdentifiers, selectedResource.getResourceMetadata());
//		
//		String message = "Metadata saved !";
//		
//		// Show info msg
//		FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO,"INFO", message);
//        FacesContext.getCurrentInstance().addMessage(null, msg);
//	}
//	
//	
//	public void cancelMetadataEdit() {
//		
//			Utils.debug("Cancel meta-data edit");
//			try {
//		
//				// Restore old values
//				
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//	}
		
	
	

	
	public ResourceInfo getSelectedResource() {
        return selectedResource;
    }
	
	public void setSelectedResource(ResourceInfo resourceInfo) {
        selectedResource = resourceInfo;
    }

	public void onTreeNodeSelect(NodeSelectEvent event) {
		
		FileResult nodeData = (FileResult) event.getTreeNode().getData(); // all nodes have type FileResult
		String nodeText = nodeData.getFoundTagOrClass();
		try {
			editedConllModelColumn = Integer.parseInt(nodeText);
			editedConllModel = null;
			} catch (Exception e) {}
		try {
			editedConllModel = ModelType.valueOf(nodeText.split(" ")[0].trim());
			} catch (Exception e) {}
		
		Utils.debug("Select : "+nodeText);
	}
	
	
	public void resourceModelTreeSelect(ActionEvent event) {
		
		if (editedConllModelColumn > 0 && editedConllModel != null) {
			Utils.debug("Column : "+editedConllModelColumn);
			Utils.debug("Model : "+editedConllModel);
			Utils.debug("");
		} else {
			Utils.debug("No model node selected !");
		}
	}
	
	public void onContextMenu(SelectEvent event) {
		
		guiRefreshButtonRefresh();
		
		try {
	
			Utils.debug("Context menu !");
			if (event.getObject() == null) {
				Utils.debug("onContextMenu call with null object !");
				showError("onContextMenu call with null object !");
				return;
			}
			Utils.debug(event.getObject().getClass().getName());
			ResourceInfo x = (ResourceInfo) event.getObject();
			Utils.debug(x.getDataURL());
			
			// Make clone of original resource (is restored on CANCEL)
        	selectedResourceOrg = (ResourceInfo) SerializationUtils.clone(x);
        	Utils.debug("Make copy of selected resource !");
        	Utils.debug(x.getDataURL());
        	selectedResource = x;
        	
        	// Init edit view
        	setDataURLGui(new String(x.getDataURL()));
        	setMetaDataURLGui(new String(x.getMetaDataURL()));
        	setCommentGui(new String(x.getFileInfo().getComment()));
        	
        	conllColumnsWithModels = selectedResource.getFileInfo().getConllColumnsWithModels();
        	if (!conllColumnsWithModels.isEmpty()) {
        		selectedCol=conllColumnsWithModels.get(0);
        	} else {
        		selectedCol=0;
        	}
        	
        	conllColumnsWithText = selectedResource.getFileInfo().getConllColumnsWithText();
        	freeConllColumns = selectedResource.getFileInfo().getFreeConllColumns();
			
        	Collections.sort(conllColumnsWithText);
        	Collections.sort(freeConllColumns);
        	
        	
        	// redundant statements because comboboxes are reseted anyway ?
			if (!conllColumnsWithText.isEmpty()) {
				selectedLangCol = conllColumnsWithText.get(0);
			} else {
				selectedLangCol = 0;
			}
     	
			newLangConllColumn = 0;
			newModelConllColumn = 0;
			// redundant statements because comboboxes are reseted anyway ?
			
			uploadURL="";
			manualISOInput="";
			
//			// init meta-data view
//			setMetaFormat(new String(x.getLinghubAttributes().getFormat()));
//			setMetaTitle(new String(x.getLinghubAttributes().getTitle()));
//			setMetaUbTitle(new String(x.getLinghubAttributes().getUbTitle()));
//			setMetaType(new String(x.getLinghubAttributes().getType()));
//			setMetaRights(new String(x.getLinghubAttributes().getRights()));
//			setMetaPublisher(new String(x.getLinghubAttributes().getPublisher()));
//			setMetaDescription(new String(x.getLinghubAttributes().getDescription()));
//			setMetaCreator(new String(x.getLinghubAttributes().getCreator()));
//			setMetaContributor(new String(x.getLinghubAttributes().getContributor()));
//			setMetaLocation(new String(x.getLinghubAttributes().getLocation()));
//			setMetaYear(x.getLinghubAttributes().getYear());
//			setMetaContact(x.getLinghubAttributes().getEmailContact());
//			setMetaWebpage(x.getLinghubAttributes().getWebpage());
//			setMetaDate(x.getLinghubAttributes().getDate());
//			setMetaDctLanguageString(new String(x.getLinghubAttributes().getDctLanguageString()));
//			setMetaDcLanguageString(new String(x.getLinghubAttributes().getDcLanguageString()));
//			setMetaSubject(new String(x.getLinghubAttributes().getKeywords()));
//			setMetaSource(new String(x.getLinghubAttributes().getMetadataSource().name()));
			
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
		
		try {
			
			// reset and initialize all comboboxes with start values
			// CONLL Models
			FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:a4787");// actually only input fields must be updated
			RequestContext.getCurrentInstance().reset("form:a4787");// actually only input fields must be updated
			// CONLL Languages
			FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:a4787a");// actually only input fields must be updated
			RequestContext.getCurrentInstance().reset("form:a4787a");// actually only input fields must be updated
			// add CONLL text column
			FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:a4900");// actually only input fields must be updated
			RequestContext.getCurrentInstance().reset("form:a4900");// actually only input fields must be updated
			// add RDF/CONLL model
			FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:a4900m");// actually only input fields must be updated
			RequestContext.getCurrentInstance().reset("form:a4900m");// actually only input fields must be updated
			// add CONLL model column
			FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:a4900mc");// actually only input fields must be updated
			RequestContext.getCurrentInstance().reset("form:a4900mc");// actually only input fields must be updated
			// update meta-data
			/*FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:a4715m");// actually only input fields must be updated
			RequestContext.getCurrentInstance().reset("form:a4715m");// actually only input fields must be updated
			// update edit window (comment)*/
			FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:a4715");// actually only input fields must be updated
			RequestContext.getCurrentInstance().reset("form:a4715");// actually only input fields must be updated
			
		} catch (Exception e){e.printStackTrace();}
	}
	


	public String getMetaDataURLGui() {
		return metaDataURLGui;
	}


	public void setMetaDataURLGui(String metaDataURLGui) {
		if(metaDataURLGui.isEmpty()) metaDataURLGui=" ";
		this.metaDataURLGui = metaDataURLGui;
	}


	public String getDataURLGui() {
		return dataURLGui;
	}


	public void setDataURLGui(String dataURLGui) {
		this.dataURLGui = dataURLGui;
	}
    
		
	
	// Conll column editing
	public void setSelectedNode(TreeNode selectedNode) {
		this.selectedNode = selectedNode;
		
		if (selectedNode != null) {
		FileResult x = (FileResult) selectedNode.getData();
		String nodeText = x.getFoundTagOrClass();
		
		try {
			selectedColModel = ModelType.valueOf(nodeText.split(" ")[0].trim());
			} catch (Exception e) {}
		
		Utils.debug("Select : "+nodeText);
		}
	}
	
	public TreeNode getSelectedNode() {
		return selectedNode;
	}
	
	public void onNodeExpand(NodeExpandEvent event) {
        setSelectedNode(event.getTreeNode());
    }

	
	public int getSelectedCol() {
		return selectedCol;
	}


	public void setSelectedCol(int selectedCol) {
		
		// catch automatic call on start
		if (selectedCol == 0) return;
		
		this.selectedCol = selectedCol;
		Utils.debug("Set column : "+selectedCol);
	}
	
	
	public void setColumnModel(ActionEvent event) {
		
		if (selectedResource.getFileInfo().isConllFile() && selectedNode != null) {
		//if (selectedCol > 0 && selectedNode != null) {
			if (selectedColModel != null) {
				selectedResource.getFileInfo().setSelectedModelForColumn(selectedCol, selectedColModel);
				showInfo("Selected model "+selectedColModel.name()+" for column "+selectedCol+ " !");
			}
		} else {
			Utils.debug("No model node selected !!");
		}
	}
	
	
	public void setResourceModel(ActionEvent event) {
		
		Utils.debug("setResourceModel");
		Utils.debug(selectedModelMatch == null);
	    
		Utils.debug("setResourceModel : "+selectedModelMatch.getModelType());
		Utils.debug("setResourceModel : "+selectedModelMatch.getConllColumn());

		
		selectedModelMatch.setSelected(true);
		selectedModelMatch.setDetectionMethod(DetectionMethod.MANUAL);

		
		if (selectedResource.getFileInfo().isConllFile()) {
			
			// unselect all other models in column
			for (ModelMatch mm : selectedResource.getFileInfo().getModelMatchings()) {
				if (mm.getConllColumn() == selectedModelMatch.getConllColumn()) {
					if (!mm.getModelType().name().equals(selectedModelMatch.getModelType().name())) {
						mm.setSelected(false);
					}
				}
			}
		}
		
	    selectedResource.getFileInfo().updateModelMatchingsAsString();
	}
	
	
	public void unsetResourceModel(ActionEvent event) {
		
		Utils.debug("unsetResourceModel");
		Utils.debug(selectedModelMatch == null);
	    	    
		Utils.debug("unsetResourceModel : "+selectedModelMatch.getModelType());
		Utils.debug("unsetResourceModel : "+selectedModelMatch.getConllColumn());
		
		selectedModelMatch.setSelected(false);
		selectedModelMatch.setDetectionMethod(DetectionMethod.MANUAL);
		selectedResource.getFileInfo().updateModelMatchingsAsString();
		
	}
	

	
	public void unsetAllRDFResourceModels (ActionEvent event) {
		Utils.debug("unsetAllResourceModels");
		
		for (ModelMatch mm : selectedResource.getFileInfo().getAllModelMatchingsForColumn(ModelMatch.NOCOLUMN)) {
			mm.setSelected(false);
			mm.setDetectionMethod(DetectionMethod.MANUAL);
		}
		selectedResource.getFileInfo().updateModelMatchingsAsString();
	}
	
	
	
	public void addResourceModel() {
		
		if (true) {
			showInfo("Not yet !");
			return;
		}
			
		Utils.debug("addResourceModel ");
		Utils.debug(newModel.name());
		
		int col;
		if (selectedResource.getFileInfo().isConllFile()) {
			col = selectedCol;
		} else {
			col = ModelMatch.NOCOLUMN;
		}
		
		// Check model already present
		ArrayList<ModelMatch> modelList = selectedResource.getFileInfo().getModelMatchings();
		for (ModelMatch lm : modelList) {
	    	if (col == lm.getConllColumn()) {
	    		if (newModel.equals(lm.getModelType())) {
	    			showInfo("Model already present !");
	    			return;
	    		} else {
	    			if (selectedResource.getFileInfo().isConllFile()) {
	    				lm.setSelected(false);
	    			}
	    		}
	    	}
	    }
					
		// add new model to list
		ModelMatch model = new ModelMatch(newModel, DetectionMethod.MANUAL);
		model.setConllColumn(col);
		model.setCoverage(1.0f);
		model.setSelected(true);
		
	    modelList.add(model);
	    selectedResource.getFileInfo().setModelMatchings(modelList);
	    		    
	    showInfo("Adding new model !");				
	}
	
	
	public void deleteResourceModel(ActionEvent event) {
		
		Utils.debug("deleteResourceModel");
		Utils.debug(selectedModelMatch == null);
	    	    
		Utils.debug("deleteResourceModel : "+selectedModelMatch.getModelType());
		Utils.debug("deleteResourceModel : "+selectedModelMatch.getConllColumn());

		Iterator <ModelMatch> iterator = selectedResource.getFileInfo().getModelMatchings().iterator();
	    while (iterator.hasNext()) {
	    	ModelMatch lm = iterator.next();
	    	if (lm.getTableID().equals(selectedModelMatch.getTableID())) {
	    		iterator.remove();
	    	}
	    }
	    
	    //update string representations
		selectedResource.getFileInfo().updateModelMatchingsAsString();
		
		//update selectedModels
		setSelectedModels();
	}
	
	
	public void selectResourceLanguage(SelectEvent event) { 
		
		selectedLanguageMatch = (LanguageMatch) event.getObject();
		Utils.debug("table-select");
		Utils.debug(selectedLanguageMatch.getLanguageISO639Identifier());
	    Utils.debug(selectedLanguageMatch.getConllColumn());
	    
	}
	
	// Set selected ModelMatch with context-menu or row-expansion event
	public void selectResourceModel(Object event) { 
		
		String eventType = event.getClass().toString();
		
		if (eventType.endsWith("SelectEvent")) {
		
			// The function is also triggered by the context menu event (show BLL-Info) 
			// in the row-expansion with file results for the model
			// Filter these events by checking the type of the selected object
			if (((SelectEvent) event).getObject().getClass().toString().endsWith("ModelMatch")) {
				selectedModelMatch = (ModelMatch) ((SelectEvent) event).getObject();	
			} else {
				// unwanted
				return;
			}
		}
		
		// Alternatively the model is selected by model row expansion event
		if (eventType.endsWith("ToggleEvent")) {
			
			if (((ToggleEvent) event).getData().getClass().toString().endsWith("ModelMatch")) {
				selectedModelMatch = (ModelMatch)  ((ToggleEvent) event).getData();	
			} else {
				// unwanted
				return;
			}
		}
	
		Utils.debug("selectResourceModel");
		Utils.debug(selectedModelMatch.getDetectionMethod().name());
	    Utils.debug(selectedModelMatch.getConllColumn());
	    
	}
	

	
	public void setResourceLanguage(ActionEvent event) {
		
		Utils.debug("Select language");
	    Utils.debug(selectedLanguageMatch.getLanguageISO639Identifier());
	    Utils.debug(selectedLanguageMatch.getConllColumn());

		selectedLanguageMatch.setSelected(true);
		selectedLanguageMatch.setDetectionMethod(DetectionMethod.MANUAL);
		
		
		if (selectedResource.getFileInfo().isConllFile()) {
		
			// deselect all other languages in column
			for (LanguageMatch lm : selectedResource.getFileInfo().getLanguageMatchings()) {
				if (lm.getConllColumn() == selectedLanguageMatch.getConllColumn()) {
					if (!lm.getLanguageISO639Identifier().equals(selectedLanguageMatch.getLanguageISO639Identifier())) {
						lm.setSelected(false);
					}
				}
			}
		}
		
	    selectedResource.getFileInfo().updateLanguageMatchingsAsString();
	}
	
	
	
	public void unsetResourceLanguage(ActionEvent event) {
		
		Utils.debug("Deselect language");
		Utils.debug(selectedLanguageMatch.getLanguageISO639Identifier());
		Utils.debug(selectedLanguageMatch.getConllColumn());
	    
		selectedLanguageMatch.setSelected(false);
		selectedLanguageMatch.setDetectionMethod(DetectionMethod.MANUAL);
	    selectedResource.getFileInfo().updateLanguageMatchingsAsString(); 
	}
	
	
	public void deleteResourceLanguage(ActionEvent event) {

	    Iterator <LanguageMatch> iterator = selectedResource.getFileInfo().getLanguageMatchings().iterator();
	    while (iterator.hasNext()) {
	    	LanguageMatch lm = iterator.next();
	    	if (lm.getTableID().equals(selectedLanguageMatch.getTableID())) {
	    		iterator.remove();
	    	}
	    }
	    
	    //update string representations
	    selectedResource.getFileInfo().setLanguageMatchings(selectedResource.getFileInfo().getLanguageMatchings());
	
	}
	
	
	public void showInfo(String message) {
		showMessage(message, FacesMessage.SEVERITY_INFO);
	}
	
	
	public void showWarning(String message) {
		showMessage(message, FacesMessage.SEVERITY_WARN);
	}
	
	
	public void showError(String message) {
		showMessage(message, FacesMessage.SEVERITY_ERROR);
	}
	
	
	public void showFatal(String message) {
		showMessage(message, FacesMessage.SEVERITY_FATAL);
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
	


	public String getMetaDataURLNew() {
		return metaDataURLNew;
	}


	public void setMetaDataURLNew(String metaDataURLNew) {
		this.metaDataURLNew = metaDataURLNew;
	}
	
	
	public StreamedContent getProcessStateImage() throws IOException {

		FacesContext context = FacesContext.getCurrentInstance();

		if (context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE) {
			return new DefaultStreamedContent();
		}

		else {

			String state = context.getExternalContext().getRequestParameterMap()
					.get("processState");
			Utils.debug("processState : "+state);

			byte[] image;
			String imagePath=null;
			
			switch (ProcessState.valueOf(state)) {
				
			case 	UNPROCESSED :
					imagePath = locateUtils.getLocalFile("/images/dot-blue.png").getAbsolutePath();
					//imagePath = new File(new File(fidConfig.getString("WebApp.iconFolder")),"dot-blue.png").getAbsolutePath();
					break;
				
			case	PROCESSED :
					imagePath = locateUtils.getLocalFile("/images/dot-yellow.png").getAbsolutePath();
					break;
			
			case	EDITED :
					imagePath =locateUtils.getLocalFile("/images/dot-red.png").getAbsolutePath();
					break;
			
			case	ACCEPTED :
					imagePath =locateUtils.getLocalFile("/images/dot-green.png").getAbsolutePath();
					break;
					
			case	CHECK :
					imagePath = locateUtils.getLocalFile("/images/dot-grey.png").getAbsolutePath();
				break;
				
			case	DISABLED :
					imagePath = locateUtils.getLocalFile("/images/dot-black.png").getAbsolutePath();
				break;
			
			default :
					Utils.debug("Error : unknown ProcessState : "+state);
					return null;
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
	}
	
	
	public void acceptResource() {

		if(selectedResource != null) {
			
			if(!canEdit) {
				showError("Editing is not allowed by owner !");
				return;
			}
			
			selectedResource.getFileInfo().setProcessState(ProcessState.ACCEPTED);
			resourceManager.updateProcessState(selectedResource);
			selectedResource.getFileInfo().setAcceptedDate(new Date());
			resourceManager.updateFileAcceptedDate(selectedResource);
		}
	}
	
	
	public void disableResource() {

		if(selectedResource != null) {
			
			if(!canEdit) {
				showError("Editing is not allowed by owner !");
				return;
			}
			
			selectedResource.getFileInfo().setProcessState(ProcessState.DISABLED);
			resourceManager.updateProcessState(selectedResource);
		}
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


	public int getSelectedLangCol() {
		return selectedLangCol;
	}


	public void setSelectedLangCol(int selectedLangCol) {
		// catch automatic call on start
		if (selectedLangCol == 0) return;
		
		this.selectedLangCol = selectedLangCol;
	}

	
	public void changeLanguageColumn(AjaxBehaviorEvent event) {
		
		Utils.debug("changeLanguageColumn");
		Utils.debug("selectedLangCol "+selectedLangCol);

		FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:a4727a");
		RequestContext.getCurrentInstance().reset("form:a4727a");
	}

	public void rescanResource() {
		
		Utils.debug("Rescan resource : "+selectedResource.getDataURL()+ " "+selectedResource.getFileInfo().getFileName());
		
		String dataUrl = selectedResource.getDataURL();
		String metaDataUrl = selectedResource.getMetaDataURL();
		
		HashSet<String> resources = new HashSet<String>();
		resources.add(dataUrl);
		rescanResourceImpl(resources);
		
    	selectedResource = null;
	}
	
	
	private void rescanResourceImpl(HashSet<String> resources) {
		
		// Delete resource(s) from rfl !
		Iterator <ResourceInfo> rt = rfl.iterator();
		while (rt.hasNext()) {
			ResourceInfo x = rt.next();
			if (resources.contains(x.getDataURL())) {
				Utils.debug("removing "+x.getDataURL()+" "+x.getFileInfo().getRelFilePath());
				rt.remove();
				
				// Queue resource for processing
				ResourceInfo resourceInfo = new ResourceInfo(x.getDataURL(), x.getMetaDataURL());
				
				// set forceRescan flag otherwise updatePolicy may cancel processing
				resourceInfo.getFileInfo().setForceRescan(true);
				
				enqueueResource(resourceInfo, true);
			}
		}
	}

	

	public void uploadResourceFile() {
		
		Utils.debug("upload file");
		
		try {
			
			String fileName = FileUploadBean.getUploadedFile().getFileName();
			String inputFilePath = new File (new File(fidConfig.getString("RunParameter.ServiceUploadDirectory")),fileName).getAbsolutePath();			
			ResourceInfo resourceInfo = new ResourceInfo("file://"+inputFilePath,"http://fid/metadata/tbc");

			// Write uploaded file to local fs
			FileUploadBean.getUploadedFile().write(inputFilePath);
			
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
				config.addProperty("RunParameter.urlPoolFile", "none");
				//config.addProperty("RunParameter.updatePolicy", updatePolicy.name());
				config.addProperty("Linghub.linghubQueries", "");
				config.addProperty("Linghub.useQueries", "false");
				config.addProperty("RunParameter.urlFilter", "RDF,CONLL,ARCHIVE,XML");
				
				UrlBroker urlBroker = new UrlBroker(resourceManager, config);
				HashSet<ResourceInfo> resourceInfoList = urlBroker.makeUrlPool();
				filterAlreadyQueuedResources(new ArrayList<ResourceInfo>(resourceInfoList));

				
				if (!resourceInfoList.isEmpty()) {
					showInfo("Found TSV file : Queuing "+resourceInfoList.size()+" resources for processing !");
					for (ResourceInfo resourceInfo_ : resourceInfoList) {
						
						// skip local files (TODO implement upload)
						if(IndexUtils.urlHasFileProtocol(resourceInfo_.getDataURL())) {continue;}
						
						Utils.debug("queuing resource : "+resourceInfo_.getDataURL());
						//rfl.add(resourceInfo_);
						enqueueResource(resourceInfo_, false);
					}
					guiTableRefresh();
					guiQueueRefresh();
				} else {
					showInfo("No resources found in "+inputFilePath+" !");
				}
				
				return;
			}
			
			if (resourceInfo.getResourceFormat() == ResourceFormat.UNKNOWN) {
				showError("File Format not supported !");
				return;
			} else {
				if (isInResourceDatabase("file://"+inputFilePath)) return;
				enqueueResource(resourceInfo, true);
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	

	public void uploadResourceUrl() {
		
		Utils.debug("upload url : "+uploadURL);
		if (isInResourceDatabase(uploadURL)) return;
		
		try {
			ResourceInfo resourceInfo = new ResourceInfo(uploadURL,"http://fid/metadata/tbc");
			if (resourceInfo.getResourceFormat() == ResourceFormat.UNKNOWN) {
				showError("File Format not supported !");
				// reset input field
				uploadURL="";
				return;
			} else {
				
				enqueueResource(resourceInfo, true);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// reset input field
		uploadURL="";
	}



//	@Override
//	public void onMessage(Message message) {
//		
//		ArrayList<ResourceInfo> updatedResources = new ArrayList<ResourceInfo>();
//		ResourceInfo resourceInfo = null;
//		
//		if (message instanceof ObjectMessage) {
//            ObjectMessage objectMessage = (ObjectMessage) message;
//            
//			try {
//				resourceInfo = (ResourceInfo) objectMessage.getObject();
//				
//				Utils.debug("Received finished ResourceInfo : "+resourceInfo.getDataURL());
//				
//				if (!resourceInfo.getFileInfo().getErrorMsg().isEmpty()) {
//					Utils.debug("received error : "+resourceInfo.getFileInfo().getErrorMsg());
//					messages.add("Error : "+resourceInfo.getDataURL()+"\n"+resourceInfo.getFileInfo().getErrorMsg());
//				}
//				
//				// Complete resourceInfo information for web-frontend
//				resourceInfo.getFileInfo().updateModelMatchingsAsString();
//				
//				updatedResources.add(resourceInfo);
//				executer.getWriter().getQueries().getFileResults(updatedResources, resourceManager);
//				
//				Utils.debug("parse result : "+resourceInfo.getFileInfo().getParseResult());
//				Utils.debug("data url : "+resourceInfo.getDataURL());
//				Utils.debug("file name : "+resourceInfo.getFileInfo().getFileName());
//				Utils.debug("file path: "+resourceInfo.getFileInfo().getRelFilePath());
//
//				
//				// Put successful resources in resource table
//				switch (resourceInfo.getFileInfo().getParseResult()) {
//				case SUCCESS :
//					
//					// add new resourceInfo to gui data table
//					rfl.addAll(updatedResources);
//					Utils.debug("Added resource "+resourceInfo.getDataURL());
//					
//					// check for metadata
//					
//					// in linghub
//					// query linghub
//					if (fidConfig.getBoolean("Linghub.enabled")) {
//						executer.getUrlBroker().sparqlLinghubAttributes(true, updatedResources);
//					}
//					
//					// in clarin
//					// query clarin
//					try {
//						//mng.updateClarinMetadata(updatedResources); accessUrl does not work
//					} catch (Exception e){}
//										
//					// TODO update bllMap (includes update hit2Class, hit2Tag - only for new Resource)
//					// TODO execute BllTools.extendFileResults(updatedResources);
//					
//					newResources++;
//					
//					break;
//				
//				case ERROR :
//				case NONE :
//					
//					if (resourceInfo.getResourceProcessState() != ResourceProcessState.ARCHIVE_FINISHED) {
//						
//						// Put unsuccessful resource in 'Error log' table
//						rflError.add(resourceInfo);
//						Utils.debug("Added resource "+resourceInfo.getDataURL());
//					}
//					break;
//				
//				default :	
//					break;
//					
//				}
//				
//				
//				resourceInfo.getFileInfo().verifyProcessState();
//				
//				dequeueResource(resourceInfo);
//				
//				Utils.debug("HIT nodes :"+executer.getWriter().getQueries().getHitNodes().size());
//				
//			} catch (JMSException e) {
//				e.printStackTrace();
//				// dequeue ?
//				dequeueResource(resourceInfo);
//				Utils.debug("Check dequeue is executed !");
//			}
//        }
//	}


	public String getUploadURL() {
		return uploadURL;
	}

	public void setUploadURL(String uploadURL) {
		this.uploadURL = uploadURL;
	}
	
	
	public void showMessages() {
		Utils.debug("showmessages");
		if (!messages.isEmpty()) {
			String text = "";
			for (String s : messages) {
				text += s+"\n";
			}
			messages.clear();
			showInfo(text);
		}
	}



	public String getCommentGui() {
		return commentGui;
	}


	public void setCommentGui(String commentGui) {
		this.commentGui = commentGui;
	}
	
	public void guiTableRefresh() {
		FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:a4711");
		RequestContext.getCurrentInstance().reset("form:a4711");
	}
	
	
	public void guiLogRefresh() {
		FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:a4821");
		RequestContext.getCurrentInstance().reset("form:a4821");
	}
	
	
	public void guiQueueRefresh() {
		FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:a4872");
		RequestContext.getCurrentInstance().reset("form:a4872");
	}
	
	
	
	public void guiRefreshButtonRefresh() {
		
	/*	CommandButton z = (org.primefaces.component.commandbutton.CommandButton) FacesContext.getCurrentInstance().getViewRoot().findComponent("form:rfb");

		if (newResources > 0) {
			z.setStyle("color:MediumSeaGreen");
			//Utils.debug("green");
		} else {
			z.setStyle("color:SlateGrey");
			//Utils.debug("black");
		}
		
		FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:rfb");
		RequestContext.getCurrentInstance().reset("form:rfb");*/
	}


	public List<ResourceInfo> getRflError() {
		return rflError;
	}


	public void setRflError(List<ResourceInfo> rflError) {
		LoginResourceInfoBean.rflError = rflError;
	}


	public List<ResourceInfo> getFilteredRflError() {
		return filteredRflError;
	}


	public void setFilteredRflError(List<ResourceInfo> filteredRflError) {
		LoginResourceInfoBean.filteredRflError = filteredRflError;
	}
	
	public void clearLog() {
		Utils.debug("Clear log");
		Iterator <ResourceInfo> rt = rflError.iterator();
		while (rt.hasNext()) {
			ResourceInfo x = rt.next();
			rt.remove();
		}
	}
	
	
	public List <ResourceInfo> getResourceQueue() {
		return resourceQueue;
	}

	
	private void enqueueResource(ResourceInfo resourceInfo, boolean updateGui) {
		
		if (filterAlreadyQueuedResources(resourceInfo)) return;
		
		resourceQueue.add(resourceInfo);
		executionEjb.addResource(resourceInfo);
		
		if (updateGui) {
			showInfo("Queuing resource for processing !");
			guiTableRefresh();
			guiQueueRefresh();
		}
	}
	
	private void dequeueResource(ResourceInfo resourceInfo) {
		
		if (resourceInfo == null) {
			Utils.debug("dequeueResource : Error resourceInfo = null");
			return;
		}
		
		Optional<ResourceInfo> x = resourceQueue.stream().filter(o -> o.getDataURL().equals(resourceInfo.getDataURL())).findFirst();
		
		if (x.isPresent()) {

			if (x.get().getFileInfo().getProcessState() == ProcessState.UNPROCESSED) { // always unprocessed 
				if (resourceInfo.getResourceProcessState() == ResourceProcessState.FINISHED ||
					resourceInfo.getResourceProcessState() == ResourceProcessState.ARCHIVE_FINISHED) {
					resourceQueue.remove(x.get());
					Utils.debug("Removed dummy resource "+x.get().getDataURL());
				}
			}
		}
	}
	
	
	private boolean filterAlreadyQueuedResources(ResourceInfo resourceInfo) {
		return filterAlreadyQueuedResources(new ArrayList<ResourceInfo>(){{add(resourceInfo);}});
	}
	
	/**
	 * Filter resources that are already queued for processing
	 * @param resourceInfoList
	 * @return true if filtering has been applied
	 */
	private boolean filterAlreadyQueuedResources(ArrayList<ResourceInfo> resourceInfoList) {
		
		Iterator<ResourceInfo> iterator = resourceInfoList.iterator();
		int filtered = 0;
		while(iterator.hasNext()) {
			ResourceInfo resourceInfo = iterator.next();
			for (ResourceInfo r : resourceQueue) {
				if (r.getDataURL().equals(resourceInfo.getDataURL())) {	
					iterator.remove();
					filtered++;
					break;
				}
			}
		}
		if (filtered == 0) return false;
		
		//showInfo("Found duplicates already in resource queue !");
		return true;
	}
	

	public String getManualISOInput() {
		return manualISOInput;
	}

	public void setManualISOInput(String manualISO) {
		manualISOInput = manualISO;
	}
	
	public void computeISOLanguage() {
		if (manualISOInput.length() != 3) {
			isoLanguageLabel= "unknown language";
		} else {
			isoLanguageLabel= ParserISONames.getIsoCodes2Names().get(manualISOInput);
		}
	}


	public String getIsoLanguageLabel() {
		return isoLanguageLabel;
	}

	public void setIsoLanguageLabel(String isoLangLabel) {
		isoLanguageLabel = isoLangLabel;
	}
	
	
	
	public void addManualISO() {

		if (isoLanguageLabel.equals("unknown language") 
				|| isoLanguageLabel.isEmpty()
				|| (selectedResource.getFileInfo().getIsConllFile() && selectedLangCol == 0)) {
			
			// reset input field
			manualISOInput= "";
			return;
		}
		
		int col;
		if (selectedResource.getFileInfo().isConllFile()) {
			col = selectedLangCol;
		} else {
			col = LanguageMatch.NOCOLUMN;
		}
	
		if (ParserISONames.getIsoCodes2Names().get(manualISOInput) != null) {
			
			// Check language already present
			ArrayList<LanguageMatch> languageList = selectedResource.getFileInfo().getLanguageMatchings();
			for (LanguageMatch lm : languageList) {
		    	if (col == lm.getConllColumn()) {
		    		if (manualISOInput.equals(lm.getLanguageISO639Identifier())) {
		    			showInfo("Language already present !");
		    			// reset input field
		    			manualISOInput= "";
		    			return;
		    		} else {
		    			if (selectedResource.getFileInfo().isConllFile()) {
		    				lm.setSelected(false);
		    			}
		    		}
		    	}
		    }
			
			// add new language to list
			try {
				LanguageMatch newLanguage = new LanguageMatch(manualISOInput, DetectionMethod.MANUAL);
			
				newLanguage.setAverageProb(1.0f);
				newLanguage.setConllColumn(col);
				
			    languageList.add(newLanguage);
			    selectedResource.getFileInfo().setLanguageMatchings(languageList);
			    		    
			    showInfo("Adding manual ISO-Code !");
			    
		    // will never happen because input is already checked
			} catch (InvalidLanguageException e) {
				showInfo("Error - invalid ISO-Code !");
			}
		}
		
		
		// reset input field
		manualISOInput= "";
	}
	
	
	
	public void addConllLangColumn () {
		Utils.debug("addlangcol");
		Utils.debug("newcol "+newLangConllColumn);
		
		// no free columns available
		if (selectedResource.getFileInfo().getIsConllFile() && newLangConllColumn == 0) return;
				
		selectedLangCol=newLangConllColumn;
		
		// add column as text column
		conllColumnsWithText.add(selectedLangCol);
		
		// remove column from free columns
		freeConllColumns.remove((Integer) selectedLangCol);
		
		FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:a4786");
		RequestContext.getCurrentInstance().reset("form:a4786");
		
		FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:a4727a");
		RequestContext.getCurrentInstance().reset("form:a4727a");
	}
	
	
	public void deleteConllLangColumn () {
		
		if(conllColumnsWithText.isEmpty()) return;
		//if (selectedResource.getFileInfo().getLanguageMatchings().isEmpty()) return;
		
		Utils.debug("deletelangcol "+selectedLangCol);
		
		// delete all languages in that column
		Iterator <LanguageMatch> iterator = selectedResource.getFileInfo().getLanguageMatchings().iterator();
		while (iterator.hasNext()) {
			// remove all languages in that column
			if (iterator.next().getConllColumn() == selectedLangCol) {
				iterator.remove();
			}
		}
		
		// update gui
		selectedResource.getFileInfo().updateLanguageMatchingsAsString();
		
		// delete column
		conllColumnsWithText.remove((Integer) selectedLangCol);
		
		// add column to free columns
		freeConllColumns.add(selectedLangCol);
		
		// set new selected column
		if (conllColumnsWithText.isEmpty()) {
			selectedLangCol = 0;
		} else {
			selectedLangCol = conllColumnsWithText.get(0);
		}
		
		
		
		// add + reset works ! (only one of either does not work !)
		FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:a4715");// actually only input fields must be updated
		RequestContext.getCurrentInstance().reset("form:a4715");// actually only input fields must be updated
		
		FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:a4786");
		RequestContext.getCurrentInstance().reset("form:a4786");
		
		FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:a4727a");
		RequestContext.getCurrentInstance().reset("form:a4727a");
		
	}
	
	
	public void addConllModelColumn () {
		Utils.debug("addmodelcol");
		Utils.debug("newcol "+newModelConllColumn);
		
		// no free columns available
		if (selectedResource.getFileInfo().getIsConllFile() && newModelConllColumn == 0) return;
				
		selectedCol=newModelConllColumn;
		
		// add column as text column
		conllColumnsWithModels.add(selectedCol);
		
		// remove column from free columns
		freeConllColumns.remove((Integer) selectedCol);
		
		FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:a4788");
		RequestContext.getCurrentInstance().reset("form:a4788");
		
		FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:a792");
		RequestContext.getCurrentInstance().reset("form:a792");
	}
	
	
	
	public void deleteConllModelColumn () {
		
		if(conllColumnsWithModels.isEmpty()) return;
		//if (selectedResource.getFileInfo().getLanguageMatchings().isEmpty()) return;
		
		Utils.debug("deletemodelcol "+selectedCol);
		
		// delete all models in that column
		Iterator <ModelMatch> iterator = selectedResource.getFileInfo().getModelMatchings().iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getConllColumn() == selectedCol) {
				iterator.remove();
			}
		}
		
		// update gui
		selectedResource.getFileInfo().updateModelMatchingsAsString();
		
		// delete column
		conllColumnsWithModels.remove((Integer) selectedCol);
		
		// add column to free columns
		freeConllColumns.add(selectedCol);
		
		// set new selected column
		if (conllColumnsWithModels.isEmpty()) {
			selectedCol = 0;
		} else {
			selectedCol = conllColumnsWithModels.get(0);
		}
		
		
		
		// add + reset works ! (only one of either does not work !)
		FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:a4715");// actually only input fields must be updated
		RequestContext.getCurrentInstance().reset("form:a4715");// actually only input fields must be updated
		
		FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:a4788");
		RequestContext.getCurrentInstance().reset("form:a4788");
		
		FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:a792");
		RequestContext.getCurrentInstance().reset("form:a792");
		
	}
	

	public int getNewLangConllColumn() {
		return newLangConllColumn;
	}

	public void setNewLangConllColumn(int newConllColumn_) {
		newLangConllColumn = newConllColumn_;
		Utils.debug("new column "+newLangConllColumn);
	}



	public ArrayList <Integer> getConllColumnsWithText() {
		return conllColumnsWithText;
	}



	public void setConllColumnsWithText(ArrayList <Integer> conllColumnsWithText) {
		this.conllColumnsWithText = conllColumnsWithText;
	}
	
	
	public void skipValidator(FacesContext context, UIComponent component, Object value) throws ValidatorException {}



	public ArrayList <Integer> getFreeConllColumns() {
		return freeConllColumns;
	}



	public void setFreeConllColumns(ArrayList <Integer> freeConllColumns) {
		this.freeConllColumns = freeConllColumns;
	}



	public ArrayList<Integer> getConllColumnsWithModels() {
		return conllColumnsWithModels;
	}



	public void setConllColumnsWithModels(
			ArrayList<Integer> conllColumnsWithModels) {
		this.conllColumnsWithModels = conllColumnsWithModels;
	}



	public ModelType getNewModel() {
		return newModel;
	}



	public void setNewModel(ModelType newModel) {
		this.newModel = newModel;
	}


	public Integer getNewModelConllColumn() {
		return newModelConllColumn;
	}



	public void setNewModelConllColumn(Integer newModelConllColumn) {
		this.newModelConllColumn = newModelConllColumn;
	}
	
	// stop works immediately/ start requires additional reload of web-page !
	public void toggleAutoUpdate() {
		
		/** INSERT CODE under <p:commandButton id="rfb" ...
		 * 	            	
		   <p:menuitem value="Auto-Update ON/OFF" action="#{rinfo.toggleAutoUpdate}" icon="ui-icon-refresh"/>
		 */
		
		noPolling = !noPolling;
		Utils.debug("nopolling : "+noPolling);
		if(noPolling) {
			showInfo("Auto-Update is OFF !");
		} else {
			showInfo("Auto-Update is ON !");
		}
		FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:poll");// actually only input fields must be updated
		RequestContext.getCurrentInstance().reset("form:poll");// actually only input fields must be updated
	
	}



	public boolean getNoPolling() {
		return noPolling;
	}
	
	public boolean getPolling() {
		return !noPolling;
	}
	
	
	public String getDbStats() {
		
		/* INSERT
    	 *<p:menuitem value="Statistic" update="a4531" icon="ui-icon-gear" oncomplete="PF('showStats').show()"/>
		*/
		
		if (selectedResource == null) return "";
		
		// Resources in database : rid
		// Resources with found languages or model Results : ridplus
		// Resource with no results : ridminus
		// File type   with-results   without-results
		// RDF             rplus              rminus
		// CONLL           cplus              cminus
		// XML             xplus              xminus
		// OTHER           oplus              ominus
		
		// Model types found : mtf
		// list of model shortcuts
		// Language types found : ltf
		// list of iso codes
		
		int parsedFileCount, parsedResourceCount, ridplus, ridminus, rplusl, rminus, cplusl, cminus, xplusl, xminus, oplusl, ominus, rplusm, cplusm, xplusm, oplusm, mtf, ltf;
		parsedFileCount = parsedResourceCount = ridplus = ridminus = rplusl = rminus = cplusl = cminus = xplusl = xminus = oplusl = ominus = rplusm = cplusm = xplusm = oplusm = mtf = ltf =0;
		
		int corpora, lexica, ontologies, unknownResourceType;
		corpora = lexica = ontologies = unknownResourceType = 0;
		
		int rdf, conll, xml, other, xmlreal, xmlfromattr;
		rdf = conll = xml = other = xmlreal = xmlfromattr= 0;
		

		
		HashSet <String> modelTypes = new HashSet<String>();
		HashSet <String> languageCodes = new HashSet<String>();
		HashSet <String> vocabularyTypes = new HashSet<String>();
		HashMap <String, ResourceInfo> differentSuccessFullResources = new HashMap<String, ResourceInfo>();
		
		int fromUser = 0;
		int fromClarin = 0;
		int fromLinghub = 0;
		int fromNone = 0;
		
		try {
			parsedResourceCount = resourceManager.getDoneResourceCount();
			parsedFileCount = resourceManager.getDoneFileResourceCount();
			ridminus = parsedFileCount - rfl.size();
			xmlreal = resourceManager.getXMLResourcesWithFileFormatXML();
			xml = xmlreal;  // other xml added below in case CONLL
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
		
		//xplusm = resourceManager.getXMLResourcesWithSelectedModelsCount();
		//xplusl = resourceManager.getXMLResourcesWithSelectedLanguagesCount();
		//xminus = resourceManager.getXMLResourcesWithFileFormatXML()+
		//		 resourceManager.getXMLResourcesWithUnselectedModelAndLanguageCount();
		//xmlfromattr =  resourceManager.getXMLResourcesWithModelOrLanguageCount();
		
		xminus = -1;
		boolean isXml;
		
		int unprocessed, processed, edited, disabled, check, excluded, accepted;
		unprocessed = processed = edited = disabled = check = excluded = accepted = 0;
		
		String result="";
		
		
		for (ResourceInfo r : rfl) {
			
			differentSuccessFullResources.put(r.getDataURL(), r);
			
			switch(r.getResourceMetadata().getMetadataSource()) {
			
			case LINGHUB : fromLinghub++;
				break;
			
			case CLARIN : fromClarin++;
				break;
				
			case USER : fromUser++;
				break;
				
			case NONE : fromNone++;
				break;
				
			default :
				break;
			}
			
			FileInfo f = r.getFileInfo();
			
			for (VocabularyMatch vm : f.getVocabularyMatchings()) {
				vocabularyTypes.add(vm.getVocabulary().name());
			}
			
			
			
			switch (f.getProcessState()) {
			
			case UNPROCESSED : unprocessed++;
				break;
			case PROCESSED : processed++;
				break;
			case EDITED : edited++;
				break;
			case DISABLED : disabled++;
				break;
			case ACCEPTED : accepted++;
				break;
			case CHECK : check++;
				break;				
			case EXCLUDED : excluded++;
				break;	
				
			default : 
				break;
			}
			
			switch (f.getResourceType()) {
			
			case CORPUS : corpora++;
				break;
			case LEXICON : lexica++;
				break;
			case ONTOLOGY : ontologies++;
				break;
			case UNKNOWN : unknownResourceType++;
				break;
			default : unknownResourceType++;
				break;
			}
			
			isXml = false;
			
			switch (f.getProcessingFormat()) {
			
			case RDF : rdf++;
				if (f.getSelectedLanguages().size() > 0) {rplusl++;}
				if (f.getSelectedModels().size() > 0) {rplusm++;} else {
					if (f.getSelectedLanguages().size() == 0) rminus++;
				}
				for (ModelMatch mm : f.getSelectedModels()) {
					modelTypes.add(mm.getModelType().name());
				}
				for (LanguageMatch lm : f.getSelectedLanguages()) {
					languageCodes.add(lm.getLanguageISO639Identifier());
				}
				break;
			
			case CONLL : 
				
				if (f.getSelectedModels().size() > 0) {
					if(f.getSelectedModels().get(0).getXmlAttribute().trim().isEmpty()) {
						cplusm++;
					} else {
						xplusm++;isXml=true;
					}	
				}
				
				if (f.getSelectedLanguages().size() > 0) {
					if(f.getSelectedLanguages().get(0).getXmlAttribute().trim().isEmpty() && isXml==false) {
						cplusl++;
					} else {
						xplusl++;isXml=true;
					}
				}
				 else {
					/*if (f.getSelectedLanguages().size() == 0) {
						cminus++;
					}*/
				}
				
				if (isXml) xml++;
				else conll++;
				
				
				for (ModelMatch mm : f.getSelectedModels()) {
					modelTypes.add(mm.getModelType().name());
				}
				for (LanguageMatch lm : f.getSelectedLanguages()) {
					languageCodes.add(lm.getLanguageISO639Identifier());
				}
				break;
				
			case XML : 
				/* xml++;
				 * if (f.getSelectedLanguages().size() > 0) {xplusl++;}
				if (f.getSelectedModels().size() > 0) {xplusm++;} else {
					if (f.getSelectedLanguages().size() == 0) xminus++;
				}
				for (ModelMatch mm : f.getSelectedModels()) {
					modelTypes.add(mm.getModelType().name());
				}
				for (LanguageMatch lm : f.getSelectedLanguages()) {
					languageCodes.add(lm.getLanguageISO639Identifier());
				}*/
				break;
				
			default : other++;
				if (f.getSelectedLanguages().size() > 0) {oplusl++;}
				if (f.getSelectedModels().size() > 0) {oplusm++;} else {
					if (f.getSelectedLanguages().size() == 0) ominus++;
				}
				for (ModelMatch mm : f.getSelectedModels()) {
					modelTypes.add(mm.getModelType().name());
				}
				for (LanguageMatch lm : f.getSelectedLanguages()) {
					languageCodes.add(lm.getLanguageISO639Identifier());
				}
				break;
			
			}
		}
		
		modelTypes.remove("UNKNOWN");
		
		String modelTypesString = "";
		for (String m : modelTypes) {
			modelTypesString += m+",";
		}
		if (!modelTypesString.trim().isEmpty())
			modelTypesString = modelTypesString.substring(0, modelTypesString.length()-1);
		
		String languageCodesString = "";
		for (String l : languageCodes) {
			languageCodesString += l+",";
		}
		if (!languageCodesString.trim().isEmpty())
			languageCodesString = languageCodesString.substring(0, languageCodesString.length()-1);
		
		String vocabularyTypesString = "";
		for (String v : vocabularyTypes) {
			vocabularyTypesString += v+",";
		}
		try {if (!vocabularyTypesString.trim().isEmpty()) {
			vocabularyTypesString = vocabularyTypesString.substring(0, vocabularyTypesString.length()-1);
		}
		} catch (Exception e) {
			e.printStackTrace();}
		
		
		
		
		String output = "";
		output +="Parsed resources : "+parsedResourceCount+"\n";
		output +="with results : "+differentSuccessFullResources.keySet().size()+"\n";
		output +="without results : "+(parsedResourceCount - differentSuccessFullResources.keySet().size())+"\n\n";
		output +="Parsed files : "+parsedFileCount+"\n";
		output +="with results : "+rfl.size()+"\n";
		output +="without results : "+(parsedFileCount-rfl.size())+"\n\n";		

		output +="Parsed files with language or model result :\n";
		output +="RDF : "+rdf+"\n";
		output +="CoNLL : "+conll+"\n";
		output +="XML : " +xml+"\n";
		output +="Other : "+(rfl.size()-rdf-conll-xml)+"\n";
		output +="\n";
		output +="Detailed results by file type :\n"; 
		output +="File type        language found        model found\n";
		output +="RDF              "+rplusl+"             "+rplusm+"\n";
		output +="CoNLL            "+cplusl+"             "+cplusm+"\n";
		output +="XML              "+xplusl+"             "+xplusm+"\n";
		output +="\n";
		output +="Edit status of files :\n";
		output +="UNPROCESSED : "+unprocessed+"\n";
		output +="PROCESSED : "+processed+"\n";
		output +="EDITED : "+edited+"\n";
		output +="DISABLED : "+ disabled+"\n";
		output +="CHECK : "+check+"\n";
		output +="ACCEPTED : "+ accepted+"\n";
		output +="\n";
		output +="Found resource types :\n";
		output +="Corpora : "+corpora+"\n";
		output +="Lexica : "+lexica+"\n";
		output +="Ontologies : "+ontologies+"\n";
		output +="Unknown type : "+unknownResourceType+"\n\n";
		output +="\nMetadata information (ALL) :\n";
		output +="From linghub : "+fromLinghub+"\n";
		output +="From CLARIN : "+fromClarin+"\n";
		output +="From User : "+fromUser+"\n\n";
		result = getMetadataStatsInfo (differentSuccessFullResources, null);
		output += "Complete : "+result.split("#")[0]+"\n";
		output += "Sufficient : "+result.split("#")[1]+"\n";
		output += "Incomplete : "+result.split("#")[2]+"\n";
		output += "Empty : "+result.split("#")[3]+"\n";
		output +="\nMetadata information (ACCEPTED) :\n";
		result = getMetadataStatsInfo (differentSuccessFullResources, ProcessState.ACCEPTED);
		output += "Complete : "+result.split("#")[0]+"\n";
		output += "Sufficient : "+result.split("#")[1]+"\n";
		output += "Incomplete : "+result.split("#")[2]+"\n";
		output += "Empty : "+result.split("#")[3]+"\n";
		output +="\nMetadata information (PROCESSED) :\n";
		result = getMetadataStatsInfo (differentSuccessFullResources, ProcessState.PROCESSED);
		output += "Complete : "+result.split("#")[0]+"\n";
		output += "Sufficient : "+result.split("#")[1]+"\n";
		output += "Incomplete : "+result.split("#")[2]+"\n";
		output += "Empty : "+result.split("#")[3]+"\n";
		output +="\n";
		output +="Different model types found : "+modelTypes.size()+"\n";
		output +="Different languages found : "+languageCodes.size()+"\n";
		output +="Different vocabularies found : "+vocabularyTypes.size()+"\n";
		output += "\n";
		output +="Models :"+"\n";
		output +=modelTypesString+"\n";
		output += "\n";
		output +="Languages (ISO-639-3) :"+"\n";
		output +=languageCodesString+"\n";
		output += "\n";
		output +="Vocabularies :"+"\n";
		output +=vocabularyTypesString+"\n";
		
		
		
		return output;
	}
	
	
	@Schedule(hour="*/1")
    public void automaticTimeoutJSON() {
		//publishJson(false);	
    }
	
	
	@Schedule(hour="*/12")
    public void automaticTimeoutRDF() {
		//exportRDF();
    }
	
	
	public void publishJsonM() {
		exportJSON(true);
		//exportRDF(); // testing
	}
	
	public void publish() {
		exportRDF();
		exportJSON(true);
	}
	
	private void exportJSON(boolean fromGui) {
		
		// Wait until jsonFilePath is initialized (in init method)
		if (jsonFilePath == null || jsonFilePath.isEmpty()) return;
				
		HashSet<ProcessState> allowedProcessStates = new HashSet<ProcessState>();
		allowedProcessStates.add(ProcessState.ACCEPTED);
		
		try {
			for (ResourceInfo rin : rfl) {
				//Utils.debug(rin.getDataURL()+" : "+rin.getResourceFormat());
			}
			String exportString = JSONSerializer.serializeResourceInfos2JSON(
					rfl,
					allowedProcessStates,
					executer.getModelDefinition()
					);
	

		Utils.debug("Saving JSON export to file "+jsonFilePath+ " ...");
		try (FileWriter writer = new FileWriter(jsonFilePath);
	             BufferedWriter bw = new BufferedWriter(writer)) {

	            bw.write(exportString);
	            Utils.debug("finished !");
	            
	            if (fromGui) {
	            	showInfo("ACCEPTED data published successfully !");
	            }
	        } catch (IOException e) {
	        	Utils.debug("jsonFilePath : "+jsonFilePath);
	            System.err.format("IOException: %s%n", e);
	        }
		} catch (Exception e) {
			e.printStackTrace();
			if (fromGui) {
				showInfo("Publish Error : "+e.getMessage().substring(0, Math.min(40, +e.getMessage().length()-1)));
			}
		}
	}
	
	public ArrayList<String> getMultiProcessActions() {
		ArrayList<String> actions = new ArrayList<String>();
		actions.add("Mark ACCEPTED");
		actions.add("Mark DISABLED");
		actions.add("Mark PROCESSED");
		//actions.add("Rescan");
		actions.add("Delete");
		//actions.add("Copy Metadata");
		//actions.add("Copy Results");
		return actions;
	}
	
	
	public void testMultiACD() {
		
		if(selectedResource == null) return;
		
		if(!canEdit) {
			showError("Editing is not allowed by owner !");
			return;
		}
		
    	HashSet<String> resourceList = resourceManager.getGlobalProcessState(selectedResource, selectedMultiAcdURL);
    	ArrayList<String> results = new ArrayList<String>();
    	multiACDInfo = "";
    	int id = 1;
    	for (ResourceInfo rs : rfl) {
    		if (resourceList.contains(rs.getDataURL())) {
    			results.add(rs.getDataURL()+"->"+rs.getFileInfo().getRelFilePath());
    		}
    	}
    	Collections.sort(results);
    	for (String result : results) {
    		multiACDInfo+=""+String.format("%1$-7s", id++)+result+"\n";
    	}
    	
	}
	
	
	public void multiACD() {
				
		Utils.debug("state:"+selectedMultiAcdState);
		Utils.debug("url:"+selectedMultiAcdURL+" !");
		
		if(selectedResource == null) return;
		
		// reset
		multiACDResources.clear();
		multiACDResources2.clear();

		
		HashSet<String> resourceList = resourceManager.getGlobalProcessState(selectedResource, selectedMultiAcdURL);
		for (ResourceInfo rs : rfl) {
			
			if (!resourceList.contains(rs.getDataURL())) continue;
		
			switch (selectedMultiAcdState) {
				case "Mark ACCEPTED" :
					
					rs.getFileInfo().setProcessState(ProcessState.ACCEPTED);
					resourceManager.updateProcessState(rs);
					rs.getFileInfo().setAcceptedDate(new Date());
					resourceManager.updateFileAcceptedDate(rs);
					break;
				
				case "Mark DISABLED" :
					rs.getFileInfo().setProcessState(ProcessState.DISABLED);
					resourceManager.updateProcessState(rs);
					break;
					
				case "Mark PROCESSED" :
					rs.getFileInfo().setProcessState(ProcessState.PROCESSED);
					resourceManager.updateProcessState(rs);
					break;
					
				case "Rescan" :
				case "Delete" :
				case "Copy Metadata" :
					multiACDResources.add(rs.getDataURL());
					Utils.debug("acd-resources : "+rs.getDataURL());
					break;
					
				/*case "Copy Results" :
					multiACDResources2.add(rs);
					break;*/
					
				default :
				 break;
			}
		}
		// delayed rescan operation
		if (selectedMultiAcdState.equals("Rescan")) {
			rescanResourceImpl(multiACDResources);
		}
		
		// delayed delete operation
		if (selectedMultiAcdState.equals("Delete")) {
			deleteResourceImpl(multiACDResources);
		}
		
		// delayed copy meta-data operation
		if (selectedMultiAcdState.equals("Copy Metadata")) {
			// update resource metadata in database & update table
			copyResourceMetadataImpl(multiACDResources, selectedResource.getResourceMetadata());
		}
		
		// delayed copy results operation
		/*if (selectedMultiAcdState.equals("Copy Results")) {
			
			String modus = "all";
			saveResourceEditForAllImpl(selectedResource, multiACDResources2, modus);
		}*/
		
	}
	
	
	private void updateTableMetadata(Set<String> resourceIdentifiers, ResourceMetadata linghubAttributes) {
	
		for (String y : resourceIdentifiers) {
			Utils.debug("updateTableMetadata:"+y);
		}
		// update meta-data for all other files in resource
		Iterator <ResourceInfo> rt = rfl.iterator();
		
		while (rt.hasNext()) {
			
			ResourceInfo x = rt.next();
			if (resourceIdentifiers.contains(x.getDataURL())) {
				
				// copy attributes instead of copying object (do not link meta-data)
				copyLinghubAttributes(x,linghubAttributes);  //x.setLinghubAttributes(linghubAttributes);
				x.setMetaDataURL(ResourceManager.MetaDataFromUser);
				//Utils.debug("+++"+x.getFileInfo().getAbsFilePath());
			}
		}
	}
	
	
	// Copy all linghub attributes
	private void copyLinghubAttributes(ResourceInfo rs, ResourceMetadata attributes) {
		
		rs.getResourceMetadata().setFormat(attributes.getFormat());
		rs.getResourceMetadata().setTitle(attributes.getTitle());
		rs.getResourceMetadata().setType(attributes.getType());
		rs.getResourceMetadata().setRights(attributes.getRights());
		rs.getResourceMetadata().setPublisher(attributes.getPublisher());
		rs.getResourceMetadata().setDescription(attributes.getDescription());
		rs.getResourceMetadata().setCreator(attributes.getCreator());
		rs.getResourceMetadata().setContributor(attributes.getContributor());
		rs.getResourceMetadata().setLocation(attributes.getLocation());
		rs.getResourceMetadata().setYear(attributes.getYear());
		rs.getResourceMetadata().setEmailContact(attributes.getEmailContact());
		rs.getResourceMetadata().setWebpage(attributes.getWebpage());
		rs.getResourceMetadata().setDate(attributes.getDate());
		rs.getResourceMetadata().setDctLanguageString(attributes.getDctLanguageString());
		rs.getResourceMetadata().setDcLanguageString(attributes.getDcLanguageString());
		rs.getResourceMetadata().setMetadataSource(attributes.getMetadataSource());
	}
	
	
	public void saveResourceEditForAll(String modus) {
		
		
		Utils.debug("saveResourceEditForAll : "+modus);
		
		if(!canEdit) {
			showError("Editing is not allowed by owner !");
			return;
		}
		
		ArrayList<ResourceInfo> resourcefiles = new ArrayList<ResourceInfo>();
		for (ResourceInfo rs : rfl) {
			if (rs.getDataURL().equals(selectedResource.getDataURL())) resourcefiles.add(rs);
		}
		resourcefiles.remove(selectedResource);
		
		// copy and save  edit of selected resource to all other files of resource 
		saveResourceEditForAllImpl(selectedResource, resourcefiles, modus);
		
		// finally save selected Resource
		saveResourceEdit();
		
	}
	
	/**
	 * Make a selection of languages / models for all files in a resource 
	 * @param source
	 * @param targets
	 * @param modus both|lang|model
	 */
	private void saveResourceEditForAllImpl (ResourceInfo source, ArrayList<ResourceInfo> targets, String modus) {
		
		
		Utils.debug("saveResourceEditForAllImpl");
		
		// only for CONLL files
		if (!selectedResource.getFileInfo().isConllFile()) {
			showInfo("Only for CONLL resources !");
			return;
		}
		
		
		Utils.debug("source :");
		for (LanguageMatch x : selectedResource.getFileInfo().getSelectedLanguages()) {
			Utils.debug(x.getConllColumn()+":"+x.getLanguageISO639Identifier());
		}
		
		
		// Get language columns of source resource
		HashSet<Integer> langColumns = new HashSet<Integer>(selectedResourceOrg.getFileInfo().getActiveConllColumnsWithText());

		// Get model columns of source resource
		HashSet<Integer> modelColumns = new HashSet<Integer>(selectedResourceOrg.getFileInfo().getActiveConllColumnsWithModels());
		
		ArrayList<ResourceInfo> targetList = new ArrayList<ResourceInfo>();
		Utils.debug("sc:"+selectedResourceOrg.getFileInfo().getFileName());
		
		// filter target resources that have same text and model columns as source
		for (ResourceInfo rs : targets) {
			
			// filter files that have the same language and model columns !
			// if result of target resource has same language and model columns
			// update language and model results and mark as edited
			// in contrast : if result format is different then skip resource
			if (new HashSet<Integer>(rs.getFileInfo().getActiveConllColumnsWithText()).equals(langColumns)
				&&
				new HashSet<Integer>(rs.getFileInfo().getActiveConllColumnsWithModels()).equals(modelColumns)
				){
				targetList.add(rs);
				/*
				Utils.debug("langcolumns source");
				for (int c : langColumns) {
					Utils.debug(c);
				}
				Utils.debug("modelcolumns source");
				for (int c : modelColumns) {
					Utils.debug(c);
				}
				Utils.debug("adding target resource :"+rs.getFileInfo().getFileName());
				Utils.debug("langcolumns target");
				for (int c : rs.getFileInfo().getActiveConllColumnsWithText()) {
					Utils.debug(c);
				}
				Utils.debug("modelcolumns target");
				for (int c : rs.getFileInfo().getActiveConllColumnsWithModels()) {
					Utils.debug(c);
				}*/
			}
		}
		
		
		// Apply language changes
		// How it works :
		// Add missing language columns in target
		// Remove not existing target language columns in target
		// Change target language to state of source language (if target language 
		if (modus.toLowerCase().equals("both") || modus.toLowerCase().equals("lang")) {
			
			for (ResourceInfo rs : targetList) {
				
				// Unselect all target languages of resource
				for (LanguageMatch lmt : rs.getFileInfo().getLanguageMatchings()) {
					lmt.setSelected(false);
				}
				
				// copy source languages
				for (LanguageMatch lm : source.getFileInfo().getLanguageMatchings()) {
				
					// source language column exists in target ?
					if (rs.getFileInfo().getConllColumnsWithText().contains(lm.getConllColumn())) {
						// yes
						boolean found = false;
						for (LanguageMatch lmt : rs.getFileInfo().getLanguageMatchingsForColumn(lm.getConllColumn())) {
							
							//Utils.debug("dklgfjlsdgf :"+lm.getConllColumn()+":"+lm.getLanguageISO639Identifier());
							if (lmt.getLanguageISO639Identifier().equals(lm.getLanguageISO639Identifier())) {
								
								found = true;
								
								// copy source state (selected/unselected) to target
								lmt.setSelected(lm.getSelected());
								Utils.debug("Select : "+lm.getConllColumn()+":"+lm.getLanguageISO639Identifier());
								break;
							}	
						}
						if (!found) { // language was not found in target
							
							// only copy selected languages to target
							if (lm.isSelected()) {
								ArrayList<LanguageMatch> olm = rs.getFileInfo().getLanguageMatchings();
								olm.add((LanguageMatch) SerializationUtils.clone(lm));
								Utils.debug("Add : "+lm.getConllColumn()+":"+lm.getLanguageISO639Identifier());
								rs.getFileInfo().setLanguageMatchings(olm);
							}
						}
						
					} else {
						Utils.debug("no");
						// copy language column from source to target (even if no language is selected !)
						ArrayList<LanguageMatch> olm = rs.getFileInfo().getLanguageMatchings();
						for (LanguageMatch lmc : source.getFileInfo().getLanguageMatchingsForColumn(lm.getConllColumn())) {
							olm.add((LanguageMatch) SerializationUtils.clone(lmc));
							Utils.debug("Add new column : "+lmc.getConllColumn()+":"+lmc.getLanguageISO639Identifier());
						}
						rs.getFileInfo().setLanguageMatchings(olm);
					}
				}
			
				//Utils.debug("source language columns : "+source.getFileInfo().getConllColumnsWithText());
				//Utils.debug("x : "+targetList.size());
				
				// Remove all language colums in target resources that are not present in source
				Utils.debug("rs "+rs.getFileInfo().getRelFilePath());
				
				ArrayList<LanguageMatch> languageMatchings = rs.getFileInfo().getLanguageMatchings();
				Iterator <LanguageMatch> iterator = languageMatchings.iterator();
				
				while (iterator.hasNext()) {
					LanguageMatch lmt = iterator.next();
					if (!source.getFileInfo().getConllColumnsWithText().contains(lmt.getConllColumn())) {
						Utils.debug("remove : "+lmt.getConllColumn());
						iterator.remove();
					}
				}
				// update languages in target
				rs.getFileInfo().setLanguageMatchings(languageMatchings);
				//Utils.debug("target language columns : "+rs.getFileInfo().getConllColumnsWithText());

				// write update to DB
				// TODO : save single languages
				resourceManager.
				updateFileLanguages(rs.getDataURL(), rs.getFileInfo().getRelFilePath(), rs.getFileInfo(), true);
			
				// set process state edited
				rs.getFileInfo().setProcessState(ProcessState.EDITED);
				resourceManager.updateProcessState(rs);
			}
			
		}


		// Apply model changes
		if (modus.toLowerCase().equals("both") || modus.toLowerCase().equals("model")) {
			
		for (ResourceInfo rs : targetList) {
					
			// Unselect all target models of resource
			for (ModelMatch mmt : rs.getFileInfo().getModelMatchings()) {
				mmt.setSelected(false);
			}
			
			// copy source models
			for (ModelMatch mm : source.getFileInfo().getModelMatchings()) {
			
				// source model column exists in target ?
				if (rs.getFileInfo().getConllColumnsWithModels().contains(mm.getConllColumn())) {
					// yes
					boolean found = false;
					for (ModelMatch lmt : rs.getFileInfo().getModelMatchingsForColumn(mm.getConllColumn())) {
						
						//Utils.debug("sdklgfjlsdgfs :"+mm.getConllColumn()+":"+mm.getModelType().name());
						
						if (lmt.getModelType().name().equals(mm.getModelType().name())) {
							found = true;
							// copy source state (selected/unselected) to target
							lmt.setSelected(mm.isSelected());
							Utils.debug("Select : "+mm.getConllColumn()+":"+mm.getModelType().name());
							break;
						}	
					}
					if (!found) { // model was not found in target
						// TODO recomputation of models in column needed instead of copying
						// only copy selected models to target
						if (mm.isSelected()) {
							ArrayList<ModelMatch> olm = rs.getFileInfo().getModelMatchings();
							olm.add((ModelMatch) SerializationUtils.clone(mm));
							Utils.debug("Add : "+mm.getConllColumn()+":"+mm.getModelType().name());
							rs.getFileInfo().setModelMatchings(olm);
						}
					}
					
				} else {
					Utils.debug("no");
					// no
					// TODO recomputation of models in column needed instead of copying
					// copy language column from source to target
					ArrayList<ModelMatch> olm = rs.getFileInfo().getModelMatchings();
					for (ModelMatch lmc : source.getFileInfo().getModelMatchingsForColumn(mm.getConllColumn())) {
						olm.add((ModelMatch) SerializationUtils.clone(lmc));
						Utils.debug("Add new column : "+lmc.getConllColumn()+":"+lmc.getModelType().name());
					}
					rs.getFileInfo().setModelMatchings(olm);
				}
			}
		
			//Utils.debug("source model columns : "+source.getFileInfo().getConllColumnsWithModels());
			//Utils.debug("x : "+targetList.size());
			
			// Remove all model colums in target resources that are not present in source
			Utils.debug("rs "+rs.getFileInfo().getRelFilePath());
			
			ArrayList<ModelMatch> modelMatchings = rs.getFileInfo().getModelMatchings();
			Iterator <ModelMatch> iterator = modelMatchings.iterator();
			
			while (iterator.hasNext()) {
				ModelMatch lmt = iterator.next();
				if (!source.getFileInfo().getConllColumnsWithModels().contains(lmt.getConllColumn())) {
					Utils.debug("remove : "+lmt.getConllColumn());
					iterator.remove();
				}
			}
			// update models in target
			rs.getFileInfo().setModelMatchings(modelMatchings);
			Utils.debug("target models columns : "+rs.getFileInfo().getConllColumnsWithModels());
			
			// write update to DB
			resourceManager.
			updateFileModels(rs.getDataURL(), rs.getFileInfo().getRelFilePath(), rs.getFileInfo(), true);
			
			// set process state edited
			rs.getFileInfo().setProcessState(ProcessState.EDITED);
			resourceManager.updateProcessState(rs);
		}
		
		// Save comment to all files
		for (ResourceInfo rs : targetList) {
			rs.getFileInfo().setComment(getCommentGui());
			resourceManager.updateFileComment(rs);
		}
		
		}
		
	}
	
	
	public String refreshButton() {
		newResources=0;
		return "loginresources?faces-redirect=true";
	}
	
	
	public String getselectedMultiAcdState() {
		return selectedMultiAcdState;
	}
	
	
	public void setSelectedMultiAcdState(String state) {
		selectedMultiAcdState=state;
	}
	
	public String getSelectedMultiAcdURL() {
		if (selectedResource != null) {
			return selectedResource.getDataURL();
		}
		return "";
	}
	
	
	public void setSelectedMultiAcdURL(String url) {
		selectedMultiAcdURL=url;
	}


	public String getMultiACDInfo() {
		return multiACDInfo;
	}


	public String getMetaFormat() {
		return metaFormat;
	}


	public void setMetaFormat(String metaFormat) {
		this.metaFormat = metaFormat;
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


	public String getMetaDate() {
		if (metaDate != null) {
			return metaDate.toString();
		} else {
			return "";
		}
	}


	public void setMetaDate(Date metaDate) {
		this.metaDate = metaDate;
	}
	
	public void setMetaYear(String year) {
		this.metaYear = year;
	}
	
	public String getMetaYear() {
		return metaYear;
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
	
	public String getMetaSubject() {
		return metaSubject;
	}


	public void setMetaSource(String metaSource) {
		this.metaSource = metaSource;
	}

	
	public String getMetaSource() {
		return metaSource;
	}

	
	public void setMetaSubject(String metaSubject) {
		this.metaSubject = metaSubject;
	}
	

	public int getTableFirstPage() {
		return tableFirstPage;
	}


	public void setTableFirstPage(int tableFirstPage) {
		this.tableFirstPage = tableFirstPage;
	}
	
	
	private String getMetadataStatsInfo (HashMap<String, ResourceInfo> differentResources, ProcessState ps) {
		
		int complete, sufficient, incomplete, empty;
		complete = sufficient = incomplete = empty = 0;
		
		for (String rd : differentResources.keySet()) {
			
			if (ps != null && differentResources.get(rd).getFileInfo().getProcessState() != ps) continue;
			
			switch (differentResources.get(rd).getResourceMetadata().getMetadataState()) {
			
			case COMPLETE : complete++;
				break;
				
			case SUFFICIENT : sufficient++;
				break;
				
			case INCOMPLETE : incomplete++;
				break;
			
			case EMPTY : empty++;
				break;
			
			default :
				break;
			}
		}
		return complete+"#"+sufficient+"#"+incomplete+"#"+empty;
	}


	public String getMetaUbTitle() {
		return metaUbTitle;
	}


	public void setMetaUbTitle(String metaUbTitle) {
		this.metaUbTitle = metaUbTitle;
	} 
	
	
	private boolean isInResourceDatabase(String resourceUrl) {
		
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
	
	
	public String getBllInfoRdf() {
		return getBllInfoConll();
	}
	
	
	public String getBllInfoConll() {
		
		/** INSERT CODE
		 * 	<p:contextMenu for="a792f">
			<p:menuitem value="Show details" icon="ui-icon-info" update="a792d" oncomplete="PF('showBllInfo2').show()"/>
			</p:contextMenu>
			
			<p:contextMenu for="a892f">
			<p:menuitem value="Show details" icon="ui-icon-info" update="a892d" oncomplete="PF('showBllInfo').show()"/>
			</p:contextMenu>
		 */
		
		Utils.debug("getBllInfoConll "+getBllInfoConllCall++);
		if(getBllInfoConllCall == 1) return "";
		
		Utils.debug(selectedModelMatch == null);
		if (selectedFileResult == null || selectedModelMatch == null) return "";

		//Utils.debug(selectResourceModel.getModelType()+" "+selectedModelMatch.getConllColumn());
		
		String result = "";
		HashSet<String> bllPathList = new HashSet<String>();
		
		ResourceInfo resourceInfo = this.getSelectedResource();
		
		bllPathList = executer.getWriter().getQueries().getBLLResultPath(resourceInfo, selectedModelMatch, selectedFileResult);
		HashSet<String> classes = new HashSet<String>();
		int id = 1;
		result = "\nPath analysis :\n\n";
		for (String r : bllPathList) {
			r = r.replace("START",selectedFileResult.getFoundTagOrClass());
			result+="["+id+++"] "+r+"\n\n";
			String [] tmp = r.replace("]","").replace("[","").split(",");
			classes.add(tmp[tmp.length-1].trim());
		}
		
		ArrayList<String> bllConcepts = executer.getWriter().getBllTools().getBllFileResult(resourceInfo, selectedModelMatch, selectedFileResult);
		result+="Available BLL classes :\n";
		for (String c : classes) {
			result+=c.split("#")[1]+", ";
		}
		result=result.substring(0,result.length()-2)+"\n\n";
		
		result+="Class dependencies :\n";
		String temp = "";
		for (String lower : classes) {
			temp+=(lower.split("#")[1]+"\n");
			int found = 0;
			for (String upper : classes) {
				
				if (upper.equals(lower)) continue;
				
				if (executer.getWriter().getBllTools().getBllMatrixParser().isLowerClass(lower, upper)) {
					temp+=("... is-lower-class-of "+upper.split("#")[1])+"\n";found++;
				}
				/*if (executer.getWriter().getBllTools().getBllMatrixParser().isUpperClass(upper, lower)) {
					result+=("... is-upper-class-of "+lower.split("#")[1])+"\n";
				}*/
			}
			if (found > 0) {
				result+=temp;
			}
			temp="";
		}
		
		result+="\nLowest indepedent classes :\n";
		HashSet <String> lowestClasses = executer.getWriter().getBllTools().getBllMatrixParser().getLowestClassesInClassHierarchy(classes);
		for (String x : lowestClasses) {
			result+=x+"\n";
		}
		
		result+="\nFinal BLL Selection :\n";
		for (String c : bllConcepts) {
			result+=c+"\n";
		}
		
		HashSet<String> check = new HashSet<String>();
		String tagKey,classKey;
		
		if (selectedFileResult.isTag()) {
			tagKey = selectedFileResult.getFoundTagOrClass()+"@"+selectedModelMatch.getModelType();
			if (executer.getWriter().getBllTools().getTag2Bll().containsKey(tagKey)) {
				check.addAll(executer.getWriter().getBllTools().getTag2Bll().get(tagKey));
			}
		}
		
		if (selectedFileResult.isUrl()) {
			classKey = selectedFileResult.getFoundTagOrClass();
			if (executer.getWriter().getBllTools().getClass2Bll().containsKey(classKey)) {
				check.addAll(executer.getWriter().getBllTools().getClass2Bll().get(classKey));
			}
		} 
		
		if (!check.isEmpty()) {
			boolean error = false;
			if (check.size() == classes.size()) {
				for (String x : check) {
					if (!classes.contains(x)) {
						error = true; break;
					}
				}
			}
			if (!error) {
				result+= "OK";
			} else {
				result+="\n Different lowest indepedent classes ! \n";
				for (String x : check) {
					result+=x+" \n";
				}
			}
		}
		
		return result;
	}
	
	
	public void selectFileResult(SelectEvent event) { 
		
		selectedFileResult = (FileResult) event.getObject();
	}
	
	
	public void onToggle(ToggleEvent e) {
		Utils.debug("toggleEvent "+(Integer) e.getData());
		guiConfiguration.getVisibleColumnsMain().set((Integer) e.getData(), e.getVisibility() == Visibility.VISIBLE);
	}
	
	public GuiConfiguration getGuiConfiguration() {
		return guiConfiguration;
	}
	
	public void setGuiConfiguration(GuiConfiguration config) {
		guiConfiguration = config;
	}


	public List<ModelMatch> getSelectedModels() {
		return selectedModels;
	}

	// is called before conll or rdf model edit
	public void setSelectedModels() {
		
		Utils.debug("setSelectedModels");
		this.selectedModels = selectedResource.getFileInfo().getModelMatchingsForColumn(selectedCol);
	}
	

	
	public void skipJob () {
		
		//TODO when removing jobs that are not already started in the worker then
		// take care to also remove these resource from the jms message queue (e.g. via receive())
		
		if(resourceQueue.isEmpty()) return;
		
		int head = resourceQueue.size()-1;
		
		// get resource beeing processed r_active
		String dataUrl = resourceQueue.get(head).getDataURL();
		Utils.debug("Cancel processing for resource : "+resourceQueue.get(head).getDataURL());
		Utils.debug("file : "+resourceQueue.get(head).getFileInfo().getRelFilePath());
		
		Utils.debug(dataUrl);
		
		// stop execution of resource
		Utils.debug("skipJob");
		executer.skipMQJob();
		
		// delete all results of r_active
		executer.getResourceManager().deleteResource(dataUrl);
		
		// Remove reosurce from process queue
		resourceQueue.remove(head);
		
		// Remove all partial results for r_active from main table
		Iterator<ResourceInfo> iterator = rfl.iterator();
		while (iterator.hasNext()) {
			ResourceInfo next = iterator.next();
			if (next.getDataURL().equals(dataUrl)) {
				iterator.remove();
			}
		}
		
		showInfo("Stopped running Job !");
	}
	
	
	public void stopQueue () {
		
		if(resourceQueue.isEmpty()) return;
		Utils.debug("stopQueue");
		executer.stopMQWorker();
		showInfo("Removing all Jobs !");
	}
	
	
	public String getconfirmMessage() {
		
		if(resourceQueue.isEmpty()) {
			return "Queue empty !";
		}
		
		int head = resourceQueue.size()-1;
		// get resource beeing processed
		return resourceQueue.get(head).getDataURL();
	}
	
	
	public Boolean getResourceQueueIsEmpty() {
		return resourceQueue.isEmpty();
	}
	
	
	public ExecutionBean getExecutionBean() {
		return executionBean;
    }
	
    public void setExecutionBean (ExecutionBean neededBean) {
    	this.executionBean = neededBean;
    }


	public ResourceInfo getDisplayedResource() {
		return displayedResource;
	}


	public void setDisplayedResource(ResourceInfo displayedResource) {
		this.displayedResource = displayedResource;
	}


	public boolean isCanEdit() {
		return canEdit;
	}


	public void setCanEdit(boolean canEdit) {
		this.canEdit = canEdit;
	}



	
	/*public String closeEdit() {
		return "login?faces-redirect=true";
	}*/
}
