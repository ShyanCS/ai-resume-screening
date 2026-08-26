package com.hiresense.api.storage;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import java.io.InputStream;

public class MinioStorageService implements StorageService {

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
        } catch (Exception e) {
            return false;
        }
    }
}
