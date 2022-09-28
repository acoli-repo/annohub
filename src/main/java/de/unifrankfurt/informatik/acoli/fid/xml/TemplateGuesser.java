package de.unifrankfurt.informatik.acoli.fid.xml;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TemplateGuesser {

    private final static Logger LOGGER =
            Logger.getLogger(TemplateGuesser.class.getName());
    /**
     * Finds the largest common xpath with the wordPath. We assume this to be the sentencePath.
     * @param stringPaths all xPaths
     * @param wordPath the path to compare to
     * @return the largest common xPath.
     */
    public static String guessSentencePath(Set<String> stringPaths, String wordPath) {
        ArrayList<ArrayList<String>> xPaths = new ArrayList<>();
        for (String path : stringPaths) {
            xPaths.add(new ArrayList<>(Arrays.asList(path.split("/"))));
        }

        // Find the longest path of all from where to go
        ArrayList<String> wordXPath = new ArrayList<>(Arrays.asList(wordPath.split("/")));
        ArrayList<String> longestCommonPath = new ArrayList<>();
        for (String pathElement : wordXPath) {
            for (ArrayList<String> xPath : xPaths) {
                if (! xPath.contains(pathElement)) { // if an element is missing we found the largest common path
                    return String.join("/", longestCommonPath);
                }
            }
            longestCommonPath.add(pathElement);
        }
        String sentencePathCandidate = String.join("/", longestCommonPath);
        if (sentencePathCandidate.equals(wordPath)) {
            LOGGER.info("Candidate for sentence path equals word Path, tracking back to parent..");
            sentencePathCandidate = String.join("/",longestCommonPath.subList(0, longestCommonPath.size()-1));
        }
        return sentencePathCandidate;
    }


    public static Template guessTemplateFromAttributeFrequencies(HashMap<String, HashMap<String,Integer>> attributeFrequencies) {
        if (attributeFrequencies.size() == 0) {
            LOGGER.warning("Can't guess Template from empty attribute frequencies.");
            return null;
        }
        // figure out which node represents the word unit
        // We assume, that the node that occurs most often is the word.

        HashMap<String, Integer> pathFrequencies = XMLSampler.attributeFrequency2PathFrequency(attributeFrequencies);
        System.out.println(attributeFrequencies);
        System.out.println(pathFrequencies);
        Template guessedTemplate;
        String wordPath = "";
        Integer max = 0;
        for (Map.Entry<String, Integer> e : pathFrequencies.entrySet()) {
            if (e.getValue() > max) {
                wordPath = e.getKey();
                max = e.getValue();
            }
        }
        // look for a sentencePath, excluding single occurrences paths (most likely root)
        String sentencePath = guessSentencePath(pathFrequencies.entrySet()
                .stream()
                .filter(e -> e.getValue()>1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet()), wordPath);
        System.out.println(sentencePath);
        System.out.println(wordPath);
        HashSet<String> cols = new HashSet<>();

        wordPath = wordPath.replace(sentencePath,"");

        for (Map.Entry<String,HashMap<String,Integer>> entry : attributeFrequencies.entrySet()) {
            String pathSuffixFromWordPathDownwards = entry.getKey().replaceFirst(".*"+Pattern.quote(sentencePath)+Pattern.quote(wordPath),"");
            System.out.println(pathSuffixFromWordPathDownwards);
            cols.add(pathSuffixFromWordPathDownwards);
        }


        HashMap<String, String> templateColumns = new HashMap<>();
        for (String col : cols) {
            String removedLeadingSlash = col.startsWith("/") ? col.replaceFirst(Pattern.quote("/"),"") : col;
            String prependAtIfNeccessary;
            if (removedLeadingSlash.endsWith("text()") || removedLeadingSlash.contains("@"))
                prependAtIfNeccessary = removedLeadingSlash;
            else {
                String[] temp = removedLeadingSlash.split("/");
                temp[-1] = "@"+temp[-1];
                prependAtIfNeccessary = String.join("/", temp);

            }
            templateColumns.put(col, prependAtIfNeccessary);
        }

        guessedTemplate = new Template(sentencePath, "/"+wordPath, templateColumns);
        return guessedTemplate;
    }
    public static Template guessTemplate(HashMap<String, ArrayList<HashMap<String, String>>> overview) {
        if (overview.size() == 0) {
            LOGGER.warning("Can't guess Template from empty overview.");
            return null;
        }
        // figure out which node represents the word unit
        // We assume, that the node that occurs most often is the word.

        HashMap<String, Integer> pathFrequencies = XMLSampler.overview2PathFrequencies(overview);
        Template guessedTemplate;
        String wordPath = "";
        Integer max = 0;
        for (Map.Entry<String, Integer> e : pathFrequencies.entrySet()) {
            if (e.getValue() > max) {
                wordPath = e.getKey();
                max = e.getValue();
            }
        }
        // look for a sentencePath, excluding single occurrences paths (most likely root)
        String sentencePath = guessSentencePath(pathFrequencies.entrySet()
                .stream()
                .filter(e -> e.getValue()>1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet()), wordPath);
        HashSet<String> cols = new HashSet<>();
        for (HashMap<String, String> attributes : overview.get(wordPath)) {
            cols.addAll(attributes.keySet());
        }

        wordPath = wordPath.replace(sentencePath,"");

        HashMap<String, String> templateColumns = new HashMap<>();
        for (String col : cols) {
            templateColumns.put(col, "@"+col);
        }

        guessedTemplate = new Template(sentencePath, "/"+wordPath, templateColumns);
        return guessedTemplate;
    }
    /**
     * Template guessing function. Receives a xml File and creates a basic Template based on a few heuristics.
     * @param xmlFile an xml File for which the Template should be created
     * @param sampleSize how many start elements should be sampled for guessing. If 0 or below 0, samples the entire file.
     * @return a Template object.
     */
    public static Template guessTemplate(File xmlFile, int sampleSize) throws XMLStreamException {
        if (sampleSize <= 0) {
            sampleSize = XMLSampler.getNumberOfStartElementsInEntireFile(xmlFile);
            LOGGER.info("Sample size <= 0. Will sample entire file of length "+sampleSize);
        }
        HashMap<String, ArrayList<HashMap<String, String>>> overview = XMLSampler.collectOverview(XMLSampler.cutSample(xmlFile, sampleSize), true);
        return guessTemplate(overview);
    }
    /**
     * Template guessing function. Receives a xml File and creates a basic Template based on a few heuristics.
     * @param xmlFile an xml File for which the Template should be created. Will sample the entire file.
     * @return a Template object.
     */
    public static Template guessTemplate(File xmlFile) throws XMLStreamException {
        return guessTemplate(xmlFile, XMLSampler.getNumberOfStartElementsInEntireFile(xmlFile));
    }
}
