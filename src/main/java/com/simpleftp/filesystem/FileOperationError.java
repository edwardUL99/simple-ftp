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

package com.simpleftp.filesystem;

import lombok.Getter;

/**
 * This class represents a non-fatal error that occurred during an extensive operation like recursive copy/move/removal.
 *
 * If you need a fatal error, a FileSystemException should be thrown.
 * Can only be constructed inside the filesystem package. To retrieve the next error message,
 * call FileSystem.getNextFileOperationError().
 */
public class FileOperationError {
    /**
     * The error message to display
     */
    @Getter
    private final String errorMessage;
    /**
     * The file path being operated on
     */
    @Getter
    private final String sourcePath;
    /**
     * The file path being copied/moved to
     */
    @Getter
    private final String destinationPath;

    /**
     * Constructs a FileOperationError with the provided error message, source and destination paths
     * @param errorMessage the error message to display
     * @param sourcePath the path of the file being operated on.
     * @param destinationPath the destination where the file is being copied or moved to. Leave null for removals
     */
    protected FileOperationError(String errorMessage, String sourcePath, String destinationPath) {
        this.errorMessage = errorMessage;
        this.sourcePath = sourcePath;
        this.destinationPath = destinationPath;
    }
}
