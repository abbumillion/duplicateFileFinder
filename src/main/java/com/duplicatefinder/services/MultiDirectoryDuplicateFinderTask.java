//package com.duplicatefinder.services;
//
//import com.duplicatefinder.models.DuplicateGroup;
//import javafx.concurrent.Task;
//import java.io.IOException;
//import java.nio.file.*;
//import java.nio.file.attribute.BasicFileAttributes;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.atomic.AtomicLong;
//
//public class MultiDirectoryDuplicateFinderTask extends Task<Map<String, List<DuplicateGroup>>> {
//
//    private final List<String> directories;
//    private final AtomicLong filesScanned = new AtomicLong();
//    private long estimatedTotalFiles;
//
//    // Constructor 1: From List of paths
//    public MultiDirectoryDuplicateFinderTask(List<String> directories) {
//        this.directories = new ArrayList<>(directories);
//    }
//
//    // Constructor 2: From array
//    public MultiDirectoryDuplicateFinderTask(String... directories) {
//        this.directories = new ArrayList<>(Arrays.asList(directories));
//    }
//
//    // Constructor 3: From ObservableList (common in JavaFX)
//    public MultiDirectoryDuplicateFinderTask(javafx.collections.ObservableList<String> directories) {
//        this.directories = new ArrayList<>(directories);
//    }
//
//    @Override
//    protected Map<String, List<DuplicateGroup>> call() throws Exception {
//        if (directories.isEmpty()) {
//            updateMessage("No directories selected");
//            return Collections.emptyMap();
//        }
//
//        updateMessage("Scanning " + directories.size() + " directories...");
//        updateProgress(-1, 1); // Indeterminate
//
//        // Phase 1: Collect potential duplicates by size
//        Map<Long, List<Path>> sizeMap = scanAllDirectories();
//
//        // Phase 2: Find actual duplicates
//        updateMessage("Finding duplicates...");
//        return findActualDuplicates(sizeMap);
//    }
//
//    private Map<Long, List<Path>> scanAllDirectories() throws IOException {
//        Map<Long, List<Path>> sizeMap = new ConcurrentHashMap<>();
//
//        // Estimate total files across all directories
//        estimatedTotalFiles = estimateTotalFileCount();
//        updateProgress(0, estimatedTotalFiles);
//
//        // Track current directory for UI updates
//        AtomicLong currentDirIndex = new AtomicLong(0);
//
//        for (String dirPath : directories) {
//            if (isCancelled()) {
//                break;
//            }
//
//            Path currentDir = Paths.get(dirPath);
//            if (!Files.exists(currentDir) || !Files.isDirectory(currentDir)) {
//                updateMessage("Skipping invalid directory: " + dirPath);
//                continue;
//            }
//
//            // Update UI with current directory
//            long dirNum = currentDirIndex.incrementAndGet();
//            updateMessage("Scanning directory " + dirNum + "/" + directories.size() +
//                    ": " + getDirectoryName(currentDir));
//
//            scanSingleDirectory(currentDir, sizeMap);
//        }
//
//        updateMessage("Scan complete. Found " + formatNumber(filesScanned.get()) + " files");
//        return sizeMap;
//    }
//
//    private void scanSingleDirectory(Path directory, Map<Long, List<Path>> sizeMap)
//            throws IOException {
//
//        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
//            @Override
//            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
//                if (isCancelled()) {
//                    return FileVisitResult.TERMINATE;
//                }
//
//                long size = attrs.size();
//                long scanned = filesScanned.incrementAndGet();
//
//                // Update progress every 1000 files
//                if (scanned % 1000 == 0) {
//                    updateMessage(getProgressMessage(scanned, directory));
//                    updateProgress(scanned, Math.max(scanned + 1, estimatedTotalFiles));}
//
//                // Skip unique small files (optimization)
//                if (size == 0 || size == 1) {
//                    return FileVisitResult.CONTINUE;
//                }
//
//                // Track files with same size
//                sizeMap.computeIfAbsent(size, k -> Collections.synchronizedList(new ArrayList<>()))
//                        .add(file);
//
//                return FileVisitResult.CONTINUE;
//            }
//
//            @Override
//            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
//                if (isCancelled()) {
//                    return FileVisitResult.TERMINATE;
//                }
//
//                // Skip system folders
//                String dirName = dir.getFileName().toString();
//                if (shouldSkipDirectory(dirName)) {
//                    return FileVisitResult.SKIP_SUBTREE;
//                }
//                return FileVisitResult.CONTINUE;
//            }
//        });
//    }
//
//    // Helper methods
//    private String getProgressMessage(long scanned, Path currentDir) {
//        return String.format("Scanned: %s files | Current: %s",
//                formatNumber(scanned),
//                getDirectoryName(currentDir));
//    }
//
//    private String getDirectoryName(Path path) {
//        String name = path.getFileName().toString();
//        return name.isEmpty() ? path.toString() : name;
//    }
//
//    private long estimateTotalFileCount() throws IOException {
//        long total = 0;
//        for (String dirPath : directories) {
//            Path dir = Paths.get(dirPath);
//            if (Files.exists(dir)) {
//                total += quickFileEstimate(dir);
//            }
//        }
//        return Math.max(total, 100000); // Minimum estimate
//    }
//
//    private long quickFileEstimate(Path dir) throws IOException {
//        AtomicLong count = new AtomicLong();
//        try {
//            Files.walk(dir, 2) // Only 2 levels deep for quick estimate
//                    .filter(Files::isRegularFile)
//                    .limit(5000)
//                    .forEach(f -> count.incrementAndGet());
//        } catch (Exception e) {
//            // Estimation failed, use default
//        }
//        return count.get() * 50; // Rough extrapolation
//    }
//
//    private Map<String,List<DuplicateGroup>> findActualDuplicates(Map<Long,List<Path>> sizeMap) throws IOException {
//        Map<String,List<Path>> duplicates = new HashMap<>();
//        long totalGroups = sizeMap.size();
//        long processedGroups = 0;
//        for (Map.Entry<Long,List<Path>> entry  : sizeMap.entrySet()){
//            if (isCancelled()){
//                break;
//            }
//            List<Path> sameSizeFiles = entry.getValue();
//            processedGroups++;
//            // update progress for duplicate finding phase
//            updateMessage("checking duplicates :" + processedGroups + "/" + totalGroups + "size groups");
//            updateProgress(processedGroups,totalGroups);
//            // only check files that could have duplicates
//            if (sameSizeFiles.size() > 1){
//                Map<String,List<Path>> hashMap = new HashMap<>();
//                // first hash check first 2kb
//                for (Path file : sameSizeFiles) {
//                    String quickHash = calculateQuickHash(file);
//                    hashMap.computeIfAbsent(quickHash,k -> new ArrayList<>()).add(file);
//                }
//                // full hash for potential duplicates
//                for (List<Path> potentialDuplicates : hashMap.values()){
//                    if (potentialDuplicates.size() > 1){
//                        Map<String,List<Path>> fullHashMap = new HashMap<>();
//                        for (Path file : potentialDuplicates){
//                            String fullHash = calculateFullHash(file);
//                            fullHashMap.computeIfAbsent(fullHash,k -> new ArrayList<>()).add(file);
//                        }
//                        // add to final results
//                        for (List<Path> dups : fullHashMap.values()){
//                            if (dups.size() > 1){
//                                DuplicateGroup duplicateGroup = new DuplicateGroup(
//                                  si
//                                );
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        return duplicates;
//    }
//    // helper methods
//    private boolean shouldSkipDirectory(String dirName){
//        return dirName.startsWith("$") ||
//                dirName.equals("System Volume Information") ||
//                dirName.equals("Windows") ||
//                dirName.startsWith(".") ||
//                dirName.equals("node_modules") ||
//                dirName.equals("target") ||
//                dirName.equals("bin");
//    }
//    private String calculateQuickHash(Path file) throws IOException {
//        try (var in = Files.newInputStream(file)){
//            byte[] buffer = new byte[2048];
//            int read = in.read(buffer);
//            return read > 0 ? Arrays.hashCode(Arrays.copyOf(buffer,read)) + "" : "empty";
//        }
//    }
//    private String calculateFullHash(Path file) throws IOException {
//        // using crc32 for speed faster than mds/sha
//        java.util.zip.CRC32 crc32 = new java.util.zip.CRC32();
//        try (
//                var in = Files.newInputStream(file)){
//            byte[] buffer = new byte[8192];
//            int read;
//            while((read = in.read(buffer)) != -1){
//                crc32.update(buffer,0,read);
//            }
//        }
//        return Long.toHexString(crc32.getValue());
//    }
//    private long estimateFileCount(Path start){
//        // quick estimation
//        AtomicLong count = new AtomicLong();
//        try {
//            Files.walk(start,3)//Only go 3 levels deep for estimation
//                    .filter(Files::isRegularFile)
//                    .limit(10000)
//                    .forEach(f->count.incrementAndGet());
//        } catch (IOException e) {
//            // throw new RuntimeException(e);
//        }
//        return Math.max(count.get() * 100,100000);// rough estimate
//    }
//    private String formatNumber(long number){
//        if (number >= 1_000_000){
//            return String.format("%.fM",number/1_000_000.0);
//        } else if (number >= 1_000) {
//            return String.format("%.1fK",number/1_000.0);
//        }
//        return Long.toString(number);
//    }
//}
