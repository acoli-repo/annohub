package de.unifrankfurt.informatik.acoli.fid.xml;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class transforms an arbitrary XML Document into CoNLL based on a template that is presented when creating the
 * object.
 */
public class XML2CoNLL {

    private Template template;
    @Deprecated
    private SubtreeGenerator sg;
    private final static Logger LOGGER =
            Logger.getLogger(XML2CoNLL.class.getName());

    /**
     * the SubtreeGenerator represents the file. The Template tells the converter
     * how to analyze the xml file to produce CoNLL.
     * @param template
     */
    public XML2CoNLL(Template template){
        this.template = template;
        this.template.compile();
    }

    @Deprecated
    public XML2CoNLL(SubtreeGenerator sg, Template template) {
        this.sg = sg;
        this.template = template;
        this.template.compile();
    }

    public String transformXMLSentenceToCoNLLSentence(Document xmlSentence, String finalDelimiter) {
        ArrayList<CoNLLRow> conllSentence;
        try {
            conllSentence = consumeSentence(xmlSentence);
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (CoNLLRow word : conllSentence) {
            sb.append(word.toString());
            sb.append("\n");
        }
        sb.append(finalDelimiter);
        return sb.toString();
    }
    public String transformXMLSentenceToCoNLLSentence(Document xmlSentence) {
        return transformXMLSentenceToCoNLLSentence(xmlSentence, "\n");
    }

    /**
     * @param out
     * @param n How many sentences to convert
     * @return
     */
    @Deprecated
    public void transform(PrintStream out, int n) {
        ArrayList<Document> xmlSentences;

        xmlSentences = this.sg.getSamples(n);
        out.print(createCommentString(this.template)+"\n");
        LOGGER.info("Extracted "+xmlSentences.size()+" sentences.");

        for (Document xmlSentence : xmlSentences){
            try {
                ArrayList<CoNLLRow> sentence = consumeSentence(xmlSentence);
                for (CoNLLRow word : sentence) {
                    out.print(word.toString()+"\n");
                }
                out.print("\n");
                out.flush();


            } catch (XPathExpressionException e) {
                e.printStackTrace();
            }
        }
        out.close();

    }


    /**
     * expects one node representing a subtree and transforms it to CoNLL by using the TEMPLATE.
     * @param node
     * @return
     */
    public CoNLLRow transformToCoNLL(Node node, Integer i){
        CoNLLRow row = new CoNLLRow(i);
        if (this.template.columnXPaths != null) {
            this.template.columnXPaths.forEach((col, xpath) -> {
                try {
                    if (xpath == null){
                        row.getColumns().put(col, "FEATS");
                    } else {
                        String result = xpath.evaluate(node, XPathConstants.STRING).toString();
                        result = result == "" ? "_" : result;
                        row.getColumns().put(col, result);
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });
        }
        if (this.template.featureXPaths != null) {
            this.template.featureXPaths.forEach((feat, xpath) -> {
                try {
                    String result = xpath.evaluate(node, XPathConstants.STRING).toString();
                    result = result.equals("") ? "_" : result;
                    LOGGER.finest(result);
                    row.getFeats().put(feat, result);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });
        }
        LOGGER.finer("CONLL:"+row);
        return row;
    }

    /**
     * receives an entire subtree, split into words and chunks and calls functions to create
     * CoNLL rows out of them.
     * @param sentence
     * @return
     * @throws XPathExpressionException
     */
    public ArrayList<CoNLLRow> consumeSentence(Node sentence) throws XPathExpressionException{
        NodeList words = (NodeList) this.template.wordXPath.evaluate(sentence, XPathConstants.NODESET);
        ArrayList<CoNLLRow> sentenceRows = new ArrayList<>();
        LOGGER.fine("Handling a sentence with "+words.getLength()+" words.");
        for (int i = 0; i < words.getLength(); i++){
            sentenceRows.add(transformToCoNLL(words.item(i), i));
        }
        return sentenceRows;
    }

    /**
     * creates the comment string denoting column names based on a template.
     * @param template
     * @return
     */
    public String createCommentString(Template template){
        ArrayList<String> comment = new ArrayList<>();
        for (String column : template.columnPaths.keySet()){
            comment.add(column);
        }
        // in case we have features we replace the comment section.
        if (template.featurePaths != null) {
            ArrayList<String> feats = new ArrayList<>();
            feats.addAll(template.featurePaths.keySet());

            if (comment.contains("FEATS")){ // replaces the keyword with the actual featurenames
                comment.set(comment.indexOf("FEATS"), String.join("|",feats));
            }
            else{
                comment.add(String.join("|", feats));
            }
        }
        // result
        return "#"+String.join("\t", comment);
    }
}
