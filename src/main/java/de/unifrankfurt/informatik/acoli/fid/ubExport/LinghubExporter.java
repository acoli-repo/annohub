package de.unifrankfurt.informatik.acoli.fid.ubExport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;




public class LinghubExporter {

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
			+"\r\n"+"PREFIX ex:    <http://example.com/resources/>";
	
	public static void main(String[] args) {
		try {
			LinghubExporter exp = new LinghubExporter();
			if (args.length == 0) {
				exp.addXmlEntry("http://linghub.lider-project.eu/datahub/brown-corpus-in-rdf-nif");
				exp.addXmlEntry("http://linghub.lider-project.eu/datahub/lemonuby");
				exp.addXmlEntry("http://linghub.lider-project.eu/datahub/apertium-rdf-eo-en");
				exp.addXmlEntry("http://linghub.lider-project.eu/metashare/e152863e92c211e28763000c291ecfc8e5ffae34b3514ec2aca79c6f2ed0a3cd");
				exp.writeXML("testspass.xml");
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
					case "-onlythesaurus":
						exp.onlythesaurus = true;
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
				} else {	
					for (String uri : uris) {
						exp.addXmlEntry(uri);
					}
				}
				exp.writeXML(outputfile.getAbsolutePath());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private Document DOCUMENT;
	private DatasetAccessor memAccessor;
	private Dataset memDataset;
	private Model linghubModel;
	public boolean onlythesaurus = false;
	
	
	public LinghubExporter() throws Exception {
		//create new XML-Document (for reduced Output)
		DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
		DOCUMENT = documentBuilder.parse(new File(Constants.config.getString("SearchEngine.xmlExportBase")));
		memDataset = DatasetFactory.createMem();
		memAccessor = DatasetAccessorFactory.create(memDataset);
		linghubModel = ModelFactory.createDefaultModel();
	}
	
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
	
	public void readInputFile(File f) {
		Model inputModel = ModelFactory.createDefaultModel();
		try {
			inputModel.read(new FileInputStream(f), "");
		} catch (Exception e) {
			System.err.println(e+" ... trying TTL");
			try {
				RDFDataMgr.read(inputModel
						, new FileInputStream(f)
						, Lang.TURTLE
						);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
		}
		if (inputModel.isEmpty()) {
			return;
		}
		
		readBllOntology();
		
		memAccessor.add("http://inputmodel", inputModel);
		ResIterator subj_iter = inputModel.listSubjects();
		while (subj_iter.hasNext()) {
			addXmlEntry(subj_iter.next().getURI());
		}
	}

	public void readBllOntology() {
//		File f = new File("D:\\_SVN\\valian\\intern\\Virtuelle_Fachbibliothek\\UB\\OWL\\BLLThesaurus\\bll-ontology.ttl");
//		Model bll = ModelFactory.createDefaultModel();
//		try {
//			bll.read(new FileInputStream(f), "");
//		} catch (Exception e) {
//			System.out.println(e+" ... trying TTL");
//			try {
//				RDFDataMgr.read(bll
//						, new FileInputStream(f)
//						, Lang.TURTLE
//						);
//			} catch (FileNotFoundException e1) {
//				e1.printStackTrace();
//			}
//		}
//		memAccessor.add("http://bll", bll);
		
		try {
			SearchTerms terms = SearchTerms.getUniqueSearchTerms(false);
			terms.getDataset().begin(ReadWrite.READ);
			memAccessor.add("http://bll", terms.getDataset().getNamedModel("http://bll-ontology.ttl"));
			terms.getDataset().end();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Element addXmlEntry(String uri) {
		try {
			String[] urisegments = uri.split("#");
			readModel(urisegments[0]);
			Element entry = DOCUMENT.createElement("mods");
			generateXmlEntry(urisegments[0], entry, uri);
			DOCUMENT.getDocumentElement().appendChild(entry);
			return entry;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	private void readModel(String uri) {
		// TODO
		// DatasetManager deprecated ! 
	}
	
	
	/*
	private void readModel(String uri) {
		linghubModel.removeAll();
		try {
			if (!uri.matches(".*\\.\\w\\w+")) uri += ".ttl";
			linghubModel.read(uri, "TURTLE");
		} catch (Exception e) {
			System.out.println("Model <"+uri+"> could not be read. Trying to find local model...");
			//TODO ___HACK___ TO BE DELETED IN FUTURE RELEASES
			DatasetManager dm = new DatasetManager(Constants.config.getString("Database.TDB.HeaderDataDirectory"));
			for (FileInfo file : dm.getAll()) {
				if (!(file.getMetaDataURL() != null && file.getMetaDataURL().equals(uri)) && !(file.getLuceneID() != null && file.getLuceneID().equals(uri))) continue;
				if (file.getFormat() == ResourceFormat.CONLL) {
					try {
						linghubModel.read(Constants.config.getString("SearchEngine.PATH-CONLL")+IndexUtils.makeHashFolderName(file.getResourceID())+".meta.ttl", "TURTLE");
						System.out.println("Successfully read from local source.");
					} catch (Exception e1) {
						System.out.println("No local source available, generating result without metadata");
//						System.err.println(e1);
//						e1.printStackTrace();
					}
					break;
				}
			}
		}
	}
	*/
	
	
	private void generateXmlEntry(String uri, Element entry, String classifiedURI) throws Exception {
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
//		sparqlExtension(uri, entry); 			//NOTE: wird gebraucht??
		
		if (uri.equals(classifiedURI)) {
			if (!memAccessor.getModel("http://inputmodel").isEmpty()) {
				sparqlClassification(uri, entry);
			}
		}
		
		//rekursiver Aufruf für Subelemente. 
		sparqlRelatedItem(uri, entry, classifiedURI);
	}
	
	private void sparqlClassification(String uri, Node entryNode) throws Exception {
		//TODO OOOOOO
		String query = PREFIX
				+"\r\n"+ "SELECT ?bll (sum(?cnt1) as ?cnt) ?lbl ?internalID {"
				+"\r\n"+ "  GRAPH <http://inputmodel> {"
				+"\r\n"+ "    <"+uri+"> ?bll ?cnt1 ."
				+"\r\n"+ "  }"
				+"\r\n"+ "  GRAPH <http://bll> {"
				+"\r\n"+ "    OPTIONAL {?bll rdfs:label ?lbl . FILTER(LANG(?lbl) = 'de')}"
				+"\r\n"+ "    OPTIONAL {?bll rdf:value ?internalID}"
				+"\r\n"+ "  }"
				+"\r\n"+ "}"
				+"\r\n"+ "group by ?bll ?lbl ?internalID";
		ResultSet rs;
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, memDataset);
			rs = exec.execSelect();
		} catch (Exception e) {
			System.err.println(e);
			return;
		}
		while (rs.hasNext()) {
			QuerySolution sol = rs.next();
			if (onlythesaurus && !sol.getResource("bll").getLocalName().startsWith("bll-")) continue;
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
	
	private void sparqlRelatedItem(String uri, Node entryNode, String classifiedURI) {
		String query = PREFIX
				+"\r\n"+ "SELECT * {"
				+"\r\n"+ "  <"+uri+"> dcat:distribution ?relatedItem ."
				+"\r\n"+ "}";

		ResultSet rs;
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, linghubModel);
			rs = exec.execSelect();
		} catch (Exception e) {
			System.err.println(e);
			return;
		}
		
		while (rs.hasNext()) {
			QuerySolution sol = rs.next();
			Element relatedItem = DOCUMENT.createElement("relatedItem");
			try {
				generateXmlEntry(sol.getResource("relatedItem").getURI(), relatedItem, classifiedURI);
				entryNode.appendChild(relatedItem);
			} catch (Exception e) {
				System.err.println(e);
			}
		}
	}

	private void sparqlRecordInfo(String uri, Node entryNode) throws Exception {
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
			QueryExecution exec = QueryExecutionFactory.create(query, linghubModel);
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
	
	private void sparqlTitleInfo(String uri, Element entry) {
		String query = PREFIX
				+"\r\n"+ "SELECT distinct ?title {"
				+"\r\n"+ "  { "
				+"\r\n"+ "  <"+uri+"> dc:title|dct:title|dc:alternative|dct:alternative ?title ."
//				+"\r\n"+ "  <"+uri+"> ?p ?title ."
				+"\r\n"+ "  } "
				+"\r\n"+ "  UNION "
				+"\r\n"+ "  { "
				+"\r\n"+ "  ?obj foaf:primaryTopic <"+uri+"> ."
				+"\r\n"+ "  ?obj dc:title|dct:title|dc:alternative|dct:alternative ?title ."
//				+"\r\n"+ "  ?obj ?p ?title ."
				+"\r\n"+ "  } "
				+"\r\n"+ "}";

		ResultSet rs;
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, linghubModel);
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
			if (sol.getLiteral("title").getLanguage() != null)
				if (!sol.getLiteral("title").getLanguage().equals(""))
					title.setAttribute("lang", sol.getLiteral("title").getLanguage());
			titleInfo.appendChild(title);
//			titleInfo.setAttribute("type", sol.getResource("p").getLocalName());

			entry.appendChild(titleInfo);
		}
		
		// ____________additional generted titles (fallback)
		query = PREFIX
				+"\r\n"+ "SELECT ?url {"
				+"\r\n"+ "  <"+uri+"> dcat:accessURL ?url ."
				+"\r\n"+ "  <"+uri+"> ?p ?url ."
				+"\r\n"+ "}";

		rs = null;
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, linghubModel);
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
		titleInfo.setAttribute("otherType", "linghubURL");
		titleInfo.appendChild(title);

		entry.appendChild(titleInfo);
	}
	
	private void sparqlName(String uri, Element entry) {
		String query = PREFIX
				+"\r\n"+ "SELECT ?role ?nameType ?namePart {"
				+"\r\n"+ "  <"+uri+"> (dc:creator|dct:creator|dct:contributor|dc:contributor) ?entity ."
				+"\r\n"+ "  <"+uri+"> ?role ?entity ."
				+"\r\n"+ "  ?entity  (!ex:p)* ?temp ."
				+"\r\n"+ "  ?temp   ?nameType ?namePart ."
				+"\r\n"+ "  FILTER(isLiteral(?namePart))"
				+"\r\n"+ "}";

		ResultSet rs;
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, linghubModel);
			rs = exec.execSelect();
		} catch (Exception e) {
			System.err.println(e);
			return;
		}
		
		while (rs.hasNext()) {
			QuerySolution sol = rs.next();
			Element name = DOCUMENT.createElement("name");

			Element namePart = DOCUMENT.createElement("namePart");
			namePart.setTextContent(sol.getLiteral("namePart").getString());
//			namePart.setAttribute("type", sol.getResource("nameType").getLocalName());
			name.appendChild(namePart);
			
			Element role = DOCUMENT.createElement("role");
			Element roleTerm = DOCUMENT.createElement("roleTerm");
			roleTerm.setTextContent(sol.getResource("role").getLocalName());
			roleTerm.setAttribute("type", "text");
			roleTerm.setAttribute("authorityURI", sol.getResource("role").getNameSpace());
			roleTerm.setAttribute("valueURI", sol.getResource("role").getURI());
			role.appendChild(roleTerm);
			name.appendChild(role);

			entry.appendChild(name);
		}
	}
	
	private void sparqlTypeOfResource(String uri, Element entry) {
		Element typeOfResource = DOCUMENT.createElement("typeOfResource");
		typeOfResource.setTextContent("software, multimedia");
		entry.appendChild(typeOfResource);
	}
	
	private void sparqlGenre(String uri, Element entry) {
		Element genre = DOCUMENT.createElement("genre");
		genre.setTextContent("Dataset");
		entry.appendChild(genre);
	}
	
	private void sparqlOriginInfo(String uri, Element entry) {
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
			QueryExecution exec = QueryExecutionFactory.create(query, linghubModel);
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
	
	private void sparqlLanguage(String uri, Element entry) {
		String query = PREFIX
				+"\r\n"+ "SELECT * {"
				+"\r\n"+ "  <"+uri+"> dc:language|dct:language ?language ."
				+"\r\n"+ "  <"+uri+"> ?p ?language ."
				+"\r\n"+ "}";

		ResultSet rs;
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, linghubModel);
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
	
	private void sparqlPhysicalDescription(String uri, Element entry) {
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
			QueryExecution exec = QueryExecutionFactory.create(query, linghubModel);
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
				+"\r\n"+ "  <"+uri+"> dc:format|dct:format|(dct:format/rdfs:label) ?imt ."
				+"\r\n"+ "  FILTER(isLiteral(?imt)) ."
				+"\r\n"+ "}";

		try {
			QueryExecution exec = QueryExecutionFactory.create(query, linghubModel);
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
	
	private void sparqlAbstract(String uri, Element entry) {
		String query = PREFIX
				+"\r\n"+ "SELECT * {"
				+"\r\n"+ "  <"+uri+"> dc:description|dct:description|dct:abstract|dct:tableOfContents|dct:provenance|metashare:versionInfo ?abstract ."
				+"\r\n"+ "  <"+uri+"> ?p ?abstract ."
				+"\r\n"+ "}";

		ResultSet rs;
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, linghubModel);
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
	

	private void sparqlSubject(String uri, Element entry) {
		String query = PREFIX
				+"\r\n"+ "SELECT * {"
				+"\r\n"+ "  <"+uri+"> dc:subject|dct:subject ?subject ."
//				+"\r\n"+ "  <"+uri+"> ?p ?subject ."
				+"\r\n"+ "}";

		ResultSet rs;
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, linghubModel);
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
	
	private void sparqlIdentifier(String uri, Element entry) {
		//search for different urls
		String query = PREFIX
				+"\r\n"+ "SELECT * {"
				+"\r\n"+ "  <"+uri+"> dc:identifier|dct:identifier ?id ."
				+"\r\n"+ "  <"+uri+"> ?p ?id ."
				+"\r\n"+ "}";

		ResultSet rs;
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, linghubModel);
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
	
	private void sparqlLocation(String uri, Element entry) {
		
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
			QueryExecution exec = QueryExecutionFactory.create(query, linghubModel);
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
	
	private void sparqlAccessCondition(String uri, Element entry) {
		String query = PREFIX
				+"\r\n"+ "SELECT * {"
				+"\r\n"+ "  <"+uri+"> dc:rights|dct:rights|dc:rightsHolder|dct:rightsHolder|dc:accessRights|dct:accessRights|dc:license|dct:license ?accessCondition ."
				+"\r\n"+ "  <"+uri+"> ?p ?accessCondition ."
				+"\r\n"+ "}";

		ResultSet rs;
		try {
			QueryExecution exec = QueryExecutionFactory.create(query, linghubModel);
			rs = exec.execSelect();
		} catch (Exception e) {
			System.err.println(e);
			return;
		}
		
		while (rs.hasNext()) {
			QuerySolution sol = rs.next();
			Element accessCondition = DOCUMENT.createElement("accessCondition");
			accessCondition.setTextContent(sol.get("accessCondition").toString());

			entry.appendChild(accessCondition);
		}
	}
}
