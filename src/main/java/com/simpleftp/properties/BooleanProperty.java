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

/**
 * Represents an IntegerProperty which getValue returns a Boolean object
 */
public final class BooleanProperty extends Property {
    /**
     * Constructs a property and the type.
     *
     * @param propertyName the name of the property. This should match the name of the property in the properties file
     * @param value        the properties value. A value should be specified here as default
     */
    BooleanProperty(String propertyName, Boolean value) {
        super(propertyName, value);
        if (!Properties.isInitialised())
            Properties.initialiseProperties();
        Properties.getProperty(this);
    }

    /**
     * Validates the value parsed in from properties
     *
     * @param value the value parsed from properties file
     */
    @Override
    void validateValue(String value) {
        value = value.toLowerCase();
        if (!value.equals("true") && !value.equals("false"))
            throw new PropertyException("The provided value: " + value + " is not a valid boolean");
    }

    /**
     * Returns the value behind this Property
     *
     * @return the property value
     */
    @Override
    public Boolean getValue() {
        return (Boolean)super.getValue();
    }

    /**
     * Sets the value for this property
     *
     * @param value the value to set it to
     */
    @Override
    public void setValue(Object value) {
        if (value instanceof Boolean)
            super.setValue(value);
        else
            throw new PropertyException("The provided value: " + value + " is not of type Boolean");
    }

    /**
     * Parses the value to the appropriate type
     *
     * @param value the value to parse
     */
    @Override
    void parseValue(String value) {
        setValue(Boolean.parseBoolean(value));
    }
}
