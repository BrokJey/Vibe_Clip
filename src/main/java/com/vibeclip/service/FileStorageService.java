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
            log.info("Каталог загрузки инициализирован: {}", this.uploadDir);
        } catch (IOException e) {
            log.error("Не удалось создать каталог для загрузки: {}", this.uploadDir, e);
            throw new RuntimeException("Не удалось создать каталог для загрузки", e);
        }
    }

    public String storeFile(MultipartFile file, String prefix) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Файл пустой");
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

            log.info("Файл сохранен: {}", filename);
            return "/uploads/" + filename;
        } catch (IOException e) {
            log.error("Не удалось сохранить файл", e);
            throw new RuntimeException("Не удалось сохранить файл", e);
        }
    }

    public Path getFilePath(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) {
            throw new IllegalArgumentException("Не верная ссылка URL: " + fileUrl);
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
            log.info("Файл удален: {}", filename);
        } catch (IOException e) {
            log.error("Ошибка удаления файла: {}", fileUrl, e);
        }
    }

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
                log.info("Извлечено миниатюрное изображение: {}", thumbnailFilename);
                return "/uploads/" + thumbnailFilename;
            } else {
                log.warn("Не удалось извлечь миниатюру, код: {}", exitCode);
                // Пытаемся извлечь кадр на 0 секунде
                return extractThumbnailAtTime(videoPath, "00:00:00", thumbnailFilename);
            }
        } catch (Exception e) {
            log.error("Ошибка при извлечении миниатюры из видео: {}", videoPath, e);
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
                log.info("Миниатюра извлечена в момент времени {}: {}", time, thumbnailFilename);
                return "/uploads/" + thumbnailFilename;
            }
        } catch (Exception e) {
            log.error("Ошибка при извлечении миниатюры {}: {}", time, e);
        }
        return null;
    }
}

