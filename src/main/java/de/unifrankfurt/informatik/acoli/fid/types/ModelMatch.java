package de.unifrankfurt.informatik.acoli.fid.types;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import de.unifrankfurt.informatik.acoli.fid.util.Utils;


public class ModelMatch implements Comparable<ModelMatch>, Serializable{
	
	
	private static final long serialVersionUID = 235683756230L;
	
	private ModelType modelType = ModelType.valueOf("UNKNOWN");
	private Long differentHitTypes=0L;	// (e.g. {V,NN,DET} => differentHitTypes = 3
	private Long exclusiveHitTypes=0L;	// annotations only found in this model
	private Long hitCountTotal=0L;
	private Long exclusiveHitCountTotal=0L; // total hit count for exclusiveHitTypes
	private DetectionMethod detectionMethod = DetectionMethod.AUTO;
	private DetectionSource detectionSource = DetectionSource.ANNOMODEL;
	private int conllColumn= NOCOLUMN;	// default no
	private String xmlAttribute="";		// default no
	private String rdfProperty="";		// default no
	private Float coverage = 1.0f;		// (default 100%) = percentage of matched annotations with this model
	private boolean selected = true;	// e.g. for a CONLL column (only one Model will be selected); for other filetypes (e.g. RDF) multiple Models can be selected.
	private float confidence = 0.0f;
	private float recall = 0.0f;		// true positives / all positives
	private Long falseNegativeTypes = 0L;
	private Long falseNegativeCount = 0L;
	public static final int NOCOLUMN = -1;
	private int maxTagLenght = -1;		// maximal tag length
	private boolean onlyNumericTags = false;  // Tags are only numbers (e.g. 1,2,3)
	private boolean onlySymbolicTags = false; // Tags are only symbols (e.g. /+#)
	private int totalTokenCount = 0; 
	// All found tokens for a conll-column | xml-attribute | rdf-property
	// (The number includes also unmatched tokens)
	// (used to compute coverage)
	private Date updated = new Date();
	private String updateText = "added";

	private int tagCount=0;
	private int classCount=0;
	

	

	public ModelMatch (ModelType modelType) {
		this.modelType = modelType;
		setDetectionMethod(DetectionMethod.AUTO);
	}
	
	public ModelMatch (ModelType modelType, DetectionMethod detectionMethod) {
		this.modelType = modelType;
		setDetectionMethod(detectionMethod);
	}
	
	public ModelMatch (ModelType modelType, Long differentHitTypes, Long hitCount, DetectionMethod detectionMethod) {
		this.modelType = modelType;
		this.differentHitTypes = differentHitTypes;
		this.hitCountTotal = hitCount;
		setDetectionMethod(detectionMethod);
	}
	
	
	public void setTotalTokenCount(int count) {
		this.totalTokenCount = count;
	}
	
	public int getTotalTokenCount() {
		return totalTokenCount;
	}
	
	public ModelType getModelType() {
		return modelType;
	}
	public void setModelType(ModelType modelType) {
		this.modelType = modelType;
	}
	public Long getDifferentHitTypes() {
		return differentHitTypes;
	}
	public void setDifferentHitTypes(Long differentHitTypes) {
		this.differentHitTypes = differentHitTypes;
	}
	public Long getHitCountTotal() {
		return hitCountTotal;
	}
	public void setHitCountTotal(Long hitCount) {
		this.hitCountTotal = hitCount;
	}

	public DetectionMethod getDetectionMethod() {
		return detectionMethod;
	}

	public void setDetectionMethod(DetectionMethod detectionMethod) {
		this.detectionMethod = detectionMethod;
		if (detectionMethod.equals(DetectionMethod.MANUAL)) this.detectionSource = DetectionSource.SELECTION;
	}
	
	public int getConllColumn() {
		return conllColumn;
	}

	public void setConllColumn(int conllColumn) {
		this.conllColumn = conllColumn;
	}

	public float getCoverage() {
		return coverage;
	}

	public void setCoverage(float coverage) {
		this.coverage = coverage;
	}

	@Override
	public int compareTo(ModelMatch other) {
		return coverage.compareTo(other.coverage);
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + conllColumn;
		result = prime * result
				+ ((coverage == null) ? 0 : coverage.hashCode());
		result = prime
				* result
				+ ((differentHitTypes == null) ? 0 : differentHitTypes
						.hashCode());
		result = prime * result
				+ ((hitCountTotal == null) ? 0 : hitCountTotal.hashCode());
		result = prime * result
				+ ((modelType == null) ? 0 : modelType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ModelMatch other = (ModelMatch) obj;
		if (conllColumn != other.conllColumn)
			return false;
		if (coverage == null) {
			if (other.coverage != null)
				return false;
		} else if (!coverage.equals(other.coverage))
			return false;
		if (differentHitTypes == null) {
			if (other.differentHitTypes != null)
				return false;
		} else if (!differentHitTypes.equals(other.differentHitTypes))
			return false;
		if (hitCountTotal == null) {
			if (other.hitCountTotal != null)
				return false;
		} else if (!hitCountTotal.equals(other.hitCountTotal))
			return false;
		if (!modelType.equals(other.modelType))
			return false;
		return true;
	}

	public String getDetectionMethodShort() {
		if (getDetectionMethod() != null && getDetectionMethod() != DetectionMethod.MANUAL) {
			return StringUtils.substring("("+getDetectionMethod().name(), 0, 4)+")";
		}
		else {
			return "";
		}
	}

	public Long getExclusiveHitTypes() {
		return exclusiveHitTypes;
	}

	public void setExclusiveHitTypes(Long exclusiveHitTypes) {
		this.exclusiveHitTypes = exclusiveHitTypes;
	}

	public Long getExclusiveHitCountTotal() {
		return exclusiveHitCountTotal;
	}

	public void setExclusiveHitCountTotal(Long exclusiveHitCountTotal) {
		this.exclusiveHitCountTotal = exclusiveHitCountTotal;
	}

	public float getConfidence() {
		return confidence;
	}

	public void setConfidence(float confidence) {
		this.confidence = confidence;
	}

	public DetectionSource getDetectionSource() {
		return detectionSource;
	}

	public void setDetectionSource(DetectionSource detectionSource) {
		this.detectionSource = detectionSource;
	}

	public float getRecall() {
		return recall;
	}

	public void setRecall(float recall) {
		this.recall = recall;
	}

	public Long getFalseNegativeTypes() {
		return falseNegativeTypes;
	}

	public void setFalseNegativeTypes(Long falseNegativeTypes) {
		this.falseNegativeTypes = falseNegativeTypes;
	}

	public Long getFalseNegativeCount() {
		return falseNegativeCount;
	}

	public void setFalseNegativeCount(Long falseNegativeCount) {
		this.falseNegativeCount = falseNegativeCount;
	}

	public String getXmlAttribute() {
		return xmlAttribute;
	}

	public void setXmlAttribute(String xmlAttribute) {
		this.xmlAttribute = xmlAttribute;
	}

	public String getTableID() {
		return modelType.name()+"#"+conllColumn+"#"+rdfProperty+"#"+xmlAttribute+"#"+detectionSource+"#"+detectionMethod+"#"+selected;
	}

	public String getRdfProperty() {
		return rdfProperty;
	}

	public void setRdfProperty(String rdfProperty) {
		this.rdfProperty = rdfProperty;
	}

	public int getMaxTagLenght() {
		return maxTagLenght;
	}

	public void setMaxTagLenght(int maxTagLenght) {
		this.maxTagLenght = maxTagLenght;
	}

	public boolean hasOnlyNumericTags() {
		return onlyNumericTags;
	}

	public void setOnlyNumericTags(boolean onlyNumericHits) {
		this.onlyNumericTags = onlyNumericHits;
	}

	public void setOnlySymbolicTags(boolean onlySymbols) {
		this.onlySymbolicTags = onlySymbols;		
	}

	public boolean hasOnlySymbolicTags() {
		return onlySymbolicTags;
	}
	
	/**
	 * Find special properties of the successfully found tags 
	 * (e.g. only numbers|symbols|1-letter tags) were found). 
	 * Set the fields onlyNumbericTags,maxTagLenght and
	 * onlySymbolicTags accordingly. 
	 * @param List of successfully found tags for this model
	 */
	public void computeTagProperties(ArrayList<String> tags) {
		if (!tags.isEmpty()) {
			boolean onlyIntegers = true;
			boolean onlySymbols = true;
			int tagCount=0;
			int classCount=0;
			int maxTagLength = 0;
			for (String ta : tags) {
				if (ta.length() > maxTagLength) maxTagLength = ta.length();
				boolean isInteger = true;
				if (ta.startsWith("http")) {
					classCount++;
				} else {
					tagCount++;
				}
				try{
					Integer.parseInt(ta);
					  // is an integer!
					} catch (NumberFormatException e) {
						isInteger=false;
					}
				if (!isInteger) onlyIntegers = false;
				if (isInteger || StringUtils.isAlpha(ta)) onlySymbols=false;
			}
			setOnlyNumericTags(onlyIntegers);
			setMaxTagLenght(maxTagLength);
			setOnlySymbolicTags(onlySymbols);
			setTagCount(tagCount);
			setClassCount(classCount);
		} 
	}
	
	

	/**
	 * Evaluate the fields onlyNumericTags, maxTagLength, onlySymbolicTags
	 * @return true if trivial else false
	 */
	public boolean isTrivialModelMatch() {
		if (hasOnlyNumericTags()  || 
			hasOnlySymbolicTags() ||	
			getMaxTagLenght() == 1 ||
			(getDifferentHitTypes() == 1 && getTagCount() > 0) // introduced for RDF
			// TODO new option minDifferentHitTypesRDF and minHitTotalCountRDF
				)  return true;
		else
		return false;
	}
	
	public void outputTagProperties() {
		Utils.debug("maximal tag length  : "+getMaxTagLenght());
		Utils.debug("hasOnlyNumericTags  : "+hasOnlyNumericTags());
		Utils.debug("hasOnlySymbolicTags : "+hasOnlySymbolicTags());
		Utils.debug("coverage            : "+getCoverage());

	}

	public Date getDate() {
		return updated;
	}

	public void setDate(Date updated) {
		this.updated = updated;
	}
	
	
	/**
	 * Return the value of the Date.getTime() function
	 * @return Date.getTime or 0 
	 */
	public Long getDateGetTime() {
		if (updated != null) {
			return updated.getTime();
		} else {
			return 0L;
		}
	}
	
	/** 
	 * Return short version YYYY-MM-DD for Date type
	 * @return
	 */
	public LocalDate getLocalDate() {
		return updated.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}

	public String getUpdateText() {
		return updateText;
	}

	public void setUpdateText(String updateText) {
		this.updateText = updateText;
	}

	public int getClassCount() {
		return classCount;
	}

	public void setClassCount(int classCount) {
		this.classCount = classCount;
	}
	
	public void setTagCount(int tagCount) {
		this.tagCount=tagCount;
	}

	public int getTagCount() {
		return tagCount;
	}

	
}
