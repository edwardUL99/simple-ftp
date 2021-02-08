/*
 *  Copyright (C) 2020-2021 Edward Lynch-Milner
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
    /**
     * Server details for this Session
     */
    private Server serverDetails;
    /**
     * Details relating to the last session
     */
    private LastSession lastSession;
    /**
     * The time this was last saved
     */
    private LocalDateTime savedTime;
    /**
     * This Session has just bene created and needs to be saved to a SessionFile.
     * This SessionFile represents the SessionFile it should be added to. This should be set after creating the session
     */
    SessionFile dirtyFile;

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
            if (dirtyFile != null && sessionFile != dirtyFile) {
                throw new UnsupportedOperationException("This Session is set to be saved to a SessionFile that is not the Session's SessionFile instance");
            }

            if (dirtyFile != null)
                sessionFile.addSession(this);
            dirtyFile = null;
            setSavedTime(LocalDateTime.now());
            sessionSaver.initialiseSaver(sessionFile);
            sessionSaver.writeSessionFile();
        } catch (IOException ex) {
            throw new SessionSaveException("Failed to save the Session", ex);
        }
    }
}
