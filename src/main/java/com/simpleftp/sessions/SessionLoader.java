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

package com.simpleftp.sessions;

import static com.simpleftp.sessions.XMLConstants.*;
import com.ctc.wstx.stax.WstxInputFactory;
import com.simpleftp.ftp.connection.Server;
import com.simpleftp.security.PasswordEncryption;
import com.simpleftp.sessions.exceptions.SessionLoadException;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.validation.XMLValidationSchema;
import org.codehaus.stax2.validation.XMLValidationSchemaFactory;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.time.LocalDateTime;

/**
 * This class loads in saved sessions from a specified xml file
 */
@NoArgsConstructor
@AllArgsConstructor
public class SessionLoader {
    private XMLStreamReader2 reader;
    private String currentFile;

    /**
     * Initialises the loader's reader instance with the specified file name
     * @param fileName the name of the file to read
     * @throws SessionLoadException if an error occurs
     */
    public void initialiseLoader(String fileName) throws SessionLoadException {
        try {
            WstxInputFactory inputFactory = new WstxInputFactory();
            inputFactory.configureForXmlConformance();
            reader = inputFactory.createXMLStreamReader(new File(fileName));
            reader.validateAgainst(getValidationSchema());
            currentFile = fileName;
        } catch (XMLStreamException ex) {
            throw new SessionLoadException("Failed to initialise the SessionLoader", ex);
        }
    }

    /**
     * Gets the schema to validate the file against
     * @return schema to validate against
     */
    private XMLValidationSchema getValidationSchema() throws XMLStreamException {
        return XMLValidationSchemaFactory.newInstance(XMLValidationSchema.SCHEMA_ID_W3C_SCHEMA)
                .createSchema(ClassLoader.getSystemResource(SESSIONS_SCHEMA));
    }

    /**
     * Allows the reader for this class to be overridden
     * @param streamReader2 the read to override the default
     */
    public void setReader(XMLStreamReader2 streamReader2) throws XMLStreamException {
        this.reader = streamReader2;
        this.reader.validateAgainst(getValidationSchema());
    }

    /**
     * Checks if the provided text is in fact text and not just new line characters
     * @param text the text to check
     * @return true if the text is text of interest
     */
    private boolean isText(String text) {
        return (!text.contains("\n") && !text.contains("\r")) || !text.replaceAll("[\\n\\r]+", "").trim().equals("");
    }

    /**
     * Reads in the next server element from the session file
     * @return the read in server object
     * @throws XMLStreamException if read fails
     */
    private Server readServer() throws XMLStreamException {
        Server server = new Server();
        String tag = "";

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLEvent.START_ELEMENT) {
                tag = reader.getLocalName();
            } else if (event == XMLEvent.CHARACTERS) {
                String text = reader.getText();
                if (isText(text)) {
                    switch (tag) {
                        case SERVER_HOST:
                            server.setServer(text);
                            break;
                        case SERVER_USER:
                            server.setUser(text);
                            break;
                        case SERVER_PASSWORD:
                            server.setPassword(PasswordEncryption.decrypt(text));
                            break;
                        case SERVER_PORT:
                            server.setPort(Integer.parseInt(text));
                            break;
                        case SERVER_TIMEOUT:
                            server.setTimeout(Integer.parseInt(text));
                            break;
                    }
                }
            } else if (event == XMLEvent.END_ELEMENT) {
                if (reader.getLocalName().equals(SERVER))
                    break; // you've reached end of this tag
                tag = "";
            }
        }

        return server;
    }

    /**
     * Reads the next last session from the file
     * @return the read in last session
     * @throws XMLStreamException if read fails
     */
    private Session.LastSession readLastSession() throws XMLStreamException {
        Session.LastSession lastSession = new Session.LastSession();
        String tag = "";

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLEvent.START_ELEMENT) {
                tag = reader.getLocalName();
            } else if (event == XMLEvent.CHARACTERS) {
                String text = reader.getText();
                if (isText(text)) {
                    switch (tag) {
                        case LAST_REMOTE_WD:
                            lastSession.setLastRemoteWD(text);
                            break;
                        case LAST_LOCAL_WD:
                            lastSession.setLastLocalWD(text);
                            break;
                    }
                }
            } else if (event == XMLEvent.END_ELEMENT) {
                if (reader.getLocalName().equals(LAST_SESSION))
                    break; // you've reached end of this tag
                tag = "";
            }
        }

        return lastSession;
    }

    /**
     * Loads in the file session by session and returns it as the in memory SessionFile
     * @param fileName the name of the session file on disk
     * @return the loaded in session file
     * @throws XMLStreamException if load fails
     */
    private SessionFile load(String fileName) throws XMLStreamException {
        SessionFile sessionFile = new SessionFile(fileName);
        Session session = null;
        boolean setId = false;
        boolean setSaveTime = false;

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLEvent.START_ELEMENT) {
                String elementName = reader.getLocalName();

                switch (elementName) {
                    case SESSION:
                        session = new Session();
                        break;
                    case SESSION_ID:
                        setId = true;
                        break;
                    case SAVED_TIME:
                        setSaveTime = true;
                        break;
                    case SERVER:
                        session.setServerDetails(readServer());
                        break;
                    case LAST_SESSION:
                        session.setLastSession(readLastSession());
                        break;
                }
            } else if (event == XMLEvent.CHARACTERS) {
                String text = reader.getText();

                if (isText(text)) {
                    if (setId) {
                        session.setSessionId(Integer.parseInt(reader.getText()));
                        setId = false;
                    } else if (setSaveTime) {
                        session.setSavedTime(LocalDateTime.parse(reader.getText()));
                        setSaveTime = false;
                    }
                }
            } else if (event == XMLEvent.END_ELEMENT) {
                String elementName = reader.getLocalName();

                if (elementName.equals(SESSION)) {
                    if (session != null) {
                        sessionFile.addSession(session);
                        session = null;
                    }
                }
            }
        }

        return sessionFile;
    }

    /**
     * Loads the file specified by the fileName in initialiseReader into a SessionFile.
     * It is undefined if the XML file specified by filename is not valid
     * @return SessionFile with loaded in sessions
     * @throws SessionLoadException if an error occurs
     */
    public SessionFile loadFile() throws SessionLoadException {
        try {
            return load(currentFile);
        } catch (XMLStreamException e) {
            throw new SessionLoadException("An error occurred loading file " + currentFile, e);
        }
    }
}
