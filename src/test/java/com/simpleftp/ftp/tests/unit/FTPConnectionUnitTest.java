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
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ftp.connection.*;
import com.simpleftp.ftp.exceptions.*;
import com.simpleftp.ftp.tests.testable.FTPConnectionTestable;
import org.apache.commons.net.ftp.*;
import org.apache.commons.net.io.CopyStreamException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class FTPConnectionUnitTest {

    @Mock
    private FTPClient ftpClient;
    @Mock
    private Server server;
    @Mock
    private FTPLookup ftpLookup;

    /**
     * A test class which uses FTPConnection but allows toggling of certain fields to help with testing
     */
    @InjectMocks
    private FTPConnectionTestable ftpConnection;

    @TempDir
    public File tempDir;

    private AutoCloseable closeable;

    private static final String TEST_SERVER_HOST = "test-host";
    private static final String TEST_SERVER_USER = "test-user";
    private static final String TEST_SERVER_PASSWORD = "test-user-password";
    private static final int TEST_SERVER_PORT = 1234;
    private static final String TEST_PATH = "ftp://test/path";
    private static final String TEST_DIR = TEST_PATH + "/directory1";
    private static final String TEST_FTP_FILE = TEST_PATH + "/test-ftp-file";
    private static final String TEST_STATUS = "test-status";
    private static final String TEST_SIZE = "test-size";
    private static final String TEST_TIME = "test-time";
    private static final String TEST_FILE_STATUS = " test-file-status";
    private final int TEST_TIMEOUT_SECS = 300;


    @BeforeEach
    void init() {
        closeable = MockitoAnnotations.openMocks(this);
        FTPSystem.setSystemTesting(true);
    }

    @AfterEach
    void clean() throws Exception {
        closeable.close();
    }

    // in all the shouldThrowIfNotConnected tests, logged in is set to true and then tested if false. This should ALWAYS pass since can't be logged in if not connected
    // just done to catch in a test, if the assertion fails there is a bug somewhere

    @Test
    void shouldConnectSuccessfully() throws IOException, FTPConnectionFailedException {
        given(server.getServer())
                .willReturn(TEST_SERVER_HOST);
        given(server.getPort())
                .willReturn(TEST_SERVER_PORT);
        doNothing().when(ftpClient).connect(TEST_SERVER_HOST, TEST_SERVER_PORT); //we'll assume it connected successfully
        given(ftpClient.getReplyCode())
                .willReturn(220); //service ready for new user code

        boolean result = ftpConnection.connect();
        assertTrue(result);
        assertTrue(ftpConnection.isConnected());
        verify(server).getServer();
        verify(server).getPort();
        verify(ftpClient).connect(TEST_SERVER_HOST, TEST_SERVER_PORT);
        verify(ftpClient).getReplyCode();
        verify(ftpClient, times(0)).disconnect();
    }

    @Test
    void shouldNotConnectSuccessfully() throws IOException {
        given(server.getServer())
                .willReturn(TEST_SERVER_HOST);
        given(server.getPort())
                .willReturn(TEST_SERVER_PORT);
        doThrow(IOException.class).when(ftpClient).connect(TEST_SERVER_HOST, TEST_SERVER_PORT);
        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.connect());
        assertFalse(ftpConnection.isConnected());
        verify(server).getServer();
        verify(server).getPort();
        verify(ftpClient).connect(TEST_SERVER_HOST, TEST_SERVER_PORT);
    }

    @Test
    void shouldNotConnectIfAlreadyConnected() throws FTPConnectionFailedException {
        ftpConnection.setConnected(true);
        boolean result = ftpConnection.connect();
        assertFalse(result);
        verifyNoInteractions(server);
    }

    @Test
    void shouldThrowIfConnectionFailsOnConnect() throws IOException {
        given(server.getServer())
                .willReturn(TEST_SERVER_HOST);
        given(server.getPort())
                .willReturn(TEST_SERVER_PORT);
        doNothing().when(ftpClient).connect(TEST_SERVER_HOST, TEST_SERVER_PORT);
        given(ftpClient.getReplyCode())
                .willReturn(426); //connection closed; transfer aborted code

        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.connect());
        assertFalse(ftpConnection.isConnected());
        verify(ftpClient).connect(TEST_SERVER_HOST, TEST_SERVER_PORT);
        verify(ftpClient).getReplyCode();
        verify(ftpClient).disconnect();
        verify(server).getServer();
        verify(server).getPort();
    }

    @Test
    void shouldDisconnectSuccessfully() throws IOException {
        doNothing().when(ftpClient).disconnect();
        assertFalse(ftpConnection.isConnected());
    }

    @Test
    void shouldLogOutOnDisconnectIfLoggedIn() throws IOException, FTPConnectionFailedException, FTPNotConnectedException, FTPCommandFailedException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        doNothing().when(ftpClient).disconnect();
        given(ftpClient.logout())
                .willReturn(true);
        ftpConnection.disconnect();
        assertFalse(ftpConnection.isLoggedIn());
        assertFalse(ftpConnection.isConnected());
        verify(ftpClient).disconnect();
        verify(ftpClient).logout();
    }

    @Test
    void shouldThrowIfNotConnectedWhenDisconnecting() {
        ftpConnection.setLoggedIn(true);
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.disconnect());
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
    }

    @Test
    void shouldThrowIfConnectionErrorOccursWhenDisconnecting() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        doThrow(FTPConnectionClosedException.class).when(ftpClient).logout();
        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.disconnect());
        assertFalse(ftpConnection.isConnected()); //should not be connected afterwards
        assertFalse(ftpConnection.isLoggedIn());
        verify(ftpClient).logout();
    }

    @Test
    void shouldThrowIfErrorOccursWhenDisconnecting() throws IOException {
        ftpConnection.setConnected(true);
        doThrow(IOException.class).when(ftpClient).disconnect();
        assertThrows(FTPCommandFailedException.class, () -> ftpConnection.disconnect());
        assertTrue(ftpConnection.isConnected()); //should be still connected afterwards
        verify(ftpClient).disconnect();
    }

    @Test
    void shouldLoginSuccessfully() throws IOException, FTPConnectionFailedException, FTPNotConnectedException, FTPCommandFailedException {
        ftpConnection.setConnected(true);
        given(server.getUser())
                .willReturn(TEST_SERVER_USER);
        given(server.getPassword())
                .willReturn(TEST_SERVER_PASSWORD);
        given(ftpClient.login(TEST_SERVER_USER, TEST_SERVER_PASSWORD))
            .willReturn(true);

        boolean result = ftpConnection.login();
        assertTrue(result);
        assertTrue(ftpConnection.isLoggedIn());
        verify(server).getUser();
        verify(server).getPassword();
        verify(ftpClient).login(TEST_SERVER_USER, TEST_SERVER_PASSWORD);
    }

    @Test
    void shouldNotLogInIfNotConnected() {
        ftpConnection.setLoggedIn(true);
        given(server.getUser())
                .willReturn(TEST_SERVER_USER);
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.login());
        verify(server).getUser();
        assertFalse(ftpConnection.isLoggedIn());
        verify(server, times(0)).getPassword();
        ;

    }

    @Test
    void shouldNotLogInIfErrorOccurs() throws IOException {
        ftpConnection.setConnected(true);
        given(server.getUser())
                .willReturn(TEST_SERVER_USER);
        given(server.getPassword())
                .willReturn(TEST_SERVER_PASSWORD);
        doThrow(IOException.class).when(ftpClient).login(TEST_SERVER_USER, TEST_SERVER_PASSWORD);

        assertThrows(FTPCommandFailedException.class, () -> ftpConnection.login());
        verify(ftpClient).login(TEST_SERVER_USER, TEST_SERVER_PASSWORD);
    }

    @Test
    void shouldNotLogInIfConnectionErrorOccurs() throws IOException {
        ftpConnection.setConnected(true);
        given(server.getUser())
                .willReturn(TEST_SERVER_USER);
        given(server.getPassword())
                .willReturn(TEST_SERVER_PASSWORD);
        doThrow(FTPConnectionClosedException.class).when(ftpClient).login(TEST_SERVER_USER, TEST_SERVER_PASSWORD);

        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.login());
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
        verify(ftpClient).login(TEST_SERVER_USER, TEST_SERVER_PASSWORD);
    }

    @Test
    void shouldNotLogInAgainIfAlreadyLoggedIn() throws FTPConnectionFailedException, FTPNotConnectedException, IOException, FTPCommandFailedException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        given(server.getUser())
                .willReturn(TEST_SERVER_USER);

        boolean result = ftpConnection.login();

        assertFalse(result);
        verify(ftpClient, times(0)).login(TEST_SERVER_USER, TEST_SERVER_PASSWORD);
        verify(server).getUser();
        verify(server, times(0)).getPassword();
    }

    @Test
    void shouldLogOutSuccessfully() throws IOException, FTPConnectionFailedException, FTPNotConnectedException, FTPCommandFailedException {
        ftpConnection.setLoggedIn(true);
        ftpConnection.setConnected(true);
        given(ftpClient.logout())
                .willReturn(true);

        boolean result = ftpConnection.logout();

        assertTrue(result);
        assertTrue(ftpConnection.isConnected()); //logout should not disconnect the connection
        assertFalse(ftpConnection.isLoggedIn());
        verify(ftpClient).logout();
    }

    @Test
    void logoutShouldBeNoopIfNotLoggedIn() throws FTPConnectionFailedException, FTPNotConnectedException, FTPCommandFailedException {
        ftpConnection.setConnected(true);

        boolean result = ftpConnection.logout();

        assertFalse(result);
        assertFalse(ftpConnection.isLoggedIn());
        assertTrue(ftpConnection.isConnected());
        ;
    }

    @Test
    void logoutShouldThrowIfErrorOccurs() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        doThrow(IOException.class).when(ftpClient).logout();

        assertThrows(FTPCommandFailedException.class, () -> ftpConnection.logout());
        assertTrue(ftpConnection.isLoggedIn());
        assertTrue(ftpConnection.isConnected());
        verify(ftpClient).logout();
    }

    @Test
    void logoutShouldThrowIfConnectionErrorOccurs() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        doThrow(FTPConnectionClosedException.class).when(ftpClient).logout();

        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.logout());
        assertFalse(ftpConnection.isLoggedIn());
        assertFalse(ftpConnection.isConnected());
        verify(ftpClient).logout();
    }

    @Test
    void logoutShouldThrowIfNotConnected() {
        /*this is a case that should NOT happen, but testing regardless to ensure that should it happen, you don't get any unexpected errors
          this was done in the interest of defensive programming
         if it does happen, it sets loggedIn to false to try and resume with a normal state and throws FTPNotConnectedException. This needs to be tested to ensure it does*/
        ftpConnection.setLoggedIn(true);
        assertFalse(ftpConnection.isConnected()); // assert just to be sure before calling

        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.logout());
        assertFalse(ftpConnection.isLoggedIn());
        ;
    }

    @Test
    void shouldChangeWorkingDirectorySuccessfully() throws IOException, FTPConnectionFailedException, FTPNotConnectedException, FTPCommandFailedException, FTPError, FTPRemotePathNotFoundException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        given(ftpClient.changeWorkingDirectory(TEST_PATH))
                .willReturn(true);

        boolean result = ftpConnection.changeWorkingDirectory(TEST_PATH);

        assertTrue(result);
        assertTrue(ftpConnection.isConnected());
        assertTrue(ftpConnection.isLoggedIn()); //this should not change the internal state
        verify(ftpClient).changeWorkingDirectory(TEST_PATH);
    }

    @Test
    void shouldNotChangeDirectoryIfNotLoggedIn() throws FTPConnectionFailedException, FTPNotConnectedException, FTPCommandFailedException, FTPError, FTPRemotePathNotFoundException {
        ftpConnection.setConnected(true);

        boolean result = ftpConnection.changeWorkingDirectory(TEST_PATH);

        assertFalse(result);
        assertFalse(ftpConnection.isLoggedIn());
        assertTrue(ftpConnection.isConnected());
        ;
    }

    @Test
    void shouldThrowIfNotConnectedWhenChangingDirectory() {
        ftpConnection.setLoggedIn(true);
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.changeWorkingDirectory(TEST_PATH));
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
        ;
    }

    @Test
    void shouldThrowIfErrorOccursChangingDirectory() throws IOException, FTPError {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        doThrow(IOException.class).when(ftpClient).changeWorkingDirectory(TEST_PATH);

        assertThrows(FTPCommandFailedException.class, () -> ftpConnection.changeWorkingDirectory(TEST_PATH));
        assertTrue(ftpConnection.isConnected());
        assertTrue(ftpConnection.isLoggedIn());
        verify(ftpClient).changeWorkingDirectory(TEST_PATH);
    }

    @Test
    void shouldThrowIfConnectionErrorOccursChangingDirectory() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        doThrow(FTPConnectionClosedException.class).when(ftpClient).changeWorkingDirectory(TEST_PATH);

        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.changeWorkingDirectory(TEST_PATH));
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
        verify(ftpClient).changeWorkingDirectory(TEST_PATH);
    }

    @Test
    void shouldChangeToParentWorkingDirectorySuccessfully() throws IOException, FTPConnectionFailedException, FTPNotConnectedException, FTPCommandFailedException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        given(ftpClient.changeToParentDirectory())
            .willReturn(true);

        boolean result = ftpConnection.changeToParentDirectory();

        assertTrue(result);
        assertTrue(ftpConnection.isConnected());
        assertTrue(ftpConnection.isLoggedIn()); //this should not change the internal state
        verify(ftpClient).changeToParentDirectory();
    }

    @Test
    void shouldNotChangeToParentDirectoryIfNotLoggedIn() throws FTPConnectionFailedException, FTPNotConnectedException, FTPCommandFailedException {
        ftpConnection.setConnected(true);

        boolean result = ftpConnection.changeToParentDirectory();

        assertFalse(result);
        assertFalse(ftpConnection.isLoggedIn());
        assertTrue(ftpConnection.isConnected());
        ;
    }

    @Test
    void shouldThrowIfNotConnectedWhenChangingToParentDirectory() {
        ftpConnection.setLoggedIn(true);
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.changeToParentDirectory());
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
        ;
    }

    @Test
    void shouldThrowIfErrorOccursChangingToParentDirectory() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        doThrow(IOException.class).when(ftpClient).changeToParentDirectory();

        assertThrows(FTPCommandFailedException.class, () -> ftpConnection.changeToParentDirectory());
        assertTrue(ftpConnection.isConnected());
        assertTrue(ftpConnection.isLoggedIn());
        verify(ftpClient).changeToParentDirectory();
    }

    @Test
    void shouldThrowIfConnectionErrorOccursChangingToParentDirectory() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        doThrow(FTPConnectionClosedException.class).when(ftpClient).changeToParentDirectory();

        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.changeToParentDirectory());
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
        verify(ftpClient).changeToParentDirectory();
    }

    @Test
    void shouldGetWorkingDirectorySuccessfully() throws IOException, FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        given(ftpLookup.getWorkingDirectory())
                .willReturn(TEST_PATH);

        String directory = ftpConnection.getWorkingDirectory();

        assertEquals(directory, TEST_PATH);
        verify(ftpLookup).getWorkingDirectory();
    }

    @Test
    void shouldReturnNullIfLoggedOutOnGetWorkingDirectory() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        ftpConnection.setConnected(true);

        String directory = ftpConnection.getWorkingDirectory();

        assertNull(directory);
        verifyNoInteractions(ftpLookup);
    }

    @Test
    void shouldThrowIfNotConnectedOnGetWorkingDirectory() {
        ftpConnection.setLoggedIn(true);
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.getWorkingDirectory());
        assertFalse(ftpConnection.isLoggedIn());
        verifyNoInteractions(ftpLookup);
    }

    @Test
    void shouldThrowIfConnectionErrorOccursOnGetWorkingDirectory() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        doThrow(FTPConnectionClosedException.class).when(ftpLookup).getWorkingDirectory();

        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.getWorkingDirectory());
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
        verify(ftpLookup).getWorkingDirectory();
    }

    @Test
    void shouldThrowIfIOExceptionOccursOnGetWorkingDirectory() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        doThrow(IOException.class).when(ftpLookup).getWorkingDirectory();

        assertThrows(FTPCommandFailedException.class, () -> ftpConnection.getWorkingDirectory());
        assertTrue(ftpConnection.isConnected());
        assertTrue(ftpConnection.isLoggedIn());
        verify(ftpLookup).getWorkingDirectory();
    }

    private FTPFile getTestFTPFile() {
        FTPFile ftpFile = new FTPFile();
        ftpFile.setUser(TEST_SERVER_USER);
        ftpFile.setGroup("test-group");
        ftpFile.setName(TEST_FTP_FILE);
        return ftpFile;
    }

    @Test
    void shouldGetFTPFileSuccessfully() throws IOException, FTPConnectionFailedException, FTPNotConnectedException, FTPCommandFailedException, FTPError, FTPRemotePathNotFoundException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        FTPFile testFile = getTestFTPFile();
        given(ftpLookup.getFTPFile(TEST_FTP_FILE))
                .willReturn(testFile);

        FTPFile result = ftpConnection.getFTPFile(TEST_FTP_FILE);

        assertEquals(result, testFile);
        verify(ftpLookup).getFTPFile(TEST_FTP_FILE);
    }

    @Test
    void shouldReturnNullWithGetFTPFile() throws IOException, FTPConnectionFailedException, FTPNotConnectedException, FTPCommandFailedException, FTPRemotePathNotFoundException, FTPError {
        ftpConnection.setLoggedIn(true);
        ftpConnection.setConnected(true);
        given(ftpLookup.getFTPFile(TEST_FTP_FILE))
                .willReturn(null);

        FTPFile result = ftpConnection.getFTPFile(TEST_FTP_FILE);

        assertNull(result);
        verify(ftpLookup).getFTPFile(TEST_FTP_FILE);
    }

    @Test
    void shouldReturnNullIfLoggedOutOnGetFTPFile() throws FTPConnectionFailedException, FTPNotConnectedException, FTPCommandFailedException, FTPRemotePathNotFoundException, FTPError {
        ftpConnection.setConnected(true);
        FTPFile result = ftpConnection.getFTPFile(TEST_FTP_FILE);

        assertNull(result);
        assertFalse(ftpConnection.isLoggedIn());
        ;
    }

    @Test
    void shouldThrowOnGetFTPFileIfNotConnected() {
        ftpConnection.setLoggedIn(true);
        assertFalse(ftpConnection.isConnected());
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.getFTPFile(TEST_FTP_FILE));
        assertFalse(ftpConnection.isLoggedIn());
    }

    @Test
    void shouldThrowIfErrorOccursWithGetFTPFile() throws IOException, FTPError {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        doThrow(IOException.class).when(ftpLookup).getFTPFile(TEST_FTP_FILE);

        assertThrows(FTPCommandFailedException.class, () -> ftpConnection.getFTPFile(TEST_FTP_FILE));
        assertTrue(ftpConnection.isConnected());
        assertTrue(ftpConnection.isLoggedIn());
        verify(ftpLookup).getFTPFile(TEST_FTP_FILE);
    }

    @Test
    void shouldThrowIfConnectionErrorOccursWithGetFTPFile() throws IOException, FTPError {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        doThrow(FTPConnectionClosedException.class).when(ftpLookup).getFTPFile(TEST_FTP_FILE);

        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.getFTPFile(TEST_FTP_FILE));
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
        verify(ftpLookup).getFTPFile(TEST_FTP_FILE);
    }

    @Test
    void shouldListFilesSuccessfully() throws IOException, FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        FTPFile[] files = {getTestFTPFile()};
        given(ftpLookup.listFTPFiles(TEST_PATH))
                .willReturn(files);

        FTPFile[] result = ftpConnection.listFiles(TEST_PATH);

        assertEquals(result, files);
        verify(ftpLookup).listFTPFiles(TEST_PATH);
    }

    @Test
    void shouldReturnNullIfNotLoggedInOnListFiles() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        ftpConnection.setConnected(true);

        FTPFile[] result = ftpConnection.listFiles(TEST_PATH);

        assertNull(result);
        verifyNoInteractions(ftpLookup);
    }

    @Test
    void shouldThrowIfNotConnectedOnListFiles() {
        ftpConnection.setLoggedIn(true);
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.listFiles(TEST_PATH));
        assertFalse(ftpConnection.isLoggedIn());
        verifyNoInteractions(ftpLookup);
    }

    @Test
    void shouldThrowIfConnectionErrorOccursOnListFiles() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        doThrow(FTPConnectionClosedException.class).when(ftpLookup).listFTPFiles(TEST_PATH);

        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.listFiles(TEST_PATH));
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
        verify(ftpLookup).listFTPFiles(TEST_PATH);
    }

    @Test
    void shouldThrowIfIOExceptionOccursOnListFiles() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        doThrow(IOException.class).when(ftpLookup).listFTPFiles(TEST_PATH);

        assertThrows(FTPCommandFailedException.class, () -> ftpConnection.listFiles(TEST_PATH));
        assertTrue(ftpConnection.isConnected());
        assertTrue(ftpConnection.isLoggedIn());
        verify(ftpLookup).listFTPFiles(TEST_PATH);
    }

    private LocalFile getTestFile(boolean create) throws IOException {
        LocalFile file = new LocalFile(tempDir.getAbsolutePath() + "/" + "test-ftp-file");

        if (create)
            file.createNewFile();

        return file;
    }

    @Test
    void shouldUploadFileSuccessfully() throws FTPConnectionFailedException, FTPError, FTPNotConnectedException, FTPCommandFailedException, IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        LocalFile testFile = getTestFile(true);

        FTPFile testFTPFile = getTestFTPFile();

        given(ftpLookup.remotePathExists(TEST_PATH, true))
                .willReturn(true);
        given(ftpLookup.getFTPFile(TEST_FTP_FILE))
                .willReturn(testFTPFile);
        doReturn(true).when(ftpClient).storeFile(eq(TEST_FTP_FILE), any(FileInputStream.class));


        FTPFile result = ftpConnection.uploadFile(testFile, TEST_PATH);

        assertEquals(result, testFTPFile);
        verify(ftpLookup).remotePathExists(TEST_PATH, true);
        verify(ftpLookup).getFTPFile(TEST_FTP_FILE);
        verify(ftpClient).storeFile(eq(TEST_FTP_FILE), any(FileInputStream.class));
    }

    @Test
    void shouldThrowIfNotConnectedWhenUploadingFile() {
        ftpConnection.setLoggedIn(true);
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.uploadFile(getTestFile(true), TEST_PATH));
        assertFalse(ftpConnection.isLoggedIn());
        verifyNoInteractions(ftpLookup);
    }

    @Test
    void shouldNotUploadIfFileDoesntExist() throws IOException, FTPConnectionFailedException, FTPError, FTPNotConnectedException, FTPCommandFailedException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        LocalFile testFile = getTestFile(false);

        given(ftpLookup.remotePathExists(TEST_PATH, true))
                .willReturn(true);

        FTPFile result = ftpConnection.uploadFile(testFile, TEST_PATH);

        assertNull(result);
    }

    @Test
    void shouldNotUploadIfFileIsDirectory() throws FTPConnectionFailedException, FTPError, FTPNotConnectedException, FTPCommandFailedException, IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        given(ftpLookup.remotePathExists(TEST_PATH, true))
                .willReturn(true);

        LocalFile tempDir = new LocalFile(this.tempDir.getAbsolutePath());
        FTPFile result = ftpConnection.uploadFile(tempDir, TEST_PATH);

        assertNull(result);
    }

    @Test
    void shouldNotUploadIfRemoteDirDoesNotExist() throws IOException, FTPConnectionFailedException, FTPError, FTPNotConnectedException, FTPCommandFailedException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        LocalFile testFile = getTestFile(true);

        given(ftpLookup.remotePathExists(TEST_PATH, true))
                .willReturn(false);

        FTPFile result = ftpConnection.uploadFile(testFile, TEST_PATH);

        assertNull(result);
        verify(ftpLookup).remotePathExists(TEST_PATH, true);
    }

    @Test
    void shouldNotUploadIfNotLoggedIn() throws FTPConnectionFailedException, FTPError, FTPNotConnectedException, FTPCommandFailedException, IOException {
        ftpConnection.setConnected(true);
        LocalFile testFile = getTestFile(true);

        given(ftpLookup.remotePathExists(TEST_PATH, true))
                .willReturn(true);

        FTPFile result = ftpConnection.uploadFile(testFile, TEST_PATH);

        assertNull(result);
        verify(ftpLookup).remotePathExists(TEST_PATH, true);
    }

    @Test
    void shouldThrowIfConnectionClosesOnUpload() throws IOException, FTPError {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        LocalFile testFile = getTestFile(true);

        given(ftpLookup.remotePathExists(TEST_PATH, true))
                .willReturn(true);
        doThrow(FTPConnectionClosedException.class).when(ftpClient).storeFile(eq(TEST_FTP_FILE), any(FileInputStream.class));

        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.uploadFile(testFile, TEST_PATH));
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
        verify(ftpClient).storeFile(eq(TEST_FTP_FILE), any(FileInputStream.class));
        verify(ftpLookup).remotePathExists(TEST_PATH, true);
    }

    @Test
    void shouldThrowIfFileNotFoundExceptionOnUpload() throws IOException, FTPConnectionFailedException, FTPError, FTPNotConnectedException, FTPCommandFailedException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        LocalFile testFile = getTestFile(true);

        given(ftpLookup.remotePathExists(TEST_PATH, true))
                .willReturn(true);
        doThrow(FileNotFoundException.class).when(ftpClient).storeFile(eq(TEST_FTP_FILE), any(FileInputStream.class)); //simulates if the file input stream failed

        assertThrows(FTPError.class, () -> ftpConnection.uploadFile(testFile, TEST_PATH));
        assertTrue(ftpConnection.isConnected());
        assertTrue(ftpConnection.isLoggedIn());
        verify(ftpClient).storeFile(eq(TEST_FTP_FILE), any(FileInputStream.class));
        verify(ftpLookup).remotePathExists(TEST_PATH, true);
    }

    @Test
    void shouldThrowIfCopyStreamExceptionUpload() throws IOException, FTPConnectionFailedException, FTPError, FTPNotConnectedException, FTPCommandFailedException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        LocalFile testFile = getTestFile(true);

        given(ftpLookup.remotePathExists(TEST_PATH, true))
                .willReturn(true);
        doThrow(CopyStreamException.class).when(ftpClient).storeFile(eq(TEST_FTP_FILE), any(FileInputStream.class)); //simulates if the file input stream failed

        assertThrows(FTPError.class, () -> ftpConnection.uploadFile(testFile, TEST_PATH));
        assertTrue(ftpConnection.isConnected());
        assertTrue(ftpConnection.isLoggedIn());
        verify(ftpClient).storeFile(eq(TEST_FTP_FILE), any(FileInputStream.class));
    }

    @Test
    void shouldThrowIfIOExceptionUpload() throws IOException, FTPConnectionFailedException, FTPError, FTPNotConnectedException, FTPCommandFailedException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        LocalFile testFile = getTestFile(true);

        given(ftpLookup.remotePathExists(TEST_PATH, true))
                .willReturn(true);
        doThrow(IOException.class).when(ftpClient).storeFile(eq(TEST_FTP_FILE), any(FileInputStream.class)); //simulates if the file input stream failed

        assertThrows(FTPCommandFailedException.class, () -> ftpConnection.uploadFile(testFile, TEST_PATH));
        assertTrue(ftpConnection.isConnected());
        assertTrue(ftpConnection.isLoggedIn());
        verify(ftpClient).storeFile(eq(TEST_FTP_FILE), any(FileInputStream.class));
        verify(ftpClient).storeFile(eq(TEST_FTP_FILE), any(FileInputStream.class));
    }

    @Test
    void shouldStringToFileForUpload() throws FTPConnectionFailedException, FTPError, FTPNotConnectedException, FTPCommandFailedException, IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        LocalFile testFile = getTestFile(true);

        FTPFile testFTPFile = getTestFTPFile();

        given(ftpLookup.remotePathExists(TEST_PATH, true))
                .willReturn(true);
        given(ftpLookup.getFTPFile(TEST_FTP_FILE))
                .willReturn(testFTPFile);
        doReturn(true).when(ftpClient).storeFile(eq(TEST_FTP_FILE), any(FileInputStream.class));

        String path = testFile.getPath();

        FTPFile result = ftpConnection.uploadFile(path, TEST_PATH);

        assertEquals(result, testFTPFile);
        verify(ftpLookup).remotePathExists(TEST_PATH, true);
        verify(ftpLookup).getFTPFile(TEST_FTP_FILE);
        verify(ftpClient).storeFile(eq(TEST_FTP_FILE), any(FileInputStream.class));
        // you don't need to test exceptions as it used the same upload method ad those exceptions are already tested
    }

    @Test
    void shouldDownloadFileSuccessfully() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException, FTPError, IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        FTPFile remoteFile = getTestFTPFile();
        LocalFile testFile = getTestFile(false);

        given(ftpLookup.getFTPFile(TEST_FTP_FILE))
                .willReturn(remoteFile);
        given(ftpLookup.remotePathExists(TEST_FTP_FILE, false))
                .willReturn(true);
        doReturn(true).when(ftpClient).retrieveFile(eq(TEST_FTP_FILE), any(FileOutputStream.class));

        LocalFile result = ftpConnection.downloadFile(TEST_FTP_FILE, tempDir.getAbsolutePath());

        assertEquals(result, testFile);
        verify(ftpClient).retrieveFile(eq(TEST_FTP_FILE), any(FileOutputStream.class));
        verify(ftpLookup).getFTPFile(TEST_FTP_FILE);
        verify(ftpLookup).remotePathExists(TEST_FTP_FILE, true);
    }

    @Test
    void shouldNotDownloadIfTheRemoteFileDoesNotExist() throws IOException, FTPConnectionFailedException, FTPError, FTPNotConnectedException, FTPCommandFailedException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        given(ftpLookup.getFTPFile(TEST_FTP_FILE))
                .willReturn(null);

        LocalFile result = ftpConnection.downloadFile(TEST_FTP_FILE, tempDir.getAbsolutePath());

        assertNull(result);
        verify(ftpLookup).getFTPFile(TEST_FTP_FILE);
    }

    @Test
    void shouldNotDownloadIfRemotePathIsADirectory() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException, FTPError, IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        given(ftpLookup.remotePathExists(TEST_FTP_FILE, true))
                .willReturn(true);

        LocalFile result = ftpConnection.downloadFile(TEST_FTP_FILE, tempDir.getAbsolutePath());

        assertNull(result);
        verify(ftpLookup).remotePathExists(TEST_FTP_FILE, true);
    }

    @Test
    void shouldNotDownloadIfLocalPathIsNotADirectory() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException, FTPError, IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        given(ftpLookup.remotePathExists(TEST_FTP_FILE, true))
                .willReturn(false);

        LocalFile result = ftpConnection.downloadFile(TEST_FTP_FILE, getTestFile(true).getAbsolutePath());

        assertNull(result);
        verify(ftpLookup).remotePathExists(TEST_FTP_FILE, true);
    }

    @Test
    void shouldNotDownloadIfNotLoggedIn() throws IOException, FTPError, FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        ftpConnection.setConnected(true);

        given(ftpLookup.remotePathExists(TEST_FTP_FILE, true))
                .willReturn(false);

        LocalFile result = ftpConnection.downloadFile(TEST_FTP_FILE, tempDir.getAbsolutePath());

        assertNull(result);
        verify(ftpLookup).remotePathExists(TEST_FTP_FILE, true);
    }

    @Test
    void shouldThrowIfNotConnected() {
        ftpConnection.setLoggedIn(true);
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.downloadFile(TEST_FTP_FILE, tempDir.getAbsolutePath()));
        assertFalse(ftpConnection.isLoggedIn());
        verifyNoInteractions(ftpLookup);
    }

    @Test
    void shouldThrowIfConnectionClosesOnDownload() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException, FTPError, IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        given(ftpLookup.getFTPFile(TEST_FTP_FILE))
                .willReturn(getTestFTPFile());
        given(ftpLookup.remotePathExists(TEST_FTP_FILE, true))
                .willReturn(false);
        doThrow(FTPConnectionClosedException.class).when(ftpClient).retrieveFile(eq(TEST_FTP_FILE), any(FileOutputStream.class));

        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.downloadFile(TEST_FTP_FILE, tempDir.getAbsolutePath()));
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
        verify(ftpClient).retrieveFile(eq(TEST_FTP_FILE), any(FileOutputStream.class));
        verify(ftpLookup).getFTPFile(TEST_FTP_FILE);
        verify(ftpLookup).remotePathExists(TEST_FTP_FILE, true);
    }

    @Test
    void shouldThrowIfFileNotFoundExceptionOnDownload() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException, FTPError, IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        given(ftpLookup.getFTPFile(TEST_FTP_FILE))
                .willReturn(getTestFTPFile());
        given(ftpLookup.remotePathExists(TEST_FTP_FILE, true))
                .willReturn(false);
        doThrow(FileNotFoundException.class).when(ftpClient).retrieveFile(eq(TEST_FTP_FILE), any(FileOutputStream.class));

        assertThrows(FTPError.class, () -> ftpConnection.downloadFile(TEST_FTP_FILE, tempDir.getAbsolutePath()));
        assertTrue(ftpConnection.isConnected());
        assertTrue(ftpConnection.isLoggedIn());
        verify(ftpClient).retrieveFile(eq(TEST_FTP_FILE), any(FileOutputStream.class));
        verify(ftpLookup).getFTPFile(TEST_FTP_FILE);
        verify(ftpLookup).remotePathExists(TEST_FTP_FILE, true);
    }

    @Test
    void shouldThrowIfCopyStreamExceptionOnDownload() throws FTPError, IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        given(ftpLookup.getFTPFile(TEST_FTP_FILE))
                .willReturn(getTestFTPFile());
        given(ftpLookup.remotePathExists(TEST_FTP_FILE, true))
                .willReturn(false);
        doThrow(CopyStreamException.class).when(ftpClient).retrieveFile(eq(TEST_FTP_FILE), any(FileOutputStream.class));

        assertThrows(FTPError.class, () -> ftpConnection.downloadFile(TEST_FTP_FILE, tempDir.getAbsolutePath()));
        assertTrue(ftpConnection.isConnected());
        assertTrue(ftpConnection.isLoggedIn());
        verify(ftpClient).retrieveFile(eq(TEST_FTP_FILE), any(FileOutputStream.class));
        verify(ftpLookup).getFTPFile(TEST_FTP_FILE);
        verify(ftpLookup).remotePathExists(TEST_FTP_FILE, true);
    }

    @Test
    void shouldThrowIfIOExceptionOnDownload() throws FTPError, IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        given(ftpLookup.getFTPFile(TEST_FTP_FILE))
                .willReturn(getTestFTPFile());
        given(ftpLookup.remotePathExists(TEST_FTP_FILE, true))
                .willReturn(false);
        doThrow(IOException.class).when(ftpClient).retrieveFile(eq(TEST_FTP_FILE), any(FileOutputStream.class));

        assertThrows(FTPCommandFailedException.class, () -> ftpConnection.downloadFile(TEST_FTP_FILE, tempDir.getAbsolutePath()));
        assertTrue(ftpConnection.isConnected());
        assertTrue(ftpConnection.isLoggedIn());
        verify(ftpClient).retrieveFile(eq(TEST_FTP_FILE), any(FileOutputStream.class));
        verify(ftpLookup).getFTPFile(TEST_FTP_FILE);
        verify(ftpLookup).remotePathExists(TEST_FTP_FILE, true);
    }

    @Test
    void shouldMakeDirectorySuccessfully() throws IOException, FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException, FTPError {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        given(ftpClient.makeDirectory(TEST_DIR))
                .willReturn(true);
        given(ftpLookup.remotePathExists(TEST_DIR))
                .willReturn(false);

        boolean result = ftpConnection.makeDirectory(TEST_DIR);

        assertTrue(result);
        verify(ftpClient).makeDirectory(TEST_DIR);
        verify(ftpLookup).remotePathExists(TEST_DIR, true);
        verify(ftpLookup).remotePathExists(TEST_DIR, false);
    }

    @Test
    void shouldNotMakeDirectoryIfAlreadyADir() throws FTPConnectionFailedException, FTPError, FTPNotConnectedException, FTPCommandFailedException, IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        given(ftpLookup.remotePathExists(TEST_DIR, true))
                .willReturn(true);
        given(ftpLookup.remotePathExists(TEST_DIR, false))
                .willReturn(false);

        boolean result = ftpConnection.makeDirectory(TEST_DIR);

        assertFalse(result);
        verify(ftpLookup).remotePathExists(TEST_DIR, true);
    }

    @Test
    void shouldNotMakeDirectoryIfAlreadyAFile() throws FTPConnectionFailedException, FTPError, FTPNotConnectedException, FTPCommandFailedException, IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        given(ftpLookup.remotePathExists(TEST_DIR, true))
                .willReturn(false);
        given(ftpLookup.remotePathExists(TEST_DIR, false))
                .willReturn(true);

        boolean result = ftpConnection.makeDirectory(TEST_DIR);

        assertFalse(result);
        verify(ftpLookup).remotePathExists(TEST_DIR, true);
        verify(ftpLookup).remotePathExists(TEST_DIR, false);
    }

    @Test
    void shouldNotMakeDirectoryIfNotLoggedIn() throws FTPConnectionFailedException, FTPError, FTPNotConnectedException, FTPCommandFailedException {
        ftpConnection.setConnected(true);

        boolean result = ftpConnection.makeDirectory(TEST_DIR);

        assertFalse(result);
        verifyNoInteractions(ftpLookup);
    }

    @Test
    void shouldThrowIfNotConnectedOnMakeDirectory() {
        ftpConnection.setLoggedIn(true);
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.makeDirectory(TEST_DIR));
        assertFalse(ftpConnection.isLoggedIn());
        verifyNoInteractions(ftpLookup);
    }

    @Test
    void shouldThrowIfConnectionErrorOccursOnMakeDirectory() throws IOException, FTPError {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        given(ftpLookup.remotePathExists(TEST_DIR, true))
                .willReturn(false);
        given(ftpLookup.remotePathExists(TEST_DIR, false))
                .willReturn(false);
        doThrow(FTPConnectionClosedException.class).when(ftpClient).makeDirectory(TEST_DIR);

        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.makeDirectory(TEST_DIR));
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
        verify(ftpLookup).remotePathExists(TEST_DIR, true);
        verify(ftpLookup).remotePathExists(TEST_DIR, false);
        verify(ftpClient).makeDirectory(TEST_DIR);
    }

    @Test
    void shouldThrowIfIOExceptionOccursOnMakeDirectory() throws IOException, FTPError {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        given(ftpLookup.remotePathExists(TEST_DIR, true))
                .willReturn(false);
        given(ftpLookup.remotePathExists(TEST_DIR, false))
                .willReturn(false);
        doThrow(IOException.class).when(ftpClient).makeDirectory(TEST_DIR);

        assertThrows(FTPCommandFailedException.class, () -> ftpConnection.makeDirectory(TEST_DIR));
        assertTrue(ftpConnection.isConnected());
        assertTrue(ftpConnection.isLoggedIn());
        verify(ftpLookup).remotePathExists(TEST_DIR, true);
        verify(ftpLookup).remotePathExists(TEST_DIR, false);
        verify(ftpClient).makeDirectory(TEST_DIR);
    }

    @Test
    void shouldRenameFileSuccessfully() throws IOException, FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        given(ftpClient.rename(TEST_PATH, TEST_FTP_FILE))
                .willReturn(true);

        boolean result = ftpConnection.renameFile(TEST_PATH, TEST_FTP_FILE);

        assertTrue(result);
        verify(ftpClient).rename(TEST_PATH, TEST_FTP_FILE);
    }

    @Test
    void shouldNotRenameFileIfNotLoggedIn() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        ftpConnection.setConnected(true);

        boolean result = ftpConnection.renameFile(TEST_PATH, TEST_FTP_FILE);

        assertFalse(result);
    }

    @Test
    void shouldThrowIfNotConnectedOnRenameFile() {
        ftpConnection.setLoggedIn(true);
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.renameFile(TEST_PATH, TEST_FTP_FILE));
        assertFalse(ftpConnection.isLoggedIn());
    }

    @Test
    void shouldThrowIfConnectionErrorOccursOnRenameFile() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        doThrow(FTPConnectionClosedException.class).when(ftpClient).rename(TEST_PATH, TEST_FTP_FILE);

        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.renameFile(TEST_PATH, TEST_FTP_FILE));
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
        verify(ftpClient).rename(TEST_PATH, TEST_FTP_FILE);
    }

    @Test
    void shouldThrowIfIOExceptionOccursOnRenameFile() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        doThrow(IOException.class).when(ftpClient).rename(TEST_PATH, TEST_FTP_FILE);

        assertThrows(FTPCommandFailedException.class, () -> ftpConnection.renameFile(TEST_PATH, TEST_FTP_FILE));
        assertTrue(ftpConnection.isConnected());
        assertTrue(ftpConnection.isLoggedIn());
        verify(ftpClient).rename(TEST_PATH, TEST_FTP_FILE);
    }

    @Test
    void shouldRemoveFileSuccessfully() throws IOException, FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        given(ftpClient.deleteFile(TEST_FTP_FILE))
                .willReturn(true);

        boolean result = ftpConnection.removeFile(TEST_FTP_FILE);

        assertTrue(result);
        verify(ftpClient).deleteFile(TEST_FTP_FILE);
    }

    @Test
    void shouldNotRemoveFileIfNotLoggedIn() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        ftpConnection.setConnected(true);

        boolean result = ftpConnection.removeFile(TEST_FTP_FILE);

        assertFalse(result);
    }

    @Test
    void shouldThrowIfNotConnectedOnRemovingFile() {
        ftpConnection.setLoggedIn(true);
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.removeFile(TEST_FTP_FILE));
        assertFalse(ftpConnection.isLoggedIn());
    }

    @Test
    void shouldThrowIfConnectionCloseOnRemovingFile() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        doThrow(FTPConnectionClosedException.class).when(ftpClient).deleteFile(TEST_FTP_FILE);

        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.removeFile(TEST_FTP_FILE));
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
        verify(ftpClient).deleteFile(TEST_FTP_FILE);
    }

    @Test
    void shouldThrowIfIOExceptionOccursOnRemovingFile() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        doThrow(IOException.class).when(ftpClient).deleteFile(TEST_FTP_FILE);

        assertThrows(FTPCommandFailedException.class, () -> ftpConnection.removeFile(TEST_FTP_FILE));
        assertTrue(ftpConnection.isConnected());
        assertTrue(ftpConnection.isLoggedIn());
        verify(ftpClient).deleteFile(TEST_FTP_FILE);
    }

    @Test
    void shouldRemoveDirectorySuccessfully() throws IOException, FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        given(ftpClient.removeDirectory(TEST_PATH))
                .willReturn(true);

        boolean result = ftpConnection.removeDirectory(TEST_PATH);

        assertTrue(result);
        verify(ftpClient).removeDirectory(TEST_PATH);
    }

    @Test
    void shouldNotRemoveDirectoryIfNotLoggedIn() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        ftpConnection.setConnected(true);

        boolean result = ftpConnection.removeDirectory(TEST_PATH);

        assertFalse(result);
    }

    @Test
    void shouldThrowIfNotConnectedOnRemovingDirectory() {
        ftpConnection.setLoggedIn(true);
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.removeDirectory(TEST_PATH));
        assertFalse(ftpConnection.isLoggedIn());
    }

    @Test
    void shouldThrowIfConnectionCloseOnRemovingDirectory() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        doThrow(FTPConnectionClosedException.class).when(ftpClient).removeDirectory(TEST_PATH);

        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.removeDirectory(TEST_PATH));
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
        verify(ftpClient).removeDirectory(TEST_PATH);
    }

    @Test
    void shouldThrowIfIOExceptionOccursOnRemovingDirectory() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        doThrow(IOException.class).when(ftpClient).removeDirectory(TEST_PATH);

        assertThrows(FTPCommandFailedException.class, () -> ftpConnection.removeDirectory(TEST_PATH));
        assertTrue(ftpConnection.isConnected());
        assertTrue(ftpConnection.isLoggedIn());
        verify(ftpClient).removeDirectory(TEST_PATH);
    }

    @Test
    void shouldGetRemotePathExistsSuccessfully() throws IOException, FTPError, FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        given(ftpLookup.remotePathExists(TEST_FTP_FILE, true))
                .willReturn(true);

        boolean exists = ftpConnection.remotePathExists(TEST_FTP_FILE, true);

        assertTrue(exists);
        verify(ftpLookup).remotePathExists(TEST_FTP_FILE, true);
    }

    @Test
    void shouldThrowIfNotConnectedOnRemotePathExists() {
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.remotePathExists(TEST_FTP_FILE, true));
        verifyNoInteractions(ftpLookup);
    }

    @Test
    void shouldReturnFalseIfNotLoggedInOnRemotePathExists() throws FTPConnectionFailedException, FTPError, FTPNotConnectedException, FTPCommandFailedException {
        ftpConnection.setConnected(true);

        boolean result = ftpConnection.remotePathExists(TEST_FTP_FILE, true);

        assertFalse(result);
        verifyNoInteractions(ftpLookup);
    }

    @Test
    void shouldThrowIfConnectionErrorOccursOnRemotePathExists() throws IOException, FTPError {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        doThrow(FTPConnectionClosedException.class).when(ftpLookup).remotePathExists(TEST_FTP_FILE, true);

        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.remotePathExists(TEST_FTP_FILE, true));
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
        verify(ftpLookup).remotePathExists(TEST_FTP_FILE, true);
    }

    @Test
    void shouldThrowIfIOExceptionOccursOnRemotePathExists() throws IOException, FTPError {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        doThrow(IOException.class).when(ftpLookup).remotePathExists(TEST_FTP_FILE, true);

        assertThrows(FTPCommandFailedException.class, () -> ftpConnection.remotePathExists(TEST_FTP_FILE, true));
        assertTrue(ftpConnection.isConnected());
        assertTrue(ftpConnection.isLoggedIn());
        verify(ftpLookup).remotePathExists(TEST_FTP_FILE, true);
    }

    @Test
    void shouldGetStatusSuccessfully() throws IOException, FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        given(ftpLookup.getStatus())
                .willReturn(TEST_STATUS);

        String status = ftpConnection.getStatus();

        assertEquals(status, TEST_STATUS);
        verify(ftpLookup).getStatus();
    }

    @Test
    void shouldGetNullStatusIfNotLoggedIn() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        ftpConnection.setConnected(true);

        String status = ftpConnection.getStatus();

        assertNull(status);
        verifyNoInteractions(ftpLookup);
    }

    @Test
    void shouldThrowIfNotConnectedOnStatus() {
        ftpConnection.setLoggedIn(true);
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.getStatus());
        assertFalse(ftpConnection.isLoggedIn());
        verifyNoInteractions(ftpLookup);
    }

    @Test
    void shouldThrowIfConnectionErrorOccursOnStatus() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        doThrow(FTPConnectionClosedException.class).when(ftpLookup).getStatus();

        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.getStatus());
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
        verify(ftpLookup).getStatus();
    }

    @Test
    void shouldThrowIfIOExceptionErrorOccursOnStatus() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        doThrow(IOException.class).when(ftpLookup).getStatus();

        assertThrows(FTPCommandFailedException.class, () -> ftpConnection.getStatus());
        assertTrue(ftpConnection.isConnected());
        assertTrue(ftpConnection.isLoggedIn());
        verify(ftpLookup).getStatus();
    }

    @Test
    void shouldGetFileStatusSuccessfully() throws IOException, FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        given(ftpLookup.getFileStatus(TEST_FTP_FILE))
                .willReturn(TEST_FILE_STATUS);

        String status = ftpConnection.getFileStatus(TEST_FTP_FILE);

        assertEquals(status, TEST_FILE_STATUS);
        verify(ftpLookup).getFileStatus(TEST_FTP_FILE);
    }

    @Test
    void shouldNotGetFileStatusIfNotLoggedIn() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        ftpConnection.setConnected(true);

        String status = ftpConnection.getFileStatus(TEST_FTP_FILE);

        assertNull(status);
        verifyNoInteractions(ftpLookup);
    }

    @Test
    void shouldThrowIfNotConnectedOnFileStatus() {
        ftpConnection.setLoggedIn(true);
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.getFileStatus(TEST_FTP_FILE));
        assertFalse(ftpConnection.isLoggedIn());
        verifyNoInteractions(ftpLookup);
    }

    @Test
    void shouldThrowIfConnectionErrorOccursOnFileStatus() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        doThrow(FTPConnectionClosedException.class).when(ftpLookup).getFileStatus(TEST_FTP_FILE);

        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.getFileStatus(TEST_FTP_FILE));
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
        verify(ftpLookup).getFileStatus(TEST_FTP_FILE);
    }

    @Test
    void shouldThrowIfIOErrorOccursOnFileStatus() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        doThrow(IOException.class).when(ftpLookup).getFileStatus(TEST_FTP_FILE);

        assertThrows(FTPCommandFailedException.class, () -> ftpConnection.getFileStatus(TEST_FTP_FILE));
        assertTrue(ftpConnection.isConnected());
        assertTrue(ftpConnection.isLoggedIn());
        verify(ftpLookup).getFileStatus(TEST_FTP_FILE);
    }

    @Test
    void shouldGetFileSizeSuccessfully() throws IOException, FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        given(ftpLookup.getFileSize(TEST_FTP_FILE))
                .willReturn(TEST_SIZE);

        String status = ftpConnection.getFileSize(TEST_FTP_FILE);

        assertEquals(status, TEST_SIZE);
        verify(ftpLookup).getFileSize(TEST_FTP_FILE);
    }

    @Test
    void shouldNotGetFileSizeIfNotLoggedIn() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        ftpConnection.setConnected(true);

        String status = ftpConnection.getFileSize(TEST_FTP_FILE);

        assertNull(status);
        verifyNoInteractions(ftpLookup);
    }

    @Test
    void shouldThrowIfNotConnectedOnFileSize() {
        ftpConnection.setLoggedIn(true);
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.getFileSize(TEST_FTP_FILE));
        assertFalse(ftpConnection.isLoggedIn());
        verifyNoInteractions(ftpLookup);
    }

    @Test
    void shouldThrowIfConnectionErrorOccursOnFileSize() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        doThrow(FTPConnectionClosedException.class).when(ftpLookup).getFileSize(TEST_FTP_FILE);

        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.getFileSize(TEST_FTP_FILE));
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
        verify(ftpLookup).getFileSize(TEST_FTP_FILE);
    }

    @Test
    void shouldThrowIfIOErrorOccursOnFileSize() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        doThrow(IOException.class).when(ftpLookup).getFileSize(TEST_FTP_FILE);

        assertThrows(FTPCommandFailedException.class, () -> ftpConnection.getFileSize(TEST_FTP_FILE));
        assertTrue(ftpConnection.isConnected());
        assertTrue(ftpConnection.isLoggedIn());
        verify(ftpLookup).getFileSize(TEST_FTP_FILE);
    }

    @Test
    void shouldGetModificationTimeSuccessfully() throws IOException, FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        given(ftpLookup.getModificationTime(TEST_FTP_FILE))
                .willReturn(TEST_TIME);

        String time = ftpConnection.getModificationTime(TEST_FTP_FILE);

        assertEquals(time, TEST_TIME);
        verify(ftpLookup).getModificationTime(TEST_FTP_FILE);
    }

    @Test
    void shouldReturnNullIfNotLoggedInOnModificationTime() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        ftpConnection.setConnected(true);

        String time = ftpConnection.getModificationTime(TEST_FTP_FILE);

        assertNull(time);
        verifyNoInteractions(ftpLookup);
    }

    @Test
    void shouldThrowIfNotConnectedOnModificationTime() {
        ftpConnection.setLoggedIn(true);
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.getModificationTime(TEST_FTP_FILE));
        assertFalse(ftpConnection.isLoggedIn());
        verifyNoInteractions(ftpLookup);
    }

    @Test
    void shouldThrowIfConnectionErrorOccursOnModificationTime() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        doThrow(FTPConnectionClosedException.class).when(ftpLookup).getModificationTime(TEST_FTP_FILE);

        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.getModificationTime(TEST_FTP_FILE));
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
        verify(ftpLookup).getModificationTime(TEST_FTP_FILE);
    }

    @Test
    void shouldThrowIfIOExceptionOccursOnModificationTime() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        doThrow(IOException.class).when(ftpLookup).getModificationTime(TEST_FTP_FILE);

        assertThrows(FTPCommandFailedException.class, () -> ftpConnection.getModificationTime(TEST_FTP_FILE));
        assertTrue(ftpConnection.isConnected());
        assertTrue(ftpConnection.isLoggedIn());
        verify(ftpLookup).getModificationTime(TEST_FTP_FILE);
    }

    @Test
    void shouldGetPathStatsSuccessfully() throws IOException, FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        FTPPathStats testStats = getTestPathStats();

        given(ftpLookup.getPathStats(TEST_FTP_FILE))
                .willReturn(testStats);

        FTPPathStats result = ftpConnection.getPathStats(TEST_FTP_FILE);

        assertEquals(result, testStats);
        verify(ftpLookup).getPathStats(TEST_FTP_FILE);
    }

    @Test
    void shouldReturnNullOnGetPathStatsIfNotLoggedIn() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        ftpConnection.setConnected(true);

        FTPPathStats result = ftpConnection.getPathStats(TEST_FTP_FILE);

        assertNull(result);
        verifyNoInteractions(ftpLookup);
    }

    @Test
    void shouldThrowIfNotConnectedOnGetPathStats() {
        ftpConnection.setLoggedIn(true);
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.getPathStats(TEST_FTP_FILE));
        assertFalse(ftpConnection.isLoggedIn());
        verifyNoInteractions(ftpLookup);
    }

    @Test
    void shouldThrowIfConnectionErrorOccursOnGetPathStats() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        doThrow(FTPConnectionClosedException.class).when(ftpLookup).getPathStats(TEST_FTP_FILE);

        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.getPathStats(TEST_FTP_FILE));
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
        verify(ftpLookup).getPathStats(TEST_FTP_FILE);
    }

    @Test
    void shouldThrowIfIOExceptionOccursOnGetPathStats() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        doThrow(IOException.class).when(ftpLookup).getPathStats(TEST_FTP_FILE);

        assertThrows(FTPCommandFailedException.class, () -> ftpConnection.getPathStats(TEST_FTP_FILE));
        assertTrue(ftpConnection.isConnected());
        assertTrue(ftpConnection.isLoggedIn());
        verify(ftpLookup).getPathStats(TEST_FTP_FILE);
    }

    private FTPPathStats getTestPathStats() {
        return new FTPPathStats(TEST_FTP_FILE, TEST_TIME, TEST_SIZE, TEST_FILE_STATUS);
    }

    @Test
    void shouldSetTimeoutTimeSuccessfully() {
        int expectedMillis = TEST_TIMEOUT_SECS * 1000;
        int expectedSecs = TEST_TIMEOUT_SECS;

        doCallRealMethod().when(ftpClient).setDefaultTimeout(expectedMillis);
        doCallRealMethod().when(ftpClient).setControlKeepAliveTimeout(expectedSecs);
        doCallRealMethod().when(ftpClient).getDefaultTimeout();
        doCallRealMethod().when(ftpClient).getControlKeepAliveTimeout();

        ftpConnection.setTimeoutTime();
        assertEquals(ftpConnection.getServer().getTimeout(), expectedSecs);
        assertEquals(ftpClient.getControlKeepAliveTimeout(), expectedSecs);
        verify(ftpClient, times(2)).setDefaultTimeout(expectedMillis); // once at construction, again on calling it
        verify(ftpClient, times(2)).setControlKeepAliveTimeout(expectedSecs);
    }

    @Test
    void shouldNotSetTimeOutIfConnected()  {
        ftpConnection.setConnected(true);

        ftpConnection.setTimeoutTime();
        ;
    }

    @Test
    void shouldGetReplyCodeSuccessfully() {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        int expectedReplyCode = FTPReply.COMMAND_OK;

        given(ftpClient.getReplyCode())
                .willReturn(expectedReplyCode);

        int code = ftpConnection.getReplyCode();

        assertEquals(expectedReplyCode, code);
        verify(ftpClient).getReplyCode();
    }

    @Test
    void shouldGetReplyStringSuccessfully() {
        String reply = "TEST_REPLY";
        given(ftpClient.getReplyString())
                .willReturn(reply);

        String result = ftpConnection.getReplyString();

        assertEquals(reply, result);
        verify(ftpClient).getReplyString();
    }

    /**
     * This is testing the assertion that <b>If connected = false, loggedIn should also be false</b>.
     * Implemented as per issue 10 {@see https://github.com/edwardUL99/simple-ftp/issues/10}
     */
    @Test
    void ifNotConnectedUserShouldNotBeLoggedIn() {
        /*
         *  SCENARIO 1:
         * 1- Attempt to login while not connected and ignore any exceptions
         * 2- Try and logout
         * 3- Logout should be a no-op
         */
        try {
            ftpConnection.login();
        } catch (Exception ex) {
            // ignore
        }
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
        try {
            ftpConnection.logout();
        } catch(Exception ec) {
            // ignore
        }
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());

        reset(ftpClient);
        /*
         * SCENARIO 2:
         * 1- Have the server be connected and logged in successfully
         * 2- Without calling logout(), call disconnect()
         * 3- Call logout(), this should be a no-op
         */
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        try {
            ftpConnection.disconnect();
            assertFalse(ftpConnection.isConnected());
            assertFalse(ftpConnection.isLoggedIn());
            verify(ftpClient).disconnect();
            reset(ftpClient);
            ftpConnection.logout();
            ;
            assertFalse(ftpConnection.isConnected());
            assertFalse(ftpConnection.isLoggedIn());
        } catch (Exception ex) {
            fail("An exception occurred where it should not have");
        }
    }

    @Test
    void shouldSetTextTransferModeSuccessfully() throws Exception {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        given(ftpClient.setFileType(FTPClient.ASCII_FILE_TYPE))
                .willReturn(true);

        boolean result = ftpConnection.setTextTransferMode(true);

        assertTrue(result);
        verify(ftpClient).setFileType(FTPClient.ASCII_FILE_TYPE);
    }

    @Test
    void shouldSetBinaryTransferModeSuccessfully() throws Exception {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        given(ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE))
                .willReturn(true);

        boolean result = ftpConnection.setTextTransferMode(false);

        assertTrue(result);
        verify(ftpClient).setFileType(FTPClient.BINARY_FILE_TYPE);
    }

    @Test
    void shouldReturnFalseIfLoggedOutOnSetTransferMode() throws Exception {
        ftpConnection.setConnected(true);

        boolean result = ftpConnection.setTextTransferMode(true);

        assertFalse(result);
    }

    @Test
    void shouldThrowIfNotConnectedOnSetTextTransferMode() {
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.setTextTransferMode(true));
    }

    @Test
    void shouldThrowIfConnectionClosesOnSetTextTransferMode() throws Exception {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        doThrow(FTPConnectionClosedException.class).when(ftpClient).setFileType(FTPClient.ASCII_FILE_TYPE);
        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.setTextTransferMode(true));
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
    }

    @Test
    void shouldThrowIfIOExceptionOccursOnSetTextTransferMode() throws Exception {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        doThrow(IOException.class).when(ftpClient).setFileType(FTPClient.BINARY_FILE_TYPE);
        assertThrows(FTPCommandFailedException.class, () -> ftpConnection.setTextTransferMode(false));
    }

    private Server getTestFTPServer() {
        return new Server().withServer(TEST_SERVER_HOST)
                .withUser(TEST_SERVER_USER)
                .withPassword(TEST_SERVER_PASSWORD)
                .withPort(TEST_SERVER_PORT);
    }

    @Test
    void shouldCreateSharedConnectionSuccessfully() {
        FTPSystem.reset();
        assertNull(FTPSystem.getConnection());

        FTPConnection connection = FTPConnection.createSharedConnection(getTestFTPServer());

        assertEquals(connection, FTPSystem.getConnection());
    }

    @Test
    void shouldReturnSharedConnectionIfCreatedWithSameDetails() {
        FTPSystem.reset();
        assertNull(FTPSystem.getConnection());

        Server testServer = getTestFTPServer();
        FTPConnection connection = FTPConnection.createSharedConnection(testServer);
        FTPConnection connection1 = FTPConnection.createSharedConnection(testServer);

        assertEquals(connection, FTPSystem.getConnection());
        assertEquals(connection1, FTPSystem.getConnection()); // should be the same
        assertSame(connection, connection1); // should be exact same address
    }

    @Test
    void shouldReturnNewConnectionIfDetailsChange() {
        FTPSystem.reset();
        assertNull(FTPSystem.getConnection());

        Server testServer = getTestFTPServer();

        FTPConnection connection = FTPConnection.createSharedConnection(testServer);

        assertEquals(connection, FTPSystem.getConnection());

        Server newServer = new Server("newServer", "user", "pass", 22, 200);
        FTPConnection connection1 = FTPConnection.createSharedConnection(newServer);

        assertEquals(connection1, FTPSystem.getConnection());
        assertNotEquals(connection, FTPSystem.getConnection());
        assertNotSame(connection, connection1);
    }

    @Test
    void shouldCreateSharedConnectionWithNullFTPServer() {
        FTPSystem.reset();
        assertNull(FTPSystem.getConnection());

        FTPConnection connection = FTPConnection.createSharedConnection(null);

        assertEquals(connection, FTPSystem.getConnection());
        assertNotNull(connection.getServer());
    }

    @Test
    void shouldCreateTempConnectionFromExisting() {
        FTPSystem.reset();

        Server server = getTestFTPServer();

        FTPConnection connection = FTPConnection.createSharedConnection(server);
        FTPConnection connection1 = FTPConnection.createTemporaryConnection(connection);
        Server server1 = connection1.getServer();

        assertEquals(connection, FTPSystem.getConnection());
        assertNotSame(connection, connection1);
        /*
          Should be equal by equals() method
         */
        assertEquals(server, server1);
        /*
            Shouldn't be reference equal, i.e. the objects should be cloned, so should be different objects
         */
        assertNotSame(server, server1);
    }

    @Test
    void shouldCreateTempConnectionFromDetails() {
        FTPSystem.reset();

        Server server = getTestFTPServer();

        FTPConnection connection = FTPConnection.createTemporaryConnection(server);
        Server server1 = connection.getServer();

        assertNull(FTPSystem.getConnection());
        assertEquals(server, server1);
        /*
        Here they should be the same objects as this method doesn't clone them
         */
        assertSame(server, server1);
    }

    @Test
    void shouldSendNoopSuccessfully() throws Exception {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        given(ftpClient.sendNoOp())
                .willReturn(true);

        boolean result = ftpConnection.sendNoop();

        assertTrue(result);
        verify(ftpClient).sendNoOp();
    }

    @Test
    void noopShouldReturnFalseWhenLoggedOut() throws Exception {
        ftpConnection.setConnected(true);

        assertFalse(ftpConnection.sendNoop());
        verify(ftpClient, times(0)).sendNoOp();
    }

    @Test
    void noopShouldThrowIfNotConnected() {
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.sendNoop());
    }

    @Test
    void noopShouldThrowIfAnErrorOccurs() throws Exception {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);

        doThrow(FTPConnectionClosedException.class).when(ftpClient).sendNoOp();

        assertThrows(FTPCommandFailedException.class, () -> ftpConnection.sendNoop());
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
    }
}