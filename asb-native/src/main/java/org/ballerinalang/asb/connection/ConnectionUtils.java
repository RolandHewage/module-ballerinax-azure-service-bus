/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.asb.connection;

import com.microsoft.azure.servicebus.*;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import org.ballerinalang.asb.ASBConstants;
import org.ballerinalang.asb.ASBUtils;
import org.ballerinalang.jvm.api.values.BArray;
import org.ballerinalang.jvm.api.values.BMap;
import org.ballerinalang.jvm.api.values.BObject;
import org.ballerinalang.jvm.api.BValueCreator;

import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.ballerinalang.asb.ASBConstants.*;

/**
 * Util class used to bridge the Asb connector's native code and the Ballerina API.
 */
public class ConnectionUtils {
    private static final Logger log = Logger.getLogger(ConnectionUtils.class.getName());

    private String connectionString;

    /**
     * Creates a Asb Sender Connection using the given connection parameters.
     *
     * @param connectionString Azure Service Bus Primary key string used to initialize the connection.
     * @param entityPath Resource entity path.
     * @return Asb Sender Connection object.
     */
    public static IMessageSender createSenderConnection(String connectionString, String entityPath) throws Exception {
        try {
            IMessageSender sender = ClientFactory.createMessageSenderFromConnectionStringBuilder(
                    new ConnectionStringBuilder(connectionString, entityPath));
            return sender;
        } catch (Exception e) {
            throw ASBUtils.returnErrorValue(e.getMessage());
        }
    }

    /**
     * Closes the Asb Sender Connection using the given connection parameters.
     *
     * @param sender Created IMessageSender instance used to close the connection.
     */
    public static void closeSenderConnection(IMessageSender sender) throws Exception {
        try {
            sender.close();
        } catch (Exception e) {
            throw ASBUtils.returnErrorValue(e.getMessage());
        }
    }

    /**
     * Creates a Asb Receiver Connection using the given connection parameters.
     *
     * @param connectionString Primary key string used to initialize the connection.
     * @param entityPath Resource entity path.
     * @return Asb Receiver Connection object.
     */
    public static IMessageReceiver createReceiverConnection(String connectionString, String entityPath)
            throws Exception {
        try {
            IMessageReceiver receiver = ClientFactory.createMessageReceiverFromConnectionStringBuilder(
                    new ConnectionStringBuilder(connectionString, entityPath), ReceiveMode.PEEKLOCK);
            return receiver;
        } catch (Exception e) {
            throw ASBUtils.returnErrorValue(e.getMessage());
        }
    }

    /**
     * Closes the Asb Receiver Connection using the given connection parameters.
     *
     * @param receiver Created IMessageReceiver instance used to close the connection.
     */
    public static void closeReceiverConnection(IMessageReceiver receiver) throws Exception {
        try {
            receiver.close();
        } catch (Exception e) {
            throw ASBUtils.returnErrorValue(e.getMessage());
        }
    }

    /**
     * Convert BMap to Map.
     *
     * @param map Input BMap used to convert to Map.
     * @return Converted Map object.
     */
    public static Map<String, String> toStringMap(BMap map) {
        Map<String, String> returnMap = new HashMap<>();
        if (map != null) {
            for (Object aKey : map.getKeys()) {
                returnMap.put(aKey.toString(), map.get(aKey).toString());
            }
        }
        return returnMap;
    }

    /**
     * Send Message with configurable parameters when Sender Connection is given as a parameter and
     * message content as a byte array.
     *
     * @param sender Input Sender connection.
     * @param content Input message content as byte array
     * @param contentType Input message content type
     * @param messageId Input Message Id
     * @param to Input Message to
     * @param replyTo Input Message reply to
     * @param label Input Message label
     * @param sessionId Input Message session Id
     * @param correlationId Input Message correlationId
     * @param properties Input Message properties
     * @param timeToLive Input Message time to live in minutes
     */
    public static void sendMessage(IMessageSender sender, BArray content, String contentType, String messageId,
                                   String to, String replyTo, String label, String sessionId, String correlationId,
                                   BMap<String, String> properties, int timeToLive) throws Exception {
        try {
            // Send messages to queue
            log.info("\tSending messages to ...\n" + sender.getEntityPath());
            IMessage message = new Message();
            message.setMessageId(messageId);
            message.setTimeToLive(Duration.ofMinutes(timeToLive));
            byte[] byteArray = content.getBytes();
            message.setBody(byteArray);
            message.setContentType(contentType);
            message.setMessageId(messageId);
            message.setTo(to);
            message.setReplyTo(replyTo);
            message.setLabel(label);
            message.setSessionId(sessionId);
            message.setCorrelationId(correlationId);
            Map<String,String> map = toStringMap(properties);
            message.setProperties(map);

            sender.send(message);
            log.info("\t=> Sent a message with messageId \n" + message.getMessageId());
        } catch (Exception e) {
            throw ASBUtils.returnErrorValue(e.getMessage());
        }
    }

    /**
     * Send Message with configurable parameters when Sender Connection is given as a parameter and
     * message content as a byte array and optional parameters as a BMap.
     *
     * @param sender Input Sender connection.
     * @param content Input message content as byte array
     * @param parameters Input message optional parameters specified as a BMap
     * @param properties Input Message properties
     */
    public static void sendMessageWithConfigurableParameters(IMessageSender sender, BArray content,
                                                             BMap<String, String> parameters,
                                                             BMap<String, String> properties) throws Exception {
        Map<String,String> map = toStringMap(parameters);

        String contentType = valueToStringOrEmpty(map, CONTENT_TYPE);
        String messageId = map.get(MESSAGE_ID) != null ? map.get(MESSAGE_ID) : UUID.randomUUID().toString();
        String to = valueToStringOrEmpty(map, TO);
        String replyTo = valueToStringOrEmpty(map, REPLY_TO);
        String label = valueToStringOrEmpty(map,LABEL);
        String sessionId = valueToStringOrEmpty(map, SESSION_ID);
        String correlationId = valueToStringOrEmpty(map, CORRELATION_ID);
        int timeToLive = map.get(TIME_TO_LIVE) != null ? Integer.parseInt(map.get(TIME_TO_LIVE)) : DEFAULT_TIME_TO_LIVE;

        try {
            // Send messages to queue
            log.info("\tSending messages to ...\n" + sender.getEntityPath());
            IMessage message = new Message();
            message.setMessageId(messageId);
            message.setTimeToLive(Duration.ofMinutes(timeToLive));
            byte[] byteArray = content.getBytes();
            message.setBody(byteArray);
            message.setContentType(contentType);
            message.setMessageId(messageId);
            message.setTo(to);
            message.setReplyTo(replyTo);
            message.setLabel(label);
            message.setSessionId(sessionId);
            message.setCorrelationId(correlationId);
            Map<String,String> propertiesMap = toStringMap(properties);
            message.setProperties(propertiesMap);

            sender.send(message);
            log.info("\t=> Sent a message with messageId \n" + message.getMessageId());
        } catch (Exception e) {
            throw ASBUtils.returnErrorValue(e.getMessage());
        }
    }

    /**
     * Receive Message with configurable parameters as Map when Receiver Connection is given as a parameter and
     * message content as a byte array and return Message object.
     *
     * @param receiver Output Receiver connection.
     * @param serverWaitTime Specified server wait time in seconds to receive message.
     * @return Message Object of the received message.
     */
    public static Object receiveMessage(IMessageReceiver receiver, int serverWaitTime) throws Exception {
        try {
            log.info("\n\tWaiting up to 'serverWaitTime' seconds for messages from \n" + receiver.getEntityPath());

            IMessage receivedMessage = receiver.receive(Duration.ofSeconds(serverWaitTime));

            if (receivedMessage == null) {
                return null;
            }
            log.info("\t<= Received a message with messageId \n" + receivedMessage.getMessageId());
            log.info("\t<= Received a message with messageBody \n" +
                    new String(receivedMessage.getBody(), UTF_8));
            receiver.complete(receivedMessage.getLockToken());

            log.info("\tDone receiving messages from \n" + receiver.getEntityPath());

            BObject messageBObject = BValueCreator.createObjectValue(ASBConstants.PACKAGE_ID_ASB,
                    ASBConstants.MESSAGE_OBJECT);
            messageBObject.set(ASBConstants.MESSAGE_CONTENT, BValueCreator.createArrayValue(receivedMessage.getBody()));
            return messageBObject;
        } catch (Exception e) {
            throw ASBUtils.returnErrorValue(e.getMessage());
        }
    }

    /**
     * Get the map value as string or as empty based on the key.
     *
     * @param map Input map.
     * @param key Input key.
     * @return map value as a string or empty.
     */
    private static String valueToStringOrEmpty(Map<String, ?> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : value.toString();
    }

    public ConnectionUtils() {
    }

    public ConnectionUtils(String connectionString) {
        this.connectionString = connectionString;
    }
}
