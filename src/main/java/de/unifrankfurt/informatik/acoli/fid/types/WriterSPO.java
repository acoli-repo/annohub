package de.unifrankfurt.informatik.acoli.fid.types;

import java.util.HashMap;

public interface WriterSPO {
	
	void writeRdf (HashMap <String, Long> subjects, 
    		HashMap <String, Long> predicates,
    		HashMap <String, Long> objects,
    		ResourceInfo resourceInfo
    		);

}
