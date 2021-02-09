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
/**
 * This package represents classes related to the associated file systems (local on the host or remote on the server).
 * It is to abstract the different file types, i.e. local files are handled by java.io.File while remote ftp files are handled by apache's FTPFile.
 * This allows a single file for both file systems to be specified in methods and as instance variables, the CommonFile
 */
package com.simpleftp.filesystem;