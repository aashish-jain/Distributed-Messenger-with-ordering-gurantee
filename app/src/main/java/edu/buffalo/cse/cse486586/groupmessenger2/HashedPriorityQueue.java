package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

/**
 * Hybrid of a HashMap and Priority Queue
 * @param Key  : <K>
 * @param Value : <V>
 */

public class HashedPriorityQueue<K,V> {
    private HashMap<K,V> hashMap;
    private PriorityQueue<V> priorityQueue;

    HashedPriorityQueue(int initialCapacity, Comparator<V> comparator){
        hashMap = new HashMap<K, V>();
        priorityQueue = new PriorityQueue<V>(initialCapacity, comparator);
    }

    public V find(K key){
        if(hashMap.containsKey(key))
            return hashMap.get(key);
        return null;
    }

    public boolean add(K key, V value){
        boolean success = false;
        if(!hashMap.containsKey(key)){
            hashMap.put(key, value);
            priorityQueue.offer(value);
            success = true;
        }
        return success;
    }

    public boolean remove(K key){
        V value = hashMap.remove(key);
        return priorityQueue.remove(value);
    }

    public boolean containsKey(K key){
        return hashMap.containsKey(key);
    }

    public boolean update(K key, V value){
        boolean success = false;
        if(hashMap.containsKey(key)){
            System.out.println("Contains");
            success = this.remove(key) && this.add(key, value);
        }
        return success;
    }

    public V peek(){
        return this.priorityQueue.peek();
    }

    public V poll(){
        return this.priorityQueue.poll();
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        PriorityQueue<V> pq = new PriorityQueue<V>(this.priorityQueue);
        int i = pq.size();
        for (; i!=0; i--){
            stringBuilder.append( pq.poll().toString() + "\n" );
        }
        return stringBuilder.toString();
    }
}

