package com.unibook.publisher.production.service;

import com.unibook.publisher.common.exception.storage.FileReadException;
import com.unibook.publisher.storage.FileStorageService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class RevisionTextReader {
    private static final String UTF8_BOM = "\uFEFF";
    private final FileStorageService fileStorageService;

    public RevisionTextReader(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public String readText(String path) {
        try (InputStream inputStream = fileStorageService.get(path)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).replace(UTF8_BOM, "");
        } catch (IOException e) {
            throw new FileReadException(path, e);
        }
    }
}
