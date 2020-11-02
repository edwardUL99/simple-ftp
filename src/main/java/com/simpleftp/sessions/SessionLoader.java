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
import com.simpleftp.ftp.connection.FTPConnectionDetails;
import com.simpleftp.ftp.connection.FTPServer;
import com.simpleftp.security.PasswordEncryption;
import com.simpleftp.sessions.exceptions.SessionLoadException;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.codehaus.stax2.XMLStreamReader2;

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
     * @throws XMLStreamException if an error occurs
     */
    public void initialiseReader(String fileName) throws XMLStreamException {
        WstxInputFactory inputFactory = new WstxInputFactory();
        inputFactory.configureForXmlConformance();
        reader = inputFactory.createXMLStreamReader(new File(fileName));
        currentFile = fileName;
    }

    /**
     * Allows the reader for this class to be overridden
     * @param streamReader2 the read to override the default
     */
    public void setReader(XMLStreamReader2 streamReader2) {
        this.reader = streamReader2;
    }

    private boolean isText(String text) {
        return (!text.contains("\n") && !text.contains("\r")) || !text.replaceAll("[\\n\\r]+", "").trim().equals("");
    }

    private FTPServer readFTPServer() throws XMLStreamException {
        FTPServer server = new FTPServer();
        String tag = "";

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLEvent.START_ELEMENT) {
                tag = reader.getLocalName();
            } else if (event == XMLEvent.CHARACTERS) {
                String text = reader.getText();
                if (isText(text)) {
                    if (tag.equals(FTP_SERVER_HOST)) {
                        server.setServer(text);
                    } else if (tag.equals(FTP_SERVER_USER)) {
                        server.setUser(text);
                    } else if (tag.equals(FTP_SERVER_PASSWORD)) {
                        server.setPassword(PasswordEncryption.decrypt(text));
                    } else if (tag.equals(FTP_SERVER_PORT)) {
                        server.setPort(Integer.parseInt(text));
                    }
                }
            } else if (event == XMLEvent.END_ELEMENT) {
                if (reader.getLocalName().equals(FTP_SERVER))
                    break; // you've reached end of this tag
                tag = "";
            }
        }

        return server;
    }

    private FTPConnectionDetails readConnectionDetails() throws XMLStreamException {
        FTPConnectionDetails connectionDetails = new FTPConnectionDetails();
        String tag = "";

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLEvent.START_ELEMENT) {
                tag = reader.getLocalName();
            } else if (event == XMLEvent.CHARACTERS) {
                String text = reader.getText();
                if (isText(text)) {
                    if (tag.equals(CONNECTION_DETAILS_PAGE_SIZE)) {
                        connectionDetails.setPageSize(Integer.parseInt(text));
                    } else if (tag.equals(CONNECTION_DETAILS_TIMEOUT)) {
                        connectionDetails.setTimeout(Integer.parseInt(text));
                    }
                }
            } else if (event == XMLEvent.END_ELEMENT) {
                if (reader.getLocalName().equals(CONNECTION_DETAILS))
                    break; // you've reached end of this tag
                tag = "";
            }
        }

        return connectionDetails;
    }

    private SavedSession.LastSession readLastSession() throws XMLStreamException {
        SavedSession.LastSession lastSession = new SavedSession.LastSession();
        String tag = "";

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLEvent.START_ELEMENT) {
                tag = reader.getLocalName();
            } else if (event == XMLEvent.CHARACTERS) {
                String text = reader.getText();
                if (isText(text)) {
                    if (tag.equals(LAST_REMOTE_WD)) {
                        lastSession.setLastRemoteWD(text);
                    } else if (tag.equals(LAST_LOCAL_WD)) {
                        lastSession.setLastLocalWD(text);
                    } else if (tag.equals(SAVED_TIME)) {
                        lastSession.setSavedTime(LocalDateTime.parse(text));
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

    private FTPSessionFile load(String fileName) throws XMLStreamException {
        FTPSessionFile sessionFile = new FTPSessionFile(fileName);
        SavedSession savedSession = null;
        boolean setId = false;

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLEvent.START_ELEMENT) {
                String elementName = reader.getLocalName();

                if (elementName.equals(SESSION)) {
                    savedSession = new SavedSession();
                } else if (elementName.equals(SESSION_ID)) {
                    setId = true;
                } else if (elementName.equals(FTP_SERVER)) {
                    savedSession.setFtpServerDetails(readFTPServer());
                } else if (elementName.equals(CONNECTION_DETAILS)) {
                    savedSession.setFtpConnectionDetails(readConnectionDetails());
                } else if (elementName.equals(LAST_SESSION)) {
                    savedSession.setLastSession(readLastSession());
                }
            } else if (event == XMLEvent.CHARACTERS) {
                if (setId) {
                    savedSession.setSessionId(reader.getText());
                    setId = false;
                }
            } else if (event == XMLEvent.END_ELEMENT) {
                String elementName = reader.getLocalName();

                if (elementName.equals(SESSION)) {
                    if (savedSession != null) {
                        sessionFile.addSavedSession(savedSession);
                        savedSession = null;
                    }
                }
            }
        }

        return sessionFile;
    }

    /**
     * Loads the file specified by the fileName in initialiseReader into a FTPSessionFile.
     * It is undefined if the XML file specified by filename is not valid
     * @return FTPSessionFile with loaded in sessions
     * @throws SessionLoadException if an error occurs
     */
    public FTPSessionFile loadFile() throws SessionLoadException {
        try {
            return load(currentFile);
        } catch (XMLStreamException e) {
            throw new SessionLoadException("An error occurred loading file " + currentFile, e);
        }
    }
}
