package com.campus.lostandfound.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
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

import com.campus.lostandfound.exception.BadRequestException;
import com.campus.lostandfound.exception.ResourceNotFoundException;

/**
 * Keeps uploaded pictures on this server's disk (folder app.upload.dir). Nothing goes to
 * the database or to any outside service; the item only stores the picture's URL path.
 *
 * Safety rules:
 *  - the real file type is checked from the file's first bytes, not from its name or the
 *    Content-Type the client claims;
 *  - the stored name is a random UUID plus a known extension, so a client can never choose
 *    a file name or a folder (no path traversal);
 *  - files are limited to 5 MB.
 */
@Service
public class ImageStorageService {

    /** Pictures are served from this URL path (see UploadController). */
    public static final String URL_PREFIX = "/api/uploads/files/";

    static final long MAX_BYTES = 5L * 1024 * 1024;

    private static final Logger log = LoggerFactory.getLogger(ImageStorageService.class);
    private static final Pattern STORED_NAME = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|gif|webp)$");

    private final Path directory;

    public ImageStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.directory = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create the upload folder " + directory, e);
        }
    }

    /** Saves the picture and returns its URL path, e.g. /api/uploads/files/3f2a....png */
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

    /** Finds a stored picture by its file name; 404 when the name is not one of ours or the file is gone. */
    public Resource load(String filename) {
        if (!STORED_NAME.matcher(filename).matches()) {
            throw new ResourceNotFoundException("Image not found");
        }
        Path file = directory.resolve(filename).normalize();
        if (!file.startsWith(directory) || !Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("Image not found");
        }
        return new FileSystemResource(file);
    }

    /** Deletes the file behind an imagePath such as /api/uploads/files/x.png. Other values (for example web links) are ignored. */
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

    /**
     * Deletes files that no report uses any more (for example the user uploaded a picture and
     * never saved the form). Only files older than minAge are touched, so a form that is still
     * being filled in is safe. Returns how many files were deleted.
     */
    public int deleteOrphans(Predicate<String> isUsed, Duration minAge) {
        Instant cutoff = Instant.now().minus(minAge);
        int deleted = 0;
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

    /** The Content-Type to send when serving a stored file. */
    public static MediaType mediaTypeFor(String filename) {
        if (filename.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (filename.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        if (filename.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }

    /** Looks at the first bytes ("magic numbers") of the file to find out what it really is. */
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
