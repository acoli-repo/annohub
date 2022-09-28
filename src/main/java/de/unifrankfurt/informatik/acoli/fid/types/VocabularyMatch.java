package de.unifrankfurt.informatik.acoli.fid.types;

import java.io.Serializable;

public class VocabularyMatch implements Serializable {

	private static final long serialVersionUID = 5475876828982935129L;
	private VocabularyType vocabulary;
	private DetectionMethod detectionMethod = DetectionMethod.AUTO;
	
	
	public VocabularyMatch (VocabularyType v) {
		this.vocabulary = v;
	}
	
	public VocabularyMatch (VocabularyType v, DetectionMethod m) {
		this.vocabulary = v;
		this.detectionMethod = m;
	}
	
	public VocabularyType getVocabulary() {
		return vocabulary;
	}
	public void setVocabulary(VocabularyType vocabulary) {
		this.vocabulary = vocabulary;
	}
	public DetectionMethod getDetectionMethod() {
		return detectionMethod;
	}
	public void setDetectionMethod(DetectionMethod detectionMethod) {
		this.detectionMethod = detectionMethod;
	}
}
