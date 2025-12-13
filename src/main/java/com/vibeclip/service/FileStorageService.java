package com.vibeclip.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(@Value("${vibeclip.upload.dir:uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
            log.info("Upload directory initialized: {}", this.uploadDir);
        } catch (IOException e) {
            log.error("Could not create upload directory: {}", this.uploadDir, e);
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    public String storeFile(MultipartFile file, String prefix) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String filename = prefix + "-" + UUID.randomUUID() + extension;
            Path targetLocation = this.uploadDir.resolve(filename);

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("File stored: {}", filename);
            return "/uploads/" + filename;
        } catch (IOException e) {
            log.error("Failed to store file", e);
            throw new RuntimeException("Failed to store file", e);
        }
    }

    /**
     * Получает абсолютный путь к файлу по его URL
     */
    public Path getFilePath(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) {
            throw new IllegalArgumentException("Invalid file URL: " + fileUrl);
        }
        String filename = fileUrl.substring("/uploads/".length());
        return this.uploadDir.resolve(filename);
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) {
            return;
        }

        try {
            String filename = fileUrl.substring("/uploads/".length());
            Path filePath = this.uploadDir.resolve(filename);
            Files.deleteIfExists(filePath);
            log.info("File deleted: {}", filename);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", fileUrl, e);
        }
    }

    /**
     * Извлекает первый кадр из видео и сохраняет как изображение
     * @param videoPath путь к видеофайлу
     * @return URL сохраненного превью
     */
    public String extractThumbnailFromVideo(Path videoPath) {
        try {
            String thumbnailFilename = "thumb-" + UUID.randomUUID() + ".jpg";
            Path thumbnailPath = this.uploadDir.resolve(thumbnailFilename);

            // Используем FFmpeg для извлечения первого кадра
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ffmpeg",
                    "-i", videoPath.toString(),
                    "-ss", "00:00:01", // Берем кадр на 1 секунде (на случай черного экрана в начале)
                    "-vframes", "1",
                    "-q:v", "2", // Качество JPEG (2 = высокое качество)
                    "-y", // Перезаписать файл, если существует
                    thumbnailPath.toString()
            );

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0 && Files.exists(thumbnailPath)) {
                log.info("Thumbnail extracted: {}", thumbnailFilename);
                return "/uploads/" + thumbnailFilename;
            } else {
                log.warn("Failed to extract thumbnail, exit code: {}", exitCode);
                // Пытаемся извлечь кадр на 0 секунде
                return extractThumbnailAtTime(videoPath, "00:00:00", thumbnailFilename);
            }
        } catch (Exception e) {
            log.error("Error extracting thumbnail from video: {}", videoPath, e);
            // Если FFmpeg недоступен, возвращаем null
            return null;
        }
    }

    private String extractThumbnailAtTime(Path videoPath, String time, String thumbnailFilename) {
        try {
            Path thumbnailPath = this.uploadDir.resolve(thumbnailFilename);
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ffmpeg",
                    "-i", videoPath.toString(),
                    "-ss", time,
                    "-vframes", "1",
                    "-q:v", "2",
                    "-y",
                    thumbnailPath.toString()
            );

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0 && Files.exists(thumbnailPath)) {
                log.info("Thumbnail extracted at time {}: {}", time, thumbnailFilename);
                return "/uploads/" + thumbnailFilename;
            }
        } catch (Exception e) {
            log.error("Error extracting thumbnail at time {}: {}", time, e);
        }
        return null;
    }
}

