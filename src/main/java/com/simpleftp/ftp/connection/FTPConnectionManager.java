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

package com.simpleftp.ftp.connection;

import com.simpleftp.ftp.exceptions.FTPException;
import lombok.Setter;

import java.util.ArrayList;

/**
 * This class is responsible for managing FTP connections.
 *
 * It is used internally by FileSystem classes (if a FTPConnection is not specifically provided) and CommonFile implementations.
 * Those classes call the createReadyConnection method. If you want to use a single connection instance throughout the system, but don't want it initially ready (connected and logged in),
 * you can call any of the createXXConnection method and change any settings with it. Provided you then connect and log in successfully and call addConnection, this connection is ready.
 * If you call createReadyConnection then, even though the connection was initially created with createIdleConnection, provided it is using the same details as the call to createIdleConenction,
 * AND isConnected() and isLoggedIn() still returns true, this connection will be returned.
 */
public class FTPConnectionManager {
    @Setter
    private ArrayList<FTPConnection> managedConnections; // this list may be used later to monitor each connection

    private enum ConnectionStates {
        IDLE,
        CONNECTED,
        READY
    }

    private FTPException lastException;


    /**
     * Constructs a FTPConnectionManager;
     */
    public FTPConnectionManager() {
        managedConnections = new ArrayList<>();
    }

    /**
     * Returns a dead FTPConnection, i.e. it has no connection details associated with it at all
     * @return an empty FTPConnection, Will need to be configured before use
     */
    public FTPConnection createDeadConnection() {
        lastException = null;
        FTPConnection deadConnection = new FTPConnection();
        managedConnections.add(deadConnection);
        return deadConnection;
    }

    /**
     * Returns a FTPConnection that has all the details required to connect and login. It is just sitting "idle" waiting to connect and then login.
     * If there is already a connection identified by the parameters, that will be returned instead
     * @param server the server hostname
     * @param user the username
     * @param password the server password
     * @param port the port of the ftp server
     * @return an idle FTPConnection
     */
    public FTPConnection createIdleConnection(String server, String user, String password, int port) {
        lastException = null;
        FTPConnection existing = checkAlreadyExists(server, user, password, port, ConnectionStates.IDLE);
        if (existing == null) {
            FTPServer serverDetails = new FTPServer(server, user, password, port);
            FTPConnection connection = new FTPConnection();
            connection.setFtpServer(serverDetails);
            managedConnections.add(connection);
            return connection;
        }

        return existing;
    }

    /**
     * Creates a FTPConnection that is connected and just needs to be logged in.
     * If there is already a connection connected identified by the parameters, that will be returned instead
     * @param server the server hostname
     * @param user the username
     * @param password the server password
     * @param port the port of the ftp server
     * @return a connected connection, null if an exception occurred
     */
    public FTPConnection createConnectedConnection(String server, String user, String password, int port) {
        lastException = null;
        FTPConnection existing = checkAlreadyExists(server, user, password, port, ConnectionStates.CONNECTED);
        FTPServer serverDetails = new FTPServer(server, user, password, port);

        if (existing == null) {
            FTPConnection connection = new FTPConnection();
            connection.setFtpServer(serverDetails);

            try {
                boolean connected = connection.connect();
                if (connected) {
                    managedConnections.add(connection);
                    return connection;
                } else {
                    return null;
                }
            } catch (FTPException ex) {
                lastException = ex;
                return null;
            }
        }

        return existing;
    }

    /**
     * Creates a connected and logged in connection.
     * If there is already a connection logged in and connected identified by the parameters, that will be returned instead
     * @param server the server host name
     * @param user the username
     * @param password the server password
     * @param port the server port
     * @return a connected and logged in connection, null if an error occurs
     */
    public FTPConnection createReadyConnection(String server, String user, String password, int port) {
        lastException = null;
        FTPConnection existing = checkAlreadyExists(server, user, password, port, ConnectionStates.READY);

        if (existing == null) {
            FTPServer serverDetails = new FTPServer(server, user, password, port);
            FTPConnection connection = new FTPConnection();
            connection.setFtpServer(serverDetails);

            try {
                boolean connected = connection.connect();

                if (connected) {
                    boolean loggedIn = connection.login();
                    if (loggedIn) {
                        managedConnections.add(connection);
                        return connection;
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } catch (FTPException ex) {
                lastException = ex;
                return null;
            }
        }

        return existing;
    }

    /**
     * Checks if a connection matching these connection parameters and state exists and if so returns it, null otherwise
     * @param server the server name to check for
     * @param user the username to check for
     * @param password the password that the user is logged in with
     * @param port the port the connection is on
     * @param connectionState the state of the connection. For all states (except DeadConnection (that state isn't mapped), each connection in managedConnections is checked for following:
     *                        <ul>
     *                          <li>server equals the connection server</li>
     *                          <li>user equals the connection user</li>
     *                          <li>password equals the user password for the connection</li>
     *                          <li>port matches the connection's port</li>
     *                        </ul>
     *
     *                        If any of these are not equal with the equivalent value in the connection in the list, it is not equals and the next connection is checked.
     *
     *                        Every value must be equal and for each connection state:
     *                        IDLE - isConnected() and isLoggedIn() both return false
     *                        CONNECTED - isConnected() returns true but isLoggedIn() returns false
     *                        READY - isConnected() and isLoggedIn() both return true
     * @return a matching connection, null if none found
     */
    private FTPConnection checkAlreadyExists(String server, String user, String password, int port, ConnectionStates connectionState) {
        for (FTPConnection connection : managedConnections) {
            FTPServer ftpServer = connection.getFtpServer();
            String cServer = ftpServer.getServer();
            String cUser = ftpServer.getUser();
            String cPass = ftpServer.getPassword();
            int cPort = ftpServer.getPort();

            if (cServer.equals("") && cUser.equals("") && cPass.equals("") && cPort == 0) {
                // we have a dead connection, just continue as a dead connection can be edited afterwards and will then be caught by the other methods if it exists for same parameters
                continue;
            }

            boolean equals = cServer.equals(server) && cUser.equals(user) && cPass.equals(password) && cPort == port;

            if (equals) {
                switch (connectionState) {
                    case IDLE:
                        if (!connection.isConnected() && !connection.isLoggedIn())
                            return connection;
                        else
                            break;
                    case CONNECTED:
                        if (connection.isConnected() && !connection.isLoggedIn())
                            return connection;
                        else
                            break;
                    case READY:
                        if (connection.isConnected() && connection.isLoggedIn())
                            return connection;
                        else
                            break;
                    default:
                        break;
                }
            }
        }

        return null;
    }

    /**
     * Adds the specific connection to the manager.
     * If you have a connection that is connected and logged in and you want FileSystems to use this (they call createReadyConnection), it should be added with this so that when used in those classes
     * with the same credentials, you will get the same connection.
     * If that connection somehow loses connection, createReadyConnection, will return a new one as the conditions for createReadyConnection to return an existing connection won't work.
     * Unless that same connection is re-connected and logged in, add the new connected and logged in one with same credentials here
     * @param connection the connection to add
     */
    public void addConnection(FTPConnection connection) {
        if (connection != null) {
            managedConnections.add(connection);
        }
    }

    /**
     * If any createXXConnection returned null, you may call this method to retrieve the exception that may have caused the issue.
     * This method is added as there are too many classes now relying on this class to start throwing the exceptions as they happen.
     * May be refactored in the future
     * @return the last FTPException that occurred. Should be called directly after a createXXConnection method is called, as on each call to these methods, this will return null, unless an exception occurred
     */
    public FTPException getLastException() {
        return lastException;
    }
}
