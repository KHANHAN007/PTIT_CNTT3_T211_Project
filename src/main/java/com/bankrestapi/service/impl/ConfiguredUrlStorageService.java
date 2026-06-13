package com.bankrestapi.service.impl;

import com.bankrestapi.service.*;

import com.bankrestapi.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.io.IOException;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class ConfiguredUrlStorageService implements StorageService {
    private final String baseUrl;
    private final Path directory;

    public ConfiguredUrlStorageService(@Value("${app.storage.base-url}") String baseUrl,
                                       @Value("${app.storage.local-dir}") String directory) {
        this.baseUrl = baseUrl;
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override
    public String upload(MultipartFile file) {
        if (file.isEmpty() || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "KYC document must be a non-empty image");
        }
        String name = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
        String storedName = UUID.randomUUID() + "-" + name;
        try {
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), directory.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
            return baseUrl + "/" + storedName;
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store KYC document");
        }
    }
}
