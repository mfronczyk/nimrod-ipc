/*
 * Copyright 2014 Andrew Crutchlow
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nimrodtechs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimrodtechs.ipc.MessageReceiverInterface;
import com.nimrodtechs.ipc.ZeroMQPubSubSubscriber;
import com.nimrodtechs.ipc.queue.QueueExecutor;
import com.nimrodtechs.serialization.NimrodObjectSerializer;
import com.nimrodtechs.serialization.kryo.KryoSerializer;

public class TestSubscriber implements MessageReceiverInterface {
    final static Logger logger = LoggerFactory.getLogger(TestSubscriber.class);
    static ZeroMQPubSubSubscriber subscriber;
    
	public static void main(String[] args) {
	  //Register a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                if(subscriber != null)
                    subscriber.dispose();
            }
        });

        //Configure the general serializer by adding a kryo serializer
        //NimrodObjectSerializer.GetInstance().getSerializers().put("kryo",new KryoSerializer());
        subscriber = new ZeroMQPubSubSubscriber();
        subscriber.setInstanceName("TestSubscriber");
        subscriber.setServerSocket(System.getProperty("rmiServerSocketUrl","ipc://"+System.getProperty("java.io.tmpdir")+"/TestPublisherSocket.pubsub"));
        try {
            subscriber.initialize();
            //subscriber.subscribe("testsubject", new TestSubscriber(), String.class,QueueExecutor.CONFLATING_QUEUE);
            //subscriber.subscribe("testsubject2", new TestSubscriber(), String.class);
            subscriber.subscribe("testsubject3", new TestSubscriber(), TestDTO.class);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	}

    @Override
    public void messageReceived(String subject, Object message) {
    	if(message instanceof String)
    		logger.info("subject="+subject+" message="+message);
    	else if (message instanceof TestDTO) {
    		TestDTO t = (TestDTO)message;
    		logger.info("subject="+subject+" field1="+t.field1+" field2="+t.field2+" field3="+t.field3);
    	}
        
    }

}
