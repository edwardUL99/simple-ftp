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

package com.simpleftp.ftp.unit;

import com.simpleftp.ftp.FTPServer;
import com.simpleftp.ftp.exceptions.FTPCommandFailedException;
import com.simpleftp.ftp.exceptions.FTPConnectionFailedException;
import com.simpleftp.ftp.exceptions.FTPNotConnectedException;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class FTPConnectionUnitTest {

    @Mock
    private FTPClient ftpClient;
    @Mock
    private FTPServer ftpServer;

    /**
     * A test class which uses FTPConnection but allows toggling of certain fields to help with testing
     */
    @InjectMocks
    private FTPConnectionTestable ftpConnection;

    private AutoCloseable closeable;

    private static final String TEST_SERVER_HOST = "test-host";
    private static final String TEST_SERVER_USER = "test-user";
    private static final String TEST_SERVER_PASSWORD = "test-user-password";
    private static final int TEST_SERVER_PORT = 1234;
    private static final String TEST_PATH = "ftp://test/path";
    private static final String TEST_FTP_FILE = TEST_PATH + "/test-ftp-file";


    @BeforeEach
    void init() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void clean() throws Exception {
        closeable.close();
    }

    @Test
    void shouldConnectSuccessfully() throws IOException, FTPConnectionFailedException {
        given(ftpServer.getServer())
                .willReturn(TEST_SERVER_HOST);
        given(ftpServer.getPort())
                .willReturn(TEST_SERVER_PORT);
        doNothing().when(ftpClient).connect(TEST_SERVER_HOST, TEST_SERVER_PORT); //we'll assume it connected successfully
        given(ftpClient.getReplyCode())
                .willReturn(220); //service ready for new user code

        boolean result = ftpConnection.connect();
        assertTrue(result);
        assertTrue(ftpConnection.isConnected());
        verify(ftpServer).getServer();
        verify(ftpServer).getPort();
        verify(ftpClient).connect(TEST_SERVER_HOST, TEST_SERVER_PORT);
        verify(ftpClient).getReplyCode();
        verify(ftpClient, times(0)).disconnect();
    }

    @Test
    void shouldNotConnectSuccessfully() throws IOException {
        given(ftpServer.getServer())
                .willReturn(TEST_SERVER_HOST);
        given(ftpServer.getPort())
                .willReturn(TEST_SERVER_PORT);
        doThrow(IOException.class).when(ftpClient).connect(TEST_SERVER_HOST, TEST_SERVER_PORT);
        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.connect());
        assertFalse(ftpConnection.isConnected());
        verify(ftpServer).getServer();
        verify(ftpServer).getPort();
        verify(ftpClient).connect(TEST_SERVER_HOST, TEST_SERVER_PORT);
    }

    @Test
    void shouldNotConnectIfAlreadyConnected() throws FTPConnectionFailedException {
        ftpConnection.setConnected(true);
        boolean result = ftpConnection.connect();
        assertFalse(result);
        verifyNoInteractions(ftpServer);
        verifyNoInteractions(ftpClient);
    }

    @Test
    void shouldThrowIfConnectionFailsOnConnect() throws IOException {
        given(ftpServer.getServer())
                .willReturn(TEST_SERVER_HOST);
        given(ftpServer.getPort())
                .willReturn(TEST_SERVER_PORT);
        doNothing().when(ftpClient).connect(TEST_SERVER_HOST, TEST_SERVER_PORT);
        given(ftpClient.getReplyCode())
                .willReturn(426); //connection closed; transfer aborted code

        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.connect());
        assertFalse(ftpConnection.isConnected());
        verify(ftpClient).connect(TEST_SERVER_HOST, TEST_SERVER_PORT);
        verify(ftpClient).getReplyCode();
        verify(ftpClient).disconnect();
        verify(ftpServer).getServer();
        verify(ftpServer).getPort();
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
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.disconnect());
        assertFalse(ftpConnection.isConnected());
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
        given(ftpServer.getUser())
                .willReturn(TEST_SERVER_USER);
        given(ftpServer.getPassword())
                .willReturn(TEST_SERVER_PASSWORD);
        given(ftpClient.login(TEST_SERVER_USER, TEST_SERVER_PASSWORD))
            .willReturn(true);

        boolean result = ftpConnection.login();
        assertTrue(result);
        assertTrue(ftpConnection.isLoggedIn());
        verify(ftpServer).getUser();
        verify(ftpServer).getPassword();
        verify(ftpClient).login(TEST_SERVER_USER, TEST_SERVER_PASSWORD);
    }

    @Test
    void shouldNotLogInIfNotConnected() {
        given(ftpServer.getUser())
                .willReturn(TEST_SERVER_USER);
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.login());
        verify(ftpServer).getUser();
        verify(ftpServer, times(0)).getPassword();
        verifyNoInteractions(ftpClient);
    }

    @Test
    void shouldNotLogInIfErrorOccurs() throws IOException {
        ftpConnection.setConnected(true);
        given(ftpServer.getUser())
                .willReturn(TEST_SERVER_USER);
        given(ftpServer.getPassword())
                .willReturn(TEST_SERVER_PASSWORD);
        doThrow(IOException.class).when(ftpClient).login(TEST_SERVER_USER, TEST_SERVER_PASSWORD);

        assertThrows(FTPCommandFailedException.class, () -> ftpConnection.login());
        verify(ftpClient).login(TEST_SERVER_USER, TEST_SERVER_PASSWORD);
    }

    @Test
    void shouldNotLogInIfConnectionErrorOccurs() throws IOException {
        ftpConnection.setConnected(true);
        given(ftpServer.getUser())
                .willReturn(TEST_SERVER_USER);
        given(ftpServer.getPassword())
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
        given(ftpServer.getUser())
                .willReturn(TEST_SERVER_USER);

        boolean result = ftpConnection.login();

        assertFalse(result);
        verify(ftpClient, times(0)).login(TEST_SERVER_USER, TEST_SERVER_PASSWORD);
        verify(ftpServer).getUser();
        verify(ftpServer, times(0)).getPassword();
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
        verifyNoInteractions(ftpClient);
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
        verifyNoInteractions(ftpClient);
    }

    @Test
    void shouldChangeWorkingDirectorySuccessfully() throws IOException, FTPConnectionFailedException, FTPNotConnectedException, FTPCommandFailedException {
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
    void shouldNotChangeDirectoryIfNotLoggedIn() throws FTPConnectionFailedException, FTPNotConnectedException, FTPCommandFailedException {
        ftpConnection.setConnected(true);

        boolean result = ftpConnection.changeWorkingDirectory(TEST_PATH);

        assertFalse(result);
        assertFalse(ftpConnection.isLoggedIn());
        assertTrue(ftpConnection.isConnected());
        verifyNoInteractions(ftpClient);
    }

    @Test
    void shouldThrowIfNotConnectedWhenChangingDirectory() {
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.changeWorkingDirectory(TEST_PATH));
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
        verifyNoInteractions(ftpClient);
    }

    @Test
    void shouldThrowIfErrorOccursChangingDirectory() throws IOException {
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
        verifyNoInteractions(ftpClient);
    }

    @Test
    void shouldThrowIfNotConnectedWhenChangingToParentDirectory() {
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.changeToParentDirectory());
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
        verifyNoInteractions(ftpClient);
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

    private FTPFile getTestFTPFile() {
        FTPFile ftpFile = new FTPFile();
        ftpFile.setUser(TEST_SERVER_USER);
        ftpFile.setGroup("test-group");
        ftpFile.setName(TEST_FTP_FILE);
        return ftpFile;
    }

    @Test
    void shouldGetFTPFileSuccessfullyByMLST() throws IOException, FTPConnectionFailedException, FTPNotConnectedException, FTPCommandFailedException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        given(ftpClient.hasFeature("MLST"))
                .willReturn(true);
        FTPFile testFile = getTestFTPFile();
        given(ftpClient.mlistFile(TEST_FTP_FILE))
                .willReturn(testFile);

        FTPFile result = ftpConnection.getFTPFile(TEST_FTP_FILE);

        assertEquals(result, testFile);
        verify(ftpClient).hasFeature("MLST");
        verify(ftpClient).mlistFile(TEST_FTP_FILE);
        verify(ftpClient, times(0)).listFiles(TEST_FTP_FILE);
    }

    @Test
    void shouldGetFTPFileSuccessfullyByLIST() throws IOException, FTPConnectionFailedException, FTPNotConnectedException, FTPCommandFailedException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        given(ftpClient.hasFeature("MLST"))
                .willReturn(false);
        FTPFile testFile = getTestFTPFile();
        given(ftpClient.listFiles(TEST_FTP_FILE))
                .willReturn(new FTPFile[]{testFile});

        FTPFile result = ftpConnection.getFTPFile(TEST_FTP_FILE);

        assertEquals(result, testFile);
        verify(ftpClient).hasFeature("MLST");
        verify(ftpClient).listFiles(TEST_FTP_FILE);
        verify(ftpClient, times(0)).mlistFile(TEST_FTP_FILE);
    }

    @Test
    void shouldReturnNullWithMLSTGetFTPFile() throws IOException, FTPConnectionFailedException, FTPNotConnectedException, FTPCommandFailedException {
        ftpConnection.setLoggedIn(true);
        ftpConnection.setConnected(true);
        given(ftpClient.hasFeature("MLST"))
                .willReturn(true);
        given(ftpClient.mlistFile(TEST_FTP_FILE))
                .willReturn(null);

        FTPFile result = ftpConnection.getFTPFile(TEST_FTP_FILE);

        assertNull(result);
        verify(ftpClient).hasFeature("MLST");
        verify(ftpClient).mlistFile(TEST_FTP_FILE);
        verify(ftpClient, times(0)).listFiles(TEST_FTP_FILE);
    }

    @Test
    void shouldReturnNullWithLISTReturningNullGetFTPFile() throws IOException, FTPConnectionFailedException, FTPNotConnectedException, FTPCommandFailedException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        given(ftpClient.hasFeature("MLST"))
                .willReturn(false);
        given(ftpClient.listFiles(TEST_FTP_FILE))
                .willReturn(null);

        FTPFile result = ftpConnection.getFTPFile(TEST_FTP_FILE);

        assertNull(result);
        verify(ftpClient).hasFeature("MLST");
        verify(ftpClient).listFiles(TEST_FTP_FILE);
        verify(ftpClient, times(0)).mlistFile(TEST_FTP_FILE);
    }

    @Test
    void shouldReturnNullIfLISTReturnsEmptyArrayGetFTPFile() throws IOException, FTPConnectionFailedException, FTPNotConnectedException, FTPCommandFailedException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        given(ftpClient.hasFeature("MLST"))
                .willReturn(false);
        given(ftpClient.listFiles(TEST_FTP_FILE))
                .willReturn(new FTPFile[]{});

        FTPFile result = ftpConnection.getFTPFile(TEST_FTP_FILE);

        assertNull(result);
        verify(ftpClient).hasFeature("MLST");
        verify(ftpClient).listFiles(TEST_FTP_FILE);
        verify(ftpClient, times(0)).mlistFile(TEST_FTP_FILE);
    }

    @Test
    void shouldReturnNullIfLoggedOutOnGetFTPFile() throws FTPConnectionFailedException, FTPNotConnectedException, FTPCommandFailedException {
        ftpConnection.setConnected(true);
        FTPFile result = ftpConnection.getFTPFile(TEST_FTP_FILE);

        assertNull(result);
        assertFalse(ftpConnection.isLoggedIn());
        verifyNoInteractions(ftpClient);
    }

    @Test
    void shouldThrowOnGetFTPFileIfNotConnected() {
        assertFalse(ftpConnection.isConnected());
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.getFTPFile(TEST_FTP_FILE));
    }

    @Test
    void shouldThrowIfErrorOccursWithMLSTGetFTPFile() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        given(ftpClient.hasFeature("MLST"))
                .willReturn(true);
        doThrow(IOException.class).when(ftpClient).mlistFile(TEST_FTP_FILE);

        assertThrows(FTPCommandFailedException.class, () -> ftpConnection.getFTPFile(TEST_FTP_FILE));
        assertTrue(ftpConnection.isConnected());
        assertTrue(ftpConnection.isLoggedIn());
        verify(ftpClient).hasFeature("MLST");
        verify(ftpClient).mlistFile(TEST_FTP_FILE);
        verify(ftpClient, times(0)).listFiles(TEST_FTP_FILE);
    }

    @Test
    void shouldThrowIfConnectionErrorOccursWithMLSTGetFTPFile() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        given(ftpClient.hasFeature("MLST"))
                .willReturn(true);
        doThrow(FTPConnectionClosedException.class).when(ftpClient).mlistFile(TEST_FTP_FILE);

        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.getFTPFile(TEST_FTP_FILE));
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
        verify(ftpClient).hasFeature("MLST");
        verify(ftpClient).mlistFile(TEST_FTP_FILE);
        verify(ftpClient, times(0)).listFiles(TEST_FTP_FILE);
    }

    @Test
    void shouldThrowIfErrorOccursWithLISTGetFTPFile() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        given(ftpClient.hasFeature("MLST"))
                .willReturn(false);
        doThrow(IOException.class).when(ftpClient).listFiles(TEST_FTP_FILE);

        assertThrows(FTPCommandFailedException.class, () -> ftpConnection.getFTPFile(TEST_FTP_FILE));
        assertTrue(ftpConnection.isConnected());
        assertTrue(ftpConnection.isLoggedIn());
        verify(ftpClient).hasFeature("MLST");
        verify(ftpClient).listFiles(TEST_FTP_FILE);
        verify(ftpClient, times(0)).mlistFile(TEST_FTP_FILE);
    }

    @Test
    void shouldThrowIfConnectionErrorOccursWithLISTGetFTPFile() throws IOException {
        ftpConnection.setConnected(true);
        ftpConnection.setLoggedIn(true);
        given(ftpClient.hasFeature("MLST"))
                .willReturn(false);
        doThrow(FTPConnectionClosedException.class).when(ftpClient).listFiles(TEST_FTP_FILE);

        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.getFTPFile(TEST_FTP_FILE));
        assertFalse(ftpConnection.isConnected());
        assertFalse(ftpConnection.isLoggedIn());
        verify(ftpClient).hasFeature("MLST");
        verify(ftpClient).listFiles(TEST_FTP_FILE);
        verify(ftpClient, times(0)).mlistFile(TEST_FTP_FILE);
    }

    @Test
    void addFile() {
    }

    @Test
    void removeFile() {
    }

    @Test
    void getStatus() {
    }

    @Test
    void getPathStats() {
    }
}