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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class FileStorageService {

    private static final int FFMPEG_TIMEOUT_SECONDS = 90;

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
            String extension = resolveFileExtension(file);
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

    /**
     * Определяет расширение по имени файла или MIME (Android часто шлёт upload*.tmp).
     */
    private String resolveFileExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (!".tmp".equals(ext) && !".temp".equals(ext)) {
                return ext;
            }
        }
        return extensionFromContentType(file.getContentType());
    }

    private String extensionFromContentType(String contentType) {
        if (contentType == null) {
            return ".mp4";
        }
        String ct = contentType.toLowerCase();
        if (ct.contains("mp4") || ct.contains("mpeg4")) {
            return ".mp4";
        }
        if (ct.contains("quicktime")) {
            return ".mov";
        }
        if (ct.contains("webm")) {
            return ".webm";
        }
        if (ct.contains("3gpp")) {
            return ".3gp";
        }
        if (ct.contains("jpeg") || ct.contains("jpg")) {
            return ".jpg";
        }
        if (ct.contains("png")) {
            return ".png";
        }
        if (ct.contains("webp")) {
            return ".webp";
        }
        return ".mp4";
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
        String thumbnailFilename = "thumb-" + UUID.randomUUID() + ".jpg";
        Path thumbnailPath = this.uploadDir.resolve(thumbnailFilename);

        log.info("Извлечение превью из {} …", videoPath.getFileName());
        int exitCode = runFfmpegExtract(videoPath, thumbnailPath, "00:00:01");
        if (exitCode == 0 && Files.exists(thumbnailPath)) {
            log.info("Извлечено миниатюрное изображение: {}", thumbnailFilename);
            return "/uploads/" + thumbnailFilename;
        }

        log.warn("Превью на 1 с не получено (код {}), пробуем кадр 0 с", exitCode);
        exitCode = runFfmpegExtract(videoPath, thumbnailPath, "00:00:00");
        if (exitCode == 0 && Files.exists(thumbnailPath)) {
            log.info("Миниатюра извлечена в 00:00:00: {}", thumbnailFilename);
            return "/uploads/" + thumbnailFilename;
        }

        log.warn("Не удалось извлечь превью из {}", videoPath.getFileName());
        return null;
    }

    /**
     * Запуск ffmpeg без зависания: stderr/stdout сбрасываются, есть таймаут.
     * Без чтения stderr процесс часто блокируется на Windows при заполнении буфера.
     */
    private int runFfmpegExtract(Path videoPath, Path thumbnailPath, String seekTime) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    List.of(
                            "ffmpeg",
                            "-hide_banner",
                            "-loglevel", "error",
                            "-nostdin",
                            "-y",
                            "-ss", seekTime,
                            "-i", videoPath.toString(),
                            "-frames:v", "1",
                            "-q:v", "5",
                            thumbnailPath.toString()
                    )
            );
            processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);

            Process process = processBuilder.start();
            boolean finished = process.waitFor(FFMPEG_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("ffmpeg превысил таймаут {} с для {}", FFMPEG_TIMEOUT_SECONDS, videoPath.getFileName());
                return -1;
            }
            return process.exitValue();
        } catch (IOException e) {
            log.error("Не удалось запустить ffmpeg для {}", videoPath, e);
            return -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Прервано ожидание ffmpeg для {}", videoPath, e);
            return -1;
        }
    }
}