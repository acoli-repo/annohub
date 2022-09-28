package de.unifrankfurt.informatik.acoli.fid.webclient;

import java.util.concurrent.Future;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;

import de.unifrankfurt.informatik.acoli.fid.activemq.Producer;
import de.unifrankfurt.informatik.acoli.fid.exec.Executer;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;



@Stateless
public class ExecuterEjb {
	
	Producer producer = new Producer(Executer.MQ_IN_1);
	
	@Asynchronous
    public void addResource(ResourceInfo resourceInfo){
        
		// alternative queue
		ExecutionBean.getResourceCache().addResource2Queue(resourceInfo);
		
		// add resource to worker queue
		producer.sendResourceInfo(resourceInfo);
    }
	
	
	@Asynchronous
    public Future<Boolean> async(){
        try {
            Thread.sleep(3000);
            return new AsyncResult<Boolean>(true);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

}
