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
import com.simpleftp.ftp.connection.Server;
import com.simpleftp.security.PasswordEncryption;
import com.simpleftp.sessions.exceptions.SessionSaveException;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.codehaus.stax2.XMLStreamWriter2;
import org.codehaus.stax2.validation.XMLValidationSchema;
import org.codehaus.stax2.validation.XMLValidationSchemaFactory;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static com.simpleftp.sessions.XMLConstants.*;

/**
 * This class is responsible for saving/writing a SessionFile to an xml file
 */
@NoArgsConstructor
@AllArgsConstructor
public class SessionSaver {
    private XMLStreamWriter2 writer;

    private SessionFile currentFile;

    /**
     * Make a backup of the provided file with the ~ name. The method assumes that file exists
     * @param file the file to backup
     */
    private void makeBackup(File file) throws IOException {
        String absolutePath = file.getAbsolutePath();
        String backupPath = absolutePath + "~";

        File backupFile = new File(backupPath);

        if (backupFile.exists())
            backupFile.delete();

        Files.copy(file.toPath(), backupFile.toPath());
        backupFile.deleteOnExit();
    }

    /**
     * Initialises the writer of this saver to save the provided file.
     * This should be called before save file, or else writer may be null or write the wrong file
     * @param file the session file to save
     * @throws IOException if an error occurs
     * @throws SessionSaveException if can't be initialised
     */
    public void initialiseSaver(SessionFile file) throws IOException, SessionSaveException {
        currentFile = file;
        WstxOutputFactory outputFactory = new WstxOutputFactory();
        outputFactory.configureForSpeed();
        String fileName = file.getFileName();
        File outFile = new File(fileName);

        boolean alreadyExists = outFile.exists();

        if (alreadyExists) {
            // make backup
            makeBackup(outFile);
        }

        try {
            writer = outputFactory.createXMLStreamWriter(new FileWriter(fileName), "UTF-8");
            writer.validateAgainst(getWriterSchema());
        } catch (XMLStreamException ex) {
            Path source = Path.of(fileName + "~");
            if (Files.exists(source)) {
                Files.move(source, Path.of(fileName));
            }
            throw new SessionSaveException("Failed to initialise this SessionSaver instance", ex);
        }
    }

    /**
     * Gets the schema for validating the written XML
     * @return the schema to validate against
     */
    private XMLValidationSchema getWriterSchema() throws XMLStreamException {
        return XMLValidationSchemaFactory.newInstance(XMLValidationSchema.SCHEMA_ID_W3C_SCHEMA)
                .createSchema(ClassLoader.getSystemResource(SESSIONS_SCHEMA));
    }

    /**
     * Allows the default XML Writer created by this class to be overridden with the supplied one
     * @param writer the writer to replace the default one with
     */
    public void setWriter(XMLStreamWriter2 writer) throws XMLStreamException {
        this.writer = writer;
        this.writer.validateAgainst(getWriterSchema());
    }

    /**
     * Writes the Server to the file
     * @param server the server to write
     * @throws XMLStreamException if write fails
     */
    private void writeServer(Server server) throws XMLStreamException {
        String[] tags = {SERVER, SERVER_HOST, SERVER_USER, SERVER_PASSWORD, SERVER_PORT};
        String password = PasswordEncryption.encrypt(server.getPassword());
        String[] values = {server.getServer(), server.getUser(), password, "" + server.getPort()};
        int valuesIndex = 0;

        for (String s : tags) {
            writer.writeStartElement(s);

            if (!s.equals(SERVER)) {
                writer.writeCharacters(values[valuesIndex++]);
                writer.writeEndElement();
            }
        }

        writer.writeEndElement(); //close off Server
    }

    /**
     * Writes the connection details to the file
     * @param ftpConnectionDetails connection details to write
     * @throws XMLStreamException if write fails
     */
    private void writeConnectionDetails(FTPConnectionDetails ftpConnectionDetails) throws XMLStreamException {
        String[] tags = {CONNECTION_DETAILS, CONNECTION_DETAILS_PAGE_SIZE, CONNECTION_DETAILS_TIMEOUT};
        String[] values = {"" + ftpConnectionDetails.getPageSize(), "" + ftpConnectionDetails.getTimeout()};
        int valuesIndex = 0;

        for (String s : tags) {
            writer.writeStartElement(s);

            if (!s.equals(CONNECTION_DETAILS)) {
                writer.writeCharacters(values[valuesIndex++]);
                writer.writeEndElement();
            }
        }

        writer.writeEndElement(); //close off ConnectionDetails
    }

    /**
     * Writes the last session tag to the file
     * @param lastSession the last session element to write
     * @throws XMLStreamException if a write error occurs
     */
    private void writeLastSession(Session.LastSession lastSession) throws XMLStreamException {
        String[] tags = {LAST_SESSION, LAST_REMOTE_WD, LAST_LOCAL_WD};
        String[] values = {lastSession.getLastRemoteWD(), lastSession.getLastLocalWD()};
        int valuesIndex = 0;

        for (String s : tags) {
            writer.writeStartElement(s);

            if (!s.equals(LAST_SESSION)) {
                writer.writeCharacters(values[valuesIndex++]);
                writer.writeEndElement();
            }
        }

        writer.writeEndElement(); //close off LastSession
    }

    /**
     * Writes the start tag of the session element and all the simple tags inside the session element before the first complex elements.
     * This tag should be closed after writing all the complex elements
     * @param session the session to write
     * @throws XMLStreamException if write fails
     */
    private void writeStartSessionElement(Session session) throws XMLStreamException {
        writer.writeStartElement(SESSION);
        writer.writeStartElement(SESSION_ID);
        writer.writeCharacters("" + session.getSessionId());
        writer.writeEndElement();
        writer.writeStartElement(SAVED_TIME);
        writer.writeCharacters(session.getSavedTime().toString());
        writer.writeEndElement();
    }

    /**
     * Writes the specified session to the file
     * @param session the session to write
     * @throws XMLStreamException if write fails
     */
    private void writeSession(Session session) throws XMLStreamException {
        writeStartSessionElement(session);
        writeServer(session.getServerDetails());
        writeConnectionDetails(session.getFtpConnectionDetails());
        writeLastSession(session.getLastSession());
        writer.writeEndElement();
    }

    /**
     * Writes all the sessions to the session files
     * @throws XMLStreamException if a write error occurs
     */
    private void write() throws Exception {
        try {
            writer.writeStartDocument();
            writer.writeStartElement(NAMESPACE_PREFIX, SIMPLE_FTP, SESSION_NAMESPACE);
            writer.writeNamespace(NAMESPACE_PREFIX, SESSION_NAMESPACE);

            for (Session session : currentFile.getSessions()) {
                writeSession(session);
            }

            writer.writeEndDocument();
        } catch (XMLStreamException ex) {
            restoreBackup();
            throw ex;
        }
    }

    /**
     * Writes the SessionFile that was specified in initialiseWriter to an XML file
     * @throws SessionSaveException if an error occurs
     */
    public void writeSessionFile() throws SessionSaveException {
        try {
            write();
        } catch (Exception e) {
            throw new SessionSaveException("An error occurred saving the session identified by file " + currentFile.getFileName(), e);
        }
    }

    /**
     * This restores a backup file created. Should be used if a save() method throws an exception
     * @throws IOException if restore of backup fails
     */
    private void restoreBackup() throws IOException {
        String path = currentFile.getFileName();
        File backup = new File(path + "~");

        if (backup.exists()) {
            Files.move(backup.toPath(), Path.of(path), StandardCopyOption.REPLACE_EXISTING); // restore the backup
        }
    }
}
