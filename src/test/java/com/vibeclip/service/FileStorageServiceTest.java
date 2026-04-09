package com.vibeclip.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FileStorageServiceTest {

    @Test
    void storeFile_success(@TempDir Path tempDir) throws Exception {
        FileStorageService service = new FileStorageService(tempDir.toString());

        MultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test content".getBytes()
        );

        String result = service.storeFile(file, "avatar");

        assertNotNull(result);
        assertTrue(result.startsWith("/uploads/avatar-"));

        String filename = result.replace("/uploads/", "");
        Path storedFile = tempDir.resolve(filename);

        assertTrue(Files.exists(storedFile));
    }

    @Test
    void storeFile_empty_shouldThrow(@TempDir Path tempDir) {
        FileStorageService service = new FileStorageService(tempDir.toString());

        MultipartFile file = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        assertThrows(IllegalArgumentException.class, () -> service.storeFile(file, "test"));
    }

    @Test
    void getFilePath_success(@TempDir Path tempDir) {
        FileStorageService service = new FileStorageService(tempDir.toString());

        String fileUrl = "/uploads/test.jpg";

        Path path = service.getFilePath(fileUrl);

        assertEquals(tempDir.resolve("test.jpg"), path);
    }

    @Test
    void getFilePath_invalid_shouldThrow(@TempDir Path tempDir) {
        FileStorageService service = new FileStorageService(tempDir.toString());

        assertThrows(IllegalArgumentException.class, () -> service.getFilePath("invalid-path"));
    }

    @Test
    void deleteFile_success(@TempDir Path tempDir) throws Exception {
        FileStorageService service = new FileStorageService(tempDir.toString());

        Path file = tempDir.resolve("test.jpg");
        Files.createFile(file);

        service.deleteFile("/uploads/test.jpg");

        assertFalse(Files.exists(file));
    }

    @Test
    void deleteFile_invalid_shouldDoNothing(@TempDir Path tempDir) {
        FileStorageService service = new FileStorageService(tempDir.toString());

        assertDoesNotThrow(() -> service.deleteFile("invalid-url"));
    }

    @Test
    void extractThumbnailFromVideo_invalidFile_shouldReturnNull(@TempDir Path tempDir) {
        FileStorageService service = new FileStorageService(tempDir.toString());

        Path fakeVideo = tempDir.resolve("fake.mp4");

        String result = service.extractThumbnailFromVideo(fakeVideo);

        assertNull(result);
    }
}
