package de.unifrankfurt.informatik.acoli.fid.webclient;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.primefaces.context.RequestContext;
import org.primefaces.event.SelectEvent;

import de.unifrankfurt.informatik.acoli.fid.exec.Executer;
import de.unifrankfurt.informatik.acoli.fid.owl.ModelDefinition;
import de.unifrankfurt.informatik.acoli.fid.owl.OntologyManager;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager;
import de.unifrankfurt.informatik.acoli.fid.spider.DownloadManager;
import de.unifrankfurt.informatik.acoli.fid.types.Backup;
import de.unifrankfurt.informatik.acoli.fid.types.DataLinkState;
import de.unifrankfurt.informatik.acoli.fid.types.InvalidModelDefinitionException;
import de.unifrankfurt.informatik.acoli.fid.types.ModelGroup;
import de.unifrankfurt.informatik.acoli.fid.types.ModelInfo;
import de.unifrankfurt.informatik.acoli.fid.types.ModelType;
import de.unifrankfurt.informatik.acoli.fid.types.ModelUsage;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceState;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;
import edu.emory.mathcs.backport.java.util.Collections;
import edu.stanford.nlp.util.StringUtils;

/**
 * @author frank
 *
 */
@ManagedBean(name="obean")
@ViewScoped
public class LoginOntologyBean implements Serializable {
	
	
	@ManagedProperty(value="#{execute}")
    private ExecutionBean executionBean;
	
	@ManagedProperty(value="#{login}")
    private LoginBean loginBean;
	
	private static final long serialVersionUID = 1L;
	private int tableFirstPage = 0;
	private String deleteMessage="Delete model permanently ?";
	private boolean loaded=false;
	
	private List<ModelInfo> modelList = new ArrayList<ModelInfo>();
	private List<ModelInfo> filteredModelList = null;
	private ModelInfo selectedDummyModel = null;
	private List<ModelInfo> modelListOld;
	
	private Executer executer;

	@ManagedProperty(value="#{login.resourceManager}")
	private ResourceManager resourceManager;
	private XMLConfiguration fidConfig;
	private ModelInfo selectedModel;
	//@ManagedProperty(value="#{login.modelDefinition}")
	private ModelDefinition modelDefinition;
	
	private String addOrEditModelDialogHeader = "";
	private Boolean modelDefinitionWasChanged = false;
	
	private String selectedModelUrl = "";
	private String selectedModelDocumentationUrl = "";
	private String selectedModelID = "";
	private ModelUsage selectedModelUsage = null;
	private boolean selectedModelIsOnline = false;
	private boolean selectedModelDocumentationIsOnline = false;
	private boolean selectedModelIsActive = false;
	
	private String progressText="";
	private boolean editModel = false;
	private String newModelID = "";

	private String confirmMessage="";

	private HashSet<ModelType> updatedModels;
	private boolean modelConfigurationEdited = false;
	
	private AtomicInteger progressInteger = new AtomicInteger();
	private AtomicReference<String> modelCheckUrl;
	private HashMap<String, Boolean> urls;
	

	
	
	//@EJB
    //ExecuterEjb executionEjb;
	

	@PostConstruct
    public void init() {
		
		if (loaded) return;
		loaded = true;
		
		// init executed always when ontology manager is opened !
		System.out.println("init");
		
		executer = ExecutionBean.getPublicExecuter();Utils.debug(executer.getExecutionMode());
		//resourceManager = executer.createNewResourceManagerInstance();
	    setFidConfig(Executer.getFidConfig());
	    
	    try {
	    	
//	    	if (Executer.modelDefinition != null) {
//	    		modelDefinition = Executer.modelDefinition;
//	    		System.out.println("using modelDefinition from Executer");
//	    	} else {
	    	
	    		// read model definitions from file
	    		modelDefinition = new ModelDefinition(fidConfig);
	    		System.out.println("using modelDefinition from fidConfig");
	    	//}
	    	
			modelList = modelDefinition.getModelInfoList();
	    	
			
			// modelDefinitionOld = SerializationUtils.clone(modelDefinition); does not work,
			// because error in modelsNeedUpdateAfterEdit

			modelListOld = new ArrayList<ModelInfo>();
			for (ModelInfo mi : modelList) {
				modelListOld.add(SerializationUtils.clone(mi));
			}
			
			//checkModelsOnline();
	    
	    } catch (InvalidModelDefinitionException e) {
			e.printStackTrace();
		}
	    
	    
	    checkModels(1); // checkModels(0); check on server start ?
	    //checkModelsOnline();
	}
	
	
	public void onContextMenu(SelectEvent event) {
		
		System.out.println("onContextMenu");
		
		if (event.getObject() == null) {
			Utils.debug("onContextMenu call with null object !");
			showError("onContextMenu call with null object !");
			return;
		}
		//Utils.debug(event.getObject().getClass().getName());
		ModelInfo x = (ModelInfo) event.getObject();
		
		selectedModel = x;
		//selectedModel = (ModelInfo) SerializationUtils.clone(x);
		
		selectedModelUrl = selectedModel.getUrl().toString();
		selectedModelDocumentationUrl = selectedModel.getDocumentationUrl().toString();
		selectedModelID = selectedModel.getModelType().getId();
		selectedModelUsage = selectedModel.getModelUsage();
		selectedModelIsOnline = selectedModel.isOnline();
		selectedModelIsActive = selectedModel.isActive();
		
		newModelID="";

		FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:modelEditDialog");
		RequestContext.getCurrentInstance().reset("form:modelEditDialog");
	}

	public void addModel() {
		
		addOrEditModelDialogHeader = "Add new model definition";
		editModel=false;
		setModelsUpdated();
		selectedModelUrl="";
		selectedModelDocumentationUrl="";
		selectedModelID="";
		selectedModelUsage=ModelUsage.ANNOTATION;
		FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:modelEditDialog");
		RequestContext.getCurrentInstance().reset("form:modelEditDialog");
		
	}
	
	public void editModel() {
		
		addOrEditModelDialogHeader = "Edit model definition";
		editModel=true;
		setModelsUpdated();
		FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:modelEditDialog");
		RequestContext.getCurrentInstance().reset("form:modelEditDialog");
	}
	
	public void deleteModel() {
		
		if (selectedModel.getModelType().equals(ModelType.valueOf("olia"))) {
			showError("Cannot delete OLIA model - try edit !");
		}
		
		System.out.println("delete model");
		setModelsUpdated();
 	}
	
	
	public void addNewModelGroup() {
		
		newModelID = newModelID.trim();
		if (newModelID.isEmpty()) return;
		if (!newModelID.matches("[a-zA-Z0-9]+")) {
			showError("Error : Only alphanumeric characters allowed in ID !");
			return;
		}
		int i=0;
		int alpha=0;
		while (i < newModelID.length()) { 
			if (StringUtils.isAlpha(newModelID.substring(i, i+1))) {
				alpha++;
			}
			i++;
		}
		if (alpha < 3) {
			showError("Error : ID should contain at least 3 alphabetic characters !");
			return;
		}
		
		if (newModelID.length() < 4 || newModelID.length() > 12) {
			showError("Error : ID is at least 4 and at most 12 characters long !");
			return;
		}
		
		if (ModelDefinition.getModelIDs().contains(newModelID)) {
			showError("Error : ID is already used for another model !");
			return;
		}

		ModelDefinition.getModelIDPool().add(newModelID);
		
		setModelsUpdated();
		selectedModelID = newModelID;
		showInfo("New model ID '"+newModelID+"' was sucessfully added !");
	}
	
	
	public void saveModel() {
		
		checkEditedModelUrlIsOnline();
		checkEditedDocumentationUrlIsOnline();
		
		System.out.println("selectedModelIsOnline "+selectedModelIsOnline);
		System.out.println("selectedModelDocumentationIsOnline "+selectedModelDocumentationIsOnline);

		
		if (!selectedModelIsOnline) {
			showError("Error : Model URL is invalid !");
			return;
		};
		if (!selectedModelDocumentationUrl.trim().isEmpty() && !selectedModelDocumentationIsOnline) {
			showError("Error : Documentation URL is invalid !");
			return;
		}
		if (selectedModelID.isEmpty() || selectedModelUsage == null) {
			showError("Error : Invalid Model ID !");
			return;
		}
		
		if (editModel) {
			selectedModel.setDocumentationUrl(selectedModelDocumentationUrl);
			try {
				
				selectedModel.setUrl(new URL(selectedModelUrl));
				showInfo("Model URL for '"+selectedModelID+"' has successfully been updated!");
				selectedModel.setDataLinkState(DataLinkState.OUTDATED);

			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		} else {
			
			// check if model already exists
			for (ModelInfo m : modelList) {
				if (m.getUrl().toString().equals(selectedModelUrl)) {
					showError("Error : URL already used for other model ("+m.getModelType()+"@"+m.getModelUsage()+") !");
					return;
				}
				if (m.getModelType().getId().equals(selectedModelID) &&
					m.getModelUsage() == selectedModelUsage) {
						showError("Error : Model with same ID and Usage already exists !");
						return;
					}
			}
			
			ModelType modelType = ModelType.valueOf(selectedModelID);
			// create new model info
			ModelInfo modelInfo = new ModelInfo(
							selectedModelUrl,
							selectedModelDocumentationUrl,
							modelType,
							selectedModelUsage,
							true);

			modelInfo.setDataLinkState(DataLinkState.OUTDATED);
			
			// Add new model to the list
			modelList.add(modelInfo);
			
			// Case 1 : model group already exists -> put new modelInfo in that model group
			if (ModelDefinition.getModelIDs().contains(selectedModelID)) {
			
				ModelGroup mg = modelDefinition.getModelDefinitions().get(modelType);
				mg.getModelFiles().add(modelInfo);
			
			} else {
				
				// Case 2 : model group does not exist -> create new model group and put new modelInfo in that model group
				ModelGroup mg = new ModelGroup();
				mg.setModelType(modelType);
				mg.setNiceName("nicename");
				mg.setDocumentationUrl(selectedModelDocumentationUrl);
				mg.getModelFiles().add(modelInfo);
				
				// Add model group to modelDef
				modelDefinition.getModelDefinitions().put(modelType, mg);
			}
			
			showInfo("A new Model with ID '"+selectedModelID+"' has successfully been created!");
		}
		
//		RequestContext context = RequestContext.getCurrentInstance();
//		context.execute("PF('addOrEditModel').hide();");
		
		// save modelList to ModelDef.json
		//modelDefinition.saveModelDef();
	}
	
	
	
	public void checkUpdateOliaModels() {
		
		
		// check conditions
		HashSet<String> notOnline = new HashSet<String>();
		for (ModelInfo mi : modelList) {
			if (!mi.isOnline()) {
				notOnline.add(mi.getUrl().toString());
			}
		}
		if (!notOnline.isEmpty()) {
			String errorMsg ="Update stopped because some model URLs are not online : \n";
			for (String url : notOnline) {
				errorMsg+="\n"+url;
			}
			showStickyMessage(errorMsg, FacesMessage.SEVERITY_ERROR);
			return;
		}
		
		if (true) {
			showInfo("Debug - not updating now");
			return;
		}
			
		OntologyManager ontologyManager = new OntologyManager(
	    		executer,
				new DownloadManager(
						executer.createNewResourceManagerInstance(), // global instance (Neo4J) or separate instance from above (GremlinServer)
						new File (fidConfig.getString("RunParameter.downloadFolder")),
						null,
						fidConfig,
						60),
						fidConfig,
				modelDefinition);
		
		
		String updateMessage = "All models are up to date !";  // default
		updatedModels = ontologyManager.checkUpdatedModels();
		
		if (!updatedModels.isEmpty()) {
			updateMessage = "The following models have been updated :";
			for (ModelType mtc : updatedModels) {
				updateMessage += mtc.getId()+" ";
			}
		}
		
		if (updateMessage.startsWith("All models")) {
			showMessageDialog(updateMessage, FacesMessage.SEVERITY_INFO);
			return;
		} else {
			showConfirmDialog(updateMessage);
		}
	}
		
		
	public void updateOliaModels() {
		
		Utils.debug("updateOliaModels");
		ResourceManager rm = executer.createNewResourceManagerInstance();		
		Backup backup = new Backup();
		String name = "before_model_update_";
		String pattern = "MM-dd-yyyy_HH_mm_ss";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		String date = simpleDateFormat.format(backup.getDate());
		backup.setName(name+date);		
		
		rm.addBackup(backup);
		
		// force flag needs not to be set
		//fidConfig.getString("OWL.modelUpdateMode") force
		
		String err = ExecutionBean.getPublicExecuter().makeBackup(backup);
		
		if (err.isEmpty()) {
			Utils.debug("backup before model update finished !");
		} else {
			Utils.debug(err);
			return;
		}
		
		try {
			Thread.sleep(fidConfig.getLong("Databases.restartTimeoutInMilliseconds"));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		OntologyManager ontologyManager = new OntologyManager(
	    		executer,
				new DownloadManager(
						executer.createNewResourceManagerInstance(), // global instance (Neo4J) or separate instance from above (GremlinServer)
						new File (fidConfig.getString("RunParameter.downloadFolder")),
						null,
						fidConfig,
						60),
						fidConfig,
				modelDefinition);
		
		// Start update
		String updateError = ontologyManager.updateOliaModels(updatedModels);
		
		ExecutionBean.initApplication(true);
		//ExecutionBean.initResourceCache();
		
		if (!updateError.isEmpty()) {
			showMessageDialog(updateError, FacesMessage.SEVERITY_INFO);
			executer.createNewResourceManagerInstance().deleteBackup(backup);
			updateError = ExecutionBean.getPublicExecuter().deleteBackup(backup);
			if (!updateError.isEmpty()) {
				showMessageDialog(updateError, FacesMessage.SEVERITY_INFO);
				return;
			} else {
				//showInfo("Backup '"+backup.getName()+"' sucessfully deleted !");
			}
		} else {
			showMessageDialog("Updating of OLiA models sucessfully finished !", FacesMessage.SEVERITY_INFO);
			
			// save changed modelDefinition to file
		}
	}
	

	/**
	 * @param updateMessage
	 */
	private void showConfirmDialog(String updateMessage) {

		confirmMessage = updateMessage;
		FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:yesNoDialog");
		RequestContext.getCurrentInstance().reset("form:yesNoDialog");
		RequestContext context = RequestContext.getCurrentInstance();
		context.execute("PF('yesNoDialogW').show();");
	}
	
	
	public int getTableFirstPage() {
		return tableFirstPage;
	}


	public void setTableFirstPage(int tableFirstPage) {
		this.tableFirstPage = tableFirstPage;
	}


	public String getDeleteMessage() {
		return deleteMessage;
	}


	public void setDeleteMessage(String deleteMessage) {
		this.deleteMessage = deleteMessage;
	}


	public List<ModelInfo> getFilteredModelList() {
		return filteredModelList;
	}


	public void setFilteredModelList(List<ModelInfo> filteredModelList) {
		this.filteredModelList = filteredModelList;
	}


	public List<ModelInfo> getModelList() {
		return modelList;
	}


	public void setModelList(List<ModelInfo> modelList) {
		this.modelList = modelList;
	}


	public ModelInfo getSelectedDummyModel() {
		return selectedDummyModel;
	}


	public void setSelectedDummyModel(ModelInfo selectedDummyModel) {
		this.selectedDummyModel = selectedDummyModel;
	}


	public ExecutionBean getExecutionBean() {
		return executionBean;
	}


	public void setExecutionBean(ExecutionBean executionBean) {
		this.executionBean = executionBean;
	}


	public XMLConfiguration getFidConfig() {
		return fidConfig;
	}


	public void setFidConfig(XMLConfiguration fidConfig) {
		this.fidConfig = fidConfig;
	}
	
	
	/*public String uploadModel() {
		
		Utils.debug("upload model file");
		
		try {
			String fileName = FileUploadBean.getUploadedFile().getFileName();
			String inputFilePath = new File (new File(fidConfig.getString("RunParameter.ServiceUploadDirectory")),fileName).getAbsolutePath();			
			Utils.debug("inputFilePath :"+inputFilePath);
			
			// Write uploaded file to local fs
			FileUploadBean.getUploadedFile().write(inputFilePath);
			String modelOntologyAsText = FileUtils.readFileToString(new File(inputFilePath), StandardCharsets.UTF_8);
			
			System.out.println(modelOntologyAsText);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
			
		return "";
	}*/
	
	
	public void showInfo(String message) {
		showMessage(message, FacesMessage.SEVERITY_INFO);
	}

	public void showError(String message) {
		Utils.debug(message);
		showMessage(message, FacesMessage.SEVERITY_ERROR);
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
	
	
	public void showMessageDialog(String message, Severity severity) {
	   	 FacesMessage msg = new FacesMessage(severity, "", message);
	     RequestContext.getCurrentInstance().showMessageInDialog(msg);
	}


	public ModelInfo getSelectedModel() {
		return selectedModel;
	}


	public void setSelectedModel(ModelInfo selectedModel) {
		this.selectedModel = selectedModel;
	}


	public ResourceManager getResourceManager() {
		return resourceManager;
	}


	public void setResourceManager(ResourceManager resourceManager) {
		this.resourceManager = resourceManager;
	}


	public String getAddOrEditModelDialogHeader() {
		return addOrEditModelDialogHeader;
	}


	public void setAddOrEditModelDialogHeader(String addOrEditModelDialogHeader) {
		this.addOrEditModelDialogHeader = addOrEditModelDialogHeader;
	}


	public Boolean getModelDefinitionWasChanged() {
		return modelDefinitionWasChanged;
	}


	public void setModelDefinitionWasChanged(Boolean modelDefinitionWasChanged) {
		this.modelDefinitionWasChanged = modelDefinitionWasChanged;
	}


	public String getSelectedModelUrl() {
		return selectedModelUrl;
	}


	public void setSelectedModelUrl(String selectedModelUrl) {
		this.selectedModelUrl = selectedModelUrl;
	}


	public String getSelectedModelDocumentationUrl() {
		return selectedModelDocumentationUrl;
	}


	public void setSelectedModelDocumentationUrl(
			String selectedModelDocumentationUrl) {
		this.selectedModelDocumentationUrl = selectedModelDocumentationUrl;
	}
	

	public ModelUsage getSelectedModelUsage() {
		return selectedModelUsage;
	}


	public void setSelectedModelUsage(ModelUsage selectedModelUsage) {
		this.selectedModelUsage = selectedModelUsage;
	}


	public boolean isSelectedModelIsOnline() {
		return selectedModelIsOnline;
	}


	public void setSelectedModelIsOnline(boolean selectedModelIsOnline) {
		this.selectedModelIsOnline = selectedModelIsOnline;
	}


	public boolean isSelectedModelIsActive() {
		return selectedModelIsActive;
	}


	public void setSelectedModelIsActive(boolean selectedModelIsActive) {
		this.selectedModelIsActive = selectedModelIsActive;
	}
	
	public List<String> getModelIDs() {
	
		List <String> tmp = new ArrayList<String>(ModelDefinition.getModelIDPool());
		Collections.sort(tmp);
		return tmp;
	}
	
	public List<ModelUsage> getModelUsages() {
		
		List<ModelUsage> usages =  Arrays.asList(ModelUsage.values());
		return usages;
	}
	
	public void checkEditedModelUrlIsOnline() {
		selectedModelIsOnline = ExecutionBean.getResourceChecker().urlIsOnline(selectedModelUrl, 10);
	}
	
	public void checkEditedDocumentationUrlIsOnline() {
		selectedModelDocumentationIsOnline = ExecutionBean.getResourceChecker().urlIsOnline(selectedModelDocumentationUrl, 10);
	}
	
	
	public void checkModels(int mode) {
		
		if (mode == 0) return;
		
		RequestContext context = RequestContext.getCurrentInstance();
		context.execute("PF('checkModels').show();");
		context.execute("PF('progressbar').start();");
		
		urls = new HashMap<String, Boolean>();
		for (ModelInfo mi : modelList) {
			urls.put(mi.getUrl().toString(), false);
		}
		
		class OneShotTask implements Runnable {
	        HashMap<String, Boolean> models;
	        OneShotTask(HashMap<String, Boolean> x) { models = x;}
	        public void run() {
	        	startLongTask(models);
	        }
	    }
		Thread t = new Thread(new OneShotTask(urls));
		t.start();
		
//	    executorService = Executors.newSingleThreadExecutor();
//	    executorService.execute(this::startLongTask);
	}
	
	
	public void modelCheckComplete() {
		
		OntologyManager ontologyManager = new OntologyManager(
	    		executer,
				new DownloadManager(
						executer.createNewResourceManagerInstance(), // global instance (Neo4J) or separate instance from above (GremlinServer)
						new File (fidConfig.getString("RunParameter.downloadFolder")),
						null,
						fidConfig,
						10),
						fidConfig,
				modelDefinition);
					
		updatedModels = ontologyManager.checkUpdatedModels();
		
		for (ModelInfo mi : modelList) {
			
			boolean isOnline = urls.get(mi.getUrl().toString());
			
			if (isOnline) {
				if (updatedModels.contains(mi.getModelType())) {
					mi.setDataLinkState(DataLinkState.OUTDATED);
				} else {
					mi.setDataLinkState(DataLinkState.UP2DATE);
				}
			} else {
				mi.setDataLinkState(DataLinkState.BROKEN);
			}
			
//			System.out.println(mi.getUrl().toString());
//			System.out.println(urls.get(mi.getUrl().toString()));
		}
		
		//FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form");
		RequestContext.getCurrentInstance().update("form");
		//return "obean?faces-redirect=true";
		//return "";
	}
	
	
	public String getModelCheckUrl() {
		
		return modelCheckUrl.get();
	}

	
	private void startLongTask(HashMap<String, Boolean> urls) {
				
	    progressInteger.set(0);
	    
	    float stepSize = 100f / modelList.size();
	    float progress = 1f; // 0 works, but add 1 to avoid the case where the progress bar stops at 99%
	                         // because of rounding errors while adding stepSize
	    
	    int count = 1;
	    for (String mi : urls.keySet()) {
	    	System.out.println(count+++"/"+modelList.size());
	  		//progressText = mi.getUrl().toString();
	    	
	    	modelCheckUrl = new AtomicReference<String>(mi);
	  		
	  		//showInfo(progressText);
	  		/*FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:progressbar");
	  		RequestContext.getCurrentInstance().reset("form:progressbar");*/
	  		
	  		ResourceInfo resourceInfo = new ResourceInfo(mi,"");
	  		ArrayList<ResourceInfo> resources = new ArrayList<ResourceInfo>();
	  		resources.add(resourceInfo);
	
	  		ExecutionBean.getResourceChecker().checkBrokenLinksThreaded(resources, 10);
	
	  		while (!ExecutionBean.getResourceChecker().getThreadRef().isDone()) {
	  			try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
	  		}
	  		urls.put(mi, resourceInfo.getResourceState() != ResourceState.ResourceUrlIsBroken);
	  		//mi.setOnline(resourceInfo.getResourceState() != ResourceState.ResourceUrlIsBroken);
	  		
			//progressInteger.getAndIncrement();
	  	   progress+=stepSize;
	  	   int newValue = (int) progress;
	  	   progressInteger.set(newValue);
	    }
	      
	      
//	      for (int i = 0; i < 100; i++) {
//	    	  System.out.println(":"+i);
//	          progressInteger.getAndIncrement();
//	          //simulating long running task
//	          try {
//	              Thread.sleep(100);
//	          } catch (InterruptedException e) {
//	              e.printStackTrace();
//	          }
//	      }
//	      executorService.shutdownNow();
//	      executorService = null;
	}
	
	
	public int getProgress() {
	      return progressInteger.get();
	}
	
	
	public void checkModelsOnline() {
		
		if (true) {
			checkModels(1);
			return;
		}
		
		RequestContext context = RequestContext.getCurrentInstance();
		context.execute("PF('checkModels').show();");
		
		
		ModelInfo.checkModelsOnline(modelList, ExecutionBean.getResourceChecker());
		
		
//    	for (ModelInfo mi : modelList) {
//    		
//    		progressText = mi.getUrl().toString();
//    		
//    		//showInfo(progressText);
//    		/*FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:progressbar");
//    		RequestContext.getCurrentInstance().reset("form:progressbar");*/
//    		
//    		ResourceInfo resourceInfo = new ResourceInfo(mi.getUrl().toString(),"");
//    		ArrayList<ResourceInfo> resources = new ArrayList<ResourceInfo>();
//    		resources.add(resourceInfo);
//
//    		ExecutionBean.getResourceChecker().checkBrokenLinksThreaded(resources, 10);
//
//    		while (!ExecutionBean.getResourceChecker().getThreadRef().isDone()) {
//    			try {
//					Thread.sleep(500);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//    		}
//    		mi.setOnline(resourceInfo.getResourceState() != ResourceState.ResourceUrlIsBroken);
//    	}
    	
    	//showMessageDialog("Checking of model download links finished successfully !", FacesMessage.SEVERITY_INFO);
		//context.execute("PF('checkModelLinks').hide();");
	}


	public boolean isSelectedModelDocumentationIsOnline() {
		return selectedModelDocumentationIsOnline;
	}


	public void setSelectedModelDocumentationIsOnline(
			boolean selectedModelDocumentationIsOnline) {
		this.selectedModelDocumentationIsOnline = selectedModelDocumentationIsOnline;
	}


	public void setProgressText(String progressText) {
		this.progressText = progressText;
	}


	public boolean isEditModel() {
		return editModel;
	}


	public void setEditModel(boolean editModel) {
		this.editModel = editModel;
	}


	public String getNewModelID() {
		return newModelID;
	}


	public void setNewModelID(String newModelID) {
		this.newModelID = newModelID;
	}


	public String getSelectedModelID() {
		return selectedModelID;
	}


	public void setSelectedModelID(String selectedModelID) {
		this.selectedModelID = selectedModelID;
	}


	public String getConfirmMessage() {
		return confirmMessage;
	}


	public void setConfirmMessage(String confirmMessage) {
		this.confirmMessage = confirmMessage;
	}


	public boolean isModelConfigurationEdited() {
		return modelConfigurationEdited;
	}
	
	public void setModelConfigurationEdited(boolean editModel) {
		this.modelConfigurationEdited = editModel;
	}
	
	
	public String exit(int mode) {
		
		int modelsNeedUpdateAfterEdit = modelsNeedUpdateAfterEdit();
		
		System.out.println("Exit code "+modelsNeedUpdateAfterEdit);
		
		// no changes in modelList
		if(modelsNeedUpdateAfterEdit == 0 || mode == 1) {
			loginBean.closeOntologyManager();
			//return "login?faces-redirect=true";
			return "";
		}

		// changes made to modelList that require an update of the model database
		if (modelsNeedUpdateAfterEdit == 1) {
			RequestContext context = RequestContext.getCurrentInstance();
			context.execute("PF('confirmDBupdate').show()");
		}
		
		// changes made to modelList, that will be saved to ModelDef.json
		if (modelsNeedUpdateAfterEdit == 2) {
			// no update, but save modelList to ModelDef.json because documentationUrl was changed
			System.out.println("saving only");
			saveModelDefinition();
			return "login?faces-redirect=true";
		}
		
		// error, because at least 2 models in modelList have equal ID (same modelType, URL, modelUsage, active)
		if (modelsNeedUpdateAfterEdit == 3) {
			showError("Model list contains two models with same ID");
			return "";
		}
		
		return "";
	}
	
	
	public void saveModelDefinition() {
		
		// save edited modelList to ModelDef.json
		modelDefinition.saveModelDef();
	}
	
	
	public void updateDatabase() {
		
		System.out.println("update model database");
		
		// there are 3 locations for olia models
		// 1. ModelDef.json
		// 2. resource entries in the registry database
		// 3. modelDefinition variable is initialized in init() method and is changed
		//    by the user with editing
		
		// different states for model data
		// a. 1,2,3 in sync
		// b. 2,3 in sync (no model edits) but 1 different from 1,2 -> ?
		// c. 1,2 in sync, but 3 is different, because of edits
		// d. 1,2,3 all not in sync with eachother
		
		// usecases
		// 1. some models have been moved to another URL
		// 2. some models have been updated with newer versions
		// 3. a user has edited model data/download URLs
		// 4. a user has deleted a model, because it is not used anymore (may be replaced by another model)
		// 5. a user has added a new model (e.g. for replacing an old model, something completely new)
		
		
		// ontology manager open
		// 1. at startup check if models are online
		// 2. at startup check if models have been updated with newer versions
		
		// user tasks
		// 1. check actual used model definitions are up2date
		// 2. repair broken link owl or documentation (point to correct URL)
		// 3. add new model definitions
		
		// requirements before a user can add a model (deleting is allowed, in case an old model is 
		// no longer available)
		// - all URLs of defined models resolve, valid documentation URLs are secondary
		// (defined models do not have to be the most recent version)
		

		// ontology manager close
		// 1. check if modellist was edited
		
		// show state of model in 'online' column
		// states : UP2DATE, OUTDATED, BROKEN 
		
		
		// modelDefinition set in init()
		// list of model definitions is changed
		
		// save edited modelList to ModelDef.json
		//modelDefinition.saveModelDef();
		
		// check for new or
		checkUpdateOliaModels();
		
		// make backup and update olia models
		//updateOliaModels();
		
	}
	
	
	public int modelsNeedUpdateAfterEdit() {
		
		// return codes : 0 = don't update, 1 = update, 2 = save, 3 = error
				
		// at least one new model has been added
		if (modelList.size() != modelListOld.size()) return 1;
		
		int changedDocumentationUrls = 0;
		HashSet<String> matchedModelIDs = new HashSet<String>();
		for (ModelInfo mi : modelList) {
			
			// error, because of two models with the same id in the modelList
			if (matchedModelIDs.contains(mi.getID())) return 3;
			
			boolean ok = false;
			for (ModelInfo mo : modelListOld) {
				// skip models that already have been matched
				if (mi.getID().equals(mo.getID())) {
					ok = true;
					matchedModelIDs.add(mi.getID());
					if (!mi.getDocumentationUrl().equals(mo.getDocumentationUrl())) {
						changedDocumentationUrls++;
					}
					break;
				}
			}
			if (!ok) return 1;
		}
		
		if (changedDocumentationUrls == 0) return 0; // no update & no save
		else
		return 2; // no update, but save because a documentationUrl was changed
	}
	
	
	public LoginBean getLoginBean() {
		return loginBean;
    }
	
    public void setLoginBean (LoginBean neededBean) {
    	this.loginBean = neededBean;
    }
	
	
	public void setModelsUpdated() {
		
//		this.modelConfigurationEdited = true;
//		FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:toolbar");
//		RequestContext.getCurrentInstance().update("form:toolbar");

	}
	

}
