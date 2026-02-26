package com.maplog.common.storage;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.maplog.common.exception.BusinessException;
import com.maplog.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.UUID;

@Service
@org.springframework.context.annotation.Profile({"dev", "aws"})
@RequiredArgsConstructor
public class S3FileStorageService implements FileStorageService {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase()
                : "";

        try {
            byte[] fileBytes;
            String uploadExtension;
            String contentType;

            if (extension.equals(".heic") || extension.equals(".heif")) {
                fileBytes = convertHeicToPng(file);
                uploadExtension = ".png";
                contentType = "image/png";
            } else {
                fileBytes = file.getBytes();
                uploadExtension = extension;
                contentType = file.getContentType();
            }

            String key = "diaries/" + UUID.randomUUID() + uploadExtension;

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(fileBytes.length);

            amazonS3.putObject(bucket, key, new ByteArrayInputStream(fileBytes), metadata);

            return amazonS3.getUrl(bucket, key).toString();

        } catch (IOException | InterruptedException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || !fileUrl.contains(bucket)) return;
        String key = extractKey(fileUrl);
        try {
            amazonS3.deleteObject(bucket, key);
        } catch (Exception ignored) {
        }
    }

    @Override
    public String generatePresignedUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.contains(bucket)) return fileUrl;

        String key = extractKey(fileUrl);

        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 60 * 60; // 1시간
        expiration.setTime(expTimeMillis);

        return amazonS3.generatePresignedUrl(bucket, key, expiration, HttpMethod.GET).toString();
    }

    private String extractKey(String fileUrl) {
        // AWS v1 getUrl은 보통 https://bucket.s3.region.amazonaws.com/key 형태임
        if (fileUrl.contains(".com/")) {
            return fileUrl.substring(fileUrl.lastIndexOf(".com/") + 5);
        }
        return fileUrl;
    }

    private byte[] convertHeicToPng(MultipartFile file) throws IOException, InterruptedException {
      File tempHeic = File.createTempFile("upload-", ".heic");
      File tempPng = File.createTempFile("upload-", ".png");
      try {
        file.transferTo(tempHeic);
        ProcessBuilder pb = new ProcessBuilder("convert", tempHeic.getAbsolutePath(), tempPng.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        return Files.readAllBytes(tempPng.toPath());
      } finally {
        tempHeic.delete();
        tempPng.delete();
      }
    }
}
