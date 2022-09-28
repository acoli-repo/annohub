package de.unifrankfurt.informatik.acoli.fid.xml;

import static de.unifrankfurt.informatik.acoli.fid.xml.Utils.isURL;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import com.ctc.wstx.exc.WstxEOFException;

/**
 * XMLSampler contains various functions to sample nodes, attributes and values from an xml document
 * and represent those samples in different ways.
 */
public class XMLSampler {

    public void setRetrieveXPaths(boolean retrieveXpath) {
        this.RETRIEVE_XPATH = retrieveXpath;
    }
    public boolean getRetrieveXPaths() {
        return this.RETRIEVE_XPATH;
    }

    private boolean RETRIEVE_XPATH = false;
    private final static Logger LOGGER = Logger.getLogger(XMLSampler.class.getName());

    public XMLSampler() {
    }

    public XMLSampler(boolean samplePseudoXPath) {
        this.RETRIEVE_XPATH = samplePseudoXPath;
    }


    static public int getNumberOfStartElementsInEntireFile(File xmlFile) throws XMLStreamException {
        // Setup IO
        LOGGER.info("Getting length of file "+xmlFile.getAbsolutePath());
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(xmlFile);
        } catch (FileNotFoundException e) {
            LOGGER.warning("Couldn't find file "+xmlFile.getAbsolutePath());
            return 0;
        }
        XMLInputFactory staxFactory = XMLInputFactory.newInstance();
        staxFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        XMLEventReader staxReader = staxFactory.createXMLEventReader(fileReader);
        int numberOfStartElements = 0;
        while (staxReader.hasNext()) {
            XMLEvent nextEvent = staxReader.nextEvent();
            if (nextEvent.isStartElement()) {
                numberOfStartElements++;
            }
        }
        LOGGER.info("File "+xmlFile.getAbsolutePath()+" has "+numberOfStartElements+" nodes.");
        return numberOfStartElements;
    }

    static public boolean cutSampleByReference(XMLEventReader staxReader, Document targetDocument, Integer sizeOfSample) throws XMLStreamException {
        int c = 0;
        XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
        XMLEventWriter writer = outputFactory.createXMLEventWriter(new DOMResult(targetDocument));
        while (staxReader.hasNext()) {
            try {
                XMLEvent nextEvent = staxReader.nextEvent();

                // we only count start elements to have a better control over number of attributes
                if (nextEvent.isStartElement()) {
                    // LOGGER.info("Counting StartElement (#"+c+"): "+nextEvent.asStartElement().getName());
                    c++;
                }
                if (nextEvent.getEventType() != XMLEvent.DTD) {
                    writer.add(nextEvent);
                }
                if (c >= sizeOfSample) {
                    break;
                }
        } catch (WstxEOFException e){
                LOGGER.warning("WstXEOFException");
            break;
        }

        }

        if (c < sizeOfSample){
            LOGGER.warning("Node count for sample ("+sizeOfSample+") too small, entire file has "+c+" StartElements.");
        }
        LOGGER.info("Sampled "+c+" StartElements from file.");
        return staxReader.hasNext();
    }
    /**
     * receives an XML File and cuts a sample of size size. Each item i1,...,in is one
     * XML StartElement.
     * @param xmlFile a xml file that is to be sampled
     * @param n number of StartElement to sample
     * @return a DOM sample from the xml file
     */
    static public Document cutSample(File xmlFile, Integer n) {
        Document sample = null;
        LOGGER.info("Cutting sample from "+xmlFile.getAbsolutePath()+" of size "+n+"..");
        try {
            // Setup IO
            FileReader fileReader = new FileReader(xmlFile);
            XMLInputFactory staxFactory = XMLInputFactory.newInstance();
            staxFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            XMLEventReader staxReader = staxFactory.createXMLEventReader(fileReader);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            sample = db.newDocument();
            cutSampleByReference(staxReader, sample, n);

        } catch (XMLStreamException | FileNotFoundException | ParserConfigurationException e) {
            e.printStackTrace();
        }
        return sample;
    }

    public XMLEventReader configureXMLEventReader(File file) throws XMLStreamException, FileNotFoundException {
        FileReader fileReader = new FileReader(file);
        XMLInputFactory xif = XMLInputFactory.newInstance();
        xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        return xif.createXMLEventReader(fileReader);
    }
    public DocumentBuilder configureDocumentBuilder() {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            return dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            return null;
        }
    }
    public HashMap<String, ArrayList<HashMap<String, String>>> collectOverview(Document document) throws XMLStreamException {
        return collectOverview(document, this.RETRIEVE_XPATH);
    }
    public static HashMap<String, Integer> attributeFrequency2PathFrequency(HashMap<String, HashMap<String, Integer>> attributeFrequencies) {
        HashMap<String, Integer> pathFrequencies = new HashMap<>();
        for (Map.Entry<String, HashMap<String, Integer>> entry : attributeFrequencies.entrySet()) {
            String[] splitPath = entry.getKey().split("/");
            String[] cutPath = Arrays.copyOfRange(splitPath, 0, splitPath.length-1);
            pathFrequencies.put(String.join("/", cutPath), entry.getValue().values().stream().mapToInt(Integer::intValue).sum());
        }
        return pathFrequencies;
    }
    public static HashMap<String, HashMap<String,Integer>> pruneAttributeFrequencies(HashMap<String, HashMap<String,Integer>> attributeFrequencies, HashSet<String> allowedAttributes) {
        HashMap<String, HashMap<String,Integer>> prunedAttributeFrequencies = new HashMap<>();
        for (Map.Entry<String, HashMap<String, Integer>> entry : attributeFrequencies.entrySet()) {
            if (allowedAttributes.contains(entry.getKey())) {
                prunedAttributeFrequencies.put(entry.getKey(), entry.getValue());
            }
        }
        if (!prunedAttributeFrequencies.keySet().containsAll(allowedAttributes)) {
            LOGGER.warning("Attribute frequencies to be pruned did not contain all allowed attributes!");
            LOGGER.warning("Allowed Attributes: "+allowedAttributes);
            LOGGER.warning("Unpruned frequencies: "+attributeFrequencies.keySet());
            allowedAttributes.removeAll(prunedAttributeFrequencies.keySet());
            LOGGER.warning("Missing: "+allowedAttributes);
        }
        return prunedAttributeFrequencies;
    }
    public static HashMap<String, ArrayList<HashMap<String, String>>> pruneOverviewByPath(HashMap<String, ArrayList<HashMap<String, String>>> overview, HashSet<String> allowedAttributes) {
        HashMap<String, ArrayList<HashMap<String, String>>> prunedOverview = new HashMap<>();
        for (Map.Entry<String, ArrayList<HashMap<String, String>>> entry : overview.entrySet()) {
            if (allowedAttributes.contains(entry.getKey())) {
                prunedOverview.put(entry.getKey(), entry.getValue());
            }
        }
        if (!prunedOverview.keySet().containsAll(allowedAttributes)) {
            LOGGER.warning("Overview to be pruned did not contain all allowed attributes!");
            LOGGER.warning("Allowed Attributes: "+allowedAttributes);
            LOGGER.warning("Unpruned Overview: "+overview.keySet());
            allowedAttributes.removeAll(prunedOverview.keySet());
            LOGGER.warning("Missing: "+allowedAttributes);
        }
        return prunedOverview;
    }
    /**
     * Example (json-like map notation for ease of reading):
     * {root/s/w :
     *  [
     *   {
     *    hun : CC,
     *    lem : and
     *   },
     *   {
     *    hun: DT,
     *    lem: the
     *   }
     *  ]
     * }, ...
     * @param document a DOM tree
     * @return a HashMap of Path-to-Node -> attribute name -> list of values
     * @throws XMLStreamException if malformed xml
     */
    public static HashMap<String, ArrayList<HashMap<String, String>>> collectOverview(Document document, boolean retrieve_XPath) throws XMLStreamException {
        XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(new DOMSource(document));
        HashMap<String, ArrayList<HashMap<String, String>>> overview = new HashMap<>();
        Stack<String> xPathStack = new Stack<>();

        while (reader.hasNext()){
            XMLEvent nextEvent = (XMLEvent) reader.next();
            if (nextEvent.isStartElement()){
                // Cleaning up the path
                String nameSpace = nextEvent.asStartElement().getName().getNamespaceURI();
                String elementName = nextEvent.asStartElement().getName().toString();
                if (nameSpace.length()>1) {
                    // removes namespace annotation, maybe we might as well keep it?
                    elementName = elementName.replace("{" + nameSpace + "}", "");
                }

                xPathStack.push(elementName);
                if (retrieve_XPath) {
                    elementName = Utils.stack2Path(xPathStack);
                }
                Iterator items = nextEvent.asStartElement().getAttributes();
                HashMap<String, String> entriesInNode = new HashMap<>();
                while (items.hasNext()){
                    Attribute att = (Attribute) items.next();
                    if (entriesInNode.keySet().contains(att.getName().toString())) {
                        LOGGER.warning("Overwriting entriesInNode value when collecting overview! DATA WILL BE MISSING FROM SAMPLE");
                    }
                    entriesInNode.put(att.getName().toString(), att.getValue());
                }
                // put the entry into the overview
                ArrayList<HashMap<String, String>> temp = overview.getOrDefault(elementName, new ArrayList<>());
                temp.add(entriesInNode);
                overview.put(elementName, temp);
            }
            if (nextEvent.isEndElement()) {
                xPathStack.pop();
            }
            if (nextEvent.isCharacters()) {
                String elementName = "";
                if (retrieve_XPath) {
                    elementName = Utils.stack2Path(xPathStack);
                }
                String cdata = nextEvent.asCharacters().getData();
                if (cdata.trim().length() > 0) {
                    ArrayList<HashMap<String, String>> temp = overview.getOrDefault(elementName, new ArrayList<>());
                    HashMap<String, String> entry = new HashMap<>();
//                    if (!temp.get(temp.size()-1).keySet().contains("text()")) {
//                        entry = temp.get(temp.size()-1);
//                        temp.remove(temp.size()-1);
//                        entry.put("text()", cdata.trim());
//                        temp.add(entry);
//                        overview.put(elementName, temp);
//                    }else {
                    entry.put("text()", cdata.trim());
                    temp.add(entry);
                    overview.put(elementName, temp);
//                    }
                }
            }
        }
        return overview;
    }

    /**
     * Converts an overview Map to a frequency Map over each attribute value in each node.
     * {root/s/chunk/w-text() :
     *   {
     *    very : 1,
     *    for : 2,
     *    Minister : 1
     *   }
     * }
     * @param overview
     * @return
     */
    public static HashMap<String, HashMap<String,Integer>> overview2AttributeFrequencies(HashMap<String, ArrayList<HashMap<String, String>>> overview) {
        HashMap<String, HashMap<String,Integer>> attributeFrequencies = new HashMap<>();
        for (String path : overview.keySet()) {
            for (HashMap<String, String> attributesOfNodes : overview.get(path)) {
                for (Map.Entry<String, String> entry : attributesOfNodes.entrySet()) {
                    String spacer = entry.getKey().equals("text()") ? "/" : "/@";
                    HashMap<String, Integer> temp = attributeFrequencies.getOrDefault(path+spacer+entry.getKey(), new HashMap<>());
                    temp.put(entry.getValue(),temp.getOrDefault(entry.getValue(), 0) + 1);
                    attributeFrequencies.put(path+spacer+entry.getKey(), temp);
                }
            }
        }
        return attributeFrequencies;
    }

    /**
     * Iterates over an overview and returns the counts of nodes with their path
     * @param overview
     */
    public static HashMap<String, Integer> overview2PathFrequencies(HashMap<String, ArrayList<HashMap<String, String>>> overview) {
        HashMap<String, Integer> pathFrequencies = new HashMap<>();
        for (String path : overview.keySet()){
            pathFrequencies.put(path, overview.get(path).size());
        }
        return pathFrequencies;
    }

    public static HashMap<String, ArrayList<String>> overview2pseudoCoNLL(HashMap<String, ArrayList<HashMap<String, String>>> overview) {
        HashMap<String, ArrayList<String>> path2Attributes = new HashMap<>();

        for (String path : overview.keySet()) {
            for (HashMap<String, String> attributesOfNodes : overview.get(path)) {
                for (Map.Entry<String, String> entry : attributesOfNodes.entrySet()) {
                    String spacer = entry.getKey().equals("text()") ? "/" : "/@";
                    ArrayList<String> temp = path2Attributes.getOrDefault(path + spacer + entry.getKey(), new ArrayList<>());
                    temp.add(entry.getValue());
                    path2Attributes.put(path + spacer + entry.getKey(), temp);
                }
            }
        }
        return path2Attributes;
    }
    public static void overview2SplitFrequencies (HashMap<String, ArrayList<HashMap<String, String>>> overview, HashMap<String, HashMap<String, Long>> xmlAttr2LitValue, HashMap<String, HashMap<String, Long>> xmlAttr2UrlValue) {
        for (String path : overview.keySet()) {
            for (HashMap<String, String> attributesOfNodes : overview.get(path)) {
                for (Map.Entry<String, String> entry : attributesOfNodes.entrySet()) {
                    String spacer = entry.getKey().equals("text()") ? "/" : "/@";
                    if (isURL(entry.getValue())) {
                        HashMap<String, Long> temp = xmlAttr2UrlValue.getOrDefault(path+spacer+entry.getKey(), new HashMap<>());
                        temp.put(entry.getValue(), temp.getOrDefault(entry.getValue(), 0L) + 1);
                        xmlAttr2UrlValue.put(path+spacer+entry.getKey(), temp);
                    } else {
                        HashMap<String, Long> temp = xmlAttr2LitValue.getOrDefault(path+spacer+entry.getKey(), new HashMap<>());
                        temp.put(entry.getValue(), temp.getOrDefault(entry.getValue(), 0L) + 1);
                        xmlAttr2LitValue.put(path+spacer+entry.getKey(), temp);
                    }
                }
            }
        }
    }

    public static String findLongestCommonXPath(ArrayList<String> xpaths) {
        ArrayList<String> longestCommonXPath = new ArrayList<>();
        // init matrix
        ArrayList<ArrayList<String>> splitXPaths = new ArrayList<>();
        int j_max = -1;
        for (String xpath : xpaths) {
            ArrayList<String> split = new ArrayList<>(Arrays.asList(xpath.split("/")));
            j_max = Math.max(split.size(), j_max);
            splitXPaths.add(split);
        }
        for (int j = 0; j < j_max; j++){
            String next = splitXPaths.get(0).get(j);
            for (int i = 0; i < splitXPaths.size(); i++) {
                if (!splitXPaths.get(i).get(j).equals(next)) {
                    return String.join("/", longestCommonXPath);
                }
            }
            longestCommonXPath.add(next);
        }
        return String.join("/", longestCommonXPath);

    }

    /**
     * Checks an xml File, if it has any node with a certain name.
     * @param xmlFile
     * @param node
     * @param n
     * @return
     */
    static public boolean hasNode (File xmlFile, String node, int n) {

    	XMLEventReader staxReader = null;
    	FileReader fileReader = null;
        try {
            fileReader = new FileReader(xmlFile);
            XMLInputFactory staxFactory = XMLInputFactory.newInstance();
            staxReader = staxFactory.createXMLEventReader(fileReader);

        int i = 0;
        while (i < n && staxReader.hasNext()){
            XMLEvent nextEvent = (XMLEvent) staxReader.next();
            if (nextEvent.isStartElement()) {

            }
            if (nextEvent.isStartElement() && nextEvent.asStartElement().getName().toString().equals(node)){
            	staxReader.close();
            	fileReader.close();
                return true;
            }
        }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
        	fileReader.close();
			staxReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
        return false; // we didn't find any or experienced an exception
    }

    /**
     * TODO: fix typo in function name
     * @param sample
     * @param threshold
     */
    static void reduceToTreshold(HashMap<String, HashMap<String, Long>> sample, int threshold) {
        for (String key : sample.keySet()) {
            HashMap<String, Long> temp = sample.get(key);
            Set<String> atts = new HashSet<>(temp.keySet());
            for (String att : atts) {
                if (temp.size() > threshold) {
                    temp.remove(att);
                } else {
                    break; // TODO: this is weird, why break and not continue?
                }
            }
            sample.put(key, temp);
        }
    }


    public void sample(File xmlFile, Set<String> atts, HashMap<String, HashMap<String, Long>> xmlAttr2LitValue, HashMap<String, HashMap<String, Long>> xmlAttr2UrlValue, int globalSampleSize) {
        try {
            overview2SplitFrequencies(collectOverview(cutSample(xmlFile, globalSampleSize), true), xmlAttr2LitValue, xmlAttr2UrlValue);
            Set<String> litAtts = new HashSet<>(xmlAttr2LitValue.keySet());
            for (String att : litAtts) {
                if (!atts.contains(att)) {
                    xmlAttr2LitValue.remove(att);
                }
            }
            Set<String> urlAtts = new HashSet<>(xmlAttr2LitValue.keySet());
            for (String att : urlAtts) {
                if (!atts.contains(att)) {
                    xmlAttr2UrlValue.remove(att);
                }
            }
        } catch (XMLStreamException e){
            System.err.println("Unable to read "+xmlFile.getAbsolutePath());

        }
    }


    public void sample(File xmlFile, int sampleSize, HashMap<String, HashMap<String, Long>> xmlAttr2LitValue, HashMap<String, HashMap<String, Long>> xmlAttr2UrlValue, int globalSampleSize) {
        try {
            overview2SplitFrequencies(collectOverview(cutSample(xmlFile, globalSampleSize), true), xmlAttr2LitValue, xmlAttr2UrlValue);
            reduceToTreshold(xmlAttr2LitValue, sampleSize);
            reduceToTreshold(xmlAttr2UrlValue, sampleSize);
        }
        catch (XMLStreamException e){
            System.err.println("Unable to read "+xmlFile.getAbsolutePath());

        }

    }

    public HashMap<String, Set<String>> collectAttributeKeySpace(HashMap<String, ArrayList<HashMap<String, String>>> overview, int threshold) {
        HashMap<String, Set<String>> attributeSpace = new HashMap<>();
        // TODO: this counting is stupid since it will lead to errors when pruning, think of something different!
        for (String path : overview.keySet()) {
            HashMap<String, Integer> counts = new HashMap<>();
            Set<String> temp = new HashSet<>();
            for (HashMap<String, String> nodeRepresentation : overview.get(path)) {
                for (String key : nodeRepresentation.keySet()) {
                    Integer count = counts.getOrDefault(key, 0);
                    count++;
                    counts.put(key, count);
                }
                temp.addAll(nodeRepresentation.keySet());
            }
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                if (entry.getValue() > threshold) {
                    temp.add(entry.getKey());
                }
                else {
                    LOGGER.info("Ignoring attribute");
                }
            }
            attributeSpace.put(path, temp);
        }
        LOGGER.info("Generated attribute space of "+attributeSpace.size()+" pseudo XPaths.");
        return attributeSpace;
    }

    public HashMap<String, ArrayList<HashMap<String, String>>> pruneIncompleteFromOverview(HashMap<String, ArrayList<HashMap<String, String>>> overview, HashMap<String, Set<String>> attributeSpace) {
        HashMap<String, ArrayList<HashMap<String, String>>> newOverview = new HashMap<>();
        int removedEntries = 0;
        for (String path : overview.keySet()) {
            ArrayList<HashMap<String, String>> temp = new ArrayList<>();
            for (HashMap<String, String> entry : overview.get(path)) {
                if (entry.keySet().equals(attributeSpace.get(path))) {
                    temp.add(entry);
                } else {
                    LOGGER.finer("SKIPPING, entry "+entry+" is missing something from attribute space ("+attributeSpace.get(path)+")");
                    removedEntries++;
                }
            }
            newOverview.put(path, temp);
        }
        LOGGER.info("Removed "+removedEntries+" entries from sample due to missing attribute values.");
        return newOverview;
    }

    public HashMap<String, ArrayList<HashMap<String, String>>> sampleOverview(File xmlFile, int n, boolean retrieveXPaths) throws XMLStreamException {
        return collectOverview(cutSample(xmlFile, n), retrieveXPaths);
    }
    public HashMap<String, ArrayList<String>> samplePseudoCoNLL (File xmlFile, int n, boolean retrieveXPaths) throws XMLStreamException {
        return overview2pseudoCoNLL(sampleOverview(xmlFile, n, retrieveXPaths));
    }
}
