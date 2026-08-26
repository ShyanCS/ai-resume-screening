package com.hiresense.api.resume;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hiresense.api.storage.StorageService;
import com.jayway.jsonpath.JsonPath;
import java.io.InputStream;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.resumes.max-size-bytes=200")
@Transactional
@EnabledIf(value = "com.hiresense.api.testsupport.DatabaseAvailability#isReachable")
class ResumeUploadIntegrationTest {

    private static final byte[] PDF_BYTES = {'%', 'P', 'D', 'F', '-', '1', '.', '4'};

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private StorageService storageService;

    private String accessTokenFor(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"%s\",\"password\":\"sup3rSecret!\"}".formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return (String) JsonPath.read(body, "$.accessToken");
    }

    private void signup(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/candidate")
                        .contentType("application/json")
                        .content("{\"email\":\"%s\",\"password\":\"sup3rSecret!\",\"fullName\":\"Resume User\"}"
                                .formatted(email)))
                .andExpect(status().isCreated());
    }

    private MockMultipartFile fakePdf(String name, byte[] contentOverride) {
        byte[] content = contentOverride != null ? contentOverride : PDF_BYTES;
        return new MockMultipartFile("file", name, "application/pdf", content);
    }

    @Test
    void authenticatedCandidateUploadsValidPdf() throws Exception {
        signup("resume.up@example.com");
        String token = accessTokenFor("resume.up@example.com");

        String response = mockMvc.perform(multipart("/api/v1/resumes")
                        .file(fakePdf("my-cv.pdf", null))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename").value("my-cv.pdf"))
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long resumeId = Long.valueOf((Integer) JsonPath.read(response, "$.id"));
        var resume = resumeRepository.findById(resumeId).orElseThrow();
        assertThat(storageService.exists(resume.getStorageKey())).isTrue();

        try (InputStream downloaded = storageService.download(resume.getStorageKey())) {
            assertThat(downloaded.readAllBytes()).isEqualTo(PDF_BYTES);
        }
    }

    @Test
    void rejectsNonPdfContentWithPdfExtension() throws Exception {
        signup("resume.badpdf@example.com");
        String token = accessTokenFor("resume.badpdf@example.com");

        mockMvc.perform(multipart("/api/v1/resumes")
                        .file(fakePdf("fake.pdf", "just text pretending".getBytes()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("File content does not look like a valid PDF"));
    }

    @Test
    void rejectsOversizedFile() throws Exception {
        signup("resume.big@example.com");
        String token = accessTokenFor("resume.big@example.com");

        byte[] big = new byte[500];
        Arrays.fill(big, (byte) 'x');
        big[0] = '%';
        big[1] = 'P';
        big[2] = 'D';
        big[3] = 'F';

        mockMvc.perform(multipart("/api/v1/resumes")
                        .file(fakePdf("big.pdf", big))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("File exceeds the maximum size of 200 bytes"));
    }

    @Test
    void rejectsWrongExtension() throws Exception {
        signup("resume.txt@example.com");
        String token = accessTokenFor("resume.txt@example.com");

        mockMvc.perform(multipart("/api/v1/resumes")
                        .file(new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Only PDF and DOCX files are accepted"));
    }

    @Test
    void unauthenticatedUploadIsUnauthorized() throws Exception {
        mockMvc.perform(multipart("/api/v1/resumes").file(fakePdf("cv.pdf", null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listReturnsOnlyOwnResumes() throws Exception {
        signup("resume.a@example.com");
        signup("resume.b@example.com");
        String tokenA = accessTokenFor("resume.a@example.com");
        String tokenB = accessTokenFor("resume.b@example.com");

        mockMvc.perform(multipart("/api/v1/resumes")
                        .file(fakePdf("a-cv.pdf", null))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/resumes").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/v1/resumes").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].originalFilename").value("a-cv.pdf"));
    }
}
