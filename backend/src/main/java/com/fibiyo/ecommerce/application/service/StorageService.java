package com.fibiyo.ecommerce.application.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.stream.Stream; // Stream importu eklendi

public interface StorageService {

    /**
     * Initializes the storage system (e.g., creates the root directory if it doesn't exist).
     */
    void init();

    /**
     * Stores an image file, applying compression and resizing.
     *
     * @param file           The image file to store.
     * @param subDirectory   The subfolder within the root storage location.
     * @param targetWidth    Target maximum width for resizing.
     * @param targetHeight   Target maximum height for resizing.
     * @param quality        Compression quality (0.0f to 1.0f for JPEGs).
     * @return The relative path (including subfolder) of the stored and compressed image.
     */
    String storeImage(MultipartFile file, String subDirectory, int targetWidth, int targetHeight, float quality);

    /**
     * Stores a generic file with validation based on fileTypeHint.
     * This is the method currently used by other services.
     *
     * @param file           The file to store.
     * @param subDirectory   The subfolder to store the file in.
     * @param fileTypeHint   A hint about the file type (e.g., "image", "video") for validation.
     * @return The relative path (including subfolder) of the stored file.
     */
    String storeFile(MultipartFile file, String subDirectory, String fileTypeHint);

    /**
     * Stores a file from a byte array.
     *
     * @param bytes            The byte array of the file.
     * @param subDirectory     The subfolder to store the file in.
     * @param fileExtensionHint The extension of the file (e.g., "jpg", "png").
     * @return The relative path (including subfolder) of the stored file.
     */
    String store(byte[] bytes, String subDirectory, String fileExtensionHint);

    /**
     * Loads all files' paths in the root location (first level only).
     * @return A stream of paths.
     */
    Stream<Path> loadAll();

    /**
     * Loads a single file path.
     * @param filePath The path relative to the root storage location.
     * @return The resolved path.
     */
    Path load(String filePath);

    Resource loadAsResource(String filePath);

    void deleteFile(String filePath);

    /**
     * Deletes all files in the storage root directory.
     */
    void deleteAll();

    String generateUrl(String filePath);

    Path getRootLocation();
}
