package com.creditlens.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test: real Spring context, real H2 database, real Flyway migration,
 * HTTP simulated by MockMvc. @Transactional rolls back after each test so tests stay independent.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApplicationApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private static String body(int score, String employment) {
        return """
                {
                  "applicantName": "Test Applicant",
                  "email": "Test.Applicant@Example.com",
                  "annualIncome": 90000,
                  "monthlyDebtPayments": 300,
                  "creditScore": %d,
                  "requestedAmount": 12000,
                  "termMonths": 36,
                  "employmentStatus": "%s",
                  "productType": "PERSONAL_LOAN"
                }
                """.formatted(score, employment);
    }

    private long createAndGetId(int score) throws Exception {
        MvcResult result = mvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON).content(body(score, "FULL_TIME")))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = json.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    @Test
    void strongApplicationIsApprovedAndReturns201WithLocation() throws Exception {
        mvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON).content(body(780, "FULL_TIME")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("http://localhost/api/applications/")))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.referenceNumber", startsWith("CL-")))
                .andExpect(jsonPath("$.emailMasked").value("t*************@example.com"))
                .andExpect(jsonPath("$.reasons", hasSize(0)));
    }

    @Test
    void invalidInputReturns400WithFieldErrors() throws Exception {
        String bad = """
                { "applicantName": "", "email": "not-an-email", "annualIncome": 50000,
                  "monthlyDebtPayments": 0, "creditScore": 200, "requestedAmount": 5000,
                  "termMonths": 36, "employmentStatus": "FULL_TIME", "productType": "CAR_LOAN" }
                """;
        mvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.creditScore").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.applicantName").exists());
    }

    @Test
    void unknownEnumValueReturns400() throws Exception {
        mvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON)
                        .content(body(700, "ASTRONAUT")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void referredApplicationCanBeApprovedByUnderwriterAndIsAudited() throws Exception {
        long id = createAndGetId(630); // 600-659 band -> REFERRED

        mvc.perform(get("/api/applications/{id}", id))
                .andExpect(jsonPath("$.status").value("REFERRED"))
                .andExpect(jsonPath("$.reasons[0].code").value("CS02"));

        mvc.perform(post("/api/applications/{id}/review", id).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"approve": true, "underwriter": "J. Rowe", "note": "Verified pay stubs"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decidedBy").value("J. Rowe"))
                .andExpect(jsonPath("$.approvedAmount").value(12000.0));

        mvc.perform(get("/api/applications/{id}/audit", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].action").value("AUTO_DECISION"))
                .andExpect(jsonPath("$[1].action").value("MANUAL_REVIEW"))
                .andExpect(jsonPath("$[1].fromStatus").value("REFERRED"));
    }

    @Test
    void reviewingAnApprovedApplicationReturns409() throws Exception {
        long id = createAndGetId(790);

        mvc.perform(post("/api/applications/{id}/review", id).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"approve": false, "underwriter": "J. Rowe", "note": "Trying to override"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid state"));
    }

    @Test
    void missingApplicationReturns404() throws Exception {
        mvc.perform(get("/api/applications/{id}", 987654))
                .andExpect(status().isNotFound());
    }

    @Test
    void previewSavesNothing() throws Exception {
        mvc.perform(post("/api/decisions/preview").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "annualIncome": 90000, "monthlyDebtPayments": 300, "creditScore": 780,
                                  "requestedAmount": 12000, "termMonths": 36,
                                  "employmentStatus": "FULL_TIME", "productType": "PERSONAL_LOAN" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("APPROVED"));

        mvc.perform(get("/api/stats"))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void correlationIdIsEchoedAndSecurityHeadersAreSet() throws Exception {
        mvc.perform(get("/api/stats").header("X-Correlation-Id", "test-correlation-1234"))
                .andExpect(header().string("X-Correlation-Id", "test-correlation-1234"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void unsafeCorrelationIdIsReplaced() throws Exception {
        mvc.perform(get("/api/stats").header("X-Correlation-Id", "bad\nvalue"))
                .andExpect(header().string("X-Correlation-Id",
                        org.hamcrest.Matchers.matchesPattern("[0-9a-f-]{36}")));
    }
}
