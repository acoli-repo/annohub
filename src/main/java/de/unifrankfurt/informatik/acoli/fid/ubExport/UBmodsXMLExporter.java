package de.unifrankfurt.informatik.acoli.fid.ubExport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import de.unifrankfurt.informatik.acoli.fid.linghub.FusekiTDB;




public class UBmodsXMLExporter {

	protected Document DOCUMENT;
	
	protected FusekiTDB tdb; // for encapsulating very large turtle files
	
	protected boolean onlyExportThesaurusConcepts = true;
	
	public enum BLLHierarchy {
		onlyThesaurus, //Might miss a few index terms since linking is established on Ontology level.
		onlyOntology,
		thesaurusAndOntology
	}
	
	
	public UBmodsXMLExporter() throws Exception {
		//create new XML-Document (for reduced Output)
		DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
		DOCUMENT = documentBuilder.parse(new File(Constants.config.getString("SearchEngine.xmlExportBase")));
//		linghubModel = ModelFactory.createDefaultModel();
		
		if(!Constants.removeFolderContent(Constants.config.getString("SearchEngine.PATH-MODS-TDB"))) {
			System.err.println("Unable to create empty TDB folder");
			System.exit(1);
		}
		tdb = new FusekiTDB(Constants.config.getString("SearchEngine.PATH-MODS-TDB"));
	}
	
	/**
	 * Writes the built in-memory DOM-tree to disk.
	 * @param path (exportPath)
	 * @throws Exception
	 */
	public void writeXML(String path) throws Exception {
		File f = new File(path);
		
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		
		DOMSource domSource = new DOMSource(DOCUMENT);
		StreamResult streamResult = new StreamResult(f);
		transformer.transform(domSource, streamResult);
	}
	
	public void readBllOntology(String path) {
		File f = new File(path);
		
		if (f.isFile() && f.canRead() && (
				f.getName().endsWith(".owl") || 
				f.getName().endsWith(".ttl") || 
				f.getName().endsWith(".rdf")
				)) {
			tdb.getDataset().begin(ReadWrite.WRITE);
			Model m = tdb.getDataset().getNamedModel("http://bll-ontology.ttl");
			try {
				m.read(new FileInputStream(f), "");
				tdb.getDataset().commit();
			} catch (Exception e) {
				System.out.println(e+" ... trying RDFXML");
				try {
					RDFDataMgr.read(m
							, new FileInputStream(f)
							, Lang.RDFXML
							);
					tdb.getDataset().commit();
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}
			} finally {
				tdb.getDataset().end();
			}
		}	
	}

	
	/**
	 * reads RDF file and adds it to the inputModel graph.
	 * @param f
	 */
	public void readInputFile(File f) {
		
		tdb.getDataset().begin(ReadWrite.WRITE); 
		Model m = tdb.getDataset().getNamedModel("http://inputmodel");
		try {
			m.read(new FileInputStream(f), "");
			tdb.getDataset().commit();
		} catch (Exception e) {
			System.out.println(e+" ... trying TTL");
			try {
				RDFDataMgr.read(m
						, new FileInputStream(f)
						, Lang.TURTLE
						);
				tdb.getDataset().commit();
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
		} finally {
			tdb.getDataset().end();
		}
	}
	
	public void addNewDatasetToInputModel(String uri) {
		try {
			Model tmp = ModelFactory.createDefaultModel();
			String[] urisegments = uri.split("#");
			tmp.add(readExternalModel(urisegments[0]));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Augments the prechached dcat-InputModel with its original data.
	 * Tries to read from each dataset- and distribution-URI and adds 
	 * the data to the inputModel.
	 */
	public void updateInputModel() {
		String query = PREFIX
				+"\r\n"+ "SELECT ?ds {"
				+"\r\n"+ "  GRAPH <http://inputmodel> {"
				+"\r\n"+ "    {?ds a dcat:Dataset .}"
				+"\r\n"+ "    UNION "
				+"\r\n"+ "    {?ds a dcat:Distribution .}"
				+"\r\n"+ "  }"
				+"\r\n"+ "}";
		ResultSet rs;
		tdb.getDataset().begin(ReadWrite.WRITE); 
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, tdb.getDataset());
			rs = exec.execSelect();
		} catch (Exception e) {
			System.err.println(e);
			return;
		}
		while (rs.hasNext()) {
			QuerySolution sol = rs.next();
			String[] urisegments = sol.getResource("ds").getURI().split("#");
			tdb.getDataset().getNamedModel("http://inputmodel").add(readExternalModel(urisegments[0]));
		}
		tdb.getDataset().commit();
		tdb.getDataset().end();
	}
	
	/**
	 * Reads a Model from any URL, tries to read turtle if no other filetype is specified
	 * Works on linghub, but is not nice.
	 * @param uri
	 * @return
	 */
	public Model readExternalModel(String uri) {
		Model m = ModelFactory.createDefaultModel();
		try {
			if (!uri.matches(".*\\.\\w\\w+")) uri += ".ttl";
			m.read(uri, "TURTLE");
		} catch (Exception e) {
			System.out.println("External Model <"+uri+"> could not be read. Using prechached data only...");
		}
		return m;
	}
	
	/**
	 * Build UBmods DOM-tree from InputModel.
	 */
	public void processInputModel() {
		tdb.getDataset().begin(ReadWrite.READ);
		try {
			String query = PREFIX
					+"\r\n"+ "SELECT ?ds {"
					+"\r\n"+ "  GRAPH <http://inputmodel> {"
					+"\r\n"+ "    ?ds a dcat:Dataset ."
					+"\r\n"+ "    FILTER(NOT EXISTS{?ds dct:isPartOf []}) ."  //only select upper level metadata ds.
					+"\r\n"+ "  }"
					+"\r\n"+ "}";
			ResultSet rs;
			try {
				QueryExecution exec = QueryExecutionFactory.create(query, tdb.getDataset());
				rs = exec.execSelect();
			} catch (Exception e) {
				System.err.println(e);
				return;
			}
			while (rs.hasNext()) {
				QuerySolution sol = rs.next();
				System.out.println("NOW PROCESSING "+sol.get("ds"));
//				try {
//					memAccessor.deleteModel("http://inprocess");
//					String constQuery = PREFIX
//							+"\r\n"+ "CONSTRUCT { "
//							+"\r\n"+ "  <"+sol.getResource("ds").getURI()+"> ?p1 ?s2 ."
//							+"\r\n"+ "  ?s2 ?p2 ?s3 ."
//							+"\r\n"+ "  ?s3 ?p3 ?s4 ."
//							+"\r\n"+ "  ?s4 ?p4 ?s5 ."
//							+"\r\n"+ "  ?s5 ?p5 ?s6 ."
//							+"\r\n"+ "  ?s6 ?p6 ?s7 ."
//							+"\r\n"+ "  ?s7 ?p7 ?s8 ."
//							+"\r\n"+ "} WHERE {"
//							+"\r\n"+ "  <"+sol.getResource("ds").getURI()+"> ?p1 ?s2 ."
//							+"\r\n"+ "  OPTIONAL { ?s2 ?p2 ?s3 ."
//							+"\r\n"+ "    OPTIONAL { ?s3 ?p3 ?s4 ."
//							+"\r\n"+ "      OPTIONAL { ?s4 ?p4 ?s5 ."
//							+"\r\n"+ "        OPTIONAL { ?s5 ?p5 ?s6 ."
//							+"\r\n"+ "          OPTIONAL { ?s6 ?p6 ?s7 ."
//							+"\r\n"+ "            OPTIONAL { ?s7 ?p7 ?s8 ."
//							+"\r\n"+ "  } } } } } }"
//							+"\r\n"+ "}";
//					QueryExecution execConst = QueryExecutionFactory.create(constQuery, tdb.getDataset().getNamedModel("http://inputmodel"));
//					Model m = execConst.execConstruct();
//					memAccessor.add("http://inprocess", m);
//				} catch (Exception e) {
//					System.err.println(e);
//					continue;
//				}
				Element entry = DOCUMENT.createElement("mods");
				generateXmlEntry(sol.getResource("ds").getURI(), entry, sol.getResource("ds").getURI());
//				generateXmlEntry("http://linguistik.de/annohub/dataset/190378032", entry, "http://linguistik.de/annohub/dataset/190378032"); // just for testing
				
				DOCUMENT.getDocumentElement().appendChild(entry);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			tdb.getDataset().end(); 
		}
	}
	
	/**
	 * Recursive Call extracting Metadata from a given Dataset or Distribution.
	 * Calls functions for each tag in ModsXML.
	 * Is called recursively for each included Distribution.
	 * @param uri
	 * @param entry
	 * @param datasetURI
	 * @throws Exception
	 */
	public void generateXmlEntry(String uri, Element entry, String datasetURI) throws Exception {
		sparqlRecordInfo(uri, entry);
		sparqlTitleInfo(uri, entry);
		sparqlName(uri, entry);                 
		sparqlTypeOfResource(uri, entry);
		sparqlGenre(uri, entry);
		sparqlOriginInfo(uri, entry);
		sparqlLanguage(uri, entry);				
		sparqlPhysicalDescription(uri, entry);
		sparqlAbstract(uri, entry);				
		sparqlSubject(uri, entry);
		sparqlIdentifier(uri, entry);			
		sparqlLocation(uri, entry);
		sparqlAccessCondition(uri, entry);
		sparqlNote(uri, entry);
//		sparqlExtension(uri, entry); 			//NOTE: wird gebraucht??
		
		
		sparqlClassification(uri, entry);       
		
		//rekursiver Aufruf für Subelemente. 
		sparqlRelatedItem(uri, entry, datasetURI);
	}
	
	/**
	 * Fetch all distributions of the given URI and add them as relatedItem recursively.
	 * @param uri
	 * @param entryNode
	 * @param datasetURI
	 */
	public void sparqlRelatedItem(String uri, Node entryNode, String datasetURI) {
		String query = PREFIX
				+"\r\n"+ "SELECT * {"
				+"\r\n"+ "  <"+uri+"> dcat:distribution ?relatedItem ."
				+"\r\n"+ "}";

		ResultSet rs;
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, tdb.getDataset().getNamedModel("http://inputmodel"));
			rs = exec.execSelect();
		} catch (Exception e) {
			System.err.println(e);
			return;
		}
		
		while (rs.hasNext()) {
			QuerySolution sol = rs.next();
			Element relatedItem = DOCUMENT.createElement("relatedItem");
			try {
				generateXmlEntry(sol.getResource("relatedItem").getURI(), relatedItem, datasetURI);
				entryNode.appendChild(relatedItem);
			} catch (Exception e) {
				System.err.println(e);
			}
		}
	}


	public void sparqlClassification(String uri, Node entryNode) throws Exception {
		String query = PREFIX
				+"\r\n"+ "SELECT ?bll (sum(xsd:integer(?count)) as ?cnt) ?lbl ?internalID {"
				+"\r\n"+ "  GRAPH <http://inputmodel> {"
				+"\r\n"+ "    <"+uri+"> dct:hasPart ?ds_anno ."
				+"\r\n"+ "    ?ds_anno a dcat:Dataset ."
				+"\r\n"+ "    "
				+"\r\n"+ "    ?ds_anno annohub:analysis/annohub:contains* ?anal ."
				+"\r\n"+ "    ?anal annohub:hasBllConcept ?bll_eq ."
				+"\r\n"+ "    OPTIONAL{?anal annohub:count ?count .}"
				+"\r\n"+ "  }"
				+"\r\n"+ "  GRAPH <http://bll-ontology.ttl> {"
				+"\r\n"+ "    ?bll_eq ((owl:equivalentClass|^owl:equivalentClass)/((owl:unionOf|owl:intersectionOf)/rdf:rest*/rdf:first)?)* ?bll ."
				+"\r\n"+ "    ?bll rdfs:label ?lbl . FILTER(LANG(?lbl) = 'de') ."
				+"\r\n"+ "    ?bll rdf:value ?internalID ."
				+"\r\n"+ "  }"
				+"\r\n"+ "} group by ?bll ?lbl ?internalID";
		ResultSet rs;
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, tdb.getDataset());
			rs = exec.execSelect();
		} catch (Exception e) {
			System.err.println(e);
			return;
		}
		while (rs.hasNext()) {
			QuerySolution sol = rs.next();
			System.out.println(sol.get("bll"));
			System.out.println(sol.get("cnt"));
			System.out.println(sol.get("lbl"));
			System.out.println(sol.get("internalID"));
			if (sol.getResource("bll") == null) continue; // BUGFIX cause for null elements currently unknown
			if (onlyExportThesaurusConcepts && !sol.getResource("bll").getLocalName().startsWith("bll-")) continue; //TODO: rethink onlythesaurus mode
			Element classification = DOCUMENT.createElement("classification");
			classification.setAttribute("authority", "bll");
			classification.setAttribute("authorityURI", "bll:bll-133074811"); //top level index term
			classification.setAttribute("valueURI", "bll:"+sol.getResource("bll").getLocalName()); //actual index term
			if (sol.getLiteral("lbl") != null)
				classification.setAttribute("displayLabel",  sol.getLiteral("lbl").getString());
			if (sol.getLiteral("cnt") != null)
				classification.setAttribute("generator",  sol.getLiteral("cnt").getLexicalForm());
			if (sol.getLiteral("internalID") != null)
				classification.setTextContent(sol.getLiteral("internalID").getString()); //04.05.04.037
			
			entryNode.appendChild(classification);
		}
	}
	
	
	public void sparqlRecordInfo(String uri, Node entryNode) throws Exception {
		//obligatory part
		Element recordInfo = DOCUMENT.createElement("recordInfo");
		entryNode.appendChild(recordInfo);
		
		Element recordIdentifier = DOCUMENT.createElement("recordIdentifier");
		recordInfo.appendChild(recordIdentifier);
		recordIdentifier.setTextContent(uri);
		
		
		//optional part
		String query = PREFIX
				+"\r\n"+ "SELECT * {"
				+"\r\n"+ "  <"+uri+"> dc:source|dct:source ?source ."
				+"\r\n"+ "  <"+uri+"> ?p ?source ."
				+"\r\n"+ "}";
		ResultSet rs;
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, tdb.getDataset().getNamedModel("http://inputmodel"));
			rs = exec.execSelect();
		} catch (Exception e) {
			System.err.println(e);
			return;
		}
		//only first row is processed (considered as main source)
		if (rs.hasNext()) {
			QuerySolution sol = rs.next();
			Element recordContentSource = DOCUMENT.createElement("recordContentSource");
			recordInfo.appendChild(recordContentSource);
			recordContentSource.setTextContent(sol.get("source").toString());
		}
	}
	
	public void sparqlTitleInfo(String uri, Element entry) {
		String query = PREFIX
				+"\r\n"+ "SELECT distinct ?title ?p {"
				+"\r\n"+ "  { "
				+"\r\n"+ "  <"+uri+"> annohub:ubTitle|dc:title|dct:title|dc:alternative|dct:alternative ?title ."
				+"\r\n"+ "  <"+uri+"> ?p ?title ."
				+"\r\n"+ "  } "
				+"\r\n"+ "  UNION "
				+"\r\n"+ "  { "
				+"\r\n"+ "  ?obj foaf:primaryTopic <"+uri+"> ."
				+"\r\n"+ "  ?obj annohub:ubTitle|dc:title|dct:title|dc:alternative|dct:alternative ?title ."
				+"\r\n"+ "  ?obj ?p ?title ."
				+"\r\n"+ "  } "
				+"\r\n"+ "}";

		ResultSet rs;
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, tdb.getDataset().getNamedModel("http://inputmodel"));
			rs = exec.execSelect();
		} catch (Exception e) {
			System.err.println(e);
			return;
		}
		
		while (rs.hasNext()) {
			QuerySolution sol = rs.next();
			Element titleInfo = DOCUMENT.createElement("titleInfo");

			Element title = DOCUMENT.createElement("title");
			title.setTextContent(sol.getLiteral("title").getString());
			titleInfo.setAttribute("otherType", sol.getResource("p").getLocalName());
			if (sol.getLiteral("title").getLanguage() != null)
				if (!sol.getLiteral("title").getLanguage().equals(""))
					title.setAttribute("lang", sol.getLiteral("title").getLanguage());
			titleInfo.setAttribute("authorityURI", sol.getResource("p").getNameSpace());
			titleInfo.setAttribute("valueURI", sol.getResource("p").getURI());
			titleInfo.appendChild(title);
//			titleInfo.setAttribute("type", sol.getResource("p").getLocalName());

			entry.appendChild(titleInfo);
		}
		
		// ____________additional generated titles (fallback)
		query = PREFIX
				+"\r\n"+ "SELECT ?url {"
				+"\r\n"+ "  <"+uri+"> dcat:accessURL ?url ."
				+"\r\n"+ "  <"+uri+"> ?p ?url ."
				+"\r\n"+ "}";

		rs = null;
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, tdb.getDataset().getNamedModel("http://inputmodel"));
			rs = exec.execSelect();
		} catch (Exception e) {
			System.err.println(e);
			return;
		}
		
		while (rs.hasNext()) {
			QuerySolution sol = rs.next();
			Element titleInfo = DOCUMENT.createElement("titleInfo");

			Element title = DOCUMENT.createElement("title");
			title.setTextContent(sol.get("url").toString().replaceAll(".*://", "").replaceAll("\\W", "-"));
			titleInfo.setAttribute("otherType", "accessURL");
			titleInfo.appendChild(title);

			entry.appendChild(titleInfo);
		}
		
		//____________default fallback linghubURL
		Element titleInfo = DOCUMENT.createElement("titleInfo");

		Element title = DOCUMENT.createElement("title");
		title.setTextContent(uri.replaceAll(".*://", "").replaceAll("\\W", "-"));
		titleInfo.setAttribute("otherType", "internalURI");
		titleInfo.appendChild(title);

		entry.appendChild(titleInfo);
	}
	
	public void sparqlName(String uri, Element entry) {
		String query = PREFIX
				+"\r\n"+ "SELECT ?entity ?role ?nameType ?namePart {"
				+"\r\n"+ "  {"
				+"\r\n"+ "    <"+uri+"> (dc:creator|dct:creator|dct:contributor|dc:contributor) ?entity ."
				+"\r\n"+ "    <"+uri+"> ?role ?entity ."
				+"\r\n"+ "    bind(?entity as ?namePart)."
//				+"\r\n"+ "    bind('fullname' as ?nameType)."
				+"\r\n"+ "    FILTER(isLiteral(?entity))"
				+"\r\n"+ "  } UNION {"
				+"\r\n"+ "    <"+uri+"> (dc:creator|dct:creator|dct:contributor|dc:contributor) ?entity ."
				+"\r\n"+ "    <"+uri+"> ?role ?entity ."
				+"\r\n"+ "    ?entity  (!ex:p)+ ?temp ."
				+"\r\n"+ "    ?temp   ?nameType ?namePart ."
				+"\r\n"+ "    FILTER(!isLiteral(?entity))"
				+"\r\n"+ "  }"
				+"\r\n"+ "} order by ?entity";

		ResultSet rs;
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, tdb.getDataset().getNamedModel("http://inputmodel"));
			rs = exec.execSelect();
		} catch (Exception e) {
			System.err.println(e);
			return;
		}
		if (rs.hasNext()) {
			String last_entity = null;
			Element name = DOCUMENT.createElement("name");
			Element role = DOCUMENT.createElement("role");
			while (rs.hasNext()) {
				QuerySolution sol = rs.next();
				if (!sol.get("entity").toString().equals(last_entity)) {
					if(last_entity != null) {
						name.appendChild(role);
						entry.appendChild(name);
					}
					name = DOCUMENT.createElement("name");
					role = DOCUMENT.createElement("role");
					Element roleTerm = DOCUMENT.createElement("roleTerm");
					roleTerm.setTextContent(sol.getResource("role").getLocalName());
					roleTerm.setAttribute("type", "text");
					roleTerm.setAttribute("authorityURI", sol.getResource("role").getNameSpace());
					roleTerm.setAttribute("valueURI", sol.getResource("role").getURI());
					role.appendChild(roleTerm);
				}
	
				Element namePart = DOCUMENT.createElement("namePart");
				namePart.setTextContent(sol.getLiteral("namePart").getString());
				if (sol.get("nameType")!= null) {
					namePart.setAttribute("type", sol.getResource("nameType").getLocalName());
				}
				name.appendChild(namePart);
				last_entity = sol.get("entity").toString();
			}
			name.appendChild(role);
			entry.appendChild(name); // add last name element
		}
	}
	
	public void sparqlTypeOfResource(String uri, Element entry) {
		Element typeOfResource = DOCUMENT.createElement("typeOfResource");
		typeOfResource.setTextContent("software, multimedia");
		entry.appendChild(typeOfResource);
	}
	
	public void sparqlGenre(String uri, Element entry) {
		Element genre = DOCUMENT.createElement("genre");
		genre.setTextContent("Dataset");
		entry.appendChild(genre);
		
		

		
		
		String query = PREFIX
				+"\r\n"+ "SELECT * {"
				+"\r\n"+ "  <"+uri+"> dc:type|dct:type ?genre ."
				+"\r\n"+ "  <"+uri+"> ?p ?genre ."
				+"\r\n"+ "}";

		ResultSet rs;
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, tdb.getDataset().getNamedModel("http://inputmodel"));
			rs = exec.execSelect();
		} catch (Exception e) {
			System.err.println(e);
			return;
		}
		
		while (rs.hasNext()) {
			QuerySolution sol = rs.next();
			genre = DOCUMENT.createElement("genre");
			if (sol.get("genre").isLiteral()) {
				genre.setTextContent(sol.getLiteral("genre").getString());
				if (sol.getLiteral("genre").getLanguage() != null)
					if (!sol.getLiteral("genre").getLanguage().equals(""))
						genre.setAttribute("lang", sol.getLiteral("genre").getLanguage());
			} else {
				genre.setTextContent(sol.getResource("genre").getLocalName());
				genre.setAttribute("authorityURI", sol.getResource("genre").getNameSpace());
				genre.setAttribute("valueURI", sol.getResource("genre").toString());
			}

			entry.appendChild(genre);
		}
	}
	
	public void sparqlOriginInfo(String uri, Element entry) {
		Element originInfo = DOCUMENT.createElement("originInfo");
		int i = 0;
		
		String query = PREFIX
				+"\r\n"+"SELECT * {"
				+"\r\n"+"  {"
				+"\r\n"+"    <"+uri+"> dc:publisher|dct:publisher ?o ."
				+"\r\n"+"	 bind ('publisher' as ?p)"
				+"\r\n"+"  } UNION {"
				+"\r\n"+"    <"+uri+"> dc:spatial|dct:spatial|dc:coverage|dct:coverage ?o ."
				+"\r\n"+"	 bind ('place' as ?p)"
				+"\r\n"+"  } UNION {"
				+"\r\n"+"    <"+uri+"> dc:date|dct:date|dct:temporal|dct:available ?o ."
				+"\r\n"+"	 bind ('dateOther' as ?p)"
				+"\r\n"+"  } UNION {"
				+"\r\n"+"    <"+uri+"> dc:issued|dct:issued ?o ."
				+"\r\n"+"	 bind ('dateIssued' as ?p)"
				+"\r\n"+"  } UNION {"
				+"\r\n"+"    <"+uri+"> dc:created|dct:created ?o ."
				+"\r\n"+"	 bind ('dateCreated' as ?p)"
				+"\r\n"+"  } UNION {"
				+"\r\n"+"    <"+uri+"> dc:modified|dct:modified ?o ."
				+"\r\n"+"	 bind ('dateModified' as ?p)"
				+"\r\n"+"  }"
				+"\r\n"+"}";

		ResultSet rs;
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, tdb.getDataset().getNamedModel("http://inputmodel"));
			rs = exec.execSelect();
		} catch (Exception e) {
			System.err.println(e);
			return;
		}
		
		while (rs.hasNext()) {
			QuerySolution sol = rs.next();

			Element p = DOCUMENT.createElement(sol.get("p").toString());
			p.setTextContent(sol.get("o").toString());
			originInfo.appendChild(p);
			i++;
		}
			
		if (i > 0) {
			entry.appendChild(originInfo);
		}
	}
	
	public void sparqlLanguage(String uri, Element entry) {
		String query = PREFIX
				+"\r\n"+ "SELECT * {"
				+"\r\n"+ "  <"+uri+"> dc:language|dct:language ?language ."
				+"\r\n"+ "  <"+uri+"> ?p ?language ."
				+"\r\n"+ "}";

		ResultSet rs;
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, tdb.getDataset().getNamedModel("http://inputmodel"));
			rs = exec.execSelect();
		} catch (Exception e) {
			System.err.println(e);
			return;
		}
		
		while (rs.hasNext()) {
			try {
				QuerySolution sol = rs.next();
				Element language = DOCUMENT.createElement("language");

				Element languageTerm = DOCUMENT.createElement("languageTerm");
				if (sol.get("language").isLiteral()) {
					languageTerm.setTextContent(sol.getLiteral("language").getString());
					languageTerm.setAttribute("type", "text");
				}
				if (sol.get("language").isURIResource()) {
					languageTerm.setTextContent(sol.getResource("language").getLocalName());
					languageTerm.setAttribute("type", "code");
					languageTerm.setAttribute("authorityURI", sol.getResource("language").getNameSpace());
					languageTerm.setAttribute("valueURI", sol.getResource("language").toString());
					String temp = sol.getResource("language").toString().toLowerCase();
					if (temp.contains("rfc3066")) languageTerm.setAttribute("authority", "rfc3066");
					if (temp.contains("iso639-2b")) languageTerm.setAttribute("authority", "iso639-2b");
					if (temp.contains("iso639-3")) languageTerm.setAttribute("authority", "iso639-3");
					if (temp.contains("rfc4646")) languageTerm.setAttribute("authority", "rfc4646");
					if (temp.contains("rfc5646")) languageTerm.setAttribute("authority", "rfc5646");
				}
				language.appendChild(languageTerm);
				//			language.setAttribute("objectPart", "meta / object language"); TODO

				entry.appendChild(language);
			}
			catch (Exception e) {
				System.err.println(e);
			}
		}
	}
	
	public void sparqlPhysicalDescription(String uri, Element entry) {
		Element physicalDescription = DOCUMENT.createElement("physicalDescription");
		int i = 0;
		
		//extent
		String query = PREFIX
				+"\r\n"+ "SELECT * {"
				+"\r\n"+ "  <"+uri+"> void:triples|dct:extent ?extent ."
				+"\r\n"+ "  <"+uri+"> ?p ?extent ."
				+"\r\n"+ "}";


		ResultSet rs;
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, tdb.getDataset().getNamedModel("http://inputmodel"));
			rs = exec.execSelect();
		} catch (Exception e) {
			System.err.println(e);
			return;
		}
		
		while (rs.hasNext()) {
			QuerySolution sol = rs.next();

			Element extent = DOCUMENT.createElement("extent");
			if(sol.getResource("p").getLocalName().equals("triples")) {
				extent.setAttribute("unit", "triples");
				extent.setTextContent(sol.get("extent").toString());
			} else {
				String unit = sol.get("extent").toString().replaceAll("[\\d,\\.]", "");
				String number = sol.get("extent").toString().replaceAll("[^\\d,\\.]", "");
				extent.setAttribute("unit", unit);
				extent.setTextContent(number);
			}
			physicalDescription.appendChild(extent);
			i++;
		}
		
		//internetMediaType
		query = PREFIX
				+"\r\n"+ "SELECT * {"
				+"\r\n"+ "  <"+uri+"> (dct:fileFormat|dc:fileFormat|dc:format|dct:format)/(rdfs:label)? ?imt ."
				+"\r\n"+ "  FILTER(isLiteral(?imt)) ."
				+"\r\n"+ "}";

		try {
			QueryExecution exec = QueryExecutionFactory.create(query, tdb.getDataset().getNamedModel("http://inputmodel"));
			rs = exec.execSelect();
		} catch (Exception e) {
			System.err.println(e);
			return;
		}

		while (rs.hasNext()) {
			QuerySolution sol = rs.next();

			Element imt = DOCUMENT.createElement("internetMediaType");
			imt.setTextContent(sol.get("imt").toString());
			physicalDescription.appendChild(imt);
			i++;
		}

		if (i > 0) {
			entry.appendChild(physicalDescription);
		}
	}
	
	public void sparqlAbstract(String uri, Element entry) {
		String query = PREFIX
				+"\r\n"+ "SELECT * {"
				+"\r\n"+ "  <"+uri+"> dc:description|dct:description|dct:abstract|dct:tableOfContents|dct:provenance|metashare:versionInfo ?abstract ."
				+"\r\n"+ "  <"+uri+"> ?p ?abstract ."
				+"\r\n"+ "}";

		ResultSet rs;
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, tdb.getDataset().getNamedModel("http://inputmodel"));
			rs = exec.execSelect();
		} catch (Exception e) {
			System.err.println(e);
			return;
		}
		
		while (rs.hasNext()) {
			QuerySolution sol = rs.next();
			Element abs = DOCUMENT.createElement("abstract");
			abs.setTextContent(sol.get("abstract").toString());
			if (sol.get("abstract").isLiteral())
				if (sol.getLiteral("abstract").getLanguage() != null)
					if (!sol.getLiteral("abstract").getLanguage().equals(""))
						abs.setAttribute("lang", sol.getLiteral("abstract").getLanguage());
			abs.setAttribute("type", sol.getResource("p").getLocalName());

			entry.appendChild(abs);
		}
	}
	
	public void sparqlSubject(String uri, Element entry) {
		String query = PREFIX
				+"\r\n"+ "SELECT * {"
				+"\r\n"+ "  <"+uri+"> dc:subject|dct:subject ?subject ."
				+"\r\n"+ "  FILTER(!strstarts(str(?subject), 'http://data.linguistik.de/bll/bll-')) "
//				+"\r\n"+ "  <"+uri+"> ?p ?subject ."
				+"\r\n"+ "}";

		ResultSet rs;
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, tdb.getDataset().getNamedModel("http://inputmodel"));
			rs = exec.execSelect();
		} catch (Exception e) {
			System.err.println(e);
			return;
		}
		
		while (rs.hasNext()) {
			QuerySolution sol = rs.next();
			Element subject = DOCUMENT.createElement("subject");
			subject.setTextContent(sol.get("subject").toString());
			if (sol.get("subject").isLiteral())
				if (sol.getLiteral("subject").getLanguage() != null)
					if (!sol.getLiteral("subject").getLanguage().equals(""))
						subject.setAttribute("lang", sol.getLiteral("subject").getLanguage());
//			subject.setAttribute("type", sol.getResource("p").getLocalName());

			entry.appendChild(subject);
		}
	}
	
	public void sparqlIdentifier(String uri, Element entry) {
		//search for different urls
		String query = PREFIX
				+"\r\n"+ "SELECT * {"
				+"\r\n"+ "  <"+uri+"> dc:identifier|dct:identifier ?id ."
				+"\r\n"+ "  <"+uri+"> ?p ?id ."
				+"\r\n"+ "}";

		ResultSet rs;
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, tdb.getDataset().getNamedModel("http://inputmodel"));
			rs = exec.execSelect();
		} catch (Exception e) {
			System.err.println(e);
			return;
		}

		while (rs.hasNext()) {
			QuerySolution sol = rs.next();
			Element identifier = DOCUMENT.createElement("identifier");
			identifier.setTextContent(sol.get("id").toString());
			identifier.setAttribute("typeURI", sol.get("p").toString());
			if (sol.get("id").toString().startsWith("urn:")) {
				identifier.setAttribute("type", "urn");
			} else if (sol.get("id").isLiteral()) {
				identifier.setAttribute("type", "text");
			} else if (sol.get("id").isURIResource()) {
				identifier.setAttribute("type", "uri");
			}
			entry.appendChild(identifier);
		}
	}
	
	public void sparqlLocation(String uri, Element entry) {
		
		//append main url
		Element location = DOCUMENT.createElement("location");

		Element url = DOCUMENT.createElement("url");
		url.setTextContent(uri);
		if(uri.contains("linghub")) url.setAttribute("note", "linghub");
		location.appendChild(url);

		entry.appendChild(location);
		
		//search for different urls
		String query = PREFIX
				+"\r\n"+ "SELECT * {"
				+"\r\n"+ "  <"+uri+"> rdfs:seeAlso|owl:sameAs|foaf:homepage|dcat:accessURL|dcat:downloadURL ?url ."
				+"\r\n"+ "  <"+uri+"> ?p ?url ."
				+"\r\n"+ "}";

		ResultSet rs;
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, tdb.getDataset().getNamedModel("http://inputmodel"));
			rs = exec.execSelect();
		} catch (Exception e) {
			System.err.println(e);
			return;
		}
		
		while (rs.hasNext()) {
			QuerySolution sol = rs.next();
			if (sol.get("url").toString().startsWith("urn:")) {
				Element identifier = DOCUMENT.createElement("identifier");

				identifier.setTextContent(sol.get("url").toString());
				identifier.setAttribute("type", "urn");
				identifier.setAttribute("typeURI", sol.get("p").toString());

				entry.appendChild(identifier);
			} else {
				location = DOCUMENT.createElement("location");

				url = DOCUMENT.createElement("url");
				url.setTextContent(sol.get("url").toString());
				url.setAttribute("note", sol.get("p").toString());
				location.appendChild(url);

				entry.appendChild(location);
			}
		}
	}
	
	public void sparqlAccessCondition(String uri, Element entry) {
		String query = PREFIX
				+"\r\n"+ "SELECT * {"
				+"\r\n"+ "  <"+uri+"> dc:rights|dct:rights|dc:rightsHolder|dct:rightsHolder|dc:accessRights|dct:accessRights|dc:license|dct:license ?accessCondition ."
				+"\r\n"+ "  <"+uri+"> ?p ?accessCondition ."
				+"\r\n"+ "}";

		ResultSet rs;
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, tdb.getDataset().getNamedModel("http://inputmodel"));
			rs = exec.execSelect();
		} catch (Exception e) {
			System.err.println(e);
			return;
		}
		
		while (rs.hasNext()) {
			QuerySolution sol = rs.next();
			Element accessCondition = DOCUMENT.createElement("accessCondition");
			accessCondition.setTextContent(sol.get("accessCondition").toString());
			accessCondition.setAttribute("type", sol.getResource("p").getURI());

			entry.appendChild(accessCondition);
		}
	}
	
	public void sparqlNote(String uri, Element entry) {
		String query = PREFIX
				+"\r\n"+ "SELECT * {"
				+"\r\n"+ "  <"+uri+"> rdfs:comment ?note ."
				+"\r\n"+ "  <"+uri+"> ?p ?note ."
				+"\r\n"+ "}";

		ResultSet rs;
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, tdb.getDataset().getNamedModel("http://inputmodel"));
			rs = exec.execSelect();
		} catch (Exception e) {
			System.err.println(e);
			return;
		}
		
		while (rs.hasNext()) {
			QuerySolution sol = rs.next();
			Element abs = DOCUMENT.createElement("note");
			abs.setTextContent(sol.get("note").toString());
			if (sol.get("note").isLiteral())
				if (sol.getLiteral("note").getLanguage() != null)
					if (!sol.getLiteral("note").getLanguage().equals(""))
						abs.setAttribute("lang", sol.getLiteral("note").getLanguage());
			abs.setAttribute("type", sol.getResource("p").getLocalName());
			abs.setAttribute("typeURI", sol.getResource("p").getURI());

			entry.appendChild(abs);
		}
	}

	
	
	
//--------------------------STATIC PART----------------------------------------------------------------------
	public static final String PREFIX = ""
			+ "PREFIX metashare: <http://purl.org/ms-lod/MetaShare.ttl#>"
			+"\r\n"+"PREFIX dc:    <http://purl.org/dc/elements/1.1/>"
			+"\r\n"+"PREFIX prov:  <http://www.w3.org/ns/prov#>"
			+"\r\n"+"PREFIX foaf:  <http://xmlns.com/foaf/0.1/>"
			+"\r\n"+"PREFIX void:  <http://rdfs.org/ns/void#>"
			+"\r\n"+"PREFIX ontology: <http://linghub.lider-project.eu/ontology#>"
			+"\r\n"+"PREFIX lremap: <http://www.resourcebook.eu/lremap/owl/lremap_resource.owl#>"
			+"\r\n"+"PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#>"
			+"\r\n"+"PREFIX iso639: <http://www.lexvo.org/id/iso639-3/>"
			+"\r\n"+"PREFIX ex9:   <http://www.example.com#>"
			+"\r\n"+"PREFIX cmd:   <http://www.clarin.eu/cmd/>"
			+"\r\n"+"PREFIX dcat:  <http://www.w3.org/ns/dcat#>"
			+"\r\n"+"PREFIX bio:   <http://purl.org/ms-lod/BioServices.ttl>"
			+"\r\n"+"PREFIX dct:   <http://purl.org/dc/terms/>"
			+"\r\n"+"PREFIX owl:   <http://www.w3.org/2002/07/owl#>"
			+"\r\n"+"PREFIX xsd:   <http://www.w3.org/2001/XMLSchema#>"
			+"\r\n"+"PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
			+"\r\n"+"PREFIX dataid: <http://dataid.dbpedia.org/ns#>"
			+"\r\n"+"PREFIX odrl:  <http://www.w3.org/ns/odrl/2/>"
			+"\r\n"+"PREFIX ex:    <http://example.com/resources/>"
			+"\r\n"+"PREFIX lexvo: <http://lexvo.org/ontology#>"
			+"\r\n"+"PREFIX bibo:  <http://purl.org/ontology/bibo/>"
			+"\r\n"+"PREFIX annohub: <http://acoli.cs.uni-frankfurt.de/annohub#>";
	
	public static void main(String[] args) {
		// Overwrite config in Constants with local one, if exists.
    	try {
    		String lol = Paths.get(System.getProperty("user.dir")+"/FIDConfig_testUBExp.xml").toString();
    		if (Files.exists(Paths.get(System.getProperty("user.dir")+"/FIDConfig_testUBExp.xml")))
    			Constants.config = new Configurations().xml(System.getProperty("user.dir")+"/FIDConfig_testUBExp.xml");
    	} catch (ConfigurationException cex) {
    		cex.printStackTrace();
    		System.exit(0);
    	}
    	try {
    		UBmodsXMLExporter exp = new UBmodsXMLExporter();
			if (args.length == 0) {
//				exp.addXmlEntry("http://linghub.lider-project.eu/datahub/brown-corpus-in-rdf-nif");
//				exp.addXmlEntry("http://linghub.lider-project.eu/datahub/lemonuby");
//				exp.addXmlEntry("http://linghub.lider-project.eu/datahub/apertium-rdf-eo-en");
//				exp.addXmlEntry("http://linghub.lider-project.eu/metashare/e152863e92c211e28763000c291ecfc8e5ffae34b3514ec2aca79c6f2ed0a3cd");
				exp.readInputFile(new File("/media/ranis/exFAT/tmp/FidExport.ttl"));
//				exp.updateInputModel(); //get data from source URI (e.g. Linghub)
	    		exp.readBllOntology("/media/ranis/exFAT/_SVN/valian/intern/Virtuelle_Fachbibliothek/UB/OWL/BLLThesaurus/bll-ontology.rdf");
				exp.processInputModel();
				exp.writeXML("/media/ranis/exFAT/tmp/FidExport_new.mods.xml");
			} else {
				File inputfile = null;
				File outputfile = null;
				ArrayList<String> uris = new ArrayList<String>();
				for (int i = 0; i < args.length; i++) {
					switch (args[i]) {
					case "--help": 
						System.out.println("Arguments: ");
						System.out.println("  -i <inputfile> (created by Crawler)");
						System.out.println("  -o <outputfile>");
						System.out.println("  -url <url1> <url2> <url3> ... (manually defined Linghub sources)");
						System.out.println("  -onlythesaurus (does not include manually added terms of bll ontology)");
						break;
					case "-i": 
						i++;
						inputfile = new File(args[i]);
						continue;
					case "-o":
						i++;
						outputfile = new File(args[i]);
						continue;
					case "-url": 
						int j = i+1;
						for (j = i+1; j < args.length; j++) {
							if (args[j].matches("\\-.*")) {
								break;
							}
							uris.add(args[j]);
						}
						i = j;
						continue;
					default: 
						continue;
					}
				}
				if (outputfile == null) 
					throw new Exception("Outputfile must be specified");
				if (inputfile == null && uris.isEmpty()) 
					throw new Exception("Either specify an inputfile or at least one URL");
				if (inputfile != null) {
					System.out.println("Inputfile detected. Manual URLs will be ignored.");
					exp.readInputFile(inputfile);
					exp.updateInputModel();
				} else {	
					for (String uri : uris) {
						exp.addNewDatasetToInputModel(uri);
					}
					exp.updateInputModel();
				}
	    		exp.readBllOntology("/media/ranis/exFAT/_SVN/valian/intern/Virtuelle_Fachbibliothek/UB/OWL/BLLThesaurus/bll-ontology.rdf");
				exp.processInputModel();
				exp.writeXML(outputfile.getAbsolutePath());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	

}
