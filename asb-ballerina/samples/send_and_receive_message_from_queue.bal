// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/config;
import ballerina/log;
import ballerinax/asb;

public function main() {

    // Input values
    string stringContent = "This is My Message Body"; 
    byte[] byteContent = stringContent.toBytes();
    json jsonContent = {name: "apple", color: "red", price: 5.36};
    byte[] byteContentFromJson = jsonContent.toJsonString().toBytes();
    map<string> parameters1 = {contentType: "text/plain", messageId: "one"};
    map<string> parameters2 = {contentType: "application/json", messageId: "two", to: "user1", replyTo: "user2", 
        label: "a1", sessionId: "b1", correlationId: "c1", timeToLive: "2"};
    map<string> properties = {a: "propertyValue1", b: "propertyValue2"};
    int serverWaitTime = 5;

    asb:ConnectionConfiguration config = {
        connectionString: config:getAsString("CONNECTION_STRING"),
        entityPath: config:getAsString("QUEUE_PATH")
    };

    log:print("Creating Asb sender connection.");
    asb:SenderConnection? senderConnection = checkpanic new (config);

    log:print("Creating Asb receiver connection.");
    asb:ReceiverConnection? receiverConnection = checkpanic new (config);

    if (senderConnection is asb:SenderConnection) {
        log:print("Sending via Asb sender connection.");
        checkpanic senderConnection->sendMessageWithConfigurableParameters(byteContent, parameters1, properties);
        checkpanic senderConnection->sendMessageWithConfigurableParameters(byteContentFromJson, parameters2, properties);
    } else {
        log:printError("Asb sender connection creation failed.");
    }

    if (receiverConnection is asb:ReceiverConnection) {
        log:print("Receiving from Asb receiver connection.");
        asb:Message|asb:Error? messageReceived = receiverConnection->receiveMessage(serverWaitTime);
        asb:Message|asb:Error? jsonMessageReceived = receiverConnection->receiveMessage(serverWaitTime);
        if (messageReceived is asb:Message && jsonMessageReceived is asb:Message) {
            string messageRead = checkpanic messageReceived.getTextContent();
            log:print("Reading Received Message : " + messageRead);
            json jsonMessageRead = checkpanic jsonMessageReceived.getJSONContent();
            log:print("Reading Received Message : " + jsonMessageRead.toString());
        } 
    }

    if (senderConnection is asb:SenderConnection) {
        log:print("Closing Asb sender connection.");
        checkpanic senderConnection.closeSenderConnection();
    }

    if (receiverConnection is asb:ReceiverConnection) {
        log:print("Closing Asb receiver connection.");
        checkpanic receiverConnection.closeReceiverConnection();
    }
}    