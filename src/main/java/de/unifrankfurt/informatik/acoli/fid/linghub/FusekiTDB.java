package de.unifrankfurt.informatik.acoli.fid.linghub;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.tdb.TDBFactory;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;


/**
 * @author Frank
 *
 */
public class FusekiTDB {
	
	private Dataset dataset;
	
	public Dataset getDataset() {
		return dataset;
	}
	
	/**
	 * Create persistent Fuseki TDB dataset
	 * @param dir TDB directory 
	 */
	public FusekiTDB(String dir) {
		
		this.dataset = TDBFactory.createDataset(dir);
		Utils.debug("Create/connect to  TDB dataset : " + dir);		

	}
	

	/**
	 * Create memory Fuseki TDB dataset
	 * @param dir TDB directory 
	 */
	public FusekiTDB() {
		super();
		this.dataset = TDBFactory.createDataset();
		Utils.debug( "Construct an in-memory dataset" );
	}
	
	
	public ResultSet query(String queryString) {
		
		 this.dataset.begin(ReadWrite.READ);
		 
		 try(QueryExecution qExec = QueryExecutionFactory.create(queryString, this.dataset)) {
		     ResultSet rs = qExec.execSelect();
		     return ResultSetFactory.copyResults(rs);
		 }
		 finally {
		 dataset.end();
		 }
	}

}
