package de.unifrankfurt.informatik.acoli.fid.resourceDB;

import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.UpdatePolicy;

import de.unifrankfurt.informatik.acoli.fid.xml.Template;
import de.unifrankfurt.informatik.acoli.fid.xml.TemplateMatcher;
import de.unifrankfurt.informatik.acoli.fid.xml.TemplateQuality;
import de.unifrankfurt.informatik.acoli.fid.xml.Utils;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.structure.Edge;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Used to interface the graph database to load, store and query templates.
 */
public class TemplateManager {
    private static ResourceManager resourceManager;
    private static Cluster cluster;
    private final static Logger LOGGER =
            Logger.getLogger(TemplateManager.class.getName());


    
    public TemplateManager(ResourceManager resourceManager_){
        resourceManager = resourceManager_;
    }
    
    public TemplateManager(UpdatePolicy updatePolicy){
        cluster = Cluster.open();
        resourceManager = new RMServer(cluster, updatePolicy);
    }

    
    /**
     * Receives a json file representing a list of templates.
     * TODO: Warn if there are doubled IDs or anything gets overwritten.
     */
    public void loadTemplatesToDatabase_(File templateFile){
     
        Template[] templates = Utils.readJSONTemplates(templateFile.getPath());
        for (Template template : templates){
            resourceManager.addXMLTemplate(template);
        }
        LOGGER.info("Loaded "+templates.length+" from disk.");
    }

    /**
     * Receives a json file representing a list of templates.
     * TODO: Warn if there are doubled IDs or anything gets overwritten.
     */
    public static void loadTemplatesToDatabase(File templateFile, UpdatePolicy updatePolicy){
        cluster = Cluster.open();
        resourceManager = new RMServer(cluster, updatePolicy);
        Template[] templates = Utils.readJSONTemplates(templateFile.getPath());
        for (Template template : templates){
            resourceManager.addXMLTemplate(template);
        }
        LOGGER.info("Loaded "+templates.length+" from disk.");
    }

    /**
     * connects to graph data base, retrieves templates and returns
     * a Template matcher containing all existing templates.
     * @return a fully configured TemplateMatcher object
     */
    public TemplateMatcher createTemplateMatcher(){
        ArrayList<Template> templates = resourceManager.getAllXMLTemplates();
        return new TemplateMatcher(templates);
    }

    public TemplateMatcher createTemplateMatcher(String templateId){
        return null;
    }


    public Edge writeMatchToGraphDB(TemplateQuality bestMatch, ResourceInfo resourceInfo) {
        return resourceManager.addXMLTemplateQualityEdge(resourceInfo, bestMatch);
	}

	public HashSet<String> getAllSentenceNames(){
        ArrayList<Template> templates = resourceManager.getAllXMLTemplates();
        return templates.stream()
                        .map(Template::getSentencePath)
                        .collect(Collectors.toCollection(HashSet::new));
    }
}
