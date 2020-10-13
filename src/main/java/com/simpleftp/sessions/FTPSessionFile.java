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

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This represents a file which can contain 1 or more saved sessions.
 * This class is transformed into an XML file by the SessionSaver class
 */
@EqualsAndHashCode
public class FTPSessionFile {
    @Getter
    private String fileName;
    private List<SavedSession> savedSessions;

    /**
     * Constructs a empty FTPSessionFile with the specified name
     * @param fileName the name of this file. Equals to the name if written on disk
     */
    public FTPSessionFile(String fileName) {
        this.fileName = fileName;
        this.savedSessions = new ArrayList<>();
    }

    /**
     * Constructs a empty FTPSessionFile with the specified name and SavedSessions
     * @param fileName the name of this file. Equals to the name if written on disk
     * @param  savedSessions the list of SavedSessions to initialise this file with
     */
    public FTPSessionFile(String fileName, List<SavedSession> savedSessions) {
        this.fileName = fileName;
        this.savedSessions = new ArrayList<>(savedSessions);
    }

    /**
     * Adds the specified session to this FTPSessionFile
     * @param savedSession the session to add
     */
    public void addSavedSession(SavedSession savedSession) {
        savedSessions.add(savedSession);
    }

    /**
     * Removes the specified saved session from this file
     * @param savedSession the session to remove
     */
    public void removeSavedSession(SavedSession savedSession) {
        savedSessions.remove(savedSession);
    }

    /**
     * @return an unmodifiable list of the sessions saved by this FTPSessionFile
     */
    public List<SavedSession> getSavedSessions() {
        return Collections.unmodifiableList(savedSessions);
    }

    /**
     * Sets the saved sessions list for this file
     * @param savedSessions the list of sessions
     */
    public void setSavedSessions(List<SavedSession> savedSessions) {
        this.savedSessions = savedSessions;
    }

    /**
     * Returns the number of saved sessions in this file
     * @return number of saved sessions
     */
    public int getNumberOfSavedSessions() {
        return savedSessions.size();
    }
}
