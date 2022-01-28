// Nicholas Delli Carpini
package main.server;

import java.net.InetAddress;

// ClientInfo holds very basic information about a client to just make it unique enough
// to be identifiable in the list
public class ClientInfo {

    // --- FIELDS ---
    String name;
    InetAddress address;

    // --- CONSTRUCTOR ---
    public ClientInfo(String name, InetAddress address) {
        this.name = name;
        this.address = address;
    }

    // name getter
    public String getName() {
        return name;
    }

    // name setter
    public void setName(String name) {
        this.name = name;
    }

    // address getter
    public InetAddress getAddress() {
        return address;
    }

    // address setter
    public void setAddress(InetAddress address) {
        this.address = address;
    }

}
