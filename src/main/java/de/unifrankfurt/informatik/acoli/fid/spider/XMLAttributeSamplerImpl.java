package de.unifrankfurt.informatik.acoli.fid.spider;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import de.unifrankfurt.informatik.acoli.fid.search.GWriter;
import de.unifrankfurt.informatik.acoli.fid.spider.XmlAttributeSamplerI;
import de.unifrankfurt.informatik.acoli.fid.types.ConllConverterChoice;
import de.unifrankfurt.informatik.acoli.fid.types.ConllConversionMode;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.xml.*;


public class XMLAttributeSamplerImpl implements XmlAttributeSamplerI {


    HashMap<String, HashMap<String, Long>> attr2Lit = new HashMap<>();
    HashMap<String, HashMap<String, Long>> attr2Uri = new HashMap<>();
    File xmlFile;
    private final static Logger LOGGER =
            Logger.getLogger(XMLAttributeSamplerImpl.class.getName());

    @Override
    public void sample(ResourceInfo resourceInfo, int attributeValues) {
        this.xmlFile = resourceInfo.getFileInfo().getResourceFile();
        XMLSampler xs = new XMLSampler(true);
        xs.sample(this.xmlFile, attributeValues, this.attr2Lit, this.attr2Uri, attributeValues*1000);
    }

    @Override
    public boolean makeConll(
    		ResourceInfo resourceInfo,
    		GWriter writer,
    		HashSet<String> allowedAttributes,
    		ConllConversionMode makeConllMode,
    		ConllConverterChoice conllConverterChoice,
    		int makeConllSampleSentenceCounts,
    		int makeConllAutoMaxFileSize)
    {
    	
        Template guessedTemplate;
        if (allowedAttributes.isEmpty()) {
            LOGGER.info("No allowed attributes, no Pseudo-CoNLL to produce.");
            return false;
        }
        //LOGGER.info("Making pseudo conll for file "+this.xmlFile.getAbsolutePath());
        LOGGER.info(allowedAttributes.size()+" allowed attributes.");
        try {

            HashMap<String, ArrayList<HashMap<String, String>>> overview = XMLSampler.collectOverview(XMLSampler.cutSample(this.xmlFile, 5000), true);
            HashMap<String, HashMap<String, Integer>> attributeFrequencies = XMLSampler.overview2AttributeFrequencies(overview);
            HashMap<String, HashMap<String, Integer>> prunedAttributeFrequencies = XMLSampler.pruneAttributeFrequencies(attributeFrequencies, allowedAttributes);
            guessedTemplate = TemplateGuesser.guessTemplateFromAttributeFrequencies(prunedAttributeFrequencies);
            LOGGER.info("Guessed Template: "+guessedTemplate);

        } catch (XMLStreamException e) {
            LOGGER.severe("Couldn't resample from conll for template guessing");
            return false;
        }

        File conllFile = new File(resourceInfo.getFileInfo().getResourceFile().getAbsolutePath() + ".conll");


        TemplateXMLConverter txc = new TemplateXMLConverter(new ArrayList<>(Arrays.asList(guessedTemplate)));
        GenericXMLConverter gxc = new GenericXMLConverter(new XMLSampler(true));
        ArrayList<String> allowedAttributesList = new ArrayList<>(allowedAttributes);
        try {
        	
        	de.unifrankfurt.informatik.acoli.fid.util.Utils.debug("makeConll parameter : ");
        	de.unifrankfurt.informatik.acoli.fid.util.Utils.debug("makeConllMode "+makeConllMode.name());
        	de.unifrankfurt.informatik.acoli.fid.util.Utils.debug("makeConllSampleSentenceCounts "+makeConllSampleSentenceCounts);
        	de.unifrankfurt.informatik.acoli.fid.util.Utils.debug("makeConllAutoMaxFileSize "+makeConllAutoMaxFileSize);
        	
        	// Evaluate file size if mode is AUTO
        	if (makeConllMode == ConllConversionMode.AUTO) {
        		if (resourceInfo.getFileInfo().getFileSizeAsMBytes() > makeConllAutoMaxFileSize) {
        			makeConllMode = ConllConversionMode.SAMPLE;
        		} else {
        			makeConllMode = ConllConversionMode.FULL;
        		}
        	}



        	switch (makeConllMode) {
        	
        	case SAMPLE :
                de.unifrankfurt.informatik.acoli.fid.util.Utils.debug("starting sample conll conversion ...");
                if (conllConverterChoice == ConllConverterChoice.TEMPLATE) {
                    txc.getSampleOfSizeKSentencesAsCoNLL(this.xmlFile, Utils.convertFileToPrintStream(conllFile), makeConllSampleSentenceCounts, guessedTemplate);
                }
                else {
                    gxc.getPseudoCoNLLOfSizeK(this.xmlFile, Utils.convertFileToPrintStream(conllFile), makeConllSampleSentenceCounts, allowedAttributesList );
                }
                break;
        	
        	case FULL :
                de.unifrankfurt.informatik.acoli.fid.util.Utils.debug("starting full conll conversion");
                if (conllConverterChoice == ConllConverterChoice.TEMPLATE) {
                    txc.getFullCoNLL(this.xmlFile, Utils.convertFileToPrintStream(conllFile), guessedTemplate, false);
                } else {
                    gxc.getFullPseudoCoNLL(this.xmlFile, Utils.convertFileToPrintStream(conllFile), allowedAttributesList);
                }
                break;
        	
            default :
                de.unifrankfurt.informatik.acoli.fid.util.Utils.debug("Error : makeConllMode "+makeConllMode.name()+ " is not supported !");
            	return false;
        	}
        	
       
        } catch (FileNotFoundException | XMLStreamException e) {
            LOGGER.severe("Couldn't write conll.");
            return false;
        }
        resourceInfo.getFileInfo().setTemporaryFilePath(conllFile.getAbsolutePath());
        HashMap<Integer, String> columnMapping;
        if (conllConverterChoice == ConllConverterChoice.TEMPLATE) {
            columnMapping = generateColumnMapping(new ArrayList<>(guessedTemplate.columnPaths.keySet()));
        } else {
            columnMapping = generateColumnMapping(allowedAttributesList);
        }
        resourceInfo.getFileInfo().setConllcolumn2XMLAttr(columnMapping);
        return true;

    }
        public HashMap<Integer, String> generateColumnMapping(ArrayList<String> attributes){
            HashMap<Integer, String> mapping = new HashMap<>();
            mapping.put(0, "id");
            for (int i = 0;i<attributes.size(); i++) {
                mapping.put(i+1, attributes.get(i));
            }
            return mapping;
        }

    @Override
    public HashMap<String, HashMap<String, Long>> getAttributes2LitObjects() {
        return this.attr2Lit;
    }

    @Override
    public HashMap<String, HashMap<String, Long>> getAttributes2URIObjects() {
        return this.attr2Uri;
    }
}
