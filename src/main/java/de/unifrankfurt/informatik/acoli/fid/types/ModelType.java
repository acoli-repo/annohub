package de.unifrankfurt.informatik.acoli.fid.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.unifrankfurt.informatik.acoli.fid.owl.ModelDefinition;


/**
 * @author frank
 *
 */
public class ModelType implements Serializable, Comparable<ModelType>{

	private static final long serialVersionUID = 3423696928788701081L;
	private String id;

	
	private ModelType(String modelID) {
		this.id = modelID.toUpperCase();
	}

	public String getId() {
		return id;
	}
	
	public String name() {
		return id;
	}
	
	public static ModelType valueOf(String id) {
		
		id = id.toUpperCase();
		if (ModelDefinition.getModelIDPool().contains(id)) {
			return new ModelType(id);
		} else {
			System.out.println("Model ID :"+id+" not found in ID pool !");
			return null;
		}
	}
	
	/**
	 * Get a list of a all available model types. Be careful when using these references in
	 * other objects.
	 * @return
	 */
	public static List<ModelType> values() {
		
		return ModelDefinition.getModels();
		/*List<ModelType> modelTypes = new ArrayList<ModelType>();
		for (String modelID : ModelDefinition.getModelIDs()) {
			modelTypes.add(ModelType.valueOf(modelID));
		}
		return modelTypes.toArray(new ModelType[ModelDefinition.getModelIDs().size()]);*/
	}

	@Override
	public String toString() {
		return id;
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if (!(obj instanceof ModelType)) {
            return false;
        }
		return id.equalsIgnoreCase(((ModelType) obj).id);
	}	
	

	@Override
	public int compareTo(ModelType m) {
		return id.compareToIgnoreCase(m.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
	
	public static void main (String [] args) {
		
		ModelType a = new ModelType("a");
		ModelType A = new ModelType("A");
		
		System.out.println(a.equals(A));
		
		List<ModelType> y = new ArrayList<ModelType>();
		y.add(ModelType.valueOf("hello"));
		y.add(ModelType.valueOf("xyz"));
		y.remove(ModelType.valueOf("xyz"));
		System.out.println(y.size());
		
	}
}
