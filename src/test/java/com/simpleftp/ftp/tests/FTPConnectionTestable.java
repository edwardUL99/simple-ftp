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

package com.simpleftp.ftp.tests;

import com.simpleftp.ftp.FTPConnection;
import com.simpleftp.ftp.FTPConnectionDetails;
import com.simpleftp.ftp.FTPServer;
import org.apache.commons.net.ftp.FTPClient;

/**
 * Set up to allow testing of the FTPConnection class in a much more flexible way.
 * It uses all the code of FTPConnection but offers way to alter how it works, like the ability to set certain fields
 *
 * This should only be used for testing. Not guaranteed the results if used for the actual connection
 */
public class FTPConnectionTestable extends FTPConnection {
    public FTPConnectionTestable() {
        super();
    }

    public FTPConnectionTestable(FTPClient ftpClient, FTPServer ftpServer, FTPConnectionDetails ftpConnectionDetails, boolean connected) {
        super(ftpClient, ftpServer, ftpConnectionDetails);
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }
}
