package com.maplog.common.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String store(MultipartFile file);
    void delete(String fileUrl);
    String generatePresignedUrl(String fileUrl);
}
