package de.unifrankfurt.informatik.acoli.fid.gremlinQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutionException;

import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.apache.tinkerpop.gremlin.driver.ResultSet;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.unifrankfurt.informatik.acoli.fid.exec.Executer;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager;
import de.unifrankfurt.informatik.acoli.fid.search.GWriter;
import de.unifrankfurt.informatik.acoli.fid.types.ModelType;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;



public class ServerQuery {
	
	private Cluster cluster;
	private Client client;
	private String graphN = "g";
	
	
	public ServerQuery (Cluster cluster) {
		this.cluster = cluster;
		this.client = cluster.connect();
		this.client.init();
	}
	

	public Client getClient() {
		return this.client;
	}
	
	
	
	/**
	 * Required !
	 */
	
	public void commit() {
		
		// works !
		//client.close();
		//client = cluster.connect();
		
		//client.submit("g.commit()"); // works not
	}
	
	private void checkConnectionAndReconnect() {
				
		if(cluster.isClosed()) {
			System.out.println("cluster closed !");
			System.out.println(".. reconnecting ");
			cluster = Cluster.open(Executer.makeBasicGremlinClusterConfig());
			this.client = cluster.connect();
			this.client.init();
			/*try { // solves timeout exception
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
		}
	}
	
	
	public Vertex addVertex(String addVertexQuery) {
		Utils.debug("addVertex :"+addVertexQuery);
		checkConnectionAndReconnect();
		Vertex result = client.submit(addVertexQuery).one().getVertex();
		commit();
		return result;
	}
	
	
	public Vertex addVertex(String addVertexQuery, HashMap <String, Object> values) {
		//Utils.debug(addVertexQuery);
		checkConnectionAndReconnect();
		Vertex result = client.submit(addVertexQuery, values).one().getVertex();
		commit();
		return result;
	}
	
	
	public void genericQuery(String genericQuery, HashMap <String, Object> values) {
		//Utils.debug(genericQuery);
		checkConnectionAndReconnect();
		try {
			client.submit(genericQuery, values).all().get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		commit();
	}
	
	
	public Vertex getVertex(String getVertexQuery) {
		checkConnectionAndReconnect();
		return client.submit(getVertexQuery).one().getVertex();	
	}
	
	
	public Edge addEdge(String addEdgeQuery) {
		Utils.debug(addEdgeQuery);
		checkConnectionAndReconnect();
		Edge result = null;
		try {
			result =  client.submit(addEdgeQuery).one().getEdge();
			commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public void setProperty(String setPropertyQuery,HashMap <String, Object> values) {
		checkConnectionAndReconnect();
		try {
			client.submit(setPropertyQuery, values).all().get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		commit();
	}
	
	
	public Long getModelVertexCount (ModelType model) {
		checkConnectionAndReconnect();
		ResultSet x = client.submit(graphN+".V().has('"+GWriter.Model+"','"+model+"').count()");
		return x.one().getLong();
	}
	
	
	
	
	
	/**
	 * Generic query which has a list of vertices as result
	 * @param query
	 * @return List of vertices
	 */
	public ArrayList <Vertex> genericVertexQuery(String query) {
		
		ArrayList <Vertex> results = new ArrayList <Vertex>();
		//Utils.debug(query);
		checkConnectionAndReconnect();

		try {
			ResultSet x = client.submit(query);
			for (Result r : x) {
				results.add(r.getVertex());
				}
			} catch (Exception e) {
			e.printStackTrace();
		}
		return results;
	}
	
	
	/**
	 * Generic query which has a list of edges as result
	 * @param query
	 * @return List of edges
	 */
	public ArrayList <Edge> genericEdgeQuery(String query) {
		
		ArrayList <Edge> results = new ArrayList <Edge>();
		checkConnectionAndReconnect();
		//Utils.debug(query);
		try {
			ResultSet x = client.submit(query);
			for (Result r : x) {
				results.add(r.getEdge());
				}
			} catch (Exception e) {
			e.printStackTrace();
		}
		return results;
	}
	

	
	/**
	 * Generic query which has a list of strings as result
	 * @param graph
	 * @param query
	 * @return List of strings
	 */
	public ArrayList <String> genericStringQuery(String query) {
		
		ArrayList <String> results = new ArrayList <String>();
		checkConnectionAndReconnect();
		
		try {
			ResultSet x = client.submit(query);
			for (Result r : x) {
				results.add(r.getString());
				}
			} catch (Exception e) {
			e.printStackTrace();
			results.clear();
		}
		return results;
	}
	
	
	
	/**
	 * Generic query which has a list of integers as result
	 * @param graph
	 * @param query
	 * @return List of strings
	 */
	public ArrayList <Integer> genericIntegerQuery(String query) {
		
		ArrayList <Integer> results = new ArrayList <Integer>();
		checkConnectionAndReconnect();
		
		try {
			ResultSet x = client.submit(query);
			for (Result r : x) {
				results.add(r.getInt());
				}
			} catch (Exception e) {
			e.printStackTrace();
			results.clear();
		}
		return results;
	}
	
	
	
	/**
	 * Generic query for deleting vertices, edges, etc.
	 * @param graph
	 * @param query
	 * @return List of vertices
	 */
	public boolean genericDeleteQuery(String query) {
		
		checkConnectionAndReconnect();
		try {
			client.submit(query+".drop().iterate()").all().get();
			commit();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			commit();
			return false;
		}
	}
	
	
	
	/**
	 * Generic query which has a boolean value as result
	 * @param query
	 * @return True or false
	 */
	public boolean genericBooleanQuery(String query) {
		
		checkConnectionAndReconnect();
		try {
			ResultSet x = client.submit(query);
			return x.one().getBoolean();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	
	
	public String makeFileQuery(Vertex resource, Vertex file) {
		String query = graphN+".V()"
				+ ".hasLabel('"+ResourceManager.ResourceVertex+"')"
				+ ".has('"+ResourceManager.ResourceUrl+"','"+resource.value(ResourceManager.ResourceUrl)+"')"
				+ ".outE('"+ResourceManager.FileEdge+"')"
				+ ".inV()"
				+ ".has('"+ResourceManager.FilePathRel+"','"+file.value(ResourceManager.FilePathRel)+"')";
		
		return query;
	}
	
	
	public HashMap<String,HashSet<String>> evaluatePredicates() {
		
		HashSet<String> successfulPredicates = new HashSet<String>();
		HashSet<String> unSuccessfulPredicates = new HashSet<String>();
		HashMap<String, HashSet<String>> result = new HashMap<String,HashSet<String>>();
		
		
		String queryPredicateStats = 
				graphN+".V().hasLabel('"+ResourceManager.PredicateVertex+"')"
			  //+ ".has('"+ResourceManager.PredicateDisabled+"', false)" // skip disabled predicates
			  + ".dedup()"
		  	  + ".project('predicate','default','disabled','good','bad')"
		  	  + ".by('"+ResourceManager.PredicateUrl+"')"
		  	  + ".by('"+ResourceManager.PredicateDefault+"')"
		  	  + ".by('"+ResourceManager.PredicateDisabled+"')"
              + ".by(inE().has('"+ResourceManager.PredicateSuccessful+"',true).count())"
              + ".by(inE().has('"+ResourceManager.PredicateSuccessful+"',false).count())";

		checkConnectionAndReconnect();
		try {
			ResultSet x = client.submit(queryPredicateStats);
		
			String predicate;
			boolean default_;
			boolean disabled_;
			long good;
			long bad;
			
			
			for (Result r : x) {
				LinkedHashMap<String,Object> z =  (LinkedHashMap<String, Object>) r.getObject();
				predicate = (String) z.get("predicate");
				default_ = (Boolean) z.get("default");
				disabled_ = (Boolean) z.get("disabled");
				good = (Long) (z.get("good"));
				bad = (Long) z.get("bad");
				
				
				
				if (default_) {successfulPredicates.add(predicate);continue;}
				
				if (disabled_) {unSuccessfulPredicates.add(predicate);continue;}
				
				if (good==0 && bad == 0) continue; // no information
				
				// no successful result -> bad (allow 4 errors)
				if (good==0  && bad > 4)  {unSuccessfulPredicates.add(predicate);continue;}
				
				// successful result -> good (allow 3 errors)
				if (good > 0 && bad < 4) {successfulPredicates.add(predicate);continue;}
				
				// Skip everything else
				// TODO make metric for decision good/bad
			}
			} catch (Exception e) {
			e.printStackTrace();
		}
		
		result.put("good", successfulPredicates);
		result.put("bad", unSuccessfulPredicates);
		Utils.debug("good :");
		for (String x : successfulPredicates) {
			Utils.debug(x);
		}
		Utils.debug("bad :");
		for (String x : unSuccessfulPredicates) {
			Utils.debug(x);
		}
		
		return result;
	}
	
	/**
	 * Generic query which has a HashMap as result. Cast to target type in caller. Returns null
	 * in case of an error.
	 * @param query
	 * @return HashMap
	 */
	public Object genericMapQuery(String query) {
		
		checkConnectionAndReconnect();
		//Utils.debug(query);
		try {
			ResultSet x = client.submit(query);
			return x.one().getObject();
			} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	/**
	 * Generic query which has a map <Integer, Object> as result
	 * @param query
	 * @return HashMap <String, Long>
	 */
	public HashMap<Integer,Object> genericMapQuery3(String query) {
					
		HashMap <Integer,Object> results = new HashMap<Integer,Object>();
		checkConnectionAndReconnect();
		//Utils.debug(query);
		int i = 1;
		try {
			ResultSet x = client.submit(query);
			for (Result r : x) {
				//Utils.debug(r.getObject().getClass());
				results.put(i++, r.getObject());
				}
			} catch (Exception e) {
			e.printStackTrace();
		}
		return results;
	}
	
	/**
	 * Generic query which has a list of HashMaps as result. Cast to target type in caller.
	 * @param query
	 * @return HashMap
	 */
	public Object genericListMapQuery(String query) {
		
		ArrayList<Object> result = new ArrayList<Object>();
		checkConnectionAndReconnect();

		try {
			ResultSet x = client.submit(query);
				for (Result z : x) {
					result.add(z.getObject());
			}
			return result;
			} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}


	/**
	 * @param query
	 * @return
	 */
	public Long genericCountQuery(String query) {
		
		Long result = null;
		
		try {
			checkConnectionAndReconnect();
			ResultSet x = client.submit(query+".count()");
			for (Result z : x) {
					result = z.getLong();
					break;
			}
			return result;
			} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

}
