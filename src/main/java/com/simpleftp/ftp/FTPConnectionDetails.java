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

package com.simpleftp.ftp;

import lombok.*;

/**
 * This class provides details to be used in creating a FTPConnection
 * Encapsulates all these details into one place
 */
@NoArgsConstructor
@AllArgsConstructor
@With
@Data
@EqualsAndHashCode
public class FTPConnectionDetails {
    /**
     * The page size for listing files in the server
     */
    private int pageSize;
    /**
     * The number of seconds for the server to time out
     * This is used for all timeouts such a keep alive etc
     */
    private int timeout;
    //this will be added to
}
