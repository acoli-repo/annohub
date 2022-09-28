package de.unifrankfurt.informatik.acoli.fid.detector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import de.unifrankfurt.informatik.acoli.fid.exec.Executer;
import de.unifrankfurt.informatik.acoli.fid.types.DetectionMethod;
import de.unifrankfurt.informatik.acoli.fid.types.ModelMatch;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;


/**
 * Compute annotation models for each CONLL posTag/feature column which provide the best coverage of the 
 * used TAGs in the file
 * @author frank
 *
 */
public class ModelEvaluatorQ implements ModelEvaluator {

	
	/**
	 * Selection of best models for CONLL resources. The selection is based on the following parameters with given priority  
	 * 1) tagset coverage
	 * 2) # exclusive tags
	 * 3) # total hit count
	 * 4) # exclusive hit count 
	 * In case of tie the next parameter with lower priority is used. If all parameters are equal then the selection
	 * of the best model for a conll column is random 
	 * @param autoDeleteConllModelsWithTrivialResults TODO
	 * @return model view
	 */
	 public static void selectBestModelMatchingsCONLL(ArrayList <ModelMatch> modelMatchings, boolean autoDeleteConllModelsWithTrivialResults) {
		 
		 	Utils.debug("selectBestModelMatchingsCONLL");
		 	
		 	// Select matching modell for each CONLL column
			HashMap<Integer, ModelMatch> bestColumnModels = new HashMap<Integer, ModelMatch>();
			HashSet<Integer> columns = new HashSet<Integer>();
			int col = 0;
			
			for (ModelMatch mm : modelMatchings) {
				
				// Skip trivial model matchings and such with coverage below threshold
				// that have not manually been selected
				if (mm.getCoverage() < Executer.coverageThresholdOnLoad || mm.isTrivialModelMatch()) {
					if (!(mm.getDetectionMethod() == DetectionMethod.MANUAL  && mm.isSelected())) {
						Utils.debug("Disabled the model "+mm.getModelType()+" "+mm.getConllColumn());
						mm.outputTagProperties();
						continue;
					}
				}
				
				// Clear model isSelected flag (now see below)
				// mm.setSelected(false);
				
				// Never choose a manually unselected model
				if (mm.getDetectionMethod() == DetectionMethod.MANUAL && !mm.isSelected()) {
					continue;
				}
				
				col = mm.getConllColumn();
				columns.add(col);
				if (!bestColumnModels.containsKey(col)) {
					bestColumnModels.put(col, mm);
					//if (mm.getCoverage() >= 0.5f && mm.getCoverage() <= 1.0f) bestColumnModels.put(col, mm);
				}
				else {
					
					// Do not change best column model if it was manually selected
					if (bestColumnModels.get(col).getDetectionMethod() == DetectionMethod.MANUAL &&
						bestColumnModels.get(col).isSelected()) {
						continue;
					}
					
					// Always choose a manually selected model over all other models
					if (mm.getDetectionMethod() == DetectionMethod.MANUAL &&
						mm.isSelected()) {
						bestColumnModels.put(col, mm);
						continue;
					}
					
					// 1. criteria is coverage
					if (bestColumnModels.get(col).getCoverage() < mm.getCoverage()) {
					//if (bestColumnModels.get(col).getCoverage() < mm.getCoverage() && mm.getCoverage() >= 0.5f && mm.getCoverage() <= 1.0f) {
						bestColumnModels.put(col, mm);
					}
					
					else {
						// if coverage of two models is equal then
						// 2. criteria is exclusive types
						if (bestColumnModels.get(col).getCoverage() == mm.getCoverage()) {
							if (bestColumnModels.get(col).getExclusiveHitTypes() < mm.getExclusiveHitTypes()) {
								bestColumnModels.put(col, mm);
							} else {
								// if #exclusiveHitTypes of two models is equal then
								// 3. criteria is total hit count
								if (bestColumnModels.get(col).getExclusiveHitTypes() == mm.getExclusiveHitTypes()) {
									if (bestColumnModels.get(col).getHitCountTotal() < mm.getHitCountTotal()) {
										bestColumnModels.put(col, mm);
									} else {
										if (bestColumnModels.get(col).getHitCountTotal() == mm.getHitCountTotal()) {
											if (bestColumnModels.get(col).getExclusiveHitCountTotal() < mm.getExclusiveHitCountTotal()) {
												bestColumnModels.put(col, mm);
											}
										}
									}
								}
							} 
						}
					}
				}
			}
			
			
			if (autoDeleteConllModelsWithTrivialResults) {
			// Drop a model column (remove all models for that column) -
			// if no best model for that column was found
			boolean deleted;
			for (int c : columns) {
				deleted = false;
				if (!bestColumnModels.containsKey(c)) {
					Iterator<ModelMatch> it = modelMatchings.iterator();
					while (it.hasNext()) {
						if (it.next().getConllColumn() == c) {
							it.remove();
							deleted = true;
						}
					}
					if (deleted) {
						Utils.debug("Deleted the model column "+col+" because all found model matchings had trivial results !");
					}
				}
			}
			}
			
			// 1. Unselect all models
			for (ModelMatch mm : modelMatchings) {
				mm.setSelected(false);
			}
			
			// 2. Set selected flag for best model for each conll column
			Utils.debug("Selected Models :");
			for (ModelMatch mm : bestColumnModels.values()) {
				mm.setSelected(true);
				Utils.debug("+"+ mm.getModelType());
			}
	}
	 
	 
	public static void filterTrivialModelMatchingsRDF(ArrayList <ModelMatch> modelMatchings, boolean autoDeleteRdfModelsWithTrivialResults) {
		 
		 	Utils.debug("filterTrivialModelMatchingsRDF");
			
		 	Iterator<ModelMatch> iterator = modelMatchings.iterator();
		 	
		 	while (iterator.hasNext()) {
			
		 		ModelMatch mm = iterator.next();
		 		
		 		//mm.outputTagProperties();
		 		//Utils.debug("differentHitTypes "+mm.getDifferentHitTypes());
		 		//Utils.debug("total hits "+mm.getHitCountTotal());
			
				// Skip trivial model matchings and such with coverage below threshold
				// that have not manually been selected
				if (mm.getCoverage() < Executer.coverageThresholdOnLoad || mm.isTrivialModelMatch()) {
					if (!(mm.getDetectionMethod() == DetectionMethod.MANUAL  && mm.isSelected())) {
						mm.outputTagProperties();
						if (autoDeleteRdfModelsWithTrivialResults) {
							Utils.debug("Deleted the model "+mm.getModelType());
							iterator.remove();
						} else {
							Utils.debug("Disabled the model "+mm.getModelType());
							mm.setSelected(false);
						}
						continue;
					}
				}
		 	}
	 }
}
