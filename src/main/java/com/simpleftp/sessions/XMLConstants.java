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

/**
 * This class provides names of the tags that are used in sessions.xsd.
 * This allows easy maintainability in case names change, you only have to change one value
 */
public final class XMLConstants {
    /**
     * The root node of the XML document
     */
    public static final String SIMPLE_FTP = "SimpleFTP";
    /**
     * The tag representing a saved session
     */
    public static final String SESSION = "Session";
    /**
     * The tag representing a session id
     */
    public static final String SESSION_ID = "Id";
    /**
     * The tag representing server details
     */
    public static final String SERVER = "Server";
    /**
     * The tag representing a Server host
     */
    public static final String SERVER_HOST = "Host";
    /**
     * The tag representing a Server username
     */
    public static final String SERVER_USER = "User";
    /**
     * The tag representing a Server password
     */
    public static final String SERVER_PASSWORD = "Password";
    /**
     * The tag representing a Server port
     */
    public static final String SERVER_PORT = "Port";
    /**
     * The tag representing Connection Details
     */
    public static final String CONNECTION_DETAILS = "ConnectionDetails";
    /**
     * The tag representing Connection Details page size
     */
    public static final String CONNECTION_DETAILS_PAGE_SIZE = "PageSize";
    /**
     * The tag representing Connection Details timeout
     */
    public static final String CONNECTION_DETAILS_TIMEOUT = "Timeout";
    /**
     * The tag representing Last Session details
     */
    public static final String LAST_SESSION = "LastSession";
    /**
     * The tag representing Last remote working directory
     */
    public static final String LAST_REMOTE_WD = "LastRemoteWD";
    /**
     * The tag representing Last local working directory
     */
    public static final String LAST_LOCAL_WD = "LastLocalWD";
    /**
     * The tag representing the time this session was saved
     */
    public static final String SAVED_TIME = "SavedTime";
    /**
     * The namespace for the session
     */
    public static final String SESSION_NAMESPACE = "urn:simple:ftp:sessions";
    /**
     * The prefix for the namespace
     */
    public static final String NAMESPACE_PREFIX = "urn";
    /**
     * The schema for the Sessions saving XML
     */
    public static final String SESSIONS_SCHEMA = "sessions.xsd";
}
