package com.campus.lostandfound.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.campus.lostandfound.exception.BadRequestException;
import com.campus.lostandfound.exception.ResourceNotFoundException;

/** Uses a temporary folder, so no real uploads are touched. */
class ImageStorageServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};

    @TempDir
    Path folder;

    private ImageStorageService service;

    @BeforeEach
    void setUp() {
        service = new ImageStorageService(folder.toString());
    }

    private static MockMultipartFile upload(String name, byte[] content) {
        return new MockMultipartFile("file", name, "image/png", content);
    }

    private Path fileOf(String imagePath) throws Exception {
        return service.load(imagePath.substring(ImageStorageService.URL_PREFIX.length())).getFile().toPath();
    }

    @Test
    void storesAnImageUnderARandomNameWithTheRealExtension() throws Exception {
        // the client calls it .txt, but the first bytes say PNG, so we trust the bytes
        String imagePath = service.store(upload("evil.txt", PNG));

        assertTrue(imagePath.startsWith(ImageStorageService.URL_PREFIX));
        assertTrue(imagePath.endsWith(".png"));
        assertTrue(Files.exists(fileOf(imagePath)));
    }

    @Test
    void detectsJpeg() {
        assertTrue(service.store(upload("photo.png", JPEG)).endsWith(".jpg"));
    }

    @Test
    void rejectsFilesThatAreNotImages() {
        byte[] text = "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);
        assertThrows(BadRequestException.class, () -> service.store(upload("a.png", text)));
    }

    @Test
    void rejectsEmptyFiles() {
        assertThrows(BadRequestException.class, () -> service.store(upload("a.png", new byte[0])));
        assertThrows(BadRequestException.class, () -> service.store(null));
    }

    @Test
    void rejectsFilesOverFiveMegabytes() {
        byte[] big = new byte[(int) ImageStorageService.MAX_BYTES + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);
        assertThrows(BadRequestException.class, () -> service.store(upload("big.png", big)));
    }

    @Test
    void loadRefusesNamesThatAreNotOurs() {
        assertThrows(ResourceNotFoundException.class, () -> service.load("../secret.txt"));
        assertThrows(ResourceNotFoundException.class, () -> service.load("anything.png"));
        assertThrows(ResourceNotFoundException.class,
                () -> service.load("00000000-0000-0000-0000-000000000000.png")); // right shape, no such file
    }

    @Test
    void deleteOrphansKeepsUsedAndRecentFiles() throws Exception {
        String used = service.store(upload("a.png", PNG));
        String unused = service.store(upload("b.png", PNG));
        String recent = service.store(upload("c.png", PNG));

        FileTime old = FileTime.from(Instant.now().minus(Duration.ofDays(2)));
        Files.setLastModifiedTime(fileOf(used), old);
        Files.setLastModifiedTime(fileOf(unused), old);

        int deleted = service.deleteOrphans(path -> path.equals(used), Duration.ofHours(24));

        assertEquals(1, deleted);
        assertFalse(Files.exists(folder.resolve(unused.substring(ImageStorageService.URL_PREFIX.length()))));
        assertTrue(Files.exists(fileOf(used)));    // still used by a report
        assertTrue(Files.exists(fileOf(recent)));  // too new to judge
    }

    @Test
    void createsTheUploadFolderWhenItIsMissing() throws Exception {
        Path missing = folder.resolve("nested").resolve("uploads");
        ImageStorageService fresh = new ImageStorageService(missing.toString());
        assertTrue(Files.isDirectory(missing));      // created at startup

        Files.delete(missing);                        // folder removed while the server runs
        String imagePath = fresh.store(upload("a.png", PNG));

        assertTrue(Files.isDirectory(missing));      // re-created before saving
        assertTrue(Files.exists(missing.resolve(imagePath.substring(ImageStorageService.URL_PREFIX.length()))));
    }

    @Test
    void deleteByImagePathRemovesOurFilesAndIgnoresOtherValues() throws Exception {
        String imagePath = service.store(upload("a.png", PNG));
        Path file = fileOf(imagePath);

        service.deleteByImagePath("https://example.com/photo.png"); // a web link: ignored
        service.deleteByImagePath(null);
        assertTrue(Files.exists(file));

        service.deleteByImagePath(imagePath);
        assertFalse(Files.exists(file));
    }
}
