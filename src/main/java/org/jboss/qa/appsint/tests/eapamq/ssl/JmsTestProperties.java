package org.jboss.qa.appsint.tests.eapamq.ssl;

public enum JmsTestProperties {

	REQUEST_SEND("send-message"),
	REQUEST_SEND_REQUEST_MESSAGE_FOR_MDB("send-request-message-for-mdb"),
	REQUEST_SEND_REQUEST_MESSAGE_FOR_MDB_AND_KILL_SERVER("send-request-message-for-mdb-and-kill-server"),
	REQUEST_CONSUME_MESSAGE("consume-message"),
	REQUEST_CONSUME_REPLY_MESSAGE_FOR_MDB("consume-reply-message-for-mdb"),
	REQUEST_CONSUME_ALL_REPLY_MESSAGES_FOR_MDB("consume-all-reply-messages-for-mdb"),

	QUEUE_SEND_RESPONSE("Sent a text message to "),
	QUEUE_TEXT_MESSAGE("Hello Servlet!"),
	QUEUE_MDB_SEND_RESPONSE("Sent a text message with to "),
	QUEUE_MDB_TEXT_MESSAGE("Hello MDB!"),
	QUEUE_MDB_TEXT_REPLY_MESSAGE("Hello MDB - reply message!");

	private final String value;

	public String value() {
		return value;
	}

	JmsTestProperties(String value) {
		this.value = value;
	}

}
