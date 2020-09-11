package com.simpleftp.ftp.unit;

import com.simpleftp.ftp.FTPConnection;
import com.simpleftp.ftp.FTPConnectionDetails;
import com.simpleftp.ftp.FTPServer;
import org.apache.commons.net.ftp.FTPClient;

/**
 * Set up to allow testing of the FTPConnection class in a much more flexible way.
 * It uses all the code of FTPConnection but offers way to alter how it works, like the ability to set certain fields
 *
 * This is only accessible within the com.simpleftp.ftp.unit package of the src/tests directory
 */
class FTPConnectionTestable extends FTPConnection {
    FTPConnectionTestable() {
        super();
    }

    FTPConnectionTestable(FTPClient ftpClient, FTPServer ftpServer, FTPConnectionDetails ftpConnectionDetails, boolean connected) {
        super(ftpClient, ftpServer, ftpConnectionDetails, connected);
    }

    void setConnected(boolean connected) {
        this.connected = connected;
    }
}
