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

import com.simpleftp.ftp.FTPConnectionDetails;
import com.simpleftp.ftp.FTPServer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * This class represents the logical entity that represents a session that has been saved.
 * This class is the equivalent to a Session element in the ftp_session.xsd
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class SavedSession {
    private FTPServer ftpServerDetails;
    private FTPConnectionDetails ftpConnectionDetails;
    private LastSession lastSession;

    public SavedSession(FTPServer ftpServerDetails, FTPConnectionDetails ftpConnectionDetails, LastSession lastSession) {
        this.ftpServerDetails = ftpServerDetails;
        this.ftpConnectionDetails = ftpConnectionDetails;
        this.lastSession = lastSession;
    }

    /**
     * Represents the LastSessionV01 type of the ftp_session.xsd
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class LastSession {
        private String lastRemoteWD;
        private String lastLocalWD;
        private LocalDateTime savedTime;

        /**
         * Constructs a LastSession object with the specified working directories. Initialises savedTime with the current date time
         * @param lastRemoteWD the last working directory the user was in remotely
         * @param lastLocalWD the last working directory the user was in locally
         */
        public LastSession(String lastRemoteWD, String lastLocalWD) {
            this.lastRemoteWD = lastRemoteWD;
            this.lastLocalWD = lastLocalWD;
            this.savedTime = LocalDateTime.now();
        }

        /**
         * Constructs a LastSession object with the specified directories and savedTime
         * @param lastRemoteWD the last working directory the user was in remotely
         * @param lastLocalWD the last working directory the user was in locally
         * @param savedTime the time this session was saved at
         */
        public LastSession(String lastRemoteWD, String lastLocalWD, LocalDateTime savedTime) {
            this.lastRemoteWD = lastRemoteWD;
            this.lastLocalWD = lastLocalWD;
            this.savedTime = savedTime;
        }
    }
}
