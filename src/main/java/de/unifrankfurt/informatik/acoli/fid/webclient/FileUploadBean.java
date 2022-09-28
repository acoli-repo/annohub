package de.unifrankfurt.informatik.acoli.fid.webclient;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;


@ManagedBean
@SessionScoped
public class FileUploadBean {
     
    private static UploadedFile file = null;
 
    public UploadedFile getFile() {
        return file;
    }
 
    public void setFile(UploadedFile ufile) {
        file = ufile;
    }
     
    
    public void fileUploadListener(FileUploadEvent e){
    	
		// Get uploaded file from the FileUploadEvent
		file = e.getFile();
		
		// Print out the information for the file
		Utils.debug("Uploaded File Name Is :: "+file.getFileName()+" :: Uploaded File Size :: "+file.getSize());
	}
    
    
    public String getFileName() {
    	if (file != null)
    		return "Uploaded :  "+file.getFileName()+"  "+file.getSize()+ " Bytes";
    	else
    		return "";
    }
    
    public static Long getFileSize() {
    	if (file != null)
    		return file.getSize();
    	else
    		return null;
    }
    
    
    public static void show() {
    	if (file != null)
    		Utils.debug("+++ "+ file.getFileName());
    }
    
    
    public static UploadedFile getUploadedFile() {
    	return file;
    }
    
    public static void resetUploadedFile() {
    	file = null;
    }
   
    public void showInfo(String message) {
	   	 FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "", message);
	     FacesContext.getCurrentInstance().addMessage(null, msg);        
	}
}