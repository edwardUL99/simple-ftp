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

package com.simpleftp.ftp.config;

import lombok.NoArgsConstructor;
import org.apache.commons.net.ftp.FTPClientConfig;

/**
 * This class is to be used to abstract the config properties for a FTPConnection
 * It only adds the config properties that are currently supported by the system
 */
public class FTPConfig {
    /**
     * The config object being wrapped by this class
     */
    private FTPClientConfig config;

    /**
     * Constructs a FTPConfig object using the specified systemType. This can be got from the FTPConnection.getSystemType() call
     * @param systemType the type of the system
     */
    public FTPConfig(String systemType) {
       config = new FTPClientConfig(systemType);
    }
}
