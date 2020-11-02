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

package com.simpleftp.ftp.tests.unit;

import com.simpleftp.FTPSystem;
import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.filesystem.RemoteFileSystem;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.connection.FTPConnectionManager;
import com.simpleftp.ftp.connection.FTPServer;
import com.simpleftp.ftp.exceptions.FTPConnectionFailedException;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.security.PasswordEncryption;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class RemoteFileSystemUnitTest {
    @Mock
    FTPConnection connection;

    @Mock
    FTPConnectionManager ftpConnectionManager;

    @InjectMocks
    RemoteFileSystem remoteFileSystem;

    @TempDir
    File mockDir;

    AutoCloseable closeable;

    private static final String TEST_SERVER = "test-server";
    private static final String TEST_USER = "test-user";
    private static final String TEST_PASS = "test-pass";
    private static final String TEST_PORT = "21";
    private static final String TEST_FILE = "/test/path/file.txt";
    private static final String TEST_PATH = "/test/path";

    @BeforeEach
    void init() {
        closeable = MockitoAnnotations.openMocks(this);
        FTPSystem.setSystemTestingFlag(true);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    private void setupProperties() {
        System.setProperty("ftp-server", TEST_SERVER);
        System.setProperty("ftp-user", TEST_USER);
        System.setProperty("ftp-pass", PasswordEncryption.encrypt(TEST_PASS));
        System.setProperty("ftp-port", TEST_PORT);
    }

    private FTPServer createFTPServer() {
        return new FTPServer(TEST_SERVER, TEST_USER, TEST_PASS, Integer.parseInt(TEST_PORT));
    }

    private RemoteFile createRemoteFile() throws FileSystemException {
        return new RemoteFile(TEST_FILE, ftpConnectionManager);
    }

    private LocalFile createLocalFile() {
        LocalFile file = new LocalFile(mockDir.getAbsolutePath() + "/file.txt");
        return file;
    }

    private FTPFile createFTPFile() {
        FTPFile ftpFile = new FTPFile();
        ftpFile.setName(TEST_FILE);
        return ftpFile;
    }

    private void mockConnection() {
        given(connection.isConnected())
                .willReturn(true);
        given(connection.isLoggedIn())
                .willReturn(true);
        given(connection.getFtpServer())
                .willReturn(createFTPServer());
        given(ftpConnectionManager.createReadyConnection(TEST_SERVER, TEST_USER, TEST_PASS, Integer.parseInt(TEST_PORT)))
                .willReturn(connection);
    }

    @Test
    void shouldAddFileSuccessfully() throws IOException, FileSystemException, FTPException {
        LocalFile localFile = createLocalFile();
        localFile.createNewFile();

        given(connection.uploadFile(localFile, TEST_PATH))
                .willReturn(createFTPFile());

        boolean result = remoteFileSystem.addFile(localFile, TEST_PATH);

        assertTrue(result);
        verify(connection).uploadFile(localFile, TEST_PATH);
    }

    @Test
    void shouldThrowIfRemoteFileIsAdded() throws FileSystemException {
        setupProperties();
        mockConnection();
        RemoteFile remoteFile = createRemoteFile();
        assertThrows(FileSystemException.class, () -> remoteFileSystem.addFile(remoteFile, TEST_PATH));
    }

    @Test
    void shouldThrowIfErrorOccursOnAddFile() throws Exception {
        LocalFile localFile = createLocalFile();
        doThrow(FTPConnectionFailedException.class).when(connection).uploadFile(localFile, TEST_PATH);
        assertThrows(FileSystemException.class, () -> remoteFileSystem.addFile(localFile, TEST_PATH));
        verify(connection).uploadFile(localFile, TEST_PATH);
    }

    @Test
    void shouldRemoveFileSuccessfully() throws FileSystemException, FTPException {
        setupProperties();
        mockConnection();
        RemoteFile remoteFile = createRemoteFile();

        given(connection.getFTPFile(TEST_FILE))
                .willReturn(remoteFile);
        given(connection.removeDirectory(TEST_FILE))
                .willReturn(true);

        boolean result = remoteFileSystem.removeFile(remoteFile);

        assertTrue(result);
    }

    @Test
    void shouldThrowIfLocalFileIsRemoved() {
        LocalFile localFile = createLocalFile();

        assertThrows(FileSystemException.class, () -> remoteFileSystem.removeFile(localFile));
    }

    @Test
    void shouldThrowIfErrorOccursOnRemoveFile() throws Exception {
        setupProperties();
        mockConnection();
        RemoteFile remoteFile = createRemoteFile();
        given(connection.getFTPFile(TEST_FILE))
                .willReturn(remoteFile);
        doThrow(FTPConnectionFailedException.class).when(connection).removeDirectory(TEST_FILE);
        assertThrows(FileSystemException.class, () -> remoteFileSystem.removeFile(TEST_FILE));
    }

    @Test
    void shouldGetFileSuccessfully() throws Exception {
        setupProperties();
        mockConnection();
        RemoteFile remoteFile = createRemoteFile();

        given(connection.getFTPFile(TEST_FILE))
                .willReturn(createFTPFile());

        RemoteFile result = (RemoteFile)remoteFileSystem.getFile(TEST_FILE);

        assertEquals(remoteFile.getName(), result.getName());
        verify(connection).getFTPFile(TEST_FILE);
    }

    @Test
    void shouldReturnNullIfFileNotFoundOnGetFile() throws Exception {
        given(connection.getFTPFile(TEST_FILE))
                .willReturn(null);

        RemoteFile result = (RemoteFile)remoteFileSystem.getFile(TEST_FILE);

        assertNull(result);
        verify(connection).getFTPFile(TEST_FILE);
    }

    @Test
    void shouldThrowIfErrorOccursOnGetFile() throws Exception {
        doThrow(FTPConnectionFailedException.class).when(connection).getFTPFile(TEST_FILE);
        assertThrows(FileSystemException.class, () -> remoteFileSystem.getFile(TEST_FILE));
        verify(connection).getFTPFile(TEST_FILE);
    }

    @Test
    void shouldCheckFileExistsSuccessfully() throws Exception {
        given(connection.remotePathExists(TEST_FILE, false))
                .willReturn(true); //in reality this is a file, so you know dir true will always return false

        boolean result = remoteFileSystem.fileExists(TEST_FILE);

        assertTrue(result);
        verify(connection).remotePathExists(TEST_FILE, false);
    }

    @Test
    void shouldThrowIfErrorOccursOnFileExists() throws Exception {
        doThrow(FTPConnectionFailedException.class).when(connection).remotePathExists(TEST_FILE, false);
        assertThrows(FileSystemException.class, () -> remoteFileSystem.fileExists(TEST_FILE));
        verify(connection).remotePathExists(TEST_FILE, false);
    }

    @Test
    void shouldListFilesSuccessfully() throws Exception {
        setupProperties();
        mockConnection();
        RemoteFile[] files = {createRemoteFile()};
        FTPFile[] ftpFiles = {createFTPFile()};
        given(connection.listFiles(TEST_PATH))
                .willReturn(ftpFiles);
        given(connection.remotePathExists(TEST_PATH, true))
                .willReturn(true);

        CommonFile[] result = remoteFileSystem.listFiles(TEST_PATH);

        assertEquals(files[0].getName(), result[0].getName());
        verify(connection).remotePathExists(TEST_PATH, true);
        verify(connection).listFiles(TEST_PATH);
    }

    @Test
    void shouldReturnNullIfDirDoesNotExistOrIsAFile() throws Exception {
        given(connection.remotePathExists(TEST_PATH, true))
                .willReturn(false);
        assertNull(remoteFileSystem.listFiles(TEST_PATH));
        verify(connection).remotePathExists(TEST_PATH, true);
    }

    @Test
    void shouldThrowIfErrorOccurs() throws Exception {
        doThrow(FTPConnectionFailedException.class).when(connection).remotePathExists(TEST_PATH, true);
        assertThrows(FileSystemException.class, () -> remoteFileSystem.listFiles(TEST_PATH));
        verify(connection).remotePathExists(TEST_PATH, true);
    }
}
