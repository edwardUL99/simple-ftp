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

package com.simpleftp.ftp.tests.testable;

import com.simpleftp.sessions.SessionLoader;

/**
 * This class extends SessionLoader class so that we can access it's constructors and test it
 */
public class SessionLoaderTestable extends SessionLoader {
    /**
     * This constructs a default sub-class
     */
    public SessionLoaderTestable() {
        super();
    }
}
