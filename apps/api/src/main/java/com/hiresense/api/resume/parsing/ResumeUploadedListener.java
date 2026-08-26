package com.hiresense.api.resume.parsing;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ResumeUploadedListener {

    private final ResumeParsingService resumeParsingService;

    public ResumeUploadedListener(ResumeParsingService resumeParsingService) {
        this.resumeParsingService = resumeParsingService;
    }

    @Async("resumeParsingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onResumeUploaded(ResumeUploadedEvent event) {
        resumeParsingService.parse(event.resumeId());
    }
}
