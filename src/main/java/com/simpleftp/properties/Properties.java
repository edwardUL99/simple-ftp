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

import com.simpleftp.properties.exceptions.PropertyException;
import lombok.Getter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * This class provides properties read from a properties file.
 * The properties file (default simpleftp.properties) is found as follows:
 * 1. If the file is in the current directory, it is picked up
 * 2. If the file given by simpleftp.properties system property is a path to a file, use that
 * 3. Else, look for either default file or file passed in with system property on the classpath
 */
public final class Properties {
    /**
     * The properties object backing this instance
     */
    private static final java.util.Properties properties = new java.util.Properties();
    /**
     * Returns true if the properties file was loaded successfully
     */
    @Getter
    private static boolean initialised = false;

    /**
     * This represents how often the connection monitor should check if the connection is still active
     */
    public static final IntegerProperty CONNECTION_MONITOR_INTERVAL = new IntegerProperty("CONNECTION_MONITOR_INTERVAL", 2000);

    /**
     * This defines the initial height of the file editor
     */
    public static final IntegerProperty FILE_EDITOR_HEIGHT = new IntegerProperty("FILE_EDITOR_HEIGHT", 700);

    /**
     * This defines the initial width of the file editor
     */
    public static final IntegerProperty FILE_EDITOR_WIDTH = new IntegerProperty("FILE_EDITOR_WIDTH", 700);

    /**
     * The property representing the size threshold to display file size warning dialog in bytes.
     * Specified in properties file as MB so this property creates an anonymous sub-class to multiply the value by 1000000 to convert to MB
     */
    public static final IntegerProperty FILE_EDITOR_SIZE_WARN_LIMIT = new IntegerProperty("FILE_EDITOR_SIZE_WARN_LIMIT", 100, 1, null) {
        /**
         * Returns the value behind this Property in bytes
         *
         * @return the property value
         */
        @Override
        public Integer getValue() {
            return super.getValue() * 1000000;
        }
    };

    /**
     * If true, the file size retrieved by CommonFile.getSize() is that of the target, not the link
     */
    public static final BooleanProperty FILE_SIZE_FOLLOW_LINK = new BooleanProperty("FILE_SIZE_FOLLOW_LINK", true);

    /**
     * If true, the permissions retrieved by CommonFile.getPermissions() is that of the target, not the link
     */
    public static final BooleanProperty FILE_PERMS_FOLLOW_LINK = new BooleanProperty("FILE_PERMS_FOLLOW_LINK", false);

    /**
     * If true, FTPConnection.getModificationTime(path) is attempted, else (or if this can't be determined), it is the time of the FTPFile returned
     */
    public static final BooleanProperty SERVER_REMOTE_MODIFICATION_TIME = new BooleanProperty("SERVER_REMOTE_MODIFICATION_TIME", false);

    /**
     * If true, LineEntries on a RemoteDirectoryPane will be cached up until the first refresh() method call
     */
    public static final BooleanProperty CACHE_REMOTE_DIRECTORY_LISTING = new BooleanProperty("CACHE_REMOTE_DIRECTORY_LISTING", true);

    /**
     * If true, all cached LineEntries are removed when DirectoryPane.refresh() is called (or refresh(false)). If false,
     * just the cached entries for the current directory are cleared
     */
    public static final BooleanProperty REMOVE_ALL_LISTING_CACHE_REFRESH = new BooleanProperty("REMOVE_ALL_LISTING_CACHE_REFRESH", true);

    /**
     * Defines the operation for drag drop on the same panel. Ctrl will do the opposite of this property
     */
    public static final StringProperty DRAG_DROP_SAME_PANEL_OPERATION = new StringProperty("DRAG_DROP_SAME_PANEL_OPERATION", "MOVE", "COPY", "MOVE");

    /**
     * Defines the operation for drag drop onto different panels. Ctrl will do the opposite of this property
     */
    public static final StringProperty DRAG_DROP_DIFFERENT_PANEL_OPERATION = new StringProperty("DRAG_DROP_DIFFERENT_PANEL_OPERATION", "COPY", "COPY", "MOVE");

    /**
     * The property representing if the LineEntry icon should replace the cursor on a drag/drop or the Cursor.MOVE
     */
    public static final BooleanProperty DRAG_DROP_CURSOR_FILE_ICON = new BooleanProperty("DRAG_DROP_CURSOR_FILE_ICON", false);

    /**
     * The property representing if clipboard should be cleared after successful path paste through local and remote toolbar menus
     */
    public static final BooleanProperty CLEAR_CLIPBOARD_PATH_PASTE = new BooleanProperty("CLEAR_CLIPBOARD_PATH_PASTE", true);

    /**
     * The property determining if match session button is enabled on the login window
     */
    public static final StringProperty MATCH_SESSION_LOGIN_BUTTON = new StringProperty("MATCH_SESSION_LOGIN_BUTTON", "ENABLED", "ENABLED", "DISABLED");

    /**
     * The property determining if tasks should be deleted after a period of time on completion
     */
    public static final BooleanProperty DELETE_TASK_ON_COMPLETION = new BooleanProperty("DELETE_TASK_ON_COMPLETION", true);

    /**
     * Property representing how long the task deletion takes after completion
     */
    public static final IntegerProperty TASK_DELETION_DELAY = new IntegerProperty("TASK_DELETION_DELAY", 10, 5, null);
    /**
     * Property representing the number of non-fatal FileService errors that can occur
     */
    public static final IntegerProperty FILE_OPERATION_ERROR_LIMIT = new IntegerProperty("FILE_OPERATION_ERROR_LIMIT", 5, 0, 20);

    /**
     * Initialises the properties object
     */
    static void initialiseProperties() {
        try {
            String propertiesLocation = System.getProperty("simpleftp.properties");
            propertiesLocation = propertiesLocation == null ? "simpleftp.properties" : propertiesLocation;
            File file = new File(propertiesLocation);

            if (file.isFile()) {
                properties.load(new FileReader(file));
            } else {
                properties.load(ClassLoader.getSystemResourceAsStream(propertiesLocation));
            }

            initialised = true;
        } catch (IOException ex) {
            initialised = false;
        }
    }

    /**
     * Validates that the property is not empty if not null (if null, default value is used) and type matches that value
     * @param property the property to validate
     * @return the value from the properties file if valid, null if not found
     */
    private static String validateProperty(Property property) {
        String propertyName = property.getPropertyName();
        String value = properties.getProperty(propertyName);
        if (value != null) {
            if (value.equals(""))
                throw new PropertyException("Property with name " + propertyName + " cannot have an empty value");

            property.validateValue(value);
        }

        return value;
    }

    /**
     * Retrieves the property identified by the provided property name. If the value in the file is not found or invalid type, PropertyException is thrown
     * @param property the property to get the value of
     */
    static void getProperty(Property property) {
        String validatedValue = validateProperty(property);

        if (validatedValue != null)
            property.parseValue(validatedValue);
    }
}
