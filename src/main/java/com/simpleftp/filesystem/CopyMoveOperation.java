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

package com.simpleftp.filesystem;

/**
 * This enum is used by the FileSystems to determine if the copy/move operation is within same file system or local to remote etc.
 */
enum CopyMoveOperation {
    /**
     * This option represents a copy/move that is staying within the local file system
     */
    LOCAL_TO_LOCAL,
    /**
     * This option represents a copy/move that is from local to remote file system
     */
    LOCAL_TO_REMOTE,
    /**
     * This option represents a copy/move that is staying within the remote file system
     */
    REMOTE_TO_REMOTE,
    /**
     * This option represents a copy/move that is from remote to local file system
     */
    REMOTE_TO_LOCAL
}
