package com.simpleftp.ftp.unit;

import com.simpleftp.ftp.FTPServer;
import com.simpleftp.ftp.exceptions.FTPConnectionFailedException;
import com.simpleftp.ftp.exceptions.FTPNotConnectedException;
import org.apache.commons.net.ftp.FTPClient;
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

        boolean result = ftpConnection.connect();
        assertTrue(result);
        assertTrue(ftpConnection.isConnected());
        verify(ftpServer).getServer();
        verify(ftpServer).getPort();
        verify(ftpClient).connect(TEST_SERVER_HOST, TEST_SERVER_PORT);
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
    void shouldNotConnectIfAlreadyConnected() throws IOException, FTPConnectionFailedException {
        ftpConnection.setConnected(true);
        boolean result = ftpConnection.connect();
        assertFalse(result);
        verifyNoInteractions(ftpServer);
        verifyNoInteractions(ftpClient);
    }

    @Test
    void shouldDisconnectSuccessfully() throws IOException {
        doNothing().when(ftpClient).disconnect();
        assertFalse(ftpConnection.isConnected());
    }

    @Test
    void shouldThrowIfNotConnectedWhenDisconnecting() {
        assertThrows(FTPNotConnectedException.class, () -> ftpConnection.disconnect());
        assertFalse(ftpConnection.isConnected());
    }

    @Test
    void shouldThrowIfErrorOccursWhenDisconnecting() throws IOException {
        ftpConnection.setConnected(true);
        doThrow(IOException.class).when(ftpClient).disconnect();
        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.disconnect());
        assertTrue(ftpConnection.isConnected()); //should still be connected afterwards
    }

    @Test
    void shouldLoginSuccessfully() throws IOException, FTPConnectionFailedException, FTPNotConnectedException {
        ftpConnection.setConnected(true);
        given(ftpServer.getUser())
                .willReturn(TEST_SERVER_USER);
        given(ftpServer.getPassword())
                .willReturn(TEST_SERVER_PASSWORD);
        given(ftpClient.login(TEST_SERVER_USER, TEST_SERVER_PASSWORD))
            .willReturn(true);

        boolean result = ftpConnection.login();
        assertTrue(result);
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

        assertThrows(FTPConnectionFailedException.class, () -> ftpConnection.login());
    }

    @Test
    void changeWorkingDirectory() {
    }

    @Test
    void changeToParentDirectory() {
    }

    @Test
    void getFile() {
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