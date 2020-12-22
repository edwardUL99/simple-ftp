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

import com.simpleftp.ftp.connection.Server;
import com.simpleftp.sessions.exceptions.SessionSaveException;
import lombok.*;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * This class represents the logical entity that represents a session that has been saved.
 * This class is the equivalent to a Session element in the sessions.xsd.
 * You cannot create this class outside this package as that can cause conflicts with IDs etc.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = {"sessionId"})
public class Session {
    /**
     * A unique ID for this session
     */
    private int sessionId;
    private Server serverDetails;
    private LastSession lastSession;
    private LocalDateTime savedTime;

    /**
     * Constructs a Session with the provided parameters
     * @param sessionId the session id to use for this session
     * @param serverDetails the server details to use
     * @param lastSession the last session object
     */
    protected Session(int sessionId, Server serverDetails, LastSession lastSession) {
        this.sessionId = sessionId;
        this.serverDetails = serverDetails;
        this.lastSession = lastSession;
        this.savedTime = LocalDateTime.now();
    }

    /**
     * Represents the LastSessionV01 type of the sessions.xsd.
     * This represents the state of the session (e.g. last working directories) before the last save. This last save may be when quitting the application, so would
     * be a good place to cat the last working directories
     */
    @Getter
    @Setter
    @EqualsAndHashCode
    public static class LastSession {
        private String lastRemoteWD;
        private String lastLocalWD;

        /**
         * Constructs a default LastSession
         */
        public LastSession() {
            lastLocalWD = System.getProperty("user.home");
            lastRemoteWD = "/";
        }

        /**
         * Constructs a LastSession object with the specified working directories. Initialises savedTime with the current date time
         * @param lastRemoteWD the last working directory the user was in remotely
         * @param lastLocalWD the last working directory the user was in locally
         */
        public LastSession(String lastRemoteWD, String lastLocalWD) {
            this.lastRemoteWD = lastRemoteWD;
            this.lastLocalWD = lastLocalWD;
        }
    }

    /**
     * Saves this session to the session file
     * @throws SessionSaveException if save fails
     * @throws UnsupportedOperationException if this Session is not a part of the SessionFile
     */
    public void save() throws SessionSaveException {
        SessionSaver sessionSaver = Sessions.getSessionSaver();

        try {
            SessionFile sessionFile = Sessions.getSessionFile();
            if (!sessionFile.getSessions().contains(this)) {
                throw new UnsupportedOperationException("Cannot save a Session that is not in the SessionFile");
            }

            setSavedTime(LocalDateTime.now());
            sessionSaver.initialiseSaver(sessionFile);
            sessionSaver.writeSessionFile();
        } catch (IOException ex) {
            throw new SessionSaveException("Failed to save the Session", ex);
        }
    }

    /**
     * Deletes this session from the session file
     * @throws SessionSaveException if an error occurs saving the file
     */
    public void delete() throws SessionSaveException {
        Sessions.getSessionFile().removeSession(this);
        save();
    }
}
