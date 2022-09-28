package de.unifrankfurt.informatik.acoli.fid.parser;

import java.net.MalformedURLException;
import java.net.URL;

import de.unifrankfurt.informatik.acoli.fid.search.GWriter;
import de.unifrankfurt.informatik.acoli.fid.types.ModelType;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;

public abstract class CSVParserA {
	
	protected ModelType model;
	private String versionString;
	private URL formatSpec;
	protected CSVParserConfig config;
	protected GWriter graphWriter;
	

	public CSVParserA (GWriter graphWriter, ModelType modelType, String versionString) {
		this.setGraphWriter(graphWriter);
		this.setModel(modelType);
		this.versionString = versionString;
	}
	
	
	public CSVParserA (CSVParserConfig config, GWriter graphWriter, ModelType modelType,  String versionString) {
		this.config = config;
		this.setGraphWriter(graphWriter);
		this.setModel(modelType);
		this.versionString = versionString;
	}
	
	
	public CSVParserA (CSVParserConfig config, ModelType modelType, String versionString) {
		this.config = config;
		this.setModel(modelType);
		this.versionString = versionString;
	}


	public URL getFormatSpec() {
		return formatSpec;
	}

	public void setFormatSpec(String formatSpecURL) {
		
		try {
			this.formatSpec = new URL(formatSpecURL);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}


	public String getVersionString() {
		return versionString;
	}


	public void setVersionString(String versionString) {
		this.versionString = versionString;
	}
	
	/**
	 * Parse resource
	 * @param resourceInfo
	 * @return success
	 */
	public abstract boolean parse (ResourceInfo resourceInfo);


	public CSVParserConfig getConfig() {
		return config;
	}


	public void setConfig(CSVParserConfig config) {
		this.config = config;
	}


	public ModelType getModel() {
		return model;
	}


	public void setModel(ModelType model) {
		this.model = model;
	}


	public GWriter getGraphWriter() {
		return graphWriter;
	}


	public void setGraphWriter(GWriter graphWriter) {
		this.graphWriter = graphWriter;
	}
	
	
	public abstract void reset ();
		
	
}
