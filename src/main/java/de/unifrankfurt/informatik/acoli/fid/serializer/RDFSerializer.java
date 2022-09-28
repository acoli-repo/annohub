package de.unifrankfurt.informatik.acoli.fid.serializer;


import static de.unifrankfurt.informatik.acoli.fid.util.Utils.sha256;

import java.io.StringWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import de.unifrankfurt.informatik.acoli.fid.gremlinQuery.BllTools;
import de.unifrankfurt.informatik.acoli.fid.owl.ModelDefinition;
import de.unifrankfurt.informatik.acoli.fid.parser.ParserISONames;
import de.unifrankfurt.informatik.acoli.fid.search.ClassMatrixParser;
import de.unifrankfurt.informatik.acoli.fid.types.BLLConcept;
import de.unifrankfurt.informatik.acoli.fid.types.DetectionSource;
import de.unifrankfurt.informatik.acoli.fid.types.ProcessingFormat;
import de.unifrankfurt.informatik.acoli.fid.types.FileResult;
import de.unifrankfurt.informatik.acoli.fid.types.LanguageMatch;
import de.unifrankfurt.informatik.acoli.fid.types.MetadataSource;
import de.unifrankfurt.informatik.acoli.fid.types.ModelInfo;
import de.unifrankfurt.informatik.acoli.fid.types.ModelMatch;
import de.unifrankfurt.informatik.acoli.fid.types.ModelType;
import de.unifrankfurt.informatik.acoli.fid.types.ModelUsage;
import de.unifrankfurt.informatik.acoli.fid.types.ProcessState;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceFormat;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceType;
import de.unifrankfurt.informatik.acoli.fid.util.AnnotationUtil;
import de.unifrankfurt.informatik.acoli.fid.util.IndexUtils;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;


public class RDFSerializer {

	static final String rdf_ = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	static final String rdfs_ = "http://www.w3.org/2000/01/rdf-schema#";
	static final String owl_ = "http://www.w3.org/2002/07/owl#";
	static final String skos_ = "http://www.w3.org/2004/02/skos/core#";
	static final String dct_ = "http://purl.org/dc/terms/";
	static final String dc_ = "http://purl.org/dc/elements/1.1/";
	static final String dcat_ = "http://www.w3.org/ns/dcat#";
	static final String lexvo_ = "http://lexvo.org/ontology#";
	static final String annohub_ = "http://acoli.cs.uni-frankfurt.de/annohub#";
	static final String prov_ = "http://www.w3.org/ns/prov#";
	static final String rbook_ = "http://www.resourcebook.eu/lremap/owl/lremap_resource.owl#";
	static final String mshare_ = "http://purl.org/ms-lod/MetaShare.ttl#";
	static final String vcard_ = "http://www.w3.org/2006/vcard/ns#";
	static final String base_ = "";
	//static Model model;
	
	static final String annoHubBaseUrl = "https://annohub.linguistik.de";

	
	// AnnoHub DCAT catalog definition 
	static final String headerDescription=""
			+ "This metadata repository contains language and annotation metadata of language resources "
			+ "such as corpora, lexicons, and dictionaries. The information about the object"
			+ " language and the applied annotation models (tag sets) is generated in an"
			+ " automated manner applying the routines and tools established as part of the"
			+ " AnnoHub workflow. Some of the basic formal metadata originate from portals "
			+ "such as LingHub (http://linghub.org/) or CLARIN VLO (https://vlo.clarin.eu/).";
	static final String headerTitle="AnnoHub language and annotation metadata collection";
	static final String headerRights="CC-BY";
	static final String headerPublisher="University Library J. C. Senckenberg, Frankfurt/Main, Germany";
	static final String headerIsReferencedBy="Abromeit, Frank & Chiarcos, Christian (2019)."
			+ " Automatic Detection of Language and Annotation Model Information in CoNLL Corpora. "
			+ "In Proceedings of the 2nd Conference on Language, Data and Knowledge (LDK 2019). "
			+ "Leipzig, Germany, 23:1-23:9. DOI: 10.4230/OASIcs.LDK.2019.0";
	static final String headerIssuedDate ="2020-03-30";
	static final String headerModifiedDate = LocalDate.now().toString();
	static final String linguistikdeEmail = "info@linguistik.de";
	//https://www.w3.org/TR/vocab-dcat-2/#Property:resource_release_date
	
	
	
	public static Model serializeResourceInfo2RDFModelIntern(ArrayList <ResourceInfo> resourceInfoList, BllTools bllTools, XMLConfiguration fidConfig, ModelDefinition modelDefinition) {
		return serializeResourceInfo2RDFModel (resourceInfoList, bllTools, fidConfig, false, modelDefinition);
	}
	
	
	public static Model serializeResourceInfo2RDFModelExtern(ArrayList <ResourceInfo> resourceInfoList, BllTools bllTools, XMLConfiguration fidConfig, ModelDefinition modelDefinition) {
		return serializeResourceInfo2RDFModel (resourceInfoList, bllTools, fidConfig, true, modelDefinition);
	}
	
	
	/**
	 * Produce RDF model for UB-Export
	 * @param resourceInfoList
	 * @param bllTools
	 * @param fidConfig
	 * @return Model
	 */
	private static Model serializeResourceInfo2RDFModel (
			ArrayList <ResourceInfo> resourceInfoList,
			BllTools bllTools,
			XMLConfiguration fidConfig,
			boolean publicVersion,
			ModelDefinition modelDefinition
			) {
		
		ClassMatrixParser bllMatrixParser;
		HashMap<String, String> bllLanguageMap = null;
		HashMap<String, HashSet<String>> bllModelMap = null;
		
		// initialize bll tools
		if (bllTools != null && fidConfig != null && bllModelMap == null) {
			bllModelMap = bllTools.makeBllMap(); // also generates BllMatrixParser + BllLanguageLinkMap
			bllMatrixParser = bllTools.getBllMatrixParser();
			bllLanguageMap = bllTools.getBllLanguageLinkMap();
		}
		
		
		Utils.debug("serializeResourceInfo2RDFModel");
		
		Model model = ModelFactory.createDefaultModel();
		OntModel ontModel = ModelFactory.createOntologyModel();
		
		// https://www.w3.org/TR/vocab-dcat/
		// http://dublincore.org/specifications/dublin-core/dcmi-terms/
		// http://dublincore.org/specifications/dublin-core/dcmi-terms/2012-06-14/?v=terms#isVersionOf
		// https://www.w3.org/TR/2013/REC-prov-o-20130430/#wasGeneratedBy
		// https://www.w3.org/TR/2013/REC-prov-o-20130430/#wasDerivedFrom
		// https://www.w3.org/TR/2013/REC-prov-o-20130430/#hadPrimarySource
		// https://www.w3.org/TR/2013/NOTE-prov-primer-20130430/
		
		
		// Define prefix names for used namespaces
		model.setNsPrefix("rdf",rdf_);
		model.setNsPrefix("rdfs",rdfs_);
		model.setNsPrefix("owl",owl_);
		model.setNsPrefix("skos",skos_);
		model.setNsPrefix("dc",dc_);
		model.setNsPrefix("dct",dct_);
		model.setNsPrefix("dcat",dcat_);
		model.setNsPrefix("lexvo",lexvo_);
		model.setNsPrefix("annohub",annohub_);
		model.setNsPrefix("prov",prov_);
		model.setNsPrefix("rbook",rbook_);
		model.setNsPrefix("vcard",vcard_);
		
		
		
		// General classes and properties
		OntClass vcardOrganization = ontModel.createClass(vcard_+"Organization");
		OntClass vcardIndividual = ontModel.createClass(vcard_+"Individual");
		Property vcardHasEmail = ontModel.createProperty(vcard_+"hasEmail");
		Property vcardFormattedName = ontModel.createProperty(vcard_+"fn");

		
		Property a = ontModel.createProperty(rdf_+"type");
		OntClass owlOntology = ontModel.createClass(owl_+"Ontology");
		
		Property dctDateAccepted = ontModel.createProperty(dct_+"dateAccepted");
		Property dctFileFormat = ontModel.createProperty(dct_+"fileFormat");
		Property dctFormat = ontModel.createProperty(dct_+"format");
		Property dctReferences = ontModel.createProperty(dct_+"references");
		Property dctSubject = ontModel.createProperty(dct_+"subject");
		Property dctHasPart = ontModel.createProperty(dct_+"hasPart");
		Property dctIsPartOf = ontModel.createProperty(dct_+"isPartOf");
		Property dctBytesSize = ontModel.createProperty(dct_+"bytesSize");
		
		OntClass dcatCatalog = ontModel.createClass(dcat_+"Catalog");
		OntClass dcatDataset = ontModel.createClass(dcat_+"Dataset");
		Property dcatdataset = ontModel.createProperty(dcat_+"dataset");
		Property dcatdistribution = ontModel.createProperty(dcat_+"distribution");
		OntClass dcatDistribution = ontModel.createClass(dcat_+"Distribution");
		
		Property lexvolanguage = ontModel.createProperty(lexvo_+"language");
		Property lexvoIso639P3Code = ontModel.createProperty(lexvo_+"iso639P3Code");
		OntClass lexvoLanguage = ontModel.createClass(lexvo_+"Language");
		
		Property references = ontModel.createProperty(dct_+"references");
		Property isVersionOf = ontModel.createProperty(dct_+"isVersionOf");
		
		Property wasGeneratedBy = ontModel.createProperty(prov_+"wasGeneratedBy");
		Property wasDerivedFrom = ontModel.createProperty(prov_+"wasDerivedFrom");
		OntClass provEntity = ontModel.createClass(prov_+"Entity");
		
		Property comment = ontModel.createProperty(rdfs_+"comment");
		Property label = ontModel.createProperty(rdfs_+"label");
		Property isDefinedBy = ontModel.createProperty(rdfs_+"isDefinedBy");
		


		// Linghub key properties
		Property dcTitle = ontModel.createProperty(dc_+"title");
		Property dcRights = ontModel.createProperty(dc_+"rights");
		Property dcCreator = ontModel.createProperty(dc_+"creator");
		Property dcSource = ontModel.createProperty(dc_+"source");
		Property dcContributor = ontModel.createProperty(dc_+"contributor");
		Property dcPublisher = ontModel.createProperty(dc_+"publisher");
		Property dcSubject = ontModel.createProperty(dc_+"subject");
		Property dcDescription = ontModel.createProperty(dc_+"description");
		Property dctLanguage = ontModel.createProperty(dct_+"language");
		Property dctType = ontModel.createProperty(dct_+"type");
		Property dctIsReferencedBy = ontModel.createProperty(dct_+"isReferencedBy");
		Property dctIssued = ontModel.createProperty(dct_+"issued");
		Property dctModified = ontModel.createProperty(dct_+"modified");
		Property dctDescription = ontModel.createProperty(dct_+"description");
		Property dcatContactPoint = ontModel.createProperty(dcat_+"contactPoint");
		Property dcatAccessURL = ontModel.createProperty(dcat_+"accessURL");
		Property mshareEmail = ontModel.createProperty(mshare_+"email");
		
		// new
		Property dcLicense = ontModel.createProperty(dc_+"license");
		Property dctSource = ontModel.createProperty(dct_+"source");
		Property dctIdentifier = ontModel.createProperty(dct_+"identifier");


		
		
		// Annohub classes
		OntClass AnnotationScheme = ontModel.createClass(annohub_+"AnnotationScheme");
		OntClass Annotation = ontModel.createClass(annohub_+"Annotation");
		OntClass AnnohubLanguage = ontModel.createClass(annohub_+"Language");

		// Annohub properties
		Property analysis = ontModel.createProperty(annohub_+"analysis");
		Property contains = ontModel.createProperty(annohub_+"contains");
		Property column = ontModel.createProperty(annohub_+"column");
		Property xmlAttribute = ontModel.createProperty(annohub_+"xmlAttribute");
		Property rdfProperty = ontModel.createProperty(annohub_+"rdfProperty");
		Property confidence = ontModel.createProperty(annohub_+"confidence");
		Property coverage = ontModel.createProperty(annohub_+"coverage");
		Property method = ontModel.createProperty(annohub_+"method");
		Property count = ontModel.createProperty(annohub_+"count");
		Property hitCount = ontModel.createProperty(annohub_+"count");
		Property exclusiveHitCount = ontModel.createProperty(annohub_+"exclusiveHitCount");	
		Property exclusiveHitTypes = ontModel.createProperty(annohub_+"exclusiveHitTypes");	
		Property hitTypes = ontModel.createProperty(annohub_+"hitTypes");	
		Property matched = ontModel.createProperty(annohub_+"matched");
		Property unmatched = ontModel.createProperty(annohub_+"unmatched");
		Property unmatchedCount = ontModel.createProperty(annohub_+"unmatchedCount");
		Property annotatedTag = ontModel.createProperty(annohub_+"annotatedTag");
		Property annotatedClass = ontModel.createProperty(annohub_+"annotatedClass");
		Property matchedTag = ontModel.createProperty(annohub_+"matchedTag");
		Property matchedClass = ontModel.createProperty(annohub_+"matchedClass");
		Property isMatched = ontModel.createProperty(annohub_+"isMatched");
		Property hasBllConcept = ontModel.createProperty(annohub_+"hasBllConcept");
		Property hasBllHierarchy = ontModel.createProperty(annohub_+"hasBllHierarchy");
		Property bllHierarchyItem = ontModel.createProperty(annohub_+"bllHierarchyItem");
		Property fileFormat = ontModel.createProperty(annohub_+"fileFormat");
		Property ubTitle = ontModel.createProperty(annohub_+"ubTitle");
		Property metadataUrl = ontModel.createProperty(annohub_+"metadataUrl");
		
		
		// TODO alternativly use corpus, lexicon and ontology classes defined in :
		// https://raw.githubusercontent.com/martavillegas/metadata/master/MetaShare.ttl
		OntClass ResourceBookCorpus = ontModel.createClass(rbook_+"Corpus");
		OntClass ResourceBookLexicon = ontModel.createClass(rbook_+"Lexicon");
		OntClass ResourceBookOntology = ontModel.createClass(rbook_+"Ontology");
		HashMap <ResourceType,OntClass> resourceTypeMap = new HashMap <ResourceType,OntClass>();
		resourceTypeMap.put(ResourceType.CORPUS, ResourceBookCorpus);
		resourceTypeMap.put(ResourceType.LEXICON, ResourceBookLexicon);
		resourceTypeMap.put(ResourceType.ONTOLOGY, ResourceBookOntology);

		HashMap<ProcessingFormat,String> fileFormatMap = new HashMap<ProcessingFormat,String>();
		fileFormatMap.put(ProcessingFormat.CONLL, "application/rdf+xml");
		fileFormatMap.put(ProcessingFormat.RDF, "application/rdf+xml");
		fileFormatMap.put(ProcessingFormat.XML, "text/xml");
		HashMap<ResourceFormat,String> resourceFormatMap = new HashMap<ResourceFormat,String>();
		resourceFormatMap.put(ResourceFormat.CONLL, "application/rdf+xml");
		resourceFormatMap.put(ResourceFormat.RDF, "application/rdf+xml");
		resourceFormatMap.put(ResourceFormat.XML, "text/xml");

		
		// Metadata properties
		String baseCatalogURL = annoHubBaseUrl+"/catalog";
		String baseDatasetURL = annoHubBaseUrl+"/resource/";
		String baseDistributionURL = annoHubBaseUrl+"/distribution/";
		String baseAnalysisURL = annoHubBaseUrl+"/analysis/";
		
		
		Resource catalog = model.createResource(baseCatalogURL);
		Resource linguistikde = model.createResource(baseCatalogURL+"/contactPoint");
		catalog.addProperty(a, dcatCatalog);
		catalog.addProperty(dcTitle, headerTitle);
		catalog.addProperty(dctDescription, headerDescription);
		catalog.addProperty(dctIsReferencedBy, headerIsReferencedBy);
		catalog.addProperty(dcatContactPoint, linguistikde);
		catalog.addProperty(dctIssued, headerIssuedDate);
		catalog.addProperty(dctModified, headerModifiedDate);
		catalog.addProperty(dcPublisher, headerPublisher);
		catalog.addProperty(dcRights, headerRights);
		linguistikde.addProperty(a, vcardOrganization);
		linguistikde.addProperty(vcardHasEmail, linguistikdeEmail);
		
		
		
		// Stores <datsetURL,[datasetObj,distributionObj1,...,distributionObjn]>
		HashMap <String, ArrayList<Resource>> doneDatasets = new HashMap <String, ArrayList<Resource>> ();
		HashMap <String, HashSet<String>> datasetLanguages = new HashMap <String, HashSet<String>> ();
		HashSet <String> doneLanguages = new HashSet <String> ();
		HashSet <String> doneModels = new HashSet <String> ();
		
		String datasetURL = "";
		String distroURL = "";
		String analysisURL = "";
		int langId = 0;
		int modelId = 0;
		int resultId = 0;
		String bllKey = "";
		
		int i = 0;
		boolean found;
		boolean isXML; // fixes missing XML state
		for (ResourceInfo rs : resourceInfoList) {
			
			if (rs.getFileInfo().getProcessState() != ProcessState.ACCEPTED) continue;
			
			// TODO Broken resources are filtered in LoginBean.exportRDFImpl() !
			/*if (!fidConfig.getBoolean("RunParameter.exportBrokenLinks") &&
				rs.getResourceState() == ResourceState.ResourceUrlIsBroken) {
					Utils.debug("Skipping resource file because resource URL is broken !");
					Utils.debug(rs.getDataURL()+"->"+rs.getFileInfo().getRelFilePath());
					continue;
			}*/
			
			Resource linghubDistro = null;
			Resource mainDataset = null;
			
			datasetURL = baseDatasetURL+sha256(rs.getDataURL());
			distroURL = datasetURL+"/distribution/";
			analysisURL = datasetURL+"/file/"+sha256(rs.getFileInfo().getRelFilePath())+"/analysis";
			
		
			if (!doneDatasets.keySet().contains(datasetURL)) {
			
				// Create main dataset
				mainDataset = model.createResource(datasetURL);
				catalog.addProperty(dcatdataset, mainDataset);
				
				mainDataset.addProperty(a, dcatDataset);
				ArrayList <Resource> resources = new ArrayList<Resource>();
				resources.add(mainDataset);
				doneDatasets.put(datasetURL, resources);
				
				mainDataset.addProperty(a, dcatDataset);
				
				// Where does the metadata come from ?
				if (rs.getResourceMetadata().getMetadataSource() != MetadataSource.NONE) {
					
					// add comment for metadate source
					mainDataset.addProperty(comment,"Metadata generated from "+rs.getResourceMetadata().getMetadataSource());
					
					if (rs.getResourceMetadata().getMetadataSource() == MetadataSource.LINGHUB) {
						mainDataset.addProperty(metadataUrl,rs.getMetaDataURL());
					}
					
					// additionally add linghub/Clarin metadata URL
					/*if (!rs.getMetaDataURL().equals(ResourceManager.MetaDataToBeClarified) &&
						!rs.getMetaDataURL().equals(ResourceManager.MetaDataFromUser))
						mainDataset.addProperty(isVersionOf,rs.getMetaDataURL());*/
				}
				
				// Linghub key metadata properties provided here
				// dcRights dcCreator dcSource dcContributor dcSubject dcDescription
				// dctLanguage dcatContactPoint
				
				// title
				if (!Utils.filterNa(rs.getResourceMetadata().getTitle()).isEmpty()) {
					mainDataset.addProperty(dcTitle, rs.getResourceMetadata().getTitle());
				}
				
				// UB title
				if (!publicVersion) {
				if (!rs.getResourceMetadata().getUbTitle().isEmpty()) {
					mainDataset.addProperty(ubTitle, rs.getResourceMetadata().getUbTitle());
				} else {
					// copy title to ubTitle
					if (!Utils.filterNa(rs.getResourceMetadata().getTitle()).isEmpty()) {
						mainDataset.addProperty(ubTitle, rs.getResourceMetadata().getTitle());
					}
				}
				}
				
				if (!Utils.filterNa(rs.getResourceMetadata().getLicense()).isEmpty()) {
					mainDataset.addProperty(dcLicense, rs.getResourceMetadata().getLicense());
				}
				if (!Utils.filterNa(rs.getResourceMetadata().getRights()).isEmpty()) {
					mainDataset.addProperty(dcRights, rs.getResourceMetadata().getRights());
				}
				if (!Utils.filterNa(rs.getResourceMetadata().getDctIdentifier()).isEmpty()) {
					mainDataset.addProperty(dctIdentifier, rs.getResourceMetadata().getDctIdentifier());
				}
				if (!Utils.filterNa(rs.getResourceMetadata().getDctSource()).isEmpty()) {
					mainDataset.addProperty(dctSource, rs.getResourceMetadata().getDctSource());
				}
				if (!rs.getResourceMetadata().getCreatorList().isEmpty()) {
					for (String c : rs.getResourceMetadata().getCreatorList()) {
						if (!c.trim().isEmpty()) mainDataset.addProperty(dcCreator, c.trim());
					}
				}
				if (!rs.getResourceMetadata().getContributorList().isEmpty()) {
					for (String c : rs.getResourceMetadata().getContributorList()) {
						if (!c.trim().isEmpty()) mainDataset.addProperty(dcContributor, c.trim());
					}
				}
				if (!rs.getResourceMetadata().getPublisherList().isEmpty()) {
					for (String c : rs.getResourceMetadata().getPublisherList()) {
						if (!c.trim().isEmpty()) mainDataset.addProperty(dcPublisher, c.trim());
					}
				}
				
				if (rs.getFileInfo().getResourceType() != null) {
					if (resourceTypeMap.containsKey(rs.getFileInfo().getResourceType())) {
						mainDataset.addProperty(dctType, resourceTypeMap.get(rs.getFileInfo().getResourceType()));
					}
				}
				for (String subject : rs.getResourceMetadata().getKeywords().split(",")) {
					if (!subject.isEmpty()) mainDataset.addProperty(dcSubject, subject);
				}
				if (!Utils.filterNa(rs.getResourceMetadata().getDescription()).isEmpty())
					mainDataset.addProperty(dcDescription, rs.getResourceMetadata().getDescription());
				
				// initialize set for dct:language (prevents double language entries)
				if (!datasetLanguages.containsKey(datasetURL)) {
					datasetLanguages.put(datasetURL,new HashSet<String>());
				}
				
				// optional
				/*if (!rs.getFileInfo().getSelectedLanguages().isEmpty() &&
					!datasetLanguages.containsKey(datasetURL)) {
						datasetLanguages.put(datasetURL,new HashSet<String>());
				}
				for (LanguageMatch lm : rs.getFileInfo().getSelectedLanguages()) {
					if (!datasetLanguages.get(datasetURL).contains(lm.getLanguageISO639Identifier())) {
						mainDataset.addProperty(dctLanguage, model.createResource(lm.getLexvoUrl().toString()));
						HashSet<String> languages = datasetLanguages.get(datasetURL);
						languages.add(lm.getLanguageISO639Identifier());
						datasetLanguages.put(datasetURL, languages);
					}
				}*/
				
				if (!Utils.filterNa(rs.getResourceMetadata().getEmailContact()).isEmpty()) {
					Resource contactPoint = model.createResource(datasetURL+"/"+"contactPoint");
					mainDataset.addProperty(dcatContactPoint, contactPoint);
					contactPoint.addProperty(a, vcardIndividual);
					contactPoint.addProperty(vcardHasEmail, rs.getResourceMetadata().getEmailContact());
					//mainDataset.addProperty(dcatContactPoint, model.createResource(contactPoint));
					//contactPoint.addProperty(mshareEmail, rs.getLinghubAttributes().getEmailContact());
				}
				
				if (!rs.getHttpContentType().isEmpty()) {
					mainDataset.addProperty(dctFileFormat, rs.getHttpContentType());
				}
				
			
			} else {
				mainDataset = doneDatasets.get(datasetURL).get(0);
			}
			
			// Check if distribution is new
			i = 0;found = false;
			for (Resource x : doneDatasets.get(datasetURL)) {
				if (i++ == 0) continue; // skip first entry (main dataset resource)
				if (x.getURI().equals(datasetURL)) {
					linghubDistro = x;
					found = true;break;
				}
			}
			if (!found) {
				
				// Create a dcat distribution and connect it to the main dataset
				linghubDistro = model.createResource(distroURL);
				linghubDistro.addProperty(a, dcatDistribution);
				linghubDistro.addProperty(dcatAccessURL, model.createResource(rs.getDataURL()));
				linghubDistro.addLiteral(dctBytesSize, rs.getHttpContentLength().toString());
				if (!Utils.filterNa(rs.getResourceMetadata().getRights()).isEmpty())
					linghubDistro.addProperty(dcRights, rs.getResourceMetadata().getRights());
				// Connect distribution to dataset
				mainDataset.addProperty(dcatdistribution, linghubDistro);


				// Update doneDatasets
				ArrayList <Resource> resources = doneDatasets.get(datasetURL);
				resources.add(linghubDistro);
				doneDatasets.put(datasetURL,resources);
			}
			
			// For each file included in the distribution (e.g. file of a zip-archive) : 
			// Create a separate dataset which is then assigned to the corresponding linghub dataset via
			// the dct:hasPart property
			
			String relFilePath = rs.getFileInfo().getRelFilePath();
			isXML = false;
			if (!relFilePath.isEmpty()) {
			
				// Create sub dataset for each resource file
				Resource dataset = model.createResource(datasetURL+"/file/"+sha256(relFilePath));
				dataset.addProperty(a, dcatDataset);
				dataset.addProperty(dctDateAccepted, rs.getFileInfo().getAcceptedDate().toString());
				dataset.addProperty(dcSource, rs.getFileInfo().getRelFilePath());
				dataset.addProperty(dctIsPartOf, mainDataset);
				//dataset.addProperty(dcFormat, rs.getFileInfo().getFileFormat().name());
				mainDataset.addProperty(dctHasPart, dataset);
				

				
				/******************************
				 * Add all results to dataset *
				 ******************************/
				
				langId = 1;
				for (LanguageMatch lm : rs.getFileInfo().getSelectedLanguages()) {
					
					// add dct:language to main dataset
					if (!datasetLanguages.get(datasetURL).contains(lm.getLanguageISO639Identifier())) {
						mainDataset.addProperty(dctLanguage, model.createResource(lm.getLexvoUrl().toString()));
						HashSet<String> languages = datasetLanguages.get(datasetURL);
						languages.add(lm.getLanguageISO639Identifier());
						datasetLanguages.put(datasetURL, languages);
					}
					
				
					// create language analysis
					Resource annohubLanguage = model.createResource(analysisURL+"/"+(langId++)+"/"+lm.getLanguageISO639Identifier());
					
					// language properties
					annohubLanguage.addProperty(a, AnnohubLanguage);
					annohubLanguage.addProperty(lexvolanguage, model.createResource(lm.getLexvoUrl().toString()));
					annohubLanguage.addProperty(method, lm.getDetectionSource().name());
					annohubLanguage.addProperty(confidence, Float.toString(lm.getAverageProb()));
					
					if (bllLanguageMap != null && bllLanguageMap.containsKey(lm.getLanguageISO639Identifier())) {
						
						// add BLL concept
						annohubLanguage.addProperty(hasBllConcept, model.createResource(bllLanguageMap.get(lm.getLanguageISO639Identifier())));
						
						// (optional)
						// Get language hierarchy 
						/*HashMap<Integer,String> languageClassHierarchy = bllMatrixParser.getLongestLowerPath(bllLanguageMap.get(lm.getLanguageISO639Identifier()));
						HashMap<Integer,String> languageClassHierarchy_ = bllMatrixParser.getLongestUpperPath(bllLanguageMap.get(lm.getLanguageISO639Identifier()));
						for (Integer k : languageClassHierarchy_.keySet()) {
							languageClassHierarchy.put(k, languageClassHierarchy_.get(k));
						}
						
						// Add language hierarchy
						Resource langSequence = model.createResource(annohubLanguage.getURI()+"/hierarchy");
						annohubLanguage.addProperty(hasBllHierarchy, langSequence);

						ArrayList<Integer> keys = new ArrayList <Integer> (languageClassHierarchy.keySet());
						for (int k : keys) {
							langSequence.addProperty(bllHierarchyItem, model.createResource(languageClassHierarchy.get(k)+"/"+k));
						}*/
					}
					
					switch (rs.getFileInfo().getProcessingFormat()) {
					
						case CONLL:
							// fixes missing XML state
							if (!lm.getXmlAttribute().trim().isEmpty()) {
								annohubLanguage.addProperty(xmlAttribute, model.createResource(lm.getXmlAttribute()));
								isXML = true; // fixes missing XML state
							} else {
								annohubLanguage.addProperty(column, Integer.toString(lm.getConllColumn()));
							}
							break;
							
						case RDF:
							if (!lm.getRdfProperty().trim().isEmpty())
								annohubLanguage.addProperty(rdfProperty, model.createResource(lm.getRdfProperty()));
							annohubLanguage.addProperty(count, Long.toString(lm.getHitCount()));
							break;
							
						case XML:
							if (!lm.getXmlAttribute().trim().isEmpty())
								annohubLanguage.addProperty(xmlAttribute, lm.getXmlAttribute());
							// TODO is count available ?
							// annohubLanguage.addProperty(count, Long.toString(lm.getHitCount()));
							break;
						
						default:
							break;
					}
					
					if (lm.getDetectionSource() == DetectionSource.LANGPROFILE)
						annohubLanguage.addProperty(wasGeneratedBy, model.createResource("https://github.com/optimaize/language-detector"));

					// add label with language name @en
					annohubLanguage.addProperty(label, ParserISONames.getIsoCodes2Names().get(lm.getLanguageISO639Identifier()),"en");

					// add language to dataset
					dataset.addProperty(analysis, annohubLanguage);
				}
				
				
				modelId = 0;
				for (ModelMatch mm : rs.getFileInfo().getSelectedModels()) {
					
					if (mm.getModelType().equals(ModelType.valueOf("UNKNOWN"))) continue;
					
					// create model analysis
					Resource annotationScheme = model.createResource(analysisURL+"/"+(++modelId)+"/"+mm.getModelType());
					
					annotationScheme.addProperty(a, AnnotationScheme);
					annotationScheme.addProperty(coverage, Float.toString(mm.getCoverage()));
					annotationScheme.addProperty(hitCount, Long.toString(mm.getHitCountTotal()));
					annotationScheme.addProperty(hitTypes, Long.toString(mm.getDifferentHitTypes()));
					annotationScheme.addProperty(exclusiveHitTypes, Long.toString(mm.getExclusiveHitTypes()));
					annotationScheme.addProperty(exclusiveHitCount, Long.toString(mm.getExclusiveHitCountTotal()));
					annotationScheme.addProperty(method, mm.getDetectionSource().name());
					
					
					switch (rs.getFileInfo().getProcessingFormat()) {
					
					case CONLL: // (real conll | xml->conll | rdf->conll) 
						
						switch (IndexUtils.determineFileFormat(rs)) {
						
							case XML :
								annotationScheme.addProperty(xmlAttribute, mm.getXmlAttribute());
								//isXML = true; // unused !
								break;
								
							case CONLL :
								annotationScheme.addProperty(column, Integer.toString(mm.getConllColumn()));
								break;
								
							case RDF : // TODO add rdf property as it is contained in the RDF file
								annotationScheme.addProperty(column, Integer.toString(mm.getConllColumn()));
								break;
							
							default :
								break;
						
						} break;
						
						/* old // fixes missing XML state
						if (!mm.getXmlAttribute().trim().isEmpty()) { // xml->conll
							annotationScheme.addProperty(xmlAttribute, mm.getXmlAttribute());
							isXML = true; // fixes missing XML state
						} else { // real conll | rdf->conll) 
							annotationScheme.addProperty(column, Integer.toString(mm.getConllColumn()));
						}
						break;*/
						
					case RDF:
						if (!mm.getRdfProperty().trim().isEmpty())
							annotationScheme.addProperty(rdfProperty, model.createResource(mm.getRdfProperty()));
						break;
					
					// never used because getFileFormat() returns processing format (RDF/CONLL) only
					case XML: 
						if (!mm.getXmlAttribute().trim().isEmpty())
							annotationScheme.addProperty(xmlAttribute, mm.getXmlAttribute());
						break;
					
					default:
						break;
				}
					
				// Create reference to definition of annotation model
				if (modelDefinition.getModelDefinitions().containsKey(mm.getModelType())) {
					for (ModelInfo minfo : modelDefinition.getModelDefinitions().get(mm.getModelType()).getModelFiles()) {						
						if (minfo.getModelUsage().equals(ModelUsage.LINK) ||
							minfo.getModelUsage().equals(ModelUsage.SYSTEM)	) {
							
							annotationScheme.addProperty(isDefinedBy, model.createResource(minfo.getUrl().toString()));
						}
					}
				}
				
				resultId = 1;
				if (! rs.getFileInfo().getFileResults().containsKey(mm)) continue; // catch bug
				for (FileResult r : rs.getFileInfo().getFileResults().get(mm)) {
					
					if (r.getFoundTagOrClass().equals(AnnotationUtil.unmatchedAnnotations)) {
						annotationScheme.addProperty(unmatchedCount, Integer.toString(r.getMatchingTagOrClass().split(",").length));
						for (String x : r.getMatchingTagOrClass().split(",")) {
							
							x = x.trim();
							if (x.isEmpty()) continue;
							
							Resource annotation = model.createResource(analysisURL+"/"+modelId+"/"+mm.getModelType()+"/"+resultId++);
							// found tag or class in file
							if (IndexUtils.isValidURL(r.getFoundTagOrClass())) {
								annotation.addProperty(annotatedClass, x);
							}
		 					else {
		 						annotation.addProperty(annotatedTag, x);
		 					}
							// flag for matched
							annotation.addLiteral(isMatched, false);
							
							// add annotation to annotationScheme
		 					annotationScheme.addProperty(contains, annotation);
						}
					} else {
					
						Resource annotation = model.createResource(analysisURL+"/"+modelId+"/"+mm.getModelType()+"/"+resultId++);
	 					
	 					// found tag or class in file
						if (IndexUtils.isValidURL(r.getFoundTagOrClass())) {
							annotation.addProperty(annotatedClass, r.getFoundTagOrClass());
						}
	 					else {
	 						annotation.addProperty(annotatedTag, r.getFoundTagOrClass());
	 					}
						
						// matched tag or class in annotation model
						if (IndexUtils.isValidURL(r.getMatchingTagOrClass())) {
							annotation.addProperty(matchedClass, model.createResource(r.getMatchingTagOrClass()));
						} else {
							annotation.addProperty(matchedTag, r.getMatchingTagOrClass());
						}
						
						// flag for matched
						annotation.addLiteral(isMatched, true);
						
	 					// count
						annotation.addProperty(count, r.getMatchCount());
						
						// add annotation to annotationScheme
	 					annotationScheme.addProperty(contains, annotation);
	 					
	 					// add matching bll concept
	 					bllKey = bllTools.getBllKey(rs, r, mm);
	 					/*if (r.getFeatureName().isEmpty()) {
	 						bllKey=rs.getDataURL()+"@"+rs.getFileInfo().getRelFilePath()+"@"+mm.getConllColumn()+"@"+r.getFoundTagOrClass()+"@"+mm.getModelType();
	 					}
	 					else {
	 						bllKey=rs.getDataURL()+"@"+rs.getFileInfo().getRelFilePath()+"@"+mm.getConllColumn()+"@"+r.getFeatureName()+"="+r.getFoundTagOrClass()+"@"+mm.getModelType();
	 						//Utils.debug("found :"+bllKey);
	 					}*/
	 					//Utils.debug(bllKey);
	 					if (bllModelMap != null && bllModelMap.get(bllKey) != null) {
	 						ArrayList<BLLConcept> bllConcepts = new ArrayList<BLLConcept>();
	 						for (String yy : bllModelMap.get(bllKey)) {
	 							annotation.addProperty(hasBllConcept, model.createResource(yy));
	 							bllConcepts.add(new BLLConcept(yy, "")); // TODO add label
	 						}
	 						r.setBllConcepts(bllConcepts);
	 						// if (feats)
	 						//	Utils.debug("found bll :"+bllKey);
	 						//        file:///media/EXTRA/tmp/en-ud-dev.conllu@en-ud-dev.conllu@5@Person=1@UDEP
	 						// found :file:///media/EXTRA/tmp/en-ud-dev.conllu@en-ud-dev.conllu@4@JJS@PENN

	 					}
						}
					}	
				
					// add model to dataset !
					dataset.addProperty(analysis, annotationScheme);
				
				}
				
				/*if (rs.getFileInfo().getFileResults() != null) {
					for (ModelMatch mm : rs.getFileInfo().getFileResults().keySet()) {
						
						switch (mm.getModelType()) {
						case BLL :
							for (FileResult fileResult : rs.getFileInfo().getFileResults().get(ModelType.BLL)) {
								dataset.addProperty(dctSubject, model.createResource(fileResult.getMatchingTagOrClass()+ "@count@"+fileResult.getMatchCount()));
							}
							break;
							
						default :
							for (FileResult fileResult : rs.getFileInfo().getFileResults().get(mm)) {
								// nif ?
								dataset.addProperty(dctSubject, model.createResource(fileResult.getMatchingTagOrClass()+ "@count@"+fileResult.getMatchCount()));
							}
						}
					}
				}*/
				
				// Get original file format
		 		ResourceFormat rf = IndexUtils.determineFileFormat(rs);
		 		if (rf != ResourceFormat.UNKNOWN) {
					dataset.addProperty(fileFormat, rf.name());
					if (resourceFormatMap.containsKey(rf)) {
						dataset.addProperty(dctFileFormat, resourceFormatMap.get(rf));
					}
		 		}

				/*if (!isXML) { // replaced by rf = IndexUtils.determineFileFormat(rs); (see above)
					// also not correct for udcatalan.rdf -> CONLL (because its CONLL-RDF)
					dataset.addProperty(fileFormat, rs.getFileInfo().getFileFormat().name());
					if (fileFormatMap.containsKey(rs.getFileInfo().getFileFormat())) {
						dataset.addProperty(dctFileFormat, fileFormatMap.get(rs.getFileInfo().getFileFormat()));
					}
				} else {
					// fixes missing XML state
					dataset.addProperty(fileFormat, FileFormat.XML.name());
					if (fileFormatMap.containsKey(rs.getFileInfo().getFileFormat())) {
						dataset.addProperty(dctFileFormat, fileFormatMap.get(FileFormat.XML));
					}
				}*/
			}
			
		}
		
		return model;
	}
	




	public static String serializeModel(Model model, RDFFormat format) {
		
		try {	
			StringWriter out = new StringWriter();
			RDFDataMgr.write(out, model, format);
			return out.toString();
		
		}
			catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
