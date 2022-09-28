package de.unifrankfurt.informatik.acoli.fid.webclient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration2.XMLConfiguration;

import de.unifrankfurt.informatik.acoli.fid.util.LocateUtils;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;


public class GuiConfiguration {

	private List<Boolean> visibleColumnsMain;
	private Properties properties = new Properties();
	private String propertiesFile = "";
	private LocateUtils locateUtils = new LocateUtils();
	
	
	public GuiConfiguration(XMLConfiguration fidConfig){
		
		// default values
		visibleColumnsMain = new ArrayList<Boolean>(Arrays.asList(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true));
		
		if (fidConfig.containsKey("RunParameter.guiPropertiesFile") &&
			!fidConfig.getString("RunParameter.guiPropertiesFile").trim().isEmpty()
			) {
			propertiesFile = fidConfig.getString("GUI.propertiesFile");
		} else {
			propertiesFile = locateUtils.getLocalFile("/gui.properties").getAbsolutePath();
		}
			
		readConfig();
	}
	
	
	public void readConfig(){

		if (!propertiesFile.isEmpty()) {
			
			try {
				
				BufferedInputStream stream;
				stream = new BufferedInputStream(new FileInputStream(propertiesFile));
				properties.load(stream);
				stream.close();
				
				// read visible columns for main data table
				String visibleColumnsMainString = properties.getProperty("visibleColumnsMain");
				String [] visibleColumnsMainSplit  = visibleColumnsMainString.split(",");
				visibleColumnsMain.clear();
				for (String b : visibleColumnsMainSplit) {
					visibleColumnsMain.add(Boolean.parseBoolean(b.trim()));
				}
				
				Utils.debug("Server GUI properties "+propertiesFile+" successfully read !");

				
			} catch (Exception e) {
				Utils.debug("Error while processing GUI properties file "+propertiesFile);
				e.printStackTrace();
			}	
		} else {
			Utils.debug("Error : Configuration parameter GUI.propertiesFile not available - configuration changes will not be saved !");
		}
	}
	
	public void writeConfig(){
		
		String visibleColumnsMainString = visibleColumnsMain.toString();
		visibleColumnsMainString = visibleColumnsMainString.substring(1, visibleColumnsMainString.length()-1);
		properties.setProperty("visibleColumnsMain", visibleColumnsMainString);
		try {
			OutputStream stream;
			stream = new BufferedOutputStream(new FileOutputStream(propertiesFile));
			properties.save(stream,"");
			stream.close();
			//Utils.debug("Properties file "+propertiesFile+" successfully written !");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	public List<Boolean> getVisibleColumnsMain() {
		
		// write after each change of column selection
		writeConfig();
		
		/*int i = 0;
		for (Boolean x : visibleColumnsMain) {
			Utils.debug(i+++" : "+x);
		}*/
		return visibleColumnsMain;
	}

	
	public void setVisibleColumnsMain(List<Boolean> visibleColumnsMain) {
		Utils.debug("setVisibleColumnsMain");
		this.visibleColumnsMain = visibleColumnsMain;
	}

}
