package com.unibook.publisher.production.controller;

import com.unibook.publisher.production.entity.response.FileUrlResponse;
import com.unibook.publisher.storage.FileStorageService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;

@RestController
@RequestMapping("/api/v1/storage")
public class StorageController {

    private final FileStorageService fileStorageService;

    public StorageController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/files/url")
    public ResponseEntity<FileUrlResponse> getPresignedUrl(@RequestParam("path") String path) {
        String url = fileStorageService.getPresignedUrl(path);
        return ResponseEntity.ok(new FileUrlResponse(url));
    }

    @GetMapping("/raw") // Використовуємо як аналог getPresignedUrl для локальних файлів
    public ResponseEntity<InputStreamResource> getRawFile(@RequestParam("path") String path) {
        InputStream inputStream = fileStorageService.get(path);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + extractFilename(path) + "\"")
                .body(new InputStreamResource(inputStream));
    }

    private String extractFilename(String path) {
        return path.contains("/") ? path.substring(path.lastIndexOf("/") + 1) : path;
    }
}