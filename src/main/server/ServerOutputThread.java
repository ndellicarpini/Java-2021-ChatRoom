// Nicholas Delli Carpini
package main.server;

import java.io.*;
import java.net.*;

// ServerOutputThread handles sending messages to a client by continually popping msgs off of the client's
// msg queue. Each client has a ServerOutputThread associated with it.
public class ServerOutputThread implements Runnable {

    // --- FIELDS ---
    private Server server;
    private BufferedWriter output;
    private String clientN;
    InetAddress address;

    // --- CONSTRUCTOR ---
    public ServerOutputThread(Server server, BufferedWriter output, String clientN, InetAddress address) {
        this.server = server;
        this.output = output;
        this.clientN = clientN;
        this.address = address;

        new Thread(this).start();
    }

    // --- RUN ---
    public void run() {
        while (true) {
            try {
                if (server.haveMsg(clientN)) {
                    this.output.write(server.getMsg(clientN) + "\n");
                    this.output.flush();
                }
            }
            catch (Exception e) {
                try {
                    this.output.close();
                }
                catch (Exception ee) {
                    Server.serverPrint("ERROR: Could not Cleanly close SeverOutputThread for "
                            + this.address + "\n");
                }
                break;
            }
        }
    }
}


