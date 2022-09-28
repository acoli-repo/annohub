package de.unifrankfurt.informatik.acoli.fid.types;

import org.apache.commons.configuration2.XMLConfiguration;

import de.unifrankfurt.informatik.acoli.fid.activemq.Consumer;
import de.unifrankfurt.informatik.acoli.fid.spider.ConllFileHandler;
import de.unifrankfurt.informatik.acoli.fid.spider.RdfFileHandlerI;
import de.unifrankfurt.informatik.acoli.fid.spider.XMLFileHandlerI;

public interface Worker extends Runnable{
	
	public RdfFileHandlerI getRdfFileHandler();

	public ConllFileHandler getConllFileHandler();

	public XMLFileHandlerI getXmlFileHandler();
	
	public int getWorkerId();
	
	public XMLConfiguration getConfiguration();
	
	public Consumer getResourceConsumer();

	public ResourceInfo getActiveResource();

}
