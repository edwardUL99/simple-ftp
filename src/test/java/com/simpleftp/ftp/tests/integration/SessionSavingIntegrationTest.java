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

import com.simpleftp.ftp.connection.Server;
import com.simpleftp.ftp.tests.testable.*;
import com.simpleftp.sessions.Session;
import com.simpleftp.sessions.SessionFile;
import com.simpleftp.sessions.SessionLoader;
import com.simpleftp.sessions.SessionSaver;
import com.simpleftp.sessions.exceptions.SessionLoadException;
import com.simpleftp.sessions.exceptions.SessionSaveException;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.XMLStreamWriter2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;

/**
 * It's hard to unit test this, so this test should be sufficient
 */
public class SessionSavingIntegrationTest {
    @Mock
    private XMLStreamWriter2 streamWriter;

    private SessionSaver sessionSaver;

    @InjectMocks
    private SessionSaver mockedSaver;

    private SessionLoader sessionLoader;

    @Mock
    private XMLStreamReader2 streamReader2;

    @InjectMocks
    private SessionLoader mockedLoader;

    private static final String FILE_NAME = "test.xml";

    private static final int TEST_ID = 1;

    private AutoCloseable autoCloseable;

    @BeforeEach
    void init() {
        sessionSaver = new SessionSaverTestable();
        sessionLoader = new SessionLoaderTestable();
        autoCloseable = MockitoAnnotations.openMocks(this);
        FTPSystemTestable.setSystemTesting(true);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    private SessionFileTestable getFTPSessionFile() {
        SessionFileTestable file = new SessionFileTestable(FILE_NAME);
        Session.LastSession lastSession = new Session.LastSession("/last/remote/dir", "/last/local/dir");
        Server server = new Server("ftp.server.com", "user", "Tester", Server.DEFAULT_FTP_PORT, 200);
        Session session = new SessionTestable(TEST_ID, server, lastSession);
        file.addSession(session);

        return file;
    }

    @Test
    void shouldWriteFTPSessionFileSuccessfully() throws IOException, XMLStreamException, SessionSaveException {
        SessionFile file = getFTPSessionFile();
        sessionSaver.initialiseSaver(file);
        assertDoesNotThrow(() -> sessionSaver.writeSessionFile());
        File savedFile = new File(FILE_NAME);
        assertTrue(savedFile.exists());
        savedFile.delete();
    }

    @Test
    void shouldThrowExceptionIfStreamErrorOccursOnWritingFile() throws XMLStreamException, IOException, SessionSaveException {
        SessionFile file = getFTPSessionFile();
        doThrow(XMLStreamException.class).when(streamWriter).writeStartDocument();

        mockedSaver.initialiseSaver(file);
        mockedSaver.setWriter(streamWriter);

        assertThrows(SessionSaveException.class, () -> mockedSaver.writeSessionFile());
        new File(FILE_NAME).delete();
    }

    @Test
    void shouldLoadFTPSessionFileSuccessfully() throws XMLStreamException, SessionLoadException {
        SessionFile file = getFTPSessionFile();
        assertDoesNotThrow(() -> {
            sessionSaver.initialiseSaver(file);
            sessionSaver.writeSessionFile();
        });

        sessionLoader.initialiseLoader(FILE_NAME);
        SessionFile loadedFile = sessionLoader.loadFile();

        assertEquals(file, loadedFile);
        new File(file.getFileName()).delete();
    }

    @Test
    void shouldThrowIfFTPSessionFileThrowsException() throws XMLStreamException, SessionLoadException {
        doThrow(XMLStreamException.class).when(streamReader2).hasNext();

        assertDoesNotThrow(() -> {
            sessionSaver.initialiseSaver(getFTPSessionFile());
            sessionSaver.writeSessionFile();
        });

        mockedLoader.initialiseLoader(FILE_NAME);
        assertThrows(SessionLoadException.class, () -> {
            mockedLoader.setReader(streamReader2);
            mockedLoader.loadFile();
        });

        new File(FILE_NAME).delete();
    }
}
