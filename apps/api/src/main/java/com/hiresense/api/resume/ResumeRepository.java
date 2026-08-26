package com.hiresense.api.resume;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    List<Resume> findAllByUserIdOrderByUploadedAtDesc(Long userId);
}
