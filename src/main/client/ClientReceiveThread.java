// Nicholas Delli Carpini
package main.client;

import java.net.*;
import java.io.*;

// ClientReceiveThread handles the outputs from the server and displays them asynchronously
// for the client. Every client has a ClientReceiveThread.
public class ClientReceiveThread implements Runnable {

    // --- FIELDS ---
    private Client client;
    private String lastMsg = "";
    private BufferedReader reader;

    // --- CONSTRUCTOR ---
    ClientReceiveThread(Client client) {
        this.client = client;
        try {
            this.reader = new BufferedReader(new InputStreamReader(client.getSocket().getInputStream()));
        }
        catch(Exception e) {
            System.out.println("ERROR: Could not Start ClientReceiveThread");
            client.disconnect(-1);

            return;
        }

        new Thread(this).start();
    }

    // --- RUN ---
    public void run() {
        String serverMsg = "";

        while (this.client.getDisconnectionMode() == 0) {
            try {
                serverMsg = this.reader.readLine();

                // print msg if not empty
                if (!serverMsg.equals("")) {
                    if (serverMsg.charAt(0) != '/') {
                        System.out.println(serverMsg);
                    }

                    // set lastMsg for client msg compares
                    this.lastMsg = serverMsg;
                    serverMsg = "";
                }
            }
            catch (SocketException e) {
                break;
            }
            catch (Exception e) {
                System.out.println("ERROR: Failed to Receive Msg from Server\n");
                e.printStackTrace();
            }
        }

    }

    // lastMsg getter
    public String getLastMsg() {
        return lastMsg;
    }

    // lastMsg setter
    public void setLastMsg(String newMsg) { this.lastMsg = newMsg; }
}
