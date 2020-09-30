
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

import lombok.*;

//This will represent a FTP server
@NoArgsConstructor
@AllArgsConstructor
@With
@Data
@EqualsAndHashCode
public class FTPServer {
    private String server;
    private String user;
    private String password;
    private int port;
    /**
     * The default port for FTP
     */
    public static final int DEFAULT_PORT = 21;

    /**
     * Overrides Object's toString
     * @return a String representation of this object
     */
    @Override
    public String toString() {
        return "Server Host: " + server + ", User: " + user + ", Port: " + port;
    }
}
