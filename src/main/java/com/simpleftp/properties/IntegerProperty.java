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
 * Represents an IntegerProperty which getValue returns an Integer object
 */
public class IntegerProperty extends Property {
    /**
     * The minimum value for this IntegerProperty
     */
    private Integer minimum;
    /**
     * The maximum value for this IntegerProperty
     */
    private Integer maximum;

    /**
     * Constructs a property and the type.
     *
     * @param propertyName the name of the property. This should match the name of the property in the properties file
     * @param value        the properties value. A value should be specified here as default
     */
    IntegerProperty(String propertyName, Integer value) {
        this(propertyName, value, null, null); // construct an object with no maximum and minimum
    }

    /**
     * Constructs a property and the type.
     *
     * @param propertyName the name of the property. This should match the name of the property in the properties file
     * @param value        the properties value. A value should be specified here as default
     * @param minimum      specifies a minimum value this integer value can take. Leave null to have no minimum. Comparison is done >=
     * @param maximum      specifies a maximum value this integer value can take. Leave null to have no maximum. Comparison is done <=.
     *                     If minimum is = maximum, a PropertyException is thrown. If minimum > maximum, they are swapped
     */
    IntegerProperty(String propertyName, Integer value, Integer minimum, Integer maximum) {
        super(propertyName, value);
        if (!Properties.isInitialised())
            Properties.initialiseProperties();
        this.minimum = minimum;
        this.maximum = maximum;
        checkMinMaxValues();
        Properties.getProperty(this);
    }

    /**
     * Checks the minimum maximum values and throws PropertyException if they are equal, or swaps if in wrong order
     */
    private void checkMinMaxValues() {
        if (minimum != null && maximum != null) {
            if (minimum.equals(maximum)) {
                throw new PropertyException("The minimum and maximum values provided to an IntegerProperty cannot be the same");
            } else {
                int temp = minimum;
                if (minimum > maximum) {
                    maximum = minimum;
                    minimum = temp;
                }
            }
        }
    }

    /**
     * Validates the value parsed in from properties
     *
     * @param value the value parsed from properties file
     */
    @Override
    void validateValue(String value) {
        try {
            int number = Integer.parseInt(value);
            validateValue(number);
        } catch (NumberFormatException ex) {
            throw new PropertyException("An IntegerProperty must be a valid numeric type");
        }
    }

    /**
     * Validates the int value against min and max if specified
     *
     * @param value the value parsed from properties file
     */
    private void validateValue(int value) {
        boolean validated = true;
        String errorMessage = "The value: " + value + " provided to this IntegerProperty should be ";

        if (minimum != null) {
            validated = value >= minimum;
            errorMessage += ">= " + minimum;
        }

        if (maximum != null) {
            validated = validated && value <= maximum;
            errorMessage += " and <= " + maximum;
        }

        if (!validated)
            throw new PropertyException(errorMessage);
    }

    /**
     * Returns the value behind this Property
     *
     * @return the property value
     */
    @Override
    public Integer getValue() {
        return (Integer)super.getValue();
    }

    /**
     * Sets the value for this property
     *
     * @param value the value to set it to
     */
    @Override
    public void setValue(Object value) {
        if (value instanceof Integer) {
            validateValue((Integer) value);
            super.setValue(value);
        } else {
            throw new PropertyException("The provided value: " + value + " is not of type Integer");
        }
    }

    /**
     * Parses the value to the appropriate type
     * Assumes validation is already done
     *
     * @param value the value to parse
     */
    @Override
    void parseValue(String value) {
        super.setValue(Integer.parseInt(value));
    }
}
