package de.unifrankfurt.informatik.acoli.fid.spider;

import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.Worker;

public interface RdfFileHandlerI {

	void parse(ResourceInfo resourceInfo, Worker fidWorker);
	
}
