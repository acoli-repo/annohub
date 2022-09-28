package de.unifrankfurt.informatik.acoli.fid.owl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.unifrankfurt.informatik.acoli.fid.conll.ParserCONLL;
import de.unifrankfurt.informatik.acoli.fid.detector.ModelEvaluatorQ;
import de.unifrankfurt.informatik.acoli.fid.exec.Executer;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager;
import de.unifrankfurt.informatik.acoli.fid.search.GWriter;
import de.unifrankfurt.informatik.acoli.fid.types.DatabaseConfiguration;
import de.unifrankfurt.informatik.acoli.fid.types.ProcessingFormat;
import de.unifrankfurt.informatik.acoli.fid.types.ModelMatch;
import de.unifrankfurt.informatik.acoli.fid.types.ModelType;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceFormat;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.util.IndexUtils;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;


/**
 * Update the model matching results for each file. This needs to be applied after any model 
 * in the model graph has been changed, removed or added. Manual selected model matchings will be
 * retained.
 * @author frank
 *
 */


public class ResultUpdater {
	
	 static DatabaseConfiguration registryDbConfig;
	 static DatabaseConfiguration dataDbConfig;
	 static Executer executer = null;
	 static XMLConfiguration fidConfig = null;
	 static String testFileDirAsUrl = "file://"+System.getProperty("user.dir")+"/testResources/";
	 static String testFileDirAsString = System.getProperty("user.dir")+"/testResources/";
	 static boolean exportregistryDb2Json = false;
	 static boolean exportdataDb2Json = false;
	 static String configurationFile = System.getProperty("user.dir")+"/FIDConfig.xml";
	 
	 static ArrayList<ModelMatch> otherUnmatchedModelsAll = new ArrayList<ModelMatch>();
	 private ResourceManager resourceManager;
	 private GWriter writer;
	 
	 
	 public ResultUpdater(Executer executer) {
		 this.resourceManager = executer.getResourceManager();
		 this.writer = executer.getWriter();
	 }
	 
	 public ResultUpdater(ResourceManager resourceManager, GWriter writer) {
		 this.resourceManager = resourceManager;
		 this.writer = writer;
	 }


	/**
	 * Try to match known HITs with updated or new models
	 */
	public void reconnectHits() {
				

		Utils.debug("Reconnecting hits");
		
		ModelType modelType = null;
		
		int all = writer.getQueries().getHitsTypeURIObject().size();
		int c = 1;
		Utils.debug("Url Object hits : "+all);
		for (Vertex v : writer.getQueries().getHitsTypeURIObject()) {
			writer.addEdgeHit2Class(v, v.value(GWriter.HitObject), null, modelType);
			Utils.debug(c+++"/"+all);
		}
		
		all = writer.getQueries().getHitsTypeLiteralObject().size();
		c = 1;
		Utils.debug("Literal object hits : "+all);
		for (Vertex v : writer.getQueries().getHitsTypeLiteralObject()) {
			writer.addEdgeHit2Tag (v, v.value(GWriter.HitObject), modelType, null);
			Utils.debug(c+++"/"+all);
		}
		
		all = writer.getQueries().getHitsTypeTag().size();
		c = 1;
		Utils.debug("Tag hits : "+all);
		for (Vertex v : writer.getQueries().getHitsTypeTag()) {
			writer.addEdgeHit2Tag (v, v.value(GWriter.HitTag), modelType, null);
			Utils.debug(c+++"/"+all);
		}
		
		all = writer.getQueries().getHitsTypeFeature().size();
		c = 1;
		Utils.debug("Feature hits : "+all);
		for (Vertex v : writer.getQueries().getHitsTypeFeature()) {

			writer.addEdgeFeatureHit2Tag (
			v,
			(String) v.value(GWriter.HitFeature),
			(String) v.value(GWriter.HitFeatureValue),
			modelType);
			
			Utils.debug(c+++"/"+all);
		}
		
		
		writer.getQueries().commit();
	}
	
	
	/**
	 * Try to match unmatched tags from CoNLL files (not implemented : unmatched classes/tags from RDF files) 
	 * to updated or new models.
	 */
	public void reconnectUnmatchedConllTagsAndFeatures(ArrayList <ResourceInfo> rfl) {
		
		
		Utils.debug("reconnectUnmatchedConllTagsAndFeatures");
		
		HashMap<String, HashMap<String, Long>> featureMap = new HashMap<String, HashMap<String, Long>>();
		HashMap<String, Long> tagCounts = new HashMap<String, Long>();
		Long tokenCount = 0L;
						
        //ArrayList <ResourceInfo> rfl = resourceManager.getAllResourcesRI();
                
        for (ResourceInfo resourceInfo : rfl) {
        	
        	Utils.debug("reconnecting "+resourceInfo.getDataURL()+"->"+resourceInfo.getFileInfo().getFileId());
		
        	if (resourceInfo.getFileInfo().isRDFFile()) {
        		Utils.debug("reconnectUnmatchedConllTagsAndFeatures : skipping RDF");
        		continue;
        	}
        	boolean isFeatureColumn;
        	String query;
        	String featureName;
        	String featureValue;
        	
        	for (int col : resourceInfo.getFileInfo().getConllColumns()) {
        		
        		isFeatureColumn=false;
        		//Utils.debug("column : "+col);
        		
        		tagCounts.clear();
            	featureMap.clear();
            	
            	// Get unmatched tokens in a CONLL column
            	HashMap<String, Long> tokens  = 
	        			resourceManager.getFileTokensWithCount(resourceInfo, col);
	        	
            	if (tokens == null) continue; // for columns that have no tokens like text columns
            	
	        	//Utils.debug("column "+col+" has "+tokens.size()+ " tokens ");
	        	
	        	// alternatively use (slow)
	        	//resourceManager.getQueries().getUnmatched(rm, resourceInfo, mm, fileResult);
	        	
	    		// sort tokens by tag (e.g. NOUN) or feature (x=y)
	        	for(String token : tokens.keySet()){
	        	
	        		tokenCount = tokens.get(token);
	        		if (token.contains("=")) {
	        			isFeatureColumn=true;
	        			ParserCONLL.addFeatures(featureMap, token);
	        		} else {
	        			tagCounts.put(token,tokenCount);
	        		}
	        	}
	        	
	        	
	        	// filter already matched tags / features
	        	if (!isFeatureColumn) {
	        		query = "g.V().hasLabel('"+GWriter.HitVertex+"').has('"+GWriter.HitResourceUrl+"','"+resourceInfo.getDataURL()+"')"
	        					+ ".has('"+GWriter.HitFileId+"','"+resourceInfo.getFileInfo().getFileId()+"')"
	        					+ ".has('"+GWriter.HitConllColumn+"',"+col+").has('"+GWriter.HitTag+"').values('"+GWriter.HitTag+"')";
	        		HashSet<String> result = new HashSet<String>(writer.getQueries().genericStringQuery(query));
	        		
	        		HashSet<String> tagCounts_ = new HashSet<String>(tagCounts.keySet());
	        		for (String key : tagCounts_) {
	        			if (result.contains(key)) {
	        				tagCounts.remove(key);
	        			}
	        		}
	        	} else {
	        		query = "g.V().hasLabel('"+GWriter.HitVertex+"').has('"+GWriter.HitResourceUrl+"','"+resourceInfo.getDataURL()+"')"
        					+ ".has('"+GWriter.HitFileId+"','"+resourceInfo.getFileInfo().getFileId()+"')"
        					+ ".has('"+GWriter.HitConllColumn+"',"+col+").has('"+GWriter.HitFeature+"')";
	        		
	        		ArrayList<Vertex> result = writer.getQueries().genericVertexQuery(query);
	        		for (Vertex v : result) {
	        			featureName = v.value(GWriter.HitFeature);
	        			featureValue = v.value(GWriter.HitFeatureValue);
	        			if (featureMap.containsKey(featureName)) {
	        				if (featureMap.get(featureName).containsKey(featureValue)) {
	        					// remove feature value
	        					featureMap.get(featureName).remove(featureValue);
	        					// remove feature completely if no more values
	        					if (featureMap.get(featureName).isEmpty()) featureMap.remove(featureName);
	        				}
	        			}
	        		}
	        	}
	        	
	        	Utils.debug("tagCounts :");
	        	for (String tag : tagCounts.keySet()) {
	        		Utils.debug(tag + " : "+tagCounts.get(tag));
	        	}
	        	
	        	
				// Add all tags
	        	writer.writeConll(col, tagCounts, resourceInfo, null, null);
	        	
				// Add all features
	        	writer.writeConllFeatures(col, featureMap, resourceInfo, null);
	        	}
	        }
		
	}
	
	/**
	 * 
	 * @param rfl
	 */
	public void updateModelResultsAfterOliaUpdate (ArrayList<ResourceInfo> rfl) {
		updateModelResults(rfl, null, null);
	}
	
	
	/**
	 * 
	 * @param rfl
	 * @param columnTokens
	 */
	public void updateModelResultsAfterCONLLOrXMLRescan (ArrayList<ResourceInfo> rfl, HashMap<Integer, HashMap<String, Long>> columnTokens) {
		updateModelResults(rfl, null, columnTokens);
	}
	
	
	/**
	 * 
	 * @param rfl
	 * @param predicate2FoundObjectsMap
	 */
	public void updateModelResultsAfterRDFRescan (ArrayList<ResourceInfo> rfl, HashMap<String, HashMap<String, Long>> predicate2FoundObjectsMap) {
		updateModelResults(rfl, predicate2FoundObjectsMap, null);
	}
	
	
	/**
	 * Function to be called after model updates or rescanning of resources
	 * @param rfl
	 * @param predicate2FoundObjectsMap
	 * @param conllTokens
	 */
	private void updateModelResults(
			ArrayList<ResourceInfo> rfl,
			HashMap<String , HashMap<String, Long>> predicate2FoundObjectsMap,
			HashMap<Integer, HashMap<String, Long>> columnTokens
			) {
	
		Utils.debug("updating model results ...");
		Utils.debug("Resources : "+rfl.size());
		
		HashSet<String> good = new HashSet<String>();
		HashSet<String> bad = new HashSet<String>();
		ArrayList <ModelMatch> foundModelsFromQuery = new ArrayList<ModelMatch>();


		for (ResourceInfo resourceInfo : rfl) {
			
			foundModelsFromQuery.clear();
			Utils.debug("processing file :"+ resourceInfo.getDataURL() + " -> "+resourceInfo.getFileInfo().getRelFilePath());
			
			if (resourceInfo.getFileInfo().isRDFFile()) {
				 
				Utils.debug("Predicates :");
				
				HashSet<String> hitRdfProperties;
				HashMap<String, Integer> rdfTokenCounts = new HashMap<String,Integer>();
				
				if (predicate2FoundObjectsMap != null) {
					hitRdfProperties = new HashSet<String>(predicate2FoundObjectsMap.keySet());
					for (String rdfProp : hitRdfProperties) {
						rdfTokenCounts.put(rdfProp, predicate2FoundObjectsMap.get(rdfProp).size());
					}
				}
				else {
					// Get all RDF properties of all HITs for file (The set of RDF properties in the RDB is a subset of the RDF properties of HITs)
					hitRdfProperties = writer.getQueries().getResourceHitRdfProperties(resourceInfo);
					rdfTokenCounts = resourceManager.getRdfTokenCounts(resourceInfo);
				}
				
				for (String key : rdfTokenCounts.keySet()) {
					Utils.debug("token : "+key +" count : "+rdfTokenCounts.get(key));
				}
				
				for (String predicate : hitRdfProperties) {
					Utils.debug("hitRdfProperty : "+predicate);
					foundModelsFromQuery.addAll(writer.getQueries().
					getModelMatchingsNew(
							writer,
							resourceInfo,
							ModelMatch.NOCOLUMN,
							predicate,
							rdfTokenCounts.get(predicate))
							);
				}
				
				
				Utils.debug("RDF models "+foundModelsFromQuery.size());
				for (ModelMatch mm : foundModelsFromQuery) {
					Utils.debug(mm.getModelType());
				}
			}
			
			Utils.debug("End debug");
			
			// CONLL & XML
			// TODO remove with native XML file format
			if (resourceInfo.getFileInfo().isConllFile()) {
				
				Utils.debug("DEBUG start (see source to enable more output)");
				
				ProcessingFormat fileFormat = resourceInfo.getFileInfo().getProcessingFormat();
				
				// TODO remove with native XML file format
				if (IndexUtils.determineFileFormat(resourceInfo) == ResourceFormat.XML)
					fileFormat = ProcessingFormat.XML; // legacy (if no extra XML format)
			
				HashSet<Integer> hitConllColumns = writer.getQueries().getResourceHitConllColumns(resourceInfo);
				
				/*Utils.debug("hallo hitConllColumns");
				for (int x : hitConllColumns) {
					Utils.debug(x);
				}*/
				
				switch (fileFormat) {
				
					case CONLL :
						
						Utils.debug("is conll file");
						
						HashMap<Integer, Integer> conllTokenCounts = new HashMap<Integer, Integer>();
						if (columnTokens != null) {
							for (int col : columnTokens.keySet()) {
								conllTokenCounts.put(col, columnTokens.get(col).keySet().size());
							}
						}
						else {
							conllTokenCounts = resourceManager.getConllTokenCounts(resourceInfo);
						}
						
							
						/*Utils.debug("conlltokencounts");
						for (int t : conllTokenCounts.keySet()){
							Utils.debug("col "+t+" : "+conllTokenCounts.get(t));
						}*/
						
						
						for (int col : hitConllColumns) {
							
							if (!conllTokenCounts.containsKey(col)) continue; // TODO quickfix for corrupt tokens/units
							
							//Utils.debug("query hitConllColumn "+col);
							foundModelsFromQuery.addAll(writer.getQueries().getModelMatchingsNew(
								writer,
								resourceInfo,
								col,
								null,
								conllTokenCounts.get(col)));
						}
						Utils.debug("CONLL models "+foundModelsFromQuery.size());
						break;
						
					case XML :
						
						Utils.debug("is xml file");
						
						HashMap<Integer, Integer> xmlTokenCounts = new HashMap<Integer, Integer>();
						if (columnTokens != null) {
							for (int col : columnTokens.keySet()) {
								xmlTokenCounts.put(col, columnTokens.get(col).keySet().size());
							}
						}
						else {
							// Get all CONLL columns of all HITs for file (The set of CONLL columns in the RDB is a subset of the CONLL columns of HITs)
							// XML resources also have attribute CONLL column
							xmlTokenCounts = resourceManager.getXmlTokenCountsByColumn(resourceInfo);
						}

						
						Utils.debug("xmlTokenCounts");
						for (int t : xmlTokenCounts.keySet()){
							Utils.debug("col "+t+" : "+xmlTokenCounts.get(t));
						}
						
						for (int col : hitConllColumns) {
							
							if (!xmlTokenCounts.containsKey(col)) {
								Utils.debug("xmlTokenCounts does not contain column "+col);
								continue; // TODO quickfix for corrupt tokens/units
							}
							
						Utils.debug("hitconllcolumn : "+col);
							foundModelsFromQuery.addAll(writer.getQueries().getModelMatchingsNew(
								writer,
								resourceInfo,
								col,
								null,
								xmlTokenCounts.get(col)));					
						}
						
						//Utils.debug("XML models "+foundModelsFromQuery.size());
						break;
						
					default :
						break;
				}
			
			}
			
			//Utils.debug("foundModelsFromQuery");
			//printModelMatchings(foundModelsFromQuery);
			//Utils.debug("oldmodels");
			//printModelMatchings(resourceInfo.getFileInfo().getModelMatchings());
			
			HashMap<String,ModelMatch> result = new HashMap<String,ModelMatch>();
			ArrayList<ModelMatch> tmp = SerializationUtils.clone(resourceInfo.getFileInfo().getModelMatchings());
			for (ModelMatch mm : tmp) {
				result.put(computeModelMatchHashCode(mm), mm);
			}
			
			ArrayList<ModelMatch> foundModelsClone = SerializationUtils.clone(foundModelsFromQuery);
			boolean ok = compareAndUpdateModelMatchings(
					resourceInfo,
					foundModelsClone,
					SerializationUtils.clone(resourceInfo.getFileInfo().getModelMatchings()), result);
			
			if (ok) {
				good.add(resourceInfo.getDataURL()+" : "+resourceInfo.getFileInfo().getRelFilePath());
			} else {			
				bad.add(resourceInfo.getDataURL()+" : "+resourceInfo.getFileInfo().getRelFilePath());
				
				// Disable or delete models for CoNLL/XML columns in special cases
				if (!resourceInfo.getFileInfo().isRDFFile()) {
					
					// too restrictive !
					//1. Unselect models in columns that previously had no selection (not for RDF) 
					/*HashSet<Integer> disabledColumns = resourceInfo.getFileInfo().getDisabledColumns();
					disabledColumns.remove(ModelMatch.NOCOLUMN);
					for (ModelMatch mx : result.values()) {
						if (disabledColumns.contains(mx.getConllColumn())) {
							mx.setSelected(false);
						}
					}*/
					
					/*
					// (only used for one patch)
					// 2. Unselect models in columns where the resource has a comment like
					// ... unknown model in column x ... OR ... column x unknown annotation model (not for RDF)
					
					int unknownModelColumn = Utils.parseUnknownModelColumn(resourceInfo.getFileInfo().getComment());
					if (unknownModelColumn > -1) {
						for (ModelMatch mx : result.values()) {
							if (mx.getConllColumn() == unknownModelColumn) {
								mx.setSelected(false);
							}
						}
					}*/
					
					// 3. Remove all models in columns that already have been selected as text column
					// ... unknown model in column x ... OR ... column x unknown annotation model (not for RDF)
					HashSet<Integer> selectedTextColumns = resourceInfo.getFileInfo().getSelectedConllColumnsWithText();
					Iterator it = result.entrySet().iterator();
				    while (it.hasNext()) {
				        Map.Entry <String, ModelMatch> pair = (Map.Entry)it.next();
				        Utils.debug(pair.getKey() + " = " + pair.getValue());
				        if (selectedTextColumns.contains(pair.getValue().getConllColumn())) {
				        	it.remove();
				        }
				    }
				}
				
				// Save updated model matchings to registry database
				saveModelMatchings(resourceInfo, result);
			}
			// Models (ok) that have the same numbers for coverage, hit types, etc. are not written.
			// Despite having the same numbers these models can differ in selection / selection method 
			// and selection source.
			// The result is that manual selected/unselected models remain unchanged, which 
			// is desired !
			
			Utils.debug("File : "+resourceInfo.getFileInfo().getRelFilePath()+":"+ok);
		}
		
		
		// Print statistic for all resource files
		Utils.debug("Total result :");
		Utils.debug("good :"+good.size());
		for (String x : good) {
			Utils.debug(x);
		}
		Utils.debug("bad :"+bad.size());
		for (String x : bad) {
			Utils.debug(x);
		}
		
		
		Utils.debug("Other unmatched models : "+otherUnmatchedModelsAll.size());
		for (ModelMatch y : otherUnmatchedModelsAll) {
			Utils.debug(this.computeModelMatchHashCode(y));
		}
		
		
		//for (ModelMatch y : otherUnmatchedModelsAll) {
		//	Utils.debug(this.computeModelMatchHashCode(y));
		//}
		
		//executer.closeDBConnections();
	}
	

	/**
	 * (Debug) Print model matchings details for resource file
	 * @param resourceInfo
	 */
	private void printResourceInfoDetails(ResourceInfo resourceInfo) {
		
		
		printModelMatchings(resourceInfo.getFileInfo().getModelMatchings());
		
		for (String rdfp : resourceInfo.getFileInfo().getRdfPropertyDifferentValues().keySet()) {
			if (!rdfp.isEmpty()) {
				Utils.debug(rdfp+" : "+resourceInfo.getFileInfo().getRdfPropertyDifferentValues().get(rdfp));
			}
		}
		
		for (String xmla : resourceInfo.getFileInfo().getXmlAttributes()) {
			if(!xmla.isEmpty())
			Utils.debug(xmla);
		}
		
		HashMap<Integer,Integer> map = resourceInfo.getFileInfo().getConllColumnDifferentValues();
		for (int col : map.keySet()) {
			Utils.debug("column "+col+ ":"+map.get(col));
		}
		
	}
	
	
	public void printModelMatchings(ArrayList<ModelMatch> modelMatchings) {
		
		int i = 0;
		for (ModelMatch mm : modelMatchings) {
			Utils.debug(i++ +":");
			Utils.debug(mm.getCoverage());
			Utils.debug("different hitTypes"+mm.getDifferentHitTypes());
			Utils.debug("coverage "+mm.getCoverage());
			Utils.debug("column count :"+mm.getDifferentHitTypes()/mm.getCoverage());
			Utils.debug("column "+mm.getConllColumn());
			if (!mm.getRdfProperty().isEmpty()) {
				Utils.debug(mm.getRdfProperty());
			}
			if (!mm.getXmlAttribute().isEmpty()) {
				Utils.debug(mm.getXmlAttribute());
			}
		}
		System.out.println();
	}


	/**
	 * Align unmatched models and update the models in the registry database
	 * @param fromQuery Unmatched models from model database
	 * @param fromRegistry Unmatched models from registry database
	 * @param result reference for updated model matchings (initially these contain the original model matchings)
	 */
	private void updateUnmatchedModelMatchings(ResourceInfo resourceInfo, ArrayList<ModelMatch> fromQuery, ArrayList<ModelMatch> fromRegistry, HashMap<String,ModelMatch> result) {
		
		// 1. Determine corresponding ModelMatchings after model update
		//   Model matchings can
		// - change if counts of hitCount,exclHitCount have changed
		// - disappear because a definition or a model was removed
		// - come up new because a definition or a model was added
		
		ArrayList<ModelMatch> otherUnmatchedModels = new ArrayList<ModelMatch>();
		String modelHashWithoutModelName ="";
		HashSet<ModelMatch> fromQueryMatch = new HashSet<ModelMatch>();

		// Iterate over original model matchings for a file the from registry database
		for (ModelMatch mm : fromRegistry) {
			
			modelHashWithoutModelName = computeModelMatchHashCodeWithoutModelType(mm);
			fromQueryMatch.clear();
			
			// match models by name and by column/RDFProperty/Xpath
			for (ModelMatch mm2 : fromQuery) {
				
				//Utils.debug("fromQuery "+mm2.getModelType().name());
			
				// 1. compare model name
				if (!mm2.getModelType().name().equals(mm.getModelType().name())) continue;
								
				// 2. compare column/RDF property/XPATH path
				if (mm2.getConllColumn() == mm.getConllColumn() &&
					mm2.getRdfProperty().equals(mm.getRdfProperty()) &&
					mm2.getXmlAttribute().equals(mm.getXmlAttribute())
					) { 
					
					// collect matching models from query (e.g. mm=PENN, mm2=PENN,PENNSYN)
					//Utils.debug("fromQuery add model :");
					//Utils.debug("new : "+this.computeModelMatchHashCode(mm2));

					fromQueryMatch.add(mm2);
				}
			}
				
			// Finally evaluate the matching models
			// 1. Compute best matched model (like usually)
			switch (resourceInfo.getFileInfo().getProcessingFormat()) {
			
				case RDF   :
					
					Utils.debug("Update RDF model :");
					
					// 1. copy detection and selection parameter
					for (ModelMatch mmq : fromQueryMatch) {
						copyModelMatchDetectionParameter(mm, mmq);
						mmq.setSelected(mm.isSelected());
					}
					// 2. remove registry model (now always, see below)
					//result.remove(computeModelMatchHashCode(mm));
					//Utils.debug("Remove model from result ");
					//Utils.debug(computeModelMatchHashCode(mm));
					
					// 3. add all models in fromQueryMatch
					for (ModelMatch mmq : fromQueryMatch) {
						result.put(this.computeModelMatchHashCode(mmq),mmq);
						Utils.debug("Add model");
						Utils.debug(computeModelMatchHashCode(mmq));
						Utils.debug("as replacement for model");
						Utils.debug(computeModelMatchHashCode(mm));
						mmq.setUpdateText("changed");
					}
					break;

				case CONLL :
				case XML   :
					
					Utils.debug("Update CONLL model :");
					
					// copy detection parameter
					for (ModelMatch mmq : fromQueryMatch) {
						copyModelMatchDetectionParameter(mm, mmq);
					}
					
					if (mm.isSelected()) {
						Utils.debug("Select best column model");
						// OLD selection will be overwritten with automatic conll selection
						for (ModelMatch x : fromQueryMatch) {
							Utils.debug(this.computeModelMatchHashCode(x)+":"+x.isSelected());
						}
						ModelEvaluatorQ.selectBestModelMatchingsCONLL(new ArrayList<ModelMatch>(fromQueryMatch),false);
						Utils.debug("after select best column model");
						for (ModelMatch x : fromQueryMatch) {
							Utils.debug(this.computeModelMatchHashCode(x)+":"+x.isSelected());
						}
					} else {
						//Utils.debug("Unselect new models");
						// unselect new models (should be false by default anyway)
						for (ModelMatch mmq : fromQueryMatch) {
							mmq.setSelected(false);
						}
					}
					
					// 1. remove registry model (now always, see below)
					//result.remove(computeModelMatchHashCode(mm));
					
					// 2. add all models in fromQueryMatch
					for (ModelMatch mmq : fromQueryMatch) {
						result.put(this.computeModelMatchHashCode(mmq),mmq);
						Utils.debug("Add model");
						Utils.debug("selected : "+mmq.isSelected());
						Utils.debug(computeModelMatchHashCode(mmq));
						Utils.debug("as replacement for model");
						Utils.debug(computeModelMatchHashCode(mm));
						mmq.setUpdateText("changed");
					}
					break;
			
			default :
					otherUnmatchedModels.add(mm);
					break;
			}	
			// Always remove registry model (if matched or unmatched)
			result.remove(computeModelMatchHashCode(mm));
			Utils.debug("Finally removed registry model : "+computeModelMatchHashCode(mm));
		}
		
		
		// Add all models in fromQuery that could not be matched to result
		for (ModelMatch mm : fromQuery) {
			if (!fromQueryMatch.contains(mm)) {
				Utils.debug("Adding new model from query : "+computeModelMatchHashCode(mm));
				result.put(computeModelMatchHashCode(mm),mm);
			}
		}
		
		// for CONLL or XML recompute best model matchings for column
		if (resourceInfo.getFileInfo().getProcessingFormat() != ProcessingFormat.RDF) {
			ModelEvaluatorQ.selectBestModelMatchingsCONLL(new ArrayList<ModelMatch>(result.values()), false);
		}
		
		Utils.debug("Other unmatched models : "+otherUnmatchedModels.size());
		for (ModelMatch mm : otherUnmatchedModels) {
			Utils.debug(computeModelMatchHashCode(mm));
		}
		otherUnmatchedModelsAll.addAll(otherUnmatchedModels);
		Utils.debug("End other unmatched models");
	}
	
	
	
	/**
	 * Save updated MMs of file to registry
	 * @param resourceInfo
	 * @param oldmodel
	 * @param newmodel
	 * 
	 */
	private void saveModelMatchings(ResourceInfo resourceInfo, HashMap<String,ModelMatch> result) {
		
		Utils.debug("Writing model changes :");
		Utils.debug(resourceInfo.getDataURL() + " -> "+resourceInfo.getFileInfo().getRelFilePath());
		System.out.println();

		Utils.debug("RESULT :");
		Utils.debug("OLD");
		for (ModelMatch mm : resourceInfo.getFileInfo().getModelMatchings()) {
			Utils.debug(computeModelMatchHashCode(mm));
		}
		
		Utils.debug("NEW");
		for (ModelMatch mm : result.values()) {
			Utils.debug(computeModelMatchHashCode(mm));
		}
		
		resourceInfo.getFileInfo().setModelMatchings(new ArrayList<ModelMatch>(result.values()));
		resourceManager.updateFileModels(resourceInfo, true);
		resourceManager.updateFileUnitInfo(resourceInfo);
	}
	
	
	/**
	 * Transfer selection parameter
	 * @param fromRegistry
	 * @param fromQuery
	 */
	private void copyModelMatchDetectionParameter(ModelMatch fromRegistry, ModelMatch fromQuery) {
		fromQuery.setDetectionMethod(fromRegistry.getDetectionMethod());
		fromQuery.setDetectionSource(fromRegistry.getDetectionSource());
	}
	
	
	
	private boolean compareAndUpdateModelMatchings(ResourceInfo resourceInfo, ArrayList<ModelMatch> fromQuery, ArrayList<ModelMatch> fromRegistry, HashMap<String,ModelMatch> result) {
		
		Utils.debug("compareAndUpdateModelMatchings");
		Utils.debug("fromRegistry models");
		for (ModelMatch mm : fromRegistry) {
			Utils.debug(mm.getModelType().name());
		}
		Utils.debug("fromQuery models");
		for (ModelMatch mm : fromQuery) {
			Utils.debug(mm.getModelType().name());
		}
		
		// case : found one model match of type UNKNOWN (will never have query results)
		if (fromQuery.isEmpty() && fromRegistry.size() == 1 && fromRegistry.get(0).getModelType().equals(ModelType.valueOf("UNKNOWN")))
		   {Utils.debug("case : found one model match of type UNKNOWN (will never have query results)");
			return true;}
		
		Iterator<ModelMatch> iterator_1 = fromQuery.iterator();
		
		// Iterate over all models found with query
		int matchings = 0;
		while (iterator_1.hasNext()) {
			
			ModelMatch mm_1 = (iterator_1.next());
			String id_1 = computeModelMatchHashCode(mm_1);
			//Utils.debug("id_1 : "+id_1);
			
			// Iterate of all models in registry database
			Iterator<ModelMatch> iterator_2 = fromRegistry.iterator();
			while (iterator_2.hasNext()) {
				
				String id_2 = computeModelMatchHashCode(iterator_2.next());
				//Utils.debug("id_2 : "+id_2);

				// Could match 'registry model' with 'query model'
				// All models that could be matched will not be changed !
				if (id_1.equals(id_2)) {
					matchings++;
					iterator_1.remove();
					iterator_2.remove();
					//-+---copyModelSelectionParameter()
					//Utils.debug("matching !");
					//Utils.debug(id_2);
					break;
				}
			}
		}
		
		
		// Print statistics of matched models
		Utils.debug(matchings);
		int rest_1 = fromQuery.size();
		int rest_2 = fromRegistry.size();
		Utils.debug("From query    : "+rest_1);
		Utils.debug("From registry : "+rest_2);
		
		Utils.debug("Unmatched models : ");
		for (ModelMatch mm : fromQuery) {
			Utils.debug("From query    : "+computeModelMatchHashCode(mm));
		}
		for (ModelMatch mm : fromRegistry) {
			Utils.debug("From registry : "+computeModelMatchHashCode(mm));
		}
		
		
		
		/*
		 *  Update unmatched models in the registry database
		 */
		updateUnmatchedModelMatchings(resourceInfo, fromQuery, fromRegistry, result);
		
		
		// Return boolean value that is used for statistic of matched and unmatched models
		if (rest_1 == 0 && rest_2 == 0) return true;
		return false;
	}
	
	
	public static String computeModelMatchHashCode(ModelMatch mm) {
		
		String result = "";
		result+=mm.getModelType().name()+"#";
		result+=mm.getDifferentHitTypes()+"#";
		result+=mm.getDifferentHitTypes()+"#";
		result+=mm.getHitCountTotal()+"#";
		result+=mm.getExclusiveHitCountTotal()+"#";
		//result+=mm.getDetectionMethod().name()+"#";
		//result+=mm.getDetectionSource()+"#";
		result+=mm.getConllColumn()+"#";
		result+=mm.getXmlAttribute()+"#";
		result+=mm.getRdfProperty()+"#";
		result+=mm.getCoverage()+"#";
		//result+=mm.isSelected()+"#";
			
		return result;
	}
	
	private String computeModelMatchHashCodeWithoutModelType(ModelMatch mm) {
		
		String result = "";
		//result+=mm.getModelType().name()+"#";
		result+=mm.getDifferentHitTypes()+"#";
		result+=mm.getDifferentHitTypes()+"#";
		result+=mm.getHitCountTotal()+"#";
		result+=mm.getExclusiveHitCountTotal()+"#";
		//result+=mm.getDetectionMethod().name()+"#";
		//result+=mm.getDetectionSource()+"#";
		result+=mm.getConllColumn()+"#";
		result+=mm.getXmlAttribute()+"#";
		result+=mm.getRdfProperty()+"#";
		result+=mm.getCoverage()+"#";
		//result+=mm.isSelected()+"#";
			
		return result;
	}
	
	/**
	 * (Patch)
	 * @param sum
	 * @param add
	 */
	private void computeModelMatchSum (ModelMatch sum, ModelMatch add) {
		
		sum.setCoverage(sum.getCoverage()+add.getCoverage());
		sum.setDifferentHitTypes(sum.getDifferentHitTypes()+add.getDifferentHitTypes());
		sum.setExclusiveHitTypes(sum.getExclusiveHitTypes()+add.getExclusiveHitTypes());
		sum.setHitCountTotal(sum.getHitCountTotal()+add.getHitCountTotal());
		sum.setExclusiveHitCountTotal(sum.getExclusiveHitCountTotal()+add.getExclusiveHitCountTotal());
		
	}
	
	
		
}	
