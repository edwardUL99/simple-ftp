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

package com.simpleftp.ui.files;

import com.simpleftp.ui.files.LineEntry;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * This class wraps an ArrayList of line entries and some flags for it
 */
public class LineEntries {
    /**
     * The list of line entries
     */
    @Getter
    @Setter
    private ArrayList<LineEntry> lineEntries;
    /**
     * A flag to determine if the line entries list should be sorted
     */
    private boolean sort;

    /**
     * Constructs a default list of line entries
     */
    public LineEntries() {
        this.lineEntries = new ArrayList<>();
    }

    /**
     * Set this to true if the list should be sorted, false if not
     * @param sort true to sort, false if not
     */
    public void setSort(boolean sort) {
        this.sort = sort;
    }

    /**
     * Clears the list of line entries
     */
    public void clear() {
        lineEntries.clear();
    }

    /**
     * Gets the size of the line entries list
     * @return size of list
     */
    public int size() {
        return lineEntries.size();
    }

    /**
     * Adds the provided line entry to this LineEntries object
     * @param lineEntry the line entry to add
     */
    public void add(LineEntry lineEntry) {
        lineEntries.add(lineEntry);
    }

    /**
     * Removes the provided line entry to this LineEntries object
     * @param lineEntry the line entry to remove
     */
    public void remove(LineEntry lineEntry) {
        lineEntries.remove(lineEntry);
    }

    /**
     * Sorts the line entries if sort is true and size is greater than 0 with directories first then files
     */
    public void sort() {
        if (lineEntries.size() > 0 && sort) {
            ArrayList<LineEntry> directoryEntries = lineEntries.stream()
                    .filter(LineEntry::isDirectory)
                    .collect(Collectors.toCollection(ArrayList::new));
            ArrayList<LineEntry> fileEntries = lineEntries.stream()
                    .filter(LineEntry::isFile)
                    .collect(Collectors.toCollection(ArrayList::new));

            // sort the lists
            Collections.sort(directoryEntries);
            Collections.sort(fileEntries);

            ArrayList<LineEntry> sorted = new ArrayList<>(directoryEntries.size() + fileEntries.size());
            sorted.addAll(directoryEntries);
            sorted.addAll(fileEntries);

            lineEntries = sorted;
        }
    }

    /**
     * Returns whether or not this LineEntries object contains the provided line entry
     * @param lineEntry the line entry to check
     * @return true if contains it or false if not
     */
    public boolean contains(LineEntry lineEntry) {
        return lineEntries.contains(lineEntry);
    }
}
