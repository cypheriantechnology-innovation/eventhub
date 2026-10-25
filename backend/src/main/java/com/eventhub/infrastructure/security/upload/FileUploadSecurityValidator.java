package com.eventhub.infrastructure.security.upload;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;

/**
 * Validates uploaded files to prevent RCE, webshells, Path Traversal, and polyglot attacks.
 * Features:
 * 1. Filename sanitization against directory traversal and null-byte injection.
 * 2. Extension whitelisting (.csv, .json, .parquet, .xml, .txt).
 * 3. Binary magic bytes header inspection rejecting executable formats (MZ, ELF, Mach-O, Shell).
 */
@Component
public class FileUploadSecurityValidator {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "csv", "json", "parquet", "xml", "txt"
    );

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50MB

    // Executable and script magic byte signatures to reject
    private static final byte[] MAGIC_MZ = new byte[]{(byte) 0x4D, (byte) 0x5A}; // Windows PE/EXE/DLL
    private static final byte[] MAGIC_ELF = new byte[]{(byte) 0x7F, (byte) 0x45, (byte) 0x4C, (byte) 0x46}; // Linux ELF
    private static final byte[] MAGIC_SHELL = new byte[]{(byte) 0x23, (byte) 0x21}; // Shebang #!

    public record ValidationResult(boolean isValid, String sanitizedFilename, String errorMessage) {}

    public ValidationResult validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return new ValidationResult(false, null, "File is empty or missing");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return new ValidationResult(false, null, "File exceeds maximum size limit of 50MB");
        }

        String rawFilename = file.getOriginalFilename();
        if (rawFilename == null || rawFilename.isBlank()) {
            return new ValidationResult(false, null, "Filename cannot be empty");
        }

        // 1. Sanitize filename: block path traversal and null bytes
        if (rawFilename.contains("\0") || rawFilename.contains("..") || rawFilename.contains("/") || rawFilename.contains("\\")) {
            return new ValidationResult(false, null, "Malicious path traversal or illegal characters detected in filename");
        }

        String cleanFilename = rawFilename.replaceAll("[^a-zA-Z0-9._-]", "_");

        // 2. Validate extension
        int dotIndex = cleanFilename.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == cleanFilename.length() - 1) {
            return new ValidationResult(false, cleanFilename, "File must have a valid extension");
        }

        String extension = cleanFilename.substring(dotIndex + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return new ValidationResult(false, cleanFilename,
                    "File extension '." + extension + "' is not permitted. Allowed: " + ALLOWED_EXTENSIONS);
        }

        // 3. Inspect binary magic bytes
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int bytesRead = is.read(header);
            if (bytesRead >= 2) {
                if (matchesSignature(header, MAGIC_MZ)) {
                    return new ValidationResult(false, cleanFilename, "Executable binary (DOS/PE) detected and rejected");
                }
                if (bytesRead >= 4 && matchesSignature(header, MAGIC_ELF)) {
                    return new ValidationResult(false, cleanFilename, "Linux executable binary (ELF) detected and rejected");
                }
                if (matchesSignature(header, MAGIC_SHELL)) {
                    return new ValidationResult(false, cleanFilename, "Shell script detected and rejected");
                }
                // Mach-O signatures: 0xFEEDFACE, 0xFEEDFACF, 0xCEFAEDFE, 0xCFFAEDFE
                if (bytesRead >= 4) {
                    if ((header[0] == (byte) 0xFE && header[1] == (byte) 0xED && header[2] == (byte) 0xFA) ||
                        (header[0] == (byte) 0xCF && header[1] == (byte) 0xFA && header[2] == (byte) 0xED && header[3] == (byte) 0xFE)) {
                        return new ValidationResult(false, cleanFilename, "Mach-O executable binary detected and rejected");
                    }
                }
            }
        } catch (IOException e) {
            return new ValidationResult(false, cleanFilename, "Failed to inspect file stream: " + e.getMessage());
        }

        return new ValidationResult(true, cleanFilename, null);
    }

    private boolean matchesSignature(byte[] data, byte[] signature) {
        if (data.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) {
            if (data[i] != signature[i]) return false;
        }
        return true;
    }
}
