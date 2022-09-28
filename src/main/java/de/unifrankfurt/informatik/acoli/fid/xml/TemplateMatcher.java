package de.unifrankfurt.informatik.acoli.fid.xml;

import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * TemplateMatcher class maps a sample of subtrees to their ideal or most viable templates
 * to extraction. This requires a list of templates described in json to be found. These
 * then each get evaluated on each sample tree to figure out how well precision, recall and 
 * accuracy perform.
 * TODO: make this class static to (probably) improve performance.
 * TODO: make matching selective on sentence node
 * @author lglaser
 *
 */
public class TemplateMatcher {
	ArrayList<Template> templates;
	ArrayList<Document> subtrees;
	TreeSet<TemplateQuality> matches;
	private final static Logger LOGGER =
			Logger.getLogger(TemplateMatcher.class.getName());
	
	public TemplateMatcher(ArrayList<Template> templates, ArrayList<Document> subtrees){
		this.templates = templates;
		this.subtrees = subtrees;
		this.matches = new TreeSet<>();
	}
	
	public TemplateMatcher(String path, ArrayList<Document> subtrees) {
		this.templates = new ArrayList<>(Arrays.asList(Utils.readJSONTemplates(path)));
		this.subtrees = subtrees;
		LOGGER.info("Created matcher with "+subtrees.size()+" subtrees and "+this.templates.size()+" templates.");
		this.matches = new TreeSet<>();
	}

	public TemplateMatcher(ArrayList<Template> templates){
		// TODO: Error Management for null subtress etc.?
		this.templates = templates;
	}
	
	/**
	 * TODO: IMPLEMENT
	 * @param path
	 * @param subtrees
	 * @param qualityCriterion
	 */
	public TemplateMatcher(String path, ArrayList<Document> subtrees, String qualityCriterion) {
		this.templates = new ArrayList<>(Arrays.asList(Utils.readJSONTemplates(path)));
		this.subtrees = subtrees;
		LOGGER.info("Created matcher with "+subtrees.size()+" subtrees and "+this.templates.size()+" templates.");
		this.matches = new TreeSet<>();
	}
	
	public void calculateQualities(){
		TreeSet<TemplateQuality> matches = new TreeSet<>();
		for (Template template : templates){
			matches.add(new TemplateQuality(template, this.subtrees));
		}
		this.matches = matches;
	}

	public TemplateQuality getTemplateQuality(int i){
		return (TemplateQuality) this.matches.toArray()[this.matches.size()-(i+1)];
	}
	public TemplateQuality getBestTemplateQuality(ArrayList<Document> subtrees){
		this.subtrees = subtrees;
		calculateQualities();
		TemplateQuality bestMatch = matches.last();
		//LOGGER.info("Best match was: \n"+bestMatch);
		return bestMatch;
	}
	public Template getBestTemplate(){
		if (matches.size() == 0){
			calculateQualities();
		}

		TemplateQuality bestMatch = matches.last();
		//LOGGER.info("Best match was: \n"+bestMatch);
		return bestMatch.getTemplate();
	}


}
