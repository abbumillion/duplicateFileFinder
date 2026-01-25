package com.duplicatefinder.controllers;

import com.duplicatefinder.models.DuplicateGroup;
import com.duplicatefinder.services.DuplicateFinderTask;
import com.duplicatefinder.services.FileIconService;
import com.duplicatefinder.utils.FileUtils;
import com.duplicatefinder.utils.FormatUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.util.Callback;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainController {

    // UI Components
    private BorderPane root;
    private Button selectDirBtn;
    private Button scanBtn;
    private Button deleteAllBtn;

    private ListView<File> directoriesListView;
    private ProgressBar progressBar;
    private Label progressLabel;
    private Label statusLabel;
    private Label spaceSavedLabel;

    private TableView<DuplicateGroup> groupsTableView;
    private TableColumn<DuplicateGroup, String> hashColumn;
    private TableColumn<DuplicateGroup, Number> countColumn;

    private TableView<File> filesTableView;
    private TableColumn<File, ImageView> thumbnailColumn;
    private TableColumn<File, String> nameColumn;
    private TableColumn<File, String> pathColumn;
    private TableColumn<File, String> sizeColumnFiles;
    private TableColumn<File, String> modifiedColumn;

    // Data
    private ObservableList<DuplicateGroup> duplicateGroups = FXCollections.observableArrayList();
    private ObservableList<File> selectedDirectories = FXCollections.observableArrayList();
    private ObservableList<File> currentFiles = FXCollections.observableArrayList();

//    private FileScanner currentScanner;
    private DuplicateFinderTask duplicateFinderTask;
    private FileIconService fileIconService;

    // Constructor
    public MainController() {
        System.out.println("Creating MainController...");
        initialize();
    }

    private void initialize() {
        // Initialize services
        fileIconService = new FileIconService();

        // Build the UI
        root = createUI();

        // Setup table data
        setupTables();

        // Initialize stats
        updateStats();

        System.out.println("MainController initialized successfully");
    }

    // Public method to get the view
    public BorderPane getView() {
        return root;
    }

    // Create the complete UI
    private BorderPane createUI() {
        BorderPane borderPane = new BorderPane();
        borderPane.getStyleClass().add("main-container");

        // Top: Control Panel
        borderPane.setTop(createTopPanel());

        // Center: Tables in SplitPane
        borderPane.setCenter(createCenterPanel());

        // Bottom: Status Bar
        borderPane.setBottom(createBottomPanel());

        return borderPane;
    }

    private VBox createTopPanel() {
        VBox topPanel = new VBox(10);
        topPanel.setPadding(new Insets(15));
//        topPanel.setStyle("-fx-background-color: #f4f4f4;");

        // Button Row
        HBox buttonRow = createButtonRow();

        // Selected Directories
        VBox dirSection = createDirectorySection();

        // Progress Section
        VBox progressSection = createProgressSection();

        topPanel.getChildren().addAll(buttonRow, dirSection, progressSection);
        return topPanel;
    }


    private Button createButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        button.setPadding(new Insets(8, 16, 8, 16));
        return button;
    }

    private VBox createDirectorySection() {
        VBox dirSection = new VBox(5);
        directoriesListView = new ListView<>();
        directoriesListView.setItems(selectedDirectories);
        directoriesListView.setPrefHeight(60);
        directoriesListView.getStyleClass().add("directory-list");

        // Custom cell factory to show full path
        directoriesListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getAbsolutePath());
                }
            }
        });

        dirSection.getChildren().addAll(directoriesListView);
        return dirSection;
    }

    private VBox createProgressSection() {
        VBox progressSection = new VBox(5);

        progressBar = new ProgressBar(0);
        progressBar.getStyleClass().add("scan-progress");
        progressBar.setPrefWidth(Double.MAX_VALUE);

        progressLabel = new Label("Ready");
        progressLabel.getStyleClass().add("progress-label");

        progressSection.getChildren().addAll(progressBar, progressLabel);
        return progressSection;
    }

    private SplitPane createCenterPanel() {
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.25);

        // Top: Duplicate Groups Table
        VBox groupsPanel = createGroupsPanel();

        // Bottom: Files Table
        VBox filesPanel = createFilesPanel();

        splitPane.getItems().addAll(groupsPanel, filesPanel);
        return splitPane;
    }

    private VBox createGroupsPanel() {
        VBox groupsPanel = new VBox(5);
        groupsPanel.setPadding(new Insets(10));

        // Header


        // Create table
        groupsTableView = createGroupsTable();

        groupsPanel.getChildren().addAll(groupsTableView);

        // Make table expand
        VBox.setVgrow(groupsTableView, Priority.ALWAYS);

        return groupsPanel;
    }

    private TableView<DuplicateGroup> createGroupsTable() {
        TableView<DuplicateGroup> table = new TableView<>();
        table.getStyleClass().add("groups-table");

        // Hash column
        hashColumn = new TableColumn<>("Group ID");
        hashColumn.prefWidthProperty().bind(table.widthProperty().multiply(.7));
        hashColumn.setCellValueFactory(cellData -> {
            String hash = cellData.getValue().getFileHash();
            return new SimpleStringProperty(FormatUtils.formatHash(hash));
        });

        // Count column
        countColumn = new TableColumn<>("count");
        countColumn.prefWidthProperty().bind(table.widthProperty().multiply(.3));
        countColumn.setCellValueFactory(new PropertyValueFactory<>("fileCount"));


        // Add columns to table
        table.getColumns().addAll(hashColumn, countColumn);

        // Set selection listener
        table.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    onGroupSelected(newVal);
                });

        return table;
    }

    private VBox createFilesPanel() {
        VBox filesPanel = new VBox(5);
        filesPanel.setPadding(new Insets(10));



        // Create table
        filesTableView = createFilesTable();

        filesPanel.getChildren().addAll(filesTableView);

        // Make table expand
        VBox.setVgrow(filesTableView, Priority.ALWAYS);

        return filesPanel;
    }

    private TableView<File> createFilesTable() {
        TableView<File> table = new TableView<>();
        table.getStyleClass().add("files-table");

        // Thumbnail column
        thumbnailColumn = new TableColumn<>("");
        thumbnailColumn.prefWidthProperty().bind(table.widthProperty().multiply(.1));
        thumbnailColumn.setCellFactory(new Callback<>() {
            @Override
            public TableCell<File, ImageView> call(TableColumn<File, ImageView> param) {
                return new TableCell<>() {
                    @Override
                    protected void updateItem(ImageView item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || getTableRow() == null) {
                            setGraphic(null);
                        } else {
                            File fileData = getTableRow().getItem();
                            if (fileData != null && fileIconService != null) {
                                loadThumbnailForCell(fileData, this);
                            } else {
                                setGraphic(null);
                            }
                        }
                    }
                };
            }
        });

        // Name column
        nameColumn = new TableColumn<>("File Name");
        nameColumn.prefWidthProperty().bind(table.widthProperty().multiply(.35));
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));

        // Path column
        pathColumn = new TableColumn<>("Path");
        pathColumn.prefWidthProperty().bind(table.widthProperty().multiply(.35));
        pathColumn.setCellValueFactory(new PropertyValueFactory<>("path"));

        // Size column
        sizeColumnFiles = new TableColumn<>("Size");
        sizeColumnFiles.prefWidthProperty().bind(table.widthProperty().multiply(.05));
        sizeColumnFiles.setCellValueFactory(cellData ->
                new SimpleStringProperty(FormatUtils.formatFileSize(cellData.getValue().length())));

        // Modified column
        modifiedColumn = new TableColumn<>("Modified");
        modifiedColumn.prefWidthProperty().bind(table.widthProperty().multiply(.15));
        modifiedColumn.setCellValueFactory(cellData ->
        {
            try {
                return new SimpleStringProperty(LocalDateTime.ofInstant(Files.getLastModifiedTime(cellData.getValue().toPath()).toInstant(), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // Add columns to table
        table.getColumns().addAll(
                thumbnailColumn, nameColumn, pathColumn,
                sizeColumnFiles, modifiedColumn
        );

        // Enable multiple selection
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        return table;
    }

    private HBox createBottomPanel() {
        HBox bottomPanel = new HBox(15);
        bottomPanel.setPadding(new Insets(8, 15, 8, 15));
//        bottomPanel.setStyle("-fx-background-color: #f0f0f0;");
        bottomPanel.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label("Ready");
        statusLabel.getStyleClass().add("status-label");

        Separator separator1 = new Separator();
        separator1.setOrientation(javafx.geometry.Orientation.VERTICAL);

        spaceSavedLabel = new Label("Potential space: 0 B");
        spaceSavedLabel.getStyleClass().add("space-saved-label");

        bottomPanel.getChildren().addAll(statusLabel, separator1, spaceSavedLabel);
        return bottomPanel;
    }

    private void setupTables() {
        // Set data for tables
        groupsTableView.setItems(duplicateGroups);
        filesTableView.setItems(currentFiles);
    }

    // Event Handlers
    private void handleSelectDirectories() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Directory");

        // Get window from any node
        Node source = (Node) selectDirBtn;
        File directory = chooser.showDialog(source.getScene().getWindow());

        if (directory != null) {
            // Check if directory already exists in list
            boolean exists = selectedDirectories.stream()
                    .anyMatch(dir -> dir.getAbsolutePath().equals(directory.getAbsolutePath()));

            if (!exists) {
                selectedDirectories.add(directory);
                updateStatus("Added directory: " + directory.getName());

                // Debug: Print all selected directories
                System.out.println("Selected directories:");
                for (int i = 0; i < selectedDirectories.size(); i++) {
                    System.out.println("  [" + i + "] " + selectedDirectories.get(i).getAbsolutePath());
                }
            } else {
                updateStatus("Directory already in list: " + directory.getName());
            }
        }
    }

    private void handleStartScan() {
        startScanning();
    }

    private void handleDeleteAll() {
        deleteAllDuplicates();
    }

    // In MainController.java - Add this button to the button row
    private HBox createButtonRow() {
        HBox buttonRow = new HBox(10);
        buttonRow.setAlignment(Pos.CENTER_LEFT);
        // one label for counting files
        // Create buttons
        selectDirBtn = createButton("Select Directories", "control-button");
        selectDirBtn.setOnAction(e -> handleSelectDirectories());

        // Add Clear Directory button
        Button clearDirBtn = createButton("Clear Directories", "control-button");
        clearDirBtn.setOnAction(e -> handleClearDirectories());


        scanBtn = createButton("Find  Duplicates", "primary-button");
        scanBtn.setOnAction(e -> handleStartScan());

        deleteAllBtn = createButton("Delete All", "danger-button");
        deleteAllBtn.setOnAction(e -> handleDeleteAll());


        buttonRow.getChildren().addAll(
                selectDirBtn, clearDirBtn, scanBtn,
                 deleteAllBtn
        );

        return buttonRow;
    }

    // Add this method to handle clearing directories
    private void handleClearDirectories() {
        selectedDirectories.clear();
        directoriesListView.refresh();
        updateStatus("Cleared all directories from list");
    }

    // Update the handleStartScan method to show which directories are being scanned
    private void startScanning() {
        if (selectedDirectories.isEmpty()) {
            showAlert("Please select at least one directory to scan.");
            return;
        }

        if (duplicateFinderTask != null && duplicateFinderTask.isRunning()) {
            showAlert("Scan is already in progress.");
            return;
        }

        // Clear previous results
        clearPreviousResults();

        // Create scanner with ALL directories
        duplicateFinderTask = new DuplicateFinderTask(selectedDirectories);

        // Bind progress
        progressBar.progressProperty().bind(duplicateFinderTask.progressProperty());
        progressLabel.textProperty().bind(duplicateFinderTask.messageProperty());
        duplicateFinderTask.setOnSucceeded(event -> {
            try {
                List<DuplicateGroup> results = duplicateFinderTask.get();

                Platform.runLater(() -> {
                    duplicateGroups.setAll(results);
                    groupsTableView.refresh();

                    if (!duplicateGroups.isEmpty()) {
                        groupsTableView.getSelectionModel().select(0);
                    }

                    updateStatus("Scan completed! Found " + results.size() + " duplicate groups.");
                    updateStats();

                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    updateStatus("Error: " + e.getMessage());
                    showAlert("Scan failed: " + e.getMessage());
                });
                e.printStackTrace();
            } finally {
                Platform.runLater(() -> {
                    progressBar.progressProperty().unbind();
                    progressLabel.textProperty().unbind();
                    progressBar.setProgress(1.0);
                    progressLabel.setText("Ready");
                });
            }
        });

        duplicateFinderTask.setOnFailed(event -> {
            Platform.runLater(() -> {
                Throwable ex = duplicateFinderTask.getException();
                updateStatus("Scan failed: " + ex.getMessage());
                showAlert("Scan failed: " + ex.getMessage());
                progressBar.progressProperty().unbind();
                progressLabel.textProperty().unbind();
                progressBar.setProgress(0);
                progressLabel.setText("Failed");
            });
        });

        // Start scanning in background thread
        Thread scanThread = new Thread(duplicateFinderTask);
        scanThread.setDaemon(true);
        scanThread.start();

        updateStatus("Scanning started...");
    }

    private void deleteAllDuplicates() {
        List<File> allDuplicates = duplicateGroups.stream()
                .flatMap(group -> {
                    List<File> files = group.getFiles();
                    return files.size() > 1 ? files.subList(1, files.size()).stream() : java.util.stream.Stream.empty();
                })
                .collect(Collectors.toList());

        if (allDuplicates.isEmpty()) {
            showAlert("No duplicates found to delete.");
            return;
        }

        if (confirmDelete("Delete all " + allDuplicates.size() + " duplicate files?")) {
            List<Path> paths = allDuplicates.stream()
                    .map(File::toPath)
                    .collect(Collectors.toList());

            long freedSpace = FileUtils.deleteFiles(paths);
            duplicateGroups.clear();
            currentFiles.clear();
            groupsTableView.refresh();
            filesTableView.refresh();
            updateStats();
            updateStatus("Deleted all duplicates, freed " + FormatUtils.formatFileSize(freedSpace));
        }
    }

    private void clearPreviousResults() {
        duplicateGroups.clear();
        currentFiles.clear();
        if (fileIconService != null) {
            fileIconService.clearCache();
        }
        groupsTableView.getSelectionModel().clearSelection();
        groupsTableView.refresh();
        filesTableView.refresh();
        updateStats();
    }

    private void onGroupSelected(DuplicateGroup group) {
        if (group != null) {
            currentFiles.setAll(group.getFiles());
            filesTableView.refresh();
            System.out.println("Selected group with " + group.getFiles().size() + " files");
        } else {
            currentFiles.clear();
            System.out.println("Cleared file selection");
        }
    }

    private void loadThumbnailForCell(File fileData, TableCell<File, ImageView> cell) {
        if (fileIconService != null) {
            Task<ImageView> thumbnailTask = fileIconService.getThumbnailForFile(
                    Paths.get(fileData.getPath()));

            thumbnailTask.setOnSucceeded(event -> {
                ImageView thumbnail = thumbnailTask.getValue();
                if (thumbnail != null) {
                    Platform.runLater(() -> {
                        cell.setGraphic(thumbnail);
                    });
                }
            });

            thumbnailTask.setOnFailed(event -> {
                Platform.runLater(() -> {
                    cell.setGraphic(null);
                });
            });
        }
    }

    private void updateStats() {
        long wasteSpace = duplicateGroups.stream()
                .mapToLong(group -> group.getSize() * (group.getFileCount() - 1))
                .sum();
        spaceSavedLabel.setText("Potential space: " + FormatUtils.formatFileSize(wasteSpace));
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private boolean confirmDelete(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText(message);
        alert.setContentText("This action cannot be undone.");

        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Duplicate File Finder");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void cleanup() {
        if (fileIconService != null) {
            fileIconService.shutdown();
        }
    }
}