// Nicholas Delli Carpini
package main.server;

import java.net.*;
import java.io.*;
import java.util.*;

// ServerInputThread handles a clients inputs and feeds them to the appropriate msg queue. It also handles
// creating the ClientInfo for a client, validating the client's name. Every client has a ServerInputThread
// associated with it.
public class ServerInputThread implements Runnable {

    // --- FIELDS ---
    private Server server;
    private Socket socket;
    private BufferedReader input;
    private BufferedWriter output;

    private ClientInfo client;

    InetAddress address;
    boolean whisperMode = false;
    String whisperTo = "";

    // --- CONSTRUCTOR ---
    public ServerInputThread(Server server, Socket socket) {
        this.server = server;
        this.socket = socket;
        this.address = socket.getInetAddress();

        new Thread(this).start();
    }

    // --- RUN ---
    public void run() {
        try {
            try {
                // reader and writer for sending data between server threads and client
                this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                Server.serverPrint("Connected to " + this.address);

                // username
                String temp = "";
                while(this.client == null) {
                    temp = this.input.readLine();

                    // disconnect
                    if (temp.startsWith("/disconnect")) {
                        this.disconnect();
                        return;
                    }
                    else if (temp.startsWith("/username")){
                        this.initClient(temp.replace("/username ", ""));
                    }
                }
            }
            catch (Exception e) {
                Server.serverPrint("ERROR: Failed to Communicate with Client\n");
                return;
            }

            // launch the ServerOutputThread after ClientInfo initialization
            new ServerOutputThread(this.server, this.output, this.client.getName(), this.address);

            Server.serverPrint(this.address + " confirmed as user " + this.client.getName() + "\n");

            // main msg loop
            String msg = "";
            while (true) {
                try {
                    msg = this.input.readLine();

                    // disconnect
                    if (msg.startsWith("/disconnect")) {
                        this.disconnect();
                        return;
                    }

                    // user list
                    else if (msg.startsWith("/users")) {
                        ArrayList<String> users = this.server.getUsers();
                        StringBuilder retString = new StringBuilder("USERS: ");

                        for (int i = 0; i < users.size(); i++) {
                            if (users.get(i).equals(this.client.getName())) {
                                retString.append(users.get(i)).append(" (YOU)");
                            }
                            else {
                                retString.append(users.get(i));
                            }

                            if (i < users.size() - 1) {
                                retString.append(", ");
                            }
                        }

                        this.sendToClient(retString + "\n");
                    }

                    // whisper to user
                    else if (msg.startsWith("/whisper")) {
                        String whisperArg = msg.replace("/whisper ", "").trim();

                        // if whisper is missing args
                        if (whisperArg.equals("/whisper") || whisperArg.equals("")) {
                            this.sendToClient("ERROR: Correct usage: /whisper [name|off]\n");
                        }

                        // disable whisper
                        else if (this.whisperMode && whisperArg.equals("off")) {
                            this.sendToClient("You are no longer whispering with [" + this.whisperTo + "]\n");

                            this.whisperMode = false;
                            this.whisperTo = "";
                        }

                        // enable whisper with user specified in whisperArg
                        else {
                            if (whisperArg.equals(this.client.getName())) {
                                this.sendToClient("ERROR: You cannot whisper yourself\n");
                            }
                            else {
                                if (!this.server.getUsers().contains(whisperArg)) {
                                    this.sendToClient("ERROR: User [" + whisperArg + "] does not exist\n");
                                }
                                else {
                                    this.whisperMode = true;
                                    this.whisperTo = whisperArg;

                                    this.sendToClient("You are now whispering with ["
                                            + whisperArg + "]. To stop whispering use [/whisper off]\n");
                                }
                            }
                        }
                    }

                    // basic help print
                    else if (msg.startsWith("/help")) {
                        this.sendToClient("COMMANDS: /disconnect, /help, /users, /whisper [name|off]\n");
                    }

                    // if invalid command
                    else if (msg.startsWith("/")) {
                        this.sendToClient("WARNING: Invalid Command | Use [/help] to see all commands\n");
                    }

                    // regular message
                    else if (!msg.equals("")){
                        // whisper msg -> whisperTo
                        if (this.whisperMode) {
                            if (!this.server.getUsers().contains(this.whisperTo)) {
                                this.whisperMode = false;
                            }
                        }

                        if (this.whisperMode) {
                            String whisperMsg = "[" + new Date() + " | "
                                    + this.client.getName() + "] " + "(whispering...) " + msg;

                            this.sendToClient(whisperMsg);
                            this.server.addWhisper(this.whisperTo, whisperMsg);
                        }

                        // broadcast msg -> all clients
                        else {
                            this.server.addMsg("[" + new Date() + " | " + this.client.getName() + "] " + msg);
                        }
                    }
                }
                catch (SocketException e) {
                    Server.serverPrint("WARNING: Dirty Disconnection " + this.address);
                    Server.serverPrint("Force Removing Connection " + this.address +  "\n");
                    this.disconnect();
                    return;
                }
                catch (Exception e) {
                    Server.serverPrint("WARNING: Failed to Read Msg " + this.address);
                    Server.serverPrint("Force Removing Connection " + this.address +  "\n");
                    this.disconnect();
                    return;
                }
            }
        }
        catch (Exception e) {
            Server.serverPrint("ERROR: Failed to Start Server Thread " + this.address);
            Server.serverPrint("Force Removing Connection " + this.address +  "\n");
        }
    }

    // initClient confirms the username and sets client info in client and server client list
    // name - name for user
    //
    // returns null
    private void initClient(String name) throws Exception {
        // if username is invalid
        if (name.equals("")) {
            this.output.write("ERROR: Invalid Username\n");
            this.output.flush();

            return;
        }

        // check if username is taken
        else {
            for (String user : this.server.getUsers()) {
                if (name.equals(user)) {
                    this.output.write("ERROR: Username already taken\n");
                    this.output.flush();

                    return;
                }
            }
        }

        // handshake to confirm username meets server criteria
        this.output.write("/username accepted\n");
        this.output.flush();

        String msg = this.input.readLine();

        // confirm successful handshake
        if (msg.equals("/username confirmed")) {
            this.client = new ClientInfo(name, this.address);

            this.server.addUser(this.client);
        }
        else {
            throw new Exception();
        }
    }

    // sendToClient whispers a message to the ServerInputThread's client
    // msg - msg to send to client
    //
    // return null
    private void sendToClient(String msg) {
        this.server.addWhisper(this.client.getName(), msg);
    }

    // disconnect closes the connection and removes the client from server client lists
    //
    // returns null
    public void disconnect() {
        try {
            Server.serverPrint("Attempting to Disconnect " + this.address + "...");

            this.input.close();
            this.socket.close();

            if (this.client != null) {
                this.server.removeUser(this.client);
            }

            Server.serverPrint("Disconnected " + this.address + "\n");
        }
        catch (Exception e) {
            Server.serverPrint("WARNING: Failed to Close Thread Connection " + this.address);
            Server.serverPrint("Force Removing Connection " + this.address +  "\n");
            this.server.removeUser(this.client);
        }
    }
}


