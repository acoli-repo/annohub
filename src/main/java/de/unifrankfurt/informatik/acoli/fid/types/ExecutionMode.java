package de.unifrankfurt.informatik.acoli.fid.types;

public enum ExecutionMode {
	INIT,			// Delete all data and initialize model database
	ADD,			// Add more data
	CLEAN,			// Deletes all data, but keeps loaded models in model database
	RESET,			// Deletes all data, used together with import of a serialized model database
	UNDEFINED,		// Unknown
	DBSTART,		// only startup databases (test only)
	MAKERESULT,		// Create Annohub RDF file
    MAKEURLPOOL,	// Create the list with all files to be processed after applying resource filters
    SERVICE,		// Web application
    PUBLICSERVICE,	// public application
    EXPORTDDB,		// Serialize model database to json file
    EXPORTRDB,		// Serialize registry database to json file
 	RUNDBPATCH,		// Run database patch
 	UPDATEMODELS,	// Update all OLiA models
    CREATEUSER,		// Create a user account
    DELETEUSER,		// Delete a user account
    SETUSERPRIVILEGES // Modify privileges of existing user
}
