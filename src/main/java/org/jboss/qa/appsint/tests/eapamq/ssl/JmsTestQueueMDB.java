package org.jboss.qa.appsint.tests.eapamq.ssl;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.TextMessage;

/**
 * Test message driven bean to consume a Text messages with "consumer = 'MDB'" selector from testQueue.
 */
@MessageDriven(name = "testQueueMDB", activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:/jms/amq/queue/inQueue"),
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
		@ActivationConfigProperty(propertyName = "connectionFactoryLookup", propertyValue = "java:jboss/RemoteJmsXA")
		/*
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "inQueue"),
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
		@ActivationConfigProperty(propertyName = "user", propertyValue = "admin"),
		@ActivationConfigProperty(propertyName = "password", propertyValue = "admin"),
		@ActivationConfigProperty(propertyName = "connectionFactoryLookup", propertyValue = "java:jboss/RemoteJmsXA"),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")
		*/
})
public class JmsTestQueueMDB implements MessageListener {

	public static AtomicInteger numberOfProcessedMessages = new AtomicInteger();

	static final String QUEUE_MDB_TEXT_REPLY_MESSAGE = "Hello MDB - reply message!";

	private static final Logger LOGGER = Logger.getLogger(JmsTestQueueMDB.class.toString());

	@Inject()
	private JMSContext context;

	@Resource(lookup = "java:/jms/amq/queue/outQueue")
	private Queue outQueue;

	/**
	 * @see MessageListener#onMessage(Message)
	 */
	public void onMessage(Message rcvMessage) {
		LOGGER.info("MDB: message received from inQueue");
		TextMessage message;
		try {
			int processedMessages = numberOfProcessedMessages.incrementAndGet();
			if (rcvMessage instanceof TextMessage) {
				message = (TextMessage) rcvMessage;
				LOGGER.info("Received " + processedMessages + " Message from queue: "
						+ message.getText() + " details: " + message);
			} else {
				LOGGER.warning("Message of wrong type: " + processedMessages + " for message: "
						+ processedMessages + " details: " + rcvMessage);
			}

			simulateBusinessLogic();

			Message newMessage = context.createTextMessage(QUEUE_MDB_TEXT_REPLY_MESSAGE);
			newMessage.setStringProperty("inMessageId", rcvMessage.getJMSMessageID());
			JMSProducer producer = context.createProducer();
			producer.send(outQueue, newMessage);

			if (processedMessages % 100 == 0) {
				LOGGER.info("100th message received - killing server");
				// do not use System.exit() as it calls all shutdowns hooks and finalizers, this will kill instantly
				Runtime.getRuntime().halt(1);
			}
		} catch (JMSException e) {
			LOGGER.log(Level.SEVERE, "MDB failed to process message.", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Slow down message processing to simulate business logic.
	 */
	private void simulateBusinessLogic() {
		for (int i = 0; i < (5 + 5 * Math.random()); i++) {
			try {
				Thread.sleep((int) (10 + 10 * Math.random()));
			} catch (InterruptedException ex) {
			}
		}
	}
}
