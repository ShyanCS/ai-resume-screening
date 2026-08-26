package com.hiresense.api.storage;

import java.io.InputStream;

public interface StorageService {

    void upload(String key, InputStream content, long size, String contentType);

    InputStream download(String key);

    boolean exists(String key);
}
