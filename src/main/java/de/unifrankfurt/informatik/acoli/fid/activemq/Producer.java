package de.unifrankfurt.informatik.acoli.fid.activemq;



import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import de.unifrankfurt.informatik.acoli.fid.exec.Executer;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;


public class Producer {
// URL of the JMS server. DEFAULT_BROKER_URL will just mean
// that JMS server is on localhost
//private String url = ActiveMQConnection.DEFAULT_BROKER_URL;
// default broker URL is : tcp://localhost:61616"

private String subject; //Queue Name
// You can create any/many queue names as per your requirement.

private Connection connection;


public Producer() {
	this(Executer.MQ_Default);
}

public Producer (String queueName) {
	
	this.subject = queueName;
	String brokerUrl = Executer.getFidConfig().getString("ActiveMQ.brokerUrl");
	
	// Getting JMS connection from the server and starting it
	ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
	connectionFactory.setUseCompression(true);
	//Properties props = new Properties();
	
	
	//Setting trusted package can be omitted ! (but not on the consumer side)
	//connectionFactory.setTrustedPackages(new ArrayList(Arrays.asList("types".split(","))));
	//connectionFactory.setTrustAllPackages(true);
	try {
		connection = connectionFactory.createConnection();
		connection.start();
		
	} catch (JMSException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}

public void sendMessage(String messageText) {
	
	try {
	// JMS messages are sent and received using a Session. We will
	// create here a non-transactional session object. If you want
	// to use transactions you should set the first parameter to 'true'
	Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

	
	// Destination represents here our queue 'SERVICE-1' on the
	// JMS server. You don't have to do anything special on the
	// server to create it, it will be created automatically.
	Destination destination = session.createQueue(subject);
	
	// MessageProducer is used for sending messages (as opposed
	// to MessageConsumer which is used for receiving them)
	MessageProducer producer = session.createProducer(destination);
	
	// We will send a small text message saying 'Hello' in Japanese
	TextMessage message = session.createTextMessage(messageText);
	
	// Here we are sending the message!
	producer.send(message);
	Utils.debug("Sent message '" + message.getText() + "'");

	connection.close();
	} catch (Exception e) {
		e.printStackTrace();
	}
	}

public void sendResourceInfo (ResourceInfo resourceInfo, String queueName) {
	this.subject = queueName;
	sendResourceInfo(resourceInfo);
}

public void sendResourceInfo (ResourceInfo resourceInfo) {
	
	try {
	Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	Destination destination = session.createQueue(subject);
	MessageProducer producer = session.createProducer(destination);
	ObjectMessage objectMessage = session.createObjectMessage();
	objectMessage.setObject(resourceInfo);
	objectMessage.setStringProperty("id", resourceInfo.getMessageID());
	producer.send(objectMessage);
	Utils.debug("Sent resourceInfo object " + resourceInfo.getDataURL()+" to "+subject);
	} 
	catch (Exception e) {
		e.printStackTrace();
	}
}

}


