package de.unifrankfurt.informatik.acoli.fid.webclient;


import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.primefaces.context.RequestContext;
import org.primefaces.event.SelectEvent;

import com.optimaize.langdetect.DetectedLanguage;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileBuilder;
import com.optimaize.langdetect.profiles.LanguageProfileWriter;

import de.unifrankfurt.informatik.acoli.fid.detector.OptimaizeLanguageTools1;
import de.unifrankfurt.informatik.acoli.fid.detector.TikaTools;
import de.unifrankfurt.informatik.acoli.fid.parser.ParserISONames;
import de.unifrankfurt.informatik.acoli.fid.types.LanguageMatch;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;


@ManagedBean(name="lpbean")
@ViewScoped
public class LoginLanguageBean implements Serializable {
	
	
	//@ManagedProperty(value="#{login.languageProfileList}")
	private List<LanguageProfile> languageProfileList = null;
	private List<LanguageProfile> filteredLanguageProfileList = null;
	private LanguageProfile selectedLanguageProfile = null;
	private String languageEvaluationInput = "Put text sample here";
	private String selectedLanguageEvalMode = "test";
	private String languageTestResult = "";
	private List<LanguageMatch> languageTestResults = new ArrayList<LanguageMatch>();
	private List<LanguageMatch> filteredLanguageTestResults = null;
	private LanguageMatch bestTestedLanguage = null;

	private static final long serialVersionUID = 1L;
	private LanguageProfile selectedDummyLP;
	private int tableFirstPage = 0;
	private String deleteMessage="Delete permanently ??";
	private boolean loaded=false;
	private LanguageMatch selectedDummyResult;
	
	private List<LanguageProfile> languageProfileListEval = new ArrayList<LanguageProfile>();
	private List<LanguageProfile> filteredLanguageProfileListEval = null;
	private LanguageProfile selectedDummyLPEval;
	
	private String inputTextHeader = "";
	private String startButtonText = "START";
	
	@ManagedProperty(value="#{login.fidConfig}")
	private XMLConfiguration fidConfig;
	private String uploadBtTitle="";
	
	private String manualISOInput;
	private String isoLanguageLabel;
	private Boolean overwriteLP=false;
	private final String dummyNewLPCode = "xyz";
	private String languageTrainingData="";

	
	//@EJB
    //ExecuterEjb executionEjb;
	

	@PostConstruct
    public void init() {
		
		if (loaded) return;
		loaded = true;
		Utils.debug("init");
		languageProfileList = new ArrayList<LanguageProfile> (OptimaizeLanguageTools1.getLanguageProfileMap().values());
		
	}
	
		
	public LanguageProfile getSelectedDummyLP() {
		return selectedDummyLP;
	}


	public void setSelectedDummyLP(LanguageProfile selectedDummyLP) {
		this.selectedDummyLP = selectedDummyLP;
	}


	public int getTableFirstPage() {
		return tableFirstPage;
	}


	public void setTableFirstPage(int tableFirstPage) {
		this.tableFirstPage = tableFirstPage;
	}
	
	
	public String getNameForLocale(String isoCode) {
		
		// support ISO6391 codes
		if (isoCode.length() != 3) {
			isoCode = TikaTools.getISO639_3CodeFromISOCode(isoCode);
		}
		if (ParserISONames.getIsoCodes2Names().containsKey(isoCode)) {
			return ParserISONames.getIsoCodes2Names().get(isoCode);
		} else {
			return "unknown";
		}
	}

	public String getDeleteMessage() {
		return deleteMessage;
	}

	public void setDeleteMessage(String deleteMessage) {
		this.deleteMessage = deleteMessage;
	}

	public List<LanguageProfile> getLanguageProfileList() {
		return languageProfileList;
	}

	public void setLanguageProfileList(List<LanguageProfile> languageProfileList) {
		this.languageProfileList = languageProfileList;
	}
	
	public List<LanguageProfile> getFilteredLanguageProfileList() {
		return filteredLanguageProfileList;
	}

	public void setFilteredLanguageProfileList(
			List<LanguageProfile> filteredLanguageProfileList) {
		this.filteredLanguageProfileList = filteredLanguageProfileList;
	}

	public void onContextMenu(SelectEvent event) {
		
		System.out.println("onContextMenu");
		
		if (event.getObject() == null) {
			Utils.debug("onContextMenu call with null object !");
			showError("onContextMenu call with null object !");
			return;
		}
		//Utils.debug(event.getObject().getClass().getName());
		LanguageProfile x = (LanguageProfile) event.getObject();
		
		this.selectedLanguageProfile = x;
		Utils.debug(x.getLocale().getLanguage());
		
		languageTrainingData="";
		languageEvaluationInput="";

		FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:inputSampleText");
		RequestContext.getCurrentInstance().reset("form:inputSampleText");
	}


	public boolean isLoaded() {
		return loaded;
	}


	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}


	public LanguageProfile getSelectedLanguageProfile() {
		return selectedLanguageProfile;
	}
	
	public String getSelectedProfileLanguage() {
		
		if (selectedLanguageProfile == null) return "";
		String lp6393Code = TikaTools.getISO639_3CodeFromISOCode(selectedLanguageProfile.getLocale().getLanguage());
		String langName = ParserISONames.getIsoCodes2Names().get(lp6393Code);
		if (langName != null) return langName;
		else return "unknown";
	}
	
	public void setSelectedLanguageProfile(LanguageProfile selectedLanguageProfile) {
		this.selectedLanguageProfile = selectedLanguageProfile;
	}
	
	public void selectLanguageProfile() {
    	System.out.println("select lang profile :"+selectedLanguageProfile);
    }
	
	public String initLanguageEvaluation(String mode) {

		System.out.println("initLanguageEvaluation");
		
		System.out.println("mode: "+mode);
		
		languageProfileListEval.clear();
		languageTestResult="";
		
		if(mode.equals("test")) {	
			
			setSelectedLanguageEvalMode("test");
			languageProfileListEval.add(selectedLanguageProfile);
			inputTextHeader="Input Text Sample";
			startButtonText="Start evaluation";
			uploadBtTitle="Upload Evaluation Data";
		} else {
			setSelectedLanguageEvalMode("train");
			inputTextHeader="Input Text Sample";
			startButtonText="Start training";
			languageTestResults.clear();
			languageTestResult="";
			uploadBtTitle="Upload Training Data";
			selectedLanguageProfile=null;
		}
		
		return "";
	}
	

	public String getLanguageEvaluationInput() {
		return languageEvaluationInput;
	}

	public void setLanguageEvaluationInput(String languageEvaluationInput) {
		this.languageEvaluationInput = languageEvaluationInput;
	}

	
	public String getSelectedLanguageEvalMode() {
		return selectedLanguageEvalMode;
	}

	public void setSelectedLanguageEvalMode(String selectedLanguageEvalMode) {
		this.selectedLanguageEvalMode = selectedLanguageEvalMode;
	}
	
	public void selectLanguageEvalMode() {
    	System.out.println("select lang eval mode  :"+selectedLanguageEvalMode);

    	if(selectedLanguageEvalMode.equals("test") && selectedLanguageProfile == null) {
    		selectedLanguageEvalMode = "train";
    		System.out.println("hello ");
    		return;
    	}
    	
    	initLanguageEvaluation(selectedLanguageEvalMode);
    }
	
	
	public void languageEvalStart() {
		
		System.out.println("languageEvalStart");
		
		switch (selectedLanguageEvalMode) {
		
		case "test":
			
			if (languageEvaluationInput.trim().isEmpty()) {
				showError("Evaluation text empty !");
				return;
			}
	
			// show best 5 languages + result of evaluated language
			ArrayList<DetectedLanguage> dls = OptimaizeLanguageTools1.detectRawISO639_3Languages(languageEvaluationInput);
			ArrayList<LanguageMatch> allFoundLanguages = OptimaizeLanguageTools1.detectedLanguage2LanguageMatch(dls);
			bestTestedLanguage = OptimaizeLanguageTools1.computeBestLanguageMatching(dls);
			
			if (bestTestedLanguage == null) {
				languageTestResult = "ERROR";
				return;
			} else {
				
				// select bestTestedLanguage in list
				for (LanguageMatch lm : allFoundLanguages) {
					if (lm.getLanguageISO639Identifier().equals(bestTestedLanguage.getLanguageISO639Identifier())) {
						lm.setSelected(true);
						break;
					}
				}
				
				if (bestTestedLanguage.getLanguageISO639Identifier().equals(selectedLanguageProfile.getLocale().getLanguage())) {
					languageTestResult = "OK : the profile was selected for this text";
				} else {
					languageTestResult = "Fail : "+bestTestedLanguage.getLanguageNameEn()+" selected instead of "+getNameForLocale(selectedLanguageProfile.getLocale().getLanguage())+" ("+selectedLanguageProfile.getLocale().getLanguage()+")";
				}
			}
			
			languageTestResults = allFoundLanguages;
			break;
		
		
		case "train":
			
			if (languageEvaluationInput.trim().isEmpty()) {
				showInfo("Training text empty !");
				return;
			}

			 LanguageProfile languageProfile = OptimaizeLanguageTools1.createOptimaizeLanguageProfile(
					languageEvaluationInput,
					dummyNewLPCode,
					null);
			selectedLanguageProfile = languageProfile;
			languageTrainingData = new String(languageEvaluationInput);
			languageProfileListEval.clear();
			languageProfileListEval.add(languageProfile);
			OptimaizeLanguageTools1.getLanguageProfiles().add(languageProfile);
			showInfo("Training is finished !");
			break;
		default :
			break;
		}
	}
	
	
	public String getLanguageTestResult() {
		return languageTestResult;
	}

	public void setLanguageTestResult(String languageTestResult) {
		this.languageTestResult = languageTestResult;
	}
	
	public void showError(String message) {
		Utils.debug(message);
		showMessage(message, FacesMessage.SEVERITY_ERROR);
	}
	
	public void showInfo(String message) {
		showMessage(message, FacesMessage.SEVERITY_INFO);
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

	public List<LanguageMatch> getLanguageTestResults() {
		return languageTestResults;
	}


	public void setLanguageTestResults(List<LanguageMatch> languageTestResults) {
		this.languageTestResults = languageTestResults;
	}


	public List<LanguageMatch> getFilteredLanguageTestResults() {
		return filteredLanguageTestResults;
	}


	public void setFilteredLanguageTestResults(
			List<LanguageMatch> filteredLanguageTestResults) {
		this.filteredLanguageTestResults = filteredLanguageTestResults;
	}


	public LanguageMatch getBestTestedLanguage() {
		return bestTestedLanguage;
	}


	public void setBestTestedLanguage(LanguageMatch bestTestedLanguage) {
		this.bestTestedLanguage = bestTestedLanguage;
	}


	public LanguageMatch getSelectedDummyResult() {
		return selectedDummyResult;
	}


	public void setSelectedDummyResult(LanguageMatch selectedDummyResult) {
		this.selectedDummyResult = selectedDummyResult;
	}


	public List<LanguageProfile> getLanguageProfileListEval() {
		return languageProfileListEval;
	}


	public void setLanguageProfileListEval(List<LanguageProfile> languageProfileListEval) {
		this.languageProfileListEval = languageProfileListEval;
	}


	public List<LanguageProfile> getFilteredLanguageProfileListEval() {
		return filteredLanguageProfileListEval;
	}


	public void setFilteredLanguageProfileListEval(
			List<LanguageProfile> filteredLanguageProfileListEval) {
		this.filteredLanguageProfileListEval = filteredLanguageProfileListEval;
	}
	

	public LanguageProfile getSelectedDummyLPEval() {
		return selectedDummyLPEval;
	}


	public void setSelectedDummyLPEval(LanguageProfile selectedDummyLPEval) {
		this.selectedDummyLPEval = selectedDummyLPEval;
	}
	


	public String getInputTextHeader() {
		return inputTextHeader;
	}


	public void setInputTextHeader(String inputTextHeader) {
		this.inputTextHeader = inputTextHeader;
	}


	public String getStartButtonText() {
		return startButtonText;
	}


	public void setStartButtonText(String startButtonText) {
		this.startButtonText = startButtonText;
	}
	
	
	
	public String uploadLanguageProfileData() {
		
		Utils.debug("upload language file");
		
		try {
			String fileName = FileUploadBean.getUploadedFile().getFileName();
			String inputFilePath = new File (new File(fidConfig.getString("RunParameter.ServiceUploadDirectory")),fileName).getAbsolutePath();			
			Utils.debug("inputFilePath :"+inputFilePath);
			
			// Write uploaded file to local fs
			FileUploadBean.getUploadedFile().write(inputFilePath);
			languageEvaluationInput = FileUtils.readFileToString(new File(inputFilePath), StandardCharsets.UTF_8);
			
			System.out.println(languageEvaluationInput);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		return "";
	}
	
	
	public XMLConfiguration getFidConfig() {
		return fidConfig;
	}


	public void setFidConfig(XMLConfiguration fidConfig) {
		this.fidConfig = fidConfig;
	}


	public String getUploadBtTitle() {
		return uploadBtTitle;
	}


	public void setUploadBtTitle(String uploadBtTitle) {
		this.uploadBtTitle = uploadBtTitle;
	}
	
	
	public String deleteLanguageProfile() {
		
		if(selectedLanguageProfile == null) return"";
		Utils.debug("deleteLanguageProfile "+selectedLanguageProfile);
		
		// check if lp is manual - otherwise will not be deleted
		File targetDirectory = new File(fidConfig.getString("RunParameter.OptimaizeManualProfilesDirectory"));
		String iso = selectedLanguageProfile.getLocale().getLanguage();
		File lpFile = new File(targetDirectory, iso);
		if (lpFile.exists()) {
			try {
				Utils.debug("Deleting "+lpFile.getAbsolutePath());
				lpFile.delete();
				
				// remove from list and update
				OptimaizeLanguageTools1.getLanguageProfileMap().remove(iso);
				//OptimaizeLanguageTools1.updateLanguageDetector();
				//languageProfileList = new ArrayList<LanguageProfile> (OptimaizeLanguageTools1.getLanguageProfileMap().values());
				
				//showStickyMessage("Language profile '"+iso+"' was sucessfully deleted !", FacesMessage.SEVERITY_INFO);
				return "login-languages?faces-redirect=true";
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			showError("Internal profile can not be deleted - instead create a new profile for '"+iso+"' to override it!");
		}
		return "";
	}
	
	
	public void showSaveProfileDialog() {
		
		if(selectedLanguageProfile == null || !selectedLanguageProfile.getLocale().getLanguage().equals(dummyNewLPCode)) {
			showInfo("Create new profile first !");
			return;
		}
		
		overwriteLP=false;
		RequestContext context = RequestContext.getCurrentInstance();
		context.execute("PF('saveLP').show();");
	}
	
	
	public String saveNewLanguageProfile() {
		
		// language profile is valid checked in showSaveProfileDialog (above)
		
		manualISOInput = manualISOInput.trim().toLowerCase();
		if (manualISOInput.isEmpty() 	||
			manualISOInput.length() != 3 ||
			manualISOInput.matches(".*\\d.*")
		) 
		{
			showError("Incorrect ISO-639 code : must have 3 characters !");
			return"";
		}
		
		// check if profile for language already exists
		for (LanguageProfile lp : OptimaizeLanguageTools1.getLanguageProfiles()) {
			
			String lpCode = TikaTools.getISO639_3CodeFromISOCode(lp.getLocale().getLanguage());
			if (lpCode == null) continue;
			
			if (lpCode.equals(manualISOInput) && !overwriteLP) {
				showError("A language profile with ISO-code '"+manualISOInput+"' already exists !");
				return"";
			}
			
			/*if (lpCode.length() > lp.getLocale().getLanguage().length()) {
			System.out.println("Expanded ISO :");
			System.out.println(lpCode);
			System.out.println(lp.getLocale().getLanguage());
		    }*/
		}
		
		// Save new profile
		// Profiles come from different sources 
		//    - internal profiles	(included in library source code)
		//    - external profiles	(included in RunParameter.OptimaizeExtraProfilesDirectory folder)
		//    - manual   profiles	(included in RunParameter.OptimaizeManualProfilesDirectory folder)
		//		- created with the LP Manager application 
		// Newly created manual profiles generally override internal/external profiles (on startup
		// it is checked if a manual profile for a certain language overrides a internal/external profile.
		// In this case the manual profile will be loaded in favour of an internal/external profile
		
		// I. Save manual profile to RunParameter.OptimaizeManualProfilesDirectory folder
		File targetDirectory = new File(fidConfig.getString("RunParameter.OptimaizeManualProfilesDirectory"));
		LanguageProfile newLP=null;
		try {
			// Build profile again with desired language code
			 newLP = OptimaizeLanguageTools1.createOptimaizeLanguageProfile(
							languageTrainingData,
							manualISOInput,
							null);
			
			// write profile to file
			String error = OptimaizeLanguageTools1.writeLanguageProfile(newLP, targetDirectory, overwriteLP);
			if (!error.isEmpty()) {
				if (error.contains("exists")) 
					error="A language profile with ISO-code '"+manualISOInput+"' already exists !";				
				showError(error);
				return"";
			}
		} catch (Exception e) {
			e.printStackTrace();
			return"";
		}
		
		// II. Load the manual profile into the memory (OptimaizeLanguageTools1.languageProfiles)
		//     and unload an existing internal/external profile for that language,
		//     e.g. add new profile 'deu' and remove old internal profile 'de' or old external profile 'deu'
		
		// Replace existing or save new lp
		OptimaizeLanguageTools1.getLanguageProfileMap().put(manualISOInput, newLP);
		OptimaizeLanguageTools1.updateLanguageDetector();
		languageProfileList = new ArrayList<LanguageProfile> (OptimaizeLanguageTools1.getLanguageProfileMap().values());
		
		// show message
		computeISOLanguage(); // set language name in case not created by key-trigger
		showInfo("Saved new language profile for '"+manualISOInput+"' ("+isoLanguageLabel+") sucessfully !");
		return "";
		//return "login-languages?faces-redirect=true"; // triggers init-method and rereads lp-list
	}
	
	
	public void computeISOLanguage() {
		if (manualISOInput.length() != 3) {
			setIsoLanguageLabel("unknown language");
		} else {
			setIsoLanguageLabel(ParserISONames.getIsoCodes2Names().get(manualISOInput));
		}
	}


	public String getManualISOInput() {
		return manualISOInput;
	}


	public void setManualISOInput(String manualISOInput) {
		this.manualISOInput = manualISOInput;
	}


	public String getIsoLanguageLabel() {
		return isoLanguageLabel;
	}


	public void setIsoLanguageLabel(String isoLanguageLabel) {
		this.isoLanguageLabel = isoLanguageLabel;
	}


	public Boolean getOverwriteLP() {
		return overwriteLP;
	}


	public void setOverwriteLP(Boolean overwriteLP) {
		this.overwriteLP = overwriteLP;
	}

}
