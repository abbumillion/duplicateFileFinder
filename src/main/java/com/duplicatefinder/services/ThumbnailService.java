// SimpleThumbnailService.java
package com.duplicatefinder.services;

import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThumbnailService {
    private static final int THUMBNAIL_SIZE = 40;
    private static final long MAX_THUMBNAIL_SIZE = 10 * 1024 * 1024; // 10MB

    private final ExecutorService executorService;
    private final Map<String, Image> thumbnailCache;
    private final Map<String, Image> iconCache;

    public ThumbnailService() {
        this.executorService = Executors.newFixedThreadPool(4);
        this.thumbnailCache = new HashMap<>();
        this.iconCache = new HashMap<>();
        loadDefaultIcons();
    }

    private void loadDefaultIcons() {
        // Create simple colored icons for different file types
        createDefaultIcon("image", Color.web("#FF6B6B"), "IMG");
        createDefaultIcon("audio", Color.web("#81C784"), "AUD");
        createDefaultIcon("video", Color.web("#FFD54F"), "VID");
        createDefaultIcon("pdf", Color.web("#F44336"), "PDF");
        createDefaultIcon("document", Color.web("#2196F3"), "DOC");
        createDefaultIcon("spreadsheet", Color.web("#4CAF50"), "XLS");
        createDefaultIcon("archive", Color.web("#795548"), "ZIP");
        createDefaultIcon("code", Color.web("#F57C00"), "CODE");
        createDefaultIcon("file", Color.web("#78909C"), "FILE");
    }

    private void createDefaultIcon(String type, javafx.scene.paint.Color color, String text) {
        // Create a simple icon using JavaFX Canvas
        // In production, you'd use actual icon images
        iconCache.put(type, createColoredIcon(color, text));
    }

    private Image createColoredIcon(javafx.scene.paint.Color color, String text) {
        // Create a simple colored rectangle as icon
        // This is a placeholder - in real app, use proper icons
        int size = 32;
        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(size, size);
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(color);
        gc.fillRoundRect(2, 2, size - 4, size - 4, 6, 6);

        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", 10));
        gc.fillText(text, size / 4, size / 1.5);

        return canvas.snapshot(null, null);
    }

    public Task<ImageView> createThumbnailView(Path filePath) {
        Task<ImageView> task = new Task<>() {
            @Override
            protected ImageView call() throws Exception {
                String fileName = filePath.getFileName().toString();
                String cacheKey = filePath.toString();

                // Check cache first
                if (thumbnailCache.containsKey(cacheKey)) {
                    return createImageView(thumbnailCache.get(cacheKey));
                }

                try {
                    // Check if file is image
                    String mimeType = Files.probeContentType(filePath);
                    long fileSize = Files.size(filePath);

                    boolean isImage = mimeType != null && mimeType.startsWith("image/");
                    boolean canLoadThumbnail = isImage && fileSize <= MAX_THUMBNAIL_SIZE;

                    if (canLoadThumbnail) {
                        Image thumbnail = generateImageThumbnail(filePath);
                        thumbnailCache.put(cacheKey, thumbnail);
                        return createImageView(thumbnail);
                    } else {
                        // Use file type icon
                        String fileType = getFileType(fileName);
                        Image icon = iconCache.getOrDefault(fileType, iconCache.get("file"));
                        thumbnailCache.put(cacheKey, icon);
                        return createImageView(icon);
                    }
                } catch (Exception e) {
                    // Use default icon on error
                    Image defaultIcon = iconCache.get("file");
                    thumbnailCache.put(cacheKey, defaultIcon);
                    return createImageView(defaultIcon);
                }
            }
        };

        executorService.submit(task);
        return task;
    }

    private Image generateImageThumbnail(Path filePath) throws Exception {
        BufferedImage originalImage = ImageIO.read(filePath.toFile());
        if (originalImage == null) {
            return iconCache.get("file");
        }

        // Calculate scaling
        double scale = Math.min(
                (double) THUMBNAIL_SIZE / originalImage.getWidth(),
                (double) THUMBNAIL_SIZE / originalImage.getHeight()
        );

        int scaledWidth = (int) (originalImage.getWidth() * scale);
        int scaledHeight = (int) (originalImage.getHeight() * scale);

        java.awt.Image scaledImage = originalImage.getScaledInstance(
                scaledWidth, scaledHeight, java.awt.Image.SCALE_SMOOTH
        );

        BufferedImage outputImage = new BufferedImage(
                THUMBNAIL_SIZE, THUMBNAIL_SIZE, BufferedImage.TYPE_INT_ARGB
        );

        int x = (THUMBNAIL_SIZE - scaledWidth) / 2;
        int y = (THUMBNAIL_SIZE - scaledHeight) / 2;

        outputImage.getGraphics().drawImage(scaledImage, x, y, scaledWidth, scaledHeight, null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(outputImage, "png", baos);
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        return new Image(is, THUMBNAIL_SIZE, THUMBNAIL_SIZE, true, true);
    }

    private ImageView createImageView(Image image) {
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(THUMBNAIL_SIZE);
        imageView.setFitHeight(THUMBNAIL_SIZE);
        imageView.setPreserveRatio(true);
        imageView.getStyleClass().add("thumbnail");
        return imageView;
    }

    private String getFileType(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();

        if (extension.matches("jpg|jpeg|png|gif|bmp|webp")) {
            return "image";
        } else if (extension.matches("mp3|wav|aac|flac|ogg")) {
            return "audio";
        } else if (extension.matches("mp4|avi|mov|mkv|wmv")) {
            return "video";
        } else if (extension.equals("pdf")) {
            return "pdf";
        } else if (extension.matches("doc|docx|txt|rtf")) {
            return "document";
        } else if (extension.matches("xls|xlsx|csv")) {
            return "spreadsheet";
        } else if (extension.matches("zip|rar|7z|tar")) {
            return "archive";
        } else if (extension.matches("java|py|cpp|html|css|js")) {
            return "code";
        } else {
            return "file";
        }
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

}