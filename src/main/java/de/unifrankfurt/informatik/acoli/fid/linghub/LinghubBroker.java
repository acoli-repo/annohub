package de.unifrankfurt.informatik.acoli.fid.linghub;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;

import com.bigdata.rdf.sail.BigdataSail;
import com.bigdata.rdf.sail.BigdataSailRepository;

import de.unifrankfurt.informatik.acoli.fid.types.ResourceFormat;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.util.BlazegraphUtils;
import de.unifrankfurt.informatik.acoli.fid.util.IndexUtils;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;


	/**
	 * Class for collecting RDF data sources from linghub RDF file
	 * @author Frank
	 *
	 */
	public class LinghubBroker {
	
	
	public FusekiTDB linghubDump = null; // not used because import linghub data fails
	Repository blazegraphRepository = null;

		
	public void shutdownRepository() {	
		try {
			blazegraphRepository.shutDown();
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
	}
	

	/**
	 * Constructor
	 * @param linghubDBDirectory Directory of DB database which contains Linghub dump
	 */
	public LinghubBroker(XMLConfiguration fidConfig) {
		
		// older implementation with Jena
		//TDB.getContext().set(TDB.symUnionDefaultGraph, true) ;
		//linghubDump = new FusekiTDB(linghubDBDirectory);
		
		if (fidConfig.containsKey("Databases.Blazegraph.loadProperties")) {
		try {
						
			Properties properties = new Properties();
			properties.load(new FileReader(new File(fidConfig.getString("Databases.Blazegraph.loadProperties"))));
			System.out.println("\nStarting Blazegraph database");
			System.out.println("using Databases.Blazegraph.loadProperties: "+fidConfig.getString("Databases.Blazegraph.loadProperties")+"\n");
			// instantiate a sail
			final BigdataSail sail = new BigdataSail(properties);
			blazegraphRepository = new BigdataSailRepository(sail);
			blazegraphRepository.initialize();
			System.out.println("Blazegraph Linghub database initialized !");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		} else {
			if (fidConfig.containsKey("Linghub.linghubQueries") && fidConfig.getString("Linghub.linghubQueries").equals("true")) {
				System.out.println("Error : config parameter Databases.Blazegraph.loadProperties missing !");
			}
		}
	}
	
	
	
	/**
	 * Query the local Linghub RDF file and generate a list of pairs with (linghub-metadata-url, linghub-data-url)
	 * The sparql variables url? for the dataUrl and resource? for the metaDataUrl are assumed.
	 * @param String stores SPARQL queries to be executed
	 * @return LinghubResources Container for (MetadataUrl, DataUrl) pairs
	 */
	public ArrayList <TupleQueryResult> queryLinghubResourcesLocal (String linghubQuery) {
		
		Utils.debug("Querying local Linghub dump !");
		//Utils.debug(linghubQuery);
		
		String countTriples = "SELECT (count(*) as ?triples)  WHERE {?s ?p ?o .}";
		TupleQueryResult countTriplesResult = BlazegraphUtils.exeSelQuery(blazegraphRepository, countTriples);
		try {
			System.out.println("Linghub dump contains "+countTriplesResult.next().getValue("triples")+ " triples");
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
		}
		
		ArrayList <TupleQueryResult> result = new ArrayList <TupleQueryResult> ();
		if (linghubQuery.isEmpty()) {
			System.out.println("Error : no query was provided in the configuration file!");
			return result;
		}
		
		String [] queries = linghubQuery.split("### Querystart ###");
		
		int i = 1;
		for (String q : queries) {
			
			System.out.println("running query "+(i++)+":\n"+q);
			
			if (q.trim().isEmpty()) continue;
			result.add(BlazegraphUtils.exeSelQuery(blazegraphRepository, q.trim()));
	    }
		
		System.out.println("Total query results: "+result.size());
		return result;
	}
	
	
	/** Code backup !
	 * Query the local Linghub RDF file and generate a list of pairs with (linghub-metadata-url, linghub-data-url)
	 * The sparql variables url? for the dataUrl and resource? for the metaDataUrl are assumed.
	 * @param String stores SPARQL queries to be executed
	 * @return LinghubResources Container for (MetadataUrl, DataUrl) pairs
	 * @deprecated
	 */
	public ArrayList <ResultSet> queryLinghubResourcesLocalTDB (String linghubQuery) {
		
		//Utils.debug("Querying local linghub dump !");
		
		ArrayList <ResultSet> result = new ArrayList <ResultSet> ();
		if (linghubQuery.isEmpty()) return result;
		
		String [] queries = linghubQuery.split("### Querystart ###");
		
		for (String q : queries) {

			if (q.trim().isEmpty()) continue;
			result.add(linghubDump.query(q.trim()));
	    	}
		return result;
	}
	
	
	
	/**
	 * (Experimental) Query linghub.org online and generate the list of pairs with (linghub-metadata-url, linghub-data-url)
	 * The sparql variables url? for the dataUrl and resource? for the metaDataUrl are assumed.
	 * @param String stores SPARQL queries to be executed
	 * @return LinghubResources Container for (MetadataUrl, DataUrl) pairs
	 */
	public ArrayList <ResultSet> queryLinghubResourcesOnline (String linghubQuery) {

		ArrayList <ResultSet> result = new ArrayList <ResultSet> ();
		if (linghubQuery.isEmpty()) return result;

		String [] queries = linghubQuery.split("### Querystart ###");
		Utils.debug(queries.length);
		for (String q : queries) {

			if (q.trim().isEmpty()) continue;
			result.add(queryLinghubOrg(q.trim()));
		}
		return result;
	}

	
	
	/** Sparql http://linghub.org/sparql endpoint
	 * @param linghubQuery queryString
	 * @return ResultSet with triples
	 */
	private ResultSet queryLinghubOrg(String linghubQuery) {
		
		ResultSet results = null;
		Query q = QueryFactory.create(linghubQuery);
		QueryEngineHTTP qexec = new QueryEngineHTTP("http://linghub.org/sparql", q);
		//qexec.addParam("format", "json");
		//qexec.setModelContentType(WebContent.contentTypeJSONLD);
        try {
            results = ResultSetFactory.copyResults(qexec.execSelect());

        	} catch (Exception ex) {
            Utils.debug(ex.getMessage());
        }
        
        qexec.close();
		return results;
	}
	
}
