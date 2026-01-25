package com.duplicatefinder.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FileUtils {

    public static long deleteFiles(List<Path> paths) {
        long freedSpace = 0;
        for (Path path : paths) {
            try {
                if (Files.exists(path)) {
                    long size = Files.size(path);
                    if (Files.deleteIfExists(path)) {
                        freedSpace += size;
                    }
                }
            } catch (IOException e) {
                System.err.println("Failed to delete file: " + path);
            }
        }
        return freedSpace;
    }

}