package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

public class HashedPriorityQueue<K,V> {
    HashMap<K,V> hashMap;
    PriorityQueue<V> priorityQueue;

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
        if(this.containsKey(key)){
            this.remove(key);
            this.add(key, value);
            success = true;
        }
        return success;
    }
}
