package de.unifrankfurt.informatik.acoli.fid.detector;

import java.util.ArrayList;

import de.unifrankfurt.informatik.acoli.fid.conll.ConllCSVSentence;
import de.unifrankfurt.informatik.acoli.fid.search.GWriter;
import de.unifrankfurt.informatik.acoli.fid.types.ModelMatch;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;


/**
 * Interface for CONLL model detection. A ModelEvaluator reads data from the class database and computes the annotation models the occur with a metric that is
 * implemented in the computeModelsFromSampleSentences method. At the end of this method the result should be written to the argument resourceInfo object.
 * Since an evaluator only reads data different implementations do not depend on a recomputation of the underlying data.
 *   
 * @author frank
 *
 */
public interface ModelEvaluator {

	static void computeModelsFromSampleSentences(
			GWriter writer,
			ResourceInfo resourceInfo,
			ArrayList <ConllCSVSentence> conllSentences
			){};
	
	static void selectBestModelMatchings(ArrayList <ModelMatch> modelMatchings){};
}
