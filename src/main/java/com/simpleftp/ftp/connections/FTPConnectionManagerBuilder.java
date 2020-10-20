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

package com.simpleftp.ftp.connections;

import com.simpleftp.FTPSystem;

import java.util.ArrayList;

/**
 * Can be used to determine the FTPConnectionManager to return
 */
public class FTPConnectionManagerBuilder {
    private boolean useSystemManager;
    private boolean setBuiltOneAsSystemManager;
    private ArrayList<FTPConnection> connections;

    /**
     * If this is called with true the resulting builder build() will return the connection manager set in FTPSystem if not null.
     * If null, it will attempt to construct a default one
     * @param useSystemManager if true it will attempt to return the manager set in the FTPSystem
     * @return this object to allow chaining
     */
    public FTPConnectionManagerBuilder useSystemConnectionManager(boolean useSystemManager) {
        this.useSystemManager = useSystemManager;
        return this;
    }

    /**
     * Sets the built manager as the FTPSystem one
     * @param setBuiltOneAsSystemManager true to set built one ass FTPSystem manager
     * @return this builder to allow chaining
     */
    public FTPConnectionManagerBuilder setBuiltManagerAsSystemManager(boolean setBuiltOneAsSystemManager) {
        this.setBuiltOneAsSystemManager = setBuiltOneAsSystemManager;
        return this;
    }

    /**
     * Builds the manager with the already existing connections
     * @param connections the connections existing to add
     * @return this builder to allow chaining
     */
    public FTPConnectionManagerBuilder withPreparedConnections(ArrayList<FTPConnection> connections) {
        this.connections = connections;
        return this;
    }

    /**
     * Adds a prepared connection to this builder to be added to the built manager
     * @param connection the connection to add
     * @return this builder to allow chaining
     */
    public FTPConnectionManagerBuilder addPreparedConnection(FTPConnection connection) {
        if (connections == null)
            connections = new ArrayList<>();
        connections.add(connection);
        return this;
    }

    /**
     * Builds the FTPConnectionManager with the parameters set
     * @return the built FTPConnectionManager
     */
    public FTPConnectionManager build() {
        FTPConnectionManager manager;

        if (useSystemManager) {
            manager = FTPSystem.getConnectionManager();
            if (manager != null)
                return manager;
        }

        manager = new FTPConnectionManager();
        if (connections != null)
            manager.setManagedConnections(connections);

        if (setBuiltOneAsSystemManager)
            FTPSystem.setConnectionManager(manager);

        return manager;
    }
}
