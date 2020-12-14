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

package com.simpleftp.ftp.tests.testable;

import com.simpleftp.sessions.Session;
import com.simpleftp.sessions.SessionFile;

/**
 * Extends a SessionFile to allow testing
 */
public class SessionFileTestable extends SessionFile {
    /**
     * Constructs a session file that can be tested
     * @param fileName the name of the file
     */
    public SessionFileTestable(String fileName) {
        super(fileName);
    }

    /**
     * Adds the specified session to this SessionFile
     *
     * @param session the session to add
     */
    @Override
    public void addSession(Session session) {
        super.addSession(session);
    }
}
