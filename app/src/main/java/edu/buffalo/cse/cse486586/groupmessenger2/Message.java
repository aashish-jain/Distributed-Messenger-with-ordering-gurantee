package edu.buffalo.cse.cse486586.groupmessenger2;

public class Message{
    int messageId;
    String message;

    Message(int messageId, String message){
        this.messageId = messageId;
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }
}