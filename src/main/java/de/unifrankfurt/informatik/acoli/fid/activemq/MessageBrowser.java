package de.unifrankfurt.informatik.acoli.fid.activemq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueSession;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQObjectMessage;

import de.unifrankfurt.informatik.acoli.fid.exec.Executer;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;

/**
 * @author frank
 *
 */
public class MessageBrowser {
	
	
	private QueueBrowser browser;
	private QueueSession queueSession;
	private Consumer consumer;

	public MessageBrowser() {
	
	String brokerUrl = Executer.getFidConfig().getString("ActiveMQ.brokerUrl");
	ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
	ArrayList<String> trusted = new ArrayList<String>(Arrays.asList("de.unifrankfurt.informatik.acoli.fid.types,java.util,java.lang,java.net,org.primefaces,java.io,org.apache.tinkerpop.gremlin".split(",")));
	connectionFactory.setTrustedPackages(trusted);
	ActiveMQConnection connection;
	try {
		Utils.debug("Starting message browser");
		connection = (ActiveMQConnection)connectionFactory.createConnection();
		connection.start();
		//DestinationSource ds = connection.getDestinationSource();
		queueSession = connection.createQueueSession(true, Session.CLIENT_ACKNOWLEDGE);
		Queue queue = queueSession.createQueue(Executer.MQ_IN_1);
		browser = queueSession.createBrowser(queue);
		consumer = new Consumer(Executer.MQ_IN_1);

		
	} catch (Exception e) {
		e.printStackTrace();
	}
	}
	
	
	public ArrayList<ResourceInfo> getQueuedResources(String userID) {
		
		ArrayList<ResourceInfo> resources = new ArrayList<ResourceInfo>();
		
		try {

			Enumeration<?> messagesInQueue = browser.getEnumeration();	
		    Utils.debug("checking resource queue");
		    
		    int index = 1;
			while (messagesInQueue.hasMoreElements()) {
				
			    ActiveMQObjectMessage queueMessage = (ActiveMQObjectMessage) messagesInQueue.nextElement();
			    System.out.println(queueMessage);
			    //System.out.println(queueMessage.AMQ_SCHEDULED_ID);
			    
			    
			    ResourceInfo x = (ResourceInfo) queueMessage.getObject();
			    if (x != null && x.getUserID().equals(userID)) {
			    	x.getFileInfo().setProcessingStartDate(new Date(queueMessage.getJMSTimestamp()));
			    	x.setQueuePosition(index++);
			    	resources.add(x);
			    }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resources;
	}
	
	// may take long, depending on the queue size
	public ArrayList<ResourceInfo> getQueuedResources() {
		
		Utils.debug("getQueuedResources (activemq)");
		
		ArrayList<ResourceInfo> resources = new ArrayList<ResourceInfo>();
		
		try {

			Enumeration<?> messagesInQueue = browser.getEnumeration();	
		    Utils.debug("checking resource queue");
		    
			while (messagesInQueue.hasMoreElements()) {
				
			    ActiveMQObjectMessage queueMessage = (ActiveMQObjectMessage) messagesInQueue.nextElement();
			    System.out.println(queueMessage);
			    
			    ResourceInfo x = (ResourceInfo) queueMessage.getObject();
			    if (x != null) {
			    	x.getFileInfo().setProcessingStartDate(new Date(queueMessage.getJMSTimestamp()));
			    	resources.add(x);
			    }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resources;
	}
	
	
	/**
	 * Remove resource from queue
	 * @param userID
	 * @param resourceID
	 * @return
	 */
	public boolean deQueueResource(String userID, String resourceID) {
				
		MessageConsumer messageConsumer=null;
		
		try {

			Enumeration<?> messagesInQueue = browser.getEnumeration();	
		    Utils.debug("deque resource");

			while (messagesInQueue.hasMoreElements()) {
				
			    ActiveMQObjectMessage queueMessage = (ActiveMQObjectMessage) messagesInQueue.nextElement();
			    System.out.println(queueMessage);
			    
			    ResourceInfo x = (ResourceInfo) queueMessage.getObject();
	
			    if (x != null && x.getUserID().equals(userID) && x.getDataURL().equals(resourceID)) {
			    	
			    	messageConsumer = consumer.getConsumer("id='"+x.getMessageID()+"'");
			    	Message z = messageConsumer.receive(1000);
			    	messageConsumer.close();
			    	if (z != null) return true; else return false;
			    }
			}
		} catch (Exception e) {
			e.printStackTrace();
			try {
				messageConsumer.close();
			} catch (JMSException e1) {
				e1.printStackTrace();
			}
		}
		return false;
	}
	
}
