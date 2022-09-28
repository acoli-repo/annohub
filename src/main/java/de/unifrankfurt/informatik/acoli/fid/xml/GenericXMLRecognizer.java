package de.unifrankfurt.informatik.acoli.fid.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

/**
 * class containing functions to recognize .xml files that contain some sort of linguistic annotations.
 * There are two hyper parameters to set:
 * <ul>
 *     <li>ALL_CHECKS_THRESHOLD: What percentage of checks must pass to accept a given annotation as relevant?</li>
 *     <li>SINGLE_CHECK_THRESHOLD: What percentage of annotations must pass a single check for the check to be true?</li>
 * </ul>
 */
public class GenericXMLRecognizer {
    private float ALL_CHECKS_THRESHOLD = 0.6f; // TODO: where to parametrize this?
    public void setAllChecksThreshold(float threshold) {
        this.ALL_CHECKS_THRESHOLD = threshold;
    }
    public float getAllChecksThreshold() {
        return this.ALL_CHECKS_THRESHOLD;
    }

    private float SINGLE_CHECK_THRESHOLD = 0.8f;
    public void setSingleCheckThreshold(float threshold) {
        this.SINGLE_CHECK_THRESHOLD = threshold;
    }
    public float getSingleCheckThreshold() {
        return this.SINGLE_CHECK_THRESHOLD;
    }
    private boolean RETRIEVE_XPATH = false;

    public void setRetrieveXPaths(boolean mode) {
        this.RETRIEVE_XPATH = mode;
    }

    public GenericXMLRecognizer() {
    }

    public GenericXMLRecognizer(boolean retrieveXPaths) {
        this.setRetrieveXPaths(retrieveXPaths);
    }
    // TODO setters getters

    static private String SYNOPSIS = "synopsis: GenericXMLRecognizer -f FILE [-s SAMPLE_SIZE] [-t THRESHOLD] [--silent]\n"+
            "\tFILE          XML file to check\n"+
            "\tSAMPLE_SIZE   default 500, How many nodes to sample\n"+
            "\tTHRESHOLD     default 0.6, float what percentage of checks should pass\n"+
            "\t--silent      no logging output (also not this synopsis!)\n";

    private final static Logger LOGGER =
            Logger.getLogger(GenericXMLRecognizer.class.getName());
    // TODO: make simple one-sentence conll
    // TODO: also it would be nice to have arbitrary check number, but I have no clue about reflection, maybe later.


    public HashMap<String, HashMap<String, Integer>> findPossibleAttributes(File xmlFile) throws XMLStreamException {
        int maxSampleSize = XMLSampler.getNumberOfStartElementsInEntireFile(xmlFile);
        return findPossibleAttributes(xmlFile, maxSampleSize);
    }

    public HashMap<String, HashMap<String, Integer>> findPossibleAttributes(File xmlFile, int sampleSize) throws XMLStreamException {
        HashMap<String, ArrayList<HashMap<String, String>>> overview = XMLSampler.collectOverview(XMLSampler.cutSample(xmlFile, sampleSize), RETRIEVE_XPATH);
        return findPossibleAttributes(overview);
    }
    public HashMap<String, HashMap<String, Integer>> findPossibleAttributes(HashMap<String, ArrayList<HashMap<String, String>>> overview) throws XMLStreamException {

        HashMap<String, HashMap<String, Integer>> counts = XMLSampler.overview2AttributeFrequencies(overview);

        HashMap<String, HashMap<String, Integer>> result = new HashMap<>();
        for (String attribute : counts.keySet()){
            HashMap<String, Integer> overviewOfAttribute = counts.get(attribute);
            float maxHitCount = 3; // TODO: if new method, ++ this.
            float hitCount = 0;
            // execute checks:
            if (this.hasSmallDomain(overviewOfAttribute)){
                hitCount++;
            }
            if (this.hasUppercaseDomain(overviewOfAttribute)){
                hitCount++;
            }
            if (this.domainHasNoSpaces(overviewOfAttribute)){
                hitCount++;
            }

            // add to result, if candidate for linguistic annotation
            if (hitCount/maxHitCount>this.ALL_CHECKS_THRESHOLD){
                result.put(attribute, overviewOfAttribute);
            }
        }
        return result;
    }

    // CHECK FUNCTIONS counts -> boolean

    /**
     * Check if all values of attributes have a limited vocabulary. This is most likely the case for
     * linguistic annotations.
     * @param counts annotation -> how often it occurred in an attribute
     * @return if this check is true
     */
    public boolean hasSmallDomain(HashMap<String, Integer> counts){
        float factor = 0.4f;
        double tokens = counts.values()
                .stream()
                .mapToInt(Integer::intValue)
                .sum();
        //System.err.println(sum+" "+sum*factor);
        double type = counts.size();
        //LOGGER.info("type: "+type+", tokens: "+tokens+", ttr: "+type/tokens+", factor: "+factor+" ttr<factor: "+(type/tokens < factor));
        return type/tokens < factor;
    }

    /**
     * Check if most attributes are in caps. This is likely for linguistic annotations like postags, however
     * dependency relations often are not.
     * @param counts annotation -> how often it occurred in an attribute
     * @return if this check is true
     */
    private boolean hasUppercaseDomain(HashMap<String, Integer> counts) {
        long upperCaseCount = counts.keySet()
                .stream()
                .filter(e -> !e.equals(e.toLowerCase()))
                .count();
        return upperCaseCount >= counts.size() * this.SINGLE_CHECK_THRESHOLD;
    }

    /**
     * Check if most attributes do not contain spaces. This is most likely the case for linguistic annotations.
     * @param counts annotation -> how often it occurred in an attribute
     * @return if this check is true
     */
    private boolean domainHasNoSpaces(HashMap<String, Integer> counts){
        long noSpaceCount = counts.keySet()
                .stream()
                .filter(e -> !e.contains(" "))
                .count();
        return noSpaceCount >= counts.size() * this.SINGLE_CHECK_THRESHOLD;
    }



    /**
     * Main method to access all functionality from cmd line.
     * @param args arguments
     */
    public static void main(String[] args){
        String filePath = "";
        HashMap<String, HashMap<String, Integer>> result;
        GenericXMLRecognizer gxr = new GenericXMLRecognizer();
        int sampleSize = 0;
        for ( int i = 0; i < args.length; i++ ) {
            switch (args[i]) {
                case "-f":
                    i++;
                    filePath = args[i];
                    break;
                case "-s":
                    i++;
                    sampleSize = Integer.parseInt(args[i]);
                    break;
                case "-t":
                    i++;
                    gxr.setAllChecksThreshold(Float.parseFloat(args[i]));
                    break;
                case "--silent":
                    LOGGER.setLevel(Level.OFF);
                    break;
                case "--xPaths":
                    gxr.setRetrieveXPaths(true);
                default:
                    break;
            }
        }

        LOGGER.info(SYNOPSIS);
        if (filePath.length() == 0){
            LOGGER.severe("NO FILE PROVIDED.");
            System.exit(1);
        }
        if (! new File(filePath).exists()) {
            LOGGER.severe("File Not Found: " + filePath);
            System.exit(1);
        }
        try {
            LOGGER.info("FILE: "+filePath);
            if (sampleSize == 0) {
                result = gxr.findPossibleAttributes(new File(filePath));
            } else {
                result = gxr.findPossibleAttributes(new File(filePath), sampleSize);
            }
            for (Map.Entry<String, HashMap<String, Integer>> entry : result.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
        } catch (XMLStreamException e){
            System.err.println("Couldn't parse file at "+filePath);
        }
    }
}
