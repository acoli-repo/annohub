package de.unifrankfurt.informatik.acoli.fid.owl;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.jena.riot.RDFDataMgr;

import de.unifrankfurt.informatik.acoli.fid.exec.Executer;
import de.unifrankfurt.informatik.acoli.fid.gremlinQuery.EmbeddedQuery;
import de.unifrankfurt.informatik.acoli.fid.search.GWriter;
import de.unifrankfurt.informatik.acoli.fid.search.GraphTools;
import de.unifrankfurt.informatik.acoli.fid.search.StreamReaderBLLModel;
import de.unifrankfurt.informatik.acoli.fid.search.StreamReaderModel;
import de.unifrankfurt.informatik.acoli.fid.spider.DownloadManager;
import de.unifrankfurt.informatik.acoli.fid.types.ModelInfo;
import de.unifrankfurt.informatik.acoli.fid.types.ModelType;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceFormat;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;

/**
 * Functions for maintaining the Ontology class graph
 * @author frank
 *
 */
public class OntologyManager {

	private Executer executer;
	private GWriter writer;
	private EmbeddedQuery queries;
	private StreamReaderModel modelParser;
	private StreamReaderBLLModel modelParserBLL;
	private DownloadManager downloadManager;
	private ResultUpdater resultUpdater;
	private Integer workerId;
	private XMLConfiguration fidConfig;
	private ModelDefinition modelDefinition;

	
	public OntologyManager (Executer executer, DownloadManager downloadManager, XMLConfiguration fidConfig, ModelDefinition modelDefinition) {
		
		this.executer = executer;
		this.writer = executer.getWriter();
		this.queries = new EmbeddedQuery(writer.getGraph());
		this.modelParser = new StreamReaderModel(writer);
		this.modelParserBLL  = new StreamReaderBLLModel(writer);
		this.fidConfig = fidConfig;
		this.downloadManager = downloadManager;
		this.workerId = 0;
		this.resultUpdater = new ResultUpdater(executer);
		this.modelDefinition = modelDefinition;	
	}
	
	
	
	/**
	 * Initialize model database
	 */
	public void initModelGraph() {
		
		reloadAllOliaModels();
	}
	
	
	/**
	 * Update all OLiA models
	 * @param newOrChangedModels
	 * @return error message or empty
	 */
	public String updateOliaModels (HashSet<ModelType> newOrChangedModels_) {
		
		/*
		 *  U P D A T E
		 *  
		 *    O L I A 
		 *  
		 *  M O D E L S
		 *
		 */
		
		HashSet<ModelType> newOrChangedModels;
		
		System.out.println("updateAllModels");
		
		// **********************************************************
		// 1. Check for changed or new models in all model group
		
		if (newOrChangedModels_ == null) {
			newOrChangedModels = checkUpdatedModels(); //new HashSet<ModelType>();
		} else {
			newOrChangedModels = newOrChangedModels_;
		}
			
		if (fidConfig.getString("OWL.modelUpdateMode").toLowerCase().equals("force")){
			
				System.out.println("***************************************************");
				System.out.println("Force OLiA model update - starting update process !");
				System.out.println("***************************************************");
			
		} else {
		
			if (newOrChangedModels.size() == 0) {
				System.out.println("***************************************");
				System.out.println("OLiA models unchanged - nothing to do !");
				System.out.println("***************************************");
	
				return"Model definitions unchanged - nothing to do !";
			} else {
				System.out.println("One or more OLiA models have changed - starting update process !");
			}
		}
		
		
		
		if (newOrChangedModels.contains(ModelType.valueOf("BLL"))) {
			
				if (fidConfig.getBoolean("RunParameter.useBllOntologiesFromSVN")) {
					writer.getBllTools().updateBLLFilesFromSVN();
				}
				
				/*// If only BLL files were changed then do not update
				if (newOrChangedModels.size() == 1) {
					System.out.println("Only BLL model was changed - nothing to do !");
					return;
				}*/
		}
		
		
		// **********************************************************
		// 2. Delete all vertices from model graph that
		//    are not HIT vertices or have a HIT vertex as a neighbor
		Utils.debug("reset model graph");
		queries.resetModelGraph();
		
		writer.restoreVertexMapEdgeMap();
		Utils.debug("reset model graph finished");
		
		// Restore hit edges for update process
		writer.setHitEdgeKeys(GraphTools.getOldHitEdges(queries));
		
		
		
		// **********************************************************
		// 3. Reload all models			
		reloadAllOliaModels();
		

		// **********************************************************
		// 4. Remove all orphan HIT vertices from model graph that
		//    do not connect to any model after models have been
		//    updated
		Utils.debug("remove orphan hits");
		queries.removeOrphanHits();
		Utils.debug("remove orphan hits finished");
		
		ArrayList<ResourceInfo> rfl = new ArrayList<ResourceInfo>();
		rfl.addAll(executer.getResourceManager().getAllResourceFilesRI());
		
		// 4. Reconnect hits to updated or new models
		Utils.debug("reconnectHits");
		resultUpdater.reconnectHits();
		Utils.debug("reconnectHits finished");
		
		// 5. Try previously unmatched tags
		resultUpdater.reconnectUnmatchedConllTagsAndFeatures(rfl);
		
		// *******************************************************
		// 6. Update results & update file unit info
		resultUpdater.updateModelResultsAfterOliaUpdate(rfl);
		
		// removed for start from gui
		//executer.closeDBConnections();
		return "";
	}
	
	/**
	 * Check if models have been updated or added
	 * @return Models that are new or have been changed
	 */
	public HashSet<ModelType> checkUpdatedModels() {
		
		HashSet<ModelType> newOrChangedModels = new HashSet<ModelType>();
		
		for (ModelType modelType : modelDefinition.getModelDefinitions().keySet()) {
			
			System.out.println("model group : "+modelType.name());
			
			for (ModelInfo model : modelDefinition.getModelDefinitions().get(modelType).getModelFiles()) {

				if (!model.isActive()) continue;
				//if (!model.isActive() && !ModelGroupsNew.useAllModels) continue;
				
				String dataUrl = model.getUrl().toString();
				System.out.println("dataUrl "+dataUrl);
				String metadataUrl = "";
				System.out.println("checking model "+ dataUrl);
				
				// Download all updated models of model group
				ResourceInfo resourceInfo = new ResourceInfo(dataUrl, metadataUrl, "http://linghub/dummy/dataset", ResourceFormat.ONTOLOGY);
				downloadManager.getResource(workerId, resourceInfo, false, false);

				if (resourceInfo.getResource() != null) {					
					newOrChangedModels.add(modelType);
				}
			}		
		}
		
		return newOrChangedModels;
	}
	
	
	/**
	 * Check if models have been updated or added
	 * @return Models that are new or have been changed
	 */
	public HashSet<ModelType> checkUpdatedModels(List<ModelInfo> modelInfoList) {
		
		HashSet<ModelType> newOrChangedModels = new HashSet<ModelType>();
			
			for (ModelInfo model : modelInfoList) {

				if (!model.isActive()) continue;
				//if (!model.isActive() && !ModelGroupsNew.useAllModels) continue;
				
				String dataUrl = model.getUrl().toString();
				System.out.println("dataUrl "+dataUrl);
				String metadataUrl = "";
				System.out.println("checking model "+ dataUrl);
				
				// Download all updated models of model group
				ResourceInfo resourceInfo = new ResourceInfo(dataUrl, metadataUrl, "http://linghub/dummy/dataset", ResourceFormat.ONTOLOGY);
				downloadManager.getResource(workerId, resourceInfo, false, false);

				if (resourceInfo.getResource() != null) {					
					newOrChangedModels.add(model.getModelType());
				}
			}		
		
		return newOrChangedModels;
	}
	
	

	private void reloadAllOliaModels() {
		
		for (ModelType modelType : modelDefinition.getModelDefinitions().keySet()) {
			
			// Load models in model-group
			Utils.debug("Updating model group "+ modelType.toString());
			
			// Reload all model files from model group
			for (ModelInfo model : modelDefinition.getModelDefinitions().get(modelType).getModelFiles()) {
				
				// Download all updated models of model group
				ResourceInfo resourceInfo = new ResourceInfo(model.getUrl().toString(), "", "http://linghub/dummy/dataset", ResourceFormat.ONTOLOGY);
				resourceInfo.getFileInfo().setForceRescan(true);
				downloadManager.getResource(workerId, resourceInfo, false, false);
				
				// TODO load all BLL models (ontology, link, language-link) 
				// (now only link is loaded)
								
				if (!model.isActive()) continue;

				loadOliaModel(model);
			}
		}
		
		// update models by namespace
		writer.updateModelsByNamespaces(
				modelDefinition.getModels2TagNamespaces(),
				modelDefinition.getModels2ClassNamespaces());

		// Update cache with actual tags and classes 
		writer.getAnnotationCache().update();
	}
	

	
	/**
	 * Load model
	 * @param model
	 */
	private void loadOliaModel(ModelInfo model) {
			
		if (!model.isActive()) return;
		//if (!model.isActive() && !ModelGroupsNew.useAllModels) return;

				
		try {
			
			String filePath = null;
			if (model.getUrl().getProtocol().equals("file")) {
				filePath = model.getUrl().getFile();
			} else {
				filePath = new File(new File(downloadManager.getDownloadFolder(), workerId.toString()), model.getFileName()).getAbsolutePath();
			}
			
			Utils.debug("Loading model "+filePath);
			URL url = new URL("file://"+filePath);
			
			
			//if (!model.getName().contains("bll")) {
			if (!(model.getModelType().equals(ModelType.valueOf("BLL")))) {
				modelParser.reset();
				modelParser.setModelInfo(model);
				RDFDataMgr.parse(modelParser, url.getPath());
			} else {
				modelParserBLL.reset();
				modelParserBLL.setModelInfo(model);
				RDFDataMgr.parse(modelParserBLL, url.getPath());
			}
		
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}
	
	/**
	 * Commit transactions, etc.
	 */
	public void finish() {
		writer.finish();
	}



	public DownloadManager getDownloadManager() {
		return downloadManager;
	}

}
