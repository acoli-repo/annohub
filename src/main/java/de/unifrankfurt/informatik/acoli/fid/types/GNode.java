package de.unifrankfurt.informatik.acoli.fid.types;


public class GNode {
	
	private String name;
	private GNodeType type;
	
	
	public GNode (String name) {
		this.name = name;
		this.type = GNodeType.CLASS;
	}

	public GNode (String name, GNodeType type) {
		this.name = name;
		this.type = type;
	}
	
	public String getName() {
		return name;
	}
	
	public GNodeType getType() {
		return type;
	}

}
