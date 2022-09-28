package de.unifrankfurt.informatik.acoli.fid.util;

import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import com.bigdata.rdf.sail.BigdataSailRepository;

public class BlazegraphUtils {

	
	public static TupleQueryResult exeSelQuery(Repository repo, String query) {
		
		RepositoryConnection cxn = null;
		
		try {
			
			if (repo instanceof BigdataSailRepository) {
				cxn = ((BigdataSailRepository) repo).getReadOnlyConnection();
			} else {
				cxn = repo.getConnection();
			}

			final TupleQuery tupleQuery = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query);
			//tupleQuery.setIncludeInferred(true /* includeInferred */);
			
			return tupleQuery.evaluate();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		  // close the repository connection
		  finally {
				try {
					cxn.close();
				} catch (RepositoryException e) {
					e.printStackTrace();
				}
		}
		return null;
	}
}

