package de.unifrankfurt.informatik.acoli.fid.activemq;

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;


@MessageDriven(
        activationConfig = {  
            @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),  
            @ActivationConfigProperty(propertyName = "destination", propertyValue = "WORKER-OUT"), 
            @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue")  
        }  
)  
/**
 * Listens for finished jobs in the WORKER-OUT queue.
 * @author frank
 *
 */
public class ProcessedQueueListener implements MessageListener {
	
    @Resource
    MessageDrivenContext messageDrivenContext;

    @Override
    public void onMessage(Message message) {
        
        try {
            Utils.debug("ProcessedQueueListener Received ResourceInfo : ");
        	Utils.debug(message.getJMSMessageID());
        	
            if (message instanceof TextMessage) {
                TextMessage msg = (TextMessage) message;
                msg.getText();
            }
            if (message instanceof ObjectMessage) {
            	Utils.debug("hello message");
	            ObjectMessage objectMessage = (ObjectMessage) message;
	            ResourceInfo resourceInfo = (ResourceInfo) objectMessage.getObject();
	            Utils.debug("ProcessedQueueListener Received ResourceInfo : "+resourceInfo.getDataURL());
	        }
        } catch (JMSException e) {
            messageDrivenContext.setRollbackOnly();
            e.printStackTrace();
        }
    }

}
