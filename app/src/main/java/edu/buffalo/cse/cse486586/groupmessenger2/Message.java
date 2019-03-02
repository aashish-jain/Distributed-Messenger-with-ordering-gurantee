package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.List;

public final class Message {
    String message;
    float priority;
    boolean deliverable;

    public Message(String message){
        this.message = message;
        this.priority = 0;
        this.deliverable = false;
    }

    @Override
    public String toString() {
        return priority+'\n'+message;
    }

    /**
     * Updates the priority if the new priority is higher
     */
    public void updatePriority(float priority){
        this.priority = (priority > this.priority)? priority : this.priority;
    }

    public void setDeliverable() {
        this.deliverable = true;
    }
}
