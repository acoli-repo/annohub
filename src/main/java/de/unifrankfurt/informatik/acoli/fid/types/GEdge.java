package de.unifrankfurt.informatik.acoli.fid.types;


public class GEdge {
	
	private GNode from;
	private GNode to;
	private RelationType relationType;

	public GEdge (GNode from, GNode to, RelationType rt) {
		this.from = from;
		this.to = to;
		this.relationType = rt;
	}

	public GNode getFrom() {
		return from;
	}

	public GNode getTo() {
		return to;
	}

	public RelationType getRelationType() {
		return relationType;
	}
}
