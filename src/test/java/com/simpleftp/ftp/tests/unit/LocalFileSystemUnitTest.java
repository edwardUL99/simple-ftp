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

import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.LocalFileSystem;
import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.ftp.FTPConnection;
import com.simpleftp.ftp.FTPConnectionManager;
import com.simpleftp.ftp.FTPServer;
import com.simpleftp.ftp.exceptions.*;
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

public class LocalFileSystemUnitTest {
    @Mock
    FTPConnection connection;
    @Mock
    FTPConnectionManager connectionManager;

    @InjectMocks
    LocalFileSystem localFileSystem;

    @TempDir
    File mockDir;

    AutoCloseable closeable;

    private static final String TEST_SERVER = "test-server";
    private static final String TEST_USER = "test-user";
    private static final String TEST_PASS = "test-pass";
    private static final String TEST_PORT = "21";

    @BeforeEach
    void init() {
        closeable = MockitoAnnotations.openMocks(this);
        setupProperties();
        mockConnection();
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    private void setupProperties() {
        System.setProperty("ftp-server", TEST_SERVER);
        System.setProperty("ftp-user", TEST_USER);
        System.setProperty("ftp-pass", TEST_PASS);
        System.setProperty("ftp-port", TEST_PORT);
    }

    private FTPServer createFTPServer() {
        return new FTPServer(TEST_SERVER, TEST_USER, TEST_PASS, Integer.parseInt(TEST_PORT));
    }

    private RemoteFile createRemoteFile() throws FileSystemException {
        return new RemoteFile("/test/path/file.txt", connectionManager);
    }

    private LocalFile createLocalFile() {
        LocalFile file = new LocalFile(mockDir.getAbsolutePath() + "/file.txt");
        return file;
    }

    private void mockConnection() {
        given(connection.isConnected())
                .willReturn(true);
        given(connection.isLoggedIn())
                .willReturn(true);
        given(connection.getFtpServer())
                .willReturn(createFTPServer());
        given(connectionManager.createReadyConnection(TEST_SERVER, TEST_USER, TEST_PASS, Integer.parseInt(TEST_PORT)))
                .willReturn(connection);
    }

    @Test
    void shouldAddFileSuccessfully() throws IOException, FileSystemException, FTPConnectionFailedException, FTPError, FTPNotConnectedException, FTPCommandFailedException {
        String path = mockDir.getAbsolutePath();
        File file = new File(path + "/file.txt");
        file.createNewFile();

        RemoteFile remoteFile = createRemoteFile();

        given(connection.downloadFile(remoteFile.getFilePath(), path))
            .willReturn(file);

        boolean result = localFileSystem.addFile(remoteFile, path);

        assertTrue(result);
        verify(connection).downloadFile(remoteFile.getFilePath(), path);
        file.delete();
    }

    @Test
    void shouldThrowIfLocalFileIsAdded() {
        String path = mockDir.getAbsolutePath();
        assertThrows(FileSystemException.class, () -> localFileSystem.addFile(createLocalFile(), path));
        verifyNoInteractions(connection);
    }

    @Test
    void shouldThrowIfErrorOccursOnAddFile() throws Exception {
        RemoteFile remoteFile = createRemoteFile();
        String path = remoteFile.getFilePath();
        String path1 = mockDir.getAbsolutePath();
        doThrow(FTPConnectionFailedException.class).when(connection).downloadFile(path, path1);
        assertThrows(FileSystemException.class, () -> localFileSystem.addFile(remoteFile, path1));
        verify(connection).downloadFile(path, path1);
    }

    @Test
    void shouldRemoveFileSuccessfully() throws IOException, FileSystemException {
        LocalFile file = createLocalFile();
        file.createNewFile();

        assertTrue(file.exists());

        boolean result = localFileSystem.removeFile(file);

        assertTrue(result);
        assertFalse(file.exists());
    }

    @Test
    void shouldThrowIfRemoteFileIsRemoved() {
        assertThrows(FileSystemException.class, () -> localFileSystem.removeFile(createRemoteFile()));
    }

    @Test
    void shouldGetFileSuccessfully() throws IOException {
        LocalFile file = createLocalFile();
        file.createNewFile();

        LocalFile result = (LocalFile) localFileSystem.getFile(file.getFilePath());

        assertEquals(file, result);
        file.delete();
    }

    @Test
    void shouldReturnNullIfNotExistsOnGetFile() {
        LocalFile file = createLocalFile();

        LocalFile result = (LocalFile) localFileSystem.getFile(file.getFilePath());

        assertNull(result);
    }

    @Test
    void shouldCheckFileExistsSuccessfully() throws IOException {
        LocalFile localFile = createLocalFile();
        localFile.createNewFile();

        assertTrue(localFileSystem.fileExists(localFile.getFilePath()));
        assertFalse(localFile.isADirectory());
        localFile.delete();
    }

    @Test
    void shouldListFilesSuccessfully() throws IOException {
        LocalFile localFile = createLocalFile();
        localFile.createNewFile();
        LocalFile[] files = new LocalFile[]{ localFile };

        CommonFile[] returned = localFileSystem.listFiles(mockDir.getAbsolutePath());

        assertArrayEquals(files, returned);
    }

    @Test
    void shouldNotListFilesIfFileDoesntExist() {
        assertNull(localFileSystem.listFiles("not exists"));
    }

    @Test
    void shouldNotListFilesIfNotDir() throws IOException {
        LocalFile file = createLocalFile();
        file.createNewFile();

        assertNull(localFileSystem.listFiles(file.getFilePath()));
        file.delete();
    }
}
