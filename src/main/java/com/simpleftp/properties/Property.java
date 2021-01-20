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
 * This class represents a possible property and its property type.
 * This does not contain a value from the file. To get a property, call Properties.getProperty with a pre-defined property
 * constant.
 *
 * Based on the type, you will want to parse the String returned by getProperty. If it is an ENUM type, it returns the String, Boolean, call Boolean.parseBoolean,
 * Integer, Integer.parseInteger etc.
 */
public abstract class Property {
    /**
     * The name of the property
     */
    @Getter
    private final String propertyName;
    /**
     * The value of the property
     */
    private Object value;

    /**
     * Constructs a property and the type.
     * @param propertyName the name of the property. This should match the name of the property in the properties file
     * @param value the properties value. A value should be specified here as default
     */
    Property(String propertyName, Object value) {
        this.propertyName = propertyName;
        this.value = value;
    }

    /**
     * Validates the value parsed in from properties.
     * This should throw a PropertyException if not valid
     * @param value the value parsed from properties file
     */
    abstract void validateValue(String value);

    /**
     * Parses the value to the appropriate type
     * @param value the value to parse
     */
    abstract void parseValue(String value);

    /**
     * Returns the value behind this Property
     * @return the property value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Sets the value for this property
     * @param value the value to set it to
     */
    public void setValue(Object value) {
        this.value = value;
    }
}
