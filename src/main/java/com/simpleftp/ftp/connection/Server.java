
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

import lombok.*;

/**
 * This class encapsulates the details required for connecting to a server
 */
@AllArgsConstructor
@With
@Data
@EqualsAndHashCode
public class Server implements Cloneable {
    @NonNull
    private String server;
    @NonNull
    private String user;
    @NonNull
    @EqualsAndHashCode.Exclude
    private String password;
    private int port;
    /**
     * The default port for FTP
     */
    public static final int DEFAULT_FTP_PORT = 21;

    /**
     * Initialises every field to empty string or 0
     */
    public Server() {
        server = user = password = "";
        port = 0;
    }

    /**
     * Overrides Object's toString
     * @return a String representation of this object
     */
    @Override
    public String toString() {
        return "Server Host: " + server + ", User: " + user + ", Port: " + port;
    }

    /**
     * Creates and returns a copy of this object
     * @return cloned Server, null if clone fails
     */
    @Override
    public Server clone() {
        try {
            return (Server) super.clone();
        } catch (CloneNotSupportedException ex) {
            return null;
        }
    }
}
