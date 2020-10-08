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

import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.filesystem.RemoteFileSystem;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.ftp.FTPServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class RemoteFileSystemIntegrationTest {
    private FakeFtpServer ftpServer;
    private RemoteFileSystem remoteFileSystem;

    private static final String TEST_SERVER = "localhost";
    private static final String TEST_SERVER_USER = "test-user";
    private static final String TEST_SERVER_PASSWORD = "test-user-password";
    private static final int TEST_SERVER_PORT = 1234;
    private static final String TEST_HOME = "/test";
    private static final String TEST_PATH = "/test/path";
    private static final String TEST_FTP_FILE = "/test/path/file.txt";

    @TempDir
    File mockDir;

    @BeforeEach
    void setup() throws Exception {
        ftpServer = new FakeFtpServer();
        ftpServer.addUserAccount(new UserAccount(TEST_SERVER_USER, TEST_SERVER_PASSWORD, TEST_HOME));

        FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry(TEST_PATH));
        ftpServer.setFileSystem(fileSystem);
        ftpServer.setServerControlPort(TEST_SERVER_PORT);

        ftpServer.start();
        setupProperties();
        remoteFileSystem = new RemoteFileSystem();
    }

    @AfterEach
    void tearDown() {
        ftpServer.stop();
    }

    private void setupProperties() {
        System.setProperty("ftp-server", TEST_SERVER);
        System.setProperty("ftp-user", TEST_SERVER_USER);
        System.setProperty("ftp-pass", TEST_SERVER_PASSWORD);
        System.setProperty("ftp-port", "" + TEST_SERVER_PORT);
    }

    private RemoteFile createRemoteFile() throws FileSystemException {
        ftpServer.getFileSystem().add(new FileEntry(TEST_FTP_FILE, "abcdef"));
        return new RemoteFile(TEST_FTP_FILE);
    }

    private LocalFile createLocalFile() {
        LocalFile file = new LocalFile(mockDir.getAbsolutePath() + "/file.txt");
        return file;
    }

    @Test
    void shouldAddFileSuccessfully() throws Exception {
        LocalFile localFile = createLocalFile();
        localFile.createNewFile();
        assertFalse(ftpServer.getFileSystem().exists(TEST_FTP_FILE));

        boolean result = remoteFileSystem.addFile(localFile, TEST_PATH);

        assertTrue(result);
        assertTrue(ftpServer.getFileSystem().exists(TEST_FTP_FILE));
        localFile.delete();
    }

    @Test
    void shouldRemoveFileSuccessfully() throws Exception {
        RemoteFile remoteFile = createRemoteFile();
        assertTrue(ftpServer.getFileSystem().exists(TEST_FTP_FILE));

        boolean result = remoteFileSystem.removeFile(remoteFile);

        assertTrue(result);
        assertFalse(ftpServer.getFileSystem().exists(TEST_FTP_FILE));
    }

    @Test
    void shouldGetFileSuccessfully() throws Exception {
        RemoteFile remoteFile = createRemoteFile();
        assertTrue(ftpServer.getFileSystem().exists(TEST_FTP_FILE));

        RemoteFile remoteFile1 = (RemoteFile)remoteFileSystem.getFile(TEST_FTP_FILE);

        assertEquals(remoteFile.getFilePath(), remoteFile1.getFilePath());
    }

    @Test
    void shouldCheckFileExistsSuccessfully() throws Exception {
        RemoteFile remoteFile = createRemoteFile();

        boolean result = remoteFileSystem.fileExists(TEST_FTP_FILE);

        assertTrue(result);
    }

    @Test
    void shouldListFilesSuccessfully() throws Exception {
        RemoteFile remoteFile = createRemoteFile();
        RemoteFile[] remoteFiles = {remoteFile};

        CommonFile[] commonFiles = remoteFileSystem.listFiles(TEST_PATH);


        assertEquals(commonFiles[0].getFilePath(), remoteFiles[0].getFilePath());
    }
}
