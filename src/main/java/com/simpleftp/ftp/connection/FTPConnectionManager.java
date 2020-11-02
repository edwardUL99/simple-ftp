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
 * It is a single class, so can only use one instance of it
 */
public class FTPConnectionManager {
    @Setter
    private ArrayList<FTPConnection> managedConnections; // this list may be used later to monitor each connection
    private enum ConnectionTypes {
        IDLE,
        CONNECTED,
        READY
    }


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
        FTPConnection existing = checkAlreadyExists(server, user, password, port, ConnectionTypes.IDLE);
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
        FTPConnection existing = checkAlreadyExists(server, user, password, port, ConnectionTypes.CONNECTED);
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
        FTPConnection existing = checkAlreadyExists(server, user, password, port, ConnectionTypes.READY);

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
                return null;
            }
        }

        return existing;
    }

    private FTPConnection checkAlreadyExists(String server, String user, String password, int port, ConnectionTypes connectionType) {
        for (FTPConnection connection : managedConnections) {
            String cServer = connection.getFtpServer().getServer();
            String cUser = connection.getFtpServer().getUser();
            String cPass = connection.getFtpServer().getPassword();
            int cPort = connection.getFtpServer().getPort();

            if (cServer.equals("") && cUser.equals("") && cPass.equals("") && cPort == 0) {
                // we have a dead connection, just return null as a dead connection can be edited afterwards and will then be caught by the other methods if it exists for same parameters
                return null;
            }

            boolean equals = cServer.equals(server) && cUser.equals(user) && cPass.equals(password) && cPort == port;

            switch (connectionType) {
                case IDLE: if (equals && !connection.isConnected() && !connection.isLoggedIn())
                                return connection;
                            else
                                break;
                case CONNECTED: if (equals && connection.isConnected() && !connection.isLoggedIn())
                                    return connection;
                                else
                                    break;
                case READY: if (equals && connection.isConnected() && connection.isLoggedIn())
                                    return connection;
                                else
                                    break;
                default: break;
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
}
