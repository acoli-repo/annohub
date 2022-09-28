package de.unifrankfurt.informatik.acoli.fid.xml;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class GenericXMLConverter {
    private XMLSampler xs;
    private boolean retrieveXPaths;
    private boolean simplifyCommentStrings = true;

    private final static Logger LOGGER =
            Logger.getLogger(GenericXMLConverter.class.getName());

    public GenericXMLConverter(XMLSampler xmlSampler) {
        this.xs = xmlSampler;
        this.retrieveXPaths = this.xs.getRetrieveXPaths();
    }
    public GenericXMLConverter(boolean xPaths, boolean simplify) {
        this.retrieveXPaths = xPaths;
        this.simplifyCommentStrings = simplify;
        this.xs = new XMLSampler(this.retrieveXPaths);
    }
    public GenericXMLConverter(boolean retrieveXPaths) {
        this.retrieveXPaths = retrieveXPaths;// TODO: MAKE CONSISTENT
        this.xs = new XMLSampler(this.retrieveXPaths);// TODO: MAKE CONSISTENT
    }

    public void getFullPseudoCoNLL(File sourceFile, PrintStream outStream) throws XMLStreamException {
        int length = XMLSampler.getNumberOfStartElementsInEntireFile(sourceFile);
        getPseudoCoNLLOfSizeK(sourceFile, outStream, length);
    }
    public void getFullPseudoCoNLL(File sourceFile, File targetFile) throws FileNotFoundException, XMLStreamException {
        LOGGER.info("xml ("+sourceFile.getAbsolutePath()+") -> pseudo-conll ("+targetFile.getAbsolutePath()+")");
        PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(targetFile)));
        getFullPseudoCoNLL(sourceFile, out);
    }
    public void getFullPseudoCoNLL(File sourceFile, PrintStream outStream, ArrayList<String> allowedAttributes) throws XMLStreamException {
        int length = XMLSampler.getNumberOfStartElementsInEntireFile(sourceFile);
        getPseudoCoNLLOfSizeK(sourceFile, outStream, length, allowedAttributes);
    }
    public void getFullPseudoCoNLL(File sourceFile, File targetFile, ArrayList<String> allowedAttributes) throws FileNotFoundException, XMLStreamException {
        PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(targetFile)));
        getFullPseudoCoNLL(sourceFile, out, allowedAttributes);
    }
    public void getPseudoCoNLLOfSizeK(File sourceFile, PrintStream outStream, int k) throws XMLStreamException {
        XMLSampler xs = new XMLSampler();
        HashMap<String, ArrayList<String>> pseudoCoNLL = xs.samplePseudoCoNLL(sourceFile, k, this.retrieveXPaths);// TODO: MAKE CONSISTENT
        writePseudoCoNLLToStream(pseudoCoNLL, outStream);
    }
    public void getPseudoCoNLLOfSizeK(File sourceFile, File targetFile, int k) throws XMLStreamException, FileNotFoundException {
        PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(targetFile)));
        getPseudoCoNLLOfSizeK(sourceFile, out, k);
    }
    public void getPseudoCoNLLOfSizeK(File sourceFile, PrintStream outStream, int k, ArrayList<String> allowedAttributes) throws XMLStreamException {
        HashMap<String, ArrayList<String>> pseudoCoNLL = xs.samplePseudoCoNLL(sourceFile, k, this.retrieveXPaths); // TODO: MAKE CONSISTENT
        HashMap<String, ArrayList<String>> prunedPseudoCoNLL = prunePseudoCoNLLNotContainingAllowedPaths(pseudoCoNLL, allowedAttributes);
        writePseudoCoNLLToStream(prunedPseudoCoNLL, outStream);
    }
    public void getPseudoCoNLLOfSizeK(File sourceFile, File targetFile, int k, ArrayList<String> allowedAttributes) throws FileNotFoundException, XMLStreamException {
        PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(targetFile)));
        getPseudoCoNLLOfSizeK(sourceFile, out, k, allowedAttributes);
    }

    @Deprecated
    public boolean convertToStream(File sourceFile, PrintStream outStream) throws FileNotFoundException {
        HashMap<String, ArrayList<String>> pseudoCoNLL;
        try {
            this.xs.setRetrieveXPaths(true);
            pseudoCoNLL = this.xs.samplePseudoCoNLL(sourceFile, 500, this.retrieveXPaths);
        } catch (XMLStreamException e) {
            LOGGER.severe("Could not create overview from file.");
            return false;
        }
        return writePseudoCoNLLToStream(pseudoCoNLL, outStream);
    }

    @Deprecated
    public boolean convertToFile(File sourceFile, File outFile) throws FileNotFoundException {
        return convertToStream(sourceFile, new PrintStream(new BufferedOutputStream(new FileOutputStream(outFile))));
    }

    /**
     * Simplifies an array of pseudoXPaths. Especially useful, if there is a lot of unnecessary clutter
     * in the beginning of each path that doesn't add any semantics. The original path can be retrieved by concatenating
     * the dedicated #path row with each of the column name comments.
     * @param allPseudoXPaths the list of column names to simplify.
     * @return the simplified CoNLL comment string.
     */
    public String createSimplifiedCommentString(ArrayList<String> allPseudoXPaths) {
        String longestCommonXPath = XMLSampler.findLongestCommonXPath(allPseudoXPaths);
        StringBuilder commentString = new StringBuilder();
        commentString.append("#Path ").append(longestCommonXPath).append("\n");
        commentString.append("#ID");
        for (String pseudoXPath: allPseudoXPaths) {
            commentString.append("\t");
            commentString.append(pseudoXPath.replaceFirst(Pattern.quote(longestCommonXPath),""));
        }
        commentString.append("\n");
        return commentString.toString();
    }

    /**
     * Prunes a given pseudoCoNLL representation of all columns that are not contained in the allowed attributes,
     * represented by a HashSet.
     * TODO: shouldn't this go into the sampler?
     * @param pseudoCoNLL a column-wise representation of a pseudo-CoNLL.
     * @param allowedAttributes the list of allowed pseudo-CoNLL column names.
     * @return the pruned pseudoCoNLL.
     */
    HashMap<String, ArrayList<String>> prunePseudoCoNLLNotContainingAllowedPaths(HashMap<String, ArrayList<String>> pseudoCoNLL, ArrayList<String> allowedAttributes) {
        HashMap<String, ArrayList<String>> prunedPseudoCoNLL = new HashMap<>();
        // TODO: somehow report on how much got pruned
//        float ratioOfRemovedRows = (float) (maxColumnLength - minColumnLength) / (float) maxColumnLength;
//        LOGGER.info("Pruning "+(maxColumnLength-minColumnLength)+" incomplete rows of "+maxColumnLength+" rows total. ("+String.format("%.3g", ratioOfRemovedRows*100)+"%)");
//        for (Map.Entry<String,ArrayList<String>> entry : pseudoCoNLL.entrySet()) {
//            maxColumnLength = Math.max(entry.getValue().size(), maxColumnLength);
//            minColumnLength = Math.min(entry.getValue().size(), minColumnLength);
//        }
        for (Map.Entry<String, ArrayList<String>> pseudoColumn : pseudoCoNLL.entrySet()) {
            if (allowedAttributes.contains(pseudoColumn.getKey())) {
                prunedPseudoCoNLL.put(pseudoColumn.getKey(), pseudoColumn.getValue());
            } else {
                LOGGER.finer("Removing column: "+pseudoColumn.getKey());
            }
        }
        int pseudoCoNLLColumnCount = pseudoCoNLL.size();
        int prunedPseudoCoNLLColumnCount = prunedPseudoCoNLL.size();
        int sizeDiff = pseudoCoNLLColumnCount-prunedPseudoCoNLLColumnCount;
        LOGGER.fine("Removed "+sizeDiff+"/"+pseudoCoNLLColumnCount+" of columns ("+String.format("%.3g", (float) sizeDiff / (float)pseudoCoNLLColumnCount*100)+"%) from pseudo-CoNLL.");
        return prunedPseudoCoNLL;
    }

    boolean writePseudoCoNLLToStream(HashMap<String, ArrayList<String>> pseudoCoNLL, PrintStream outStream) {
        if (this.simplifyCommentStrings) {
            outStream.print(createSimplifiedCommentString(new ArrayList<>(pseudoCoNLL.keySet())));
        } else {
            outStream.print(String.join("\t",new ArrayList<>(pseudoCoNLL.keySet())));
        }
        if (pseudoCoNLL.size() == 0) {
            LOGGER.warning("Pseudo CoNLL to write is empty.");
            return false;
        }
        StringBuilder sb = new StringBuilder();
        int maxColumnLength = 0;
        int minColumnLength = Integer.MAX_VALUE;
        // Figure out the maximum number of lines for the conll sample,
        // but only for columns that are allowed.
        for (Map.Entry<String,ArrayList<String>> entry : pseudoCoNLL.entrySet()) {
            maxColumnLength = Math.max(entry.getValue().size(), maxColumnLength);
            minColumnLength = Math.min(entry.getValue().size(), minColumnLength);
        }
        LOGGER.info("Writing "+maxColumnLength+" lines of CoNLL");
        for (int i = 0; i < maxColumnLength; i++) {
            sb.append(i);
            sb.append("\t");
            for (String j : pseudoCoNLL.keySet()) {
                if (i < pseudoCoNLL.get(j).size()) {
                    sb.append(pseudoCoNLL.get(j).get(i));
                } else {
                    sb.append("_");
                }
                sb.append("\t");
            }
            sb.append("\n");
            outStream.print(sb.toString());
            sb = new StringBuilder();

        }
        outStream.flush();
//        outStream.close();
        return true;
    }

    @Deprecated
    public boolean covertToFileButOnlyWithAllowedAttributes(File sourceFile, PrintStream outStream, HashSet<String> allowedAttributes) {
        HashMap<String, ArrayList<String>> pseudoCoNLL;
        ArrayList<String> allowedAttributeList = new ArrayList<>(allowedAttributes);

        try {
            this.xs.setRetrieveXPaths(true);
            pseudoCoNLL = this.xs.samplePseudoCoNLL(sourceFile, 500, this.retrieveXPaths);
        } catch (XMLStreamException e) {
            LOGGER.severe("Could not create overview from file.");
            return false;
        }
        outStream.print(createSimplifiedCommentString(allowedAttributeList));
        HashMap<String, ArrayList<String>> prunedPseudoCoNLL = prunePseudoCoNLLNotContainingAllowedPaths(pseudoCoNLL, allowedAttributeList);

        return writePseudoCoNLLToStream(prunedPseudoCoNLL, outStream);
    }

}