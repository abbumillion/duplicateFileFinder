package com.duplicatefinder.services;

import com.duplicatefinder.models.DuplicateGroup;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class DuplicateFinderTask extends Task<List<DuplicateGroup>> {

    private final ObservableList<File> directories;
    private final AtomicLong filesScanned = new AtomicLong();
    private final AtomicLong directoriesScanned = new AtomicLong();
    private long estimatedTotalFiles;

    // Constructor options
    public DuplicateFinderTask(ObservableList<File> directories) {
        this.directories = directories;
    }

    @Override
    protected List<DuplicateGroup> call() throws Exception {
        updateMessage("Initializing duplicate scan...");
        updateProgress(0, 100);

        // Phase 1: Scan all files and group by size
        updateMessage("Scanning files by size...");
        updateProgress(10, 100);
        Map<Long, List<File>> sizeMap = scanAllDirectories();

        // Phase 2: Find actual duplicates among same-size files
        updateMessage("Checking for actual duplicates...");
        updateProgress(50, 100);
        List<DuplicateGroup> duplicateGroups = findDuplicateGroups(sizeMap);

        // Sort by wasted space (most wasteful first)
        duplicateGroups.sort((g1, g2) -> Long.compare(g2.getWastedSize(), g1.getWastedSize()));

        updateMessage(String.format("Found %d duplicate groups", duplicateGroups.size()));
        updateProgress(100, 100);

        return duplicateGroups;
    }

    private Map<Long, List<File>> scanAllDirectories() throws IOException {
        Map<Long, List<File>> sizeMap = new ConcurrentHashMap<>();
        // Estimate total files
        estimatedTotalFiles = estimateTotalFileCount();
        for (File dirPath : directories) {
            if (isCancelled()) return sizeMap;

            File dir = Paths.get(dirPath.getAbsolutePath()).toFile();
            if (!Files.exists(dir.toPath())) continue;
            directoriesScanned.incrementAndGet();
            updateMessage(String.format("Scanning: %s (%d/%d directories)",
                    dir, directoriesScanned.get(), directories.size()));
            scanDirectory(dir, sizeMap);
        }
        return sizeMap;
    }

    private void scanDirectory(File directory, Map<Long, List<File>> sizeMap) throws IOException {
        Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (isCancelled()) return FileVisitResult.TERMINATE;

                long scanned = filesScanned.incrementAndGet();
                long size = attrs.size();

                // Update progress every 1000 files
                if (scanned % 1000 == 0) {
                    updateMessage(String.format("Scanned: %s files", formatNumber(scanned)));
                    double progress = Math.min(40.0, 10.0 + 30.0 * scanned / estimatedTotalFiles);
                    updateProgress(progress, 100);
                }

                // Skip obviously unique files (0 or 1 byte)
                if (size <= 1) return FileVisitResult.CONTINUE;

                // Add to size map
                sizeMap.computeIfAbsent(size, k -> Collections.synchronizedList(new ArrayList<>()))
                        .add(file.toFile());

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (isCancelled()) return FileVisitResult.TERMINATE;

                String dirName = dir.getFileName().toString();
                if (shouldSkipDirectory(dirName)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private List<DuplicateGroup> findDuplicateGroups(Map<Long, List<File>> sizeMap) throws IOException {
        List<DuplicateGroup> duplicateGroups = Collections.synchronizedList(new ArrayList<>());
        long totalSizeGroups = sizeMap.size();
        AtomicLong processedGroups = new AtomicLong();

        // Process size groups in parallel for speed
        sizeMap.entrySet().parallelStream().forEach(entry -> {
            if (isCancelled()) return;

            long size = entry.getKey();
            List<File> sameSizeFiles = entry.getValue();

            // Update progress for this phase
            long processed = processedGroups.incrementAndGet();
            if (processed % 100 == 0) {
                updateMessage(String.format("Checking duplicates: %d/%d size groups",
                        processed, totalSizeGroups));
                double progress = Math.min(90.0, 50.0 + 40.0 * processed / totalSizeGroups);
                updateProgress(progress, 100);
            }

            // Only check files that could have duplicates
            if (sameSizeFiles.size() > 1) {
                try {
                    List<DuplicateGroup> groupsForThisSize =
                            findDuplicatesInSizeGroup(size, sameSizeFiles);
                    duplicateGroups.addAll(groupsForThisSize);
                } catch (IOException e) {
                    // Skip this group on error
                }
            }
        });

        return duplicateGroups;
    }

    private List<DuplicateGroup> findDuplicatesInSizeGroup(long size, List<File> sameSizeFiles)
            throws IOException {
        List<DuplicateGroup> groups = new ArrayList<>();

        // Quick hash (first 4KB) for initial grouping
        Map<String, List<File>> quickHashGroups = new HashMap<>();
        for (File file : sameSizeFiles) {
            String quickHash = calculateQuickHash(file, 4096);
            quickHashGroups.computeIfAbsent(quickHash, k -> new ArrayList<>())
                    .add(file);
        }

        // Full hash for potential duplicates
        for (List<File> potentialDupes : quickHashGroups.values()) {
            if (potentialDupes.size() > 1) {
                Map<String, List<File>> fullHashGroups = new HashMap<>();

                for (File file : potentialDupes) {
                    String fullHash = calculateFullHash(file);
                    fullHashGroups.computeIfAbsent(fullHash, k -> new ArrayList<>())
                            .add(file);
                }

                // Create DuplicateGroup for each hash match
                for (Map.Entry<String, List<File>> hashEntry : fullHashGroups.entrySet()) {
                    if (hashEntry.getValue().size() > 1) {

                        DuplicateGroup group = new DuplicateGroup(
                                size,
                                hashEntry.getKey(),
                                new ArrayList<>(hashEntry.getValue())
                        );
                        groups.add(group);}
                }
            }
        }

        return groups;
    }

    // Hash calculation methods
    private String calculateQuickHash(File file, int bytes) throws IOException {
        try (var in = Files.newInputStream(file.toPath())) {
            byte[] buffer = new byte[bytes];
            int read = in.read(buffer);
            if (read > 0) {
                // Use CRC32 for speed
                java.util.zip.CRC32 crc = new java.util.zip.CRC32();
                crc.update(buffer, 0, read);
                return Long.toHexString(crc.getValue());
            }
        }
        return "empty";
    }

    private String calculateFullHash(File file) throws IOException {
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        try (var in = Files.newInputStream(file.toPath())) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                if (isCancelled()) return "cancelled";
                crc.update(buffer, 0, read);
            }
        }
        return Long.toHexString(crc.getValue());
    }

    // Helper methods
    private boolean shouldSkipDirectory(String dirName) {
        return dirName.startsWith(".") ||
                dirName.equals("node_modules") ||
                dirName.equals("target") ||
                dirName.equals("bin") ||
                dirName.equals("obj") ||
                dirName.startsWith("$");
    }

    private long estimateTotalFileCount() throws IOException {
        long total = 0;
        for (File dirPath : directories) {
            Path dir = Paths.get(dirPath.getAbsolutePath());
            if (Files.exists(dir)) {
                total += quickEstimate(dir);
            }
        }
        return Math.max(total, 10000);
    }

    private long quickEstimate(Path dir) throws IOException {
        AtomicLong count = new AtomicLong();
        try {
            Files.walk(dir, 2)
                    .filter(Files::isRegularFile)
                    .limit(1000)
                    .forEach(f -> count.incrementAndGet());
        } catch (Exception e) {
            // Estimation failed
        }
        return count.get() * 100; // Rough extrapolation
    }

    private String formatNumber(long number) {
        if (number >= 1_000_000_000) {
            return String.format("%.1fB", number / 1_000_000_000.0);
        } else if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return Long.toString(number);
    }
}