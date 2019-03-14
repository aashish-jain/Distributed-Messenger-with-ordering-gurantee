package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.IOException;
import java.util.Comparator;

enum MessageType{
    PROPOSAL_REQUEST, AGREEMENT;
}

public class Message{
    static final int idLem=4;
    String seperator = "<sep>";

    private int id, proposalCount;
    private String message;
    private MessageType type;
    private float priority ;


    public boolean isDeliverable() {
        return type.equals(MessageType.AGREEMENT);
    }

    void agreed(){

    }

    Message(int id, String message){
        this.id = id;
        this.message = message;
        this.priority = -1;
        /* When a message is first created, it needs to request proposals*/
        this.type = type.PROPOSAL_REQUEST;
    }

    Message(Message message){
        this.id = message.id;
        this.message = message.message;
        this.type = message.type;
        this.priority = message.priority;
    }

    Message(String string) throws IOException {
        String[] strings = string.split(seperator);
        if(strings.length == 4) {
            this.type = type.valueOf(strings[0]);
            this.id = Integer.parseInt(strings[1]);
            this.message = strings[2];
            this.priority = Float.parseFloat(strings[3]);
        }
        else {
            throw new IOException("unable to parse the String");
        }
    }

    @Override
    public String toString() {
        return id+"   "+message;
    }

    public String encodeMessage(){
        return type + seperator + id
                +seperator + message + seperator + priority;
    }

    public int getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public void setPriority(float priority){
        proposalCount += 1;
        if(priority > priority) {
            this.priority = priority;
            type = MessageType.PROPOSAL_REQUEST;
        }
    }

    public float getPriority(){
        return priority;
    }

    public MessageType getType() {
        return type;
    }

    public int getProposalCount() {
        return proposalCount;
    }
}

class MessageComparator implements Comparator<Message>{

    @Override
    public int compare(Message m1, Message m2) {
//        if(m1.getPriority() > m2.getPriority())
        if(m1.getId() > m2.getId())
            return 1;
        else
            return -1;
    }
}
