package com.duplicatefinder.utils;

public class FormatUtils {

    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    public static String formatHash(String hash) {
        if (hash.length() > 50) {
            return hash.substring(0, 8) + "...";
        }
        return hash;
    }
}