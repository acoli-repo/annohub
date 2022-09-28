package de.unifrankfurt.informatik.acoli.fid.types;
/**
 * Update strategy : <br>
 * ALL : process new files and already seen files <br>
 * CHANGED : process new files and already seen files that have changed <br>
 * NEW : process only new files
 * @author frank
 *
 */
public enum UpdatePolicy {
	
	UPDATE_ALL,
	UPDATE_CHANGED,
	UPDATE_NEW
	
}
