// Nicholas Delli Carpini
package main.server;

import java.net.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// Server is an executable class that handles the list of clients connected, and all of the messages
// going out to each client. Server creates a separate ServerInputThread & ServerOutputThread for
// each new client
public class Server {

    // --- FIELDS ---
    private ServerSocket server;
    private int port = 5000;

    private ArrayList<ClientInfo> clients = new ArrayList<>();
    private HashMap<String, Stack<String>> globalMsgQueue = new HashMap<>();

    private ReadWriteLock clientsLock = new ReentrantReadWriteLock();
    private ReadWriteLock msgQueueLock = new ReentrantReadWriteLock();

    // --- CONSTRUCTOR ---
    public Server() {
        try {
            this.server = new ServerSocket(this.port);

            Server.serverPrint("Chat Server has Started\n");

            Socket tempSock;
            while (true) {
                try {
                    tempSock = server.accept();

                    Server.serverPrint(tempSock.getInetAddress() + " Requested Connection...");
                    new ServerInputThread(this, tempSock);

                }
                catch (Exception e) {
                    Server.serverPrint("ERROR: Failed to Accept Connection\n");
                }
            }
        }
        catch (Exception e) {
            Server.serverPrint("ERROR: Failed to Start Server Socket\n");
            e.printStackTrace();
        }
    }

    // addUser adds a client to the clients list and creates a msg queue for the client
    // newClient - client to be added
    //
    // returns null
    public void addUser(ClientInfo newClient) {

        this.clientsLock.writeLock().lock();
        try {
            this.clients.add(newClient);
        }
        finally {
            this.clientsLock.writeLock().unlock();
        }

        this.msgQueueLock.writeLock().lock();
        try {
            this.globalMsgQueue.put(newClient.getName(), new Stack<>());
        }
        finally {
            this.msgQueueLock.writeLock().unlock();
        }

        this.addMsg("[" + new Date() + "] " + newClient.getName() + " has joined the Chat Server");
    }

    // removeUser removes a client from both the client list and the msg queue
    // oldClient - client to be deleted
    //
    // returns null
    public void removeUser(ClientInfo oldClient) {

        this.clientsLock.writeLock().lock();
        try {
            this.clients.remove(oldClient);
        }
        finally {
            this.clientsLock.writeLock().unlock();
        }

        this.msgQueueLock.writeLock().lock();
        try {
            this.globalMsgQueue.remove(oldClient.getName());
        }
        finally {
            this.msgQueueLock.writeLock().unlock();
        }

        this.addMsg("[" + new Date() + "] " + oldClient.getName() + " has disconnected from the Chat Server");
    }

    // getUsers gets the names of all of the clients on the server
    //
    // returns a list of string containing all of the names
    public ArrayList<String> getUsers() {
        ArrayList<String> users = new ArrayList<>();
        for (ClientInfo client : this.clients) {
            users.add(client.getName());
        }

        return users;
    }

    // addMsg pushes a new msg to every clients queue - effectively sending the message to every client
    // msg - msg to be sent to every client
    //
    // returns null
    public void addMsg(String msg) {
        for (ClientInfo client : this.clients) {
            this.globalMsgQueue.get(client.getName()).push(msg);
        }

    }

    // addWhisper sends a msg to a specific client identified by name
    // name - client that the msg will be sent to
    // msg - msg to be sent
    //
    // returns null
    public void addWhisper(String name, String msg) {
        this.globalMsgQueue.get(name).push(msg);
    }

    // haveMsg checks a specific client's msg queue and returns isEmpty()
    // name - client to check queue
    //
    // returns boolean if client msg queue is empty
    public boolean haveMsg(String name) {
        return !this.globalMsgQueue.get(name).isEmpty();
    }

    // getMsg pops the top msg off of a client's msg queue and returns the msg
    // name - client to get top msg from
    //
    // returns popped msg
    public String getMsg(String name) {
        return this.globalMsgQueue.get(name).pop();
    }

    // prints server msg in format [new Date()] + msg
    // msg - msg to print
    //
    // returns null
    public static void serverPrint(String msg) { System.out.println("[" + new Date() + "] " + msg); }

    // --- MAIN ---
    public static void main(String[] args) {
        System.out.println("Java Chat Server");

        try {
            System.out.println("Current IP Address: " + InetAddress.getLocalHost().getHostAddress() + "\n");
        }
        catch (Exception e) {
            System.out.println("ERROR: Could not get Server's IP Address");
            e.printStackTrace();
        }

        new Server();
    }
}
