// Nicholas Delli Carpini
package main.client;

import java.net.*;
import java.io.*;

// Client is an executable class that handles the user input, and confirms the username with the
// server. Client creates a separate ClientReceiveThread to handle msgs from the server.
public class Client {

    // --- FIELDS ---
    private String name = "";
    private int port = 5000;
    private int timeout = 10000;
    private int disconnectMode = 0;

    private Socket socket;
    private BufferedReader userInput;
    private BufferedWriter output;

    private ClientReceiveThread receiver;

    // --- CONSTRUCTOR ---
    public Client(String address, BufferedReader userInput) {
        try {
            System.out.println("Connecting to Server [" + address + ":" + this.port + "]...");

            this.socket = new Socket();
            this.socket.connect(new InetSocketAddress(address, this.port), timeout);

            System.out.println("Connected to Server");

            try {
                // readers & writer for client
                this.userInput = userInput;
                this.output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                // launch the ClientReceiverThread to handle msgs from the server
                this.receiver = new ClientReceiveThread(this);

                // username
                String temp = "";
                while (this.name.equals("")) {
                    System.out.print("\nPlease Enter a Username: ");

                    temp = this.userInput.readLine();

                    if (temp.equals("/disconnect")) {
                        this.output.write(temp + "\n");
                        this.output.flush();

                        this.disconnect(1);
                        return;
                    }
                    else {
                        this.setName(temp);
                    }
                }

                // main msg loop
                String userMsg = "";
                while (true) {
                    try {
                        userMsg = this.userInput.readLine();

                       if (!userMsg.equals("")){
                            this.output.write(userMsg + "\n");
                            this.output.flush();

                            // disconnect
                           if (userMsg.startsWith("/disconnect")) {
                               this.disconnect(1);
                               return;
                           }
                        }
                    }
                    catch (Exception e) {
                        System.out.println("ERROR: Failed to Send Msg\n");
                        this.disconnect(-1);
                        return;
                    }
                }
            }
            catch (Exception e) {
                System.out.println("ERROR: Failed to Communicate with Server\n");
                this.disconnect(-1);
            }
        }
        catch (SocketTimeoutException e) {
            System.out.println("ERROR: Timed Out Connecting to Server\nDisconnected");
            this.disconnectMode = -1;
        }
        catch (Exception e) {
            System.out.println("ERROR: Failed to Connect to Server\nDisconnected");
            this.disconnectMode = -1;
        }
    }

    // setName sends the username request to the server then waits for acceptance. Once
    // acceptance has been sent by server, client sends back confirmation to let client
    // into the chat room.
    // input - requested name
    //
    // returns null
    private void setName(String input) {
            try {
                String lastlastMsg = this.receiver.getLastMsg();

                // send username to server
                this.output.write("/username " + input + "\n");
                this.output.flush();

                // wait for response from server
                while(lastlastMsg.equals(this.receiver.getLastMsg())) {}

                // ensure response is acceptance
                if (this.receiver.getLastMsg().equals("/username accepted")) {
                    this.name = input;

                    // send confirmation back to server
                    this.output.write("/username confirmed\n");
                    this.output.flush();
                }

                this.receiver.setLastMsg("");
            }
            catch (Exception e) {
                System.out.println("ERROR: Failed to Send Msg\n");
            }
    }

    // disconnect closes the connection and removes the client from server client lists
    // connected - type of disconnection that occurs -> if error disconnect give user
    //             option to reconnect
    //
    // returns null
    public void disconnect(int connected) {
        try {
            System.out.println("Disconnecting from Server...");
            this.output.close();
            this.socket.close();

            this.disconnectMode = connected;
            if (this.disconnectMode == 1) {
                this.userInput.close();
            }

            System.out.println("Disconnected");
        }
        catch (Exception e) {
            System.out.println("ERROR: Failed to Close Connection\n");
            e.printStackTrace();
        }
    }

    // disconnectionMode getter
    public int getDisconnectionMode() {
        return this.disconnectMode;
    }

    // socket getter
    public Socket getSocket() { return this.socket; }

    // tryConnection gives user input for server ip address, then tries to connect to server
    // userInput - single cmd line input that is shared with the client
    //
    // returns the successful Client connection
    public static Client tryConnection(BufferedReader userInput) {

        System.out.print("\nPlease Enter Server Address: ");
        String address = "";
        try {
            address = userInput.readLine();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return new Client(address, userInput);
    }

    // --- MAIN ---
    public static void main(String[] args) {
        System.out.println("Java Chat Client");

        try {
            System.out.println("Current IP Address: " + InetAddress.getLocalHost().getHostAddress());
        }
        catch (Exception e) {
            System.out.println("ERROR: Could not get Client's IP Address");
            e.printStackTrace();
        }

        BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
        Client client = tryConnection(userInput);

        // while connection has not completed cleanly, attempt to reconnect
        while (client.getDisconnectionMode() != 1) {
            if (client.getDisconnectionMode() == -1) {
                client = tryConnection(userInput);
            }
        }
    }
}
