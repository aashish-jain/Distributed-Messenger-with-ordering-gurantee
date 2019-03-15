package edu.buffalo.cse.cse486586.groupmessenger2;

import android.media.MediaExtractor;

import java.io.IOException;
import java.util.Comparator;

enum MessageType{
    PROPOSAL_REQUEST, AGREEMENT, DELIVERABLE, FAILURE;
}

public class Message{
    static final int idLen=4;
    static String seperator = "<sep>";

    private int id;
    private String message;
    private MessageType type;

    /* Last 'n' didgits are the process/port number*/
    private long priority ;

    Message(int id, String message){
        this.id = id;
        this.message = message;
        this.priority = -1;
        /* When a message is first created, it needs to request proposals*/
        this.type = type.PROPOSAL_REQUEST;
    }

    Message(String string) throws IOException {
        String[] strings = string.split(seperator);
        if(strings.length == 4) {
            this.type = type.valueOf(strings[0]);
            this.id = Integer.parseInt(strings[1]);
            this.message = strings[2];
            this.priority = Long.parseLong(strings[3]);
        }
        else {
            throw new IOException("unable to parse the String");
        }
    }


    public boolean isProposal(){
        return type.equals(MessageType.PROPOSAL_REQUEST);
    }

    public boolean isAgreement(){
        return type.equals(MessageType.AGREEMENT);
    }

    public boolean isDeliverable() {
        return type.equals(MessageType.DELIVERABLE);
    }

    public boolean isFailure(){
        return type.equals((MessageType.FAILURE));
    }

    public void setFailure(){
        type = MessageType.FAILURE;
    }

    public void agree(){
        type = MessageType.AGREEMENT;
    }

    public void setToDeliverable(){
        type = MessageType.DELIVERABLE;
    }

    @Override
    public String toString() {
        return id+"   "+message;
    }

    public String allData(){
        return id+" "+message+" "+type+" "+priority;
    }

    public String encodeMessage(){
        return type + seperator + id + seperator + message + seperator + priority;
    }

    public int getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public void setPriority(long priority){
        if(this.priority < priority)
            this.priority = priority;
    }

    public long getPriority(){
        return priority;
    }

    public long getSender(){
        return id%idLen;
    }

}
