package org.jboss.qa.appsint.tests.eapamq.ssl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Test servlet which can be used to invoke common JMS tasks in test classes.
 */
@WebServlet("/jms-test")
public class JmsTestServlet extends HttpServlet {

	private static final Logger LOGGER = Logger.getLogger(HttpServlet.class.toString());

	@Resource(lookup = "java:/jms/amq/queue/testQueue")
	private Queue testQueue;

	@Resource(lookup = "java:/jms/amq/queue/inQueue")
	private Queue inQueue;

	@Resource(lookup = "java:/jms/amq/queue/outQueue")
	private Queue outQueue;

	@Inject()
	private JMSContext jmsContext;

	@Resource(lookup = "java:jboss/RemoteJmsXA")
	private ConnectionFactory connectionFactory;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html");
		TextMessage textMessage;

		String request = req.getParameter("request");

		try (PrintWriter out = resp.getWriter()) {

			if (request == null || "".equals(request)) { // log usage and return
				logUsage(out, null);
				return;
			}

			if (JmsTestProperties.REQUEST_SEND.value().equalsIgnoreCase(request)) {
				textMessage = jmsContext.createTextMessage(JmsTestProperties.QUEUE_TEXT_MESSAGE.value());
				jmsContext.createProducer().send(testQueue, textMessage);
				out.println(JmsTestProperties.QUEUE_SEND_RESPONSE.value() + testQueue.toString());
				// produce and send a text message to testQueue
			} else if (JmsTestProperties.REQUEST_SEND_REQUEST_MESSAGE_FOR_MDB.value().equalsIgnoreCase(request)) {
				textMessage = jmsContext.createTextMessage(JmsTestProperties.QUEUE_MDB_TEXT_MESSAGE.value());
				jmsContext.createProducer().send(inQueue, textMessage);
				out.println(JmsTestProperties.QUEUE_MDB_SEND_RESPONSE.value() + inQueue.toString());
				// produce 180 text messages to testQueue, MDB will kill server when 100th message is consumed
				// this must done in transaction to avoid situation that MDB kill server before this call is finished
				// note that JMSContext cannot be used as Wildfly/EAP does no allow to inject it with transacted session
			} else if (JmsTestProperties.REQUEST_SEND_REQUEST_MESSAGE_FOR_MDB_AND_KILL_SERVER.value()
					.equalsIgnoreCase(request)) {
				String messageCount = req.getParameter("messageCount");
				int messageCountToSend = isEmpty(messageCount) ? 180 : Integer.valueOf(messageCount);
				Connection connection = null;
				try {
					connection = connectionFactory.createConnection();
					Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
					MessageProducer producer = session.createProducer(inQueue);
					for (int i = 0; i < messageCountToSend; i++) {
						textMessage = session.createTextMessage(JmsTestProperties.QUEUE_MDB_TEXT_MESSAGE.value());
						producer.send(textMessage);
					}
					out.println(messageCountToSend + " messages were sent into queue: " + inQueue.toString());
					session.commit();
				} catch (Exception ex) {
					LOGGER.log(Level.SEVERE, ex.getMessage());
					out.println(ex);
				} finally {
					if (connection != null) {
						connection.close();
					}
				}
				// consume a text message from testQueue
			} else if (JmsTestProperties.REQUEST_CONSUME_MESSAGE.value().equalsIgnoreCase(request)) {
				textMessage = (TextMessage) jmsContext.createConsumer(testQueue).receive(1000);
				out.println(textMessage.getText());
				// consume a reply text message from outQueue, processed by MDB
			} else if (JmsTestProperties.REQUEST_CONSUME_REPLY_MESSAGE_FOR_MDB.value().equalsIgnoreCase(request)) {
				textMessage = (TextMessage) jmsContext.createConsumer(outQueue).receive(1000);
				out.println((textMessage == null ? null : textMessage.getText()) + " message details: " + textMessage);
				// consume all reply text messages from outQueue, processed by MDB
			} else if (JmsTestProperties.REQUEST_CONSUME_ALL_REPLY_MESSAGES_FOR_MDB.value().equalsIgnoreCase(request)) {
				JMSConsumer consumer = jmsContext.createConsumer(outQueue);
				int count = 0;
				while ((consumer.receive(1000)) != null) {
					count++;
				}
				out.print(count);
				// print usage
			} else {
				logUsage(out, request);
			}
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage());
		}
	}

	private boolean isEmpty(String value) {
		return value == null || "".equals(value);
	}

	private void logUsage(PrintWriter out, String request) {
		out.println("Invalid request parameter: " + request + "<br>" +
				"Usage:<ul>" +
				"<li>use <b>?request=" + JmsTestProperties.REQUEST_SEND.value()
				+ "</b> parameter to send a message to test queue</li>" +
				"<li>use <b>?request=" + JmsTestProperties.REQUEST_SEND_REQUEST_MESSAGE_FOR_MDB.value()
				+ " </b> parameter to send a message " +
				"to test queue</li>" +
				"<li>use <b>?request=" + JmsTestProperties.REQUEST_SEND_REQUEST_MESSAGE_FOR_MDB_AND_KILL_SERVER.value()
				+ " </b> parameter " +
				"to send 180 messages to test queue, MDB will kill server when 100th message is received," +
				"you can use paramter &messageCount=20 to change number of messages</li>" +
				"<li>use <b>?request=" + JmsTestProperties.REQUEST_CONSUME_MESSAGE.value()
				+ "</b> parameter to consume a message from test queue</li>" +
				"<li>use <b>?request=" + JmsTestProperties.REQUEST_CONSUME_REPLY_MESSAGE_FOR_MDB.value()
				+ "</b> parameter to consume a reply " +
				"message from outQueue queue which was processed by MDB</li>" +
				"<li>use <b>?request=" + JmsTestProperties.REQUEST_CONSUME_ALL_REPLY_MESSAGES_FOR_MDB.value()
				+ "</b> parameter to consume all reply " +
				"messages from outQueue queue which were processed by MDB</li>" +
				"</ul>");
	}
}
