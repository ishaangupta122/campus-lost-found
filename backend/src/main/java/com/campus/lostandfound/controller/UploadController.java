package com.campus.lostandfound.controller;

import java.time.Duration;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.campus.lostandfound.dto.UploadResponse;
import com.campus.lostandfound.service.ImageStorageService;

/**
 * Image upload and download. HTTP only; every rule is in ImageStorageService.
 *
 * POST /api/uploads               (logged in) multipart field "file" -> { "imagePath": "/api/uploads/files/..." }
 * GET  /api/uploads/files/{name}  (public)    the picture itself, so an <img> tag can show it
 */
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final ImageStorageService imageStorageService;

    public UploadController(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        String imagePath = imageStorageService.store(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(new UploadResponse(imagePath));
    }

    @GetMapping("/files/{filename}")
    public ResponseEntity<Resource> download(@PathVariable String filename) {
        Resource image = imageStorageService.load(filename);
        return ResponseEntity.ok()
                .contentType(ImageStorageService.mediaTypeFor(filename))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(image);
    }
}
