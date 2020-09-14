/*
 *  Copyright (C) 2020  Edward Lynch-Milner
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.simpleftp.ftp.exceptions;

import com.simpleftp.ftp.FTPServer;
import lombok.Getter;

/**
 * This class represents exceptions related to connection to the server and errors sending and receving commands over this connection and not the status of the connection, exceptions like FTPNotConnectedException handles this
 *
 * This exception represents a serious fault with the connection to the server, so if this is thrown, the FTP connection should be logged out and closed before throwing it
 * Normal FTP connection is not possible after this exception is thrown
 *
 * It is a fault otherwise if this is thrown and FTPConnection.isConnected() or FTPConnection.isLoggedIn() still return true after it
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
