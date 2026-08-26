package com.hiresense.api.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIf(value = "com.hiresense.api.testsupport.MinioAvailability#isReachable")
class StorageServiceIntegrationTest {

    @Autowired
    private StorageService storageService;

    @Test
    void uploadDownloadRoundTripPreservesContent() {
        String key = "resumes/test/" + UUID.randomUUID() + ".txt";
        byte[] payload = "resume text content".getBytes(StandardCharsets.UTF_8);

        storageService.upload(key, new ByteArrayInputStream(payload), payload.length, "text/plain");

        assertThat(storageService.exists(key)).isTrue();

        byte[] downloaded = readAll(storageService.download(key));
        assertThat(downloaded).isEqualTo(payload);
    }

    @Test
    void existsReturnsFalseForUnknownKey() {
        assertThat(storageService.exists("resumes/definitely-missing-" + UUID.randomUUID() + ".pdf"))
                .isFalse();
    }

    @Test
    void downloadUnknownKeyThrowsStorageException() {
        String key = "resumes/missing-" + UUID.randomUUID() + ".pdf";
        assertThatThrownBy(() -> storageService.download(key)).isInstanceOf(StorageException.class);
    }

    private static byte[] readAll(InputStream inputStream) {
        try (inputStream) {
            return inputStream.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
