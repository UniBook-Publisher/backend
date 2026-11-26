package com.unibook.publisher.production.service;

import com.unibook.publisher.common.exception.storage.FileReadException;
import com.unibook.publisher.storage.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RevisionTextReaderTest {

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private RevisionTextReader revisionTextReader;

    @Test
    void readText_Success() {
        String textWithBom = "\uFEFFЗаголовок розділу";
        String path = "path/to/file.txt";
        when(fileStorageService.get(path)).thenReturn(new ByteArrayInputStream(textWithBom.getBytes(StandardCharsets.UTF_8)));
        String result = revisionTextReader.readText(path);
        assertEquals("Заголовок розділу", result);
    }

    @Test
    void readText_ThrowsFileReadException() {
        String path = "path/to/corrupted.txt";

        InputStream failingStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("S3 or file system read error");
            }
        };

        when(fileStorageService.get(path)).thenReturn(failingStream);
        FileReadException exception = assertThrows(FileReadException.class, () -> revisionTextReader.readText(path));
        assertEquals(IOException.class, exception.getCause().getClass());
    }
}
