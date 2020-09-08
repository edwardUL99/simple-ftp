package com.simpleftp.ftp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

/**
 * This class provides details to be used in creating a FTPConnection
 * Encapsulates all these details into one place
 */
@NoArgsConstructor
@AllArgsConstructor
@With
@Data
public class FTPConnectionDetails {
    /**
     * The page size for listing files in the server
     */
    private int pageSize;
    //this will be added to
}
