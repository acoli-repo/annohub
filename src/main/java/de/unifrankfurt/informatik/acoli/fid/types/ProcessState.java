package de.unifrankfurt.informatik.acoli.fid.types;

/**
 * Process state of a resource </p>
 * Export to RDF only for state : ACCEPTED </p>
 * EXPORT to REST for states : ACCEPTED, EDITED, PROCESSED
 * @author frank
 *
 */

public enum ProcessState {
	
	ACCEPTED,		// Final state after revision of an editor
	CHECK,			// Informs a user that a resource is probably not relevant although model information has been found 
	DISABLED,		// The resource is marked as not relevant
	EDITED,			// Results haven been edited
	EXCLUDED,		// The resource did not yield any results (can not be edited)
	PROCESSED,		// State after automatic processing has finished and results were detected
	UNPROCESSED		// Initial state before processing (applies only to manual added resource via GUI)
	
}
