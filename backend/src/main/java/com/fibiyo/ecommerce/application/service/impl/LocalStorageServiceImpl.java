package com.fibiyo.ecommerce.application.service.impl;

import com.fibiyo.ecommerce.application.exception.BadRequestException;
import com.fibiyo.ecommerce.application.exception.ResourceNotFoundException;
import com.fibiyo.ecommerce.application.exception.StorageException;
import com.fibiyo.ecommerce.application.service.StorageService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import net.coobird.thumbnailator.Thumbnails; // Thumbnailator importu
import org.apache.commons.io.FilenameUtils; // FilenameUtils importu eklendi (UUID için değil, uzantı almak için)

@Service
public class LocalStorageServiceImpl implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(LocalStorageServiceImpl.class);

    @Value("${file.upload-dir:./uploads/}") // Default value added
    private String uploadDir;

    @Value("${file.serve-path:/uploads/}")
    private String servePath;

    private Path rootLocation;

    // File Type Constants
    private static final String FILE_TYPE_IMAGE = "image";
    private static final String FILE_TYPE_VIDEO = "video";
    private static final String FILE_TYPE_THUMBNAIL = "thumbnail";

    // Configuration for different file types
    private static final long MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final long MAX_VIDEO_SIZE_BYTES = 50 * 1024 * 1024; // 50 MB (adjust as needed)
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    private static final List<String> ALLOWED_VIDEO_EXTENSIONS = Arrays.asList("mp4", "mov", "webm");

    @Override
    @PostConstruct
    public void init() {
        try {
            if (!StringUtils.hasText(this.uploadDir)) {
                logger.error("Configuration error: 'file.upload-dir' property is not set or empty.");
                throw new StorageException("Storage location configuration is missing.");
            }
            this.rootLocation = Paths.get(this.uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(rootLocation)) {
                Files.createDirectories(rootLocation);
                logger.info("Created root upload directory: {}", rootLocation);
            } else {
                logger.info("Root upload directory already exists: {}", rootLocation);
            }
            if (!Files.isWritable(rootLocation)) {
                logger.error("Root upload directory '{}' is not writable.", rootLocation);
                throw new StorageException("Storage location is not writable.");
            }
        } catch (IOException e) {
            logger.error("Could not initialize storage location: '{}'", this.uploadDir, e);
            throw new StorageException("Could not initialize storage location: " + this.uploadDir, e);
        } catch (Exception e) {
            logger.error("Unexpected error during storage initialization for path: '{}'", this.uploadDir, e);
            throw new StorageException("Unexpected error during storage initialization.", e);
        }
    }

    @Override
    public String storeFile(@NonNull MultipartFile file, String subDirectory, String fileTypeHint) { // Bu metod arayüzle uyumlu
        Objects.requireNonNull(file, "File cannot be null");
        if (file.isEmpty()) {
            throw new BadRequestException("Cannot store empty file.");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (extension == null) {
            extension = ""; // Handle files with no extension
        }
        extension = extension.toLowerCase();

        validateFile(file, extension, fileTypeHint);

        String uniqueFilenameWithoutExtension = UUID.randomUUID().toString();
        String uniqueFilenameWithExtension = uniqueFilenameWithoutExtension + "." + extension;

        Path targetSubDirectory = this.rootLocation;
        String relativePathWithFilename;

        if (StringUtils.hasText(subDirectory)) {
            targetSubDirectory = this.rootLocation.resolve(subDirectory).normalize();
            relativePathWithFilename = Paths.get(subDirectory, uniqueFilenameWithExtension).toString();
        } else {
            relativePathWithFilename = uniqueFilenameWithExtension;
        }

        try {
            if (!Files.exists(targetSubDirectory)) {
                Files.createDirectories(targetSubDirectory);
                logger.info("Created subdirectory: {}", targetSubDirectory);
            }

            Path destinationFile = targetSubDirectory.resolve(uniqueFilenameWithExtension).normalize().toAbsolutePath();

            if (!destinationFile.getParent().startsWith(this.rootLocation.toAbsolutePath())) {
                logger.error("SECURITY ALERT: Path Traversal attempt! Destination: '{}' is outside root: '{}'", destinationFile, this.rootLocation);
                throw new StorageException("Cannot store file outside designated directory structure.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Successfully stored file '{}' to '{}'", originalFilename, destinationFile);
            }
            return relativePathWithFilename.replace("\\", "/"); // Return path relative to root upload dir
        } catch (IOException e) {
            logger.error("Failed to store file '{}': {}", originalFilename, e.getMessage(), e);
            throw new StorageException("Failed to store file " + originalFilename, e);
        }
    }

    @Override
    public String store(byte[] bytes, String subDirectory, String fileExtensionHint) { // Bu metod arayüzle uyumlu
        if (bytes == null || bytes.length == 0) {
            throw new StorageException("Failed to store empty byte array.");
        }

        String extension = StringUtils.hasText(fileExtensionHint) ? ("." + fileExtensionHint.replace(".", "")) : "";
        String filename = UUID.randomUUID().toString() + extension;
        Path targetDirectory = this.rootLocation.resolve(Paths.get(subDirectory)).normalize();

        try {
            Files.createDirectories(targetDirectory); // Ensure subdirectory exists
            Path destinationFile = targetDirectory.resolve(filename).normalize();

            if (!destinationFile.getParent().equals(targetDirectory)) {
                throw new StorageException("Cannot store file outside current directory.");
            }
            try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return Paths.get(subDirectory).resolve(filename).toString().replace("\\", "/"); // Return relative path
        } catch (IOException e) {
            throw new StorageException("Failed to store byte array as file " + filename, e);
        }
    }

    private void validateFile(MultipartFile file, String extension, String fileTypeHint) {
        long maxSize;
        List<String> allowedExtensions;

        switch (fileTypeHint.toLowerCase()) {
            case FILE_TYPE_VIDEO:
                maxSize = MAX_VIDEO_SIZE_BYTES;
                allowedExtensions = ALLOWED_VIDEO_EXTENSIONS;
                break;
            case FILE_TYPE_IMAGE:
            case FILE_TYPE_THUMBNAIL: // Thumbnails are images
                maxSize = MAX_IMAGE_SIZE_BYTES;
                allowedExtensions = ALLOWED_IMAGE_EXTENSIONS;
                break;
            default:
                // If fileTypeHint is not recognized, we might skip validation or use a default restrictive one.
                // For now, let's assume if it's not video or image, it's not validated here.
                // This part might need refinement based on how other file types are handled.
                logger.warn("Unrecognized fileTypeHint for validation: {}", fileTypeHint);
                return; // Or throw new BadRequestException("Invalid file type hint: " + fileTypeHint);
        }

        if (!allowedExtensions.contains(extension)) {
            throw new BadRequestException("Invalid file extension: " + extension + ". Allowed for " + fileTypeHint + ": " + String.join(", ", allowedExtensions));
        }

        if (file.getSize() > maxSize) {
            throw new BadRequestException("File size exceeds the maximum limit for " + fileTypeHint + " (Max: " + (maxSize / 1024 / 1024) + "MB).");
        }
    }

    @Override
    public Stream<Path> loadAll() { // Bu metod arayüzle uyumlu
        try {
            return Files.walk(this.rootLocation, 1)
                    .filter(path -> !path.equals(this.rootLocation))
                    .map(this.rootLocation::relativize);
        } catch (IOException e) {
            logger.error("Failed to read stored files", e);
            throw new StorageException("Failed to read stored files", e);
        }
    }

    @Override
    public Path load(@NonNull String filePath) { // Bu metod arayüzle uyumlu
        Objects.requireNonNull(filePath, "File path cannot be null");
        return rootLocation.resolve(filePath).normalize();
    }

    @Override
    public Resource loadAsResource(@NonNull String filePath) { // Bu metod arayüzle uyumlu
        Objects.requireNonNull(filePath, "File path cannot be null");
        try {
            Path file = load(filePath);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                logger.error("Could not read file: {}", filePath);
                throw new ResourceNotFoundException("Could not read file: " + filePath);
            }
        } catch (MalformedURLException e) {
            logger.error("Could not create URL for file: {} (MalformedURLException)", filePath, e);
            throw new StorageException("Could not read file: " + filePath, e);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error loading file as resource: {}", filePath, e);
            throw new StorageException("Error loading file as resource: " + filePath, e);
        }
    }

    @Override
    public String generateUrl(@NonNull String filePath) { // Bu metod arayüzle uyumlu
        Objects.requireNonNull(filePath, "File path cannot be null");
        if (!StringUtils.hasText(filePath)) {
            return null;
        }

        String cleanServePath = servePath.startsWith("/") ? servePath : "/" + servePath;
        cleanServePath = cleanServePath.endsWith("/") ? cleanServePath : cleanServePath + "/";

        // Ensure filePath does not start with a slash if cleanServePath ends with one
        String cleanFilePath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
        cleanFilePath = cleanFilePath.replace("\\", "/"); // Ensure forward slashes

        try {
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path(cleanServePath)
                    .path(cleanFilePath)
                    .toUriString();
        } catch (Exception e) {
            logger.warn("Could not generate URL using ServletUriComponentsBuilder (request context might be missing). Falling back to relative path. FilePath: {}", filePath);
            String backendBaseUrl = "http://localhost:8080"; // Consider making this configurable
            return backendBaseUrl + cleanServePath + cleanFilePath;
        }
    }

    @Override
    public void deleteFile(@NonNull String filePath) throws StorageException { // Bu metod arayüzle uyumlu
        Objects.requireNonNull(filePath, "File path cannot be null");
        if (!StringUtils.hasText(filePath)) {
            logger.warn("Attempted to delete null or empty file path.");
            return;
        }
        try {
            Path fileToDelete = load(filePath);
            if (!fileToDelete.startsWith(this.rootLocation)) {
                 logger.error("SECURITY ALERT: Attempt to delete file outside root directory! Path: '{}'", filePath);
                 throw new StorageException("Cannot delete file outside designated directory.");
            }
            boolean deleted = Files.deleteIfExists(fileToDelete);
            if (deleted) {
                logger.info("Deleted file: {}", filePath);
            } else {
                logger.warn("File to delete not found: {}", filePath);
            }
        } catch (IOException e) {
            logger.error("Could not delete file: {}", filePath, e);
            throw new StorageException("Could not delete file: " + filePath, e);
        }
    }

    @Override
    public void deleteAll() { // Bu metod arayüzle uyumlu
        try {
            FileSystemUtils.deleteRecursively(rootLocation);
            logger.info("Deleted all files and directories under: {}", rootLocation);
            init(); // Re-initialize the root directory
        } catch (IOException e) {
            logger.error("Could not delete all files: {}", e.getMessage(), e);
            throw new StorageException("Could not delete all files.", e);
        }
    }

    // Yeni eklenen storeImage metodu
    @Override
    public String storeImage(MultipartFile file, String subDirectory, int targetWidth, int targetHeight, float quality) {
        Objects.requireNonNull(file, "File cannot be null");
        if (file.isEmpty()) {
            throw new BadRequestException("Cannot store empty image file.");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = FilenameUtils.getExtension(originalFilename);
        if (extension == null || !ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BadRequestException("Invalid image file extension: " + extension + ". Allowed: " + String.join(", ", ALLOWED_IMAGE_EXTENSIONS));
        }
        extension = extension.toLowerCase(); // Güvenlik için

        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
             throw new BadRequestException("Image file size exceeds the maximum limit (Max: " + (MAX_IMAGE_SIZE_BYTES / 1024 / 1024) + "MB).");
        }

        String uniqueFilenameWithoutExtension = UUID.randomUUID().toString();
        String uniqueFilenameWithExtension = uniqueFilenameWithoutExtension + "." + extension;

        Path targetSubDir = this.rootLocation;
        String relativePathWithFilename;

        if (StringUtils.hasText(subDirectory)) {
            targetSubDir = this.rootLocation.resolve(subDirectory).normalize();
            relativePathWithFilename = Paths.get(subDirectory, uniqueFilenameWithExtension).toString();
        } else {
            relativePathWithFilename = uniqueFilenameWithExtension;
        }
        
        try {
            if (!Files.exists(targetSubDir)) {
                Files.createDirectories(targetSubDir);
                logger.info("Created subdirectory for image: {}", targetSubDir);
            }

            Path destinationFile = targetSubDir.resolve(uniqueFilenameWithExtension).normalize().toAbsolutePath();

            if (!destinationFile.getParent().startsWith(this.rootLocation.toAbsolutePath())) {
                logger.error("SECURITY ALERT: Path Traversal attempt for image! Destination: '{}' is outside root: '{}'", destinationFile, this.rootLocation);
                throw new StorageException("Cannot store image file outside designated directory structure.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Thumbnails.of(inputStream)
                        .size(targetWidth, targetHeight)
                        .outputQuality(quality)
                        .outputFormat(extension) // Sıkıştırılmış formatı orijinalle aynı tut
                        .toFile(destinationFile.toFile());
                logger.info("Successfully stored and compressed image '{}' to '{}'", originalFilename, destinationFile);
            }
            return relativePathWithFilename.replace("\\", "/");
        } catch (IOException e) {
            logger.error("Failed to store and compress image '{}': {}", originalFilename, e.getMessage(), e);
            throw new StorageException("Failed to store and compress image " + originalFilename, e);
        }
    }

    // Yeni eklenen getRootLocation metodu
    @Override
    public Path getRootLocation() {
        return this.rootLocation;
    }

    @Override
    public String getDefaultImagePlaceholder() {
        // Eğer projenizde assets altında veya uploads altında sabit bir placeholder.png gibi
        // bir dosya varsa, onun adını (veya göreli yolunu) dönebilirsiniz.
        // Örneğin: return "placeholders/default-image.png";
        // Şimdilik, özel bir placeholder olmadığını varsayarak null dönelim.
        // Bu durumda, CategoryServiceImpl'deki kontrol, null olmayan her imageUrl'u silmeye çalışacaktır.
        return null; 
    }
}
