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

import com.simpleftp.ftp.FTPLookup;
import com.simpleftp.ftp.FTPPathStats;
import com.simpleftp.ftp.exceptions.FTPError;
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
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class FTPLookupUnitTest {
    @Mock
    private FTPClient ftpClient;
    @InjectMocks
    private FTPLookup ftpLookup;

    private static final String TEST_SERVER_USER = "test-user";
    private static final String TEST_PATH = "ftp://test/path";
    private static final String TEST_FTP_FILE = TEST_PATH + "/test-ftp-file";
    private static final String TEST_STATUS = "test-status";
    private static final String TEST_SIZE = "test-size";
    private static final String TEST_TIME = "Tue Jun 03 11:05:30 " + TimeZone.getDefault().getDisplayName(TimeZone.getDefault().observesDaylightTime(), TimeZone.SHORT) + " 2008";
    private static final String TEST_FILE_STATUS = " test-file-status";

    private AutoCloseable closeable;

    @BeforeEach
    void init() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void clean() throws Exception {
        closeable.close();
    }

    private FTPFile getTestFTPFile() {
        FTPFile ftpFile = new FTPFile();
        ftpFile.setUser(TEST_SERVER_USER);
        ftpFile.setGroup("test-group");
        ftpFile.setName(TEST_FTP_FILE);
        return ftpFile;
    }

    @Test
    void shouldGetFTPFileUsingMLSTSuccessfully() throws IOException {
        FTPFile testFile = getTestFTPFile();
        given(ftpClient.hasFeature("MLST"))
                .willReturn(true);
        given(ftpClient.mlistFile(TEST_FTP_FILE))
                .willReturn(testFile);

        FTPFile result = ftpLookup.getFTPFile(TEST_FTP_FILE);

        assertEquals(testFile, result);
        verify(ftpClient).hasFeature("MLST");
        verify(ftpClient).mlistFile(TEST_FTP_FILE);
    }

    @Test
    void shouldGetFTPFileUsingLISTSuccessfully() throws IOException {
        FTPFile testFile = getTestFTPFile();
        given(ftpClient.hasFeature("MLST"))
                .willReturn(false);
        given(ftpClient.listFiles(TEST_FTP_FILE))
                .willReturn(new FTPFile[]{testFile});

        FTPFile result = ftpLookup.getFTPFile(TEST_FTP_FILE);

        assertEquals(testFile, result);
        verify(ftpClient).hasFeature("MLST");
        verify(ftpClient).listFiles(TEST_FTP_FILE);
    }

    @Test
    void shouldThrowIfIOExceptionOccursOnMLSTGetFTPFile() throws IOException {
        given(ftpClient.hasFeature("MLST"))
                .willReturn(true);

        doThrow(IOException.class).when(ftpClient).mlistFile(TEST_FTP_FILE);

        assertThrows(IOException.class, () -> ftpLookup.getFTPFile(TEST_FTP_FILE));
        verify(ftpClient).hasFeature("MLST");
        verify(ftpClient).mlistFile(TEST_FTP_FILE);
    }

    @Test
    void shouldThrowIfConnectionErrorOccursOnLISTGetFTPFile() throws IOException {
        given(ftpClient.hasFeature("MLST"))
                .willReturn(false);

        doThrow(FTPConnectionClosedException.class).when(ftpClient).listFiles(TEST_FTP_FILE);

        assertThrows(FTPConnectionClosedException.class, () -> ftpLookup.getFTPFile(TEST_FTP_FILE));
        verify(ftpClient).hasFeature("MLST");
        verify(ftpClient).listFiles(TEST_FTP_FILE);
    }

    @Test
    void shouldThrowIfIOExceptionOccursOnLISTGetFTPFile() throws IOException {
        given(ftpClient.hasFeature("MLST"))
                .willReturn(false);

        doThrow(IOException.class).when(ftpClient).listFiles(TEST_FTP_FILE);

        assertThrows(IOException.class, () -> ftpLookup.getFTPFile(TEST_FTP_FILE));
        verify(ftpClient).hasFeature("MLST");
        verify(ftpClient).listFiles(TEST_FTP_FILE);
    }

    @Test
    void shouldDoRemotePathExistsDirSuccessfully() throws IOException, FTPError {
        given(ftpClient.printWorkingDirectory())
                .willReturn(TEST_PATH);
        given(ftpClient.changeWorkingDirectory(TEST_FTP_FILE)) //here "pretend" this is a dir
                .willReturn(true);

        boolean result = ftpLookup.remotePathExists(TEST_FTP_FILE, true);

        assertTrue(result);
        verify(ftpClient).printWorkingDirectory();
        verify(ftpClient).changeWorkingDirectory(TEST_FTP_FILE);
        verify(ftpClient).changeWorkingDirectory(TEST_PATH);
    }

    @Test
    void shouldThrowIfCurrentDirectoryRetrievalFailsRemotePathExistsDir() throws IOException, FTPError {
        given(ftpClient.printWorkingDirectory())
                .willReturn(null);

        assertThrows(FTPError.class, () -> ftpLookup.remotePathExists(TEST_PATH, true));
        verify(ftpClient).printWorkingDirectory();
        verify(ftpClient, times(0)).changeWorkingDirectory(any(String.class));
    }

    @Test
    void shouldDoRemotePathExistsSingleFileSuccessfully() throws IOException, FTPError {
        FTPFile testFTPFile1 = getTestFTPFile();
        FTPFile testFTPFile2 = getTestFTPFile();
        testFTPFile2.setName(TEST_FTP_FILE + "/other");

        given(ftpClient.listFiles(TEST_FTP_FILE))
                .willReturn(new FTPFile[]{testFTPFile1, testFTPFile2});

        boolean result = ftpLookup.remotePathExists(TEST_FTP_FILE, false);

        assertTrue(result);
        verify(ftpClient).listFiles(TEST_FTP_FILE);
    }

    @Test
    void shouldNotDoRemotePathExistsSingleFileSuccessfully() throws IOException, FTPError {
        given(ftpClient.listFiles(TEST_FTP_FILE))
                .willReturn(null);

        boolean result = ftpLookup.remotePathExists(TEST_FTP_FILE, false);

        assertFalse(result);
        verify(ftpClient).listFiles(TEST_FTP_FILE);
    }

    @Test
    void shouldThrowIfConnectionErrorOnRemotePathExistSingleFile() throws IOException {
        doThrow(FTPConnectionClosedException.class).when(ftpClient).listFiles(TEST_FTP_FILE);

        assertThrows(IOException.class, () -> ftpLookup.remotePathExists(TEST_FTP_FILE, false));
        verify(ftpClient).listFiles(TEST_FTP_FILE);
    }

    @Test
    void shouldThrowIfIOExceptionOnRemotePathExistSingleFile() throws IOException {
        doThrow(IOException.class).when(ftpClient).listFiles(TEST_FTP_FILE);

        assertThrows(IOException.class, () -> ftpLookup.remotePathExists(TEST_FTP_FILE, false));
        verify(ftpClient).listFiles(TEST_FTP_FILE);
    }

    @Test
    void shouldGetStatusSuccessfully() throws IOException {
        given(ftpClient.getStatus())
                .willReturn(TEST_STATUS);

        String status = ftpLookup.getStatus();

        assertEquals(status, TEST_STATUS);
        verify(ftpClient).getStatus();
    }

    @Test
    void shouldThrowIfConnectionErrorOccursOnGetStatus() throws IOException {
        doThrow(FTPConnectionClosedException.class).when(ftpClient).getStatus();

        assertThrows(IOException.class, () -> ftpLookup.getStatus());
        verify(ftpClient).getStatus();
    }

    @Test
    void shouldThrowIfIOExceptionOccursOnGetStatus() throws IOException {
        doThrow(IOException.class).when(ftpClient).getStatus();

        assertThrows(IOException.class, () -> ftpLookup.getStatus());
        verify(ftpClient).getStatus();
    }

    @Test
    void shouldGetFileStatusSuccessfully() throws IOException {
        given(ftpClient.getStatus(TEST_FTP_FILE))
                .willReturn(TEST_FILE_STATUS);

        String status = ftpLookup.getFileStatus(TEST_FTP_FILE);

        assertEquals(status, TEST_FILE_STATUS);
        verify(ftpClient).getStatus(TEST_FTP_FILE);
    }

    @Test
    void shouldThrowIfConnectionErrorOccursOnGetFileStatus() throws IOException {
        doThrow(FTPConnectionClosedException.class).when(ftpClient).getStatus(TEST_FTP_FILE);

        assertThrows(IOException.class, () -> ftpLookup.getFileStatus(TEST_FTP_FILE));
        verify(ftpClient).getStatus(TEST_FTP_FILE);
    }

    @Test
    void shouldThrowIfIOExceptionOccursOnGetFileStatus() throws IOException {
        doThrow(IOException.class).when(ftpClient).getStatus(TEST_FTP_FILE);

        assertThrows(IOException.class, () -> ftpLookup.getFileStatus(TEST_FTP_FILE));
        verify(ftpClient).getStatus(TEST_FTP_FILE);
    }

    @Test
    void shouldGetFileSizeSuccessfully() throws IOException {
        given(ftpClient.getSize(TEST_FTP_FILE))
                .willReturn(TEST_SIZE);

        String size = ftpLookup.getFileSize(TEST_FTP_FILE);

        assertEquals(size, TEST_SIZE);
        verify(ftpClient).getSize(TEST_FTP_FILE);
    }

    @Test
    void shouldThrowIfConnectionErrorOccursOnGetFileSize() throws IOException {
        doThrow(FTPConnectionClosedException.class).when(ftpClient).getSize(TEST_FTP_FILE);

        assertThrows(IOException.class, () -> ftpLookup.getFileSize(TEST_FTP_FILE));
        verify(ftpClient).getSize(TEST_FTP_FILE);
    }

    @Test
    void shouldThrowIfIOExceptionOccursOnGetFileSize() throws IOException {
        doThrow(IOException.class).when(ftpClient).getSize(TEST_FTP_FILE);

        assertThrows(IOException.class, () -> ftpLookup.getFileSize(TEST_FTP_FILE));
        verify(ftpClient).getSize(TEST_FTP_FILE);
    }

    @Test
    void shouldGetModificationTimeSuccessfully() throws IOException {
        given(ftpClient.getModificationTime(TEST_FTP_FILE))
                .willReturn("20080603110530");

        String result = ftpLookup.getModificationTime(TEST_FTP_FILE);

        assertEquals(result, TEST_TIME);
        verify(ftpClient).getModificationTime(TEST_FTP_FILE);
    }

    @Test
    void shouldNotReturnModificationTimeSuccessfully() throws IOException {
        given(ftpClient.getModificationTime(TEST_FTP_FILE))
                .willReturn(null);

        String result = ftpLookup.getModificationTime(TEST_FTP_FILE);

        assertNull(result);
        verify(ftpClient).getModificationTime(TEST_FTP_FILE);
    }

    @Test
    void shouldThrowIfIOExceptionOccursOnGetModificationTime() throws IOException {
        doThrow(IOException.class).when(ftpClient).getModificationTime(TEST_FTP_FILE);

        assertThrows(IOException.class, () -> ftpLookup.getModificationTime(TEST_FTP_FILE));
        verify(ftpClient).getModificationTime(TEST_FTP_FILE);
    }

    @Test
    void shouldGetPathStatsSuccessfully() throws IOException {
        FTPPathStats pathStats = new FTPPathStats(TEST_FTP_FILE, TEST_TIME, TEST_FILE_STATUS, TEST_SIZE);
        given(ftpClient.getModificationTime(TEST_FTP_FILE))
                .willReturn("20080603110530");
        given(ftpClient.getStatus(TEST_FTP_FILE))
                .willReturn(TEST_FILE_STATUS);
        given(ftpClient.getSize(TEST_FTP_FILE))
                .willReturn(TEST_SIZE);

        FTPPathStats result = ftpLookup.getPathStats(TEST_FTP_FILE);

        assertEquals(pathStats, result);
        verify(ftpClient).getModificationTime(TEST_FTP_FILE);
        verify(ftpClient).getStatus(TEST_FTP_FILE);
        verify(ftpClient).getSize(TEST_FTP_FILE);
    }

    @Test
    void shouldThrowIfIOExceptionOccursOnGetPathStats() throws IOException {
        // all sub-methods throw IOException, so only make one throw, as exception cases have been tested for each
        doThrow(IOException.class).when(ftpClient).getModificationTime(TEST_FTP_FILE);

        assertThrows(IOException.class, () -> ftpLookup.getPathStats(TEST_FTP_FILE));
        verify(ftpClient).getModificationTime(TEST_FTP_FILE);
        verify(ftpClient, times(0)).getStatus(TEST_FTP_FILE);
        verify(ftpClient, times(0)).getSize(TEST_FTP_FILE);
    }
}
