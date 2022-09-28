package de.unifrankfurt.informatik.acoli.fid.resourceDB;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.unifrankfurt.informatik.acoli.fid.gremlinQuery.EmbeddedQuery;
import de.unifrankfurt.informatik.acoli.fid.gremlinQuery.ServerQuery;
import de.unifrankfurt.informatik.acoli.fid.types.AccountType;
import de.unifrankfurt.informatik.acoli.fid.types.Backup;
import de.unifrankfurt.informatik.acoli.fid.types.Comment;
import de.unifrankfurt.informatik.acoli.fid.types.ProcessingFormat;
import de.unifrankfurt.informatik.acoli.fid.types.FileInfo;
import de.unifrankfurt.informatik.acoli.fid.types.LanguageMatch;
import de.unifrankfurt.informatik.acoli.fid.types.ModelMatch;
import de.unifrankfurt.informatik.acoli.fid.types.ModelType;
import de.unifrankfurt.informatik.acoli.fid.types.ParseResult;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceCache;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceFormat;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceType;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceTypeInfo;
import de.unifrankfurt.informatik.acoli.fid.types.UserAccount;
import de.unifrankfurt.informatik.acoli.fid.types.VocabularyMatch;
import de.unifrankfurt.informatik.acoli.fid.types.VocabularyType;
import de.unifrankfurt.informatik.acoli.fid.xml.Template;
import de.unifrankfurt.informatik.acoli.fid.xml.TemplateQuality;

public interface ResourceManager {
	
	// Vertices
	String ResourceVertex 	= "RES";
	String FileVertex	 	= "FIL";
	String LanguageVertex	= "LANG";
	String ModelVertex		= "MODEL";
	String XMLTemplateVertex = "XMLT";
	String VocabularyVertex = "VOC";
	String TokenVertex		= "TOK";
	String PredicateVertex  = "PRED";
	String MetadataVertex 	= "META";
	String UnitVertex		= "UNIT";	// save info about the file structure
	String UserVertex		= "USER";
	String ResourceTypeVertex = "RESTYPE"; // corresponds to enum ResourceType
	String CommentVertex = "COM"; // saves all comments made on a resource
	String BackupVertex = "BAK";
	String QueueVertex = "QUEUE";
	String QueuedResourceVertex = "QRES";
	
	// Vertex properties
	String ResourceUrl		= "url";		// used for linghub accessUrl
	String ResourceMetaUrl	= "metaUrl";	// used for linghub distro
	String ResourceMetaUrl2	= "metaUrl2";	// used for linghub dataset
	String Resource4mat		= "format";
	String ResourceSize		= "content-length";
	String ResourceETag		= "http-etag";
	String ResourceResponseCode	= "http-status";
	String ResourceContentType	= "content-type";
	String ResourceLastModified = "last-modified";
	String ResourceDetectionType = "resourceDetection";
	String ResourceTypeName 	 = "rtname";
	String ResourceTypeSpecifier = "rtSpec";
	String ResourceMd5 			 = "md5";
	String ResourceSha256 		 = "sha256";


	
	String MetaFormat 			= "mformat";
	String MetaType 			= "mtype";
	String MetaLicense			= "mlicense";
	String MetaRights 			= "mrights";
	String MetaPublisher		= "mpublisher";
	String MetaTitle			= "mtitle";
	String MetaUbTitle			= "mubtitle";
	String MetaDescription		= "mdescr";
	String MetaCreator			= "mcreator";
	String MetaContributor		= "mcontrib";
	String MetaLocation			= "mloc";
	String MetaYear				= "myear";
	String MetaDate				= "mdate";
	String MetaDcLanguages		= "dclangs";
	String MetaDctLanguages		= "dctlangs";
	String MetaContact			= "mcontact";
	String MetaWebpage			= "mwebpage";
	String MetaSubject			= "msubject";
	String MetaDctSource		= "mdctsource";
	String MetaDctIdentifier	= "mdctidentifier";
	String MetaDatasource		= "msource";
	
	
	String Date			= "date";
	/**use filePathRel instead !
	 * @deprecated
	 */
	String FileId	 		= "id";
	String FileName	 		= "name";
	String FilePathAbs		= "abspath"; // place where file resides when processed first time (may be deleted or moved later)
	String FilePathRel		= "relpath"; // relative path (e.g. in archive file : archive/subfolder/file.txt
	
	String File4ormat	 	= "format";
	String FileStatusCode	= "status";
	String FileTripleCount	= "triples";
	String FileSizeInBytes	= "fileBytes";
	String FileErrorCode	= "ecode";
	String FileErrorMsg		= "emsg";
	String FileLanguageSample		= "langSample";
	String FileProcessState		= "pState";
	String FileComment		= "comment";
	String FileProcessingStartDate = "processStart";
	String FileProcessingEndDate = "processEnd";
	String FileAcceptedDate = "accepted";
	String FileSample		= "sample";
	String LanguageName		= "engName";
	String LanguageLexvoUrl	= "lexvo";
	String LanguageIso		= "iso";
	String LanguageDescription	= "langInfo";
	String VocabularyDescription	= "vocInfo";
	String Modeltype		= "mtype";
	String Vocabularytype	= "voctype";
	String XMLTemplateId	= "xmltid";
	String XMLTemplateDescription = "xmldesc";
	String XMLTemplateDefinition	= "xmltdef";
	String TokenString		= "tstr";
	String PredicateUrl		= "predUrl";
	String PredicateType	= "predType"; // can be lit | uri
	String PredicateDisabled	= "disabled"; 	// true | false
	String PredicateDefault	= "default";		// true | false (overrides disabled !)
	
	String UnitType			= "uType";			// RDF | CONLL | XML
	String UnitColumn		= "uColumn";
	String UnitXmlAttribute = "uXmlAttr";
	String UnitRdfProperty  = "uRdfProp";
	String UnitTokenCount   = "uTCount";
	
	String UserLogin		= "userlogin";
	String UserPassword		= "userpwd";
	String UserEmail		= "useremail";
	String UserAccountType	= "userRights";
	String UserMaxResourceUploads = "userMaxRcount";
	String UserMaxResourceFiles = "userMaxFcount";
	String UserMaxResourceUploadSize = "userMaxRsize";

	
	String CommentID		= "comId";
	String CommentDate		= "comDate";
	String CommentUserID	= "comUserId";
	String CommentTitle		= "comTitle";
	String CommentText 		= "comTex";
	String CommentAnswerToPostId = "comRelTo";
	
	String BackupName		= "bakName";
	String BackupDate		= "bakDate";
	String BackupVersionGremlin	= "bakVGremin";
	String BackupVersionDBReg	= "bakVDBReg";
	String BackupVersionDBData	= "bakVDBData";
	String BackupComment	= "bakCom";
	
	String QueueName		= "qname";
	String UserID 			= "userID";


	// Edges
	String FileEdge			= "includes";
	String LanguageEdgeLexvo = "hasLexvo";   	// points to a language
	String ModelEdge		= "usesModel";		// connects a file with a model (with a certain tag set)
	String XMLQualityEdge	= "xmlQualityE";	// connects a xml resource with conversion quality.
	String VocabularyEdge	= "hasVoc";			// points to a vocabulary
	String TokenEdge		= "hasTok";
	String PredicateEdge 	= "usesPred";		// connects a file vertex with a RDF predicate it uses
	String MetadataEdge		= "hasMetaD";		// connects a resource vertex with a metadata vertex
	String UnitEdge			= "hasUnit";		// connects a file vertex with a unit vertex
	String UserEdge			= "ownsRes";		// connects a user vertex with a resource vertex
	String ResourceSelectionEdge	= "selRes";	// connects a user vertex with a resource vertex (that is not owned by the user)
	String ResourceTypeEdge = "hasResType";		// connects a resource vertex with a resource type vertex
	String CommentEdge		= "hasCom";				// connects a resource vertex with a comment vertex
	String QueueHasEntry	= "qentry";
	
	// Model / Language / Predicate Edge properties
	String Detectionmethod		= "dtMethod";	// auto | manual | linghub (refers to DetectionMethod) 
	String ResourceTypeDetectionMethod = Detectionmethod;
	String Detectionsource		= "dtSource";	// for languages : 'langTag' | RDF-predicate URL (e.g. label) , for models : RDF-predicate URL (e.g. hasTag)
	String DifferentHitTypes 	= "dHitTypes";	// number of different hit types
	String ExclusiveHitTypes 	= "eHitTypes";	// hit types found only in this model
	String TotalHitCount 		= "tHitCount";	// hit sum total
	String ExclusiveHitCount 	= "eHitCount";	// hit sum for ExclusiveHitTypes
	String HitConllColumn 		= "conllColumn";
	String ModelCoverage		= "coverage";
	String Selected				= "selected";
	String Confidence			= "confidence";
	String ModelRecall			= "recall";		// true positives / all positives
	String ModelFalseNegativeTypes		= "fnTypes";
	String ModelFalseNegativeCount		= "fnCount";
	String ModelRdfProperty		= "modelRdfProp";
	String ModelWasUpdated		= Date;
	String UpdateText			= "utext";
	String ModelUpdateText		= UpdateText;
	String TokenCount			= "tcount";
	String TokenColumn			= "tcol";
	String LanguageMinProb		= "langMinProb";
	String LanguageMaxProb		= "langMaxProb";
	String LanguageAverageProb	= "langAVProb";
	String LanguageNameEn		= "langNameEn";
	String LanguageRdfProperty	= "langRdfProp";
	String LanguageWasUpdated	= Date;
	String LanguageUpdateText	= UpdateText;
	String XMLAttribute			= "xmlAtt";
	String PredicateSuccessful	= "good";	// 	true | false
	
	// User edge
	String UserPermissions 	= "userRights"; //"xyz"; where x=owner,y=group,z=all (encoded in unix manner, e.g. 700)
									           // 1=read,2=edit,4=export Used values 700 and 777, groups not implemented
	// Queue edge
	String QueuePosition	="qpos";
	
	
	// Values
	String DetectedByAuto = "auto";
	String DetectedByManual = "manual";
	String DetectedByLinghub = "linghub";
	
	String MetaDataToBeClarified = "http://linghub/metadata/tbc";
	String MetaDataFromClarin = "http://clarin/metadata";
	String MetaDataFromUser = "http://non-linghub.com";
	

	
	String getResourceMetaDataURL(String url);
	
	String getResourceMetaDataURL2(String url);

	ResourceFormat getResourceFormat(String url);
	
	Vertex addResource(ResourceInfo resourceInfo);
	
	void deleteResource(String resourceIdentifier);
	
	void deleteResourceFile(String resourceIdentifier, String relFilePath);

	Vertex getResource(String resourceIdentifier);

	boolean resourceExists(String resourceIdentifier);

	boolean resourceHadResults(String resourceIdentifier);

	void registerResource(ResourceInfo resourceInfo, HttpResponse header);
	
	Vertex addFile(ResourceInfo resourceInfo, ProcessingFormat fileFormat);
			
	Vertex addLanguage(URL lexvo, String description);
	
	void deleteLanguage(URL lexvoUrl);
	
	Vertex getLanguage(URL lexvo);
	
	Vertex addModel(ModelType modelType);
	
	void deleteModel(ModelType modelType);
	
	Vertex getModel(ModelType modelType);
	
	Graph getGraph();

	int getResourceResponseCode(String resourceIdentifier);

	String getResourceContentType(String resourceIdentifier);

	String getResourceLastModified(String resourceIdentifier);

	Long getResourceSize(String resourceIdentifier);
	
	ArrayList<Vertex> getResourceFiles(String resourceIdentifier);

	ArrayList<Vertex> getDoneResources();
	
	ArrayList<ResourceInfo> getDoneResourcesRI();

	HashSet<String> getDoneResourceUrls();

	HashSet<String> getDoneResourcesWithFileWithStatus(String statusCode,
			boolean checkAll);

	EmbeddedQuery getQueries();
	
	ServerQuery getServerQueries();
	
	void closeDb();

	void setFileErrorCode(Vertex resource, Vertex file, String errorCode);

	void setFileErrorMsg(Vertex resource, Vertex file, String errorMsg);

	void setFileStatusCode(Vertex resource, Vertex file, String statusCode);

	void setFileTripleCount(Vertex resource, Vertex file, long tripleCount);
	
    void setFileLanguageSample(Vertex resource, Vertex file, String textSample);
	
	void updateFileModels(Vertex resource, Vertex fileVertex, FileInfo fileInfo);

	ArrayList<Vertex> getFileLanguages(Vertex resource, Vertex file);
	
	void deleteDatabase();

	ArrayList<Vertex> getVertices();

	ArrayList<Edge> getEdges();
	
	ArrayList<Vertex> getFileModels(Vertex resource, Vertex file);

	boolean isModelLoaded(ModelType modelType);

	ArrayList<Vertex> getResourceFilesWithHits(String resourceIdentifier);
	
	
	void setResourceMetaDataUrl(String dataUrl, String metaDataUrl);
	
	void setResourceMetaDataUrl2(String dataUrl, String metaDataUrl);

	boolean setResourceDataUrl(String dataUrl, String newDataUrl);

	void updateFileModels(String resourceIdentifier, String fileIdentifier,
			FileInfo fileInfo, boolean forceOverwrite);
	
	void updateFileModels(ResourceInfo resourceInfo, boolean forceOverwrite);

	void updateFileLanguages(Vertex resourceVertex, Vertex fileVertex, FileInfo fileInfo);

	void updateFileLanguages(String resourceIdentifier, String fileIdentifier, FileInfo fileInfo, boolean forceOverwrite);

	ArrayList<Edge> getModelFileEdge(Vertex resource, Vertex f, Vertex model);

	ArrayList<ResourceInfo> getDoneResourcesRI(boolean onlySuccessfull);

	Cluster getCluster();
	
	Vertex addXMLTemplate(Template template);
	
	void removeXMLTemplate(String xmlTemplateIdentifier);
	
	Template getXMLTemplate(String xmlTemplateIdentifier);

	ArrayList<Template> getAllXMLTemplates();

	ArrayList<Edge> getLanguageFileEdge(Vertex resource, Vertex f, Vertex language);

	void setFileFormat(Vertex resource, Vertex file, ProcessingFormat fileFormat);

	void updateProcessState(ResourceInfo resourceInfo);

	void updateFileComment(ResourceInfo resourceInfo);

	ArrayList<ResourceInfo> getAllResourceFilesRI();

	ArrayList<ResourceInfo> getSuccessFullResourceFilesRI();

	ArrayList<ResourceInfo> getErrorResourceFilesRI();

	ArrayList<ResourceInfo> getUnSuccessFullResourceFilesRI();

	void updateFileProcessingStartDate(ResourceInfo resourceInfo);

	void updateFileProcessingEndDate(ResourceInfo resourceInfo);

	Edge addXMLTemplateQualityEdge(ResourceInfo resourceInfo, TemplateQuality bestMatch);
	
	void updateFileVocabularies(Vertex resourceVertex, Vertex fileVertex,
			ArrayList<VocabularyMatch> vocabularyMatches);

	void updateFileVocabularies(String resourceIdentifier, String fileIdentifier,
			ArrayList<VocabularyMatch> vocabularyMatches);

	Vertex addVocabulary(VocabularyType vocabulary, String description);

	Vertex getVocabulary(VocabularyType vocabulary);

	ArrayList<Vertex> getFileVocabularies(Vertex resource, Vertex file);

	ArrayList<Edge> getVocabularyFileEdge(Vertex resource, Vertex f,
			Vertex vocabulary);

	Vertex addToken(String token);

	Vertex getToken(String token);

	void updateFileTokens(Vertex resourceVertex, Vertex fileVertex,
			HashMap<Integer, HashMap<String, Long>> tokenMap);

	void updateFileTokens(String resourceIdentifier, String fileIdentifier,
			HashMap<Integer, HashMap<String, Long>> tokenMap);

	ArrayList<String> getFileTokens(ResourceInfo resourceInfo, int column);

	void setFileSample(Vertex resource, Vertex file, String sample);

	void updateFileAcceptedDate(ResourceInfo resourceInfo);

	void resetResource(String resourceIdentifier, Boolean deleteUnitInfo);

	ArrayList<ResourceInfo> getDoneResourceRI(Vertex resourceVertex);

	Vertex getResourceFile(ResourceInfo resourceInfo);

	Vertex getResourceFile(Vertex resourceVertex, String fileIdentifier);

	Vertex getResourceFile(String resourceIdentifier, String fileIdentifier);

	Long getFileBytes(Vertex resource, String fileIdentifier);

	Long getFileTripleCount(Vertex resource, String fileIdentifier);

	String getFileErrorCode(Vertex resource, String fileIdentifier);

	String getFileErrorMsg(Vertex resource, String fileIdentifier);

	String getFileStatusCode(Vertex resource, String fileIdentifier);

	String getFileName(Vertex resource, String fileIdentifer);

	ProcessingFormat getFileFormat(Vertex resource, String fileIdentifier);

	String getFileLanguageSample(Vertex resource, String fileIdentifier);

	Integer getFileId(Vertex resource, String fileIdentifier);

	String getFileSample(Vertex resource, String fileIdentifier);

	ArrayList<Vertex> getResourceFiles(Vertex resource);

	Vertex addPredicate(String predicate);

	Vertex getPredicate(String predicate);

	void updateFilePredicates(Vertex resourceVertex, Vertex fileVertex,
			HashMap<String, Boolean> predicateMap);

	void updateFilePredicates(String resourceIdentifier, String fileIdentifier,
			HashMap<String, Boolean> predicateMap);

	void setPredicateDisabled(String predicateUrl);

	void setPredicateEnabled(String predicateUrl);

	HashSet<String> getUnsuccessfulPredicates();
	
	HashSet<String> getSuccessfulPredicates();

	/**
	 * Set this predicate as default. This implies that it will always be used during search.
	 * (overrides the predicate disabled)
	 * @param predicate URL
	 */
	void setPredicateDefault(String predicateUrl);

	void initPredicates();

	void setFileSize(Vertex resource, Vertex file, long fileSizeInBytes);

	void setFileError(Vertex resource, Vertex file, String errorCode,
			String errorMsg);

	int getDoneResourceCount();

	/**
	 * @deprecated
	 * @return
	 */
	int getXMLResourcesWithModelOrLanguageCount();

	/**
	 * @deprecated
	 * @return
	 */
	int getXMLResourcesWithSelectedModelsCount();

	/**
	 * @deprecated
	 * @return
	 */
	int getXMLResourcesWithSelectedLanguagesCount();

	int getXMLResourcesWithFileFormatXML();
	/**
	 * @deprecated
	 * @return
	 */
	int getXMLResourcesWithUnselectedModelAndLanguageCount();

	ArrayList<Vertex> getDoneFileResources();

	int getDoneFileResourceCount();


	HashSet<String> getGlobalProcessState(ResourceInfo resourceInfo,
			String resourcePrefix);

	ArrayList<Vertex> getResourceMetadata(String resourceIdentifier);

	Vertex addResourceMetadata(ResourceInfo resourceInfo);

	void updateResourceMetadata(ResourceInfo resourceInfo);

	void resetResourceFile(String resourceIdentifier, String relFilePath);

	void setFileRelPath(String resourceIdentifier, String fileIdentifier,
			String newFileIdentifier);

	void setFileName(String resourceIdentifier, String relFilePath,
			String newFileName);

	void setFileAbsPath(String resourceIdentifier, String relFilePath,
			String newFilePath);

	ArrayList<ResourceInfo> getDoneResourceRI(String resourceIdentifier);
	
	HashMap<String,Long> getFileTokensWithCount(ResourceInfo resourceInfo,
			int column);

	Boolean updateResourceHeaderData(ResourceInfo resourceInfo);

	void updateFileUnitInfo(ResourceInfo resourceInfo);

	HashMap<String, Integer> getRdfTokenCounts(ResourceInfo resourceInfo);

	HashMap<String, Integer> getXmlTokenCountsByAttribute(ResourceInfo resourceInfo);

	HashMap<Integer, Integer> getConllTokenCounts(ResourceInfo resourceInfo);
	
	HashMap<String, Integer> getXmlAttributes2ConllColumns(ResourceInfo resourceInfo);

	HashMap<Integer, String> getConllColumns2XmlAttributes(ResourceInfo resourceInfo);

	HashMap<Integer, Integer> getXmlTokenCountsByColumn(ResourceInfo resourceInfo);

	ArrayList<Integer> getFileTokenColumns(ResourceInfo resourceInfo);

	void deleteResourceFileTokens(ResourceInfo resourceInfo, int col);
	
	Vertex addUser(UserAccount userAccount);
	
	Vertex getUser(String login);
		
	Boolean deleteUser(UserAccount userAccount);
	
	void setUserEmail (String login, String newEmail);
	
	void setUserPassword (String login, String newPassWd);
	
	Boolean setResourceOwner(String resourceIdentifier, String userLogin);

	Vertex addResource(ResourceInfo resourceInfo, String userLogin);
	
	UpdateManager getUpdateManager();

	ArrayList<Vertex> getResourceOwners(String resourceIdentifier);

	ArrayList<Vertex> getResourcesOwnedByUser(String userID);

	ArrayList<ResourceInfo> getDoneResourcesRINoFiles(ArrayList<Vertex> resourceVertices);

	ArrayList<ResourceInfo> getDoneResourcesOwnedByUser(String userID);
	
	Boolean setResourcePermissions(String resourceIdentifier, String userID, String permissions);
	
	String getResourcePermissions(String resourceIdentifier);
	
	boolean userExists(String userID);
	
	boolean userExists(String userID, String password);
	
	Boolean selectResource(String userID, String resourceIdentifier);
	
	Boolean deselectResource(String userID, String resourceIdentifier);
	
	ArrayList<Vertex> getResourcesSelectedByUser(String userID);

	ArrayList<ResourceInfo> getDoneResourcesSelectedByUser(String userID);

	String getResourceOwner(String resourceIdentifier);

	boolean isResourceSelectedByUser(String resourceIdentifier, String userID);

	ArrayList<Vertex> resourcesWithLanguages(Set<String> languages, Boolean andTrueOrFalse, Boolean only);

	ArrayList<Vertex> resourcesWithModels(Set<String> models, Boolean andTrueOrFalse, Boolean findExclusiveModels);

	ArrayList<Vertex> resourcesWithNameLike(String resourceName, Boolean ignoreCase);

	ArrayList<Vertex> getResourcesWithType(ResourceType resourceType);

	List<ResourceInfo> addFileInfo2Resources(List<ResourceInfo> rfl,
			HashSet<ParseResult> withParseResults);

	Vertex addResourceType (ResourceType resourceType);
	
	void updateResourceTypeInfos(String resourceIdentifier, ArrayList<ResourceTypeInfo> resourceTypeInfo);
	
	ArrayList<ResourceTypeInfo> getResourceTypeInfo(String resourceIdentifier);

	ArrayList<Vertex> getResourceType();

	Vertex getResourceType(ResourceType resourceType);

	void addResourceTypeInfo(String dataURL, ResourceTypeInfo resourceTypeInfo);
	
	void addComment(String resourceIdentifier, Comment comment);
	
	void deleteComment(String resourceIdentifier, int postId);
	
	Comment getComment(String resourceIdentifier, int postId);
	
	List<Comment> getAllComments(String resourceIdentifier);
	
	Integer getNextCommentId(String resourceIdentifier);

	void setUserPrivileges(UserAccount userAccount);

	UserAccount getUserAccount(String user);

	ArrayList<ResourceInfo> getDoneResourcesRI(HashSet<ParseResult> withParseResults);

	ResourceCache getDoneResourcesRIMap(HashSet<ParseResult> withParseResults);

	ResourceCache getDoneResourcesRIMap(String resourceIdentifier);

	ArrayList<String> getResourcesOwnedByUserAsUrl(String userID);

	ArrayList<String> getResourcesSelectedByUserAsUrl(String userID);

	void deleteResourceTypeInfos(String resourceIdentifier);

	ArrayList<String> getDoneResourcesAsString();

	ArrayList<Vertex> getResourcesWithMetadataLike(String queryString, Boolean ignoreCase);

	ArrayList<Vertex> getResourcesWithCommentLike(String queryString, Boolean ignoreCase);

	Long getResourceFileCountOwnedByUser(String userID);

	ArrayList<String> getAllUserLogins();

	Long getUserCount(AccountType accountType);

	Vertex updateUser(UserAccount userAccount);
	
	Vertex addBackup(Backup backup);

	Boolean deleteBackup(Backup backup);
	
	Vertex getBackup(Backup backup);

	List<Backup> getBackups();

	boolean backupExists(Backup backup);

	void setCluster(Cluster cluster);
	
	boolean addQueue (String queueName, List<ResourceInfo> resources);
		
	List<ResourceInfo> getQueue(String queueName);

	void deleteQueue (String queueName);

	void updateFileSample(ResourceInfo resourceInfo);

	void updateFileLanguageSample(ResourceInfo resourceInfo);

	void addFileLanguage(String resourceIdentifier, String fileIdentifier, LanguageMatch languageMatch);

	void deleteFileLanguage(String resourceIdentifier, String fileIdentifier, LanguageMatch languageMatch);

	void addFileModel(String dataURL, String relFilePath, ModelMatch model);

	void deleteFileModel(String dataURL, String relFilePath, ModelMatch model);

	void updateResourceMD5Sha256(ResourceInfo resourceInfo);

	ArrayList<Vertex> findResourcesWithHash(String md5, String sha256);
}
