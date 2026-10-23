package com.lostfound.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.lostfound.exception.BadRequestException;

/**
 * Stores pictures on the server's disk; the item keeps only the URL path.
 * Type is checked from the file's first bytes, names are random UUIDs, max 5 MB.
 */
@Service
public class ImageStorageService {

    /** URL path the pictures are served from. */
    public static final String URL_PREFIX = "/api/uploads/files/";

    static final long MAX_BYTES = 5L * 1024 * 1024;

    private static final Logger log = LoggerFactory.getLogger(ImageStorageService.class);
    private static final Pattern STORED_NAME = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|gif|webp)$");

    private final Path directory;

    public ImageStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.directory = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(directory); // git keeps no empty folders
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create the upload folder " + directory, e);
        }
    }

    /** Saves the picture and returns its URL path. */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please choose an image to upload");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("The image is too large. The limit is 5 MB.");
        }

        String extension;
        try (InputStream in = file.getInputStream()) {
            extension = detectExtension(in.readNBytes(12));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the uploaded file", e);
        }
        if (extension == null) {
            throw new BadRequestException("Only JPG, PNG, GIF or WebP images are allowed");
        }

        String filename = UUID.randomUUID() + "." + extension;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, directory.resolve(filename));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save the uploaded file", e);
        }
        return URL_PREFIX + filename;
    }

    /** Finds a stored picture; 404 if unknown. */
    public Resource load(String filename) {
        if (!STORED_NAME.matcher(filename).matches()) {
            throw new NoSuchElementException("Image not found");
        }
        Path file = directory.resolve(filename);
        if (!Files.isRegularFile(file)) {
            throw new NoSuchElementException("Image not found");
        }
        return new FileSystemResource(file);
    }

    /** Deletes the file behind an imagePath (other values are ignored). */
    public void deleteByImagePath(String imagePath) {
        if (imagePath == null || !imagePath.startsWith(URL_PREFIX)) {
            return;
        }
        String filename = imagePath.substring(URL_PREFIX.length());
        if (!STORED_NAME.matcher(filename).matches()) {
            return;
        }
        try {
            Files.deleteIfExists(directory.resolve(filename));
        } catch (IOException e) {
            log.warn("Could not delete {}: {}", filename, e.getMessage());
        }
    }

    /** Deletes files no report uses and older than minAge; returns how many. */
    public int deleteOrphans(Predicate<String> isUsed, Duration minAge) {
        Instant cutoff = Instant.now().minus(minAge);
        int deleted = 0;
        if (!Files.isDirectory(directory)) {
            return 0; // nothing uploaded yet
        }
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                String name = file.getFileName().toString();
                if (!STORED_NAME.matcher(name).matches() || !Files.isRegularFile(file)) {
                    continue;
                }
                if (Files.getLastModifiedTime(file).toInstant().isAfter(cutoff) || isUsed.test(URL_PREFIX + name)) {
                    continue;
                }
                Files.deleteIfExists(file);
                deleted++;
            }
        } catch (IOException e) {
            log.warn("Could not clean the upload folder: {}", e.getMessage());
        }
        return deleted;
    }

    /** Content-Type for a stored file. */
    public static MediaType mediaTypeFor(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1);
        return MediaType.parseMediaType("image/" + (extension.equals("jpg") ? "jpeg" : extension));
    }

    /** Finds the real type from the first bytes ("magic numbers"). */
    private static String detectExtension(byte[] h) {
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (h.length >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G'
                && h[4] == 0x0D && h[5] == 0x0A && h[6] == 0x1A && h[7] == 0x0A) {
            return "png";
        }
        if (h.length >= 4 && h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8') {
            return "gif";
        }
        if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
            return "webp";
        }
        return null;
    }
}
