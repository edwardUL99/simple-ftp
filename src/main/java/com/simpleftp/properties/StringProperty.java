/*
 *  Copyright (C) 2020-2021  Edward Lynch-Milner
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

import java.util.List;

/**
 * Represents a StringProperty which getValue returns a String object
 */
public class StringProperty extends Property {
    /**
     * A list of valid values the string value can take
     */
    private final List<String> validValues;

    /**
     * Constructs a property and the type.
     *
     * @param propertyName the name of the property. This should match the name of the property in the properties file
     * @param value        the properties value. A value should be specified here as default
     * @param validValues  the values that value should match one of them case-sensitive. Don't provide it if value can be any non-empty string
     */
    StringProperty(String propertyName, String value, String... validValues) {
        super(propertyName, value);
        if (validValues.length > 0)
            this.validValues = List.of(validValues);
        else
            this.validValues = null;

        if (!Properties.isInitialised())
            Properties.initialiseProperties();
        Properties.getProperty(this);
    }

    /**
     * Validates the value parsed in from properties. If validValues are passed in, this checks if the value matches, else just checks for not empty
     *
     * @param value the value parsed from properties file
     */
    @Override
    void validateValue(String value) {
        if (value.length() > 0) {
            if (validValues != null) {
                boolean found = false;
                for (String valid : validValues) {
                    if (valid.equals(value)) {
                        found = true;
                        break;
                    }
                }

                if (!found)
                    throw new PropertyException("Invalid StringProperty value provided: " + value + ", one of " + validValues + " expected");
            }
        } else {
            throw new PropertyException("A StringProperty cannot be empty");
        }
    }

    /**
     * Returns the value behind this Property
     *
     * @return the property value
     */
    @Override
    public String getValue() {
        return (String)super.getValue();
    }

    /**
     * Sets the value for this property
     *
     * @param value the value to set it to
     */
    @Override
    public void setValue(Object value) {
        if (value instanceof String) {
            validateValue((String)value);
            super.setValue(value);
        } else {
            throw new PropertyException("The provided value: " + value + " is not of type String");
        }
    }

    /**
     * Parses the value to the appropriate type
     *
     * @param value the value to parse
     */
    @Override
    void parseValue(String value) {
        setValue(value);
    }
}
