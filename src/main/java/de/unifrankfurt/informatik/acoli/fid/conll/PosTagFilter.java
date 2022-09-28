package de.unifrankfurt.informatik.acoli.fid.conll;

public class PosTagFilter {
	
	/**
	 * Returns true if the TAG does not contain any alphabetic character
	 * @param tag
	 * @return
	 */
	public static boolean tagHasAlpha(String tag) {
		
		return tag.matches("[a-zA-Z]+");
	}
	
	
	/**
	 * Returns true if the TAG is a positive integer number
	 * @param tag
	 * @return
	 */
	public static boolean tagIsNumber(String tag) {
		
		return tag.matches("\\d+");
	}
}
