package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;



public class SharedQueue {
    Lock lock;
    int sharedWith;
    int topCount;
    Message topMessage;

    /* https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/LinkedBlockingQueue.html#LinkedBlockingQueue()*/

    LinkedBlockingQueue<Message> linkedBlockingQueue;

    public SharedQueue(int sharedWith){
        this.lock = new ReentrantLock();
        this.sharedWith = sharedWith;
        this.linkedBlockingQueue = new LinkedBlockingQueue<Message>();
    }

    /* Adds the message to the end of the list */
    public void addToQueue(Message message){
        try {
            lock.lock();
            linkedBlockingQueue.put(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }


    /* Gets a new message from the front of the queue after all clients have the message*/
    public Message popFront(){
        try {
            lock.lock();

            if(topCount == 0) {
                topMessage = linkedBlockingQueue.take();
                topCount = sharedWith;
            }
            topCount--;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return topMessage;
    }

    /* If any peer is down call this function to update the sharedWith variable*/
    public void updateSharedWith(int sharedWith){
        lock.lock();
        this.sharedWith = sharedWith;
        lock.unlock();
    }
}
