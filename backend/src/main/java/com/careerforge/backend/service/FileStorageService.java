package com.careerforge.backend.service;

import com.careerforge.backend.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F', '-'};

    private final Path root;

    public FileStorageService(@Value("${app.upload.dir}") String dir) {
        try {
            this.root = Paths.get(dir).toAbsolutePath().normalize();
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create the upload folder: " + dir, e);
        }
    }

    /** Checks the file really is a PDF, saves it under a random name, returns that name. */
    public String storePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a PDF file to upload");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The PDF must be 2 MB or smaller");
        }
        try (InputStream in = file.getInputStream()) {
            byte[] head = new byte[PDF_MAGIC.length];
            if (in.readNBytes(head, 0, head.length) < head.length || !Arrays.equals(head, PDF_MAGIC)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "This file is not a valid PDF");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        String name = UUID.randomUUID() + ".pdf";
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, resolve(name));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return name;
    }

    /** A name for display only. It is never used to build a file path. */
    public String displayName(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) return "resume.pdf";
        name = name.replaceAll("[\\p{Cntrl}\\\\/]", "_").trim();
        return name.length() > 100 ? name.substring(0, 100) : name;
    }

    public byte[] read(String storedName) {
        try {
            return Files.readAllBytes(resolve(storedName));
        } catch (NoSuchFileException e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Resume not found");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void deleteQuietly(String storedName) {
        if (storedName == null) return;
        try {
            Files.deleteIfExists(resolve(storedName));
        } catch (IOException ignored) {
            // a leftover file is harmless; it is not reachable through the API
        }
    }

    private Path resolve(String name) {
        Path path = root.resolve(name).normalize();
        if (!path.startsWith(root)) throw new IllegalArgumentException("Invalid file name");
        return path;
    }
}