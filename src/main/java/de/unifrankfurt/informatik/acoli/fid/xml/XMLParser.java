package de.unifrankfurt.informatik.acoli.fid.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import de.unifrankfurt.informatik.acoli.fid.parser.CSVParserA;
import de.unifrankfurt.informatik.acoli.fid.search.GWriter;
import de.unifrankfurt.informatik.acoli.fid.types.ModelType;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.xml.SubtreeGenerator;
import de.unifrankfurt.informatik.acoli.fid.xml.Template;
import de.unifrankfurt.informatik.acoli.fid.xml.TemplateXMLConverter;
import de.unifrankfurt.informatik.acoli.fid.xml.Utils;

public class XMLParser extends CSVParserA {

	private final static Logger LOGGER =
			Logger.getLogger(XMLParser.class.getName());
	TemplateXMLConverter txc;
	Template template;
	String sentenceName;
	SubtreeGenerator sg;
	int n;


	@Deprecated
	public XMLParser(GWriter graphWriter, String versionString, String sentenceName, Template template) { 
		super(graphWriter, ModelType.valueOf("UDEP"), versionString);
		this.txc = new TemplateXMLConverter();
		this.template = template;
		this.sentenceName = sentenceName;
	}

	public XMLParser(GWriter graphWriter, SubtreeGenerator sg, Template template) {
		super(graphWriter, ModelType.valueOf("UDEP"), "version 1.0");
		this.txc = new TemplateXMLConverter();
		this.template = template;
		this.sg = sg;
	}
	public XMLParser(GWriter graphWriter, SubtreeGenerator sg, Template template, int resultingNoSentences) {
		super(graphWriter, ModelType.valueOf("UDEP"),  "version 1.0");
		this.txc = new TemplateXMLConverter();
		this.template = template;
		this.sg = sg;
		this.n = resultingNoSentences;
	}
	@Override
	public boolean parse(ResourceInfo resourceInfo) {
		this.sg.reset(); // reset the state of the subtreeGenerator.

		File conllFile = new File(resourceInfo.getFileInfo().getResourceFile().getAbsolutePath()+".conll");
		LOGGER.info("Writing conll to file :"+conllFile.getAbsolutePath());
		try {
			this.txc.getSampleOfSizeKSentencesAsCoNLL(this.sg, Utils.convertFileToPrintStream(conllFile), this.n, this.template);
		} catch (FileNotFoundException e){
			e.printStackTrace();
			LOGGER.severe("Couldn't convert "+resourceInfo.getFileInfo().getFileName());
			return false;
		}
		
		resourceInfo.getFileInfo().setTemporaryFilePath(conllFile.getAbsolutePath());

		HashMap<Integer, String> columnMapping = createColumnMapping(this.template);
		resourceInfo.getFileInfo().setConllcolumn2XMLAttr(columnMapping);

		return true;
	}

	/**
	 * Recieves a template and creates a mapping for each conll column to its original pseudo-xpath.
	 * @param template
	 * @return
	 */
	public HashMap<Integer, String> createColumnMapping(Template template) {
		HashMap<Integer, String> columnMapping = new HashMap<>();
		String path2Attributes = template.getSentencePath()+template.wordPath+"/";
		ArrayList<String> columns = new ArrayList<>(template.columnPaths.keySet());
		for (int i = 0; i < columns.size(); i++) {
		    String currentColumn = columns.get(i);
            if (currentColumn.equals("FEATS")) { // Special symbol
                columnMapping.put(i + 1, path2Attributes+"FEATS");
            } else {
                // bit convoluted iteration since we want to maintain the index and need to access to the string paths
                String columnPath = path2Attributes + template.columnPaths.get(currentColumn);
                columnMapping.put(i + 1, columnPath);
            }
		}
		return columnMapping;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}
}
