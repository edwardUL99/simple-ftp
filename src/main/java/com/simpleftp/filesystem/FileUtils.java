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

import com.simpleftp.properties.Properties;
import com.simpleftp.properties.Property;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.HashMap;

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
     * The mappings of r, w, x or - to octal number
     */
    private static final HashMap<Character, Integer> octalMappings = new HashMap<>();

    /**
     * Maps a octal digit to the triplet of rwx
     *
     * CHMOD octal mappings:
     *         r = 4 w = 2 x = 1
     *
     *         rwx = 4 (r) + 2 (w) + 1 (x) = 7
     *         rw- = 4 (r) + 2 (w) + 0 (x) = 6
     *         r-x = 4 (r) + 0 (w) + 1 (x) = 5
     *         r-- = 4 (r) + 0 (w) + 0 (x) = 4
     *         -wx = 0 (r) + 2 (w) + 1 (x) = 3
     *         -w- = 0 (r) + 2 (w) + 0 (x) = 2
     *         --x = 0 (r) + 0 (w) + 1 (x) = 1
     *         --- = 0 (r) + 0 (w) + 0 (x) = 0
     */
    private static final HashMap<Integer, String> octalToPermissionTriplet = new HashMap<>();

    static {
        octalMappings.put('-', 0);
        octalMappings.put('r', 4);
        octalMappings.put('w', 2);
        octalMappings.put('x', 1);

        octalToPermissionTriplet.put(7, "rwx");
        octalToPermissionTriplet.put(6, "rw-");
        octalToPermissionTriplet.put(5, "r-x");
        octalToPermissionTriplet.put(4, "r--");
        octalToPermissionTriplet.put(3, "-wx");
        octalToPermissionTriplet.put(2, "-w-");
        octalToPermissionTriplet.put(1, "--x");
        octalToPermissionTriplet.put(0, "---");
    }

    /**
     * The temp directory
     */
    public static final String TEMP_DIRECTORY = System.getProperty("java.io.tmpdir");

    /**
     * The separator for local file paths
     */
    public static final String PATH_SEPARATOR = System.getProperty("file.separator");

    /**
     * The boolean value for file sizes and following links
     */
    static final boolean FILE_SIZE_FOLLOW_LINK = Boolean.parseBoolean(Properties.getProperty(Property.FILE_SIZE_FOLLOW_LINK));

    /**
     * The boolean value for file permissions and following links
     */
    static final boolean FILE_PERMS_FOLLOW_LINK = Boolean.parseBoolean(Properties.getProperty(Property.FILE_PERMS_FOLLOW_LINK));

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
            parentPath = parentPath == null ? getRootPath(true):parentPath; // if windows, find the root

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

    /**
     * Gets the path representing root. On Windows, this is the SystemDrive and on Unix, it is "/"
     * @param local true if you want local root, false if remote
     * @return the path representing root
     */
    public static String getRootPath(boolean local) {
        String systemDrive = System.getenv("SystemDrive");
        if (!local || systemDrive == null) { // if local is true and system drive is null, we are on a Unix system. If local is false, we use the remote separator
            return "/";
        } else {
            return systemDrive + PATH_SEPARATOR;
        }
    }

    /**
     * Gets the octal number for the given permission group of 3 (e.g. ugo, the user group is rwx)
     * @param permissionsGroup the triplet from the 3 triplets of permissions in a permissions string
     * @return the single octal number for this permissions group
     */
    private static String getOctalForGroup(String permissionsGroup) {
        int octal = 0;
        char read = permissionsGroup.charAt(0), write = permissionsGroup.charAt(1), execute = permissionsGroup.charAt(2);

        if (read != '-' && read != 'r') {
            throw new IllegalArgumentException("Invalid character at read position of permission group: " + read + ", one of {-, r} expected");
        } else {
            octal += octalMappings.get(read);
        }

        if (write != '-' && write != 'w') {
            throw new IllegalArgumentException("Invalid character at write position of permission group: " + write + ", one of {-, w} expected");
        } else {
            octal += octalMappings.get(write);
        }

        if (execute != '-' && execute != 'x') {
            throw new IllegalArgumentException("Invalid character at execute position of permission group: " + execute + ", one of {-, x} expected");
        } else {
            octal += octalMappings.get(execute);
        }

        return "" + octal;
    }

    /**
     * Gets the ugo sections in an array from the given permissions string. The first character is stripped off
     * @param permissionsString the permissions string to split
     * @return the array containing the 3 sections
     */
    private static String[] getPermissionsGroups(String permissionsString) {
        String[] groups = new String[3];
        permissionsString = permissionsString.substring(1);
        groups[0] = permissionsString.substring(0, 3);
        groups[1] = permissionsString.substring(3, 6);
        groups[2] = permissionsString.substring(6, 9);

        return groups;
    }

    /**
     * Parses the given permissions string to an octal number that would be passed in to chmod.
     * Example, -rwxrwxrwx permissions would be converted to octal 777 and -rwxrw-r-- would be converted to 764
     * @param permissionsString the permissions string to parse
     * @return the string of octal numbers representing the permissions
     * @throws IllegalArgumentException if the permissionsString is not valid, i.e. > or < 10 characters long, contains an invalid character
     */
    public static String permissionsToOctal(String permissionsString) throws IllegalArgumentException {
        int length = permissionsString.length();

        if (length != 10) {
            throw new IllegalArgumentException("A permissions string must be 10 characters long");
        }

        char firstCh = permissionsString.charAt(0);

        if (firstCh != '-' && firstCh != 'l' && firstCh != 'd') {
            throw new IllegalArgumentException("Unknown character at start of permissions string: " + firstCh + ", one of {-, l, d} allowed");
        }

        String[] groups = getPermissionsGroups(permissionsString);

        StringBuilder octal = new StringBuilder();

        for (String group : groups) {
            octal.append(getOctalForGroup(group));
        }

        return octal.toString();
    }

    /**
     * Converts octal to String permissions. E.g converts 777 to rwxrwxrwx
     * @param octal the octal numbers to convert
     * @return permissions string that matches the octal number
     */
    public static String convertOctalToPermissions(String octal) {
        return octalToPermissionTriplet.get(octal.charAt(0) - '0') // 7 is 37 in ASCII, 0 is 30, so minusing 30 from number gives octal digit, e.g. 7 is 37 - 30 = 7 35 - 30 = 5
                + octalToPermissionTriplet.get(octal.charAt(1) - '0')
                + octalToPermissionTriplet.get(octal.charAt(2) - '0');
    }

    /**
     * Validates an Octal string to ensure that it is 3 characters long, and each character is 0-7 inclusive
     * @param octalString the octal string to validate
     * @return an object array with index 0 = Boolean valid 1 = null if valid, if not valid, error message as a String
     */
    public static Object[] isValidOctal(String octalString) {
        Object[] ret = new Object[2];
        ret[0] = true;

        if (octalString.length() != 3) {
            ret[0] = false;
            ret[1] = "An octal string must have 3 characters";
        } else {
            for (int i = 0; i < 3; i++) {
                char ch = octalString.charAt(i);

                try {
                    int number = Integer.parseInt("" + ch);

                    if (number < 0 || number > 7) {
                        ret[0] = false;
                        ret[1] = "An octal digit must be between 0 and 7. Digit provided is: " + ch;
                        break;
                    }
                } catch (NumberFormatException ex) {
                    ret[0] = false;
                    ret[1] = "A character in the octal string must be numeric. Character provided is: " + ch;
                    break;
                }
            }
        }

        return ret;
    }
}
