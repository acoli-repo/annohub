package de.unifrankfurt.informatik.acoli.fid.webclient;


import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.maven.shared.utils.io.FileUtils;
import org.primefaces.context.RequestContext;
import org.primefaces.event.SelectEvent;

import de.unifrankfurt.informatik.acoli.fid.exec.Executer;
import de.unifrankfurt.informatik.acoli.fid.exec.Run;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager;
import de.unifrankfurt.informatik.acoli.fid.types.Backup;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;


@ManagedBean(name="backupbean")
@ViewScoped
public class LoginBackupBean implements Serializable {
	
	
	@ManagedProperty(value="#{execute}")
    private ExecutionBean executionBean;
	
	private static final long serialVersionUID = 1L;
	private int tableFirstPage = 0;
	private String deleteMessage="Delete backup permanently ?";
	private String restoreMessage="Restoring backup will delete all current resources - continue ???";
	private String statusDialogMessage = "Processing backup - please stand by !";

	private boolean loaded=false;
	
	private List<Backup> backupList = new ArrayList<Backup>();
	private List<Backup> filteredBackupList = null;
	private Backup selectedDummyBackup = null;
	private int backupInterval=0;
	private Backup selectedBackup=null;
	
	private String newBackupName="";
	private String newBackupComment="";
	private String newBackupGremlinVersion="";
	private String newBackupRegDBVersion="";
	private String newBackupDataDBVersion="";

	private Executer executer;

	//@ManagedProperty(value="#{login.resourceManager}")
	private ResourceManager resourceManager;
	
	private XMLConfiguration fidConfig;
	private String backupDirectory="";
	private int autobackupInterval=0;

	private String progressText = "";
	private static String backupCompleteMessage = "";
	private static BackupProcess backupProcess = BackupProcess.IDLE;
	enum BackupProcess {
	    CREATE,
	    RESTORE,
	    IDLE,
	    FAILED
	  }
	
	
	//@EJB
    //ExecuterEjb executionEjb;
	

	@PostConstruct
    public void init() {
		
		Utils.debug("Backup init");
		
		if (loaded) return;
		loaded = true;
		
		executer = ExecutionBean.getPublicExecuter();Utils.debug(executer.getExecutionMode());
	    resourceManager = executer.createNewResourceManagerInstance();
	    fidConfig = Executer.getFidConfig();
	    
		initBackupManager();
		
	}
	
	

	public void setBackupList(List<Backup> backupList) {
		this.backupList = backupList;
	}
	
	
	public List<Backup> getFilteredBackupList() {
		return filteredBackupList;
	}
	
	public void setFilteredBackupList(List<Backup> filteredBackupList) {
		this.filteredBackupList = filteredBackupList;
	}
	
	public String initBackupManager() {
		
		System.out.println("initBackupManager");
		
		autobackupInterval = Executer.getFidConfig().getInt("Backup.autobackupInterval");
		backupDirectory = Executer.getFidConfig().getString("Backup.directory");
		
	
		/*backupList.clear();
		Backup b1 = new Backup("Backup-1");
	    b1.setVersionGremlin("3.3.3");
	    b1.setVersionDBReg("Neo4j 3.2.4");
	    backupList.add(b1);
	    Backup b2 = new Backup("Backup-2");
	    b2.setVersionGremlin("3.3.3");
	    b2.setVersionDBReg("Neo4j 3.2.4");
	    backupList.add(b2);
	    Backup b3 = new Backup("Backup-3");
	    b3.setVersionGremlin("3.4.0");
	    b3.setVersionDBReg("Neo4j 3.2.5");
	    backupList.add(b3);*/
		
		//backupList=resourceManager.getBackups();
		backupList=Backup.readBackups(new File(backupDirectory, "backups.json"));
		return "";
	}



	public List<Backup> getBackupList() {
		return backupList;//resourceManager.getBackups();
	}

	
	public Backup getSelectedDummyBackup() {
		return selectedDummyBackup;
	}
	
	
	
	public void setSelectedDummyBackup(Backup selectedDummyBackup) {
		this.selectedDummyBackup = selectedDummyBackup;
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

	

	public void onContextMenu(SelectEvent event) {
		
		System.out.println("onContextMenu");
		
		if (event.getObject() == null) {
			Utils.debug("onContextMenu call with null object !");
			showError("onContextMenu call with null object !");
			return;
		}
		//Utils.debug(event.getObject().getClass().getName());
		Backup x = (Backup) event.getObject();
		this.selectedBackup = x;

		FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:a7831");
		RequestContext.getCurrentInstance().reset("form:a7831");
	}


	public boolean isLoaded() {
		return loaded;
	}


	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}

	
	public void showError(String message) {
		Utils.debug(message);
		showMessage(message, FacesMessage.SEVERITY_ERROR);
	}
	
	
	public void showInfo(String message) {
		showMessage(message, FacesMessage.SEVERITY_INFO);
	}
	
	
	public void showMessage(String message, Severity severity) {
	   	 FacesMessage msg = new FacesMessage(severity, "", message);
	     FacesContext.getCurrentInstance().addMessage(null, msg);
	     RequestContext.getCurrentInstance().update(("form:msgs"));
	}
	
	
	public void showMessageDialog(String message, Severity severity) {
	   	 FacesMessage msg = new FacesMessage(severity, "", message);
	     RequestContext.getCurrentInstance().showMessageInDialog(msg);
	}


	public Backup getSelectedBackup() {
		return selectedBackup;
	}



	public void setSelectedBackup(Backup selectedBackup) {
		this.selectedBackup = selectedBackup;
	}



	public String getNewBackupName() {
		return newBackupName;
	}

	public void setNewBackupName(String newBackupName) {
		this.newBackupName = newBackupName;
	}

	public String getNewBackupComment() {
		return newBackupComment;
	}

	public void setNewBackupComment(String newBackupComment) {
		this.newBackupComment = newBackupComment;
	}

	public String getNewBackupGremlinVersion() {
		return newBackupGremlinVersion;
	}

	public void setNewBackupGremlinVersion(String newBackupGremlinVersion) {
		this.newBackupGremlinVersion = newBackupGremlinVersion;
	}

	public String getNewBackupRegDBVersion() {
		return newBackupRegDBVersion;
	}

	public void setNewBackupRegDBVersion(String newBackupRegDBVersion) {
		this.newBackupRegDBVersion = newBackupRegDBVersion;
	}

	public String getNewBackupDataDBVersion() {
		return newBackupDataDBVersion;
	}

	public void setNewBackupDataDBVersion(String newBackupDataDBVersion) {
		this.newBackupDataDBVersion = newBackupDataDBVersion;
	}
		
	public void initCreateBackup() {
		
	}
	
	
	public void createNewBackup() {
		
		Utils.debug("createNewBackup");

		setBackupProcess(BackupProcess.CREATE);
		
		if (newBackupName.trim().isEmpty()) return;
		
		// check input fields
		boolean backupNameExists = new Backup(newBackupName).backupRecordExists();
		boolean backupDirectoryExists = new Backup(newBackupName).backupDirectoryExists();
		//boolean backupNameExists = resourceManager.backupExists(new Backup(newBackupName));
		boolean backupNameCharsOk = newBackupName.matches("[a-zA-Z0-9\\-_]+");
		boolean commentLengthOk = newBackupComment.length() < 100;
		
		if (newBackupName.length() < 5 || newBackupName.length() > 15 || !backupNameCharsOk) {
			backupFailed("Error : Backup name must have 4 to 15 alphanumeric characters or -_");
			return;
		}
		
		if (backupNameExists) {backupFailed("Error : Backup with same name already exists !");return;}
		if (!commentLengthOk) {backupFailed("Error : Comment can contain at most 100 characters !");return;}
		if (backupDirectoryExists) {backupFailed("Error : A backup directory with the same name already exists - please check the file system !");return;}

		
	
		
		// hide dialog
		//RequestContext.getCurrentInstance().execute("PF('createBackup').hide()");
		//RequestContext.getCurrentInstance().execute("PF('statusDialog').show()");

		setProgressText("Creating new backup '"+newBackupName+"'");
		
		
		class OneShotTask implements Runnable {
	        OneShotTask() {}
	        public void run() {
	        	

	    		// init progressbar parameter 
	   		 	ExecutionBean.setProgressValue(1);
	   		 	
	   			Backup backup = new Backup(newBackupName);
	   			backup.setVersionGremlin(newBackupGremlinVersion);
	   			backup.setVersionDBReg(newBackupRegDBVersion);
	   			backup.setVersionDBData(newBackupDataDBVersion);
	   			backup.setComment(newBackupComment);
	   			
	   			// make physical backup
	   			String backupCreationError = ExecutionBean.getPublicExecuter().makePhysicalBackup(backup);
	   			
	   			if (!backupCreationError.isEmpty()) {
	   				//resourceManager.deleteBackup(backup);
	   				backupFailed("Creating backup failed with error : "+backupCreationError);
	   				return;
	   			}
	   			
	   			// create backup record
	   			if (!backup.addBackupRecord()) {
	   				//if (resourceManager.addBackup(backup) == null) {
	   				// TODO delete backup folder created in the previous step
	   				backupFailed("Backup failed !");
	   				return;
	   			} else {
	   				Utils.debug("backup '"+newBackupName+"' success");
	   			}
	   			
	   			// String error = ExecutionBean.getPublicExecuter().makeBackup(backup);
	   			
	   			refreshResourceManager();
	   			setBackupCompleteMessage("Successfully created backup '"+backup.getName()+"' !");	
	        }
	    }
		
		Thread t = new Thread(new OneShotTask());
		t.start();
		
		return;
		//return "login-backup?faces-redirect=true";
	}
	
	
	
	
	public String restoreBackup() {
		
		setBackupProcess(BackupProcess.RESTORE);
		ExecutionBean.setProgressValue(1);
		setProgressText("Restoring Backup '"+selectedBackup.getName()+"'");
		
		Utils.debug("restoreBackup");
				
		if (selectedBackup == null) {
			backupFailed("Internal Error : restoreBackup=null");
			return "";
		}
		
		setBackupCompleteMessage("Backup '"+selectedBackup.getName()+"' was sucessfully restored !");

		class OneShotTask implements Runnable {
	        OneShotTask() {}
	        public void run() {
	        	
		        String error = ExecutionBean.getPublicExecuter().restoreBackup(selectedBackup);
	    		if (!error.isEmpty()) {
	    			backupFailed("Restoring backup failed with error : "+error);
	    			return;
	    		}
	        	
	    		// set progressbar parameter 
	   		 	ExecutionBean.setProgressValue(30);
	   		 	ExecutionBean.setProgressRange(70);

				ExecutionBean.initApplication(true);
	    		refreshResourceManager();		    		
	    		initBackupManager();
	    		setBackupCompleteMessage("Backup '"+selectedBackup.getName()+"' was sucessfully restored !");
	        }
	    }
		
		Thread t = new Thread(new OneShotTask());
		t.start();
					
		return "";
		//return "login-backup?faces-redirect=true";
	}
	
	
	
	/**
	 * @param string
	 *//*
	private void updateStatusDialogMessage(String message) {
		
		statusDialogMessage = message;
		FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:progressbar");
		RequestContext.getCurrentInstance().reset("form:progressbar");
	}*/


	
	
	public String deleteBackup() {
		
		Utils.debug("deleteBackup");
				
		String error = ExecutionBean.getPublicExecuter().deletePhysicalBackup(selectedBackup);
		if (!error.isEmpty()) {
			showError(error);
			return "";
		} else {
			
			selectedBackup.deleteBackupRecord();
			showMessageDialog("Backup '"+selectedBackup.getName()+"' sucessfully deleted !", FacesMessage.SEVERITY_INFO);		
		}
		selectedBackup=null;
		initBackupManager();
		return "";
	}
	

	/*public String deleteBackupFromDatabaseOld() {
		
		Utils.debug("deleteBackup");
		
		resourceManager.deleteBackup(selectedBackup);
		
		String error = ExecutionBean.getPublicExecuter().deleteBackup(selectedBackup);
		if (!error.isEmpty()) {
			showError(error);
			return "";
		} else {
			showMessageDialog("Backup '"+selectedBackup.getName()+"' sucessfully deleted !", FacesMessage.SEVERITY_INFO);		
		}
		selectedBackup=null;
		initBackupManager();
		return "";
		//return "login-backup?faces-redirect=true"; 
	}*/
	
	
	/**
	 * 
	 */
	private void refreshResourceManager() {
		
		resourceManager = ExecutionBean.getPublicExecuter().createNewResourceManagerInstance();
		
		try {
			Thread.sleep(fidConfig.getLong("Databases.restartTimeoutInMilliseconds"));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public ExecutionBean getExecutionBean() {
		return executionBean;
	}

	
	public void setExecutionBean(ExecutionBean executionBean) {
		this.executionBean = executionBean;
	}
	
	
	
	public String getBackupDirectory() {
		return backupDirectory;
	}



	public void setBackupDirectory(String backupDirectory) {
		this.backupDirectory = backupDirectory;
	}



	public int getAutobackupInterval() {
		return autobackupInterval;
	}



	public void setAutobackupInterval(int autobackupInterval) {
		this.autobackupInterval = autobackupInterval;
	}
	
	
	public void saveBackupOptions(){
		
		Utils.debug("saveBackupOptions");
		
		// Save parameter to loaded config
		fidConfig.setProperty("Backup.directory", backupDirectory);
		fidConfig.setProperty("Backup.autobackupInterval", autobackupInterval);
		
		// Save parameter to FidConfig file
		String error = Run.saveFIDConfig(fidConfig);
		if (error.isEmpty()) {
			showInfo("FID configuration succesfully updated !");
		}
		else {
			showError("Error : "+error+"!");
		}
	}
	

	public ResourceManager getResourceManager() {
		return resourceManager;
	}

	public void setResourceManager(ResourceManager resourceManager) {
		this.resourceManager = resourceManager;
	}



	public String getRestoreMessage() {
		return restoreMessage;
	}



	public void setRestoreMessage(String restoreMessage) {
		this.restoreMessage = restoreMessage;
	}



	public String getStatusDialogMessage() {
		return statusDialogMessage;
	}



	public void setStatusDialogMessage(String statusDialogMessage) {
		this.statusDialogMessage = statusDialogMessage;
	}

	
	public String progressBackupComplete() {
		
		if (getBackupProcess() == BackupProcess.FAILED) return "";
		
		if (backupProcess == BackupProcess.CREATE) {
			refreshResourceManager();
			initBackupManager();
		}

		showMessageDialog(getBackupCompleteMessage(), FacesMessage.SEVERITY_INFO);
		backupProcess=BackupProcess.IDLE;
        RequestContext.getCurrentInstance().update(("form:msgs"));
		return "";
	}
	
	
	
	public void backupFailed(String error) {
		Utils.debug("backup failed");
		setBackupProcess(BackupProcess.FAILED);
		ExecutionBean.setProgressValue(100);
	
		setBackupCompleteMessage("Backup '"+selectedBackup.getName()+"' failed !"); // ??
		showError(error); // ??
		//showMessageDialog(error, FacesMessage.SEVERITY_ERROR);
	}
	
	

	public synchronized String getProgressText() {
		return progressText;
	}



	public synchronized void setProgressText(String progressText) {
		this.progressText = progressText;
	}



	public String getBackupCompleteMessage() {
		return backupCompleteMessage;
	}



	public void setBackupCompleteMessage(String message) {
		LoginBackupBean.backupCompleteMessage = message;
	}



	public static synchronized BackupProcess getBackupProcess() {
		return backupProcess;
	}



	public static synchronized void setBackupProcess(BackupProcess type) {
		LoginBackupBean.backupProcess = type;
	}


}
