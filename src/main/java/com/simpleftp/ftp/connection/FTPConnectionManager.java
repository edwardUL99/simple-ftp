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
import com.simpleftp.security.PasswordEncryption;
import lombok.Setter;

import java.util.ArrayList;

/**
 * This class is responsible for managing FTP connections.
 *
 * It prevents duplicate connections. I.e. multiple connections with same connection details, i.e. same user, password, server and port
 * If createXXConnection is called with same details as an already created connection, it returns the already existing one.
 *
 * If the state (XX part of method) does not match the state of that existing connection, e.g if the existing is ready, but createIdle is called, the connection will be made idle, i.e logged out and disconnected.
 * Keep that in mind when using
 */
public class FTPConnectionManager {
    @Setter
    private ArrayList<FTPConnection> managedConnections; // this list may be used later to monitor each connection

    private enum ConnectionState {
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
     * Gets the state of the provided connection
     * @param connection the connection to check state of
     * @return the ConnectionStates value
     */
    private ConnectionState getConnectionState(FTPConnection connection) {
        ConnectionState state;

        if (!connection.isConnected() && !connection.isLoggedIn()) {
            state = ConnectionState.IDLE;
        } else if (connection.isConnected() && !connection.isLoggedIn()) {
            state = ConnectionState.CONNECTED;
        } else {
            state = ConnectionState.READY;
        }

        return state;
    }

    /**
     * Handles the connection for the appropriate method to match the state of the associated method. I.e. if a connection already exists when createReadyConnection is called, but it is Idle, this will attempt to make it Ready
     * @param connection
     * @param desiredState the state the connection should be
     * @throws FTPException if an error occurs
     */
    private void handleConnection(FTPConnection connection, ConnectionState desiredState) throws FTPException {
        ConnectionState state = getConnectionState(connection);

        if (state != desiredState) {
            if (desiredState == ConnectionState.IDLE) {
                if (state == ConnectionState.READY) {
                    connection.logout();
                    connection.disconnect();
                } else if (state == ConnectionState.CONNECTED) {
                    connection.disconnect();
                }
            } else if (desiredState == ConnectionState.CONNECTED) {
                if (state == ConnectionState.IDLE) {
                    connection.connect();
                } else if (state == ConnectionState.READY) {
                    connection.logout();
                }
            } else if (desiredState == ConnectionState.READY) {
                if (state == ConnectionState.IDLE) {
                    connection.connect();
                    connection.login();
                } else if (state == ConnectionState.CONNECTED) {
                    connection.login();
                }
            }
        }
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
        FTPConnection existing = checkAlreadyExists(server, user, password, port);
        if (existing == null) {
            FTPServer serverDetails = new FTPServer(server, user, password, port);
            FTPConnection connection = new FTPConnection();
            connection.setFtpServer(serverDetails);
            managedConnections.add(connection);
            return connection;
        } else {
            try {
                handleConnection(existing, ConnectionState.IDLE);
                if (getConnectionState(existing) != ConnectionState.IDLE) return null;
            } catch (FTPException ex) {
                lastException = ex;
                return null;
            }
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
        FTPConnection existing = checkAlreadyExists(server, user, password, port);
        FTPServer serverDetails = new FTPServer(server, user, password, port);

        try {
            if (existing == null) {
                FTPConnection connection = new FTPConnection();
                connection.setFtpServer(serverDetails);

                boolean connected = connection.connect();
                if (connected) {
                    managedConnections.add(connection);
                    return connection;
                } else {
                    return null;
                }
            } else {
                handleConnection(existing, ConnectionState.CONNECTED);
                if (getConnectionState(existing) != ConnectionState.CONNECTED) return null;
            }
        } catch (FTPException ex) {
            lastException = ex;
            return null;
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
        FTPConnection existing = checkAlreadyExists(server, user, password, port);

        try {
            if (existing == null) {
                FTPServer serverDetails = new FTPServer(server, user, password, port);
                FTPConnection connection = new FTPConnection();
                connection.setFtpServer(serverDetails);

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
            } else {
                handleConnection(existing, ConnectionState.READY);
                if (getConnectionState(existing) != ConnectionState.READY) return null; // an error occurred
            }
        } catch (FTPException ex) {
            lastException = ex;
            return null;
        }

        return existing;
    }

    /**
     * Checks if a connection matching these connection parameters exists and if so returns it, null otherwise
     * @param server the server name to check for
     * @param user the username to check for
     * @param password the password that the user is logged in with
     * @param port the port the connection is on
     * each connection in managedConnections is checked for following:
     *                        <ul>
     *                          <li>server equals the connection server</li>
     *                          <li>user equals the connection user</li>
     *                          <li>password equals the user password for the connection</li>
     *                          <li>port matches the connection's port</li>
     *                        </ul>
     * @return a matching connection, null if none found
     */
    private FTPConnection checkAlreadyExists(String server, String user, String password, int port) {
        for (FTPConnection connection : managedConnections) {
            FTPServer ftpServer = connection.getFtpServer();
            String cServer = ftpServer.getServer();
            String cUser = ftpServer.getUser();
            String cPass = ftpServer.getPassword();
            int cPort = ftpServer.getPort();

            boolean equals = cServer.equals(server) && cUser.equals(user) && cPass.equals(password) && cPort == port;

            if (equals) {
                return connection;
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
     * Attempts to find an existing connection matching the parameters provided and returns it
     * @param server the server hostname
     * @param user the user logged in
     * @param pass the password of the user
     * @param port the port the server is running on
     * @return connection found if exists, null otherwise
     */
    public FTPConnection getExistingConnection(String server, String user, String pass, int port) {
        return checkAlreadyExists(server, user, pass, port);
    }

    /**
     * Checks if this connection manager has a record of a connection matching the specified parameters
     * @param server the server hostname
     * @param user the user logged in
     * @param pass the password of the user
     * @param port the port the server is running on
     * @return true if a connection matching the parameters was created by this manager, false otherwise
     */
    public boolean doesConnectionExist(String server, String user, String pass, int port) {
        return checkAlreadyExists(server, user, pass, port) != null;
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
