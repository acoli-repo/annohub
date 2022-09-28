package de.unifrankfurt.informatik.acoli.fid.ubExport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.tdb.TDB;

import de.unifrankfurt.informatik.acoli.fid.linghub.FusekiTDB;




/**
 * Maintains complete OLiA ontologies inside a separate TDB store. 
 * Is capable of creating searchTerms file for efficient classification of resources.
 * The current classification ontology is BLL. All BLL terms will be mapped to corresponding terms 
 * in any other OLiA Ontology via their linking models.
 * searchTerms can be created in two manners:
 * - regular searchterms file maps all concepts which have some kind of sibling or ancestor relationship throughout all models.
 * - concise searchterms file is a substract of the regular file which removes all concepts which can be mapped as ancestors 
 *   inside the classification ontology (BLL). 
 *   This file can be used to create the most precise results by removing more generic terms.
 */
public class SearchTerms {

	private static SearchTerms staticTerms;

	public static SearchTerms getUniqueSearchTerms(boolean refresh) throws Exception {
		//If OLiA Model needs refresh, delete TDB-Folder BEFORE it is loaded.
		if (refresh) {
			if (staticTerms != null) {
				throw new Exception("OLiA TDB already loaded. Cannot be refreshed during runtime.");
			} 
			if (!Constants.removeFolderContent(Constants.config.getString("SearchEngine.PATH-OLiA-TDB"))) {
				throw new Exception("Unable to delete OLiA TDB Folder. Please delete manually.");
			}
		}
		
		//create SearchTerms 
		if (staticTerms == null) {
			staticTerms = new SearchTerms();
		}
		
		if (staticTerms.getOliaFull().isEmpty()) {
			refresh = true;
		}
		
		String pathST = Constants.config.getString("SearchEngine.PATH-SearchTerms");
		String pathSTC = Constants.config.getString("SearchEngine.PATH-SearchTermsConcise");
		String sourceNS = Constants.config.getString("PrefixURIs.bll-owl");
		
		if (refresh) {
			staticTerms.refreshOliaModel();
			staticTerms.generateSearchTerms(sourceNS);
			try {
				staticTerms.saveSearchTerms(new File(pathST));
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
			staticTerms.generateConciseTermModel();
			try {
				staticTerms.saveSearchTermsConcise(new File(pathSTC));
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
		} else if (staticTerms.getTermModel().isEmpty()) {
			try {
				staticTerms.loadSearchTerms(new File(pathST));
				staticTerms.loadSearchTermsConcise(new File(pathSTC));
			} catch (Exception e) {
				System.err.println(e);
				System.err.println("... reconstructing searchTerms");
				staticTerms.generateSearchTerms(sourceNS);
				try {
					staticTerms.saveSearchTerms(new File(pathST));
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}
				staticTerms.generateConciseTermModel();
				try {
					staticTerms.saveSearchTermsConcise(new File(pathSTC));
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}
			}
		}
		
		return staticTerms;
	}
	
	

	private FusekiTDB tdb;
	
	private boolean conciseMode = false;
	
	private Model termModel;
	private Model termModelConcise;
	
	private String[] namespaces;
	private String[] tagPredicates;
	
	
	public SearchTerms() {
//		accessor = DatasetAccessorFactory.create(DatasetFactory.createMem());
		tdb = new FusekiTDB(Constants.config.getString("SearchEngine.PATH-OLiA-TDB"));
		termModel = ModelFactory.createDefaultModel();
		termModelConcise = ModelFactory.createDefaultModel();
		try {
			conciseMode = Constants.config.getBoolean("SearchEngine.conciseSearchTerms");
		} catch (Exception e) {
			conciseMode = false;
		}
	}
	
	public String[] getNamespaces() {
		return namespaces;
	}
	
	public String[] getTagPredicates() {
		if (tagPredicates == null)
			searchTagPredicates();
		return tagPredicates;
	}
	
	public Model getTermModel() {
		if (conciseMode) {
			return termModelConcise;
		}
		return termModel;
	}
	
	public Dataset getDataset() {
		return tdb.getDataset();
	}
	
	public Model getOliaFull() {
		tdb.getDataset().begin(ReadWrite.READ);
		Model result = tdb.getDataset().getNamedModel("urn:x-arq:UnionGraph");
		tdb.getDataset().end(); 
		return result;
	}
	
	/**
	 * Write contents of termModel to ttl-File:
	 * 		sourceNS:term equivalentTo otherNS:term
	 * 
	 * @param f: the target file
	 * @throws FileNotFoundException 
	 */
	public void saveSearchTerms(File f) throws FileNotFoundException {
		if (termModel != null) {
			RDFDataMgr.write(
					new FileOutputStream(f), 
					termModel, 
					RDFFormat.NTRIPLES);
		}
	}
	
	/**
	 * Write contents of termMap to ttl-File:
	 * 		sourceNS:term equivalentTo otherNS:term
	 * 
	 * @param f: the target file
	 * @throws FileNotFoundException 
	 */
	public void saveSearchTermsConcise(File f) throws FileNotFoundException {
		if (termModelConcise != null) {
			RDFDataMgr.write(
					new FileOutputStream(f), 
					termModelConcise, 
					RDFFormat.NTRIPLES);
		}
	}
	
	/**
	 * Loads contents of termMap from ttl-File:
	 * 		sourceNS:term equivalentTo otherNS:term
	 * 
	 * @param f: the source file
	 * @throws FileNotFoundException 
	 */
	public void loadSearchTerms(File f) throws FileNotFoundException {
		if (termModel != null) {
			termModel.removeAll();
			RDFDataMgr.read(termModel, new FileInputStream(f), Lang.NTRIPLES);
//			termModel.read(new FileInputStream(f), "");
			searchNS();
		}
	}
	
	/**
	 * Loads contents of termMap from ttl-File:
	 * 		sourceNS:term equivalentTo otherNS:term
	 * 
	 * @param f: the source file
	 * @throws FileNotFoundException 
	 */
	public void loadSearchTermsConcise(File f) throws FileNotFoundException {
		if (termModelConcise != null) {
			termModelConcise.removeAll();
			RDFDataMgr.read(termModelConcise, new FileInputStream(f), Lang.NTRIPLES);
//			termModel.read(new FileInputStream(f), "");
			searchNS();
		}
	}
	
	public void loadDefaultSearchTerms() {
		String sourceNS = Constants.config.getString("PrefixURIs.bll-owl");
		String path = Constants.config.getString("SearchEngine.PATH-SearchTerms");
		try {
			loadSearchTerms(new File(path));
		} catch (Exception e) {
			//Either FileNotFound or NullPointer
			System.err.println(e);
			refreshOliaModel();
			generateSearchTerms(sourceNS);
			try {
				saveSearchTerms(new File(path));
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	public void refreshOliaModel() {
		System.out.println("Updating OLiA TDB ...");
		for (File f : Constants.OLIA_PATHS) {
			traverseFileTree(f);
		}
		
//		accessor.putModel(m);
	}
	
	private void traverseFileTree(File f) {
		if (f.isDirectory()) {
			if (Constants.config.getArray(String.class, "OliaSVN.exception") != null) {
				for (String s:(String[])Constants.config.getArray(String.class, "OliaSVN.exception")) {
					if (f.getName().equals(s)) return;
				}
			}
			for (File sf : f.listFiles()) {
				traverseFileTree(sf);
			}
		} else {
			if (f.isFile() && f.canRead() && (
					f.getName().endsWith(".owl") || 
					f.getName().endsWith(".ttl") || 
					f.getName().endsWith(".rdf")
					)) {
				tdb.getDataset().begin(ReadWrite.WRITE); 
				Model m = tdb.getDataset().getNamedModel("http://"+f.getName());
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
		}
	}
	
	/**
	 * Searches for all classes and instances in the targetDS
	 * linked to any instance in the source namespace.
	 * 
	 * writes Results to termMap
	 * 
	 * @param sourceNS: the source namespace
	 * @throws UnsupportedEncodingException 
	 * @throws FileNotFoundException 
	 */
	public void generateSearchTerms(String sourceNS) {
		
		if (!termModel.isEmpty()) {
			termModel.removeAll();
		}
		System.out.println("Generating SearchTerms ...");
		System.out.println("Scanning Annotation Models ...");
		termModel.add(searchAMs(sourceNS));
		System.out.println("Scanning External Reference Models ...");
		termModel.add(searchERMs(sourceNS));
		
		searchNS();
		
	}
	
	private void searchNS() {
		final String query = Constants.getSparqlPrefix()
				+ "\r\n" + "select distinct (afn:namespace(?x) as ?ns)"
				+ "\r\n" + "{"
				+ "\r\n" + "{"
				+ "\r\n" + "?s1 ?p1 ?x"
				+ "\r\n" + "}"
				+ "\r\n" + "UNION"
				+ "\r\n" + "{"
				+ "\r\n" + "?x ?p2 ?o2"
				+ "\r\n" + "}"
				+ "\r\n" + "}";
//		System.out.println(query);

		QueryExecution exec = QueryExecutionFactory.create(query, termModel);

		ResultSet rs = exec.execSelect();
		ArrayList<String> list = new ArrayList<String>();
		while (rs.hasNext()) {
			QuerySolution qs = rs.next();
			if (qs.get("ns") != null)
				list.add(qs.get("ns").toString());
		}
		namespaces = list.toArray(new String[10]);
	}
	
	private void searchTagPredicates() {
		final String query = Constants.getSparqlPrefix()
				+ "\r\n" + "select distinct ?p"
				+ "\r\n" + "{"
				+ "\r\n" + "?p rdfs:subPropertyOf* olia-system:hasTag"
				+ "\r\n" + "}";

		tdb.getDataset().begin(ReadWrite.READ);
		QueryExecution exec = QueryExecutionFactory.create(query, tdb.getDataset());
		exec.getContext().set(TDB.symUnionDefaultGraph, true) ;
		ResultSet rs = exec.execSelect();
		ArrayList<String> list = new ArrayList<String>();
		while (rs.hasNext()) {
			list.add(rs.next().get("p").toString());
		}
		tdb.getDataset().end(); 
		
		tagPredicates = list.toArray(new String[0]);
	}
	
	public Model generateConciseTermModel() {
		System.out.println("Generating concise SearchTerms ...");
		tdb.getDataset().begin(ReadWrite.WRITE); 
		tdb.getDataset().addNamedModel("http://termmodel", termModel);
		tdb.getDataset().commit();
		tdb.getDataset().end();
		final String query = "" 
				+ "\r\n" + Constants.getSparqlPrefix()
				+ "\r\n" + "CONSTRUCT {"
				+ "\r\n" + "?bll owl:equivalentTo ?am ."
				+ "\r\n" + "?inst rdf:type ?bll ."
				+ "\r\n" + "} {"
					+ "\r\n" + "select ?bll ?am ?inst"
					+ "\r\n" + "{"
					+ "\r\n" + "GRAPH <http://termmodel> {"
					+ "\r\n" + "{"
					+ "\r\n" + "?bll owl:equivalentTo ?am ."
					+ "\r\n" + "?bll1 owl:equivalentTo ?am ."
					+ "\r\n" + "} UNION {"
					+ "\r\n" + "?inst rdf:type ?bll ."
					+ "\r\n" + "?inst rdf:type ?bll1 ."
					+ "\r\n" + "}"
					+ "\r\n" + "}"
					+ "\r\n" + "FILTER EXISTS {"
					+ "\r\n" + "GRAPH <http://bll-ontology.ttl> {"
					+ "\r\n" + "?bll1 (rdfs:subClassOf|owl:intersectionOf|owl:unionOf|owl:equivalentClass|^owl:equivalentClass)*/rdfs:subClassOf/(rdfs:subClassOf|owl:intersectionOf|owl:unionOf|owl:equivalentClass|^owl:equivalentClass)* ?bll"
					+ "\r\n" + "}"
					+ "\r\n" + "}"
					+ "\r\n" + "}"
				+ "\r\n" + "}";

//		System.out.println(query);
		
		tdb.getDataset().begin(ReadWrite.READ);
		QueryExecution exec = QueryExecutionFactory.create(query, tdb.getDataset());
//		exec.getContext().set(TDB.symUnionDefaultGraph, true) ;
		Model result = exec.execConstruct();
		tdb.getDataset().end(); 
		
		tdb.getDataset().begin(ReadWrite.WRITE); 
		tdb.getDataset().removeNamedModel("http://termmodel");
		tdb.getDataset().commit();
		tdb.getDataset().end();
		
		termModelConcise.removeAll();
		termModelConcise.add(termModel);
		termModelConcise.remove(result);
		
		return termModelConcise;
	}
	
	private Model searchAMs(String sourceNS) {
		final String query = "prefix : <"+sourceNS+">" 
				+ "\r\n" + Constants.getSparqlPrefix()
				+ "\r\n" + "CONSTRUCT {"
				+ "\r\n" + "?bll1 owl:equivalentTo ?am2 ."
				+ "\r\n" + "?inst rdf:type ?bll1 ."
				+ "\r\n" + "} {"
					+ "\r\n" + "select ?bll1 ?am2 ?inst ?p ?o "
					+ "\r\n" + "{"
//					+ "\r\n" + "?bll1 (skos:broader|rdfs:subClassOf|owl:equivalentClass|^owl:equivalentClass)+ bll-skos:BLLConcept ."
//					+ "\r\n" + "?bll2 (skos:broader|rdfs:subClassOf|owl:equivalentClass|^owl:equivalentClass)* ?bll1 ."
					+ "\r\n" + "?bll1 (rdfs:subClassOf|owl:intersectionOf|owl:unionOf|(rdf:rest*/rdf:first)|owl:equivalentClass|^owl:equivalentClass|(^rdf:first/^rdf:rest*/^owl:unionOf/^owl:equivalentClass))+ bll-owl:BLLFormalizedConcept ."
					+ "\r\n" + "?bll2 (rdfs:subClassOf|owl:intersectionOf|owl:unionOf|(rdf:rest*/rdf:first)|owl:equivalentClass|^owl:equivalentClass|(^rdf:first/^rdf:rest*/^owl:unionOf/^owl:equivalentClass))* ?bll1 ."
					+ "\r\n" + ""
					+ "\r\n" + "# find all linked terms in OLiA reference model (rm1) and their respective subterms (rm2)"
					+ "\r\n" + "?bll2 rdfs:subClassOf/((owl:intersectionOf|owl:unionOf)/rdf:rest*/rdf:first)* ?rm1 ."
					+ "\r\n" + "?rm2 (rdfs:subClassOf|owl:intersectionOf|owl:unionOf|owl:equivalentClass|^owl:equivalentClass)* ?rm1 ."
					+ "\r\n" + ""
					+ "\r\n" + "FILTER ( regex(str(?bll1), '"+Constants.BLL_REGEX+"', 'i')) ."
					+ "\r\n" + "FILTER ( regex(str(?bll2), '"+Constants.BLL_REGEX+"', 'i')) ."
					+ "\r\n" + "FILTER ( regex(str(?rm1), '"+Constants.OLIA_REGEX+"', 'i')) ."
					+ "\r\n" + "FILTER ( regex(str(?rm2), '"+Constants.OLIA_REGEX+"', 'i')) ."
//					+ "\r\n" + ""
//					+ "\r\n" + "# find all direct links to AMs and external RMs (inverse path)"
//					+ "\r\n" + "?am1 (rdfs:subClassOf/((owl:intersectionOf|owl:unionOf)/rdf:rest*/rdf:first)*)|((^owl:intersectionOf|^owl:unionOf)*/^rdfs:subClassOf) ?rm2 ."
//					+ "\r\n" + ""
					+ "\r\n" + "# find all subclasses of linked terms within their models."
					+ "\r\n" + "?am2 (rdfs:subClassOf|owl:intersectionOf|owl:unionOf|(rdf:rest*/rdf:first)|owl:equivalentClass|^owl:equivalentClass)* ?rm2 ."
					+ "\r\n" + ""
//					+ "\r\n" + "FILTER (!regex(str(?am1), '"+Constants.BLL_REGEX+"', 'i')) ."
//					+ "\r\n" + "FILTER (!regex(str(?am1), '"+Constants.OLIA_REGEX+"', 'i')) ."
					+ "\r\n" + "FILTER (!regex(str(?am2), '"+Constants.BLL_REGEX+"', 'i')) ."
//					+ "\r\n" + "FILTER (!regex(str(?am2), '"+Constants.OLIA_REGEX+"', 'i')) ."
					+ "\r\n" + ""
					+ "\r\n" + "# find all further specifications of am2 terms"
					+ "\r\n" + "OPTIONAL {"
					+ "\r\n" + "  ?inst rdf:type ?am2 ."
					+ "\r\n" + "} ."
					+ "\r\n" + "}"
				+ "\r\n" + "}";

//		System.out.println(query);
		
		tdb.getDataset().begin(ReadWrite.READ);
		QueryExecution exec = QueryExecutionFactory.create(query, tdb.getDataset());
		exec.getContext().set(TDB.symUnionDefaultGraph, true) ;
		Model result = exec.execConstruct();
		tdb.getDataset().end(); 
		
		return result;
	}
	
	private Model searchERMs(String sourceNS) {
		final String query = "prefix : <"+sourceNS+">" 
				+ "\r\n" + Constants.getSparqlPrefix()
				+ "\r\n" + "CONSTRUCT {"
				+ "\r\n" + "?bll1 owl:equivalentTo ?erm2 ."
				+ "\r\n" + "?inst rdf:type ?bll1 . ?inst olia-system:hasTag ?tag"
				+ "\r\n" + "} {"
					+ "\r\n" + "select ?bll1 ?erm2 ?inst ?p ?o ?tag"
					+ "\r\n" + "{"
//					+ "\r\n" + "?bll1 (skos:broader|rdfs:subClassOf|owl:equivalentClass|^owl:equivalentClass)+ bll-skos:BLLConcept ."
//					+ "\r\n" + "?bll2 (skos:broader|rdfs:subClassOf|owl:equivalentClass|^owl:equivalentClass)* ?bll1 ."
					+ "\r\n" + "?bll1 (rdfs:subClassOf|owl:intersectionOf|owl:unionOf|(rdf:rest*/rdf:first)|owl:equivalentClass|^owl:equivalentClass|(^rdf:first/^rdf:rest*/^owl:unionOf/^owl:equivalentClass))+ bll-owl:BLLFormalizedConcept ."
					+ "\r\n" + "?bll2 (rdfs:subClassOf|owl:intersectionOf|owl:unionOf|(rdf:rest*/rdf:first)|owl:equivalentClass|^owl:equivalentClass|(^rdf:first/^rdf:rest*/^owl:unionOf/^owl:equivalentClass))* ?bll1 ."
					+ "\r\n" + ""
					+ "\r\n" + "# find all linked terms in OLiA reference model (rm1) and their respective subterms (rm2)"
					+ "\r\n" + "?bll2 rdfs:subClassOf/((owl:intersectionOf|owl:unionOf)/rdf:rest*/rdf:first)* ?rm1 ."
					+ "\r\n" + "?rm2 (rdfs:subClassOf|owl:intersectionOf|owl:unionOf|owl:equivalentClass|^owl:equivalentClass)* ?rm1 ."
					+ "\r\n" + ""
					+ "\r\n" + "FILTER ( regex(str(?bll1), '"+Constants.BLL_REGEX+"', 'i')) ."
					+ "\r\n" + "FILTER ( regex(str(?bll2), '"+Constants.BLL_REGEX+"', 'i')) ."
					+ "\r\n" + "FILTER ( regex(str(?rm1), '"+Constants.OLIA_REGEX+"', 'i')) ."
					+ "\r\n" + "FILTER ( regex(str(?rm2), '"+Constants.OLIA_REGEX+"', 'i')) ."
					+ "\r\n" + ""
					+ "\r\n" + "# find all direct links to AMs and external RMs (inverse path)"
					+ "\r\n" + "?rm2 rdfs:subClassOf/((owl:intersectionOf|owl:unionOf)/rdf:rest*/rdf:first)* ?erm1."
					+ "\r\n" + "FILTER (!regex(str(?erm1), '"+Constants.BLL_REGEX+"', 'i')) ."
					+ "\r\n" + "FILTER (!regex(str(?erm1), '"+Constants.OLIA_REGEX+"', 'i')) ."
					+ "\r\n" + "FILTER (!regex(str(?erm1), 'http://www.w3.org/2002/07/owl#.*', 'i')) ."
					+ "\r\n" + ""
					+ "\r\n" + "# find all subclasses of linked terms within their models."
					+ "\r\n" + "?erm2 (rdfs:subClassOf|owl:intersectionOf|owl:unionOf|owl:equivalentClass|^owl:equivalentClass)* ?erm1 ."
					+ "\r\n" + "FILTER ( afn:namespace(?erm2) = afn:namespace(?erm1)) ."
//					+ "\r\n" + "FILTER (!regex(str(?erm2), '"+Constants.BLL_REGEX+"', 'i')) ."
//					+ "\r\n" + "FILTER (!regex(str(?erm2), '"+Constants.OLIA_REGEX+"', 'i')) ."
//					+ "\r\n" + "FILTER (!regex(str(?erm2), 'http://www.w3.org/2002/07/owl#.*', 'i')) ."
					+ "\r\n" + ""
					+ "\r\n" + "# find all further specifications of am2 terms"
					+ "\r\n" + "OPTIONAL {"
					+ "\r\n" + "  {?inst rdf:type ?erm2 .}"
					+ "\r\n" + "  UNION"
					+ "\r\n" + "  {?erm2 dcr:datcat|rdfs:isDefinedBy ?inst . bind (afn:localname(?inst) as ?tag)}"
					+ "\r\n" + "} ."
					+ "\r\n" + "}"
				+ "\r\n" + "}";

//		System.out.println(query);
		
		tdb.getDataset().begin(ReadWrite.READ);
		QueryExecution exec = QueryExecutionFactory.create(query, tdb.getDataset());
		exec.getContext().set(TDB.symUnionDefaultGraph, true) ;
		Model result = exec.execConstruct();
		tdb.getDataset().end(); 
		
		return result;
	}

	
}
