package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.IOException;
import java.util.Comparator;

enum MessageType{
    PROPOSAL_REQUEST, ORDERED_MESSAGE;
}

public class Message implements Comparator<Message> {
    static final int messageIdLen=4;
    String seperator = "<sep>";

    private int messageId;
    private String message;
    private MessageType messageType;
    private float messagePriority ;

    Message(int messageId, String message){
        this.messageId = messageId;
        this.message = message;
        this.messagePriority = -1.0F;
        /* When a message is first created, it needs to request proposals*/
        this.messageType = MessageType.PROPOSAL_REQUEST;
    }

    Message(String string) throws IOException {
        String[] strings = string.split(seperator);
        if(strings.length == 4) {
            this.messageType = messageType.valueOf(strings[0]);
            this.messageId = Integer.parseInt(strings[1]);
            this.message = strings[2];
            this.messagePriority = Float.parseFloat(strings[3]);
        }
        else {
            throw new IOException("unable to parse the String");
        }
    }

    @Override
    public String toString() {
        return message;
    }

    public String encodeMessage(){
            return messageType + seperator + messageId
                    +seperator + message + seperator + messagePriority;
    }

    public int getMessageId() {
        return messageId;
    }

    public String getMessage() {
        return message;
    }

    public synchronized void setMessagePriority(float priority){
        if(messagePriority > priority) {
            messagePriority = priority;
            messageType = messageType.ORDERED_MESSAGE;
        }
    }

    public MessageType getMessageType() {
        return messageType;
    }

    @Override
    public int compare(Message lhs, Message rhs) {
        return 0;
    }
}

