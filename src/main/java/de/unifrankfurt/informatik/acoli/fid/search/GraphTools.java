package de.unifrankfurt.informatik.acoli.fid.search;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONWriter;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONWriter.Builder;

import de.unifrankfurt.informatik.acoli.fid.gremlinQuery.EmbeddedQuery;

public class GraphTools {
	
	
	
	public static void saveAsJSON (Graph graph, String file) {

		Builder x = GraphSONWriter.build();
		GraphSONWriter y = x.create();
		OutputStream stream = createOutputstream(file);
		try {
			y.writeGraph(stream, graph);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void restoreVertexMapEdgeMap(
			EmbeddedQuery queries,
			HashMap <String, Vertex> vertexM,
			HashMap <String, Edge> edgeM
			) {
		
		// Restore vertexM
		for (Vertex v : queries.getClassNodes()) {
			vertexM.put(v.value(GWriter.Class)+"#"+v.label(), v); // GWriter.ClassClass
		}
		for (Vertex v : queries.getTagNodes()) {
			vertexM.put(v.value(GWriter.Class)+"#"+v.label(), v); // GWriter.TagClass
		}
		
		
		// Restore edgeM
		for (Edge e : queries.getClassEdges()) {
			String key = 
					e.inVertex().value(GWriter.Class)
					+ "#"
					+ e.inVertex().label()
					+ "***"
					+ e.value("relation")
					+ "***"
					+ e.outVertex().value(GWriter.Class)
					+ "#"
					+ e.outVertex().label();
			
			//from+"#"+tFrom.toString()+"***"+rt+"***"+to+"#"+tTo.toString()
			
			edgeM.put(key, e);
		}

		System.out.println("VertexM : "+vertexM.size()+" EdgeM : "+edgeM.size());
	}
	
	
	/**
	 * Get set of HITs already in the model graph. This is used then for the model update.
	 * @param queries
	 * @param ĥitEdgeKeys
	 */
	public static HashSet<String> getOldHitEdges(EmbeddedQuery queries) {
		
		HashSet<String> ĥitEdgeKeys = new HashSet<String>();
		
		// Restore HIT -> TAG/Class  edges
		//System.out.println("Edge-map :"+queries.getHitEdges().size());
		for (Edge e : queries.getHitEdges()) {
			ĥitEdgeKeys.add(getHitEdgeKey(e.outVertex(), e.inVertex()));
			//System.out.println("hello"+getHitEdgeKey(e.outVertex(), e.inVertex()));
		}
		
		return ĥitEdgeKeys;
	}
	
	
	
	public static String getHitEdgeKey(Vertex hit, Vertex target) {
	
		String key = "";
		
		switch ((String) hit.value(GWriter.HitType)) {
				
		case GWriter.HitTypeLiteralObject :
		case GWriter.HitTypeURIObject :
		
			key= (String) hit.value(GWriter.HitResourceUrl)+"#"
			    +(String) hit.value(GWriter.HitFileId)+"#"
			    +(String) hit.value(GWriter.HitType)+"#"
			    +(String) hit.value(GWriter.HitPredicate)+"#"
			    +(String) hit.value(GWriter.HitObject)+"#"
			    +(Long) hit.value(GWriter.HitCount);
			
			break;
			
		
		case GWriter.HitTypeTag :
			
			key= (String) hit.value(GWriter.HitResourceUrl)+"#"
		    	+(String) hit.value(GWriter.HitFileId)+"#"
		    	+(String) hit.value(GWriter.HitType)+"#"
		    	+(String) hit.value(GWriter.HitTag)+"#"
		    	+(Integer) hit.value(GWriter.HitConllColumn)+"#"
		    	+(Long) hit.value(GWriter.HitCount);
			
			break;
			
		case GWriter.HitTypeFeature :
			
			key= (String) hit.value(GWriter.HitResourceUrl)+"#"
		    	+(String) hit.value(GWriter.HitFileId)+"#"
		    	+(String) hit.value(GWriter.HitType)+"#"
		    	+(String) hit.value(GWriter.HitFeature)+"#"
		    	+(String) hit.value(GWriter.HitFeatureValue)+"#"
		    	+(Integer) hit.value(GWriter.HitConllColumn)+"#"
		    	+(Long) hit.value(GWriter.HitCount);
			
			break;
	
		default :
			System.out.println("Error getHitEdgeKey : unexpected hit type "+hit.value(GWriter.HitType));
			break;
		}
		
		// Add target TAG/CLASS info
		key+="***"+target.label()+"#"+ (String) target.value(GWriter.Class);
		
		
		return key;
	}
	
	
	// TODO use it to build keys for GWriter.edgeM
	public static String getClassEdgeKey(Vertex hit, Vertex target) {
		return null;
	}
	

	
	public static FileOutputStream createOutputstream (String filepath) {
		try {
			return new FileOutputStream(filepath);
			} catch (FileNotFoundException e) {
			e.printStackTrace();
				return null;
			}
	}

}
