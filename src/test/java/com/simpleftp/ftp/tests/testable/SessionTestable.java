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

package com.simpleftp.ftp.tests.testable;

import com.simpleftp.ftp.connection.FTPConnectionDetails;
import com.simpleftp.ftp.connection.Server;
import com.simpleftp.sessions.Session;

/**
 * This class extends Session class so that we can access it's constructors and test it
 */
public class SessionTestable extends Session {
    /**
     * Constructs default
     */
    public SessionTestable() {
        super();
    }

    /**
     * All arg constructor
     * @param id the id to use
     * @param server the server details
     * @param connectionDetails the connection details to use
     * @param lastSession the last sessions object
     */
    public SessionTestable(int id, Server server, FTPConnectionDetails connectionDetails, LastSession lastSession) {
        super(id, server, connectionDetails, lastSession);
    }
}
