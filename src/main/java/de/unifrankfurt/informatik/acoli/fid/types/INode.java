package de.unifrankfurt.informatik.acoli.fid.types;

import de.unifrankfurt.informatik.acoli.fid.types.GNode;
import de.unifrankfurt.informatik.acoli.fid.types.GNodeType;
import de.unifrankfurt.informatik.acoli.fid.types.TagType;

public class INode extends GNode {
	
	private String tag;
	private TagType tagType;

	/**
	 * Node type for individuals. Carries also tag & tagType information.
	 * Example :
	 * URL =http://purl.org/olia/eagles.owl#AJ
	 * Tag = "AJ"
	 * Tag type = STARTS (Startswith)
	 * @param name Node name is URL
	 * @param tagType Tag property
	 * @param tag Tag value
	 */
	public INode (String name, String tag, TagType tagType) {
		super(name,GNodeType.TAG);
		this.tag = tag;
		this.tagType = tagType;
	}
	
	public String getTag() {
		return tag;
	}
	
	public TagType getTagType() {
		return tagType;
	}
	
}
