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

package com.simpleftp.ftp;

import com.simpleftp.ftp.exceptions.FTPException;

import java.util.ArrayList;

/**
 * This class is responsible for managing FTP connections.
 * It is a single class, so can only use one instance of it
 */
public class FTPConnectionManager {
    private static FTPConnectionManager instance;
    private ArrayList<FTPConnection> managedConnections; // this list may be used later to monitor each connection

    private FTPConnectionManager() {
        managedConnections = new ArrayList<>();
    }

    /**
     * Lazily instantiates the instance for this FTPConnectionManager
     * @return the single instance of this FTPConnectionManager
     */
    public static FTPConnectionManager getInstance() {
        if (instance == null) {
            instance = new FTPConnectionManager();
        }

        return instance;
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
     * Returns a FTPConnection that has all the details required to connect and login. It is just sitting "idle" waiting to connect and then login
     * @param server the server hostname
     * @param user the username
     * @param password the server password
     * @param port the port of the ftp server
     * @return an idle FTPConnection
     */
    public FTPConnection createIdleConnection(String server, String user, String password, int port) {
        FTPServer serverDetails = new FTPServer(server, user, password, port);
        FTPConnection connection = new FTPConnection();
        connection.setFtpServer(serverDetails);
        managedConnections.add(connection);
        return connection;
    }

    /**
     * Creates a FTPConnection that is connected and just needs to be logged in
     * @param server the server hostname
     * @param user the username
     * @param password the server password
     * @param port the port of the ftp server
     * @return a connected connection, null if an exception occurred
     */
    public FTPConnection createConnectedConnection(String server, String user, String password, int port) {
        FTPServer serverDetails = new FTPServer(server, user, password, port);
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

    /**
     * Creates a connected and logged in connection
     * @param server the server host name
     * @param user the username
     * @param password the server password
     * @param port the server port
     * @return a connected and logged in connection, null if an error occurs
     */
    public FTPConnection createReadyConnection(String server, String user, String password, int port) {
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
}
