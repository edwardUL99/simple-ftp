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

import com.simpleftp.ftp.FTPConnectionDetails;
import com.simpleftp.ftp.FTPServer;
import com.simpleftp.sessions.*;
import com.simpleftp.sessions.exceptions.SessionLoadException;
import com.simpleftp.sessions.exceptions.SessionSaveException;
import org.codehaus.stax2.XMLStreamReader2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;

/**
 * It's hard to unit test this, so this test should be sufficient
 */
public class SessionSavingIntegrationTest {
    @Mock
    private XMLStreamWriter streamWriter;

    private SessionSaver sessionSaver;

    @InjectMocks
    private SessionSaver mockedSaver;

    private SessionLoader sessionLoader;

    @Mock
    private XMLStreamReader2 streamReader2;

    @InjectMocks
    private SessionLoader mockedLoader;

    private static final String FILE_NAME = "test.xml";

    private static final String TEST_ID = "1234-TEST-5678";

    private AutoCloseable autoCloseable;

    @BeforeEach
    void init() {
        sessionSaver = new SessionSaver();
        sessionLoader = new SessionLoader();
        autoCloseable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    private FTPSessionFile getFTPSessionFile() {
        FTPSessionFile file = new FTPSessionFile(FILE_NAME);
        SavedSession.LastSession lastSession = new SavedSession.LastSession("/last/remote/dir", "/last/local/dir");
        FTPServer server = new FTPServer("ftp.server.com", "user", "Tester", FTPServer.DEFAULT_PORT);
        FTPConnectionDetails connectionDetails = new FTPConnectionDetails(2, 100);
        SavedSession savedSession = new SavedSession(TEST_ID, server, connectionDetails, lastSession);
        file.addSavedSession(savedSession);

        return file;
    }

    @Test
    void shouldWriteFTPSessionFileSuccessfully() throws IOException, XMLStreamException {
        FTPSessionFile file = getFTPSessionFile();
        sessionSaver.initialiseWriter(file);
        assertDoesNotThrow(() -> sessionSaver.writeSessionFile());
        File savedFile = new File(FILE_NAME);
        assertTrue(savedFile.exists());
        savedFile.delete();
    }

    @Test
    void shouldThrowExceptionIfStreamErrorOccursOnWritingFile() throws XMLStreamException, IOException {
        FTPSessionFile file = getFTPSessionFile();
        doThrow(XMLStreamException.class).when(streamWriter).writeStartDocument();

        mockedSaver.initialiseWriter(file);
        mockedSaver.setWriter(streamWriter);

        assertThrows(SessionSaveException.class, () -> mockedSaver.writeSessionFile());
        new File(FILE_NAME).delete();
    }

    @Test
    void shouldLoadFTPSessionFileSuccessfully() throws SessionLoadException, XMLStreamException {
        FTPSessionFile file = getFTPSessionFile();
        assertDoesNotThrow(() -> {
            sessionSaver.initialiseWriter(file);
            sessionSaver.writeSessionFile();
        });

        sessionLoader.initialiseReader(FILE_NAME);
        FTPSessionFile loadedFile = sessionLoader.loadFile();

        assertEquals(file, loadedFile);
        new File(file.getFileName()).delete();
    }

    @Test
    void shouldThrowIfFTPSessionFileThrowsException() throws XMLStreamException {
        doThrow(XMLStreamException.class).when(streamReader2).hasNext();

        assertDoesNotThrow(() -> {
            sessionSaver.initialiseWriter(getFTPSessionFile());
            sessionSaver.writeSessionFile();
        });

        mockedLoader.initialiseReader(FILE_NAME);
        assertThrows(SessionLoadException.class, () -> {
            mockedLoader.setReader(streamReader2);
            mockedLoader.loadFile();
        });

        new File(FILE_NAME).delete();
    }
}
