package com.hiresense.api.resume.parsing;

import static org.assertj.core.api.Assertions.assertThat;

import com.hiresense.api.testsupport.TestDocuments;
import org.junit.jupiter.api.Test;

class TextExtractionServiceTest {

    private final TextExtractionService service = new TextExtractionService();

    @Test
    void extractsTextFromMinimalPdf() {
        String text =
                service.extract(new java.io.ByteArrayInputStream(TestDocuments.pdfWithText("Hello Resume World")));

        assertThat(text).contains("Hello Resume World");
    }

    @Test
    void extractsTextFromMinimalDocx() {
        String text =
                service.extract(new java.io.ByteArrayInputStream(TestDocuments.docxWithText("Hello Docx Candidate")));

        assertThat(text).contains("Hello Docx Candidate");
    }

    @Test
    void plainTextFallsBackToRawContent() {
        String text = service.extract(new java.io.ByteArrayInputStream("plain notes content".getBytes()));

        assertThat(text).contains("plain notes content");
    }
}
