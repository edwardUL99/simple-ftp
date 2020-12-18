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

package com.simpleftp.properties;

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

    static {
        initialiseProperties();
    }

    /**
     * This enum represents each possible property
     */
    public enum Property {
        CONNECTION_MONITOR_INTERVAL, // the interval that the system should check the connection state
        FILE_EDITOR_HEIGHT, // the height for the file editor window
        FILE_EDITOR_WIDTH // the width for the file editor window
    }

    /**
     * Initialises the properties object
     */
    private static void initialiseProperties() {
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
     * Retrieves the property identified by the provided property name
     * @param property the property to get the value of
     * @return the property value if found
     */
    public static String getProperty(Property property) {
        return properties.getProperty(property.toString());
    }
}
