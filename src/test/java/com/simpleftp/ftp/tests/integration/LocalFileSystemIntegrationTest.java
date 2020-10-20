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
import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.LocalFileSystem;
import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.ftp.FTPServer;
import com.simpleftp.ftp.connections.FTPConnectionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.mockftpserver.fake.filesystem.FileSystem;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class LocalFileSystemIntegrationTest {
    private FakeFtpServer ftpServer;
    private LocalFileSystem localFileSystem;

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
        fileSystem.add(new FileEntry(TEST_FTP_FILE, "abcdef"));
        ftpServer.setFileSystem(fileSystem);
        ftpServer.setServerControlPort(TEST_SERVER_PORT);

        FTPSystem.setSystemTestingFlag(true);

        ftpServer.start();
        setupProperties();
        localFileSystem = new LocalFileSystem();
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
        return new RemoteFile("/test/path/file.txt");
    }

    private LocalFile createLocalFile() {
        LocalFile file = new LocalFile(mockDir.getAbsolutePath() + "/file.txt");
        return file;
    }

    @Test
    void shouldAddFileSuccessfully() throws Exception {
        RemoteFile remoteFile = createRemoteFile();
        String localPath = mockDir.getAbsolutePath() + "/file.txt";
        File file = new File(localPath);
        assertFalse(file.exists());

        boolean added = localFileSystem.addFile(remoteFile, mockDir.getAbsolutePath());

        assertTrue(added);
        assertTrue(file.exists());
        file.delete();
    }

    @Test
    void shouldRemoveFileSuccessfully() throws Exception {
        LocalFile localFile = createLocalFile();
        localFile.createNewFile();

        assertTrue(localFile.exists());

        boolean removed = localFileSystem.removeFile(localFile);

        assertTrue(removed);
        assertFalse(localFile.exists());
    }

    @Test
    void shouldGetFileSuccessfully() throws Exception {
        LocalFile file = createLocalFile();
        file.createNewFile();
        assertTrue(file.exists());

        LocalFile returned = (LocalFile)localFileSystem.getFile(file.getFilePath());

        assertEquals(file, returned);
        file.delete();
    }

    @Test
    void shouldReturnNullOnGetFileIfNotFound() {
        LocalFile file = createLocalFile();
        LocalFile returned = (LocalFile)localFileSystem.getFile(file.getFilePath());

        assertNull(returned);
    }

    @Test
    void shouldFileExistsSuccessfully() throws Exception {
        LocalFile localFile = createLocalFile();
        localFile.createNewFile();

        boolean result = localFileSystem.fileExists(localFile.getFilePath());

        assertTrue(result);
        localFile.delete();
    }

    @Test
    void shouldListFilesSuccessfully() throws Exception {
        LocalFile localFile = createLocalFile();
        localFile.createNewFile();

        LocalFile[] localFiles = {localFile};

        CommonFile[] result = localFileSystem.listFiles(mockDir.getAbsolutePath());

        assertArrayEquals(localFiles, result);
        localFile.delete();
    }

    @Test
    void shouldNotListFilesIfDirIsNotADirectoryOnListFiles() {
        LocalFile localFile = createLocalFile();

        CommonFile[] result = localFileSystem.listFiles(localFile.getFilePath());

        assertNull(result);
    }

}
