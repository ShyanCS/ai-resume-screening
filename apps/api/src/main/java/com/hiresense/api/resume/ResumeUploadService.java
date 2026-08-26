package com.hiresense.api.resume;

import com.hiresense.api.auth.CurrentUser;
import com.hiresense.api.storage.StorageService;
import com.hiresense.api.user.UserRepository;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ResumeUploadService {

    private static final byte[] PDF_MAGIC = new byte[] {37, 80, 68, 70};
    private static final byte[] ZIP_MAGIC = new byte[] {80, 75, 3, 4};

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final ApplicationEventPublisher eventPublisher;
    private final long maxSizeBytes;

    public ResumeUploadService(
            ResumeRepository resumeRepository,
            UserRepository userRepository,
            StorageService storageService,
            ApplicationEventPublisher eventPublisher,
            @Value("${app.resumes.max-size-bytes:10485760}") long maxSizeBytes) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.eventPublisher = eventPublisher;
        this.maxSizeBytes = maxSizeBytes;
    }

    @Transactional
    public Resume upload(MultipartFile file) {
        Long userId = CurrentUser.id();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (file == null || file.isEmpty()) {
            throw new InvalidResumeFileException("File is required");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new InvalidResumeFileException("File exceeds the maximum size of " + maxSizeBytes + " bytes");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new InvalidResumeFileException("Filename is required");
        }
        String extension = extensionOf(originalFilename);
        if (!extension.equals("pdf") && !extension.equals("docx")) {
            throw new InvalidResumeFileException("Only PDF and DOCX files are accepted");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new InvalidResumeFileException("Unable to read uploaded file");
        }
        validateMagicBytes(extension, content);

        var user = userRepository.findById(userId).orElseThrow();
        String storageKey = "resumes/" + userId + "/" + UUID.randomUUID() + "." + extension;
        storageService.upload(
                storageKey, new java.io.ByteArrayInputStream(content), content.length, file.getContentType());

        Resume resume = new Resume(user, storageKey, originalFilename, file.getContentType(), content.length);
        Resume saved = resumeRepository.save(resume);
        eventPublisher.publishEvent(new com.hiresense.api.resume.parsing.ResumeUploadedEvent(saved.getId()));
        return saved;
    }

    @Transactional(readOnly = true)
    public java.util.List<Resume> listMine() {
        Long userId = CurrentUser.id();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return resumeRepository.findAllByUserIdOrderByUploadedAtDesc(userId);
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static void validateMagicBytes(String extension, byte[] content) {
        boolean pdf = startsWith(content, PDF_MAGIC);
        boolean zip = startsWith(content, ZIP_MAGIC);
        if (extension.equals("pdf") && !pdf) {
            throw new InvalidResumeFileException("File content does not look like a valid PDF");
        }
        if (extension.equals("docx") && !zip) {
            throw new InvalidResumeFileException("File content does not look like a valid DOCX");
        }
    }

    private static boolean startsWith(byte[] content, byte[] prefix) {
        return content.length >= prefix.length && Arrays.equals(Arrays.copyOf(content, prefix.length), prefix);
    }
}
