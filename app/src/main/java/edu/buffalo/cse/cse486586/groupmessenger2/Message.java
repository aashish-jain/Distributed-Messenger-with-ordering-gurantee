package edu.buffalo.cse.cse486586.groupmessenger2;

enum MessageType{
    PROPOSAL_REQUEST, ORDERED_MESSAGE;
}

public class Message{
    int messageId;
    String message;
    MessageType messageType;

    Message(int messageId, String message){
        this.messageId = messageId;
        this.message = message;
        /* When a message is first created, it needs to request proposals*/
        this.messageType = MessageType.PROPOSAL_REQUEST;
    }

    @Override
    public String toString() {

        return message;
    }
}