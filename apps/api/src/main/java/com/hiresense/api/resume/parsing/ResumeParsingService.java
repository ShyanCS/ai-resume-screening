package com.hiresense.api.resume.parsing;

import com.hiresense.api.resume.Resume;
import com.hiresense.api.resume.ResumeRepository;
import com.hiresense.api.resume.ResumeStatus;
import com.hiresense.api.skill.CandidateSkillService;
import com.hiresense.api.skill.SkillExtractionService;
import com.hiresense.api.skill.SkillSource;
import com.hiresense.api.storage.StorageService;
import java.io.InputStream;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResumeParsingService {

    private static final Logger log = LoggerFactory.getLogger(ResumeParsingService.class);

    private final ResumeRepository resumeRepository;
    private final StorageService storageService;
    private final TextExtractionService textExtractionService;
    private final SkillExtractionService skillExtractionService;
    private final CandidateSkillService candidateSkillService;

    public ResumeParsingService(
            ResumeRepository resumeRepository,
            StorageService storageService,
            TextExtractionService textExtractionService,
            SkillExtractionService skillExtractionService,
            CandidateSkillService candidateSkillService) {
        this.resumeRepository = resumeRepository;
        this.storageService = storageService;
        this.textExtractionService = textExtractionService;
        this.skillExtractionService = skillExtractionService;
        this.candidateSkillService = candidateSkillService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void parse(Long resumeId) {
        Resume resume = resumeRepository.findById(resumeId).orElse(null);
        if (resume == null || resume.getStatus() != ResumeStatus.UPLOADED) {
            return;
        }
        try (InputStream content = storageService.download(resume.getStorageKey())) {
            resume.setStatus(ResumeStatus.PARSING);
            resumeRepository.saveAndFlush(resume);

            String text = textExtractionService.extract(content);
            resume.setContentText(text);
            resume.setStatus(ResumeStatus.PARSED);
            resume.setParsedAt(Instant.now());
            resume.setParseError(null);
            resumeRepository.save(resume);

            var matchedSkills = skillExtractionService.extractSkills(text);
            candidateSkillService.attachSkills(resume.getUser().getId(), matchedSkills, SkillSource.RESUME_PARSED);
            log.info("Resume {} parsed: {} chars, {} skills matched", resumeId, text.length(), matchedSkills.size());
        } catch (Exception e) {
            log.error("Parsing failed for resume {}", resumeId, e);
            resume.setStatus(ResumeStatus.FAILED);
            resume.setParseError(e.getMessage());
            resumeRepository.save(resume);
        }
    }
}
