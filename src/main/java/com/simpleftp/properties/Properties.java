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
     * The location of the properties file
     */
    private static String propertiesLocation;

    static {
        initialiseProperties();
    }

    /**
     * Initialises the properties object
     */
    private static void initialiseProperties() {
        try {
            propertiesLocation = System.getProperty("simpleftp.properties");
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
     * Validates the the property's value matches the value in the properties file
     * @param propertyName the name of the property
     * @param propertyValue the value of the property
     * @param propertyType the type of the property
     */
    private static void validatePropertyValue(String propertyName, String propertyValue, Property.Type propertyType) {
        switch (propertyType) {
            case INTEGER: try {
                Integer.parseInt(propertyValue);
            } catch (NumberFormatException ex) {
                throw new PropertyException("The property with name " + propertyName + " with value " + propertyValue + " is not a valid value of type " + propertyType.toString());
            }
            break;
            case BOOLEAN: propertyValue = propertyValue.toLowerCase();
            if (!propertyValue.equals("true") && !propertyValue.equals("false")) {
                throw new PropertyException("The property with name " + propertyName + " with value " + propertyValue + " is not a valid value of type " + propertyType.toString());
            }

        }
    }

    /**
     * Validates that the property is in the properties value and type matches that value
     * @param property the property to validate
     */
    private static void validateProperty(Property property) {
        String propertyName = property.getPropertyName();
        Property.Type propertyType = property.getType();
        String value = properties.getProperty(property.getPropertyName());
        if (value == null)
            throw new PropertyException("Property with name " + propertyName + " with type " + propertyType.toString() + " not found in properties file " + propertiesLocation);
        else if (value.equals(""))
            throw new PropertyException("Property with name " + propertyName + " with type " + propertyType.toString() + " cannot have an empty value");

        validatePropertyValue(propertyName, value, propertyType);
    }

    /**
     * Retrieves the property identified by the provided property name. If the value in the file is not found or invalid type, PropertyException is thrown
     * @param property the property to get the value of
     * @return the property value if found
     */
    public static String getProperty(Property property) {
        validateProperty(property);
        return properties.getProperty(property.getPropertyName());
    }
}
