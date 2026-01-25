package com.duplicatefinder.services;

import javafx.concurrent.Task;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileIconService {
    private static final int ICON_SIZE = 40;

    private final ExecutorService executorService;
    private final Map<String, Image> iconCache;
    private final Map<String, Color> colorMap;

    public FileIconService() {
        this.executorService = Executors.newFixedThreadPool(4);
        this.iconCache = new HashMap<>();
        this.colorMap = new HashMap<>();

        initializeColorMap();
        createDefaultIcons();
    }

    private void initializeColorMap() {
        // File type colors
        colorMap.put("image", Color.web("#FF6B6B"));
        colorMap.put("audio", Color.web("#81C784"));
        colorMap.put("video", Color.web("#FFD54F"));
        colorMap.put("pdf", Color.web("#F44336"));
        colorMap.put("document", Color.web("#2196F3"));
        colorMap.put("spreadsheet", Color.web("#4CAF50"));
        colorMap.put("archive", Color.web("#795548"));
        colorMap.put("code", Color.web("#F57C00"));
        colorMap.put("executable", Color.web("#9C27B0"));
        colorMap.put("font", Color.web("#607D8B"));
        colorMap.put("file", Color.web("#78909C"));
    }

    private void createDefaultIcons() {
        // Create icons for all file types
        for (Map.Entry<String, Color> entry : colorMap.entrySet()) {
            String type = entry.getKey();
            Color color = entry.getValue();
            String text = getIconText(type);
            Image icon = createColoredIcon(color, text);
            iconCache.put(type, icon);
        }
    }

    private String getIconText(String fileType) {
        switch (fileType) {
            case "image":
                return "IMG";
            case "audio":
                return "AUD";
            case "video":
                return "VID";
            case "pdf":
                return "PDF";
            case "document":
                return "DOC";
            case "spreadsheet":
                return "XLS";
            case "archive":
                return "ZIP";
            case "code":
                return "CODE";
            case "executable":
                return "EXE";
            case "font":
                return "FONT";
            default:
                return "FILE";
        }
    }

    private Image createColoredIcon(Color color, String text) {
        Canvas canvas = new Canvas(ICON_SIZE, ICON_SIZE);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Draw rounded rectangle
        gc.setFill(color);
        gc.fillRoundRect(2, 2, ICON_SIZE - 4, ICON_SIZE - 4, 8, 8);

        // Add highlight
        gc.setFill(color.brighter().brighter());
        gc.fillRoundRect(2, 2, ICON_SIZE - 4, 6, 8, 8);

        // Draw text
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", ICON_SIZE * 0.25));

        // Center text
        double textWidth = text.length() * ICON_SIZE * 0.15;
        double textHeight = ICON_SIZE * 0.25;
        double x = (ICON_SIZE - textWidth) / 2;
        double y = (ICON_SIZE + textHeight) / 2 - 2;

        gc.fillText(text, x, y);

        return canvas.snapshot(null, null);
    }

    /**
     * Get thumbnail for file (asynchronous)
     */
    public Task<ImageView> getThumbnailForFile(Path filePath) {
        Task<ImageView> task = new Task<>() {
            @Override
            protected ImageView call() throws Exception {
                String fileName = filePath.getFileName().toString();
                String fileType = getFileType(fileName);

                // Get appropriate icon
                Image icon = iconCache.getOrDefault(fileType, iconCache.get("file"));

                // Create image view
                ImageView imageView = new ImageView(icon);
                imageView.setFitWidth(ICON_SIZE);
                imageView.setFitHeight(ICON_SIZE);
                imageView.setPreserveRatio(true);
                imageView.getStyleClass().add("thumbnail");

                // Add file type class for CSS styling
                imageView.getStyleClass().add("file-icon-" + fileType);

                return imageView;
            }
        };

        executorService.submit(task);
        return task;
    }

    private String getFileType(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();

        if (extension.matches("jpg|jpeg|png|gif|bmp|webp|tiff|tif|ico")) {
            return "image";
        } else if (extension.matches("mp3|wav|aac|flac|ogg|m4a")) {
            return "audio";
        } else if (extension.matches("mp4|avi|mov|mkv|wmv|webm")) {
            return "video";
        } else if (extension.equals("pdf")) {
            return "pdf";
        } else if (extension.matches("doc|docx|txt|rtf")) {
            return "document";
        } else if (extension.matches("xls|xlsx|csv")) {
            return "spreadsheet";
        } else if (extension.matches("zip|rar|7z|tar|gz")) {
            return "archive";
        } else if (extension.matches("java|py|cpp|html|css|js|json|xml")) {
            return "code";
        } else if (extension.matches("exe|msi|app|sh|bat|jar")) {
            return "executable";
        } else {
            return "file";
        }
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    public void clearCache() {
        iconCache.clear();
        createDefaultIcons(); // Recreate default icons
    }

    public void shutdown() {
        executorService.shutdown();
    }
}