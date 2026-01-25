//package com.duplicatefinder.services;
//
//import com.duplicatefinder.models.DuplicateGroup;
//import com.duplicatefinder.models.FileData;
//import javafx.beans.property.IntegerProperty;
//import javafx.beans.property.SimpleIntegerProperty;
//import javafx.beans.value.ObservableValue;
//import javafx.concurrent.Task;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.nio.file.Files;
//import java.security.MessageDigest;
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.util.*;
//import java.util.concurrent.ConcurrentLinkedQueue;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;
//
//public class FileScanner extends Task<Map<String, DuplicateGroup>> {
//    private final List<File> directories;
//    private final Map<String, DuplicateGroup> resultMap = new HashMap<>();
//    private int processedFiles = 0;
//
//    public FileScanner(List<File> directories) {
//        this.directories = directories;
//    }
//
//    @Override
//    protected Map<String, DuplicateGroup> call() throws Exception {
//        updateMessage("Counting files...");
//
//        // Get all files first to count
//        List<File> allFiles = getAllFiles();
//        int totalFiles = allFiles.size();
//
//        if (totalFiles == 0) {
//            updateMessage("No files found to scan");
//            return resultMap;
//        }
//
//        updateMessage("Calculating hashes for " + totalFiles + " files...");
//
//        // Map to store hashes
//        Map<String, List<File>> hashMap = new HashMap<>();
//
//        for (int i = 0; i < allFiles.size(); i++) {
//            if (isCancelled()) break;
//
//            File file = allFiles.get(i);
//
//            try {
//                // Calculate hash
//                String hash = calculateMD5(file);
//
//                // Store file in hash map
//                hashMap.computeIfAbsent(hash, k -> new ArrayList<>()).add(file);
//
//                processedFiles++;
//                updateProgress(processedFiles, totalFiles);
//                updateMessage(String.format("Processing: %d/%d files", processedFiles, totalFiles));
//
//            } catch (Exception e) {
//                System.err.println("Error processing file: " + file.getPath() + " - " + e.getMessage());
//                processedFiles++;
//                updateProgress(processedFiles, totalFiles);
//            }
//        }
//
//        // Create duplicate groups from hash map
//        for (Map.Entry<String, List<File>> entry : hashMap.entrySet()) {
//            List<File> duplicateFiles = entry.getValue();
//
//            // Only create group if there are duplicates
//            if (duplicateFiles.size() > 1) {
//                String hash = entry.getKey();
//                long fileSize = duplicateFiles.getFirst().length();
//                DuplicateGroup group = new DuplicateGroup(hash, fileSize);
//
//                // Add all files to the group
//                for (File file : duplicateFiles) {
//                    try {
//                        LocalDateTime lastModified = LocalDateTime.ofInstant(
//                                Files.getLastModifiedTime(file.toPath()).toInstant(),
//                                ZoneId.systemDefault()
//                        );
//
//                        FileData fileData = new FileData(
//                                file.getAbsolutePath(),
//                                hash,
//                                file.length(),
//                                lastModified
//                        );
//                        group.addFile(fileData);
//                    } catch (Exception e) {
//                        System.err.println("Error adding file to group: " + file.getPath());
//                    }
//                }
//
//                resultMap.put(hash, group);
//            }
//        }
//
//        updateMessage("Scan completed. Found " + resultMap.size() + " duplicate groups.");
//        return resultMap;
//    }
//
//    private List<File> getAllFiles() {
//        List<File> allFiles = Collections.synchronizedList(new ArrayList<>());
//        for (File dir : directories) {
//            if (dir.exists() && dir.isDirectory()) {
//                fastCollectFiles(dir, allFiles);
//            }
//        }
//        return allFiles;
//    }
//
//    private void collectFiles(File directory, List<File> fileList) {
//        if (directory.isDirectory()) {
//            File[] files = directory.listFiles();
//            if (files != null) {
//                for (File file : files) {
//                    if (isCancelled()) return;
//                    if (file.isDirectory()) {
//                        collectFiles(file, fileList);
//                    } else if (file.isFile() && file.canRead()) {
//                        // Skip very small files and system files
//                        if (file.length() > 0 && !file.isHidden()) {
//                            fileList.add(file);
//                            updateMessage("Found " + fileList.size() + " Files");
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    private void fastCollectFiles(File directory, List<File> fileList) {
//        ExecutorService executorService = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
//        if (!directory.exists())
//            return;
//        //bfs queue for directories
//        ConcurrentLinkedQueue<File> dirQueue = new ConcurrentLinkedQueue<>();
//        dirQueue.add(directory);
//        AtomicInteger activeThreads = new AtomicInteger(0);
//
//        //
//        while(!dirQueue.isEmpty() || activeThreads.get() > 0)
//        {
//            if (!dirQueue.isEmpty())
//            {
//                activeThreads.incrementAndGet();
//                File dir = dirQueue.poll();
//
//                executorService.submit(() -> {
//                    try {
//                        File[] files = dir.listFiles();
//                        if (files != null)
//                        {
//                            for (File file : files)
//                            {
//                                if (file.isDirectory())
//                                {
//                                    dirQueue.add(file);
//                                }else {
//                                    fileList.add(file);
//                                    updateMessage("Found " + fileList.size() + " Files");
//                                }
//                            }
//                        }
//                    }finally {
//                        activeThreads.decrementAndGet();
//                    }
//                });
//            }else {
//                try {
//                    Thread.sleep(1); // small yield
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    break;
//                }
//            }
//        }
//        executorService.shutdown();
//        try {
//            executorService.awaitTermination(1, TimeUnit.HOURS);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//    }
//
//    private String calculateMD5(File file) {
//        try (FileInputStream fis = new FileInputStream(file)) {
//            MessageDigest digest = MessageDigest.getInstance("MD5");
//            byte[] buffer = new byte[8192];
//            int read;
//
//            while ((read = fis.read(buffer)) > 0) {
//                digest.update(buffer, 0, read);
//                if (isCancelled()) break;
//            }
//
//            byte[] md5Bytes = digest.digest();
//            StringBuilder sb = new StringBuilder();
//
//            for (byte b : md5Bytes) {
//                sb.append(String.format("%02x", b));
//            }
//
//            return sb.toString();
//
//        } catch (Exception e) {
//            // Fallback: use file size as hash for unreadable files
//            return "error-" + file.length() + "-" + file.getName();
//        }
//    }
//
//
//}