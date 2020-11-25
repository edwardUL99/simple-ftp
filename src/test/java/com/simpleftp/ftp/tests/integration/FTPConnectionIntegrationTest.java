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

import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ftp.connection.FTPPathStats;
import com.simpleftp.ftp.connection.FTPServer;
import com.simpleftp.ftp.exceptions.*;
import com.simpleftp.ftp.tests.FTPConnectionTestable;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockftpserver.core.command.AbstractCommandHandler;
import org.mockftpserver.core.command.Command;
import org.mockftpserver.core.session.Session;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

public class FTPConnectionIntegrationTest {
    private FakeFtpServer ftpServer;

    private FTPConnectionTestable ftpConnection;

    private static final String TEST_SERVER_USER = "test-user";
    private static final String TEST_SERVER_PASSWORD = "test-user-password";
    private static final int TEST_SERVER_PORT = 1234;
    private static final String TEST_HOME = "/test";
    private static final String TEST_PATH = "/test/path";
    private static final String TEST_FTP_FILE = "/test/path/test-ftp-file";
    private static final String TEST_FTP_FILE_RENAMED = "/test/path/test-ftp-file.txt";
    private static final String TEST_DIR1 = "/test/path/dir1";
    private static final String TEST_DIR2 = "dir2";

    @BeforeEach
    void setup() {
        ftpServer = new FakeFtpServer();
        ftpServer.addUserAccount(new UserAccount(TEST_SERVER_USER, TEST_SERVER_PASSWORD, TEST_HOME));

        FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry(TEST_PATH));
        fileSystem.add(new FileEntry(TEST_FTP_FILE, "abcdef"));
        ftpServer.setFileSystem(fileSystem);
        ftpServer.setServerControlPort(TEST_SERVER_PORT);
        ftpServer.setCommandHandler("MDTM", new MDTMHandler());

        ftpServer.start();

        FTPServer serverDetails = new FTPServer("localhost", TEST_SERVER_USER, TEST_SERVER_PASSWORD, TEST_SERVER_PORT);
        FTPSystem.setSystemTesting(true);
        ftpConnection = new FTPConnectionTestable();
        ftpConnection.setFtpServer(serverDetails);
    }

    @AfterEach
    void tearDown() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        if (ftpConnection.isConnected())
            ftpConnection.disconnect();
        ftpServer.stop();
    }

    @Test
    void shouldConnectSuccessfully() throws FTPConnectionFailedException {
        assertFalse(ftpConnection.isConnected());
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.isConnected());
    }

    @Test
    void shouldDisconnectSuccessfully() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.isConnected());
        ftpConnection.disconnect();
        assertFalse(ftpConnection.isConnected());
    }

    @Test
    void shouldLoginSuccessfully() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());
        assertTrue(ftpConnection.isConnected());
        assertTrue(ftpConnection.isLoggedIn());
    }

    @Test
    void shouldLogoutSuccessfully() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());
        assertTrue(ftpConnection.isLoggedIn());
        assertTrue(ftpConnection.logout());
        assertFalse(ftpConnection.isLoggedIn());
    }

    @Test
    void shouldChangeWorkingDirectorySuccessfully() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException, FTPError, FTPRemotePathNotFoundException {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());
        assertTrue(ftpConnection.changeWorkingDirectory(TEST_PATH));
    }

    @Test
    void shouldChangeToParentDirectorySuccessfully() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException, FTPError, FTPRemotePathNotFoundException {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());
        assertTrue(ftpConnection.changeWorkingDirectory(TEST_PATH));
        assertTrue(ftpConnection.changeToParentDirectory());
    }

    @Test
    void shouldGetWorkingDirectorySuccessfully() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());
        assertEquals(ftpConnection.getWorkingDirectory(), TEST_HOME);
    }

    @Test
    void shouldGetFTPFileSuccessfully() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException, FTPRemotePathNotFoundException, FTPError {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());
        assertTrue(ftpServer.getFileSystem().exists(TEST_FTP_FILE));
        assertNotNull(ftpConnection.getFTPFile(TEST_FTP_FILE));
    }

    @Test
    void shouldListFilesSuccessfully() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());
        assertNotNull(ftpConnection.listFiles(TEST_PATH));
    }

    @Test
    void shouldUploadFileSuccessfully() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException, IOException, FTPError {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());

        File file = new File("test.txt");
        file.createNewFile();

        FTPFile uploaded = ftpConnection.uploadFile(file, TEST_PATH);
        assertNotNull(uploaded);
        assertTrue(ftpServer.getFileSystem().exists(TEST_PATH + "/test.txt"));
        file.delete();
    }

    @Test
    void shouldUploadAsStringSuccessfully() throws IOException, FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException, FTPError {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());

        File file = new File("test.txt");
        file.createNewFile();

        FTPFile uploaded = ftpConnection.uploadFile("test.txt", TEST_PATH);
        assertNotNull(uploaded);
        assertTrue(ftpServer.getFileSystem().exists(TEST_PATH + "/test.txt"));
        file.delete();
    }

    @Test
    void shouldDownloadFileSuccessfully() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException, FTPError {
        String localPath = System.getProperty("user.dir");

        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());

        File file = ftpConnection.downloadFile(TEST_FTP_FILE, localPath);
        assertNotNull(file);
        assertTrue(file.exists());

        file.delete();
    }

    @Test
    void shouldMakeDirSuccessfullyAsAbsolutePath() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException, FTPError {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());
        assertTrue(ftpConnection.makeDirectory(TEST_DIR1));
        assertTrue(ftpServer.getFileSystem().exists(TEST_DIR1));
    }

    @Test
    void shouldMakeDirSuccessfullyAsAbstractPath() throws FTPConnectionFailedException, FTPError, FTPNotConnectedException, FTPCommandFailedException {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());
        assertTrue(ftpConnection.makeDirectory(TEST_DIR2));
        assertTrue(ftpServer.getFileSystem().exists("/test/dir2"));
    }

    @Test
    void shouldRenameFileSuccessfully() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());
        assertTrue(ftpServer.getFileSystem().exists(TEST_FTP_FILE));
        assertTrue(ftpConnection.renameFile(TEST_FTP_FILE, TEST_FTP_FILE_RENAMED));
        assertTrue(ftpServer.getFileSystem().exists(TEST_FTP_FILE_RENAMED));
        assertFalse(ftpServer.getFileSystem().exists(TEST_FTP_FILE));
    }

    @Test
    void shouldNotRenameFileThatDoesntExist() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());
        assertFalse(ftpConnection.renameFile("NOT_EXISTS", "EXISTS"));
    }

    @Test
    void shouldRemoveFileSuccessfully() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());
        assertTrue(ftpConnection.removeFile(TEST_FTP_FILE));
        assertFalse(ftpServer.getFileSystem().exists(TEST_FTP_FILE));
    }

    @Test
    void shouldRemoveDirectorySuccessfully() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        String emptyDir = "/test/path/empty-dir";
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());
        ftpServer.getFileSystem().add(new DirectoryEntry(emptyDir));
        assertTrue(ftpServer.getFileSystem().exists(emptyDir));
        assertTrue(ftpConnection.removeDirectory(emptyDir));
        assertFalse(ftpServer.getFileSystem().exists(emptyDir));
    }

    @Test
    void shouldNotRemoveNonEmptyDirectory() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());
        assertFalse(ftpConnection.removeDirectory(TEST_PATH));
        assertTrue(ftpServer.getFileSystem().exists(TEST_PATH));
    }

    @Test
    void shouldCheckIfRemoteDirExistsSuccessfully() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException, FTPError {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());
        assertTrue(ftpConnection.remotePathExists(TEST_PATH, true));
        assertTrue(ftpServer.getFileSystem().exists(TEST_PATH));
    }

    @Test
    void shouldReturnFalseIfRemotePathCalledOnDirWithFile() throws FTPConnectionFailedException, FTPError, FTPNotConnectedException, FTPCommandFailedException {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());
        assertFalse(ftpConnection.remotePathExists(TEST_PATH, false));
    }

    @Test
    void shouldCheckIfRemoteFileExistsSuccessfully() throws FTPConnectionFailedException, FTPError, FTPNotConnectedException, FTPCommandFailedException {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());
        assertTrue(ftpServer.getFileSystem().exists(TEST_FTP_FILE));
        assertTrue(ftpConnection.remotePathExists(TEST_FTP_FILE, false));
    }

    @Test
    void shouldReturnFalseIfRemoteFileExistsOnFileWithDirSpecified() throws FTPConnectionFailedException, FTPError, FTPNotConnectedException, FTPCommandFailedException {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());
        assertFalse(ftpConnection.remotePathExists(TEST_FTP_FILE, true));
    }

    @Test
    void shouldCheckIfFileGenerallyExistsSuccessfully() throws FTPConnectionFailedException, FTPError, FTPNotConnectedException, FTPCommandFailedException {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());
        assertTrue(ftpConnection.remotePathExists(TEST_PATH));
        assertTrue(ftpServer.getFileSystem().exists(TEST_PATH));
        assertTrue(ftpConnection.remotePathExists(TEST_FTP_FILE));
        assertTrue(ftpServer.getFileSystem().exists(TEST_FTP_FILE));
    }

    @Test
    void shouldGetStatusSuccessfully() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());

        String status = ftpConnection.getStatus();

        assertNotNull(status);
        assertNotEquals("", status);
    }

    @Test
    void shouldGetFileStatusSuccessfully() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());

        String status = ftpConnection.getFileStatus(TEST_FTP_FILE);

        assertNotNull(status);
        assertNotEquals("", status);
    }

    @Test
    void shouldGetFileSizeSuccessfully() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());

        String size = ftpConnection.getFileSize(TEST_FTP_FILE);
        assertNotEquals("", size);
    }

    @Test
    void shouldGetModificationTimeSuccessfully() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());

        String time = ftpConnection.getModificationTime(TEST_FTP_FILE);
        assertNotEquals("", time);
        assertDoesNotThrow(() -> LocalDateTime.parse(time, DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")));
    }

    @Test
    void shouldGetPathStatsSuccessfully() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());

        FTPPathStats stats = ftpConnection.getPathStats(TEST_FTP_FILE);
        assertNotNull(stats);
    }

    @Test
    void shouldGetReplyStringSuccessfully() throws FTPConnectionFailedException, FTPCommandFailedException, FTPNotConnectedException {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());
        assertEquals("230 User logged in, proceed.\r\n", ftpConnection.getReplyString());
    }

    @Test
    void shouldSetTextTransferModeSuccessfully() throws FTPException {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());
        assertTrue(ftpConnection.setTextTransferMode(true));
    }

    @Test
    void shouldSetBinaryTransferModeSuccessfully() throws FTPException {
        assertTrue(ftpConnection.connect());
        assertTrue(ftpConnection.login());
        assertTrue(ftpConnection.setTextTransferMode(false));
    }
}

class MDTMHandler extends AbstractCommandHandler {
    @Override
    public void handleCommand(Command command, Session session) throws Exception {
        if (command.getName().equals("MDTM")) {
            LocalDateTime current = LocalDateTime.now();
            String time = current.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            session.sendReply(FTPReply.COMMAND_OK, time);
        }
    }
}