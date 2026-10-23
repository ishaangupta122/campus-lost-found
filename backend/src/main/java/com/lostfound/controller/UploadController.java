package com.lostfound.controller;

import java.util.Map;

import org.springframework.core.io.Resource;
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

import com.lostfound.service.ImageStorageService;

/** Picture upload (login required) and download (public, so <img> works). */
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final ImageStorageService imageStorageService;

    public UploadController(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        String imagePath = imageStorageService.store(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("imagePath", imagePath));
    }

    @GetMapping("/files/{filename}")
    public ResponseEntity<Resource> download(@PathVariable String filename) {
        Resource image = imageStorageService.load(filename);
        return ResponseEntity.ok()
                .contentType(ImageStorageService.mediaTypeFor(filename))
                .header("X-Content-Type-Options", "nosniff")
                .body(image);
    }
}
