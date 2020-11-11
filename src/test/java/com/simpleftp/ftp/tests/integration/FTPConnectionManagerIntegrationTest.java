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

package com.simpleftp.ftp.tests.integration;

import com.simpleftp.FTPSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.connection.FTPConnectionManager;
import com.simpleftp.ftp.connection.FTPServer;
import com.simpleftp.ftp.exceptions.FTPCommandFailedException;
import com.simpleftp.ftp.exceptions.FTPConnectionFailedException;
import com.simpleftp.ftp.exceptions.FTPNotConnectedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import static org.junit.jupiter.api.Assertions.*;

public class FTPConnectionManagerIntegrationTest {
    private FakeFtpServer ftpServer;

    private static final String TEST_SERVER = "localhost";
    private static final String TEST_SERVER_USER = "test-user";
    private static final String TEST_SERVER_PASSWORD = "test-user-password";
    private static final int TEST_SERVER_PORT = 1234;
    private static final FTPServer SERVER = new FTPServer(TEST_SERVER, TEST_SERVER_USER, TEST_SERVER_PASSWORD, TEST_SERVER_PORT);

    private FTPConnectionManager manager;

    @BeforeEach
    void setup() {
        manager = new FTPConnectionManager();
        ftpServer = new FakeFtpServer();
        ftpServer.addUserAccount(new UserAccount(TEST_SERVER_USER, TEST_SERVER_PASSWORD, "/home"));
        FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry("/home"));
        ftpServer.setFileSystem(fileSystem);
        ftpServer.setServerControlPort(TEST_SERVER_PORT);

        ftpServer.start();
        FTPSystem.setSystemTestingFlag(true);
    }

    @AfterEach
    public void tearDown() {
        ftpServer.stop();
    }

    @Test
    void shouldCreateIdleConnectionSuccessfully() {
        FTPConnection idle = new FTPConnection();
        idle.setFtpServer(SERVER);
        assertEquals(idle, manager.createIdleConnection(TEST_SERVER, TEST_SERVER_USER, TEST_SERVER_PASSWORD, TEST_SERVER_PORT));
    }

    @Test
    void shouldCreateConnectedConnectionSuccessfully() throws FTPConnectionFailedException {
        FTPConnection connected = new FTPConnection();
        connected.setFtpServer(SERVER);
        assertTrue(connected.connect());
        assertEquals(connected, manager.createConnectedConnection(TEST_SERVER, TEST_SERVER_USER, TEST_SERVER_PASSWORD, TEST_SERVER_PORT));
    }

    @Test
    void shouldCreateReadyConnectionSuccessfully() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        FTPConnection connected = new FTPConnection();
        connected.setFtpServer(SERVER);
        assertTrue(connected.connect());
        assertTrue(connected.login());
        assertEquals(connected, manager.createReadyConnection(TEST_SERVER, TEST_SERVER_USER, TEST_SERVER_PASSWORD, TEST_SERVER_PORT));
    }
}
