package de.unifrankfurt.informatik.acoli.fid.xml;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.*;

public class TemplateQuality implements Comparable<TemplateQuality>{

	private Template template;
	private HashMap<String, Float> quality;

	public Template getTemplate() {
		return template;
	}

	@Override
	public String toString(){
		StringBuilder out = new StringBuilder();
		out.append("TemplateQuality with template \n");
		out.append(template);
		for (String key : quality.keySet()){
			out.append("\t");
			out.append(key);
			out.append(": ");
			out.append(quality.get(key));
			out.append("\n");
		}
		return out.toString();
		
	}
	
	TemplateQuality(Template template, ArrayList<Document> documents){
		this.template = template;
		Document[] removeA = {null};
		ArrayList<Document> remove = new ArrayList<>(Arrays.asList(removeA));
		documents.removeAll(remove);
		this.quality = calculateQuality(documents);
	}
	
	/**
	 * @return the quality
	 */
	public HashMap<String, Float> getQuality() {
		return quality;
	}

	/**
	 * @param quality the quality to set
	 */
	void setQuality(HashMap<String, Float> quality) {
		this.quality = quality;
	}


	@Override
	public int compareTo(TemplateQuality o) {
		HashMap<String, Float> other = o.getQuality();
		return Comparator.comparingDouble((HashMap<String, Float> q) -> q.get("recall"))
				.thenComparingDouble((HashMap<String, Float> q) -> q.get("accuracy"))
				.thenComparingDouble((HashMap<String, Float> q) -> q.get("precision"))
				// add new comparing fields here
				.compare(this.getQuality(), other);
    }
	
	/**
	 * receives the samples from the document and fills the qualities
	 * HashMap with all relevant fields. In case new values should be added,
	 * add them here into the qualities and add the comparators
	 * to the {@link this.compareTo} compare to function.
	 * @param documents
	 * @return
	 */
	HashMap<String, Float> calculateQuality(ArrayList<Document> documents){
		HashMap<String, Float> qualities = new HashMap<>();
		HashSet<String> templateValues = this.template.getAllPaths();
		// TODO: quickfix since text content was too hard to retrieve from xml as a path
		templateValues.remove("text()");
		qualities.put("accuracy",calculateAccuracy(templateValues, documents));
		qualities.put("precision",calculatePrecision(templateValues, documents));
		qualities.put("recall",calculateRecall(templateValues,documents));
		return qualities;

		
	}
	/**
	 * Calculates the accuracy.
	 * Accuracy is defined as
	 * |{relevant documents} intersect {retrieved documents}| + true negatives  /
	 * |{retrieved documents}| + |{retrieved documents} intersect {relevant documents}| + true negatives
	 * @param templateValues
	 * @param documents
	 * @return
	 */
	Float calculateAccuracy(HashSet<String> templateValues, ArrayList<Document> documents){
		HashSet<String> retrievedSource = new HashSet<>(templateValues);
		float accuracy = 0.0000f;
		for (Document document : documents){
			HashSet<String> relevantSource = getAllAttributeNames(document); // source of the relevant items since we have to copy them multiple times

			// |{relevant documents} intersect {retrieved documents}| + true negatives 
			HashSet<String> relevant = new HashSet<>(relevantSource);
			HashSet<String> retrieved = new HashSet<>(retrievedSource);
			relevant.retainAll(retrieved);
			float tp = (float) relevant.size() + 0; // 0 since we never have true negatives;
			// |{retrieved documents}| + |{retrieved documents} intersect {relevant documents}| + true negatives

			relevant = new HashSet<>(relevantSource);
			retrieved = new HashSet<>(retrievedSource);
			relevant.addAll(retrieved);
			float n = (float) relevant.size();
			accuracy += tp/n;
		}
		return accuracy/documents.size();
	}
	
	/**
	 * Calculates the precision.
	 * Precision is defined as 
	 * |{relevant documents} intersect {retrieved documents}|  /
	 * |{retrieved documents}|
	 * @param templateValues
	 * @param documents
	 * @return
	 */
	Float calculatePrecision(HashSet<String> templateValues, ArrayList<Document> documents){
		HashSet<String> retrieved = new HashSet<>(templateValues);
		float precision = 0.0000f;
		float cardinalityRetrieved = (float) retrieved.size(); // |{retrieved documents}|
		for (Document document : documents){
			HashSet<String> relevant = getAllAttributeNames(document);
			relevant.retainAll(retrieved); // {relevant documents} intersect {retrieved documents}
			precision += relevant.size()/cardinalityRetrieved; 
		}
		return precision/documents.size(); // avg.
	}
	
	/**
	 * Calculates the recall.
	 * Precision is defined as 
	 * |{relevant documents} intersect {retrieved documents}|  /
	 * |{relevant documents}|
	 * @param templateValues
	 * @param documents
	 * @return
	 */
	Float calculateRecall(HashSet<String> templateValues, ArrayList<Document> documents){
		HashSet<String> retrieved = new HashSet<>(templateValues);
		Float recall = 0.0000f;
		for (Document document : documents){
			HashSet<String> relevant = getAllAttributeNames(document);
			Float cardinalityRetrieved = (float) relevant.size(); // |{relevant documents}|
			relevant.retainAll(retrieved); // {relevant documents} intersect {retrieved documents}
			recall += relevant.size()/cardinalityRetrieved; 
		
		}
		return recall/documents.size(); // avg.
	}

	/**
	 * Receives a document and extracts all attribute names from the document.
	 * @param document
	 * @return
	 */
	HashSet<String> getAllAttributeNames(Document document){
		XPathFactory xPath = XPathFactory.newInstance();
		try{
		NodeList atts = (NodeList) xPath.newXPath().compile("//@*").evaluate(document, XPathConstants.NODESET);
		HashSet<String> attributes = new HashSet<>();

		for (int i = 0; i < atts.getLength(); i++){
			attributes.add("@"+atts.item(i).getLocalName());
		}
		return attributes;
		}
		catch(XPathExpressionException e){ // when we can't read the subtree we just assume we can't retrieve anything.
			System.err.println("Couldn't read attributes. "+e.getMessage());
			return new HashSet<>();
		}
	}


}
