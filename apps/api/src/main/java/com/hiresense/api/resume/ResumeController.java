package com.hiresense.api.resume;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeController {

    public record ResumeResponse(
            Long id, String originalFilename, long fileSizeBytes, String status, String uploadedAt) {}

    private final ResumeUploadService resumeUploadService;

    public ResumeController(ResumeUploadService resumeUploadService) {
        this.resumeUploadService = resumeUploadService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeResponse> upload(@RequestPart("file") MultipartFile file) {
        Resume saved = resumeUploadService.upload(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @GetMapping
    public List<ResumeResponse> listMine() {
        return resumeUploadService.listMine().stream()
                .map(ResumeController::toResponse)
                .toList();
    }

    static ResumeResponse toResponse(Resume resume) {
        return new ResumeResponse(
                resume.getId(),
                resume.getOriginalFilename(),
                resume.getFileSizeBytes(),
                resume.getStatus().name(),
                resume.getUploadedAt().toString());
    }
}
