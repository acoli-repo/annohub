package de.unifrankfurt.informatik.acoli.fid.xml;

public class XMLConverterFactory {

    enum ConversionMode {
        generic, template, both
    }

    private float ALL_CHECKS_THRESHOLD = 0.6f;
    private float SINGLE_CHECK_THRESHOLD = 0.8f;
    private boolean RETRIEVE_XPATH = false;
    private int SAMPLE_SIZE = 500;
    private ConversionMode mode = ConversionMode.both;
    private String templatePath;
    private boolean SPLIT_ON_XPATH = false;
    private int NUMBER_OF_SENTENCES_TO_OUTPUT = 10;

    void setTemplatePath(String templatePath) {
        // TODO: check if path exists?
        this.templatePath = templatePath;
    }

    XMLConverter newConverter() {
        XMLConverter configuredConverter = null;
        switch (mode) {
            case both:
            case generic:
            case template:
        }
        return configuredConverter;
    }
}
