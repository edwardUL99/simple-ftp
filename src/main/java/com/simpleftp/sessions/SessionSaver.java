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
import com.simpleftp.ftp.FTPConnectionDetails;
import com.simpleftp.ftp.FTPServer;
import org.codehaus.stax2.XMLStreamWriter2;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This class is responsible for saving/writing a FTPSessionFile to an xml file
 */
public class SessionSaver {
    private XMLStreamWriter writer;

    private void initialiseWriter(String fileName) throws IOException, FileNotFoundException, XMLStreamException {
        WstxOutputFactory outputFactory = new WstxOutputFactory();
        outputFactory.configureForSpeed();
        File file = new File(fileName);
        file.createNewFile();
        writer = outputFactory.createXMLStreamWriter(new FileOutputStream(fileName));
    }

    private void writeFTPServer(FTPServer ftpServer) throws XMLStreamException {
        String[] tags = {"FTPServer", "Host", "User", "Port"};
        String[] values = {ftpServer.getServer(), ftpServer.getUser(), "" + ftpServer.getPort()};
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

    private void write(FTPSessionFile file) throws XMLStreamException {
        writer.writeStartDocument();
        writer.writeStartElement("urn", "SimpleFTP", "urn:simple:ftp");
        writer.writeNamespace("urn", "urn:simple:ftp");

        for (SavedSession session : file.getSavedSessions()) {
            writer.writeStartElement("Session");
            writeFTPServer(session.getFtpServerDetails());
            writeConnectionDetails(session.getFtpConnectionDetails());
            writeLastSession(session.getLastSession());
            writer.writeEndElement();
        }

        writer.writeEndDocument();
    }

    /**
     * Writes the FTPSessionFile to an XML file
     * @param file the file to write
     * @throws SessionSaveException if an error occurs
     */
    public void writeSessionFile(FTPSessionFile file) throws SessionSaveException {
        try {
            initialiseWriter(file.getFileName());
            write(file);
        } catch (Exception e) {
            e.printStackTrace();
            throw new SessionSaveException("An error occurred saving the session identified by file " + file.getFileName(), e);
        }
    }
}
