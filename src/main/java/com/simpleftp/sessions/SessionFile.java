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

import java.util.*;

/**
 * This represents a file which can contain 1 or more saved sessions.
 * This class is transformed into an XML file by the SessionSaver class
 */
@EqualsAndHashCode
public class SessionFile {
    @Getter
    private final String fileName;
    /**
     * The sessions stored in this file
     */
    private final Set<Session> sessions;
    /**
     * Comparator to sort the set based on ID
     */
    private static final Comparator<Session> SET_COMPARATOR = (o1, o2) -> o1.getSessionId() - o2.getSessionId();
    /**
     * A set to keep track of session ids so we can validate uniqueness quickly
     */
    private final Set<Integer> addedIDs;

    /**
     * Constructs a empty SessionFile with the specified name
     * @param fileName the name of this file. Equals to the name if written on disk
     */
    protected SessionFile(String fileName) {
        this.fileName = fileName;
        this.sessions = new TreeSet<>(SET_COMPARATOR);
        this.addedIDs = new TreeSet<>();
    }

    /**
     * Validates the uniqueness of our ID
     * @param id the id to validate
     */
    private void validateIDIntegrity(int id) {
        if (addedIDs.contains(id))
            throw new IllegalArgumentException("Session ID uniqueness integrity violated. Duplicate ID: " + id);
    }

    /**
     * Adds the specified session to this SessionFile
     * @param session the session to add
     * @throws IllegalArgumentException if a duplicate ID is found
     */
    protected void addSession(Session session) {
        int id = session.getSessionId();
        validateIDIntegrity(id);

        addedIDs.add(id);
        sessions.add(session);
    }

    /**
     * Removes the specified session from this file
     * @param session the session to remove
     */
    protected void removeSession(Session session) {
        sessions.remove(session);
        addedIDs.remove(session.getSessionId());
    }

    /**
     * @return an unmodifiable list of the sessions saved by this SessionFile
     */
    public Set<Session> getSessions() {
        return Collections.unmodifiableSet(sessions);
    }

    /**
     * Returns the number of saved sessions in this file
     * @return number of saved sessions
     */
    public int getNumberOfSavedSessions() {
        return sessions.size();
    }
}
