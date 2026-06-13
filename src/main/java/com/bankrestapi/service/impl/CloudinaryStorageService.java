package com.bankrestapi.service.impl;

import com.bankrestapi.service.*;

import com.bankrestapi.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "cloudinary")
public class CloudinaryStorageService implements StorageService {
    private final RestClient restClient = RestClient.create();
    private final String cloudName;
    private final String uploadPreset;

    public CloudinaryStorageService(@Value("${app.storage.cloudinary-cloud-name}") String cloudName,
                                    @Value("${app.storage.cloudinary-upload-preset}") String uploadPreset) {
        this.cloudName = cloudName;
        this.uploadPreset = uploadPreset;
    }

    @Override
    public String upload(MultipartFile file) {
        if (file.isEmpty() || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "KYC document must be a non-empty image");
        }
        try {
            var body = new LinkedMultiValueMap<String, Object>();
            body.add("upload_preset", uploadPreset);
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override public String getFilename() { return file.getOriginalFilename(); }
            });
            Map<?, ?> response = restClient.post()
                    .uri("https://api.cloudinary.com/v1_1/{cloud}/image/upload", cloudName)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body).retrieve().body(Map.class);
            Object url = response == null ? null : response.get("secure_url");
            if (url == null) throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "Cloud storage returned no URL");
            return url.toString();
        } catch (IOException | RuntimeException ex) {
            if (ex instanceof BusinessException business) throw business;
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "Cloud storage upload failed");
        }
    }
}
