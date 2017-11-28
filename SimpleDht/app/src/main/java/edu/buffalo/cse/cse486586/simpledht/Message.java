package edu.buffalo.cse.cse486586.simpledht;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by anna on 8/12/17.
 */

public class Message implements Serializable {
    String request;
    ArrayList<Integer> ports;
    String key;
    String value;
    int cooordinator;
    int requester;


    int successor;
    int predecessor;
    String foundValue;

    public int getCooordinator(){
        return cooordinator;
    }

    public String getFoundValue() {
        return foundValue;
    }

    public void setFoundValue(String foundValue) {
        this.foundValue = foundValue;
    }



    public void setSuccessorPred(int successor, int predecessor){
        this.successor = successor;
        this.predecessor = predecessor;
    }
    public int getSuccessor(){
        return this.successor;
    }
    public int getPredecessor(){
        return this.predecessor;
    }

    public Message(String request, String key, String value, int requester){
        this.request = request;
        this.key = key;
        this.value = value;
        this.requester = requester;
        this.foundValue = "";
    }
    public boolean isRequest(String req){
        return this.request.equals(req);
    }
    public void setPorts(ArrayList<Integer> ports){
        this.ports = ports;
    }
    public ArrayList<Integer> getPorts(){
        return this.ports;
    }
    public void setCooordinator(int coordinator){
        this.cooordinator = coordinator;
    }
    public int getRequester(){
        return this.requester;
    }
    public String getKey(){
        return this.key;
    }
    public  String getValue(){
        return this.value;
    }
}
