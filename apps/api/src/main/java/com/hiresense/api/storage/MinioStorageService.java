package com.hiresense.api.storage;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinioStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    private final MinioClient client;
    private final String bucket;

    public MinioStorageService(MinioClient client, String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public void upload(String key, InputStream content, long size, String contentType) {
        try {
            client.putObject(PutObjectArgs.builder().bucket(bucket).object(key).stream(content, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Upload failed for key " + key, e);
        }
    }

    @Override
    public InputStream download(String key) {
        try {
            return client.getObject(
                    GetObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception e) {
            throw new StorageException("Download failed for key " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            client.statObject(
                    StatObjectArgs.builder().bucket(bucket).object(key).build());
            return true;
        } catch (io.minio.errors.ErrorResponseException e) {
            String code = e.errorResponse() != null ? e.errorResponse().code() : "";
            log.warn("Exists check miss for key '{}' in bucket '{}': code={}", key, bucket, code);
            if ("NoSuchKey".equals(code) || "NoSuchBucket".equals(code)) {
                return false;
            }
            throw new StorageException("Exists check failed for key " + key, e);
        } catch (Exception e) {
            throw new StorageException("Exists check failed for key " + key, e);
        }
    }
}
