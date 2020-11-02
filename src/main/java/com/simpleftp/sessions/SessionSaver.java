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

import com.ctc.wstx.stax.WstxOutputFactory;
import com.simpleftp.ftp.connection.FTPConnectionDetails;
import com.simpleftp.ftp.connection.FTPServer;
import com.simpleftp.security.PasswordEncryption;
import com.simpleftp.sessions.exceptions.SessionSaveException;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This class is responsible for saving/writing a FTPSessionFile to an xml file
 */
@NoArgsConstructor
@AllArgsConstructor
public class SessionSaver {
    private XMLStreamWriter writer;

    private FTPSessionFile currentFile;

    /**
     * Initialises the writer of this saver to save the provided file.
     * This should be called before save file, or else writer may be null or write the wrong file
     * @param file the session file to save
     * @throws IOException if an error occurs
     * @throws XMLStreamException if an error occurs creating the writer
     */
    public void initialiseWriter(FTPSessionFile file) throws IOException, XMLStreamException {
        currentFile = file;
        WstxOutputFactory outputFactory = new WstxOutputFactory();
        outputFactory.configureForSpeed();
        File outFile = new File(file.getFileName());
        outFile.createNewFile();
        writer = outputFactory.createXMLStreamWriter(new FileOutputStream(outFile));
    }

    /**
     * Allows the default XML Writer created by this class to be overridden with the supplied one
     * @param writer the writer to replace the default one with
     */
    public void setWriter(XMLStreamWriter writer) {
        this.writer = writer;
    }

    private void writeFTPServer(FTPServer ftpServer) throws XMLStreamException {
        String[] tags = {"FTPServer", "Host", "User", "Password", "Port"};
        String password = PasswordEncryption.encrypt(ftpServer.getPassword());
        String[] values = {ftpServer.getServer(), ftpServer.getUser(), password, "" + ftpServer.getPort()};
        int valuesIndex = 0;

        for (String s : tags) {
            writer.writeStartElement(s);

            if (!s.equals("FTPServer")) {
                writer.writeCharacters(values[valuesIndex++]);
                writer.writeEndElement();
            }
        }

        writer.writeEndElement(); //close off FTPServer
    }

    private void writeConnectionDetails(FTPConnectionDetails ftpConnectionDetails) throws XMLStreamException {
        String[] tags = {"ConnectionDetails", "PageSize", "Timeout"};
        String[] values = {"" + ftpConnectionDetails.getPageSize(), "" + ftpConnectionDetails.getTimeout()};
        int valuesIndex = 0;

        for (String s : tags) {
            writer.writeStartElement(s);

            if (!s.equals("ConnectionDetails")) {
                writer.writeCharacters(values[valuesIndex++]);
                writer.writeEndElement();
            }
        }

        writer.writeEndElement(); //close off ConnectionDetails
    }

    private void writeLastSession(SavedSession.LastSession lastSession) throws XMLStreamException {
        String[] tags = {"LastSession", "LastRemoteWD", "LastLocalWD", "SavedTime"};
        String[] values = {lastSession.getLastRemoteWD(), lastSession.getLastLocalWD(), lastSession.getSavedTime().toString()};
        int valuesIndex = 0;

        for (String s : tags) {
            writer.writeStartElement(s);

            if (!s.equals("LastSession")) {
                writer.writeCharacters(values[valuesIndex++]);
                writer.writeEndElement();
            }
        }

        writer.writeEndElement(); //close off LastSession
    }

    private void write() throws XMLStreamException {
        writer.writeStartDocument();
        writer.writeStartElement("urn", "SimpleFTP", "urn:simple:ftp");
        writer.writeNamespace("urn", "urn:simple:ftp");

        for (SavedSession session : currentFile.getSavedSessions()) {
            writer.writeStartElement("Session");
            writer.writeStartElement("Id");
            writer.writeCharacters(session.getSessionId());
            writer.writeEndElement();
            writeFTPServer(session.getFtpServerDetails());
            writeConnectionDetails(session.getFtpConnectionDetails());
            writeLastSession(session.getLastSession());
            writer.writeEndElement();
        }

        writer.writeEndDocument();
    }

    /**
     * Writes the FTPSessionFile that was specified in initialiseWriter to an XML file
     * @throws SessionSaveException if an error occurs
     */
    public void writeSessionFile() throws SessionSaveException {
        try {
            write();
        } catch (Exception e) {
            throw new SessionSaveException("An error occurred saving the session identified by file " + currentFile.getFileName(), e);
        }
    }
}
