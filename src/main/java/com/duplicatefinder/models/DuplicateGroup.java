// DuplicateGroup.java
package com.duplicatefinder.models;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class DuplicateGroup {
    private final String groupId;
    private final long size;
    private final String fileHash;
    private List<File> files;

    public DuplicateGroup(long fileSize, String fileHash, List<File> duplicateFiles) {
        this.size = fileSize;
        this.fileHash = fileHash;
        files = duplicateFiles;
        this.groupId = generateGroupId(fileSize, fileHash);
    }

    private String generateGroupId(long size, String hash) {
        return String.format("size_%d_hash_%s", size,
                hash != null ? hash.substring(0, Math.min(8, hash.length())) : "null");
    }

    // Getters
    public String getGroupId() { return groupId; }
    public long getSize() { return size; }
    public String getFileHash() { return fileHash; }
    public List<File> getFiles() { return files; }
    public int getFileCount() { return files.size(); }
    public long getTotalSize() { return size * files.size(); }
    public long getWastedSize() { return size * (files.size() - 1); }

    @Override
    public String toString() {
        return String.format("DuplicateGroup[%s: %d files, %s each]",
                groupId, getFileCount(), formatSize(size));
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}