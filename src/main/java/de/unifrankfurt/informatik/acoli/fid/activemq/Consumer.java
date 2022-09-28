package de.unifrankfurt.informatik.acoli.fid.activemq;



import java.util.ArrayList;
import java.util.Arrays;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;

import de.unifrankfurt.informatik.acoli.fid.exec.Executer;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;


public class Consumer implements MessageListener {
	
	
	//URL of the JMS server
	//private  String url = ActiveMQConnection.DEFAULT_BROKER_URL;
	//default broker URL is : tcp://localhost:61616"

	//Name of the queue we will receive messages from
	private String subject;

	private Connection connection;

	private ActiveMQConnectionFactory connectionFactory;

	private Session session;
	
	
	public Consumer() {
		this(Executer.MQ_Default);
	}
	
	
	public Consumer (String queueName) {
		
		// Set queue name
		this.subject = queueName;
		String brokerUrl = Executer.getFidConfig().getString("ActiveMQ.brokerUrl");
		
		try {
			// Getting JMS connection from the server
			//connectionFactory = new ActiveMQConnectionFactory(url);
			connectionFactory = new ActiveMQConnectionFactory("failover://("+brokerUrl+")?initialReconnectDelay=2000&maxReconnectAttempts=2");
			ArrayList<String> trusted = new ArrayList<String>(Arrays.asList("de.unifrankfurt.informatik.acoli.fid.types,java.util,java.lang,java.net,org.primefaces,java.io,org.apache.tinkerpop.gremlin".split(",")));
			connectionFactory.setTrustedPackages(trusted);
			//connectionFactory.setTrustAllPackages(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
	
	
	public void close() {
		try {
			connection.close();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public String receiveTextMessage () {
			
		String receivedText = "";
		try {
			
			Message message = receive();
			// There are many types of Message and TextMessage
			// is just one of them. Producer sent us a TextMessage
			// so we must cast to it to get access to its .getText()
			// method.
			if (message instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) message;
				receivedText = textMessage.getText();
				Utils.debug("Received message '" + receivedText
				+ "'");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return receivedText;
	}

	
	public ResourceInfo receiveResourceInfo (String queueName) {
		this.subject = queueName;
		return receiveResourceInfo();
	}
	
	public ResourceInfo receiveResourceInfo () {
		
		ResourceInfo resourceInfo = null;
		try {
			
			Message message = receive();
			// There are many types of Message and TextMessage
			// is just one of them. Producer sent us a TextMessage
			// so we must cast to it to get access to its .getText()
			// method.
			if (message instanceof ObjectMessage) {
		            ObjectMessage objectMessage = (ObjectMessage) message;
		            resourceInfo = (ResourceInfo) objectMessage.getObject();
		            Utils.debug("Received ResourceInfo : "+resourceInfo.getDataURL()+ " from "+subject);
		        }
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return resourceInfo;
	}
	
	
	public static ResourceInfo extractResourceInfo (Message message) {
		
		ResourceInfo resourceInfo = null;
		try {
			
			// There are many types of Message and TextMessage
			// is just one of them. Producer sent us a TextMessage
			// so we must cast to it to get access to its .getText()
			// method.
			if (message instanceof ObjectMessage) {
		            ObjectMessage objectMessage = (ObjectMessage) message;
		            resourceInfo = (ResourceInfo) objectMessage.getObject();
		            Utils.debug("extract ResourceInfo : "+resourceInfo.getDataURL());
		        }
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return resourceInfo;
	}
	


	/**
	 * Methods blocks until message is received. If there are no messages in the message
	 * queue then it waits forever !
	 * @return
	 */
	private Message receive () {
		
		Message message = null;
		try {
			
			connection = connectionFactory.createConnection();
			connection.start();

			//connection.createDurableConnectionConsumer(arg0, arg1, arg2, arg3, arg4);
			
			// Creating session for seding messages
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	
			// Getting the queue 'VALLYSOFTQ'
			//Utils.debug("receive from "+subject);
			Destination destination = session.createQueue(this.subject);
	
			// MessageConsumer is used for receiving (consuming) messages
			MessageConsumer consumer = session.createConsumer(destination);
	
			// Here we receive the message.
			// By default this call is blocking, which means it will wait
			// for a message to arrive on the queue.
			message = consumer.receive();
			session.close();
			connection.close();
			
			} catch (JMSException e) {
			} catch (Exception e) {
				e.printStackTrace();

			}

			return message;
	}
	

public MessageConsumer getConsumer (){
	try {
		connection = connectionFactory.createConnection();
		connection.start();
		
		//connection.createDurableConnectionConsumer(arg0, arg1, arg2, arg3, arg4);
		
		// Creating session for sending messages
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

		//Utils.debug("receive from "+subject);
		Destination destination = session.createQueue(this.subject);

		// MessageConsumer is used for receiving (consuming) messages
		MessageConsumer consumer = session.createConsumer(destination);
		return consumer;
	} catch (JMSException e) {
		e.printStackTrace();
		return null;
	}
}

/**
 * 
 * @param messageFilter (e.g. "id='xyz'")
 * @return
 */
public MessageConsumer getConsumer (String messageFilter){
	try {
		connection = connectionFactory.createConnection();
		connection.start();
		
		// Creating session for sending messages
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

		//Utils.debug("receive from "+subject);
		Destination destination = session.createQueue(this.subject);

		// MessageConsumer is used for receiving (consuming) messages
		MessageConsumer consumer = session.createConsumer(destination, messageFilter);
		return consumer;
		
	} catch (JMSException e) {
		e.printStackTrace();
		return null;
	}
}


public static void main(String[] args) throws JMSException {
	
	Consumer consumer = new Consumer(Executer.MQ_IN_1);
	consumer.receiveTextMessage();
}


@Override
public void onMessage(Message message) {
	
}


public Session getSession() {
	return session;
}


public Connection getConnection() {
	return this.connection;
}

}
