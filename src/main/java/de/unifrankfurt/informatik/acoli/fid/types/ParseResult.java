package de.unifrankfurt.informatik.acoli.fid.types;

public enum ParseResult {
	SUCCESS,	// models and/or languages could be retrieved
	NONE,		// no metadata could be retrieved
	ERROR,		// the processing did not finish because of an error
	UNKNOWN		// initial state before processing
}
