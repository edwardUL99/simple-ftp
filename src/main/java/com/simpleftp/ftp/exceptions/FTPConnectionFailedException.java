package com.simpleftp.ftp.exceptions;

import com.simpleftp.ftp.FTPServer;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * This class represents exceptions related to connection to the server and not the status of the connection, exceptions like FTPNotConnectedException handles this
 */
@Getter
public class FTPConnectionFailedException extends FTPException {
    /**
     * The FTPServer object containing the details at which the connection failed
     */
    private FTPServer ftpServer;

    /**
     * Constructs an object for this exception with the specified message and FTPServer object
     * @param message the message for this exception to display
     * @param ftpServer the FTPServer containing the details that were used to maintain the FTPConnection
     */
    public FTPConnectionFailedException(String message, FTPServer ftpServer) {
        super(message);
        this.ftpServer = ftpServer;
    }

    /**
     * Constructs an object for this exception with the specified message, causing exception and FTPServer object
     * @param message the message for this exception to display
     * @param e the causing exception for this one
     * @param ftpServer the FTPServer object containing connection details
     */
    public FTPConnectionFailedException(String message, Exception e, FTPServer ftpServer) {
        super(message, e);
        this.ftpServer = ftpServer;
    }

    /**
     * Overrides super's getMessage call by appending the ftp server obejct to the result
     * @return an enriched exception message
     */
    @Override
    public String getMessage() {
        return super.getMessage() + ", with connection details: " + ftpServer.toString();
    }
}
