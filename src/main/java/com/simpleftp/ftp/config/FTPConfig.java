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
