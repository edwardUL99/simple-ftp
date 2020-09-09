
package com.simpleftp.ftp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

//This will represent a FTP server
@NoArgsConstructor
@AllArgsConstructor
@With
@Data
public class FTPServer {
    private String server;
    private String user;
    private String password;
    private int port;

    /**
     * Overrides Object's toString
     * @return a String representation of this object
     */
    @Override
    public String toString() {
        return "Server Host: " + server + ", User: " + user + ", Port: " + port;
    }
}
