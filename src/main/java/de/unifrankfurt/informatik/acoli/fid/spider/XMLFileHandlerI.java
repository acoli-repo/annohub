package de.unifrankfurt.informatik.acoli.fid.spider;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.Worker;

public interface XMLFileHandlerI {

	void parse(ResourceInfo resourceInfo, Worker fidWorker) throws XMLStreamException, IOException;
	
}
