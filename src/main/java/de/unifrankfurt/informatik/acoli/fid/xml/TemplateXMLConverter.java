package de.unifrankfurt.informatik.acoli.fid.xml;

import org.w3c.dom.Document;

import de.unifrankfurt.informatik.acoli.fid.exec.Executer;

import javax.xml.stream.XMLStreamException;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TemplateXMLConverter {

    private ArrayList<Template> templates;
    private TemplateMatcher tm;
    @Deprecated
    private int n;
    private int k; // TODO: set default

    private final static Logger LOGGER =
            Logger.getLogger(TemplateXMLConverter.class.getName());
    static private String SYNOPSIS = "synopsis: XMLConverter -f IN_FILE -t TEMPLATE_PATH [-o OUT_FILE] [-l LENGTH] [-s SAMPLE_SIZE] [--silent]\n"+
            "\tIN_FILE       XML file to convert\n"+
            "\tTEMPLATE_PATH path to template json\n"+
            "\tOUT_FILE      default std out, where to write converted conll\n"+
            "\tLENGTH        default 999, how many sentences to convert\n"+
            "\tSAMPLE_SIZE   default 500, How many nodes to sample\n"+ // TODO: This is false
            "\t--silent      no logging output (also not this synopsis!)\n";


    @Deprecated
    public TemplateXMLConverter(){

    } // TODO: remove this one!
    public TemplateXMLConverter(String templatePath) {
        this.templates = new ArrayList<>(Arrays.asList(Utils.readJSONTemplates(templatePath)));
        this.tm = new TemplateMatcher(this.templates);
    }
    public TemplateXMLConverter(ArrayList<Template> templates) {
        this.templates = templates;
        this.tm = new TemplateMatcher(this.templates);
    }
    public TemplateXMLConverter(TemplateMatcher templateMatcher) {
        this.tm = templateMatcher;
        this.templates = templateMatcher.templates;
    }

    public TemplateXMLConverter(String templatePath, int n, int k) {
        this.templates = new ArrayList<>(Arrays.asList(Utils.readJSONTemplates(templatePath)));
        this.n = n;
        LOGGER.warning("Using TemplateXMLConverter with deprecated argument n!");
        this.k = k;
    }

    // TODO: Implement
    public void getSampleOfSizeKSentencesAsCoNLL(File sourceFile, PrintStream outStream, int k) throws FileNotFoundException {
        String sentenceName = findAppropriateSentenceNameInFileFromTemplates(sourceFile, this.templates);
        SubtreeGenerator sg = new SubtreeGenerator(sentenceName, sourceFile);

        TemplateQuality bestMatch = this.tm.getBestTemplateQuality(sg.getSamples(42));
        this.getSampleOfSizeKSentencesAsCoNLL(sourceFile, outStream, k, bestMatch.getTemplate());

    }
    public void getSampleOfSizeKSentencesAsCoNLL(File sourceFile, PrintStream outStream, int k, Template template) throws FileNotFoundException {
        SubtreeGenerator sg = new SubtreeGenerator(template.getSentencePath(), sourceFile);
        getSampleOfSizeKSentencesAsCoNLL(sg, outStream, k, template);
    }
    public void getSampleOfSizeKSentencesAsCoNLL(SubtreeGenerator sg, PrintStream outStream, int k, Template template) {
        ArrayList<Document> sample = sg.getSamples(k);
        XML2CoNLL x2c = new XML2CoNLL(template);
        
        de.unifrankfurt.informatik.acoli.fid.util.Utils.debug("getSampleOfSizeKSentencesAsCoNLL "+Thread.currentThread().getId());
        int y = 0;
        for (Document document : sample) {
        	
        	de.unifrankfurt.informatik.acoli.fid.util.Utils.debug(y++);
        	if(Executer.isInterrupted() || Thread.interrupted()) {
        		Executer.setInterrupted(true);
        		de.unifrankfurt.informatik.acoli.fid.util.Utils.debug("interrupted");
        		break;
        	}
            outStream.print(x2c.transformXMLSentenceToCoNLLSentence(document));
        }
        outStream.flush();
    }
    public void getFirstKSentencesAsCoNLL(File sourceFile, PrintStream outStream, int k) throws FileNotFoundException {
        String sentenceName = findAppropriateSentenceNameInFileFromTemplates(sourceFile, this.templates);
        if (sentenceName == null) { // TODO: Move this somewhere else
            LOGGER.info("Could not find matching sentence name in templates for file "+sourceFile.getAbsolutePath() + " with sample size "+this.k+".");
        }
        SubtreeGenerator sg = new SubtreeGenerator(sentenceName, sourceFile);

        TemplateQuality bestMatch = this.tm.getBestTemplateQuality(sg.getSamples(42));
        this.getFirstKSentencesAsCoNLL(sourceFile, outStream, k, bestMatch.getTemplate());
    }
    public void getFirstKSentencesAsCoNLL(File sourceFile, PrintStream outStream, int k, Template template) throws FileNotFoundException {
        SubtreeGenerator sg = new SubtreeGenerator(template.getSentencePath(), sourceFile);
        XML2CoNLL x2c = new XML2CoNLL(template);
        for (int i = 0; i<k; i++) {
            if (sg.hasNext()) {
                outStream.print(x2c.transformXMLSentenceToCoNLLSentence(sg.next()));
            }
        }
        outStream.flush();
    }
    // TODO: Implement
    public void getSampleByIndices(File sourceFile, PrintStream outStream, ArrayList<Integer> indices, Template template) {}
    public void getFullCoNLL(File sourceFile, PrintStream outStream, Template template, boolean newlineBetweenSentence) throws FileNotFoundException {
        SubtreeGenerator sg = new SubtreeGenerator(template.getSentencePath(), sourceFile);
        String delimiter = newlineBetweenSentence? "\n" : "";
        XML2CoNLL x2c = new XML2CoNLL(template);
        int progress = 0;
        de.unifrankfurt.informatik.acoli.fid.util.Utils.debug("Starting XML2CONLL conversion..");
        int y=0;
        while (sg.hasNext()) {
        	de.unifrankfurt.informatik.acoli.fid.util.Utils.debug(y++);
        	if(Executer.isInterrupted() || Thread.interrupted()) {
        		Executer.setInterrupted(true);
        		de.unifrankfurt.informatik.acoli.fid.util.Utils.debug("interrupted");
      	      	break;
        	}
            outStream.print(x2c.transformXMLSentenceToCoNLLSentence(sg.next(), delimiter));
            progress++;
            if (progress % 100 == 0) {
                LOGGER.info(progress+"/"+sg.getDocumentLength()+" subtrees processed.");
            }
        }
        outStream.flush();
    }
    public void getFullCoNLL(File sourceFile, PrintStream outStream, Template template) throws FileNotFoundException {
        getFullCoNLL(sourceFile, outStream, template, true);
    }
    public boolean getFullCoNLL(File sourceFile, PrintStream outStream) throws FileNotFoundException {
        String sentenceName = findAppropriateSentenceNameInFileFromTemplates(sourceFile, this.templates);
        if (sentenceName == null) {
            LOGGER.info("Could not find matching sentence name in templates for file "+sourceFile.getAbsolutePath() + " with sample size "+this.k+".");
            return false;
        }
        SubtreeGenerator sg = new SubtreeGenerator(sentenceName, sourceFile);

        TemplateQuality bestMatch = this.tm.getBestTemplateQuality(sg.getSamples(42));
        getFullCoNLL(sourceFile, outStream, bestMatch.getTemplate());
        return true;
    }

    private String findAppropriateSentenceNameInFileFromTemplates(File sourceFile, ArrayList<Template> templates) {
        HashSet<String> sentenceNameCandidates = templates.stream()
                .map(Template::getSentencePath)
                .collect(Collectors.toCollection(HashSet::new));

        String sentenceName = null;

        for (String sentenceNameCandidate : sentenceNameCandidates) {
            if (XMLSampler.hasNode(sourceFile, sentenceNameCandidate, 500)) { // TODO: parameterize
                sentenceName = sentenceNameCandidate;
            }
        }
        return sentenceName;
    }

    @Deprecated
    public boolean convertToStream(File sourceFile, PrintStream outStream) throws FileNotFoundException {
        // get all possible sentence boarders
        HashSet<String> sentenceNameCandidates = this.templates.stream()
                .map(Template::getSentencePath)
                .collect(Collectors.toCollection(HashSet::new));

        String sentenceName = null;

        for (String sentenceNameCandidate : sentenceNameCandidates) {
            if (XMLSampler.hasNode(sourceFile, sentenceNameCandidate, this.k)) {
                sentenceName = sentenceNameCandidate;
            }
        }
        if (sentenceName == null) {
            LOGGER.info("Could not find matching sentence name in templates for file "+sourceFile.getAbsolutePath() + " with sample size "+this.k+".");
            return false;
        }

        SubtreeGenerator sg = new SubtreeGenerator(sentenceName, sourceFile);
        ArrayList<Document> sample = sg.getSamples(this.k);

        TemplateMatcher tm = new TemplateMatcher(this.templates);
        TemplateQuality bestMatch = tm.getBestTemplateQuality(sample);

        TemplateXMLConverter xc = new TemplateXMLConverter();
        xc.parse(sg, outStream, this.n, bestMatch.getTemplate());
        return true;
    }

    @Deprecated
    public PrintStream convertToStream(File sourceFile) {
        LOGGER.severe("NOT IMPLEMENTED YET.");
        return null;
    }

    @Deprecated
    public boolean convertToFile(File sourceFile, File outFile) {
        try {
            return convertToStream(sourceFile, new PrintStream(outFile));
        } catch (FileNotFoundException e) {
            LOGGER.severe("Cannot not write to output file "+outFile.getAbsolutePath());
            // Shouldn't happen. (Probably will tho) TODO errors
            return false;
        }
    }

    @Deprecated
    public File convertToFile(File sourceFile) {
        LOGGER.severe("NOT IMPLEMENTED YET.");
        return null;
    }

    /**
     * Receives an xml file, and the name of the sentence nodes. Will then load
     * all templates and convert.
     * @param sg
     * @param out
     * @param n
     * @param template
     */
    @Deprecated
    public void parse(SubtreeGenerator sg, PrintStream out, int n, Template template) {
        XML2CoNLL xmlParser = new XML2CoNLL(sg, template);
        xmlParser.transform(out, n);
        // Figure out a fitting one
    }

    @Deprecated
    public void parse(File xmlFile, String outPath, int n, String templatePath, int k) throws IOException, XMLStreamException {

        // first, load the templates from disk
        ArrayList<Template> templates = new ArrayList<>(Arrays.asList(Utils.readJSONTemplates(templatePath)));
        // get all possible sentence boarders
        HashSet<String> sentenceNameCandidates = templates.stream()
                .map(Template::getSentencePath)
                .collect(Collectors.toCollection(HashSet::new));

        String sentenceName = null;

        for (String sentenceNameCandidate : sentenceNameCandidates) {
            if (XMLSampler.hasNode(xmlFile, sentenceNameCandidate, k)) {
                sentenceName = sentenceNameCandidate;
            }
        }
        if (sentenceName == null) {
            LOGGER.severe("No Template found with fitting sentenceName for file " + xmlFile);
            System.exit(1);
        }

        SubtreeGenerator sg = new SubtreeGenerator(sentenceName, xmlFile);
        ArrayList<Document> sample = sg.getSamples(k);

        TemplateMatcher tm = new TemplateMatcher(templates);
        TemplateQuality bestMatch = tm.getBestTemplateQuality(sample);

        PrintStream out = outPath == null ? System.out : new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(outPath))));

        TemplateXMLConverter xc = new TemplateXMLConverter();
        xc.parse(sg, out, n, bestMatch.getTemplate());

    }

    public static void main(String[] args) {
        String filePath = null;
        String templatePath = null;
        String outPath = null;
        int n = 999;
        int k = 10;
        // First, read in cmd line args
        for (int i = 0; i<args.length; i++) {
            switch (args[i]) {
                case "-f":
                    i++;
                    filePath = args[i];
                    break;
                case "-t":
                    i++;
                    templatePath = args[i];
                    break;
                case "-o":
                    i++;
                    outPath = args[i];
                    break;
                case "-l":
                    i++;
                    n = Integer.parseInt(args[i]);
                    break;
                case "-s":
                    i++;
                    k = Integer.parseInt(args[i]);
                    break;
                case "--silent":
                    LOGGER.setLevel(Level.OFF);
                    break;
                default:
                    break;
            }
        }

        LOGGER.info(SYNOPSIS);
        if (filePath == null){
            LOGGER.severe("NO FILE PROVIDED.");
            System.exit(1);
        }
        File xmlFile = new File(filePath);
        if (! xmlFile.exists()) {
            LOGGER.severe("File Not Found: " + filePath);
            System.exit(1);
        }
        if (templatePath == null){
            LOGGER.severe("NO TEMPLATES PROVIDED.");
            System.exit(1);
        }
        if (! new File(templatePath).exists()) {
            LOGGER.severe("File Not Found: " + templatePath);
            System.exit(1);
        }

        try {
            TemplateXMLConverter xc = new TemplateXMLConverter();
            xc.parse(xmlFile, outPath, n, templatePath, k);
        } catch (XMLStreamException e ) {
            LOGGER.severe("Couldn't parse XML file at"+filePath);
            e.printStackTrace();
            System.exit(1);
        } catch (FileNotFoundException e) {
            LOGGER.severe(filePath + "doesn't exist.");
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}
