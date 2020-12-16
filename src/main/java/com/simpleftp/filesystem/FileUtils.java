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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

/**
 * This class provides utility methods to the FileSystsemPackage
 */
public final class FileUtils {
    /**
     * Prevent instantiation
     */
    private FileUtils() {}

    /**
     * The format for the date time string throughout the UI for files
     */
    public static final String FILE_DATETIME_FORMAT = "MMM dd HH:mm";

    /**
     * Parses the calendar object to the FILE_DATETIME_FORMAT
     * @param calendar the calendar object to parse
     * @return the formatted date time
     */
    public static String parseCalendarToFormattedDate(Calendar calendar) {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int year = calendar.get(Calendar.YEAR);

        LocalDateTime localDateTime = LocalDateTime.of(year, month, day, hour, minute);
        return localDateTime.format(DateTimeFormatter.ofPattern(FILE_DATETIME_FORMAT));
    }

    /**
     * Constructs a path by prepending current working directory onto the given path separated with the specified separator
     * @param currentWorkingDir the working directory to prepend
     * @param path the path to prepend to
     * @param pathSeparator the separator used for the paths
     * @return the constructed path
     */
    public static String addPwdToPath(String currentWorkingDir, String path, String pathSeparator) {
        if (currentWorkingDir.endsWith(pathSeparator)) {
            path = currentWorkingDir + path;
        } else {
            path = currentWorkingDir + pathSeparator + path;
        }

        return path;
    }

    /**
     * Retrieves the parent path of the provided path. If it is already, root, it is returned
     * @param path the path to find parent of
     * @param local true if local path false if not
     * @return the parent path
     */
    public static String getParentPath(String path, boolean local) {
        if (local) {
            String parentPath = new LocalFile(path).getParent();
            String windowsParent;
            parentPath = parentPath == null ? ((windowsParent = System.getenv("SystemDrive")) != null ? windowsParent:"/"):parentPath; // if windows, find the root

            return parentPath;
        } else {
            String fileSeparator = "/";
            if (path.equals(fileSeparator)) {
                return path;
            }

            path = path.endsWith(fileSeparator) ? path.substring(0, path.length() - 1):path;

            int lastSepIndex = path.lastIndexOf(fileSeparator);
            if (lastSepIndex != -1 && lastSepIndex != 0) {
                return path.substring(0, lastSepIndex);
            } else {
                return fileSeparator;
            }
        }
    }
}
