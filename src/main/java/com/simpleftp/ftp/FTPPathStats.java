package com.simpleftp.ftp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * This class returns a collection of statistics for a specified path in one location
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class FTPPathStats {
    private String filePath;
    private String modificationTime;
    private String status;
    private String size;
}
