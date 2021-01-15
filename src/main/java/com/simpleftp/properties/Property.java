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

package com.simpleftp.properties;

import lombok.Getter;

/**
 * This class represents a possible property
 */
public class Property {
    /**
     * This enum represents the type of the enum. Whenever adding a new property and type,
     * add it to this enum as the type is used by Properties class to validate the value
     */
    enum Type {
        /**
         * The property is an Integer
         */
        INTEGER,
        /**
         * The property is a true/false value
         */
        BOOLEAN
    }

    /**
     * This represents how often the connection monitor should check if the connection is still active
     */
    public static final Property CONNECTION_MONITOR_INTERVAL = new Property("CONNECTION_MONITOR_INTERVAL", Type.INTEGER);

    /**
     * This defines the initial height of the file editor
     */
    public static final Property FILE_EDITOR_HEIGHT = new Property("FILE_EDITOR_HEIGHT", Type.INTEGER);

    /**
     * This defines the initial width of the file editor
     */
    public static final Property FILE_EDITOR_WIDTH = new Property("FILE_EDITOR_WIDTH", Type.INTEGER);

    /**
     * If true, the file size retrieved by CommonFile.getSize() is that of the target, not the link
     */
    public static final Property FILE_SIZE_FOLLOW_LINK = new Property("FILE_SIZE_FOLLOW_LINK", Type.BOOLEAN);

    /**
     * If true, the permissions retrieved by CommonFile.getPermissions() is that of the target, not the link
     */
    public static final Property FILE_PERMS_FOLLOW_LINK = new Property("FILE_PERMS_FOLLOW_LINK", Type.BOOLEAN);

    /**
     * If true, FTPConnection.getModificationTime(path) is attempted, else (or if this can't be determined), it is the time of the FTPFile returned
     */
    public static final Property SERVER_REMOTE_MODIFICATION_TIME = new Property("SERVER_REMOTE_MODIFICATION_TIME", Type.BOOLEAN);

    /**
     * If true, LineEntries on a RemoteDirectoryPane will be cached up until the first refresh() method call
     */
    public static final Property CACHE_REMOTE_DIRECTORY_LISTING = new Property("CACHE_REMOTE_DIRECTORY_LISTING", Type.BOOLEAN);

    /**
     * If true, all cached LineEntries are removed when DirectoryPane.refresh() is called (or refresh(false)). If false,
     * just the cached entries for the current directory are cleared
     */
    public static final Property REMOVE_ALL_LISTING_CACHE_REFRESH = new Property("REMOVE_ALL_LISTING_CACHE_REFRESH", Type.BOOLEAN);

    /**
     * The type of this property
     */
    @Getter
    private final Type type;
    /**
     * The name of the property
     */
    @Getter
    private final String propertyName;

    /**
     * Constructs a property and the type.
     * @param propertyName the name of the property. This should match the name of the property in the properties file
     * @param type the type of the property
     */
    Property(String propertyName, Type type) {
        this.propertyName = propertyName;
        this.type = type;
    }
}
