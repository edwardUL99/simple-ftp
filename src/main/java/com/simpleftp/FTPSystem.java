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

package com.simpleftp;

import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.connection.FTPConnectionManager;
import com.simpleftp.ftp.exceptions.FTPException;

/**
 * This class is to be used to keep track of many different static variables/objects used throughout the FTP program
 */
public class FTPSystem {
    /**
     * The system connection manager
     */
    private static FTPConnectionManager connectionManager;
    /**
     * The connection to be used throughout the system
     */
    private static FTPConnection connection;
    /**
     * Flag to indicate that the system is being tested. If true, getConnectionManager will always return null and setConnectionManager will be a no-op
     */
    private static boolean systemTesting;
    /**
     * Indicates whether debugging should take place inside the system
     */
    private static final boolean debug = System.getProperty("simpleftp.debug") != null;

    /**
     * Prevent instantiation
     */
    private FTPSystem() {}

    /**
     * Allows the connection manager for this FTPSystem to be used throughout different classes.
     * @param connectionManager the connection manager to set
     */
    public static void setConnectionManager(FTPConnectionManager connectionManager) {
        if (!systemTesting)
            FTPSystem.connectionManager = connectionManager;
    }

    /**
     * Allows the retrieval of the connection manager that has been set using setConnectionManager
     * @return the connection manager set for the system, may be null
     */
    public static FTPConnectionManager getConnectionManager() {
        if (!systemTesting)
            return connectionManager;
        else
            return null;
    }

    /**
     * Set this to indicate the system is being tested.
     * This flag helps ensure there are no junit/maven synchronization problems
     * @param systemTesting true if systemTesting is to be enabled
     */
    public static void setSystemTestingFlag(boolean systemTesting) {
        FTPSystem.systemTesting = systemTesting;
    }

    /**
     * Returns true if the system is under test
     * @return system under test status
     */
    public static boolean isSystemTesting() {
        return systemTesting;
    }

    /**
     * Sets the connection to be used throughout the system
     * @param connection the connection to use
     */
    public static void setConnection(FTPConnection connection) {
        if (FTPSystem.connection != null) {
            try {
                FTPSystem.connection.disconnect();
            } catch (FTPException ex) {
                ex.printStackTrace();
            }
        }
        FTPSystem.connection = connection;
    }

    /**
     * Gets the connection used throughout the system
     * @return the connection being used
     */
    public static FTPConnection getConnection() {
        return connection;
    }

    /**
     * Returns whether debugging is enabled. This can be only enabled at startup with the property -Dsimpleftp.debug
     * @return true if debugging is enabled, false otherwise
     */
    public static boolean isDebugEnabled() {
        return debug;
    }
}
