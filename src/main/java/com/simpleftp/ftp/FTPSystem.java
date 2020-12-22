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

import com.simpleftp.ftp.connection.FTPConnection;
import lombok.Getter;
import lombok.Setter;

/**
 * This class is to be used to keep track of many different static variables/objects used throughout the FTP program
 */
public class FTPSystem {
    /**
     * The connection to be used throughout the system
     */
    protected static FTPConnection connection;
    /**
     * Indicates whether debugging should take place inside the system
     */
    private static final boolean debug = System.getProperty("simpleftp.debug") != null;
    /**
     * Indicates system testing is taking place
     */
    @Getter
    @Setter
    private static boolean systemTesting;

    /**
     * Prevent instantiation outside the FTP package
     */
    protected FTPSystem() {}

    /**
     * Gets the connection used throughout the system
     * @return the connection being used
     */
    public static FTPConnection getConnection() {
        return connection;
    }

    /**
     * Resets the connection, i.e. sets it to null
     */
    public static void reset() {
        connection = null;
    }

    /**
     * Returns whether debugging is enabled. This can be only enabled at startup with the property -Dsimpleftp.debug
     * @return true if debugging is enabled, false otherwise
     */
    public static boolean isDebugEnabled() {
        return debug;
    }
}
