package com.careerforge.backend.service;

import com.careerforge.backend.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @TempDir
    Path dir;

    private FileStorageService storage() {
        return new FileStorageService(dir.toString());
    }

    private MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, "application/pdf", content);
    }

    @Test
    void aRealPdfIsStoredUnderARandomName() throws Exception {
        byte[] content = "%PDF-1.4 hello".getBytes(StandardCharsets.US_ASCII);
        String stored = storage().storePdf(file("cv.pdf", content));

        assertTrue(stored.endsWith(".pdf"));
        assertNotEquals("cv.pdf", stored);
        assertArrayEquals(content, Files.readAllBytes(dir.resolve(stored)));
    }

    @Test
    void whatWasStoredCanBeReadBack() {
        byte[] content = "%PDF-1.7 data".getBytes(StandardCharsets.US_ASCII);
        FileStorageService storage = storage();
        String stored = storage.storePdf(file("cv.pdf", content));
        assertArrayEquals(content, storage.read(stored));
    }

    @Test
    void aFileThatIsNotReallyAPdfIsRejected() {
        byte[] fake = "just some text, not a pdf".getBytes(StandardCharsets.US_ASCII);
        assertThrows(ApiException.class, () -> storage().storePdf(file("cv.pdf", fake)));
    }

    @Test
    void anEmptyFileIsRejected() {
        assertThrows(ApiException.class, () -> storage().storePdf(file("cv.pdf", new byte[0])));
    }

    @Test
    void aFileOverTwoMegabytesIsRejected() {
        byte[] big = new byte[2 * 1024 * 1024 + 1];
        byte[] header = "%PDF-".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(header, 0, big, 0, header.length);
        assertThrows(ApiException.class, () -> storage().storePdf(file("big.pdf", big)));
    }

    @Test
    void pathTricksInAStoredNameAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> storage().read("../secret.txt"));
    }

    @Test
    void displayNameLosesPathCharacters() {
        String name = storage().displayName(file("..\\evil/cv.pdf", "%PDF-1".getBytes(StandardCharsets.US_ASCII)));
        assertFalse(name.contains("/"));
        assertFalse(name.contains("\\"));
    }

    @Test
    void deletedFilesAreGone() {
        FileStorageService storage = storage();
        String stored = storage.storePdf(file("cv.pdf", "%PDF-1.4 x".getBytes(StandardCharsets.US_ASCII)));
        storage.deleteQuietly(stored);
        assertFalse(Files.exists(dir.resolve(stored)));
    }
}