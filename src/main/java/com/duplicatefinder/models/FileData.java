//package com.duplicatefinder.models;
//
//import javafx.beans.property.*;
//
//import java.time.LocalDateTime;
//
//public class FileData {
//    private final StringProperty path;
//    private final StringProperty hash;
//    private final LongProperty size;
//    private final ObjectProperty<LocalDateTime> lastModified;
//    private final BooleanProperty markedForDeletion;
//    private final StringProperty fileName;
//    private final StringProperty fileType;
//
//    // Updated constructor with hash parameter
//    public FileData(String path, String hash, long size, LocalDateTime lastModified) {
//        this.path = new SimpleStringProperty(path);
//        this.hash = new SimpleStringProperty(hash);
//        this.size = new SimpleLongProperty(size);
//        this.lastModified = new SimpleObjectProperty<>(lastModified);
//        this.markedForDeletion = new SimpleBooleanProperty(false);
//        this.fileName = new SimpleStringProperty(extractFileName(path));
//        this.fileType = new SimpleStringProperty(extractFileType(path));
//    }
//
//    private String extractFileName(String path) {
//        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
//        return lastSlash != -1 ? path.substring(lastSlash + 1) : path;
//    }
//
//    private String extractFileType(String path) {
//        int dotIndex = path.lastIndexOf('.');
//        return dotIndex != -1 ? path.substring(dotIndex + 1).toLowerCase() : "";
//    }
//
//    // Getters
//    public String getPath() {
//        return path.get();
//    }
//
//    public String getHash() {
//        return hash.get();
//    }
//
//    public long getSize() {
//        return size.get();
//    }
//
//    public LocalDateTime getLastModified() {
//        return lastModified.get();
//    }
//
//    public boolean isMarkedForDeletion() {
//        return markedForDeletion.get();
//    }
//
//    // Setters
//    public void setMarkedForDeletion(boolean value) {
//        this.markedForDeletion.set(value);
//    }
//
//    public String getFileName() {
//        return fileName.get();
//    }
//
//    public String getFileType() {
//        return fileType.get();
//    }
//
//    // Property getters
//    public StringProperty pathProperty() {
//        return path;
//    }
//
//    public StringProperty hashProperty() {
//        return hash;
//    }
//
//    public LongProperty sizeProperty() {
//        return size;
//    }
//
//    public ObjectProperty<LocalDateTime> lastModifiedProperty() {
//        return lastModified;
//    }
//
//    public BooleanProperty markedForDeletionProperty() {
//        return markedForDeletion;
//    }
//
//    public StringProperty fileNameProperty() {
//        return fileName;
//    }
//
//    public StringProperty fileTypeProperty() {
//        return fileType;
//    }
//}